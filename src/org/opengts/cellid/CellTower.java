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
//  2010/05/24  Martin D. Flynn
//      -Initial release
//  2011/06/16  Martin D. Flynn
//      -Added "GetLocation" interface.
//      -Moved from org.opengts.util
//  2011/07/01  Martin D. Flynn
//      -Moved GetLocation interface to MobileLocationProvider
//  2017/09/07  Martin D. Flynn
//      -Added support for the requesting MobileID(UniqueID)
// ----------------------------------------------------------------------------
package org.opengts.cellid;

import java.io.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.db.tables.Device; // [2.6.5-B49]

import org.opengts.cellid.google.GoogleMobileService;

/**
*** A Container for Cell-Tower information
**/

public class CellTower
{
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String ARG_MCC              = "mcc";    // Mobile Country Code
    public static final String ARG_MNC              = "mnc";    // Mobile Network Code
    public static final String ARG_LAC              = "lac";    // Location Area Code (0..65535)
    public static final String ARG_CID              = "cid";    // Cell Tower ID
    public static final String ARG_TAV              = "tav";    // Timing Advance (0..63)
    public static final String ARG_RAT              = "rat";    // Radio Access Technology
    public static final String ARG_RXLEV            = "rxlev";  // Reception Level (dBm)
    public static final String ARG_ARFCN            = "arfcn";  // Absolute Radio-Frequency Channel Number
    public static final String ARG_PSC              = "psc";    // Primary Scrambling Code (0..511)
    public static final String ARG_RADIO            = "radio";  // Radio Type: GSM, CDMA, UMTS, LTE

    // ------------------------------------------------------------------------

    private String          cellTowerName           = null;
    private MobileLocation  cellLocation            = null;

    private int             cellTowerID             = -1;
    private int             radioAccessTechnology   = -1;
    private int             mobileCountryCode       = -1;
    private int             mobileNetworkCode       = -1;
    private int             locationAreaCode        = -1;
    private int             arfChannelNumber        = -1;
    private int             receptionLevel          = -1; // RxLevel, signal strength
    private int             timingAdvance           = -1;
    private int             primaryScramblingCode   = -1;
    private String          radioType               = "";

    private Device          deviceReq               = null;
    private String          mobileID                = null;

    /**
    *** Constructor
    **/
    public CellTower()
    {
        super();
        this._init(
            -1/*RAT*/, 
            -1/*MCC*/, 
            -1/*MNC*/, 
            -1/*TAV*/, 
            -1/*CID*/, 
            -1/*LAC*/, 
            -1/*ARFCN*/, 
            -1/*RXLEV*/,
            -1/*PSC*/,
            null/*RADIO*/
            );
    }

    /**
    *** Copy Constructor
    **/
    public CellTower(CellTower other)
    {
        super();
        if (other != null) {
            this._init(
                other.getRadioAccessTechnology(),
                other.getMobileCountryCode(),
                other.getMobileNetworkCode(),
                other.getTimingAdvance(),
                other.getCellTowerID(),
                other.getLocationAreaCode(),
                other.getAbsoluteRadioFrequencyChannelNumber(),
                other.getReceptionLevel(),
                other.getPrimaryScramblingCode(),
                other.getRadioType()
                );
            this.setName(other.getName());
            if (other.hasMobileLocation()) {
                this.setMobileLocation(new MobileLocation(other.getMobileLocation()));
            }
        }
    }

    /**
    *** Constructor
    **/
    public CellTower(int mcc, int mnc, int tav, int cid, int lac, int arfcn, int rxlev)
    {
        super();
        this._init(
            -1/*RAT*/,
            mcc,
            mnc,
            tav,
            cid,
            lac,
            arfcn,
            rxlev,
            -1/*PSC*/,
            null/*RADIO*/
            );
    }

    /**
    *** Constructor
    **/
    public CellTower(int rat, int mcc, int mnc, int tav, int cid, int lac, int arfcn, int rxlev, int psc)
    {
        super();
        this._init(
            rat,
            mcc,
            mnc,
            tav,
            cid,
            lac,
            arfcn,
            rxlev,
            psc,
            null/*radio*/
            );
    }

    /**
    *** Constructor
    **/
    public CellTower(int cid, int lac, int arfcn, int rxlev)
    {
        super();
        this._init(
            -1/*RAT*/, 
            -1/*MCC*/,
            -1/*MNC*/,
            -1/*TAV*/,
            cid,
            lac,
            arfcn,
            rxlev,
            -1/*PSC*/,
            null/*RADIO*/
            );
    }

    /**
    *** Constructor
    **/
    public CellTower(String cidStr)
    {
        this(new RTProperties(cidStr));
    }

