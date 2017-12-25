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
//  Support for Google Roads API
// ----------------------------------------------------------------------------
// Change History:
//  2015/08/16  Martin D. Flynn
//     -Initial release
//  2016/01/04  Martin D. Flynn
//     -Ignore speed limits above 250 (ie. unknown==999, etc) [2.6.1-B11]
//  2016/09/01  Martin D. Flynn
//     -Moved to org.opengts.google
// ----------------------------------------------------------------------------
package org.opengts.google;

import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;

public class GoogleRoads
{

    // ------------------------------------------------------------------------
    // References:
    //  - https://developers.google.com/maps/documentation/roads/speed-limits
    //  - https://developers.google.com/console/help/new/#usingkeys
    //  - https://console.developers.google.com/project/164795502565/apiui/credential
    //
    // Speed-Limit API
    //  - https://roads.googleapis.com/v1/speedLimits?key=API_KEY&path=60.170880,24.942795
    //  - https://roads.googleapis.com/v1/speedLimits?key=API_KEY&placeId=ChIJ1Wi6I2pNFmsRQL9GbW7qABM
    //    {
    //      "speedLimits": [
    //        {
    //          "placeId": "ChIJ1Wi6I2pNFmsRQL9GbW7qABM",
    //          "speedLimit": 104.60736,
    //          "units": "KPH"
    //        }
    //      ]
    //    }
    // SnapToRoads API
    //  - https://developers.google.com/maps/documentation/roads/snap
    //  - https://roads.googleapis.com/v1/snapToRoads?path=-35.27801,149.12958|-35.28032,149.12907|-35.28099,149.12929|-35.28144,149.12984|-35.28194,149.13003|-35.28282,149.12956|-35.28302,149.12881|-35.28473,149.12836&interpolate=true&key=YOUR_API_KEY
    //    {
    //      "snappedPoints": [
    //        {
    //          "location": {
    //            "latitude": -35.2784167,
    //            "longitude": 149.1294692
    //          },
    //          "originalIndex": 0,
    //          "placeId": "ChIJoR7CemhNFmsRQB9QbW7qABM"
    //        },
    //        {
    //          "location": {
    //            "latitude": -35.280321693840129,
    //            "longitude": 149.12908274880189
    //          },
    //          "originalIndex": 1,
    //          "placeId": "ChIJiy6YT2hNFmsRkHZAbW7qABM"
    //        },
    //        ...
    //      ]
    //    }
    // ------------------------------------------------------------------------

    /* V1 URL */
    protected static final String   URL_SpeedLimits_        = "https://roads.googleapis.com/v1/speedLimits?";
    protected static final String   URL_SnapToRoads_        = "https://roads.googleapis.com/v1/snapToRoads?";

    /* SpeedLimits tags */
    public    static final String   TAG_speedLimits         = "speedLimits";
    public    static final String   TAG_placeId             = "placeId";
    public    static final String   TAG_speedLimit          = "speedLimit";
    public    static final String   TAG_units               = "units";      // "KPH"|"MPH"

    /* SnapToRoad tags */
    public    static final String   TAG_snappedPoints       = "snappedPoints";
    public    static final String   TAG_location            = "location";
    public    static final String   TAG_latitude            = "latitude";
    public    static final String   TAG_longitude           = "longitude";
    public    static final String   TAG_originalIndex       = "originalIndex";
    public    static final String   TAG_warningMessage      = "warningMessage";
  //public    static final String   TAG_placeId             = "placeId";

    // ------------------------------------------------------------------------

    // -- maximum number of points that the Road API accepts in a single snap-to-road
    public    static final int      SNAPTOROAD_MaxPoints    = 100;

    // -- maximum recommended distance between points
    // -  Google may refuse to calculate a snap-to-road for points that are too far apart
    // -  The maximum distance seems to be right at 500 meters
  //public    static final double   SNAPTOROAD_MaxDistanceM = 300.0; // recommended, but not practical
    public    static final double   SNAPTOROAD_MaxDistanceM = 500.0; // 

    // ------------------------------------------------------------------------

    /* properties */
    public    static final String   PROP_roadsApiKey        = "roadsApiKey";        // speed   snap
    public    static final String   PROP_speedLimitsURL     = "speedLimitsURL";     // speed
    public    static final String   PROP_snapToRoadsURL     = "snapToRoadsURL";     //         snap
    public    static final String   PROP_maxSnapDistanceM   = "maxSnapDistanceM";   //         snap

