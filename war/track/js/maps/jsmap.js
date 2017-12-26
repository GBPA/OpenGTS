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
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/08/08  Martin D. Flynn
//     -Added support for Geozones
//  2008/08/17  Martin D. Flynn
//     -Distance now always displayed, even if value falls to '0'.
//  2008/09/19  Martin D. Flynn
//     -Added warning message when MAX_PUSH_PINS has been reached.
//  2008/12/01  Martin D. Flynn
//     -Added support for optional event data fields
//     -Added JS code to highlite the details report row when the info balloon
//      is displayed for a specific pushpin.
//  2009/01/01  Martin D. Flynn
//     -Added option for displaying altitude in info-bubble.
//  2009/07/01  Martin D. Flynn
//     -Map points wrapped in XML Data/Set tags
//  2009/08/07  Martin D. Flynn
//     -Changed "Time" and "LastEvent" tag sections
//  2009/11/01  Martin D. Flynn
//     -Display all device detail records when showing multiple events per device
//      on group map.  Line breaks separate devices.
//  2010/07/04  Martin D. Flynn
//     -Added support for collapsible map controls
//  2011/05/13  Martin D. Flynn
//     -Fixed "evHeadingMarkerURL_eu" conversion of kilometers to miles
//  2011/06/16  Martin D. Flynn
//     -Added "showpp" Action to map XML load
//  2011/12/06  Martin D. Flynn
//     -Fixed XML format geozone shape display radius type
//     -Support JSON map update format
//  2012/02/03  Martin D. Flynn
//     -Make sure last pushpin is displayed for "showPushpins=false"
// ----------------------------------------------------------------------------
// External funtions:
//   new JSMap(Element mapElem)
//   JSClearLayers()
//   JSSetCenter(JSMapPoint center)
//   JSDrawPushpins(JSMapPushpin pushPin[], int recenterMode, int replay)
//   JSDrawPOI(JSMapPushpin pushPin[])
//   JSDrawRoute(JSMapPoint points[], String color)
//   JSDrawGeozone(int type, double radius, JSMapPoint points[], String color, int primaryIndex)
//   JSShowPushpin(JSMapPushpin pushPin, boolean center)
//   JSPauseReplay(int replay)
//   JSUnload()
// ----------------------------------------------------------------------------

/* these must match the data response values in "Track.java" */
var DATA_RESPONSE_LOGOUT        = "LOGOUT";
var DATA_RESPONSE_ERROR         = "ERROR";
var DATA_RESPONSE_PING_OK       = "PING:OK";
var DATA_RESPONSE_PING_ERROR    = "PING:ERROR";

/* these must match the class definitions in "Controls.css" */
var CLASS_DETAILS_DIV           = "trackMapDetailLocation";
var CLASS_DETAILS_TABLE_N       = "mapDetailsTable";                // TABLE
var CLASS_DETAILS_TABLE_S       = "mapDetailsTable_sortable";       // TABLE
var CLASS_DETAILS_HEADER_ROW    = "mapDetailsHeaderRow";            // TR
var CLASS_DETAILS_HEADER_COL_N  = "mapDetailsHeaderColumn_nosort";  // TH
var CLASS_DETAILS_HEADER_COL_S  = "mapDetailsHeaderColumn_sort";    // TH
var CLASS_DETAILS_ROW_HILITE    = "mapDetailsDataRowHiLite";
var CLASS_DETAILS_ROW_ODD       = "mapDetailsDataRowOdd";
var CLASS_DETAILS_ROW_EVEN      = "mapDetailsDataRowEven";
var CLASS_DETAILS_INDEX_COL     = "mapDetailsIndexColumn";
var CLASS_DETAILS_DATA_COL_NEW  = "mapDetailsDataColumn_new";
var CLASS_DETAILS_DATA_COL      = "mapDetailsDataColumn";

var ID_DETAIL_ROW_              = "detailRow_";

var DETAILS_WINDOW              = false; // not fully implemented
var jsvDetailsWindow            = null;
var jsvDetailsLastHilightedRow  = null;
var jsvUseDeviceBreaks          = false;

/* replay state */
var REPLAY_STOPPED              = 0;
var REPLAY_PAUSED               = 1;
var REPLAY_RUNNING              = 2;

/* recenter modes (recenterMode) */
var RECENTER_NONE               = 0; // don't change current zoom
var RECENTER_LAST               = 1; // center/zoom on last point
var RECENTER_ZOOM               = 2; // normal center zoom on all points
var RECENTER_PAN                = 3; // pan to last point

/* Geozone types */
var ZONE_POINT_RADIUS           = 0;
var ZONE_BOUNDED_RECT           = 1; // not yet supported
var ZONE_SWEPT_POINT_RADIUS     = 2; // not supported
var ZONE_POLYGON                = 3; // not yet supported

/* JSON MapData tags */
var JSON_JMapData               = "JMapData";   // top-level tag
var JSON_Time                   = "Time";       // update time (server time)
var JSON_LastEvent              = "LastEvent";  // last event time for current device
var JSON_DataColumns            = "DataColumns";
var JSON_Data                   = "Data";
var JSON_Shapes                 = "Shapes";     // map shape array
var JSON_DataSets               = "DataSets";   // map point datasets array
var JSON_Points                 = "Points";     // CSV data record array
var JSON_Actions                = "Actions";    // actions array
var JSON_cmd                    = "cmd";        // action to perform ("autoupdate", "alert", "gotourl", etc)
var JSON_arg                    = "arg";        // action command argument

/* XML MapData tags */
var TAG_MapData                 = "MapData";    // top-level tag
var TAG_Action                  = "Action";     // action to perform ("autoupdate", "alert", "gotourl", etc)
var TAG_Time                    = "Time";       // update time (server time)
var TAG_LastEvent               = "LastEvent";  // last event time for current device
var TAG_DataSet                 = "DataSet";    // map point datasets
var TAG_Point                   = "P";          // CSV data record
var TAG_Shape                   = "Shape";      // CSV data record
var TAG_Geozone                 = "Geozone";    // Geozone [attr: type, radius]

/* XML/JSON attributes */
var ATTR_isFleet                = "isFleet";
var ATTR_type                   = "type";
var ATTR_routeColor             = "routeColor";
var ATTR_textColor              = "textColor";
var ATTR_color                  = "color";
var ATTR_desc                   = "desc";
var ATTR_ppNdx                  = "ppNdx";
var ATTR_id                     = "id";
var ATTR_route                  = "route";
var ATTR_timestamp              = "timestamp";
var ATTR_timezone               = "timezone";
var ATTR_year                   = "year";
var ATTR_month                  = "month";
var ATTR_day                    = "day";
var ATTR_command                = "command";
var ATTR_radius                 = "radius";
var ATTR_battery                = "battery";
var ATTR_signal                 = "signal";

/* partial data */
var jsvPartialData              = false;

/* jsmap image base dir */
var jsvImageBaseDir             = ".";

/* fixed zoom mode */
var jsvFixedZoom                = false;

/* Latitude/Longitude format Deg:Min decimal places. */
var LATLON_FORMAT_MIN_DEC       = 2; // 2=20'34.12",  3=20'34.123"

/* Device display color type (0=off, 1=foreground, 2=background) */
var DISPLAY_COLOR_TYPE          = 1;

/* Route color NONE */
var ROUTE_COLOR_NONE            = "none";

/* "evDeviceNameIconURL" Marker font point size */
var TEXT_LABEL_FONT_SIZE        = "11";

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSMapPoint

/**
*** Constructor: Creates a GeoPoint
*** @param lat  The latitude  (decimal degrees)
*** @param lon  The longitude (decimal degrees)
**/
function JSMapPoint(lat, lon)
{
    this.lat = lat;
    this.lon = lon;
};

//JSMapPoint.prototype.isValid = funtion()
//{
//    return ((this.lat != 0.0) || (this.lon != 0.0))? true : false;
//};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSMapShape

