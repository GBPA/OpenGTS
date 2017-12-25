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
//  2016/01/04  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.db.tables.Account;
import org.opengts.db.tables.Device;

public interface ELogHOSProvider
{

    // ------------------------------------------------------------------------

    /* unknown */
    public  static final int ERR_UNKNOWN            = -1;

    /* success */
    public  static final int ERR_SUCCESS            =   0;
    public  static final int ERR_QUEUED             =   1;

    /* general errors */
    public  static final int ERR_ERROR              = 100;
    public  static final int ERR_NO_DATA            = 101;
    public  static final int ERR_NO_ACCOUNTS        = 102;
    public  static final int ERR_NO_DEVICES         = 103;
    public  static final int ERR_NOT_SUPPORTED      = 105;
    public  static final int ERR_INVALID_REQUEST    = 140;
    public  static final int ERR_CONNECTION         = 160;
    public  static final int ERR_INTERNAL           = 180;

    /* Account errors */
    public  static final int ERR_ACCOUNT_NULL       = 200;
    public  static final int ERR_ACCOUNT_INVALID    = 201;
    public  static final int ERR_ACCOUNT_INACTIVE   = 202;
    public  static final int ERR_ACCOUNT_EXPIRED    = 203;
    public  static final int ERR_ACCOUNT_DISABLED   = 204;
    public  static final int ERR_ACCOUNT_READ       = 205;
    public  static final int ERR_ACCOUNT_INTERNAL   = 280;

    /* Device errors */
    public  static final int ERR_DEVICE_NULL        = 300;
    public  static final int ERR_DEVICE_INVALID     = 301;
    public  static final int ERR_DEVICE_INACTIVE    = 302;
    public  static final int ERR_DEVICE_EXPIRED     = 303;
    public  static final int ERR_DEVICE_DISABLED    = 304;
    public  static final int ERR_NO_IGNITION        = 305;
    public  static final int ERR_DEVICE_READ        = 306;
    public  static final int ERR_DEVICE_UPDATE      = 307;
    public  static final int ERR_DEVICE_INPROCESS   = 308;
    public  static final int ERR_DEVICE_INTERNAL    = 380;

    /* EventData errors */
    public  static final int ERR_EVENT_READ         = 500;
    
    public enum ELogHOSError implements EnumTools.IntValue {
        UNKNOWN         (ERR_UNKNOWN         , "Unknown"                ),
        // --
        SUCCESS         (ERR_SUCCESS         , "Success"                ),
        QUEUED          (ERR_QUEUED          , "Queued"                 ),
        // --
        ERROR           (ERR_ERROR           , "Error"                  ),
        NO_DATA         (ERR_NO_DATA         , "No Data"                ),
        NO_ACCOUNTS     (ERR_NO_ACCOUNTS     , "No Accounts"            ),
        NO_DEVICES      (ERR_NO_DEVICES      , "No Devices"             ),
        NOT_SUPPORTED   (ERR_NOT_SUPPORTED   , "Not Supported"          ),
        INVALID_REQUEST (ERR_INVALID_REQUEST , "Invalid Request"        ),
        CONNECTION      (ERR_CONNECTION      , "Connection Error"       ),
        INTERNAL        (ERR_INTERNAL        , "Internal Error"         ),
        // --
        ACCOUNT_NULL    (ERR_ACCOUNT_NULL    , "Account is null"        ),
        ACCOUNT_INVALID (ERR_ACCOUNT_INVALID , "Account is invalid"     ),
        ACCOUNT_INACTIVE(ERR_ACCOUNT_INACTIVE, "Account is inactive"    ),
        ACCOUNT_EXPIRED (ERR_ACCOUNT_EXPIRED , "Account is expired"     ),
        ACCOUNT_DISABLED(ERR_ACCOUNT_DISABLED, "Account is disabled"    ),
        ACCOUNT_READ    (ERR_ACCOUNT_READ    , "Account read error"     ),
        ACCOUNT_INTERNAL(ERR_ACCOUNT_INTERNAL, "Account internal error" ),
        // --
        DEVICE_NULL     (ERR_DEVICE_NULL     , "Device is null"         ),
        DEVICE_INVALID  (ERR_DEVICE_INVALID  , "Device is invalid"      ),
        DEVICE_INACTIVE (ERR_DEVICE_INACTIVE , "Device is inactive"     ),
        DEVICE_EXPIRED  (ERR_DEVICE_EXPIRED  , "Device is expired"      ),
        DEVICE_DISABLED (ERR_DEVICE_DISABLED , "Device is disabled"     ),
        NO_IGNITION     (ERR_NO_IGNITION     , "No Device Ignition"     ),
        DEVICE_READ     (ERR_DEVICE_READ     , "Device read error"      ),
        DEVICE_UPDATE   (ERR_DEVICE_UPDATE   , "Device update error"    ),
        DEVICE_INPROCESS(ERR_DEVICE_INPROCESS, "Device in process"      ),
        DEVICE_INTERNAL (ERR_DEVICE_INTERNAL , "Device internal error"  ),
        // --
        EVENT_READ      (ERR_EVENT_READ      , "EventData read error"   );
        // --
        private int    ss = ERR_UNKNOWN;
        private String mm = null;
        ELogHOSError(int s, String m)       { ss = s; mm = m; }
        public int          getIntValue()   { return ss; }
        public String       toString()      { return mm; }
        public boolean      isSuccess()     { return ((ss >= ERR_SUCCESS) && (ss < ERR_ERROR))? true : false; }
        public boolean      isError()       { return (ss >= ERR_ERROR)? true : false; }
        public ELogHOSError get(int s)      { return EnumTools.getValueOf(ELogHOSError.class,s,this,true); }
        public String       getDesc(int s)  { return this.get(s).toString(); }
    };

    // ------------------------------------------------------------------------

    /**
    *** Gets the current version
    **/
    public String getVersion();

    /**
    *** Gets the description of this ELog/HOS provider
    **/
    public String getDescription();

    // ------------------------------------------------------------------------

    /**
    *** Process recent events for all enabled Devices within the specified Account
    **/
    public int processELogEvents(Account account, long limit);

    /**
    *** Process recent events for the specified enabled Device
    **/
    public int processELogEvents(Device device, long limit);

    // ------------------------------------------------------------------------

    /**
    *** Gets the ELog/HOS login URL
    *** @param account         The current account
    *** @param productionMode  True for production mode, else development/test mode
    *** @return The login URL
    **/
    public String getLoginURL(Account account);

}
