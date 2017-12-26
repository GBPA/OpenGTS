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
//  - http://wiki.openstreetmap.org/index.php/OpenLayers_Simple_Example
//  - http://wiki.openstreetmap.org/index.php/Kosmos
//  - http://www.openlayers.org/
//  - http://www.openlayers.org/dev/examples/
//  - http://cfis.savagexi.com/articles/2007/09/29/can-open-source-dethrone-google-maps
//  - http://www.developpez.net/forums/showthread.php?t=533436
// Polygons:
//  - http://dev.openlayers.org/sandbox/tschaub/feature/examples/regular-polygons.html
// Dragging a feature:
//  - http://dev.openlayers.org/sandbox/tschaub/feature/examples/drag-marker.html
//  - http://dev.openlayers.org/sandbox/tschaub/feature/examples/drag-feature.html
// Resizing a feature:
//  - http://dev.openlayers.org/sandbox/tschaub/feature/examples/resize-features.html
// Using with Virtual Earth:
//  - http://www.mp2kmag.com/a147--open.layers.mappoint.html
// Zooming
//  - http://www.cartogrammar.com/blog/map-panning-and-zooming-methods/
// ----------------------------------------------------------------------------
// Change History:
//  2008/08/08  Martin D. Flynn
//     -Initial release
//     -Includes Geozone support
//  2008/10/16  Martin D. Flynn
//     -Initial support for GeoServer
//  2010/04/11  Martin D. Flynn
//     -Added support for drawing polygon and corridor geozones.  However, in the
//      case of corridor geozones, additional GTS features may be required to
//      fully utilize this type of geozone.
//  2012/04/03  Martin D. Flynn
//     -Updated map layer creation interface
//     -Added support for MapQuest OSM/Aerial layers
//  2013/08/06  Martin D. Flynn
//     -Fixed spelling, renamed "Arial" to "Aerial"
//  2016/09/01  Martin D. Flynn
//     -Added support for PROP_useSSL.
//  2017/06/16 Martin D. Flynn
//     -Added support for OpenSeaMap (see PROP_OPENSEAMAP_showOverlay) [EXPERIMENTAL/incomplete]
// ----------------------------------------------------------------------------
package org.opengts.war.maps.jsmap;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

import org.opengts.db.tables.Geozone;
import org.opengts.war.tools.*;
import org.opengts.war.maps.JSMap;

