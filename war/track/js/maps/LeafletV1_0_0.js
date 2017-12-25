// ----------------------------------------------------------------------------
// Copyright(C) 2007-2017 GeoTelematic Solutions, Inc.
// ----------------------------------------------------------------------------
// Version: 1.0.0-K
// ----------------------------------------------------------------------------
// Required funtions defined by this module:
//   new JSMap(String mapID)
//   JSClearLayers()
//   JSSetCenter(JSMapPoint center [, int zoom])
//   JSDrawPushpins(JSMapPushpin pushPin[], int recenterMode, int replay)
//   JSDrawPOI(JSMapPushpin pushPin[])
//   JSDrawRoute(JSMapPoint points[], String color)
//   JSDrawShape(String type, double radius, JSMapPoint points[], String color, boolean zoomTo, String desc, int ppNdx)
//   JSDrawGeozone(int type, double radius, JSMapPoint points[], String color, int primaryIndex)
//   JSShowPushpin(JSMapPushpin pushPin, boolean center)
//   JSPauseReplay(int replay)
//   JSUnload()
// ----------------------------------------------------------------------------
// References:
//  - http://leafletjs.com/reference-1.0.0.html
//  - https://docs.onemap.sg/maps/
//      Singaore Land Authority
// ----------------------------------------------------------------------------
// Change History:
//  2016/09/01  Martin D. Flynn
//     -Initial release
//  2017/05/02  Martin D. Flynn
//     -Leaflet "L.latlngBounds()" appears to return null/undefined in some browser/map
//      environments.   Replaced "L.latlngBounds()" with "new L.LatlngBounds()".
// ----------------------------------------------------------------------------

var DRAG_NONE               = 0x00;
var DRAG_RULER              = 0x01;
var DRAG_GEOZONE            = 0x10;
var DRAG_GEOZONE_CENTER     = 0x11;
var DRAG_GEOZONE_RADIUS     = 0x12;

// ----------------------------------------------------------------------------

function LeafletColorStyle(borderColor, borderOpacity, fillColor, fillOpacity)
{
    this.strokeColor   = borderColor;
    this.strokeOpacity = borderOpacity;
    this.strokeWidth   = 1;
    this.fillColor     = fillColor;
    this.fillOpacity   = fillOpacity;
};

var GEOZONE_STYLE = [
    new LeafletColorStyle("#CC1111", 0.80, "#11CC22", 0.28), /* primary */
    new LeafletColorStyle("#11CC11", 0.80, "#11CC22", 0.18)
    ];
    
function GetGeozoneStyle(isPrimary, fillColor)
{
    var s = GEOZONE_STYLE[isPrimary? 0 : 1];
    if (fillColor && (fillColor != "")) {
        return new LeafletColorStyle(
            s.strokeColor, s.strokeOpacity,
            fillColor, s.fillOpacity);
    } else {
        return s;
    }
};

// ----------------------------------------------------------------------------

/**
*** Creates the Leaflet map instance
**/
function CreateLeafletMap(mapElement)
{
    if (CREATE_MAP_CALLBACK) {
        // -- custom Leaflet map
        return CREATE_MAP_CALLBACK(mapElement);
    } else {
        // -- default Leaflet map
        return L.map(mapElement.id);
        //{
        //    center: L.LatLng(DEFAULT_CENTER.lat,DEFAULT_CENTER.lon),
        //    zoom: 13
        //});
    }
};

/**
*** JSMap constructor
**/
function JSMap(mapElement)
{

    /* custom fix for MSIE */
    this.userAgent_MSIE = /MSIE/.test(navigator.userAgent);

    /* crosshair mouse cursor */
    mapElement.style.cursor = "crosshair";

    /* create map object */
    this.leafletMap = CreateLeafletMap(mapElement);
    // --
    var dftCenter = this._toLeafletPointLatLon(DEFAULT_CENTER.lat, DEFAULT_CENTER.lon);
    var dftZoom   = 13;
    this.leafletMap.setView(dftCenter, dftZoom);

    /* initialize map layers */
    var mapLayer = []
    if ((LEAFLET_CUSTOM1_NAME != "") && (LEAFLET_CUSTOM1_URL != "")) {
        var map = this._initCustomMap(mapElement,
            LEAFLET_CUSTOM1_NAME,
            LEAFLET_CUSTOM1_URL,
            LEAFLET_CUSTOM1_SUBDOMAINS,
            LEAFLET_CUSTOM1_MAX_ZOOM,
            LEAFLET_CUSTOM1_COPYRIGHT,
            LEAFLET_CUSTOM1_INIT_FTN);
        mapLayer.push(map);
    }
    if ((LEAFLET_CUSTOM2_NAME != "") && (LEAFLET_CUSTOM2_URL != "")) {
        var map = this._initCustomMap(mapElement,
            LEAFLET_CUSTOM2_NAME,
            LEAFLET_CUSTOM2_URL,
            LEAFLET_CUSTOM2_SUBDOMAINS,
            LEAFLET_CUSTOM2_MAX_ZOOM,
            LEAFLET_CUSTOM2_COPYRIGHT,
            LEAFLET_CUSTOM2_INIT_FTN);
        mapLayer.push(map);
    }
    if ((LEAFLET_CUSTOM3_NAME != "") && (LEAFLET_CUSTOM3_URL != "")) {
        var map = this._initCustomMap(mapElement,
            LEAFLET_CUSTOM3_NAME,
            LEAFLET_CUSTOM3_URL,
            LEAFLET_CUSTOM3_SUBDOMAINS,
            LEAFLET_CUSTOM3_MAX_ZOOM,
            LEAFLET_CUSTOM3_COPYRIGHT,
            LEAFLET_CUSTOM3_INIT_FTN);
        mapLayer.push(map);
    }
    if (LEAFLET_ADD_OPEN_STREET_MAP) {
        var map = this._initOpenStreetMap(mapElement);
        mapLayer.push(map);
    }
    if (LEAFLET_ADD_OPEN_CYCLE_MAP) {
        var map = this._initOpenCycleMap(mapElement);
        mapLayer.push(map);
    }
    if (mapLayer.length > 0) {
        this.leafletMap.addLayer(mapLayer[0]); // add first mapLayer
        if (mapLayer.length > 1) {
            var baseMaps = { };
            for (var i = 0; i < mapLayer.length; i++) {
                var map  = mapLayer[i];
                var name = map.gtsMapName;
                baseMaps[name] = map;
            }
            var layerCtl = L.control.layers(baseMaps).addTo(this.leafletMap);
        }
    } else {
        console.log("WARN: no map layers added!");
    }

    /* attribution prefix */
    if (LEAFLET_ATTRIBUTION_PREFIX != "") {
        var pfx = (LEAFLET_ATTRIBUTION_PREFIX == "blank")? "" : LEAFLET_ATTRIBUTION_PREFIX;
        this.leafletMap.attributionControl.setPrefix(pfx);
    }

    /* center bounds (JSBounds is used because L.latLngBounds(..) has issues with MapBox) */
    this.centerBounds = new JSBounds(); // L.latLngBounds();

    /* "ruler" layer */
    this.rulerFeatures = null;
    this.rulerLayer = null;
    try {
        this.rulerLayer = L.featureGroup();
        this.leafletMap.addLayer(this.rulerLayer);
    } catch (e) {
        alert("Error: Creating ruler layer - " + e);
    }

    /* POI layer */
    this.poiLayer = null;
    try {
        this.poiLayer = L.featureGroup();
        this.leafletMap.addLayer(this.poiLayer);
    } catch (e) {
        alert("Error: Creating POI layer - " + e);
    }

    /* route/zone layer */
    this.routeLines    = [];        // JSMapPoint[]
    this.primaryIndex  = -1;
    this.primaryCenter = null;      // L.LatLng
    this.geozonePoints = null;      // JSMapPoint[]
    this.dragZoneOffsetLat = 0.0;
    this.dragZoneOffsetLon = 0.0;
    this.drawFeatures = null;
    this.drawShapes = [];
    this.drawLayer = [];
    try {
        this.drawLayer.push(L.featureGroup());
        this.leafletMap.addLayer(this.drawLayer[this.drawLayer.length - 1]);
    } catch (e) {
        alert("Error: Creating Draw layer - " + e);
    }

    /* marker/pushpin layer */
    this.markerLayer = null;
    try {
        this.markerLayer = L.featureGroup();
        this.markerLayer.addTo(this.leafletMap);
    } catch (e) {
        alert("Error: Creating Marker layer - " + e);
    }

    /* replay vars */
    this.replayTimer = null;
    this.replayIndex = 0;
    this.replayInterval = (REPLAY_INTERVAL < 100)? 100 : REPLAY_INTERVAL;
    this.replayInProgress = false;
    this.replayPushpins = [];

    /* drag handler vars */
    this.dragType = DRAG_NONE;
    this.dragRulerStart = null;
    this.dragRulerEnd = null;
    
    /* key shift states */
    this.keyShfPress = false; // keyCode == 16
    this.keyCtlPress = false; // keyCode == 17
    this.keyAltPress = false; // keyCode == 18

    /* event handlers */
    try {
        var jsmap = this;
        this.leafletMap.on("mousemove" , function(me) { // MouseEvent
            jsmap._event_OnMouseMove(me);
        });
        this.leafletMap.on("mousedown" , function(me) { // MouseEvent
            jsmap._event_OnMouseDown(me);
        });
        this.leafletMap.on("mouseup"   , function(me) { // MouseEvent
            jsmap._event_OnMouseUp(me);
        });
        this.leafletMap.on("click", function(me) { // MouseEvent
            jsmap._event_OnClick(me);
        });
        this.leafletMap.on("popupopen" , function(pe) { // PopupEvent
            jsmap._event_PopupOpen(pe);
        });
        this.leafletMap.on("popupclose", function(pe) { // PopupEvent
            jsmap._event_PopupClose(pe);
        });
        // --
        L.DomEvent.on(document, "keydown", function(ke) { // KeyboardEvent
            switch (ke.keyCode) {
                case 16: jsmap.keyShfPress = true; break;
                case 17: jsmap.keyCtlPress = true; break;
                case 18: jsmap.keyAltPress = true; break;
            }
        });
        L.DomEvent.on(document, "keyup", function(ke) { // KeyboardEvent
            switch (ke.keyCode) {
                case 16: jsmap.keyShfPress = false; break;
                case 17: jsmap.keyCtlPress = false; break;
                case 18: jsmap.keyAltPress = false; break;
            }
        });
    } catch (e) {
        alert("Event Handler Error: " + e);
    }

    /* init lat/lon display */
    jsmSetLatLonDisplay(0,0);

    /* zoom event */
    this.lastMapZoom = 0;
    this.lastMapSize = L.point(0,0); // [x,y]

    /* post map init callback */
    if (POST_MAP_INIT_CALLBACK) {
        try {
            POST_MAP_INIT_CALLBACK(this);
        } catch (e) {
            console.log("Post map init callback error: " + e);
        }
    }

};

