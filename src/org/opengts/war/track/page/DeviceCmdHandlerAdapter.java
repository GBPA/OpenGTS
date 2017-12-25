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
//  2016/12/21  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.Calendar;
import org.opengts.war.track.*;

public abstract class DeviceCmdHandlerAdapter
    implements DeviceCmdHandler
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* default command */
    public static final String  DEFAULT_COMMAND     = "LocateNow";

    /* maximum command arguments */
    public static final int     MAX_COMMAND_ARGS    = 10;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String              serverID            = null;
    private Map<String,String>  commands            = null;

    /**
    *** Constructor
    **/
    public DeviceCmdHandlerAdapter(String servID)
    {
        super();
        this.serverID = StringTools.trim(servID);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the ServerID (aka "deviceCode") supported by this instance
    **/
    public String getServerID()
    {
        return this.serverID;
    }

    /**
    *** Appends the specified argument to the ServerID for this instance
    **/
    public void setServerIDArg(String arg)
    {
        if (!StringTools.isBlank(arg)) {
            this.serverID += arg.trim();
        }
    }

    /**
    *** Gets the DCServerConfig instance for the handled ServerID
    **/
    public DCServerConfig getServerConfig()
    {
        return DCServerFactory.getServerConfig(this.getServerID());
    }

    /**
    *** Gets the description of the handled ServerID
    **/
    public String getServerDescription()
    {
        return DCServerFactory.getServerConfigDescription(this.getServerID());
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance supports commands for the specified device. 
    **/
    public boolean deviceSupportsCommands(Device dev)
    {
        if (dev == null) {
            Print.logWarn("Device is null");
            return false;
        } else
        if (StringTools.isBlank(dev.getDeviceCode())) {
            Print.logWarn("DeviceCode is null/blank");
            return false;
        } else
        if (!dev.getDeviceCode().equalsIgnoreCase(this.getServerID())) {
            Print.logWarn("DeviceCode does not match: found " + dev.getDeviceCode() + ", expecting " + this.getServerID());
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes the HTML form output that supports the commands for this instance
    **/
    public abstract boolean writeCommandForm(PrintWriter out, RequestProperties reqState, Device selDev,
        String actionURL, boolean editProps) throws IOException;

    // ------------------------------------------------------------------------

    /**
    *** Handles the command selection from the HTML form, for the specified device
    **/
    public abstract String handleDeviceCommands(RequestProperties reqState, Device selDev);

    // ------------------------------------------------------------------------

}
