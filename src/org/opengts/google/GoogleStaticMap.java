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
// Description:
//  Tools for obtaining static Google Maps for mobile devices
//      https://developers.google.com/maps/documentation/static-maps/
//      https://developers.google.com/maps/documentation/static-maps/upgrade
//  References:
//      http://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
//      http://slappy.cs.uiuc.edu/fall06/cs492/Group2/example.html  [dead link?]
//          zoom_level = log(ppd_lon/(256/360)) / log(2)
//          m/px = cos(lat) * (1 / 2^zoom) * (40075017 / 256)
//      http://blogs.esri.com/Support/blogs/mappingcenter/archive/2009/03/19/How-can-you-tell-what-map-scales-are-shown-for-online-maps_3F00_.aspx
//      http://squall.nrel.colostate.edu/cwis438/DisplayHTML.php?FilePath=D:/WebContent/Jim/GoogleMapsProjection.html&WebSiteID=9
// ----------------------------------------------------------------------------
// Change History:
//  2009/04/02  Martin D. Flynn
//     -Initial release
//  2016/09/01  Martin D. Flynn
//     -Removed obsolete "sensor" parameter
//  2016/12/21  Martin D. Flynn
//     -Fixed color tag (changed from "rgb:" to "color:" and removed '#' from color value
//     -Added initial support for determining the lat/lon bounds of the returned image [EXPERIMENTAL] (see "getMapImageBounds")
// ----------------------------------------------------------------------------
package org.opengts.google;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

import javax.swing.ImageIcon; // used to read the image width/height

import org.opengts.util.*;

/**
*** Tools for obtaining static Google Maps for mobile devices
**/

public class GoogleStaticMap
{

    // ------------------------------------------------------------------------

    public static final String  GOOGLE_MAP_URL          = "http://maps.googleapis.com/maps/api/staticmap"; // V2
    public static final String  GOOGLE_MAP_URL_SSL      = "https://maps.googleapis.com/maps/api/staticmap";
    
    // -- parameters (not all are supported)
    public static final String  PARM_center             = "center";
    public static final String  PARM_zoom               = "zoom";
    public static final String  PARM_size               = "size";
    public static final String  PARM_scale              = "scale";
    public static final String  PARM_maptype            = "maptype";
    public static final String  PARM_mobile             = "mobile";
    public static final String  PARM_language           = "language";
    public static final String  PARM_region             = "region";
  //public static final String  PARM_sensor             = "sensor";
    public static final String  PARM_format             = "format";     // PNG
    public static final String  PARM_path               = "path";
    public static final String  PARM_visible            = "visible";
    public static final String  PARM_style              = "style";
    public static final String  PARM_markers            = "markers";
    public static final String  PARM_key                = "key";
    public static final String  PARM_signature          = "signature";

    // -- zoom: 0..21
    public static final int     ZOOM_WORLD              =  1;
    public static final int     ZOOM_CONTINENT          =  5;
    public static final int     ZOOM_CITY               = 10;
    public static final int     ZOOM_STREET             = 15;
    public static final int     ZOOM_BUILDING           = 20;

    // ------------------------------------------------------------------------

    public static final String  MAPTYPE_ROADMAP         = "roadmap";
    public static final String  MAPTYPE_SATELLITE       = "satellite";
    public static final String  MAPTYPE_HYBRID          = "hybrid";
    public static final String  MAPTYPE_TERRAIN         = "terrain";

    // ------------------------------------------------------------------------

    public static final String  PUSHPIN_SIZE_TINY       = "tiny";
    public static final String  PUSHPIN_SIZE_MID        = "mid";
    public static final String  PUSHPIN_SIZE_SMALL      = "small";
    public static final String  PUSHPIN_SIZE[]          = new String[] {
        PUSHPIN_SIZE_TINY, 
        PUSHPIN_SIZE_MID,
        PUSHPIN_SIZE_SMALL
    };