    /**
    *** Constructor
    **/
    public CellTower(RTProperties cidp)
    {
        super();
        if (cidp != null) {
            int    rat   = cidp.getInt(   ARG_RAT  , -1);
            int    mcc   = cidp.getInt(   ARG_MCC  , -1);
            int    mnc   = cidp.getInt(   ARG_MNC  , -1);
            int    tav   = cidp.getInt(   ARG_TAV  , -1);
            int    cid   = cidp.getInt(   ARG_CID  , -1);
            int    lac   = cidp.getInt(   ARG_LAC  , -1);
            int    arfcn = cidp.getInt(   ARG_ARFCN, -1);
            int    rxlev = cidp.getInt(   ARG_RXLEV, -1);
            int    psc   = cidp.getInt(   ARG_PSC  , -1);
            String radio = cidp.getString(ARG_RADIO, "");
            this._init(rat, mcc, mnc, tav, cid, lac, arfcn, rxlev, psc, radio);
        }
    }

    /**
    *** private init
    **/
    private void _init(int rat, int mcc, int mnc, int tav, int cid, int lac, int arfcn, int rxlev, int psc, String radio)
    {
        // --
        this.radioAccessTechnology  = rat;
        // --
        this.mobileCountryCode      = mcc;
        this.mobileNetworkCode      = mnc;
        this.timingAdvance          = tav;
        // --
        this.cellTowerID            = cid;
        this.locationAreaCode       = lac;
        this.arfChannelNumber       = arfcn;
        this.receptionLevel         = rxlev;
        // --
        this.primaryScramblingCode  = psc;
        // --
        this.radioType              = radio;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this CellTower instance contains valid information
    **/
    public boolean isValid()
    {
        if (this.getCellTowerID() <= 0) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the Cell Tower Name (if any)
    *** @param name The Cell Tower name
    **/
    public void setName(String name)
    {
        this.cellTowerName = !StringTools.isBlank(name)? name.trim() : null;
    }

    /** 
    *** Gets the Cell Tower Name (if available)
    *** @return The Cell Tower name (or null if not available)
    **/
    public String getName()
    {
        return this.cellTowerName;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the Mobile location
    *** @param mobLoc The Mobile location
    **/
    public void setMobileLocation(MobileLocation mobLoc)
    {
        this.cellLocation = mobLoc;
    }

    /** 
    *** Sets the Mobile location
    *** @param mobLoc The Mobile location
    **/
    public void setMobileLocation(GeoPoint gp, double accuracy)
    {
        if (this.cellLocation != null) {
            this.cellLocation.setGeoPoint(gp);
            this.cellLocation.setAccuracy(accuracy);
        } else {
            this.cellLocation = new MobileLocation(gp, accuracy);
        }
    }

    /** 
    *** Gets the Mobile Location (if available)
    *** @return The Mobile Location (or null if not available)
    **/
    public MobileLocation getMobileLocation()
    {
        return this.cellLocation;
    }

    /** 
    *** Returns true if this instance has a Mobile Location
    *** @return True if this instance has a Mobile Location
    **/
    public boolean hasMobileLocation()
    {
        return (this.cellLocation != null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Radio Access Technology code
    *** @param rat The Radio Access Technology code
    **/
    public void setRadioAccessTechnology(int rat)
    {
        this.radioAccessTechnology = rat;
    }

    /**
    *** Gets the Radio Access Technology code
    *** @return The Radio Access Technology code
    **/
    public int getRadioAccessTechnology()
    {
        return this.radioAccessTechnology;
    }

    /**
    *** Returns true if the Radio Access Technology code has been defined
    *** @return True if the Radio Access Technology code has been defined
    **/
    public boolean hasRadioAccessTechnology()
    {
        return (this.radioAccessTechnology >= 0);
    }

    /**
    *** Gets the Radio Access Technology code as a String
    *** @return The Radio Access Technology code as a String
    **/
    public String getRadioAccessTechnologyString()
    {
        return this.hasRadioAccessTechnology()?
            String.valueOf(this.getRadioAccessTechnology()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Mobile Country Code (MCC)
    *** @param mcc The Mobile Country Code (MCC)
    **/
    public void setMobileCountryCode(int mcc)
    {
        this.mobileCountryCode = mcc;
    }

    /**
    *** Gets the Mobile Country Code (MCC)
    *** @return The Mobile Country Code (MCC)
    **/
    public int getMobileCountryCode()
    {
        return this.mobileCountryCode;
    }

    /**
    *** Returns true if the Mobile Country Code has been defined
    *** @return True if the Mobile Country Code has been defined
    **/
    public boolean hasMobileCountryCode()
    {
        return (this.mobileCountryCode >= 0);
    }

    /**
    *** Gets the Mobile Country Code as a String
    *** @return The Mobile Country Code as a String
    **/
    public String getMobileCountryCodeString()
    {
        return this.hasMobileCountryCode()? 
            String.valueOf(this.getMobileCountryCode()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Mobile Network Code (MNC)
    *** @param mnc The Mobile Network Code (MNC)
    **/
    public void setMobileNetworkCode(int mnc)
    {
        this.mobileNetworkCode = mnc;
    }

    /**
    *** Gets the Mobile Network Code (MNC)
    *** @return The Mobile Network Code (MNC)
    **/
    public int getMobileNetworkCode()
    {
        return this.mobileNetworkCode;
    }

    /**
    *** Returns true if the Mobile Network Code has been defined
    *** @return True if the Mobile Network Code has been defined
    **/
    public boolean hasMobileNetworkCode()
    {
        return (this.mobileNetworkCode >= 0);
    }

    /**
    *** Gets the Mobile Network Code as a String
    *** @return The Mobile Network Code as a String
    **/
    public String getMobileNetworkCodeString()
    {
        return this.hasMobileNetworkCode()? 
            String.valueOf(this.getMobileNetworkCode()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Timing Advance
    *** @param tav The Timing Advance
    **/
    public void setTimingAdvance(int tav)
    {
        this.timingAdvance = tav;
    }

    /**
    *** Gets the Timing Advance
    *** @return The Timing Advance
    **/
    public int getTimingAdvance()
    {
        return this.timingAdvance;
    }

    /**
    *** Returns true if the Timing Advance has been defined
    *** @return True if the Timing Advance has been defined
    **/
    public boolean hasTimingAdvance()
    {
        return (this.timingAdvance >= 0);
    }

    /**
    *** Gets the Timing Advance as a String
    *** @return The Timing Advance as a String
    **/
    public String getTimingAdvanceString()
    {
        return this.hasTimingAdvance()? 
            String.valueOf(this.getTimingAdvance()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Cell Tower ID (CID)
    *** @param cid The Cell Tower ID
    **/
    public void setCellTowerID(int cid)
    {
        this.cellTowerID = cid;
    }

    /**
    *** Gets the Cell Tower ID (CID)
    *** @return The Cell Tower ID
    **/
    public int getCellTowerID()
    {
        return this.cellTowerID;
    }

    /**
    *** Returns true if the Cell Tower ID has been defined
    *** @return True if the Cell Tower ID has been defined
    **/
    public boolean hasCellTowerID()
    {
        return (this.cellTowerID >= 0);
    }

    /**
    *** Gets the Cell Tower ID as a String
    *** @return The Cell Tower ID as a String
    **/
    public String getCellTowerIDString()
    {
        return this.hasCellTowerID()? 
            String.valueOf(this.getCellTowerID()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Location Area Code (LAC)
    *** @param lac  The Location Area Code
    **/
    public void setLocationAreaCode(int lac)
    {
        this.locationAreaCode = lac;
    }

    /**
    *** Gets the Location Area Code (LAC)
    *** @return The Location Area Code
    **/
    public int getLocationAreaCode()
    {
        return this.locationAreaCode;
    }

    /**
    *** Returns true if the Location Area Code has been defined
    *** @return True if the Location Area Code has been defined
    **/
    public boolean hasLocationAreaCode()
    {
        return (this.locationAreaCode >= 0);
    }

    /**
    *** Gets the Location Area Code (LAC) as a String
    *** @return The Location Area Code (LAC) as a String
    **/
    public String getLocationAreaCodeString()
    {
        return this.hasLocationAreaCode()? 
            String.valueOf(this.getLocationAreaCode()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Absolute Radio Frequency Channel Number
    *** @param arfcn The Absolute Radio Frequency Channel Number
    **/
    public void setAbsoluteRadioFrequencyChannelNumber(int arfcn)
    {
        this.arfChannelNumber = arfcn;
    }

    /**
    *** Gets the Absolute Radio Frequency Channel Number
    *** @return The Absolute Radio Frequency Channel Number
    **/
    public int getAbsoluteRadioFrequencyChannelNumber()
    {
        return this.arfChannelNumber;
    }

    /**
    *** Returns true if the Absolute Radio Frequency Channel Number has been defined
    *** @return True if the Absolute Radio Frequency Channel Number has been defined
    **/
    public boolean hasAbsoluteRadioFrequencyChannelNumber()
    {
        return (this.arfChannelNumber >= 0);
    }

    /**
    *** Gets the Absolute Radio Frequency Channel Number as a String
    *** @return The Absolute Radio Frequency Channel Number as a String
    **/
    public String getAbsoluteRadioFrequencyChannelNumberString()
    {
        return this.hasAbsoluteRadioFrequencyChannelNumber()? 
            String.valueOf(this.getAbsoluteRadioFrequencyChannelNumber()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Reception Level
    *** @param rxlev The Reception Level
    **/
    public void setReceptionLevel(int rxlev)
    {
        this.receptionLevel = rxlev;
    }

    /**
    *** Gets the Reception Level
    *** @return The Reception Level
    **/
    public int getReceptionLevel()
    {
        return this.receptionLevel;
    }

    /**
    *** Returns true if the Reception Level has been defined
    *** @return True if the Reception Level has been defined
    **/
    public boolean hasReceptionLevel()
    {
        return (this.receptionLevel >= 0);
    }

    /**
    *** Gets the Reception Level as a String
    *** @return The Reception Level as a String
    **/
    public String getReceptionLevelString()
    {
        return this.hasReceptionLevel()? 
            String.valueOf(this.getReceptionLevel()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Primary Scrambling Code
    *** @param psc The Primary Scrambling Code
    **/
    public void setPrimaryScramblingCode(int psc)
    {
        this.primaryScramblingCode = psc;
    }

    /**
    *** Gets the Primary Scrambling Code
    *** @return The Primary Scrambling Code
    **/
    public int getPrimaryScramblingCode()
    {
        return this.primaryScramblingCode;
    }

    /**
    *** Returns true if the Primary Scrambling Code has been defined
    *** @return True if the Primary Scrambling Code has been defined
    **/
    public boolean hasPrimaryScramblingCode()
    {
        return (this.primaryScramblingCode >= 0);
    }

    /**
    *** Gets the Primary Scrambling Code as a String
    *** @return The Primary Scrambling Code as a String
    **/
    public String getPrimaryScramblingCodeString()
    {
        return this.hasPrimaryScramblingCode()? 
            String.valueOf(this.getPrimaryScramblingCode()) : 
            "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Radio Type
    *** @param radio The Radio Type ("gsm", "cdma", "umts", "lte")
    **/
    public void setRadioType(String radio)
    {
        this.radioType = radio; // "gsm", "cdma", "umts", "lte"
    }

    /**
    *** Gets the Radio Type
    *** @return The Radio Type
    **/
    public String getRadioType()
    {
        return StringTools.trim(this.radioType);
    }

    /**
    *** Returns true if the Radio Type has been defined
    *** @return True if the Radio Type has been defined
    **/
    public boolean hasRadioType()
    {
        return !StringTools.isBlank(this.getRadioType())? true : false;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Sets the Device instance requesting the CellTower location
    *** @param devReq The Device instance requesting the CellTower location
    **/
    public void setRequestorDevice(Device devReq)
    {
        this.deviceReq = devReq; // may be null
        this.mobileID  = null;
    }

    /** 
    *** Gets the Device instance requesting the CellTower location
    *** @return The Device instance requesting the CellTower location (null if undefined)
    **/
    public Device getRequestorDevice()
    {
        return this.deviceReq;
    }

    /** 
    *** Returns true if the Device instance requesting the CellTower location is defined
    *** @return True if the Device instance requesting the CellTower location is defined
    **/
    public boolean hasRequestorDevice()
    {
        return (this.deviceReq != null)? true : false;
    }

    // --------------------------------

    /**
    *** Gets the Device instance UniqueID (system-wide unique id)
    **/
    private String _getRequestorDeviceUniqueID()
    {
        return (this.deviceReq != null)? this.deviceReq.getUniqueID() : null;
    }

    /**
    *** Gets the Device instance ModemID (UniqueID without the prefix)
    **/
    private String _getRequestorDeviceModemID()
    {
        return (this.deviceReq != null)? this.deviceReq.getModemID() : null;
    }

    /**
    *** Gets the Device instance AccountID/DeviceID 
    **/
    private String _getRequestorDeviceID()
    {
        return (this.deviceReq != null)? (this.deviceReq.getAccountID()+"/"+this.deviceReq.getDeviceID()) : null;
    }

    // --------------------------------

    /** 
    *** Sets the MobileID of the requesting Device.
    *** @return The MobileID of the requesting Device
    **/
    public void setRequestorMobileID(String mobID)
    {
        this.mobileID  = !StringTools.isBlank(mobID)? mobID.trim() : null;
        this.deviceReq = null;
    }

    /** 
    *** Gets the MobileID of the requesting Device (null if undefined).
    *** @return The MobileID of the requesting Device (null if undefined)
    **/
    public String getRequestorMobileID()
    {
        if (!StringTools.isBlank(this.mobileID)) {
            return this.mobileID;
        } else {
            return this._getRequestorDeviceUniqueID(); // may return null
        }
    }

    /** 
    *** Returns true if the MobileID for this instance has been defined, false otherwise.
    *** @return True if the MobileID for this instance has been defined.
    **/
    public boolean hasRequestorMobileID()
    {
        return !StringTools.isBlank(this.getRequestorMobileID())? true : false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a RTProperties representation of this instance
    *** @return A RTProperties representation of this instance
    **/
    public RTProperties getRTProperties(RTProperties cidp)
    {
        if (cidp == null) { cidp = new RTProperties(); }
        // -- RAT
        if (this.hasRadioAccessTechnology()) {
            cidp.setInt(ARG_RAT  , this.getRadioAccessTechnology());
        } else {
            cidp.removeProperty(ARG_RAT);
        }
        // -- MCC
        if (this.hasMobileCountryCode()) {
            cidp.setInt(ARG_MCC  , this.getMobileCountryCode());
        } else {
            cidp.removeProperty(ARG_MCC);
        }
        // -- MNC
        if (this.hasMobileNetworkCode()) {
            cidp.setInt(ARG_MNC  , this.getMobileNetworkCode());
        } else {
            cidp.removeProperty(ARG_MNC);
        }
        // -- TAV
        if (this.hasTimingAdvance()) {
            cidp.setInt(ARG_TAV  , this.getTimingAdvance());
        } else {
            cidp.removeProperty(ARG_TAV);
        }
        // -- CID
        if (this.hasCellTowerID()) {
            cidp.setInt(ARG_CID  , this.getCellTowerID());
        } else {
            cidp.removeProperty(ARG_CID);
        }
        // -- LAC
        if (this.hasLocationAreaCode()) {
            cidp.setInt(ARG_LAC  , this.getLocationAreaCode());
        } else {
            cidp.removeProperty(ARG_LAC);
        }
        // -- ARFCN
        if (this.hasAbsoluteRadioFrequencyChannelNumber()) {
            cidp.setInt(ARG_ARFCN, this.getAbsoluteRadioFrequencyChannelNumber());
        } else {
            cidp.removeProperty(ARG_ARFCN);
        }
        // -- RXLEV
        if (this.hasReceptionLevel()) {
            cidp.setInt(ARG_RXLEV, this.getReceptionLevel());
        } else {
            cidp.removeProperty(ARG_RXLEV);
        }
        // -- PSC
        if (this.hasPrimaryScramblingCode()) {
            cidp.setInt(ARG_PSC, this.getPrimaryScramblingCode());
        } else {
            cidp.removeProperty(ARG_PSC);
        }
        // -- RADIO
        if (this.hasRadioType()) {
            cidp.setString(ARG_RADIO, this.getRadioType());
        } else {
            cidp.removeProperty(ARG_RADIO);
        }
        // -- MobileLocation
        if (this.hasMobileLocation()) {
            this.getMobileLocation().getRTProperties(cidp);
        } else {
            cidp.removeProperty(MobileLocation.ARG_GPS);
            cidp.removeProperty(MobileLocation.ARG_ACC);
        }
        // -- return
        return cidp;
    }

    /**
    *** Returns the String representation of this instance
    *** @return String representation of this instance
    **/
    public String toString()
    {
        // -- This is used for "<EventData>.setCellServingInfo(...)" and MUST
        // -  return an RTProperties encoded String! 
        return this.getRTProperties(null).toString();
    }
        
    // ------------------------------------------------------------------------

    /** 
    *** Returns true if the specified instance is equal to this insance
    *** (the MobileLocation is not tested for equality)
    *** @param other  The other instance
    *** @return True if equal, false otherwise
    **/
    public boolean equals(Object other)
    {
        if (other instanceof CellTower) {
            CellTower oct = (CellTower)other;
            if (this.cellTowerID            != oct.cellTowerID          ) { return false; }
            if (this.radioAccessTechnology  != oct.radioAccessTechnology) { return false; }
            if (this.mobileCountryCode      != oct.mobileCountryCode    ) { return false; }
            if (this.mobileNetworkCode      != oct.mobileNetworkCode    ) { return false; }
            if (this.locationAreaCode       != oct.locationAreaCode     ) { return false; }
            if (this.arfChannelNumber       != oct.arfChannelNumber     ) { return false; }
            if (this.receptionLevel         != oct.receptionLevel       ) { return false; }
            if (this.timingAdvance          != oct.timingAdvance        ) { return false; }
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