function JSMapShape(type, radiusM, points, color, zoomTo, desc, ppNdx)
{
    this.type   = type;     // string
    this.radius = radiusM;  // radius meters
    this.points = points;   // array of points
    this.color  = color;    // color
    this.zoomTo = zoomTo;   // boolean
    this.desc   = desc;     // string
    this.ppNdx  = ppNdx;    // int
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSBounds

/**
*** Constructor: Creates a Bounds object
*** (Note: will not work if the bounds span 180/-180)
**/
function JSBounds()
{
    this.maxLat =  -90.0; // North
    this.maxLon = -180.0; // East
    this.minLat =   90.0; // South
    this.minLon =  180.0; // West
};

/**
*** Extends the bound to include the specified JSMapPoint
*** @param pt  The JSMapPoint
**/
JSBounds.prototype.extend = function(pt)
{
    if (pt != null) {
        this.extendLat(pt.lat);
        this.extendLon(pt.lon);
    }
};

/**
*** Extends the bound to include the specified Latitude
*** @param lat  The Latitude
**/
JSBounds.prototype.extendLat = function(lat)
{
    if (lat > this.maxLat) { this.maxLat = lat; }
    if (lat < this.minLat) { this.minLat = lat; }
};

/**
*** Extends the bound to include the specified Longitude
*** @param lat  The Longitude
**/
JSBounds.prototype.extendLon = function(lon)
{
    if (lon > this.maxLon) { this.maxLon = lon; }
    if (lon < this.minLon) { this.minLon = lon; }
};

/**
*** Gets the SouthWest point
*** @return The SouthWest point
**/
JSBounds.prototype.getSouthWest = function()
{
    return new JSMapPoint(this.minLat,this.minLon);
};

/**
*** Gets the NorthEast point
*** @return The NorthEast point
**/
JSBounds.prototype.getNorthEast = function()
{
    return new JSMapPoint(this.maxLat,this.maxLon);
};

/**
*** Gets the center of the bounds
*** @return The center JSMapPoint
**/
JSBounds.prototype.getCenter = function()
{
    return new JSMapPoint((this.minLat + this.maxLat) / 2.0, (this.minLon + this.maxLon) / 2.0);
};

/**
*** Gets the width of the bounds
*** @return The bounds width (ie. delta longitude)
**/
JSBounds.prototype.getWidth = function()
{
    return this.maxLon - this.minLon;
};

/**
*** Gets the width of the bounds (in delta meters)
*** @return The bounds width (ie. delta meters)
**/
JSBounds.prototype.getWidthMeters = function()
{
    var lat = this.minLat;
    return geoDistanceMeters(lat, this.minLon, lat, this.maxLon);
};

/**
*** Gets the height of the bounds
*** @return The bounds height (ie. delta latitude)
**/
JSBounds.prototype.getHeight = function()
{
    return this.maxLat - this.minLat;
};

/**
*** Gets the height of the bounds (in delta meters)
*** @return The bounds height (ie. delta meters)
**/
JSBounds.prototype.getHeightMeters = function()
{
    var lon = this.minLon;
    return geoDistanceMeters(this.minLat, lon, this.maxLat, lon);
};

/**
*** Calculates the best zoom for this bounds (in meters per pixel)
*** @param viewWidth  The map width in pixels
*** @param viewHeight The map height in pixels
**/
JSBounds.prototype.calculateMetersPerPixel = function(viewWidth, viewHeight)
{
    var mppW = this.getWidthMeters()  / viewWidth;
    var mppH = this.getHeightMeters() / viewHeight;
    return (mppW > mppH)? mppW : mppH;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSMapPushpin

/**
*** Constructor: Creates a JSMapPushpin
**/
function JSMapPushpin(rcdNdx, dsNdx, ppNdx, evRcd,
    lat, lon,
    label, html,
    ppIcon)
    //iconURL, iconSize, iconHotspot,
    //shadowURL, shadowSize)
{

    /* detail record index */
    this.rcdNdx      = rcdNdx;  // record index
    this.dsNdx       = dsNdx;   // dataset index
    this.ppNdx       = ppNdx;   // pushpin index
    this.evRcd       = evRcd;   // event record
    this.show        = true;

    /* latitude/longitude */
    this.lat         = lat;
    this.lon         = lon;
    this.accRadM     = 0;
    this.isCellLoc   = false;
    if (this.evRcd != null) {
        this.accRadM   = this.evRcd.accuracy;
        this.isCellLoc = this.evRcd.isCellLoc;
        if (this.isCellLoc && (this.accRadM < 100)) {
            this.accRadM = 100;
        }
    }

    /* displayed information */
    this.label       = label;
    this.html        = html;    // may be null

    /* icon attributes */
    this.iconUrl     = ppIcon.iconURL;     //*/ iconURL;
    this.iconSize    = ppIcon.iconSize;    //*/ iconSize;
    this.iconHotspot = ppIcon.iconHotspot; //*/ iconHotspot;
    this.shadowUrl   = ppIcon.shadowURL;   //*/ shadowURL;
    this.shadowSize  = ppIcon.shadowSize;  //*/ shadowSize;

    /* accuracy radius shape */
    this.accRadius   = null;

    /* background icon attribute */
    var  hasBG       = ppIcon.bgURL? true : false;
    this.bgUrl       = hasBG? ppIcon.bgURL    : null; // "extra/images/pp/CrosshairRed.gif";
    this.bgSize      = hasBG? ppIcon.bgSize   : null; // [32, 32];
    this.bgOffset    = hasBG? ppIcon.bgOffset : null; // [16, 16];
    this.bgMarker    = null;

    /* popup attributes */
    this.map         = null;
    this.marker      = null;
    this.hoverPopup  = false;
    this.popup       = null;
    this.popupShown  = false;

};

JSMapPushpin.prototype.getHTML = function()
{

    /* already initialized */
    if (this.html != null) {
        //alert("HTML already defined ...");
        return this.html;
    }

    /* no event record */
    if (this.evRcd == null) {
        alert("No Event ...");
        this.html = "";
        return this.html;
    }

    /* initialize */
    var evRcd   = this.evRcd;
    var ndx     = this.rcdNdx;
    var dev     = evRcd.device;
    var dtime   = evRcd.dateFmt + ' ' + evRcd.timeFmt;
    var tmz     = evRcd.timeZone; // _tmz
    var accRadM = evRcd.accuracy; // meters
    var flat    = jsmFormatCoord(this.lat,true ,5);
    var flon    = jsmFormatCoord(this.lon,false,5);
    var spdfmt  = numFormatFloat(evRcd.speedKPH * SPEED_KPH_MULT, 1) + " " + SPEED_UNITS;
    var addr    = evRcd.address;
    var icon    = evRcd.iconNdx;
    var code    = evRcd.code;
    var sats    = evRcd.satCount;
    var stopSec = evRcd.stopSec;

    /* extra GPS location information */
    var gpsStr  = "";
    if (sats > 0) {
        gpsStr += TEXT_INFO_SATS + " " + sats;
    }
    if (accRadM > 0) {
        if (gpsStr != "") { gpsStr += ", "; }
        gpsStr += "+/- " + numFormatFloat(accRadM * ALTITUDE_METERS_MULT,0) + " " + ALTITUDE_UNITS;
    }
    if (gpsStr != "") {
        gpsStr = "[" + gpsStr + "]";
    }

    /* balloon text */
    var h = "";
    //h += "<div style='width:300px'>";
    h += "<table class='infoBoxTable table' cellspacing='1' cellpadding='1' border='0'>";
    h += "<tr class='infoBoxRow'><td class='infoBoxCell'>[#"+ndx+"] &nbsp; <b>"+dev+" : "+code+"</b></td></tr>";
    h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_DATE   +":</b> "+dtime+" ["+tmz+"]</td></tr>";
    h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_GPS    +":</b> "+flat+" / "+flon+" "+gpsStr+"</td></tr>";
    if (SHOW_SPEED) {
        if (COMBINE_SPEED_HEAD) {
            if (evRcd.speedKPH > 0) {
                h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_SPEED  +":</b> "+spdfmt+" &nbsp;("+evRcd.compass+")</td></tr>";
            } else {
                h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_SPEED  +":</b> "+spdfmt+"</td></tr>";
            }
        } else {
            h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_SPEED  +":</b> "+spdfmt+"</td></tr>";
            if (evRcd.speedKPH > 0) {
                var head = numFormatFloat(evRcd.heading,0) + "&deg; &nbsp;(" + evRcd.compass + ")";
                h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_HEADING+":</b> "+head+"</td></tr>";
            }
        }
    }
    if (stopSec > 0) {
        // 42d 02:08:56
        // 42d 02:08
        // 42d 02h 08m 56s
        // 42d 02h 08m
        // 42d 2.5h
        var stopFmt = "";
        var ss = stopSec;
        // seconds
        var S = ss % 60;
        //stopFmt = S + "s" + stopFmt;
        //if (S <= 9) { stopFmt = "0" + stopFmt; }
        ss = Math.floor(ss/60);
        // minutes
        var M = ss % 60;
        stopFmt = M + "m " + stopFmt;
        if (M <= 9) { stopFmt = "0" + stopFmt; }
        ss = Math.floor(ss/60);
        if (ss > 0) {
            // hours
            var H = ss % 24;
            stopFmt = H + "h " + stopFmt;
            if (H <= 9) { stopFmt = "0" + stopFmt; }
            ss = Math.floor(ss/24);
            if (ss > 0) {
                // days
                var D = ss;
                stopFmt = D + "d " + stopFmt;
            }
        }
        //var stopFmt = numFormatFloat((stopSec / 60.0),1);
        h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_STOP_TIME+":</b> "+stopFmt+"</td></tr>";
    }
    if (SHOW_ALTITUDE) {
        var altfmt = numFormatFloat((evRcd.altitude * ALTITUDE_METERS_MULT),0) + " " + ALTITUDE_UNITS;
        h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_ALTITUDE +":</b> "+altfmt+"</td></tr>";
    }
    if (/*SHOW_ADDR && */((addr != "") || INCL_BLANK_ADDR)) {
        if (addr == "") { addr = '&nbsp;'; }
        h += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_ADDR+":</b> "+addr+"</td></tr>";
    }
    if (SHOW_OPT_FIELDS) {
        if (evRcd.optDesc && (evRcd.optDesc.length > 0)) {
            for (var i = 0; i < evRcd.optDesc.length; i++) {
                var v = evRcd.optDesc[i];
                if (INCL_BLANK_OPT_FIELDS || (v != "")) {
                    var d = OptionalEventFieldTitle(i);
                    var r = (d && (d != ""))?  ("<b>"+d+":</b> "+v) : v;
                    h += "<tr class='infoBoxRow'><td class='infoBoxCell'>"+r+"</td></tr>";
                }
            }
        }
    }
    h += "</table>";
    //h += "</div>";
    h += "<script type='text/javascript'> jsmHighlightDetailRow("+ndx+",true); </script>\n";
    this.html = h;
    //alert("Initialized InfoBalloon HTML");
    return this.html;

};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSMapDataSet

/**
*** Constructor: Creates a JSMapDataSet
**/
function JSMapDataSet(pushPins, routePoints, routeColor, partial)
{
    this.pushPins    = pushPins;    // device pushpins
    this.routePoints = routePoints; // route line
    this.routeColor  = routeColor;
    this.partial     = partial;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSDetailPoint

/**
*** Constructor: Creates a JSDetailPoint
**/
function JSDetailPoint(rcdNdx, dsNdx, ppNdx, evRcd, textColor)
{
    this.device   = evRcd.device;
    this.latlon   = jsmFormatCoord(evRcd.latitude,true,4) + "/" + jsmFormatCoord(evRcd.longitude,false,4); // AA.AAAA/-NNN.NNNN
    this.satCount = evRcd.satCount;
    this.dsNdx    = dsNdx;                                              // dataset index
    this.ppNdx    = ppNdx;                                              // XX [1+, -1 if no pushpin]
    this.index    = rcdNdx;                                             // XX [1+]
    this.code     = evRcd.code;                                         // A...A
    this.timestamp= evRcd.timestamp;                                    // ttttttttt
    this.dateTime = evRcd.dateFmt + ' ' + evRcd.timeFmt;                // YYYY/MM/DD HH:MM:SS
    this.timeZone = evRcd.timeZone; // tmz;                             // US/Pacific
    this.speed    = numFormatFloat(evRcd.speedKPH * SPEED_KPH_MULT, 1); // SS
    this.heading  = numFormatFloat(evRcd.heading, 0);                   // SS
    this.compass  = evRcd.compass;                                      // NE
    this.altitude = evRcd.altitude;
    this.address  = evRcd.address;
    this.optDesc  = evRcd.optDesc;
    this.color    = textColor;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- map initialization

/**
*** Initializes the map
**/
function jsMapInit()
{
    if (jsmap == null) {
        jsmapElem = document.getElementById(MAP_ID);
        if (jsmapElem != null) {
            try {
                jsmap = new JSMap(jsmapElem);
                if (jsmap) {
                    jsmap.JSClearLayers();
                    jsmap.JSDrawPushpins(null, RECENTER_ZOOM, 0);  // TODO: check "jsvGeozoneMode"?
                } else {
                    // seems to be necessary on IE (it doesn't throw an exception)
                    alert(
                        "[jsMapInit]: " + MAP_PROVIDER_NAME + "\n" +
                        "Error occured while creating JSMap" +
                        "(map provider service may be temporarily unavailable)"
                        );
                }
            } catch (e) {
                alert(
                    "[jsMapInit]: " + MAP_PROVIDER_NAME + "\n" +
                    "Error initializing map\n" +
                    "(map provider service may be temporarily unavailable)\n" +
                    e
                    );
            }
        } else {
            alert(
                "[jsMapInit]: " + MAP_PROVIDER_NAME + "\n" +
                "Div '" + MAP_ID + "' not found"
                );
        }
    }
};

// ----------------------------------------------------------------------------
// --- unload

/**
*** Releases any map resources
**/
function jsmUnload()
{
    if (!jsmap) { return; }
    try {
        jsmap.JSUnload();
    } catch (e) {
        // quietly ignore "unload" errors
    }
};

// ----------------------------------------------------------------------------
// fixed zoom mode

function jsmSetFixedZoom(fixedZoom)
{
    jsvFixedZoom = fixedZoom; // true|false
};

function jsmRecenterZoomMode(mode)
{
    return jsvFixedZoom? RECENTER_NONE : mode;
};

// ----------------------------------------------------------------------------
// --- load/update

/**
*** Centers on the last available point
**/
function jsmSetCenter(lat, lon, zoom)
{
    if (!jsmap) { return; }
    var center = new JSMapPoint(lat, lon);
    jsmap.JSSetCenter(center, zoom);
};

/**
*** Centers on the last available point
**/
function jsmCenterOnLastPushpin(showLastPointOnly)
{
    if (!jsmap) { return; }

    /* remove old layers */
    jsmap.JSClearLayers();

    /* draw POI */
    if (jsvPoiPins && (jsvPoiPins.length > 0)) {
        jsmap.JSDrawPOI(jsvPoiPins);
    }

    /* data set */
    if (jsvDataSets && (jsvDataSets.length > 0)) {
        var jds = jsvDataSets[0];

        /* draw the route line first */
        if (jds.routePoints && (jds.routePoints.length >= 2) && (jds.routeColor != ROUTE_COLOR_NONE)) {
            jsmap.JSDrawRoute(jds.routePoints, jds.routeColor);
        }

        /* draw the pushpins */
        var jpp = jds.pushPins;
        if (jpp && (jpp.length > 0)) {
            if (showLastPointOnly) {
                var lastPoint = [ jpp[jpp.length - 1] ];
                jsmap.JSDrawPushpins(lastPoint, jsmRecenterZoomMode(RECENTER_ZOOM), REPLAY_STOPPED);
            } else {
                jsmap.JSDrawPushpins(jpp, jsmRecenterZoomMode(RECENTER_ZOOM), REPLAY_STOPPED);
            }
        }

    }

    /* close the detail report */
    jsvDetailVisible = false;
    jsmShowDetailReport();

};

// ----------------------------------------------------------------------------

/**
*** Sets the points/attributes on the current map
**/
function _jsmSetMap(recenterMode, /*JSMapDataSet[]*/mapDataSets, poiPins, replay)
{
    if (!jsmap) { return; }

    /* remove old layers */
    jsmap.JSClearLayers();

    /* draw POI */
    jsvPoiPins = poiPins;
    if (jsvPoiPins && (jsvPoiPins.length > 0)) {
        jsmap.JSDrawPOI(jsvPoiPins);
    }

    /* draw datasets */
    jsvDataSets = mapDataSets;
    if (jsvDataSets) {

        for (var i = 0; i < jsvDataSets.length; i++) {
            var jds = jsvDataSets[i]; // JSMapDataSet

            /* draw the route line first */
            if (jds.routePoints && (jds.routePoints.length >= 2) && (jds.routeColor != ROUTE_COLOR_NONE)) {
                jsmap.JSDrawRoute(jds.routePoints, jds.routeColor);
            }

            /* draw the pushpins */
            if (jds.pushPins && (jds.pushPins.length > 0)) {
                var rcm = ((i + 1) == jsvDataSets.length)? recenterMode : RECENTER_NONE;
                jsmap.JSDrawPushpins(jds.pushPins, jsmRecenterZoomMode(rcm), replay);
            }

            /* only one dataset if 'replay' active */
            if (replay > 0) {
                break;
            }

        }
    }

};

/**
*** Returns an AJAX request object
**/
function jsmGetXMLHttpRequest()
{
    return getXMLHttpRequest();
};

// ----------------------------------------------------------------------------

/**
*** Parse the specified XML/JSON
**/
function jsmParseAJAXPoints(dataText, recenterMode, replay) // tmz
{
    if (dataText.startsWith("{")) {
        //alert("Found JSON:\n" + dataText);
        return jsmParseAJAXPoints_JSON(dataText, recenterMode, replay);
    } else {
        //alert("Found XML");
        return jsmParseAJAXPoints_XML(dataText, recenterMode, replay);
    }
};

/**
*** Parse the specified JSON
**/
function jsmParseAJAXPoints_JSON(jsonText, recenterMode, replay) // tmz
{
// {
//   "JMapData" : {
//      "isFleet": false,
//      "Time": {
//          "timestamp": EPOCH,
//          "timezone": "TMZ",
//          "ymd": { YYYY:2011, MM:9, DD:12 },
//          "date": "YYYY/MM/DD",
//          "time": hh:mm:ss"
//      },
//      "LastEvent": {
//          "device": "DEVICE",
//          "timestamp": EPOCH,
//          "timezone": "TMZ",
//          "year": YYYY,
//          "month": MM,
//          "day": DD,
//          "battery": 0.42,
//          "signal": 0.45,
//          "Data": "YYYY/MM/DD|hh:mm:ss"
//      },
//      "Shapes": [
//          {
//              "type": "circle",
//              "radius": 1000,
//              "color": "#FF0000",
//              "Points": [
//                  "lat/lon", "lat/lon", ...
//              ]
//          }
//       ],
//      "DataColumns": "Desc|Epoch|Date|Time|Tmz|Stat|Icon|Lat|Lon|#Sats|kph|Heading|Alt|Addr",
//      "DataSets": [
//          {
//              "type": "poi",
//              "route": "false",
//              "Points": [
//                  "POIDesc|||0|Latitude|Longitude|0.0|0.0|0.0|Address",
//                  ...
//              ],
//          },
//          {
//              "type": "device",
//              "id": "deviceid",
//              "route": "true",
//              "routeColor": "#FF0000",
//              "textColor": "#FF0000",
//              "Points": [
//                  "DeviceDesc|Data|Time|StatusCode|Latitude|Longitude|SpeedKPH|Heading|Altitude|Address",
//                  ...
//              ],
//          }
//     ],
//     "Actions": [
//          {
//              "cmd": "showpp",
//              "arg": "2"
//          },
//          {
//              "cmd": "zoompp",
//              "arg": "2"
//          },
//     ]
//   }
// }

    /* get JMapData object */
    var jsonDoc = JSON.parse(jsonText);
    if (jsonDoc == null) {
        //alert('No data points provided');
        return 0;
    }
    var JMapData = jsonDoc.JMapData; // JSON_JMapData
    if (JMapData == null) {
        alert("JMapData not found in JSON response");
        return 0;
    }
    var isFleet = JMapData.isFleet;  // ATTR_isFleet

    /* time */
    var Time = JMapData.Time; // JSON_Time
    if (Time != null) {
        jsvTodayEpoch         = Time.timestamp;         // ATTR_timestamp
        jsvTodayTmzFmt        = Time.timezone;          // ATTR_timezone
        jsvTodayYMD           = Time.YMD;               // ATTR_ymd   (in selected timezone)
        jsvTodayDateFmt       = Time.date;              // ATTR_date
        jsvTodayTimeFmt       = Time.time;              // ATTR_time
    }

    /* last event */
    var LastEvent = JMapData.LastEvent; // JSON_LastEvent
    if (LastEvent != null) {
        jsvLastEventEpoch     = LastEvent.timestamp;    // ATTR_timestamp
        jsvLastEventTmzFmt    = LastEvent.timezone;     // ATTR_timezone
        jsvLastEventYMD       = LastEvent.YMD;          // ATTR_ymd       (in selected timezone)
        jsvLastEventDateFmt   = LastEvent.date;         // ATTR_date
        jsvLastEventTimeFmt   = LastEvent.time;         // ATTR_time
        jsvLastBatteryLevel   = LastEvent.battery;      // ATTR_battery   // battery level (%)
        jsvLastSignalStrength = LastEvent.signal;       // ATTR_signal    // signal strength (%)
    } else {
        jsvLastBatteryLevel   = 0.0;
        jsvLastSignalStrength = 0.0;
    }

    /* detail report */
    //var detailList  = []; // detailed report table

    /* points of interest */
    var poiPinList = []; // POI pushpins

    /* dataset */
    var dsNdx      = 0;
    var dsList     = []; // dataset list

    /* "Location Detail" report */
    var detailList = []; // detailed report table

    /* parse Shape tags [MapShape] */
    var shapes = [];
    var Shapes = JMapData.Shapes; // JSON_Shapes [JSMapShape]
    if ((Shapes != null) && (Shapes.length > 0)) {
        //alert('Parsing JSON shapes ...');
        for (var msi = 0; msi < Shapes.length; msi++) {
            var ms      = Shapes[msi];  // JSMapShape
            var type    = ms.type;      // ATTR_type ("circle", "rectangle", "polygon")
            var radiusM = ms.radius;    // ATTR_radius (meters)
            var color   = ms.color;     // ATTR_color
            var desc    = ms.desc;      // ATTR_desc
            var ppNdx   = ms.ppNdx;     // ATTR_ppNdx

            /* points "<lat>/<lon>,<lat>/<lon>,..." */
            var dbgPts = "";
            var ptFld  = ms.Points;    // JSON_Points
            var points = [];
            for (var i = 0; i < ptFld.length; i++) {
                var LL = ptFld[i].split('/');
                if (LL.length < 2) { continue; }
                var lat = numParseFloat(LL[0], 0);
                var lon = numParseFloat(LL[1], 0);
                if (((lat != 0) || (lon != 0))) {
                    points.push(new JSMapPoint(lat,lon));
                    dbgPts += lat+"/"+lon+", ";
                }
            }
            //alert("Parsed JSON shape type="+type +" radius="+radiusM +" color="+color +" pt="+dbgPts);

            /* create shape (draw later) */
            shapes.push(new JSMapShape(type,radiusM,points,color,false,desc,ppNdx)); // JSON

        }
    }

    /* JSON: parse DataSet tags */
    var rcdNdx = 0;
    var maxDataSetPoints = 0;
    var DataSets = (JMapData.DataSets != null)? JMapData.DataSets : [];  // JSON_DataSets
    for (var dsi = 0; dsi < DataSets.length; dsi++) {
        //alert('Parsing JSON DataSet '+dsi+' ...');
        var ds = DataSets[dsi];

        /* dataset vars */
        var showPushpin = PUSHPINS_SHOW; // JSON
        var pushPinCnt  = 0;
        var pushPinList = []; // device pushpins
        var partial     = false;

        /* type */
        var type        = strDefault(ds.type,"device"); // ATTR_type ("group", "device", "poi")
        var isPOI       = (type == "poi")? true : false;

        /* device/group/poi ID */
        var typeID      = strDefault(ds.id,""); // ATTR_id

        /* route-line */
        var showRoute   = ROUTE_LINE_SHOW;
        var routeList   = []; // route line
        var route       = ds.route;         // ATTR_route
        var textColor   = strDefault(ds.textColor,"");     // ATTR_textColor
        var routeColor  = strDefault(ds.routeColor,"");    // ATTR_routeColor
        if (routeColor == "") { routeColor = ROUTE_LINE_COLOR; }

        /* show route-line? */
        if (showRoute) {
            if (isPOI) {
                // route-line is already not shown for POI
            } else
            if (!route) {
                showRoute = false;
            }
        }

        /* points */
        var pts = ds.Points; // JSON_Points
        var startNdx = 0;
        if ((pts.length - startNdx) > MAX_PUSH_PINS) {
            startNdx = pts.length - MAX_PUSH_PINS;
            partial  = true;
        }

        /* parse points */
        var dsPtCount = 0;
        var lastValidPushpin = null;
        var lastEvRcd = null;
        var lastStopEv = null;
        for (var p = startNdx; p < pts.length; p++) {
            var cvsRcd = pts[p];

            /* parse point */
            var evRcd = new MapEventRecord(cvsRcd); // JSON
            if (!evRcd.valid) {
                continue; // skip invalid records
            }

            /* add type/typeID */
            evRcd.type   = type;
            evRcd.typeID = typeID;

            /* device/fleet? */
            evRcd.isFleet = isFleet;

            /* Point Of Interest? [JSON] */
            if (isPOI) {
                if (evRcd.validGPS) {
                    var ndx   = -1; // ++rcdNdx;
                    var ppNdx = poiPinList.length;
                    var ppObj = jsmCreatePushPin(ndx, -1, ppNdx, evRcd); // JSMapPushpin
                    if (ppObj) {
                        poiPinList.push(ppObj);
                    }
                }
                continue;
            }

            /* linked-list [JSON] */
            if (lastEvRcd != null) {
                lastEvRcd.nextEv = evRcd; // last.next = this
                evRcd.lastEv = lastEvRcd; // this.last = last
            }
            lastEvRcd = evRcd;

            /* record index */
            if (evRcd.timestamp > 0) {
                rcdNdx++;
                evRcd.index = rcdNdx;
                dsPtCount++;
            } else {
                evRcd.index = 0;
            }

            /* save displayable point [JSON] */
            if (evRcd.validGPS) {
                if (showRoute) {
                    routeList.push(new JSMapPoint(evRcd.latitude, evRcd.longitude));
                }
                if (evRcd.timestamp > 0) {
                    if (!SHOW_ADDR && (evRcd.address != null)) { SHOW_ADDR = true; }
                    var ppNdx = pushPinCnt;
                    var ppObj = jsmCreatePushPin(rcdNdx, dsNdx, ppNdx, evRcd); // JSMapPushpin
                    if (ppObj) {
                        lastValidPushpin = ppObj;
                        lastValidPushpin.show = showPushpin; // this.show  [JSON]
                        if (showPushpin) {
                            lastValidPushpin.show = true;
                            pushPinList.push(lastValidPushpin);
                            //alert("[JSON] Created Pushpin: " + ppNdx);
                        }
                        detailList.push(new JSDetailPoint(rcdNdx, dsNdx, pushPinCnt, evRcd, textColor));
                        pushPinCnt++;
                    }
                }
            } else
            if (evRcd.timestamp > 0) {
                detailList.push(new JSDetailPoint(rcdNdx,    -1,    -1, evRcd, textColor));
            }

            /* motion change [JSON] */
            if (evRcd.stopped < 0) {
                // undefined,
            } else
            if (evRcd.stopped == 0) { // moving
                if (lastStopEv != null) {
                    var deltaSec = evRcd.timestamp - lastStopEv.timestamp;
                    lastStopEv.stopSec = deltaSec;
                }
                lastStopEv = null;
            } else
            if (evRcd.stopped == 2) { // stop event
                lastStopEv = evRcd;
            } else
            if (evRcd.stopped == 1) { // still stopped
                //
            }

            /* save last reported event times */
            if (!IS_FLEET && (evRcd.timestamp > jsvLastEventEpoch)) {
                jsvLastEventEpoch   = evRcd.timestamp;
                jsvLastEventYMD     = { YYYY:evRcd.year, MM:evRcd.month1, DD:evRcd.day }; // in selected timezone
                jsvLastEventDateFmt = evRcd.dateFmt;
                jsvLastEventTimeFmt = evRcd.timeFmt;
                jsvLastEventTmzFmt  = evRcd.timeZone; // Timezone
            }

        }

        /* contimue if last was POI */
        if (isPOI) {
            continue;
        }

        /* check stop-time for last stop event [JSON] */
        if ((lastStopEv != null) && (jsvTodayEpoch > 0)) {
            var deltaSec = jsvTodayEpoch - lastStopEv.timestamp;
            lastStopEv.stopSec = deltaSec;
        }

        /* !showPushpin? always show at least the last pushpin [JSON] */
        if (!showPushpin && (lastValidPushpin != null)) {
            lastValidPushpin.show = true; // GTS_2.4.0-BB36
            pushPinList.push(lastValidPushpin);
        }

        /* save dataset */
        if (dsPtCount > maxDataSetPoints) { maxDataSetPoints = dsPtCount; }
        dsList.push(new JSMapDataSet(pushPinList,(showRoute?routeList:null),routeColor,partial));
        dsNdx++;

    } // parsing datasets

    /* device breaks? (more than one dataset and any single dataset has more than one point) */
    jsvUseDeviceBreaks = (dsList.length > 1) && (maxDataSetPoints > 1);

    /* save datasets */
    jsvDetailPoints = detailList; // JSDetailPoint[]

    /* update map */
    _jsmSetMap(recenterMode, dsList, poiPinList, replay);

    /* draw shapes */
    if (shapes && (shapes.length > 0)) {
        //alert('Drawing shapes ...');
        for (var i = 0; i < shapes.length; i++) {
            var s = shapes[i]; // JSMapShape
            var ok = jsmDrawShape(s.type,s.radius,s.points,s.color,s.zoomTo,s.desc,s.ppNdx);
            //alert("Drew JSON shape type="+s.type + "rad="+s.radius +" ok="+ok);
        }
    }

    /* update last event times */
    if (jsvLastEventDateFmt && jsvLastEventTimeFmt) {
        jsmSetIDInnerHTML(ID_LATEST_EVENT_DATE, jsvLastEventDateFmt);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TIME, jsvLastEventTimeFmt);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TMZ , jsvLastEventTmzFmt );
    } else {
        jsmSetIDInnerHTML(ID_LATEST_EVENT_DATE, "");
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TIME, TEXT_UNAVAILABLE);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TMZ , jsvLastEventTmzFmt );
    }

    /* update battery */
    jsmSetIDInnerHTML(ID_LATEST_BATTERY, jsmBatteryLevelIMG(jsvLastBatteryLevel));

    /* reached maximum allowed pushpins? */
    jsvPartialData = partial;
    if (jsvPartialData) {
        jsmSetIDInnerHTML(ID_MESSAGE_TEXT, TEXT_MAXPUSHPINS_MSG);
    } else {
        jsmSetIDInnerHTML(ID_MESSAGE_TEXT, "");
    }

    /* update detail report */
    //alert('Populating detail report ...');
    jsmShowDetailReport();

    /* check for action */
    var Actions = (JMapData.Actions != null)? JMapData.Actions : [];  // JSON_Actions
    for (var ai = 0; ai < Actions.length; ai++) {
        var act = Actions[ai];
        var cmd = strDefault(act.cmd,"");  // JSON_cmd  ("autoupdate", "alert", "gotourl", "zoompp", "showpp")
        var arg = strDefault(act.arg,"");  // JSON_arg
        if (cmd == "autoupdate") {
            try {
                if (arg == "true") {
                    // AutoInterval?
                    //alert('Start auto update timer ...');
                    startAutoUpdateMapTimer();
                } else {
                    //alert('Stop auto update timer ...');
                    stopAutoUpdateMapTimer();
                }
            } catch (e) {
                // ignore
            }
        } else
        if (cmd == "alert") {
            alert(arg);
        } else
        if (cmd == "gotourl") {
            target = "_self";
            //alert('Opening URL ...');
            openURL(arg, target)
        } else
        if (cmd == "showpp") {
            var ndx = arg - 1;
            //alert('Showing detail pushpin ...');
            jsmShowDetailPushpin(0,ndx);
        } else
        if (cmd == "zoompp") {
            //alert('Zooming to pushpin ...');
            var ndx = arg - 1;
            var pp = jsmGetPushpin(0,ndx); // JSMapPushpin
            if (pp != null) {
                var lat  = pp.lat;
                var lon  = pp.lon;
                var zoom = -1; // TODO:
                jsmSetCenter(lat, lon, zoom);
            }
        }
    }

    /* return number of points parsed */
    return jsvDetailPoints.length;

};

