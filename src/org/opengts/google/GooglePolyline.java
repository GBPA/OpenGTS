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
//  2017/05/12  Martin D. Flynn
//     -Extracted from GoogleDirections
// ----------------------------------------------------------------------------
package org.opengts.google;

import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;

public class GooglePolyline
{

    private static final double MINIMUM_DISTANCE_METERS = 0.0;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Decodes the specified Polyline
    *** @param polyline  The polyline to decode
    *** @return A list of GeoPoints, or null if the polyline could not be decoded
    **/
    public static java.util.List<GeoPointProvider> Decode(String polyline)
    {
        return GooglePolyline.Decode(polyline, null);
    }

    /**
    *** Decodes the specified Polyline
    *** @param polyline  The polyline to decode
    *** @param gpList    The list into which the decoded polyline points will be placed
    *** @return A list of GeoPoints, or null if the polyline could not be decoded
    **/
    public static java.util.List<GeoPointProvider> Decode(String polyline, java.util.List<GeoPointProvider> gpList)
    {
        // -- References
        // -  https://developers.google.com/maps/documentation/utilities/polylinealgorithm
        // -  http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java

        /* initial polyline validation */
        if (StringTools.isBlank(polyline)) {
            return null;
        }

        /* init GeoPoint list */
        if (gpList == null) {
            gpList = new Vector<GeoPointProvider>();
        }

        /* last lat/lon */
        GeoPoint lastGP = null;
        long lastLat = 0L, lastLon = 0L;
        if (gpList.size() > 0) {
            GeoPointProvider gpp = gpList.get(gpList.size() - 1);
            if (gpp instanceof GeoRoute) {
                lastGP = ((GeoRoute)gpp).getLastGeoPoint();
            } else {
                lastGP = gpp.getGeoPoint();
            }
            lastLat = Math.round(lastGP.getLatitude()  * 100000.0);
            lastLon = Math.round(lastGP.getLongitude() * 100000.0);
        }

        /* parse polyline */
        long latAccum = 0L, lonAccum = 0L;
        try {
            int c = 0;
            int polyLen = polyline.length();
            while (c < polyLen) {
                // -- latitude
                {
                    int  shift = 0;
                    long accum = 0L;
                    for (;;) {
                        long X = ((long)polyline.charAt(c++) & 0xFFL) - 63L;
                        accum |= (X & 0x1FL) << shift;
                        shift += 5;
                        if (X < 0x20L) { break; }
                    }
                    latAccum += ((accum & 0x01L) != 0L)? ~(accum >> 1) : (accum >> 1);
                }
                // -- longitude
                {
                    int  shift = 0;
                    long accum = 0L;
                    for (;;) {
                        long X = ((long)polyline.charAt(c++) & 0xFFL) - 63L;
                        accum |= (X & 0x1FL) << shift;
                        shift += 5;
                        if (X < 0x20L) { break; }
                    }
                    lonAccum += ((accum & 0x01L) != 0L)? ~(accum >> 1) : (accum >> 1);
                }
                // -- same as last latitude/longitude?
                if ((lastLat == latAccum) && (lastLon == lonAccum)) {
                    // -- skip this latitude/longitude
                    continue;
                }
                lastLat = latAccum;
                lastLon = lonAccum;
                // -- save lat/lon
                double latitude  = (double)latAccum / 100000.0;
                double longitude = (double)lonAccum / 100000.0;
                if (GeoPoint.isValid(latitude,longitude)) {
                    GeoPoint gp = new GeoPoint(latitude,longitude);
                    if ((lastGP != null) && (MINIMUM_DISTANCE_METERS > 0.0) && 
                        (lastGP.metersToPoint(gp) < MINIMUM_DISTANCE_METERS)) {
                        // -- ignore this point
                    } else {
                        // -- save gp
                        gpList.add((GeoPointProvider)gp);
                        lastGP = gp;
                    }
                } else {
                    // -- encountered an invalid latitude/longitude
                    Print.logError("Invalid Latitude/Longitude: " + latitude + "/" + longitude);
                    // -- we are likely out-of-sync due to missing or invalid characters.
                    // -  more errors would likely follow, so we will bail now.
                    break;
                }
            } // while (c < polyline.length())
        } catch (StringIndexOutOfBoundsException sie) {
            // -- polyline format was invalid
            Print.logError("Polyline format error: " + sie);
            return null;
        } catch (Throwable th) {
            // -- catch any other parsing errors
            Print.logError("Polyline format error: " + th);
            return null;
        }

        /* return decoded polyline */
        return gpList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Encode coordinate
    **/
    private static long _encodeCoord(double C, long lastL, StringBuffer sb)
    {
        long L  = Math.round(C * 100000.0);
        long dL = L - lastL;
        long aL = dL << 1;
        if (dL < 0L) { aL = ~aL; }
        while (aL >= 0x20L) {
            sb.append((char)(((aL & 0x1FL) | 0x20L) + 63L));
            aL >>>= 5;
        }
        sb.append((char)(aL + 63L));
        return L;
    }

    /**
    *** Encodes the specified GeoPoints into a Polyline String
    *** @param gpList  The list of GeoPoints
    *** @return A Polyline String
    **/
    public static String Encode(java.util.List<GeoPointProvider> gpList)
    {
        if (!ListTools.isEmpty(gpList)) {
            StringBuffer sb = new StringBuffer();
            long lastLat = 0L;
            long lastLon = 0L;
            for (GeoPointProvider gpp : gpList) {
                GeoPoint gp = gpp.getGeoPoint();
                if (GeoPoint.isValid(gp)) {
                    lastLat = _encodeCoord(gp.getLatitude() , lastLat, sb);
                    lastLon = _encodeCoord(gp.getLongitude(), lastLon, sb);
                }
            }
            return sb.toString();
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

    }

}
