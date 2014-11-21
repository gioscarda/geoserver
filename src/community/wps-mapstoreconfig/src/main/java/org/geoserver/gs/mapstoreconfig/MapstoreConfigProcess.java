/*
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.gs.mapstoreconfig;

/**
 * @author DamianoG
 *
 */
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.gs.mapstoreconfig.components.GeoserverTemplateDirLoader;
import org.geoserver.gs.mapstoreconfig.ftl.model.LayerTemplateModel;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.gs.GeorectifyConfiguration;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.BoundingBox;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@DescribeProcess(title = "mapstoreConfigProcess", description = "A wps process responsible for generate Mapstore jsnon configuration files")
public class MapstoreConfigProcess  implements GeoServerProcess {

    private static final Logger LOGGER = Logging.getLogger(MapstoreConfigProcess.class);
    
    /**
     * The name of the template to load
     */
    public final static String TEMPLATE_NAME = "mapstoreTemplate.tpl";
    /**
     * The name of the properties file that stores the default values
     */
    public final static String DEFAULT_PROPERTIES_NAME = "defaultMapstoreConfigValues.tpl";
    
    /**
     * Interface responsible to load the template dir. Use {@link GeoserverTemplateDirLoader} as implementation to place the template dir inside a Geoserver Datadir
     */
    private TemplateDirLoader templateDirLoader;
    
    /**
     * This Interface manage a layer descriptor. Different implementation should support different datatype supported (xml, json, plaintext ecc...).
     */
    private LayerDescriptorManager layerDescriptorManager;
    
    /**
     * @param templateDirLoader the templateDirLoader to set
     */
    public void setTemplateDirLoader(TemplateDirLoader templateDirLoader) {
        this.templateDirLoader = templateDirLoader;
    }
    
    /**
     * @param layerDescriptorManager the layerDescriptorManager to set
     */
    public void setLayerDescriptorManager(LayerDescriptorManager layerDescriptorManager) {
        this.layerDescriptorManager = layerDescriptorManager;
    }

    @DescribeResult(name = "JSON MapStore configuration file", description = "output result", type=String.class)
    public String execute(
            @DescribeParameter(name = "layerDescriptor", min=1, description="An xml document that provides a description of a set of layers") String layerDescriptor, 
            @DescribeParameter(name = "bbox", min=0, description="The default BoundingBox to set in the layer definition" ) BoundingBox bbox, 
            @DescribeParameter(name = "minTime", min=0, description="The default lower time interval border") String minTime, 
            @DescribeParameter(name = "maxTime", min=0, description="The default upper interval border") String maxTime,
            //This flag still deoesn't have any effect since atm the only model provided to parse the layerDescriptor doesn't have the Coordinates
            @DescribeParameter(name = "forceDefaultValuesUsage", min=1, description="***NB: still not implemented*** Flag indicate that the default bbox and interval values must overrides also the ones in the XML document if presents") boolean forceDefaultValuesUsage) throws IOException {
        
        //Manage the layerDescriptor and produce the value to substitute in the FTL template
        layerDescriptorManager.loadDocument(layerDescriptor, true);
        if(!layerDescriptorManager.validateDocument()){
         // TODO How handle this? How throw an Exception???
            LOGGER.severe("The provided layerDescriptor Document is not valid for the '" + layerDescriptorManager.mimeFormatHandled() + "' input format...");
        }
        layerDescriptorManager.setBbox(bbox);
        layerDescriptorManager.setMinTime(minTime);
        layerDescriptorManager.setMaxTime(maxTime);
        layerDescriptorManager.setForceDefaultValuesUsage(forceDefaultValuesUsage);
        List<LayerTemplateModel> model = null;
        
        // Could maybe throw an exception
        model = layerDescriptorManager.produceModelForFTLTemplate(templateDirLoader);
        
        //Load the template Location
        File templateDir = null;
     
        // Could maybe throw an exception
        templateDir = templateDirLoader.getTemplateDir();
        
        // Setup the FTL Context
        Configuration cfg = new Configuration();
        
        // Could maybe throw an exception
        // Where do we load the templates from:
        cfg.setDirectoryForTemplateLoading(templateDir);
        
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.ENGLISH);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
        // Set the values to use for the template resolving
        Map<String, Object> input = new HashMap<String, Object>();
        
//        input.put("layers", MapstoreConfigTest.produceModel());
        input.put("layers", model);
        
        //Load the FTL template
        // Could maybe throw an exception
        Template template = cfg.getTemplate(TEMPLATE_NAME);

        
        // Resolve the template
        // Could maybe throw an exception
        Writer writer = new StringWriter();
        try {
            template.process(input, writer);
        } catch (TemplateException e) {
            throw new IOException("An instance of TemplateException as been thrown, reporting its mesage: '" + e.getMessage() + "'");
        }
        
        return writer.toString();
    }
}