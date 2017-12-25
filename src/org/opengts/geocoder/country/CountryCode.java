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
//  2015/06/12  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.geocoder.country;

import java.util.*;

import org.opengts.util.*;

public class CountryCode
{

    // ------------------------------------------------------------------------

    public static final String SUBDIVISION_SEPARATOR    = "/";

    // ------------------------------------------------------------------------

    private static HashMap<String,CountryInfo> GlobalCountryMap = new HashMap<String,CountryInfo>();

    /**
    *** CountryInfo class
    **/
    public static class CountryInfo
    {

        private String       code2    = null; // 2-letter code
        private String       code3    = null; // 3-letter code
        private String       phone    = null; // phone country dialing code
        private String       name     = null; // name
        private MethodAction sdMeth   = null; // subdivision name method

        public CountryInfo(String code2, String code3, String phone, String name) {
            this(code2, code3, phone, name, null, null);
        }

        public CountryInfo(String code2, String code3, String phone, String name, Class<?> sdClass, String sdMeth) {
            this.code2   = code2;
            this.code3   = code3;
            this.phone   = phone;
            this.name    = name;
            if (sdClass != null) {
                try {
                    this.sdMeth = new MethodAction(sdClass, sdMeth, String.class, String.class);
                } catch (Throwable th) {
                    Print.logException("Unable to create Subdivision lookup method ["+name+"]",th);
                }
            }
            //String R = StringTools.replicateString(" ", 23-name.length());
            //Print.sysPrintln("new CountryInfo(\""+code2+"\", \""+code3+"\", \""+name+"\""+R+"),");
        }

        public String getCode2() {
            return this.code2;
        }

        public String getCode3() {
            return this.code3;
        }

        public String getCode() {
            return this.getCode2();
        }

        public String getDialingCode() {
            return this.phone;
        }

        public String getName() {
            return this.name;
        }

        public boolean supportsSubdivisionName() {
            return (this.sdMeth != null)? true : false;
        }

