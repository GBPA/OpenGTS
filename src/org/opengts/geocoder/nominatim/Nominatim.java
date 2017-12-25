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
// Notes:
//  - http://wiki.openstreetmap.org/wiki/Nominatim
//  - http://open.mapquestapi.com/nominatim/
//  - http://nominatim.openstreetmap.org
//  - OpenStreetMap "Nominative Usage Policy" can be found at the following link: 
//      http://wiki.openstreetmap.org/wiki/Nominatim_usage_policy
//  - http://koo.fi/blog/2015/03/19/openstreetmap-nominatim-server-for-geocoding/#more-1094
// ----------------------------------------------------------------------------
// Example
//  - http://nominatim.openstreetmap.org/reverse?format=xml&lat=46.17330&lon=21.29370&zoom=18&addressdetails=1
//  - http://open.mapquestapi.com/nominatim/v1/reverse?format=xml&lat=46.17330&lon=21.29370&zoom=18&addressdetails=1
//   <?xml version="1.0" encoding="UTF-8" ?>
//   <reversegeocode timestamp='Sat, 08 Jan 11 01:43:35 -0500' 
//      attribution='Data Copyright OpenStreetMap Contributors, Some Rights Reserved. CC-BY-SA 2.0.' 
//      querystring='format=xml&amp;lat=46.17330&amp;lon=21.29370&amp;zoom=18&amp;addressdetails=1'>
//      <result place_id="25016501" osm_type="way" osm_id="17508617">P?durii, Arad, 310365, Romania</result>
//      <addressparts>
//          <tram>P?durii</tram>
//          <road>P?durii</road>
//          <residential>Arad</residential>
//          <city>Arad</city>
//          <postcode>310365</postcode>
//          <country>Romania</country>
//          <country_code>ro</country_code>
//      </addressparts>
//   </reversegeocode>
// ----------------------------------------------------------------------------
// Change History:
//  2011/01/28  Martin D. Flynn
//     -Initial release
//  2013/05/21  Martin D. Flynn
//     -Added language support (see "accept-language=")
//  2014/09/16  Martin D. Flynn
//     -Added property "useResultAddress" to use "result" tag value as full address
//  2015/09/16  Martin D. Flynn
//     -Added support for new "key=" AppKey requirement. [2.6.0-B83]
//     -Added support for ReverseGeocodeCache (EXPERIMENTAL) [2.6.0-B83]
//     -Added lazy starting of auto-trim thread [2.6.1-B35]
// ----------------------------------------------------------------------------
package org.opengts.geocoder.nominatim;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.opengts.util.*;
import org.opengts.geocoder.*;

