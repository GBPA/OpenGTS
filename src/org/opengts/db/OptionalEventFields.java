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
//     -Initial release, extracted from EventUtil.java
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.Vector;
import java.util.Locale;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

// -- was "EventUtil.OptionalEventFields"
public abstract class OptionalEventFields
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* extra map data fields */
    public static String    PROP_OptionalEventFields_DeviceMap[] = {
        "OptionalEventFields.DeviceMap",
        "OptionalEventFields.EventData"
    };
    public static String    PROP_OptionalEventFields_FleetMap[]  = {
        "OptionalEventFields.FleetMap",
        "OptionalEventFields.GroupMap",
        "OptionalEventFields.Device"
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static class EVField
    {
        private String  name  = "";
        private DBField field = null;
        public EVField(String name) {
            this.name  = StringTools.trim(name);
            this.field = null;
        }
        public EVField(DBField field) {
            this.name  = (field != null)? field._getName() : "";
            this.field = field;
        }
        public String getName() {
            return this.name; // not null
        }
        public boolean hasDBField() {
            return (this.field != null);
        }
        public DBField getDBField() {
            return this.field;
        }
    }

    protected static EVField[] parseFields(DBFactory<? extends DBRecord<?>> factory, String flda[])
    {
        if (factory == null) {
            return null;
        } else
        if (ListTools.isEmpty(flda)) {
            // -- no defined field names, return nothing
            return null;
        } else {
            //return factory.getFields(flda); 
            java.util.List<EVField> fldList = new Vector<EVField>();
            for (int i = 0; i < flda.length; i++) {
                String n = StringTools.trim(flda[i]);
                if (!StringTools.isBlank(n)) {
                    DBField dfld = factory.getField(n);
                    fldList.add((dfld != null)? new EVField(dfld) : new EVField(n));
                }
            }
            return !ListTools.isEmpty(fldList)? fldList.toArray(new EVField[fldList.size()]) : null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String KEY_fuelLevel           = "fuelLevel";
    private static final String KEY_fuelLevelVolume     = "fuelLevelVolume";
    private static final String KEY_elapsedTimeStopped  = "elapsedTimeStopped";
    private static final String KEY_geozoneName         = "geozoneName";

    private static final String KEY_lastFuelLevel       = "lastFuelLevel";
    private static final String KEY_lastFuelLevelVolume = "lastFuelLevelVolume";

    /**
    *** Creates a generic custom OptionalEventFields instance
    *** @return An OptionalEventFields instance
    **/
    public static OptionalEventFields createOptionalEventFieldsHandler(
        String deviceMapFlds[], 
        String fleetMapFlds[])
    {

        /* always use EventData to resolve optionalEventFields? */
        final boolean useEventDataToResolveFields = true;

        /* Device/Fleet map fields */
        final EVField optDeviceFields[] = OptionalEventFields.parseFields(EventData.getFactory(), deviceMapFlds);
        final EVField optFleetFields[]  = OptionalEventFields.parseFields(Device.getFactory()   , fleetMapFlds);
        if (ListTools.isEmpty(optDeviceFields) && ListTools.isEmpty(optFleetFields)) {
            // -- both Device/Fleet field list is empty
            return null;
        }

        /* return OptionalEventFields instance */
        return new OptionalEventFields() {

            // -- return number of 'optional' fields
            public int getOptionalEventFieldCount(boolean isFleet) {
                if (isFleet) {
                    // -- Group/Fleet map count
                    return ListTools.size(optFleetFields);
                } else {
                    // -- Device/Vehicle map count
                    return ListTools.size(optDeviceFields);
                }
            }

            // -- return the title for a specific 'optional' field
            public String getOptionalEventFieldTitle(int ndx, boolean isFleetMap, Locale locale) {
                // -- invalid argument checks
                if (ndx < 0) { 
                    return ""; 
                }
                // -- default vars
                I18N i18n = I18N.getI18N(OptionalEventFields.class, locale);
                // -- check map type
                if (isFleetMap) {
                    // -- "Fleet" map title
                    if (ndx >= ListTools.size(optFleetFields)) {
                        return "";
                    }
                    String name = optFleetFields[ndx].getName();
                    // -- try custom field names
                    if (name.equalsIgnoreCase(Device.FLD_linkURL)) {
                        DBField dbfld = optFleetFields[ndx].getDBField();
                        return dbfld.getTitle(locale);
                    }
                    // -- try EventData/Device.getKeyFieldValue(...)
                    if (useEventDataToResolveFields) {
                        Object val = EventData.getKeyFieldTitle(name, ""/*arg*/, locale);
                        return StringTools.trim(val);
                    } else
                    if (name.equalsIgnoreCase(OptionalEventFields.KEY_fuelLevel)          ||
                        name.equalsIgnoreCase(OptionalEventFields.KEY_fuelLevelVolume)    ||
                        name.equalsIgnoreCase(OptionalEventFields.KEY_elapsedTimeStopped) ||
                        name.equalsIgnoreCase(OptionalEventFields.KEY_geozoneName)          ) {
                        Object val = EventData.getKeyFieldTitle(name, ""/*arg*/, locale);
                        return StringTools.trim(val);
                    } else {
                        Object val = Device.getKeyFieldTitle(name, ""/*arg*/, locale);
                        if (val == null) {
                            val = EventData.getKeyFieldTitle(name, ""/*arg*/, locale);
                        }
                        return StringTools.trim(val);
                    }
                } else {
                    // -- "Device" map title
                    if (ndx >= ListTools.size(optDeviceFields)) {
                        return "";
                    }
                    String name = optDeviceFields[ndx].getName();
                    // -- try custom field names
                       // none ...
                    // -- try EventData.getKeyFieldValue(...)
                    Object val = EventData.getKeyFieldTitle(name, "", locale);
                    if (false) { // keep these strings in the LocatString_XX.properties files
                        String s1 = i18n.getString("OptionalEventFields.fuelLevelVolume", "Fuel Volume");
                        String s2 = i18n.getString("OptionalEventFields.elapsedTimeStopped", "Time Stopped");
                        String s3 = i18n.getString("OptionalEventFields.geozoneName", "Geozone Name");
                        //i18n.getString("OptionalEventFields.info.digInput", "Digital Input");
                    }
                    return StringTools.trim(val);
                }
            }

            // -- return the value for a specific 'optional' field
            public String getOptionalEventFieldValue(int ndx, boolean isFleetMap, Locale locale, EventDataProvider edp) {
                // -- invalid argument checks
                if (ndx < 0) { 
                    return ""; 
                } else
                if (!(edp instanceof EventData)) {
                    return "";
                }
                // -- default vars
                EventData event = (EventData)edp;  // non-null
                Account account = event.getAccount();
                Device  device  = event.getDevice();
                if ((account == null) || (device == null)) {
                    return "";
                }
                // -- check map type
                if (isFleetMap) {
                    // -- Group/Fleet map value
                    if (ndx >= ListTools.size(optFleetFields)) {
                        return "";
                    }
                    String name = optFleetFields[ndx].getName();
                    // -- try custom field names
                    if (name.equalsIgnoreCase(Device.FLD_linkURL)) {
                        // -- NOTE! Enabling 'getLinkURL' and 'getLinkDescrption' requires
                        // -  that the following property be specified a '.conf' file:
                        // -    startupInit.Device.LinkFieldInfo=true
                        String url = device.getLinkURL();
                        if (!StringTools.isBlank(url)) {
                            String desc = device.getLinkDescription();
                            if (StringTools.isBlank(desc)) {
                                BasicPrivateLabel bpl = Account.getPrivateLabel(account);
                                I18N i18n = I18N.getI18N(OptionalEventFields.class, bpl.getLocale());
                                desc = i18n.getString("OptionalEventFields.info.link", "Link");
                            }
                            String a = "<a href='"+url+"' target='_blank'>"+desc+"</a>";
                            return a;
                        }
                        return "";
                    }
                    // -- try EventData/Device.getKeyFieldValue(...)
                    BasicPrivateLabel bpl = Account.getPrivateLabel(account);
                    if (useEventDataToResolveFields) {
                        Object val = event.getKeyFieldValue(name, ""/*arg*/, bpl);
                        return StringTools.trim(val);
                    } else
                    if (name.equalsIgnoreCase(OptionalEventFields.KEY_fuelLevel)          ||
                        name.equalsIgnoreCase(OptionalEventFields.KEY_fuelLevelVolume)    ||
                        name.equalsIgnoreCase(OptionalEventFields.KEY_elapsedTimeStopped) ||
                        name.equalsIgnoreCase(OptionalEventFields.KEY_geozoneName)          ) {
                        Object val = event.getKeyFieldValue(name, ""/*arg*/, bpl);
                        return StringTools.trim(val);
                    } else {
                        Object val = device.getKeyFieldValue(name, ""/*arg*/, bpl);
                        if (val == null) {
                            val = event.getKeyFieldValue(name, ""/*arg*/, bpl);
                        }
                        return StringTools.trim(val);
                    }
                } else {
                    // -- "Device" map value
                    if (ndx >= ListTools.size(optDeviceFields)) {
                        return "";
                    }
                    String name = optDeviceFields[ndx].getName();
                    // -- try custom field names
                       // none ...
                    // -- try EventData.getKeyFieldValue(...)
                    BasicPrivateLabel bpl = Account.getPrivateLabel(account);
                    Object val = event.getKeyFieldValue(name, "", bpl);
                    return StringTools.trim(val);
                }
            }

        }; // new OptionalEventField
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* abstract methods */
    public abstract int    getOptionalEventFieldCount(boolean isFleet);
    public abstract String getOptionalEventFieldTitle(int ndx, boolean isFleet, Locale locale);
    public abstract String getOptionalEventFieldValue(int ndx, boolean isFleet, Locale locale, EventDataProvider edp);

    // ------------------------------------------------------------------------

}
