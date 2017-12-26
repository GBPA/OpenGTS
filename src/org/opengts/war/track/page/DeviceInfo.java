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
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/02/25  Martin D. Flynn
//     -Fixed possible exception when notification email has been disabled.
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/06/30  Martin D. Flynn
//     -Added Device table view
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/12/13  Martin D. Flynn
//     -Fixed NPE when no devices are yet defined for the account
//  2007/01/10  Martin D. Flynn
//     -Added edit fields for 'notes', 'simPhoneNumber'.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/10/16  Martin D. Flynn
//     -Added ability to create/delete devices.
//     -Added ability to edit unique-id and 'active' state.
//     -'setAllowNotify()' now properly set when rule selector has been specified.
//     -Update with new ACL usage
//  2008/12/01  Martin D. Flynn
//     -Added 'Map Pushpin ID' field to specify group map icon for a specific device.
//  2009/01/01  Martin D. Flynn
//     -Added Notification Description fields (if a valid RuleFactory is in place).
//  2009/05/01  Martin D. Flynn
//     -Added "Ignition Input Line" field.
//  2009/08/23  Martin D. Flynn
//     -Convert new entered IDs to lowercase
//  2009/11/10  Martin D. Flynn
//     -Added PushpinChooser support
//  2010/01/29  Martin D. Flynn
//     -Added 'codeVersion' field displayed as "Firmware Version".
//  2010/04/11  Martin D. Flynn
//     -Added Link fields (linkURL/linkDescription)
//     -"Properties" button will not display if no listed device supports the DeviceCmdHandler
//  2010/07/18  Martin D. Flynn
//     -Allow editing Server-id ("deviceCode") if device has not yet connected to server.
//     -Added "Fuel Capacity" field.
//  2010/09/09  Martin D. Flynn
//     -Added "vehicleID" field
//  2011/01/28  Martin D. Flynn
//     -Added "Creation Date" display (read-only) field
//     -Added "Driver ID"
//  2011/10/03  Martin D. Flynn
//     -Added PROP_DeviceInfo_maxIgnitionIndex
//     -Added edit "Serial Number" [device.getSerialNumber()]
//  2011/12/06  Martin D. Flynn
//     -Added ACL support for fields: IMEI, SIM, SMS, Serial#, DataKey
//     -Hide Rule Selector if "Device.CheckNotifySelector()" is false.
//  2012/02/03  Martin D. Flynn
//     -Added ACL support for fields: Active
//     -Added support for engine hours offset
//  2012/04/03  Martin D. Flynn
//     -Hide UniqueID,SIM#,Active in device list based on ACLs
//     -Extract a single address from the SMS Email address
//     -Added support for displaying/resetting fault codes
//  2012/06/29  Martin D. Flynn
//     -Added "Preferred Group" field.
//     -Added Reminder Message field.
//  2012/08/01  Martin D. Flynn
//     -Added "Assigned User ID"
//  2013/04/08  Martin D. Flynn
//     -Added "PROP_DeviceInfo_showCommandState_" property support.
//      (Configured command state bits are set/cleared when specific command
//      are selected and sent to the device).
//     -Added PARM_HOURS_OF_OPERATION, controlled by PROP_DeviceInfo_showHoursOfOperation
//  2013/05/19  Martin D. Flynn
//     -Added Calendar display for dates (ie. last/next service time)
//  2013/08/06  Martin D. Flynn
//     -Added PROP_DeviceInfo_uniqueSimPhoneNumber
//     -Added ACL support for "fuelCapacity", "fuelEconomy"
//  2013/11/11  Martin D. Flynn
//     -Added "manager" option to "deviceInfo.allowNewDevice" property
//     -Allow device "delete" if over limit
//  2014/05/05  Martin D. Flynn
//     -Added a fuel cost field.
//  2014/06/29  Martin D. Flynn
//     -Added support for FuelLevelProfile
//  2014/12/22  Martin D. Flynn
//     -Added pull-down menu list for "DCS Property ID"
//  2015/05/05  Martin D. Flynn
//     -Added support for PROP_DeviceInfo_showFuelLevelProfile
//     -Added support for "fixedAddress" (see PARM_FIXED_ADDRESS)
//     -Added support for "fixedContactPhone" (see PARM_FIXED_PHONE)
//  2016/01/04  Martin D. Flynn
//     -Fixed race-condition when initializing Device Command Handlers [2.6.1-B21]
//     -Added support for Fuel Tank #2 Profile [2.6.1-B45] (see fuelProf2)
//  2016/04/06  Martin D. Flynn
//     -Check actual selected device prior to delete (see "actualDev") [2.6.2-B15]
//  2016/12/21  Martin D. Flynn
//     -Added support for device expiration time (see DEVICE_EXPIRE, PARM_DEV_EXPIRE) [2.6.4-B12]
//  2017/06/01  Martin D. Flynn
//     -Fixed parsing of expiration dates [2.6.5-B19]
//     -Increased size of "dcsConfigString" field [2.6.5-B27]
//     -Check for blank SIM phone# before checking for uniqueness [2.6.5-B29]
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.Locale;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Collection;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;
import org.opengts.war.maps.JSMap;

import org.opengts.war.track.page.devcmd.*;

