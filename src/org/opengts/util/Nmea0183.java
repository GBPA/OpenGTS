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
//  NMEA-0183 $GP records, currently the following types are supported:
//      - $GPRMC: Recommended Minimum Specific GPS/TRANSIT Data
//      - $GPGGA: Global Positioning System Fix Data
//      - $GPVTG: Track Made Good and Ground Speed
//      - $GPZDA: UTC Date/Time and Local Time Zone Offset
//  Custom $GT records, current the following types are supported:
//      - $GTUID: Unique-ID only
//      - $GTSTC: Status-Code only
//      - $GTEVT: GTS Event (timestamp,statusCode,latitude/longitude,etc)
// References:
//  http://www.scientificcomponent.com/nmea0183.htm
//  http://home.mira.net/~gnb/gps/nmea.html
// ----------------------------------------------------------------------------
// Change History:
//  2007/07/27  Martin D. Flynn
//     -Initial release
//  2007/09/16  Martin D. Flynn
//     -Added 'getExtraData' method to return data following checksum.
//  2008/02/10  Martin D. Flynn
//     -Added handling of $GPVTG and $GPZDA record types
//     -Support parsing and combining multiple record types
//  2008/08/07  Martin D. Flynn
//     -Changed private '_calcChecksum' to public static 'calcXORChecksum'
//  2010/09/09  Martin D. Flynn
//     -Added ability to specify an array of records for parsing.
//  2010/10/21  Martin D. Flynn
//     -Added specific field checks: hasLatitude, hasLongitude, hasSpeed, etc.
//     -"getExtraData" now returns a String array.
//  2011/06/16  Martin D. Flynn
//     -Added "getIgnoreInvalidGpsFlag"/"setIgnoreInvalidGpsFlag" methods to
//      allow ignoring the A|V (valid|invalid) flag (yes, some actually want this)
//  2011/10/03  Martin D. Flynn
//     -Added check for valid data/time on GPRMC record.
//  2011/12/06  Martin D. Flynn
//     -Added "parseFixtime" method
//  2015/05/03  Martin D. Flynn
//     -Added support for parsing custom "$GTUID" (unique id) records [2.5.9-B43]
//     -Added support for parsing custom "$GTSTC" (status code) records
//     -Added support for parsing custom "$GTEVT" (event) records
//     -Added callbacks for subclassing to support additional custom record types.
//      "parseCustomReord" and "appendCustomTypes" intended for subclass override.
//  2016/04/15  Martin D. Flynn
//     -Fixed "ParseLongitude(...)" [2.6.2-B57]
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** A container for a NMEA-0183 record
**/

public class Nmea0183
{

    // ------------------------------------------------------------------------

    /* version string (added 2.5.9-B04) */
    public static final String  VERSION                 = "1.2.1"; // [2.5.9-B43]

    // ------------------------------------------------------------------------

    /* record types */
    public static final String  NMEA_GP_                = "$GP";
    public static final String  CUSTOM_GT_              = "$GT";

    /* NMEA type names */
    public static final String  NAME_NONE               = "NONE";
    public static final String  NAME_GPRMC              = "GPRMC";
    public static final String  NAME_GPGGA              = "GPGGA";
    public static final String  NAME_GPVTG              = "GPVTG";
    public static final String  NAME_GPZDA              = "GPZDA";
    // --
    public static final String  NAME_GTUID              = "GTUID";
    public static final String  NAME_GTSTC              = "GTSTC";
    public static final String  NAME_GTEVT              = "GTEVT";

    /* $type names */
    public static final String  DNAME_GPRMC             = "$" + NAME_GPRMC;
    public static final String  DNAME_GPGGA             = "$" + NAME_GPGGA;
    public static final String  DNAME_GPVTG             = "$" + NAME_GPVTG;
    public static final String  DNAME_GPZDA             = "$" + NAME_GPZDA;
    // --
    public static final String  DNAME_GTUID             = "$" + NAME_GTUID;
    public static final String  DNAME_GTSTC             = "$" + NAME_GTSTC;
    public static final String  DNAME_GTEVT             = "$" + NAME_GTEVT;

    // -- Note: these values may change between releases (stored in "this.parsedRcdTypes")
    public static final long    TYPE_NONE               = 0x0000000000000000L;
    public static final long    TYPE_GPRMC              = 0x0000000000000001L;
    public static final long    TYPE_GPGGA              = 0x0000000000000002L;
    public static final long    TYPE_GPVTG              = 0x0000000000000004L;
    public static final long    TYPE_GPZDA              = 0x0000000000000008L;
    // --
    public static final long    TYPE_GTUID              = 0x0000000000010000L;
    public static final long    TYPE_GTSTC              = 0x0000000000020000L;
    public static final long    TYPE_GTEVT              = 0x0000000000040000L;
    // --
    public static final long    TYPE_CUSTOM_1           = 0x0001000000000000L;
    public static final long    TYPE_CUSTOM_2           = 0x0002000000000000L;
    public static final long    TYPE_CUSTOM_3           = 0x0004000000000000L;
    public static final long    TYPE_CUSTOM_4           = 0x0008000000000000L;
    public static final long    TYPE_CUSTOM_5           = 0x0010000000000000L;
    public static final long    TYPE_CUSTOM_6           = 0x0020000000000000L;
    public static final long    TYPE_CUSTOM_7           = 0x0040000000000000L;
    public static final long    TYPE_CUSTOM_8           = 0x0080000000000000L;

