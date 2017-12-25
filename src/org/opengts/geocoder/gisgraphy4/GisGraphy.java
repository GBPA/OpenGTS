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
//  - http://www.gisgraphy.com/
// ----------------------------------------------------------------------------
// Examples:
//  - http://free.gisgraphy.com/reversegeocoding/reversegeocode?lat=46.17330&lng=21.29370&format=XML&from=1&to=1&indent=true
//      <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
//      <results>
//        <numFound>1</numFound>
//        <QTime>15</QTime>
//        <result>
//          <city>Arad</city>
//          <countryCode>RO</countryCode>
//          <distance>177.79999899247082</distance>
//          <formatedFull>Strada Padurii, Arad, Arad, Romania, RO</formatedFull>
//          <geocodingLevel>STREET</geocodingLevel>
//          <lat>46.17177105333476</lat>
//          <lng>21.293030651959974</lng>
//          <state>Arad</state>
//          <streetName>Strada Padurii</streetName>
//        </result>
//      </results>
// ----------------------------------------------------------------------------
// Change History:
//  2010/01/12  mihai, SysOP Consulting SRL
//     -Initial release
//  2010/04/25  Martin D. Flynn
//     -Misc changes
//  2011/06/16  Martin D. Flynn
//     -Changed "isFastOperation()" to look for "alwaysFast" property settings.
//     -Fixed CountryCode always overwriting State/Province
//  2012/04/03  Martin D. Flynn
//     -Made the street address lookup optional
//  2016/01/04  Martin D. Flynn
//     -Cloned from "GisGraphy.java" to support GISGraphy V4 
//  2017/03/19  Martin D. Flynn
//     -Fixed "null" appearing in constructed street address.
// ----------------------------------------------------------------------------
package org.opengts.geocoder.gisgraphy4;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.opengts.util.*;
import org.opengts.geocoder.*;
import org.opengts.geocoder.country.*;

