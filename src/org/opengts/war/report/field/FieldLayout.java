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
//  Report definition based on generic field definitions
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/25  Martin D. Flynn
//     -Initial release
//  2007/11/28  Martin D. Flynn
//     -Added additional 'stop' fields for motion reporting
//  2007/01/10  Martin D. Flynn
//     -Added several new fields.
//  2008/03/12  Martin D. Flynn
//     -Added additional decimal point options to various fields
//  2008/03/28  Martin D. Flynn
//     -Added property/diagnostic fields
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//     -Added City/State/Country/Subdivision fields
//  2008/06/20  Martin D. Flynn
//     -Added DATA_ACCOUNT_ID
//  2008/10/16  Martin D. Flynn
//     -Added DATA_LAST_EVENT (not used)
//  2009/08/23  Martin D. Flynn
//     -Fixed StatusCode description lookup (now first looks up account custom 
//      status codes).
//  2009/09/23  Martin D. Flynn
//     -Added 'startOdometer' column
//  2010/04/11  Martin D. Flynn
//     -Added columns "serviceNotes"/"serviceRemaining"/"accountDesc"/...
//  2010/06/17  Martin D. Flynn
//     -Added columns "stopCount", "engineHours", "ptoHours", "idleHours", etc.
//  2010/09/09  Martin D. Flynn
//     -Added "deviceBattery"
//  2011/05/13  Martin D. Flynn
//     -Added DATA_EVENT_INDEX (not used)
//  2011/06/16  Martin D. Flynn
//     -Added status code/description coloring option
//  2011/10/03  Martin D. Flynn
//     -Added DATA_STOP_FUEL, DATA_START_FUEL, DATA_FUEL_DELTA
//  2011/12/06  Martin D. Flynn
//     -Added DATA_PLAN_DISTANCE
//  2012/02/03  Martin D. Flynn
//     -Added DATA_ODOMETER_DELTA_AH, DATA_DRIVING_ELAPSED_AH, DATA_FUEL_TRIP_AH
//  2012/04/03  Martin D. Flynn
//     -Changed "formatElapsedTime" to call "StringTools.formatElapsedSeconds"
//     -Added DATA_SERVICE_LAST_HR, DATA_SERVICE_INTERVAL_HR, DATA_SERVICE_NEXT_HR,
//      DATA_SERVICE_REMAINING_HR, DATA_FAULT_CODES
//  2012/06/29  Martin D. Flynn
//     -Added DATA_STATUS_COUNT
//     -Initial support for DATA_GEOCORRIDOR_ID
//  2012/12/24  Martin D. Flynn
//     -Added DATA_ENTITY_TYPE
//  2013/05/28  Martin D. Flynn
//     -Added DATA_SPEED_DURATION
//  2013/08/06  Martin D. Flynn
//     -Added DATA_ODOMETER_DELTA_BIT, DATA_START_ADDRESS
//     -Added DATA_START_GEOPOINT, DATA_START_LATITUDE, DATA_START_LONGITUDE
//     -Added DATA_START_FUEL_LEVEL, DATA_STOP_FUEL_LEVEL, DATA_DELTA_FUEL_LEVEL
//     -Added DATA_DISPLAY_NAME, DATA_DRIVER_NICKNAME, DATA_DRIVER_LICENSE_TYPE
//     -Added DATA_DRIVER_DEVICE_ID, DATA_DRIVER_DEVICE_DESC
//     -Added ability to format the driver's licenses expiration in number-of-days
//  2014/01/01  Martin D. Flynn
//     -Added DATA_ENTER_ODOMETER, DATA_ENTER_FUEL
//     -Added DATA_EXIT_ODOMETER, DATA_EXIT_FUEL
//  2014/09/16  Martin D. Flynn
//     -Added DATA_VEHICLE_MAKE, DATA_VEHICLE_MODEL
//  2014/11/30  Martin D. Flynn
//     -Updated DATA_CHECKIN_DATETIME, DATA_CHECKIN_AGE, to include "never".
//     -Added DATA_DEVICE_NAME
//     -Support device titles for DATA_DEVICE_DESC
//  2015/05/03  Martin D. Flynn
//     -Added "googleLink" support to DATA_INDEX.
//  2015/08/16  Martin D. Flynn
//     -Added DATA_DEVICE_IMEI, DATA_DEVICE_MODEM_ID, DATA_DEVICE_LICENSE
//     -Added DATA_MULTI_FIELDS.
//     -Added DATA_WORK_SHIFT (experimental)
//  2016/01/04  Martin D. FLynn
//     -Added DATA_TRIP_ELAPSED[_WH|_AH]
//  2016/04/06  Martin D. Flynn
//     -Added DATA_LAST_[LATITUDE|LONGITUDE|GEOPOINT], DATA_LAST_ADDRESS
//     -Added DATA_SPEED_MAXIMUM
//  2016/09/01  Martin D. Flynn
//     -Added support for service interval (distance) #1 [DATA_SERVICE_LAST_KM1]
//     -Renamed hour maintenance "DATA_SERVICE_*_HR" to "DATA_SERVICE_*_HR0"
//     -Renamed distance maintenance "DATA_SERVICE_*" to ""DATA_SERVICE_*_KM0"
//     -Added DATA_DEVICE_PHONE
//  2016/12/21  Martin D. Flynn
//     -Added DATA_DEVICE_FUEL_COST, DATA_DEVICE_ACTIVE, 
//     -Added DATA_BILLING_COST, DATA_BILLING_PRORATED
//     -Added DATA_INCIDENT_TIMESTAMP, DATA_INCIDENT_DATETIME
//     -Added DATA_ACCELERATION, DATA_ACCEL_MAGNITUDE
//     -Added DATA_BOOLEAN_1, DATA_BOOLEAN_2, DATA_COUNT_1, DATA_COUNT_2
// ----------------------------------------------------------------------------
package org.opengts.war.report.field;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.RequestProperties;
import org.opengts.war.tools.WebPageAdaptor;
import org.opengts.war.tools.MapDimension;

import org.opengts.war.report.*;

import org.opengts.db.dmtp.PropertyKey;