/**
*** Parse the specified XML
**/
function jsmParseAJAXPoints_XML(xmlText, recenterMode, replay) // tmz
{

    /* create XML doc */
    //alert("Parse XML: " + xmlText);
    var xmlDoc = createXMLDocument(xmlText);
    if (xmlDoc == null) {
        // alert('No data points provided');
        return 0;
    }

    /* parse */
    var data = xmlDoc.getElementsByTagName(TAG_MapData);
    if (data.length <= 0) {
        return 0;
    }
    var dataElem = data[0];
    var dataAttr = dataElem.attributes;
    var isFleet  = getXMLNodeAttribute(dataAttr,ATTR_isFleet,false);

    /* last event */
    var latest    = dataElem.getElementsByTagName(TAG_LastEvent);
    var latestVal = (latest.length > 0)? latest[0].childNodes[0].nodeValue : null;
    if (latestVal != null) {
        var timeAttr          = latest[0].attributes;
        jsvLastEventEpoch     = numParseInt(getXMLNodeAttribute(timeAttr,ATTR_timestamp,0),0);
        jsvLastEventTmzFmt    = getXMLNodeAttribute(timeAttr,ATTR_timezone,"");
        var year              = getXMLNodeAttribute(timeAttr,ATTR_year,0);     // year (in selected timezone)
        var month1            = getXMLNodeAttribute(timeAttr,ATTR_month,0);    // month1 (in selected timezone)
        var day               = getXMLNodeAttribute(timeAttr,ATTR_day,0);      // date (in selected timezone)
        var battery           = getXMLNodeAttribute(timeAttr,ATTR_battery,0);  // battery level (%)
        var signal            = getXMLNodeAttribute(timeAttr,ATTR_signal,0);   // signal strength (%)
        jsvLastEventYMD       = { YYYY:year, MM:month1, DD:day };              // in selected timezone
        jsvLastBatteryLevel   = battery;
        jsvLastSignalStrength = signal;
        var fld = latestVal.split('|');
        if (fld.length > 0) {
            var dateFmt         = (fld.length > 0)? fld[0] : '';            // formatted date
            var timeFmt         = (fld.length > 1)? fld[1] : '';            // formatted time
            var battFmt         = (fld.length > 2)? fld[2] : '';            // formatted battery level
            var signFmt         = (fld.length > 3)? fld[3] : '';            // formatted signal strength
            jsvLastEventDateFmt = dateFmt;
            jsvLastEventTimeFmt = timeFmt;
        }
    } else {
        jsvLastBatteryLevel   = 0.0;
        jsvLastSignalStrength = 0.0;
    }

    /* time */
    var today    = dataElem.getElementsByTagName(TAG_Time);
    var todayVal = (today.length > 0)? today[0].childNodes[0].nodeValue : null;
    if (todayVal != null) {
        var timeAttr       = today[0].attributes;
        jsvTodayEpoch      = getXMLNodeAttribute(timeAttr,ATTR_timestamp,0);
        jsvTodayTmzFmt     = getXMLNodeAttribute(timeAttr,ATTR_timezone ,"");
        var year           = getXMLNodeAttribute(timeAttr,ATTR_year ,0);    // year (in selected timezone)
        var month1         = getXMLNodeAttribute(timeAttr,ATTR_month,0);    // month1 (in selected timezone)
        var day            = getXMLNodeAttribute(timeAttr,ATTR_day  ,0);    // date (in selected timezone)
        jsvTodayYMD        = { YYYY:year, MM:month1, DD:day };              // in selected timezone
        var fld = todayVal.split('|');
        if (fld.length > 0) {
            var dateFmt       = (fld.length > 0)? fld[0] : '';              // formatted date
            var timeFmt       = (fld.length > 1)? fld[1] : '';              // formatted time
            jsvTodayDateFmt   = dateFmt;
            jsvTodayTimeFmt   = timeFmt;
        }
    }

    /* detail report */
    //var detailList  = []; // detailed report table

    /* points of interest */
    var poiPinList  = []; // POI pushpins

    /* dataset */
    var dsNdx       = 0;
    var dsList      = []; // dataset list

    /* "Location Detail" report */
    var detailList  = []; // detailed report table

    /* parse Shape tags [Shape] */
    var shapes      = [];
    var mapShapes   = dataElem.getElementsByTagName(TAG_Shape);
    for (var msi = 0; msi < mapShapes.length; msi++) {
        var ms      = mapShapes[msi];
        var msAttr  = ms.attributes;
        var type    = getXMLNodeAttribute(msAttr, ATTR_type   , "none"); // "circle", "rectangle", "polygon"
        var radiusM = numParseFloat(getXMLNodeAttribute(msAttr,ATTR_radius,0),0); // fix 2.3.9-B17
        var color   = getXMLNodeAttribute(msAttr, ATTR_color  , "#0000FF");
        var desc    = getXMLNodeAttribute(msAttr, ATTR_desc   , "");
        var ppNdx   = numParseInt(getXMLNodeAttribute(msAttr,ATTR_ppNdx,0),0);

        /* points "<lat>/<lon>,<lat>/<lon>,..." */
        var dbgPts = "";
        var ptsStr = ms.childNodes[0].nodeValue;
        var ptFld  = ptsStr.split(',');
        var points = [];
        for (var i = 0; i < ptFld.length; i++) {
            var LL = ptFld[i].split('/');
            if (LL.length < 2) { continue; }
            var lat = numParseFloat(LL[0], 0);
            var lon = numParseFloat(LL[1], 0);
            if (((lat != 0) || (lon != 0))) {
                points.push(new JSMapPoint(lat,lon));
                dbgPts += lat+"/"+lon+", ";
            }
        }
        //alert("Parsed XML shape type="+type +" radius="+radiusM +" color="+color +" pt="+dbgPts);

        /* create shape (draw later) */
        shapes.push(new JSMapShape(type,radiusM,points,color,false,desc,ppNdx)); // XML

    }

    /* XML: parse DataSet tags */
    var rcdNdx = 0;
    var maxDataSetPoints = 0;
    var dataSets = dataElem.getElementsByTagName(TAG_DataSet);
    for (var dsi = 0; dsi < dataSets.length; dsi++) {
        var ds = dataSets[dsi];
        var dsAttr = ds.attributes;

        /* dataset vars */
        var showPushpin = PUSHPINS_SHOW; // [XML]
        var pushPinCnt  = 0;
        var pushPinList = []; // device pushpins
        var partial     = false;

        /* type */
        var type        = getXMLNodeAttribute(dsAttr, ATTR_type, "device"); // "group", "device", "poi"
        var isPOI       = (type == "poi")? true : false;

        /* device/group/poi ID */
        var typeID      = getXMLNodeAttribute(dsAttr, ATTR_id, "");

        /* route-line */
        var showRoute   = ROUTE_LINE_SHOW;
        var routeList   = []; // route line
        var route       = (getXMLNodeAttribute(dsAttr, ATTR_route, "true") != "false")? true : false;
        var textColor   = getXMLNodeAttribute(dsAttr, ATTR_textColor, "");
        var routeColor  = getXMLNodeAttribute(dsAttr, ATTR_routeColor, "");
        if (routeColor == "") { routeColor = ROUTE_LINE_COLOR; }

        /* show route-line? */
        if (showRoute) {
            if (isPOI) {
                // route-line is already not shown for POI
            } else
            if (!route) {
                showRoute = false;
            }
        }
        //alert("Route = "+route+"/"+showRoute+" [" + getXMLNodeAttribute(dsAttr,ATTR_route,"?"));

        /* points */
        var pts = ds.getElementsByTagName(TAG_Point);
        var startNdx = 0;
        if ((pts.length - startNdx) > MAX_PUSH_PINS) {
            startNdx = pts.length - MAX_PUSH_PINS;
            partial  = true;
        }

        /* parse points */
        var dsPtCount = 0;
        var lastValidPushpin = null;
        var lastEvRcd = null;
        var lastStopEv = null;
        for (var p = startNdx; p < pts.length; p++) {
            var cvsRcd = pts[p].childNodes[0].nodeValue;

            /* parse point */
            var evRcd = new MapEventRecord(cvsRcd); // XML
            if (!evRcd.valid) {
                continue; // skip invalid records
            }

            /* add type/typeID */
            evRcd.type   = type;
            evRcd.typeID = typeID;

            /* Point Of Interest? [XML] */
            if (isPOI) {
                if (evRcd.validGPS) {
                    var ndx   = -1; // ++rcdNdx;
                    var ppNdx = poiPinList.length;
                    var ppObj = jsmCreatePushPin(ndx, -1, ppNdx, evRcd); // JSMapPushpin
                    if (ppObj) {
                        poiPinList.push(ppObj);
                    }
                }
                continue;
            }

            /* linked-list [XML] */
            if (lastEvRcd != null) {
                lastEvRcd.nextEv = evRcd; // last.next = this
                evRcd.lastEv = lastEvRcd; // this.last = last
            }
            lastEvRcd = evRcd;

            /* record index */
            if (evRcd.timestamp > 0) {
                rcdNdx++;
                evRcd.index = rcdNdx;
                dsPtCount++;
            } else {
                evRcd.index = 0;
            }

            /* save displayable point [XML] */
            if (evRcd.validGPS) {
                if (showRoute) {
                    routeList.push(new JSMapPoint(evRcd.latitude, evRcd.longitude));
                }
                if (evRcd.timestamp > 0) {
                    if (!SHOW_ADDR && (evRcd.address != null)) { SHOW_ADDR = true; }
                    var ppNdx = pushPinCnt;
                    var ppObj = jsmCreatePushPin(rcdNdx, dsNdx, ppNdx, evRcd); // JSMapPushpin
                    if (ppObj) {
                        lastValidPushpin = ppObj;
                        lastValidPushpin.show = showPushpin; // this.show   [XML]
                        if (showPushpin) {
                            lastValidPushpin.show = true;
                            pushPinList.push(lastValidPushpin);
                            //alert("[XML] Created Pushpin: " + ppNdx);
                        }
                        detailList.push(new JSDetailPoint(rcdNdx, dsNdx, ppNdx, evRcd, textColor));
                        pushPinCnt++;
                    }
                }
            } else
            if (evRcd.timestamp > 0) {
                detailList.push(new JSDetailPoint(rcdNdx,    -1,    -1, evRcd, textColor));
            }

            /* motion change [XML] */
            if (evRcd.stopped < 0) {
                //
            } else
            if (evRcd.stopped == 0) { // moving
                if (lastStopEv != null) {
                    var deltaSec = evRcd.timestamp - lastStopEv.timestamp;
                    lastStopEv.stopSec = deltaSec;
                }
                lastStopEv = null;
            } else
            if (evRcd.stopped == 2) { // stop event
                lastStopEv = evRcd;
            } else
            if (evRcd.stopped == 1) { // still stopped
                //
            }

            /* save last reported event times */
            if (!IS_FLEET && (evRcd.timestamp > jsvLastEventEpoch)) {
                jsvLastEventEpoch   = evRcd.timestamp;
                jsvLastEventYMD     = { YYYY:evRcd.year, MM:evRcd.month1, DD:evRcd.day }; // in selected timezone
                jsvLastEventDateFmt = evRcd.dateFmt;
                jsvLastEventTimeFmt = evRcd.timeFmt;
                jsvLastEventTmzFmt  = evRcd.timeZone; // Timezone
            }

        }

        /* contimue if last was POI */
        if (isPOI) {
            continue;
        }

        /* check stop-time for last stop event [XML] */
        if ((lastStopEv != null) && (jsvTodayEpoch > 0)) {
            var deltaSec = jsvTodayEpoch - lastStopEv.timestamp;
            lastStopEv.stopSec = deltaSec;
        }

        /* !showPushpin? always show at least the last pushpin [XML] */
        if (!showPushpin && (lastValidPushpin != null)) {
            lastValidPushpin.show = true; // GTS_2.4.0-BB36
            pushPinList.push(lastValidPushpin);
        }

        /* save dataset */
        if (dsPtCount > maxDataSetPoints) { maxDataSetPoints = dsPtCount; }
        dsList.push(new JSMapDataSet(pushPinList,(showRoute?routeList:null),routeColor,partial));
        dsNdx++;

    } // parsing datasets

    /* device breaks? (more than one dataset and any single dataset has more than one point) */
    jsvUseDeviceBreaks = (dsList.length > 1) && (maxDataSetPoints > 1);

    /* save datasets */
    jsvDetailPoints = detailList; // JSDetailPoint[]

    /* update map */
    _jsmSetMap(recenterMode, dsList, poiPinList, replay);

    /* draw shapes */
    if (shapes && (shapes.length > 0)) {
        for (var i = 0; i < shapes.length; i++) {
            var s = shapes[i]; // JSMapShape
            var ok = jsmDrawShape(s.type,s.radius,s.points,s.color,s.zoomTo,s.desc,s.ppNdx);
            //alert("Drew XML shape type="+s.type + "rad="+s.radius +" ok="+ok);
        }
    }

    /* update last event times */
    if (jsvLastEventDateFmt && jsvLastEventTimeFmt) {
        jsmSetIDInnerHTML(ID_LATEST_EVENT_DATE, jsvLastEventDateFmt);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TIME, jsvLastEventTimeFmt);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TMZ , jsvLastEventTmzFmt );
    } else {
        jsmSetIDInnerHTML(ID_LATEST_EVENT_DATE, "");
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TIME, TEXT_UNAVAILABLE);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TMZ , jsvLastEventTmzFmt );
    }

    /* update battery */
    jsmSetIDInnerHTML(ID_LATEST_BATTERY, jsmBatteryLevelIMG(jsvLastBatteryLevel));

    /* reached maximum allowed pushpins? */
    jsvPartialData = partial;
    if (jsvPartialData) {
        //alert(TEXT_MAXPUSHPINS_ALERT);
        jsmSetIDInnerHTML(ID_MESSAGE_TEXT, TEXT_MAXPUSHPINS_MSG);
    } else {
        jsmSetIDInnerHTML(ID_MESSAGE_TEXT, "");
    }

    /* update detail report */
    jsmShowDetailReport();

    /* check for action */
    // <Action command="autoupdate">true</Action>
    //alert("Checking Action ...");
    var actions = dataElem.getElementsByTagName(TAG_Action);
    for (var ai = 0; ai < actions.length; ai++) {
        var act  = actions[ai];
        var attr = act.attributes;
        var cmd  = getXMLNodeAttribute(attr,ATTR_command,""); // "autoupdate", "alert", "gotourl"
        var arg  = act.childNodes[0].nodeValue;
        //alert("Action: " + cmd + " " + arg);
        if (cmd == "autoupdate") {
            try {
                if (arg == "true") {
                    // AutoInterval?
                    startAutoUpdateMapTimer();
                } else {
                    stopAutoUpdateMapTimer();
                }
            } catch (e) {
                // ignore
            }
        } else
        if (cmd == "alert") {
            alert(arg);
        } else
        if (cmd == "gotourl") {
            target = "_self";
            openURL(arg, target)
        } else
        if (cmd == "showpp") {
            var ndx = arg - 1;
            jsmShowDetailPushpin(0,ndx);
        } else
        if (cmd == "zoompp") {
            var ndx = arg - 1;
            var pp = jsmGetPushpin(0,ndx); // JSMapPushpin
            if (pp != null) {
                var lat  = pp.lat;
                var lon  = pp.lon;
                var zoom = -1; // TODO:
                jsmSetCenter(lat, lon, zoom);
            }
        }
    }

    /* return number of points parsed */
    return jsvDetailPoints.length;

};

