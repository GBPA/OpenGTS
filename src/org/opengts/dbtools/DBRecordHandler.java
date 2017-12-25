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
//  2007/03/04  Martin D. Flynn
//     -Initial release
//  2007/03/25  Martin D. Flynn
//     -Changed return type to 'int' to allow use as a record selector.
//  2014/12/17  Martin D. Flynn
//     -Redefined DBRH_* values, and added additional options.
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBRecordHandler</code> is the interface for a callback SQL record handler.
**/

public interface DBRecordHandler<RT extends DBRecord<RT>>
{

    // ------------------------------------------------------------------------

    /* DBREcordHandler return types */
    // -- Note: these must NOT be cached in any persistent storage
    public static final int DBRH_SKIP           = 0x00; // skip this record and continue
    public static final int DBRH_STOP           = 0x01; // stop DBRecord selection loop
    public static final int DBRH_SAVE           = 0x10; // save record and continue
    public static final int DBRH_SAVE_STOP      = 0x11; // save record and stop
    public static final int DBRH_SAVE_LAST      = 0x20; // save last record and continue
    public static final int DBRH_SAVE_LAST_STOP = 0x21; // save last record and stop

    // ------------------------------------------------------------------------
    
    /**
    *** Callback handler for DBRecords retrieved from a database select
    *** @param rcd  The DBRecord
    *** @return  The implementation method should return one of the following values:<br>
    ***             DBRH_SKIP      - skip current record and continue<br>
    ***             DBRH_STOP      - skip current record and stop<br>
    ***             DBRH_SAVE      - save current record and continue<br>
    ***             DBRH_SAVE_STOP - save current record and stop<br>
    ***             DBRH_SAVE_LAST - save prior record and continue<br>
    ***          Any saved records will be returned in an array to the original calling thread.
    **/
    public int handleDBRecord(RT rcd)
        throws DBException;
    
}