// ----------------------------------------------------------------------------

/* convert Leaflet point to JSMapPoint */
JSMap.prototype._toJSMapPointLatLon = function(lat, lng) {
    return new JSMapPoint(lat, lng);
};

JSMap.prototype._toJSMapPoint = function(pt) {
    return this._toJSMapPointLatLon(pt.lat, pt.lng); // L.LatLng to JSMapPoint
};

/* convert JSMapPoint to Leaflet point */
JSMap.prototype._toLeafletPointLatLon = function(lat, lon) {
    // -- "L.latLng(...)" appears to fail in some browser/map environments
    //return L.latLng(lat, lon);
    return new L.LatLng(lat, lon);
};

JSMap.prototype._toLeafletPoint = function(mp) { // JSMapPoint to L.LatLng
    return this._toLeafletPointLatLon(mp.lat, mp.lon);
};

JSMap.prototype._toLeafletPointArray = function(mpa) { // JSMapPoint[] to L.LatLng[]
    var lpa = []; // L.LatLng[]
    if (mpa && (mpa.length > 0)) {
        for (var i = 0; i < mpa.length; i++) {
            lpa.push(this._toLeafletPoint(mpa[i])); // L.LatLng
        }
    }
    return lpa;
};

// ----------------------------------------------------------------------------

/* create new Leaflet LatLngBounds */
JSMap.prototype._newLeafletBounds = function() {
    // -- "L.latLngBounds()" appears to fail in some browser/map environments
    return new L.LatLngBounds(); // L.latLngBounds();
};

/* convert JSBounds to Leaflet Bounds */
JSMap.prototype._toLeafletBounds = function(b) { // JSBounds to L.LatLngBounds
    //return L.latLngBounds([
    //    [ b.minLat, b.minLon ],
    //    [ b.maxLat, b.maxLon ]
    //]);
    var LLB = this._newLeafletBounds();
    //LLB.extend([ b.minLat, b.minLon ]);
    //LLB.extend([ b.maxLat, b.maxLon ]);
    LLB.extend(this._toLeafletPointLatLon(b.minLat,b.minLon)); // L.LatLng
    LLB.extend(this._toLeafletPointLatLon(b.maxLat,b.maxLon)); // L.LatLng
    return LLB;
};

// ----------------------------------------------------------------------------

/* gets the map resolution: degrees-per-pixel */
JSMap.prototype._getMapResolution = function() {
    var S = this.leafletMap.getSize();   // L.Point
    var B = this.leafletMap.getBounds(); // L.LatLngBounds
    var W = B.getWest();
    var E = B.getEast();
    if (E >= W) {
        return (E - W) / S.x;
    } else {
        // -- special case: bounds span 180/-180 longitude
        return (E - W + 360.0) / S.x;
    }
};

// ----------------------------------------------------------------------------

