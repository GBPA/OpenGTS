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
// Last know location
// ----------------------------------------------------------------------------
// Change History:
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/06/03  Martin D. Flynn
//     -Added PrivateLabel to constructor
//  2014/12/16  Martin D. Flynn
//     -Added support for selecting first matching event for a device.
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

public class EventSummaryReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Summary report
    // 1 EventData record per device
    // As-of date ('To' date only)
    // ------------------------------------------------------------------------

    private static final String PROP_ignoreReportStartTime = "ignoreReportStartTime";
    private static final String PROP_minimumCheckInAge     = "minimumCheckInAge";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* minimum checkin-age (used for "Last Known Location" report) */
    private long    minCheckInAgeSec    = 0L;

    /* traversing backwards? */
    private boolean traversingBackwards = false;

    /**
    *** Event Summary Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public EventSummaryReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportAccountException();
        }
        //if ((acct == null) || (this.getDeviceCount() < 1)) {
        //    throw new ReportDeviceException();
        //}
        // report on all authorized devices
        ////this.getReportDeviceList().addAllAuthorizedDevices();
    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {
        RTProperties rtp = this.getProperties();

        /* disregard report start time? */
        if (rtp.hasProperty(PROP_ignoreReportStartTime)) {
            // -- explicit "ignoreReportStartTime" specification
            if (rtp.getBoolean(PROP_ignoreReportStartTime,true)) {
                Print.logInfo("Ignoring report start time (per 'ignoreReportStartTime') ...");
                this.getReportConstraints().setTimeStart(-1L); // disregard 'start' time
            }
        } else {
            // -- default, disregard 'start' time if limit type is "LAST"
            if (EventData.LimitType.LAST.equals(this.getSelectionLimitType())) {
                Print.logInfo("Ignoring report start time (per 'LimitType.LAST') ...");
                this.getReportConstraints().setTimeStart(-1L); // disregard 'start' time
            }
        }

        /* minimum check-in age */
        this.minCheckInAgeSec = rtp.getLong(PROP_minimumCheckInAge,0L);

        /* traversing backwards? */
        this.traversingBackwards = (this.isSelectionLimitTypeLAST() && !this.getOrderAscending())? true : false;

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

    /**
    *** Callback for each EventData record selected.  This method can be overridden 
    *** by the subclass to allow for additional criteria selection.
    *** @param ev  The current EventData record to test
    *** @return True to accept record, false to skip record
    **/
    @Override
    protected boolean isEventDataMatch(EventData ev)
    {

        /* have we already matched an EventData record? */
        if (super.getEventMatchCount() >= 1L) {
            // -- match at most 1 event per device
            return false;
        }

        /* minimum check-in age */
        if (this.minCheckInAgeSec > 0L) {
            // -- select event iff no dev comm received in last "minCheckInAgeSec" seconds
            Device  dev = ev.getDevice(); // should not be null
            long lcTime = (dev != null)? dev.getLastTotalConnectTime() : 0L;
            long lcAge  = DateTime.getCurrentTimeSec() - lcTime; // last connect age
            if (lcAge < this.minCheckInAgeSec) {
                // -- device comm received in the last "minCheckInAgeSec" seconds
                // -  omit this event
                return false;
            } else {
                // -- no device comm in the last "minCheckInAgeSec" seconds
                // -  select this event
                return true;
            }
        }

        /* default select/match */
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {

        /* special record handler */
        DBRecordHandler<EventData> rcdHandler = null;
        if (this.traversingBackwards) {
            // -- starting at the end and traversing backwards
            rcdHandler = new DBRecordHandler<EventData>() {
                public int handleDBRecord(EventData rcd) throws DBException {
                    // -- TODO: handle "LastEvent" processing (iff "LAST" and "DESCENDING")
                    return DBRH_SAVE;
                }
            };
        }

        /* get events */
        EventData ed[] = this.getEventData_DeviceList(rcdHandler);

        /* TODO: add devices never connected? (ie. devices that are absent from "ed[]") */
        boolean ADD_DEVICES_NEVER_CONNECTED = false;
        if (ADD_DEVICES_NEVER_CONNECTED) {
            // -- TODO:
        }

        /* sort by device description and return */
        Arrays.sort(ed, EventData.getDeviceDescriptionComparator()); // sort by device description
        return new ArrayDataIterator(ed); // 'EventDataLayout' expects EventData[]

    }
 
    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        return null;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns the limit type contraint
    *** @return The limit type
    **/
    public EventData.LimitType getSelectionLimitType()
    {
        //return EventData.LimitType.LAST;
        return super.getSelectionLimitType();
    }

    /**
    *** Returns true if the selection limit type is LimitType.LAST
    **/
    public boolean isSelectionLimitTypeLAST()
    {
        EventData.LimitType limType = this.getSelectionLimitType();
        return EventData.LimitType.LAST.equals(limType);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the selection limit (events per device)
    *** @return The selection limit
    **/
    public long getSelectionLimit()
    {
        //return 1L;
        return super.getSelectionLimit();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the key order is ascending
    *** @return True if the key order is ascending
    **/
    public boolean getOrderAscending()
    {
        return super.getOrderAscending();
    }

    // ------------------------------------------------------------------------

}
