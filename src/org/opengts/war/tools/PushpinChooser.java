// ----------------------------------------------------------------------------
// Copyright 2007-2017, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2016/12/21  Martin D. Flynn
//     -Extracted from PushpinIcon.java
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.opengts.util.*;
import org.opengts.db.*;
//import org.opengts.db.tables.*;

public class PushpinChooser
{

    /**
    *** Writes the PushpinChooser JavaScript to the specified PrintWriter
    **/
    public static void writePushpinChooserJS(PrintWriter out, RequestProperties reqState, boolean inclBlank)
        throws IOException
    {
        Locale             locale  = reqState.getLocale();
        MapProvider        mp      = reqState.getMapProvider();
        HttpServletRequest request = reqState.getHttpServletRequest();
        OrderedMap<String,PushpinIcon> iconMap = (mp != null)? mp.getPushpinIconMap(reqState) : null;

        /* write Pushpin image array */
        // -- start of array section
        JavaScriptTools.writeStartJavaScript(out);
        StringBuffer sb = new StringBuffer();
        sb.append("var ppcPushpinChooserList = new Array(\n");
        // --
        int count = 0;
        if (inclBlank) {
            I18N   i18n = I18N.getI18N(PushpinIcon.class, locale);
            String desc = i18n.getString("PushpinIcon.default","default");
            sb.append("   { name:\"\", desc:\""+desc+"\", isEval:false, image:\"\", width:24, height:0, index:-1 }");
            count++;
        }
        if (iconMap != null) {
            int ppNdx = 0;
            for (Iterator<PushpinIcon> p = iconMap.values().iterator(); p.hasNext();) {
                PushpinIcon ppi = p.next();
                // -- Description
                String desc = ppi.getName();
                if (desc.length() > 4) {
                    int st = 0;
                    StringBuffer nd = new StringBuffer();
                    for (int c = 0; c < desc.length(); c++) {
                        char ch = desc.charAt(c);
                        if (Character.isUpperCase(ch)) {
                            // -- is upper case
                            if (st != 0) {
                                // -- previous not upper case
                                nd.append(" ");
                                st = 0;
                            }
                            nd.append(ch);
                        } else 
                        if (Character.isLowerCase(ch)) {
                            // -- is lower case
                            if ((st != 0) && (st != 1)) {
                                // -- previous not a letter
                                nd.append(" ");
                            }
                            st = 1;
                            nd.append(ch);
                        } else
                        if (Character.isDigit(ch)) {
                            // -- is a digit
                            if (st != 2) {
                                // -- previous not a digit
                                nd.append(" ");
                                st = 2;
                            }
                            nd.append(ch);
                        } else {
                            // -- is a special char
                            if (st != 3) {
                                // -- previous not a special char
                                nd.append(" ");
                                st = 3;
                            }
                        }
                    }
                    desc = nd.toString().trim();
                }
                // -- Javascript array record
                if (count++ > 0) { sb.append(",\n"); }
                sb.append("   {");
                sb.append(" name:\"").append(ppi.getName()).append("\",");
                sb.append(" desc:\"").append(desc).append("\",");
                sb.append(" isEval:").append(ppi.getIconEval()).append(",");
                sb.append(" image:\"").append(StringTools.blankDefault(ppi.getImageURL(),"?")).append("\",");
                sb.append(" width:").append(ppi.getIconWidth()).append(",");
                sb.append(" height:").append(ppi.getIconHeight()).append(",");
                sb.append(" index:").append(ppNdx++);
                sb.append(" }");
            }
            sb.append("\n");
        }
        // -- end of array section
        sb.append("   );\n");
        out.write(sb.toString());
        JavaScriptTools.writeEndJavaScript(out);

        /* write include files */
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("PushpinChooser.js"), request);

    }

}