// ----------------------------------------------------------------------------

/**
*** Load and display point from the specified URL and display them on the current map
**/
function jsmLoadPoints(mapURL, recenterMode, replay)
{
    try {
        var req = jsmGetXMLHttpRequest();
        if (req) {
            req.open("GET", mapURL, true);
            //req.setRequestHeader("CACHE-CONTROL", "NO-CACHE");
            //req.setRequestHeader("PRAGMA", "NO-CACHE");
            req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
            req.onreadystatechange = function() {
                if (req.readyState == 4) {
                    var data = req.responseText; // JSON/XML
                    if (data.trim().toUpperCase() == DATA_RESPONSE_LOGOUT) {
                        alert(TEXT_TIMEOUT);
                        jsmSetLoadingPointsState(0);
                    } else {
                        jsmParseAJAXPoints(data, recenterMode, replay);
                        jsmSetLoadingPointsState(0);
                    }
                } else
                if (req.readyState == 1) {
                    // alert('Loading points from URL: [' + req.readyState + ']\n' + mapURL);
                } else {
                    // alert('Problem loading URL? [' + req.readyState + ']\n' + mapURL);
                }
            };
            jsmSetLoadingPointsState(1);
            req.send(null);
        } else {
            alert("Error [jsmLoadPoints]:\n" + mapURL);
        }
    } catch (e) {
        alert("Error [jsmLoadPoints]:\n" + e);
    }
};