public class FieldLayout
    extends ReportLayout
{

    // ------------------------------------------------------------------------

    private static final long   MIN_REASONABLE_TIMESTAMP = (new DateTime(null,2000,1,1)).getTimeSec();

    // ------------------------------------------------------------------------
    // Data keys
    // - These define what data is available (see 'FieldDataRow') and what columns will be 
    //   displayed in the table.
    // - Column names must only contain <alpha>/<numeric>/'_' characters

    public static final String  DATA_INDEX                  = "index";
    public static final String  DATA_DISPLAY_NAME           = "displayName";            // field (general)

    public static final String  DATA_MULTI_FIELDS           = "multiFields";            // "arg" must contain field list

    public static final String  DATA_DBRECORD               = "dbrecord";               // field

    public static final String  DATA_ACCOUNT_ID             = "accountId";              // field
    public static final String  DATA_ACCOUNT_DESC           = "accountDesc";            // Accout record

    public static final String  DATA_USER_ID                = "userId";                 // field
    public static final String  DATA_USER_DESC              = "userDesc";               // User record

    public static final String  DATA_EVENTDATA              = "eventData";              // field

    public static final String  DATA_VEHICLE_ID             = "vehicleId";              // field
    public static final String  DATA_VEHICLE_MAKE           = "vehicleMake";            // field/Device
    public static final String  DATA_VEHICLE_MODEL          = "vehicleModel";           // field/Device
    public static final String  DATA_LICENSE_PLATE          = "licensePlate";           // field
    public static final String  DATA_EQUIPMENT_TYPE         = "equipmentType";          // field
    public static final String  DATA_UNIQUE_ID              = "uniqueId";               // field
    public static final String  DATA_GROUP_ID               = "groupId";                // field
    public static final String  DATA_STATUS_CODE            = "statusCode";             // field
    public static final String  DATA_STATUS_DESC            = "statusDesc";             // StatusCodes
    public static final String  DATA_STATUS_COUNT           = "statusCount";            // field
    public static final String  DATA_PUSHPIN                = "pushpin";                // field
    public static final String  DATA_ENTITY_ID              = "entityId";               // field
    public static final String  DATA_ENTITY_TYPE            = "entityType";             // field
    public static final String  DATA_ENTITY_DESC            = "entityDesc";             // field

    public static final String  DATA_EVENT_SUMMARY          = "eventSummary";           // field

    public static final String  DATA_DRIVER_ID              = "driverId";               // field
    public static final String  DATA_DRIVER_DESC            = "driverDesc";             // field
    public static final String  DATA_DRIVER_NICKNAME        = "driverNickname";         // field
    public static final String  DATA_DRIVER_STATUS          = "driverStatus";           // field
    public static final String  DATA_DRIVER_BADGEID         = "driverBadge";            // field
    public static final String  DATA_DRIVER_LICENSE         = "driverLicense";          // field
    public static final String  DATA_DRIVER_LICENSE_TYPE    = "driverLicenseType";      // field
    public static final String  DATA_DRIVER_LICENSE_EXP     = "driverLicenseExp";       // field
    public static final String  DATA_DRIVER_BIRTHDATE       = "driverBirthdate";        // field
    public static final String  DATA_DRIVER_DEVICE_ID       = "driverDeviceID";         // field
    public static final String  DATA_DRIVER_DEVICE_DESC     = "driverDeviceDesc";       // field

    public static final String  DATA_LATITUDE               = "latitude";               // field
    public static final String  DATA_LONGITUDE              = "longitude";              // field
    public static final String  DATA_GEOPOINT               = "geoPoint";               // field
    public static final String  DATA_ALTITUDE               = "altitude";               // field
    public static final String  DATA_SPEED_LIMIT            = "speedLimit";             // field
    public static final String  DATA_SPEED                  = "speed";                  // field
    public static final String  DATA_SPEED_HEADING          = "speedH";
    public static final String  DATA_SPEED_UNITS            = "speedU";
    public static final String  DATA_SPEED_DURATION         = "speedDuration";          // field
    public static final String  DATA_SPEED_MAXIMUM          = "speedMaximum";           // field
    public static final String  DATA_HEADING                = "heading";                // field
    public static final String  DATA_DISTANCE               = "distance";               // field
    public static final String  DATA_PLAN_DISTANCE          = "plannedDistance";        // field
    public static final String  DATA_ODOMETER               = "odometer";               // field
    public static final String  DATA_ODOMETER_DELTA         = "odomDelta";              // field
    public static final String  DATA_ODOMETER_DELTA_WH      = "odomDeltaWH";            // field (during WorkHours)
    public static final String  DATA_ODOMETER_DELTA_AH      = "odomDeltaAH";            // field (during AfterHours)
    public static final String  DATA_ODOMETER_DELTA_BIT     = "odomDeltaBit";           // field (while bit is on)
    public static final String  DATA_ODOMETER_OFFSET        = "odomOffset";             // field
    public static final String  DATA_EVENT_INDEX            = "eventIndex";             // NO-OP?
    public static final String  DATA_LAST_EVENT             = "lastEvent";              // NO-OP?
    public static final String  DATA_SERVER_ID              = "serverId";               // field (deviceCode)
    public static final String  DATA_JOB_NUMBER             = "jobNumber";              // field

    public static final String  DATA_BEST_LATITUDE          = "bestLatitude";           // field
    public static final String  DATA_BEST_LONGITUDE         = "bestLongitude";          // field
    public static final String  DATA_BEST_GEOPOINT          = "bestGeoPoint";           // field

    public static final String  DATA_LAST_LATITUDE          = "lastLatitude";           // Device
    public static final String  DATA_LAST_LONGITUDE         = "lastLongitude";          // Device
    public static final String  DATA_LAST_GEOPOINT          = "lastGeoPoint";           // Device
    public static final String  DATA_LAST_ADDRESS           = "lastAddress";            // Device/EventData

    public static final String  DATA_DATE                   = "date";                   // field (DayNumber)
    public static final String  DATA_TIME                   = "time";                   // field
    public static final String  DATA_DATETIME               = "dateTime";               // field
    public static final String  DATA_TIMESTAMP              = "timestamp";              // field

    public static final String  DATA_CREATE_TIMESTAMP       = "createTimestamp";        // field
    public static final String  DATA_CREATE_DATETIME        = "createDateTime";         // DATA_CREATE_TIMESTAMP
    public static final String  DATA_CREATE_DATE            = "createDate";             // DATA_CREATE_TIMESTAMP
    public static final String  DATA_CREATE_TIME            = "createTime";             // DATA_CREATE_TIMESTAMP

    public static final String  DATA_INCIDENT_DATETIME      = "incidentDateTime";       // field
    public static final String  DATA_INCIDENT_TIMESTAMP     = "incidentTimestamp";      // field

    public static final String  DATA_PING_DATETIME          = "commandDateTime";        // field/Device
    public static final String  DATA_PING_TIMESTAMP         = "commandTimestamp";       // field/Device

    public static final String  DATA_ACK_DATETIME           = "ackDateTime";            // field/Device
    public static final String  DATA_ACK_TIMESTAMP          = "ackTimestamp";           // field/Device

    public static final String  DATA_ADDRESS                = "address";                // field
    public static final String  DATA_CITY                   = "city";                   // field
    public static final String  DATA_STATE                  = "state";                  // field
    public static final String  DATA_COUNTRY                = "country";                // field
    public static final String  DATA_SUBDIVISION            = "subdivision";            // field
    public static final String  DATA_GEOZONE_ID             = "geozoneId";              // field
    public static final String  DATA_GEOZONE_DESC           = "geozoneDesc";            // Geozone record
    
    public static final String  DATA_GEOCORRIDOR_ID         = "corridorID";             // field/Geozone

    public static final String  DATA_PROPERTY_KEY           = "propertyKey";            // field
    public static final String  DATA_PROPERTY_DESC          = "propertyDesc";           // field/DATA_PROPERTY_KEY
    public static final String  DATA_PROPERTY_VALUE         = "propertyValue";          // field

    public static final String  DATA_DIAGNOSTIC_ERROR       = "diagError";              // field
    public static final String  DATA_DIAGNOSTIC_KEY         = "diagKey";                // field
    public static final String  DATA_DIAGNOSTIC_DESC        = "diagDesc";               // field
    public static final String  DATA_DIAGNOSTIC_VALUE       = "diagValue";              // field

    public static final String  DATA_NOTES                  = "notes";                  // field
    public static final String  DATA_DESCRIPTION            = "description";            // field
    public static final String  DATA_UTILIZATION            = "utilization";            // field
    public static final String  DATA_COUNT                  = "count";                  // field

    public static final String  DATA_START_DATETIME         = "startDateTime";
    public static final String  DATA_STOP_DATETIME          = "stopDateTime";
    public static final String  DATA_STARTSTOP_DATETIME     = "startStopDateTime";      // field

    public static final String  DATA_START_TIMESTAMP        = "startTimestamp";         // field
    public static final String  DATA_STOP_TIMESTAMP         = "stopTimestamp";          // field

    public static final String  DATA_START_LATITUDE         = "startLatitude";          // field
    public static final String  DATA_STOP_LATITUDE          = "stopLatitude";           // field

    public static final String  DATA_START_LONGITUDE        = "startLongitude";         // field
    public static final String  DATA_STOP_LONGITUDE         = "stopLongitude";          // field

    public static final String  DATA_START_GEOPOINT         = "startGeoPoint";          // field
    public static final String  DATA_STOP_GEOPOINT          = "stopGeoPoint";           // field
    public static final String  DATA_STARTSTOP_GEOPOINT     = "startStopGeoPoint";      // field

    public static final String  DATA_START_GEOZONE_ID       = "startGeozoneId";         // field
    public static final String  DATA_STOP_GEOZONE_ID        = "stopGeozoneId";          // field

    public static final String  DATA_START_HOURS            = "startEngineHours";       // field
    public static final String  DATA_STOP_HOURS             = "stopEngineHours";        // field

    public static final String  DATA_START_ODOMETER         = "startOdometer";          // field
    public static final String  DATA_STOP_ODOMETER          = "stopOdometer";           // field
    public static final String  DATA_STARTSTOP_ODOMETER     = "startStopOdometer";      // field

    public static final String  DATA_START_FUEL             = "startFuel";              // field
    public static final String  DATA_STOP_FUEL              = "stopFuel";               // field
  //public static final String  DATA_STARTSTOP_FUEL         = "startStopFuel";          // field

    public static final String  DATA_START_FUEL_LEVEL       = "startFuelLevel";         // field
    public static final String  DATA_STOP_FUEL_LEVEL        = "stopFuelLevel";          // field

    public static final String  DATA_START_ADDRESS          = "startAddress";           // field
    public static final String  DATA_STOP_ADDRESS           = "stopAddress";            // field
    public static final String  DATA_STARTSTOP_ADDRESS      = "startStopAddress";       // field

    public static final String  DATA_START_EVENT            = "startEventData";         // field
    public static final String  DATA_STOP_EVENT             = "stopEventData";          // field

    public static final String  DATA_STOP_ELAPSED           = "stopElapse";             // field
    public static final String  DATA_STOP_COUNT             = "stopCount";              // field
    public static final String  DATA_STOP_COUNT_WH          = "stopCountWH";            // field
    
    public static final String  DATA_ENTER_DATETIME         = "enterDateTime";          // field
    public static final String  DATA_EXIT_DATETIME          = "exitDateTime";           // field

    public static final String  DATA_ENTER_TIMESTAMP        = "enterTimestamp";         // field
    public static final String  DATA_EXIT_TIMESTAMP         = "exitTimestamp";          // field

    public static final String  DATA_ENTER_GEOZONE_ID       = "enterGeozoneId";         // field
    public static final String  DATA_EXIT_GEOZONE_ID        = "exitGeozoneId";          // field

    public static final String  DATA_ENTER_ADDRESS          = "enterAddress";           // field
    public static final String  DATA_EXIT_ADDRESS           = "exitAddress";            // field

    public static final String  DATA_ENTER_FUEL             = "enterFuel";              // field
    public static final String  DATA_EXIT_FUEL              = "exitFuel";               // field

    public static final String  DATA_ENTER_ODOMETER         = "enterOdometer";          // field
    public static final String  DATA_EXIT_ODOMETER          = "exitOdometer";           // field

    public static final String  DATA_ENTER_HOURS            = "enterEngineHours";       // field
    public static final String  DATA_EXIT_HOURS             = "exitEngineHours";        // field

    public static final String  DATA_ENTER_EVENT            = "enterEventData";         // field
    public static final String  DATA_EXIT_EVENT             = "exitEventData";          // field

    public static final String  DATA_HOURS_DELTA            = "engineHoursDelta";       // field
    public static final String  DATA_FUEL_DELTA             = "fuelDelta";              // field
    public static final String  DATA_DELTA_FUEL_LEVEL       = "deltaFuelLevel";         // field

    public static final String  DATA_ELAPSE_SEC             = "elapseSec";              // field
    public static final String  DATA_INSIDE_ELAPSED         = "insideElapse";           // field
    public static final String  DATA_OUTSIDE_ELAPSED        = "outsideElapse";          // field
    public static final String  DATA_TRIP_ELAPSED           = "tripElapse";             // field
    public static final String  DATA_TRIP_ELAPSED_WH        = "tripElapseWH";           // field (during WorkHours)
    public static final String  DATA_TRIP_ELAPSED_AH        = "tripElapseAH";           // field (during AfterHours)
    public static final String  DATA_DRIVING_ELAPSED        = "drivingElapse";          // field
    public static final String  DATA_DRIVING_ELAPSED_WH     = "drivingElapseWH";        // field (during WorkHours)
    public static final String  DATA_DRIVING_ELAPSED_AH     = "drivingElapseAH";        // field (during AfterHours)
    public static final String  DATA_IDLE_ELAPSED           = "idleElapse";             // field
    public static final String  DATA_AVERAGE_IDLE_HOURS     = "avgIdleHours";           // field
    public static final String  DATA_ATTACHED               = "attached";               // field
    
    public static final String  DATA_RULE_ID                = "ruleId";                 // field
    public static final String  DATA_MESSAGE_ID             = "messageId";              // field

    public static final String  DATA_TCP_CONNECTIONS        = "tcpConnections";         // field
    public static final String  DATA_UDP_CONNECTIONS        = "udpConnections";         // field
    public static final String  DATA_CONNECTIONS            = "connections";            // field
    public static final String  DATA_IPADDRESS              = "ipAddress";              // field
    public static final String  DATA_ISDUPLEX               = "isDuplex";               // field
    public static final String  DATA_BYTES_READ             = "bytesRead";              // field
    public static final String  DATA_BYTES_WRITTEN          = "bytesWritten";           // field
    public static final String  DATA_BYTES_OVERHEAD         = "bytesOverhead";          // field
    public static final String  DATA_BYTES_TOTAL            = "bytesTotal";             // field
    public static final String  DATA_BYTES_ROUNDED          = "bytesRounded";           // field
    public static final String  DATA_EVENTS_RECEIVED        = "eventsReceived";         // field

    public static final String  DATA_ENGINE_HOURS           = "engineHours";            // field getEngineHoursOffset
    public static final String  DATA_IDLE_HOURS             = "idleHours";              // field
    public static final String  DATA_WORK_HOURS             = "workHours";              // field
    public static final String  DATA_PTO_HOURS              = "ptoHours";               // field

    public static final String  DATA_WORK_SHIFT             = "workShift";              // field

    public static final String  DATA_FUEL_CAPACITY          = "fuelCapacity";           // field
    public static final String  DATA_FUEL_LEVEL             = "fuelLevel";              // field
    public static final String  DATA_FUEL_TOTAL             = "fuelTotal";              // field
    public static final String  DATA_FUEL_REMAIN            = "fuelRemain";             // field
    public static final String  DATA_FUEL_REFILL            = "fuelRefill";             // field
    public static final String  DATA_FUEL_TRIP              = "fuelTrip";               // field
    public static final String  DATA_FUEL_TRIP_WH           = "fuelTripWH";             // field (during WorkHours)
    public static final String  DATA_FUEL_TRIP_AH           = "fuelTripAH";             // field (during AfterHours)
    public static final String  DATA_FUEL_IDLE              = "fuelIdle";               // field
    public static final String  DATA_FUEL_IDLE_WH           = "fuelIdleWH";             // field (during WorkHours)
    public static final String  DATA_FUEL_IDLE_AH           = "fuelIdleAH";             // field (during AfterHours)
    public static final String  DATA_FUEL_WORK              = "fuelWork";               // field
    public static final String  DATA_FUEL_PTO               = "fuelPTO";                // field
    public static final String  DATA_FUEL_ECONOMY           = "fuelEconomy";            // field
    public static final String  DATA_FUEL_ECONOMY_TYPE      = "fuelEconomyType";        // field

    public static final String  DATA_ENGINE_RPM             = "engineRpm";              // field

    public static final String  DATA_ACCELERATION           = "acceleration";           // field
    public static final String  DATA_ACCEL_MAGNITUDE        = "accelMagnitude";         // field

    public static final String  DATA_DEVICE_ID              = "deviceId";               // field
    public static final String  DATA_DEVICE_IMEI            = "deviceIMEI";             // Device record
    public static final String  DATA_DEVICE_MODEM_ID        = "deviceModemID";          // Device record
    public static final String  DATA_DEVICE_DESC            = "deviceDesc";             // Device record
    public static final String  DATA_DEVICE_NAME            = "deviceName";             // Device record
    public static final String  DATA_DEVICE_ACTIVE          = "deviceActive";           // Device record
    public static final String  DATA_DEVICE_BATTERY_LEVEL   = "deviceBatteryLevel";     // Device record
    public static final String  DATA_DEVICE_BATTERY_VOLTS   = "deviceBatteryVolts";     // Device record
    public static final String  DATA_DEVICE_VEHICLE_VOLTS   = "deviceVehicleVolts";     // Device record
    public static final String  DATA_DEVICE_VIN             = "deviceVehicleID";        // Device record
    public static final String  DATA_DEVICE_LICENSE_EXP     = "deviceLicenseExp";       // Device record
    public static final String  DATA_DEVICE_INSURANCE_EXP   = "deviceInsuranceExp";     // Device record
    public static final String  DATA_DEVICE_LICENSE         = "deviceLicensePlate";     // Device record
    public static final String  DATA_DEVICE_PHONE           = "devicePhoneNumber";      // Device record
    public static final String  DATA_DEVICE_FUEL_COST       = "deviceFuelCost";         // Device record
    public static final String  DATA_DEVICE_NOTES           = "deviceNotes";            // Device record

    public static final String  DATA_CHECKIN_DATETIME       = "checkinDateTime";        // Device record
    public static final String  DATA_CHECKIN_AGE            = "checkinAge";             // Device record
    public static final String  DATA_LAST_IPADDRESS         = "lastIPAddress";          // Device record
    public static final String  DATA_CODE_VERSION           = "codeVersion";            // Device record
    public static final String  DATA_CUSTOM_FIELD           = "customField";            // Device record
    public static final String  DATA_FAULT_CODES            = "faultCodes";             // Device record
    public static final String  DATA_COMMAND_STATE_MASK     = "commandStateMask";       // Device record
    public static final String  DATA_COMMAND_STATE_BIT      = "commandStateBit";        // Device record

    public static final String  DATA_SERVICE_LAST_HR0       = "serviceLastHR0";         // Device record getEngineHoursOffset
    public static final String  DATA_SERVICE_INTERVAL_HR0   = "serviceIntervalHR0";     // Device record
    public static final String  DATA_SERVICE_NEXT_HR0       = "serviceNextHR0";         // Device record getEngineHoursOffset
    public static final String  DATA_SERVICE_REMAINING_HR0  = "serviceRemainingHR0";    // Device record

    public static final String  DATA_SERVICE_LAST_KM0       = "serviceLastDist0";       // Device record
    public static final String  DATA_SERVICE_INTERVAL_KM0   = "serviceIntervalDist0";   // Device record
    public static final String  DATA_SERVICE_NEXT_KM0       = "serviceNextDist0";       // Device record
    public static final String  DATA_SERVICE_REMAINING_KM0  = "serviceRemainingDist0";  // Device record

    public static final String  DATA_SERVICE_LAST_KM1       = "serviceLastDist1";       // Device record
    public static final String  DATA_SERVICE_INTERVAL_KM1   = "serviceIntervalDist1";   // Device record
    public static final String  DATA_SERVICE_NEXT_KM1       = "serviceNextDist1";       // Device record
    public static final String  DATA_SERVICE_REMAINING_KM1  = "serviceRemainingDist1";  // Device record

    public static final String  DATA_SERVICE_NOTES          = "serviceNotes";           // Device record

    public static final String  DATA_LOGIN_DATETIME         = "loginDateTime";          // Account record
    public static final String  DATA_LOGIN_AGE              = "loginAge";               // Account record
    public static final String  DATA_ACCOUNT_ACTIVE         = "accountActive";          // Account record
    public static final String  DATA_DEVICE_COUNT           = "deviceCount";            // Account record
    public static final String  DATA_PRIVATE_LABEL          = "privateLabelName";       // Account record
    public static final String  DATA_SMS_COUNT              = "smsCount";               // Account record

    public static final String  DATA_BILLING_COST           = "billingCost";            // field: billing cost
    public static final String  DATA_BILLING_PRORATED       = "billingProrated";        // field: billing prorated

    public static final String  DATA_RATIO_1                = "ratio_1";                // field: generic ratio (double)
    public static final String  DATA_RATIO_2                = "ratio_2";                // field: generic ratio (double)
    public static final String  DATA_BOOLEAN_1              = "boolean_1";              // field: generic boolean
    public static final String  DATA_BOOLEAN_2              = "boolean_2";              // field: generic boolean
    public static final String  DATA_COUNT_1                = "count_1";                // field: generic count (long)
    public static final String  DATA_COUNT_2                = "count_2";                // field: generic count (long)
    public static final String  DATA_GRAPH_LINK             = "graphLink";              // field: generic graph link (URL)

    public static final String  DATA_LEFT_ALIGN_1           = "leftAlign_1";            // field: left aligned string
    public static final String  DATA_LEFT_ALIGN_2           = "leftAlign_2";            // field: left aligned string
    public static final String  DATA_RIGHT_ALIGN_1          = "rightAlign_1";           // field: right aligned string
    public static final String  DATA_RIGHT_ALIGN_2          = "rightAlign_2";           // field: right aligned string

    public static final String  DATA_BLANK_SPACE            = "blankSpace";             // nothing displayed

    public static final String  DATA_RULE_SELECTOR          = "ruleSelector";           // RuleFactory

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String GetLocalStringValue(String v, I18N i18n)
    {

        /* blank */
        if (StringTools.isBlank(v)) {
            return v;
        } 

        /* No/Yes */
        if (v.equalsIgnoreCase("no")) {
            return i18n.getString("FieldLayout.no","No");
        } else
        if (v.equalsIgnoreCase("yes")) {
            return i18n.getString("FieldLayout.yes","Yes");
        }

        /* False/True */
        if (v.equalsIgnoreCase("false")) {
            return i18n.getString("FieldLayout.false","False");
        } else
        if (v.equalsIgnoreCase("true")) {
            return i18n.getString("FieldLayout.true","True");
        }

        /* Off/On */
        if (v.equalsIgnoreCase("off")) {
            return i18n.getString("FieldLayout.off","Off");
        } else
        if (v.equalsIgnoreCase("on")) {
            return i18n.getString("FieldLayout.true","True");
        }

        /* Disabled/Enabled */
        if (v.equalsIgnoreCase("disabled")) {
            return i18n.getString("FieldLayout.disabled","Disabled");
        } else
        if (v.equalsIgnoreCase("enabled")) {
            return i18n.getString("FieldLayout.enabled","Enabled");
        }

        /* default */
        return v;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the preferred Lat/Lon display format from the specified parameters,
    *** or from the specified Account if the parameters do not specify a format.
    **/
    private static String GetLatLonFormat(String args[], Account account)
    {
        // -- "4,mapLink", "mapLink", "dms"

        /* check parameters */
        if (!ListTools.isEmpty(args)) {
            for (int a = 0; a < args.length; a++) {
                if (GeoPoint.IsValidFormatCode(args[a])) {
                    return args[a];
                }
            }
        }

        /* check account default */
        switch (Account.getLatLonFormat(account)) {
            case DM : return GeoPoint.SFORMAT_DM;
            case DMS: return GeoPoint.SFORMAT_DMS;
        }

        /* default */
        return GeoPoint.SFORMAT_DEC_4;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // FieldLayout is a singleton
    
    private static FieldLayout reportDef = null;

    public static ReportLayout getReportLayout()
    {
        if (reportDef == null) {
            reportDef = new FieldLayout();
        }
        return reportDef;
    }
    
    private FieldLayout()
    {
        super();
        this.setDataRowTemplate(new FieldDataRow());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /* format double value */
    protected static String formatDouble(double value, String arg, String dftArg)
    {

        /* get format */
        String fmt = dftArg;
        if ((arg == null) || (arg.length() <= 0)) { // StringTools.isBlank(arg)
            // -- use default format
            fmt = dftArg;
        } else
        if (arg.startsWith("#")) {
            // -- 'arg' contains specific format
            fmt = arg;
        } else
        if (Character.isDigit(arg.charAt(0))) {
            // -- 'arg' contains desired nuber of decimal points
            switch (arg.charAt(0)) {
                case '0': fmt = "#0"          ; break;
                case '1': fmt = "#0.0"        ; break;
                case '2': fmt = "#0.00"       ; break;
                case '3': fmt = "#0.000"      ; break;
                case '4': fmt = "#0.0000"     ; break;
                case '5': fmt = "#0.00000"    ; break;
                case '6': fmt = "#0.000000"   ; break;
                case '7': fmt = "#0.0000000"  ; break;
                case '8': fmt = "#0.00000000" ; break;
                case '9': fmt = "#0.000000000"; break;
            }
        } else {
            // -- 'arg' not recognized, use default? or use 'arg' as-is?
            fmt = arg; // dftArg; // arg;
        }

        /* return formatted String */
        if (fmt == null) {
            return String.valueOf(value);
        } else
        if (fmt.equalsIgnoreCase("pct") || fmt.equalsIgnoreCase("percent")) {
            return Math.round(value*100.0) + "%";
        } else {
            return StringTools.format(value, fmt);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified Object contains a valid GeoPoint
    **/
    protected static boolean HasValidGeoPoint(Object dataObj)
    {
        if (dataObj instanceof FieldData) {
            FieldData fd = (FieldData)dataObj;
            if (fd.hasValue(DATA_GEOPOINT)) {
                return GeoPoint.isValid(fd.getGeoPoint(DATA_GEOPOINT));
            } else
            if (fd.hasValue(DATA_LATITUDE) && fd.hasValue(DATA_LONGITUDE)) {
                double lat = fd.getDouble(DATA_LATITUDE);
                double lon = fd.getDouble(DATA_LONGITUDE);
                return GeoPoint.isValid(lat,lon);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
    *** Gets the GeoPoint from the specified Object
    **/
    protected static GeoPoint GetGeoPoint(Object dataObj)
    {
        if (dataObj instanceof FieldData) {
            FieldData fd = (FieldData)dataObj;
            if (fd.hasValue(DATA_GEOPOINT)) {
                return fd.getGeoPoint(DATA_GEOPOINT);
            } else
            if (fd.hasValue(DATA_LATITUDE) && fd.hasValue(DATA_LONGITUDE)) {
                double lat = fd.getDouble(DATA_LATITUDE);
                double lon = fd.getDouble(DATA_LONGITUDE);
                return new GeoPoint(lat,lon);
            } else {
                return GeoPoint.INVALID_GEOPOINT;
            }
        } else
        if (dataObj instanceof GeoPoint) {
            return (GeoPoint)dataObj;
        } else {
            return GeoPoint.INVALID_GEOPOINT;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static class FieldDataRow
        extends DataRowTemplate
    {
        public DBDataRow.RowType getRowType(Object dataObj) {
            return (dataObj instanceof FieldData)? ((FieldData)dataObj).getRowType() : DBDataRow.RowType.DETAIL;
        }
        public FieldDataRow() {
            super();

            // -- Index
            this.addColumnTemplate(new DataColumnTemplate(DATA_INDEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    int ofs = 1;
                    if (fd.hasValue(DATA_INDEX)) {
                        int ndx = fd.getInt(DATA_INDEX);
                        if (ndx >= 0) {
                            return String.valueOf(ndx + ofs);
                        } else {
                            return "";
                        }
                    } else
                    if (rowNdx >= 0) {
                        String ndxVal = String.valueOf(rowNdx + ofs);
                        if (StringTools.isBlank(arg)) {
                            return ndxVal;
                        } else
                        if (!HasValidGeoPoint(fd)) {
                            return ndxVal;
                        } else
                        if (arg.equalsIgnoreCase("mapLink") || arg.equalsIgnoreCase("map")) {
                            URIArg mapURL = rd.getMapURL();
                            if (mapURL != null) {
                                mapURL.addArg("showpp",rowNdx+1);
                                mapURL.addArg("zoompp",rowNdx+1);
                                MapDimension sz = rd.getMapWindowSize();
                                int W = sz.getWidth();
                                int H = sz.getHeight();
                                String  encMapURL = WebPageAdaptor.EncodeURL(rd.getRequestProperties(),mapURL);
                                String  target    = null;
                                boolean button    = false;
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(" "+ndxVal+" ");
                                cv.setLinkURL("javascript:openResizableWindow('"+encMapURL+"','ReportMap',"+W+","+H+");",target,button);
                                return cv;
                            } else {
                                return ndxVal;
                            }
                        } else
                        if (arg.equalsIgnoreCase("googleMap") ||
                            arg.equalsIgnoreCase("googleLink")  ) {
                            GeoPoint gp = GetGeoPoint(fd);
                            if (GeoPoint.isValid(gp)) {
                                double lat = gp.getLatitude();
                                double lon = gp.getLongitude();
                                URIArg  mapURL = new URIArg("https://www.google.com/maps/search/"+lat+","+lon);
                                String  target = "_blank";
                                boolean button = false;
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(" "+ndxVal+" ");
                                cv.setLinkURL(mapURL.toString(),target,button);
                                return cv;
                            } else {
                                return ndxVal;
                            }
                        } else
                        if (arg.charAt(0) == '0') {
                            return String.valueOf(rowNdx);
                        } else {
                            return String.valueOf(rowNdx + ofs);
                        }
                    } else {
                        return "";
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return "#";
                }
            });

            // -- Display name
            this.addColumnTemplate(new DataColumnTemplate(DATA_DISPLAY_NAME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String name = fd.getString(DATA_DISPLAY_NAME,null);
                    return fd.filterReturnedValue(DATA_DISPLAY_NAME,name);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.displayName","Display Name");
                }
            });

            // -- Multiple field combination
            this.addColumnTemplate(new DataColumnTemplate(DATA_MULTI_FIELDS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    StringBuffer sb = new StringBuffer();
                    String multiColKeys[] = StringTools.split(arg,',');
                    for (String multiColKey : multiColKeys) {
                        DataColumnTemplate dct = FieldDataRow.this.getColumnTemplate(multiColKey);
                        if (dct != null) {
                            Object valObj = dct.getColumnValue(rowNdx, rd, rc, dataObj);
                            String valStr = StringTools.trim(valObj);
                            //Print.logInfo("Column key/value: " + multiColKey + " ==> " + valStr);
                            if (sb.length() > 0) { sb.append("-"); } // separator
                            sb.append(valStr);
                        } else {
                            Print.logWarn("Column key not found: " + multiColKey);
                        }
                    }
                    return fd.filterReturnedValue(DATA_MULTI_FIELDS,sb.toString());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.multiFields","Multi-Fields");
                }
            });

            // -- DBRecord
            this.addColumnTemplate(new DataColumnTemplate(DATA_DBRECORD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String tableName = rd.getProperties().getString("tableName", "");
                    String fieldName = rc.getArg();
                    FieldData fd     = (FieldData)dataObj;
                    Object dbrObj    = fd.getValue(DATA_DBRECORD,null);
                    DBRecord dbr     = (dbrObj instanceof DBRecord)? (DBRecord)dbrObj : null;
                    if (dbr == null) {
                        // -- "dbrecord" value not specified, get record based on 'tableName'
                        if (StringTools.isBlank(tableName)) {
                            tableName = Account.TABLE_NAME();
                            dbr = fd.getAccount();
                        } else
                        if (tableName.equalsIgnoreCase(Account.TABLE_NAME())) {
                            dbr = fd.getAccount();
                        } else
                        if (tableName.equalsIgnoreCase(Device.TABLE_NAME())) {
                            dbr = fd.getDevice();
                        } else
                        if (tableName.equalsIgnoreCase(Driver.TABLE_NAME())) {
                            dbr = fd.getDriver();
                        }
                    }
                    if (dbr != null) {
                        // -- get actual DBField, if available
                        try {
                            DBField dbFld = dbr.getFactory(true).getField(fieldName);
                            if (dbFld != null) {
                                // -- found matching field
                                String valStr = StringTools.trim(dbFld.formatValue(dbr.getFieldValue(fieldName)));
                                return fd.filterReturnedValue(DATA_DBRECORD,valStr);
                            }
                        } catch (DBException dbe) {
                            Print.logError("Unable to get DBField value: " + dbe);
                            return fd.filterReturnedValue(DATA_DBRECORD,"error");
                        }
                        // -- DBField not found, try getter method
                        try {
                            Object valObj = MethodAction.InvokeGetter(dbr,fieldName);
                            String valStr = StringTools.trim(valObj);
                            return fd.filterReturnedValue(DATA_DBRECORD,valStr);
                        } catch (Throwable th) { // NoSuchMethodException, ClassNotFoundException, ...
                            // -- quietly ignore
                        }
                        // -- field/getter not found
                        return fd.filterReturnedValue(DATA_DBRECORD,"?field");
                    }
                    // -- no DBRecord
                    return fd.filterReturnedValue(DATA_DBRECORD,"?table");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String tableName = rd.getProperties().getString("tableName", "");
                    String fieldName = rc.getArg();
                    DBFactory dbFact = !StringTools.isBlank(tableName)? 
                        DBFactory.getFactoryByName(tableName) :
                        Account.getFactory();
                    DBField dbFld = (dbFact != null)? dbFact.getField(fieldName) : null;
                    if (dbFld != null) {
                        String title = dbFld.getTitle(rd.getLocale());
                        String ttl[] = StringTools.splitOnLength(title,' ',12);
                        return StringTools.join(ttl,'\n');
                    }
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.dbRecordField","Field");
                }
            });

            // -- Account-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCOUNT_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String acctID = fd.getAccountID();
                    return fd.filterReturnedValue(DATA_ACCOUNT_ID,acctID);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.accountID","Account-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCOUNT_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = fd.getString(DATA_ACCOUNT_DESC,null);
                    if (desc == null) {
                        Account acct = fd.getAccount();
                        if (acct != null) {
                            desc = acct.getDescription();
                            if (StringTools.isBlank(desc) && acct.isSystemAdmin()) {
                                I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                                desc = i18n.getString("FieldLayout.accountAdmin","Administrator");
                            }
                        }
                    }
                    return fd.filterReturnedValue(DATA_ACCOUNT_DESC,desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.accountDescription","Account\nDescription");
                }
            });

            // -- User-ID/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_USER_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String userID = fd.getString(FieldLayout.DATA_USER_ID, "");
                    if (StringTools.isBlank(userID)) {
                        Device dev = fd.getDevice();
                        userID = (dev != null)? dev.getAssignedUserID() : "";
                    }
                    return fd.filterReturnedValue(DATA_USER_ID,userID);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.userID","User-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_USER_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String userID = fd.getString(FieldLayout.DATA_USER_ID, "");
                    if (StringTools.isBlank(userID)) {
                        Device dev = fd.getDevice();
                        userID = (dev != null)? dev.getAssignedUserID() : "";
                    }
                    String desc = fd.getString(FieldLayout.DATA_USER_DESC, "");
                    if (StringTools.isBlank(desc) && !StringTools.isBlank(userID)) {
                        if (User.isAdminUser(userID)) {
                            I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                            desc = i18n.getString("FieldLayout.userAdmin","Account Administrator");
                        } else {
                            try {
                                User user = User.getUser(fd.getAccount(),userID);
                                desc = (user != null)? user.getDescription() : userID;
                            } catch (DBException dbe) {
                                desc = userID;
                            }
                        }
                    }
                    return fd.filterReturnedValue(DATA_USER_DESC,desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.userDescription","User\nDescription");
                }
            });

            // -- Device-ID/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String deviceID = fd.getDeviceID();
                    if (arg.startsWith("admin")) { // "admin", "adminView", "adminEdit"
                        // -- wrap in link to Device Admin page
                        ColumnValue cv = new ColumnValue(deviceID);
                        RequestProperties reqState = rd.getRequestProperties();
                        URIArg devAdminURL = WebPageAdaptor.MakeURL(reqState.getBaseURI(),"dev.info"); // Constants.PAGE_DEVICE_INFO);
                        String  target    = null;
                        boolean button    = false;
                        devAdminURL.addArg("device"   , deviceID);    // Constants.PARM_DEVICE, devID);
                        devAdminURL.addArg("page_cmd" , "selectDev"); // CommonServlet.PAGE_COMMAND, DeviceInfo.COMMAND_INFO_SEL_DEVICE);
                        if (arg.equalsIgnoreCase("adminEdit")) {
                            devAdminURL.addArg("d_subedit", "edit");      // DeviceInfo.PARM_SUBMIT_EDIT, "edit");
                        } else {
                            devAdminURL.addArg("d_subview", "view");      // DeviceInfo.PARM_SUBMIT_VIEW, "view");
                        }
                        cv.setLinkURL(devAdminURL.toString(),target,button);
                        return cv;
                    } else {
                        return fd.filterReturnedValue(DATA_DEVICE_ID,deviceID);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceID","{0} ID",devTitles);
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_IMEI) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String imei = fd.getString(DATA_DEVICE_IMEI,null);
                    if (imei == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            imei = dev.getImeiNumber();
                        }
                    }
                    return fd.filterReturnedValue(DATA_DEVICE_IMEI,imei);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceIMEI","{0}\nIMEI/ESN/etc.",devTitles);
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_MODEM_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String mid = fd.getString(DATA_DEVICE_MODEM_ID,null);
                    if (mid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            mid = dev.getModemID();
                        }
                    }
                    return fd.filterReturnedValue(DATA_DEVICE_MODEM_ID,mid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceIMEI","{0}\nModemID",devTitles);
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = fd.getString(DATA_DEVICE_DESC,null);
                    if (desc == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            desc = dev.getDescription();
                        }
                    }
                    return fd.filterReturnedValue(DATA_DEVICE_DESC,desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceDescription","{0}\nDescription",devTitles);
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_NAME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = fd.getString(DATA_DEVICE_NAME,null);
                    if (desc == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            desc = dev.getDisplayName();
                        }
                    }
                    return fd.filterReturnedValue(DATA_DEVICE_NAME,desc);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceDisplayName","{0}\nName",devTitles);
                }
            });

            // -- Device active
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_ACTIVE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        boolean isActive = dev.isActive();
                        String value = isActive?
                            i18n.getString("FieldLayout.activeYes","Yes") :
                            i18n.getString("FieldLayout.activeNo" ,"No" );
                        return fd.filterReturnedValue(DATA_DEVICE_ACTIVE,value);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceActive","Device\nActive");
                }
            });

            // -- Device Battery-Level/Volts
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_BATTERY_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        double level = dev.getLastBatteryLevel();
                        if (level <= 0.0) {
                            return rc.getBlankFiller();
                        } else
                        if (level <= 1.0) {
                            String p = Math.round(level*100.0) + "%";
                            return fd.filterReturnedValue(DATA_DEVICE_BATTERY_LEVEL, p);  // percent
                        } else {
                            String v = formatDouble(level, arg, "0.0") + "v";
                            return fd.filterReturnedValue(DATA_DEVICE_BATTERY_LEVEL, v);  // volts
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceBatteryLevel","Last\nBattery\nLevel");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_BATTERY_VOLTS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        double level = dev.getLastBatteryVolts();
                        if (level <= 0.0) {
                            return rc.getBlankFiller();
                        } else {
                            String v = formatDouble(level, arg, "0.0");
                            return fd.filterReturnedValue(DATA_DEVICE_BATTERY_VOLTS, v);  // volts
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceBatteryVolts","Last\nBattery\nVolts");
                }
            });

            // -- Device Vehicle Battery-Volts
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_VEHICLE_VOLTS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        double level = dev.getLastVBatteryVolts();
                        if (level <= 0.0) {
                            return rc.getBlankFiller();
                        } else {
                            String v = formatDouble(level, arg, "0.0");
                            return fd.filterReturnedValue(DATA_DEVICE_VEHICLE_VOLTS, v);  // volts
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceVehicleVolts","Last\nVehicle\nVolts");
                }
            });

            // -- Device VehicleID (see also DATA_VEHICLE_ID)
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_VIN) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String vin = fd.getString(DATA_DEVICE_VIN,null);
                    if (vin == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            vin = dev.getVehicleID();
                        }
                    }
                    return fd.filterReturnedValue(DATA_DEVICE_VIN,vin);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                  //return i18n.getString("FieldLayout.vehicleID","Vehicle-ID");
                    return i18n.getString("FieldLayout.deviceVehicleID","{0} ID",devTitles);
                }
            });

            // -- Device license
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_LICENSE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String lic = fd.getString(DATA_DEVICE_LICENSE,null);
                    if (lic == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            lic = dev.getLicensePlate();
                        }
                    }
                    return fd.filterReturnedValue(DATA_DEVICE_LICENSE,lic);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceLicense","{0}\nLicense",devTitles);
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_LICENSE_EXP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    long licExp = 0L;
                    if (fd.hasValue(DATA_DEVICE_LICENSE_EXP)) {
                        licExp = fd.getLong(DATA_DEVICE_LICENSE_EXP);
                    } else {
                        Device device = fd.getDevice();
                        licExp = (device != null)? device.getLicenseExpire() : 0L;
                    }
                    if (licExp > 0L) {
                        TimeZone tz = rd.getTimeZone();
                        long currdn = DateTime.getCurrentDayNumber(tz);
                        long days = licExp - currdn;
                        boolean expired = (days <= 0L)? true : false;
                        if (arg.equalsIgnoreCase("days")) {
                            I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                            String expStr = "";
                            ColorTools.RGB fgColor = null;
                            if (days < 0L) {
                                // -- Red: Expired
                                expStr = i18n.getString("FieldLayout.expired", "Expired");
                                fgColor = ColorTools.RED;
                            } else
                            if (days == 0L) {
                                // -- Red: Today
                                expStr = i18n.getString("FieldLayout.today", "Today");
                                fgColor = ColorTools.RED;
                            } else
                            if (days == 1L) {
                                // -- Yellow: Tomorrow
                                //expStr = i18n.getString("FieldLayout.nday", "{0} day", String.valueOf(days));
                                expStr = i18n.getString("FieldLayout.tomorrow", "Tomorrow");
                                fgColor = ColorTools.DARK_YELLOW;
                            } else
                            if (days <= 30L) {
                                // -- Yellow: N days from now
                                expStr = i18n.getString("FieldLayout.ndays", "{0} days", String.valueOf(days));
                                fgColor = ColorTools.DARK_YELLOW;
                            } else {
                                // -- Green/Black: N days from now
                                expStr = i18n.getString("FieldLayout.ndays", "{0} days", String.valueOf(days));
                                //fgColor = ColorTools.GREEN;
                            }
                            ColumnValue cv = new ColumnValue(expStr).setSortKey(licExp);
                            if (fgColor != null) {
                                cv.setForegroundColor(fgColor);
                            }
                            return fd.filterReturnedValue(DATA_DEVICE_LICENSE_EXP, cv);
                        } else {
                            ReportLayout rl = rd.getReportLayout();
                            DayNumber dn = new DayNumber(licExp);
                            String dnFmt = dn.format(rl.getDateFormat(rd.getPrivateLabel()));
                            ColumnValue cv = new ColumnValue(dnFmt).setSortKey(licExp);
                            if (days <= 0L) {
                                cv.setForegroundColor(ColorTools.RED);
                            } else
                            if (days <= 30L) {
                                cv.setForegroundColor(ColorTools.DARK_YELLOW);
                            }
                            return fd.filterReturnedValue(DATA_DEVICE_LICENSE_EXP, cv);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceLicenseExpire","{0}\nLicense Exp",devTitles);
                }
            });

            // -- Device Phone#
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_PHONE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg   = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev   = fd.getDevice();
                    String ph    = fd.getString(DATA_DEVICE_PHONE,null);
                    String phDig = "";
                    if (ph != null) {
                        phDig = StringTools.stripNonDigitChars(ph);
                    } else 
                    if (dev == null) {
                        return rc.getBlankFiller();
                    } else {
                        ph    = dev.getFixedContactPhone();
                        phDig = StringTools.stripNonDigitChars(ph);
                        if (StringTools.isBlank(phDig)) {
                            ph    = dev.getSimPhoneNumber();
                            phDig = StringTools.stripNonDigitChars(ph);
                            if (StringTools.isBlank(phDig)) {
                                return rc.getBlankFiller();
                            }
                        }
                    }
                    String  target = null;
                    boolean button = false;
                    ColumnValue cv = new ColumnValue();
                    cv.setValue(ph);
                    cv.setLinkURL("tel:"+phDig,target,button); // <a href="tel:5551234">555-1234</a>
                    return fd.filterReturnedValue(DATA_DEVICE_PHONE,cv);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.devicePhone","{0}\nPhone#",devTitles);
                }
            });

            // -- Device fuel cost
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_FUEL_COST) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg   = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Account acct = fd.getAccount();
                    Device  dev  = fd.getDevice();
                    // -- get fuel cost per liter
                    double costPerLiter = 0.0;
                    if (dev != null) {
                        // -- try Device
                        costPerLiter = dev.getFuelCostPerLiter();
                    }
                    if ((costPerLiter <= 0.0) && (acct != null)) {
                        // -- try Account
                        costPerLiter = acct.getFuelCostPerLiter();
                    }
                    // -- convert to volume unit cost ($/Liter * Liters/Gallon = $/Gallon)
                    if (costPerLiter > 0.0) {
                        double costPerUnit = costPerLiter * Account.getVolumeUnits(acct).convertToLiters(1.0);
                        String fmt = ((arg != null) && arg.startsWith("$"))? arg.substring(1) : StringTools.trim(arg);
                        return fd.filterReturnedValue(DATA_DEVICE_FUEL_COST,formatDouble(costPerUnit, fmt, "#0.00"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceFuelCost","{0}\nFuel Cost",devTitles) + " (${currency})";
                }
            });

            // -- Device notes
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_NOTES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg   = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Account acct = fd.getAccount();
                    Device  dev  = fd.getDevice();
                    String notes = (dev != null)? dev.getNotes() : "";
                    // -- insert new-line wrap characters
                    int wrapAt = StringTools.parseInt(arg,40);
                    if (wrapAt > 0) {
                        notes = StringTools.insertLineBreaks(notes,wrapAt,"\n");
                    }
                    // -- return
                    return fd.filterReturnedValue(DATA_DEVICE_NOTES,notes);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.deviceNotes","{0}\nNotes",devTitles);
                }
            });

            // -- Device expirations
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_INSURANCE_EXP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    long licExp = 0L;
                    if (fd.hasValue(DATA_DEVICE_INSURANCE_EXP)) {
                        licExp = fd.getLong(DATA_DEVICE_INSURANCE_EXP);
                    } else {
                        Device device = fd.getDevice();
                        licExp = (device != null)? device.getInsuranceExpire() : 0L;
                    }
                    if (licExp > 0L) {
                        TimeZone tz = rd.getTimeZone();
                        long currdn = DateTime.getCurrentDayNumber(tz);
                        long days = licExp - currdn;
                        boolean expired = (days <= 0L)? true : false;
                        if (arg.equalsIgnoreCase("days")) {
                            I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                            String expStr = "";
                            ColorTools.RGB fgColor = null;
                            if (days < 0L) {
                                // -- Red: Expired
                                expStr = i18n.getString("FieldLayout.expired", "Expired");
                                fgColor = ColorTools.RED;
                            } else
                            if (days == 0L) {
                                // -- Red: Today
                                expStr = i18n.getString("FieldLayout.today", "Today");
                                fgColor = ColorTools.RED;
                            } else
                            if (days == 1L) {
                                // -- Yellow: Tomorrow
                                //expStr = i18n.getString("FieldLayout.nday", "{0} day", String.valueOf(days));
                                expStr = i18n.getString("FieldLayout.tomorrow", "Tomorrow");
                                fgColor = ColorTools.DARK_YELLOW;
                            } else
                            if (days <= 30L) {
                                // -- Yellow: N days from now
                                expStr = i18n.getString("FieldLayout.ndays", "{0} days", String.valueOf(days));
                                fgColor = ColorTools.DARK_YELLOW;
                            } else {
                                // -- Green/Black: N days from now
                                expStr = i18n.getString("FieldLayout.ndays", "{0} days", String.valueOf(days));
                                //fgColor = ColorTools.GREEN;
                            }
                            ColumnValue cv = new ColumnValue(expStr).setSortKey(licExp);
                            if (fgColor != null) {
                                cv.setForegroundColor(fgColor);
                            }
                            return fd.filterReturnedValue(DATA_DEVICE_INSURANCE_EXP, cv);
                        } else {
                            ReportLayout rl = rd.getReportLayout();
                            DayNumber dn = new DayNumber(licExp);
                            String dnFmt = dn.format(rl.getDateFormat(rd.getPrivateLabel()));
                            ColumnValue cv = new ColumnValue(dnFmt).setSortKey(licExp);
                            if (days <= 0L) {
                                cv.setForegroundColor(ColorTools.RED);
                            } else
                            if (days <= 30L) {
                                cv.setForegroundColor(ColorTools.DARK_YELLOW);
                            }
                            return fd.filterReturnedValue(DATA_DEVICE_INSURANCE_EXP, cv);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceInsuranceExpire","Device\nInsurance Exp");
                }
            });

            // -- Vehicle-ID (see also DATA_DEVICE_VIN)
            this.addColumnTemplate(new DataColumnTemplate(DATA_VEHICLE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String vin = fd.getString(DATA_VEHICLE_ID,null);
                    if (vin == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            vin = dev.getVehicleID();
                        }
                    }
                    return fd.filterReturnedValue(DATA_VEHICLE_ID,vin);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                  //return i18n.getString("FieldLayout.vehicleID","Vehicle-ID");
                    return i18n.getString("FieldLayout.deviceVehicleID","{0} ID",devTitles);
                }
            });

            // -- Vehicle Make
            this.addColumnTemplate(new DataColumnTemplate(DATA_VEHICLE_MAKE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String vid = fd.getString(DATA_VEHICLE_MAKE,null);
                    if (vid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            vid = dev.getVehicleMake();
                        }
                    }
                    return fd.filterReturnedValue(DATA_VEHICLE_MAKE,vid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.vehicleMake","Vehicle-Make");
                }
            });

            // -- Vehicle Model
            this.addColumnTemplate(new DataColumnTemplate(DATA_VEHICLE_MODEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String vid = fd.getString(DATA_VEHICLE_MODEL,null);
                    if (vid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            vid = dev.getVehicleModel();
                        }
                    }
                    return fd.filterReturnedValue(DATA_VEHICLE_MODEL,vid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.vehicleModel","Vehicle-Model");
                }
            });

            // -- License Plate
            this.addColumnTemplate(new DataColumnTemplate(DATA_LICENSE_PLATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String lic = fd.getString(DATA_LICENSE_PLATE,null);
                    if (lic == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            lic = dev.getLicensePlate();
                        }
                    }
                    return fd.filterReturnedValue(DATA_LICENSE_PLATE,lic);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.licensePlate","License\nPlate");
                }
            });

            // -- Equipment Type
            this.addColumnTemplate(new DataColumnTemplate(DATA_EQUIPMENT_TYPE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String eqType = fd.getString(DATA_EQUIPMENT_TYPE,null);
                    if (eqType == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            eqType = dev.getEquipmentType();
                        }
                    }
                    return fd.filterReturnedValue(DATA_EQUIPMENT_TYPE,eqType);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.equipmentType","Equipment\nType");
                }
            });

            // -- Unique-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_UNIQUE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String uid = fd.getString(DATA_UNIQUE_ID,null);
                    if (uid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            uid = dev.getUniqueID();
                        }
                    }
                    return fd.filterReturnedValue(DATA_UNIQUE_ID,uid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.uniqueID","Unique-ID");
                }
            });

            // -- Server-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVER_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String sid = fd.getString(DATA_SERVER_ID,null);
                    if (sid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            sid = dev.getDeviceCode();
                        }
                    }
                    return fd.filterReturnedValue(DATA_SERVER_ID,sid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.serverID","Server-ID");
                }
            });

            // -- JobNumber
            this.addColumnTemplate(new DataColumnTemplate(DATA_JOB_NUMBER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String job = fd.getString(DATA_JOB_NUMBER,"");
                    return fd.filterReturnedValue(DATA_JOB_NUMBER,job);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.jobNumber","Job#");
                }
            });

            // -- Group-ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GROUP_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String gid = fd.getString(DATA_GROUP_ID,null);
                    if (gid == null) {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            gid = dev.getGroupID();
                        }
                    }
                    return fd.filterReturnedValue(DATA_GROUP_ID,gid);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.groupID","Group-ID");
                }
            });

            // -- Date/Time
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_DATE)) {
                        // -- DayNumber
                        long ds = fd.getLong(DATA_DATE);
                        if (ds > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            TimeZone tz  = rd.getTimeZone();
                            DayNumber dn = new DayNumber(ds);
                            String dnFmt = dn.format(rl.getDateFormat(rd.getPrivateLabel()));
                            ColumnValue cv = new ColumnValue(dnFmt).setSortKey(ds);
                            return fd.filterReturnedValue(DATA_DATE, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_TIMESTAMP)) {
                        // -- Epoch timestamp
                        long ts = fd.getLong(DATA_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz  = rd.getTimeZone();
                            DateTime dt  = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateFormat(rd.getPrivateLabel()),tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_DATE, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.date","Date");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz = rd.getTimeZone();
                            DateTime dt = new DateTime(ts);
                            String tmFmt = dt.format(rl.getTimeFormat(rd.getPrivateLabel()),tz);
                            return fd.filterReturnedValue(DATA_TIME, tmFmt);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.time","Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            TimeZone     tz = rd.getTimeZone();
                            DateTime     dt = new DateTime(ts);
                            String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                            String    dtFmt = dt.format(fmtStr, tz);
                            ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.dateTime","Date/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_TIMESTAMP);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.timestamp","Timestamp") + "\n(Epoch)";
                }
            });

            // -- Creation Date/Time
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_DATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_CREATE_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_CREATE_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz  = rd.getTimeZone();
                            DateTime dt  = new DateTime(ts);
                            String dtFmt = dt.format(rl.getDateFormat(rd.getPrivateLabel()),tz);
                            ColumnValue cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_CREATE_DATE, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.createDate","Insert\nDate");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_TIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_CREATE_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_CREATE_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            //Account a = rd.getAccount();
                            //TimeZone tz = (a != null)? TimeZone.getTimeZone(a.getTimeZone()) : null;
                            TimeZone tz = rd.getTimeZone();
                            DateTime dt = new DateTime(ts);
                            return fd.filterReturnedValue(DATA_CREATE_TIME,dt.format(rl.getTimeFormat(rd.getPrivateLabel()),tz));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.createTime","Insert\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_CREATE_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_CREATE_TIMESTAMP);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            TimeZone     tz = rd.getTimeZone();
                            DateTime     dt = new DateTime(ts);
                            String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                            String    dtFmt = dt.format(fmtStr, tz);
                            ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_CREATE_DATETIME,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.createDateTime","Insert\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CREATE_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_CREATE_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_CREATE_TIMESTAMP);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_CREATE_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.createTimestamp","Insert\nTimestamp") + "\n(Epoch)";
                }
            });

            // -- Ping/Command time
            this.addColumnTemplate(new DataColumnTemplate(DATA_PING_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    Device    dev = fd.getDevice();
                    long       ts = fd.hasValue(DATA_PING_TIMESTAMP)? 
                        fd.getLong(DATA_PING_TIMESTAMP) : 
                        ((dev != null)? dev.getLastPingTime() : -1L);
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone     tz = rd.getTimeZone();
                        DateTime     dt = new DateTime(ts);
                        String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                        String    dtFmt = dt.format(fmtStr, tz);
                        ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                        return fd.filterReturnedValue(DATA_PING_DATETIME,cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.commandDateTime","Command\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PING_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    long      ts = fd.hasValue(DATA_PING_TIMESTAMP)? 
                        fd.getLong(DATA_PING_TIMESTAMP) : 
                        ((dev != null)? dev.getLastPingTime() : -1L);
                    if (ts > 0L) {
                        return fd.filterReturnedValue(DATA_PING_TIMESTAMP,String.valueOf(ts));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.commandTimestamp","Command\nTimestamp") + "\n(Epoch)";
                }
            });

            // -- Incident time
            this.addColumnTemplate(new DataColumnTemplate(DATA_INCIDENT_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    Device    dev = fd.getDevice();
                    long       ts = fd.getLong(DATA_INCIDENT_TIMESTAMP,-1L);
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone     tz = rd.getTimeZone();
                        DateTime     dt = new DateTime(ts);
                        String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                        String    dtFmt = dt.format(fmtStr, tz);
                        ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                        return fd.filterReturnedValue(DATA_INCIDENT_DATETIME,cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.incidentDateTime","Incident\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_INCIDENT_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    long      ts = fd.getLong(DATA_INCIDENT_TIMESTAMP,-1L);
                    if (ts > 0L) {
                        return fd.filterReturnedValue(DATA_INCIDENT_TIMESTAMP,String.valueOf(ts));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.incidentTimestamp","Incident\nTimestamp") + "\n(Epoch)";
                }
            });

            // -- ACK time
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACK_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    Device    dev = fd.getDevice();
                    long       ts = fd.hasValue(DATA_ACK_TIMESTAMP)? 
                        fd.getLong(DATA_ACK_TIMESTAMP) : 
                        ((dev != null)? dev.getLastAckTime() : -1L);
                    if (ts > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone     tz = rd.getTimeZone();
                        DateTime     dt = new DateTime(ts);
                        String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                        String    dtFmt = dt.format(fmtStr, tz);
                        ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                        return fd.filterReturnedValue(DATA_ACK_DATETIME,cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ackDateTime","ACK\nDate/Time") + "\n${timezone}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACK_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    long      ts = fd.hasValue(DATA_ACK_TIMESTAMP)? 
                        fd.getLong(DATA_ACK_TIMESTAMP) : 
                        ((dev != null)? dev.getLastAckTime() : -1L);
                    if (ts > 0L) {
                        return fd.filterReturnedValue(DATA_ACK_TIMESTAMP,String.valueOf(ts));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ackTimestamp","ACK\nTimestamp") + "\n(Epoch)";
                }
            });

            // -- Status Code/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_CODE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STATUS_CODE)) {
                        String arg    = rc.getArg();
                        int    code   = (int)fd.getLong(DATA_STATUS_CODE);
                        String scCode = "0x" + StringTools.toHexString(code,16);
                        if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("color")) {
                            Device dev = fd.getDevice();
                            BasicPrivateLabel bpl = rd.getPrivateLabel();
                            StatusCodeProvider scp = StatusCode.getStatusCodeProvider(dev,code,bpl,null/*dftSCP*/);
                            if (scp == null) {
                                return fd.filterReturnedValue(DATA_STATUS_CODE,scCode);
                            } else {
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(scCode);
                                cv.setForegroundColor(scp.getForegroundColor());
                                cv.setBackgroundColor(scp.getBackgroundColor());
                                return fd.filterReturnedValue(DATA_STATUS_CODE,cv);
                            }
                        } else {
                            return fd.filterReturnedValue(DATA_STATUS_CODE,scCode);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.statusCode","Status#");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg    = rc.getArg();
                    FieldData fd  = (FieldData)dataObj;
                    String scDesc = fd.getString(DATA_STATUS_DESC);
                    Device dev    = fd.getDevice();
                    BasicPrivateLabel bpl = rd.getPrivateLabel();
                    int    code   = (int)fd.getLong(DATA_STATUS_CODE, StatusCodes.STATUS_NONE);
                    if (StringTools.isBlank(scDesc) && (code != StatusCodes.STATUS_NONE)) {
                        scDesc = StatusCode.getDescription(dev,code,bpl,null);
                    }
                    if (!StringTools.isBlank(scDesc)) {
                        if (code == StatusCodes.STATUS_NONE) {
                            // -- no code specified to provide coloring
                            return fd.filterReturnedValue(DATA_STATUS_DESC,scDesc);
                        } else
                        if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("color")) {
                            // -- check for status code coloring
                            StatusCodeProvider scp = StatusCode.getStatusCodeProvider(dev,code,bpl,null/*dftSCP*/);
                            if (scp == null) {
                                return fd.filterReturnedValue(DATA_STATUS_DESC,scDesc);
                            } else {
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(scDesc);
                                cv.setForegroundColor(scp.getForegroundColor());
                                cv.setBackgroundColor(scp.getBackgroundColor());
                                return fd.filterReturnedValue(DATA_STATUS_DESC,cv);
                            }
                        } else {
                            // -- coloring not wanted
                            return fd.filterReturnedValue(DATA_STATUS_DESC,scDesc);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.statusDescription","Status");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATUS_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    FieldData fd  = (FieldData)dataObj;
                    String  sca[] = StringTools.split(rc.getArg(),',');
                    int     count = 0;
                    // -- accumulate individual status code counts
                    for (String sc : sca) {
                        int    code = StringTools.parseInt(sc,StatusCodes.STATUS_NONE);
                        String cKey = DATA_STATUS_COUNT + "_" + StringTools.toHexString(code,16);
                        if (fd.hasValue(cKey)) { // "statusCount_F020"
                            count += (int)fd.getLong(cKey);
                        }
                    }
                    // -- if no individual counts, try generic count
                    if ((count <= 0) && fd.hasValue(DATA_STATUS_COUNT)) {
                        count = (int)fd.getLong(DATA_STATUS_COUNT); // "statusCount"
                    }
                    // -- return value
                    if (count > 0) {
                        return fd.filterReturnedValue(DATA_STATUS_COUNT,String.valueOf(count));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String sca[] = StringTools.split(rc.getArg(),',');
                    String   SC0 = (sca.length > 0)? sca[0] : ""; // first entry
                    int     code = StringTools.parseInt(SC0,StatusCodes.STATUS_NONE);
                    Device   dev = null;
                    try { dev = rd.getDevice(rd.getFirstDeviceID()); } catch (Throwable th) {/*ignore*/}
                    String scDesc = StatusCode.getDescription(dev,code,rd.getPrivateLabel(),null);
                    if (StringTools.isBlank(scDesc)) {
                        // -- no description found for this statusCode, use hex value
                        scDesc = "0x" + StringTools.toHexString(code,16);
                    } else {
                        // -- replace " " and "_" with "\n"
                        StringBuffer sb = new StringBuffer();
                        int C = 0;
                        for (int i = 0; i < scDesc.length(); i++) {
                            char ch = scDesc.charAt(i);
                            if ((ch == '_') || (ch == ' ')) {
                                if (C >= 3) {
                                    sb.append("\n");
                                    C = 0;
                                } else {
                                    sb.append(ch);
                                    C++;
                                }
                            } else {
                                sb.append(ch);
                                C++;
                            }
                        }
                        scDesc = sb.toString();
                    }
                    return scDesc;
                }
            });

            /* EventSummary count/elapsed */
            this.addColumnTemplate(new DataColumnTemplate(DATA_EVENT_SUMMARY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    FieldData        fd = (FieldData)dataObj;
                    String       stName = StringTools.trim(rc.getArg());
                    EventSummaryType st = EventSummaryType.GetEventSummaryType(stName);
                    String          key = DATA_EVENT_SUMMARY + "_" + stName;
                    long          count = fd.getLong(key); // may be '0'
                    if (count > 0) {
                        if ((st != null) && st.isElapsedTime()) {
                            String hms = FieldLayout.formatElapsedTime(count, StringTools.ELAPSED_FORMAT_HHMMSS);
                            return fd.filterReturnedValue(DATA_EVENT_SUMMARY,hms);
                        } else {
                            return fd.filterReturnedValue(DATA_EVENT_SUMMARY,String.valueOf(count));
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String stName = StringTools.trim(rc.getArg());
                    EventSummaryType est = EventSummaryType.GetEventSummaryType(stName);
                    if (est != null) {
                        BasicPrivateLabel bpl = rd.getPrivateLabel();
                        int type    = est.getType();
                        int subtype = est.getSubtype();
                        return EventSummaryType.GetDescription(type,subtype,null/*device*/,bpl);
                      //return st.getDescription(locale);
                    } else {
                        return stName;
                    }
                }
            });

            /* Pushpins */
            this.addColumnTemplate(new DataColumnTemplate(DATA_PUSHPIN) {
                // -- EXPERIMENTAL! (the icons produced by this code section may not exactly match
                // -  those produced on the actual map by the JavaScript functions.
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    RequestProperties reqState = rd.getRequestProperties();
                    FieldData fd = (FieldData)dataObj;
                    String iconURL = fd.getString(DATA_PUSHPIN);
                    if (StringTools.isBlank(iconURL)) {
                        return rc.getBlankFiller();
                    } else
                    if (StringTools.isInt(iconURL,true)) {
                        int ppNdx = StringTools.parseInt(iconURL, 0);
                        PushpinIcon ppi = reqState.getPushpinIcon(ppNdx);
                        iconURL = (ppi != null)? ppi.getIconEvalURL(null/*EventData*/,rowNdx) : "";
                        ColumnValue cv = new ColumnValue().setImageURL(iconURL);
                        return fd.filterReturnedValue(DATA_PUSHPIN, cv);
                    } else {
                        ColumnValue cv = new ColumnValue().setImageURL(iconURL);
                        return fd.filterReturnedValue(DATA_PUSHPIN, cv);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.pushpin","Pushpin");
                }
            });

            // -- Property Key/Description/Value
            this.addColumnTemplate(new DataColumnTemplate(DATA_PROPERTY_KEY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_PROPERTY_KEY)) {
                        long propKey = fd.getLong(DATA_PROPERTY_KEY);
                        return fd.filterReturnedValue(DATA_PROPERTY_KEY,"0x"+StringTools.toHexString(propKey,16));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.propertyKey","Property\nKey");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PROPERTY_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = fd.getString(DATA_PROPERTY_DESC);
                    if (!StringTools.isBlank(desc)) {
                        return fd.filterReturnedValue(DATA_PROPERTY_DESC,desc);
                    } else
                    if (fd.hasValue(DATA_PROPERTY_KEY)) {
                        int propKey = (int)fd.getLong(DATA_PROPERTY_KEY);
                        //return PropertyKey.GetKeyDescription(propKey);
                        return fd.filterReturnedValue(DATA_PROPERTY_DESC,"0x"+StringTools.toHexString((long)propKey,16));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.propertyDescription","Property\nDescription");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PROPERTY_VALUE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String value = fd.getString(DATA_PROPERTY_VALUE);
                    return fd.filterReturnedValue(DATA_PROPERTY_VALUE,(value!=null)?value:"");
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.propertyValue","Property\nValue");
                }
            });

            // -- Diagnostic Key/Description/Value
            this.addColumnTemplate(new DataColumnTemplate(DATA_DIAGNOSTIC_ERROR) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String isErr = fd.getString(DATA_DIAGNOSTIC_ERROR);
                    if (!StringTools.isBlank(isErr)) {
                        return fd.filterReturnedValue(DATA_DIAGNOSTIC_ERROR,isErr); // "true" : "false";
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.diagnosticError","Is Error?");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DIAGNOSTIC_KEY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_DIAGNOSTIC_ERROR) || fd.hasValue(DATA_DIAGNOSTIC_KEY)) {
                        boolean isError = fd.getBoolean(DATA_DIAGNOSTIC_ERROR);
                        long    diagKey = fd.getLong(DATA_DIAGNOSTIC_KEY);
                        return fd.filterReturnedValue(DATA_DIAGNOSTIC_KEY,(isError?"[E]":"[D]")+"0x"+StringTools.toHexString(diagKey,16));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.diagnosticKey","Diagnostic\nKey");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DIAGNOSTIC_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = fd.getString(DATA_DIAGNOSTIC_DESC);
                    if (!StringTools.isBlank(desc)) {
                        return fd.filterReturnedValue(DATA_DIAGNOSTIC_DESC,desc);
                    } else
                    if (fd.hasValue(DATA_DIAGNOSTIC_KEY)) {
                        int diagKey = (int)fd.getLong(DATA_DIAGNOSTIC_KEY);
                        //return ClientDiagnostic.GetKeyDescription(diagKey);
                        return fd.filterReturnedValue(DATA_DIAGNOSTIC_DESC,"0x"+StringTools.toHexString((long)diagKey,16));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.diagnosticDescription","Diagnostic\nDescription");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DIAGNOSTIC_VALUE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String value = fd.getString(DATA_DIAGNOSTIC_VALUE);
                    return (value != null)? fd.filterReturnedValue(DATA_DIAGNOSTIC_VALUE,value) : "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.diagnosticValue","Diagnostic\nValue");
                }
            });

            // -- Entity-ID/Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = fd.getString(DATA_ENTITY_ID);
                    if (!StringTools.isBlank(desc)) {
                        return fd.filterReturnedValue(DATA_ENTITY_ID,desc);
                    } else {
                        return fd.filterReturnedValue(DATA_ENTITY_ID,fd.getString(DATA_ENTITY_DESC));
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.entityID","Entity-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_TYPE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    long type = fd.getLong(DATA_ENTITY_TYPE);
                    EntityManager.EntityType et = EntityManager.getEntityTypeFromCode(type,null);
                    if (et != null) {
                        Locale locale = rd.getLocale();
                        return fd.filterReturnedValue(DATA_ENTITY_TYPE,et.toString(locale));
                    } else {
                        return fd.filterReturnedValue(DATA_ENTITY_TYPE,String.valueOf(type));
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.entityType","Entity-Type");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTITY_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = fd.getString(DATA_ENTITY_DESC);
                    if (!StringTools.isBlank(desc)) {
                        return fd.filterReturnedValue(DATA_ENTITY_DESC,desc);
                    } else {
                        return fd.filterReturnedValue(DATA_ENTITY_DESC,fd.getString(DATA_ENTITY_ID));
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.entityDescription","Entity\nDescription");
                }
            });

            // -- Driver
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String driverID = fd.getDriverID(); // DATA_DRIVER_ID or fd.getDevice().getDriverID()
                    if (driverID != null) {
                        return fd.filterReturnedValue(DATA_DRIVER_ID,driverID);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverID","Driver-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = null;
                    if (fd.hasValue(DATA_DRIVER_DESC)) {
                        desc = fd.getString(DATA_DRIVER_DESC);
                    } else {
                        Driver driver = fd.getDriver();
                        desc = (driver != null)? driver.getDescription() : fd.getDriverID();
                    }
                    if (desc != null) {
                        return fd.filterReturnedValue(DATA_DRIVER_DESC,desc);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverDescription","Driver\nDescription");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_NICKNAME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String name = null;
                    if (fd.hasValue(DATA_DRIVER_NICKNAME)) {
                        name = fd.getString(DATA_DRIVER_NICKNAME,null);
                    } else
                    if (fd.hasValue(DATA_DISPLAY_NAME)) {
                        name = fd.getString(DATA_DISPLAY_NAME,null);
                    } else {
                        Driver driver = fd.getDriver();
                        name = (driver != null)? driver.getDisplayName() : null;
                    }
                    if (name != null) {
                        return fd.filterReturnedValue(DATA_DRIVER_NICKNAME,name);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverNickname","Driver\nNickname");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_STATUS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    long drvStat = Driver.DutyStatus_INVALID;
                    if (fd.hasValue(DATA_DRIVER_STATUS)) {
                        drvStat = fd.getLong(DATA_DRIVER_STATUS);
                    } else {
                        Driver driver = fd.getDriver();
                        drvStat = (driver != null)? driver.getDriverStatus() : Driver.DutyStatus_INVALID;
                        if ((drvStat < 0L) || (drvStat == Driver.DutyStatus_INVALID)) {
                            // -- Default to Device Driver Status
                            Device device = fd.getDevice();
                            if ((device != null) && device.hasDriverStatus()) {
                                drvStat = device.getDriverStatus();
                            }
                        }
                    }
                    if (drvStat <= Driver.DutyStatus_UNKNOWN) {
                        return rc.getBlankFiller();
                    } else
                    if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("desc")) {
                        Driver.DutyStatus ds = Driver.getDutyStatus(drvStat);
                        if (ds != null) {
                            Locale locale = rd.getLocale();
                            return fd.filterReturnedValue(DATA_DRIVER_STATUS,ds.toString(locale));
                        } else {
                            return fd.filterReturnedValue(DATA_DRIVER_STATUS,String.valueOf(drvStat));
                        }
                    } else {
                        return fd.filterReturnedValue(DATA_DRIVER_STATUS,String.valueOf(drvStat));
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverStatus","Driver\nStatus");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_BADGEID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String badge = null;
                    if (fd.hasValue(DATA_DRIVER_BADGEID)) {
                        badge = fd.getString(DATA_DRIVER_BADGEID);
                    } else {
                        Driver driver = fd.getDriver();
                        badge = (driver != null)? driver.getBadgeID() : null;
                    }
                    if (badge != null) {
                        return fd.filterReturnedValue(DATA_DRIVER_BADGEID,badge);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverBadge","Driver\nBadge-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_LICENSE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String lic = null;
                    if (fd.hasValue(DATA_DRIVER_LICENSE)) {
                        lic = fd.getString(DATA_DRIVER_LICENSE);
                    } else {
                        Driver driver = fd.getDriver();
                        lic = (driver != null)? driver.getLicenseNumber() : null;
                    }
                    if (lic != null) {
                        return fd.filterReturnedValue(DATA_DRIVER_LICENSE,lic);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverLicense","Driver\nLicense");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_LICENSE_TYPE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String type = null;
                    if (fd.hasValue(DATA_DRIVER_LICENSE_TYPE)) {
                        type = fd.getString(DATA_DRIVER_LICENSE_TYPE);
                    } else {
                        Driver driver = fd.getDriver();
                        type = (driver != null)? driver.getLicenseType() : null;
                    }
                    if (type != null) {
                        return fd.filterReturnedValue(DATA_DRIVER_LICENSE_TYPE,type);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverLicenseType","Driver\nLicense Type");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_LICENSE_EXP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    long licExp = 0L;
                    if (fd.hasValue(DATA_DRIVER_LICENSE_EXP)) {
                        licExp = fd.getLong(DATA_DRIVER_LICENSE_EXP);
                    } else {
                        Driver driver = fd.getDriver();
                        licExp = (driver != null)? driver.getLicenseExpire() : 0L;
                    }
                    if (licExp > 0L) {
                        TimeZone tz = rd.getTimeZone();
                        long currdn = DateTime.getCurrentDayNumber(tz);
                        long days = licExp - currdn;
                        boolean expired = (days <= 0L)? true : false;
                        if (arg.equalsIgnoreCase("days")) {
                            I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                            String expStr = "";
                            ColorTools.RGB fgColor = null;
                            if (days < 0L) {
                                // -- Red: Expired
                                expStr = i18n.getString("FieldLayout.expired", "Expired");
                                fgColor = ColorTools.RED;
                            } else
                            if (days == 0L) {
                                // -- Red: Today
                                expStr = i18n.getString("FieldLayout.today", "Today");
                                fgColor = ColorTools.RED;
                            } else
                            if (days == 1L) {
                                // -- Yellow: Tomorrow
                                //expStr = i18n.getString("FieldLayout.nday", "{0} day", String.valueOf(days));
                                expStr = i18n.getString("FieldLayout.tomorrow", "Tomorrow");
                                fgColor = ColorTools.DARK_YELLOW;
                            } else
                            if (days <= 30L) {
                                // -- Yellow: N days from now
                                expStr = i18n.getString("FieldLayout.ndays", "{0} days", String.valueOf(days));
                                fgColor = ColorTools.DARK_YELLOW;
                            } else {
                                // -- Green/Black: N days from now
                                expStr = i18n.getString("FieldLayout.ndays", "{0} days", String.valueOf(days));
                                //fgColor = ColorTools.GREEN;
                            }
                            ColumnValue cv = new ColumnValue(expStr).setSortKey(licExp);
                            if (fgColor != null) {
                                cv.setForegroundColor(fgColor);
                            }
                            return fd.filterReturnedValue(DATA_DRIVER_LICENSE_EXP, cv);
                        } else {
                            ReportLayout rl = rd.getReportLayout();
                            DayNumber dn = new DayNumber(licExp);
                            String dnFmt = dn.format(rl.getDateFormat(rd.getPrivateLabel()));
                            ColumnValue cv = new ColumnValue(dnFmt).setSortKey(licExp);
                            if (days <= 0L) {
                                cv.setForegroundColor(ColorTools.RED);
                            } else
                            if (days <= 30L) {
                                cv.setForegroundColor(ColorTools.DARK_YELLOW);
                            }
                            return fd.filterReturnedValue(DATA_DRIVER_LICENSE_EXP, cv);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverLicenseExpire","Driver\nLicense Exp");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_BIRTHDATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    long bDay = 0L;
                    if (fd.hasValue(DATA_DRIVER_BIRTHDATE)) {
                        // -- explicitly specified
                        bDay = fd.getLong(DATA_DRIVER_BIRTHDATE);
                    } else {
                        Driver driver = fd.getDriver();
                        bDay = (driver != null)? driver.getBirthdate() : 0L;
                    }
                    if (bDay > 0L) {
                        ReportLayout rl = rd.getReportLayout();
                        DayNumber dn = new DayNumber(bDay);
                        String dnFmt = dn.format(rl.getDateFormat(rd.getPrivateLabel()));
                        ColumnValue cv = new ColumnValue(dnFmt).setSortKey(bDay);
                        return fd.filterReturnedValue(DATA_DRIVER_BIRTHDATE, cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driverBirthday","Driver\nBirthday");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_DEVICE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String deviceID = null;
                    if (fd.hasValue(DATA_DRIVER_DEVICE_ID)) {
                        deviceID = fd.getString(DATA_DRIVER_DEVICE_ID);
                    } else
                    if (fd.hasValue(DATA_DEVICE_ID)) {
                        deviceID = fd.getString(DATA_DEVICE_ID);
                    } else {
                        Driver driver = fd.getDriver();
                        deviceID = (driver != null)? driver.getDeviceID() : null;
                    }
                    if (deviceID != null) {
                        return fd.filterReturnedValue(DATA_DRIVER_DEVICE_ID,deviceID);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.driverDeviceID","Driver\n{0} ID",devTitles);
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVER_DEVICE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String desc = null;
                    if (fd.hasValue(DATA_DRIVER_DEVICE_DESC)) {
                        desc = fd.getString(DATA_DRIVER_DEVICE_DESC,null);
                    } else {
                        Driver driver = fd.getDriver();
                        Device device = (driver != null)? driver.getDevice() : null;
                        desc = (device != null)? device.getDescription() : null;
                    }
                    if (desc != null) {
                        return fd.filterReturnedValue(DATA_DRIVER_DEVICE_DESC,desc);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    RequestProperties reqState = rd.getRequestProperties();
                    String devTitles[] = (reqState != null)? reqState.getDeviceTitles() : Device.GetTitles(locale);
                    return i18n.getString("FieldLayout.driverDeviceDesc","Driver\n{0}",devTitles);
                }
            });

            // -- Latitude/Longitude/GeoPoint
            this.addColumnTemplate(new DataColumnTemplate(DATA_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_GEOPOINT) || fd.hasValue(DATA_LATITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude() : fd.getDouble(DATA_LATITUDE);
                        if (GeoPoint.isValid(lat,1.0)) {
                            String args[] = StringTools.split(arg,',');
                            String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                            String valStr = GeoPoint.formatLatitude(lat, latlonFmt, locale);
                            return fd.filterReturnedValue(DATA_LATITUDE,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lat","Lat");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_GEOPOINT) || fd.hasValue(DATA_LONGITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_LONGITUDE);
                        if (GeoPoint.isValid(1.0,lon)) {
                            String args[] = StringTools.split(arg,',');
                            String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                            String valStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                            return fd.filterReturnedValue(DATA_LONGITUDE,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lon","Lon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_GEOPOINT) || fd.hasValue(DATA_LATITUDE) || fd.hasValue(DATA_LONGITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude()  : fd.getDouble(DATA_LATITUDE);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_LONGITUDE);
                        if (GeoPoint.isValid(lat,lon)) {
                            String args[] = StringTools.split(arg,',');
                            String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                            String latStr = GeoPoint.formatLatitude( lat, latlonFmt, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                            String valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            // -- TODO: "mapLink"?
                            return fd.filterReturnedValue(DATA_GEOPOINT,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.latLon","Lat/Lon");
                }
            });

            // -- Best Latitude/Longitude/GeoPoint
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    double lat = 999.0;
                    if (fd.hasValue(DATA_BEST_GEOPOINT)) {
                        lat = GeoPoint.getLatitude(fd.getGeoPoint(DATA_BEST_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_BEST_LATITUDE)) {
                        lat = fd.getDouble(DATA_BEST_LATITUDE);
                    } else
                    if (fd.hasValue(DATA_GEOPOINT)) {
                        lat = GeoPoint.getLatitude(fd.getGeoPoint(DATA_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_LATITUDE)) {
                        lat = fd.getDouble(DATA_LATITUDE);
                    }
                    if (GeoPoint.isValid(lat,1.0)) {
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String valStr = GeoPoint.formatLatitude(lat, latlonFmt, locale);
                        return fd.filterReturnedValue(DATA_BEST_LATITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bestLat","Lat");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    double lon = 999.0;
                    if (fd.hasValue(DATA_BEST_GEOPOINT)) {
                        lon = GeoPoint.getLongitude(fd.getGeoPoint(DATA_BEST_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_BEST_LONGITUDE)) {
                        lon = fd.getDouble(DATA_BEST_LONGITUDE);
                    } else
                    if (fd.hasValue(DATA_GEOPOINT)) {
                        lon = GeoPoint.getLongitude(fd.getGeoPoint(DATA_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_LONGITUDE)) {
                        lon = fd.getDouble(DATA_LONGITUDE);
                    }
                    if (GeoPoint.isValid(1.0,lon)) {
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String valStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                        return fd.filterReturnedValue(DATA_BEST_LONGITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bestLon","Lon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BEST_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    double lat = 999.0;
                    double lon = 999.0;
                    if (fd.hasValue(DATA_BEST_GEOPOINT)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_BEST_GEOPOINT);
                        lat = GeoPoint.getLatitude( gp,999.0);
                        lon = GeoPoint.getLongitude(gp,999.0);
                    } else
                    if (fd.hasValue(DATA_BEST_LATITUDE) && fd.hasValue(DATA_BEST_LONGITUDE)) {
                        lat = fd.getDouble(DATA_BEST_LATITUDE );
                        lon = fd.getDouble(DATA_BEST_LONGITUDE);
                    } else
                    if (fd.hasValue(DATA_GEOPOINT)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                        lat = GeoPoint.getLatitude( gp,999.0);
                        lon = GeoPoint.getLongitude(gp,999.0);
                    } else
                    if (fd.hasValue(DATA_LATITUDE) && fd.hasValue(DATA_LONGITUDE)) {
                        lat = fd.getDouble(DATA_LATITUDE );
                        lon = fd.getDouble(DATA_LONGITUDE);
                    }
                    if (GeoPoint.isValid(lat,lon)) {
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String latStr = GeoPoint.formatLatitude( lat, latlonFmt, locale);
                        String lonStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                        String valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        return fd.filterReturnedValue(DATA_BEST_GEOPOINT,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bestLatLon","Lat/Lon");
                }
            });

            // -- Last Latitude/Longitude/GeoPoint
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device device = fd.getDevice();
                    double lat = 999.0;
                    if (fd.hasValue(DATA_LAST_GEOPOINT)) {
                        lat = GeoPoint.getLatitude(fd.getGeoPoint(DATA_LAST_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_LAST_LATITUDE)) {
                        lat = fd.getDouble(DATA_LAST_LATITUDE);
                    } else
                    if (device != null) {
                        lat = device.getLastValidLatitude();
                    }
                    if (GeoPoint.isValid(lat,1.0)) {
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String valStr = GeoPoint.formatLatitude(lat, latlonFmt, locale);
                        return fd.filterReturnedValue(DATA_LAST_LATITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastLat","Last\nLat");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device device = fd.getDevice();
                    double lon = 999.0;
                    if (fd.hasValue(DATA_LAST_GEOPOINT)) {
                        lon = GeoPoint.getLongitude(fd.getGeoPoint(DATA_LAST_GEOPOINT),999.0);
                    } else
                    if (fd.hasValue(DATA_LAST_LONGITUDE)) {
                        lon = fd.getDouble(DATA_LAST_LONGITUDE);
                    } else
                    if (device != null) {
                        lon = device.getLastValidLongitude();
                    }
                    if (GeoPoint.isValid(1.0,lon)) {
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String valStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                        return fd.filterReturnedValue(DATA_LAST_LONGITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastLon","Last\nLon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device device = fd.getDevice();
                    double lat = 999.0;
                    double lon = 999.0;
                    if (fd.hasValue(DATA_LAST_GEOPOINT)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_LAST_GEOPOINT);
                        lat = GeoPoint.getLatitude( gp,999.0);
                        lon = GeoPoint.getLongitude(gp,999.0);
                    } else
                    if (fd.hasValue(DATA_LAST_LATITUDE) && fd.hasValue(DATA_LAST_LONGITUDE)) {
                        lat = fd.getDouble(DATA_LAST_LATITUDE );
                        lon = fd.getDouble(DATA_LAST_LONGITUDE);
                    } else 
                    if (device != null) {
                        lat = device.getLastValidLatitude();
                        lon = device.getLastValidLongitude();
                    }
                    if (GeoPoint.isValid(lat,lon)) {
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String latStr = GeoPoint.formatLatitude( lat, latlonFmt, locale);
                        String lonStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                        String valStr = latStr + GeoPoint.PointSeparator + lonStr;
                        return fd.filterReturnedValue(DATA_LAST_GEOPOINT,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastLatLon","Last\nLat/Lon");
                }
            });

            // -- Last Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device device = fd.getDevice();
                    if (fd.hasValue(DATA_LAST_ADDRESS)) {
                        return fd.filterReturnedValue(DATA_LAST_ADDRESS,fd.getString(DATA_LAST_ADDRESS));
                    } else
                    if (device != null) {
                        try {
                            EventData ev = device.getLastEvent(-1, true);
                            if (ev != null) {
                                String addr = ev.getAddress();
                                return fd.filterReturnedValue(DATA_LAST_ADDRESS,addr);
                            } else {
                                return rc.getBlankFiller();
                            }
                        } catch (DBException dbe) {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    RequestProperties reqState = rd.getRequestProperties();
                    String addrTitles[] = (reqState != null)? reqState.getAddressTitles() : null;
                    String addrTitle    = (ListTools.size(addrTitles) > 0)? addrTitles[0] : null;
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    if (!StringTools.isBlank(addrTitle)) {
                        return i18n.getString("FieldLayout.lastAddress","Last {0}", addrTitle);
                    } else {
                        addrTitle = i18n.getString("FieldLayout.address","Address");
                        return i18n.getString("FieldLayout.lastAddress","Last {0}", addrTitle);
                    }
                }
            });

            // -- Altitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_ALTITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ALTITUDE)) {
                        Account.AltitudeUnits altUnits = Account.getAltitudeUnits(rd.getAccount());
                        double alt = altUnits.convertFromMeters(fd.getDouble(DATA_ALTITUDE));
                        return fd.filterReturnedValue(DATA_ALTITUDE,formatDouble(alt,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.altitude","Altitude") + "\n${altitudeUnits}";
                }
            });

            // -- Speed
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_LIMIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    if (fd.hasValue(DATA_SPEED_LIMIT)) {
                        double kph = fd.getDouble(DATA_SPEED_LIMIT); // KPH
                        if (kph <= 0.0) {
                            //return i18n.getString("FieldLayout.notAvailable","n/a");
                            return rc.getBlankFiller();
                        } else
                        if (kph > 250.0) {
                            return i18n.getString("FieldLayout.notAvailable","n/a");
                        } else {
                            Account a = rd.getAccount();
                            return fd.filterReturnedValue(DATA_SPEED_LIMIT,formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph),arg,"0"));
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speedLimit","Speed Limit") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_SPEED)) {
                        double kph = fd.getDouble(DATA_SPEED); // KPH
                        if (kph > 0.0) {
                            Account a = rd.getAccount();
                            return fd.filterReturnedValue(DATA_SPEED,formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph),arg,"0"));
                        } else {
                            return fd.filterReturnedValue(DATA_SPEED,"0   ");
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speed","Speed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_HEADING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_SPEED)) {
                        double kph = fd.getDouble(DATA_SPEED); // KPH
                        if (kph > 0.0) {
                            Account a = rd.getAccount();
                            String speedStr = formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph), arg, "0");
                            String headStr  = GeoPoint.GetHeadingString(fd.getDouble(DATA_HEADING),rd.getLocale()).toUpperCase();
                            if (headStr.length() == 1) {
                                headStr += " ";
                            }
                            return fd.filterReturnedValue(DATA_SPEED_HEADING,speedStr+" "+headStr);
                        } else {
                            return "0   ";
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speed","Speed") + "\n${speedUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_UNITS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_SPEED)) {
                        double kph = fd.getDouble(DATA_SPEED); // KPH
                        if (kph > 0.0) {
                            Account a = rd.getAccount();
                            String unitAbbr = Account.getSpeedUnits(a).toString(rd.getLocale());
                            String speedStr = formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph), arg, "0");
                            String headStr  = GeoPoint.GetHeadingString(fd.getDouble(DATA_HEADING),rd.getLocale()).toUpperCase();
                            if (headStr.length() == 1) {
                                headStr += " ";
                            }
                            return fd.filterReturnedValue(DATA_SPEED_UNITS,speedStr+unitAbbr+" "+headStr);
                        } else {
                            return "0    ";
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speed","Speed");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_DURATION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_SPEED_DURATION)) {
                        long duraSec = fd.getLong(DATA_SPEED_DURATION); // seconds
                        if (duraSec >= 0L) { // >= 0 to indicate start of speeding
                            String ds;
                            if (arg.equalsIgnoreCase("sec")) {
                                I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                                ds = i18n.getString("FieldLayout.durationSeconds","{0} sec",String.valueOf(duraSec));
                            } else {
                                int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                                ds = FieldLayout.formatElapsedTime(duraSec, fmt);
                            }
                            ColumnValue cv = new ColumnValue(ds).setSortKey(duraSec);
                            return fd.filterReturnedValue(DATA_SPEED_DURATION, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speedingDuration","Speeding\nDuration");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_SPEED_MAXIMUM) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_SPEED_MAXIMUM)) {
                        double kph = fd.getDouble(DATA_SPEED_MAXIMUM); // km/h
                        if (kph > 0.0) {
                            Account a = rd.getAccount();
                            return fd.filterReturnedValue(DATA_SPEED_MAXIMUM,formatDouble(Account.getSpeedUnits(a).convertFromKPH(kph),arg,"0"));
                        } else {
                            return fd.filterReturnedValue(DATA_SPEED_MAXIMUM,"0   ");
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.speedMaximum","Max. Speed") + "\n${speedUnits}";
                }
            });

            // -- Heading
            this.addColumnTemplate(new DataColumnTemplate(DATA_HEADING) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_SPEED)) {
                        double speed = fd.getDouble(DATA_SPEED); // KPH
                        if (speed > 0.0) {
                            double heading = fd.getDouble(DATA_HEADING);
                            if (!StringTools.isBlank(arg)) {
                                return fd.filterReturnedValue(DATA_HEADING,formatDouble(heading, arg, "0"));
                            } else {
                                return fd.filterReturnedValue(DATA_HEADING,GeoPoint.GetHeadingString(heading,rd.getLocale()).toUpperCase());
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return GeoPoint.GetHeadingTitle(rd.getLocale());
                }
            });

            // -- Distance/Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_DISTANCE)) {
                        double dist = fd.getDouble(DATA_DISTANCE); // kilometers
                        if (dist > 0.0) {
                            dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                            return fd.filterReturnedValue(DATA_DISTANCE,formatDouble(dist, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.distance","Distance") + "\n${distanceUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PLAN_DISTANCE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String    arg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    Device    dev = fd.getDevice();
                    double distKM = fd.hasValue(DATA_PLAN_DISTANCE)? 
                        fd.getDouble(DATA_PLAN_DISTANCE) : 
                        ((dev != null)? dev.getPlanDistanceKM() : -1.0);
                    if (distKM > 0.0) {
                        double dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(distKM);
                        return fd.filterReturnedValue(DATA_PLAN_DISTANCE,formatDouble(dist, arg, "#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.plannedDistance","Planned\nDistance") + "\n${distanceUnits}";
                }
            });

            // -- Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    double odom = 0.0;
                    if (fd.hasValue(DATA_ODOMETER)) {
                        odom = fd.getDouble(DATA_ODOMETER); // kilometers
                    } else
                    if (fd.hasValue(DATA_DISTANCE)) {
                        odom = fd.getDouble(DATA_DISTANCE); // kilometers
                    } else
                    if (dev != null) {
                        odom = dev.getLastOdometerKM();
                    }
                    if (odom > 0.0) {
                        if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                            odom += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                        } else
                        if (dev != null) {
                            odom += dev.getOdometerOffsetKM(); // ok
                        }
                        odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(odom);
                        return fd.filterReturnedValue(DATA_ODOMETER,formatDouble(odom,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.odometer","Odometer") + "\n${distanceUnits}";
                }
            });

            // -- Start/Stop/Delta Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_ODOMETER)) {
                        double odom = fd.getDouble(DATA_START_ODOMETER); // kilometers
                        if (odom > 0.0) {
                            Device dev = fd.getDevice();
                            if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                                odom += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                            } else
                            if (dev != null) {
                                odom += dev.getOdometerOffsetKM();
                            }
                            odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(odom);
                            return fd.filterReturnedValue(DATA_START_ODOMETER,formatDouble(odom, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startOdometer","Start\nOdometer") + "\n${distanceUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_ODOMETER)) {
                        double odom = fd.getDouble(DATA_STOP_ODOMETER); // kilometers
                        if (odom > 0.0) {
                            Device dev = fd.getDevice();
                            if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                                odom += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                            } else
                            if (dev != null) {
                                odom += dev.getOdometerOffsetKM();
                            }
                            odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(odom);
                            return fd.filterReturnedValue(DATA_STOP_ODOMETER,formatDouble(odom, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopOdometer","Odometer") + "\n${distanceUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STARTSTOP_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    double startOdom = fd.getDouble(DATA_START_ODOMETER, -1.0);
                    double stopOdom  = fd.getDouble(DATA_STOP_ODOMETER , -1.0);
                    if ((startOdom <= 0.0) && (stopOdom <= 0.0)) {
                        return rc.getBlankFiller();
                    }
                    Device dev      = fd.getDevice();
                    double odomOffs = fd.hasValue(DATA_ODOMETER_OFFSET)? fd.getDouble(DATA_ODOMETER_OFFSET) : (dev != null)? dev.getOdometerOffsetKM() : 0.0;
                    StringBuffer sb = new StringBuffer();
                    if (startOdom > 0.0) {
                        double odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(startOdom + odomOffs);
                        sb.append(formatDouble(odom, arg, "#0"));
                    }
                    if (stopOdom > 0.0) {
                        double odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(stopOdom + odomOffs);
                        if (sb.length() > 0) { sb.append("\n"); }
                        sb.append(formatDouble(odom, arg, "#0"));
                    }
                    return fd.filterReturnedValue(DATA_STARTSTOP_ODOMETER, sb.toString());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startStopOdometer","Start/Stop Odometer") + "\n${distanceUnits}";
                }
            });

            // -- Odometer Delta (ie. "Miles Driven")
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER_DELTA) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ODOMETER_DELTA)) {
                        double deltaOdom = fd.getDouble(DATA_ODOMETER_DELTA); // kilometers
                        if (deltaOdom >= 0.0) {
                            deltaOdom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(deltaOdom);
                            return fd.filterReturnedValue(DATA_ODOMETER_DELTA,formatDouble(deltaOdom,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        // -- start odometer
                        double startOdom = -1.0;
                        if (fd.hasValue(DATA_START_ODOMETER)) {
                            startOdom = fd.getDouble(DATA_START_ODOMETER); // kilometers
                        } else
                        if (fd.hasValue(DATA_ENTER_ODOMETER)) {
                            startOdom = fd.getDouble(DATA_ENTER_ODOMETER); // kilometers
                        }
                        // -- stop odometer
                        double stopOdom  = -1.0;  // kilometers
                        if (fd.hasValue(DATA_STOP_ODOMETER)) {
                            stopOdom  = fd.getDouble(DATA_STOP_ODOMETER);  // kilometers
                        } else
                        if (fd.hasValue(DATA_EXIT_ODOMETER)) {
                            stopOdom  = fd.getDouble(DATA_EXIT_ODOMETER);  // kilometers
                        }
                        // -- calc delta odometer
                        if ((startOdom >= 0.0) && (stopOdom >= 0.0) && (stopOdom >= startOdom)) {
                            double deltaOdom = stopOdom - startOdom;
                            deltaOdom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(deltaOdom);
                            return fd.filterReturnedValue(DATA_ODOMETER_DELTA,formatDouble(deltaOdom,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.driven","Driven") + "\n${distanceUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER_DELTA_WH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ODOMETER_DELTA_WH)) {
                        double deltaOdom = fd.getDouble(DATA_ODOMETER_DELTA_WH); // kilometers
                        if (deltaOdom >= 0.0) {
                            deltaOdom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(deltaOdom);
                            return fd.filterReturnedValue(DATA_ODOMETER_DELTA_WH,formatDouble(deltaOdom,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                 }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.drivenWorkHours","Work Hour") + "\n${distanceUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER_DELTA_AH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ODOMETER_DELTA_AH)) {
                        double deltaOdom = fd.getDouble(DATA_ODOMETER_DELTA_AH); // kilometers
                        if (deltaOdom >= 0.0) {
                            deltaOdom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(deltaOdom);
                            return fd.filterReturnedValue(DATA_ODOMETER_DELTA_AH,formatDouble(deltaOdom,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_ODOMETER_DELTA) && fd.hasValue(DATA_ODOMETER_DELTA_WH)) {
                        double deltaOdomTot = fd.getDouble(DATA_ODOMETER_DELTA);    // kilometers
                        double deltaOdomWH  = fd.getDouble(DATA_ODOMETER_DELTA_WH); // kilometers
                        if ((deltaOdomWH >= 0.0) && (deltaOdomTot >= deltaOdomWH)) {
                            double deltaOdom = deltaOdomTot - deltaOdomWH;
                            deltaOdom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(deltaOdom);
                            return fd.filterReturnedValue(DATA_ODOMETER_DELTA_AH,formatDouble(deltaOdom,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                 }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.drivenAfterHours","After Hour") + "\n${distanceUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ODOMETER_DELTA_BIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ODOMETER_DELTA_BIT)) {
                        double deltaOdom = fd.getDouble(DATA_ODOMETER_DELTA_BIT); // kilometers
                        if (deltaOdom >= 0.0) {
                            deltaOdom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(deltaOdom);
                            return fd.filterReturnedValue(DATA_ODOMETER_DELTA_BIT,formatDouble(deltaOdom,arg,"#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                 }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.drivenInputBit","Odometer(bit)") + "\n${distanceUnits}";
                }
            });

            // -- Last Service Hours
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_LAST_HR0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    double hours = 0.0;
                    int    hrNdx = 0;
                    if (fd.hasValue(DATA_SERVICE_LAST_HR0)) {
                        hours = fd.getDouble(DATA_SERVICE_LAST_HR0); // hours
                    } else
                    if (dev != null) {
                        hours = dev.getMaintEngHoursHR(hrNdx); // hours
                    } else {
                        // -- not available
                    }
                    if (hours > 0.0) {
                        if (dev != null) {
                            hours += dev.getEngineHoursOffset();
                        }
                        return fd.filterReturnedValue(DATA_SERVICE_LAST_HR0,formatDouble(hours,arg,"#0.0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintLastServiceHR","Last Service\nHours");
                }
            });
            // -- Service Interval Hours
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_INTERVAL_HR0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    double hours = 0.0;
                    int    hrNdx = 0;
                    if (fd.hasValue(DATA_SERVICE_INTERVAL_HR0)) {
                        hours = fd.getDouble(DATA_SERVICE_INTERVAL_HR0); // hours
                    } else {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            hours = dev.getMaintIntervalHR(hrNdx); // hours
                        }
                    }
                    if (hours > 0.0) {
                        return fd.filterReturnedValue(DATA_SERVICE_INTERVAL_HR0,formatDouble(hours,arg,"#0.0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintIntervalHR","Service\nInterval\nHours");
                }
            });
            // -- Next Service Hours
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_NEXT_HR0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    double hours = 0.0;
                    int    hrNdx = 0;
                    if (fd.hasValue(DATA_SERVICE_NEXT_HR0)) {
                        hours = fd.getDouble(DATA_SERVICE_NEXT_HR0); // hours
                    } else
                    if (dev != null) {
                        hours = dev.getMaintEngHoursHR(hrNdx) + dev.getMaintIntervalHR(hrNdx); // hours
                    }
                    if (hours > 0.0) {
                        if (dev != null) {
                            hours += dev.getEngineHoursOffset();
                        }
                        String hoursS = formatDouble(hours, arg, "#0.0");
                        double engHrs = dev.getLastEngineHours();
                        if (dev != null) {
                            engHrs += dev.getEngineHoursOffset(); // ok
                        }
                        if ((engHrs > 0.0) && (engHrs >= hours)) {
                            // -- beyond service time
                            return (new ColumnValue(hoursS)).setForegroundColor(ColorTools.RED);
                        } else {
                            // -- have not reached service time yet
                            return fd.filterReturnedValue(DATA_SERVICE_NEXT_HR0,hoursS);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintNextServiceHR","Next Service\nHours");
                }
            });
            // -- Remaining hours until next Service (next - engHours)
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_REMAINING_HR0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String       arg = rc.getArg();
                    FieldData     fd = (FieldData)dataObj;
                    Device       dev = fd.getDevice();
                    double remaining = 0.0;
                    int        hrNdx = 0;
                    if (fd.hasValue(DATA_SERVICE_REMAINING_HR0)) {
                        remaining = fd.getDouble(DATA_SERVICE_REMAINING_HR0); // hours
                    } else {
                        // -- next
                        double next = 0.0;
                        if (fd.hasValue(DATA_SERVICE_NEXT_HR0)) {
                            next = fd.getDouble(DATA_SERVICE_NEXT_HR0); // hours
                        } else
                        if (dev != null) {
                            next = dev.getMaintEngHoursHR(hrNdx) + dev.getMaintIntervalHR(hrNdx); // hours
                        }
                        // -- hours
                        double hours = 0.0;
                        if (fd.hasValue(DATA_ENGINE_HOURS)) {
                            hours = fd.getDouble(DATA_ENGINE_HOURS); // hours
                        } else
                        if (dev != null) {
                            hours = dev.getLastEngineHours();
                        }
                        remaining = ((next > 0.0) && (hours > 0.0))? (next - hours) : 0.0;
                    }
                    if (remaining != 0.0) { // may be < 0.0
                        String hoursS = formatDouble(remaining, arg, "#0.0");
                        if (remaining >= 0.0) {
                            return fd.filterReturnedValue(DATA_SERVICE_REMAINING_HR0,hoursS);
                        } else {
                            return (new ColumnValue(hoursS)).setForegroundColor(ColorTools.RED);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintRemainingHR","Remaining\nHours");
                }
            });

            // -- Last Service Odometer km #0
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_LAST_KM0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    double  dist = 0.0;
                    int    kmNdx = 0;
                    if (fd.hasValue(DATA_SERVICE_LAST_KM0)) {
                        dist = fd.getDouble(DATA_SERVICE_LAST_KM0); // kilometers
                    } else
                    if (dev != null) {
                        dist = dev.getMaintOdometerKM(kmNdx); // kilometers
                    } else {
                        // -- not available
                    }
                    if (dist > 0.0) {
                        if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                            dist += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                        } else
                        if (dev != null) {
                            dist += dev.getOdometerOffsetKM(); // ok
                        }
                        dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        return fd.filterReturnedValue(DATA_SERVICE_LAST_KM0,formatDouble(dist,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintLastService","Last Service") + "\n${distanceUnits}";
                }
            });
            // -- Service Interval km #0
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_INTERVAL_KM0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    double dist = 0.0;
                    int kmNdx = 0;
                    if (fd.hasValue(DATA_SERVICE_INTERVAL_KM0)) {
                        dist = fd.getDouble(DATA_SERVICE_INTERVAL_KM0); // kilometers
                    } else {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            dist = dev.getMaintIntervalKM(kmNdx); // kilometers
                        }
                    }
                    if (dist > 0.0) {
                        dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        return fd.filterReturnedValue(DATA_SERVICE_INTERVAL_KM0,formatDouble(dist,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintInterval","Service\nInterval");
                }
            });
            // -- Next Service Odometer km #0
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_NEXT_KM0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    double dist = 0.0;
                    int kmNdx = 0;
                    if (fd.hasValue(DATA_SERVICE_NEXT_KM0)) {
                        dist = fd.getDouble(DATA_SERVICE_NEXT_KM0); // kilometers
                    } else
                    if (dev != null) {
                        dist = dev.getMaintOdometerKM(kmNdx) + dev.getMaintIntervalKM(kmNdx); // km
                    }
                    if (dist > 0.0) {
                        // -- a "Next Service Interval" has been specified
                        if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                            dist += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                        } else
                        if (dev != null) {
                            dist += dev.getOdometerOffsetKM(); // ok
                        }
                        double distU = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        String distS = formatDouble(distU, arg, "#0");
                        // -- get last odometer value
                        double odom  = dev.getLastOdometerKM(); // DATA_ODOMETER?
                        if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                            odom += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                        } else
                        if (dev != null) {
                            odom += dev.getOdometerOffsetKM(); // ok
                        }
                        // -- check for elapsed interval
                        if ((odom > 0.0) && (odom >= dist)) {
                            // -- beyond service time
                            return (new ColumnValue(distS)).setForegroundColor(ColorTools.RED);
                        } else {
                            // -- have not reached service time yet
                            return fd.filterReturnedValue(DATA_SERVICE_NEXT_KM0,distS);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintNextService","Next Service") + "\n${distanceUnits}";
                }
            });
            // -- Remaining Km until next Service (next - odometer) #0
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_REMAINING_KM0) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    double remaining = 0.0;
                    int kmNdx = 0;
                    if (fd.hasValue(DATA_SERVICE_REMAINING_KM0)) {
                        remaining = fd.getDouble(DATA_SERVICE_REMAINING_KM0); // kilometers
                    } else {
                        // -- next
                        double next = 0.0;
                        if (fd.hasValue(DATA_SERVICE_NEXT_KM0)) {
                            next = fd.getDouble(DATA_SERVICE_NEXT_KM0); // kilometers
                        } else
                        if (dev != null) {
                            next = dev.getMaintOdometerKM(kmNdx) + dev.getMaintIntervalKM(kmNdx); // km
                        }
                        // -- odometer
                        double odom = 0.0;
                        if (fd.hasValue(DATA_ODOMETER)) {
                            odom = fd.getDouble(DATA_ODOMETER); // kilometers
                        } else
                        if (fd.hasValue(DATA_DISTANCE)) {
                            odom = fd.getDouble(DATA_DISTANCE); // kilometers
                        } else
                        if (dev != null) {
                            odom = dev.getLastOdometerKM();
                        }
                        remaining = ((next > 0.0) && (odom > 0.0))? (next - odom) : 0.0;
                    }
                    if (remaining != 0.0) { // may be < 0.0
                        double distU = Account.getDistanceUnits(rd.getAccount()).convertFromKM(remaining);
                        String distS = formatDouble(distU, arg, "#0");
                        if (remaining >= 0.0) {
                            return fd.filterReturnedValue(DATA_SERVICE_REMAINING_KM0,distS);
                        } else {
                            return (new ColumnValue(distS)).setForegroundColor(ColorTools.RED);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintRemaining","Remaining") + "\n${distanceUnits}";
                }
            });

            // -- Last Service Odometer km #1
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_LAST_KM1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    double  dist = 0.0;
                    int    kmNdx = 1;
                    if (fd.hasValue(DATA_SERVICE_LAST_KM1)) {
                        dist = fd.getDouble(DATA_SERVICE_LAST_KM1); // kilometers
                    } else
                    if (dev != null) {
                        dist = dev.getMaintOdometerKM(kmNdx); // kilometers
                    } else {
                        // -- not available
                    }
                    if (dist > 0.0) {
                        if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                            dist += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                        } else
                        if (dev != null) {
                            dist += dev.getOdometerOffsetKM(); // ok
                        }
                        dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        return fd.filterReturnedValue(DATA_SERVICE_LAST_KM1,formatDouble(dist,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintLastService1","Last Service #1") + "\n${distanceUnits}";
                }
            });
            // -- Service Interval km #1
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_INTERVAL_KM1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    double  dist = 0.0;
                    int    kmNdx = 1;
                    if (fd.hasValue(DATA_SERVICE_INTERVAL_KM1)) {
                        dist = fd.getDouble(DATA_SERVICE_INTERVAL_KM1); // kilometers
                    } else {
                        Device dev = fd.getDevice();
                        if (dev != null) {
                            dist = dev.getMaintIntervalKM(kmNdx); // kilometers
                        }
                    }
                    if (dist > 0.0) {
                        dist = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        return fd.filterReturnedValue(DATA_SERVICE_INTERVAL_KM1,formatDouble(dist,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintInterval1","Service #1\nInterval");
                }
            });
            // -- Next Service Odometer km #1
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_NEXT_KM1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    double  dist = 0.0;
                    int    kmNdx = 1;
                    if (fd.hasValue(DATA_SERVICE_NEXT_KM1)) {
                        dist = fd.getDouble(DATA_SERVICE_NEXT_KM1); // kilometers
                    } else
                    if (dev != null) {
                        dist = dev.getMaintOdometerKM(kmNdx) + dev.getMaintIntervalKM(kmNdx); // km
                    }
                    if (dist > 0.0) {
                        // -- a "Next Service Interval" has been specified
                        if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                            dist += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                        } else
                        if (dev != null) {
                            dist += dev.getOdometerOffsetKM(); // ok
                        }
                        double distU = Account.getDistanceUnits(rd.getAccount()).convertFromKM(dist);
                        String distS = formatDouble(distU, arg, "#0");
                        // -- get last odometer value
                        double odom  = dev.getLastOdometerKM(); // DATA_ODOMETER?
                        if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                            odom += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                        } else
                        if (dev != null) {
                            odom += dev.getOdometerOffsetKM(); // ok
                        }
                        // -- check for elapsed interval
                        if ((odom > 0.0) && (odom >= dist)) {
                            // -- beyond service time
                            return (new ColumnValue(distS)).setForegroundColor(ColorTools.RED);
                        } else {
                            // -- have not reached service time yet
                            return fd.filterReturnedValue(DATA_SERVICE_NEXT_KM1,distS);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintNextService1","Next Service #1") + "\n${distanceUnits}";
                }
            });
            // -- Remaining Km until next Service (next - odometer) #1
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_REMAINING_KM1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    int    kmNdx = 1;
                    double remaining = 0.0;
                    if (fd.hasValue(DATA_SERVICE_REMAINING_KM1)) {
                        remaining = fd.getDouble(DATA_SERVICE_REMAINING_KM1); // kilometers
                    } else {
                        // -- next
                        double next = 0.0;
                        if (fd.hasValue(DATA_SERVICE_NEXT_KM1)) {
                            next = fd.getDouble(DATA_SERVICE_NEXT_KM1); // kilometers
                        } else
                        if (dev != null) {
                            next = dev.getMaintOdometerKM(kmNdx) + dev.getMaintIntervalKM(kmNdx); // km
                        }
                        // -- odometer
                        double odom = 0.0;
                        if (fd.hasValue(DATA_ODOMETER)) {
                            odom = fd.getDouble(DATA_ODOMETER); // kilometers
                        } else
                        if (fd.hasValue(DATA_DISTANCE)) {
                            odom = fd.getDouble(DATA_DISTANCE); // kilometers
                        } else
                        if (dev != null) {
                            odom = dev.getLastOdometerKM();
                        }
                        remaining = ((next > 0.0) && (odom > 0.0))? (next - odom) : 0.0;
                    }
                    if (remaining != 0.0) { // may be < 0.0
                        double distU = Account.getDistanceUnits(rd.getAccount()).convertFromKM(remaining);
                        String distS = formatDouble(distU, arg, "#0");
                        if (remaining >= 0.0) {
                            return fd.filterReturnedValue(DATA_SERVICE_REMAINING_KM1,distS);
                        } else {
                            return (new ColumnValue(distS)).setForegroundColor(ColorTools.RED);
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintRemaining1","Remaining #1") + "\n${distanceUnits}";
                }
            });

            // -- Service Notes
            this.addColumnTemplate(new DataColumnTemplate(DATA_SERVICE_NOTES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    String notes = (dev != null)? dev.getMaintNotes() : "";
                    return fd.filterReturnedValue(DATA_SERVICE_NOTES,notes);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.maintServiceNotes","Service Notes");
                }
            });

            // -- Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_ADDRESS,fd.getString(DATA_ADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    RequestProperties reqState = rd.getRequestProperties();
                    String addrTitles[] = (reqState != null)? reqState.getAddressTitles() : null;
                    String addrTitle    = (ListTools.size(addrTitles) > 0)? addrTitles[0] : null;
                    if (!StringTools.isBlank(addrTitle)) {
                        return addrTitle;
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        return i18n.getString("FieldLayout.address","Address");
                    }
                }
            });

            // -- City
            this.addColumnTemplate(new DataColumnTemplate(DATA_CITY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_CITY,fd.getString(DATA_CITY));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.city","City");
                }
            });

            // -- State/Province
            this.addColumnTemplate(new DataColumnTemplate(DATA_STATE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_STATE,fd.getString(DATA_STATE));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stateProvince","State\nProvince");
                }
            });

            // -- Country
            this.addColumnTemplate(new DataColumnTemplate(DATA_COUNTRY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_COUNTRY,fd.getString(DATA_COUNTRY));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.country","Country");
                }
            });

            // -- Subdivision
            this.addColumnTemplate(new DataColumnTemplate(DATA_SUBDIVISION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_SUBDIVISION,fd.getString(DATA_SUBDIVISION));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.subdivision","Subdivision");
                }
            });

            // -- Geozone ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_GEOZONE_ID,fd.getString(DATA_GEOZONE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.geozoneID","Geozone-ID");
                }
            });

            // -- Geozone Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOZONE_DESC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_GEOZONE_DESC)) {
                        return fd.filterReturnedValue(DATA_GEOZONE_DESC,fd.getString(DATA_GEOZONE_DESC));
                    } else
                    if (fd.hasValue(DATA_GEOZONE_ID)) {
                        String geozoneID = fd.getString(DATA_GEOZONE_ID);
                        try {
                            Geozone gz[] = Geozone.getGeozone(rd.getAccount(), geozoneID);
                            return !ListTools.isEmpty(gz)? gz[0].getDescription() : "";
                        } catch (DBException dbe) {
                            // -- error
                        }
                    }
                    return rc.getBlankFiller();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.geozoneDescription","Geozone\nDescription");
                }
            });

            // -- GeoCorridor ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_GEOCORRIDOR_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String corrID = fd.getString(DATA_GEOCORRIDOR_ID);
                    if (StringTools.isBlank(corrID)) {
                        // -- lookup corridor by geozoneID
                        if (fd.hasValue(DATA_GEOZONE_ID)) {
                            String geozoneID = fd.getString(DATA_GEOZONE_ID);
                            try {
                                Geozone gz[] = Geozone.getGeozone(rd.getAccount(), geozoneID);
                                corrID = !ListTools.isEmpty(gz)? gz[0].getCorridorID() : "";
                            } catch (DBException dbe) {
                                // -- error
                            }
                        }
                    }
                    if (StringTools.isBlank(corrID)) {
                        // -- lookup corridor by lat/lon
                        double lat = 0.0, lon = 0.0;
                        if (fd.hasValue(DATA_GEOPOINT)) {
                            GeoPoint gp = fd.getGeoPoint(DATA_GEOPOINT);
                            lat = GeoPoint.getLatitude(gp,999.0);
                            lon = GeoPoint.getLongitude(gp,999.0);
                        } else
                        if (fd.hasValue(DATA_LATITUDE) && fd.hasValue(DATA_LONGITUDE)) {
                            lat = fd.getDouble(DATA_BEST_LATITUDE );
                            lon = fd.getDouble(DATA_BEST_LONGITUDE);
                        }
                        if (GeoPoint.isValid(lat,lon)) {
                            // -- TODO: Need to find a corridor that this point is in
                        }
                    }
                    // -- return corridor-id
                    if (!StringTools.isBlank(corrID)) {
                        return fd.filterReturnedValue(DATA_GEOCORRIDOR_ID,corrID);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.corridorID","Corridor-ID");
                }
            });

            // -- Notes
            this.addColumnTemplate(new DataColumnTemplate(DATA_NOTES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_NOTES,fd.getString(DATA_NOTES));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.notes","Notes");
                }
            });

            // -- Description
            this.addColumnTemplate(new DataColumnTemplate(DATA_DESCRIPTION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_DESCRIPTION,fd.getString(DATA_DESCRIPTION));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.description","Description");
                }
            });

            // -- %Utilization
            this.addColumnTemplate(new DataColumnTemplate(DATA_UTILIZATION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_UTILIZATION)) {
                        double util = fd.getDouble(DATA_UTILIZATION) * 100.0;
                        return fd.filterReturnedValue(DATA_UTILIZATION,formatDouble(util,arg,"#0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.utilization","%Util");
                }
            });

            // -- Count
            this.addColumnTemplate(new DataColumnTemplate(DATA_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_COUNT)) {
                        long count = fd.getLong(DATA_COUNT);
                        if (count >= 0L) {
                            return fd.filterReturnedValue(DATA_COUNT,String.valueOf(count));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.count","Count");
                }
            });

            // -- Start/Stop date/time 
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_START_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            TimeZone     tz = rd.getTimeZone();
                            DateTime     dt = new DateTime(ts);
                            String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                            String    dtFmt = dt.format(fmtStr, tz);
                            ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_START_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startDateTime","Start\nDate/Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_STOP_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            TimeZone     tz = rd.getTimeZone();
                            DateTime     dt = new DateTime(ts);
                            String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                            String    dtFmt = dt.format(fmtStr, tz);
                            ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_STOP_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopDateTime","Stop\nDate/Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STARTSTOP_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    FieldData    fd = (FieldData)dataObj;
                    ReportLayout rl = rd.getReportLayout();
                    TimeZone     tz = rd.getTimeZone();
                    String   fmtArg = rc.getArg();
                    String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                    long    tsStart = fd.getLong(DATA_START_TIMESTAMP,-1L);
                    long    tsStop  = fd.getLong(DATA_STOP_TIMESTAMP ,-1L);
                    StringBuffer sb = new StringBuffer();
                    if (tsStart > 0L) {
                        sb.append((new DateTime(tsStart)).format(fmtStr, tz));
                    }
                    if (tsStop > 0L) {
                        if (sb.length() > 0) { sb.append("\n"); }
                        sb.append((new DateTime(tsStop)).format(fmtStr, tz));
                    }
                    if (sb.length() > 0) {
                        long tsSort = (tsStart > 0L)? tsStart : tsStop;
                        ColumnValue cv = new ColumnValue(sb.toString()).setSortKey(tsSort);
                        return fd.filterReturnedValue(DATA_STARTSTOP_DATETIME, cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startStopDateTime","Start/Stop\nDate/Time");
                }
            });

            // -- Start/Stop timestamp
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_START_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_START_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startTimestamp","Start\nTimestamp");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_STOP_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_STOP_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopTimestamp","Stop\nTimestamp");
                }
            });

            // -- Start/Stop Latitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_GEOPOINT) || fd.hasValue(DATA_START_LATITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_START_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude() : fd.getDouble(DATA_START_LATITUDE);
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String valStr = GeoPoint.formatLatitude(lat, latlonFmt, locale);
                        return fd.filterReturnedValue(DATA_START_LATITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lat","Lat");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_LATITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_GEOPOINT) || fd.hasValue(DATA_STOP_LATITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_STOP_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude() : fd.getDouble(DATA_STOP_LATITUDE);
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String valStr = GeoPoint.formatLatitude(lat, latlonFmt, locale);
                        return fd.filterReturnedValue(DATA_STOP_LATITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lat","Lat");
                }
            });

            // -- Start/Stop Longitude
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_GEOPOINT) || fd.hasValue(DATA_START_LONGITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_START_GEOPOINT);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_START_LONGITUDE);
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String valStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                        return fd.filterReturnedValue(DATA_START_LONGITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lon","Lon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_LONGITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_GEOPOINT) || fd.hasValue(DATA_STOP_LONGITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_STOP_GEOPOINT);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_STOP_LONGITUDE);
                        String args[] = StringTools.split(arg,',');
                        String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                        String valStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                        return fd.filterReturnedValue(DATA_STOP_LONGITUDE,valStr);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lon","Lon");
                }
            });
            
            // -- Start/Stop GeoPoint
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_GEOPOINT) || fd.hasValue(DATA_START_LATITUDE) || fd.hasValue(DATA_START_LONGITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_START_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude()  : fd.getDouble(DATA_START_LATITUDE);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_START_LONGITUDE);
                        if (GeoPoint.isValid(lat,lon)) {
                            String args[] = StringTools.split(arg,',');
                            String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                            String latStr = GeoPoint.formatLatitude( lat, latlonFmt, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                            String valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            return fd.filterReturnedValue(DATA_START_GEOPOINT,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.latLon","Lat/Lon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg(); // "4,mapLink"
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_GEOPOINT) || fd.hasValue(DATA_STOP_LATITUDE) || fd.hasValue(DATA_STOP_LONGITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_STOP_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude()  : fd.getDouble(DATA_STOP_LATITUDE);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_STOP_LONGITUDE);
                        if (GeoPoint.isValid(lat,lon)) {
                            String args[] = StringTools.split(arg,',');
                            String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                            String latStr = GeoPoint.formatLatitude( lat, latlonFmt, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                            String valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            // -- map|mapLink
                            String argMap = (args.length > 1)? args[1] : (args.length > 0)? args[0] : "";
                            if ((fd instanceof EventDataProvider) && rd.hasMapURL() &&
                                (argMap.equalsIgnoreCase("mapLink") || argMap.equalsIgnoreCase("map"))) {
                                URIArg mapURL = rd.getMapURL(); // non-null, already tested by 'hasMapURL()'
                                mapURL.addArg("showpp",rowNdx+1);
                                mapURL.addArg("zoompp",rowNdx+1);
                                MapDimension sz = rd.getMapWindowSize();
                                int W = sz.getWidth();
                                int H = sz.getHeight();
                                String  encMapURL = WebPageAdaptor.EncodeURL(rd.getRequestProperties(),mapURL);
                                String  target    = null;
                                boolean button    = false;
                                ColumnValue cv = new ColumnValue();
                                cv.setValue(valStr);
                                cv.setLinkURL("javascript:openResizableWindow('"+encMapURL+"','ReportMap',"+W+","+H+");",target,button);
                                return fd.filterReturnedValue(DATA_STOP_GEOPOINT,cv);
                            }
                            return fd.filterReturnedValue(DATA_STOP_GEOPOINT,valStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.latLon","Lat/Lon");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STARTSTOP_GEOPOINT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    Locale locale = rd.getLocale();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    StringBuffer sb = new StringBuffer();
                    if (fd.hasValue(DATA_START_GEOPOINT) || fd.hasValue(DATA_START_LATITUDE) || fd.hasValue(DATA_START_LONGITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_START_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude()  : fd.getDouble(DATA_START_LATITUDE);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_START_LONGITUDE);
                        if (GeoPoint.isValid(lat,lon)) {
                            String args[] = StringTools.split(arg,',');
                            String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                            String latStr = GeoPoint.formatLatitude( lat, latlonFmt, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                            String valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            sb.append(valStr);
                        }
                    }
                    if (fd.hasValue(DATA_STOP_GEOPOINT) || fd.hasValue(DATA_STOP_LATITUDE) || fd.hasValue(DATA_STOP_LONGITUDE)) {
                        GeoPoint gp = fd.getGeoPoint(DATA_STOP_GEOPOINT);
                        double lat = (gp != null)? gp.getLatitude()  : fd.getDouble(DATA_STOP_LATITUDE);
                        double lon = (gp != null)? gp.getLongitude() : fd.getDouble(DATA_STOP_LONGITUDE);
                        if (GeoPoint.isValid(lat,lon)) {
                            String args[] = StringTools.split(arg,',');
                            String latlonFmt = GetLatLonFormat(args,rd.getAccount());
                            String latStr = GeoPoint.formatLatitude( lat, latlonFmt, locale);
                            String lonStr = GeoPoint.formatLongitude(lon, latlonFmt, locale);
                            String valStr = latStr + GeoPoint.PointSeparator + lonStr;
                            if (sb.length() > 0) { sb.append("\n"); }
                            sb.append(valStr);
                        }
                    }
                    if (sb.length() > 0) {
                        return fd.filterReturnedValue(DATA_STARTSTOP_GEOPOINT,sb.toString());
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startStopLatLon","Start/Stop\nLat/Lon");
                }
            });

            // -- Enter/Exit Geozone ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_ENTER_GEOZONE_ID,fd.getString(DATA_ENTER_GEOZONE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterGeozoneID","Arrive\nGeozone-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_EXIT_GEOZONE_ID,fd.getString(DATA_EXIT_GEOZONE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitGeozoneID","Departure\nGeozone-ID");
                }
            });

            // -- Start/Stiop Geozone ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_START_GEOZONE_ID,fd.getString(DATA_START_GEOZONE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startGeozoneID","Starting\nGeozone-ID");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_GEOZONE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_STOP_GEOZONE_ID,fd.getString(DATA_STOP_GEOZONE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopGeozoneID","Ending\nGeozone-ID");
                }
            });

            // -- Enter/Exit Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_ENTER_ADDRESS,fd.getString(DATA_ENTER_ADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterAddress","Arrive\nAddress");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_EXIT_ADDRESS,fd.getString(DATA_EXIT_ADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitAddress","Departure\nAddress");
                }
            });

            // -- Enter/Exit date/time
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ENTER_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_ENTER_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            TimeZone     tz = rd.getTimeZone();
                            DateTime     dt = new DateTime(ts);
                            String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                            String    dtFmt = dt.format(fmtStr, tz);
                            ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_ENTER_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterDateTime","Arrive\nDate/Time");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_EXIT_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_EXIT_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            ReportLayout rl = rd.getReportLayout();
                            TimeZone     tz = rd.getTimeZone();
                            DateTime     dt = new DateTime(ts);
                            String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                            String    dtFmt = dt.format(fmtStr, tz);
                            ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                            return fd.filterReturnedValue(DATA_EXIT_DATETIME, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitDateTime","Departure\nDate/Time");
                }
            });

            // -- Enter/Exit timestamp
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ENTER_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_ENTER_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_ENTER_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterTimestamp","Arrive\nTimestamp");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_TIMESTAMP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_EXIT_TIMESTAMP)) {
                        long ts = fd.getLong(DATA_EXIT_TIMESTAMP,-1L);
                        if (ts > 0L) {
                            return fd.filterReturnedValue(DATA_EXIT_TIMESTAMP,String.valueOf(ts));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitTimestamp","Departure\nTimestamp");
                }
            });

            // -- Enter/Exit fuel
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_FUEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ENTER_FUEL)) {
                        double fuel = fd.getDouble(DATA_ENTER_FUEL); // Liters
                        if (fuel > 0.0) {
                            //Device dev = fd.getDevice();
                            fuel = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(fuel);
                            return fd.filterReturnedValue(DATA_ENTER_FUEL,formatDouble(fuel, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterFuel","Enter\nFuel Total") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_FUEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_EXIT_FUEL)) {
                        double fuel = fd.getDouble(DATA_EXIT_FUEL); // Liters
                        if (fuel > 0.0) {
                            //Device dev = fd.getDevice();
                            fuel = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(fuel);
                            return fd.filterReturnedValue(DATA_EXIT_FUEL,formatDouble(fuel, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitFuel","Exit\nFuel Total") + "\n${volumeUnits}";
                }
            });

            // -- Enter/Exit Odometer
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ENTER_ODOMETER)) {
                        double odom = fd.getDouble(DATA_ENTER_ODOMETER); // kilometers
                        if (odom > 0.0) {
                            Device dev = fd.getDevice();
                            if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                                odom += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                            } else
                            if (dev != null) {
                                odom += dev.getOdometerOffsetKM();
                            }
                            odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(odom);
                            return fd.filterReturnedValue(DATA_ENTER_ODOMETER,formatDouble(odom, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterOdometer","Enter\nOdometer") + "\n${distanceUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_ODOMETER) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_EXIT_ODOMETER)) {
                        double odom = fd.getDouble(DATA_EXIT_ODOMETER); // kilometers
                        if (odom > 0.0) {
                            Device dev = fd.getDevice();
                            if (fd.hasValue(DATA_ODOMETER_OFFSET)) {
                                odom += fd.getDouble(DATA_ODOMETER_OFFSET); // kilometers
                            } else
                            if (dev != null) {
                                odom += dev.getOdometerOffsetKM();
                            }
                            odom = Account.getDistanceUnits(rd.getAccount()).convertFromKM(odom);
                            return fd.filterReturnedValue(DATA_EXIT_ODOMETER,formatDouble(odom, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitOdometer","Exit\nOdometer") + "\n${distanceUnits}";
                }
            });

            // -- Start/Stop Address
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_START_ADDRESS,fd.getString(DATA_START_ADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startAddress","Starting Address");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    String valStr = fd.getString(DATA_STOP_ADDRESS);
                    if ((fd instanceof EventDataProvider) && rd.hasMapURL() &&
                        (arg.equalsIgnoreCase("mapLink") || arg.equalsIgnoreCase("map"))) {
                        URIArg mapURL = rd.getMapURL(); // non-null, already tested by 'hasMapURL()'
                        mapURL.addArg("showpp",rowNdx+1);
                        mapURL.addArg("zoompp",rowNdx+1);
                        MapDimension sz = rd.getMapWindowSize();
                        int W = sz.getWidth();
                        int H = sz.getHeight();
                        String  encMapURL = WebPageAdaptor.EncodeURL(rd.getRequestProperties(),mapURL);
                        String  target    = null;
                        boolean button    = false;
                        ColumnValue cv = new ColumnValue();
                        cv.setValue(valStr);
                        cv.setLinkURL("javascript:openResizableWindow('"+encMapURL+"','ReportMap',"+W+","+H+");",target,button);
                        return fd.filterReturnedValue(DATA_STOP_ADDRESS,valStr);
                    }
                    return fd.filterReturnedValue(DATA_STOP_ADDRESS,valStr);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.address","Address");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STARTSTOP_ADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    String startAddr = fd.getString(DATA_START_ADDRESS);
                    String stopAddr  = fd.getString(DATA_STOP_ADDRESS);
                    StringBuffer sb = new StringBuffer();
                    if (!StringTools.isBlank(startAddr)) {
                        sb.append(startAddr);
                    }
                    if (!StringTools.isBlank(stopAddr)) {
                        if (sb.length() > 0) { sb.append("\n"); }
                        sb.append(startAddr);
                    }
                    return fd.filterReturnedValue(DATA_STARTSTOP_ADDRESS, sb.toString());
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startStopAddress","Start/Stop Address");
                }
            });

            // -- (Generic) Elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_ELAPSE_SEC) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ELAPSE_SEC)) {
                        long elapsedSec = fd.getLong(DATA_ELAPSE_SEC,-1L);
                        if (elapsedSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(elapsedSec,fmt)).setSortKey(elapsedSec);
                            return fd.filterReturnedValue(DATA_ELAPSE_SEC, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.elapsedTime","Elapsed\nTime");
                }
            });

            // -- Inside Elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_INSIDE_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_INSIDE_ELAPSED)) {
                        long elapsedSec = fd.getLong(DATA_INSIDE_ELAPSED,-1L);
                        if (elapsedSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(elapsedSec,fmt)).setSortKey(elapsedSec);
                            return fd.filterReturnedValue(DATA_INSIDE_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.insideElapsed","Inside\nElapsed");
                }
            });

            // -- Outside Elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_OUTSIDE_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_OUTSIDE_ELAPSED)) {
                        long elapsedSec = fd.getLong(DATA_OUTSIDE_ELAPSED,-1L);
                        if (elapsedSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(elapsedSec,fmt)).setSortKey(elapsedSec);
                            return fd.filterReturnedValue(DATA_OUTSIDE_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.outsideElapsed","Outside\nElapsed");
                }
            });

            // -- "Trip" elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_TRIP_ELAPSED)) {
                        long driveSec = fd.getLong(DATA_TRIP_ELAPSED,-1L);
                        if (driveSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_TRIP_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.tripElapsed","Trip\nElapsed");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_ELAPSED_WH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_TRIP_ELAPSED_WH)) {
                        long driveSec = fd.getLong(DATA_TRIP_ELAPSED_WH,-1L);
                        if (driveSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_TRIP_ELAPSED_WH, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.tripElapsedWorkHours","Work Hour\nElapsed");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_TRIP_ELAPSED_AH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_TRIP_ELAPSED_AH)) {
                        long driveSec = fd.getLong(DATA_TRIP_ELAPSED_AH,-1L);
                        if (driveSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_TRIP_ELAPSED_AH, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_TRIP_ELAPSED) && fd.hasValue(DATA_TRIP_ELAPSED_WH)) {
                        long driveSecTot = fd.getLong(DATA_TRIP_ELAPSED,-1L);
                        long driveSecWH  = fd.getLong(DATA_TRIP_ELAPSED_WH,-1L);
                        if ((driveSecWH >= 0L) && (driveSecTot >= driveSecWH)) {
                            long driveSec = driveSecTot - driveSecWH;
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_TRIP_ELAPSED_AH, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.tripElapsedAfterHours","After Hour\nElapsed");
                }
            });

            // -- Driving/Moving elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVING_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_DRIVING_ELAPSED)) {
                        long driveSec = fd.getLong(DATA_DRIVING_ELAPSED,-1L);
                        if (driveSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_DRIVING_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.drivingElapsed","Driving\nElapsed");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVING_ELAPSED_WH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_DRIVING_ELAPSED_WH)) {
                        long driveSec = fd.getLong(DATA_DRIVING_ELAPSED_WH,-1L);
                        if (driveSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_DRIVING_ELAPSED_WH, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.drivingElapsedWorkHours","Work Hour\nElapsed");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_DRIVING_ELAPSED_AH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_DRIVING_ELAPSED_AH)) {
                        long driveSec = fd.getLong(DATA_DRIVING_ELAPSED_AH,-1L);
                        if (driveSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_DRIVING_ELAPSED_AH, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_DRIVING_ELAPSED) && fd.hasValue(DATA_DRIVING_ELAPSED_WH)) {
                        long driveSecTot = fd.getLong(DATA_DRIVING_ELAPSED,-1L);
                        long driveSecWH  = fd.getLong(DATA_DRIVING_ELAPSED_WH,-1L);
                        if ((driveSecWH >= 0L) && (driveSecTot >= driveSecWH)) {
                            long driveSec = driveSecTot - driveSecWH;
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(driveSec,fmt)).setSortKey(driveSec);
                            return fd.filterReturnedValue(DATA_DRIVING_ELAPSED_AH, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.drivingElapsedAfterHours","After Hour\nElapsed");
                }
            });

            // -- Stopped elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_ELAPSED)) {
                        long stopSec = fd.getLong(DATA_STOP_ELAPSED,-1L);
                        if (stopSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(stopSec,fmt)).setSortKey(stopSec);
                            return fd.filterReturnedValue(DATA_STOP_ELAPSED, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stoppedElapsed","Stopped\nElapsed");
                }
            });

            // -- Stop count (number of stops)
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_COUNT)) {
                        double stopCount = fd.getDouble(DATA_STOP_COUNT,-1.0);
                        if (stopCount >= 0.0) {
                            if ((double)Math.round(stopCount) == stopCount) {
                                // -- Long value
                                return fd.filterReturnedValue(DATA_STOP_COUNT, String.valueOf((long)stopCount));
                            } else {
                                // -- Double value (1 decimal place)
                                return fd.filterReturnedValue(DATA_STOP_COUNT,formatDouble(stopCount,"1","#0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopCount","Num. of\nStops");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_COUNT_WH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_COUNT_WH)) {
                        long stopCount = fd.getLong(DATA_STOP_COUNT_WH,-1L);
                        if (stopCount >= 0L) {
                            return fd.filterReturnedValue(DATA_STOP_COUNT_WH, String.valueOf(stopCount));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopCountWH","Num. of\nStops\nWork Hours");
                }
            });

            // -- Idle elapsed time
            this.addColumnTemplate(new DataColumnTemplate(DATA_IDLE_ELAPSED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_IDLE_ELAPSED)) {
                        long idleSec = fd.getLong(DATA_IDLE_ELAPSED,-1L);
                        if (idleSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(idleSec,fmt)).setSortKey(idleSec);
                            if (idleSec >= DateTime.HourSeconds(2)) {
                                //cv.setForegroundColor(ColorTools.RED);
                            }
                            return fd.filterReturnedValue(DATA_IDLE_ELAPSED,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.idleElapsed","Idle\nElapsed");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_AVERAGE_IDLE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_AVERAGE_IDLE_HOURS)) {
                        long idleSec = fd.getLong(DATA_AVERAGE_IDLE_HOURS,-1L);
                        if (idleSec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(idleSec,fmt)).setSortKey(idleSec);
                            if (idleSec >= DateTime.HourSeconds(2)) {
                                //cv.setForegroundColor(ColorTools.RED);
                            }
                            return fd.filterReturnedValue(DATA_AVERAGE_IDLE_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.averageIdleHours","Average\nIdle Hours");
                }
            });

            // -- Attached
            this.addColumnTemplate(new DataColumnTemplate(DATA_ATTACHED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ATTACHED)) {
                        return fd.filterReturnedValue(DATA_ATTACHED,String.valueOf(fd.getBoolean(DATA_ATTACHED)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.attached","Attached");
                }
            });

            // -- Rule ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_RULE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_RULE_ID,fd.getString(DATA_RULE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ruleID","Rule-ID");
                }
            });

            // -- Message ID
            this.addColumnTemplate(new DataColumnTemplate(DATA_MESSAGE_ID) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_MESSAGE_ID,fd.getString(DATA_MESSAGE_ID));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.messageID","Message-ID");
                }
            });

            // -- IP address (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_IPADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_IPADDRESS,fd.getString(DATA_IPADDRESS));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ipAddress","IP Address");
                }
            });

            // -- Is Duplex (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ISDUPLEX) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ISDUPLEX)) {
                        return fd.filterReturnedValue(DATA_ISDUPLEX,String.valueOf(fd.getBoolean(DATA_ISDUPLEX)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.isDuplex","Is Duplex?");
                }
            });

            // -- TCP Connections (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_TCP_CONNECTIONS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_TCP_CONNECTIONS)) {
                        return fd.filterReturnedValue(DATA_TCP_CONNECTIONS,String.valueOf(fd.getLong(DATA_TCP_CONNECTIONS,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.tcpConnections","TCP\nConnects");
                }
            });

            // -- UDP Connections (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_UDP_CONNECTIONS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_UDP_CONNECTIONS)) {
                        return fd.filterReturnedValue(DATA_UDP_CONNECTIONS,String.valueOf(fd.getLong(DATA_UDP_CONNECTIONS,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.udpConnections","UDP\nConnects");
                }
            });

            // -- TCP/UDP Connections (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CONNECTIONS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_CONNECTIONS)) {
                        return fd.filterReturnedValue(DATA_CONNECTIONS,String.valueOf(fd.getLong(DATA_CONNECTIONS,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.connections","Connections");
                }
            });

            // -- Bytes Read (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_READ) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BYTES_READ)) {
                        return fd.filterReturnedValue(DATA_BYTES_READ,String.valueOf(fd.getLong(DATA_BYTES_READ,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesRead","Bytes\nRead");
                }
            });

            // -- Bytes Overhead (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_OVERHEAD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BYTES_OVERHEAD)) {
                        return fd.filterReturnedValue(DATA_BYTES_OVERHEAD,String.valueOf(fd.getLong(DATA_BYTES_OVERHEAD,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesOverhead","Bytes\nOverhead");
                }
            });

            // -- Bytes Written (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_WRITTEN) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BYTES_WRITTEN)) {
                        return fd.filterReturnedValue(DATA_BYTES_WRITTEN,String.valueOf(fd.getLong(DATA_BYTES_WRITTEN,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesWritten","Bytes\nWritten");
                }
            });

            // -- Bytes Total (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BYTES_TOTAL)) {
                        return fd.filterReturnedValue(DATA_BYTES_TOTAL,String.valueOf(fd.getLong(DATA_BYTES_TOTAL,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesTotal","Bytes\nTotal");
                }
            });

            // -- Bytes Rounded (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BYTES_ROUNDED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BYTES_ROUNDED)) {
                        return fd.filterReturnedValue(DATA_BYTES_ROUNDED,String.valueOf(fd.getLong(DATA_BYTES_ROUNDED,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.bytesRounded","Bytes\nRounded");
                }
            });

            // -- Events Received (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_EVENTS_RECEIVED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_EVENTS_RECEIVED)) {
                        return fd.filterReturnedValue(DATA_EVENTS_RECEIVED,String.valueOf(fd.getLong(DATA_EVENTS_RECEIVED,-1L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.eventsReceived","Events\nReceived");
                }
            });

            // -- Engine RPM (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_RPM) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ENGINE_RPM)) {
                        return fd.filterReturnedValue(DATA_ENGINE_RPM,String.valueOf(fd.getLong(DATA_ENGINE_RPM,0L)));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.engineRpm","Engine\nRPM");
                }
            });

            // -- Acceleration/MaximumAcceleration
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCELERATION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    double     A = fd.getDouble(DATA_ACCELERATION,Double.NaN);
                    if (!Double.isNaN(A)) {
                        if (arg.startsWith("G") || arg.startsWith("g")) {
                            A *= Accelerometer.G_PER_MPSS_FORCE; // G = MSS * G/MSS
                            arg = arg.substring(1).trim();
                            if (arg.startsWith(",")) {
                                arg = arg.substring(1).trim();
                            }
                        }
                        return fd.filterReturnedValue(DATA_ACCELERATION,formatDouble(A,arg,"#0.0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg = StringTools.trim(rc.getArg());
                    boolean G = false;
                    if (arg.startsWith("G") || arg.startsWith("g")) {
                        G = true;
                    }
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    Accelerometer.ForceUnits fUnits = Accelerometer.GetForceUnitsForName(G?"G":"MPSS"); // not null
                    return i18n.getString("FieldLayout.acceleration","Accel\n({0})",fUnits.toString(locale));
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCEL_MAGNITUDE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    double     A = fd.getDouble(DATA_ACCEL_MAGNITUDE,Double.NaN);
                    if (!Double.isNaN(A)) {
                        if (arg.startsWith("G") || arg.startsWith("g")) {
                            A *= Accelerometer.G_PER_MPSS_FORCE; // G = MSS * G/MSS
                            arg = arg.substring(1).trim();
                            if (arg.startsWith(",")) {
                                arg = arg.substring(1).trim();
                            }
                        }
                        return fd.filterReturnedValue(DATA_ACCEL_MAGNITUDE,formatDouble(A,arg,"#0.0"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg = StringTools.trim(rc.getArg());
                    boolean G = arg.equalsIgnoreCase("G");
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    Accelerometer.ForceUnits fUnits = Accelerometer.GetForceUnitsForName(G?"G":"MPSS"); // not null
                    return i18n.getString("FieldLayout.accelerometerMagnitude","Accelerometer\nMagnitude ({0})",fUnits.toString(locale));
                }
            });

            // -- Hours (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENGINE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    double hours = 0.0;
                    if (fd.hasValue(DATA_ENGINE_HOURS)) {
                        hours = fd.getDouble(DATA_ENGINE_HOURS,0.0);
                    } else
                    if (dev != null) {
                        hours = dev.getLastEngineHours();
                    }
                    if (hours > 0.0) {
                        if (dev != null) {
                            hours += dev.getEngineHoursOffset(); // ok
                        }
                        long sec = Math.round(hours * 3600.0);
                        if (sec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHHh);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(sec,fmt)).setSortKey(sec);
                            return fd.filterReturnedValue(DATA_ENGINE_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.engineHours","Engine\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_IDLE_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_IDLE_HOURS)) {
                        double hours = fd.getDouble(DATA_IDLE_HOURS,0.0);
                        long sec = Math.round(hours * 3600.0);
                        if (sec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHHh);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(sec,fmt)).setSortKey(sec);
                            return fd.filterReturnedValue(DATA_IDLE_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.idleHours","Idle\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_WORK_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_WORK_HOURS)) {
                        double hours = fd.getDouble(DATA_WORK_HOURS,0.0);
                        long sec = Math.round(hours * 3600.0);
                        if (sec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHHh);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(sec,fmt)).setSortKey(sec);
                            return fd.filterReturnedValue(DATA_WORK_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.workHours","Work\nHours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_PTO_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_PTO_HOURS)) {
                        double hours = fd.getDouble(DATA_PTO_HOURS,0.0);
                        long sec = Math.round(hours * 3600.0);
                        if (sec >= 0L) {
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHHh);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(sec,fmt)).setSortKey(sec);
                            return fd.filterReturnedValue(DATA_PTO_HOURS,cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ptoHours","PTO\nHours");
                }
            });

            // -- work shift (EXPERIMENTAL)
            this.addColumnTemplate(new DataColumnTemplate(DATA_WORK_SHIFT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg(); // "0000:2,0800:1,1800:2" / "0000:2,0100:3,0900:1,1700:2"
                    FieldData fd = (FieldData)dataObj;
                    // -- explicitly specified
                    if (fd.hasValue(DATA_WORK_SHIFT)) {
                        int shiftNum = fd.getInt(DATA_WORK_SHIFT,0);
                        return fd.filterReturnedValue(DATA_WORK_SHIFT,String.valueOf(shiftNum));
                    }
                    // -- get timestamp
                    long timestamp = 0L;
                    for (;/*single-pass loop*/;) {
                        // -- from DATA_TIMESTAMP
                        if (fd.hasValue(DATA_TIMESTAMP)) {
                            timestamp = fd.getLong(DATA_TIMESTAMP);
                            if (timestamp > 0L) {
                                break;
                            }
                        }
                        // -- from EventData record (if present)
                        String evKeys[] = {DATA_EVENTDATA,DATA_START_EVENT,DATA_ENTER_EVENT,DATA_STOP_EVENT,DATA_EXIT_EVENT};
                        Object edObj = fd.getValue(evKeys,null);
                        if (edObj instanceof EventData) {
                            EventData ed = (EventData)edObj;
                            timestamp = ed.getTimestamp();
                            if (timestamp > 0L) {
                                break;
                            }
                        }
                        // -- from any timestamp field we can find
                        String tsKeys[] = {DATA_START_TIMESTAMP,DATA_ENTER_TIMESTAMP,DATA_STOP_TIMESTAMP,DATA_EXIT_TIMESTAMP};
                        timestamp = StringTools.parseLong(fd.getValue(tsKeys,null),0L);
                        if (timestamp > 0L) {
                            break;
                        }
                        // -- not found
                        break;
                    } // single-pass loop
                    // -- calculate shift
                    if (timestamp > 0L) {
                        TimeZone tz = rd.getTimeZone();
                        DateTime dt = new DateTime(timestamp, tz);
                        int   evTOD = (dt.getHour24() * 60) + dt.getMinute();
                        // -- arg: "0000:2,0800:1,1800:2" / "0000:2,0100:3,0900:1,1700:2"
                        String shiftNum = null;
                        String shifts[] = StringTools.split(arg,',');
                        for (String S : shifts) {
                            int p = S.indexOf(':');
                            if (p < 0) { continue; }
                            int    shTOD = DateTime.MinuteOfDay(S.substring(0,p),tz);
                            String shftN = S.substring(p+1).trim();
                            if ((shTOD < 0) || (shftN == null)) { continue; }
                            if (evTOD < shTOD) {
                                break;
                            }
                            shiftNum = shftN; // my be updated on next pass
                        }
                        if (shiftNum != null) {
                            return fd.filterReturnedValue(DATA_WORK_SHIFT,shiftNum);
                        }
                    }
                    // --
                    return rc.getBlankFiller();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.workShift","Work\nShift");
                }
            });

            // -- Start/Stop Engine Hours
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_HOURS)) {
                        double hours = fd.getDouble(DATA_START_HOURS); // Liters
                        if (hours >= 0.0) {
                            long hrsSec = (long)(hours * 3600.0);
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(hrsSec,fmt)).setSortKey(hrsSec);
                            return fd.filterReturnedValue(DATA_START_HOURS, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startEngineHours","Start\nEng. Hours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_HOURS)) {
                        double hours = fd.getDouble(DATA_STOP_HOURS); // Liters
                        if (hours >= 0.0) {
                            long hrsSec = (long)(hours * 3600.0);
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(hrsSec,fmt)).setSortKey(hrsSec);
                            return fd.filterReturnedValue(DATA_STOP_HOURS, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopEngineHours","Stop\nEng. Hours");
                }
            });

            // -- Enter/Exit Engine Hours
            this.addColumnTemplate(new DataColumnTemplate(DATA_ENTER_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_ENTER_HOURS)) {
                        double hours = fd.getDouble(DATA_ENTER_HOURS); // Liters
                        if (hours >= 0.0) {
                            long hrsSec = (long)(hours * 3600.0);
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(hrsSec,fmt)).setSortKey(hrsSec);
                            return fd.filterReturnedValue(DATA_ENTER_HOURS, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.enterEngineHours","Enter\nEng. Hours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_EXIT_HOURS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_EXIT_HOURS)) {
                        double hours = fd.getDouble(DATA_EXIT_HOURS); // Liters
                        if (hours >= 0.0) {
                            long hrsSec = (long)(hours * 3600.0);
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(hrsSec,fmt)).setSortKey(hrsSec);
                            return fd.filterReturnedValue(DATA_EXIT_HOURS, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.exitEngineHours","Exit\nEng. Hours");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_HOURS_DELTA) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_HOURS_DELTA)) {
                        double deltaHours = fd.getDouble(DATA_HOURS_DELTA); // Liters
                        if (deltaHours >= 0.0) {
                            long hrsSec = (long)(deltaHours * 3600.0);
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(hrsSec,fmt)).setSortKey(hrsSec);
                            return fd.filterReturnedValue(DATA_HOURS_DELTA, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        // -- start hours
                        double startHours = -1.0;
                        if (fd.hasValue(DATA_START_HOURS)) { 
                            startHours = fd.getDouble(DATA_START_HOURS);
                        } else
                        if (fd.hasValue(DATA_ENTER_HOURS)) { 
                            startHours = fd.getDouble(DATA_ENTER_HOURS);
                        }
                        // -- stop hours
                        double stopHours  = -1.0;
                        if (fd.hasValue(DATA_STOP_HOURS)) { 
                            stopHours  = fd.getDouble(DATA_STOP_HOURS);
                        } else
                        if (fd.hasValue(DATA_EXIT_HOURS)) { 
                            stopHours  = fd.getDouble(DATA_EXIT_HOURS);
                        }
                        // -- calc delta hours
                        if ((startHours >= 0.0) && (stopHours >= 0.0) && (stopHours >= startHours)) {
                            double deltaHours = stopHours - startHours;
                            long hrsSec = (long)(deltaHours * 3600.0);
                            int fmt = FieldLayout.getElapsedFormat(arg, StringTools.ELAPSED_FORMAT_HHMMSS);
                            ColumnValue cv = new ColumnValue(FieldLayout.formatElapsedTime(hrsSec,fmt)).setSortKey(hrsSec);
                            return fd.filterReturnedValue(DATA_HOURS_DELTA, cv);
                        } else {
                            return rc.getBlankFiller();
                        }
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.elapsedEngineHours","Elapsed\nEng. Hours");
                }
            });

            // -- Fuel (field)
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_CAPACITY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_CAPACITY)) {
                        double literVol = fd.getDouble(DATA_FUEL_CAPACITY); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_CAPACITY,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_CAPACITY,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelCapacity","Fuel Capacity");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- add "Cost ($)"
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_LEVEL)) {
                        double level = fd.getDouble(DATA_FUEL_LEVEL); // percent
                        if (level < 0.0) {
                            I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                            return i18n.getString("FieldLayout.notAvailable","n/a");
                        } else
                        if (level <= 0.0) {
                            return rc.getBlankFiller(); // "0%"
                        } else
                        if (level <= 1.0) {
                            String p = Math.round(level*100.0) + "%";
                            return fd.filterReturnedValue(DATA_FUEL_LEVEL, p);  // percent
                        } else {
                            String p = "100%";
                            return fd.filterReturnedValue(DATA_FUEL_LEVEL, p);  // percent
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelLevel","Fuel Level") + "\n%";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TOTAL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_TOTAL)) {
                        double literVol = fd.getDouble(DATA_FUEL_TOTAL); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol); // Liters * $/Liter = $
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_TOTAL,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                                return fd.filterReturnedValue(DATA_FUEL_TOTAL,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelTotal","Total Fuel");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_REMAIN) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_REMAIN)) {
                        double literVol = fd.getDouble(DATA_FUEL_REMAIN); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_REMAIN,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_REMAIN,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelRemain","Remaining Fuel");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_REFILL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_REFILL)) {
                        double literVol = fd.getDouble(DATA_FUEL_REFILL); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_REFILL,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_REFILL,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelRefill","Fuel Refill");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TRIP) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_TRIP)) {
                        double literVol = fd.getDouble(DATA_FUEL_TRIP); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_TRIP,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_TRIP,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelTrip","Trip Fuel");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TRIP_WH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_TRIP_WH)) {
                        double literVol = fd.getDouble(DATA_FUEL_TRIP_WH); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_TRIP_WH,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_TRIP_WH,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelTripWorkHours","Trip Fuel\nWork Hours");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_TRIP_AH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_TRIP_AH)) {
                        double literVol = fd.getDouble(DATA_FUEL_TRIP_AH); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_TRIP_AH,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_TRIP_AH,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_FUEL_TRIP) && fd.hasValue(DATA_FUEL_TRIP_WH)) {
                        double ft   = fd.getDouble(DATA_FUEL_TRIP); // liters
                        double ftwh = fd.getDouble(DATA_FUEL_TRIP_WH); // liters
                        if ((ft >= 0.0) && (ft > ftwh)) {
                            double literVol = ft - ftwh; // liters
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_TRIP_AH,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_TRIP_AH,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelTripAfterHours","Trip Fuel\nAfter Hours");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_IDLE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_IDLE)) {
                        double literVol = fd.getDouble(DATA_FUEL_IDLE); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_IDLE,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_IDLE,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelIdle","Idle Fuel");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_IDLE_WH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_IDLE_WH)) {
                        double literVol = fd.getDouble(DATA_FUEL_IDLE_WH); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_IDLE_WH,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_IDLE_WH,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelIdleWorkHours","Idle Fuel\nWork Hours");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_IDLE_AH) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_IDLE_AH)) {
                        double literVol = fd.getDouble(DATA_FUEL_IDLE_AH); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_IDLE_AH,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_IDLE_AH,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_FUEL_IDLE) && fd.hasValue(DATA_FUEL_IDLE_WH)) {
                        double fi   = fd.getDouble(DATA_FUEL_IDLE); // liters
                        double fiwh = fd.getDouble(DATA_FUEL_IDLE_WH); // liters
                        if ((fi >= 0.0) && (fi > fiwh)) {
                            double literVol = fi - fiwh; // liters
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_IDLE_AH,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_IDLE_AH,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelIdleAfterHours","Idle Fuel\nAfter Hours");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_WORK) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_WORK)) {
                        double literVol = fd.getDouble(DATA_FUEL_WORK); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_WORK,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_WORK,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else
                    if (fd.hasValue(DATA_FUEL_TOTAL) && fd.hasValue(DATA_FUEL_IDLE)) {
                        double literVol = fd.getDouble(DATA_FUEL_TOTAL) - fd.getDouble(DATA_FUEL_IDLE); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_WORK,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_WORK,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelWork","Work Fuel");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_PTO) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_PTO)) {
                        double literVol = fd.getDouble(DATA_FUEL_PTO); // liters
                        if (literVol > 0.0) {
                            Account acct = rd.getAccount();
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_PTO,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                                return fd.filterReturnedValue(DATA_FUEL_PTO,formatDouble(vol, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelPTO","PTO Fuel");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_ECONOMY) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_ECONOMY)) {
                        double econ = fd.getDouble(DATA_FUEL_ECONOMY); // kilometers per liter
                        if (econ > 0.0) {
                            Account acct = rd.getAccount();
                            econ = Account.getEconomyUnits(acct).convertFromKPL(econ);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost (TODO: kilometers per $)
                                //double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                return rc.getBlankFiller();
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_ECONOMY,formatDouble(econ, arg, "#0.0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelEcon","Fuel Econ");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost: $ per mile (TODO: not yet supported)
                        Account acct = rd.getAccount();
                        return title + "\n(blank)";
                    } else {
                        return title + "\n${economyUnits}";
                    }
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_ECONOMY_TYPE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_ECONOMY_TYPE)) {
                        Object type = fd.getValue(DATA_FUEL_ECONOMY_TYPE); // Device.FuelEconomyType enum
                        if (type instanceof Device.FuelEconomyType) {
                            Locale locale = rd.getPrivateLabel().getLocale();
                            String typeStr = arg.equalsIgnoreCase("abbr")?
                                ((Device.FuelEconomyType)type).getAbbrev(locale) :
                                ((Device.FuelEconomyType)type).toString(locale);
                            return fd.filterReturnedValue(DATA_FUEL_ECONOMY_TYPE,typeStr);
                        } else
                        if (type != null) {
                            String typeStr = StringTools.trim(type); // convert to string
                            return fd.filterReturnedValue(DATA_FUEL_ECONOMY_TYPE,typeStr);
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.fuelEconType","Fuel Econ\nType");
                }
            });

            // -- Start/Stop Fuel
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_FUEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_FUEL)) {
                        double fuel = fd.getDouble(DATA_START_FUEL); // Liters
                        if (fuel > 0.0) {
                            fuel = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(fuel);
                            return fd.filterReturnedValue(DATA_START_FUEL,formatDouble(fuel, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startFuel","Start\nFuel Total") + "\n${volumeUnits}";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_FUEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_FUEL)) {
                        double fuel = fd.getDouble(DATA_STOP_FUEL); // Liters
                        if (fuel > 0.0) {
                            fuel = Account.getVolumeUnits(rd.getAccount()).convertFromLiters(fuel);
                            return fd.filterReturnedValue(DATA_STOP_FUEL,formatDouble(fuel, arg, "#0"));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopFuel","Stop\nFuel Total") + "\n${volumeUnits}";
                }
            });

            // -- Start/Stop FuelLevel
            this.addColumnTemplate(new DataColumnTemplate(DATA_START_FUEL_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_START_FUEL_LEVEL)) {
                        double level = fd.getDouble(DATA_START_FUEL_LEVEL); // percent
                        if (level < 0.0) {
                            I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                            return i18n.getString("FieldLayout.notAvailable","n/a");
                        } else
                        if (level <= 0.0) {
                            return rc.getBlankFiller(); // "0%"
                        } else
                        if (level <= 1.0) {
                            String p = Math.round(level*100.0) + "%";
                            return fd.filterReturnedValue(DATA_START_FUEL_LEVEL, p);  // percent
                        } else {
                            String p = "100%";
                            return fd.filterReturnedValue(DATA_START_FUEL_LEVEL, p);  // percent
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.startFuelLevel","Start\nFuel Level") + "\n%";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_STOP_FUEL_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_STOP_FUEL_LEVEL)) {
                        double level = fd.getDouble(DATA_STOP_FUEL_LEVEL); // percent
                        if (level < 0.0) {
                            I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                            return i18n.getString("FieldLayout.notAvailable","n/a");
                        } else
                        if (level <= 0.0) {
                            return rc.getBlankFiller(); // "0%"
                        } else
                        if (level <= 1.0) {
                            String p = Math.round(level*100.0) + "%";
                            return fd.filterReturnedValue(DATA_STOP_FUEL_LEVEL, p);  // percent
                        } else {
                            String p = "100%";
                            return fd.filterReturnedValue(DATA_STOP_FUEL_LEVEL, p);  // percent
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.stopFuelLevel","Stop\nFuel Level") + "\n%";
                }
            });

            // -- Fuel Delta (ie. "Gallons Consumed")
            this.addColumnTemplate(new DataColumnTemplate(DATA_FUEL_DELTA) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_FUEL_DELTA)) {
                        double literVol = fd.getDouble(DATA_FUEL_DELTA); // Liters
                        if (literVol >= 0.0) {
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_DELTA,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_DELTA,formatDouble(vol,arg,"#0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        // -- start fuel
                        double startFuel = -1.0; // Liters
                        if (fd.hasValue(DATA_START_FUEL)) { 
                            startFuel = fd.getDouble(DATA_START_FUEL); // Liters
                        } else
                        if (fd.hasValue(DATA_ENTER_FUEL)) { 
                            startFuel = fd.getDouble(DATA_ENTER_FUEL); // Liters
                        }
                        // -- stop fuel
                        double stopFuel  = -1.0; // Liters
                        if (fd.hasValue(DATA_STOP_FUEL)) { 
                            stopFuel  = fd.getDouble(DATA_STOP_FUEL); // Liters
                        } else
                        if (fd.hasValue(DATA_EXIT_FUEL)) { 
                            stopFuel  = fd.getDouble(DATA_EXIT_FUEL); // Liters
                        }
                        // -- calc delta fuel
                        if ((startFuel >= 0.0) && (stopFuel >= 0.0) && (stopFuel >= startFuel)) {
                            double literVol = stopFuel - startFuel; // Liters
                            Account acct = rd.getAccount();
                            double vol = Account.getVolumeUnits(acct).convertFromLiters(literVol);
                            if ((arg != null) && arg.startsWith("$")) {
                                // -- cost
                                double cost = Device.CalculateFuelCost(acct, fd.getDevice(), literVol);
                                if (cost > 0.0) {
                                    String fmt = arg.substring(1);
                                    return fd.filterReturnedValue(DATA_FUEL_DELTA,formatDouble(cost, fmt, "#0.00"));
                                } else {
                                    return rc.getBlankFiller();
                                }
                            } else {
                                // -- volume
                                return fd.filterReturnedValue(DATA_FUEL_DELTA,formatDouble(vol,arg,"#0"));
                            }
                        } else {
                            return rc.getBlankFiller();
                        }
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg   = rc.getArg(); // cost
                    I18N   i18n  = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String title = i18n.getString("FieldLayout.fuelUsed","Fuel Used");
                    if ((arg != null) && arg.startsWith("$")) {
                        // -- Cost ($)
                        String costTitle = i18n.getString("FieldLayout.cost","Cost");
                        return title + "\n" + costTitle + " (${currency})";
                    } else {
                        return title + "\n${volumeUnits}";
                    }
                }
            });

            // -- Delta Fuel Level
            this.addColumnTemplate(new DataColumnTemplate(DATA_DELTA_FUEL_LEVEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_DELTA_FUEL_LEVEL)) {
                        double deltaLevel = fd.getDouble(DATA_DELTA_FUEL_LEVEL); // delta percent
                        String p = Math.round(deltaLevel*100.0) + "%";
                        return fd.filterReturnedValue(DATA_DELTA_FUEL_LEVEL, p);  // percent
                    } else
                    if (fd.hasValue(DATA_START_FUEL_LEVEL) && fd.hasValue(DATA_STOP_FUEL_LEVEL)) {
                        double startLevel = fd.getDouble(DATA_START_FUEL_LEVEL); 
                        double stopLevel  = fd.getDouble(DATA_STOP_FUEL_LEVEL); 
                        double deltaLevel = stopLevel - startLevel;
                        String p = Math.round(deltaLevel*100.0) + "%";
                        return fd.filterReturnedValue(DATA_DELTA_FUEL_LEVEL, p);  // percent
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deltaFuelLevel","Delta\nFuel Level") + "\n%";
                }
            });

            // -- last connect/checkin date/time (Device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHECKIN_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    long   nowSec = DateTime.getCurrentTimeSec();
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    // -- last check-in time
                    long ts = 0L;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        ts = dev.getLastTotalConnectTime();
                        if (ts <= 0L) {
                            try {
                                EventData lastEv = dev.getLastEvent(-1L, false);
                                if (lastEv != null) {
                                    ts = lastEv.getTimestamp();
                                }
                            } catch (DBException dbe) {
                                // -- error retrieving event record
                            }
                        }
                    }
                    long ageSec = nowSec - ts; // may be <0 if event is in the future
                    if (ts > MIN_REASONABLE_TIMESTAMP) {
                        // -- last check-in time
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone     tz = rd.getTimeZone();
                        DateTime     dt = new DateTime(ts);
                        String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                        String    dtFmt = dt.format(fmtStr, tz);
                        ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                        ReportLayout.AgeColorRange acr = rd.getCheckinAgeColorRange(ageSec);
                        ReportLayout.SetColumnValueAgeColor(cv,acr);
                        return fd.filterReturnedValue(DATA_CHECKIN_DATETIME, cv);
                    } else
                    if (ts == 0L) {
                        // -- never?
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        String never = i18n.getString("FieldLayout.never","never");
                        ColumnValue cv = new ColumnValue(never).setSortKey(ts);
                        ReportLayout.AgeColorRange acr = rd.getCheckinAgeColorRange(ageSec);
                        ReportLayout.SetColumnValueAgeColor(cv,acr);
                        return fd.filterReturnedValue(DATA_CHECKIN_DATETIME, cv);
                    } else {
                        // -- unreasonable date?
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastCheckinTime","Last Check-In\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_CHECKIN_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    long nowSec = DateTime.getCurrentTimeSec();
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    // -- last check-in time
                    long ts = 0L;
                    Device dev = fd.getDevice();
                    if (dev != null) {
                        ts = dev.getLastTotalConnectTime();
                        if (ts <= 0L) {
                            try {
                                EventData lastEv = dev.getLastEvent(-1L, false);
                                if (lastEv != null) {
                                    ts = lastEv.getTimestamp();
                                }
                            } catch (DBException dbe) {
                                // -- error retrieving event record
                            }
                        }
                    }
                    long ageSec = nowSec - ts; // may be <0 if event is in the future
                    if (ts > nowSec) {
                        // -- event is in the future (relative to current time)
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        String future = i18n.getString("FieldLayout.future","Future");
                        ColumnValue cv = new ColumnValue(future).setSortKey(ageSec);
                        cv.setForegroundColor("orange");
                        return fd.filterReturnedValue(DATA_CHECKIN_AGE,cv);
                    } else
                    if (ts > MIN_REASONABLE_TIMESTAMP) {
                        // -- event is in the past
                        long days   = (ageSec / DateTime.DaySeconds(1));
                        long hours  = (ageSec % DateTime.DaySeconds(1)) / DateTime.HourSeconds(1);
                        long min    = (ageSec % DateTime.HourSeconds(1)) / DateTime.MinuteSeconds(1);
                        StringBuffer sb = new StringBuffer();
                        sb.append(days ).append("d ");
                        if (hours < 10) { sb.append("0"); }
                        sb.append(hours).append("h ");
                        if (min   < 10) { sb.append("0"); }
                        sb.append(min  ).append("m");
                        ColumnValue cv = new ColumnValue(sb.toString()).setSortKey(ageSec);
                        ReportLayout.AgeColorRange acr = rd.getCheckinAgeColorRange(ageSec);
                        ReportLayout.SetColumnValueAgeColor(cv,acr);
                        return fd.filterReturnedValue(DATA_CHECKIN_AGE,cv);
                    } else
                    if (ts == 0L) {
                        // -- never?
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        String never = i18n.getString("FieldLayout.never","never");
                        ColumnValue cv = new ColumnValue(never).setSortKey(ts);
                        ReportLayout.AgeColorRange acr = rd.getCheckinAgeColorRange(ageSec);
                        ReportLayout.SetColumnValueAgeColor(cv,acr);
                        return fd.filterReturnedValue(DATA_CHECKIN_AGE, cv);
                    } else {
                        // -- unreasonable date?
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastCheckinAge","Since Last\nCheck-In");
                }
            });

            // -- last IP address (DataTransport record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_LAST_IPADDRESS) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    DTIPAddress ipAddr = (dev != null)? dev.getDataTransport().getIpAddressCurrent() : null;
                    return (ipAddr != null)? fd.filterReturnedValue(DATA_LAST_IPADDRESS,ipAddr.toString()) : "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastIPAddress","Last IP\nAddress");
                }
            });

            // -- Code Version (DataTransport record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CODE_VERSION) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    String cv = (dev != null)? dev.getDataTransport().getCodeVersion() : null;
                    return (cv != null)? fd.filterReturnedValue(DATA_CODE_VERSION,cv) : "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.codeVersion","Code\nVersion");
                }
            });

            // -- custom field value (Device record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_CUSTOM_FIELD) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    String value = dev.getCustomAttribute(arg);
                    return !StringTools.isBlank(value)? value : "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    String arg = rc.getArg();
                    String desc = rd.getPrivateLabel().getStringProperty(BasicPrivateLabel.PROP_DeviceInfo_custom_ + arg, null);
                    if (!StringTools.isBlank(desc)) {
                        if (desc.length() > 12) {
                            int p = desc.lastIndexOf(" ");
                            if (p > 0) {
                                desc = desc.substring(0,p) + "\n" + desc.substring(p+1);
                            }
                        }
                        return desc;
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        return i18n.getString("FieldLayout.customAttribute","Custom\nAttribute");
                    }
                }
            });

            // -- Fault Codes
            this.addColumnTemplate(new DataColumnTemplate(DATA_FAULT_CODES) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device dev = fd.getDevice();
                    String faultStr = (dev != null)? dev.getLastFaultCode() : null;
                    if (StringTools.isBlank(faultStr)) {
                        return rc.getBlankFiller();
                    } else {
                        String fc = DTOBDFault.GetFaultString(new RTProperties(faultStr));
                        return fd.filterReturnedValue(DATA_FAULT_CODES,fc);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.faultCodes","OBD Faults");
                }
            });

            // -- Command state mask
            this.addColumnTemplate(new DataColumnTemplate(DATA_COMMAND_STATE_MASK) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String   arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Device   dev = fd.getDevice();
                    long     csm = (dev != null)? dev.getCommandStateMask() : 0L;
                    String     v = "";
                    if (StringTools.isBlank(arg) || arg.equalsIgnoreCase("dec")) {
                        v = String.valueOf(csm);
                    } else
                    if (arg.equalsIgnoreCase("hex")) {
                        v = "0x" + StringTools.toHexString(csm,16);
                    } else
                    if (arg.equalsIgnoreCase("bin")) {
                        v = StringTools.toBinaryString(csm,16,null).toString();
                    } else {
                        v = String.valueOf(csm);
                    }
                    return fd.filterReturnedValue(DATA_COMMAND_STATE_MASK,v);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.commandState","Command\nState");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_COMMAND_STATE_BIT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String  arg[] = StringTools.split(rc.getArg(),','); // "0,no,yes"
                    int    bitNdx = (arg.length > 0)? StringTools.parseInt(arg[0],0) : 0;
                    FieldData  fd = (FieldData)dataObj;
                    Device    dev = fd.getDevice();
                    boolean state = (dev != null)? dev.getCommandStateMaskBit(bitNdx) : false;
                    Locale locale = rd.getPrivateLabel().getLocale();
                    String      v = state?
                        AccountRecord.GetSimpleLocalString((arg.length>2)?arg[2]:"1",locale) :
                        AccountRecord.GetSimpleLocalString((arg.length>1)?arg[1]:"0",locale);
                    return fd.filterReturnedValue(DATA_COMMAND_STATE_BIT,v);
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String arg[] = StringTools.split(rc.getArg(),','); // "0,no,yes"
                    int bitNdx = (arg.length > 0)? StringTools.parseInt(arg[0],0) : 0;
                    String bitNdxS = String.valueOf(bitNdx);
                    return i18n.getString("FieldLayout.commandStateBit","Command\nState\n#{0}", bitNdxS);
                }
            });

            // -- last login date/time (Account record)
            this.addColumnTemplate(new DataColumnTemplate(DATA_LOGIN_DATETIME) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String fmtArg = rc.getArg();
                    FieldData  fd = (FieldData)dataObj;
                    long       ts = 0L;
                    if (fd.hasValue(DATA_LOGIN_DATETIME)) {
                        ts = fd.getLong(DATA_LOGIN_DATETIME);
                    } else {
                        Account acct = fd.getAccount();
                        ts = (acct != null)? acct.getLastLoginTime() : 0L;
                    }
                    if (ts > MIN_REASONABLE_TIMESTAMP) {
                        ReportLayout rl = rd.getReportLayout();
                        TimeZone     tz = rd.getTimeZone();
                        DateTime     dt = new DateTime(ts);
                        String   fmtStr = !StringTools.isBlank(fmtArg)? fmtArg : rl.getDateTimeFormat(rd.getPrivateLabel());
                        String    dtFmt = dt.format(fmtStr, tz);
                        ColumnValue  cv = new ColumnValue(dtFmt).setSortKey(ts);
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        ReportLayout.AgeColorRange acr = rd.getLoginAgeColorRange(ageSec);
                        ReportLayout.SetColumnValueAgeColor(cv,acr);
                        return fd.filterReturnedValue(DATA_LOGIN_DATETIME, cv);
                    } else {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        String never = i18n.getString("FieldLayout.loginNever","never");
                        ColumnValue cv = new ColumnValue(never).setSortKey(0);
                        cv.setForegroundColor(ColorTools.RED);
                        return fd.filterReturnedValue(DATA_LOGIN_DATETIME, cv);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastLoginTime","Last Login\nTime");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LOGIN_AGE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    long ts = 0L;
                    if (fd.hasValue(DATA_LOGIN_DATETIME)) {
                        ts = fd.getLong(DATA_LOGIN_DATETIME);
                    } else {
                        Account acct = fd.getAccount();
                        ts = (acct != null)? acct.getLastLoginTime() : 0L;
                    }
                    if (ts > MIN_REASONABLE_TIMESTAMP) {
                        long ageSec = DateTime.getCurrentTimeSec() - ts;
                        long days   = (ageSec / DateTime.DaySeconds(1));
                        long hours  = (ageSec % DateTime.DaySeconds(1)) / DateTime.HourSeconds(1);
                        long min    = (ageSec % DateTime.HourSeconds(1)) / DateTime.MinuteSeconds(1);
                        StringBuffer sb = new StringBuffer();
                        sb.append(days ).append("d ");
                        if (hours < 10) { sb.append("0"); }
                        sb.append(hours).append("h ");
                        if (min   < 10) { sb.append("0"); }
                        sb.append(min  ).append("m");
                        ColumnValue cv = new ColumnValue(sb.toString()).setSortKey(ageSec);
                        ReportLayout.AgeColorRange acr = rd.getLoginAgeColorRange(ageSec);
                        ReportLayout.SetColumnValueAgeColor(cv,acr);
                        return fd.filterReturnedValue(DATA_LOGIN_AGE,cv);
                    } else {
                        ColumnValue cv = new ColumnValue(rc.getBlankFiller()).setSortKey(999999999L);
                        cv.setForegroundColor(ColorTools.RED);
                        return fd.filterReturnedValue(DATA_LOGIN_AGE,cv);
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.lastLoginAge","Since Last\nLogin");
                }
            });

            // -- Account active
            this.addColumnTemplate(new DataColumnTemplate(DATA_ACCOUNT_ACTIVE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Account acct = fd.getAccount();
                    if (acct != null) {
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        boolean isActive = acct.isActive();
                        String value = isActive?
                            i18n.getString("FieldLayout.activeYes","Yes") :
                            i18n.getString("FieldLayout.activeNo" ,"No" );
                        return fd.filterReturnedValue(DATA_ACCOUNT_ACTIVE,value);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.accountActive","Account\nActive");
                }
            });

            // -- Account device Count
            this.addColumnTemplate(new DataColumnTemplate(DATA_DEVICE_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Account acct = fd.getAccount();
                    if (acct != null) {
                        long devCount = acct.getDeviceCount();
                        return fd.filterReturnedValue(DATA_DEVICE_COUNT,String.valueOf(devCount));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.deviceCount","Device\nCount");
                }
            });

            // -- Account SMS Message Count
            this.addColumnTemplate(new DataColumnTemplate(DATA_SMS_COUNT) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Account acct = fd.getAccount();
                    if (acct != null) {
                        ReportConstraints rptCon = rd.getReportConstraints();
                        long fromTime = rptCon.getTimeStart();
                        long toTime   = rptCon.getTimeEnd();
                        long smsCount = acct.getSmsMessagesSent(fromTime, toTime);
                        if (smsCount > 0) {
                            return fd.filterReturnedValue(DATA_SMS_COUNT,String.valueOf(smsCount));
                        } else {
                            return rc.getBlankFiller();
                        }
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.smsCount","SMS\nCount");
                }
            });

            // -- Account PrivateLabel name
            this.addColumnTemplate(new DataColumnTemplate(DATA_PRIVATE_LABEL) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    Account acct = fd.getAccount();
                    if (acct != null) {
                        String privLabel = acct.getPrivateLabelName();
                        return fd.filterReturnedValue(DATA_PRIVATE_LABEL,privLabel);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.privateLabelName","PrivateLabel\nName");
                }
            });

            // -- Rule selector
            this.addColumnTemplate(new DataColumnTemplate(DATA_RULE_SELECTOR) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    FieldData fd = (FieldData)dataObj;
                    // -- rule selector
                    String ruleSel = rc.getArg(); // TODO: this should be a reference to a property value
                    if (StringTools.isBlank(ruleSel)) {
                        // -- no rule selector to evaluate
                        return "";
                    }
                    // -- RuleFactory
                    RuleFactory ruleFact = Device.getRuleFactory();
                    if (ruleFact == null) {
                        // -- RuleFactory not available to evaluate selector
                        return "";
                    }
                    // -- get associated EventData record
                    String evKeys[] = {DATA_EVENTDATA,DATA_START_EVENT,DATA_ENTER_EVENT,DATA_STOP_EVENT,DATA_EXIT_EVENT};
                    Object edObj = fd.getValue(evKeys,null);
                    if (edObj instanceof EventData) {
                        try {
                            EventData ed = (EventData)edObj;
                            Object result = ruleFact.evaluateSelector(ruleSel,ed);
                            return StringTools.trim(result);
                        } catch (RuleParseException rpe) {
                            Print.logError("Invalid rule selector: " + ruleSel);
                            return "";
                        }
                    } else
                    if (edObj != null) {
                        Print.logError("Expecting 'EventData', found " + StringTools.className(edObj));
                    }
                    return "";
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ruleSelector","Rule\nSelector");
                }
            });

            // -- Billing cost/prorated
            this.addColumnTemplate(new DataColumnTemplate(DATA_BILLING_COST) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg   = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BILLING_COST)) {
                        double totCost = fd.getDouble(DATA_BILLING_COST); // already in currency units
                        String fmt = ((arg != null) && arg.startsWith("$"))? arg.substring(1) : StringTools.trim(arg);
                        return fd.filterReturnedValue(DATA_BILLING_COST,formatDouble(totCost, fmt, "#0.00"));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    Locale locale = rd.getLocale();
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.billingCost","Cost") + " (${currency})";
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BILLING_PRORATED) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BILLING_PRORATED)) {
                        boolean isProrated = fd.getBoolean(DATA_BILLING_PRORATED);
                        I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                        String value = isProrated?
                            i18n.getString("FieldLayout.proratedYes","Yes") :
                            i18n.getString("FieldLayout.proratedNo" ,"" );
                        return fd.filterReturnedValue(DATA_BILLING_PRORATED,value);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.billingProrated","Prorated");
                }
            });

            // -- Generic Ratio #1/#2
            this.addColumnTemplate(new DataColumnTemplate(DATA_RATIO_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_RATIO_1)) { // default percent
                        double val = fd.getDouble(DATA_RATIO_1);
                        String fmt = StringTools.isBlank(arg)? arg : "percent";
                        return fd.filterReturnedValue(DATA_RATIO_1,formatDouble(val,fmt,"#0.000"));

                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ratio1","Ratio #1");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_RATIO_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_RATIO_2)) { // default percent
                        double val = fd.getDouble(DATA_RATIO_2);
                        String fmt = StringTools.isBlank(arg)? arg : "percent";
                        return fd.filterReturnedValue(DATA_RATIO_2,formatDouble(val,fmt,"#0.000"));

                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.ratio2","Ratio #2");
                }
            });

            // -- Generic Boolean #1/#2
            this.addColumnTemplate(new DataColumnTemplate(DATA_BOOLEAN_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BOOLEAN_1)) {
                        boolean bool = fd.getBoolean(DATA_BOOLEAN_1,false);
                        String B[] = StringTools.split(arg,',');
                        String boolS = bool?
                            ((B.length > 0)? B[0] : GetLocalStringValue("yes",i18n)) : 
                            ((B.length > 1)? B[1] : GetLocalStringValue("no" ,i18n));
                        return fd.filterReturnedValue(DATA_BOOLEAN_1,boolS);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.boolean1","Boolean #1");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_BOOLEAN_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_BOOLEAN_2)) {
                        boolean bool = fd.getBoolean(DATA_BOOLEAN_2,false);
                        String B[] = StringTools.split(arg,',');
                        String boolS = bool?
                            ((B.length > 0)? B[0] : GetLocalStringValue("yes",i18n)) : 
                            ((B.length > 1)? B[1] : GetLocalStringValue("no" ,i18n));
                        return fd.filterReturnedValue(DATA_BOOLEAN_2,boolS);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.boolean2","Boolean #2");
                }
            });

            // -- Generic Count #1/#2
            this.addColumnTemplate(new DataColumnTemplate(DATA_COUNT_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_COUNT_1)) {
                        long count = fd.getLong(DATA_COUNT_1,0L);
                        return fd.filterReturnedValue(DATA_COUNT_1,String.valueOf(count));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.count1","Count #1");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_COUNT_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    if (fd.hasValue(DATA_COUNT_2)) {
                        long count = fd.getLong(DATA_COUNT_2,0L);
                        return fd.filterReturnedValue(DATA_COUNT_2,String.valueOf(count));
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.count2","Count #2");
                }
            });

            // -- Graph link (mapLink)
            this.addColumnTemplate(new DataColumnTemplate(DATA_GRAPH_LINK) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    String arg = StringTools.trim(rc.getArg());
                    FieldData fd = (FieldData)dataObj;
                    String urlStr = fd.getString(DATA_GRAPH_LINK,null);
                    if (!StringTools.isBlank(urlStr)) {
                        URIArg graphURL = new URIArg(urlStr,true);
                        MapDimension sz = rd.getGraphWindowSize();
                        int W = sz.getWidth();
                        int H = sz.getHeight();
                        String  encGraphURL = WebPageAdaptor.EncodeURL(rd.getRequestProperties(),graphURL);
                        String  target      = null;
                        boolean button      = true;
                        ColumnValue cv = new ColumnValue();
                        cv.setValue(i18n.getString("FieldLayout.graph","Graph"));
                        cv.setLinkURL("javascript:openResizableWindow('"+encGraphURL+"','GraphLink',"+W+","+H+");",target,button);
                        return fd.filterReturnedValue(DATA_GRAPH_LINK,cv);
                    } else {
                        return rc.getBlankFiller();
                    }
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.graph","Graph");
                }
            });

            // -- Left-align string #1/#2
            this.addColumnTemplate(new DataColumnTemplate(DATA_LEFT_ALIGN_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_LEFT_ALIGN_1,fd.getString(DATA_LEFT_ALIGN_1));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.leftAlign1","String 1");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_LEFT_ALIGN_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_LEFT_ALIGN_2,fd.getString(DATA_LEFT_ALIGN_2));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.leftAlign1","String 1");
                }
            });

            // -- Right-align string #1/#2
            this.addColumnTemplate(new DataColumnTemplate(DATA_RIGHT_ALIGN_1) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_RIGHT_ALIGN_1,fd.getString(DATA_RIGHT_ALIGN_1));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.rightAlign1","String 1");
                }
            });
            this.addColumnTemplate(new DataColumnTemplate(DATA_RIGHT_ALIGN_2) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    FieldData fd = (FieldData)dataObj;
                    return fd.filterReturnedValue(DATA_RIGHT_ALIGN_2,fd.getString(DATA_RIGHT_ALIGN_2));
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    I18N i18n = rd.getPrivateLabel().getI18N(FieldLayout.class);
                    return i18n.getString("FieldLayout.rightAlign2","String 2");
                }
            });

            // -- Blank space (this was included per a users request)
            this.addColumnTemplate(new DataColumnTemplate(DATA_BLANK_SPACE) {
                public Object getColumnValue(int rowNdx, ReportData rd, ReportColumn rc, Object dataObj) {
                    String arg = rc.getArg();
                    return rc.getBlankFiller();
                }
                public String getTitle(ReportData rd, ReportColumn rc) {
                    return "";
                }
            });

        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static int getElapsedFormat(char fmt, int dft)
    {
        switch (fmt) {
            case '-': return StringTools.ELAPSED_FORMAT_SS    ;
            case '0': return StringTools.ELAPSED_FORMAT_HHMMSS;
            case '1': return StringTools.ELAPSED_FORMAT_HHMM  ;
            case '2': return StringTools.ELAPSED_FORMAT_HHHhh ;
            case '3': return StringTools.ELAPSED_FORMAT_HHHh  ;
            case '4': return StringTools.ELAPSED_FORMAT_MMMSS ;
            default : return dft;
        }
    }

    protected static int getElapsedFormat(String arg, int dft)
    {
        /* default? */
        if (StringTools.isBlank(arg)) {
            return dft;
        } else 
        if (arg.equalsIgnoreCase("SS") || arg.equalsIgnoreCase("SECONDS")) {
            return StringTools.ELAPSED_FORMAT_SS;
        } else 
        if (arg.equalsIgnoreCase("HHMMSS")) {
            return StringTools.ELAPSED_FORMAT_HHMMSS;
        } else 
        if (arg.equalsIgnoreCase("HHMM")) {
            return StringTools.ELAPSED_FORMAT_HHMM;
        } else 
        if (arg.equalsIgnoreCase("HHHhh")) {
            return StringTools.ELAPSED_FORMAT_HHHhh;
        } else 
        if (arg.equalsIgnoreCase("HHHh")) {
            return StringTools.ELAPSED_FORMAT_HHHh;
        } else 
        if (arg.equalsIgnoreCase("MMMSS")) {
            return StringTools.ELAPSED_FORMAT_MMMSS;
        } else {
            return FieldLayout.getElapsedFormat(arg.charAt(0), dft);
        }
    }

    protected static String formatElapsedTime(long elapsedSec, int fmt)
    {
        return StringTools.formatElapsedSeconds(elapsedSec, fmt);
    }

}
