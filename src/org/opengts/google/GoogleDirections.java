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
// Support for Google Directions API
// ----------------------------------------------------------------------------
// Change History:
//  2016/09/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.google;

import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;

public class GoogleDirections
{

    // ------------------------------------------------------------------------
    // References:
    //  - https://developers.google.com/maps/documentation/roads/speed-limits
    //  - https://developers.google.com/console/help/new/#usingkeys
    //  - https://console.developers.google.com/project/164795502565/apiui/credential
    //
    // Directions API
    //  - https://maps.googleapis.com/maps/api/directions/json?origin=Disneyland&destination=Universal+Studios+Hollywood4&key=YOUR_API_KEY
    // ------------------------------------------------------------------------

    /* V1 URL */
    protected static final String   URL_Directions_         = "https://maps.googleapis.com/maps/api/directions/json?";

    /* Directions tags */
    public    static final String   TAG_geocoded_waypoints  = "geocoded_waypoints"; // array
    public    static final String   TAG_geocoder_status     = "geocoder_status";    // String
    public    static final String   TAG_types               = "types";              // array
    public    static final String   TAG_partial_match       = "partial_match";      // Boolean
    public    static final String   TAG_placeId             = "placeId";            // String

    public    static final String   TAG_routes              = "routes";             // array
    public    static final String   TAG_legs                = "legs";               // array
    public    static final String   TAG_distance            = "distance";           // Object
    public    static final String   TAG_duration            = "duration";           // Object
    public    static final String   TAG_steps               = "steps";              // array
    public    static final String   TAG_start_location      = "start_location";     // Object ["lat","lng"]
    public    static final String   TAG_end_location        = "end_location";       // Object ["lat","lng"]
    public    static final String   TAG_html_instructions   = "html_instructions";  // String
    public    static final String   TAG_maneuver            = "maneuver";           // String
    public    static final String   TAG_polyline            = "polyline";           // Object
    public    static final String   TAG_points              = "points";             // String
    public    static final String   TAG_overview_polyline   = "overview_polyline";  // Object

    public    static final String   TAG_status              = "status";             // String [OK|ZERO_RESULTS]

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* properties */
    public    static final String   PROP_directionsURL      = "directionsURL";
    public    static final String   PROP_directionsApiKey   = "directionsApiKey";

    // ------------------------------------------------------------------------

    protected static final int      TIMEOUT_DirectionsAPI   = 2500; // milliseconds

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

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

    public GoogleDirections(RTProperties rtProps)
    {
        super();
        this.props = (rtProps != null)? rtProps : new RTProperties();
    }

    // ------------------------------------------------------------------------

