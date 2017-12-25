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
//  2014/09/26  Martin D. Flynn
//     -Initial release
//  2015/08/16  Martin D. Flynn
//     -Added support for neighoring cell-towers
//  2017/10/09  Martin D. Flynn
//     -Added "id" tag support [2.6.5-B49]
// ----------------------------------------------------------------------------
package org.opengts.cellid.unwiredlabs;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

import org.opengts.db.*;
import org.opengts.cellid.*;

public class UnwiredLabs
    extends MobileLocationProviderAdapter
    implements MobileLocationProvider
{

    private static final String  VERSION                        = "0.1.1";

    // ------------------------------------------------------------------------
    //
    // References:
    //   - https://unwiredlabs.com/api
    //
    // ------------------------------------------------------------------------

    private static final String  MOBILE_LOCATION_US_EAST        = "https://us1.unwiredlabs.com/v2/process.php"; // (Northern Virginia)
    private static final String  MOBILE_LOCATION_US_WEST        = "https://us2.unwiredlabs.com/v2/process.php"; // (San Francisco)
    private static final String  MOBILE_LOCATION_EUROPE         = "https://eu1.unwiredlabs.com/v2/process.php"; // (Ireland)
    private static final String  MOBILE_LOCATION_ASIA_PACIFIC   = "https://ap1.unwiredlabs.com/v2/process.php"; // (Singapore)

    // ------------------------------------------------------------------------

    private static final String  PROP_timeoutMS                 = "timeoutMS";

    // ------------------------------------------------------------------------

    private static final long    DefaultServiceTimeout          = 5000L; // milliseconds

    // ------------------------------------------------------------------------

    private static final String  TAG_token                      = "token";
    private static final String  TAG_id                         = "id";

    private static final String  TAG_radio                      = "radio";
    private static final String  TAG_mcc                        = "mcc";
    private static final String  TAG_mnc                        = "mnc";
    private static final String  TAG_cells                      = "cells";
    private static final String  TAG_lac                        = "lac";
    private static final String  TAG_cid                        = "cid";
    private static final String  TAG_signal                     = "signal";
    private static final String  TAG_tA                         = "tA";
    private static final String  TAG_asu                        = "asu";
    private static final String  TAG_psc                        = "psc";
    private static final String  TAG_address                    = "address";

    private static final String  TAG_status                     = "status";
    private static final String  TAG_balance                    = "balance";
    private static final String  TAG_message                    = "message";
    private static final String  TAG_lat                        = "lat";
    private static final String  TAG_lon                        = "lon";
    private static final String  TAG_accuracy                   = "accuracy";
    private static final String  TAG_aged                       = "aged";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static MobileLocation _getMobileLocation(
        CellTower servCT, CellTower nborCT[], 
        String token, long timeoutMS)
    {
        // -- Doc: http://www.unwiredlabs.com/api#requests
        
        /* no serving cell-tower? */
        if (servCT == null) {
            Print.logWarn("Serving Cell-Tower is null");
            return null;
        }

        /* URL */
        String url = MOBILE_LOCATION_US_EAST;
        if (StringTools.isBlank(url)) {
            Print.logWarn("Cell-Tower web-service URL is blank/null");
            return null;
        }

        /* encode JSON request */
        // -- Example request:
        // -    {
        // -        "token": "Your_API_Token",
        // -        "id": "Device_ID",
        // -        "radio": "gsm",
        // -        "mcc": 310,
        // -        "mnc": 410,
        // -        "cells": [{
        // -            "lac": 7033,
        // -            "cid": 17811
        // -        }, {
        // -            "lac": 7033,
        // -            "cid": 17812,
        // -            "signal": -60,
        // -            "tA": 13
        // -        }, {
        // -            "lac": 7033,
        // -            "cid": 18513
        // -        }, {
        // -            "lac": 7033,
        // -            "cid": 16383
        // -        }, {
        // -            "lac": 7033,
        // -            "cid": 12812
        // -        }, {
        // -            "lac": 7033,
        // -            "cid": 12811
        // -        }],
        // -        "address": 1
        // -    }
        JSON._Object jsonReq = new JSON._Object();
        jsonReq.addKeyValue(TAG_token , StringTools.trim(token));
        if (servCT.hasRequestorMobileID()) { 
            jsonReq.addKeyValue(TAG_id, servCT.getRequestorMobileID());  // [2.6.5-B49]
        }
      //jsonReq.addKeyValue(TAG_radio , "gsm"); // "gsm", "cdma", "umts", "lte"
        if (servCT.hasRadioType())             { jsonReq.addKeyValue(TAG_radio , servCT.getRadioType());             }
        if (servCT.hasMobileCountryCode())     { jsonReq.addKeyValue(TAG_mcc   , servCT.getMobileCountryCode());     }
        if (servCT.hasMobileNetworkCode())     { jsonReq.addKeyValue(TAG_mnc   , servCT.getMobileNetworkCode());     }
        JSON._Array  cellReqArry = new JSON._Array();
        // -- serving cell-tower
        JSON._Object servCellReq = new JSON._Object();
        if (servCT.hasCellTowerID())           { servCellReq.addKeyValue(TAG_cid   , servCT.getCellTowerID());           }
        if (servCT.hasLocationAreaCode())      { servCellReq.addKeyValue(TAG_lac   , servCT.getLocationAreaCode());      }
        if (servCT.hasMobileCountryCode())     { servCellReq.addKeyValue(TAG_mcc   , servCT.getMobileCountryCode());     }
        if (servCT.hasMobileNetworkCode())     { servCellReq.addKeyValue(TAG_mnc   , servCT.getMobileNetworkCode());     }
        if (servCT.hasReceptionLevel())        { servCellReq.addKeyValue(TAG_signal, servCT.getReceptionLevel());        }
        if (servCT.hasTimingAdvance())         { servCellReq.addKeyValue(TAG_tA    , servCT.getTimingAdvance());         }
        if (servCT.hasPrimaryScramblingCode()) { servCellReq.addKeyValue(TAG_psc   , servCT.getPrimaryScramblingCode()); }
        cellReqArry.addValue(servCellReq);
        // -- neighbor cell-towers
        if (!ListTools.isEmpty(nborCT)) {
            for (CellTower nCT : nborCT) {
                JSON._Object nborCellReq = new JSON._Object();
                if (nCT.hasCellTowerID())           { nborCellReq.addKeyValue(TAG_cid   , servCT.getCellTowerID());           }
                if (nCT.hasLocationAreaCode())      { nborCellReq.addKeyValue(TAG_lac   , servCT.getLocationAreaCode());      }
                if (nCT.hasMobileCountryCode())     { nborCellReq.addKeyValue(TAG_mcc   , servCT.getMobileCountryCode());     }
                if (nCT.hasMobileNetworkCode())     { nborCellReq.addKeyValue(TAG_mnc   , servCT.getMobileNetworkCode());     }
                if (nCT.hasReceptionLevel())        { nborCellReq.addKeyValue(TAG_signal, servCT.getReceptionLevel());        }
                if (nCT.hasTimingAdvance())         { nborCellReq.addKeyValue(TAG_tA    , servCT.getTimingAdvance());         }
                if (nCT.hasPrimaryScramblingCode()) { nborCellReq.addKeyValue(TAG_psc   , servCT.getPrimaryScramblingCode()); }
                cellReqArry.addValue(nborCellReq);
            }
        }
        // -- 
        jsonReq.addKeyValue(TAG_cells, cellReqArry);
        jsonReq.addKeyValue(TAG_address, 1); // include address

        /* get HTTP result */
        JSON._Object jsonResp = null;
        try {
            String reqS = jsonReq.toString(true);
            Print.logDebug("CellTower loc URL: " + url);
            Print.logDebug("CellTower loc JSON: \n" + reqS);
            byte rspB[] = HTMLTools.readPage_POST(
                new URL(url), 
                HTMLTools.MIME_JSON(), reqS.getBytes(), 
                (int)timeoutMS, -1);
            if (ListTools.isEmpty(rspB)) {
                // -- invalid response
                return null;
            } 
            String rspS = StringTools.toStringValue(rspB);
            Print.logDebug("CellTower loc response: \n" + rspS);
            jsonResp = JSON.parse_Object(rspS);
        } catch (JSON.JSONParsingException jpe) {
            // -- invalid JSON
            return null;
        } catch (Throwable th) {
            // -- timeout, invalid response
            return null;
        }

        /* status/balance */
        String status  = jsonResp.getStringForName(TAG_status ,"");
        int    balance = jsonResp.getIntForName(   TAG_balance,-1);
        String message = jsonResp.getStringForName(TAG_message,"");
        if (!status.equalsIgnoreCase("ok")) {
            // -- invalid status
            Print.logError("Invalid status: " + status + " [balance " + balance + "] " + message);
            return null;
        }

        /* parse lat/lon */
        double latitude  = jsonResp.getDoubleForName(TAG_lat     ,0.0);
        double longitude = jsonResp.getDoubleForName(TAG_lon     ,0.0);
        double accuracy  = jsonResp.getDoubleForName(TAG_accuracy,0.0);
        String address   = jsonResp.getStringForName(TAG_address ,"");

        /* valid GeoPoint? */
        if (GeoPoint.isValid(latitude,longitude)) {
            MobileLocation ML = new MobileLocation(latitude,longitude,accuracy);
            if (!StringTools.isBlank(address)) {
                ML.setAddress(address);
            }
            return ML;
        } else {
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // MobileLocationProvider interface

    public UnwiredLabs(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
    }

    public MobileLocation getMobileLocation(CellTower servCT, CellTower nborCT[]) 
    {
        long tmoMS = this.getProperties().getLong(PROP_timeoutMS, DefaultServiceTimeout);
        return UnwiredLabs._getMobileLocation(servCT, nborCT, this.getAuthorization(), tmoMS);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final String ARG_TOKEN[]  = { "token", "key", "auth"  };
    public static final String ARG_MOBID[]  = { "id"   , "mid", "mobid" };
    public static final String ARG_RADIO[]  = { "radio"                 };
    public static final String ARG_CID[]    = { "CID"  , "cellid"       };
    public static final String ARG_MNC[]    = { "MNC"                   };
    public static final String ARG_MCC[]    = { "MCC"                   };
    public static final String ARG_LAC[]    = { "LAC"                   };

    /**
    *** Main entery point for debugging/testing
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        Print.setAllOutputToStdout(true);
        Print.setEncoding(StringTools.CharEncoding_UTF_8);

        /* parameters */
        // -- bin/exeJava org.opengts.cellid.unwiredlabs.UnwiredLabs -token=123456789 -CID=565110   -MNC=8   -MCC=240 -LAC=318   -id=xx_12345
        // -- bin/exeJava org.opengts.cellid.unwiredlabs.UnwiredLabs -token=123456789 -CID=23119111 -MNC=720 -MCC=302 -LAC=35022 -id=xx_12345
        String radio = RTConfig.getString(ARG_RADIO, "gsm"); // "gsm"
        int    CID   = RTConfig.getInt(   ARG_CID  , 0    ); // 565110
        int    MNC   = RTConfig.getInt(   ARG_MNC  , 0    ); // 8
        int    MCC   = RTConfig.getInt(   ARG_MCC  , 0    ); // 240
        int    LAC   = RTConfig.getInt(   ARG_LAC  , 0    ); // 318
        String mobID = RTConfig.getString(ARG_MOBID, null );
        String token = RTConfig.getString(ARG_TOKEN, ""   );

        /* geocode lookup */
        CellTower ct = new CellTower();
        ct.setRadioType(radio); // [2.6.5-B54]
        ct.setCellTowerID(CID);
        ct.setMobileNetworkCode(MNC);
        ct.setMobileCountryCode(MCC);
        ct.setLocationAreaCode(LAC);
        if (!StringTools.isBlank(mobID)) {
            ct.setRequestorMobileID(mobID); // [2.6.5-B49]
        }

        /* get CellTower location */
        UnwiredLabs mobLoc = new UnwiredLabs("unwiredlabs", token, null);
        MobileLocation ml = mobLoc.getMobileLocation(ct, null/*neighbors*/);
        if (ml != null) {
            Print.sysPrintln("Mobile Loc: " + ml);
        } else {
            Print.sysPrintln("No Mobile Location available");
        }

    }

}