public class Nominatim
    extends ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider
{
   
    // ------------------------------------------------------------------------
    // TAGs
    
    protected static final String TAG_reversegeocode            = "reversegeocode";     // main tag
    protected static final String TAG_result                    = "result";             // full address
    protected static final String TAG_addressparts              = "addressparts";       // address components
    protected static final String TAG_bar                       = "bar";                // not sure what this is
    protected static final String TAG_fast_food                 = "fast_food";          // not sure what this is
    protected static final String TAG_house                     = "house";              // 
    protected static final String TAG_tram                      = "tram";               // same as road?
    protected static final String TAG_road                      = "road";               // 
    protected static final String TAG_residential               = "residential";        // same as city?
    protected static final String TAG_village                   = "village";            // 
    protected static final String TAG_town                      = "town";               // alternate for city?
    protected static final String TAG_city                      = "city";               // 
    protected static final String TAG_county                    = "county";             // 
    protected static final String TAG_postcode                  = "postcode";           // 
    protected static final String TAG_hamlet                    = "hamlet";             // Wherefor art thou?
    protected static final String TAG_suburb                    = "suburb";             // 
    protected static final String TAG_state                     = "state";              // 
    protected static final String TAG_state_district            = "state_district";     // 
    protected static final String TAG_country                   = "country";            // Country name
    protected static final String TAG_country_code              = "country_code";       // Country code
    
    protected static final String ATTR_osm_type                 = "osm_type";

    // ------------------------------------------------------------------------

    protected static final String PROP_reverseURL               = "reverseURL";      // String: "http://localhost:8081/reverse?"
    protected static final String PROP_hostName                 = "host";            // String: "localhost:8081"
    protected static final String PROP_zoom                     = "zoom";            // String: "18"
    protected static final String PROP_addressdetails           = "addressdetails";  // String: "1"
    protected static final String PROP_email                    = "email";           // String: "joe@example.com"
    protected static final String PROP_useResultAddress         = "useResultAddress";// Boolean: use "result" tag as full address
    protected static final String PROP_mapquestAppKey           = "mapquestAppKey";  // String: "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    protected static final String PROP_ignoreIfMoving           = "ignoreIfMoving";  // Boolean: Reverse-Geocode iff stopped

    protected static final String PROP_cacheMaximumSize         = "cacheMaximumSize";
    protected static final String PROP_cacheMaxEntryAgeSec      = "cacheMaxEntryAgeSec";
    protected static final String PROP_cacheMaxEntryAgeMS       = "cacheMaxEntryAgeMS";
    protected static final String PROP_cacheTrimIntervalSec     = "cacheTrimIntervalSec";
    protected static final String PROP_cacheTrimIntervalMS      = "cacheTrimIntervalMS";

    protected static       String HOST_OPENSTREETMAP            = "nominatim.openstreetmap.org";
    protected static       String HOST_MAPQUEST                 = "open.mapquestapi.com";
    protected static       String HOST_PRIMARY                  = HOST_MAPQUEST;

    // ------------------------------------------------------------------------

    /* ReverseGeocodeCache */
    public    static       int    CACHE_MAXIMUM_SIZE            = 0;            // "0" means disabled
    public    static       long   CACHE_MAXIMUM_AGE_MS          = 20L * 60000L; // 20 minutes?
    public    static       long   AUTO_TRIM_INTERVAL_MS         = 10L * 60000L; // 10 minutes?

    // ------------------------------------------------------------------------

    protected static final String ENCODING_UTF8                 = StringTools.CharEncoding_UTF_8;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean                 ignoreIfMoving  = false;

    private ReverseGeocodeCache     rgCache         = null;

    /**
    *** Constructor
    *** @param name    The name assigned to this ReverseGeocodeProvider
    *** @param key     The optional authorization key
    *** @param rtProps The properties associated with this ReverseGeocodeProvider
    **/
    public Nominatim(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);

        /* load runtime properties */
        if (rtProps != null) {
            // -- do not reverse-geocode if moving
            this.ignoreIfMoving = rtProps.getBoolean(PROP_ignoreIfMoving,false);
            // -- ReverseGeocodeCache (may not be supported in this release)
            CACHE_MAXIMUM_SIZE = rtProps.getInt(PROP_cacheMaximumSize, CACHE_MAXIMUM_SIZE);
            long maxEntryAgeMS = rtProps.getLong(PROP_cacheMaxEntryAgeSec,0L) * 1000L;
            CACHE_MAXIMUM_AGE_MS = (maxEntryAgeMS > 0L)? maxEntryAgeMS :
                rtProps.getLong(PROP_cacheMaxEntryAgeMS,CACHE_MAXIMUM_AGE_MS);
            // -- auto-trim interval
            long autoTrimIntervMS = rtProps.getLong(PROP_cacheTrimIntervalSec,0L) * 1000L;
            AUTO_TRIM_INTERVAL_MS = (autoTrimIntervMS > 0L)? autoTrimIntervMS :
                rtProps.getLong(PROP_cacheTrimIntervalMS,AUTO_TRIM_INTERVAL_MS);
        }

        /* start ReverseGeocodeCache */
        // -- may not be supported in this release
        if (CACHE_MAXIMUM_SIZE > 0L) {
            this.rgCache = new ReverseGeocodeCache(this.getName(),
                CACHE_MAXIMUM_SIZE, CACHE_MAXIMUM_AGE_MS, AUTO_TRIM_INTERVAL_MS);
        }

    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets the authorization key of this ReverseGeocodeProvider
    *** @return The access key of this reverse-geocode provider
    **/
    @Override
    public String getAuthorization()
    {
        String appKey = this.getProperties().getString(PROP_mapquestAppKey,null);
        if (!StringTools.isBlank(appKey)) {
            return appKey;
        } else {
            return super.getAuthorization();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if locally resolved, false otherwise.
    *** (ie. remote address resolution takes more than 20ms to complete)
    *** @return true if locally resolved, false otherwise.
    **/
    @Override
    public boolean isFastOperation() 
    {
        // -- this is a slow operation
        return super.isFastOperation();
    }

    /**
    *** Returns a ReverseGeocode instance for the specified GeoPoint
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    @Override
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr, boolean cache) 
    {
        RTProperties rtp = this.getProperties();

        /* check "cache" state */
        // -- NOTE: This method depends on "cache" being true when stopped, and false when 
        // -  moving (ie. speed > 0).  See EventData.updateAddress(...) for more info.
        boolean isStopped = cache;      // true if stopped, false if moving
        boolean isMoving  = !isStopped; // moving is opposite of stopped

        /* get ReverseGeocode */
        ReverseGeocode rg = null;
        boolean isCached = false;
        long startMS = System.currentTimeMillis();
        for (;;) {

            /* check ReverseGeocodeCache */
            if (this.rgCache != null) {
                // -- ReverseGeocodeCache check here
                rg = this.rgCache.getReverseGeocode(gp); // "localeStr" is ignored
                if (rg != null) {
                    isCached = true;
                    break;
                }
            }

            /* ReverseGeocoding iff moving? */
            if (this.ignoreIfMoving && isMoving) {
                // -- do not reverse-geocode if moving
                break;
            }

            /* get ReverseGeocode from provider */
            rg = this.getAddressReverseGeocode(gp, localeStr, cache);
            if (rg != null) {
                isCached = false;
                if (this.rgCache != null) {
                    this.rgCache.addReverseGeocode(gp, rg);
                }
                break;
            }

            /* single-pass loop break */
            // -- no Reverse-Geocode at this point
            break;

        }
        long endMS = System.currentTimeMillis();

        /* report */
        if (rg != null) {
            Print.logDebug("Time to obtain ReverseGeocode: " + (endMS - startMS) + " ms" + (isCached?" [cached]":""));
        }

        /* return result */
        return rg; // may be null

    }

    /* return subdivision */
    public String getSubdivision(GeoPoint gp) 
    {
        throw new UnsupportedOperationException("Not supported");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a ReverseGeocode instance containing address information
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    private ReverseGeocode getAddressReverseGeocode(GeoPoint gp, String localeStr, boolean cache) 
    {
        RTProperties rtp = this.getProperties();

        /* URL */
        String url = this.getAddressReverseGeocodeURL(gp, localeStr);
        Print.logInfo("Address URL: " + url);

        /* create XML document */
        Document xmlDoc = GetXMLDocument(url);
        if (xmlDoc == null) {
            return null;
        }

        /* create ReverseGeocode response */
        Element reversegeocode = xmlDoc.getDocumentElement();
        if (!reversegeocode.getTagName().equalsIgnoreCase(TAG_reversegeocode)) {
            return null;
        }

        /* init */
        String address_val      = null;     // full address
        String bar_val          = null;     // ???
        String house_val        = null;     // house number
        String road_val         = null;     // street name
        String city_val         = null;     // city name
        String county_val       = null;     // county name
        String suburb_val       = null;     // suburb name
        String state_val        = null;     // state/province
        String postcode_val     = null;     // postal code
        String hamlet_val       = null;     // 
        String country_name_val = null;     // country name
        String country_code_val = null;     // country code

        // -- full address
        NodeList resultList = XMLTools.getChildElements(reversegeocode,TAG_result);
        for (int r = 0; r < resultList.getLength(); r++) {
            Element result = (Element)resultList.item(r);
            //String osmType = XMLTools.getAttribute(result, ATTR_osm_type, null, false);
            address_val = XMLTools.getNodeText(result," ",false);
            break; // only the first element
        }

        // -- address components
        NodeList addresspartsList = XMLTools.getChildElements(reversegeocode,TAG_addressparts);
        for (int a = 0; (a < addresspartsList.getLength()); a++) {
            Element addressparts = (Element)addresspartsList.item(a);
            NodeList addresspartsChildren = addressparts.getChildNodes();
            for (int ac = 0; ac < addresspartsChildren.getLength(); ac++) {
                Node child = addresspartsChildren.item(ac);
                if (!(child instanceof Element)) { continue; }
                Element elem = (Element)child;
                String elemName = elem.getNodeName();
                //Print.logInfo("Element Name: " + elemName);
                if (elemName.equalsIgnoreCase(TAG_bar)) {
                    bar_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_house)) {
                    house_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_tram)) {
                    // -- ignore
                } else
                if (elemName.equalsIgnoreCase(TAG_road)) {
                    road_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_residential)) {
                    // -- ignore
                } else
                if (elemName.equalsIgnoreCase(TAG_village)) {
                    // -- ignore
                } else
                if (elemName.equalsIgnoreCase(TAG_town)) {
                    if (StringTools.isBlank(city_val)) {
                        city_val = XMLTools.getNodeText(elem," ",false);
                    }
                } else
                if (elemName.equalsIgnoreCase(TAG_city)) {
                    city_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_county)) {
                    county_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_postcode)) {
                    postcode_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_hamlet)) {
                    hamlet_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_suburb)) {
                    suburb_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_state)) {
                    state_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_country)) {
                    country_name_val = XMLTools.getNodeText(elem," ",false);
                } else
                if (elemName.equalsIgnoreCase(TAG_country_code)) {
                    country_code_val = StringTools.trim(XMLTools.getNodeText(elem," ",false)).toUpperCase();
                } else {
                    // elemName unrecognized
                }
            }
            break; // only the first element
        }

        /* populate ReverseGeocode instance */
        ReverseGeocode rg = new ReverseGeocode();
        StringBuffer addr = new StringBuffer();
        // -- house number /road
        if (!StringTools.isBlank(house_val)) {
            addr.append(house_val);
            if (!StringTools.isBlank(road_val)) {
                addr.append(" ");
                addr.append(road_val);
                rg.setStreetAddress(house_val + " " + road_val);
            } else {
                rg.setStreetAddress(house_val);
            }
        } else
        if (!StringTools.isBlank(road_val)) {
            addr.append(road_val);
            rg.setStreetAddress(road_val);
        }
        // -- suburb
        if (!StringTools.isBlank(suburb_val)) {
            if (addr.length() > 0) { addr.append(", "); }
            addr.append(suburb_val);
            //rg.setSuburb(suburb_val);
        }
        // -- city/county
        if (!StringTools.isBlank(city_val)) {
            if (addr.length() > 0) { addr.append(", "); }
            addr.append(city_val);
            rg.setCity(city_val);
        }
        if (!StringTools.isBlank(county_val)) {
            if (StringTools.isBlank(city_val)) {
                // "city" not provided, at least include the "county"
                if (addr.length() > 0) { addr.append(", "); }
                addr.append("[").append(county_val).append("]");
            }
            //rg.setCounty(county_val);
        }
        // -- state/province/postcode
        if (!StringTools.isBlank(state_val)) {
            if (addr.length() > 0) { addr.append(", "); }
            addr.append(state_val);
            rg.setStateProvince(state_val);
            if (!StringTools.isBlank(postcode_val)) {
                addr.append(" ").append(postcode_val);
                rg.setPostalCode(postcode_val);
            }
        } else {
            if (!StringTools.isBlank(postcode_val)) {
                if (addr.length() > 0) { addr.append(", "); }
                addr.append(postcode_val);
                rg.setPostalCode(postcode_val);
            }
        }
        // -- country
        if (!StringTools.isBlank(country_code_val)) {
            if (country_code_val.equalsIgnoreCase("US")) {
                //if (addr.length() > 0) { addr.append(", "); }
                //addr.append("USA");
            } else
            if (!StringTools.isBlank(country_name_val)) {
                if (addr.length() > 0) { addr.append(", "); }
                addr.append(country_name_val);
            } else {
                if (addr.length() > 0) { addr.append(", "); }
                addr.append(country_code_val);
            }
            rg.setCountryCode(country_code_val);
        }
        // -- full address
        if (!StringTools.isBlank(address_val) && rtp.getBoolean(PROP_useResultAddress,false)) {
            rg.setFullAddress(address_val);
        } else {
            rg.setFullAddress(addr.toString());
        }

        return rg;
    
    }

    private String getEmail()
    {
        return this.getProperties().getString(PROP_email,null);
    }

    private String getAddressReverseGeocodeURL(GeoPoint gp, String localeStr) 
    {
        //  - http://nominatim.openstreetmap.org/reverse?format=xml&addressdetails=1&zoom=18&lat=46.17330&lon=21.29370
        StringBuffer sb = new StringBuffer();
        RTProperties rtp = this.getProperties();
        String url = rtp.getString(PROP_reverseURL, null);
        boolean urlHasKey = false;
        if (!StringTools.isBlank(url)) {
            sb.append(url);
            urlHasKey = (url.indexOf("key=") >= 0)? true : false;
        } else {
            String host = rtp.getString(PROP_hostName, HOST_PRIMARY);
            sb.append("http://");
            sb.append(host);
            if (host.indexOf("mapquest") >= 0) {
                sb.append("/nominatim/v1/reverse?");
            } else {
                sb.append("/reverse?");
            }
        }
        // --- key
        if (!urlHasKey && this.hasAuthorization()) {
            String key = this.getAuthorization();
            sb.append("key="+key+"&");
        }
        // --- format xml
        sb.append("format=xml&");
        // --- limit to 1 match only
        sb.append("limit=1&");
        // --- language
        if (!StringTools.isBlank(localeStr)) {
            sb.append("accept-language=").append(localeStr).append("&");
        }
        //sb.append("osm_type=W&");
        sb.append("addressdetails=").append(rtp.getString(PROP_addressdetails,"1")).append("&"); // 0|1
        sb.append("zoom=").append(rtp.getString(PROP_zoom,"18")).append("&"); // 0..18
        sb.append("email=").append(this.getEmail()).append("&"); // required, per usage policy
        sb.append("lat=").append(gp.getLatitudeString( GeoPoint.SFORMAT_DEC_5,null)).append("&");
        sb.append("lon=").append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));
        return sb.toString();
    }

    private Document GetXMLDocument(String url) 
    {
         try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream input = HTMLTools.inputStream_GET(url, null, 5000);
            InputStreamReader reader = new InputStreamReader(input, ENCODING_UTF8);
            InputSource inSrc = new InputSource(reader);
            inSrc.setEncoding(ENCODING_UTF8);
            return db.parse(inSrc);
        } catch (ParserConfigurationException pce) {
            Print.logError("Parse error: " + pce);
            return null;
        } catch (SAXException se) {
            Print.logError("Parse error: " + se);
            return null;
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(ENCODING_UTF8);

        /* host */
        String host = RTConfig.getString("host",null);
        if (!StringTools.isBlank(host)) {
            HOST_PRIMARY = host;
        }

        /* GeoPoint */
        GeoPoint gp = new GeoPoint(RTConfig.getString("gp",null));
        if (!gp.isValid()) {
            Print.logInfo("Invalid GeoPoint specified");
            System.exit(1);
        }
        Print.logInfo("Reverse-Geocoding GeoPoint: " + gp);

        /* Reverse Geocoding */
        Nominatim gn = new Nominatim("nominatim", null, RTConfig.getCommandLineProperties());
        Print.sysPrintln("RevGeocode = " + gn.getReverseGeocode(gp,null/*localeStr*/,false/*cache*/));

    }

}
