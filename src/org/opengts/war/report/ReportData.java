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
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/03/25  Martin D. Flynn
//     -Added support for rule selectors
//     -Updated to use 'DeviceList'
//  2007/06/03  Martin D. Flynn
//     -Added PrivateLabel to constructor
//  2007/06/13  Martin D. Flynn
//     -Renamed 'DeviceList' to 'ReportDeviceList'
//  2007/06/30  Martin D. Flynn
//     -Added 'getTotalsDataIterator'
//  2007/11/28  Martin D. Flynn
//     -Integrated use of 'ReportColumn'
//  2008/02/21  Martin D. Flynn
//     -Modified '_getEventData' to set the Device on retrieved EventData records
//  2009/01/01  Martin D. Flynn
//     -Added 'setOrderAscending' to allow descending order EventData reports.
//  2009/11/01  Martin D. Flynn
//     -Added ReportOption support
//  2015/08/16  Martin D. Flynn
//     -Added support to "_getEventData_Device(..)" to allow null Device and 
//      retrieve EventData records based on the "where" specification only.
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.PrivateLabel;
import org.opengts.war.tools.RequestProperties;
import org.opengts.war.tools.MapDimension;
import org.opengts.war.tools.OutputProvider;

import org.opengts.war.report.ReportFactory;
import org.opengts.war.report.ReportColumn;

public abstract class ReportData
{

    // ------------------------------------------------------------------------

    //public static final long RECORD_LIMIT           = 800L;

    // ------------------------------------------------------------------------

    private static final boolean REPORT_DATA_FIELDS_ENABLED        = false;
    private static final String  PROP_reportDataFieldEnabled       = "reportDataFieldEnabled";

    private static final String  PROP_gpsAgeColorRange             = "gpsAgeColorRange";
    private static final String  PROP_gpsAgeColorRange_array       = "gpsAgeColorRange.array";

    private static final String  PROP_creationAgeColorRange        = "creationAgeColorRange";
    private static final String  PROP_creationAgeColorRange_array  = "creationAgeColorRange.array";

    private static final String  PROP_checkinAgeColorRange         = "checkinAgeColorRange";
    private static final String  PROP_checkinAgeColorRange_array   = "checkinAgeColorRange.array";

    private static final String  PROP_loginAgeColorRange           = "loginAgeColorRange";
    private static final String  PROP_loginAgeColorRange_array     = "loginAgeColorRange.array";

    // ------------------------------------------------------------------------

    public  static final String  FORMAT_MAP                        = "map";
    public  static final String  FORMAT_KML                        = "kml";
    public  static final String  FORMAT_PDF                        = "pdf";
    public  static final String  FORMAT_GRAPH                      = "graph";

    // ------------------------------------------------------------------------

    private static final String  DFT_REPORT_NAME                   = "generic.report";
    
    private static final String  DFT_REPORT_TITLE                  = "Generic Report";
    private static final String  DFT_REPORT_SUBTITLE               = "${deviceDesc} [${deviceId}]\n${dateRange}";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final ReportColumn EMPTY_COLUMNS[]              = new ReportColumn[0];

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /*
    public static ReportOptionsProvider getReportOptionsProvider()
    {
        return new ReportOptionsProvider() {
            public OrderedMap<String,ReportOption> getReportOptionMap(ReportFactory rptFact, RequestProperties reqState) {
                //PrivateLabel privLabel = reqState.getPrivateLabel();
                //I18N i18n = privLabel.getI18N(ReportData.class);
                //OrderedMap<String,ReportOption> map = new OrderedMap<String,ReportOption>();
                //map.put("test1", new ReportOption("test1", i18n.getString("ReportData.option.1","This is Option 1"), null));
                //map.put("test2", new ReportOption("test2", i18n.getString("ReportData.option.2","This is Option 2"), null));
                //return map;
                return null;
            }
        };
    }
    */

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String              reportName          = DFT_REPORT_NAME;
    private String              reportTitle         = DFT_REPORT_TITLE;
    private String              reportSubtitle      = DFT_REPORT_SUBTITLE;

    private ReportEntry         rptEntry            = null;
    private ReportFactory       rptFactory          = null;

    private Object/*ReportJob*/ rptJob              = null;

    private PrivateLabel        privLabel           = null;
    private RequestProperties   reqState            = null;
    private Account             account             = null;
    private User                user                = null;
    
    private String              preferredFormat     = "";
    
    private ReportDeviceList    deviceList          = null;

    private int                 eventDataCount      = 0; // per device
    private int                 eventMatchCount     = 0; // per device
    private int                 maxEventDataCount   = 0; // max device counted events

    private int                 rptRecordCount      = 0;
    private boolean             rptIsPartial        = false;

    private ReportConstraints   rptConstraints      = null;
    
    private ReportOption        reportOption        = null;
    private RTProperties        reportProperties    = null;

    private ReportHeaderGroup   rptHdrGrps[]        = null;

    private ReportColumn        rptColumns[]        = EMPTY_COLUMNS; // never null
    
    private URIArg              refreshURL          = null;
    private URIArg              autoReportURL       = null;
    private URIArg              graphImageURL       = null; // OBSOLETE
    private URIArg              mapURL              = null;
    private URIArg              kmlURL              = null;

    private String              iconSelector        = null;
    
    private ReportCallback      rptCallback         = null;

    // ------------------------------------------------------------------------

    /* OBSOLETE: create an instance of a report */
    public ReportData(ReportFactory rptFact, RequestProperties reqState, Account acct, User user, ReportDeviceList devList)
        throws ReportException
    {
        this.rptFactory = rptFact;      // never null
        this.reqState   = reqState;     // never null
        this.privLabel  = this.reqState.getPrivateLabel();
        this.account    = acct;
        this.user       = user;
        this.deviceList = devList;
    }

