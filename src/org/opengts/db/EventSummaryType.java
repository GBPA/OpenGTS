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
//  EventSummaryType (EXPERIMENTAL)
// ----------------------------------------------------------------------------
// Change History:
//  2016/05/16  Martin D. Flynn
//     -Initial Release
//  2017/03/14  Martin D. Flynn
//     -Added support for STYPE_INPUT_ELAPSED*
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

/**
*** EventSummaryType class<br>
**/
public class EventSummaryType
{

    // ------------------------------------------------------------------------
    
    /* pre-calculated type */
    public static final int     TYPE_PRECALC_SUMMARY_DAILY      = 0x10000;
  //public static final int     TYPE_PRECALC_SUMMARY_WEEKLY     = 0x20000;
  //public static final int     TYPE_PRECALC_SUMMARY_MONTHLY    = 0x40000;

    /* Status Code counts */
    public static final int     TYPE_STATUSCODE_COUNT           = 0x00000;  // Count   [long]   
    public static final String  TYPE_STATUSCODE_NAME_           = "StatusCode:";

    /* Combined similar Status Code counts */
    public static final int     TYPE_SIMILARCODE_COUNT          = 0x00002;  // Count   [long]   
    public static final String  TYPE_SIMILARCODE_NAME_          = "SimilarCode:";

    /* Event counts */
    public static final int     TYPE_EVENT_COUNT                = 0x00010;  // Count   [long]
    public static final String  TYPE_EVENT_COUNT_NAME_          = "Count:";
    public static final int     STYPE_EVENT_TOTAL               = 0x00000;  // 
    public static final String  STYPE_EVENT_TOTAL_NAME          = TYPE_EVENT_COUNT_NAME_ + "Total";
    public static final int     STYPE_EVENT_VALID_GPS           = 0x00100;  // 
    public static final String  STYPE_EVENT_VALID_GPS_NAME      = TYPE_EVENT_COUNT_NAME_ + "ValidGPS";
    public static final int     STYPE_EVENT_INVALID_GPS         = 0x00110;  // 
    public static final String  STYPE_EVENT_INVALID_GPS_NAME    = TYPE_EVENT_COUNT_NAME_ + "InvalidGPS";
    public static final int     STYPE_EVENT_SPEEDING_1          = 0x00201;  //
    public static final String  STYPE_EVENT_SPEEDING_1_NAME     = TYPE_EVENT_COUNT_NAME_ + "Speeding-1";
    public static final int     STYPE_EVENT_SPEEDING_2          = 0x00202;  //
    public static final String  STYPE_EVENT_SPEEDING_2_NAME     = TYPE_EVENT_COUNT_NAME_ + "Speeding-2";
    public static final int     STYPE_EVENT_SPEEDING_3          = 0x00203;  //
    public static final String  STYPE_EVENT_SPEEDING_3_NAME     = TYPE_EVENT_COUNT_NAME_ + "Speeding-3";
    public static final int     STYPE_EVENT_SPEEDING_4          = 0x00204;  //
    public static final String  STYPE_EVENT_SPEEDING_4_NAME     = TYPE_EVENT_COUNT_NAME_ + "Speeding-4";
    public static final int     STYPE_EVENT_SPEEDING_UNK        = 0x0020F;  //
    public static final String  STYPE_EVENT_SPEEDING_UNK_NAME   = TYPE_EVENT_COUNT_NAME_ + "Speeding-U";

