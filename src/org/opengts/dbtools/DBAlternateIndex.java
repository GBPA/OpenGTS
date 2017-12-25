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
//  2008/05/14  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBAlternateIndex</code> holds information for a single defined alternate index.
**/

public class DBAlternateIndex
{

    // ------------------------------------------------------------------------

    private static int                          UniqueIndexID = 1;

    private DBFactory<? extends DBRecord<?>>    factory     = null;
    private String                              name        = "";
    private java.util.List<DBField>             keys        = null;
    private boolean                             doSort      = true;
    private DBField                             fld[]       = null;

    private boolean                             isUnique    = false;

    /**
    *** Constructor
    *** @param factory   The DBFactory instance
    *** @param indexName The index name
    **/
    public DBAlternateIndex(DBFactory<? extends DBRecord<?>> factory, String indexName) 
    {
        super();
        this.factory = factory;
        if (StringTools.isBlank(indexName)) {
            // this should be fixed
            Print.logStackTrace("Index name is blank!");
            this.name = DBProvider.DEFAULT_ALT_INDEX_NAME + (UniqueIndexID++);
        } else {
            this.name = indexName;
        }
        this.keys = new Vector<DBField>();
        this.doSort = true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the DBFactory instance
    *** @return The DBFactory instance
    **/
    public DBFactory<? extends DBRecord<?>> getFactory() 
    {
        return this.factory;
    }
    
    /**
    *** Returns the table name
    *** @return The table name
    **/
    public String getUntranslatedTableName()
    {
        return (this.factory != null)? this.factory.getUntranslatedTableName() : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the index name
    *** @return The index name
    **/
    public String getIndexName() 
    {
        return this.name;
    }

    /**
    *** Returns true if the index is unique
    *** @return True if the index is unique
    **/
    public boolean isUnique() 
    {
        return this.isUnique;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified field to this index
    *** @param field  The DBField to add
    **/
    public void addField(DBField field) 
    {
        if (field != null) {
            this.keys.add(field);
            this.fld = null;
            if (field.isUniqueAltKey()) {
                this.isUnique = true;
            }
            if (field.getAlternateIndexSequence(this.getIndexName()) < 0) {
                this.doSort = false;
            }
        }
    }

    /**
    *** Returns an array of DBFields for this index
    *** @return An array of DBFields for this index
    **/
    public DBField[] getFields() 
    {
        if (this.fld == null) {
            // -- initialize fields
            this.fld = this.keys.toArray(new DBField[this.keys.size()]);
            // -- sort by sequence
            if (this.doSort) {
                final String ndxName = this.getIndexName();
                ListTools.sort(this.fld, new Comparator<DBField>() {
                    public int compare(DBField df1, DBField df2) {
                        int s1 = df1.getAlternateIndexSequence(ndxName);
                        int s2 = df2.getAlternateIndexSequence(ndxName);
                        if ((s1 < 0) && (s2 < 0)) {
                            // -- (unlikely) stable sort, these fields will retain their original order
                            return 0;
                        } else
                        if (s1 < 0) {
                            // -- (unlikely) df1 sorts to end of list
                            return 1;
                        } else
                        if (s2 < 0) {
                            // -- (unlikely) df2 sorts to end of list
                            return -1;
                        } else {
                            // -- sorts based on sequence
                            return (s1 < s2)? -1 : 1;
                        }
                    }
                });
            }
        }
        return this.fld;
    }
    
    /**
    *** Returns a String list of comma-separated field names
    *** @return A String containing a comma-separated list of field names
    **/
    public String getFieldNames()
    {
        StringBuffer sb = new StringBuffer();
        for (Iterator<DBField> i = this.keys.iterator(); i.hasNext();) {
            DBField f = i.next();
            if (sb.length() > 0) { sb.append(","); }
            sb.append(f.getName());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this DBAlternateIndex
    *** @return A String representation of this DBAlternateIndex
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[").append(this.getUntranslatedTableName()).append("]");
        sb.append(this.getIndexName()).append(":").append(this.getFieldNames());
        return sb.toString();
    }

    /** Returns true if the specified Object is equivalent to this DBAlternativeIndex
    *** @param other  The other Object
    *** @return True if the specified Object is equivalent to this DBAlternativeIndex
    **/
    public boolean equals(Object other)
    {
        if (other instanceof DBAlternateIndex) {
            return this.toString().equals(other.toString());
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

}
