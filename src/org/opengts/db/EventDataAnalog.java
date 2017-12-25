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
//  EventData Analog field claculation
// ----------------------------------------------------------------------------
// Change History:
//  2014/10/22  Martin D. Flynn
//     -Extracted from "DCServerConfig.java".
//     -Modified to support a linear interprolated profile.
//     -Modified to accept multiple destination field names [2.5.8-B61]
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

/**
*** EventDataAnalog class<br>
**/
public class EventDataAnalog
{

    public static final boolean USE_CURVEFIT = true;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Cylinder tank level */
    public static final String      PROFILE_CYLINDER_NAME[] = {
        "LevelCylinder",
        "TankLevelCylinder",
        "FuelLevelCylinder",
        "PercentLevelCylinder",
        "SensorLevelCylinder",
    };
    public static final XYPair      PROFILE_CYLINDER_XY[] = {
        // -- The X-value must be scaled to 0..1 (inclusive)
        new XYPair(0.000,0.0000000),
        new XYPair(0.125,0.0721468),
        new XYPair(0.250,0.1955011),
        new XYPair(0.375,0.3425188),
        new XYPair(0.500,0.5000000),
        new XYPair(0.625,0.6574812),
        new XYPair(0.750,0.8044989),
        new XYPair(0.875,0.9278532),
        new XYPair(1.000,1.0000000)
    };
    public static final CurveFit    PROFILE_CYLINDER_CURVEFIT = new CurveFit(PROFILE_CYLINDER_XY);