    /* Elapsed time (seconds) */
    public static final int     TYPE_ELAPSED_TIME               = 0x00020;  // Seconds [long]
    public static final String  TYPE_ELAPSED_TIME_NAME_         = "Elapsed:";
    public static final int     STYPE_MOVING_ELAPSED            = 0x00100;  //  (Speed > 0)
    public static final String  STYPE_MOVING_ELAPSED_NAME       = TYPE_ELAPSED_TIME_NAME_ + "Moving";
    public static final int     STYPE_STOPPED_ELAPSED           = 0x00110;  //  (Speed <= 0)
    public static final String  STYPE_STOPPED_ELAPSED_NAME      = TYPE_ELAPSED_TIME_NAME_ + "Stopped";
    public static final int     STYPE_SPEEDING_ELAPSED          = 0x00200;  //  (Speed >= Threshold)
    public static final String  STYPE_SPEEDING_ELAPSED_NAME     = TYPE_ELAPSED_TIME_NAME_ + "Speeding";
    public static final int     STYPE_IGNITION_ELAPSED          = 0x00300;  //  (IgnitionOn)
    public static final String  STYPE_IGNITION_ELAPSED_NAME     = TYPE_ELAPSED_TIME_NAME_ + "Ignition";
    public static final int     STYPE_IDLE_ELAPSED              = 0x00320;  //  (IgnitionOn && (Speed == 0))
    public static final String  STYPE_IDLE_ELAPSED_NAME         = TYPE_ELAPSED_TIME_NAME_ + "Idle";
    public static final int     STYPE_INPUT_ELAPSED             = 0x00400;  //  (InputOn_X)
    public static final int     STYPE_INPUT_ELAPSED_MASK        = 0x000FF;  //  InputOn_X bit#
    public static final String  STYPE_INPUT_ELAPSED_NAME        = TYPE_ELAPSED_TIME_NAME_ + "Input";

    /* Distance (meters) */
    public static final int     TYPE_DISTANCE                   = 0x0030;  // Meters [long]
    public static final String  TYPE_DISTANCE_NAME_             = "Distance:";
    public static final int     STYPE_TRAVEL_DISTANCE           = 0x0100;  //
    public static final String  STYPE_TRAVEL_DISTANCE_NAME      = TYPE_DISTANCE_NAME_ + "Travel";

    // ------------------------------------------------------------------------

    /**
    *** StatusCode Summary Type
    **/
    public static final EventSummaryType EventSummaryType_StatusCode = new EventSummaryType(
        TYPE_STATUSCODE_NAME_,
        TYPE_STATUSCODE_COUNT, -1, 
        I18N.getString(EventSummaryType.class,"EventSummaryType.code.statusCode" ,"Status Count\n{1}"));

    /**
    *** Create an EventSummaryType instance for a specific StatusCode
    **/
    public static EventSummaryType CreateStatusCodeSummaryType(int sc)
    {
        EventSummaryType est = EventSummaryType_StatusCode;
        String n = TYPE_STATUSCODE_NAME_ + StringTools.toHexString(sc,16);
        return new EventSummaryType(n, est.getType(), sc, est._getDescription());
    }

    // ------------------------------------------------------------------------

    /**
    *** SimilarCode Summary Type
    **/
    public static final EventSummaryType EventSummaryType_SimilarCode = new EventSummaryType(
        TYPE_SIMILARCODE_NAME_,
        TYPE_SIMILARCODE_COUNT, -1, 
        I18N.getString(EventSummaryType.class,"EventSummaryType.code.similarCode" ,"Status Count\n{1}"));

    /**
    *** Create an EventSummaryType instance for a specific SimilarCode
    **/
    public static EventSummaryType CreateSimilarCodeSummaryType(int sc)
    {
        EventSummaryType est = EventSummaryType_SimilarCode;
        String n = TYPE_SIMILARCODE_NAME_ + StringTools.toHexString(sc,16);
        return new EventSummaryType(n, est.getType(), sc, est._getDescription());
    }

    // ------------------------------------------------------------------------