public class DeviceInfo
    extends WebPageAdaptor
    implements Constants
{


    // ------------------------------------------------------------------------

    private static final boolean EDIT_SERVER_ID             = false;
    private static final boolean EDIT_CODE_VERSION          = false;
    private static final boolean SHOW_LAST_CONNECT          = false;
    private static final boolean SHOW_NOTES                 = false;
    private static final String  SHOW_DATA_KEY              = "false";
    private static final boolean SHOW_PUSHPIN_ID            = true;
    private static final boolean SHOW_IGNITION_NDX          = true;
    private static final boolean SHOW_DISPLAY_COLOR         = true;
    private static final boolean SHOW_SPEED_LIMIT           = false;
    private static final boolean SHOW_REPORTED_ODOM         = true;
    private static final boolean SHOW_REPORTED_ENG_HRS      = true;
    private static final boolean SHOW_MAINTENANCE_ODOM      = true;  // still requires maintenance support
    private static final boolean SHOW_MAINTENANCE_HOURS     = true;  // still requires maintenance support
    private static final boolean SHOW_MAINTENANCE_NOTES     = true;  // still requires maintenance support
    private static final boolean SHOW_REMINDER_MESSAGE      = false; // still requires maintenance support
    private static final boolean SHOW_SERVICE_TIME          = false; // still requires maintenance support
    private static final boolean SHOW_SERVICE_ID            = true;
    private static final boolean SHOW_EXPIRATION_TIME       = true; // false; // device expiration
    private static final boolean SHOW_FIXED_LOCATION        = false;
    private static final boolean SHOW_NOTIFY_SELECTOR       = false; // only if RuleFactory defined
    private static final boolean SHOW_DCS_PROPERTIES_ID     = false;
    private static final boolean SHOW_DCS_CONFIG_STRING     = false;
    private static final boolean SHOW_FIXED_TCP_SESSION_ID  = false;
    private static final boolean SHOW_PREFERRED_GROUP       = false;
    private static final boolean ADD_TO_PREFERRED_GROUP     = true;
    private static final boolean SHOW_ASSIGNED_USER         = false;
    private static final boolean SHOW_HOURS_OF_OPERATION    = false;
    private static final boolean SHOW_FAULT_CODES           = true;
    private static final boolean SHOW_FUEL_CAPACITY         = true;
    private static final boolean OPTIMIZE_IGNITION_STATE    = false;
    private static final boolean ADD_DEV_TO_USER_AUTH_GROUP = true;
    private static final boolean SHOW_PUSHPIN_CHOOSER       = false;
    private static final boolean SHOW_DATE_CALENDAR         = false;
    private static final boolean SHOW_MAP_SHARE_DATES       = false;

    // ------------------------------------------------------------------------

    public  static final String _ACL_SERVERID               = "serverID";
    public  static final String _ACL_FIRMWARE               = "firmware";
    public  static final String _ACL_RULES                  = "rules";
    public  static final String _ACL_UNIQUEID               = "uniqueID";
    public  static final String _ACL_ACTIVE                 = "active";
    public  static final String _ACL_EXPIRE                 = "expire";
    public  static final String _ACL_SHARE                  = "share";
    public  static final String _ACL_COMMANDS               = "commands";
    public  static final String _ACL_SMS                    = "sms";
    public  static final String _ACL_EDIT_SMS               = "editSMS";
    public  static final String _ACL_EDIT_SIM               = "editSIM";
    public  static final String _ACL_EDIT_EQSTAT            = "editEquipStat";
    public  static final String _ACL_EDIT_IMEI              = "editIMEI";
    public  static final String _ACL_EDIT_SERIAL            = "editSerial";
    public  static final String _ACL_EDIT_DATKEY            = "editDatKey";
    public  static final String _ACL_FUEL_CAPACITY          = "fuelCapacity";
    public  static final String _ACL_FUEL_PROFILE           = "fuelProfile";
    public  static final String _ACL_FUEL_ECONOMY           = "fuelEconomy";
    private static final String _ACL_LIST[]                 = new String[] {
      //_ACL_SERVERID,      // [2.6.2-B19]
      //_ACL_FIRMWARE,      // [2.6.2-B19]
        _ACL_RULES,
        _ACL_UNIQUEID,
        _ACL_ACTIVE,
        _ACL_EXPIRE,        // [2.6.4-B12]
        _ACL_SHARE,         // [2.6.4-B12]
        _ACL_COMMANDS,
        _ACL_SMS,
        _ACL_EDIT_SMS,
        _ACL_EDIT_SIM,
        _ACL_EDIT_EQSTAT,
        _ACL_EDIT_IMEI,
        _ACL_EDIT_SERIAL,
        _ACL_EDIT_DATKEY,
        _ACL_FUEL_CAPACITY,
        _ACL_FUEL_PROFILE,
        _ACL_FUEL_ECONOMY
    };

    // ------------------------------------------------------------------------
    // allow new/delete device modes

    public  static final int    DEVMODE_DENY                = 0;
    public  static final int    DEVMODE_ALLOW               = 1;
    public  static final int    DEVMODE_SYSADMIN            = 2;
    public  static final int    DEVMODE_MANAGER             = 3;
    public  static final int    DEVMODE_MAXLIMIT            = 4;

    // ------------------------------------------------------------------------
    // Ignition type selection

    public  static final String IGNITION_notApplicable      = "n/a";
    public  static final String IGNITION_ignition           = "IgnitionOn/Off"; // "ign"
    public  static final String IGNITION_startStop          = "Start/Stop";     // "s/s"


    // ------------------------------------------------------------------------
    // Parameters

    // -- device expiration vars
    public  static final String DEVICE_EXPIRE               = "devExpCal";

    // -- device expiration vars
    public  static final String SHARE_START                 = "shareFrCal";
    public  static final String SHARE_END                   = "shareToCal";

    // -- license/registration expiration vars
    public  static final String LICENSE_EXPIRE              = "licExpCal";

    // -- last service time calendar vars
    public  static final String LAST_SERVICE_TIME           = "lastSrvDateCal";
    public  static final String NEXT_SERVICE_TIME           = "nextSrvDateCal";

    // -- forms
    public  static final String FORM_DEVICE_SELECT          = "DeviceInfoSelect";
    public  static final String FORM_DEVICE_EDIT            = "DeviceInfoEdit";
    public  static final String FORM_DEVICE_NEW             = "DeviceInfoNew";

    // -- commands
    public  static final String COMMAND_INFO_UPD_DEVICE     = "updateDev";
    public  static final String COMMAND_INFO_UPD_PROPS      = "updateProps"; // see DeviceCmdHandler
    public  static final String COMMAND_INFO_UPD_SMS        = "updateSms";   // see DeviceCmd_SMS
  //public  static final String COMMAND_INFO_REFRESH        = "refreshList";
    public  static final String COMMAND_INFO_SEL_DEVICE     = "selectDev";
    public  static final String COMMAND_INFO_NEW_DEVICE     = "newDev";

    // -- submit
    public  static final String PARM_SUBMIT_EDIT            = "d_subedit";
    public  static final String PARM_SUBMIT_VIEW            = "d_subview";
    public  static final String PARM_SUBMIT_CHG             = "d_subchg";
    public  static final String PARM_SUBMIT_DEL             = "d_subdel";
    public  static final String PARM_SUBMIT_NEW             = "d_subnew";
    public  static final String PARM_SUBMIT_QUE             = "d_subque";
    public  static final String PARM_SUBMIT_PROP            = "d_subprop";
    public  static final String PARM_SUBMIT_SMS             = "d_subsms";

    // -- buttons
    public  static final String PARM_BUTTON_CANCEL          = "d_btncan";
    public  static final String PARM_BUTTON_BACK            = "d_btnbak";

    // -- device table fields
    public  static final String PARM_NEW_NAME               = "d_newname";
    public  static final String PARM_CREATE_DATE            = "d_creation";
    public  static final String PARM_SERVER_ID              = "d_servid";
    public  static final String PARM_CODE_VERS              = "d_codevers";
    public  static final String PARM_DEV_UNIQ               = "d_uniq";
    public  static final String PARM_DEV_DESC               = "d_desc";
    public  static final String PARM_DEV_NAME               = "d_name";
    public  static final String PARM_VEHICLE_ID             = "d_vehicid";
    public  static final String PARM_VEHICLE_MAKE           = "d_vehMake";
    public  static final String PARM_VEHICLE_MODEL          = "d_vehModel";
    public  static final String PARM_LICENSE_PLATE          = "d_licPlate";
    public  static final String PARM_LICENSE_EXPIRE         = "d_licExp";
    public  static final String PARM_DEV_EQUIP_TYPE         = "d_equipt";
    public  static final String PARM_DEV_EQUIP_STATUS       = "d_equips";
    public  static final String PARM_DEV_FUEL_CAP1          = "d_fuelcap1";
    public  static final String PARM_DEV_FUEL_CAP2          = "d_fuelcap2";
    public  static final String PARM_DEV_FUEL_PROFILE1      = "d_fuelprof1";
    public  static final String PARM_DEV_FUEL_PROFILE1_SCALE= "d_fuelprof1Sca";
    public  static final String PARM_DEV_FUEL_PROFILE1_OFFS = "d_fuelprof1Ofs";
    public  static final String PARM_DEV_FUEL_PROFILE2      = "d_fuelprof2";
    public  static final String PARM_DEV_FUEL_PROFILE2_SCALE= "d_fuelprof2Sca";
    public  static final String PARM_DEV_FUEL_PROFILE2_OFFS = "d_fuelprof2Ofs";
    public  static final String PARM_DEV_FUEL_ECON          = "d_fuelecon";
    public  static final String PARM_DEV_FUEL_COST          = "d_fuelcost";
    public  static final String PARM_DEV_SPEED_LIMIT        = "d_speedLim";
    public  static final String PARM_DEV_IMEI               = "d_imei";
    public  static final String PARM_DEV_SERIAL_NO          = "d_sernum";
    public  static final String PARM_DATA_KEY               = "d_datakey";
    public  static final String PARM_DEV_SIMPHONE           = "d_simph";
    public  static final String PARM_SMS_EMAIL              = "d_smsemail";
    public  static final String PARM_ICON_ID                = "d_iconid";
    public  static final String PARM_DISPLAY_COLOR          = "d_dcolor";
    public  static final String PARM_DEV_ACTIVE             = "d_actv";
    public  static final String PARM_DEV_EXPIRE             = "d_devExp";
    public  static final String PARM_DEV_SERIAL             = "d_ser";
    public  static final String PARM_DEV_LAST_CONNECT       = "d_lconn";
    public  static final String PARM_DEV_LAST_EVENT         = "d_levnt";
    public  static final String PARM_DEV_NOTES              = "d_notes";
    public  static final String PARM_LINK_URL               = "d_linkurl";
    public  static final String PARM_LINK_DESC              = "d_linkdesc";
    public  static final String PARM_FIXED_LAT              = "d_fixlat";
    public  static final String PARM_FIXED_LON              = "d_fixlon";
    public  static final String PARM_FIXED_ADDRESS          = "d_fixAddr";
    public  static final String PARM_FIXED_PHONE            = "d_fixPhone";
    public  static final String PARM_IGNITION_INDEX         = "d_ignndx";
    public  static final String PARM_DCS_PROPS_ID           = "d_dcspropid";
    public  static final String PARM_DCS_CONFIG_STR         = "d_dcscfgstr";
    public  static final String PARM_TCP_SESSION_ID         = "d_tcpSessID";
    public  static final String PARM_DEV_ENABLE_ELOG        = "d_elog";
    public  static final String PARM_DRIVER_ID              = "d_driver";
    public  static final String PARM_USER_ID                = "d_user";
    public  static final String PARM_GROUP_ID               = "d_group";
    public  static final String PARM_DEV_GROUP_             = "d_grp_";

    public  static final String PARM_REPORT_ODOM            = "d_rptodom";
    public  static final String PARM_REPORT_HOURS           = "d_rpthours";
    public  static final String PARM_MAINT_INTERVKM_        = "d_mntintrkm";
    public  static final String PARM_MAINT_LASTKM_          = "d_mntlastkm";    // read-only
    public  static final String PARM_MAINT_NEXTKM_          = "d_mntnextkm";    // read-only
    public  static final String PARM_MAINT_RESETKM_         = "d_mntreskm";     // reset checkbox
    public  static final String PARM_MAINT_INTERVKM_0       = PARM_MAINT_INTERVKM_+"0";
    public  static final String PARM_MAINT_LASTKM_0         = PARM_MAINT_LASTKM_+"0";
    public  static final String PARM_MAINT_NEXTKM_0         = PARM_MAINT_NEXTKM_+"0";
    public  static final String PARM_MAINT_RESETKM_0        = PARM_MAINT_RESETKM_+"0";
    public  static final String PARM_MAINT_INTERVKM_1       = PARM_MAINT_INTERVKM_+"1";
    public  static final String PARM_MAINT_LASTKM_1         = PARM_MAINT_LASTKM_+"1";
    public  static final String PARM_MAINT_NEXTKM_1         = PARM_MAINT_NEXTKM_+"1";
    public  static final String PARM_MAINT_RESETKM_1        = PARM_MAINT_RESETKM_+"1";
    public  static final String PARM_MAINT_INTERVHR_        = "d_mntintrhr";
    public  static final String PARM_MAINT_LASTHR_          = "d_mntlasthr";    // read-only
    public  static final String PARM_MAINT_RESETHR_         = "d_mntreshr";     // reset checkbox
    public  static final String PARM_MAINT_INTERVHR_0       = PARM_MAINT_INTERVHR_+"0";
    public  static final String PARM_MAINT_LASTHR_0         = PARM_MAINT_LASTHR_+"0";
    public  static final String PARM_MAINT_RESETHR_0        = PARM_MAINT_RESETHR_+"0";
    public  static final String PARM_MAINT_NOTES            = "d_mntnotes";

    public  static final String PARM_REMIND_MESSAGE         = "d_remmsg";
    public  static final String PARM_REMIND_INTERVAL        = "d_remintrv";

    public  static final String PARM_LAST_SERVICE_TIME      = "d_lastserv";
    public  static final String PARM_NEXT_SERVICE_TIME      = "d_nextserv";

    public  static final String PARM_HOURS_OF_OPERATION     = "d_hrOfOp";

    public  static final String PARM_FAULT_CODES            = "d_faultc";       // read-only
    public  static final String PARM_FAULT_RESET            = "d_faultres";     // reset checkbox

    public  static final String PARM_DEV_RULE_ALLOW         = "d_ruleallw";     // "Notify Enable"
    public  static final String PARM_DEV_RULE_EMAIL         = "d_rulemail";
    public  static final String PARM_DEV_RULE_SEL           = "d_rulesel";
    public  static final String PARM_DEV_RULE_DESC          = "d_ruledesc";
    public  static final String PARM_DEV_RULE_SUBJ          = "d_rulesubj";
    public  static final String PARM_DEV_RULE_TEXT          = "d_ruletext";
  //public  static final String PARM_DEV_RULE_WRAP          = "d_rulewrap";
    public  static final String PARM_LAST_ALERT_TIME        = "d_alertTime";
    public  static final String PARM_LAST_ALERT_RESET       = "d_alertReset";

    public  static final String PARM_ACTIVE_CORRIDOR        = "d_actvcorr";

    public  static final String PARM_BORDER_CROSS_ENAB      = "d_bcEnable";
    public  static final String PARM_BORDER_CROSS_TIME      = "d_bcTime";

    public  static final String PARM_SHARE_START            = "d_shareFr";
    public  static final String PARM_SHARE_END              = "d_shareTo";

    public  static final String PARM_SUBSCRIBE_ID           = "d_subsID";
    public  static final String PARM_SUBSCRIBE_NAME         = "d_subsName";
    public  static final String PARM_SUBSCRIBE_AVATAR       = "d_subsAvat";

  //public  static final String PARM_WORKORDER_ID           = "d_workid";

    public  static final String PARM_DEV_CUSTOM_            = "d_c_";

    // ------------------------------------------------------------------------
    // Device command handlers

    private static Map<String,DeviceCmdHandler> _DCMap = new HashMap<String,DeviceCmdHandler>();
    private static DeviceCmd_SMS                SMSCommandHandler = null;
    private static volatile boolean             didInitDeviceCommandHandlers = false;

    static {
        // DeviceInfo._initDeviceCommandHandlers(); <== cannot be initialized here
    };

    private static Map<String,DeviceCmdHandler> _getDCMap()
    {
        DeviceInfo._initDeviceCommandHandlers();
        return _DCMap;
    }

    private static void _addDeviceCommandHandler(DeviceCmdHandler dch)
    {
        String servID = dch.getServerID();
        if (!_DCMap.containsKey(servID)) {
            //Print.logInfo("Installing DeviceCmdHandler: " + servID);
            DeviceInfo._DCMap.put(servID, dch);
        } else {
            DeviceCmdHandler dupDCH = DeviceInfo._DCMap.get(servID);
            Print.logError("Duplicate ServerID found: " + servID);
            Print.logError("  Current class : " + StringTools.className(dch));
            Print.logError("  Previous class: " + StringTools.className(dupDCH));
            Print.logStackTrace("Duplicate ServerID: " + servID);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<DeviceCmd_SMS> GetDeviceCmd_SMS_class()
    {
        try {
            return (Class<DeviceCmd_SMS>)Class.forName("org.opengts.war.track.page.devcmd.DeviceCmd_SMS");
        } catch (Throwable th) { // ClassNotFoundException
            Print.logWarn("Unable to find class DeviceCmd_SMS ...");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<DeviceCmdHandler> GetDeviceCmd_General_class()
    {
        try {
            return (Class<DeviceCmdHandler>)Class.forName("org.opengts.war.track.page.devcmd.DeviceCmd_General");
        } catch (Throwable th) { // ClassNotFoundException
            Print.logWarn("Unable to find class DeviceCmd_General ...");
            return null;
        }
    }

    private static void _initDeviceCommandHandlers()
    {
        String threadName = Thread.currentThread().getName();

        /* already initialized? */
        if (didInitDeviceCommandHandlers) {
            // -- already initiualized
            //Print.logInfo("**** Device Command Handlers already initialized ["+threadName+"] ****");
            return;
        }

        /* initialize (thread safe) */
        synchronized (DeviceInfo._DCMap) { // [2.6.1-B21]
            if (!didInitDeviceCommandHandlers) {
                //Print.logInfo("**** Initializing Device Command Handlers ["+threadName+"] ****");

                /* list of possible DeviceCmdHandler UI pages */
                /* The following list is no longer required
                String optDevCmdHandlerClasses[] = new String[] {
                    "DeviceCmd_SMS",    // always available
                    "DeviceCmd_gtsdmtp",    // always available
                    "DeviceCmd_template",   // always available
                    ...
                };
                */

                /* DeviceCmd_SMS.class */
                Class<DeviceCmd_SMS> DeviceCmd_SMS_class = GetDeviceCmd_SMS_class();
                if (DeviceCmd_SMS_class != null) {

                    /* add available Device command pages */
                    String DeviceCmd_ = "DeviceCmd_";
                    String devCmdPackage = DeviceCmd_SMS_class.getPackage().getName(); // always present
                    java.util.List<DCServerConfig> dcServerList = DCServerFactory.getServerConfigList(true/*inclAll*/);
                    for (DCServerConfig dcs : dcServerList) { // for (String _dchn : optDevCmdHandlerClasses)

                        /* existing "DeviceCmd_xxxx" command ui module */
                        String dcsName = dcs.getName();
                        String _dchn = DeviceCmd_ + dcsName;

                        /* separate any class:args */
                        // DeviceInfo.DeviceCmdAlternate.calamp=1,2,3
                        int p = _dchn.indexOf(":");
                        String dcn = (p >= 0)? _dchn.substring(0,p).trim() : _dchn;
                        String dca = (p >= 0)? _dchn.substring(p+1).trim() : null;
                        String dchArgs[] = RTConfig.getStringArray(DBConfig.PROP_DeviceInfo_DeviceCmdAlternate_+dcsName, null);
                        if (ListTools.isEmpty(dchArgs) && !StringTools.isBlank(dca)) {
                            dchArgs = StringTools.split(dca,',');
                        }

                        /* has commands */
                        if (ListTools.isEmpty(dcs.getCommandList())) {
                            // -- no defined commands
                            //Print.logInfo("DCS does not define any commands: " + dcsName);
                            continue;
                        }

                        /* get class */
                        String   dchClassName = devCmdPackage + "." + dcn;
                        Class<?> dchClass     = null;
                        try {
                            //Print.logInfo("Getting Class.forName: " + dchClassName);
                            dchClass = Class.forName(dchClassName);
                        } catch (Throwable th) { // ClassNotFoundException
                            // -- not found
                            //Print.logInfo("DeviceCmdHandler UI class not found: " + dchClassName + " (using General)");
                            dchClass = null;
                        }

                        /* add command handler UI */
                        if (dchClass != null) {
                            // -- found existing page, create instance
                            try {
                                DeviceCmdHandler stdDCH = (DeviceCmdHandler)dchClass.newInstance();
                                DeviceInfo._addDeviceCommandHandler(stdDCH);
                                //Print.logInfo("Added existing command handler UI: "+dcn+"("+dcsName+")");
                            } catch (Throwable th) { // ClassNotFoundException, ...
                                Print.logException("DeviceCmdHandler UI instantiation error: " + dchClassName, th);
                                continue;
                            }
                        } else {
                            // -- existing page not found, use DeviceCmd_General
                            DeviceCmdHandler stdDCH; // = new DeviceCmd_General(dcsName);
                            try {
                                String dcgClassN = "org.opengts.war.track.page.devcmd.DeviceCmd_General";
                                MethodAction dchMA = new MethodAction(dcgClassN, String.class);
                                stdDCH = (DeviceCmdHandler)dchMA.invoke(dcsName);
                            } catch (Throwable th) {
                                Print.logInfo("Unable to load DeviceCmd_General class: " + th.getMessage());
                                continue;
                            }
                            DeviceInfo._addDeviceCommandHandler(stdDCH);
                            //Print.logInfo("Added General command handler UI: DeviceCmd_General("+dcsName+")");
                        }

                        // -- check for additional DCS name args
                        if (!ListTools.isEmpty(dchArgs)) {
                            for (int a = 0; a < dchArgs.length; a++) {
                                String arg = StringTools.trim(dchArgs[a]);
                                if (!StringTools.isBlank(arg)) {
                                    if (dchClass != null) {
                                        try {
                                            DeviceCmdHandler argDCH = (DeviceCmdHandler)dchClass.newInstance();
                                            argDCH.setServerIDArg(arg);
                                            DeviceInfo._addDeviceCommandHandler(argDCH);
                                            Print.logInfo("Added custom command handler: "+dcn+"("+dcsName+") [" + arg + "]");
                                        } catch (Throwable th) { // ClassNotFoundException, ...
                                            Print.logException("DeviceCmdHandler UI instantiation error: " + dchClassName, th);
                                            break;
                                        }
                                    } else {
                                        DeviceCmdHandler argDCH; // = new DeviceCmd_General(dcsName);
                                        try {
                                            String dcgClassN = "org.opengts.war.track.page.devcmd.DeviceCmd_General";
                                            MethodAction dchMA = new MethodAction(dcgClassN, String.class);
                                            argDCH = (DeviceCmdHandler)dchMA.invoke(dcsName);
                                        } catch (Throwable th) {
                                            Print.logInfo("Unable to load DeviceCmd_General class: " + th.getMessage());
                                            continue;
                                        }
                                        argDCH.setServerIDArg(arg);
                                        DeviceInfo._addDeviceCommandHandler(argDCH);
                                        Print.logInfo("Added custom command handler: DeviceCmd_General("+dcsName+") [" + arg + "]");
                                    }
                                }
                            }
                        }

                    }

                    /* SMS */
                    try {
                        SMSCommandHandler = /*(DeviceCmd_SMS)*/DeviceCmd_SMS_class.newInstance(); // new DeviceCmd_SMS();
                        Print.logDebug("Created DeviceCmd_SMS command handler");
                    } catch (Throwable th) {
                        Print.logWarn("Unable to create DeviceCmd_SMS ...");
                    }

                }

                /* initialized */
                didInitDeviceCommandHandlers = true;

            }
        }

    }

    private DeviceCmdHandler getDeviceCommandHandler(String dcid)
    {

        /* must not be null/blank */
        if (StringTools.isBlank(dcid)) {
            return null;
        }

        /* get DCS module */
        DCServerConfig dcs = DCServerFactory.getServerConfig(dcid);
        if (dcs == null) {
            Print.logWarn("DCSServerConfig not found for DCS: " + dcid);
            return null;
        } else
        if (ListTools.isEmpty(dcs.getCommandList())) {
            //Print.logInfo("No commands defined for DCS: " + dcid);
            return null;
        }

        /* get command handler UI */
        DeviceCmdHandler dch = DeviceInfo._getDCMap().get(dcid); // may still be null
        if (dch == null) {
            //Print.logWarn("DCS does not have a command handler UI: " + dcid);
            return null;
        }

        /* return DeviceCmdHandler */
        return dch;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static class StateMaskBit // see "inpStateList"/"cmdStateList"
    {
        int     bitNdx      = 0;
        String  title       = "";
        String  falseDesc   = "";
        String  trueDesc    = "";
        public StateMaskBit(int ndx, String csvDesc, String dftTitle, Locale locale) {
            this.bitNdx    = ndx;
            String v[]     = StringTools.split(csvDesc,','); // Title,FalseDesc,TrueDesc
            this.title     = AccountRecord.GetSimpleLocalString((v.length > 0)? v[0] : dftTitle+ndx, locale);
            this.falseDesc = AccountRecord.GetSimpleLocalString((v.length > 1)? v[1] : "0"         , locale);
            this.trueDesc  = AccountRecord.GetSimpleLocalString((v.length > 2)? v[2] : "1"         , locale);
        }
        public StateMaskBit(int ndx, String title, String offDesc, String onDesc) {
            this.bitNdx    = ndx;
            this.title     = title;
            this.falseDesc = offDesc;
            this.trueDesc  = onDesc;
        }
        public int getBitIndex() {
            return this.bitNdx;
        }
        public String getTitle() {
            return this.title;
        }
        public String getFalseDesc() {
            return this.falseDesc;
        }
        public String getTrueDesc() {
            return this.trueDesc;
        }
        public String getStateDescription(boolean state) {
            return state? this.getTrueDesc() : this.getFalseDesc();
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getBitIndex());
            sb.append(",");
            sb.append(this.getTitle());
            sb.append(",");
            sb.append(this.getFalseDesc());
            sb.append(",");
            sb.append(this.getTrueDesc());
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean ShowNotifySelector()
    {
        return Device.CheckNotifySelector();
    }

    // ------------------------------------------------------------------------

    private static long GetEpochTime(String ymd, TimeZone tz, DateTime.DefaultParsedTime dftTime)
    {
        if (StringTools.isBlank(ymd)) {
            return 0L;
        } else {
            try {
                DateTime.ParsedDateTime pdt = DateTime.parseDateTime(ymd, tz, dftTime);
                return (pdt != null)? pdt.getEpochTime() : 0L;
            } catch (DateTime.DateParseException dpe) {
                return 0L;
            }
        }
    }

    private static long GetEpochTimeDayStart(DateTime.DateStringFormat dsf, String dat, TimeZone tz)
    {
        String ymd = (dsf != null)? dsf.convertToYMD(dat) : dat;
        return DeviceInfo.GetEpochTime(ymd, tz, DateTime.DefaultParsedTime.DayStart);
    }

    private static long GetEpochTimeDayEnd(DateTime.DateStringFormat dsf, String dat, TimeZone tz)
    {
        String ymd = (dsf != null)? dsf.convertToYMD(dat) : dat;
        return DeviceInfo.GetEpochTime(ymd, tz, DateTime.DefaultParsedTime.DayEnd);
    }

    // ------------------------------------------------------------------------

    private static long GetDayNumber(DateTime.DateStringFormat dsf, String dateStr)
    {
        if (StringTools.isBlank(dateStr)) {
            return 0L;
        } else {
            String ymd = (dsf != null)? dsf.convertToYMD(dateStr) : dateStr;
            DayNumber dn = DayNumber.parseDayNumber(ymd);
            return (dn != null)? dn.getDayNumber() : 0L;
        }
    }

    private static long GetDayNumberTS(DateTime.DateStringFormat dsf, String dateStr, TimeZone tmz, boolean dayStart)
    {
        if (StringTools.isBlank(dateStr)) {
            return 0L;
        } else {
            String ymd = (dsf != null)? dsf.convertToYMD(dateStr) : dateStr;
            DayNumber dn = DayNumber.parseDayNumber(ymd); //
            DateTime  dt = (dn != null)? (dayStart?dn.getDayStart(tmz):dn.getDayEnd(tmz)) : null;
            return (dt != null)? dt.getTimeSec() : 0L;
        }
    }

    // --------------------------------

    private static String FormatDayNumber(DateTime.DateStringFormat dsf, long dn)
    {
        if (dn < 0L) {
            return "";
        } else {
            String dateYMD = (new DayNumber(dn)).format(DayNumber.DATE_FORMAT_YMD_1);
            if (dsf != null) {
                return dsf.convertFromYMD(dateYMD);
            } else {
                return dateYMD;
            }
        }
    }

    private static String FormatDayNumberTS(DateTime.DateStringFormat dsf, long ts, TimeZone tmz)
    {
        if (ts <= 0L) {
            return FormatDayNumber(dsf, 0L);
        } else {
            long dn = new DateTime(ts,tmz).getDayNumber();
            return FormatDayNumber(dsf, dn);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // WebPage interface

    public DeviceInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_DEVICE_INFO);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceInfo.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getMenuDescription(reqState,i18n.getString("DeviceInfo.editMenuDesc","View/Edit {0} Information", devTitles));
    }

    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceInfo.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getMenuHelp(reqState,i18n.getString("DeviceInfo.editMenuHelp","View and Edit {0} information", devTitles));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceInfo.class);
        String devTitles[] = reqState.getDeviceTitles();
        return super._getNavigationDescription(reqState,i18n.getString("DeviceInfo.navDesc","{0}", devTitles));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(DeviceInfo.class);
        String devTitles[] = reqState.getDeviceTitles();
        return i18n.getString("DeviceInfo.navTab","{0} Admin", devTitles);
    }

    // ------------------------------------------------------------------------

    public String[] getChildAclList()
    {
        return _ACL_LIST;
    }

    // ------------------------------------------------------------------------

    private String _filterPhoneNum(String simPhone)
    {
        if (StringTools.isBlank(simPhone)) {
            return "";
        } else {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < simPhone.length(); i++) {
                char ch = simPhone.charAt(i);
                if (Character.isDigit(ch)) {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
    }

    private boolean _showNotificationFields(PrivateLabel privLabel)
    {
        String propShowNotify = PrivateLabel.PROP_DeviceInfo_showNotificationFields;
        String snf = (privLabel != null)? privLabel.getStringProperty(propShowNotify,null) : null;

        /* has notification fields? */
        if (!Device.supportsNotification()) {
            if ((snf != null) && snf.equalsIgnoreCase(StringTools.STRING_TRUE)) {
                Print.logWarn("Property "+propShowNotify+" is 'true', but Device notification fields are not supported");
            }
            return false;
        }

        /* show notification fields? */
        if (StringTools.isBlank(snf) || snf.equalsIgnoreCase("default")) {
            return Device.hasRuleFactory(); // default value
        } else {
            return StringTools.parseBoolean(snf,false);
        }

    }

    private boolean _showSubscriberInfo(PrivateLabel privLabel)
    {
        String propShowSubscr = PrivateLabel.PROP_DeviceInfo_showSubscriberInfo;
        boolean ssi = (privLabel != null)? privLabel.getBooleanProperty(propShowSubscr,false) : false;

        /* has subscriber fields? */
        if (!Device.supportsSubscriber()) {
            if (ssi) {
                Print.logWarn("Property "+propShowSubscr+" is 'true', but Device subscriber fields are not supported");
            }
            return false;
        }

        /* show subscriber info*/
        return ssi;

    }

    /*
    private boolean _showWorkOrderID(PrivateLabel privLabel)
    {
        String propShowWorkOrder = PrivateLabel.PROP_DeviceInfo_showWorkOrderField;
        String swo = (privLabel != null)? privLabel.getStringProperty(propShowWorkOrder,null) : null;

        //* has workOrderID field?
        if (!Device.getFactory().hasField(Device.FLD_workOrderID)) {
            if ((swo != null) && swo.equalsIgnoreCase(StringTools.STRING_TRUE)) {
                Print.logWarn("Property "+propShowWorkOrder+" is 'true', but Device workOrderID not supported");
            }
            return false;
        }

        //* show workOrder field?
        if (StringTools.isBlank(swo) || swo.equalsIgnoreCase("default")) {
            return true; // default value
        } else {
            return StringTools.parseBoolean(swo,false);
        }

    }
    */

    private int _allowNewDevice(Account currAcct, PrivateLabel privLabel)
    {
        if (currAcct == null) {
            // -- always deny if no account
            Print.logInfo("Allow New Device? Deny - null account");
            return DEVMODE_DENY;
        } else
        if (Account.isSystemAdmin(currAcct)) {
            // -- always allow if "sysadmin"
            Print.logDebug("Allow New Device? Allow - SystemAdmin");
            return DEVMODE_ALLOW;
        } else {
            String globAND = privLabel.getStringProperty(PrivateLabel.PROP_DeviceInfo_allowNewDevice,"");
            if (globAND.equalsIgnoreCase("sysadmin")) { // "deviceInfo.allowNewDevice" keyword
                // -- only allow if logged-in from "sysadmin"
                Print.logDebug("Allow New Device? Allow - if SystemAdmin");
                return DEVMODE_SYSADMIN;
            } else
            if (globAND.equalsIgnoreCase("manager")) { // "deviceInfo.allowNewDevice" keyword
                // -- only allow if logged-in account is "sysadmin" or an account-manager
                Print.logDebug("Allow New Device? Allow - if AccountManager");
                return DEVMODE_MANAGER;
            } else
            if (globAND.equalsIgnoreCase("maxlimit")) { // "deviceInfo.allowNewDevice" keyword
                // -- only allow if specified max-limit is > 0 (and not over-limit)
                Print.logDebug("Allow New Device? Allow - if MaxLimit > 0");
                return DEVMODE_MAXLIMIT;
            } else
            if (StringTools.parseBoolean(globAND,false) == false) {
                // -- explicit deny
                Print.logInfo("Allow New Device? Deny - disallowed");
                return DEVMODE_DENY;
            } else
            //if (currAcct.isAtMaximumDevices(true)) {
            //    // deny if over limit
            //    Print.logInfo("Allow New Device? Deny - over limit");
            //    return DEVMODE_DENY;
            //} else
            {
                // -- otherwise allow
                Print.logDebug("Allow New Device? Allow - as specified");
                return DEVMODE_ALLOW;
            }
        }
    }

    private int _allowDeleteDevice(Account currAcct, PrivateLabel privLabel)
    {
        if (currAcct == null) {
            // -- always deny if no account
            Print.logInfo("Allow Delete Device? Deny - null account");
            return DEVMODE_DENY;
        } else
        if (Account.isSystemAdmin(currAcct)) {
            // -- always allow if "sysadmin"
            Print.logDebug("Allow Delete Device? Allow - SystemAdmin");
            return DEVMODE_ALLOW;
        } else {
            String globAND = privLabel.getStringProperty(PrivateLabel.PROP_DeviceInfo_allowDeleteDevice,"");
            if (globAND.equalsIgnoreCase("sysadmin")) { // "deviceInfo.allowDelete" keyword
                // -- only allow if logged-in from "sysadmin"
                Print.logDebug("Allow Delete Device? Allow - if SystemAdmin");
                return DEVMODE_SYSADMIN;
            } else
            if (globAND.equalsIgnoreCase("manager")) { // "deviceInfo.allowNewDevice" keyword
                // -- only allow if logged-in account is "sysadmin" or an account-manager
                Print.logDebug("Allow Delete Device? Allow - if AccountManager");
                return DEVMODE_MANAGER;
            } else
            if (!StringTools.isBlank(globAND) &&
                StringTools.parseBoolean(globAND,false) == false) {
                // -- explicit deny
                Print.logInfo("Allow Delete Device? Deny - disallowed");
                return DEVMODE_DENY;
            } else
            {
                // -- otherwise allow
                Print.logDebug("Allow Delete Device? Allow - as specified");
                return DEVMODE_ALLOW;
            }
        }
    }

    /* update Device table with user entered information */
    private String _updateDeviceTable(RequestProperties reqState)
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final boolean      sysadminLogin = reqState.isLoggedInFromSysAdmin();
        final Account      currAcct      = reqState.getCurrentAccount(); // should not be null
        final User         currUser      = reqState.getCurrentUser();
        final PrivateLabel privLabel     = reqState.getPrivateLabel();
        final Device       selDev        = reqState.getSelectedDevice(); // 'selDev' is non-null
        final I18N         i18n          = privLabel.getI18N(DeviceInfo.class);
        final Locale       locale        = reqState.getLocale();
        final String       devTitles[]   = reqState.getDeviceTitles();
        final boolean      showFuelCap   = true;
        final boolean      showFuelProf  = showFuelCap;
        final boolean      showFuelEcon  = DBConfig.hasExtraPackage();
        final boolean      showFuelCost  = showFuelEcon;
        final boolean      acctBCEnabled = ((currAcct!=null)&&currAcct.getIsBorderCrossing())?true:false;
        final TimeZone     acctTimeZone  = Account.getTimeZone(currAcct,DateTime.getGMTTimeZone());
        final DateTime.DateStringFormat dsf = privLabel.getDateStringFormat();
        String  msg         = null;
        boolean groupsChg   = false;
        String  serverID    = AttributeTools.getRequestString(request, PARM_SERVER_ID              , "");
        String  codeVers    = AttributeTools.getRequestString(request, PARM_CODE_VERS              , "");
        String  uniqueID    = AttributeTools.getRequestString(request, PARM_DEV_UNIQ               , "");
        String  vehicleID   = AttributeTools.getRequestString(request, PARM_VEHICLE_ID             , null);
        String  vehMake     = AttributeTools.getRequestString(request, PARM_VEHICLE_MAKE           , null);
        String  vehModel    = AttributeTools.getRequestString(request, PARM_VEHICLE_MODEL          , null);
        String  licPlate    = AttributeTools.getRequestString(request, PARM_LICENSE_PLATE          , null);
        String  licExpire   = AttributeTools.getRequestString(request, PARM_LICENSE_EXPIRE         , "");
        String  shareStart  = AttributeTools.getRequestString(request, PARM_SHARE_START            , "");
        String  shareEnd    = AttributeTools.getRequestString(request, PARM_SHARE_END              , "");
        String  devActive   = AttributeTools.getRequestString(request, PARM_DEV_ACTIVE             , "");
        String  devExpire   = AttributeTools.getRequestString(request, PARM_DEV_EXPIRE             , "");
        String  devDesc     = AttributeTools.getRequestString(request, PARM_DEV_DESC               , "");
        String  devName     = AttributeTools.getRequestString(request, PARM_DEV_NAME               , "");
        String  pushpinID   = AttributeTools.getRequestString(request, PARM_ICON_ID                , "");
        String  dispColor   = AttributeTools.getRequestString(request, PARM_DISPLAY_COLOR          , "");
        String  equipType   = AttributeTools.getRequestString(request, PARM_DEV_EQUIP_TYPE         , "");
        String  equipStatus = AttributeTools.getRequestString(request, PARM_DEV_EQUIP_STATUS       , "");
        double  fuelCap1    = AttributeTools.getRequestDouble(request, PARM_DEV_FUEL_CAP1          , 0.0);
        double  fuelCap2    = AttributeTools.getRequestDouble(request, PARM_DEV_FUEL_CAP2          , 0.0);
        String  fuelProf1   = AttributeTools.getRequestString(request, PARM_DEV_FUEL_PROFILE1      , "");
        String  fuelProf1Sca= AttributeTools.getRequestString(request, PARM_DEV_FUEL_PROFILE1_SCALE, "");
        String  fuelProf1Ofs= AttributeTools.getRequestString(request, PARM_DEV_FUEL_PROFILE1_OFFS , "");
        String  fuelProf2   = AttributeTools.getRequestString(request, PARM_DEV_FUEL_PROFILE2      , "");
        String  fuelProf2Sca= AttributeTools.getRequestString(request, PARM_DEV_FUEL_PROFILE2_SCALE, "");
        String  fuelProf2Ofs= AttributeTools.getRequestString(request, PARM_DEV_FUEL_PROFILE2_OFFS , "");
        double  fuelEcon    = AttributeTools.getRequestDouble(request, PARM_DEV_FUEL_ECON          , 0.0);
        double  fuelCostPU  = AttributeTools.getRequestDouble(request, PARM_DEV_FUEL_COST          , 0.0);
        double  speedLimU   = AttributeTools.getRequestDouble(request, PARM_DEV_SPEED_LIMIT        , 0.0);
        String  driverID    = AttributeTools.getRequestString(request, PARM_DRIVER_ID              , "");
        String  prefUsrID   = AttributeTools.getRequestString(request, PARM_USER_ID                , "");
        String  imeiNum     = AttributeTools.getRequestString(request, PARM_DEV_IMEI               , "");
        String  serialNum   = AttributeTools.getRequestString(request, PARM_DEV_SERIAL_NO          , "");
        String  dataKey     = AttributeTools.getRequestString(request, PARM_DATA_KEY               , "");
        String  simPhone    = this._filterPhoneNum(AttributeTools.getRequestString(request,PARM_DEV_SIMPHONE,""));
        String  smsEmail    = AttributeTools.getRequestString(request, PARM_SMS_EMAIL              , "");
        String  noteText    = AttributeTools.getRequestString(request, PARM_DEV_NOTES              , "");
        String  linkURL     = AttributeTools.getRequestString(request, PARM_LINK_URL               , "");
        String  linkDesc    = AttributeTools.getRequestString(request, PARM_LINK_DESC              , "");
        double  fixedLat    = AttributeTools.getRequestDouble(request, PARM_FIXED_LAT              , 0.0);
        double  fixedLon    = AttributeTools.getRequestDouble(request, PARM_FIXED_LON              , 0.0);
        String  fixedAddr   = AttributeTools.getRequestString(request, PARM_FIXED_ADDRESS          , "");
        String  fixedPhone  = AttributeTools.getRequestString(request, PARM_FIXED_PHONE            , ""); // _filterPhoneNum?
        String  ignition    = AttributeTools.getRequestString(request, PARM_IGNITION_INDEX         , "");
        String  dcsPropsID  = AttributeTools.getRequestString(request, PARM_DCS_PROPS_ID           , "");
        String  dcsCfgStr   = AttributeTools.getRequestString(request, PARM_DCS_CONFIG_STR         , "");
        String  tcpSessID   = AttributeTools.getRequestString(request, PARM_TCP_SESSION_ID         , "");
        String  enELogStr   = AttributeTools.getRequestString(request, PARM_DEV_ENABLE_ELOG        , "");
        String  prefGrpID   = AttributeTools.getRequestString(request, PARM_GROUP_ID               , "");
        String  grpKeys[]   = AttributeTools.getMatchingKeys( request, PARM_DEV_GROUP_);
        String  cstKeys[]   = AttributeTools.getMatchingKeys( request, PARM_DEV_CUSTOM_);
        double  rptOdom     = AttributeTools.getRequestDouble(request, PARM_REPORT_ODOM            , 0.0);
        double  rptEngHrs   = AttributeTools.getRequestDouble(request, PARM_REPORT_HOURS           , 0.0);
        String  actvCorr    = AttributeTools.getRequestString(request, PARM_ACTIVE_CORRIDOR        , "");
        String  borderCross = AttributeTools.getRequestString(request, PARM_BORDER_CROSS_ENAB      , "");
      //String  worderID    = AttributeTools.getRequestString(request, PARM_WORKORDER_ID           , "");
        try {
            // -- unique id
            boolean editUniqID = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_UNIQUEID));
            if (editUniqID && !selDev.getUniqueID().equals(uniqueID)) {
                if (StringTools.isBlank(uniqueID)) {
                    selDev.setUniqueID("");
                } else {
                    // -- TODO: incorporate <Device>.validateUniqueID(...)
                    try {
                        Device dev = Transport.loadDeviceByUniqueID(uniqueID);
                        if (dev == null) {
                            selDev.setUniqueID(uniqueID);
                        } else {
                            String devAcctID = dev.getAccountID();
                            String devDevID  = dev.getDeviceID();
                            if (devAcctID.equals(reqState.getCurrentAccountID())) {
                                // -- same account, this user can fix this himself
                                msg = i18n.getString("DeviceInfo.uniqueIdAlreadyAssignedToDevice","UniqueID is already assigned to {0}: {1}", devTitles[0], devDevID); // UserErrMsg
                                selDev.setError(msg);
                            } else {
                                // -- different account, this user cannot fix this himself
                                Print.logWarn("UniqueID '%s' already assigned: %s/%s", uniqueID, devAcctID, devDevID);
                                msg = i18n.getString("DeviceInfo.uniqueIdAlreadyAssigned","UniqueID is already assigned to another Account"); // UserErrMsg
                                selDev.setError(msg);
                            }
                        }
                    } catch (DBException dbe) {
                        msg = i18n.getString("DeviceInfo.errorReadingUniqueID","Error while looking for other matching UniqueIDs"); // UserErrMsg
                        selDev.setError(msg);
                    }
                }
            }
            // -- server id
            boolean editServID = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_SERVERID)) &&
                privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_allowEditServerID,EDIT_SERVER_ID) && (selDev.getLastConnectTime() <= 0L);
            if (editServID && !selDev.getDeviceCode().equals(serverID) && DCServerFactory.hasServerConfig(serverID)) {
                selDev.setDeviceCode(serverID);
            }
            // -- code version (firmware version)
            boolean editCodeVer = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FIRMWARE)) &&
                privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_allowEditFirmwareVersion,EDIT_CODE_VERSION);
            if (editCodeVer && !selDev.getCodeVersion().equals(codeVers)) {
                selDev.setCodeVersion(codeVers);
            }
            // -- active
            boolean editActive = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_ACTIVE));
            boolean devActv = ComboOption.parseYesNoText(locale, devActive, true);
            if (editActive && (selDev.getIsActive() != devActv)) {
                selDev.setIsActive(devActv);
            }
            // -- expiration timestamp [2.6.4-B12]
            boolean devExpOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDeviceExpirationTime,SHOW_EXPIRATION_TIME);;
            boolean editExpire = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EXPIRE));
            if (devExpOK && editExpire) {
                long expireTS = GetDayNumberTS(dsf, devExpire, acctTimeZone, true);
                if (expireTS != selDev.getExpirationTime()) {
                    selDev.setExpirationTime(expireTS); // Epoch timestamp
                }
            }
            // -- share start/end timestamp [2.6.4-B12]
            if (Device.supportsMapShare()) {
                // -- share map date fields are available
                boolean shareMapOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSharedMapDates,SHOW_MAP_SHARE_DATES);;
                boolean editShare = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_SHARE));
                if (!editShare) {
                    // -- do not update shareMap dates
                } else
                if (shareMapOK) {
                    // -- shareMap dates displayed and editable
                    long shareStartTS = GetDayNumberTS(dsf, shareStart, acctTimeZone, true);
                    long shareEndTS   = GetDayNumberTS(dsf, shareEnd  , acctTimeZone, false);
                    if ((shareStartTS <= 0L) || (shareStartTS >= shareEndTS)) {
                        shareStartTS = 0L;
                        shareEndTS   = 0L;
                    }
                    if (shareStartTS != selDev.getMapShareStartTime()) {
                        selDev.setMapShareStartTime(shareStartTS); // Epoch timestamp
                    }
                    if (shareEndTS != selDev.getMapShareEndTime()) {
                        selDev.setMapShareEndTime(shareEndTS); // Epoch timestamp
                    }
                } else {
                    // -- shareMap dates not displayed, but editable
                    if (selDev.getMapShareStartTime() != 0L) {
                        selDev.setMapShareStartTime(0L); // Epoch timestamp
                    }
                    if (selDev.getMapShareEndTime() != 0L) {
                        selDev.setMapShareEndTime(0L); // Epoch timestamp
                    }
                }
            }
            // -- description
            if (!selDev.getDescription().equals(devDesc)) {
                selDev.setDescription(devDesc);
            }
            // -- display name
            if (!selDev.getDisplayName().equals(devName)) {
                selDev.setDisplayName(devName);
            }
            // -- vehicle ID (VIN)
            if ((vehicleID != null) && !selDev.getVehicleID().equals(vehicleID)) {
                selDev.setVehicleID(vehicleID);
            }
            // -- vehicle Make
            if ((vehMake != null) && !selDev.getVehicleMake().equals(vehMake)) {
                selDev.setVehicleMake(vehMake);
            }
            // -- vehicle Model
            if ((vehModel != null) && !selDev.getVehicleModel().equals(vehModel)) {
                selDev.setVehicleModel(vehModel);
            }
            // -- License Plate
            if ((licPlate != null) && !selDev.getLicensePlate().equals(licPlate)) {
                selDev.setLicensePlate(licPlate);
            }
            // -- License Expire
            long licExpireDN = GetDayNumber(dsf, licExpire);
            if ((licExpireDN >= 0L) && (licExpireDN != selDev.getLicenseExpire())) {
                selDev.setLicenseExpire(licExpireDN);
            }
            // -- equipment type/status
            if (!selDev.getEquipmentType().equals(equipType)) {
                selDev.setEquipmentType(equipType);
            }
            if (!selDev.getEquipmentStatus().equals(equipStatus)) {
                selDev.setEquipmentStatus(equipStatus);
            }
            // -- fuel capacity tank #1
            boolean editFuelCap1 = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_CAPACITY));
            if (showFuelCap && editFuelCap1) {
                double fuelCap1L = Account.getVolumeUnits(currAcct).convertToLiters(fuelCap1);
                if (selDev.getFuelCapacity() != fuelCap1L) {
                    selDev.setFuelCapacity(fuelCap1L);
                }
            }
            // -- fuel capacity tank #2
            boolean editFuelCap2 = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_CAPACITY));
            if (showFuelCap && editFuelCap2) {
                double fuelCap2L = Account.getVolumeUnits(currAcct).convertToLiters(fuelCap2);
                if (selDev.getFuelCapacity2() != fuelCap2L) {
                    selDev.setFuelCapacity2(fuelCap2L);
                }
            }
            // -- fuel tank profile #1
            boolean editFuelProf1 = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_PROFILE));
            if (showFuelProf && editFuelProf1) {
                String flpn1 = fuelProf1.equalsIgnoreCase(FuelLevelProfile.FLP_NONE_ID)? "" : fuelProf1;
                if ((flpn1.indexOf(":") < 0)                                       &&
                    (flpn1.equalsIgnoreCase(FuelLevelProfile.FLP_LINEAR_NAME  ) ||
                     flpn1.equalsIgnoreCase(FuelLevelProfile.FLP_CYLINDER_NAME)   )  ) {
                    int scale1  = StringTools.parseInt(fuelProf1Sca,0);
                    int offset1 = StringTools.parseInt(fuelProf1Ofs,0);
                    if (scale1 > 0) {
                        flpn1 += ":" + scale1; // "CYLIDER:600"
                        if (offset1 > 0) {
                            flpn1 += "," + offset1; // "CYLIDER:600,40"
                        }
                    }
                }
                if (!selDev.getFuelTankProfile().equals(flpn1)) {
                    selDev.setFuelTankProfile(flpn1);
                }
            }
            // -- fuel tank profile #2
            boolean editFuelProf2 = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_PROFILE));
            if (showFuelProf && editFuelProf2) {
                String flpn2 = fuelProf1.equalsIgnoreCase(FuelLevelProfile.FLP_NONE_ID)? "" : fuelProf2;
                if ((flpn2.indexOf(":") < 0)                                       &&
                    (flpn2.equalsIgnoreCase(FuelLevelProfile.FLP_LINEAR_NAME  ) ||
                     flpn2.equalsIgnoreCase(FuelLevelProfile.FLP_CYLINDER_NAME)   )  ) {
                    int scale2  = StringTools.parseInt(fuelProf2Sca,0);
                    int offset2 = StringTools.parseInt(fuelProf2Ofs,0);
                    if (scale2 > 0) {
                        flpn2 += ":" + scale2; // "CYLIDER:600"
                        if (offset2 > 0) {
                            flpn2 += "," + offset2; // "CYLIDER:600,40"
                        }
                    }
                }
                if (!selDev.getFuelTankProfile2().equals(flpn2)) {
                    selDev.setFuelTankProfile2(flpn2);
                }
            }
            // -- fuel economy (km/L)
            boolean editFuelEcon = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_ECONOMY));
            if (showFuelEcon && editFuelEcon) {
                double fuelEconomy = Account.getEconomyUnits(currAcct).convertToKPL(fuelEcon);
                if (selDev.getFuelEconomy() != fuelEconomy) {
                    selDev.setFuelEconomy(fuelEconomy);
                }
            }
            // -- fuel cost ($/L)
            boolean editFuelCost = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_ECONOMY));
            if (showFuelCost && editFuelCost) {
                double unitsPerLiter = Account.getVolumeUnits(currAcct).convertFromLiters(1.0);
                double costPerUnit   = fuelCostPU;
                double costPerLiter  = costPerUnit * unitsPerLiter;
                if (selDev.getFuelCostPerLiter() != costPerLiter) {
                    selDev.setFuelCostPerLiter(costPerLiter);
                }
            }
            // -- Driver ID
            if (!selDev.getDriverID().equals(driverID)) {
                selDev.setDriverID(driverID);
            }
            // -- Assigned User ID
            boolean showAssgnUsr = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showAssignedUserID,SHOW_ASSIGNED_USER);
            if (showAssgnUsr && Device.supportsAssignedUserID()) {
                if (!StringTools.isBlank(prefUsrID)) {
                    try {
                        User user = User.getUser(selDev.getAccount(),prefUsrID);
                        if (user == null) {
                            Print.logWarn("Assigned User ID does not exist: " + prefUsrID);
                            prefUsrID = "";
                        } else
                        if (!user.isAuthorizedDevice(selDev.getDeviceID())) {
                            Print.logWarn("Assigned User ID not authorized: " + prefUsrID);
                            prefUsrID = "";
                        }
                    } catch (DBException dbe) {
                        Print.logError("Unable to read user: " + prefUsrID + " [" + dbe);
                        prefUsrID = "";
                    }
                }
                if (!selDev.getAssignedUserID().equals(prefUsrID)) {
                    //Print.logInfo("Setting Assigned User ID: " + prefUsrID);
                    selDev.setAssignedUserID(prefUsrID);
                }
            }
            // -- IMEI number
            boolean editIMEI = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_IMEI));
            if (editIMEI && !selDev.getImeiNumber().equals(imeiNum)) {
                selDev.setImeiNumber(imeiNum);
            }
            // -- Serial number
            boolean editSERIAL = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_SERIAL));
            if (!selDev.getSerialNumber().equals(serialNum)) {
                selDev.setSerialNumber(serialNum);
            }
            // -- SIM phone number
            boolean editSIM = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_SIM));
            if (editSIM && !selDev.getSimPhoneNumber().equals(simPhone)) {
                boolean uniqSimPhone = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_uniqueSimPhoneNumber,false);
                if (uniqSimPhone && !StringTools.isBlank(simPhone)) { // [2.6.5-B29]
                    char   sep       = '/';
                    String devAcctID = selDev.getAccountID();
                    String devDevID  = selDev.getDeviceID();
                    java.util.List<String> devList = Device.getDeviceIDsForSimPhoneNumber(simPhone,sep); // "account,device"
                    int devListCnt = ListTools.size(devList);
                    if (devListCnt <= 0) {
                        // -- Ok.  SIM phone number has not yet been assigned
                    } else
                    if (devListCnt == 1) {
                        String acctDevID = devList.get(0); // "account,device"
                        if (!acctDevID.equalsIgnoreCase(devAcctID + sep + devDevID)) {
                            Print.logWarn("SIM Phone number ["+simPhone+"] already assigned: "+acctDevID);
                            msg = i18n.getString("DeviceInfo.simPhoneNotUnique","SIM phone number is not unique: " + simPhone);
                            selDev.setError(msg);
                            simPhone = "";
                        }
                    } else {
                        // multiple occurances found
                        String simAssignList = StringTools.join(devList,", ");
                        Print.logWarn("SIM Phone number ["+simPhone+"] already assigned: "+simAssignList);
                        msg = i18n.getString("DeviceInfo.simPhoneNotUnique","SIM phone number is not unique: " + simPhone);
                        selDev.setError(msg);
                        simPhone = "";
                    }
                }
                selDev.setSimPhoneNumber(simPhone);
            }
            // -- SMS EMail address
            boolean editSMSEm = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_SMS));
            if (editSMSEm && !selDev.getSmsEmail().equals(smsEmail)) { // EMail.validateAddress(smsEmail)
                String se[] = StringTools.split(smsEmail,',');
                if (ListTools.size(se) > 0) {
                    for (String s : se) {
                        if (!StringTools.isBlank(s)) {
                            // -- save first non-blank entry
                            smsEmail = s;
                            break;
                        }
                    }
                }
                selDev.setSmsEmail(smsEmail);
            }
            // -- Notes
            boolean notesOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showNotes,SHOW_NOTES);
            if (notesOK && !selDev.getNotes().equals(noteText)) {
                selDev.setNotes(noteText);
            }
            // -- Link URL/Description
            if (Device.supportsLinkURL()) {
                if (!selDev.getLinkURL().equals(linkURL)) {
                    selDev.setLinkURL(linkURL);
                }
                if (!selDev.getLinkDescription().equals(linkDesc)) {
                    selDev.setLinkDescription(linkDesc);
                }
            }
            // -- Fixed Latitude/Longitude
            if (Device.supportsFixedLocation()) {
                if ((fixedLat != 0.0) || (fixedLon != 0.0)) {
                    selDev.setFixedLatitude(fixedLat);
                    selDev.setFixedLongitude(fixedLon);
                }
                if (!selDev.getFixedAddress().equals(fixedAddr)) {
                    selDev.setFixedAddress(fixedAddr);
                }
                if (!selDev.getFixedContactPhone().equals(fixedPhone)) {
                    selDev.setFixedContactPhone(fixedPhone);
                }
            }
            // -- Ignition index
            boolean ignOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showIgnitionIndex,SHOW_IGNITION_NDX);
            if (ignOK && !StringTools.isBlank(ignition)) {
                String ign = ignition.toLowerCase();
                int ignNdx = -1;
                if (ign.equalsIgnoreCase(IGNITION_notApplicable)) {
                    ignNdx = -1;
                } else
                if (ign.equalsIgnoreCase(IGNITION_ignition)) {
                    ignNdx = StatusCodes.IGNITION_INPUT_INDEX;
                } else
                if (ign.equalsIgnoreCase(IGNITION_startStop)) {
                    ignNdx = StatusCodes.IGNITION_START_STOP;
                } else {
                    ignNdx = StringTools.parseInt(ignition,-1);
                }
                selDev.setIgnitionIndex(ignNdx); // may also clear "lastIgnitionOnTime"/"lastIgnitionOffTime"
            }
            // -- speed limit
            boolean speedLimOK = Device.hasRuleFactory(); // || privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSpeedLimit,SHOW_SPEED_LIMIT);
            double speedLimKPH = Account.getSpeedUnits(currAcct).convertToKPH(speedLimU);
            if (speedLimOK && (selDev.getSpeedLimitKPH() != speedLimKPH)) {
                selDev.setSpeedLimitKPH(speedLimKPH);
            }
            // -- Data Key
            boolean editDATKEY = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_DATKEY));
            String  dataKeyStr = privLabel.getStringProperty(PrivateLabel.PROP_DeviceInfo_showDataKey,SHOW_DATA_KEY);
            boolean dataKeyReq = "required".equalsIgnoreCase(dataKeyStr);
            boolean dataKeyOK  = dataKeyReq || "true".equalsIgnoreCase(dataKeyStr);
            if (!editDATKEY || !dataKeyOK) {
                // -- we are not editing the dataKey
            } else
            if (dataKeyReq && StringTools.isBlank(dataKey)) {
                msg = i18n.getString("DeviceInfo.dataKeyRequired","Non-blank 'Data Key' entry required");
                selDev.setError(msg);
            } else
            if (!selDev.getDataKey().equals(dataKey)) {
                // -- dataKey has changed
                selDev.setDataKey(dataKey);
            }
            // -- Pushpin ID
            boolean ppidOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPushpinID,SHOW_PUSHPIN_ID);
            if (ppidOK && !selDev.getPushpinID().equals(pushpinID)) {
                selDev.setPushpinID(pushpinID);
            }
            // -- Display Color
            boolean dcolorOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDisplayColor,SHOW_DISPLAY_COLOR);
            if (dcolorOK && !selDev.getDisplayColor().equals(dispColor)) {
                selDev.setDisplayColor(dispColor);
            }
            // -- Reported Odometer
            boolean lastOdomOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showReportedOdometer,SHOW_REPORTED_ODOM);
            if (lastOdomOK && Device.supportsLastOdometer()) { // Domain.Properties.deviceInfo.showReportedOdometer=false
                if (rptOdom >= 0.0) {
                    Account.DistanceUnits distUnits = Account.getDistanceUnits(currAcct);
                    double rptOdomKM  = distUnits.convertToKM(rptOdom);
                    double lastOdomKM = selDev.getLastOdometerKM();
                    double offsetKM   = rptOdomKM - lastOdomKM;
                    if (Math.abs(offsetKM - selDev.getOdometerOffsetKM()) >= 0.1) {
                        selDev.setOdometerOffsetKM(offsetKM);
                    }
                } else
                if (rptOdom < 0.0) {
                    selDev.setOdometerOffsetKM(0.0);
                }
            }
            // -- Reported EngineHours
            boolean lastEngHrsOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showReportedEngineHours,SHOW_REPORTED_ENG_HRS);
            if (lastEngHrsOK && Device.supportsLastEngineHours()) { // Domain.Properties.deviceInfo.showReportedEngineHours=false
                if (rptEngHrs >= 0.0) {
                    double lastEngHrs = selDev.getLastEngineHours();
                    double offsetHrs  = rptEngHrs - lastEngHrs;
                    if (Math.abs(offsetHrs - selDev.getEngineHoursOffset()) >= 0.01) {
                        selDev.setEngineHoursOffset(offsetHrs);
                    }
                }
            }
            // -- Maintenance Interval
            if (Device.supportsPeriodicMaintenance()) {
                Account.DistanceUnits distUnits = Account.getDistanceUnits(currAcct);
                // -- Odometer Maintenance
                boolean maintOdomOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showMaintenanceOdometer,SHOW_MAINTENANCE_ODOM);
                if (maintOdomOK) { // Domain.Properties.deviceInfo.showMaintenanceOdometer=false
                    // -- Odometer Maintenance #0
                    long    mIntrvKM0 = AttributeTools.getRequestLong(request, PARM_MAINT_INTERVKM_0, 0L);
                    double  intrvKM0  = distUnits.convertToKM((double)mIntrvKM0);
                    boolean mResetKM0 = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_MAINT_RESETKM_0,null));
                    selDev.setMaintIntervalKM0(intrvKM0);
                    if (mResetKM0) {
                        selDev.setMaintOdometerKM0(selDev.getLastOdometerKM());
                    }
                    // -- Odometer Maintenance #1
                    long    mIntrvKM1 = AttributeTools.getRequestLong(request, PARM_MAINT_INTERVKM_1, 0L);
                    double  intrvKM1  = distUnits.convertToKM((double)mIntrvKM1);
                    boolean mResetKM1 = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_MAINT_RESETKM_1,null));
                    selDev.setMaintIntervalKM1(intrvKM1);
                    if (mResetKM1) {
                        selDev.setMaintOdometerKM1(selDev.getLastOdometerKM());
                    }
                }
                // -- EngineHours Maintenane #0
                boolean maintHoursOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showMaintenanceHours,SHOW_MAINTENANCE_HOURS);
                if (maintHoursOK) { // Domain.Properties.deviceInfo.showMaintenanceHours=false
                    double  mIntrvHR0 = AttributeTools.getRequestDouble(request, PARM_MAINT_INTERVHR_0, 0.0);
                    double  intrvHR0  = mIntrvHR0;
                    boolean mResetHR0 = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_MAINT_RESETHR_0,null));
                    selDev.setMaintIntervalHR0(intrvHR0);
                    if (mResetHR0) {
                        double lastEngHrs = selDev.getLastEngineHours();
                        selDev.setMaintEngHoursHR0(lastEngHrs);
                    }
                }
                // -- maintenance notes
                boolean maintNotesOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showMaintenanceNotes,SHOW_MAINTENANCE_NOTES);
                if (maintNotesOK) { // Domain.Properties.deviceInfo.showMaintenanceNotes=false
                    String maintText = AttributeTools.getRequestString(request, PARM_MAINT_NOTES   , "");
                    if (!selDev.getMaintNotes().equals(maintText)) {
                        selDev.setMaintNotes(maintText);
                    }
                }
                // -- reminder interval
                boolean reminderOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showReminderMessage,SHOW_REMINDER_MESSAGE);
                if (reminderOK) { // Domain.Properties.deviceInfo.showReminderMessage=false
                    // -- reminder interval
                    String  remIntrv = AttributeTools.getRequestString(request, PARM_REMIND_INTERVAL, "");
                    if (!selDev.getReminderInterval().equals(remIntrv)) {
                        selDev.setReminderInterval(remIntrv);
                    }
                    // -- reminder message
                    String  remMsg = AttributeTools.getRequestString(request, PARM_REMIND_MESSAGE, "");
                    if (!selDev.getReminderMessage().equals(remMsg)) {
                        selDev.setReminderMessage(remMsg);
                    }
                }
                // -- service time
                boolean servTimeOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showServiceTime,SHOW_SERVICE_TIME);
                if (servTimeOK) { // Domain.Properties.deviceInfo.showServiceTime=false
                    TimeZone tmz = Account.getTimeZone(selDev.getAccount(),DateTime.getGMTTimeZone());
                    String lastServ   = AttributeTools.getRequestString(request, PARM_LAST_SERVICE_TIME, "");
                    long   lastServTS = GetEpochTimeDayEnd(dsf, lastServ, tmz);
                    selDev.setLastServiceTime(lastServTS); // saved as epoch timestamp
                    String nextServ   = AttributeTools.getRequestString(request, PARM_NEXT_SERVICE_TIME, "");
                    long   nextServTS = GetEpochTimeDayStart(dsf, nextServ, tmz);
                    selDev.setNextServiceTime(nextServTS); // saved as epoch timestamp
                }
            }
            // -- hours of operation
            boolean workHoursOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showHoursOfOperation,SHOW_HOURS_OF_OPERATION);
            if (workHoursOK && Device.supportsHoursOfOperation()) { // Domain.Properties.deviceInfo.showHoursOfOperation=false
                String whStr = AttributeTools.getRequestString(request, PARM_HOURS_OF_OPERATION, "");
                if (!whStr.equals(selDev.getHoursOfOperation())) {
                    selDev.setHoursOfOperation(whStr);
                }
            }
            // -- Reset Fault codes
            boolean faultCodesOK = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showFaultCodes,SHOW_FAULT_CODES);
            if (faultCodesOK && Device.supportsFaultCodes()) {
                boolean fReset = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_FAULT_RESET,null));
                if (fReset) {
                    Print.logInfo("Resetting Fault Codes ...");
                    selDev.setLastFaultCode(null);          // clear fault codes
                    selDev.setLastMalfunctionLamp(false);   // clear MIL indicator
                }
            }
            // -- Rule Engine Notification
            if (_showNotificationFields(privLabel)) {
                String  ruleAllow   = AttributeTools.getRequestString(  request, PARM_DEV_RULE_ALLOW  ,  null); // "Notify Enable"
                String  notifyEmail = AttributeTools.getRequestString(  request, PARM_DEV_RULE_EMAIL  ,  null);
                boolean alertReset  = AttributeTools.getRequestCheckbox(request, PARM_LAST_ALERT_RESET);
                String  ruleSel     = AttributeTools.getRequestString(  request, PARM_DEV_RULE_SEL    ,  null);
                String  ruleDesc    = AttributeTools.getRequestString(  request, PARM_DEV_RULE_DESC   ,  null);
                String  ruleSubj    = AttributeTools.getRequestString(  request, PARM_DEV_RULE_SUBJ   ,  null);
                String  ruleText    = AttributeTools.getRequestString(  request, PARM_DEV_RULE_TEXT   ,  null);
              //String  ruleWrap    = AttributeTools.getRequestString(  request, PARM_DEV_RULE_WRAP   ,  null);
                // -- Allow Notification
                boolean allowNtfy = ComboOption.parseYesNoText(locale, ruleAllow, true);
                if (selDev.getAllowNotify() != allowNtfy) {
                    selDev.setAllowNotify(allowNtfy);
                }
                // -- Notification email
                if (notifyEmail != null) {
                    String devNE = selDev.getNotifyEmail();  // no acct/user email
                    if (StringTools.isBlank(notifyEmail)) {
                        if (!devNE.equals(notifyEmail)) {
                            selDev.setNotifyEmail(notifyEmail);
                        }
                    } else
                    if (EMail.validateAddresses(notifyEmail,true)) {
                        if (!devNE.equals(notifyEmail)) {
                            selDev.setNotifyEmail(notifyEmail);
                        }
                    } else {
                        msg = i18n.getString("DeviceInfo.enterEMail","Please enter a valid notification email/sms address");
                        selDev.setError(msg);
                    }
                }
                // -- notification selector
                if (ruleSel != null) {
                    if (DeviceInfo.ShowNotifySelector() && !Device.CheckSelectorSyntax(ruleSel)) {
                        Print.logInfo("Notification selector has a syntax error: " + ruleSel);
                        msg = i18n.getString("DeviceInfo.ruleError","Notification rule contains a syntax error");
                        selDev.setError(msg);
                    } else {
                        // -- update rule selector (if changed)
                        if (!selDev.getNotifySelector().equals(ruleSel)) {
                            selDev.setNotifySelector(ruleSel);
                        }
                        //selDev.setAllowNotify(!StringTools.isBlank(ruleSel));
                    }
                }
                // -- notification description
                if (ruleDesc != null) {
                    if (!selDev.getNotifyDescription().equals(ruleDesc)) {
                        selDev.setNotifyDescription(ruleDesc);
                    }
                }
                // -- notification subject
                if (ruleSubj != null) {
                    if (!selDev.getNotifySubject().equals(ruleSubj)) {
                        selDev.setNotifySubject(ruleSubj);
                    }
                }
                // -- notification message
                if (ruleText != null) {
                    if (!selDev.getNotifyText().equals(ruleText)) {
                        selDev.setNotifyText(ruleText);
                    }
                }
                // -- notify wrapper
                boolean ntfyWrap = false; // ComboOption.parseYesNoText(locale, ruleWrap, true);
                if (selDev.getNotifyUseWrapper() != ntfyWrap) {
                    selDev.setNotifyUseWrapper(ntfyWrap);
                }
                // -- last-notify-time reset
                if ((selDev.getLastNotifyTime() != 0L) && alertReset) {
                    selDev.clearLastNotifyEvent(false/*nosave*/);
                }
            }
            // -- Active Corridor
            if (Device.hasENRE() && Device.supportsActiveCorridor()) {
                if (!selDev.getActiveCorridor().equals(actvCorr)) {
                    selDev.setActiveCorridor(actvCorr);
                }
            }
            // -- BorderCrossing
            if (acctBCEnabled && Device.supportsBorderCrossing()) {
                int bcState = ComboOption.parseYesNoText(locale,borderCross,true)?
                    Device.BorderCrossingState.ON.getIntValue() :
                    Device.BorderCrossingState.OFF.getIntValue();
                if (selDev.getBorderCrossing() != bcState) {
                    selDev.setBorderCrossing(bcState);
                }
            }
            // -- WorkOrder ID
            /*
            if (_showWorkOrderID(privLabel)) {
                if (!selDev.getWorkOrderID().equals(worderID)) {
                    selDev.setWorkOrderID(worderID);
                }
            }
            */
            // -- DCS properties ID
            boolean showDCSPropID = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDcsPropertiesID,SHOW_DCS_PROPERTIES_ID);
            if (showDCSPropID) {
                if (!selDev.getDcsPropertiesID().equals(dcsPropsID)) {
                    selDev.setDcsPropertiesID(dcsPropsID);
                }
            }
            // -- DCS config string
            boolean showDCSCfgStr = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDcsConfigString,SHOW_DCS_CONFIG_STRING);
            if (showDCSCfgStr) {
                if (!selDev.getDcsConfigString().equals(dcsCfgStr)) {
                    selDev.setDcsConfigString(dcsCfgStr);
                }
            }
            // -- Fixed TCP Session ID
            boolean showTcpSessID = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showStaticTcpSessionID,SHOW_FIXED_TCP_SESSION_ID);
            if (showTcpSessID) {
                if (!selDev.getFixedTcpSessionID().equals(tcpSessID)) {
                    selDev.setFixedTcpSessionID(tcpSessID);
                }
            }
            // -- Enable Driver ELogs
            if (Account.IsELogEnabled(selDev.getAccount())) {
                boolean elogEnable = ComboOption.parseYesNoText(locale, enELogStr, false);
                if (selDev.getELogEnabled() != elogEnable) {
                    selDev.setELogEnabled(elogEnable);
                }
            }
            // -- Preferred Group ID [PARM_GROUP_ID]
            String  skipGrpID     = null;
            boolean showPrefGrp   = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPreferredGroupID,SHOW_PREFERRED_GROUP);
            if (showPrefGrp) {
                if (DeviceGroup.DEVICE_GROUP_ALL.equalsIgnoreCase(prefGrpID)) {
                    prefGrpID = "";  // "all" ==> ""
                }
                //Print.logInfo("Preferred groupID: " + prefGrpID);
                if (selDev.getGroupID().equals(prefGrpID)) {
                    // -- already the same groupID, skip
                    //Print.logInfo("Preferred groupID already matched Device");
                    prefGrpID = "";
                } else
                if (StringTools.isBlank(prefGrpID)) {
                    // -- clear groupID
                    //Print.logInfo("Clearing Preferred GroupID ...");
                    selDev.setGroupID(prefGrpID);
                } else {
                    // -- make sure that the current user is authorized for the group "prefGrpID"
                    DeviceGroup dg = DeviceGroup.getDeviceGroup(currAcct, prefGrpID);
                    OrderedSet<String> dgList = reqState.getDeviceGroupIDList(false); // user groups
                    if (dg == null) {
                        Print.logError("DeviceGroup does not exist: " + prefGrpID);
                        prefGrpID = "";
                    } else
                    if (!dgList.contains(prefGrpID)) {
                        Print.logError("User is not authorized to DeviceGroup: " + prefGrpID);
                        prefGrpID = "";
                    } else {
                        // -- preferred groupID
                        //Print.logInfo("Setting current Device Preferred GroupID: " + prefGrpID);
                        selDev.setGroupID(prefGrpID);
                        boolean addGroupEntry = showPrefGrp &&
                            privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_addToPreferredGroup,ADD_TO_PREFERRED_GROUP);
                        if (addGroupEntry) {
                            // -- add a group entry for this group
                            try {
                                //Print.logInfo("Adding current Device to Group: " + prefGrpID);
                                dg.addDeviceToDeviceGroup(selDev);
                                skipGrpID = prefGrpID;
                            } catch (DBException dbe) {
                                //
                                Print.logException("Unable to add current Device to preferred group", dbe);
                            }
                        } else {
                            //Print.logInfo("Not adding current Device to Group: " + prefGrpID);
                        }
                    }
                }
            } else {
                prefGrpID = "";
            }
            // -- Subscriber info
            if (_showSubscriberInfo(privLabel)) {
                String subsID   = AttributeTools.getRequestString(request, PARM_SUBSCRIBE_ID    , null);
                String subsName = AttributeTools.getRequestString(request, PARM_SUBSCRIBE_NAME  , null);
                String subsAvat = AttributeTools.getRequestString(request, PARM_SUBSCRIBE_AVATAR, null);
                selDev.setSubscriberID(subsID);
                selDev.setSubscriberName(subsName);
                selDev.setSubscriberAvatar(subsAvat);
            }
            // -- DeviceGroups
            if (!selDev.hasError()) {
                String accountID = selDev.getAccountID();
                String deviceID  = selDev.getDeviceID();
                // -- 'grpKey' may only contain 'checked' items!
                OrderedSet<String> fullGroupSet = reqState.getDeviceGroupIDList(true);
                // -- add checked groups
                if (!ListTools.isEmpty(grpKeys)) {
                    for (int i = 0; i < grpKeys.length; i++) {
                        String grpID = grpKeys[i].substring(PARM_DEV_GROUP_.length());
                        //Print.logInfo("Checking GroupID: " + grpID);
                        if (grpID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                            // -- skip Group "ALL"
                        } else
                        if (!StringTools.isBlank(skipGrpID) && skipGrpID.equals(grpID)) {
                            // -- "grpID" already handled
                        } else {
                            String  chkStr = AttributeTools.getRequestString(request,grpKeys[i],"");
                            boolean chked  = chkStr.equalsIgnoreCase("on");
                            boolean exists = DeviceGroup.isDeviceInDeviceGroup(accountID, grpID, deviceID);
                            //Print.logInfo("Checking group : " + grpID + " [checked=" + chked + "]");
                            if (chked) {
                                if (!exists) {
                                    DeviceGroup.addDeviceToDeviceGroup(accountID, grpID, deviceID);
                                    groupsChg = true;
                                }
                            } else {
                                if (exists) {
                                    DeviceGroup.removeDeviceFromDeviceGroup(accountID, grpID, deviceID);
                                    groupsChg = true;
                                }
                            }
                            fullGroupSet.remove(grpID);
                        }
                    }
                }
                // -- delete remaining (unchecked) groups
                for (Iterator<String> i = fullGroupSet.iterator(); i.hasNext();) {
                    String grpID = i.next();
                    if (grpID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                        // -- skip Group "ALL"
                    } else
                    if (!StringTools.isBlank(skipGrpID) && skipGrpID.equals(grpID)) {
                        // -- "grpID" already handled
                    } else {
                        boolean exists = DeviceGroup.isDeviceInDeviceGroup(accountID, grpID, deviceID);
                        //Print.logInfo("Removing group: " + grpID + " [" + exists + "]");
                        if (exists) {
                            DeviceGroup.removeDeviceFromDeviceGroup(accountID, grpID, deviceID);
                            groupsChg = true;
                        }
                    }
                }
            }
            // -- Custom Attributes
            if (!ListTools.isEmpty(cstKeys)) {
                String oldCustAttr = selDev.getCustomAttributes();
                RTProperties rtp = selDev.getCustomAttributesRTP();
                for (int i = 0; i < cstKeys.length; i++) {
                    String cstKey = cstKeys[i];
                    String rtpVal = AttributeTools.getRequestString(request, cstKey, "");
                    String rtpKey = cstKey.substring(PARM_DEV_CUSTOM_.length());
                    rtp.setString(rtpKey, rtpVal);
                }
                String rtpStr = rtp.toString();
                if (!rtpStr.equals(oldCustAttr)) {
                    //Print.logInfo("Setting custom attributes: " + rtpStr);
                    selDev.setCustomAttributes(rtpStr);
                }
            }
            // -- save
            if (selDev.hasError()) {
                // -- should stay on same page
                Print.logInfo("An error occured during Edit ...");
            } else
            if (selDev.hasChanged()) {
                selDev.save();
                msg = i18n.getString("DeviceInfo.updatedDevice","{0} information updated", devTitles);
            } else
            if (groupsChg) {
                String grpTitles[] = reqState.getDeviceGroupTitles();
                msg = i18n.getString("DeviceInfo.updatedDeviceGroups","{0} membership updated", grpTitles);
            } else {
                // -- nothing changed
                Print.logInfo("Nothing has changed for this Device ...");
            }
        } catch (Throwable t) {
            Print.logException("Updating Device", t);
            msg = i18n.getString("DeviceInfo.errorUpdate","Internal error updating {0}", devTitles);
            selDev.setError(msg);
        }
        return msg;
    }

    // ------------------------------------------------------------------------

    /* write html */
    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final boolean      sysadminLogin = reqState.isLoggedInFromSysAdmin();
        final PrivateLabel privLabel     = reqState.getPrivateLabel();
        final I18N         i18n          = privLabel.getI18N(DeviceInfo.class);
        final Locale       locale        = reqState.getLocale();
        final String       devTitles[]   = reqState.getDeviceTitles();
        final String       grpTitles[]   = reqState.getDeviceGroupTitles();
        final Account      currAcct      = reqState.getCurrentAccount(); // should not be null
        final String       currAcctID    = reqState.getCurrentAccountID();
        final User         currUser      = reqState.getCurrentUser(); // may be null
        final String       pageName      = this.getPageName();
        final boolean      acctBCEnabled = ((currAcct!=null)&&currAcct.getIsBorderCrossing())?true:false;
        final TimeZone     acctTimeZone  = Account.getTimeZone(currAcct,DateTime.getGMTTimeZone());
        String m = pageMsg;
        boolean error = false;
        String cmdBtnTitle = i18n.getString("DeviceInfo.commands","Commands"); // included here to maintain I18N string

        /* device */
        OrderedSet<String> devList = reqState.getDeviceIDList(true/*inclInactv*/);
        if (devList == null) { devList = new OrderedSet<String>(); }
        Device  selDev    = reqState.getSelectedDevice();
        boolean actualDev = reqState.isActualSpecifiedDevice();
        String  selDevID  = (selDev != null)? selDev.getDeviceID() : "";

        /* new/delete device creation */
        final int allowNewDevMode = this._allowNewDevice(currAcct, privLabel);
        final int allowDelDevMode = this._allowDeleteDevice(currAcct, privLabel);
        boolean allowNew        = privLabel.hasAllAccess(currUser,this.getAclName());
        boolean allowDelete     = allowNew;
        int     overDevLimit    = -1; // undefined
        boolean allowNewZeroUnl = true;
        // -- check "New"
        if (!allowNew) {
            // -- already denied per ACL
            Print.logInfo("Allow New Device? Deny - per ACL");
        } else
        if (currAcct == null) {
            // -- null account (should not be null)
            Print.logInfo("Allow New Device? Deny - current account is null");
            allowNew    = false;
            allowDelete = false;
        } else
        if (allowNewDevMode == DEVMODE_DENY) {
            // -- explicitly denied
            Print.logInfo("Allow New Device: Deny - per '"+PrivateLabel.PROP_DeviceInfo_allowNewDevice+"' property");
            allowNew    = false;
            allowDelete = false;
        } else
        if (allowNewDevMode == DEVMODE_SYSADMIN) {
            if (!sysadminLogin) {
                // -- allow only if currently logged in from sysadmin "System Accounts"
                Print.logInfo("Allow New Device? Deny - not SysAdmin login");
                allowNew    = false;
                allowDelete = false;
            }
        } else
        if (allowNewDevMode == DEVMODE_MANAGER) {
            if (!Account.isAccountManager(currAcct) && !Account.isSystemAdmin(currAcct)) {
                // -- allow only if AccountManager or SystemAdmin
                Print.logInfo("Allow New Device? Deny - not AccountManager");
                allowNew    = false;
                allowDelete = false;
            }
        } else
        if (allowNewDevMode == DEVMODE_MAXLIMIT) {
            allowNewZeroUnl = false;
            if (sysadminLogin) {
                // -- no max limit
            } else
            if (currAcct.isAtMaximumDevices(false)) {
                // -- over device limit
                Print.logInfo("Allow New Device: Deny - at maximum devices, per '"+PrivateLabel.PROP_DeviceInfo_allowNewDevice+"' property");
                overDevLimit = 1;
              //allowNew     = false;
              //allowDelete  = false; <== leave as-is (allow delete, for now)
            } else {
                // -- not over device limit
                overDevLimit = 0;
            }
        } else
        if (allowNewDevMode == DEVMODE_ALLOW) {
            if (sysadminLogin) {
                // -- no max limit
            } else
            if (currAcct.isAtMaximumDevices(true/*0=unlim*/)) {
                // -- over device limit
                Print.logInfo("Allow New Device? Deny - over limit");
                overDevLimit = 1;
              //allowNew     = false;
              //allowDelete  = false; <== leave as-is (allow delete, for now)
            } else {
                // -- not over device limit
                overDevLimit = 0;
            }
        }
        // -- check "Delete"
        if (!allowDelete) {
            Print.logInfo("Allow Delete Device? Deny - per ACL/New");
        } else
        if (allowDelDevMode == DEVMODE_DENY) {
            // -- explicitly denied
            Print.logInfo("Allow Delete Device: Deny - per '"+PrivateLabel.PROP_DeviceInfo_allowDeleteDevice+"' property");
            allowDelete = false;
        } else
        if (allowDelDevMode == DEVMODE_SYSADMIN) {
            if (!sysadminLogin) {
                // -- allow only if currently logged in from sysadmin "System Accouts"
                Print.logInfo("Allow Delete Device? Deny - not SysAdmin login");
                allowDelete = false;
            }
        } else
        if (allowDelDevMode == DEVMODE_MANAGER) {
            if (!Account.isAccountManager(currAcct) && !Account.isSystemAdmin(currAcct)) {
                // -- allow only if AccountManager or SystemAdmin
                Print.logInfo("Allow Delete Device? Deny - not AccountManager");
                allowDelete = false;
            }
        }

        /* ACL allow edit/view */
        boolean allowEdit = allowNew  || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* "Commands"("Properties") */
        boolean allowCommand =
            (currAcct != null) &&
            allowView &&
            privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_COMMANDS)) &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPropertiesButton,true);
        if (Print.isDebugLoggingLevel()) {
            if (allowCommand) {
                Print.logInfo("Commands: Allow Commands: " + allowCommand);
            } else {
                StringBuffer sb = new StringBuffer();
                if (!(currAcct != null)) { sb.append("-Account"); }
                if (!allowView) { sb.append("-View"); }
                if (!privLabel.hasWriteAccess(currUser,this.getAclName(_ACL_COMMANDS))) { sb.append("-ACL"); }
                if (!privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPropertiesButton,true)) { sb.append("-Show"); }
                Print.logWarn("Commands: Allow Commands: " + allowCommand + " ["+sb+"]");
            }
        }

        /* "SMS" Commands */
        DeviceInfo._initDeviceCommandHandlers(); // initialize to pick up SMS command button [2.6.1-B21]
        boolean allowSmsCmd =
            (currAcct != null) &&
            currAcct.getSmsEnabled() &&
            allowView &&
            privLabel.hasWriteAccess(currUser,this.getAclName(_ACL_SMS)) &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSmsButton,false) &&
            (SMSCommandHandler != null) &&
            SMSCommandHandler.hasSmsCommands(reqState); // DeviceCmd_SMS
        if (Print.isDebugLoggingLevel()) {
            if (allowSmsCmd) {
                Print.logInfo("Commands: Allow SMS Commands: " + allowSmsCmd);
            } else {
                StringBuffer sb = new StringBuffer();
                if (!(currAcct != null)) { sb.append("-Account"); }
                if (!((currAcct != null) && currAcct.getSmsEnabled())) { sb.append("-Enabled"); }
                if (!allowView) { sb.append("-View"); }
                if (!privLabel.hasWriteAccess(currUser,this.getAclName(_ACL_SMS))) { sb.append("-ACL"); }
                if (!privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSmsButton,false)) { sb.append("-Show"); }
                if (!(SMSCommandHandler != null)) { sb.append("-Null"); }
                if (!((SMSCommandHandler != null) && SMSCommandHandler.hasSmsCommands(reqState))) { sb.append("-SMS"); }
                Print.logWarn("Commands: Allow SMS commands: " + allowSmsCmd + " ["+sb+"]");
            }
        }

        /* submit buttons */
        String  submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String  submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String  submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String  submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String  submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");
        String  submitQueue  = AttributeTools.getRequestString(request, PARM_SUBMIT_QUE , "");
        String  submitProps  = AttributeTools.getRequestString(request, PARM_SUBMIT_PROP, "");
        String  submitSms    = AttributeTools.getRequestString(request, PARM_SUBMIT_SMS , "");

        /* ACL view/edit  */
        boolean editServID   = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_SERVERID     ));
        boolean viewServID   = editServID    || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_SERVERID     ));
        boolean editCodeVer  = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FIRMWARE     ));
        boolean viewCodeVer  = editCodeVer   || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_FIRMWARE     ));
        boolean editUniqID   = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_UNIQUEID     ));
        boolean viewUniqID   = editUniqID    || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_UNIQUEID     ));
        boolean editActive   = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_ACTIVE       ));
        boolean viewActive   = editActive    || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_ACTIVE       ));
        boolean editExpire   = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EXPIRE       ));
        boolean viewExpire   = editExpire    || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_EXPIRE       ));
        boolean editShare    = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_SHARE        ));
        boolean viewShare    = editShare     || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_SHARE        ));
        boolean editRules    = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_RULES        ));
        boolean viewRules    = editRules     || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_RULES        ));
        boolean editSMSEm    = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_SMS     ));
        boolean viewSMSEm    = editSMSEm     || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_EDIT_SMS     ));
        boolean editSIM      = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_SIM     ));
        boolean viewSIM      = editSIM       || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_EDIT_SIM     ));
        boolean editEqStat   = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_EQSTAT  ));
        boolean viewEqStat   = editEqStat    || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_EDIT_EQSTAT  ));
        boolean editIMEI     = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_IMEI    ));
        boolean viewIMEI     = editIMEI      || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_EDIT_IMEI    ));
        boolean editSERIAL   = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_SERIAL  ));
        boolean viewSERIAL   = editSERIAL    || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_EDIT_SERIAL  ));
        boolean editDATKEY   = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_EDIT_DATKEY  ));
        boolean viewDATKEY   = editDATKEY    || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_EDIT_DATKEY  ));
        boolean editFuelCap  = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_CAPACITY));
        boolean viewFuelCap  = editFuelCap   || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_FUEL_CAPACITY));
        boolean editFuelPrf  = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_PROFILE ));
        boolean viewFuelPrf  = editFuelPrf   || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_FUEL_PROFILE ));
        boolean editFuelEco  = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_ECONOMY ));
        boolean viewFuelEco  = editFuelEco   || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_FUEL_ECONOMY ));
        boolean editFuelCst  = sysadminLogin || privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_FUEL_ECONOMY ));
        boolean viewFuelCst  = editFuelCst   || privLabel.hasReadAccess( currUser, this.getAclName(_ACL_FUEL_ECONOMY ));

        /* command */
        String  deviceCmd    = reqState.getCommandName();
      //boolean refreshList  = deviceCmd.equals(COMMAND_INFO_REFRESH);
        boolean selectDevice = deviceCmd.equals(COMMAND_INFO_SEL_DEVICE);
        boolean newDevice    = deviceCmd.equals(COMMAND_INFO_NEW_DEVICE);
        boolean updateDevice = deviceCmd.equals(COMMAND_INFO_UPD_DEVICE);
        boolean sendCommand  = deviceCmd.equals(COMMAND_INFO_UPD_PROPS);
        boolean updateSms    = deviceCmd.equals(COMMAND_INFO_UPD_SMS);
        boolean deleteDevice = false;

        /* ui display */
        boolean uiList       = false;
        boolean uiEdit       = false;
        boolean uiView       = false;
        boolean uiCmd        = false;
        boolean uiSms        = false;

        /* DeviceCmdHandler */
        DeviceCmdHandler dcHandler = null;

        /* pre-qualify commands */
        String newDeviceID = null;
        if (newDevice) {
            if (!allowNew) {
                newDevice = false; // not authorized
            } else
            if (overDevLimit > 0) {
                newDevice = false; // over limit
            } else {
                if (allowNewDevMode == DEVMODE_SYSADMIN) {
                    // -- TODO: need to check some "Create Device Password" field
                }
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newDeviceID = AttributeTools.getRequestString(httpReq,PARM_NEW_NAME,"").trim();
                newDeviceID = newDeviceID.toLowerCase();
                if (StringTools.isBlank(newDeviceID)) {
                    m = i18n.getString("DeviceInfo.enterNewDevice","Please enter a new {0} ID.", devTitles); // UserErrMsg
                    error = true;
                    newDevice = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_DeviceInfo_validateNewIDs,*/newDeviceID)) {
                    m = i18n.getString("DeviceInfo.invalidIDChar","ID contains invalid characters"); // UserErrMsg
                    error = true;
                    newDevice = false;
                }
            }
        } else
        if (updateDevice) {
            if (!allowEdit) {
                updateDevice = false; // not authorized
            } else
            if (!SubmitMatch(submitChange,i18n.getString("DeviceInfo.change","Change"))) {
                updateDevice = false;
            } else
            if (selDev == null) {
                // should not occur
                m = i18n.getString("DeviceInfo.unableToUpdate","Unable to update Device, ID not found"); // UserErrMsg
                error = true;
                updateDevice = false;
            }
        } else
        if (sendCommand) {
            if (!allowCommand) {
                sendCommand = false; // not authorized
            } else
            if (!SubmitMatch(submitQueue,i18n.getString("DeviceInfo.queue","Queue"))) {
                sendCommand = false; // button not pressed
            } else
            if (selDev == null) {
                m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles); // UserErrMsg
                error = true;
                sendCommand = false; // no device selected
            } else
            if (StringTools.isBlank(selDev.getDeviceCode())) {
                Print.logInfo("DeviceCode/ServerID is blank");
                sendCommand = false;
            } else {
                dcHandler = this.getDeviceCommandHandler(selDev.getDeviceCode());
                if (dcHandler == null) {
                    Print.logWarn("DeviceCmdHandler not found: " + selDev.getDeviceCode());
                    sendCommand = false; // not found
                } else
                if (!dcHandler.deviceSupportsCommands(selDev)) {
                    Print.logWarn("DeviceCode/ServerID not supported by handler: " + selDev.getDeviceCode());
                    sendCommand = false; // not supported
                }
            }
        } else
        if (updateSms) {
            if (!allowSmsCmd) {
                updateSms = false; // not authorized
            } else
            if (!SubmitMatch(submitQueue,i18n.getString("DeviceInfo.queue","Queue"))) {
                updateSms = false; // button not pressed
            } else
            if (selDev == null) {
                m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles); // UserErrMsg
                error = true;
                updateSms = false; // no device selected
            //} else
            //if (StringTools.isBlank(selDev.getDeviceCode())) {
            //    Print.logInfo("DeviceCode/ServerID is blank");
            //    updateSms = false;
            } else {
                dcHandler = SMSCommandHandler;
                if (dcHandler == null) {
                    Print.logWarn("SMS DeviceCmdHandler not found");
                    updateSms = false; // not found
                } else
                if (!dcHandler.deviceSupportsCommands(selDev)) {
                    Print.logWarn("DeviceCode/ServerID not supported by handler: " + selDev.getDeviceCode());
                    updateSms = false; // not supported
                }
            }
        } else
        if (selectDevice) {
            if (SubmitMatch(submitDelete,i18n.getString("DeviceInfo.delete","Delete"))) {
                if (!allowDelete) {
                    deleteDevice = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles); // UserErrMsg
                    error = true;
                    deleteDevice = false; // not selected
                } else
                if (!actualDev) {
                    // -- "selDev" is not the actual selected device (possibly due to invalid deviceID?)
                    m = i18n.getString("DeviceInfo.unableToDelete","Unable to delete selected {0}", devTitles); // UserErrMsg
                    error = true;
                    deleteDevice = false; // not selected
                } else {
                    deleteDevice = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("DeviceInfo.edit","Edit"))) {
                if (!allowEdit) {
                    uiEdit = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles); // UserErrMsg
                    error = true;
                    uiEdit = false; // not selected
                } else {
                    uiEdit = true;
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("DeviceInfo.view","View"))) {
                if (!allowView) {
                    uiView = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles); // UserErrMsg
                    error = true;
                    uiView = false; // not selected
                } else {
                    uiView = true;
                }
            } else
            if (SubmitMatch(submitProps,i18n.getString("DeviceInfo.properties","Commands"))) {
                if (!allowCommand) {
                    uiCmd = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles); // UserErrMsg
                    error = true;
                    uiCmd = false; // not selected
                } else {
                    String dcsName = selDev.getDeviceCode();
                    dcHandler = this.getDeviceCommandHandler(dcsName);
                    if (dcHandler == null) {
                        if (StringTools.isBlank(dcsName)) {
                            Print.logWarn("ServerID is blank");
                            m = i18n.getString("DeviceInfo.deviceCommandsNotDefined","Unknown {0} type (blank)", devTitles); // UserErrMsg
                        } else {
                            Print.logWarn("DeviceCmdHandler not found: " + dcsName);
                            m = i18n.getString("DeviceInfo.deviceCommandsNotFound","{0} not supported", devTitles); // UserErrMsg
                        }
                        error = true;
                        uiCmd = false; // not supported / blank
                    } else
                    if (!dcHandler.deviceSupportsCommands(selDev)) {
                        Print.logWarn("ServerID does not support Device commands: " + dcsName);
                        m = i18n.getString("DeviceInfo.deviceCommandsNotSupported","{0} Commands not supported", devTitles); // UserErrMsg
                        error = true;
                        uiCmd = false; // not supported
                    } else {
                        uiCmd = true;
                    }
                }
            } else
            if (SubmitMatch(submitSms,i18n.getString("DeviceInfo.sms","SMS"))) {
                if (!allowSmsCmd) {
                    uiSms = false; // not authorized
                } else
                if (selDev == null) {
                    m = i18n.getString("DeviceInfo.pleaseSelectDevice","Please select a {0}", devTitles); // UserErrMsg
                    error = true;
                    uiSms = false; // not selected
                } else {
                    dcHandler = SMSCommandHandler;
                    if (dcHandler == null) {
                        Print.logWarn("SMS DeviceCmdHandler not found: " + currAcctID + "/" + selDevID);
                        m = i18n.getString("DeviceInfo.deviceSmsNotSupported","{0} SMS not supported", devTitles); // UserErrMsg
                        error = true;
                        uiSms = false; // not supported
                    } else
                    if (!dcHandler.deviceSupportsCommands(selDev)) {
                        Print.logWarn("SMS not supported by device: " + currAcctID + "/" + selDevID);
                        m = i18n.getString("DeviceInfo.deviceSmsNotSupported","{0} SMS not supported", devTitles); // UserErrMsg
                        error = true;
                        uiSms = false; // not supported
                    } else {
                        uiSms = true;
                    }
                }
            }
        }

        /* delete device? */
        if (deleteDevice) {
            // -- 'selDev' guaranteed non-null here
            try {
                // -- delete device
                Device.Key devKey = (Device.Key)selDev.getRecordKey();
                Print.logWarn("Deleting Device: " + devKey);
                devKey.delete(true); // will also delete dependencies
                selDevID  = "";
                selDev    = null;
                actualDev = false;
                reqState.clearDeviceList();
                // -- select another device
                devList = reqState.getDeviceIDList(true/*inclInactv*/);
                if (!ListTools.isEmpty(devList)) {
                    selDevID = devList.get(0);
                    try {
                        selDev = !selDevID.equals("")? Device.getDevice(currAcct,selDevID) : null; // null if non-existent
                    } catch (DBException dbe) {
                        // -- ignore
                    }
                }
                // -- still over limit?
                if ((overDevLimit > 0) && !currAcct.isAtMaximumDevices(allowNewDevMode == DEVMODE_ALLOW)) {
                    overDevLimit = 0; // no, no longer over limit
                }
            } catch (DBException dbe) {
                Print.logException("Deleting Device", dbe);
                m = i18n.getString("DeviceInfo.errorDelete","Internal error deleting {0}", devTitles); // UserErrMsg
                error = true;
            }
            uiList = true;
        }

        /* new device? */
        if (newDevice) {
            boolean createDeviceOK = true;
            for (int d = 0; d < devList.size(); d++) {
                if (newDeviceID.equalsIgnoreCase(devList.get(d))) {
                    m = i18n.getString("DeviceInfo.alreadyExists","This {0} already exists", devTitles); // UserErrMsg
                    error = true;
                    createDeviceOK = false;
                    break;
                }
            }
            if (createDeviceOK) {
                try {
                    // -- create device
                    Device device = Device.createNewDevice(currAcct, newDeviceID, null); // also saves
                    selDev   = device;
                    selDevID = device.getDeviceID();
                    // -- add device to Users first authorized group
                    boolean addDevToUsrGrp = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_addDeviceToUserAuthGroup,ADD_DEV_TO_USER_AUTH_GROUP);
                    if (addDevToUsrGrp && (currUser != null)) {
                        String groupID = currUser.getFirstDeviceGroupID();
                        if (!StringTools.isBlank(groupID) && !groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                            try {
                                DeviceGroup.addDeviceToDeviceGroup(currAcctID, groupID, selDevID);
                                Print.logInfo("Device '"+currAcctID+"/"+selDevID+"' added to group: "+groupID);
                            } catch (DBException dbe) {
                                Print.logException("Adding Device to DeviceGroup: " + currAcctID + "/" + groupID, dbe);
                                // -- ignore
                            }
                        }
                    }
                    // -- update device list
                    reqState.clearDeviceList();
                    devList = reqState.getDeviceIDList(true/*inclInactv*/);
                    // -- now over limit?
                    if ((overDevLimit == 0) && currAcct.isAtMaximumDevices(allowNewDevMode == DEVMODE_ALLOW)) {
                        overDevLimit = 1; // yes, now over limit
                    }
                    // -- message
                  //m = i18n.getString("DeviceInfo.createdUser","New {0} has been created", devTitles); // UserErrMsg
                    m = i18n.getString("DeviceInfo.createdDevice","New {0} has been created", devTitles); // UserErrMsg
                } catch (DBAlreadyExistsException dbaee) {
                    Print.logError("Device already exists: " + currAcct + "/" + newDeviceID);
                    m = i18n.getString("DeviceInfo.alreadyExists","This {0} already exists", devTitles); // UserErrMsg
                    error = true;
                } catch (DBException dbe) {
                    Print.logException("Creating Device", dbe);
                    m = i18n.getString("DeviceInfo.errorCreate","Internal error creating {0}", devTitles); // UserErrMsg
                    error = true;
                }
            }
            uiList = true;
        }

        /* update the device info? */
        if (updateDevice) {
            // -- 'selDev' guaranteed non-null here
            selDev.clearChanged();
            m = _updateDeviceTable(reqState);
            if (selDev.hasError()) {
                // -- stay on this page
                uiEdit = true;
            } else {
                uiList = true;
            }
        }

        /* send command */
        if (sendCommand) {
            // 'selDev' and 'dcHandler' guaranteed non-null here
            m = dcHandler.handleDeviceCommands(reqState, selDev);
            Print.logInfo("Returned Message: " + m);
            error = true;
            uiList = true;
        }

        /* update SMS */
        if (updateSms) {
            // 'selDev' and 'dcHandler' guaranteed non-null here
            m = dcHandler.handleDeviceCommands(reqState, selDev);
            Print.logInfo("Returned Message: " + m);
            error = true;
            uiList = true;
        }

        /* last event from device */
        try {
            EventData evd[] = (selDev != null)? selDev.getLatestEvents(1L,false) : null;
            if ((evd != null) && (evd.length > 0)) {
                reqState.setLastEventTime(new DateTime(evd[0].getTimestamp()));
            }
        } catch (DBException dbe) {
            // ignore
        }

        /* config */
        final boolean showPushpinChooser = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPushpinChooser,SHOW_PUSHPIN_CHOOSER);
        final boolean showDateCal        = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDateCalendar,SHOW_DATE_CALENDAR);
        final boolean showServiceID      = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showServiceID,SHOW_SERVICE_ID);

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = DeviceInfo.this.getCssDirectory();
                //WebPageAdaptor.writeCssLink(out, reqState, "DeviceInfo.css", cssDir);
                if (showDateCal)  {
                    //Calendar.writeStyle(out, reqState);
                }
                if (showPushpinChooser) {
                    //WebPageAdaptor.writeCssLink(out, reqState, "PushpinChooser.css", cssDir);
                }
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/js/DeviceInfo.js"), request);
                if (showDateCal)  {
                    Calendar.writeJavaScript(out, reqState);
                    JavaScriptTools.writeStartJavaScript(out);
                    // -- device expire
                    out.write("// Device Expiration Calendar vars \n");
                    JavaScriptTools.writeJSVar(out, "ID_DEVICE_EXPIRE", DEVICE_EXPIRE);
                    Calendar.writeNewCalendar(out, DEVICE_EXPIRE, null/*formID*/, "", null);
                    out.write(DEVICE_EXPIRE+".setYearAdvanceSelection(true);\n");
                    out.write(DEVICE_EXPIRE+".setCollapsible(true,false,true);\n");
                    // -- Share Start/End
                    out.write("// Share Start/End Calendar vars \n");
                    JavaScriptTools.writeJSVar(out, "ID_SHARE_START", SHARE_START);
                    Calendar.writeNewCalendar(out, SHARE_START, null/*formID*/, "", null);
                    out.write(SHARE_START+".setYearAdvanceSelection(true);\n");
                    out.write(SHARE_START+".setCollapsible(true,false,true);\n");
                    // -- Share End
                    JavaScriptTools.writeJSVar(out, "ID_SHARE_END", SHARE_END);
                    Calendar.writeNewCalendar(out, SHARE_END, null/*formID*/, "", null);
                    out.write(SHARE_END+".setYearAdvanceSelection(true);\n");
                    out.write(SHARE_END+".setCollapsible(true,false,true);\n");
                    // -- license expire
                    out.write("// License Expiration Calendar vars \n");
                    JavaScriptTools.writeJSVar(out, "ID_LICENSE_EXPIRE", LICENSE_EXPIRE);
                    Calendar.writeNewCalendar(out, LICENSE_EXPIRE, null/*formID*/, "", null);
                    out.write(LICENSE_EXPIRE+".setYearAdvanceSelection(true);\n");
                    out.write(LICENSE_EXPIRE+".setCollapsible(true,false,true);\n");
                    // -- last service time
                    out.write("// last Service Time Calendar vars \n");
                    JavaScriptTools.writeJSVar(out, "ID_LAST_SERVICE_DATE", LAST_SERVICE_TIME);
                    Calendar.writeNewCalendar(out, LAST_SERVICE_TIME, null/*formID*/, "", null);
                    out.write(LAST_SERVICE_TIME+".setYearAdvanceSelection(true);\n");
                    out.write(LAST_SERVICE_TIME+".setCollapsible(true,false,true);\n");
                    // -- next service time
                    out.write("// Next Service date Calendar vars \n");
                    JavaScriptTools.writeJSVar(out, "ID_NEXT_SERVICE_DATE", NEXT_SERVICE_TIME);
                    Calendar.writeNewCalendar(out, NEXT_SERVICE_TIME, null/*formID*/, "", null);
                    out.write(NEXT_SERVICE_TIME+".setYearAdvanceSelection(true);\n");
                    out.write(NEXT_SERVICE_TIME+".setCollapsible(true,false,true);\n");
                    // --
                    JavaScriptTools.writeEndJavaScript(out);
                }
                if (showPushpinChooser) {
                    PushpinChooser.writePushpinChooserJS(out, reqState, true);
                }
            }
        };

        /* Content */
        final Device  _selDev       = selDev; // may be null !!!
        final OrderedSet<String> _deviceList = devList;
        final String  _selDevID     = selDevID;
        final boolean _allowEdit    = allowEdit;
        final boolean _allowView    = allowView;
        final boolean _allowCommand = allowCommand;
        final boolean _allowSmsCmd  = allowSmsCmd;
        final int     _overDevLimit = overDevLimit;
        final boolean _allowNew     = allowNew;
        final boolean _allowDelete  = allowDelete;
        final boolean _uiCmd        = _allowCommand && uiCmd ;
        final boolean _uiSms        = _allowSmsCmd  && uiSms ;
        final boolean _uiEdit       = _allowEdit    && uiEdit;
        final boolean _uiView       = _uiEdit || uiView;
        final boolean _uiList       = uiList || (!_uiEdit && !_uiView && !_uiCmd && !_uiSms);
        HTMLOutput HTML_CONTENT     = null;
        if (_uiList) {

            final boolean _viewUniqID  = viewUniqID;
            final boolean _viewServID  = viewServID && showServiceID;
            final boolean _viewSIM     = viewSIM;
            final boolean _viewActive  = viewActive;
            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String selectURL  = DeviceInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String refreshURL = DeviceInfo.this.encodePageURL(reqState);
                  //String editURL    = DeviceInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    String newURL     = DeviceInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());

                    /* digital input state */
                    Vector<StateMaskBit> inpStateList = null;
                    for (int i = 0; i < 16; i++) {
                        // -- DeviceInfo.showInputState.01=Panic,Off,On
                        String key = PrivateLabel.PROP_DeviceInfo_showInputState_ + i;
                        String val = privLabel.getStringProperty(key,null);
                        if (StringTools.isBlank(val)) {
                            // -- not found, try again with prefixing "0"
                            if (i < 10) {
                                key = PrivateLabel.PROP_DeviceInfo_showInputState_ + "0" + i;
                                val = privLabel.getStringProperty(key,null);
                                if (StringTools.isBlank(val)) { continue; }
                            } else {
                                continue;
                            }
                        }
                        StateMaskBit smb = new StateMaskBit(i,val,"Input_",locale);
                        if (inpStateList == null) { inpStateList = new Vector<StateMaskBit>(); }
                        inpStateList.add(smb);
                        //Print.logInfo("Added InputState : " + smb);
                    }

                    /* digital output state */
                    Vector<StateMaskBit> outStateList = null;
                    for (int i = 0; i < 16; i++) {
                        // -- DeviceInfo.showOutputState.01=Panic,Off,On
                        String key = PrivateLabel.PROP_DeviceInfo_showOutputState_ + i;
                        String val = privLabel.getStringProperty(key,null);
                        if (StringTools.isBlank(val)) {
                            // -- not found, try again with prefixing "0"
                            if (i < 10) {
                                key = PrivateLabel.PROP_DeviceInfo_showOutputState_ + "0" + i;
                                val = privLabel.getStringProperty(key,null);
                                if (StringTools.isBlank(val)) { continue; }
                            } else {
                                continue;
                            }
                        }
                        StateMaskBit smb = new StateMaskBit(i,val,"Output_",locale);
                        if (outStateList == null) { outStateList = new Vector<StateMaskBit>(); }
                        outStateList.add(smb);
                        //Print.logInfo("Added OutputState : " + smb);
                    }

                    /* command state */
                    Vector<StateMaskBit> cmdStateList = null;
                    for (int i = 0; i < 16; i++) {
                        String key = PrivateLabel.PROP_DeviceInfo_showCommandState_ + i;
                        String val = privLabel.getStringProperty(key,null);
                        if (StringTools.isBlank(val)) {
                            // -- not found, try again with prefixing "0"
                            if (i < 10) {
                                key = PrivateLabel.PROP_DeviceInfo_showCommandState_ + "0" + i;
                                val = privLabel.getStringProperty(key,null);
                                if (StringTools.isBlank(val)) { continue; }
                            } else {
                                continue;
                            }
                        }
                        StateMaskBit smb = new StateMaskBit(i,val,"Command_",locale);
                        if (cmdStateList == null) { cmdStateList = new Vector<StateMaskBit>(); }
                        cmdStateList.add(smb);
                        //Print.logInfo("Added CommandState : " + smb);
                    }

                    /* show expected ACKs */
                    boolean showAcks = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showExpectedAcks,false);

                    // -- frame header
                    String frameTitle = _allowEdit?
                        i18n.getString("DeviceInfo.viewEditDevice","View/Edit {0} Information", devTitles) :
                        i18n.getString("DeviceInfo.viewDevice","View {0} Information", devTitles);
                    out.write("<h1 class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</h1>&nbsp;\n");
                    out.write("<a href='"+refreshURL+"'><span class=''>"+i18n.getString("DeviceInfo.refresh","Refresh")+"</a>\n");


                    // -- device selection table (Select, Device ID, Description, ...)
                    out.write("<h2 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("DeviceInfo.selectDevice","Select a {0}",devTitles)+":</h2>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form class='form-horizontal' name='"+FORM_DEVICE_SELECT+"' method='post' action='"+selectURL+"' target='_self'>"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SEL_DEVICE+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+" table' cellspacing='0' cellpadding='0' border='0'>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='" +CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+FilterText(i18n.getString("DeviceInfo.select","Select"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.deviceID","{0} ID",devTitles))+"</th>\n");
                    if (_viewUniqID) {
                        out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.uniqueID","Unique ID"))+"</th>\n");
                    }
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.decription","Description",devTitles))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.devEquipType","Equipment\nType"))+"</th>\n");
                    if (_viewSIM) {
                        out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.simPhoneNumber","SIM Phone#"))+"</th>\n");
                    }
                    if (_viewServID) {
                        out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.devServerID","Server ID"))+"</th>\n");
                    }
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.ignitionState","Ignition\nState"))+"</th>\n");
                    if (!ListTools.isEmpty(inpStateList)) {
                        for (StateMaskBit smb : inpStateList) {
                            out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL +"' nowrap>"+FilterText(smb.getTitle())+"</th>\n");
                        }
                    }
                    if (!ListTools.isEmpty(outStateList)) {
                        for (StateMaskBit smb : outStateList) {
                            out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL +"' nowrap>"+FilterText(smb.getTitle())+"</th>\n");
                        }
                    }
                    if (!ListTools.isEmpty(cmdStateList)) {
                        for (StateMaskBit smb : cmdStateList) {
                            out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(smb.getTitle())+"</th>\n");
                        }
                    }
                    if (showAcks) {
                        out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.ackExpected","Expecting\nACK"))+"</th>\n");
                    }
                    if (_viewActive) {
                        out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("DeviceInfo.active","Active"))+"</th>\n");
                    }
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    Device deviceRcd[] = new Device[_deviceList.size()];
                    int hasCommandHandlers = 0;
                    boolean okUpdateDevice = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_optimizeUpdateDevice,OPTIMIZE_IGNITION_STATE);
                    for (int d = 0; d < _deviceList.size(); d++) {
                        String rowClass = ((d & 1) == 0)?
                            CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD :
                            CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN;
                        out.write("  <tr class='"+rowClass+"'>\n");
                        try {
                            Device dev = Device._getDevice(currAcct, _deviceList.get(d)); // may throw DBException
                            if (dev != null) {
                                // -- will not be null if "_getDevice" is used to retrieve records
                                String  deviceID    = FilterText(dev.getDeviceID());
                                String  uniqueID    = FilterText(dev.getUniqueID());
                                String  deviceDesc  = FilterText(dev.getDescription());
                                String  equipType   = FilterText(dev.getEquipmentType());
                                String  imeiNum     = FilterText(dev.getImeiNumber());
                                String  simPhone    = FilterText(dev.getSimPhoneNumber());
                                String  devCode     = FilterText(DCServerFactory.getServerConfigDescription(dev.getDeviceCode()));
                                int     ignState    = dev.getCurrentIgnitionState(); // may update ignition state times
                                String  ignDesc     = "";
                                String  ignColor    = "black";
                                switch (ignState) {
                                    case  0:
                                        ignDesc  = FilterText(i18n.getString("DeviceInfo.ignitionOff"    , "Off"));
                                        ignColor = ColorTools.BLACK.toString(true);
                                        break;
                                    case  1:
                                        ignDesc  = FilterText(i18n.getString("DeviceInfo.ignitionOn"     , "On"));
                                        ignColor = ColorTools.GREEN.toString(true);
                                        break;
                                    default:
                                        ignDesc  = FilterText(i18n.getString("DeviceInfo.ignitionUnknown", "Unknown"));
                                        ignColor = ColorTools.DARK_YELLOW.toString(true);
                                        break;
                                }
                                String pendingACK = FilterText(dev.getExpectingCommandAck()?ComboOption.getYesNoText(locale,true):"");
                                String active     = FilterText(ComboOption.getYesNoText(locale,dev.isActive()));
                                String checked    = _selDevID.equals(dev.getDeviceID())? "checked" : "";
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+d+"'><input type='radio' name='"+PARM_DEVICE+"' id='"+deviceID+"' value='"+deviceID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+deviceID+"'>"+deviceID+"</label></td>\n");
                                if (_viewUniqID) {
                                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+uniqueID+"</td>\n");
                                }
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+deviceDesc+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+equipType+"</td>\n");
                                if (_viewSIM) {
                                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+simPhone+"</td>\n");
                                }
                                if (_viewServID) {
                                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+devCode+"</td>\n");
                                }
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap style='color:"+ignColor+"'>"+ignDesc+"</td>\n");
                                if (!ListTools.isEmpty(inpStateList)) {
                                    for (StateMaskBit smb : inpStateList) {
                                        String state_val = smb.getStateDescription(dev.getLastInputState(smb.getBitIndex()));
                                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+state_val+"</td>\n");
                                    }
                                }
                                if (!ListTools.isEmpty(outStateList)) {
                                    for (StateMaskBit smb : outStateList) {
                                        String state_val = smb.getStateDescription(dev.getLastOutputState(smb.getBitIndex()));
                                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+state_val+"</td>\n");
                                    }
                                }
                                if (!ListTools.isEmpty(cmdStateList)) {
                                    for (StateMaskBit smb : cmdStateList) {
                                        String state_val = smb.getStateDescription(dev.getCommandStateMaskBit(smb.getBitIndex()));
                                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+state_val+"</td>\n");
                                    }
                                }
                                if (showAcks) {
                                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap style='color:red'>"+pendingACK+"</td>\n");
                                }
                                if (_viewActive) {
                                    out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+active+"</td>\n");
                                }
                                // -- count command handlers
                                String serverID = dev.getDeviceCode();
                                if (StringTools.isBlank(serverID)) {
                                    // -- ignore
                                } else
                                if (DeviceInfo.this.getDeviceCommandHandler(serverID) != null) {
                                    //Print.logInfo("Found DeviceCommandHandler: " + serverID);
                                    hasCommandHandlers++;
                                } else {
                                    //Print.logInfo("DCS not found: " + serverID);
                                }
                                // -- update device ignition state?
                                if (okUpdateDevice && dev.hasChangedFieldNames()) {
                                    dev.updateOtherChangedEventFields();
                                }
                            } else {
                                // -- unlikely
                                deviceRcd[d] = null;
                            }
                        } catch (DBException dbe) {
                            deviceRcd[d] = null;
                        }
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' class='table' cellspacing='0' border='0' style=''>\n");
                    out.write("<tr>\n");
                    if (_allowView) {
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' class='btn btn-success' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("DeviceInfo.view","View")+"'>");
                        out.write("</td>\n");
                    }
                    if (_allowEdit) {
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' class='btn btn-warning' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("DeviceInfo.edit","Edit")+"'>");
                        out.write("</td>\n");
                    }
                    if (_allowCommand) {
                        if (hasCommandHandlers > 0) {
                            out.write("<td style='padding-left:5px;'>");
                            out.write("<input type='submit' class='btn btn-success' name='"+PARM_SUBMIT_PROP+"' value='"+i18n.getString("DeviceInfo.properties","Commands")+"'>");
                            out.write("</td>\n");
                        } else {
                            //Print.logWarn("No matching DeviceCommandHandlers found");
                        }
                    }
                    if (_allowSmsCmd) {  // SMS commands
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' class='btn btn-info' name='"+PARM_SUBMIT_SMS +"' value='"+i18n.getString("DeviceInfo.sms","SMS")+"'>");
                        out.write("</td>\n");
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' class='btn btn-danger' name='"+PARM_SUBMIT_DEL +"' value='"+i18n.getString("DeviceInfo.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;");
                    }
                    out.write("</td>\n");
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");


                    /* new device */
                    if (_allowNew && (_overDevLimit <= 0)) {
                        String createText = i18n.getString("DeviceInfo.createNewDevice","Create a new device");
                        if (allowNewDevMode == DEVMODE_SYSADMIN) {
                            createText += " " + i18n.getString("DeviceInfo.sysadminOnly","(System Admin only)");
                        } else
                        if (allowNewDevMode == DEVMODE_MANAGER) {
                            createText += " " + i18n.getString("DeviceInfo.managerOnly","(AccountManager only)");
                        }
                        out.write("<h2 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+createText+":</h2>\n");
                        out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                        out.write("<form name='"+FORM_DEVICE_NEW+"' method='post' class='form-horizontal' action='"+newURL+"' target='_self'>"); // target='_top'
                        out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW_DEVICE+"'/>");
                        out.write(i18n.getString("DeviceInfo.deviceID","{0} ID",devTitles)+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+" form-control' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='32'><br>\n");
                        out.write(" <input class='btn btn-success' type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("DeviceInfo.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                        out.write("</form>\n");
                        out.write("</div>\n");

                    }

                }
            };

        } else
        if (_uiEdit || _uiView) {

            final boolean _editServID  = _uiEdit && editServID && showServiceID; // fix: was '_uiView' [2.6.4-B12]
            final boolean _viewServID  = _uiView && viewServID && showServiceID;
            final boolean _editCodeVer = _uiEdit && editCodeVer;
            final boolean _viewCodeVer = _uiView && viewCodeVer;
            final boolean _editUniqID  = _uiEdit && editUniqID;
            final boolean _viewUniqID  = _uiView && viewUniqID;
            final boolean _editActive  = _uiEdit && editActive;
            final boolean _viewActive  = _uiView && viewActive;
            final boolean _editExpire  = _uiEdit && editExpire;
            final boolean _viewExpire  = _uiView && viewExpire;
            final boolean _editShare   = _uiEdit && editShare;
            final boolean _viewShare   = _uiView && viewShare;
            final boolean _editRules   = _uiEdit && editRules;
            final boolean _viewRules   = _uiView && viewRules;
            final boolean _editSMSEm   = _uiEdit && editSMSEm;
            final boolean _viewSMSEm   = _uiView && viewSMSEm;
            final boolean _editSIM     = _uiEdit && editSIM;
            final boolean _viewSIM     = _uiView && viewSIM;
            final boolean _editEqStat  = _uiEdit && editEqStat;
            final boolean _viewEqStat  = _uiView && viewEqStat;
            final boolean _editIMEI    = _uiEdit && editIMEI;
            final boolean _viewIMEI    = _uiView && viewIMEI;
            final boolean _editSERIAL  = _uiEdit && editSERIAL;
            final boolean _viewSERIAL  = _uiView && viewSERIAL;
            final boolean _editDATKEY  = _uiEdit && editDATKEY;
            final boolean _viewDATKEY  = _uiView && viewDATKEY;
            final boolean _editFuelCap = _uiEdit && editFuelCap;
            final boolean _viewFuelCap = _uiView && viewFuelCap;
            final boolean _editFuelPrf = _uiEdit && editFuelPrf;
            final boolean _viewFuelPrf = _uiView && viewFuelPrf;
            final boolean _editFuelEco = _uiEdit && editFuelEco;
            final boolean _viewFuelEco = _uiView && viewFuelEco;
            final boolean _editFuelCst = _uiEdit && editFuelCst;
            final boolean _viewFuelCst = _uiView && viewFuelCst;
            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {
                    String editURL        = DeviceInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                    boolean ntfyOK        = _viewRules && _showNotificationFields(privLabel);
                    boolean ntfyEdit      = ntfyOK && _editRules;
                    boolean subscribeOK   = _showSubscriberInfo(privLabel);
                    boolean ignOK         = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showIgnitionIndex,SHOW_IGNITION_NDX);
                    boolean ppidOK        = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPushpinID,SHOW_PUSHPIN_ID);
                    boolean dcolorOK      = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDisplayColor,SHOW_DISPLAY_COLOR);
                    boolean notesOK       = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showNotes,SHOW_NOTES);
                    boolean lastOdomOK    = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showReportedOdometer,SHOW_REPORTED_ODOM);
                    boolean lastEngHrsOK  = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showReportedEngineHours,SHOW_REPORTED_ENG_HRS);
                    boolean maintOdomOK   = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showMaintenanceOdometer,SHOW_MAINTENANCE_ODOM);
                    boolean maintHoursOK  = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showMaintenanceHours,SHOW_MAINTENANCE_HOURS);
                    boolean maintNotesOK  = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showMaintenanceNotes,SHOW_MAINTENANCE_NOTES);
                    boolean reminderOK    = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showReminderMessage,SHOW_REMINDER_MESSAGE);
                    boolean servTimeOK    = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showServiceTime,SHOW_SERVICE_TIME);
                    boolean devExpOK      = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDeviceExpirationTime,SHOW_EXPIRATION_TIME);;
                    boolean licExpOK      = true;
                    String  dataKeyStr    = privLabel.getStringProperty( PrivateLabel.PROP_DeviceInfo_showDataKey,SHOW_DATA_KEY);
                    boolean dataKeyReq    = "required".equalsIgnoreCase(dataKeyStr);
                    boolean dataKeyOK     = dataKeyReq || "true".equalsIgnoreCase(dataKeyStr);
                    boolean workHoursOK   = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showHoursOfOperation,SHOW_HOURS_OF_OPERATION);
                    boolean faultCodesOK  = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showFaultCodes,SHOW_FAULT_CODES);
                    boolean speedLimOK    = Device.hasRuleFactory(); // || privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSpeedLimit,SHOW_SPEED_LIMIT);
                    boolean fixLocOK      = (_selDev != null) && Device.supportsFixedLocation() &&
                        privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showFixedLocation,SHOW_FIXED_LOCATION);
                    boolean shareMapOK    = (_selDev != null) && Device.supportsMapShare() &&
                        privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showSharedMapDates,SHOW_MAP_SHARE_DATES);
                    boolean edServID_     = _editServID && ((_selDev == null) || (_selDev.getLastConnectTime() <= 0L)) &&
                        privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_allowEditServerID,EDIT_SERVER_ID);
                    boolean edCodeVer_    = _editCodeVer &&
                        privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_allowEditFirmwareVersion,EDIT_CODE_VERSION);
                    boolean showDCSPropID = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDcsPropertiesID,SHOW_DCS_PROPERTIES_ID);
                    boolean showDCSCfgStr = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showDcsConfigString,SHOW_DCS_CONFIG_STRING);
                    boolean showTcpSessID = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showStaticTcpSessionID,SHOW_FIXED_TCP_SESSION_ID);
                    boolean showPrefGrp   = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showPreferredGroupID,SHOW_PREFERRED_GROUP);
                    boolean showAssgnUsr  = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showAssignedUserID,SHOW_ASSIGNED_USER);
                    // --
                    boolean showFuelEcon  = DBConfig.hasExtraPackage();
                    boolean showFuelCost  = showFuelEcon;
                    boolean showFuelCap   = privLabel.getBooleanProperty(PrivateLabel.PROP_DeviceInfo_showFuelCapacity,SHOW_FUEL_CAPACITY);
                    String  showFuelProfS = privLabel.getStringProperty(PrivateLabel.PROP_DeviceInfo_showFuelLevelProfile,"");
                    boolean showFuelProf  = showFuelCap;
                    int     fuelProfType  = 1; // 0=None, 1=text, 2=select, 3=scale
                    //Print.logInfo("deviceInfo.showFuelLevelProfile: " + showFuelProfS);
                    if (StringTools.isBlank(showFuelProfS)) {
                        // -- default when blank
                        showFuelProf = showFuelCap;
                        fuelProfType = 1;      // text
                    } else
                    if (showFuelProfS.equalsIgnoreCase("false" ) ||
                        showFuelProfS.equalsIgnoreCase("no"    )   ) {
                        // -- do not show
                        showFuelProf = false;  // no display
                        fuelProfType = 0;      // none
                    } else
                    if (showFuelProfS.equalsIgnoreCase("true"  ) ||
                        showFuelProfS.equalsIgnoreCase("yes"   ) ||
                        showFuelProfS.equalsIgnoreCase("combo" ) ||
                        showFuelProfS.equalsIgnoreCase("select")   ) {
                        // -- show as ComboBox
                        showFuelProf = true;   // display
                        fuelProfType = 2;      // combo
                    } else
                    if (showFuelProfS.equalsIgnoreCase("text"  )   ) {
                        // -- show as TextField
                        showFuelProf = true;   // display
                        fuelProfType = 1;      // text
                    } else
                    if (showFuelProfS.equalsIgnoreCase("scale" )   ) {
                        showFuelProf = true;   // display
                        fuelProfType = 3;      // scale
                    } else {
                        // -- default text
                        showFuelProf = showFuelCap;
                        fuelProfType = 1;      // text
                    }

                    /* distance units description */
                    Account.DistanceUnits distUnits = Account.getDistanceUnits(currAcct);
                    String distUnitsStr = distUnits.toString(locale);

                    /* speed units description */
                    Account.SpeedUnits speedUnits = Account.getSpeedUnits(currAcct);
                    String speedUnitsStr = speedUnits.toString(locale);

                    /* volume units description */
                    Account.VolumeUnits volmUnits = Account.getVolumeUnits(currAcct);
                    String volmUnitsStr = volmUnits.toString(locale);

                    /* volume units description */
                    Account.EconomyUnits econUnits = Account.getEconomyUnits(currAcct);
                    String econUnitsStr = econUnits.toString(locale);

                    /* custom attributes */
                    Collection<String> customKeys = new OrderedSet<String>();
                    String ppAccounts[] = StringTools.parseArray(privLabel.getStringProperty(PrivateLabel.PROP_DeviceInfo_customAccounts,null));
                    if (ListTools.isEmpty(ppAccounts)) {
                        // -- no accounts specified, custom fields disabled
                    } else
                    if (ppAccounts[0].equals("*") || ListTools.contains(ppAccounts,currAcctID)) {
                        // -- either all accounts, or current account is a match
                        Collection<String> ppKeys = privLabel.getPropertyKeys(PrivateLabel.PROP_DeviceInfo_custom_);
                        for (String ppKey : ppKeys) {
                            // -- Add all custom field keys defined in the PrivateLabel properties
                            // -  "DeviceInfo.custom.DEVICEKEY" ==> "DEVICEKEY"
                            String desc = privLabel.getStringProperty(ppKey, null); // must have a non-blank description
                            if (!StringTools.isBlank(desc)) {
                                String key = ppKey.substring(PrivateLabel.PROP_DeviceInfo_custom_.length());
                                customKeys.add(key);
                            }
                        }
                    }
                    if (_selDev != null) {
                        // -- Add additional custom field keys defined in the Device table
                        customKeys.addAll(_selDev.getCustomAttributeKeys());
                    }

                    /* last connect times */
                    String lastConnectTime = (_selDev != null)? reqState.formatDateTime(_selDev.getLastTotalConnectTime()) : "";
                    if (StringTools.isBlank(lastConnectTime)) { lastConnectTime = i18n.getString("DeviceInfo.neverConnected","never"); }
                    String lastEventTime   = reqState.formatDateTime(reqState.getLastEventTime());
                    if (StringTools.isBlank(lastEventTime  )) { lastEventTime   = i18n.getString("DeviceInfo.noLastEvent"   ,"none" ); }

                    /* frame header */
                    String frameTitle = _allowEdit?
                        i18n.getString("DeviceInfo.viewEditDevice","View/Edit {0} Information", devTitles) :
                        i18n.getString("DeviceInfo.viewDevice","View {0} Information", devTitles);
                    out.write("<h1 class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</h1>\n");


                    /* start of form */
                    out.write("<form class='form-horizontal' name='"+FORM_DEVICE_EDIT+"' method='post' action='"+editURL+"' target='_self'>\n"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPD_DEVICE+"'/>\n");

                    /* Device fields */
                    ComboOption devActive = ComboOption.getYesNoOption(locale, ((_selDev != null) && _selDev.isActive()));
                    ComboOption equipStat = ComboOption.getYesNoOption(locale, ((_selDev != null) && _selDev.isActive()));
                    String firmVers  = (_selDev!=null)? _selDev.getCodeVersion() : "";
                    long   createTS  = (_selDev!=null)? _selDev.getCreationTime() : 0L;
                    String createStr = reqState.formatDateTime(createTS, "--");
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+" table' cellspacing='0' callpadding='0' border='0'>");
                    out.println(FormRow_TextField(PARM_DEVICE           , false      , i18n.getString("DeviceInfo.deviceID","{0} ID",devTitles)+":"        , _selDevID, 32, 32));
                    out.println(FormRow_TextField(PARM_CREATE_DATE      , false      , i18n.getString("DeviceInfo.creationDate","Creation Date")+":"       , createStr, 30, 30));
                    String deviceCode = (_selDev != null)? DCServerFactory.getServerConfigDescription(_selDev.getDeviceCode()) : "";
                    String autoFill = null;
                    if (StringTools.isBlank(deviceCode)) {
                        autoFill = i18n.getString("DeviceInfo.serverIdAutoFill","(automatically entered by the DCS)");
                    }
                    if (_viewServID) {
                        out.println(FormRow_TextField(PARM_SERVER_ID        , edServID_  , i18n.getString("DeviceInfo.serverID","Server ID")+":"               , deviceCode, 18, 20, autoFill));
                    }
                    if (_viewCodeVer) {
                        out.println(FormRow_TextField(PARM_CODE_VERS        , edCodeVer_ , i18n.getString("DeviceInfo.firmwareVers","Firmware Version")+":"    , firmVers, 28, 28));
                    }
                    if (_viewUniqID) {
                        out.println(FormRow_TextField(PARM_DEV_UNIQ         , _editUniqID, i18n.getString("DeviceInfo.uniqueID","Unique ID")+":"               , (_selDev!=null)?_selDev.getUniqueID():""       , 30, 30));
                    }
                    if (_viewActive) {
                        out.println(FormRow_ComboBox (PARM_DEV_ACTIVE       , _editActive, i18n.getString("DeviceInfo.active","Active")+":"                    , devActive, ComboMap.getYesNoMap(locale), ""    , -1));
                    }
                    if (devExpOK && _viewExpire) {
                        String expTitle = i18n.getString("DeviceInfo.deviceExpire","{0} Expiration",devTitles) + ":";
                        DateTime.DateStringFormat dsf = privLabel.getDateStringFormat();
                        String devExpStr = (_selDev != null)? FormatDayNumberTS(dsf,_selDev.getExpirationTime(),acctTimeZone) : "";
                        if (showDateCal) {
                            String onclick = _editExpire? "javascript:deviceToggleDevExpCalendar()" : null;
                            out.println(FormRow_TextField(DEVICE_EXPIRE, PARM_DEV_EXPIRE, _editExpire, expTitle, devExpStr, onclick, 13, 13, null));
                        } else {
                          //String extraYMD = i18n.getString("DeviceInfo.dateYMD","(yyyy/mm/dd)");
                            String extraYMD = "(" + dsf.toString() + ")";
                            out.println(FormRow_TextField(PARM_DEV_EXPIRE, _editExpire, expTitle, devExpStr, 13, 13, extraYMD));
                        }
                    }
                    // -- Device description, name, id, make , model
                    out.println(FormRow_TextField(PARM_DEV_DESC         , _uiEdit    , i18n.getString("DeviceInfo.deviceDesc","{0} Description",devTitles) +":",(_selDev!=null)?_selDev.getDescription():"" , 40, 64));
                    out.println(FormRow_TextField(PARM_DEV_NAME         , _uiEdit    , i18n.getString("DeviceInfo.displayName","Short Name") +":"          , (_selDev!=null)?_selDev.getDisplayName():""    , 16, 64));
                    out.println(FormRow_TextField(PARM_VEHICLE_ID       , _uiEdit    , i18n.getString("DeviceInfo.vehicleID","Vehicle ID") +":"            , (_selDev!=null)?_selDev.getVehicleID():""      , 24, 24));
                    out.println(FormRow_TextField(PARM_VEHICLE_MAKE     , _uiEdit    , i18n.getString("DeviceInfo.vehicleMake","Vehicle Make") +":"        , (_selDev!=null)?_selDev.getVehicleMake():""    , 40, 40));
                    out.println(FormRow_TextField(PARM_VEHICLE_MODEL    , _uiEdit    , i18n.getString("DeviceInfo.vehicleModel","Vehicle Model") +":"      , (_selDev!=null)?_selDev.getVehicleModel():""   , 40, 40));
                    // -- license plate/expire
                    out.println(FormRow_TextField(PARM_LICENSE_PLATE    , _uiEdit    , i18n.getString("DeviceInfo.licensePlate","License Plate") +":"      , (_selDev!=null)?_selDev.getLicensePlate():""   , 16, 24));
                    if (licExpOK) {
                        String licTitle = i18n.getString("DeviceInfo.licenseExpire","License Expiration") + ":";
                        DateTime.DateStringFormat dsf = privLabel.getDateStringFormat();
                        String licExpStr = (_selDev != null)? FormatDayNumber(dsf,_selDev.getLicenseExpire()) : "";
                        if (showDateCal) {
                            String onclick = _uiEdit? "javascript:deviceToggleLicExpCalendar()" : null;
                            out.println(FormRow_TextField(LICENSE_EXPIRE, PARM_LICENSE_EXPIRE, _uiEdit, licTitle, licExpStr, onclick, 13, 13, null));
                        } else {
                          //String extraYMD = i18n.getString("DeviceInfo.dateYMD","(yyyy/mm/dd)");
                            String extraYMD = "(" + dsf.toString() + ")";
                            out.println(FormRow_TextField(PARM_LICENSE_EXPIRE, _uiEdit, licTitle, licExpStr, 13, 13, extraYMD));
                        }
                    }
                    // -- Equipment type/status
                    out.println(FormRow_TextField(PARM_DEV_EQUIP_TYPE   , _uiEdit    , i18n.getString("DeviceInfo.equipmentType","Equipment Type") +":"    , (_selDev!=null)?_selDev.getEquipmentType():""  , 30, 40));
                    if (_viewEqStat) {
                        String eqTitle = i18n.getString("DeviceInfo.equipmentStatus","Equipment Status") + ":";
                        String devEqSt = (_selDev != null)? _selDev.getEquipmentStatus() : "";
                        OrderedMap<String,String> eqStatMap = Device.GetEquipmentStatusMap(locale);
                        if (!ListTools.isEmpty(eqStatMap)) {
                            ComboMap esList = new ComboMap(eqStatMap);
                            if (!esList.containsKeyIgnoreCase(devEqSt)) {
                                String desc = !StringTools.isBlank(devEqSt)? devEqSt : i18n.getString("DeviceInfo.blank","(blank)");
                                esList.insert(devEqSt, desc);
                            }
                            out.println(FormRow_ComboBox( PARM_DEV_EQUIP_STATUS, _editEqStat, eqTitle, devEqSt, esList, "", -1));
                        } else {
                            out.println(FormRow_TextField(PARM_DEV_EQUIP_STATUS, _editEqStat, eqTitle, devEqSt, 20, 20));
                        }
                    }
                    if (_viewIMEI) {
                        out.println(FormRow_TextField(PARM_DEV_IMEI         , _editIMEI  , i18n.getString("DeviceInfo.imeiNumber","IMEI/ESN Number") +":"      , (_selDev!=null)?_selDev.getImeiNumber():""     , 18, 24));
                    }
                    if (_viewSERIAL) {
                        out.println(FormRow_TextField(PARM_DEV_SERIAL_NO    , _editSERIAL, i18n.getString("DeviceInfo.serialNumber","Serial Number") +":"      , (_selDev!=null)?_selDev.getSerialNumber():""   , 20, 24));
                    }
                    if (dataKeyOK && _viewDATKEY) {
                        out.println(FormRow_TextField(PARM_DATA_KEY         , _editDATKEY, i18n.getString("DeviceInfo.dataKey","Data Key") +":"                , (_selDev!=null)?_selDev.getDataKey():""        , 60, 200));
                    }
                    if (_viewSIM) {
                        out.println(FormRow_TextField(PARM_DEV_SIMPHONE     , _editSIM   , i18n.getString("DeviceInfo.simPhoneNumber","SIM Phone#") +":"       , (_selDev!=null)?_selDev.getSimPhoneNumber():"" , 14, 18));
                    }
                    if (_viewSMSEm) {
                        out.println(FormRow_TextField(PARM_SMS_EMAIL        , _editSMSEm , i18n.getString("DeviceInfo.smsEmail","SMS Email Address") +":"      , (_selDev!=null)?_selDev.getSmsEmail():""       , 60, 60));
                    }
                    if (ppidOK) {
                        String ppDesc = i18n.getString("DeviceInfo.mapPushpinID","{0} Pushpin ID",grpTitles)+":";
                        String ppid = (_selDev != null)? _selDev.getPushpinID() : "";
                        if (showPushpinChooser) {
                            String ID_ICONSEL = "PushpinChooser";
                            String onclick    = _uiEdit? "javascript:ppcShowPushpinChooser('"+ID_ICONSEL+"')" : null;
                            out.println(FormRow_TextField(ID_ICONSEL, PARM_ICON_ID, _uiEdit, ppDesc, ppid, onclick, 32, 32, null));
                        } else {
                            ComboMap ppList = new ComboMap(reqState.getMapProviderPushpinIDs());
                            ppList.insert(""); // insert a blank as the first entry
                            out.println(FormRow_ComboBox(PARM_ICON_ID, _uiEdit, ppDesc, ppid, ppList, "", -1));
                        }
                    }
                    if (dcolorOK) {
                        String dcDesc = i18n.getString("DeviceInfo.mapRouteColor","Map Route Color")+":";
                        String dcolor = (_selDev != null)? _selDev.getDisplayColor() : "";
                        double P = 0.30;
                        ComboMap dcCombo = new ComboMap();
                        dcCombo.add(""                                        ,i18n.getString("DeviceInfo.color.default","Default"));
                        dcCombo.add(ColorTools.BLACK.toString(true)           ,i18n.getString("DeviceInfo.color.black"  ,"Black"  ));
                        dcCombo.add(ColorTools.BROWN.toString(true)           ,i18n.getString("DeviceInfo.color.brown"  ,"Brown"  ));
                        dcCombo.add(ColorTools.RED.toString(true)             ,i18n.getString("DeviceInfo.color.red"    ,"Red"    ));
                        dcCombo.add(ColorTools.ORANGE.darker(P).toString(true),i18n.getString("DeviceInfo.color.orange" ,"Orange" ));
                      //dcCombo.add(ColorTools.YELLOW.darker(P).toString(true),i18n.getString("DeviceInfo.color.yellow" ,"Yellow" ));
                        dcCombo.add(ColorTools.GREEN.darker(P).toString(true) ,i18n.getString("DeviceInfo.color.green"  ,"Green"  ));
                        dcCombo.add(ColorTools.BLUE.toString(true)            ,i18n.getString("DeviceInfo.color.blue"   ,"Blue"   ));
                        dcCombo.add(ColorTools.PURPLE.toString(true)          ,i18n.getString("DeviceInfo.color.purple" ,"Purple" ));
                        dcCombo.add(ColorTools.DARK_GRAY.toString(true)       ,i18n.getString("DeviceInfo.color.gray"   ,"Gray"   ));
                      //dcCombo.add(ColorTools.WHITE.toString(true)           ,i18n.getString("DeviceInfo.color.white"  ,"White"  ));
                        dcCombo.add(ColorTools.CYAN.darker(P).toString(true)  ,i18n.getString("DeviceInfo.color.cyan"   ,"Cyan"   ));
                        dcCombo.add(ColorTools.PINK.toString(true)            ,i18n.getString("DeviceInfo.color.pink"   ,"Pink"   ));
                        dcCombo.add("none"                                    ,i18n.getString("DeviceInfo.color.none"   ,"None"   ));
                        out.println(FormRow_ComboBox(PARM_DISPLAY_COLOR, _uiEdit, dcDesc, dcolor, dcCombo, "", -1));
                    }
                    if (ignOK) {
                        ComboMap ignList = new ComboMap(new String[] { IGNITION_notApplicable, IGNITION_ignition, IGNITION_startStop }); //,"0","1","2","3","4","5","6","7" });
                        int maxIgnNdx = privLabel.getIntProperty(PrivateLabel.PROP_DeviceInfo_maximumIgnitionIndex,7);
                        for (int igx = 0; igx <= maxIgnNdx; igx++) {
                            String igs = String.valueOf(igx);
                            ignList.add(igs, igs);
                        }
                        int ignNdx = (_selDev != null)? _selDev.getIgnitionIndex() : -1;
                        String ignSel = "";
                        if (ignNdx < 0) {
                            ignSel = IGNITION_notApplicable;
                        } else
                        if (ignNdx == StatusCodes.IGNITION_INPUT_INDEX) {
                            ignSel = IGNITION_ignition;
                        } else
                        if (ignNdx == StatusCodes.IGNITION_START_STOP) {
                            ignSel = IGNITION_startStop;
                        } else {
                            ignSel = String.valueOf(ignNdx);
                        }
                        out.println(FormRow_ComboBox( PARM_IGNITION_INDEX, _uiEdit   , i18n.getString("DeviceInfo.ignitionIndex","Ignition Input") +":" , ignSel, ignList, "", -1, i18n.getString("DeviceInfo.ignitionIndexDesc","(ignition input line, if applicable)")));
                    }
                    // -- Speed Limit
                    if (speedLimOK) {
                        double speedLimUnits = (_selDev!=null)? Account.getSpeedUnits(currAcct).convertFromKPH(_selDev.getSpeedLimitKPH()) : 0.0;
                        String speedLimStr   = StringTools.format(speedLimUnits, "0.0");
                        out.println(FormRow_TextField(PARM_DEV_SPEED_LIMIT, _uiEdit  , i18n.getString("DeviceInfo.speedLimit","Maximum Speed") +":"            , speedLimStr                                    , 10, 10, speedUnitsStr));
                    }
                    // -- Driver ID
                    out.println(FormRow_TextField(PARM_DRIVER_ID, _uiEdit, i18n.getString("DeviceInfo.driverID","Driver ID")+":", (_selDev!=null)?_selDev.getDriverID():""       , 24, 30));
                    // -- User ID
                    if (showAssgnUsr && Device.supportsAssignedUserID()) {
                        OrderedMap<String,String> userMap = new OrderedMap<String,String>();
                        userMap.put("", "---");
                        try {
                            OrderedSet<User> userList = Device.getAuthorizedUsers(_selDev);
                            for (User usr : userList) {
                                String id   = usr.getUserID();
                                String desc = usr.getDescription() + " [" +  id + "]";
                                userMap.put(id, desc);
                            }
                        } catch (DBException dbe) {
                            // -- ignore
                        }
                        ComboMap userCombo = new ComboMap(userMap);
                        String userSel = (_selDev != null)? _selDev.getAssignedUserID() : "";
                      //out.println(FormRow_TextField(PARM_USER_ID, _uiEdit, i18n.getString("DeviceInfo.userID","Assigned User ID")+":", userSel , 24, 24));
                        out.println(FormRow_ComboBox(PARM_USER_ID, _uiEdit, i18n.getString("DeviceInfo.userID","Assigned User ID")+":", userSel, userCombo, "", -1));
                    }
                    // ------------------------------------
                    //-- Fuel Section
                    // -- fuel capacity
                    int fuelItemDisplayed = 0;
                    if ((showFuelEcon && _viewFuelEco) ||
                        (showFuelCost && _viewFuelCst) ||
                        (showFuelCap  && _viewFuelCap) ||
                        (showFuelProf && _viewFuelPrf)   ) {
                        // -- add separator before odometer if maintenance is supported
                        out.println(FormRow_Separator());
                    }
                    // -- fuel economy
                    if (showFuelEcon && _viewFuelEco) {
                        double fuelEconUnits = (_selDev!=null)? econUnits.convertFromKPL(_selDev.getFuelEconomy()) : 0.0;
                        String fuelEconStr   = StringTools.format(fuelEconUnits, "0.0");
                        out.println(FormRow_TextField(PARM_DEV_FUEL_ECON   , _editFuelEco, i18n.getString("DeviceInfo.fuelEconomy","Fuel Economy") +":"        , fuelEconStr, 10, 10, econUnitsStr));
                        fuelItemDisplayed++;
                    }
                    // -- fuel cost
                    if (showFuelCost && _viewFuelCst) {
                        double unitsPerLiter = volmUnits.convertFromLiters(1.0);
                        double litersPerUnit = (unitsPerLiter > 0.0)? (1.0 / unitsPerLiter) : 0.0;
                        double costPerLiter  = (_selDev!=null)? _selDev.getFuelCostPerLiter() : 0.0;
                        double costPerUnit   = costPerLiter * litersPerUnit;
                        double fuelCostUnits = costPerUnit;
                        String fuelCostStr   = StringTools.format(fuelCostUnits, "0.00");
                        String costUnitsStr  = Account.getCurrency(currAcct) + "/" + volmUnitsStr;
                        out.println(FormRow_TextField(PARM_DEV_FUEL_COST   , _editFuelCst, i18n.getString("DeviceInfo.fuelCost","Fuel Cost") +":"              , fuelCostStr, 10, 10, costUnitsStr));
                        fuelItemDisplayed++;
                    }
                    // -- fuel capacity (tank #1/tank#2)
                    if (showFuelCap && _viewFuelCap) {
                        if (fuelItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                        // -- Tank #1
                        double fuelCap1Units = (_selDev!=null)? volmUnits.convertFromLiters(_selDev.getFuelCapacity()) : 0.0; // tank #1
                        String fuelCap1Str   = StringTools.format(fuelCap1Units, "0.0");
                        out.println(FormRow_TextField(PARM_DEV_FUEL_CAP1  , _editFuelCap, i18n.getString("DeviceInfo.fuelCapacity","Fuel Tank #1 Capacity") +":", fuelCap1Str, 10, 10, volmUnitsStr));
                        // -- Tank #2
                        double fuelCap2Units = (_selDev!=null)? volmUnits.convertFromLiters(_selDev.getFuelCapacity2()) : 0.0; // tank #2
                        String fuelCap2Str   = StringTools.format(fuelCap2Units, "0.0");
                        out.println(FormRow_TextField(PARM_DEV_FUEL_CAP2  , _editFuelCap, i18n.getString("DeviceInfo.fuelCapacity2","Fuel Tank #2 Capacity") +":", fuelCap2Str, 10, 10, volmUnitsStr));
                        fuelItemDisplayed++;
                    }
                    // -- fuel tank profile
                    if (showFuelProf && _viewFuelPrf) {
                        String profSel1 = (_selDev!=null)? _selDev.getFuelTankProfile(Device.FuelTankIndex.TANK_1) : null;
                        String profSel2 = (_selDev!=null)? _selDev.getFuelTankProfile(Device.FuelTankIndex.TANK_2) : null;
                        if (StringTools.isBlank(profSel1)) { profSel1 = FuelLevelProfile.FLP_NONE_ID; }
                        if (StringTools.isBlank(profSel2)) { profSel2 = FuelLevelProfile.FLP_NONE_ID; }
                        switch (fuelProfType) {
                            default:
                            case 0: {// "none"
                                // -- no-op (fuel tank profiles not displayed)
                                } break;
                            case 1: { // "text"
                                // -- view as TextField
                                // -- #1
                                if (fuelItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                                out.println(FormRow_TextField(PARM_DEV_FUEL_PROFILE1, _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile1","Fuel Tank #1 Profile")+":", profSel1, 80, 250));
                                fuelItemDisplayed++;
                                // -- #2
                                if (fuelItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                                out.println(FormRow_TextField(PARM_DEV_FUEL_PROFILE2, _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile2","Fuel Tank #2 Profile")+":", profSel2, 80, 250));
                                fuelItemDisplayed++;
                                } break;
                            case 2: { // "select"
                                // -- view as ComboBox
                                OrderedMap<String,String> profMap = FuelLevelProfile.GetFuelLevelProfiles(locale,true/*inclNone*/);
                                ComboMap flpCombo = new ComboMap(profMap);
                                // -- #1
                                if (fuelItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                                out.println(FormRow_ComboBox( PARM_DEV_FUEL_PROFILE1, _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile1","Fuel Tank #1 Profile")+":", profSel1, flpCombo, "", -1));
                                fuelItemDisplayed++;
                                // -- #1
                                if (fuelItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                                out.println(FormRow_ComboBox( PARM_DEV_FUEL_PROFILE2, _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile2","Fuel Tank #2 Profile")+":", profSel2, flpCombo, "", -1));
                                fuelItemDisplayed++;
                                } break;
                            case 3: { // "scale"
                                // -- view as "Linear"/"Cylider" pull-down and scale/offset text
                                OrderedMap<String,String> profMap = new OrderedMap<String,String>();
                                profMap.put(FuelLevelProfile.FLP_NONE_NAME    , i18n.getString("DeviceInfo.None"    ,"None"    ));
                                profMap.put(FuelLevelProfile.FLP_LINEAR_NAME  , i18n.getString("DeviceInfo.Linear"  ,"Linear"  ));
                                profMap.put(FuelLevelProfile.FLP_CYLINDER_NAME, i18n.getString("DeviceInfo.Cylinder","Cylinder"));
                                ComboMap flpCombo = new ComboMap(profMap);
                                // -- #1
                                {
                                String name1, scale1, offset1;
                                if (profSel1 == null) {
                                    name1   = FuelLevelProfile.FLP_NONE_NAME;
                                    scale1  = "";
                                    offset1 = "";
                                } else
                                if (profSel1.indexOf(":") < 0) {
                                    name1 = profSel1.toUpperCase();
                                    if (!name1.equals(FuelLevelProfile.FLP_LINEAR_NAME)  &&
                                        !name1.equals(FuelLevelProfile.FLP_CYLINDER_NAME)  ) {
                                        name1   = FuelLevelProfile.FLP_NONE_NAME;
                                        scale1  = "";
                                        offset1 = "";
                                    } else {
                                        scale1  = "1";
                                        offset1 = "";
                                    }
                                } else {
                                    FuelLevelProfile flp = new FuelLevelProfile(profSel1, null);
                                    name1   = flp.getID();
                                    scale1  = String.valueOf(Math.round(flp.getScale()));
                                    offset1 = String.valueOf(Math.round(flp.getOffset()));
                                }
                                if (fuelItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                                out.println(FormRow_ComboBox( PARM_DEV_FUEL_PROFILE1      , _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile1"      ,"Fuel Tank #1 Profile"       )+":", name1  , flpCombo, "", -1));
                                out.println(FormRow_TextField(PARM_DEV_FUEL_PROFILE1_SCALE, _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile1Scale" ,"Fuel Tank #1 Profile Scale" )+":", scale1 , 5, 5));
                                out.println(FormRow_TextField(PARM_DEV_FUEL_PROFILE1_OFFS , _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile1Offset","Fuel Tank #1 Profile Offset")+":", offset1, 5, 5));
                                fuelItemDisplayed++;
                                }
                                // -- #2
                                {
                                String name2, scale2, offset2;
                                if (profSel2 == null) {
                                    name2   = FuelLevelProfile.FLP_NONE_NAME;
                                    scale2  = "";
                                    offset2 = "";
                                } else
                                if (profSel2.indexOf(":") < 0) {
                                    name2 = profSel2.toUpperCase();
                                    if (!name2.equals(FuelLevelProfile.FLP_LINEAR_NAME)  &&
                                        !name2.equals(FuelLevelProfile.FLP_CYLINDER_NAME)  ) {
                                        name2   = FuelLevelProfile.FLP_NONE_NAME;
                                        scale2  = "";
                                        offset2 = "";
                                    } else {
                                        scale2  = "1";
                                        offset2 = "";
                                    }
                                } else {
                                    FuelLevelProfile flp = new FuelLevelProfile(profSel2, null);
                                    name2   = flp.getID();
                                    scale2  = String.valueOf(Math.round(flp.getScale()));
                                    offset2 = String.valueOf(Math.round(flp.getOffset()));
                                }
                                if (fuelItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                                out.println(FormRow_ComboBox( PARM_DEV_FUEL_PROFILE2      , _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile2"      ,"Fuel Tank #2 Profile"       )+":", name2  , flpCombo, "", -1));
                                out.println(FormRow_TextField(PARM_DEV_FUEL_PROFILE2_SCALE, _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile2Scale" ,"Fuel Tank #2 Profile Scale" )+":", scale2 , 5, 5));
                                out.println(FormRow_TextField(PARM_DEV_FUEL_PROFILE2_OFFS , _editFuelPrf, i18n.getString("DeviceInfo.fuelProfile2Offset","Fuel Tank #2 Profile Offset")+":", offset2, 5, 5));
                                fuelItemDisplayed++;
                                }
                                } break;
                        }
                    }
                    // ------------------------------------
                    // -- Maintenance Section
                    int maintItemDisplayed = 0;
                    if ((lastOdomOK && Device.supportsLastOdometer())      ||
                        (lastEngHrsOK && Device.supportsLastEngineHours()) ||
                        Device.supportsPeriodicMaintenance()               ||
                        (workHoursOK && Device.supportsHoursOfOperation())   ) {
                        // -- add separator before odometer if maintenance is supported
                        out.println(FormRow_Separator());
                    }
                    if (lastOdomOK && Device.supportsLastOdometer()) {
                        double odomKM   = (_selDev != null)? _selDev.getLastOdometerKM() : 0.0;
                        double offsetKM = (_selDev != null)? _selDev.getOdometerOffsetKM() : 0.0;
                        double rptOdom  = distUnits.convertFromKM(odomKM + offsetKM);
                        String odomStr  = StringTools.format(rptOdom, "0.0");
                        String ofsStr   = StringTools.format(distUnits.convertFromKM(offsetKM),"0.0");
                        String ofsText  = i18n.getString("DeviceInfo.offset","Offset");
                        String odom_ofs = distUnitsStr;
                        odom_ofs += " [" + ofsText + " " + ofsStr + "]";
                        out.println(FormRow_TextField(PARM_REPORT_ODOM, _uiEdit, i18n.getString("DeviceInfo.reportOdometer","Reported Odometer") +":" , odomStr, 10, 11, odom_ofs));
                        maintItemDisplayed++;
                    }
                    if (lastEngHrsOK && Device.supportsLastEngineHours()) {
                        double engHR    = (_selDev != null)? _selDev.getLastEngineHours() : 0.0;
                        double offsetHr = (_selDev != null)? _selDev.getEngineHoursOffset() : 0.0;
                        String hourStr  = StringTools.format(engHR + offsetHr, "0.00");
                      //String lastHrT  = i18n.getString("DeviceInfo.lastEngineHours","Last Engine Hours");
                        String ofsStr   = StringTools.format(offsetHr,"0.00");
                        String ofsText  = i18n.getString("DeviceInfo.offset","Offset");
                        String hour_ofs = i18n.getString("DeviceInfo.hours","Hours");
                        hour_ofs += " [" + ofsText + " " + ofsStr + "]";
                        out.println(FormRow_TextField(PARM_REPORT_HOURS, _uiEdit, i18n.getString("DeviceInfo.reportEngineHours","Reported Engine Hours") +":" , hourStr, 10, 11, hour_ofs));
                        maintItemDisplayed++;
                    }
                    if (Device.supportsPeriodicMaintenance()) {
                        double offsetKM = (_selDev != null)? _selDev.getOdometerOffsetKM() : 0.0;
                        double offsetHR = (_selDev != null)? _selDev.getEngineHoursOffset() : 0.0;
                        // -- Maintenance Notes
                        if (maintNotesOK) { // Domain.Properties.deviceInfo.showMaintenanceNotes=false
                            String noteText = (_selDev!=null)? StringTools.decodeNewline(_selDev.getMaintNotes()) : "";
                            out.println(FormRow_TextArea(PARM_MAINT_NOTES, _uiEdit, i18n.getString("DeviceInfo.maintNotes" ,"Maintenance Notes")+":", noteText, 3, 70));
                            maintItemDisplayed++;
                        }
                        // -- Odometer Maintenance / Interval
                        if (maintOdomOK) { // Domain.Properties.deviceInfo.showMaintenanceOdometer=false
                            for (int ndx = 0; ndx < Device.getPeriodicMaintOdometerCount(); ndx++) {
                                String ndxStr = String.valueOf(ndx + 1);
                                if (maintItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                                double lastMaintKM = (_selDev != null)? _selDev.getMaintOdometerKM(ndx) : 0.0;
                                double lastMaint   = distUnits.convertFromKM(lastMaintKM + offsetKM);
                                String lastMaintSt = StringTools.format(lastMaint, "0.0");
                                out.println(FormRow_TextField(PARM_MAINT_LASTKM_+ndx, false, i18n.getString("DeviceInfo.mainLast","Last Maintenance #{0}",ndxStr) +":" , lastMaintSt, 10, 11, distUnitsStr));
                                double intrvKM = (_selDev != null)? _selDev.getMaintIntervalKM(ndx) : 0.0;
                                double intrv   = distUnits.convertFromKM(intrvKM);
                                out.print("<tr>");
                                out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("DeviceInfo.maintInterval","Maintenance Interval #{0}",ndxStr)+":</td>");
                                out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                                out.print(Form_TextField(PARM_MAINT_INTERVKM_+ndx, _uiEdit, String.valueOf((long)intrv), 10, 11));
                                out.print("&nbsp;" + distUnitsStr);
                                if (_uiEdit) {
                                    out.print(" &nbsp;&nbsp;(" + i18n.getString("DeviceInfo.maintReset","Check to Reset Service") + " ");
                                    out.print(Form_CheckBox(PARM_MAINT_RESETKM_+ndx, PARM_MAINT_RESETKM_+ndx, _uiEdit, false, null, null));
                                    out.print(")");
                                }
                                out.print("</td>");
                                out.print("</tr>\n");
                                maintItemDisplayed++;
                            }
                        }
                        // -- EngineHours Maintenance / Interval
                        if (maintHoursOK) { // Domain.Properties.deviceInfo.showMaintenanceHours=false
                            for (int ndx = 0; ndx < Device.getPeriodicMaintEngHoursCount(); ndx++) {
                                String ndxStr = String.valueOf(ndx + 1);
                                if (maintItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                                double lastMaintHR = (_selDev != null)? _selDev.getMaintEngHoursHR(ndx) : 0.0;
                                double lastMaint   = lastMaintHR + offsetHR; // _selDev.getEngineHoursOffset()
                                String lastMaintSt = StringTools.format(lastMaint, "0.00");
                                out.println(FormRow_TextField(PARM_MAINT_LASTHR_+ndx, false, i18n.getString("DeviceInfo.maintEngHours","Last Eng Hours Maint",ndxStr) +":" , lastMaintSt, 10, 11));
                                double intrvHR = (_selDev != null)? _selDev.getMaintIntervalHR(ndx) : 0.0;
                                double intrv   = intrvHR;
                                out.print("<tr>");
                                out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("DeviceInfo.maintIntervalHR","Eng Hours Maint Interval",ndxStr)+":</td>");
                                out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                                out.print(Form_TextField(PARM_MAINT_INTERVHR_+ndx, _uiEdit, String.valueOf((long)intrv), 10, 11));
                                out.print("&nbsp;");
                                if (_uiEdit) {
                                    out.print(" &nbsp;&nbsp;(" + i18n.getString("DeviceInfo.maintResetHR","Check to Reset Service") + " ");
                                    out.print(Form_CheckBox(PARM_MAINT_RESETHR_+ndx, PARM_MAINT_RESETHR_+ndx, _uiEdit, false, null, null));
                                    out.print(")");
                                }
                                out.print("</td>");
                                out.print("</tr>\n");
                                maintItemDisplayed++;
                            }
                        }
                        // -- Reminder
                        if (reminderOK) { // Domain.Properties.deviceInfo.showReminderMessage=false
                            String remIntr = (_selDev!=null)? _selDev.getReminderInterval() : "";
                            String remText = (_selDev!=null)? StringTools.decodeNewline(_selDev.getReminderMessage()) : "";
                            if (maintItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                            out.println(FormRow_TextField(PARM_REMIND_INTERVAL, _uiEdit, i18n.getString("DeviceInfo.reminderInterval","Reminder Interval") +":", remIntr, 30, 60));
                            out.println(FormRow_TextArea( PARM_REMIND_MESSAGE , _uiEdit, i18n.getString("DeviceInfo.reminderMessage" ,"Reminder Message")+":", remText, 3, 70));
                            maintItemDisplayed++;
                        }
                        // -- Last/Next Service time
                        if (servTimeOK) {
                            DateTime.DateStringFormat dsf = privLabel.getDateStringFormat();
                            String dateFmtTxt  = privLabel.getDateStringFormatText(locale);
                            String lastServDay = (_selDev!=null)?FormatDayNumber(dsf,_selDev.getLastServiceDayNumber()):"";
                            String nextServDay = (_selDev!=null)?FormatDayNumber(dsf,_selDev.getNextServiceDayNumber()):"";
                            String lastTitle   = i18n.getString("DeviceInfo.lastServiceTime","Last Service Time")+":";
                            String nextTitle   = i18n.getString("DeviceInfo.nextServiceTime","Next Service Time")+":";
                            if (maintItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                            if (showDateCal) {
                                String lastOnclick = _uiEdit? "javascript:deviceToggleLastServiceTimeCalendar()" : null;
                                String nextOnclick = _uiEdit? "javascript:deviceToggleNextServiceTimeCalendar()" : null;
                                out.println(FormRow_TextField(LAST_SERVICE_TIME, PARM_LAST_SERVICE_TIME, _uiEdit, lastTitle, lastServDay, lastOnclick, 13, 13));
                                out.println(FormRow_TextField(NEXT_SERVICE_TIME, PARM_NEXT_SERVICE_TIME, _uiEdit, nextTitle, nextServDay, nextOnclick, 13, 13));
                            } else {
                                out.println(FormRow_TextField(PARM_LAST_SERVICE_TIME, _uiEdit, lastTitle, lastServDay, 13,13, dateFmtTxt));
                                out.println(FormRow_TextField(PARM_NEXT_SERVICE_TIME, _uiEdit, nextTitle, nextServDay, 13,13, dateFmtTxt));
                            }
                            maintItemDisplayed++;
                        }
                    } // Device.supportsPeriodicMaintenance
                    if (workHoursOK && Device.supportsHoursOfOperation()) {
                        String hooStr = (_selDev != null)? _selDev.getHoursOfOperation() : "";
                        if (maintItemDisplayed > 0) { out.println(FormRow_SubSeparator()); }
                        out.println(FormRow_TextField(PARM_HOURS_OF_OPERATION, _uiEdit, i18n.getString("DeviceInfo.hoursOfOperation","Hours of Operation")+":", hooStr, 100,140));
                        maintItemDisplayed++;
                    }
                    // ------------------------------------
                    // -- FaultCode Section
                    if (faultCodesOK && Device.supportsFaultCodes()) {
                        String fs = (_selDev != null)? _selDev.getLastFaultCode() : null;
                        String fc = !StringTools.isBlank(fs)? DTOBDFault.GetFaultString(new RTProperties(fs)) : "";
                        out.println(FormRow_Separator());
                        out.print("<tr>");
                        out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("DeviceInfo.faultCodes","Fault Codes")+":</td>");
                        out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                        out.print(Form_TextField(PARM_FAULT_CODES, false, fc, 60, 60));
                        out.print("&nbsp;");
                        if (_uiEdit) {
                            out.print(" &nbsp;&nbsp;(" + i18n.getString("DeviceInfo.faultReset","Check to Reset") + " ");
                            out.print(Form_CheckBox(PARM_FAULT_RESET, PARM_FAULT_RESET, _uiEdit, false, null, null));
                            out.print(")");
                        }
                        out.print("</td>");
                        out.print("</tr>\n");
                    }
                    // ------------------------------------
                    // -- Notification Section
                    if (ntfyOK) {
                        int neFldLen = Device.getMaximumNotifyEmailLength(); // 125
                        ComboOption allowNotfy = ComboOption.getYesNoOption(locale, ((_selDev!=null) && _selDev.getAllowNotify()));
                        out.println(FormRow_Separator());
                        out.println(FormRow_ComboBox (PARM_DEV_RULE_ALLOW, ntfyEdit  , i18n.getString("DeviceInfo.notifyAllow","Notify Enable")+":"     , allowNotfy, ComboMap.getYesNoMap(locale), "", -1));
                        out.println(FormRow_TextField(PARM_DEV_RULE_EMAIL, ntfyEdit  , i18n.getString("DeviceInfo.notifyEMail","Notify Email")+":"      , (_selDev!=null)?_selDev.getNotifyEmail():"", 95, neFldLen));
                        out.print("<tr>");
                        out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("DeviceInfo.lastAlertTime","Last Alert Time")+":</td>");
                        out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                        long lastNotifyTime = (_selDev != null)? _selDev.getLastNotifyTime() : 0L;
                        if (lastNotifyTime <= 0L) {
                            String na = i18n.getString("DeviceInfo.noAlert","n/a");
                            out.print(Form_TextField(PARM_LAST_ALERT_TIME, false, na, 10, 10));
                        } else {
                            String lastAlertTime = reqState.formatDateTime(lastNotifyTime,"--");
                            out.print(Form_TextField(PARM_LAST_ALERT_TIME, false, lastAlertTime, 24, 24));
                            if (ntfyEdit) {
                                out.print(" &nbsp;&nbsp;(" + i18n.getString("DeviceInfo.lastAlertReset","Check to Reset Alert") + " ");
                                out.print(Form_CheckBox(PARM_LAST_ALERT_RESET, PARM_LAST_ALERT_RESET, ntfyEdit, false, null, null));
                                out.print(")");
                            }
                        }
                        out.print("</td>");
                        out.print("</tr>\n");
                        // -- notify selector, subject, body ...
                        String ntfySel  = (_selDev!=null)?_selDev.getNotifySelector():"";
                        String ntfyDesc = (_selDev!=null)?_selDev.getNotifyDescription():"";
                        String ntfySubj = (_selDev!=null)?_selDev.getNotifySubject():"";
                        String ntfyText = (_selDev!=null)? StringTools.decodeNewline(_selDev.getNotifyText()) : "";
                        if (!DeviceInfo.ShowNotifySelector()) {
                            // the Device rule selector is not used, don't show
                        } else
                        if (SHOW_NOTIFY_SELECTOR           || // always show?
                            !Device.hasENRE()              || // show if non-ENRE is installed
                            !StringTools.isBlank(ntfySel)  || //   or if the selector or message is specified
                            !StringTools.isBlank(ntfySubj) ||
                            !StringTools.isBlank(ntfyText)   ) {
                            out.println(FormRow_SubSeparator());
                            String notifyRuleTitle = i18n.getString("DeviceInfo.notifyRule","Notify Rule");
                            out.println(FormRow_TextField(PARM_DEV_RULE_SEL  , ntfyEdit  , i18n.getString("DeviceInfo.ruleSelector","Rule Selector")+":"     , ntfySel  , 80, 200));
                          //out.println(FormRow_TextField(PARM_DEV_RULE_DESC , ntfyEdit  , i18n.getString("DeviceInfo.notifyDesc"  ,"Notify Description")+":", ntfyDesc , 75,  90));
                            out.println(FormRow_TextField(PARM_DEV_RULE_SUBJ , ntfyEdit  , i18n.getString("DeviceInfo.notifySubj"  ,"Notify Subject")+":"    , ntfySubj , 75,  90));
                            out.println(FormRow_TextArea( PARM_DEV_RULE_TEXT , ntfyEdit  , i18n.getString("DeviceInfo.notifyText"  ,"Notify Message")+":"    , ntfyText ,  5,  70));
                            //if (privLabel.hasEventNotificationEMail()) {
                            //    boolean editWrap = ntfyEdit; // && privLabel.hasEventNotificationEMail()
                            //    boolean useEMailWrapper = (_selDev!=null) && _selDev.getNotifyUseWrapper();
                            //    ComboOption wrapEmail = ComboOption.getYesNoOption(locale, useEMailWrapper);
                            //    out.println(FormRow_ComboBox(PARM_DEV_RULE_WRAP, editWrap, i18n.getString("DeviceInfo.notifyWrap" ,"Notify Use Wrapper")+":", wrapEmail, ComboMap.getYesNoMap(locale), "", -1, i18n.getString("DeviceInfo.seeEventNotificationEMail","(See 'EventNotificationEMail' tag in 'private.xml')")));
                            //} else {
                            //    Print.logInfo("PrivateLabel EventNotificationEMail Subject/Body not defined");
                            //}
                        }

                    }
                    // -- Active Corridor
                    if (Device.hasENRE() && Device.supportsActiveCorridor()) {
                        String actvGC   = (_selDev != null)? _selDev.getActiveCorridor() : "";
                        String gcTitle  = i18n.getString("DeviceInfo.activeCorridor","Active Corridor") + ":";
                        String gcList[] = Device.getCorridorIDsForAccount(reqState.getCurrentAccountID());
                        out.println(FormRow_Separator());
                        if (gcList != null) {
                            ComboMap gcCombo = new ComboMap(gcList);
                            gcCombo.insert("", i18n.getString("DeviceInfo.corridorNone","(none)"));
                            out.println(FormRow_ComboBox( PARM_ACTIVE_CORRIDOR, _uiEdit, gcTitle, actvGC, gcCombo, "", -1));
                        } else {
                            out.println(FormRow_TextField(PARM_ACTIVE_CORRIDOR, _uiEdit, gcTitle, actvGC, 20, 20));
                        }
                    }
                    // -- BorderCrossing
                    if (acctBCEnabled && Device.supportsBorderCrossing()) {
                        long      bcTime  = (_selDev != null)? _selDev.getLastBorderCrossTime() : 0L;
                        String    bcTimeS = reqState.formatDateTime(bcTime, "--");
                        int       bcState = (_selDev != null)? _selDev.getBorderCrossing() : Device.BorderCrossingState.OFF.getIntValue();
                        boolean   bcOK    = (bcState == Device.BorderCrossingState.ON.getIntValue())? true : false;
                        ComboOption bcOpt = ComboOption.getYesNoOption(locale, bcOK);
                        out.println(FormRow_Separator());
                        out.println(FormRow_ComboBox (PARM_BORDER_CROSS_ENAB, _uiEdit, i18n.getString("DeviceInfo.borderCrossingEnabled","Border Crossing Enable")+":", bcOpt, ComboMap.getYesNoMap(locale), "", -1));
                        out.println(FormRow_TextField(PARM_BORDER_CROSS_TIME, false  , i18n.getString("DeviceInfo.borderCrossingTime"   ,"Border Crossing Time"  )+":", bcTimeS, 30, 30));
                    }
                    // -- WorkOrder info
                    /*
                    if (_showWorkOrderID(privLabel)) {
                        String actvWO = (_selDev != null)? _selDev.getWorkOrderID() : "";
                        String woTitle = i18n.getString("DeviceInfo.workOrderID","Work Order ID") + ":";
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_WORKORDER_ID     , _uiEdit, woTitle, actvWO, 20, 20));
                    }
                    */
                    // -- Device URL links
                    if (Device.supportsLinkURL()) {
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_LINK_URL         , _uiEdit, i18n.getString("DeviceInfo.linkURL"        ,"Link URL")         +":" , (_selDev!=null)?String.valueOf(_selDev.getLinkURL()):""        , 60, 70));
                        out.println(FormRow_TextField(PARM_LINK_DESC        , _uiEdit, i18n.getString("DeviceInfo.linkDescription","Link Description") +":" , (_selDev!=null)?String.valueOf(_selDev.getLinkDescription()):"", 10, 24));
                    }
                    // -- Fixed location
                    if (fixLocOK) {
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_FIXED_LAT        , _uiEdit, i18n.getString("DeviceInfo.fixedLatitude" ,"Fixed Latitude")     +":"  , (_selDev!=null)?String.valueOf(_selDev.getFixedLatitude()) :"0.0",  9, 10));
                        out.println(FormRow_TextField(PARM_FIXED_LON        , _uiEdit, i18n.getString("DeviceInfo.fixedLongitude","Fixed Longitude")    +":"  , (_selDev!=null)?String.valueOf(_selDev.getFixedLongitude()):"0.0", 10, 11));
                        out.println(FormRow_TextField(PARM_FIXED_ADDRESS    , _uiEdit, i18n.getString("DeviceInfo.fixedAddress"  ,"Fixed Address")      +":"  , (_selDev!=null)?_selDev.getFixedAddress():""                     , 70, 80));
                        out.println(FormRow_TextField(PARM_FIXED_PHONE      , _uiEdit, i18n.getString("DeviceInfo.fixedPhone"    ,"Fixed Contact Phone")+":"  , (_selDev!=null)?_selDev.getFixedContactPhone():""                , 30, 40));
                    }
                    // -- Shared Map start/end times
                    if (shareMapOK) {
                        out.println(FormRow_Separator());
                        DateTime.DateStringFormat dsf = privLabel.getDateStringFormat();
                      //String extraYMD   = i18n.getString("DeviceInfo.dateYMD", "(yyyy/mm/dd)");
                        String extraYMD   = "(" + dsf.toString() + ")";
                        String startTitle = i18n.getString("DeviceInfo.shareMapStartDay","ShareMap Start Day") + ":";
                        String endTitle   = i18n.getString("DeviceInfo.shareMapEndDay"  ,"ShareMap End Day") + ":";
                        String startStr   = (_selDev != null)? FormatDayNumberTS(dsf,_selDev.getMapShareStartTime(),acctTimeZone) : "";
                        String endStr     = (_selDev != null)? FormatDayNumberTS(dsf,_selDev.getMapShareEndTime()  ,acctTimeZone) : "";
                        if (showDateCal) {
                            String onclickStart = _editShare? "javascript:deviceToggleShareStartCalendar()" : null;
                            out.println(FormRow_TextField(SHARE_START, PARM_SHARE_START, _editShare, startTitle, startStr, onclickStart, 13, 13, extraYMD));
                            String onclickEnd   = _editShare? "javascript:deviceToggleShareEndCalendar()" : null;
                            out.println(FormRow_TextField(SHARE_END  , PARM_SHARE_END  , _editShare, endTitle  , endStr  , onclickEnd  , 13, 13, extraYMD));
                        } else {
                            out.println(FormRow_TextField(PARM_SHARE_START, _editShare, startTitle, startStr, 13, 13, extraYMD));
                            out.println(FormRow_TextField(PARM_SHARE_END  , _editShare, endTitle  , endStr  , 13, 13, extraYMD));
                        }
                    }
                    // -- Subscriber Section
                    if (subscribeOK) {
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_SUBSCRIBE_ID    , _uiEdit, i18n.getString("DeviceInfo.subscriberID"    ,"Subscriber ID")+":"     , _selDev.getSubscriberID()    , 24, 24));
                        out.println(FormRow_TextField(PARM_SUBSCRIBE_NAME  , _uiEdit, i18n.getString("DeviceInfo.subscriberName"  ,"Subscriber Name")+":"   , _selDev.getSubscriberName()  , 40, 40));
                        out.println(FormRow_TextField(PARM_SUBSCRIBE_AVATAR, _uiEdit, i18n.getString("DeviceInfo.subscriberAvatar","Subscriber Avatar")+":" , _selDev.getSubscriberAvatar(), 80, 80));
                    }
                    // -- Last connect time
                    if (SHOW_LAST_CONNECT) {
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_DEV_LAST_CONNECT , false  , i18n.getString("DeviceInfo.lastConnect","Last Connect")+":" , lastConnectTime, 30, 30, i18n.getString("DeviceInfo.serverTime","(Server time)"))); // read-only
                        out.println(FormRow_TextField(PARM_DEV_LAST_EVENT   , false  , i18n.getString("DeviceInfo.lastEvent"  ,"Last Event"  )+":" , lastEventTime  , 30, 30, i18n.getString("DeviceInfo.deviceTime","(Device time)"))); // read-only
                    }
                    // -- Notes
                    if (notesOK) {
                        String noteText = (_selDev!=null)? StringTools.decodeNewline(_selDev.getNotes()) : "";
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextArea(PARM_DEV_NOTES, _uiEdit, i18n.getString("DeviceInfo.notes" ,"General Notes")+":", noteText, 5, 70));
                    }
                    // -- Custom attributes/fields
                    if (!ListTools.isEmpty(customKeys)) {
                        out.println(FormRow_Separator());
                        for (String key : customKeys) {
                            // -- PrivateLabel defines description, Device defined value
                            String desc  = privLabel.getStringProperty(PrivateLabel.PROP_DeviceInfo_custom_ + key, key);
                            String value = (_selDev != null)? _selDev.getCustomAttribute(key) : "";
                            out.println(FormRow_TextField(PARM_DEV_CUSTOM_ + key, _uiEdit, desc + ":", value, 40, 50));
                        }
                    }
                    out.println(FormRow_Separator());

                    /* DCS propID / config string / TCP Session ID */
                    if (showDCSPropID || showDCSCfgStr || showTcpSessID) {
                        // -- properties ID
                        if (showDCSPropID) {
                            String propID = (_selDev != null)? _selDev.getDcsPropertiesID() : "";
                            String gnID = !StringTools.isBlank(propID)? propID : DCServerConfig.DEFAULT_PROP_GROUP_ID;
                            DCServerConfig dcsc = (_selDev != null)? _selDev.getDCServerConfig() : null;
                            Set<String> gnList = (dcsc != null)? dcsc.getPropertyGroupNames() : null;
                            String desc = i18n.getString("DeviceInfo.dcsPropertiesID" ,"DCS Properties ID");
                            if (gnList != null) {
                                ComboMap gnCM = new ComboMap(gnList);
                                if (!gnList.contains(gnID)) {
                                    gnCM.insert(gnID);
                                }
                                out.println(FormRow_ComboBox(PARM_DCS_PROPS_ID, _uiEdit, desc+":", gnID, gnCM, "", 20));
                            } else {
                                out.println(FormRow_TextField(PARM_DCS_PROPS_ID, _uiEdit, desc+":", gnID, 20, 32));
                            }
                        }
                        // -- configuration string
                        if (showDCSCfgStr) {
                            String desc  = i18n.getString("DeviceInfo.dcsConfigString","DCS Configuration String");
                            String value = (_selDev != null)? _selDev.getDcsConfigString() : "";
                            out.println(FormRow_TextField(PARM_DCS_CONFIG_STR, _uiEdit, desc+":", value, 50, 78));
                        }
                        // -- static TCP session ID
                        if (showTcpSessID) {
                            String desc  = i18n.getString("DeviceInfo.tcpSessionID","Static TCP Session ID");
                            String value = (_selDev != null)? _selDev.getFixedTcpSessionID() : "";
                            out.println(FormRow_TextField(PARM_TCP_SESSION_ID, _uiEdit, desc+":", value, 32, 32));
                        }
                        // --
                        out.println(FormRow_Separator());
                    }

                    /* Enable Driver ELogs */
                    if ((_selDev != null) && Account.IsELogEnabled(_selDev.getAccount())) {
                        // -- "Enable Driver ELogs":
                        boolean elogEnabled  = _selDev.getELogEnabled();
                        ComboOption elogOpt  = ComboOption.getYesNoOption(locale, elogEnabled);
                        ComboMap    elogMap  = ComboMap.getYesNoMap(locale);
                        String      elogDesc = null;
                        out.println(FormRow_ComboBox(PARM_DEV_ENABLE_ELOG, _uiEdit, i18n.getString("DeviceInfo.enableDriverELogs","Enable Driver ELogs")+":", elogOpt, elogMap, "", -1, elogDesc));
                        out.println(FormRow_Separator());
                    } else {
                        //Print.logInfo("HOS/ELog not enabled");
                    }

                    /* preferred group-id */
                    if (showPrefGrp) {
                        boolean includeAll = false;
                        ComboMap grpMap = new ComboMap(reqState.createGroupDescriptionMap(true/*includeID*/,includeAll));
                        if (!includeAll) {
                            grpMap.insert("", i18n.getString("DeviceInfo.noGroup" ,"No {0}",grpTitles));
                        }
                        String grpSel = (_selDev != null)? _selDev.getGroupID() : "";
                        out.println(FormRow_ComboBox(PARM_GROUP_ID, _uiEdit, i18n.getString("DeviceInfo.groupID","Preferred {0}",grpTitles)+":", grpSel, grpMap, "", -1));
                        out.println(FormRow_SubSeparator());
                    }

                    /* end of field table */
                    out.println("</table>");

                    /* DeviceGroup membership */
                    final OrderedSet<String> grpList = reqState.getDeviceGroupIDList(true/*includeAll*/);
                    out.write("<span style='margin-left:4px; margin-top:10px; font-weight:bold;'>");
                    out.write(  i18n.getString("DeviceInfo.groupMembership","{0} Membership:",grpTitles));
                    out.write(  "</span>\n");
                    out.write("<div style=' margin: 2px 20px 5px 10px; height:80px; width:400px; overflow-x: hidden; overflow-y: scroll;'>\n");
                    out.write("<table>\n");
                    for (int g = 0; g < grpList.size(); g++) {
                        String grpID = grpList.get(g);
                        String pname = PARM_DEV_GROUP_ + grpID;
                        String desc  = reqState.getDeviceGroupDescription(grpID,false/*!rtnDispName*/);
                        if (!desc.equals(grpID)) { desc += " ["+grpID+"]"; }
                        out.write("<tr>");
                        out.write("<td><label for='"+grpID+"'>"+FilterText(desc)+" : </label></td>"); // [2.6.3-B21]
                        out.write("<td>");
                        if (grpID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                            out.write(Form_CheckBox(null,pname,false,true,null,null));
                        } else {
                            boolean devInGroup = (_selDev != null)?
                                DeviceGroup.isDeviceInDeviceGroup(_selDev.getAccountID(), grpID, _selDevID) :
                                false;
                            out.write(Form_CheckBox(grpID,pname,_uiEdit,devInGroup,null,null));
                        }
                        out.write("</td>");
                        out.write("</tr>\n");
                    }
                    out.write("</table>\n");
                    out.write("</div>\n");

                    /* end of form */

                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_uiEdit) {
                        out.write("<input type='submit' 'btn btn-warning' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("DeviceInfo.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' class='btn btn-default' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("DeviceInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    } else {
                        out.write("<input type='button' class='btn btn-default' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("DeviceInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    }
                    out.write("</form>\n");

                }
            };

        } else
        if (_uiCmd) {

            final boolean _editProps = _allowCommand && (selDev != null);
            final DeviceCmdHandler _dcHandler = dcHandler; // non-null here
            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {

                    /* frame title */
                    String frameTitle = i18n.getString("DeviceInfo.setDeviceProperties","({0}) Set {1} Properties",
                        _dcHandler.getServerDescription(), devTitles[0]);
                    out.write("<h1 class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</h1>\n");

                    /* Device Command/Properties content */
                    String editURL = DeviceInfo.this.encodePageURL(reqState);//, RequestProperties.TRACK_BASE_URI());
                    _dcHandler.writeCommandForm(out, reqState, _selDev, editURL, _editProps);

                }
            };
        } else
        if (_uiSms) {

            final boolean _editSmsCmd = _allowSmsCmd && (selDev != null);
            final DeviceCmdHandler _dcHandler = dcHandler; // non-null here
            HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
                public void write(PrintWriter out) throws IOException {

                    /* frame title */
                    String frameTitle = i18n.getString("DeviceInfo.setDeviceSMS","({0}) Send {1} SMS",
                        _dcHandler.getServerDescription(), devTitles[0]);
                    out.write("<h1 class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</h1>\n");

                    /* Device Command/Properties content */
                    String editURL = DeviceInfo.this.encodePageURL(reqState);//, RequestProperties.TRACK_BASE_URI());
                    _dcHandler.writeCommandForm(out, reqState, _selDev, editURL, _editSmsCmd);

                }
            };

        }

        /* write frame */
        String onloadAlert = error? JS_alert(true,m) : null;
        String onload = (_uiCmd || _uiSms)? "javascript:devCommandOnLoad();" : onloadAlert;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------
}
