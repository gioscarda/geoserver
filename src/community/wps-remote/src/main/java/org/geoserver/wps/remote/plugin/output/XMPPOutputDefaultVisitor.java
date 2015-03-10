/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.output;

import org.geoserver.wps.remote.plugin.XMPPClient;

/**
 * @author Alessio
 * 
 */
public class XMPPOutputDefaultVisitor implements XMPPOutputVisitor {

    @Override
    public Object visit(XMPPTextualOutput visitor, Object value, String type, String pID, String baseURL,
            XMPPClient xmppClient, boolean publish, String defaultStyle, String targetWorkspace) throws Exception {
        if (type.equals("textual")) {
            return visitor.produceOutput(value, type, pID, baseURL, xmppClient, publish, defaultStyle, targetWorkspace);
        }

        return null;
    }

    @Override
    public Object visit(XMPPRawDataOutput visitor, Object value, String type, String pID, String baseURL,
            XMPPClient xmppClient, boolean publish, String defaultStyle, String targetWorkspace) throws Exception {
        if (value != null && value instanceof String && !((String) value).isEmpty()) {
            return visitor.produceOutput(value, type, pID, baseURL, xmppClient, publish, defaultStyle, targetWorkspace);
        }

        return null;
    }

}
