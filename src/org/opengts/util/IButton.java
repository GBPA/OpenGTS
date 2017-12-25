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
//  2014/11/30  Martin D. Flynn
//     -Initial release
//  2015/05/03  Martin D. Flynn
//     -Support for handling IDs in reverse/reverse-byte order
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.*;

/**
*** iButton container
**/

public class IButton
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** DisplayFormat Enumeration
    **/
    public enum DisplayFormat implements EnumTools.StringLocale, EnumTools.IntValue {
        DECIMAL    (  0, I18N.getString(IButton.class,"IButton.DisplayFormat.decimal.be","Decimal (Big-Endian)"   )),
        HEX64      (  1, I18N.getString(IButton.class,"IButton.DisplayFormat.hex64.be"  ,"Hex-64 (Big-Endian)"    )),
        HEX48      (  2, I18N.getString(IButton.class,"IButton.DisplayFormat.hex48.be"  ,"Hex-48 (Big-Endian)"    )),
        HEX32      (  3, I18N.getString(IButton.class,"IButton.DisplayFormat.hex32.be"  ,"Hex-32 (Big-Endian)"    )),
        DECIMAL_LE ( 10, I18N.getString(IButton.class,"IButton.DisplayFormat.decimal.le","Decimal (Little-Endian)")),
        HEX64_LE   ( 11, I18N.getString(IButton.class,"IButton.DisplayFormat.hex64.le"  ,"Hex-64 (Little-Endian)" )),
        HEX48_LE   ( 12, I18N.getString(IButton.class,"IButton.DisplayFormat.hex48.le"  ,"Hex-48 (Little-Endian)" )),
        HEX32_LE   ( 13, I18N.getString(IButton.class,"IButton.DisplayFormat.hex32.le"  ,"Hex-32 (Little-Endian)" ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        DisplayFormat(int v, I18N.Text a)           { vv = v; aa = a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isFormate(int fmt)           { return this.getIntValue() == fmt; }
    };

    /**
    *** Gets the DisplayFormat enumeration value from the specified name
    **/
    public static DisplayFormat getDisplayFormatFromName(String stn, DisplayFormat dft)
    {
        if (!StringTools.isBlank(stn)) {
            // -- Big-Endian
            if (stn.equalsIgnoreCase("DECIMAL"   )) { return DisplayFormat.DECIMAL;    } 
            if (stn.equalsIgnoreCase("HEX"       )) { return DisplayFormat.HEX64;      } 
            if (stn.equalsIgnoreCase("HEX64"     )) { return DisplayFormat.HEX64;      } 
            if (stn.equalsIgnoreCase("HEX48"     )) { return DisplayFormat.HEX48;      }
            if (stn.equalsIgnoreCase("HEX32"     )) { return DisplayFormat.HEX32;      }
            // -- Big-Endian
            if (stn.equalsIgnoreCase("DECIMAL_BE")) { return DisplayFormat.DECIMAL;    } 
            if (stn.equalsIgnoreCase("HEX_BE"    )) { return DisplayFormat.HEX64;      } 
            if (stn.equalsIgnoreCase("HEX64_BE"  )) { return DisplayFormat.HEX64;      } 
            if (stn.equalsIgnoreCase("HEX48_BE"  )) { return DisplayFormat.HEX48;      }
            // -- Little-Endian
            if (stn.equalsIgnoreCase("DECIMAL_LE")) { return DisplayFormat.DECIMAL_LE; } 
            if (stn.equalsIgnoreCase("HEX_LE"    )) { return DisplayFormat.HEX64_LE;   } 
            if (stn.equalsIgnoreCase("HEX64_LE"  )) { return DisplayFormat.HEX64_LE;   } 
            if (stn.equalsIgnoreCase("HEX48_LE"  )) { return DisplayFormat.HEX48_LE;   }
            if (stn.equalsIgnoreCase("HEX32_LE"  )) { return DisplayFormat.HEX32_LE;   }
        }
        return dft;
    }

    /**
    *** Gets the String representation of the specified DisplayFormat.
    *** Returns "Unknown" id the specified DisplayFormat is null.
    **/
    public static String GetDisplayFormatDescription(DisplayFormat df, Locale locale)
    {
        if (df != null) {
            return df.toString(locale);
        } else {
            I18N i18n = I18N.getI18N(IButton.class, locale);
            return i18n.getString("IButton.DisplayFormat.unknown", "Unknown");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String ConvertToBigEndianID(String value)
    {
        String Vs = StringTools.trim(value);
        if (!StringTools.isHex(Vs,true)) {
            Print.logError("Invalid iButton-ID hex value: " + Vs);
            return value;
        } else
        if (Vs.length() != 16) {
            Print.logError("Invalid iButton-ID hex length: " + Vs);
            return value;
        } else {
            long LE = StringTools.parseHexLong(Vs,0L); // in Little-Endian format
            long BE = Payload.reverseByteOrder(LE,8); // convert to Big-Endian
            return StringTools.toHexString(BE,64);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* value stored in 64-bit Little-Endian format as provided by the device */
    private long valueLE = 0L; 

    /**
    *** Constructor.
    *** Values provided by iButton devices are in Little-Endian format.
    *** As such, these values are stored within this container in Little-Endian format.
    *** @param value  The 64-bit iButton value (in Little-Endian format)
    **/
    public IButton(long value)
    {
        // -- value assumed to be in Little-Endian format
        this(value, false/*Little-Endian*/);
    }

    /**
    *** Constructor.
    *** Values provided by iButton devices are in Little-Endian format.
    *** As such, these values are stored within this container in Little-Endian format.
    *** If the specified value will be in Big-Endian format, then the second parameter
    *** should specify "true" so that this specified value will be converted into
    *** the internal Little-Endian format.
    *** @param value      The 64-bit iButton value.
    *** @param bigEndian  True if the specified value is in Big-Endian format, false if Little-Endian.
    **/
    public IButton(long value, boolean bigEndian)
    {
        // -- if only a 32-bit or 48-bit Little-Endian value is available, then this constructor 
        // -  should be called as:  IButton ib = new IButton(((v32LE << 8) | 0x01L), false);
        if (bigEndian) {
            // -- convert 64-bit Big-Endian to Little-Endian format
            this.valueLE = Payload.reverseByteOrder(value,8); // 64-bit
        } else {
            // -- save as provided in Little-Endian format
            this.valueLE = value;
        }
    }

    // --------------------------------

    /**
    *** Constructor.
    *** The iButton value displayed on the case is in Big-Endian format.
    *** The iButton value presented as a binary value from the device is in Little-Endian format.
    *** Depending on the origin of the iButton value, the "bigEndian" parameter should be set accordingly.
    **/
    public IButton(String value)
    {
        // -- value assumed to be in Little-Endian format
        this(value, false/*Little-Endian*/);
    }

    /**
    *** Constructor.
    *** The iButton value displayed on the case is in Big-Endian format.
    *** The iButton value presented as a binary value from the device is in Little-Endian format.
    *** Depending on the origin of the iButton value, the "bigEndian" parameter should be set accordingly.
    **/
    public IButton(String value, boolean bigEndian)
    {
        String Vs = StringTools.trim(value);
        if (!StringTools.isHex(Vs,true)) {
            // -- does not contain all hex values
            Print.logError("Invalid iButton-ID hex value: " + Vs);
            this.valueLE = 0L;
        } else
        if (Vs.length() == 16) {
            // -- 64-bit: full length, Little-Endian format
            long ib64    = StringTools.parseHexLong(Vs,0L);
            long ib64LE  = bigEndian? Payload.reverseByteOrder(ib64,8) : ib64;
            this.valueLE = ib64LE;
        } else
        if (Vs.length() == 12) {
            // -- 48-bit: assume missing CRC and FamilyCode (assume FamilyCode 0x01)
            long ib48    = StringTools.parseHexLong(Vs,0L);
            long ib48LE  = bigEndian? Payload.reverseByteOrder(ib48,6) : ib48;
            long ib64LE  = IButton.ResetCRC((ib48LE << 8) | 0x01L); 
            this.valueLE = ib64LE;
        } else
        if (Vs.length() == 8) {
            // -- 32-bit: assume missing CRC, FamilyCode, an 2 MSB bytes of serial#
            long ib32    = StringTools.parseHexLong(Vs,0L);
            long ib32LE  = bigEndian? Payload.reverseByteOrder(ib32,4) : ib32;
            long ib64LE  = IButton.ResetCRC((ib32LE << 8) | 0x01L); 
            this.valueLE = ib64LE;
        } else {
            Print.logError("Invalid iButton-ID hex length: " + Vs);
            this.valueLE = 0L;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance contains a valid iButton tag value.
    *** - FamilyCode must not be zero
    *** - CRC must be valid
    **/
    public boolean isValid()
    {
        // -- check for presence of non-zero FamilyCode
        long V = this.getValue(); // Little-Endian
        if ((V & 0x00000000000000FFL) == 0L) {  // FamilyCode is LE hi-order byte
            // -- FamilyCode not specified
            return false;
        } else
        if (V != ResetCRC(V)) {
            // -- invalid CRC
            return false;
        } else {
            return true;
        }
    }

    /**
    *** Gets the iButton family code
    **/
    public int getFamilyCode()
    {
        long V = this.getValue(); // Little-Endian
        return (int)(V & 0xFFL);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the iButton tag value (Little-Endian format)
    **/
    public long getValue()
    {
        return this.valueLE;
    }

    /**
    *** Gets the iButton tag value
    *** @param bigEndian True to return as a Big-Endian value
    **/
    public long getValue(boolean bigEndian)
    {
        long V = this.getValue();
        if (bigEndian) {
            return Payload.reverseByteOrder(V,8);
        } else {
            return V;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts the iButton tag value to a 64-bit decimal String
    **/
    public String toDecimalString(boolean bigEndian) 
    {
        return String.valueOf(this.getValue(bigEndian));
    }

    /**
    *** Converts the iButton tag value to a 64-bit decimal String (Little-Endian format)
    **/
    public String toDecimalString() 
    {
        return this.toDecimalString(false);
    }

    // --------------------------------

    /**
    *** Converts the iButton tag value to a 64-bit hex String (Big-Endian format)
    *** (this format typically matches the hex value displayed on the iButton device)
    **/
    public String toHexString(boolean bigEndian) 
    {
        long BE = this.getValue(bigEndian);
        return StringTools.toHexString(BE,64).toUpperCase();
    }

    /**
    *** Converts the iButton tag value to a 64-bit hex String (Big-Endian format)
    *** (this format typically matches the hex value displayed on the iButton device)
    **/
    public String toHexString() 
    {
        return this.toHexString(true/*Big-Endian*/);
    }

    // --------------------------------

    /**
    *** Extract the middle 48-bits of the iButton tag value and returns it
    *** as a 48-bit hex String (Big-Endian format).
    **/
    public String toHexString48() 
    {
        return this.toHexString48(true);
    }

    /**
    *** Extract the middle 48-bits of the iButton tag value and returns it
    *** as a 48-bit hex String.
    **/
    public String toHexString48(boolean bigEndian) 
    {
        String H = this.toHexString(bigEndian);
        if (H.length() == 16) {
            // -- expected length, extract middle 48-bit hex
            return H.substring(2,14);
        } else {
            // -- should not occur
            return H; // return as-is
        }
    }

    // --------------------------------

    /**
    *** Extract the middle 32-bits of the iButton tag value and returns it
    *** as a 32-bit hex String (Big-Endian format).
    **/
    public String toHexString32() 
    {
        return this.toHexString32(true);
    }

    /**
    *** Extract the middle 32-bits of the iButton tag value and returns it
    *** as a 32-bit hex String.
    **/
    public String toHexString32(boolean bigEndian) 
    {
        String H = this.toHexString(bigEndian);
        if (H.length() == 16) {
            // -- expected length, extract middle 32-bit hex
            return H.substring(6,14);
        } else {
            // -- should not occur
            return H; // return as-is
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the 64-bit hex String iButton tag value
    **/
    public String toString() 
    {
        return this.toHexString(true);
    }

    /**
    *** Returns the 64-bit hex String iButton tag value
    **/
    public String toString(boolean bigEndian) 
    {
        return this.toHexString(bigEndian);
    }

    /**
    *** Returns the String representation of this iButton value in the specified
    *** DisplayFormat.  If the DisplayFormat is null, returns the 64-bit hex representation.
    **/
    public String toString(DisplayFormat df) 
    {
        if (df != null) {
            switch (df) {
                case DECIMAL : 
                    // -- decimal (Big-Endian) 
                    return this.toDecimalString(true);
                case HEX64   : // preferred
                    // -- 64-bit hex (converted to Big-Endian): "310000179D3A2A01"
                    return this.toHexString(true);
                case HEX48   : // not recommended
                    // -- 48-bit hex (converted to Big-Endian): "0000179D3A2A" (strip hi/lo bytes)
                    return this.toHexString48(true);
                case HEX32   : // not recommended
                    // -- 32-bit hex (converted to Big-Endian): "179D3A2A" (strip hi/lo bytes)
                    return this.toHexString32(true);
                case DECIMAL_LE : 
                    // -- decimal (Little-Endian) as provided by device
                    return this.toDecimalString(false);
                case HEX64_LE   : // not recommended
                    // -- 64-bit hex (Little-Endian): "012A3A9D17000031"
                    return this.toHexString(false);
                case HEX48_LE   : // not recommended
                    // -- 48-bit hex (Little-Endian): "A3A9D170000" (strip hi/lo bytes)
                    return this.toHexString48(false);
                case HEX32_LE   : // not recommended
                    // -- 32-bit hex (Little-Endian): "2A3A9D17" (strip hi/lo bytes)
                    return this.toHexString32(false);
            }
        }
        return this.toHexString(true); // default: 64-bit hex (Big-Endian)
    }

    /** 
    *** Converts the specified iButton value into a String value
    *** @param value The iButton value
    *** @param st    The display format
    *** @return The value converted to the specified output format, or null if the 
    ***         specified display format is null.
    **/
    public static String toString(long value, DisplayFormat st)
    {
        return (new IButton(value)).toString(st);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Calculate iButton ROM-ID CRC

    private static int iButtonCRC[] = {
          0, 94,188,226, 97, 63,221,131,194,156,126, 32,163,253, 31, 65,
        157,195, 33,127,252,162, 64, 30, 95,  1,227,189, 62, 96,130,220,
         35,125,159,193, 66, 28,254,160,225,191, 93,  3,128,222, 60, 98,
        190,224,  2, 92,223,129, 99, 61,124, 34,192,158, 29, 67,161,255,
         70, 24,250,164, 39,121,155,197,132,218, 56,102,229,187, 89,  7,
        219,133,103, 57,186,228,  6, 88, 25, 71,165,251,120, 38,196,154,
        101, 59,217,135,  4, 90,184,230,167,249, 27, 69,198,152,122, 36,
        248,166, 68, 26,153,199, 37,123, 58,100,134,216, 91,  5,231,185,
        140,210, 48,110,237,179, 81, 15, 78, 16,242,172, 47,113,147,205,
         17, 79,173,243,112, 46,204,146,211,141,111, 49,178,236, 14, 80,
        175,241, 19, 77,206,144,114, 44,109, 51,209,143, 12, 82,176,238,
         50,108,142,208, 83, 13,239,177,240,174, 76, 18,145,207, 45,115,
        202,148,118, 40,171,245, 23, 73,  8, 86,180,234,105, 55,213,139,
         87,  9,235,181, 54,104,138,212,149,203, 41,119,244,170, 72, 22,
        233,183, 85, 11,136,214, 52,106, 43,117,151,201, 74, 20,246,168,
        116, 42,200,150, 21, 75,169,247,182,232, 10, 84,215,137,107, 53
    };

    /** 
    *** Recalculate and re-insert CRC into iButton ROM ID
    *** @param ib64LE  64-bit ROM ID in Little-Endian format
    **/
    public static long ResetCRC(long ib64LE)
    {
        // -- CRC = X^8 + X^5 + X^4 + 1
        // -- Maxim Application Note 27:
        // -  "Understanding and Using Cyclic Redundancy Checks with Maxim iButton Products"
        int CRC = 0;
        for (int bi = 0; bi < 7; bi++) {
            int X = (int)((ib64LE >> (bi * 8)) & 0xFFL);
            CRC = iButtonCRC[(CRC ^ X) & 0xFF];
        }
        return (ib64LE & 0x00FFFFFFFFFFFFFFL) | (((long)CRC & 0xFFL) << 56);
    }

    /** 
    *** Recalculate and re-insert CRC into iButton ROM ID
    *** @param ib64LE  64-bit ROM ID in Little-Endian format
    **/
    public static long ResetCRC2(long ib64LE)
    {
        // -- CRC = X^8 + X^5 + X^4 + 1
        // -- Maxim Application Note 27:
        // -  "Understanding and Using Cyclic Redundancy Checks with Maxim iButton Products"
        int CRC = 0;
        for (int bi = 0; bi < 7; bi++) {
            int X = (int)((ib64LE >> (bi * 8)) & 0xFFL);
            for (int t = 0; t < 8; t++) {
                boolean doXOR = (((CRC ^ X) & 1) != 0)? true : false;
                CRC >>= 1;
                if (doXOR) { CRC ^= 0x8C; } // 10001100
                X >>= 1;
            }
        }
        return (ib64LE & 0x00FFFFFFFFFFFFFFL) | (((long)CRC & 0xFFL) << 56);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main debug entry point
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        if (RTConfig.hasProperty("dec")) {
            String ibStr = RTConfig.getString("dec","");
            int p = ibStr.indexOf(",");
            String iBtn  = (p >= 0)? ibStr.substring(0,p).trim() : ibStr;
            String beStr = (p >= 0)? ibStr.substring(p+1).trim() : "";
            boolean bigEndian = true;
            if (StringTools.isBlank(beStr) || beStr.equalsIgnoreCase("LE") || beStr.equalsIgnoreCase("false")) {
                bigEndian = false;
            }
            IButton decBtn = new IButton(StringTools.parseLong(iBtn,0L),bigEndian);
            Print.sysPrintln("Decimal LE : " + decBtn.toString(false) + " (displayed on the iButton case)");
            Print.sysPrintln("Decimal BE : " + decBtn.toString(true ) + " (presented by the iButton in binary form)");
            Print.sysPrintln("Decimal CRC: " + StringTools.toHexString(ResetCRC(decBtn.getValue()),64));
        }

        if (RTConfig.hasProperty("str")) {
            String ibStr = RTConfig.getString("str","");
            int p = ibStr.indexOf(",");
            String iBtn  = (p >= 0)? ibStr.substring(0,p).trim() : ibStr;
            String beStr = (p >= 0)? ibStr.substring(p+1).trim() : "";
            boolean bigEndian = true;
            if (StringTools.isBlank(beStr) || beStr.equalsIgnoreCase("LE") || beStr.equalsIgnoreCase("false")) {
                bigEndian = false;
            }
            IButton strBtn = new IButton(iBtn, bigEndian);
            Print.sysPrintln("String LE : " + strBtn.toString(false) + " (displayed on the iButton case)");
            Print.sysPrintln("String BE : " + strBtn.toString(true ) + " (presented by the iButton in binary form)");
            Print.sysPrintln("String CRC: " + StringTools.toHexString(ResetCRC(strBtn.getValue()),64));
        }
     
    }

}
