/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.Resources;
import org.geoserver.wps.gs.resource.model.impl.VectorialLayer;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;
import org.geoserver.wps.gs.resource.model.translate.TranslateItem;
import org.geoserver.wps.gs.resource.model.translate.impl.DataStoreItem;
import org.geoserver.wps.gs.resource.model.translate.impl.TransformItem;
import org.geotools.process.ProcessException;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class ResourceModelTest extends WPSResourceTestSupport {

    @Test
    public void testVectorialLayerMarshalling() {

        // Initialize Unmarshaller
        XStream xs = initialize();

        // De-serialize resources
        File test1;
        try {
            test1 = new File(testData.getDataDirectoryRoot().getAbsolutePath(), "test1.xml");
            assertTrue(test1.exists());

            String dump = null;
            FileInputStream fis = new FileInputStream(test1);
            try {
                dump = IOUtils.toString(fis, "UTF-8");
            } finally {
                IOUtils.closeQuietly(fis);
            }

            Resources resources = (Resources) xs.fromXML(dump);
            assertNotNull(resources);

            assertEquals("Number of un-marshalled resources", resources.getResources().size(), 1);

            // Create-or-update the resources
            for (Resource resource : resources.getResources()) {

                // Sanity Checks
                assertTrue(resource.isWellDefined());
            }

            Resource layer = resources.getResources().get(0);
            assertTrue(layer instanceof VectorialLayer);
            assertEquals("test", layer.getName());
            assertEquals("A Test Vectorial Resource", ((VectorialLayer) layer).getTitle());

            TranslateContext txContext = layer.getTranslateContext();
            assertNotNull(txContext);

            assertEquals("Number of un-marshalled resource translate items", txContext.getItems()
                    .size(), 3);
            final TranslateItem txCtxFirstItem = txContext.getFirst();
            final TranslateItem txCtxLastItem = txContext.getLast();

            assertEquals(txCtxFirstItem, txContext.getItems().first());
            assertEquals(txCtxLastItem, txContext.getItems().last());

            assertTrue(txCtxFirstItem instanceof DataStoreItem);

            final TranslateItem txCtxTxItem = txContext.getNext(txCtxFirstItem);
            assertTrue(txCtxTxItem instanceof TransformItem);

            TransformItem tx = (TransformItem) txCtxTxItem;
            assertNotNull(tx.getTransform());

        } catch (Exception cause) {
            fail(cause.getMessage());
            throw new ProcessException(cause);
        }
    }

}