/* init Leaflet with OpenStreetMap */
JSMap.prototype._initOpenStreetMap = function(mapElement)
{
    console.log("Leaflet: Initializing OSM ...");
    var OpenStreetMap_Mapnik = L.tileLayer(
        "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", 
        {
            subdomains: 'abc',
            maxZoom: 19,
            attribution: '&copy;<a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }
    );
    OpenStreetMap_Mapnik.gtsMapName = "OSM";
    return OpenStreetMap_Mapnik;
};

/* init Leaflet with OpenCycleMap */
JSMap.prototype._initOpenCycleMap = function(mapElement)
{
    console.log("Leaflet: Initializing OpenCycleMap ...");
    var OpenCycleMap_Mapnik = L.tileLayer(
        "http://{s}.tile.opencyclemap.org/cycle/{z}/{x}/{y}.png", 
        {
            subdomains: 'abc',
            maxZoom: 18,
            attribution: '&copy;<a href="http://www.openstreetmap.org/copyright">OpenCycleMap</a>'
        }
    );
    OpenCycleMap_Mapnik.gtsMapName = "OSM Cycle";
    return OpenCycleMap_Mapnik;
};

/* init Leaflet with Custom Map */
JSMap.prototype._initCustomMap = function(mapElement, name, url, subdomains, maxZoom, copyright, initFTN)
{
    console.log("Leaflet: Initializing custom map: " + name);
    var Custom_Map = L.tileLayer(
        url, 
        {
            subdomains: subdomains,
            maxZoom: maxZoom,
            attribution: copyright
        }
    );
    Custom_Map.gtsMapName = name;
    if (initFTN) {
        try {
            initFTN(this);
        } catch (e) {
            console.log("ERROR: Custom Map init: " + name + " - " + e);
        }
    }
    return Custom_Map;
};

// ----------------------------------------------------------------------------

/**
*** Unload/release resources
**/
JSMap.prototype.JSUnload = function()
{
    // nothing to do?
};

// ----------------------------------------------------------------------------

/**
*** Clear all pushpins and drawn lines
**/
JSMap.prototype.JSClearLayers = function()
{
    this._hidePushpinPopup();
    this._clearPoiLayer();
    this._clearMarkerLayer();
    this._removeShapes(); // this._clearDrawLayer();
    this._clearRulerLayer(true);
    this.centerBounds = new JSBounds(); // L.latLngBounds();
    this.routeLines = [];
};

/**
*** Clear the POI layer
**/
JSMap.prototype._clearPoiLayer = function()
{
    if (this.poiLayer !== null) {
        try { this.poiLayer.clearLayers(); } catch (e) {}
        try { this.leafletMap.removeLayer(this.poiLayer); } catch (e) {}
    }
};

/**
*** Clear the marker layer
**/
JSMap.prototype._clearMarkerLayer = function()
{
    if (this.markerLayer !== null) {

        /* clear Leaflet markers */
		try {
		    var markers = this.markerLayer.getLayers();
			if (markers !== null) {
				for (var i = 0; i < markers.length; i++) {
				    markers[i].closePopup();
				    markers[i].unbindPopup();
					this.markerLayer.removeLayer(markers[i]);
				}
			}
		} catch (e) {
		    console.log("ERROR[_clearMarkerLayer]: " + e);
		}

		/* clear marker layers */
        try { this.markerLayer.clearLayers(); } catch (e) {}
        try { this.leafletMap.removeLayer(this.markerLayer); } catch (e) {}

    }
    this._clearReplay();
};

// ----------------------------------------------------------------------------

/**
*** Pause/Resume replay
**/
JSMap.prototype.JSPauseReplay = function(replay)
{
    /* stop replay? */
    if (!replay || (replay <= 0) || !this.replayInProgress) {
        // stopping replay
        this._clearReplay();
        return REPLAY_STOPPED;
    } else {
        // replay currently in progress
        if (this.replayTimer === null) {
            // replay is "PAUSED" ... resuming replay
            this._hidePushpinPopup();
            jsmHighlightDetailRow(-1, false);
            this._startReplayTimer(replay, 100);
            return REPLAY_RUNNING;
        } else {
            // replaying "RUNNING" ... pausing replay
            this._stopReplayTimer();
            return REPLAY_PAUSED;
        }
    }
};

/**
*** Start the replay timer
*** @param replay  0=off, 1=pushpin_only, 2=pushpin&balloon
**/
JSMap.prototype._startReplayTimer = function(replay, interval)
{
    if (this.replayInProgress) {
        this.replayTimer = setTimeout("jsmap._replayPushpins("+replay+")", interval);
    }
    jsmSetReplayState(REPLAY_RUNNING);
};

/**
*** Stop the current replay timer
**/
JSMap.prototype._stopReplayTimer = function()
{
    if (this.replayTimer !== null) { 
        clearTimeout(this.replayTimer); 
        this.replayTimer = null;
    }
    jsmSetReplayState(this.replayInProgress? REPLAY_PAUSED : REPLAY_STOPPED);
};

/**
*** Clear any current replay in process
**/
JSMap.prototype._clearReplay = function()
{
    this.replayPushpins = [];
    this.replayInProgress = false;
    this._stopReplayTimer();
    this.replayIndex = 0;
    jsmHighlightDetailRow(-1, false);
};

/**
*** Gets the current replay state
**/
JSMap.prototype._getReplayState = function()
{
    if (this.replayInProgress) {
        if (this.replayTimer === null) {
            return REPLAY_PAUSED;
        } else {
            return REPLAY_RUNNING;
        }
    } else {
        return REPLAY_STOPPED;
    }
};

// ----------------------------------------------------------------------------

/**
*** Clear the draw layer
**/
JSMap.prototype._clearRulerLayer = function(clearStart)
{
    if ((this.rulerLayer !== null) && (this.rulerFeatures !== null)) {
        try { 
            for (var i = 0; i < this.rulerFeatures.length; i++) {
                this.rulerLayer.removeLayer(this.rulerFeatures[i]);
            }
        } catch (e) {
            console.log("ERROR[_clearRulerLayer]: " + e);
        }
        this.rulerFeatures = null;
    }
    if (clearStart) {
        this.dragRulerStart = null;
        this.dragRulerEnd = null;
    }
};

/**
*** Draw Ruler
**/
JSMap.prototype._drawRuler = function(features) // L.Layer[]
{
    this._clearRulerLayer(false);
    this.rulerFeatures = features; // L.Layer[]
    if (this.rulerFeatures !== null) {
        if (this.rulerLayer === null) {
            this.rulerLayer = L.featureGroup();
            this.leafletMap.addLayer(this.rulerLayer);
        }
        for (var i = 0; i < this.rulerFeatures.length; i++) {
            this.ruleLayer.addLayer(this.rulerFeatures[i]);
        }
    }
};

/**
*** Create/return ruler feature
**/
JSMap.prototype._createRulerFeature = function(pt1, pt2)
{
    if ((pt1 !== null) && (pt2 !== null)) {
        var rp = [];
        rp.push(this._toLeafletPoint(pt1)); // L.LatLng
        rp.push(this._toLeafletPoint(pt2)); // L.LatLng
        var polyline = L.polyline(rp, 
            {
                stroke:        true,
                color:         "#FF6422",
                weight:        2, // width
                opacity:       1.0,
                lineCap:       "round",
                lineJoin:      "round",
                fill:          false
            }
        );
        return polyline;
    } else {
        return null;
    }
};

// ----------------------------------------------------------------------------

/**
*** Clear the draw layer
**/
JSMap.prototype._clearDrawLayer = function()
{
    if (this.drawFeatures !== null) {
        for (var i = 0; i < this.drawFeatures.length; i++) {
            try { 
                this.drawLayer[0].removeLayer(this.drawFeatures[i]); 
            } catch (e) {
                console.log("ERROR[_clearDrawLayer]: " + e);
            }
        }
        this.drawFeatures = null;
    }
};

/**
*** Draw Feature
*** @param clear     (boolean) true to clear draw layer
*** @param features  (L.circle, L.polyline, L.polygon)
**/
JSMap.prototype._drawFeatures = function(clear, features)
{

    /* clear existing features */
    if (clear) {
        this._clearDrawLayer();
    }

    /* add features */
    if (features) { // Layer[]
        if (!this.drawFeatures) { this.drawFeatures = []; }
        for (var i = 0; i < features.length; i++) {
            this.drawFeatures.push(features[i]);
        }
    }

    /* draw features */
    if (this.drawFeatures && (this.drawFeatures.length > 0)) {
        for (var i = 0; i < this.drawFeatures.length; i++) {
            var F = this.drawFeatures[i];
            this.drawLayer[0].addLayer(F); // L.featureGroup
        }
    }

};

// ----------------------------------------------------------------------------

/**
*** Sets the center of the map
**/
JSMap.prototype.JSSetCenter = function(center, zoom)
{
    try {
        var opt = this._toLeafletPoint(center); // L.LatLng
        if (!zoom || (zoom == 0)) {
            this.leafletMap.setView(opt, this.leafletMap.getZoom());
        } else
        if (zoom > 0) {
            this.leafletMap.setView(opt, zoom);
        } else {
            var ob = this._newLeafletBounds(); // L.latLngBounds();
            ob.extend(opt);
            zoom = this.leafletMap.getBoundsZoom(ob);
            this.leafletMap.setView(opt, zoom);
        }
    } catch (e) {
        //
    }
};

/**
*** Draw the specified pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
*** @param recenter  0=no-recenter, 1=last-pushpin, 2=all-pushpins
*** @param replay    0=off, 1=pushpin_only, 2=pushpin&balloon
**/
JSMap.prototype.JSDrawPushpins = function(pushPins, recenterMode, replay)
{

    /* there are no pushpins in geozone mode */
    if (jsvGeozoneMode) {
        return;
    }

    /* reset pushpin layer */
    this._hidePushpinPopup();

    /* drawn pushpins */
    var drawPushpins = [];

    /* make sure we have a bounding box instance */
    if (!this.centerBounds) {
        this.centerBounds = new JSBounds(); // new L.LatLngBounds();
    }

    /* recenter map on points */
    var pointCount = 0;
    if ((pushPins !== null) && (pushPins.length > 0)) {
        // extend bounding box around displayed pushpins
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i]; // JSMapPushpin
            if (pp.show && ((pp.lat != 0.0) || (pp.lon != 0.0))) {
                pointCount++;
                this.centerBounds.extend(pp); // (this._toLeafletPoint(pp))
                drawPushpins.push(pp);
            }
        }
        // make sure points span a minimum distance top to bottom
        var rangeRadiusM = 400; // TODO: should make this a configurable options
        var cenPt = this.centerBounds.getCenter(); // this._toJSMapPoint(...)
        var topPt = geoRadiusPoint(cenPt.lat, cenPt.lon, rangeRadiusM,   0.0); // top JSMapPoint
        this.centerBounds.extend(topPt); // (this._toLeafletPoint(topPt))
        var botPt = geoRadiusPoint(cenPt.lat, cenPt.lon, rangeRadiusM, 180.0); // bottom JSMapPoint
        this.centerBounds.extend(botPt); // (this._toLeafletPoint(botPt))
    }
    if (recenterMode > 0) {
        var centerPt;
        var zoomFactor;
        if (pointCount <= 0) {
            centerPt   = this._toLeafletPoint(DEFAULT_CENTER); // L.LatLng
            zoomFactor = DEFAULT_ZOOM;
        } else 
        if (recenterMode == RECENTER_LAST) { // center on last point
            var pp     = drawPushpins[drawPushpins.length - 1];
            centerPt   = this._toLeafletPoint(pp); // L.LatLng
            zoomFactor = this.leafletMap.getZoom();
        } else 
        if (recenterMode == RECENTER_PAN) { // pan to last point
            var pp     = drawPushpins[drawPushpins.length - 1];
            centerPt   = this._toLeafletPoint(pp); // L.LatLng
            zoomFactor = this.leafletMap.getZoom();
        } else {
            centerPt   = this._toLeafletPoint(this.centerBounds.getCenter()); // L.LatLng
            zoomFactor = this.leafletMap.getBoundsZoom(this._toLeafletBounds(this.centerBounds));
        }
        try {
            this.leafletMap.setView(centerPt, zoomFactor); 
            // -- l.fn.call is not a function. (In 'l.fn.call(l.ctx||this,n)', 'l.fn.call' is undefined)
        } catch (e) {
            alert("Error: [JSDrawPushpins] (pointCount="+pointCount+", recenterMode="+recenterMode+", center="+centerPt+", zoom="+zoomFactor+") " + e);
            return;
        }
    }
    if (pointCount <= 0) {
        return;
    }

    /* replay pushpins? */
    if (replay && (replay >= 1)) {
        this.leafletMap.addLayer(this.markerLayer);   // must re-add layer
        this.replayIndex = 0;
        this.replayInProgress = true;
        this.replayPushpins = drawPushpins;
        this._startReplayTimer(replay, 100);
        return;
    }

    /* draw pushpins now */
    var pushpinErr = null;
    for (var i = 0; i < drawPushpins.length; i++) {
        var pp = drawPushpins[i]; // JSMapPushpin
        try {
            pp.hoverPopup = false;
            this._addPushpin(pp, this.markerLayer);
        } catch (e) {
            if (pushpinErr === null) { pushpinErr = e; }
        }
    }
    try { 
        this.leafletMap.addLayer(this.markerLayer);  // must re-add layer
    } catch (e) {
        if (pushpinErr === null) { pushpinErr = e; }
    }
    if (pushpinErr !== null) {
        alert("Error: adding pushpins:\n" + pushpinErr);
    }

};