    /**
    *** List of SummaryTypes
    **/
    public static final EventSummaryType EventSummaryTypeList[] = {
        // -- Status Codes
        EventSummaryType_StatusCode,
        // -- Similar Codes
        EventSummaryType_SimilarCode,
        // -- Count
        new EventSummaryType(
            STYPE_EVENT_TOTAL_NAME,
            TYPE_EVENT_COUNT, STYPE_EVENT_TOTAL,
            I18N.getString(EventSummaryType.class,"EventSummaryType.count.total"       ,"Event Count\nTotal")),
        new EventSummaryType(
            STYPE_EVENT_VALID_GPS_NAME,
            TYPE_EVENT_COUNT, STYPE_EVENT_VALID_GPS,
            I18N.getString(EventSummaryType.class,"EventSummaryType.count.validGPS"    ,"Event Count\nValid GPS")),
        new EventSummaryType(
            STYPE_EVENT_INVALID_GPS_NAME,
            TYPE_EVENT_COUNT, STYPE_EVENT_INVALID_GPS,
            I18N.getString(EventSummaryType.class,"EventSummaryType.count.invalidGPS"  ,"Event Count\nInvalid GPS")),
        new EventSummaryType(
            STYPE_EVENT_SPEEDING_1_NAME,
            TYPE_EVENT_COUNT, STYPE_EVENT_SPEEDING_1,
            I18N.getString(EventSummaryType.class,"EventSummaryType.count.speeding.1"  ,"Event Count\nSpeeding-1")),
        new EventSummaryType(
            STYPE_EVENT_SPEEDING_2_NAME,
            TYPE_EVENT_COUNT, STYPE_EVENT_SPEEDING_2,
            I18N.getString(EventSummaryType.class,"EventSummaryType.count.speeding.2"  ,"Event Count\nSpeeding-2")),
        new EventSummaryType(
            STYPE_EVENT_SPEEDING_3_NAME,
            TYPE_EVENT_COUNT, STYPE_EVENT_SPEEDING_3,
            I18N.getString(EventSummaryType.class,"EventSummaryType.count.speeding.3"  ,"Event Count\nSpeeding-3")),
        new EventSummaryType(
            STYPE_EVENT_SPEEDING_4_NAME,
            TYPE_EVENT_COUNT, STYPE_EVENT_SPEEDING_4,
            I18N.getString(EventSummaryType.class,"EventSummaryType.count.speeding.4"  ,"Event Count\nSpeeding-4")),
        new EventSummaryType(
            STYPE_EVENT_SPEEDING_UNK_NAME,
            TYPE_EVENT_COUNT, STYPE_EVENT_SPEEDING_UNK,
            I18N.getString(EventSummaryType.class,"EventSummaryType.count.speeding.unk","Event Count\nSpeeding-?")),
        // -- Elapsed time
        new EventSummaryType(
            STYPE_MOVING_ELAPSED_NAME,
            TYPE_ELAPSED_TIME, STYPE_MOVING_ELAPSED,
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.moving"    ,"Elapsed\nMoving")),
        new EventSummaryType(
            STYPE_STOPPED_ELAPSED_NAME,
            TYPE_ELAPSED_TIME, STYPE_STOPPED_ELAPSED,
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.stopped"   ,"Elapsed\nStopped")),
        new EventSummaryType(
            STYPE_SPEEDING_ELAPSED_NAME,
            TYPE_ELAPSED_TIME, STYPE_SPEEDING_ELAPSED,
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.speeding"  ,"Elapsed\nSpeeding")),
        new EventSummaryType(
            STYPE_IGNITION_ELAPSED_NAME,
            TYPE_ELAPSED_TIME, STYPE_IGNITION_ELAPSED,
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.ignition"  ,"Elapsed\nIgnition-On")),
        new EventSummaryType(
            STYPE_IDLE_ELAPSED_NAME,
            TYPE_ELAPSED_TIME, STYPE_IDLE_ELAPSED,
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.idle"      ,"Elapsed\nIdle")),
        new EventSummaryType(
            STYPE_INPUT_ELAPSED_NAME+" 0",
            TYPE_ELAPSED_TIME, STYPE_INPUT_ELAPSED | 0, 
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.input_0"   ,"Elapsed\nInput #0")),
        new EventSummaryType(
            STYPE_INPUT_ELAPSED_NAME+" 1",
            TYPE_ELAPSED_TIME, STYPE_INPUT_ELAPSED | 1, 
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.input_1"   ,"Elapsed\nInput #1")),
        new EventSummaryType(
            STYPE_INPUT_ELAPSED_NAME+" 2",
            TYPE_ELAPSED_TIME, STYPE_INPUT_ELAPSED | 2, 
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.input_2"   ,"Elapsed\nInput #2")),
        new EventSummaryType(
            STYPE_INPUT_ELAPSED_NAME+" 3",
            TYPE_ELAPSED_TIME, STYPE_INPUT_ELAPSED | 3, 
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.input_3"   ,"Elapsed\nInput #3")),
        new EventSummaryType(
            STYPE_INPUT_ELAPSED_NAME+" 4",
            TYPE_ELAPSED_TIME, STYPE_INPUT_ELAPSED | 4, 
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.input_4"   ,"Elapsed\nInput #4")),
        new EventSummaryType(
            STYPE_INPUT_ELAPSED_NAME+" 5",
            TYPE_ELAPSED_TIME, STYPE_INPUT_ELAPSED | 5, 
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.input_5"   ,"Elapsed\nInput #5")),
        new EventSummaryType(
            STYPE_INPUT_ELAPSED_NAME+" 6",
            TYPE_ELAPSED_TIME, STYPE_INPUT_ELAPSED | 6, 
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.input_6"   ,"Elapsed\nInput #6")),
        new EventSummaryType(
            STYPE_INPUT_ELAPSED_NAME+" 7",
            TYPE_ELAPSED_TIME, STYPE_INPUT_ELAPSED | 7, 
            I18N.getString(EventSummaryType.class,"EventSummaryType.elapsed.input_7"   ,"Elapsed\nInput #7")),
        // -- Distance
        new EventSummaryType(
            STYPE_TRAVEL_DISTANCE_NAME,
            TYPE_DISTANCE, STYPE_TRAVEL_DISTANCE,
            I18N.getString(EventSummaryType.class,"EventSummaryType.distance.travelled","Distance\nTravelled")),
    };

    // ------------------------------------------------------------------------

    /**
    *** Gets the SummaryType for the specified summary name
    **/
    public static EventSummaryType GetEventSummaryType(int type, int subtype)
    {
        if (type == TYPE_STATUSCODE_COUNT) {
            int sc = subtype;
            if ((sc > 0) && (sc <= 0xFFFF)) {
                return EventSummaryType.CreateStatusCodeSummaryType(sc);
            } else {
                return null;
            }
        } else
        if (type == TYPE_SIMILARCODE_COUNT) {
            int sc = subtype;
            if ((sc > 0) && (sc <= 0xFFFF)) {
                return EventSummaryType.CreateSimilarCodeSummaryType(sc);
            } else {
                return null;
            }
        } else {
            for (EventSummaryType est : EventSummaryTypeList) {
                if ((est.getType() == type) && (est.getSubtype() == subtype)) {
                    return est;
                }
            }
            return null;
        }
    }

    /**
    *** Gets the SummarType for the specified summary name
    **/
    public static EventSummaryType GetEventSummaryType(String name)
    {

        /* invalid name */
        if (StringTools.isBlank(name)) {
            return null;
        }

        /* Status code count */
        if (StringTools.startsWithIgnoreCase(name,TYPE_STATUSCODE_NAME_)) {
            int sc = StringTools.parseHexInt(name.substring(TYPE_STATUSCODE_NAME_.length()),0);
            if ((sc > 0) && (sc <= 0xFFFF)) {
                return EventSummaryType.CreateStatusCodeSummaryType(sc);
            } else {
                return null;
            }
        }

        /* Similar Status code count */
        if (StringTools.startsWithIgnoreCase(name,TYPE_SIMILARCODE_NAME_)) {
            int sc = StringTools.parseHexInt(name.substring(TYPE_SIMILARCODE_NAME_.length()),0);
            if ((sc > 0) && (sc <= 0xFFFF)) {
                return EventSummaryType.CreateSimilarCodeSummaryType(sc);
            } else {
                return null;
            }
        }

        /* find name */
        for (EventSummaryType est : EventSummaryTypeList) {
            if (est.getName().equalsIgnoreCase(name)) {
                return est;
            }
        }

        /* not found */
        return null;

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the description of the specified type/subtype
    **/
    public static String GetDescription(
        int type, int subtype, 
        Device device, BasicPrivateLabel bpl)
    {
        Locale locale = (bpl != null)? bpl.getLocale() : null;
        if (type == TYPE_STATUSCODE_COUNT) {
            String args[] = new String[] { 
                String.valueOf(type), 
                StatusCode.getDescription(device,subtype,bpl,null) 
            };
            I18N.Text i18nDesc = EventSummaryType_StatusCode._getDescription();
            return i18nDesc.toString(locale, args);
        } else
        if (type == TYPE_SIMILARCODE_COUNT) {
            String args[] = new String[] { 
                String.valueOf(type), 
                StatusCode.getDescription(device,subtype,bpl,null) 
            };
            I18N.Text i18nDesc = EventSummaryType_SimilarCode._getDescription();
            return i18nDesc.toString(locale, args);
        } else {
            EventSummaryType est = EventSummaryType.GetEventSummaryType(type, subtype);
            if (est != null) {
                return est.getDescription(locale);
            }
            return "";
        }
    }

    /**
    *** Gets the description of the specified type/subtype
    **/
    public static String GetDescription(
        int type, int subtype)
    {
        return EventSummaryType.GetDescription(type, subtype, null, null);
    }

    // --------------------------------

    /**
    *** Gets the description of the specified type/subtype
    **/
    public static String GetDescription(
        String name, 
        Device device, BasicPrivateLabel bpl)
    {
        Locale locale = (bpl != null)? bpl.getLocale() : null;
        if (StringTools.startsWithIgnoreCase(name,TYPE_STATUSCODE_NAME_)) {
            int type    = TYPE_STATUSCODE_COUNT;
            int subtype = StringTools.parseHexInt(name.substring(TYPE_STATUSCODE_NAME_.length()),0);
            String args[] = new String[] { 
                String.valueOf(type), 
                StatusCode.getDescription(device,subtype,bpl,null) 
            };
            I18N.Text i18nDesc = EventSummaryType_StatusCode._getDescription();
            return i18nDesc.toString(locale, args);
        } else
        if (StringTools.startsWithIgnoreCase(name,TYPE_SIMILARCODE_NAME_)) {
            int type    = TYPE_SIMILARCODE_COUNT;
            int subtype = StringTools.parseHexInt(name.substring(TYPE_SIMILARCODE_NAME_.length()),0);
            String args[] = new String[] { 
                String.valueOf(type), 
                StatusCode.getDescription(device,subtype,bpl,null) 
            };
            I18N.Text i18nDesc = EventSummaryType_SimilarCode._getDescription();
            return i18nDesc.toString(locale, args);
        } else {
            EventSummaryType est = EventSummaryType.GetEventSummaryType(name);
            if (est != null) {
                return est.getDescription(locale);
            }
            return "";
        }
    }

    /**
    *** Gets the description of the specified type/subtype
    **/
    public static String GetDescription(
        String name)
    {
        return EventSummaryType.GetDescription(name, null, null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String    name    = "";
    private int       type    = 0;
    private int       subtype = 0;
    private I18N.Text desc    = null;

    /**
    *** Constructor
    **/
    public EventSummaryType(String name, int type, int subtype, I18N.Text desc) 
    {
        this.type    = type;
        this.subtype = subtype;
        this.name    = StringTools.trim(name);
        this.desc    = desc;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the primary type
    **/
    public int getType()
    {
        return this.type;
    }

    /**
    *** Gets the subtype
    **/
    public int getSubtype()
    {
        return this.subtype;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name 
    **/
    public String getName()
    {
        return StringTools.trim(this.name);
    }

    /**
    *** Returns try if this EventSummaryType represend elapsed time
    **/
    public boolean isElapsedTime()
    {
        String n = StringTools.toUpperCase(this.getName());
        return n.startsWith("ELAPSED:");
    }

    /**
    *** Returns try if this EventSummaryType represend elapsed time
    **/
    public boolean isCount()
    {
        return !this.isElapsedTime();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the internal I18N description instance
    **/
    public I18N.Text _getDescription()
    {
        return this.desc;
    }

    /**
    *** Gets the description of thie EventSummaryType for the specified Locale
    **/
    public String getDescription(Locale locale)
    {
        String args[] = new String[] { 
            String.valueOf(this.type), 
            ("0x" + StringTools.toHexString(this.subtype,16)) };
        return this.desc.toString(locale, args);
    }

    // ------------------------------------------------------------------------

}
