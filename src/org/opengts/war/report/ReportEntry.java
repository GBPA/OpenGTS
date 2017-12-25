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
//  2007/03/30  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.report;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class ReportEntry
{

    // ------------------------------------------------------------------------

    private ReportFactory   reportFactory = null;
    private String          aclName       = null;
    private Set<String>     omitColNames  = null;
        
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public ReportEntry()
    {
        super();
    }

    /**
    *** Constructor
    **/
    public ReportEntry(ReportFactory rf, String aclName)
    {
        this(rf, aclName, null);
    }

    /**
    *** Constructor
    **/
    public ReportEntry(ReportFactory rf, String aclName, Set<String> omitCols)
    {
        this();
        this.reportFactory = rf;
        this.aclName       = aclName;
        this.omitColNames  = omitCols;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ReportFactory instance for this report entry
    **/
    public ReportFactory getReportFactory()
    {
        return this.reportFactory;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ACL name for this report entry
    **/
    public String getAclName()
    {
        return this.aclName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the set of columns names to be omitted from the generated report
    *** (may return null if no columns are to be omitted)
    **/
    public Set<String> getOmittedColumnNames()
    {
        return this.omitColNames; // may be null
    }

    // ------------------------------------------------------------------------

}
