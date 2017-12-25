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
*** <code>DBLoadValidatorAdapter</code> provides table record load validation
**/

public class DBLoadValidatorAdapter
    implements DBLoadValidator
{

    // ------------------------------------------------------------------------

    private String                  fields[]        = null;
    private HashMap<String,Integer> fieldIndexMap   = null;
    private int                     count           = 0;
    private int                     errors          = 0;

    /** 
    *** Constructor
    **/
    public DBLoadValidatorAdapter()
    {
        super();
        this.errors = 0;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Clears the record count
    **/
    public void clearCount()
    {
        this.count = 0;
    }

    /**
    *** Gets the record count
    **/
    public int getCount() 
    {
        return this.count;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets an error condition
    **/
    public void setError()
    {
        this.errors++;
    }

    /**
    *** Returns trus if an error has occurred
    **/
    public boolean hasErrors()
    {
        return (this.errors > 0)? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Set fields on which this DBLoadValidatorAdapter operates
    **/
    public boolean setFields(String f[]) 
        throws DBException
    {

        /* clear record count */
        this.clearCount();

        /* set fields */
        this.fields = f;
        if (ListTools.isEmpty(this.fields)) {
            Print.logError("No fields specified");
            this.setError();
            return false;
        }

        /* init field index map */
        this.fieldIndexMap = new HashMap<String,Integer>();
        for (int ndx = 0; ndx < this.fields.length; ndx++) {
            String fld = this.fields[ndx];
            if (this.fieldIndexMap.containsKey(fld)) {
                Print.logError("Field specified more than once: " + fld);
                this.setError();
                return false;
            }
            this.fieldIndexMap.put(fld,new Integer(ndx));
        }

        /* success */
        return true;

    }
    
    /**
    *** Gets the fields on which this DBLoadValidatorAdapter operates
    **/
    public String[] getFields()
    {
        return this.fields;
    }

    /**
    *** Returns true if the specified field is defined
    **/
    public boolean hasField(String fldName) 
    {
      //return ListTools.contains(this.getFields(),fldName);
        return (this.fieldIndexMap != null)? this.fieldIndexMap.containsKey(fldName) : false;
    }

    /**
    *** Returns true if the specified field is defined
    **/
    public int getFieldIndex(String fldName) 
    {
      //return ListTools.indexOf(this.getFields(),fldName);
        Integer iNdx = (this.fieldIndexMap != null)? this.fieldIndexMap.get(fldName) : null;
        return (iNdx != null)? iNdx.intValue() : -1;
    }

    /**
    *** Gets the value from the array for the specified field name.
    *** Returns null if the value is not defined
    **/
    public String getFieldValue(String fldName, String values[])
    {

        /* no values? */
        if (ListTools.isEmpty(values)) {
            return null;
        }

        /* get index */
        int ndx = this.getFieldIndex(fldName);
        if ((ndx < 0) || (ndx >= values.length)) {
            return null;
        }

        /* return value */
        return values[ndx];

    }

    /**
    *** Gets the value from the array for the specified field name.
    *** Returns null if the value is not defined
    **/
    public boolean setFieldValue(String fldName, String values[], String val)
    {

        /* no values? */
        if (ListTools.isEmpty(values)) {
            return false;
        }

        /* get index */
        int ndx = this.getFieldIndex(fldName);
        if ((ndx < 0) || (ndx >= values.length)) {
            return false;
        }

        /* return value */
        values[ndx] = (val != null)? val : "";
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Validate the initial values which will be updated/inserted into the table
    **/
    public boolean validateValues(String v[])
        throws DBException
    {

        /* count record */
        this.count++;

        /* no fields? */
        if (this.fields == null) { // empty fields already checked in "setFields"
            Print.logError("No fields specified");
            this.setError();
            return false;
        } 

        /* number of values matches number of fields? */
        if (v == null) {
            Print.logError("No field values specified");
            this.setError();
            return false;
        } else
        if (v.length != this.fields.length) {
            int vlen = v.length;
            int flen = this.fields.length;
            Print.logError("Invalid # of fields (found=" + vlen + ", expected=" + flen + ") [" + this.count + "]");
            this.setError();
            return false;
        }

        /* success */
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Validate the actual DBRecord prior to insertion (new record)
    **/
    public boolean validateInsert(DBRecord<?> dbr) 
        throws DBException
    {
        return this.validateRecord(dbr, true, null);
    }

    /**
    *** Callback after record has been inserted
    **/
    public void recordDidInsert(DBRecord<?> dbr)
    {
        this.recordDidInsertUpdate(dbr, true, null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Validate the actual DBRecord prior to update (record exists)
    **/
    public boolean validateUpdate(DBRecord<?> dbr, Set<String> fieldNameList)
        throws DBException
    {
        return this.validateRecord(dbr, false, fieldNameList);
    }

    /**
    *** Callback after record has been updated
    **/
    public void recordDidUpdate(DBRecord<?> dbr, Set<String> updFields)
    {
        this.recordDidInsertUpdate(dbr, false, updFields);
    }

    // ------------------------------------------------------------------------

    /**
    *** Called to validate record to be inserted/updated
    *** @param dbr        The record to be inserted/updated
    *** @param newRecord  True if the record will be inserted, false if updated
    *** @param updFields  The list of fields that will be updated (only specified if "newRecord" is false)
    **/
    public boolean validateRecord(DBRecord<?> dbr, boolean newRecord, Set<String> updFields)
        throws DBException
    {
        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Callback after record has been inserted/updated
    *** @param dbr        The record inserted/updated
    *** @param newRecord  True if the record was inserted, false if updated
    *** @param updFields  The list of fields that were updated (only specified if "newRecord" is false)
    **/
    protected void recordDidInsertUpdate(DBRecord<?> dbr, boolean newRecord, Set<String> updFields)
    {
        // --
    }

    // ------------------------------------------------------------------------

}
