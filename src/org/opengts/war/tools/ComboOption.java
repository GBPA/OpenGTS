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
//  2009/01/28  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;

import org.opengts.war.tools.*;

public class ComboOption
{

    // ------------------------------------------------------------------------

    public static final String BOOLEAN_NAME_FALSE       = "false";
    public static final String BOOLEAN_NAME_TRUE        = "true";

    public static final String TRISTATE_NAME_DEFAULT    = "default";
    public static final String TRISTATE_NAME_FALSE      = BOOLEAN_NAME_FALSE;
    public static final String TRISTATE_NAME_TRUE       = BOOLEAN_NAME_TRUE;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String getOnOffText(Locale locale, boolean state)
    {
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        return state?
            i18n.getString("ComboOption.on" , "On" ) :
            i18n.getString("ComboOption.off", "Off");
    }

    public static boolean parseOnOffText(Locale locale, String state)
    {
        return ComboOption.parseOnOffText(locale, state, false);
    }

    public static boolean parseOnOffText(Locale locale, String state, boolean dft)
    {

        // initial checks
        if (StringTools.isBlank(state)) {
            return dft;
        } else
        if (state.equalsIgnoreCase(BOOLEAN_NAME_FALSE)) {
            return false;
        } else
        if (state.equalsIgnoreCase(BOOLEAN_NAME_TRUE )) {
            return true;
        }

        // local language checks
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.off","Off"))) {
            return false;
        } else
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.on" ,"On" ))) {
            return true;
        } else {
            return StringTools.parseBoolean(state, dft);
        }

    }

    public static ComboOption getOnOffOption(Locale locale, boolean state)
    {
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        return state?
            new ComboOption(BOOLEAN_NAME_TRUE , i18n.getString("ComboOption.on" , "On" )) :
            new ComboOption(BOOLEAN_NAME_FALSE, i18n.getString("ComboOption.off", "Off"));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String getYesNoText(Locale locale, boolean state)
    {
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        return state?
            i18n.getString("ComboOption.yes", "Yes") :
            i18n.getString("ComboOption.no" , "No" );
    }

    public static boolean parseYesNoText(Locale locale, String state)
    {
        return ComboOption.parseYesNoText(locale, state, false);
    }

    public static boolean parseYesNoText(Locale locale, String state, boolean dft)
    {

        // -- initial checks
        if (StringTools.isBlank(state)) {
            return dft;
        } else
        if (state.equalsIgnoreCase(BOOLEAN_NAME_FALSE)) {
            return false;
        } else
        if (state.equalsIgnoreCase(BOOLEAN_NAME_TRUE )) {
            return true;
        }

        // local language checks
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.no" ,"No" ))) {
            return false;
        } else
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.yes","Yes"))) {
            return true;
        } else {
            return StringTools.parseBoolean(state, dft);
        }

    }

    public static ComboOption getYesNoOption(Locale locale, boolean state)
    {
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        return state?
            new ComboOption(BOOLEAN_NAME_TRUE , i18n.getString("ComboOption.yes", "Yes")) :
            new ComboOption(BOOLEAN_NAME_FALSE, i18n.getString("ComboOption.no" , "No" ));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String getTrueFalseText(Locale locale, boolean state)
    {
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        return state?
            i18n.getString("ComboOption.true" , "True") :
            i18n.getString("ComboOption.false", "False" );
    }

    public static boolean parseTrueFalseText(Locale locale, String state)
    {
        return ComboOption.parseTrueFalseText(locale, state, false);
    }

    public static boolean parseTrueFalseText(Locale locale, String state, boolean dft)
    {

        // initial checks
        if (StringTools.isBlank(state)) {
            return dft;
        } else
        if (state.equalsIgnoreCase(BOOLEAN_NAME_FALSE)) {
            return false;
        } else
        if (state.equalsIgnoreCase(BOOLEAN_NAME_TRUE )) {
            return true;
        }

        // local language checks
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.false","False" ))) {
            return false;
        } else
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.true" ,"True"))) {
            return true;
        } else {
            return StringTools.parseBoolean(state, dft);
        }

    }

    public static ComboOption getTrueFalseOption(Locale locale, boolean state)
    {
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        return state?
            new ComboOption(BOOLEAN_NAME_TRUE , i18n.getString("ComboOption.true" , "True" )) :
            new ComboOption(BOOLEAN_NAME_FALSE, i18n.getString("ComboOption.false", "False"));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String getTriStateText(Locale locale, TriState state)
    {
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        if (state != null) {
            switch (state) {
                case TRUE:
                    return i18n.getString("ComboOption.true"   , "True"   );
                case FALSE:
                    return i18n.getString("ComboOption.false"  , "False"  );
                case UNKNOWN: // "default"
                    return i18n.getString("ComboOption.default", "Default");
            }
        }
        return i18n.getString("ComboOption.default", "Default");
    }

    public static TriState parseTriStateText(Locale locale, String state)
    {
        return ComboOption.parseTriStateText(locale, state, TriState.UNKNOWN);
    }

    public static TriState parseTriStateText(Locale locale, String state, TriState dft)
    {

        // -- initial checks
        if (StringTools.isBlank(state)) {
            return dft;
        } else
        if (state.equalsIgnoreCase(TRISTATE_NAME_TRUE   )) {
            return TriState.TRUE;
        } else
        if (state.equalsIgnoreCase(TRISTATE_NAME_FALSE  )) {
            return TriState.FALSE;
        } else
        if (state.equalsIgnoreCase(TRISTATE_NAME_DEFAULT)) {
            return TriState.UNKNOWN;
        }

        // -- local language checks
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.true"   ,"True"   ))) {
            return TriState.TRUE;
        } else
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.false"  ,"False"  ))) {
            return TriState.FALSE;
        } else
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.default","Default"))) {
            return TriState.UNKNOWN;
        } else
        if (state.equalsIgnoreCase(i18n.getString("ComboOption.unknown","Unknown"))) {
            return TriState.UNKNOWN;
        } else {
            return dft;
        }

    }

    public static ComboOption getTriStateOption(Locale locale, TriState state)
    {
        I18N i18n = I18N.getI18N(ComboOption.class, locale);
        if (state != null) {
            switch (state) {
                case TRUE:
                    return new ComboOption(TRISTATE_NAME_TRUE   , i18n.getString("ComboOption.true"   , "True"   ));
                case FALSE:
                    return new ComboOption(TRISTATE_NAME_FALSE  , i18n.getString("ComboOption.false"  , "False"  ));
                case UNKNOWN:
                    return new ComboOption(TRISTATE_NAME_DEFAULT, i18n.getString("ComboOption.default", "Default"));
            }
        }
        return new ComboOption(TRISTATE_NAME_DEFAULT, i18n.getString("ComboOption.default", "Default"));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String key  = "";
    private String desc = "";

    public ComboOption(String key, String desc) 
    {
        this.key  = StringTools.trim(key);
        this.desc = StringTools.isBlank(desc)? this.key : StringTools.trim(desc);
    }

    public ComboOption(String key) 
    {
        this(key,null);
    }

    public ComboOption(EnumTools.StringLocale enumType, Locale locale) 
    {
        if (enumType != null) {
            this.key  = ((Enum)enumType).name();
            this.desc = enumType.toString(locale);
        }
    }

    // ------------------------------------------------------------------------

    public String getKey() 
    {
        return this.key;
    }

    public String getDescription() 
    {
        return this.desc;
    }

    // ------------------------------------------------------------------------

    public boolean isKey(String k) 
    {
        return this.key.equalsIgnoreCase(StringTools.trim(k));
    }

    public boolean isMatch(String n) 
    {
        String k = StringTools.trim(n);
        return this.key.equalsIgnoreCase(k) || this.desc.equalsIgnoreCase(k);
    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getKey());
        sb.append("|");
        sb.append(this.getDescription());
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------

}
