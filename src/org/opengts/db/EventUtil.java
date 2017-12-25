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
//  2006/04/09  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//  2007/03/16  Martin D. Flynn
//     -Added XML output
//  2007/06/30  Martin D. Flynn
//     -Included additional EventData fields in KML output.
//  2007/07/14  Martin D. Flynn
//     -Fixed closing tag name (changed from "</Record>" to "</Event>")
//  2007/09/16  Martin D. Flynn
//     -Added additional XML field support
//  2007/11/28  Martin D. Flynn
//     -Added EventData geozone update option
//     -XML output for Geozone now correctly checks 'getGeozoneID()'
//     -Added City/PostalCode to XML output
//     -Made EventUtil a singleton
//  2008/03/12  Martin D. Flynn
//     -Tool date ranges can now be specified in "YYYY/MM/DD" format
//  2008/12/01  Martin D. Flynn
//     -Added support for optional map event data fields.
//  2009/05/27  Martin D. Flynn
//     -Added speed 'limit' to XML output.
//  2009/07/01  Martin D. Flynn
//     -Map points wrapped in XML "MapData"/"DataSet" tags
//  2009/09/23  Martin D. Flynn, Clifton Flynn
//     -Changed to support SOAP xml encoding
//  2009/10/02  Martin D. Flynn
//     -Modified "getParseMapEventJS" and "formatMapEvent" to include the device
//      vehicle ID in the dataset sent to the client browser.
//  2010/09/09  Martin D. Flynn
//     -Added "DeviceID" column to CSV event output format
//  2011/05/13  Martin D. Flynn
//     -Changed "writeMapEvents" to support reading all enclosed Geozones
//      (see "GET_ALL_CONTAINED_GEOZONES")
//  2011/06/16  Martin D. Flynn
//     -Changed "DigitalImputMask" to "DigitalInputMask" (many thanks to Aykut Kara)
//  2011/07/01  Martin D. Flynn
//     -Lat/Lon accuracy added to AJAX event XML sent to browser.
//     -Added support for JSON event output format.
//  2011/07/15  Martin D. Flynn
//     -Changed JSON numeric output to non-quoted strings.
//  2011/08/21  Martin D. Flynn
//     -Added "_encUnicode" to filter and encode unicode characters
//  2012/04/03  Martin D. Flynn
//     -Added "stop" state to map data (see "formatMapEvent"/"getParseMapEventJS")
//  2012/06/29  Martin D. Flynn
//     -Added "createAge" to Event AJAX map protocol.
//  2012/12/24  Martin D. Flynn
//     -Also use DistanceUnits "NM" for altitude units default to "feet".
//  2013/03/01  Martin D. Flynn
//     -Added "GET_NEARBY_GEOZONES", "MAP_INCL_SC_COLOR"
//  2013/04/08  Martin D. Flynn
//     -Changed "GET_NEARBY_GEOZONES" to "NEARBY_GEOZONE_RADIUS"
//  2014/06/29  Martin D. Flynn
//     -Added additional "zoneShapes" initialization to fix NPE
//  2016/09/01  Martin D. Flynn
//     -Set "trackMap.includeStatusCodeColor" default to "true".
//  2016/12/21  Martin D. Flynn
//     -Moved OptionalEventFields to "OptionalEventFields.java"
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.Locale;
import java.util.TimeZone;
import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.Vector;
import java.util.HashSet;
import java.util.HashMap;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.Version;
import org.opengts.dbtypes.*;
import org.opengts.geocoder.*;
import org.opengts.db.tables.*;

public class EventUtil
{

    // ------------------------------------------------------------------------

    /* maximum pushpins allowed on map */
    public  static final long    DFT_MAX_PUSHPIN_LIMIT      = 1000L;

    /* default ARG_EVENTS CSV event display limit */
    private static final long    DFT_CSV_LIMIT              = 30L;

    // ------------------------------------------------------------------------

    public  static final String  MAP_ESCAPE_HTML            = "$HTML:";
    public  static final String  MAP_ESCAPE_B64             = "$B64:";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

  //public  static final int     MAPDATA_DEFAULT            = 0; 
    public  static final int     MAPDATA_XML                = 1;
    public  static final int     MAPDATA_JSON               = 2;

  //private static       int     DefaultMapDataFormat       = EventUtil.MAPDATA_JSON;

  //public static void SetDefaultMapDataFormat(int dftDataFmt)
  //{
  //    DefaultMapDataFormat = dftDataFmt;
  //}

    public static int GetDefaultMapDataFormat()
    {
        return EventUtil.MAPDATA_JSON; // Default was XML, now JSON [2.6.3-B55]
    }

