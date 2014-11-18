/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.*;

import java.util.List;

import org.geoserver.catalog.CascadeRemovalReporter;
import org.geoserver.catalog.CascadeRemovalReporter.ModificationType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class CascadeRemovalReporterTest extends CascadeVisitorAbstractTest {
    
    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }

    @Test
    public void testCascadeLayer() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        String name = getLayerId(MockData.LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);
        visitor.visit(layer);
        //layer.accept(visitor);
        
        // we expect a layer, a resource and a group
        assertEquals(3, visitor.getObjects(null).size());
        
        // check the layer and resource have been marked to delete (and
        assertEquals(catalog.getLayerByName(name), 
                visitor.getObjects(LayerInfo.class, ModificationType.DELETE).get(0));
        assertEquals(catalog.getResourceByName(name, ResourceInfo.class), 
                visitor.getObjects(ResourceInfo.class, ModificationType.DELETE).get(0));
        
        // the group has been marked to update? (we need to compare by id as the
        // objects won't compare properly by equality)
        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(group.getId(), visitor.getObjects(LayerGroupInfo.class, 
                ModificationType.GROUP_CHANGED).get(0).getId());
    }
    
    @Test
    public void testCascadeStore() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        String citeStore = MockData.CITE_PREFIX;
        StoreInfo store = catalog.getStoreByName(citeStore, StoreInfo.class);
        String buildings = getLayerId(MockData.BUILDINGS);
        String lakes = getLayerId(MockData.LAKES);
        LayerInfo bl = catalog.getLayerByName(buildings);
        ResourceInfo br = catalog.getResourceByName(buildings, ResourceInfo.class);
        LayerInfo ll = catalog.getLayerByName(lakes);
        ResourceInfo lr = catalog.getResourceByName(lakes, ResourceInfo.class);
        
        visitor.visit((DataStoreInfo)store);
        
        assertEquals(store, visitor.getObjects(StoreInfo.class, ModificationType.DELETE).get(0));
        List<LayerInfo> layers = visitor.getObjects(LayerInfo.class, ModificationType.DELETE);
        assertTrue(layers.contains(bl));
        assertTrue(layers.contains(ll));
        List<ResourceInfo> resources = visitor.getObjects(ResourceInfo.class, ModificationType.DELETE);
        assertTrue(resources.contains(br));
        assertTrue(resources.contains(lr));
    }
    
    @Test
    public void testCascadeWorkspace() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        assertNotNull(ws);
        List<StoreInfo> stores = getCatalog().getStoresByWorkspace(ws, StoreInfo.class);
        
        ws.accept(visitor);
        
        assertTrue(stores.containsAll(visitor.getObjects(StoreInfo.class, ModificationType.DELETE)));
    }
    
    @Test
    public void testCascadeStyle() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        StyleInfo style = catalog.getStyleByName(MockData.LAKES.getLocalPart());
        LayerInfo buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        
        visitor.visit(style);

        // test style reset
        assertEquals(style, visitor.getObjects(StyleInfo.class, ModificationType.DELETE).get(0));
        assertEquals(lakes, visitor.getObjects(LayerInfo.class, ModificationType.STYLE_RESET).get(0));
        assertEquals(buildings, visitor.getObjects(LayerInfo.class, ModificationType.EXTRA_STYLE_REMOVED).get(0));
    }


}