/**
*** Draw the specified PointsOfInterest pushpins on the map
*** @param pushPins  An array of JSMapPushpin objects
**/
JSMap.prototype.JSDrawPOI = function(pushPins)
{

    /* reset pushpin layer */
    this._clearPoiLayer();
    //this.poiLayer.display(false);
    this._hidePushpinPopup();

    /* draw pushpins */
    if ((pushPins !== null) && (pushPins.length > 0)) {
        var pushpinErr = null;
        for (var i = 0; i < pushPins.length; i++) {
            var pp = pushPins[i]; // JSMapPushpin
            try {
                pp.hoverPopup = false;
                this._addPushpin(pp, this.poiLayer);
            } catch (e) {
                if (pushpinErr === null) { pushpinErr = e; }
            }
        }
        try { 
            this.leafletMap.addLayer(this.poiLayer);  // must re-add layer
            //this.poiLayer.display(true);
        } catch (e) {
            if (pushpinErr === null) { pushpinErr = e; }
        }
        if (pushpinErr !== null) {
            alert("Error: adding pushpins:\n" + pushpinErr);
        }
    }

};

/**
*** Adds a single pushpin to the map
*** @param pp  The JSMapPushpin object to add to the map
**/
JSMap.prototype._addPushpin = function(pp, layer)
{
    try {
        var self = this;

        pp.map = this.leafletMap;

        var ppMarker = null;
        if (pp.iconUrl) {
            var iIcon = L.icon(
                {
                    iconUrl: pp.iconUrl,
                    iconSize: [ pp.iconSize[0], pp.iconSize[1] ],
                    iconAnchor: [ pp.iconHotspot[0], pp.iconHotspot[1] ],
                    popupAnchor: [ 0, -pp.iconHotspot[1]+3 ]
                  //shadowURL: 
                  //shadowSize:
                  //shadowAnchor:
                }
            );
            ppMarker = L.marker(this._toLeafletPoint(pp), 
                {
                    icon: iIcon,
                    clickable: true,
                    draggable: false,
                    title: '',
                    zIndexOffset: (pp.ppNdx * 10)  // TODO: verify this var name
                }
            );
        }

        var bgMarker = null;
        if (pp.bgUrl) {
            var bIcon = L.icon(
                {
                    iconUrl: pp.bgUrl,
                    iconSize: [ pp.bgSize[0], pp.bgSize[1] ],
                    iconAnchor: [ pp.bgOffset[0], pp.bgOffset[1] ],
                    popupAnchor: [ 0, -pp.iconHotspot[1]+3 ]
                }
            );
            bgMarker = L.marker(this._toLeafletPoint(pp),
                {
                    icon: bIcon,
                    clickable: false,
                    draggable: false,
                    zIndexOffset: ((pp.ppNdx * 10) + 1)  // TODO: verify this var name
                }
            );
        }

        if (pp.getHTML()) {
            pp.popup = L.popup()
                .setLatLng(this._toLeafletPoint(pp)) // L.LatLng
                .setContent(pp.getHTML())
                ;
            ppMarker.bindPopup(pp.popup);
            pp.popup.jsmMPP = pp; // back pointer to this JSMapPushpin object
            // -- [TODO: pp.hoverPopup]
        }
        
        if (bgMarker) {
            pp.bgMarker = bgMarker;
            layer.addLayer(bgMarker);
        }

        pp.marker = ppMarker;
        layer.addLayer(ppMarker);

    } catch (e) {
        // -- ppMarker.events is undefined
        console.log("ERROR[_addPushpin]: " + e);
    }
};

/**
*** Replays the list of pushpins on the map
*** @param replay  0=off, 1=pushpin_only, 2=pushpin&balloon
**/
JSMap.prototype._replayPushpins = function(replay)
{

    /* no replay pushpins? */
    if (this.replayPushpins === null) {
        this._clearReplay();
        jsmHighlightDetailRow(-1, false);
        return; // stop
    }

    /* advance to next valid point */
    while (true) {
        // -- end of replay
        if (this.replayIndex >= this.replayPushpins.length) {
            this._clearReplay();
            jsmHighlightDetailRow(-1, false);
            return; // stop
        }
        // -- get valid point
        var pp = this.replayPushpins[this.replayIndex]; // JSMapPushpin
        if (!geoIsValid(pp.lat,pp.lon)) {
            // -- current point invalid
            this.replayIndex++;
            continue; // goto next point
        }
        // -- skip nearby points?
        if ((AutoSkipRadius > 0.0) && (this.replayIndex > 0) && 
            ((this.replayIndex + 1) < this.replayPushpins.length)) {
            var lastPP = this.replayPushpins[this.replayIndex - 1];
            if (geoIsValid(lastPP.lat,lastPP.lon)) {
                var distM = geoDistanceMeters(pp.lat,pp.lon,lastPP.lat,lastPP.lon);
                if (distM <= AutoSkipRadius) {
                    // -- current point too close to previous point
                    this.replayIndex++;
                    continue; // goto next point
                }
            }
        }
        // -- use current point
        break;
    }

    /* add pushpin */
    try {
        var lastNdx = this.replayIndex - 1;
        var pp = this.replayPushpins[this.replayIndex++]; // JSMapPushpin
        pp.hoverPopup = false;
        if (REPLAY_SINGLE && (lastNdx >= 0)) {
            try { this.markerLayer.clearLayers(); } catch (e) {}
        }
        this._addPushpin(pp, this.markerLayer);
        if (replay && (replay >= 2)) {
            this._showPushpinPopup(pp); // JSMapPushpin
        } else {
            jsmHighlightDetailRow(pp.rcdNdx, true);
        }
        this._startReplayTimer(replay, this.replayInterval);
    } catch (e) {
        // ignore
        console.log("ERROR[_replayPushpins]: " + e);
    }

};

// ----------------------------------------------------------------------------

/**
*** This method should cause the info-bubble popup for the specified pushpin to display
*** @param pp   The JSMapPushpin object to popup its info-bubble
**/
JSMap.prototype.JSShowPushpin = function(pp, center)
{
    if (pp) { // JSMapPushpin
        if (center) {
            this.JSSetCenter(new JSMapPoint(pp.lat, pp.lon));
        }
        this._showPushpinPopup(pp); // JSMapPushpin
    }
};

JSMap.prototype._showPushpinPopup = function(pp) // JSMapPushpin
{
    this._hidePushpinPopup();
    if (pp) {
        pp.marker.openPopup();
    }
};