    public static final String  PUSHPIN_COLOR_BLACK     = "black";
    public static final String  PUSHPIN_COLOR_BROWN     = "brown";
    public static final String  PUSHPIN_COLOR_RED       = "red";
    public static final String  PUSHPIN_COLOR_ORANGE    = "orange";
    public static final String  PUSHPIN_COLOR_YELLOW    = "yellow";
    public static final String  PUSHPIN_COLOR_GREEN     = "green";
    public static final String  PUSHPIN_COLOR_BLUE      = "blue";
    public static final String  PUSHPIN_COLOR_PURPLE    = "purple";
    public static final String  PUSHPIN_COLOR_GRAY      = "gray";
    public static final String  PUSHPIN_COLOR_WHITE     = "white";
    public static final String  PUSHPIN_COLOR[]         = new String[] {
        PUSHPIN_COLOR_BLACK,
        PUSHPIN_COLOR_BROWN,
        PUSHPIN_COLOR_RED,
        PUSHPIN_COLOR_ORANGE,
        PUSHPIN_COLOR_YELLOW,
        PUSHPIN_COLOR_GREEN,
        PUSHPIN_COLOR_BLUE,
        PUSHPIN_COLOR_PURPLE,
        PUSHPIN_COLOR_GRAY,
        PUSHPIN_COLOR_WHITE,
    };

    /**
    *** Creates a pushpin name based on the specified size, color, and tag
    *** @param size  The pushpin size ("tiny", "mid", "small")
    *** @param color The pushpin color ("red", "green", ...)
    *** @param tag   Alphanumeric letter/digit tag
    *** @return The composite pushpin name
    **/
    public static String CreatePushpinIcon(String size, String color, String tag)
    {
        String S = StringTools.blankDefault(size , PUSHPIN_SIZE_MID );
        String C = StringTools.blankDefault(color, PUSHPIN_COLOR_RED);
        if (PUSHPIN_SIZE_TINY.equals(S)) {
            return S + C;
        } else {
            String L = StringTools.blankDefault(tag,"").toLowerCase();
            return S + C + L;
        }
    }

    public static String DEFAULT_PUSHPIN = CreatePushpinIcon(PUSHPIN_SIZE_MID,PUSHPIN_COLOR_RED,"o");

    // ------------------------------------------------------------------------

    public static final ColorTools.RGB DEFAULT_PATH_COLOR     = ColorTools.RED;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- http://stackoverflow.com/questions/12507274/how-to-get-bounds-of-a-google-static-map

    private static final double   MERCATOR_Range              = 256.0;
    private static final double   MERCATOR_PixelsPerLonDegree = MERCATOR_Range / 360.0;
    private static final double   MERCATOR_PixelsPerLonRadian = MERCATOR_Range / (2.0 * Math.PI);
    private static final double   MERCATOR_OriginX            = MERCATOR_Range / 2.0;
    private static final double   MERCATOR_OriginY            = MERCATOR_Range / 2.0;

    /**
    *** Convert the PixelPoint to a GeoPoint
    **/
    private static GeoPoint _xyToGeoPoint(double X, double Y) 
    {
        double lonDeg = (X - MERCATOR_OriginX) /  MERCATOR_PixelsPerLonDegree;
        double latRad = (Y - MERCATOR_OriginY) / -MERCATOR_PixelsPerLonRadian;
        double latDeg = GeoPoint.radiansToDegrees((2.0 * Math.atan(Math.exp(latRad))) - (Math.PI / 2.0));
        return new GeoPoint(latDeg, lonDeg);
    }

