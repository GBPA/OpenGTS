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
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/09  Martin D. Flynn
//     -Integrate DBException
//  2006/05/23  Martin D. Flynn
//     -Changed column 'uniqueID' to a 'VARCHAR(40)'
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -Various new fields added
//  2007/03/25  Martin D. Flynn
//     -Added 'equipmentType', 'groupID'
//     -Moved to 'org.opengts.db.tables'
//  2007/04/15  Martin D. Flynn
//     -Added 'borderCrossing' column.
//  2007/06/30  Martin D. Flynn
//     -Added 'getFirstEvent', 'getLastEvent'
//  2007/07/14  Martin D. Flynn
//     -Added '-uniqueid' command-line option.
//  2007/07/27  Martin D. Flynn
//     -Added 'notifyAction' column
//  2007/08/09  Martin D. Flynn
//     -Renamed command-line option "uniqid" to "uniqueid"
//     -Set 'deviceExists' to true when creating a new device.
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//     -Added handlers for client device errors, diagnostics, and properties.
//     -Added device lookup for the specified unique-id.
//  2007/11/28  Martin D. Flynn
//     -Added columns 'lastBorderCrossTime', 'simPhoneNumber', 'lastInputState'.
//     -Added additional 'Entity' methods
//     -Added OpenDMTP 'CommandErrors' definition section.
//     -Added '-editall' command-line option to display all fields.
//  2007/12/13  Martin D. Flynn
//     -Added an EventData filter to check for invalid odometer values.
//  2007/01/10  Martin D. Flynn
//     -Added column 'notes', 'imeiNumber'
//     -Removed handlers for client device errors, diagnostics, and properties
//      (these handlers have been implemented in 'DeviceDBImpl.java')
//  2008/02/11  Martin D. Flynn
//     -Added columns 'FLD_deviceCode', 'FLD_vehicleID'
//  2008/03/12  Martin D. Flynn
//     -Added column 'FLD_notifyPriority'
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/05/20  Martin D. Flynn
//     -Fixed 'UniqueID" to again make it visible to the CLI record editor.
//  2008/06/20  Martin D. Flynn
//     -Added column 'FLD_notifyDescription'
//  2008/07/21  Martin D. Flynn
//     -Added column 'FLD_linkURL'
//  2008/08/24  Martin D. Flynn
//     -Added 'validGPS' argument to 'getRangeEvents' and 'getLatestEvents'
//  2008/09/01  Martin D. Flynn
//     -Added optional field list "FixedLocationFieldInfo"
//     -Added field/column "FLD_smsEmail"
//  2008/10/16  Martin D. Flynn
//     -Added FLD_lastPingTime, FLD_totalPingCount
//  2008/12/01  Martin D. Flynn
//     -Added FLD_linkDescription, FLD_pushpinID
//     -Added optional field list 'GeoCorridorFieldInfo'
//  2009/05/24  Martin D. Flynn
//     -Added FLD_pendingPingCommand, FLD_remotePortCurrent
//     -Added FLD_lastValidLatitude/Longitude to optimize Geozone calculations.
//     -Added FLS_lastOdometerKM to optimize GPS odometer calculations.
//  2009/06/01  Martin D. Flynn
//     -Increased background thread pool size/limit to 25.
//  2009/09/23  Martin D. Flynn
//     -Added support for ignoring/truncating events with future timestamps
//     -Added FLD_maxPingCount
//  2009/10/02  Martin D. Flynn
//     -Changed "checkGeozoneTransitions" to return an array of Geozone transitions,
//      fixing the case where 2 adjacent events occur in 2 different geozones.
//  2009/11/01  Martin D. Flynn
//     -Added FLD_expectAck, FLD_lastAckCommand, FLD_lastAckTime
//  2009/12/16  Martin D. Flynn
//     -Added command-line check for "Periodic Maintenance/Service Due" (-maintkm=email)
//  2010/01/29  Martin D. Flynn
//     -Added FLD_listenPortCurrent
//  2010/04/11  Martin D. Flynn
//     -Added FLD_dataKey, FLD_displayColor, FLD_licensePlate
//     -Added 'deleteEventDataPriorTo' to delete old historical EventData records.
//  2010/07/04  Martin D. Flynn
//     -Added FLD_expirationTime, FLD_maintIntervalKM1, FLD_maintOdometerKM1
//  2010/07/18  Martin D. Flynn
//     -Added FLD_lastBatteryLevel, FLD_fuelCapacity
//  2010/09/09  Martin D. Flynn
//     -Added "deleteOldEvents" option
//  2010/11/29  Martin D. Flynn
//     -Added FLD_lastFuelLevel
//     -Added configurable "maximum odometer km"
//  2011/01/28  Martin D. Flynn
//     -Added FLD_lastOilLevel
//  2011/03/08  Martin D. Flynn
//     -Added "getFieldValueString"
//     -Added alternate key "simphone" to field FLD_simPhoneNumber.
//     -Added "loadDeviceBySimPhoneNumber(...)"
//     -Added column FLD_speedLimitKPH
//  2011/04/01  Martin D. Flynn
//     -Added FuelManager module support (requires installed FuelManager)
//     -If "ALLOW_USE_EMAIL_WRAPPER" is false, "getNotifyUseWrapper()" returns false.
//  2011/05/13  Martin D. Flynn
//     -Change to invalid speed maximum checking.
//  2011/06/16  Martin D. Flynn
//     -"lastNotifyTime"/"lastNotifyCode" now only changed if modified.
//      (ie. removed from "DefaultUpdatedFieldsList")
//     -Added FLD_fuelEconomy (approximate fuel economy), FLD_lastEngineHours
//  2011/07/01  Martin D. Flynn
//     -Added "CheckNotifySelector()"
//     -Added FLD_lastValidHeading
//  2011/08/21  Martin D. Flynn
//     -Added convenience setting check for geozone arrive/depart auto-notify
//  2011/10/03  Martin D. Flynn
//     -Added FLD_parkedLatitude, FLD_parkedLongitude, FLD_parkedRadius, FLD_lastFuelTotal
//  2011/12/06  Martin D. Flynn
//     -Added FLD_jobNumber, FLD_jobLatitude, FLD_jobLongitude, FLD_jobRadius, FLD_planDistanceKM
//     -Added KEY_DRIVERID, KEY_DRIVER to "getFieldValueString"
//     -Updated "getFieldValueString" to also search for matching table fields.
//  2012/02/03  Martin D. Flynn
//     -Added FLD_lastIgnitionOffTime
//     -Added optimization for "getCurrentIgnitionState()"
//  2012/04/03  Martin D. Flynn
//     -Validate both last ignition on/off times in "getCurrentIgnitionState()"
//      before comparing timestamps.
//     -Added FLD_simID, FLD_lastTcpSessionID
//     -Added runtime config settings for "GetSimulateEngineHours()".
//     -Added checking for "<zone>.isDeviceInGroup(...)" during Geozone arrive/depart detection.
//     -Added "appendLastFaultCode(...)" to append new OBDII fault codes to old.
//     -Renamed "getFieldValueString" to "getKeyFieldValue", and added title support.
//  2012/05/27  Martin D. Flynn
//     -Added option to save EventData "driverID" to Device. (see SAVE_EVENT_DRIVER_ID)
//     -Fixed NPE in "_getKeyFieldString"
//  2012/06/29  Martin D. Flynn
//     -Added FLD_lastAckResponse (pending, not yet fully implemented)
//  2012/08/01  Martin D. Flynn
//     -Added FLD_assignedUserID, FLD_lastServiceTime, FLD_nextServiceTime
//  2012/10/16  Martin D. Flynn
//     -Added FLD_expectAckCode
//  2012/12/24  Martin D. Flynn
//     -Fix Reverse-Geocoding for cell-tower locations when RG 'alwaysFast' is true.
//  2013/02/06  Martin D. Flynn
//     -Added check for "<Account>.getSmsEnabled()" before sending SMS messages.
//  2013/03/01  Martin D. Flynn
//     -Added check for past event timestamps (see "pastEventDateAction")
//     -Ignore Geozone check ("checkGeozoneTransitions") for events that are older than 
//      the last received event.
//     -Set event "inputMask" to last device input state, if unset by DCS.
//  2013/04/08  Martin D. Flynn
//     -Added "zero"/"setzero" to invalid speed action (see "invalidSpeedAction")
//     -Added check for duplicate STATUS_GFMI_STOP_STATUS_# events
//     -Added FLD_installTime, FLD_resetTime, FLD_vehicleMake, FLD_vehicleModel
//     -Added FLD_commandStateMask, FLD_hoursOfOperation
//  2013/05/28  Martin D. Flynn
//     -Added FLD_lastDistanceKM to default update list
//     -Updated handling of DriverID
//  2013/08/06  Martin D. Flynn
//     -Added FLD_lastEngineOnTime/FLD_lastEngineOffTime
//     -Support engine-hour accumulation based on Engine On/Off events.
//  2013/11/11  Martin D. Flynn
//     -Added FLD_equipmentStatus, FLD_licenseExpire, FLD_lastEngineOnHours, FLD_lastIgnitionOnHours
//     -Added FLD_fuelRatePerHour? FLD_dcsConfigString? FLD_lastValidSpeedKPH? FLD_pendingMessage? FLD_pendingMessageACK? (??)
//     -Added delta-distance check to event motion state change on event insertion.
//      (see "EVENT_START_MOTION_RADIUS_M")
//  2014/03/03  Martin D. Flynn
//     -Made some adjustments to display warnings when unable to count InnoDB events.
//     -Added KEY_DRIVER_PHONE [B28]
//     -Added FLD_fuelCostPerLiter
//  2014/06/29  Martin D. Flynn
//     -Check for inactive Geozone during Arrive/Depart test
//     -Added support for FuelLevelProfile
//  2014/09/16  Martin D. Flynn
//     -Limit upper value for "totalPingCount"/"maxPingCount" to 0xFFFF [v2.5.7-B11]
//     -Added support for obtaining 'notifyEmail' from DeviceGroup [2.5.7-B27]
//     -Fixed ignition-hours accumulation when ignition is off (see "ignHours") [2.5.7-B32]
//     -Added 'fieldNames' parameter option to "getDevice" (to get device with specific fields only) [2.5.7-B35]
//     -Added "_getDevice" which throws exception on error (ie. if key does not exist) [2.5.7-B35]
//  2014/11/03  Martin D. Flynn
//     -Added FLD_parkedMaxSpeedKPH.
//     -Changes to the accumulated ignition/engine hours [2.5.8-B36]
//  2015/05/03  Martin D. Flynn
//     -Added FLD_vehicleYear
//     -Use "Recipients" to separate email/sms destinations
//     -Retain DriverStatus if specified in the inserted EventData record. [2.5.9-B37]
//     -Added "getNearbyDeviceMap(...)" [2.5.9-B57]
//  2015/08/16  Martin D. Flynn
//     -Added FLD_fuelTankProfile2
//     -Updated "getNearbyDevices"
//     -Added KEY_LAST_IGN_ON, KEY_LAST_IGN_OFF, KEY_IGNITION_STATE
//  2016/01/04  Martin D. Flynn
//     -Added FLD_vehicleCurbWeight, FLD_vehicleGrossWeight, FLD_lastFuelLevel2
//     -"getDevice" optimized to eliminate 'exists(...)' db query.
//  2016/04/06  Martin D. Flynn
//     -Added FLD_fuelCapacity2
//     -Apply event GPS-Age when setting the last known GPS timestamp [2.6.2-B44]
//     -Added KEY_MAINT_DIST_NEXT [2.6.2-B53]
//     -Added "validateUniqueID" [2.6.2-B67]
//  2016/12/21  Martin D. Flynn
//     -Added FLD_lastVBatteryVolts, FLD_lastEventStatusCode [2.6.4-B35]
//     -Added support for FLD_lastPtoOnTime, FLD_lastPtoOffTime, FLD_lastPtoOnHours [2.6.4-B60]
//     -Updated deferred rule check (see "deferRuleCheck")
//     -Added FLD_lastBatteryVolts
//  2017/06/16  Martin D. Flynn
//     -Pending FLD_fuelProperties [2.6.5-B20]
//     -Added FLD_maximumRpm [2.6.5-B34]
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
//import java.net.*;
import java.sql.*;

import org.opengts.util.*;

import org.opengts.geocoder.*;
import org.opengts.cellid.*;

import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.RuleFactory.NotifyAction;
import org.opengts.db.tables.Transport.Encodings;

/**
*** This class represents a tracked asset (ie. something that is being tracked).
*** Currently, this DBRecord also represents the tracking hardware device as well.
**/

public class Device // Asset
    extends DeviceRecord<Device>
    implements DataTransport
{

    // ------------------------------------------------------------------------

    private static int LogEventDataInsertion = Print.LOG_UNDEFINED;
    public static void SetLogEventDataInsertion(int logLevel)
    {
        if (logLevel >= Print.LOG_WARN) {
            Device.LogEventDataInsertion = logLevel;
        } else {
            Print.logWarn("'SetLogEventDataInsertion' ignoring excessive log level: " +
                Print.getLogLevelString(logLevel) + " (using LOG_WARN instead)");
            Device.LogEventDataInsertion = Print.LOG_WARN;
        }
    }

    // ------------------------------------------------------------------------

    /* optimization for caching status code descriptions */
    public  static      boolean CACHE_STATUS_CODE_DESCRIPTIONS      = true;

    /* ReverseGeocodeProvider required on command-line "-insertGP" */
    public  static      boolean INSERT_REVERSEGEOCODE_REQUIRED      = false;

    /* allow Device record specified "notifyUseWrapper" value */
    public  static      boolean ALLOW_USE_EMAIL_WRAPPER             = false;

    /* auto-generate non-moving event just prior to motion-change */
    public  static      boolean AUTO_GENERATE_NON_MOVING_EVENT      = false;
    public  static      long    MAX_STOPPED_DELTA_SEC               = DateTime.MinuteSeconds(20);

    public  static      double  EVENT_START_MOTION_RADIUS_M         = 150.0; // meters

    // ------------------------------------------------------------------------

    /* deferred rule check */
    private static volatile int DEFERRED_RULE_CHECK = -1; // false
    // -- intended to be used for deferred rule checking (a snapshot of the Device record
    // -  is saved and passed to the rule trigger handling).  
    // -- (ENRE "<EventFunctionMap>.getPreviousGeoPoint" may still need some additional  
    // -  implementation before this can be considered complete).

    /**
    *** Gets the DeferredRuleCheck property
    **/
    private static int GetDeferRuleCheckToPostInsert()
    {
        if (DEFERRED_RULE_CHECK < 0) {
            // -- not yet initialized
            synchronized (DBConfig.PROP_Device_deferRuleCheckToPostInsert) {
                if (DEFERRED_RULE_CHECK < 0) {
                    String deferStr = RTConfig.getString(DBConfig.PROP_Device_deferRuleCheckToPostInsert, null);
                    if (StringTools.isBlank(deferStr)) {
                        DEFERRED_RULE_CHECK = 0;
                    } else
                    if (deferStr.equalsIgnoreCase("false"  ) || deferStr.equals("0")) {
                        DEFERRED_RULE_CHECK = 0;
                    } else
                    if (deferStr.equalsIgnoreCase("true"   ) || deferStr.equals("1")) {
                        DEFERRED_RULE_CHECK = 1;
                    } else
                    if (deferStr.equalsIgnoreCase("account") || deferStr.equals("2")) {
                        DEFERRED_RULE_CHECK = 2;
                    } else
                    if (deferStr.equalsIgnoreCase("device")  || deferStr.equals("3")) {
                        DEFERRED_RULE_CHECK = 3;
                    } else {
                        DEFERRED_RULE_CHECK = 0;
                    }
                }
            }
        }
        return DEFERRED_RULE_CHECK;
    }

    /**
    *** Gets the DeferredRuleCheck property
    **/
    private boolean getDeferRuleCheckToPostInsert()
    {
        int defer = Device.GetDeferRuleCheckToPostInsert();
        if (defer <= 0) {
            // -- always false
            return false;
        } else
        if (defer == 1) {
            // -- always true
            return true;
        } else 
        if (defer == 2) {
            // -- TODO: check Account: "this.getAccount()"
            return true;
        } else 
        if (defer == 3) {
            // -- TODO: check Device: "this"
            return true;
        } else {
            // -- other?
            return true;
        }
    }

    // ------------------------------------------------------------------------

    private static EventDataInsertionListener _EventDataInsertionListener = null;

    /**
    *** Sets the EventDataInsertionListener 
    **/
    @SuppressWarnings("unchecked")
    public static void initEventDataInsertionListener()
    {
        String elidClassName = RTConfig.getString(DBConfig.PROP_Device_EventDataInsertionListener, null);
        if (!StringTools.isBlank(elidClassName)) {
            try {
                Class<?> edilClass = (Class<EventDataInsertionListener>)Class.forName(elidClassName);
                EventDataInsertionListener edil = (EventDataInsertionListener)edilClass.newInstance();
                Device.setEventDataInsertionListener(edil);
                Print.logDebug("Installed EventDataInsertionListener: ["+StringTools.className(edil)+"]");
            } catch (ClassNotFoundException cnfe) {
                // -- EventDataInsertionListener class not found (quietly ignore?)
                Print.logError("EventDataInsertionListener class not found: " + elidClassName);
            } catch (ClassCastException cce) {
                // -- specified class is not a EventDataInsertionListener
                Print.logError("Invalid EventDataInsertionListener class: " + elidClassName);
            } catch (Throwable th) { // catch all
                // -- ELogHOSProvider support not present?
                Print.logError("Unexpected EventDataInsertionListener error: " + th);
            }
        }
    }

    /**
    *** Sets the EventDataInsertionListener 
    **/
    public static void setEventDataInsertionListener(EventDataInsertionListener edil)
    {
        Device._EventDataInsertionListener = edil;
    }

    /** 
    *** Returns true if an EventDataInsertionListener has been defined
    *** @return True if an EventDataInsertionListener has been defined
    **/
    public static boolean hasEventDataInsertionListener()
    {
        return (Device._EventDataInsertionListener != null);
    }

    /**
    *** Gets the EventDataInsertionListener 
    **/
    public static EventDataInsertionListener getEventDataInsertionListener()
    {
        return Device._EventDataInsertionListener;
    }

    // ------------------------------------------------------------------------

    public  static final Device EMPTY_ARRAY[]                       = new Device[0];

    private static       int    LastFaultCodeColumnLength           = -1; // FLD_lastFaultCode
    private static       int    NotifyEmailColumnLength             = -1; // FLD_notifyEmail
    private static       int    FuelProfileColumnLength             = -1; // FLD_fuelTankProfile
  //private static       int    FuelPropertiesColumnLength          = -1; // FLD_fuelProperties

    // ------------------------------------------------------------------------

    /* optional columns */
    public static final String  OPTCOLS_OpenDMTPFieldInfo           = "startupInit.Device.OpenDMTPFieldInfo";
    public static final String  OPTCOLS_NotificationFieldInfo       = "startupInit.Device.NotificationFieldInfo";
    public static final String  OPTCOLS_BorderCrossingFieldInfo     = "startupInit.Device.BorderCrossingFieldInfo";
    public static final String  OPTCOLS_LinkFieldInfo               = "startupInit.Device.LinkFieldInfo";
    public static final String  OPTCOLS_FixedLocationFieldInfo      = "startupInit.Device.FixedLocationFieldInfo";
    public static final String  OPTCOLS_GeoCorridorFieldInfo        = "startupInit.Device.GeoCorridorFieldInfo";
    public static final String  OPTCOLS_MaintOdometerFieldInfo      = "startupInit.Device.MaintOdometerFieldInfo";
    public static final String  OPTCOLS_WorkOrderInfo               = "startupInit.Device.WorkOrderInfo";
    public static final String  OPTCOLS_DataPushInfo                = "startupInit.Device.DataPushInfo";
    public static final String  OPTCOLS_ELogHOSInfo                 = "startupInit.Device.ELogHOSInfo";
    public static final String  OPTCOLS_MapShareInfo                = "startupInit.Device.MapShareInfo";
    public static final String  OPTCOLS_AttributeInfo               = "startupInit.Device.AttributeInfo";
    public static final String  OPTCOLS_GlobalSubscriber            = "startupInit.Device.GlobalSubscriber";
    public static final String  OPTCOLS_PlatinumInfo                = "startupInit.Device.PlatinumInfo";

    // ------------------------------------------------------------------------

    /* Event update background ThreadPool */
    // Device.ThreadPool.DeviceEventUpdate.maximumPoolSize=50
    // Device.ThreadPool.DeviceEventUpdate.maximumIdleSeconds=0
    // Device.ThreadPool.DeviceEventUpdate.maximumQueueSize=0
    private static final RTKey PROP_ThreadPool_DeviceEventUpdate_   = RTKey.valueOf(RTKey.ThreadPool_DeviceEventUpdate_);
    private static final int   ThreadPool_DeviceEventUpdate_Size    = 50;
    private static final int   ThreadPool_DeviceEventUpdate_IdleSec =  0;
    private static final int   ThreadPool_DeviceEventUpdate_QueSize =  0;
    private static ThreadPool  ThreadPool_DeviceEventUpdate         = new ThreadPool(
        "DeviceEventUpdate",
        PROP_ThreadPool_DeviceEventUpdate_, // property allowing default override
        ThreadPool_DeviceEventUpdate_Size, 
        ThreadPool_DeviceEventUpdate_IdleSec, 
        ThreadPool_DeviceEventUpdate_QueSize);

    // ------------------------------------------------------------------------
    // new asset defaults

    private static final String NEW_DEVICE_NAME_                    = "New Device";

    // ------------------------------------------------------------------------

    private static final int    EXT_UPDATE_MASK                     = 0xFFFF;
    private static final int    EXT_UPDATE_NONE                     = 0x0000;
    private static final int    EXT_UPDATE_CELLGPS                  = 0x0001;
    private static final int    EXT_UPDATE_ADDRESS                  = 0x0002;
    private static final int    EXT_UPDATE_BORDER                   = 0x0004;
    private static final int    EXT_UPDATE_JMS                      = 0x0008;

    // ------------------------------------------------------------------------
    // -- Events-Per-Second calculations

    private static       boolean EPS_did_init      = false;

    public  static       long    EPS_RANGE_SECONDS = 3600L; // 1 hour
    public  static       long    EPS_RANGE_MS      = EPS_RANGE_SECONDS * 1000L;
    public  static       double  EPS_WEIGHT        = 0.90;
    public  static       double  EPS_ALPHA         = 1.0 - Math.exp(Math.log(1.0 - EPS_WEIGHT) / (double)EPS_RANGE_MS);

    public static long MinMax(long val, long min, long max)
    {
        return (val < min)? min : (val > max)? max : val;
    }

    public static double MinMax(double val, double min, double max)
    {
        return (val < min)? min : (val > max)? max : val;
    }

    public static void initEventsPerSecond()
    {
        // -- synchronized
        if (!EPS_did_init) {
            synchronized (DBConfig.PROP_Device_eventsPerSecond) {
                if (!EPS_did_init) {
                    double E[] = RTConfig.getDoubleArray(DBConfig.PROP_Device_eventsPerSecond,null);
                    if (!ListTools.isEmpty(E)) {
                        // -- <RangeSeconds>,<WeightPercent>
                        // -  Range Seconds
                        long rangeSec = (E.length > 0)? Math.round(E[0]) : EPS_RANGE_SECONDS;
                        // -- Weight
                        double weight = (E.length > 1)? E[1] : EPS_WEIGHT;
                        if (weight >= 2.0) { weight = weight / 100.0; }
                        // -- adjust EPS vars
                        EPS_RANGE_SECONDS = MinMax(rangeSec, 10L, DateTime.DaySeconds(1));;
                        EPS_RANGE_MS      = EPS_RANGE_SECONDS * 1000L;
                        EPS_WEIGHT        = MinMax(weight, 0.05, 0.999);
                        EPS_ALPHA         = 1.0 - Math.exp(Math.log(1.0 - EPS_WEIGHT) / (double)EPS_RANGE_MS);
                    } else {
                        // -- leave as-is
                    }
                    EPS_did_init = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // -- "Device"/"Devices" titles

    /* Default "Device" title (ie. "Taxi", "Tractor", "Vehicle", etc) */
    public static String[] GetTitles(Locale loc)
    {
        I18N i18n = I18N.getI18N(Device.class, loc);
        return new String[] {
            i18n.getString("Device.title.singular", "Vehicle"),
            i18n.getString("Device.title.plural"  , "Vehicles"),
        };
    }

    // ------------------------------------------------------------------------
    // Equipment state

    /**
    *** Equipment State enumeration
    **/
    /*
    public enum EquipmentStatus implements EnumTools.StringLocale, EnumTools.IntValue {
        // -- unspecified
        UNSPECIFIED   (     0, "UNSPECIFIED", I18N.getString(Device.class,"Device.EquipmentStatus.unspecified"   ,"Unspecified"   )),
        // -- active
        ASSIGNED      ( 11000, "ASSIGNED"     , I18N.getString(Device.class,"Device.EquipmentStatus.assigned"      ,"Assigned"      )),
        ACTIVE        ( 11100, "ACTIVE"       , I18N.getString(Device.class,"Device.EquipmentStatus.active"        ,"Active"        )),
        INSERVICE     ( 11200, "INSERVICE"    , I18N.getString(Device.class,"Device.EquipmentStatus.inService"     ,"InService"     )),
        RENTED        ( 11400, "RENTED"       , I18N.getString(Device.class,"Device.EquipmentStatus.rented"        ,"Rented"        )),
        AUTHORIZED    ( 12000, "AUTHORIZED"   , I18N.getString(Device.class,"Device.EquipmentStatus.authorized"    ,"Authorized"    )),
        COMPLETED     ( 13000, "COMPLETED"    , I18N.getString(Device.class,"Device.EquipmentStatus.completed"     ,"Completed"     )),
        // -- available
        AVAILABLE     ( 23000, "AVAILABLE"    , I18N.getString(Device.class,"Device.EquipmentStatus.available"     ,"Available"     )),
        SPARE         ( 23300, "SPARE"        , I18N.getString(Device.class,"Device.EquipmentStatus.spare"         ,"Spare"         )),
        // -- unavailable
        UNAVAILABLE   ( 34000, "UNAVAILABLE"  , I18N.getString(Device.class,"Device.EquipmentStatus.unavailable"   ,"Unavailable"   )),
        REPAIR        ( 34200, "REPAIR"       , I18N.getString(Device.class,"Device.EquipmentStatus.repair"        ,"Repair"        )),
        PENDING       ( 34400, "PENDING"      , I18N.getString(Device.class,"Device.EquipmentStatus.pending"       ,"Pending"       )),
        IMPOUND       ( 34600, "IMPOUND"      , I18N.getString(Device.class,"Device.EquipmentStatus.impound"       ,"Impound"       )),
        STOLEN        ( 36000, "STOLEN"       , I18N.getString(Device.class,"Device.EquipmentStatus.stolen"        ,"Stolen"        )),
        DECOMMISIONED ( 39000, "DECOMMISIONED", I18N.getString(Device.class,"Device.EquipmentStatus.decommissioned","Decommissioned"));
        // ---
        private int         vv = 0;
        private String      nn = null;
        private I18N.Text   aa = null;
        EquipmentStatus(int v, String n, I18N.Text a) { vv = v; nn = n; aa = a; }
        public int     getIntValue()                  { return vv; }
        public String  getName()                      { return nn; }
        public String  toString()                     { return aa.toString(); }
        public String  toString(Locale loc)           { return aa.toString(loc); }
    };
    */

    /**
    *** Returns the defined EquipmentStatus for the specified device.
    *** @param d  The device from which the EquipmentStatus will be obtained.  
    ***           If null, the default EquipmentStatus will be returned.
    *** @return The EquipmentStatus
    **/
    /*
    public static EquipmentStatus getEquipmentStatus(Device d)
    {
        return (d != null)? 
            EnumTools.getValueOf(EquipmentStatus.class,d.getEquipmentStatus()) : 
            EnumTools.getDefault(EquipmentStatus.class);
    }
    */

    /**
    *** Default Device Equipment Status definition
    **/
    private static class EQStat extends Tuple.Pair<String,I18N.Text> {
        public EQStat(String key, I18N.Text desc) { super(key, desc); }
    }
    private static EQStat EQStatDefaultList[] = {
        new EQStat(""           , I18N.getString(Device.class,"Device.EquipmentStatus.unspecified","Unspecified")),
        new EQStat("inservice"  , I18N.getString(Device.class,"Device.EquipmentStatus.service"    ,"In Service" )),
        new EQStat("rented"     , I18N.getString(Device.class,"Device.EquipmentStatus.rented"     ,"Rented"     )),
        new EQStat("pending"    , I18N.getString(Device.class,"Device.EquipmentStatus.pending"    ,"Pending"    )),
        new EQStat("completed"  , I18N.getString(Device.class,"Device.EquipmentStatus.completed"  ,"Completed"  )),
        new EQStat("available"  , I18N.getString(Device.class,"Device.EquipmentStatus.available"  ,"Available"  )),
        new EQStat("unavailable", I18N.getString(Device.class,"Device.EquipmentStatus.unavailable","Unavailable")),
        new EQStat("repair"     , I18N.getString(Device.class,"Device.EquipmentStatus.repair"     ,"Repair"     )),
        new EQStat("retired"    , I18N.getString(Device.class,"Device.EquipmentStatus.retired"    ,"Retired"    )),
    };

    /**
    *** Return a map of EquipmentStatus keys to description
    **/
    public static OrderedMap<String,String> GetEquipmentStatusMap(Locale locale)
    {
        if (!ListTools.isEmpty(EQStatDefaultList)) {
            OrderedMap<String,String> eqMap = new OrderedMap<String,String>();
            for (EQStat eqs : EQStatDefaultList) {
                String K = eqs.a;
                String D = eqs.b.toString(locale);
                eqMap.put(K, D);
            }
            return eqMap;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // border crossing flags (see 'borderCrossing' column)

    /**
    *** BorderCrossing enabled state enumeration
    **/
    public enum BorderCrossingState implements EnumTools.StringLocale, EnumTools.IntValue {
        OFF ( 0, I18N.getString(Device.class,"Device.boarderCrossing.off","off")),
        ON  ( 1, I18N.getString(Device.class,"Device.boarderCrossing.on" ,"on" ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        BorderCrossingState(int v, I18N.Text a)     { vv = v; aa = a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
    };

    // ------------------------------------------------------------------------
    // Reminder type

    /**
    *** Reminder Type enumeration
    **/
    public enum ReminderType implements EnumTools.StringLocale, EnumTools.IntValue {
        PERIODIC_INTERVAL ( 0, I18N.getString(Device.class,"Device.reminderType.periodicInterval","Periodic Interval")),
        SINGLE_INTERVAL   ( 1, I18N.getString(Device.class,"Device.reminderType.singleInterval"  ,"Single Interval"  ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        ReminderType(int v, I18N.Text a)        { vv = v; aa = a; }
        public int     getIntValue()            { return vv; }
        public String  toString()               { return aa.toString(); }
        public String  toString(Locale loc)     { return aa.toString(loc); }
    };

    /**
    *** Returns the defined ReminderType for the specified device.
    *** @param d  The device from which the ReminderType will be obtained.  
    ***           If null, the default ReminderType will be returned.
    *** @return The ReminderType [0=periodic, 1=single]
    **/
    public static ReminderType getReminderType(Device d)
    {
        return (d != null)? 
            EnumTools.getValueOf(ReminderType.class,d.getReminderType()) : 
            EnumTools.getDefault(ReminderType.class);
    }

    // ------------------------------------------------------------------------
    // FuelTankIndex

    /**
    *** FuelTankIndex
    **/
    public enum FuelTankIndex implements EnumTools.IntValue {
        TANK_1(0),
        TANK_2(1),
        TOTAL(-1);
        private int nn = -1;
        FuelTankIndex(int n)         { nn = n; }
        public int     getIntValue() { return nn; }
        public boolean isTank()      { return (nn >= 0)? true : false; }
    }
    
    /**
    *** Tank names
    **/
    private static final String NAME_TANK_1[]   = { "tank1", "tank-1", "tank_1" };
    private static final String NAME_TANK_2[]   = { "tank2", "tank-2", "tank_2" };
    private static final String NAME_TANK_ALL[] = { "total", "all"              };

    /**
    *** Gets the FuelTankIndex for the specified name
    **/
    public static FuelTankIndex GetFuelTankIndex(String name, FuelTankIndex dft)
    {
        String n = StringTools.trim(name).toLowerCase();

        /* default index */
        if (StringTools.isBlank(n)) {
            return dft;
        }

        /* tank #1 */
        if (ListTools.contains(NAME_TANK_1, n)) {
            return FuelTankIndex.TANK_1;
        }

        /* tank #2 */
        if (ListTools.contains(NAME_TANK_2, n)) {
            return FuelTankIndex.TANK_2;
        }

        /* all tanks (total/all) */
        if (ListTools.contains(NAME_TANK_ALL, n)) {
            return FuelTankIndex.TOTAL;
        }

        /* not found, return default */
        return dft;

    }

    // ------------------------------------------------------------------------
    // FuelEconomy type

    /**
    *** FuelEconomy Type enumeration
    **/
    public enum FuelEconomyType implements EnumTools.StringLocale, EnumTools.IntValue {
        UNKNOWN        ( 0, I18N.getString(Device.class,"Device.fuelEconomyType.notAvail","n/a"   ), I18N.getString(Device.class,"Device.fuelEconomyType.unknown"      ,"Unknown"       )),
        FUEL_CONSUMED  ( 1, I18N.getString(Device.class,"Device.fuelEconomyType.used"    ,"Used"  ), I18N.getString(Device.class,"Device.fuelEconomyType.fuelUsed"     ,"Fuel Used"     )),
        FUEL_REMAINING ( 2, I18N.getString(Device.class,"Device.fuelEconomyType.tank"    ,"Tank"  ), I18N.getString(Device.class,"Device.fuelEconomyType.tankRemaining","Tank Remaining")),
        FUEL_LEVEL     ( 3, I18N.getString(Device.class,"Device.fuelEconomyType.level"   ,"Level" ), I18N.getString(Device.class,"Device.fuelEconomyType.fuelLevel"    ,"Fuel Level"    )),
        EVENT_ECONOMY  ( 8, I18N.getString(Device.class,"Device.fuelEconomyType.event"   ,"Event" ), I18N.getString(Device.class,"Device.fuelEconomyType.eventEconomy" ,"Event Economy" )),
        DEVICE_ECONOMY ( 9, I18N.getString(Device.class,"Device.fuelEconomyType.device"  ,"Device"), I18N.getString(Device.class,"Device.fuelEconomyType.deviceEconomy","Device Economy"));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null; // abbreviation
        private I18N.Text   dd = null; // description
        FuelEconomyType(int v, I18N.Text a, I18N.Text d) { vv = v; aa = a; dd = d; }
        public int     getIntValue()                     { return vv; }
        public String  getAbbrev()                       { return aa.toString(); }
        public String  getAbbrev(Locale loc)             { return aa.toString(loc); }
        public String  toString()                        { return dd.toString(); }
        public String  toString(Locale loc)              { return dd.toString(loc); }
    }

    // ------------------------------------------------------------------------
    // Calculate Fuel Cost

    public static double CalculateFuelCost(Account a, Device d, double liters)
    {
        Account A = (d != null)? d.getAccount() : a;
        Device  D = d;

        /* account defined? */
        if (A == null) {
            return 0.0;
        }

        /* no Liters? */
        if (liters <= 0.0) {
            return 0.0;
        }

        /* get fuel cost per Liter */
        double costPerLiter = 0.0;
        if (D != null) {
            // -- try Device
            costPerLiter = D.getFuelCostPerLiter();
        }
        if (costPerLiter <= 0.0) {
            // -- try Account
            costPerLiter = A.getFuelCostPerLiter();
        }

        /* return cost */
        return (costPerLiter > 0.0)? (costPerLiter * liters) : 0.0;

    }

    // ------------------------------------------------------------------------
    // maximum reasonable odometer value for a vehicle

    // TODO: this should be device dependent
    public  static final boolean CHECK_LAST_ODOMETER        = false;

    /**
    *** Get configured maximum allowed odometer value
    *** @return Maximum configured allowed odometer value
    **/
    public static boolean GetCheckLastOdometer()
    {
        // TODO: optimize
        return RTConfig.getBoolean(DBConfig.PROP_Device_checkLastOdometer, CHECK_LAST_ODOMETER);
    }

    // ------------------------------------------------------------------------
    // (EXPERIMENTAL) Simulate Engine Hours from ignition state

    // TODO: this should be device dependent
    public  static      boolean SIMULATE_ENGINE_HOURS       = false;

    /**
    *** Get configured state of estimating engine hours based on ignition state
    *** @return Estimating engine hours based on ignition state
    **/
    public static boolean GetSimulateEngineHours(Device dev)
    {
        // TODO: optimize
        return RTConfig.getBoolean(DBConfig.PROP_Device_simulateEngineHours, SIMULATE_ENGINE_HOURS);
    }

    // ------------------------------------------------------------------------
    // (EXPERIMENTAL) Simulate PTO Hours from PTO on/off state

    // TODO: this should be device dependent
    public  static      boolean SIMULATE_PTO_HOURS          = true;

    /**
    *** Get configured state of estimating PTO hours based on PTO on/off state
    *** @return Estimating PTO hours based on PTO on/off state
    **/
    public static boolean GetSimulatePtoHours(Device dev)
    {
        // TODO: optimize
        return RTConfig.getBoolean(DBConfig.PROP_Device_simulatePtoHours, SIMULATE_PTO_HOURS);
    }

    // ------------------------------------------------------------------------
    // (EXPERIMENTAL) Update Event loc if invalid and it has a valid GeozoneID

    // TODO: this should be device dependent
    public  static      boolean UPDATE_EVENT_WITH_GEOZONE_LOC   = false;

    /**
    *** Get configured state obtaining lat/lon from Geozone
    *** @return Obtain lat/lon from Geozone
    **/
    public static boolean UpdateEventWithGeozoneLocation()
    {
        // TODO: optimize
        return RTConfig.getBoolean(DBConfig.PROP_Device_updateEventWithGeozoneLoc, UPDATE_EVENT_WITH_GEOZONE_LOC);
    }

    // ------------------------------------------------------------------------
    // maximum reasonable odometer value for a vehicle

    // TODO: this should be device dependent
    public  static final double MAX_DEVICE_ODOM_KM          = 1000000.0 * GeoPoint.KILOMETERS_PER_MILE; // 1609344
    
    /**
    *** Get configured maximum allowed odometer value
    *** @return Maximum configured allowed odometer value
    **/
    public static double GetMaximumOdometerKM()
    {
        // TODO: optimize
        return RTConfig.getDouble(DBConfig.PROP_Device_maximumOdometerKM, MAX_DEVICE_ODOM_KM);
    }

    // ------------------------------------------------------------------------
    // maximum reasonable odometer value for a vehicle

    // TODO: this should be device dependent
    private static final double MAX_DEVICE_RUNTIME_HOURS    = DateTime.DaySeconds(365*30)/3600.0;
    
    /**
    *** Get configured maximum allowed engine-hours value
    *** @return Maximum configured allowed engine-hours value
    **/
    public static double GetMaximumRuntimeHours()
    {
        return RTConfig.getDouble(DBConfig.PROP_Device_maximumRuntimeHours, MAX_DEVICE_RUNTIME_HOURS);
    }

    // ------------------------------------------------------------------------
    // check notify rule selector
    
    /* check device rule selector */
    private static final boolean CHECK_NOTIFY_SELECTOR      = true;

    /**
    *** True to test notify rule selector, false to ignore
    *** @return True to test notify rule selector, false to ignore
    **/
    public static boolean CheckNotifySelector()
    {
        if (!Device.hasRuleFactory()) {
            // no rule factory, do not check selector
            return false;
        } else
        if (!RTConfig.getBoolean(DBConfig.PROP_Device_checkNotifySelector,CHECK_NOTIFY_SELECTOR)) {
            // explicit false
            return false;
        } else
        if (Device.hasENRE()) {
            // check ENRE specific setting
            return RTConfig.getBoolean(DBConfig.PROP_Device_checkNotifySelector_ENRE,false);
        } else {
            // true
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // save DriverID from EventData record into Device and Driver record
    // -- TODO: this should be device dependent

    public  static final    int    SaveDriverID_NEVER      = 0; // "never" | "false"
    public  static final    int    SaveDriverID_NONBLANK   = 1; // "nonblank" | "true"
    public  static final    int    SaveDriverID_ALWAYS     = 2; // "always"
    public  static final    int    SaveDriverID_DEFAULT    = SaveDriverID_NONBLANK;

    public  static volatile int    SAVE_EVENT_DRIVER_ID    = -1;

    /**
    *** Returns true if configured to save the EventData "driverID" into the Device record
    *** @return True to save EventData "driverID", false otherwise.
    **/
    private static int GetSaveEventDriverID()
    {
        // -- TODO: optimize
        //return RTConfig.getBoolean(DBConfig.PROP_Device_saveEventDriverID, SAVE_EVENT_DRIVER_ID);
        if (SAVE_EVENT_DRIVER_ID <= -1) {
            synchronized (DBConfig.PROP_Device_saveEventDriverID) {
                if (SAVE_EVENT_DRIVER_ID <= -1) {
                    String v = RTConfig.getString(DBConfig.PROP_Device_saveEventDriverID,""); // never|nonblank|always|true|false
                    if (StringTools.isBlank(v)) {
                        SAVE_EVENT_DRIVER_ID = SaveDriverID_DEFAULT;
                    } else
                    if (v.equals("never")) {
                        // -- never save device-id
                        SAVE_EVENT_DRIVER_ID = SaveDriverID_NEVER;
                    } else
                    if (v.equals("nonblank")) {
                        // -- only save device-id if non-blank
                        SAVE_EVENT_DRIVER_ID = SaveDriverID_NONBLANK;
                    } else
                    if (v.equals("always")) {
                        // -- always save device-id
                        SAVE_EVENT_DRIVER_ID = SaveDriverID_ALWAYS;
                    } else
                    if (StringTools.isBoolean(v,false)) {
                        // -- check for true|false
                        boolean b = StringTools.parseBoolean(v,true);
                        SAVE_EVENT_DRIVER_ID = b? SaveDriverID_NONBLANK : SaveDriverID_NEVER;
                    } else {
                        // -- return default
                        SAVE_EVENT_DRIVER_ID = SaveDriverID_DEFAULT;
                    }
                }
            }
        }
        return SAVE_EVENT_DRIVER_ID;
    }

    // ------------------------------------------------------------------------
    // CellTower Location API

    private static boolean                cellTower_initDefault = false;
    private static MobileLocationProvider cellTower_GetLocation = null;

    /** OBSOLETE
    *** Sets the MobileLocationProvider
    *** @param ctgl  The MobileLocationProvider
    **/
    private static void setCellTowerGetLocation(MobileLocationProvider ctgl)
    {
        Device.cellTower_initDefault = true;
        if (ctgl != null) {
            Device.cellTower_GetLocation = ctgl;
            Print.logDebug("Device CellTower.GetLocation installed: " + StringTools.className(ctgl));
        } else
        if (Device.cellTower_GetLocation != null) {
            Device.cellTower_GetLocation = null;
            Print.logDebug("Device CellTower.GetLocation removed.");
        }
    }

    /** OBSOLETE
    *** Returns true if a MobileLocationProvider is defined
    *** @return True if a MobileLocationProvider is defined
    **/
    private static boolean hasCellTowerGetLocation()
    {
        return (Device.cellTower_GetLocation != null);
    }

    /** OBSOLETE
    *** Gets the MobileLocationProvider
    *** @return  The MobileLocationProvider
    **/
    private static MobileLocationProvider getMobileLocationProvider()
    {
        if (!Device.cellTower_initDefault) {
            Device.cellTower_initDefault = true;
            if (Device.cellTower_GetLocation == null) {
                Device.cellTower_GetLocation = null; // CellTower.GetDefaultCellTowerLocationInterface(); 
                // may still be null
            }
        }
        return Device.cellTower_GetLocation;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) Rule factory

    private static RuleFactory ruleFactory = null;

    /**
    *** Sets the RuleFactory
    *** @param rf  The RuleFactory
    **/
    public static void setRuleFactory(RuleFactory rf)
    {
        // -- ENRE: called by Rule.getFactory() ==> Rule.initRuleFactory()
        if (rf != null) {
            Device.ruleFactory = rf;
            Print.logDebug("Device RuleFactory installed: " + StringTools.className(rf));
        } else
        if (Device.ruleFactory != null) {
            Device.ruleFactory = null;
            Print.logDebug("Device RuleFactory removed.");
        }
    }

    /**
    *** Returns true if a RuleFactory is defined
    *** @return True if a RuleFactory is defined
    **/
    public static boolean hasRuleFactory()
    {
        return (Device.ruleFactory != null);
    }

    /**
    *** Returns true if the defined RuleFactory is the ENRE
    *** @return True if the defined RuleFactory is the ENRE
    **/
    public static boolean hasENRE()
    {
        if (Device.ruleFactory != null) {
            //return Device.ruleFactory.getName().equals("GTSRulesEngine");
            //return DBConfig.hasRulePackage();
            return OSTools.instanceOf(Device.ruleFactory, DBConfig.CLASS_RULE_EventRuleFactory);
        } else {
            return false;
        }
    }

    /**
    *** Gets the RuleFactory
    *** @return  The RuleFactory
    **/
    public static RuleFactory getRuleFactory()
    {
        return Device.ruleFactory;
    }

    /**
    *** Gets the RuleFactory
    *** @param checkRuntime  True to peform the RuleFactory runtime validation
    *** @return  The RuleFactory
    **/
    public static RuleFactory getRuleFactory(boolean checkRuntime)
    {
        if (checkRuntime && (Device.ruleFactory != null)) {
            return Device.ruleFactory.checkRuntime()? Device.ruleFactory : null;
        } else {
            return Device.ruleFactory;
        }
    }

    // ------------------------------------------------------------------------
    // (Device) Session statistics 

    private static SessionStatsFactory statsFactory = null;

    /**
    *** Sets the SessionStatsFactory
    *** @param rf  The SessionStatsFactory
    **/
    public static void setSessionStatsFactory(SessionStatsFactory rf)
    {
        if (rf != null) {
            Device.statsFactory = rf;
            Print.logDebug("Device SessionStatsFactory installed: " + StringTools.className(Device.statsFactory));
        } else
        if (Device.statsFactory != null) {
            Device.statsFactory = null;
            Print.logDebug("Device SessionStatsFactory removed.");
        }
    }

    /**
    *** Returns true if a SessionStatsFactory has been defined
    *** @return True if a SessionStatsFactory has been defined
    **/
    public static boolean hasSessionStatsFactory()
    {
        return (Device.statsFactory != null);
    }

    /**
    *** Gets the SessionStatsFactory
    *** @return  The SessionStatsFactory
    **/
    public static SessionStatsFactory getSessionStatsFactory()
    {
        return Device.statsFactory;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) Entity manager

    private static EntityManager entityManager = null;

    /**
    *** Sets the EntityManager
    *** @param ef  The EntityManager
    **/
    public static void setEntityManager(EntityManager ef)
    {
        // -- called by Entity.getFactory() ==> Entity.initEntityManager()
        if (ef != null) {
            Device.entityManager = ef;
            //Print.logDebug("Device EntityManager installed: " + StringTools.className(Device.entityManager));
        } else
        if (Device.entityManager != null) {
            Device.entityManager = null;
            //Print.logDebug("Device EntityManager removed.");
        }
    }

    /** 
    *** Returns true if an EntityManager has been defined
    *** @return True if an EntityManager has been defined
    **/
    public static boolean hasEntityManager()
    {
        return (Device.entityManager != null);
    }

    /**
    *** Gets the defined EntityManager
    *** @return The defined EntityManager
    **/
    public static EntityManager getEntityManager()
    {
        return Device.entityManager;
    }

    // --------------------------------

    /**
    *** Gets the Description for the specified Entity ID
    *** @param accountID  The Account ID
    *** @param entityID   The Entity ID
    *** @param etype      The Entity type
    *** @return The Entity Description
    **/
    public static String getEntityDescription(String accountID, String entityID, EntityManager.EntityType etype)
    {
        EntityManager.EntityType et = EntityManager.getEntityType(etype);
        return Device.getEntityDescription(accountID, entityID, et.getIntValue());
    }

    /**
    *** Gets the Description for the specified Entity ID
    *** @param accountID  The Account ID
    *** @param entityID   The Entity ID
    *** @param etype      The Entity type
    *** @return The Entity Description
    **/
    public static String getEntityDescription(String accountID, String entityID, int etype)
    {
        String eid = StringTools.trim(entityID);
        if (!eid.equals("") && Device.hasEntityManager()) {
            eid = Device.getEntityManager().getEntityDescription(accountID, eid, etype);
        }
        return eid;
    }

    // --------------------------------

    /**
    *** Returns true if the specified Entity is attached to the specified Device ID
    *** @param accountID  The Account ID
    *** @param deviceID   The Device ID
    *** @param entityID   The Entity ID
    *** @param etype      The Entity type
    *** @return True if the Entity is attached to the device
    **/
    public static boolean isEntityAttached(String accountID, String deviceID, String entityID, EntityManager.EntityType etype)
    {
        EntityManager.EntityType et = EntityManager.getEntityType(etype);
        return Device.isEntityAttached(accountID, deviceID, entityID, et.getIntValue());
    }

    /**
    *** Returns true if the specified Entity is attached to the specified Device ID
    *** @param accountID  The Account ID
    *** @param deviceID   The Device ID
    *** @param entityID   The Entity ID
    *** @param etype      The Entity type
    *** @return True if the Entity is attached to the device
    **/
    public static boolean isEntityAttached(String accountID, String deviceID, String entityID, int etype)
    {
        String eid = StringTools.trim(entityID);
        if (!eid.equals("") && Device.hasEntityManager()) {
            return Device.getEntityManager().isEntityAttached(accountID, deviceID, eid, etype);
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // (Vehicle) Fuel manager

    private static FuelManager fuelManager = null;

    /**
    *** Sets the FuelManager
    *** @param fm  The FuelManager
    **/
    public static void setFuelManager(FuelManager fm)
    {
        // -- called by FuelRegister.getFactory() ==> FuelRegister.initFuelManager()
        if (fm != null) {
            Device.fuelManager = fm;
            //Print.logDebug("Device FuelManager installed: " + StringTools.className(Device.fuelManager));
        } else
        if (Device.fuelManager != null) {
            Device.fuelManager = null;
            //Print.logDebug("Device FuelManager removed.");
        }
    }

    /**
    *** Returns true if a FuelManager has been defined
    *** @return True if a FuelManager has been defined
    **/
    public static boolean hasFuelManager()
    {
        return (Device.fuelManager != null);
    }

    /**
    *** Gets the FuelManager
    *** @return  The FuelManager
    **/
    public static FuelManager getFuelManager()
    {
        return Device.fuelManager;
    }

    // ------------------------------------------------------------------------
    // (Vehicle) "Ping" dispatcher

    private static PingDispatcher pingDispatcher = null;

    /**
    *** Sets the PingDispatcher
    *** @param pd  The PingDispatcher
    **/
    public static void setPingDispatcher(PingDispatcher pd)
    {
        if (pd != null) {
            Device.pingDispatcher = pd;
            Print.logDebug("Device PingDispatcher installed: " + StringTools.className(Device.pingDispatcher));
        } else
        if (Device.pingDispatcher != null) {
            Device.pingDispatcher = null;
            Print.logDebug("Device PingDispatcher removed.");
        }
    }

    /**
    *** Returns true if a PingDispatcher has been defined
    *** @return True if a PingDispatcher has been defined
    **/
    public static boolean hasPingDispatcher()
    {
        return (Device.pingDispatcher != null);
    }

    /**
    *** Gets the PingDispatcher
    *** @return  The PingDispatcher
    **/
    public static PingDispatcher getPingDispatcher()
    {
        return Device.pingDispatcher;
    }

    // ------------------------------------------------------------------------
    // Past EventDate timestamp check

    public static final int PAST_DATE_UNDEFINED     = -999;
    public static final int PAST_DATE_IGNORE        = -1;
    public static final int PAST_DATE_DISABLED      = 0;
    public static final int PAST_DATE_TRUNCATE      = 1;

    private static int  PastEventDateAction = PAST_DATE_UNDEFINED;
    /**
    *** Gets the action to perform when a past event date is detected
    *** @return The action to perform when a past event date is detected
    **/
    public static int pastEventDateAction()
    {
        if (PastEventDateAction == PAST_DATE_UNDEFINED) {
            // TODO: synchronize?
            // "Device.pastDate.action="
            String act = RTConfig.getString(DBConfig.PROP_Device_pastDate_action,"");
            if (act.equalsIgnoreCase("ignore")   ||
                act.equalsIgnoreCase("skip")     ||
                act.equalsIgnoreCase("discard")  ||  // 2.6.0-B25
                act.equalsIgnoreCase("-1")         ) {
                PastEventDateAction = PAST_DATE_IGNORE;
            } else
            if (act.equalsIgnoreCase("truncate") ||
                act.equalsIgnoreCase("current")  ||  // 2.5.3-B10
                act.equalsIgnoreCase("1")          ) {
                PastEventDateAction = PAST_DATE_TRUNCATE;
            } else
            if (StringTools.isBlank(act)         ||
                act.equalsIgnoreCase("disabled") ||
                act.equalsIgnoreCase("disable")  ||
                act.equalsIgnoreCase("0")          ) {
                PastEventDateAction = PAST_DATE_DISABLED;
            } else {
                Print.logError("Invalid property value %s => %s", DBConfig.PROP_Device_pastDate_action, act);
                PastEventDateAction = PAST_DATE_DISABLED;
            }
        }
        return PastEventDateAction;
    }

    private static long PastEventDateMaxSec = -999L;
    /**
    *** Gets the maximum number of seconds an event is allowed to be into the past
    *** @return The maximum number of seconds into the past
    **/
    public static long pastEventDateMaximumSec()
    {
        if (PastEventDateMaxSec == -999L) {
            // TODO: synchronize?
            String M = RTConfig.getString(DBConfig.PROP_Device_pastDate_maximumSec,"");
            if (!StringTools.isBlank(M)) {
                long S = StringTools.parseLong(M,0L);
                if (StringTools.endsWithIgnoreCase(M,"d")) {
                    S = DateTime.DaySeconds(S);
                }
                PastEventDateMaxSec = Math.abs(S);
            } else {
                PastEventDateMaxSec = 0L;
            }
        }
        return PastEventDateMaxSec;
    }

    // ------------------------------------------------------------------------
    // Future EventDate timestamp check

    public static final int FUTURE_DATE_UNDEFINED   = -999;
    public static final int FUTURE_DATE_IGNORE      = -1;
    public static final int FUTURE_DATE_DISABLED    = 0;
    public static final int FUTURE_DATE_TRUNCATE    = 1;

    private static int  FutureEventDateAction = FUTURE_DATE_UNDEFINED;
    /**
    *** Gets the action to perform when a future event date is detected
    *** @return The action to perform when a future event date is detected
    **/
    public static int futureEventDateAction()
    {
        if (FutureEventDateAction == FUTURE_DATE_UNDEFINED) {
            // TODO: synchronize?
            // "Device.futureDate.action="
            String act = RTConfig.getString(DBConfig.PROP_Device_futureDate_action,"");
            if (act.equalsIgnoreCase("ignore")   ||
                act.equalsIgnoreCase("skip")     ||
                act.equalsIgnoreCase("discard")  ||  // 2.6.0-B25
                act.equalsIgnoreCase("-1")         ) {
                FutureEventDateAction = FUTURE_DATE_IGNORE;
            } else
            if (act.equalsIgnoreCase("truncate") ||
                act.equalsIgnoreCase("current")  ||  // 2.5.3-B10
                act.equalsIgnoreCase("1")          ) {
                FutureEventDateAction = FUTURE_DATE_TRUNCATE;
            } else
            if (StringTools.isBlank(act)         ||
                act.equalsIgnoreCase("disabled") ||
                act.equalsIgnoreCase("disable")  ||
                act.equalsIgnoreCase("0")          ) {
                FutureEventDateAction = FUTURE_DATE_DISABLED;
            } else {
                Print.logError("Invalid property value %s => %s", DBConfig.PROP_Device_futureDate_action, act);
                FutureEventDateAction = FUTURE_DATE_DISABLED;
            }
        }
        return FutureEventDateAction;
    }

    private static long FutureEventDateMaxSec = -999L;
    /**
    *** Gets the maximum number of seconds an event is allowed to be into the future
    *** @return The maximum number of seconds into the future
    **/
    public static long futureEventDateMaximumSec()
    {
        if (FutureEventDateMaxSec == -999L) {
            // TODO: synchronize?
            String M = RTConfig.getString(DBConfig.PROP_Device_futureDate_maximumSec,"");
            if (!StringTools.isBlank(M)) {
                long S = StringTools.parseLong(M,0L);
                if (StringTools.endsWithIgnoreCase(M,"d")) {
                    S = DateTime.DaySeconds(S);
                }
                FutureEventDateMaxSec = Math.abs(S);
            } else {
                FutureEventDateMaxSec = 0L;
            }
        }
        return FutureEventDateMaxSec;
    }

    // ------------------------------------------------------------------------
    // Invalid speed check

    public static final int INVALID_SPEED_UNDEFINED   = -999;
    public static final int INVALID_SPEED_IGNORE      = -1;
    public static final int INVALID_SPEED_DISABLED    = 0;
    public static final int INVALID_SPEED_TRUNCATE    = 1;
    public static final int INVALID_SPEED_ZERO        = 2;
    public static final int INVALID_SPEED_IGNORE_LOC  = 3;

    private static int  InvalidSpeedAction = INVALID_SPEED_UNDEFINED;
    public static int invalidSpeedAction()
    {
        // TODO: synchronize?
        if (InvalidSpeedAction == INVALID_SPEED_UNDEFINED) {
            // "Device.invalidSpeed.action="
            String act = RTConfig.getString(DBConfig.PROP_Device_invalidSpeed_action,"");
            if (act.equalsIgnoreCase("ignore")    ||
                act.equalsIgnoreCase("skip")      ||
                act.equalsIgnoreCase("-1")          ) {
                // -- events with invalid speed will be ignored
                // -- WARNING: this could cause events with important status codes to be ignored.
                InvalidSpeedAction = INVALID_SPEED_IGNORE;
            } else
            if (act.equalsIgnoreCase("ignoreLoc") ||
                act.equalsIgnoreCase("3")           ) {
                // -- events with invalid speed will be ignored (only if status code is "Location")
                InvalidSpeedAction = INVALID_SPEED_IGNORE_LOC;
            } else
            if (act.equalsIgnoreCase("truncate")  ||
                act.equalsIgnoreCase("max")       ||  // 2.5.3-B10
                act.equalsIgnoreCase("1")           ) {
                // -- event speeds exceeding the max allowed speed will be set to the max allowed speed
                InvalidSpeedAction = INVALID_SPEED_TRUNCATE;
            } else
            if (act.equalsIgnoreCase("zero")      ||
                act.equalsIgnoreCase("setzero")   ||
                act.equalsIgnoreCase("2")           ) {
                // -- event speeds exceeding the max allowed speed will be set to zero speed
                InvalidSpeedAction = INVALID_SPEED_ZERO;
            } else
            if (StringTools.isBlank(act)          ||
                act.equalsIgnoreCase("blank")     || // <-- in case someone took "blank" literally
                act.equalsIgnoreCase("disabled")  ||
                act.equalsIgnoreCase("disable")   ||
                act.equalsIgnoreCase("0")           ) {
                // -- maximum allowed speed will not be checked
                InvalidSpeedAction = INVALID_SPEED_DISABLED;
            } else {
                Print.logError("Invalid property value %s => %s", DBConfig.PROP_Device_invalidSpeed_action, act);
                InvalidSpeedAction = INVALID_SPEED_DISABLED;
            }
        }
        return InvalidSpeedAction;
    }

    private static double InvalidSpeedMaxKPH = -999.0;
    public static double invalidSpeedMaximumKPH()
    {
        // TODO: synchronize?
        if (InvalidSpeedMaxKPH <= -999.0) {
            String spdMaxProp = DBConfig.PROP_Device_invalidSpeed_maximumKPH;
            InvalidSpeedMaxKPH = RTConfig.getDouble(spdMaxProp, 0.0);
            if (InvalidSpeedMaxKPH <= 0.0) {
                // essentially "disabled"
                InvalidSpeedMaxKPH = 0.0;
            } else
            if (InvalidSpeedMaxKPH <= 100.0) {
                // a low maximum speed warning
                Print.logWarn("**** \""+spdMaxProp+"\" set to " + InvalidSpeedMaxKPH + " km/h ****");
            }
        }
        return InvalidSpeedMaxKPH;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* keyed FLD_simPhoneNumber? */
    private static String _simPhoneNumber_attr()
    {
        // FLD_simPhoneNumber
        String commonAttr = "edit=2";
        if (RTConfig.getBoolean(DBConfig.PROP_Device_keyedSimPhoneNumber,false)) {
            return commonAttr + " altkey=simphone";
        } else {
            return commonAttr;
        }
    }

    /* keyed FLD_lastNotifyTime? */
    private static String _lastNotifyTime_attr()
    {
        // FLD_lastNotifyTime
        String commonAttr = "format=time";
        if (RTConfig.getBoolean(DBConfig.PROP_Device_keyedLastNotifyTime,false)) {
            return commonAttr + " altkey=notifyTime";
        } else {
            return commonAttr;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below
    // Note: The following fields should be updated upon each connection from the client device:
    //  - FLD_lastInputState
    //  - FLD_ipAddressCurrent
    //  - FLD_remotePortCurrent
    //  - FLD_lastTotalConnectTime
    //  - FLD_lastDuplexConnectTime (OpenDMTP clients, otherwise optional)
    //  - FLD_totalProfileMask (OpenDMTP clients)
    //  - FLD_duplexProfileMask (OpenDMTP clients)
    //  - etc ... (see "DefaultUpdatedFieldsList")

    /* table name */
    public static final String _TABLE_NAME               = "Device"; // "Asset"
    public static String TABLE_NAME() { return DBProvider._preTranslateTableName(_TABLE_NAME); }

    /* field definition */
    // -- Device/Asset specific information:
    public static final String FLD_groupID               = "groupID";               // vehicle group (user informational only)
    public static final String FLD_equipmentType         = "equipmentType";         // equipment/vehicle type
    public static final String FLD_equipmentStatus       = "equipmentStatus";       // equipment/vehicle status (InService, InRepair, Available, etc)
    public static final String FLD_vehicleMake           = "vehicleMake";           // vehicle make (ie "Toyota")
    public static final String FLD_vehicleModel          = "vehicleModel";          // vehicle model (ie. "L150F")
    public static final String FLD_vehicleColor          = "vehicleColor";          // vehicle color (ie. "Green")
    public static final String FLD_vehicleYear           = "vehicleYear";           // vehicle year (ie. "2010")
    public static final String FLD_vehicleID             = "vehicleID";             // vehicle id number (ie VIN)
    public static final String FLD_licensePlate          = "licensePlate";          // licensePlate / registration id
    public static final String FLD_licenseExpire         = "licenseExpire";         // licensePlate / registration expiration date
    public static final String FLD_insuranceExpire       = "insuranceExpire";       // insurance expiration date
    public static final String FLD_driverID              = "driverID";              // driver id
    public static final String FLD_driverStatus          = "driverStatus";          // driver status
    public static final String FLD_fuelCapacity          = "fuelCapacity";          // fuel capacity liters
    public static final String FLD_fuelCapacity2         = "fuelCapacity2";         // fuel capacity liters (tank #2)
    public static final String FLD_fuelEconomy           = "fuelEconomy";           // approximate fuel economy km/L
    public static final String FLD_fuelRatePerHour       = "fuelRatePerHour";       // approximate L/h
    public static final String FLD_fuelCostPerLiter      = "fuelCostPerLiter";      // fuel cost per liter
    public static final String FLD_fuelTankProfile       = "fuelTankProfile";       // fuel tank profile ID
    public static final String FLD_fuelTankProfile2      = "fuelTankProfile2";      // fuel tank profile ID (tank #2)
  //public static final String FLD_fuelProperties        = "fuelProperties";        // fuel misc properties not covered elsewhere (ie. "type", "coeff[.0]"
  //public static final String FLD_fuelType              = "fuelType";              // fuel type
  //public static final String FLD_fuelCO2PerLiter       = "fuelCO2PerLiter";       // CO2 emissions per Liter (kg)
    public static final String FLD_speedLimitKPH         = "speedLimitKPH";         // Maximum speed km/h
    public static final String FLD_maximumRpm            = "maximumRpm";            // Maximum RPM
    public static final String FLD_planDistanceKM        = "planDistanceKM";        // Planned trip distance traveled
    public static final String FLD_installTime           = "installTime";           // install time (date/time when device was installed)
    public static final String FLD_resetTime             = "resetTime";             // reset time (date/time when device was reset - typically odometer, fuel, etc)
    public static final String FLD_expirationTime        = "expirationTime";        // expiration time
    // -- DataTransport specific attributes (see also Transport.java)
    // -  (These fields contain the default DataTransport attributes)
    public static final String FLD_uniqueID              = "uniqueID";              // unique device ID
    public static final String FLD_deviceCode            = "deviceCode";            // DCServerConfig ID ("serverID")
    public static final String FLD_deviceType            = "deviceType";            // reserved
    public static final String FLD_pushpinID             = "pushpinID";             // map pushpin ID
    public static final String FLD_displayColor          = "displayColor";          // display color (maps, reports, etc).
    public static final String FLD_serialNumber          = "serialNumber";          // device hardware serial#.
    public static final String FLD_simPhoneNumber        = "simPhoneNumber";        // SIM phone number
    public static final String FLD_simID                 = "simID";                 // SIM ID
    public static final String FLD_smsEmail              = "smsEmail";              // SMS email address (to the device itself)
  //public static final String FLD_smsGatewayProps       = "smsGatewayProps";       // SMS gateway properties
    public static final String FLD_imeiNumber            = "imeiNumber";            // IMEI number (or moblie ID)
    public static final String FLD_dataKey               = "dataKey";               // Data key (IE. Device PIN number)
    public static final String FLD_ignitionIndex         = "ignitionIndex";         // hardware ignition I/O index
    public static final String FLD_codeVersion           = "codeVersion";           // code version installed on device
    public static final String FLD_featureSet            = "featureSet";            // device features
    public static final String FLD_ipAddressValid        = "ipAddressValid";        // valid IP address block
    // -- Last Device IP Address:Port
    public static final String FLD_fixedTcpSessionID     = "fixedTcpSessionID";     // fixed TCP session ID
    public static final String FLD_lastTcpSessionID      = "lastTcpSessionID";      // last TCP session ID
    public static final String FLD_ipAddressLocal        = "ipAddressLocal";        // local IP address (to which device sent packet)
    public static final String FLD_ipAddressCurrent      = "ipAddressCurrent";      // current(last) IP address
    public static final String FLD_remotePortCurrent     = "remotePortCurrent";     // current(last) remote port
    public static final String FLD_listenPortCurrent     = "listenPortCurrent";     // current(last) local/listen port
    // -- Ping/Command
    public static final String FLD_pingCommandURI        = "pingCommandURI";        // ping command URL
    public static final String FLD_pendingPingCommand    = "pendingPingCommand";    // pending ping command (should just be 'pendingCommand')
    public static final String FLD_lastPingTime          = "lastPingTime";          // last ping time
    public static final String FLD_totalPingCount        = "totalPingCount";        // total ping count
    public static final String FLD_maxPingCount          = "maxPingCount";          // maximum allowed ping count
    public static final String FLD_commandStateMask      = "commandStateMask";      // command state mask (set by command sent)
    public static final String FLD_expectAck             = "expectAck";             // expecting a returned ACK
    public static final String FLD_expectAckCode         = "expectAckCode";         // expected ACK status code
    public static final String FLD_lastAckCommand        = "lastAckCommand";        // last command expecting an ACK
    public static final String FLD_lastAckResponse       = "lastAckResponse";       // last command response
    public static final String FLD_lastAckTime           = "lastAckTime";           // last received ACK time
    // -- Device Communication Server Configuration
    public static final String FLD_dcsPropertiesID       = "dcsPropertiesID";       // DCS property group name
    public static final String FLD_dcsConfigMask         = "dcsConfigMask";         // DCS Config Mask
    public static final String FLD_dcsConfigString       = "dcsConfigString";       // DCS Config String
    public static final String FLD_dcsCommandHost        = "dcsCommandHost";        // DCS Command host name
    public static final String FLD_dcsCommandState       = "dcsCommandState";       // DCS Command State
    // -- Last Event values
    // -    lastAddress
    // -    lastSpeedKPH   ==> FLD_lastValidSpeedKPH
    // -    lastStatusCode ==> FLD_lastEventStatusCode
    // -    lastDriverID   ==> FLD_driverID
    public static final String FLD_lastTotalConnectTime  = "lastTotalConnectTime";  // last connect time
    public static final String FLD_lastDuplexConnectTime = "lastDuplexConnectTime"; // last TCP connect time
    public static final String FLD_lastInputState        = "lastInputState";        // last known digital input state (GPIO/inputMask)
    public static final String FLD_lastOutputState       = "lastOutputState";       // last known digital output state (GPIO/outputMask)
    public static final String FLD_statusCodeState       = "statusCodeState";       // selected statusCode on/off states
    public static final String FLD_lastBatteryLevel      = "lastBatteryLevel";      // last known internal battery level (%)
    public static final String FLD_lastBatteryVolts      = "lastBatteryVolts";      // last known internal battery volts
    public static final String FLD_lastVBatteryVolts     = "lastVBatteryVolts";     // last known vehicle battery volts
    public static final String FLD_lastFuelLevel         = "lastFuelLevel";         // last fuelLevel value #1
    public static final String FLD_lastFuelLevel2        = "lastFuelLevel2";        // last fuelLevel value #2
    public static final String FLD_lastFuelTotal         = "lastFuelTotal";         // last fuelTotal value
    public static final String FLD_lastOilLevel          = "lastOilLevel";          // last oilLevel value
    public static final String FLD_lastValidLatitude     = "lastValidLatitude";     // last known valid latitude
    public static final String FLD_lastValidLongitude    = "lastValidLongitude";    // last known valid longitude
    public static final String FLD_lastValidHeading      = "lastValidHeading";      // last known valid heading
    public static final String FLD_lastValidSpeedKPH     = "lastValidSpeedKPH";     // last known valid speed
    public static final String FLD_lastGPSTimestamp      = "lastGPSTimestamp";      // timestamp of last valid GPS Location
    public static final String FLD_lastEventTimestamp    = "lastEventTimestamp";    // timestamp of last event
    public static final String FLD_lastEventStatusCode   = "lastEventStatusCode";   // statusCode of last event
    public static final String FLD_lastCellServingInfo   = "lastCellServingInfo";   // last Serving CellTower info
    public static final String FLD_lastDistanceKM        = "lastDistanceKM";        // last distance value (may be simulated)
    public static final String FLD_lastOdometerKM        = "lastOdometerKM";        // last odometer value (may be simulated)
    public static final String FLD_odometerOffsetKM      = "odometerOffsetKM";      // offset to reported odometer
    public static final String FLD_lastEngineOnHours     = "lastEngineOnHours";     // engine-hours at last engine-on
    public static final String FLD_lastEngineOnTime      = "lastEngineOnTime";      // last engine-on time (may be '0' if engine is off)
    public static final String FLD_lastEngineOffTime     = "lastEngineOffTime";     // last engine-off time (may be '0' if engine is on)
    public static final String FLD_lastEngineHours       = "lastEngineHours";       // last engine-hours value (may be simulated)
    public static final String FLD_engineHoursOffset     = "engineHoursOffset";     // offset to reported engine hours
    public static final String FLD_lastIgnitionOnHours   = "lastIgnitionOnHours";   // ignition-hours at last ignition-on
    public static final String FLD_lastIgnitionOnTime    = "lastIgnitionOnTime";    // last ignition-on time (may be '0' if ignition is off)
    public static final String FLD_lastIgnitionOffTime   = "lastIgnitionOffTime";   // last ignition-off time (may be '0' if ignition is on)
    public static final String FLD_lastIgnitionHours     = "lastIgnitionHours";     // last ignition hours at time of last ignition-on
    public static final String FLD_lastStopTime          = "lastStopTime";          // last Stop time ('0' if not stopped)
    public static final String FLD_lastStartTime         = "lastStartTime";         // last Start time ('0' if stopped)
    public static final String FLD_lastMalfunctionLamp   = "lastMalfunctionLamp";   // last MIL state
    public static final String FLD_lastFaultCode         = "lastFaultCode";         // last fault code properties
    public static final String FLD_lastPtoOnHours        = "lastPtoOnHours";        // pto-hours at last pto-on
    public static final String FLD_lastPtoOnTime         = "lastPtoOnTime";         // last PTO-on time (may be '0' if PTO is off)
    public static final String FLD_lastPtoOffTime        = "lastPtoOffTime";        // last PTO-off time (may be '0' if PTO is on)
    public static final String FLD_lastPtoHours          = "lastPtoHours";          // last PTO-hours value (may be simulated)
    //
    private static DBField FieldInfo[] = {
        // -- Asset/Vehicle specific fields
        newField_accountID(true),
        newField_deviceID(true),
        new DBField(FLD_groupID              , String.class        , DBField.TYPE_GROUP_ID()  , I18N.getString(Device.class,"Device.fld.groupID"              , "Group ID"                    ), "edit=2"),
        new DBField(FLD_equipmentType        , String.class        , DBField.TYPE_STRING(40)  , I18N.getString(Device.class,"Device.fld.equipmentType"        , "Equipment Type"              ), "edit=2"),
        new DBField(FLD_equipmentStatus      , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.equipmentStatus"      , "Equipment Status"            ), "edit=2"),
        new DBField(FLD_vehicleMake          , String.class        , DBField.TYPE_STRING(40)  , I18N.getString(Device.class,"Device.fld.vehicleMake"          , "Vehicle Make"                ), "edit=2"),
        new DBField(FLD_vehicleModel         , String.class        , DBField.TYPE_STRING(40)  , I18N.getString(Device.class,"Device.fld.vehicleModel"         , "Vehicle Model"               ), "edit=2"),
        new DBField(FLD_vehicleColor         , String.class        , DBField.TYPE_COLOR()     , I18N.getString(Device.class,"Device.fld.vehicleColor"         , "Vehicle Color"               ), "edit=2"),
        new DBField(FLD_vehicleYear          , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.vehicleYear"          , "Vehicle Year"                ), "edit=2"),
        new DBField(FLD_vehicleID            , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.vehicleID"            , "VIN"                         ), "edit=2"),
        new DBField(FLD_licensePlate         , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.licensePlate"         , "License Plate"               ), "edit=2"),
        new DBField(FLD_licenseExpire        , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.licenseExpire"        , "License Expiration Day"      ), "edit=2 format=date"),
        new DBField(FLD_insuranceExpire      , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.insuranceExpire"      , "Insurance Expiration Day"    ), "edit=2 format=date"),
        new DBField(FLD_driverID             , String.class        , DBField.TYPE_DRIVER_ID() , I18N.getString(Device.class,"Device.fld.driverID"             , "Driver ID"                   ), "edit=2"),
        new DBField(FLD_driverStatus         , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.driverStatus"         , "Driver Status"               ), "edit=2"),
        new DBField(FLD_fuelCapacity         , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fuelCapacity"         , "Fuel Capacity"               ), "edit=2 format=#0.0"),
        new DBField(FLD_fuelCapacity2        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fuelCapacity"         , "Fuel Capacity"               ), "edit=2 format=#0.0"),
        new DBField(FLD_fuelEconomy          , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fuelEconomy"          , "Approx. Fuel Economy"        ), "edit=2 format=#0.0"),
        new DBField(FLD_fuelRatePerHour      , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fuelRatePerHour"      , "Approx. Fuel Rate per Hour"  ), "edit=2 format=#0.0"),
        new DBField(FLD_fuelCostPerLiter     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fuelCostPerLiter"     , "Approx. Fuel Cost per Liter" ), "edit=2 format=#0.00"),
        new DBField(FLD_fuelTankProfile      , String.class        , DBField.TYPE_STRING(320) , I18N.getString(Device.class,"Device.fld.fuelTankProfile"      , "Fuel Tank Profile"           ), "edit=2"),
        new DBField(FLD_fuelTankProfile2     , String.class        , DBField.TYPE_STRING(320) , I18N.getString(Device.class,"Device.fld.fuelTankProfile"      , "Fuel Tank Profile"           ), "edit=2"),
      //new DBField(FLD_fuelProperties       , String.class        , DBField.TYPE_STRING(200) , I18N.getString(Device.class,"Device.fld.fuelProperties"       , "Fuel Properties"             ), "edit=2"),
        new DBField(FLD_speedLimitKPH        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.speedLimitKPH"        , "Max Speed km/h"              ), "edit=2 format=#0.0"),
        new DBField(FLD_maximumRpm           , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.maximumRpm"           , "Max RPM"                     ), "edit=2"),
        new DBField(FLD_planDistanceKM       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.planDistance"         , "Planned Trip Distance"       ), "edit=2 format=#0.0"),
        new DBField(FLD_installTime          , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.installTime"          , "Install Time"                ), "edit=2 format=time"),
        new DBField(FLD_resetTime            , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.resetTime"            , "Reset Time"                  ), "edit=2 format=time"),
        new DBField(FLD_expirationTime       , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.expirationTime"       , "Expiration Time"             ), "edit=2 format=time"),
        // -- DataTransport fields (These fields contain the default DataTransport attributes)
        new DBField(FLD_uniqueID             , String.class        , DBField.TYPE_UNIQ_ID()   , I18N.getString(Device.class,"Device.fld.uniqueID"             , "Unique ID"                   ), "edit=2 altkey=true presep"),
        new DBField(FLD_deviceCode           , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.deviceCode"           , "Server ID"                   ), "edit=2"),
        new DBField(FLD_deviceType           , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.deviceType"           , "Device Type"                 ), "edit=2"),
        new DBField(FLD_pushpinID            , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.pushpinID"            , "Pushpin ID"                  ), "edit=2"),
        new DBField(FLD_displayColor         , String.class        , DBField.TYPE_STRING(16)  , I18N.getString(Device.class,"Device.fld.displayColor"         , "Display Color"               ), "edit=2"),
        new DBField(FLD_serialNumber         , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.serialNumber"         , "Serial Number"               ), "edit=2"),
        new DBField(FLD_simPhoneNumber       , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.simPhoneNumber"       , "SIM Phone Number"            ), Device._simPhoneNumber_attr()),
        new DBField(FLD_simID                , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.simID"                , "SIM ID"                      ), "edit=2"),
        new DBField(FLD_smsEmail             , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.smsEmail"             , "SMS EMail Address"           ), "edit=2"),
        new DBField(FLD_imeiNumber           , String.class        , DBField.TYPE_STRING(24)  , I18N.getString(Device.class,"Device.fld.imeiNumber"           , "IMEI Number"                 ), "edit=2"),
        new DBField(FLD_dataKey              , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.dataKey"              , "Data Key"                    ), "edit=2"),
        new DBField(FLD_ignitionIndex        , Integer.TYPE        , DBField.TYPE_INT16       , I18N.getString(Device.class,"Device.fld.ignitionIndex"        , "Ignition I/O Index"          ), "edit=2"),
        new DBField(FLD_codeVersion          , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.codeVersion"          , "Code Version"                ), ""),
        new DBField(FLD_featureSet           , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.featureSet"           , "Feature Set"                 ), ""),
        new DBField(FLD_ipAddressValid       , DTIPAddrList.class  , DBField.TYPE_STRING(128) , I18N.getString(Device.class,"Device.fld.ipAddressValid"       , "Valid IP Addresses"          ), "edit=2"),
        new DBField(FLD_lastTotalConnectTime , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastTotalConnectTime" , "Last Total Connect Time"     ), "format=time"),
        new DBField(FLD_lastDuplexConnectTime, Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastDuplexConnectTime", "Last Duplex Connect Time"    ), "format=time"),
        // -- Ping/Command
      //new DBField(FLD_pingCommandURI       , String.class        , DBField.TYPE_STRING(128) , I18N.getString(Device.class,"Device.fld.pingCommandURI"  , "Ping Command URL"            , "edit=2"),
        new DBField(FLD_pendingPingCommand   , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.pendingPingCommand"   , "Pending Ping Command"        ), "edit=2"),
        new DBField(FLD_lastPingTime         , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastPingTime"         , "Last 'Ping' Time"            ), "format=time"),
        new DBField(FLD_totalPingCount       , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.totalPingCount"       , "Total 'Ping' Count"          ), ""),
        new DBField(FLD_maxPingCount         , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.maxPingCount"         , "Maximum 'Ping' Count"        ), "edit=2"),
        new DBField(FLD_commandStateMask     , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.commandStateMask"     , "Command State Mask"          ), "edit=2"),
        new DBField(FLD_expectAck            , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.expectAck"            , "Expecting an ACK"            ), "edit=2"),
        new DBField(FLD_expectAckCode        , Integer.TYPE        , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.expectAckStatusCode"  , "Expected ACK Status Code"    ), "edit=2"),
        new DBField(FLD_lastAckCommand       , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.lastAckCommand"       , "Last Command Expecting ACK"  ), ""),
      //new DBField(FLD_lastAckResponse      , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.lastAckResponse"      , "Last Command Response"       ), ""),
        new DBField(FLD_lastAckTime          , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastAckTime"          , "Last Received 'ACK' Time"    ), "format=time"),
        // -- Device Communication Server Configuration
        new DBField(FLD_dcsPropertiesID      , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.dcsPropertiesID"      , "DCS Properties ID"           ), "edit=2"),
        new DBField(FLD_dcsConfigMask        , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.dcsConfigMask"        , "DCS Configuration Mask"      ), "edit=2"),
        new DBField(FLD_dcsConfigString      , String.class        , DBField.TYPE_STRING(80)  , I18N.getString(Device.class,"Device.fld.dcsConfigString"      , "DCS Configuration String"    ), "edit=2"),
        new DBField(FLD_dcsCommandHost       , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.dcsCommandHost"       , "DCS Command Host"            ), "edit=2"),
        new DBField(FLD_dcsCommandState      , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.commandState"         , "Command State"               ), "edit=2"),
        // -- Last connection status
        new DBField(FLD_fixedTcpSessionID    , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.fixedTcpSessionID"    , "Fixed TCP Session ID"        ), ""),
        new DBField(FLD_lastTcpSessionID     , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.tcpSessionID"         , "Last TCP Session ID"         ), ""),
      //new DBField(FLD_ipAddressLocal       , DTIPAddress.class   , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.ipAddressLocal"       , "Local IP Address"            ), ""),
        new DBField(FLD_ipAddressCurrent     , DTIPAddress.class   , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.ipAddressCurrent"     , "Current IP Address"          ), ""),
        new DBField(FLD_remotePortCurrent    , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.remotePortCurrent"    , "Current Remote Port"         ), ""),
        new DBField(FLD_listenPortCurrent    , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.listenPortCurrent"    , "Current Listen Port"         ), ""),
        new DBField(FLD_lastInputState       , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastInputState"       , "Last Input State"            ), ""),
        new DBField(FLD_lastOutputState      , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastOutputState"      , "Last Output State"           ), ""),
        new DBField(FLD_statusCodeState      , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.statusCodeState"      , "StatusCode On/Off State"     ), ""),
        new DBField(FLD_lastBatteryLevel     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastBatteryLevel"     , "Last Internal Battery Level" ), "format=#0.0 units=percent"),
        new DBField(FLD_lastBatteryVolts     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastBatteryVolts"     , "Last Internal Battery Volts" ), "format=#0.0"),
        new DBField(FLD_lastVBatteryVolts    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastVBatteryVolts"    , "Last Vehicle Battery Volts"  ), "format=#0.0"),
        new DBField(FLD_lastFuelLevel        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastFuelLevel"        , "Last Fuel Level"             ), "format=#0.0 units=percent"),
        new DBField(FLD_lastFuelLevel2       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastFuelLevel2"       , "Last Fuel Level #2"          ), "format=#0.0 units=percent"),
        new DBField(FLD_lastFuelTotal        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastFuelTotal"        , "Last Fuel Total Liters"      ), "format=#0.0 units=volume"),
        new DBField(FLD_lastOilLevel         , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastOilLevel"         , "Last Oil Level"              ), "format=#0.0"),
        new DBField(FLD_lastValidLatitude    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastValidLatitude"    , "Last Valid Latitude"         ), "format=#0.00000"),
        new DBField(FLD_lastValidLongitude   , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastValidLongitude"   , "Last Valid Longitude"        ), "format=#0.00000"),
        new DBField(FLD_lastValidHeading     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastValidHeading"     , "Last Valid Heading"          ), "format=#0.0"),
        new DBField(FLD_lastValidSpeedKPH    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastValidSpeedKPH"    , "Last Valid SpeedKPH"         ), "format=#0.0"),
        new DBField(FLD_lastGPSTimestamp     , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastGPSTimestamp"     , "Last Valid GPS Timestamp"    ), "format=time"),
        new DBField(FLD_lastEventTimestamp   , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastEventTimestamp"   , "Last Event Timestamp"        ), "format=time"),
        new DBField(FLD_lastEventStatusCode  , Integer.TYPE        , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastEventStatusCode"  , "Last Event StatusCode"       ), "format=X2 editor=statusCode"),
        new DBField(FLD_lastCellServingInfo  , String.class        , DBField.TYPE_STRING(100) , I18N.getString(Device.class,"Device.fld.lastCellServingInfo"  , "Last Serving Cell Info"      ), ""),
        new DBField(FLD_lastDistanceKM       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastDistanceKM"       , "Last Distance km"            ), "format=#0.0 units=distance"),
        new DBField(FLD_lastOdometerKM       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastOdometerKM"       , "Last Odometer km"            ), "format=#0.0 units=distance"),
        new DBField(FLD_odometerOffsetKM     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.odometerOffsetKM"     , "Odometer Offset km"          ), "format=#0.0 units=distance"),
        new DBField(FLD_lastEngineOnHours    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastEngineOnHours"    , "Last Engine On Hours"        ), "format=#0.0"),
        new DBField(FLD_lastEngineOnTime     , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastEngineOnTime"     , "Last Engine On Time"         ), "format=time"),
        new DBField(FLD_lastEngineOffTime    , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastEngineOffTime"    , "Last Engine Off Time"        ), "format=time"),
        new DBField(FLD_lastEngineHours      , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastEngineHours"      , "Last Engine Hours"           ), "format=#0.0"),
        new DBField(FLD_engineHoursOffset    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.ngineHoursOffset"     , "Engine Hours Offset"         ), "format=#0.0"),
        new DBField(FLD_lastIgnitionOnHours  , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastIgnitionOnHours"  , "Last Ignition On Hours"      ), "format=#0.0"),
        new DBField(FLD_lastIgnitionOnTime   , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastIgnitionOnTime"   , "Last Ignition On Time"       ), "format=time"),
        new DBField(FLD_lastIgnitionOffTime  , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastIgnitionOffTime"  , "Last Ignition Off Time"      ), "format=time"),
        new DBField(FLD_lastIgnitionHours    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastIgnitionHours"    , "Last Ignition Hours"         ), "format=#0.0"),
        new DBField(FLD_lastStopTime         , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastStopTime"         , "Last Stop  Time"             ), "format=time"),
        new DBField(FLD_lastStartTime        , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastStartTime"        , "Last Start Time"             ), "format=time"),
        new DBField(FLD_lastMalfunctionLamp  , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.lastMalfunctionLamp"  , "Last MIL"                    ), "edit=2"),
        new DBField(FLD_lastFaultCode        , String.class        , DBField.TYPE_STRING(96)  , I18N.getString(Device.class,"Device.fld.lastFaultCode"        , "Last Fault Code"             ), ""),
        new DBField(FLD_lastPtoOnHours       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastPtoOnHours"       , "Last PTO On Hours"           ), "format=#0.0"),
        new DBField(FLD_lastPtoOnTime        , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastPtoOnTime"        , "Last PTO On Time"            ), "format=time"),
        new DBField(FLD_lastPtoOffTime       , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastPtoOffTime"       , "Last PTO Off Time"           ), "format=time"),
        new DBField(FLD_lastPtoHours         , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastPtoHours"         , "Last PTO Hours"              ), "format=#0.0"),
        // -- Common fields
        newField_isActive(),
        newField_displayName(), // short name, ShortName
        newField_description(),
        newField_notes(),
        newField_lastUpdateTime(),
        newField_lastUpdateAccount(true),
        newField_lastUpdateUser(true),
        newField_creationTime(),
    };

    // -- OpenDMTP support
    // -  OPTCOLS_OpenDMTPFieldInfo
    // -  startupInit.Device.OpenDMTPFieldInfo=true
    public static final String FLD_supportsDMTP          = "supportsDMTP";          // DMTP
    public static final String FLD_supportedEncodings    = "supportedEncodings";    // DMTP
    public static final String FLD_unitLimitInterval     = "unitLimitInterval";     // DMTP
    public static final String FLD_maxAllowedEvents      = "maxAllowedEvents";      // DMTP
    public static final String FLD_totalProfileMask      = "totalProfileMask";      // DMTP
    public static final String FLD_totalMaxConn          = "totalMaxConn";          // DMTP
    public static final String FLD_totalMaxConnPerMin    = "totalMaxConnPerMin";    // DMTP
    public static final String FLD_duplexProfileMask     = "duplexProfileMask";     // DMTP
    public static final String FLD_duplexMaxConn         = "duplexMaxConn";         // DMTP
    public static final String FLD_duplexMaxConnPerMin   = "duplexMaxConnPerMin";   // DMTP
    public static final DBField OpenDMTPFieldInfo[] = {
        new DBField(FLD_supportsDMTP         , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.supportsDMTP"         , "Supports DMTP"               ), "edit=2"),
        new DBField(FLD_supportedEncodings   , Integer.TYPE        , DBField.TYPE_UINT8       , I18N.getString(Device.class,"Device.fld.supportedEncodings"   , "Supported Encodings"         ), "edit=2 format=X1 editor=encodings mask=Transport$Encodings"),
        new DBField(FLD_unitLimitInterval    , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.unitLimitInterval"    , "Accounting Time Interval Min"), "edit=2"),
        new DBField(FLD_maxAllowedEvents     , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.maxAllowedEvents"     , "Max Events per Interval"     ), "edit=2"),
        new DBField(FLD_totalProfileMask     , DTProfileMask.class , DBField.TYPE_BLOB        , I18N.getString(Device.class,"Device.fld.totalProfileMask"     , "Total Profile Mask"          ), ""),
        new DBField(FLD_totalMaxConn         , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.totalMaxConn"         , "Max Total Conn per Interval" ), "edit=2"),
        new DBField(FLD_totalMaxConnPerMin   , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.totalMaxConnPerMin"   , "Max Total Conn per Minute"   ), "edit=2"),
        new DBField(FLD_duplexProfileMask    , DTProfileMask.class , DBField.TYPE_BLOB        , I18N.getString(Device.class,"Device.fld.duplexProfileMask"    , "Duplex Profile Mask"         ), ""),
        new DBField(FLD_duplexMaxConn        , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.duplexMaxConn"        , "Max Duplex Conn per Interval"), "edit=2"),
        new DBField(FLD_duplexMaxConnPerMin  , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.duplexMaxConnPerMin"  , "Max Duplex Conn per Minute"  ), "edit=2"),
    };

    // -- Default Notification (may require RulesEngine support)
    // -  OPTCOLS_NotificationFieldInfo
    // -  startupInit.Device.NotificationFieldInfo=true
    public static final String FLD_allowNotify           = "allowNotify";           // allow notification
    public static final String FLD_lastNotifyTime        = "lastNotifyTime";        // last notification time
    public static final String FLD_lastNotifyCode        = "lastNotifyCode";        // last notification status code
    public static final String FLD_lastNotifyRule        = "lastNotifyRule";        // last notification rule id
    public static final String FLD_notifyEmail           = "notifyEmail";           // notification email address
    public static final String FLD_notifySelector        = "notifySelector";        // notification rule
    public static final String FLD_notifyAction          = "notifyAction";          // notification action
    public static final String FLD_notifyDescription     = "notifyDescription";     // notification description
    public static final String FLD_notifySubject         = "notifySubject";         // notification subject
    public static final String FLD_notifyText            = "notifyText";            // notification message
    public static final String FLD_notifyUseWrapper      = "notifyUseWrapper";      // notification email wrapper
    public static final String FLD_notifyPriority        = "notifyPriority";        // notification priority
    public static final String FLD_lastSubdivision       = "lastSubdivision";       // last subdivision
    public static final String FLD_parkedLatitude        = "parkedLatitude";        // parked latitude
    public static final String FLD_parkedLongitude       = "parkedLongitude";       // parked longitude
    public static final String FLD_parkedRadius          = "parkedRadius";          // parked radius meters
    public static final String FLD_parkedMaxSpeedKPH     = "parkedMaxSpeedKPH";     // parked maximum speed km/h
    public static final String FLD_proximityRadius       = "proximityRadius";       // nearby device proximity
    public static final String FLD_proximityGroupID      = "proximityGroupID";      // nearby device check group
    public static final String FLD_proximityMaximumAge   = "proximityMaximumAge";   // maximum age between nearby device locations
    public static final String FLD_assignedUserID        = "assignedUserID";        // assigned/preferred user-id
    public static final String FLD_thermalProfile        = "thermalProfile";        // temperature profile
    public static final String FLD_hoursOfOperation      = "hoursOfOperation";      // hours of operation (RTP)
    public static final String FLD_pendingMessage        = "pendingMessage";        // pending message to send to device/operator
    public static final String FLD_pendingMessageACK     = "pendingMessageACK";     // pending message acknowledgement
  //public static final String FLD_rentalStartDate       = "rentalStartDate";       // rental start date
  //public static final String FLD_rentalEndDate         = "rentalEndDate";         // rental end date
  //public static final String FLD_rentalAllowedKM       = "rentalAllowedKM";       // rental allowed kilometers
    public static final String FLD_lastEventsPerSecond   = "lastEventsPerSecond";   // estimated events per second
    public static final String FLD_lastEventsPerSecondMS = "lastEventsPerSecondMS"; // system time of last events per second calculation
    public static final DBField NotificationFieldInfo[] = {
        new DBField(FLD_allowNotify          , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.allowNotify"          , "Allow Notification"          ), "edit=2"),
        new DBField(FLD_lastNotifyTime       , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastNotifyTime"       , "Last Notify Time"            ), _lastNotifyTime_attr()),
        new DBField(FLD_lastNotifyCode       , Integer.TYPE        , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastNotifyCode"       , "Last Notify Status Code"     ), "format=X2"),
        new DBField(FLD_lastNotifyRule       , String.class        , DBField.TYPE_RULE_ID()   , I18N.getString(Device.class,"Device.fld.lastNotifyRule"       , "Last Notify Rule ID"         ), ""),
        new DBField(FLD_notifyEmail          , String.class        , DBField.TYPE_EMAIL_LIST(), I18N.getString(Device.class,"Device.fld.notifyEmail"          , "Notification EMail Address"  ), "edit=2"),
        new DBField(FLD_notifySelector       , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.notifySelector"       , "Notification Selector"       ), "edit=2 editor=ruleSelector"),
        new DBField(FLD_notifyAction         , Integer.TYPE        , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.notifyAction"         , "Notification Action"         ), "edit=2 format=X2 editor=ruleAction mask=RuleFactory$NotifyAction"),
        new DBField(FLD_notifyDescription    , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.notifyDescription"    , "Notification Description"    ), "edit=2 utf8=true"),
        new DBField(FLD_notifySubject        , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.notifySubject"        , "Notification Subject"        ), "edit=2 utf8=true"),
        new DBField(FLD_notifyText           , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.notifyText"           , "Notification Message"        ), "edit=2 editor=textArea utf8=true"),
        new DBField(FLD_notifyUseWrapper     , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.notifyUseWrapper"     , "Notification Use Wrapper"    ), "edit=2"),
        new DBField(FLD_notifyPriority       , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.notifyPriority"       , "Notification Priority"       ), "edit=2"),
      //new DBField(FLD_lastSubdivision      , String.class        , DBField.TYPE_STRING(20)  , I18N.getString(Device.class,"Device.fld.lastSubdivision"      , "Last Subdivision/State"      ), ""),
        new DBField(FLD_parkedLatitude       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.parkedLatitude"       , "Parked Latitude"             ), "format=#0.00000 edit=2"),
        new DBField(FLD_parkedLongitude      , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.parkedLongitude"      , "Parked Longitude"            ), "format=#0.00000 edit=2"),
        new DBField(FLD_parkedRadius         , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.parkedRadius"         , "Parked Radius"               ), "format=#0.0 edit=2"),
        new DBField(FLD_parkedMaxSpeedKPH    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.parkedMaxSpeedKPH"    , "Parked Maximum Speed"        ), "format=#0.0 edit=2"),
      //new DBField(FLD_proximityRadius      , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.proximityRadius"      , "Proximity Radius"            ), "format=#0.0 edit=2"),
      //new DBField(FLD_proximityGroupID     , String.class        , DBField.TYPE_GROUP_ID()  , I18N.getString(Device.class,"Device.fld.proximityGroupID"     , "Proximity Group ID"          ), "edit=2"),
      //new DBField(FLD_proximityMaximumAge  , Long.class          , DBField.TYPE_INT64       , I18N.getString(Device.class,"Device.fld.proximityMaximumAge"  , "Proximity Maximum Age"       ), "edit=2"),
        new DBField(FLD_assignedUserID       , String.class        , DBField.TYPE_USER_ID()   , I18N.getString(Device.class,"Device.fld.assignedUserID"       , "Assigned User"               ), "edit=2"),
        new DBField(FLD_thermalProfile       , String.class        , DBField.TYPE_STRING(200) , I18N.getString(Device.class,"Device.fld.thermalProfile"       , "Temperature Profile"         ), "edit=2"),
        new DBField(FLD_hoursOfOperation     , String.class        , DBField.TYPE_STRING(200) , I18N.getString(Device.class,"Device.fld.hoursOfOperation"     , "Hours Of Operation"          ), "edit=2"),
        new DBField(FLD_pendingMessage       , String.class        , DBField.TYPE_STRING(200) , I18N.getString(Device.class,"Device.fld.pendingMessage"       , "Pending Message"             ), "edit=2"),
        new DBField(FLD_pendingMessageACK    , String.class        , DBField.TYPE_STRING(100) , I18N.getString(Device.class,"Device.fld.pendingMessage"       , "Pending Message"             ), "edit=2"),
        new DBField(FLD_lastEventsPerSecond  , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.lastEventsPerSecond"  , "Last Event per Second"       ), "format=#0.00"),
        new DBField(FLD_lastEventsPerSecondMS, Long.TYPE           , DBField.TYPE_INT64       , I18N.getString(Device.class,"Device.fld.lastEventsPerSecondMS", "Last Event/Second time MS"   ), "format=time"),
    };

    // -- Border Crossing
    // -  OPTCOLS_BorderCrossingFieldInfo
    // -  startupInit.Device.BorderCrossingFieldInfo=true
    public static final String FLD_borderCrossing        = "borderCrossing";        // border crossing flags
    public static final String FLD_lastBorderCrossTime   = "lastBorderCrossTime";   // timestamp of last border crossing calcs
    public static final DBField BorderCrossingFieldInfo[] = {
        new DBField(FLD_borderCrossing       , Integer.TYPE        , DBField.TYPE_UINT8       , I18N.getString(Device.class,"Device.fld.borderCrossing"       , "Border Crossing Flags"       ), "edit=2 enum=Device$BorderCrossingState"),
        new DBField(FLD_lastBorderCrossTime  , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastBorderCrossTime"  , "Last Border Crossing Time"   ), "format=time"),
    };

    // -- Device/Asset Link information
    // -  startupInit.Device.LinkFieldInfo=true
    public static final String FLD_linkURL               = "linkURL";               // Link URL
    public static final String FLD_linkDescription       = "linkDescription";       // Link Description
    public static final DBField LinkFieldInfo[] = {
        new DBField(FLD_linkURL              , String.class        , DBField.TYPE_STRING(128) , I18N.getString(Device.class,"Device.fld.linkURL"              , "Link URL"                    ), "edit=2"),
        new DBField(FLD_linkDescription      , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.linkDescription"      , "Link Description"            ), "edit=2"),
    };

    // -- Fixed device location fields
    // startupInit.Device.FixedLocationFieldInfo=true
    public static final String FLD_fixedLatitude         = "fixedLatitude";         // fixed latitude
    public static final String FLD_fixedLongitude        = "fixedLongitude";        // fixed longitude
    public static final String FLD_fixedAddress          = "fixedAddress";          // fixed address
    public static final String FLD_fixedContactPhone     = "fixedContactPhone";     // fixed contact phone#
    public static final String FLD_fixedServiceTime      = "fixedServiceTime";      // timestamp of last service
    public static final DBField FixedLocationFieldInfo[] = {
        new DBField(FLD_fixedLatitude        , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fixedLatitude"        , "Fixed Latitude"              ), "format=#0.00000 edit=2"),
        new DBField(FLD_fixedLongitude       , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.fixedLongitude"       , "Fixed Longitude"             ), "format=#0.00000 edit=2"),
        new DBField(FLD_fixedAddress         , String.class        , DBField.TYPE_STRING(90)  , I18N.getString(Device.class,"Device.fld.fixedAddress"         , "Fixed Address (Physical)"    ), "utf8=true"),
        new DBField(FLD_fixedContactPhone    , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.fixedContactPhone"    , "Fixed Contact Phone"         ), "utf8=true"),
        new DBField(FLD_fixedServiceTime     , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.fixedServiceTime"     , "Last Service Time"           ), "format=time edit=2"),
    };

    // -- GeoCorridor fields (requires Event Notification Rules Engine)
    // -  startupInit.Device.GeoCorridorFieldInfo=true
    public static final String FLD_activeCorridor        = "activeCorridor";        // active GeoCorridor
    public static final DBField GeoCorridorFieldInfo[]   = {
        new DBField(FLD_activeCorridor       , String.class        , DBField.TYPE_CORR_ID()   , "Active GeoCorridor"          , ""),
    };
    
    // -- Maintenance odometer fields
    // -  startupInit.Device.MaintOdometerFieldInfo=true
  //public static final String FLD_maintDescriptionKM0   = "maintDescriptionKM0";   // odometer maint #0 description
    public static final String FLD_maintIntervalKM0      = "maintIntervalKM0";      // odometer maint #0 interval distance to next
    public static final String FLD_maintOdometerKM0      = "maintOdometerKM0";      // odometer maint #0 last Odometer 
  //public static final String FLD_maintDescriptionKM1   = "maintDescriptionKM1";   // odometer maint #1 description
    public static final String FLD_maintIntervalKM1      = "maintIntervalKM1";      // odometer maint #1 interval distance to next
    public static final String FLD_maintOdometerKM1      = "maintOdometerKM1";      // odometer maint #1 last Odometer
  //public static final String FLD_maintDescriptionHR0   = "maintDescriptionHR0";   // hours maint #0 description
    public static final String FLD_maintIntervalHR0      = "maintIntervalHR0";      // hours maint #0 interval hours to next
    public static final String FLD_maintEngHoursHR0      = "maintEngHoursHR0";      // hours maint #0 last EngineHours
    public static final String FLD_maintNotes            = "maintNotes";
    public static final String FLD_reminderType          = "reminderType";
    public static final String FLD_reminderMessage       = "reminderMessage";
    public static final String FLD_reminderInterval      = "reminderInterval";      // String: 
    public static final String FLD_reminderTime          = "reminderTime";          // timestamp
    public static final String FLD_lastServiceTime       = "lastServiceTime";       // timestamp (last reminder time)
    public static final String FLD_nextServiceTime       = "nextServiceTime";       // timestamp
    public static final DBField MaintOdometerFieldInfo[] = {
      //new DBField(FLD_maintDescriptionKM0  , String.class        , DBField.TYPE_STRING(40)  , I18N.getString(Device.class,"Device.fld.maintDescriptionKM0"  , "#0 Maint Description"        ), "edit=2"),
        new DBField(FLD_maintIntervalKM0     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintIntervalKM0"     , "#0 Maint Distance"           ), "format=#0.0 edit=2"),
        new DBField(FLD_maintOdometerKM0     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintOdometerKM0"     , "#0 Maint Last Odom"          ), "format=#0.0"),
      //new DBField(FLD_maintDescriptionKM1  , String.class        , DBField.TYPE_STRING(40)  , I18N.getString(Device.class,"Device.fld.maintDescriptionKM1"  , "#1 Maint Description"        ), "edit=2"),
        new DBField(FLD_maintIntervalKM1     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintIntervalKM1"     , "#1 Maint Distance "          ), "format=#0.0 edit=2"),
        new DBField(FLD_maintOdometerKM1     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintOdometerKM1"     , "#1 Maint Last Odom"          ), "format=#0.0"),
        new DBField(FLD_maintIntervalHR0     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintIntervalHR0"     , "#0 Maint ElapsedHours"       ), "format=#0.0 edit=2"),
        new DBField(FLD_maintEngHoursHR0     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maintEngHoursHR0"     , "#0 Maint Last EngineHours"   ), "format=#0.0"),
        new DBField(FLD_maintNotes           , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.maintNotes"           , "Maint Notes"                 ), "edit=2 editor=textArea utf8=true"),
      //new DBField(FLD_reminderType         , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.reminderType"         , "Reminder Type"               ), "edit=2"),
        new DBField(FLD_reminderMessage      , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.reminderMessage"      , "Reminder Message"            ), "edit=2 editor=textArea utf8=true"),
        new DBField(FLD_reminderInterval     , String.class        , DBField.TYPE_STRING(64)  , I18N.getString(Device.class,"Device.fld.reminderInterval"     , "Reminder Interval"           ), "edit=2"),
        new DBField(FLD_reminderTime         , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.reminderTime"         , "Last Reminder Time"          ), "format=time"),
        new DBField(FLD_lastServiceTime      , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastServiceTime"      , "Last Service Time"           ), "format=time"),
        new DBField(FLD_nextServiceTime      , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.nextServiceTime"      , "Next Service Time"           ), "format=time"),
    };

    // -- WorkOrder/Job fields
    // -  startupInit.Device.WorkOrderInfo=true
    public static final String FLD_workOrderID           = "workOrderID";           // WorkOrder ID
    public static final String FLD_jobNumber             = "jobNumber";             // associated job number
    public static final String FLD_jobLatitude           = "jobLatitude";           // job latitude
    public static final String FLD_jobLongitude          = "jobLongitude";          // job longitude
    public static final String FLD_jobRadius             = "jobRadius";             // job radius meters
    public static final DBField WorkOrderInfo[]          = {
        new DBField(FLD_workOrderID          , String.class        , DBField.TYPE_STRING(512) , I18N.getString(Device.class,"Device.fld.workOrderID"          , "Work Order ID"               ), "edit=2"),
        new DBField(FLD_jobNumber            , String.class        , DBField.TYPE_STRING(32)  , I18N.getString(Device.class,"Device.fld.jobNumber"            , "Job Number"                  ), "edit=2"),
        new DBField(FLD_jobLatitude          , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.jobLatitude"          , "Job Latitude"                ), "format=#0.00000 edit=2"),
        new DBField(FLD_jobLongitude         , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.jobLongitude"         , "Job Longitude"               ), "format=#0.00000 edit=2"),
        new DBField(FLD_jobRadius            , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.jobRadius"            , "Job Radius"                  ), "format=#0.0 edit=2"),
    };

    // -- Data "Push" fields
    // -  startupInit.Device.DataPushInfo=true
    public static final String FLD_lastDataPushTime      = "lastDataPushTime";      // timestamp of last EventData record push
    public static final String FLD_lastEventCreateMillis = "lastEventCreateMillis"; // timestamp of last data push event creation time
    public static final DBField DataPushInfo[]           = {
        new DBField(FLD_lastDataPushTime     , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastDataPushTime"     , "Last Event Push Time (sec)"  ), "format=time edit=2"),
        new DBField(FLD_lastEventCreateMillis, Long.TYPE           , DBField.TYPE_INT64       , I18N.getString(Device.class,"Device.fld.lastEventCreateMillis", "Last Event Create Time (MS)" ), "format=time"),
    };

    // -- ELog/HOS fields
    // -  startupInit.Device.ELogHOSInfo=true
    public static final String FLD_eLogEnabled           = "eLogEnabled";           // ELog/HOS enabled 
    public static final String FLD_lastELogState         = "lastELogState";         // last ELog/HOS state (timestamp, driving)
    public static final DBField ELogHOSInfo[]            = {
        new DBField(FLD_eLogEnabled          , Boolean.TYPE        , DBField.TYPE_BOOLEAN     , I18N.getString(Device.class,"Device.fld.eLogEnabled"          , "ELog/HOS Enabled"            ), "edit=2"),
        new DBField(FLD_lastELogState        , DTELogState.class   , DBField.TYPE_STRING(120) , I18N.getString(Device.class,"Device.fld.lastELogState"        , "Last ELog/HOS state"         ), "edit=2"),
    };

    // -- Shared map (not yet supported) [OPTCOLS_MapShareInfo]
    // -  startupInit.Device.MapShareInfo=true
    public static final String FLD_mapShareStartTime     = "mapShareStartTime";     // map share start time
    public static final String FLD_mapShareEndTime       = "mapShareEndTime";       // map share start time
    public static final String FLD_mapSharePasscode      = "mapSharePasscode";      // map share passcode
    public static final DBField MapShareInfo[]           = {
        new DBField(FLD_mapShareStartTime    , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.mapShareStartTime"    , "Map Share Start Time"        ), "format=time edit=2"),
        new DBField(FLD_mapShareEndTime      , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.mapShareEndTime"      , "Map Share End Time"          ), "format=time edit=2"),
        new DBField(FLD_mapSharePasscode     , String.class        , DBField.TYPE_STRING(40)  , I18N.getString(Device.class,"Device.fld.mapSharePasscode"     , "Map Share Passcode"          ), "edit=2"),
    };

    // -- Misc Attribute fields
    // -  startupInit.Device.AttributeInfo=true [OPTCOLS_AttributeInfo]
    // -  "vehicleWeight", "wheelchairAccess", ...
    public static final String FLD_maxPassengers         = "maxPassengers";         // maximum number of passengers (not including driver)
    public static final String FLD_customAttributes      = "customAttributes";      // custom attributes
    public static final String FLD_vehicleCurbWeight     = "vehicleCurbWeight";     // vehicle curb weight
    public static final String FLD_vehicleGrossWeight    = "vehicleGrossWeight";    // vehicle weight weight
    public static final DBField AttributeInfo[]          = {
        new DBField(FLD_maxPassengers        , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.maxPassengers"        , "Maximum Passengers"          ), "edit=2"),
        new DBField(FLD_customAttributes     , String.class        , DBField.TYPE_TEXT        , I18N.getString(Device.class,"Device.fld.customFields"         , "Custom Fields"               ), "edit=2"),
        new DBField(FLD_vehicleCurbWeight    , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.vehicleCurbWeight"    , "Vehicle Curb Weight"         ), "format=#0.0 edit=2"),
        new DBField(FLD_vehicleGrossWeight   , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.vehicleGrossWeight"   , "Vehicle Gross Weight"        ), "format=#0.0 edit=2"),
    };

    // -- Global Subscriber fields
    // -  startupInit.Device.GlobalSubscriber=true [OPTCOLS_GlobalSubscriber]
    public static final String FLD_subscriberID          = "subscriberID";          // subscriber ID
    public static final String FLD_subscriberName        = "subscriberName";        // subscriber Name
    public static final String FLD_subscriberAvatar      = "subscriberAvatar";      // subscriber Avatar (pushpin/icon)
    public static final DBField GlobalSubscriber[] = {
        new DBField(FLD_subscriberID         , String.class        , DBField.TYPE_STRING(30)  , I18N.getString(Device.class,"Device.fld.subscriberID"         , "Subscriber ID"               ), "edit=2 altkey=subscriber"),
        new DBField(FLD_subscriberName       , String.class        , DBField.TYPE_STRING(50)  , I18N.getString(Device.class,"Device.fld.subscriberName"       , "Subscriber Name"             ), "edit=2"),
        new DBField(FLD_subscriberAvatar     , String.class        , DBField.TYPE_STRING(120) , I18N.getString(Device.class,"Device.fld.subscriberID"         , "Subscriber Avatar"           ), "edit=2"),
    };

    // -- Platinum fields
    // -  startupInit.Device.PlatinumInfo=true [OPTCOLS_PlatinumInfo]
    public static final String FLD_maxSpeed			 	 = "maxSpeed";
    public static final String FLD_lastTimePause		 = "lastTimePause";
    public static final String FLD_totalJourney		 	 = "totalJourney";
    public static final String FLD_startDayTimestamp 	 = "startDayTimestamp";
  //public static final String FLD_endDayTimestamp 	 	 = "endDayTimestamp"; // usage appears to be same as FLD_lastEventTimestamp
    public static final String FLD_startJourneyTimestamp = "startJourneyTimestamp";
    public static final DBField PlatinumInfo[] = {
        new DBField(FLD_maxSpeed		     , Double.TYPE         , DBField.TYPE_DOUBLE      , I18N.getString(Device.class,"Device.fld.maxSpeed"    		  , "Max Speed"         		  ), "format=#0.0"),
        new DBField(FLD_lastTimePause        , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.lastTimePause"        , "Last Time Pause"             ), ""), 
        new DBField(FLD_totalJourney  		 , Integer.TYPE        , DBField.TYPE_UINT16      , I18N.getString(Device.class,"Device.fld.totalJourney"  		  , "Total Journey"  			  ), "edit=2"),
        new DBField(FLD_startDayTimestamp    , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.startDayTimestamp"    , "Start Day Timestamp"         ), ""), 
      //new DBField(FLD_endDayTimestamp		 , Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.endDayTimestamp"      , "End Day Timestamp"           ), ""), 
        new DBField(FLD_startJourneyTimestamp, Long.TYPE           , DBField.TYPE_UINT32      , I18N.getString(Device.class,"Device.fld.startJourneyTimestamp", "Start Journey Timestamp"     ), ""), 
    };

    /**
    *** Device record key
    **/
    public static class Key
        extends DeviceKey<Device>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String devId) {
            super.setKeyValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setKeyValue(FLD_deviceID , ((devId  != null)? devId.toLowerCase()  : ""));
        }
        public DBFactory<Device> getFactory() {
            return Device.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<Device> factory = null;
    /**
    *** Gets the Device DBFactory
    *** @return The Device DBFactory
    **/
    public static DBFactory<Device> getFactory()
    {
        if (factory == null) {
            EnumTools.registerEnumClass(NotifyAction.class);
            factory = DBFactory.createDBFactory(
                Device.TABLE_NAME(),
                Device.FieldInfo,
                DBFactory.KeyType.PRIMARY,
                Device.class,
                Device.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
          //factory.setLogMissingColumnWarnings(RTConfig.getBoolean(DBConfig.PROP_Device_logMissingColumns,true));
            // --
            Device.initEventDataInsertionListener();
            // -- FLD_notifyEmail max length
            DBField emailFld = factory.getField(FLD_notifyEmail);
            Device.NotifyEmailColumnLength = (emailFld != null)? emailFld.getStringLength() : 0;
            // -- FLD_lastFaultCode max length
            DBField lastFCFld = factory.getField(FLD_lastFaultCode);
            Device.LastFaultCodeColumnLength = (lastFCFld != null)? lastFCFld.getStringLength() : 0;
            // -- FLD_fuelTankProfile max length
            DBField fuelTPFld = factory.getField(FLD_fuelTankProfile);
            Device.FuelProfileColumnLength = (fuelTPFld != null)? fuelTPFld.getStringLength() : 0;
            // -- FLD_fuelProperties max length
          //DBField fuelPropFld = factory.getField(FLD_fuelProperties);
          //Device.FuelPropertiesColumnLength = (fuelPropFld != null)? fuelPropFld.getStringLength() : 0;
        }
        return factory;
    }

    /* Bean instance */
    public Device()
    {
        super();
    }

    /* database record */
    public Device(Device.Key key)
    {
        super(key);
    }

    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(Device.class, loc);
        return i18n.getString("Device.description",
            "This table defines " +
            "Device/Vehicle specific information for an Account. " +
            "A 'Device' record typically represents something that is being 'tracked', such as a Vehicle."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below
    // ------------------------------------------------------------------------

    /**
    *** Gets the user informational Group ID <br>
    *** (user informational only, not used by DeviceGroup)<br>
    *** (currently used in various ReportLayout subclasses)
    *** @return The groupID used for user informational purposes only
    **/
    public String getGroupID()
    {
        String v = (String)this.getFieldValue(FLD_groupID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the user informational Group ID <br>
    *** (user informational only, not used by DeviceGroup)<br>
    *** (currently used in various ReportLayout subclasses)
    *** @param v  The user informational group id
    **/
    public void setGroupID(String v)
    {
        this.setFieldValue(FLD_groupID, StringTools.trim(v));
    }

    /**
    *** Gets the preferred device group description
    *** @return The preferred device group description
    **/
    public String getGroupDescription()
    {
        String groupID = this.getGroupID();
        if (!StringTools.isBlank(groupID)) {
            try {
                DeviceGroup dg = DeviceGroup.getDeviceGroup(this.getAccount(), groupID);
                if (dg != null) {
                    // -- GroupID found, return description
                    return dg.getDescription();
                } else {
                    // -- GroupID not found, return "groupID" itself
                    return groupID;
                }
            } catch (DBException dbe) {
                // -- ignore error, return "groupID" itself
                return groupID;
            }
        }
        return "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the equipment type
    *** @return The equipment type
    **/
    public String getEquipmentType()
    {
        String v = (String)this.getFieldValue(FLD_equipmentType);
        return StringTools.trim(v);
    }

    /**
    *** Sets the equipment type
    *** @param v The equipment type
    **/
    public void setEquipmentType(String v)
    {
        this.setFieldValue(FLD_equipmentType, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if an equipment-status is defined for this Device
    *** @return True if an equipment-status is defined for this Device
    **/
    public boolean hasEquipmentStatus()
    {
        return !StringTools.isBlank(this.getEquipmentStatus());
    }

    /**
    *** Gets the equipment status
    *** @return The equipment status
    **/
    public String getEquipmentStatus()
    {
        String v = (String)this.getFieldValue(FLD_equipmentStatus);
        return StringTools.trim(v);
    }

    /**
    *** Sets the equipment status
    *** @param v The equipment status
    **/
    public void setEquipmentStatus(String v)
    {
        this.setFieldValue(FLD_equipmentStatus, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the vehicle make
    *** @return The vehicle make
    **/
    public String getVehicleMake()
    {
        String v = (String)this.getFieldValue(FLD_vehicleMake);
        return StringTools.trim(v);
    }

    /**
    *** Sets the vehicle make
    *** @param v The vehicle make
    **/
    public void setVehicleMake(String v)
    {
        this.setFieldValue(FLD_vehicleMake, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the vehicle model
    *** @return The vehicle model
    **/
    public String getVehicleModel()
    {
        String v = (String)this.getFieldValue(FLD_vehicleModel);
        return StringTools.trim(v);
    }

    /**
    *** Sets the vehicle model
    *** @param v The vehicle model
    **/
    public void setVehicleModel(String v)
    {
        this.setFieldValue(FLD_vehicleModel, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the vehicle color
    *** @return The vehicle color
    **/
    public String getVehicleColor()
    {
        String v = (String)this.getFieldValue(FLD_vehicleColor);
        return StringTools.trim(v);
    }

    /**
    *** Sets the vehicle color
    *** @param v The vehicle color
    **/
    public void setVehicleColor(String v)
    {
        this.setFieldValue(FLD_vehicleColor, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the vehicle year
    *** @return The vehicle year
    **/
    public int getVehicleYear()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_vehicleYear);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** Sets the vehicle year
    *** @param v The vehicle year
    **/
    public void setVehicleYear(int v)
    {
        int year = 0;
        if (v <= 0) {
            // -- invalid year
            year = 0;
        } else 
        if ((v >= 1800) && (v <= 2999)) {
            // -- assume exactly specified
            year = v;
        } else
        if ((v >= 1) && (v <= 49)) {
            // -- 20xx abbreviation
            year = 2000 + v;
        } else
        if ((v >= 50) && (v <= 99)) {
            // -- 19xx abbreviation
            year = 1900 + v;
        } else {
            // -- invalid year (((v > 99) && (v < 1800)) || (v >= 3000))
            year = 0;
        }
        this.setFieldValue(FLD_vehicleYear, year);
    }

    // ------------------------------------------------------------------------

    private String lastVehicleID = null;

    /**
    *** Returns true if the Vehicle Identification Number (VIN) is defined
    *** @return The Vehicle ID (VIN)
    **/
    public boolean hasVehicleID() // VIN
    {
        return !StringTools.isBlank(this.getVehicleID());
    }

    /**
    *** Gets the Vehicle Identification Number (VIN)
    *** @return The Vehicle ID (VIN)
    **/
    public String getVehicleID() // VIN
    {
        String v = (String)this.getFieldValue(FLD_vehicleID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Vehicle Identification Number (VIN)
    *** @param v The Vehicle ID (VIN)
    **/
    public void setVehicleID(String v)
    {

        /* new VIN */
        String VIN = StringTools.trim(v); // new VIN
        if (VIN.equals("0")) {
            // -- invalid VIN specified, ignore
            // -  some devices may provide this value when the VIN is not available
            return;
        }

        /* last VIN */
        String lastVIN = this.getVehicleID(); // old/previous VIN
        if (VIN.equals(lastVIN)) {
            // -- VIN has not changed, ignore
            // -  clear changed?
            //this.lastVehicleID = null; // reset changed
            return;
        }

        /* VIN has changed */
        // -- save last VIN
        this.lastVehicleID = lastVIN; // may be blank (will not be null)
        // -- set new VIN
        this.setFieldValue(FLD_vehicleID, VIN);
        // -- call change notification
        this.vinDidChange(this.lastVehicleID,VIN);

    }

    // --------------------------------

    /**
    *** Gets the previous value for the VIN.
    *** (Used for testing VIN changes only)
    *** Does not return null.
    **/
    public String getLastVehicleID()
    {
        if (this.lastVehicleID != null) { // !StringTools.isBlank(this.lastVehicleID)
            // -- "setVehicleID(...)" was called at least once with a changed VIN
            return this.lastVehicleID; // not null here
        } else {
            // -- last VIN not yet set, return current VIN
            return this.getVehicleID(); // never null
        }
    }
    
    /**
    *** Returns true if the VIN has changed
    **/
    public boolean hasVinChanged()
    {
        // -- "lastVehicleID" will not be set (non-null) unless the VIN has changed
        return (this.lastVehicleID != null)? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Vehicle License Plate
    *** @return The License Plate
    **/
    public String getLicensePlate()
    {
        String v = (String)this.getFieldValue(FLD_licensePlate);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Vehicle License Plate
    *** @param v The License Plate
    **/
    public void setLicensePlate(String v)
    {
        this.setFieldValue(FLD_licensePlate, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the license/registration expiration date as a DayNumber value
    *** @return The license expiration date as a DayNumber value
    **/
    public long getLicenseExpire()
    {
        // DayNumber licExpire = new DayNumber(driver.getLicenseExpire());
        return this.getFieldValue(FLD_licenseExpire, 0L);
    }

    /**
    *** Sets the license/registration expiration date as a DayNumber value
    *** @param v The license expiration date as a DayNumber value
    **/
    public void setLicenseExpire(long v)
    {
        this.setFieldValue(FLD_licenseExpire, ((v >= 0L)? v : 0L));
    }

    /**
    *** Sets the license/registration expiration date
    *** @param year   The expiration year
    *** @param month1 The expiration month
    *** @param day    The expiration day
    **/
    public void setLicenseExpire(int year, int month1, int day)
    {
        this.setLicenseExpire(DateTime.getDayNumberFromDate(year, month1, day));
    }

    /**
    *** Sets the license/registration expiration date as a DayNumber instance
    *** @param dn The license expiration date as a DayNumber instance
    **/
    public void setLicenseExpire(DayNumber dn)
    {
        this.setLicenseExpire((dn != null)? dn.getDayNumber() : 0L);
    }

    /**
    *** Returns true if the license/registration is expired as-of the specified date
    *** @param asofDay  The as-of expiration test DayNumber
    **/
    public boolean isLicenseExpired(long asofDay)
    {

        /* get license expiration date */
        long le = this.getLicenseExpire();
        if (le <= 0L) {
            // date not specified
            return false;
        }

        /* as-of date */
        long ae = asofDay;
        if (ae <= 0L) {
            // as-of date not specified, use current day
            Account acct = this.getAccount();
            TimeZone tz = (acct != null)? acct.getTimeZone((TimeZone)null) : null;
            ae = DateTime.getDayNumberFromDate(new DateTime(tz));
        }

        /* compare and return */
        return (le < ae)? true : false;

    }

    /**
    *** Returns true if the license/registration is expired as-of the specified date
    *** @param asof  The as-of expiration test DayNumber
    **/
    public boolean isLicenseExpired(DayNumber asof)
    {
        return this.isLicenseExpired((asof != null)? asof.getDayNumber() : 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the insurance expiration date as a DayNumber value
    *** @return The insurance expiration date as a DayNumber value
    **/
    public long getInsuranceExpire()
    {
        // DayNumber insExpire = new DayNumber(driver.getInsuranceExpire());
        return this.getFieldValue(FLD_insuranceExpire, 0L);
    }

    /**
    *** Sets the insurance expiration date as a DayNumber value
    *** @param v The insurance expiration date as a DayNumber value
    **/
    public void setInsuranceExpire(long v)
    {
        this.setFieldValue(FLD_insuranceExpire, ((v >= 0L)? v : 0L));
    }

    /**
    *** Sets the insurance expiration date
    *** @param year   The expiration year
    *** @param month1 The expiration month
    *** @param day    The expiration day
    **/
    public void setInsuranceExpire(int year, int month1, int day)
    {
        this.setInsuranceExpire(DateTime.getDayNumberFromDate(year, month1, day));
    }

    /**
    *** Sets the insurance expiration date as a DayNumber instance
    *** @param dn The insurance expiration date as a DayNumber instance
    **/
    public void setInsuranceExpire(DayNumber dn)
    {
        this.setInsuranceExpire((dn != null)? dn.getDayNumber() : 0L);
    }

    /**
    *** Returns true if the insurance is expired as-of the specified date
    *** @param asofDay  The as-of expiration test DayNumber
    **/
    public boolean isInsuranceExpired(long asofDay)
    {

        /* get insurance expiration date */
        long le = this.getInsuranceExpire();
        if (le <= 0L) {
            // -- date not specified
            return false;
        }

        /* as-of date */
        long ae = asofDay;
        if (ae <= 0L) {
            // -- as-of date not specified, use current day
            Account acct = this.getAccount();
            TimeZone tz = (acct != null)? acct.getTimeZone((TimeZone)null) : null;
            ae = DateTime.getDayNumberFromDate(new DateTime(tz));
        }

        /* compare and return */
        return (le < ae)? true : false;

    }

    /**
    *** Returns true if the insurance is expired as-of the specified date
    *** @param asof  The as-of expiration test DayNumber
    **/
    public boolean isInsuranceExpired(DayNumber asof)
    {
        return this.isInsuranceExpired((asof != null)? asof.getDayNumber() : 0L);
    }

    // ------------------------------------------------------------------------

    private Driver driver = null;

    /**
    *** Returns true if a driver-id is defined for this Device
    *** @return True if this Device record defines a DriverID
    **/
    public boolean hasDriverID()
    {
        return !StringTools.isBlank(this.getDriverID());
    }

    /**
    *** Returns true if the specified driver-id matches the current driver-id.
    *** If either the specified driver-id or current driver-id are blank, then returns false.
    *** @return True if the specified driver-id matches the current driver-id.
    **/
    public boolean isDriverID(String drvID)
    {
        if (!StringTools.isBlank(drvID)) {
            // -- returns false if current driver-id is blank
            return this.getDriverID().equalsIgnoreCase(drvID);
        } else {
            // -- specified driver-id is blank
            return false;
        }
    }

    /**
    *** Gets the Driver-ID, or blank if not defined
    *** @return The Driver-ID
    **/
    public String getDriverID()
    {
        String v = (String)this.getFieldValue(FLD_driverID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Driver-ID
    *** @param v The Driver-ID
    **/
    public void setDriverID(String v)
    {
        this.setFieldValue(FLD_driverID, StringTools.trim(v));
        this.addOtherChangedFieldNames(FLD_driverID);
        this.driver = null;
    }

    /**
    *** Gets the Driver record, or null if not defined
    *** @return The Driver record, or null if undefined
    **/
    public Driver getDriver()
    {
        if (this.driver == null) {
            String driverID = this.getDriverID();
            if (!StringTools.isBlank(driverID)) {
                try {
                    this.driver = Driver.getDriver(this.getAccount(), driverID); // may still be null
                    // -- "this.driver" will be null if driverID does not exist
                } catch (DBException dbe) {
                    // -- error getting driver
                    this.driver = null;
                }
            }
        }
        return this.driver;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a driver-status is defined for this Device
    *** @return True if a driver-status is defined for this Device
    **/
    public boolean hasDriverStatus()
    {
        return (this.getDriverStatus() > Driver.DutyStatus_UNKNOWN)? true : false;
    }

    /**
    *** Gets the driver status
    *** @return The driver status
    **/
    public long getDriverStatus()
    {
        Long v = (Long)this.getFieldValue(FLD_driverStatus);
        return (v != null)? v.longValue() : Driver.DutyStatus_UNKNOWN;
    }

    /**
    *** Sets the driver status
    *** @param v The driver status
    **/
    public void setDriverStatus(long v)
    {
        long ds = (v >= Driver.DutyStatus_UNKNOWN)? v : Driver.DutyStatus_UNKNOWN;
        this.setFieldValue(FLD_driverStatus, ds);
        this.addOtherChangedFieldNames(FLD_driverStatus);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the approximate Fuel Economy, in Km/Liter
    *** @return The approximate Fuel Economy, in Km/Liter
    **/
    public double getFuelEconomy()
    {
        Double v = (Double)this.getFieldValue(FLD_fuelEconomy);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the approximate Fuel Economy, in Km/Liter
    *** @param v The approximate Fuel Economy, in Km/Liter
    **/
    public void setFuelEconomy(double v)
    {
        this.setFieldValue(FLD_fuelEconomy, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the approximate Fuel Consumption Rate in Litres/Hour
    *** @return The approximate Fuel Consumption Rate in Litres/Hour
    **/
    public double getFuelRatePerHour()
    {
        Double v = (Double)this.getFieldValue(FLD_fuelRatePerHour);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the approximate Fuel Consumption Rate in Litres/Hour
    *** @param v The approximate Fuel Consumption Rate in Litres/Hour
    **/
    public void setFuelRatePerHour(double v)
    {
        this.setFieldValue(FLD_fuelRatePerHour, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Fuel cost per Liter
    *** @return The Fuel cost per Liter
    **/
    public double getFuelCostPerLiter()
    {
        Double v = (Double)this.getFieldValue(FLD_fuelCostPerLiter);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the Fuel cost per Liter
    *** @param v The Fuel cost per Liter
    **/
    public void setFuelCostPerLiter(double v)
    {
        this.setFieldValue(FLD_fuelCostPerLiter, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------
    // Fuel Capacity Tank #1

    /**
    *** Gets the tank #1 Fuel Capacity, in Liters
    *** @return The tank #1 Fuel Capacity, in Liters
    **/
    public double getFuelCapacity() // tank #1
    {
        Double v = (Double)this.getFieldValue(FLD_fuelCapacity);
        double V = (v != null)? v.doubleValue() : 0.0;
        return (V >= 0.0)? V : 0.0;
    }

    /**
    *** Sets the tank #1 Fuel Capacity, in Liters
    *** @param v The tank #1 Fuel Capacity, in Liters
    **/
    public void setFuelCapacity(double v) // tank #1
    {
        this.setFieldValue(FLD_fuelCapacity, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------
    // Fuel Capacity Tank #2

    /**
    *** Gets the tank #2 Fuel Capacity, in Liters
    *** @return The tank #2 Fuel Capacity, in Liters
    **/
    public double getFuelCapacity2() // tank #2
    {
        Double v = (Double)this.getFieldValue(FLD_fuelCapacity2);
        double V = (v != null)? v.doubleValue() : 0.0;
        return (V >= 0.0)? V : 0.0;
    }

    /**
    *** Sets the tank #2 Fuel Capacity, in Liters
    *** @param v The tank #2 Fuel Capacity, in Liters
    **/
    public void setFuelCapacity2(double v)
    {
        this.setFieldValue(FLD_fuelCapacity2, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the tank #2 Fuel Capacity, in Liters
    *** @return The tank #2 Fuel Capacity, in Liters
    **/
    public double getFuelCapacity(Device.FuelTankIndex tank)
    {
        if (tank != null) {
            switch (tank) {
                case TANK_1 : return this.getFuelCapacity();    // tank #1
                case TANK_2 : return this.getFuelCapacity2();   // tank #2
                case TOTAL  : return this.getFuelCapacity() + this.getFuelCapacity2();
            }
        }
        return 0.0;
    }

    // ------------------------------------------------------------------------
    // Fuel Tank Profile #1:

    /**
    *** Gets the Fuel tank #1 profile
    *** @return The Fuel tank #1 profile
    **/
    public String getFuelTankProfile()
    {
        String v = (String)this.getFieldValue(FLD_fuelTankProfile);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Fuel tank #1 profile
    *** @param v The Fuel tank #1 profile
    **/
    public void setFuelTankProfile(String v)
    {
        v = StringTools.trim(v);
        if (FuelLevelProfile.IsNone(v)) { v = ""; }
        if ((Device.FuelProfileColumnLength > 0)           &&
            (v.length() >= Device.FuelProfileColumnLength)  ) {
            // // -1 so we are not so close to the edge of the cliff
            // int newLen = Device.FuelProfileColumnLength - 1; 
            // v = v.substring(0, newLen).trim();
            // // Note: MySQL will refuse to insert the entire record if the data 
            // // length is greater than the table column length.
            Print.logWarn("'fuelTankProfile' value is too large (ignored): " + v);
            return;
        }
        this.setFieldValue(FLD_fuelTankProfile, v);
    }

    // ------------------------------------------------------------------------
    // Fuel Tank Profile #2:

    /**
    *** Gets the Fuel tank #2 profile
    *** @return The Fuel tank #2 profile
    **/
    public String getFuelTankProfile2()
    {
        String v = (String)this.getFieldValue(FLD_fuelTankProfile2);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Fuel tank #2 profile
    *** @param v The Fuel tank #2 profile
    **/
    public void setFuelTankProfile2(String v)
    {
        v = StringTools.trim(v);
        if (FuelLevelProfile.IsNone(v)) { v = ""; }
        if ((Device.FuelProfileColumnLength > 0)           &&
            (v.length() >= Device.FuelProfileColumnLength)  ) {
            // // -1 so we are not so close to the edge of the cliff
            // int newLen = Device.FuelProfileColumnLength - 1; 
            // v = v.substring(0, newLen).trim();
            // // Note: MySQL will refuse to insert the entire record if the data 
            // // length is greater than the table column length.
            Print.logWarn("'fuelTankProfile2' value is too large (ignored): " + v);
            return;
        }
        this.setFieldValue(FLD_fuelTankProfile2, v);
    }

    // --------------------------------

    /**
    *** Gets the Fuel tank profile for the specified index
    *** @param tank  The fuel-tank index 
    *** @return The Fuel tank profile
    **/
    public String getFuelTankProfile(Device.FuelTankIndex tank)
    {
        if (tank != null) {
            switch (tank) {
                case TANK_1 : return this.getFuelTankProfile();
                case TANK_2 : return this.getFuelTankProfile2();
                case TOTAL  : /*no-profile*/; return "";
            }
        }
        return "";
    }

    /**
    *** Returns true if a Fuel tank profile is defined for the specified tank index
    *** @return True if a Fuel tank profile is defined for the specified tank index
    **/
    public boolean hasFuelTankProfile(Device.FuelTankIndex tank)
    {
        String flpn = this.getFuelTankProfile(tank);
        if (FuelLevelProfile.IsNone(flpn)) {
            // -- device FuelLevelProfile is blank/NONE
            return false;
        } else
        if (this.getFuelLevelProfile(tank,null) == null) {
            // -- device specified FuelLevelProfile name is not found
            return false;
        } else {
            // -- device specified FuelLevelProfile exists
            return true;
        }
    }

    /**
    *** Gets the FuelLevelProfile instance for the specified index.  
    *** Returns "dft" if no FuelLevelProfile defined.
    **/
    public FuelLevelProfile getFuelLevelProfile(Device.FuelTankIndex tank, FuelLevelProfile dft)
    {
        String profName = this.getFuelTankProfile(tank);
        return FuelLevelProfile.GetFuelLevelProfile(profName,dft); // may return null, if "dft" is null
    }

    /**
    *** Gets the FuelLevelProfile instance for the specified index.  
    *** Returns "LINEAR" if no FuelLevelProfile defined. 
    *** (does not return null)
    **/
    public FuelLevelProfile getFuelLevelProfile(Device.FuelTankIndex tank)
    {
        String profName = this.getFuelTankProfile(tank);
        return FuelLevelProfile.GetFuelLevelProfile(profName); // never null
    }

    /**
    *** Gets the FuelLevelProfile instance for the specified index
    **/
    public double getActualFuelLevel(Device.FuelTankIndex tank, double fuelLevel)
    {
        FuelLevelProfile flp = this.getFuelLevelProfile(tank); // never null
        return flp.getActualFuelLevel(fuelLevel);
    }

    // ------------------------------------------------------------------------
    // Fuel Properties (defined by the context in which they are used)

    //private RTProperties fuelProperties = null;

    /**
    *** Gets a Fuel Property value
    *** @return The Fuel Property value
    **/
    /*
    public String getFuelPropertyValue(String key, String dft)
    {
        if (this.fuelProperties == null) {
            String fuelPropStr = this.getFuelProperties();
            if (StringTools.isBlank(fuelPropStr)) {
                return dft;
            }
            this.fuelProperties = new RTProperties(fuelPropStr);
        }
        return this.fuelProperties.getString(key, dft);
    }
    */

    // --------------------------------

    /**
    *** Gets the Fuel Properties not defined elsewhere
    *** @return The Fuel Properties
    **/
    /*
    public String getFuelProperties()
    {
        String v = (String)this.getFieldValue(FLD_fuelProperties);
        return StringTools.trim(v);
    }
    */

    /**
    *** Sets the Fuel Properties not defined elsewhere
    *** @param v The Fuel Properties
    **/
    /*
    public void setFuelProperties(String v)
    {
        v = StringTools.trim(v);
        if ((Device.FuelPropertiesColumnLength > 0)          &&
            (v.length() >= Device.FuelPropertiesColumnLength)  ) {
            // // -1 so we are not so close to the edge of the cliff
            // int newLen = Device.FuelProfileColumnLength - 1; 
            // v = v.substring(0, newLen).trim();
            // // Note: MySQL will refuse to insert the entire record if the data 
            // // length is greater than the table column length.
            Print.logWarn("'fuelProperties' value is too large (ignored): " + v);
            return;
        }
        this.setFieldValue(FLD_fuelProperties, v);
        this.fuelProperties = null;
    }
    */

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the assigned speed limit for this device, in km/h
    *** @return The assigned speed limit for this device, in km/h
    **/
    public double getSpeedLimitKPH()
    {
        Double v = (Double)this.getFieldValue(FLD_speedLimitKPH);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the assigned speed limit for this device, in km/h
    *** @param v The assigned speed limit for this device, in km/h
    **/
    public void setSpeedLimitKPH(double v)
    {
        this.setFieldValue(FLD_speedLimitKPH, (v >= 0.0)? v : 0.0);
    }

    /**
    *** Returns true if this device has a maximum speed assigned
    *** @param v The assigned speed limit for this device, in km/h
    **/
    public boolean hasSpeedLimitKPH()
    {
        return (this.getSpeedLimitKPH() > 0.0)? true : false;
    }

    /**
    *** Returns true if the specified speed exceeds the speed limit of this Device instance
    **/
    public boolean isSpeeding(double speedKPH, double offsetKPH)
    {
        double limKPH = this.getSpeedLimitKPH();
        if ((limKPH > 0.0) && (speedKPH > (limKPH + offsetKPH))) {
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Return true if event indicates that device is speeding 
    *** used by (Rule function "$Speeding")
    **/
    public static boolean IsSpeeding(EventData ed, double dftLimitKPH, double dftOffsetKPH, boolean checkGeozones)
    {

        // -- no event?
        if (ed == null) {
            return false;
        }
        Device device = ed.getDevice(); // should not be null
        double evSpeedKPH = ed.getSpeedKPH();

        // -- 1: specified-limit (arguments)
        if ((dftLimitKPH > 0.0) && (evSpeedKPH > (dftLimitKPH + dftOffsetKPH))) {
            //Print.logInfo("Speeding: > specified limit");
            if (device != null) {
                RTProperties rtp = device.getTemporaryProperties();
                rtp.setProperty("Speeding.type"  ,"parameter");
                rtp.setProperty("Speeding.limit" ,dftLimitKPH);
                rtp.setProperty("Speeding.offset",dftOffsetKPH);
            }
            return true; // exceeded specified limit
        }

        // -- 2: device-speed
        if (device != null) { // device.hasSpeedLimitKPH()
            double limKPH = device.getSpeedLimitKPH();
            if ((limKPH > 0.0) && (evSpeedKPH > (limKPH + dftOffsetKPH))) {
                //Print.logInfo("Speeding: > device limit");
                if (device != null) {
                    RTProperties rtp = device.getTemporaryProperties();
                    rtp.setProperty("Speeding.type"  ,"device");
                    rtp.setProperty("Speeding.limit" ,limKPH);
                    rtp.setProperty("Speeding.offset",dftOffsetKPH);
                }
                return true; // exceeded max device speed
            }
        }

        // -- 3: posted-speed
        if (ed.hasSpeedLimitKPH()) {
            double limKPH = ed.getSpeedLimitKPH(); // posted limit
            if ((limKPH > 0.0) && (evSpeedKPH > (limKPH + dftOffsetKPH))) {
                //Print.logInfo("Speeding: > posted limit");
                if (device != null) {
                    RTProperties rtp = device.getTemporaryProperties();
                    rtp.setProperty("Speeding.type"  ,"posted");
                    rtp.setProperty("Speeding.limit" ,limKPH);
                    rtp.setProperty("Speeding.offset",dftOffsetKPH);
                }
                return true; // exceeded event reverse-geocoded speed limit
            }
        }

        // -- 4: zone-speed
        if (checkGeozones) {
            Geozone GZ = Geozone.getGeozone(
                ed.getAccountID(), null/*ZoneID*/, 
                ed.getGeoPoint() , null/*purpose*/, false/*RGOnly*/);
            if ((GZ != null) && GZ.isDeviceInGroup(ed.getDeviceID())) {
                double limKPH = GZ.getSpeedLimitKPH();
                if ((limKPH > 0.0) && (evSpeedKPH > (limKPH + dftOffsetKPH))) {
                    //Print.logInfo("Speeding: > geozone limit");
                    if (device != null) {
                        RTProperties rtp = device.getTemporaryProperties();
                        rtp.setProperty("Speeding.type"  ,"geozone");
                        rtp.setProperty("Speeding.limit" ,limKPH);
                        rtp.setProperty("Speeding.offset",dftOffsetKPH);
                    }
                    return true;
                }
            }
        }

        // -- not speeding
        if (device != null) {
            RTProperties rtp = device.getTemporaryProperties();
            rtp.removeProperty("Speeding.type"  );
            rtp.removeProperty("Speeding.limit" );
            rtp.removeProperty("Speeding.offset");
        }
        return false;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the assigned maximum RPM for this device
    *** @return The assigned maximum RPM for this device
    **/
    public long getMaximumRpm()
    {
        Long v = (Long)this.getFieldValue(FLD_maximumRpm);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the assigned maximum RPM for this device
    *** @param v The assigned maximum RPM for this device
    **/
    public void setMaximumRpm(long v)
    {
        this.setFieldValue(FLD_maximumRpm, (v >= 0L)? v : 0L);
    }

    /**
    *** Returns true if this device has a maximum speed assigned
    *** @param v The assigned speed limit for this device, in km/h
    **/
    public boolean hasMaximumRpm()
    {
        return (this.getMaximumRpm() > 0L)? true : false;
    }

    /**
    *** Returns true if the specified RPM exceeds the maximum RPM of this Device instance
    **/
    public boolean isExcessiveRpm(long engRPM)
    {
        long maxRPM = this.getMaximumRpm();
        if ((maxRPM > 0.0) && (engRPM > maxRPM)) {
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Return true if event indicates that device has exessive RPM
    *** used by (Rule function "$ExcessiveRPM")
    **/
    public static boolean IsExcessiveRpm(EventData ed, long dftMaxRPM)
    {

        // -- no event?
        if (ed == null) {
            return false;
        }
        Device device = ed.getDevice(); // should not be null
        long evEngineRpm = ed.getEngineRpm();

        // -- 1: specified-limit (arguments)
        if ((dftMaxRPM > 0L) && (evEngineRpm > dftMaxRPM)) {
            //Print.logInfo("ExcessRPM: > specified limit");
            if (device != null) {
                RTProperties rtp = device.getTemporaryProperties();
                rtp.setProperty("ExcessRPM.type"  ,"parameter");
                rtp.setProperty("ExcessRPM.limit" ,dftMaxRPM);
            }
            return true; // exceeded specified limit
        }

        // -- 2: device-maximum
        if (device != null) { // device.hasMaximumRpm()
            long maxRPM = device.getMaximumRpm();
            if ((maxRPM > 0L) && (evEngineRpm > maxRPM)) {
                //Print.logInfo("ExcessRPM: > device limit");
                if (device != null) {
                    RTProperties rtp = device.getTemporaryProperties();
                    rtp.setProperty("ExcessRPM.type"  ,"device");
                    rtp.setProperty("ExcessRPM.limit" ,maxRPM);
                }
                return true; // exceeded max device maximum
            }
        }

        // -- not excessive rpm
        if (device != null) {
            RTProperties rtp = device.getTemporaryProperties();
            rtp.removeProperty("ExcessRPM.type"  );
            rtp.removeProperty("ExcessRPM.limit" );
        }
        return false;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the assigned "Plan Distance" for this device, in kilometers
    *** @return The assigned "Plan Distance" for this device, in kilometers
    **/
    public double getPlanDistanceKM()
    {
        Double v = (Double)this.getFieldValue(FLD_planDistanceKM);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the assigned "Plan Distance" for this device, in kilometers
    *** @param v The assigned "Plan Distance" for this device, in kilometers
    **/
    public void setPlanDistanceKM(double v)
    {
        this.setFieldValue(FLD_planDistanceKM, (v >= 0.0)? v : 0.0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the install time of this Device (in Unix Epoch time format)
    *** @return The install time of this Device, or '0' if undefined.
    **/
    public long getInstallTime()
    {
        Long v = (Long)this.getFieldValue(FLD_installTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the install time of this Device (in Unix Epoch time format)
    *** @param v The install time of this Device, or '0' if undefined.
    **/
    public void setInstallTime(long v)
    {
        this.setFieldValue(FLD_installTime, (v >= 0L)? v : 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the reset time of this Device (in Unix Epoch time format)
    *** @return The reset time of this Device, or '0' if undefined.
    **/
    public long getResetTime()
    {
        Long v = (Long)this.getFieldValue(FLD_resetTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the reset time of this Device (in Unix Epoch time format)
    *** @param v The reset time of this Device, or '0' if undefined.
    **/
    public void setResetTime(long v)
    {
        this.setFieldValue(FLD_resetTime, (v >= 0L)? v : 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the expiration time of this Device (in Unix Epoch time format)
    *** @return The expiration time of this Device, or '0' if this Device never expires.
    **/
    public long getExpirationTime()
    {
        Long v = (Long)this.getFieldValue(FLD_expirationTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the expiration time of this Device (in Unix Epoch time format)
    *** @param v The expiration time of this Device, or '0' if this Device never expires.
    **/
    public void setExpirationTime(long v)
    {
        this.setFieldValue(FLD_expirationTime, (v >= 0L)? v : 0L);
    }

    /**
    *** Returns true if this Device has expired
    *** @return True if this Device has expired
    **/
    public boolean isExpired()
    {

        /* device expired? */
        long expireTime = this.getExpirationTime();
        if ((expireTime > 0L) && (expireTime < DateTime.getCurrentTimeSec())) {
            return true;
        }

        /* account expired? */
        //Account account = this.getAccount();
        //if ((account != null) && account.isExpired()) {
        //    return true;
        //}

        /* not expired */
        return false;

    }

    /**
    *** Returns true if this Device has an expiry date
    *** @return True if this Device has an expiry date
    **/
    public boolean doesExpire()
    {
        long expireTime = this.getExpirationTime();
        return (expireTime > 0L);
    }

    /**
    *** Returns true if this Device will expire within the specified number of seconds
    *** @param withinSec  The tested expiry time range (in seconds)
    *** @return True if this Device will expire within the specified number of seconds
    **/
    public boolean willExpire(long withinSec)
    {

        /* will device expire? */
        long expireTime = this.getExpirationTime();
        if ((expireTime > 0L) && 
            ((withinSec < 0L) || (expireTime < (DateTime.getCurrentTimeSec() + withinSec)))) {
            return true;
        }

        /* will account expire */
        //Account account = this.getAccount();
        //if ((account != null) && account.willExpire(withinSec)) {
        //    return true;
        //}

        /* will not expired */
        return false;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this Device record supports the "linkURL" field
    *** @return True if this Device record supports the "linkURL" field
    **/
    public static boolean supportsLinkURL()
    {
        return Device.getFactory().hasField(FLD_linkURL);
    }

    /**
    *** Returns true if this Device record defines a non-blank Link-URL value
    *** @return True if this Device record defines a non-blank Link-URL value
    **/
    public boolean hasLink()
    {
        return !StringTools.isBlank(this.getLinkURL());
    }

    /**
    *** Gets the Link-URL for this Device
    *** @return The Link-URL for this Device
    **/
    public String getLinkURL()
    {
        String v = (String)this.getOptionalFieldValue(FLD_linkURL);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Link-URL for this Device
    *** @param v The Link-URL for this Device
    **/
    public void setLinkURL(String v)
    {
        this.setOptionalFieldValue(FLD_linkURL, StringTools.trim(v));
    }
 
    // ------------------------------------------------------------------------

    /**
    *** Gets the Link-Description for this Device
    *** @return The Link-Description for this Device
    **/
    public String getLinkDescription()
    {
        String v = (String)this.getOptionalFieldValue(FLD_linkDescription);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Link-Description for this Device
    *** @param v The Link-Description for this Device
    **/
    public void setLinkDescription(String v)
    {
        this.setOptionalFieldValue(FLD_linkDescription, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    private static final boolean CHECK_ACCOUNT_ALLOWNOTIFY = false;

    /**
    *** Returns true if this Device record supports the "allowNotify" field
    *** @return True if this Device record supports the "allowNotify" field
    **/
    public static boolean supportsNotification()
    { // hasOption
        return Device.getFactory().hasField(FLD_allowNotify);
    }

    /**
    *** Returns true if this device allows notifications
    *** @return True if this device allows notifications
    **/
    public boolean getAllowNotify()
    {
        Boolean v = (Boolean)this.getOptionalFieldValue(FLD_allowNotify);
        return (v != null)? v.booleanValue() : false;
    }

    /**
    *** Sets the "Allow Notification" state for this Device
    *** @param v The "Allow Notification" state for this Device
    **/
    public void setAllowNotify(boolean v)
    {
        this.setOptionalFieldValue(FLD_allowNotify, v);
    }

    /**
    *** Returns true if this device allows notifications
    *** @param checkAccount True to also check Account
    *** @return True if this device allows notifications
    **/
    public boolean getAllowNotify(boolean checkAccount)
    {

        /* check device */
        if (!this.getAllowNotify()) {
            // -- device says to not allow notify
            return false;
        }

        /* check account? */
        if (!checkAccount) {
            // -- explicit, do not check account
            return true;
        } else
        if (!RTConfig.getBoolean(DBConfig.PROP_Device_checkAccountAllowNotify,CHECK_ACCOUNT_ALLOWNOTIFY)) {
            // -- property says to not check account
            return true;
        }

        /* check account */
        Account acct = this.getAccount();
        if (acct != null) {
            // -- defer to Account allowNotify
            return acct.getAllowNotify();
        } else {
            // -- unlikely, but if there is no account, then do not allow notification
            return false;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Last Notification time for this Device (in Unix Epoch time format)
    *** @return The Last Notification time for this Device (in Unix Epoch time format)
    **/
    public long getLastNotifyTime()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_lastNotifyTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the Last Notification time for this Device (in Unix Epoch time format)
    *** @param v The Last Notification time for this Device (in Unix Epoch time format)
    **/
    public void setLastNotifyTime(long v)
    {
        this.setOptionalFieldValue(FLD_lastNotifyTime, v);
    }

    // ---

    /**
    *** Gets the Last Notification Status-Code for this Device
    *** @return The Last Notification Status-Code for this Device
    **/
    public int getLastNotifyCode()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_lastNotifyCode);
        return (v != null)? v.intValue() : StatusCodes.STATUS_NONE;
    }

    /**
    *** Sets the Last Notification Status-Code for this Device
    *** @param v The Last Notification Status-Code for this Device
    **/
    public void setLastNotifyCode(int v)
    {
        this.setOptionalFieldValue(FLD_lastNotifyCode, v);
    }

    // ---

    /**
    *** Gets the Rule-ID which triggered the Last Notification for this Device
    *** @return The Rule-ID which triggered the Last Notification for this Device
    **/
    public String getLastNotifyRule()
    {
        String v = (String)this.getOptionalFieldValue(FLD_lastNotifyRule);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Rule-ID which triggered the Last Notification for this Device
    *** @param v The Rule-ID which triggered the Last Notification for this Device
    **/
    public void setLastNotifyRule(String v)
    {
        this.setOptionalFieldValue(FLD_lastNotifyRule, StringTools.trim(v));
    }

    // ---

    /**
    *** Sets the event information for the last rule-triggered notification
    *** @param timestamp The timestamp (Unix Epoch format) of the notification
    *** @param ruleID The Rule-ID which triggered the notification
    *** @param update True to update the Device record now
    **/
    public void setLastNotifyEvent(long timestamp, String ruleID, boolean update)
        throws DBException
    {
        if (timestamp >= 0L) {
            this.setLastNotifyTime(timestamp);                  // FLD_lastNotifyTime
            this.setLastNotifyCode(StatusCodes.STATUS_NONE);    // FLD_lastNotifyCode
        } else {
            this.setLastNotifyTime(0L);                         // FLD_lastNotifyTime
            this.setLastNotifyCode(StatusCodes.STATUS_NONE);    // FLD_lastNotifyCode
        }
        this.setLastNotifyRule(ruleID);                         // FLD_lastNotifyRule
        if (update) {
            this.update(
                Device.FLD_lastNotifyTime, 
                Device.FLD_lastNotifyCode,
                Device.FLD_lastNotifyRule
                );
        } else {
            this.addOtherChangedFieldNames(
                Device.FLD_lastNotifyTime, 
                Device.FLD_lastNotifyCode,
                Device.FLD_lastNotifyRule
                );
        }
    }

    /**
    *** Sets the event information for the last rule-triggered notification
    *** @param event The EventData record of the notification
    *** @param ruleID The Rule-ID which triggered the notification
    *** @param update True to update the Device record now
    **/
    public void setLastNotifyEvent(EventData event, String ruleID, boolean update)
        throws DBException
    {
        if (event != null) {
            this.setLastNotifyTime(event.getTimestamp());       // FLD_lastNotifyTime
            this.setLastNotifyCode(event.getStatusCode());      // FLD_lastNotifyCode
        } else {
            this.setLastNotifyTime(0L);                         // FLD_lastNotifyTime
            this.setLastNotifyCode(StatusCodes.STATUS_NONE);    // FLD_lastNotifyCode
        }
        this.setLastNotifyRule(ruleID);                         // FLD_lastNotifyRule
        if (update) {
            this.update(
                Device.FLD_lastNotifyTime, 
                Device.FLD_lastNotifyCode,
                Device.FLD_lastNotifyRule
                );
        } else {
            this.addOtherChangedFieldNames(
                Device.FLD_lastNotifyTime, 
                Device.FLD_lastNotifyCode,
                Device.FLD_lastNotifyRule
                );
        }
    }

    /**
    *** Clears the last notification for this Device
    *** @param update True to update the Device record after clearing
    **/
    public void clearLastNotifyEvent(boolean update)
        throws DBException
    {
        this.setLastNotifyEvent(null/*EventData*/, ""/*RuleID*/, update);
    }

    /**
    *** Gets the EventData record for the last notification
    *** @return The EventData record for the last notification
    **/
    public EventData getLastNotifyEvent()
    {
        long ts = this.getLastNotifyTime();
        int  sc = this.getLastNotifyCode();

        /* no active notify event */
        if ((ts <= 0L) || (sc <= 0)) {
            return null;
        }

        /* get event */
        String A = this.getAccountID();
        String D = this.getDeviceID();
        try {
            EventData ev = EventData.getEventData(A, D, ts, sc);
            if (ev == null) {
                Print.logWarn("LastNotifyEvent not found: "+A+"/"+D+", " + ts + " " + StatusCodes.ToString(sc));
                return null;
            } else {
                return ev;
            }
        } catch (DBException dbe) {
            Print.logError("Error reading Device notify event ["+A+"/"+D+"]: " + dbe);
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Notification Email address
    *** @return The Notification Email Address
    **/
    public String getNotifyEmail()
    {
        String v = (String)this.getOptionalFieldValue(FLD_notifyEmail);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Notification Email address
    *** @param v The Notification Email Address
    **/
    public void setNotifyEmail(String v)
    {
        String ne = StringTools.trim(v);
        if ((Device.NotifyEmailColumnLength > 0)           &&
            (ne.length() >= Device.NotifyEmailColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = Device.getMaximumNotifyEmailLength(); 
            ne = ne.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the entire record if the data 
            // length is greater than the table column length.
        }
        this.setOptionalFieldValue(FLD_notifyEmail, ne);
    }

    /**
    *** Gets the maximum Notify Email length
    **/
    public static int getMaximumNotifyEmailLength()
    {
        // -1 so we are not so close to the edge of the cliff
        return Device.NotifyEmailColumnLength - 1;
    }

    /**
    *** Returns a String containing all email address that should be notified for this Device
    *** @param inclAccount  True to include the Account notify email address
    *** @param inclUser     True to include the assigned User notify email address
    *** @return The String containing email addresses to notify
    **/
    @Deprecated
    public String getNotifyEmail(boolean inclAccount, boolean inclUser)
    {
        return this.getNotifyEmail(inclAccount, inclUser, false/*inclGroup*/);
    }

    /**
    *** Returns a String containing all email address that should be notified for this Device
    *** @param inclAccount  True to include the Account notify email address
    *** @param inclUser     True to include the assigned User notify email address
    *** @param inclGroup    True to include the notify email address from all group memberships
    *** @return The String containing email addresses to notify
    **/
    public String getNotifyEmail(boolean inclAccount, boolean inclUser, boolean inclGroup)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getNotifyEmail()); // start with just Device notifyEmail

        /* include Account notify email addresses */
        if (inclAccount) {
            Account acct = this.getAccount();
            if (acct == null) {
                // skip (should not occur)
            } else {
                String ae = acct.getNotifyEmail();
                if (!StringTools.isBlank(ae)) {
                    if (sb.length() > 0) { sb.append(","); }
                    sb.append(ae);
                }
            }
        }

        /* include User notify email addresses */
        if (inclUser) {
            User user = this.getAssignedUser();
            try {
                if (user == null) {
                    // skip (invalid/blank user-d)
                } else
                if (!user.isAuthorizedDevice(this.getDeviceID())) { // DBException
                    // skip (not authorized)
                } else {
                    String ue = user.getNotifyEmail();
                    if (!StringTools.isBlank(ue)) {
                        if (sb.length() > 0) { sb.append(","); }
                        sb.append(ue);
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Checking User authorization", dbe);
                // skip (exception)
            }
        }

        /* include DeviceGroup notify email addresses */
        if (inclGroup && DeviceGroup.supportsNotification()) {
            try {
                String acctID = this.getAccountID();
                String devID  = this.getDeviceID();
                Collection<String> grpIDs = DeviceGroup.getDeviceGroupsForDevice(acctID, devID, false);
                if (!ListTools.isEmpty(grpIDs)) {
                    Account acct = this.getAccount();
                    for (String gid : grpIDs) {
                        DeviceGroup group = DeviceGroup.getDeviceGroup(acct,gid);
                        if ((group != null) && group.getAllowNotify()) {
                            String ge = group.getNotifyEmail();
                            if (!StringTools.isBlank(ge)) {
                                if (sb.length() > 0) { sb.append(","); }
                                sb.append(ge);
                            }
                        }
                    }
                }
            } catch (DBException dbe) {
                Print.logException("Getting DeviceGroups for Device", dbe);
            }
        }

        /* return accumulated addresses */
        return sb.toString();

    }

    // ---

    /** 
    *** Gets the Rule Selector to be evaluated by the installed RuleFactory.<br>
    *** This rule-selector is currently only used by default with the "RuleFactoryLite" module.
    *** @return The rule-selector to evaluate
    **/
    public String getNotifySelector()
    {
        // see CHECK_NOTIFY_SELECTOR
        String v = (String)this.getOptionalFieldValue(FLD_notifySelector);
        return StringTools.trim(v);
    }

    /** 
    *** Sets the Rule Selector to be evaluated by the installed RuleFactory.<br>
    *** This rule-selector is currently only used by default with the "RuleFactoryLite" module.
    *** @param v The rule-selector to evaluate
    **/
    public void setNotifySelector(String v)
    {
        this.setOptionalFieldValue(FLD_notifySelector, StringTools.trim(v));
    }

    // ---

    /** 
    *** Gets the Notify Actions to be executed if the Notify Rule-Selector is triggered.<br>
    *** This notify action is currently only used by default with the "RuleFactoryLite" module.
    *** @return The notify action mask
    **/
    public int getNotifyAction()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_notifyAction);
        return (v != null)? RuleFactoryAdapter.ValidateActionMask(v.intValue()) : RuleFactory.ACTION_DEFAULT;
    }

    /** 
    *** Sets the Notify Actions to be executed if the Notify Rule-Selector is triggered.<br>
    *** This notify action is currently only used by default with the "RuleFactoryLite" module.
    *** @param v The notify action mask
    **/
    public void setNotifyAction(int v)
    {
        this.setOptionalFieldValue(FLD_notifyAction, RuleFactoryAdapter.ValidateActionMask(v));
    }

    // ---

    /** 
    *** Gets the Notify Description for the rule-selector specified.<br>
    *** This notify description is currently only used by default with the "RuleFactoryLite" module.
    *** @return The notify description
    **/
    public String getNotifyDescription()
    {
        String v = (String)this.getOptionalFieldValue(FLD_notifyDescription);
        return StringTools.trim(v);
    }

    /** 
    *** Sets the Notify Description for the rule-selector specified.<br>
    *** This notify description is currently only used by default with the "RuleFactoryLite" module.
    *** @param v The notify description
    **/
    public void setNotifyDescription(String v)
    {
        this.setOptionalFieldValue(FLD_notifyDescription, StringTools.trim(v));
    }

    // ---

    /** 
    *** Gets the Email Subject for the triggered notification email .<br>
    *** This email subject is currently only used by default with the "RuleFactoryLite" module.
    *** @return The notify email subject
    **/
    public String getNotifySubject()
    {
        String v = (String)this.getFieldValue(FLD_notifySubject);
        return (v != null)? v : "";
    }

    /** 
    *** Sets the Email Subject for the triggered notification email .<br>
    *** This email subject is currently only used by default with the "RuleFactoryLite" module.
    *** @param v The notify email subject
    **/
    public void setNotifySubject(String v)
    {
        this.setFieldValue(FLD_notifySubject, ((v != null)? v : ""));
    }

    // ---

    /** 
    *** Gets the Email Body/Text for the triggered notification email .<br>
    *** This email body/text is currently only used by default with the "RuleFactoryLite" module.
    *** @return The notify email body
    **/
    public String getNotifyText()
    {
        String v = (String)this.getFieldValue(FLD_notifyText);
        return (v != null)? v : "";
    }

    /** 
    *** Sets the Email Body/Text for the triggered notification email .<br>
    *** This email body/text is currently only used by default with the "RuleFactoryLite" module.
    *** @param v The notify email body
    **/
    public void setNotifyText(String v)
    {
        String s = (v != null)? StringTools.encodeNewline(v) : "";
        this.setFieldValue(FLD_notifyText, s);
    }

    // ---

    /** 
    *** (OBSOLETE) Gets the configuration state indicating whether the email wrapper from the "private.xml"
    *** file should be used.<br>
    *** The method is obsolete and should not be used.
    *** @return The email wrapper configuration state
    **/
    public boolean getNotifyUseWrapper()
    {
        if (ALLOW_USE_EMAIL_WRAPPER) {
            Boolean v = (Boolean)this.getFieldValue(FLD_notifyUseWrapper);
            return (v != null)? v.booleanValue() : true;
        } else {
            return false;
        }
    }

    /** 
    *** (OBSOLETE) Sets the configuration state indicating whether the email wrapper from the "private.xml"
    *** file should be used.<br>
    *** The method is obsolete and should not be used.
    *** @param v The email wrapper configuration state
    **/
    public void setNotifyUseWrapper(boolean v)
    {
        this.setFieldValue(FLD_notifyUseWrapper, v);
    }

    // ---

    /** 
    *** (OBSOLETE) Gets the notification priority.<br>
    *** The method is obsolete and should not be used.
    *** @return The notification priority
    **/
    public int getNotifyPriority()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_notifyPriority);
        return (v != null)? v.intValue() : 0;
    }

    /** 
    *** (OBSOLETE) Sets the notification priority.<br>
    *** The method is obsolete and should not be used.
    *** @param v The notification priority
    **/
    public void setNotifyPriority(int v)
    {
        this.setOptionalFieldValue(FLD_notifyPriority, ((v < 0)? 0 : v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Clears the parked location state
    **/
    public void clearParkedLocation(boolean update)
        throws DBException
    {
        this.setParkedLocation(null, 0.0/*radius*/, 0.0/*speed*/, update);
    }

    /** 
    *** Sets the parked location state
    *** @param parkLoc   The GeoPoint of the location where the device should be "parked"
    *** @param parkRadM  The radius, in meters, of the parked location
    *** @param parkSpeed The maximum speed allowed within the parked location
    *** @param update    True to update the Device record now
    ***/
    public void setParkedLocation(GeoPoint parkLoc, double parkRadM, double parkSpeed, boolean update)
        throws DBException
    {
        String adID = this.getAccountID() + "/" + this.getDeviceID(); 
        if (!GeoPoint.isValid(parkLoc) || (parkRadM <= 0.0)) {
            Print.logInfo("["+adID+"] Clearing parked location");
            this.setParkedLatitude(0.0);                        // FLD_parkedLatitude
            this.setParkedLongitude(0.0);                       // FLD_parkedLongitude
            this.setParkedRadius(0.0);                          // FLD_parkedRadius
            this.setParkedMaxSpeedKPH(0.0);                     // FLD_parkedMaxSpeedKPH
        } else {
            Print.logInfo("["+adID+"] Setting parked location: " + parkLoc + " " + parkRadM + " m");
            this.setParkedLatitude(parkLoc.getLatitude());      // FLD_parkedLatitude
            this.setParkedLongitude(parkLoc.getLongitude());    // FLD_parkedLongitude
            this.setParkedRadius(parkRadM);                     // FLD_parkedRadius
            this.setParkedMaxSpeedKPH(parkSpeed);               // FLD_parkedMaxSpeedKPH
        }
        if (update) {
            this.update(
                Device.FLD_parkedLatitude,
                Device.FLD_parkedLongitude,
                Device.FLD_parkedRadius,
                Device.FLD_parkedMaxSpeedKPH
                ); // may throw DBException
        } else {
            this.addOtherChangedFieldNames(
                Device.FLD_parkedLatitude,
                Device.FLD_parkedLongitude,
                Device.FLD_parkedRadius,
                Device.FLD_parkedMaxSpeedKPH
                );
        }
    }

    /** 
    *** Saves the set parked location back to the Device table
    *** @throws DBException
    ***/
    public void saveParkedLocation()
        throws DBException
    {
        this.update(
            Device.FLD_parkedLatitude,
            Device.FLD_parkedLongitude,
            Device.FLD_parkedRadius,
            Device.FLD_parkedMaxSpeedKPH
            );
    }

    // --------------------------------

    /**
    *** Returns true if this Device is parked
    *** @return True if this Device is parked
    **/
    public boolean isParked()
    {
        if (this.getParkedRadius() <= 0.0) {
            return false;
        } else
        if (!GeoPoint.isValid(this.getParkedLatitude(),this.getParkedLongitude())) {
            return false;
        } else {
            return true;
        }
    }

    /**
    *** Returns true if the specified GeoPoint location indicates a "Park" violation
    *** @param gp  The current GeoPoint to test
    *** @return True if the specified GeoPoint location indicates a "Park" violation
    **/
    //public boolean isParkedViolation(GeoPoint gp)
    //{
    //    return this.isParkedViolation(gp, 0.0);
    //}

    /**
    *** Returns true if the specified GeoPoint location indicates a "Park" violation
    *** @param gp        The current GeoPoint to test
    *** @param speedKPH  The current speed to test
    *** @return True if the specified GeoPoint location indicates a "Park" violation
    **/
    public boolean isParkedViolation(GeoPoint gp, double speedKPH)
    {

        /* no point specified */
        if (!GeoPoint.isValid(gp)) {
            return false; // invalid point, no violation
        }

        /* get parked location */
        double parkLat = this.getParkedLatitude();
        double parkLon = this.getParkedLongitude();
        double parkRad = this.getParkedRadius();
        if (!GeoPoint.isValid(parkLat,parkLon) || (parkRad <= 0.0)) {
            return false; // not parked, no violation
        }
        GeoPoint parkLoc = new GeoPoint(parkLat,parkLon);

        /* outside of parked zone? */
        double distM = parkLoc.metersToPoint(gp);
        if (distM > parkRad) {
            // -- outside of parked radius
            return true;
        }

        /* speed check */
        if (speedKPH > 0.0) {
            // -- speed check requested
            double parkMaxSpd = this.getParkedMaxSpeedKPH();
            if ((parkMaxSpd > 0.0) && (speedKPH > parkMaxSpd)) {
                // -- exceeds maximum parked speed
                return true;
            }
        }

        /* no violation */
        return false;

    }

    // --------------------------------

    /**
    *** Gets the Parked Latitude
    *** @return The parked latitude
    **/
    public double getParkedLatitude()
    {
        return this.getOptionalFieldValue(FLD_parkedLatitude, 0.0);
    }

    /**
    *** Sets the Parked Latitude
    *** @param v The parked latitude
    **/
    public void setParkedLatitude(double v)
    {
        this.setOptionalFieldValue(FLD_parkedLatitude, v);
    }

    // --------------------------------

    /**
    *** Gets the Parked Longitude
    *** @return The parked Longitude
    **/
    public double getParkedLongitude()
    {
        return this.getOptionalFieldValue(FLD_parkedLongitude, 0.0);
    }

    /**
    *** Sets the Parked Longitude
    *** @param v The parked Longitude
    **/
    public void setParkedLongitude(double v)
    {
        this.setOptionalFieldValue(FLD_parkedLongitude, v);
    }

    // --------------------------------

    /**
    *** Gets the Parked GeoPoint
    *** @return The Parked GeoPoint, or an invalid GeoPoint (0/0) if not parked
    **/
    public GeoPoint getParkedLocation()
    {
        double pLat = this.getParkedLatitude();
        double pLon = this.getParkedLongitude();
        double pRad = this.getParkedRadius();
        if ((pRad > 0.0) && GeoPoint.isValid(pLat,pLon)) {
            return new GeoPoint(pLat,pLon);
        } else {
            return GeoPoint.INVALID_GEOPOINT;
        }
    }

    // --------------------------------

    /**
    *** Gets the parked radius, in meters
    *** @return The parked radius, in meters
    **/
    public double getParkedRadius()
    {
        return this.getOptionalFieldValue(FLD_parkedRadius, 0.0);
    }

    /**
    *** Sets the parked radius, in meters
    *** @param v The parked radius, in meters
    **/
    public void setParkedRadius(double v)
    {
        this.setOptionalFieldValue(FLD_parkedRadius, v);
    }

    // --------------------------------

    /**
    *** Gets the parked maximum speed km/h
    *** @return The parked maximum speed km/h
    **/
    public double getParkedMaxSpeedKPH()
    {
        return this.getOptionalFieldValue(FLD_parkedMaxSpeedKPH, 0.0);
    }

    /**
    *** Sets the parked maximum speed km/h
    *** @param v The parked maximum speed km/h
    **/
    public void setParkedMaxSpeedKPH(double v)
    {
        this.setOptionalFieldValue(FLD_parkedMaxSpeedKPH, v);
    }

    // --------------------------------

    /**
    *** Gets the parked address, if parked
    *** @return The parked address, if parked
    **/
    public String getParkedAddress()
    {
        return ""; // this.getOptionalFieldValue(FLD_parkedAddress, "");
    }

    /**
    *** Sets the parked address, if parked
    *** @param v The parked address, if parked
    **/
    public void setParkedAddress(String v)
    {
        //this.setOptionalFieldValue(FLD_parkedAddress, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the nearby proximity radius, in meters
    *** @return The nearby proximity radius, in meters
    **/
    public double getProximityRadius()
    {
        return this.getOptionalFieldValue(FLD_proximityRadius, 0.0);
    }

    /**
    *** Sets the nearby proximity radius, in meters
    *** @param v The nearby proximity radius, in meters
    **/
    public void setProximityRadius(double v)
    {
        this.setOptionalFieldValue(FLD_proximityRadius, v);
    }
    
    /**
    *** Gets the proximity bounding box.
    *** Returns null if a proximity bounding box is not applicable
    **/
    public GeoBounds getProximityBoundingBox(long asOfTime)
    {
        // -- get proximity radius
        double proxRadM = this.getProximityRadius();
        if (proxRadM <= 0.0) {
            // -- proximity check disabled
            return null;
        }
        // -- last location
        GeoPoint lastGP = this.getLastValidLocation();
        if (lastGP == null) {
            // -- unknown last location
            return null;
        }
        // -- check age
        long   proxAgeS = this.getProximityMaximumAge();
        long     lastTS = this.getLastGPSTimestamp();
        long      nowTS = (asOfTime > 0L)? asOfTime : DateTime.getCurrentTimeSec();
        if ((lastTS + proxAgeS) < nowTS) {
            // -- last location is too old
            return null;
        }
        // -- create/return GeoBounds
        return new GeoBounds(proxRadM, lastGP);
    }

    // --------------------------------

    /**
    *** Gets the nearby proximity Group ID <br>
    *** @return The groupID used for checking nearby devices
    **/
    public String getProximityGroupID()
    {
        String v = (String)this.getOptionalFieldValue(FLD_proximityGroupID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the nearby proximity Group ID <br>
    *** @param v  The nearby proximity group id
    **/
    public void setProximityGroupID(String v)
    {
        this.setOptionalFieldValue(FLD_proximityGroupID, StringTools.trim(v));
    }

    // --------------------------------

    /**
    *** Gets the nearby proximity location maximum age (in seconds)
    *** @return The nearby proximity location maximum age (in seconds)
    **/
    public long getProximityMaximumAge()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_proximityMaximumAge);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the nearby proximity location maximum age (in seconds)
    *** @param v  The nearby proximity location maximum age (in seconds)
    **/
    public void setProximityMaximumAge(long v)
    {
        this.setOptionalFieldValue(FLD_proximityMaximumAge, ((v > 0L)? v : 0L));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this Device record supports Border-Crossing
    *** @return True if this Device record supports Border-Crossing
    **/
    public static boolean supportsBorderCrossing()
    {
        return Device.getFactory().hasField(FLD_borderCrossing);
    }

    /**
    *** Returns true if Border-Crossing is enabled
    *** @return True if Border-Crossing is enabled
    **/
    public boolean isBorderCrossing()
    {
        return (this.getBorderCrossing() == Device.BorderCrossingState.ON.getIntValue());
    }

    /**
    *** Gets the Border-Crossing enabled state
    *** @return The Border-Crossing enabled state
    **/
    public int getBorderCrossing()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_borderCrossing);
        return (v != null)? v.intValue() : Device.BorderCrossingState.OFF.getIntValue();
        // -- Note the returned value of this flag may be ignored by 'BorderCrossing'
    }

    /**
    *** Sets the Border-Crossing enabled state
    *** @param flags The Border-Crossing enabled state
    **/
    public void setBorderCrossing(int flags)
    {
        this.setOptionalFieldValue(FLD_borderCrossing, flags);
    }

    /**
    *** Sets the Border-Crossing enabled state
    *** @param bcs The Border-Crossing enabled state
    **/
    public void setBorderCrossing(Device.BorderCrossingState bcs)
    {
        int bcf = (bcs != null)? bcs.getIntValue() : Device.BorderCrossingState.OFF.getIntValue();
        this.setBorderCrossing(bcf);
    }

    // ---

    /**
    *** Gets the last calculated Border-Crossing time (Unix Epoch format)
    *** @return The last calculated Border-Crossing time (Unix Epoch format)
    **/
    public long getLastBorderCrossTime()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_lastBorderCrossTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last calculated Border-Crossing time (Unix Epoch format)
    *** @param v The last calculated Border-Crossing time (Unix Epoch format)
    **/
    public void setLastBorderCrossTime(long v)
    {
        this.setOptionalFieldValue(FLD_lastBorderCrossTime, v);
    }

    // Device/Asset specific data above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DataTransport specific data below
    
    private static boolean CHECK_IMEI_FOR_MODEM_ID = false;
    private String modemID = "";

    /**
    *** Extracts the Mobile-ID from the IMEI# or Unique-ID<br>
    *** This method relies on the unique-id prefix to end with "_" or "-".
    *** @return The extracted Mobile-ID
    **/
    public String getModemID()
    {
        if (StringTools.isBlank(this.modemID)) {
            String imei = this.getImeiNumber();
            if (CHECK_IMEI_FOR_MODEM_ID && !StringTools.isBlank(imei)) {
                this.modemID = imei;
            } else {
                String uniqID = this.getUniqueID();
                if (!StringTools.isBlank(uniqID)) {
                    int p = uniqID.indexOf("_");
                    if (p < 0) { p = uniqID.indexOf("-"); }
                    if (p < 0) {
                        this.modemID = uniqID;
                    } else {
                        this.modemID = uniqID.substring(p+1);
                    }
                }
            }
        }
        return this.modemID;
    }

    /**
    *** Sets the preextracted Mobile-ID for this device
    *** @param mid  The Mobile-ID for this device
    **/
    public void setModemID(String mid)
    {
        // -- NOT stored in the Device table.  Only used by the caller
        this.modemID = StringTools.trim(mid);
    }

    // --------

    /**
    *** Gets the Unique-ID for this Device
    *** @return The Unique-ID
    **/
    public String getUniqueID()
    {
        String v = (String)this.getFieldValue(FLD_uniqueID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Unique-ID for this Device
    *** @param v The Unique-ID
    **/
    public void setUniqueID(String v)
    {
        String uniq = StringTools.stripChars(StringTools.trim(v),' '); // remove all spaces
        this.setFieldValue(FLD_uniqueID, uniq);
    }
    
    /**
    *** Validates a new UniqueID to make sure that it isn't already in use.
    *** Will return true if the specified UniqueID is blank 
    *** (blank is valid since it means that the UniqueID is to be cleared)
    **/
    public boolean validateUniqueID(String newUID)
        throws DBException
    {
        if (StringTools.isBlank(newUID)) {
            // -- UniqueID is to be cleared
            return true;
        } else
        if (this.getUniqueID().equalsIgnoreCase(newUID)) {
            // -- UniqueID already matches this Device
            return true;
        } else {
            // -- check for existing Device record 
            Device nd = Transport.loadDeviceByUniqueID(newUID);
            return (nd == null)? true : false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Device-Code (also called Server-ID) for this Device
    *** @return The Device-Code / Server-ID
    **/
    public String getDeviceCode()
    {
        String v = (String)this.getFieldValue(FLD_deviceCode);  // serverID
        return StringTools.trim(v);
    }

    /**
    *** Sets the Device-Code (also called Server-ID) for this Device
    *** @param v The Device-Code / Server-ID
    **/
    public void setDeviceCode(String v)
    {
        this.setFieldValue(FLD_deviceCode, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Device-Type for this Device
    *** @return The Device-Type
    **/
    public String getDeviceType()
    {
        String v = (String)this.getFieldValue(FLD_deviceType);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Device-Type for this Device
    *** @param v The Device-Type
    **/
    public void setDeviceType(String v)
    {
        this.setFieldValue(FLD_deviceType, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this device has a non-default DCS Properties ID
    *** @return True if this device has a non-default DCS Properties ID
    **/
    public boolean hasDcsPropertiesID()
    {
        String grpPropsID = this.getDcsPropertiesID();
        if (StringTools.isBlank(grpPropsID)) {
            // -- no dcs property id defined
            return false;
        } else
        if (grpPropsID.equalsIgnoreCase(DCServerConfig.DEFAULT_PROP_GROUP_ID)) {
            // -- "default" dcs property id defined
            return false;
        } else {
            // -- non-default dcs property id defined
            return true;
        }
    }

    /**
    *** Gets the DCS Properties ID assigned to this device (DCS Property ID)<br>
    *** Used by some DCS modules to select specific device configurations
    *** @return The DCS Property ID
    **/
    public String getDcsPropertiesID()
    {
        String v = (String)this.getFieldValue(FLD_dcsPropertiesID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the DCS Properties ID assigned to this device (DCS Property ID)<br>
    *** Used by some DCS modules to select specific device configurations
    *** @param v The DCS Property ID
    **/
    public void setDcsPropertiesID(String v)
    {
        this.setFieldValue(FLD_dcsPropertiesID, StringTools.trim(v));
    }

    /**
    *** Gets the DCS Property ID for the specified device
    *** @param device The Device
    *** @return The DCS Property ID
    **/
    public static String GetDcsPropertiesID(Device device)
    {

        /* no device */
        if (device == null) {
            return "";
        }

        /* Device defined? */
        String dcsPropsID = device.getDcsPropertiesID();
        if (!StringTools.isBlank(dcsPropsID)) {
            return dcsPropsID;
        }

        /* Account defined? */
        Account account = device.getAccount();
        if (account != null) {
            dcsPropsID = account.getDcsPropertiesID();
            if (!StringTools.isBlank(dcsPropsID)) {
                return dcsPropsID;
            }
        }

        /* not defined */
        return "";

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the DCS Command Host assigned to this device (ie. the host name
    *** where the DCS for this device is running)<br>
    *** May return blank to indicate that the default DCS command host should
    *** be used.
    *** @return The DCS Command Hostname
    **/
    public String getDcsCommandHost()
    {
        String v = (String)this.getFieldValue(FLD_dcsCommandHost);
        return StringTools.trim(v);
    }

    /**
    *** Returns true if this device defines a custom command host.
    *** @return True if a custom command host is defined.
    **/
    public boolean hasDcsCommandHost()
    {
        return !StringTools.isBlank(this.getDcsCommandHost());
    }

    /**
    *** Sets the DCS Command Host assigned to this device (ie. the host name
    *** where the DCS for this device is running)<br>
    *** May be blank to indicate that the default DCS command host should be
    *** used.
    *** @param v The DCS Command Hostname
    **/
    public void setDcsCommandHost(String v)
    {
        this.setFieldValue(FLD_dcsCommandHost, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    
    public static final String CmdState_command     = "command";
    public static final String CmdState_timestamp   = "timestamp";
    public static final String CmdState_state       = "state";

    /**
    *** Gets the current DCS Command State<br>
    *** Used by some DCS modules when sending compound commands to a device
    *** @return The DCS Command State
    **/
    public String getDcsCommandState()
    {
        // command=COMMAND timestamp=EPOCH state=300
        String v = (String)this.getFieldValue(FLD_dcsCommandState);
        return StringTools.trim(v);
    }

    /**
    *** Gets the current DCS Command State<br>
    *** Used by some DCS modules when sending compound commands to a device
    *** @return The DCS Command State
    **/
    public RTProperties getDcsCommandStateProperties()
    {
        // command=COMMAND timestamp=EPOCH state=300
        return new RTProperties(this.getDcsCommandState());
    }

    /**
    *** Sets the current DCS Command State<br>
    *** Used by some DCS modules to select specific device configurations
    *** @param v The DCS Command State
    **/
    public void setDcsCommandState(String v)
    {
        this.setFieldValue(FLD_dcsCommandState, StringTools.trim(v));
    }

    /**
    *** Sets the current DCS Command State<br>
    *** Used by some DCS modules to select specific device configurations
    *** @param v The DCS Command State
    **/
    public void setDcsCommandState(RTProperties v)
    {
        this.setDcsCommandStateProperties(v);
    }

    /**
    *** Sets the current DCS Command State<br>
    *** Used by some DCS modules to select specific device configurations
    *** @param v The DCS Command State properties
    **/
    public void setDcsCommandStateProperties(RTProperties v)
    {
        this.setDcsCommandState((v != null)? v.toString() : "");
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this Device has a specific defined pushpin-id
    *** @return True if this Device has a specific defined pushpin-id
    **/
    public boolean hasPushpinID()
    {
        return !StringTools.isBlank(this.getPushpinID());
    }

    /**
    *** Gets the defined pushpin-id, or blank if no pushpin-id is defined
    *** @return The defined pushpin-id, or blank if no pushpin-id is defined
    **/
    public String getPushpinID()
    {
        String v = (String)this.getFieldValue(FLD_pushpinID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the defined pushpin-id, or blank if no pushpin-id is defined
    *** @param v The defined pushpin-id, or blank if no pushpin-id is defined
    **/
    public void setPushpinID(String v)
    {
        this.setFieldValue(FLD_pushpinID, StringTools.trim(v));
    }

    /**
    *** Gets the defined pushpin-id, or blank if no pushpin-id is defined
    *** @param evalSelector True to check and evaluate rule selector
    *** @return The defined pushpin-id, or blank if no pushpin-id is defined
    **/
    public String getPushpinID(boolean evalSelector)
    {

        /* get unparsed device pushpin */
        String devPP = this.getPushpinID();
        if (!evalSelector) {
            // -- return as-is
            return devPP; // may be blank
        }

        /* return non-selector pushpinID */
        if (!devPP.startsWith("(")) {
            // -- not a rule selector
            return devPP;
        }

        /* get RuleFactory */
        RuleFactory rf = Device.getRuleFactory();
        if (rf == null) {
            // -- no RuleFactory, return blank
            return "";
        } else
        if (!rf.checkRuntime()) {
            // -- RuleFactory error, return blank
            Print.logWarn("Device RuleFactory pushpin selector failed.");
            return "";
        }

        /* evaluate rule selector */
        try {
            // -- 0----+----1----+----2----+----3-
            // -  ($RULE("otherrule",""))
            // -  ($DOW==Sun&&$IgnOn?"ignon":"")
            //Print.logInfo("Device Pushpin Selector: " + devPP);
            Object result = rf.evaluateSelector(devPP,this); // evaluate based on this Device instance
            if (result instanceof String) {
                // -- return String result
                return (String)result;
            } else
            if (result instanceof Number) {
                // -- return index result
                int iconNdx = ((Number)result).intValue();
                return (iconNdx >= 0)? ("#" + iconNdx) : ""; // ie. #8
            } else {
                // -- invalid result, return blank
                Print.logWarn("Device pushpin selector result is not a String: " + result);
                return "";
            }
        } catch (RuleParseException rpe) {
            // -- error
            Print.logError("Device pushpin selector parse error: " + rpe.getMessage());
            return "";
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this Device has a specific defined display color
    *** @return True if this Device has a specific defined display color
    **/
    public boolean hasDisplayColor()
    {
        return !StringTools.isBlank(this.getDisplayColor());
    }

    /**
    *** Gets the defined display-color
    *** @return The defined display-color
    **/
    public String getDisplayColor()
    {
        String v = (String)this.getFieldValue(FLD_displayColor);
        return StringTools.trim(v);
    }

    /**
    *** Gets the defined display-color, or returns the specified default color
    *** if not display-color is defined.
    *** @return The defined display-color
    **/
    public ColorTools.RGB getDisplayColor(ColorTools.RGB dft)
    {
        return ColorTools.parseColor(this.getDisplayColor(),dft);
    }

    /**
    *** Sets the display color
    *** @param v The display color
    **/
    public void setDisplayColor(ColorTools.RGB v)
    {
        this.setDisplayColor((v != null)? v.toString(true) : null);
    }

    /**
    *** Sets the display color
    *** @param v The display color
    **/
    public void setDisplayColor(String v)
    {
        this.setFieldValue(FLD_displayColor, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the map legend (currently not used)
    *** @return  The map legend
    **/
    public String getMapLegend()
    {
        return "";
    }

    /**
    *** Sets the map legend (currently not used)
    *** @param legend  The map legend
    **/
    public void setMapLegend(String legend)
    {
        //
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the assigned device serial number
    *** @return  The serial number
    **/
    public String getSerialNumber()
    {
        String v = (String)this.getFieldValue(FLD_serialNumber);
        return StringTools.trim(v);
    }

    /**
    *** Sets the assigned device serial number
    *** @param v  The serial number
    **/
    public void setSerialNumber(String v)
    {
        this.setFieldValue(FLD_serialNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the SIM phone number
    *** @return  The SIM phone number
    **/
    public String getSimPhoneNumber()
    {
        String v = (String)this.getFieldValue(FLD_simPhoneNumber);
        return StringTools.trim(v);
    }

    /**
    *** Sets the SIM phone number
    *** @param v  The SIM phone number
    **/
    public void setSimPhoneNumber(String v)
    {
        this.setFieldValue(FLD_simPhoneNumber, StringTools.trim(v));
    }

    /**
    *** Gets the SIM phone number
    *** @param digitsOnly  If true, the non-digit characters will be stripped from the returned value
    *** @return The SIM phone number
    **/
    public String getSimPhoneNumber(boolean digitsOnly)
    {
        String ph = this.getSimPhoneNumber();
        return digitsOnly? StringTools.stripNonDigitChars(ph) : ph;
    }

    /**
    *** Returns true if the SIM phone number is defined
    *** @return True if the SIM phone number is defined
    **/
    public boolean hasSimPhoneNumber() // fixedContactPhone
    {
        return !StringTools.isBlank(this.getSimPhoneNumber(true));
    }

    // --------------------------------

    /**
    *** Returns an array of Device-IDs for the specified SIM phone number
    *** @param simPhone  The SIM phone number
    *** @return And array of Device-IDs for the specified SIM phone number
    **/
    public static java.util.List<String> getDeviceIDsForSimPhoneNumber(String simPhone)
        throws DBException
    {
        return Device.getDeviceIDsForSimPhoneNumber(simPhone, (char)0);
    }

    /**
    *** Returns an array of Device-IDs for the specified SIM phone number
    *** @param simPhone  The SIM phone number
    *** @param sepCH     The preferred character for separating the account/device ids
    *** @return And array of Device-IDs for the specified SIM phone number
    **/
    public static java.util.List<String> getDeviceIDsForSimPhoneNumber(String simPhone, char sepCH)
        throws DBException
    {
        String sep = (sepCH == (char)0)? "," : String.valueOf(sepCH);

        /* Phone number specified? */
        if (StringTools.isBlank(simPhone)) {
            throw new DBException("SIM phone number not specified");
        }

        /* read devices */
        java.util.List<String> devList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Device WHERE (simPhoneNumber='12345')
            DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
            dsel.setSelectedFields(
                Device.FLD_accountID, 
                Device.FLD_deviceID, 
                Device.FLD_simPhoneNumber);
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE(
                dwh.EQ(Device.FLD_simPhoneNumber,simPhone)
                ));
            // Note: The index on the column FLD_simPhoneNumber is not unique
            // (since null/empty values are allowed and needed)
    
            /* get records */
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String acctId = rs.getString(FLD_accountID);
                String devId  = rs.getString(FLD_deviceID);
                devList.add(acctId + sep + devId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Get Device SimPhoneNumber", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        return devList;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the SIM-ID
    *** @return  The SIM-ID
    **/
    public String getSimID()
    {
        String v = (String)this.getFieldValue(FLD_simID);
        return StringTools.trim(v);
    }

    /**
    *** Gets the SIM-ID
    *** @param v  The SIM-ID
    **/
    public void setSimID(String v)
    {
        this.setFieldValue(FLD_simID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the SMS email address for this device.<br>
    *** Used for sending commands to the device using email-to-SMS.
    *** @return  The SMS email address for this device
    **/
    public String getSmsEmail()
    {
        String v = (String)this.getFieldValue(FLD_smsEmail);
        return StringTools.trim(v);
    }

    /**
    *** Sets the SMS email address for this device.<br>
    *** Used for sending commands to the device using email-to-SMS.
    *** @param v  The SMS email address for this device
    **/
    public void setSmsEmail(String v)
    {
        this.setFieldValue(FLD_smsEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the IMEI# (or ESN) for this device.
    *** @return  The IMEI# for this device
    **/
    public String getImeiNumber()
    {
        String v = (String)this.getFieldValue(FLD_imeiNumber);
        return StringTools.trim(v);
    }

    /**
    *** Gets the IMEI# (or ESN) for this device.
    *** @param v  The IMEI# for this device
    **/
    public void setImeiNumber(String v)
    {
        this.setFieldValue(FLD_imeiNumber, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified key matches the "dataKey" (also call PIN) for this device
    *** @param _dk The data key (PIN) to check
    *** @return True if the keys match
    **/
    public boolean validateDataKey(String _dk)
    {
        String pin = StringTools.trim(_dk);
        // -- check for a valid key/pin here
        String dkey = this.getDataKey();
        if (!StringTools.isBlank(dkey)) {
            // -- dataKey is non-blank ("pin" required)
            return dkey.equals(pin);
        } else {
            // -- dataKey is blank
            //return dkey.equals(pin); // "pin" must also be blank
            return true; // "pin" ignored iff dataKey is blank
        }
    }

    /**
    *** Returns true if this device defines a data-key
    *** @return True if this device defines a data-key
    **/
    public boolean hasDataKey()
    {
        return !StringTools.isBlank(this.getDataKey());
    }

    /**
    *** Gets the data key (PIN) for this device 
    *** @return The Data key (PIN) for this device
    **/
    public String getDataKey()
    {
        String v = (String)this.getFieldValue(FLD_dataKey);
        return StringTools.trim(v);
    }

    /**
    *** Gets the data key (PIN) for this device, as a byte array
    *** @return The Data key (PIN) for this device, as a byte array
    **/
    public byte[] getDataKeyAsByteArray()
    {
        String dk = this.getDataKey();
        if (dk.startsWith("0x") || dk.startsWith("0X")) {
            return StringTools.parseHex(dk, null);
        } else {
            return dk.getBytes();
        }
    }

    /**
    *** Gets the data key (PIN) for this device 
    *** @param v The Data key (PIN) for this device
    **/
    public void setDataKey(String v)
    {
        this.setFieldValue(FLD_dataKey, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the specified bit state of the last digital input received
    *** @return The specified bit state of the last digital input received
    **/
    public boolean getLastInputState(int bit) // see also "getCommandStateMaskBit"
    {
        long mask = this.getLastInputState();
        DCServerConfig dcs = this.getDCServerConfig();
        if (dcs != null) {
            return dcs.getDigitalInputState(mask, bit);
        } else {
            return ((mask & (1L << bit)) != 0L);
        }
    }

    /**
    *** Gets the bit mask of the last digital input received<br>
    *** see also EventData.getInputMask()
    *** @return The bit mask of the last digital input received
    **/
    public long getLastInputState()
    {
        Long v = (Long)this.getFieldValue(FLD_lastInputState);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the bit mask of the last digital input received
    *** @param v The bit mask of the last digital input received
    **/
    public void setLastInputState(long v)
    {
        this.setFieldValue(FLD_lastInputState, v & 0xFFFFFFFFL); // 32-bits only
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the specified bit state of the last digital output received
    *** @return The specified bit state of the last digital output received
    **/
    public boolean getLastOutputState(int bit)
    {
        long mask = this.getLastOutputState();
        DCServerConfig dcs = this.getDCServerConfig();
        if (dcs != null) {
            return dcs.getDigitalOutputState(mask, bit);
        } else {
            return ((mask & (1L << bit)) != 0L);
        }
    }

    /**
    *** Gets the bit mask of the last digital output received<br>
    *** see also EventData.getOutputMask()
    *** @return The bit mask of the last digital output received
    **/
    public long getLastOutputState()
    {
        Long v = (Long)this.getFieldValue(FLD_lastOutputState);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the bit mask of the last digital output received
    *** @param v The bit mask of the last digital output received
    **/
    public void setLastOutputState(long v)
    {
        this.setFieldValue(FLD_lastOutputState, v & 0xFFFFFFFFL); // 32-bits only
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the StatusCode state for the specified bit
    *** @param bit The bit index
    *** @return The StatusCode state for the specified bit
    **/
    public boolean getStatusCodeStateBit(int bit)
    {
        long mask = this.getStatusCodeState();
        return ((mask & (1L << bit)) != 0L);
    }

    /**
    *** Sets the StatusCode state for the specified bit
    *** @param bit    The bit index
    *** @param state  The bit state
    **/
    public void setStatusCodeStateBit(int bit, boolean state)
    {
        long mask = this.getStatusCodeState();
        if (state) {
            mask |= (1L << bit);
        } else {
            mask &= ~(1L << bit);
        }
        this.setStatusCodeState(mask);
    }

    /**
    *** Gets the bit mask of the StatusCode on/off state
    *** @return The bit mask of the StatusCode on/off state
    **/
    public long getStatusCodeState()
    {
        Long v = (Long)this.getFieldValue(FLD_statusCodeState);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the bit mask of the StatusCode on/off state
    *** @param v The bit mask of the StatusCode on/off state
    **/
    public void setStatusCodeState(long v)
    {
        this.setFieldValue(FLD_statusCodeState, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last battery level recieved
    *** @return The last battery level recieved
    **/
    public double getLastBatteryLevel()
    {
        Double v = (Double)this.getFieldValue(FLD_lastBatteryLevel);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the last battery level recieved
    *** @param v The last battery level recieved
    **/
    public void setLastBatteryLevel(double v)
    {
        this.setFieldValue(FLD_lastBatteryLevel, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last battery volts recieved
    *** @return The last battery volts recieved
    **/
    public double getLastBatteryVolts()
    {
        Double v = (Double)this.getFieldValue(FLD_lastBatteryVolts);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the last battery volts recieved
    *** @param v The last battery volts recieved
    **/
    public void setLastBatteryVolts(double v)
    {
        this.setFieldValue(FLD_lastBatteryVolts, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the last vehicle battery volts recieved [2.6.4-B35]
    *** @return The last battery volts recieved
    **/
    public double getLastVBatteryVolts()
    {
        Double v = (Double)this.getFieldValue(FLD_lastVBatteryVolts); // [2.6.4-B35]
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the last vehicle battery volts recieved
    *** @param v The last battery volts recieved
    **/
    public void setLastVBatteryVolts(double v)
    {
        this.setFieldValue(FLD_lastVBatteryVolts, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last fuel level recieved tank #1
    *** @return The last fuel level recieved tank #1
    **/
    public double getLastFuelLevel()
    {
        Double v = (Double)this.getFieldValue(FLD_lastFuelLevel);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the last fuel level recieved tank #1
    *** @param v The last fuel level recieved tank #1
    **/
    public void setLastFuelLevel(double v)
    {
        this.setFieldValue(FLD_lastFuelLevel, ((v >= 0.0)? v : 0.0));
    }

    // --------------------------------

    /**
    *** Gets the last fuel level recieved for tank #2
    *** @return The last fuel level recieved for tank #2
    **/
    public double getLastFuelLevel2()
    {
        Double v = (Double)this.getFieldValue(FLD_lastFuelLevel2);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the last fuel level recieved for tank #2
    *** @param v The last fuel level recieved for tank #2
    **/
    public void setLastFuelLevel2(double v)
    {
        this.setFieldValue(FLD_lastFuelLevel2, ((v >= 0.0)? v : 0.0));
    }

    // --------------------------------

    private double _fuelLevelTotal = 0.0;

    /**
    *** Gets the last fuel level recieved
    *** @return The last fuel level recieved
    **/
    public double getLastFuelLevel(Device.FuelTankIndex tank)
    {
        if (tank != null) {
            switch (tank) {
                case TANK_1 : return this.getLastFuelLevel();
                case TANK_2 : return this.getLastFuelLevel2();
                case TOTAL  :
                    if (this._fuelLevelTotal > 0.0) {
                        return this._fuelLevelTotal; // previously set total level
                    } else {
                        double allCapL = this.getFuelCapacity(Device.FuelTankIndex.TOTAL);
                        return (allCapL > 0.0)? (this.getLastFuelRemain() / allCapL) : 0.0;
                    }
            }
        }
        return 0.0;
    }

    /**
    *** Sets the last fuel level recieved
    *** @param tank fuel level tank index
    *** @param v    The last fuel level recieved
    **/
    public void setLastFuelLevel(Device.FuelTankIndex tank, double v)
    {
        if (tank != null) {
            switch (tank) {
                case TANK_1 : this.setLastFuelLevel(v);  break;
                case TANK_2 : this.setLastFuelLevel2(v); break;
                case TOTAL  : this._fuelLevelTotal = (v > 0.0)? v : 0.0; break;
            }
        }
    }

    /**
    *** Gets the last fuel remaining (Litres) based on the fuel levels and capacities.
    **/
    public double getLastFuelRemain()
    {
        double fuelRem = 0.0;
        for (Device.FuelTankIndex T : Device.FuelTankIndex.values()) {
            if (!T.isTank()) { continue; }
            fuelRem += this.getLastFuelLevel(T) * this.getFuelCapacity(T);
        }
        return fuelRem;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last fuel total recieved
    *** @return The last fuel total recieved
    **/
    public double getLastFuelTotal()
    {
        Double v = (Double)this.getFieldValue(FLD_lastFuelTotal);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the last fuel total recieved
    *** @param v The last fuel total recieved
    **/
    public void setLastFuelTotal(double v)
    {
        this.setFieldValue(FLD_lastFuelTotal, ((v >= 0.0)? v : 0.0));
    }

    /** 
    *** Gets the fuel used within the specified time range
    **/
    public double getFuelUsedInRange(long startTime, long endTime)
    {
        long limit = 5L;
        long timeOfs = 120L; // seconds

        /* start time must be specified */
        if (startTime <= 0L) {
            return -1.0;
        }

        /* get starting events */
        EventData evStrBest = null;
        try {
            EventData evStr[] = this.getRangeEvents(
                (startTime - timeOfs), -1/*timeEnd*/,
                null, // statusCodes[]
                false, // validGPS
                EventData.LimitType.FIRST, limit);
            if (ListTools.isEmpty(evStr)) {
                // no starting events
                return -1.0;
            }
            for (int i = 0; i < evStr.length; i++) {
                long   ts       = evStr[i].getTimestamp();
                double fuelUsed = evStr[i].getFuelTotal();
                if (fuelUsed > 0.0) {
                    evStrBest = evStr[i];
                }
                if ((ts >= startTime) && (evStrBest != null)) {
                    // we've past the startTime event and we have a valid fuel-used event
                    break;
                }
            }
            if (evStrBest == null) {
                // none of the events had fuel-used information
                return -1.0;
            }
        } catch (DBException dbe) {
            Print.logException("Getting starting fuel events", dbe);
            return -1.0;
        }

        /* get ending events */
        EventData evEndBest = null;
        try {
            EventData evEnd[] = this.getRangeEvents(
                -1/*timeStart*/, ((endTime > 0L)? (endTime + timeOfs) : -1L),
                null, // statusCodes[]
                false, // validGPS
                EventData.LimitType.LAST, limit);
            if (ListTools.isEmpty(evEnd)) {
                // no ending events
                return -1.0;
            }
            for (int i = evEnd.length - 1; i >= 0; i--) {
                long   ts       = evEnd[i].getTimestamp();
                double fuelUsed = evEnd[i].getFuelTotal();
                if (fuelUsed > 0.0) {
                    evEndBest = evEnd[i];
                }
                if (((endTime <= 0L) || (ts <= endTime)) && (evEndBest != null)) {
                    // we've past the endTime event and we have a valid fuel-used event
                    break;
                }
            }
            if (evStrBest == null) {
                // none of the events had fuel-used information
                return -1.0;
            }
        } catch (DBException dbe) {
            Print.logException("Getting ending fuel events", dbe);
            return -1.0;
        }

        /* calculate fuel usage delta */
        // evStrBest/evEndBest are non null, and have a positive fuelTotal value
        double fuelUsedStr = evStrBest.getFuelTotal();
        double fuelUsedEnd = evEndBest.getFuelTotal();
        if (fuelUsedEnd >= fuelUsedStr) {
            return (fuelUsedEnd - fuelUsedStr);
        } else {
            // invalid fuel total values
            return -1.0;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last oil level recieved
    *** @return The last oil level recieved
    **/
    public double getLastOilLevel()
    {
        Double v = (Double)this.getFieldValue(FLD_lastOilLevel);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the last oil level recieved
    *** @param v The last oil level recieved
    **/
    public void setLastOilLevel(double v)
    {
        this.setFieldValue(FLD_lastOilLevel, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the bit index within the input mask which indicates the ignition state.
    *** If the ignition state is not indicated by a bit in the input state, but rather
    *** specific IGNITION_ON/IGNITION_OFF status code events, then this method returns "99".
    *** Returns "-1" if no ignition state bit index is defined.
    *** @return The ignition state but index.
    **/
    public int getIgnitionIndex()
    {
        Integer v = (Integer)this.getFieldValue(FLD_ignitionIndex);
        return (v != null)? v.intValue() : -1;
    }

    /**
    *** Sets the bit index for the inputMask ignition state indicator.
    *** @param v  The bit index, or 99 [StatusCodes.IGNITION_INPUT_INDEX] to indicate IGNITION_ON/IGNITION_OFF status codes
    **/
    public void setIgnitionIndex(int v)
    {
        // -- May be one of the following:
        // -  StatusCodes.IGNITION_UNKNOWN     [ -1]
        // -  Bit index 0..63
        // -  StatusCodes.IGNITION_INPUT_INDEX [ 99]
        // -  StatusCodes.IGNITION_START_STOP  [200]
        int ignNdx = this.getIgnitionIndex();
        if (ignNdx != v) {
            // -- ignition state has changed
            this.setFieldValue(FLD_ignitionIndex, ((v >= 0)? v : StatusCodes.IGNITION_UNKNOWN));
            // -- reset last ignition on/off times
            this.setLastIgnitionOnHours(0.0);   // FLD_lastIgnitionOnHours
            this.setLastIgnitionOnTime(0L);     // FLD_lastIgnitionOnTime
            this.setLastIgnitionOffTime(0L);    // FLD_lastIgnitionOffTime
            // -- assuming the ignition on/off times will get updated when the inginition index does
        }
    }

    /**
    *** Returns a 2 element array indicating the status codes which indicate the ignition state.
    *** The first element represents the ignition-off status code, and the second represents
    *** the ignition-on status code.  Returns null if no ignition state status codes are defined.
    *** @return The status codes indicating the ignition state, or null if not defined
    **/
    public int[] getIgnitionStatusCodes()
    {
        int ndx = this.getIgnitionIndex();
        if (ndx < 0) {
            return null;
        } else {
            int scOFF = StatusCodes.GetDigitalInputStatusCode(ndx, false);
            int scON  = StatusCodes.GetDigitalInputStatusCode(ndx, true );
            if (scOFF != StatusCodes.STATUS_NONE) {
                return new int[] { scOFF, scON };
            } else {
                return null;
            }
        }
    }

    /* ignition state check */
    private static boolean CHECK_LAST_EVENT_IGNITION = false;
    /**
    *** Returns the current ignition state<br>
    ***  0 = Off<br>
    ***  1 = On
    *** -1 = Unknown<br>
    *** @return The current ignition state for this device.
    **/
    public int getCurrentIgnitionState()
    {
        boolean checkSC = RTConfig.getBoolean(DBConfig.PROP_Device_checkLastEventIgnitionState,CHECK_LAST_EVENT_IGNITION);
        return this.getCurrentIgnitionState(checkSC, true);
    }

    /**
    *** Returns the specified ignition state as a String representation<br>
    ***  0 = "Off"<br>
    ***  1 = "On"
    *** -1 = "Unknown"<br>
    *** @return The specified ignition state String representation.
    **/
    public String getIgnitionStateString(int ignState)
    {
        I18N i18n = I18N.getI18N(Device.class, Account.GetLocale(this.getAccount()));
        switch (ignState) {
            case 0:  return i18n.getString("Device.ignitionOff"    , "Off");
            case 1:  return i18n.getString("Device.ignitionOn"     , "On");
            default: return i18n.getString("Device.ignitionUnknown", "Unknown");
        }
    }

    /**
    *** Returns the current ignition state as a String representation<br>
    ***  0 = "Off"<br>
    ***  1 = "On"
    *** -1 = "Unknown"<br>
    *** @return The current ignition state String representation for this device.
    **/
    public String getCurrentIgnitionStateString()
    {
        return this.getIgnitionStateString(this.getCurrentIgnitionState());
    }

    /**
    *** Returns true if the specified status code represents an Ignition-On for this device
    **/
    public boolean isIgnitionOn(int code)
    {
        int ignSC[] = this.getIgnitionStatusCodes();
        if (ignSC == null) {
            return false;
        } else {
            return (ignSC[1] == code)? true : false;
        }
    }

    /**
    *** Returns true if the specified status code represents an Ignition-Off for this device
    **/
    public boolean isIgnitionOff(int code)
    {
        int ignSC[] = this.getIgnitionStatusCodes();
        if (ignSC == null) {
            return false;
        } else {
            return (ignSC[0] == code)? true : false;
        }
    }

    /* ignition state cache */
    private int cacheIgnitionState = -2;

    /**
    *** Returns the current ignition state<br>
    *** -1 = unknown<br>
    ***  0 = off<br>
    ***  1 = on
    *** @param checkSC True to check ignition state based on the most recent event status codes
    *** @param update True to update Device last ignition on/off times
    *** @return The current ignition state for this device.
    **/
    public int getCurrentIgnitionState(boolean checkSC, boolean update)
    {

        /* check last ignition on/off times */
        long lastIgnOn  = this.getLastIgnitionOnTime();
        long lastIgnOff = this.getLastIgnitionOffTime();
        if ((lastIgnOn > 0L) || (lastIgnOff > 0L)) { // [2.6.2-B38] (changed to 'or')
            // -- ignition state should be known
            if ((lastIgnOn > 0L) && (lastIgnOn > lastIgnOff)) {
                // -- last ignition was on
                this.cacheIgnitionState = 1;
                //Print.logInfo("Last Ignition-On was after last Ignition-Off: "+this.cacheIgnitionState+" LastIgnOn="+lastIgnOn+" LastIgnOff="+lastIgnOff);
                return this.cacheIgnitionState;
            } else
            if ((lastIgnOff > 0L) && (lastIgnOff > lastIgnOn)) {
                // -- last ignition was off
                this.cacheIgnitionState = 0;
                //Print.logInfo("Last Ignition-Off was after last Ignition-On: "+this.cacheIgnitionState+" LastIgnOn="+lastIgnOn+" LastIgnOff="+lastIgnOff);
                return this.cacheIgnitionState;
            } else {
                // (lastIgnOff == lastIgnOn) unlikely (but can occur)
                //this.setLastIgnitionOffTime(0L);             // FLD_lastIgnitionOffTime
                //this.setLastIgnitionOnTime(0L);              // FLD_lastIgnitionOnTime
                // TODO: ? this.setLastIgnitionOnHours(0.0); // FLD_lastIgnitionOnHours
            }
        }

        /* get ignition state index */
        int ignNdx = this.getIgnitionIndex();
        if (ignNdx < 0) {
            // -- no ignition input specified
            this.cacheIgnitionState = -1; //  unknwon
            //Print.logInfo("Unknown Ignition Index: " + this.cacheIgnitionState);
            return this.cacheIgnitionState;
        }

        /* check inputMask if we are not checking a specific status code */
        if (ignNdx < StatusCodes.IGNITION_INPUT_INDEX) {
            // -- ignition state expressed as digital-input
            this.cacheIgnitionState = this.getLastInputState(ignNdx)? 1 : 0;
            //Print.logInfo("Ignition State expresses as digital input: " + this.cacheIgnitionState);
            return this.cacheIgnitionState;
        }

        /* check for status code based ignition definition? */
        int ignSC[] = this.getIgnitionStatusCodes();
        if (ignSC == null) {
            // -- no status code definition
            this.cacheIgnitionState = -1;
            //Print.logInfo("Unknown Ignition StatusCode: " + this.cacheIgnitionState);
            return this.cacheIgnitionState;
        }

        /* if not checking last event, return unknown now */
        if (!checkSC) {
            this.cacheIgnitionState = -1;
            //Print.logInfo("Unknown Ignition State: " + this.cacheIgnitionState);
            return this.cacheIgnitionState;
        }

        // -- the above executes quickly
        // -------------------------------------
        // -- the below may need to query the db

        /* already determined? */
        if (this.cacheIgnitionState >= -1) {
            // -- already initialized
            //Print.logInfo("Previously Cached Ignition State: " + this.cacheIgnitionState);
            return this.cacheIgnitionState;
        }

        /* look for the last ignition state based on a status code */
        // -- non-optimized 
        try {
            EventData ev = this.getLastEvent(ignSC);
            if (ev == null) {
                // -- no such event
                this.cacheIgnitionState = -1;
                //Print.logInfo("Last Ignition State Event: " + this.cacheIgnitionState);
            } else
            if (ev.getStatusCode() == ignSC[0]) {
                // -- Ignition-OFF
                this.cacheIgnitionState = 0;
                //Print.logInfo("Last Ignition State Off: " + this.cacheIgnitionState);
                long ignTS = ev.getTimestamp();
                this.setLastIgnitionOffTime(ignTS);             // FLD_lastIgnitionOffTime
                if (ignTS < this.getLastIgnitionOnTime()) {
                    // -- make sure last Ignition-On time reflects "Ignition-OFF"
                    this.setLastIgnitionOnHours(0.0);           // FLD_lastIgnitionOnHours
                    this.setLastIgnitionOnTime(0L);             // FLD_lastIgnitionOnTime
                }
                // -- set update fields
                boolean didUpdate = false;
                if (update) {
                    // -- try to update fields now
                    try {
                        this.update(
                            Device.FLD_lastIgnitionOnHours,
                            Device.FLD_lastIgnitionOnTime,
                            Device.FLD_lastIgnitionOffTime);
                        didUpdate = true;
                    } catch (DBException dbe) {
                        didUpdate = false;
                    }
                }
                if (!didUpdate) {
                    // -- if we did not update here, tag these fields as changed
                    this.addOtherChangedFieldNames(
                        Device.FLD_lastIgnitionOnHours,
                        Device.FLD_lastIgnitionOnTime,
                        Device.FLD_lastIgnitionOffTime);
                }
            } else
            if (ev.getStatusCode() == ignSC[1]) {
                // -- Ignition-ON
                this.cacheIgnitionState = 1;
                //Print.logInfo("Last Ignition State On: " + this.cacheIgnitionState);
                double ignH = this.getLastIgnitionHours();
                long  ignTS = ev.getTimestamp();
                this.setLastIgnitionOnHours(ignH);              // FLD_lastIgnitionOnHours
                this.setLastIgnitionOnTime(ignTS);              // FLD_lastIgnitionOnTime
                if (ignTS < this.getLastIgnitionOffTime()) {
                    // -- make sure last ignition Off time reflects "Ignition-ON"
                    this.setLastIgnitionOffTime(0L);            // FLD_lastIgnitionOffTime
                }
                // -- set update fields
                boolean didUpdate = false;
                if (update) {
                    // -- try to update fields now
                    try {
                        this.update(
                            Device.FLD_lastIgnitionOnHours,
                            Device.FLD_lastIgnitionOnTime,
                            Device.FLD_lastIgnitionOffTime);
                        didUpdate = true;
                    } catch (DBException dbe) {
                        didUpdate = false;
                    }
                }
                if (!didUpdate) {
                    // -- if we did not update here, tag these fields as changed
                    this.addOtherChangedFieldNames(
                        Device.FLD_lastIgnitionOnHours,
                        Device.FLD_lastIgnitionOnTime,
                        Device.FLD_lastIgnitionOffTime);
                }
            }
        } catch (DBException dbe) {
            // -- set to 'unknown' on error
            this.cacheIgnitionState = -1;
            //Print.logInfo("Error Ignition State: " + this.cacheIgnitionState);
        }
        return this.cacheIgnitionState;

    }

    /**
    *** Returns the ignition state as-of the specified Event<br>
    *** -1 = unknown<br>
    ***  0 = off<br>
    ***  1 = on
    **/
    public int getIgnitionStateAsOfEvent(EventData ev)
    {
        boolean checkSC = RTConfig.getBoolean(DBConfig.PROP_Device_checkLastEventIgnitionState,CHECK_LAST_EVENT_IGNITION);
        return this.getIgnitionStateAsOfEvent(ev, checkSC);
    }
    
    /**
    *** Returns the ignition state as-of the specified Event<br>
    *** -1 = unknown<br>
    ***  0 = off<br>
    ***  1 = on
    **/
    public int getIgnitionStateAsOfEvent(EventData ev, boolean checkLastSC)
    {

        /* not event? */
        if (ev == null) {
            // -- event is null, return current device ignition state
            return this.getCurrentIgnitionState();
        }

        /* check EventData "ignitionState" */
        switch (EventData._getIgnitionState(ev)) {
            case ON : return 1;
            case OFF: return 0;
            default : break; // unknown, continue below
        }

        /* get ignition state index */
        int ignNdx = this.getIgnitionIndex();
        if (ignNdx < 0) {
            // -- no ignition bit specified
            return -1; // UNKNOWN
        }

        /* check inputMask if we are not checking a specific status code */
        if (ignNdx <= 63) { // StatusCodes.IGNITION_INPUT_INDEX
            // -- ignition state expressed as digital-input
            long mask = ev.getInputMask();
            DCServerConfig dcs = this.getDCServerConfig();
            if (dcs != null) {
                return dcs.getDigitalInputState(mask, ignNdx)? 1 : 0;
            } else {
                return ((mask & (1L << ignNdx)) != 0L)? 1 : 0;
            }
        }

        /* check for status code based ignition definition? */
        int ignSC[] = this.getIgnitionStatusCodes();
        if (ignSC == null) {
            // -- no status code definition
            return -1;
        }

        /* check event for matching status code */
        if (ev.getStatusCode() == ignSC[0]) {
            return 0; // ignition explicitly off
        } else
        if (ev.getStatusCode() == ignSC[1]) {
            return 1; // ignition explicitly on
        }

        /* look for StatusCodes.IGNITION_[ON|OFF]? */
        if (checkLastSC) {
            try {
                EventData priorEV = this.getLastEvent(ignSC, ev.getTimestamp(), false);
                if (priorEV != null) {
                    return (priorEV.getStatusCode() == ignSC[1])? 1 : 0;
                }
            } catch (DBException dbe) {
                // -- ignore, return unknown below
            }
        }

        /* unknown */
        return -1;

    }

    /**
    *** Returns the ignition state change of the specified Event<br>
    ***  -1 = no change<br>
    ***   0 = changed to off<br>
    ***   1 = changed to on
    **/
    public int getEventIgnitionState(EventData ev)
    {

        /* no event? */
        if (ev == null) {
            // -- event is null
            return -1;
        }

        /* ignition state vars */
        int ignNdx = this.getIgnitionIndex();
        if (ignNdx < 0) {
            // -- no defined ignition indicator
            return -1;
        }

        /* ignition status code */
        if (ignNdx >= StatusCodes.IGNITION_INPUT_INDEX) {
            // -- ignition state expressed as custom status-code
            int evSC    = ev.getStatusCode();
            int ignSC[] = this.getIgnitionStatusCodes();
            if (ignSC == null) {
                return -1;  // unknown
            } else
            if (evSC == ignSC[0]) {
                return 0;   // ignition off
            } else
            if (evSC == ignSC[1]) {
                return 1;   // ignition on
            } else {
                return -1;  // unknown (or no change)
            }
        }

        /* check input mask state change */
        boolean lastIgnState = this.getLastInputState(ignNdx);
        boolean evntIgnState = ev.getInputMaskBitState(ignNdx);
        if (lastIgnState == evntIgnState) {
            // -- no change 
            return -1;
        } else
        if (evntIgnState) {
            // -- (lastIgnState == false) && (evntIgnState == true)
            return 1;   // ignition state turned from OFF to ON
        } else {
            // -- (lastIgnState == true) && (evntIgnState == false)
            return 0;   // ignition state turned from ON to OFF
        }

    }

    /**
    *** Returns the ignition state based on the specified digital input mask.
    *** Returns false if this device ignition state is based on Ignition On/Off status codes
    *** @param gpioInput  The digital input mask
    *** @return True if the ignition bit index is on, false otherwise.
    **/
    public boolean getDigitalInputIgnitionState(long gpioInput)
    {

        /* no input mask? */
        if (gpioInput < 0L) {
            // -- no digital input value
            return false;
        }

        /* get digital input bit index */
        int bitNdx = this.getIgnitionIndex();
        if (bitNdx < 0) {
            // --- unknown/undefined ignition state
            return false;
        } else
        if (bitNdx > 63) {
            // -- may be one of the following
            // -    StatusCodes.IGNITION_INPUT_INDEX
            // -    StatusCodes.IGNITION_START_STOP
            return false;
        }

        /* return ignition state */
        return ((gpioInput & (1L << bitNdx)) != 0)? true : false;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the code/firmware version of this Device
    *** @return the code/firmware version of this Device
    **/
    public String getCodeVersion()
    {
        String v = (String)this.getFieldValue(FLD_codeVersion);
        return StringTools.trim(v);
    }

    /**
    *** Sets the code/firmware version of this Device
    *** @param v the code/firmware version of this Device
    **/
    public void setCodeVersion(String v)
    {
        this.setFieldValue(FLD_codeVersion, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the feature set of this Device
    *** @return the feature set of this Device
    **/
    public String getFeatureSet()
    {
        String v = (String)this.getFieldValue(FLD_featureSet);
        return StringTools.trim(v);
    }

    /**
    *** Sets the feature set of this Device
    *** @param v The feature set of this Device
    **/
    public void setFeatureSet(String v)
    {
        this.setFieldValue(FLD_featureSet, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the valid assigned IP address/mask for this Device<br>
    *** (used for validating incoming IP addresses used by this Device)
    *** @return The valid assigned IP address/mask for this Device
    **/
    public DTIPAddrList getIpAddressValid()
    {
        DTIPAddrList v = (DTIPAddrList)this.getFieldValue(FLD_ipAddressValid);
        return v; // May return null!!
    }

    /**
    *** Sets the valid assigned IP address/mask for this Device<br>
    *** (used for validating incoming IP addresses used by this Device)
    *** @param v The valid assigned IP address/mask for this Device
    **/
    public void setIpAddressValid(DTIPAddrList v)
    {
        this.setFieldValue(FLD_ipAddressValid, v);
    }

    /**
    *** Sets the valid assigned IP address/mask for this Device<br>
    *** (used for validating incoming IP addresses used by this Device)
    *** @param v The valid assigned IP address/mask for this Device
    **/
    public void setIpAddressValid(String v)
    {
        this.setIpAddressValid((v != null)? new DTIPAddrList(v) : null);
    }

    /**
    *** Returns true if the specified IP address matches the IP address/mask
    *** assigned to this Device.
    *** (used for validating incoming IP addresses used by this Device)
    *** @param ipAddr The IP address the Device is currently using to send data to the server
    *** @return True if IP address matches
    **/
    public boolean isValidIPAddress(String ipAddr)
    {
        DTIPAddrList ipList = this.getIpAddressValid();
        if ((ipList == null) || ipList.isEmpty()) {
            return true;
        } else
        if (!ipList.isMatch(ipAddr)) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this device has an fixed/static assigned TCP session-id
    *** @return True if this device has an fixed/static assigned TCP session-id
    **/
    public boolean hasFixedTcpSessionID()
    {
        return !StringTools.isBlank(this.getFixedTcpSessionID());
    }

    /**
    *** Gets the fixed/static TCP session ID
    *** @return The fixed/static TCP session ID
    **/
    public String getFixedTcpSessionID()
    {
        // -- used for sending commands to a device over a specific IP:port
        // -  (currently used by the JVC/Kenwood DCS)
        String v = (String)this.getFieldValue(FLD_fixedTcpSessionID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the fixed/static TCP session ID
    *** @param v The fixed/static TCP session ID
    **/
    public void setFixedTcpSessionID(String v)
    {
        this.setFieldValue(FLD_fixedTcpSessionID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this device has an assigned TCP session-id
    *** @return True if this device has an assigned TCP session-id
    **/
    public boolean hasLastTcpSessionID()
    {
        return !StringTools.isBlank(this.getLastTcpSessionID());
    }

    /**
    *** Gets the last TCP session ID
    *** @return The last TCP session ID
    **/
    public String getLastTcpSessionID()
    {
        String v = (String)this.getFieldValue(FLD_lastTcpSessionID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the last TCP session ID
    *** @param v The last TCP session ID
    **/
    public void setLastTcpSessionID(String v)
    {
        this.setFieldValue(FLD_lastTcpSessionID, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the local IP address to which this device sent its latest packet
    *** @return The local IP address for this Device
    **/
    public DTIPAddress getIpAddressLocal()
    {
        DTIPAddress v = (DTIPAddress)this.getFieldValue(FLD_ipAddressLocal);
        return v; // May return null!!
    }

    /**
    *** Sets the local IP address to which this device sent its latest packet
    *** @param v The local IP address for this Device
    **/
    public void setIpAddressLocal(DTIPAddress v)
    {
        this.setFieldValue(FLD_ipAddressLocal, v);
    }

    /**
    *** Sets the local IP address to which this device sent its latest packet
    *** @param v The local IP address for this Device
    **/
    public void setIpAddressLocal(String v)
    {
        this.setIpAddressCurrent((v != null)? new DTIPAddress(v) : null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last known IP address used by the Device
    *** @return The last known IP address used by the Device
    **/
    public DTIPAddress getIpAddressCurrent()
    {
        DTIPAddress v = (DTIPAddress)this.getFieldValue(FLD_ipAddressCurrent);
        return v; // May return null!!
    }

    /**
    *** Sets the last known IP address used by the Device
    *** @param v The last known IP address used by the Device
    **/
    public void setIpAddressCurrent(DTIPAddress v)
    {
        this.setFieldValue(FLD_ipAddressCurrent, v);
    }

    /**
    *** Sets the last known IP address used by the Device
    *** @param v The last known IP address used by the Device
    **/
    public void setIpAddressCurrent(String v)
    {
        this.setIpAddressCurrent((v != null)? new DTIPAddress(v) : null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last known remote port used by the Device
    *** @return The last known remote port used by the Device
    **/
    public int getRemotePortCurrent()
    {
        Integer v = (Integer)this.getFieldValue(FLD_remotePortCurrent);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** Sets the last known remote port used by the Device
    *** @param v The last known remote port used by the Device
    **/
    public void setRemotePortCurrent(int v)
    {
        this.setFieldValue(FLD_remotePortCurrent, ((v > 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last known listen port used by the Device
    *** @return The last known listen port used by the Device
    **/
    public int getListenPortCurrent()
    {
        Integer v = (Integer)this.getFieldValue(FLD_listenPortCurrent);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** Sets the last known listen port used by the Device
    *** @param v The last known listen port used by the Device
    **/
    public void setListenPortCurrent(int v)
    {
        this.setFieldValue(FLD_listenPortCurrent, ((v > 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    private GeoPoint savedLastValidGeoPoint = null;

    /**
    *** Save last valid GeoPoint 
    **/
    public boolean saveOriginalLastValidGeoPoint()
    {
        // -- used for deferred rule checking [see Device.GetDeferRuleCheckToPostInsert()]
        if (this.savedLastValidGeoPoint == null) {
            double lat = this.getLastValidLatitude();
            double lon = this.getLastValidLongitude();
            // -- TODO: timestamp of last valid GP must also be saved
            if (GeoPoint.isValid(lat,lon)) {
                this.savedLastValidGeoPoint = new GeoPoint(lat,lon);
                return true;
            }
        }
        return false;
    }

    /**
    *** Clear original last valid GeoPoint 
    **/
    public void clearOriginalLastValidGeoPoint()
    {
        this.savedLastValidGeoPoint = null;
    }

    /**
    *** Get Original last valid GeoPoint
    **/
    public GeoPoint getOriginalLastValidGeoPoint()
    {
        if (this.savedLastValidGeoPoint != null) {
            return this.savedLastValidGeoPoint;
        } else {
            return this.getLastValidLocation();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last known valid latitude for this Device
    *** @return The last known valid latitude for this Device
    **/
    public double getLastValidLatitude() // getLastLatitude
    {
        return this.getOptionalFieldValue(FLD_lastValidLatitude, 0.0);
    }

    /**
    *** Gets the last known valid latitude for this Device
    *** @param v The last known valid latitude for this Device
    **/
    public void setLastValidLatitude(double v)
    {
        this.setOptionalFieldValue(FLD_lastValidLatitude, v);
    }

    /**
    *** Gets the last known valid longitude for this Device
    *** @return The last known valid longitude for this Device
    **/
    public double getLastValidLongitude() // getLastLongitude
    {
        return this.getOptionalFieldValue(FLD_lastValidLongitude, 0.0);
    }

    /**
    *** Sets the last known valid longitude for this Device
    *** @param v The last known valid longitude for this Device
    **/
    public void setLastValidLongitude(double v)
    {
        this.setOptionalFieldValue(FLD_lastValidLongitude, v);
    }

    /**
    *** Returns true if this device has a last known valid GeoPoint
    *** @return True if this device has a last known valid GeoPoint
    **/
    public boolean hasLastValidLocation() // hasLastLocation
    {
        double lat = this.getLastValidLatitude();
        double lon = this.getLastValidLongitude();
        return GeoPoint.isValid(lat,lon);
    }

    /**
    *** Gets the last known valid GeoPoint for this Device
    *** @return The last known valid GeoPoint for this Device
    **/
    public GeoPoint getLastValidLocation() // getLastLocation getLastGeoPoint
    {
        // returns null if invalid
        double lat = this.getLastValidLatitude();
        double lon = this.getLastValidLongitude();
        return GeoPoint.isValid(lat,lon)? new GeoPoint(lat,lon) : null;
    }

    /**
    *** Gets the last known valid GeoPoint for this Device
    *** @param tryLastEvent If true, the last valid EventData record may be queried
    *** @return The last known valid GeoPoint for this Device
    **/
    public GeoPoint getLastValidLocation(boolean tryLastEvent)
    {
        GeoPoint gp = this.getLastValidLocation();
        if ((gp == null) && tryLastEvent) {
            try {
                EventData lastEv = this.getLastEvent(true); // valid GPS only
                if ((lastEv != null) && lastEv.isValidGeoPoint()) {
                    gp = lastEv.getGeoPoint();
                    this.setLastValidLocation(
                        lastEv.getTimestamp(),      // FLD_lastGPSTimestamp
                        lastEv.getGpsAge(),         // 
                        lastEv.getGeoPoint(),       // FLD_lastValidLatitude/FLD_lastValidLongitude
                        lastEv.getSpeedKPH(),       // FLD_lastValidSpeedKPH
                        lastEv.getHeading());       // FLD_lastValidHeading
                    if (this.getLastOdometerKM() <= 0.0) {
                        double odomKM = lastEv.getOdometerKM();
                        this.setLastOdometerKM(odomKM); // may still be '0.0'
                    }
                }
            } catch (DBException dbe) {
                // ignore error
            }
        }
        return gp;
    }

    /**
    *** Sets the last known valid location for this Device
    *** @param timestamp  The time of the location
    *** @param gpsAge     The age of the gps fix
    *** @param gp         The GeoPoint location
    *** @param heading    The direction of travel
    **/
    private void setLastValidLocation(
        long timestamp, long gpsAge, GeoPoint gp, 
        double speedKPH, double heading)
    {
        if ((gp != null) && gp.isValid()) {
            long gpsTimestamp = timestamp - gpsAge;         // [2.6.2-B44]
            this.setLastGPSTimestamp(gpsTimestamp);         // FLD_lastGPSTimestamp
            this.setLastValidLatitude(gp.getLatitude());    // FLD_lastValidLatitude
            this.setLastValidLongitude(gp.getLongitude());  // FLD_lastValidLongitude
            if (speedKPH >= 0.0) {
                this.setLastValidSpeedKPH(speedKPH);        // FLD_lastValidSpeedKPH
            }
            if (heading >= 0.0) {
                this.setLastValidHeading(heading);          // FLD_lastValidHeading
            }
        } else {
            this.setLastGPSTimestamp(0L);                   // FLD_lastGPSTimestamp
            this.setLastValidLatitude(0.0);                 // FLD_lastValidLatitude
            this.setLastValidLongitude(0.0);                // FLD_lastValidLongitude
            this.setLastValidSpeedKPH(0.0);                 // FLD_lastValidSpeedKPH
            this.setLastValidHeading(0.0);                  // FLD_lastValidHeading
        }
    }

    /**
    *** Calculates and returns the number of meters from the last valid GPS location
    *** to the specified GeoPoint.
    *** @param gp  The GeoPoint to test
    *** @return The number of meters to the specified GeoPoint
    **/
    public double getMetersToLastValidLocation(GeoPoint gp)
    {
        if (GeoPoint.isValid(gp)) {
            GeoPoint lastValidLoc = this.getLastValidLocation(true);
            if (lastValidLoc != null) {
                return gp.metersToPoint(lastValidLoc);
            }
        }
        return -1.0;
    }

    /** 
    *** Returns true if the last know location of this device is is within the specified
    *** number of meters to the specified GeoPoint.
    *** @param gp   The GeoPoint to test
    *** @param meters The radius to test, in meters
    **/
    public boolean isNearLastValidLocation(GeoPoint gp, double meters)
    {
        /*
        if (meters > 0.0) {
            double deltaM = this.getMetersToLastValidLocation(gp); // '-1' if 'gp' is invalid
            return ((deltaM >= 0.0) && (deltaM < meters)); // false if gp is invalid
        } else {
            return false;
        }
        */
        return this.isNearLastValidLocation(gp, meters, -1L);
    }

    /** 
    *** Returns true if the last know location of this device is is within the specified
    *** number of meters to the specified GeoPoint.
    *** @param gp        The GeoPoint to test
    *** @param meters    The radius to test, in meters
    *** @param maxFixAge The maximum fix age
    **/
    public boolean isNearLastValidLocation(GeoPoint gp, double meters, long maxFixAge)
    {

        /* invalid/disabled meter specification */
        if (meters <= 0.0) {
            return false; // not near prior location
        }

        /* beyond maximum fix age */
        if (maxFixAge > 0L) {
            // -- maximum fix age specified
            long gpsTime = this.getLastGPSTimestamp();
            if (gpsTime > 0L) {
                // -- we have a last GPS timestamp
                long nowTime = DateTime.getCurrentTimeSec();
                if ((nowTime - gpsTime) > maxFixAge) {
                    // -- elapsed time between last GPS time and now is greater than maxFixAge
                    return false;
                }
            }
        }

        /* within specified distance */
        double deltaM = this.getMetersToLastValidLocation(gp); // '-1' if 'gp' is invalid
        return ((deltaM >= 0.0) && (deltaM < meters))? true : false; // false if gp is invalid

    }

    // ------------------------------------------------------------------------

    /**
    *** Container class for a detected nearby device
    **/
    public static class NearbyDevice
    {
        private Account  account    = null;
        private Device   device     = null;
        private String   targetID   = null;
        private String   deviceID   = null;
        private GeoPoint lastGP     = null; // GeoPoint
        private double   lastKPH    = 0L;   // Speed
        private double   lastDir    = 0L;   // Heading
        private long     lastTS     = 0L;   // Timestamp
        private double   distMeters = 0.0;
        private boolean  didGetAddr = false;
        private String   address    = null;
        public NearbyDevice(
            Account acct, String targID,
            String devID, GeoPoint gp, double kph, double dir, long timestamp,
            double distM) {
            this.account    = acct;
            this.targetID   = StringTools.trim(targID);
            this.deviceID   = StringTools.trim(devID);  // FLD_deviceID
            this.lastGP     = gp;                       // FLD_lastValidLatitude/FLD_lastValidLongitude
            this.lastKPH    = kph;                      // FLD_lastValidSpeedKPH
            this.lastDir    = dir;                      // FLD_lastValidHeading
            this.lastTS     = timestamp;                // FLD_lastGPSTimestamp
            this.distMeters = distM;
        }
        public Account getAccount() {
            return this.account;
        }
        public String getAccountID() {
            return (this.account != null)? this.account.getAccountID() : "";
        }
        public String getTargetDeviceID() {
            return this.targetID;
        }
        public String getDeviceID() {
            return this.deviceID;
        }
        public boolean isTargetDevice() {
            return this.targetID.equals(this.deviceID);
        }
        public Device getDevice() throws DBException {
            if (this.device == null) {
                this.device = Device.getDevice(this.getAccount(),this.getDeviceID());
            } 
            return this.device;
        }
        public String getDeviceDescription() {
            try {
                Device dev = this.getDevice();
                return (dev != null)? dev.getDescription() : "";
            } catch (DBException dbe) {
                // -- error reading Device
                return "";
            }
        }
        public long getTimestamp() {
            return this.lastTS;
        }
        public int getStatusCode() {
            return StatusCodes.STATUS_NONE;
        }
        public boolean hasGeoPoint() {
            return GeoPoint.isValid(this.getGeoPoint());
        }
        public GeoPoint getGeoPoint() {
            return this.lastGP;
        }
        public double getLatitude() {
            return (this.lastGP != null)? this.lastGP.getLatitude() : 0.0;
        }
        public double getLongitude() {
            return (this.lastGP != null)? this.lastGP.getLongitude() : 0.0;
        }
        public long getGpsAge() {
            return DateTime.getCurrentTimeSec() - this.getTimestamp();
        }
        public double getSpeedKPH() {
            return this.lastKPH;
        }
        public double getHeading() {
            return this.lastDir;
        }
        public double getDistanceMeters() {
            return this.distMeters;
        }
        public double getDistanceKM() {
            return this.distMeters / 1000.0;
        }
        public String getAddress() {
            if (!this.didGetAddr) {
                this.didGetAddr = true;
                try {
                    EventData ev[] = EventData.getRangeEvents(
                        this.getAccountID(), this.getDeviceID(),
                        this.getTimestamp(), this.getTimestamp(),
                        null/*statusCodes*/,
                        true/*validGPS*/,
                        EventData.LimitType.FIRST, -1L/*limit*/, true/*ascending*/,
                        null/*additionalSelect*/);
                    if (ListTools.size(ev) > 0) {
                        for (EventData e : ev) {
                            String addr = e.getAddress();
                            if (!StringTools.isBlank(addr)) {
                                this.address = addr;
                                break;
                            }
                        }
                    }
                } catch (DBException dbe) {
                    // -- error getting Address
                }
            }
            return this.address;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Device=").append(this.getAccountID()).append("/").append(this.getDeviceID());
            if (this.hasGeoPoint()) {
                sb.append(" ");
                sb.append("GeoPoint=").append(this.getGeoPoint().toString());
            } 
            sb.append(" ");
            sb.append("Speed=").append(StringTools.format(this.getSpeedKPH(),"0"));
            sb.append("-").append(GeoPoint.GetHeadingString(this.getHeading(),null));
            sb.append(" ");
            sb.append("Timestamp=").append(this.getTimestamp());
            sb.append(" ");
            sb.append("DistMeters=").append(StringTools.format(this.getDistanceMeters(),"0"));
            return sb.toString();
        }
    }

    /**
    *** Return Device.NearbyDevice list of Devices near this Device
    *** Active only, this device excluded from list.
    *** @param radiusM     The radius in meters
    *** @param maxAgeSec   Maximum last location age (in seconds)
    *** @param userAuth    If non-null, returns devices authorized to user only
    **/
    public Map<String,Device.NearbyDevice> getNearbyDevices(double radiusM, long maxAgeSec, User userAuth)
        throws DBException
    {
        Account account = this.getAccount();
        String   targID = this.getDeviceID();
        GeoPoint lastGP = this.getLastValidLocation();
        long     lastTS = this.getLastGPSTimestamp();

        /* validate */
        if (radiusM <= 0.0) {
            return null;
        } else
        if (!GeoPoint.isValid(lastGP)) {
            return null;
        } 
        
        /* start/end time */
        long startTime = -1L;
        long endTime   = -1L;
        if (maxAgeSec > 0L) {
            startTime = lastTS - maxAgeSec;
            if (startTime < 0L) { startTime = 0L; }
            endTime   = lastTS + maxAgeSec;
        }

        /* nearby devices */
        return Device.GetNearbyDeviceMap(
            account, targID, false/*inclThisDev?*/, 
            startTime, endTime,
            lastGP, radiusM, true/*activeOnly*/, userAuth, 
            true/*sort*/);

    }

    /**
    *** Return Device.NearbyDevice list of Devices near specified location
    *** @param account        The Account instance (cannot be null)
    *** @param targetDevID    The target deviceID
    *** @param inclTargetID   True to include targetDeviceID in NearbyDevice list, false to exclude.
    *** @param startTime      Include last location timestamps after this time (0 to disable)
    *** @param endTime        Include last location timestamps before this time (0 to disable)
    *** @param gp             The GPS location
    *** @param radiusM        The radius in meters
    *** @param activeOnly     True for active devices only
    *** @param userAuth       If non-null, returns devices authorized to user only
    *** @param sortByDist     True to return list sorted by distance, from closest to farthest
    **/
    public static Map<String,NearbyDevice> GetNearbyDeviceMap(
        Account account,
        String targetDevID, boolean inclTargetID, 
        long startTime, long endTime,
        GeoPoint gp, double radiusM, 
        boolean activeOnly, User userAuth,
        boolean sortByDist)
        throws DBException
    {

        /* validate Account/GeoPoint/radius */
        if (account == null) {
            Print.logError("Specified Account is null");
            return null;
        } else
        if (!GeoPoint.isValid(gp)) {
            Print.logError("Specified GeoPoint is invalid");
            return null;
        }

        /* get min/max lat/lon */
        GeoBounds bounds = null;
        if (radiusM > 0.0) {
            bounds = new GeoBounds(radiusM, gp);
            if (!bounds.isValid()) {
                // -- invalid bounds
                Print.logError("GeoBounds is invalid: " + bounds);
                return null;
            }
        }

        /* create "WHERE" */
        // SELECT * FROM Device WHERE 
        //       (accountID = "account") 
        //   AND (deviceID != "TARGET_ID") 
        //   AND (isActive != 0) 
        //   AND (lastValidLatitude  <= maxLat) 
        //   AND (lastValidLatitude  >= minLat) 
        //   AND (lastValidLongitude <= maxLon) 
        //   AND (lastValidLongitude >= minLon) 
        //   AND (lastGPSTimestamp >= startTime) 
        //   AND (lastGPSTimestamp <= endTime)
        DBWhere dwh = new DBWhere(Device.getFactory());
        // -- account
        dwh.append(dwh.EQ(Device.FLD_accountID, account.getAccountID()));
        // -- exclude target deviceID
        if (inclTargetID && !StringTools.isBlank(targetDevID)) {
            dwh.append(dwh.AND_(dwh.NE(Device.FLD_deviceID, targetDevID)));
        }
        // -- active only
        if (activeOnly) {
            dwh.append(dwh.AND_(dwh.NE(Device.FLD_isActive,0)));
        }
        // -- location bounding box
        if (bounds != null) {
            double minLat = bounds.getMinLatitude();
            double maxLat = bounds.getMaxLatitude();
            double minLon = bounds.getMinLongitude();
            double maxLon = bounds.getMaxLongitude();
            dwh.append(dwh.AND_(
                dwh.AND(
                    dwh.LE(Device.FLD_lastValidLatitude , maxLat),
                    dwh.GE(Device.FLD_lastValidLatitude , minLat),
                    dwh.LE(Device.FLD_lastValidLongitude, maxLon),
                    dwh.GE(Device.FLD_lastValidLongitude, minLon)
                )
            ));
        }
        // -- time range
        if (startTime > 0L) {
            dwh.append(dwh.AND_(dwh.GE(Device.FLD_lastGPSTimestamp, startTime)));
        }
        if (endTime > 0L) {
            dwh.append(dwh.AND_(dwh.LE(Device.FLD_lastGPSTimestamp, endTime  )));
        }
        // --
        String wh = dwh.WHERE(dwh.toString());

        /* select */
        DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
        dsel.setSelectedFields(
          //Device.FLD_accountID, <== all are from same account
            Device.FLD_deviceID,
            Device.FLD_lastValidLatitude,
            Device.FLD_lastValidLongitude,
            Device.FLD_lastValidSpeedKPH,
            Device.FLD_lastValidHeading,
            Device.FLD_lastGPSTimestamp
            );
        dsel.setWhere(wh);
        dsel.setOrderByFields(Device.FLD_deviceID);

        /* get Devices */
        OrderedMap<String,Device.NearbyDevice> nbMap = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs = stmt.getResultSet();
            while (rs.next()) {
              //String acctID  = rs.getString(Device.FLD_accountID); <== all from same account
                String devID   = rs.getString(Device.FLD_deviceID);
                double lastLat = rs.getDouble(Device.FLD_lastValidLatitude);
                double lastLon = rs.getDouble(Device.FLD_lastValidLongitude);
                double lastKPH = rs.getDouble(Device.FLD_lastValidSpeedKPH);
                double lastDir = rs.getDouble(Device.FLD_lastValidHeading);
                long   lastTS  = rs.getLong(  Device.FLD_lastGPSTimestamp);
                if (StringTools.isBlank(devID) || !GeoPoint.isValid(lastLat,lastLon)) {
                    // -- unlikely, skip deviceID
                } else
                if (!inclTargetID && (targetDevID != null) && devID.equals(targetDevID)) {
                    // -- exclude this device (already excluded in above select)
                } else
                if ((userAuth != null) && !userAuth.isAuthorizedDevice(devID)) {
                    // -- user not authorized
                } else {
                    // -- save deviceID
                    if (nbMap == null) { nbMap = new OrderedMap<String,Device.NearbyDevice>(); }
                    GeoPoint lastGP = new GeoPoint(lastLat, lastLon);
                    double   distM  = gp.metersToPoint(lastGP);
                    NearbyDevice nb = new NearbyDevice(
                        account, targetDevID,
                        devID, lastGP, lastKPH, lastDir, lastTS,
                        distM);
                    nbMap.put(devID, nb);
                }
            }
        } catch (SQLException sqe) {
            throw new DBException("Getting Nearby Devices", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* sort by distance */
        if (sortByDist && (nbMap != null)) {
            // -- TODO: sort map by distance
            final Map<String,Device.NearbyDevice> nbm = nbMap;
            nbMap.sortKeys(new Comparator<String>() {
                public int compare(String v1, String v2) {
                    Device.NearbyDevice n1 = nbm.get(v1);
                    Device.NearbyDevice n2 = nbm.get(v2);
                    double d1 = (n1 != null)? n1.getDistanceMeters() : 9999999999.0;
                    double d2 = (n2 != null)? n2.getDistanceMeters() : 9999999999.0;
                    return (d1 == d2)? 0 : (d1 < d2)? -1 : 1;
                }
            });
            /*
            ListTools.sort(nbMap, new Comparator<Device.NearbyDevice>() {
                public int compare(Device.NearbyDevice n1, Device.NearbyDevice n2) {
                    double d1 = (n1 != null)? n1.getDistanceMeters() : 9999999999.0;
                    double d2 = (n2 != null)? n2.getDistanceMeters() : 9999999999.0;
                    return (d1 == d2)? 0 : (d1 < d2)? -1 : 1;
                }
            });
            */
        }

        /* return list */
        return nbMap; // may be null

    } // GetNearbyDeviceMap

    // ------------------------------------------------------------------------

    /**
    *** Container class for a detected nearby subscriber devices
    **/
    public static class NearbySubscriber
    {
        private String   accountID  = null;
        private String   deviceID   = null;
        private String   subscrID   = null;
        private String   name       = null;
        private String   avatar     = null;
        private String   phone      = null;
        private GeoPoint lastGP     = null; // GeoPoint
        private double   lastKPH    = 0L;   // Speed
        private double   lastDir    = 0L;   // Heading
        private long     lastTS     = 0L;   // Timestamp
        private double   distanceM  = 0.0;  // Distance to target
        public NearbySubscriber(
            String acctID, String devID, 
            String subID, String subName, String avat, String phone,
            GeoPoint gp, double kph, double dir, long timestamp,
            double distM) {
            this.accountID  = StringTools.trim(acctID); // FLD_accountID
            this.deviceID   = StringTools.trim(devID);  // FLD_deviceID
            this.subscrID   = StringTools.trim(subID);  // FLD_subscriberID
            this.name       = StringTools.trim(subName);// FLD_subscriberName
            this.avatar     = StringTools.trim(avat);   // FLD_subscriberAvatar
            this.phone      = StringTools.trim(phone);  // FLD_simPhoneNumber
            this.lastGP     = gp;                       // FLD_lastValidLatitude/FLD_lastValidLongitude
            this.lastKPH    = kph;                      // FLD_lastValidSpeedKPH
            this.lastDir    = dir;                      // FLD_lastValidHeading
            this.lastTS     = timestamp;                // FLD_lastGPSTimestamp
            this.distanceM  = distM;
        }
        public String getAccountID() {
            return this.accountID;
        }
        public String getDeviceID() {
            return this.deviceID;
        }
        public String getSubscriberID() {
            return this.subscrID;
        }
        public String getName() {
            return this.name;
        }
        public String getAvatar() {
            return this.avatar;
        }
        public String getPhone() {
            return this.phone;
        }
        public long getLastTimestamp() {
            return this.lastTS;
        }
        public GeoPoint getLastGeoPoint() {
            return this.lastGP;
        }
        public double getLastLatitude() {
            return GeoPoint.isValid(this.lastGP)? this.lastGP.getLatitude() : 0.0;
        }
        public double getLastLongitude() {
            return GeoPoint.isValid(this.lastGP)? this.lastGP.getLongitude() : 0.0;
        }
        public double getLastSpeedKPH() {
            return this.lastKPH;
        }
        public double getLastHeading() {
            return this.lastDir;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Device=").append(this.getAccountID()).append("/").append(this.getDeviceID());
            if (GeoPoint.isValid(this.getLastGeoPoint())) {
                sb.append(" ");
                sb.append("GeoPoint=").append(this.getLastGeoPoint().toString());
            } 
            sb.append(" ");
            sb.append("Speed=").append(StringTools.format(this.getLastSpeedKPH(),"0"));
            sb.append("-").append(GeoPoint.GetHeadingString(this.getLastHeading(),null));
            sb.append(" ");
            sb.append("Timestamp=").append(this.getLastTimestamp());
            return sb.toString();
        }
    }

    /**
    *** Return Device.NearbyDevice list of Devices near specified location
    *** @param gp             The GPS location
    *** @param radiusM        The radius in meters
    *** @param activeOnly     True for active subscribers only
    **/
    public static Map<String,NearbySubscriber> GetNearbySubscriberMap(
        GeoPoint gp, double radiusM, 
        boolean activeOnly)
        throws DBException
    {
        String targetSubID = "";

        /* validate GeoPoint/Radius */
        if (!GeoPoint.isValid(gp)) {
            Print.logError("Specified GeoPoint is invalid");
            return null;
        } else
        if (radiusM <= 0.0) {
            Print.logError("Specified Radius is invalid");
            return null;
        }

        /* get min/max lat/lon */
        GeoBounds bounds = new GeoBounds(radiusM, gp);
        if (!bounds.isValid()) {
            // -- invalid bounds
            Print.logError("GeoBounds is invalid: " + bounds);
            return null;
        }

        /* create "WHERE" */
        // SELECT * FROM Device WHERE 
        //       (subscriberID != "") 
        //   AND (isActive != 0) 
        //   AND (lastValidLatitude  <= maxLat) 
        //   AND (lastValidLatitude  >= minLat) 
        //   AND (lastValidLongitude <= maxLon) 
        //   AND (lastValidLongitude >= minLon) 
        DBWhere dwh = new DBWhere(Device.getFactory());
        // -- subscriberID not blank
        dwh.append(dwh.NE(Device.FLD_subscriberID,""));
        // -- exclude target subscriberID
        if (!StringTools.isBlank(targetSubID)) {
            dwh.append(dwh.AND_(dwh.NE(Device.FLD_subscriberID, targetSubID)));
        }
        // -- active only
        if (activeOnly) {
            dwh.append(dwh.AND_(dwh.NE(Device.FLD_isActive,0)));
        }
        // -- location bounding box
        double minLat = bounds.getMinLatitude();
        double maxLat = bounds.getMaxLatitude();
        double minLon = bounds.getMinLongitude();
        double maxLon = bounds.getMaxLongitude();
        dwh.append(dwh.AND_(
            dwh.AND(
                dwh.LE(Device.FLD_lastValidLatitude , maxLat),
                dwh.GE(Device.FLD_lastValidLatitude , minLat),
                dwh.LE(Device.FLD_lastValidLongitude, maxLon),
                dwh.GE(Device.FLD_lastValidLongitude, minLon)
            )
        ));
        // --
        String wh = dwh.WHERE(dwh.toString());

        /* select */
        DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
        dsel.setSelectedFields(
            Device.FLD_accountID,
            Device.FLD_deviceID,
            Device.FLD_subscriberID,
            Device.FLD_subscriberName,
            Device.FLD_subscriberAvatar,
            Device.FLD_simPhoneNumber, // FLD_fixedContactPhone
            Device.FLD_lastValidLatitude,
            Device.FLD_lastValidLongitude,
            Device.FLD_lastValidSpeedKPH,
            Device.FLD_lastValidHeading,
            Device.FLD_lastGPSTimestamp
            );
        dsel.setWhere(wh);
        dsel.setOrderByFields(Device.FLD_subscriberID);
        Print.logInfo("NearbySubscriber SELECT: " + dsel);

        /* get Devices */
        OrderedMap<String,Device.NearbySubscriber> nbMap = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs = stmt.getResultSet();
            while (rs.next()) {
                String acctID  = rs.getString(Device.FLD_accountID);
                String devID   = rs.getString(Device.FLD_deviceID);
                String subID   = rs.getString(Device.FLD_subscriberID);
                String subName = rs.getString(Device.FLD_subscriberName);
                String avatar  = rs.getString(Device.FLD_subscriberAvatar);
                String phone   = rs.getString(Device.FLD_simPhoneNumber); // FLD_fixedContactPhone
                double lastLat = rs.getDouble(Device.FLD_lastValidLatitude);
                double lastLon = rs.getDouble(Device.FLD_lastValidLongitude);
                double lastKPH = rs.getDouble(Device.FLD_lastValidSpeedKPH);
                double lastDir = rs.getDouble(Device.FLD_lastValidHeading);
                long   lastTS  = rs.getLong(  Device.FLD_lastGPSTimestamp);
                if (StringTools.isBlank(acctID) || 
                    StringTools.isBlank(devID)  || 
                    !GeoPoint.isValid(lastLat,lastLon)) {
                    // -- unlikely, skip deviceID
                } else {
                    // -- save subscriber
                    if (nbMap == null) { nbMap = new OrderedMap<String,Device.NearbySubscriber>(); }
                    GeoPoint lastGP = new GeoPoint(lastLat, lastLon);
                    double   distM  = gp.metersToPoint(lastGP);
                    Device.NearbySubscriber nb = new Device.NearbySubscriber(
                        acctID, devID,
                        subID, subName, avatar, phone,
                        lastGP, lastKPH, lastDir, lastTS,
                        distM);
                    nbMap.put(subID, nb);
                }
            }
        } catch (SQLException sqe) {
            throw new DBException("Getting Nearby Subscribers", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return nbMap; // may be null

    } // GetNearbyDeviceMap

    // ------------------------------------------------------------------------

    /** 
    *** Returns the last valid street address, based on the last know location
    *** (not currently supported)
    *** @return The last valid street address
    **/
    public String getLastValidAddress()
    {
        return ""; // not yet supported
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last valid subdivision/state
    *** @return The last valid subdivision/state
    **/
    public String getLastSubdivision()
    {
        String v = (String)this.getFieldValue(FLD_lastSubdivision);
        return StringTools.trim(v);
    }

    /**
    *** Sets the last valid subdivision/state
    *** @param v The last valid subdivision/state
    **/
    public void setLastSubdivision(String v)
    {
        this.setFieldValue(FLD_lastSubdivision, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last valid speed
    *** @return The last valid speed
    **/
    public double getLastValidSpeedKPH()
    {
        return this.getOptionalFieldValue(FLD_lastValidSpeedKPH, -1.0);
    }

    /**
    *** Sets the last valid speed
    *** @param v The last valid speed
    **/
    public void setLastValidSpeedKPH(double v)
    {
        this.setOptionalFieldValue(FLD_lastValidSpeedKPH, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last valid heading
    *** @return The last valid heading
    **/
    public double getLastValidHeading()
    {
        return this.getOptionalFieldValue(FLD_lastValidHeading, 0.0);
    }

    /**
    *** Sets the last valid heading
    *** @param v The last valid heading
    **/
    public void setLastValidHeading(double v)
    {
        this.setOptionalFieldValue(FLD_lastValidHeading, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last valid GPS timestamp
    *** @return The last valid GPS timestamp
    **/
    public long getLastGPSTimestamp()
    {
        Long v = (Long)this.getFieldValue(FLD_lastGPSTimestamp);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last valid GPS timestamp
    *** @param v  The last valid GPS timestamp
    **/
    public void setLastGPSTimestamp(long v)
    {
        this.setFieldValue(FLD_lastGPSTimestamp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last event timestamp
    *** @return The last event timestamp
    **/
    public long getLastEventTimestamp()
    {
        Long v = (Long)this.getFieldValue(FLD_lastEventTimestamp);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last event timestamp
    *** @param v  The last event timestamp
    **/
    public void setLastEventTimestamp(long v)
    {
        this.setFieldValue(FLD_lastEventTimestamp, v);
    }

    /**
    *** Returns true if the specified timestamp is prior to the last received event timestamp
    *** @param timestamp  The timestamp to check
    *** @return True if the timestamp is prior to the last received event timestamp
    **/
    public boolean isOldEventTimestamp(long timestamp)
    {
        if (timestamp <= 0L) {
            // invalid timestamp
            return true;
        } else 
        if (timestamp < this.getLastGPSTimestamp()) {
            // prior to last valid GPS timestamp
            return true;
        } else 
        if (timestamp < this.getLastEventTimestamp()) {
            // prior to last event timestamp
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last status code received for this device
    *** @return The last status code received for this device
    **/
    public int getLastEventStatusCode()
    {
        return this.getFieldValue(FLD_lastEventStatusCode, 0);
    }

    /**
    *** Gets the Hex String representation of the last status code received for this device
    *** @return The Hex String representation of the last status code received for this device
    **/
    public String getLastEventStatusCodeHex()
    {
        return StatusCodes.GetHex(this.getLastEventStatusCode());
    }

    /**
    *** Gets the StatusCodeProvider for the last event statusCode received for this device<br>
    *** (may return null if the event status code is not pre-defined).
    *** @return The StatusCodeProvider for last event statusCode received
    **/
    public StatusCodeProvider getLastEventStatusCodeProvider(BasicPrivateLabel bpl)
    {
        Device dev  = this;
        int    code = this.getLastEventStatusCode();
        return StatusCode.getStatusCodeProvider(dev, code, bpl, null/*dftSCP*/);
    }

    /**
    *** Gets the String representation of the status code of this event
    *** @return The String representation of the status code of this event,
    ***     or null if the last event statusCode is not pre-defined.
    **/
    public String getLastEventStatusCodeDescription(BasicPrivateLabel bpl)
    {
        Device dev  = this;
        int    code = this.getLastEventStatusCode();
        return StatusCode.getDescription(dev, code, bpl, null/*dftDesc*/);
    }

    /**
    *** Sets the last status code received for this device
    *** @param v The last status code received for this device
    **/
    public void setLastEventStatusCode(int v)
    {
        this.setFieldValue(FLD_lastEventStatusCode, ((v >= 0)? v : StatusCodes.STATUS_NONE));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if EventsPerSecond is supported/enabled
    **/
    public static boolean supportsEventsPerSecond()
    {
        return Device.getFactory().hasField(FLD_lastEventsPerSecond);
    }

    /**
    *** Gets the last estimated events-per-second
    *** @return The last estimated events-per-second
    **/
    public double getLastEventsPerSecond()
    {
        Double v = (Double)this.getOptionalFieldValue(FLD_lastEventsPerSecond);
        return (v != null)? v.doubleValue() : 0.0;
    }

    /**
    *** Sets the last estimated events-per-second
    *** @param v  The last estimated events-per-second
    **/
    public void setLastEventsPerSecond(double v)
    {
        this.setOptionalFieldValue(FLD_lastEventsPerSecond, v);
    }

    /**
    *** Gets the aged estimated events-per-second
    *** @param ageMS Aged timestamp
    *** @return The aged estimated events-per-second
    **/
    public double getAgedEventsPerSecond(long ageMS)
    {
        Device.initEventsPerSecond();
        long   epst = this.getLastEventsPerSecondMS();
        double epms = this.getLastEventsPerSecond() / 1000.0;
        double deltaVal = (double)(ageMS - epst); // deltaMS
        if (deltaVal > 0.0) {
            double AGE_A = Math.pow(1.0 - Device.EPS_ALPHA, deltaVal); // age old value
            epms = AGE_A * epms;
        }
        return epms * 1000.0;
    }

    /**
    *** Gets the last event-per-second timestamp (milliseconds)
    *** @return The last event-per-second timestamp (milliseconds)
    **/
    public long getLastEventsPerSecondMS()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_lastEventsPerSecondMS);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last event-per-second timestamp (milliseconds)
    *** @param v  The last event-per-second timestamp (milliseconds)
    **/
    public void setLastEventsPerSecondMS(long v)
    {
        this.setOptionalFieldValue(FLD_lastEventsPerSecondMS, v);
    }

    /**
    *** Counts event "lastEventPerSecond" field.
    **/
    private void _countEventsPerSecond()
    {
        /* not supported? */
        if (!Device.supportsEventsPerSecond()) {
            // -- not supported, don't bother counting ...
            return;
        }

        /* init (synchronized) */
        Device.initEventsPerSecond();

        /* get last EPS */
        long   nowTimeMS    = System.currentTimeMillis();
        long   lastEvTimeMS = this.getLastEventsPerSecondMS();
        double epsLastEPS   = this.getLastEventsPerSecond();
        double deltaMS      = (double)(nowTimeMS - lastEvTimeMS);
        double deltaSec     = deltaMS / 1000.0;
        double deltaVal     = deltaMS;

        /* calculate new EPS */
        double newVal = 0.0;
        try {
            double thisVal = 1.0; 
            double agedVal = epsLastEPS / 1000.0; // epsLastValue;
            if (deltaVal > 0.0) {
                // -- count last 'deltaMS' intervals as '0' values
                double AGE_A = Math.pow(1.0 - EPS_ALPHA, deltaVal); // age old value
                agedVal = AGE_A * agedVal;
            }
            newVal = (EPS_ALPHA * thisVal) + agedVal;
        } catch (Throwable th) { // unlikely
            newVal = 0.0;
        }

        /* set new EPS */
        this.setLastEventsPerSecond(newVal * 1000.0);
        this.setLastEventsPerSecondMS(nowTimeMS);
        this.addOtherChangedFieldNames(FLD_lastEventsPerSecond, FLD_lastEventsPerSecondMS);

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this device Account has a configured MobileLocationProvider
    **/
    public boolean hasMobileLocationProvider()
    {

        /* account check */
        Account acct = this.getAccount();
        if (acct == null) {
            return false; // should not occur
        }

        /* MobileLocationProvider check */
        MobileLocationProvider mlp = acct.getPrivateLabel().getMobileLocationProvider();
        if ((mlp == null) || !mlp.isEnabled()) {
            return false; // MobileLocationProvider not found, or not enabled
        }

        /* has configured MobileLocationProvider */
        return true;

    }

    /**
    *** Gets the last received serving cell-tower information
    *** @return The last received serving cell-tower information
    **/
    public String getLastCellServingInfo()
    {
        String v = (String)this.getFieldValue(FLD_lastCellServingInfo);
        return StringTools.trim(v);
    }

    /**
    *** Sets the last received serving cell-tower information
    *** @param v The last received serving cell-tower information
    **/
    public void setLastCellServingInfo(String v)
    {
        this.setFieldValue(FLD_lastCellServingInfo, StringTools.trim(v));
    }
    
    /**
    *** Sets the last received serving cell-tower information
    *** @param sct The last received serving cell-tower information
    **/
    public void setLastServingCellTower(CellTower sct)
    {
        if (sct != null) {
            this.setLastCellServingInfo(sct.toString());
        } else {
            this.setLastCellServingInfo(null);
        }
    }

    /**
    *** Gets the last received serving cell-tower information
    *** @return The last received serving cell-tower information
    **/
    public CellTower getLastServingCellTower()
    {
        String csi = this.getLastCellServingInfo();
        if (!StringTools.isBlank(csi)) {
            CellTower ct = new CellTower(csi);
            ct.setRequestorDevice(this); // [2.6.5-B49]
            return ct;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Geozone transition container for providing arrive/depart events
    **/
    public static class GeozoneTransition
    {
        private long    time = 0L;
        private int     code = StatusCodes.STATUS_NONE;
        private Geozone zone = null;
        public GeozoneTransition(long timestamp, int code, Geozone zone) {
            this.time = timestamp;
            this.code = code;
            this.zone = zone; // can be null
        }
        public long getTimestamp() {
            return this.time;
        }
        public int getStatusCode() {
            return this.code;
        }
        public Geozone getGeozone() {
            return this.zone;
        }
        public String getGeozoneID() {
            return (this.zone != null)? this.zone.getGeozoneID() : "";
        }
        public String getGeozoneDescription() {
            return (this.zone != null)? this.zone.getDescription() : "";
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[").append(StatusCodes.GetDescription(this.getStatusCode(),null)).append("] ");
            sb.append(this.getGeozoneID()).append(" - ");
            sb.append(this.getGeozoneDescription());
            return sb.toString();
        }
    }

    /**
    *** Checks the new event time and GeoPoint to calculate and returns a set of 
    *** Geozone arrive/depart events, which should be inserted into the EventData table.
    *** If no Geozone transition occurred, then this method returns null.
    *** @param eventTime  The tie of the event
    *** @param eventGP    The new event location
    *** @return A list of Geozone transitions, or null if no transition occurred.
    **/
    public java.util.List<GeozoneTransition> checkGeozoneTransitions(long eventTime, GeoPoint eventGP)
    {

        /* invalid point? */
        if (!GeoPoint.isValid(eventGP)) {
            return null;
        }

        /* invalid timestamp? */
        if (eventTime < 2L) {
            return null;
        }

        /* ignore event if older than last received event */
        if (this.isOldEventTimestamp(eventTime)) { // [2.4.8-B25]
            Print.logWarn("Geozone check: ignoring older event ...");
            return null;
        }

        /* look for Geozone transitions */
        String deviceID  = this.getDeviceID();
        String accountID = this.getAccountID();

        /* previous location */
        GeoPoint prevGP = this.getLastValidLocation(true);
        if (!GeoPoint.isValid(prevGP)) {
            // -- this device has no previous valid location, thus no arrive/depart
            return null;
        }

        /* transition accumulator */
        java.util.List<GeozoneTransition> geoTrans = null;

        /* properties */
        // -- Device.separateGeozoneCategories=false
        boolean GET_CATEGORY_GEOZONES_FOR_DEVICE = RTConfig.getBoolean(DBConfig.PROP_Device_separateGeozoneCategories,true); // [2.6.0-B47]
        boolean GET_SINGLE_GEOZONE_FOR_DEVICE    = true; // [2.4.9-B15]

        // ---------------------------------------
        // -- Device transitions
        // -- DeviceProximity
        // -  Fields:
        // -    accountID     (key)
        // -    deviceID      (key)
        // -    proximityType (key) [0=Unknown, 1=GeozoneID, 2=DeviceID]
        // -    proximityID   (key) [GeozoneID/DeviceID]
        // -    timestamp
        // -    statusCode
        // -  Methods:
        // -    Collection<DeviceProximity> getProximityList(Device dev);
        // -    void getProximityGeozoneID(); // if (proximityType == 1)
        // -    void getProximityDeviceID();  // if (proximityType == 2)
        // -    static void removeProximityID(Device dev, int proxType, String proxID);
        // -    static void removeProximityDeviceID(Device dev, String pDevID);
        // -    static void addProximityID(Device dev, int proxType, String proxID, long timestamp, int statusCode);
        // -    static void addProximityDeviceID(Device dev, String pDevID, long timestamp, int statusCode);
        // ---
        // -  TODO: NOT YET IMPLEMENTED
        // -    double proxRadM = this.getProximityRadius();
        // -    if (proxRadM > 0.0) {
        // -        String proxGroupID = this.getProximityGroupID();
        // -        User currUser = null; // TODO: get current user
        // -        Map<String,Device.NearbyDevice> nbMap = this.getNearbyDevices(proxRadM, 0L, currUser);
        // -        Collection<DeviceProximity> dpList = DeviceProximity.getProximityList(this); // proxGroupID
        // -        // -- check for proximity departs
        // -        for (DeviceProximity dp : dpList) {
        // -            String dpDevID = dp.getProximityDeviceID();
        // -            Device.NearbyDevice nb = (nbMap != null)? nbMap.get(dpDevID) : null;
        // -            if (nb != null) {
        // -                // -- still near this device
        // -                nbMap.remove(dpDevID);
        // -            } else {
        // -                // -- departed this device
        // -                DeviceProximity.removeProximityDeviceID(this,dpDevID);
        // -                // -- TODO: add DeviceTransition: StatusCodes.STATUS_PROXIMITY_DEPART
        // -                if (geoTrans == null) { geoTrans = new Vector<GeozoneTransition>(); }
        // -                int sc = StatusCodes.STATUS_PROXIMITY_DEPART;
        // -                geoTrans.add(new GeozoneTransition(eventTime - 2L, sc, prevZone)); // depart
        // -                // --
        // -            }
        // -        }
        // -        // -- check for proximity arrives
        // -        if (ListTools.size(nbMap) > 0) {
        // -            for (String nbDevID : nbMap) {
        // -                Device.NearbyDevice nb = nbMap.get(nbDevID);
        // -                // -- arrived this device
        // -                DeviceProximity.addProximityDeviceID(this,nbDevID,eventTime);
        // -                // -- TODO: add DeviceTransition: StatusCodes.STATUS_PROXIMITY_ARRIVE
        // -                if (geoTrans == null) { geoTrans = new Vector<GeozoneTransition>(); }
        // -                int sc = StatusCodes.STATUS_PROXIMITY_ARRIVE;
        // -                geoTrans.add(new GeozoneTransition(eventTime - 1L, sc, prevZone)); // arrive
        // -                // --
        // -            }
        // -        }
        // -    }

        // ---------------------------------------
        // -- Geozone transitions

        /* get active previous event Geozone */
        Map<String,Geozone> prevCatZones = null;
        try {
            // -- get previous Geozone(s)
            if (GET_CATEGORY_GEOZONES_FOR_DEVICE) { // [2.6.0-B47]
                // -- treats different Geozone arrive/depart categories separately
                // -  returned Geozones are guaranteed to be active and applicable to the DeviceID
                prevCatZones = Geozone.getGeozonesForDevice(accountID, prevGP, deviceID); // may be null
            } else
            if (GET_SINGLE_GEOZONE_FOR_DEVICE) { // [2.4.9-B15]
                // -- treats all Geozone arrive/depart categories the same
                // -  returned Geozone is guaranteed to be active and applicable to the DeviceID
                Geozone gz = Geozone.getGeozoneForDevice(accountID, prevGP, deviceID);
                prevCatZones = new HashMap<String,Geozone>();
                prevCatZones.put(gz.getArrivalStatusCodeCategory(), gz);
            } else {
                // -- OBSOLETE
                // -  Note: This may fail for concentric Geozones with different group assignments.
                Geozone gz = Geozone.getGeozone(accountID, null/*zoneID*/, prevGP, false/*RGOnly*/);
                if ((gz != null) && gz.isDeviceInGroup(deviceID)) {
                    prevCatZones = new HashMap<String,Geozone>();
                    prevCatZones.put(gz.getArrivalStatusCodeCategory(), gz);
                }
            }
        } catch (DBException dbe) {
            Print.logException("Geozone error (previous zone)", dbe);
            prevCatZones = null;
        }
        // -- "prevCatZones" is non-null iff it is active and applicable to the specified DeviceID, null otherwise

        /* get current event Geozone */
        Map<String,Geozone> thisCatZones = null;
        try {
            // -- get current Geozone(s)
            if (GET_CATEGORY_GEOZONES_FOR_DEVICE) { // [2.6.0-B47]
                // -- treats different Geozone arrive/depart categories separately
                // -  returned Geozones are guaranteed to be active and applicable to the DeviceID
                thisCatZones = Geozone.getGeozonesForDevice(accountID, eventGP, deviceID); // may be null
            } else
            if (GET_SINGLE_GEOZONE_FOR_DEVICE) { // [2.4.9-B15]
                // -- treats all Geozone arrive/depart categories the same
                // -  returned Geozone is guaranteed to be active and applicable to the DeviceID
                Geozone gz = Geozone.getGeozoneForDevice(accountID, eventGP, deviceID);
                thisCatZones = new HashMap<String,Geozone>();
                thisCatZones.put(gz.getArrivalStatusCodeCategory(), gz);
            } else {
                // -- OBSOLETE
                // -  Note: This may fail for concentric Geozones with different group assignments.
                Geozone gz = Geozone.getGeozone(accountID, null/*zoneID*/, eventGP, false/*RGOnly*/);
                if ((gz != null) && gz.isDeviceInGroup(deviceID)) {
                    thisCatZones = new HashMap<String,Geozone>();
                    thisCatZones.put(gz.getArrivalStatusCodeCategory(), gz);
                }
            }
        } catch (DBException dbe) {
            Print.logException("Geozone error (current zone)", dbe);
            thisCatZones = null;
        }
        // -- "thisCatZones" is non-null iff it is active and applicable to the specified DeviceID, null otherwise

        /* loop through categories */
        if ((prevCatZones != null) || (thisCatZones != null)) {
          //Set<String> gzAllCats = ListTools.unionMapKeys(new HashSet<String>(),prevCatZones,thisCatZones);
            Set<String> gzAllCats = new HashSet<String>();
            if (prevCatZones != null) { gzAllCats.addAll(prevCatZones.keySet()); }
            if (thisCatZones != null) { gzAllCats.addAll(thisCatZones.keySet()); }
            for (String cat : gzAllCats) {

                /* get previous/this Geozone for category */
                Geozone prevZone = (prevCatZones != null)? prevCatZones.get(cat) : null;
                Geozone thisZone = (thisCatZones != null)? thisCatZones.get(cat) : null;

                /* Geozone depart only */
                // -- simple depart, is not in any Geozone now
                if ((prevZone != null) && (thisZone == null)) {
                    String devID = null; // <-- We've already verified that this is applicable to "devID"
                    boolean isDepart = prevZone.isDepartureZone(devID);
                    if (isDepart) {
                        if (geoTrans == null) { geoTrans = new Vector<GeozoneTransition>(); }
                        int sc = prevZone.getDepartureStatusCode(); // StatusCodes.STATUS_GEOFENCE_DEPART
                        geoTrans.add(new GeozoneTransition(eventTime - 2L, sc, prevZone)); // depart
                    }
                    continue;
                }
    
                /* Geozone arrive only */
                // -- simple arrive, was not in any Geozone before
                if ((prevZone == null) && (thisZone != null)) {
                    String devID = null; // <-- We've already verified that this is applicable to "devID"
                    boolean isArrive = thisZone.isArrivalZone(devID);
                    if (isArrive) {
                        if (geoTrans == null) { geoTrans = new Vector<GeozoneTransition>(); }
                        int sc = thisZone.getArrivalStatusCode(); // StatusCodes.STATUS_GEOFENCE_ARRIVE
                        geoTrans.add(new GeozoneTransition(eventTime - 1L, sc, thisZone)); // arrive
                    }
                    continue;
                }
    
                /* Geozone depart, then Geozone arrive */
                // -- transition from one Geozone straight into another
                if ((prevZone != null) && (thisZone != null) && 
                    !prevZone.getGeozoneID().equals(thisZone.getGeozoneID())) {
                    String devID = null; // <-- We've already verified that this is applicable to "devID"
                    // -- depart?
                    boolean isDepart = prevZone.isDepartureZone(devID);
                    if (isDepart) {
                        if (geoTrans == null) { geoTrans = new Vector<GeozoneTransition>(); }
                        int sc = prevZone.getDepartureStatusCode(); // StatusCodes.STATUS_GEOFENCE_DEPART
                        geoTrans.add(new GeozoneTransition(eventTime - 2L, sc, prevZone)); // depart
                    }
                    // -- arrive?
                    boolean isArrive = thisZone.isArrivalZone(devID);
                    if (isArrive) {
                        if (geoTrans == null) { geoTrans = new Vector<GeozoneTransition>(); }
                        int sc = thisZone.getArrivalStatusCode();   // StatusCodes.STATUS_GEOFENCE_ARRIVE
                        geoTrans.add(new GeozoneTransition(eventTime - 1L, sc, thisZone)); // arrive
                    }
                    continue;
                }
    
            } // loop through categories
        }

        /* return geozone transition (may be null) */
        return geoTrans;

    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a unique Geozone ID which may be used by the ENRE predefined-actions
    **/
    public String getAutoGeozoneID(EventData ev)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getDeviceID());
        sb.append("_");
        sb.append((ev != null)? ev.getTimestamp() : DateTime.getCurrentTimeSec());
        return sb.toString(); // "demo_1428735600"
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last distance value
    *** @return The last distance value
    **/
    public double getLastDistanceKM()
    {
        return this.getOptionalFieldValue(FLD_lastDistanceKM, 0.0);
    }

    /**
    *** Sets the last distance value
    *** @param distKM  The last distance value
    **/
    public void setLastDistanceKM(double distKM)
    {
        if (distKM < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_lastDistanceKM, distKM);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum allowed odometer value
    *** @return The maximum allowed odometer value
    **/
    public double getMaxOdometerKM()
    {
        // TODO: should be device dependent
        return Device.GetMaximumOdometerKM();
    }

    /**
    *** Returns true if the Device record supports the last odometer fields 
    *** @return True if the Device record supports the last odometer fields 
    **/
    public static boolean supportsLastOdometer()
    {
        // now always true
        return Device.getFactory().hasField(FLD_lastOdometerKM);
    }

    /**
    *** Gets the last odometer value
    *** @return The last odometer value
    **/
    public double getLastOdometerKM()
    {
        return this.getOptionalFieldValue(FLD_lastOdometerKM, 0.0);
    }

    /*
    public double getLastOdometerKM(boolean tryLastEvent)
    {
        double odomKM = this.getLastOdometerKM();
        if (odomKM > 0.0) {
            return odomKM;
        } else
        if (tryLastEvent) {
            try {
                EventData lastEv = this.getLastEvent(true);
                if ((lastEv != null) && lastEv.isValidGeoPoint()) {
                    odomKM = lastEv.getOdometerKM(); // may be 0
                    this.setLastOdometerKM(odomKM);                         // FLD_lastOdometerKM
                    if (this.getLastValidLocation() == null) {
                        this.setLastValidLocation(
                            lastEv.getTimestamp(),      // FLD_lastGPSTimestamp
                            lastEv.getGpsAge(),         // [2.6.2-B44]
                            lastEv.getGeoPoint(),       // FLD_lastValidLatitude/FLD_lastValidLongitude
                            lastEv.getSpeedKPH(),       // FLD_lastValidSpeedKPH
                            lastEv.getHeading());       // FLD_lastValidHeading
                        this.setLastGPSTimestamp();    
                    }
                    return odomKM;
                } else {
                    return 0.0;
                }
            } catch (DBException dbe) {
                // ignore error
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }
    */

    /**
    *** Sets the last odometer value
    *** @param odomKM  The last odometer value
    **/
    public void setLastOdometerKM(double odomKM)
    {
        if (odomKM < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_lastOdometerKM, odomKM);
        }
    }

    /**
    *** Calculates the next odometer value based on the specified GeoPoint
    *** @param geoPoint  The next GPS location
    *** @return The last odometer, plus the distance to the specified GeoPoint
    **/
    public double getNextOdometerKM(GeoPoint geoPoint)
    {
        GeoPoint lastValidLoc = this.getLastValidLocation(true); // try last event
        double odomKM = this.getLastOdometerKM(); // only try cached value
        if (GeoPoint.isValid(geoPoint) && (lastValidLoc != null)) {
            odomKM += geoPoint.kilometersToPoint(lastValidLoc);
        }
        return odomKM;
    }

    /**
    *** Adjusts the specified odometer value to the maximum allow value
    *** @param odomKM  The odometer value to adjust
    *** @return The adjusted odometer value
    **/
    public double adjustOdometerKM(double odomKM)
    {
        return this.adjustOdometerKM(odomKM, Device.GetCheckLastOdometer());
    }

    /**
    *** Adjusts the specified odometer value to the maximum allow value
    *** @param odomKM     The odometer value to adjust
    *** @param checkLast  True to return last-odometer if specified odometer is 
    ***                   less than last-odometer.
    *** @return The adjusted odometer value
    **/
    public double adjustOdometerKM(double odomKM, boolean checkLast)
    {
        double lastOdomKM = this.getLastOdometerKM();
        if (checkLast && (odomKM < lastOdomKM)) {
            return lastOdomKM;
        } else
        if (odomKM >= this.getMaxOdometerKM()) {
            return lastOdomKM;
        } else {
            return odomKM;
        }
    }

    /**
    *** Calculates an odometer value based on the specified attributes
    *** @param odomKM   The odometer value from the device (or 0.0 if the device does not provide an odometer)
    *** @param fixtime  The timestamp of the event
    *** @param validGPS The GPS fix state
    *** @param geoPoint The GPS location
    *** @param estimate True if the odometer is to be calculated based on the GPS location
    *** @param logInfo  True to display the results via "Print.logInfo"
    *** @return The calculated odometer value
    **/
    public double calculateOdometerKM(double odomKM,
        long fixtime, boolean validGPS, GeoPoint geoPoint,
        boolean estimate, boolean logInfo)
    {
        if (this.isOldEventTimestamp(fixtime)) {
            // old event, only allow odometer values from the device itself
            odomKM = estimate? 
                0.0 : // we cannot accurately calculate an odometer value
                this.adjustOdometerKM(odomKM);
            if (logInfo) { Print.logInfo("OdometerKM: " + odomKM + " (old event)"); }
        } else
        if ((odomKM <= 0.0) || estimate) {
            // current event and we need to calculate the odomenter
            odomKM = (estimate && validGPS)? 
                this.getNextOdometerKM(geoPoint) : 
                this.getLastOdometerKM();
            if (logInfo) { Print.logInfo("OdometerKM: " + odomKM + " (estimated)"); }
        } else {
            // we already have an odometer value from the device
            odomKM = this.adjustOdometerKM(odomKM);
            if (logInfo) { Print.logInfo("OdometerKM: " + odomKM + " (actual)"); }
        }
        return odomKM;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the displayed odometer offset in kilometers.
    *** @return The displayed odometer offset in kilometers.
    **/
    public double getOdometerOffsetKM()
    {
        return this.getOptionalFieldValue(FLD_odometerOffsetKM, 0.0);
    }

    /**
    *** Sets the displayed odometer offset in kilometers.
    *** @param v The displayed odometer offset in kilometers.
    **/
    public void setOdometerOffsetKM(double v)
    {
        if (v < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_odometerOffsetKM, v);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the engine-hours at the last engine-on time
    *** @return The engine-hours at the last engine-on time
    **/
    public double getLastEngineOnHours()
    {
        return this.getOptionalFieldValue(FLD_lastEngineOnHours, 0.0);
    }

    /**
    *** Sets the engine-hours at the last engine-on time
    *** @param v The engine-hours at the last engine-on time
    **/
    public void setLastEngineOnHours(double v)
    {
        if (v < this.getMaxRuntimeHours()) {
            this.setOptionalFieldValue(FLD_lastEngineOnHours, v);
        }
    }

    /**
    *** Gets the last engine on time received
    *** @return The last engine on time received
    **/
    public long getLastEngineOnTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastEngineOnTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last engine on time received
    *** @param v The last engine on time received
    **/
    public void setLastEngineOnTime(long v)
    {
        this.setFieldValue(FLD_lastEngineOnTime, v);
    }

    /**
    *** Gets the last engine off time received
    *** @return The last engine off time received
    **/
    public long getLastEngineOffTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastEngineOffTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last engine off time received
    *** @param v The last engine off time received
    **/
    public void setLastEngineOffTime(long v)
    {
        this.setFieldValue(FLD_lastEngineOffTime, v);
    }

    /**
    *** Gets the maximum allowed engine-hours value
    *** @return The maximum allowed engine-hours value
    **/
    public double getMaxRuntimeHours()
    {
        // TODO: should be device dependent
        return Device.GetMaximumRuntimeHours();
    }

    /**
    *** Returns true if LastEngineHours is supported
    *** @return True if LastEngineHours is supported
    **/
    public static boolean supportsLastEngineHours()
    {
        // alway true
        return Device.getFactory().hasField(FLD_lastEngineHours);
    }

    /**
    *** Gets the last engine-hours received
    *** @return The last engine-hours received
    **/
    public double getLastEngineHours()
    {
        return this.getOptionalFieldValue(FLD_lastEngineHours, 0.0);
    }

    /**
    *** Sets the last engine-hours received
    *** @param v The last engine-hours received
    **/
    public void setLastEngineHours(double v)
    {
        if (v < this.getMaxRuntimeHours()) {
            this.setOptionalFieldValue(FLD_lastEngineHours, v);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the PTO-hours at the last PTO-on time
    *** @return The PTO-hours at the last PTO-on time
    **/
    public double getLastPtoOnHours()
    {
        return this.getOptionalFieldValue(FLD_lastPtoOnHours, 0.0);
    }

    /**
    *** Sets the PTO-hours at the last PTO-on time
    *** @param v The PTO-hours at the last PTO-on time
    **/
    public void setLastPtoOnHours(double v)
    {
        this.setOptionalFieldValue(FLD_lastPtoOnHours, v);
    }

    /**
    *** Gets the last PTO on time received
    *** @return The last PTO on time received
    **/
    public long getLastPtoOnTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastPtoOnTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last PTO on time received
    *** @param v The last PTO on time received
    **/
    public void setLastPtoOnTime(long v)
    {
        this.setFieldValue(FLD_lastPtoOnTime, v);
    }

    /**
    *** Gets the last PTO off time received
    *** @return The last PTO off time received
    **/
    public long getLastPtoOffTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastPtoOffTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last PTO off time received
    *** @param v The last PTO off time received
    **/
    public void setLastPtoOffTime(long v)
    {
        this.setFieldValue(FLD_lastPtoOffTime, v);
    }

    /**
    *** Returns true if LastPtoHours is supported
    *** @return True if LastPtoHours is supported
    **/
    public static boolean supportsLastPtoHours()
    {
        // -- alway true?
        return Device.getFactory().hasField(FLD_lastPtoHours);
    }

    /**
    *** Gets the last PTO-hours received
    *** @return The last PTO-hours received
    **/
    public double getLastPtoHours()
    {
        return this.getOptionalFieldValue(FLD_lastPtoHours, 0.0);
    }

    /**
    *** Sets the last PTO-hours received
    *** @param v The last PTO-hours received
    **/
    public void setLastPtoHours(double v)
    {
        this.setOptionalFieldValue(FLD_lastPtoHours, v);
    }

    /**
    *** Gets the current PTO state
    **/
    public int getCurrentPtoState()
    {
        long lastPtoOn  = this.getLastPtoOnTime();
        long lastPtoOff = this.getLastPtoOffTime();
        if ((lastPtoOn > 0L) || (lastPtoOff > 0L)) {
            // -- PTO state should be known
            if ((lastPtoOn > 0L) && (lastPtoOn > lastPtoOff)) {
                // -- last PTO was on
                return 1;
            } else
            if ((lastPtoOff > 0L) && (lastPtoOff > lastPtoOn)) {
                // -- last PTO was off
                return 0;
            } else {
                // (lastPtoOff == lastPtoOn) unlikely (but can occur)
            }
        }
        // -- unknown
        return -1;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the displayed engine-hours offset
    *** @return The displayed engine-hours offset
    **/
    public double getEngineHoursOffset()
    {
        return this.getOptionalFieldValue(FLD_engineHoursOffset, 0.0);
    }

    /**
    *** Sets the displayed engine-hours offset
    *** @param v The displayed engine-hours offset
    **/
    public void setEngineHoursOffset(double v)
    {
        this.setOptionalFieldValue(FLD_engineHoursOffset, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ignition hours at the last ignition-on.
    *** @return The ignition hours at the last ignition-on.
    **/
    public double getLastIgnitionOnHours()
    {
        return this.getOptionalFieldValue(FLD_lastIgnitionOnHours, 0.0);
    }

    /**
    *** Sets the ignition hours at the last ignition-on.
    *** @param v The ignition hours at the last ignition-on.
    **/
    public void setLastIgnitionOnHours(double v)
    {
        this.setOptionalFieldValue(FLD_lastIgnitionOnHours, v);
    }


    /**
    *** Gets the last ignition on time received
    *** @return The last ignition on time received
    **/
    public long getLastIgnitionOnTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastIgnitionOnTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last ignition on time received
    *** @param v The last ignition on time received
    **/
    public void setLastIgnitionOnTime(long v)
    {
        long lastTS = this.getLastIgnitionOnTime();
        this.setFieldValue(FLD_lastIgnitionOnTime, v);
        if (v != lastTS) {
            // -- clear cached ignition state, if timestamp changed
            this.cacheIgnitionState = -2;
        }
    }

    /**
    *** Gets the last ignition off time received
    *** @return The last ignition off time received
    **/
    public long getLastIgnitionOffTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastIgnitionOffTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last ignition off time received
    *** @param v The last ignition off time received
    **/
    public void setLastIgnitionOffTime(long v)
    {
        long lastTS = this.getLastIgnitionOffTime();
        this.setFieldValue(FLD_lastIgnitionOffTime, v);
        if (v != lastTS) {
            // -- clear cached ignition state, if timestamp changed
            this.cacheIgnitionState = -2;
        }
    }

    /**
    *** Gets the last ignition hours received.
    *** @return The last ignition hours received
    **/
    public double getLastIgnitionHours()
    {
        return this.getOptionalFieldValue(FLD_lastIgnitionHours, 0.0);
    }

    /**
    *** Sets the last ignition hours received.
    *** @param v The last ignition hours received
    **/
    public void setLastIgnitionHours(double v)
    {
        this.setOptionalFieldValue(FLD_lastIgnitionHours, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last stopped time received.
    *** @return The last stopped time received
    **/
    public long getLastStopTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastStopTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last stopped time received.
    *** @param v The last stopped time received
    **/
    public void setLastStopTime(long v)
    {
        this.setFieldValue(FLD_lastStopTime, v);
    }

    /**
    *** Returns true if the device is currently stopped, based on the start/stop
    *** configuration calculated as each event arrives.
    *** @return True if the device is currently stopped.
    **/
    public boolean isStopped()
    {
        long stopTime = this.getLastStopTime();  // may be '0' if uninitialized
        if (stopTime <= 0L) {
            return false;
        } else {
            return (stopTime > this.getLastStartTime())? true : false;
        }
    }

    /**
    *** Gets the last Stop event
    *** @return The last Stop event
    **/
    public EventData getLastStopEvent()
    {

        // last stopped timestamp
        long st = this.getLastStopTime();
        if ((st <= 0L) || (st <= this.getLastStartTime())) {
            // not stopped
            return null;
        }

        // get event
        try {
            EventData ev[] = this.getRangeEvents(
                st, st, // timeStart, timeEnd
                null, // statusCodes[]
                false, // validGPS
                EventData.LimitType.FIRST, -1L); // limit
            if (ListTools.isEmpty(ev)) {
                Print.logWarn("Last stopped time event not found: " + st);
                return null;
            } else {
                for (EventData evt : ev) {
                    if (evt.isStopEvent(true)) {
                        return evt;
                    }
                }
                Print.logWarn("LastStopEvent is not a stop-event! " + st);
                return null;
            }
        } catch (DBException dbe) {
            Print.logException("Getting last stop event", dbe);
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last Start time
    *** @return The last Start time
    **/
    public long getLastStartTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastStartTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last Start time
    *** @param v The last Start time
    **/
    public void setLastStartTime(long v)
    {
        this.setFieldValue(FLD_lastStartTime, v);
    }

    // --------------------------------

    /**
    *** Returns true if device has moved since the prior midnight
    **/
    public boolean hasMovedSinceMidnight(long currentTime, TimeZone localTMZ)
    {
        long lastStopTime  = this.getLastStopTime();  // '0' if uninitialized
        long lastStartTime = this.getLastStartTime(); // '0' if uninitialized
        if (lastStopTime <= 0L) {
            // -- assume moving: stop is uninitialized, assume we've never stopped
            return false;
        } else 
        if (lastStartTime > lastStopTime) {
            // -- is moving: start time is more recent that stop time
            return false;
        } else {
            // -- is stopped: check for stop > lastMidnight
            long nowTime = (currentTime > 0L)? currentTime : DateTime.getCurrentTimeSec();
            TimeZone tmz = (localTMZ != null)? localTMZ : Account.getTimeZone(this.getAccount(),DateTime.GMT);
            long lastMidnightTime = (new DateTime(nowTime)).getDayStart(tmz);
            return (lastStopTime > lastMidnightTime)? true : false;
        }
    }

    /**
    *** Returns true if device has moved since the prior midnight
    **/
    public boolean hasMovedSinceMidnight(long currentTime)
    {
        return this.hasMovedSinceMidnight(currentTime, null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last Malfuntion Indicator Lamp (MIL) state
    *** @return The last Malfuntion Indicator Lamp (MIL) state
    **/
    public boolean getLastMalfunctionLamp()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_lastMalfunctionLamp);
        return (v != null)? v.booleanValue() : true;
    }

    /**
    *** Sets the last Malfuntion Indicator Lamp (MIL) state
    *** @param v The last Malfuntion Indicator Lamp (MIL) state
    **/
    public void setLastMalfunctionLamp(boolean v)
    {
        this.setFieldValue(FLD_lastMalfunctionLamp, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if Fault Codes are supported
    *** @return True if Fault Codes are supported
    **/
    public static boolean supportsFaultCodes()
    {
        return Device.getFactory().hasField(Device.FLD_lastFaultCode)
            && EventData.getFactory().hasField(EventData.FLD_faultCode)
            ;
    }

    /**
    *** Gets the last fault codes
    *** @return The last fault codes
    **/
    public String getLastFaultCode()
    {
        String v = (String)this.getFieldValue(FLD_lastFaultCode);
        return StringTools.trim(v);
    }

    /**
    *** Sets the last fault codes
    *** @param v The last fault codes
    **/
    public void setLastFaultCode(String v)
    {
        String fc = StringTools.trim(v);
        if ((Device.LastFaultCodeColumnLength > 0)           &&
            (fc.length() >= Device.LastFaultCodeColumnLength)  ) {
            // -1 so we are not so close to the edge of the cliff
            int newLen = Device.LastFaultCodeColumnLength - 1; 
            fc = fc.substring(0, newLen).trim();
            // Note: MySQL will refuse to insert the entire record if the data 
            // length is greater than the table column length.
        }
        this.setFieldValue(FLD_lastFaultCode, fc);
    }

    /**
    *** Appends the specified fault code to the current list of fault codes
    *** @param v  The fault code to add
    **/
    public void appendLastFaultCode(String v)
    {
        String lastFCStr = this.getLastFaultCode();
        if (StringTools.isBlank(lastFCStr)) {
            // this Device does not already have a fault code 
            this.setLastFaultCode(v);
        } else {
            // append new fault codes to old
            RTProperties newFC = new RTProperties(v);
            RTProperties oldFC = new RTProperties(lastFCStr);
            if (DTOBDFault.IsOBDII(oldFC)) {
                String newDTC[] = StringTools.split(newFC.getString(DTOBDFault.PROP_DTC,""),',');
                String oldDTC[] = StringTools.split(oldFC.getString(DTOBDFault.PROP_DTC,""),',');
                boolean changed = false;
                for (String dtc : newDTC) {
                    if (StringTools.isBlank(dtc)) { continue; }
                    if (!ListTools.contains(oldDTC,dtc)) {
                        oldDTC = ListTools.add(oldDTC,dtc);
                        changed = true;
                    }
                }
                if (changed) {
                    oldFC.setString(DTOBDFault.PROP_DTC[0],StringTools.join(oldDTC,","));
                    this.setLastFaultCode(oldFC.toString());
                }
            } else
            if (DTOBDFault.IsJ1708(oldFC)) {
                // TODO: append
                this.setLastFaultCode(v);
            } else
            if (DTOBDFault.IsJ1939(oldFC)) {
                // TODO: append
                this.setLastFaultCode(v);
            } else {
                // ???
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** (NOT FULLY IMPLEMENTED) Gets the Ping command URI/URL
    *** @return The Ping command URI/URL
    **/
    public String getPingCommandURI()
    {
        String v = (String)this.getFieldValue(FLD_pingCommandURI);
        return StringTools.trim(v);
    }

    /**
    *** (NOT FULLY IMPLEMENTED) Sets the Ping command URI/URL
    *** @param v The Ping command URI/URL
    **/
    public void setPingCommandURI(String v)
    {
        // valid options:
        //   tcp://192.168.11.11:21500
        //   udp://192.168.11.11:31400
        //   sms://9165551212
        //   smtp://9165551212@example.com
        this.setFieldValue(FLD_pingCommandURI, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the currently pending commands which should be sent to the device
    *** @return Any pending device commands
    **/
    public String getPendingPingCommand()
    {
        String v = (String)this.getFieldValue(FLD_pendingPingCommand);
        return StringTools.trim(v);
    }
    public String getPendingCommand()
    {
        return this.getPendingPingCommand();
    }

    /**
    *** Sets the currently pending commands which should be sent to the device
    *** @param v Any pending device commands
    **/
    public void setPendingPingCommand(String v)
    {
        this.setFieldValue(FLD_pendingPingCommand, StringTools.trim(v));
    }
    public void setPendingCommand(String v)
    {
        this.setPendingPingCommand(v);
    }

    /**
    *** Returns true if this device contains any pending commands
    *** @return True if this device contains any pending commands
    **/
    public boolean hasPendingPingCommand()
    {
        return !StringTools.isBlank(this.getPendingPingCommand());
    }
    public boolean hasPendingCommand()
    {
        return this.hasPendingPingCommand();
    }
    
    /**
    *** Clears the device pending commands
    *** @param update  True to update the device record now
    *** @return True if the device record was successfully cleared and updated, false otherwise
    **/
    public boolean clearPendingPingCommand(boolean update)
    {
        this.setPendingPingCommand(null);
        if (update) {
            try {
                this.update(Device.FLD_pendingPingCommand);
                return true; // successfully updated
            } catch (DBException dbe) {
                Print.logException("Unable to update Device.pendingPingCommand", dbe);
                return false; // failed to update
            }
        } else {
            return false; // update not requested
        }
    }
    public boolean clearPendingCommand(boolean update)
    {
        return this.clearPendingPingCommand(update);
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the time of the last command sent to the device
    *** @return The time of the last command sent to the device
    **/
    public long getLastPingTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastPingTime);
        return (v != null)? v.longValue() : 0L;
    }

    /** 
    *** Sets the time of the last command sent to the device
    *** @param v The time of the last command sent to the device
    **/
    public void _setLastPingTime(long v)
    {
        this.setFieldValue(FLD_lastPingTime, v);
    }

    /** 
    *** Sets the time of the last command sent to the device
    *** @param v The time of the last command sent to the device
    **/
    public void setLastPingTime(long v)
    {
        this._setLastPingTime(v);
        if (this.transport != null) {
            this.transport._setLastPingTime(v);
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the total number of commands sent to the device, since last reset
    *** @return The total number of commands sent to the device, since last reset
    **/
    public int getTotalPingCount()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalPingCount);
        return (v != null)? v.intValue() : 0;
    }

    /** 
    *** Sets the total number of commands sent to the device, since last reset
    *** @param v The total number of commands sent to the device, since last reset
    **/
    public void _setTotalPingCount(int v)
    {
        v = (v > 0xFFFF)? 0xFFFF : (v > 0)? v : 0; // limit to 16-bit
        this.setFieldValue(FLD_totalPingCount, v);
    }

    /** 
    *** Sets the total number of commands sent to the device, since last reset
    *** @param v The total number of commands sent to the device, since last reset
    **/
    public void setTotalPingCount(int v)
    {
        v = (v > 0xFFFF)? 0xFFFF : (v > 0)? v : 0; // limit to 16-bit
        this._setTotalPingCount(v);
        if (this.transport != null) {
            this.transport._setTotalPingCount(v);
        }
    }

    /**
    *** Increments the command count for this device
    *** @param pingTime  The time of the command
    *** @param reload    True to force a reload of the Device record prior to counting the command
    *** @param update    True to update the Device after incrementing the count
    *** @return True if successfully incremented
    **/
    public boolean incrementPingCount(long pingTime, boolean reload, boolean update)
    {

        /* refresh current value */
        if (reload) {
            // in case another Device 'ping' has changed this value already
            this.reload(Device.FLD_totalPingCount);
        }

        /* increment ping count */
        this.setTotalPingCount(this.getTotalPingCount() + 1);
        if (pingTime > 0L) {
            this.setLastPingTime(pingTime);
        }

        /* update Device record */
        if (update) {
            try {
                this.update( // may throw DBException
                    Device.FLD_lastPingTime,
                    Device.FLD_totalPingCount);
            } catch (DBException dbe) {
                Print.logException("Unable to update 'ping' count", dbe);
                return false;
            }
        }

        /* update Account */
        Account account = this.getAccount();
        if (account != null) {
            account.incrementPingCount(pingTime, reload, update);
        }

        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum number of commands that can be sent to the device
    *** @return The maximum number of commands that can be sent to the device
    **/
    public int getMaxPingCount()
    {
        Integer v = (Integer)this.getFieldValue(FLD_maxPingCount);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** Sets the maximum number of commands that can be sent to the device
    *** @param v The maximum number of commands that can be sent to the device
    **/
    public void _setMaxPingCount(int v)
    {
        v = (v > 0xFFFF)? 0xFFFF : (v > 0)? v : 0; // limit to 16-bit
        this.setFieldValue(FLD_maxPingCount, v);
    }

    /**
    *** Sets the maximum number of commands that can be sent to the device
    *** @param v The maximum number of commands that can be sent to the device
    **/
    public void setMaxPingCount(int v)
    {
        v = (v > 0xFFFF)? 0xFFFF : (v > 0)? v : 0; // limit to 16-bit
        this._setMaxPingCount(v);
        if (this.transport != null) {
            this.transport._setMaxPingCount(v);
        }
    }
    
    /**
    *** Returns true if the maximum number of commands sent to the server has 
    *** been exceeded.
    *** @return True if the maximum number has been exceeded
    ***/
    public boolean exceedsMaxPingCount() 
    {

        /* check device */
        {
            int totPings = this.getTotalPingCount();
            int maxPings = this.getMaxPingCount();
            if ((maxPings > 0) && (totPings >= maxPings)) {
                Print.logInfo("Device exceeded maximum allowed pings: %d >= %d", totPings, maxPings);
                return true;
            }
        }

        /* check account */
        Account account = this.getAccount();
        if (account != null) {
            int totPings = account.getTotalPingCount();
            int maxPings = account.getMaxPingCount();
            if ((maxPings > 0) && (totPings >= maxPings)) {
                Print.logInfo("Account exceeded maximum allowed pings: %d >= %d", totPings, maxPings);
                return true;
            }
        }

        /* not over limit */
        return false;
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the command state mask as set by commands sent to the device
    *** @return The command state mask
    **/
    public long getCommandStateMask()
    {
        Long v = (Long)this.getFieldValue(FLD_commandStateMask);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Gets the state for the specified bit from the command state mask
    *** @param bit  The command state bit index
    *** @return The state for the specified bit from the command state mask
    **/
    public boolean getCommandStateMaskBit(int bit)
    {
        if ((bit >= 0) && (bit < 64)) {
            long v = this.getCommandStateMask();
            return ((v & (1L << bit)) != 0L)? true : false;
        } else {
            return false;
        }
    }

    /**
    *** Sets the command state mask as set by commands sent to the device
    *** @param v The command state mask
    **/
    public void setCommandStateMask(long v)
    {
        this.setFieldValue(FLD_commandStateMask, v);
        this.addOtherChangedFieldNames(FLD_commandStateMask);
    }

    /**
    *** Sets the state for the specified bit from the command state mask
    *** @param bit    The command state bit
    *** @param state  The state for the specified bits in the command state mask
    **/
    public void setCommandStateBit(int bit, boolean state)
    {
        if (bit >= 0) {
            this._setCommandStateMask((bit << 1L), state);
        }
    }

    /**
    *** Sets the state for the specified bit from the command state mask
    *** @param mask   The command state bit mask
    *** @param state  The state for the specified bits in the command state mask
    **/
    private void _setCommandStateMask(long mask, boolean state)
    {
        long v = this.getCommandStateMask();
        if (state) {
            v |=  mask; // bits on
        } else {
            v &= ~mask; // bits off
        }
        this.setCommandStateMask(v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if an ACK is expected from the device
    *** @return True if an ACK is expected from the device
    **/
    public boolean getExpectAck()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_expectAck);
        return (v != null)? v.booleanValue() : true;
    }

    /**
    *** Sets the expected ACK state
    *** @param v The expected ACK state
    **/
    public void _setExpectAck(boolean v)
    {
        this.setFieldValue(FLD_expectAck, v);
        this.addOtherChangedFieldNames(FLD_expectAck);
    }

    /**
    *** Sets the expected ACK state
    *** @param v The expected ACK state
    **/
    public void setExpectAck(boolean v)
    {
        this._setExpectAck(v);
        if (this.transport != null) {
            this.transport._setExpectAck(v);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the expected ACK status code, or '0' if any code should match
    *** @return The expected ACK status code
    **/
    public int getExpectAckCode()
    {
        Integer v = (Integer)this.getFieldValue(FLD_expectAckCode);
        return (v != null)? v.intValue() : StatusCodes.STATUS_NONE;
    }

    /**
    *** Sets the expected ACK status code, or '0' if any code should match
    *** @param v The expected ACK status code
    **/
    public void setExpectAckCode(int v)
    {
        this.setFieldValue(FLD_expectAckCode, ((v >= 0)? v : StatusCodes.STATUS_NONE));
        this.addOtherChangedFieldNames(FLD_expectAckCode);
    }

    /**
    *** Returns true if the device is expecting an ACK and the specified status
    *** code matched the expected ACK status code.
    *** @param statusCode The current event status code
    *** @return True if statusCode matched expected ACK status code
    **/
    public boolean isAckStatusCode(int statusCode)
    {

        /* invalid status code */
        if (statusCode <= 0) {
            // invalid status code specification
            return false;
        }

        /* device is not expecting an ACK */
        if (!this.getExpectingCommandAck()) {
            // device is not expecting an ACK
            return false;
        }

        /* check device ackCode */
        int ackCode = this.getExpectAckCode();
        if (ackCode <= 0) {
            // any status code specified an ACK
            return true;
        }

        /* check specific code */
        // true if codes match
        return (statusCode == ackCode)? true : false;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last ACK command sent to the device (if supported)
    *** @return The last ACK command sent to the device
    **/
    public String getLastAckCommand()
    {
        String v = (String)this.getFieldValue(FLD_lastAckCommand);
        return StringTools.trim(v);
    }

    /**
    *** Sets the last ACK command sent to the device
    *** @param v The last ACK command sent to the device
    **/
    public void setLastAckCommand(String v)
    {
        this.setFieldValue(FLD_lastAckCommand, StringTools.trim(v));
    }

    /** 
    *** Returns true if an ACK is expected from the device
    *** @return True if an ACK is expected from the device
    **/
    public boolean getExpectingCommandAck()
    {
        return this.getExpectAck() && (this.getLastAckTime() <= 0L);
    }

    /** 
    *** Clears the expect ACK state for the specified command
    *** @param didAck True if the ACK was received
    *** @param update True to update the Device record
    *** @return True if cleared
    **/
    public boolean clearExpectCommandAck(boolean didAck, boolean update)
    {

        /* not expecting an ACK? */
        if (!this.getExpectingCommandAck()) {
            Print.logInfo("Device is not expecting an ACK");
            return false;
        }

        /* clear ACK fields */
        String lastAckCmd = this.getLastAckCommand();
        this.setExpectAck(false);
        this.setExpectAckCode(StatusCodes.STATUS_NONE);
      //this.setLastAckCommand("");
      //this.setLastAckResponse("");
        if (didAck) {
            this.setLastAckTime(DateTime.getCurrentTimeSec());
            Print.logInfo("ACK received for command: " + lastAckCmd);
        } else {
            this.setLastAckTime(0L);
        }

        /* clear ACK command */
        if (update) {
            try {
                this.update(
                    Device.FLD_expectAck, 
                    Device.FLD_expectAckCode, 
                    Device.FLD_lastAckTime
                    );
                return true;
            } catch (DBException dbe) {
                Print.logException("Unable to set Device.lastAck...", dbe);
                return false;
            }
        } else {
            this.addOtherChangedFieldNames(
                Device.FLD_expectAck,
                Device.FLD_expectAckCode, 
                Device.FLD_lastAckTime
                );
            return true;
        }

    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the last ACK response (if supported)
    *** @return The last ACK response
    **/
    public String getLastAckResponse()
    {
        String v = (String)this.getFieldValue(FLD_lastAckResponse);
        return StringTools.trim(v);
    }

    /** 
    *** Sets the last ACK response
    *** @param v The last ACK response
    **/
    public void setLastAckResponse(String v)
    {
        this.setFieldValue(FLD_lastAckResponse, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the last ACK time (if supported)
    *** @return The last ACK time
    **/
    public long getLastAckTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastAckTime);
        return (v != null)? v.longValue() : 0L;
    }

    /** 
    *** Sets the last ACK time (if supported)
    *** @param v The last ACK time
    **/
    public void _setLastAckTime(long v)
    {
        this.setFieldValue(FLD_lastAckTime, v);
        this.addOtherChangedFieldNames(FLD_lastAckTime);
    }

    /** 
    *** Sets the last ACK time (if supported)
    *** @param v The last ACK time
    **/
    public void setLastAckTime(long v)
    {
        this._setLastAckTime(v);
        if (this.transport != null) {
            this.transport._setLastAckTime(v);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the DCS configuration mask.
    *** Usage defined by specific DCS. For example, the Enfora DCS uses this to
    *** set a default event field data-mask.
    *** @return The DCS configuration mask
    **/
    public long getDcsConfigMask()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_dcsConfigMask);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the DCS configuration mask (usage defined by specific DCS)
    *** @param v The DCS configuration mask
    **/
    public void setDcsConfigMask(long v)
    {
        this.setOptionalFieldValue(FLD_dcsConfigMask, v);
    }

    // --------------------------------

    private RTProperties dcsConfigProps = null;

    /**
    *** Returns true if the DCS configuration String is defined.
    *** @return True if the DCS configuration String is defined.
    **/
    public boolean hasDcsConfigString()
    {
        return !StringTools.isBlank(this.getDcsConfigString())? true : false;
    }

    /**
    *** Gets the DCS configuration String.
    *** Usage defined by specific DCS. For example, the Xirgo DCS uses this to get 
    *** the default firmware version.
    *** @return The DCS configuration String
    **/
    public String getDcsConfigString()
    {
        String v = (String)this.getOptionalFieldValue(FLD_dcsConfigString);
        return StringTools.trim(v);
    }

    /**
    *** Sets the DCS configuration String (usage defined by specific DCS)
    *** @param v The DCS configuration String
    **/
    public void setDcsConfigString(String v)
    {
        this.setOptionalFieldValue(FLD_dcsConfigString, StringTools.trim(v));
        this.dcsConfigProps = null;
    }

    /**
    *** Gets the DCS configuration as an RTProperties instance
    **/
    public RTProperties getDcsConfigProperties()
    {
        if (this.dcsConfigProps == null) {
            String dcsPropStr = this.getDcsConfigString();
            this.dcsConfigProps = new RTProperties(dcsPropStr);
        }
        return this.dcsConfigProps;
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Returns true if this device supports the OpenDMTP protocol
    *** @return True if this device supports the OpenDMTP protocol
    **/
    public boolean getSupportsDMTP()
    {
        Boolean v = (Boolean)this.getFieldValue(FLD_supportsDMTP);
        return (v != null)? v.booleanValue() : true;
    }

    /**
    *** OpenDMTP: Returns true if this device supports the OpenDMTP protocol
    *** @return True if this device supports the OpenDMTP protocol
    **/
    public boolean supportsDMTP()
    {
        return this.getSupportsDMTP();
    }

    /**
    *** OpenDMTP: Sets the OpenDMTP protocol support state
    *** @param v The OpenDMTP protocol support state
    **/
    public void setSupportsDMTP(boolean v)
    {
        this.setFieldValue(FLD_supportsDMTP, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the supported OpenDMTP encodings
    *** @return The supported OpenDMTP encodings
    **/
    public int getSupportedEncodings()
    {
        Integer v = (Integer)this.getFieldValue(FLD_supportedEncodings);
        return (v != null)? v.intValue() : (int)Encodings.BINARY.getLongValue();
    }

    /**
    *** OpenDMTP: Sets the supported OpenDMTP encodings
    *** @param v The supported OpenDMTP encodings
    **/
    public void setSupportedEncodings(int v)
    {
        v &= (int)EnumTools.getValueMask(Encodings.class);
        if (v == 0) { v = (int)Encodings.BINARY.getLongValue(); }
        this.setFieldValue(FLD_supportedEncodings, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the OpenDMTP unit limit interval
    *** @return The OpenDMTP unit limit interval
    **/
    public int getUnitLimitInterval() // Minutes
    {
        Integer v = (Integer)this.getFieldValue(FLD_unitLimitInterval);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** OpenDMTP: Sets the OpenDMTP unit limit interval
    *** @param v The OpenDMTP unit limit interval
    **/
    public void setUnitLimitInterval(int v) // Minutes
    {
        this.setFieldValue(FLD_unitLimitInterval, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the maximum allowed OpenDMTP events
    *** @return The maximum allowed OpenDMTP events
    **/
    public int getMaxAllowedEvents()
    {
        Integer v = (Integer)this.getFieldValue(FLD_maxAllowedEvents);
        return (v != null)? v.intValue() : 1;
    }

    /**
    *** OpenDMTP: Sets the maximum allowed OpenDMTP events
    *** @param v The maximum allowed OpenDMTP events
    **/
    public void setMaxAllowedEvents(int v)
    {
        this.setFieldValue(FLD_maxAllowedEvents, ((v >= 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the total (UDP/TCP) connection profile mask
    *** @return The total (UDP/TCP) connection profile mask
    **/
    public DTProfileMask getTotalProfileMask()
    {
        DTProfileMask v = (DTProfileMask)this.getFieldValue(FLD_totalProfileMask);
        return v;
    }

    /**
    *** OpenDMTP: Sets the total (UDP/TCP) connection profile mask
    *** @param v The total (UDP/TCP) connection profile mask
    **/
    public void setTotalProfileMask(DTProfileMask v)
    {
        this.setFieldValue(FLD_totalProfileMask, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the maximum total connections allowed per interval<br>
    *** Note: The effective maximum value for this field is defined by the following:<br>
    *** (org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK * this.getUnitLimitIntervalMinutes())
    *** @return The maximum total connections allowed per interval
    **/
    public int getTotalMaxConn()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalMaxConn);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** OpenDMTP: Sets the maximum total connections allowed per interval
    *** @param v The maximum total connections allowed per interval
    **/
    public void setTotalMaxConn(int v)
    {
        this.setFieldValue(FLD_totalMaxConn, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the maximum total connections allowed per minute<br>
    *** Note: The effective maximum value for this field is defined by the constant:<br>
    *** "org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK"
    *** @return The maximum total connections allowed per minute
    **/
    public int getTotalMaxConnPerMin()
    {
        Integer v = (Integer)this.getFieldValue(FLD_totalMaxConnPerMin);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** OpenDMTP: Sets the maximum total connections allowed per minute<br>
    *** @param v The maximum total connections allowed per minute
    **/
    public void setTotalMaxConnPerMin(int v)
    {
        this.setFieldValue(FLD_totalMaxConnPerMin, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the Duplex/TCP connection profile mask
    *** @return The Duplex/TCP connection profile mask
    **/
    public DTProfileMask getDuplexProfileMask()
    {
        DTProfileMask v = (DTProfileMask)this.getFieldValue(FLD_duplexProfileMask);
        return v;
    }

    /**
    *** OpenDMTP: Sets the Duplex/TCP connection profile mask
    *** @param v The Duplex/TCP connection profile mask
    **/
    public void setDuplexProfileMask(DTProfileMask v)
    {
        this.setFieldValue(FLD_duplexProfileMask, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the maximum Duplex/TCP connections per Interval
    *** Note: The effective maximum value for this field is defined by the following:
    *** (org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK * this.getUnitLimitIntervalMinutes())
    *** @return The maximum Duplex/TCP connections per Interval
    **/
    public int getDuplexMaxConn()
    {
        Integer v = (Integer)this.getFieldValue(FLD_duplexMaxConn);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** OpenDMTP: Sets the maximum Duplex/TCP connections per Interval
    *** @param v The maximum Duplex/TCP connections per Interval
    **/
    public void setDuplexMaxConn(int v)
    {
        this.setFieldValue(FLD_duplexMaxConn, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** OpenDMTP: Gets the maximum Duplex/TCP connections per Minute
    *** Note: The effective maximum value for this field is defined by the constant:
    *** "org.opendmtp.server.base.ValidateConnections.BITS_PER_MINUTE_MASK"
    *** @return The maximum Duplex/TCP connections per Minute
    **/
    public int getDuplexMaxConnPerMin()
    {
        Integer v = (Integer)this.getFieldValue(FLD_duplexMaxConnPerMin);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** OpenDMTP: Sets the maximum Duplex/TCP connections per Minute
    *** @param v The maximum Duplex/TCP connections per Minute
    **/
    public void setDuplexMaxConnPerMin(int v)
    {
        this.setFieldValue(FLD_duplexMaxConnPerMin, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last Duplex/TCP connection time
    *** @return The last Duplex/TCP connection time
    **/
    public long getLastDuplexConnectTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastDuplexConnectTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last Duplex/TCP connection time
    *** @param v The last Duplex/TCP connection time
    **/
    public void _setLastDuplexConnectTime(long v)
    {
        this.setFieldValue(FLD_lastDuplexConnectTime, v);
    }

    /**
    *** Sets the last Duplex/TCP connection time
    *** @param v The last Duplex/TCP connection time
    **/
    public void setLastDuplexConnectTime(long v)
    {
        this._setLastDuplexConnectTime(v);
        if (this.transport != null) {
            this.transport._setLastDuplexConnectTime(v);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last UDP/TCP connection time
    *** @return The last UDP/TCP connection time
    **/
    public long getLastTotalConnectTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastTotalConnectTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last UDP/TCP connection time
    *** @param v The last UDP/TCP connection time
    **/
    public void _setLastTotalConnectTime(long v)
    {
        this.setFieldValue(FLD_lastTotalConnectTime, v);
    }

    /**
    *** Sets the last UDP/TCP connection time
    *** @param v The last UDP/TCP connection time
    **/
    public void setLastTotalConnectTime(long v)
    {
        this._setLastTotalConnectTime(v);
        if (this.transport != null) {
            this.transport._setLastTotalConnectTime(v);
        }
    }

    /**
    *** Gets the last UDP/TCP connection time
    *** @return The last UDP/TCP connection time
    **/
    public long getLastConnectTime()
    {
        return this.getLastTotalConnectTime();
    }

    /**
    *** Sets the last UDP/TCP connection time
    *** @param v The last UDP/TCP connection time
    **/
    public void setLastConnectTime(long v, boolean isDuplex)
    {
        this.setLastTotalConnectTime(v);
        if (isDuplex) {
            this.setLastDuplexConnectTime(v);
        } else {
            // Simplex?
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if Fixed Locations are supported
    *** @return True if Fixed Locations are supported
    **/
    public static boolean supportsFixedLocation()
    {
        return Device.getFactory().hasField(FLD_fixedLatitude);
    }
    
    /**
    *** Gets the fixed latitude for this device
    *** @return The fixed latitude for this device
    **/
    public double getFixedLatitude()
    {
        return this.getOptionalFieldValue(FLD_fixedLatitude, 0.0);
    }

    /**
    *** Sets the fixed latitude for this device
    *** @param v The fixed latitude for this device
    **/
    public void setFixedLatitude(double v)
    {
        this.setOptionalFieldValue(FLD_fixedLatitude, v);
    }

    /**
    *** Gets the fixed longitude for this device
    *** @return The fixed longitude for this device
    **/
    public double getFixedLongitude()
    {
        return this.getOptionalFieldValue(FLD_fixedLongitude, 0.0);
    }

    /**
    *** Sets the fixed longitude for this device
    *** @param v The fixed longitude for this device
    **/
    public void setFixedLongitude(double v)
    {
        this.setOptionalFieldValue(FLD_fixedLongitude, v);
    }

    /**
    *** Returns true if this device supports fixed locations
    *** @return True if this device supports fixed locations
    **/
    public boolean hasFixedLocation()
    {
        // we assume FLD_fixedLongitude exists if FLD_fixedLatitude exists
        return this.hasField(FLD_fixedLatitude); // && this.isValidFixedLocation();
    }

    /**
    *** Returns true if this device defines a valid fixed location
    *** @return True if this device defines a valid fixed location
    **/
    public boolean isValidFixedLocation()
    {
        return GeoPoint.isValid(this.getFixedLatitude(), this.getFixedLongitude());
    }

    /**
    *** Gets the fixed location for this device
    *** @return The fixed location for this device
    **/
    public GeoPoint getFixedLocation()
    {
        return new GeoPoint(this.getFixedLatitude(), this.getFixedLongitude());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the address for the fixed location for this device
    *** @return The address for the fixed location for this device
    **/
    public String getFixedAddress()
    {
        String v = StringTools.trim((String)this.getFieldValue(FLD_fixedAddress));
        return v;
    }

    /**
    *** Sets the address for the fixed location for this device
    *** @param v The address for the fixed location for this device
    **/
    public void setFixedAddress(String v)
    {
        this.setFieldValue(FLD_fixedAddress, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the phone number for the fixed location for this device
    *** @return The phone number for the fixed location for this device
    **/
    public String getFixedContactPhone()
    {
        String v = StringTools.trim((String)this.getOptionalFieldValue(FLD_fixedContactPhone));
        return v;
    }

    /**
    *** Sets the phone number for the fixed location for this device
    *** @param v The phone number for the fixed location for this device
    **/
    public void setFixedContactPhone(String v)
    {
        this.setOptionalFieldValue(FLD_fixedContactPhone, StringTools.trim(v));
    }

    /**
    *** Gets the phone number for the fixed location for this device
    *** @param digitsOnly  If true, the non-digit characters will be stripped from the returned value
    *** @return The phone number for the fixed location for this device
    **/
    public String getFixedContactPhone(boolean digitsOnly)
    {
        String ph = this.getFixedContactPhone();
        return digitsOnly? StringTools.stripNonDigitChars(ph) : ph;
    }

    /**
    *** Returns true if the fixed contact phone# is defined
    *** @return True if the fixed contact phone# is defined
    **/
    public boolean hasFixedContactPhone()
    {
        return !StringTools.isBlank(this.getFixedContactPhone());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last time this fixed location was serviced
    *** @return The last time this fixed location was serviced
    **/
    public long getFixedServiceTime()
    {
        Long v = (Long)this.getOptionalFieldValue(FLD_fixedServiceTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last time this fixed location was serviced
    *** @param v The last time this fixed location was serviced
    **/
    public void setFixedServiceTime(long v)
    {
        this.setOptionalFieldValue(FLD_fixedServiceTime, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if active corridors are supported
    *** @return True if active corridors are supported
    **/
    public static boolean supportsActiveCorridor()
    {
        return Device.getFactory().hasField(FLD_activeCorridor);
    }

    /**
    *** Gets the active corridor for this device
    *** @return The active corridor for this device
    **/
    public String getActiveCorridor()
    {
        String v = (String)this.getOptionalFieldValue(FLD_activeCorridor);
        return StringTools.trim(v);
    }

    /**
    *** Returns true if this device has an active corridor
    *** @return True if this device has an active corridor
    **/
    public boolean hasActiveCorridor()
    {
        return !StringTools.isBlank(this.getActiveCorridor());
    }

    /**
    *** Sets the active corridor for this device
    *** @param v The active corridor for this device
    **/
    public void setActiveCorridor(String v)
    {
        this.setOptionalFieldValue(FLD_activeCorridor, StringTools.trim(v));
    }

    /**
    *** Gets a String array of all GeoCorridor ID for the specified account<br>
    *** (TODO: move to Account.java)
    *** @param acctId  The Account ID
    *** @return String array of GeoCorridor IDs
    **/
    public static String[] getCorridorIDsForAccount(String acctId)
    {

        /* GeoCorridor Class */
        Class<?> gcClass = null;
        try {
            gcClass = Class.forName(DBConfig.PACKAGE_RULE_TABLES_ + "GeoCorridor");
        } catch (Throwable th) { // ClassNotFoundException
            return null;
        }

        /* Method action */
        MethodAction gcListMeth = null;
        try {
            gcListMeth = new MethodAction(gcClass, "getCorridorIDsForAccount", String.class);
        } catch (Throwable th) { // NoSuchMethodException, ClassNotFoundException
            return null;
        }

        /* get list */
        try {
            return (String[])gcListMeth.invoke(acctId);
        } catch (DBException dbe) {
            Print.logError("DBException: " + dbe);
            return null;
        } catch (Throwable th) {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // -- Distance maintenance field components:
    // -    Description (String: not "\"'\\|", " " replaced with "_")
    // -    Interval (Double)
    // -    Last Distance (Double)
    // -    "maint0=<LastDistance>|<Interval>|<Description>"
    // -    EG: maint0=12345.6|1234|Oil_Change maint1=12300.3|2345|Tire_Change

    /**
    *** Returns true if Periodic Maintenance fields are supported
    *** @return True if Periodic Maintenance fields are supported
    **/
    public static boolean supportsPeriodicMaintenance()
    {
        if (Device.getPeriodicMaintOdometerCount() <= 0) {
            return false; 
        } else {
            return Device.getFactory().hasField(FLD_maintOdometerKM0);
        }
    }

    // ---------------

    private static int MAX_MAINT_ODOM_COUNT = 2;
    private static Vector<Integer> RuleMaintTrigger = null;

    /**
    *** Gets the number of maintenance fields to support
    *** (current maximum value is 2)
    *** @return The number of maintenance fields to support
    **/
    public static int getPeriodicMaintOdometerCount()
    {
        int mc = RTConfig.getInt(DBConfig.PROP_Device_maintenanceOdometerCount, MAX_MAINT_ODOM_COUNT);
        if (mc <= 0) {
            return 0;
        } else
        if (mc >= MAX_MAINT_ODOM_COUNT) {
            return MAX_MAINT_ODOM_COUNT;
        } else {
            return mc;
        }
    }

    // ---------------

    /**
    *** Sets the index of the first triggered device maintenance.<br>
    *** (Used for Rule '$MAINTKM' trigger state caching)
    **/
    public void setMaintTriggeredKM(int ndx)
    {
        if ((ndx >= 0) && (ndx < MAX_MAINT_ODOM_COUNT)) {
            if (RuleMaintTrigger == null) { RuleMaintTrigger = new Vector<Integer>(); }
            RuleMaintTrigger.add(new Integer(ndx));
        }
    }

    /**
    *** Gets the index of the first triggered device maintenance.<br>
    *** Returns '-1' if not defined.
    *** (Used for Rule '$MAINTKM' trigger state caching)
    **/
    public int getMaintTriggeredKM()
    {
        if (!ListTools.isEmpty(RuleMaintTrigger)) {
            return RuleMaintTrigger.get(0).intValue(); 
        } else {
            return -1;
        }
    }

    // ---------------

    /**
    *** Gets the maintenance descrption for the specified index
    *** @param ndx  The index of the maintenance descrption to return
    *** @return The maintenance description
    **/
    public String getMaintDescriptionKM(int ndx)
    {
        switch (ndx) {
            case 0 : return this.getMaintDescriptionKM0();
            case 1 : return this.getMaintDescriptionKM1();
            default: return "";
        }
    }

    /**
    *** Gets the last maintenance odometer for the specified index
    *** @param ndx  The index of the maintenance odometer to return
    *** @return The maintenance odometer (in kilometers)
    **/
    public double getMaintOdometerKM(int ndx)
    {
        switch (ndx) {
            case 0 : return this.getMaintOdometerKM0();
            case 1 : return this.getMaintOdometerKM1();
            default: return 0.0;
        }
    }

    /**
    *** Resets the last maintenance odometer for the specified index
    *** @param ndx  The index of the maintenance odometer to reset
    **/
    public void resetMaintOdometerKM(int ndx)
    {
        switch (ndx) {
            case 0 : this.resetMaintOdometerKM0(); break;
            case 1 : this.resetMaintOdometerKM1(); break;
        }
    }

    /**
    *** Gets the last maintenance interval for the specified index
    *** @param ndx  The index of the maintenance interval to return
    *** @return The maintenance interval (in kilometers)
    **/
    public double getMaintIntervalKM(int ndx, double dft)
    {
        switch (ndx) {
            case 0 : return this.getMaintIntervalKM0();
            case 1 : return this.getMaintIntervalKM1();
            default: return dft;
        }
    }

    /**
    *** Gets the last maintenance interval for the specified index
    *** @param ndx  The index of the maintenance interval to return
    *** @return The maintenance interval (in kilometers)
    **/
    public double getMaintIntervalKM(int ndx)
    {
        return this.getMaintIntervalKM(ndx, 0.0);
    }

    /**
    *** Returns true if the maintenance interval is due for the specified index and
    *** specified number of delta kilometers.
    *** @param ndx  The index of the maintenance interval
    *** @param deltaKM  The delta-kilometers to check
    *** @return True if maintenance is due
    **/
    public boolean isMaintenanceDueKM(int ndx, double deltaKM)
    {
        if (Device.supportsPeriodicMaintenance()) {
            double odomKM = this.getLastOdometerKM();
            if (odomKM > 0.0) {
                double lastKM = this.getMaintOdometerKM(ndx);
                double intvKM = this.getMaintIntervalKM(ndx);
                if ((odomKM + deltaKM) >= (lastKM + intvKM)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
    *** Gets the number of kilometers until the next maintenance interval is due.
    *** Returns zero if past due, or if periodic maintenance is not supported.
    **/
    public double getMaintenanceRemainingKM(int ndx)
    {
        if (Device.supportsPeriodicMaintenance()) {
            double odomKM = this.getLastOdometerKM();
            if (odomKM > 0.0) {
                double lastKM = this.getMaintOdometerKM(ndx);
                double intvKM = this.getMaintIntervalKM(ndx);
                double nextKM = lastKM + intvKM;
                if (odomKM > nextKM) {
                    // -- maintenance is past due
                    return 0.0;
                } else {
                    return nextKM - odomKM;
                }
            }
            // -- odometer not available
            return -1.0;
        } else {
            // -- periodic maintenance not supported
            return -1.0;
        }
    }

    // ---------------

    /**
    *** Gets the maintenance descrption for index #0
    *** @return The maintenance descrption #0
    **/
    public String getMaintDescriptionKM0()
    {
        // return this.getOptionalFieldValue(FLD_maintDescriptionKM0, 0.0);
        return RTConfig.getString(DBConfig.PROP_Device_maintenanceDescriptionKM_0, "#1"); // 1-indexed
    }

    /**
    *** Gets the maintenance interval for index #0
    *** @return The maintenance interval #0, in kilometers
    **/
    public double getMaintIntervalKM0()
    {
        return this.getOptionalFieldValue(FLD_maintIntervalKM0, 0.0);
    }

    /**
    *** Sets the maintenance interval for index #0
    *** @param v The maintenance interval #0, in kilometers
    **/
    public void setMaintIntervalKM0(double v)
    {
        this.setOptionalFieldValue(FLD_maintIntervalKM0, v);
    }

    /**
    *** Gets the maintenance odometer for index #0
    *** @return The maintenance odometer #0, in kilometers
    **/
    public double getMaintOdometerKM0()
    {
        return this.getOptionalFieldValue(FLD_maintOdometerKM0, 0.0);
    }

    /**
    *** Sets the maintenance odometer for index #0
    *** @param v The maintenance odometer #0, in kilometers
    **/
    public void setMaintOdometerKM0(double v)
    {
        if (v < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_maintOdometerKM0, ((v >= 0.0)? v : 0.0));
        }
    }

    /**
    *** Resets the maintenance odometer for index #0
    **/
    public void resetMaintOdometerKM0()
    {
        this.setMaintOdometerKM0(this.getLastOdometerKM());
        this.addOtherChangedFieldNames(Device.FLD_maintOdometerKM0);
    }

    // ---------------

    /**
    *** Gets the maintenance descrption for index #1
    *** @return The maintenance descrption #1
    **/
    public String getMaintDescriptionKM1()
    {
        // return this.getOptionalFieldValue(FLD_maintDescriptionKM1, 0.0);
        return RTConfig.getString(DBConfig.PROP_Device_maintenanceDescriptionKM_1, "#2"); // 1-indexed
    }

    /**
    *** Gets the maintenance interval for index #1
    *** @return The maintenance interval #1, in kilometers
    **/
    public double getMaintIntervalKM1()
    {
        return this.getOptionalFieldValue(FLD_maintIntervalKM1, 0.0);
    }

    /**
    *** Sets the maintenance interval for index #1
    *** @param v The maintenance interval #1, in kilometers
    **/
    public void setMaintIntervalKM1(double v)
    {
        this.setOptionalFieldValue(FLD_maintIntervalKM1, v);
    }

    /**
    *** Gets the maintenance odometer for index #1
    *** @return The maintenance odometer #1, in kilometers
    **/
    public double getMaintOdometerKM1()
    {
        return this.getOptionalFieldValue(FLD_maintOdometerKM1, 0.0);
    }

    /**
    *** Sets the maintenance odometer for index #1
    *** @param v The maintenance odometer #1, in kilometers
    **/
    public void setMaintOdometerKM1(double v)
    {
        if (v < this.getMaxOdometerKM()) {
            this.setOptionalFieldValue(FLD_maintOdometerKM1, ((v >= 0.0)? v : 0.0));
        }
    }

    /**
    *** Resets the maintenance odometer for index #1
    **/
    public void resetMaintOdometerKM1()
    {
        this.setMaintOdometerKM1(this.getLastOdometerKM());
        this.addOtherChangedFieldNames(Device.FLD_maintOdometerKM0);
    }

    // ---------------

    /**
    *** Gets the number of supported maintenance engine-hour fields
    *** @return The number of supported maintenance engine-hour fields
    **/
    public static int getPeriodicMaintEngHoursCount()
    {
        return 1;
    }

    /**
    *** Gets the Maintenance Engine Hours for the specified index
    *** @param ndx  The maintenance engine-hours index
    *** @return The maintenance engine hours
    **/
    public double getMaintEngHoursHR(int ndx)
    {
        switch (ndx) {
            case 0 : return this.getMaintEngHoursHR0();
            default: return 0.0;
        }
    }

    /** 
    *** Resets the maintenance engine hours for the specified index
    *** @param ndx  The maintenance engine-hours index
    **/
    public void resetMaintEngHoursHR(int ndx)
    {
        switch (ndx) {
            case 0 : this.resetMaintEngHoursHR0(); break;
        }
    }

    /**
    *** Gets the maintenance engine-hours interval for the specified index
    *** @param ndx  The maintenance engine-hours index
    *** @return The maintenance engine-hours interval
    **/
    public double getMaintIntervalHR(int ndx, double dft)
    {
        switch (ndx) {
            case 0 : return this.getMaintIntervalHR0();
            default: return dft;
        }
    }

    /**
    *** Gets the maintenance engine-hours interval for the specified index
    *** @param ndx  The maintenance engine-hours index
    *** @return The maintenance engine-hours interval
    **/
    public double getMaintIntervalHR(int ndx)
    {
        return this.getMaintIntervalHR(ndx, 0.0);
    }

    /**
    *** Returns true if the maintenance engine hours for the specified index is due
    *** @param ndx  The maintenance engine-hours index
    *** @param deltaHR  The delta engine-hours to check
    **/
    public boolean isMaintenanceDueHR(int ndx, double deltaHR)
    {
        if (Device.supportsPeriodicMaintenance()) {
            double engHrs = this.getLastEngineHours();
            if (engHrs > 0.0) {
                double lastHR = this.getMaintEngHoursHR(ndx);
                double intvHR = this.getMaintIntervalHR(ndx);
                if ((engHrs + deltaHR) >= (lastHR + intvHR)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
    *** Gets the number of engine hours until the next maintenance interval is due.
    *** Returns zero if past due, or if periodic maintenance is not supported.
    **/
    public double getMaintenanceRemainingHR(int ndx)
    {
        if (Device.supportsPeriodicMaintenance()) {
            double engHrs = this.getLastEngineHours();
            if (engHrs > 0.0) {
                double lastHR = this.getMaintEngHoursHR(ndx);
                double intvHR = this.getMaintIntervalHR(ndx);
                double nextHR = lastHR + intvHR;
                if (engHrs > nextHR) {
                    // -- maintenance is past due
                    return 0.0;
                } else {
                    return nextHR - engHrs;
                }
            }
            // -- engine-hours not available
            return -1.0;
        } else {
            // -- periodic maintenance not supported
            return -1.0;
        }
    }

    // ---------------

    /** 
    *** Gets the maintenance engine-hours interval for index #0
    *** @return The maintenance engine-hours interval for index #0
    **/
    public double getMaintIntervalHR0()
    {
        return this.getOptionalFieldValue(FLD_maintIntervalHR0, 0.0);
    }

    /** 
    *** Sets the maintenance engine-hours interval for index #0
    *** @param v The maintenance engine-hours interval for index #0
    **/
    public void setMaintIntervalHR0(double v)
    {
        this.setOptionalFieldValue(FLD_maintIntervalHR0, v);
    }

    /** 
    *** Gets the maintenance engine-hours elapsed for index #0
    *** @return The maintenance engine-hours elapsed for index #0
    **/
    public double getMaintEngHoursHR0()
    {
        return this.getOptionalFieldValue(FLD_maintEngHoursHR0, 0.0);
    }

    /** 
    *** Sets the maintenance engine-hours elapsed for index #0
    *** @param v The maintenance engine-hours elapsed for index #0
    **/
    public void setMaintEngHoursHR0(double v)
    {
        if (v < this.getMaxRuntimeHours()) {
            this.setOptionalFieldValue(FLD_maintEngHoursHR0, ((v >= 0.0)? v : 0.0));
        }
    }

    /**
    *** Resets the maintenance engine-hours for index #0
    **/
    public void resetMaintEngHoursHR0()
    {
        this.setMaintEngHoursHR0(this.getLastEngineHours());
        this.addOtherChangedFieldNames(Device.FLD_maintEngHoursHR0);
    }

    // ---------------

    /**
    *** Gets the maintenance notes
    *** @return the maintenance notes
    **/
    public String getMaintNotes()
    {
        String v = (String)this.getOptionalFieldValue(FLD_maintNotes);
        return StringTools.trim(v);
    }

    /**
    *** Sets the maintenance notes
    *** @param v the maintenance notes
    **/
    public void setMaintNotes(String v)
    {
        this.setOptionalFieldValue(FLD_maintNotes, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------
    // "Reminder" support (may require Event Notification Rules Engine)
    // With Cron and ENRE installed, configure as follows:
    // 1) Enable (in "config.conf") optional Device MaintOdometerFieldInfo fields
    //      startupInit.Device.MaintOdometerFieldInfo=true
    // 2) Enable (in "config.conf") the "Reminder" fields on Device and Rule Admin:
    //      Domain.Properties.deviceInfo.showReminderMessage=true
    //      Domain.Properties.ruleInfo.showPredefinedActions=true
    // 3) Rebuild/Redeploy after making above config changes.  Then update the table
    //    columns to make sure that the Device table maintenance fields are created.
    // 4) Make sure "Cron" service is running
    // 5) Create a rule with following attributes:
    //      Selector          : $REMINDER           [Rule.selector]
    //      Is Cron Rule      : Hourly              [Rule.isCronRule, Rule.ruleTag]
    //      Trigger Action    : EMail               [Rule.actionMask]
    //      Prefedined Actions: resetReminder       [Rule.cannedActions]
    //      Email subj/body   : ${reminderMessage}
    //    The above rule will check the "$REMINDER" every hour.  When "$REMINDER"
    //    returns 'true', the email subj/body message will be sent.  The "resetReminder"
    //    predefined action will then reset the reminder for the next interval. 
    //    (Note: without the "resetReminder" predefined-action, the reminder notification
    //    will continue to be sent every hour)
    // 6) Set "Reminder Interval" and "Reminder Message" values on the Device Admin
    //    Acceptable values:
    //      Date specification - "date:2012/12/25"
    //      Month Abbreviation - "jan"
    //      Day Abbreviation   - "tue"
    //      Periodic Interval  - "12345"  (in seconds)

    /**
    *** Gets the maintenance reminder type (currently always returns '0')
    *** @return the maintenance reminder type
    **/
    public int getReminderType()
    {
        // FLD_reminderType column is not currently part of the Device record
        Integer v = (Integer)this.getOptionalFieldValue(FLD_reminderType);
        return (v != null)? v.intValue() : ReminderType.PERIODIC_INTERVAL.getIntValue();
    }

    /**
    *** Sets the maintenance reminder type
    *** @param v the maintenance reminder type
    **/
    public void setReminderType(int v)
    {
        this.setOptionalFieldValue(FLD_reminderType, v);
    }

    /**
    *** Sets the maintenance reminder type
    *** @param r the maintenance reminder type
    **/
    public void setReminderType(ReminderType r)
    {
        int v = (r != null)? r.getIntValue() : ReminderType.PERIODIC_INTERVAL.getIntValue();
        this.setReminderType(v);
    }

    // ---------------

    /**
    *** Gets the maintenance reminder message
    *** @return the maintenance reminder message
    **/
    public String getReminderMessage()
    {
        String v = (String)this.getOptionalFieldValue(FLD_reminderMessage);
        return StringTools.trim(v);
    }

    /**
    *** Sets the maintenance reminder message
    *** @param v the maintenance reminder message
    **/
    public void setReminderMessage(String v)
    {
        this.setOptionalFieldValue(FLD_reminderMessage, StringTools.trim(v));
    }

    // ---------------

    /**
    *** Gets the maintenance reminder time
    *** @return the maintenance reminder time
    **/
    public long getReminderTime()
    {
        Long v = (Long)this.getFieldValue(FLD_reminderTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the maintenance reminder time
    *** @param v the maintenance reminder time
    **/
    public void setReminderTime(long v)
    {
        this.setFieldValue(FLD_reminderTime, v);
    }

    // ---------------

    /**
    *** Gets the maintenance reminder interval
    *** @return the maintenance reminder interval
    **/
    public String getReminderInterval()
    {
        // Valid values:
        //  <#ElapsedInterval>,date:<ArgDate>,<MonthAbbrev>,<DayAbbrev>
        String v = (String)this.getFieldValue(FLD_reminderInterval);
        return StringTools.trim(v);
    }

    /**
    *** Sets the maintenance reminder interval
    *** @param v the maintenance reminder interval
    **/
    public void setReminderInterval(String v)
    {
        this.setFieldValue(FLD_reminderInterval, StringTools.trim(v).toLowerCase());
    }

    // ---------------

    /**
    *** Returns true if the maintenance reminder has expired
    *** @param tz The TimeZone
    *** @param nowTime  The current time
    *** @return True if expired
    **/
    public boolean isReminderExpired(TimeZone tz, long nowTime)
    {
        int    remType   = this.getReminderType();      // 0=periodic, 1=single
        long   remTime   = this.getReminderTime();      // last reminder time
        String remIntStr = this.getReminderInterval();  // <#ElapsIntrv>,date:<ArgDate>,<MonthAbbr>,<DayAbbr>
        return Device._isReminderExpired(
            remType, remTime, remIntStr, 
            tz, nowTime);
    }

    private static boolean REMINDER_LOG = RTConfig.getBoolean("device.debugReminder",false);
    /**
    *** Returns true if the maintenance reminder has expired
    *** @param remType   The reminder type (currently unused)
    *** @param remTime   The reminder time
    *** @param remIntStr The reminder interval string
    *** @param tz        The TimeZone
    *** @param nowTime   The current time
    *** @return True if expired
    **/
    private static boolean _isReminderExpired(
        int remType, long remTime, String remIntStr,
        TimeZone tz, long nowTime)
    {

        /* invalid time specified? */
        if (nowTime <= 0L) {
            if (REMINDER_LOG) { Print.logInfo("Invalid Reminder Now Time"); }
            return false;
        }

        /* reminder time/interval */
        if (StringTools.isBlank(remIntStr)) {
            // -- no interval specified
            if (REMINDER_LOG) { Print.logInfo("No Reminder Interval Specified"); }
            return false;
        }

        /* extract reminder date components */
        DateTime remDT   = new DateTime(remTime, tz);
        int      remDOW  = remDT.getDayOfWeek();
        int      remMon0 = remDT.getMonth0();
        int      remYear = remDT.getYear();

        /* extract now date components */
        DateTime nowDT   = new DateTime(nowTime, tz);
        int      nowDOW  = nowDT.getDayOfWeek();
        int      nowMon0 = nowDT.getMonth0();
        int      nowYear = nowDT.getYear();

        /* loop through intervals */
        // -- "12345,mon,tue,jan,feb,date:2012/05/24"
        String remInt[] = StringTools.split(remIntStr,',');
        for (int i = 0; i < remInt.length; i++) {
            String rem = remInt[i].toLowerCase();

            /* skip blank entries */
            if (StringTools.isBlank(rem)) { 
                // -- skip this entry
                if (REMINDER_LOG) { Print.logInfo("Skipping blank Reminder Entry: " + i); }
                continue;
            }

            /* seconds elapsed */
            if (StringTools.isLong(rem,true/*strict*/)) {
                // -- "12345" - (absolute) standard interval
                long interval = StringTools.parseLong(rem,0L);
                if (interval > 0L) {
                    long nexTime = remTime + interval;
                    if (nexTime <= nowTime) {
                        if (REMINDER_LOG) { Print.logInfo("Interval Reminder Expired: " + rem); }
                        return true;
                    } else {
                        //if (REMINDER_LOG) { Print.logInfo("Interval Reminder not expired: " + rem); }
                    }
                } else {
                    // -- invalid interval specification
                    if (REMINDER_LOG) { Print.logWarn("Invalid Reminder Interval: " + rem); }
                }
                continue;
            }

            /* specific date */
            // -- "date:2012/12/25", "2012/12/25|12:12:34"
            if (rem.startsWith("date:") || (rem.indexOf("/") > 0)) {
                String dateStr = rem.startsWith("date:")? rem.substring("date:".length()) : rem;
                try {
                    DateTime dateDT = DateTime.parseArgumentDate(dateStr, tz);
                    long dateTime = dateDT.getTimeSec();
                    if (dateTime <= remTime) {
                        if (REMINDER_LOG) { Print.logInfo("Date Reminder Already Expired: " + rem); }
                    } else
                    if (dateTime <= nowTime) {
                        if (REMINDER_LOG) { Print.logInfo("Date Reminder Expired: " + rem); }
                        return true;
                    } else {
                        //if (REMINDER_LOG) { Print.logInfo("Date Reminder not expired: " + rem); }
                    }
                } catch (DateTime.DateParseException dpe) {
                    // -- invalid date format
                    if (REMINDER_LOG) { Print.logWarn("Invalid Reminder Date: " + rem); }
                }
                continue;
            } 

            /* day of week */
            // -- "mon", "tue" - day of week abbreviation
            int dowNdx = DateTime.getDayIndex(rem, -1);
            if (dowNdx >= 0) {
                // -- 0..6 - (absolute) day of week
                if (dowNdx == nowDOW) {
                    // -- current date matches specified DOW
                    int deltaDays = dowNdx - remDOW;
                    if (deltaDays <= 0) { deltaDays += 7; } // "0" becomes "7" (for the following week)
                    long nextTime = remTime + DateTime.DaySeconds(deltaDays);
                    DateTime nextDT = new DateTime(nextTime, tz);
                    if (nextDT.getDayStart() <= nowTime) {
                        if (REMINDER_LOG) { Print.logInfo("DOW Reminder Expired: " + rem); }
                        return true;
                    }
                }
                continue;
            }

            /* month */
            // -- "jan", "feb" - month abbreviation
            int monNdx0 = DateTime.getMonthIndex0(rem, -1);
            if (monNdx0 >= 0) {
                // 0..11 - (absolute) start of month
                if (monNdx0 == nowMon0) {
                    // current date matches specified month
                    if ((remYear == nowYear) && (remMon0 == nowMon0)) {
                        // skip (already expired for this month)
                        if (REMINDER_LOG) { Print.logInfo("Reminder Already Expired: " + rem); }
                    } else {
                        if (REMINDER_LOG) { Print.logInfo("Month Reminder Expired: " + rem); }
                        return true;
                    }
                }
                continue;
            }

            /* unrecognized reminder type */
            if (REMINDER_LOG) { Print.logInfo("Ignoring unrecognized reminder format: " + remIntStr); }

        }

        /* not expired */
        if (REMINDER_LOG) { Print.logInfo("Reminder not expired"); }
        return false;

    }

    /**
    *** Returns true if the reminder time has expired
    *** @param tz  The TimeZone
    *** @return True if expired
    **/
    public boolean isReminderExpired(TimeZone tz)
    {
        return this.isReminderExpired(tz, DateTime.getCurrentTimeSec());
    }

    // ---------------

    /**
    *** Reset the reminder time
    *** @param currentTime  The time to which the reminder is reset
    **/
    public void resetReminder(long currentTime)
    {
        this.setReminderTime(currentTime);
        this.addOtherChangedFieldNames(Device.FLD_reminderTime);
    }

    /**
    *** Reset the reminder time (to current time)
    **/
    public void resetReminder()
    {
        this.resetReminder(DateTime.getCurrentTimeSec());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last service time
    *** @return The last service time
    **/
    public long getLastServiceTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastServiceTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last service time
    *** @param v The last service time
    **/
    public void setLastServiceTime(long v)
    {
        this.setFieldValue(FLD_lastServiceTime, ((v >= 0L)? v : 0L));
    }

    // ---------------

    /**
    *** Gets the day of the last service
    *** @return The day of the last service
    **/
    public long getLastServiceDayNumber()
    {
        long ts = this.getLastServiceTime();
        if (ts <= 0L) {
            return 0L;
        } else {
            //TimeZone tmz = this.getTimeZone(null);
            TimeZone tmz = Account.getTimeZone(this.getAccount(),DateTime.getGMTTimeZone()); 
            return (new DateTime(ts,tmz)).getDayNumber();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the next service time
    *** @return The next service time
    **/
    public long getNextServiceTime()
    {
        Long v = (Long)this.getFieldValue(FLD_nextServiceTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the next service time
    *** @param v The next service time
    **/
    public void setNextServiceTime(long v)
    {
        this.setFieldValue(FLD_nextServiceTime, ((v >= 0L)? v : 0L));
    }

    // ---------------

    /**
    *** Gets the day of the next service
    *** @return The day of the next service
    **/
    public long getNextServiceDayNumber()
    {
        long ts = this.getNextServiceTime();
        if (ts <= 0L) {
            return 0L;
        } else {
            //TimeZone tmz = this.getTimeZone(null);
            TimeZone tmz = Account.getTimeZone(this.getAccount(),DateTime.getGMTTimeZone()); 
            return (new DateTime(ts,tmz)).getDayNumber();
        }
    }

    // ------------------------------------------------------------------------
    
    private User assignedUser = null;

    /**
    *** Returns true if assigned-userID is supported
    *** @return True if assigned-userID is supported
    **/
    public static boolean supportsAssignedUserID()
    {
        return Device.getFactory().hasField(FLD_assignedUserID);
    }

    /** 
    *** Gets the assigned User-ID
    *** @return The assigned User-ID
    **/
    public String getAssignedUserID()
    {
        String v = (String)this.getOptionalFieldValue(FLD_assignedUserID);
        return StringTools.trim(v);
    }

    /** 
    *** Sets the assigned User-ID
    *** @param v The assigned User-ID
    **/
    public void setAssignedUserID(String v)
    {
        this.setOptionalFieldValue(FLD_assignedUserID, StringTools.trim(v).toLowerCase());
        this.assignedUser = null;
    }

    /** 
    *** Gets the assigned User
    *** @return The assigned User, or null if no assigned User
    **/
    public User getAssignedUser()
    {
        if (this.assignedUser == null) {
            String userID = this.getAssignedUserID();
            if (!StringTools.isBlank(userID)) {
                try {
                    this.assignedUser = User.getUser(this.getAccount(), userID); // may stil be null
                } catch (DBException dbe) {
                    // ignore
                }
            }
        }
        return this.assignedUser;
    }

    /**
    *** Gets the TimeZone for the assigned user-id.  If there is no assigned user-id,
    *** or the assigned user-id does not have a TimeZone preference, then it returns
    *** the Account preferred timezone. 
    *** Does not return null
    *** @return The assigned user timezone, or the Account timezone.
    **/
    public TimeZone getAssignedUserTimeZone()
    {
        Account acct = this.getAccount();
        if (acct != null) {
            User user = this.getAssignedUser();
            if (user != null) {
                TimeZone tmz = DateTime.getTimeZone(user.getTimeZone(), null);
                if (tmz != null) {
                    return tmz;
                }
            }
            return acct.getTimeZone(null); // non-null
        }
        return DateTime.getGMTTimeZone();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if ThermalProfile is supported
    *** @return True if ThermalProfile is supported
    **/
    public static boolean supportsThermalProfile()
    {
        return Device.getFactory().hasField(FLD_thermalProfile);
    }

    /**
    *** Gets the temperature profile for this device
    *** @return The temperature profile
    **/
    public String getThermalProfile()
    {
        // temp0=130.0/130.0 temp1=130.0/130.0 temp2=130.0/130.0 temp3=130.0/130.0 temp4=130.0/130.0 temp5=130.0/130.0 temp6=130.0/130.0 temp7=130.0/130.0 
        String v = (String)this.getOptionalFieldValue(FLD_thermalProfile);
        return StringTools.trim(v);
    }

    /**
    *** Sets the temperature profile for this device
    *** @param v The temperature profile
    **/
    public void setThermalProfile(String v)
    {
        this.setOptionalFieldValue(FLD_thermalProfile, StringTools.trim(v).toLowerCase());
    }

    // ------------------------------------------------------------------------

    /* default WorkHours/HoursOfOperation */
    public static WorkHours DefaultWorkHours = new WorkHours(RTConfig.getPropertyGetter(),RuleFactory.PROP_rule_workHours_);
    private WorkHours cacheWorkHours = null;
    
    /**
    *** Returns true if HoursOfOperation/WorkHours is supported
    *** @return True if HoursOfOperation/WorkHours is supported
    **/
    public static boolean supportsHoursOfOperation()
    {
        return Device.getFactory().hasField(FLD_hoursOfOperation);
    }

    /**
    *** Returns true if this device has a defined HoursOfOperation (ie. non-blank)
    *** @return True if this device has a defined HoursOfOperation (ie. non-blank)
    **/
    public boolean hasHoursOfOperation()
    {
        return !StringTools.isBlank(this.getHoursOfOperation());
    }

    /**
    *** Gets the HoursOfOperation for this device (as a String)
    *** @return The HoursOfOperation 
    **/
    public String getHoursOfOperation()
    {
        // sun= mon=06:00-18:00 tue=06:00-18:00 wed=06:00-18:00 thu=06:00-18:00 fri=06:00-18:00 sat=
        String v = (String)this.getOptionalFieldValue(FLD_hoursOfOperation);
        return StringTools.trim(v);
    }

    /**
    *** Sets the HoursOfOperation for this device
    *** @param v The HoursOfOperation
    **/
    public void setHoursOfOperation(String v)
    {
        if (!StringTools.isBlank(v)) {
            RTProperties vRTP  = new RTProperties(v.toLowerCase().replace(',',' '));
            WorkHours    wh    = new WorkHours(vRTP,"");
            RTProperties whRTP = wh.getProperties();
            this.setOptionalFieldValue(FLD_hoursOfOperation, whRTP.toString());
        } else {
            this.setOptionalFieldValue(FLD_hoursOfOperation, "");
        }
        this.cacheWorkHours = null;
    }

    /**
    *** Sets the HoursOfOperation for this device
    *** @param rtp  The HoursOfOperation specification (as a <code>RTProperties</code> instance)
    **/
    public void setHoursOfOperation(RTProperties rtp)
    {
        this.setHoursOfOperation((rtp != null)? rtp.toString() : (String)null);
    }

    /**
    *** Sets the HoursOfOperation for this device
    *** @param wh  The HoursOfOperation specification (as a <code>WorkHours</code> instance)
    **/
    public void setHoursOfOperation(WorkHours wh)
    {
        if (wh != null) {
            this.setHoursOfOperation(wh.getProperties());
        } else {
            this.setHoursOfOperation("");
        }
    }

    /**
    *** Gets a WorkHours instance, based on the HoursOfOperation of this Device
    *** @return The WorkHours instance.
    **/
    public WorkHours getWorkHours(WorkHours dft)
    {

        /* already cached? */
        if (this.cacheWorkHours != null) {
            return this.cacheWorkHours;
        }

        /* get/cache WorkHours */
        String whStr = this.getHoursOfOperation();
        if (!StringTools.isBlank(whStr)) {
            // assume it is an RTProperties list
            this.cacheWorkHours = new WorkHours(new RTProperties(whStr),""/*no-prefix*/);
            return this.cacheWorkHours;
        }

        /* not found, return specified default */
        return dft;

    }

    /**
    *** Returns true if the specified time is within the HoursOfOperation relative to the
    *** Account timezone.
    *** @param ts  The timestamp representing the time to check
    *** @return True if time is within HoursOfOperation, false otherwise
    **/
    public boolean isHoursOfOperation(long ts)
    {

        /* invalid time? */
        if (ts < 0L) {
            return false;
        }

        /* DateTime */
        //TimeZone tz = this.getTimeZone(null);
        TimeZone tz = Account.getTimeZone(this.getAccount(),null);
        DateTime dt = new DateTime(ts,tz);

        /* forward to "isHoursOfOperation(DateTime...)" */
        return this.isHoursOfOperation(dt,tz);
        
    }

    /**
    *** Returns true if the specified time is within the HoursOfOperation.
    *** @param dt  The DateTime instance representing the time to check
    *** @param tz  The TimeZone (null to use the Account TimeZone)
    *** @return True if time is within HoursOfOperation, false otherwise
    **/
    public boolean isHoursOfOperation(DateTime dt, TimeZone tz)
    {

        /* invalid time? */
        if (dt == null) {
            return false;
        }

        /* default TimeZone */
        if (tz == null) {
            //TimeZone tz = this.getTimeZone(dt.getTimeZone());
            tz = Account.getTimeZone(this.getAccount(), dt.getTimeZone());
        }

        /* check for time match */
        WorkHours wh = this.getWorkHours(Device.DefaultWorkHours); // not null
        return wh.isMatch(dt,tz);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if PendingMessage is supported
    *** @return True if PendingMessage is supported
    **/
    public static boolean supportsPendingMessage()
    {
        return Device.getFactory().hasField(FLD_pendingMessage);
    }

    /**
    *** Returns true if this device has a pending message
    *** @return True if this device has a pending message
    **/
    public boolean hasPendingMessage()
    {
        if (StringTools.isBlank(this.getPendingMessage())) {
            // -- pending message is blank
            return false;
        } else {
            // -- unack'ed pending message found
            return true;
        }
    }

    /**
    *** Gets the pending message for this device
    *** @return The pending message
    **/
    public String getPendingMessage()
    {
        String v = (String)this.getOptionalFieldValue(FLD_pendingMessage);
        return StringTools.trim(v);
    }

    /**
    *** Sets the pending message for this device
    *** @param v The pending message
    **/
    public void setPendingMessage(String v)
    {
        this.setOptionalFieldValue(FLD_pendingMessage, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the pending message acknowledgement for this device
    *** @return The pending message acknowledgement
    **/
    public String getPendingMessageACK()
    {
        String v = (String)this.getOptionalFieldValue(FLD_pendingMessageACK);
        return StringTools.trim(v);
    }

    /**
    *** Sets the pending message acknowledgement for this device
    *** @param v The pending message acknowledgement
    **/
    public void setPendingMessageACK(String v)
    {
        this.setOptionalFieldValue(FLD_pendingMessageACK, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum number of passengers
    *** @return The maximum number of passengers
    **/
    public int getMaxPassengers()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_maxPassengers);
        return (v != null)? v.intValue() : 0;
    }

    /**
    *** Sets the maximum number of passengers
    *** @param v The maximum number of passengers
    **/
    public void setMaxPassengers(int v)
    {
        this.setOptionalFieldValue(FLD_maxPassengers, ((v >= 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    private RTProperties customAttrRTP = null;
    private Collection<String> customAttrKeys = null;

    /**
    *** Gets the custom attributes for this device
    *** @return The custom attributes for this device
    **/
    public String getCustomAttributes()
    {
        String v = (String)this.getOptionalFieldValue(FLD_customAttributes);
        return StringTools.trim(v);
    }

    /**
    *** Sets the custom attributes for this device
    *** @param v The custom attributes for this device
    **/
    public void setCustomAttributes(String v)
    {
        this.setOptionalFieldValue(FLD_customAttributes, StringTools.trim(v));
        this.customAttrRTP  = null;
        this.customAttrKeys = null;
    }

    /**
    *** Gets the custom attributes for this device as an RTProperties instance
    *** @return The custom RTProperties attributes for this device
    **/
    public RTProperties getCustomAttributesRTP()
    {
        if (this.customAttrRTP == null) {
            this.customAttrRTP = new RTProperties(this.getCustomAttributes());
        }
        return this.customAttrRTP;
    }

    /**
    *** Gets a Collection of custom attribute keys for this device
    *** @return A Collection of custom attribute keys for this device
    **/
    public Collection<String> getCustomAttributeKeys()
    {
        if (this.customAttrKeys == null) {
            this.customAttrKeys = this.getCustomAttributesRTP().getPropertyKeys(null);
        }
        return this.customAttrKeys;
    }

    /**
    *** Gets the value for a specific custom attribute key
    *** @return The value for a specific custom attribute key
    **/
    public String getCustomAttribute(String key)
    {
        return this.getCustomAttributesRTP().getString(key,null);
    }

    /**
    *** Sets a specific custom attribute value
    *** @param key  The custom attribute key
    *** @param value The custom value
    **/
    public String setCustomAttribute(String key, String value)
    {
        return this.getCustomAttributesRTP().getString(key,value);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the vehicle curb weight (in kilograms)
    *** @return The vehicle curb weight (in kilograms)
    **/
    public double getVehicleCurbWeight()
    {
        return this.getOptionalFieldValue(FLD_vehicleCurbWeight, 0.0);
    }

    /**
    *** Sets the vehicle curb weight (in kilograms)
    *** @param v The vehicle curb weight (in kilograms)
    **/
    public void setVehicleCurbWeight(double v)
    {
        this.setOptionalFieldValue(FLD_vehicleCurbWeight, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the vehicle gross weight (in kilograms)
    *** @return The vehicle gross weight (in kilograms)
    **/
    public double getVehicleGrossWeight()
    {
        return this.getOptionalFieldValue(FLD_vehicleGrossWeight, 0.0);
    }

    /**
    *** Sets the vehicle gross weight (in kilograms)
    *** @param v The vehicle gross weight (in kilograms)
    **/
    public void setVehicleGrossWeight(double v)
    {
        this.setOptionalFieldValue(FLD_vehicleGrossWeight, ((v >= 0.0)? v : 0.0));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Work Order ID
    *** @return The Work Order ID (may be a comma separated list)
    **/
    public String getWorkOrderID()
    {
        String v = (String)this.getOptionalFieldValue(FLD_workOrderID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Work Order ID
    *** @param v The Work Order ID (may be a comma separated list)
    **/
    public void setWorkOrderID(String v)
    {
        this.setOptionalFieldValue(FLD_workOrderID, StringTools.trim(v));
    }

    // ----------------------

    /**
    *** Gets the Work Order IDs as an array
    *** @return The array of Work Order IDs
    **/
    public String[] getWorkOrderIDs()
    {
        String woid = this.getWorkOrderID();
        if (StringTools.isBlank(woid)) {
            return new String[0];
        } else {
            return StringTools.split(woid,',');
        }
    }

    /**
    *** Adds the specified Work Order ID to the current list
    *** @param woid The Work Order ID to add
    *** @return True if added
    **/
    public boolean addWorkOrderID(String woid)
    {

        /* blank id specified */
        woid = StringTools.trim(woid);
        if (StringTools.isBlank(woid)) {
            return false; // nothing to add
        }

        /* already exists? */
        if (ListTools.containsIgnoreCase(this.getWorkOrderIDs(),woid)) {
            return false; // already in list
        }

        /* add */
        String woidStr = this.getWorkOrderID();
        if (StringTools.isBlank(woidStr)) {
            this.setWorkOrderID(woid);
        } else {
            String nWL = woidStr + "," + woid;
            this.setWorkOrderID(nWL);
        }
        return true; // not yet saved

    }

    /**
    *** Removes the specified Work Order ID from the current list
    *** @param woid The Work Order ID to remove
    *** @return True if removed
    **/
    public boolean removeWorkOrderID(String woid)
    {

        /* blank id specified */
        if (StringTools.isBlank(woid)) {
            return false; // nothing to remove
        }

        /* get current list */
        String W[] = this.getWorkOrderIDs();
        if (W.length == 0) {
            return false; // already removed
        }

        /* create new list with workOrderID removed */
        java.util.List<String> WL = ListTools.toList(W);
        for (String WID : W) {
            if (!woid.equalsIgnoreCase(WID)) {
                WL.add(WID);
            }
        }

        /* removed? */
        if (WL.size() == W.length) {
            return false; // nothing removed
        } else {
            String woidStr = StringTools.join(WL,",");
            this.setWorkOrderID(woidStr);
            return true;
        }

    }

    /**
    *** Sets the Work Order IDs as an array
    *** @param W The Work Order ID array
    **/
    public void setWorkOrderIDs(String W[])
    {
        if (ListTools.isEmpty(W)) {
            this.setWorkOrderID("");
        } else {
            String woidStr = StringTools.join(W,",");
            this.setWorkOrderID(woidStr);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Job number
    *** @return The Job number
    **/
    public String getJobNumber()
    {
        return this.getFieldValue(FLD_jobNumber, "");
    }

    /**
    *** Sets the Job number
    *** @param v The Job number
    **/
    public void setJobNumber(String v)
    {
        this.setFieldValue(FLD_jobNumber, StringTools.trim(v));
        this.addOtherChangedFieldNames(FLD_jobNumber);
    }

    /**
    *** Returns true if Job number is defined
    *** @return True if Job number is defined
    **/
    public boolean hasJobNumber()
    {
        return !StringTools.isBlank(this.getJobNumber());
    }


    // ------------------------------------------------------------------------

    /**
    *** Sets the Job location
    *** @param jobLoc  The Job Location GeoPoint
    *** @param jobRadM The Job radius in meters
    **/
    public void setJobLocation(GeoPoint jobLoc, double jobRadM)
    {
        if (!GeoPoint.isValid(jobLoc) || (jobRadM <= 0.0)) {
            //Print.logInfo("Clearing job location");
            this.setJobLatitude(0.0);
            this.setJobLongitude(0.0);
            this.setJobRadius(0.0);
        } else {
            //Print.logInfo("Setting job location: " + jobLoc + " " + jobRadM + " m");
            this.setJobLatitude(jobLoc.getLatitude());
            this.setJobLongitude(jobLoc.getLongitude());
            this.setJobRadius(jobRadM);
        }
    }

    /**
    *** Returns true if there is a current job defined
    *** @return True if there is a current job defined
    **/
    public boolean hasCurrentJob()
    {
        if (this.getJobRadius() <= 0.0) {
            return false;
        } else
        if (!GeoPoint.isValid(this.getJobLatitude(),this.getJobLongitude())) {
            return false;
        } else {
            return true;
        }
    }

    /**
    *** Returns true if the specified location would represent a job depart
    *** @param gp  The GeoPoint to test
    *** @return True if the specified location represents a job depart 
    **/
    public boolean isImplicitJobDepart(GeoPoint gp)
    {

        /* no point specified */
        if (!GeoPoint.isValid(gp)) {
            return false; // invalid point, no implicit job depart
        }

        /* get job location */
        double jobLat = this.getJobLatitude();
        double jobLon = this.getJobLongitude();
        double jobRad = this.getJobRadius();
        if (!GeoPoint.isValid(jobLat,jobLon) || (jobRad <= 0.0)) {
            return false; // no job, no job-deprt
        }
        GeoPoint jobLoc = new GeoPoint(jobLat,jobLon);

        /* outside of job zone? */
        double distM = jobLoc.metersToPoint(gp);
        //Print.logInfo("Comparing JobRadius '"+jobRad+"' to distance '"+distM+"' m");
        return (distM > jobRad)? true : false;

    }

    /**
    *** Gets the Job latitude
    *** @return The Job latitude
    **/
    public double getJobLatitude()
    {
        return this.getOptionalFieldValue(FLD_jobLatitude, 0.0);
    }

    /**
    *** Sets the Job latitude
    *** @param v The Job latitude
    **/
    public void setJobLatitude(double v)
    {
        this.setOptionalFieldValue(FLD_jobLatitude, v);
    }

    /**
    *** Gets the Job longitude
    *** @return The Job longitude
    **/
    public double getJobLongitude()
    {
        return this.getOptionalFieldValue(FLD_jobLongitude, 0.0);
    }

    /**
    *** Sets the Job longitude
    *** @param v The Job longitude
    **/
    public void setJobLongitude(double v)
    {
        this.setOptionalFieldValue(FLD_jobLongitude, v);
    }

    /**
    *** Gets the Job radius, in meters
    *** @return The Job radius, in meters
    **/
    public double getJobRadius()
    {
        return this.getOptionalFieldValue(FLD_jobRadius, 0.0);
    }

    /**
    *** Sets the Job radius, in meters
    *** @param v The Job radius, in meters
    **/
    public void setJobRadius(double v)
    {
        this.setOptionalFieldValue(FLD_jobRadius, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if ELogState is supported
    *** @return True if ELogState is supported
    **/
    public boolean supportsELogState()
    {
        return Device.getFactory().hasField(FLD_lastELogState);
    }

    // --------------------------------

    /**
    *** Returns true if ELog/HOS is enabled for the specified Device
    *** @return True if ELog/HOS is enabled for the specified Device
    **/
    public static boolean IsELogEnabled(Device device)
    {
        return Device.IsELogEnabled(device, false);
    }

    /**
    *** Returns true if ELog/HOS is enabled for the specified Device
    *** @return True if ELog/HOS is enabled for the specified Device
    **/
    public static boolean IsELogEnabled(Device dev, boolean showReason)
    {

        /* check Device */
        if (dev == null) {
            if (showReason) { Print.logDebug("Device is null"); }
            return false;
        } else
        if (!dev.isActive()) {
            if (showReason) { Print.logDebug("Device is inactive: " + dev.getAccountID() + "/" + dev.getDeviceID()); }
            return false;
        } else
        if (!dev.getELogEnabled()) {
            if (showReason) { Print.logDebug("Device ELog is not enabled: " + dev.getAccountID() + "/" + dev.getDeviceID()); }
            return false;
        }

        /* check Account */
        return Account.IsELogEnabled(dev.getAccount(), showReason);

    }

    /**
    *** Returns true if ELog/HOS is enabled for this Device
    *** @return True if ELog/HOS is enabled for this Device
    **/
    public boolean getELogEnabled()
    {
        Boolean v = (Boolean)this.getOptionalFieldValue(FLD_eLogEnabled);
        return (v != null)? v.booleanValue() : false;
    }

    /**
    *** Sets the "ELog/HOS Enabled" state for this Device
    *** @param v The "ELog/HOS Enabled" state for this Device
    **/
    public void setELogEnabled(boolean v)
    {
        this.setOptionalFieldValue(FLD_eLogEnabled, v);
    }

    // --------------------------------

    /**
    *** Gets the last ELog/HOS state<br>
    *** RTP:"lastTS=123456789 lastSC=0xF010 isDriving=true|false distKM=1234.5"
    *** @return The last ELog/HOS state
    **/
    public DTELogState getLastELogState()
    {
        DTELogState v = (DTELogState)this.getFieldValue(FLD_lastELogState);
        return (v != null)? v : new DTELogState();
    }

    /**
    *** Sets the last HOS data push time
    *** @param v The last HOS data push time
    **/
    public void setLastELogState(DTELogState v)
    {
        if (v == null) { v = new DTELogState(); }
        this.setFieldValue(FLD_lastELogState, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the map share fields are supported
    *** @return True if the map share fields are supported
    **/
    public static boolean supportsMapShare()
    {
        return Device.getFactory().hasField(FLD_mapShareStartTime) &&
               Device.getFactory().hasField(FLD_mapShareEndTime);
    }

    // --------------------------------

    /**
    *** Gets the map share start time (zero if disabled)
    *** @return The map share start time
    **/
    public long getMapShareStartTime()
    {
        Long v = (Long)this.getFieldValue(FLD_mapShareStartTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the map share start time (zero to disable)
    *** @param v The map share start time
    **/
    public void setMapShareStartTime(long v)
    {
        this.setFieldValue(FLD_mapShareStartTime, v);
    }

    // --------------------------------

    /**
    *** Gets the map share end time (zero if disabled)
    *** @return The map share end time
    **/
    public long getMapShareEndTime()
    {
        Long v = (Long)this.getFieldValue(FLD_mapShareEndTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the map share end time (zero to disable)
    *** @param v The map share end time
    **/
    public void setMapShareEndTime(long v)
    {
        this.setFieldValue(FLD_mapShareEndTime, v);
    }

    // --------------------------------

    /**
    *** Returns true if the map share passcode is defined
    *** @return True if the map share passcode is defined
    **/
    public boolean hasMapSharePasscode()
    {
        return !StringTools.isBlank(this.getMapSharePasscode());
    }

    /**
    *** Gets the map share passcode
    *** @return The map share passcode
    **/
    public String getMapSharePasscode()
    {
        String v = (String)this.getOptionalFieldValue(FLD_mapSharePasscode);
        return StringTools.trim(v);
    }

    /**
    *** Sets the map share passcode
    *** @param v The map share passcode
    **/
    public void setMapSharePasscode(String v)
    {
        this.setOptionalFieldValue(FLD_mapSharePasscode, StringTools.trim(v));
    }
    
    /**
    *** Returns true if the specified passcode matches the defined map share passcode
    *** @return True if the specified passcode matches the defined map share passcode
    **/
    public boolean checkMapSharePasscode(String passcode)
    {
        if (this.hasMapSharePasscode()) {
            return this.getMapSharePasscode().equals(passcode)? true : false;
        } else {
            // -- passcode not required, accept any specified passcode
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if Subscriber is supported
    *** @return True if Subscriber is supported
    **/
    public static boolean supportsSubscriber()
    {
        return Device.getFactory().hasField(FLD_subscriberID);
    }

    // --------------------------------

    /**
    *** Gets the Device corresponding to the specified SubscriberID
    *** It is up to the caller to check whether this Device or Account are inactive.
    *** @param subID  The Subscriber-ID of the device
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceBySubscriberID(String subID)
        throws DBException
    {

        /* invalid id? */
        if (StringTools.isBlank(subID)) {
            return null; // just say it doesn't exist
        }

        /* read device for subscriber-id */
        Device       dev = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // -- DBSelect: SELECT * FROM Device WHERE (uniqueID='unique')
            DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(
                dwh.EQ(Device.FLD_subscriberID,subID)
            ));
            dsel.setLimit(2);
            // -- Note: The index on the column FLD_subscriberID does not enforce uniqueness
            // -  (since null/empty values are allowed and needed)

            /* get record */
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String acctId = rs.getString(FLD_accountID);
                String devId  = rs.getString(FLD_deviceID);
                dev = new Device(new Device.Key(acctId,devId));
                dev.setAllFieldValues(rs);
                if (rs.next()) {
                    Print.logError("Found multiple occurances of this subscriber-id: " + subID);
                }
                break; // only one record
            }
            // -- it's possible at this point that we haven't even read 1 device

        } catch (SQLException sqe) {
            throw new DBException("Getting Device subscriber-id: " + subID, sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return device */
        // -- Note: 'dev' may be null if it wasn't found
        return dev;

    }

    // --------------------------------

    /**
    *** Returns true if a subscriber-id is defined for this Device
    *** @return True if this Device record defines a SubscriberID
    **/
    public boolean hasSubscriberID()
    {
        return !StringTools.isBlank(this.getSubscriberID());
    }

    /**
    *** Gets the Subscriber-ID, or blank if not defined
    *** @return The Subscriber-ID
    **/
    public String getSubscriberID()
    {
        String v = (String)this.getFieldValue(FLD_subscriberID);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Subscriber-ID
    *** @param v The Subscriber-ID
    **/
    public void setSubscriberID(String v)
    {
        this.setFieldValue(FLD_subscriberID, StringTools.trim(v));
    }

    // --------------------------------

    /**
    *** Gets the Subscriber-Name
    *** @return The Subscriber-Name
    **/
    public String getSubscriberName()
    {
        String v = (String)this.getFieldValue(FLD_subscriberName);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Subscriber-Name
    *** @param v The Subscriber-Name
    **/
    public void setSubscriberName(String v)
    {
        this.setFieldValue(FLD_subscriberName, StringTools.trim(v));
    }

    // --------------------------------

    /**
    *** Gets the Subscriber-Avatar (pushpin/icon URL)
    *** @return The Subscriber-Avatar
    **/
    public String getSubscriberAvatar()
    {
        String v = (String)this.getFieldValue(FLD_subscriberAvatar);
        return StringTools.trim(v);
    }

    /**
    *** Sets the Subscriber-Avatar (pushpin/icon URL)
    *** @param v The Subscriber-Avatar
    **/
    public void setSubscriberAvatar(String v)
    {
        this.setFieldValue(FLD_subscriberAvatar, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if DataPushTime is supported
    *** @return True if DataPushTime is supported
    **/
    public static boolean SupportsDataPushTime()
    {
        return Device.getFactory().hasField(FLD_lastDataPushTime);
    }

    /**
    *** Gets the last data push time
    *** @return The last data push time
    **/
    public long getLastDataPushTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastDataPushTime);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the last data push time
    *** @param v The last data push time
    **/
    public void setLastDataPushTime(long v)
    {
        this.setFieldValue(FLD_lastDataPushTime, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if DataPush is enabled for this Device
    **/
    public boolean isDataPushEnabled()
    {
        return Account.IsDataPushEnabled(this.getAccount());
    }

    /**
    *** Returns true if DataPush is enabled for this Device
    **/
    public static boolean IsDataPushEnabled(Device device)
    {
        return (device != null)? device.isDataPushEnabled() : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Last Event Creation time, in milliseconds
    *** @return The Last Event Creation time, in milliseconds
    **/
    public long getLastEventCreateMillis()
    {
        Long v = (Long)this.getFieldValue(FLD_lastEventCreateMillis);
        return (v != null)? v.longValue() : 0L;
    }

    /**
    *** Sets the Last Event Creation time, in milliseconds
    *** @param v The Last Event Creation time, in milliseconds
    **/
    public void setLastEventCreateMillis(long v)
    {
        this.setFieldValue(FLD_lastEventCreateMillis, v);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the active state of this Device record
    **/
    public boolean getIsActive()
    {
        return super.getIsActive();
    }
    
    /** 
    *** Gets the DCS ative state of this Device record
    **/
    public boolean isDCSActive()
    {
        if (this.getIsActive()) {
            // -- device is active
            return true;
        } else
        if (RTConfig.getBoolean(DBConfig.PROP_Device_insertEventsIfInactive,false)) {
            // -- device is inactive, however save events anyway
            return true;
        } else {
            // -- device is inactive, do not save events
            return false;
        }
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the record creation default values
    **/
    public void setCreationDefaultValues()
    {
        this.setIsActive(true);
        this.setDescription(NEW_DEVICE_NAME_ + " [" + this.getDeviceID() + "]");
        this.setIgnitionIndex(-1);
        // Rules-Engine Allow Notification
        if (Device.hasRuleFactory()) {
            this.setAllowNotify(true);
        }
        this.setNotifyAction(RuleFactory.ACTION_DEFAULT);
        // BorderCrossing 
        if (Device.supportsBorderCrossing()) {
            this.setBorderCrossing(Device.BorderCrossingState.ON);
        }
        // DataTransport attributes below
        this.setSupportedEncodings(Transport.DEFAULT_ENCODING);
        this.setTotalMaxConn(Transport.DEFAULT_TOTAL_MAX_CONNECTIONS);
        this.setDuplexMaxConn(Transport.DEFAULT_DUPLEX_MAX_CONNECTIONS);
        this.setUnitLimitInterval(Transport.DEFAULT_UNIT_LIMIT_INTERVAL_MIN); // Minutes
        this.setTotalMaxConnPerMin(Transport.DEFAULT_TOTAL_MAX_CONNECTIONS_PER_MIN);
        this.setDuplexMaxConnPerMin(Transport.DEFAULT_DUPLEX_MAX_CONNECTIONS_PER_MIN);
        this.setMaxAllowedEvents(Transport.DEFAULT_MAX_ALLOWED_EVENTS);
        // other defaults
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** DataTransport Interface: Gets the Associated AccountID
    *** @return The Associated AccountID
    **/
    public String getAssocAccountID()
    {
        return this.getAccountID();
    }

    /** 
    *** DataTransport Interface: Gets the Associated DeviceID
    *** @return The Associated DeviceID
    **/
    public String getAssocDeviceID()
    {
        return this.getDeviceID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the DCServerConfig instance for this Device
    *** @return The DCServerConfig instance for this Device
    **/
    public DCServerConfig getDCServerConfig()
    {
        return DCServerFactory.getServerConfig(this.getDeviceCode());
    }

    /**
    *** Return a list of supported commands
    *** @param privLabel  The current PrivateLabel instance
    *** @param user       The current user instance
    *** @param type       The command location type (ie. "map", "admin", ...)
    *** @return A map of the specified commands
    **/
    public Map<String,String> getSupportedCommands(BasicPrivateLabel privLabel, User user, 
        String type)
    {
        DCServerConfig dcs = this.getDCServerConfig();
        return (dcs != null)? dcs.getCommandDescriptionMap(privLabel,user,this,type) : null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int     startStopStatusCodes[]    = null;
    private boolean startStopStatusCodes_init = false;

    /**
    *** Gets the "Start/Stop StatusCode supported" config
    *** @return The "Start/Stop StatusCode supported" state
    **/
    public boolean getStartStopSupported()
    {
        return (this.getStartStopStatusCodes() != null);
    }

    /**
    *** Returns the start/stop status codes defined in the Device record
    *** @return The start/stop status codes
    **/
    public int[] getStartStopStatusCodes()
    {
        if (!this.startStopStatusCodes_init) {
            DCServerConfig dcs = this.getDCServerConfig();
            this.startStopStatusCodes = (dcs != null)? dcs.getStartStopStatusCodes() : null;
            this.startStopStatusCodes_init = true;
        }
        return this.startStopStatusCodes;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if sending commands is supported for the specified private-label and user
    *** @param privLabel  The BasicPrivateLabel instance
    *** @param user  The User
    **/
    public boolean isPingSupported(BasicPrivateLabel privLabel, User user)
    {

        /* check ACL */
        DCServerConfig dcs = this.getDCServerConfig();
        if ((privLabel != null) && (dcs != null) && !privLabel.hasWriteAccess(user, dcs.getCommandsAclName())) {
            Print.logDebug("User does not have access to device command handler");
            return false;
        }

        /* PingDispatcher */
        if (Device.hasPingDispatcher()) {
            boolean supported = Device.getPingDispatcher().isPingSupported(this);
            Print.logDebug("Device "+this.getDeviceID()+" isPingSupported = " + supported);
            return supported;
        } else {
            Print.logDebug("Device "+this.getDeviceID()+" does not have a command-handler");
            return false;
        }

    }

    /**
    *** Sends the specified command to this device
    *** @param cmdType  The Command type
    *** @param cmdName  The Command name
    *** @param cmdArgs  The Command args/parameters
    *** @return True if the command was sent successfully
    **/
    public boolean sendDeviceCommand(String cmdType, String cmdName, String cmdArgs[])
    {
        String ct = !StringTools.isBlank(cmdType)? cmdType : DCServerConfig.COMMAND_CONFIG;

        /* DCServerConfig */
        DCServerConfig dcs = this.getDCServerConfig();
        if (dcs != null) {
            // a DCServerConfig is defined
            RTProperties resp = DCServerFactory.sendServerCommand(this, ct, cmdName, cmdArgs);
            Print.logInfo("Ping Response: " + resp);
            boolean sentOK = DCServerFactory.isCommandResultOK(resp);
            return sentOK;
        }

        /* PingDispatcher */
        if (Device.hasPingDispatcher()) {
            boolean sentOK = Device.getPingDispatcher().sendDeviceCommand(this, ct, cmdName, cmdArgs);
            return sentOK;
        } else {
            Print.logWarn("Device has no PingDispatcher");
            return false;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean  allowSlowReverseGeocode = true;

    /**
    *** Enabled/Disabled slow reverse-geocoding (default is to allow)
    *** @param allow  True to allow, false to dis-allow
    **/
    public static void SetAllowSlowReverseGeocoding(boolean allow)
    {
        Device.allowSlowReverseGeocode = allow;
    }

    /**
    *** Returns true is slow reverse-geocoding is allowed
    *** @return  True if allowed, false otherwise
    **/
    public static boolean GetAllowSlowReverseGeocoding()
    {
        return Device.allowSlowReverseGeocode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean  ENABLE_LOAD_TESTING     = true;
    private static Object   loadTestingLock         = new Object();
    private static DateTime loadTestingTime         = null;
    private static long     loadTestingCount        = 0L;

    /**
    *** Gets the number of events between the specified timestamps (inclusive)<br>
    *** Note: will return -1 if EventData table is InnoDB
    *** @param timeStart  The starting timestamp
    *** @param timeEnd    The ending timestamp
    *** @return The number of events between the specified timestamps (inclusive)
    **/
    public long getEventCount(long timeStart, long timeEnd)
        throws DBException
    {
        long count = EventData.getRecordCount(  // -1 for InnoDB?
            this.getAccountID(), this.getDeviceID(),
            timeStart, timeEnd);
        return count;
    }

    /**
    *** Gets the total number of events for this Device/Vehicle
    *** @return The total number of EventData records for this Device
    **/
    public long getEventCount()
        throws DBException
    {
        return this.getEventCount(-1L, -1L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the event information to the log file
    *** @param ev  The event to log
    **/
    public void log_EventData(int logLevel, EventData ev)
    {
        // -- We assume that "ev.getDeviceID().equals(this.getDeviceID())" is true

        /* assemble log info */
        StringBuffer sb = new StringBuffer();
        sb.append("Event: ");
        if (ev != null) {
            // -- Event: 2015/02/27|12:32:45|PST|1425069165, account|device|unique, 0xF020|Location|geozone, latitude|longitude, speed|heading
            int sc = ev.getStatusCode();
            long evTime = ev.getTimestamp();
            DateTime dt = new DateTime(evTime);
            String zone = ev.getGeozoneID();

            /* date|time */
            // -- "2015/02/27|12:32:45|PST|1425069165"
            sb.append(dt.format("yyyy/MM/dd|HH:mm:ss|zzz"));
            sb.append("|").append(evTime);
            sb.append(", ");

            /* account|device (added uniqueID [2.6.5-B58]) */
            sb.append(ev.getAccountID()).append("|").append(ev.getDeviceID()).append("|").append(this.getUniqueID());
            sb.append(", ");

            /* status code */
            sb.append(StatusCodes.GetHex(sc)).append("|").append(StatusCodes.GetDescription(sc,null));
            if (!StringTools.isBlank(zone)) {
                sb.append("|").append(zone);
            } else
            if ((sc == StatusCodes.STATUS_GEOFENCE_ARRIVE) ||
                (sc == StatusCodes.STATUS_GEOFENCE_ARRIVE)   ) {
                sb.append("|?");
            }
            sb.append(", ");

            /* lat|lon */
            sb.append(ev.getGeoPoint().toString('|'));
            sb.append(", ");

            /* speed|heading */
            sb.append(StringTools.format(ev.getSpeedKPH(),"0.0"));
            sb.append("|");
            sb.append(StringTools.format(ev.getHeading(),"0"));

        } else {
            // Event: null

            // create log message
            sb.append("null");

        }

        /* log */
        Print.log(logLevel, sb.toString());

    }

    // ------------------------------------------------------------------------

    /** 
    *** Insert event into EventData table
    *** @param evdb  The EventData record to insert
    *** @return True if no database error occurred, false otherwise.
    ***     Note that the meaning of this return value is different from "insertEventData_throw".
    **/
    public boolean insertEventData(EventData evdb)
    {
        try {
            this.insertEventData_throw(evdb); // return value ignored
            return true; // no database error
        } catch (DBException dbe) {
            return false; // database error (errors already displayed)
        }
    }

    /** 
    *** Insert event into EventData table
    *** @param evdb  The EventData record to insert
    *** @return True if EventData record was inserted
    ***     Note that the meaning of this return value is different from "insertEventData".
    *** @throws DBException If database insert error occurred
    **/
    public boolean insertEventData_throw(EventData evdb)
        throws DBException
    {

        /* log event insertion */
        if (Device.LogEventDataInsertion >= Print.LOG_WARN) {
            // -- LOG_WARN, LOG_INFO, LOG_DEBUG
            this.log_EventData(Device.LogEventDataInsertion, evdb);
        }

        /* insert event */
        try {
            if (!this._insertEventData(evdb)) {
                // -- event was ignored
                Print.logWarn("Event not inserted ...");
                return false; // EventData not inserted, not a database error
            }
        } catch (DBException dbe) {
            // -- error message already displayed
            throw dbe;
        }

        /* status code */
        int sc = evdb.getStatusCode();

        /* check for synthesized GeoCorridor events */
        if (sc == StatusCodes.STATUS_GEOFENCE_ARRIVE) {
            // -- check for GeoCorridor deactivation
            Geozone zone = evdb.getGeozone();
            if (this.hasActiveCorridor() && (zone != null) && zone.isCorridorEnd(evdb)) {
                 this.setActiveCorridor("");            // FLD_activeCorridor
                 // -- TODO: insert STATUS_CORRIDOR_INACTIVE
                 // evdb.setStatusCode(StatusCodes.CORRIDOR_INACTIVE);
                 // this._insertEventData(evdb);
                 // Print.logInfo("Synthesized Corridor deactivation event");
            }
        } else
        if (sc == StatusCodes.STATUS_GEOFENCE_DEPART) {
            // -- check for GeoCorridor activation
            Geozone zone = evdb.getGeozone();
            if ((zone != null) && zone.isCorridorStart(evdb)) {
                String corridorID = zone.getCorridorID();
                this.setActiveCorridor(corridorID);     // FLD_activeCorridor
                 // -- TODO: insert STATUS_CORRIDOR_ACTIVE
                 // evdb.setStatusCode(StatusCodes.STATUS_CORRIDOR_ACTIVE);
                 // this._insertEventData(evdb);
                 // Print.logInfo("Synthesized Corridor activation event");
            }
        }

        /* EventData record inserted */
        return true;

    }

    /**
    *** Insert event into EventData table
    *** @param evdb  The EventData record to insert
    *** @return True if EventData record inserted, false otherwise
    *** @throws DBException If database error has occurred
    **/
    protected boolean _insertEventData(final EventData evdb)
        throws DBException // if database insert exception occurs
    {
        // -- Notes:
        // -  1) This incoming EventData record is populated, but hasn't been saved
        // -  2) This Device record at this point _must_ contain old/last field values 
        //       for proper rule triggers, etc.
        Account account = this.getAccount();
        String  acctID  = this.getAccountID();
        String  devID   = this.getDeviceID();

        /* invalid EventData? */
        if (evdb == null) {
            //Print.logError("EventData is null");
            return false; // not a database error
        }
        int statusCode = evdb.getStatusCode();

        /* set device */
        if (this.getDeferRuleCheckToPostInsert()) {
            // -- save current snapshot of this Device instance
            // -  done to allow deferred EventData rule triggers
            try {
                Device.Key dk = new Device.Key(acctID, devID);
                Device devCopy = dk.getDBRecord();
                devCopy.setAllFieldValues(this); // copy all non-key fields from original Device
                devCopy.setAccount(account);
                evdb.setDevice(devCopy); // pre-rule-trigger copy
            } catch (DBException dbe) {
                // -- will not occur
                Print.logError("Unable to save copy of Device record: " + dbe);
                evdb.setDevice(this); // pre-rule-trigger
            }
            // -- "checkEventRules" may be called in "_postEventInsertionProcessing" thread
        } else {
            // -- save this Device instance
            evdb.setDevice(this); // pre-rule-trigger
            // -- "checkEventRules" must be called inline below
        }
        // -- set the Device used for insertion processing
        evdb.setInsertionDevice(this);

        /* Transport ID */
        evdb.setTransportID(this.getTransportID());

        /* Event time check */
        long eventTime = evdb.getTimestamp();
        long lastEventTime = this.getLastEventTimestamp(); // may be zero
        if (eventTime >= 5000000000L) {
            // -- Event time might be specified in milliseconds
            Print.logWarn("EventData time is invalid (too large): "+eventTime+" [ignoring record]");
            return false; // not a database error
        } else
        if (eventTime <= 0L) {
            // -- Event time is invalid
            Print.logWarn("EventData time is invalid (<=1970/01/01): "+eventTime+" [ignoring record]");
            return false; // not a database error
        }

        /* check for past timestamp */
        int pastDateAction = Device.pastEventDateAction();
        if (pastDateAction != PAST_DATE_DISABLED) {
            long nowTime    = DateTime.getCurrentTimeSec();
            long maxPastSec = Device.pastEventDateMaximumSec();
            long minTime    = (maxPastSec > 0L)? (nowTime - maxPastSec) : 0L;
            if ((minTime > 0L) && (eventTime < minTime)) {
                switch (pastDateAction) {
                    case PAST_DATE_IGNORE:
                        // -- ignore this record
                        Print.logWarn("Invalid EventData past time: "+new DateTime(eventTime)+" [ignoring record per configuration]");
                        return false; // not a database error
                    case PAST_DATE_TRUNCATE:
                        // -- truncate date/time
                        Print.logWarn("Invalid EventData past time: "+new DateTime(eventTime)+" [set/truncate to "+new DateTime(nowTime)+"]");
                        evdb.setTimestamp(nowTime);
                        eventTime = nowTime;
                        break;
                    default:
                        // -- should not occur (just continue)
                        Print.logWarn("Invalid EventData past time: "+new DateTime(eventTime)+" [unexpected action "+pastDateAction+"]");
                        break;
                }
            }
        }

        /* check for future timestamp */
        int futureDateAction = Device.futureEventDateAction();
        if (futureDateAction != FUTURE_DATE_DISABLED) {
            long nowTime      = DateTime.getCurrentTimeSec();
            long maxFutureSec = Device.futureEventDateMaximumSec();
            long maxTime      = (maxFutureSec > 0L)? (nowTime + maxFutureSec) : 0L;
            if ((maxTime > 0L) && (eventTime > maxTime)) {
                switch (futureDateAction) {
                    case FUTURE_DATE_IGNORE:
                        // -- ignore this record
                        Print.logWarn("Invalid EventData future time: "+new DateTime(eventTime)+" [ignoring record per configuration]");
                        return false; // not a database error
                    case FUTURE_DATE_TRUNCATE:
                        // -- truncate date/time
                        Print.logWarn("Invalid EventData future time: "+new DateTime(eventTime)+" [set/truncate to "+new DateTime(nowTime)+"]");
                        evdb.setTimestamp(nowTime);
                        eventTime = nowTime;
                        break;
                    default:
                        // -- should not occur (just continue)
                        Print.logWarn("Invalid EventData future time: "+new DateTime(eventTime)+" [unexpected action "+futureDateAction+"]");
                        break;
                }
            }
        }

        /* old event? (test after time checks) */
        boolean isOldEvent = (eventTime < lastEventTime)? true : false;

        /* check for invalid speed (beyond reasonable maximum) */
        int invalidSpeedAction = Device.invalidSpeedAction();
        if (invalidSpeedAction != INVALID_SPEED_DISABLED) {
            double maxSpeedKPH = Device.invalidSpeedMaximumKPH();
            double evSpeedKPH  = evdb.getSpeedKPH();
            if ((maxSpeedKPH > 0.0) && (evSpeedKPH > maxSpeedKPH)) {
                switch (invalidSpeedAction) {
                    case INVALID_SPEED_DISABLED:
                        // -- will not occur (already checked above)
                        break;
                    case INVALID_SPEED_IGNORE:
                        // -- ignore/skip this record
                        Print.logWarn("Invalid EventData speed: "+evSpeedKPH+" km/h [ignoring/skipping event per config]");
                        // -- TODO: what if this event has an important status-code?
                        return false; // not a database error
                    case INVALID_SPEED_IGNORE_LOC:
                        // -- ignore/skip this record if statusCode is "Location"
                        if (StatusCodes.IsLocation(statusCode)) {
                            // -- ignore/skip this record
                            Print.logWarn("Invalid EventData speed: "+evSpeedKPH+" km/h [ignoring/skipping location event per config]");
                            return false; // not a database error
                        }
                        // -- otherwise set speed to zero
                        evdb.setSpeedKPH(0.0);
                        break;
                    case INVALID_SPEED_TRUNCATE:
                        // -- truncate speed to maximum
                        Print.logWarn("Invalid EventData speed: "+evSpeedKPH+" km/h [set/truncate to "+maxSpeedKPH+"]");
                        evdb.setSpeedKPH(maxSpeedKPH);
                        break;
                    case INVALID_SPEED_ZERO:
                        // -- set speed to zero
                        Print.logWarn("Invalid EventData speed: "+evSpeedKPH+" km/h [set to 0.0]");
                        evdb.setSpeedKPH(0.0);
                        break;
                    default:
                        // -- should not occur (just continue)
                        Print.logWarn("Invalid EventData speed: "+evSpeedKPH+" km/h [unexpected action "+invalidSpeedAction+"]");
                        break;
                }
            }
        }

        /* no status code? */
        if (statusCode == StatusCodes.STATUS_NONE) {
            // -- '0' status codes are quietly consumed/ignored.
            if (ENABLE_LOAD_TESTING) {
                // -- This section is for load testing (not used in production)
                if (loadTestingTime == null) {
                    synchronized (loadTestingLock) {
                        if (loadTestingTime == null) { loadTestingTime = new DateTime(/*tz*/); }
                    }
                }
                long deltaSec = DateTime.getCurrentTimeSec() - loadTestingTime.getTimeSec();
                if (deltaSec > 60L) {
                    // -- reset every minute
                    synchronized (loadTestingLock) {
                        loadTestingTime = new DateTime(/*tz*/);
                        loadTestingCount = 0L;
                        deltaSec = 0L;
                    }
                }
                loadTestingCount++;
                double eps = (deltaSec > 0L)? ((double)loadTestingCount / (double)deltaSec) : loadTestingCount;
                if ((loadTestingCount % 50) == 0) {
                    System.err.println("EventData LoadTest (" + eps + " ev/sec)");
                }
            }
            return true; // debug/testing only
        }

        /* extended EventData record update */
        int extUpdate = EXT_UPDATE_NONE;

        /* update GPS location based on Geozone */
        if (UpdateEventWithGeozoneLocation() && !evdb.isValidGeoPoint() && evdb.hasGeozoneID()) {
            if ((statusCode != StatusCodes.STATUS_GEOFENCE_DEPART  ) &&
                (statusCode != StatusCodes.STATUS_GEOFENCE_INACTIVE)   ) {
                Geozone gz = evdb.getGeozone();
                if (gz != null) {
                    GeoPoint gp = gz.getCenterGeoPoint();
                    evdb.setGeoPoint(gp);
                }
            }
        }

        /* CellTower GPS location */
        if (!evdb.isValidGeoPoint() && evdb.canUpdateCellTowerLocation()) {
            boolean ALWAYS_UPDATE_CELLGPS = true;
            CellTower dct = !ALWAYS_UPDATE_CELLGPS? this.getLastServingCellTower() : null;
            if (dct == null) {
                // -- No last CellID
                extUpdate |= EXT_UPDATE_CELLGPS;
            } else
            if (!dct.equals(evdb.getServingCellTower())) {
                // -- Last CellID does not match current CellID
                extUpdate |= EXT_UPDATE_CELLGPS;
            } else {
                // -- We have a last cellID
                MobileLocation ml = dct.getMobileLocation();
                if ((ml != null) && ml.isValid()) {
                    GeoPoint mgp = ml.getGeoPoint();
                    double   acc = ml.getAccuracy();
                    evdb.setCellGeoPoint(mgp);
                    evdb.setCellAccuracy(acc);
                    Print.logInfo("Using cached CellTower location: " + mgp + " [+/- " + acc + " meters]");
                } else {
                    // -- No MobileLocation means that the CellID did not have a known location
                    Print.logInfo("Using cached CellTower location: no location");
                }
            }
        }

        /* set geozone/reverse-geocode address */
        try {
            Set<String> updFields = evdb.updateAddress(true/*fastOnly*/); // may throw SlowOperationException
            if (updFields != null) {
                BasicPrivateLabel privLabel = account.getPrivateLabel();
                ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
                String rgName = (rgp != null)? rgp.getName() : "???";
                Print.logInfo("EventData address: [%s/%s:%s] %s: %s",
                    this.getAccountID(), this.getDeviceID(), rgName,
                    evdb.getGeoPoint().toString(), evdb.getAddress());
                // -- we don't care about the names of the fields updated, since all fields will be saved below
                // -- update Device "lastSubdivision"
                String subDiv = evdb.getSubdivision();
                if (!StringTools.isBlank(subDiv)) {
                    this.setLastSubdivision(subDiv); // FLD_lastSubdivision
                    try {
                        this.update(Device.FLD_lastSubdivision);
                    } catch (DBException dbe) {
                        Print.logError("Unable to update Device: " + dbe);
                    }
                }
            } else {
                //Print.logDebug("Address not updated (already set, or account disabled).");
            }
        } catch (SlowOperationException soe) {
            // -- The address update has not been performed because the operation would have
            // -  taken too long [per 'isFastOperation()' method in ReverseGeocodeProvider instance].
            // -  This address update will need to be queued for background processing.
            if (Device.allowSlowReverseGeocode) {
                extUpdate |= EXT_UPDATE_ADDRESS;
            } else {
                Print.logWarn("Skipping slow reverse-geocoding ...");
            }
        } catch (Throwable th) {
            Print.logException("Address update error", th);
        }

        /* stateline border-crossing check */
        // -- not performed here (SLBC check performed in cron task)
        //if (this.getBorderCrossing() == Device.BorderCrossingState.ON.getIntValue()) {
        //   // -- border-crossing is always considered a slow operation
        //   //extUpdate |= EXT_UPDATE_BORDER;
        //}

        /* distance since last valid GeoPoint */
        GeoPoint evdbGP = evdb.getGeoPoint();
        GeoPoint lastGP = this.getLastValidLocation(); // null if uninitialized
        double   deltaM = (GeoPoint.isValid(lastGP) && GeoPoint.isValid(evdbGP))?
            lastGP.metersToPoint(evdbGP) : -1.0;

        /* check for motion change */
        long lastStopTime  = this.getLastStopTime();  // '0' if uninitialized
        long lastStartTime = this.getLastStartTime(); // '0' if uninitialized
        long nextStopTime  = 0L;
        long nextStartTime = 0L;
        if ((lastStartTime > lastStopTime)/* && (eventTime > lastStartTime)*/) {
            // -- last state was "moving"
            if (evdb.isStopEvent(true)) {
                // -- was "moving", now "stopped"
                evdb.setStopped(true);
                nextStopTime = eventTime;
            } else {
                // -- continue "moving"
                evdb.setStopped(false);
            }
        } else
        if ((lastStopTime > lastStartTime)/* && (eventTime > lastStopTime)*/) {
            // -- last state was "stopped"
            if ((EVENT_START_MOTION_RADIUS_M > 0.0) && (deltaM > EVENT_START_MOTION_RADIUS_M)) {
                // -- was "stopped", new GeoPoint has moved, now "moving"
                evdb.setStopped(false);
                nextStartTime = eventTime;
            } else
            if (evdb.isStartEvent(true)) {
                // -- was "stopped", now "moving"
                /*
                if (!isOldEvent && AUTO_GENERATE_NON_MOVING_EVENT && 
                    ((eventTime - lastEventTime) < MAX_STOPPED_DELTA_SEC)) {
                    EventData newEv = this.getLastStopEvent();
                    if (newEv != null) {
                        newEv.setTimestamp(eventTime - 2L);
                        newEv.setStatusCode(StatusCodes.STATUS_MOTION_DORMANT);
                        newEv.setSpeedKPH(0.0); // make sure speed is 0
                        //newEv.setMotionChangeTime(lastStopTime);
                        try {
                            newEv.save(); // insert();
                        } catch (DBException dbe) {
                            // save failed
                        }
                    }
                }
                */
                evdb.setStopped(false);
                nextStartTime = eventTime;
            } else {
                // -- continue "stopped"
                evdb.setStopped(true);
            }
        } else {
            // -- undefined lastStopTime/lastStartTime
            if ((EVENT_START_MOTION_RADIUS_M > 0.0) && (deltaM > EVENT_START_MOTION_RADIUS_M)) {
                // -- new GeoPoint has moved, is "moving"
                evdb.setStopped(false);
                nextStartTime = eventTime;
            } else
            if (evdb.isStopEvent(true)) {
                // -- is "stopped"
                evdb.setStopped(true);
                nextStopTime = eventTime;
            } else
            if (evdb.isStartEvent(true)) {
                // -- is "moving"
                evdb.setStopped(false);
                nextStartTime = eventTime;
            } else {
                // -- check speed only
                if (evdb.getSpeedKPH() <= 0.0) {
                    // -- assume "stopped"
                    evdb.setStopped(true);
                    nextStopTime = eventTime;
                } else {
                    // -- assume "moving"
                    evdb.setStopped(false);
                    nextStartTime = eventTime;
                }
            }
        }

        /* last digital input? */
        if (!evdb.isInputMaskExplicitlySet()) {
            // -- "inputMask" not explicitly set by DCS, set to last saved Device input state
            evdb.setInputMask(this.getLastInputState());
        }

        /* last digital output? */
        if (!evdb.isOutputMaskExplicitlySet()) {
            // -- "outputMask" not explicitly set by DCS, set to last saved Device output state
            evdb.setOutputMask(this.getLastOutputState());
        }

        /* status code on/off state */
        /*
        StatusCodeState statusCodeState = this.getStatusCodeBinaryState(statusCode);
        if (statusCodeState != null) {
            int     scBitNdx = statusCodeState.getIndex();
            boolean scBitVal = statusCodeState.getState();
            this.setStatusCodeStateBit(scBitNdx, scBitVal);
        }
        */

        /* ignition state */
        long  lastIgnOn = this.getLastIgnitionOnTime();     // Prior: the time the ignition previously turned on
        long lastIgnOff = this.getLastIgnitionOffTime();    // Prior: the time the ignition previously turned off
        int  ignStateCh = this.getEventIgnitionState(evdb); // Current: -1=NoChange, 0=Off, 1=On
        // -- TODO: what if 'eventTime' is less than 'lastIgnOn' or 'lastIgnOff'
        if (lastIgnOn < lastIgnOff) {
            // -- last ignition-on is BEFORE ignition-off, clear ignition-on
            lastIgnOn  = 0L; // last state confirmed ignition-off
            // -- (lastIgnOff > 0) 'ignStateCh' should not be '0'
        } else
        if (lastIgnOff < lastIgnOn) {
            // -- last ignition-off is BEFORE ignition-on, clear ignition-off
            lastIgnOff = 0L; // last state confirmed ignition-off
            // -- (lastIgnOn > 0) 'ignStateCh' should not be '1'
        } else
        if ((lastIgnOff > 0L) && (lastIgnOff == lastIgnOn)) {
            // -- [2.5.8-B36]
            // -  Ignition On/Off occurring at the same time?  Should never occur, however go through
            // -  some additional checks to see what the ignition state actually was previously.
            Print.logWarn("Resolving simultaneous Igniton On/Off state ...");
            int ignNdx = this.getIgnitionIndex();
            if (ignNdx < 0) {  // [2.5.8-B37]
                // -- ignition state not defined (this should not occur here, since both "lastIgnOff"/"lastIgnOn" would be 0)
                // -  assume prior ignition state was off
                Print.logInfo("Assuming prior Igniton state was off ...");
                lastIgnOn = 0L; // clear "On" time
                this.setLastIgnitionOnTime(0L);  // FLD_lastIgnitionOnTime
            } else
            if (ignNdx < StatusCodes.IGNITION_INPUT_INDEX) {  // [2.5.8-B37]
                // -- ignition state expressed by digital input
                if (this.getLastInputState(ignNdx)) {
                    // -- ignition was on, "lastIgnOff" should be "0"
                    Print.logInfo("Assuming prior Igniton state was on ...");
                    lastIgnOff = 0L; // clear "Off" time
                    this.setLastIgnitionOffTime(0L);  // FLD_lastIgnitionOffTime
                } else {
                    // -- prior ignition was off, "lastIgnOn" should be "0"
                    Print.logInfo("Assuming prior Igniton state was off ...");
                    lastIgnOn = 0L; // clear "On" time
                    this.setLastIgnitionOnTime(0L);  // FLD_lastIgnitionOnTime
                }
            } else {  // [2.5.8-B37]
                // -- ignition state expressed by status code?
                // -  TODO: should we look backward for an ignition statis code 
                // -  assume prior ignition state was off for now
                Print.logInfo("Assuming prior Igniton state was off ...");
                lastIgnOn = 0L; // clear "On" time
                this.setLastIgnitionOnTime(0L);  // FLD_lastIgnitionOnTime
            }
        }
        boolean hasIgnSt = ((lastIgnOn > 0L) || (lastIgnOff > 0L))? true : false;
        // -- simulated ignition-hours based on ignition-on elapsed time
        double ignHours;
        if (lastIgnOn > 0L) {
            // -- ignition is on (relative to the current event "evdb")
            evdb.setIgnitionState(EventData.IgnitionState.ON);
            ignHours = this.getLastIgnitionOnHours(); // ignition-hours at last ignition on
            if (eventTime > lastIgnOn) {
                // -- ignition has been on, and current event timestamp is after lastIgnitionOn time
                double runHrs = (double)(eventTime - lastIgnOn) / 3600.0; // elapsed hours since last ignition-on
                ignHours += runHrs; // total elapsed ignition-on since last ignition-on
            }
        } else
        if (lastIgnOff > 0L) {
            // -- ignition is off (relative to the current event "evdb")
            // -    use last ignition hours [fixed v2.5.7-B32]
            evdb.setIgnitionState(EventData.IgnitionState.OFF);
            ignHours = this.getLastIgnitionHours();
        } else {
            // -- ignition is unknown (relative to the current event "evdb")
            // -    use last ignition hours
            evdb.setIgnitionState(EventData.IgnitionState.UNKNOWN);
            ignHours = this.getLastIgnitionHours();
        }

        /* engine state (if available) */
        // -- TODO: what if the device used to emit Engine On/Off, but no longer does?
        long lastEngOn  = this.getLastEngineOnTime();
        long lastEngOff = this.getLastEngineOffTime();
        // -- TODO: what if 'eventTime' is less than 'lastEngOn' or 'lastEngOff'
        if (lastEngOn < lastEngOff) {
            // -- last engine-on is BEFORE engine-off, clear engine-on
            lastEngOn  = 0L; // last state confirmed engine-off
            this.setLastEngineOnTime(0L); // FLD_lastEngineOnTime
        } else
        if (lastEngOff < lastEngOn) {
            // -- last engine-off is BEFORE engine-on, clear engine-off
            lastEngOff = 0L; // last state confirmed engine-off
            this.setLastEngineOffTime(0L); // FLD_lastEngineOffTime
        } else
        if ((lastEngOff > 0L) && (lastEngOff == lastEngOn)) {
            // -- [2.5.8-B36]
            // -  Engine On/Off occurring at the same time?  Should never occur, assume engine-off
            lastEngOn  = 0L; // last state confirmed engine-off
            this.setLastEngineOnTime(0L); // FLD_lastEngineOnTime
        }
        // -- simulated engine-hours based on engine-on elapsed time (EXPERIMENTAL)
        if (Device.GetSimulateEngineHours(this)) {
            boolean hasEngSt = ((lastEngOn > 0L) || (lastEngOff > 0L))? true : false;
            // -- current engine hours
            double engHours;
            if (lastEngOn <= 0L) {
                // -- engine is off, use last engine hours [fixed v2.5.7-B32]
                engHours = this.getLastEngineHours();
            } else {
                engHours = this.getLastEngineOnHours(); // engine-hours at engine on
                if (eventTime > lastEngOn) {
                    // -- engine has been on, and current event timestamp is after lastEngineOn time
                    double runHrs = (double)(eventTime - lastEngOn) / 3600.0; // elapsed hours since last engine-on
                    engHours += runHrs; // total elapsed engine-on since last engine-on
                }
            }
            // -- set event engine hours
            if (evdb.getEngineHours() > 0.0) {
                // -- event already has engine hours, leave as-is
                Print.logDebug("["+acctID+"/"+devID+"] SimEngHours: Event engine-hours already set (leaving as-is): " + evdb.getEngineHours());
            } else
            if (hasEngSt && (engHours > 0.0)) {
                // -- save current engine-hours as event engine-hours
                Print.logDebug("["+acctID+"/"+devID+"] SimEngHours: Event engine-hours set to " + engHours + " hours (based on Engine On/Off)");
                evdb.setEngineHours(engHours);
                // -- saved in Device record below (setLastEngineHours)
            } else
            if (hasIgnSt && (ignHours > 0.0)) {
                // -- save current ignition-hours as event engine-hours
                Print.logDebug("["+acctID+"/"+devID+"] SimEngHours: Event engine-hours set to " + ignHours + " hours (based on Ignition On/Off)");
                evdb.setEngineHours(ignHours);
                // -- saved in Device record below (setLastEngineHours)
            } else {
                // -- save old Device engine-hours as event engine-hours (may be 0.0)
                Print.logDebug("["+acctID+"/"+devID+"] SimEngHours: No available engine/ignition hours");
                evdb.setEngineHours(engHours);
                // -- saved in Device record below (setLastEngineHours)
            }
        }

        /* PTO state (we are assuming that PTO ON and ENGAGED mean the same thing) */
        int  lastPtoState = -1; // unknown
        long lastPtoOn    = this.getLastPtoOnTime();
        long lastPtoOff   = this.getLastPtoOffTime();
        // -- last PTO state
        if (lastPtoOn < lastPtoOff) {
            // -- last PTO-on is BEFORE PTO-off, clear PTO-on
            if (lastPtoOn > 0L) { // should already be "0", but check anyway
                lastPtoOn = 0L; // last state confirmed PTO-off
                this.setLastPtoOnTime(0L); // FLD_lastPtoOnTime
            }
            // -- PTO was off
            lastPtoState = 0;
        } else
        if (lastPtoOff < lastPtoOn) {
            // -- last pto-off is BEFORE pto-on, clear pto-off
            if (lastPtoOff > 0L) { // should already be "0", but check anyway
                lastPtoOff = 0L; // last state confirmed pto-off
                this.setLastPtoOffTime(0L); // FLD_lastPtoOffTime
            }
            // -- PTO was on
            lastPtoState = 1;
        } else
        if ((lastPtoOff > 0L) && (lastPtoOff == lastPtoOn)) {
            // -- PTO On/Off occurring at the same time?  Should never occur, assume pto-off
            lastPtoOn = 0L; // last state confirmed pto-off
            this.setLastPtoOnTime(0L); // FLD_lastPtoOnTime
            // -- assume PTO was off
            lastPtoState = 0;
        }
        // -- new PTO state
        int newPtoState = -1; // unknown
        if (statusCode == StatusCodes.STATUS_PTO_ON) {
            // -- PTO is on 
            evdb.setPtoEngaged(true); // device may not provide a separate indicator
            newPtoState = 1;
        } else
        if (statusCode == StatusCodes.STATUS_PTO_OFF) {
            // -- PTO is off
            if (evdb.getPtoEngaged()) {
                Print.logWarn("PTO-Off event received with PTO-engaged set to 'true' (resetting to off)");
            }
            evdb.setPtoEngaged(false);
            newPtoState = 0;
        } else 
        if (evdb.getPtoEngaged()) {
            // -- event indicates PTO is ON
            newPtoState = 1;
        } else {
            // -- event indicates PTO is OFF (could be unknown?)
            newPtoState = 0; 
        }
        // -- simulated PTO-hours based on PTO-on elapsed time (EXPERIMENTAL)
        if (Device.GetSimulatePtoHours(this)) {
            boolean hasPtoSt = ((lastPtoOn > 0L) || (lastPtoOff > 0L))? true : false;
            // -- current calculated PTO hours
            double ptoHours;
            if (lastPtoOn <= 0L) {
                // -- pto is off, use last pto hours
                ptoHours = this.getLastPtoHours();
            } else {
                // -- pto is on
                ptoHours = this.getLastPtoOnHours(); // pto-hours at pto on
                if (eventTime > lastPtoOn) {
                    // -- pto has been on, and current event timestamp is after lastPtoOn time
                    double runHrs = (double)(eventTime - lastPtoOn) / 3600.0; // elapsed hours since last pto-on
                    ptoHours += runHrs; // total elapsed pto-on since last pto-on
                }
            }
            // -- set event PTO hours
            if (evdb.getPtoHours() > 0.0) {
                // -- event already has PTO hours, leave as-is
                Print.logDebug("["+acctID+"/"+devID+"] SimPtoHours: Event PTO-hours already set (leaving as-is): "+evdb.getPtoHours()+" ["+ptoHours+"]");
            } else
            if (hasPtoSt && (ptoHours > 0.0)) {
                // -- save current PTO-hours as event PTO-hours
                Print.logDebug("["+acctID+"/"+devID+"] SimPtoHours: Event PTO-hours set to "+ptoHours+" hours (based on PTO On/Off)");
                evdb.setPtoHours(ptoHours);
                // -- saved in Device record below (setLastPtoHours)
            } else {
                // -- save old Device PTO-hours as event PTO-hours (may be 0.0)
                Print.logDebug("["+acctID+"/"+devID+"] SimPtoHours: No available PTO hours");
                evdb.setPtoHours(ptoHours);
                // -- saved in Device record below (setLastPtoHours)
            }
        }

        /* driver ID */
        String  driverID         = null;
        long    driverStatus     = -1L;
        boolean saveDriverID     = false;
        boolean saveDriverStatus = false;
        int     propSaveDriverID = Device.GetSaveEventDriverID();
        if (propSaveDriverID != SaveDriverID_NEVER) { // "always" or "nonblank"
            // -- save EventData DriverID
            // evdb.setDriverID(evdb.getRfidTag());
            // evdb.setDriverID(StringTools.toHexString(Payload.reverseByteOrder(StringTools.parseLong(evdb.getRfidTag(),0L),8),64));
            if (evdb.hasDriverID() || (propSaveDriverID == SaveDriverID_ALWAYS)) {
                // -- update device driver-id (even if blank)
                driverID = evdb.getDriverID(); // may be blank
                saveDriverID = true;
                if (evdb.hasDriverStatus()) {
                    // -- also update device driver-status
                    driverStatus = evdb.getDriverStatus();
                    saveDriverStatus = true;
                } else
                if (this.isDriverID(driverID)) {
                    // -- same driver-id: update driver-status with last device driver status
                    driverStatus = this.getDriverStatus();
                    evdb.setDriverStatus(driverStatus);
                } else {
                    // -- different driver-id: clear driver device driver status
                    driverStatus = -1L;
                    saveDriverStatus = true;
                }
            } else
            if (this.hasDriverID()) {
                // -- update event with previous DriverID
                driverID = this.getDriverID();
                evdb.setDriverID(driverID);
                if (evdb.hasDriverStatus()) {
                    // -- TODO: leave as-is? or reset?
                    driverStatus = evdb.getDriverStatus();
                    saveDriverStatus = true;
                } else
                if (this.hasDriverStatus()) {
                    // -- update event with previous driver-status
                    driverStatus = this.getDriverStatus();
                    evdb.setDriverStatus(driverStatus);
                } else {
                    // -- clear event driver status
                    driverStatus = -1L;
                    evdb.setDriverStatus(driverStatus);
                    saveDriverStatus = true;
                }
            } else {
                // -- neither EvenData, nor Device record have a driverID
            }
        }

        /* reset odometer if ignition off? */
        boolean RESET_ODOMETER_WHILE_IGNITION_OFF = false;
        if (RESET_ODOMETER_WHILE_IGNITION_OFF) {
            // -- Check ignition state
            boolean ignTurnedOn = (ignStateCh == 1)? true : false; // ignition just turned on
            boolean ignIsOn     = (lastIgnOn > 0L)? true : false;  // current ignition state
            if (!ignIsOn || ignTurnedOn) {
                // -- ignition is off, or ignition just turned back on
                evdb.setOdometerKM(this.getLastOdometerKM());
            }
        }

        /* current odometer offset */
        evdb.setOdometerOffsetKM(this.getOdometerOffsetKM());

        // ---------------------------------------------------------------------

        /* event is STATUS_GFMI_STOP_STATUS_1 and event already exists */
        // -- Some devices have been known to send multiple STATUS_GFMI_STOP_STATUS_1 events
        // -  within the same second, which could cause some events to overwrite others.
        // -  This section attemps to prevent overwrites by prechecking for existing stop-status
        // -  events and modifying the status code if existing such events are found.
        if (statusCode == StatusCodes.STATUS_GFMI_STOP_STATUS_1) {
            String aid = evdb.getAccountID();
            String did = evdb.getDeviceID();
            long   ts  = evdb.getTimestamp();
            try {
                for (int sc : StatusCodes.GFMI_StopStatus) {
                    EventData gfmiEV = EventData.getEventData(aid,did,ts,sc);
                    // -- check if not found
                    if (gfmiEV == null) {
                        // -- not found, use this status code
                        if (sc != statusCode) {
                            statusCode = sc;
                            evdb.setStatusCode(statusCode);
                        }
                        break;
                    }
                    // -- event already exists
                    if ((gfmiEV.getStopID()     == evdb.getStopID()    ) || 
                        (gfmiEV.getStopStatus() == evdb.getStopStatus())   ) {
                        // same event (this stop id/status apparently already exists)
                        if (sc != statusCode) {
                            statusCode = sc;
                            evdb.setStatusCode(statusCode);
                        }
                        break;
                    }
                    // -- stop id/status is different, try again
                    continue;
                }
            } catch (DBException dbe) {
                Print.logException("Unable to read GFMI StopStatus events", dbe);
            }
        }

        // ---------------------------------------------------------------------

        /* callback: event will be inserted */
        // -- pre-adjustments to EventData record have been completed.
        // -- reverse-geocoding, etc, may be deferred.
        // -- "this" Device instance has not yet been modified.
        this.eventWillInsert(evdb);

        /* save EventData record */
        try {
            evdb.save(); // insert();
            // -- may be re-saved below after deferred reverse-geocode
        } catch (DBException dbe) {
            // -- save failed, print error
            Print.logError("EventData save failed: " + dbe);
            if (Print.isDebugLoggingLevel()) {
                dbe.printException();
            }
            //return false; // databse error, unable to save event
            throw dbe;
        }

        /* are we deferring the call to "checkEventRules(evdb)"? */
        final boolean deferRuleCheck;
        if (!this.getDeferRuleCheckToPostInsert()) {
            // -- do not defer: deferred rule check not enabled
            deferRuleCheck = false;
        } else
        if (extUpdate == EXT_UPDATE_NONE) {
            // -- do not defer: post event processing will not be invoked!
            deferRuleCheck = false;
        } else {
            // -- deferred rule check enabled, and post processing will occur
            deferRuleCheck = true;
        }

        /* background processes */
        if (extUpdate != EXT_UPDATE_NONE) {
            // -- queue for background processing
            final int extUpd = extUpdate; // mask
            Runnable job = new Runnable() {
                public void run() {
                    Device.this._postEventInsertionProcessing(evdb, extUpd, deferRuleCheck);
                }
            };
            ThreadPool_DeviceEventUpdate.run(job);
            Print.logDebug("Address update queued for background operation");
        }

        // ---------------------------------------------------------------------
        // Device record MUST not have been changed before this point

        /* check rules */
        // -- "evdb.setDevice(this)" already set above
        // -- "checkEventRules" may recursively call "_insertEventData"
        // -- TODO: check "((extUpdate & EXT_UPDATE_ADDRESS) != 0)"
        if (!deferRuleCheck) { // this.getDeferRuleCheckToPostInsert()
            // -- not deferred: perform rule check now
            if (this.checkEventRules(evdb)) { 
                // -- Fields may have changed: (NOTE: not yet saved)
                // -   FLD_lastNotifyTime
                // -   FLD_lastNotifyCode
            }
        }

        // ---------------------------------------------------------------------
        // Device record can now be updated
        // Note: if this.getDeferRuleCheckToPostInsert()==true, then the copy held by evdb.getDevice() is unchanged

        /* update fields to reflect this event */
        // -- NOTE: Device not yet saved!

        /* count Events-Per-Second */
        this._countEventsPerSecond(); // FLD_lastEventsPerSecond, FLD_lastEventsPerSecondMS

        /* last valid event timestamp/statusCode */
        this.setLastEventTimestamp(evdb.getTimestamp());        // FLD_lastEventTimestamp
        this.setLastEventStatusCode(evdb.getStatusCode());      // FLD_lastEventStatusCode

        /* latitude/longitude */
        if (evdb.isValidGeoPoint()) {
            // -- save current lastValid location
            this.saveOriginalLastValidGeoPoint(); // may be used by this.getDeferRuleCheckToPostInsert()
            // -- update last valid location
            long gpsTimestamp = evdb.getGpsTimestamp();         // [2.6.2-B44]
            this.setLastValidLatitude(evdb.getLatitude());      // FLD_lastValidLatitude
            this.setLastValidLongitude(evdb.getLongitude());    // FLD_lastValidLongitude
            this.setLastValidSpeedKPH(evdb.getSpeedKPH());      // FLD_lastValidSpeedKPH
            this.setLastValidHeading(evdb.getHeading());        // FLD_lastValidHeading
            this.setLastGPSTimestamp(gpsTimestamp);             // FLD_lastGPSTimestamp
        }

        /* motion change */
        if (nextStopTime > 0L) {
            this.setLastStopTime(nextStopTime);                 // FLD_lastStopTime
        }
        if (nextStartTime > 0L) {
            this.setLastStartTime(nextStartTime);               // FLD_lastStartTime
        }

        /* malfunction-indicator-lamp (MIL) */
        if (evdb.hasMalfunctionLamp()) {
            // -- sets Device last MIL if it was explicitly set in EventData
            this.setLastMalfunctionLamp(evdb.getMalfunctionLamp());   // FLD_lastMalfunctionLamp
        }

        /* fault code */
        if (evdb.hasFaultCode()) {
            this.appendLastFaultCode(evdb.getFaultCode());      // FLD_lastFaultCode
        }

        /* distance */
        {
            // -- set last distance
            double distKM = evdb.getDistanceKM();
            if (distKM < 0.0) {
                // -- skip (not provided by DCS)
            } else {
                this.setLastDistanceKM(distKM);       // FLD_lastDistanceKM
            }
        }

        /* odometer */
        {
            // -- set last odometer
            double odomKM = evdb.getOdometerKM();
            if (odomKM < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (odomKM == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastOdometerKM(odomKM);       // FLD_lastOdometerKM
            }
        }

        /* fuel consumption */
        {
            double fuelTotal = evdb.getFuelTotal();
            if (fuelTotal < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (fuelTotal == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastFuelTotal(fuelTotal);         // FLD_lastFuelTotal
            }
        }

        /* engine hours, as provided by the EventData record */
        {
            // -- save engine hours
            double evEngHours = evdb.getEngineHours();
            if (evEngHours < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (evEngHours == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastEngineHours(evEngHours);        // FLD_lastEngineHours
            }
            // -- set engine on/off time, based on event status code
            if (statusCode == StatusCodes.STATUS_ENGINE_START) {
                double devEngHours = this.getLastEngineHours();
                this.setLastEngineOnHours(devEngHours);     // FLD_lastEngineOnHours
                this.setLastEngineOnTime(eventTime);        // FLD_lastEngineOnTime
                this.setLastEngineOffTime(0L);              // FLD_lastEngineOffTime
            } else
            if (statusCode == StatusCodes.STATUS_ENGINE_STOP) {
                this.setLastEngineOnHours(0.0);             // FLD_lastEngineOnHours
                this.setLastEngineOnTime(0L);               // FLD_lastEngineOnTime
                this.setLastEngineOffTime(eventTime);       // FLD_lastEngineOffTime
            }
        }

        /* PTO hours, as provided by the EventData record */
        {
            // -- save pto hours
            double evPtoHours = evdb.getPtoHours();
            if (evPtoHours < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (evPtoHours == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastPtoHours(evPtoHours);        // FLD_lastPtoHours
            }
            // -- set pto on/off time, based on event status code
            if ((lastPtoState != 1) && (newPtoState == 1)) {
                // -- PTO went from OFF/unknown to ON
                double devPtoHours = this.getLastPtoHours();
                this.setLastPtoOnHours(devPtoHours);     // FLD_lastPtoOnHours
                this.setLastPtoOnTime(eventTime);        // FLD_lastPtoOnTime
                this.setLastPtoOffTime(0L);              // FLD_lastPtoOffTime
            } else
            if ((lastPtoState != 0) && (newPtoState == 0)) {
                // -- PTO went from ON/unknown to OFF
                this.setLastPtoOnHours(0.0);             // FLD_lastPtoOnHours
                this.setLastPtoOnTime(0L);               // FLD_lastPtoOnTime
                this.setLastPtoOffTime(eventTime);       // FLD_lastPtoOffTime
            }
        } 

        /* current ignition state change */
        if (ignStateCh == 1) { 
            // -- current Ignition state changed from OFF to ON
            if (lastIgnOn > 0L) { // (lastIgnOn > lastIgnOff)
                // -- last ignition ON was already set
                // -  can occur if a previous EventData was handled during the same device event [2.5.8-B34]
                if (lastIgnOn == eventTime) { // [2.5.8-B37]
                    // -- we've likely already handled ignition state on a previous pass for the same device event
                } else {
                    Print.logWarn("Ignition-ON event found, without interleaving Ignition-OFF: " + acctID + "/" + devID);
                }
                // -- leave ignition-on time as-is
                //double ignH = this.getLastIgnitionHours();
                //this.setLastIgnitionOnHours(ignH);            // FLD_lastIgnitionOnHours
                //this.setLastIgnitionOnTime(eventTime);        // FLD_lastIgnitionOnTime
            } else {
                // -- last ignition state was OFF (not ON), save new last ignition-ON time
                double ignH = this.getLastIgnitionHours();
                this.setLastIgnitionOnHours(ignH);              // FLD_lastIgnitionOnHours
                this.setLastIgnitionOnTime(eventTime);          // FLD_lastIgnitionOnTime
                // -- "ignHours" is old here, no need to set
            }
            // -- clear ignition OFF time
            if (eventTime < this.getLastIgnitionOffTime()) { // unlikely
                // -- make sure lastIgnitionOffTime reflects Ignition-ON
                Print.logWarn("Event time is prior to last Ignition-OFF! " + acctID + "/" + devID);
            }
            this.setLastIgnitionOffTime(0L);                    // FLD_lastIgnitionOffTime  [2.5.8-B37]
        } else
        if (ignStateCh == 0) { 
            // -- current Ignition state changed from ON to OFF
            if (lastIgnOff > 0L) { // (lastIgnOff > lastIgnOn)
                // -- last ignition OFF was already set
                // -  can occur if a previous EventData was handled during the same device event [2.5.8-B34]
                if (lastIgnOff == eventTime) { // [2.5.8-B37]
                    // -- we've likely already handled ignition state on a previous pass for the same device event
                } else {
                    Print.logWarn("Ignition-OFF event found, without interleaving Ignition-ON: " + acctID + "/" + devID);
                }
                // -- leave ignition-off time as-is
                //this.setLastIgnitionOffTime(eventTime);       // FLD_lastIgnitionOffTime
            } else {
                // -- save last ignition off time and save accumulated ignition-hours
                this.setLastIgnitionOffTime(eventTime);         // FLD_lastIgnitionOffTime
                this.setLastIgnitionHours(ignHours);            // FLD_lastIgnitionHours
            }
            // -- clear ignition on
            if (eventTime < this.getLastIgnitionOnTime()) { // unlikely
                // -- make sure lastIgnitionOnTime reflects Ignition-OFF
                Print.logWarn("Event time is prior to last Ignition-ON! " + acctID + "/" + devID);
            }
            this.setLastIgnitionOnTime(0L);                     // FLD_lastIgnitionOnTime   [2.5.8-B37]
            this.setLastIgnitionOnHours(0.0);                   // FLD_lastIgnitionOnHours  [2.5.8-B37]
        } else {
            // -- Ignition state has not changed
        }

        /* battery level */
        {
            double battLevel = evdb.getBatteryLevel();
            if (battLevel < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (battLevel == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastBatteryLevel(battLevel);   // FLD_lastBatteryLevel
            }
        }

        /* battery volts */
        {
            double battVolts = evdb.getBatteryVolts();
            if (battVolts < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (battVolts == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastBatteryVolts(battVolts);   // FLD_lastBatteryVolts
            }
        }

        /* vehicle battery volts */
        {
            double vBattVolts = evdb.getVBatteryVolts();
            if (vBattVolts < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (vBattVolts == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastVBatteryVolts(vBattVolts);   // FLD_lastVBatteryVolts
            }
        }

        /* fuel level (#1,#2,...) */
        for (Device.FuelTankIndex T : Device.FuelTankIndex.values()) {
            if (!T.isTank()) { continue; }
            double fuelLevel = evdb.getFuelLevel(T,true/*estimate*/);
            if (fuelLevel < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (fuelLevel == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastFuelLevel(T,fuelLevel); // FLD_lastFuelLevel
            }
        }

        /* oil level */
        {
            double oilLevel = evdb.getOilLevel();
            if (oilLevel < 0.0) {
                // -- skip (not provided by DCS)
            } else
            if (oilLevel == 0.0) {
                // -- TODO: decide whether or not to skip?
            } else {
                this.setLastOilLevel(oilLevel);           // FLD_lastOilLevel
            }
        }

        /* driver ID/Status */
        if (saveDriverID) {
            this.setDriverID(driverID);                   // FLD_driverID
            if (saveDriverStatus) {
                this.setDriverStatus(driverStatus);       // FLD_driverStatus
            }
            try {
                Driver.updateDriverStatus(account, driverID, driverStatus, this.getDeviceID());
            } catch (DBException dbe) {
                Print.logError("Driver status save failed: " + dbe);
            }
        }

        /* expects an acknowledgement? */
        if (this.isAckStatusCode(statusCode)) {
            String ad = this.getAccountID() + "/" + this.getDeviceID();
            Print.logInfo("ACK status code match [" + ad + "]: " + StatusCodes.GetHex(statusCode));
            this.clearExpectCommandAck(true/*didAck*/,false/*update*/);
        }

        // TODO: GPIO? "lastInputState"  (must currently be set by DCS)
        // TODO: GPIO? "lastOutputState" (must currently be set by DCS)

        /* callback: after event has been inserted */
        // -- reverse-geocoding, etc, may still be deferred.
        // -- post-adjustments to Device record have been completed.
        this.eventDidInsert(evdb);

        /* return success */
        return true;
        // try .. catch

    } // _insertEventData

    /**
    *** Post EventData record insertion processing
    *** @param evdb  The EventData instance
    *** @param extUpdate  The mask indicating post processing to perform
    **/
    private void _postEventInsertionProcessing(EventData evdb, int extUpdate, boolean deferRuleCheck)
    {
        Set<String> updatedEvFields = null;

        /* cell tower GPS location */
        if ((extUpdate & EXT_UPDATE_CELLGPS) != 0) {
            Set<String> updf = evdb.updateCellTowerLocation();
            if (updf != null) {
                // -- MobileLocation was successful (but may not have returned a valid location)
                if (updatedEvFields == null) { updatedEvFields = new HashSet<String>(); }
                updatedEvFields.addAll(updf);
                CellTower sct = evdb.getServingCellTower();
                if (sct != null) {
                    // -- update Device lastCellServingInfo
                    this.setLastServingCellTower(sct); // FLD_lastCellServingInfo
                    try {
                        this.update(Device.FLD_lastCellServingInfo);
                    } catch (DBException dbe) {
                        Print.logError("Unable to update Device: " + dbe);
                    }
                    // -- we've updated the cell-tower location, also update address
                    if (Device.allowSlowReverseGeocode) {
                        extUpdate |= EXT_UPDATE_ADDRESS;
                    }
                }
            }
        }

        /* address */
        if ((extUpdate & EXT_UPDATE_ADDRESS) != 0) {
            try {
                Set<String> updf = evdb.updateAddress(false/*!fastOnly*/);
                if (updf != null) {
                    if (updatedEvFields == null) { updatedEvFields = new HashSet<String>(); }
                    updatedEvFields.addAll(updf);
                    // -- update Device "lastSubdivision"
                    String subDiv = evdb.getSubdivision();
                    if (!StringTools.isBlank(subDiv)) {
                        this.setLastSubdivision(subDiv); // FLD_lastSubdivision
                        try {
                            this.update(Device.FLD_lastSubdivision);
                        } catch (DBException dbe) {
                            Print.logError("Unable to update Device: " + dbe);
                        }
                    }
                }
            } catch (SlowOperationException soe) {
                // -- this will not occur ('fastOnly' is false)
            }
        }

        /* stateline border-crossing check here */
        // -- check border-crossing in nightly cron

        /* update */
        if (!ListTools.isEmpty(updatedEvFields)) {
            try {
                // -- Note: address may not be written to the table correctly if the
                // -  current character encoding is not "UTF-8"!
                evdb.update(updatedEvFields);
                Print.logInfo("EventData address: [%s/%s] %s: %s",
                    this.getAccountID(), this.getDeviceID(),
                    evdb.getGeoPoint().toString(), evdb.getAddress());
            } catch (DBException dbe) {
                Print.logError("EventData update error: " + dbe);
            }
        }

        /* rule check (in "_postEventInsertionProcessing") */
        // -- Cannot generally defer rule check to here without proper initialization!
        // -  Rule triggers may be based on values which may be changing in the Device record,
        // -  which will have already changed by the time we get here!
        // -  (this.getDeferRuleCheckToPostInsert()==true) assumes that "evdb.getDevice()" is in
        // -  the same unchanged state it was just prior to receiving the event.
        if (deferRuleCheck) { // this.getDeferRuleCheckToPostInsert()
            // -- rule check deferred until now
            if (this.checkEventRules(evdb)) {
                // -- Fields may have changed: (NOTE: automatically updated)
                // -   FLD_lastNotifyTime
                // -   FLD_lastNotifyCode
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Post command handling 
    *** (called by "DCServerConfig.Command.setDeviceCommandAttributes" after a successful command)
    *** @param command  The executed DCServerConfig.Command 
    *** @param cmdStr   The command string sent to the device
    *** @param update   True to update the Device record now.
    *** @return True if there are changed fields.
    **/
    public boolean postCommandHandling(DCServerConfig.Command command, String cmdStr, boolean update)
    {

        /* command valid? */
        if (command == null) {
            return false;
        }

        /* expect return acknowledgement */
        if (!command.getExpectAck()) {
            // continue
        } else
        if (this.getExpectingCommandAck()) {
            Print.logWarn("Already expecting an ACK for: " + this.getLastAckCommand());
        } else {
            int ackCode = command.getExpectAckCode();
            String cs = !StringTools.isBlank(cmdStr)? 
                StringTools.trim(cmdStr) : 
                command.getCommandString(this,null);
            this.setExpectAck(true);
            this.setExpectAckCode(ackCode);
            this.setLastAckCommand(cmdStr);
            //this.setLastAckResponse(null);
            this.setLastAckTime(0L);
            this.addOtherChangedFieldNames(
                FLD_expectAck, 
                FLD_expectAckCode, 
                FLD_lastAckCommand, 
              //FLD_lastAckResponse, 
                FLD_lastAckTime);
        }

        /* command state bit? */
        if (command.hasStateBitMask()) {
            long    bitMask = command.getStateBitMask();
            boolean bitVal  = command.getStateBitValue();
            this._setCommandStateMask(bitMask, bitVal); 
            // FLD_commandStateMask
        }

        /* audit event? */
        if (command.hasAuditStatusCode()) {
            int       sc      = command.getAuditStatusCode();
            String    acctID  = this.getAccountID();
            String    devID   = this.getDeviceID();
            long      fixtime = DateTime.getCurrentTimeSec(); // now
            EventData.Key evk = new EventData.Key(acctID, devID, fixtime, sc);
            EventData evd     = evk.getDBRecord();
            this.insertEventData(evd);
            // FLD_??
        }

        /* update changed? */
        if (this.hasChangedFieldNames()) {
            if (update) {
                try {
                    this.updateOtherChangedEventFields();
                } catch (DBException dbe) {
                    Print.logException("Unable to update Device", dbe);
                }
            }
            return true;
        } else {
            return false;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets a list of authorised Users for this Device
    *** @param device The device for which the User list will be returned.
    *** @return The list of authorized users
    **/
    public static OrderedSet<User> getAuthorizedUsers(Device device)
        throws DBException
    {
        OrderedSet<User> userList = new OrderedSet<User>();

        /* no Device? */
        if (device == null) {
            return userList; // empty
        }
        String deviceID = device.getDeviceID();

        /* get account */
        Account account = device.getAccount();
        if (account == null) {
            return userList; // empty
        }
        String accountID = device.getAccountID();

        /* Get list of all users */
        String userIDs[] = User.getUsersForAccount(accountID);
        if (ListTools.isEmpty(userIDs)) {
            return userList; // empty
        }

        /* save all authorized users */
        for (String userID : userIDs) {
            try {
                User user = User.getUser(account, userID);
                if (user.isAuthorizedDevice(deviceID)) {
                    userList.add(user);
                }
            } catch (DBException dbe) {
                // ignore
            }
        }

        /* return list */
        return userList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Deletes old events from EventData table.
    *** @param priorToTime  EventData records up to (but excluding) this timestamp will be deleted.
    *** @return The number of records deleted
    **/
    public long deleteEventsPriorTo(long priorToTime)
        throws DBException
    {

        /* valid timestamp? */
        if (priorToTime <= 0L) {
            throw new DBException("Invalid 'priorTo' timestamp specified: " + priorToTime);
        }

        /* starting event count */
        long delEventCount = this.getEventCount(-1L, priorToTime - 1L); // -1 for InnoDB

        /* delete all EventData entries prior to the specified date */
        // [DELETE FROM EventData WHERE accountID='account' and deviceID='device' and timestamp<priorToTime]
        DBConnection dbc = null;
        try {
            DBDelete edel = new DBDelete(EventData.getFactory());
            DBWhere  ewh  = edel.createDBWhere();
            edel.setWhere(ewh.WHERE_(
                ewh.AND(
                    ewh.EQ(EventData.FLD_accountID, this.getAccountID()),
                    ewh.EQ(EventData.FLD_deviceID , this.getDeviceID()),
                    ewh.LT(EventData.FLD_timestamp, priorToTime)
                )
            ));
            Print.logInfo("EventData delete command: " + edel);
            dbc = DBConnection.getDBConnection_delete();
            dbc.executeUpdate(edel.toString());
        } catch (SQLException sqe) {
            throw new DBException("Deleting EventData records", sqe);
        } finally {
            DBConnection.release(dbc);
        }

        /* number of records deleted (or supposed to have been deleted) */
        return delEventCount; // -1 for InnoDB

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String[] DefaultUpdatedFieldsList = new String[] {
        Device.FLD_deviceCode,              // serverID
        Device.FLD_imeiNumber,
        Device.FLD_lastTcpSessionID,
        Device.FLD_ipAddressCurrent,
        Device.FLD_remotePortCurrent,
        Device.FLD_listenPortCurrent,
        Device.FLD_lastInputState,
        Device.FLD_lastBatteryLevel,
        Device.FLD_lastBatteryVolts,
        Device.FLD_lastVBatteryVolts,
        Device.FLD_lastFuelLevel,           
        Device.FLD_lastFuelLevel2,           
        Device.FLD_lastFuelTotal,
        Device.FLD_lastOilLevel,           
        Device.FLD_lastValidLatitude,
        Device.FLD_lastValidLongitude,
        Device.FLD_lastValidSpeedKPH,
        Device.FLD_lastValidHeading,
        Device.FLD_lastGPSTimestamp,
        Device.FLD_lastEventTimestamp,
        Device.FLD_lastEventStatusCode,
        Device.FLD_lastMalfunctionLamp,
        Device.FLD_lastFaultCode,
        Device.FLD_lastOdometerKM,
        Device.FLD_lastDistanceKM,
        Device.FLD_lastEngineOnHours,
        Device.FLD_lastEngineOnTime,
        Device.FLD_lastEngineOffTime,
        Device.FLD_lastEngineHours,
        Device.FLD_lastPtoOnHours,
        Device.FLD_lastPtoOnTime,
        Device.FLD_lastPtoOffTime,
        Device.FLD_lastPtoHours,
        Device.FLD_lastIgnitionOnHours,
        Device.FLD_lastIgnitionOnTime,
        Device.FLD_lastIgnitionOffTime,
        Device.FLD_lastIgnitionHours,
        Device.FLD_lastStopTime,
        Device.FLD_lastStartTime,
        Device.FLD_lastTotalConnectTime,
      //Device.FLD_driverID,                // CalAmp, ...
      //Device.FLD_lastNotifyTime,          // optional field (should only be updated if changed)
      //Device.FLD_lastNotifyCode,          // optional field (should only be updated if changed)
      //Device.FLD_activeCorridor           // optional field (should only be updated if changed)
    };
    
    private static final Set<String> DefaultUpdatedFieldsSet = 
        ListTools.toSet(DefaultUpdatedFieldsList);

    /**
    *** Creates a list of fields that should be updated for this device
    *** @param flds  The pre-initialized list of fields to update
    *** @return The final set of fields to update
    **/
    public Set<String> _createChangedFieldsSet(Set<String> flds)
        throws DBException
    {
        Set<String> otherSet = this.getOtherChangedFieldNames();
        if ((flds == null) && (otherSet == null)) {
            return DefaultUpdatedFieldsSet; // minor optimization
        } else {
            // make a new copy and add the additional fields
            Set<String> updFields = ListTools.toSet(DefaultUpdatedFieldsList);
            if (flds != null) {
                ListTools.toSet(flds/*from*/, updFields/*to*/);
            }
            if (otherSet != null) {
                ListTools.toSet(otherSet/*from*/, updFields/*to*/);
            }
            return updFields;
        }
    }

    /**
    *** Creates a list of fields that should be updated for this device
    *** @param flds  The pre-initialized list of fields to update
    *** @return The final set of fields to update
    **/
    public Set<String> _createChangedFieldsSet(String... flds)
        throws DBException
    {
        Set<String> otherSet = this.getOtherChangedFieldNames();
        if ((flds == null) && (otherSet == null)) {
            return DefaultUpdatedFieldsSet; // minor optimization
        } else {
            Set<String> updFields = ListTools.toSet(DefaultUpdatedFieldsList);
            if (flds != null) {
                ListTools.toSet(flds, updFields);
            }
            if (otherSet != null) {
                ListTools.toSet(otherSet, updFields);
            }
            return updFields;
        }
    }

    // --------------------------------

    private Set<String> otherChangedFieldsSet = null;

    /**
    *** Creates/returns the set that will contains fields to update
    *** @return The set of fields to update
    **/
    private Set<String> _createOtherChangedFieldsSet()
    {
        if (this.otherChangedFieldsSet == null) {
            this.otherChangedFieldsSet = new HashSet<String>();
        }
        return this.otherChangedFieldsSet;
    }

    /**
    *** Clears the set that will contains fields to update
    **/
    private void _clearOtherChangedFieldsSet()
    {
        this.otherChangedFieldsSet = null;
    }

    /**
    *** clear all fields specified in the internal update field set
    **/
    public void clearOtherChangedFieldNames()
    {
        this._clearOtherChangedFieldsSet();
    }

    /**
    *** Returns true if the changed field set has been initialized
    *** @return True if the changed field set has been initialized
    **/
    public boolean hasChangedFieldNames()
    {
        return (this.otherChangedFieldsSet != null);
    }

    /**
    *** Returns the set of fields to update (may be null)
    *** @return The set of fields to update (may be null)
    **/
    public Set<String> getOtherChangedFieldNames()
    {
        return this.otherChangedFieldsSet; // may be null
    }

    /**
    *** Adds the specified list of update fields to the internal set
    *** @param flds The list of fields to add
    **/
    public void addOtherChangedFieldNames(Set<String> flds)
    {
        if (flds != null) {
            ListTools.toSet(flds, this._createOtherChangedFieldsSet());
        }
    }

    /**
    *** Adds the specified list of update fields to the internal set
    *** @param flds The list of fields to add
    **/
    public void addOtherChangedFieldNames(String... flds)
    {
        if (flds != null) {
            ListTools.toSet(flds, this._createOtherChangedFieldsSet());
        }
    }

    /**
    *** Updates all fields specified in the internal update field set
    **/
    public void updateOtherChangedEventFields()
        throws DBException
    {
        Set<String> updSet = this.getOtherChangedFieldNames();
        if (updSet != null) {
            this.update(updSet);
            this._clearOtherChangedFieldsSet();
        }
    }

    // --------------------------------

    /**
    *** Updates all fields specified in the internal changed field set
    **/
    public void updateChangedEventFields()
        throws DBException
    {
        this.update(_createChangedFieldsSet((String[])null));
        this._postDeviceEventUpdate();
    }

    /**
    *** Updates the specified changed fields
    *** @param flds The field set to update
    **/
    public void updateChangedEventFields(Set<String> flds)
        throws DBException
    {
        this.update(_createChangedFieldsSet(flds));
        this._postDeviceEventUpdate();
    }

    /**
    *** Updates the specified changed fields
    *** @param flds The field set to update
    **/
    public void updateChangedEventFields(String... flds)
        throws DBException
    {
        this.update(_createChangedFieldsSet(flds));
        this._postDeviceEventUpdate();
    }

    // --------------------------------

    /**
    *** Post EventData record insertion processing
    *** This method is called when the device fields have been updated after having 
    *** received an event and the "lastXXXX" fields have been changed.
    **/
    private void _postDeviceEventUpdate()
    {

        /* ELog/HOS */
        if (Account.hasELogHOSProvider() && Device.IsELogEnabled(this)) {
            // -- add ELog/HOS job to thread
            ELogHOSProvider elp = Account.getELogHOSProvider();
            int stat = elp.processELogEvents(this,-1L);
            if (stat >= ELogHOSProvider.ERR_ERROR) {
                Print.logWarn("ELog/HOS queue status error: " + stat);
            }
        }

        /* DataPush */
        if (Account.hasDataPushProvider()) {
            // -- add DataPush job to thread
            DataPushProvider dpp = Account.getDataPushProvider();
            int stat = dpp.processPushEvents(this);
            if (stat >= DataPushProvider.ERR_ERROR) {
                Print.logWarn("DataPush queue status error: " + stat);
            }
        }

    }

    /**
    *** Post EventData record insertion processing
    **/
    //public void postDeviceEventUpdate()
    //{
    //    this._postDeviceEventUpdate();
    //}

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Insert connection session statistics into the SessionStats table
    *** @param startTime  The time of the session stat
    *** @param ipAddr     The IP address
    *** @param isDuplex   True if TCP/Duplex
    *** @param bytesRead  The number of bytes read
    *** @param bytesWritten  The number of bytes written
    *** @param evtsRecv   The number of events received
    **/
    public void insertSessionStatistic(long startTime, String ipAddr, boolean isDuplex, long bytesRead, long bytesWritten, long evtsRecv)
    {
        // save session statistics
        SessionStatsFactory csf = Device.getSessionStatsFactory();
        if (csf != null) {
            try {
                csf.addSessionStatistic(this,startTime,ipAddr,isDuplex,bytesRead,bytesWritten,evtsRecv);
            } catch (DBException dbe) {
                Print.logError("Session statistic: " + dbe);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Validates the syntax of the specified Rule selector
    *** @param selector  The rule selector to check
    *** @return True if the rule selector is valid
    **/
    public static boolean CheckSelectorSyntax(String selector)
    {
        if (StringTools.isBlank(selector)) {
            // a blank selector should always be valid
            return true;
        } else {
            RuleFactory ruleFact = Device.getRuleFactory();
            if (ruleFact != null) {
                return ruleFact.checkSelectorSyntax(selector);
            } else {
                Print.logWarn("No RuleFactory defined");
                return false;
            }
        }
    }

    /**
    *** Checks the rules which are applicable for the specified event
    *** Note: The EventData instance MUST already be set with the parent Device record!
    *** @param event The EventData instance to check
    **/
    protected boolean checkEventRules(EventData event)
    {
        Account account = this.getAccount();
        //Print.logInfo("Checking Rules ...");

        /* no event? */
        if (event == null) {
            // -- we have no event, don't bother with the rest
            //Print.logDebug("No EventData record specified: " + this.getAccountID() + "/" + this.getDeviceID());
            return false;
        }
        int statusCode = event.getStatusCode();

        /* skip rule checks for rule-trigger events */
        // -- to prevent any potential for recursive rule triggers and event insertion
        if (StatusCodes.IsRuleTrigger(statusCode)) {
            Print.logInfo("Skipping recursive rule check for rule-trigger event ...");
            return false;
        }

        /* synthesized events */
        boolean isSynthesizedEvent = event.getIsSynthesizedEvent();
        //if (isSynthesizedEvent) {
        //    return false;
        //}

        /* set device */
        // -- This provides an optimization so that any Account/Device requests on the EventData
        // -  record won't have to explicitly query the database to retrieve the Account/Device.
        //event.setDevice(this); <== [2.5.9-B23] must be set prior to calling "checkEventRules(...)"

        /* Entity attach/detach (if installed) */
        if (!isSynthesizedEvent && Device.hasEntityManager()) {
            Device.getEntityManager().insertEntityChange(event);
        }

        /* Fuel Manager (if installed) */
        if (!isSynthesizedEvent && Device.hasFuelManager()) {
            FuelManager fm = Device.getFuelManager();
            FuelManager.LevelChangeType lvlType = fm.insertFuelLevelChange(event);
            switch (lvlType) {
                case INCREASE: // refill
                    if (statusCode != StatusCodes.STATUS_FUEL_REFILL) {
                        // -- insert synthesized fuel-level change event
                        int fuelCode = StatusCodes.STATUS_FUEL_REFILL;
                        EventData fuelEv = EventData.copySynthesizedEvent(event, fuelCode);
                        if (this.insertEventData(fuelEv)) { // recursive call
                            Print.logWarn("FuelManager: Added new Fuel 'REFILL' Event - " + fuelEv);
                        } else {
                            Print.logError("FuelManager: New Fuel 'REFILL' Event failed!");
                        }
                    }
                    break;
                case DECREASE: // theft
                    if (statusCode != StatusCodes.STATUS_FUEL_THEFT) {
                        // -- insert synthesized fuel-level change event
                        int fuelCode = StatusCodes.STATUS_FUEL_THEFT;
                        EventData fuelEv = EventData.copySynthesizedEvent(event, fuelCode);
                        if (this.insertEventData(fuelEv)) { // recursive call
                            Print.logWarn("FuelManager: Added new Fuel 'THEFT' Event - " + fuelEv);
                        } else {
                            Print.logError("FuelManager: New Fuel 'THEFT' Event failed!");
                        }
                    }
                    break;
                case NONE: // no change
                    // -- no change
                    break;
                case UNKNOWN: // fuel-level not availabl
                    // -- unknown (fuel-level not available?)
                    break;
            }
        }

        /* notification not allowed for this device? */
        boolean allowNotify = this.getAllowNotify(true);
        if (!allowNotify) {
            /* display message if a rule-selector has been specified */
            Print.logInfo("Notification disallowed for this Account/Device: " + this);
            //if (!StringTools.isBlank(ruleSelector)) {
            //    Print.logWarn("Notification disallowed [selector = " + ruleSelector + "] " + this);
            //} else {
            //    //Print.logDebug("Notification disallowed: " + this);
            //}
            return false;
        }

        /* device rule selector (null if ENRE is present) */
        String ruleSelector = Device.CheckNotifySelector()? this.getNotifySelector() : null;

        /* check for rule factory */
        RuleFactory ruleFact = Device.getRuleFactory();
        if (ruleFact == null) {
            /* display message if a rule-selector has been specified */
            //if (!StringTools.isBlank(ruleSelector)) {
            //    Print.logWarn("No RuleFactory to process rule: " + ruleSelector);
            //} else {
            //    //Print.logDebug("RuleFactory not installed: " + this);
            //}
            return false;
        }

        /* accumulated action mask */
        int accumActionMask = RuleFactory.ACTION_NONE;
        boolean didTrigger = false;

        /* check local email notification selector */
        // -- This executes a single selector-based rule.
        if (!StringTools.isBlank(ruleSelector)) {
            Print.logDebug("Processing Device rule [selector = " + ruleSelector + "] " + this);
            int actionMask = ruleFact.executeSelector(ruleSelector, event);
            if (this._setDeviceAction(actionMask, event, null/*Rule*/)) {
                didTrigger = true;
                accumActionMask |= actionMask;
            }
        }

        /* ExecuteRules: test statusCode rule/action list */
        // -- This method allows for a complete check of multiple rules
        {
            //Print.logDebug("Executing rules for event: " + this);
            int actionMask = ruleFact.executeRules(event);
            if (this._setDeviceAction(actionMask, event, null/*Rule*/)) {
                didTrigger = true;
                accumActionMask |= actionMask;
            }
        }

        /* Geozone AutoNotify: convenience geozone arrive/depart notification */
        if (StatusCodes.IsGeozoneTransition(statusCode)) {
            BasicPrivateLabel bpl = Account.getPrivateLabel(account);
            Geozone zone = event.getGeozone();
            if (account == null) {
                // -- very unlikely
                Print.logError("Unable to determine account for Geozone email: " +
                    this.getAccountID() + "/" + this.getDeviceID() +
                    " (zone: " + event.getGeozoneID() + ")");
            } else
            if (bpl == null) {
                // -- unlikely
                Print.logWarn("Unable to determine Account PrivateLabel for Geozone email: " + 
                    this.getAccountID() + "/" + this.getDeviceID() +
                    " (zone: " + event.getGeozoneID() + ")");
            } else
            if (zone == null) {
                // -- unlikely (DCS is supposed to populate a valid value)
                Print.logWarn("Geozone status code, but Geozone not found: " + 
                    this.getAccountID() + "/" + this.getDeviceID() +
                    " (zone: " + event.getGeozoneID() + ")");
            } else
            if (zone.getAutoNotify()) {
                // -- IE: "VehicleName" arrived "DeviceDescription"
                I18N i18n = I18N.getI18N(Device.class, account.getLocale());

                /* time format */
                String timeFmt  = bpl.getDateFormat() + " " + bpl.getTimeFormat();
                //TimeZone tmz  = this.getTimeZone(null);
                TimeZone tmz    = Account.getTimeZone(account,DateTime.getGMTTimeZone()); 
                String timeStr  = new DateTime(event.getTimestamp()).format(timeFmt,tmz);

                /* device/geozone description */
                String devDesc  = this.getDescription();
                String zoneDesc = zone.getDescription();

                /* message subject/body */
                String subj = null;
                String body = null;
                if (statusCode == StatusCodes.STATUS_GEOFENCE_ARRIVE) {
                    subj = i18n.getString("Device.autoArriveMessage", "{0}: \"{1}\" arrived \"{2}\"",
                        new String[] { timeStr, devDesc, zoneDesc });
                    body = subj;
                } else
                if (statusCode == StatusCodes.STATUS_GEOFENCE_DEPART) {
                    subj = i18n.getString("Device.autoDepartMessage", "{0}: \"{1}\" departed \"{2}\"",
                        new String[] { timeStr, devDesc, zoneDesc });
                    body = subj;
                } else {
                    // -- unlikely
                    subj = i18n.getString("Device.autoGeozoneMessage", "{0}: \"{1}\" arrived/departed \"{2}\"",
                        new String[] { timeStr, devDesc, zoneDesc });
                    body = subj;
                }

                /* assemble auto-notify recipient list */
                Recipients recipients = new Recipients();
                recipients.addRecipients(account.getNotifyEmail());
                recipients.addRecipients(this.getNotifyEmail(false/*inclAcct*/,true/*inclUser*/,true/*inclGroup*/));
                String toSMS   = recipients.getSmsRecipientsString();
                String toEmail = recipients.getEmailRecipientsString();

                /* Send email */
                SmtpProperties smtpProps = account.getSmtpProperties(bpl);
                String frEmail = smtpProps.getFromEmailType("notify");
                if (StringTools.isBlank(toEmail)) {
                    Print.logInfo("No email recipients, skipping email ...");
                } else
                if (StringTools.isBlank(frEmail)) {
                    Print.logWarn("No 'From:' email address, skipping email ...");
                } else {
                    Print.logInfo("From     : "  + frEmail);
                    Print.logInfo("To(email): "  + toEmail);
                    Print.logInfo("Subject  : "  + subj);
                    Print.logInfo("Body     :\n" + body);
                    try {
                        Print.logInfo("Sending Geozone auto notify email ...");
                        boolean retry = false;
                        SendMail.send(frEmail,toEmail,null,null,subj,body,null,smtpProps,retry);
                    } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                        // -- this will fail if JavaMail support for SendMail is not available.
                        Print.logWarn("SendMail error: " + t);
                    }
                }

                /* send SMS */
                if (StringTools.isBlank(toSMS)) {
                    // -- no SMS destinations
                } else
                if (account.getSmsEnabled()) {
                    SMSProperties smsProps = account.getSmsProperties(null);
                    // --
                    String smsMsg = subj;
                    Print.logInfo("To(SMS): " + toSMS);
                    Print.logInfo("Message: " + smsMsg);
                    // -- SMS gateway
                    String    smsGatewayName = smsProps.getGatewayName();
                    SMSOutboundGateway smsGW = SMSOutboundGateway.GetSMSGateway(smsGatewayName);
                    if (smsGW != null) {
                        Print.logInfo("Sending SMS via gateway: " + smsGatewayName);
                        // -- list of SMS recipients
                        String smsPhoneList[] = StringTools.split(toSMS,',');
                        for (String smsPhone : smsPhoneList) {
                            smsPhone = SMSOutboundGateway.RemovePrefixSMS(smsPhone);
                            if (!StringTools.isBlank(smsPhone)) {
                                Print.logInfo("SMS: " + smsPhone + " --> " + smsMsg);
                                DCServerFactory.ResultCode result = smsGW.sendSMSMessage(account, this, smsMsg, smsPhone);
                                if (!result.isSuccess()) {
                                    Print.logWarn("SMS error: " + result);
                                }
                            }
                        }
                    } else {
                        Print.logWarn("SMS Gateway not found: " + smsGatewayName);
                    }
                } else {
                    Print.logWarn("SMS notification disabled for account: " + account.getAccountID());
                }

            }
        }

        /* return trigger state */
        return didTrigger;

    }

    /**
    *** Saves the specified Device action for this Device
    *** @param actionMask  The Action mask
    *** @param event       The EventData instance
    *** @param ruleID      The triggered rule-id
    *** @return True if saved
    **/
    private boolean _setDeviceAction(int actionMask, EventData event, String ruleID)
    {

        /* no action? */
        if ((actionMask < 0) || (actionMask == RuleFactory.ACTION_NONE)) {
            return false;
        }

        /* save last triggered notification */
        if ((actionMask & RuleFactory.ACTION_SAVE_LAST) != 0) {
            try {
                this.setLastNotifyEvent(event, ruleID, false/*update*/);
            } catch (DBException dbe) {
                // -- we are not updating, so this will not occur
            }
        } else {
            // -- "lastNotifyTime" and "lastNotifyCode" should be left as-is
            // -  NOTE: An external DB trigger may have changed these values, and updating
            // -  them may end up resetting these back to '0'
        }

        /* disable active corridor */
        /*
        if ((actionMask & RuleFactory.ACTION_DISABLE_CORRIDOR) != 0) {
            if (this.hasActiveCorridor()) {
                this.setActiveCorridor("");                     // FLD_activeCorridor
            } else {
                // -- no active corridor
            }
        }
        */

        /* enable new corridor */
        /*
        if ((actionMask & RuleFactory.ACTION_ENABLE_CORRIDOR) != 0) {
            Geozone zone = event.getGeozone();
            if ((zone != null) && zone.hasCorridorID()) {
                this.setActiveCorridor(zone.getCorridorID());   // FLD_activeCorridor
            } else {
                // -- leave as-is
            }
        }
        */

        // changes not yet saved
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Optimization for StatusCode description lookup (typically for map display)
    // This is a temporary cache of StatusCodes that are used for Events which
    // are either displayed on a map, or in a report.  Access to this cache does not need
    // to be synchronized since all status code lookups will occur within the same thread.
    // This cache is temporary and is garbage collected along with this Device record.

    private Map<Integer,StatusCode> cacheStatusCodeMap = null;

    /** 
    *** Gets the StatusCode instance for the specified code
    *** @param code  The numeric status code value
    *** @return The StatusCode instance
    **/
    public StatusCode getStatusCode(int code)
    {

        /* create map */
        if (this.cacheStatusCodeMap == null) {
            this.cacheStatusCodeMap = new HashMap<Integer,StatusCode>();
        }

        /* already in cache */
        Integer codeKey = new Integer(code);
        if (this.cacheStatusCodeMap.containsKey(codeKey)) {
            return this.cacheStatusCodeMap.get(codeKey); // may return null;
        }

        /* add to cache */
        String accountID = this.getAccountID();
        String deviceID  = this.getDeviceID();
        StatusCode sc = StatusCode.findStatusCode(accountID, deviceID, code);
        this.cacheStatusCodeMap.put(new Integer(code), sc);
        return sc; // may be null;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets and array of the attached Entity IDs which are attached to this Device
    *** @param etype  The EntityType
    *** @return  The array of Entity IDs
    **/
    public String[] getAttachedEntityIDs(EntityManager.EntityType etype)
    {
        EntityManager.EntityType et = EntityManager.getEntityType(etype);
        return this.getAttachedEntityIDs(et.getIntValue());
    }

    /**
    *** Gets and array of the attached Entity IDs which are attached to this Device
    *** @param entityType  The EntityType
    *** @return  The array of Entity IDs
    **/
    public String[] getAttachedEntityIDs(int entityType)
    {
        if (Device.hasEntityManager()) {
            String attEnt[] = null;
            try {
                String acctID = this.getAccountID();
                String devID  = this.getDeviceID();
                attEnt = Device.getEntityManager().getAttachedEntityIDs(acctID, devID, entityType);
            } catch (DBException dbe) {
                Print.logException("Error reading Device Entities", dbe);
            }
            return attEnt;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets and array of the attached Entity Descriptions which are attached to this Device
    *** @param etype  The EntityType
    *** @return  The array of Entity IDs
    **/
    public String[] getAttachedEntityDescriptions(EntityManager.EntityType etype)
    {
        EntityManager.EntityType et = EntityManager.getEntityType(etype);
        return this.getAttachedEntityDescriptions(et.getIntValue());
    }

    /**
    *** Gets and array of the attached Entity Descriptions which are attached to this Device
    *** @param entityType  The EntityType
    *** @return  The array of Entity IDs
    **/
    public String[] getAttachedEntityDescriptions(int entityType)
    {
        if (Device.hasEntityManager()) {
            String attEnt[] = null;
            try {
                String acctID = this.getAccountID();
                String devID  = this.getDeviceID();
                attEnt = Device.getEntityManager().getAttachedEntityDescriptions(acctID, devID, entityType);
            } catch (DBException dbe) {
                Print.logException("Error reading Device Entities", dbe);
            }
            return attEnt;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private EventData   cachedRangeEvents[] = null;

    /**
    *** Gets the saved list of cached events
    *** @return The list of events to cache
    **/
    public EventData[] getSavedRangeEvents()
    {
        return this.cachedRangeEvents;
    }

    /**
    *** Sets the saved list of cached events
    *** @param events The list of events to cache
    **/
    public void setSavedRangeEvents(EventData events[])
    {
        this.cachedRangeEvents = events;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    private static boolean STOPPED_IN_RANGE_IGNITION_OFF = true;

    public static class StoppedSeconds
    {
        private long        timeStart           = 0L;
        private long        timeEnd             = 0L;
        private long        minStopElapsedSec   = 0L;
        private long        minExpectedStopSec  = 0L;
        private long        totalStoppedSec     = 0L; // calculated
        private I18N.Text   description         = null;
        public StoppedSeconds(I18N.Text desc, long timeS, long timeE, long minStopSec, long expStopSec) {
            this.description        = desc;
            this.timeStart          = timeS;
            this.timeEnd            = timeE;
            this.minStopElapsedSec  = minStopSec;
            this.minExpectedStopSec = expStopSec;
            this.clearTotalStoppedSeconds();
        }
        public StoppedSeconds(long timeS, long timeE, long minStopSec, long expStopSec) {
            this(null, timeS, timeE, minStopSec, expStopSec);
        }
        public StoppedSeconds(I18N.Text desc, long timeS, long timeE, long minStopSec) {
            this(desc, timeS, timeE, minStopSec, 0L);
        }
        public StoppedSeconds(long timeS, long timeE, long minStopSec) {
            this(null, timeS, timeE, minStopSec, 0L);
        }
        // --
        public I18N.Text getDescription_I18N(Locale locale) {
            if (this.description != null) {
                return this.description;
            } else {
                return new I18N.Text(this.getDescription(locale));
            }
        }
        public String getDescription(Locale locale) {
            if (this.description != null) {
                // -- pre-defined description
                return this.description.toString(locale);
            } else {
                // -- i18n text
                // -  eg. "24-hr window, 30-min minimum stop, 9-hr expected total."
                I18N   i18n       = I18N.getI18N(Device.class, locale);
                String textHour   = i18n.getString("Device.StoppedSeconds.hour"  ,"hr" );
                String textMinute = i18n.getString("Device.StoppedSeconds.minute","min");
                String textSecond = i18n.getString("Device.StoppedSeconds.second","sec");
                // -- window
                String windowDesc = null;
                {
                    long deltaTimeRangeSec = this.timeEnd - this.timeStart;
                    if ((deltaTimeRangeSec % 3600L) == 0L) {
                        // -- 1-hour boundary
                        windowDesc = (deltaTimeRangeSec / 3600L) + "-" + textHour;
                    } else
                    if ((deltaTimeRangeSec % 60L) == 0L) {
                        // -- 1-minute boundary
                        windowDesc = (deltaTimeRangeSec / 60L) + "-" + textMinute;
                    } else {
                        // -- 1-second boundary
                        windowDesc = (deltaTimeRangeSec) + "-" + textSecond;
                    }
                }
                // -- minimum stop
                String minStopDesc = null;
                {
                    if ((this.minStopElapsedSec % 3600L) == 0L) {
                        // -- 1-hour boundary
                        minStopDesc = (this.minStopElapsedSec / 3600L) + "-" + textHour;
                    } else
                    if ((this.minStopElapsedSec % 60L) == 0L) {
                        // -- 1-minute boundary
                        minStopDesc = (this.minStopElapsedSec / 60L) + "-" + textMinute;
                    } else {
                        // -- 1-second boundary
                        minStopDesc = (this.minStopElapsedSec) + "-" + textSecond;
                    }
                }
                // -- expected value
                String expValDesc = null;
                {
                    if ((this.minExpectedStopSec % 3600L) == 0L) {
                        // -- 1-hour boundary
                        expValDesc = (this.minExpectedStopSec / 3600L) + "-" + textHour;
                    } else
                    if ((minExpectedStopSec % 60L) == 0L) {
                        // -- 1-minute boundary
                        expValDesc = (this.minExpectedStopSec / 60L) + "-" + textMinute;
                    } else {
                        // -- 1-second boundary
                        expValDesc = (this.minExpectedStopSec) + "-" + textSecond;
                    }
                }
                // --
                return i18n.getString("Device.StoppedSeconds.description",
                    "{0} window, {1} minimum stop, {2} expected total.",
                    windowDesc, minStopDesc, expValDesc);
            }
        }
        // --
        public long getTimeStart() {
            return this.timeStart;
        }
        public long getTimeEnd() {
            return this.timeEnd;
        }
        public boolean isTimestampInRange(long evTime) {
            return ((evTime >= this.timeStart) && (evTime <= this.timeEnd))? true : false;
        }
        public long getMinimumStoppedElapsedSeconds() {
            return this.minStopElapsedSec;
        }
        public long getMinimumExpectedStoppedSeconds() {
            return this.minExpectedStopSec;
        }
        // --
        public void addDeltaStoppedSeconds(long lastTimestamp, long currTimestamp) {
            if (lastTimestamp < currTimestamp) {
                long deltaStopSec;
                if (currTimestamp < this.timeStart) {
                    // -- currTimestamp is before beginning of test-range, ignore
                    deltaStopSec = 0L;
                } else
                if (lastTimestamp > this.timeEnd) {
                    // -- lastTimestamp is after end of test-range, ignore
                    deltaStopSec = 0L;
                } else
                if ((lastTimestamp < this.timeStart) && (currTimestamp > this.timeEnd)) {
                    // -- deltaStopSec spans entire test-range, ???
                    deltaStopSec = this.timeEnd - this.timeStart;
                } else
                if ((lastTimestamp < this.timeStart) && (currTimestamp >= this.timeStart)) {
                    // -- deltaStopSec spans start of test-range
                    deltaStopSec = currTimestamp - this.timeStart;
                } else
                if ((lastTimestamp >= this.timeStart) && (currTimestamp <= this.timeEnd)) {
                    // -- deltatime contained by test-range
                    deltaStopSec = currTimestamp - lastTimestamp;
                } else 
                if ((lastTimestamp <= this.timeEnd) && (currTimestamp > this.timeEnd)) {
                    // -- deltaStopSec spans end of test-range
                    deltaStopSec = this.timeEnd - lastTimestamp;
                } else {
                    // -- should not occur, we should have covered all bases above
                    deltaStopSec = 0L;
                }
                if (deltaStopSec >= this.minStopElapsedSec) {
                    this.totalStoppedSec += deltaStopSec;
                }
            }
        }
        public void clearTotalStoppedSeconds() {
            this.totalStoppedSec = 0L;
        }
        public long getTotalStoppedSeconds() {
            return this.totalStoppedSec;
        }
        public boolean isExpectedStoppedSeconds() {
            return (this.totalStoppedSec >= this.minExpectedStopSec)? true : false;
        }
    }

    // --------------------------------

    /** 
    *** Gets the accumulated stopped seconds within the specified time range.
    *** Used for driver fatigue checking.
    *** @param stoppedSec  Array of StoppedSeconds (modified with total stopped seconds)
    *** @throws DBException
    **/
    public void calculateStoppedSecondsInRange(final StoppedSeconds stoppedSec[], 
        final boolean checkIgnition, final boolean checkSpeed)
        throws DBException
    {

        /* no stoppedSec specified? */
        if (ListTools.size(stoppedSec) <= 0) {
            // -- nothing to do
            return;
        }

        /* get start/stop times */
        final long timeStart;
        final long timeEnd;
        {
            long _timeStart = 0L; // earliest start time
            long _timeEnd   = 0L; // latest end time
            for (StoppedSeconds ss : stoppedSec) {
                if ((_timeStart <= 0L) || (ss.getTimeStart() < _timeStart)) {
                    _timeStart = ss.getTimeStart();
                }
                if ((_timeEnd   <= 0L) || (ss.getTimeEnd()   > _timeEnd  )) {
                    _timeEnd = ss.getTimeEnd();
                }
            }
            timeStart = _timeStart;
            timeEnd   = _timeEnd;
        }

        /* accumulators */
        final AccumulatorLong    lastEventTime  = new AccumulatorLong(0L);
        final AccumulatorBoolean wasMoving      = new AccumulatorBoolean(true); // init on first event
        final AccumulatorLong    lastStopTime   = new AccumulatorLong(0L);

        /* device ignition indicator and status codes */
        final int                ignNdx         = this.getIgnitionIndex();
        final int                ignCodes[]     = this.getIgnitionStatusCodes();
        final AccumulatorInteger lastIgnState   = new AccumulatorInteger(-1); // unknown

        /* pre-pass check */
        // -- if "stopOnPass" is true, then the "StoppedSeconds" array will be checked after
        // -  each event is processed and will stop processing events when all conditions PASS.
        final boolean            stopOnPass     = false; // [experimental]
        final AccumulatorBoolean hasPassed      = new AccumulatorBoolean(false);

        /* iterate through all events in time-range and accumulate stop time */
        boolean validGPSOnly = false; // must be false to make sure we get all ignition status-code events
        EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            timeStart, timeEnd,
            null/*statusCodes*/, // any status codes
            validGPSOnly,
            EventData.LimitType.FIRST, -1L/*limit*/, true/*ascending*/,
            null/*additionalSelect*/,
            new DBRecordHandler<EventData>() {
                public int handleDBRecord(EventData ev) throws DBException {
                    boolean firstEvent = (lastEventTime.get() <= 0L)? true : false;
                    boolean validGPS   = ev.isValidGeoPoint();
                    // -- ignition state
                    int ignState = -1; // -1=unknown, 0=OFF, 1=ON
                    if (checkIgnition) {
                        boolean checkLastSC = firstEvent; // check prior events for ignState if first event
                        ignState = Device.this.getIgnitionStateAsOfEvent(ev,checkLastSC); // may be -1
                        if (ignState < 0) {
                            // -- unknown (ignition may be based on status-code)
                            ignState = lastIgnState.get(); // last ignition state (may still be -1)
                        } else {
                            // -- update ignition state
                            lastIgnState.set(ignState);
                        }
                    } else {
                        // -- leave as -1 (unknown)
                    }
                    // -- motion state (moving or ignition-on)
                    boolean isMoving = false;
                    if (ignState == 1) {
                        // -- checkIgnition and (ignition == on)
                        isMoving = true;
                    } else
                    if (checkSpeed && (ev.getSpeedKPH() > 0.0)) {
                        // -- checkSpeed and (speed > 0.0)
                        isMoving = true;
                    }
                    // -- 
                    boolean deltaStoppedChanged = false;
                    if (!validGPS) {
                        // -- disregard events with invalid GPS
                    } else
                    if (firstEvent) {
                        // -- first event in range, initialize "wasMoving", "lastStopTime"
                        if (isMoving) {
                            // -- if first event is ignition-on, assume all prior events are stopped
                            if (Device.this.isIgnitionOn(ev.getStatusCode())) { // [2.6.2-B01]
                                // -- special case when first event is an ignition-on
                                long lastTS = timeStart; // will not be 0 here
                                long currTS = ev.getTimestamp();
                                for (StoppedSeconds ss : stoppedSec) {
                                    // -- add delta-time between start-time and current-time
                                    ss.addDeltaStoppedSeconds(lastTS, currTS);
                                }
                                deltaStoppedChanged = true;
                            }
                            // -- (first ==> moving)
                            lastStopTime.set(0L); // not stopped
                            wasMoving.set(true);
                        } else {
                            // -- (first ==> stopped) start "stopped-timer"
                            // -  First stopped-time setting:
                            // -    timeStart         - may over estimate the stopped-time (false negatives)
                            // -    ev.getTimestamp() - may under estimate the stopped-time (false positives)
                            lastStopTime.set(timeStart);         // [2.6.1-B51]
                          //lastStopTime.set(ev.getTimestamp()); 
                            wasMoving.set(false);
                        }
                    } else
                    if (wasMoving.get()) { // true
                        // -- was previously moving
                        if (isMoving) {
                            // -- (moving ==> moving)
                        } else {
                            // -- (moving ==> stopped)  start timer at current event.
                            lastStopTime.set(ev.getTimestamp()); // start "stopped" timer
                            wasMoving.set(false); // switch to "stopped" state
                        }
                    } else {
                        // -- was previously stopped (wasMoving.get() == false)
                        if (isMoving) {
                            // -- (stopped ==> moving) tabulate previous stopped time
                            long lastTS = lastStopTime.get(); // will not be 0 here
                            long currTS = ev.getTimestamp();
                            for (StoppedSeconds ss : stoppedSec) {
                                // -- add delta-time between previous stop and current time
                                ss.addDeltaStoppedSeconds(lastTS, currTS);
                            }
                            deltaStoppedChanged = true;
                            // -- set moving
                            lastStopTime.set(0L); // no longer stopped
                            wasMoving.set(true); // switch to "moving" state
                        } else {
                            // -- (stopped ==> stopped) "lastStopTime" is last time stopped
                        }
                    }
                    // -- save last event time
                    lastEventTime.set(ev.getTimestamp());
                    // -- return
                    if (stopOnPass && deltaStoppedChanged) {
                        // -- [experimental] check to see if we already have what we need
                        for (StoppedSeconds ss : stoppedSec) {
                            if (!ss.isExpectedStoppedSeconds()) {
                                // -- not yet passed, continue with next record
                                return DBRecordHandler.DBRH_SKIP;
                            }
                        }
                        // -- we've passed, we can stop processing events
                        hasPassed.set(true); // final stopped seconds below must not be run
                        return DBRecordHandler.DBRH_STOP;
                    } else {
                        // -- continue with next event
                        return DBRecordHandler.DBRH_SKIP;
                    }
                }
            });

        /* final stopped seconds (not moving and not already passed) */
        if (wasMoving.get()) {
            // -- we are currently moving, nothing to add to stopped seconds
        } else
        if (hasPassed.get()) {
            // -- we've already passed, no need to add more stopped time
        } else {
            // -- (stopped ==> final) add final stopped seconds up to timeEnd
            long lastTS = lastStopTime.get(); // will not be 0 here
            for (StoppedSeconds ss : stoppedSec) {
                // -- add delta-time between previous stop and time-end
                ss.addDeltaStoppedSeconds(lastTS, ss.getTimeEnd());
              //boolean pass = ss.isExpectedStoppedSeconds();
            }
        }

    }

    /** 
    *** Returns true if all accumulated stopped seconds within the specified time range are greater-than
    *** or equal-to the corresponding expected stop seconds.  
    *** Used for driver fatigue checking.
    *** @param stoppedSec     Array of StoppedSeconds
    *** @param checkIgnition  True to check ignition for moving state
    *** @param checkSpeed     True to check speed for moving state
    *** @return True if all accumulated stopped seconds are >= their corresponding expected stop secods.
    *** @throws DBException
    **/
    public boolean isExpectedStoppedSeconds(StoppedSeconds stoppedSec[], 
        boolean checkIgnition, boolean checkSpeed)
        throws DBException
    {
        int i = this.getFailedExpectedStoppedSecondsIndex(stoppedSec,checkIgnition,checkSpeed);
        return (i < 0)? true : false;
    }

    /** 
    *** Returns the index of the failed accumulated stopped-seconds range, or -1 if all accumulated stopped
    *** seconds within the specified time range are greater-than or equal-to the corresponding expected stop 
    *** seconds.  
    *** Used for driver fatigue checking.
    *** @param stoppedSec     Array of StoppedSeconds
    *** @param checkIgnition  True to check ignition state for moving condition
    *** @param checkSpeed     True to check speed for moving state
    *** @return The index of the failed accumulated stopped-seconds range, or -1 if all pass.
    *** @throws DBException
    **/
    public int getFailedExpectedStoppedSecondsIndex(StoppedSeconds stoppedSec[], 
        boolean checkIgnition, boolean checkSpeed)
        throws DBException
    {
        if (ListTools.size(stoppedSec) > 0) {
            this.calculateStoppedSecondsInRange(stoppedSec,checkIgnition,checkSpeed);
            for (int i = 0; i < stoppedSec.length; i++) {
                StoppedSeconds ss = stoppedSec[i];
                if (!ss.isExpectedStoppedSeconds()) {
                    Print.logDebug("StoppedSeconds failed test: " + ss.getDescription(null));
                    return i; // false
                }
            }
        }
        return -1; // true
    }

    /** 
    *** Gets the accumulated stopped seconds within the specified time range.
    *** Used for driver fatigue checking.
    *** @param timeStart      The event start time
    *** @param timeEnd        The event end time
    *** @param minStopSec     Minimum elapsed seconds stopped required to add to stopped accumulator
    *** @param checkIgnition  True to check ignition state for moving condition
    *** @param checkSpeed     True to check speed for moving state
    *** @return The elapsed number of seconds stopped
    *** @throws DBException
    **/
    public long getStoppedSecondsInRange(long timeStart, long timeEnd, long minStopSec, 
        final boolean checkIgnition, final boolean checkSpeed)
        throws DBException
    {
      //final boolean checkIgnition = RTConfig.getBoolean(DBConfig.PROP_Device_stoppedInRangeIgnitionOff,STOPPED_IN_RANGE_IGNITION_OFF);
        StoppedSeconds SS[] = new StoppedSeconds[] { new StoppedSeconds(timeStart,timeEnd,minStopSec) };
        this.calculateStoppedSecondsInRange(SS,checkIgnition,checkSpeed);
        return SS[0].getTotalStoppedSeconds();
    }

    /**
    *** Check driver fatigue conditions
    *** @param name           The name/id of the specific DriverFatigue profile to use
    *** @param eventTime      The timestamp from which the DriverFatigue conditions are checked
    *** @param checkIgnition  True to check ignition state for moving condition
    *** @param checkSpeed     True to check speed for moving state
    *** @return 0 if the no DriverFatigue condition was triggered, -1 if an error occurred,
    ***         >0 if a DriverFatigue condition was triggered (return value indicates a 1-based
    ***         index of the condition that failed).
    **/
    public int checkDriverFatigue(String name, long eventTime, 
        boolean checkIgnition, boolean checkSpeed)
    {
        Locale locale = Account.GetLocale(this.getAccount());
        I18N i18n = I18N.getI18N(Device.class, locale);
        Device dev = this;
        RTProperties rtp = dev.getTemporaryProperties();

        /* property key */
        String defaultKey = "DriverFatigue";
        String propKey = !StringTools.isBlank(name)? name : defaultKey;

        /* create StoppedSeconds array */
        // -- True if Vehicle motion/stop conditions fail any of the following conditions
        long timeWindowSec;
        Device.StoppedSeconds ssList[];
        if (propKey.equalsIgnoreCase(defaultKey)) { // "DriverFatigue"
            timeWindowSec = DateTime.HourSeconds(24);
            ssList = new Device.StoppedSeconds[] {
                // 1) Last  24-hour window: Stopped at least 7-continguous-hours
                new Device.StoppedSeconds(eventTime - DateTime.HourSeconds(24 ), eventTime, DateTime.HourSeconds(7)   , DateTime.HourSeconds(7)),
                // 2) Last  24-hour window: Stopped at least 9-hours (minimum 30-continguous-min)
                new Device.StoppedSeconds(eventTime - DateTime.HourSeconds(24 ), eventTime, DateTime.MinuteSeconds(30), DateTime.HourSeconds(9)),
                // 3) Last   6-hour window: Stopped at least 30-minutes (minimum 30-continguous-min)
                new Device.StoppedSeconds(eventTime - DateTime.HourSeconds( 6 ), eventTime, DateTime.MinuteSeconds(30), DateTime.MinuteSeconds(30)),
            };
        } else
        if (propKey.equalsIgnoreCase("DriverFatigue_au")) {
            timeWindowSec = DateTime.HourSeconds(24);
            ssList = new Device.StoppedSeconds[] {
                // 1) Last  24-hour window: Stopped at least 7-continguous-hours
                new Device.StoppedSeconds(eventTime - DateTime.HourSeconds(24 ), eventTime, DateTime.HourSeconds(7)   , DateTime.HourSeconds(7)),
                // 2) Last  24-hour window: Stopped at least 9-hours (minimum 30-continguous-min)
                new Device.StoppedSeconds(eventTime - DateTime.HourSeconds(24 ), eventTime, DateTime.MinuteSeconds(30), DateTime.HourSeconds(9)),
                // 3) Last 5.5-hour window: Stopped at least 30-minutes (minimum 30-continguous-min)
                new Device.StoppedSeconds(eventTime - DateTime.HourSeconds(5.5), eventTime, DateTime.MinuteSeconds(30), DateTime.MinuteSeconds(30)),
            };
        } else {
            Print.logError("DriverFatigue name not found: " + name);
            rtp.setProperty(propKey, new I18N.Text(Device.class,"Device.DriverFatigue.undefined","Undefined"));
            rtp.setProperty(propKey+".index", new I18N.Text("")); // 1-based, "0"=none
            //Print.logInfo("'DriverFatigue' error: " + rtp.getProperty(propKey,"???"));
            return -1; // false
        }

        /* get start/stopped time */
        long startTime = eventTime - timeWindowSec;
        long stopTime  = eventTime;

        /* calculate/return */
        try {
            int ndx = dev.getFailedExpectedStoppedSecondsIndex(ssList,checkIgnition,checkSpeed);
            if (ndx >= 0) {
                // -- [0..2] true if failed
                rtp.setProperty(propKey, ssList[ndx].getDescription_I18N(locale));
                rtp.setProperty(propKey+".index", new I18N.Text(String.valueOf(ndx+1))); // 1-based
                //Print.logInfo("Set 'DriverFatigue' key string: " + rtp.getProperty(key,"???"));
                return ndx + 1; // true
            } else {
                // -- [-1] false if succeeded (not a failure)
                rtp.setProperty(propKey, new I18N.Text(Device.class,"Device.DriverFatigue.success","Success"));
                rtp.setProperty(propKey+".index", new I18N.Text("")); // 1-based, "0"=none
                //Print.logInfo("Set 'DriverFatigue' key string: " + rtp.getProperty(key,"???"));
                return 0; // false
            }
        } catch (DBException dbe) {
            // -- should not occur (fail)
            Print.logError("Error encountered while calculating DriverFatigue: " + dbe);
            rtp.setProperty(propKey, new I18N.Text(Device.class,"Device.DriverFatigue.error","Error"));
            rtp.setProperty(propKey+".index", new I18N.Text("?"));
            //Print.logInfo("Set 'DriverFatigue' key string: " + rtp.getProperty(key,"???"));
            return -1; // error
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets an array of events for the specified range and attributes
    *** @param timeStart  The event start time
    *** @param timeEnd    The event end time
    *** @param statusCodes  The list of status-codes
    *** @param validGPS    True to read only events with valid GPS locations
    *** @param limitType   The limit type (FIRST, LAST)
    *** @param limit       The maximum number of events to return
    *** @return The array of EventData records
    *** @throws DBException
    **/
    public EventData[] getRangeEvents(
        long timeStart, long timeEnd,
        int statusCodes[],
        boolean validGPS,
        EventData.LimitType limitType, long limit)
        throws DBException
    {

        /* get data */
        EventData ev[] = EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            timeStart, timeEnd,
            statusCodes,
            validGPS,
            limitType, limit, true/*ascending*/,
            null/*additionalSelect*/);

        /* apply current Device to all EventData records */
        if (ev != null) {
            for (int i = 0; i < ev.length; i++) {
                ev[i].setDevice(this);
            }
        }
        return ev;

    }

    /**
    *** Gets an array of events for the specified range and attributes
    *** @param timeStart  The event start time
    *** @param timeEnd    The event end time
    *** @param validGPS    True to read only events with valid GPS locations
    *** @param limitType   The limit type (FIRST, LAST)
    *** @param limit       The maximum number of events to return
    *** @return The array of EventData records
    *** @throws DBException
    **/
    public EventData[] getRangeEvents(
        long timeStart, long timeEnd,
        boolean validGPS,
        EventData.LimitType limitType, long limit)
        throws DBException
    {
        return this.getRangeEvents(
            timeStart, timeEnd, 
            null/*statusCodes*/, validGPS, 
            limitType, limit);
    }

    /**
    *** Gets an array of EventData records
    *** @param limit       The maximum number of events to return
    *** @param validGPS    True to read only events with valid GPS locations
    *** @return The array of EventData records
    **/
    public EventData[] getLatestEvents(long limit, boolean validGPS)
        throws DBException
    {
        long timeStart = -1L;
        long timeEnd   = -1L;
        return this.getRangeEvents(
            timeStart, timeEnd, 
            null/*statusCodes*/, validGPS, 
            EventData.LimitType.LAST, limit);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the first EventData record greater-than or equal-to the specified start time
    *** @param startTime  The event start time
    *** @param validGPS   True to only return the first event with a valid GPS
    *** @return The EventData record
    **/
    public EventData getFirstEvent(long startTime, boolean validGPS)
        throws DBException
    {
        long endTime = -1L;
        EventData ev[] = EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            startTime, endTime,
            null/*statusCodes[]*/,
            validGPS,
            EventData.LimitType.FIRST, 1/*limit*/, true/*ascending*/,
            null/*additionalSelect*/);
        if ((ev == null) || (ev.length <= 0)) {
            return null;
        } else {
            ev[0].setDevice(this);
            return ev[0];
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the last EventData record in the EventData table for this Device
    *** @param validGPS  True to only return the last event with a valid GPS
    *** @return The EventData record
    **/
    public EventData getLastEvent(boolean validGPS)
        throws DBException
    {
        // TODO: cache this event?
        return this.getLastEvent(null, -1L, validGPS);
    }

    /**
    *** Gets the last EventData record in the EventData table for this Device
    *** @param endTime Return the EventData record less-than or equal-to this time
    *** @param validGPS  True to only return the last event with a valid GPS
    *** @return The EventData record
    **/
    public EventData getLastEvent(long endTime, boolean validGPS)
        throws DBException
    {
        return this.getLastEvent(null, endTime, validGPS);
    }

    /**
    *** Gets the last EventData record in the EventData table for this Device
    *** @param statusCodes Return the last event that matches one of these status codes
    *** @return The EventData record
    **/
    public EventData getLastEvent(int statusCodes[])
        throws DBException
    {
        return this.getLastEvent(statusCodes, -1L, false/*validGPS?*/);
    }

    /**
    *** Gets the last EventData record in the EventData table for this Device
    *** @param statusCodes Return the last event that matches one of these status codes
    *** @param endTime     Return the EventData record less-than or equal-to this time
    *** @param validGPS    True to only return the last event with a valid GPS
    *** @return The EventData record
    **/
    public EventData getLastEvent(int statusCodes[], long endTime, boolean validGPS)
        throws DBException
    {
        long startTime = -1L;
        EventData ev[] = EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            startTime, endTime,
            statusCodes,
            validGPS,
            EventData.LimitType.LAST, 1, true,
            null/*additionalSelect*/);
        if ((ev == null) || (ev.length <= 0)) {
            return null;
        } else {
            ev[0].setDevice(this);
            return ev[0];
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** EventDataHandler interface
    **/
    public interface EventDataHandler
    {
        public void handleEventDataRecord(EventData ev);
    }
    
    /**
    *** Reprocesses a range of EventData reocrds
    *** @param timeStart  The start time
    *** @param timeEnd    The end time
    *** @param edh        The callback EventDataHandler instance
    **/
    public void reprocessEventDataRecords(long timeStart, long timeEnd, final EventDataHandler edh)
        throws DBException
    {
        EventData.getRangeEvents(
            this.getAccountID(), this.getDeviceID(),
            timeStart, timeEnd,
            null/*statusCodes*/,
            false/*validGPS*/,
            EventData.LimitType.LAST, -1L/*limit*/, true/*ascending*/,
            null/*additionalSelect*/,
            new DBRecordHandler<EventData>() {
                public int handleDBRecord(EventData rcd) throws DBException {
                    edh.handleEventDataRecord(rcd);
                    return DBRecordHandler.DBRH_SKIP;
                }
            });
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Callback for VIN changed
    **/
    protected void vinDidChange(String oldVIN, String newVIN) 
    {
        // -- TODO:
        // -  add VIN "change" implementation
    }
    
    // --------------------------------

    /**
    *** Callback indicating EventData instance will be inserted
    *** Note: <br>
    ***  - "evdb.getDevice()" may be a copy of the Device instance used during the EventData insertion process.
    ***  - No adjustments have been made to the Device instance.
    ***  - The EventData instance likely does not yet have an address assigned yet by the Reverse-Geocoder.
    **/
    protected void eventWillInsert(EventData evdb)
    {
        if (evdb == null) {
            // -- no EventData instance
        } else
        if (!Device.hasEventDataInsertionListener()) {
            // -- no EventDataInsertionListener
        } else {
            // -- "evdb.getDevice()" may be copy, but is always unmodified/non-updated
            try {
                Device.getEventDataInsertionListener().eventWillInsert(evdb);
            } catch (Throwable th) {
                Print.logError("Unexpected Exception: " + th);
            }
        }
    }

    /**
    *** Callback indicating EventData instance has been inserted
    *** Note: <br>
    ***  - "evdb.getDevice()" may be a unmodified copy of the Device instance used during the EventData insertion process.
    ***    "evdb.getDevice().getIsUnmodifiedCopy()" returns true if the Device instance is an unmodified copy.<br>
    ***  - The EventData instance likely does not yet have an address assigned yet by the Reverse-Geocoder.
    **/
    protected void eventDidInsert(EventData evdb)
    {
        if (evdb == null) {
            // -- no EventData instance
        } else
        if (!Device.hasEventDataInsertionListener()) {
            // -- no EventDataInsertionListener
        } else {
            // -- "evdb.getDevice()" may be an unmodified/non-updated copy of the Device
            // -- "evdb.getInsertionDevice()" returns the updated instance of the Device
            try {
                Device.getEventDataInsertionListener().eventDidInsert(evdb);
            } catch (Throwable th) {
                Print.logError("Unexpected Exception: " + th);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Save this Device to db storage
    **/
    public void save()
        throws DBException
    {

        /* save */
        super.save();
        if (this.transport != null) { this.transport.save(); }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return a String representation of this Device
    *** @return The String representation
    **/
    public String toString()
    {
        return this.getAccountID() + "/" + this.getDeviceID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Transport transport = null;

    /**
    *** Sets the Transport for this Device
    *** @param xport  The Transport instance
    **/
    public void setTransport(Transport xport)
    {
        this.transport = xport;
    }

    /**
    *** Gets the Transport-ID for this Device (if any)
    *** @return The Transport-ID for this Device, or an empty string is not defined
    **/
    public String getTransportID()
    {
        return (this.transport != null)? this.transport.getTransportID() : "";
    }

    /**
    *** Gets the DataTransport for this Device
    *** @return The DataTransport for this Device
    **/
    public DataTransport getDataTransport()
    {
        return (this.transport != null)? (DataTransport)this.transport : (DataTransport)this;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Count the number of events prior to the specified time<br>
    *** Note: Will return -1 if EventData table is InnoDB.
    *** @param oldTimeSec  The timestamp before which events will be counted
    *** @return The number of events counted
    **/
    public long countOldEvents(long oldTimeSec)
        throws DBException
    {
        String acctID = this.getAccountID();
        String devID  = this.getDeviceID();
        long count = EventData.getRecordCount(acctID, devID, -1L/*startTime*/, oldTimeSec); // -1 for InnoDB?
        return count;
    }

    // ------------------------------------------------------------------------

    /**
    *** Delete events prior to the specified time.<br>
    *** Note: Will return -1 if EventData table is InnoDB.  
    ***       Old events will still be deleted, however it will still go through the
    ***       motions of attempting to delete events, event if the range is empty.
    *** @param oldTimeSec  The timestamp before which events will be deleted
    *** @param logMsg      A StringBuffer instance into which deletion log messages are placed.
    *** @return The number of events deleted
    **/
    public long deleteOldEvents(
        long oldTimeSec,
        StringBuffer logMsg)
        throws DBException
    {
        return EventData.deleteOldEvents(this, oldTimeSec, logMsg); // -1 for InnoDB
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* reload override (from DBRecord.java) */
    public Device _reload(String... fldNames)
        throws DBException
    {
        Device devThis = super._reload(fldNames);
        /* clear cached items */
        this.cacheIgnitionState = -2; // reset cached ignition state
        this.cacheWorkHours     = null;
        this.cacheStatusCodeMap = null;
      //this.cachedRangeEvents  = null; <== explicitly set, no need to clear
        return devThis; // null if device does not exist
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified record exists
    *** @param acctID  The Account ID
    *** @param devID   The Device ID
    *** @return True if the record exists
    **/
    public static boolean exists(String acctID, String devID)
        throws DBException // if error occurs while testing existence
    {
        if ((acctID != null) && (devID != null)) {
            Device.Key devKey = new Device.Key(acctID, devID);
            return devKey.exists();
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** This method is used to load a Device record based on the SIM phone number. 
    *** Intended for use by an incoming SMS message handler.
    *** It is up to the caller to check whether this Device or Account are inactive.
    *** @param simPhone  The SIM phone number of the device
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceBySimPhoneNumber(String simPhone)
        throws DBException
    {

        /* invalid id? */
        if ((simPhone == null) || simPhone.equals("")) {
            return null; // just say it doesn't exist
        }

        /* read device for simPhone */
        Device       dev = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Device WHERE (simPhoneNumber='<phone>')
            DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(
                dwh.EQ(Device.FLD_simPhoneNumber,simPhone)
            ));
            dsel.setLimit(2);
            // Note: The index on the column FLD_simPhoneNumber does not enforce uniqueness
            // (since null/empty values are allowed and needed)

            /* get record */
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String acctId = rs.getString(FLD_accountID);
                String devId  = rs.getString(FLD_deviceID);
                dev = new Device(new Device.Key(acctId,devId));
                dev.setAllFieldValues(rs);
                if (rs.next()) {
                    Print.logError("Found multiple occurances of this SIM phone number: " + simPhone);
                }
                break; // only one record
            }
            // it's possible at this point that we haven't even read 1 device

        } catch (SQLException sqe) {
            throw new DBException("Getting Device SIM phone number: " + simPhone, sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return device */
        // Note: 'dev' may be null if it wasn't found
        return dev;

    }

    // ------------------------------------------------------------------------

    /**
    *** This method is called by "Transport.loadDeviceByUniqueID(String)" to load a Device
    *** within a Device Communication Server, based on a Unique-ID.  It is up to the caller
    *** to check whether this Device or Account are inactive.
    *** @param uniqId  The Unique-ID of the device (ie. IMEI, ESN, Serial#, etc)
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceByUniqueID(String uniqId)
        throws DBException
    {

        /* invalid id? */
        if ((uniqId == null) || uniqId.equals("")) {
            return null; // just say it doesn't exist
        }

        /* read device for unique-id */
        Device       dev = null;
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Device WHERE (uniqueID='unique')
            DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(
                dwh.EQ(Device.FLD_uniqueID,uniqId)
            ));
            dsel.setLimit(2);
            // Note: The index on the column FLD_uniqueID does not enforce uniqueness
            // (since null/empty values are allowed and needed)

            /* get record */
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String acctId = rs.getString(FLD_accountID);
                String devId  = rs.getString(FLD_deviceID);
                dev = new Device(new Device.Key(acctId,devId));
                dev.setAllFieldValues(rs);
                if (rs.next()) {
                    Print.logError("Found multiple occurances of this unique-id: " + uniqId);
                }
                break; // only one record
            }
            // it's possible at this point that we haven't even read 1 device

        } catch (SQLException sqe) {
            throw new DBException("Getting Device unique-id: " + uniqId, sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return device */
        // Note: 'dev' may be null if it wasn't found
        return dev;

    }

    // ------------------------------------------------------------------------

    /**
    *** This method is called by "Transport.loadDeviceByTransportID(...)" to load a Device
    *** within a Device Communication Server, based on the Account and Device IDs.
    *** @param account  The Account instance representing the owning account
    *** @param devID    The Device-ID
    *** @return The loaded Device instance, or null if the Device was not found
    *** @throws DBException if a database error occurs
    **/
    public static Device loadDeviceByName(Account account, String devID)
        throws DBException
    {
        Device dev = Device.getDevice(account, devID); // null if non-existent
        return dev;
    }

    // ------------------------------------------------------------------------
    
    private static final boolean GetDevice_SkipExistsCheck = true;

    /**
    *** Gets the specified Device record
    *** @param account  The Account
    *** @param devID    The Device ID
    *** @return The Device record
    **/
    public static Device getDevice(Account account, String devID)
        throws DBException
    {
        return Device.getDevice(account, devID, (String[])null/*allFields*/);
    }

    /**
    *** Gets the specified Device record
    *** @param account     The Account
    *** @param devID       The Device ID
    *** @param fieldNames  The specific field-names to load (null to load all)
    *** @return The Device record (null if Device does not exist)
    **/
    public static Device getDevice(Account account, String devID, String... fieldNames)
        throws DBException
    {
        if ((account != null) && (devID != null)) {
            // -- create key
            String acctID = account.getAccountID();
            Device.Key key = new Device.Key(acctID, devID);
            // -- get DBRecord
            if (GetDevice_SkipExistsCheck) {
                //Print.logDebug("*** Skipping 'exists()' check ...");
                Device dev = key._getDBRecord(false,(String[])null); // throws DBException
                if (dev._reload(fieldNames) != null) {
                    // -- device exists
                    dev.setAccount(account);
                    return dev;
                } else {
                    // -- device does not exist
                    return null; // [DBNotFoundException?]
                }
            } else
            if (key.exists()) {
                // -- device exists
                Device dev = key.getDBRecord(true, fieldNames); // throws DBException
                dev.setAccount(account);
                return dev;
            } else {
                // -- device does not exist
                return null; // [DBNotFoundException?]
            }
        } else {
            return null; // just say it doesn't exist [DBNotFoundException?]
        }
    }

    /**
    *** Gets the specified Device record.<br>
    *** This method should only be used when relatively sure that the specified deviceID exists.
    *** @param account     The Account
    *** @param devID       The Device ID
    *** @return The Device record (does not return null)
    *** @throws DBException If any DB error occurs.
    **/
    public static Device _getDevice(Account account, String devID)
        throws DBException
    {
        return Device._getDevice(account, devID, (String[])null/*fieldNames*/);
    }

    /**
    *** Gets the specified Device record.<br>
    *** This method should only be used when relatively sure that the specified deviceID exists.
    *** @param account     The Account
    *** @param devID       The Device ID
    *** @param fieldNames  The specific field-names to load (null to load all).  
    ***                    All non-specified fields will be undefined in this Device record.
    *** @return The Device record (does not return null)
    *** @throws DBException If any DB error occurs.
    **/
    public static Device _getDevice(Account account, String devID, String... fieldNames)
        throws DBException
    {
        if ((account != null) && (devID != null)) {
            // -- create key
            String acctID = account.getAccountID();
            Device.Key key = new Device.Key(acctID, devID);
            // -- get DBRecord
            Device dev = key._getDBRecord(true, fieldNames); // may throw DBException
            dev.setAccount(account);
            return dev;
        } else {
            throw new DBException("Account/Device null");
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets or Creates a Device record
    *** @param account The Account
    *** @param devID  The Device ID
    *** @param create  True to create the Device if it does not already exist
    *** @return The Device record (never null)
    *** @throws DBExeption
    **/
    public static Device getDevice(Account account, String devID, boolean create)
        throws DBException // DBNotFoundException, DBAlreadyExistsException
    {

        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctID = account.getAccountID();

        /* device-id specified? */
        if (StringTools.isBlank(devID)) {
            throw new DBNotFoundException("Device-ID not specified for account: " + acctID);
        }

        /* create */
        if (create) {
            // -- create new Device record (not yet saved)
            Device.Key devKey = new Device.Key(acctID, devID);
            if (devKey.exists()) {
                // -- we've been asked to create the device, and it already exists
                throw new DBAlreadyExistsException("Device-ID already exists '" + devKey + "'");
            }
            Device dev = devKey.getDBRecord();
            dev.setAccount(account);
            dev.setCreationDefaultValues();
            return dev; // not null, not yet saved!
        }

        /* get */
        Device dev = Device.getDevice(account, devID); // null if non-existent
        if (dev == null) {
            // -- device likely does not exist
            throw new DBNotFoundException("Unable to read Device-ID: " + acctID + "/" + devID);
        }
        return dev; // not null

        /* get/create */
        /*
        Device.Key devKey = new Device.Key(acctID, devID);
        if (!devKey.exists()) {
            if (create) {
                Device dev = devKey.getDBRecord();
                dev.setAccount(account);
                dev.setCreationDefaultValues();
                return dev; // not yet saved!
            } else {
                throw new DBNotFoundException("Device-ID does not exists: " + devKey);
            }
        } else
        if (create) {
            // -- we've been asked to create the device, and it already exists
            throw new DBAlreadyExistsException("Device-ID already exists '" + devKey + "'");
        } else {
            Device dev = Device.getDevice(account, devID); // null if non-existent
            if (dev == null) {
                throw new DBException("Unable to read existing Device-ID: " + devKey);
            }
            return dev;
        }
        */

    }

    // ------------------------------------------------------------------------

    /** 
    *** Create/Save Device record
    *** @param account The Account
    *** @param devID   The Device ID
    *** @param uniqueID The Device Unique ID
    *** @return The Device record
    *** @throws DBExeption
    **/
    public static Device createNewDevice(Account account, String devID, String uniqueID)
        throws DBException
    {
        if ((account != null) && !StringTools.isBlank(devID)) {
            Device dev = Device.getDevice(account, devID, true); // does not return null
            if (!StringTools.isBlank(uniqueID)) {
                dev.setUniqueID(uniqueID);
            }
            dev.save();
            return dev;
        } else {
            throw new DBException("Invalid Account/DeviceID specified");
        }
    }

    /**
    *** (EXPERIMENTAL) Creates a virtual Device record
    *** @param acctID  The Account ID
    *** @param devID   The Device ID
    *** @return The Device record
    **/
    public static Device createVirtualDevice(String acctID, String devID)
    {

        /* get/create */
        Device.Key devKey = new Device.Key(acctID, devID);
        Device dev = devKey.getDBRecord();
        dev.setCreationDefaultValues();
        dev.setVirtual(true);
        return dev;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a set of Device IDs for the specified Account (oes not return null)
    *** @param acctId  The Account ID
    *** @param userAuth  The User record
    *** @param inclInactv  True to include inactive Devices
    *** @return A set of Device IDs
    *** @throws DBExeption
    **/
    public static OrderedSet<String> getDeviceIDsForAccount(String acctId, User userAuth, boolean inclInactv)
        throws DBException
    {
        return Device.getDeviceIDsForAccount(acctId, userAuth, inclInactv, -1L);
    }

    /**
    *** Gets a set of Device IDs for the specified Account (oes not return null)
    *** @param acctId  The Account ID
    *** @param userAuth  The User record
    *** @param inclInactv  True to include inactive Devices
    *** @param limit  The maximum number of Device IDs to return
    *** @return A set of Device IDs
    *** @throws DBExeption
    **/
    public static OrderedSet<String> getDeviceIDsForAccount(String acctId, User userAuth, boolean inclInactv, long limit)
        throws DBException
    {

        /* no account specified? */
        if (StringTools.isBlank(acctId)) {
            if (userAuth != null) {
                acctId = userAuth.getAccountID();
            } else {
                Print.logError("Account not specified!");
                return new OrderedSet<String>();
            }
        }

        /* read devices for account */
        OrderedSet<String> devList = new OrderedSet<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT * FROM Device WHERE (accountID='acct') ORDER BY deviceID
            DBSelect<Device> dsel = new DBSelect<Device>(Device.getFactory());
            dsel.setSelectedFields(Device.FLD_deviceID);
            DBWhere dwh = dsel.createDBWhere();
            if (inclInactv) {
                dsel.setWhere(dwh.WHERE(
                    dwh.EQ(Device.FLD_accountID,acctId)
                ));
            } else {
                dsel.setWhere(dwh.WHERE_(
                    dwh.AND(
                        dwh.EQ(Device.FLD_accountID,acctId),
                        dwh.NE(Device.FLD_isActive,0)
                    )
                ));
            }
            dsel.setOrderByFields(Device.FLD_deviceID);
            dsel.setLimit(limit);

            /* get records */
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(Device.FLD_deviceID);
                if ((userAuth == null) || userAuth.isAuthorizedDevice(devId)) {
                    devList.add(devId);
                }
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account Device List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return devList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // This section supports a method for obtaining human readable information from
    // the Device record for reporting, or email purposes. (currently this is
    // used by the 'rules' engine when generating notification emails).
    
    public  static final String KEY_ACCOUNT[]         = new String[] { "account"          , "accountDesc"     };  // "Smith Trucking"
    public  static final String KEY_ACCOUNT_ID[]      = new String[] { "accountID"                            };  // "smith"
    private static final String KEY_DEVICE[]          = EventData.KEY_DEVICE;
    private static final String KEY_DEVICE_ID[]       = EventData.KEY_DEVICE_ID;
    private static final String KEY_DEVICE_LINK[]     = EventData.KEY_DEVICE_LINK;
    private static final String KEY_DEV_CONN_AGE[]    = EventData.KEY_DEV_CONN_AGE;
    private static final String KEY_DEV_TRAILERS[]    = EventData.KEY_DEV_TRAILERS;
    public  static final String KEY_LICENSE_PLATE[]   = new String[] { "licensePlate"                          };  // "ABC123"
    public  static final String KEY_EQUIP_STATUS[]    = new String[] { "equipmentStatus"    , "equipStatus"    };  // "active" [2.6.5-B14]
    private static final String KEY_EVENT_COUNT24[]   = EventData.KEY_EVENT_COUNT24;
    private static final String KEY_LAST_EPS[]        = new String[] { "lastEventsPerSecond", "lastEPS" , "eventsPerSecond" }; 
    private static final String KEY_LAST_EPH[]        = new String[] { "lastEventsPerHour"  , "lastEPH" , "eventsPerHour"   }; 
    private static final String KEY_SIM_PHONE[]       = new String[] { "simPhoneNumber"     , "simPhone"   }; 

    private static final String KEY_DRIVERID[]        = EventData.KEY_DRIVERID;
    private static final String KEY_DRIVER_DESC[]     = EventData.KEY_DRIVER_DESC;
    public  static final String KEY_DRIVER_BADGE[]    = EventData.KEY_DRIVER_BADGE;
    public  static final String KEY_DRIVER_LICENSE[]  = EventData.KEY_DRIVER_LICENSE;
    public  static final String KEY_DRIVER_PHONE[]    = EventData.KEY_DRIVER_PHONE;

    private static final String KEY_FAULT_CODE[]      = EventData.KEY_FAULT_CODE;
    private static final String KEY_FAULT_CODES[]     = EventData.KEY_FAULT_CODES;

    private static final String KEY_FUEL_LEVEL[]      = new String[] { "fuelLevel"                              };  // "25.0 %"
    private static final String KEY_LAST_FUEL_LEV[]   = new String[] { "lastFuelLevel"                          };  // "25.0 %"
    private static final String KEY_FUEL_VOLUME[]     = new String[] { "fuelLevelVolume"     , "fuelVolume"     };  // "12 gal"
    private static final String KEY_LAST_FUEL_VOL[]   = new String[] { "lastFuelLevelVolume" , "lastFuelVolume" };  // "12 gal"

    private static final String KEY_FUEL_LEVEL2[]     = new String[] { "fuelLevel2"                             };  // "25.0 %"
    private static final String KEY_LAST_FUEL_LEV2[]  = new String[] { "lastFuelLevel2"                         };  // "25.0 %"
    private static final String KEY_FUEL_VOLUME2[]    = new String[] { "fuelLevelVolume2"    , "fuelVolume2"    };  // "12 gal"
    private static final String KEY_LAST_FUEL_VOL2[]  = new String[] { "lastFuelLevelVolume2", "lastFuelVolume2"};  // "12 gal"

    private static final String KEY_CORRIDOR_ID[]     = new String[] { "activeCorridor"      , "corridorID"     };  // "freeway"
    private static final String KEY_CORRIDOR_DESC[]   = new String[] { "activeCorridorDesc"  , "corridorDesc"   };  // "freeway"
    private static final String KEY_STOP_ELAPSED[]    = new String[] { "stopElapsed"         , "timeStopped"    }; 
    private static final String KEY_SPEED_LIMIT[]     = new String[] { "devSpeedLimit"       , "speedLimit"     }; 
    private static final String KEY_REMINDER[]        = new String[] { "reminderMessage"     , "reminder"       }; 

    private static final String KEY_LAST_IGN_HOURS[]  = new String[] { "lastIgnitionHours"                      };
    private static final String KEY_LAST_ENG_HOURS[]  = new String[] { "lastEngineHours"                        };
    private static final String KEY_LAST_IGN_ON[]     = new String[] { "lastIgnitionOn"                         };
    private static final String KEY_LAST_IGN_OFF[]    = new String[] { "lastIgnitionOff"                        };
    private static final String KEY_IGNITION_STATE[]  = new String[] { "ignitionState"       , "ignition"       };

    private static final String KEY_COMMAND_STATE[]   = new String[] { "commandState"                           };
    private static final String KEY_COMMAND_TIME[]    = new String[] { "commandDateTime"     , "commandTime", "pingDateTime"};
    private static final String KEY_ACK_DATETIME[]    = new String[] { "ackDateTime"                            };
    
    private static final String KEY_MAINT_DESC[]      = new String[] { "maintDesc"                              };
    private static final String KEY_MAINT_ODOMETER[]  = new String[] { "maintOdometer"       , "maintOdom"      };
    private static final String KEY_MAINT_DIST_NEXT[] = new String[] { "maintDistToNext"     , "maintNextDelta" };
    private static final String KEY_MAINT_INTERVAL[]  = new String[] { "maintInterval"       , "maintInter"     };
    private static final String KEY_MAINT_NOTES[]     = new String[] { "maintNotes"                             };

    private static final String KEY_DEVICE_PHONE[]    = new String[] { "fixedPhone"          , "devicePhone"    }; 

    private static final String KEY_EVAL[]            = new String[] { "evaluate"            , "eval"           }; //

    private static final String KEY_CUSTOM[]          = new String[] { "custom"                                 }; 

    /**
    *** Gets the field title for the specified key
    *** @param key  The key
    *** @param arg  The type parameter
    *** @param locale  The Locale
    *** @return The title
    **/
    public static String getKeyFieldTitle(String key, String arg, Locale locale)
    {
        return Device._getKeyFieldString(
            true/*title*/, key, arg, 
            locale, null/*BasicPrivateLabel*/, null/*Device*/);
    }

    /**
    *** Gets the field value for the specified key
    *** @param key  The key
    *** @param arg  The type parameter
    *** @param bpl  The BasicPrivateLabel
    *** @return The value
    **/
    public String getKeyFieldValue(String key, String arg, BasicPrivateLabel bpl)
    {
        Locale locale = (bpl != null)? bpl.getLocale() : null;
        return Device._getKeyFieldString(
            false/*value*/, key, arg, 
            locale, bpl, this);
    }

    /**
    *** Gets the field title/value for the specified key
    *** @param getTitle  True to get the title, false for value
    *** @param key      The key
    *** @param arg      The type parameter
    *** @param locale   The Locale
    *** @param bpl      The BasicPrivateLabel
    *** @param dev      The Device record
    *** @return The title/value
    **/
    public static String _getKeyFieldString(
        boolean getTitle, String key, String arg, 
        Locale locale, BasicPrivateLabel bpl, Device dev)
    {
        //Print.logInfo("Getting "+(getTitle?"title":"value")+" for key: " + key);

        /* check for valid field name */
        if (key == null) {
            return null;
        } else
        if ((dev == null) && !getTitle) {
            return null;
        }
        if ((locale == null) && (bpl != null)) { locale = bpl.getLocale(); }
        I18N i18n = I18N.getI18N(Device.class, locale);
        long now = DateTime.getCurrentTimeSec();

        /* make sure arg is not null */
        arg = StringTools.trim(arg);
        int argP = key.indexOf(":");
        if (argP >= 0) {
            if (arg.equals("")) {
                arg = key.substring(argP+1);
            }
            key = key.substring(0,argP);
        }

        /* Device values */
        if (EventData._keyMatch(key,Device.KEY_ACCOUNT)) {
            if (getTitle) {
                return i18n.getString("Device.key.accountDescription", "Account");
            } else {
                Account account = dev.getAccount();
                if (arg.equalsIgnoreCase("id") || (account == null)) {
                    return dev.getAccountID();
                } else {
                    return account.getDescription();
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_ACCOUNT_ID)) {
            if (getTitle) {
                return i18n.getString("Device.key.accountID", "Account-ID");
            } else {
                return dev.getAccountID();
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_DEVICE)) {
            if (getTitle) {
                return i18n.getString("Device.key.deviceDescription", "Device");
            } else {
                if (arg.equalsIgnoreCase("id")) {
                    return dev.getDeviceID();
                } else {
                    String desc = dev.getDescription();
                    if (!StringTools.isBlank(desc)) {
                        return desc;
                    } else {
                        return "(" + dev.getDeviceID() + ")";
                    }
                }
            }
        } else
        if (EventData._keyMatch(key,EventData.KEY_DEVICE_ID)) {
            if (getTitle) {
                return i18n.getString("Device.key.deviceID", "Device-ID");
            } else {
                return dev.getDeviceID();
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_DEVICE_LINK)) {
            if (getTitle) {
                return i18n.getString("Device.key.deviceLink", "Device Link");
            } else {
                String url = dev.getLinkURL();
                String dsc = StringTools.blankDefault(dev.getLinkDescription(),
                    i18n.getString("Device.key.link", "Link"));
                if (StringTools.isBlank(url)) {
                    return "";
                } else
                if (StringTools.isBlank(arg)    || 
                    arg.equalsIgnoreCase("a")   ||  // "anchor"
                    arg.equalsIgnoreCase("html")||
                    arg.equalsIgnoreCase("link")  ) {
                    if (!StringTools.isBlank(url)) {
                        return EventUtil.MAP_ESCAPE_HTML+"<a href='"+url+"' target='_blank'>"+dsc+"</a>";
                    } else {
                        return EventUtil.MAP_ESCAPE_HTML+"<a>"+dsc+"</a>";
                    }
                } else
                if (arg.equalsIgnoreCase("plain") ||
                    arg.equalsIgnoreCase("desc")    ) {
                    return dsc + ": " + url;
                } else { // "url"
                    return url;
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_DEV_CONN_AGE)) {
            if (getTitle) {
                return i18n.getString("Device.key.sinceLastConnect", "Since Connection");
            } else {
                // HHH:MM:SS
                long lastConnectTime = dev.getLastTotalConnectTime();
                if (lastConnectTime <= 0L) {
                    return "--:--:--";
                }
                long ageSec = DateTime.getCurrentTimeSec() - lastConnectTime;
                if (ageSec < 0L) { ageSec = 0L; }
                long hours  = (ageSec        ) / 3600L;
                long min    = (ageSec % 3600L) /   60L;
                long sec    = (ageSec %   60L);
                StringBuffer sb = new StringBuffer();
                sb.append(hours).append(":");
                if (min   < 10) { sb.append("0"); }
                sb.append(min  ).append(":");
                if (sec   < 10) { sb.append("0"); }
                sb.append(sec  );
                return sb.toString();
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_DEV_TRAILERS)) {
            if (getTitle) {
                return i18n.getString("Device.key.attachedTrailers", "Attached Trailers");
            } else {
                String e[] = dev.getAttachedEntityDescriptions(EntityManager.EntityType.TRAILER);
                if (!ListTools.isEmpty(e)) {
                    return StringTools.join(e,",");
                } else {
                    return "";
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LICENSE_PLATE)) {
            if (getTitle) {
                return i18n.getString("Device.key.licensePlate", "License Plate");
            } else {
                return dev.getLicensePlate();
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_EQUIP_STATUS)) { // [2.6.5-B14]
            if (getTitle) {
                return i18n.getString("Device.key.equipmentStatus", "Equip. Status");
            } else {
                return dev.getEquipmentStatus();
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_EVENT_COUNT24)) {
            if (getTitle) {
                return i18n.getString("Device.key.24HourEventCount", "24Hr Event Count");
            } else {
                String a[]       = StringTools.split(arg,',');
                int sinceHH      = (a.length > 1)? StringTools.parseInt(a[0],24) : 24;
                int statCodes[]  = ((a.length > 2) && !StringTools.isBlank(a[1]))? 
                    new int[] { StringTools.parseInt(a[1],StatusCodes.STATUS_NONE) } : 
                    null;
                long timeStart   = now - DateTime.HourSeconds((sinceHH > 0)? sinceHH : 24);
                long timeEnd     = -1L;
                long recordCount = -1L;
                try {
                    recordCount = EventData.countRangeEvents(
                        dev.getAccountID(), dev.getDeviceID(),
                        timeStart, timeEnd,
                        statCodes,
                        false/*validGPS*/,
                        EventData.LimitType.LAST/*limitType*/, -1L/*limit*/, // no limit
                        null/*where*/);
                } catch (DBException dbe) {
                    Print.logError("Unable to obtain EventData record count [" + dbe);
                }
                return String.valueOf(recordCount);
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_EPS)) { // 2.5.1-B31
            if (getTitle) {
                return i18n.getString("Device.key.lastEventsPerSecond", "Events/Sec");
            } else {
                double eps = dev.getAgedEventsPerSecond(System.currentTimeMillis());
                if (eps <= 0.0) {
                    return i18n.getString("Device.notAvailable", "n/a");
                } else {
                    return StringTools.format(eps,"0.000");
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_EPH)) { // 2.5.1-B31
            if (getTitle) {
                return i18n.getString("Device.key.lastEventsPerHour", "Events/Hour");
            } else {
                double eps = dev.getAgedEventsPerSecond(System.currentTimeMillis());
                if (eps <= 0.0) {
                    return i18n.getString("Device.notAvailable", "n/a");
                } else {
                    double eph = eps * 60.0 * 60.0;
                    return StringTools.format(eph,"0.0");
                }
            }
        }

        /* SIM phone */
        if (EventData._keyMatch(key,Device.KEY_SIM_PHONE)) {
            if (getTitle) {
                return i18n.getString("Device.key.simPhone", "SIM Phone");
            } else {
                String ph = dev.getSimPhoneNumber();
                if (StringTools.isBlank(ph)) {
                    return "";
                } else
                if (StringTools.isBlank(arg)      || 
                    arg.equalsIgnoreCase("plain")   ) {
                    return ph;
                } else
                if (arg.equalsIgnoreCase("a")   ||  // "anchor"
                    arg.equalsIgnoreCase("html")||
                    arg.equalsIgnoreCase("link")  ) {
                    return EventUtil.MAP_ESCAPE_HTML+"<a href='tel:"+ph+"' target='_blank'>"+ph+"</a>";
                } else {
                    return ph;
                }
            }
        } 

        /* fuel (tank #1) */
        if (EventData._keyMatch(key,Device.KEY_FUEL_LEVEL)) {
            if (getTitle) {
                return i18n.getString("Device.key.fuelLevel", "Fuel Level");
            } else {
                Device.FuelTankIndex tank = Device.FuelTankIndex.TANK_1;
                double level = dev.getLastFuelLevel(tank); // tank #1
                if (level < 0.0) {
                    return i18n.getString("Device.notAvailable", "n/a");
                } else {
                    long pct = Math.round(level * 100.0);
                    return pct+"%";
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_FUEL_LEV)) {
            if (getTitle) {
                return i18n.getString("Device.key.lastFuelLevel", "Last Fuel Level");
            } else {
                Device.FuelTankIndex tank = Device.FuelTankIndex.TANK_1;
                double level = dev.getLastFuelLevel(tank); // tank #1
                if (level < 0.0) {
                    return i18n.getString("Device.notAvailable", "n/a");
                } else {
                    long pct = Math.round(level * 100.0);
                    return pct+"%";
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_FUEL_VOLUME)) {
            if (getTitle) {
                return i18n.getString("Device.key.fuelVolume", "Fuel Volume");
            } else {
                Account.VolumeUnits vu = Account.getVolumeUnits(dev.getAccount());
                Device.FuelTankIndex tank = Device.FuelTankIndex.TANK_1;
                double C = dev.getFuelCapacity(tank);
                double L = dev.getLastFuelLevel(tank); // tank #1
                double V = vu.convertFromLiters(C * L);
                return StringTools.format(V,"0.0") + " " + vu.toString(locale);
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_FUEL_VOL)) {
            if (getTitle) {
                return i18n.getString("Device.key.lastFuelVolume", "Last Fuel Volume");
            } else {
                Account.VolumeUnits vu = Account.getVolumeUnits(dev.getAccount());
                Device.FuelTankIndex tank = Device.FuelTankIndex.TANK_1;
                double C = dev.getFuelCapacity(tank);
                double L = dev.getLastFuelLevel(tank); // tank #1
                double V = vu.convertFromLiters(C * L);
                return StringTools.format(V,"0.0") + " " + vu.toString(locale);
            }
        }

        /* fuel (tank #2) */
        if (EventData._keyMatch(key,Device.KEY_FUEL_LEVEL2)) {
            if (getTitle) {
                return i18n.getString("Device.key.fuelLevel2", "Fuel Level #2");
            } else {
                Device.FuelTankIndex tank = Device.FuelTankIndex.TANK_2;
                double level = dev.getLastFuelLevel(tank); // tank #2
                if (level < 0.0) {
                    return i18n.getString("Device.notAvailable", "n/a");
                } else {
                    long pct = Math.round(level * 100.0);
                    return pct+"%";
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_FUEL_LEV2)) {
            if (getTitle) {
                return i18n.getString("Device.key.lastFuelLevel2", "Last Fuel Level #2");
            } else {
                Device.FuelTankIndex tank = Device.FuelTankIndex.TANK_2;
                double level = dev.getLastFuelLevel(tank); // tank #2
                if (level < 0.0) {
                    return i18n.getString("Device.notAvailable", "n/a");
                } else {
                    long pct = Math.round(level * 100.0);
                    return pct+"%";
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_FUEL_VOLUME2)) {
            if (getTitle) {
                return i18n.getString("Device.key.fuelVolume2", "Fuel Volume #2");
            } else {
                Account.VolumeUnits vu = Account.getVolumeUnits(dev.getAccount());
                Device.FuelTankIndex tank = Device.FuelTankIndex.TANK_2;
                double C = dev.getFuelCapacity(tank);
                double L = dev.getLastFuelLevel(tank); // tank #2
                double V = vu.convertFromLiters(C * L);
                return StringTools.format(V,"0.0") + " " + vu.toString(locale);
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_FUEL_VOL2)) {
            if (getTitle) {
                return i18n.getString("Device.key.lastFuelVolume2", "Last Fuel Volume #2");
            } else {
                Account.VolumeUnits vu = Account.getVolumeUnits(dev.getAccount());
                Device.FuelTankIndex tank = Device.FuelTankIndex.TANK_2;
                double C = dev.getFuelCapacity(tank);
                double L = dev.getLastFuelLevel(tank); // tank #2
                double V = vu.convertFromLiters(C * L);
                return StringTools.format(V,"0.0") + " " + vu.toString(locale);
            }
        }

        /* Driver */
        if (EventData._keyMatch(key,Device.KEY_DRIVERID)) {
            if (getTitle) {
                return i18n.getString("Device.key.driverID", "Driver ID");
            } else {
                // -- first check DriverID
                String driverID = dev.getDriverID();
                if (!StringTools.isBlank(driverID)) {
                    return driverID;
                }
                // next try attached Driver Entities
                String d[] = dev.getAttachedEntityIDs(EntityManager.EntityType.DRIVER);
                if (!ListTools.isEmpty(d)) {
                    return StringTools.join(d,",");
                }
                // return blank
                return "";
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_DRIVER_DESC)) {
            if (getTitle) {
                return i18n.getString("Device.key.driverDescription", "Driver");
            } else {
                // first check DriverID
                String driverID = dev.getDriverID();
                if (!StringTools.isBlank(driverID)) {
                    Driver driver = dev.getDriver();
                    if (driver != null) {
                        return driver.getDescription();
                    } else {
                        Print.logDebug("Unable to read Driver: " + driverID);
                        return driverID;
                    }
                }
                // next try attached Driver Entities
                String d[] = dev.getAttachedEntityDescriptions(EntityManager.EntityType.DRIVER);
                if (!ListTools.isEmpty(d)) {
                    return StringTools.join(d,",");
                }
                // return blank
                return "";
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_DRIVER_BADGE)) {
            if (getTitle) {
                return i18n.getString("Device.key.driverBadge", "Driver Badge");
            } else {
                String driverID = dev.getDriverID();
                if (!StringTools.isBlank(driverID)) {
                    Driver driver = dev.getDriver();
                    if (driver != null) {
                        return driver.getBadgeID();
                    } else {
                        Print.logDebug("Unable to read Driver: " + driverID);
                        return driverID;
                    }
                }
                // return blank
                return "";
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_DRIVER_LICENSE)) {
            if (getTitle) {
                return i18n.getString("Device.key.driverLicense", "Driver License");
            } else {
                String driverID = dev.getDriverID();
                if (!StringTools.isBlank(driverID)) {
                    Driver driver = dev.getDriver();
                    if (driver != null) {
                        return driver.getLicenseNumber();
                    } else {
                        Print.logDebug("Unable to read Driver: " + driverID);
                        return driverID;
                    }
                }
                // return blank
                return "";
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_DRIVER_PHONE)) {
            if (getTitle) {
                return i18n.getString("Device.key.driverPhone", "Driver Phone");
            } else {
                String driverID = dev.getDriverID();
                if (StringTools.isBlank(driverID)) {
                    return "";
                }
                Driver driver = dev.getDriver();
                if (driver == null) {
                    Print.logDebug("Unable to read Driver: " + driverID);
                    return "";
                }
                String ph = driver.getContactPhone();
                if (StringTools.isBlank(ph)) {
                    return "";
                } else
                if (StringTools.isBlank(arg)      || 
                    arg.equalsIgnoreCase("plain")   ) {
                    return ph;
                } else
                if (arg.equalsIgnoreCase("a")   ||  // "anchor"
                    arg.equalsIgnoreCase("html")||
                    arg.equalsIgnoreCase("link")  ) {
                    return EventUtil.MAP_ESCAPE_HTML+"<a href='tel:"+ph+"' target='_blank'>"+ph+"</a>";
                } else {
                    return ph;
                }
            }
        } 

        /* OBD fault values */
        if (EventData._keyMatch(key,Device.KEY_FAULT_CODES)) {
            if (getTitle) {
                return i18n.getString("Device.key.faultCodes", "Fault Codes");
            } else {
                String fault = dev.getLastFaultCode().toUpperCase();
                if (!StringTools.isBlank(fault)) {
                    RTProperties rtpFault = new RTProperties(fault);
                    return DTOBDFault.GetFaultString(rtpFault);
                }
                // return blank
                return "";
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_FAULT_CODE)) {
            if (getTitle) {
                return i18n.getString("Device.key.faultCodes", "Fault Codes");
            } else {
                String fault = dev.getLastFaultCode().toUpperCase();
                if (!StringTools.isBlank(fault)) {
                    RTProperties rtpFault = new RTProperties(fault);
                    return DTOBDFault.GetFaultString(rtpFault);
                }
                // return blank
                return "";
            }
        }

        /* GeoCorridor */
        if (EventData._keyMatch(key,Device.KEY_CORRIDOR_ID)) {
            if (getTitle) {
                return i18n.getString("Device.key.activeCorridor", "Active Corridor");
            } else {
                String actvCorr = dev.getActiveCorridor();
                return actvCorr;
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_CORRIDOR_DESC)) {
            if (getTitle) {
                return i18n.getString("Device.key.activeCorridorDesc", "Active Corridor\nDescription");
            } else {
                String actvCorr = dev.getActiveCorridor();
                if (!StringTools.isBlank(actvCorr)) {
                    Account acct = dev.getAccount();
                    RuleFactory rf = Device.getRuleFactory();
                    String corrDesc = (rf != null)? rf.getGeoCorridorDescription(acct,actvCorr) : null;
                    return !StringTools.isBlank(corrDesc)? corrDesc : actvCorr;
                } else {
                    return actvCorr;
                }
            }
        }

        /* Last Ignition/Engine Hours */
        if (EventData._keyMatch(key,Device.KEY_LAST_IGN_HOURS)) {
            if (getTitle) {
                return i18n.getString("Device.key.lastIgnitionHours", "Ignition Hours");
            } else {
                double ignHrs = dev.getLastIgnitionHours();
                long   ignSec = (long)(ignHrs * 3600.0);
                if (ignSec <= 0L) {
                    return "";
                } else {
                    return StringTools.formatElapsedSeconds(ignSec,StringTools.ELAPSED_FORMAT_HHMMSS);
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_ENG_HOURS)) {
            if (getTitle) {
                return i18n.getString("Device.key.lastEngineHours", "Engine Hours");
            } else {
                double engHrs = dev.getLastEngineHours();
                long   engSec = (long)(engHrs * 3600.0);
                if (engSec <= 0L) {
                    return "";
                } else {
                    return StringTools.formatElapsedSeconds(engSec,StringTools.ELAPSED_FORMAT_HHMMSS);
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_IGN_ON)) {
            if (getTitle) {
                return i18n.getString("Device.key.lastIgnitionOn", "Ignition On\nTime");
            } else {
                long lastIgnOn  = dev.getLastIgnitionOnTime();
                long lastIgnOff = dev.getLastIgnitionOffTime();
                if ((lastIgnOn > 0L) && (lastIgnOn > lastIgnOff)) {
                    // -- Ignition On
                    Account acct = dev.getAccount();
                    TimeZone tmz = (acct != null)? acct.getTimeZone(null) : DateTime.getGMTTimeZone();
                    return EventData.getTimestampString(lastIgnOn, acct, tmz, bpl);
                } else {
                    // -- Ignition Off, or Unknown
                    return i18n.getString("Device.notAvailable", "n/a");
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_LAST_IGN_OFF)) {
            if (getTitle) {
                return i18n.getString("Device.key.lastIgnitionOff", "Ignition Off\nTime");
            } else {
                long lastIgnOn  = dev.getLastIgnitionOnTime();
                long lastIgnOff = dev.getLastIgnitionOffTime();
                if ((lastIgnOff > 0L) && (lastIgnOff > lastIgnOn)) {
                    // -- Ignition Off
                    Account acct = dev.getAccount();
                    TimeZone tmz = (acct != null)? acct.getTimeZone(null) : DateTime.getGMTTimeZone();
                    return EventData.getTimestampString(lastIgnOff, acct, tmz, bpl);
                } else {
                    // -- Ignition On, or Unknown
                    return i18n.getString("Device.notAvailable", "n/a");
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_IGNITION_STATE)) {
            if (getTitle) {
                return i18n.getString("Device.key.ignitionState", "Ignition\nState");
            } else {
                long lastIgnOn  = dev.getLastIgnitionOnTime();
                long lastIgnOff = dev.getLastIgnitionOffTime();
                if ((lastIgnOn > 0L) && (lastIgnOn > lastIgnOff)) {
                    // -- Ignition On
                    return i18n.getString("Device.ignitionOn", "On");
                } else
                if ((lastIgnOff > 0L) && (lastIgnOff > lastIgnOn)) {
                    // -- Ignition Off
                    return i18n.getString("Device.ignitionOff", "Off");
                } else {
                    // -- Ignition Unknown
                    return i18n.getString("Device.ignitionUnknown", "Unkown");
                }
            }
        }

        /* Misc */
        if (EventData._keyMatch(key,Device.KEY_STOP_ELAPSED)) {
            if (getTitle) {
                return i18n.getString("Device.key.stopElapsed", "Stop Elapsed");
            } else {
                long    startTime   = dev.getLastStartTime();
                long    stopTime    = dev.getLastStopTime();
                boolean isStopped   = ((stopTime > 0L) && (stopTime > startTime));
                long    stopDelta   = isStopped? (DateTime.getCurrentTimeSec() - stopTime) : 0L;
                if (stopDelta <= 0L) {
                    return "";
                } else {
                    return StringTools.formatElapsedSeconds(stopDelta,StringTools.ELAPSED_FORMAT_HHMMSS);
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_SPEED_LIMIT)) {
            if (getTitle) {
                return i18n.getString("Device.key.speedLimit", "Speed Limit");
            } else {
                double kph = dev.getSpeedLimitKPH();
                if (kph <= 0.0) { // still <= 0.0
                    return i18n.getString("Device.notAvailable", "n/a");
                }
                Account account = dev.getAccount();
                if (account != null) {
                    return account.getSpeedString(kph,true,locale);
                } else {
                    return StringTools.format(kph,"0") + " " + Account.SpeedUnits.KPH.toString(locale);
                }
            }
        }

        /* reminder */
        if (EventData._keyMatch(key,Device.KEY_REMINDER)) {
            if (getTitle) {
                return i18n.getString("Device.key.reminder", "Reminder");
            } else {
                String reminder = dev.getReminderMessage();
                return StringTools.trim(reminder);
            }
        }

        /* command state */
        if (EventData._keyMatch(key,Device.KEY_COMMAND_STATE)) {
            int bitNdx = StringTools.parseInt(arg,0);
            if (getTitle) {
                return i18n.getString("Device.key.commandState", "Command State #{0}", String.valueOf(bitNdx));
            } else {
                return dev.getCommandStateMaskBit(bitNdx)? 
                    AccountRecord.GetSimpleLocalString("true" ,locale) :
                    AccountRecord.GetSimpleLocalString("false",locale);
            }
        }

        /* ping time */
        if (EventData._keyMatch(key,Device.KEY_COMMAND_TIME)) {
            if (getTitle) {
                return i18n.getString("Device.key.commandTime", "Command Time");
            } else {
                long T = dev.getLastPingTime();
                Account acct = dev.getAccount();
                //TimeZone tmz = dev.getTimeZone(null);
                TimeZone tmz = (acct != null)? acct.getTimeZone(null) : DateTime.getGMTTimeZone();
                return EventData.getTimestampString(T, acct, tmz, bpl);
            }
        }

        /* ack time */
        if (EventData._keyMatch(key,Device.KEY_ACK_DATETIME)) {
            if (getTitle) {
                return i18n.getString("Device.key.ackTime", "Ack Time");
            } else {
                long T = dev.getLastAckTime();
                Account acct = dev.getAccount();
                //TimeZone tmz = dev.getTimeZone(null);
                TimeZone tmz = (acct != null)? acct.getTimeZone(null) : DateTime.getGMTTimeZone();
                return EventData.getTimestampString(T, acct, tmz, bpl);
            }
        }

        /* maintenance fields */
        if (EventData._keyMatch(key,Device.KEY_MAINT_DESC)) {
            if (getTitle) {
                return i18n.getString("Device.key.maintDesc", "Maint. #");
            } else {
                int ndx = StringTools.parseInt(arg, dev.getMaintTriggeredKM());
                String desc = dev.getMaintDescriptionKM(ndx);
                return StringTools.trim(desc);
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_MAINT_ODOMETER)) {
            if (getTitle) {
                return i18n.getString("Device.key.maintOdometer", "Maint. Odom");
            } else {
                int ndx = StringTools.parseInt(arg, dev.getMaintTriggeredKM());
                double odomKM = dev.getMaintOdometerKM(ndx);
                Account acct = dev.getAccount();
                if (acct != null) {
                    return acct.getDistanceString(odomKM, true, locale);
                } else {
                    return StringTools.format(odomKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_MAINT_INTERVAL)) {
            if (getTitle) {
                return i18n.getString("Device.key.maintInterval", "Maint. Intrv");
            } else {
                int ndx = StringTools.parseInt(arg, dev.getMaintTriggeredKM());
                double intrvKM = dev.getMaintIntervalKM(ndx);
                Account acct = dev.getAccount();
                if (acct != null) {
                    return acct.getDistanceString(intrvKM, true, locale);
                } else {
                    return StringTools.format(intrvKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_MAINT_DIST_NEXT)) {
            if (getTitle) {
                return i18n.getString("Device.key.maintDistNext", "Distance To Maint");
            } else {
                int ndx = StringTools.parseInt(arg, dev.getMaintTriggeredKM());
                double odomKM   = dev.getLastOdometerKM();
                double maintKM  = dev.getMaintOdometerKM(ndx);
                double intervKM = dev.getMaintIntervalKM(ndx); // next interval
                double nextKM   = maintKM + intervKM;
                double deltaKM  = nextKM - odomKM; // <0 if past due
                if (deltaKM <= 0.0) {
                    return i18n.getString("Device.key.maintDistNext.pastDue", "Past Due");
                } else {
                    Account acct = dev.getAccount();
                    if (acct != null) {
                        return acct.getDistanceString(deltaKM, true, locale);
                    } else {
                        return StringTools.format(deltaKM,"0") + " " + Account.DistanceUnits.KM.toString(locale);
                    }
                }
            }
        } else
        if (EventData._keyMatch(key,Device.KEY_MAINT_NOTES)) {
            if (getTitle) {
                return i18n.getString("Device.key.maintNotes", "Maint. Notes");
            } else {
                String notes = dev.getMaintNotes();
                return StringTools.trim(notes);
            }
        }

        /* Fixed contact phone [2.6.3-B67] */
        if (EventData._keyMatch(key,Device.KEY_DEVICE_PHONE)) {
            if (getTitle) {
                return i18n.getString("Device.key.fixedContactPhone", "Phone");
            } else {
                String ph = dev.getFixedContactPhone();
                if (StringTools.isBlank(ph)) {
                    return "";
                } else
                if (StringTools.isBlank(arg)      || 
                    arg.equalsIgnoreCase("plain")   ) {
                    return ph;
                } else
                if (arg.equalsIgnoreCase("a")   ||  // "anchor"
                    arg.equalsIgnoreCase("html")||
                    arg.equalsIgnoreCase("link")  ) {
                    return EventUtil.MAP_ESCAPE_HTML+"<a href='tel:"+ph+"' target='_blank'>"+ph+"</a>";
                } else {
                    return ph;
                }
            }
        } 

        /* RuleFactory */
        if (EventData._keyMatch(key,Device.KEY_EVAL)) {
            if (getTitle) {
                return i18n.getString("Device.key.value", "Value");
            } else {
                // -- rule selector
                String ruleSel = arg; // rule selector to evaluate
                if (StringTools.isBlank(ruleSel)) {
                    return "";
                }
                // -- RuleFactory
                RuleFactory rf = Device.getRuleFactory();
                if ((rf == null) || !rf.checkRuntime()) {
                    Print.logError("Invalid RuleFactory");
                    return "";
                }
                // -- evaluate and return result
                try {
                    Object result = rf.evaluateSelector(ruleSel,dev);
                    if (result == null) {
                        return "";
                    } else {
                        return result.toString();
                    }
                } catch (RuleParseException rpe) {
                    Print.logError("Unable to parse Rule selector: " + arg);
                    return "";
                }
            }
        }

        /* custom */
        if (EventData._keyMatch(key,Device.KEY_CUSTOM)) {
            if (getTitle) {
                if (!StringTools.isBlank(arg) && (bpl != null)) {
                    String K = BasicPrivateLabel.PROP_DeviceInfo_custom_ + arg;
                    String D = bpl.getStringProperty(K, null);
                    if (!StringTools.isBlank(D)) {
                        return D;
                    }
                }
                return i18n.getString("Device.key.custom", "Custom");
            } else {
                String value = dev.getCustomAttribute(arg);
                return StringTools.trim(value);
            }
        }

        /* Device fields */
        if (getTitle) {
            DBField dbFld = Device.getFactory().getField(key);
            if (dbFld != null) {
                return dbFld.getTitle(locale);
            }
            // -- field not found
        } else {
            String fldName = dev.getFieldName(key); // this gets the field name with proper case
            DBField dbFld = (fldName != null)? dev.getField(fldName) : null;
            if (dbFld != null) {
                Object val = dev.getFieldValue(fldName); // straight from table
                if (val == null) { val = dbFld.getDefaultValue(); }
                Account account = dev.getAccount();
                if (account != null) {
                    val = account.convertFieldUnits(dbFld, val, true/*inclUnits*/, locale);
                    return StringTools.trim(val);
                } else {
                    return dbFld.formatValue(val);
                }
            }
            // -- field not found
        }

        /* try temporary properties */
        //Print.logWarn("Key not yet found: " + key);
        if ((dev != null) && dev.hasTemporaryProperties()) {
            RTProperties rtp = dev.getTemporaryProperties();
            Object text = (rtp != null)? rtp.getProperty(key,null) : null;
            if (text instanceof I18N.Text) {
                if (getTitle) {
                    // -- all we have is the key name for the title
                    return key;
                } else {
                    return ((I18N.Text)text).toString(locale);
                }
            }
        }

        // ----------------------------
        // Device key not found

        /* try Account */
        if (getTitle) {
            return Account._getKeyFieldString(
                true/*title*/, key, arg,
                locale, null/*BasicPrivateLabel*/, null/*Account*/);
        } else {
            return Account._getKeyFieldString(
                false/*value*/, key, arg,
                locale, bpl, dev.getAccount());
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Comparator<Device> devDescComparator = null;

    /**
    *** Gets the Device Description Sort Comparator
    *** @return The Device Description Sort Comparator
    **/
    public static Comparator<Device> getDeviceDescriptionComparator()
    {
        if (devDescComparator == null) {
            devDescComparator = new DeviceDescriptionComparator(); // ascending
        }
        return devDescComparator;
    }

    /**
    *** Comparator optimized for EventData device description 
    **/
    public static class DeviceDescriptionComparator
        implements Comparator<Device>
    {
        private boolean ascending = true;
        public DeviceDescriptionComparator() {
            this(true);
        }
        public DeviceDescriptionComparator(boolean ascending) {
            this.ascending  = ascending;
        }
        public int compare(Device dv1, Device dv2) {
            // assume we are comparing Device records
            if (dv1 == dv2) {
                return 0; // exact same object (or both null)
            } else 
            if (dv1 == null) {
                return this.ascending? -1 :  1; // null < non-null
            } else
            if (dv2 == null) {
                return this.ascending?  1 : -1; // non-null > null
            } else {
                String D1 = dv1.getDescription().toLowerCase();
                String D2 = dv2.getDescription().toLowerCase();
                return this.ascending? D1.compareTo(D2) : D2.compareTo(D1);
            }
        }
        public boolean equals(Object other) {
            if (other instanceof DeviceDescriptionComparator) {
                DeviceDescriptionComparator ddc = (DeviceDescriptionComparator)other;
                return (this.ascending == ddc.ascending);
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]           = new String[] { "account"   , "acct"  , "a" };
    private static final String ARG_DEVICE[]            = new String[] { "device"    , "dev"   , "d" };
    private static final String ARG_UNIQID[]            = new String[] { "uniqueid"  , "unique", "uniq", "uid", "u" };
    private static final String ARG_CREATE[]            = new String[] { "create"                   };
    private static final String ARG_EDIT[]              = new String[] { "edit"      , "ed"         };
    private static final String ARG_EDITALL[]           = new String[] { "editall"   , "eda"        };
    private static final String ARG_DELETE[]            = new String[] { "delete"                   };
    private static final String ARG_EVENTS[]            = new String[] { "events"    , "ev"         };
    private static final String ARG_FORMAT[]            = new String[] { "format"    , "fmt"        };
  //private static final String ARG_SETPROP[]           = new String[] { "setprop"                  };
    private static final String ARG_INSERT[]            = new String[] { "insertGP"                 };
    private static final String ARG_CLEARACK[]          = new String[] { "clearAck"                 };
    private static final String ARG_RESET_ACCUM[]       = new String[] { "resetAccum", "resetAccumulators" };
    private static final String ARG_MAINTKM[]           = new String[] { "maint"     , "maintkm"    };
    private static final String ARG_CHECKRULES[]        = new String[] { "checkRules", "ckRules"    };
    private static final String ARG_RESET_ODOM[]        = new String[] { "resetOdom"                };
    private static final String ARG_SEND_COMMAND[]      = new String[] { "sendCmd"                  };
    private static final String ARG_SEND_COMMAND_ARG[]  = new String[] { "sendCmdArg"               };
    private static final String ARG_CNT_FUTURE_EV[]     = new String[] { "countFutureEvents"        };
    private static final String ARG_DEL_FUTURE_EV[]     = new String[] { "deleteFutureEvents"       };
    private static final String ARG_CNT_OLD_EV[]        = new String[] { "countOldEvents"           };
    private static final String ARG_DEL_OLD_EV[]        = new String[] { "deleteOldEvents"          };
    private static final String ARG_CONFIRM_DEL[]       = new String[] { "confirmDelete"            };
    private static final String ARG_ZONECHECK[]         = new String[] { "zoneCheck"                };
    private static final String ARG_SIM_PHONE[]         = new String[] { "simPhone"                 };
    private static final String ARG_FIND_NEARBY[]       = new String[] { "findNearby", "nearby"     };
    private static final String ARG_ACTIVE_SESSION[]    = new String[] { "activeSession"            };

    /**
    *** Convenience for creating a combined account/device description
    *** @param acctID  The Account ID
    *** @param devID   The Device ID
    *** @return The combined account/device description
    **/
    private static String _fmtDevID(String acctID, String devID)
    {
        return acctID + "/" + devID;
    }

    /**
    *** Usage display
    **/
    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + Device.class.getName() + " {options}");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("  -account=<id>               Acount ID which owns Device");
        Print.sysPrintln("  -device=<id>                Device ID to create/edit");
        Print.sysPrintln("  -uniqueid=<id>              Unique ID to create/edit");
        Print.sysPrintln("");
        Print.sysPrintln("  -create                     Create a new Device");
        Print.sysPrintln("  -edit                       Edit an existing (or newly created) Device");
        Print.sysPrintln("  -delete                     Delete specified Device");
        Print.sysPrintln("");
        Print.sysPrintln("  -events=<limit>             Retrieve the last <limit> events");
        Print.sysPrintln("  -ckRules=<lat>/<lon>,<sc>   Check rule (may change db!)");
        Print.sysPrintln("");
        Print.sysPrintln("  -countFutureEvents=<sec>    Count events beyond (now + sec) into the future");
        Print.sysPrintln("  -deleteFutureEvents=<sec>   Delete events beyond (now + sec) into the future");
        Print.sysPrintln("");
        Print.sysPrintln("  -countOldEvents=<time>      Count events before specified time (requires '-confirm')");
        Print.sysPrintln("  -deleteOldEvents=<time>     Delete events before specified time (requires '-confirm')");
        Print.sysPrintln("  -confirm                    Confirms countOldEvents/deleteOldEvents");
        Print.sysPrintln("");
        Print.sysPrintln("  -zoneCheck=<GP1>/<GP2>      Geozone transition check");
        System.exit(1);
    }

    /**
    *** Main entry point for Device command-line tools
    *** @param args  The main entry point arguments
    **/
    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String devID   = RTConfig.getString(ARG_DEVICE , "");
        String uniqID  = RTConfig.getString(ARG_UNIQID , "");

        /* get account */
        Account acct = null;
        boolean acctExists = false;
        if (!StringTools.isBlank(acctID)) {
            try {
                acct = Account.getAccount(acctID); // may throw DBException
                if (acct != null) {
                    acctExists = true;
                } else {
                    // Account specified, but does not exist
                    acctExists = false;
                    Print.logError("Account-ID does not exist: " + acctID);
                    usage();
                }
            } catch (DBException dbe) {
                Print.logException("Error loading Account: " + acctID, dbe);
                //dbe.printException();
                System.exit(99);
            }
        } else {
            //Print.logError("Account-ID not specified.");
            //usage();
        }
        BasicPrivateLabel privLabel = (acct != null)? acct.getPrivateLabel() : null;

        /* device exists? */
        boolean deviceExists = false;
        if (!StringTools.isBlank(devID)) {
            if (acctExists) {
                try {
                    deviceExists = Device.exists(acctID, devID);
                } catch (DBException dbe) {
                    Print.logError("Error determining if Device exists: " + _fmtDevID(acctID,devID));
                    System.exit(99);
                }
            } else {
                Print.logError("Account-ID not specified, or does not exist");
                usage();
            }
        } else {
            //Print.logError("Device-ID not specified.");
            //usage();
        }

        /* get device if it exists */
        Device deviceRcd = null;
        if (deviceExists) {
            try {
                deviceRcd = Device.getDevice(acct, devID, false); // may throw DBException
            } catch (DBException dbe) {
                Print.logError("Error getting Device: " + _fmtDevID(acctID,devID));
                dbe.printException();
                System.exit(99);
            }
        }

        /* option count */
        int opts = 0;

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && 
            !StringTools.isBlank(acctID) && !StringTools.isBlank(devID)) {
            opts++;
            if (!acctExists || !deviceExists) {
                Print.logWarn("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                Device.Key devKey = new Device.Key(acctID, devID);
                devKey.delete(true); // also delete dependencies
                Print.logInfo("Device deleted: " + _fmtDevID(acctID,devID));
                deviceExists = false;
            } catch (DBException dbe) {
                Print.logError("Error deleting Device: " + _fmtDevID(acctID,devID));
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (!acctExists) {
                Print.logError("Account does not exist: " + acctID);
            } else
            if (deviceExists) {
                Print.logWarn("Device already exists: " + _fmtDevID(acctID,devID));
            } else {
                try {
                    Device.createNewDevice(acct, devID, uniqID);
                    Print.logInfo("Created Device: " + _fmtDevID(acctID,devID));
                    deviceExists = true;
                } catch (DBException dbe) {
                    Print.logError("Error creating Device: " + _fmtDevID(acctID,devID));
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false) || RTConfig.getBoolean(ARG_EDITALL,false)) {
            opts++;
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    DBEdit editor = new DBEdit(deviceRcd);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                }
            }
            System.exit(0);
        }

        /* events */
        if (RTConfig.hasProperty(ARG_EVENTS)) {
            opts++;
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
            } else {
                int limit = RTConfig.getInt(ARG_EVENTS, 10);
                int fmt   = EventUtil.parseOutputFormat(RTConfig.getString(ARG_FORMAT,null),EventUtil.FORMAT_CSV);
                try {
                    EventData evList[] = deviceRcd.getLatestEvents(limit,false);
                    deviceRcd.setSavedRangeEvents(evList);
                    java.util.List<Device> devList = new Vector<Device>();
                    devList.add(deviceRcd);
                    EventUtil evUtil = EventUtil.getInstance();
                    evUtil.writeEvents((PrintWriter)null, 
                        acct, devList, 
                        fmt, true/*allTags*/, null/*timezone*/,
                        privLabel);
                } catch (IOException ioe) {
                    Print.logError("IO Error");
                } catch (DBException dbe) {
                    Print.logError("Error getting events for Device: " + _fmtDevID(acctID,devID));
                    dbe.printException();
                }
            }
            System.exit(0);
        }

        /* zone check */
        if (RTConfig.hasProperty(ARG_ZONECHECK)) {
            opts++;
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
            } else {
                String gpStr[] = StringTools.split(RTConfig.getString(ARG_ZONECHECK,""),',');
                GeoPoint gp1 = (gpStr.length > 0)? new GeoPoint(gpStr[0]) : GeoPoint.INVALID_GEOPOINT;
                GeoPoint gp2 = (gpStr.length > 1)? new GeoPoint(gpStr[1]) : GeoPoint.INVALID_GEOPOINT;
                long fixtime = DateTime.getCurrentTimeSec();
                deviceRcd.setLastValidLocation(fixtime, 0L/*age*/, gp1, 0.0/*speed*/, 0.0/*heading*/); // NOT SAVED!
                java.util.List<Device.GeozoneTransition> zone = deviceRcd.checkGeozoneTransitions(fixtime, gp2);
                if (ListTools.size(zone) > 0) {
                    for (Device.GeozoneTransition z : zone) {
                        Print.sysPrintln("Zone Transition: " + z);
                    }
                } else {
                    Print.sysPrintln("Not in a Geozone ...");
                }
            }
            System.exit(0);
        }

        /* insert GeoPoint */
        if (RTConfig.hasProperty(ARG_INSERT)) {
            opts++;
            GeoPoint gp = new GeoPoint(RTConfig.getString(ARG_INSERT,""));
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(1);
            } else
            if (!gp.isValid()) {
                Print.logError("Invalid GeoPoint: " + gp);
                System.exit(1);
            } else {
                // 'privLabel' non-null here
                SendMail.SetThreadModel(SendMail.THREAD_DEBUG);
                Print.sysPrintln("Account PrivateLabel: " + privLabel.getName());
                ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
                if (INSERT_REVERSEGEOCODE_REQUIRED && (rgp == null)) {
                    Print.sysPrintln("Account has no ReverseGeocodeProvider (record not inserted)");
                    System.exit(1);
                }
                Print.sysPrintln("Account ReverseGeocodeProvider: " + ((rgp!=null)?rgp.getName():"<none>"));
                if (INSERT_REVERSEGEOCODE_REQUIRED && !Account.getGeocoderMode(acct).equals(Account.GeocoderMode.FULL)) {
                    Print.sysPrintln("Overriding Account GeocoderMode to 'FULL'");
                    acct.setGeocoderMode(Account.GeocoderMode.FULL);
                }
                // insert
                long timestamp = DateTime.getCurrentTimeSec();
                int statusCode = StatusCodes.STATUS_WAYMARK_0;
                EventData.Key evKey = new EventData.Key(acctID,devID,timestamp,statusCode);
                EventData evRcd = evKey.getDBRecord();
                evRcd.setGeoPoint(gp);
                evRcd.setAddress(null); // updated later
                if (deviceRcd.insertEventData(evRcd)) {
                    Print.sysPrintln("EventData record inserted ...");
                } else {
                    Print.logError("*** Unable to insert EventData record!!!");
                }
                ThreadPool_DeviceEventUpdate.stopThreads();
                if (ThreadPool_DeviceEventUpdate.getPoolSize() > 0) {
                    do {
                        Print.sysPrintln("Waiting for background threads to complete ...");
                        try { Thread.sleep(3000L); } catch (Throwable t) {}
                    } while (ThreadPool_DeviceEventUpdate.getPoolSize() > 0);
                }
                Print.sysPrintln("... done");
                System.exit(0);
            }
        }

        /* clear any pending ACK */
        if (RTConfig.hasProperty(ARG_CLEARACK)) {
            opts++;
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // clear ack
            boolean didClear = deviceRcd.clearExpectCommandAck(false/*didAck*/,true/*update*/);
            Print.logInfo("Cleared Device ACK: " + didClear);
            System.exit(0);
        }

        /* clear/reset all device accumulators and timestamps */
        if (RTConfig.hasProperty(ARG_RESET_ACCUM)) {
            opts++;
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // clear accumulators
            final Device device = deviceRcd;
            // --
            device.setLastEngineOnHours(0.0);
            device.setLastEngineOnTime(0L);
            device.setLastEngineOffTime(0L);
            device.setLastEngineHours(0.0);
            // --
            device.setLastPtoOnHours(0.0);
            device.setLastPtoOnTime(0L);
            device.setLastPtoOffTime(0L);
            device.setLastPtoHours(0.0);
            // --
            device.setLastIgnitionOnHours(0.0);
            device.setLastIgnitionOnTime(0L);
            device.setLastIgnitionOffTime(0L);
            device.setLastIgnitionHours(0.0);
            // --
            device.setLastStopTime(0L);
            device.setLastStartTime(0L);
            device.setLastOdometerKM(0.0);
            device.setLastDistanceKM(0.0);
            device.setLastValidLatitude(0.0);
            device.setLastValidLongitude(0.0);
            device.setLastValidSpeedKPH(0.0);
            device.setLastValidHeading(0.0);
            device.setLastGPSTimestamp(0L);
            // --
            device.setLastNotifyTime(0L);
            device.setLastNotifyCode(StatusCodes.STATUS_NONE);
            device.setLastNotifyRule(null);
            // --
            device.setLastInputState(0x00L);
            device.setLastOutputState(0x00L);
            // --
            device.setLastBatteryLevel(0.0);
            device.setLastFuelLevel(0.0);
            device.setLastFuelLevel2(0.0);
            device.setLastFuelTotal(0.0);
            device.setLastOilLevel(0.0);
            device.setLastTcpSessionID(null);
            device.setLastEventTimestamp(0L);
            device.setLastEventsPerSecond(0.0);
            device.setLastEventsPerSecondMS(0L);
            device.setLastCellServingInfo(null);
            device.setLastMalfunctionLamp(false);
            device.setLastFaultCode(null);
            device.setLastPingTime(0L);
            device.setTotalPingCount(0);
            device.setLastAckCommand(null);
            device.setLastAckResponse(null);
            device.setLastAckTime(0L);
            device.setLastDuplexConnectTime(0L);
            device.setLastTotalConnectTime(0L);
            device.setLastServiceTime(0L);
            // --
          //device.setLastBorderCrossTime(0L);
          //device.setLastELogState(null);
          //device.setLastDataPushTime(0L);
          //device.setLastEventCreateMillis(0L);
            try {
                device.save();
                Print.logInfo("Cleared Device accumulators");
                System.exit(0);
            } catch (DBException dbe) {
                Print.logException("Unable to clear Device accumulators", dbe);
                System.exit(99);
            }
        }

        /* periodic maintenance check */
        if (RTConfig.hasProperty(ARG_MAINTKM)) {
            opts++;
            // device exists?
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // maintenance index
            int ndx = 0;
            // odometer/interval
            double odomKM   = deviceRcd.getLastOdometerKM();
            double intervKM = deviceRcd.getMaintIntervalKM(ndx);
            if ((odomKM <= 0.0) || (intervKM <= 0.0)) {
                System.exit(2); // no odometer/interval
            }
            // check service maintenance interval
            double maintKM = deviceRcd.getMaintOdometerKM(ndx);
            if (odomKM >= (maintKM + intervKM)) {
                // send email ('privLabel' non-null here)
                String devDesc   = deviceRcd.getDescription();
                String maintDesc = deviceRcd.getMaintDescriptionKM(ndx);
                Print.logInfo("'"+maintDesc+"' Service Interval due for " + deviceRcd.getDescription());
                SmtpProperties smtpProps = acct.getSmtpProperties(null);
                String frEmail = smtpProps.getFromEmailType("notify");
                String toEmail = RTConfig.getString(ARG_MAINTKM, "");
                if (!StringTools.isBlank(frEmail) && !StringTools.isBlank(toEmail)) {
                    I18N   i18n = I18N.getI18N(Device.class, acct.getLocale());
                    String text = i18n.getString("Device.serviceMaint.dueFor","Periodic Maintenance({1}) due for {0}",devDesc,maintDesc);
                    String odom = i18n.getString("Device.serviceMaint.odometer","Odometer");
                    String subj = text;
                    String body = text + "\n" +
                        odom + ": " + odomKM + "\n" +
                        "\n";
                    try {
                        Print.logInfo("From:"     + frEmail);
                        Print.logInfo("To:"       + toEmail);
                        Print.logInfo("Subject: " + subj);
                        Print.logInfo("Body:\n"   + body);
                        Print.logInfo("Sending email ...");
                        SendMail.SetThreadModel(SendMail.THREAD_CURRENT);
                        boolean retry = false;
                        SendMail.send(frEmail,toEmail,null,null,subj,body,null,smtpProps,retry);
                        // SystemAudit.sentRuleNotification(acctID, null, devID, subj);
                        System.exit(0); // success
                    } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                        // this will fail if JavaMail support for SendMail is not available.
                        Print.logWarn("SendMail error: " + t);
                        System.exit(97);
                    }
                }
                System.exit(1);
            } else {
                System.exit(2); // no interval
            }
        }

        /* debug rule check ["-checkRules"] */
        if (RTConfig.hasProperty(ARG_CHECKRULES)) {
            opts++;
            // -- args "<lat>/<lon>[,<code>]"
            String crArgs[] = StringTools.split(RTConfig.getString(ARG_CHECKRULES,""),',');
            if (crArgs.length < 1) {
                Print.logError("Invalid 'checkRules' arguments ['lat/lon,code']");
                System.exit(99);
            }
            GeoPoint gp = new GeoPoint(crArgs[0]);
            int    code = StatusCodes.ParseCode(ListTools.itemAt(crArgs,1,""),null,StatusCodes.STATUS_WAYMARK_0);
            // -- account/device exists?
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            } else
            if (!gp.isValid()) {
                Print.logError("Invalid GeoPoint: " + gp);
                System.exit(98);
            }
            // -- sample event
            long timestamp = DateTime.getCurrentTimeSec();
            EventData.Key evKey = new EventData.Key(acctID, devID, timestamp, code);
            EventData evRcd = evKey.getDBRecord();
            evRcd.setDevice(deviceRcd);
            evRcd.setGeoPoint(gp);
            evRcd.setAddress(null);
            Print.logInfo("Created Event: " + _fmtDevID(acctID,devID) + " " + gp + 
                " [" + StatusCodes.GetHex(code) + ":" + StatusCodes.GetDescription(code,null) + "]");
            // -- check rules
            if (!deviceRcd.checkEventRules(evRcd)) { // command-line
                Print.logWarn("No rules triggered ...");
            }
            // -- stop (email, etc)
            ThreadPool_DeviceEventUpdate.stopThreads();
            if (ThreadPool_DeviceEventUpdate.getPoolSize() > 0) {
                do {
                    Print.sysPrintln("Waiting for background threads to complete ...");
                    try { Thread.sleep(3000L); } catch (Throwable t) {}
                } while (ThreadPool_DeviceEventUpdate.getPoolSize() > 0);
            }
            Print.sysPrintln("... done");
            System.exit(0);
        }

        /* count future events */
        if (RTConfig.hasProperty(ARG_CNT_FUTURE_EV)) {
            opts++;
            // Device must exist
            if (!acctExists || !deviceExists) {
                Print.logError("Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // arg seconds
            long sec = RTConfig.getLong(ARG_CNT_FUTURE_EV,0L);
            // count future events
            long nowTime    = DateTime.getCurrentTimeSec();
            long futureTime = nowTime + sec;
            Print.sysPrintln("Counting events after \"" + (new DateTime(futureTime)) + "\" ...");
            try {
                long rcdCount = EventData.getRecordCount(acctID,devID,futureTime,-1L); // -1 for InnoDB?
                if (rcdCount < 0L) { // InnoDB
                    Print.sysPrintln("Unable to determine number of future events (EventData table is InnoDB?)");
                } else
                if (rcdCount == 0L) {
                    Print.sysPrintln("No future events found");
                } else {
                    Print.sysPrintln("Found "+rcdCount+" future events");
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logError("Error counting future events: " + dbe);
                System.exit(99);
            }
        }

        /* delete future events */
        if (RTConfig.hasProperty(ARG_DEL_FUTURE_EV)) {
            opts++;
            // Device must exist
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // arg seconds
            long sec = RTConfig.getLong(ARG_DEL_FUTURE_EV,0L);
            if (sec < 60L) {
                Print.logError("Specified seconds must be >= 60");
                System.exit(99);
            }
            // delete future events
            long nowTime    = DateTime.getCurrentTimeSec();
            long futureTime = nowTime + sec;
            Print.sysPrintln("Deleting events after \"" + (new DateTime(futureTime)) + "\" ...");
            try {
                long delCount = EventData.deleteFutureEvents(acctID, devID, futureTime);
                if (delCount <= 0L) {
                    Print.sysPrintln("No future events found");
                } else {
                    Print.sysPrintln("Deleted "+delCount+" future events");
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logError("Error deleting future events: " + dbe);
                System.exit(99);
            }
        }

        /* count/delete old events */
        if (RTConfig.hasProperty(ARG_CNT_OLD_EV) || 
            RTConfig.hasProperty(ARG_DEL_OLD_EV)   ) {
            opts++;
            // -- delete/count
            boolean deleteEvents = RTConfig.hasProperty(ARG_DEL_OLD_EV);
            String actionText = deleteEvents? "Deleting" : "Counting";
            // -- account/device must exist
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // -- arg time
            TimeZone acctTMZ = acct.getTimeZone(null);
            String   argTime = deleteEvents?
                RTConfig.getString(ARG_DEL_OLD_EV,"") :
                RTConfig.getString(ARG_CNT_OLD_EV,"");
            DateTime oldTime = null; // delete/count prior to this date
            if (StringTools.isBlank(argTime)) {
                Print.logError("Invalid time specification: " + argTime);
                System.exit(98);
            } else
            if (argTime.equalsIgnoreCase("current")) {
                oldTime = new DateTime(acctTMZ);
            } else {
                try {
                    oldTime = DateTime.parseArgumentDate(argTime,acctTMZ,true); // end of day default
                } catch (DateTime.DateParseException dpe) {
                    oldTime = null;
                }
                if (oldTime == null) {
                    Print.sysPrintln("Invalid Time specification: " + argTime);
                    System.exit(98);
                } else
                if (oldTime.getTimeSec() > DateTime.getCurrentTimeSec()) {
                    Print.sysPrintln(actionText + " future events not allowed");
                    System.exit(98);
                }
            }
            // -- adjust deletion time
            boolean usingRetainedDate = false;
            long oldTimeSec = oldTime.getTimeSec(); // delete/count prior to this date
            if (deleteEvents) {
                long delOldTimeSec = deleteEvents? acct.adjustRetainedEventTime(oldTimeSec) : oldTimeSec;
                if (delOldTimeSec != oldTimeSec) {
                    oldTimeSec = delOldTimeSec;
                    usingRetainedDate = true;
                }
            }
            if (usingRetainedDate) {
                Print.sysPrintln(actionText + " events before \"" + (new DateTime(oldTimeSec)) + "\" (retained-date) ...");
            } else {
                Print.sysPrintln(actionText + " events before \"" + (new DateTime(oldTimeSec)) + "\" ...");
            }
            // count/delete old events
            boolean confirmDel = RTConfig.getBoolean(ARG_CONFIRM_DEL,false);
            try {
                String _devIDStr = devID;
                if (deleteEvents) {
                    if (!confirmDel) {
                        Print.sysPrintln("ERROR: Missing '-confirmDelete', aborting delete ...");
                        System.exit(1);
                    }
                    StringBuffer sbMsg = new StringBuffer();
                    long delCount = deviceRcd.deleteOldEvents(oldTimeSec,sbMsg); // -1 for InnoDB
                    String delMsg = (sbMsg.length() > 0)? (" ("+sbMsg+")") : "";
                    if (delCount >= 0L) {
                        Print.sysPrintln("  Device: " + _devIDStr + " - Deleted " + delCount + " old events" + delMsg);
                    } else
                    if (delCount < 0L) {
                        Print.sysPrintln("  Device: " + _devIDStr + " - Deleted old events (InnoDB?)" + delMsg);
                    }
                } else {
                    long rcdCount = deviceRcd.countOldEvents(oldTimeSec);
                    if (rcdCount >= 0L) {
                        Print.sysPrintln("  Device: " + _devIDStr + " - Counted " + rcdCount + " old events");
                    } else
                    if (rcdCount < 0L) {
                        Print.sysPrintln("  Device: " + _devIDStr + " - Unable to count events (InnoDB?)");
                    }
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logError("Error " + actionText + " old events: " + dbe);
                System.exit(99);
            }
        }

        /* reset odometer */
        if (RTConfig.hasProperty(ARG_RESET_ODOM)) {
            opts++;
            // args "timestamp"
            long resetTime = RTConfig.getLong(ARG_RESET_ODOM,0L);
            // account/device exists?
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // reset odometer for device events
            final AccumulatorLong count = new AccumulatorLong();
            final Device device = deviceRcd;
            double lastOdomKM = device.getLastOdometerKM();
            device.setLastOdometerKM(0.0);
            DBRecordHandler<EventData> odomResetHandler = new DBRecordHandler<EventData>() {
                private boolean   firstZeroOdom  = false;
                private EventData lastEvent      = null;
                private double    lastOdomKM     = 0.0;
                public int handleDBRecord(EventData rcd) throws DBException {
                    EventData ev = rcd;
                    ev.setDevice(device);
                    ev.setPreviousEventData(this.lastEvent);
                    double evOdomKM = ev.getOdometerKM();
                    long   evTime   = ev.getTimestamp();
                    long   gpsTime  = ev.getGpsTimestamp(); // [2.6.2-B44]
                    double gpsLat   = ev.getLatitude();
                    double gpsLon   = ev.getLongitude();
                    double gpsSpeed = ev.getSpeedKPH();
                    double gpsHead  = ev.getHeading();
                    // found first zero odometer?
                    if (!firstZeroOdom) {
                        // still looking
                        if (evOdomKM > 0.0) {
                            this.lastOdomKM = evOdomKM;
                            this.lastEvent  = ev;
                            device.setLastOdometerKM(this.lastOdomKM);
                            device.setLastValidLatitude(gpsLat);
                            device.setLastValidLongitude(gpsLon);
                            device.setLastValidSpeedKPH(gpsSpeed);
                            device.setLastValidHeading(gpsHead);
                            device.setLastGPSTimestamp(gpsTime);
                            return DBRH_SKIP;
                        }
                        // found it
                        firstZeroOdom = true;
                        if (this.lastEvent == null) {
                            // we've never found a non-zero odometer
                            this.lastOdomKM = evOdomKM; // which is '0.0'
                            this.lastEvent  = ev;
                            device.setLastOdometerKM(this.lastOdomKM); // "0.0"
                            device.setLastValidLatitude(gpsLat);
                            device.setLastValidLongitude(gpsLon);
                            device.setLastValidSpeedKPH(gpsSpeed);
                            device.setLastValidHeading(gpsHead);
                            device.setLastGPSTimestamp(gpsTime);
                        }
                    }
                    // reset event odometer
                    this.lastOdomKM += ev.getGeoPoint().kilometersToPoint(this.lastEvent.getGeoPoint());
                    if ((count.get() % 100L) == 0L) {
                        Print.sysPrintln("Updating Event "+evTime+" (" + (new DateTime(evTime)) + ") ==> " + this.lastOdomKM);
                    }
                    ev.setOdometerKM(this.lastOdomKM);
                    ev.update(EventData.FLD_odometerKM); // may throw DBException
                    this.lastEvent = ev;
                    device.setLastOdometerKM(this.lastOdomKM);
                    device.setLastValidLatitude(gpsLat);
                    device.setLastValidLongitude(gpsLon);
                    device.setLastValidSpeedKPH(gpsSpeed);
                    device.setLastValidHeading(gpsHead);
                    device.setLastGPSTimestamp(gpsTime);
                    count.increment();
                    return DBRH_SKIP;
                }
            };
            try {
                // update events
                // (it's possible that this could run out of memory if this range is too large)
                EventData.getRangeEvents(
                    acctID, devID,
                    resetTime, -1L/*toDateSec*/,
                    null/*statusCodes*/,                            // all status codes
                    true/*validGPS*/,                               // valid GPS only
                    EventData.LimitType.FIRST, -1/*limit*/, true,   // no limit, ascending
                    null/*additionalSelect*/,
                    odomResetHandler);
                // update device record
                device.update(
                    Device.FLD_lastValidLatitude,
                    Device.FLD_lastValidLongitude,
                    Device.FLD_lastValidSpeedKPH,
                    Device.FLD_lastValidHeading,
                    Device.FLD_lastGPSTimestamp,
                    Device.FLD_lastOdometerKM
                    );
                // return number of records updated
                long lastGPSTime = device.getLastGPSTimestamp();
                Print.sysPrintln("Timestamp of last event processed: " + lastGPSTime + " ("+(new DateTime(lastGPSTime))+")");
                long c = count.get();
                if (c == 0L) {
                    Print.sysPrintln("... done (no events updated)");
                } else {
                    Print.sysPrintln("... done (updated "+c+" events)");
                }
            } catch (DBException dbe) {
                Print.logException("Error reading event records: " + acctID + "/" + devID, dbe);
                System.exit(99);
            }
            System.exit(0);
        }

        /* send command */
        // org.opengts.db.tables.Device -account=ACCOUNT -device=DEVICE -sendCmd=COMMAND_ID -sendCmdArg=COMMAND_ARG
        if (RTConfig.hasProperty(ARG_SEND_COMMAND)) {
            opts++;
            // -- device exists?
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // -- send command
            String cmdType   = DCServerConfig.COMMAND_CONFIG;
            String cmdName   = RTConfig.getString(ARG_SEND_COMMAND,"");
            String cmdArgs[] = new String[] { RTConfig.getString(ARG_SEND_COMMAND_ARG,"") }; // [2.6.4-B83]
            boolean success  = deviceRcd.sendDeviceCommand(null, cmdName, cmdArgs);
            Print.sysPrintln("Command Sent: " + success);
            System.exit(0);
        }

        /* find matching SIM phone numbers */
        if (RTConfig.hasProperty(ARG_SIM_PHONE)) {
            opts++;
            String simPhone = RTConfig.getString(ARG_SIM_PHONE,"");
            try {
                java.util.List<String> devList = Device.getDeviceIDsForSimPhoneNumber(simPhone);
                if (!ListTools.isEmpty(devList)) {
                    Print.sysPrintln("Found SIM Phone number: " + simPhone);
                    for (String ad : devList) {
                        Print.sysPrintln("  " + ad);
                    }
                } else {
                    Print.sysPrintln("SIM Phone number not found: " + simPhone);
                }
                System.exit(0);
            } catch (DBException dbe) {
                Print.logException("Getting SIM phone number", dbe);
                System.exit(99);
            }
        }

        /* find nearby */
        // -nearby=1000
        // -nearby=39.1234/-142.1234,2000
        if (RTConfig.hasProperty(ARG_FIND_NEARBY)) {
            opts++;
            // -- Account must exist
            if (!acctExists) {
                Print.sysPrintln("ERROR: Account does not exist: " + acctID);
                System.exit(99);
            }
            // -- parse GeoPoint, RadiusM
            String gpStr[] = StringTools.split(RTConfig.getString(ARG_FIND_NEARBY,""),',');
            GeoPoint  gp   = null;
            double   radM  = 500.0;
            if (gpStr.length > 0) {
                if (gpStr[0].indexOf("/") >= 0) {
                    gp = new GeoPoint(gpStr[0]);
                    if (gpStr.length > 1) {
                        radM = StringTools.parseDouble(gpStr[1],500.0);
                    }
                } else {
                    radM = StringTools.parseDouble(gpStr[0],500.0);
                }
            }
            if (radM < 0.0) {
                Print.sysPrintln("ERROR: Invalid Radius(meters) specified");
                System.exit(99);
            }
            // -- get list of NearbyDevice's
            Map<String,Device.NearbyDevice> nbMap = null;
            try {
                if (deviceExists && !GeoPoint.isValid(gp)) {
                    // -- device specified and GeoPoint is invalid
                    GeoPoint devGP = deviceRcd.getLastValidLocation();
                    Print.sysPrintln("Check Device location: " + devGP + " RadiusM=" + radM);
                    nbMap = Device.GetNearbyDeviceMap(
                        acct, deviceRcd.getDeviceID(), false/*inclThisDev?*/, 
                        -1L/*startTime*/, -1L/*endTime*/,
                        devGP, radM, true/*active*/, null/*User*/, 
                        true/*sort*/);
                } else {
                    // -- no device specified, or a valid GeoPoint was specified
                    Print.sysPrintln("Check specified location: " + gp + " RadiusM=" + radM);
                    nbMap = Device.GetNearbyDeviceMap(
                        acct, null/*targetDevID*/, false/*inclThisDev?*/,
                        -1L/*startTime*/, -1L/*endTime*/,
                        gp, radM, true/*active*/, null/*User*/,
                        true/*sort*/);
                }
            } catch (DBException dbe) {
                Print.logStackTrace("Unable to get list of NearbyDevice's", dbe);
                System.exit(1);
            }
            // -- display list
            if (ListTools.size(nbMap) > 0) {
                for (String nbDevID : nbMap.keySet()) {
                    Device.NearbyDevice nb = nbMap.get(nbDevID);
                    Print.sysPrintln("NearbyDevice: " + nb);
                }
            } else {
                Print.sysPrintln("No NearbyDevices found ...");
            }
            System.exit(0);
        }

        /* active session? */
        if (RTConfig.hasProperty(ARG_ACTIVE_SESSION)) {
            // -- bin/admin.pl Device -account=demo -device=dcstest -activeSession
            opts++;
            // -- Account/Device exists?
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            Device device = deviceRcd;
            // --
            DCServerConfig dcs = device.getDCServerConfig();
            if (dcs == null) {
                Print.logError("Device does not define a DCS module: " + _fmtDevID(acctID,devID));
                System.exit(97);
            }
            // -- 
            String cmdHost = dcs.getCommandDispatcherHost(device);
            int    cmdPort = dcs.getCommandDispatcherPort();
            if (StringTools.isBlank(cmdHost) || (cmdPort <= 0)) {
                Print.logError("DCS does not define a command host/port: " + dcs.getName());
                System.exit(97);
            }
            // -- get session info
            long startMS = DateTime.getCurrentTimeMillis();
            RTProperties sessResp = DCServerFactory.getActiveSessionInfo(device);
            long deltaMS = DateTime.getCurrentTimeMillis() - startMS;
            Print.sysPrintln("SessionID ["+deltaMS+"ms]: " + sessResp);
            // --
            System.exit(0);
        }

        /* reminder test */
        if (RTConfig.hasProperty("reminderTest")) {
            opts++;
            // -- Account/Device exists?
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // -- reset odometer for device events
            Device device = deviceRcd;
            TimeZone tz = acct.getTimeZone(null);
            String remIntStr = RTConfig.getString("reminderTest","");
            long nowTime = DateTime.getCurrentTimeSec();
            REMINDER_LOG = true;
            device.setReminderInterval(remIntStr);
            boolean expired = device.isReminderExpired(tz, nowTime);
            if (expired) {
                try {
                    device.setReminderTime(nowTime);
                    device.update(FLD_reminderTime);
                } catch (DBException dbe) {
                    Print.logException("Error updating Device", dbe);
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* toJSON test */
        if (RTConfig.hasProperty("toJSON")) {
            opts++;
            // -- Account/Device exists?
            if (!acctExists || !deviceExists) {
                Print.logError("Account/Device does not exist: " + _fmtDevID(acctID,devID));
                System.exit(98);
            }
            // -- include mask
            int incMask = 0
              //| DBFactory.INCL_TABLE 
              //| DBFactory.INCL_BLANK 
              | DBFactory.INCL_TYPE  
              //| DBFactory.INCL_TITLE 
              //| DBFactory.INCL_KEY   
              ;
            // -- toJSON
            Device device = deviceRcd;
            JSON._Object devJSON = device.toJSON(null,incMask);
            Print.sysPrintln("Device JSON:\n" + devJSON.toString(true));
            System.exit(0);
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }

}
