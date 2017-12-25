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
//  Fuel level profile
// ----------------------------------------------------------------------------
// Change History:
//  2014/06/29  Martin D. Flynn
//     -Initial release (extracted from DCServerConfig.java)
//  2014/10/22  Martin D. Flynn
//     -Initial CurveFit support
//     -Added additional custom profiles with different scales.
//  2015/05/03  Martin D. Flynn
//     -Adjustment for sensor-heights that are shorter than the tank-height.
//  2016/01/04  Martin D. Flynn
//     -Fixed fuel level profile validation check [2.6.1-B34]
//  2016/06/10  Martin D. Flynn
//     -Fixed fuel level profile validation check (again) [2.6.2-B53]
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;

import org.opengts.util.*;

public class FuelLevelProfile
{

    // ------------------------------------------------------------------------

    private static /*final*/   boolean ENABLE_CURVEFIT      = true;

    private static final       String  PROP_FuelLevelProfile_enableCurveFit = "FuelLevelProfile.enableCurveFit";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Empty profile */
    public  static String      FLP_NONE_NAME                = "NONE";
    public  static String      FLP_NONE_ID                  = FLP_NONE_NAME;

    /* Linear profile */
    public  static String      FLP_LINEAR_NAME              = "LINEAR";
    public  static String      FLP_LINEAR_ID_percent        = FLP_LINEAR_NAME;
    private static double      FLP_LINEAR_SCALE_percent     = 1.0; // percentage only
    private static ProfileItem FLP_LINEAR_GRAPH[]           = {
        // LINEAR:1.0:0,0|100,100
        new ProfileItem(0.000,0.0000000),
        new ProfileItem(1.000,1.0000000)
    };
    private static FuelLevelProfile FLP_LINEAR_percent = new FuelLevelProfile(
        FLP_LINEAR_ID_percent, FLP_LINEAR_SCALE_percent, FLP_LINEAR_GRAPH, 
        I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear.percent","Linear Profile (measured percent)"));

    /* Cylinder profile (measured percent) */
    // double A = ((Math.PI/2.0) - Math.asin(1.0-(2.0*E)) - (1.0-(2.0*E)) * Math.sqrt((2.0*E)*(2.0-(2.0*E)))) / Math.PI;
    public  static String      FLP_CYLINDER_NAME            = "CYLINDER";
    public  static String      FLP_CYLINDER_ID_percent      = FLP_CYLINDER_NAME;
    private static double      FLP_CYLINDER_SCALE_percent   = 1.0; // percentage only
    private static ProfileItem FLP_CYLINDER_GRAPH[]         = {
        // CYLINDER:1.0:0.000,0.0000|0.125,0.0721|0.250,0.1955|0.375,0.3425|0.500,0.5000|0.625,0.6575|0.750,0.8045|0.875,0.9279|1.000,1.0000
        // CYLINDER:1.0:0,0|12,7|25,20|38,34|50,50|63,66|75,80|88,93|100,100
        // CYLINDER:1.0
        new ProfileItem(0.000,0.0000000),
        new ProfileItem(0.125,0.0721468),
        new ProfileItem(0.250,0.1955011),
        new ProfileItem(0.375,0.3425188),
        new ProfileItem(0.500,0.5000000),
        new ProfileItem(0.625,0.6574812),
        new ProfileItem(0.750,0.8044989),
        new ProfileItem(0.875,0.9278532),
        new ProfileItem(1.000,1.0000000)
    };
    private static FuelLevelProfile FLP_CYLINDER_percent = new FuelLevelProfile(
        FLP_CYLINDER_ID_percent, FLP_CYLINDER_SCALE_percent, FLP_CYLINDER_GRAPH, 
        I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.cylinder.percent","Horizontal Cylinder (measured percent)"));

    /* Generic/Custom profile */
    public  static String FLP_PROFILE_NAME  = "PROFILE";
    public  static String FLP_PROFILE_ID    = FLP_PROFILE_NAME;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final HashMap<String,FuelLevelProfile> FuelLevelProfileMap = new HashMap<String,FuelLevelProfile>();

    /* static initialization of custom profiles */
    static {
        // -- Linear
        AddFuelLevelProfile(FLP_LINEAR_NAME  +".400m",400.0,FLP_LINEAR_GRAPH  ,I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear.400mm"  ,"Linear Profile (400mm sensor)"     ));
        AddFuelLevelProfile(FLP_LINEAR_NAME  +".500m",500.0,FLP_LINEAR_GRAPH  ,I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear.500mm"  ,"Linear Profile (500mm sensor)"     ));
        AddFuelLevelProfile(FLP_LINEAR_NAME  +".600m",600.0,FLP_LINEAR_GRAPH  ,I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear.600mm"  ,"Linear Profile (600mm sensor)"     ));
        // -- Cylinder
        AddFuelLevelProfile(FLP_CYLINDER_NAME+".400m",400.0,FLP_CYLINDER_GRAPH,I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.cylinder.400mm","Horizontal Cylinder (400mm sensor)"));
        AddFuelLevelProfile(FLP_CYLINDER_NAME+".500m",500.0,FLP_CYLINDER_GRAPH,I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.cylinder.500mm","Horizontal Cylinder (500mm sensor)"));
        AddFuelLevelProfile(FLP_CYLINDER_NAME+".600m",600.0,FLP_CYLINDER_GRAPH,I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.cylinder.600mm","Horizontal Cylinder (600mm sensor)"));
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Convenience for checking the FuelLevelProfile name "NONE"
    *** @return True if the specified name is equivalent to "NONE"
    **/
    public static boolean IsNone(String flpn)
    {
        String n = StringTools.trim(flpn).toUpperCase();
        if (StringTools.isBlank(n)) {
            // -- is blank
            return true; // default to NONE
        } else
        if (n.equals(FLP_NONE_NAME)) {
            // -- is "NONE"
            return true;
        } else 
        if (n.startsWith(FLP_NONE_NAME+":")) {
            // -- is "NONE:..."
            return true;
        } else {
            // -- else not NONE
            return false;
        }
    }

    /**
    *** Convenience for checking the FuelLevelProfile name "LINEAR"
    *** @return True if the specified name is equivalent to "LINEAR"
    **/
    public static boolean IsLinear(String flpn)
    {
        String n = StringTools.trim(flpn).toUpperCase();
        if (StringTools.isBlank(n)) {
            // -- is blank
            return false; // not LINEAR
        } else
        if (n.equals(FLP_LINEAR_NAME)) {
            // -- is "LINEAR"
            return true;
        } else 
        if (n.startsWith(FLP_LINEAR_NAME+":")) {
            // -- is "LINEAR:..."
            return true;
        } else 
        if (n.startsWith(FLP_LINEAR_NAME+".")) {
            // -- is "LINEAR.percent", "LINEAR.400mm", etc
            return true;
        } else {
            // -- else not LINEAR
            return false;
        }
    }

    /**
    *** Convenience for checking the FuelLevelProfile name "CYLINDER"
    *** @return True if the specified name is equivalent to "CYLINDER"
    **/
    public static boolean IsCylinder(String flpn)
    {
        String n = StringTools.trim(flpn).toUpperCase();
        if (StringTools.isBlank(n)) {
            // -- is blank
            return false; // not CYLINDER
        } else
        if (n.equals(FLP_CYLINDER_NAME)) {
            // -- is "CYLINDER"
            return true;
        } else 
        if (n.startsWith(FLP_CYLINDER_NAME+":")) {
            // -- is "CYLINDER:..."
            return true;
        } else 
        if (n.startsWith(FLP_CYLINDER_NAME+".")) {
            // -- is "CYLINDER.percent", "CYLINDER.400mm", etc
            return true;
        } else {
            // -- else not CYLINDER
            return false;
        }
    }

    /**
    *** Convenience for checking the FuelLevelProfile name "PROFILE"
    *** @return True if the specified name is equivalent to "PROFILE"
    **/
    public static boolean IsProfile(String flpn)
    {
        String n = StringTools.trim(flpn).toUpperCase();
        if (StringTools.isBlank(n)) {
            // -- is blank
            return false; // not PROFILE
        } else
        if (n.equals(FLP_PROFILE_NAME)) {
            // -- is "PROFILE"
            return true;
        } else 
        if (n.startsWith(FLP_PROFILE_NAME+":")) {
            // -- is "PROFILE:..."
            return true;
        } else 
        if (n.startsWith(FLP_PROFILE_NAME+".")) {
            // -- is "PROFILE.xxx", etc
            return true;
        } else {
            // -- else not PROFILE
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a list of FuelLevelProfile IDs
    **/
    public static OrderedMap<String,String> GetFuelLevelProfiles(Locale locale, boolean inclNone)
    {
        OrderedMap<String,String> profList = new OrderedMap<String,String>();

        /* include None/Default? */
        if (inclNone) {
            I18N i18n = I18N.getI18N(FuelLevelProfile.class, locale);
            profList.put(FLP_NONE_ID, i18n.getString("FuelLevelProfile.default","Default"));
        }

        /* add pre-defined profiles */
        profList.put(FLP_LINEAR_percent.getID()  ,FLP_LINEAR_percent.getDescription(locale));
        profList.put(FLP_CYLINDER_percent.getID(),FLP_CYLINDER_percent.getDescription(locale));

        /* include any added profiles */
        for (String profID : FuelLevelProfileMap.keySet()) {
            FuelLevelProfile flp = FuelLevelProfileMap.get(profID);
            if (!profList.containsKey(profID)) {
                profList.put(profID, flp.getDescription(locale));
            }
        }

        /* sort by profile ID */
        profList.sortKeys(new Comparator<String>() {
            public int compare(String k1, String k2) {
                int v1 = IsNone(k1)? 0 : IsLinear(k1)? 1 : IsCylinder(k1)? 2 : IsProfile(k1)? 3 : 4;
                int v2 = IsNone(k2)? 0 : IsLinear(k2)? 1 : IsCylinder(k2)? 2 : IsProfile(k2)? 3 : 4;
                return (v1 == v2)? 0 : (v1 < v2)? -1 : 1;
            }
        });

        /* return profile list */
        return profList;

    }

    // --------------------------------

    /**
    *** Add a FuelLevelProfile 
    **/
    public static FuelLevelProfile AddFuelLevelProfile(String ID, String profile, I18N.Text desc)
    {
        FuelLevelProfile flp = new FuelLevelProfile(profile, desc);
        if (flp.isValid()) {
            if (!StringTools.isBlank(ID)) {
                // -- specified ID overrides any ID found in "profile"
                flp.setID(ID);
            } else
            if (StringTools.isBlank(flp.getID())) {
                // -- "profile" did not contain an ID, assign an ID
                String pid = "profile_" + ListTools.size(FuelLevelProfileMap);
                flp.setID(pid);
            }
            FuelLevelProfileMap.put(flp.getID(), flp);
            return flp;
        } else {
            // -- invalid profile specification
            return null;
        }
    }

    /**
    *** Add a FuelLevelProfile 
    **/
    public static FuelLevelProfile AddFuelLevelProfile(String ID, double scale, ProfileItem prof[], I18N.Text desc)
    {
        FuelLevelProfile flp = new FuelLevelProfile(ID, scale, prof, desc);
        if (flp.isValid()) {
            if (!StringTools.isBlank(ID)) {
                // -- specified ID overrides any ID found in "profile"
                flp.setID(ID);
            } else
            if (StringTools.isBlank(flp.getID())) {
                // -- "profile" did not contain an ID, assign an ID
                String pid = "profile_" + ListTools.size(FuelLevelProfileMap);
                flp.setID(pid);
            }
            FuelLevelProfileMap.put(flp.getID(), flp);
            return flp;
        } else {
            // -- invalid profile specification
            return null;
        }
    }

    // --------------------------------

    /**
    *** Gets the FuelLevelProfile for the specified name.
    *** Does not return null.  If the profile name is not found, the LINEAR(percent) profile will be returned.
    **/
    public static FuelLevelProfile GetFuelLevelProfile(String profID)
    {
        return FuelLevelProfile.GetFuelLevelProfile(profID, FLP_LINEAR_percent);
    }

    /**
    *** Gets the FuelLevelProfile for the specified name.
    *** Does not return null.  If the profile name is not found, the LINEAR profile will be returned.
    **/
    public static FuelLevelProfile GetFuelLevelProfile(String profID, FuelLevelProfile dft)
    {

        /* return default? */
        // -- blank or "NONE"
        if (IsNone(profID)) {
            return dft;
        }

        /* check for in-line defined profile */
        if (profID.indexOf(":") >= 0) {
            // -- create single-use profile
            FuelLevelProfile flp = new FuelLevelProfile(profID,null/*desc*/);
            if (!flp.isValid()) { // [2.6.1-B34]
                Print.logWarn("Invalid FuelLevelProfile: " + profID);
                return dft; // [2.6.2-B53]
            }
            return flp; // [2.6.2-B53]
        }

        /* get saved profile? */
        // -- customid
        FuelLevelProfile flp = FuelLevelProfileMap.get(profID);
        if (flp != null) {
            // -- found custom added profile
            return flp;
        }

        /* LINEAR (measured percent)? */
        // -- LINEAR
        if (profID.equalsIgnoreCase(FLP_LINEAR_ID_percent)) {
            return FLP_LINEAR_percent;
        }

        /* CYLINDER (measured percent) */
        // -- CYLINDER
        if (profID.equalsIgnoreCase(FLP_CYLINDER_ID_percent)) {
            return FLP_CYLINDER_percent;
        }

        /* return default if not found */
        return dft;

    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the defined FuelLevelProfile headers
    **/
    public static void printFuelLevelProfile(DCServerConfig dcs, int width)
    {
        if (dcs != null) {
            for (String grpID : dcs.getPropertyGroupNames()) {
                FuelLevelProfile flp = dcs.getFuelLevelProfile(grpID,null);
                if (flp != null) {
                    String hdr = StringTools.padRight("FuelLevelProfile",' ',width) + ": ";
                    Print.logInfo(hdr + "["+grpID+"] "+flp);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** FuelLevelProfile class<br>
    **/
    private static class ProfileItem
        extends XYPair
    {
        // "25,25"
        public ProfileItem(String lvl) {
            super(lvl);
            if (this.isValid()) {
                // -- assume integer percentage if no decimal point
                //String L[] = StringTools.split(lvl,','); // at least 2 points
                //if (L[0].indexOf(".") < 0) { super.X /= 100.0; } <-- "X" may not be a percent
                //if (L[1].indexOf(".") < 0) { super.Y /= 100.0; } <-- more general solution for "Y" below
                // -- assume "Y" is a full percentage if > 1
                if (super.Y > 1.0) { super.Y /= 100.0; }    // <-- accepts "25", "25.0", and "0.25"
                // -- check for valid range
                this._validateRange();
            }
        }
        public ProfileItem(double evLvl, double acLvl) {
            super(evLvl, acLvl);
            // -- check for valid range
            this._validateRange();
        }
        private boolean _validateRange() {
            if (!this.isValid()) {
                // -- already invalid
            } else
            if (/*(this.X < 0.0) || (this.X > 1.0) || */ // <-- "X" may be fuel-sensor height
                (this.Y < 0.0) || (this.Y > 1.0)   ) {
                Print.logError("Invalid FuelLevelProfile values: " + this.X + "," + this.Y);
                super.X       = 0.0;
                super.Y       = 0.0;
                super.isValid = false;
            }
            return this.isValid();
        }
        public boolean isValid() {
            return super.isValid();
        }
        public double getEventLevel() {
            return super.getX();
        }
        public double getActualLevel() {
            return super.getY();
        }
        public String toString() {
            return super.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String      profID     = "";
    private I18N.Text   profDesc   = null;
    private double      profScale  = 1.0;
    private double      profOffset = 0.0;
    private ProfileItem profItem[] = null;
    private CurveFit    curveFit   = null;

    /**
    *** Constructor
    **/
    public FuelLevelProfile(String profile, I18N.Text desc)
    {
        // -- "profile" is expected to be in the following format:
        // -    <ProfileID>:<Scale>[,<Offset>][:<Points>]
        // -        <ProfileID> - One of "LINEAR", "CYLINDER", or "PROFILE"
        // -        <Scale>     - The incoming measured value (plus offset) will be divided by this Scale.
        // -        <Offset>    - This value will be ADDED to the incoming measure value (before dividing by Scale).
        // -        <Points>    - A list of "X,Y" points (separated by "|"), where "X" is the measured value,
        // -                      and "Y" is the actual value.
        // -- Examples:
        // -    CYLINDAR:1.0:0,0|5,50|10,100
        // -    CYLINDAR:600,40
        // -    PROFILE:1.0:0,0|2.5,0.5|5,1.0

        /* save ID/Scale/description */
        String P[]   = StringTools.split(StringTools.trim(profile).toUpperCase(),':');
        String ID    = (P.length > 0)? P[0] : "";
        String SCALE = (P.length > 1)? P[1] : "";
        String PROF  = (P.length > 2)? P[2] : "";
        this.setID(!StringTools.isBlank(ID)? ID : FLP_PROFILE_ID);
        this.setDescription(desc);
        if (!StringTools.isBlank(SCALE)) {
            double scaOfs[] = StringTools.parseDouble(StringTools.split(SCALE,','),0.0);
            double sca      = (scaOfs.length > 0)? scaOfs[0] : 1.0;
            double ofs      = (scaOfs.length > 1)? scaOfs[1] : 0.0;
            this.setScale(sca, ofs);
        }
        //Print.logInfo("ID="+ID+", SCALE="+SCALE+", PROF="+PROF);

        /* profile */
        if (!StringTools.isBlank(PROF)) {
            // -- PROFILE:<SCALE>:0.00,0.00|0.50,0.50|1.00:1.00
            String rp[] = StringTools.split(PROF,'|');
            Vector<ProfileItem> flp = new Vector<ProfileItem>();
            for (int i = 0; i < rp.length; i++) {
                ProfileItem pi = new ProfileItem(rp[i]);
                if (pi.isValid()) {
                    flp.add(pi);
                }
            }
            this.setProfile(flp.toArray(new ProfileItem[flp.size()]));
        } else
        if (StringTools.isBlank(ID) || 
            ID.equalsIgnoreCase(FLP_LINEAR_NAME)) {
            // -- LINEAR:<SCALE>   (default)
            this.setProfile(FLP_LINEAR_GRAPH);
            this.setDescription(I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear","Linear profile"));
        } else
        if (ID.equalsIgnoreCase(FLP_CYLINDER_NAME)) {
            // -- CYLINDER:<SCALE>
            this.setProfile(FLP_CYLINDER_GRAPH);
            this.setDescription(I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.cylinder","Horizontal Cylinder"));
        } else
        if (ID.equalsIgnoreCase(FLP_PROFILE_NAME)) {
            // -- PROFILE:<SCALE>:??? (profile graph not specified!)
            // -  Should NOT be here! (profile should have been set above)
            this.setProfile(FLP_LINEAR_GRAPH);
            this.setDescription(I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.linear","Linear profile"));
        } else {
            // -- UNKNOWN:<SCALE>:???
            // -  Should NOT be here! (profile should have been set above)
            this.setProfile(FLP_LINEAR_GRAPH);
            this.setDescription(I18N.getString(FuelLevelProfile.class,"FuelLevelProfile.unknown","Unrecognized profile"));
        }

    }

    /**
    *** Constructor
    **/
    private FuelLevelProfile(String ID, double scale, ProfileItem prof[], I18N.Text desc)
    {
        this.setID(ID);
        this.setScale(scale, 0.0);
        this.setProfile(prof);
        this.setDescription(desc);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ID of this FuelLevelProfile
    **/
    public void setID(String ID)
    {
        this.profID = StringTools.trim(ID);
    }

    /**
    *** Gets the ID of this FuelLevelProfile
    **/
    public String getID()
    {
        return this.profID;
    }

    /**
    *** Gets the ID of this FuelLevelProfile
    **/
    public String getName()
    {
        return this.getID();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Description of this FuelLevelProfile
    **/
    public void setDescription(I18N.Text desc)
    {
        this.profDesc = desc;
    }

    /**
    *** Gets the Description of this FuelLevelProfile
    **/
    public String getDescription(Locale locale)
    {
        if (this.profDesc != null) {
            return this.profDesc.toString(locale);
        } else {
            return this.getName();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the profile offset
    **/
    private void setScale(double scale, double preOfs)
    {
        this.profScale  = (scale <= 0.0)? 1.0 : scale;
        this.profOffset = preOfs;
    }

    /**
    *** Sets the profile scale
    **/
    private void setScale(double scale)
    {
        this.setScale(scale, 0.0); // no offset
    }
    
    /**
    *** Gets the profile scale
    **/
    public double getScale()
    {
        return (this.profScale <= 0.0)? 1.0 : this.profScale;
    }

    /**
    *** Gets the profile offset
    **/
    public double getOffset()
    {
        return this.profOffset;
    }
    
    /**
    *** Adjusts the input value with the profile offset and scale
    **/
    public double scaleValue(double val)
    {
        return (val + this.getOffset()) / this.getScale();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Profile list 
    **/
    private void setProfile(ProfileItem flp[])
    {
        this.profItem = flp; // ProfileItem list
        if (RTConfig.getBoolean(PROP_FuelLevelProfile_enableCurveFit,ENABLE_CURVEFIT)) {
            // -- enable Curve-Fitting (if valid)
            CurveFit cf = new CurveFit(this.profItem);
            this.curveFit = cf.isValid()? cf : null;
        } else {
            // -- disable Curve-Fitting
            this.curveFit = null;
        }
    }

    /**
    *** Gets the Profile list 
    **/
    private ProfileItem[] getProfile()
    {
        return this.profItem;
    }

    /**
    *** Returns true if this FuelLevelProfile is valid
    **/
    public boolean isValid()
    {
        if (ListTools.size(this.getProfile()) < 2) {
            // -- must have at-least 2 points
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if CurveFit is enabled
    **/
    public boolean hasCurveFit()
    {
        CurveFit cf = this._getCurveFit();
        return ((cf != null) && cf.isValid())? true : false;
    }

    /**
    *** Gets the CurveFit
    **/
    private CurveFit _getCurveFit()
    {
        return this.curveFit; // may be null
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the actual fuel level from the event specified fuel level
    *** @param fuelLevelVal  The event specified fuel level
    *** @return The actual fuel level
    **/
    public double getActualFuelLevel(double fuelLevelVal)
    {
        final double preVal = this.scaleValue(fuelLevelVal);

        /* check CurveFit */
        CurveFit cf = this._getCurveFit();
        if ((cf != null) && cf.isValid()) {
            // -- apply CurveFit function
            double P = cf.FTN(preVal);
            return (P < 0.0)? 0.0 : (P > 1.0)? 1.0 : P;
        }

        /* get profile */
        ProfileItem flp[] = this.getProfile();
        if (ListTools.size(flp) < 2) {
            // -- profile is invalid, return preVal as-is
            double P = preVal;
            return (P < 0.0)? 0.0 : (P > 1.0)? 1.0 : P;
        }

        /* get high/low values */
        ProfileItem hi = null;
        ProfileItem lo = null;
        for (int i = 0; i < flp.length; i++) {
            double flpv = flp[i].getEventLevel();
            if (flpv == preVal) {
                // -- exact match
                hi = flp[i];
                lo = flp[i];
                break;
            } else
            if (flpv >= preVal) {
                // -- found range
                hi = flp[i];
                lo = (i > 0)? flp[i - 1] : null;
                break;
            } else {
                // -- preVal < flp[i].getEventLevel()
                lo = flp[i];
                // continue;
            }
        }

        /* calculate linear interpolation between points */
        final double postVal;
        if ((hi != null) && (lo != null)) {
            // -- interprolate
            double evHi = hi.getEventLevel();
            double evLo = lo.getEventLevel();
            if (evHi != evLo) {
                // -- hi/lo differ, interprolate
                double evD  = (preVal - evLo) / (evHi - evLo);
                double acHi = hi.getActualLevel();
                double acLo = lo.getActualLevel();
                postVal = acLo + (evD * (acHi - acLo));
            } else {
                // -- hi/lo are the same
                double acLo = lo.getActualLevel();
                postVal = acLo;
            }
        } else
        if (lo != null) {
            // -- hi is null
            postVal = lo.getActualLevel();
        } else 
        if (hi != null) {
            // -- lo is null
            postVal = hi.getActualLevel();
        } else {
            // -- hi/lo are null
            postVal = (preVal < 0.0)? 0.0 : (preVal > 1.0)? 1.0 : preVal;
        }
        double P = postVal;
        return (P < 0.0)? 0.0 : (P > 1.0)? 1.0 : P;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getID());
        sb.append(":");
        sb.append(this.getScale());
        if (this.getOffset() != 0.0) {
            sb.append(",");
            sb.append(this.getOffset());
        }
        ProfileItem pi[] = this.getProfile();
        if (!ListTools.isEmpty(pi)) {
            sb.append(":");
            XYPair.ToListString(pi,sb);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