JSMap.prototype._hidePushpinPopup = function()
{
    this.leafletMap.closePopup();
};

// ----------------------------------------------------------------------------

/**
*** Draws a line between the specified points on the map.
*** @param points   An array of JSMapPoint objects
**/
JSMap.prototype.JSDrawRoute = function(points, color)
{
    if ((points !== null) && (points.length > 0)) {
        var route = {
            points: points,
            color:  color
        };
        if (!this.routeLines) { this.routeLines = []; }
        this.routeLines.push(route);
        var routeFeatures = [];
        for (var i = 0; i < this.routeLines.length; i++) {
            var r = this.routeLines[i];
            var polyline = this._createPolyline(r.points,r.color);
            if (polyline != null) {
                routeFeatures.push(polyline);
            }
        }
        this._drawFeatures(true, routeFeatures);
    } else {
        //this.routeLines = [];
        //this._clearDrawLayer();
    }
};

/**
*** Create/Return route feature
**/
JSMap.prototype._createPolyline = function(points, color) // JSMapPoint
{
    if ((points !== null) && (points.length > 0)) {
        return L.polyline(this._toLeafletPointArray(points), // L.LatLng[]
            {
                stroke:    true,
                color:     color,
                weight:    2,     // line width
                opacity:   1.0,
                lineCap:   "round",
                lineJoin:  "round",
                fill:      false
            }
        );
    } else {
        return null;
    }
};

// ----------------------------------------------------------------------------

/**
*** Remove previously drawn shapes 
**/
JSMap.prototype._removeShapes = function()
{
    this._clearDrawLayer();
    this.drawShapes = [];
};