/**
*** Call-back on load state change
***   -1 == finish error
***    0 == finish success
***    1 == start
**/
var jsmapLoadingView = null;
function jsmSetLoadingPointsState(state)
{

    /* change color of "Update" button */
    var elem = ID_MAP_UPDATE_BTN? document.getElementById(ID_MAP_UPDATE_BTN) : null;
    if (elem != null) {
        switch (state) {
            case 0 : elem.style.color = '#000000'; break;
            case 1 : elem.style.color = '#338833'; break;
            default: elem.style.color = '#FF0000'; break;
        }
    }

    /* show "Loading Map Points ..." message */
    if (TEXT_LOADING_MAP_POINTS) {
        if (state == 1) {
            // show
            if (jsmapLoadingView == null) {
                var absLoc = getElementPosition(jsmapElem);
                var absSiz = getElementSize(jsmapElem);
                var X = absLoc.left + (absSiz.width /2) - 70;
                var Y = absLoc.top  + (absSiz.height/2) - 40;
                jsmapLoadingView = createDivBox("mapLoadingView", X, Y, -1, -1);
                var html = "";
                html += "<table class='jsmapLoadingView table' cellspacing='0' cellpadding='0' border='0'>\n";
                html += "<tbody>\n";
                html += "<tr class='jsmapLoadingRow'>";
                if (MAP_LOADING_IMAGE_URI) {
                    html += "<td nowrap class='jsmapLoadingImage' valign='center'>";
                    html += "<img src=\"" + MAP_LOADING_IMAGE_URI + "\">";
                    html += "</td>";
                }
                html += "<td nowrap class='jsmapLoadingText' valign='center'>";
                html += TEXT_LOADING_MAP_POINTS;
                html += "</td>";
                html += "</tr>\n";
                html += "</tbody>\n";
                html += "</table>\n";
                jsmapLoadingView.innerHTML = html;
            }
            document.body.appendChild(jsmapLoadingView);
        } else {
            // hide
            if (jsmapLoadingView != null) {
                document.body.removeChild(jsmapLoadingView);
            }
        }
    }

};

// ----------------------------------------------------------------------------

/**
*** Return true if the device last event has been received within the
*** delta-time specified.
**/
function jsmIsDeviceOnline(offlineSec)
{
    var nowTime  = (new Date()).getTime() / 1000; // current Epoch time
    var deltaSec = nowTime - jsvLastEventEpoch;
    return (deltaSec <= offlineSec)? true : false;
};

// ----------------------------------------------------------------------------

/**
*** Ping device
**/
function jsmDevicePing(pingURL)
{
    try {
        var req = jsmGetXMLHttpRequest();
        if (req) {
            req.open("GET", pingURL, true);
            //req.setRequestHeader("CACHE-CONTROL", "NO-CACHE");
            //req.setRequestHeader("PRAGMA", "NO-CACHE");
            req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
            req.onreadystatechange = function() {
                if (req.readyState == 4) {
                    var data = req.responseText;
                    if (data.trim().toUpperCase() == DATA_RESPONSE_LOGOUT) {
                        alert(TEXT_TIMEOUT);
                    } else
                    if (data.trim().toUpperCase() == DATA_RESPONSE_PING_OK) {
                        alert(TEXT_PING_OK);
                    } else {
                        // TODO: extract a 'reason' from the error text
                        alert(TEXT_PING_ERROR);
                    }
                } else
                if (req.readyState == 1) {
                    // alert('Pinging device, URL: [' + req.readyState + ']\n' + pingURL);
                } else {
                    // alert('Problem sending URL? [' + req.readyState + ']\n' + pingURL);
                }
            };
            req.send(null);
        } else {
            alert("Error [jsmDevicePing]:\n" + pingURL);
        }
    } catch (e) {
        alert("Error [jsmDevicePing]:\n" + e);
    }
};

// ----------------------------------------------------------------------------
// --- shape

/**
*** Parse the specified csv Geozones and display them on the current map
*** @param type     The Geozone shape type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points (JSMapPoint[])
*** @param color    Shape color
*** @param zoomTo   True to center/zoom to shape
*** @param desc     The shape description
*** @param ppNdx    The pushpin index
*** @return True if shape was drawn, false otherwise
**/
function jsmDrawShape(type, radiusM, points, color, zoomTo, desc, ppNdx)
{
    try {
        return jsmap? jsmap.JSDrawShape(type,radiusM,points,color,zoomTo,desc,ppNdx) : false;
    } catch (e) {
        return false; // "JSDrawShape" not defined?
    }
};

// ----------------------------------------------------------------------------
// --- zone points

/**
*** Parse the specified csv Geozones and display them on the current map
*** @param zonePoints  Array of JSMapPoint's
**/
function jsmParseGeozones(zonePoints)
{
    if (!jsmap) { return; }
    // external var: jsvZoneRadiusMeters, jsvZoneEditable

    /* no zones? */
    if (zonePoints == null) {
        return 0;
    }

    /* parse zones */
    var pointCount = 0;
    //var points = [];
    var zoneIndex = zoneMapGetIndex();
    if ((zoneIndex >= 0) && (zoneIndex < zonePoints.length)) {
        // a specific point
        var z = zoneIndex;
        if (geoIsValid(zonePoints[z].lat,zonePoints[z].lon)) {
            //points.push(zonePoints[z]);
            pointCount++;
        }
    } else {
        // all points
        for (var z = 0; z < zonePoints.length; z++) {
            if (geoIsValid(zonePoints[z].lat,zonePoints[z].lon)) {
                //points.push(zonePoints[z]);
                pointCount++;
            }
        }
    }

    /* draw Geozone */
    // jsvZoneType:
    //   0 - ZONE_POINT_RADIUS
    //   1 - ZONE_BOUNDED_RECT
    //   2 - ZONE_SWEPT_POINT_RADIUS
    //   3 - ZONE_POLYGON
    //alert("Selected Zone Index: " + zoneIndex);
    jsmap.JSDrawGeozone(jsvZoneType, jsvZoneRadiusMeters, zonePoints, jsvZoneColor, zoneIndex);
    return pointCount;

};

// ----------------------------------------------------------------------------
// --- map points

function jsmFormatCoord(loc, isLat, dec)
{
    if ((LATLON_FORMAT == 1) || (LATLON_FORMAT == "DMS")) { // DD^MM'SS"
        var isPos = (loc >= 0.0)? true : false;
        loc = Math.abs(loc);
        var deg = parseInt(loc);
        loc = (loc - deg) * 60.0;
        var min = parseInt(loc);
        if (min <= 9) { min = '0' + min; }
        loc = (loc - min) * 60.0;
        var sec = parseInt(loc);
        if (sec <= 9) { sec = '0' + sec; }
        var quad = isLat? (isPos? HEADING[0] : HEADING[4]) : (isPos? HEADING[2] : HEADING[6]);
        return deg + "&deg;" + min + "'" + sec + "&quot;" + quad;
    } else
    if ((LATLON_FORMAT == 2) || (LATLON_FORMAT == "DM")) {  // DD^MM.mm'
        var isPos = (loc >= 0.0)? true : false;
        loc = Math.abs(loc);
        var deg = parseInt(loc);
        loc = (loc - deg) * 60.0;
        var min = numFormatFloat(loc, LATLON_FORMAT_MIN_DEC); // minutes decimal places
        if (min <= 9) { min = '0' + min; }
        var quad = isLat? (isPos? HEADING[0] : HEADING[4]) : (isPos? HEADING[2] : HEADING[6]);
        return deg + "&deg;" + min + "'" + quad;
    } else {
        return numFormatFloat(loc, dec);
    }
};

/**
*** Creates/Returns a JSMapPushpin object
**/
function jsmCreatePushPin(rcdNdx, dsNdx, ppNdx, evRcd)
{

    /* balloon text */
    var html = null;

    /* return JSMapPushpin */
    var ppi = jsmGetPushPinIcon(evRcd.iconNdx, evRcd);
    return new JSMapPushpin(
        rcdNdx, dsNdx, ppNdx, evRcd,
        evRcd.latitude, evRcd.longitude,
        evRcd.device, html,
        ppi);
        //ppi.iconURL, ppi.iconSize, ppi.iconHotspot,
        //ppi.shadow, ppi.shadowSize);

};

/**
*** Returns the pushpin object
*** @param icon  Icon index
*** @param e     The current event
**/
function jsmGetPushPinIcon(icon, e)
{
    var pp = ((icon >= 0) && (icon < jsvPushpinIcon.length))?
        jsvPushpinIcon[icon] :
        jsvPushpinIcon[0]; // black
    // -- main pushpin icon
    if (pp.iconEval) {
        try {
            var url = eval(pp.iconEval); // 'e' may be used within this 'eval'
            pp.iconURL = url;
        } catch(err) {
            // Exceptions are possible, since we cannot control what the configuration has specified
            // for the evaluated string.  Also note that this Javascript 'eval' trusts the authority
            // of the configuration admin to not place rogue code into the 'iconSelector'
            //alert("Pushpin error: " + err);
            if (!jsvPushpinIcon[0].iconEval) {
                pp.iconURL = jsvPushpinIcon[0].iconURL; // default to black icon
            } else {
                pp.iconURL = ""; // unknown icon (icons will show as broken images)
            }
        }
    }
    // -- background url/pushpin
    if ((typeof pp.bgPP !== 'undefined') && (pp.bgPP >= 0) && (pp.bgPP < jsvPushpinIcon.length)) {
        var bgPP = jsvPushpinIcon[pp.bgPP];
        if (bgPP.iconEval) {
            try {
                var url     = eval(bgPP.iconEval); // 'e' may be used within this 'eval'
                pp.bgURL    = url;
                pp.bgSize   = bgPP.iconSize;
                pp.bgOffset = bgPP.iconHotspot;
            } catch(err) {
                pp.bgURL = null;
            }
        }
    }
    // -- return PushPin
    return pp;
};

// ----------------------------------------------------------------------------
// --- create circle

/**
*** Returns an array of JSMapPoints representing a circle polygon
*** @param center  The circle center (JSMapPoint)
*** @param radiusM The circle radius, in meters
*** @return An array of JSMapPoints
**/
function jsmCreateCircle(center, radiusM)
{
    var rLat = geoRadians(center.lat);  // radians
    var rLon = geoRadians(center.lon);  // radians
    var d    = radiusM / EARTH_RADIUS_METERS;
    var circlePoints = new Array();
    for (x = 0; x <= 360; x += 5) {         // 5 degrees (saves memory, & it still looks like a circle)
        var xrad = geoRadians(x);           // radians
        var tLat = Math.asin(Math.sin(rLat) * Math.cos(d) + Math.cos(rLat) * Math.sin(d) * Math.cos(xrad));
        var tLon = rLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(rLat), Math.cos(d)-Math.sin(rLat) * Math.sin(tLat));
        circlePoints.push(new JSMapPoint(geoDegrees(tLat),geoDegrees(tLon)));
    }
    return circlePoints; // (JSMapPoint[])
};

/**
*** Returns a point that is 'radiusM' from the specified lat/lon in the 'heading' direction
*** @param lat     The Latitude
*** @param lon     The Longitude
*** @param radiusM The radius, in meters
*** @param heading The compass heading
*** @return A JSMapPoint
**/
function jsmCalcRadiusPoint(lat, lon, radiusM, heading)
{
    var crLat = geoRadians(lat);          // radians
    var crLon = geoRadians(lon);          // radians
    var d     = radiusM / EARTH_RADIUS_METERS;
    var xrad  = geoRadians(heading);            // radians
    var rrLat = Math.asin(Math.sin(crLat) * Math.cos(d) + Math.cos(crLat) * Math.sin(d) * Math.cos(xrad));
    var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(crLat), Math.cos(d)-Math.sin(crLat) * Math.sin(rrLat));
    return new JSMapPoint(geoDegrees(rrLat), geoDegrees(rrLon));
};

// ----------------------------------------------------------------------------
// --- detail report

/**
*** Attempts to return the pushpin at the specified index
*** @param dsNdx  The dataset index
*** @param ppNdx  The pushpin index
**/
function jsmGetPushpin(dsNdx, ppNdx)
{
    if (!jsmap) { return; }
    if ((dsNdx < 0) || (ppNdx < 0)) {
        return null;
    } else
    if ((jsvDataSets == null) || (jsvDataSets.length <= 0)) {
        return null;
    } else
    if (dsNdx >= jsvDataSets.length) {
        return null;
    }
    var jpp = jsvDataSets[dsNdx].pushPins;
    if (jpp && (ppNdx < jpp.length)) {
        return jpp[ppNdx];
    } else {
        return null;
    }
};

/**
*** Attempts to show the pushpin info bubble at the specified index
*** @param dsNdx  The dataset index
*** @param ppNdx  The pushpin index
**/
function jsmShowDetailPushpin(dsNdx, ppNdx)
{
    if (!jsmap) { return; }

    /* skip pushpin box? */
    if (!DETAIL_INFO_BOX || (dsNdx < 0) || (ppNdx < 0)) {
        // skip info box/bubble
        //alert("Skip show pushpin detail box ...");
        return;
    }

    /* no datasets? */
    if ((jsvDataSets == null) || (jsvDataSets.length <= 0)) {
        // skip info box/bubble
        //alert("No Datasets ... " + dsNdx);
        return;
    } else
    if (dsNdx >= jsvDataSets.length) {
        // skip info box/bubble
        //alert("Invalid Index ... " + dsNdx);
        return;
    }

    /* show pushpin */
    var jpp = jsvDataSets[dsNdx].pushPins;
    if (jpp && (ppNdx < jpp.length)) {
        jsmap.JSShowPushpin(jpp[ppNdx], jsvDetailCenterPushpin);
    } else {
        //alert("Invalid pushpin list or invalid index");
    }

};

