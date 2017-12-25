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
//  This class provides many String based utilities.
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/04  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

/**
*** Provides various String parsing/format utilities
**/

public enum TriState
    implements EnumTools.IntValue
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Enum value definition */
    UNKNOWN     ( -1, "unknown"  ), // default
    FALSE       (  0, "false"    ),
    TRUE        (  1, "true"     );

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the TriState value for the specified String representation
    **/
    public static TriState parseTriState(String val, TriState dft)
    {
        if (StringTools.isBlank(val)) {
            return dft;
        } else
        if (val.equalsIgnoreCase("unknown") ||
            val.equalsIgnoreCase("default")   ) {
            return TriState.UNKNOWN;
        } else
        if (val.equalsIgnoreCase("true"   ) ||
            val.equalsIgnoreCase("yes"    )   ) {
            return TriState.TRUE;
        } else
        if (val.equalsIgnoreCase("false"  ) ||
            val.equalsIgnoreCase("no"     )   ) {
            return TriState.FALSE;
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the String representation for the specified TriState value
    *** @param state   The TriState value
    *** @param unknown The String value to return for <code>TriState.UNKNOWN</code>
    **/
    public static String toStringValue(TriState state, String unknown)
    {
        if (state != null) {
            switch (state) {
                case TRUE : return "TRUE";
                case FALSE: return "FALSE";
                default   : return unknown;
            }
        } else {
            return unknown;
        }
    }

    /**
    *** Returns the String representation for the specified TriState value
    *** @param state   The TriState value
    **/
    public static String toStringValue(TriState state)
    {
        return TriState.toStringValue(state, "UNKNOWN");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int         value = 0;
    private String      text  = null;

    // ------------------------------------------------------------------------

    /**
    *** Enum Constructor
    **/
    TriState(int v, String t) 
    { 
        this.value = v; 
        this.text  = t; 
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the integer value of this instance
    *** @return The integer value of this instance
    **/
    public int getIntValue()
    { 
        return this.value; 
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the String representation of this instance
    *** @return The String representation of this instance
    **/
    public String toString()
    { 
        return text.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this is the default value (UNKNOWN) or this TriState
    *** @return True if this is the default value (UNKNOWN) or this TriState
    **/
    public boolean isDefault()
    { 
        return this.equals(UNKNOWN);
    }

    /**
    *** Returns true if the value is "Unknown"
    *** @return True if the value is "Unknown"
    **/
    public boolean isUnknown()
    { 
        return this.equals(UNKNOWN); 
    }

    /**
    *** Returns true if the value is "True"
    *** @return True if the value is "True"
    **/
    public boolean isTrue()
    { 
        return this.equals(TRUE); 
    }

    /**
    *** Returns true if the value is "False"
    *** @return True if the value is "False"
    **/
    public boolean isFalse()
    { 
        return this.equals(FALSE); 
    }

}