    public RTProperties getProperties()
    {
        return this.props;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the authorization key of this ReverseGeocodeProvider
    *** @return The access key of this reverse-geocode provider
    **/
    public String getAuthorization()
    {
        String apiKey = this.getProperties().getString(PROP_directionsApiKey,null);
        if (!StringTools.isBlank(apiKey)) {
            return apiKey;
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Directions API timeout (milliseconds)
    **/
    protected int getDirectionsTimeout()
    {
        return TIMEOUT_DirectionsAPI;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* gets the Google Directions URI */
    protected String getDirectionsURI()
    {
        return URL_Directions_;
    }

    /* gets the Google Directions URL */
    protected String getDirectionsURL(String fromAddr, String toAddr, Locale locale)
    {
        // -- URL parameters
        // -    "origin"      - From Address (required)
        // -    "destination" - To Address (required)
        // -    "region"      - country code
        // -    "waypoints"   - visited waypoints (ie: waypoints=optimize:true|LOCATION|LOCATION)
        // -    "avoid"       - "tolls"|"highways"|"ferries"

        /* predefined URL */
        String dirURL = this.getProperties().getString(PROP_directionsURL,null); // must already contain API_KEY
        if (!StringTools.isBlank(dirURL)) {
            URIArg uriArg = new URIArg(dirURL, true);
            uriArg.addArg("origin"     , fromAddr);
            uriArg.addArg("destination", toAddr);
            return uriArg.toString();
        }

        /* assemble URL */
        URIArg uriArg = new URIArg(this.getDirectionsURI(), true);
        uriArg.addArg("origin"     , fromAddr);
        uriArg.addArg("destination", toAddr);

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
    *** Gets the GeoRoute with directions from the specified 'fromAddr' to the specified 'toAddr'.
    *** @param fromAddr  The origination address
    *** @param toAddr    The destination address
    *** @param locale    The preferred locale
    *** @param attr      The additional attributes used to determine the returned route
    *** @return The GeoRoute
    **/
    public GeoRoute getDirections(String fromAddr, String toAddr, Locale locale, RTProperties attr)
    {

        /* URL */
        String url = this.getDirectionsURL(fromAddr, toAddr, locale);
        Print.logInfo("Google Directions URL: " + url);

        /* create JSON document */
        JSON jsonDoc = null;
        JSON._Object jsonObj = null;
        try {
            jsonDoc = GetJSONDocument(url, this.getDirectionsTimeout());
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

        /* get status */
        // -- "OK", "ZERO_RESULTS", ...
        String status = jsonObj.getStringForName(TAG_status,"");
        if (status.equalsIgnoreCase("ZERO_RESULTS")) {
            Print.logError("No results found: " + status);
            return null;
        } else
        if (!status.equalsIgnoreCase("OK")) {
            Print.logError("Invalid status: " + status);
            return null;
        }

        /* parse routes */
        JSON._Array routes = jsonObj.getArrayForName(TAG_routes, null);
        if (ListTools.isEmpty(routes)) {
            // -- nothing to report
            Print.logWarn("'"+TAG_routes+"' not found in response");
            return null;
        }

        /* iterate through routes/legs/steps */
        java.util.List<GeoPointProvider> detailPolyList  = new Vector<GeoPointProvider>();
        java.util.List<GeoPointProvider> summaryPolyList = new Vector<GeoPointProvider>();
        int rtSize = routes.size();
        for (int n = 0; n < rtSize; n++) {
            JSON._Object routesObj = routes.getObjectValueAt(n,null);
            if (routesObj != null) {
                // -- "legs"
                JSON._Array legs = routesObj.getArrayForName(TAG_legs, null);
                if (legs != null) {
                    int legsSize = legs.size();
                    for (int l = 0; l < legsSize; l++) {
                        JSON._Object legsObj = legs.getObjectValueAt(l,null);
                        // -- "steps"
                        JSON._Array steps = legsObj.getArrayForName(TAG_steps, null);
                        if (steps != null) {
                            int stepsSize = steps.size();
                            for (int s = 0; s < stepsSize; s++) {
                                JSON._Object stepsObj = steps.getObjectValueAt(s,null);
                                if (stepsObj != null) {
                                    // -- individual step
                                    String instructions = stepsObj.getStringForName(TAG_html_instructions,null);
                                    String maneuver     = stepsObj.getStringForName(TAG_maneuver,null);
                                    JSON._Object polylineObj = stepsObj.getObjectForName(TAG_polyline, null);
                                    if (polylineObj != null) {
                                        String points = polylineObj.getStringForName(TAG_points, null);
                                        if (!StringTools.isBlank(points)) {
                                            java.util.List<GeoPointProvider> stepPolyList  = new Vector<GeoPointProvider>();
                                            GooglePolyline.Decode(points, stepPolyList);
                                            GeoRoute step = new GeoRoute(stepPolyList);
                                            step.setInstructions(instructions);
                                            step.setManeuver(maneuver);
                                            detailPolyList.add(step);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // -- "overview_polyline"
                JSON._Object overviewPolylineObj = routesObj.getObjectForName(TAG_overview_polyline, null);
                if (overviewPolylineObj != null) {
                    String points = overviewPolylineObj.getStringForName(TAG_points, null);
                    if (!StringTools.isBlank(points)) {
                        GooglePolyline.Decode(points, summaryPolyList);
                        /*
                        int x1 = 0, x2 = 0;
                        for (;;) {
                            GeoRoute.Node gp1 = (x1 <  detailPolyList.size())? (GeoRoute.Node) detailPolyList.get(x1) : null;
                            GeoRoute.Node gp2 = (x2 < summaryPolyList.size())? (GeoRoute.Node)summaryPolyList.get(x2) : null;
                            if ((gp1 == null) && (gp2 == null)) {
                                Print.sysPrintln(x1+") GP1:------------------- ==> GP2:-------------------");
                                break;
                            } else
                            if (gp1 == null) {
                                Print.sysPrintln(x1+") GP1:------------------- ==> GP2:???????????????????");
                                break;
                            } else
                            if ((gp1 != null) && gp1.equals(gp2,0.00001)) {
                                String instr = gp1.getPropertyString(GeoRoute.NODE_instructions, "");
                                String manuv = gp1.getPropertyString(GeoRoute.NODE_maneuver    , "");
                                String msg = manuv + "  " + instr;
                                Print.sysPrintln(x1+") GP1:"+gp1+" ==> GP2:"+gp2 + "  " + msg);
                                x2++;
                            } else {
                                Print.sysPrintln(x1+") GP1:"+gp1+" ==> GP2:-------------------");
                            }
                            x1++;
                        }
                        */
                    }
                }
            }
        } // loop through routes

        /* assemble GeoRoute */
        GeoRoute route = null;
        if (!ListTools.isEmpty(detailPolyList)) {
            // -- detail route found
            route = new GeoRoute(detailPolyList);
            if (!ListTools.isEmpty(summaryPolyList)) {
                // -- add summary
                GeoPointProvider gpp[] = new GeoPointProvider[summaryPolyList.size()];
                route.setSummary(summaryPolyList.toArray(gpp));
            }
        } else
        if (!ListTools.isEmpty(summaryPolyList)) {
            // -- only summary route found
            route = new GeoRoute(summaryPolyList);
        } else {
            // -- no route found
            route = null;
        }

        /* return route */
        if (route != null) {
            return route;
        } else {
            Print.logWarn("No route defined in response");
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_roadsApiKey[]   = { PROP_directionsApiKey, "apiKey", "key" };
    private static final String ARG_origin[]        = { "origin"     , "from" };
    private static final String ARG_destination[]   = { "destination", "to"   };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        // -- api key
        RTProperties rtp = new RTProperties();
        rtp.setString(PROP_directionsApiKey, RTConfig.getString(ARG_roadsApiKey,""));
        GoogleDirections directions = new GoogleDirections(rtp);
        // -- directions
        String origin      = RTConfig.getString(ARG_origin     , null);
        String destination = RTConfig.getString(ARG_destination, null);
        if (!StringTools.isBlank(origin) && !StringTools.isBlank(destination)) {
            Print.sysPrintln("From Addr: " + origin);
            Print.sysPrintln("To Addr  : " + destination);
            GeoRoute route = directions.getDirections(origin, destination, null, null);
            if (route != null) {
                route.printRoute();
            } else {
                Print.logInfo("No GeoRoute");
            }
            System.exit(0);
        }
        // --
        System.exit(1);
    }
}
