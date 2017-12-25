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
//  2009/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;

public interface DeviceCmdHandler
{

    // ------------------------------------------------------------------------

    /**
    *** Gets the ServerID (aka "deviceCode") supported by this instance
    **/
    public String getServerID();

    /**
    *** Appends the specified argument to the ServerID for this instance
    **/
    public void setServerIDArg(String arg);

    /**
    *** Gets the description of the handled ServerID
    **/
    public String getServerDescription();

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance supports commands for the specified device. 
    **/
    public boolean deviceSupportsCommands(Device dev);

    /**
    *** Writes the HTML form output that supports the commands for this instance
    **/
    public boolean writeCommandForm(PrintWriter out, RequestProperties reqState, Device selDev,
        String actionURL, boolean editProps) throws IOException;
        
    /**
    *** Handles the command selection from the HTML form, for the specified device
    **/
    public String handleDeviceCommands(RequestProperties reqState, Device selDev);

    // ------------------------------------------------------------------------

    //public boolean hasSmsCommands(RequestProperties reqState);

    // ------------------------------------------------------------------------

}