/**
*** Draws a Shape on the map at the specified location
*** @param type     The Geozone shape type ("line", "circle", "rectangle", "polygon", "center")
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points (JSMapPoint[])
*** @param color    Shape color
*** @param zoomTo   True to zoom to drawn shape
*** @param desc     The shape description
*** @param ppNdx    The pushpin index
*** @return True if shape was drawn, false otherwise
**/
JSMap.prototype.JSDrawShape = function(type, radiusM, verticePts, color, zoomTo, desc, ppNdx)
{

    /* no type? */
    if (!type || (type == "") || (type == "!")) {
        this._removeShapes();
        return false;
    }

    /* clear existing shapes? */
    if (type.startsWith("!")) { 
        this._removeShapes();
        type = type.substr(1); 
    }

    /* no geopoints? */
    if (!verticePts || (verticePts.length == 0)) {
        return false;
    }

    /* color */
    if (!color || (color == "")) {
        color = "#0000FF";
    }

    /* zoom bounds */
    var mapBounds = (zoomTo || (ppNdx >= 0))? this._newLeafletBounds() : null; // L.latLngBounds() : null;

    /* color/style */
    var colorStyle = new LeafletColorStyle(color, 0.75, color, 0.08);

    /* draw shape */
    var didDrawShape = false;
    if (type == "circle") { // ZONE_POINT_RADIUS

        var circleList = [];
        for (var p = 0; p < verticePts.length; p++) {
            var jsPt    = verticePts[p]; // JSMapPoint
            var center  = this._toLeafletPoint(jsPt); // L.LatLng
            var circleF = this._createCircleFeature(center, radiusM, colorStyle);
            if (circleF != null) {
                if (mapBounds) { // LatLngBounds
                    mapBounds.extend(center);
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(jsPt, radiusM,   0.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(jsPt, radiusM,  90.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(jsPt, radiusM, 180.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(jsPt, radiusM, 270.0)));
                }
                this.drawShapes.push(circleF);
            }
        }
        if (this.drawShapes.length > 0) {
            this._drawFeatures(false, this.drawShapes);
            didDrawShape = true;
        }
            
    } else 
    if (type == "rectangle") { // ZONE_BOUNDED_RECT

        if (verticePts.length >= 2) {

            /* create rectangle */
            var vp0   = verticePts[0];
            var vp1   = verticePts[1];
            var TL    = this._toLeafletPointLatLon(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var TR    = this._toLeafletPointLatLon(((vp0.lat>vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var BL    = this._toLeafletPointLatLon(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon<vp1.lon)?vp0.lon:vp1.lon));
            var BR    = this._toLeafletPointLatLon(((vp0.lat<vp1.lat)?vp0.lat:vp1.lat),((vp0.lon>vp1.lon)?vp0.lon:vp1.lon));
            var crPts = [ TL, TR, BR, BL, TL ];
            var poly  = this._createPolygonFeature(crPts, colorStyle);
            if (mapBounds) { for (var b = 0; b < crPts.length; b++) { mapBounds.extend(crPts[b]); } }
            this.drawShapes.push(poly);
            this._drawFeatures(false, this.drawShapes);
            didDrawShape = true;

        }
            
    } else 
    if (type == "polygon") { // ZONE_POLYGON
       
        if (verticePts.length >= 3) {

            var crPts = [];
            for (var p = 0; p < verticePts.length; p++) {
                var olPt = this._toLeafletPointLatLon(verticePts[p].lat, verticePts[p].lon);
                crPts.push(olPt);
                if (mapBounds) { mapBounds.extend(olPt); }
            }
            var poly  = this._createPolygonFeature(crPts, colorStyle);
            this.drawShapes.push(poly);
            this._drawFeatures(false, this.drawShapes);
            didDrawShape = true;

        }

    } else
    if (type == "corridor") { // ZONE_SWEPT_POINT_RADIUS

        // TODO: 

    } else
    if (type == "center") {

        if (mapBounds) {
            for (var p = 0; p < verticePts.length; p++) {
                var olPt = this._toLeafletPointLatLon(verticePts[p].lat, verticePts[p].lon);
                mapBounds.extend(olPt);
            }
            didDrawShape = true;
        }

    }

    /* draw pushpin icon */
    if (didDrawShape && (ppNdx >= 0) && (ppNdx < jsvPushpinIcon.length) && mapBounds) {
        var ppLL = mapBounds.getCenter(); // L.LatLng
        var pp = jsvPushpinIcon[ppNdx]; // JSMap.writePushpinArray
        if (pp.iconEval) { pp = jsvPushpinIcon[0]; } // black
        // --
        var iIcon = L.icon(
            {
                iconUrl: pp.iconUrl,
                iconSize: [ pp.iconSize[0], pp.iconSize[1] ],
                iconAnchor: [ pp.iconHotspot[0], pp.iconHotspot[1] ],
                popupAnchor: [ 0, -pp.iconHotspot[1]+3 ],
              //shadowURL: 
              //shadowSize:
              //shadowAnchor:
            }
        );
        var marker = L.marker(this._toLeafletPoint(pp), 
            {
                icon: iIcon,
                clickable: true,
                draggable: false,
                title: '',
              //zIndexOffset: (pp.ppNdx * 10)  // TODO: verify this var name
            }
        );
        // -- see also JSMap.prototype._addPushpin
        var popup = L.popup()
            .setLatLng(this._toLeafletPointLatLon(ppLL.lat,ppLL.lng))
            .setContent("<div class='geozoneMarker'>"+desc+"&nbsp;&nbsp;</div>")
            ;
        var map = this.leafletMap;
        marker.bindPopup(popup);
        this.markerLayer.addLayer(marker);
        this.drawShapes.push(marker);
    }

    /* center on shape */
    if (didDrawShape && zoomTo && mapBounds) {
        var centerPt   = mapBounds.getCenter(); // L.LatLng
        var zoomFactor = this.leafletMap.getBoundsZoom(mapBounds);
        try { 
            this.leafletMap.setView(centerPt, zoomFactor); 
        } catch (e) { 
            console.log("ERROR[JSDrawShape]: " + e);
        }
    }

    /* shape not supported */
    return didDrawShape;

};

// ----------------------------------------------------------------------------

/**
*** Draws a Geozone on the map at the specified location
*** @param type     The Geozone type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of JSMapPoints
*** @return An object representing the Circle.
**/
JSMap.prototype.JSDrawGeozone = function(type, radiusM, points, color, primNdx)
{
    // type:
    //   0 - ZONE_POINT_RADIUS
    //   1 - ZONE_BOUNDED_RECT
    //   2 - ZONE_SWEPT_POINT_RADIUS
    //   3 - ZONE_POLYGON
    // (type ZONE_POINT_RADIUS may only be currently supported)
    this._JSDrawGeozone(type, radiusM, points, color, primNdx, false)
};

/**
*** Draws a Geozone on the map at the specified location
*** @param type     The Geozone type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of JSMapPoints
*** @return An object representing the Circle.
**/
JSMap.prototype._JSDrawGeozone = function(type, radiusM, points, color, primNdx, isDragging)
{

    /* Geozone mode */
    jsvGeozoneMode = true;

    /* remove old primary */
    if (!isDragging) { 
        this.primaryCenter = null;
        this.primaryIndex  = primNdx;
    }

    /* save geozone points */
    this.geozonePoints = points; // JSMapPoint[]

    /* no points? */
    if ((points === null) || (points.length <= 0)) {
        //alert("No Zone center!");
        this._clearDrawLayer();
        return null;
    }

    /* point-radius */
    if (type == ZONE_POINT_RADIUS) {

        /* adjust radius */
        if (isNaN(radiusM))              { radiusM = 5000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }
        jsvZoneRadiusMeters = radiusM;

        /* draw points */
        var count = 0;
        var zoneFeatures = new Array();
        var mapBounds = this._newLeafletBounds(); // new L.LatLngBounds();
        var polyPts = []; // L.LatLng[]
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if (geoIsValid(c.lat,c.lon)) {
                var isPrimary = (i == primNdx);
                var center    = (isPrimary && isDragging)? this.primaryCenter : this._toLeafletPoint(c); // L.LatLng
                var circStyle = GetGeozoneStyle(isPrimary, color);
                var circleF   = this._createCircleFeature(center, radiusM, circStyle);
                if (circleF != null) {
                    zoneFeatures.push(circleF);
                    if (isPrimary && !isDragging) {
                        this.primaryCenter = center; // L.LatLng
                    }
                    polyPts.push(center); // L.LatLng
                    mapBounds.extend(center);
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(c, radiusM,   0.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(c, radiusM,  90.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(c, radiusM, 180.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(c, radiusM, 270.0)));
                    count++;
                }
            }
        }

        /* center on geozone */
        if (!isDragging) {
            var centerPt   = this._toLeafletPoint(DEFAULT_CENTER); // L.LatLng
            var zoomFactor = DEFAULT_ZOOM;
            if (count > 0) {
                centerPt   = mapBounds.getCenter(); // L.LatLng
                zoomFactor = this.leafletMap.getBoundsZoom(mapBounds);
            }
            try { 
                this.leafletMap.setView(centerPt, zoomFactor); 
            } catch (e) { 
                console.log("ERROR[_JSDrawGeozone]: "+e);
            }
        }

        /* create zone feature */
        this._drawFeatures(true, zoneFeatures);

    } else
    if (type == ZONE_POLYGON) {

        /* set radius (should be about 30 pixels radius) */
        jsvZoneRadiusMeters = radiusM;

        /* draw points */
        var count = 0;
        var zoneFeatures = new Array();
        var mapBounds = this._newLeafletBounds(); // new L.LatLngBounds();
        var polyPts = [];
        var polyPtPrim = -1;
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if ((c.lat != 0.0) || (c.lon != 0.0)) {
                var isPrimary = (i == primNdx);
                var center    = (isPrimary && isDragging)? this.primaryCenter : this._toLeafletPoint(c); // L.LatLng
                if (isPrimary) {
                    this.primaryCenter = center; // L.LatLng
                    polyPtsPrim = polyPts.length;
                }
                polyPts.push(center); // L.LatLng
                mapBounds.extend(center);
                count++;
            }
        }
        if (polyPts.length >= 3) {
            zoneFeatures.push(this._createPolygonFeature(polyPts, GetGeozoneStyle(false,color)));
        }

        /* center on geozone */
        if (!isDragging) {
            var centerPt   = this._toLeafletPoint(DEFAULT_CENTER); // L.LatLng
            var zoomFactor = DEFAULT_ZOOM;
            if (count > 0) {
                centerPt   = mapBounds.getCenter(); // L.LatLng
                zoomFactor = this.leafletMap.getBoundsZoom(mapBounds);
            }
            try { 
                this.leafletMap.setView(centerPt, zoomFactor); 
            } catch (e) { 
                console.log("ERROR[_JSDrawGeozone]: "+e);
            }
        }

        /* create polygon vertices based on current degrees-per-pixel */
        var resolution = this._getMapResolution();
        radiusM = 15.0 * resolution;
        //alert("Radius="+radiusM + ", Resolution="+resolution);
        jsvZoneRadiusMeters = radiusM;

        /* draw drag circles at vertices */
        for (var i = 0; i < polyPts.length; i++) {
            var center    = polyPts[i]; // L.LatLng
            var dragStyle = GetGeozoneStyle((i == polyPtsPrim), color);
            var circleF   = this._createCircleFeature(center, radiusM, dragStyle);
            if (circleF != null) {
                zoneFeatures.push(circleF);
            }
        }

        /* create zone feature */
        this._drawFeatures(true, zoneFeatures);

    } else
    if (type == ZONE_SWEPT_POINT_RADIUS) {

        /* adjust radius */
        if (isNaN(radiusM))              { radiusM = 1000; }
        if (radiusM > MAX_ZONE_RADIUS_M) { radiusM = MAX_ZONE_RADIUS_M; }
        if (radiusM < MIN_ZONE_RADIUS_M) { radiusM = MIN_ZONE_RADIUS_M; }
        jsvZoneRadiusMeters = radiusM;

        /* draw vertices */
        var count = 0;
        var zoneFeatures = new Array();
        var mapBounds = this._newLeafletBounds(); // new L.LatLngBounds();
        var polyPts = []; // L.LatLng[]
        for (var i = 0; i < points.length; i++) {
            var c = points[i]; // JSMapPoint
            if ((c.lat != 0.0) || (c.lon != 0.0)) {
                var isPrimary = (i == primNdx);
                var center    = (isPrimary && isDragging)? this.primaryCenter : this._toLeafletPoint(c); // L.LatLng
                var circStyle = GetGeozoneStyle(isPrimary,color);
                var circleF   = this._createCircleFeature(center, radiusM, circStyle);
                if (circleF != null) {
                    zoneFeatures.push(circleF);
                    if (isPrimary && !isDragging) {
                        this.primaryCenter = center; // L.LatLng
                    }
                    polyPts.push(center); // L.LatLng
                    mapBounds.extend(center);
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(c, radiusM,   0.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(c, radiusM,  90.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(c, radiusM, 180.0)));
                    mapBounds.extend(this._toLeafletPoint(this._calcRadiusPoint(c, radiusM, 270.0)));
                    count++;
                }
            }
        }

        /* draw corridors */
        if (polyPts.length >= 2) {
            // routeline "_createPolyline"
            for (var i = 0; i < (polyPts.length - 1); i++) {
                var ptA = this._toJSMapPoint(polyPts[i  ]);
                var ptB = this._toJSMapPoint(polyPts[i+1]);
                var hAB = geoHeading(ptA.lat, ptA.lon, ptB.lat, ptB.lon) - 90.0; // perpendicular
                var rp1 = this._toLeafletPoint(this._calcRadiusPoint(ptA, radiusM, hAB        ));
                var rp2 = this._toLeafletPoint(this._calcRadiusPoint(ptB, radiusM, hAB        ));
                var rp3 = this._toLeafletPoint(this._calcRadiusPoint(ptB, radiusM, hAB + 180.0));
                var rp4 = this._toLeafletPoint(this._calcRadiusPoint(ptA, radiusM, hAB + 180.0));
                var rectPts = [ rp1, rp2, rp3, rp4 ];
                zoneFeatures.push(this._createPolygonFeature(rectPts, GetGeozoneStyle(false,color)));
            }
        }

        /* center on geozone */
        if (!isDragging) {
            var centerPt   = this._toLeafletPoint(DEFAULT_CENTER); // L.LatLng
            var zoomFactor = DEFAULT_ZOOM;
            if (count > 0) {
                centerPt   = mapBounds.getCenter(); // L.LatLng
                zoomFactor = this.leafletMap.getBoundsZoom(mapBounds);
            }
            try { 
                this.leafletMap.setView(centerPt, zoomFactor); 
            } catch (e) { 
                console.log("ERROR[_JSDrawGeozone]: "+e);
            }
        }

        /* create zone feature */
        this._drawFeatures(true, zoneFeatures);

    } else {
        
        alert("Geozone type not supported: " + type);
        
    }
    
    return null;
};

// ----------------------------------------------------------------------------

/**
*** Returns a circle shape 
*** @param center   (L.LatLng) The center point of the circle
*** @param radiusM  The radius of the circle in meters
*** @return The circle object
**/
JSMap.prototype._createCircleFeature = function(center, radiusM, circleStyle)
{
    if ((center !== null) && (radiusM > 0)) {
        var circle = new L.Circle(center, // was "L.circle(..."
            {
                radius: radiusM,
                stroke: true,
                color: circleStyle.strokeColor,
                weight: circleStyle.strokeWidth,
                opacity: circleStyle.strokeOpacity,
                linkCap: "round",
                lineJoin: "round",
                fill: true,
                fillColor: circleStyle.fillColor,
                fillOpacity: circleStyle.fillOpacity
            }
        );
        return circle;
    } else {
        return null;
    }
};

/**
*** Calculate the lat/lon on the radius of the circle in the 'heading' direction
*** @param center  (JSMapPoint) the center lat/lon
*** @param radiusM (double) the radius
*** @param heading (double) the vector heading
**/
JSMap.prototype._calcRadiusPoint = function(center/*JSMapPoint*/, radiusM, heading)
{
    var pt = geoRadiusPoint(center.lat, center.lon, radiusM, heading); // { lat: <>, lon: <> }
    return new JSMapPoint(pt.lat, pt.lon);
};

// ----------------------------------------------------------------------------

/**
*** Returns a polygon shape
*** @param vertices   (L.LatLng) An array of polygon vertice points of the circle
*** @return The polygon object
**/
JSMap.prototype._createPolygonFeature = function(vertices, colorStyle)
{
    if ((vertices !== null) && (vertices.length >= 3)) {
        var polygon = L.polygon(vertices, 
            {
                stroke: true,
                color: colorStyle.strokeColor,
                weight: colorStyle.strokeWidth,
                opacity: colorStyle.strokeOpacity,
                lineCap: "round",
                lineJoin: "round",
                fill: true,
                fillColor: colorStyle.fillColor,
                fillOpacity: colorStyle.fillOpacity,
                fillRule: "eventodd"
            }
        );
        return polygon;
    } else {
        return null;
    }
};

// ----------------------------------------------------------------------------

/**
*** Create/Adjust feature points 
*** @param llLL The Leaflet.LatLng point
**/
JSMap.prototype._createGeometryPoint = function(llLL) // L.LatLng
{
    return L.point(llLL.lat, llLL.lng);
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** MouseEvent handler to draw circles/lines on the map 
*** @param me  The MouseEvent
**/
JSMap.prototype._event_OnMouseDown = function(me) // MousrEvent
{
    var meXY     = me.layerPoint;    // L.point
    var meLL     = me.latlng         // L.LatLng
    var domME    = me.originalEvent; // DOMMouseEvent
    var shiftKey = this.keyShfPress;
    var altKey   = this.keyAltPress;
    var ctrlKey  = this.keyCtlPress;

    /* quick exits */
    if (altKey || (ctrlKey && shiftKey)) {
        return true;
    }

    /* mouse down point */
    var LL = this._toJSMapPoint(meLL);
    jsmapElem.style.cursor = 'crosshair';

    /* start distance ruler drag */
    if (ctrlKey) {
        this.dragType = DRAG_RULER;
        this._clearRulerLayer(true);
        this.dragRulerStart = LL; // JSMapPoint
        jsmSetDistanceDisplay(0);
        //L.DomEvent.stop(domME);
        domME.preventDefault(domME);
        domME.stopPropagation(domME);
        this.leafletMap.dragging.disable();
        return false;
    }

    /* geozone mode */
    if (jsvGeozoneMode && jsvZoneEditable) {
        var radiusM = zoneMapGetRadius(false);
        // check primary point
        if (this.primaryCenter !== null) {
            var CC = this._toJSMapPoint(this.primaryCenter);
            if (geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon) <= radiusM) {
                if (shiftKey) {
                    // resize
                    this.dragType = DRAG_GEOZONE_RADIUS;
                    this._clearRulerLayer(true);
                } else {
                    // inside geozone, move
                    this.dragType = DRAG_GEOZONE_CENTER;
                    this.dragZoneOffsetLat = LL.lat - CC.lat;
                    this.dragZoneOffsetLon = LL.lon - CC.lon;
                }
                domME.preventDefault(domME);
                domME.stopPropagation(domME);
                this.leafletMap.dragging.disable();
                return false;
            }
        }
        // check other points
        if (!shiftKey && this.geozonePoints && (this.geozonePoints.length > 0)) {
            for (var i = 0; i < this.geozonePoints.length; i++) {
                if (geoDistanceMeters(this.geozonePoints[i].lat, this.geozonePoints[i].lon, LL.lat, LL.lon) <= radiusM) {
                    this.primaryIndex  = i;
                    this.primaryCenter = this._toLeafletPoint(this.geozonePoints[i]); // L.LatLng
                    zoneMapSetIndex(this.primaryIndex,true);
                    this._JSDrawGeozone(jsvZoneType, jsvZoneRadiusMeters, this.geozonePoints, jsvZoneColor, this.primaryIndex, false);
                    // inside geozone, move
                    CC = this._toJSMapPoint(this.primaryCenter);
                    this.dragType = DRAG_GEOZONE_CENTER;
                    this.dragZoneOffsetLat = LL.lat - CC.lat;
                    this.dragZoneOffsetLon = LL.lon - CC.lon;
                    domME.preventDefault(domME);
                    domME.stopPropagation(domME);
                    this.leafletMap.dragging.disable();
                    return false;
                }
            }
        }
    }

    this.dragType = DRAG_NONE;
    if (!this.leafletMap.dragging.enabled()) {
        this.leafletMap.dragging.enable();
    }
    return true;
};

/**
*** MouseEvent handler to draw circles on the map 
*** @param me  The MouseEvent
**/
JSMap.prototype._event_OnMouseUp = function(me) // MouseEvent
{
    var meXY     = me.layerPoint;    // L.Point
    var meLL     = me.latlng         // L.LatLng
    var domME    = me.originalEvent; // DOMMouseEvent
    var shiftKey = this.keyShfPress;
    var altKey   = this.keyAltPress;
    var ctrlKey  = this.keyCtlPress;

    /* geozone mode */
    if (jsvGeozoneMode && ((this.dragType & DRAG_GEOZONE) != 0)) {
        var CC      = this._toJSMapPoint(this.primaryCenter);
        var radiusM = zoneMapGetRadius(false);
        jsmSetPointZoneValue(CC.lat, CC.lon, radiusM);
        this.dragType = DRAG_NONE;
        mapProviderParseZones(jsvZoneList);
        domME.preventDefault(domME);
        domME.stopPropagation(domME);
        this.leafletMap.dragging.enable();
        return false;
    }

    /* normal mode */
    this.dragType = DRAG_NONE;
    if (!this.leafletMap.dragging.enabled()) {
        this.leafletMap.dragging.enable();
    }
    return true;
};

/**
*** MouseEvent handler to detect lat/lon changes and draw circles/lines on the map 
*** @param me  The mouse event
**/
JSMap.prototype._event_OnMouseMove = function(me) // MouseEvent
{
    var meXY     = me.layerPoint;    // L.Point
    var meLL     = me.latlng         // L.LatLng
    var domME    = me.originalEvent; // DOMMouseEvent
    var shiftKey = this.keyShfPress;
    var altKey   = this.keyAltPress;
    var ctrlKey  = this.keyCtlPress;

    /* Latitude/Longitude change */
    var LL = this._toJSMapPoint(meLL);
    jsmSetLatLonDisplay(LL.lat, LL.lon);
    jsmapElem.style.cursor = 'crosshair';

    /* distance ruler */
    if (this.dragType == DRAG_RULER) {
        this.dragRulerEnd = LL;
        var CC = this.dragRulerStart;
        jsmSetDistanceDisplay(geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon));
        this._drawRuler([ this._createRulerFeature(this.dragRulerStart, this.dragRulerEnd) ]);
        domME.preventDefault(domME);
        domME.stopPropagation(domME);
        return false;
    }

    /* geozone mode */
    if (this.dragType == DRAG_GEOZONE_RADIUS) {
        var CC = this._toJSMapPoint(this.primaryCenter);
        jsvZoneRadiusMeters = Math.round(geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon));
        if (jsvZoneRadiusMeters > MAX_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MAX_ZONE_RADIUS_M; }
        if (jsvZoneRadiusMeters < MIN_ZONE_RADIUS_M) { jsvZoneRadiusMeters = MIN_ZONE_RADIUS_M; }
        var circleF = this._createCircleFeature(this.primaryCenter,jsvZoneRadiusMeters,GetGeozoneStyle(true,jsvZoneColor));
        if (circleF != null) {
            var features = [ circleF ];
            this._drawFeatures(true, features);
            jsmSetDistanceDisplay(jsvZoneRadiusMeters);
            //mapProviderParseZones(jsvZoneList);
        }
        domME.preventDefault(domME);
        domME.stopPropagation(domME);
        return false;
    }

    /* geozone mode */
    if (this.dragType == DRAG_GEOZONE_CENTER) {
        var CC = new JSMapPoint(LL.lat - this.dragZoneOffsetLat, LL.lon - this.dragZoneOffsetLon);
        this.primaryCenter = this._toLeafletPoint(CC); // L.LatLng
        var REDRAW_GEOZONE = true;
        if (REDRAW_GEOZONE) {
            // redraw the entire Geozone
            this._JSDrawGeozone(jsvZoneType, jsvZoneRadiusMeters, this.geozonePoints, jsvZoneColor, this.primaryIndex, true);
            //mapProviderParseZones(jsvZoneList);
        } else {
            // just draw the single point-radius [zoneFeatures]
            var circleF = this._createCircleFeature(this.primaryCenter, jsvZoneRadiusMeters, GetGeozoneStyle(true,jsvZoneColor));
            if (circleF != null) {
                var features = [ circleF ];
                this._drawFeatures(true, features);
            }
        }
        domME.preventDefault(domME);
        domME.stopPropagation(domME);
        return false;
    }

    return true;

};

