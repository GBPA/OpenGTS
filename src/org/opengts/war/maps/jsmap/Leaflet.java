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
// References:
//  - http://leafletjs.com/
//  - http://leafletjs.com/reference.html
//  - http://leafletjs.com/reference-1.0.0.html
// ----------------------------------------------------------------------------
// Change History:
//  2016/09/01  Martin D. Flynn
//     -Initial release
//  2017/10/09  Martin D. Flynn
//     -Updated to specify "leaflet.js" version 1.0.3
// ----------------------------------------------------------------------------
package org.opengts.war.maps.jsmap;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.db.tables.Geozone;
import org.opengts.war.tools.*;
import org.opengts.war.maps.JSMap;

public class Leaflet
    extends JSMap
{

    // ------------------------------------------------------------------------

    /* Leaflet 0.7.5 [OBSOLETE] */
    private static final String LEAFLET_CSS_0_7_5           = "cdn.leafletjs.com/leaflet-0.7.5/leaflet.css";
    private static final String LEAFLET_JS_0_7_5            = "cdn.leafletjs.com/leaflet-0.7.5/leaflet.js";

    /* Leaflet 1.0.0 */
    // -- https://unpkg.com/leaflet@1.0.0-rc.3/dist/leaflet.js
    private static final String LEAFLET_CSS_1_0_0           = "unpkg.com/leaflet@1.0.0-rc.3/dist/leaflet.css";
    private static final String LEAFLET_JS_1_0_0            = "unpkg.com/leaflet@1.0.0-rc.3/dist/leaflet.js";

    /* Leaflet 1.0.3 */
    // -- https://unpkg.com/leaflet@1.0.3/dist/leaflet.css
    private static final String LEAFLET_CSS_1_0_3           = "unpkg.com/leaflet@1.0.3/dist/leaflet.css";
    private static final String LEAFLET_JS_1_0_3            = "unpkg.com/leaflet@1.0.3/dist/leaflet.js";

    /* Leaflet */
    private static final String LEAFLET_CSS                 = LEAFLET_CSS_1_0_3;
    private static final String LEAFLET_JS                  = LEAFLET_JS_1_0_3;

    // ------------------------------------------------------------------------

    /* http ssl */
    private static final String HTTP_                       = "http://";
    private static final String HTTPS_                      = "https://";

    // ------------------------------------------------------------------------

    /* Leaflet support properties */
    private static final String PROP_useSSL[]               = { "leaflet.useSSL"                                         };
    private static final String PROP_css[]                  = { "leaflet.style"                , "leaflet.css"           };
    private static final String PROP_js[]                   = { "leaflet.mapcontrol"           , "leaflet.js"            };
    private static final String PROP_attributionPrefix[]    = { "leaflet.attributionPrefix"    , "attributionPrefix"     };
    private static final String PROP_addOpenStreetMap[]     = { "leaflet.addOpenStreetMap"     , "addOpenStreetMap"      };
    private static final String PROP_addOpenCycleMap[]      = { "leaflet.addOpenCycleMap"      , "addOpenCycleMap"       };
  //private static final String PROP_addOneMapSLAMap[]      = { "leaflet.addOneMapSLAMap"      , "addOneMapSLAMap"       };

    /* Custom Map Tile Provider #1 */
    private static final String PROP_customMap1_Name[]      = { "leaflet.customMap1.name"      , "customMap1.name"       };
    private static final String PROP_customMap1_URL[]       = { "leaflet.customMap1.URL"       , "customMap1.URL"        };
    private static final String PROP_customMap1_Subdomains[]= { "leaflet.customMap1.subdomains", "customMap1.subdomains" };
    private static final String PROP_customMap1_AuthID[]    = { "leaflet.customMap1.authID"    , "customMap1.authID"     };
    private static final String PROP_customMap1_AuthCode[]  = { "leaflet.customMap1.authCode"  , "customMap1.authCode"   };
    private static final String PROP_customMap1_Copyright[] = { "leaflet.customMap1.copyright" , "customMap1.copyright"  };
    private static final String PROP_customMap1_MaxZoom[]   = { "leaflet.customMap1.maxZoom"   , "customMap1.maxZoom"    };
    private static final String PROP_customMap1_InitFTN[]   = { "leaflet.customMap1.initFTN"   , "customMap1.initFTN"    };

    /* Custom Map Tile Provider #2 */
    private static final String PROP_customMap2_Name[]      = { "leaflet.customMap2.name"      , "customMap2.name"        };
    private static final String PROP_customMap2_URL[]       = { "leaflet.customMap2.URL"       , "customMap2.URL"         };
    private static final String PROP_customMap2_Subdomains[]= { "leaflet.customMap2.subdomains", "customMap2.subdomains"  };
    private static final String PROP_customMap2_AuthID[]    = { "leaflet.customMap2.authID"    , "customMap2.authID"      };
    private static final String PROP_customMap2_AuthCode[]  = { "leaflet.customMap2.authCode"  , "customMap2.authCode"    };
    private static final String PROP_customMap2_Copyright[] = { "leaflet.customMap2.copyright" , "customMap2.copyright"   };
    private static final String PROP_customMap2_MaxZoom[]   = { "leaflet.customMap2.maxZoom"   , "customMap2.maxZoom"     };
    private static final String PROP_customMap2_InitFTN[]   = { "leaflet.customMap2.initFTN"   , "customMap2.initFTN"    };

    /* Custom Map Tile Provider #3 */
    private static final String PROP_customMap3_Name[]      = { "leaflet.customMap3.name"      , "customMap3.name"        };
    private static final String PROP_customMap3_URL[]       = { "leaflet.customMap3.URL"       , "customMap3.URL"         };
    private static final String PROP_customMap3_Subdomains[]= { "leaflet.customMap3.subdomains", "customMap3.subdomains"  };
    private static final String PROP_customMap3_AuthID[]    = { "leaflet.customMap3.authID"    , "customMap3.authID"      };
    private static final String PROP_customMap3_AuthCode[]  = { "leaflet.customMap3.authCode"  , "customMap3.authCode"    };
    private static final String PROP_customMap3_Copyright[] = { "leaflet.customMap3.copyright" , "customMap3.copyright"   };
    private static final String PROP_customMap3_MaxZoom[]   = { "leaflet.customMap3.maxZoom"   , "customMap3.maxZoom"     };
    private static final String PROP_customMap3_InitFTN[]   = { "leaflet.customMap3.initFTN"   , "customMap3.initFTN"    };

    // ------------------------------------------------------------------------

    private static final int    DEFAULT_ZOOM                = 4;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Leaflet instance */
    public Leaflet(String name, String key)
    {
        super(name, key);
        this.addSupportedFeature(FEATURE_LATLON_DISPLAY);
        this.addSupportedFeature(FEATURE_DISTANCE_RULER);
        this.addSupportedFeature(FEATURE_GEOZONES);
        this.addSupportedFeature(FEATURE_DETAIL_REPORT);
        this.addSupportedFeature(FEATURE_DETAIL_INFO_BOX);
        this.addSupportedFeature(FEATURE_REPLAY_POINTS);
        this.addSupportedFeature(FEATURE_CENTER_ON_LAST);
        this.addSupportedFeature(FEATURE_CORRIDORS);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if SSL should be used
    **/
    private boolean _useSSL(RequestProperties reqState)
    {
        MapProvider mp = reqState.getMapProvider();
        RTProperties mrtp = (mp != null)? mp.getProperties() : null;
        /* SSL? */
        boolean useSSL = false;
        String  useSSLStr = (mrtp != null)? mrtp.getString(PROP_useSSL, null) : null;
        if (StringTools.isBlank(useSSLStr)) {
            // -- default: follow parent URL secure protocol
            useSSL = reqState.isSecure()? true : false;
        } else
        if (useSSLStr.equalsIgnoreCase("auto")) {
            // -- auto: follow parent URL secure protocol
            useSSL = reqState.isSecure()? true : false;
        } else {
            // -- explicit: use specified ssl mode
            useSSL = StringTools.parseBoolean(useSSLStr, false);
        }
        return useSSL;
    }

    /**
    *** Returns the Leaflet JavaScript URL
    **/
    public String[] _getLeafletJS(RequestProperties reqState)
    {
        boolean useSSL = this._useSSL(reqState);
        MapProvider mp = reqState.getMapProvider();
        RTProperties mrtp = (mp != null)? mp.getProperties() : null;
        // --
        String jsURLs[] = (mrtp != null)? StringTools.parseStringArray(mrtp.getString(PROP_js,""),'\n') : null;
        if (ListTools.isEmpty(jsURLs) || StringTools.isBlank(jsURLs[0])) {
            jsURLs = new String[] { (useSSL? HTTPS_ : HTTP_) + LEAFLET_JS };
        }
        // --
        String auth = this.getAuthorization();
        for (int i = 0; i < jsURLs.length; i++) {
            jsURLs[i] = StringTools.replace(jsURLs[i], "${key}", auth); // may not be necessary
            if (useSSL && !StringTools.startsWithIgnoreCase(jsURLs[i],"https")) {
                Print.logWarn("SSL indicated, but URL does not specified 'https': " + jsURLs[i]);
            }
        }
        return jsURLs;
    }

    /**
    *** Returns the Leaflet CSS URL
    **/
    public String[] _getLeafletCSS(RequestProperties reqState)
    {
        boolean useSSL = this._useSSL(reqState);
        MapProvider mp = reqState.getMapProvider();
        RTProperties mrtp = (mp != null)? mp.getProperties() : null;
        // --
        String cssURLs[] = (mrtp != null)? StringTools.parseStringArray(mrtp.getString(PROP_css,""),'\n') : null;
        if (ListTools.isEmpty(cssURLs) || StringTools.isBlank(cssURLs[0])) {
            cssURLs = new String[] { (useSSL? HTTPS_ : HTTP_) + LEAFLET_CSS };
        }
        // --
        for (int i = 0; i < cssURLs.length; i++) {
            if (useSSL && !StringTools.startsWithIgnoreCase(cssURLs[i],"https")) {
                Print.logWarn("SSL indicated, but URL does not specified 'https': " + cssURLs[i]);
            }
        }
        return cssURLs;
    }

    // ------------------------------------------------------------------------

    /* write css to stream */
    public void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        super.writeStyle(out, reqState);
        String cssURLs[] = this._getLeafletCSS(reqState);
        for (int i = 0; i < cssURLs.length; i++) {
            //WebPageAdaptor.writeCssLink(out, reqState, cssURLs[i], null);
        }
    }

    // ------------------------------------------------------------------------


    /**
    *** Writes the map table view to the http output stream
    *** @param out      The http output stream
    *** @param reqState The current session state
    *** @param mapDim   The specified map dimensions
    **/
    public void writeMapCell(PrintWriter out, RequestProperties reqState, MapDimension mapDim)
        throws IOException
    {
        super.writeMapCell(out, reqState, mapDim);
    }

    // ------------------------------------------------------------------------

    /* parse subdomains */
    protected Object _parseSubdomains(String subdom)
    {
        if (StringTools.isBlank(subdom)) {
            return "abc"; // default
        } else
        if (subdom.indexOf(",") < 0) {
            return subdom;
        } else {
            return StringTools.split(subdom,','); // { "1", "2", "3" }
        }
    }

    /* write mapping support JS to stream */
    protected void writeJSVariables(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        PrivateLabel privLabel   = reqState.getPrivateLabel();
        I18N         i18n        = privLabel.getI18N(Leaflet.class);
        Locale       locale      = reqState.getLocale();
        RTProperties rtp         = this.getProperties();

        /* Leaflet vars */
        out.write("// --- Leaflet specific vars ["+this.getName()+"]\n");
        // -- Leaflet properties
        JavaScriptTools.writeJSVar(out, "LEAFLET_ATTRIBUTION_PREFIX" , rtp.getString(PROP_attributionPrefix,""));
        JavaScriptTools.writeJSVar(out, "LEAFLET_ADD_OPEN_STREET_MAP", rtp.getBoolean(PROP_addOpenStreetMap,true));
        JavaScriptTools.writeJSVar(out, "LEAFLET_ADD_OPEN_CYCLE_MAP" , rtp.getBoolean(PROP_addOpenCycleMap ,false));
      //JavaScriptTools.writeJSVar(out, "LEAFLET_ADD_ONEMAP_SLA_MAP" , rtp.getBoolean(PROP_addOneMapSLAMap ,false));
        // -- Custom Map #1
        {
            String customName = rtp.getString(PROP_customMap1_Name,"");
            String customURL  = rtp.getString(PROP_customMap1_URL,"");
            Object subdomains = this._parseSubdomains(rtp.getString(PROP_customMap1_Subdomains,""));
            String authID     = rtp.getString(PROP_customMap1_AuthID,"");
            String authCode   = rtp.getString(PROP_customMap1_AuthCode,"");
            customURL = StringTools.replace(customURL, "{authID}"  , authID);
            customURL = StringTools.replace(customURL, "{authCode}", authCode);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM1_NAME"       , customName);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM1_URL"        , customURL);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM1_SUBDOMAINS" , subdomains);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM1_COPYRIGHT"  , rtp.getString(PROP_customMap1_Copyright,""));
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM1_MAX_ZOOM"   , rtp.getInt(PROP_customMap1_MaxZoom,19));
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM1_INIT_FTN"   , rtp.getString(PROP_customMap1_InitFTN,""),false);
        }
        // -- Custom Map #2
        {
            String customName = rtp.getString(PROP_customMap2_Name,"");
            String customURL  = rtp.getString(PROP_customMap2_URL,"");
            Object subdomains = this._parseSubdomains(rtp.getString(PROP_customMap2_Subdomains,""));
            String authID     = rtp.getString(PROP_customMap2_AuthID,"");
            String authCode   = rtp.getString(PROP_customMap2_AuthCode,"");
            customURL = StringTools.replace(customURL, "{authID}"  , authID);
            customURL = StringTools.replace(customURL, "{authCode}", authCode);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM2_NAME"       , customName);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM2_URL"        , customURL);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM2_SUBDOMAINS" , subdomains);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM2_COPYRIGHT"  , rtp.getString(PROP_customMap2_Copyright,""));
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM2_MAX_ZOOM"   , rtp.getInt(PROP_customMap2_MaxZoom,19));
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM2_INIT_FTN"   , rtp.getString(PROP_customMap2_InitFTN,""),false);
        }
        // -- Custom Map #3
        {
            String customName = rtp.getString(PROP_customMap3_Name,"");
            String customURL  = rtp.getString(PROP_customMap3_URL,"");
            Object subdomains = this._parseSubdomains(rtp.getString(PROP_customMap3_Subdomains,""));
            String authID     = rtp.getString(PROP_customMap3_AuthID,"");
            String authCode   = rtp.getString(PROP_customMap3_AuthCode,"");
            customURL = StringTools.replace(customURL, "{authID}"  , authID);
            customURL = StringTools.replace(customURL, "{authCode}", authCode);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM3_NAME"       , customName);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM3_URL"        , customURL);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM3_SUBDOMAINS" , subdomains);
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM3_COPYRIGHT"  , rtp.getString(PROP_customMap3_Copyright,""));
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM3_MAX_ZOOM"   , rtp.getInt(PROP_customMap3_MaxZoom,19));
            JavaScriptTools.writeJSVar(out, "LEAFLET_CUSTOM3_INIT_FTN"   , rtp.getString(PROP_customMap3_InitFTN,""),false);
        }

        /* general JS vars */
        super.writeJSVariables(out, reqState);

    }

    // ------------------------------------------------------------------------

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        Vector<String> jsURLs = new Vector<String>();
        jsURLs.add(JavaScriptTools.qualifyJSFileRef("https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/js/maps/jsmap.js"));
        ListTools.toList(this._getLeafletJS(reqState), jsURLs);
        jsURLs.add(JavaScriptTools.qualifyJSFileRef("https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/js/maps/LeafletV1_0_0.js"));
        super.writeJSIncludes(out, reqState, jsURLs.toArray(new String[jsURLs.size()]));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the number of supported points for the specified Geozone type
    *** @param type  The Geozone type
    *** @return The number of supported points for the specified Geozone type
    **/
    public int getGeozoneSupportedPointCount(int type)
    {

        /* Geozone type supported? */
        Geozone.GeozoneType gzType = Geozone.getGeozoneType(type);
        if (!Geozone.IsGeozoneTypeSupported(gzType)) {
            Print.logWarn("Geozone type not supported: " + type);
            return 0;
        }

        /* return supported point count */
        int ptCnt = 0;
        RTProperties rtp = this.getProperties();
        switch (gzType) {
            case POINT_RADIUS        : // 0
                ptCnt = rtp.getBoolean(PROP_zone_map_multipoint,false)? Geozone.GetMaxVerticesCount() : 1;
                break;
            case BOUNDED_RECT        : // 1
                return 0; // not yet supported
            case SWEPT_POINT_RADIUS  : // 2
                ptCnt = rtp.getBoolean(PROP_zone_map_corridor  ,false)? Geozone.GetMaxVerticesCount() : 0;
                break;
            case POLYGON             : // 3
                ptCnt = rtp.getBoolean(PROP_zone_map_polygon   ,false)? Geozone.GetMaxVerticesCount() : 0;
                break;
        }
        if (ptCnt <= 0) {
            Print.logWarn("Geozone type not enabled: " + type);
        }
        return ptCnt;

    }

    public String[] getGeozoneInstructions(int type, Locale loc)
    {
        I18N i18n = I18N.getI18N(Leaflet.class, loc);
        if (type == Geozone.GeozoneType.POINT_RADIUS.getIntValue()) {
            return new String[] {
                i18n.getString("Leaflet.geozoneNotes.1", "Click to reset center."),
                i18n.getString("Leaflet.geozoneNotes.2", "Click-drag Geozone to move."),
              //i18n.getString("Leaflet.geozoneNotes.3", "Shift-click-drag to resize."),
              //i18n.getString("Leaflet.geozoneNotes.4", "Ctrl-click-drag for distance."),
            };
        } else
        if (type == Geozone.GeozoneType.POLYGON.getIntValue()) {
            return new String[] {
                i18n.getString("Leaflet.geozoneNotes.1", "Click to reset center."),
                i18n.getString("Leaflet.geozoneNotes.5", "Click-drag corner to resize."),
              //i18n.getString("Leaflet.geozoneNotes.4", "Ctrl-click-drag for distance."),
            };
        } else {
            return new String[0];
        }
    }

    // ------------------------------------------------------------------------

}
