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
//  2016/04/06  Martin D. Flynn
//     -Moved from DBFactory.java
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.*;
import java.text.*;
import java.sql.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;

/**
*** <code>DBLoadValidator</code> provides table record load validation
**/

public interface DBLoadValidator
{

    /**
    *** Set fields on which this DBLoadValidator operates
    **/
    public boolean setFields(String fields[]) throws DBException;

    // ------------------------------------------------------------------------

    /**
    *** Validate the initial values which will be updated/inserted into the table
    **/
    public boolean validateValues(String values[])  throws DBException;

    // ------------------------------------------------------------------------

    /**
    *** Validate the actual DBRecord prior to insertion (new record)
    **/
    public boolean validateInsert(DBRecord<?> dbr) throws DBException;

    /**
    *** Callback after record has been inserted
    **/
    public void    recordDidInsert(DBRecord<?> dbr);

    // ------------------------------------------------------------------------

    /**
    *** Validate the actual DBRecord prior to update (record exists)
    **/
    public boolean validateUpdate(DBRecord<?> dbr, Set<String> updFields) throws DBException;

    /**
    *** Callback after record has been updated
    **/
    public void    recordDidUpdate(DBRecord<?> dbr, Set<String> updFields);

}