    /**
    *** Gets the SouthWest corner Latitude/Longitude
    **/
    public static GeoBounds GetMapBounds(GeoPoint centerGP, int zoom, int width, int height)
    {
        // -- http://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
        try {
            // -- center X/Y
            double centerX  = MERCATOR_OriginX + (centerGP.getLongitude() * MERCATOR_PixelsPerLonDegree);
            double centSinY = Math.sin(centerGP.getLatitude() * (Math.PI / 180.0));
            if (centSinY < -0.9999) { centSinY = -0.9999; }
            if (centSinY >  0.9999) { centSinY =  0.9999; }
            double centerY  = MERCATOR_OriginY + (0.5 * Math.log((1.0 + centSinY) / (1.0 - centSinY)) * -MERCATOR_PixelsPerLonRadian);
            // -- bounds
            double    scale = Math.pow(2.0, (double)zoom); // (2 << zoom)
            double   scaleX = width  / 2.0 / scale;
            double   scaleY = height / 2.0 / scale;
            GeoPoint   gpSW = _xyToGeoPoint((centerX - scaleX), (centerY + scaleY));
            GeoPoint   gpNE = _xyToGeoPoint((centerX + scaleX), (centerY - scaleY));
            return new GeoBounds(gpSW, gpNE);
        } catch (Throwable th) {
            // -- in case of some ivalid mathematical operation
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private java.util.List<String>  pushpins    = new Vector<String>();

    private java.util.List<String>  pathLine    = new Vector<String>();
    private ColorTools.RGB          pathColor   = DEFAULT_PATH_COLOR;
    private int                     pathWeight  = 2;

    private GeoBounds               ppBounds    = new GeoBounds();
    private GeoPoint                center      = null;

    private int                     width       = 200;
    private int                     height      = 250;
    private String                  googleKey   = "";
    private String                  mapType     = MAPTYPE_HYBRID;
    private int                     zoom        = -1;

  //private boolean                 sensor      = false; // OBSOLETE

    public GoogleStaticMap()
    {
        //
    }

    public GoogleStaticMap(int width, int height, String key)
    {
        this.setSize(width, height);
        this.setGoogleKey(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** clears all pushpins
    **/
    public void resetPushpins(boolean resetZoom)
    {
        this.pushpins.clear();
        this.pathLine.clear();
        this.ppBounds.reset();
        this.setCenter(null);
        if (resetZoom) {
            this.setZoom(-1);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the preferred map size
    *** @return The map width
    **/
    public void setSize(PixelDimension pxd)
    {
        if (pxd != null) {
            this.setSize(pxd.getWidth(), pxd.getHeight());
        }
    }

    /**
    *** Sets the preferred map size
    *** @return The map width
    **/
    public void setSize(int W, int H)
    {
        this.width  = W;
        this.height = H;
    }

    /**
    *** Returns true if the size has been defined
    *** @return The map width
    **/
    public boolean hasSize()
    {
        return ((this.width > 0) && (this.height > 0))? true : false;
    }

    /**
    *** Gets the preferred map width
    *** @return The map width
    **/
    public int getWidth()
    {
        return this.width;
    }

    /**
    *** Gets the preferred map height
    *** @return The map height
    **/
    public int getHeight()
    {
        return this.height;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the Google Map key has been defined
    *** @return True if the Google Map key has been defined
    **/
    public boolean hasGoogleKey()
    {
        return !StringTools.isBlank(this.googleKey);
    }
    
    /**
    *** Gets the Google map authorization key
    *** @return The Google map authorization key
    **/
    public String getGoogleKey()
    {
        return this.googleKey;
    }
    
    /**
    *** Sets the Google map authorization key
    *** @param key The Google map authorization key
    **/
    public void setGoogleKey(String key)
    {
        this.googleKey = StringTools.trim(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the map type (mobile, roadmap, satellite, terrain, hybrid, ...)
    *** @param mapType  The map type
    **/
    public void setMapType(String mapType)
    {
        this.mapType = !StringTools.isBlank(mapType)? StringTools.trim(mapType) : MAPTYPE_HYBRID;
    }

    /**
    *** Gets the map type
    *** @return The map type
    **/
    public String getMapType()
    {
        return !StringTools.isBlank(mapType)? this.mapType : MAPTYPE_HYBRID;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the map image format.
    **/
    public String getImageFormat()
    {
        return "png";
    }

    // ------------------------------------------------------------------------

    private static final int ZOOM_METHOD = 2;

    /**
    *** Sets the current zoom level [from 0(lowest) to 21(highest)]
    *** @param zoom  The current zoom level
    **/
    public void setZoom(int zoom)
    {
        this.zoom = zoom;
    }

    /**
    *** Gets the current zoom level
    *** @return The current zoom level
    **/
    public int getZoom()
    {
        if (this.zoom < 0) {
            this.zoom = this.calculateZoom(ZOOM_METHOD);
        }
        return this.zoom;
    }

    /**
    *** Calculates the best zoom of the map based on added points
    *** @return The calculated zoom of the map
    **/
    public int calculateZoom(int zoomMeth)
    {
        if (!this.ppBounds.isValid()) {
            return ZOOM_CONTINENT;
        }
        switch (zoomMeth) {
            case 1: {
                // -- http://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
                double NLat = this.ppBounds.getTop();
                double SLat = this.ppBounds.getBottom();
                double ELon = this.ppBounds.getRight();
                double WLon = this.ppBounds.getLeft();
                double mapW = (double)this.getWidth();
                double mapH = (double)this.getHeight();
                double wrlW = 256.0; // World view width
                double wrlH = 256.0; // World view height
                int    maxZ = this.getMaxZoom();
                // --
                double NLatRad; {
                    double sin   = Math.sin(NLat * Math.PI / 180.0);
                    double radX2 = Math.log((1.0 + sin) / (1.0 - sin)) / 2.0;
                    NLatRad = Math.max(Math.min(radX2,Math.PI),-Math.PI) / 2.0;
                }
                // --
                double SLatRad; {
                    double sin   = Math.sin(SLat * Math.PI / 180.0);
                    double radX2 = Math.log((1.0 + sin) / (1.0 - sin)) / 2.0;
                    SLatRad = Math.max(Math.min(radX2,Math.PI),-Math.PI) / 2.0;
                }
                // --
                double latFrac = (NLatRad - SLatRad) / Math.PI;
                // --
                double lonDiff = ELon - WLon;
                double lonFrac = ((lonDiff < 0.0)? (lonDiff + 360.0) : lonDiff) / 360.0;
                // --
                int    latZoom = (int)Math.floor(Math.log(mapH / wrlH / latFrac) / GeoPoint.LN2);
                int    lonZoom = (int)Math.floor(Math.log(mapW / wrlW / lonFrac) / GeoPoint.LN2);
                // -- 
                int    zoom    = Math.min(Math.min(latZoom,lonZoom),maxZ);
                return zoom;
            }
            case 2: {
                // -- http://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
                double     mapW  = (double)this.getWidth();
                double     mapH  = (double)this.getHeight();
              //PixelPoint gpxSW = this.ppBounds.getSouthWest().getGooglePixelPoint();
              //PixelPoint gpxNE = this.ppBounds.getNorthEast().getGooglePixelPoint();
                PixelPoint gpxSE = this.ppBounds.getSouthEast().getGooglePixelPoint();
                PixelPoint gpxNW = this.ppBounds.getNorthWest().getGooglePixelPoint();
                for (int zoom = this.getMaxZoom(); zoom >= 0; zoom--) {
                    PixelPoint zoomSE = new PixelPoint(gpxSE.getIntX() >> (21 - zoom), gpxSE.getIntY() >> (21 - zoom));
                    PixelPoint zoomNW = new PixelPoint(gpxNW.getIntX() >> (21 - zoom), gpxNW.getIntY() >> (21 - zoom));
                    double zoomW = zoomSE.getIntX() - zoomNW.getIntX();
                    double zoomH = zoomSE.getIntY() - zoomNW.getIntY();
                    if ((zoomW <= mapW) && (zoomH <= mapH)) {
                        Print.logInfo("(#3) Returning zoom = " + zoom);
                        return zoom;
                    }
                }
                return 0;
            }
            default: {
                double ppd_lat = this.getHeight() / this.ppBounds.getDeltaLatitude();
                double ppd_lon = this.getWidth()  / this.ppBounds.getDeltaLongitude();
                double ppd = (ppd_lon < ppd_lat)? ppd_lon : ppd_lat;
                double zoom = Math.log(ppd_lon/(256.0/360.0)) / Math.log(2.0);
                int z = (int)Math.floor(zoom - 1.95);
                return (z >= 0)? z : 0;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum zoom level
    **/
    public int getMaxZoom()
    {
        return 21;
    }

    /**
    *** Gets the minimum zoom level
    **/
    public int getMinZoom()
    {
        return 0;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the map center point
    *** @param cp  The map center
    **/
    public void setCenter(GeoPoint cp)
    {
        this.center = ((cp != null) && cp.isValid())? cp : null;
    }

    /**
    *** Gets the center of the map (may return null)
    *** @return The map center (may be null)
    **/
    public GeoPoint getCenter()
    {
        if (this.center != null) {
            return this.center;
        } else
        if (this.ppBounds.isValid()) {
            return this.ppBounds.getCenter();
        } else {
            return GeoPoint.INVALID_GEOPOINT;
        }
    }

    /**
    *** Returns true if the center has been defined
    *** @return True if the center has been defined
    **/
    public boolean hasCenter()
    {
        return GeoPoint.isValid(this.getCenter());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the static map image bounds. [EXPERIMENTAL]
    *** @return  The static map image bounds, or null if this instance is invalid.
    **/
    public GeoBounds getMapImageBounds()
    {
        GeoPoint center = this.getCenter();
        if (GeoPoint.isValid(center)) {
            int zoom = this.getZoom();
            int mapW = this.getWidth();
            int mapH = this.getHeight();
            return GetMapBounds(center,zoom,mapW,mapH);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /** OBSOLETE
    *** Sets the 'sensor generated' state of the included pushpins
    *** @param sensor True if lat/lon is autogenerated
    **/
    //public void setSensorState(boolean sensor)
    //{
    //    this.sensor = sensor;
    //}

    /** OBSOLETE
    *** Should return true if latitude/longitude is auto-generated
    *** @return True if lat/lon is autogenerated
    **/
    //public boolean getSensorState()
    //{
    //    return this.sensor;
    //}

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified pushpin to the map 
    *** @param gpLat The latitude
    *** @param gpLon The lonfitude
    *** @param icon  The pushpin icon name
    **/
    public void addPushpin(double gpLat, double gpLon, String icon, boolean shadow, boolean addToPath)
    {
        if (GeoPoint.isValid(gpLat,gpLon)) {
            this.addPushpin(new GeoPoint(gpLat,gpLon), icon, '\0', shadow, addToPath);
        }
    }

    /**
    *** Adds the specified pushpin to the map 
    *** @param gpLat  The latitude
    *** @param gpLon  The lonfitude
    *** @param icon   The pushpin icon name
    **/
    public void addPushpin(double gpLat, double gpLon, String icon, char label, boolean shadow, boolean addToPath)
    {
        if (GeoPoint.isValid(gpLat,gpLon)) {
            this.addPushpin(new GeoPoint(gpLat,gpLon), icon, label, shadow, addToPath);
        }
    }

    /**
    *** Adds the specified pushpin to the map 
    *** @param gp        The pushpin location
    *** @param icon      The pushpin icon name
    *** @param shadow    True to include pushpin shadow
    *** @param addToPath True to include pushpin in drawn route
    **/
    public void addPushpin(GeoPoint gp, String icon, boolean shadow, boolean addToPath)
    {
        // https://developers.google.com/chart/image/docs/gallery/dynamic_icons#icon_markers
        char label = '\0';
        this.addPushpin(gp, icon, label, shadow, addToPath);
    }

    /**
    *** Adds the specified pushpin to the map 
    *** @param gp         The pushpin location
    *** @param colorIcon  The pushpin icon name
    *** @param label      The pushpin label character (currently upper case only)
    *** @param shadow     True to include pushpin shadow
    *** @param addToPath  True to include pushpin in drawn route
    **/
    public void addPushpin(GeoPoint gp, String colorIcon, char label, boolean shadow, boolean addToPath)
    {
        if (GeoPoint.isValid(gp)) {
            // -- center
            if (StringTools.startsWithIgnoreCase(colorIcon,"center")) {
                // -- this pushpin just indicates the center of the map
                this.setCenter(gp);
                return;
            }
            // -- Icon
            StringBuffer pp = new StringBuffer();
            if (StringTools.startsWithIgnoreCase(colorIcon,"icon:")) {
                // -- explicit icon specification
                pp.append(colorIcon);
            } else
            if (StringTools.startsWithIgnoreCase(colorIcon,"http:")) {
                // -- implicit icon specification
                pp.append("icon:").append(colorIcon);
            } else
            if (StringTools.startsWithIgnoreCase(colorIcon,"color:")) {
                // -- explicit color specification
                int p = colorIcon.indexOf("/"); // "color:red/Z"
                if (p >= 0) {
                    if ((label == '\0') && (colorIcon.length() > (p + 1))) {
                        label = colorIcon.charAt(p+1);
                    }
                    colorIcon = colorIcon.substring(0,p).trim();
                }
                pp.append(colorIcon);
            } else {
                // -- implicit color specification
                int p = colorIcon.indexOf("/"); // "red/Z"
                if (p >= 0) {
                    if ((label == '\0') && (colorIcon.length() > (p + 1))) {
                        label = colorIcon.charAt(p+1);
                    }
                    colorIcon = colorIcon.substring(0,p).trim();
                }
                pp.append("color:").append(StringTools.trim(colorIcon));
            }
            // -- Shadow
            if (shadow) {
                pp.append("|");
                pp.append("shadow:").append(shadow);
            }
            // -- Label
            if (Character.isLetterOrDigit(label)) {
                pp.append("|");
                pp.append("label:").append(Character.toUpperCase(label));
            } else {
                //pp.append("label:A"); // may be required?
            }
            // -- Latitude/Longitude
            String lat = GeoPoint.formatLatitude( gp.getLatitude() );
            String lon = GeoPoint.formatLongitude(gp.getLongitude());
            pp.append("|");
            pp.append(lat).append(",").append(lon);
            if (addToPath) {
                String rt  = lat + "," + lon;
                this.pathLine.add(rt);
            }
            // -- add to pushpins
            this.pushpins.add(pp.toString());
            this.ppBounds.extendByCircle(200.0, gp);
            //Print.logInfo("Bound: " + this.ppBounds);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a path has been defined
    **/
    public boolean hasPath()
    {
        return (ListTools.size(this.pathLine) >= 2)? true : false;
    }
    
    /**
    *** Gets the pushpin route line (path)
    **/
    public java.util.List<String> getPath()
    {
        return this.pathLine;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the route path color and weight
    *** @param rgb  The route path color
    *** @param weight The route path weight
    **/
    public void setPathAttributes(ColorTools.RGB rgb, int weight)
    {
        this.pathColor  = (rgb != null)? rgb : DEFAULT_PATH_COLOR;
        this.pathWeight = (weight < 1)? 1 : (weight > 10)? 10 : weight;
    }

    /**
    *** Gets the path color (null if undefined) 
    **/
    public ColorTools.RGB getPathColor()
    {
        return (this.pathColor != null)? this.pathColor : DEFAULT_PATH_COLOR;
    }

    /**
    *** Gets the path weight
    **/
    public int getPathWeight()
    {
        return this.pathWeight;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the number of pushpins currently on this map
    *** @return The current number of pushpins
    **/
    public int getPushpinCount()
    {
        return ListTools.size(this.pushpins);
    }
    
    /**
    *** Returns true if pushpins have been defined
    **/
    public boolean hasPushpins()
    {
        return !ListTools.isEmpty(this.pushpins);
    }
    
    /**
    *** Gets the pushpins
    **/
    public java.util.List<String> getPushpins()
    {
        return this.pushpins;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance has valid attributes
    **/
    public boolean isValid()
    {
        if (!this.hasCenter()) {
            Print.logWarn("Center is not defined");
            return false;
        } else
        if (!this.hasSize()) {
            Print.logWarn("Size is not defined");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Google Map URL for retrieving the map image data
    *** @return A String representation of the Google map URL
    **/
    public String getMapURL()
    {
        URIArg url = new URIArg(GOOGLE_MAP_URL);
        url.addNonEncodedCharacters(",:");

        /* center */
        if (this.hasCenter()) {
            GeoPoint gp = this.getCenter();
            url.addArg(PARM_center, gp.getLatitude() + "," + gp.getLongitude());
        }

        /* common arguments */
        url.addArg(PARM_zoom   , this.getZoom());
        url.addArg(PARM_size   , this.getWidth() + "x" + this.getHeight());
        url.addArg(PARM_maptype, this.getMapType());
        url.addArg(PARM_mobile , "true");
      //url.addArg(PARM_sensor , String.valueOf(this.getSensorState()));
        url.addArg(PARM_format , this.getImageFormat());

        /* path */
        if (this.hasPath()) {
            StringBuffer sb = new StringBuffer();
            sb.append("color:0x").append(this.getPathColor().toString(false)); // [2.6.4-B03]
            sb.append(",weight:").append(this.getPathWeight());
            for (String pt : this.getPath()) {
                sb.append("|");
                sb.append(pt);
            }
            url.addArg(PARM_path, sb.toString());
        }

        /* markers */
        if (this.hasPushpins()) {
            StringBuffer sb = new StringBuffer();
            for (String pp : this.getPushpins()) {
                //if (sb.length() > 0) { sb.append("|"); /*%7C*/ }
                //sb.append(pp);
                url.addArg(PARM_markers, pp);
            }
            //url.addArg("markers", sb.toString());
        }

        /* google key at the end */
        if (this.hasGoogleKey()) {
            url.addArg(PARM_key, this.getGoogleKey());
        }

        /* signature */
        // -- TODO:

        /* return URL */
        //Print.logInfo("StaticMap: " + url);
        return url.toString();

    }

    /**
    *** Gets the Google Map URL for retrieving the map image data
    *** @return A String representation of the Google map URL
    **/
    public String toString()
    {
        return this.getMapURL();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets an array of bytes containing the Google Map for the specified location
    *** @param timeoutMS Timeout milliseconds
    *** @return A byte array containing the PNG map image
    **/
    public byte[] getMap(int timeoutMS)
        throws Throwable
    {
        String url = this.getMapURL();
        return HTMLTools.readPage_GET(
            new URL(url),
            timeoutMS, -1);
        // -- Possible Exceptions:
        // -    UnknownHostException
        // -    NoRouteToHostException
        // -    ConnectException
        // -    SocketException
        // -    FileNotFoundException
        // -    HttpIOException
        // -    IOException
        // -    Throwable
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Command-line example: (replace lat/lon with preferred location)
    // -    bin/exeJava -gp=39.1234/-142.1234 -icon=green -type=roadmap -save=./staticMap.png

    private static final String ARG_KEY[]       = { "key"      };
    private static final String ARG_SIZE[]      = { "size"     }; // "<W>/<H>"
    private static final String ARG_GEOPOINT[]  = { "pp", "gp" }; // "<LAT>/<LON>,<LAT>/<LON>,..."
    private static final String ARG_ICON[]      = { "icon"     }; // "red"|"http://..."
    private static final String ARG_CENTER[]    = { "center"   }; // "<LAT>/<LON>"
    private static final String ARG_TYPE[]      = { "type"     }; // "hybrid", "roadmap"
    private static final String ARG_SAVE[]      = { "save"     }; // ./staticMap.png

    /**
    *** Debug/Test entry point
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String         mapKey    = RTConfig.getString(ARG_KEY, null);
        PixelDimension mapSize   = new PixelDimension(RTConfig.getString(ARG_SIZE, "640,480"));
        String         mapPPStr  = RTConfig.getString(ARG_GEOPOINT, "");
        GeoPoint       mapCenter = new GeoPoint(RTConfig.getString(ARG_CENTER, null));
        String         ppIcon    = RTConfig.getString(ARG_ICON, "red");
        String         mapType   = RTConfig.getString(ARG_TYPE, MAPTYPE_HYBRID);

        /* create GoogleStaticMap instance */
        GoogleStaticMap gsm = new GoogleStaticMap();
        gsm.setPathAttributes(ColorTools.COLOR_RED, 2);
        gsm.setGoogleKey(mapKey);
        gsm.setCenter(mapCenter);
        gsm.setSize(mapSize);
        gsm.setMapType(mapType);

        /* pushpins */
        boolean inclPath = false;
        String gps[] = StringTools.parseStringArray(mapPPStr,',');
        for (String g : gps) {
            GeoPoint gp = new GeoPoint(g);
            if (gp.isValid()) {
                char label = 'S';
                gsm.addPushpin(gp, ppIcon, label, true/*shadow*/, inclPath);
            } else {
                Print.sysPrintln("Invalid GeoPoint: " + g);
            }
        }

        /* must have a center, or pushpins */
        if (!gsm.isValid()) {
            Print.sysPrintln("Invalid GoogleStaticMap attributes ...");
            System.exit(99);
        }

        /* map size, zoom, center */
        String    mapLink   = gsm.getMapURL();
        GeoBounds mapBounds = gsm.getMapImageBounds();
        Print.sysPrintln("Map Size    : " + gsm.getWidth() + "/" + gsm.getHeight());
        Print.sysPrintln("Map Zoom    : " + gsm.getZoom());
        Print.sysPrintln("Map Center  : " + gsm.getCenter());
        Print.sysPrintln("Map Link    : " + mapLink);
        Print.sysPrintln("Pixel Bounds: " + mapBounds);

        /* get static map */
        File saveFile = RTConfig.getFile(ARG_SAVE,null);
        if (saveFile == null) {
            Print.sysPrintln("[WARN] Missing '-save=FILE', map image will not be saved");
            System.exit(0);
        } else
        if (saveFile.exists()) {
            Print.sysPrintln("[ERROR] file already exists: " + saveFile);
            System.exit(1);
        } else {
            try {
                byte b[] = gsm.getMap(5000);
                if (!ListTools.isEmpty(b)) {
                    ImageIcon mapImg = new ImageIcon(b); 
                    int mapW = mapImg.getIconWidth();
                    int mapH = mapImg.getIconHeight();
                    FileTools.writeFile(b, saveFile);
                    Print.sysPrintln("Static map image ["+mapW+"/"+mapH+"] written to file: " + saveFile);
                } else {
                    Print.sysPrintln("[ERROR] Static map image is blank");
                    System.exit(3);
                }
            } catch (Throwable th) {
                Print.sysPrintln("[ERROR] Unable to read static map: " + th);
                System.exit(2);
            }
        }

    }
    
}
