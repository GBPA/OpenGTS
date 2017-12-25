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
//  2013/03/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.db.tables.*;

public interface CustomCommand
{

    // ------------------------------------------------------------------------

    /* command argument separator */
    public  static final String  ARG_SEPARATOR          = "|";
    public  static final char    ARG_SEPARATOR_CHAR     = '|';

    // ------------------------------------------------------------------------

    /**
    *** Callback to handle custom web-service commands.  
    *** device communication server.
    *** @param cmdID    The command id
    *** @param cmdArg   The command argument string
    *** @param account  The current Account instance
    *** @param user     The current User instance
    *** @param bpl      The context BasicPrivateLabel (if available)
    *** @param respType The expect response type (ie. "xml", "json", "csv", etc).
    *** @return The response which will be sent back to the requestor
    **/
    public byte[] handleCommand(
        String cmdID, String cmdArg,
        Account account, User user, 
        BasicPrivateLabel bpl,
        String respType);

}
