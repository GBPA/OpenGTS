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
//  2007/06/03  Martin D. Flynn
//     -Added PrivateLabel to constructor
//  2007/06/14  Martin D. Flynn
//     -Display appropriate error text when no devices have been specified for 
//      this report.
//  2015/08/16  Martin D. Flynn
//     -Added property "selectByWhereOnly" to allow selecting EventData records
//      based on the specified "where" only (device selection is ignored).
// ----------------------------------------------------------------------------
package org.opengts.war.report.event;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class EventDetailReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Detail report
    // Multiple EventData records per device
    // 'From'/'To' date
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Properties

    private static final String PROP_selectByWhereOnly  = "selectByWhereOnly"; // OBSOLETE
    private static final String PROP_selectEventsBy     = "selectEventsBy";    // where|driver|deviceList

    /* EventData selection type (PROP_selectEventsBy) */
    private static final String SelectBy_Where[]        = { "where" , "whereOnly" };
    private static final String SelectBy_Driver[]       = { "driver", "drivers"   };
    private static final String SelectBy_DeviceList[]   = { "device", "devices", "deviceList" };

    // ------------------------------------------------------------------------

    /**
    *** Event Detail Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public EventDetailReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);

        /* has account */
        if (this.getAccount() == null) {
            throw new ReportAccountException();
        }

        /* has at least one device */
        if (this.isSelectByDeviceList() && (this.getDeviceCount() <= 0)) {
            throw new ReportDeviceException();
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report handles only a single device at a time
    *** @return True If this report handles only a single device at a time
    **/
    public boolean isSingleDeviceOnly()
    {
        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this report supports displaying a map
    *** @return True if this report supports displaying a map, false otherwise
    **/
    public boolean getSupportsMapDisplay() // true
    {
        return true;
    }

    /**
    *** Returns true if this report supports displaying KML
    *** @return True if this report supports displaying KML, false otherwise
    **/
    public boolean getSupportsKmlDisplay()
    {
        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public static ReportLayout GetReportLayout()
    {
        // bind the report format to this data
        return EventDataLayout.getReportLayout();
    }

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public ReportLayout getReportLayout()
    {
        // bind the report format to this data
        return GetReportLayout();
    }

    // ------------------------------------------------------------------------

    private String getSelectBy()
    {
        // -- EventData record selection type [where|driver|deviceList]
        RTProperties rtp = super.getProperties();
        String selectBy = rtp.getString(PROP_selectEventsBy,null);
        return selectBy;
    }

    private boolean isSelectByWhere()
    {
        String selectBy = this.getSelectBy();
        return ListTools.containsIgnoreCase(SelectBy_Where,selectBy);
    }

    private boolean isSelectByDriver()
    {
        String selectBy = this.getSelectBy();
        return ListTools.containsIgnoreCase(SelectBy_Driver,selectBy);
    }

    private boolean isSelectByDeviceList()
    {
        String selectBy = this.getSelectBy();
        if (StringTools.isBlank(selectBy)) {
            return true;
        } else {
            return ListTools.containsIgnoreCase(SelectBy_DeviceList,selectBy);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {
        //return super.getEventDataIterator();

        /* EventData record selection type [where|driver|deviceList] */
        RTProperties rtp = super.getProperties();
        String selectBy = rtp.getString(PROP_selectEventsBy,null);

        /* get array of EventData records */
        EventData ed[] = null;
        DBRecordHandler<EventData> rcdHandler = null;
        if (this.isSelectByWhere()) {
            // -- select events based on "super.getWhereSelector()" only
            // -  will return an empty array if "getWhereSelector()" returns null/blank
            //Print.logInfo("Getting events by Device ...");
            ed = super.getEventData_Device(null/*Device*/, rcdHandler);
        } else
        if (this.isSelectByDriver()) {
            // -- select events by selected driver
            RequestProperties reqState = super.getRequestProperties();
            String driverID = reqState.getSelectedDriverID();
            if (StringTools.isBlank(driverID)) {
                ReportOption ro = this.getReportOption();
                driverID = (ro != null)? ro.getValue(Driver.FLD_driverID) : null;
            }
            Print.logInfo("Getting events by Driver: " + driverID);
            ed = super.getEventData_Driver(driverID, rcdHandler);
        } else
        if (this.isSelectByDeviceList()) {
            // -- select events based on Devices in "super.getReportDeviceList()"
            //Print.logInfo("Getting events by DeviceList ...");
            ed = super.getEventData_DeviceList(rcdHandler);
        } else {
            // -- default to select by device
            //Print.logInfo("Getting events by DeviceList ...");
            ed = super.getEventData_DeviceList(rcdHandler);
        }

        /* return iterator */
        return new ArrayDataIterator(ed); // 'EventDataLayout' expects EventData[]

    }
 
    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        // -- TODO: keep track of distance traveled for report?
        return null;
    }

    // ------------------------------------------------------------------------

}