/**
*** MouseEvent handler to recenter map
*** @param me  The MouseEvent
**/
JSMap.prototype._event_OnClick = function(me) // MouseEvent
{
    var meXY  = me.layerPoint;    // L.Point
    var meLL  = me.latlng         // L.LatLng
    var domME = me.originalEvent; // DOMMouseEvent
    var shift = true; // !e.ctrlKey && !e.shiftKey && !e.altKey

    /* geozone mode */
    if (jsvGeozoneMode && jsvZoneEditable && shift) {
        var LL = this._toJSMapPoint(meLL); // where you clicked
        var CC = (this.primaryCenter !== null)? this._toJSMapPoint(this.primaryCenter) : new JSMapPoint(0.0,0.0); // where the primary center is
        var CCIsValid = geoIsValid(CC.lat,CC.lon);
        var CCLLDistKM = geoDistanceMeters(CC.lat, CC.lon, LL.lat, LL.lon);
        if (jsvZoneType == ZONE_POINT_RADIUS) {
            var radiusM = zoneMapGetRadius(false);
            // inside primary zone?
            if (CCLLDistKM <= radiusM) {
                return false;
            }
            // inside any zone?
            if (this.geozonePoints && (this.geozonePoints.length > 0)) {
                for (var i = 0; i < this.geozonePoints.length; i++) {
                    if (i == this.primaryIndex) { continue; }
                    var gpt = this.geozonePoints[i];
                    if (geoDistanceMeters(gpt.lat, gpt.lon, LL.lat, LL.lon) <= radiusM) {
                        return false; // inside this zone
                    }
                }
            }
            // outside geozone, recenter
            jsmSetPointZoneValue(LL.lat, LL.lon, radiusM);
            mapProviderParseZones(jsvZoneList);
            domME.preventDefault(domME);
            domME.stopPropagation(domME);
            return true;
        } else
        if (jsvZoneType == ZONE_POLYGON) {
            var radiusM = jsvZoneRadiusMeters; // vertice radius
            // inside primary vertice?
            if (CCLLDistKM <= radiusM) {
                return false;
            }
            // inside any vertice?
            if (this.geozonePoints && (this.geozonePoints.length > 0)) {
                for (var i = 0; i < this.geozonePoints.length; i++) {
                    if (i == this.primaryIndex) { continue; }
                    var gpt = this.geozonePoints[i];
                    if (geoDistanceMeters(gpt.lat, gpt.lon, LL.lat, LL.lon) <= radiusM) {
                        return false;
                    }
                }
            }
            // count number of valid points
            var count = 0;
            for (var x = 0; x < jsvZoneList.length; x++) {
                var pt = jsvZoneList[x];
                if (geoIsValid(pt.lat,pt.lon)) {
                    count++;
                }
            }
            if (count == 0) {
                // no valid points - create default polygon
                var radiusM = 450;
                var crLat   = geoRadians(LL.lat);  // radians
                var crLon   = geoRadians(LL.lon);  // radians
                for (x = 0; x < jsvZoneList.length; x++) {
                    var deg   = x * (360.0 / jsvZoneList.length);
                    var radM  = radiusM / EARTH_RADIUS_METERS;
                    if ((deg == 0.0) || ((deg > 170.0) && (deg < 190.0))) { radM *= 0.8; }
                    var xrad  = geoRadians(deg); // radians
                    var rrLat = Math.asin(Math.sin(crLat) * Math.cos(radM) + Math.cos(crLat) * Math.sin(radM) * Math.cos(xrad));
                    var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(radM) * Math.cos(crLat), Math.cos(radM)-Math.sin(crLat) * Math.sin(rrLat));
                    _jsmSetPointZoneValue(x, geoDegrees(rrLat), geoDegrees(rrLon), 0);
                }
            } else {
                // move valid points to new location
                var deltaLat = LL.lat - CC.lat;
                var deltaLon = LL.lon - CC.lon;
                for (var x = 0; x < jsvZoneList.length; x++) {
                    var pt = jsvZoneList[x];
                    if (geoIsValid(pt.lat,pt.lon)) {
                        _jsmSetPointZoneValue(x, (pt.lat + deltaLat), (pt.lon + deltaLon), 0);
                    }
                }
            }
            mapProviderParseZones(jsvZoneList);
            domME.preventDefault(domME);
            domME.stopPropagation(domME);
            return true;
        } else
        if (jsvZoneType == ZONE_SWEPT_POINT_RADIUS) {
            var radiusM = jsvZoneRadiusMeters;
            // inside primary zone?
            if (CCLLDistKM <= radiusM) {
                return false;
            }
            // inside any zone?
            if (this.geozonePoints && (this.geozonePoints.length > 0)) {
                for (var i = 0; i < this.geozonePoints.length; i++) {
                    if (i == this.primaryIndex) { continue; }
                    var gpt = this.geozonePoints[i];
                    if (geoDistanceMeters(gpt.lat, gpt.lon, LL.lat, LL.lon) <= radiusM) {
                        return false;
                    }
                }
            }
            // count number of valid points
            var count = 0;
            var maxDistKM = 0.0;
            var lastPT = null;
            for (var x = 0; x < jsvZoneList.length; x++) {
                var pt = jsvZoneList[x];
                if (geoIsValid(pt.lat,pt.lon)) {
                    count++;
                    if (lastPT !== null) {
                        var dkm = geoDistanceMeters(lastPT.lat, lastPT.lon, pt.lat, pt.lon);
                        if (dkm > maxDistKM) {
                            maxDistKM = dkm;
                        }
                    } else {
                        lastPT = pt; // first valid point
                    }
                }
            }
            var maxDeltaKM = ((maxDistKM > 5000)? maxDistKM : 5000) * 1.5;
            if (!CCIsValid || (count <= 0) || (CCLLDistKM <= maxDeltaKM)) {
                jsmSetPointZoneValue(LL.lat, LL.lon, radiusM);
            }
            // reparse zone
            mapProviderParseZones(jsvZoneList);
            domME.preventDefault(domME);
            domME.stopPropagation(domME);
            return true;
        } else {
            return false;
        }
    }

};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** PopupEvent handler to keep track of the open Popup
*** @param pe  The PopupEvent
**/
JSMap.prototype._event_PopupOpen = function(pe) // PopupEvent
{
    var popup  = pe.popup;
    var jsmMPP = popup.jsmMPP;
    if (jsmMPP) {
        jsmHighlightDetailRow(jsmMPP.rcdNdx, true);
    }
}

/**
*** PopupEvent handler to keep track of the open Popup
*** @param pe  The PopupEvent
**/
JSMap.prototype._event_PopupClose = function(pe) // PopupEvent
{
    var popup  = pe.popup;
    var jsmMPP = popup.jsmMPP;
    if (jsmMPP) {
        jsmHighlightDetailRow(jsmMPP.rcdNdx, false);
    }
}

// ----------------------------------------------------------------------------