/**
*** Shows the 'Location Details' table
**/
function jsmShowDetailReport()
{

    /* clear details */
    var parentWin = null;
    var dpt = jsvDetailPoints; // JSDetailPoint[]
    if (!jsvDetailVisible || (dpt == null) || (dpt.length <= 0)) {
        /* destroy location detail report */
        if (DETAILS_WINDOW) {
            if (jsvDetailsWindow != null) {
                jsvDetailsWindow.close();
                jsvDetailsWindow = null;
            }
        } else {
            var tableDiv = document.getElementById(ID_DETAIL_TABLE);
            if (tableDiv != null) {
                tableDiv.innerHTML = "";
                tableDiv.style.display = "none";
                // TODO: need to refresh/redraw parent
            }
        }
        /* reset control to "Show Location Details" text */
        var detailControl = document.getElementById(ID_DETAIL_CONTROL);
        if (detailControl != null) {
            detailControl.innerHTML = TEXT_showLocationDetails;
        }
        return;
    }

    /* open element to contain report */
    var winVar = "window";
    var tableDiv = null;
    if (DETAILS_WINDOW) {
        winVar = "opener";
        if (jsvDetailsWindow == null) {
            var W = SHOW_ADDR? 600 : 500;
            var H = 300;
            var L = ((screen.width  - W) / 2);
            var T = ((screen.height - H) / 2);
            var attr = "resizable=yes,scrollbars=yes";
            attr += ",width=" + W + ",height=" + H;
            attr += ",screenX=" + L + ",screenY=" + T + ",left=" + L + ",top=" + T;
            jsvDetailsWindow = window.open("", "TrackLocationDetail", attr, true);
            if (jsvDetailsWindow != null) {
                jsvDetailsWindow.document.write("<html>\n");
                jsvDetailsWindow.document.write("<body onunload=\"opener.jsvDetailVisible=false;opener.jsvDetailsWindow=null;\">\n");
                jsvDetailsWindow.document.write("<center><div id='"+ID_DETAIL_TABLE+"'></div></center>");
                jsvDetailsWindow.document.write("</body>\n");
                jsvDetailsWindow.document.write("</html>\n");
                jsvDetailsWindow.moveTo(L,T);
                jsvDetailsWindow.focus();
                tableDiv = jsvDetailsWindow.document.getElementById(ID_DETAIL_TABLE);
            } else {
                // unable to create window?
                return;
            }
        } else {
            jsvDetailsWindow.focus();
        }
    } else {
        winVar = "window";
        tableDiv = document.getElementById(ID_DETAIL_TABLE);
    }
    if (tableDiv == null) {
        return;
    }
    tableDiv.innerHTML = "";

    /* generate HTML table header */
    var html = "";
    var CLASS_DETAILS_TABLE = SORTABLE_LOCATION_DETAILS? CLASS_DETAILS_TABLE_S : CLASS_DETAILS_TABLE_N;
    var CLASS_DETAILS_HEADER_COL = SORTABLE_LOCATION_DETAILS? CLASS_DETAILS_HEADER_COL_S : CLASS_DETAILS_HEADER_COL_N;
    html += "<table id='LocationDetailsReport' class='"+CLASS_DETAILS_TABLE+" table' cellspacing='0' cellpadding='0'>\n";
    html += "<thead>\n";
    if (jsvPartialData) {
        var columns = 4 + (IS_FLEET?1:0) + (SHOW_ADDR?1:0) + (SHOW_SPEED?(COMBINE_SPEED_HEAD?1:2):0);
        html += "<tr class='"+CLASS_DETAILS_HEADER_ROW+"'>";
        html += "<th class='"+CLASS_DETAILS_HEADER_COL+"' colSpan='"+columns+"' valign='center' style=''>"+TEXT_MAXPUSHPINS_MSG+"</th>";
        html += "</tr>\n";
    }
    html += "<tr class='"+CLASS_DETAILS_HEADER_ROW+"'>";
    html += _jsmShowDetailReport_header( 25, "#", CLASS_DETAILS_HEADER_COL); // sort ok
    if (IS_FLEET) { html += _jsmShowDetailReport_header(-1, TEXT_DEVICE, CLASS_DETAILS_HEADER_COL); } // sort ok
    html += _jsmShowDetailReport_header( -1, TEXT_DATE, CLASS_DETAILS_HEADER_COL); // sort ok
    html += _jsmShowDetailReport_header( -1, TEXT_CODE, CLASS_DETAILS_HEADER_COL); // sort ok
    html += _jsmShowDetailReport_header( -1, TEXT_LATLON, CLASS_DETAILS_HEADER_COL_N); // nosort
    if (SHOW_SAT_COUNT) {
        html += _jsmShowDetailReport_header( -1, TEXT_SATCOUNT, CLASS_DETAILS_HEADER_COL); // sort ok
    }
    if (SHOW_SPEED) {
        if (COMBINE_SPEED_HEAD) {
            html += _jsmShowDetailReport_header( -1, TEXT_SPEED, CLASS_DETAILS_HEADER_COL); // sort ok
        } else {
            html += _jsmShowDetailReport_header( -1, TEXT_SPEED, CLASS_DETAILS_HEADER_COL); // sort ok
            html += _jsmShowDetailReport_header( -1, TEXT_HEADING, CLASS_DETAILS_HEADER_COL_N); // nosort
        }
    }
    if (SHOW_ADDR) {
        html += _jsmShowDetailReport_header( -1, TEXT_ADDR, CLASS_DETAILS_HEADER_COL_N); // nosort
    }
    if (SHOW_OPT_FIELDS) {
        var sortClass = CLASS_DETAILS_HEADER_COL_N;
        for (var opti = 0; opti < OptionalEventFieldCount(); opti++) {
            var d = OptionalEventFieldTitle(opti);
            html += _jsmShowDetailReport_header( -1, d, sortClass); // sort/nosort?
        }
    }
    html += "</tr>\n";
    html += "</thead>\n";

    /* generate HTML table body */
    html += "<tbody>\n";
    var lastDevice = "";
    for (var i = 0; i < dpt.length; i++) {
        var pt = jsvDetailAscending? dpt[i] : dpt[dpt.length - i - 1]; // JSDetailPoint
        // new device?
        var isNew = jsvUseDeviceBreaks && (lastDevice != pt.device);
        var dataClass = isNew? CLASS_DETAILS_DATA_COL_NEW : CLASS_DETAILS_DATA_COL;
        lastDevice = pt.device;
        // class
        var isOdd = ((pt.index & 1) == 1);
        var rowClass = isOdd? CLASS_DETAILS_ROW_ODD : CLASS_DETAILS_ROW_EVEN;
        html += "<tr class='"+rowClass+"' id='"+(ID_DETAIL_ROW_+pt.index)+"'";
        if (pt.color != "") {
            var c = pt.color;
            if (DISPLAY_COLOR_TYPE == 1) {
                html += " style='color:"+c+";'";
            } else
            if (DISPLAY_COLOR_TYPE == 2) {
                if (isOdd) {
                    var RGB = rgbLighter(rgbVal(c),0.35);
                    c = "#" + rgbHex(RGB.R,RGB.G,RGB.B);
                }
                html += " style='background-color:"+c+";'";
            }
        }
        html += ">";
        // index
        if (DETAIL_INFO_BOX) {
            html += "<td nowrap class='"+CLASS_DETAILS_INDEX_COL+"' onclick=\"javascript:"+winVar+".jsmShowDetailPushpin("+pt.dsNdx+","+pt.ppNdx+")\">" + pt.index + "</td>";
        } else {
            html += "<td nowrap class='"+dataClass+"'>" + pt.index + "</td>";
        }
        // device id (fleet only)
        if (IS_FLEET) {
            html += "<td nowrap class='"+dataClass+"'>" + pt.device + "</td>";
        }
        // date/time
        html += "<td nowrap class='"+dataClass+"' sorttable_customkey='" + pt.timestamp + "'>" + pt.dateTime + "</td>";
        // status code
        html += "<td nowrap class='"+dataClass+"'>" + pt.code     + "</td>";
        // latitude/longitude
        html += "<td nowrap class='"+dataClass+"'>" + pt.latlon   + "</td>";
        // # Sats
        if (SHOW_SAT_COUNT) {
            html += "<td nowrap class='"+dataClass+"'>" + pt.satCount + "</td>";
        }
        if (SHOW_SPEED) {
            if (COMBINE_SPEED_HEAD) {
                // speed/compass
                var spdHead = (pt.speed > 0)? (pt.speed + " " + pt.compass) : pt.speed;
                html += "<td nowrap class='"+dataClass+"'>" + spdHead  + "</td>";
            } else {
                // speed
                html += "<td nowrap class='"+dataClass+"'>" + pt.speed  + "</td>";
                // heading
                html += "<td nowrap class='"+dataClass+"'>" + pt.heading + "&deg; " + pt.compass + "</td>";
            }
        }
        // address
        if (SHOW_ADDR) {
            html += "<td nowrap class='"+dataClass+"'>" + pt.address + "&nbsp;</td>";
        }
        // optional fields
        if (SHOW_OPT_FIELDS) {
            for (var opti = 0; opti < OptionalEventFieldCount() && (opti < 10); opti++) {
                var v = (pt.optDesc && (opti < pt.optDesc.length))? pt.optDesc[opti] : "";
                html += "<td nowrap class='"+dataClass+"'>" + v + "&nbsp;</td>";
            }
        }
        html += "</tr>\n";
    }
    html += "</tbody>\n";
    html += "</table>\n";

    /* write HTML into table DIV */
    if (DETAILS_WINDOW) {
        // separate window
        tableDiv.innerHTML = html;
    } else {
        // inline (under map)
        var tableHTML = "";
        tableHTML += "<div class='"+CLASS_DETAILS_DIV+"'>" + html + "</div>";
        tableDiv.innerHTML = tableHTML;
        tableDiv.style.display = "block";
        if (SORTABLE_LOCATION_DETAILS) {
            try {
                sorttable.makeSortable(document.getElementById("LocationDetailsReport"));
                //alert("'sorttable.init()' called ...");
            } catch (e) {
                alert("JavaScript function 'sorttable' not found: " + e);
            }
        }
    }

    /* set control to "Hide Location Details" */
    var detailControl = document.getElementById(ID_DETAIL_CONTROL);
    if (detailControl != null) {
        detailControl.innerHTML = TEXT_hideLocationDetails;
    }

};

function _jsmShowDetailReport_header(W, T, C)
{
    var TH = "<th class='"+C+"' nowrap valign='center'"
    if (W > 0) { TH += " width='"+W+"'"; }
    TH += ">"+T+"</th>";
    return TH;
};

function _jsmHighlightDetailRow(rowNdx, highlight)
{
    var detailsWin = DETAILS_WINDOW? jsvDetailsWindow : window;
    if ((detailsWin != null) && (rowNdx >= 1)) {
        var row = detailsWin.document.getElementById(ID_DETAIL_ROW_ + rowNdx);
        if (row != null) {
            if (highlight) {
                row.className = CLASS_DETAILS_ROW_HILITE;
            } else {
                var isOdd = ((rowNdx & 1) == 1);
                row.className = isOdd? CLASS_DETAILS_ROW_ODD : CLASS_DETAILS_ROW_EVEN;
            }
        }
    }
};

function jsmHighlightDetailRow(rcdNdx, highlight)
{
    // remove old highlight
    if (jsvDetailsLastHilightedRow != null) {
        _jsmHighlightDetailRow(jsvDetailsLastHilightedRow.row, false);
        jsvDetailsLastHilightedRow = null;
    }
    // assign new highlight
    var rowNdx = parseInt(rcdNdx);
    if ((rowNdx >= 1) && highlight) {
        _jsmHighlightDetailRow(rowNdx, true);
        if (highlight) {
            jsvDetailsLastHilightedRow = { row:rowNdx };
        }
    }
};

// ----------------------------------------------------------------------------
// --- set replay state

function jsmSetReplayState(state)
{
    // REPLAY_STOPPED
    // REPLAY_PAUSED
    // REPLAY_RUNNING
    try {
        var btn = ID_MAP_REPLAY_BTN? document.getElementById(ID_MAP_REPLAY_BTN) : null;
        if (btn) {
            if (state == REPLAY_RUNNING) {
                // Replay is running, option is "Pause"
                btn.src = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Pause20.png";
            } else
            if (state == REPLAY_PAUSED) {
                // Replay is paused, option is "Continue"
                btn.src = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Continue20.png";
            } else {
                // Replay is stopped, option is "Play"
                btn.src = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Play20.png";
            }
        }
    } catch (e) {
        // may occur if ID_MAP_REPLAY_BTN is not defined
    }
};

// ----------------------------------------------------------------------------
// --- set Lat/Lon/Distance display

/**
*** Returns the distance display 'div' element (or null if not found)
**/
function jsmGetDistanceDisplayElement()
{
    return document.getElementById(ID_DISTANCE_DISPLAY);
};

/**
*** Displays the specified distance value
*** @param distM  The distance value, in meters
**/
function jsmSetDistanceDisplay(distM)
{
    var distItem = jsmGetDistanceDisplayElement();
    if (distItem != null) {
        //if (distM < 1.0) {
        //    distItem.innerHTML = '';
        //} else
        if (jsvGeozoneMode) {
            // always meters
            var d = numFormatFloat(distM,0) + ' ' + TEXT_METERS;
            distItem.innerHTML = d;
        } else {
            // distance units
            var dist = DISTANCE_KM_MULT * distM / 1000.0;
            var d = numFormatFloat(dist,2) + ' ' + TEXT_DISTANCE;
            distItem.innerHTML = d;
        }
    }
};

/**
*** Returns the Lat/Lon display 'div' element (or null if not found)
**/
function jsmGetLatLonDisplayElement()
{
    return document.getElementById(ID_LAT_LON_DISPLAY);
};

/**
*** Displays the specified Lat/Lon value
*** @param lat  The Latitude
*** @param lon  The Longitude
**/
function jsmSetLatLonDisplay(lat,lon)
{
    var latlonItem = jsmGetLatLonDisplayElement();
    if (latlonItem != null) {
        var dec = 4; // jsvGeozoneMode? 5 : 4; // lat/lon decimal points
        if ((lat == 0) && (lon == 0)) {
            // may want to handle this case differently
            var loc = jsmFormatCoord(0.0,true ,dec) + ', ' + jsmFormatCoord(0.0,false,dec)
            latlonItem.innerHTML = loc;
        } else {
            // display latitude/longitude
            var loc = jsmFormatCoord(lat,true ,dec) + ', ' + jsmFormatCoord(lon,false,dec)
            latlonItem.innerHTML = loc;
        }
    }
};

