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
//  2016/04/06  Martin D. Flynn
//     -Updated to optimize name lookups
// ----------------------------------------------------------------------------
package org.opengts.geocoder.country;

import java.util.*;

import org.opengts.util.*;

public class Canada
{

    // ------------------------------------------------------------------------

    public static final String COUNTRY_CA       = "CA";
    public static final String COUNTRY_CA_      = COUNTRY_CA + CountryCode.SUBDIVISION_SEPARATOR;

    // ------------------------------------------------------------------------

    private static HashMap<String,ProvinceInfo> GlobalLookupMap   = new HashMap<String,ProvinceInfo>();

    /**
    *** ProvinceInfo class
    **/
    public static class ProvinceInfo
    {

        private String code     = null;
        private String name     = null;
        private String abbrev   = null;
        private String fips     = null;

        public ProvinceInfo(String code, String name, String abbrev, String fips) {
            this.code   = code;
            this.name   = name;
            this.abbrev = abbrev;
            this.fips   = fips;
        }

        public String getCode() {
            return this.code;
        }

        public String getAbbreviation() {
            return this.abbrev;
        }

        public String getName() {
            return this.name;
        }

        public boolean hasFIPS() {
            return !StringTools.isBlank(this.getFIPS())? true : false;
        }
        public String getFIPS() {
            return this.fips;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getCode());
            if (this.hasFIPS()) {
                sb.append("[").append(this.getFIPS()).append("]");
            }
            sb.append(" ");
            sb.append(this.getName());
            sb.append(" (").append(this.getAbbreviation()).append(")");
            return sb.toString();
        }

    }

    // ------------------------------------------------------------------------

    public static final ProvinceInfo ProvinceMapArray[] = new ProvinceInfo[] {
        //               Code  Name                    Abbrev    FIPS
        //               ----  ----------------------  --------  ----
        new ProvinceInfo("AB", "Alberta"              , "Alb."  , "01"),
        new ProvinceInfo("BC", "British Columbia"     , "B.C."  , "02"),
        new ProvinceInfo("MB", "Manitoba"             , "Man."  , "03"),
        new ProvinceInfo("NB", "New Brunswick"        , "N.B."  , "04"),
        new ProvinceInfo("NL", "Newfoundland"         , "New."  , "05"),
        new ProvinceInfo("NT", "Northwest Territories", "N.T."  , "06"), // 13?
        new ProvinceInfo("NS", "Nova Scotia"          , "N.C."  , "07"), // 
        new ProvinceInfo("NU", "Nunavut"              , "Nun."  , "13"),
        new ProvinceInfo("ON", "Ontario"              , "Ont."  , "08"),
        new ProvinceInfo("PE", "Prince Edward Island" , "PEI"   , "09"),
        new ProvinceInfo("QC", "Quebec"               , "Queb." , "10"),
        new ProvinceInfo("SK", "Saskatchewan"         , "Sas."  , "11"),
        new ProvinceInfo("YT", "Yukon"                , "Yuk."  , "12"),
    };

    // -- startup initialization
    static {
        for (int i = 0; i < ProvinceMapArray.length; i++) {
            // -- add CODE
            String code = ProvinceMapArray[i].getCode(); // never blank
            GlobalLookupMap.put(code, ProvinceMapArray[i]);
            // -- add FIPS
            String fips = ProvinceMapArray[i].getFIPS(); // may be blank
            if (!StringTools.isBlank(fips)) {
                GlobalLookupMap.put(fips, ProvinceMapArray[i]);
            }
            // -- add name (uppercase)
            String name = ProvinceMapArray[i].getName().toUpperCase(); // never blank
            GlobalLookupMap.put(name, ProvinceMapArray[i]);
        }
    }

    /**
    *** Gets the ProvinceInfo instance for the specified province code/abbreviation
    *** @param code Must be either province code, name, or FIPS
    **/
    public static ProvinceInfo getProvinceInfo(String code)
    {
        String c = StringTools.trim(code).toUpperCase();
        if (!StringTools.isBlank(c)) {
            if (c.startsWith(Canada.COUNTRY_CA_)) { 
                // -- CA/##: remove prefixing "CA/"
                c = c.substring(Canada.COUNTRY_CA_.length()); 
            } else
            if ((c.length() == 4) && c.startsWith(Canada.COUNTRY_CA)) { 
                // -- CA##: remove prefixing "CA"
                c = c.substring(Canada.COUNTRY_CA.length()); 
            }
            return GlobalLookupMap.get(c);
        } else {
            return null;
        }
    }

    /**
    *** Returns true if the specified Canada province code/abbreviation exists
    *** @param code Must be either province code, name, or FIPS
    **/
    public static boolean hasCode(String code)
    {
        return (Canada.getProvinceInfo(code) != null)? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Canada province code for the specified province name
    *** @param code Must be either province code, name, or FIPS
    *** @param dft  The default code to return if the specified province name is not found
    *** @return The province code
    **/
    public static String getCode(String code, String dft)
    {
        ProvinceInfo pi = Canada.getProvinceInfo(code);
        return (pi != null)? pi.getCode() : dft;
    }

    /**
    *** Gets the province code for the specified FIPS
    *** @param fips  The province FIPS code (as an int)
    *** @param dft   The default code to return if the specified province FIPS is not found
    *** @return The province code
    **/
    public static String getCode(int fips, String dft)
    {
        return Canada.getCode(StringTools.format(fips,"00"), dft);
    }

    /**
    *** Gets the Canada province name for the specified province code
    *** @param code Must be either province code, name, or FIPS
    *** @param dft  The default name to return if the code is not defined
    *** @return The province name, or an empty String if the province code was not found
    **/
    public static String getName(String code, String dft)
    {
        ProvinceInfo pi = Canada.getProvinceInfo(code);
        return (pi != null)? pi.getName() : dft;
    }

    /**
    *** Gets the Canada province abbreviation for the specified province code
    *** @param code Must be either province code, name, or FIPS
    *** @param dft  The default abbreviation to return if the specified code was not found 
    *** @return The province abbreviation
    **/
    public static String getAbbreviation(String code, String dft)
    {
        ProvinceInfo pi = Canada.getProvinceInfo(code);
        return (pi != null)? pi.getAbbreviation() : "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_CODE[]          = { "code" };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        // -- lookup code/fips
        if (RTConfig.hasProperty(ARG_CODE)) {
            String code = RTConfig.getString(ARG_CODE,"");
            ProvinceInfo pi = Canada.getProvinceInfo(code);
            if (pi == null) {
                Print.sysPrintln("ERROR: code not found");
                System.exit(1);
            }
            Print.sysPrintln("Province: " + pi);
            System.exit(0);
        }

    }

}
