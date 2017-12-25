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
//  2017/08/16  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.dbtools.DBException;
import org.opengts.db.tables.Account;
import org.opengts.db.tables.Device;
import org.opengts.db.tables.EventData;

public abstract class DataPushProvider
{

    // ------------------------------------------------------------------------

    public  static final String PROP_DataPushProvider_dispatchHost  = "DataPushProvider.dispatchHost";
    public  static final String PROP_DataPushProvider_dispatchPort  = "DataPushProvider.dispatchPort";

    // ------------------------------------------------------------------------
    // -- Account DataPush properties
    
    public  static final String VERSION                             = "DP-0.1";

    public  static final String CMD_HEADER_                         = "PUSH";

    public  static final String CMD_VERSION                         = "version";
    public  static final String CMD_PUSH                            = "push";

    public  static final String RTP_COMMAND[]                       = { "command" };
    public  static final String RTP_ACCOUNT[]                       = { "account" };
    public  static final String RTP_DEVICE[]                        = { "device"  };

    // ------------------------------------------------------------------------
    // -- dispatch response

    public  static final String RESP_version                        = "version";
    public  static final String RESP_status                         = "status";
    public  static final String RESP_message                        = "message";

    /**
    *** Creates a response message to a dispatch queue request.
    *** Only used in the process implementing the actual data push
    **/
    public static byte[] CreateResponse(int status, String msg)
    {
        // -- RESPONSE: version=2.3.4 status=OK message=Queued
        RTProperties rtp = new RTProperties();
        rtp.setString(DataPushProvider.RESP_version, DataPushProvider.VERSION);
        if (status >= 0) {
            rtp.setInt(DataPushProvider.RESP_status , status);
        }
        if (!StringTools.isBlank(msg)) {
            rtp.setString(DataPushProvider.RESP_message, msg);
        }
        return ("RESPONSE: " + rtp + "\n").getBytes();
    }

    // ------------------------------------------------------------------------
    // -- errors

    /* unknown */
    public  static final int    ERR_UNKNOWN                         =   -1;

    /* success */
    public  static final int    ERR_SUCCESS                         =    0;
    public  static final int    ERR_QUEUED                          =    1;

    /* general errors */
    public  static final int    ERR_ERROR                           =  100;
    public  static final int    ERR_NO_DATA                         =  101;
    public  static final int    ERR_NO_ACCOUNTS                     =  102;
    public  static final int    ERR_NO_DEVICES                      =  103;
    public  static final int    ERR_NOT_SUPPORTED                   =  105;
    public  static final int    ERR_INVALID_REQUEST                 =  140;
    public  static final int    ERR_CONNECTION                      =  160;
    public  static final int    ERR_INTERNAL                        =  180;

    /* Account errors */
    public  static final int    ERR_ACCOUNT_NULL                    =  200;
    public  static final int    ERR_ACCOUNT_INVALID                 =  201;
    public  static final int    ERR_ACCOUNT_INACTIVE                =  202;
    public  static final int    ERR_ACCOUNT_EXPIRED                 =  203;
    public  static final int    ERR_ACCOUNT_DISABLED                =  204;
    public  static final int    ERR_ACCOUNT_READ                    =  205;
    public  static final int    ERR_ACCOUNT_INTERNAL                =  280;

    /* Device errors */
    public  static final int    ERR_DEVICE_NULL                     =  300;
    public  static final int    ERR_DEVICE_INVALID                  =  301;
    public  static final int    ERR_DEVICE_INACTIVE                 =  302;
    public  static final int    ERR_DEVICE_EXPIRED                  =  303;
    public  static final int    ERR_DEVICE_DISABLED                 =  304;
    public  static final int    ERR_RESERVED_1                      =  305; // ERR_NO_IGNITION
    public  static final int    ERR_DEVICE_READ                     =  306;
    public  static final int    ERR_DEVICE_UPDATE                   =  307;
    public  static final int    ERR_DEVICE_INPROCESS                =  308;
    public  static final int    ERR_DEVICE_INTERNAL                 =  380;

    /* EventData errors */
    public  static final int    ERR_EVENT_READ                      =  500;
    
    public enum DataPushError implements EnumTools.IntValue {
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
        DEVICE_READ     (ERR_DEVICE_READ     , "Device read error"      ),
        DEVICE_UPDATE   (ERR_DEVICE_UPDATE   , "Device update error"    ),
        DEVICE_INPROCESS(ERR_DEVICE_INPROCESS, "Device in process"      ),
        DEVICE_INTERNAL (ERR_DEVICE_INTERNAL , "Device internal error"  ),
        // --
        EVENT_READ      (ERR_EVENT_READ      , "EventData read error"   );
        // --
        private int    ss = ERR_UNKNOWN;
        private String mm = null;
        DataPushError(int s, String m)       { ss = s; mm = m; }
        public int           getIntValue()   { return ss; }
        public String        toString()      { return mm; }
        public boolean       isSuccess()     { return ((ss >= ERR_SUCCESS) && (ss < ERR_ERROR))? true : false; }
        public boolean       isError()       { return (ss >= ERR_ERROR)? true : false; }
        public DataPushError get(int s)      { return EnumTools.getValueOf(DataPushError.class,s,this,true); }
        public String        getDesc(int s)  { return this.get(s).toString(); }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String  dispatchHost    = null;
    private int     dispatchPort    = 0;