    public static boolean IsXMLMapDataFormat(int mdf)
    {
        return (mdf == MAPDATA_XML);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static OptionalEventFields optionalEventFieldHandler = null;

    /**
    *** Sets the global default OptionalEventFields handler
    **/
    public static void setOptionalEventFieldHandler(OptionalEventFields oef)
    {
        EventUtil.optionalEventFieldHandler = oef; // may be null
    }

    /**
    *** Gets the OptionalEventFields handler. 
    *** Returns the OptionalEventFields instance for the specified PrivateLabel, if defined.
    *** Otherwise returns the global default OptionalEventFields handler (if defined).
    *** @param privLabel  The PrivateLabel instance to also check for an available OptionalEventFields instance.
    *** @return The available OptionalEventFields instance (if defined)
    **/
    public static OptionalEventFields getOptionalEventFieldHandler(BasicPrivateLabel privLabel)
    {
        if ((privLabel != null) && privLabel.hasOptionalEventFieldHandler()) {
            // -- return PrivateLabel OptionalEventFields
            return privLabel.getOptionalEventFieldHandler();
        } else {
            // -- return global default (may be null)
            return EventUtil.optionalEventFieldHandler; // may be null
        }
    }

    /**
    *** Returns true if an OptionalEventFields handler is available.
    *** @param privLabel  The PrivateLabel instance to also check for an available OptionalEventFields instance.
    *** @return True is an OptionalEventFields is available
    **/
    public static boolean hasOptionalEventFieldHandler(BasicPrivateLabel privLabel)
    {
        if ((privLabel != null) && privLabel.hasOptionalEventFieldHandler()) {
            return true;
        } else {
            return (EventUtil.optionalEventFieldHandler != null)? true : false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final int    FORMAT_UNKNOWN      =  0;
    public  static final int    FORMAT_CSV          =  1;
    public  static final int    FORMAT_KML          =  2;
    public  static final int    FORMAT_XML          =  3;
    public  static final int    FORMAT_XMLOLD       =  4;
    public  static final int    FORMAT_TXT          =  5;
    public  static final int    FORMAT_GPX          =  6;
    public  static final int    FORMAT_JSON         =  7;
    public  static final int    FORMAT_JSONX        =  8;
    public  static final int    FORMAT_BML          =  9;
    public  static final int    FORMAT_AEMP         = 10;

    public static int parseOutputFormat(String fmt, int dftFmt)
    {
        if (fmt == null) {
            return dftFmt;
        } else
        if (fmt.equalsIgnoreCase("csv")) {
            return EventUtil.FORMAT_CSV;
        } else
        if (fmt.equalsIgnoreCase("kml")) {
            return EventUtil.FORMAT_KML;
        } else
        if (fmt.equalsIgnoreCase("xml")) {
            return EventUtil.FORMAT_XML;
        } else
        if (fmt.equalsIgnoreCase("txt")) {
            return EventUtil.FORMAT_TXT;
        } else
        if (fmt.equalsIgnoreCase("gpx")) {
            return EventUtil.FORMAT_GPX;
        } else
        if (fmt.equalsIgnoreCase("json")) {
            return EventUtil.FORMAT_JSON;
        } else
        if (fmt.equalsIgnoreCase("jsonx")) {
            return EventUtil.FORMAT_JSONX;
        } else
        if (fmt.equalsIgnoreCase("bml")) {
            return EventUtil.FORMAT_BML;
        } else
        if (fmt.equalsIgnoreCase("aemp")) {
            return EventUtil.FORMAT_AEMP;
        } else {
            return dftFmt;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static EventUtil instance = null;
    public static EventUtil getInstance()
    {
        if (EventUtil.instance == null) {
            EventUtil.instance = new EventUtil();
        }
        return EventUtil.instance;
    }

    static {
        EventUtil.getInstance();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    private EventUtil() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /**
    *** Write output to specified PrintWriter
    *** @param out  The PrintWriter
    *** @param s    The String to write
    **/
    private void write(PrintWriter pwout, String s)
        throws IOException
    {
        if (s != null) {
            if (pwout != null) {
                pwout.write(s);
                //Print.logInfo(s);
            } else {
                Print.sysPrint(s);
            }
        }
    }

    /**
    *** Flushes the specified PrintStream
    *** @param out  The PrintStream
    **/
    private void flush(PrintWriter pwout)
    {
        if (pwout != null) {
            pwout.flush();
        }
    }

    /**
    *** Print output to specified PrintStream
    *** @param psout  The PrintStream
    *** @param s      The String to print
    **/
    private void print(PrintStream psout, String s)
    {
        if (s != null) {
            if (psout != null) {
                psout.print(s);
            } else {
                Print.sysPrint(s);
            }
        }
    }

    /**
    *** Print output to specified PrintStream, appending a newline after printing the String
    *** @param psout  The PrintStream
    *** @param s      The String to print
    **/
    private void println(PrintStream psout, String s)
    {
        if (s != null) {
            if (psout != null) {
                psout.println(s);
            } else {
                Print.sysPrintln(s);
            }
        }
    }

    // ------------------------------------------------------------------------

    /*
    private boolean writeEvents_CSV_short(PrintWriter pwout, 
        Account account, Collection<Device> devList,
        BasicPrivateLabel privLabel)
        throws IOException
    {
        boolean  allTags    = false;
        TimeZone tmzone     = null;
        char     csvSep     = ',';
        boolean  inclHeader = true;
        return writeEvents_CSV(pwout, 
            account, devList,
            allTags, tmzone,
            csvSep, inclHeader, privLabel);
    }
    */

    private boolean writeEvents_CSV(PrintWriter pwout, 
        Account account, Collection<Device> devList, 
        boolean allTags, TimeZone dispTmz, 
        char csvSep, boolean inclHeader, BasicPrivateLabel privLabel)
        throws IOException
    {

        /* fields to place in CSV format */
        String evFields[] = null;
        if (allTags) {
            evFields = new String[] {
                EventData.FLD_deviceID,
                // --
              //"Device." + Device.FLD_description,   // <-- uncomment to include Device "description"
              //"Device." + Device.FLD_serialNumber,  // <-- uncomment to include Device "serialNumber"
                // --
                EventData.FLD_timestamp,
                EventData.FLD_statusCode,
                EventData.FLD_latitude,
                EventData.FLD_longitude,
                EventData.FLD_speedKPH,
                EventData.FLD_heading,
                EventData.FLD_altitude,
                EventData.FLD_address,
                // --
                EventData.FLD_gpsAge,
                EventData.FLD_satelliteCount,
                EventData.FLD_inputMask,
                EventData.FLD_odometerKM,
                EventData.FLD_geozoneID,
                EventData.FLD_driverID,
                EventData.FLD_driverMessage,
                // --
                EventData.FLD_fuelTotal,
                EventData.FLD_engineRpm,
                EventData.FLD_engineHours,
                EventData.FLD_vBatteryVolts,
                EventData.FLD_coolantLevel,
                EventData.FLD_coolantTemp,
            };
        } else {
            evFields = new String[] {
                EventData.FLD_deviceID,
                // --
              //"Device." + Device.FLD_description,   // <-- uncomment to include Device "description"
              //"Device." + Device.FLD_serialNumber,  // <-- uncomment to include Device "serialNumber"
                // --
                EventData.FLD_timestamp,
                EventData.FLD_statusCode,
                EventData.FLD_latitude,
                EventData.FLD_longitude,
                EventData.FLD_speedKPH,
                EventData.FLD_heading,
                EventData.FLD_altitude,
                EventData.FLD_address,
            };
        }

        /* write events */
        return this.writeEvents_CSV_fields(pwout, 
            account, devList, evFields, 
            dispTmz, 
            csvSep, inclHeader, privLabel);

    }

    private boolean writeEvents_CSV_fields(PrintWriter pwout, 
        Account account, Collection<Device> devList, String evFields[], 
        TimeZone dispTmz, 
        char csvSep, boolean inclHeader, BasicPrivateLabel privLabel)
        throws IOException
    {
        // Note: If all of the specified EventData records do not belong to the 
        // same 'deviceID', then 'evFields' should contain the 'deviceID'.
        
        /* account required */
        if (account == null) {
            return false;
        }
        String accountID = account.getAccountID();

        /* print header */
        if (inclHeader) {
            String hdr = this.formatHeader_CSV(evFields,csvSep);
            hdr += "\n";
            this.write(pwout, hdr);
        }

        /* date/time format */
        String dateFmt = account.getDateFormat();
        String timeFmt = account.getTimeFormat();

        /* account timezone */
        TimeZone acctTmz = account.getTimeZone(null);
        if (dispTmz == null) {
            dispTmz = acctTmz;
        }

        /* print events */
        if (!ListTools.isEmpty(devList)) {
            for (Device dev : devList) {

                /* check account ID */
                if (!dev.getAccountID().equals(accountID)) {
                    // mismatched AccountID
                    continue;
                }

                /* Device events */
                EventData evList[] = dev.getSavedRangeEvents();
                if (ListTools.isEmpty(evList)) {
                    // no events for this device
                    continue;
                }

                /* write events */
                for (EventData ev : evList) {

                    /* same account? */
                    if (!ev.getAccountID().equals(accountID)) {
                        // mismatched AccountID
                        continue;
                    }
                    ev.setAccount(account); // likely redundant

                    /* write event */
                    String rcd = this.formatEventData_CSV(ev, evFields, 
                        dispTmz, dateFmt, timeFmt, csvSep) + "\n";
                    this.write(pwout, rcd);

                }

            }
        }

        /* flush (output may not occur until the PrintWriter is flushed) */
        this.flush(pwout);
        return true;

    }

    private String formatHeader_CSV(String f[], char csvSep)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < f.length; i++) {
            if (sb.length() > 0) { sb.append(csvSep); }
            if (f[i].equals(EventData.PFLD_deviceDesc)) { // <-- pseudo field
                sb.append("DeviceDesc");
            } else
            if (f[i].startsWith(EventData.PFLD_Account_)) { // <-- pseudo field
                String fn = f[i].substring(EventData.PFLD_Account_.length());
                //DBField dbf = Account.getFactory().getField(fn);
                sb.append("Account.").append(fn); // Account field
            } else
            if (f[i].startsWith("Account.")) { // <-- explicitly "Account" table
                sb.append(f[i]); // Account field
            } else
            if (f[i].startsWith(EventData.PFLD_Device_)) { // <-- pseudo field
                String fn = f[i].substring(EventData.PFLD_Device_.length());
                //DBField dbf = Device.getFactory().getField(fn);
                sb.append("Device.").append(fn); // Device field
            } else
            if (f[i].startsWith("Device.")) { // <-- explicitly "Device" table
                sb.append(f[i]); // Device field
            } else
            if (f[i].equals(EventData.FLD_deviceID)) {
                sb.append("DeviceID");
            } else
            if (f[i].equals(EventData.FLD_timestamp)) {
                // 'timestamp' is separated into "date,time"
                sb.append("Date").append(csvSep).append("Time");
            } else
            if (f[i].equals(EventData.FLD_statusCode)) {
                sb.append("Code");
            } else
            if (f[i].equals(EventData.FLD_latitude)) {
                sb.append("Latitude");
            } else
            if (f[i].equals(EventData.FLD_longitude)) {
                sb.append("Longitude");
            } else
            if (f[i].equals(EventData.FLD_speedKPH)) {
                sb.append("Speed");
            } else
            if (f[i].equals(EventData.FLD_heading)) {
                sb.append("Heading");
            } else
            if (f[i].equals(EventData.FLD_altitude)) {
                sb.append("Altitude");
            } else
            if (f[i].equals(EventData.FLD_address)) {
                sb.append("Address");
            } else
            if (EventData.getFactory().hasField(f[i])) {
                sb.append(f[i]); // field name
            } else
            if (Device.getFactory().hasField(f[i])) {
                sb.append("Device.").append(f[i]); // Device field
            } else
            if (Account.getFactory().hasField(f[i])) {
                sb.append("Account.").append(f[i]); // Account field
            } else {
                // -- field not found
                sb.append("?");
            }
        }
        return sb.toString();
    }
    
    private String formatEventData_CSV(EventData evdata, String fields[], 
        TimeZone dispTmz, String dateFmt, String timeFmt, char csvSep)
    {
        StringBuffer sb = new StringBuffer();
        if ((evdata != null) && (fields != null)) {
            Account account = evdata.getAccount();
            Device  device  = evdata.getDevice();
            BasicPrivateLabel privLabel = account.getPrivateLabel();
            for (int i = 0; i < fields.length; i++) {
                //if (i > 0) { sb.append(csvSep); }
                if (sb.length() > 0) { sb.append(csvSep); }

                // -- Pseudo fields here (if any)
                if (fields[i].startsWith(DBRecord.PSEUDO_FIELD_CHAR)) {
                    if (fields[i].equals(EventData.PFLD_deviceDesc)) {
                        // -- TODO: filter CSV column data
                        sb.append(evdata.getDeviceDescription());
                    } else
                    if (fields[i].startsWith(EventData.PFLD_Account_) && (account != null)) {
                        // -- TODO: filter CSV column data
                        String fn = fields[i].substring(EventData.PFLD_Account_.length());
                        sb.append(StringTools.trim(account.getValue(fn)));
                    } else
                    if (fields[i].startsWith(EventData.PFLD_Device_) && (device != null)) {
                        // -- TODO: filter CSV column data
                        String fn = fields[i].substring(EventData.PFLD_Device_.length());
                        sb.append(StringTools.trim(device.getValue(fn)));
                    } else {
                        // -- pseudo-field not found
                        Print.logWarn("CSV field not found: " + fields[i]);
                        sb.append("?");
                    }
                    continue;
                } else
                if (fields[i].startsWith("Device.")) {
                    if (device != null) {
                        String fn = fields[i].substring("Device.".length());
                        sb.append(StringTools.trim(device.getValue(fn)));
                    } else {
                        sb.append("?");
                    }
                    continue;
                } else
                if (fields[i].startsWith("Account.")) {
                    if (account != null) {
                        String fn = fields[i].substring("Account.".length());
                        sb.append(StringTools.trim(account.getValue(fn)));
                    } else {
                        sb.append("?");
                    }
                    continue;
                }

                // -- get DBField value
                Object val = null;
                DBField dbFld = evdata.getRecordKey().getField(fields[i]); // TODO: could be optimized
                if (dbFld != null) {
                    // -- found in EventData table
                    val = evdata.getFieldValue(fields[i]);
                } else
                if (device != null) {
                    dbFld = device.getRecordKey().getField(fields[i]);
                    if (dbFld != null) {
                        // -- found in Device table
                        val = device.getFieldValue(fields[i]);
                    } else
                    if (account != null) {
                        dbFld = account.getRecordKey().getField(fields[i]);
                        if (dbFld != null) {
                            // -- found in Account table
                            val = account.getFieldValue(fields[i]);
                        } else {
                            Print.logWarn("CSV field not found: " + fields[i]);
                            val = null;
                        }
                    }
                }

                // -- no value?
                if ((dbFld == null) || (val == null)) {
                    // -- append placeholder
                    sb.append("?");
                    continue;
                }

                // -- format value
                Class<?> typeClass = dbFld.getTypeClass();
                if (fields[i].equals(EventData.FLD_timestamp)) {
                    // -- format timestamp
                    long time = ((Long)val).longValue();
                    DateTime dt = new DateTime(time); // GMT
                    String fmt = dateFmt + csvSep + timeFmt; // -- double CSV separator
                    if (dispTmz == null) {
                        sb.append(dt.gmtFormat(fmt));
                    } else {
                        sb.append(dt.format(fmt,dispTmz));
                    }
                } else
                if (fields[i].equals(EventData.FLD_statusCode)) {
                    // -- return statusCode description
                    //int code = ((Integer)val).intValue();
                    //sb.append(StatusCodes.GetDescription(code));
                    String scd = evdata.getStatusCodeDescription(privLabel);
                    sb.append(scd);
                } else
                if ((typeClass == Float.class) || (typeClass == Float.TYPE)) {
                    // -- generic Float type
                    float d = ((Float)val).floatValue();
                    String fmt = dbFld.getFormat();
                    if ((fmt != null) && !fmt.equals("")) {
                        sb.append(StringTools.format(d,fmt));
                    } else {
                        sb.append(String.valueOf(d));
                    }
                } else
                if ((typeClass == Double.class) || (typeClass == Double.TYPE)) {
                    // -- generic Double type
                    double d = ((Double)val).doubleValue();
                    String fmt = dbFld.getFormat();
                    if ((fmt != null) && !fmt.equals("")) {
                        sb.append(StringTools.format(d,fmt));
                    } else {
                        sb.append(String.valueOf(d));
                    }
                } else
                if ((typeClass == Long.class) || (typeClass == Long.TYPE)) {
                    // -- generic Long type
                    sb.append(val.toString());
                } else
                if ((typeClass == Integer.class) || (typeClass == Integer.TYPE)) {
                    // -- generic Integer type
                    sb.append(val.toString());
                } else
                if (fields[i].equals(EventData.FLD_address)) {
                    // -- format Address
                    String v = val.toString().replace(csvSep,' '); // remove csv separators
                    sb.append(StringTools.quoteString(v)); // always quote address
                } else {
                    // -- everything else
                    String v = val.toString().replace(csvSep,' '); // remove csv separators
                    if ((v.indexOf(" ") >= 0) || (v.indexOf('\"') >= 0)) {
                        sb.append(StringTools.quoteString(v));
                    } else {
                        sb.append(v);
                    }
                }

            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // This section specifically handles writing encoded events to the map browser
    
    public  static final boolean SEPARATE_DATASET_PER_DEVICE    = false;
    
    public  static final String  TAG_MapData                    = "MapData";
  //public  static final String  TAG_Latest                     = "Latest";
    public  static final String  TAG_LastEvent                  = "LastEvent";
    public  static final String  TAG_Time                       = "Time";
    public  static final String  TAG_DataColumns                = "DataColumns";
    public  static final String  TAG_DataSet                    = "DataSet";
    public  static final String  TAG_Point                      = "P";
    public  static final String  TAG_Shape                      = "Shape"; // MapShape
    public  static final String  TAG_Action                     = "Action"; // used by ReportDisplay
    
    public  static final String  JSON_Error                     = "Error";
    public  static final String  JSON_Command                   = "Command";
    public  static final String  JSON_Account                   = "Account";
    public  static final String  JSON_User                      = "User";
    public  static final String  JSON_Version                   = "Version";
    public  static final String  JSON_JMapData                  = "JMapData";
    public  static final String  JSON_Time                      = TAG_Time;
    public  static final String  JSON_LastEvent                 = TAG_LastEvent;
    public  static final String  JSON_DataColumns               = TAG_DataColumns;
    public  static final String  JSON_Data                      = "Data";
    public  static final String  JSON_Shapes                    = "Shapes";
    public  static final String  JSON_DataSets                  = "DataSets";
    public  static final String  JSON_Points                    = "Points";
    public  static final String  JSON_Actions                   = "Actions";
    public  static final String  JSON_YMD                       = "YMD";
    public  static final String  JSON_year                      = "YYYY";
    public  static final String  JSON_month                     = "MM";
    public  static final String  JSON_day                       = "DD";
    public  static final String  JSON_date                      = "date";
    public  static final String  JSON_time                      = "time";
    public  static final String  JSON_cmd                       = "cmd";
    public  static final String  JSON_arg                       = "arg";
    public  static final String  JSON_type                      = "type";
    public  static final String  JSON_radius                    = "radius";
    public  static final String  JSON_color                     = "color";
    public  static final String  JSON_desc                      = "desc";
    public  static final String  JSON_ppNdx                     = "ppNdx";
    public  static final String  JSON_route                     = "route";
    public  static final String  JSON_routeColor                = "routeColor";
    public  static final String  JSON_textColor                 = "textColor";
    public  static final String  JSON_id                        = "id";

    public  static final String  ATTR_isFleet                   = "isFleet";
    public  static final String  ATTR_type                      = "type";
    public  static final String  ATTR_id                        = "id";
    public  static final String  ATTR_route                     = "route";
    public  static final String  ATTR_routeColor                = "routeColor";
    public  static final String  ATTR_textColor                 = "textColor";
    public  static final String  ATTR_timestamp                 = "timestamp";
    public  static final String  ATTR_timezone                  = "timezone";
    public  static final String  ATTR_device                    = "device";
    public  static final String  ATTR_year                      = "year";
    public  static final String  ATTR_month                     = "month";
    public  static final String  ATTR_day                       = "day";
    public  static final String  ATTR_color                     = "color";
    public  static final String  ATTR_desc                      = "desc";
    public  static final String  ATTR_ppNdx                     = "ppNdx";
    public  static final String  ATTR_radius                    = "radius";
    public  static final String  ATTR_battery                   = "battery";
    public  static final String  ATTR_signal                    = "signal";
    public  static final String  ATTR_command                   = "command";

    public  static final String  DSTYPE_device                  = "device";
    public  static final String  DSTYPE_group                   = "group";
    public  static final String  DSTYPE_poi                     = "poi";

  //public  static final String  CSV_SEPARATOR                  = "|";
    public  static final char    CSV_SEPARATOR_CHAR             = '|';

    /* return JavaScript for parsing the formatted CSV EventDataProvider record */
    public String getParseMapEventJS(
        BasicPrivateLabel privLabel, 
        boolean isFleet, Locale locale)
    {
        return this.getParseMapEventJS(
            privLabel, 
            isFleet, locale, CSV_SEPARATOR_CHAR);
    }

    /* return JavaScript for parsing the formatted CSV EventDataProvider record */
    // -- NOTE: The format parsed here must match the formatter 'formatMapEvent' below
    // -- NOTE: The pushpin InfoBox is generated in "jsmap.js" (JSMapPushpin.prototype.getHTML)
    public String getParseMapEventJS(
        BasicPrivateLabel privLabel, 
        boolean isFleet, Locale locale, char csvSep)
    {
        StringBuffer js = new StringBuffer();

        /* MapEventRecord */
        js.append("// (generated by 'EventUtil.getParseMapEventJS')\n");
        js.append("function MapEventRecord(csvRcd) {\n");
        js.append("    var fld        = csvRcd.split('" + csvSep + "');\n");
        js.append("    this.index     = 0;\n");     // index       (will be set later)
        js.append("    this.lastEv    = null;\n");  // linked-list (will be set later)
        js.append("    this.nextEv    = null;\n");  // linked-list (will be set later)
        js.append("    this.valid     = (fld.length > 9);\n"); // must include at least up to latitude/longitude
        js.append("    this.devVIN    = (fld.length > 0)? decodeUnicode(fld[ 0]) : '';\n"); // device ID/VIN
        js.append("    this.device    = (fld.length > 1)? decodeUnicode(fld[ 1]) : '';\n"); // description
        js.append("    this.timestamp = (fld.length > 2)? parseInt(fld[ 2]) : 0;\n");       // epoch
        js.append("    this.dateFmt   = (fld.length > 3)? fld[ 3] : '';\n");                // date in 'account' format (in selected timezone)
        js.append("    this.timeFmt   = (fld.length > 4)? fld[ 4] : '';\n");                // time in 'account' format (in selected timezone)
        js.append("    this.timeZone  = (fld.length > 5)? fld[ 5] : '';\n");                // short timezone name
        js.append("    this.code      = (fld.length > 6)? decodeUnicode(fld[ 6]) : '';\n"); // status code (description)
        js.append("    if (this.code.startsWith('"+EventUtil.MAP_ESCAPE_HTML+"')) { this.code = decodeBase64(this.code.substring("+EventUtil.MAP_ESCAPE_HTML.length()+")); }\n");
        js.append("    this.iconNdx   = (fld.length > 7)? fld[ 7] : '';\n");                // display icon index
        js.append("    this.isCellLoc = false;\n");                                         // cell-tower location?
        js.append("    this.latitude  = numParseFloat(((fld.length >  8)? fld[ 8] : '0'), 0);\n");
        js.append("    this.longitude = numParseFloat(((fld.length >  9)? fld[ 9] : '0'), 0);\n");
        js.append("    this.gpsAge    = numParseFloat(((fld.length > 10)? fld[10] : '0'), 0);\n");
        js.append("    this.createAge = numParseFloat(((fld.length > 11)? fld[11] : '0'), 0);\n");
        js.append("    this.accuracy  = numParseFloat(((fld.length > 12)? fld[12] : '0'), 0);\n");
        js.append("    if (this.accuracy < 0) { this.accuracy = 0; }\n");
        js.append("    this.validGPS  = ((this.latitude != 0) || (this.longitude != 0))? true : false;\n");
        js.append("    this.satCount  = (fld.length > 13)? fld[13] : '0';\n");              // satellite count
        js.append("    if (this.satCount < 0) { this.isCellLoc = true; this.satCount = 0; }\n");
        js.append("    this.speedKPH  = numParseFloat(((fld.length > 14)? fld[14] : '0'), 0);\n");
        js.append("    this.speedMPH  = this.speedKPH * " + GeoPoint.MILES_PER_KILOMETER + ";\n");
        js.append("    this.heading   = numParseFloat(((fld.length > 15)? fld[15] : '0'), 0);\n");
        js.append("    this.compass   = HEADING[Math.round(this.heading / 45.0) % 8];\n");
        js.append("    this.altitude  = numParseFloat(((fld.length > 16)? fld[16] : '0'), 0); // meters\n");
        js.append("    this.odomKM    = numParseFloat(((fld.length > 17)? fld[17] : '0'), 0);\n");  // km
        js.append("    this.stopped   = (fld.length > 18)? parseInt(fld[18]) : 0;\n");      // enum (stoppedState)
        js.append("    this.stopSec   = 0;\n");      // seconds (will be set later for this.stopped==2)
        js.append("    this.gpioInput = numParseInt(((fld.length > 19)? fld[19] : '0'), 0);\n");
        js.append("    this.address   = (fld.length > 20)? decodeUnicode(fld[20].trim()) : '';\n");
        js.append("    if (this.address.startsWith('\\\"')) { this.address = this.address.substring(1); }\n");
        js.append("    if (this.address.endsWith('\\\"')  ) { this.address = this.address.substring(0, this.address.length - 1); }\n");
      //js.append("    //if (this.address == '') { this.address = '&nbsp;'; }\n"); // fill with space, so field isn't blank
        js.append("    if (fld.length > 21) {\n");
        js.append("        this.optDesc = new Array();\n");
        js.append("        for (var i = 21; i < fld.length; i++) {\n");
        js.append("            var v = decodeUnicode(fld[i]);\n");
        js.append("            if (v.startsWith('"+EventUtil.MAP_ESCAPE_B64 +"')) { v = decodeBase64(v.substring("+EventUtil.MAP_ESCAPE_B64.length() +")); }\n");
        js.append("            if (v.startsWith('"+EventUtil.MAP_ESCAPE_HTML+"')) { v = decodeBase64(v.substring("+EventUtil.MAP_ESCAPE_HTML.length()+")); }\n");
        js.append("            this.optDesc.push(v);\n");
        js.append("        }\n");
        js.append("    }\n");
        js.append("};\n");

        /* OptionalEventFields */
        OptionalEventFields optEvFlds = EventUtil.getOptionalEventFieldHandler(privLabel);
        int optFieldCount = (optEvFlds != null)? optEvFlds.getOptionalEventFieldCount(isFleet) : 0;

        /* OptionalEventFieldCount */
        js.append("function OptionalEventFieldCount() {\n");
        js.append("    return "+ optFieldCount +";\n");
        js.append("};\n");

        /* OptionalEventFieldTitle */
        js.append("function OptionalEventFieldTitle(ndx) {\n");
        if ((optEvFlds != null) && (optFieldCount > 0)) {
            js.append("    switch (ndx) {\n");
            for (int i = 0; i < optFieldCount; i++) {
                String t = optEvFlds.getOptionalEventFieldTitle(i, isFleet, locale);
                js.append("        case "+i+": return \""+t+"\";\n");
            }
            js.append("    }\n");
        }
        js.append("    return '';\n");
        js.append("};\n");

        return js.toString();
    }

    // -------------------------------

    /* encode a single map event record */
    public String formatMapEvent(
        BasicPrivateLabel privLabel,
        EventDataProvider edp,
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String>iconKeys, 
        boolean isFleet, int stoppedState,
        TimeZone tmz, String dateFmt, String timeFmt)
    {
        char cvsSep = CSV_SEPARATOR_CHAR;
        return this.formatMapEvent(privLabel, edp, 
            iconSelector, iconMap, 
            isFleet, stoppedState,
            tmz, dateFmt, timeFmt, cvsSep);
    }

    /* encode a single map event record */
    // -- NOTE: The format encoded here must match the parser 'getParseMapEventJS'/'MapEventRecord' above
    public String formatMapEvent(
        BasicPrivateLabel privLabel, 
        EventDataProvider edp,
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String> iconKeys, 
        boolean isFleet, int stoppedState,
        TimeZone tmz, String dateFmt, String timeFmt, char csvSep)
    {
        // ID   |DeviceDesc|Epoch     |Date      |Time    |Timezone|StatusCodeDesc|Icon|Latitude |Longitude  |GPSAge|CreateAge|Accuracy|#Sats|SpeedKPH|Heading|Altitude|Odometer|Stopped|GPIO|Address           |extra...
        // demo2|New Device|1268394640|2010/03/12|05:50:40|CST     |InMotion      |19  |37.785248|-121.307275|0     |0        |0.0     |0    |87.2    |227.7  |7       |16124.9 |0      |1234|"I-5, Lathrop, CA"
        // 0---- 1--------- 2--------- 3--------- 4------- 5------- 6------------- 7--- 8-------- 9---------- A----- B-------- C------- D---- E------- F------ G------- H------- I------ J--- K----------------- L-------

        /* valid EventDataProvider? */
        if (edp == null) {
            return "";
        }
        Locale locale = (privLabel != null)? privLabel.getLocale() : null;

        /* start record assembly */
        StringBuffer sb = new StringBuffer();

        /* fld[ 0] VehicleID/VIN (unicode encoded) */
        sb.append(EventUtil._encUnicode(edp.getDeviceVIN(),csvSep));
        sb.append(csvSep);

        /* fld[ 1] DeviceDesc (unicode encoded) */
        sb.append(EventUtil._encUnicode(edp.getDeviceDescription(),csvSep));
        sb.append(csvSep);

        /* fld[ 2] Timestamp */
        long time = edp.getTimestamp();
        sb.append(time);
        sb.append(csvSep);

        /* fld[ 3] Date */
        /* fld[ 4] Time */
        DateTime dt = null;
        if ((dateFmt != null) && (time > 0L)) {
            dt = new DateTime(time,tmz);
            String dtfmt = dateFmt + csvSep + timeFmt;
            sb.append(dt.format(dtfmt));
        } else {
            sb.append(csvSep);
        }
        sb.append(csvSep);

        /* fld[ 5] TimeZone (short name) */
        if (dt != null) {
            //sb.append(dt.getTimeZoneShortName());
            sb.append(dt.format("zzz",tmz)); // PDT
        }
        sb.append(csvSep);

        /* fld[ 6] StatusCode Description (unicode encoded) */
        boolean MAP_INCL_SC_COLOR = privLabel.getBooleanProperty(BasicPrivateLabel.PROP_TrackMap_includeStatusCodeColor, true);
        if (MAP_INCL_SC_COLOR) {
            StatusCodeProvider scp = edp.getStatusCodeProvider(privLabel);
            if (scp == null) {
                String scd = edp.getStatusCodeDescription(privLabel);
                sb.append(EventUtil._encUnicode(scd,csvSep));
            } else
            if (!scp.hasStyle()) {
                String scd = scp.getDescription(privLabel.getLocale());
                sb.append(EventUtil._encUnicode(scd,csvSep));
            } else {
                String scd = scp.getDescription(privLabel.getLocale());
                StringBuffer sbHtml = new StringBuffer();
                sbHtml.append("<span style=\"").append(scp.getStyleString()).append("\">");
                sbHtml.append(StringTools.htmlFilterText(scd));
                sbHtml.append("</span>");
                String scdHtml = EventUtil.MAP_ESCAPE_HTML + Base64.encode(sbHtml.toString());
                sb.append(scdHtml);
            }
        } else {
            String scd = edp.getStatusCodeDescription(privLabel);
            sb.append(EventUtil._encUnicode(scd,csvSep));
        }
        sb.append(csvSep);

        /* fld[ 7] Pushpin Icon */
        //Print.logInfo("isFleet="+isFleet);
        sb.append(edp.getPushpinIconIndex(iconSelector, iconMap, isFleet, privLabel));
        sb.append(csvSep);

        /* fld[ 8] Latitude  (6 decimal places) */
        /* fld[ 9] Longitude (6 decimal places) */
        boolean isGPSLocation = edp.isValidGeoPoint();
        GeoPoint bestGP = isGPSLocation? edp.getGeoPoint() : edp.getBestGeoPoint();
        sb.append(StringTools.format(bestGP.getLatitude() ,"0.000000"));
        sb.append(csvSep);
        sb.append(StringTools.format(bestGP.getLongitude(),"0.000000"));
        sb.append(csvSep);

        /* A fld[10] GPS Age */
        long gpsAge = edp.getGpsAge();
        sb.append(gpsAge);
        sb.append(csvSep);

        /* B fld[11] Creation Age */
        long createAge = edp.getCreationAge();
        sb.append(createAge);
        sb.append(csvSep);

        /* C fld[12] Accuracy (meters) */
        double bestAcc = isGPSLocation? edp.getHorzAccuracy() : edp.getBestAccuracy();
        sb.append(StringTools.format(bestAcc,"0.0"));
        sb.append(csvSep);
        //Print.logInfo("GeoPoint: " + bgp + " [accuracy " + edp.getBestAccuracy() + " meters]");

        /* D fld[13] Satellite Count */
        int satCount = isGPSLocation? edp.getSatelliteCount() : -1;
        sb.append(String.valueOf(satCount));
        sb.append(csvSep);

        /* E fld[14] SpeedKPH */
        sb.append(StringTools.format(edp.getSpeedKPH(),"0.0"));
        sb.append(csvSep);

        /* F fld[15] Heading */
        sb.append(StringTools.format(edp.getHeading(),"0.0"));
        sb.append(csvSep);

        /* G fld[16] Altitude (meters) */
        sb.append(StringTools.format(edp.getAltitude(),"0"));
        sb.append(csvSep);

        /* H fld[17] Odometer (kilometers) */
        sb.append(StringTools.format(edp.getOdometerKM(),"0.0"));
        sb.append(csvSep);

        /* I fld[18] Stopped state */
        sb.append(stoppedState);
        sb.append(csvSep);
        
        /* J fld[19] GPIO Input "gpioInput" */
        sb.append(edp.getInputMask());
        sb.append(csvSep);

        /* K fld[20] Address (unicode encoded) */
        sb.append("\"" + EventUtil._encUnicode(edp.getAddress(),csvSep) + "\""); 

        /* L fld[21]+ other/extra fields? (unicode encoded) */
        OptionalEventFields optEvFlds = EventUtil.getOptionalEventFieldHandler(privLabel);
        if (optEvFlds != null) {
            int optFieldCount = optEvFlds.getOptionalEventFieldCount(isFleet);
            for (int i = 0; i < optFieldCount; i++) {
                String v = StringTools.trim(optEvFlds.getOptionalEventFieldValue(i,isFleet,locale,edp));
                sb.append(csvSep);
                if (v.startsWith(EventUtil.MAP_ESCAPE_HTML)) {
                    String b64 = EventUtil.MAP_ESCAPE_HTML + Base64.encode(v.substring(EventUtil.MAP_ESCAPE_HTML.length()));
                    sb.append(b64);
                } else {
                    sb.append(EventUtil._encUnicode(v,csvSep));
                }
            }
        }

        /* return CSV record */
        String csv = sb.toString();
        //Print.logInfo("Event CSV: " + csv);
        return csv;

    }

    private static String _encUnicode(String str, char csvSep)
    {
        // "V\u00EDa 1 ..."
        // Requires JavaScript function "decodeUnicode" (in "utils.js") 
        StringBuffer sb = new StringBuffer();
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                if (ch < 0x0020) {
                    // ignore control characters (below a space)
                } else
                if ((ch == '\"') || (ch == '\'')) {
                    // ignore quotes
                } else 
                if (ch == csvSep) {
                    // replace csv separator char
                    sb.append(' '); 
                } else
                if (Character.isDigit(ch)) {
                    // accept digits
                    sb.append(ch);
                } else
                if (Character.isWhitespace(ch) || Character.isSpaceChar(ch)) {
                    // convert all whitespace to a simple space
                    sb.append(' ');
                } else
                if ((ch == '\\') || (ch == '/')) {
                    // convert '\' to '/'
                    sb.append('/');
                } else
                if (ch == '<') {
                    // convert '<' to '('
                    sb.append('(');
                } else
                if (ch == '>') {
                    // convert '>' to ')'
                    sb.append(')');
                } else
                if (ch == '%') {
                    // cautiously include '%'
                    sb.append('%');
                } else
                if ("!#$()*+,-.:;=[]^_{}?~@/".indexOf(ch) >= 0) {
                    // accept only these special characters (omitting "&`%")
                    sb.append(ch);
                } else
                if ((ch >= 'A') && (ch <= 'Z')) {
                    // accept upper alpha
                    sb.append(ch);
                } else
                if ((ch >= 'a') && (ch <= 'z')) {
                    // accept lower alpha
                    sb.append(ch);
                } else
                if (ch > 0x007E) {
                    // escape UTF-8
                    sb.append("\\u");
                    sb.append(StringTools.hexNybble((ch >> 12) & 0xF));
                    sb.append(StringTools.hexNybble((ch >>  8) & 0xF));
                    sb.append(StringTools.hexNybble((ch >>  4) & 0xF));
                    sb.append(StringTools.hexNybble( ch        & 0xF));
                    // decoded by javascript "decodeUnicode"
                } else {
                    // ignore anything else
                }
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    private PoiProvider[] _getPOI(String accountID, BasicPrivateLabel privLabel)
    {
        java.util.List<PoiProvider> poiList = privLabel.getPointsOfInterest();
        if (!ListTools.isEmpty(poiList)) {
            //Print.logInfo("POI Count: %d", poiList.size());
            return poiList.toArray(new PoiProvider[poiList.size()]);
        } else {
            //Print.logInfo("POI Count: none");
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /* write encoded map event data to the specified PrintWriter */
    public boolean writeMapEvents(
        int dataFmt, int indentLevel, PrintWriter pwout, 
        boolean isSoapRequest, boolean isTopLevelTag,
        BasicPrivateLabel privLabel,
        EventDataProvider edp[], boolean includeShapes,
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String> iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID,
        TimeZone tmz, 
        Account acct, User user,
        DateTime latestTime, double lastBattery, double lastSignal,
        double minProximityM)
        throws IOException
    {
        if (dataFmt == EventUtil.MAPDATA_XML) {
            Print.logInfo("Writing map events in XML format ...");
            return this.writeMapEvents_xml(
                indentLevel, pwout, 
                isSoapRequest, isTopLevelTag,
                privLabel,
                edp, includeShapes,
                iconSelector, iconMap,
                isFleet, fleetRoute, selID,
                tmz,
                acct, user,
                latestTime, lastBattery, lastSignal,
                minProximityM,
                CSV_SEPARATOR_CHAR);
        } else {
            //Print.logInfo("Writing map events in JSON format ...");
            return this.writeMapEvents_json(
                pwout, 
                isSoapRequest, isTopLevelTag,
                privLabel,
                edp, includeShapes,
                iconSelector, iconMap,
                isFleet, fleetRoute, selID,
                tmz,
                acct, user,
                latestTime, lastBattery, lastSignal,
                minProximityM,
                CSV_SEPARATOR_CHAR);
        }
    }

    // ------------------------------------------------------------------------

    /* write encoded map event data to the specified PrintWriter */
    public boolean writeMapEvents_xml(
        int indentLevel, PrintWriter pwout, 
        boolean isSoapRequest, boolean isTopLevelTag,
        BasicPrivateLabel privLabel, // never null
        EventDataProvider edp[], boolean includeShapes,
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String> iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID,
        TimeZone tmz, 
        Account acct, User user,
        DateTime latestTime, double lastBattery, double lastSignal,
        double minProximityM)
        throws IOException
    {
        return this.writeMapEvents_xml(
            indentLevel, pwout, 
            isSoapRequest, isTopLevelTag,
            privLabel,
            edp, includeShapes,
            iconSelector, iconMap,
            isFleet, fleetRoute, selID,
            tmz,
            acct, user,
            latestTime, lastBattery, lastSignal,
            minProximityM,
            CSV_SEPARATOR_CHAR);
    }

    /* write encoded map event data to the specified PrintWriter */
    private boolean writeMapEvents_xml(
        int indentLevel, PrintWriter pwout, 
        boolean isSoapRequest, boolean isTopLevelTag,
        BasicPrivateLabel privLabel, // never null
        EventDataProvider edp[],  boolean includeShapes,
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String>iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID,
        TimeZone tmz, 
        Account acct, User user,
        DateTime latestTime, double lastBattery, double lastSignal,
        double minProximityM,
        char csvSep)
        throws IOException
    {
        // <?xml version='1.0' encoding='UTF-8' standalone='no' ?>
        // <MapData isFleet="false">
        //   <Time timestamp="EPOCH" timezone="TMZ" year="YYYY" month="MM" day="DD">YYYY/MM/DD|hh:mm:ss</Time>
        //   <LastEvent device="DEVICE" timestamp="EPOCH" timezone="TMZ" year="YYYY" month="MM" day="DD" battery="0.82" signal="0.45">YYYY/MM/DD|hh:mm:ss</LastEvent>
        //   <Shape type="circle" radius="1000" color="#FF0000"><![CDATA[
        //      lat/lon, lat/lon, ...
        //   ]]></Shape>
        //   <DataColumns><![CDATA[
        //      id|desc|epoch|date|time|tmz|status|icon|lat|lon|acc|sats|kph|heading|alt|odomkm|addr
        //   ]]></DataColumns>
        //   <DataSet type="poi">
        //     <P>|POIDesc||||||19|37.783522|-121.225672||0|||||"Address"</P>
        //   </DataSet>
        //   <DataSet type="device" id="deviceid" route="true" routeColor="#FF0000" textColor="#000000">
        //     <P>demo2|New Device|1268394337|2010/03/12|05:45:37|CST|Start|19|37.783522|-121.225672|0.0|0|12.0|269.8|8|16117.3|"778 Mission Ridge Dr, Manteca, CA 95337"</P>
        //   </DataSet>
        //   <Action command="showpp">2</Action>
        //   <Action command="zoompp">2</Action>
        // </MapData>

        /* Locale */
        Locale  locale = privLabel.getLocale(); // should be "reqState.getLocale();"
        I18N    i18n   = I18N.getI18N(EventUtil.class, locale);

        /* account ID */
        String accountID = (acct != null)? acct.getAccountID() : "?";

        /* date/time format */
        String dateFmt = (acct != null)? acct.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt = (acct != null)? acct.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();

        /* TimeZone */
        if ((acct != null) && (tmz == null)) { 
            tmz = acct.getTimeZone(null); 
        }
        String tmzStr = null;
        //tmzStr = (tmz != null)? tmz.getID() : null;
        //tmzStr = (tmz != null)? tmz.getDisplayName(true,TimeZone.SHORT) : null;

        /* refix spacing */
        String PFX1 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest, indentLevel   *3) : "";
        String PFX2 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest,(indentLevel+1)*3) : "";

        /* MIME type */
        //response.setContentType(HTMLTools.MIME_XML()); // HTMLTools.MIME_PLAIN());

        /* XML header */
        if (!isSoapRequest && isTopLevelTag) {
            // "Could not complete the operation due to error c00ce56e"
            this.write(pwout, PFX1);
            this.write(pwout, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        }

        /* "MapData" tag */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_MapData,
            XMLTools.ATTR(ATTR_isFleet,isFleet),
            false,true));

        /* today time */
        // <Time timestamp="EPOCH" timezone="TMZ" year="YYYY" month="MM" day="DD">YYYY/MM/DD|hh:mm:ss</Time>
        DateTime today = new DateTime(tmz);
        String   todayTmzFmt = (tmzStr != null)? tmzStr : today.format("zzz",tmz);
        this.write(pwout, PFX2);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Time,
            XMLTools.ATTR(ATTR_timestamp, today.getTimeSec()) +
            XMLTools.ATTR(ATTR_timezone , todayTmzFmt) +
            XMLTools.ATTR(ATTR_year     , today.getYear(tmz)) +
            XMLTools.ATTR(ATTR_month    , today.getMonth1(tmz)) +
            XMLTools.ATTR(ATTR_day      , today.getDayOfMonth(tmz)),
            false,false));
        this.write(pwout, today.format(dateFmt,tmz) + csvSep + today.format(timeFmt,tmz));
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Time,true));

        /* latest event? */
        // <LastEvent device="DEVICE" timestamp="EPOCH" timezone="TMZ" year="YYYY" month="MM" day="DD" battery="0.82" signal="0.45">YYYY/MM/DD|hh:mm:ss</LastEvent>
        if (!isFleet && (latestTime != null)) {
            String lastTmzFmt = (tmzStr != null)? tmzStr : latestTime.format("zzz",tmz);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_LastEvent,
                XMLTools.ATTR(ATTR_device   , selID) +
                XMLTools.ATTR(ATTR_timestamp, latestTime.getTimeSec()) +
                XMLTools.ATTR(ATTR_timezone , lastTmzFmt) +
                XMLTools.ATTR(ATTR_year     , latestTime.getYear(tmz)) +
                XMLTools.ATTR(ATTR_month    , latestTime.getMonth1(tmz)) +
                XMLTools.ATTR(ATTR_day      , latestTime.getDayOfMonth(tmz)) +
                XMLTools.ATTR(ATTR_battery  , lastBattery) +
                XMLTools.ATTR(ATTR_signal   , lastSignal),
                false,false));
            this.write(pwout, 
                latestTime.format(dateFmt,tmz) + csvSep + 
                latestTime.format(timeFmt,tmz));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_LastEvent,true));
        }

