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
//  Determine Battery-Level from Battery-Voltage
// ----------------------------------------------------------------------------
// Change History:
//  2014/10/22  Martin D. Flynn
//     -Initial Release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

/**
*** BatteryLevelProfile class<br>
**/
public class BatteryLevelProfile
{

    private static final boolean    USE_CURVEFIT            = true;

    private static final double     DEFAULT_BATTERY_LEVEL   = 0.0;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private XYPair[]        profile  = null; // invalid
    private CurveFit        curveFit = null; // invalid
    private double          loVolts  = -1.0; // invalid
    private double          hiVolts  = -1.0; // invalid

    // --------------------------------

    /**
    *** Low/High Volt Constructor
    **/
    public BatteryLevelProfile(double loV, double hiV) 
    {
        this._setVoltRange(loV, hiV);
        this.logInvalidWarning("Low/High Voltage Range");
    }

    /**
    *** XYPair Constructor
    **/
    public BatteryLevelProfile(XYPair xyProf[]) 
    {
        if (ListTools.size(xyProf) >= 2) {
            if (USE_CURVEFIT) {
                CurveFit cf = new CurveFit(xyProf);
                if (cf.isValid()) {
                    // -- valid CurveFit
                    this.curveFit = cf;
                    this.profile  = xyProf;
                }
            } else {
                // -- linear profile only
                this.curveFit = null;
                this.profile  = xyProf;
            }
        }
        this.logInvalidWarning("XYPair[] Profile");
    }

    /**
    *** CurveFit Constructor
    **/
    public BatteryLevelProfile(CurveFit cf) 
    {
        if ((cf != null) && cf.isValid()) {
            this.curveFit = cf;
        } else {
            this.curveFit = null;
        }
        this.logInvalidWarning("CurveFit Specification");
    }

    /**
    *** String Constructor
    **/
    public BatteryLevelProfile(String blpStr, double dftLoV, double dftHiV) 
    {
        // -- Examples:
        // -    LowVolts,HighVolts
        // -      "3.4,4.8"
        // -    Profile
        // -      "[3.4,0.0|3.8,0.286|4.2,0.571|4.5,0.786|4.8,1.0]"

        /* trim */
        blpStr = StringTools.trim(blpStr);

        /* check CF/LI preference */
        boolean CF = USE_CURVEFIT;
        if (blpStr.startsWith("CF[") ||
            blpStr.startsWith("CF(")   ) {
            // -- CurveFit
            CF = true;
            blpStr = blpStr.substring(2);
        } else
        if (blpStr.startsWith("LI[") ||
            blpStr.startsWith("LI(")   ) {
            // -- Linear
            CF = false;
            blpStr = blpStr.substring(2);
        }

        /* parse BatteryLevelProfile String */
        if (StringTools.isBlank(blpStr)) {
            // -- no profile specification, use specified default
            this._setVoltRange(dftLoV, dftHiV); // may be invalid
        } else
        if (XYPair.startsWithListChar(blpStr)) { // "[..."
            // -- parse profile specification
            XYPair xyProf[] = XYPair.ParseXYPair(blpStr,0); // may be null if invalid
            if ((ListTools.size(xyProf) >= 2) && CF) {
                // -- parse as CurveFit
                CurveFit cf = new CurveFit(xyProf);
                if (cf.isValid()) {
                    // -- valid CurveFit
                    this.profile  = xyProf;
                    this.curveFit = cf;
                } else {
                    // -- invalid CurveFit, instance is invalid
                }
            } else {
                // -- invalid Profile, instance is invalid
            }
        } else {
            // -- default to low/high voltage range
            // -  PERCENT = (volts - LOW_VOLTAGE) / (HIGH_VOLTAGE - LOW_VOLTAGE);
            String v[] = StringTools.split(blpStr,',');
            if (v.length >= 2) {
                double loV = StringTools.parseDouble(v[0], -1.0);
                double hiV = StringTools.parseDouble(v[1], -1.0);
                this._setVoltRange(loV, hiV); // may be invalid
            } else {
                // -- invalid low/high voltage range, instance is invalid
            }
        }

        /* warn if invalid */
        this.logInvalidWarning(blpStr);

    }

