/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.Resources;
import org.geoserver.wps.gs.resource.model.Translate;
import org.geoserver.wps.gs.resource.model.TranslateItem;
import org.geoserver.wps.gs.resource.model.plugin.VectorialLayer;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * 
 * @author alessio.fabiani
 * 
 */
@SuppressWarnings("deprecation")
@DescribeProcess(title = "Resource Loader Process", description = "Downloads Layer Stream and provides a ZIP.")
public class ResourceLoaderProcess implements GSProcess {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(ResourceLoaderProcess.class);

    /** The catalog. */
    private final Catalog catalog;
    
    private WPSResourceManager resourceManager;

    public ResourceLoaderProcess(GeoServer geoServer, WPSResourceManager resourceManager) {
        Utilities.ensureNonNull("geoServer", geoServer);
        this.catalog = geoServer.getCatalog();
        this.resourceManager = resourceManager;
    }

    @DescribeResult(name = "result", description = "Zipped output files to download")
    public String execute(
            @DescribeParameter(name = "resourcesXML", min = 1, description = "XML describing the Resources to load") String resourcesXML,
            final ProgressListener progressListener) throws ProcessException {

        // Initialize Unmarshaller
        XStream xs = initialize();

        // De-serialize resources
        File test;
        try {
            test = new File(ResourceLoaderProcess.class.getResource("test1.xml").toURI());
            Resources resources = (Resources) xs.fromXML(test);
            
            // Create-or-update the resources
            for (Resource resource : resources.getResources()) {
                
                // Sanity Checks
                if (!ResourceLoaderUtilities.resourceIsWellDefined(resource)) {
                    throw new IllegalArgumentException("The resources definition were not well formed.");
                }
            }

            // Store XML into the WPS folder
            // TODO
            storeResourcesXML(xs, resources);
            
        } catch (Exception cause) {
            throw new ProcessException(cause);
        }

        return null;
    }

    /**
     * @return
     * @throws IllegalArgumentException
     */
    protected XStream initialize() throws IllegalArgumentException {
        XStreamPersisterFactory xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStream xs = xpf.createXMLPersister().getXStream();

        // Aliases
        xs.alias("resources", Resources.class);
        xs.alias("resource", Resource.class);
        xs.aliasField("abstract", Resource.class, "abstractTxt");
        xs.aliasAttribute(Resource.class, "type", "class");

        xs.alias("nativeBoundingBox", Map.class);

        xs.alias("defaultStyle", Map.class);
        xs.alias("metadata", Map.class);

        xs.alias("translate", Translate.class);
        xs.alias("item", TranslateItem.class);
        xs.aliasAttribute(TranslateItem.class, "storeClass", "class");

        // Converters
        xs.addImplicitCollection(Resources.class, "resources");
        xs.addImplicitCollection(Translate.class, "items");

        xs.registerConverter(new MapEntryConverter());
        
        xs.registerConverter(new ResourceConverter());
        
        /*for (ResourceLoaderConverter extension : GeoServerExtensions.extensions(ResourceLoaderConverter.class)) {
            xs.registerConverter(extension);
        }*/
        
        return xs;
    }

    /**
     * @param xs
     * @param resources
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void storeResourcesXML(XStream xs, Resources resources)
            throws IllegalArgumentException, IOException, FileNotFoundException {
        final String executionId = resourceManager.getExecutionId(null);
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        File wpsProcessFolder = new File(loader.getBaseDirectory().getCanonicalPath(), "/temp/wps/" + executionId);
        
        if (!wpsProcessFolder.exists()) {
            wpsProcessFolder.mkdirs();
        }
        
        if (wpsProcessFolder.exists() && wpsProcessFolder.isDirectory()) {
            final File wpsResourceLoaderOutput = new File(wpsProcessFolder, "resources_" + executionId + ".xml");
            final FileOutputStream fos = new FileOutputStream(wpsResourceLoaderOutput);
            try {
                xs.toXML(resources, fos);
            }
            catch(Exception cause) {
                LOGGER.log(Level.SEVERE, "Could not marshall Resources.", cause);
            }
            finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }
    }

    /**
     * 
     * @author alessio.fabiani
     *
     */
    public static class MapEntryConverter implements Converter {

        @Override
        public boolean canConvert(Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            AbstractMap map = (AbstractMap) value;
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;
                writer.startNode(entry.getKey().toString());
                writer.setValue(entry.getValue().toString());
                writer.endNode();
            }

        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            Map<String, Object> map = new HashMap<String, Object>();

            while (reader.hasMoreChildren()) {
                reader.moveDown();

                String key = reader.getNodeName(); // nodeName aka element's name
                Object value = reader.getValue();
                if (AbstractMap.class.isAssignableFrom(value.getClass()))
                    value = unmarshal(reader, context);
                map.put(key, value);

                reader.moveUp();
            }

            return map;
        }

    }
    
    
    /**
     * 
     * @author alessio.fabiani
     *
     */
    public static class ResourceConverter implements Converter {

        @Override
        public boolean canConvert(Class clazz) {
            return Resource.class.isAssignableFrom(clazz);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            for (ResourceLoaderConverter extension : GeoServerExtensions.extensions(ResourceLoaderConverter.class)) {
                if (extension.canConvert(value.getClass())) {
                    extension.marshal(value, writer, context);
                    break;
                }
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            final String type = reader.getAttribute("class");
            
            for (ResourceLoaderConverter extension : GeoServerExtensions.extensions(ResourceLoaderConverter.class)) {
                if (extension.getTYPE().equals(type)) {
                    Resource resource = (Resource) extension.unmarshal(reader, context);
                    resource.setType(type);
                    return resource;
                }
            }
            
            return null;
        }
        
    }
}