        /* XML: map shapes [MapShape] */
        // <Shape type="circle" radius="0" color="#FF0000"><![CDATA[ <lat>/<lon>,... ]]></Shape>
        boolean includeParkedGeofence = includeShapes;
        if (!includeShapes && !includeParkedGeofence) {
            Print.logDebug("(XML) Geozone shapes are not included");
        } else
        if ((edp != null) && (acct != null)) {
            //Print.logInfo("XML: including Geozones ...");

            /* XML: include all contained geozone? */
            String nearbyGzRadM = StringTools.trim(privLabel.getStringProperty(BasicPrivateLabel.PROP_TrackMap_showNearbyGeozones,""));
            double NEARBY_GEOZONE_RADIUS = 0.0; // was GET_NEARBY_GEOZONES
            if (StringTools.isBlank(nearbyGzRadM) || nearbyGzRadM.equalsIgnoreCase("false")) {
                NEARBY_GEOZONE_RADIUS = 0.0;
            } else
            if (nearbyGzRadM.equalsIgnoreCase("true")) {
                NEARBY_GEOZONE_RADIUS = 1000.0; // default 1000 meters
            } else {
                NEARBY_GEOZONE_RADIUS = StringTools.parseDouble(nearbyGzRadM, 0.0);
                if (NEARBY_GEOZONE_RADIUS > 15000.0) { NEARBY_GEOZONE_RADIUS = 15000.0; } // max radius
            }
            boolean GET_ALL_CONTAINED_GEOZONES = privLabel.getBooleanProperty(BasicPrivateLabel.PROP_TrackMap_showAllContainedGeozones, true);
            if (NEARBY_GEOZONE_RADIUS > 0.0) {
                Print.logDebug("(XML) ["+accountID+"] Including all nearby Geozone shapes found ["+NEARBY_GEOZONE_RADIUS+"]");
            } else
            if (GET_ALL_CONTAINED_GEOZONES) {
                Print.logDebug("(XML) ["+accountID+"] Including all contained Geozone shapes found");
            } else {
                Print.logDebug("(XML) ["+accountID+"] Including only 'geozoneID' Geozone shapes found");
            }

            /* parked zone from Device record */
            if (includeParkedGeofence && !isFleet && 
                !ListTools.isEmpty(edp) && (edp[0] instanceof EventData)) {
                Device dev   = ((EventData)edp[0]).getDevice();
                String devID = (dev != null)? dev.getDeviceID()        : "?";
                double pLat  = (dev != null)? dev.getParkedLatitude()  : 0.0;
                double pLon  = (dev != null)? dev.getParkedLongitude() : 0.0;
                double pRad  = (dev != null)? dev.getParkedRadius()    : 0.0; // meters
                String pDesc = i18n.getString("EventUtil.parked","Parked"); // shape description (XML)
                int    ppNdx = -1; // "park"
                if ((pRad > 0.0) && GeoPoint.isValid(pLat,pLon)) {
                    // -- write Geozone XML
                    Print.logDebug("(XML) ["+accountID+"/"+devID+"] Found parked location: "+pLat+"/"+pLon+" radius="+pRad);
                    this.write(pwout, PFX2);
                    this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Shape,
                        XMLTools.ATTR(ATTR_type   , "circle" ) +
                        XMLTools.ATTR(ATTR_radius , pRad     ) +
                        XMLTools.ATTR(ATTR_color  , "#0000FF") +
                        XMLTools.ATTR(ATTR_desc   , pDesc    ) +
                        XMLTools.ATTR(ATTR_ppNdx  , ppNdx    ) +
                        "",
                        false,false));
                    this.write(pwout, XMLTools.CDATA(isSoapRequest,pLat+"/"+pLon));
                    this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Shape,true));
                }
            }

            /* find EventData Geozones to display */
            GeoBounds gzgb = new GeoBounds();
            Set<String> zoneShapes = null; // to store any displayed GeozoneIDs
            for (EventDataProvider e : edp) {
                String devID = e.getDeviceID();
                GeoPoint egp = e.getGeoPoint();
                gzgb.extendByPoint(egp);
                /* check event "geozoneID" */
                String zid = e.getGeozoneID();
                if (GET_ALL_CONTAINED_GEOZONES || !StringTools.isBlank(zid)) {
                    if (zoneShapes == null) { zoneShapes = new HashSet<String>(); }
                    if (!zoneShapes.contains(zid)) {
                        // -- this 'geozoneID' has not yet been added to our list
                        Geozone zone[] = null;
                        try {
                            if (GET_ALL_CONTAINED_GEOZONES) {
                                zone = Geozone.getGeozones(acct.getAccountID(), egp); // all geozones
                            } else {
                                zone = Geozone.getGeozone(acct, zid); // specific geozone
                            }
                        } catch (DBException dbe) {
                            zone = null;
                        }
                        //Print.logInfo("XML: found Geozones: " + ListTools.size(zone));
                        if (ListTools.size(zone) > 0) {
                            for (int iz = zone.length - 1; iz >= 0; iz--) {
                                Geozone z = zone[iz];
                                String zoneID = z.getGeozoneID();
                                // -- "zoneShapes" already initialized above
                                if (zoneShapes.contains(zoneID)) {
                                    // -- we've already added this one
                                    continue;
                                }
                                zoneShapes.add(zoneID);
                                Print.logDebug("(XML) ["+accountID+"/"+devID+"] Found Geozone: " + zoneID);
                                // -- get zone type/radius/color/etc
                                String type = "circle";
                                switch (Geozone.getGeozoneType(z)) {
                                    case POINT_RADIUS: type = "circle";    break;
                                    case BOUNDED_RECT: type = "rectangle"; break;
                                    case POLYGON     : type = "polygon";   break;
                                    default          : continue; // not supported
                                }
                                int  radiusM = z.getRadius();
                                String color = z.getShapeColor("#00FF00");
                                String zDesc = z.getDescription(); // TODO: shape description (XML)
                                int    ppNdx = z.getPushpinIconIndex(iconMap); // z.getPushpinID();
                                GeoPoint gpList[] = z.getGeoPoints();
                                // -- write Geozone XML
                                this.write(pwout, PFX2);
                                this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Shape,
                                    XMLTools.ATTR(ATTR_type   , type   ) +
                                    XMLTools.ATTR(ATTR_radius , radiusM) +
                                    XMLTools.ATTR(ATTR_color  , color  ) +
                                    XMLTools.ATTR(ATTR_desc   , zDesc  ) +
                                    XMLTools.ATTR(ATTR_ppNdx  , ppNdx  ) +
                                    "",
                                    false,false));
                                this.write(pwout, XMLTools.CDATA(isSoapRequest,StringTools.join(gpList,",")));
                                this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Shape,true));
                                //Print.logInfo("XML: wrote Geozones: " + z.getGeozoneID());
                            }
                        } else
                        if (!StringTools.isBlank(zid)) {
                            // -- "zoneShapes" already initialized above
                            zoneShapes.add(zid); // also add if 'zone' is null (so we don't try this again)
                        }
                    } // new geozone
                } // get event geozones
            } // loop through events

            /* get nearby Geozones */
            if (NEARBY_GEOZONE_RADIUS > 0.0) {
                //double extraExtM = gzgb.getDiagonalMeters() * 0.10;
                //gzgb.extendByRadius((extraExtM > 1000.0)? extraExtM : 1000.0);
                gzgb.extendByRadius(NEARBY_GEOZONE_RADIUS); // meters
                Geozone zone[] = null;
                try {
                    zone = Geozone.getGeozones(acct.getAccountID(), gzgb);
                } catch (DBException dbe) {
                    zone = null;
                }
                if (ListTools.size(zone) > 0) {
                    if (zoneShapes == null) { zoneShapes = new HashSet<String>(); } // 2.5.6-B11
                    for (int iz = zone.length - 1; iz >= 0; iz--) {
                        Geozone z = zone[iz];
                        String zoneID = z.getGeozoneID();
                        // -- "zoneShapes" already initialized above
                        if (zoneShapes.contains(zoneID)) {
                            // we've already added this one
                            continue;
                        }
                        zoneShapes.add(zoneID);
                        Print.logDebug("(XML) ["+accountID+"] Found Nearby Geozone: " + zoneID);
                        // -- get zone type/radius/color/etc
                        String type = "circle";
                        switch (Geozone.getGeozoneType(z)) {
                            case POINT_RADIUS: type = "circle";    break;
                            case BOUNDED_RECT: type = "rectangle"; break;
                            case POLYGON     : type = "polygon";   break;
                            default          : continue; // not supported
                        }
                        int  radiusM = z.getRadius();
                        String color = z.getShapeColor("#00FF00");
                        String zDesc = z.getDescription(); // shape description (XML)
                        int    ppNdx = z.getPushpinIconIndex(iconMap); // z.getPushpinID();
                        GeoPoint gpList[] = z.getGeoPoints();
                        // -- write Geozone XML
                        this.write(pwout, PFX2);
                        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Shape,
                            XMLTools.ATTR(ATTR_type   , type   ) +
                            XMLTools.ATTR(ATTR_radius , radiusM) +
                            XMLTools.ATTR(ATTR_color  , color  ) +
                            XMLTools.ATTR(ATTR_desc   , zDesc  ) +
                            XMLTools.ATTR(ATTR_ppNdx  , ppNdx  ) +
                            "",
                            false,false));
                        this.write(pwout, XMLTools.CDATA(isSoapRequest,StringTools.join(gpList,",")));
                        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Shape,true));
                        //Print.logInfo("XML: wrote Geozones: " + z.getGeozoneID());
                    }
                }
            }

        }

        /* column headers "DataColumns" */
        this.write(pwout, PFX2);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DataColumns,"",false,false));
        this.write(pwout, XMLTools.CDATA(isSoapRequest, // see "formatMapEvent"
            "id|desc|epoch|date|time|tmz|status|icon|lat|lon|acc|sats|kph|heading|alt|odomkm|addr|" // |extra...
            ));
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DataColumns,true));

        // <DataSet type="poi">
        this._writeMapPoi_xml(
            (indentLevel>0)?(indentLevel+1):0, pwout, isSoapRequest,
            privLabel,
            this._getPOI(((acct != null)? acct.getAccountID() : null), privLabel), 
            iconMap, 
            csvSep);

        // <DataSet type="device" id="deviceid" route="true">
        boolean rtn = this._writeMapEvents_xml(
            (indentLevel>0)?(indentLevel+1):0, pwout, 
            isSoapRequest,
            privLabel,
            edp, 
            iconSelector, iconMap, 
            isFleet, fleetRoute, selID,
            tmz, dateFmt, timeFmt, 
            csvSep,
            minProximityM);

        /* XML footer */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_MapData,true));

        /* flush (output may not occur until the PrintWriter is flushed) */
        this.flush(pwout);
        return rtn;

    }

    /* write encoded map points-of-interest to the specified PrintWriter */
    private boolean _writeMapPoi_xml(
        int indentLevel, PrintWriter pwout, boolean isSoapRequest,
        BasicPrivateLabel privLabel,
        PoiProvider poip[], 
        OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String> iconKeys, 
        char csvSep)
        throws IOException
    {

        /* valid EventDataProvider? */
        if (ListTools.isEmpty(poip)) {
            //Print.logInfo("No PointsOfInterest ...");
            return false;
        }

        /* indent */
        String PFX1 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest, indentLevel   *3) : "";
        String PFX2 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest,(indentLevel+1)*3) : "";

        /* header */
        String type = DSTYPE_poi;
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DataSet,
            XMLTools.ATTR(ATTR_type     , type ) +
            XMLTools.ATTR(ATTR_route    , false),
            false,true));

        /* points of interest */
        for (int i = 0; i < poip.length; i++) {
            final PoiProvider pp = poip[i];
            EventDataProvider edp = new EventDataProviderAdapter() {
                public String getAccountID()         { return pp.getAccountID(); }
                public String getDeviceID()          { return pp.getPoiID(); }
                public String getDeviceDescription() { return pp.getPoiDescription(); }
                public double getLatitude()          { return pp.getLatitude(); }
                public double getLongitude()         { return pp.getLongitude(); }
                public String getAddress()           { return pp.getAddress(); }
                public int    getPushpinIconIndex(String iconSelector, OrderedMap<String,PushpinIcon> iconMap, boolean isFleet, BasicPrivateLabel bpl) { return pp.getPushpinIconIndex(iconMap,bpl); }
            };
            Print.logDebug("(XML) Found POI: " + edp.getGeoPoint());
            String rcd = this.formatMapEvent(privLabel, edp, // POI XML
                null/*iconSelector*/, iconMap, 
                false/*isFleet*/, 1/*stoppedState*/,
                null/*TimeZone*/, null/*dateFmt*/, null/*timeFmt*/, csvSep);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Point,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,rcd));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Point,true));
        }

        /* footer */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DataSet,true));

        /* flush (output may not occur until the PrintWriter is flushed) */
        this.flush(pwout);
        return true;

    }

    /* write encoded map event data to the specified PrintWriter */
    private boolean _writeMapEvents_xml(
        int indentLevel, PrintWriter pwout, 
        boolean isSoapRequest,
        BasicPrivateLabel privLabel, // should never null
        EventDataProvider edp[], 
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String> iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID, // "selID" is either a DeviceID or GroupID
        TimeZone tmz, 
        String dateFmt, String timeFmt, 
        char csvSep,
        double minProximityM)
        throws IOException
    {

        /* valid EventDataProvider? */
        if (ListTools.isEmpty(edp)) {
            return false;
        }

        /* use custom Device 'displayColor' for routeLine color? */
        boolean useRouteDisplayColor = (privLabel != null)?
            privLabel.getBooleanProperty(BasicPrivateLabel.PROP_TrackMap_useRouteDisplayColor, true) :
            true;

        /* indent */
        String PFX1 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest, indentLevel   *3) : "";
        String PFX2 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest,(indentLevel+1)*3) : "";

        /* print events (XML) */
        boolean  isDeviceData  = !isFleet;
        boolean  didStartSet   = false;
        GeoPoint lastGP        = null;
        String   lastDevID     = "";
        String   routeColor    = "";
        String   textColor     = "";
        int      evNdx         = 0;
        boolean  startStopOK   = false;
        int      stoppedState  = -1; // -1=uninitialized, 0=moving, 1=stopped, 2=stopEvent
        for (int i = 0; i < edp.length; i++) {
            EventData ev = (edp[i] instanceof EventData)? (EventData)edp[i] : null; // likely not-null
            String thisDevID = edp[i].getDeviceID();

            /* device changed? (XML) */
            if (!thisDevID.equals(lastDevID)) {
                Device dev = ((EventData)edp[i]).getDevice();
                if (isFleet /*&& fleetRoute*/) {
                    if (didStartSet) {
                        // close previous dataset
                        this.write(pwout, PFX1);
                        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DataSet,true));
                        didStartSet = false;
                    }
                    isDeviceData = true;
                    selID        = thisDevID;
                }
                lastDevID    = thisDevID;
                lastGP       = null;
                textColor    = "";
                routeColor   = "";
                startStopOK  = ((dev != null) && dev.getStartStopSupported())? true : false;
                if ((edp[i] instanceof EventData) && (isFleet || useRouteDisplayColor)) {
                    if ((dev != null) && dev.hasDisplayColor()) {
                        if (isFleet) {
                            textColor = dev.getDisplayColor();
                        }
                        if (useRouteDisplayColor) {
                            routeColor = dev.getDisplayColor();
                        }
                    }
                }
            }

            /* event index */
            edp[i].setEventIndex(evNdx++); // TODO: 

            /* stopped/moving? (XML) */
            if (ev == null) {
                // not an EventData record
                stoppedState = (edp[i].getSpeedKPH() <= 0.0)? 1/*stopped*/ : 0/*moving*/; 
            } else
            if (!startStopOK) {
                // does not support start/stop
                stoppedState = (edp[i].getSpeedKPH() <= 0.0)? 1/*stopped*/ : 0/*moving*/; 
                ev.setStopped(stoppedState == 1);
            } else {
                // init stoppedState
                if (stoppedState < 0) { 
                    // Regardless of the speed value, we really don't know if we should be
                    // in a stopped state or in-motion state.  If we arbitrarily decide we
                    // are stopped because of a currently zero-speed, then possibly all 
                    // following events will have a stopped pushpin, even though we should
                    // be moving.  For this reason, we try to guess conservatively, and
                    // commit to an "in-motion" detected state, but do not commit to a 
                    // possible "stopped" detected state.
                    stoppedState = (edp[i].getSpeedKPH() <= 0.0)? -1/*unknown*/ : 0/*moving*/; 
                }
                // check previous stoppedState
                if (stoppedState < 0) {
                    // still unknown [state -1]
                } else
                if (stoppedState > 0) {
                    // previously "stopped" [state 1 or 2]
                    if (ev == null) {
                        // not an EventData record, just check speed
                        stoppedState = (edp[i].getSpeedKPH() <= 0.0)? 1/*stopped*/ : 0/*moving*/;
                    } else
                    if (ev.isStartEvent(true/*defaultToSpeedCheck*/)) { 
                        // was "stopped", now "moving"
                        stoppedState = 0; // moving
                        ev.setStopped(false);
                    } else {
                        // still "stopped"
                        stoppedState = 1; // stopped
                        ev.setStopped(true);
                    }
                } else {
                    // previously moving [state 0]
                    if (ev == null) {
                        // not an EventData record, just check speed
                        stoppedState = (edp[i].getSpeedKPH() <= 0.0)? 1/*stopped*/ : 0/*moving*/;
                    } else
                    if (ev.isStopEvent(true/*defaultToSpeedCheck*/)) {
                        // was "moving", now "stopped"
                        stoppedState = 2; // stopEvent
                        ev.setStopped(true);
                    } else {
                        // still "moving"
                        stoppedState = 0; // moving
                        ev.setStopped(false);
                    }
                }
            }

            /* last event for device? */
            if ((i + 1) >= edp.length) {
                // last event in list
                //Print.logInfo("Setting last event ...");
                edp[i].setIsLastEvent(true);
            } else {
                String nextDevID = edp[i + 1].getDeviceID();
                if (!thisDevID.equals(nextDevID)) {
                    // DeviceID will change on next iteration
                    edp[i].setIsLastEvent(true);
                    evNdx = 0; // reset
                }
            }

            /* trim events in close proximity */
            if (minProximityM > 0.0) {
                // check proximity to established target location (range 20-200 metera)
                double lat = edp[i].getLatitude();
                double lon = edp[i].getLongitude();
                if (GeoPoint.isValid(lat,lon)) {
                    // guarantee that this point is valid
                    GeoPoint thisGP = new GeoPoint(lat,lon);
                    if (lastGP == null) {
                        // the first 'last' event, set target location and continue
                        lastGP = thisGP;
                    } else
                    if (thisGP.metersToPoint(lastGP) >= minProximityM) {
                        // outside tolerance zone, set new target location and continue
                        lastGP = thisGP;
                    } else {
                        // inside tolerance zone, skip this event
                        //Print.logError(i + ") Skipping this record!");
                        continue;
                    }
                }
            }

            /* start "DataSet" (if not already started) */
            if (!didStartSet) {
                String type = isDeviceData? DSTYPE_device : DSTYPE_group; // "poi"
                this.write(pwout, PFX1);
                this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DataSet,
                    XMLTools.ATTR(ATTR_type      , type        ) +
                    XMLTools.ATTR(ATTR_id        , selID       ) +
                    XMLTools.ATTR(ATTR_route     , isDeviceData) +
                    XMLTools.ATTR(ATTR_routeColor, routeColor  ) +
                    XMLTools.ATTR(ATTR_textColor , textColor   ),
                    false,true));
                didStartSet = true;
                //Print.logWarn(i + ") New DataSet: " + selID);
            }

            /* fleet icon [XML] */
            boolean showFleetIcon;
            if (!isFleet) {
                // -- not a 'fleet' map, do not show fleet icon
                showFleetIcon = false;
            } else {
                String sfi = privLabel.getStringProperty(BasicPrivateLabel.PROP_TrackMap_showFleetMapDevicePushpin,"");
                if (StringTools.isBlank(sfi) || sfi.equalsIgnoreCase("default")) {
                    if (!fleetRoute) {
                        // -- fleet map, single point, show fleet icon
                        showFleetIcon = true;
                    } else {
                        // -- fleet map, multiple points, show fleet icon if last event
                        showFleetIcon = edp[i].getIsLastEvent();
                    }
                } else {
                    // -- 'true' will display all device pushpins
                    // -- 'false' will display the default pushpins
                    showFleetIcon = StringTools.parseBoolean(sfi,false);
                }
            }
            //Print.logInfo("isFleet="+isFleet + ", showFleetIcon="+showFleetIcon);

            /* format and print event */
            String rcd = this.formatMapEvent(privLabel, edp[i], // Events XML
                iconSelector, iconMap, 
                showFleetIcon/*isFleet*/, stoppedState,
                tmz, dateFmt, timeFmt, csvSep);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Point,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,rcd));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Point,true));

        } // looping through events

        /* Dataset footer */
        if (didStartSet) {
            this.write(pwout, PFX1);
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DataSet,true));
        }

        return true;

    }

    // ------------------------------------------------------------------------

    /* write encoded map event data to the specified PrintWriter */
    private boolean writeMapEvents_json(
        //HttpServletResponse response,
        PrintWriter pwout, 
        boolean isSoapRequest, boolean isTopLevelTag,
        BasicPrivateLabel privLabel,
        EventDataProvider edp[], boolean includeShapes,
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String> iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID,
        TimeZone tmz, 
        Account acct, User user,
        DateTime latestTime, double lastBattery, double lastSignal,
        double minProximityM)
        throws IOException
    {
        return this.writeMapEvents_json(
            //response,
            pwout, 
            isSoapRequest, isTopLevelTag,
            privLabel,
            edp, includeShapes,
            iconSelector, iconMap,
            isFleet, fleetRoute, selID,
            tmz,
            acct, user,
            latestTime, lastBattery, lastSignal,
            minProximityM,
            CSV_SEPARATOR_CHAR);
    }

    /* write encoded map event data to the specified PrintWriter */
    private boolean writeMapEvents_json(
        //HttpServletResponse response,
        PrintWriter pwout, 
        boolean isSoapRequest, boolean isTopLevelTag,
        BasicPrivateLabel privLabel,
        EventDataProvider edp[],  boolean includeShapes,
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String>iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID,
        TimeZone tmz, 
        Account acct, User user,
        DateTime latestTime, double lastBattery, double lastSignal,
        double minProximityM,
        char csvSep)
        throws IOException
    {
        JSON._Object JMapData = this.getJMapData_JSON(
            privLabel,
            edp, includeShapes,
            iconSelector, iconMap,
            isFleet, fleetRoute, selID,
            tmz,
            acct, user,
            latestTime, lastBattery, lastSignal,
            minProximityM,
            null/*actions*/,
            csvSep);
        JSON._Object jsonObj = new JSON._Object();
        jsonObj.addKeyValue(JSON_JMapData, JMapData);
        String jsonStr = jsonObj.toString(false);
        //Print.logInfo("Return JMapData request:\n"+jsonStr);
        //response.setContentType(HTMLTools.MIME_JSON()); // HTMLTools.MIME_PLAIN());
        this.write(pwout, jsonStr);
        this.flush(pwout);
        return true;
    }

    /* Returns a JSON object containing the shapes and events to display on the map */
    public JSON._Object getJMapData_JSON(
        BasicPrivateLabel privLabel,
        EventDataProvider edp[],  boolean includeShapes,
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String>iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID,
        TimeZone tmz, 
        Account acct, User user,
        DateTime latestTime, double lastBattery, double lastSignal,
        double minProximityM,
        Collection<String> actions,
        char csvSep)
    {
        // {
        //   "JMapData" : {
        //      "isFleet": false,
        //      "Time": {
        //          "timestamp": EPOCH,
        //          "timezone": "TMZ",
        //          "ymd" : { YYYY:year, MM:month1, DD:day },
        //          "Data": "YYYY/MM/DD|hh:mm:ss"
        //      },
        //      "LastEvent": {
        //          "device": "DEVICE",
        //          "timestamp": EPOCH,
        //          "timezone": "TMZ",
        //          "ymd" : { YYYY:year, MM:month1, DD:day },
        //          "battery": 0.42,
        //          "signal": 0.45,
        //          "Data": "YYYY/MM/DD|hh:mm:ss"
        //      },
        //      "Shapes": [
        //          {
        //              "type": "circle",
        //              "radius": 1000,
        //              "color": "#FF0000",
        //              "Points": [
        //                  "lat/lon", "lat/lon", ...
        //              ]
        //          }
        //       ],
        //      "DataColumns": "Desc|Epoch|Date|Time|Tmz|Stat|Icon|Lat|Lon|#Sats|kph|Heading|Alt|Addr",
        //      "DataSets": [
        //          {
        //              "type": "poi",
        //              "route": "false",
        //              "Points": [
        //                  "POIDesc|||0|Latitude|Longitude|0.0|0.0|0.0|Address",
        //                  ...
        //              ],
        //          },
        //          {
        //              "type": "device",
        //              "id": "deviceid",
        //              "route": "true",
        //              "routeColor": "#FF0000",
        //              "textColor": "#FF0000",
        //              "Points": [
        //                  "DeviceDesc|Data|Time|StatusCode|Latitude|Longitude|SpeedKPH|Heading|Altitude|Address",
        //                  ...
        //              ],
        //          }
        //     ],
        //     "Actions": [
        //          {
        //              "command": "showpp",
        //              "arg": "2"
        //          },
        //          {
        //              "command": "zoompp",
        //              "arg": "2"
        //          },
        //     ]
        // }

        /* Locale */
        Locale  locale   = (privLabel != null)? privLabel.getLocale() : null; // should be "reqState.getLocale();"
        I18N    i18n     = I18N.getI18N(EventUtil.class, locale);

        /* account ID */
        String accountID = (acct != null)? acct.getAccountID() : "?";

        /* date/time format */
        String dateFmt = (acct != null)? acct.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt = (acct != null)? acct.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();

        /* TimeZone */
        if ((acct != null) && (tmz == null)) { 
            tmz = acct.getTimeZone(null); 
        }
        String tmzStr = null;
        //tmzStr = (tmz != null)? tmz.getID() : null;
        //tmzStr = (tmz != null)? tmz.getDisplayName(true,TimeZone.SHORT) : null;

        /* MapData JSON object */
        JSON._Object jMapDataObj = new JSON._Object();

        /* isFleet */
        jMapDataObj.addKeyValue(ATTR_isFleet, isFleet);

        /* today time */
        // "Time": {
        //    "timestamp": EPOCH,
        //    "timezone": "TMZ",
        //    "ymd": { YYYY:2011, MM:9, DD:12 },
        //    "date": "YYYY/MM/DD",
        //    "time": "hh:mm:ss"
        // },
        {
            DateTime today = new DateTime(tmz);
            String   todayTmzFmt = (tmzStr != null)? tmzStr : today.format("zzz",tmz);
            JSON._Object timeObj = new JSON._Object();
            timeObj.addKeyValue(ATTR_timestamp, today.getTimeSec());
            timeObj.addKeyValue(ATTR_timezone , todayTmzFmt);
            JSON._Object ymdObj = new JSON._Object();
            ymdObj.addKeyValue(JSON_year , today.getYear(tmz));
            ymdObj.addKeyValue(JSON_month, today.getMonth1(tmz));
            ymdObj.addKeyValue(JSON_day  , today.getDayOfMonth(tmz));
            timeObj.addKeyValue(JSON_YMD , ymdObj);
            timeObj.addKeyValue(JSON_date, today.format(dateFmt,tmz));
            timeObj.addKeyValue(JSON_time, today.format(timeFmt,tmz));
            jMapDataObj.addKeyValue(JSON_Time,timeObj);
        }

        /* latest event? */
        // "LastEvent": {
        //    "device": "DEVICE",
        //    "timestamp": EPOCH,
        //    "timezone": "TMZ",
        //    "ymd": { YYYY:2011, MM:9, DD:12 },
        //    "date": "YYYY/MM/DD",
        //    "time": "hh:mm:ss"
        //    "battery": 0.42,
        //    "signal": 0.45,
        // },
        if (!isFleet && (latestTime != null)) {
            String lastTmzFmt = (tmzStr != null)? tmzStr : latestTime.format("zzz",tmz);
            JSON._Object lastEventObj = new JSON._Object();
            lastEventObj.addKeyValue(ATTR_device   , selID);
            lastEventObj.addKeyValue(ATTR_timestamp, latestTime.getTimeSec());
            lastEventObj.addKeyValue(ATTR_timezone , lastTmzFmt);
            JSON._Object ymdObj = new JSON._Object();
            ymdObj.addKeyValue(JSON_year , latestTime.getYear(tmz));
            ymdObj.addKeyValue(JSON_month, latestTime.getMonth1(tmz));
            ymdObj.addKeyValue(JSON_day  , latestTime.getDayOfMonth(tmz));
            lastEventObj.addKeyValue(JSON_YMD     , ymdObj);
            lastEventObj.addKeyValue(JSON_date    , latestTime.format(dateFmt,tmz));
            lastEventObj.addKeyValue(JSON_time    , latestTime.format(timeFmt,tmz));
            lastEventObj.addKeyValue(ATTR_battery , lastBattery);
            lastEventObj.addKeyValue(ATTR_signal  , lastSignal);
            jMapDataObj.addKeyValue(JSON_LastEvent,lastEventObj);
        }

        /* JSON: map shapes (EXPERIMENTAL) [MapShape] */
        // "Shapes": [
        //    {
        //      "type": "circle",
        //      "radius": 1000,
        //      "color": "#FF0000",
        //      "Points": [
        //          "lat/lon", "lat/lon", ...
        //      ]
        //    }
        // ],
        boolean includeParkedGeofence = includeShapes;
        if (!includeShapes && !includeParkedGeofence) {
            Print.logDebug("(JSON) ["+accountID+"] Geozone shapes are not included");
        } else
        if ((edp != null) && (acct != null)) {
            //Print.logInfo("JSON: including Geozones ...");
            JSON._Array shapeArray = new JSON._Array();

            /* JSON: include all contained geozone? */
            String nearbyGzRadM = StringTools.trim(privLabel.getStringProperty(BasicPrivateLabel.PROP_TrackMap_showNearbyGeozones,""));
            double NEARBY_GEOZONE_RADIUS = 0.0; // was GET_NEARBY_GEOZONES
            if (StringTools.isBlank(nearbyGzRadM) || nearbyGzRadM.equalsIgnoreCase("false")) {
                NEARBY_GEOZONE_RADIUS = 0.0;
            } else
            if (nearbyGzRadM.equalsIgnoreCase("true")) {
                NEARBY_GEOZONE_RADIUS = 1000.0; // default 1000 meters
            } else {
                NEARBY_GEOZONE_RADIUS = StringTools.parseDouble(nearbyGzRadM, 0.0);
                if (NEARBY_GEOZONE_RADIUS > 10000.0) { NEARBY_GEOZONE_RADIUS = 10000.0; } // max radius
            }
            boolean GET_ALL_CONTAINED_GEOZONES = privLabel.getBooleanProperty(BasicPrivateLabel.PROP_TrackMap_showAllContainedGeozones, true);
            if (NEARBY_GEOZONE_RADIUS > 0.0) {
                Print.logDebug("(JSON) ["+accountID+"] Including all nearby Geozone shapes found ["+NEARBY_GEOZONE_RADIUS+"]");
            } else
            if (GET_ALL_CONTAINED_GEOZONES) {
                Print.logDebug("(JSON) ["+accountID+"] Including all contained Geozone shapes found");
            } else {
                Print.logDebug("(JSON) ["+accountID+"] Including only 'geozoneID' Geozone shapes found");
            }

            /* parked zone from Device record */
            if (includeParkedGeofence && !isFleet && 
                !ListTools.isEmpty(edp) && (edp[0] instanceof EventData)) {
                Device dev   = ((EventData)edp[0]).getDevice();
                String devID = (dev != null)? dev.getDeviceID()        : "?";
                double pLat  = (dev != null)? dev.getParkedLatitude()  : 0.0;
                double pLon  = (dev != null)? dev.getParkedLongitude() : 0.0;
                double pRad  = (dev != null)? dev.getParkedRadius()    : 0.0; // meters
                String pDesc = i18n.getString("EventUtil.parked","Parked"); // shape description (JSON) I18N?
                int    ppNdx = -1; // "park"
                if ((pRad > 0.0) && GeoPoint.isValid(pLat,pLon)) {
                    // -- write Geozone JSON
                    Print.logDebug("(JSON) ["+accountID+"/"+devID+"] Found parked location: "+pLat+"/"+pLon+" radius="+pRad);
                    JSON._Object shapeObj = new JSON._Object();
                    shapeObj.addKeyValue(JSON_type   , "circle" );  // ATTR_type
                    shapeObj.addKeyValue(JSON_radius , pRad     );  // ATTR_radius
                    shapeObj.addKeyValue(JSON_color  , "#0000FF");  // ATTR_color
                    shapeObj.addKeyValue(JSON_desc   , pDesc);      // ATTR_desc
                    shapeObj.addKeyValue(JSON_ppNdx  , ppNdx);      // ATTR_ppNdx
                    JSON._Array pointArray = new JSON._Array();
                    pointArray.addValue(pLat+"/"+pLon);
                    shapeObj.addKeyValue(JSON_Points, pointArray);
                    shapeArray.addValue(shapeObj);
                }
            }

            /* find EventData Geozones to display */
            GeoBounds gzgb = new GeoBounds();
            Set<String> zoneShapes = null; // to store any displayed GeozoneIDs
            for (EventDataProvider e : edp) {
                String devID = e.getDeviceID();
                GeoPoint egp = e.getGeoPoint();
                gzgb.extendByPoint(egp);
                /* check event "geozoneID" */
                String zid = e.getGeozoneID();
                if (GET_ALL_CONTAINED_GEOZONES || !StringTools.isBlank(zid)) {
                    if (zoneShapes == null) { zoneShapes = new HashSet<String>(); }
                    if (!zoneShapes.contains(zid)) {
                        // -- this 'geozoneID' has not yet been added to our list
                        Geozone zone[] = null;
                        try {
                            if (GET_ALL_CONTAINED_GEOZONES) {
                                zone = Geozone.getGeozones(acct.getAccountID(), egp); // all geozones
                            } else {
                                zone = Geozone.getGeozone(acct, zid); // specific geozone
                            }
                        } catch (DBException dbe) {
                            zone = null;
                        }
                        //Print.logInfo("JSON: found Geozones: " + ListTools.size(zone));
                        if (ListTools.size(zone) > 0) {
                            for (int iz = zone.length - 1; iz >= 0; iz--) {
                                Geozone z = zone[iz];
                                String zoneID = z.getGeozoneID();
                                // -- "zoneShapes" already initialized above
                                if (zoneShapes.contains(zoneID)) {
                                    // -- we've already added this one
                                    continue;
                                }
                                zoneShapes.add(zoneID);
                                Print.logDebug("(JSON) ["+accountID+"/"+devID+"] Found Geozone: " + zoneID);
                                // -- get zone type/radius/color/etc
                                String type = "circle";
                                switch (Geozone.getGeozoneType(z)) {
                                    case POINT_RADIUS: type = "circle";    break;
                                    case BOUNDED_RECT: type = "rectangle"; break;
                                    case POLYGON     : type = "polygon";   break;
                                    default          : continue; // not supported?
                                }
                                int  radiusM = z.getRadius();
                                String color = z.getShapeColor("#00FF00");
                                String zDesc = z.getDescription(); // TODO: shape description (JSON)
                                int    ppNdx = z.getPushpinIconIndex(iconMap); // z.getPushpinID()
                                GeoPoint gpList[] = z.getGeoPoints();
                                // -- write Geozone XML
                                JSON._Object shapeObj = new JSON._Object();
                                shapeObj.addKeyValue(JSON_type   , type);    // ATTR_type
                                shapeObj.addKeyValue(JSON_radius , radiusM); // ATTR_radius
                                shapeObj.addKeyValue(JSON_color  , color);   // ATTR_color
                                shapeObj.addKeyValue(JSON_desc   , zDesc);   // ATTR_desc
                                shapeObj.addKeyValue(JSON_ppNdx  , ppNdx);   // ATTR_ppNdx
                                JSON._Array pointArray = new JSON._Array();
                                for (GeoPoint gp : gpList) {
                                    pointArray.addValue(gp.toString());
                                }
                                shapeObj.addKeyValue(JSON_Points, pointArray);
                                shapeArray.addValue(shapeObj);
                                //Print.logInfo("JSON: wrote Geozones: " + z.getGeozoneID());
                            }
                        } else
                        if (!StringTools.isBlank(zid)) {
                            // -- "zoneShapes" already initialzed above
                            zoneShapes.add(zid); // also add if 'zone' is null (so we don't try this again)
                        }
                    } // new geozone
                } // get event geozones
            } // loop through events

            /* get nearby Geozones */
            if (NEARBY_GEOZONE_RADIUS > 0.0) {
                //double extraExtM = gzgb.getDiagonalMeters() * 0.10;
                //gzgb.extendByRadius((extraExtM > 1000.0)? extraExtM : 1000.0);
                gzgb.extendByRadius(NEARBY_GEOZONE_RADIUS); // meters
                Geozone zone[] = null;
                try {
                    zone = Geozone.getGeozones(acct.getAccountID(), gzgb);
                } catch (DBException dbe) {
                    zone = null;
                }
                if (ListTools.size(zone) > 0) {
                    if (zoneShapes == null) { zoneShapes = new HashSet<String>(); } // 2.5.6-B11
                    for (int iz = zone.length - 1; iz >= 0; iz--) {
                        Geozone z = zone[iz];
                        String zoneID = z.getGeozoneID();
                        // -- "zoneShapes" already initialized above
                        if (zoneShapes.contains(zoneID)) {
                            // we've already added this one
                            continue;
                        }
                        zoneShapes.add(zoneID);
                        Print.logDebug("(JSON) ["+accountID+"] Found Nearby Geozone: " + zoneID);
                        // -- get zone type/radius/color/etc
                        String type = "circle";
                        switch (Geozone.getGeozoneType(z)) {
                            case POINT_RADIUS: type = "circle";    break;
                            case BOUNDED_RECT: type = "rectangle"; break;
                            case POLYGON     : type = "polygon";   break;
                            default          : continue; // not supported
                        }
                        int  radiusM = z.getRadius();
                        String color = z.getShapeColor("#00FF00");
                        String zDesc = z.getDescription(); // shape description (JSON)
                        int    ppNdx = z.getPushpinIconIndex(iconMap); // z.getPushpinID()
                        GeoPoint gpList[] = z.getGeoPoints();
                        // -- write Geozone XML
                        JSON._Object shapeObj = new JSON._Object();
                        shapeObj.addKeyValue(JSON_type   , type);       // ATTR_type
                        shapeObj.addKeyValue(JSON_radius , radiusM);    // ATTR_radius
                        shapeObj.addKeyValue(JSON_color  , color);      // ATTR_color
                        shapeObj.addKeyValue(JSON_desc   , zDesc);      // ATTR_desc
                        shapeObj.addKeyValue(JSON_ppNdx  , ppNdx);      // ATTR_ppNdx
                        JSON._Array pointArray = new JSON._Array();
                        for (GeoPoint gp : gpList) {
                            pointArray.addValue(gp.toString());
                        }
                        shapeObj.addKeyValue(JSON_Points, pointArray);
                        shapeArray.addValue(shapeObj);
                        //Print.logInfo("JSON: wrote Geozones: " + z.getGeozoneID());
                    }
                }
            }

            /* add shapes */
            if (!shapeArray.isEmpty()) {
                jMapDataObj.addKeyValue(JSON_Shapes, shapeArray);
            }

        }

        /* column headers */
        // "DataColumns": "Desc|Epoch|Date|Time|Tmz|Stat|Icon|Lat|Lon|#Sats|kph|Heading|Alt|Addr",
        jMapDataObj.addKeyValue(JSON_DataColumns,"Desc|Epoch|Date|Time|Tmz|Stat|Icon|Lat|Lon|#Sats|kph|Heading|Alt|Addr");

        /* DataSets */
        {
            JSON._Array dataSetArray = new JSON._Array();
            // {
            //    "type": "poi",
            //    "route": "false",
            //    "Points": [
            //      "POIDesc|||0|Latitude|Longitude|0.0|0.0|0.0|Address",
            //      ...
            //    ],
            // },
            this._getMapPoi_json(
                dataSetArray,
                privLabel,
                this._getPOI(((acct != null)? acct.getAccountID() : null), privLabel), 
                iconMap, 
                csvSep);
            // {
            //    "type": "device",
            //    "id": "deviceid",
            //    "route": "true",
            //    "routeColor": "#FF0000",
            //    "textColor": "#FF0000",
            //    "Points": [
            //      "DeviceDesc|Data|Time|StatusCode|Latitude|Longitude|SpeedKPH|Heading|Altitude|Address",
            //      ...
            //    ],
            // }
            // <DataSet type="device" id="deviceid" route="true">
            boolean rtn = this._addDataSet_json(
                dataSetArray,
                privLabel,
                edp, 
                iconSelector, iconMap, 
                isFleet, fleetRoute, selID,
                tmz, dateFmt, timeFmt, 
                csvSep,
                minProximityM);
            // add key value
            jMapDataObj.addKeyValue(JSON_DataSets,dataSetArray);
        }

        /* Actions */
        // "Actions": [
        //    {
        //      "cmd": "showpp",
        //      "arg": "2"
        //    },
        //    {
        //      "cmd": "zoompp",
        //      "arg": "2"
        //    }
        // ]
        if (!ListTools.isEmpty(actions)) {
            // "command|Data"
            JSON._Array actionArray = new JSON._Array();
            for (String a : actions) {
                int p = a.indexOf("|");
                String command = (p >= 0)? a.substring(0,p) : a;
                String arg     = (p >= 0)? a.substring(p+1) : "";
                if (!StringTools.isBlank(command)) {
                    JSON._Object act = new JSON._Object();
                    act.addKeyValue(JSON_cmd, command);
                    act.addKeyValue(JSON_arg, arg);
                    actionArray.addValue(act);
                }
            }
            if (!actionArray.isEmpty()) {
                jMapDataObj.addKeyValue(JSON_Actions, actionArray);
            }
        }

        /* return JSON Object */
        return jMapDataObj;

    }

    /* write encoded map points-of-interest to the specified PrintWriter */
    private boolean _getMapPoi_json(
        JSON._Array dataSetArray,
        BasicPrivateLabel privLabel,
        PoiProvider poip[], 
        OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String> iconKeys, 
        char csvSep)
    {
        // {
        //    "type": "poi",
        //    "route": "false",
        //    "Points": [
        //      "POIDesc|||0|Latitude|Longitude|0.0|0.0|0.0|Address",
        //      ...
        //    ],
        // },

        /* valid EventDataProvider? */
        if (ListTools.isEmpty(poip)) {
            //Print.logInfo("No PointsOfInterest ...");
            return false;
        }

        /* header */
        JSON._Object poiObj = new JSON._Object();
        poiObj.addKeyValue(JSON_type    , DSTYPE_poi);
        poiObj.addKeyValue(JSON_route   , false);
        JSON._Array pointArray = new JSON._Array();
        poiObj.addKeyValue(JSON_Points  , pointArray);
        dataSetArray.addValue(poiObj);

        /* points of interest */
        for (int i = 0; i < poip.length; i++) {
            final PoiProvider pp = poip[i];
            EventDataProvider edp = new EventDataProviderAdapter() {
                public String getAccountID()         { return pp.getAccountID(); }
                public String getDeviceID()          { return pp.getPoiID(); }
                public String getDeviceDescription() { return pp.getPoiDescription(); }
                public double getLatitude()          { return pp.getLatitude(); }
                public double getLongitude()         { return pp.getLongitude(); }
                public String getAddress()           { return pp.getAddress(); }
                public int    getPushpinIconIndex(String iconSelector, OrderedMap<String,PushpinIcon> iconMap, boolean isFleet, BasicPrivateLabel bpl) { return pp.getPushpinIconIndex(iconMap,bpl); }
            };
            Print.logDebug("(JSON) Found POI: " + edp.getGeoPoint());
            String rcd = this.formatMapEvent(privLabel, edp, // POI JSON
                null/*iconSelector*/, iconMap, 
                false/*isFleet*/, 1/*stoppedState*/,
                null/*TimeZone*/, null/*dateFmt*/, null/*timeFmt*/, csvSep);
            pointArray.addValue(rcd);
        }

        /* add to DataSet */
        return true;

    }

    /* add map event dataset to the specified JSON array */
    private boolean _addDataSet_json(
        JSON._Array dataSetArray,
        BasicPrivateLabel privLabel,
        EventDataProvider edp[], 
        String iconSelector, OrderedMap<String,PushpinIcon> iconMap, // OrderedSet<String> iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID, // "selID" is either a DeviceID or GroupID
        TimeZone tmz, 
        String dateFmt, String timeFmt, 
        char csvSep,
        double minProximityM)
    {
        // {
        //    "type": "device",
        //    "id": "deviceid",
        //    "route": "true",
        //    "routeColor": "#FF0000",
        //    "textColor": "#FF0000",
        //    "Points": [
        //      "DeviceDesc|Data|Time|StatusCode|Latitude|Longitude|SpeedKPH|Heading|Altitude|Address",
        //      ...
        //    ],
        // }

        /* valid EventDataProvider? */
        if (ListTools.isEmpty(edp)) {
            return false;
        }

        /* use custom Device 'displayColor' for routeLine color? */
        boolean useRouteDisplayColor = (privLabel != null)?
            privLabel.getBooleanProperty(BasicPrivateLabel.PROP_TrackMap_useRouteDisplayColor, true) :
            true;

        /* print events (JSON) */
        JSON._Array pointArray = null;
        boolean  isDeviceData  = !isFleet;
        boolean  didStartSet   = false;
        GeoPoint lastGP        = null;
        String   lastDevID     = "";
        String   routeColor    = "";
        String   textColor     = "";
        int      evNdx         = 0;
        boolean  startStopOK   = false;
        int      stoppedState  = -1; // -1=uninitialized, 0=moving, 1=stopped, 2=stopEvent
        for (int i = 0; i < edp.length; i++) {
            EventData ev = (edp[i] instanceof EventData)? (EventData)edp[i] : null; // likely not-null
            String thisDevID = edp[i].getDeviceID();

            /* device changed? (JSON) */
            if (!thisDevID.equals(lastDevID)) {
                Device dev = (ev != null)? ev.getDevice() : null;
                if (isFleet /*&& fleetRoute*/) {
                    if (didStartSet) {
                        // -- close previous dataset
                        didStartSet = false;
                    }
                    isDeviceData = true;
                    selID        = thisDevID;
                }
                lastDevID    = thisDevID;
                lastGP       = null;
                textColor    = "";
                routeColor   = "";
                startStopOK  = ((dev != null) && dev.getStartStopSupported())? true : false;
                if ((edp[i] instanceof EventData) && (isFleet || useRouteDisplayColor)) {
                    if ((dev != null) && dev.hasDisplayColor()) {
                        if (isFleet) {
                            textColor = dev.getDisplayColor();
                        }
                        if (useRouteDisplayColor) {
                            routeColor = dev.getDisplayColor();
                        }
                    }
                }
            }

            /* event index */
            edp[i].setEventIndex(evNdx++); // TODO: 

            /* stopped/moving? (JSON) */
            if (ev == null) {
                // -- not an EventData record
                stoppedState = (edp[i].getSpeedKPH() <= 0.0)? 1/*stopped*/ : 0/*moving*/; 
            } else
            if (!startStopOK) {
                // -- does not support start/stop
                stoppedState = (edp[i].getSpeedKPH() <= 0.0)? 1/*stopped*/ : 0/*moving*/; 
                ev.setStopped(stoppedState == 1);
            } else {
                // -- init stoppedState
                if (stoppedState < 0) { 
                    // -- Regardless of the speed value, we really don't know if we should be
                    // -  in a stopped state or in-motion state.  If we arbitrarily decide we
                    // -  are stopped because of a currently zero-speed, then possibly all 
                    // -  following events will have a stopped pushpin, even though we should
                    // -  be moving.  For this reason, we try to guess conservatively, and
                    // -  commit to an "in-motion" detected state, but do not commit to a 
                    // -  possible "stopped" detected state.
                    stoppedState = (edp[i].getSpeedKPH() <= 0.0)? -1/*unknown*/ : 0/*moving*/; 
                }
                // -- check previous stoppedState
                if (stoppedState < 0) {
                    // -- still unknown [state -1]
                } else
                if (stoppedState > 0) {
                    // -- previously "stopped" [state 1 or 2]
                    if (ev == null) {
                        // -- not an EventData record, just check speed
                        stoppedState = (edp[i].getSpeedKPH() <= 0.0)? 1/*stopped*/ : 0/*moving*/;
                    } else
                    if (ev.isStartEvent(true/*defaultToSpeedCheck*/)) { 
                        // -- was "stopped", now "moving"
                        stoppedState = 0; // moving
                        ev.setStopped(false);
                    } else {
                        // -- still "stopped"
                        stoppedState = 1; // stopped
                        ev.setStopped(true);
                    }
                } else {
                    // -- previously moving [state 0]
                    if (ev == null) {
                        // -- not an EventData record, just check speed
                        stoppedState = (edp[i].getSpeedKPH() <= 0.0)? 1/*stopped*/ : 0/*moving*/;
                    } else
                    if (ev.isStopEvent(true/*defaultToSpeedCheck*/)) {
                        // -- was "moving", now "stopped"
                        stoppedState = 2; // stopEvent
                        ev.setStopped(true);
                    } else {
                        // -- still "moving"
                        stoppedState = 0; // moving
                        ev.setStopped(false);
                    }
                }
            }

            /* last event for device? */
            if ((i + 1) >= edp.length) {
                // -- last event in list
                //Print.logInfo("Setting last event ...");
                edp[i].setIsLastEvent(true);
            } else {
                String nextDevID = edp[i + 1].getDeviceID();
                if (!thisDevID.equals(nextDevID)) {
                    // -- DeviceID will change on next iteration
                    edp[i].setIsLastEvent(true);
                    evNdx = 0; // reset
                }
            }

            /* trim events in close proximity */
            if (minProximityM > 0.0) {
                // -- check proximity to established target location (range 20-200 metera)
                double lat = edp[i].getLatitude();
                double lon = edp[i].getLongitude();
                if (GeoPoint.isValid(lat,lon)) {
                    // -- guarantee that this point is valid
                    GeoPoint thisGP = new GeoPoint(lat,lon);
                    if (lastGP == null) {
                        // -- the first 'last' event, set target location and continue
                        lastGP = thisGP;
                    } else
                    if (thisGP.metersToPoint(lastGP) >= minProximityM) {
                        // -- outside tolerance zone, set new target location and continue
                        lastGP = thisGP;
                    } else {
                        // -- inside tolerance zone, skip this event
                        //Print.logError(i + ") Skipping this record!");
                        continue;
                    }
                }
            }

            /* start "DataSet" (if not already started) */
            if (!didStartSet) {
                String type = isDeviceData? DSTYPE_device : DSTYPE_group; // "poi"
                JSON._Object dataSetObj = new JSON._Object();
                dataSetObj.addKeyValue(JSON_type      , type);
                dataSetObj.addKeyValue(JSON_id        , selID);
                dataSetObj.addKeyValue(JSON_route     , isDeviceData);
                dataSetObj.addKeyValue(JSON_routeColor, routeColor);
                dataSetObj.addKeyValue(JSON_textColor , textColor);
                pointArray = new JSON._Array();
                dataSetObj.addKeyValue(JSON_Points    , pointArray);
                dataSetArray.addValue(dataSetObj);
                didStartSet = true;
            }

            /* fleet icon [JSON] */
            boolean showFleetIcon;
            if (!isFleet) {
                // -- not a 'fleet' map, do not show fleet icon
                showFleetIcon = false;
            } else {
                String sfi = privLabel.getStringProperty(BasicPrivateLabel.PROP_TrackMap_showFleetMapDevicePushpin,"");
                if (StringTools.isBlank(sfi) || sfi.equalsIgnoreCase("default")) {
                    if (!fleetRoute) {
                        // -- fleet map, single point, show fleet icon
                        showFleetIcon = true;
                    } else {
                        // -- fleet map, multiple points, show fleet icon if last event
                        showFleetIcon = edp[i].getIsLastEvent();
                    }
                } else {
                    // -- 'true' will display all device pushpins
                    // -  'false' will display the default pushpins
                    showFleetIcon = StringTools.parseBoolean(sfi,false);
                }
            }

            /* format and print event */
            String rcd = this.formatMapEvent(privLabel, edp[i], // Events JSON
                iconSelector, iconMap, 
                showFleetIcon, stoppedState,
                tmz, dateFmt, timeFmt, csvSep);
            pointArray.addValue(rcd);

        } // looping through events

        /* Dataset footer */
        if (didStartSet) {
            didStartSet = false;
        }

        return true;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // <?xml version=\"1.0\" encoding=\"UTF-8\"?>
    // <EventData account="account" timezone="US/Pacific">
    //    <Event device="device">
    //       <Timestamp epoch="1183397093">yyyy/MM/dd HH:mm:ss</Timestamp>
    //       <StatusCode code="0xF112">IN-MOTION</StatusCode>
    //       <Entity>entity</Entity>
    //       <GPSPoint age="5">35.12345/-135.12345</GPSPoint>
    //       <Speed units="kph">113.0</Speed>
    //       <Heading degrees="21.0">N</Heading>
    //       <Altitude units="meters">567</Altitude>
    //       <Odometer units="Km">123456.0</Odometer>
    //       <Sensor type="low">0xAA112233</Sensor>
    //       <Sensor type="high">0xAA112233</Sensor>
    //    </Event>
    // </EventData>
    // ---
    // <Account account="account" timezone="US/Pacific">
    //    <Description>Demo Account</Description>
    //    <Device id="device">
    //        <Description>Device Description</Description>
    //        <EventData device="device">
    //            <Timestamp epoch="1183397093">yyyy/MM/dd HH:mm:ss</Timestamp>
    //            <StatusCode code="0xF112">IN-MOTION</StatusCode>
    //            <Entity>entity</Entity>
    //            <GPSPoint age="5">35.12345/-135.12345</GPSPoint>
    //            <Speed units="kph">113.0</Speed>
    //            <Heading degrees="21.0">N</Heading>
    //            <Altitude units="meters">567</Altitude>
    //            <Odometer units="Km">123456.0</Odometer>
    //            <Sensor type="low">0xAA112233</Sensor>
    //            <Sensor type="high">0xAA112233</Sensor>
    //        </EventData>
    //    </Device>
    // </Account>
    
    public  static final String  TAG_Account                    = "Account";
    public  static final String  TAG_Device                     = "Device";
    public  static final String  TAG_EventData                  = "EventData";
    public  static final String  TAG_Description                = "Description";
    public  static final String  TAG_Event                      = "Event";
    public  static final String  TAG_Timestamp                  = "Timestamp";
    public  static final String  TAG_StatusCode                 = "StatusCode";
    public  static final String  TAG_GPSPoint                   = "GPSPoint";
    public  static final String  TAG_Speed                      = "Speed";
    public  static final String  TAG_Heading                    = "Heading";
    public  static final String  TAG_Altitude                   = "Altitude";
    public  static final String  TAG_Odometer                   = "Odometer";
    public  static final String  TAG_Geozone                    = "Geozone";
    public  static final String  TAG_Address                    = "Address";
    public  static final String  TAG_City                       = "City";
    public  static final String  TAG_PostalCode                 = "PostalCode";
    public  static final String  TAG_DigitalInputMask           = "DigitalInputMask";
    public  static final String  TAG_DriverID                   = "DriverID";
    public  static final String  TAG_DriverMessage              = "DriverMessage";
    public  static final String  TAG_EngineRPM                  = "EngineRPM";
    public  static final String  TAG_EngineHours                = "EngineHours";
    public  static final String  TAG_VehicleBatteryVolts        = "VehicleBatteryVolts";
    public  static final String  TAG_EngineCoolantLevel         = "EngineCoolantLevel";
    public  static final String  TAG_EngineCoolantTemperature   = "EngineCoolantTemperature";
    public  static final String  TAG_EngineFuelUsed             = "EngineFuelUsed";

    public  static final String  ATTR_account                   = "account";
  //public  static final String  ATTR_device                    = "device";
  //public  static final String  ATTR_timezone                  = "timezone";
    public  static final String  ATTR_epoch                     = "epoch";
    public  static final String  ATTR_age                       = "age";
    public  static final String  ATTR_units                     = "units";
    public  static final String  ATTR_limit                     = "limit";
    public  static final String  ATTR_index                     = "index";
    public  static final String  ATTR_code                      = "code";

    private void writeEvents_XML_Event(PrintWriter pwout, 
        Device dev, EventData ev,
        int indent, boolean allTags,
        BasicPrivateLabel privLabel, boolean oldFormat)
        throws IOException
    {
        boolean isSoapRequest = false;
        Account account  = ev.getAccount();
        Device  device   = ev.getDevice();
        Locale  locale   = privLabel.getLocale(); // should be "reqState.getLocale();"
        String  PFX1     = XMLTools.PREFIX(isSoapRequest, indent);
        String  PFX2     = XMLTools.PREFIX(isSoapRequest, indent + 1);
        String  eventTag = oldFormat? TAG_Event : TAG_EventData;

        /* Event tag start */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,eventTag,
            XMLTools.ATTR(ATTR_device,ev.getDeviceID()),
            false,true));

        // -- Timestamp
        long timestamp = ev.getTimestamp();
        if (allTags || (timestamp > 0L)) {
            TimeZone dispTmz = account.getTimeZone(null);
            DateTime ts = new DateTime(timestamp); // 'dispTmz' used below
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Timestamp,
                XMLTools.ATTR(ATTR_epoch,timestamp),
                false,false));
            this.write(pwout, ts.format("yyyy/MM/dd HH:mm:ss zzz",dispTmz));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Timestamp,true));
        }
        
        // -- StatusCode
        int statusCode = ev.getStatusCode();
        String code = "0x" + StringTools.toHexString(statusCode, 16);
        String desc = ev.getStatusCodeDescription(privLabel);
        this.write(pwout, PFX2);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_StatusCode,
            XMLTools.ATTR(ATTR_code,code),
            false,false));
        this.write(pwout, XMLTools.CDATA(isSoapRequest,desc));
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_StatusCode,true));

        // -- GPSPoint
        GeoPoint geoPoint = ev.getGeoPoint();
        if (allTags || geoPoint.isValid()) {
            long age = ev.getGpsAge();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_GPSPoint,
                ((allTags || (age > 0))?XMLTools.ATTR(ATTR_age,age):""),
                false,false));
            // satellite count?
            this.write(pwout, geoPoint.toString(','));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_GPSPoint,true));
        }
        
        // -- SpeedKPH
        double speedKPH = ev.getSpeedKPH();
        if (allTags || (speedKPH >= 0.0)) {
            Account.SpeedUnits speedUnits = Account.getSpeedUnits(account);
            double speed = speedUnits.convertFromKPH(speedKPH);
            String units = speedUnits.toString(locale);
            double speedLimKPH = ev.getSpeedLimitKPH();
            double limit = (speedLimKPH > 0.0)? speedUnits.convertFromKPH(speedLimKPH) : 0.0;
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Speed,
                XMLTools.ATTR(ATTR_units,units) +
                ((limit>0.0)?XMLTools.ATTR(ATTR_limit,StringTools.format(limit,"0.0")):""),
                false,false));
            this.write(pwout, StringTools.format(speed,"0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Speed,true));
        }
        
        // -- Heading (only if speed is > 0)
        if (allTags || (speedKPH > 0.0)) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Heading,
                XMLTools.ATTR("degrees",StringTools.format(ev.getHeading(),"0.0")),
                false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,GeoPoint.GetHeadingString(ev.getHeading(),locale)));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Heading,true));
        }

        // -- Altitude
        double altitudeM = ev.getAltitude();
        if (allTags || (altitudeM > 0.0)) {
            Account.AltitudeUnits altUnits = Account.getAltitudeUnits(account);
            int    alt   = (int)Math.round(altUnits.convertFromMeters(altitudeM));
            String units = altUnits.toString(locale);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Altitude,
                XMLTools.ATTR(ATTR_units,units),
                false,false));
            this.write(pwout, String.valueOf(alt));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Altitude,true));
        }

        // -- Odometer
        double odomKM = ev.getOdometerWithOffsetKM();  // + device.getOdometerOffsetKM(); // ok
        if (allTags || (odomKM > 0.0)) {
            Account.DistanceUnits distUnits = Account.getDistanceUnits(account);
            double odometer = distUnits.convertFromKM(odomKM);
            String units    = distUnits.toString(locale);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Odometer,
                XMLTools.ATTR(ATTR_units,units),
                false,false));
            this.write(pwout, StringTools.format(odometer,"#0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Odometer,true));
        }

        // -- Geozone
        String geozoneID = ev.getGeozoneID();
        long geozoneNdx  = ev.getGeozoneIndex();
        if (allTags || !geozoneID.equals("") || (geozoneNdx > 0L)) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Geozone,
                XMLTools.ATTR(ATTR_index,geozoneNdx),
                false,false));
            this.write(pwout, geozoneID);
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Geozone,true));
        }

        // -- Address
        String address = ev.getAddress();
        if (allTags || !address.equals("")) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Address,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,address));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Address,true));
        }

        // -- City
        String city = ev.getCity();
        if (allTags || !city.equals("")) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_City,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,city));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_City,true));
        }

        // -- PostalCode
        String postalCode = ev.getPostalCode();
        if (allTags || !postalCode.equals("")) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_PostalCode,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,postalCode));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_PostalCode,true));
        }

        // -- DigitalInputMask
        long inputMask = ev.getInputMask();
        if (allTags || (inputMask != 0L)) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DigitalInputMask,"",false,false));
            this.write(pwout, "0x"+StringTools.toHexString(inputMask));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DigitalInputMask,true));
        }

        // -- [allTags] DriverID
        if (allTags && EventData.getFactory().hasField(EventData.FLD_driverID)) {
            String driverID = ev.getDriverID();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DriverID,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,driverID));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DriverID,true));
        }

        // -- [allTags] DriverMessage
        if (allTags && EventData.getFactory().hasField(EventData.FLD_driverMessage)) {
            String driverMsg = ev.getDriverMessage();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DriverMessage,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,driverMsg));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DriverMessage,true));
        }

        // -- [allTags] EngineRPM
        if (allTags && EventData.getFactory().hasField(EventData.FLD_engineRpm)) {
            long engineRpm = ev.getEngineRpm();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_EngineRPM,"",false,false));
            this.write(pwout, String.valueOf(engineRpm));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_EngineRPM,true));
        }

        // -- [allTags] EngineHours
        if (allTags && EventData.getFactory().hasField(EventData.FLD_engineHours)) {
            double engineHours = ev.getEngineHours() + device.getEngineHoursOffset();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_EngineHours,"",false,false));
            this.write(pwout, StringTools.format(engineHours,"#0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_EngineHours,true));
        }

        // -- [allTags] VehicleBatteryVolts
        if (allTags && EventData.getFactory().hasField(EventData.FLD_vBatteryVolts)) {
            double battVolts = ev.getVBatteryVolts();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_VehicleBatteryVolts,"",false,false));
            this.write(pwout, StringTools.format(battVolts,"#0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_VehicleBatteryVolts,true));
        }

        // -- [allTags] EngineCoolantLevel
        if (allTags && EventData.getFactory().hasField(EventData.FLD_coolantLevel)) {
            double pct100 = ev.getCoolantLevel() * 100.0;
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_EngineCoolantLevel,
                XMLTools.ATTR(ATTR_units,"percent"),
                false,false));
            this.write(pwout, StringTools.format(pct100,"#0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_EngineCoolantLevel,true));
        }

        // -- [allTags] EngineCoolantTemperature
        if (allTags && EventData.getFactory().hasField(EventData.FLD_coolantTemp)) {
            Account.TemperatureUnits tempUnits = Account.getTemperatureUnits(account);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_EngineCoolantTemperature,
                XMLTools.ATTR(ATTR_units,tempUnits.toString()),
                false,false));
            double tempC = ev.getCoolantTemp();
            if (tempC > 0.0) {
                double temp = tempUnits.convertFromC(tempC);
                this.write(pwout, StringTools.format(temp,"#0.0"));
            }
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_EngineCoolantTemperature,true));
        }

        // -- [allTags] EngineFuelUsed
        if (allTags && EventData.getFactory().hasField(EventData.FLD_fuelTotal)) {
            Account.VolumeUnits volUnits = Account.getVolumeUnits(account);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_EngineFuelUsed,
                XMLTools.ATTR(ATTR_units,volUnits.toString()),
                false,false));
            double fuelL = ev.getFuelTotal();
            if (fuelL > 0.0) {
                double fuel = volUnits.convertFromLiters(fuelL);
                this.write(pwout, StringTools.format(fuel,"#0.0"));
            }
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_EngineFuelUsed,true));
        }

        /* Event tag end */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.endTAG(isSoapRequest,eventTag,true));

    }

    private void writeEvents_XML_TopTag(PrintWriter pwout, 
        Account account, String tz, 
        boolean startTag, boolean oldFormat)
        throws IOException
    {
        boolean isSoapRequest = false;
        String topTag = oldFormat? TAG_EventData : TAG_Account;
        if (startTag) {
            this.write(pwout, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            String accountID = (account != null)? account.getAccountID() : null;
            this.write(pwout, XMLTools.startTAG(isSoapRequest,topTag,
                XMLTools.ATTR(ATTR_account,accountID) + 
                XMLTools.ATTR(ATTR_timezone,tz),
                false/*endTag*/,true/*newLine*/));
            if (account != null) {
                this.write(pwout, XMLTools.PREFIX(isSoapRequest, 1));
                this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Description,
                    null,
                    false/*endTag*/,false/*newLine*/));
                this.write(pwout, XMLTools.CDATA(isSoapRequest,account.getDescription()));
                this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Description,true));
            }
        } else {
            this.write(pwout, XMLTools.endTAG(isSoapRequest,topTag,true));
        }
    }

    private boolean writeEvents_XML(PrintWriter pwout, 
        Account account, Collection<Device> devList, 
        boolean allTags, TimeZone dispTmz,
        BasicPrivateLabel privLabel, boolean oldFormat)
        throws IOException
    {
        // -- This does assume that all events belong to the same "Account"
        boolean isSoapRequest = false;

        /* account required */
        if (account == null) {
            return false;
        }
        String accountID = account.getAccountID();
        String tzStr = account.getTimeZone();
        if (StringTools.isBlank(tzStr)) {
            tzStr = DateTime.GMT_TIMEZONE;
        }

        /* header */
        this.writeEvents_XML_TopTag(pwout, account, tzStr, true/*startTag*/, oldFormat);
        String PFX1 = XMLTools.PREFIX(isSoapRequest, 1);
        String PFX2 = XMLTools.PREFIX(isSoapRequest, 2);

        /* list device event data */
        if (!ListTools.isEmpty(devList)) {
            for (Device dev : devList) {
                String deviceID = dev.getDeviceID();

                /* check account ID */
                if (!dev.getAccountID().equals(accountID)) {
                    // -- mismatched AccountID
                    continue;
                }

                /* device events */
                EventData evList[] = dev.getSavedRangeEvents();
                if (ListTools.isEmpty(evList)) {
                    // -- no events for this device
                    continue;
                }

                /* Device start tag */
                if (!oldFormat) {
                    this.write(pwout, PFX1);
                    this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Device,
                        XMLTools.ATTR("id",deviceID),
                        false/*endTag*/,true/*newLine*/));
                    // <Description><![CDATA[Description]]></Description>
                    this.write(pwout, PFX2);
                    this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Description,
                        null,
                        false/*endTag*/,false/*newLine*/));
                    this.write(pwout, XMLTools.CDATA(isSoapRequest,dev.getDescription()));
                    this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Description,true));
                }

                /* Device events */
                for (int i = 0; i < evList.length; i++) {
                    EventData ev = evList[i];
    
                    /* same account? */
                    if (!ev.getAccountID().equals(accountID)) {
                        // -- mismatched AccountID
                        continue;
                    }
                    ev.setAccount(account); // redundant
    
                    /* write Event */
                    this.writeEvents_XML_Event(pwout, 
                        dev, ev,
                        2, allTags,
                        privLabel, oldFormat);
    
                }

                /* Device end tag */
                if (!oldFormat) {
                    this.write(pwout, PFX1);
                    this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Device,true));
                }
                
            }

        }

        /* trailer */
        this.writeEvents_XML_TopTag(pwout, null, null, false/*startTag*/, oldFormat);
        this.flush(pwout); // flush (output may not occur this the PrintWriter is flushed)
        return true;

    }

    // ------------------------------------------------------------------------
    // <?xml version="1.0" encoding="UTF-8"?>
    // <gpx version="1.0"
    //      creator="OpenGTS - http://www.opengts.org"
    //      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    //      xmlns="http://www.topografix.com/GPX/1/0"
    //      xsi:schemaLocation="http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd">
    //    <time>2009-05-30T12:48:43Z</time>
    //    <wpt lat="39.4431641" lon="-142.7295456">
    //      <name>Device</name>
    //      <ele>1234.5</ele>
    //    </wpt>
    //    <trk>
    //      <name>Device</name>
    //      <src>GPS Tracking Device</src>
    //      <trkseg>
    //        <trkpt lat="39.4431641" lon="-142.7295456">
    //          <time>2009-05-30T12:48:43Z</time>
    //          <ele>1234.5</ele>
    //        </trkpt>
    //        <trkpt lat="39.4431641" lon="-142.7295456">
    //          <time>2009-05-30T12:48:43Z</time>
    //          <ele>1234.5</ele>
    //        </trkpt>
    //      </trkseg>
    //    </trk>
    // </gpx>

    private boolean writeEvents_GPX(PrintWriter pwout, 
        Account account, Collection<Device> devList, 
        BasicPrivateLabel privLabel)
        throws IOException
    {
        String dateFmt = "yyyy-MM-dd'T'HH:mm:ss'Z'";

        /* account required */
        if (account == null) {
            return false;
        }
        String accountID = account.getAccountID();
        TimeZone tz = DateTime.getGMTTimeZone();

        /* header */
        this.write(pwout, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        this.write(pwout, "<gpx version=\"1.0\"\n");
        this.write(pwout, "    creator=\"OpenGTS "+Version.getVersion()+" - http://www.opengts.org\"\n");
        this.write(pwout, "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        this.write(pwout, "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
        this.write(pwout, "  <time>" + (new DateTime(tz)).format(dateFmt) + "</time>\n");

        /* track body */
        if (!ListTools.isEmpty(devList)) {
            for (Device dev : devList) {
                String deviceID = dev.getDeviceID();
    
                /* check account ID */
                if (!dev.getAccountID().equals(accountID)) {
                    // mismatched AccountID
                    continue;
                }

                /* Device start tag */
                this.write(pwout, "  <trk>\n");
                this.write(pwout, "  <name><![CDATA["+deviceID+"]]></name>\n");
                this.write(pwout, "  <desc><![CDATA["+dev.getDescription()+"]]></desc>\n");
                this.write(pwout, "  <trkseg>\n");

                /* events */
                EventData evList[] = dev.getSavedRangeEvents();
                if (!ListTools.isEmpty(evList)) {
                    for (EventData ev : evList) {
                        this.write(pwout, "    <trkpt lat=\"" + ev.getLatitude() + "\" lon=\"" + ev.getLongitude() + "\">\n");
                        this.write(pwout, "      <time>" + (new DateTime(ev.getTimestamp(),tz)).format(dateFmt) + "</time>\n");
                        this.write(pwout, "      <ele>"+ev.getAltitude()+"</ele>\n"); // meters
                        this.write(pwout, "    </trkpt>\n");
                    }
                }
        
                /* Device end tag */
                this.write(pwout, "  </trkseg>\n");
                this.write(pwout, "  </trk>\n");
                
            }
            
        }

        /* footer */
        this.write(pwout, "</gpx>\n");

        return true;
    }

    // ------------------------------------------------------------------------
    // {
    //    "Account": "demo",
    //    "Account_desc": "Demo Account",
    //    "TimeZone": "US/Pacific",
    //    "DeviceList": [
    //       {
    //          "Device": "demo",
    //          "Device_desc": "New Device [demo]",
    //          "EventData": [
    //             {
    //                "Device": "demo",
    //                "Timestamp": 1268430461,
    //                "Timestamp_date": "2010/03/12",
    //                "Timestamp_time": "13:47:41",
    //                "StatusCode": 12345,
    //                "StatusCode_hex": "0xF112",
    //                "StatusCode_desc": "InMotion",
    //                "GPSPoint": "37.78340,-122.40246",
    //                "GPSPoint_lat": 37.78340,
    //                "GPSPoint_lon": -122.40246,
    //                "GPSPoint_age" : 37,
    //                "GPSPoint_accuracy" : 54,
    //                "Speed_kph": 0.0,
    //                "Speed": 0.0,
    //                "Speed_units": "mph",
    //                "Heading": 0.0,
    //                "Heading_desc": "N",
    //                "Altitude_meters": 16,
    //                "Altitude": 16,
    //                "Altitude_units": "meters",
    //                "Odometer_km": 711.3,
    //                "Odometer": 711.3,
    //                "Odometer_units": "Km",
    //                "Address": "789 Howard St, San Francisco, CA 94103",
    //                "City": "San Francisco",
    //                "PostalCode": "94103",
    //                "Index": 0
    //             },
    //          ]
    //       }
    //    ]
    // }

    private static String JSON_INDENT = "   ";
    
    public boolean writeEvents_JSON(PrintWriter pwout, 
        Account account, Collection<Device> devList, 
        boolean allTags, TimeZone dispTmz,
        BasicPrivateLabel privLabel)
        throws IOException
    {

        /* account required */
        if (account == null) {
            return false;
        }
        String accountID = account.getAccountID();
        String acctDesc  = account.getDescription();
        String tzStr     = account.getTimeZone();
        if (StringTools.isBlank(tzStr)) {
            tzStr = DateTime.GMT_TIMEZONE;
        }

        /* header */
        String PFX0 = "";
        String PFX1 = StringTools.replicateString(JSON_INDENT,1);
        String PFX2 = StringTools.replicateString(JSON_INDENT,2);
        String PFX3 = StringTools.replicateString(JSON_INDENT,3);
        this.writeKeyValue_JSON(pwout,PFX0,"{"           , null, true);
        this.writeKeyValue_JSON(pwout,PFX1,"Account"     , accountID, false);
        this.writeKeyValue_JSON(pwout,PFX1,"Account_desc", acctDesc, false);
        this.writeKeyValue_JSON(pwout,PFX1,"TimeZone"    , tzStr, false);
        this.writeKeyValue_JSON(pwout,PFX1,"DeviceList"  , "[", true);

        if (!ListTools.isEmpty(devList)) {
            int d = 0, lastDevNdx = devList.size() - 1;
            for (Device dev : devList) {
                boolean isLastDev = (d++ == lastDevNdx);

                /* check account ID */
                if (!dev.getAccountID().equals(accountID)) {
                    // -- mismatched AccountID
                    continue;
                }
                String deviceID   = dev.getDeviceID();
                String deviceDesc = dev.getDescription();

                /* Device header */
                this.writeKeyValue_JSON(pwout,PFX2,"{", null, true);
                this.writeKeyValue_JSON(pwout,PFX3,"Device", deviceID, false);

                /* event data */
                EventData evList[] = dev.getSavedRangeEvents();
                if (evList != null) {
                    this.writeKeyValue_JSON(pwout,PFX3,"Device_desc", deviceDesc, false);
                    this.writeKeyValue_JSON(pwout,PFX3,"EventData", "[", true);
                    int lastEventNdx = evList.length - 1;
                    for (int e = 0; e <= lastEventNdx; e++) {
                        EventData ev = evList[e];
                        boolean isLastEvent = (e == lastEventNdx);
        
                        /* same account? */
                        if (!ev.getAccountID().equals(accountID)) {
                            // mismatched AccountID
                            continue;
                        }
                        ev.setAccount(account); // redundant
    
                        /* event */
                        this.writeEvents_JSON_Event(pwout, e, ev, isLastEvent, 4, allTags, privLabel);
    
                    }
                    this.writeKeyValue_JSON(pwout,PFX3,"]", null, true);
                } else {
                    this.writeKeyValue_JSON(pwout,PFX3,"Device_desc", deviceDesc, true);
                }

                /* Device footer */
                this.writeKeyValue_JSON(pwout,PFX2,"}", null, isLastDev);
                
            }

        }

        /* trailer */
        this.writeKeyValue_JSON(pwout,PFX1,"]", null, true);
        this.writeKeyValue_JSON(pwout,PFX0,"}", null, true);
        this.flush(pwout); // flush (output may not occur this the PrintWriter is flushed)
        return true;
        
    }

    private void writeEvents_JSON_Event(PrintWriter pwout, int index, EventData ev, boolean isLast, 
        int indent, boolean allTags, BasicPrivateLabel privLabel)
        throws IOException
    {
        boolean isSoapRequest = false;
        Account account = ev.getAccount();
        Device  device  = ev.getDevice();
        Locale  locale  = privLabel.getLocale(); // should be "reqState.getLocale();"
        String  PFX1    = StringTools.replicateString(JSON_INDENT,indent);
        String  PFX2    = StringTools.replicateString(JSON_INDENT,indent + 1);

        /* Event tag start */
        this.writeKeyValue_JSON(pwout,PFX1,"{",null,true);

        // -- Device
        this.writeKeyValue_JSON(pwout,PFX2,"Device", ev.getDeviceID(), false);

        // -- Timestamp
        long timestamp = ev.getTimestamp();
        if (allTags || (timestamp > 0L)) {
            TimeZone tz = account.getTimeZone(null);
            DateTime ts = new DateTime(timestamp); // 'tz' used below
            this.writeKeyValue_JSON(pwout,PFX2,"Timestamp"     , timestamp                 , false);
            this.writeKeyValue_JSON(pwout,PFX2,"Timestamp_date", ts.format("yyyy/MM/dd",tz), false);
            this.writeKeyValue_JSON(pwout,PFX2,"Timestamp_time", ts.format("HH:mm:ss",tz)  , false);
            //this.writeKeyValue_JSON(pwout,PFX2,"Timestamp_desc", ts.format("yyyy/MM/dd HH:mm:ss zzz",tz), false);
        }

        // -- StatusCode
        int statusCode = ev.getStatusCode();
        String hexCode = "0x" + StringTools.toHexString(statusCode, 16);
        String desc    = ev.getStatusCodeDescription(privLabel);
        this.writeKeyValue_JSON(pwout,PFX2,"StatusCode"     , statusCode, false);
        this.writeKeyValue_JSON(pwout,PFX2,"StatusCode_hex" , hexCode   , false);
        this.writeKeyValue_JSON(pwout,PFX2,"StatusCode_desc", desc      , false);

        // -- GPSPoint
        GeoPoint geoPoint = ev.getBestGeoPoint();
        if (allTags || geoPoint.isValid()) {
            long gpsAge = ev.getGpsAge();
            int accuracy = (int)Math.round(ev.getBestAccuracy());
            this.writeKeyValue_JSON( pwout,PFX2,"GPSPoint"    , geoPoint.toString(',')                , false);
            this._writeKeyValue_JSON(pwout,PFX2,"GPSPoint_lat", geoPoint.getLatitudeString( null,null), false);
            this._writeKeyValue_JSON(pwout,PFX2,"GPSPoint_lon", geoPoint.getLongitudeString(null,null), false);
            if (gpsAge > 0) {
                this.writeKeyValue_JSON(pwout,PFX2,"GPSPoint_age", gpsAge, false);
            }
            if (accuracy > 0) {
                this.writeKeyValue_JSON(pwout,PFX2,"GPSPoint_accuracy", accuracy, false);
            }
        }
        
        // -- SpeedKPH
        double speedKPH = ev.getSpeedKPH();
        if (allTags || (speedKPH >= 0.0)) {
            Account.SpeedUnits speedUnits = Account.getSpeedUnits(account);
            double speed = speedUnits.convertFromKPH(speedKPH);
            String units = speedUnits.toString(locale);
            double speedLimKPH = ev.getSpeedLimitKPH();
            double limit = (speedLimKPH > 0.0)? speedUnits.convertFromKPH(speedLimKPH) : 0.0;
            this._writeKeyValue_JSON(pwout,PFX2,"Speed_kph"  , StringTools.format(speedKPH,"0.0"), false);
            this._writeKeyValue_JSON(pwout,PFX2,"Speed"      , StringTools.format(speed,"0.0"), false);
            this.writeKeyValue_JSON( pwout,PFX2,"Speed_units", units, false);
            if (limit > 0.0) { 
                this._writeKeyValue_JSON(pwout,PFX2,"Speed_limit", StringTools.format(limit,"0.0"), false); 
            }
        }

        // -- Heading (only if speed is > 0)
        if (allTags || (speedKPH > 0.0)) {
            double heading     = ev.getHeading();
            String headingDesc = GeoPoint.GetHeadingString(heading,locale);
            this._writeKeyValue_JSON(pwout,PFX2,"Heading"     , StringTools.format(heading,"0.0"), false);
            this.writeKeyValue_JSON( pwout,PFX2,"Heading_desc", headingDesc, false);
        }

        // -- Altitude
        double altitudeM = ev.getAltitude();
        if (allTags || (altitudeM > 0.0)) {
            Account.AltitudeUnits altUnits = Account.getAltitudeUnits(account);
            int    alt   = (int)Math.round(altUnits.convertFromMeters(altitudeM));
            String units = altUnits.toString(locale);
            this._writeKeyValue_JSON(pwout,PFX2,"Altitude_meters", StringTools.format(altitudeM,"0.0"), false);
            this.writeKeyValue_JSON( pwout,PFX2,"Altitude"       , alt      , false);
            this.writeKeyValue_JSON( pwout,PFX2,"Altitude_units" , units    , false);
        }

        // -- Odometer
        double odomKM = ev.getOdometerWithOffsetKM(); // + device.getOdometerOffsetKM(); // ok
        if (allTags || (odomKM > 0.0)) {
            Account.DistanceUnits distUnits = Account.getDistanceUnits(account);
            double odometer = distUnits.convertFromKM(odomKM);
            String units    = distUnits.toString(locale);
            this._writeKeyValue_JSON(pwout,PFX2,"Odometer_km"   , StringTools.format(odomKM,"0.000"), false);
            this._writeKeyValue_JSON(pwout,PFX2,"Odometer"      , StringTools.format(odometer,"0.000"), false);
            this.writeKeyValue_JSON( pwout,PFX2,"Odometer_units", units   , false);
        }

        // -- Geozone
        String geozoneID = ev.getGeozoneID();
        long geozoneNdx  = ev.getGeozoneIndex();
        if (allTags || !geozoneID.equals("") || (geozoneNdx > 0L)) {
            this.writeKeyValue_JSON(pwout,PFX2,"Geozone"      , geozoneID , false);
            this.writeKeyValue_JSON(pwout,PFX2,"Geozone_index", geozoneNdx, false);
        }

        // -- Address
        String address = ev.getAddress();
        if (allTags || !address.equals("")) {
            String addrStr = StringTools.replace(address,"\"","'");
            this.writeKeyValue_JSON(pwout,PFX2,"Address", addrStr, false);
        }

        // -- City
        String city = ev.getCity();
        if (allTags || !city.equals("")) {
            String cityStr = StringTools.replace(city,"\"","'");
            this.writeKeyValue_JSON(pwout,PFX2,"City", cityStr, false);
        }

        // -- PostalCode
        String postalCode = ev.getPostalCode();
        if (allTags || !postalCode.equals("")) {
            this.writeKeyValue_JSON(pwout,PFX2,"PostalCode", postalCode, false);
        }

        // -- DigitalInputMask
        long inputMask = ev.getInputMask();
        if (allTags || (inputMask != 0L)) {
            String hexInpStr =  "0x" + StringTools.toHexString(inputMask,0);
            this.writeKeyValue_JSON(pwout,PFX2,"DigitalInputMask"    , inputMask, false);
            this.writeKeyValue_JSON(pwout,PFX2,"DigitalInputMask_hex", hexInpStr, false);
        }

        // -- [allTags] DriverID
        if (allTags && EventData.getFactory().hasField(EventData.FLD_driverID)) {
            String driverID = ev.getDriverID();
            this.writeKeyValue_JSON(pwout,PFX2,"DriverID", driverID, false);
        }

        // -- [allTags] DriverMessage
        if (allTags && EventData.getFactory().hasField(EventData.FLD_driverMessage)) {
            String driverMsg = StringTools.replace(ev.getDriverMessage(),"\"","'");
            this.writeKeyValue_JSON(pwout,PFX2,"DriverMessage", driverMsg, false);
        }

        // -- [allTags] EngineRPM
        if (allTags && EventData.getFactory().hasField(EventData.FLD_engineRpm)) {
            long engineRpm = ev.getEngineRpm();
            this.writeKeyValue_JSON(pwout,PFX2,"EngineRPM", engineRpm, false);
        }

        // -- [allTags] EngineHours
        if (allTags && EventData.getFactory().hasField(EventData.FLD_engineHours)) {
            double engineHours = ev.getEngineHours() + device.getEngineHoursOffset();
            this._writeKeyValue_JSON(pwout,PFX2,"EngineHours", StringTools.format(engineHours,"0.00"), false);
        }

        // -- [allTags] VehicleBatteryVolts
        if (allTags && EventData.getFactory().hasField(EventData.FLD_vBatteryVolts)) {
            double battVolts = ev.getVBatteryVolts();
            this._writeKeyValue_JSON(pwout,PFX2,"VehicleBatteryVolts", StringTools.format(battVolts,"0.0"), false);
        }

        // -- [allTags] EngineCoolantLevel
        if (allTags && EventData.getFactory().hasField(EventData.FLD_coolantLevel)) {
            double pct100 = ev.getCoolantLevel() * 100.0;
            String units  = "percent";
            this._writeKeyValue_JSON(pwout,PFX2,"EngineCoolantLevel"      , StringTools.format(pct100,"0.0"), false);
            this.writeKeyValue_JSON( pwout,PFX2,"EngineCoolantLevel_units", units, false);
        }

        // -- [allTags] EngineCoolantTemperature
        if (allTags && EventData.getFactory().hasField(EventData.FLD_coolantTemp)) {
            Account.TemperatureUnits tempUnits = Account.getTemperatureUnits(account);
            double tempC = ev.getCoolantTemp();
            double temp  = tempUnits.convertFromC(tempC);
            String units = tempUnits.toString(locale);
            this._writeKeyValue_JSON(pwout,PFX2,"EngineCoolantTemperature_C"    , StringTools.format(tempC,"0.0"), false);
            this._writeKeyValue_JSON(pwout,PFX2,"EngineCoolantTemperature"      , StringTools.format(temp,"0.0"), false);
            this.writeKeyValue_JSON( pwout,PFX2,"EngineCoolantTemperature_units", units, false);
        }

        // -- [allTags] EngineFuelUsed
        if (allTags && EventData.getFactory().hasField(EventData.FLD_fuelTotal)) {
            Account.VolumeUnits volUnits = Account.getVolumeUnits(account);
            double fuelL = ev.getFuelTotal();
            double fuel  = volUnits.convertFromLiters(fuelL);
            String units = volUnits.toString(locale);
            this._writeKeyValue_JSON(pwout,PFX2,"EngineFuelUsed_Liter", StringTools.format(fuelL,"0.0"), false);
            this._writeKeyValue_JSON(pwout,PFX2,"EngineFuelUsed"      , StringTools.format(fuel,"0.0"), false);
            this.writeKeyValue_JSON( pwout,PFX2,"EngineFuelUsed_units", units, false);
        }

        /* index */
        this.writeKeyValue_JSON(pwout,PFX2,"Index", index, true);

        /* Event tag end */
        this.writeKeyValue_JSON(pwout,PFX1,"}",null,isLast);

    }

    private void writeKeyValue_JSON(PrintWriter pwout, String pfx, String key, int value, boolean isLast)
        throws IOException
    {
        String val = String.valueOf(value);
        this._writeKeyValue_JSON(pwout, pfx, key, val, isLast);
    }

    private void writeKeyValue_JSON(PrintWriter pwout, String pfx, String key, long value, boolean isLast)
        throws IOException
    {
        String val = String.valueOf(value);
        this._writeKeyValue_JSON(pwout, pfx, key, val, isLast);
    }

    private void writeKeyValue_JSON(PrintWriter pwout, String pfx, String key, double value, boolean isLast)
        throws IOException
    {
        String val = String.valueOf(value);
        this._writeKeyValue_JSON(pwout, pfx, key, val, isLast);
    }

    private void writeKeyValue_JSON(PrintWriter pwout, String pfx, String key, String value, boolean isLast)
        throws IOException
    {
        String val;
        if (StringTools.isBlank(value)) {
            val = "\"\"";
        } else 
        if (value.equals("[")) {
            val = "[";
        } else {
            val = "\"" + StringTools.escapeJSON(value) + "\"";
        }
        this._writeKeyValue_JSON(pwout, pfx, key, val, isLast);
    }

    private void _writeKeyValue_JSON(PrintWriter pwout, String pfx, String key, String value, boolean isLast)
        throws IOException
    {
        if ("{".equals(key)) {
            // -- start of object (ignore value, isLast)
            this.write(pwout, pfx + "{\n");
        } else
        if ("}".equals(key)) {
            // -- end of object (ignore value)
            if (isLast) {
                this.write(pwout, pfx + "}\n");
            } else {
                this.write(pwout, pfx + "},\n");
            }
        } else
        if ("[".equals(value)) {
            // -- start of array (ignore value, isLast)
            this.write(pwout, pfx + "\"" + key + "\": [\n");
        } else
        if ("]".equals(key)) {
            // -- end of array (ignore value, isLast)
            this.write(pwout, pfx + "]\n");
        } else {
            // -- property definition
            this.write(pwout, pfx + "\"" + key + "\": " + value);
            if (isLast) {
                this.write(pwout, "\n");
            } else {
                this.write(pwout, ",\n");
            }
        }
    }

    // ------------------------------------------------------------------------

    private boolean writeEvents_BML(PrintWriter pwout, 
        Account account, Collection<Device> devList, 
        BasicPrivateLabel privLabel)
        throws IOException
    {

        /* account required */
        if (account == null) {
            return false;
        }
        String accountID = account.getAccountID();

        /* header */
        pwout.write("<lbs>\n");

        /* devices */
        if (!ListTools.isEmpty(devList)) {
            for (Device dev : devList) {
                String deviceID = dev.getDeviceID();
    
                /* check account ID */
                if (!dev.getAccountID().equals(accountID)) {
                    // mismatched AccountID
                    continue;
                }

                /* events */
                EventData evList[] = dev.getSavedRangeEvents();
                if (!ListTools.isEmpty(evList)) {
                    for (EventData ev : evList) {
        
                        /* same account? */
                        if (!ev.getAccountID().equals(accountID)) {
                            // mismatched AccountID
                            continue;
                        }
                        ev.setAccount(account); // redundant

                        /* event */
                        pwout.write("<location lon=\""+ev.getLongitude()+"\" lat=\""+ev.getLatitude()+"\"");
                        pwout.write(" label=\""+ev.getDeviceID()+"\"");
                        pwout.write(" description=\""+ev.getAddress()+"\"");
                        // other options available as well
                        pwout.write("/>\n");
                        
                    }
                }
                
            }
        }

        /* footer */
        pwout.write("</lbs>\n");

        /* return success */
        return true;

    }

    // ------------------------------------------------------------------------

    // not fully supported
    private boolean writeEvents_AEMP(PrintWriter pwout, 
        Account account, Collection<Device> devList, 
        BasicPrivateLabel privLabel)
        throws IOException
    {
        // not fully supported

        /* account required */
        if (account == null) {
            return false;
        }
        String accountID = account.getAccountID();

        /* header */

        /* devices */
        if (!ListTools.isEmpty(devList)) {
            //
        }

        /* footer */

        /* not yet supported */
        return false;
        
    }

    // ------------------------------------------------------------------------

    public boolean writeEvents(OutputStream out, 
        Account account, Collection<Device> devList,
        int formatEnum, boolean allTags, TimeZone tmz,
        BasicPrivateLabel privLabel)
        throws IOException
    {
        PrintWriter pwout = (out != null)? new PrintWriter(out) : null;
        return this.writeEvents(pwout, 
            account, devList,
            formatEnum, allTags, tmz,
            privLabel);
    }
    
    public boolean writeEvents(PrintWriter pwout, 
        Account account, Collection<Device> devList,
        int formatEnum, boolean allTags, TimeZone dispTmz, 
        BasicPrivateLabel privLabel)
        throws IOException
    {
        if (devList != null) {
            switch (formatEnum) {
                case EventUtil.FORMAT_TXT:
                case EventUtil.FORMAT_CSV:
                    return this.writeEvents_CSV(pwout, 
                        account, devList, 
                        allTags, dispTmz, 
                        ',', true/*inclHeader*/, privLabel);
                case EventUtil.FORMAT_KML:
                    return GoogleKML.getInstance().writeEvents(pwout, 
                        account, devList, 
                        privLabel);
                case EventUtil.FORMAT_XML:
                case EventUtil.FORMAT_XMLOLD:
                    return this.writeEvents_XML(pwout, 
                        account, devList, 
                        allTags, dispTmz,
                        privLabel, (formatEnum == EventUtil.FORMAT_XMLOLD));
                case EventUtil.FORMAT_GPX:
                    return this.writeEvents_GPX(pwout, 
                        account, devList, 
                        privLabel);
                case EventUtil.FORMAT_JSON:
                case EventUtil.FORMAT_JSONX:
                    return this.writeEvents_JSON(pwout, 
                        account, devList, 
                        allTags, dispTmz,
                        privLabel);
                case EventUtil.FORMAT_BML:
                    return this.writeEvents_BML(pwout, 
                        account, devList, 
                        privLabel);
                case EventUtil.FORMAT_AEMP:
                    return this.writeEvents_AEMP(pwout, 
                        account, devList, 
                        privLabel);
                default:
                    Print.logError("Unrecognized data format: " + formatEnum);
                    return false;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    public static OutputStream openFileOutputStream(String outFile)
    {
        try {
            if (StringTools.isBlank(outFile) || outFile.equalsIgnoreCase("stdout")) {
                return System.out;
            } else
            if (outFile.equalsIgnoreCase("stderr")) {
                return System.err;
            } else {
                return new FileOutputStream(outFile, false/*no-append*/);
            }
        } catch (IOException ioe) {
            Print.logException("Unable to open output file: " + outFile, ioe);
            return null;
        }
    }
    
    public static void closeOutputStream(OutputStream out)
    {
        if ((out != null) && (out != System.out) && (out != System.err)) {
            try { out.close(); } catch (Throwable t) {/*ignore*/}
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse date range (format: "YYYY/MM/DD,YYYY/MM/DD[,LIMIT]" */
    private static long[] parseArgDateRange(String range, TimeZone tz)
    {
        String rangeFlds[] = StringTools.parseStringArray(range, "|,");
        
        /* Start time */
        long startTime = -1L;            
        if (rangeFlds.length >= 1) {
            if (rangeFlds[0].indexOf("/") >= 0) {
                try {
                    DateTime startDT = DateTime.parseArgumentDate(rangeFlds[0], tz, false);
                    Print.logInfo("Start Date: " + startDT);
                    startTime = (startDT != null)? startDT.getTimeSec() : -1L;
                } catch (DateTime.DateParseException dtpe) {
                    Print.logError("Invalid Start Date: " + rangeFlds[0] + " [" + dtpe.getMessage() + "]");
                    startTime = -1L;
                }
            } else {
                startTime = StringTools.parseLong(rangeFlds[0], -1L);
            }
        }
        
        /* End time */
        long endTime = -1L;
        if (rangeFlds.length >= 2) {
            if (rangeFlds[1].indexOf("/") >= 0) {
                try {
                    DateTime endDT = DateTime.parseArgumentDate(rangeFlds[1], tz, true);
                    Print.logInfo("End Date: " + endDT);
                    endTime = (endDT != null)? endDT.getTimeSec() : -1L;
                } catch (DateTime.DateParseException dtpe) {
                    Print.logError("Invalid End Date: " + rangeFlds[1] + " [" + dtpe.getMessage() + "]");
                    endTime = -1L;
                }
            } else {
                endTime = StringTools.parseLong(rangeFlds[1], -1L);
            }
        }
        
        /* limit */
        long limit = -1L;
        if (rangeFlds.length >= 3) {
            limit = StringTools.parseLong(rangeFlds[2], -1L);
        }
        
        /* return start/end times */
        if ((startTime <= 0L) && (endTime <= 0L)) {
            return null;
        } else {
            return new long[] { startTime, endTime, limit };
        }
        
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]   = new String[] { "acct"    , "account" };
    private static final String ARG_DEVICE[]    = new String[] { "dev"     , "device"  };
    private static final String ARG_EVENTS[]    = new String[] { "events"              };
    private static final String ARG_OUTPUT[]    = new String[] { "out"     , "output"  };
    private static final String ARG_FORMAT[]    = new String[] { "fmt"     , "format"  };
    private static final String ARG_GEOZONE[]   = new String[] { "geozone"             };
    private static final String ARG_GEOCODE[]   = new String[] { "rg"      , "geocode" };
    private static final String ARG_UPDATE[]    = new String[] { "update"  , "upd"     };
    private static final String ARG_ODOMETER[]  = new String[] { "odometer", "odom"    };

    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + EventUtil.class.getName() + " {options}");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -account=<id>                         Acount ID which owns Device");
        Print.sysPrintln("  -device=<id>                          Device ID to create/edit");
        Print.sysPrintln("  -events=<count>                       Write last <count> events to output file");
        Print.sysPrintln("  -events=<from>,<to>[,<limit>]         Write events in specified range to output file");
        Print.sysPrintln("  -format=[csv|kml]                     Event output format");
        Print.sysPrintln("  -output=<file>                        Event output file");
        Print.sysPrintln("  -geozone=<from>,<to>                  Look for matching geozones for account/device");
        Print.sysPrintln("  -geocode=<from>,<to>[,RGMODE[,RENEW]] Apply reverse-geocode to addresses");
        Print.sysPrintln("  -update                               Update matching geozone/address [-geozone,-geocode]");
        Print.sysPrintln("Retroactive Reverse-geocoding:");
        Print.sysPrintln("  Required parameters: ");
        Print.sysPrintln("    -account=ID                         (required) Account ID");
        Print.sysPrintln("    -device=ID                          (required) Device ID");
        Print.sysPrintln("    -update                             (optional) True to update EventData record");
        Print.sysPrintln("    -geocode=FROM,TO[,RGMODE[,RENEW]]   (required)");
        Print.sysPrintln("      FROM   - 'From' date range in the format YYYY/MM/DD");
        Print.sysPrintln("      TO     - 'To' date range in the format YYYY/MM/DD");
        Print.sysPrintln("      RGMODE - GeocoderMode: one of \"geozone\", \"partial\", or \"full\"");
        Print.sysPrintln("      RENEW  - \"renew\" or \"true\" to force the address to be renewed");
        Print.sysPrintln("  Example: ");
        Print.sysPrintln("    bin/exe org.opengts.db.EventUtil -account=demo -device=demo -geocode=2014/12/01,2014/12/03,geozone,renew -update");
        System.exit(1);
    }

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String devID   = RTConfig.getString(ARG_DEVICE, "");

        /* account/device specified? */
        if ((acctID == null) || acctID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        } else
        if ((devID == null) || devID.equals("")) {
            Print.logError("Device-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID);
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logError("Error loading Account: " + acctID);
            dbe.printException();
            System.exit(99);
        }
        TimeZone timeZone = DateTime.getTimeZone(acct.getTimeZone()); // will be GMT if invalid
        BasicPrivateLabel privLabel = acct.getPrivateLabel();
        boolean allTags = true;

        /* get device(s) */
        Device devList[] = null;
        try {
            if (devID.equals("*") || devID.equals("ALL")) {
                OrderedSet<String> devIdList = Device.getDeviceIDsForAccount(acctID, null, true);
                if (devIdList.size() <= 0) {
                    Print.logError("Account does not contain any Devices: " + acctID);
                    usage();
                }
                devList = new Device[devIdList.size()];
                for (int i = 0; i < devIdList.size(); i++) {
                    Device dev = Device._getDevice(acct, devIdList.get(i));
                    if (dev == null) {
                        Print.logError("Device-ID does not exist: " + acctID + "," + devIdList.get(i));
                        usage();
                    }
                    devList[i] = dev;
                }
            } else
            if (devID.indexOf(",") > 0) {
                String devIdList[] = StringTools.split(devID,',');
                if (devIdList.length <= 0) {
                    Print.logError("Account does not contain any Devices: " + acctID);
                    usage();
                }
                devList = new Device[devIdList.length];
                for (int i = 0; i < devIdList.length; i++) {
                    Device dev = Device._getDevice(acct, devIdList[i]);
                    if (dev == null) {
                        Print.logError("Device-ID does not exist: " + acctID + "," + devIdList[i]);
                        usage();
                    }
                    devList[i] = dev;
                }
            } else
            if (devID.startsWith("G:")) {
                String groupID = devID.substring("G:".length()).toLowerCase().trim();
                OrderedSet<String> devIdList = DeviceGroup.getDeviceIDsForGroup(acctID,groupID,null,false/*inclInactv*/,-1L);
                if (devIdList.size() <= 0) {
                    Print.logError("DeviceGroup does not contain any Devices: " + groupID);
                    usage();
                }
                devList = new Device[devIdList.size()];
                for (int i = 0; i < devIdList.size(); i++) {
                    Device dev = Device._getDevice(acct, devIdList.get(i));
                    if (dev == null) {
                        Print.logError("Device-ID does not exist: " + acctID + "," + devIdList.get(i));
                        usage();
                    }
                    devList[i] = dev;
                }
            } else {
                Device dev = Device.getDevice(acct, devID); // null if non-existent
                if (dev == null) {
                    Print.logError("Device-ID does not exist: " + acctID + "," + devID);
                    usage();
                }
                devList = new Device[] { dev };
            }
        } catch (DBException dbe) {
            Print.logError("Error loading Device: " + acctID + "," + devID);
            dbe.printException();
            System.exit(99);
        }

        /* events */
        // -- Writes events to "-output=FILE"
        // -    -events=17345678|17364636|40
        // -    -events=YYYY/MM/DD|YYYY/MM/DD|40
        if (RTConfig.hasProperty(ARG_EVENTS)) {

            /* get requested date range */
            long startTime = -1L;            
            long endTime   = -1L;
            long limit     = DFT_CSV_LIMIT;
            long rangeTime[] = EventUtil.parseArgDateRange(RTConfig.getString(ARG_EVENTS,""), timeZone);
            if (rangeTime != null) {
                startTime = rangeTime[0];            
                endTime   = rangeTime[1];
                limit     = (rangeTime[2] > 0L)? rangeTime[2] : DFT_CSV_LIMIT;
            }

            /* open output file */
            String evFile = RTConfig.getString(ARG_OUTPUT, "");
            OutputStream fos = EventUtil.openFileOutputStream(evFile);
            if (fos == null) {
                System.exit(1);
            }

            /* extract records */
            // this assumes that the number of returned records is reasonable and fits in memory
            try {
                EventData evList[] = null;
                if ((startTime <= 0L) && (endTime <= 0L)) {
                    evList = devList[0].getLatestEvents(limit, false);
                } else {
                    evList = devList[0].getRangeEvents(startTime, endTime, false, EventData.LimitType.FIRST, limit);
                }
                devList[0].setSavedRangeEvents(evList);
            } catch (DBException dbe) {
                dbe.printException();
                System.exit(99);
            }

            /* output records */
            int outFmt = EventUtil.parseOutputFormat(RTConfig.getString(ARG_FORMAT,null),FORMAT_CSV);
            EventUtil evUtil = new EventUtil();
            try {
                java.util.List<Device> devVector = new Vector<Device>();
                devVector.add(devList[0]);
                evUtil.writeEvents(fos, 
                    acct, devVector,
                    outFmt, allTags, null/*timezone*/,
                    privLabel);
            } catch (IOException t) {
                Print.logException("Error writing events", t);
                System.exit(1);
            }

            /* close output file */
            EventUtil.closeOutputStream(fos);

            /* done */
            System.exit(0);
            
        }

        /* odometer */
        // -- update events with new calculated/accumulated odometer
        // -    '-odom=17345678|17364636'
        // -    '-odom=YYYY/MM/DD|YYYY/MM/DD'
        if (RTConfig.hasProperty(ARG_ODOMETER)) {
            // -- bin/exeJava org.opengts.db.EventUtil -acct=ACCOUNT -dev=DEVICE '-odom=2016/04/10|2016/07/31' -upd

            /* get requested date range */
            long rangeTime[] = EventUtil.parseArgDateRange(RTConfig.getString(ARG_ODOMETER,""), timeZone);
            if (rangeTime == null) {
                Print.logError("Date range not specified...");
                System.exit(99);
            }
            long startTime = rangeTime[0];            
            long endTime   = rangeTime[1];
            Print.sysPrintln("Starting date: " + startTime + " [" + new DateTime(startTime) + "]");
            Print.sysPrintln("Ending date  : " + endTime   + " [" + new DateTime(endTime  ) + "]");

            /* maximum speed calculated between points */
            final double MAX_MPH = 100.0;
            final Vector<String> invMphDevList = new Vector<String>();

            /* traverse records */
            try {
                final Account rhAccount = acct;
                final boolean rhUpdate  = RTConfig.getBoolean(ARG_UPDATE,false);
                final String rhUpdateFields[] = { 
                    EventData.FLD_workDistanceKM,
                    EventData.FLD_odometerKM, 
                  //EventData.FLD_distanceKM,
                    EventData.FLD_odometerOffsetKM,
                };
                for (int d = 0; d < devList.length; d++) {
                    Device device = devList[d];
                    final String accDevID = device.getAccountID() + "/" + device.getDeviceID();
                    Print.sysPrintln("-----------------------------------------------------");
                    Print.sysPrintln("Device : " + accDevID);
                    final AccumulatorDouble invalMPH = new AccumulatorDouble(0.0);
                    // -- get starting odometer
                    EventData priorEvent = device.getLastEvent(startTime-1L,true); // prior valid GPS event
                    if (priorEvent != null) {
                        long evTS = priorEvent.getTimestamp();
                        Print.sysPrintln(accDevID + ": " + evTS + " Starting odometer: " + priorEvent.getOdometerKM());
                    } else {
                        Print.sysPrintln(accDevID + ": Starting odometer: 0.0");
                    }
                    final Tuple.Single<EventData> lastEvTup = new Tuple.Single<EventData>(priorEvent);
                    // -- get range of events
                    Print.sysPrintln("Getting event range ...");
                    final AccumulatorLong count = new AccumulatorLong(1L);
                    EventData.getRangeEvents(
                        device.getAccountID(), device.getDeviceID(),
                        startTime, endTime,
                        null/*statusCodes[]*/, // all status codes
                        false/*validGPS*/,
                        EventData.LimitType.FIRST, -1L/*limit*/, true/*ascending*/,
                        null/*additionalSelect*/,
                        new DBRecordHandler<EventData>() {
                            public int handleDBRecord(EventData rcd) throws DBException {
                                // -- count
                                long rcdNum = count.postIncrement();
                                // --
                                EventData lastEV  = lastEvTup.a; // could be null on first events
                                double    odomKM  = (lastEV != null)? lastEV.getOdometerKM() : 0.0;
                                double    deltaKM = 0.0;
                                EventData ev      = rcd; // not null
                                long      evTS    = ev.getTimestamp();
                                double    evOdom  = ev.getOdometerKM();
                                // -- update odometer accumulator
                                double    mph     = 0.0;
                                boolean   valid   = true;
                                if (ev.isValidGeoPoint()) {
                                    if (lastEV != null) { // 'lastEV' contains a valid GeoPoint
                                        long     ts1  = lastEV.getTimestamp();
                                        long     ts2  = ev.getTimestamp();
                                        long deltaTS  = ts2 - ts1;
                                        GeoPoint gp1  = lastEV.getGeoPoint();
                                        GeoPoint gp2  = ev.getGeoPoint();
                                        deltaKM       = gp1.kilometersToPoint(gp2);
                                        if (deltaTS > 0L) {
                                            // -- mph = (deltaKM / deltaTS) * Miles/KM * Sec/Hour;
                                            mph = (deltaKM / (double)deltaTS) * GeoPoint.MILES_PER_KILOMETER * 3600.0;
                                        } else {
                                            // -- assume time delta is at least 1 second
                                            deltaTS = 1L;
                                            mph = (deltaKM / (double)deltaTS) * GeoPoint.MILES_PER_KILOMETER * 3600.0;
                                        }
                                        if (mph <= MAX_MPH) {
                                            odomKM += deltaKM;
                                            valid = true;
                                        } else {
                                            valid = false;
                                            if (invalMPH.get() <= 0.0) {
                                                invalMPH.set(mph);
                                            } else { 
                                                invalMPH.set(Math.min(invalMPH.get(),mph));
                                            }
                                        }
                                    }
                                    if (valid) {
                                        // -- save only valid events
                                        lastEvTup.a = ev; // valid GPS only
                                    }
                                }
                                // -- save old odometer
                                if (ev.getWorkDistanceKM() <= 0.0) {
                                    ev.setWorkDistanceKM(ev.getOdometerKM()); // prior odometer
                                } else {
                                    //Print.sysPrintln("Work odometer already set: " + ev.getWorkDistanceKM());
                                }
                                // -- set new odometer
                                ev.setOdometerKM(odomKM);
                              //ev.setDistanceKM(deltaKM);
                                ev.setOdometerOffsetKM(0.0); // clear offset
                                // -- update EventData
                                String odomKMStr = StringTools.format(odomKM, "0.0");
                                String evOdomStr = StringTools.format(evOdom, "0.0");
                                String msg = rcdNum+") "+accDevID+": "+evTS+"["+(new DateTime(evTS).shortFormat(null))+"] "+ev.getGeoPoint()+" Odometer="+odomKMStr+" [was "+evOdomStr+"]";
                                if (!valid) {
                                    String mphStr = StringTools.format(mph,"0.0");
                                    msg += " (INVALID SPEED " + mphStr + " mph - skipped)";
                                }
                                if (rhUpdate) {
                                    if (!valid) {
                                        Print.sysPrintln(msg+" (updated)");
                                    } else {
                                        Print.sysPrintln(msg+" (updated)");
                                    }
                                    ev.update(rhUpdateFields);
                                } else {
                                    if (!valid) {
                                        Print.sysPrintln(msg+" (calculated)");
                                    } else {
                                        Print.sysPrintln(msg+" (calculated)");
                                    }
                                }
                                // -- do not save this record for list
                                return DBRH_SKIP;
                            }
                        }
                    );
                    // -- invalid speed event
                    if (invalMPH.get() > 0.0) {
                        String mphStr = StringTools.format(invalMPH.get(),"0.0");
                        Print.sysPrintln(accDevID+": Found invalid location event ["+mphStr+"]");
                        invMphDevList.add(device.getDeviceID());
                    }
                    // -- update device last
                    EventData lastEV = lastEvTup.a; // could be null on first events
                    if (lastEV != null) {
                        double odomKM = lastEV.getOdometerKM();
                        double lastOdomKM = device.getLastOdometerKM();
                        device.setLastOdometerKM(odomKM);
                        if (rhUpdate) {
                            Print.sysPrintln(accDevID+": Updated Device last odometer: "+odomKM+" [was "+lastOdomKM+"]");
                            device.update(Device.FLD_lastOdometerKM);
                        } else {
                            Print.sysPrintln(accDevID+": Calculated Device last odometer: "+odomKM+" [was "+lastOdomKM+"]");
                        }
                    }
                }
            } catch (DBException dbe) {
                dbe.printException();
                System.exit(99);
            }

            /* Done processing */
            Print.sysPrintln("");
            Print.sysPrintln(ListTools.size(devList) + " devices have been processed.");

            /* found devices with invalid events? */
            if (!ListTools.isEmpty(invMphDevList)) {
                Print.sysPrintln("Invalid events were found in the following "+ListTools.size(invMphDevList)+" deviceIDs:");
                for (String D : invMphDevList) {
                    Print.sysPrintln("  " + D);
                }
            }

            /* done */
            Print.sysPrintln("");
            System.exit(0);

        }

        /* geozone */
        // -- updates events with current "geozoneID"
        // -    -geozone=17345678|17364636
        // -    -geozone=YYYY/MM/DD|YYYY/MM/DD
        if (RTConfig.hasProperty(ARG_GEOZONE)) {

            /* get requested date range */
            long rangeTime[] = EventUtil.parseArgDateRange(RTConfig.getString(ARG_GEOZONE,""), timeZone);
            if (rangeTime == null) {
                Print.logError("Date range not specified...");
                System.exit(99);
            }
            long startTime = rangeTime[0];            
            long endTime   = rangeTime[1];

            /* traverse records */
            try {
                final Account rhAccount = acct;
                final boolean rhUpdate  = RTConfig.getBoolean(ARG_UPDATE,false);
                final String rhUpdateFields[] = { 
                    EventData.FLD_geozoneID, 
                    EventData.FLD_address 
                };
                EventData.getRangeEvents(
                    devList[0].getAccountID(), devList[0].getDeviceID(),
                    startTime, endTime,
                    null/*statusCodes[]*/,
                    true/*validGPS*/,
                    EventData.LimitType.FIRST, -1L/*limit*/, true/*ascending*/,
                    null/*additionalSelect*/,
                    new DBRecordHandler<EventData>() {
                        public int handleDBRecord(EventData rcd) throws DBException {
                            EventData ev = rcd;
                            GeoPoint  gp = ev.getGeoPoint();
                            Geozone   gz = Geozone.getGeozone(rhAccount, null, gp, true);
                            if (gz != null) {
                                if (rhUpdate) {
                                    Print.logInfo("Updating Geozone: [" + gz.getGeozoneID() + "] " + gz.getDescription());
                                    //if (gzone.getClientUpload() && (ev.getGeozoneIndex() == 0L)) {
                                    //    ev.setGeozoneIndex(gzone.getClientID());
                                    //}
                                    ev.setGeozoneID(gz.getGeozoneID());
                                    ev.setAddress(gz.getDescription());
                                    ev.update(rhUpdateFields);
                                } else {
                                    Print.logInfo("Found Geozone: [" + gz.getGeozoneID() + "] " + gz.getDescription());
                                }
                            }
                            return DBRH_SKIP;
                        }
                    }
                );
            } catch (DBException dbe) {
                dbe.printException();
                System.exit(99);
            }

            /* done */
            System.exit(0);

        }

        /* reverse-geocode */
        // -- reverse-geocodes date-range in EventData
        // -    -geocode=17345678,17364636[,GeocoderMode[,RenewAddress]]     [-update=true]
        // -    -geocode=YYYY/MM/DD,YYYY/MM/DD[,GeocoderMode[,RenewAddress]] [-update=true]
        // - IE: bin/exeJava org.opengts.db.EventUtil -acct=demo -dev=test-1 -rg=2015/06/05,2015/06/07 -update=true
        if (RTConfig.hasProperty(ARG_GEOCODE)) {

            /* get requested date range */
            String gcArgs[] = StringTools.split(RTConfig.getString(ARG_GEOCODE,""),',');
            // -- date range
            String rangeStr  = "";
            if (gcArgs.length > 1) {
                // -- YYYY/MM/DD,YYYY/MM/DD
                rangeStr  = gcArgs[0] + "," + gcArgs[1];
            }
            // -- GeocoderMode
            String rgModeStr = "";
            if (gcArgs.length > 2) {
                // -- none,geozone,partial,full
                rgModeStr = StringTools.trim(gcArgs[2]);
            }
            // -- RenewAddress (Force)
            boolean renewAddress = false;
            if (gcArgs.length > 3) {
                // -- renew|true|false
                if (gcArgs[3].equalsIgnoreCase("renew") ||
                    gcArgs[3].equalsIgnoreCase("force")   ) {
                    // -- "renew" | "force"
                    renewAddress = true;
                } else {
                    // -- "true" | "false"
                    renewAddress = StringTools.parseBoolean(gcArgs[3],renewAddress);
                }
            }

            /* get requested date range */
            long rangeTime[] = EventUtil.parseArgDateRange(rangeStr, timeZone);
            if (rangeTime == null) {
                Print.logError("Date range not specified...");
                System.exit(99);
            }
            long startTime = rangeTime[0];            
            long endTime   = rangeTime[1];

            /* geocoder mode */
            Account.GeocoderMode rgMode = null;
            if (!StringTools.isBlank(rgModeStr) && !rgModeStr.equalsIgnoreCase("default")) {
                //Print.sysPrintln("Parsing GeocoderMode: " + rgModeStr);
                rgMode = Account.getGeocoderMode(rgModeStr, rgMode);
            }

            /* log */
            Print.sysPrintln("Reverse-geocoding events in range:");
            Print.sysPrintln("   Start        : " + new DateTime(startTime));
            Print.sysPrintln("   End          : " + new DateTime(endTime  ));
            Print.sysPrintln("   GeocoderMode : " + ((rgMode != null)? rgMode.toString() : "default"));
            Print.sysPrintln("   RenewAddress : " + renewAddress);
            //Print.sysPrintln("... exiting ..."); System.exit(0);

            /* get geocoder mode */
            Account.GeocoderMode geocoderMode = Account.getGeocoderMode(acct);
            if (geocoderMode.isNone()) {
                // -- no geocoding is performed for this account
                Print.logError("GeocoderMode.NONE found/specified: " + acct.getAccountID());
                System.exit(99);
            }

            /* check for reverse-geocoder */
            /*if (geocoderMode.okPartial())*/ {
                ReverseGeocodeProvider rgp = acct.getPrivateLabel().getReverseGeocodeProvider();
                if (rgp == null) {
                    // -- no ReverseGeocodeProvider, no reverse-geocoding
                    Print.logError("No ReverseGeocodeProvider for this account: " + acct.getAccountID());
                    System.exit(99);
                }
            }

            /* traverse records (for each device) */
            for (int d = 0; d < devList.length; d++) {
                Print.sysPrintln("");
                Print.sysPrintln("--- Reverse-Gecoding Events: " + devList[d].getAccountID() + "," + devList[d].getDeviceID());
                try {
                    final Account rhAccount     = acct;
                    final Device  rhDevice      = devList[d];
                    final boolean rhUpdate      = RTConfig.getBoolean(ARG_UPDATE,false);
                    final boolean rhRenewAddr   = renewAddress; // force new address
                    final AccumulatorLong accum = new AccumulatorLong(0L);
                    final Account.GeocoderMode rhRgMode = rgMode;
                    EventData.getRangeEvents(
                        devList[d].getAccountID(), devList[d].getDeviceID(),
                        startTime, endTime,
                        null/*statusCodes[]*/,
                        true/*validGPS*/,
                        EventData.LimitType.FIRST, -1L/*limit*/, true/*ascending*/,
                        null/*additionalSelect*/,
                        new DBRecordHandler<EventData>() {
                            public int handleDBRecord(EventData rcd) throws DBException {
                                EventData ev = rcd;
                                ev.setAccount(rhAccount);
                                ev.setDevice(rhDevice);
                                accum.increment();
                                String dt = (new DateTime(ev.getTimestamp())).shortFormat(null);
                                //Print.logInfo("Checking event: " + ev.getGeoPoint());
                                try {
                                    Set<String> updf = ev.updateAddress(false,rhRenewAddr,rhRgMode);
                                    if (!ListTools.isEmpty(updf)) {
                                        if (rhUpdate) {
                                            Print.logInfo("Update: ["+dt+"] " + ev.getAddress());
                                            ev.update(updf);
                                            // -- do not update Device.FLD_lastSubdivision here !
                                        } else {
                                            Print.logWarn("Found : ["+dt+"] " + ev.getAddress() + " [NOT UPDATED]");
                                        }
                                    }
                                } catch (SlowOperationException soe) {
                                    // will not occur
                                }
                                return DBRH_SKIP;
                            }
                        }
                    );
                } catch (DBException dbe) {
                    dbe.printException();
                    System.exit(99);
                }
            }

            /* done */
            System.exit(0);

        }

        /* usage */
        usage();

    }

}