    /* attributes */
    public    static final String   ATTR_interpolate        = "interpolate";

    // ------------------------------------------------------------------------

    protected static final int      TIMEOUT_RoadsAPI        = 3000; // milliseconds

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Reads a JSON object from the specified URL
    **/
    private static JSON GetJSONDocument(String url, int timeoutMS)
    {
        JSON jsonDoc = null;
        HTMLTools.HttpBufferedInputStream input = null;
        try {
            input = HTMLTools.inputStream_GET(url, null, timeoutMS);
            jsonDoc = new JSON(input);
        } catch (JSON.JSONParsingException jpe) {
            Print.logError("JSON parse error: " + jpe);
        } catch (HTMLTools.HttpIOException hioe) {
            // -- IO error: java.io.IOException: 
            int    rc = hioe.getResponseCode();
            String rm = hioe.getResponseMessage();
            Print.logError("HttpIOException ["+rc+"-"+rm+"]: " + hioe.getMessage());
        } catch (IOException ioe) {
            Print.logError("IOException: " + ioe.getMessage());
        } finally {
            if (input != null) {
                try { input.close(); } catch (Throwable th) {/*ignore*/}
            }
        }
        return jsonDoc;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private RTProperties props = null;

    /**
    *** Constructor
    **/
    public GoogleRoads(RTProperties rtProps)
    {
        super();
        this.props = (rtProps != null)? rtProps : new RTProperties();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the properties for this GeoRoute (not null)
    **/
    public RTProperties getProperties()
    {
        return this.props;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the authorization key for the Google Roads API
    *** @return The access key of the Google Roads API
    **/
    public String getAuthorization()
    {
        String apiKey = this.getProperties().getString(PROP_roadsApiKey,null);
        if (!StringTools.isBlank(apiKey)) {
            return apiKey;
        } else {
            Print.logWarn("'"+PROP_roadsApiKey+"' not defined");
            this.getProperties().printProperties("Looking for " + PROP_roadsApiKey);
            return "";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Roads API timeout
    **/
    protected int getRoadsTimeout()
    {
        return TIMEOUT_RoadsAPI;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the SpeedLimit URI
    **/
    protected String getSpeedLimitsURI()
    {
        return URL_SpeedLimits_;
    }

    /**
    *** Gets the SpeedLimit URL with the specified GeoPoint
    **/
    protected String getSpeedLimitsURL(GeoPoint gp)
    {
        String gps = (gp != null)? (gp.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null)+","+gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null)) : "";

        /* predefined URL */
        String rgURL = this.getProperties().getString(PROP_speedLimitsURL,null); // must already contain API_KEY
        if (!StringTools.isBlank(rgURL)) {
            URIArg uriArg = new URIArg(rgURL, true);
            uriArg.addArg("path", gps);
            return uriArg.toString();
        }

        /* assemble URL */
        URIArg uriArg = new URIArg(this.getSpeedLimitsURI(), true);
        uriArg.addArg("units", "KPH");
        uriArg.addArg("path", gps);

        /* API_KEY */
        String apiKey = this.getAuthorization(); // API_KEY
        if (StringTools.isBlank(apiKey) || apiKey.startsWith("*")) {
            // -- invalid key
        } else {
            uriArg.addArg("key", apiKey);
        }

        /* return url */
        return uriArg.toString();

    }

    /** 
    *** Gets the speed limit for the specified location 
    *** or < 0 if unable to obtain the speed limit.
    **/
    public double getSpeedLimitKPH(GeoPoint gp)
    {

        /* URL */
        String url = this.getSpeedLimitsURL(gp);
        Print.logInfo("Google Roads SpeedLimit URL: " + url);

        /* create JSON document */
        JSON jsonDoc = null;
        JSON._Object jsonObj = null;
        try {
            jsonDoc = GetJSONDocument(url, this.getRoadsTimeout());
            //Print.logInfo("Response:\n"+jsonDoc);
            jsonObj = (jsonDoc != null)? jsonDoc.getObject() : null;
            if (jsonObj == null) {
                Print.logWarn("Unable to obtain top-level JSON object");
                return -1.0;
            }
        } catch (Throwable th) {
            Print.logException("Error", th);
            return -1.0;
        }

        /* parse speed limit */
        JSON._Array speedLimits = jsonObj.getArrayForName(TAG_speedLimits,null);
        if (ListTools.isEmpty(speedLimits)) {
            // -- nothing to report
            return -1.0;
        }

        /* iterate through speed limits */
        int slSize = speedLimits.size();
        for (int n = 0; n < slSize; n++) {
            JSON._Object speedLimitObj = speedLimits.getObjectValueAt(n,null);
            if (speedLimitObj != null) {
                String  speedUnits = speedLimitObj.getStringForName(TAG_units, "KPH");
                boolean isUnitMPH  = speedUnits.equalsIgnoreCase("MPH")? true : false;
                double  speedLimit = speedLimitObj.getDoubleForName(TAG_speedLimit, -1.0);
                if (speedLimit < 0.0) {
                    // -- ignore invalid speed limits < 0
                } else
                if (speedLimit > 250.0) {
                    // -- not a valid speed limit (unknown == 999)
                } else {
                    // -- reasonable speed limit
                    double kph = isUnitMPH? (speedLimit * GeoPoint.KILOMETERS_PER_MILE) : speedLimit;
                    return kph;
                }
            }
        }

        /* not found */
        return -1.0;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the SnapToRoad URI
    **/
    protected String getSnapToRoadsURI()
    {
        return URL_SnapToRoads_;
    }

    /**
    *** Gets the SnapToRoad URL with the specified GeoPoints
    **/
    protected String getSnapToRoadsURL(GeoPointProvider gppa[], int pathOfs, int pathLen, 
        boolean interpolate, Locale locale)
    {

        /* invalid offset? */
        if (pathOfs < 0) {
            // -- invalid offset
            return null;
        } else
        if (pathOfs >= ListTools.size(gppa)) {
            // -- excessive offset
            return null;
        }

        /* invalid length? */
        if (pathLen < 2) {
            // -- must have at least 2 points
            return null;
        } else
        if (pathLen > SNAPTOROAD_MaxPoints) {
            pathLen = SNAPTOROAD_MaxPoints;
        }

        /* assemble path */
        StringBuffer pathSB = new StringBuffer();
        if (!ListTools.isEmpty(gppa)) {
            for (int i = 0; i < pathLen; i++) {
                int n = i + pathOfs;
                if (n > gppa.length) { break; }
                GeoPointProvider gpp = gppa[n];
                if (GeoPoint.isValid(gpp)) {
                    GeoPoint gp = gpp.getGeoPoint();
                    if (pathSB.length() > 0) { pathSB.append("|"); }
                    pathSB.append(gp.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null));
                    pathSB.append(",");
                    pathSB.append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));
                }
            }
        }
        String path = pathSB.toString();
        if (StringTools.isBlank(path)) {
            // -- no path specified
            return null;
        }

        /* predefined URL */
        String rgURL = this.getProperties().getString(PROP_snapToRoadsURL,null); // must already contain API_KEY
        if (!StringTools.isBlank(rgURL)) {
            URIArg uriArg = new URIArg(rgURL, true);
            uriArg.addArg("path", path);
            if (interpolate) {
                uriArg.addArg("interpolate", "true");
            }
            return uriArg.toString();
        }
        // --------------------------------------

        /* assemble URL */
        URIArg uriArg = new URIArg(this.getSnapToRoadsURI(), true);
        uriArg.addArg("path", path);
        if (interpolate) {
            uriArg.addArg("interpolate", "true");
        }

        /* API_KEY */
        String apiKey = this.getAuthorization(); // API_KEY
        if (StringTools.isBlank(apiKey) || apiKey.startsWith("*")) {
            // -- invalid key
        } else {
            uriArg.addArg("key", apiKey);
        }

        /* return url */
        return uriArg.toString();

    }

    /**
    *** Returns a GeoRoute of the requested path which has been snapped to the nearest road
    *** @param path      An array of GeoPoints representing the path
    *** @param pathOfs   The offset into the path array of the points to send to the snap-to-road service
    *** @param pathLen   The number of points to send to the snap-to-road service
    *** @param locale    The locale
    *** @param attr      The routing attributes
    *** @return The requested route
    **/
    private GeoRoute _getSnapToRoad(GeoPointProvider path[], int pathOfs, int pathLen, Locale locale, RTProperties attr)
    {
        long startMS = System.currentTimeMillis();
        int  maxLen  = ListTools.size(path);

        /* invalid offset */
        if (pathOfs < 0) {
            return null;
        } else
        if (pathOfs >= maxLen) {
            return null;
        }

        /* invalid length? */
        if (pathLen <= 0) {
            return null;
        } else
        if ((pathOfs + pathLen) > maxLen) {
            pathLen = maxLen - pathOfs;
        }

        /* at least 2 points? */
        if (pathLen < 2) {
            return null;
        }

        /* attributes */
        boolean interpolate = (attr != null)? attr.getBoolean(ATTR_interpolate,false) : false;

        /* URL */
        String url = this.getSnapToRoadsURL(path, pathOfs, pathLen, interpolate, locale);
        Print.logInfo("Google Roads SnapToRoad URL: " + url);
        if (StringTools.isBlank(url)) {
            return null;
        }

        /* create JSON document */
        JSON jsonDoc = null;
        JSON._Object jsonObj = null;
        try {
            jsonDoc = GetJSONDocument(url, this.getRoadsTimeout());
            //Print.logInfo("Response:\n"+jsonDoc);
            jsonObj = (jsonDoc != null)? jsonDoc.getObject() : null;
            if (jsonObj == null) {
                Print.logWarn("Unable to obtain top-level JSON object");
                return null;
            }
        } catch (Throwable th) {
            Print.logException("Error", th);
            return null;
        }

        /* warning message */
        String warningMessage = jsonObj.getStringForName(TAG_warningMessage,null);

        /* parse snapped points */
        JSON._Array snappedPoints = jsonObj.getArrayForName(TAG_snappedPoints,null);
        if (ListTools.isEmpty(snappedPoints)) {
            // -- nothing to report
            return null;
        }

        /* original point count */
        int originalCount = 0;
        int firstOriginalIndex = -1;
        int lastOriginalIndex = -1;

        /* iterate through snapped points */
        Vector<GeoPoint> pathGP = new Vector<GeoPoint>();
        int spSize = snappedPoints.size();
        for (int n = 0; n < spSize; n++) {
            JSON._Object snappedPointObj = snappedPoints.getObjectValueAt(n,null);
            if (snappedPointObj != null) {
                int originalIndex = snappedPointObj.getIntForName(TAG_originalIndex,-1);
                JSON._Object location = snappedPointObj.getObjectForName(TAG_location,null);
                if (location != null) {
                    double lat = location.getDoubleForName(TAG_latitude ,0.0);
                    double lon = location.getDoubleForName(TAG_longitude,0.0);
                    if (GeoPoint.isValid(lat,lon)) {
                        if (originalIndex >= 0) {
                            GeoRoute.GPNode node = new GeoRoute.GPNode(lat,lon);
                            if (firstOriginalIndex < 0) {
                                firstOriginalIndex = pathOfs + originalIndex;
                            }
                            lastOriginalIndex = pathOfs + originalIndex;
                            node.setOriginalIndex(lastOriginalIndex);
                            pathGP.add(node);
                            originalCount++;
                        } else {
                            GeoPoint gp = new GeoPoint(lat,lon);
                            pathGP.add(gp);
                        }
                    }
                }
            }
        }

        /* check that all original points were accounted for */
        if (originalCount != pathLen) {
            //Print.logWarn("Not all original points accounted for.  Expected "+pathLen+", Found "+originalCount);
        } else {
            //Print.logInfo("All original points accounted for.  Expected "+pathLen+", Found "+originalCount);
        }

        /* nothing to return? */
        if (ListTools.isEmpty(pathGP)) {
            return null;
        }

        /* return GeoRoute */
        GeoRoute route = new GeoRoute(pathGP);
        route.setElapsedTimeMS(System.currentTimeMillis() - startMS);
        if (!StringTools.isBlank(warningMessage)) {
            route.setWarningMessage(warningMessage);
            Print.logWarn("RoadsAPIWarning: " + warningMessage);
        }
        route.setFirstOriginalIndex(firstOriginalIndex);
        route.setLastOriginalIndex(lastOriginalIndex);
        return route;

    }

    /**
    *** Returns a GeoRoute of the requested path which has been snapped to the nearest road
    *** @param path      An array of GeoPoints representing the path
    *** @param locale    The locale
    *** @param attr      The routing attributes
    *** @return The requested route (or null if no route is found)
    **/
    public GeoRoute getSnapToRoad(GeoPointProvider path[], Locale locale, RTProperties attr)
    {
        long startMS = System.currentTimeMillis();

        /* path length */
        int pathLen = ListTools.size(path);
        if (pathLen < 2) {
            return null;
        }
        //Print.logInfo("Original path length: " + pathLen);

        /* GeoPointProvider accumulator */
        java.util.List<GeoPointProvider> routeGPP = new Vector<GeoPointProvider>();

        /* maximum snap distance between points (meters) */
        double maxSnapDistanceM = this.getProperties().getDouble(PROP_maxSnapDistanceM, SNAPTOROAD_MaxDistanceM);

        /* break points into segments, if necessary */
        // -- break into segments if
        // -    - The distance between points is more than "maxSnapDistanceM"
        // -    - The number of points exceeds "SNAPTOROAD_MaxPoints"
        // -    - Google snapToRoad did not process all points
        int state = 0;
        int runStartNdx = -1;
        boolean hasRoute = false;
        checkPath:
        for (int pti = 0; pti <= pathLen;) {
            boolean lastPass = (pti == (pathLen - 1))? true : false;
            switch (state) {
                case 0: // starting (first point)
                    if (pti == pathLen) {
                        // -- we just started and we are at the end of the list
                        // -  (will not occur since we are guaranteed to have at least 2 points)
                        routeGPP.add(path[pti]);
                        //Print.logInfo("Added GeoPoint #"+(pti)+" "+path[pti].getGeoPoint());
                        state = 99; // done
                        break checkPath;
                    } else {
                        state = 1; // check last/prior point
                    }
                    break;
                case 1: // check to see if last/prior point is beginning of run
                    if (pti == pathLen) {
                        // -- done with list, add last/prior point
                        routeGPP.add(path[pti-1]);
                        //Print.logInfo("Added GeoPoint #"+(pti-1)+" "+path[pti-1].getGeoPoint());
                        state = 99; // done
                        break checkPath;
                    } else
                    if (GeoPoint.deltaMeters(path[pti-1],path[pti]) <= maxSnapDistanceM) {
                        // -- last/prior point is start of run
                        runStartNdx = pti - 1;
                        state = 2; // start checking current points
                    } else {
                        // -- last/prior point is solo
                        routeGPP.add(path[pti-1]);
                        //Print.logInfo("Added GeoPoint #"+(pti-1)+" "+path[pti-1].getGeoPoint());
                        state = 1; // still checking for start of run
                    }
                    break;
                case 2: // check to see if current point is part of current run
                    if (pti == pathLen) {
                        // -- done with list, add last route
                        int runLen = pti - runStartNdx;
                        GeoRoute r = this._getSnapToRoad(path, runStartNdx, runLen, locale, attr);
                        if (r == null) {
                            return null;
                        }
                        // -- add any leading unprocessed points
                        int firstOrigNdx = r.getFirstOriginalIndex();
                        if (firstOrigNdx > runStartNdx) {
                            // -- a bunch of points were skipped, add them as solo points
                            for (int p = runStartNdx; p < firstOrigNdx; p++) {
                                routeGPP.add(path[p]);
                                //Print.logInfo("Added GeoPoint #"+p+" "+path[p].getGeoPoint());
                            }
                        }
                        // -- add route
                        routeGPP.add(r);
                        hasRoute = true;
                        // -- check for any unprocessed trailing points
                        int lastOrigNdx = r.getLastOriginalIndex();
                        if (lastOrigNdx < (runStartNdx + runLen - 1)) {
                            // -- not all points were processed, restart at last unprocessed point
                            //Print.logInfo("Added GeoRoute #"+firstOrigNdx+" .. #"+lastOrigNdx+" (partial)");
                            pti = lastOrigNdx + 1; // restart where snapToRoad left off
                            runStartNdx = -1;
                            state = 1;
                            //continue;
                        } else {
                            //Print.logInfo("Added GeoRoute #"+firstOrigNdx+" .. #"+(pti-1));
                            state = 99; // done
                            break checkPath;
                        }
                    } else
                    if (GeoPoint.deltaMeters(path[pti-1],path[pti]) <= maxSnapDistanceM) {
                        // -- continuation of current run
                        if ((pti - runStartNdx + 1) >= SNAPTOROAD_MaxPoints) {
                            // -- at 100 point limit
                            int runLen = pti - runStartNdx + 1;
                            GeoRoute r = this._getSnapToRoad(path, runStartNdx, runLen, locale, attr);
                            if (r == null) {
                                return null;
                            }
                            // -- add any leading unprocessed points
                            int firstOrigNdx = r.getFirstOriginalIndex();
                            if (firstOrigNdx > runStartNdx) {
                                // -- a bunch of points were skipped, add them as solo points
                                for (int p = runStartNdx; p < firstOrigNdx; p++) {
                                    routeGPP.add(path[p]);
                                    //Print.logInfo("Added GeoPoint #"+p+" "+path[p].getGeoPoint());
                                }
                            }
                            // -- add route
                            routeGPP.add(r);
                            hasRoute = true;
                            // -- check for any unprocessed trailing points
                            int lastOrigNdx = r.getLastOriginalIndex();
                            if (lastOrigNdx < (runStartNdx + runLen - 1)) {
                                // -- not all points were processed, restart at last unprocessed point
                                //Print.logInfo("Added GeoRoute #"+firstOrigNdx+" .. #"+lastOrigNdx+" (partial)");
                                pti = lastOrigNdx + 1; // restart where snapToRoad left off
                                runStartNdx = -1;
                                state = 1;
                                //continue;
                            } else {
                                // -- new run at current point
                                //Print.logInfo("Added GeoRoute #"+firstOrigNdx+" .. #"+pti);
                                runStartNdx = pti; // start new run at current point (overlap 1 point)
                                state = 2; // still checking current points
                            }
                        } else {
                            state = 2; // still checking current points
                        }
                    } else {
                        // -- prior point marks end of run. 
                        int runLen = (pti - 1) - runStartNdx + 1;
                        GeoRoute r = this._getSnapToRoad(path, runStartNdx, runLen, locale, attr);
                        if (r == null) {
                            return null;
                        }
                        // -- add any leading unprocessed points
                        int firstOrigNdx = r.getFirstOriginalIndex();
                        if (firstOrigNdx > runStartNdx) {
                            // -- a bunch of points were skipped, add them as solo points
                            for (int p = runStartNdx; p < firstOrigNdx; p++) {
                                routeGPP.add(path[p]);
                                //Print.logInfo("Added GeoPoint #"+p+" "+path[p].getGeoPoint());
                            }
                        }
                        // -- add route
                        routeGPP.add(r);
                        hasRoute = true;
                        // -- check for any unprocessed trailing points
                        int lastOrigNdx = r.getLastOriginalIndex();
                        if (lastOrigNdx < (runStartNdx + runLen - 1)) {
                            // -- not all points were processed, restart at last unprocessed point
                            //Print.logInfo("Added GeoRoute #"+firstOrigNdx+" .. #"+lastOrigNdx+" (partial)");
                            pti = lastOrigNdx + 1; // restart where snapToRoad left off
                            runStartNdx = -1;
                            state = 1;
                            //continue;
                        } else {
                            //Print.logInfo("Added GeoRoute #"+firstOrigNdx+"..#"+(pti-1));
                            runStartNdx = -1;
                            state = 1; // go back to checking for beginning of run
                        }
                    }
                    break;
            } // switch (state)
            pti++;
        } // checkPath loop

        /* return */
        GeoRoute route = null;
        int routeLen = ListTools.size(routeGPP);
        if (routeLen <= 0) {
            // -- no route points
            return null;
        } else
        if (!hasRoute) {
            // -- no GeoRoute was added to the list
            return null;
        } else
        if (routeLen == 1) {
            GeoPointProvider gpp = routeGPP.get(0);
            if (gpp instanceof GeoRoute) {
                // -- contains a single GeoRoute entry
                route = (GeoRoute)gpp;
            } else {
                // -- entry is not a GeoRoute
                // -  (will not occur since we already guaranteed a GeoReoute above)
                return null;
            }
        } else {
            // -- create a route of the contained points/routes
            route = new GeoRoute(routeGPP);
            route.setElapsedTimeMS(System.currentTimeMillis() - startMS);
        }

        /* return route */
        //Print.logInfo("Original Points:");
        //for (int i = 0; i < path.length; i++) { Print.logInfo("  "+i+") Original: " + path[i].getGeoPoint()); }
        //Print.logInfo("Route Points:");
        //route.printRoute();
        return route;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
}