public class OpenLayers
    extends JSMap
{

    // ------------------------------------------------------------------------
    // http://www.openlayers.org/api/OpenLayers.js
    // http://www.openstreetmap.org/openlayers/OpenStreetMap.js
    //
    /* OpenLayers JavaScript URL */
    private static final String OPENLAYERS_2_JS_URL             = "https://openlayers.org/api/OpenLayers.js";
    private static final String OPENLAYERS_2_JS_URL_SSL         = "https://openlayers.org/api/OpenLayers.js";

    /* OpenLayers 3 */
    private static final String OPENLAYERS_4_JS_URL             = "https://openlayers.org/en/v4.1.1/build/ol.js";
    private static final String OPENLAYERS_4_JS_URL_SSL         = "https://openlayers.org/en/v4.1.1/build/ol.js";

    // ------------------------------------------------------------------------

    /* OpenLayers support properties */
    private static final String PROP_mapcontrol[]               = new String[] { "openlayers.mapcontrol", "openlayers.js" };
    private static final String PROP_useSSL[]                   = new String[] { "openlayers.useSSL"          };

    /* SitiMapa support properties */
    public  static final String PROP_SITIMAPA_enable[]          = new String[] { "sitiMapa.enable"            };

    /* GeoServer support properties */
    public  static final String PROP_GEOSERVER_enable[]         = new String[] { "geoServer.enable"           };
    public  static final String PROP_GEOSERVER_title[]          = new String[] { "geoServer.title"            };
    public  static final String PROP_GEOSERVER_url[]            = new String[] { "geoServer.url"              };
    public  static final String PROP_GEOSERVER_maxResolution[]  = new String[] { "geoServer.maxResolution"    };
    public  static final String PROP_GEOSERVER_size[]           = new String[] { "geoServer.size"             };
    public  static final String PROP_GEOSERVER_projection[]     = new String[] { "geoServer.projection"       };
    public  static final String PROP_GEOSERVER_layers[]         = new String[] { "geoServer.layers"           };
    public  static final String PROP_GEOSERVER_bounds[]         = new String[] { "geoServer.bounds"           };
    public  static final String PROP_GEOSERVER_units[]          = new String[] { "geoServer.units"            };
    public  static final String PROP_GEOSERVER_layerType[]      = new String[] { "geoServer.layerType"        };

    /* Mapquest layers (as of 2016/07/11, no longer supported by Mapquest) */
    public  static final String PROP_MAPQUEST_showOSMLayer[]    = new String[] { "mapQuest.enableLayer.osm"   };
    public  static final String PROP_MAPQUEST_showAerialLayer[] = new String[] { "mapQuest.enableLayer.aerial", "mapQuest.enableLayer.arial" };

    /* Nokia-Here layers */
    public  static final String PROP_NOKIAHERE_showLayer[]      = new String[] { "nokiaHere.enableLayer"      };
    public  static final String PROP_NOKIAHERE_appID[]          = new String[] { "nokiaHere.appID"            };
    public  static final String PROP_NOKIAHERE_appCode[]        = new String[] { "nokiaHere.appCode", "nokiaHere.token" };

    /* OpenSeaMap overlay */
    public  static final String PROP_OPENSEAMAP_showOverlay[]   = new String[] { "openSeaMap.showOverlay"     };

    // ------------------------------------------------------------------------

    private static final int   DEFAULT_ZOOM             = 4;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* OpenLayers instance */
    public OpenLayers(String name, String key)
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

    /* write mapping support JS to stream */
    protected void writeJSVariables(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        super.writeJSVariables(out, reqState);
        out.write("// OpenLayers custom vars\n");
        RTProperties rtp = this.getProperties();
        // -- GeoServer support
        boolean gsEnable = rtp.getBoolean(PROP_GEOSERVER_enable, false);
        JavaScriptTools.writeJSVar(out, "GEOSERVER_enable", gsEnable);
        if (gsEnable) {
            String gsTitle  = StringTools.blankDefault(rtp.getString(PROP_GEOSERVER_title,null), "WMS Provider");
            String gsURL    = rtp.getString(PROP_GEOSERVER_url, "");
            String gsMaxRes = StringTools.blankDefault(rtp.getString(PROP_GEOSERVER_maxResolution,null), "auto");
            String gsSize   = StringTools.blankDefault(rtp.getString(PROP_GEOSERVER_size,null), "{ width:0, height:0 }");
            String gsProj   = StringTools.blankDefault(rtp.getString(PROP_GEOSERVER_projection,null), "EPSG:4326");
            String gsLayers = rtp.getString(PROP_GEOSERVER_layers, "");
            String gsBounds = StringTools.blankDefault(rtp.getString(PROP_GEOSERVER_bounds,null), "{ top:0, left:0, bottom:0, right:0 }");
            String gsUnits  = StringTools.blankDefault(rtp.getString(PROP_GEOSERVER_units,null), "degrees");
            String gsLayerT = StringTools.blankDefault(rtp.getString(PROP_GEOSERVER_layerType,null), "tiled");
            JavaScriptTools.writeJSVar(out, "GEOSERVER_title"        , gsTitle);
            JavaScriptTools.writeJSVar(out, "GEOSERVER_url"          , gsURL);
            JavaScriptTools.writeJSVar(out, "GEOSERVER_maxResolution", gsMaxRes, !StringTools.isDouble(gsMaxRes,true));
            JavaScriptTools.writeJSVar(out, "GEOSERVER_size"         , gsSize  , false);
            JavaScriptTools.writeJSVar(out, "GEOSERVER_projection"   , gsProj);
            JavaScriptTools.writeJSVar(out, "GEOSERVER_layers"       , gsLayers);
            JavaScriptTools.writeJSVar(out, "GEOSERVER_bounds"       , gsBounds, false);
            JavaScriptTools.writeJSVar(out, "GEOSERVER_units"        , gsUnits);
            JavaScriptTools.writeJSVar(out, "GEOSERVER_layerType"    , gsLayerT);
        }
        // -- SitiMapa
        boolean smEnable = !gsEnable? rtp.getBoolean(PROP_SITIMAPA_enable,false) : false;
        JavaScriptTools.writeJSVar(out, "SITIMAPA_enable", smEnable);
        // -- Mapquest support (Mapquest dropped support on 2016/07/11)
        boolean mqOsmLayer    = rtp.getBoolean(PROP_MAPQUEST_showOSMLayer   ,false);
        boolean mqAerialLayer = rtp.getBoolean(PROP_MAPQUEST_showAerialLayer,false);
        JavaScriptTools.writeJSVar(out, "MAPQUEST_showOSMLayer"   , mqOsmLayer);
        JavaScriptTools.writeJSVar(out, "MAPQUEST_showAerialLayer", mqAerialLayer);
        // -- Nokia-Here support
        boolean nhLayer   = rtp.getBoolean(PROP_NOKIAHERE_showLayer,false);
        String  nhAppID   = rtp.getString(PROP_NOKIAHERE_appID     ,"APP_ID");
        String  nhAppCode = rtp.getString(PROP_NOKIAHERE_appCode   ,"APP_CODE");
        JavaScriptTools.writeJSVar(out, "NOKIAHERE_showLayer", nhLayer);
        JavaScriptTools.writeJSVar(out, "NOKIAHERE_appID"    , nhAppID);
        JavaScriptTools.writeJSVar(out, "NOKIAHERE_appCode"  , nhAppCode);
        // -- OpenSeaMap overlay
        boolean seaOverlay = rtp.getBoolean(PROP_OPENSEAMAP_showOverlay,false);
        JavaScriptTools.writeJSVar(out, "OPENSEAMAP_showOverlay", seaOverlay);
    }

    // ------------------------------------------------------------------------

    protected void writeJSIncludes(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        MapProvider mp = reqState.getMapProvider();
        RTProperties mrtp = (mp != null)? mp.getProperties() : null;

        /* SSL? */
        String useSSLStr = (mrtp != null)? mrtp.getString(PROP_useSSL, null) : null;
        boolean useSSL = false;
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

        /* URL */
        String jsURL = (mrtp != null)? mrtp.getString(PROP_mapcontrol, null) : null;
        if (!StringTools.isBlank(jsURL)) {
            String authKey = this.getAuthorization();
            jsURL = StringTools.replace(jsURL, "${key}", authKey);
            if (useSSL && !StringTools.startsWithIgnoreCase(jsURL,"https")) {
                Print.logWarn("SSL indicated, but mapcontrol URL does not specified 'https'");
            }
        } else {
            jsURL = useSSL? OPENLAYERS_2_JS_URL_SSL : OPENLAYERS_2_JS_URL;
            String authKey = this.getAuthorization();
            jsURL = StringTools.replace(jsURL, "${key}", authKey);
        }

        /* write JavaScript */
        Vector<String> jsURLs = new Vector<String>();
        jsURLs.add(JavaScriptTools.qualifyJSFileRef("maps/jsmap.js"));
        jsURLs.add(jsURL);
        jsURLs.add(JavaScriptTools.qualifyJSFileRef("maps/OpenLayers.js"));
        if ((mrtp != null) && mrtp.getBoolean(PROP_OPENSEAMAP_showOverlay,false)) {
            jsURLs.add("http://map.openseamap.org/javascript/harbours.js");
            jsURLs.add("http://map.openseamap.org/javascript/map_utils.js");
            jsURLs.add("http://map.openseamap.org/javascript/utilities.js");
        }
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
                //Print.logInfo("Polygon supported: " + rtp.getBoolean(PROP_zone_map_polygon,false));
                //if (!rtp.getBoolean(PROP_zone_map_polygon,false)) {
                //    rtp.printProperties("OpenLayers Properties: ");
                //}
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
        I18N i18n = I18N.getI18N(OpenLayers.class, loc);
        if (type == Geozone.GeozoneType.POINT_RADIUS.getIntValue()) {
            return new String[] {
                i18n.getString("OpenLayers.geozoneNotes.1", "Click to reset center."),
                i18n.getString("OpenLayers.geozoneNotes.2", "Click-drag Geozone to move."),
                i18n.getString("OpenLayers.geozoneNotes.3", "Shift-click-drag to resize."),
                i18n.getString("OpenLayers.geozoneNotes.4", "Ctrl-click-drag for distance.")
            };
        } else
        if (type == Geozone.GeozoneType.POLYGON.getIntValue()) {
            return new String[] {
                i18n.getString("OpenLayers.geozoneNotes.1", "Click to reset center."),
                i18n.getString("OpenLayers.geozoneNotes.5", "Click-drag corner to resize."),
                i18n.getString("OpenLayers.geozoneNotes.4", "Ctrl-click-drag for distance.")
            };
        } else {
            return new String[0];
        }
    }

    // ------------------------------------------------------------------------

}
