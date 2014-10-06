/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.gs.resource.model.translate;

import java.io.IOException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.Importer;
import org.geoserver.importer.MemoryImportStore;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geotools.util.logging.Logging;

/**
 * @author alessio.fabiani
 * 
 */
public class TranslateContext {

    /** */
    static Logger LOGGER = Logging.getLogger(TranslateContext.class);

    private SortedSet<TranslateItem> items = new TreeSet<TranslateItem>(
            new Comparator<TranslateItem>() {

                @Override
                public int compare(TranslateItem o1, TranslateItem o2) {
                    return Integer.compare(o1.getOrder(), o2.getOrder());
                }

            });

    /** we don't want this field streamed out */
    transient private Importer importer;

    /** we don't want this field streamed out */
    transient private ImportContext importContext;

    /** we don't want this field streamed out */
    transient private Resource originator;

    /** we don't want this field streamed out */
    transient private Catalog catalog;

    /**
     * @return the importer
     */
    public Importer getImporter() {
        return importer;
    }

    /**
     * @return the importContext
     */
    public ImportContext getImportContext() {
        return importContext;
    }

    /**
     * @param importContext the importContext to set
     */
    public void setImportContext(ImportContext importContext) {
        this.importContext = importContext;
    }

    /**
     * 
     * @return
     */
    public Catalog getCatalog() {
        return this.catalog;
    }

    /**
     * 
     * @param catalog
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 
     * @return
     */
    public Resource getOriginator() {
        return this.originator;
    }

    /**
     * 
     * @param resource
     */
    public void setOriginator(Resource resource) {
        this.originator = resource;
    }

    /**
     * @return the items
     */
    public SortedSet<TranslateItem> getItems() {
        return items;
    }

    /**
     * @param items the items to set
     */
    public void setItems(SortedSet<TranslateItem> items) {
        this.items = items;
    }

    /**
     * 
     * @param item
     * @return
     */
    public TranslateItem getPrevious(TranslateItem item) {
        TranslateItem previous = null;

        if (item.getOrder() > 0) {
            O: for (TranslateItem ii : items) {
                if (ii.getOrder() < item.getOrder()) {
                    previous = ii;
                } else {
                    break O;
                }
            }
        }

        return previous;
    }

    /**
     * 
     * @param item
     * @return
     */
    public TranslateItem getNext(TranslateItem item) {
        TranslateItem next = null;

        O: for (TranslateItem ii : items) {
            if (ii.getOrder() > item.getOrder()) {
                next = ii;
                break O;
            } else {
                continue;
            }
        }

        return next;
    }

    /**
     * 
     * @return
     */
    public TranslateItem getFirst() {
        return this.items.first();
    }

    /**
     * 
     * @return
     */
    public TranslateItem getLast() {
        return this.items.last();
    }

    /**
     * @throws IOException
     * 
     */
    public void run() throws IOException {
        if (items != null && !items.isEmpty()) {

            importer = (Importer) GeoServerExtensions.bean("importer");
            // clean up the import history (to isolate tests from each other)
            MemoryImportStore store = (MemoryImportStore) importer.getStore();
            store.destroy();

            // start the workflow
            TranslateItem next = this.items.first();
            while (next != null) {

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Running Resource Translation Item {" + next.getOrder()
                            + "}");
                }
                next = next.run(this);
            }

            // lunch the importer
            importer.run(importContext);
        }
    }

}