    /**
    *** Gets the record type String from the type mask
    *** @return The record types read by this instance as a string
    **/
    public String getTypeNames()
    {
        String sep = ",";
        StringBuffer sb = new StringBuffer();
        // -- NMEA types
        if (this.hasGPRMC()) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GPRMC);
        }
        if (this.hasGPGGA()) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GPGGA);
        }
        if (this.hasGPVTG()) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GPVTG);
        }
        if (this.hasGPZDA()) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GPZDA);
        }
        // -- GTS types
        if (this.hasGTUID()) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GTUID);
        }
        if (this.hasGTSTC()) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GTSTC);
        }
        if (this.hasGTEVT()) {
            if (sb.length() > 0) { sb.append(sep); }
            sb.append(NAME_GTEVT);
        }
        // -- custom types
        this.appendCustomTypes(sb,sep);
        // -- return
        return (sb.length() > 0)? sb.toString() : NAME_NONE;
    }

    /**
    *** Callback to append any contained custom record types to the specified StringBuffer
    **/
    protected void appendCustomTypes(StringBuffer sb, String sep) 
    {
        // -- OVERRIDE/SUBCLASS
        // -- override in subclass to provide custom implementation
    }

    // ------------------------------------------------------------------------

    /* fields */
    // -- these should not be changed
    protected static final long     FIELD_RECORD_TYPE       = 0x0000000000000001L;
    protected static final long     FIELD_VALID_FIX         = 0x0000000000000002L;
    protected static final long     FIELD_DDMMYY            = 0x0000000000000004L;
    protected static final long     FIELD_HHMMSS            = 0x0000000000000008L;
    protected static final long     FIELD_LATITUDE          = 0x0000000000000010L;
    protected static final long     FIELD_LONGITUDE         = 0x0000000000000020L;
    protected static final long     FIELD_SPEED             = 0x0000000000000040L;
    protected static final long     FIELD_HEADING           = 0x0000000000000080L;
    protected static final long     FIELD_HDOP              = 0x0000000000000100L;
    protected static final long     FIELD_NUMBER_SATS       = 0x0000000000000200L;
    protected static final long     FIELD_ALTITUDE          = 0x0000000000000400L;
    protected static final long     FIELD_FIX_TYPE          = 0x0000000000000800L;
    protected static final long     FIELD_MAG_VARIATION     = 0x0000000000001000L;
    protected static final long     FIELD_RECORD_VERSION    = 0x0000000000002000L;
    // --
    protected static final long     FIELD_MOBILE_ID         = 0x0000000100000000L; // [2.5.9-B43] new
    protected static final long     FIELD_EVENT_CODE        = 0x0000000200000000L; // [2.5.9-B43] new
    protected static final long     FIELD_STATUS_CODE       = 0x0000000400000000L; // [2.5.9-B43] changed
    protected static final long     FIELD_GPS_AGE           = 0x0000000800000000L;

    // ------------------------------------------------------------------------

    public   static final double    KILOMETERS_PER_KNOT     = 1.85200000;
    public   static final double    KNOTS_PER_KILOMETER     = 1.0 / KILOMETERS_PER_KNOT;

    public   static final int       STATUS_NONE             = 0;
    public   static final int       STATUS_CODE_MASK        = 0xFFFF;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected boolean   validChecksum       = false;
    protected long      parsedRcdTypes      = TYPE_NONE;
    protected String    lastRcdType         = "";
    protected long      fieldMask           = 0L;

    protected long      ddmmyy              = 0L;
    protected long      hhmmss              = 0L;
    protected long      fixtime             = 0L;

    protected boolean   ignoreGpsFlag       = false;
    protected boolean   ignoredInvalidGPS   = false;
    protected boolean   isValidGPS          = false;
    protected double    latitude            = 0.0;
    protected double    longitude           = 0.0;
    protected GeoPoint  geoPoint            = null;

    protected double    speedKnots          = 0.0;
    protected double    heading             = 0.0;

    protected long      gpsAge              = 0L;
    protected double    hdop                = 0.0;
    protected int       numSats             = 0;
    protected double    altitudeM           = 0.0;
    protected int       fixType             = 0;

    protected double    magVariation        = 0.0;

    protected String    rcdVersion          = null;

    protected String    mobileID            = null;
    protected Object    eventCode           = null;
    protected int       statusCode          = STATUS_NONE; // undefined

    protected String    extraData[]         = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public Nmea0183()
    {
        super();
    }
    
    /**
    *** Constructor
    *** @param rcd The NMEA-0183 record
    **/
    public Nmea0183(String rcd)
    {
        this();
        this.parse(rcd, false);
    }
    
    /**
    *** Constructor
    *** @param rcds An array of NMEA-0183 records
    **/
    public Nmea0183(String rcds[])
    {
        this();
        this.parse(rcds, false);
    }

    /**
    *** Constructor
    *** @param rcd The NMEA-0183 record
    *** @param ignoreChecksum True if the record's checksum is to be ignored
    **/
    public Nmea0183(String rcd, boolean ignoreChecksum)
    {
        this();
        this.parse(rcd, ignoreChecksum);
    }

    /**
    *** Constructor
    *** @param rcds An array of NMEA-0183 records
    *** @param ignoreChecksum True if the record's checksum is to be ignored
    **/
    public Nmea0183(String rcds[], boolean ignoreChecksum)
    {
        this();
        this.parse(rcds, ignoreChecksum);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets whether the A|V (valid|invalid) GPS location flag should be ignored.<br>
    *** Setting to "true" is not recommended as invalid GPS locations may be returned.
    **/
    public void setIgnoreInvalidGpsFlag(boolean ignore)
    {
        this.ignoreGpsFlag = ignore;
        this.ignoredInvalidGPS = false;
    }

    /**
    *** gets whether the A|V (valid|invalid) GPS location flag should be ignored.<br>
    *** @return True if the GPS A|V flag should be ignored, false otherwise.
    **/
    public boolean getIgnoreInvalidGpsFlag()
    {
        return this.ignoreGpsFlag;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the mask of available fields
    *** @return The mask of available fields
    **/
    public long getFieldMask()
    {
        return this.fieldMask;
    }
    
    public void setFieldMask(long fld)
    {
        this.fieldMask |= fld;
    }
    
    public void clearFieldMask(long fld)
    {
        this.fieldMask &= ~fld;
    }

    /**
    *** Returns true if specified field is available
    *** @return True if specified field is available
    **/
    public boolean hasField(long fld)
    {
        return ((this.fieldMask & fld) != 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance has at least one parsed record type
    *** @return all parsed record types (mask)
    **/
    public boolean hasParsedRecordTypes()
    {
        return (this.parsedRcdTypes != Nmea0183.TYPE_NONE);
    }

    /**
    *** Gets all parsed record types (mask)
    *** @return all parsed record types (mask)
    **/
    protected long getParsedRecordTypes()
    {
        return this.parsedRcdTypes;
    }

    /**
    *** Adds the parsed record types (mask)
    *** @param type  The parsed record type to add to the parsed record type bitmask
    *** @param typeS  A String representation of the parsed record type
    **/
    protected void setParsedRecordType(long type, String typeS)
    {
        this.parsedRcdTypes |= type;
        this.lastRcdType     = typeS;
        this.setFieldMask(Nmea0183.FIELD_RECORD_TYPE);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last record type
    *** @return The last record type
    **/
    public String getLastRecordType()
    {
        return this.lastRcdType;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the checksum is valid
    *** @return True if the checksum is valid
    **/
    public boolean isValidChecksum()
    {
        return this.validChecksum;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the day/month/year of the fix
    *** @return The day/month/year of the fix
    **/
    public long getDDMMYY()
    {
        // this.hasField(Nmea0183.FIELD_DDMMYY)?
        return (this.ddmmyy > 0L)? this.ddmmyy : 0L;
    }

    /**
    *** Sets the day/month/year
    *** @param ddmmyy The day/month/year [CHECK](as what?)
    **/
    public void setDDMMYY(long ddmmyy)
    {
        if ((ddmmyy >= 10100L) && (ddmmyy <= 311299L)) { // day/month must be specified
            this.ddmmyy = ddmmyy;
            this.setFieldMask(Nmea0183.FIELD_DDMMYY);
        } else {
            this.ddmmyy = 0L;
            this.clearFieldMask(Nmea0183.FIELD_DDMMYY);
        }
    }

    // --------------------------------

    /**
    *** Gets the hour/minute/seconds of the fix
    *** @return The hour/minute/seconds of the fix 
    **/
    public long getHHMMSS()
    {
        // this.hasField(Nmea0183.FIELD_HHMMSS)?
        return (this.hhmmss >= 0L)? this.hhmmss : 0L;
    }

    /**
    *** Sets the hours/minutes/seconds
    *** @param hhmmss The ours/minutes/seconds
    **/
    public void setHHMMSS(long hhmmss)
    {
        if ((hhmmss >= 0L) && (hhmmss < 240000L)) {
            this.hhmmss = hhmmss;
            this.setFieldMask(Nmea0183.FIELD_HHMMSS);
        } else {
            this.hhmmss = 0L;
            this.clearFieldMask(Nmea0183.FIELD_HHMMSS);
        }
    }

    // --------------------------------

    /**
    *** Returns true if the fixtime has been defined
    *** @return True if the fixtime has been defined
    **/
    public boolean hasFixtime()
    {
        if (this.fixtime > 0L) {
            // -- fixtime has been previously set
            return true;
        } else {
            return this.hasField(Nmea0183.FIELD_DDMMYY) && this.hasField(Nmea0183.FIELD_HHMMSS);
        }
    }

    /**
    *** Gets the epoch fix time
    *** @return the epoch fix time 
    **/
    public long getFixtime()
    {
        return this.getFixtime(false);
    }

    /**
    *** Gets the epoch fix time
    *** @return the epoch fix time 
    **/
    public long getFixtime(boolean dftToCurrentTOD)
    {
        if (this.fixtime <= 0L) {
            // -- fix time not yet set
            long DMY = this.hasField(Nmea0183.FIELD_DDMMYY)? this.ddmmyy : -1L;
            long HMS = this.hasField(Nmea0183.FIELD_HHMMSS)? this.hhmmss : -1L;
            this.fixtime = Nmea0183.parseFixtime(DMY, HMS, dftToCurrentTOD);
        }
        return this.fixtime;
    }

    /**
    *** Sets the epoch fix time (relative to GMT)
    *** @param timestamp the epoch fix time 
    **/
    public void setFixtime(long timestamp)
    {
        this.fixtime = timestamp;
        if (this.fixtime > 0L) {
            TimeZone tz = DateTime.GMT;
            DateTime dt = new DateTime(this.fixtime, tz);
            // -- HHMMSS
            int  hh  = dt.getHour24(tz);
            int  mm  = dt.getMinute(tz);
            int  ss  = dt.getSecond(tz);
            long hms = ((long)hh * 10000L) + ((long)mm * 100L) + (long)ss;
            this.setHHMMSS(hms);
            // -- DDMMYY
            int  DD  = dt.getDayOfMonth(tz);
            int  MM  = dt.getMonth1(tz);
            int  YY  = dt.getYear(tz) & 100;
            long DMY = ((long)DD * 10000L) + ((long)MM * 100L) + (long)YY;
            this.setDDMMYY(DMY);
        } else {
            this.setHHMMSS(-1L);
            this.setDDMMYY(-1L);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse valid GPS indicator "A"/"V"
    **/
    protected boolean _parseValidGPSIndicator(String AV)
    {
        boolean validGPS;
        if (StringTools.isBlank(AV)) {
            // -- unknown
            Print.logWarn("Unexpected valid GPS fix indicator: " + AV);
            validGPS = true; // assume valid for now (validated below)
        } else
        if (AV.equals("A")) {
            // -- vAlid
            validGPS = true;
        } else
        if (this.getIgnoreInvalidGpsFlag()) {
            // -- forced valid
            this.ignoredInvalidGPS = true;
            validGPS = true;
        } else
        if (AV.equals("V")) {
            // -- inValid
            validGPS = false;
        } else
        if (AV.equals("L")) {
            // -- staLe?
            Print.logWarn("Unexpected valid GPS fix indicator: " + AV);
            validGPS = true; // assume "staLe" means valid, but old
        } else {
            // -- unknown
            Print.logWarn("Unexpected valid GPS fix indicator: " + AV);
            validGPS = true; // assume valid for now (validated below)
        }
        this.setFieldMask(Nmea0183.FIELD_VALID_FIX);
        return validGPS;
    }

    /**
    *** Returns true if the GPS fix is valid
    *** @return True if the GPS fix is valid
    **/
    public boolean isValidGPS()
    {
        return this.isValidGPS;
    }

    /**
    *** Returns true if ignoring invalid GPS flags, and the flag indicator was not "A"
    *** @return True if ignoring invalid GPS flags, and the flag indicator was not "A"
    **/
    public boolean didIgnoreInvalidGPS()
    {
        return this.isValidGPS() && this.ignoredInvalidGPS;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the latitude has been defined
    *** @return Ttrue if the latitude has have been defined
    **/
    public boolean hasLatitude()
    {
        if (this.latitude != 0.0) {
            // -- valid latitude previously set
            return true;
        } else {
            return this.hasField(Nmea0183.FIELD_LATITUDE);
        }
    }

    /**
    *** Gets the latitude
    *** @return The latitude
    **/
    public double getLatitude()
    {
        return this.latitude;
    }

    // --------------------------------

    /**
    *** Returns true if the longitude has been defined
    *** @return Ttrue if the longitude has have been defined
    **/
    public boolean hasLongitude()
    {
        if (this.longitude != 0.0) {
            // -- valid longitude previously set
            return true;
        } else {
            return this.hasField(Nmea0183.FIELD_LONGITUDE);
        }
    }

    /**
    *** Gets the longitude
    *** @return The longitude
    **/
    public double getLongitude()
    {
        return this.longitude;
    }

    // --------------------------------

    /**
    *** Returns true if the latitude/longitude have been defined
    *** @return Ttrue if the latitude/longitude have been defined
    **/
    public boolean hasGeoPoint()
    {
        if (this.geoPoint != null) {
            // -- valid GeoPoint previously initialized
            return true;
        } else {
            return this.hasLatitude() && this.hasLongitude();
        }
    }

    /**
    *** Gets the lat/lon as a GeoPoint
    *** @return the lat/lon as a GeoPoint
    **/
    public GeoPoint getGeoPoint()
    {

        /* already has a cached GeoPoint? */
        if (this.geoPoint != null) {
            return this.geoPoint;
        }

        /* valid lat,lon? */
        double lat = this.getLatitude();
        double lon = this.getLongitude();
        if (GeoPoint.isValid(lat,lon)) {
            // -- cache and return valid GeoPoint
            this.geoPoint = new GeoPoint(lat, lon);
            return this.geoPoint;
        } else {
            // -- do not cache, return invalid GeoPoint
            return new GeoPoint(lat,lon);
        }

    }

    /**
    *** Sets the lat/lon as a GeoPoint
    *** @param gp  The GeoPoint
    **/
    public void setGeoPoint(GeoPoint gp)
    {
        if (GeoPoint.isValid(gp)) {
            this.latitude   = gp.getLatitude();
            this.setFieldMask(Nmea0183.FIELD_LATITUDE);
            this.longitude  = gp.getLongitude();
            this.setFieldMask(Nmea0183.FIELD_LONGITUDE);
            this.isValidGPS = true;
            this.geoPoint   = gp;
        } else {
            this.latitude   = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_LATITUDE);
            this.longitude  = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_LONGITUDE);
            this.isValidGPS = false;
            this.geoPoint   = null;
            this.ignoredInvalidGPS = false; // in case it was set true previously
        }
    }

    /**
    *** Sets the lat/lon as a GeoPoint
    *** @param lat  The Latitude
    *** @param lon  The Longitude
    **/
    public void setGeoPoint(double lat, double lon)
    {
        if (GeoPoint.isValid(lat,lon)) {
            this.latitude   = lat;
            this.setFieldMask(Nmea0183.FIELD_LATITUDE);
            this.longitude  = lon;
            this.setFieldMask(Nmea0183.FIELD_LONGITUDE);
            this.isValidGPS = true;
            this.geoPoint   = null; // initialized later
        } else {
            this.latitude   = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_LATITUDE);
            this.longitude  = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_LONGITUDE);
            this.isValidGPS = false;
            this.geoPoint   = null; // reset
            this.ignoredInvalidGPS = false; // in case it was set true previously
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns true if the speed has been defined
    *** @return True if the speed has been defined
    **/
    public boolean hasSpeed()
    {
        return this.hasField(Nmea0183.FIELD_SPEED);
    }

    /**
    *** Returns the speed in knots 
    *** @return The speed in knots
    **/
    public double getSpeedKnots()
    {
        return (this.speedKnots >= 0.0)? this.speedKnots : 0.0;
    }

    /**
    *** Gets the speed in KPH
    *** @return The speed in KPH
    **/
    public double getSpeedKPH()
    {
        return this.getSpeedKnots() * KILOMETERS_PER_KNOT;
    }

    /**
    *** Sets the speed value, in Knots
    **/
    public void setSpeedKnots(double knots)
    {
        if (knots >= 0.0) {
            this.speedKnots = knots;
            this.setFieldMask(Nmea0183.FIELD_SPEED);
        } else {
            this.speedKnots = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_SPEED);
        }
    }

    /**
    *** Sets the speed value, in km/h
    **/
    public void setSpeedKPH(double kph)
    {
        this.setSpeedKnots(kph * KNOTS_PER_KILOMETER);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the heading has been defined
    *** @return True if the heading has been defined
    **/
    public boolean hasHeading()
    {
        return this.hasField(Nmea0183.FIELD_HEADING);
    }

    /**
    *** Gets the heading/course in degrees
    *** @return The heading/course in degrees
    **/
    public double getHeading()
    {
        return (this.heading > 0.0)? this.heading : 0.0;
    }

    /**
    *** Sets the heading value, in degrees
    **/
    public void setHeading(double dir)
    {
        if (dir >= 0.0) {
            this.heading = dir;
            this.setFieldMask(Nmea0183.FIELD_HEADING);
        } else {
            this.heading = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_HEADING);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the fix type has been defined
    *** @return True if the fix type has been defined
    **/
    public boolean hasFixType()
    {
        return this.hasField(Nmea0183.FIELD_FIX_TYPE);
    }

    /** 
    *** Gets the "$GPGGA" fix type (0=no fix, 1=GPS, 2=DGPS, 3=PPS?, 6=dead-reckoning)
    *** @return The "$GPGGA fix type
    **/
    public int getFixType()
    {
        return this.fixType;
    }

    /**
    *** Sets the fix type
    **/
    public void setFixType(int ft)
    {
        if (ft > 0) {
            this.fixType = ft;
            this.setFieldMask(Nmea0183.FIELD_FIX_TYPE);
        } else {
            this.fixType = 0;
            this.clearFieldMask(Nmea0183.FIELD_FIX_TYPE);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the number of satellites has been defined
    *** @return True if the number of satellites has been defined
    **/
    public boolean hasNumberOfSatellites()
    {
        if (this.numSats > 0) {
            // -- valid #Sats previously set
            return true;
        } else {
            return this.hasField(Nmea0183.FIELD_NUMBER_SATS);
        }
    }

    /** 
    *** Gets the number of satellites used in fix
    *** @return The number of satellites used in fix
    **/
    public int getNumberOfSatellites()
    {
        return (this.numSats > 0)? this.numSats : 0;
    }

    /**
    *** Sets the number of satellites in view
    **/
    public void setNumberOfSatellites(int sats)
    {
        if (sats > 0) {
            this.numSats = sats;
            this.setFieldMask(Nmea0183.FIELD_NUMBER_SATS);
        } else {
            this.numSats = 0;
            this.clearFieldMask(Nmea0183.FIELD_NUMBER_SATS);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the HDOP has been defined
    *** @return True if the HDOP has been defined
    **/
    public boolean hasHDOP()
    {
        if (this.hdop >= 0.0) {
            // -- valid HDOP previously set
            return true;
        } else {
            return this.hasField(Nmea0183.FIELD_HDOP);
        }
    }

    /**
    *** Gets the horizontal-dilution-of-precision
    *** @return The horizontal-dilution-of-precision
    **/
    public double getHDOP()
    {
        return (this.hdop >= 0.0)? this.hdop : 0.0;
    }

    /**
    *** Sets the HDOP value
    **/
    public void setHDOP(double dop)
    {
        if (dop >= 0.0) {
            this.hdop = dop;
            this.setFieldMask(Nmea0183.FIELD_HDOP);
        } else {
            this.hdop = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_HDOP);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the altitude has been defined
    *** @return True if the altitude has been defined
    **/
    public boolean hasAltitude()
    {
        return this.hasField(Nmea0183.FIELD_ALTITUDE);
    }

    /**
    *** Gets the altitude in meters
    *** @return The altitude in meters
    **/
    public double getAltitudeMeters()
    {
        return this.altitudeM;
    }

    /**
    *** Sets the magnetic variation in degrees
    *** @param alt The magnetic variation in degrees
    **/
    public void setAltitudeMeters(double alt)
    {
        if ((alt > -20000.0) && (alt < 50000.0)) {
            this.altitudeM = alt;
            this.setFieldMask(Nmea0183.FIELD_ALTITUDE);
        } else {
            this.altitudeM = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_ALTITUDE);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the magnetic variation has been defined
    *** @return True if the magnetic variation has been defined
    **/
    public boolean hasMagneticVariation()
    {
        return this.hasField(Nmea0183.FIELD_MAG_VARIATION);
    }

    /**
    *** Gets the magnetic variation in degrees
    *** @return The magnetic variation in degrees
    **/
    public double getMagneticVariation()
    {
        return this.magVariation;
    }

    /**
    *** Sets the magnetic variation in degrees
    *** @param mv The magnetic variation in degrees
    **/
    public void setMagneticVariation(double mv)
    {
        if ((mv > -180.0) && (mv < 180.0)) {
            this.magVariation = mv;
            this.setFieldMask(Nmea0183.FIELD_MAG_VARIATION);
        } else {
            this.magVariation = 0.0;
            this.clearFieldMask(Nmea0183.FIELD_MAG_VARIATION);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the record version has been defined
    *** @return True if the record version has been defined
    **/
    public boolean hasRecordVersion()
    {
        return this.hasField(Nmea0183.FIELD_RECORD_VERSION);
    }

    /**
    *** Gets the record version
    *** @return The record version
    **/
    public String getRecordVersion()
    {
        return (this.rcdVersion != null)? this.rcdVersion : "";
    }

    /**
    *** Sets the record version
    **/
    public void setRecordVersion(String vers)
    {
        if (!StringTools.isBlank(vers)) {
            this.rcdVersion = StringTools.trim(vers);
            this.setFieldMask(Nmea0183.FIELD_RECORD_VERSION);
        } else {
            this.rcdVersion = null;
            this.clearFieldMask(Nmea0183.FIELD_RECORD_VERSION);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a mobile-id is defined
    *** @return True if the mobile-id has been defined
    **/
    public boolean hasMobileID()
    {
        return !StringTools.isBlank(this.mobileID);
      //return this.hasField(Nmea0183.FIELD_MOBILE_ID);
    }

    /**
    *** Gets the mobile-id
    *** @return The mobile-id (or null if undefined)
    **/
    public String getMobileID()
    {
        return this.hasMobileID()? this.mobileID : null;
    }

    /**
    *** Sets the mobile-id
    **/
    public void setMobileID(String id)
    {
        if (!StringTools.isBlank(id)) {
            this.mobileID = id.trim();
            this.setFieldMask(Nmea0183.FIELD_MOBILE_ID);
        } else {
            this.mobileID = null;
            this.clearFieldMask(Nmea0183.FIELD_MOBILE_ID);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified event code is non-null/blank
    **/
    public boolean isEventCode(Object ec)
    {
        if (ec == null) {
            // -- null
            return false;
        } else
        if ((ec instanceof String) && StringTools.isBlank((String)ec)) {
            // -- blank
            return false;
        } else {
            // -- not null/blank
            return true;
        }
    }

    /**
    *** Returns true if an event-code has been defined
    *** @return True if an event-code has been defined
    **/
    public boolean hasEventCode()
    {
        //return this.hasField(Nmea0183.FIELD_EVENT_CODE);
        return this.isEventCode(this.eventCode);
    }

    /**
    *** Gets the event-code
    *** @return The event-code (may be null)
    **/
    public Object getEventCode()
    {
        return this.eventCode; // may be null
    }


    /**
    *** Sets the event-code
    **/
    public void setEventCode(Object ec)
    {
        if (this.isEventCode(ec)) {
            this.eventCode = (ec instanceof String)? ((String)ec).trim() : ec;
            this.setFieldMask(Nmea0183.FIELD_EVENT_CODE);
        } else {
            this.eventCode = null;
            this.clearFieldMask(Nmea0183.FIELD_EVENT_CODE);
        }
    }
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the status-code has been defined
    *** @return True if the status-code has been defined
    **/
    public boolean hasStatusCode()
    {
        if (this.statusCode > 0) { // > STATUS_NONE
            // -- statusCode previously set
            return true;
        } else {
          //return this.hasField(Nmea0183.FIELD_STATUS_CODE); <-- not good enough, must also be valid
            return false;
        }
    }

    /**
    *** Gets the status-code
    *** @return The status-code
    **/
    protected int _getStatusCode()
    {
        return this.statusCode;
    }

    /**
    *** Gets the status-code
    *** @return The status-code
    **/
    public int getStatusCode()
    {
        // -- OVERRIDE/SUBCLASS
        // -- calls "_getStatusCode" to allow subclassing without causing recursion
        // -  when calling "translateEventCodeToStatusCode" in the subclass.
        return this._getStatusCode();
        // -- subclass may define "eventCodeMap" and call the following:
        // -  return this.translateEventCodeToStatusCode(eventCodeMap);
    }

    /**
    *** Sets the status-code
    **/
    public void setStatusCode(int sc)
    {

        /* invalid/none status code? */
        if (sc <= 0) {
            this.statusCode = STATUS_NONE;
            this.clearFieldMask(Nmea0183.FIELD_STATUS_CODE);
            return;
        }

        /* apply mask and check again */
        sc &= STATUS_CODE_MASK;
        if (sc <= 0) {
            this.statusCode = STATUS_NONE;
            this.clearFieldMask(Nmea0183.FIELD_STATUS_CODE);
            return;
        }

        /* save status code */
        this.statusCode = sc;
        this.setFieldMask(Nmea0183.FIELD_STATUS_CODE);

    }

    /**
    *** Translates the event-code to status-code, using the specified map.
    *** Once translated, the resulting status code will be cached and returned on
    *** subsequent calls to this method or "<code>getStatusCode</code>".
    *** If the event-code is not found in the specified map, the status code will not
    *** be set, and 0 (STATUS_NONE) will be returned.
    *** @param evCodeMap  The Event-Code to Status-Code translation map
    *** @return The translated status-code.
    **/
    public int translateEventCodeToStatusCode(Map<Object,Integer> evCodeMap)
    {

        /* statusCode already cached? */
        if (this.hasStatusCode()) {
            // -- statusCode already defined
            this._getStatusCode();
        }

        /* check event code */
        Object ec = this.getEventCode();
        if (!this.isEventCode(ec)) {
            // -- no event-code to translate
            return STATUS_NONE;
        }

        /* no valid eventCode/statusCode map? */
        if (ListTools.isEmpty(evCodeMap)) {
            // -- empty event-code map
            return STATUS_NONE;
        }

        /* get mapped status-code */
        Integer sci = evCodeMap.get(ec);
        if (sci != null) {
            // -- event-code found
            int sc = sci.intValue();
            this.setStatusCode(sc);
            return this._getStatusCode(); // may still be STATUS_NONE
        }

        /* unable to translate event-code to status-code */
        // -- exit single-pass loop
        return STATUS_NONE;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the GPS age has been defined
    *** @return True if the GPS age has been defined
    **/
    public boolean hasGpsAge()
    {
        if (this.gpsAge > 0L) {
            // -- valid GPS age previously set
            return true;
        } else {
            return this.hasField(Nmea0183.FIELD_GPS_AGE);
        }
    }

    /**
    *** Gets the GPS age in seconds
    *** @return The GPS age in seconds
    **/
    public long getGpsAge()
    {
        return (this.gpsAge >= 0L)? this.gpsAge : 0L;
    }

    /**
    *** Sets the GPS age
    **/
    public void setGpsAge(long age)
    {
        if (age >= 0L) {
            this.gpsAge = age;
            this.setFieldMask(Nmea0183.FIELD_GPS_AGE);
        } else {
            this.gpsAge = 0L;
            this.clearFieldMask(Nmea0183.FIELD_GPS_AGE);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if extra-data was found
    *** @return True if extra-data was found
    **/
    public boolean hasExtraData()
    {
        return !ListTools.isEmpty(this.extraData);
    }

    /** 
    *** Gets any data that may follow the checksum
    *** @return Any data that may follow the checksum (may be null)
    **/
    public String[] getExtraData()
    {
        return this.extraData;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a string representation of this object
    *** @return A string representation of this object
    **/
    public StringBuffer toStringBuffer(StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        sb.append("RcdTypes : ").append(this.getTypeNames()).append("\n");
        if (!this.isValidChecksum()) { // <-- only if Checksum is invalid
            sb.append("Checksum : ").append(this.isValidChecksum()?"ok":"failed").append("\n");
        }
        if (this.hasMobileID()) {
            sb.append("MobileID : ").append(this.getMobileID()).append("\n");
        }
        if (this.hasEventCode()) {
            sb.append("EventCode: ").append(this.getEventCode()).append("\n");
        }
        if (this.hasStatusCode()) {
            sb.append("Status   : 0x").append(StringTools.toHexString(this.getStatusCode(),16)).append("\n");
        }
        if (this.hasFixtime()) {
            sb.append("Fixtime  : ").append(this.getFixtime()).append(" [").append(new DateTime(this.getFixtime()).toString()).append("]\n");
        }
        if (this.hasGeoPoint()) {
            sb.append("GPS      : ").append(this.isValidGPS()?"valid ":"invalid ").append(this.getGeoPoint().toString()).append("\n");
        }
        if (this.hasSpeed()) {
            sb.append("SpeedKPH : ").append(this.getSpeedKPH()).append(" kph");
            if (this.hasHeading()) {
                sb.append(", heading ").append(this.getHeading());
            }
            sb.append("\n");
        }
        if (this.hasAltitude()) {
            sb.append("Altitude : ").append(this.getAltitudeMeters()).append(" meters\n");
        }
        if (this.hasMagneticVariation()) {
            sb.append("MagVar   : ").append(this.getMagneticVariation()).append("\n");
        }
        if (this.hasRecordVersion()) {
            sb.append("RVersion : ").append(this.getRecordVersion()).append("\n");
        }
        return sb;
    }

    /**
    *** Returns a string representation of this object
    *** @return A string representation of this object
    **/
    public String toString()
    {
        return this.toStringBuffer(null).toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return a formatted $GPRMC record from the values in this instance
    *** @return A formatted $GPRMC record
    **/
    public String toGPRMC()
    {
        // $GPRMC,080701.00,A,3128.7540,N,14257.6714,W,000.0,000.0,180707,,,A*1C
        DateTime ft = new DateTime(this.getFixtime(), DateTime.GMT);
        GeoPoint gp = this.getGeoPoint();
        StringBuffer sb = new StringBuffer();
        sb.append("$").append(NAME_GPRMC);
        sb.append(",");
        sb.append(ft.format("HHmmss")).append(".00");
        sb.append(",");
        sb.append(this.isValidGPS()? "A" : "V");
        sb.append(",");
        sb.append(gp.getLatitudeString(GeoPoint.SFORMAT_NMEA,null));
        sb.append(",");
        sb.append(gp.getLongitudeString(GeoPoint.SFORMAT_NMEA,null));
        sb.append(",");
        sb.append(StringTools.format(this.getSpeedKnots(),"0.0"));
        sb.append(",");
        sb.append(StringTools.format(this.getHeading(),"0.0"));
        sb.append(",");
        sb.append(ft.format("ddMMyy"));
        sb.append(",");
        double magDeg = this.getMagneticVariation();
        if (magDeg != 0.0) {
            sb.append(StringTools.format(Math.abs(magDeg),"0.0")).append((magDeg >= 0.0)? ",E" : ",W");
        } else {
            // blank mag variation/direction
            sb.append(",");
        }
        sb.append(",A");
        int cksum = Nmea0183.calcXORChecksum(sb.toString(),false);
        sb.append("*");
        sb.append(StringTools.toHexString(cksum,8));
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parses a NMEA-0183 record
    *** @param rcds An array of NMEA-0183 records to parse
    *** @return True if this record was successfully parsed
    **/
    public boolean parse(String rcds[])
    {
        return this.parse(rcds, false);
    }

    /**
    *** Parses a NMEA-0183 record
    *** @param rcd the NMEA-0183 record to parse
    *** @return True if this record was successfully parsed
    **/
    public boolean parse(String rcd)
    {
        return this.parse(rcd, false);
    }

    /**
    *** Parses an array of NMEA-0183 records
    *** @param rcds An array of NMEA-0183 records to parse
    *** @param ignoreChecksum True to ignore the terminating checksum
    *** @return True if all records were successfully parsed
    **/
    public boolean parse(String rcds[], boolean ignoreChecksum)
    {
        if (!ListTools.isEmpty(rcds)) {
            boolean rtn = true;
            for (int i = 0; i < rcds.length; i++) {
                if (!this.parse(rcds[i], ignoreChecksum)) {
                    rtn = false;
                }
            }
            return rtn;
        } else {
            return false;
        }
    }

    /**
    *** Parses a NMEA-0183 record
    *** @param rcd the NMEA-0183 record to parse
    *** @param ignoreChecksum True to ignore the terminating checksum
    *** @return True if this record was successfully parsed
    **/
    public boolean parse(String rcd, boolean ignoreChecksum)
    {

        /* pre-validate */
        if (rcd == null) {
            Print.logError("Null record specified");
            return false;
        } else
        if (!rcd.startsWith("$")) {
            Print.logError("Invalid record (must begin with '$'): " + rcd);
            return false;
        }

        /* valid checksum? */
        if (ignoreChecksum) {
            this.validChecksum = true;
        } else {
            this.validChecksum = this._hasValidChecksum(rcd);
            if (!this.validChecksum) {
                Print.logError("Invalid Checksum: " + rcd);
                return false;
            }
        }

        /* parse into fields */
        String fld[] = StringTools.parseStringArray(rcd, ',');
        if ((fld == null) || (fld.length < 1)) {
            Print.logError("Insufficient fields: " + rcd);
            return false;
        }

        /* parse record type */
        if (fld[0].equals(DNAME_GPRMC)) {
            this.setParsedRecordType(TYPE_GPRMC,fld[0]);
            return this._parse_GPRMC(fld);
        } else
        if (fld[0].equals(DNAME_GPGGA)) {
            this.setParsedRecordType(TYPE_GPGGA,fld[0]);
            return this._parse_GPGGA(fld);
        } else
        if (fld[0].equals(DNAME_GPVTG)) {
            this.setParsedRecordType(TYPE_GPVTG,fld[0]);
            return this._parse_GPVTG(fld); // speed/heading
        } else
        if (fld[0].equals(DNAME_GPZDA)) {
            this.setParsedRecordType(TYPE_GPZDA,fld[0]);
            return this._parse_GPZDA(fld);
        }

        /* GTS records */
        if (fld[0].equals(DNAME_GTUID)) {
            this.setParsedRecordType(TYPE_GTUID,fld[0]);
            return this._parse_GTUID(fld);
        } else
        if (fld[0].equals(DNAME_GTSTC)) {
            this.setParsedRecordType(TYPE_GTSTC,fld[0]);
            return this._parse_GTSTC(fld);
        } else
        if (fld[0].equals(DNAME_GTEVT)) {
            this.setParsedRecordType(TYPE_GTEVT,fld[0]);
            return this._parse_GTEVT(fld);
        }

        /* custom records */
        if (this.parseCustomReord(fld)) {
            // -- custom record handler
            // -  subclass is responsible for calling "this.setParsedRecordType(type,fld[0])"
            return true;
            
        }

        /* record not supported */
        Print.logError("Record not supported: " + rcd);
        return false;
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Callback to parse custom record type
    **/
    protected boolean parseCustomReord(String fld[])
    {
        // -- OVERRIDE/SUBCLASS
        // -- override in subclass to provide custom implementation
        // -  this method is responsible for updating "this.parsedRcdTypes"
        //this.parsedRcdTypes |= TYPE_CUSTOM_#;
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if a $GPRMC record has been parsed
    **/
    public boolean hasGPRMC()
    {
        return ((this.parsedRcdTypes & TYPE_GPRMC) != 0L);
    }

    /**
    *** Parses "$GPRMC" 
    **/
    protected boolean _parse_GPRMC(String fld[])
    {
        // $GPRMC - Recommended Minimum Specific GPS/TRANSIT Data
        // Format 1:
        // $GPRMC,025423.494,A,3709.0642,N,14207.8315,W,7.094,108.52,200505,13.1,E*12,E
        // $GPRMC,1---------,2,3--------,4,5---------,6,7----,8-----,9-----,A---,B*MM,E
        //        |          | |         | |          | |     |      |      |    ||   |>Extra data (may not be present)
        //        |          | |         | |          | |     |      |      |    ||>Checksum
        //        |          | |         | |          | |     |      |      |    |>direction of magnetic variation ("E" = east, "W" = west)
        //        |          | |         | |          | |     |      |      |>Magnetic variation (degrees)
        //        |          | |         | |          | |     |      |>UTC Date: DDMMYY 
        //        |          | |         | |          | |     |>Course/Heading (degrees)
        //        |          | |         | |          | |>Speed (knots)
        //        |          | |         | |          |>Longitude hemisphere ("E" = eastern, "W" = western)
        //        |          | |         | |>Longitude: dddmm.mm
        //        |          | |         |>Latitude hemisphere ("N" = northern, "S" = southern)
        //        |          | |>Latitude: ddmm.mm
        //        |          |>GPS validity ("A"=valid, "V"=invalid)
        //        |>UTC Time: HHMMSS
        // Format 2:
        // $GPRMC,025423.494,A,3709.0642,N,14207.8315,W,7.094,108.52,200505,13.1,E,A*71
        // $GPRMC,1---------,2,3--------,4,5---------,6,7----,8-----,9-----,A---,B,C*MM,E
        //      C   Mode indicator, (A=Autonomous, D=Diff, E=Estimated, N=Not valid) 

        /* valid number of fields? */
        if (fld.length < 10) {
            return false;
        }

        /* valid GPS? */
        boolean validGPS = this._parseValidGPSIndicator(fld[2]);

        /* date */
        this.fixtime = 0L; // calculated later
        boolean hasDate = false;
        if (!fld[9].equals("000000")) {
            this.ddmmyy = StringTools.parseLong(fld[9], 0L);
            this.setFieldMask(Nmea0183.FIELD_DDMMYY);
            hasDate = true;
        }

        /* time */
        if (hasDate || !fld[1].equals("000000.000")) {
            // -- either we have a date, or the time is not "000000"
            this.hhmmss = StringTools.parseLong(fld[1], 0L);
            this.setFieldMask(Nmea0183.FIELD_HHMMSS);
        }

        /* latitude, longitude, speed, heading */
        if (validGPS) {
            double lat = Nmea0183.ParseLatitude (fld[3], fld[4],  90.0);
            double lon = Nmea0183.ParseLongitude(fld[5], fld[6], 180.0);
            if (GeoPoint.isValid(lat,lon)) {
                this.setGeoPoint(  lat,lon); // $GPRMC
                this.setSpeedKnots(StringTools.parseDouble(fld[7],-1.0));
                this.setHeading(   StringTools.parseDouble(fld[8],-1.0));
            } else {
                validGPS = false;
                this.setGeoPoint(0.0,0.0);
                this.setSpeedKnots(-1.0);
                this.setHeading(-1.0);
            }
        } else {
            this.setGeoPoint(0.0,0.0);
            this.setSpeedKnots(-1.0);
            this.setHeading(-1.0);
        }
        this.geoPoint = null; // reset (initialized later)
        this.isValidGPS = validGPS;

        /* magnetic variation */
        if (fld.length > 11) {
            double magDeg = StringTools.parseDouble(fld[10], 0.0);
            this.magVariation = fld[11].equalsIgnoreCase("W")? -magDeg : magDeg;
            this.setFieldMask(Nmea0183.FIELD_MAG_VARIATION);
        }

        /* extra data? */
        this.extraData = null;
        if (fld.length > 12) {
            int ePos = (fld[11].indexOf('*') >= 0)? 12 : ((fld.length > 13) && (fld[12].indexOf('*') >= 0))? 13 : 12;
            this.extraData = new String[fld.length - ePos];
            System.arraycopy(fld, ePos, this.extraData, 0, this.extraData.length);
            /*
            int eNdx = ePos + 1;
            if (fld.length == eNdx) {
                this.extraData = fld[ePos];
            } else {
                StringBuffer ed = new StringBuffer(fld[ePos]);
                for (int e = eNdx; e < fld.length; e++) {
                    ed.append(",").append(fld[e]);
                }
                this.extraData = ed.toString();
            }
            */
        }

        /* success */
        return true;

    }

    // ----------------------------------------------------------------------------

    /**
    *** Returns true if a $GPGGA record has been parsed
    **/
    public boolean hasGPGGA()
    {
        return ((this.parsedRcdTypes & TYPE_GPGGA) != 0L);
    }

    /**
    *** Parses "$GPGGA" 
    **/
    protected boolean _parse_GPGGA(String fld[])
    {
        // $GPGGA - Global Positioning System Fix Data
        // $GPGGA,015402.240,0000.0000,N,00000.0000,E,0,00,5.0,  0.0,M, 18.0,M,0.0,0000*4B
        // $GPGGA,025425.494,3509.0743,N,14207.6314,W,1,04,2.3,530.3,M,-21.9,M,0.0,0000*4D,
        // $GPGGA,1---------,2--------,3,4---------,5,6,7-,8--,9----,A,B----,C,D--,E---*MM,F
        //        |          |         | |          | | |  |   |     | |     | |   |   |   |>Extra data (may not be present)
        //        |          |         | |          | | |  |   |     | |     | |   |   |>Checksum
        //        |          |         | |          | | |  |   |     | |     | |   |>Differential reference station ID (always '0000')
        //        |          |         | |          | | |  |   |     | |     | |>Age of differential GPS
        //        |          |         | |          | | |  |   |     | |     |>Unit of Geoidal separation (meters)
        //        |          |         | |          | | |  |   |     | |>Geoidal separation (add to #9 to get WGS-84 ellipsoid height)
        //        |          |         | |          | | |  |   |     |>Unit of height, always 'M' meters
        //        |          |         | |          | | |  |   |>Height above/below mean geoid (above mean sea level, not WGS-84 ellipsoid height)
        //        |          |         | |          | | |  |>Horizontal Dilution of Precision
        //        |          |         | |          | | |>number of satellites (00-12)
        //        |          |         | |          | |>(0=no fix, 1=GPS, 2=DGPS, 3=PPS?, 6=dead-reckoning)
        //        |          |         | |          |>Longitude hemisphere ("E" = eastern, "W" = western)
        //        |          |         | |>Longitude: dddmm.mm
        //        |          |         |>Latitude hemisphere ("N" = northern, "S" = southern)
        //        |          |>Latitude: ddmm.mm
        //        |>UTC Time: HHMMSS

        /* valid number of fields? */
        if (fld.length < 14) {
            return false;
        }

        /* valid GPS? */
        boolean validGPS = !fld[6].equals("0");
        this.setFieldMask(Nmea0183.FIELD_VALID_FIX);

        /* date */
        this.fixtime = 0L; // calculated later
        this.ddmmyy  = 0L; // we don't know the day
        this.clearFieldMask(Nmea0183.FIELD_DDMMYY);

        /* time */
        this.hhmmss  = StringTools.parseLong(fld[1], 0L);
        this.setFieldMask(Nmea0183.FIELD_HHMMSS);

        /* latitude, longitude, altitude */
        if (validGPS) {
            double lat = Nmea0183.ParseLatitude (fld[2], fld[3],  90.0);
            double lon = Nmea0183.ParseLongitude(fld[4], fld[5], 180.0);
            if (GeoPoint.isValid(lat,lon)) {
                this.setGeoPoint(          lat,lon);  // $GPGGA
                this.setFixType(           StringTools.parseInt(   fld[6],    1));
                this.setNumberOfSatellites(StringTools.parseInt(   fld[7],   -1));
                this.setHDOP(              StringTools.parseDouble(fld[8], -1.0));
                this.setAltitudeMeters(    StringTools.parseDouble(fld[9],  0.0));
            } else {
                validGPS = false;
                this.setGeoPoint(0.0,0.0);
            }
        } else {
            this.setGeoPoint(          0.0,0.0);
            this.setFixType(                 0);
            this.setNumberOfSatellites(     -1);
            this.setHDOP(                 -1.0);
            this.setAltitudeMeters(    99999.9);
        }
        this.geoPoint = null; // reset (initialized later)
        this.isValidGPS = validGPS;

        /* extra data? */
        if (fld.length > 15) {
            int ePos = 15;
            this.extraData = new String[fld.length - ePos];
            System.arraycopy(fld, ePos, this.extraData, 0, this.extraData.length);
            /*
            if (fld.length == 16) {
                this.extraData = fld[15];
            } else {
                StringBuffer ed = new StringBuffer(fld[15]);
                for (int e = 16; e < fld.length; e++) {
                    ed.append(",").append(fld[e]);
                }
                this.extraData = ed.toString();
            }
            */
        } else {
            this.extraData = null;
        }

        /* success */
        return true;
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a $GPVTG record has been parsed
    **/
    public boolean hasGPVTG()
    {
        return ((this.parsedRcdTypes & TYPE_GPVTG) != 0L);
    }

    /**
    *** Parses "$GPVTG" (speed/heading)
    **/
    protected boolean _parse_GPVTG(String fld[])
    {
        // $GPVTG - Track Made Good and Ground Speed
        // $GPVTG,229.86,T, ,M,0.00,N,0.0046,K*55
        // $GPVTG,1-----,2,3,4,5---,6,7-----,8*MM
        //        |      | | | |    | |      ||>Checksum
        //        |      | | | |    | |      |>"K" ("KM/H")
        //        |      | | | |    | |>Speed over ground in KM/H
        //        |      | | | |    |>"N" ("Knots")
        //        |      | | | |>Speed over ground in Knots
        //        |      | | |>"M" ("Magnetic" course)
        //        |      | |>Magnetic course over ground, degrees
        //        |      |>"T" ("True" course)
        //        |>True course over ground, degrees

        /* valid number of fields? */
        if (fld.length < 3) {
            return false;
        }

        /* loop through values */
        for (int i = 1; (i + 1) < fld.length; i += 2) {
            if (fld[i+1].equals("T")) { // True course
                this.setHeading(StringTools.parseDouble(fld[i],-1.0));
            } else
            if (fld[i+1].equals("N")) { // Knots
                this.setSpeedKnots(StringTools.parseDouble(fld[i],-1.0));
            } else
            if (fld[i+1].equals("K")) { // KPH
                double kph = StringTools.parseDouble(fld[i], -1.0);
                double nmh = (kph >= 0.0)? (kph * KNOTS_PER_KILOMETER) : -1.0;
                this.setSpeedKnots(nmh);
            }
        }

        /* success */
        return true;
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a $GPZDA record has been parsed
    **/
    public boolean hasGPZDA()
    {
        return ((this.parsedRcdTypes & TYPE_GPZDA) != 0L);
    }

    /**
    *** Parses "$GPZDA"
    **/
    protected boolean _parse_GPZDA(String fld[])
    {
        // $GPZDA - UTC Date/Time and Local Time Zone Offset
        // $GPZDA,125653.00,13,09,2007,00,00*6E 
        // $GPZDA,1--------,2-,3-,4---,5-,6-*MM
        //        |         |  |  |    |  | |>Checksum
        //        |         |  |  |    |  |>Local zone minutes description (same sign as hours)
        //        |         |  |  |    |>Local zone hours description: -13..00..+13 hours
        //        |         |  |  |>Year
        //        |         |  |>Month: 01..12
        //        |         |>Day: 01..31
        //        |>UTC hhmmss.ss

        /* valid number of fields? */
        if (fld.length < 5) {
            return false;
        }

        /* parse date */
        this.fixtime = 0L; // calculated later
        long day     = StringTools.parseLong(fld[2], 0L) % 100L;
        long month   = StringTools.parseLong(fld[3], 0L) % 100L;
        long year    = StringTools.parseLong(fld[4], 0L) % 10000L;
        this.ddmmyy  = (day * 10000L) + (month * 100L) + (year % 100L);
        this.setFieldMask(Nmea0183.FIELD_DDMMYY);

        /* parse time */
        this.hhmmss  = StringTools.parseLong(fld[1], 0L);
        this.setFieldMask(Nmea0183.FIELD_HHMMSS);

        /* success */
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if a $GTUID record has been parsed
    **/
    public boolean hasGTUID()
    {
        return ((this.parsedRcdTypes & TYPE_GTUID) != 0L);
    }

    /**
    *** Parses "$GTUID" (Unique-ID)
    **/
    protected boolean _parse_GTUID(String fld[])
    {
        // $GTUID - GTS Unique ID
        // $GTUID,1234567890*6E 
        // $GTUID,1---------*MM
        //        |         |>Checksum (optional)
        //        |>Unique ID

        /* valid number of fields? */
        if (fld.length < 2) {
            return false;
        }

        /* parse status code */
        String uid = StringTools.trim(fld[1]);
        this.setMobileID(uid); // sets Nmea0183.FIELD_MOBILE_ID

        /* success */
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a $GTSTC record has been parsed
    **/
    public boolean hasGTSTC()
    {
        return ((this.parsedRcdTypes & TYPE_GTSTC) != 0L);
    }

    /**
    *** Parses "$GTSTC" (Status Code)
    **/
    protected boolean _parse_GTSTC(String fld[])
    {
        // $GTSTC - GTS Status Code
        // $GTSTC,0xF020*6E 
        // $GTSTC,1-----*MM
        //        |     |>Checksum (optional)
        //        |>Status code

        /* valid number of fields? */
        if (fld.length < 2) {
            return false;
        }

        /* parse status code */
        int sc = StringTools.parseInt(fld[1], STATUS_NONE);
        this.setStatusCode(sc); // sets Nmea0183.FIELD_STATUS_CODE

        /* success */
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a $GTEVT record has been parsed
    **/
    public boolean hasGTEVT()
    {
        return ((this.parsedRcdTypes & TYPE_GTEVT) != 0L);
    }

    /**
    *** Parses "$GTEVT" (Event Record)
    **/
    protected boolean _parse_GTEVT(String fld[])
    {
        // $GTEVT,Time,Code,Lat,Lon,Speed,Heading,Alt,GpsAge,HDOP,Sat*MM
        // $GTEVT,1---,2---,3--,4--,5----,6------,7--,8-----,9---,A--*MM
        //        |    |    |   |   |     |       |   |      |    |  |>Checksum (optional)
        //        |    |    |   |   |     |       |   |      |    |>#Satellites
        //        |    |    |   |   |     |       |   |      |>HDOP
        //        |    |    |   |   |     |       |   |>GPS age (seconds)
        //        |    |    |   |   |     |       |>Altitude (meters)
        //        |    |    |   |   |     |>Heading (degrees)
        //        |    |    |   |   |>Speed (km/h)
        //        |    |    |   |>Longitude (degrees)
        //        |    |    |>Latitude (degrees)
        //        |    |>Status code (hex or decimal)
        //        |>Timstamp (Epoch)

        /* valid number of fields? */
        if (fld.length < 5) { // <-- at least lon
            return false;
        }

        /* parse fixtime (Epoch) */
        this.fixtime = StringTools.parseLong(fld[1], 0L);

        /* parse status code */
        int scode = StringTools.parseInt(fld[2], STATUS_NONE);
        this.setStatusCode(scode); // sets Nmea0183.FIELD_STATUS_CODE

        /* parse lat/lon */
        double lat = StringTools.parseDouble(fld[3], 0.0);
        double lon = StringTools.parseDouble(fld[4], 0.0);
        this.setGeoPoint(lat,lon);

        /* parse speed km/h */
        if ((fld.length > 5) && !StringTools.isBlank(fld[5])) {
            double kph = StringTools.parseDouble(fld[5],-1.0); // km/h only
            double nmh = (kph >= 0.0)? (kph * KNOTS_PER_KILOMETER) : -1.0;
            this.setSpeedKnots(nmh);
        }

        /* parse heading */
        if ((fld.length > 6) && !StringTools.isBlank(fld[6])) {
            double deg = StringTools.parseDouble(fld[6],-1.0);
            this.setHeading((deg >= 0.0)? deg : -1.0);
        }

        /* parse altitude */
        if ((fld.length > 7) && !StringTools.isBlank(fld[7])) {
            this.setAltitudeMeters(StringTools.parseDouble(fld[7], 0.0)); // meters (may be negative)
        }

        /* parse GPS age */
        if ((fld.length > 8) && !StringTools.isBlank(fld[8])) {
            this.setGpsAge(StringTools.parseLong(fld[8],-1L));
        }

        /* parse HDOP */
        if ((fld.length > 9) && !StringTools.isBlank(fld[9])) {
            this.setHDOP(StringTools.parseDouble(fld[9],-1.0));
        }

        /* parse #Satellites */
        if ((fld.length > 10) && !StringTools.isBlank(fld[10])) {
            this.setNumberOfSatellites(StringTools.parseInt(fld[10], 0));
        }

        /* success */
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the current HHMMSS
    *** @return The current HHMMSS
    **/
    public static long getCurrentHHMMSS(TimeZone tz)
    {
        if (tz == null) { tz = DateTime.GMT; }
        DateTime nowDT = new DateTime(tz);
        int HH = nowDT.getHour24(tz);
        int MM = nowDT.getMinute(tz);
        int SS = nowDT.getSecond(tz);
        return (HH * 10000L) + (MM * 100L) + SS;
    }

    // ----------------------------------------------------------------------------

    /**
    *** Returns the parsed epoch fix time
    *** @param DDMMYY  The String representation of the "DDMMYY" 
    *** @param HHMMSS  The String representation of the "HHMMSS" 
    *** @param dftToCurrentTOD  True to default to current time-of-day
    *** @return The parsed epoch fix time 
    **/
    public static long parseFixtime(String DDMMYY, String HHMMSS, boolean dftToCurrentTOD)
    {
        long ddmmyy = StringTools.parseLong(DDMMYY,-1L);
        long hhmmss = StringTools.parseLong(HHMMSS,-1L);
        return Nmea0183.parseFixtime(ddmmyy, hhmmss, dftToCurrentTOD);
    }

    /**
    *** Returns the parsed epoch fix time
    *** @param ddmmyy  The Integer representation of the "DDMMYY" 
    *** @param hhmmss  The Integer representation of the "HHMMSS" 
    *** @return The parsed epoch fix time 
    **/
    public static long parseFixtime(long ddmmyy, long hhmmss, boolean dftToCurrentTOD)
    {
        boolean hasDMY = (ddmmyy >  0L);
        boolean hasHMS = (hhmmss >= 0L);

        /* neither DMY nor HMS defined? */
        if (!hasDMY && !hasHMS) {
            return DateTime.getCurrentTimeSec();
        }

        /* parse */
        long DMY = hasDMY? ddmmyy : -1L/*NoDate*/;
        long HMS = hasHMS? hhmmss : dftToCurrentTOD? Nmea0183.getCurrentHHMMSS(null) : -1L/*NoTime*/;
        // -- Warning: (if "dftToCurrentTOD" is true, and HMS is not defined)
        // -  If DMY is defined and the current time is near midnight, this may generate
        // -  an HMS just after midnight, when a time just before midnight is more accurate.
        return Nmea0183._getUTCSeconds(DMY, HMS);

    }

    // ----------------------------------------------------------------------------

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param dmy Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***     YY is year.
    *** @param hms Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***     and SS is second.
    *** @return Time in UTC seconds.
    **/
    protected static long _getUTCSeconds(long dmy, long hms)
    {
    
        /* time of day [TOD] */
        long TOD;
        if (hms >= 0L) {
            int   HH  = (int)((hms / 10000L) % 100L);
            int   MM  = (int)((hms /   100L) % 100L);
            int   SS  = (int)((hms /     1L) % 100L);
            TOD       = (HH * 3600L) + (MM * 60L) + SS;
        } else {
            TOD       = 0L; // midnight
        }
    
        /* current UTC day */
        long DAY;
        if (dmy > 0L) {
            // -- we have a valid date (DDMMYY)
            int   dd  = (int)((dmy / 10000L) % 100L);
            int   mm  = (int)((dmy /   100L) % 100L);
            int   yy  = (int)((dmy /     1L) % 100L);
            int yyyy  = (yy < 90)? (yy + 2000) : (yy + 1900);
            long  yr  = ((long)yyyy * 1000L) + (long)(((mm - 3) * 1000) / 12);
            DAY       = ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
                        + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
                        + (long)dd - 719469L;
        } else {
            // -- we don't have the day, so we need to figure out as close as we can what it should be.
            // Print.logWarn("Attempting to determine closest day ...");
            long   utc = DateTime.getCurrentTimeSec();
            long   tod = utc % DateTime.DaySeconds(1);
            DAY        = utc / DateTime.DaySeconds(1);
            long   dif = (tod >= TOD)? (tod - TOD) : (TOD - tod); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // -- >12 hour difference, assume we've crossed a day boundary
                if (tod > TOD) {
                    // -- tod > TOD likely represents the next day
                    DAY++;
                } else {
                    // -- tod < TOD likely represents the previous day
                    DAY--;
                }
            }
        }

        /* return UTC seconds */
        long sec = DateTime.DaySeconds(DAY) + TOD;
        return sec;
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Parses latitude given values from GPS device.
    *** @param sd  Latitude String from GPS device in ddmm.mmN/S format.
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         '90.0' if invalid latitude provided.
    **/
    public static double ParseLatitude(String sd)
    {
        return Nmea0183.ParseLatitude(sd, 90.0);
    }

    /**
    *** Parses latitude given values from GPS device.
    *** @param sd  Latitude String from GPS device in ddmm.mmN/S format.
    *** @param dft The default latitude, if the specified latitude cannot be parsed
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         'dft' if invalid latitude provided.
    **/
    public static double ParseLatitude(String sd, double dft)
    {
        // -- eg. "01626.47342N"
        if (sd == null) {
            return dft; // invalid latitude
        } else
        if (sd.endsWith("S") || sd.endsWith("s")) {
            String s = sd.substring(0,sd.length()-1);
            return Nmea0183.ParseLatitude(s,"S",dft);
        } else {
            String s = sd.substring(0,sd.length()-1);
            return Nmea0183.ParseLatitude(s,"N",dft);
        }
    }

    /**
    *** Parses latitude given values from GPS device.
    *** @param s   Latitude String from GPS device in ddmm.mm format.
    *** @param d   Latitude hemisphere, "N" for northern, "S" for southern.
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         '90.0' if invalid latitude provided.
    **/
    public static double ParseLatitude(String s, String d)
    {
        return Nmea0183.ParseLatitude(s,d,90.0);
    }

    /**
    *** Parses latitude given values from GPS device.
    *** @param s   Latitude String from GPS device in ddmm.mm format.
    *** @param d   Latitude hemisphere, "N" for northern, "S" for southern.
    *** @param dft The default latitude, if the specified latitude cannot be parsed
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         'dft' if invalid latitude provided.
    **/
    public static double ParseLatitude(String s, String d, double dft)
    {
        double _lat = StringTools.parseDouble(s, 99999.0);
        if (_lat < 99999.0) {
            double lat = (double)((long)_lat / 100L); // _lat is always positive here
            lat += (_lat - (lat * 100.0)) / 60.0;
            return d.equalsIgnoreCase("S")? -lat : lat;
        } else {
            return dft; // invalid latitude
        }
    }

    // --------------------------------

    /**
    *** Parses longitude given values from GPS device.
    *** @param sd  Longitude String from GPS device in dddmm.mmE/W format.
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         '180.0' if invalid longitude provided.
    **/
    public static double ParseLongitude(String sd)
    {
        return Nmea0183.ParseLongitude(sd,180.0);
    }

    /**
    *** Parses longitude given values from GPS device.
    *** @param sd  Longitude String from GPS device in dddmm.mmE/W format.
    *** @param dft The default latitude, if the specified latitude cannot be parsed
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         'dft' if invalid longitude provided.
    **/
    public static double ParseLongitude(String sd, double dft)
    {
        // -- eg. "01626.47342W"
        if (sd == null) {
            return dft; // invalid longitude
        } else
        if (sd.endsWith("W") || sd.endsWith("w")) {
            String s = sd.substring(0,sd.length()-1);
            return Nmea0183.ParseLongitude(s,"W",dft); // [2.6.2-B57]
        } else {
            String s = sd.substring(0,sd.length()-1);
            return Nmea0183.ParseLongitude(s,"E",dft); // [2.6.2-B57]
        }
    }

    /**
    *** Parses longitude given values from GPS device.
    *** @param s Longitude String from GPS device in dddmm.mmE/W format.
    *** @param d Longitude hemisphere, "E" for eastern, "W" for western.
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         '180.0' if invalid longitude provided.
    **/
    public static double ParseLongitude(String s, String d)
    {
        return Nmea0183.ParseLongitude(s,d,180.0);
    }

    /**
    *** Parses longitude given values from GPS device.
    *** @param s Longitude String from GPS device in ddmm.mm format.
    *** @param d Longitude hemisphere, "E" for eastern, "W" for western.
    *** @param dft The default latitude, if the specified latitude cannot be parsed
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         'dft' if invalid longitude provided.
    **/
    public static double ParseLongitude(String s, String d, double dft)
    {
        double _lon = StringTools.parseDouble(s, 99999.0);
        if (_lon < 99999.0) {
            double lon = (double)((long)_lon / 100L); // _lon is always positive here
            lon += (_lon - (lon * 100.0)) / 60.0;
            return d.equalsIgnoreCase("W")? -lon : lon;
        } else {
            return dft; // invalid longitude
        }
    }

    // ------------------------------------------------------------------------

    /**
    * Checks if NMEA-0183 formatted String has valid checksum by calculating the
    * checksum of the payload and comparing that to the received checksum.
    * @param str NMEA-0183 formatted String to be checked.
    * @return true if checksum is valid, false otherwise.
    */
    protected boolean _hasValidChecksum(String str)
    {

        /* extract checksum value */
        int c = str.indexOf("*");
        if (c < 0) {
            // -- does not contain a checksum char
            if (str.startsWith(CUSTOM_GT_)) {
                // -- checksum specifications are optional with "$GTxxx" records
                return true;
            }
            return false;
        }
        String chkSum = str.substring(c + 1);

        /* parse specified checksum */
        byte cs[] = StringTools.parseHex(chkSum,null);
        if ((cs == null) || (cs.length != 1)) {
            // -- invalid checksum hex length
            return false;
        }

        /* calculate actual checksum */
        int calcSum = Nmea0183.calcXORChecksum(str,false);

        /* check if equivalent */
        boolean isValid = (calcSum == ((int)cs[0] & 0xFF));
        if (!isValid) { Print.logWarn("Expected checksum: 0x" + StringTools.toHexString(calcSum,8)); }
        return isValid;

    }

    /**
    *** Calculates/Returns the checksum for a NMEA-0183 formatted String
    *** @param str NMEA-0183 formatted String to be checksummed.
    *** @return Checksum computed from input.
    **/
    public static int calcXORChecksum(String str, boolean includeAll)
    {
        byte b[] = StringTools.getBytes(str);
        if (b == null) {

            /* no bytes */
            return -1;

        } else {

            int cksum = 0, s = 0;

            /* skip leading '$' */
            if (!includeAll && (b.length > 0) && (b[0] == '$')) { 
                s++; 
            }

            /* calc checksum */
            for (; s < b.length; s++) {
                if (!includeAll && (b[s] == '*')) { break; }
                if ((b[s] == '\r') || (b[s] == '\n')) { break; }
                cksum = (cksum ^ b[s]) & 0xFF;
            }

            /* return checksum */
            return cksum;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* parse record */
        if (RTConfig.hasProperty("parse")) {
            String gprmc = RTConfig.getString("parse","");
            if (!gprmc.startsWith("$")) {
                gprmc = "$" + gprmc;
            }
            Nmea0183 n = new Nmea0183(gprmc, true); // ignore checksum
            Print.sysPrintln("NMEA-0183: \n" + n);
            System.exit(0);
        }

        /* calculate checksum */
        if (RTConfig.hasProperty("xor")) {
            String cksumStr = RTConfig.getString("xor","");
            int cksum = Nmea0183.calcXORChecksum(cksumStr,true);
            Print.sysPrintln("Checksum: " + StringTools.toHexString(cksum,8));
            System.exit(0);
        }

        /* test */
        if (RTConfig.hasProperty("test")) {
            Nmea0183 n;

            Print.sysPrintln("--------------------");
            String gprmc = "$GPRMC,080701.00,A,3128.7540,N,14257.6714,W,27.6,107.5,180607,13.1,E,A*2D";
            n = new Nmea0183(gprmc);
            Print.sysPrintln("$GPRMC   : " + gprmc);
            Print.sysPrintln("      ==>: " + n.toGPRMC());
            Print.sysPrintln("NMEA-0183(1): \n" + n);
            n.parse("$GTUID,1234567890");
            n.parse("$GTSTC,0xF021");
            Print.sysPrintln("NMEA-0183(2): \n" + n);

            Print.sysPrintln("--------------------");
            String gpgga = "$GPGGA,025425.494,3509.0743,N,14207.6314,W,1,04,2.3,530.3,M,-21.9,M,0.0,0000*45";
            n = new Nmea0183(gpgga);
            Print.sysPrintln("NMEA-0183: \n" + n);

            Print.sysPrintln("--------------------");
            n = new Nmea0183("$GPGGA,125653.00,3845.165,N,14228.961,W,1,05,,102.1331,M,,M,,*75");
            n.parse("$GPVTG,229.86,T,,M,0.00,N,0.0046,K*55");   // speed/heading
            n.parse("$GPZDA,125653.00,13,09,2007,00,00*6E");    // date/time
            Print.sysPrintln("NMEA-0183: \n" + n);

            Print.sysPrintln("--------------------");
            String gtevt = "$GTEVT,1311546722,0xF022,39.1234,-142.1234,45.0,121.0,1008,5,1.1,6";
            n = new Nmea0183(gtevt);
            Print.sysPrintln("$GTEVT   : " + gtevt);
            Print.sysPrintln("      ==>: " + n.toGPRMC());
            Print.sysPrintln("NMEA-0183: \n" + n);

        }
        
    }
    
}