    /**
    *** Constructor
    **/
    public DataPushProvider()
    {
        super();
        String dispHost = RTConfig.getString(DataPushProvider.PROP_DataPushProvider_dispatchHost,null);
        int    dispPort = RTConfig.getInt(   DataPushProvider.PROP_DataPushProvider_dispatchPort,0);
        this.setDispatch(dispHost, dispPort);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the current version
    **/
    public String getVersion()
    {
        return "DataPushProvider";
    }

    /**
    *** Gets the description of this DataPush provider
    **/
    public String getDescription()
    {
        return "DataPush Dispatcher";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the dispatch host:port
    **/
    public void setDispatch(String host, int port)
    {
        this.dispatchHost = StringTools.trim(host);
        this.dispatchPort = (port <= 0)? 0 : port;
    }
    
    /**
    *** Gets the bind host for interprocess communication
    **/
    public String getDispatchHost()
    {
        return this.dispatchHost;
    }

    /**
    *** Gets the dispatch port for interprocess communication
    **/
    public int getDispatchPort()
    {
        return this.dispatchPort;
    }

    /**
    *** Connect to DataPush processing service and queue Account/Device
    **/
    protected int sendPushCommand(RTProperties rtp)
    {

        /* get host:port */
        String dispHost = this.getDispatchHost();
        int    dispPort = this.getDispatchPort();
        if (StringTools.isBlank(dispHost)) {
            return DataPushProvider.ERR_NOT_SUPPORTED;
        } else 
        if (dispPort <= 0) {
            return DataPushProvider.ERR_NOT_SUPPORTED;
        }

        /* command to send */
        StringBuffer pushCmd = new StringBuffer();
        pushCmd.append(DataPushProvider.CMD_HEADER_);
        pushCmd.append(": ");
        pushCmd.append(rtp.toString());
        pushCmd.append("\n");
        Print.logInfo("Sending: " + StringTools.trim(pushCmd));

        /* send */
        ClientSocketThread cst = new ClientSocketThread(dispHost, dispPort);
        int status = DataPushProvider.ERR_UNKNOWN;
        try {
            cst.openSocket();
            cst.socketWriteString(pushCmd);
            String respStr = cst.socketReadLine();
            RTProperties respRTP = new RTProperties(respStr);
            status = respRTP.getInt(DataPushProvider.RESP_status,DataPushProvider.ERR_UNKNOWN);
        } catch (ConnectException ce) {
            Print.logError("Unable to connect to DataPush service: " + ce.getMessage());
            status = DataPushProvider.ERR_CONNECTION;
        } catch (Throwable t) {
            Print.logException("Error", t);
            status = DataPushProvider.ERR_ERROR;
        } finally {
            cst.closeSocket();
        }

        /* return response */
        return status;

    }

    /**
    *** Connect to DataPush processing service and queue Account/Device
    **/
    protected int dispatchDataPush(String accountID, String deviceID)
    {
        RTProperties rtp = new RTProperties();
        rtp.setString(DataPushProvider.RTP_COMMAND[0], DataPushProvider.CMD_PUSH);
        rtp.setString(DataPushProvider.RTP_ACCOUNT[0], StringTools.trim(accountID));
        rtp.setString(DataPushProvider.RTP_DEVICE[0] , StringTools.trim(deviceID));
        return this.sendPushCommand(rtp);
    }

    // ------------------------------------------------------------------------

    /**
    *** Push event to destination.   
    *** This implementation does nothing, must be overridden to provide actual implementation.
    *** @param ev  The EventData instance to push to the destination address
    *** @return True if event was actually pushed to external services, false otherwise
    *** @throws DBException If a database error occurs
    *** @throws IOException If an I/O error occurs
    **/
    public boolean pushEvent(EventData ev) 
        throws DBException, IOException
    {
        // -- NO-OP
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Process recent events for the specified enabled Device
    **/
    public int processPushEvents(Device device)
    {
        if (device == null) {
            return DataPushProvider.ERR_DEVICE_NULL;
        } else {
            String accountID = device.getAccountID();
            String deviceID  = device.getDeviceID();
            return this.dispatchDataPush(accountID, deviceID);
        }
    }

    /**
    *** Process recent events for all enabled Devices within the specified Account
    **/
    public int processPushEvents(Account account)
    {
        if (account == null) {
            return DataPushProvider.ERR_ACCOUNT_NULL;
        } else {
            String accountID = account.getAccountID();
            String deviceID  = "ALL";
            return this.dispatchDataPush(accountID, deviceID);
        }
    }

    /**
    *** Process recent events for all enabled Devices within the specified Account
    **/
    /*
    private int _processPushEvents(Account account)
    {
        if (account == null) {
            return DataPushProvider.ERR_ACCOUNT_NULL;
        }
        String accountID = account.getAccountID();
        // -- read list of Account devices
        OrderedSet<String> devIDList;
        try {
            devIDList = Device.getDeviceIDsForAccount(accountID, null, false);
        } catch (DBException dbe) {
            return DataPushProvider.ERR_ACCOUNT_READ;
        }
        // -- loop through devices
        int rtn = DataPushProvider.ERR_SUCCESS;
        for (String devID : devIDList) {
            // -- read Device
            Device device;
            try {
                device = account.getDevice(devID);
            } catch (DBException dbe) {
                Print.logException("Unable to read Account device: " + accountID + "/" + devID, dbe);
                return DataPushProvider.ERR_DEVICE_READ;
            }
            // -- process device
            if (device != null) { // unlikely to be null
                int err = this.processPushEvents(device);
                if (err >= DataPushProvider.ERR_ERROR) {
                    return err;
                } else
                if (err == DataPushProvider.ERR_QUEUED) {
                    rtn = DataPushProvider.ERR_QUEUED;
                }
            } else {
                // -- ignore (unlikely to occur)
            }
        }
        return rtn;
    }
    */

    // ------------------------------------------------------------------------

}