// ----------------------------------------------------------------------------
// --- set Zone values

/**
*** Sets/Displays the specified GeoZone value
*** @param lat      The Zone Latitude
*** @param lon      The Zone Longitude
*** @param radiusM  The Zone radius, in meters
**/
function jsmSetPointZoneValue(lat, lon, radiusM)
{
    var ndx = zoneMapGetIndex();
    _jsmSetPointZoneValue(ndx, lat, lon, radiusM);
};

/**
*** Sets/Displays the specified GeoZone value
*** @param lat      The Zone Latitude
*** @param lon      The Zone Longitude
*** @param radiusM  The Zone radius, in meters
**/
function _jsmSetPointZoneValue(ndx, lat, lon, radiusM)
{
    if ((ndx >= 0) && (ndx < jsvZoneList.length)) {

        /* set array values */
        jsvZoneList[ndx].lat = lat;
        jsvZoneList[ndx].lon = lon;

        /* set GeoZone display values */
        jsmSetIDValue(ID_ZONE_LATITUDE_  + ndx, numFormatFloat(lat,5));
        jsmSetIDValue(ID_ZONE_LONGITUDE_ + ndx, numFormatFloat(lon,5));

        /* set radius */
        if (radiusM >= 0) {
            jsmSetIDValue(ID_ZONE_RADIUS_M, radiusM);
        }

        /* display (remove for production?) */
        jsmSetLatLonDisplay(lat, lon);
        jsmSetDistanceDisplay(radiusM);

    }
};

// ----------------------------------------------------------------------------
// --- misc

/**
*** Gets the 'value' of the specified element
**/
function jsmGetElementValue(item)
{
    return (item != null)? item.value : null;
};

/**
*** Sets the 'value' of the specified element
**/
function jsmSetElementValue(item, value)
{
    if (item != null) {
        item.value = value;
    }
};

/**
*** Gets the 'value' of the specified element ID
**/
function jsmGetIDValue(idName)
{
    return jsmGetElementValue(document.getElementById(idName));
};

/**
*** Sets the 'value' of the specified element ID
**/
function jsmSetIDValue(idName, value)
{
    jsmSetElementValue(document.getElementById(idName), value);
};

/**
*** Sets the 'innerHTML' of the specified element
**/
function jsmSetElementInnerHTML(item, html)
{
    if (item != null) {
        item.innerHTML = html;
    }
};

/**
*** Sets the 'innerHTML' of the specified element ID
**/
function jsmSetIDInnerHTML(idName, html)
{
    jsmSetElementInnerHTML(document.getElementById(idName), html);
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** Creates and returns a custom map marker URL based on the Google Chart api.
*** Developer's Guide: http://code.google.com/apis/chart/
*** Usage Policy     : http://code.google.com/apis/chart/#usage
**/
function jsmCustomMapMarker(W, H, C)
{
    // http://chart.apis.google.com/chart?cht=mm&ext=.png&chs=18x30&chco=FF0000FF,00FF00FF,000000FF
    // The following assumptions are made regarding the generated map marker image:
    //  - The 'corner' color is the same as the 'fill' color
    //  - The border is always black
    //  - No transparency
    return "https://chart.apis.google.com/chart?cht=mm&ext=.png&chs="+W+"x"+H+"&chco="+C+"FF,"+C+"FF,000000FF";
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

var mapControlVisible = true;

/**
*** Toggle Map Control display
**/
function jsmControlToggleCollapse()
{
    var mapCtl    = (ID_MAP_CONTROL    )? document.getElementById(ID_MAP_CONTROL)     : null;
    var mapCtlBar = (ID_MAP_CONTROL_BAR)? document.getElementById(ID_MAP_CONTROL_BAR) : null;
    if ((mapCtl != null) && (CLASS_CONTROL_BAR)) {
        if (mapControlVisible) {
            // is visible, make invisible
            mapCtl.style.display = "none";
            if (mapCtlBar) { mapCtlBar.className = CLASS_CONTROL_BAR[1]; }
            mapControlVisible = false;
        } else {
            // is invisible, make visible
            mapCtl.style.display = "";
            if (mapCtlBar) { mapCtlBar.className = CLASS_CONTROL_BAR[0]; }
            mapControlVisible = true;
        }
    }
};

/**
*** Set Map Control display
**/
function jsmControlDisplay(expand)
{
    var mapCtl    = (ID_MAP_CONTROL    )? document.getElementById(ID_MAP_CONTROL)     : null;
    var mapCtlBar = (ID_MAP_CONTROL_BAR)? document.getElementById(ID_MAP_CONTROL_BAR) : null;
    if ((mapCtl != null) && (CLASS_CONTROL_BAR)) {
        if (expand) {
            if (mapControlVisible) {
                // already visible
            } else {
                // is invisible, make visible
                mapCtl.style.display = "";
                if (mapCtlBar) { mapCtlBar.className = CLASS_CONTROL_BAR[0]; }
                mapControlVisible = true;
            }
        } else {
            if (mapControlVisible) {
                // is visible, make invisible
                mapCtl.style.display = "none";
                if (mapCtlBar) { mapCtlBar.className = CLASS_CONTROL_BAR[1]; }
                mapControlVisible = false;
            } else {
                // already invisible
            }
        }
    }
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** Pushpin:
*** Calculate and return a color based on the event speed.
*** This algorithm performs the following:
***  - If the speed is greater than 70 mph, the returned color is green.
***  - If the speed is between 20 and 70 mph, the color will be a mix between yellow and green.
***  - If the speed is between  0 and 20 mph, the color will be a mix between red and yellow.
*** @param e  The 'MapEventRecord' object
**/
function evSpeedColor(e)
{
    var mph = e.speedMPH;
    var green  = [   0, 210, 0 ];
    var yellow = [ 240, 240, 0 ];
    var red    = [ 255,   0, 0 ];
    if (mph > 70.0) {
        // return green
        return rgbHex(green[0], green[1], green[2]);
    } else
    if (mph >= 20.0) {
        // fade from yellow to green
        var D = (mph - 20.0) / (70.0 - 20.0);
        var R = ((green[0] - yellow[0]) * D) + yellow[0];
        var G = ((green[1] - yellow[1]) * D) + yellow[1];
        var B = ((green[2] - yellow[2]) * D) + yellow[2];
        return rgbHex(R, G, B);
    } else
    if (mph >= 0.0) {
        // fade from red to yellow
        var D = (mph - 0.0) / (20.0 - 0.0);
        var R = ((yellow[0] - red[0]) * D) + red[0];
        var G = ((yellow[1] - red[1]) * D) + red[1];
        var B = ((yellow[2] - red[2]) * D) + red[2];
        return rgbHex(R, G, B);
    } else {
        // return black
        return "000000";
    }

};

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event speed.
*** Analyzes the event 'speed' and creates an icon marker URL on the fly that fades from
*** RED (stopped) to YELLOW (slow) to GREEN (fast).  It uses the Google Charts API to create the map
*** marker icon [http://code.google.com/apis/chart/].  The first 2 arguments are the desired icon
*** width and height (required since it is dynamically creating this icon).
*** Example usage (in the Pushpins tag, within a MapProvider):
***   <Pushpin key="moving" eval="evSpeedMarkerURL(16,24,e)" iconSize="16,24" iconHotspot="8,24"/>
*** @param W  The pushpin width
*** @param H  The pushpin height
*** @param e  The 'MapEventRecord' object
**/
function evSpeedMarkerURL(W,H,e)
{
    return jsmCustomMapMarker(W, H, evSpeedColor(e));
};

// ----------------------------------------------------------------------------

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event age, based on the current time.
*** @param e  The 'MapEventRecord' object
**/
function evCurrentAgeMarkerURL(e)
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30";
    var nowTime  = (new Date()).getTime() / 1000;
    var deltaSec = (nowTime > e.timestamp)? (nowTime - e.timestamp) : 0;
    if (deltaSec <= (    15 * 60)) {    // <= 15 minutes
        // 0 < X <= 15min
        return ppImg + "_green.png";
    } else
    if (deltaSec <= (1 * 60 * 60)) {    // <= 1 hour
        // 15min < X <= 1hr
        return ppImg + "_yellow.png";
    } else
    if (deltaSec <= (6 * 60 * 60)) {    // <= 6 hour
        // 1hr < X <= 6hr
        return ppImg + "_red.png";
    } else {
        // 6hr < X
        return ppImg + "_red_dot.png";
    }
};

// ----------------------------------------------------------------------------

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event creation age (time difference between
*** when the event was generated in the device and when it finally sent it to the
*** server).
*** @param e  The 'MapEventRecord' object
**/
function evCreateAgeMarkerURL(e)
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30";
    var deltaSec = e.createAge;
    if (deltaSec <= (    15 * 60)) {    // <= 15 minutes
        // 0 < X <= 15min
        return ppImg + "_green.png";
    } else
    if (deltaSec <= (1 * 60 * 60)) {    // <= 1 hour
        // 15min < X <= 1hr
        return ppImg + "_yellow.png";
    } else
    if (deltaSec <= (6 * 60 * 60)) {    // <= 6 hour
        // 1hr < X <= 6hr
        return ppImg + "_red.png";
    } else {
        // 6hr < X
        return ppImg + "_red_dot.png";
    }
};

// ----------------------------------------------------------------------------

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event GPS age (time difference between
*** when the event was generated in the device and when it finally sent it to the
*** server).
*** @param e  The 'MapEventRecord' object
**/
function evGpsAgeMarkerURL(e)
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30";
    var deltaSec = e.gpsAge;
    if (deltaSec <= (    15 * 60)) {    // <= 15 minutes
        // 0 < X <= 15min
        return ppImg + "_green.png";
    } else
    if (deltaSec <= (1 * 60 * 60)) {    // <= 1 hour
        // 15min < X <= 1hr
        return ppImg + "_yellow.png";
    } else
    if (deltaSec <= (6 * 60 * 60)) {    // <= 6 hour
        // 1hr < X <= 6hr
        return ppImg + "_red.png";
    } else {
        // 6hr < X
        return ppImg + "_red_dot.png";
    }
};

// ----------------------------------------------------------------------------

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingMarkerURL(e) // US
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30"; // "extra/images/pp/circle"
    if ((MAX_CREATION_AGE_SEC > 0) && (e.createAge > MAX_CREATION_AGE_SEC)) {
        if ((e.stopped > 0) || (e.speedKPH < 5.0)) {
            return ppImg + "_black.png";
        } else {
            var x = Math.round(e.heading / 45.0) % 8;
            return ppImg + "_black_h"+x+".png";
        }
    } else
    if (e.stopped > 0) {
        // stopped
        if (e.stopped == 2) {
            // stop-event
            return ppImg + "_red_dot.png";
        } else {
            // general stopped
            return ppImg + "_red.png";
        }
    } else
    if (e.speedKPH <  5.0/*km/h*/) { // 3.107 mph
        // probably not moving
        return ppImg + "_red_dot.png";
    } else
    if (e.speedKPH < 32.0/*km/h*/) { // 19.884 mph
        // 5 <= X < 32
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_yellow_h"+x+".png";
    } else {
        // 32 <= X
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_green_h"+x+".png";
    }
};

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingMarkerURL_eu(e) // Europe
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30"; // "extra/images/pp/circle"
    var speedMPH = e.speedKPH * 0.621371192; // 1/1.609344;
    if (e.stopped > 0) {
        // stopped
        if (e.stopped == 2) {
            // stop-event
            return ppImg + "_red_dot.png";
        } else {
            // general stopped
            return ppImg + "_red.png";
        }
    } else
    if (speedMPH <  5.0) {
        // probably not moving
        return ppImg + "_red_dot.png";
    } else
    if (speedMPH < 50.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_yellow_h"+x+".png";
    } else
    if (speedMPH < 90.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_green_h"+x+".png";
    } else
    if (speedMPH < 110.0) {
        return ppImg + "_gray.png";
    } else {
        return ppImg + "_black.png";
    }
};

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingMarkerURL_ca(e) // Canada
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30"; // "extra/images/pp/circle"
    if (e.stopped > 0) {
        // stopped
        if (e.stopped == 2) {
            // stop-event
            return ppImg + "_red_dot.png";
        } else {
            // general stopped
            return ppImg + "_red.png";
        }
    } else
    if (e.speedKPH <  1.0) {
        // probably not moving
        return ppImg + "_red.png";
    } else
    if (e.speedKPH < 70.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_yellow_h"+x+".png";
    } else
    if (e.speedKPH < 100.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_green_h"+x+".png";
    } else
    if (e.speedKPH < 130.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_blue_h"+x+".png";
    } else {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_gray_h"+x+".png";
    }
};

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingMarkerURL_CC(e) // Custom
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30"; // "extra/images/pp/circle"
    var HEADING_MARKER_SPEED_RED     =  10.0; // <  10 km/h
    var HEADING_MARKER_SPEED_YELLOW  =  60.0; // <  60 km/h
    var HEADING_MARKER_SPEED_GREEN   =  90.0; // <  90 km/h
    var HEADING_MARKER_SPEED_BLUE    = 110.0; // < 110 km/h
    if (e.stopped > 0) { // stopped
        if (e.stopped == 2) { // stop-event
            return ppImg + "_red_dot.png";
        } else { // general stopped
            return ppImg + "_red.png";
        }
    } else
    if (e.speedKPH < HEADING_MARKER_SPEED_RED) {
        return ppImg + "_red.png";
    } else
    if (e.speedKPH < HEADING_MARKER_SPEED_YELLOW) {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_yellow_h"+x+".png";
    } else
    if (e.speedKPH < HEADING_MARKER_SPEED_GREEN) {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_green_h"+x+".png";
    } else
    if (e.speedKPH < HEADING_MARKER_SPEED_BLUE) {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_blue_h"+x+".png";
    } else {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_black_h"+x+".png";
    }
};

/**
*** Pushpin:
*** Returns a pushpin/icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingMiniDotURL_ar(e) // Argentina
{
    //if ((MAX_CREATION_AGE_SEC > 0) && (e.createAge > MAX_CREATION_AGE_SEC)) {
    //    return "images/led/led16_black.png";
    //} else
    if (e.stopped > 0) {
        // -- stopped
        return "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/mini5/mini5_red.png";
    } else
    if (e.speedKPH <  30.0/*km/h*/) { //
        // X < 30
        return "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/mini5/mini5_red.png";
    } else
    if (e.speedKPH < 100.0/*km/h*/) { //
        // 30 <= X < 100
        return "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/mini5/mini5_yellow.png";
    } else {
        // 100 <= X
        return "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/mini5/mini5_green.png";
    }
};