public class GisGraphy // v4
    extends ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider, SubdivisionProvider
{

  //private   static final String DEBUG_STREET_URL = null;

    // ------------------------------------------------------------------------

    // -- XML tags
    protected static final String TAG_error                     = "error";          // error indication
    protected static final String TAG_results                   = "results";        // begin of results
    protected static final String TAG_numFound                  = "numFound";       // number of entries found
    protected static final String TAG_QTime                     = "QTime";          // The execution time of the query in ms
    protected static final String TAG_result                    = "result";         // one result

    protected static final String TAG_distance                  = "distance";       // Distance to address (meters)
    protected static final String TAG_geocodingLevel            = "geocodingLevel"; // Geocoding level [NONE|STREET]
    protected static final String TAG_lat                       = "lat";            // Latitude
    protected static final String TAG_lng                       = "lng";            // Longitude

    protected static final String TAG_formatedFull              = "formatedFull";   // fully formatted address
    protected static final String TAG_streetName                = "streetName";     // Street
    protected static final String TAG_houseNumber               = "houseNumber";    // House number
    protected static final String TAG_city                      = "city";           // City
    protected static final String TAG_citySubdivision           = "citySubdivision";// City Subdivision (Estate)
    protected static final String TAG_state                     = "state";          // County? (does not return the "State")
    protected static final String TAG_countryCode               = "countryCode";    // Country abbrev (eg. "US")
    protected static final String TAG_zipCode                   = "zipCode";        // Zip Code
    protected static final String TAG_name                      = "name";           // Name of address?

    // ------------------------------------------------------------------------
    // http://free.gisgraphy.com/reversegeocoding/reversegeocode?format=XML&from=1&to=1&lat=46.17330&lng=21.29370

    protected static final String PROP_gisgraphyApikey          = "gisgraphyApikey";  // String : "123456abcdef"
    protected static final String PROP_reversegeocodeURL        = "reversegeocodeURL";// String : "http://localhost:8081/reversegeocoding/reversegeocode?"
    protected static final String PROP_useSSL                   = "useSSL";           // boolean: true
    protected static final String PROP_host                     = "host";             // String : "localhost:8081"
    protected static final String PROP_failoverHost             = "failoverHost";     // String : ""
    
    protected static       String HOST_PRIMARY                  = "localhost";
    protected static       String HOST_FAILOVER                 = "";
    
    protected static       int    SERVICE_TIMEOUT_MS            = 5000;
    
    protected static       String GISGRAPHY_URL                 = "http://free.gisgraphy.com/reversegeocoding/reversegeocode?"; // DO NOT USE FOR PRODUCTION
    protected static       String REVERSEGEOCODE_URL            = null;

    // ------------------------------------------------------------------------

    // address has to be within this distance to qualify (cannot be greater than 5.0 kilometers)
    protected static final double MAX_ADDRESS_DISTANCE_KM       = 10.0;

    // ------------------------------------------------------------------------

    protected static final String ENCODING_UTF8                 = StringTools.CharEncoding_UTF_8;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    *** @param name    The name assigned to this ReverseGeocodeProvider
    *** @param key     The optional authorization key
    *** @param rtProps The properties associated with this ReverseGeocodeProvider
    **/
    public GisGraphy(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if locally resolved, false otherwise.
    *** (ie. remote address resolution takes more than 20ms to complete)
    *** @return true if locally resolved, false otherwise.
    **/
    public boolean isFastOperation() 
    {
        String host = this.getHostname(true);
        // -- "localhost:9090"
        int p = host.indexOf(":");
        String h = (p >= 0)? host.substring(0,p) : host;
        if (h.equalsIgnoreCase("localhost") || h.equals("127.0.0.1")) {
            // -- resolved locally, assume fast
            return true;
        } else {
            // -- this may be a slow operation
            return super.isFastOperation();
        }
    }

    /**
    *** Returns a ReverseGeocode instance for the specified GeoPoint
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    public ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr, boolean cache) 
    {
        ReverseGeocode rg = this.getAddressReverseGeocode(gp, localeStr, cache);
        return rg;
    }

    /* return subdivision */
    public String getSubdivision(GeoPoint gp) 
    {
        throw new UnsupportedOperationException("Not supported");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the hostname
    *** @param primary  True to return the primary host, else failover host
    *** @return The hostname
    **/
    private String getHostname(boolean primary) 
    {
        RTProperties rtp = this.getProperties();
        String host = primary?
            rtp.getString(PROP_host        , HOST_PRIMARY ) :
            rtp.getString(PROP_failoverHost, HOST_FAILOVER);
        return host;
    }

    /**
    *** Returns a ReverseGeocode instance containing address information
    *** @param gp  The GeoPoint
    *** @return The ReverseGeocode instance
    **/
    private ReverseGeocode getAddressReverseGeocode(GeoPoint gp, String localeStr, boolean cache) 
    {

        /* ReverseGeocode instance */
        ReverseGeocode rg = new ReverseGeocode();

        /* get street address URL */
        String street_url = this.getStreetReverseGeocodeURL(gp);
        //if (!StringTools.isBlank(DEBUG_STREET_URL)) { street_url = DEBUG_STREET_URL; }
        Print.logInfo("Street URL: " + street_url);

        /* read/parse Street address */
        Document street_xmlDoc = GetXMLDocument(street_url);
        if (street_xmlDoc != null) {
            Element results = street_xmlDoc.getDocumentElement();
            if (results.getTagName().equalsIgnoreCase(TAG_results)) {
                NodeList ResultList = results.getElementsByTagName(TAG_result);
                for (int g = 0; (g < ResultList.getLength()); g++) {
                    // -- parse "result" tag section
                    Element response = (Element)ResultList.item(g);
                    NodeList responseNodes = response.getChildNodes();
                    for (int n = 0; n < responseNodes.getLength(); n++) {
                        // -- iterate through "result" sub-tags
                        Node responseNode = responseNodes.item(n);
                        if (!(responseNode instanceof Element)) { continue; }
                        Element responseElem = (Element)responseNode;
                        String responseNodeName = responseElem.getNodeName();
                        if (responseNodeName.equalsIgnoreCase(TAG_formatedFull)) {
                            String address = GisGraphy.GetNodeText(responseElem);
                            rg.setFullAddress(address);
                        } else
                        if (responseNodeName.equalsIgnoreCase(TAG_streetName)) {
                            String _streetAddr = StringTools.trim(rg.getStreetAddress());
                            String streetName  = GisGraphy.GetNodeText(responseElem);
                            rg.setStreetAddress(StringTools.trim(_streetAddr+" "+streetName));
                        } else
                        if (responseNodeName.equalsIgnoreCase(TAG_houseNumber)) {
                            String _streetAddr = StringTools.trim(rg.getStreetAddress());
                            String houseNumber = GisGraphy.GetNodeText(responseElem);
                            rg.setStreetAddress(StringTools.trim(houseNumber+" "+_streetAddr));
                        } else
                        if (responseNodeName.equalsIgnoreCase(TAG_state)) {
                            String state = GisGraphy.GetNodeText(responseElem);
                            // -- appears to return the County, rather than the state.
                            String code = USState.getCode(state,null); // ie. "California" ==> "CA"
                            if (!StringTools.isBlank(code)) {
                                rg.setStateProvince(code);
                            } else {
                                rg.setStateProvince(state);
                            }
                        } else
                        if (responseNodeName.equalsIgnoreCase(TAG_zipCode)) {
                            rg.setPostalCode(GisGraphy.GetNodeText(responseElem));
                        } else
                        if (responseNodeName.equalsIgnoreCase(TAG_countryCode)) {
                            rg.setCountryCode(GisGraphy.GetNodeText(responseElem));
                        } 
                    }
                    break; // first result only
                } // <result>
            }  // <results>
        }

        /* create/return address */
        if (rg.hasFullAddress()) {
            String addr = rg.getFullAddress();
            // -- remove "United States," (may also contains "US")
            {
                String s = "United States,";
                int p = addr.indexOf(s); // superfluous addition
                if (p >= 0) {
                    addr = addr.substring(0,p).trim() + " " + addr.substring(p+s.length()).trim();
                }
            }
            // -- remove ", US" (country is assumed)
            {
                String s = ", US";
                int p = addr.indexOf(s); // "US" is already assumed
                if (p >= 0) {
                    addr = addr.substring(0,p).trim() + " " + addr.substring(p+s.length()).trim();
                }
            }
            // -- convert state name to state abbreviation (ie. "California" ==> "CA")
            {
                int p = addr.lastIndexOf(",");
                if (p >= 0) {
                    String st = addr.substring(p+1).trim();
                    String cd = USState.getCode(st,null);
                    if (!StringTools.isBlank(cd)) {
                        addr = addr.substring(0,p) + ", " + cd;
                    }
                }
            }
            // -- set resulting address
            rg.setFullAddress(addr);
        } else {
            String addr = createFullAddress(rg);
            rg.setFullAddress(addr);
        }
        
        /* return address */
        return rg;
    
    }

    private String getStreetReverseGeocodeURL(GeoPoint gp) 
    {
        StringBuffer sb = new StringBuffer();
        RTProperties rtp = this.getProperties();
        String url = rtp.getString(PROP_reversegeocodeURL, REVERSEGEOCODE_URL);
        if (!StringTools.isBlank(url)) {
            sb.append(url);
            if (!url.endsWith("?") && !url.endsWith("&")) {
                sb.append("?");
            }
        } else {
            boolean useSSL = rtp.getBoolean(PROP_useSSL,true);
            if (useSSL) {
                sb.append("https://");
            } else {
                sb.append("http://");
            }
            sb.append(this.getHostname(true));
            sb.append("/reversegeocoding/reversegeocode?");
        }
        // -- standard vars
        sb.append("from=1&to=1&format=xml&indent=true");
        // -- apikey
        String apikey = this.getProperties().getString(PROP_gisgraphyApikey,null);
        if (StringTools.isBlank(apikey)) { apikey = super.getAuthorization(); }
        if (!StringTools.isBlank(apikey)) {
            sb.append("&apikey=").append(URIArg.encodeArg(apikey));
        }
        // -- latitude/longitude
        sb.append("&lat=").append(gp.getLatitudeString(GeoPoint.SFORMAT_DEC_5,null));
        sb.append("&lng=").append(gp.getLongitudeString(GeoPoint.SFORMAT_DEC_5,null));
        return sb.toString();
    }

    private Document GetXMLDocument(String url) 
    {
         try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream input = HTMLTools.inputStream_GET(url, null, SERVICE_TIMEOUT_MS);
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

    /* return the value of the XML text node */
    protected static String GetNodeText(Node root)
    {
        StringBuffer sb = new StringBuffer();
        if (root != null) {
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) { // CDATA Section
                    sb.append(n.getNodeValue());
                } else
                if (n.getNodeType() == Node.TEXT_NODE) {
                    sb.append(n.getNodeValue());
                } 
            }
        }
        return sb.toString();
    }

    private String createFullAddress(ReverseGeocode rg) 
    {
        StringBuffer sb = new StringBuffer();
        // -- street address
        if (!StringTools.isBlank(rg.getStreetAddress())) {
            if (sb.length() > 0) { sb.append(", "); }
            String street = rg.getStreetAddress();
            sb.append(street);
        }
        // -- city
        if (!StringTools.isBlank(rg.getCity())){
            if (sb.length() > 0) { sb.append(", "); }
            String city = rg.getCity();
            sb.append(city);
        }
        // -- state
        if (!StringTools.isBlank(rg.getStateProvince())) {
            if (sb.length() > 0) { sb.append(", "); }
            String state = rg.getStateProvince();
            String code  = USState.getCode(state,null);
            if (!StringTools.isBlank(code)) {
                sb.append(code);
            } else {
                sb.append(state);
            }
        }
        // -- zip code
        if (!StringTools.isBlank(rg.getPostalCode())) {
            if (sb.length() > 0) { sb.append(" "); }
            String zip = rg.getPostalCode();
            sb.append(zip);
        }
        // -- country
        if (!StringTools.isBlank(rg.getCountryCode())) {
            if (sb.length() > 0) { sb.append(", "); }
            String country = rg.getCountryCode();
            sb.append(country);
        }
        // -- return
        return sb.toString();
    }

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

        /* failover */
        String failover = RTConfig.getString("failoverHost",null);
        if (!StringTools.isBlank(failover)) {
            HOST_FAILOVER = failover;
        }

        /* GeoPoint */
        GeoPoint gp = new GeoPoint(RTConfig.getString("gp",null));
        if (!gp.isValid()) {
            Print.logInfo("Invalid GeoPoint specified");
            System.exit(1);
        }
        Print.logInfo("Reverse-Geocoding GeoPoint: " + gp);

        /* Reverse Geocoding */
        GisGraphy gn = new GisGraphy("gisgraphy", null, RTConfig.getCommandLineProperties());

        Print.sysPrintln("RevGeocode = " + gn.getReverseGeocode(gp,null/*localeStr*/,false/*cache*/));
        //Print.sysPrintln("Address    = " + gn.getAddressReverseGeocode(gp));
        //Print.sysPrintln("PostalCode = " + gn.getPostalReverseGeocode(gp));
        //Print.sysPrintln("PlaceName  = " + gn.getPlaceNameReverseGeocode(gp));
        // Note: Even though the values are printed in UTF-8 character encoding, the
        // characters may not appear to be properly displayed if the console display
        // does not support UTF-8.

    }

}