        public String getSubdivisionName(String stateCode) {
            if ((this.sdMeth != null) && !StringTools.isBlank(stateCode)) {
                try {
                    return (String)this.sdMeth.invoke(stateCode,"");
                } catch (Throwable th) {
                    Print.logException("Unable to get Subdivision name ["+this.getName()+"]",th);
                }
            }
            return "";
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getCode2());
            sb.append("/");
            sb.append(this.getCode3());
            sb.append(" ");
            sb.append(this.getName());
            return sb.toString();
        }

    }

    // ------------------------------------------------------------------------
    // References:
    //  - https://countrycode.org
    //  - https://www.iso.org/iso-3166-country-codes.html

    public static final CountryInfo CountryMapArray[] = new CountryInfo[] {
        //              Code  Code   Phone    Name
        //              2-dig 3-dig  Code
        //              ----- ------ -------- -------------------------
        new CountryInfo("AF", "AFG",     "93", "Afghanistan"            ),
        new CountryInfo("AL", "ALB",    "355", "Albania"                ),
        new CountryInfo("DZ", "DZA",    "213", "Algeria"                ),
        new CountryInfo("AD", "AND",    "376", "Andorra"                ),
        new CountryInfo("AO", "AGO",    "244", "Angola"                 ),
        new CountryInfo("AI", "AIA",  "1-264", "Anguilla"               ),
        new CountryInfo("AQ", "ATA",    "672", "Antarctica"             ),
        new CountryInfo("AG", "ATG",  "1-268", "Antigua/Barbuda"        ),
        new CountryInfo("AR", "ARG",     "54", "Argentina"              ),
        new CountryInfo("AM", "ARM",    "374", "Armenia"                ),
        new CountryInfo("AW", "ABW",    "297", "Aruba"                  ),
        new CountryInfo("AU", "AUS",     "61", "Australia"              ),
        new CountryInfo("AT", "AUT",     "43", "Austria"                ),
        new CountryInfo("AZ", "AZE",    "994", "Azerbaijan"             ),
        new CountryInfo("BS", "BHS",  "1-242", "Bahamas"                ),
        new CountryInfo("BH", "BHR",    "973", "Bahrain"                ),
        new CountryInfo("BD", "BGD",    "880", "Bangladesh"             ),
        new CountryInfo("BB", "BRB",  "1-246", "Barbados"               ),
        new CountryInfo("BY", "BLR",    "375", "Belarus"                ),
        new CountryInfo("BE", "BEL",     "32", "Belgium"                ),
        new CountryInfo("BZ", "BLZ",    "501", "Belize"                 ),
        new CountryInfo("BJ", "BEN",    "229", "Benin"                  ),
        new CountryInfo("BM", "BMU",  "1-441", "Bermuda"                ),
        new CountryInfo("BT", "BTN",    "975", "Bhutan"                 ),
        new CountryInfo("BO", "BOL",    "591", "Bolivia"                ),
        new CountryInfo("BQ", "BES",    "599", "Bonaire"                ),
        new CountryInfo("BA", "BIH",    "387", "Bosnia/Herzegovina"     ),
        new CountryInfo("BW", "BWA",    "267", "Botswana"               ),
        new CountryInfo("BR", "BRA",     "55", "Brazil"                 ), // Brasil
        new CountryInfo("BN", "BRN",    "673", "Brunei Darussalam"      ),
        new CountryInfo("BG", "BGR",    "359", "Bulgaria"               ),
        new CountryInfo("BF", "BFA",    "226", "Burkina Faso"           ),
        new CountryInfo("BI", "BDI",    "257", "Burundi"                ),
        new CountryInfo("KH", "KHM",    "855", "Cambodia"               ),
        new CountryInfo("CM", "CMR",    "237", "Cameroon"               ),
        new CountryInfo("CA", "CAN",      "1", "Canada"                 , Canada.class, "getName"),
        new CountryInfo("CV", "CPV",    "238", "Cape Verde"             ),
        new CountryInfo("KY", "CYM",  "1-345", "Cayman Islands"         ),
        new CountryInfo("TD", "TCD",    "235", "Chad"                   ),
        new CountryInfo("CL", "CHL",     "56", "Chile"                  ),
        new CountryInfo("CN", "CHN",     "86", "China"                  ),
        new CountryInfo("CO", "COL",     "57", "Colombia"               ),
        new CountryInfo("KM", "COM",    "269", "Comoros"                ),
        new CountryInfo("CG", "COG",    "242", "Republic of the Congo"  ),
        new CountryInfo("CD", "COD",    "243", "Dem Rep of the Congo"   ),
        new CountryInfo("CR", "CRI",    "506", "Costa Rica"             ),
        new CountryInfo("HR", "HRV",    "385", "Croatia"                ),
        new CountryInfo("CU", "CUB",     "53", "Cuba"                   ),
        new CountryInfo("CW", "CUW",    "599", "Curacao"                ),
        new CountryInfo("CY", "CYP",    "357", "Cyprus"                 ),
        new CountryInfo("CZ", "CZE",    "420", "Czech Republic"         ),
        new CountryInfo("CI", "CIV",    "225", "Ivory Coast "           ),
        new CountryInfo("DK", "DNK",     "45", "Denmark"                ),
        new CountryInfo("DJ", "DJI",    "253", "Djibouti"               ),
        new CountryInfo("DM", "DMA",  "1-767", "Dominica"               ),
        new CountryInfo("DO", "DOM",  "1-809", "Dominican Republic"     ), // 1-809, 1-829, 1-849
        new CountryInfo("EC", "ECU",    "593", "Ecuador"                ),
        new CountryInfo("EG", "EGY",     "20", "Egypt"                  ),
        new CountryInfo("SV", "SLV",    "503", "El Salvador"            ),
        new CountryInfo("ER", "ERI",    "291", "Eritrea"                ),
        new CountryInfo("EE", "EST",    "372", "Estonia"                ),
        new CountryInfo("ET", "ETH",    "251", "Ethiopia"               ),
        new CountryInfo("FJ", "FJI",    "679", "Fiji"                   ),
        new CountryInfo("FI", "FIN",    "358", "Finland"                ),
        new CountryInfo("FR", "FRA",     "33", "France"                 ),
        new CountryInfo("GA", "GAB",    "241", "Gabon"                  ),
        new CountryInfo("GM", "GMB",    "220", "Gambia"                 ),
        new CountryInfo("GE", "GEO",    "995", "Georgia"                ),
        new CountryInfo("DE", "DEU",     "49", "Germany"                ),
        new CountryInfo("GH", "GHA",    "233", "Ghana"                  ),
        new CountryInfo("GI", "GIB",    "350", "Gibraltar"              ),
        new CountryInfo("GR", "GRC",     "30", "Greece"                 ),
        new CountryInfo("GL", "GRL",    "299", "Greenland"              ),
        new CountryInfo("GD", "GRD",  "1-473", "Grenada"                ),
        new CountryInfo("GP", "GLP",    "590", "Guadeloupe"             ),
        new CountryInfo("GU", "GUM",  "1-671", "Guam"                   ),
        new CountryInfo("GT", "GTM",    "502", "Guatemala"              ),
        new CountryInfo("GG", "GGY","44-1481", "Guernsey"               ),
        new CountryInfo("GN", "GIN",    "224", "Guinea"                 ),
        new CountryInfo("GY", "GUY",    "592", "Guyana"                 ),
        new CountryInfo("HT", "HTI",    "509", "Haiti"                  ),
        new CountryInfo("HN", "HND",    "504", "Honduras"               ),
        new CountryInfo("HK", "HKG",    "852", "Hong Kong"              ),
        new CountryInfo("HU", "HUN",     "36", "Hungary"                ),
        new CountryInfo("IS", "ISL",    "354", "Iceland"                ),
        new CountryInfo("IN", "IND",     "91", "India"                  ),
        new CountryInfo("ID", "IDN",     "62", "Indonesia"              ),
        new CountryInfo("IR", "IRN",     "98", "Iran"                   ),
        new CountryInfo("IQ", "IRQ",    "964", "Iraq"                   ),
        new CountryInfo("IE", "IRL",    "353", "Ireland"                ),
        new CountryInfo("IM", "IMN","44-1624", "Isle of Man"            ),
        new CountryInfo("IL", "ISR",    "972", "Israel"                 ),
        new CountryInfo("IT", "ITA",     "39", "Italy"                  ),
        new CountryInfo("JM", "JAM",  "1-876", "Jamaica"                ),
        new CountryInfo("JP", "JPN",     "81", "Japan"                  ),
        new CountryInfo("JE", "JEY","44-1534", "Jersey"                 ),
        new CountryInfo("JO", "JOR",    "962", "Jordan"                 ),
        new CountryInfo("KZ", "KAZ",      "7", "Kazakhstan"             ),
        new CountryInfo("KE", "KEN",    "254", "Kenya"                  ),
        new CountryInfo("KI", "KIR",    "686", "Kiribati"               ),
        new CountryInfo("XK", "XKX",    "383", "Kosovo"                 ),
        new CountryInfo("KR", "KOR",     "82", "South Korea"            ),
        new CountryInfo("KP", "PRK",    "850", "North Korea"            ),
        new CountryInfo("KW", "KWT",    "965", "Kuwait"                 ),
        new CountryInfo("KG", "KGZ",    "996", "Kyrgyzstan"             ),
        new CountryInfo("LA", "LAO",    "856", "Laos"                   ),
        new CountryInfo("LV", "LVA",    "371", "Latvia"                 ),
        new CountryInfo("LB", "LBN",    "961", "Lebanon"                ),
        new CountryInfo("LS", "LSO",    "266", "Lesotho"                ),
        new CountryInfo("LR", "LBR",    "231", "Liberia"                ),
        new CountryInfo("LY", "LBY",    "218", "Libya"                  ),
        new CountryInfo("LI", "LIE",    "423", "Liechtenstein"          ),
        new CountryInfo("LT", "LTU",    "370", "Lithuania"              ),
        new CountryInfo("LU", "LUX",    "352", "Luxembourg"             ),
        new CountryInfo("MO", "MAC",    "853", "Macao"                  ),
        new CountryInfo("MK", "MKD",    "389", "Macedonia"              ),
        new CountryInfo("MG", "MDG",    "261", "Madagascar"             ),
        new CountryInfo("MW", "MWI",    "265", "Malawi"                 ),
        new CountryInfo("MY", "MYS",     "60", "Malaysia"               ),
        new CountryInfo("MV", "MDV",    "960", "Maldives"               ),
        new CountryInfo("ML", "MLI",    "223", "Mali"                   ),
        new CountryInfo("MT", "MLT",    "356", "Malta"                  ),
        new CountryInfo("MH", "MHL",    "692", "Marshall Islands"       ),
        new CountryInfo("MQ", "MTQ",    "596", "Martinique"             ),
        new CountryInfo("MR", "MRT",    "222", "Mauritania"             ),
        new CountryInfo("MU", "MUS",    "230", "Mauritius"              ),
        new CountryInfo("YT", "MYT",    "262", "Mayotte"                ),
        new CountryInfo("MX", "MEX",     "52", "Mexico"                 , Mexico.class, "getName"),
        new CountryInfo("FM", "FSM",    "691", "Micronesia"             ),
        new CountryInfo("MD", "MDA",    "373", "Moldova"                ),
        new CountryInfo("MC", "MCO",    "377", "Monaco"                 ),
        new CountryInfo("MN", "MNG",    "976", "Mongolia"               ),
        new CountryInfo("ME", "MNE",    "382", "Montenegro"             ),
        new CountryInfo("MS", "MSR",  "1-664", "Montserrat"             ),
        new CountryInfo("MA", "MAR",    "212", "Morocco"                ),
        new CountryInfo("MZ", "MOZ",    "258", "Mozambique"             ),
        new CountryInfo("MM", "MMR",     "95", "Myanmar"                ),
        new CountryInfo("NA", "NAM",    "264", "Namibia"                ),
        new CountryInfo("NR", "NRU",    "674", "Nauru"                  ),
        new CountryInfo("NP", "NPL",    "977", "Nepal"                  ),
        new CountryInfo("NL", "NLD",     "31", "Netherlands"            ),
        new CountryInfo("NC", "NCL",    "687", "New Caledonia"          ),
        new CountryInfo("NZ", "NZL",     "64", "New Zealand"            ),
        new CountryInfo("NI", "NIC",    "505", "Nicaragua"              ),
        new CountryInfo("NE", "NER",    "227", "Niger"                  ),
        new CountryInfo("NG", "NGA",    "234", "Nigeria"                ),
        new CountryInfo("NU", "NIU",    "683", "Niue"                   ),
        new CountryInfo("NF", "NFK",    "672", "Norfolk Island"         ),
        new CountryInfo("NO", "NOR",     "47", "Norway"                 ),
        new CountryInfo("OM", "OMN",    "968", "Oman"                   ),
        new CountryInfo("PK", "PAK",     "92", "Pakistan"               ),
        new CountryInfo("PW", "PLW",    "680", "Palau"                  ),
        new CountryInfo("PS", "PSE",    "970", "Palestine"              ),
        new CountryInfo("PA", "PAN",    "507", "Panama"                 ),
        new CountryInfo("PY", "PRY",    "595", "Paraguay"               ),
        new CountryInfo("PE", "PER",     "51", "Peru"                   ),
        new CountryInfo("PH", "PHL",     "63", "Philippines"            ),
        new CountryInfo("PN", "PCN",     "64", "Pitcairn"               ),
        new CountryInfo("PL", "POL",     "48", "Poland"                 ),
        new CountryInfo("PT", "PRT",    "351", "Portugal"               ),
        new CountryInfo("PR", "PRI",  "1-787", "Puerto Rico"            ), // 1-787, 1-939
        new CountryInfo("QA", "QAT",    "974", "Qatar"                  ),
        new CountryInfo("RE", "REU",    "262", "Reunion"                ),
        new CountryInfo("RO", "ROU",     "40", "Romania"                ),
        new CountryInfo("RU", "RUS",      "7", "Russia"                 ),
        new CountryInfo("RW", "RWA",    "250", "Rwanda"                 ),
        new CountryInfo("LC", "LCA",  "1-758", "Saint Lucia"            ),
        new CountryInfo("MF", "MAF",  "1-784", "Saint Martin (French)"  ),
        new CountryInfo("WS", "WSM",    "685", "Samoa"                  ),
        new CountryInfo("SM", "SMR",    "378", "San Marino"             ),
        new CountryInfo("ST", "STP",    "239", "Sao Tome and Principe"  ),
        new CountryInfo("SA", "SAU",    "966", "Saudi Arabia"           ),
        new CountryInfo("SN", "SEN",    "221", "Senegal"                ),
        new CountryInfo("RS", "SRB",    "381", "Serbia"                 ),
        new CountryInfo("SC", "SYC",    "248", "Seychelles"             ),
        new CountryInfo("SL", "SLE",    "232", "Sierra Leone"           ),
        new CountryInfo("SG", "SGP",     "65", "Singapore"              ),
        new CountryInfo("SX", "SXM",  "1-721", "Sint Maarten (Dutch)"   ),
        new CountryInfo("SK", "SVK",    "421", "Slovakia"               ),
        new CountryInfo("SI", "SVN",    "386", "Slovenia"               ),
        new CountryInfo("SO", "SOM",    "252", "Somalia"                ),
        new CountryInfo("ZA", "ZAF",     "27", "South Africa"           ),
        new CountryInfo("ES", "ESP",     "34", "Spain"                  ),
        new CountryInfo("LK", "LKA",     "94", "Sri Lanka"              ),
        new CountryInfo("SD", "SDN",    "249", "Sudan"                  ),
        new CountryInfo("SR", "SUR",    "597", "Suriname"               ),
        new CountryInfo("SZ", "SWZ",    "268", "Swaziland"              ),
        new CountryInfo("SE", "SWE",     "46", "Sweden"                 ),
        new CountryInfo("CH", "CHE",     "41", "Switzerland"            ),
        new CountryInfo("SY", "SYR",    "963", "Syria"                  ),
        new CountryInfo("TW", "TWN",    "886", "Taiwan"                 ),
        new CountryInfo("TJ", "TJK",    "992", "Tajikistan"             ),
        new CountryInfo("TZ", "TZA",    "255", "Tanzania"               ),
        new CountryInfo("TH", "THA",     "66", "Thailand"               ),
        new CountryInfo("TL", "TLS",    "670", "Timor-Leste"            ), // East Timor
        new CountryInfo("TG", "TGO",    "228", "Togo"                   ),
        new CountryInfo("TK", "TKL",    "690", "Tokelau"                ),
        new CountryInfo("TO", "TON",    "676", "Tonga"                  ),
        new CountryInfo("TT", "TTO",  "1-868", "Trinidad/Tobago"        ),
        new CountryInfo("TN", "TUN",    "216", "Tunisia"                ),
        new CountryInfo("TR", "TUR",     "90", "Turkey"                 ),
        new CountryInfo("TM", "TKM",    "993", "Turkmenistan"           ),
        new CountryInfo("TV", "TUV",    "688", "Tuvalu"                 ),
        new CountryInfo("UG", "UGA",    "256", "Uganda"                 ),
        new CountryInfo("UA", "UKR",    "380", "Ukraine"                ),
        new CountryInfo("AE", "ARE",    "971", "United Arab Emirates"   ),
        new CountryInfo("GB", "GBR",     "44", "United Kingdom"         ),
        new CountryInfo("US", "USA",      "1", "United States"          , USState.class, "getName"),
        new CountryInfo("UY", "URY",    "598", "Uruguay"                ),
        new CountryInfo("UZ", "UZB",    "998", "Uzbekistan"             ),
        new CountryInfo("VU", "VUT",    "678", "Vanuatu"                ),
        new CountryInfo("VA", "VAT",    "379", "Vatican"                ),
        new CountryInfo("VE", "VEN",     "58", "Venezuela"              ),
        new CountryInfo("VN", "VNM",     "84", "Viet Nam"               ),
        new CountryInfo("VG", "VGB",  "1-284", "British Virgin Islands" ),
        new CountryInfo("VI", "VIR",  "1-340", "US Virgin Islands"      ),
        new CountryInfo("ZM", "ZMB",    "260", "Zambia"                 ),
        new CountryInfo("ZW", "ZWE",    "263", "Zimbabwe"               ),
    };

    // -- startup initialization
    static {
        for (int i = 0; i < CountryMapArray.length; i++) {
            // -- add CODE-2
            String code2 = CountryMapArray[i].getCode2(); // never blank
            GlobalCountryMap.put(code2, CountryMapArray[i]);
            // -- add CODE-3
            String code3 = CountryMapArray[i].getCode3(); // may be blank
            if (!StringTools.isBlank(code3)) {
                GlobalCountryMap.put(code3, CountryMapArray[i]);
            }
        }
    }

    /**
    *** Gets the collection of StateInfo keys (state codes)
    **/
    public static Collection<String> getCountryInfoKeys()
    {
        return GlobalCountryMap.keySet();
    }

    /**
    *** Gets the CountryInfo instance for the specified country code
    **/
    public static CountryInfo getCountryInfo(String code)
    {
        if (!StringTools.isBlank(code)) {
            return GlobalCountryMap.get(code);
        } else {
            return null;
        }
    }

    /**
    *** Returns true if the specified country code exists
    **/
    public static boolean hasCountryInfo(String code)
    {
        return (CountryCode.getCountryInfo(code) != null)? true : false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified country code is defined
    *** @param code  The country code
    *** @return True if teh specified country is defined, false otherwise
    **/
    public static boolean isCountryCode(String code)
    {
        return CountryCode.hasCountryInfo(code);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the country name for the specified country code
    *** @param code  The country code
    *** @return The country name, or an empty String if the country code was not found
    **/
    public static String getCountryName(String code)
    {
        CountryInfo ci = CountryCode.getCountryInfo(code);
        return (ci != null)? ci.getName() : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the subdivision name for the specified CountryCode/StateCode
    **/
    public static String getSubdivisionName(String subDiv)
    {
        if (!StringTools.isBlank(subDiv)) {
            int p = subDiv.indexOf(SUBDIVISION_SEPARATOR);
            if (p >= 0) {
                String CC = subDiv.substring(0,p);
                String SC = subDiv.substring(p+1);
                return CountryCode.getSubdivisionName(CC,SC);
            }
        }
        return "";
    }

    /**
    *** Gets the subdivision name for the specified CountryCode/StateCode
    **/
    public static String getSubdivisionName(String countryCode, String stateCode)
    {
        if (!StringTools.isBlank(countryCode) && !StringTools.isBlank(stateCode)) {
            CountryInfo ci = CountryCode.getCountryInfo(countryCode);
            if ((ci != null) && ci.supportsSubdivisionName()) {
                return ci.getSubdivisionName(stateCode);
            }
        }
        return "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the 2-digit country code for the specified code
    *** @param code  The 2 or 3-digit country code
    *** @param dft   The default code to return if the specified country code is not found
    *** @return The state code
    **/
    public static String getCountryCode(String code, String dft)
    {
        CountryInfo ci = CountryCode.getCountryInfo(code);
        return (ci != null)? ci.getCode() : dft;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* get code */
        String code = RTConfig.getString("code",null);
        if (StringTools.isBlank(code)) {
            Print.sysPrintln("'-code=XXX' not specified");
            System.exit(1);
        }

        /* display country matching code */
        int found = 0;
        for (CountryInfo ci : CountryMapArray) {
            String code2 = ci.getCode2();
            String code3 = ci.getCode3();
            String dial  = ci.getDialingCode();
            String name  = ci.getName();
            // -- find match
            boolean match = false;
            if (!StringTools.isBlank(code2) && code.equalsIgnoreCase(code2)) {
                match = true;
            } else
            if (!StringTools.isBlank(code3) && code.equalsIgnoreCase(code3)) {
                match = true;
            } else
            if (!StringTools.isBlank(dial ) && code.equalsIgnoreCase(dial )) {
                match = true;
            }
            // -- display if match
            if (match) {
                String _dial = !StringTools.isBlank(dial)? ("+"+dial) : "n/a";
                Print.sysPrintln(code2+"/"+code3 + " ["+_dial+"] " + name);
                found++;
            }
        }

        /* found a match? */
        if (found <= 0) {
            Print.sysPrintln("Not found ...");
        }
        System.exit(0);

    }

}

