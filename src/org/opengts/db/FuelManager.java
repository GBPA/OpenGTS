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
//  2011/04/01  Martin D. Flynn
//     -Initial release
//  2017/09/28  Martin D. Flynn
//     -Added "FuelType" Enum [2.6.5-B59]
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.tables.*;

public abstract class FuelManager
{

    // ------------------------------------------------------------------------

    public static final double BTU_PER_THERM            = 100000.0;
    public static final double THERM_PER_BTU            = 1.0 / BTU_PER_THERM;
    public static final double THERM_PER_JOULE          = 9.4804342797335E-9;
    public static final double JOULE_PER_THERM          = 1.0 / THERM_PER_JOULE;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Fuel Type

    // https://www.fuelfreedom.org/our-work/fuels-101/fuel-types/
    // https://itstillruns.com/six-fuels-used-todays-vehicles-7347672.html
    // http://www.eia.gov/tools/faqs/faq.cfm?id=307&t=11
    // http://ecoscore.be/en/info/ecoscore/co2
    public enum FuelType implements EnumTools.StringLocale, EnumTools.IntValue {
        UNKNOWN        (    0, I18N.getString(FuelManager.class,"FuelManager.FuelType.unknown"    ,"Unknown"  ), 0.00),
        GASOLINE       ( 1000, I18N.getString(FuelManager.class,"FuelManager.FuelType.gasoline"   ,"Gasoline" ), 2.35), // 2.31
        UNLEADED       ( 1100, I18N.getString(FuelManager.class,"FuelManager.FuelType.unleaded"   ,"Unleaded" ), 2.35), // 2.31
        LEADED         ( 1900, I18N.getString(FuelManager.class,"FuelManager.FuelType.unleaded"   ,"Unleaded" ), 2.35), // 2.31
        ETHENOL        ( 2000, I18N.getString(FuelManager.class,"FuelManager.FuelType.ethenol"    ,"Ethenol"  ), 2.27),
        METHENOL       ( 3000, I18N.getString(FuelManager.class,"FuelManager.FuelType.methenol"   ,"Methenol" ), 2.27),
        DIESEL         ( 4000, I18N.getString(FuelManager.class,"FuelManager.FuelType.diesel"     ,"Diesel"   ), 2.68),
        BIODIESEL      ( 5000, I18N.getString(FuelManager.class,"FuelManager.FuelType.biodiesel"  ,"Biodiesel"), 2.64),
        KEROSENE       ( 6000, I18N.getString(FuelManager.class,"FuelManager.FuelType.kerosene"   ,"Kerosene" ), 2.58),
        AVIATION       ( 7000, I18N.getString(FuelManager.class,"FuelManager.FuelType.aviation"   ,"Aviation" ), 2.58),
        JETA           ( 7100, I18N.getString(FuelManager.class,"FuelManager.FuelType.jeta"       ,"JetA"     ), 2.58),
        JETA1          ( 7110, I18N.getString(FuelManager.class,"FuelManager.FuelType.jeta1"      ,"JetA1"    ), 2.58),
        JETB           ( 7200, I18N.getString(FuelManager.class,"FuelManager.FuelType.jetb"       ,"JetB"     ), 2.58),
        PROPANE        ( 8000, I18N.getString(FuelManager.class,"FuelManager.FuelType.propane"    ,"LPG"      ), 1.51),
        NATURAL_GAS    ( 9000, I18N.getString(FuelManager.class,"FuelManager.FuelType.natural_gas","CNG"      ), 1.51),
        HYDROGEN       (10000, I18N.getString(FuelManager.class,"FuelManager.FuelType.hydrogen"   ,"Hydrogen" ), 0.00), // unknown
        OTHER          (99999, I18N.getString(FuelManager.class,"FuelManager.FuelType.other"      ,"Other"    ), 2.50);
        // --- Notes:
        // --   - There are 2 types of hydogen fueled vehicles: cumbustion and fuel-cell
        // --   - There are more than the above listed jet fuel types
        private int         vv  = 0;    // int value
        private I18N.Text   tt  = null; // text description
        private double      c02 = 0.0;  // amount CO2 kg/Litre
        FuelType(int v, I18N.Text t, double CO2) { vv = v; tt = t; c02 = CO2; };
        public int     getIntValue()             { return vv; }
        public String  toString()                { return tt.toString(); }
        public String  toString(Locale loc)      { return tt.toString(loc); }
        public boolean isUnknown()               { return this.equals(UNKNOWN); }
        public boolean isOther()                 { return this.equals(OTHER); }
        public double  c02KgPerLitre()           { return c02; }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Fuel Level Change 

    public enum LevelChangeType implements EnumTools.StringLocale, EnumTools.IntValue {
        NONE        (  0, I18N.getString(FuelManager.class,"FuelManager.LevelChangeType.none"    ,"None"    )), // default
        INCREASE    (  1, I18N.getString(FuelManager.class,"FuelManager.LevelChangeType.increase","Increase")),
        DECREASE    (  2, I18N.getString(FuelManager.class,"FuelManager.LevelChangeType.decrease","Decrease")),
        UNKNOWN     ( 99, I18N.getString(FuelManager.class,"FuelManager.LevelChangeType.unknown" ,"Unknown" ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        LevelChangeType(int v, I18N.Text a)         { vv = v; aa = a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isType(int type)             { return this.getIntValue() == type; }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public abstract LevelChangeType insertFuelLevelChange(EventData event);

    // ------------------------------------------------------------------------

}