    /**
    *** Constructor
    **/
    public ReportData(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        this.rptEntry   = rptEntry;                             // never null
        this.rptFactory = this.rptEntry.getReportFactory();     // never null
        this.reqState   = reqState;                             // never null
        this.privLabel  = this.reqState.getPrivateLabel();      // never null
        this.account    = this.reqState.getCurrentAccount();    // should not be null
        this.user       = this.reqState.getCurrentUser();       // may be null;
        this.deviceList = devList;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance defines a ReportJob
    **/
    public boolean hasReportJob()
    {
        return (this.rptJob != null)? true : false;
    }

    /**
    *** Gets the ReportJob for this report (if any)
    **/
    public Object/*ReportJob*/ getReportJob()
    {
        return this.rptJob;
    }

    /**
    *** Sets the ReportJob for this report (if any)
    **/
    public void setReportJob(Object/*ReportJob*/ rj)
    {
        this.rptJob = rj;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the report entry which created this report 
    **/
    public ReportEntry getReportEntry()
    {
        return this.rptEntry; // may be null
    }

    /**
    *** Returns the report factory which ctreated this report 
    **/
    public ReportFactory getReportFactory()
    {
        return this.rptFactory; // never null
    }

    /** 
    *** Returns the ReportFactory properties 
    **/
    public RTProperties getProperties()
    {
        if (this.reportProperties == null) {
            this.reportProperties = this.getReportFactory().getProperties(); // never null
            if (this.hasReportOption()) {
                this.reportProperties = new RTProperties(this.reportProperties);
                //this.reportProperties.printProperties("ReportData Properties:");
                //this.getReportOption().getProperties().printProperties("ReportOption Properties:");
                this.reportProperties.setProperties(this.getReportOption().getProperties());
                //this.reportProperties.printProperties("Combined Properties:");
            }
        }
        return this.reportProperties; // never null
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this report 
    **/
    public void setReportName(String name)
    {
        this.reportName = name;
    }

    /**
    *** Gets the name of this report 
    **/
    public String getReportName()
    {
        if ((this.reportName != null) && !this.reportName.equals("")) {
            return this.reportName;
        } else {
            return DFT_REPORT_NAME;
        }
    }
   
    // ------------------------------------------------------------------------

    /**
    *** Gets the report type
    **/
    public String getReportType()
    {
        return this.getReportFactory().getReportType();
    }
   
    // ------------------------------------------------------------------------

    /**
    *** Sets the report title
    **/
    public void setReportTitle(String title)
    {
        this.reportTitle = title;
    }

    /**
    *** Gets the report title
    **/
    public String getReportTitle()
    {
        if ((this.reportTitle != null) && !this.reportTitle.equals("")) {
            return this.expandHeaderText(this.reportTitle);
        } else {
            return this.expandHeaderText(DFT_REPORT_NAME);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the report sub-title
    **/
    public void setReportSubtitle(String title)
    {
        this.reportSubtitle = title;
    }

    /**
    *** Gets the report sub-title
    **/
    public String getReportSubtitle()
    {
        //if (!StringTools.isBlank(this.reportSubtitle)) {
            return this.expandHeaderText(this.reportSubtitle);
        //} else {
        //    return this.expandHeaderText(DFT_REPORT_SUBTITLE);
        //}
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Replaces ${key} fields with the representative text
    **/
    public String expandHeaderText(String text)
    {
        return ReportLayout.expandHeaderText(text, this);
    }

    // ------------------------------------------------------------------------
    // RequestProperties

    /**
    *** Gets the current RequestProperties instance
    **/
    public RequestProperties getRequestProperties()
    {
        return this.reqState; // never null
    }

    /**
    *** Returns true if this is a "SOAP" request
    **/
    public boolean isSoapRequest()
    {
        return this.getRequestProperties().isSoapRequest();
    }

    /**
    *** Gets the selected TimeZone 
    **/
    public TimeZone getTimeZone()
    {
        return this.getRequestProperties().getTimeZone();
    }

    /**
    *** Gets the selected TimeZone as a String
    **/
    public String getTimeZoneString()
    {
        return this.getRequestProperties().getTimeZoneString(null);
    }

    // ------------------------------------------------------------------------
    // PrivateLabel

    /**
    *** Gets the current PrivateLabel instance
    **/
    public PrivateLabel getPrivateLabel()
    {
        return this.privLabel;
    }

    /**
    *** Gets the current PrivateLabel Locale
    **/
    public Locale getLocale()
    {
        return this.getRequestProperties().getLocale();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the map icon selector 
    **/
    public void setMapIconSelector(String iconSel)
    {
        this.iconSelector = iconSel;
    }

    /**
    *** Gets the map icon selector 
    **/
    public String getMapIconSelector()
    {
        return this.iconSelector;
    }

    // ------------------------------------------------------------------------
    // Account 

    /**
    *** Gets the current Account
    **/
    public Account getAccount()
    {
        return this.account;
    }

    /**
    *** Gets the current Account-ID
    **/
    public String getAccountID()
    {
        Account a = this.getAccount();
        return (a != null)? a.getAccountID() : "";
    }

    /**
    *** Gets the current Account description
    **/
    public String getAccountDescription()
    {
        Account a = this.getAccount();
        return (a != null)? a.getDescription() : "";
    }

    // ------------------------------------------------------------------------
    // User 

    /**
    *** Gets the current User (may be null)
    **/
    public User getUser()
    {
        return this.user;
    }

    /**
    *** Gets the current User-ID (may be blank)
    **/
    public String getUserID()
    {
        User u = this.getUser();
        return (u != null)? u.getUserID() : "";
    }

    // ------------------------------------------------------------------------
    // preferred format 

    /**
    *** Gets the preferred report format 
    **/
    public String getPreferredFormat()
    {
        return StringTools.trim(this.preferredFormat);
    }

    /**
    *** Sets the preferred report format 
    **/
    public void setPreferredFormat(String format)
    {
        this.preferredFormat = StringTools.trim(format);
    }

    // ------------------------------------------------------------------------
    // single device report

    /**
    *** Returns true if this report handles only a single device at a time
    *** @return True If this report handles only a single device at a time
    **/
    public boolean isSingleDeviceOnly()
    {
        return false;
    }

    // ------------------------------------------------------------------------
    // Devices

    /**
    *** Sets the device list 
    **/
    protected void setReportDeviceList(ReportDeviceList devList)
        throws ReportException
    {
        this.deviceList = devList;
    }

    /**
    *** Gets the device list 
    **/
    public ReportDeviceList getReportDeviceList()
    {
        if (this.deviceList == null) {
            this.deviceList = new ReportDeviceList(this.getAccount(),this.getUser());
            // -- sort by device description!
        }
        return this.deviceList;
    }

    /**
    *** Returns true if this was a group selection report
    **/
    public boolean isDeviceGroupReport()
    {
        ReportDeviceList rdl = this.getReportDeviceList();
        return rdl.isDeviceGroup();
    }

    /**
    *** Gets the device list size
    **/
    public int getDeviceCount()
    {
        if (this.deviceList == null) {
            return 0;
        } else {
            return this.deviceList.size();
        }
    }

    /**
    *** Gets the first device id in the list
    **/
    public String getFirstDeviceID()
    {
        return this.getReportDeviceList().getFirstDeviceID();
    }

    /**
    *** Gets the Device instance for the specified ID
    **/
    public Device getDevice(String deviceID)
        throws DBException
    {
        ReportDeviceList devList = this.getReportDeviceList();
        return devList.getDevice(deviceID);
    }

    // ------------------------------------------------------------------------
    // report header groups

    /**
    *** Sets the report header groups 
    **/
    public void setReportHeaderGroups(ReportHeaderGroup rhg[])
    {
        this.rptHdrGrps = rhg;
    }

    /**
    *** Gets the report header groups 
    **/
    public ReportHeaderGroup[] getReportHeaderGroups()
    {
        return this.rptHdrGrps;
    }

    /**
    *** Sets the report header group at the specified column
    **/
    public ReportHeaderGroup getReportHeaderGroup(int col)
    {

        /* no report header groups? */
        if (ListTools.isEmpty(this.rptHdrGrps)) {
            return null;
        }

        /* search for column */
        for (ReportHeaderGroup rhg : this.rptHdrGrps) {
            int C = rhg.getColIndex();
            if (col == C) {
                return rhg;
            }
            // TODO: optimize
        }

        /* not found */
        return null;

    }

    // ------------------------------------------------------------------------
    // report columns

    /**
    *** Sets the report columns [initialized by ReportFactory.createReport(...)]
    **/
    public void setReportColumns(ReportColumn columns[])
    {
        this.rptColumns = (columns != null)? columns : EMPTY_COLUMNS;
    }

    /**
    *** Gets the report columns (does not return null)
    **/
    public ReportColumn[] getReportColumns()
    {
        return this.rptColumns;
    }

    /**
    *** Gets the report column count
    **/
    public int getColumnCount()
    {
        return this.rptColumns.length;
    }

    /**
    *** Returns true if this report has the named column
    **/
    public boolean hasReportColumn(String name)
    {
        if (!StringTools.isBlank(name)) {
            for (ReportColumn rc : this.rptColumns) {
                if (rc.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    // --------------------------------

    /**
    *** Sets the column names to include in the report (null/empty for all columns)
    *** (currently only called from "Service.java"
    **/
    public boolean setIncludeColumnNames(Set<String> colNames)
    {
        if (!ListTools.isEmpty(colNames)) {
            int colLen = 0;
            ReportColumn newCols[] = new ReportColumn[this.rptColumns.length];
            for (ReportColumn RC : this.rptColumns) {
                if (colNames.contains(RC.getName())) {
                    newCols[colLen++] = RC;
                }
            }
            // -- trim new column array
            if (colLen <= 0) {
                // -- no columns were chosen, leave as-is
                return false;
            } else 
            if (colLen < newCols.length) { 
                // -- new array is smaller (likely)
                this.rptColumns = ListTools.toArray(newCols,0,colLen);
                return true;
            } else {
                // -- all columns selected, leave as-is
                return true;
            }
        } else {
            // -- no columns to include
            return false;
        }
    }

    // --------------------------------

    /**
    *** Sets the column names to omit from the report
    **/
    public boolean setOmitColumnNames(Set<String> colNames)
    {
        if (!ListTools.isEmpty(colNames)) {
            int colLen = 0; // count of columns to retain
            ReportColumn newCols[] = new ReportColumn[this.rptColumns.length];
            for (ReportColumn RC : this.rptColumns) {
                if (!colNames.contains(RC.getName())) {
                    newCols[colLen++] = RC;
                }
            }
            // -- trim new column array
            if (colLen <= 0) {
                // -- all columns were omitted. Not allowed, leave as-is
                return false;
            } else 
            if (colLen < newCols.length) { 
                // -- new array is smaller (likely)
                this.rptColumns = ListTools.toArray(newCols,0,colLen);
                return true;
            } else {
                // -- no columns omitted, leave as-is
                return true;
            }
        } else {
            // -- no columns to omit
            return false;
        }
    }

    // --------------------------------

    /**
    *** Dynamic checking for whether a specific column should be displayed.
    *** (callback from "HeaderRowTemplate.java" and "BodyRowTemplate.java")
    **/
    public boolean showColumn(ReportColumn rtpCol)
    {
        return true;
    }

    // ------------------------------------------------------------------------
    // ReportOption

    /**
    *** Returns true if this report has report options
    **/
    public boolean hasReportOption()
    {
        return (this.reportOption != null);
    }

    /**
    *** Gets the report options
    **/
    public ReportOption getReportOption()
    {
        return this.reportOption;
    }

    /**
    *** Sets the report options
    **/
    public void setReportOption(ReportOption rptOpt)
    {
        this.reportOption = rptOpt;
        this.reportProperties = null;
    }

    // ------------------------------------------------------------------------
    // set constraints used for retrieving EventData records

    /**
    *** Sets the ReportConstraints for this report
    *** @param rc  The ReportConstraints
    **/
    public void setReportConstraints(ReportConstraints rc)
    {
        // This is a clone of the ReportConstraints found in the report factory
        // This ReportConstraints object is owned only by this specific report and may
        // be modified if necessary.
        this.rptConstraints = rc;
    }

    /**
    *** Gets the ReportConstraints for this report
    *** @return The ReportConstraints
    **/
    public ReportConstraints getReportConstraints()
    {
        if (this.rptConstraints == null) {
            // this should never occur, but return a default report constraints anyway
            this.rptConstraints = new ReportConstraints();  // should never occur!
        }
        return this.rptConstraints;
    }

    // ------------------------------------------------------------------------
    // Enable report fields

    /** 
    *** Return true if report data fields should be enabled.<br>
    *** Report data fields are those that are calculated as the report is
    *** generated. (not used by all report types).
    *** @return True if the report data fields should be enabled
    **/
    public boolean getReportDataFieldsEnabled()
    {
        return this.getProperties().getBoolean(PROP_reportDataFieldEnabled,REPORT_DATA_FIELDS_ENABLED);
    }

    // ------------------------------------------------------------------------
    // GPS AgeColorRange

    // ReportLayout.ParseAgeColorRange("1200:#550000,3600:#BB0000",null)
    public static final ReportLayout.AgeColorRange GpsAgeColorRangeDefault[] = new ReportLayout.AgeColorRange[] {
        new ReportLayout.AgeColorRange(3600, "#BB0000", "italic"), // 60 minutes
        new ReportLayout.AgeColorRange(1200, "#550000", null    ), // 20 minutes
    };

    /**
    *** Gets the GPS age color range array
    **/
    public ReportLayout.AgeColorRange[] getGpsAgeColorRangeArray()
    {
        RTProperties rtp = this.getProperties();
        Object gacra = rtp.getProperty(PROP_gpsAgeColorRange_array,null);
        if (!(gacra instanceof ReportLayout.AgeColorRange[])) {
            String gacrl = rtp.getString(PROP_gpsAgeColorRange,null);
            gacra = ReportLayout.ParseAgeColorRange(gacrl,GpsAgeColorRangeDefault);
            if (gacra == null) {
                gacra = new ReportLayout.AgeColorRange[0];
            }
            rtp.setProperty(PROP_gpsAgeColorRange_array,gacra); // cache
        }
        return (ReportLayout.AgeColorRange[])gacra;
    }

    /**
    *** Gets the GPS age color for the specified age
    **/
    public ReportLayout.AgeColorRange getGpsAgeColorRange(long age)
    {
        ReportLayout.AgeColorRange acra[] = this.getGpsAgeColorRangeArray();
        return ReportLayout.GetAgeColorRange(age, acra);
    }

    // ------------------------------------------------------------------------
    // Creation AgeColorRange

    // ReportLayout.ParseAgeColorRange("1200:#550000,3600:#BB0000",null)
    public static final ReportLayout.AgeColorRange CreationAgeColorRangeDefault[] = new ReportLayout.AgeColorRange[] {
        // empty
    };

    /**
    *** Gets the creation age color range array
    **/
    public ReportLayout.AgeColorRange[] getCreationAgeColorRangeArray()
    {
        RTProperties rtp = this.getProperties();
        Object cacra = rtp.getProperty(PROP_creationAgeColorRange_array,null);
        if (!(cacra instanceof ReportLayout.AgeColorRange[])) {
            String cacrl = rtp.getString(PROP_creationAgeColorRange,null);
            cacra = ReportLayout.ParseAgeColorRange(cacrl,CreationAgeColorRangeDefault);
            if (cacra == null) {
                cacra = new ReportLayout.AgeColorRange[0];
            }
            rtp.setProperty(PROP_creationAgeColorRange_array,cacra); // cache
        }
        return (ReportLayout.AgeColorRange[])cacra;
    }

    /**
    *** Gets the creation age color for the specified age
    **/
    public ReportLayout.AgeColorRange getCreationAgeColorRange(long age)
    {
        ReportLayout.AgeColorRange acra[] = this.getCreationAgeColorRangeArray();
        return ReportLayout.GetAgeColorRange(age, acra);
    }

    // ------------------------------------------------------------------------
    // CheckIn AgeColorRange

    // ReportLayout.ParseAgeColorRange("86400:#BB0000",null)
    public static final ReportLayout.AgeColorRange CheckinAgeColorRangeDefault[] = new ReportLayout.AgeColorRange[] {
        new ReportLayout.AgeColorRange(DateTime.HourSeconds(24), "#BB0000", null), // 24 hours
    };

    /**
    *** Gets the check-in age color range array
    **/
    public ReportLayout.AgeColorRange[] getCheckinAgeColorRangeArray()
    {
        RTProperties rtp = this.getProperties();
        Object cacra = rtp.getProperty(PROP_checkinAgeColorRange_array,null);
        if (!(cacra instanceof ReportLayout.AgeColorRange[])) {
            String cacrl = rtp.getString(PROP_checkinAgeColorRange,null);
            cacra = ReportLayout.ParseAgeColorRange(cacrl,CheckinAgeColorRangeDefault);
            if (cacra == null) {
                cacra = new ReportLayout.AgeColorRange[0];
            }
            rtp.setProperty(PROP_checkinAgeColorRange_array,cacra); // cache
        }
        return (ReportLayout.AgeColorRange[])cacra;
    }

    /**
    *** Gets the check-in age color for the specified age
    **/
    public ReportLayout.AgeColorRange getCheckinAgeColorRange(long age)
    {
        ReportLayout.AgeColorRange acra[] = this.getCheckinAgeColorRangeArray();
        return ReportLayout.GetAgeColorRange(age, acra);
    }

    // ------------------------------------------------------------------------
    // Login AgeColorRange

    // ReportLayout.ParseAgeColorRange("604800:#AA9700,2592000:#DD0000",null)
    public static final ReportLayout.AgeColorRange LoginAgeColorRangeDefault[] = new ReportLayout.AgeColorRange[] {
        new ReportLayout.AgeColorRange(DateTime.DaySeconds( 7), "#AA9700", null), // 1 week (yellow)
        new ReportLayout.AgeColorRange(DateTime.DaySeconds(30), "#DD0000", null), // 1 month (red)
    };

    /**
    *** Gets the log-in age color range array
    **/
    public ReportLayout.AgeColorRange[] getLoginAgeColorRangeArray()
    {
        RTProperties rtp = this.getProperties();
        Object cacra = rtp.getProperty(PROP_loginAgeColorRange_array,null);
        if (!(cacra instanceof ReportLayout.AgeColorRange[])) {
            String cacrl = rtp.getString(PROP_loginAgeColorRange,null);
            cacra = ReportLayout.ParseAgeColorRange(cacrl,LoginAgeColorRangeDefault);
            if (cacra == null) {
                cacra = new ReportLayout.AgeColorRange[0];
            }
            rtp.setProperty(PROP_loginAgeColorRange_array,cacra); // cache
        }
        return (ReportLayout.AgeColorRange[])cacra;
    }

    /**
    *** Gets the log-in age color for the specified age
    **/
    public ReportLayout.AgeColorRange getLoginAgeColorRange(long age)
    {
        ReportLayout.AgeColorRange acra[] = this.getLoginAgeColorRangeArray();
        return ReportLayout.GetAgeColorRange(age, acra);
    }

    // ------------------------------------------------------------------------
    // -- The following allows the specific report to override any of the defined constraints

    /**
    *** Returns the 'rule' selector constraint
    *** @return The 'rule' selector constraint
    **/
    public String getRuleSelector()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getRuleSelector();
    }

    /**
    *** Returns the 'WHERE' selector constraint
    *** @return The 'WHERE' selector constraint
    **/
    public String getWhereSelector()
    {
        ReportConstraints rc = this.getReportConstraints();
        String wh = rc.getWhere();
        if (this.hasReportOption()) {
            ReportOption ro = this.getReportOption();
            wh = StringTools.replaceKeys(wh, ro.getProperties());
        }
        return wh;
    }

    // --------------------------------

    /** 
    *** Returns the selection limit type constraint
    *** @return The selection limit type constraint
    **/
    public EventData.LimitType getSelectionLimitType()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getSelectionLimitType();
    }
    
    /** 
    *** Returns the selection limit constraint.
    *** @return The selection limit constraint
    **/
    public long getSelectionLimit()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getSelectionLimit();
    }

    /** 
    *** Returns the report limit constraint.
    *** @return The report limit constraint
    **/
    public long getReportLimit()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getReportLimit();
    }

    // --------------------------------

    /**
    *** Returns true if the report time-start is required
    *** @return True if the report time-start is required
    **/
    public boolean getRequiresTimeStart()
    {
        return true;
    }

    /**
    *** Returns true if the report time-end is required
    *** @return True if the report time-end is required
    **/
    public boolean getRequiresTimeEnd()
    {
        return true;
    }

    // --------------------------------

    /** 
    *** Returns the time start constraint
    *** @return The time start constraint
    **/
    public long getTimeStart()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getTimeStart();
    }

    /** 
    *** Returns the time end constraint
    *** @return The time end constraint
    **/
    public long getTimeEnd()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getTimeEnd();
    }

    // --------------------------------

    /** 
    *** Returns the "valid GPS required" constraint
    *** @return The "valid GPS required" constraint
    **/
    public boolean getValidGPSRequired()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getValidGPSRequired();
    }

    /** 
    *** Returns the status codes constraint
    *** @return The status codes constraint
    **/
    public int[] getStatusCodes()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getStatusCodes();
    }

    /** 
    *** Returns true if the data records are to be in ascending order
    *** @return True if the data records are to be in ascending order
    **/
    public boolean getOrderAscending()
    {
        ReportConstraints rc = this.getReportConstraints();
        return rc.getOrderAscending();
    }

    // ------------------------------------------------------------------------
    // ReportCallback
    
    /**
    *** Gets the ReportCallback instance (if specified)
    *** @return The ReportCallback instance, or null if not set
    **/
    public ReportCallback getReportCallback()
    {
        return this.rptCallback;
    }
    
    /**
    *** Sets the ReportCallback instance
    *** @param rptCB The ReportCallback instance
    **/
    public void setReportCallback(ReportCallback rptCB)
        throws ReportException
    {
        this.rptCallback = rptCB;
        if (this.rptCallback != null) {
            this.rptCallback.setReport(this);
        }
    }

    // ------------------------------------------------------------------------
    // EventData record retrieval

    /**
    *** Creates and returns an iterator over the EventData records based on the 
    *** defined selection criteria
    *** @return The EventData row data iterator
    **/
    /*
    public DBDataIterator getEventDataIterator()
    {
        EventData ed[] = this.getEventData_DeviceList(null);
        return new ArrayDataIterator(ed); // 'EventDataLayout' expects EventData[]
    }
    */

    /**
    *** Returns an array EventData records based on the predefined ReportDeviceList and constraints
    *** @param rcdHandler   The callback DBRecordHandler.  If specified, the returned EventData
    ***                     array may be null.
    *** @return An array of EventData records for the device (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] getEventData_DeviceList(DBRecordHandler<EventData> rcdHandler)
    {
        long rptLimit = this.getReportLimit(); // report record limit

        /* EventData record accumulator */
        java.util.List<EventData> edList = new Vector<EventData>();

        /* iterate through devices */
        this.maxEventDataCount = 0;
        ReportDeviceList devList = this.getReportDeviceList();
        for (Iterator<String> i = devList.iterator(); i.hasNext();) {
            String devID = i.next();
            this.eventDataCount  = 0; // per device
            this.eventMatchCount = 0; // per device

            /* have we reached our limit? */
            if ((rptLimit >= 0L) && (edList.size() >= rptLimit)) {
                break;
            }
            // -- there is room for at least one more record

            /* get device records */
            try {
                Device device  = devList.getDevice(devID);
                EventData ed[] = this._getEventData_Device(device, null, rcdHandler); // may be empty
                if (rptLimit < 0L) {
                    // -- no limit: add all of new EventData records to list
                    ListTools.toList(ed, edList);
                } else {
                    int maxRcds = (int)rptLimit - edList.size(); // > 0
                    if (ed.length <= maxRcds) {
                        // -- under limit: add all of new EventData records to list
                        ListTools.toList(ed, edList);
                    } else {
                        // -- clip to limit
                        ListTools.toList(ed, 0, maxRcds, edList);
                    }
                }
            } catch (DBException dbe) {
                Print.logError("Error retrieving EventData for Device: " + devID);
            }

            /* maximum selected EventData records */
            if (this.eventDataCount > this.maxEventDataCount) {
                this.maxEventDataCount = this.eventDataCount;
            }

        }
        return edList.toArray(new EventData[edList.size()]);
    }

    /**
    *** Returns an array EventData records for the specified Device
    *** @param deviceDB     The Device for which EventData records will be selected
    *** @param rcdHandler   The callback DBRecordHandler.  If specified, the returned EventData
    ***                     array may be null.
    *** @return An array of EventData records for the device (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] getEventData_Device(Device deviceDB, DBRecordHandler<EventData> rcdHandler)
    {
        this.eventDataCount  = 0; // per device
        this.eventMatchCount = 0; // per device
        EventData ed[] = this._getEventData_Device(deviceDB, null, rcdHandler);
        this.maxEventDataCount = this.eventDataCount;
        return ed;
    }

    /**
    *** Returns an array EventData records for the specified Device
    *** @param deviceDB     The Device for which EventData records will be selected
    *** @param timeStart    The Start time
    *** @param timeEnd      The End time
    *** @param rcdHandler   The callback DBRecordHandler.  If specified, the returned EventData
    ***                     array may be null.
    *** @return An array of EventData records for the device (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] getEventData_Device(Device deviceDB, 
        long timeStart, long timeEnd,
        DBRecordHandler<EventData> rcdHandler)
    {
        this.eventDataCount  = 0; // per device
        this.eventMatchCount = 0; // per device
        long ts = (timeStart > 0L)? timeStart : this.getTimeStart();
        long te = (timeEnd   > 0L)? timeEnd   : this.getTimeEnd();
        EventData ed[] = this._getEventData_Device(deviceDB, ts, te, null, rcdHandler);
        this.maxEventDataCount = this.eventDataCount;
        return ed;
    }

    /**
    *** Returns an array EventData records for the specified Driver
    *** @param driverID     The DriverID for which EventData records will be selected
    *** @param rcdHandler   The callback DBRecordHandler.  If specified, the returned EventData
    ***                     array may be null.
    *** @return An array of EventData records for the driver (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] getEventData_Driver(String driverID, DBRecordHandler<EventData> rcdHandler)
    {
        String accountID = this.getAccountID();
        this.eventDataCount  = 0; // per device
        this.eventMatchCount = 0; // per device
        if (!StringTools.isBlank(driverID)) {
            Print.logInfo("Getting EventData records by Driver: " + accountID + "/" + driverID);
            DBWhere dwh = new DBWhere(EventData.getFactory());
            dwh.append(dwh.EQ(EventData.FLD_driverID, driverID));
            EventData ed[] = this._getEventData_Device(null/*Device*/, dwh.toString(), rcdHandler);
            this.maxEventDataCount = this.eventDataCount;
            return ed;
        } else {
            Print.logWarn("DriverID is null/blank: " + accountID);
            return new EventData[0];
        }
    }

    /**
    *** Callback for each EventData record selected.  This method can be overridden by
    *** the subclass to allow for additional criteria selection.
    *** @param ev  The current EventData record to test
    *** @return True to accept record, false to skip record
    **/
    protected boolean isEventDataMatch(EventData ev)
    {
        return true;
    }

    // ------------------------------------------------------------------------
    // read EventData records (based on Device)

    private static class LastEventData
    {
        private EventData event = null;
        public void setEvent(EventData ev) { this.event = ev; }
        public EventData getEvent() { return this.event; }
    }

    /**
    *** Returns an array EventData records for the specified Device
    *** @param deviceDB     The Device for which EventData records will be selected
    *** @param rcdHandler   The callback DBRecordHandler.  If specified, the returned EventData
    ***                     array may be null.
    *** @return An array of EventData records for the device (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] _getEventData_Device(final Device deviceDB, 
        String addtlWhereSelect_1,
        final DBRecordHandler<EventData> rcdHandler)
    {
        long timeStart = this.getTimeStart();
        long timeEnd   = this.getTimeEnd();
        return this._getEventData_Device(deviceDB, 
            timeStart, timeEnd, 
            addtlWhereSelect_1, 
            rcdHandler);
    }

    /**
    *** Returns an array EventData records for the specified Device
    *** @param deviceDB            The Device for which EventData records will be selected
    *** @param timeStart           Selection Start time
    *** @param timeEnd             Selection End time
    *** @param addtlWhereSelect_1  Additional "Where" selection criteria
    *** @param rcdHandler          The callback DBRecordHandler.  If specified, the returned 
    ***                            EventData array may be null.
    *** @return An array of EventData records for the device (may be null if a callback
    ***         DBRecordHandler has been specified).
    **/
    protected EventData[] _getEventData_Device(final Device deviceDB, 
        long timeStart, long timeEnd,
        String addtlWhereSelect_1,
        final DBRecordHandler<EventData> rcdHandler)
    {
        String addtlWhereSelect_2 = this.getWhereSelector();
        //Print.logInfo("Additional Where #2: " + addtlWhereSelect_2);

        /* Device */
        // -- a null device may be allowed to for getting driver associated events
        if ((deviceDB == null) && 
            StringTools.isBlank(addtlWhereSelect_1) && 
            StringTools.isBlank(addtlWhereSelect_2)) {
            // -- no device, with a blank additional-select, would select too many records
            Print.logWarn("Device not specified and no additional 'WHERE' selection specified.");
            return EventData.EMPTY_ARRAY;
        }

        /* Account */
        String accountID = this.getAccountID();
        //Print.logInfo("Getting EventData for " + accountID + "/" + deviceID);

        /* EventData rule selector (RuleFactory support required) */
        final String ruleSelector = this.getRuleSelector();
        final RuleFactory ruleFact;
        if (!StringTools.isBlank(ruleSelector)) {
            //Print.logInfo("Constraint Rule Selector: " + ruleSelector);
            ruleFact = Device.getRuleFactory();
            if (ruleFact == null) {
                Print.logWarn("RuleSelector not supported");
            }
        } else {
            //Print.logInfo("No Constraint Rule Selector");
            ruleFact = null;
        }

        /* create record handler */
        final LastEventData lastEDR = new LastEventData(); 
        DBRecordHandler<EventData> evRcdHandler = new DBRecordHandler<EventData>() {
            public int handleDBRecord(EventData rcd) throws DBException {
                //Print.logInfo("Read EventData: " + rcd);
                ReportData.this.eventDataCount++;
                EventData ev = rcd;
                // -- chain events together
                EventData lastEv = lastEDR.getEvent(); // may be null
                ev.setPreviousEventData(lastEv); // may set null
                lastEDR.setEvent(ev);
                // -- set the Device instance for this EventData
                if (deviceDB != null) {
                    // -- (assume Account/Device match) cache Device
                    ev.setDevice(deviceDB);
                    // -- TODO: mark device as having had an event
                } else 
                if (lastEv != null) {
                    // -- try getting Device from last event
                    String A = lastEv.getAccountID();
                    String D = lastEv.getDeviceID();
                    if (ev.getAccountID().equals(A) && ev.getDeviceID().equals(D)) {
                        // -- Account/Device match, try getting Device from last event
                        Device dev = lastEv.getDevice(); // may force DB query
                        if (dev != null) {
                            ev.setDevice(dev);
                        } else {
                            // -- last Device instance is null
                        }
                    } else {
                        // -- last event does not match this event
                    }
                }
                // -- calculate report distance
                if (ReportData.this.getReportDataFieldsEnabled() && (lastEv != null)) {
                    ev.calculateReportDistance(lastEv);
                }
                // -- check match 
                if (!ReportData.this.isEventDataMatch(ev)) {
                    // -- no match: skip this event
                    // -  TODO: remove this event from the EventData previous-event chain?
                    return DBRH_SKIP;
                } else
                if ((ruleFact != null) && !ruleFact.isSelectorMatch(ruleSelector,ev)) {
                    // -- no match: skip this event
                    // -  TODO: remove this event from the EventData previous-event chain?
                    return DBRH_SKIP;
                }
                // -- mark device as having had a match?
                ReportData.this.eventMatchCount++;
                // -  TODO:
                // -- check RecordHandler
                if (rcdHandler == null) {
                    // -- match, no default record handler 
                    return DBRH_SAVE;
                } else {
                    // -- match, send to default record handler
                    try {
                        return rcdHandler.handleDBRecord(rcd);
                    } catch (DBException dbe) {
                        throw dbe; // re-throw DBException
                    } catch (Throwable th) {
                        Print.logException("RecordHandler callback exception", th);
                        return DBRH_STOP;
                    }
                }
            }
        };

        /* get events */
        EventData ed[] = null;
        try {
            String devID = (deviceDB != null)? deviceDB.getDeviceID() : null;
            //Print.logInfo("Reading EventData: dev="+deviceDB +", ts="+timeStart +", te="+timeEnd);
            ed = EventData.getRangeEvents(
                accountID, devID,
                timeStart, timeEnd,
                this.getStatusCodes(),
                this.getValidGPSRequired(),
                this.getSelectionLimitType(), this.getSelectionLimit(), this.getOrderAscending(),
                addtlWhereSelect_1, addtlWhereSelect_2,
                evRcdHandler);
        } catch (DBException dbe) {
            Print.logException("Unable to obtain EventData records", dbe);
        }

        /* no events? */
        if (ed == null) {
            return EventData.EMPTY_ARRAY;
        }

        /* update device */
        if (deviceDB != null) {
            // -- set device in each retrieved event
            for (int i = 0; i < ed.length; i++) {
                ed[i].setDevice(deviceDB);
            }
        }

        /* return events */
        return ed;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the actual counted EventData records from the last query (including all devices)
    **/
    public long getEventDataCount()
    {
        return (long)this.eventDataCount;
    }

    /**
    *** Returns the number of EventData records matched by "isEventDataMatch()", or the rule selector.
    **/
    public long getEventMatchCount()
    {
        return (long)this.eventMatchCount;
    }

    /**
    *** Return the largest counted EventData records from the last query for a single device 
    **/
    public long getMaximumEventDataCount()
    {
        return (long)this.maxEventDataCount;
    }

    /**
    *** Returns the count of EventData records based on the EventData constraints 
    **/
    protected long countEventData(Device deviceDB)
    {
        long timeStart = this.getTimeStart();
        long timeEnd   = this.getTimeEnd();
        return this._countEventData(deviceDB, timeStart, timeEnd);
    }

    /**
    *** Returns the count of EventData records based on the EventData constraints 
    **/
    protected long countEventData(Device deviceDB, long timeStart, long timeEnd)
    {
        return this._countEventData(deviceDB, timeStart, timeEnd);
    }

    /* return the count of EventData records based on the EventData constraints */
    //protected long _countEventData(Device deviceDB)
    //{
    //    long timeStart = this.getTimeStart();
    //    long timeEnd   = this.getTimeEnd();
    //    return this._countEventData(deviceDB, timeStart, timeEnd);
    //}

    /**
    *** Returns the count of EventData records based on the EventData constraints 
    **/
    protected long _countEventData(Device deviceDB, long timeStart, long timeEnd)
    {

        /* Device */
        if (deviceDB == null) {
            return 0L;
        }

        /* Account */
        String accountID = this.getAccountID();
         //Print.logInfo("Getting EventData for " + accountID + "/" + deviceID);

        /* EventData rule selector */
        // (not supported)
        final String ruleSelector = this.getRuleSelector();
        if ((ruleSelector != null) && !ruleSelector.equals("")) {
            Print.logWarn("RuleSelector not supported when obtaining EventData record counts!");
        }

        /* get events */
        long recordCount = 0L;
        try {
            recordCount = EventData.countRangeEvents(
                accountID, deviceDB.getDeviceID(),
                timeStart, timeEnd,
                this.getStatusCodes(),
                this.getValidGPSRequired(),
                this.getSelectionLimitType(), this.getSelectionLimit(),
                this.getWhereSelector());
        } catch (DBException dbe) {
            Print.logException("Unable to obtain EventData record count", dbe);
        }
        
        /* return events */
        return recordCount;
        
    }

    // ------------------------------------------------------------------------
    // Report Reord Count

    /**
    *** Sets the report record count
    **/
    public void setReportRecordCount(int count, boolean isPartial)
    {
        this.rptRecordCount = count;
        this.rptIsPartial   = isPartial;
    }

    /**
    *** Gets the report record count
    **/
    public int getReportRecordCount()
    {
        return this.rptRecordCount;
    }

    /**
    *** Returns true if the actual number of report records would have exceeded
    *** the maximum nuber of allowed report records, indicating that this report
    *** contains only partial data.
    **/
    public boolean getReportIsPartial()
    {
        return this.rptIsPartial;
    }

    // ------------------------------------------------------------------------
    // Auto Report URL

    /** 
    *** Set the auto report URL
    **/
    public void setAutoReportURL(URIArg autoReportURL)
    {
        this.autoReportURL = autoReportURL;
    }

    /** 
    *** Get the auto report URL
    **/
    public URIArg getAutoReportURL()
    {
        return this.autoReportURL;
    }

    // ------------------------------------------------------------------------
    // Graph

    /**
    *** Returns true if this report supports displaying a graph
    *** @return True if this report supports displaying a graph, false otherwise
    **/
    public boolean getSupportsGraphDisplay()
    {
        // -- override in subclass
        return false;
    }

    /**
    *** Writes any required report JavaScript
    **/
    public void writeJavaScript(PrintWriter pw, RequestProperties reqState)
        throws IOException
    {
        // -- override in subclass
    }

    /**
    *** Writes the report html body content
    **/
    public void writeHtmlBody(PrintWriter pw, RequestProperties reqState)
        throws IOException
    {
        // -- override in subclass
    }

    /**
    *** Gets the Graph link description (if any)
    **/
    public String getGraphLinkDescription()
    {
        return null;
    }

    /**
    *** Gets the Graph window size
    **/
    public MapDimension getGraphWindowSize()
    {
        return new MapDimension(730,440);
    }

    // ------------------------------------------------------------------------
    // Graph URL

    /**
    *** Sets the Graph image URL (OBSOLETE)
    **/
    public void setGraphImageURL(URIArg graphURL)
    {
        if (this.getSupportsGraphDisplay()) {
            this.graphImageURL = graphURL;
        }
    }

    /**
    *** Sets the Graph image URL (OBSOLETE)
    **/
    public URIArg getGraphImageURL() // OBSOLETE
    {
        return this.getSupportsGraphDisplay()? this.graphImageURL : null;
    }

    // ------------------------------------------------------------------------
    // Map URL

    /**
    *** Returns true if this report supports displaying a map
    *** @return True if this report supports displaying a map, false otherwise
    **/
    public boolean getSupportsMapDisplay()
    {
        // -- override in subclass
        return false;
    }

    /** 
    *** Returns true if the map route-line is to be displayed, false otherwise
    *** @param isFleet  True if this maps represents a Group/Fleet of devices
    **/
    public boolean showMapRouteLine(boolean isFleet)
    {
        // -- default to show route-line if not a Fleet/Group map
        return isFleet? false : true;
    }

    /**
    *** Sets the Map URL
    **/
    public void setMapURL(URIArg mapURL)
    {
        if (this.getSupportsMapDisplay()) {
            this.mapURL = mapURL;
            //Print.logInfo("Map URL: " + this.mapURL);
        }
    }

    /**
    *** Gets the Map URL
    **/
    public URIArg getMapURL()
    {
        return this.getSupportsMapDisplay()? this.mapURL : null;
    }
    
    /**
    *** Returns true if the Map URL has been defined
    **/
    public boolean hasMapURL()
    {
        return !StringTools.isBlank(this.getMapURL())? true : false;
    }

    /**
    *** Gets the Map link description
    **/
    public String getMapLinkDescription()
    {
        return null;
    }

    /**
    *** Gets the Map window size
    **/
    public MapDimension getMapWindowSize()
    {
        return new MapDimension(700,500);
    }

    // ------------------------------------------------------------------------
    // KML URL

    /**
    *** Returns true if this report supports displaying KML
    *** @return True if this report supports displaying KML, false otherwise
    **/
    public boolean getSupportsKmlDisplay()
    {
        // -- override in subclass
        return false;
    }

    /**
    *** Sets the Google KML URL
    **/
    public void setKmlURL(URIArg kmlURL)
    {
        if (this.getSupportsKmlDisplay()) {
            this.kmlURL = kmlURL;
            //Print.logInfo("KML URL: " + this.kmlURL);
        }
    }

    /**
    *** Gets the Google KML URL
    **/
    public URIArg getKmlURL()
    {
        return this.getSupportsKmlDisplay()? this.kmlURL : null;
    }

    /**
    *** Gets the Google KML link description
    **/
    public String getKmlLinkDescription()
    {
        return null;
    }

    // ------------------------------------------------------------------------
    // Refresh URL

    /**
    *** Returns true id the refresh-url has been defined
    **/
    public boolean hasRefreshURL()
    {
        return !StringTools.isBlank(this.refreshURL)? true : false;
    }

    /**
    *** Sets the report refresh URL
    **/
    public void setRefreshURL(URIArg refreshURL)
    {
        this.refreshURL = refreshURL;
        //Print.logInfo("Refresh URL: " + this.refreshURL);
    }

    /**
    *** Gets the report refresh URL
    **/
    public URIArg getRefreshURL()
    {
        return this.refreshURL;
    }

    // ------------------------------------------------------------------------
    // Start report
    
    /**
    *** This method is called after all other ReportConstraints have been set.
    *** The report has this opportunity to make any changes to the ReportConstraints
    *** before the report is actually generated
    **/
    public void postInitialize()
    {
        // last oportunity for the report to configure itself before actually writing out data
        // To prevent requireing that the subclass call "super.postInitialize()" it is
        // strongly recommended that this placeholder method always be empty.
    }

    // ------------------------------------------------------------------------
    // ReportLayout

    /**
    *** Gets the report layout
    **/
    public abstract ReportLayout getReportLayout();

    // ------------------------------------------------------------------------
    // DataRow

    /** 
    *** Gets the DataRowTemplate instance
    **/
    public DataRowTemplate getDataRowTemplate()
    {
        return this.getReportLayout().getDataRowTemplate();
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes report style attributes
    **/
    public void writeReportStyle(String format, OutputProvider out)
        throws ReportException
    {
        String fmt = StringTools.blankDefault(format, this.getPreferredFormat());
        this.getReportLayout().writeReportStyle(fmt, this, out, 0);
    }

    /**
    *** Writes the report
    **/
    public int writeReport(String format, OutputProvider out)
        throws ReportException
    {
        String fmt = StringTools.blankDefault(format, this.getPreferredFormat());
        return this.getReportLayout().writeReport(fmt, this, out, 0);
    }

    /**
    *** Writes the report
    **/
    public int writeReport(String format, OutputProvider out, int indentLevel)
        throws ReportException
    {
        String fmt = StringTools.blankDefault(format, this.getPreferredFormat());
        return this.getReportLayout().writeReport(fmt, this, out, indentLevel);
    }

    // ------------------------------------------------------------------------
    // DBDataIterator

    /**
    *** Gets the details data interator for this report.<br>
    *** The subclass of this object must implement this method.<br>
    *** For simple EventData record data, this method could simply return:<br>
    ***   new ArrayDataIterator(this.getEventData_DeviceList());
    **/
    public abstract DBDataIterator getBodyDataIterator();

    /**
    *** Gets the totals data interator for this report.<br>
    *** The subclass of this object must implement this method.<br>
    *** For simple EventData record data, this method may simply return null.
    **/
    public abstract DBDataIterator getTotalsDataIterator();

    /**
    *** This is an implementation of DBDataIterator that iterates through an array of row objects
    **/
    public class ArrayDataIterator
        implements DBDataIterator
    {
        private int recordIndex = -1;
        private Object    data[]   = null;
        private Object    dataObj  = null;
        private DBDataRow dataRow  = null;

        public ArrayDataIterator(Object data[]) {
            this.data = data;
            this.recordIndex = -1;
            this.dataRow = new DBDataRowAdapter(ReportData.this) {
                public Object getRowObject() {
                    return ArrayDataIterator.this.dataObj;
                }
                public Object getDBValue(String name, int rowNdx, ReportColumn rptCol) {
                    Object obj = ArrayDataIterator.this.dataObj;
                    if (obj != null) {
                        DataRowTemplate drt = ReportData.this.getDataRowTemplate();
                        return drt.getFieldValue(name, rowNdx, ReportData.this, rptCol, obj); // DataRowTemplate.getFieldValue
                    } else {
                        return "";
                    }
                }
            };
        }
        
        public Object[] getArray() {
            return this.data;
        }

        public boolean hasNext() {
            return (this.data != null) && ((this.recordIndex + 1) < this.data.length);
        }

        public DBDataRow next() {
            if (this.hasNext()) {
                this.recordIndex++;
                this.dataObj = this.data[this.recordIndex];
                return this.dataRow;
            } else {
                this.dataObj = null;
                return null;
            }
        }
        
    }

    /**
    *** This is an implementation of DBDataIterator that iterates through an array of row objects 
    **/
    protected class ListDataIterator
        implements DBDataIterator
    {
        private Iterator<?> dataIter = null;
        private Object      dataObj  = null;
        private DBDataRow   dataRow  = null;
        
        public ListDataIterator(java.util.List<?> data) {
            this.dataIter = (data != null)? data.iterator() : null;
            this.dataRow = new DBDataRowAdapter(ReportData.this) {
                public Object getRowObject() {
                    return ListDataIterator.this.dataObj;
                }
                public Object getDBValue(String name, int rowNdx, ReportColumn rptCol) {
                    Object obj = ListDataIterator.this.dataObj;
                    if (obj != null) {
                        DataRowTemplate rdp = ReportData.this.getDataRowTemplate();
                        return rdp.getFieldValue(name, rowNdx, ReportData.this, rptCol, obj); // DataRowTemplate.getFieldValue
                    } else {
                        return "";
                    }
                }
            };
        }

        public boolean hasNext() {
            return (this.dataIter != null) && this.dataIter.hasNext();
        }

        public DBDataRow next() {
            if (this.hasNext()) {
                this.dataObj = this.dataIter.next();
                return this.dataRow;
            } else {
                this.dataObj = null;
                return null;
            }
        }

    }

    // ------------------------------------------------------------------------
    
}