    /* Linear tank level */
    public static final String      PROFILE_LINEAR_NAME[] = {
        "LevelLinear",
        "TankLevelLinear",
        "FuelLevelLinear",
        "PercentLevelLinear",
        "SensorLevelLinear",
    };
    public static final XYPair      PROFILE_LINEAR_XY[] = {
        // -- The X-value must be scaled to 0..1 (inclusive)
        new XYPair(0.000,0.0000000),
        new XYPair(1.000,1.0000000)
    };
    public static final CurveFit    PROFILE_LINEAR_CURVEFIT = new CurveFit(PROFILE_LINEAR_XY);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Creates an instance of the EventDataAnalog subclass
    *** @param edaClass  The subclass of EventDataAnalog to instantiate (null for EventDataAnalog)
    *** @param aNdx1     The 1-based index of this specific Analog sensor
    *** @param alogStr   The analog configuration profile
    **/
    public static EventDataAnalog newEventDataAnalog(
        Class<? extends EventDataAnalog> edaClass, int aNdx1, 
        String alogStr)
    {
        // -- get/create EventDataAnalog instance
        EventDataAnalog eda = null;
        if (edaClass != null) {
            try {
                MethodAction edaMA = new MethodAction(edaClass,Integer.TYPE,String.class);
                eda = (EventDataAnalog)edaMA.invoke(new Integer(aNdx1), alogStr);
            } catch (Throwable th) {
                Print.logWarn("Unable to create EventDataAnalog subclass: " + th);
                eda = null;
            }
        } else {
            eda = new EventDataAnalog(aNdx1, alogStr);
        }
        // -- return result
        if (EventDataAnalog.isValid(eda)) {
            return eda;
        } else {
            Print.logWarn("Invalid EventDataAnalog["+aNdx1+"] ==> " + alogStr);
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Initializes the DCS EventDataAnalog map. 
    *** Returns null if DCServerConfig not properly specified.
    **/
    public static Map<String,EventDataAnalog> initEventDataAnalogMap(
        DCServerConfig dcsc, String keyPfx,
        Class<? extends EventDataAnalog> edaClass) 
    {

        /* validate arguments */
        if (dcsc == null) {
            return null;
        }

        /* extract/create EventDataAnalog entries */
        Map<String,EventDataAnalog> edaMap = new HashMap<String,EventDataAnalog>();
        int ANALOG_COUNT = EventData.GetAnalogFieldCount(); // the maximum number of analog property entries examined.
        Set<String> dcsPropIDs = dcsc.getPropertyGroupNames();
        for (String dcsPropID : dcsPropIDs) {
            // -- loop through DCS property names
            RTProperties alogRTP = dcsc.getProperties(dcsPropID);
            for (int aNdx = 1; aNdx <= ANALOG_COUNT; aNdx++) {
                // -- for each possible analog sensor
                String alogStr = null;
                if (!StringTools.isBlank(keyPfx)) {
                    String alogKey = keyPfx + aNdx;
                    alogStr = alogRTP.getString(alogKey,null);
                } else {
                    String alogKey[] = DCServerFactory.CONFIG_eventDataAnalog(dcsc.getName(),aNdx);
                    alogStr = alogRTP.getString(alogKey,null);
                }
                if (!StringTools.isBlank(alogStr)) {
                    // -- found "analog.#"
                    EventDataAnalog eda = null;
                    if (edaClass != null) {
                        // -- create custom EventDataAnalog instance
                        try {
                            MethodAction edaMA = new MethodAction(edaClass,Integer.TYPE,String.class);
                            eda = (EventDataAnalog)edaMA.invoke(new Integer(aNdx), alogStr);
                        } catch (Throwable th) {
                            Print.logWarn("Unable to create EventDataAnalog subclass: " + th);
                            eda = null;
                        }
                    } else {
                        // -- create standard EventDataAnalog instance
                        eda = new EventDataAnalog(aNdx, alogStr);
                    }
                    if (EventDataAnalog.isValid(eda)) {
                        eda.setDCSPropertyID(dcsPropID);
                        Print.logDebug("Analog #"+aNdx+": " + eda);
                      //String dcsAlogKey = dcsPropID + "." + alogKey;
                        String dcsAlogKey = dcsPropID + ".analog." + aNdx; // EventDataAnalog Key
                        edaMap.put(dcsAlogKey, eda);
                    }
                } else {
                    // -- "analog.#" not found
                }
            } // loop through "analog.#"
        } // loop though DCS property IDs

        /* return map */
        return edaMap;

    }

    // ------------------------------------------------------------------------

    /**
    *** Get the EventDataAnalog instance for the specified DCServerConfig and Device
    *** @param device   The Device instance
    *** @param dcsc     The DCServerConfig instance
    *** @param aNdx1    The 1-based analog index
    *** @param edaMap   The map of EventDataAnalog instances
    **/
    public static EventDataAnalog getEventDataAnalog(
        Device device, DCServerConfig dcsc, int aNdx1,
        Map<String,EventDataAnalog> edaMap)
    {

        /* invalid EventData, map */
        if (edaMap == null) {
            return null;
        }

        /* get EventDataAnalog */
        if (dcsc != null) {
            String dcsPropID = dcsc.getPropertiesID(device); // grpID: may return "default"
            String dcsAlogKey = dcsPropID + ".analog." + aNdx1; // EventDataAnalog Key
            if (edaMap.containsKey(dcsAlogKey)) {
                return edaMap.get(dcsAlogKey);
            }
        }

        /* return default */
        String dcsAlogKey = DCServerConfig.DEFAULT_PROP_GROUP_ID + ".analog." + aNdx1; // EventDataAnalog Key
        return edaMap.get(dcsAlogKey);

    }

    // ------------------------------------------------------------------------

    /**
    *** Get the EventDataAnalog instance for the specified DCServerConfig and Device.
    *** 
    *** @param evdb     The EventData instance (ignored if this value is null)
    *** @param dcsc     The DCServerConfig instance
    *** @param aNdx1    The 1-based analog index (ignored if this value is invalid)
    *** @param edaMap   The map of EventDataAnalog instances (ignored if this value is null)
    *** @param value    The value to set (ignored if this value is NaN)
    *** @return True if the EventData field value was set, false otherwise.
    **/
    public static boolean convertSaveEventDataAnalogFieldValue(
        EventData evdb, DCServerConfig dcsc, int aNdx1,
        Map<String,EventDataAnalog> edaMap,
        double value)
    {

        /* value is NaN */
        if (Double.isNaN(value)) {
            return false;
        }

        /* invalid EventDataAnalog map */
        if (edaMap == null) {
            return false;
        }

        /* no EventData record */
        if (evdb == null) {
            return false;
        }

        /* get/convert/save */
        EventDataAnalog eda = getEventDataAnalog(
            evdb.getDevice(), dcsc, aNdx1,
            edaMap);
        if (EventDataAnalog.isValid(eda)) {
            double yVal = eda.convert(value);
            return eda.saveEventDataFieldValue(evdb, yVal);
        }

        /* failed */
        return false;

    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the defined Analog headers
    **/
    public static void printEventDataAnalog(Map<String,EventDataAnalog> edaMap, int width)
    {
        if (edaMap != null) {
            for (String key : edaMap.keySet()) {
                // "DCSID.analog.#"
                EventDataAnalog eda = edaMap.get(key);
                int n = eda.getIndex();
                String hdr = StringTools.padRight("Analog Input #"+n,' ',width) + ": ";
                Print.logInfo(hdr + eda);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the analog value based on the specified profile string
    **/
    public static double calculateAnalogValue(String profStr, double inpVal)
    {
        if (!Double.isNaN(inpVal)) {
            EventDataAnalog eda = new EventDataAnalog(0,profStr);
            if (eda.isValid()) {
                return eda.convert(inpVal);
            }
        } 
        return inpVal;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String          dcsPropID = "";
    private int             index     = 0;
    private boolean         isValid   = false;
    private XYPair[]        profile   = null;
    private CurveFit        curveFit  = null;
    private double          scale     = 0.0; // "0.0" means disregard
    private double          gain      = 0.0; // 1.0 / (double)(1L << 12);  // default 12-bit analog
    private double          offset    = 0.0;

    private Vector<DBField> dbFields  = null;

    // --------------------------------

    /**
    *** Gain/Offset Constructor
    **/
    public EventDataAnalog(int ndx, double gain, double offset) 
    {
        this(ndx, gain, offset, (String[])null);
    }

    /**
    *** Gain/Offset Constructor
    **/
    public EventDataAnalog(int ndx, double gain, double offset, String... fieldN) 
    {
        this.index   = ndx;
        this.isValid = true;
        this.gain    = gain;
        this.offset  = offset;
        if (this.isValid()) {
            this.setFieldNames(fieldN);
        }
    }

    // --------------------------------

    /**
    *** XYPair Constructor
    **/
    public EventDataAnalog(int ndx, XYPair api[]) 
    {
        this(ndx, api, (String[])null);
    }

    /**
    *** XYPair Constructor
    **/
    public EventDataAnalog(int ndx, XYPair api[], String... fieldN) 
    {
        this.index   = ndx;
        this.isValid = (ListTools.size(api) >= 2)? true : false;
        this.profile = api;
        if (USE_CURVEFIT) {
            this.curveFit = new CurveFit(this.profile);
            if (!this.curveFit.isValid()) {
                // -- CurveFit discovered a problem with the data
                this.isValid  = false;
                this.curveFit = null;
            }
        }
        if (this.isValid()) {
            this.setFieldNames(fieldN);
        }
    }

    // --------------------------------

    /**
    *** CurveFit Constructor
    **/
    public EventDataAnalog(int ndx, CurveFit cf) 
    {
        this(ndx, cf, (String[])null);
    }

    /**
    *** CurveFit Constructor
    **/
    public EventDataAnalog(int ndx, CurveFit cf, String... fieldN) 
    {
        this.index    = ndx;
        this.isValid  = (cf != null)? cf.isValid() : false;
        this.curveFit = this.isValid? cf : null;
        if (this.isValid()) {
            this.setFieldNames(fieldN);
        }
    }

    // --------------------------------

    /**
    *** String Constructor
    **/
    public EventDataAnalog(int ndx, String gof) 
    {
        this(ndx, gof, 1.0/*gain*/, 0.0/*offset*/);
    }

    /**
    *** String Constructor
    **/
    public EventDataAnalog(int ndx, String gof, double dftGain, double dftOffset) 
    {
        // -- Examples:
        // -    Gain,Offset,Field
        // -      "1,0,fuelLevel"
        // -    Profile,Field
        // -      "[0.0,0.0|0.25,0.20|0.5,0.5|0.75,0.8|1.0,1.0],fuelLevel"
        // -    CannedProfile,Field
        // -      "[PercentLevelCylinder:0.02],fuelLevel"
        // -      "[PercentLevelLinear:0.02],fuelLevel"
        // -    Indirect Reference
        // -      "%deviceAnalogProperty" // TODO:

        /* Analog index */
        this.index = ndx;

        /* trim */
        gof = StringTools.trim(gof);

        /* check CF/CB/LI preference */
        boolean CF    = USE_CURVEFIT;
        boolean CFbig = false;
        if (gof.startsWith("CF[") ||
            gof.startsWith("CF(")   ) {
            // -- CurveFit
            CF = true;
            gof = gof.substring(2);
        } else
        if (gof.startsWith("CB[") ||
            gof.startsWith("CB(")   ) {
            // -- CurveFit
            CF = true;
            CFbig = true;
            gof = gof.substring(2);
        } else
        if (gof.startsWith("LI[") ||
            gof.startsWith("LI(")   ) {
            // -- Linear
            CF = false;
            gof = gof.substring(2);
        }

        /* profile list? */
        if (XYPair.startsWithListChar(gof)) { // "[..."
            // -- parse profile
            int p = XYPair.IndexOfListEndChar(gof,1);
            if (p > 0) {
                // -- assume valid
                this.isValid = true;
                // -- check for canned profiles
                String profStr  = gof.substring(0,p+1); // "[...]" brackets included
                String profName = gof.substring(1,p).trim();    // brackets excluded
                if (StringTools.isBlank(profName)) {
                    // -- empty profile name/points
                    this.profile  = null;
                    this.curveFit = null;
                    this.isValid  = false;
                } else
                if (StringTools.startsWithIgnoreCase(profName,PROFILE_CYLINDER_NAME)) {
                    // -- [PercentLevelCylinder:0.02]
                    this.profile  = PROFILE_CYLINDER_XY;
                    this.curveFit = CF? PROFILE_CYLINDER_CURVEFIT : null;
                    int sp = profStr.indexOf(":");
                    this.scale = (sp >= 0)? StringTools.parseDouble(profStr.substring(sp+1),0.0) : 0.0;
                    this.isValid  = true;
                } else
                if (StringTools.startsWithIgnoreCase(profName,PROFILE_LINEAR_NAME)) {
                    // -- [PercentLevelLinear:0.02]
                    this.profile  = PROFILE_LINEAR_XY;
                    this.curveFit = CF? PROFILE_LINEAR_CURVEFIT : null;
                    int sp = profStr.indexOf(":");
                    this.scale = (sp >= 0)? StringTools.parseDouble(profStr.substring(sp+1),0.0) : 0.0;
                    this.isValid  = true;
                } else
                if (Character.isLetter(profName.charAt(0))) {
                    // -- Unrecognized profile name
                    Print.logWarn("Unrecognized Profile name: " + profName);
                    this.profile  = null;
                    this.curveFit = null;
                    this.isValid  = false;
                } else {
                    // -- standard profile points
                    this.profile  = XYPair.ParseXYPair(profStr,0); // may be null
                    this.curveFit = null;
                    this.isValid  = (ListTools.size(this.profile) >= 2)? true : false;
                }
                // -- CurveFit?
                if (CF && this.isValid() && (this.curveFit == null)) {
                    this.curveFit = CFbig?
                        new CurveFit(this.profile, CurveFit.Precision.Digits20) :
                        new CurveFit(this.profile); // default precision
                    if (!this.curveFit.isValid()) {
                        // -- CurveFit discovered a problem with the data
                        this.isValid  = false;
                        this.curveFit = null;
                    }
                }
                // -- field name(s)
                if (this.isValid()) {
                    String fieldN = gof.substring(p+1).trim();
                    if (fieldN.startsWith(",")) { 
                        // -- skip leading ','
                        fieldN = fieldN.substring(1); 
                    }
                    this.setFieldNames(StringTools.split(fieldN,','));
                }
            } else {
                // -- not valid
                this.isValid  = false;
                // -- no profile points
                this.profile  = new XYPair[0];
                this.curveFit = null;
                // -- set specified default gain/offset
                this.gain     = dftGain;
                this.offset   = dftOffset;
                // -- no field names
                this.setFieldNames((String[])null);
            }
            // -- warning if invalid
            if (!this.isValid()) {
                Print.logWarn("Invalid Analog Profile specification ["+this.index+"]: " + gof);
            }
            return;
        }

        /* Gain/Offset */
        String  v[]  = StringTools.split(gof,',');
        this.isValid = true;
        this.gain    = (v.length >= 1)? StringTools.parseDouble(v[0],dftGain  ) : dftGain;
        this.offset  = (v.length >= 2)? StringTools.parseDouble(v[1],dftOffset) : dftOffset;
        if (!this.isValid() || (v.length < 3)) {
            // -- no field names specified
            this.setFieldNames((String[])null);
        } else
        if (v.length == 3) {
            // -- only one field name specified
            this.setFieldNames(v[2]);
        } else {
            // -- multiple field names specified
            this.setFieldNames(ListTools.toArray(v,2,-1)); // remainder of array
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the DCS Property ID (for informational/debug use only)
    **/
    public void setDCSPropertyID(String dcsPropID)
    {
        this.dcsPropID = StringTools.trim(dcsPropID);
    }

    /**
    *** Gets the DCS Property ID (for informational/debug use only)
    **/
    public String getDCSPropertyID()
    {
        return this.dcsPropID;
    }

    /**
    *** Returns true if this instance has a defined DCS Property ID
    **/
    public boolean hasDCSPropertyID()
    {
        return !StringTools.isBlank(this.dcsPropID);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the Analog index (1-based)
    **/
    public int getIndex() 
    {
        return this.index;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this EventDataAnalog is valid
    **/
    public boolean isValid() 
    {
        return this.isValid;
    }

    /**
    *** Returns true if the specified EventDataAnalog is valid
    **/
    public static boolean isValid(EventDataAnalog eda) 
    {
        return ((eda != null) && eda.isValid())? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the contained XYPair profile array (may be null)
    **/
    public XYPair[] getXYPairProfile()
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
    public CurveFit getCurveFit()
    {
        return this.curveFit;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the Scale
    **/
    public void setScale(double scale) 
    {
        this.scale = scale;
    }

    /** 
    *** Gets the Scale
    **/
    public double getScale() 
    {
        return this.scale;
    }

    /** 
    *** Returns true if Scale is defined
    **/
    public boolean hasScale() 
    {
        return (this.scale != 0.0)? true : false;
    }
    
    /**
    *** Scales the specified value
    **/
    public double scaleValue(double val)
    {
        return (this.hasScale() && !Double.isNaN(val))? (val * this.getScale()) : val;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the Gain
    **/
    public void setGain(double gain) 
    {
        this.gain = gain;
    }

    /** 
    *** Gets the Gain
    **/
    public double getGain() 
    {
        return this.gain;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the Offset
    **/
    public void setOffset(double offset) 
    {
        this.offset = offset;
    }

    /** 
    *** Gets the Offset
    **/
    public double getOffset() 
    {
        return this.offset;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Converts the specified analog value to the proper sensor value
    **/
    public double convert(long value) 
    {
        return this.convert((double)value);
    }

    /** 
    *** Converts the specified analog value to the proper sensor value
    **/
    public double convert(double value)
    {

        /* invalid EventDataAnalog? */
        if (!this.isValid()) {
            Print.logWarn("Invalid EventDataAnalog instance: " + this);
            return value;
        }

        /* NaN? */
        if (Double.isNaN(value)) {
            return value; // return as-is
        }

        /* scale (applied before curvefit/profile) */
        value = this.scaleValue(value);

        /* check for profile curve-fit algorithm */
        CurveFit cf = this.getCurveFit();
        if (CurveFit.isValid(cf)) {
            // -- return value based on profile curve-fit
            return cf.FTN(value);
        }

        /* check for profile linear-interprolation algorithm */
        if (this.profile != null) {
            // -- return value based on linear-interprolation between closest points

            /* get high/low */
            XYPair hi = null;
            XYPair lo = null;
            for (int i = 0; i < this.profile.length; i++) {
                double inpv = this.profile[i].getX();
                if (inpv == value) {
                    // -- exact match
                    hi = this.profile[i];
                    lo = this.profile[i];
                    break;
                } else
                if (inpv >= value) {
                    // -- found range
                    hi = this.profile[i];
                    lo = (i > 0)? this.profile[i - 1] : null;
                    break;
                } else {
                    // -- value < this.profile[i].getX()
                    lo = this.profile[i];
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
                    double inpD  = (value - inpLo) / (inpHi - inpLo);
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
            if (this.profile.length > 0) {
                // -- hi/lo are null (value is greater than the largest input value)
                double outLo = this.profile[this.profile.length - 1].getY();
                return outLo;
            } else {
                // -- no profile available (length == 0)
                return 0.0;
            }

        }

        /* default to simple gain/offset */
        // -- return value based on Gain/Offset
        return (value * this.gain) + this.offset;

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the EventData field name
    **/
    public void setFieldNames(String... fieldN) 
    {

        /* clear */
        if (this.dbFields != null) {
            this.dbFields.clear();
        } else {
            // -- "this.dbFields" is null and already cleared
        }

        /* add field names */
        this.addFieldNames(fieldN);

    }

    /**
    *** Sets the EventData field name
    **/
    public void addFieldNames(String... fieldN) 
    {

        /* pre-validate */
        if (ListTools.isEmpty(fieldN)) {
            // -- nothing to add
            return;
        }

        /* init */
        this._getDBFields(); // makes sure that "this.dbFields" is non-null

        /* loop through names */
        for (String FNcsv : fieldN) {
            // -- blank/null?
            if (StringTools.isBlank(FNcsv)) {
                continue;
            }
            // -- parse each comma-separated name
            for (String fn : StringTools.split(FNcsv,',')) {
                DBField dbf = EventData.getFactory().getField(fn);
                if (dbf != null) {
                    // -- foubd DBField for name
                    Object val = this._getValueObject(dbf.getTypeClass(), 0.0);
                    if (val != null) {
                        // -- value can be converted, save DBField
                        this.dbFields.add(dbf);
                    } else {
                        // -- value can not be converted
                        Print.logError("**** EventData field type not supported: " + fn);
                    }
                } else {
                    // -- name not found
                    Print.logError("**** EventData analog field does not exist: " + fn);
                }
            }
        }

    }

    /**
    *** Gets an array of field names (returns an empty array if no fields are defined)
    **/
    public String[] getFieldNames() 
    {

        /* no fields? */
        if (ListTools.isEmpty(this.dbFields)) {
            return new String[0];
        }

        /* create/return array of field names */
        java.util.List<DBField> dbfList = this._getDBFields();
        java.util.List<String> fnList = new Vector<String>();
        for (DBField dbf : dbfList) {
            fnList.add(dbf.getName());
        }
        return fnList.toArray(new String[fnList.size()]);

    }

    /**
    *** Returns true this instance has the specified field name defined
    **/
    public boolean hasFieldName(String fieldN) 
    {
        return this._hasDBField(fieldN);
    }

    /**
    *** Returns true if this instance defines any fields
    **/
    public boolean hasFields()
    {
        return this._hasDBFields();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the DBField instance for the current EventData field name
    **/
    private java.util.List<DBField> _getDBFields() 
    {
        if (this.dbFields == null) {
            this.dbFields = new Vector<DBField>();
        }
        return this.dbFields;
    }

    /** 
    *** Returns true if this instance contains 1 or more DBFields
    **/
    private boolean _hasDBFields()
    {
        return !ListTools.isEmpty(this.dbFields)? true : false;
    }

    /**
    *** Gets the DBField for the specified field name.
    *** Returns null if the specified field name is not defined for the EvewntDataAnalog instance.
    **/
    private DBField _getDBField(String fieldN)
    {

        /* no specified field name? */
        if (StringTools.isBlank(fieldN)) {
            return null;
        }

        /* no fields? (test "this.dbFields" directory to prevent side-effect of creating list) */
        if (ListTools.isEmpty(this.dbFields)) {
            return null;
        }

        /* loop through fields for matching field name (case-sensitive) */
        for (DBField dbf : this._getDBFields()) {
            if (fieldN.equals(dbf.getName())) {
                return dbf; // exact case-sensitive match
            }
        }

        /* not found */
        return null;

    }

    /**
    *** Returns true this instance has the specified field name defined
    **/
    private boolean _hasDBField(String fieldN) 
    {
        return (this._getDBField(fieldN) != null)? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Converts/Gets the Object value for the specified field name.
    *** Returns null if the specified field name is not defined for the EvewntDataAnalog instance.
    **/
    public Object getValueObject(String fieldN, double value) 
    {
        DBField dbf = this._getDBField(fieldN);
        if (dbf != null) {
            return this._getValueObject(dbf.getTypeClass(), value);
        } else {
            return null;
        }
    }

    /**
    *** Gets the Object value for the specified object type
    **/
    private Object _getValueObject(Class<?> dbfc, double value) 
    {
        if (dbfc == null) {
            return null;
        } else
        if (dbfc == String.class) {
            return String.valueOf(value);
        } else
        if ((dbfc == Integer.class) || (dbfc == Integer.TYPE)) {
            return new Integer((int)value);
        } else
        if ((dbfc == Long.class)    || (dbfc == Long.TYPE   )) {
            return new Long((long)value);
        } else
        if ((dbfc == Float.class)   || (dbfc == Float.TYPE  )) {
            return new Float((float)value);
        } else
        if ((dbfc == Double.class)  || (dbfc == Double.TYPE )) {
            return new Double(value);
        } else
        if ((dbfc == Boolean.class) || (dbfc == Boolean.TYPE)) {
            return new Boolean(value != 0.0);
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the EventData field to the specified value for the specified EventData instance
    **/
    public boolean saveEventDataFieldValue(EventData evdb, double value) 
    {

        /* no EventData instance? */
        if (evdb == null) {
            // -- null EventData (unlikely)
            Print.logWarn("Null EventData instance specified");
            return false;
        }
        String adID = evdb.getAccountID() + "/" + evdb.getDeviceID();

        /* check for NaN */
        if (Double.isNaN(value)) {
            // -- TODO: should we set value=0.0?
            Print.logWarn("("+adID+") NaN value specified, ignored ...");
            return false;
        }

        /* set EventData fields */
        boolean allOk = true;
        if (this._hasDBFields()) {
            for (DBField dbf : this._getDBFields()) {
                Object objVal = this._getValueObject(dbf.getTypeClass(), value);
                if (objVal != null) {
                    // -- set EventData field
                    String fn = dbf.getName();
                    boolean ok = evdb.setFieldValue(fn, objVal);
                    Print.logDebug("("+adID+") AnalogField["+this.getIndex()+"] Set: "+fn+" ==> "+(ok?evdb.getFieldValue(fn):"n/a"));
                    if (allOk && !ok) { allOk = false; }
                } else {
                    // -- unable to convert to Object (unlikely)
                    Print.logWarn("("+adID+") AnalogField["+this.getIndex()+"] Inconvertable value: "+value+" ["+StringTools.className(dbf.getTypeClass())+"]");
                    allOk = false;
                }
            }
        } else {
            Print.logWarn("("+adID+") No Analog fields defined");
            allOk = false;
        }
        
        /* return success */
        return allOk;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    **/
    public String toString() 
    {
        StringBuffer sb = new StringBuffer();
        if (this.hasDCSPropertyID()) {
            sb.append("[");
            sb.append(this.getDCSPropertyID());
            sb.append("] ");
        }
        sb.append("index="  ).append(this.getIndex());
        if (this.hasCurveFit()) {
            sb.append(" curveFit=");
            if (this.profile != null) {
                XYPair.ToListString(this.profile,sb);
            } else {
                sb.append("na");
            }
        } else
        if (this.profile != null) {
            sb.append(" profile=");
            XYPair.ToListString(this.profile,sb);
        } else {
            sb.append(" gain="  ).append(this.getGain());
            sb.append(" offset=").append(this.getOffset());
        }
        if (this._hasDBFields()) {
            sb.append(" field=" );
            int c = 0;
            for (DBField dbf : this._getDBFields()) {
                if (c++ > 0) { sb.append(","); }
                sb.append(dbf.getName());
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        //RTConfig.setCommandLineArgs(argv);
        DBConfig.cmdLineInit(argv,true);  // main

        /* args */
        String  profileStr = RTConfig.getString( "profile"  , null);
        double  value      = RTConfig.getDouble( "value"    ,  0.0); // value of X
        double  scale      = RTConfig.getDouble( "scale"    ,  0.0);
        int     perfCount  = RTConfig.getInt(    "perfCount",  100); // 5000;
        boolean test       = RTConfig.getBoolean("test"     , false);

        /* profile specified? */
        if (StringTools.isBlank(profileStr)) {
            Print.sysPrintln("No profile specified");
            System.exit(1);
        }

        // ---------------------
        double newVal = EventDataAnalog.calculateAnalogValue(profileStr, value) ;
        EventDataAnalog eda = new EventDataAnalog(1,profileStr);
        Print.sysPrintln("Value: " + value + " ==> " + newVal);
        System.exit(0);

    }

}
