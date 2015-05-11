/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.util.logging.Level;

import org.geoserver.importer.ImportTask;
import org.geotools.data.DataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class AttributesToPointGeometryTransform extends AbstractTransform implements
        InlineVectorTransform {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final String POINT_NAME = "location";

    private final String latField;

    private final String lngField;

    private final String pointFieldName;

    private final GeometryFactory geometryFactory;

    public AttributesToPointGeometryTransform(String latField, String lngField) {
        this(latField, lngField, AttributesToPointGeometryTransform.POINT_NAME);
    }

    public AttributesToPointGeometryTransform(String latField, String lngField,
            String pointFieldName) {
        this.latField = latField;
        this.lngField = lngField;
        this.pointFieldName = pointFieldName;
        geometryFactory = new GeometryFactory();
    }

    @Override
    public SimpleFeatureType apply(ImportTask task, DataStore dataStore,
            SimpleFeatureType featureType) throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(featureType);

        int latIndex = featureType.indexOf(latField);
        int lngIndex = featureType.indexOf(lngField);
        if (latIndex < 0 || lngIndex < 0) {
            throw new Exception("FeatureType " + featureType.getName()
                    + " does not have lat lng fields named '" + latField + "'" + " and " + "'"
                    + lngField + "'");
        }

        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        if (geometryDescriptor != null) {
            builder.remove(geometryDescriptor.getLocalName());
        }
        builder.remove(latField);
        builder.remove(lngField);
		builder.srid(this.geometryFactory.getSRID());
        if (this.geometryFactory.getSRID() > 0) {
        	try {
        		CoordinateReferenceSystem crs = CRS.decode("EPSG:" + this.geometryFactory.getSRID(), true);
				builder.setCRS(crs);
        	} catch (Exception e) {
        		LOGGER.log(Level.WARNING, "Could not set Default Geometry SRID!", e);
        	}
        }
        builder.add(pointFieldName, Point.class);
        builder.setDefaultGeometry(pointFieldName);

        final SimpleFeatureType buildFeatureType = builder.buildFeatureType();
        
		return buildFeatureType;
    }

    @Override
    public SimpleFeature apply(ImportTask task, DataStore dataStore, SimpleFeature oldFeature,
            SimpleFeature feature) throws Exception {
        Object latObject = oldFeature.getAttribute(latField);
        Object lngObject = oldFeature.getAttribute(lngField);
        Double lat = asDouble(latObject);
        Double lng = asDouble(lngObject);
        if (lat == null || lng == null) {
            feature.setDefaultGeometry(null);
        } else {
            Coordinate coordinate = new Coordinate(lng, lat);
            Point point = geometryFactory.createPoint(coordinate);
            final String geomAttName = feature.getDefaultGeometryProperty().getName().getLocalPart();
            if (geomAttName
                    .equals(pointFieldName)) {
                feature.setAttribute(pointFieldName, point);
            } else {
                feature.setAttribute(geomAttName, point);
            }
        }
        return feature;
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLatField() {
        return latField;
    }

    public String getLngField() {
        return lngField;
    }

}
