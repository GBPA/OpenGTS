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
//  Base64 encoding/decoding
// ----------------------------------------------------------------------------
// Change History:
//  2017/02/02  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;

/**
*** Tire state 
**/

public class TireState
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final int     DFT_MAX_TIRES          = 64; // absolute max number of tires
    private static final int     DFT_MAX_AXLES          = 16;
    private static final int     DFT_TIRES_PER_AXLE     =  4;

    private static       int     MAX_TIRES              = DFT_MAX_TIRES;
    private static       int     MAX_AXLES              = DFT_MAX_AXLES;
    private static       int     TIRES_PER_AXLE         = DFT_TIRES_PER_AXLE;

    private static final char    INDEX_PREFIX_T         = 'T'; // "Txx"
    private static final char    INDEX_PREFIX_L         = 'L'; // "Lax"
    
    private static final boolean SAVE_NUMERIC_STATUS    = false;

    /**
    *** Sets the configures default number of tires per axle
    *** @param tpa The tires-per-axle value (valid values 2,4,6,8)
    **/
    public static void SetTiresPerAxle(int tpa)
    {

        /* constrain tires-per-axle */
        if (tpa <= 0) {
            tpa = DFT_TIRES_PER_AXLE;
        } else
        if (tpa > 6) {
            tpa = DFT_TIRES_PER_AXLE;
        }

        /* round up to next even number */
        if ((tpa & 1) != 0) {
            tpa++;
        }

        /* constrain max tires and max axles */
        TIRES_PER_AXLE = tpa; // 2=32, 4=16, 6=10*, 8=8
        MAX_AXLES      = DFT_MAX_TIRES / tpa; // truncate
        MAX_TIRES      = MAX_AXLES * tpa;

    }

    /**
    *** Gets the configured default number of tires per axle
    *** @return The configured number of tires per axle
    **/
    public static int GetTiresPerAxle()
    {
        return TIRES_PER_AXLE;
    }

    /**
    *** Gets the configured default maximum number of tires
    *** @return The maximum number of supported tires
    **/
    public static int GetMaximumTires()
    {
        return MAX_TIRES;
    }

    /**
    *** Gets the configured default maximum number of axles
    *** @return The maximum number of supported axles
    **/
    public static int GetMaximumAxles()
    {
        return MAX_AXLES;
    }

    /**
    *** Gets the tire index for the specified axle/tire index
    *** @param axleNdx      The axle index (ie. 0..15)
    *** @param axleTireNdx  The index of the tire on the axle (fron left to right) (ie. 0..3)
    *** @return The tire index corresponding to the specified axle/tire index.
    **/
    public static int GetTireIndex(int axleNdx, int axleTireNdx)
    {
        return (axleNdx >= 0)? ((axleNdx * GetTiresPerAxle()) + axleTireNdx) : axleTireNdx;
    }

    /**
    *** Gets the axle index for the specified tire index
    *** @param tireNdx  The tire index
    *** @return The axle index corresponding to the specified tire index
    **/
    public static int GetAxleIndex(int tireNdx)
    {
        return (tireNdx >= 0)? (tireNdx / GetTiresPerAxle()) : -1;
    }

    /**
    *** Gets the axle/tire index for the specified tire index
    *** @param tireNdx  The tire index
    *** @return The tire index on the axle corresponding to the specified tire index
    **/
    public static int GetAxleTireIndex(int tireNdx)
    {
        return (tireNdx >= 0)? (tireNdx % GetTiresPerAxle()) : -1;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final double  PSI_PER_KPA         = 0.14503773773020923;      
    public static final double  KPA_PER_PSI         = 1.0 / PSI_PER_KPA; // 6.89475729316836
    public static final double  KPA_PER_BAR         = 100.0;
    public static final double  BAR_PER_KPA         = 1.0 / KPA_PER_BAR;      
    public static final double  BAR_PER_PSI         = BAR_PER_KPA * KPA_PER_PSI; // 0.06894757293168
    public static final double  PSI_PER_BAR         = PSI_PER_KPA * KPA_PER_BAR; // 14.50377377302092
    public static final double  INVALID_PRESSURE    = -999.0;

    public enum PressureUnits {
        KPA  (1.0         ),
        PSI  (PSI_PER_KPA ),
        BAR  (BAR_PER_KPA );
        // ---
        private double      mm = 1.0;
        PressureUnits(double m) { mm=m; }
        public double  getMultiplier()              { return mm; }
        public double  convertFromKPa(double v)     { return v * mm; }
        public double  convertToKPa(double v)       { return v / mm; }
    };

    /**
    *** Returns true is the specified pressure is valid, false otherwise.
    *** (this method assumes that a zero or negative pressure is invalid)
    **/
    public static boolean IsValidPressure(double P)
    {
        return (!Double.isNaN(P) && (P > 0.0) && (P <= 999.0))? true : false;
    }

    /**
    *** kPa to PSI
    *** @param kPa kPa pressure
    *** @return PSI pressure
    **/
    public static double kPa2PSI(double kPa)
    {
        return kPa * PSI_PER_KPA;
    }

    /**
    *** PSI to kPa
    *** @param psi PSI pressure
    *** @return kPa pressure
    **/
    public static double PSI2kPa(double psi)
    {
        return psi * KPA_PER_PSI;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final double  TEMP_LIMIT_LO       =  -273.15; // degrees C (absolute Kelvin)
    public static final double  TEMP_LIMIT_HI       =   500.0;  // degrees C
    public static final double  INVALID_TEMPERATURE = -9999.0;  // degrees C

    public enum TemperatureUnits {
        F,  // Fahrenheit
        C;  // Celsius (default)
        // ---
        TemperatureUnits() { }
        public boolean isC()                        { return this.equals(C); }
        public boolean isF()                        { return this.equals(F); }
        public double  convertFromC(double c)       { return this.isF()? ((c * 9.0 / 5.0) + 32.0) : c; }
        public double  convertToC(double c)         { return this.isF()? ((c - 32.0) * 5.0 / 9.0) : c; }
    };

    /**
    *** Returns true is the specified temperature is valid, false otherwise.
    *** @param C  The temperature to test
    **/
    public static boolean IsValidTemperature(double C)
    {
        return (!Double.isNaN(C) && (C >= TEMP_LIMIT_LO) && (C <= TEMP_LIMIT_HI))? true : false;
    }

    /**
    *** Fahrenheit to Celsius
    *** @param F Fahrenheit temperature
    *** @return Celsius temperature
    **/
    public static double F2C(double F)
    {
        return (F - 32.0) * 5.0 / 9.0;
    }

    /**
    *** Celsius to Fahrenheit
    *** @param C Celsius temperature
    *** @return Fahrenheit temperature
    **/
    public static double C2F(double C)
    {
        return (C * 9.0 / 5.0) + 32.0;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final long    TIRE_UNDER_PRESSURE     = 0x0001L;  // [p]
    public static final long    TIRE_OVER_PRESSURE      = 0x0002L;  // [P]

    public static final long    TIRE_LOW_TEMPERATURE    = 0x0010L;  // [t] not used
    public static final long    TIRE_HIGH_TEMPERATURE   = 0x0020L;  // [T]

    public static final long    TIRE_SLOW_LEAK          = 0x0100L;  // [l]
    public static final long    TIRE_FAST_LEAK          = 0x0200L;  // [L]

    /**
    *** Parse tire status 
    **/
    public static long ParseTireStatus(String ss)
    {
        long stat = 0L;
        for (char ch : StringTools.trim(ss).toCharArray()) {
            switch (ch) {
                // -- under/over pressure
                case 'p': stat |= TIRE_UNDER_PRESSURE;   break;
                case 'P': stat |= TIRE_OVER_PRESSURE;    break;
                // -- low/high temperature
                case 't': stat |= TIRE_LOW_TEMPERATURE;  break; // not used
                case 'T': stat |= TIRE_HIGH_TEMPERATURE; break;
                // -- slow/fast leak
                case 'l': stat |= TIRE_SLOW_LEAK;        break;
                case 'L': stat |= TIRE_FAST_LEAK;        break;
            }
        }
        return stat;
    }

    /**
    *** Return tire status string
    **/
    public static String GetTireStatusString(long status)
    {
        StringBuffer sb = new StringBuffer();
        // -- under/over pressure
        if ((status & TIRE_UNDER_PRESSURE) != 0L) {
            sb.append("p");
        } 
        if ((status & TIRE_OVER_PRESSURE) != 0L) {
            sb.append("P");
        }
        // -- low/high temperature
        if ((status & TIRE_LOW_TEMPERATURE) != 0L) {
            sb.append("t"); // not used (low tire temperature is never critical)
        }
        if ((status & TIRE_HIGH_TEMPERATURE) != 0L) {
            sb.append("T");
        }
        // -- slow/fast leak
        if ((status & TIRE_SLOW_LEAK) != 0L) {
            sb.append("l");
        }
        if ((status & TIRE_FAST_LEAK) != 0L) {
            sb.append("L");
        }
        // -- return
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final long    SENSOR_SLEEP            = 0x0001L;  // [S]
    public static final long    SENSOR_BATTERY          = 0x0002L;  // [B]
    public static final long    SENSOR_TEMPERATURE      = 0x0004L;  // [T]
    public static final long    SENSOR_PRESSURE         = 0x0008L;  // [P]
    public static final long    SENSOR_ERROR            = 0x0010L;  // [E]

    /**
    *** Parse sensor status 
    **/
    public static long ParseSensorStatus(String ss)
    {
        long stat = 0L;
        for (char ch : StringTools.trim(ss).toCharArray()) {
            switch (ch) {
                case 'S': stat |= SENSOR_SLEEP;       break;
                case 'B': stat |= SENSOR_BATTERY;     break;
                case 'T': stat |= SENSOR_TEMPERATURE; break;
                case 'P': stat |= SENSOR_PRESSURE;    break;
                case 'E': stat |= SENSOR_ERROR;       break;
            }
        }
        return stat;
    }

    /**
    *** Return tire status description
    **/
    public static String GetSensorStatusString(long status)
    {
        StringBuffer sb = new StringBuffer();
        // -- sleep
        if ((status & SENSOR_SLEEP) != 0L) {
            sb.append("S");
        }
        // -- battery
        if ((status & SENSOR_BATTERY) != 0L) {
            sb.append("B");
        }
        // -- high temperature
        if ((status & SENSOR_TEMPERATURE) != 0L) {
            sb.append("T");
        }
        // -- high pressure
        if ((status & SENSOR_PRESSURE) != 0L) {
            sb.append("P");
        }
        // -- general error
        if ((status & SENSOR_ERROR) != 0L) {
            sb.append("E");
        }
        // -- return
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Updates the specified TireState with values from the specified String.
    *** If the specified TireState is null, it will be created and returned if the String 
    *** contains valid parsable data.  May return null if the specified TireState is null 
    *** and the String cannot be parsed. 
    *** If the TireState is created within this method, it will not have had the tire
    *** index set.
    *** @param ts        The TireState that will be updated
    *** @param s         The String containing the tire state information to add to the TireState instance
    *** @param tempOnly  True if the String contains only temperature values
    *** @param pUnits    PressureUnits
    *** @param tUnits    TemperatureUnits
    **/
    public static TireState UpdateTireState(TireState ts, String s, boolean tempOnly,
        PressureUnits pUnits, TemperatureUnits tUnits)
    {
        if (!StringTools.isBlank(s)) {
            // -- Pressure   :  "ActualPress[/PreferredPress][,Temperature][,TireStatus[,SensorStatus[,SensorVolts]]]"
            // -- Temperature:  "Temperature[,TireStatus[,SensorStatus[,SensorVolts]]]"

            /* parse pressure,temperature */
            String PTS[] = StringTools.split(s,',');
            String TPv;  // "ActualPress[/PreferredPress]"
            String TTv;  // "Temperature"
            int    sNdx; // starting index of "TireStatus,..."
            if (tempOnly) {
                // -- "Temperature,..."
                TPv  = null;
                TTv  = (PTS.length > 0)? PTS[0] : null;
                sNdx = 1; // "TireStatus,..." starts at index 1
            } else {
                // -- "ActualPress[/PreferredPress],Temperature,..."
                TPv  = (PTS.length > 0)? PTS[0] : null;
                TTv  = (PTS.length > 1)? PTS[1] : null;
                sNdx = 2; // "TireStatus,..." starts at index 2
            }

            /* parse tire/sensor status */
            String TSv = (PTS.length > (sNdx+0))? PTS[sNdx+0] : null; // TireStatus
            String SSv = (PTS.length > (sNdx+1))? PTS[sNdx+1] : null; // SensorStatus
            String SVv = (PTS.length > (sNdx+2))? PTS[sNdx+2] : null; // SensorVolts

            /* "Actual[/Preferred]" pressure */
            if (!StringTools.isBlank(TPv)) {
                String _P[] = StringTools.split(TPv,'/');
                if (_P.length > 0) {
                    double P = StringTools.parseDouble(_P[0],INVALID_PRESSURE);
                    if (IsValidPressure(P)) {
                        if (ts == null) { ts = new TireState(); }
                        double kPa = (pUnits != null)? pUnits.convertToKPa(P) : P/*kPa*/;
                        ts.setActualPressure(kPa);
                    }
                }
                if (_P.length > 1) {
                    double P = StringTools.parseDouble(_P[1],INVALID_PRESSURE);
                    if (IsValidPressure(P)) {
                        if (ts == null) { ts = new TireState(); }
                        double kPa = (pUnits != null)? pUnits.convertToKPa(P) : P/*kPa*/;
                        ts.setPreferredPressure(kPa);
                    }
                }
            }

            /* "Temperature" */
            if (!StringTools.isBlank(TTv)) {
                double T = StringTools.parseDouble(TTv,INVALID_TEMPERATURE);
                if (IsValidTemperature(T)) {
                    if (ts == null) { ts = new TireState(); }
                    double C = (tUnits != null)? tUnits.convertToC(T) : T/*C*/;
                    ts.setActualTemperature(C);
                }
            }

            /* "TireStatus" */
            if (!StringTools.isBlank(TSv)) {
                ts.setTireStatus(TSv);
            }

            /* "SensorStatus" */
            if (!StringTools.isBlank(SSv)) {
                ts.setSensorStatus(SSv);
            }

            /* "SensorVolts" */
            if (!StringTools.isBlank(SVv)) {
                ts.setSensorVolts(StringTools.parseDouble(SVv,0.0));
            }

        }
        return ts;
    }

    // ------------------------------------------------------------------------

    /**
    *** Create TireState from CANBUS PGN 65268 (8 bytes)
    *** "Tire Condition"
    *** (Sent per tire)
    **/
    public static TireState UpdateTireState_PGN65268(Map<Integer,TireState> tsm, byte pgn65268[])
    {
        if ((tsm != null) && (ListTools.size(pgn65268) >= 8)) {
            // --
            Payload p       = new Payload(pgn65268,false/*LittleEndian*/); // should be LittleEndian?
            int tireLoc     = p.readUInt(1,0,"PGN65268.TireLoc");
            int tirePress   = p.readUInt(1,0,"PGN65268.TirePressure");
            int tireTemp    = p.readUInt(2,0,"PGN65268.TireTemperature");
            int tireStat    = p.readUInt(1,0,"PGN65268.TireCTIStat");
            int tireLeak    = p.readUInt(2,0,"PGN65268.TireLeakage");
            int tireThold   = p.readUInt(1,0,"PGN65268.TireThreshold");
            // -- 
            int axleNdx     = (tireLoc >> 4) & 0x0F; // axle index
            int axleTireNdx = (tireLoc >> 0) & 0x0F; // tire index on axle
            //Print.logInfo("TireLoc="+tireLoc+" Axle="+axleNdx+", Tire="+axleTireNdx);
            Integer tireNdx = new Integer(GetTireIndex(axleNdx, axleTireNdx));
            // --
            double actKPA   = (tirePress <= 0xFA  )? ((double)tirePress / 4.0) : INVALID_PRESSURE;
            double actC     = (tireTemp  <= 0xFAFF)? (((double)tireTemp * 0.03125) - 273.0) : INVALID_TEMPERATURE; 
            // --
            TireState ts    = tsm.get(tireNdx);
            if (ts == null) {
                ts = new TireState(axleNdx, axleTireNdx);
                tsm.put(tireNdx,ts);
            }
            //Print.logInfo(axleNdx+"/"+axleTireNdx+" Press="+actKPA+", Temp="+actC);
            ts.setActualPressure(actKPA);
            ts.setActualTemperature(actC);
            return ts;
        } else {
            return null;
        }
    }

    /**
    *** Create TireState from CANBUS PGN 64578 (5 bytes)
    *** "Tire Condition 2"
    *** (Sent per tire)
    **/
    public static TireState UpdateTireState_PGN64578(Map<Integer,TireState> tsm, byte pgn64578[])
    {
        if ((tsm != null) && (ListTools.size(pgn64578) >= 5)) {
            // --
            Payload p       = new Payload(pgn64578,false/*LittleEndian*/); // should be LittleEndian?
            int tireLoc     = p.readUInt(1,0,"PGN64578.TireLoc");
            int tirePress   = p.readUInt(2,0,"PGN64578.TirePressure");
            int tirePrefP   = p.readUInt(2,0,"PGN64578.TirePreferred");
            // -- 
            int axleNdx     = (tireLoc >> 4) & 0x0F; // axle index
            int axleTireNdx = (tireLoc >> 0) & 0x0F; // tire index on axle
            Integer tireNdx = new Integer(GetTireIndex(axleNdx, axleTireNdx));
            // --
            double actKPA   = (tirePress <= 0xFAFF)? (double)tirePress : INVALID_PRESSURE;
            double prfKPA   = (tirePrefP <= 0xFAFF)? (double)tirePrefP : INVALID_PRESSURE; 
            // --
            TireState ts    = tsm.get(tireNdx);
            if (ts == null) {
                ts = new TireState(axleNdx, axleTireNdx);
                tsm.put(tireNdx,ts);
            }
            //Print.logInfo(axleNdx+"/"+axleTireNdx+" Press="+actKPA+", Pref="+prfKPA);
            ts.setActualPressure(actKPA);
            ts.setPreferredPressure(prfKPA);
            return ts;
        } else {
            return null;
        }
    }

    /**
    *** Create TireState from CANBUS PGN 65282 (7 bytes)
    *** "CPC TTM Data"
    *** (Sent per tire)
    **/
    public static TireState UpdateTireState_PGN65282(Map<Integer,TireState> tsm, byte pgn65282[])
    {
        if ((tsm != null) && (ListTools.size(pgn65282) >= 8)) {
            // -- "TTM" ==> "Truck tire module"
            Payload p       = new Payload(pgn65282,false/*LittleEndian*/); // should be LittleEndian?
            int sysTireID   = p.readUInt(1,0,"PGN65282.SysTireID");     //  0.. 7
            int ttmPress    = p.readUInt(1,0,"PGN65282.TTMPressure");   //  8..15
            int ttmTemp     = p.readUInt(1,0,"PGN65282.TTMTemp");       // 16..23
            int RESERVED_0  = p.readUInt(1,0,"PGN65282.Reserved0");     // 24..31
            int ttmState    = p.readUInt(1,0,"PGN65282.TTMState");      // 32..39
            int ttmAlarm    = p.readUInt(1,0,"PGN65282.TTMAlarm");      // 40..47
            int ttmBattSt   = p.readUInt(1,0,"PGN65282.TTMBattSt");     // 48..55
            // -- 
            int systemID    = (sysTireID >> 0) & 0x03; // 0=True, 1=Trailer
            int tireID      = (sysTireID >> 2) & 0x1F; // range 0..23
            Integer tireNdx = new Integer(tireID);
            // --
            double actKPA   = (ttmPress > 0x00)? ((double)(ttmPress - 1) * 4.706) : INVALID_PRESSURE;
            double actC     = (ttmTemp  > 0x00)? ((double)(ttmTemp - 50) * 1.0  ) : INVALID_TEMPERATURE; 
            // --
            TireState ts    = tsm.get(tireNdx);
            if (ts == null) {
                ts = new TireState().setTireIndex(tireID);
                tsm.put(tireNdx,ts);
            }
            ts.setActualPressure(actKPA);
            ts.setActualTemperature(actC);
            return ts;
        } else {
            return null;
        }
    }

    // ---------------
    // -- other possible PGN's
    // -    65284
    // -    65226, 65227, 65228, 64583, 64579, 64582
    // -    65280(1), 65281(1), 65282(+), 65284(+)

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parses the tire index from the specified String
    *** @param txv  The String containing the tire index information in the format
    ***             "Txx", "Lat" ("xx"=decimal tire index on vehicle, 
    ***             "a"=hex axle index, "t"=hex tire index on axle)
    *** @return The parsed tire index
    **/
    public static int ParseTireIndex(String txv)
    {

        /* nothing to parse? */
        txv = StringTools.trim(txv);
        if (StringTools.isBlank(txv)) {
            // -- invalid String, return invalid index
            return -1;
        }
        char pfx = Character.toUpperCase(txv.charAt(0));

        /* "Lax" */
        if (pfx == INDEX_PREFIX_L) {
            String Lx = txv.substring(1).trim();
            if (Lx.length() >= 2) {
                // -- "Lat" ("a"=axle index, "t"=tire index on axle)
                int axleNdx = StringTools.hexIndex(Lx.charAt(0)); // axle
                int tireNdx = StringTools.hexIndex(Lx.charAt(1)); // tire
                if ((axleNdx >= 0) && (tireNdx >= 0)) {
                    return GetTireIndex(axleNdx, tireNdx);
                }
            }
            return -1;
        }

        /* "Txx" or "xx" */
        if ((pfx == INDEX_PREFIX_T) || Character.isDigit(txv.charAt(0))) {
            String Tx = (pfx == INDEX_PREFIX_T)? txv.substring(1).trim() : txv;
            if ((Tx.length() == 1) || (Tx.length() == 2)) {
                // -- "[T]xx" (xx=tire index on vehicle/trailer)
                int tireNdx = StringTools.parseInt(Tx,-1);
                if (tireNdx < 0) {
                    return -1;
                } else
                if (tireNdx >= MAX_TIRES) {
                    return MAX_TIRES - 1;
                } else {
                    return tireNdx;
                }
            } else
            if (Tx.length() == 3) {
                // -- "[T]aat" (a=axle index, t=tire index on axle)
                int axleNdx = StringTools.parseInt(Tx.substring(0,2),-1);
                int tireNdx = StringTools.parseInt(Tx.substring(2)  ,-1);
                if ((axleNdx >= 0) && (tireNdx >= 0)) {
                    return GetTireIndex(axleNdx, tireNdx);
                }
            }
            return -1;
        }

        return -1;
    }

    /**
    *** Parses the tire states from the specified String and returns them in the specified Map instance.
    *** May return null if the specified Map instance is null, and nothing is parsed from the specified
    *** String.
    *** @param s         The String containing the tire state list information
    *** @param tempOnly  True if the String contains temperature values only
    *** @param pUnits    PressureUnits
    *** @param tUnits    TemperatureUnits
    *** @param map       The map containing the tire index and corresponding TireState
    *** @return The map containing the tire index and corresponding TireState
    **/
    public static Map<Integer,TireState> ParseTireState(String s, boolean tempOnly, 
        PressureUnits pUnits, TemperatureUnits tUnits,
        Map<Integer,TireState> map)
    {

        /* validate String */
        s = s.trim();
        if (StringTools.isBlank(s)) {
            // -- invalid String, return map as-is
            return map;
        }
        char pfx = Character.toUpperCase(s.charAt(0));

        /* pressure/temperature properties */
        if ((pfx == INDEX_PREFIX_T) || (pfx == INDEX_PREFIX_L) || (s.indexOf("=") >= 0)) {
            // -- Pressure   : "T01=25/30,99,P,E,3.5 T02=27,110,p,B,3.4 ..."
            // -- Temperature: "T01=100 T02=103 ..."
            RTProperties rtp = new RTProperties(s);
            for (Object K : rtp.getPropertyKeys()) {
                String Ks = (String)K;            // key  : eg. "L13"
                String Vs = rtp.getString(Ks,""); // value: eg. "25/30,100,P,E,3.5"
                int tNdx = ParseTireIndex(Ks);
                if (tNdx >= 0) {
                    // -- tire index is valid
                    TireState ts = (map != null)? map.get(new Integer(tNdx)) : null;
                    if (ts == null) {
                        // -- new TireState
                        ts = new TireState();
                        ts.setTireIndex(tNdx);
                        // -- save in map
                        if (map == null) { map = new HashMap<Integer,TireState>(); }
                        map.put(new Integer(tNdx), ts);
                    }
                    // -- update TireState
                    TireState.UpdateTireState(ts, Vs, tempOnly, pUnits, tUnits);
                } else {
                    // -- invalid tire index
                }
            }
        } else 
        if (s.indexOf(",") >= 0) {
            // -- "25,27,30,35,..."
            String P[] = StringTools.split(s,',');
            for (int tNdx = 0; (tNdx < P.length) && (tNdx < GetMaximumTires()); tNdx++) {
                String Vs = P[tNdx];
                TireState ts = (map != null)? map.get(new Integer(tNdx)) : null;
                if (ts == null) {
                    // -- new TireState
                    ts = new TireState();
                    ts.setTireIndex(tNdx);
                    // -- save in map
                    if (map == null) { map = new HashMap<Integer,TireState>(); }
                    map.put(new Integer(tNdx), ts);
                }
                // -- update TireState
                TireState.UpdateTireState(ts, Vs, tempOnly, pUnits, tUnits);
            }
        } else {
            // -- unable to recognize format 
        }

        /* return map */
        return map;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the TireState for the specified tire index
    **/
    public static TireState GetTireStateForIndex(Map<Integer,TireState> tsm, int tireNdx)
    {
        if (!ListTools.isEmpty(tsm) && (tireNdx >= 0)) {
            return tsm.get(new Integer(tireNdx));
        }
        return null;
    }

    /**
    *** Gets the TireState for the specified tire index
    **/
    public static TireState GetTireStateForIndex(Map<Integer,TireState> tsm, int axleNdx, int axleTireNdx)
    {
        int tireNdx = GetTireIndex(axleNdx, axleTireNdx);
        return TireState.GetTireStateForIndex(tsm, tireNdx);
    }

    // --------------------------------

    /**
    *** Gets the TireState for the specified tire index
    **/
    public static TireState GetTireStateForIndex(TireState tsa[], int tireNdx)
    {
        if (!ListTools.isEmpty(tsa) && (tireNdx >= 0)) {
            for (TireState ts : tsa) {
                if ((ts != null) && (ts.getTireIndex() == tireNdx)) {
                    return ts;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets a String representation of the combined TireState array
    **/
    public static String GetTireStatePropertyString(Map<Integer,TireState> tsm)
    {
        if (!ListTools.isEmpty(tsm)) {
            // -- "L01=500/500,10 L02_1=500/500,10 ..."
            StringBuffer sb = new StringBuffer();
            for (Integer ti : tsm.keySet()) {
                TireState ts = tsm.get(ti);
                if (TireState.IsValid(ts)) {
                    if (sb.length() > 0) { sb.append(" "); }
                    ts.toKeyString(sb,true,-1/*dftNdx*/);
                    sb.append("=");
                    ts.getPressureString(sb);
                    if (ts.hasActualTemperature()) {
                        sb.append(",");
                        ts.getTemperatureString(sb);
                    }
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** Gets a String representation of the combined TireState array
    **/
    public static String GetTireStatePropertyString(TireState tsa[])
    {
        if (!ListTools.isEmpty(tsa)) {
            // -- "T00_0=500/500,10 T00_1=500/500,10 ..."
            StringBuffer sb = new StringBuffer();
            for (TireState ts : tsa) {
                if (TireState.IsValid(ts)) {
                    if (sb.length() > 0) { sb.append(" "); }
                    ts.toKeyString(sb,true,-1/*dftNdx*/);
                    sb.append("=");
                    ts.getPressureString(sb);
                    if (ts.hasActualTemperature()) {
                        sb.append(",");
                        ts.getTemperatureString(sb);
                    }
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int    tireNdx          = -1;
    private int    axleNdx          = -1;
    private int    axleTireNdx      = -1;

    private double actualPressKPA   = INVALID_PRESSURE;
    private double prefPressKPA     = INVALID_PRESSURE;
    private double temperatureC     = INVALID_TEMPERATURE;

    private long   tireStatus       = 0L;
    private double sensorVolts      = 0.0;
    private long   sensorStatus     = 0L;

    /**
    *** Constructor
    **/
    public TireState()
    {
        super();
        this.clearTireIndex();
        this.clearState();
    }

    /**
    *** Constructor
    **/
    public TireState(int tireNdx)
    {
        super();
        this.setTireIndex(tireNdx);
        this.clearState();
    }

    /**
    *** Constructor
    **/
    public TireState(int axleNdx, int tireNdx)
    {
        super();
        this.setTireIndex(axleNdx, tireNdx);
        this.clearState();
    }
    
    /**
    *** Copy Constructor
    **/
    public TireState(TireState ts)
    {
        super();
        if (ts != null) {
            this.tireNdx        = ts.tireNdx;
            this.axleNdx        = ts.axleNdx;
            this.axleTireNdx    = ts.axleTireNdx;
            this.actualPressKPA = ts.actualPressKPA;
            this.prefPressKPA   = ts.prefPressKPA;
            this.temperatureC   = ts.temperatureC;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Clears the tire index
    **/
    public TireState clearTireIndex()
    {
        this.tireNdx     = -1;
        this.axleNdx     = -1;
        this.axleTireNdx = -1;
        return this;
    }

    /**
    *** Sets the tire index
    **/
    public TireState setTireIndex(int tireNdx)
    {
        this.tireNdx     = tireNdx;
        this.axleNdx     = GetAxleIndex(tireNdx);
        this.axleTireNdx = GetAxleTireIndex(tireNdx);
        return this;
    }

    /**
    *** Sets the tire index
    **/
    public TireState setTireIndex(int axleNdx, int tireNdx)
    {
        if (axleNdx < 0) {
            this.setTireIndex(tireNdx);
        } else {
            this.axleNdx     = axleNdx;
            this.axleTireNdx = tireNdx;
            this.tireNdx     = GetTireIndex(this.axleNdx, this.axleTireNdx);
        }
        return this;
    }

    // --------------------------------

    /**
    *** Returns true if this instance has an axle index
    **/
    public boolean hasAxleIndex()
    {
        return (this.axleNdx >= 0)? true : false;
    }

    /**
    *** Gets the axle index, or -1 if no axle index is defined
    **/
    public int getAxleIndex()
    {
        return this.axleNdx;
    }

    // --------------------------------

    /**
    *** Returns true if this instance has an axle/tire index
    **/
    public boolean hasAxleTireIndex()
    {
        return (this.axleTireNdx >= 0)? true : false;
    }

    /**
    *** Gets the axle/tire index, or -1 if no axle index is defined
    **/
    public int getAxleTireIndex()
    {
        return this.axleTireNdx;
    }

    // --------------------------------

    /**
    *** Returns true if this instance has a tire index
    **/
    public boolean hasTireIndex()
    {
        return (this.tireNdx >= 0)? true : false;
    }

    /**
    *** Gets the axle index, or -1 if no axle index is defined
    **/
    public int getTireIndex()
    {
        return this.tireNdx;
    }

    // ------------------------------------------------------------------------

    /**
    *** Clears the tire pressure/temperature state
    **/
    public TireState clearState()
    {
        this.setActualPressure(INVALID_PRESSURE);
        this.setPreferredPressure(INVALID_PRESSURE); 
        this.setActualTemperature(INVALID_TEMPERATURE); 
        this.setTireStatus(0L);
        this.setSensorStatus(0L);
        this.setSensorVolts(0.0);
        return this;
    }

    /**
    *** Set tire pressure/temperature information
    **/
    public TireState updateState(String s, boolean tempOnly,
        PressureUnits pUnits, TemperatureUnits tUnits)
    {
        TireState.UpdateTireState(this, s, tempOnly, pUnits, tUnits);
        return this;
    }

    /**
    *** Set tire pressure/temperature information from the specified TireState
    **/
    public TireState updateState(TireState ts)
    {
        if (ts != null) {
            // -- actual pressure
            if (ts.hasActualPressure()) {
                this.setActualPressure(ts.getActualPressure());
            }
            // -- preferred pressure
            if (ts.hasPreferredPressure()) {
                this.setPreferredPressure(ts.getPreferredPressure());
            }
            // -- actual temperature
            if (ts.hasActualTemperature()) {
                this.setActualTemperature(ts.getActualTemperature());
            }
        }
        return this;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Actual Pressure from the specified TireState
    *** @param ts   The TireState from which the actual pressure will be returned
    *** @return The actual tire pressure
    **/
    public static double GetActualPressure(TireState ts)
    {
        if ((ts != null) && ts.hasActualPressure()) {
            return ts.getActualPressure();
        } else {
            return TireState.INVALID_PRESSURE;
        }
    }

    /**
    *** Returns true if this instance has a valid actual pressure
    **/
    public boolean hasActualPressure()
    {
        return TireState.IsValidPressure(this.getActualPressure());
    }

    /**
    *** Sets the actual tire pressure (in kPa)
    **/
    public void setActualPressure(double kpa)
    {
        this.actualPressKPA = TireState.IsValidPressure(kpa)? kpa : INVALID_PRESSURE;
    }

    /**
    *** Sets the actual tire pressure
    **/
    public double getActualPressure()
    {
        return this.actualPressKPA;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Preferred Pressure from the specified TireState
    *** @param ts   The TireState from which the preferred pressure will be returned
    *** @return The preferred tire pressure
    **/
    public static double GetPreferredPressure(TireState ts)
    {
        if ((ts != null) && ts.hasPreferredPressure()) {
            return ts.getPreferredPressure();
        } else {
            return TireState.INVALID_PRESSURE;
        }
    }

    /**
    *** Returns true if this instance has a valid preferred pressure
    **/
    public boolean hasPreferredPressure()
    {
        return TireState.IsValidPressure(this.getPreferredPressure());
    }

    /**
    *** Sets the preferred tire pressure (in kPa)
    **/
    public void setPreferredPressure(double kpa)
    {
        this.prefPressKPA = TireState.IsValidPressure(kpa)? kpa : INVALID_PRESSURE;
    }

    /**
    *** Sets the preferred tire pressure (kPa)
    **/
    public double getPreferredPressure()
    {
        return this.prefPressKPA;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ratio of actual pressure divided by preferred pressure, minus one.
    *** Return will be:<br>
    ***     Double.NaN if the actual or preferred pressure is not available.<br>
    ***     < 0 if the pressure is low
    ***     > 0 if the pressure is high
    **/
    public double getPressureRatio(double dftPrefPress)
    {

        /* get actual pressure */
        double actPress = this.getActualPressure();
        if (!TireState.IsValidPressure(actPress)) {
            // -- no actual pressure value
            return Double.NaN;
        }

        /* get preferred pressure */
        double prefPress = this.getPreferredPressure();
        if (!TireState.IsValidPressure(prefPress) || (prefPress <= 0.0)) {
            if (TireState.IsValidPressure(dftPrefPress) && (dftPrefPress > 0.0)) {
                prefPress = dftPrefPress;
            } else {
                // -- no preferred pressure to compare against
                return Double.NaN;
            }
        }
        // -- prefPress is > 0.0 (no divide by zero errors)

        /* pressure ratio */
        return (actPress / prefPress) - 1.0; // ie. (90/100) - 1 = -0.10

    }

    // --------------------------------

    /**
    *** Returns true if the tire pressure is low
    **/
    public boolean isPressureLow(double pct, double dftPrefPress)
    {

        /* get ratio */
        double ratio = this.getPressureRatio(dftPrefPress);
        if (Double.isNaN(ratio)) {
            // -- unable to determine ratio
            return false;
        } else
        if (ratio > 0.0) {
            // -- high pressure
            return false;
        } else
        if (ratio == 0.0) {
            // -- exactly at preferred pressure
            return false;
        }

        /* pressure is low */
        if (Math.abs(ratio) < Math.abs(pct)) {
            // -- but not too low
            return false;
        }

        /* pressure is too low */
        return true;

    }

    /**
    *** Gets the first TireState instance with low pressure
    **/
    public static Collection<TireState> GetLowPressureTireState(Map<Integer,TireState> tsm, double pct, double dftPrefPress)
    {
        if (!ListTools.isEmpty(tsm)) {
            Vector<TireState> list = null;
            for (Integer tsi : tsm.keySet()) {
                TireState ts = tsm.get(tsi);
                if ((ts != null) && ts.isPressureLow(pct,dftPrefPress)) {
                    if (list == null) { list = new Vector<TireState>(); }
                    list.add(ts);
                }
            }
            return list;
        } else {
            return null;
        }
    }

    /**
    *** Gets the first TireState instance with low pressure
    **/
    public static Collection<TireState> GetLowPressureTireState(TireState tsa[], double pct, double dftPrefPress)
    {
        if (!ListTools.isEmpty(tsa)) {
            Vector<TireState> list = null;
            for (TireState ts : tsa) {
                if ((ts != null) && ts.isPressureLow(pct,dftPrefPress)) {
                    if (list == null) { list = new Vector<TireState>(); }
                    list.add(ts);
                }
            }
            return list;
        } else {
            return null;
        }
    }

    // --------------------------------

    /**
    *** Returns true if the tire pressure is low
    **/
    public boolean isPressureHigh(double pct, double dftPrefPress)
    {

        /* get ratio */
        double ratio = this.getPressureRatio(dftPrefPress);
        if (Double.isNaN(ratio)) {
            // -- unable to determine ratio
            return false;
        } else
        if (ratio < 0.0) {
            // -- low pressure
            return false;
        } else
        if (ratio == 0.0) {
            // -- exactly at preferred pressure
            return false;
        }

        /* pressure is high */
        //Print.logInfo("Compare ratio: "+Math.abs(ratio)+" <= "+Math.abs(pct));
        if (Math.abs(ratio) < Math.abs(pct)) {
            // -- but not too high
            return false;
        }

        /* pressure is too high */
        return true;

    }

    /**
    *** Gets the first TireState instance with high pressure
    **/
    public static Collection<TireState> GetHighPressureTireState(Map<Integer,TireState> tsm, double pct, double dftPrefPress)
    {
        if (!ListTools.isEmpty(tsm)) {
            Vector<TireState> list = null;
            for (Integer tsi : tsm.keySet()) {
                TireState ts = tsm.get(tsi);
                if ((ts != null) && ts.isPressureHigh(pct,dftPrefPress)) {
                    if (list == null) { list = new Vector<TireState>(); }
                    list.add(ts);
                }
            }
            return list;
        } else {
            return null;
        }
    }

    /**
    *** Gets the first TireState instance with high pressure
    **/
    public static Collection<TireState> GetHighPressureTireState(TireState tsa[], double pct, double dftPrefPress)
    {
        if (!ListTools.isEmpty(tsa)) {
            Vector<TireState> list = null;
            for (TireState ts : tsa) {
                if ((ts != null) && ts.isPressureHigh(pct,dftPrefPress)) {
                    if (list == null) { list = new Vector<TireState>(); }
                    list.add(ts);
                }
            }
            return list;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Actual Temperature from the specified TireState
    *** @param ts   The TireState from which the temperature will be returned (C)
    *** @return The tire temperature (C)
    **/
    public static double GetActualTemperature(TireState ts)
    {
        if ((ts != null) && ts.hasActualTemperature()) {
            return ts.getActualTemperature();
        } else {
            return TireState.INVALID_TEMPERATURE;
        }
    }

    /**
    *** Returns true if this instance has a valid temperature
    **/
    public boolean hasActualTemperature()
    {
        return IsValidTemperature(this.getActualTemperature());
    }

    /**
    *** Sets the actual tire temperature (in C)
    **/
    public void setActualTemperature(double C)
    {
        this.temperatureC = TireState.IsValidTemperature(C)? C : INVALID_TEMPERATURE;
    }

    /**
    *** Sets the actual tire temperature (in C)
    **/
    public double getActualTemperature()
    {
        return this.temperatureC;
    }

    // --------------------------------

    /**
    *** Returns true if the tire pressure is low
    **/
    public boolean isTemperatureHigh(double highC)
    {
        if (!IsValidTemperature(highC)) {
            return false;
        } else
        if (!this.hasActualTemperature()) {
            return false;
        } else {
            double C = this.getActualTemperature();
            return (C > highC)? true : false;
        }
    }

    /**
    *** Gets the first TireState instance with high pressure
    **/
    public static Collection<TireState> GetHighTemperatureTireState(Map<Integer,TireState> tsm, double highC)
    {
        if (!ListTools.isEmpty(tsm) && IsValidTemperature(highC)) {
            Vector<TireState> list = null;
            for (Integer tsi : tsm.keySet()) {
                TireState ts = tsm.get(tsi);
                if ((ts != null) && ts.isTemperatureHigh(highC)) {
                    if (list == null) { list = new Vector<TireState>(); }
                    list.add(ts);
                }
            }
            return list;
        } else {
            return null;
        }
    }

    /**
    *** Gets the first TireState instance with high temperature
    **/
    public static Collection<TireState> GetHighTemperatureTireState(TireState tsa[], double highC)
    {
        if (!ListTools.isEmpty(tsa) && IsValidTemperature(highC)) {
            Vector<TireState> list = null;
            for (TireState ts : tsa) {
                if ((ts != null) && ts.isTemperatureHigh(highC)) {
                    if (list == null) { list = new Vector<TireState>(); }
                    list.add(ts);
                }
            }
            return list;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance has a tire status
    **/
    public boolean hasTireStatus()
    {
        return (this.tireStatus != 0L)? true : false;
    }

    /**
    *** Gets the tire status
    **/
    public long getTireStatus()
    {
        return this.tireStatus;
    }

    /**
    *** Sets the tire status as a Long valie
    **/
    public void setTireStatus(long stat)
    {
        this.tireStatus = stat;
    }

    /**
    *** Sets the tire status as a String valie
    **/
    public void setTireStatus(String stat)
    {
        String s = StringTools.trim(stat);
        if (!StringTools.isBlank(s)) {
            long TS = Character.isDigit(s.charAt(0))? StringTools.parseLong(s,0L) : ParseTireStatus(s);
            this.setTireStatus(TS);
        } else {
            this.setTireStatus(0L);
        }
    }

    /**
    *** Appends the tire status to the specified StringBuffer
    **/
    public StringBuffer getTireStatusString(StringBuffer sb)
    {
        if (sb == null) {
            sb = new StringBuffer();
        }
        if (!this.hasTireStatus()) {
            // -- nothing appended to StringBuffer
        } else
        if (SAVE_NUMERIC_STATUS) {
            sb.append(this.tireStatus);
        } else {
            sb.append(GetTireStatusString(this.tireStatus));
        }
        return sb;
    }

    /**
    *** Gets the tire status as a String
    **/
    public String getTireStatusString()
    {
        return this.getTireStatusString(new StringBuffer()).toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance has a sensor status
    **/
    public boolean hasSensorStatus()
    {
        return (this.sensorStatus != 0L)? true : false;
    }

    /**
    *** Gets the sensor status
    **/
    public long getSensorStatus()
    {
        return this.sensorStatus;
    }

    /**
    *** Sets the sensor status as a Long valie
    **/
    public void setSensorStatus(long stat)
    {
        this.sensorStatus = stat;
    }

    /**
    *** Sets the sensor status as a String value
    **/
    public void setSensorStatus(String stat)
    {
        String s = StringTools.trim(stat);
        if (!StringTools.isBlank(s)) {
            long SS = Character.isDigit(s.charAt(0))? StringTools.parseLong(s,0L) : ParseSensorStatus(s);
            this.setSensorStatus(SS);
        } else {
            this.setSensorStatus(0L);
        }
    }

    /**
    *** Appends the sensor status to the specified StringBuffer
    **/
    public StringBuffer getSensorStatusString(StringBuffer sb)
    {
        if (sb == null) {
            sb = new StringBuffer();
        }
        if (!this.hasSensorStatus()) {
            // -- nothing appended to StringBuffer
        } else
        if (SAVE_NUMERIC_STATUS) {
            sb.append(this.sensorStatus);
        } else {
            sb.append(GetSensorStatusString(this.sensorStatus));
        }
        return sb;
    }

    /**
    *** Gets the sensor status as a String
    **/
    public String getSensorStatusString()
    {
        return this.getSensorStatusString(new StringBuffer()).toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance has a sensor volts value
    **/
    public boolean hasSensorVolts()
    {
        return (this.sensorVolts > 0.0)? true : false;
    }

    /**
    *** Sets the sensor volts
    **/
    public void setSensorVolts(double volts)
    {
        this.sensorVolts = (volts >= 0.0)? volts : 0.0;
    }

    /**
    *** Sets the sensor volts as a String value
    **/
    public void setSensorVolts(String volts)
    {
        String v = StringTools.trim(volts);
        if (!StringTools.isBlank(v)) {
            double SV = StringTools.parseDouble(v,0.0);
            this.setSensorVolts(SV);
        } else {
            this.setSensorVolts(0.0);
        }
    }

    /**
    *** Gets the sensor volts
    **/
    public double getSensorVolts()
    {
        return this.sensorVolts;
    }

    /**
    *** Appends the sensor volts to the specified StringBuffer
    **/
    public StringBuffer getSensorVoltsString(StringBuffer sb)
    {
        if (sb == null) {
            sb = new StringBuffer();
        }
        if (!this.hasSensorVolts()) {
            // -- nothing appended to StringBuffer
        } else {
            String v = StringTools.format(this.sensorVolts,"0.0");
            sb.append(v);
        }
        return sb;
    }

    /**
    *** Gets the sensor volts as a String
    **/
    public String getSensorVoltsString()
    {
        return this.getSensorVoltsString(new StringBuffer()).toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified TireState is valid
    **/
    public static boolean IsValid(TireState ts)
    {
        return ((ts != null) && ts.isValid())? true : false;
    }

    /**
    *** Returns trus if this instance has a defined valid pressure or temperature
    **/
    public boolean isValid()
    {
        if (this.hasActualPressure()) {
            return true;
        } else
        if (this.hasPreferredPressure()) {
            return true;
        } else
        if (this.hasActualTemperature()) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a user-displayable description of the axle-tire.
    **/
    public static String ToAxleTireString(boolean oneBased, int tireNdx)
    {
        if (tireNdx >= 0) {
            int axleNdx     = GetAxleIndex(tireNdx);
            int axleTireNdx = GetAxleTireIndex(tireNdx);
            StringBuffer sb = new StringBuffer();
            sb.append(StringTools.hexNybble(oneBased?(axleNdx+1):axleNdx));
            sb.append("-");
            sb.append(StringTools.hexNybble(oneBased?(axleTireNdx+1):axleTireNdx));
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** Gets a user-displayable description of the axle-tire.
    **/
    public String toAxleTireString(boolean oneBased, int dftNdx)
    {
        int tNdx = this.hasTireIndex()? this.getTireIndex() : dftNdx;
        return TireState.ToAxleTireString(oneBased, tNdx);
    }

    /**
    *** Gets a user-displayable description of the axle-tire
    **/
    public String toAxleTireString()
    {
        return this.toAxleTireString(true,-1);
    }

    // --------------------------------

    /**
    *** Gets a Key name for this instance, representing the Tire Index
    *** @param sb  The StringBuffer to which the Key name will be appended
    *** @return The StringBuffer containing the Key name
    **/
    private StringBuffer toKeyString(StringBuffer sb, boolean perAxle, int dftNdx)
    {
        // -- tire index
        int tireNdx     = -1;
        int axleNdx     = -1;
        int axleTireNdx = -1;
        if (this.hasTireIndex()) {
            tireNdx     = this.getTireIndex();
            axleNdx     = this.getAxleIndex();
            axleTireNdx = this.getAxleTireIndex();
        } else
        if (dftNdx >= 0) {
            tireNdx     = dftNdx;
            axleNdx     = GetAxleIndex(dftNdx);
            axleTireNdx = GetAxleTireIndex(dftNdx);
        }
        // -- assemble key
        if (sb == null) {
            sb = new StringBuffer();
        }
        if (tireNdx < 0) {
            // -- no index
        } else
        if (perAxle) {
            // -- "L23"
            sb.append(INDEX_PREFIX_L);
            sb.append(StringTools.hexNybble(axleNdx));
            sb.append(StringTools.hexNybble(axleTireNdx));
        } else {
            // -- "T09"
            sb.append(INDEX_PREFIX_T);
            sb.append(StringTools.format(tireNdx,"00"));
        }
        return sb;
    }

    /**
    *** Gets a Key name for this instance, representing the Tire Index
    *** @param dftNdx  The default tire location index if this TireState does not specify a location.
    *** @return The String containing the Key name, in the format "Lax"
    **/
    public String toKeyString(int dftNdx)
    {
        return this.toKeyString(null,true,dftNdx).toString();
    }

    // --------------------------------

    /** 
    *** Gets a String representation of the pressure values in the format "Actual/Preferred".
    *** Returns only the "Actual" pressure if the Preferred pressure is not available.
    **/
    public StringBuffer getPressureString(String prefix, StringBuffer sb)
    {
        if (sb == null) {
            sb = new StringBuffer();
        }
        if (this.hasActualPressure() || this.hasPreferredPressure()) {
            if (prefix != null) {
                sb.append(prefix);
            }
            if (this.hasActualPressure()) {
                String v = StringTools.format(this.getActualPressure(),"0.0");
                if (v.endsWith(".0")) {
                    v = v.substring(0,v.length() - 2);
                }
                sb.append(v);
            }
            if (this.hasPreferredPressure()) {
                sb.append("/");
                String v = StringTools.format(this.getPreferredPressure(),"0.0");
                if (v.endsWith(".0")) {
                    v = v.substring(0,v.length() - 2);
                }
                sb.append(v);
            }
        }
        return sb;
    }

    /** 
    *** Gets a String representation of the pressure values in the format "Actual/Preferred".
    *** Returns only the "Actual" pressure if the Preferred pressure is not available.
    **/
    public StringBuffer getPressureString(StringBuffer sb)
    {
        return this.getPressureString(null, sb);
    }

    /** 
    *** Gets a String representation of the pressure values in the format "Actual/Preferred".
    *** Returns only the "Actual" pressure if the Preferred pressure is not available.
    **/
    public String getPressureString(String prefix)
    {
        return this.getPressureString(prefix,null).toString();
    }

    /** 
    *** Gets a String representation of the pressure values in the format "Actual/Preferred".
    *** Returns only the "Actual" pressure if the Preferred pressure is not available.
    **/
    public String getPressureString()
    {
        return this.getPressureString(null,null).toString();
    }

    // --------------------------------

    /** 
    *** Gets a String representation of the temperature value.
    **/
    public StringBuffer getTemperatureString(String prefix, StringBuffer sb)
    {
        if (sb == null) {
            sb = new StringBuffer();
        }
        if (this.hasActualTemperature()) {
            if (prefix != null) {
                sb.append(prefix);
            }
            String v = StringTools.format(this.getActualTemperature(),"0.0");
            if (v.endsWith(".0")) {
                v = v.substring(0,v.length() - 2);
            }
            sb.append(v);
        }
        return sb;
    }

    /** 
    *** Gets a String representation of the temperature value.
    **/
    public StringBuffer getTemperatureString(StringBuffer sb)
    {
        return this.getTemperatureString(null, sb);
    }

    /** 
    *** Gets a String representation of the temperature value.
    **/
    public String getTemperatureString(String prefix)
    {
        return this.getTemperatureString(prefix,null).toString();
    }

    /** 
    *** Gets a String representation of the temperature value.
    **/
    public String getTemperatureString()
    {
        return this.getTemperatureString(null,null).toString();
    }

    // --------------------------------

    /**
    *** Return a String representation of this instance, in the following format: <br>
    ***     Lat=Pressure[/Preferred][,[Temperature][,[TireStatus][,[SensorStatus][,[SensorVolts]]]]]
    *** Where:<br>
    ***     a            is the Axle index <br>
    ***     t            is the Axle/Tire index <br>
    ***     Pressure     is the actual tire pressure <br>
    ***     Preferred    is the preferred tire pressure (optional) <br>
    ***     Temperature  is the tire temperature (optional) <br>
    ***     TireStatus   is the tire status (optional) <br>
    ***     SensorStatus is the sensor status (optional) <br>
    ***     SensorVolts  is the sensor volts (optional) <br>
    **/
    public StringBuffer toString(StringBuffer sb)
    {
        if (sb == null) {
            sb = new StringBuffer();
        }
        // --
        this.toKeyString(sb,true,-1);
        sb.append("=");
        int c = 0;
        // -- 0: pressure
        this.getPressureString(sb);
        // -- 1: temperature
        if (this.hasActualTemperature()) {
            while (c < 1) { sb.append(","); c++; }
            this.getTemperatureString(sb);
        }
        // -- 2: tire status
        if (this.hasTireStatus()) {
            while (c < 2) { sb.append(","); c++; }
            this.getTireStatusString(sb);
        }
        // -- 3: sensor status
        if (this.hasSensorStatus()) {
            while (c < 3) { sb.append(","); c++; }
            this.getSensorStatusString(sb);
        }
        // -- 4: sensor volts
        if (this.hasSensorVolts()) {
            while (c < 4) { sb.append(","); c++; }
            this.getSensorVoltsString(sb);
        }
        // -- return
        return sb;
    }

    /**
    *** @see toString(StringBuffer)
    **/
    public String toString()
    {
        return this.toString(new StringBuffer()).toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