    /**
    *** String Constructor
    **/
    public BatteryLevelProfile(String blpStr) 
    {
        this(blpStr, -1.0, -1.0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance is valid
    **/
    public boolean isValid() 
    {
        if (this.hasCurveFit()) {
            return true;
        } else 
        if (this.hasProfile()) {
            return true;
        } else 
        if (this.hasVoltageRange()) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
    *** Returns true if the specified instance is valid
    **/
    public static boolean isValid(BatteryLevelProfile blp)
    {
        return (blp != null)? blp.isValid() : false;
    }

    /**
    *** Logs a warning if this instance is invalid
    **/
    public void logInvalidWarning(String blpStr)
    {
        if (!this.isValid()) {
            Print.logWarn("Invalid BatteryLevelProfile specification: " + blpStr);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the voltage range
    **/
    private boolean _setVoltRange(double loV, double hiV)
    {
        double LV = Math.min(loV,hiV);
        double HV = Math.max(loV,hiV);
        if ((LV >= 0.0) && (HV > LV)) {
            this.loVolts = LV;
            this.hiVolts = HV;
            return true;
        } else {
            this.loVolts = -1.0;
            this.hiVolts = -1.0;
            return false;
        }
    }

    /**
    *** Returns true if this instance contains a valid voltage range
    **/
    public boolean hasVoltageRange()
    {
        if ((this.loVolts >= 0.0) && (this.hiVolts > this.loVolts)) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance has a defined XYPair profile
    **/
    public boolean hasProfile()
    {
        return (ListTools.size(this.getProfile()) >= 2)? true : false;
    }

    /**
    *** Gets the contained XYPair profile
    **/
    private XYPair[] getProfile()
    {
        return this.profile;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance has a defined CurveFit
    **/
    public boolean hasCurveFit()
    {
        return CurveFit.isValid(this.getCurveFit());
    }

    /**
    *** Gets the contained CurveFit instance (may be null)
    **/
    private CurveFit getCurveFit()
    {
        return this.curveFit;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance 
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if (this.hasCurveFit()) {
            sb.append("CurveFitProfile: ");
            this.getCurveFit().toString(sb,"0.0000");
        } else 
        if (this.hasProfile()) {
            sb.append("LinearProfile: ");
            XYPair.ToListString(this.getProfile(),sb);
        } else 
        if (this.hasVoltageRange()) {
            sb.append("VoltageRange: ");
            sb.append(this.loVolts);
            sb.append(",");
            sb.append(this.hiVolts);
        } else {
            sb.append("Invalid");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Converts the specified battery voltage to the proper battery percent
    **/
    public double getLevelFromVolts(double V)
    {

        /* NaN? */
        if (Double.isNaN(V)) {
            // -- invalid voltage, return default battery level
            return DEFAULT_BATTERY_LEVEL;
        }

        /* check for profile curve-fit algorithm */
        if (this.hasCurveFit()) {
            // -- return percent based on profile curve-fit
            CurveFit cf = this.getCurveFit(); // guaranteed valid
            return cf.FTN(V);
        }

        /* check for profile linear-interprolation algorithm */
        if (this.hasProfile()) {
            // -- return percent based on linear-interprolation between closest points
            XYPair xyProf[] = this.getProfile(); // guaranteed valid

            /* get high/low entries */
            XYPair hi = null;
            XYPair lo = null;
            for (int i = 0; i < xyProf.length; i++) {
                double inpv = xyProf[i].getX();
                if (inpv == V) {
                    // -- exact match
                    hi = xyProf[i];
                    lo = xyProf[i];
                    break;
                } else
                if (inpv >= V) {
                    // -- found range
                    hi = xyProf[i];
                    lo = (i > 0)? xyProf[i - 1] : null;
                    break;
                } else {
                    // -- voltage < xyProf[i].getX()
                    lo = xyProf[i];
                    // continue;
                }
            }

            /* linear interprolate between hi/lo */
            if ((hi != null) && (lo != null)) {
                // -- interprolate
                double inpHi = hi.getX();
                double inpLo = lo.getX();
                if (inpHi != inpLo) {
                    // -- hi/lo differ, interprolate
                    double inpD  = (V - inpLo) / (inpHi - inpLo);
                    double outHi = hi.getY();
                    double outLo = lo.getY();
                    return outLo + (inpD * (outHi - outLo));
                } else {
                    // -- hi/lo are the same
                    double outLo = lo.getY();
                    return outLo;
                }
            } else
            if (lo != null) {
                // -- hi is null
                double outLo = lo.getY();
                return outLo;
            } else 
            if (hi != null) {
                // -- lo is null
                double outHi = hi.getY();
                return outHi;
            } else
            if (xyProf.length > 0) {
                // -- hi/lo are null (voltage is greater than the largest input value)
                double outLo = xyProf[xyProf.length - 1].getY();
                return outLo;
            } else {
                // -- no profile available (length == 0)
                return DEFAULT_BATTERY_LEVEL; // 0.0
            }

        }

        /* default to simple voltage range percent */
        if (this.hasVoltageRange()) {
            // -- use voltage range linear interprolation
            if (V < this.loVolts) {
                // -- can't have less than 0%
                return 0.0; // 0%
            } else
            if (V > this.hiVolts) {
                // -- excessive voltage, assume 100%
                return 1.0; // 100%
            } else {
                // -- linear interprolation
                double pct = (V - this.loVolts) / (this.hiVolts - this.loVolts);
                return pct; // 0.0 <= pct <= 1.0
            }
        }

        /* invalid BatteryLevelProfile */
        Print.logWarn("Invalid BatteryLevelProfile instance: " + this);
        return DEFAULT_BATTERY_LEVEL;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
