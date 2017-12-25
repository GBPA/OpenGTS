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
//  2008/06/20  Martin D. Flynn
//     -Initial release
//  2009/05/24  Martin D. Flynn
//     -Changed "addDBFields" method 'alwaysAdd' to 'defaultAdd' to only add fields 
//      if not explicitly specified in the runtime conf file.
//  2010/09/09  Martin D. Flynn
//     -Added unit conversion for "getOptionalEventField".
//  2011/08/21  Martin D. Flynn
//     -Modified optional group/device map field specification.
//  2015/02/06  Martin D. Flynn
//     -Added DNS caching timeout runtime configuration (see DNS_CACHE_TIMEOUT).
// ----------------------------------------------------------------------------
package org.opengts;

import java.util.Vector;
import java.util.Locale;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.geocoder.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.db.dmtp.*;

/**
*** Provides startup initialization.<br>
*** This class is loaded by <code>DBConfig.java</code> at startup initialization time, and 
*** various methods are called within this class to allow custom DB initialization.<br>
*** The actual class loaded and executed by <code>DBConfig</code> can be overridden by placing 
*** the following line in the system 'default.conf' and 'webapp.conf' files:
*** <pre>
***   startup.initClass=org.opengts.StartupInit
*** </pre>
*** Where 'org.opengts.opt.StartupInit' is the name of the class you wish to have loaded in
*** place of this class file.
**/

