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
//  2015/05/03  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.dbtypes;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.StatusCodes;
import org.opengts.db.tables.EventData;
import org.opengts.db.tables.Driver; // DutyStatus

public class DTELogState
    extends DBFieldType
{

    // ------------------------------------------------------------------------
    // lastTS=123456789 lastSC=0xF010 isDriving=true|false distKM=1234.5 [lastGP=42.123/-142.123] [driver=smith]

    public  static final String     KEY_lastTimestamp      = "lastTS";
    public  static final String     KEY_lastStatusCode     = "lastSC";
    public  static final String     KEY_isDriving          = "isDriving";
    public  static final String     KEY_distanceKM         = "distKM";
    public  static final String     KEY_lastGeoPoint       = "lastGP";
    public  static final String     KEY_driver             = "driver";

    public  static final long       DFT_lastTimestamp      = 0L;
    public  static final int        DFT_lastStatusCode     = StatusCodes.STATUS_NONE;
    public  static final boolean    DFT_isDriving          = false;
    public  static final double     DFT_distanceKM         = 0.0;
    public  static final GeoPoint   DFT_lastGeoPoint       = null;
    public  static final String     DFT_driver             = null;

    public  static final boolean    AUTO_INIT_TIMESTAMP    = false;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private long        lastTimestamp   = DFT_lastTimestamp;
    private int         lastStatusCode  = DFT_lastStatusCode;
    private boolean     isDriving       = DFT_isDriving;
    private double      lastDistanceKM  = DFT_distanceKM;
    private GeoPoint    lastGeoPoint    = DFT_lastGeoPoint;
    private String      driverID        = DFT_driver;

    /**
    *** Default constructor (disabled)
    **/
    public DTELogState()
    {
        this.init(null);
    }

    /**
    *** Constructor
    **/
    public DTELogState(RTProperties rtp)
    {
        this.init(rtp);
    }

    /**
    *** Constructor
    **/
    public DTELogState(String rtpStr)
    {
        this.init(new RTProperties(rtpStr));
    }            

    /**
    *** Constructor
    **/
    public DTELogState(long lastTS, int lastSC, boolean isDriving, double distKM, GeoPoint lastGP, String driverID)
    {
        this.init(lastTS, lastSC, isDriving, distKM, lastGP, driverID);
    }

    /** [DBFieldType interface]
    *** Constructor
    **/
    public DTELogState(ResultSet rs, String fldName)
        throws SQLException
    {
        super(rs, fldName);
        String elp = (rs != null)? rs.getString(fldName) : null;
        if (!StringTools.isBlank(elp)) {
            this.init(new RTProperties(elp));
        } else {
            this.init(null);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Initializes values
    **/
    private void init(long lastTS, int lastSC, boolean isDriving, double distKM, GeoPoint lastGP, String driverID) 
    {
        this.setLastTimestamp(lastTS);
        this.setLastStatusCode(lastSC);
        this.setIsDriving(isDriving);
        this.setLastDistanceKM(distKM);
        this.setLastGeoPoint(lastGP);
        this.setDriverID(driverID);
    }

    /**
    *** Initializes values
    **/
    public void init(RTProperties rtp) 
    {
        if (rtp != null) {
            this.init(
                rtp.getLong(    KEY_lastTimestamp , DFT_lastTimestamp),
                rtp.getInt(     KEY_lastStatusCode, DFT_lastStatusCode),
                rtp.getBoolean( KEY_isDriving     , DFT_isDriving),
                rtp.getDouble(  KEY_distanceKM    , DFT_distanceKM),
                GeoPoint.NewGeoPoint(rtp,KEY_lastGeoPoint,DFT_lastGeoPoint),
                rtp.getString(  KEY_driver        , DFT_driver)
                );
        } else {
            this.clear();
        }
    }

    /**
    *** Clears all values to their default
    **/
    public void clear()
    {
        this.init(
            DFT_lastTimestamp,
            DFT_lastStatusCode,
            DFT_isDriving,
            DFT_distanceKM,
            DFT_lastGeoPoint,
            DFT_driver
            );
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the last timestamp 
    **/
    public long getLastTimestamp()
    {
        if ((this.lastTimestamp <= 0L) && AUTO_INIT_TIMESTAMP) {
            this.lastTimestamp = DateTime.getCurrentTimeSec() - 1L;
        }
        return this.lastTimestamp; // may be "0"
    }

    /**
    *** Sets the last timestamp 
    **/
    public void setLastTimestamp(long lastTS)
    {
        this.lastTimestamp = lastTS;
    }

    /**
    *** Returns true if last timestamp is defined
    **/
    public boolean hasLastTimestamp()
    {
        return (this.getLastTimestamp() > 0L)? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last status-code 
    **/
    public int getLastStatusCode()
    {
        return this.lastStatusCode;
    }

    /**
    *** Sets the last status-code 
    **/
    public void setLastStatusCode(int lastSC)
    {
        this.lastStatusCode = lastSC;
    }

    /**
    *** Returns true if the last status-code is defined
    **/
    public boolean hasLastStatusCode()
    {
        return (this.getLastStatusCode() > 0)? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Set Last timestamp and status-code per specified EventData instance
    **/
    public void setLastEventData(EventData ev)
    {
        if (ev != null) {
            this.setLastTimestamp(ev.getTimestamp());
            this.setLastStatusCode(ev.getStatusCode());
          //this.setIsDriving(...);                 <== left as-is
          //this.setLastDistanceKM(...);            <== left as-is
            this.setLastGeoPoint(ev.getGeoPoint());
            this.setDriverID(ev.getDriverID());
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the driving state 
    **/
    public boolean getIsDriving()
    {
        return this.isDriving;
    }

    /**
    *** Sets the driving state 
    **/
    public void setIsDriving(boolean isDriving)
    {
        this.isDriving = isDriving;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the distance travelled, in Kilometers
    **/
    public double getLastDistanceKM()
    {
        return this.lastDistanceKM;
    }

    /**
    *** Sets the distance travelled, in Kilometers
    **/
    public void setLastDistanceKM(double distKM)
    {
        this.lastDistanceKM = distKM;
    }

    /**
    *** Adds the specified distance to the accumulator, in Kilometers
    **/
    public void addLastDistanceKM(double deltaKM)
    {
        this.lastDistanceKM += deltaKM;
    }

    // --------------------------------

    /**
    *** Gets the distance travelled, in Miles
    **/
    public double getLastDistanceMI()
    {
        return this.getLastDistanceKM() * GeoPoint.MILES_PER_KILOMETER;
    }

    /**
    *** Sets the distance travelled, in Miles
    **/
    public void setLastDistanceMI(double distMI)
    {
        this.setLastDistanceKM(distMI * GeoPoint.KILOMETERS_PER_MILE);
    }

    /**
    *** Adds the specified distance to the accumulator, in Miles
    **/
    public void addLastDistanceMI(double deltaMI)
    {
        this.addLastDistanceKM(deltaMI * GeoPoint.KILOMETERS_PER_MILE);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the last GeoPoint is defined/valid
    **/
    public boolean hasLastGeoPoint()
    {
        return GeoPoint.isValid(this.getLastGeoPoint());
    }

    /**
    *** Gets the last GeoPoint.
    *** Will return null if not defined/specified/invalid
    **/
    public GeoPoint getLastGeoPoint()
    {
        return this.lastGeoPoint;
    }

    /**
    *** Sets the last GeoPoint.
    **/
    public void setLastGeoPoint(GeoPoint lastGP)
    {
        this.lastGeoPoint = lastGP;
    }

    // ------------------------------------------------------------------------

    /**
    *** Return true if driver-id is defined
    **/
    public boolean hasDriverID()
    {
        return !StringTools.isBlank(this.getDriverID())? true : false;
    }
    
    /** 
    *** Gets the driver-id
    **/
    public String getDriverID()
    {
        return StringTools.trim(this.driverID);
    }
    
    /**
    *** Sets the driver-id
    **/
    public void setDriverID(String drvID)
    {
        this.driverID = StringTools.trim(drvID);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the RTProperties representation of this instance
    **/
    public RTProperties getRTProperties()
    {
        RTProperties rtp = new RTProperties();
        rtp.setLong(KEY_lastTimestamp, this.getLastTimestamp());
        if (this.hasLastStatusCode()) {
            rtp.setHexInt( KEY_lastStatusCode , this.getLastStatusCode(), 16);
            rtp.setBoolean(KEY_isDriving      , this.getIsDriving());
            rtp.setDouble( KEY_distanceKM     , this.getLastDistanceKM());
            if (this.hasLastGeoPoint()) {
                rtp.setString(KEY_lastGeoPoint, this.getLastGeoPoint().toString());
            }
            if (this.hasDriverID()) {
                rtp.setString(KEY_driver      , this.getDriverID());
            }
        }
        return rtp;
    }

    /**
    *** Gets the String representation of this instance
    **/
    public String toString()
    {
        // "lastTS=1432734421 lastSC=0xF401 isDriving=true distKM=0.0 lastGP=37.78352/-121.22567"
        return this.getRTProperties().toString();
    }

    // ------------------------------------------------------------------------

    /** [DBFieldType interface]
    *** Return Object as saved in the DB
    **/
    public Object getObject()
    {
        return this.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified Object is equal to this instance
    **/
    public boolean equals(Object other)
    {
        if (other instanceof DTELogState) {
            DTELogState es = (DTELogState)other;
            return this.toString().equals(es.toString());
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
    }

}
