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
//  2016/01/04  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.geocoder.opencage;

import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.geocoder.*;

public class OpenCageData
    extends ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider, GeocodeProvider
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // -- disable SNI 
    // -  (required due to possible misconfiguration in opencagedata server SSL certificate)
    public static void disableSNIExtension() {
        System.setProperty("jsse.enableSNIExtension", "false");
    }
    static {
        //OpenCageData.disableSNIExtension();
    };

    // ------------------------------------------------------------------------
    // References:
    //   - http://geocoder.opencagedata.com/
    //
    // Nearest Address: 
	//   - https://api.opencagedata.com/geocode/v1/json?q=-22.6792,+14.5272&pretty=1&key=AUTH_KEY
    // {
    //    "licenses" : [
    //       {
    //          "name" : "CC-BY-SA",
    //          "url" : "http://creativecommons.org/licenses/by-sa/3.0/"
    //       },
    //       {
    //          "name" : "ODbL",
    //          "url" : "http://opendatacommons.org/licenses/odbl/summary/"
    //       }
    //    ],
    //    "rate" : {
    //       "limit" : 2500,
    //       "remaining" : 2494,
    //       "reset" : 1434844800
    //    },
    //    "results" : [
    //       {
    //          "annotations" : {
    //             "DMS" : {
    //                "lat" : "22\u00b0 40' 46.34184'' S",
    //                "lng" : "14\u00b0 31' 39.36216'' E"
    //             },
    //             "MGRS" : "33KVQ5147391877",
    //             "Maidenhead" : "JG77gh36hv",
    //             "Mercator" : {
    //                "x" : 1617205.101,
    //                "y" : -2576841.391
    //             },
    //             "OSM" : {
    //                "url" : "http://www.openstreetmap.org/?mlat=-22.67954&mlon=14.52760#map=17/-22.67954/14.52760"
    //             },
    //             "callingcode" : 264,
    //             "geohash" : "k7fqfx6djekju86um1br",
    //             "sun" : {
    //                "rise" : {
    //                   "astronomical" : 1434774000,
    //                   "civil" : 1434777300,
    //                   "nautical" : 1434775620
    //                },
    //                "set" : {
    //                   "astronomical" : 1434822420,
    //                   "civil" : 1434819120,
    //                   "nautical" : 1434820800
    //                }
    //             },
    //             "timezone" : {
    //                "name" : "Africa/Windhoek",
    //                "now_in_dst" : 0,
    //                "offset_sec" : 3600,
    //                "offset_string" : 100,
    //                "short_name" : "WAT"
    //             },
    //             "what3words" : {
    //                "words" : "matriarchs.nano.rotates"
    //             }
    //          },
    //          "components" : {
    //             "city" : "Swakopmund",
    //             "clothes" : "Jet",
    //             "country" : "Namibia",
    //             "country_code" : "na",
    //             "road" : "Nathaniel Maxuilili St (Breite St)",
    //             "state" : "Erongo Region",
    //             "suburb" : "Central"
    //          },
    //          "confidence" : 0,
    //          "formatted" : "Jet, Nathaniel Maxuilili St (Breite St), Swakopmund, Namibia",
    //          "geometry" : {
    //             "lat" : -22.6795394,
    //             "lng" : 14.5276006
    //          }
    //       }
    //    ],
    //    "status" : {
    //       "code" : 200,
    //       "message" : "OK"
    //    },
    //    "thanks" : "For using an OpenCage Data API",
    //    "timestamp" : {
    //       "created_http" : "Sat, 20 Jun 2015 21:54:45 GMT",
    //       "created_unix" : 1434837285
    //    },
    //    "total_results" : 1
    // }
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- only a subset of the tags included here

    protected static final String TAG_total_results             = "total_results";

    protected static final String TAG_status                    = "status";
    protected static final String   TAG_code                    = "code";
    protected static final String   TAG_message                 = "message";

    protected static final String TAG_results                   = "results";    // array
    protected static final String   TAG_confidence              = "confidence";
    protected static final String   TAG_formatted               = "formatted";
    protected static final String   TAG_components              = "components";
    protected static final String     TAG_clothes               = "clothes";
    protected static final String     TAG_city                  = "city";
    protected static final String     TAG_country               = "country";
    protected static final String     TAG_country_code          = "country_code";
    protected static final String     TAG_road                  = "road";
    protected static final String     TAG_hamlet                = "hamlet";
    protected static final String     TAG_county                = "county";
    protected static final String     TAG_state                 = "state";
    protected static final String     TAG_postcode              = "postcode";
    protected static final String     TAG_suburb                = "suburb";

    /* URLs */
    protected static final String URL_ReverseGeocode_           = "https://api.opencagedata.com/geocode/v1/json?limit=1&pretty=1&no_annotations=1&";
    protected static final String URL_Geocode_                  = "";
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static final String  PROP_reverseGeocodeURL       = "reverseGeocodeURL";
    protected static final String  PROP_geocodeURL              = "geocodeURL";

    // ------------------------------------------------------------------------

    protected static final int     TIMEOUT_ReverseGeocode       = 2500; // milliseconds
    protected static final int     TIMEOUT_Geocode              = 5000; // milliseconds

    protected static final String  DEFAULT_COUNTRY              = "US"; // http://en.wikipedia.org/wiki/CcTLD

    // ------------------------------------------------------------------------

    protected static final String  STATUS_UNDEFINED             = "?";
    protected static final String  STATUS_OK                    = "200";
    protected static final String  STATUS_INVALID_REQUEST       = "400";
    protected static final String  STATUS_LIMIT_EXCEEDED        = "402";
    protected static final String  STATUS_INVALID_AUTH          = "403";
    protected static final String  STATUS_INVALID_API           = "404";
    protected static final String  STATUS_TIMEOUT               = "408";
    protected static final String  STATUS_REQUEST_TOO_LONG      = "410";
    protected static final String  STATUS_INTERNAL_ERROR        = "503";

    private static JSON JSONStatus(String status)
    {
        // -- {"status":{"code":"403"}}
        StringBuffer J = new StringBuffer();
        J.append("{\"");
          J.append(TAG_status).append("\":");
          J.append("{\"");
            J.append("\"").append(TAG_code).append("\":\"").append(status).append("\"");
          J.append("}");
        J.append("}");
        String j = J.toString();
        try {
            return new JSON(j);
        } catch (JSON.JSONParsingException jpe) {
            Print.logError("Invalid JSON: " + J);
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /* MUST BE FALSE IN PRODUCTION!!! */
    protected static final boolean FAILOVER_DEBUG               = false;

    // ------------------------------------------------------------------------

    protected static final String  ENCODING_UTF8                = StringTools.CharEncoding_UTF_8;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public OpenCageData(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
    }

    // ------------------------------------------------------------------------

    public boolean isFastOperation()
    {
        // -- this is a slow operation
        return super.isFastOperation();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Geocode timeout
    **/
    protected int getGeocodeTimeout()
    {
        return TIMEOUT_Geocode;
    }

    /**
    *** Returns the ReverseGeocode timeout
    **/
    protected int getReverseGeocodeTimeout()
    {
        return TIMEOUT_ReverseGeocode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return reverse-geocode */
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr, boolean cache)
    {
        ReverseGeocode rg = this.getAddressReverseGeocode(gp, localeStr, cache);
        return rg;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* nearest address URI */
    protected String getAddressReverseGeocodeURI()
    {
        return URL_ReverseGeocode_;
    }

    /* encode GeoPoint into nearest address URI */
    protected String getAddressReverseGeocodeURL(GeoPoint gp, String localeStr)
    {
        StringBuffer sb = new StringBuffer();

        /* predefined URL */
        String rgURL = this.getProperties().getString(PROP_reverseGeocodeURL,null);
        if (!StringTools.isBlank(rgURL)) {
            // -- assume "&key=" is already part of this URL
            sb.append(rgURL);
            sb.append("&q=");
            if (gp != null) {
                String lat = gp.getLatitudeString( GeoPoint.SFORMAT_DEC_5,null);
                String lon = gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null);
                sb.append(lat).append(",").append(lon);
            }
            String defURL = sb.toString();
            return defURL;
        }

        /* assemble URL */
        sb.append(this.getAddressReverseGeocodeURI());

        /* UserID */
        String userID = this.getAuthorization();
        if (StringTools.isBlank(userID) || userID.startsWith("*")) {
            // -- invalid key
        } else {
            sb.append("&key=").append(userID);
        }

        /* Query */
        sb.append("&q=");
        if (gp != null) {
            String lat = gp.getLatitudeString( GeoPoint.SFORMAT_DEC_5,null);
            String lon = gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null);
            sb.append(lat).append(",").append(lon);
        }

        /* return url */
        String defURL = sb.toString();
        return defURL;

    }

    /* return reverse-geocode using nearest address */
    public ReverseGeocode getAddressReverseGeocode(GeoPoint gp, String localeStr, boolean cache)
    {

        /* check for failover mode */
        if (this.isReverseGeocodeFailoverMode()) {
            ReverseGeocodeProvider frgp = this.getFailoverReverseGeocodeProvider();
            return frgp.getReverseGeocode(gp, localeStr, cache);
        }

        /* URL */
        String url = this.getAddressReverseGeocodeURL(gp, localeStr);
        Print.logDebug("OpenCageData RG URL: " + url);

        /* create JSON document */
        JSON jsonDoc = null;
        JSON._Object jsonObj = null;
        try {
            jsonDoc = GetJSONDocument(url, this.getReverseGeocodeTimeout());
            jsonObj = (jsonDoc != null)? jsonDoc.getObject() : null;
            if (jsonObj == null) {
                return null;
            }
        } catch (Throwable th) {
            Print.logException("Error", th);
        }

        /* get status */
        String status  = STATUS_UNDEFINED;
        String message = "";
        JSON._Object j_status = jsonObj.getObjectForName(TAG_status, null);
        if (j_status != null) {
            status  = j_status.getStringForName(TAG_code   , status);  // expect "200"
            message = j_status.getStringForName(TAG_message, message); // expect "OK"
        }

        /* parse address */
        int    confidence = 0;
        String address    = null;
        String street     = null;
        String city       = null; // country_code
        String hamlet     = null;
        String county     = null;
        String state      = null;
        String postal     = null;
        String country    = null;
        JSON._Array j_results = jsonObj.getArrayForName(TAG_results, null);
        JSON._Object j_results_0 = (j_results != null)? j_results.getObjectValueAt(0,null) : null;
        if (j_results_0 != null) {
            confidence = j_results_0.getIntForName(TAG_confidence, confidence);
            address    = j_results_0.getStringForName(TAG_formatted, address);
            JSON._Object j_components = j_results_0.getObjectForName(TAG_components, null);
            if (j_components != null) {
                street  = j_components.getStringForName(TAG_road, street);
                city    = j_components.getStringForName(TAG_city, city);
                hamlet  = j_components.getStringForName(TAG_hamlet, hamlet);
                county  = j_components.getStringForName(TAG_county, county);
                state   = j_components.getStringForName(TAG_state, state);
                postal  = j_components.getStringForName(TAG_postcode, postal);
                country = j_components.getStringForName(TAG_country_code, country);
            }
        } else {
            Print.logInfo("No address found: null");
        }

        /* create address */
        if (FAILOVER_DEBUG) {
            status = STATUS_LIMIT_EXCEEDED;
        } else 
        if (!StringTools.isBlank(address)) {
            // -- replace "United States of America" with "USA"
            String usaRep = DEFAULT_COUNTRY.equals("US")? "" : "USA";
            address = StringTools.replace(address,"United States of America",usaRep).trim();
            if (address.endsWith(",")) {
                address = address.substring(0,address.length()-1);
            }
            // -- address found 
            Print.logDebug("Address: " + address);
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress(address);
            rg.setStreetAddress(street);
            rg.setCity(city);
          //rg.setHamlet(hamlet);
          //rg.setCounty(county);
            rg.setStateProvince(state);
            rg.setPostalCode(postal);
            rg.setCountryCode(country);
            return rg;
        } else
        if ((status.equals(STATUS_OK) || status.equals(""))) {
            // -- address not found, but status indicates successful
            Print.logDebug("No Address found for location: " + gp);
            ReverseGeocode rg = new ReverseGeocode();
            rg.setFullAddress("");
            return rg;
        }

        /* check for failover */
        boolean failover = false;
        if (status.equals(STATUS_LIMIT_EXCEEDED)) {
            Print.logError("Limit Exceeded! ["+status+"]");
            failover = true;
        } else
        if (status.startsWith(STATUS_INVALID_REQUEST)) {
            Print.logError("Invalid request ["+status+"]");
            failover = true;
        } else
        if (status.startsWith(STATUS_INVALID_AUTH)) {
            Print.logError("Invalid authorization ["+status+"]");
            failover = true;
        } else
        if (status.startsWith(STATUS_INVALID_API)) {
            Print.logError("Invalid API ["+status+"]");
            failover = true;
        } else
        if (status.startsWith(STATUS_TIMEOUT)) {
            Print.logError("Request Timeout ["+status+"]");
            failover = true;
        } else
        if (status.startsWith(STATUS_REQUEST_TOO_LONG)) {
            Print.logError("Request Too Long ["+status+"]");
            failover = true;
        } else
        if (status.startsWith(STATUS_INTERNAL_ERROR)) {
            Print.logError("Internal service error ["+status+"]");
            failover = true;
        } else {
            Print.logError("OpenCageData Unrecognized Error! ["+status+"]");
            failover = true;
        }
        // -- failover?
        if (failover && this.hasFailoverReverseGeocodeProvider()) {
            this.startReverseGeocodeFailoverMode();
            ReverseGeocodeProvider frgp = this.getFailoverReverseGeocodeProvider();
            Print.logWarn("Failing over to '" + frgp.getName() + "'");
            return frgp.getReverseGeocode(gp, localeStr, cache);
        }

        /* no reverse-geocode available */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* nearest address URI */
    protected String getGeoPointGeocodeURI()
    {
        return URL_Geocode_;
    }

    /* encode GeoPoint into nearest address URI */
    protected String getGeoPointGeocodeURL(String address, String country)
    {
        StringBuffer sb = new StringBuffer();

        /* predefined URL */
        String gcURL = this.getProperties().getString(PROP_geocodeURL,null);
        if (!StringTools.isBlank(gcURL)) {
            // -- assume "&key=" is already part of this URL
            sb.append(gcURL);
            sb.append("&q=").append(URIArg.encodeArg(address));
            if (!StringTools.isBlank(country)) {
                // -- country code bias: http://en.wikipedia.org/wiki/CcTLD
                sb.append("&countrycode=").append(country);
            }
            String defURL = sb.toString();
            return defURL;
        }

        /* assemble URL */
        sb.append(this.getGeoPointGeocodeURI());

        /* key */
        String apiKey = this.getAuthorization();
        if (StringTools.isBlank(apiKey) || apiKey.startsWith("*")) {
            // -- invalid key
        } else {
            sb.append("&key=").append(apiKey);
        }

        /* address/country */
        sb.append("&q=").append(URIArg.encodeArg(address));
        if (!StringTools.isBlank(country)) {
            sb.append("&countrycode=").append(country);
        }

        /* return url */
        String defURL = sb.toString();
        return defURL;

    }

    /* return geocode */
    public GeoPoint getGeocode(String address, String country)
    {

        /* URL */
        String url = this.getGeoPointGeocodeURL(address, country);
        Print.logDebug("OpenCageData GC URL: " + url);

        /* create JSON document */
        JSON jsonDoc = GetJSONDocument(url, this.getReverseGeocodeTimeout());
        JSON._Object jsonObj = (jsonDoc != null)? jsonDoc.getObject() : null;
        if (jsonObj == null) {
            return null;
        }

        /* get status */
        String status  = STATUS_UNDEFINED;
        String message = "";
        JSON._Object j_status = jsonObj.getObjectForName(TAG_status, null);
        if (j_status != null) {
            status  = j_status.getStringForName(TAG_code   , status);  // expect "200"
            message = j_status.getStringForName(TAG_message, message); // expect "OK"
        }

        /* parse GeoPoint */
        GeoPoint geoPoint = null;
        JSON._Array j_results = jsonObj.getArrayForName(TAG_results, null);
        JSON._Object j_results_0 = (j_results != null)? j_results.getObjectValueAt(0,null) : null;
        if (j_results_0 != null) {
            // -- TODO
        } else {
            Print.logDebug("'results' is null/empty");
        }

        /* return GeoPoint */
        if (geoPoint != null) {
            // -- GeoPoint found 
            Print.logDebug("GeoPoint: " + geoPoint);
            return geoPoint;
        } else
        if ((status.equals(STATUS_OK) || status.equals(""))) {
            Print.logDebug("No GeoPoint returned for address: " + address);
            return null;
        }

        /* check for errors */
        if (status.equals(STATUS_LIMIT_EXCEEDED)) {
            Print.logError("Limit Exceeded! ["+status+"]");
        } else
        if (status.startsWith(STATUS_INVALID_REQUEST)) {
            Print.logError("Invalid request ["+status+"]");
        } else
        if (status.startsWith(STATUS_INVALID_AUTH)) {
            Print.logError("Invalid authorization ["+status+"]");
        } else
        if (status.startsWith(STATUS_INVALID_API)) {
            Print.logError("Invalid API ["+status+"]");
        } else
        if (status.startsWith(STATUS_TIMEOUT)) {
            Print.logError("Request Timeout ["+status+"]");
        } else
        if (status.startsWith(STATUS_REQUEST_TOO_LONG)) {
            Print.logError("Request Too Long ["+status+"]");
        } else
        if (status.startsWith(STATUS_INTERNAL_ERROR)) {
            Print.logError("Internal service error ["+status+"]");
        } else {
            Print.logError("OpenCageData Unrecognized Error! ["+status+"]");
        }

        /* no geocode available */
        return null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static JSON GetJSONDocument(String url, int timeoutMS)
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
            //Print.logException("HttpIOException ["+rc+"-"+rm+"]: " + hioe.getMessage(), hioe);
            if (rc == 400) {
                jsonDoc = JSONStatus(STATUS_INVALID_REQUEST);
            } else
            if (rc == 402) {
                jsonDoc = JSONStatus(STATUS_LIMIT_EXCEEDED);
            } else
            if (rc == 403) {
                jsonDoc = JSONStatus(STATUS_INVALID_AUTH);
            } else
            if (rc == 404) {
                jsonDoc = JSONStatus(STATUS_INVALID_API);
            } else
            if (rc == 408) {
                jsonDoc = JSONStatus(STATUS_TIMEOUT);
            } else
            if (rc == 410) {
                jsonDoc = JSONStatus(STATUS_REQUEST_TOO_LONG);
            } else
            if (rc == 503) {
                jsonDoc = JSONStatus(STATUS_INTERNAL_ERROR);
            }
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
    // ------------------------------------------------------------------------
    
    private static final String ARG_ACCOUNT[]       = new String[] { "account", "a"  };
    private static final String ARG_GEOCODE[]       = new String[] { "geocode", "gc" };
    private static final String ARG_REVGEOCODE[]    = new String[] { "revgeo" , "rg" };
    
    private static String FilterID(String id)
    {
        if (id == null) {
            return null;
        } else {
            StringBuffer newID = new StringBuffer();
            int st = 0;
            for (int i = 0; i < id.length(); i++) {
                char ch = Character.toLowerCase(id.charAt(i));
                if (Character.isLetterOrDigit(ch)) {
                    newID.append(ch);
                    st = 1;
                } else
                if (st == 1) {
                    newID.append("_");
                    st = 0;
                } else {
                    // ignore char
                }
            }
            while ((newID.length() > 0) && (newID.charAt(newID.length() - 1) == '_')) {
                newID.setLength(newID.length() - 1);
            }
            return newID.toString();
        }
    }

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(ENCODING_UTF8);
        String accountID = RTConfig.getString(ARG_ACCOUNT,"demo");
        OpenCageData gn = new OpenCageData("opencage", null, null);

        /* reverse geocode */
        if (RTConfig.hasProperty(ARG_REVGEOCODE)) {
            GeoPoint gp = new GeoPoint(RTConfig.getString(ARG_REVGEOCODE,null));
            if (!gp.isValid()) {
                Print.logInfo("Invalid GeoPoint specified");
                System.exit(1);
            }
            Print.logInfo("Reverse-Geocoding GeoPoint: " + gp);
            Print.sysPrintln("RevGeocode = " + gn.getReverseGeocode(gp,null/*localeStr*/,false/*cache*/));
            // -- Note: Even though the values are printed in UTF-8 character encoding, the
            // -  characters may not appear to be properly displayed if the console display
            // -  does not support UTF-8.
            System.exit(0);
        }

        /* no options */
        Print.sysPrintln("No options specified");
        System.exit(1);

    }

}