public class StartupInit
    // -- standard/parent StartupInit class
    implements DBConfig.DBInitialization, DBFactory.CustomFactoryHandler
{

    // ------------------------------------------------------------------------

    /* local property keys */
    private static String      PROP_RuleFactory_class           = "RuleFactory.class";
    private static String      PROP_PasswordHandler_class       = "PasswordHandler.class";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean            didInitRuleFactory               = false;
    private RuleFactory        ruleFactoryInstance              = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor.<br>
    *** (Created with the DBConfig db startup initialization)
    **/
    public StartupInit()
    {
        super(); // <-- Object

        /* set DNS cache timeout */
        {
            int dnsTmoSec = RTConfig.getInt(RTKey.DNS_CACHE_TIMEOUT, -999);
            if (dnsTmoSec >= 0) {
                // -- "-1" is a valid value (meaning "cache forever"), however this is already the 
                // -  default so we ignore anyting less-than 0 here.
                try {
                    // -- Google "networkaddress.cache.ttl" for more info
                    java.security.Security.setProperty("networkaddress.cache.ttl", String.valueOf(dnsTmoSec));
                    Print.logDebug("Set DNS caching timeout: " + dnsTmoSec + " seconds");
                    // -- this value can also be set in the "$JAVA_HOME\lib\security" file
                } catch (SecurityException se) {
                    // -- denied by Security Manager
                    Print.logWarn("Unable to set DNS caching timeout: " + se);
                } catch (Throwable th) {
                    // -- anything else
                    Print.logWarn("Unable to set DNS caching timeout: " + th);
                }
            }
        }

        /* set a default "User-Agent" in the config file properties (if not already present) */
        {
            RTProperties cfgFileProps = RTConfig.getConfigFileProperties();
            String userAgent = cfgFileProps.getString(RTKey.HTTP_USER_AGENT, null, false);
            if (StringTools.isBlank(userAgent)) {
                // -- no default "http.userAgent" defined in the config-file properties
                cfgFileProps.setString(RTKey.HTTP_USER_AGENT, "OpenGTS/" + org.opengts.Version.getVersion());
            }
            //Print.logInfo("HTTP User-Agent set to '%s'", HTMLTools.getHttpUserAgent());
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DBConfig.DBInitialization interface

    /**
    *** Pre-DBInitialization.<br>
    *** This method is called just before the standard database factory classes are initialized/added.
    **/
    public void preInitialization()
    {
        if (RTConfig.isWebApp()) {

            /* version */
            Print.logInfo("Version: " + Version.getVersion());

            /* current GTS_HOME */
            String GTS_HOME = System.getenv(DBConfig.env_GTS_HOME);
            Print.logInfo("GTS_HOME: " + StringTools.blankDefault(GTS_HOME,"not defined"));

            /* current memory usage */
            OSTools.printMemoryUsage();

        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Opportunity to add custom DBFactory classes.<br>
    *** This method is called just after all standard database factory classes have been intialized/added.
    *** Additional database factories that are needed for the custom installation may be added here.
    **/
    public void addTableFactories()
    {

        /* MUST add standard DBFactories */
        DBConfig.addTableFactories();

        /* add custom DBFactories here */
        //DBAdmin.addTableFactory("com.example.db.tables.MyCustomTable", true);

        /* add custom RuleFactory */
        // See "RuleFactoryExample.java" for more information
        if (!Device.hasRuleFactory()) {
            // -- To add the RuleFactoryExample module:
            // -   Device.setRuleFactory(new RuleFactoryExample());
            // -- To add a different customized RuleFactory implementation:
            // -   Device.setRuleFactory(new org.opengts.extra.rule.RuleFactoryLite());
            RuleFactory rf = this._getRuleFactoryInstance();
            if (rf != null) {
                Device.setRuleFactory(rf);
                Print.logInfo("RuleFactory installed: " + StringTools.className(rf));
            }
        }

        /* add custom map event data handler */
        String deviceMapFlds[] = RTConfig.getStringArray(OptionalEventFields.PROP_OptionalEventFields_DeviceMap, null);
        String fleetMapFlds[]  = RTConfig.getStringArray(OptionalEventFields.PROP_OptionalEventFields_FleetMap , null);
        OptionalEventFields optEvFlds = OptionalEventFields.createOptionalEventFieldsHandler(deviceMapFlds,fleetMapFlds);
        EventUtil.setOptionalEventFieldHandler(optEvFlds);
        Print.logDebug("Installed OptionalEventFieldHandler: " + StringTools.className(optEvFlds));

    }
    
    // --------------------------------
    
    private RuleFactory _getRuleFactoryInstance()
    {
        
        /* already initialized? */
        if (this.ruleFactoryInstance != null) {
            return this.ruleFactoryInstance;
        } else
        if (this.didInitRuleFactory) {
            return null;
        }
        this.didInitRuleFactory = true;

        /* get RuleFactory class */
        Class<?> rfClass   = null;
        String rfClassName = RTConfig.getString(PROP_RuleFactory_class,null);
        try {
            String rfcName = !StringTools.isBlank(rfClassName)?
                rfClassName : 
                (DBConfig.PACKAGE_EXTRA_ + "rule.RuleFactoryLite");
            rfClass     = Class.forName(rfcName);
            rfClassName = rfcName;
        } catch (Throwable th) {
            if (!StringTools.isBlank(rfClassName)) {
                Print.logException("Unable to locate RuleFactory class: " + rfClassName, th);
            }
            return null;
        }

        /* instantiate RuleFactory */
        try {
            this.ruleFactoryInstance = (RuleFactory)rfClass.newInstance();
            return this.ruleFactoryInstance;
        } catch (Throwable th) {
            Print.logException("Unable to instantiate RuleFactory: " + rfClassName, th);
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Post-DBInitialization.<br>
    *** This method is called after all startup initialization has completed.
    **/
    public void postInitialization()
    {

        /* init StatusCode descriptions */
        StatusCodes.initStatusCodes(null); // include all status codes
        /* //The following specifies the list of specific status codes to include:
        StatusCodes.initStatusCodes(new int[] {
            StatusCodes.STATUS_LOCATION,
            StatusCodes.STATUS_MOTION_START,
            StatusCodes.STATUS_MOTION_IN_MOTION,
            StatusCodes.STATUS_MOTION_STOP,
            StatusCodes.STATUS_MOTION_DORMANT,
            ... include other StatusCodes here ...
        });
        */

        /* This sets the description for all accounts, all 'private.xml' domains, and all Localizations. */
        //StatusCodes.SetDescription(StatusCodes.STATUS_LOCATION      , "Marker");
        //StatusCodes.SetDescription(StatusCodes.STATUS_MOTION_START  , "Start Point");
        //StatusCodes.SetDescription(StatusCodes.STATUS_MOTION_STOP   , "Stop Point");

        /* Install custom PasswordHandler */
        this.initPasswordHandler();

    }
    
    protected void initPasswordHandler()
    {
        String phClassName = RTConfig.getString(PROP_PasswordHandler_class,null);
        if (StringTools.isBlank(phClassName)) {
            // -- ignore
        } else
        if (phClassName.equalsIgnoreCase("md5")) {
            RTProperties rtp = new RTProperties();
            rtp.setString(GeneralPasswordHandler.PROP_passwordEncoding, "md5");
            GeneralPasswordHandler pwh = new GeneralPasswordHandler(rtp);
            Account.setDefaultPasswordHandler(pwh);
        } else
        if (phClassName.equalsIgnoreCase("md5plain")) {
            RTProperties rtp = new RTProperties();
            rtp.setString(GeneralPasswordHandler.PROP_passwordEncoding, "md5plain");
            GeneralPasswordHandler pwh = new GeneralPasswordHandler(rtp);
            Account.setDefaultPasswordHandler(pwh);
        } else
        if (phClassName.equalsIgnoreCase("default")) {
            GeneralPasswordHandler pwh = new GeneralPasswordHandler();
            Account.setDefaultPasswordHandler(pwh);
        } else {
            try {
                Class<?> phClass = Class.forName(phClassName);
                PasswordHandler pwh = (PasswordHandler)phClass.newInstance();
                Account.setDefaultPasswordHandler(pwh);
            } catch (Throwable th) { // ClassCastException, ClassNotFoundException, ...
                Print.logException("Unable to instantiate PasswordHandler: " + phClassName, th);
            }
        }
        //Print.logDebug("Default PasswordHandler: " + Account.getDefaultPasswordHandler());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DBFactory.CustomFactoryHandler interface

    /**
    *** Create a DBFactory instance.  The DBFactory initialization process will call this method
    *** when creating a DBFactory for a given table, allowing this class to override/customize
    *** any specific table attributes.  If this method returns null, the default table DBFactory
    *** will be created.
    *** @param tableName  The name of the table
    *** @param field      The DBFields in the table
    *** @param keyType    The table key type
    *** @param rcdClass   The DBRecord subclass representing the table
    *** @param keyClass   The DBRecordKey subclass representing the table key
    *** @param editable   True if this table should be editable, false otherwise.  
    ***                   This value is used by the GTSAdmin application.
    *** @param viewable   True if this table should be viewable, false otherwise.  
    ***                   An 'editable' table is automatically considered viewable.
    ***                   This value is used by the GTSAdmin application.
    *** @return The DBFactory instance (or null to indicate that the default DBFactory should be created).
    ***/
    public <T extends DBRecord<T>> DBFactory<T> createDBFactory(
        String tableName, 
        DBField field[], 
        DBFactory.KeyType keyType, 
        Class<T> rcdClass, 
        Class<? extends DBRecordKey<T>> keyClass, 
        boolean editable, boolean viewable)
    {
        //Print.logInfo("Intercept creation of DBFactory: %s", tableName);
        return null; // returning null indicates default behavior
    }

    /**
    *** Augment DBFactory fields.  This method is called before fields have been added to any
    *** given DBFactory.  This method may alter the list of DBFields by adding new fields, or 
    *** altering/deleting existing fields.  However, deleting/altering fields that have other
    *** significant systems dependencies may cause unpredictable behavior.
    *** @param factory  The DBFactory
    *** @param fields   The list of fields scheduled to be added to the DBFactory
    *** @return The list of fields which will be added to the DBFactory
    **/
    public <T extends DBRecord<T>> java.util.List<DBField> selectFields(DBFactory<T> factory, java.util.List<DBField> fields)
    {
        String tblName = factory.getUntranslatedTableName();
        // -- These additional fields can be enabled by placing the appropriate/specified 
        // -  property "<key>=true" in a 'custom.conf' file.

        /* Account */
        if (tblName.equalsIgnoreCase(Account.TABLE_NAME())) {
            // -- startupInit.Account.AddressFieldInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_AddressFieldInfo               , false, Account.AddressFieldInfo);
            // -- startupInit.Account.MapLegendFieldInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_MapLegendFieldInfo             , false, Account.MapLegendFieldInfo);
            // -- startupInit.Account.AccountManagerInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_AccountManagerInfo             , false, Account.AccountManagerInfo);
            // -- startupInit.Account.DataPushInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_DataPushInfo                   , false, Account.DataPushInfo);
            // startupInit.Account.ELogHOSInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_ELogHOSInfo                    , false, Account.ELogHOSInfo);
            // startupInit.Account.PlatinumInfo=true
            addDBFields(tblName, fields, Account.OPTCOLS_PlatinumInfo                   , false, Account.PlatinumInfo);
            return fields;
        }

        /* User */
        if (tblName.equalsIgnoreCase(User.TABLE_NAME())) {
            // -- startupInit.User.AddressFieldInfo=true
            addDBFields(tblName, fields, User.OPTCOLS_AddressFieldInfo                  , false, User.AddressFieldInfo);
            // -- startupInit.User.ExtraFieldInfo=true
            addDBFields(tblName, fields, User.OPTCOLS_ExtraFieldInfo                    , false, User.ExtraFieldInfo);
            // -- startupInit.User.PlatinumInfo=true
            addDBFields(tblName, fields, User.OPTCOLS_PlatinumInfo                      , false, User.PlatinumInfo);
            return fields;
        }

        /* Device */
        if (tblName.equalsIgnoreCase(Device.TABLE_NAME())) {
            // -- startupInit.Device.OpenDMTPFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_OpenDMTPFieldInfo               , false, Device.OpenDMTPFieldInfo);
            // -- startupInit.Device.NotificationFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_NotificationFieldInfo           , false, Device.NotificationFieldInfo);
            // -- startupInit.Device.GeoCorridorFieldInfo=true
            boolean devGC = DBConfig.hasRulePackage();
            addDBFields(tblName, fields, Device.OPTCOLS_GeoCorridorFieldInfo            , devGC, Device.GeoCorridorFieldInfo);
            // -- startupInit.Device.FixedLocationFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_FixedLocationFieldInfo          , false, Device.FixedLocationFieldInfo);
            // -- startupInit.Device.LinkFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_LinkFieldInfo                   , false, Device.LinkFieldInfo);
            // -- startupInit.Device.BorderCrossingFieldInfo=true
            boolean devBC = Account.SupportsBorderCrossing();
            addDBFields(tblName, fields, Device.OPTCOLS_BorderCrossingFieldInfo         , devBC, Device.BorderCrossingFieldInfo);
            // -- startupInit.Device.MaintOdometerFieldInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_MaintOdometerFieldInfo          , false, Device.MaintOdometerFieldInfo);
            // -- startupInit.Device.WorkOrderInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_WorkOrderInfo                   , false, Device.WorkOrderInfo);
            // -- startupInit.Device.DataPushInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_DataPushInfo                    , false, Device.DataPushInfo);
            // -- startupInit.Device.ELogHOSInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_ELogHOSInfo                     , false, Device.ELogHOSInfo);
            // -- startupInit.Device.MapShareInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_MapShareInfo                    , false, Device.MapShareInfo);
            // -- startupInit.Device.AttributeInfo=true
            addDBFields(tblName, fields, Device.OPTCOLS_AttributeInfo                   , false, Device.AttributeInfo);
            // -- startupInit.Device.GlobalSubscriber=true
            addDBFields(tblName, fields, Device.OPTCOLS_GlobalSubscriber                , false, Device.GlobalSubscriber);
            return fields;
        }

        /* DeviceGroup */
        if (tblName.equalsIgnoreCase(DeviceGroup.TABLE_NAME())) {
            // startupInit.DeviceGroup.WorkOrderInfo=true
            addDBFields(tblName, fields, DeviceGroup.OPTCOLS_WorkOrderInfo              , false, DeviceGroup.WorkOrderInfo);
            // startupInit.DeviceGroup.RuleNotification=true
            addDBFields(tblName, fields, DeviceGroup.OPTCOLS_RuleNotification           , false, DeviceGroup.RuleNotification);
            // startupInit.DeviceGroup.Miscellaneous=true
            addDBFields(tblName, fields, DeviceGroup.OPTCOLS_Miscellaneous              , false, DeviceGroup.Miscellaneous);
            return fields;
        }

        /* EventData */
        if (tblName.equalsIgnoreCase(EventData.TABLE_NAME())) {
            // -- startupInit.EventData.AutoIncrementIndex=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AutoIncrementIndex           , false, EventData.AutoIncrementIndex);
            // -- startupInit.EventData.CreationTimeMillisecond=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CreationTimeMillisecond      , false, EventData.CreationTimeMillisecond);
            // -- startupInit.EventData.AddressFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AddressFieldInfo             , false, EventData.AddressFieldInfo);
            // -- startupInit.EventData.GPSFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_GPSFieldInfo                 , false, EventData.GPSFieldInfo);
            // -- startupInit.EventData.CustomFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CustomFieldInfo              , false, EventData.CustomFieldInfo);
            // -- startupInit.EventData.GarminFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_GarminFieldInfo              , false, EventData.GarminFieldInfo);
            // -- startupInit.EventData.CANBUSFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_CANBUSFieldInfo              , false, EventData.CANBUSFieldInfo);
            // -- startupInit.EventData.AtmosphereFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AtmosphereFieldInfo          , false, EventData.AtmosphereFieldInfo);
            // -- startupInit.EventData.ThermoFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_ThermoFieldInfo              , false, EventData.ThermoFieldInfo, 4);
            // -- startupInit.EventData.AnalogFieldInfo=true
            addDBFields(tblName, fields, EventData.OPTCOLS_AnalogFieldInfo              , false, EventData.AnalogFieldInfo);
            // -- startupInit.EventData.TripSummary=true
            addDBFields(tblName, fields, EventData.OPTCOLS_TripSummary                  , false, EventData.TripSummary);
            // -- startupInit.EventData.EndOfDaySummary=true
            addDBFields(tblName, fields, EventData.OPTCOLS_EndOfDaySummary              , false, EventData.EndOfDaySummary);
            // -- startupInit.EventData.ServingCellTowerData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_ServingCellTowerData         , false, EventData.ServingCellTowerData);
            // -- startupInit.EventData.NeighborCellTowerData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_NeighborCellTowerData        , false, EventData.NeighborCellTowerData);
            // -- startupInit.EventData.WorkZoneGridData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_WorkZoneGridData             , false, EventData.WorkZoneGridData);
            // -- startupInit.EventData.LeaseRentalData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_LeaseRentalData              , false, EventData.LeaseRentalData);
            // startupInit.EventData.ImpactData=true
            addDBFields(tblName, fields, EventData.OPTCOLS_ImpactData                   , false, EventData.ImpactData);
            return fields;
        }

        /* Geozone */
        if (tblName.equalsIgnoreCase(Geozone.TABLE_NAME())) {
            // -- startupInit.Geozone.PriorityFieldInfo
            addDBFields(tblName, fields, "startupInit.Geozone.PriorityFieldInfo"        , false, Geozone.PriorityFieldInfo);
            // -- startupInit.Geozone.CorridorFieldInfo
            addDBFields(tblName, fields, "startupInit.Geozone.CorridorFieldInfo"        , false, Geozone.CorridorFieldInfo);
            return fields;
        }

        /* leave as-is */
        return fields;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Add the specified fields to the table
    *** @param tblName      The table name
    *** @param tblFields    The list of table fields
    *** @param key          The boolean key used to check for permission to add these fields
    *** @param defaultAdd   The default if the property is not explicitly specfified
    *** @param customFields The fields to add, assuming the boolean key returns true.
    **/
    protected void addDBFields(String tblName, java.util.List<DBField> tblFields, String key, boolean defaultAdd, DBField customFields[])
    {
        this.addDBFields(tblName, tblFields, key, defaultAdd, customFields, -1);
    }

    /**
    *** Add the specified fields to the table
    *** @param tblName      The table name
    *** @param tblFields    The list of table fields
    *** @param key          The boolean key used to check for permission to add these fields
    *** @param defaultAdd   The default if the property is not explicitly specfified
    *** @param customFields The fields to add, assuming the boolean key returns true.
    *** @param maxCount     The maximum number of fields to add from the customFields array
    **/
    protected void addDBFields(String tblName, java.util.List<DBField> tblFields, String key, boolean defaultAdd, DBField customFields[], int maxCount)
    {

        /* add additional fields? */
        boolean addFields = false;
        if (StringTools.isBlank(key)) {
            addFields = defaultAdd;
        } else
        if (maxCount >= 0) {
            String keyVal = RTConfig.getString(key,null); // yes|no|true|false|0|1|2|3|4|5|6|7|9
            int valInt = StringTools.isInt(keyVal,true)? StringTools.parseInt(keyVal,-1) : -1;
            if (valInt > 0) {
                maxCount  = valInt;
                addFields = true;
            } else {
                addFields = (maxCount > 0)? RTConfig.getBoolean(key,defaultAdd) : false;
            }
            //if (addFields && 
            //    tblName.equalsIgnoreCase(EventData.TABLE_NAME()) &&
            //    key.equals(EventData.OPTCOLS_ThermoFieldInfo)      ) {
            //    EventData.setThermoCount(maxCount);
            //}
        } else {
            addFields = RTConfig.getBoolean(key,defaultAdd);
        }

        /* add fields */
        if (customFields == null) {
            Print.logStackTrace("'customFields' is null! Table=" + tblName + " Key=" + key);
        } else
        if (addFields) {
            int cnt = ((maxCount >= 0) && (maxCount <= customFields.length))? maxCount : customFields.length;
            for (int i = 0; i < cnt; i++) {
                tblFields.add(customFields[i]);
            }
        }

    }

    // ------------------------------------------------------------------------

}
