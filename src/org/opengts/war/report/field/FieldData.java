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
//  2007/01/10  Martin D. Flynn
//     -Added methods to sort FieldData lists by the device description
// ----------------------------------------------------------------------------
package org.opengts.war.report.field;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.report.*;

public class FieldData
    implements CSSRowClass
{

    // ------------------------------------------------------------------------

    public static final String KEY_ACCOUNT      = "$account";
    public static final String KEY_DEVICE       = "$device";
    public static final String KEY_DEVICE_DESC  = "$deviceDesc";
    public static final String KEY_DEVICE_VIN   = "$deviceVIN";
    public static final String KEY_DRIVER       = "$driver";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                  cssClass    = null;
    private DBDataRow.RowType       rowType     = DBDataRow.RowType.DETAIL;
    private HashMap<String,Object>  fieldValues = null;

    public FieldData()
    {
        super();
        this.fieldValues = new HashMap<String,Object>();
    }

    // ------------------------------------------------------------------------
    
    public void setRowType(DBDataRow.RowType rt)
    {
        this.rowType = (rt != null)? rt : DBDataRow.RowType.DETAIL;
    }
    
    public DBDataRow.RowType getRowType()
    {
        return (this.rowType != null)? this.rowType : DBDataRow.RowType.DETAIL;
    }

    // ------------------------------------------------------------------------

    public boolean hasCssClass()
    {
        return !StringTools.isBlank(this.getCssClass());
    }

    public String getCssClass()
    {
        return this.cssClass;
    }

    public void setCssClass(String rowClass)
    {
        this.cssClass = rowClass;
    }

    // ------------------------------------------------------------------------

    /**
    *** This method provide this instance to tweak (colorise, etc) the returned 
    *** value from the FieldLayout class.
    *** @param key     The field key that the value represents
    *** @param rtnVal  The value to filter (is either a String or ColumnValue instance)
    *** @return The returned value (default is to return the specified value unfiltered)
    **/
    public Object filterReturnedValue(String key, Object rtnVal)
    {
        return rtnVal;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Object value for the specified key
    *** @param key  The key to set
    *** @param val  The value to set
    **/
    public void setValue(String key, Object val)
    {
        this.fieldValues.put(key, val);
    }

    /**
    *** Gets the Object value for the specified key
    *** @param key  The key to get
    *** @param dft  The default value returned if the specified key is undefined
    **/
    public Object getValue(String key, Object dft)
    {
        Object val = this.fieldValues.get(key);
        return (val != null)? val : dft;
    }

    /**
    *** Gets the Object value for the specified key
    *** @param key  The key to get
    **/
    public Object getValue(String key)
    {
        return this.getValue(key, null);
    }

    /**
    *** Returns true if the key is defined
    *** @param key  The key to check for a value
    **/
    public boolean hasValue(String key)
    {
        return this.fieldValues.containsKey(key);
    }

    // --------------------------------

    /**
    *** Gets the Object value for the first defined key in the specified array
    *** @param keys An array of keys  
    *** @param dft  The default value returned if the value is not found
    **/
    public Object getValue(String keys[], Object dft)
    {
        if (!ListTools.isEmpty(keys)) {
            for (String key : keys) {
                Object val = this.getValue(key,null);
                if (val != null) {
                    return val;
                }
            }
        }
        return dft;
    }

    /**
    *** Gets the Object value for the first defined key in the specified array
    *** @param keys An array of keys  
    **/
    public Object getValue(String keys[])
    {
        return this.getValue(keys, null);
    }

    /**
    *** Returns true if at least one of the specified keys is defined
    *** @param keys  An array of keys
    **/
    public boolean hasValue(String keys[])
    {
        if (!ListTools.isEmpty(keys)) {
            for (String key : keys) {
                if (this.hasValue(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // -- Account

    /* return the account id associated with this data record */
    public String getAccountID()
    {
        // may return "" if undefined
        String acctID = this.getString(FieldLayout.DATA_ACCOUNT_ID, null);
        if (acctID != null) {
            return acctID;
        } else {
            Account acct = this.getAccount(null);
            if (acct != null) {
                return acct.getAccountID();
            } else {
                return "";
            }
        }
    }

    /* set the account associated with this record */
    public void setAccount(Account account)
    {
        this.setValue(KEY_ACCOUNT, account);
        if (account != null) {
            this.setValue(FieldLayout.DATA_ACCOUNT_ID, account.getAccountID());
        }
    }

    /* get the account associated with this record (or the default if not defined) */
    public Account getAccount(Account dft)
    {
        Object val = this.getValue(KEY_ACCOUNT,null); // this.fieldValues.get(KEY_ACCOUNT);
        if (val instanceof Account) {
            // we have an account
            return (Account)val;
        } else
        if (dft != null) {
            // a default account has been specified
            return dft;
        } else {
            // no account, and no default
            Device dev = this.getDevice();
            if (dev != null) {
                // obtain the account from the device
                Account acct = dev.getAccount();
                this.setAccount(acct);
                return acct;
            } else {
                // no account, no device, no default, return null
                return null;
            }
        }
    }

    /* get the account associated with this record (or null if not defined) */
    public Account getAccount()
    {
        return this.getAccount(null);
    }
    
    // ------------------------------------------------------------------------
    // -- Device

    /* return the device id associated with this data record */
    public String getDeviceID()
    {
        // -- may return (blank) "" if undefined
        String devID = this.getString(FieldLayout.DATA_DEVICE_ID, null);
        if (devID != null) {
            return devID;
        } else {
            Device dev = this.getDevice(null);
            if (dev != null) {
                return dev.getDeviceID();
            } else {
                return "";
            }
        }
    }

    /* set the device associated with this data record */
    public void setDevice(Device device)
    {
        this.setValue(KEY_DEVICE, device);
        if (device != null) {
            this.setValue(FieldLayout.DATA_DEVICE_ID, device.getDeviceID());
        }
    }
    
    /* return the cached device (or the default device if not defined) */
    public Device getDevice(Device dft)
    {
        Object val = this.getValue(KEY_DEVICE,null); // this.fieldValues.get(KEY_DEVICE);
        return (val instanceof Device)? (Device)val : dft;
    }

    /* return the cached device (or null if not defined) */
    public Device getDevice()
    {
        return this.getDevice(null);
    }
    
    /* return the device description */
    public String getDeviceDescription()
    {
        Object devDesc = this.getValue(KEY_DEVICE_DESC,null); // this.fieldValues.get(KEY_DEVICE_DESC);
        if (devDesc != null) {
            return (String)devDesc;
        } else {
            Device dev = this.getDevice(null);
            if (dev != null) {
                String desc = dev.getDescription();
                this.setValue(KEY_DEVICE_DESC, desc);
                return desc;
            } else {
                // default to returning the device ID (if defined)
                return this.getString(FieldLayout.DATA_DEVICE_ID, "");
            }
        } 
    }
    
    /* return the device description */
    public String getDeviceVIN()
    {
        Object devVIN = this.getValue(KEY_DEVICE_VIN,null); // this.fieldValues.get(KEY_DEVICE_VIN);
        if (devVIN != null) {
            return (String)devVIN;
        } else {
            Device dev = this.getDevice(null);
            if (dev != null) {
                String vin = dev.getDeviceVIN();
                this.setValue(KEY_DEVICE_VIN, vin);
                return vin;
            } else {
                // default to returning the device ID (if defined)
                return this.getString(FieldLayout.DATA_DEVICE_ID, "");
            }
        } 
    }

    // ------------------------------------------------------------------------
    // -- Driver

    /* return the driver id associated with this data record */
    public String getDriverID()
    {
        // may return "" if undefined
        String driverID = this.getString(FieldLayout.DATA_DRIVER_ID, null);
        if (driverID != null) {
            return driverID;
        } else {
            Device device = this.getDevice(null);
            if (device != null) {
                return device.getDriverID();
            } else {
                return "";
            }
        }
    }

    /* set the account associated with this record */
    public void setDriver(Driver driver)
    {
        this.setValue(KEY_DRIVER, driver);
        if (driver != null) {
            this.setValue(FieldLayout.DATA_DRIVER_ID, driver.getDriverID());
        }
    }

    /* get the account associated with this record (or the default if not defined) */
    public Driver getDriver(Driver dft)
    {
        Object val = this.getValue(KEY_DRIVER,null); // this.fieldValues.get(KEY_DRIVER);
        if (val instanceof Driver) {
            // we have a driver
            return (Driver)val;
        } else
        if (dft != null) {
            // a default driver has been specified
            return dft;
        } else {
            Account account = this.getAccount();
            String driverID = this.getDriverID();
            try {
                Driver driver = Driver.getDriver(account,driverID);
                if (driver != null) {
                    this.setDriver(driver);
                    return driver;
                } else {
                    return null;
                }
            } catch (DBException dbe) {
                // error
                return null;
            }
        }
    }

    /* get the driver associated with this record (or null if not defined) */
    public Driver getDriver()
    {
        return this.getDriver(null);
    }

    // ------------------------------------------------------------------------
    // -- GeoPoint

    public void setGeoPoint(String key, GeoPoint gp)
    {
        this.setValue(key, gp);
    }

    public GeoPoint getGeoPoint(String key, GeoPoint dft)
    {
        Object val = this.getValue(key,null);
        if (val instanceof GeoPoint) {
            return (GeoPoint)val;
        } else {
            return dft;
        }
    }

    public GeoPoint getGeoPoint(String key)
    {
        return this.getGeoPoint(key, null);
    }

    // ------------------------------------------------------------------------
    // -- Latitude/Longitude

    public double getLatitude(String key)
    {
        return this.getLatitude(key, 0.0);
    }

    public double getLatitude(String key, double dft)
    {
        Object val = this.getValue(key,null);
        if (val instanceof GeoPoint) {
            return ((GeoPoint)val).getLatitude();
        } else
        if (val instanceof Number) {
            return ((Number)val).doubleValue();
        } else {
            return dft;
        }
    }

    // --------------------------------

    public double getLongitude(String key)
    {
        return this.getLongitude(key, 0.0);
    }

    public double getLongitude(String key, double dft)
    {
        Object val = this.getValue(key,null);
        if (val instanceof GeoPoint) {
            return ((GeoPoint)val).getLongitude();
        } else
        if (val instanceof Number) {
            return ((Number)val).doubleValue();
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------
    // -- Boolean

    public void setValue(String key, boolean val)
    {
        this.setValue(key, new Boolean(val));
    }
    
    public void setBoolean(String key, boolean val)
    {
        this.setValue(key, new Boolean(val));
    }

    public boolean getBoolean(String key, boolean dft)
    {
        Object val = this.getValue(key,null);
        return StringTools.parseBoolean(val, dft);
    }

    public boolean getBoolean(String key)
    {
        return this.getBoolean(key, false);
    }

    // ------------------------------------------------------------------------
    // -- String

    public void setString(String key, String val)
    {
        this.setValue(key, val);
    }

    public String getString(String key, String dft)
    {
        Object val = this.getValue(key, null);
        return (val != null)? val.toString() : dft;
    }

    public String getString(String key)
    {
        return this.getString(key, "");
    }

    // ------------------------------------------------------------------------
    // -- Integer

    public void setValue(String key, int val)
    {
        this.setValue(key, new Integer(val));
    }

    public void setInt(String key, int val)
    {
        this.setValue(key, new Integer(val));
    }

    public int getInt(String key, int dft)
    {
        Object val = this.getValue(key,null);
        return (val instanceof Number)? ((Number)val).intValue() : dft;
    }

    public int getInt(String key)
    {
        return this.getInt(key, 0);
    }

    public boolean addInt(String key, int val)
    {
        Object obj = this.getValue(key,null);
        if (obj == null) {
            this.setValue(key, val);
            return true;
        } else
        if (obj instanceof Number) {
            val += StringTools.parseInt(obj,0);
            this.setValue(key, val);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // -- Long

    public void setValue(String key, long val)
    {
        this.setValue(key, new Long(val));
    }

    public void setLong(String key, long val)
    {
        this.setValue(key, new Long(val));
    }

    public long getLong(String key, long dft)
    {
        Object val = this.getValue(key,null); // this.fieldValues.get(key);
        return (val instanceof Number)? ((Number)val).longValue() : dft;
    }

    public long getLong(String key)
    {
        return this.getLong(key, 0L);
    }

    public long getTimestamp(String key)
    {
        return this.getLong(key, 0L);
    }

    public boolean addLong(String key, long val)
    {
        Object obj = this.getValue(key,null);
        if (obj == null) {
            this.setValue(key, val);
            return true;
        } else
        if (obj instanceof Number) {
            val += StringTools.parseLong(obj,0L);
            this.setValue(key, val);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // -- Double

    public void setValue(String key, double val)
    {
        this.setValue(key, new Double(val));
    }

    public void setDouble(String key, double val)
    {
        this.setValue(key, new Double(val));
    }

    public double getDouble(String key, double dft)
    {
        Object val = this.getValue(key,null); // this.fieldValues.get(key);
        return (val instanceof Number)? ((Number)val).doubleValue() : dft;
    }

    public double getDouble(String key)
    {
        return this.getDouble(key, 0.0);
    }

    public boolean addDouble(String key, double val)
    {
        Object obj = this.getValue(key,null);
        if (obj == null) {
            this.setValue(key, val);
            return true;
        } else
        if (obj instanceof Number) {
            val += StringTools.parseDouble(obj,0.0);
            this.setValue(key, val);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // -- DBRecord

    public void setDBRecord(String key, DBRecord<?> val)
    {
        this.setValue(key, val);
    }

    public DBRecord<?> getDBRecord(String key, DBRecord<?> dft)
    {
        Object val = this.getValue(key, null);
        return (val instanceof DBRecord)? (DBRecord)val : dft;
    }

    public DBRecord<?> getDBRecord(String key)
    {
        return this.getDBRecord(key, null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* singleton instance of DeviceDescriptionComparator */
    private static Comparator<FieldData> devDescComparator = null;
    public static Comparator<FieldData> getDeviceDescriptionComparator()
    {
        if (devDescComparator == null) {
            devDescComparator = new DeviceDescriptionComparator(); // ascending
        }
        return devDescComparator;
    }

    /* sort by device description ascending */
    public static void sortByDeviceDescription(java.util.List<FieldData> fieldDataList) 
    {
        if (fieldDataList != null) {
            try {
                Collections.sort(fieldDataList, FieldData.getDeviceDescriptionComparator());
            } catch (Throwable th) { // ClassCastException, etc
                Print.logException("Invalid FieldData list", th);
            }
        }
    }

    /* Comparator for FieldData device descriptions */
    public static class DeviceDescriptionComparator
        implements Comparator<FieldData>
    {
        private boolean ascending = true;
        public DeviceDescriptionComparator() {
            this(true);
        }
        public DeviceDescriptionComparator(boolean ascending) {
            this.ascending  = ascending;
        }
        public int compare(FieldData o1, FieldData o2) {
            // assume we are comparing FieldData instances
            String D1 = o1.getDeviceDescription();
            String D2 = o2.getDeviceDescription();
            return this.ascending? D1.compareTo(D2) : D2.compareTo(D1);
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

    /* singleton instance of DeviceMaintenanceKMComparator */
    private static Comparator<FieldData> devMaintKMComparator0 = null;
    private static Comparator<FieldData> devMaintKMComparator1 = null;
    public static Comparator<FieldData> getDeviceMaintenanceKMComparator(int ndx)
    {
        if (ndx == 1) {
            if (devMaintKMComparator1 == null) {
                devMaintKMComparator1 = new DeviceMaintenanceKMComparator(true,1); // ascending
            }
            return devMaintKMComparator1;
        } else {
            if (devMaintKMComparator0 == null) {
                devMaintKMComparator0 = new DeviceMaintenanceKMComparator(true,0); // ascending
            }
            return devMaintKMComparator0;
        }
    }

    /* sort by device maintenance-km ascending */
    public static void sortByDeviceMaintenanceKM(java.util.List<FieldData> fieldDataList, int ndx) 
    {
        if (fieldDataList != null) {
            try {
                Collections.sort(fieldDataList, FieldData.getDeviceMaintenanceKMComparator(ndx));
            } catch (Throwable th) { // ClassCastException, etc
                Print.logException("Invalid FieldData list", th);
            }
        }
    }

    /* Comparator for FieldData device descriptions */
    public static class DeviceMaintenanceKMComparator
        implements Comparator<FieldData>
    {
        private boolean ascending = true;
        private int     maintNdx  = 0;
        public DeviceMaintenanceKMComparator() {
            this(true,0);
        }
        public DeviceMaintenanceKMComparator(boolean ascending, int maintNdx) {
            this.ascending = ascending;
            this.maintNdx  = maintNdx;
        }
        public int compare(FieldData o1, FieldData o2) {
            // -- assume we are comparing FieldData instances
            Device D1 = o1.getDevice();
            Device D2 = o2.getDevice();
            double remainKM_1 = (D1 != null)? D1.getMaintenanceRemainingKM(this.maintNdx) : -1.0;
            double remainKM_2 = (D2 != null)? D2.getMaintenanceRemainingKM(this.maintNdx) : -1.0;
            if (remainKM_1 == remainKM_2) {
                return 0;
            } else
            if (remainKM_1 < 0.0) {
                // -- (remainKM_1 > remainKM_2) <0 always sorts to end of list
                return 1;
            } else
            if (remainKM_2 < 0.0) {
                // -- (remainKM_1 < remainKM_2) <0 always sorts to end of list
                return -1;
            } else
            if (remainKM_1 > remainKM_2) {
                // -- (remainKM_1 > remainKM_2)
                return this.ascending? 1  : -1;
            } else
            if (remainKM_1 < remainKM_2) {
                // -- (remainKM_1 < remainKM_2)
                return this.ascending? -1 :  1;
            } else {
                // -- (remainKM_1 == remainKM_2)
                return 0;
            }
        }
        public boolean equals(Object other) {
            if (other instanceof DeviceMaintenanceKMComparator) {
                DeviceMaintenanceKMComparator ddc = (DeviceMaintenanceKMComparator)other;
                if (this.ascending != ddc.ascending) {
                    return false;
                } else
                if (this.maintNdx != ddc.maintNdx) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* singleton instance of DeviceMaintenanceHRComparator */
    private static Comparator<FieldData> devMaintHRComparator0 = null;
    private static Comparator<FieldData> devMaintHRComparator1 = null;
    public static Comparator<FieldData> getDeviceMaintenanceHRComparator(int ndx)
    {
        if (ndx == 1) {
            if (devMaintHRComparator1 == null) {
                devMaintHRComparator1 = new DeviceMaintenanceHRComparator(true,1); // ascending
            }
            return devMaintHRComparator1;
        } else {
            if (devMaintHRComparator0 == null) {
                devMaintHRComparator0 = new DeviceMaintenanceHRComparator(true,0); // ascending
            }
            return devMaintHRComparator0;
        }
    }

    /* sort by device maintenance-hr ascending */
    public static void sortByDeviceMaintenanceHR(java.util.List<FieldData> fieldDataList, int ndx) 
    {
        if (fieldDataList != null) {
            try {
                Collections.sort(fieldDataList, FieldData.getDeviceMaintenanceHRComparator(ndx));
            } catch (Throwable th) { // ClassCastException, etc
                Print.logException("Invalid FieldData list", th);
            }
        }
    }

    /* Comparator for FieldData device descriptions */
    public static class DeviceMaintenanceHRComparator
        implements Comparator<FieldData>
    {
        private boolean ascending = true;
        private int     maintNdx  = 0;
        public DeviceMaintenanceHRComparator() {
            this(true,0);
        }
        public DeviceMaintenanceHRComparator(boolean ascending, int maintNdx) {
            this.ascending = ascending;
            this.maintNdx  = maintNdx;
        }
        public int compare(FieldData o1, FieldData o2) {
            // -- assume we are comparing FieldData instances
            Device D1 = o1.getDevice();
            Device D2 = o2.getDevice();
            double remainKM_1 = (D1 != null)? D1.getMaintenanceRemainingHR(this.maintNdx) : -1.0;
            double remainKM_2 = (D2 != null)? D2.getMaintenanceRemainingHR(this.maintNdx) : -1.0;
            if (remainKM_1 == remainKM_2) {
                return 0;
            } else
            if (remainKM_1 < 0.0) {
                // -- (remainKM_1 > remainKM_2) <0 always sorts to end of list
                return 1;
            } else
            if (remainKM_2 < 0.0) {
                // -- (remainKM_1 < remainKM_2) <0 always sorts to end of list
                return -1;
            } else
            if (remainKM_1 > remainKM_2) {
                // -- (remainKM_1 > remainKM_2)
                return this.ascending? 1  : -1;
            } else
            if (remainKM_1 < remainKM_2) {
                // -- (remainKM_1 < remainKM_2)
                return this.ascending? -1 :  1;
            } else {
                // -- (remainKM_1 == remainKM_2)
                return 0;
            }
        }
        public boolean equals(Object other) {
            if (other instanceof DeviceMaintenanceHRComparator) {
                DeviceMaintenanceHRComparator dhc = (DeviceMaintenanceHRComparator)other;
                if (this.ascending != dhc.ascending) {
                    return false;
                } else
                if (this.maintNdx != dhc.maintNdx) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Custom FieldEventDataProvider class
    **/
    public static class FieldEventDataProvider
        extends FieldData
        implements EventDataProvider
    {
        public FieldEventDataProvider() {
            super();
        }
        public String getAccountID() {
            return super.getAccountID();
        }
        public String getDeviceID() {
            return super.getDeviceID();
        }
        public String getDeviceDescription() {
            return super.getDeviceDescription();
        }
        public String getDeviceVIN() {
            return super.getDeviceVIN();
        }
        public long getTimestamp() {
            return super.getTimestamp(FieldLayout.DATA_TIMESTAMP);
        }
        public int getStatusCode() {
            return StatusCodes.STATUS_LOCATION;
        }
        public String getStatusCodeDescription(BasicPrivateLabel bpl) {
            Device dev  = null;
            int    code = this.getStatusCode();
            return StatusCode.getDescription(dev, code, bpl, "Location");
        }
        public StatusCodeProvider getStatusCodeProvider(BasicPrivateLabel bpl) {
            Device dev  = null;
            int    code = this.getStatusCode();
            return StatusCode.getStatusCodeProvider(dev, code, bpl, null/*dftSCP*/);
        }
        public int getPushpinIconIndex(String iconSelector, OrderedMap<String,PushpinIcon> iconMap, 
            boolean isFleet, BasicPrivateLabel bpl) {
            String pid = null;
            return EventData._getPushpinIconIndex(pid, iconMap, PushpinIcon.ICON_PUSHPIN_ORANGE);
        }
        public boolean isValidGeoPoint() {
            return GeoPoint.isValid(this.getLatitude(),this.getLongitude());
        }
        public double getLatitude() {
            double lat = super.getLatitude(FieldLayout.DATA_LATITUDE,GeoPoint.MAX_LATITUDE+1.0);
            if (GeoPoint.isValidLatitude(lat)) {
                return lat;
            } else {
                lat = super.getLatitude(FieldLayout.DATA_GEOPOINT,GeoPoint.MAX_LATITUDE+1.0);
                return GeoPoint.isValidLatitude(lat)? lat : 0.0;
            }
        }
        public double getLongitude() {
            double lon = super.getLongitude(FieldLayout.DATA_LONGITUDE,GeoPoint.MAX_LONGITUDE+1.0);
            if (GeoPoint.isValidLongitude(lon)) {
                return lon;
            } else {
                lon = super.getLongitude(FieldLayout.DATA_GEOPOINT,GeoPoint.MAX_LONGITUDE+1.0);
                return GeoPoint.isValidLongitude(lon)? lon : 0.0;
            }
        }
        public GeoPoint getGeoPoint() {
            GeoPoint gp = super.getGeoPoint(FieldLayout.DATA_GEOPOINT);
            if (gp != null) {
                return gp;
            } else {
                double lat = super.getLatitude( FieldLayout.DATA_LATITUDE ,GeoPoint.MAX_LATITUDE +1.0);
                double lon = super.getLongitude(FieldLayout.DATA_LONGITUDE,GeoPoint.MAX_LONGITUDE+1.0);
                return GeoPoint.isValid(lat,lon)? new GeoPoint(lat,lon) : GeoPoint.INVALID_GEOPOINT;
            }
        }
        public long getGpsAge() {
            return 0L; // not available
        }
        public long getCreationAge() {
            return 0L; // not available
        }
        public double getHorzAccuracy() {
            return -1.0; // not available
        }
        public GeoPoint getBestGeoPoint() {
            return this.getGeoPoint();
        }
        public double getBestAccuracy() {
            return this.getHorzAccuracy();
        }
        public int getSatelliteCount() {
            return 0;
        }
        public double getBatteryLevel() {
            return super.getDouble(FieldLayout.DATA_DEVICE_BATTERY_LEVEL, 0.0);
        }
        public double getBatteryVolts() {
            return super.getDouble(FieldLayout.DATA_DEVICE_BATTERY_VOLTS, 0.0);
        }
        public double getVBatteryVolts() {
            return super.getDouble(FieldLayout.DATA_DEVICE_VEHICLE_VOLTS, 0.0);
        }
        public double getSpeedKPH() {
            return super.getDouble(FieldLayout.DATA_SPEED, 0.0);
        }
        public double getHeading() {
            return super.getDouble(FieldLayout.DATA_HEADING, 0.0);
        }
        public double getAltitude() {
            return super.getDouble(FieldLayout.DATA_ALTITUDE, 0.0);
        }
        public double getOdometerKM() {
            return super.getDouble(FieldLayout.DATA_ODOMETER, 0.0);
        }
        public String getGeozoneID() {
            return super.getString(FieldLayout.DATA_GEOZONE_ID, "");
        }
        public String getAddress() {
            return super.getString(FieldLayout.DATA_ADDRESS, "");
        }
        public long getInputMask() {
            return 0L;
        }
        public void setEventIndex(int ndx)
        {
            super.setInt(FieldLayout.DATA_EVENT_INDEX,ndx);
        }
        public int getEventIndex()
        {
            return super.getInt(FieldLayout.DATA_EVENT_INDEX,-1);
        }
        public boolean getIsFirstEvent()
        {
            return (this.getEventIndex() == 0);
        }
        public void setIsLastEvent(boolean isLast) {
            super.setBoolean(FieldLayout.DATA_LAST_EVENT,isLast);
        }
        public boolean getIsLastEvent() {
            return super.getBoolean(FieldLayout.DATA_LAST_EVENT,false);
        }
    }

    // ------------------------------------------------------------------------

}