// ----------------------------------------------------------------------------

/**
*** Pushpin:
*** Returns a 'yellow' pushpin/icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingYellowURL(e)
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30"; // "extra/images/pp/circle"
    if ((MAX_CREATION_AGE_SEC > 0) && (e.createAge > MAX_CREATION_AGE_SEC)) {
        if (e.speedKPH < 1.0) {
            return ppImg + "_black.png";
        } else {
            var x = Math.round(e.heading / 45.0) % 8;
            return ppImg + "_black_h"+x+".png";
        }
    } else
    if (e.speedKPH < 1.0) {
        // probably not moving
        return ppImg + "_yellow.png";
    } else {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_yellow_h"+x+".png";
    }
};

/**
*** Pushpin:
*** Returns a 'green' pushpin/icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingGreenURL(e)
{
    var ppImg = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30"; // "extra/images/pp/circle"
    if ((MAX_CREATION_AGE_SEC > 0) && (e.createAge > MAX_CREATION_AGE_SEC)) {
        if (e.speedKPH < 1.0) {
            return ppImg + "_black.png";
        } else {
            var x = Math.round(e.heading / 45.0) % 8;
            return ppImg + "_black_h"+x+".png";
        }
    } else
    if (e.speedKPH < 1.0) {
        // probably not moving
        return ppImg + "_green_dot.png";
    } else {
        var x = Math.round(e.heading / 45.0) % 8;
        return ppImg + "_green_h"+x+".png";
    }
};

/**
*** Pushpin:
*** Returns a 'target' pushpin: 'green' if moving fast, 'yellow' if moving
*** slow, and 'red' if stopped.
*** Note: "CrosshairXXXX.gif" images may need to be replaced with your
****      Custom images, if not present in this version.
*** @param e  The 'MapEventRecord' object
**/
function evSpeedLastURL(e)
{
    if (e.speedKPH < 1.0) {
        // probably not moving
        return "extra/images/pp/CrosshairRed.gif";
    } else
    if (e.speedKPH < 40.0) {
        return "extra/images/pp/CrosshairYellow.gif";
    } else {
        return "extra/images/pp/CrosshairGreen.gif";
    }
};

// ----------------------------------------------------------------------------

/**
*** Pushpin:
*** Return the Green pushpin URL path if the specified digital input it ON.
*** Return the Red pushpin URL path if the specified digital input it OFF.
**/
function evGpioMarkerURL(I,e)
{
    if ((e.gpioInput & (1 << I)) != 0) {
        return "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30_green.png";
    } else {
        return "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30_red.png";
    }
};

// ----------------------------------------------------------------------------
// custom label markers

/**
*** Pushpin:
*** Returns a pushpin/icon URL which includes the event index
*** @param e  The 'MapEventRecord' object
**/
function evIndexedIconURL(e)
{
    // http://DOMAIN/track/Marker?icon=/images/pp/pin30_blue_fill.png?fr=3,4,11,7,9,Serif&color=880000&text=99
    var tx = e.index;
    return evTextLabelIconURL("https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/pp/pin30_blue_fill.png", "3,4,11,7,9,Serif", "", "", "880000", tx);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes the device short name (up to 7 digits, 5/6 chars)
*** @param e  The 'MapEventRecord' object
**/
function evDeviceNameIconURL(e, fill, border)
{
    // http://DOMAIN/track/Marker?icon=#White_47x1&text=Demo2&border=red&fill=yellow
    var tx = e.devVIN;
    var fc = fill?   fill   : "";
    var bc = border? border : "";
    return evTextLabelIconURL("#White_47x1","",fc,bc,"",tx);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes the device short name (up to 10 digits, 8/9 chars)
*** @param e  The 'MapEventRecord' object
**/
function evDeviceNameIconURL_W(e, fill, border)
{
    // http://DOMAIN/track/Marker?icon=#White_67x1&text=1234567890
    var tx = e.devVIN;
    var fc = fill?   fill   : "";
    var bc = border? border : "";
    return evTextLabelIconURL("#White_67x1","",fc,bc,"",tx);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes the following:
***   - Device Short-Name
***   - Speed
*** @param e  The 'MapEventRecord' object
**/
function evDeviceNameIconURL_PPL2(e, fill, border)
{
    // http://DOMAIN/track/Marker?icon=#White_67x2_pp&text=1234567890
    var tx = "";
    tx += e.devVIN + "\n";
    tx += numFormatFloat(e.speedKPH * SPEED_KPH_MULT, 0) + " " + SPEED_UNITS;
    var fc = fill?   fill   : "#EEF5F5F5";
    var bc = border? border : "";
    return evTextLabelIconURL("#White_67x2_pp","",fc,bc,"",tx);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes the following:
***   - Device Short-Name
***   - Timestamp (HH:MM:SS)
***   - Speed
*** @param e  The 'MapEventRecord' object
**/
function evDeviceNameIconURL_PPL3(e, fill, border)
{
    // http://DOMAIN/track/Marker?icon=#White_67x3_pp&text=1234567890
    var tx = "";
    tx += e.devVIN + "\n";
    tx += /*"%{-2,RED}"+*/ e.timeFmt + "\n";
    tx += numFormatFloat(e.speedKPH * SPEED_KPH_MULT, 0) + " " + SPEED_UNITS;
    var fc = fill?   fill   : "#EEF5F5F5";
    var bc = border? border : "";
    return evTextLabelIconURL("#White_67x3_pp","",fc,bc,"",tx);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes the device short name,
*** with a background color that is based on the vehicle speed
*** @param e  The 'MapEventRecord' object
**/
function evDeviceNameSpeedIconURL(e)
{
    // http://DOMAIN/track/Marker?icon=#White_47x1&text=Demo2&border=red&fill=yellow
    // - get fill color based on speed
    var fc = "";
    var bc = "";
    if (e.speedKPH < 5.0) {
        fc = "FF8888"; // red (probably not moving)
    } else
    if (e.speedKPH < 32.0) {
        fc = "FFFF88"; // yellow (5 <= X < 32)
    } else {
        fc = "88FF88"; // green (32 <= X)
    }
    // - create URL
    var tx = e.devVIN;
    return evTextLabelIconURL("#White_47x1","",fc,bc,"",tx);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes the ID/Name at the specified index
*** @param e  The 'MapEventRecord' object
*** @param x  The 'optional' field index with the event object
**/
function evOptFieldIconURL(e,x)
{
    var tx = (SHOW_OPT_FIELDS && e.optDesc && (x >= 0) && (x < e.optDesc.length))? e.optDesc[x] : "";
    return evTextLabelIconURL("#White_47x1","","","yellow","",tx);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes the device short name
*** @param e    The 'MapEventRecord' object
*** @param icon The image URL
*** @param fr   The draw 'frame'
*** @param fill The fill-color
**/
function evLabelIconURL(e, icon, fr, fill)
{
    var text = e.devVIN;
    return evTextLabelIconURL(icon, fr, fill, "", "", text);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes the device short name
**/
function evTextLabelIconURL(icon, fr, fill, border, color, text)
{
    // Marker?icon=/images/pp/label47_fill.png&fr=3,2,42,13,8&text=Demo2&border=red&fill=yellow
    // Options:
    //   icon=<PathToImageIcon>                             - icon URI (relative to "/track/")
    //   fr=<XOffset>,<YOffset>,<Width>,<Height>,<FontPt>   - text frame definition
    //   fill=<FillColor>                                   - text frame fill color
    //   border=<BorderColor>                               - text frame border color
    //   color=<TextColor>                                  - text color
    //   text=<ShortText>                                   - text
    var url =
        "Marker?"  +
        "icon="    + strEncode(icon) +
        "&fr="     + strEncode(fr) +
        "&fill="   + strEncode(fill) +
        "&border=" + strEncode(border) +
        "&color="  + strEncode(color) +
        "&text="   + strEncode(text);
    //alert("Marker URL: " + url);
    return url;
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes an arrow pointing in the specified direction
*** @param e    The 'MapEventRecord' object
*** @param icon The image URL
*** @param fr   The draw 'frame'
**/
function evArrowIconURL(e, icon, fr, c)
{
    var arrow = (e.speedKPH > 0)? e.heading : 360.0;
    var color = "000000";
    if (c) {
        color = c;
    } else
    if (e.speedKPH < 5.0) {
        color = "DD0000"; // red (probably not moving)
    } else
    if (e.speedKPH < 32.0) {
        color = "FFD700"; // yellow (5 <= X < 32)
    } else {
        color = "00CC00"; // green (32 <= X)
    }
    return evArrowLabelIconURL(icon, fr, "", "", color, arrow);
};

/**
*** Pushpin:
*** Returns a label pushpin/icon URL which includes an arrow pointing in the specified direction
**/
function evArrowLabelIconURL(icon, fr, fill, border, color, heading)
{
    // Marker?icon=/images/pp/label47_fill.png&fr=3,2,42,13,10&border=red&fill=yellow&arrow=120
    // Options:
    //   icon=<PathToImageIcon>                             - icon URI (relative to "/track/")
    //   fr=<XOffset>,<YOffset>,<Width>,<Height>,<FontPt>   - frame definition
    //   fill=<FillColor>                                   - frame fill color
    //   border=<BorderColor>                               - frame border color
    //   color=<TextColor>                                  - arrow color
    //   arrow=<Heading>                                    - arrow direction
    var url =
        "Marker?"  +
        "icon="    + strEncode(icon) +
        "&fr="     + strEncode(fr) +
        "&fill="   + strEncode(fill) +
        "&border=" + strEncode(border) +
        "&color="  + strEncode(color) +
        "&arrow="  + strEncode(heading);
    //alert("Marker URL: " + url);
    return url;
};

/**
*** Pushpin:
*** Returns a Google Chart custom bubble icon URL which includes the device short name
*** @param e   The 'MapEventRecord' object
*** @param bgc The background color
*** @param fgc The foreground color
**/
function evDeviceNameBubbleURL(e, bgc, fgc)
{
    // http://chart.googleapis.com/chart?chst=d_bubble_text_small&chld=bb|Hello%20World|FFFFFF|AA0000
    // Note: The anchor is at 0,0.   The icon width depends on the width of the text in the icon.
    var tx  = e.devVIN;
    var url =
        "https://chart.googleapis.com/chart?chst=d_bubble_text_small" +
        "&chld=bb|"+tx+"|"+bgc+"|"+fgc;
    return url;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** Returns an icon URL based on the event battery level
*** @param e  The 'MapEventRecord' object
**/
function jsmBatteryLevelIMG(lvl)
{
    if (lvl > 1.5) { lvl = lvl / 100.0; }
    var battLevelType = 1;
    try {
        battLevelType = BATTERY_LEVEL_TYPE;
    } catch (e) {
        battLevelType = 1;
    }
    var battIcon = "";
    if (battLevelType == 2) {
        // percent
        if (lvl <= 0.01) {
            battIcon = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Batt000.png";
        } else {
            var battLvl = numParseInt(((lvl * 100.0) + 0.5), 0);
            if (battLvl > 99) { battLvl = 99; }
            battIcon = "Marker?icon=https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Batt000.png&fr=5,2,25,12,10&text="+battLvl+"%25";
        }
    } else {
        //icon
        if (lvl <= 0.01) {
            battIcon = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Batt000.png";
        } else
        if (lvl <= 0.25) {
            battIcon = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Batt025.png";
        } else
        if (lvl <= 0.50) {
            battIcon = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Batt050.png";
        } else
        if (lvl <= 0.70) {
            battIcon = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Batt070.png";
        } else
        if (lvl <= 0.90) {
            battIcon = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Batt090.png";
        } else {
            battIcon = "https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Batt100.png";
        }
    }
    return "<img src=\""+battIcon+"\"/>";
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- accessed by TrackMap/ZoneInfo/ReportDisplay

var recursiveRefresh = 0;

/**
*** Device ping
**/
function mapDevicePing(pingURL)
{ // required function
    jsmDevicePing(pingURL);
};

/**
*** Parse CSV points and display on map
*** @param zonePoints  Array of JSMapPoint's
**/
function mapProviderParseZones(zonePoints)
{
    jsMapInit();
    jsmParseGeozones(zonePoints);
};

/**
*** Parse XML/JSON points and display on map
**/
function mapProviderParseXML(xmlText)
{ // required function
    jsMapInit();
    jsmParseAJAXPoints(xmlText, jsmRecenterZoomMode(RECENTER_ZOOM), 0);
};

/**
*** Update map, and recenter if specified
**/
function mapProviderUpdateMap(mapURL, recenterMode, replay)
{ // required function
    if (recursiveRefresh != 0) { return; } // we're already in a 'update'
    recursiveRefresh++;
    jsMapInit();
    jsmLoadPoints(mapURL, recenterMode, replay);
    recursiveRefresh--;
};

/**
*** Stop replay (if running)
**/
function mapProviderPauseReplay(replay)
{
    return jsmap? jsmap.JSPauseReplay(replay) : REPLAY_STOPPED;
};

/**
*** Display "Location Details" report if it is hidden, else hide
**/
function mapProviderToggleDetails()
{ // required function
    jsvDetailVisible = DETAILS_WINDOW? true : !jsvDetailVisible;
    jsmShowDetailReport();
};

/**
*** Unload any map resources
**/
function mapProviderUnload()
{ // required function
    jsmUnload();
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- Geocode address, return location

function googleGeocodeAddress(address) {
    var addr  = strEncode(address);
	var rgURL = "https://maps.googleapis.com/maps/api/geocode/json?address=" + addr + "&sensor=false";
	try {
	    var req  = jsmGetXMLHttpRequest();
	    if (req) {
            req.open("GET", rgURL, true);
            //req.setRequestHeader("CACHE-CONTROL", "NO-CACHE");
            //req.setRequestHeader("PRAGMA", "NO-CACHE");
            req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
            req.send(null);
            var rgObj = JSON.parse(req.responseText);
            var status = rgObj.status;
            if (status && (status === "OK")) {
                var location = rgObj.results[0].geometry.location;
                return location;
            } else {
                //alert("Invalid Status Response [geocodeAddress]: " + status);
                return null;
            }
	    } else {
	        return null;
	    }
    } catch (e) {
        //alert("Error [geocodeAddress]:\n" + e);
        return null;
	}
};

// ----------------------------------------------------------------------------
