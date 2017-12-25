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
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/09  Martin D. Flynn
//     -Integrate DBException
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Integrated with "OpenGTS"
//  2007/02/25  Martin D. Flynn
//     -Added ability to load records from CSV files
//  2007/02/28  Martin D. Flynn
//     -Added load file 'InsertionValidator'
//     -Changed 'validateColumns' to make sure all 'defined' columns are found in
//      the table.  Additional columns existing in the table are allowed.
//  2007/03/16  Martin D. Flynn 
//     -Added XML output format to 'dump support
//  2007/06/13  Martin D. Flynn
//     -Added 'displayWarnings' flag to 'validateColumns(...)'
//  2007/07/27  Martin D. Flynn
//     -Added 'validateMask' to 'validateColumns' method
//  2007/09/16  Martin D. Flynn
//     -Added check for "UNIQUE KEY" row in "CREATE TABLE" block when reloading 
//      tables.
//     -Added check for OutOfMemoryError when attempting to retrieve large result
//      sets.  [see DBConnection.createStatement(...) for more information]
//     -Added "Row-by-Row" option on the 'execute' and 'getStatement' methods.
//      [see DBConnection.createStatement(...) for more information]
//     -Integrated DBSelect
//     -Display summary of dropped columns before loading table data from file.
//  2008/02/27  Martin D. Flynn
//     -Added 'isEditable' method
//  2008/03/12  Martin D. Flynn
//     -Added 'recreateAlternateIndexes' method for recreating the alternate key index
//     -Added a separate method for analyzing bean-access methods for columns.
//     -Added javadocs.
//  2008/05/14  Martin D. Flynn
//     -Fixed possible NPE in '_dumpTable' when 'dsel' is null.
//     -Added support for creating multiple alternate indexes
//     -Optimized field lookups
//     -Added initial Java 5 'generics'
//  2008/05/20  Martin D. Flynn
//     -Added initial implementation for ommiting optional fields, based on a runtime
//      configuration.
//  2008/06/20  Martin D. Flynn
//     -Added 'CustomFactoryHandler' interface for overriding DBFacory creation and
//      field selection on startup initialization.
//  2009/01/28  Martin D. Flynn
//     -Added changes to support UTF8 character sets
//  2009/05/24  Martin D. Flynn
//     -Changed "_loadInsertRecord" to load records via DBRecord insert/update.
//  2009/07/01  Martin D. Flynn
//     -Changed DBRecord XML format
//  2009/09/23  Clifton Flynn / Martin D. Flynn
//     -Added 'soapXML' argument to various methods.
//  2009/11/01  Martin D. Flynn
//     -Added support for 'autoIndex' field
//  2010/01/29  Martin D. Flynn
//     -Modified MySQL handler to add all missing columns at one time.
//  2011/03/08  Martin D. Flynn
//     -Added "validateColumns" check for invalid primary/alternate keys
//  2011/06/16  Martin D. Flynn
//     -Remove FLD_creationTime and FLD_creationMillis update on load.
//     -Added alternate MySQL mechanism for checking table existance on Windows.
//      (see "tableExists()")
//     -Changed "isOptional" to "isRequired" ("setRequired" called in DBAdmin.java)
//  2012/01/29  Martin D. Flynn
//     -Added check for "DBField.IgnoreColumnError" when checking for missing columns
//      during table verification.
//     -PostgreSQL support added (by Gaurav Kohli)
//  2013/05/28  Martin D. Flynn
//     -Added support for reading approximate InnoDB record counts from the statement
//      "show table status where Name='TABLE'". (fixed NPE issue 2.5.2-B56)
//  2013/09/29  Martin D. Flynn/
//     -Fixed NPE caused by "getMySQLTableStatus" not calling "_setIndexType". 
//  2014/09/16  Martin D. Flynn
//     -Added initial code to improve performance for checking the existance of 
//      SQL Server tables (may need additional testing). [see "tableExists()"]
//  2015/12/29  Martin D. Flynn
//     -Added/Changed InsertionValidator interface methods. [2.6.1-B45]
//  2016/04/06  Martin D. Flynn
//     -Moved "InsertionValidator" to "DBLoadValidator.java"
//     -Added "hasExistingColumn" [2.6.2-B72]
//  2017/03/14  Martin D. Flynn
//     -Added "writeJSON_DBField" to return a JSON object of the target record.
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
*** <code>DBFactory</code> provides SQL table specific information
**/

public class DBFactory<gDBR extends DBRecord<gDBR>>
    implements DBRecordListener<gDBR>
{

    // ------------------------------------------------------------------------
    // allow "SELECT COUNT(*)" method on MySQL for table existance?

    private   static       boolean MYSQL_tableExists_init           = false;
    private   static       boolean MYSQL_tableExists_UseSelectCount = true;

    public static boolean mysqlTableExistsUseSelectCount()
    {
        // db.mysql.tableExistsSelectCount
        if (!MYSQL_tableExists_UseSelectCount) {
            // "false" prevails
            return false;
        } else
        if (!MYSQL_tableExists_init) {
            MYSQL_tableExists_init = true;
            boolean v = RTConfig.getBoolean(RTKey.DB_MYSQL_TBLEXIST_SEL_COUNT,MYSQL_tableExists_UseSelectCount);
            // should also not use "count(*)" for InnoDB engines
            if (!v) {
                // explicit disallow
                MYSQL_tableExists_UseSelectCount = false;
                Print.logDebug("MySQL: Explicitly disallow 'SELECT COUNT(*)' for table existence"); 
            } else
            if (OSTools.isWindows()) {
                // do not use "SELECT COUNT(*) FROM table" on Windows (too slow)
                MYSQL_tableExists_UseSelectCount = false;
                Print.logDebug("MySQL: Windows disallow 'SELECT COUNT(*)' for table existence"); 
            } else
            if (DBProvider.isMySqlInnoDB()) {
                // do not use "SELECT COUNT(*) FROM table" for MySQL/InnoDB engine
                MYSQL_tableExists_UseSelectCount = false;
                Print.logDebug("MySQL/InnoDB: disallow 'SELECT COUNT(*)' for table existence on InnoDB"); 
            } else {
                // otherwise allow
                MYSQL_tableExists_UseSelectCount = true;
                //Print.logDebug("MySQL: Allowing 'SELECT COUNT(*)' for table existence"); 
            }
        }
        return MYSQL_tableExists_UseSelectCount;
    }

    // ------------------------------------------------------------------------
    // archived db file extensions

    public    static final String  ARCHIVE_EXT_CSV          = "csv";
    public    static final String  ARCHIVE_EXT_DUMP         = "dump";
    public    static final String  ARCHIVE_EXT_SQL          = "sql";
    public    static final String  ARCHIVE_EXT_TXT          = "txt";
    public    static final String  ARCHIVE_EXT_XML          = "xml";

    // ------------------------------------------------------------------------
    // XML
    
    public    static final String  CMD_dbget                = "dbget";
    public    static final String  CMD_dbput                = "dbput";
    public    static final String  CMD_dbdel                = "dbdel";
    public    static final String  CMD_dbcreate             = "dbcreate";
    public    static final String  CMD_dbschema             = "dbschema";

    public    static final String  TAG_TableSchemas         = "TableSchemas";
    public    static final String  TAG_TableSchema          = "TableSchema";

    public    static final String  TAG_Records              = "Records";
    public    static final String  TAG_Record               = "Record";
    
    public    static final String  TAG_RecordKeys           = "RecordKeys";
    public    static final String  TAG_RecordKey            = "RecordKey";
    
    public    static final String  TAG_Field                = "Field";
    public    static final String  TAG_Description          = "Description";

    public    static final String  ATTR_table               = "table";
    public    static final String  ATTR_name                = "name";
    public    static final String  ATTR_sequence            = "sequence";
    public    static final String  ATTR_type                = "type";
    public    static final String  ATTR_title               = "title";
    public    static final String  ATTR_primaryKey          = "primaryKey";
    public    static final String  ATTR_alternateKeys       = "alternateKeys";
    public    static final String  ATTR_partial             = "partial";
    public    static final String  ATTR_count               = "count";
    public    static final String  ATTR_value               = "value";

    // ------------------------------------------------------------------------
    // Key type
    
    /**
    *** DBRecord key type 
    **/
    public enum KeyType {
        PRIMARY,            // table
        UNIQUE,             // table
        INDEX,              // table, index
        UNIQUE_INDEX        // table, index
    };

    // ------------------------------------------------------------------------
    // MySQL error codes: [as returned by "sqe.getErrorCode()"]
    // [http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html]
    //   1007 - Database already exists
    //   1045 - Invalid authorization specification: Access denied for user
    //   1049 - Unknown database '??'
    //   1054 - Unknown column '??' in 'field list'.
    //   1062 - Duplicate entry
    //   1064 - Statement syntax error
    //   1100 - Didn't lock all tables
    //   1146 - Table doesn't exist
    //   ...

    // MySQL
    public static final int SQLERR_DATABASE_EXISTS      =  1007;
    public static final int SQLERR_INVALID_AUTH         =  1045; // Access Denied
    public static final int SQLERR_UNKNOWN_DATABASE     =  1049;
    public static final int SQLERR_UNKNOWN_COLUMN       =  1054;
    public static final int SQLERR_DUPLICATE_KEY        =  1062;
    public static final int SQLERR_SYNTAX_ERROR         =  1064;
    public static final int SQLERR_TABLE_NOTLOCKED      =  1100;
    public static final int SQLERR_TABLE_NONEXIST       =  1146;

    // SQLServer
    public static final int MSQL_ERR_INVALID_OBJECT     =   208; 
    public static final int MSQL_ERR_CANT_DROP_TABLE    =  3701; 
    public static final int MSQL_ERR_LOGIN_EXISTS       = 15025;  // "The server principal 'zzzz' already exists"
    public static final int MSQL_ERR_USER_EXISTS        = 15023;  // "User, group, or role 'zzzz' already exists"
  //public static final int MSQL_ERR_UNKNOWN_COLUMN     =     ?;  // does not appear to be supported by MSQL 

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static CustomFactoryHandler factoryHandler = null;
    
    /**
    *** CustomFactoryHandler interface
    **/
    public static interface CustomFactoryHandler
    {

        /**
        *** Create a DBFactory instance.<br>
        *** (may return null to indicate that the default DBFactory should be created)
        **/
        public <T extends DBRecord<T>> DBFactory<T> createDBFactory(
            String utableName, // untranslated table name
            DBField field[], 
            DBFactory.KeyType keyType, 
            Class<T> rcdClass, 
            Class<? extends DBRecordKey<T>> keyClass, 
            boolean editable, boolean viewable);
        
        /**
        *** Augment selected DBFields
        **/
        public <T extends DBRecord<T>> java.util.List<DBField> selectFields(DBFactory<T> factory, java.util.List<DBField> fields);
        
    }

    /**
    *** Sets the global CustomFactoryHandler
    *** @param factoryHandler  The CustomFactoryHandler
    **/
    public static void setCustomFactoryHandler(CustomFactoryHandler factoryHandler)
    {
        DBFactory.factoryHandler = factoryHandler;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected static HashMap<String,String>     TableNameMap = null; 
    protected static java.util.List<DBFactory<? extends DBRecord<?>>> factoryList = new Vector<DBFactory<? extends DBRecord<?>>>();
    
    /**
    *** Add a table name mapping 
    *** @param globalTableName  Class name of table (sans package name)
    *** @param actualTableName  Assigned name of table (untranslated)
    **/
    protected static void addActualTableName(String globalTableName, String actualTableName)
    {
        if (!globalTableName.equals(actualTableName)) {
            if (DBFactory.TableNameMap == null) {
                DBFactory.TableNameMap = new HashMap<String,String>();
            }
            DBFactory.TableNameMap.put(globalTableName, actualTableName);
        }
    }

    /**
    *** Gets the actual table name for the specified class module table name
    **/
    public static String getActualTableName(String utableName)
    {
        String tn = (DBFactory.TableNameMap != null)? DBFactory.TableNameMap.get(utableName) : null;
        return (tn != null)? tn : utableName;
    }

    /**
    *** Gets the DBFactory for the specified table name
    *** @param utableName  The untranslated table name
    *** @return The DBFactory for the specified table, or null if the table cannot be found
    **/
    public static DBFactory<? extends DBRecord<?>> getFactoryByName(String utableName)
    {
        String utn = DBFactory.getActualTableName(utableName);
        for (Iterator<DBFactory<? extends DBRecord<?>>> i = DBFactory.factoryList.iterator(); i.hasNext();) {
            DBFactory<? extends DBRecord<?>> fact = i.next();
            if (fact.getUntranslatedTableName().equals(utn)) { 
                return fact; 
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified tableName is a subclass of the specified class name
    **/
    public static <T extends DBRecord<T>> boolean isTableClass(DBFactory<T> tableFact, Class<?> tableClass)
    {
        if (tableFact == null) {
            return false;
        } else
        if (tableClass == null) {
            return false;
        } else {
            return tableClass.isAssignableFrom(tableFact.getRecordClass());
        }
    }

    /**
    *** Returns true if the specified tableName is a subclass of the specified class name
    **/
    public static boolean isTableClass(String utableName, Class<?> tableClass)
    {
        return DBFactory.isTableClass(DBFactory.getFactoryByName(utableName), tableClass);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                                  utableName          = null;

    private Object                                  indexTypeLock       = new Object();
    private String                                  indexType           = ""; // defaults to "MyISAM" for MySQL

    private Class<gDBR>                             rcdClass            = null;
    private boolean                                 isRequired          = false;

    private DBField                                 priKeys[]           = null;
    private KeyType                                 keyType             = KeyType.PRIMARY;
    private Class<? extends DBRecordKey<gDBR>>      keyClass            = null;

    private OrderedMap<String,DBAlternateIndex>     altIndexMap         = null;

    private Object                                  existingFieldLock   = new Object();
    private DBField                                 existingField[]     = null;
    private Map<String,DBField>                     existingFieldMap    = null;

    private OrderedMap<String,DBField>              fieldMap            = null;
    private boolean                                 fieldArrayReady     = false;
    private DBField                                 fieldArray[]        = null;     // optimization

    private boolean                                 editable            = true;
    private boolean                                 viewable            = true;

    private Vector<String>                          parentTables        = new Vector<String>();
    private volatile String                         hierarchy           = null; // lazy initialization

    private DBFactory<? extends DBRecord<gDBR>>     childFactories[]    = null;

    private DBRecordListener<gDBR>                  recordListener      = null;

    private boolean                                 logMissingColumns   = true; // default log

    private boolean                                 allowInnoDBCOUNT    = true; // allow "COUNT(*)" for InnoDB

    // ------------------------------------------------------------------------

    /**
    *** Create a DBFactory instance.
    *** @param utableName The untranslated name of the table
    *** @param field      The DBFields in the table
    *** @param keyType    The table key type
    *** @param rcdClass   The DBRecord subclass representing the table
    *** @param keyClass   The DBRecordKey subclass representing the table key
    *** @param editable   True if this table should be editable, false otherwise.  
    ***                   This value is used by the GTSAdmin application.
    *** @param viewable   True if this table should be viewable, false otherwise.  
    ***                   An 'editable' table is automatically considered viewable.
    ***                   This value is used by the GTSAdmin application.
    *** @return The DBFactory instance.
    ***/
    public static <T extends DBRecord<T>> DBFactory<T> createDBFactory(
        String  utableName, // untranslated table name
        DBField field[], 
        KeyType keyType, 
        Class<T> rcdClass, 
        Class<? extends DBRecordKey<T>> keyClass, 
        boolean editable, boolean viewable) 
    {
        if (DBFactory.factoryHandler != null) {
            try {
                DBFactory<T> fact = DBFactory.factoryHandler.createDBFactory(
                    utableName, field, keyType, rcdClass, keyClass, editable, viewable);
                if (fact != null) {
                    return fact;
                }
            } catch (Throwable th) {
                Print.logException("'<DBFactory.CustomFactoryHandler>.createDBFactory' failed for table " + utableName, th);
            }
        }
        return new DBFactory<T>(utableName, field, keyType, rcdClass, keyClass, editable, viewable);
    }

    /**
    *** Constructor 
    *** @param utableName The untranslated name of the table
    *** @param field      The DBFields in the table
    *** @param keyType    The table key type
    *** @param rcdClass   The DBRecord subclass representing the table
    *** @param keyClass   The DBRecordKey subclass representing the table key
    *** @param editable   True if this table should be editable, false otherwise.  
    ***                   This value is used by the GTSAdmin application.
    *** @param viewable   True if this table should be viewable, false otherwise.
    ***                   An 'editable' table is automatically considered viewable.
    ***                   This value is used by the GTSAdmin application.
    **/
    public DBFactory(
        String  utableName, // untranslated table name
        DBField field[], 
        KeyType keyType, 
        Class<gDBR> rcdClass, 
        Class<? extends DBRecordKey<gDBR>> keyClass, 
        boolean editable, boolean viewable) 
    {

        /* table name */
        this.utableName = StringTools.trim(utableName); // untranslated
        if (StringTools.isBlank(this.utableName)) {
            Print.logError("Table name may not be null/blank!");
            throw new RuntimeException("Table name may not be null/blank!");
        }

        /* DBRecord class type */
        if (rcdClass == null) {
            Print.logStackTrace("Record class type cannot be null");
            throw new RuntimeException("Record class type cannot be null");
        } else
        if (!DBRecord.class.isAssignableFrom(rcdClass)) {
            Print.logStackTrace("Record class must be a subclass of DBRecord");
            throw new RuntimeException("Record class must be a subclass of DBRecord");
        }

        /* DBRecordKey class type */
        if (keyClass == null) {
            Print.logStackTrace("Key class type cannot be null");
            throw new RuntimeException("Key class type cannot be null");
        } else
        if (!DBRecordKey.class.isAssignableFrom(keyClass)) {
            Print.logStackTrace("Key class must be a subclass of DBRecordKey");
            throw new RuntimeException("Key class class must be a subclass of DBRecordKey");
        }

        /* record/key class */
        this.rcdClass    = rcdClass;
        this.keyClass    = keyClass;
        this.keyType     = keyType;

        /* index type */
        this.indexType   = null; // initialized later

        /* register contained Enum classes */
        EnumTools.registerPublicEnumClasses(this.rcdClass);

        /* table attributes */
        this.editable    = editable;
        this.viewable    = this.editable || viewable;

        /* optional? */
        String rcdClassStr = StringTools.className(this.rcdClass);
        //this.setRequired((rcdClassStr.indexOf(".opt.") < 0));

        /* class module table name */
        int p = rcdClassStr.lastIndexOf(".");
        if (p > 0) {
            String classTableName = rcdClassStr.substring(p+1); // class name
            DBFactory.addActualTableName(classTableName, this.utableName);
        }

        /* precheck field list */
        java.util.List<DBField> preList = ListTools.toList(field);
        for (Iterator<DBField> i = preList.iterator(); i.hasNext();) {
            if (i.next() == null) { i.remove(); } // remove null fields
        }
        // -- all null fields have been removed at this point 
        if (DBFactory.factoryHandler != null) {
            try {
                preList = DBFactory.factoryHandler.selectFields(this, preList);
            } catch (Throwable th) {
                Print.logException("'<DBFactory.CustomFactoryHandler>.selectFields' failed for table " + this.utableName, th);
            }
            if (preList == null) {
                Print.logWarn("'<DBFactory.CustomFactoryHandler>.selectFields' returned NULL for table " + this.utableName);
                preList = new Vector<DBField>();
            }
        }

        /* accumulate primary/alternate key fields */
        this.altIndexMap = null;
        this.fieldMap    = new OrderedMap<String,DBField>();
        java.util.List<DBField> pkList = new Vector<DBField>();
        for (DBField fld : preList) {
            fld.setFactory(this);
            String fn = fld.getName();

            // -- already defined?
            if (this.fieldMap.containsKey(fn)) {
                Print.logError("DBField already defined for table: " + this.utableName + "." + fn);
                //Print.logStackTrace("DBField already defined for table: " + this.utableName + "." + fn);
                continue;
            }

            // -- save
            this.fieldMap.put(fn, fld);

            // -- primary key
            if (fld.isPrimaryKey()) { 
                pkList.add(fld); 
            }

            // -- alternate keys
            String altNames[] = fld.getAlternateIndexes();
            if ((altNames != null) && (altNames.length > 0)) {
                if (this.altIndexMap == null) {
                    this.altIndexMap = new OrderedMap<String,DBAlternateIndex>();
                }
                for (int a = 0; a < altNames.length; a++) {
                    DBAlternateIndex altKey = this.altIndexMap.get(altNames[a]);
                    if (altKey == null) {
                        altKey = new DBAlternateIndex(this, altNames[a]);
                        this.altIndexMap.put(altNames[a], altKey);
                    }
                    altKey.addField(fld);
                }
            }

        }
        this.priKeys = pkList.toArray(new DBField[pkList.size()]);

        /* force creation of field array now */
        this.fieldArray = null;
        this.fieldArrayReady = false;
        this.getFields();

        /* record insert/update listeners (EXPERIMENTAL) */
        // -- ie. "User.recordListener=PATH_TO_CLASS"
        // -    recordWillInsert/recordDidInsert
        // -    recordWillUpdate/recordDidUpdate
        String rcdCBKey = utableName + RTKey._DB_RECORD_LISTENER;
        if (RTConfig.hasProperty(rcdCBKey)) {
            String cbClassName = RTConfig.getString(rcdCBKey,"");
            this.setRecordListener(cbClassName);
        }

        /* set "logMissingColumns" default */
        // -- ie. EventData.logMissingColumns=false
        String lmcKey = utableName + RTKey._DB_LOG_MISSING_COLUMNS;
        if (RTConfig.hasProperty(lmcKey)) {
            boolean lmc = RTConfig.getBoolean(lmcKey,true);
            //Print.logInfo(lmcKey + "=" + lmc);
            this.setLogMissingColumnWarnings(lmc);
        }

        /* add this DBFactory to the list of managed factories */
        DBFactory.factoryList.add(this);

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a list of all added DBFactory instances
    **/
    public static java.util.List<DBFactory<? extends DBRecord<?>>> getFactoryList()
    {
        return DBFactory.factoryList;
    }

    /**
    *** Sort the list of DBFactory instances by their hierarchy.
    *** The sort is performed in-place
    **/
    public static void sortByHierarchy(DBFactory<? extends DBRecord<?>> list[])
    {
        Comparator<DBFactory<? extends DBRecord<?>>> comp = new ListTools.StringComparator<DBFactory<? extends DBRecord<?>>>() {
            public String getObjectString(Object obj) {
                return (obj != null)? ((DBFactory<?>)obj).getHierarchyString() : "";
            }
        };
        ListTools.sort(list, comp);
    }

    // ------------------------------------------------------------------------

    /* table description static method handle */
    private MethodAction tableDescriptionMethod = null;
    
    /**
    *** Return the description of the table represented by this DBFactory
    *** @param loc   The Locale representing the language for the returned description
    *** @return The description of the table
    **/
    public String getDescription(Locale loc)
    {
        try {
            if (this.tableDescriptionMethod == null) {
                this.tableDescriptionMethod = new MethodAction(this.rcdClass,"getTableDescription",Locale.class);
            }
            return (String)this.tableDescriptionMethod.invoke(loc);
        } catch (Throwable th) {
            Print.logError("Unable to retrieve table description: " + th);
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the 'required' state of this DBFactory
    *** @param required The 'required' state
    **/
    public void setRequired(boolean required)
    {
        this.isRequired = required;
    }
    
    /**
    *** Returns true if this table is required.  
    *** @return True if this table is required
    **/
    public boolean isRequired()
    {
        return this.isRequired;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this table is editable
    *** @return True if this table is editable
    **/
    public boolean isEditable()
    {
        return this.editable;
    }

    /**
    *** Returns true if this table is viewable
    *** @return True if this table is viewable
    **/
    public boolean isViewable()
    {
        return this.isEditable() || this.viewable;
    }

    // ------------------------------------------------------------------------

    /**
    *** Clears the column map for the existing columns
    **/
    public void clearExistingColumnMap()
    {
        synchronized (this.existingFieldLock) {
            this.existingField = null;
            this.existingFieldMap = null;
        }
    }
    
    /**
    *** Retrieves the list of existing columns in the database for this DBFactory
    *** @param update  True to update the current map, false to return the previously cached map
    *** @return A map of exiting table columns
    *** @throws DBException   If a database error occurs
    **/
    public Map<String,DBField> getExistingColumnMap(boolean update)
        throws DBException
    {
        synchronized (this.existingFieldLock) {
            if (update || (this.existingFieldMap == null)) {
                DBField fld[] = this.getExistingColumns(update);
                if (fld != null) {
                    this.existingFieldMap = new HashMap<String,DBField>();
                    for (int i = 0; i < fld.length; i++) {
                        String fldName = fld[i].getName();
                        this.existingFieldMap.put(fldName, fld[i]);
                    }
                } else {
                    this.existingFieldMap = null;
                }
            }
        }
        return this.existingFieldMap;
    }
    
    /**
    *** Retrieves an array of existing columns in the database for this DBFactory
    *** @param update  True to update the current list of columns, false to return the previously cached columns
    *** @return An array of exiting table columns
    *** @throws DBException   If a database error occurs
    **/
    public DBField[] getExistingColumns(boolean update)
        throws DBException
    {
        synchronized (this.existingFieldLock) {
            if (update || (this.existingField == null)) {
                // get fields (may throw DBException)
                this.existingFieldMap = null;
                this.existingField = DBProvider.getActualTableFields(this.getUntranslatedTableName());
            }
        }
        return this.existingField;
    }

    /** 
    *** Returns true if the specified field already exists in the table
    *** @param name  A field name to test
    *** @return True if the specified field exists in the table, false otherwise
    **/
    public boolean hasExistingColumn(String name) // see also "hasField(String)"
    {
        try {
            String n = this.getMappedFieldName(name);
            Map<String,DBField> colMap = this.getExistingColumnMap(false);
            return ((colMap != null) && colMap.containsKey(n))? true : false;
        } catch (DBException dbe) {
            Print.logException("Checking for existing column: " + name, dbe);
            return false;
        }
    }

    // --------------------------------

    /**
    *** Validate all existing table columns against the list of defined table columns.
    *** Warnings will be printed to the console.
    *** @return True if the validation was successful with no errors, false otherwise
    **/
    public boolean validateColumns()
    {
        return this.validateColumns(DBAdmin.VALIDATE_DISPLAY_ERRORS | DBAdmin.VALIDATE_DISPLAY_WARNINGS);
    }
    
    /**
    *** Validate all existing table columns against the list of defined table columns.
    *** @param validateMask  A bitmask indicating which type(s) of validation to perform
    *** @return True if the validation was successful with no errors, false otherwise
    **/
    public boolean validateColumns(int validateMask)
    {
        return this.validateColumns(validateMask, false);
    }

    /**
    *** Validate all existing table columns against the list of defined table columns.
    *** @param validateMask  A bitmask indicating which type(s) of validation to perform
    *** @param refreshColumns  If true, the list of existing columns will be updated prior to validation.
    *** @return True if the validation was successful with no errors, false otherwise
    **/
    public boolean validateColumns(int validateMask, boolean refreshColumns)
    {
        String  utableName          = this.getUntranslatedTableName();
        boolean addMissingColumns   = ((validateMask & DBAdmin.VALIDATE_ADD_COLUMNS     ) != 0);
        boolean alterColumnTypes    = ((validateMask & DBAdmin.VALIDATE_ALTER_COLUMNS   ) != 0);
        boolean rebuildKeys         = ((validateMask & DBAdmin.VALIDATE_REBUILD_KEYS    ) != 0);
        boolean checkColumnEncoding = ((validateMask & DBAdmin.VALIDATE_CHECK_ENCODING  ) != 0);
        boolean showColumns         = ((validateMask & DBAdmin.VALIDATE_SHOW_COLUMNS    ) != 0);
        boolean displayWarnings     = ((validateMask & DBAdmin.VALIDATE_DISPLAY_WARNINGS) != 0);
        boolean displayErrors       = ((validateMask & DBAdmin.VALIDATE_DISPLAY_ERRORS  ) != 0) || displayWarnings || showColumns;

        /* defined columns (as defined in the Java table wrapper) */
        DBField colDefined[] = this.getFields();
        if ((colDefined == null) || (colDefined.length == 0)) {
            if (displayErrors) {
                Print.logInfo("ERROR - " + utableName + ": No table columns defined!!!");
            }
            return false;
        }
        if (showColumns) {
            Print.logInfo("    Defined columns: " + utableName);
            for (int i = 0; i < colDefined.length; i++) {
                Print.logInfo("      " + this._columnInfo(i, colDefined[i], true));
            }
        }

        /* table columns (as found in the SQL table itself) */
        DBField colTable[];
        try {;
            colTable = this.getExistingColumns(refreshColumns);
            if (ListTools.isEmpty(colTable)) {
                if (displayErrors) {
                    Print.logInfo("ERROR - " + utableName + ": Existing table columns list is empty (not supported?)");
                }
                return false;
            }
        } catch (DBException dbe) {
            if (displayErrors) {
                Print.logInfo("ERROR - " + utableName + ": Error reading table columns!");
            }
            return false;
        }
        if (showColumns) {
            boolean showActual = false;
            if (colTable.length != colDefined.length) {
                showActual = true;
            } else {
                for (int i = 0; i < colTable.length; i++) {
                    if (!colTable[i].isTypeMatch() || !colDefined[i].equals(colTable[i])) {
                        showActual = true;
                        break;
                    }
                }
            }
            if (showActual) {
                Print.logInfo("    Actual columns: " + utableName + "  (as described in the database)");
                for (int i = 0; i < colTable.length; i++) {
                    Print.logInfo("      " + this._columnInfo(i, colTable[i], false));
                }
            }
        }

        /* create a set of existing column fields and names */
        OrderedMap<String,DBField> colTableMap = new OrderedMap<String,DBField>();
        for (int i = 0; i < colTable.length; i++) {
            colTableMap.put(colTable[i].getName(), colTable[i]);
        }

        /* compare individual columns */
        boolean columnsOK = true;
        OrderedSet<DBField> missingColumns        = new OrderedSet<DBField>();
        OrderedSet<DBField> typeMismatchColumns   = new OrderedSet<DBField>();
        OrderedSet<DBField> priKeyMismatchColumns = new OrderedSet<DBField>();
        OrderedSet<DBField> altKeyMismatchColumns = new OrderedSet<DBField>();
        for (int i = 0; i < colDefined.length; i++) {
            String columnName = colDefined[i].getName();

            /* check for column existance */
            DBField existingField = colTableMap.get(columnName);
            if (existingField == null) {
                // defined column not found in existing columns
                if (!DBField.IgnoreColumnError(utableName,columnName)) {
                    if (displayErrors) {
                        Print.logInfo("ERROR - " + utableName + ": Column '" + colDefined[i] + "' [" + i + "] not found");
                    }
                    missingColumns.add(colDefined[i]);
                    columnsOK = false;
                } else {
                    if (displayErrors) {
                        Print.logInfo("WARNING - " + utableName + ": Column '" + colDefined[i] + "' [" + i + "] not found (ignored)");
                    }
                }
                continue;
                // goto next defined column
            }

            /* check for matching character sets */
            String actualCS  = existingField.getCharacterSet();
            String definedCS = colDefined[i].getCharacterSet();
            if (StringTools.isBlank(definedCS)) { definedCS = "<default>"; }
            if (colDefined[i].isUTF8() && !existingField.isUTF8()) {
                // Defined column is UTF8, but actual column is not
                if (!DBField.IgnoreColumnError(utableName,columnName)) {
                    if (displayErrors) {
                        Print.logInfo("ERROR - " + utableName + ": Column '" + columnName + "' UTF8 mismatch '" + definedCS + "' != '" + actualCS + "'");
                    }
                    typeMismatchColumns.add(colDefined[i]);
                    columnsOK = false;
                } else {
                    if (displayErrors) {
                        Print.logInfo("WARNING - " + utableName + ": Column '" + columnName + "' UTF8 mismatch '" + definedCS + "' != '" + actualCS + "' (ignored)");
                    }
                }
            } else
            if (!colDefined[i].isUTF8() && existingField.isUTF8()) {
                // Actual column is UTF8, but Defined column is not
                if (checkColumnEncoding) {
                    if (alterColumnTypes) {
                        if (!DBField.IgnoreColumnError(utableName,columnName)) {
                            if (displayErrors) {
                                Print.logInfo("ERROR - " + utableName + ": Column '" + columnName + "' UTF8 mismatch '" + definedCS + "' != '" + actualCS + "'");
                            }
                            typeMismatchColumns.add(colDefined[i]);
                            columnsOK = false;
                        } else {
                            if (displayErrors) {
                                Print.logInfo("WARNING - " + utableName + ": Column '" + columnName + "' UTF8 mismatch '" + definedCS + "' != '" + actualCS + "' (ignored)");
                            }
                        }
                    } else {
                        if (displayErrors) {
                            Print.logInfo("WARNING - " + utableName + ": Column '" + columnName + "' UTF8 mismatch '" + definedCS + "' != '" + actualCS + "'");
                        }
                    }
                }
            }

            /* check for matching data types */
            String actualType  = existingField.getDataType();
            String definedType = colDefined[i].getDataType();
            if (!DBProvider.areTypesEquivalent(definedType,actualType)) {
                // Column type mismatch
                if (displayErrors) {
                    Print.logInfo("ERROR - " + utableName + ": Column '" + columnName + "' Type mismatch expected::" + definedType + " != found:" + actualType + "");
                }
                typeMismatchColumns.add(colDefined[i]);
                columnsOK = false;
            }

            /* check for matching keys */
            if (existingField.isPrimaryKey() != colDefined[i].isPrimaryKey()) {
                // primary keys do not match
                if (displayErrors) {
                    if (colDefined[i].isPrimaryKey()) {
                        Print.logInfo("ERROR - " + utableName + ": Column '" + columnName + "' missing Primary key");
                    } else {
                        Print.logInfo("ERROR - " + utableName + ": Column '" + columnName + "' extra Primary key");
                    }
                }
                priKeyMismatchColumns.add(colDefined[i]);
                columnsOK = false;
            } else
            if (existingField.hasMissingAlternateIndexes(colDefined[i].getAlternateIndexes())) {
                // existing field is missing some alternate keys
                if (displayErrors) {
                    String altKeys = StringTools.join(existingField.getMissingAlternateIndexes(colDefined[i].getAlternateIndexes()),",");
                    Print.logInfo("ERROR - " + utableName + ": Column '" + columnName + "' missing Alternate key ["+altKeys+"]");
                }
                altKeyMismatchColumns.add(colDefined[i]);
                columnsOK = false;
            } else
            if (existingField.hasExtraAlternateIndexes(colDefined[i].getAlternateIndexes())) {
                // existing field has some extra alternate keys
                if (displayWarnings) { // displayErrors/displayWarnings
                    String altKeys = StringTools.join(existingField.getAlternateIndexes(),",");
                    Print.logInfo("Warn - " + utableName + ": Column '" + columnName + "' extra Alternate key ["+altKeys+"]");
                }
                //altKeyMismatchColumns.add(colDefined[i]);
                //columnsOK = false;
            }

            /* table column index */
            int colTableNdx = colTableMap.indexOfKey(columnName);
            if (colTableNdx != i) {
                // Column is located at a different index
                if (displayWarnings) {
                    // This is more of an 'informational' message than a warning
                    //Print.logInfo("WARNING - " + utableName + ": Column '" + columnName + "' [" + i + "] found @ " + colTableNdx);
                }
            }
            
        }

        /* warn about 'existing' columns that aren't 'defined' */
        if (displayWarnings) {
            for (int i = 0; i < colTable.length; i++) {
                String columnName = colTable[i].getName();
                DBField definedField = this.getField(columnName);
                if (definedField == null) {
                    Print.logInfo("WARNING - " + utableName + ": Actual column '" + colTable[i] + "' not used");
                }
            }
        }

        /* add missing columns? */
        // This list includes columns that may have the same name, but different attributes
        // adding such a column will likely produce an error
        if (addMissingColumns && (missingColumns.size() > 0)) {
            try {
                DBField columns[] = missingColumns.toArray(new DBField[missingColumns.size()]);
                /* add column */
                int ndx = 0;
                for (;ndx < columns.length;) {
                    int cnt = this.addColumns(columns, ndx);
                    if (cnt == 0) {
                        // should not occur
                        break;
                    }
                    ndx += cnt;
                }
                /* rebuild indexes? */
                for (int c = 0; c < columns.length; c++) {
                    if (columns[c].isAlternateKey()) {
                        this.recreateAlternateIndexes();
                        break;
                    }
                }
            } catch (DBException dbe) {
                if (displayErrors) {
                    Print.logException("ERROR - " + utableName + ": Unable to add missing columns!", dbe);
                }
                return false;
            }
        }

        /* alter columns types? */
        if (alterColumnTypes && (typeMismatchColumns.size() > 0)) {
            try {
                DBField columns[] = typeMismatchColumns.toArray(new DBField[typeMismatchColumns.size()]);
                /* alter column type */
                int ndx = 0;
                for (;ndx < columns.length;) {
                    int cnt = this.addColumns(columns, ndx);
                    if (cnt == 0) {
                        // should not occur
                        break;
                    }
                    ndx += cnt;
                }
            } catch (DBException dbe) {
                if (displayErrors) {
                    Print.logException("ERROR - " + utableName + ": Unable to alter column type!", dbe);
                }
                return false;
            }
        }

        /* recreate primary key? */
        if (rebuildKeys && (priKeyMismatchColumns.size() > 0)) {
            try {
                this.recreatePrimaryKey();
            } catch (DBException dbe) {
                if (displayErrors) {
                    Print.logException("ERROR - " + utableName + ": Unable to rebuild primary key!", dbe);
                }
                return false;
            }
        }

        /* recreate alternate keys? */
        if (rebuildKeys && (altKeyMismatchColumns.size() > 0)) {
            try {
                this.recreateAlternateIndexes();
            } catch (DBException dbe) {
                if (displayErrors) {
                    Print.logException("ERROR - " + utableName + ": Unable to rebuild alternate keys!", dbe);
                }
                return false;
            }
        }

        /* return results */
        if (!columnsOK && displayWarnings) {
            for (int i = 0; i < colTable.length; i++) {
                Print.logInfo("WARNING - " + utableName + ": Found - " + colTable[i]);
            }
        }
        return columnsOK;

    }

    /**
    *** Return a String representation of the specified DBField (display purposes only)
    *** @param i  The column index
    *** @param f  The DBField
    *** @param isDefined True if this is a 'defined' column
    *** @return The String representation
    **/
    private String _columnInfo(int i, DBField f, boolean isDefined)
    {
        String ndx  = StringTools.format(i,"00");
        String name = StringTools.leftAlign(f.getName(),22);
        String desc = isDefined? (": " + f.getTitle(null)) : "";
        String type = f.getSqlType();
        if (f.isPrimaryKey()) { type += " key"; }
        if (f.isAlternateKey()) { type += " altkey"; }
        if (f.isAutoIncrement()) { type += " auto"; }
        if (f.isUTF8()) { type += " utf8"; }
        type = StringTools.leftAlign(type, 32);
        return ndx + ") " + name + " " + type + desc;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an array of all DBFields defined by this DBFactory
    *** @return An array of defined DBFields
    **/
    public DBField[] getFields()
    {
        // Note: this method is called frequently
        if (!this.fieldArrayReady) {
            // optimize access to field array
            synchronized (this.fieldMap) {
                if (!this.fieldArrayReady) { // test again inside lock
                    this.fieldArray = this.fieldMap.valueArray(DBField.class);
                    this.fieldArrayReady = true;
                }
            }
        } 
        return this.fieldArray;
    }

    /**
    *** Returns a set of DBFields defined by this DBFactory
    *** @param fieldNames  A set of field names representing fields to return (null for all)
    ***                    field names which are not defined in this DBFactory are quietly ignored.
    *** @return A set of DBFields
    **/
    public DBField[] getFields(Set<String> fieldNames)
    {
        if (ListTools.isEmpty(fieldNames)) {
            return this.getFields();
        } else {
            java.util.List<DBField> fldList = new Vector<DBField>();
            synchronized (this.fieldMap) {
                for (DBField dbf : this.fieldMap.values()) {
                    String n = dbf.getName(); // this.getMappedFieldName(name); <== should this be used here?
                    if (fieldNames.contains(n)) {
                        fldList.add(dbf);
                    }
                }
            } 
            return fldList.toArray(new DBField[fldList.size()]);
        }
    }

    /**
    *** Returns a set of DBFields defined by this DBFactory
    *** @param fieldNames  An array of field names representing fields to return (null for all).
    ***                    Field names which are not defined in this DBFactory are quietly ignored.
    *** @return A set of DBFields
    **/
    public DBField[] getFields(String... fieldNames)
    {
        if (ListTools.isEmpty(fieldNames)) {
            return this.getFields();
        } else {
            return this.getFields(ListTools.toSet(fieldNames,null));
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** This method can be used for field 'renaming'.  That is, converting an old
    *** obsolete name (ie. 'interval') to a new field name (ie. 'intervalMinutes').
    *** This feature is currently not in use and exists here for future use.
    *** @param cn   The old field name
    *** @return The new field name
    **/
    public String getMappedFieldName(String cn)
    { // map old name to a new name
        return DBProvider.translateColumnName(cn);
    }

    /**
    *** Returns true if the specified field exists in this DBFactory
    *** @param name  A field name to test
    *** @return True if the specified field exists, false otherwise
    **/
    public boolean hasField(String name)
    {
        String n = this.getMappedFieldName(name);
        return (n != null)? this.fieldMap.containsKey(n) : false;
    }

    /**
    *** Gets the DBField for the specified name
    *** @param name  The name of the field to retrieve
    *** @return The DBField for the specified name, or null if the field name was not found
    **/
    public DBField getField(String name) 
    {
        String n = this.getMappedFieldName(name);
        return (n != null)? this.fieldMap.get(n) : null;
    }

    /**
    *** Gets the String length for the specified DBField
    *** @param name  The name of the field for which the String length is retrieved.
    *** @return The String length of the specified DBField
    **/
    public int getFieldStringLength(String name) 
    {
        DBField fld = this.getField(name);
        return (fld != null)? fld.getStringLength() : 0;
    }

    /**
    *** Returns the number of defined fields in this DBFactory
    *** @return The number of defined fields in this DBFactory
    **/
    public int getFieldCount()
    {
        return this.fieldMap.size();
    }

    /**
    *** Returns a String array of all field names defined in this DBFactory
    *** @return The String array of field names defined in this DBFactory
    **/
    public String[] getFieldNames()
    {
        return this.fieldMap.keyArray(String.class);
    }
    
    /**
    *** Returns an array of names for the specified fields
    *** @param flds  An array of fields
    *** @return An array of the specified field names
    **/
    public static String[] getFieldNames(DBField flds[])
    {
        if (ListTools.isEmpty(flds)) {
            return new String[0];
        } else {
            String fldNames[] = new String[flds.length];
            for (int i = 0; i < flds.length; i++) {
                fldNames[i] = (flds[i] != null)? flds[i].getName() : "";
            }
            return fldNames;
        }
    }

    /**
    *** Returns all fields which have the specified property set to the specified boolean value.
    *** This method searches the field property/attribute string for matching key/values.
    *** @param key  The property key searched in the property/attribute string
    *** @param value The required value of the specified property used for inclusion in the return field list
    *** @return The list of fields matching the property/value criteria.
    **/
    public DBField[] getFieldsWithBoolean(String key, boolean value)
    {
        java.util.List<DBField> af = new Vector<DBField>();
        for (Iterator<DBField> i = this.fieldMap.valueIterator(); i.hasNext();) {
            DBField fld = i.next();
            if (fld.getBooleanAttribute(key,false) == value) {
                af.add(fld);
            }
        }
        return af.toArray(new DBField[af.size()]);
    }

    /**
    *** Return an array of DBFields matching the names of the specified fields.  If a field name is
    *** not found, it will be omitted from the returned DBField array.
    *** @param fieldNames  The field names for which the array of DBFields will be returned
    *** @return The array of DBFields
    **/
    public DBField[] getNamedFields(String fieldNames[])
    {
        java.util.List<DBField> fields = new Vector<DBField>();
        for (int i = 0; i < fieldNames.length; i++) {
            DBField fld = this.getField(fieldNames[i]);
            if (fld != null) {
                fields.add(fld);
            } else {
                Print.logStackTrace("Invalid field for table: " + fieldNames[i]);
            }
        }
        return fields.toArray(new DBField[fields.size()]);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Set the field default value.
    *** @param fldName  The field name
    *** @param dftVal   The default value (no checking is done on the datatype)
    *** @return True if the specified field name exists, false otherwise
    **/
    public boolean setFieldDefaultValue(String fldName, Object dftVal)
    {
        DBField fld = this.getField(fldName);
        if (fld != null) {
            fld.setDefaultValue(dftVal);
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the state for logging warnings for missing table columns
    *** @param state  True to warn (default), false otherwise
    **/
    public void setLogMissingColumnWarnings(boolean state)
    {
        this.logMissingColumns = state;
    }

    /**
    *** Returns true to log warnings regarding missing columns on insert/update
    *** @return True to log warnings, false otherwise
    **/
    public boolean logMissingColumnWarning() 
    {
        return this.logMissingColumns;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "autoIndex" key field (if any)
    *** @return The "autoIndex" key field, or null if not found
    **/
    public DBField getAutoIndexField()
    {
        return this.getField(DBRecordKey.FLD_autoIndex);
    }

    /**
    *** Gets a list of primary key fields
    *** @return An array of primary key fields
    **/
    public DBField[] getKeyFields()
    {
        return this.priKeys; // should never be null
    }

    /**
    *** Gets a list of field names representing the primary keys fields
    *** @return A String arry of primary key field names
    **/
    public String[] getKeyNames() 
    {
        DBField f[] = this.getKeyFields();
        String kn[] = new String[f.length];
        for (int i = 0; i < f.length; i++) {
            kn[i] = f[i].getName();
        }
        return kn;
    }

    /**
    *** Gets the key type
    *** @return The key type
    **/
    public String getKeyType() 
    {
        return DBFactory.getKeyTypeName(this.keyType);
    }
    
    /**
    *** Returns the key type String name for the specified key type index
    *** @param type The key type index
    *** @return The key type name
    **/
    public static String getKeyTypeName(KeyType type)
    {
        // TODO: should be moved to DBProvider ...
        // MySQL     : key identifier
        // SQLServer : key identifier
        // Derby     : key identifier
        // PostgreSQL: key identifier
        switch (type) {
            // MySQL: key types ...
            case PRIMARY        : return "PRIMARY KEY";     // table
            case UNIQUE         : return "UNIQUE";          // table
            case INDEX          : return "INDEX";           // table/index
            case UNIQUE_INDEX   : return "UNIQUE INDEX";    // table/index
            default             : return "UNKNOWN";
        }
    }

    /**
    *** Gets the key class
    *** @return The key class
    **/
    public Class<? extends DBRecordKey<gDBR>> getKeyClass()
    {
        return this.keyClass;
    }

    /**
    *** Creates/returns an empty DBRecordKey for this DBFactory
    *** @return A DBRecordKey instance 
    *** @throws DBException   If a database error occurs
    **/
    public DBRecordKey<gDBR> createKey() // <? extends DBRecord<?>>
        throws DBException
    {
        if (this.keyClass != null) {
            try {
                // this creates an empty key with no key fields
                Constructor<? extends DBRecordKey<gDBR>> kc = this.keyClass.getConstructor(new Class<?>[0]);
                return kc.newInstance(new Object[0]); // "unchecked cast"
            } catch (Throwable t) { // NoSuchMethodException, ...
                // Implementation error (should never occur)
                throw new DBException("Key Creation", t);
            }
        }
        return null;
    }

    /**
    *** Creates/returns a DBRecordKey for this DBFactory populated with key values from the
    *** specified ResultSet
    *** @param rs The SQL query ResultSet
    *** @return The DBRecordKey
    *** @throws DBException   If a database error occurs
    **/
    public DBRecordKey<gDBR> createKey(ResultSet rs) // <? extends DBRecord<?>>
        throws DBException 
    {
        DBRecordKey<gDBR> key = this.createKey(); // may throw DBException
        if (rs != null) {
            DBField pk[] = this.getKeyFields();
            try {
                for (int i = 0; i < pk.length; i++) {
                    String name = pk[i].getName();
                    Object val  = pk[i].getResultSetValue(rs);
                    key.setKeyValue(name, val); // setFieldValue
                }
            } catch (SQLException sqe) {
                /*
                Print.logInfo("ResultSet Column Information:");
                try {
                    ResultSetMetaData meta = rs.getMetaData();
                    int numCols = meta.getColumnCount();
                    for (int c = 1; c <= numCols; c++) {
                        String colName = meta.getColumnName(c);
                        Print.logInfo("  Column "+c+": " + colName);
                    }
                } catch (SQLException sq2) {
                    //
                }
                */
                throw new DBException("Creating Key", sqe);
            }
        }
        return key;
    }

    /**
    *** Creates/returns a DBRecordKey for this DBFactory populated with key values from the
    *** specified Value-Map
    *** @param valMap  The Field==>Value map
    *** @return The DBRecordKey
    *** @throws DBException   If a database error occurs
    **/
    public DBRecordKey<gDBR> createKey(Map<String,String> valMap) // , boolean partialOK)
        throws DBException 
    {
        return this.createKey(valMap, DBWhere.KEY_FULL);
    }
    
    /**
    *** Creates/returns a DBRecordKey for this DBFactory populated with key values from the
    *** specified Value-Map
    *** @param valMap  The Field==>Value map
    *** @param partialKeyType  DBWhere [KEY_FULL|KEY_PARTIAL_FIRST|KEY_PARTIAL_ALL]
    *** @return The DBRecordKey [CHECK]
    *** @throws DBException   If no key field specified or field no found
    **/
    public DBRecordKey<gDBR> createKey(Map<String,String> valMap, int partialKeyType)
        throws DBException 
    {
        String utableName = this.getUntranslatedTableName();

        /* no keys specified */
        if (ListTools.isEmpty(valMap) && (partialKeyType != DBWhere.KEY_PARTIAL_ALL_EMPTY)) {
            throw new DBException("Creating Key: No key fields - " + utableName);
        }

        /* key fields */
        DBField pk[];
        if (partialKeyType == DBWhere.KEY_AUTO_INDEX) {
            DBField autoKey = this.getAutoIndexField();
            if (autoKey == null) {
                throw new DBException("Creating Key: auto-index key not found - " + utableName);
            }
            pk = new DBField[] { autoKey };
        } else {
            pk = this.getKeyFields();
        }

        /* create key */
        DBRecordKey<gDBR> key = this.createKey(); // may throw DBException
        for (int i = 0; i < pk.length; i++) {
            String name = pk[i].getName();
            String sval = (valMap != null)? valMap.get(name) : null; // may be defined, but null
            if (sval == null) { // either not defined, or no value assigned
                if (partialKeyType == DBWhere.KEY_AUTO_INDEX) {
                    // -- "autoIndex" key required
                    throw new DBException("Creating Key: auto-index required, but auto-index not found/specified - " + utableName + "." + name);
                } else
                if (partialKeyType == DBWhere.KEY_FULL) {
                    // -- all keys required
                    throw new DBException("Creating Key: full key required, but field not specified - " + utableName + "." + name);
                } else
                if (partialKeyType == DBWhere.KEY_PARTIAL_ALL_EMPTY) {
                    // -- missing first key is allowed
                    continue;
                } else
                if (i == 0) {
                    // -- first key is required
                    throw new DBException("Creating Key: first key required, but first field not found/specified - " + utableName + "." + name);
                } else
                if (partialKeyType == DBWhere.KEY_PARTIAL_FIRST) {
                    // -- we at-least have the first key, skip remaining after first missing key
                    break;
                } else { // DBWhere.KEY_PARTIAL_ALL
                    // -- we at-least have the first key, continue seaarching for keys
                    continue;
                }
            } else {
                Object val = pk[i].parseStringValue(sval);
                key.setKeyValue(name, val); // setFieldValue
            }
        } // primary key loop
        return key;

    }

    // ------------------------------------------------------------------------

    /**
    *** Creates/returns a DBRecord for this DBFactory populated with key values from the
    *** specified ResultSet (record not yet saved)
    *** @param rs The SQL query ResultSet
    *** @return The DBRecord
    *** @throws DBException   If a database error occurs
    **/
    public gDBR createRecord(ResultSet rs)
        throws DBException 
    {
        DBRecordKey<? extends DBRecord<?>> rcdKey = this.createKey(rs); // may throw DBException
        if (rcdKey != null) {
            @SuppressWarnings("unchecked")
            gDBR rcd = (gDBR)rcdKey.getDBRecord();
            rcd.setAllFieldValues(rs);
            return rcd; // not yet saved
        } else {
            Print.logError("Unable to create record: " + this.getUntranslatedTableName());
            return null;
        }
    }

    /**
    *** Creates/returns a DBRecord for this DBFactory populated with key values from the
    *** specified Value-Map (record not yet saved)
    *** @param valMap  The Field==>Value map
    *** @return The DBRecord
    *** @throws DBException   If a database error occurs
    **/
    //@SuppressWarnings("unchecked")
    public gDBR createRecord(Map<String,String> valMap)
        throws DBException 
    {
        DBRecordKey<? extends DBRecord<?>> rcdKey = this.createKey(valMap); // may throw DBException
        if (rcdKey != null) {
            @SuppressWarnings("unchecked")
            gDBR rcd = (gDBR)rcdKey.getDBRecord();
            // (Java 5) I don't know why this produced an "unchecked" warning (the "setFieldValues" above does not)
            rcd.setAllFieldValues(valMap);
            return rcd; // not yet saved
        } else {
            Print.logError("Unable to create key: " + this.getUntranslatedTableName());
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates/returns a DBRecord for this DBFactory populated with key values from the
    *** specified XML Element
    *** @param record  The XML Element
    *** @return The DBRecord
    *** @throws DBException   If field does not specify a 'name' or a general 
    ***         DB exception occurs
    **/
    public gDBR createRecord(Element record)
        throws DBException 
    {

        /* load field values from XML Element */
        Map<String,String> valMap = new HashMap<String,String>();
        NodeList fieldNodes = XMLTools.getChildElements(record,TAG_Field);
        for (int f = 0; f < fieldNodes.getLength(); f++) {
            Element field = (Element)fieldNodes.item(f);
            String name   = XMLTools.getAttribute(field, ATTR_name, null, false);
            if (!StringTools.isBlank(name)) {
                String val = XMLTools.getNodeText(field, "\\n", false, "");
                valMap.put(name, val);
            } else {
                throw new DBException("Field does not specify a 'name'");
            }
        }

        /* return DBRecord */
        return this.createRecord(valMap);

    }

    // ------------------------------------------------------------------------
    
    /**
    *** Returns the number of alternate indexes defined for this table
    *** @return The number of alternate indexes defined for this table
    **/
    public int getAlternateIndexCount()
    {
        return (this.altIndexMap != null)? this.altIndexMap.size() : 0;
    }
    
    /**
    *** Returns true if this DBFactory supports any alternate indexes
    *** @return True if this DBFactory supports any alternate indexes
    **/
    public boolean hasAlternateIndexes()
    {
        return (this.getAlternateIndexCount() > 0);
    }

    /**
    *** Returns the DBAlternateIndex definition (if any) for this DBFactory
    *** @return The DBAlternateIndex definition, or null if this table has no alternate indexes
    **/
    public DBAlternateIndex[] getAlternateIndexes() 
    {
        if (!this.hasAlternateIndexes()) {
            return null;
        } else {
            return this.altIndexMap.valueArray(DBAlternateIndex.class);
        }
    }

    /**
    *** Returns an array of alternate index names (if any) for this DBFactory
    *** @return An array of alternate index names (if any) for this DBFactory
    **/
    public String[] getAlternateIndexNames() 
    {
        if (!this.hasAlternateIndexes()) {
            return null;
        } else {
            return this.altIndexMap.keyArray(String.class);
        }
    }

    /**
    *** Returns the DBAlternateIndex definition for the specified alternate index name
    *** @return The DBAlternateIndex definition, or null if the named index is not found
    **/
    public DBAlternateIndex getAlternateIndex(String name) 
    {
        if (this.altIndexMap == null) {
            return null;
        } else {
            String n = !StringTools.isBlank(name)? name : DBProvider.DEFAULT_ALT_INDEX_NAME;
            return this.altIndexMap.get(n); // may return null
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the DBRecord class for this DBFactory
    *** @return The DBRecord class
    **/
    public Class<gDBR> getRecordClass() 
    {
        return this.rcdClass;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the untranslated table for for this DBFactory
    *** @return The table name
    **/
    public String getUntranslatedTableName()
    {
        return this.utableName;
    }

    /**
    *** Gets the translated table name for this DBSelect
    *** @return The defined table name
    **/
    public String getTranslatedTableName()
    {
        return DBProvider.translateTableName(this.getUntranslatedTableName());
    }

    // ------------------------------------------------------------------------

    /**
    *** Return true if the table represented by this DBFactory exists
    *** @return True if the table exists
    *** @throws DBException   If a database error occurs
    **/
    public boolean tableExists()
        throws DBException
    {
        DBConnection dbc    = null;
        Statement    stmt   = null;
        ResultSet    rs     = null;
        boolean      innodb = this.isMySQLInnoDB();
        try {
            int provID = DBProvider.getProvider().getID();

            /* connection */
            dbc = DBConnection.getDBConnection_read();

            /* check for SQLServer */
            if (provID == DBProvider.DB_SQLSERVER) {
                // -  SQLServer "COUNT(*)"
                // -  expect SQLException if does not exist
                boolean CUSTOM_SQLSERVER_EXISTS = false;
                String existsSel;
                if (CUSTOM_SQLSERVER_EXISTS) {
                    // -- A full "COUNT(*)" runs slow on SQL Server.
                    // -- "SELECT COUNT(*) FROM (SELECT TOP 1 * FROM MSeventdata ) t"
                    StringBuffer sb = new StringBuffer();
                    sb.append("SELECT COUNT(*) FROM (SELECT TOP 1 * FROM ");
                    sb.append(this.getTranslatedTableName());
                    sb.append(" ) t;"); // <-- is trailing ";" required??
                    existsSel = sb.toString();
                } else {
                    // -- "SELECT COUNT(*) FROM table" 
                    DBSelect<gDBR> dsel = new DBSelect<gDBR>(this); 
                    dsel.setSelectedFields(DBProvider.FLD_COUNT()); // tableExists: non MySQL/InnoDB
                    existsSel = dsel.toString();
                }
                stmt = dbc.execute(existsSel); // may throw DBException, SQLException
                return true; // assume exists if no SQLException
            }

            /* check for non-MySQL */
            if (provID != DBProvider.DB_MYSQL) {
                // -- non-MySQL "COUNT(*)" 
                // -  "SELECT COUNT(*) FROM table"
                // -  expect SQLException if does not exist
                DBSelect<gDBR> dsel = new DBSelect<gDBR>(this); 
                dsel.setSelectedFields(DBProvider.FLD_COUNT()); // tableExists: non MySQL/InnoDB
                stmt = dbc.execute(dsel.toString()); // may throw DBException, SQLException
                return true; // assume exists if no SQLException
            }

            // ---------------------------------------------
            // MySQL below
            // SELECT COUNT(*) FROM (SELECT * FROM EventData LIMIT 1) EventData;

            /* InnoDB? */
            if (innodb) {
                // do not use "COUNT(*)" for table existance on InnoDB
                // "SHOW COLUMNS FROM table"
                //Print.logInfo("Using 'show columns' method for table existence ... " + this.getTranslatedTableName());
                String xtableName = this.getTranslatedTableName();
                String sqlExists  = "SHOW COLUMNS FROM " + this.getTranslatedTableName();
                stmt = dbc.execute(sqlExists); // may throw DBException, SQLException
                //rs = stmt.getResultSet();
                return true; // assume exists if no SQLException
            }

            /* get table existence */
            if (DBFactory.mysqlTableExistsUseSelectCount()) {
                /* VERY slow on Windows or InnoDB */ 
                // "SELECT COUNT(*) FROM table"
                DBSelect<gDBR> dsel = new DBSelect<gDBR>(this); 
                dsel.setSelectedFields(DBProvider.FLD_COUNT());  // tableExists: non MySQL/InnoDB
                stmt = dbc.execute(dsel.toString()); // may throw DBException, SQLException
                //rs = stmt.getResultSet();
                return true; // assume exists if no SQLException
            }

            /* alternate method for MySQL */ 
            // "SHOW COLUMNS FROM table"
            //Print.logInfo("Using 'show columns' method for table existence ... " + this.getTranslatedTableName());
            String xtableName = this.getTranslatedTableName();
            String sqlExists  = "SHOW COLUMNS FROM " + this.getTranslatedTableName();
            stmt = dbc.execute(sqlExists); // may throw DBException, SQLException
            //rs = stmt.getResultSet();
            return true; // assume exists if no SQLException

        } catch (SQLException sqe) {

            String sqlMsg = sqe.getMessage();
            int errCode = sqe.getErrorCode();
            if (errCode == SQLERR_TABLE_NONEXIST) { // MySQL: ?
                return false;
            } else
            if (errCode == SQLERR_UNKNOWN_DATABASE) { // MySQL: ?
                String dbName = DBProvider.getDBName();
                Print.logError("Database does not exist '" + dbName + "'"); // thus, table does not exist
                return false;
            } else
            if (errCode == MSQL_ERR_INVALID_OBJECT) { // SQLServer: :
                return false;
            } else
            if (sqlMsg.indexOf("does not exist") >= 0) { // PostgreSQL: ?
                return false;
            } else {
                String dbName = DBProvider.getDBName();
                throw new DBException("Table Existance '" + dbName + "'", sqe);
            }

        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Return true if the table represented by this DBFactory is required
    *** @return True if the table is required
    **/
    public boolean tableRequired()
    {
        return true;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Retrieve the current table status.
    *** @return The current number of rows in the table
    *** @throws DBException if an error occured fetching table status
    **/
    public long getMySQLTableStatus()
        throws DBException
    {
        // - http://stackoverflow.com/questions/8624408/why-is-innodbs-show-table-status-so-unreliable

        /* column names */
        String colName_Name             = "Name";            // Table
        String colName_Engine           = "Engine";          // MyISAM | InnoDB
        String colName_Version          = "Version";         // 10
        String colName_Row_format       = "Row_format";      // Dynamic
        String colName_Rows             = "Rows";            // 13   (only approximate for InnoDB)
        String colName_Avg_row_length   = "Avg_row_length";  // 135
        String colName_Data_length      = "Data_length";     // 1764
        String colName_Max_data_length  = "Max_data_length"; // 281474976710655
        String colName_Index_length     = "Index_length";    // 4096
        String colName_Data_free        = "Data_free";       // 0
        String colName_Auto_increment   = "Auto_increment";  // NULL
        String colName_Create_time      = "Create_time";     // 2012-07-26 23:00:34
        String colName_Update_time      = "Update_time";     // 2013-05-05 01:16:51
        String colName_Check_time       = "Check_time";      // NULL
        String colName_Collation        = "Collation";       // latin1_swedish_ci
        String colName_Checksum         = "Checksum";        // NULL
        String colName_Create_options   = "Create_options";  // 
        String colName_Comment          = "Comment";         // 

        /* this is currently only supported by MySQL */
        String utableName = this.getUntranslatedTableName();
        String xtableName = DBProvider.translateTableName(utableName);
        DBProvider dbp    = DBProvider.getProvider();
        String showStatus = null;
        String dftNdxType = "?"; // non-null
        if (dbp.getID() == DBProvider.DB_MYSQL) {
            // MySQL: "SHOW TABLE STATUS WHERE Name='table';"
            // ie: SHOW TABLE STATUS WHERE Name="EventData";
            showStatus = "SHOW TABLE STATUS WHERE Name=\"" + xtableName + "\"";
            dftNdxType = DBProvider.isMySqlInnoDB()? "InnoDB" : "MyISAM";
        } else {
            this._setIndexType(""); // not MySQL (2.5.2-B56)
            return -1L;
        }

        /* get table status */
        long         rows = -1L;
        DBConnection dbc  = null;
        Statement    stmt = null;
        ResultSet    rs   = null;
        try {
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(showStatus);
            rs   = stmt.getResultSet();
            if (rs.next()) {
                // table found, read first entry
                this._setIndexType(rs.getString(colName_Engine));
                rows = rs.getLong(colName_Rows); 
            } else {
                // table not found (fix NPE 2.5.2-B56)
                //throw new DBException("Table not found: " + xtableName); // catch below
                this._setIndexType(dftNdxType);
                rows = -1L;
            }
        } catch (SQLException sqe) {
            Print.logError("SQLException: " + sqe);
            this._setIndexType(dftNdxType);
            throw new DBException("Unable to get table status", sqe);
        } catch (DBException dbe) {
            Print.logError("DBException: " + dbe);
            this._setIndexType(dftNdxType);
            throw dbe; // re-throw
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return row count ("-1" if not found) */
        return rows;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** ValidationLog class
    **/
    public static class ValidationLog
    {
        private String       utableName = "";
        private boolean      inclWarn = true;
        private StringBuffer out = null;
        private String       header = "";
        private int          hasErrors = 0;
        public ValidationLog(String utableName, boolean inclWarn) {
            this.utableName = (utableName != null)? utableName : ""; // untranslated
            this.inclWarn   = inclWarn;
            this.out        = new StringBuffer();
            this.header     = "";
            this.hasErrors  = 0;
        }
        public void clear() {
            this.out.setLength(0);
            this.header = "";
            this.hasErrors = 0;
        }
        public void logHeader(String header) {
            this.header = (header != null)? header : "";
        }
        public boolean hasHeader() {
            return !StringTools.isBlank(this.header);
        }
        public void logInfo(String msg) {
            this.out.append("\n  [INFO] " + msg);
        }
        public void logWarn(String msg) {
            if (this.inclWarn) {
                this.out.append("\n  [WARN] " + msg);
                this.hasErrors++;
            }
        }
        public void logSevere(String msg) {
            this.out.append("\n  [SEVERE] " + msg);
            this.hasErrors++;
        }
        public boolean hasErrors() {
            return (this.hasErrors > 0);
        }
        public String toString() {
            return this.header + this.out.toString();
        }
    }

    /**
    *** ValidationNotImplementedException class
    **/
    public static class ValidationNotImplementedException
        extends Exception
    {
        public ValidationNotImplementedException(String msg) {
            super(msg);
        }
    }

    /**
    *** Validates the table defined by this DBFactory.
    *** @param inclWarn If true, warnings will also be displayed
    *** @return True if this validation passed, false otherwise
    **/
    public boolean validateTable(boolean inclWarn)
    {
        // This method is intended to be executed from the command line
        String utableName = this.getUntranslatedTableName();
        Print.logInfo("");
        Print.logInfo("Validating " + utableName + ":");

        boolean pass = true;
        try {

            /* validation constructor */
            MethodAction valConst = null;
            try {
                valConst = new MethodAction(this.getRecordClass(), ResultSet.class, ValidationLog.class);
            } catch (Throwable t) { // NoSuchMethodException, ...
                throw new ValidationNotImplementedException("Missing validation Constructor");
            }
            
            /* 'select' */
            // DBSelect: SELECT * FROM table
            DBConnection dbc  = null;
            Statement    stmt = null;
            ResultSet    rs   = null;
            try {
                DBSelect<gDBR> dsel = new DBSelect<gDBR>(this);
                dbc  = DBConnection.getDBConnection_read();
                stmt = dbc.execute(dsel.toString(), true); // needs to be row-by-row
                rs   = stmt.getResultSet();
                while (rs.next()) {
                    ValidationLog failLog = new ValidationLog(utableName, inclWarn);
                    try {
                        valConst.invoke(rs, failLog);
                        if (failLog.hasErrors()) {
                            Print.logError(failLog.toString());
                            pass = false; 
                        } else
                        if (inclWarn && !failLog.hasHeader()) {
                            throw new ValidationNotImplementedException("No log header");
                        }
                    } catch (Throwable t) { // InvocationTargetException, ValidationNotImplementedException, ...
                        Print.logException("Validating " + utableName + ": ", t);
                        pass = false;
                    }
                }
            } finally {
                if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
                if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
                DBConnection.release(dbc);
            }
            
            /* passed? */
            if (pass) {
                String e = inclWarn? "errors/warnings" : "severe errors";
                Print.logInfo("  No " + e + " detected");
            }
            return pass;
            
        } catch (ValidationNotImplementedException vnie) {
            Print.logError("  Validation not implemented: " + vnie.getMessage());
        } catch (DBException dbe) {
            Print.logException("Validating " + utableName + ": ", dbe);
        } catch (SQLException sqe) {
            Print.logException("Validating " + utableName + ": ", sqe);
        }
        
        return false;
            
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Adds the specified column to this table, or alters the column if it already exists.
    *** @param cols     An array of columns to add/alter
    *** @param ndx      The index of the first column to start adding/altering
    *** @return The number of columns added/altered
    *** @throws DBException   If a database error occurs
    **/
    public int addColumns(DBField cols[], int ndx)
        throws DBException
    {
        try {
            return this._addColumns(cols, ndx);
        } catch (SQLException sqe) {
            throw new DBException("Adding/Altering column", sqe);
        }
    }
    
    /**
    *** Adds the specified column to this table, or alters the column if it already exists.
    *** @param cols     An array of columns to add/alter
    *** @param ndx      The index of the first column to start adding/altering
    *** @return The number of columns added/altered
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    protected int _addColumns(DBField cols[], int ndx)
        throws SQLException, DBException
    {
        // Drop a column:
        //   MySQL: ALTER TABLE <TableName> DROP COLUMN <ColumnName>

        /* existing column map */
        Map<String,DBField> colMap = this.getExistingColumnMap(false);
        if (colMap == null) {
            return 0;
        }

        /* column index */
        if ((cols == null) || (ndx < 0) || (ndx >= cols.length)) {
            return 0;
        }
        
        /* count columns */
        int colCount = 0;

        /* create "ALTER" command sentence */
        StringBuffer sb   = new StringBuffer();
        DBProvider   dbp  = DBProvider.getProvider();
        String xtableName = this.getTranslatedTableName();
        if (dbp.getID() == DBProvider.DB_MYSQL) {
            // MySQL: ALTER TABLE <table> ...
            // Character Set Notes:
            //  - Show all available character sets:
            //      mysql> show character set;
            //  - Show all available collations:
            //      mysql> show collation;
            //  - Show current database character set defaults (as configured in 'my.cnf'):
            //      mysql> show variables like "character_set_%";       // "character_set_database"
            //      mysql> show variables like "collation_%";           // "collation_database"
            //  - Default character-set/collation in 'my.cnf':
            //      default-character-set=utf8 
            //      default-collation=utf8_general_ci 
            //  - References:
            //      http://dev.mysql.com/doc/refman/5.0/en/charset-configuration.html
            //      http://www.phpwact.org/php/i18n/utf-8/mysql
            sb.append("ALTER TABLE ").append(xtableName); // DB_MYSQL
            for (;ndx < cols.length; ndx++){
                if (colCount > 0) { sb.append(","); }
                DBField col = cols[ndx];
                String colName = col.getName();
                if (colMap.containsKey(colName)) {
                    // Change an existing column name:
                    //   MySQL: ALTER TABLE <TableName> CHANGE <column> <column> <type> [CHARACTER SET utf8 [COLLATE utf8_general_ci]]
                    sb.append(" CHANGE ").append(colName).append(" ").append(col.getFieldDefinition());
                    //TODO: [MAMUN] need to handle the column name - mamun.
                    if (col.isAutoIncrement()) {
                        //sb.append(" NOT NULL");
                        sb.append(" auto_increment");
                    }
                    if (col.isUTF8()) {
                        sb.append(" CHARACTER SET utf8");
                    }
                    Print.logInfo("Changing column: " + xtableName + "." + col.getFieldDefinition());
                } else {
                    // Add a new column:
                    //   MySQL: ALTER TABLE <TableName> ADD COLUMN <column> <type> [CHARACTER SET utf8 [COLLATE utf8_general_ci]]
                    sb.append(" ADD COLUMN ").append(col.getFieldDefinition());
                    if (col.isAutoIncrement()) {
                        // This will likely fail because 'auto_increment' is only valid on primary keys                        //sb.append(" NOT NULL");
                        //sb.append(" NOT NULL");
                        sb.append(" auto_increment");
                        //sb.append(", ADD PRIMARY KEY ("+colName+")");
                    } else
                    if (col.isUTF8()) {
                        sb.append(" CHARACTER SET utf8");
                    }
                    Print.logInfo("Adding column: " + xtableName + "." + col.getFieldDefinition());
                }
                colCount++;
            }
        } else
        if (dbp.getID() == DBProvider.DB_SQLSERVER) {
            // SQLServer: ALTER TABLE '<table>' ALTER COLUMN ...
            sb.append("ALTER TABLE ").append(xtableName); // DB_SQLSERVER
            DBField col = cols[ndx];
            String colName = col.getName();
            if (colMap.containsKey(colName)) {
                // Change an existing column name:
                //   SQLServer: ALTER TABLE <table> ALTER COLUMN <column> <type>
                sb.append(" ALTER COLUMN ").append(col.getFieldDefinition());
                Print.logInfo("Changing column: " + xtableName + "." + col.getFieldDefinition());
            } else {
                // Add a new column:
                //   SQLServer: ALTER TABLE <table> ADD <column> <type>
                sb.append(" ADD ").append(col.getFieldDefinition());
                Print.logInfo("Adding column: " + xtableName + "." + col.getFieldDefinition());
            }
            colCount++;
        } else
        if (dbp.getID() == DBProvider.DB_DERBY) {
            // Derby: ALTER TABLE <table> ...
            sb.append("ALTER TABLE ").append(xtableName); // DB_DERBY
            DBField col = cols[ndx];
            String colName = col.getName();
            if (colMap.containsKey(colName)) {
                // Change an existing column name:
                //   Derby: ALTER TABLE <table> ALTER <column> SET DATA TYPE <type>
                sb.append(" ALTER ").append(colName).append(" SET DATA TYPE ").append(col.getFieldDefinition());
            } else {
                // Add a new column:
                //   Derby: ALTER TABLE <table> ADD COLUMN <column> <type>
                sb.append(" ADD COLUMN ").append(col.getFieldDefinition());
                Print.logInfo("Adding column: " + xtableName + "." + col.getFieldDefinition());
            }
            colCount++;
        }

        /* execute */
        if (sb.length() > 0) {
            Print.logInfo("Executing SQL 'ALTER TABLE ...'"); // + sb);
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDBConnection_write();
                dbc.executeUpdate(sb.toString());
            } finally {
                DBConnection.release(dbc);
            }
        }
        
        return colCount;

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Drips the specified column from this table
    *** @param col  The column specification to drop
    *** @throws DBException   If a database error occurs
    **/
    public void dropColumn(DBField col)
        throws DBException
    {
        if (col != null) {
            try {
                this._dropColumn(col);
            } catch (SQLException sqe) {
                throw new DBException("Dropping column", sqe);
            }
        }
    }
    
    /**
    *** Drips the specified column from this table
    *** @param col  The column specification to drop
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    protected void _dropColumn(DBField col)
        throws SQLException, DBException
    {
        String colName = col.getName();
        Map<String,DBField> colMap = this.getExistingColumnMap(false);
        if ((colMap != null) && colMap.containsKey(colName)) {
            String xtableName = this.getTranslatedTableName();
            String fieldDef   = col.getFieldDefinition();
            StringBuffer sb   = new StringBuffer();
            DBProvider dbp    = DBProvider.getProvider();
            if (dbp.getID() == DBProvider.DB_MYSQL) {
                //   MySQL: ALTER TABLE <TableName> DROP COLUMN <ColumnName>
                sb.append("ALTER TABLE ").append(xtableName); // DB_MYSQL
                sb.append(" DROP COLUMN ");
                //sb.append(DBProvider.getProvider().getStartColumnChar());
                //sb.append(colName);
                //sb.append(DBProvider.getProvider().getEndColumnChar());
                sb.append(DBProvider.getProvider().quoteColumnName(colName));
                Print.logInfo("Dropping column: " + xtableName + "." + fieldDef);
            } else
            if (dbp.getID() == DBProvider.DB_SQLSERVER) {
                sb.append("ALTER TABLE ").append(xtableName); // DB_SQLSERVER
                sb.append(" DROP COLUMN ");
                //sb.append(DBProvider.getProvider().getStartColumnChar());
                //sb.append(colName);
                //sb.append(DBProvider.getProvider().getEndColumnChar());
                sb.append(DBProvider.getProvider().quoteColumnName(colName));
                Print.logInfo("Dropping column: " + xtableName + "." + fieldDef);
            } else
            if (dbp.getID() == DBProvider.DB_DERBY) {
                sb.append("ALTER TABLE ").append(xtableName); // DB_DERBY
                sb.append(" DROP COLUMN ");
                //sb.append(DBProvider.getProvider().getStartColumnChar());
                //sb.append(colName);
                //sb.append(DBProvider.getProvider().getEndColumnChar());
                sb.append(DBProvider.getProvider().quoteColumnName(colName));
                Print.logInfo("Dropping column: " + xtableName + "." + fieldDef);
            }
            if (sb.length() > 0) {
                Print.logInfo("SQL Drop Column: " + sb);
                DBConnection dbc = null;
                try {
                    dbc = DBConnection.getDBConnection_write();
                    dbc.executeUpdate(sb.toString());
                } finally {
                    DBConnection.release(dbc);
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Recreates the Primary Key for the table represented by this DBFactory
    *** @throws DBException   If a database error occurs
    **/
    public void recreatePrimaryKey()
        throws DBException
    {
        
        /* drop existing primary key */
        try {
            DBProvider.removePrimaryIndex(this.getUntranslatedTableName());
        } catch (Throwable th) {
            Print.logWarn("Primary key does not currently exist");
            // ignore exception
        }
        
        /* recreate primary key */
        try {
            DBProvider.createPrimaryIndex(this);
        } catch (SQLException sqe) {
            throw new DBException("Alter primary key", sqe);
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Recreates the alternate key index for the table represented by this DBFactory
    **/
    public void recreateAlternateIndexes()
        throws DBException
    {
        String utableName = this.getUntranslatedTableName();

        /* drop all alternate indexes */
        try {
            DBTableIndexMap indexMap = DBProvider.getActualTableIndexMap(utableName);
            if (indexMap != null) {
                Set<String> altIndexSet = indexMap.getAlternateIndexes();
                if (altIndexSet != null) {
                    for (Iterator<String> i = altIndexSet.iterator(); i.hasNext();) {
                        String indexName = i.next();
                        try {
                            DBProvider.removeAlternateIndex(utableName, indexName);
                            Print.logInfo("Dropped alternate index '"+indexName+"' from table "+utableName);
                        } catch (Throwable th) {
                            //Print.logWarn("Unable to delete alternate index '"+indexName+"' for table "+TN);
                            // ignore exception
                        }
                    }
                }
            }
        } catch (DBException dbe) {
            Print.logWarn("Unable to retrieve index information: " + dbe.getMessage());
        }

        /* recreate alternate indexes */
        DBAlternateIndex altIndexes[] = this.getAlternateIndexes();
        if ((altIndexes != null) && (altIndexes.length > 0)) {
            // this table has alternate keys

            /* loop through alternate indexes */
            for (int i = 0; i < altIndexes.length; i++) {
                String indexName = altIndexes[i].getIndexName();
    
                /* drop existing alternate key */
                try {
                    DBProvider.removeAlternateIndex(utableName, indexName);
                } catch (Throwable th) {
                    // This exception is expected
                    //Print.logWarn("Delete Alternate Index: '"+indexName+"' doesn't exist for table " + TN);
                    // ignore exception
                }

                /* rebuild alternate key */
                try {
                    DBField indexFields[] = altIndexes[i].getFields();
                    DBProvider.createAlternateIndex(utableName, altIndexes[i]);
                    Print.logInfo("Created alternate index '" + indexName + "' for table " + utableName);
                } catch (SQLException sqe) {
                    throw new DBException("Alternate index create error", sqe);
                }

            }

        } else {
            // this table does not have alternate keys

            /* drop default alternate key ('altIndex') */
            try {
                DBProvider.removeAlternateIndex(utableName, null);
            } catch (Throwable th) {
                // This exception is expected
                //Print.logWarn("Alternate key does not currently exist for table: " + utableName);
                // ignore exception
            }
            
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Creates the table represented by this DBFactory
    *** @throws DBException   If a database error occurs
    **/
    public void createTable()
        throws DBException
    {
        try {
            DBProvider.createTable(this);
        } catch (SQLException sqe) {
            throw new DBException("Table creation", sqe);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Drops the table represented by this DBFactory
    *** @throws DBException   If a database error occurs
    **/
    public void dropTable()
        throws DBException
    {
        try {
            DBProvider.dropTable(this.getUntranslatedTableName());
        } catch (SQLException sqe) {
            throw new DBException("Drop table", sqe);
        }
    }

    // ------------------------------------------------------------------------
        
    public    static final String _DUMP_EXT_TXT         = "." + ARCHIVE_EXT_TXT;
    public    static final String _DUMP_EXT_SQL         = "." + ARCHIVE_EXT_SQL;
    public    static final String _DUMP_EXT_CSV         = "." + ARCHIVE_EXT_CSV;
    public    static final String _DUMP_EXT_XML         = "." + ARCHIVE_EXT_XML;

    public    static final int    DUMP_FORMAT_CSV       = 0;
    public    static final int    DUMP_FORMAT_SQL       = 1;
    public    static final int    DUMP_FORMAT_XML       = 2;

    /**
    *** Dumps the table represented by this DBFactory to the specified file
    *** @param toFile  The destination file
    *** @param append  True to append to existing table dump file
    *** @throws DBException   If a database error occurs
    **/
    public void dumpTable(File toFile, boolean append)
        throws DBException
    {
        this.dumpTable(toFile, append, /*DBSelect*/null, /*String[]*/null);
    }

    /**
    *** Dumps the table represented by this DBFactory to the specified file
    *** @param toFile  The destination file
    *** @param append  True to append to existing table dump file
    *** @param where   Record selection WHERE clause
    *** @throws DBException   If a database error occurs
    **/
    public void dumpTable(File toFile, boolean append, String where)
        throws DBException
    {
        DBSelect<gDBR> dbSel = null;
        if (!StringTools.isBlank(where)) {
            dbSel = new DBSelect<gDBR>(this, where);
        }
        this.dumpTable(toFile, append, dbSel, /*String[]*/null);
    }

    /**
    *** Dumps the table represented by this DBFactory to the specified file.
    *** @param toFile  The destination file
    *** @param append  True to append to existing table dump file
    *** @param dsel    The selection specification indicating which records should be 'dumped'
    *** @throws DBException   If a database error occurs
    **/
    public void dumpTable(File toFile, boolean append, DBSelect<gDBR> dsel)
        throws DBException
    {
        this.dumpTable(toFile, append, dsel, /*String[]*/null);
    }

    /**
    *** Dumps the table represented by this DBFactory to the specified file.
    *** @param toFile  The destination file
    *** @param dsel    The selection specification indicating which records should be 'dumped'
    *** @param fldn    A list of field names to include in the dump file
    *** @throws DBException   If a database error occurs
    **/
    public void dumpTable(File toFile, boolean append, DBSelect<gDBR> dsel, String fldn[])
        throws DBException
    {
            
        /* validate filename */
        if (toFile == null) {
            throw new DBException("'To' file not specified");
        }

        /* include header */
        boolean inclHeader = (append && toFile.exists())? false : true;

        /* dump to file */
        PrintWriter dumpOutStream = null;
        boolean closeStream = true;
        try {

            /* output format */
            String fn = toFile.getName();
            int outputFmt = DUMP_FORMAT_CSV;
            if (fn.endsWith(_DUMP_EXT_CSV)) {
                outputFmt = DUMP_FORMAT_CSV;
            } else 
            if (fn.endsWith(_DUMP_EXT_XML)) {
                outputFmt = DUMP_FORMAT_XML;
            } else {
                outputFmt = DUMP_FORMAT_SQL;
            }
            
            /* open output and dump */
            if (fn.startsWith("stdout.")) {
                Print.logDebug("Output to STDOUT ...");
                dumpOutStream = new PrintWriter(System.out, true);
                closeStream = false;
            } else
            if (fn.startsWith("stderr.")) {
                Print.logDebug("Output to STDERR ...");
                dumpOutStream = new PrintWriter(System.err, true);
                closeStream = false;
            } else {
                Print.logDebug("Output to File: '%s' ...", toFile.toString());
                dumpOutStream = new PrintWriter(new FileOutputStream(toFile,append));
                closeStream = true;
            }
            this._dumpTable(dumpOutStream, inclHeader, dsel, fldn, outputFmt);
            
        } catch (IOException ioe) {
            throw new DBException("Dumping table", ioe);
        } finally{
            if (closeStream && (dumpOutStream != null)) { 
                try{ dumpOutStream.close(); } catch (Throwable th) {} 
            }
        }
        
    }

    /**
    *** Dumps the table represented by this DBFactory to the specified file.
    *** @param dumpOutStream The destination output stream
    *** @param dsel          The selection specification indicating which records should be 'dumped'
    *** @param outFmt        The output format
    *** @throws DBException   If unable to successfully dump the table
    **/
    protected void _dumpTable(PrintWriter dumpOutStream, boolean inclHeader, DBSelect<gDBR> dsel, String fldn[], int outFmt)
        throws DBException
    { // rs

        /* validate filename */
        if (dumpOutStream == null) {
            throw new DBException("Output stream not specified");
        }

        /* validate DBSelect */
        if (dsel == null) {
            //throw new DBException("DBSelect must not be null");
            dsel = new DBSelect<gDBR>(this);
        } else
        if (!this.equals(dsel.getFactory())) {
            // DBFactory instances do not match
            throw new DBException("DBSelect factory does not match this factory");
        }

        /* dump to PrintWriter */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        DBField fields[] = this.getFields(fldn);
        try {
            StringBuffer sbData = new StringBuffer();

            /* field header */
            if (inclHeader) {
                sbData.setLength(0);
                if (outFmt == DUMP_FORMAT_CSV) {
                    // .CSV format
                    for (int i = 0; i < fields.length; i++) {
                        if (i > 0) { sbData.append(","); }
                        sbData.append("\"" + fields[i].getName() + "\"");
                    }
                    sbData.append("\n");
                } else
                if (outFmt == DUMP_FORMAT_XML) {
                    // .XML format
                    sbData.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    sbData.append("<" + TAG_Records);
                        sbData.append(" " + ATTR_table + "=\"" + this.getUntranslatedTableName() + "\"");
                        sbData.append(">\n");
                } else
                if (outFmt == DUMP_FORMAT_SQL) {
                    // .SQL format
                    sbData.append("# ");
                    for (int i = 0; i < fields.length; i++) {
                        if (i > 0) { sbData.append(", "); }
                        sbData.append(fields[i].getName());
                    }
                    sbData.append("\n");
                } else {
                    // ??? format
                }
                dumpOutStream.write(sbData.toString());
            }
            sbData.setLength(0);

            /* get result set */
            // MySQL can create/cause an OutOfMemoryError here (and has):
            //   http://forums.mysql.com/read.php?39,152636,153012#msg-153012
            //   http://forums.mysql.com/read.php?39,152636,153560#msg-153560
            try {
                // Retrieved rowByRow: "http://forums.mysql.com/read.php?39,152636,153012#msg-153012"
                dbc  = DBConnection.getDBConnection_read();
                stmt = dbc.execute(dsel.toString(), true); // row by row
                rs   = stmt.getResultSet();
            } catch (OutOfMemoryError oome) {
                Print.logException("Out of memory", oome);
                throw new DBException("Out of memeory", oome);
            } // dbc released below

            /* iterate through result set */
            long recordCount = 0L;
            while (rs.next()) {
                recordCount++;
                //Print.logInfo("Record #" + recordCount);
                sbData.setLength(0);
                if (outFmt == DUMP_FORMAT_CSV) {
                    // .CSV format
                    for (int i = 0; i < fields.length; i++) {
                        if (i > 0) { sbData.append(","); }
                        String n = fields[i].getName();
                        Object r = fields[i].getResultSetValue(rs);
                        String v = (r != null)? r.toString() : "";
                      //String v = rs.getString(n);
                        sbData.append(fields[i].getQValue(v));
                    }
                    sbData.append("\n");
                } else
                if (outFmt == DUMP_FORMAT_XML) {
                    // .XML format
                    int indent = 3;
                    String prefix = StringTools.replicateString(" ", indent);
                    sbData.append(prefix).append("<"+TAG_Record+" " + ATTR_sequence + "=\"" + recordCount + "\">\n");
                    for (int i = 0; i < fields.length; i++) {
                        String value = DBFieldValues.toStringValue(fields[i].getResultSetValue(rs));
                        DBFactory.writeXML_DBField(sbData, 2*indent, fields[i], false/*inclInfo*/, value);
                    }
                    sbData.append(prefix).append("</"+TAG_Record+">\n");
                } else
                if (outFmt == DUMP_FORMAT_SQL) {
                    // .SQL format
                    for (int i = 0; i < fields.length; i++) {
                        if (i > 0) { sbData.append(", "); }
                        String n = fields[i].getName();
                        Object r = fields[i].getResultSetValue(rs);
                        String v = (r != null)? r.toString() : "";
                      //String v = rs.getString(n);
                        sbData.append(fields[i].getQValue(v));
                    }
                    sbData.append("\n");
                } else {
                    // ??? format
                }
                dumpOutStream.write(sbData.toString());
            }

            /* field footer */
            sbData.setLength(0);
            if (outFmt == DUMP_FORMAT_CSV) {
                // .CSV format
                // (nothing need be done here)
            } else
            if (outFmt == DUMP_FORMAT_XML) {
                // .XML format
                sbData.append("</" + TAG_Records + ">\n");
            } else
            if (outFmt == DUMP_FORMAT_SQL) {
                // .SQL format
                // (nothing need be done here)
            } else {
                // ??? format
            }
            dumpOutStream.write(sbData.toString());
            dumpOutStream.flush();

        } catch (DBException dbe) {
            throw dbe; // re-throw
        } catch (SQLException sqe) {
            throw new DBException("Dumping table", sqe);
        } catch (Throwable th) {
            throw new DBException("Dumping table", th);
        } finally {
            if (rs   != null) { try{ rs.close();    } catch (SQLException sqe) {} }
            if (stmt != null) { try{ stmt.close();  } catch (SQLException sqe) {} }
            DBConnection.release(dbc);
        }
        
    }
    
    // ------------------------------------------------------------------------

    /**
    *** MySQLDumpReader class
    **/
    protected static class MySQLDumpReader
    {
        private int pushedByte = -1;
        private FileInputStream fis = null;
        public MySQLDumpReader(File file) throws IOException {
            super();
            this.fis = new FileInputStream(file);
        }
        public String readLineString() throws IOException {
            byte buff[] = this.readLineBytes();
            if (buff != null) {
                String line = StringTools.toStringValue(buff);
                //Print.logDebug("Line: " + line + " [" + buff.length + "/" + line.length() + "]");
                return line;
            } else {
                return null;
            }
        }
        public byte[] readLineBytes() throws IOException {
            byte buff[] = new byte[10 * 1024];
            int len = 0;
            boolean quoted = false;
            boolean eof = false;
            for (;len < buff.length;) {
                
                /* read single byte */
                int ch = this.read();
                if (ch < 0) {
                    eof = true;
                    break;
                }
                //Print.logDebug("Char: " + ((char)ch) + " [" + ch);
                
                /* parse character */
                if (ch == '\"') {
                    quoted = !quoted;
                    buff[len++] = '\"';
                } else
                if (ch == '\\') {
                    buff[len++] = '\\';
                    ch = this.read(); // read next character
                    if (ch < 0) { break; }
                    buff[len++] = (byte)ch; // unfiltered if preceded with \
                } else
                if (quoted) {
                    buff[len++] = (byte)ch; // unfiltered if quoted
                } else
                if (ch == '\r') {
                    ch = this.read(); // skip '\n' (if present)
                    if ((ch >= 0) && (ch != '\n')) {
                        this.pushedByte = ch & 0xFF;
                    }
                    break; // end-of-line
                } else
                if (ch == '\n') {
                    break; // end-of-line
                } else {
                    buff[len++] = (byte)ch; // unfiltered
                }

            }
            if (!eof || (len > 0)) {
                byte line[] = new byte[len];
                System.arraycopy(buff, 0, line, 0, len);
                return line;
            } else {
                return null;
            }
        }
        private int read() throws IOException {
            int b = -1;
            if (this.pushedByte >= 0) {
                b = (byte)this.pushedByte;
                this.pushedByte = -1;
            } else {
                b = this.fis.read();
            }
            return (b == -1)? -1 : (b & 0xFF);
        }
        public void close() throws IOException {
            this.fis.close();
        }
    }
    
    // ------------------------------------------------------------------------

    public    static final String _LOAD_EXT_CSV         = "." + ARCHIVE_EXT_CSV;
    public    static final String _LOAD_EXT_DUMP        = "." + ARCHIVE_EXT_DUMP;
    public    static final String _LOAD_EXT_SQL         = "." + ARCHIVE_EXT_SQL;
    public    static final String _LOAD_EXT_TXT         = "." + ARCHIVE_EXT_TXT;

    /** 
    *** Loads the data in the specified file into the table represented by this DBFactory
    *** @param fromFile  The file containing the record data to load
    *** @return The number of records loaded into the table from the specified file
    *** @throws DBException   If a database error occurs
    **/
    public long loadTable(File fromFile)
        throws DBException
    {
        return this.loadTable(fromFile, null, 
            true/*insertRecords*/, true/*overwriteExisting*/, false/*noDropWarning*/);
    }

    /** 
    *** Loads the data in the specified file into the table represented by this DBFactory
    *** @param fromFile  The file containing the record data to load
    *** @param insertRecords True to insert records, false otherwise
    *** @param overwriteExisting  True to overwrite existing matching records
    *** @return The number of records loaded into the table from the specified file
    *** @throws DBException   If a database error occurs
    **/
    @Deprecated
    public long loadTable(File fromFile, 
        boolean insertRecords, boolean overwriteExisting)
        throws DBException
    {
        return this.loadTable(fromFile, null/*DBLoadValidator*/, 
            insertRecords, overwriteExisting, false/*noDropWarning*/);
    }

    /** 
    *** Loads the data in the specified file into the table represented by this DBFactory
    *** @param fromFile  The file containing the record data to load
    *** @param insertRecords True to insert records, false otherwise
    *** @param overwriteExisting  True to overwrite existing matching records
    *** @param noDropWarning True to supress field "will be dropped" warnings.
    *** @return The number of records loaded into the table from the specified file
    *** @throws DBException   If a database error occurs
    **/
    public long loadTable(File fromFile, 
        boolean insertRecords, boolean overwriteExisting, boolean noDropWarning)
        throws DBException
    {
        return this.loadTable(fromFile, null/*DBLoadValidator*/, 
            insertRecords, overwriteExisting, noDropWarning);
    }

    /** 
    *** Loads the data in the specified file into the table represented by this DBFactory
    *** @param fromFile  The file containing the record data to load
    *** @param validator The InserstionValidator filter which determines whether a given record
    ***                  should be inserted into the table.
    *** @param insertRecords True to insert records, false otherwise
    *** @param overwriteExisting  True to overwrite existing matching records
    *** @return The number of records loaded into the table from the specified file
    *** @throws DBException   If unable to load the table
    **/
    @Deprecated
    public long loadTable(File fromFile, DBLoadValidator validator, 
        boolean insertRecords, boolean overwriteExisting)
        throws DBException
    {
        return this.loadTable(fromFile, validator, 
            insertRecords, overwriteExisting, false/*noDropWarning*/);
    }

    /** 
    *** Loads the data in the specified file into the table represented by this DBFactory
    *** @param fromFile  The file containing the record data to load
    *** @param validator The InserstionValidator filter which determines whether a given record
    ***                  should be inserted into the table.
    *** @param insertRecords True to insert records, false otherwise
    *** @param overwriteExisting  True to overwrite existing matching records
    *** @param noDropWarning True to supress field "will be dropped" warnings.
    *** @return The number of records loaded into the table from the specified file
    *** @throws DBException   If unable to load the table
    **/
    public long loadTable(File fromFile, DBLoadValidator validator, 
        boolean insertRecords, boolean overwriteExisting, boolean noDropWarning)
        throws DBException
    {

        /* validate filename */
        if (fromFile == null) {
            throw new DBException("'From' file not specified");
        }
        String fn = fromFile.getName();

        /* .CSV */
        if (fn.endsWith(_LOAD_EXT_CSV)) {
            return this._loadTableCSV(fromFile, validator, insertRecords, overwriteExisting, noDropWarning);
        }

        /* .DUMP */
        if (fn.endsWith(_LOAD_EXT_DUMP)) {
            return this._loadTable(null, fromFile, validator, insertRecords, overwriteExisting, noDropWarning);
        }

        /* .SQL */
        if (fn.endsWith(_LOAD_EXT_SQL)) {
            File sqlFile = fromFile;
            String fields[] = this.readSQLDumpColumns(sqlFile);
            File txtFile = new File(FileTools.removeExtension(fromFile.getPath()) + _LOAD_EXT_TXT);
            return this._loadTable(fields, txtFile, validator, insertRecords, overwriteExisting, noDropWarning);
        }

        /* .TXT */
        if (fn.endsWith(_LOAD_EXT_TXT)) {
            File sqlFile = new File(FileTools.removeExtension(fromFile.getPath()) + _LOAD_EXT_SQL);
            String fields[] = this.readSQLDumpColumns(sqlFile);
            File txtFile = fromFile;
            return this._loadTable(fields, txtFile, validator, insertRecords, overwriteExisting, noDropWarning);
        }

        /* error if we reach here */
        throw new DBException("Unrecognized file extension '" + fromFile + "'");

    }
    
    /** 
    *** Loads the data in the specified file into the table represented by this DBFactory
    *** @param oldFieldNames  A list of field names to use for insertion validation instead of the list
    ***                       of field names present in the loaded file specification section.
    *** @param fromFile  The file containing the record data to load
    *** @param validator The InserstionValidator filter which determines whether a given record
    ***                  should be inserted into the table.
    *** @param insertRecords True to insert records, false otherwise
    *** @param overwriteExisting  True to overwrite existing matching records
    *** @param noDropWarning True to supress field "will be dropped" warnings.
    *** @return The number of records loaded into the table from the specified file
    *** @throws DBException   If a database error occurs
    **/
    protected long _loadTable(String oldFieldNames[], File fromFile, DBLoadValidator validator, 
        boolean insertRecords, boolean overwriteExisting, boolean noDropWarning)
        throws DBException
    {
        MySQLDumpReader fr = null;

        long recordCount = 0L;
        try {

            /* open file */
            fr = new MySQLDumpReader(fromFile);

            /* field/column definition */
            if (ListTools.isEmpty(oldFieldNames)) {
                String firstLine = fr.readLineString();
                if (firstLine.startsWith("#")) {
                    oldFieldNames = StringTools.parseArray(firstLine.substring(1).trim());
                } else {
                    Print.logError("Unable to determine column mapping definitions");
                    throw new DBException("Missing column definitions, unable to load file");
                }
            }

            /* list fields */
            for (int i = 0; i < oldFieldNames.length; i++) {
                DBField field = this.getField(oldFieldNames[i]);
                if (field != null) {
                    Print.logInfo("Column : " + oldFieldNames[i]);
                } else
                if (!noDropWarning) {
                    Print.logInfo("Column : " + oldFieldNames[i] + "  - will be dropped");
                }
            }

            /* initialize DBLoadValidator */
            if ((validator != null) && !validator.setFields(oldFieldNames)) {
                throw new DBException("Load fields rejected by insertion validator");
            }

            /* loop through file */
            int rowNumber = 2; // start at line '2'
            for (;;rowNumber++) {

                /* read line */
                String r = fr.readLineString();
                if (r == null) { break; }
                if ((r == null) || r.startsWith("#")) { continue; }
                //if ((r.length == 0) || (r[0] == '#')) { continue; }

                /* parse line */
                //Print.logInfo("Row: " + r);
                String rowValues[] = StringTools.parseArray(r);
                //String partialKey = (rowValues.length > 0)? rowValues[0] : "?";
                if (rowValues.length != oldFieldNames.length) {
                    Print.logError("Fields - #found != #expected: " + 
                        rowValues.length + " != " + oldFieldNames.length +
                        " [row " + rowNumber + "]");
                    Print.logError("Row: " + r);
                    continue;
                }

                /* create/insert record from fields */
                if (this._loadInsertRecord(oldFieldNames,rowValues,
                    validator,insertRecords,overwriteExisting)) {
                    recordCount++;
                }
                
            } // next record

        } catch (DBException dbe) {
            throw dbe; // re-throw
        } catch (SQLException sqe) {
            throw new DBException("SQL error", sqe);
        } catch (IOException ioe) {
            throw new DBException("Parsing error", ioe);
        } catch (Throwable th) {
            throw new DBException("Unexpected error", th);
        } finally {
            if (fr != null) { try { fr.close();  } catch (Throwable t) {} }
        }

        /* return number of records loaded */
        return recordCount;

    }

    /**
    *** Reads the column/field names from the specified SQL dump file
    *** @param tableSQLFile  The SQL dump file
    *** @return The SQL column/field names
    *** @throws DBException   If unable to load the SQL dump
    **/
    private String[] readSQLDumpColumns(File tableSQLFile)
        throws DBException
    {
        
        /* table */
        if (!tableSQLFile.exists() || tableSQLFile.isDirectory()) {
            return null;
        }
        
        /* parse */
        java.util.List<String> clist = new Vector<String>();
        BufferedReader fr = null;
        boolean createFound = false;
        try {
            fr = new BufferedReader(new FileReader(tableSQLFile));
            for (;;) {
                String r = fr.readLine();
                if (r == null) { break; }
                r = r.trim();
                
                /* blank line or start with comment */
                if (r.length() == 0) { continue; }
                if (r.startsWith("/") || r.startsWith("-")) { continue; }
                                
                /* drop everything until "CREATE" is found */
                if (!createFound) {
                    if (r.toUpperCase().startsWith("CREATE")) { // CREATE TABLE `Geozone` (
                        createFound = true;
                    }
                    continue;
                }
                
                /* exit if we find the ending ")" */
                if (r.startsWith(")")) {
                    break;
                }
                
                /* skip 'KEY' indicators */
                String rup = r.toUpperCase();
                if (rup.startsWith("PRIMARY") ||        // PRIMARY KEY  (`accountID`,`geozoneID`,`sortID`),
                    rup.startsWith("UNIQUE" ) ||        // UNIQUE KEY `rowID` (`rowID`)
                    rup.startsWith("KEY")       ) {     // KEY `altIndex` (`uniqueID`)
                    continue;
                }
                
                /* extract field name */
                // `accountID` varchar(32) NOT NULL,
                // `geozoneID` varchar(32) NOT NULL,
                // `rowID` int(11) NOT NULL auto_increment,
                String c = r;
                int s = c.startsWith("`")? 1 : 0;   // field name may start with "`"
                int p = c.indexOf(" ");             // first blank space after field name
                if ((p > (s + 1)) && (c.charAt(p - 1) == '`')) { p--; } // field name may end with "`"
                String cnam = (p > s)? c.substring(s, p) : ""; // extrac field name
                if (!cnam.equals("")) {
                    //Print.logDebug("Found field: " + cnam);
                    clist.add(cnam);
                }
                
            }
        } catch (IOException ioe) {
            Print.logStackTrace("Parsing error", ioe);
            return null;
        } finally {
            if (fr != null) { try { fr.close(); } catch (Throwable t) {} }
        }
        
        /* return columns */
        return clist.toArray(new String[clist.size()]);
        
    }

    /** 
    *** Loads table data from the specified CSV file
    *** @param fromFile  The CSV file
    *** @param validator The insertion validator
    *** @param overwriteExisting  True to overwrite any existing db record, false to leave existing db record as-is
    *** @return The number of records loaded into the table
    *** @throws DBException   If unable to load the table
    **/
    protected long _loadTableCSV(File fromFile, DBLoadValidator validator, 
        boolean insertRecords, boolean overwriteExisting, boolean noDropWarning)
        throws DBException
    {
        InputStream fis = null;

        /* load csv file */
        long recordCount = 0L;
        try {

            /* open csv file */
            try {
                fis = new FileInputStream(fromFile);
            } catch (IOException ioe) {
                throw new DBException("Unable to open CSV file", ioe);
            }

            /* field/column definition */
            String oldFieldNames[]= null;
            try {
                String header = FileTools.readLine(fis);
                oldFieldNames = StringTools.parseArray(header);
                if (ListTools.isEmpty(oldFieldNames)) {
                    throw new DBException("Unable to parse field names (no field names found)");
                }
            } catch (EOFException eofe) {
                throw new DBException("Premature EOF");
            }

            /* list fields */
            for (int i = 0; i < oldFieldNames.length; i++) {
                DBField field = this.getField(oldFieldNames[i]);
                if (field != null) {
                    Print.logInfo("Column : " + oldFieldNames[i]);
                } else
                if (!noDropWarning) {
                    Print.logInfo("Column : " + oldFieldNames[i] + "  - will be dropped");
                }
            }

            /* initialize DBLoadValidator */
            if ((validator != null) && !validator.setFields(oldFieldNames)) {
                throw new DBException("Load fields rejected by insertion validator");
            }

            /* loop through CSV file */
            int rowNumber = 2; // start at line '2'
            for (;;rowNumber++) {
                
                /* read/parse line */
                String rowValues[] = null;
                try {
                    String line = FileTools.readLine(fis).trim();
                    if (line.equals("")) { 
                        // -- ignore blank lines
                        continue; 
                    }
                    //Print.logInfo("Parsing: " + line);
                    rowValues = StringTools.parseArray(line);
                    if (rowValues.length != oldFieldNames.length) {
                        // -- unexpected number of fields
                        Print.logError("Fields - #found != #expected: " + 
                            rowValues.length + " != " + oldFieldNames.length +
                            " [row " + rowNumber + "]");
                        Print.logError("Row: " + line);
                        continue;
                    }
                } catch (EOFException eofe) {
                    break;
                }

                /* create/insert record from fields */
                if (this._loadInsertRecord(oldFieldNames,rowValues,
                    validator,insertRecords,overwriteExisting)) {
                    recordCount++;
                }

            }
            
        } catch (DBException dbe) {
            throw dbe; // re-throw
        } catch (SQLException sqe) {
            throw new DBException("SQL error", sqe);
        } catch (IOException ioe) {
            throw new DBException("Parsing error", ioe);
        } catch (Throwable th) {
            throw new DBException("Unexpected error", th);
        } finally {
            if (fis != null) { try { fis.close();  } catch (Throwable t) {} }
        }

        /* return number of records loaded */
        return recordCount;

    }

    /**
    *** Inserts the specified data, using the specified field layout, into this table
    *** @param oldFieldNames  The column/field layout
    *** @param rowValues      The values for the specifies columns/fields
    *** @param overwriteExisting  True to allow updating existing records
    *** @param validator      The insertion validator
    *** @return True if the opperation was succesful
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    *** @throws IOException   If an I/O error occurs
    **/
    private boolean _loadInsertRecord(String oldFieldNames[], String rowValues[], 
        DBLoadValidator validator, boolean insertRecord, boolean overwriteExisting)
        throws DBException, SQLException, IOException
    {

        /* row values DBLoadValidator */
        if ((validator != null) && !validator.validateValues(rowValues)) {
            // -- update/insertion failed validation
            return false;
        }

        /* create record key/values */
        DBRecordKey<?> dbRcdKey  = this.createKey();        // may throw DBException
        DBFieldValues  dbKeyVals = dbRcdKey.getKeyValues();
        DBFieldValues  dbFldVals = dbRcdKey.getFieldValues();
        DBRecord<?>    dbRcd     = dbRcdKey.getDBRecord();  // does not reload record

        /* parse all field values */
        boolean addedField = false;
        Set<String> fieldNameList = new HashSet<String>();
        for (int i = 0; i < oldFieldNames.length; i++) {
            String fieldName = oldFieldNames[i];
            DBField dbFld = this.getField(fieldName);
            if (dbFld != null) {
                // -- column exists
                boolean       priKey = dbFld.isPrimaryKey();
                DBFieldValues dbVals = priKey? dbKeyVals : dbFldVals;
                // -- column value
                String rowVal = ((i < rowValues.length) && !rowValues[i].equals("\\N"))? rowValues[i] : null;
                Object objVal = dbFld.parseStringValue(rowVal);
                // -- set value
                boolean didSet = dbVals._setFieldValue(dbFld, objVal);
                if (!didSet) {
                    Print.logError("Invalid field type: %s [%s]", fieldName, StringTools.className(objVal));
                } else
                if (priKey) {
                    // -- skip primary key update
                } else {
                    fieldNameList.add(fieldName);
                }
                addedField = true;
            } else {
                // -- column does not exist in defined table (will be dropped)
            }
        }

        /* skip record insert/update? */
        if (!insertRecord) {
            // -- do not insert/update
            return false;
        }

        /* update/insert */
        if (dbRcdKey.exists()) {

            /* disallow overwrite? */
            if (!overwriteExisting) {
                // -- Record already exists, and we don't have permission to overwrite the record
                return false;
            }

            /* remove fields which should not be updated */
            fieldNameList.remove(DBRecord.FLD_creationTime);
            fieldNameList.remove(DBRecord.FLD_creationMillis);

            /* nothing to update? */
            if (fieldNameList.isEmpty()) {
                // -- invalid record
                throw new DBException("No fields to update");
            }

            /* insert */
            if (validator != null) {
                if (!validator.validateUpdate(dbRcd,fieldNameList)) {
                    return false; // failed validation
                }
                dbRcd.update(fieldNameList);
                validator.recordDidUpdate(dbRcd,fieldNameList);
            } else {
                dbRcd.update(fieldNameList);
            }

        } else {

            /* nothing to insert?
            if (!addedField) {
                throw new DBException("No fields to insert");
            }
            */

            /* insert */
            if (validator != null) {
                if (!validator.validateInsert(dbRcd)) {
                    return false; // failed validation
                }
                dbRcd.insert();
                validator.recordDidInsert(dbRcd);
            } else {
                dbRcd.insert();
            }

        }
        return true;

    }
    
    /**
    *** Inserts the specified data, using the specified field layout, into this table
    *** @param oldFieldNames  The column/field layout
    *** @param rowValues      The values for the specifies columns/fields
    *** @param overwriteExisting  True to allow updating existing records
    *** @return True if the opperation was succesful
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    *** @throws IOException   If an I/O error occurs
    *** @Deprecated Old code. Use {@link #_loadInsertRecord} instead
    **/
    private boolean _old_loadInsertRecord(String oldFieldNames[], String rowValues[], 
        boolean insertRecord, boolean overwriteExisting)
        throws DBException, SQLException, IOException
    {
        String xtableName = this.getTranslatedTableName();
        DBRecordKey<? extends DBRecord<?>> rcdKey = this.createKey(); // may throw DBException
        
        /* parse all field values */
        Object fieldValues[] = new Object[oldFieldNames.length];
        for (int i = 0; i < oldFieldNames.length; i++) {
            String fieldName = oldFieldNames[i];
            DBField field = this.getField(fieldName);
            if (field != null) {
                String rowVal = ((i < rowValues.length) && !rowValues[i].equals("\\N"))? rowValues[i] : null;
                Object objVal = field.parseStringValue(rowVal);
                fieldValues[i] = objVal;
                if (field.isPrimaryKey() && !rcdKey.setKeyValue(fieldName, objVal)) {
                    Print.logError("Couldn't find Key fieldName: " + fieldName);
                }
            } else {
                // column does not exist in defined table
                fieldValues[i] = null;
            }
        }

        /* skip record insertion */
        if (!insertRecord) {
            return false;
        }

        /* now check for record existance */
        boolean recordExists = rcdKey.exists(); // may throw DBException
        if (recordExists && !overwriteExisting) {
            // Record already exists, and we don't have permission to overwrite the record
            return false;
        }

        /* update/insert? */
        boolean addedField = false;
        StringBuffer sbSql = new StringBuffer();
        if (recordExists) {
            // MySQL:     UPDATE <table> SET <column>=<value>, ... WHERE ...
            // SQLServer: UPDATE <table> SET <column>=<value>, ... WHERE ...
            // Derby:     UPDATE <table> SET <column>=<value>, ... WHERE ...
            sbSql.append("UPDATE ").append(this.getTranslatedTableName());
            sbSql.append(" SET ");
            for (int i = 0; i < oldFieldNames.length; i++) {
                String fieldName = oldFieldNames[i];
                DBField field = this.getField(fieldName);
                if ((field != null) && !field.isPrimaryKey()) {
                    // update field iff it exists and is not a primary key
                    if (addedField) {
                        sbSql.append(","); 
                    }
                    //sbSql.append(DBProvider.getProvider().getStartColumnChar());
                    //sbSql.append(fieldName);
                    //sbSql.append(DBProvider.getProvider().getEndColumnChar());
                    sbSql.append(DBProvider.getProvider().quoteColumnName(fieldName));
                    sbSql.append("=").append(field.getQValue(fieldValues[i]));
                    addedField = true;
                } else {
                    // column ignored (dropped)
                }
            }
            sbSql.append(" ").append(rcdKey.getWhereClause(DBWhere.KEY_FULL));
        } else {
            // MySQL:     INSERT INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
            // SQLServer: INSERT INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
            // Derby:     INSERT INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
            StringBuffer colSB = new StringBuffer();
            StringBuffer valSB = new StringBuffer();
            for (int i = 0; i < oldFieldNames.length; i++) {
                String fieldName = oldFieldNames[i];
                DBField field = this.getField(fieldName);
                if (field != null) {
                    if (addedField) { 
                        colSB.append(","); 
                        valSB.append(","); 
                    }
                    //colSB.append(DBProvider.getProvider().getStartColumnChar());
                    //colSB.append(fieldName);
                    //colSB.append(DBProvider.getProvider().getEndColumnChar());
                    colSB.append(DBProvider.getProvider().quoteColumnName(fieldName));
                    valSB.append(field.getQValue(fieldValues[i]));
                    addedField = true;
                } else {
                    // column ignored (dropped)
                }
            }
            sbSql.append("INSERT INTO ").append(xtableName);
            sbSql.append(" (").append(colSB).append(")");
            sbSql.append(" VALUES (").append(valSB).append(")");
        }

        /* no fields? */
        if (!addedField) {
            throw new DBException("No fields in dump file match fields in current table");
        }
        
        /* insert/update */
        DBConnection dbc = null;
        Statement   stmt = null;
        try {
            //Print.logDebug("[SQL] " + sbSql);
            dbc  = DBConnection.getDBConnection_write();
            stmt = dbc.execute(sbSql.toString());
        } catch (SQLException sqe) {
            if (sqe.getErrorCode() == SQLERR_DUPLICATE_KEY) {
                Print.logInfo("Duplicate Key Skipped: " + rcdKey);
            } else {
                throw sqe; // will be re-caught by outer catch
            }
        } finally {
            if (stmt != null) { try{ stmt.close();  } catch (SQLException sqe) {} }
            DBConnection.release(dbc);
        }
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Executes the specified SQL query
    *** @param sql  The SQL query
    **/
    //protected static void executeUpdate(String sql) 
    //    throws SQLException, DBException
    //{
    //    DBConnection dbc = null;
    //    try {
    //        dbc = DBConnection.getDBConnection_readWrite();
    //        dbc.executeUpdate(sql);
    //    } finally {
    //        DBConnection.release(dbc);
    //    }
    //}

    /**
    *** Executes the specified SQL query
    *** @param sql  The SQL query
    *** @return The resulting SQL Statement
    **/
    //protected static Statement execute(String sql) 
    //    throws SQLException, DBException
    //{
    //    DBConnection dbc = null;
    //    Statement   stmt = null;
    //    try {
    //        dbc  = DBConnection.getDBConnection_readWrite();
    //        stmt = dbc.execute(sql);
    //    } finally {
    //        DBConnection.release(dbc);
    //    }
    //    // WARNING: This DBConnection may not be available until the statement is closed!
    //    return stmt;
    //}

    /**
    *** Executes the specified SQL query
    *** @param sql  The SQL query
    *** @param rowByRow True to return data row-by-row
    *** @return The resulting SQL Statement
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    //protected static Statement execute(String sql, boolean rowByRow) 
    //    throws SQLException, DBException
    //{
    //    DBConnection dbc = null;
    //    Statement   stmt = null;
    //    try {
    //        dbc  = DBConnection.getDBConnection_readWrite();
    //        stmt = dbc.execute(sql, rowByRow);
    //    } finally {
    //        DBConnection.release(dbc);
    //    }
    //    // WARNING: This DBConnection may not be available until the statement is closed!
    //    return stmt;
    //}

    // ------------------------------------------------------------------------

    /**
    *** Adds this table as a dependent of the specified parent table name
    *** @param utableName The parent untranslated table name
    **/
    public void addParentTable(String utableName)
    {
        if ((utableName != null) && !this.parentTables.contains(utableName)) {
            this.parentTables.add(utableName);
            this.hierarchy = null;
        }
    }

    /**
    *** Get the list of parent ancestors
    *** @return A list of parent ancestor table names that this table is a depentend of
    **/
    public java.util.List<String> getParentTables()
    {
        return this.parentTables; // never null
    }

    /**
    *** Returns true if this table has the specified table name as a parent
    *** @param utableName The parent untranslated table name to test
    *** @return True if this table is a dependent of the specified table, false otherwise
    **/
    public boolean hasParentTable(String utableName)
    {
        return this.parentTables.contains(utableName);
    }

    /**
    *** Returns the DBFactories of ALL dependent children/grandchildren of this table.<br>
    *** The returned list is sorted by the hierarchy.
    *** @return An array of dependent children/grandchildren DBFactories
    **/
    public DBFactory<? extends DBRecord<?>>[] getChildFactories()
    {
        if (this.childFactories == null) {
            this.childFactories = DBAdmin.getChildTableFactories(this);
        }
        return this.childFactories;
    }

    /**
    *** Get the "/" separated hierarchy of this table
    *** @return The "/" separated hierarchy of this table.
    **/
    public String getHierarchyString()
    {
        if (this.hierarchy == null) {
            synchronized (this.parentTables) {
                if (this.hierarchy == null) {
                    String utableName = this.getUntranslatedTableName();
                    String parentHier = StringTools.join(this.getParentTables(),"/");
                    this.hierarchy = (!StringTools.isBlank(parentHier)?(parentHier+"/"):"") + utableName;
                }
            }
        }
        return this.hierarchy; // never null
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the MySQL index type (engine)
    **/
    public String getIndexType()
    {
        if (this.indexType == null) {
            try {
                this.getMySQLTableStatus();
            } catch (DBException dbe) {
                // continue below
            }
            // "this.indexType" should be set (but possibly "?" if error)
            if (this.indexType == null) {
                // still null? set to "?"
                this._setIndexType("?");
            }
        }
        return this.indexType;
    }

    /**
    *** Sets the index type
    **/
    private void _setIndexType(String ndxType)
    {
        if (this.indexType == null) {
            synchronized (this.indexTypeLock) {
                if (this.indexType == null) {
                    this.indexType = ndxType;
                }
            }
        }
    }

    /**
    *** Returns true if this table is MySQL and "InnoDB"
    **/
    public boolean isMySQLInnoDB()
    {
        if (DBProvider.getProvider().getID() != DBProvider.DB_MYSQL) {
            return false;
        } else {
            return this.getIndexType().equalsIgnoreCase("InnoDB");
        }
    }

    /**
    *** Return true if this DBFactory suports efficient records countins.
    **/
    public boolean supportsEfficientCount()
    {
        // MySQL/InnoDB does not provide efficient record counting
        return !this.isMySQLInnoDB();
    }

    /**
    *** Gets Allow 'COUNT(*)' 
    **/
    public boolean getAllowInnoDBCOUNT()
    {
        return this.allowInnoDBCOUNT;
    }

    /**
    *** Gets Allow 'COUNT(*)' 
    **/
    public void setAllowInnoDBCOUNT(boolean countOK)
    {
        this.allowInnoDBCOUNT = countOK;
    }

    /**
    *** Returns the number of records  contained in the table represented by this DBFactory
    *** and based on the specified 'where' clause.
    *** @param where The 'where' selection clause
    *** @param actualCount True to return actual record count, false for estimated (InnoDB only)
    *** @return The number of records contained in the SQL table.
    *** @throws DBException   If a database error occurs
    **/
    public long getRecordCount(String where, boolean actualCount)
        throws DBException
    {

        /* invalid arguments */
        if (where == null) { // but "" is allowed
            return -1L;
        }

        /* return count */
        if (!StringTools.isBlank(where)) {
            // specific count based on 'where'
            return DBRecord.getRecordCount(new DBSelect<gDBR>(this, where)); // "unchecked call"
        } else
        if (actualCount) {
            // possibly non-efficient for InnoDB
            return DBRecord.getRecordCount(new DBSelect<gDBR>(this, "")); // "unchecked call"
        } else {
            // use table status, may be estimated count for InnoDB
            return this.getMySQLTableStatus();
        }

    }

    /**
    *** Returns the number of records  contained in the table represented by this DBFactory
    *** and based on the specified 'where' clause.
    *** @param where The 'where' selection clause
    *** @return The number of records contained in the SQL table.
    *** @throws DBException   If a database error occurs
    **/
    public long getRecordCount(DBWhere where, boolean actualCount)
        throws DBException
    {

        /* invalid arguments */
        if (where == null) {
            return -1L;
        }

        /* return count */
        if (actualCount) {
            // possibly non-efficient for InnoDB
            return DBRecord.getRecordCount(new DBSelect<gDBR>(this, where)); // "unchecked call"
        } else {
            // use table status, may be estimated count for InnoDB
            return this.getMySQLTableStatus();
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns an XML representation of this DBFactory
    *** @return An XML representation of this DBFactory
    **/
    public StringBuffer toXML(StringBuffer sb, int indent)
    {
        return this.toXML(sb, indent, false);
    }

    /**
    *** Returns an XML representation of this DBFactory
    *** @return An XML representation of this DBFactory
    **/
    public StringBuffer toXML(StringBuffer sb, int indent, boolean soapXML)
    {
        if (sb == null) { sb = new StringBuffer(); }
        String PFX1 = XMLTools.PREFIX(soapXML, indent);
        String PFX2 = XMLTools.PREFIX(soapXML, 2*indent);

        /* begin DBFactory tag */
        String utableName = this.getUntranslatedTableName();
        sb.append(PFX1);
        sb.append(XMLTools.startTAG(soapXML,DBFactory.TAG_TableSchema,
            XMLTools.ATTR(DBFactory.ATTR_table,utableName),
            false,true));
        
        /* description */
        sb.append(PFX2);
        sb.append(XMLTools.startTAG(soapXML,TAG_Description,"",false,false));
        sb.append(XMLTools.CDATA(soapXML,this.getDescription(null)));
        sb.append(XMLTools.endTAG(soapXML,TAG_Description,true));

        /* fields */            
        DBField fld[] = this.getFields();
        for (int i = 0; i < fld.length; i++) {
            DBFactory.writeXML_DBField(sb, 2*indent, fld[i], true/*inclInfo*/, null/*value*/, soapXML);
        }

        /* end Record tag */
        sb.append(PFX1);
        sb.append(XMLTools.endTAG(soapXML,DBFactory.TAG_TableSchema,true));
        return sb;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this DBFactory (the table name)
    *** @return The table name of this DBFactory
    **/
    public String toString()
    {
        return this.getUntranslatedTableName();
    }
    
    /**
    *** Returns true if this DBFactory is equal to the specified object. 
    *** (DBFactories are considered equivalent if their table names are equivalent)
    *** @param other  The other Object to test
    *** @return True if the 'other' Object is equals to this DBFactory
    **/
    public boolean equals(Object other)
    {
        if (other instanceof DBFactory) {
            return this.toString().equals(other.toString());
        } else {
            return false;
        }
    }
    
    /**
    *** Returns this hashcode for this DBFactory
    *** @return The hascode for this DBFactory
    **/
    public int hashCode()
    {
        return this.getUntranslatedTableName().hashCode();
    }

    // ------------------------------------------------------------------------
    // DBRecordListener interface
    
    /**
    *** Sets the DBRecordListnener for this DBFactory
    *** @param rlClassName  The DBRecordListener class name
    **/
    @SuppressWarnings("unchecked")
    public void setRecordListener(String rlClassName)
    {
        String utn = this.getUntranslatedTableName();

        /* clear? */
        if (StringTools.isBlank(rlClassName)) {
            this.setRecordListener((DBRecordListener<gDBR>)null);
            return;
        }

        /* load class */
        Class<?> rlClass;
        try {
            rlClass = Class.forName(rlClassName);
            if (!DBRecordListener.class.isAssignableFrom(rlClass)) {
                Print.logError("Not a DBRecordListener class: " + rlClassName);
                return;
            }
        } catch (ClassNotFoundException cnfe) {
            Print.logError("DBRecordListener("+utn+") class not found: " + rlClassName);
            return;
        } catch (Throwable th) {
            Print.logException("Error instantiating DBRecordListener("+utn+") class", th);
            return;
        }

        /* instantiate/set */
        DBRecordListener<gDBR> rcdListner;
        try {
            Print.logDebug("DBRecordListener("+utn+"): " + StringTools.className(rlClass));
            rcdListner = (DBRecordListener<gDBR>)rlClass.newInstance(); // Unchecked cast
        } catch (ClassCastException cce) {
            Print.logError("Not a DBRecordListener class: " + rlClassName);
            return;
        } catch (Throwable th) {
            Print.logException("Error instantiating DBRecordListener("+utn+") class", th);
            return;
        }

        /* set */
        this.setRecordListener(rcdListner);

    }

    /**
    *** Sets the DBRecordListnener for this DBFactory
    *** @param rcdListener  The DBRecordListener
    **/
    public void setRecordListener(DBRecordListener<gDBR> rcdListener)
    {
        this.recordListener = rcdListener;
    }
    
    /**
    *** Gets the DBRecordListnener for this DBFactory
    *** @return The DBRecordListener
    **/
    public DBRecordListener<gDBR> getRecordListener()
    {
        return this.recordListener;
    }

    /**
    *** Callback when record is about to be inserted into the table
    *** @param rcd  The record about to be inserted
    **/
    public void recordWillInsert(gDBR rcd)
    {
        if (this.recordListener != null) {
            this.recordListener.recordWillInsert(rcd);
        }
    }

    /**
    *** Callback after record has been be inserted into the table
    *** @param rcd  The record that was just inserted
    **/
    public void recordDidInsert(gDBR rcd)
    {
        if (this.recordListener != null) {
            this.recordListener.recordDidInsert(rcd);
        }
    }

    /**
    *** Callback when record is about to be updated in the table
    *** @param rcd  The record about to be updated
    **/
    public void recordWillUpdate(gDBR rcd)
    {
        if (this.recordListener != null) {
            this.recordListener.recordWillUpdate(rcd);
        }
    }

    /**
    *** Callback after record has been be updated in the table
    *** @param rcd  The record that was just updated
    **/
    public void recordDidUpdate(gDBR rcd)
    {
        if (this.recordListener != null) {
            this.recordListener.recordDidUpdate(rcd);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Returns the BeanMethod name for the specified field name
    *** @param prefix  The "get" or "set" prefix
    *** @param fieldName  The field name
    *** @return The bean-method nane
    **/
    protected static String _beanMethodName(String prefix, String fieldName)
    {
        StringBuffer sb = new StringBuffer(prefix);
        sb.append(fieldName.substring(0,1).toUpperCase());
        sb.append(fieldName.substring(1));
        return sb.toString();
    }
    
    /**
    *** Returns the String representation of the scope 'modifications'
    *** @param mods  The method modifications
    *** @return The String representation of the scope
    **/
    protected static String _methodScope(int mods)
    {
        if ((mods & Modifier.PUBLIC) == 1) {
            return "public";
        } else
        if ((mods & Modifier.PROTECTED) == 1) {
            return "protected";
        } else
        if ((mods & Modifier.PRIVATE) == 1) {
            return "private";
        } else {
            return "package";
        }
    }
    
    /**
    *** Validate the specified DBField for proper getter/setter bean access methods
    *** @param field  The DBField to validate
    *** @return A list of error message Strings, or null if no errors were encountered
    **/
    public java.util.List<String> validateFieldBeanMethods(DBField field)
    {
        Class<gDBR> tableClass = this.getRecordClass();
        String fldName = field.getName();
        Class<?> typeClass = field.getTypeClass();

        /* header */
        boolean ok = true;
        java.util.List<String> errMsg = new Vector<String>();

        /* check getter */
        String getMethN  = _beanMethodName("get", fldName);
        Method getMethod = null;
        for (Class<?> target = tableClass; target != null ; target = target.getSuperclass()) {
            try {
                getMethod = target.getDeclaredMethod(getMethN, new Class<?>[0]);
                break;
            } catch (NoSuchMethodException nsme) {
                // ignore and try again on next iteration
            }
        }
        if (getMethod != null) {
            Class<?> rtnClass = getMethod.getReturnType();
            if (!rtnClass.equals(typeClass)) {
                errMsg.add("Invalid getter return type: " + rtnClass.getName() + " [expected " + StringTools.className(typeClass) + "]");
                ok = false;
            }
            int mods = getMethod.getModifiers();
            if ((mods & Modifier.PUBLIC) == 0) {
                //errMsg.add("Invalid getter scope: " + _methodScope(mods));
                //ok = false;
            }
        } else {
            errMsg.add("Getter not found");
            ok = false;
        }

        /* check setter */
        boolean setFound = false;
        String setMethN  = _beanMethodName("set", fldName);
        Method setMethod = null;
        for (Class<?> target = tableClass; target != null ; target = target.getSuperclass()) {
            try {
                setMethod = target.getDeclaredMethod(setMethN, new Class<?>[] { typeClass });
                break;
            } catch (NoSuchMethodException nsme) {
                // ignore and try again on next iteration
            }
        }
        if (setMethod != null) {
            Class<?> rtnClass = setMethod.getReturnType();
            if (!rtnClass.equals(Void.TYPE)) {
                errMsg.add("Invalid setter return type: " + rtnClass.getName() + " [expected void]");
                ok = false;
            }
            int mods = setMethod.getModifiers();
            if ((mods & Modifier.PUBLIC) == 0) {
                //errMsg.add("Invalid setter scope: " + _methodScope(mods));
                //ok = false;
            }
        } else {
            errMsg.add("Setter not found");
            ok = false;
        }
        
        /* ok? */
        return ok? null : errMsg;
        
    }

    /**
    *** Validates all bean-access methods for this DBFactory
    **/
    public void validateTableBeanMethods()
    {
        String utableName = this.getUntranslatedTableName();
        Class<gDBR> tableClass = this.getRecordClass();
        DBField field[]  = this.getFields();
        
        Print.logInfo("");
        Print.logInfo("Validating bean access methods for table: " + utableName);
        
        for (int i = 0; i < field.length; i++) {
            String fieldName = field[i].getName();
            String className = StringTools.className(field[i].getTypeClass());
            Print.logInfo("  Field: " + fieldName + " (type=" + className + ")");
            java.util.List<String> errMsg = this.validateFieldBeanMethods(field[i]);
            if ((errMsg == null) || errMsg.isEmpty()) {
                Print.logInfo("    OK");
            } else {
                for (Iterator<String> e = errMsg.iterator(); e.hasNext();) {
                    Print.logInfo("    " + e.next());
                }
            }
        }
        
        Print.logInfo("");
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Create a list of XML DBField description tags with values
    *** @param sb The StringBuffer to write the tag to
    *** @param indent The number of spaces to indent
    *** @param fld The DBFields to include
    *** @param fldVals The field column values
    *** @return The StringBuffer
    **/
    public static StringBuffer writeXML_DBFields(StringBuffer sb, int indent, DBField fld[], DBFieldValues fldVals)
    {
        return DBFactory.writeXML_DBFields(sb, indent, fld, fldVals, false);
    }
    
    /**
     *** Create XML DBField description XML (entity reference encoded)
     *** @param sb      The StringBuffer to write the tag to
     *** @param indent  The number of spaces to indent
     *** @param fld The DBFields to include
     *** @param fldVals The field column values
     *** @param soapXML True if SOAP XML
     *** @return The StringBuffer
     **/
     public static StringBuffer writeXML_DBFields(StringBuffer sb, int indent, DBField fld[], DBFieldValues fldVals, boolean soapXML)
     {
         for (int i = 0; i < fld.length; i++) {
             String name  = fld[i].getName();
             String value = (fldVals != null)? fldVals.getFieldValueAsString(name) : null;
             DBFactory.writeXML_DBField(sb, indent, fld[i], false/*inclInfo*/, value, soapXML);
         }
         return sb;
     }

    /**
    *** Create an XML DBField description tag
    *** @param sb The StringBuffer to write the tag to
    *** @param indent The number of spaces to indent
    *** @param fld The DBField to create a tag for
    *** @param inclInfo True if field datatype/description should be included
    *** @param value The value of the tag element
    *** @return The StringBuffer
    **/
    public static StringBuffer writeXML_DBField(StringBuffer sb, int indent, DBField fld, boolean inclInfo, String value)
    {
        return DBFactory.writeXML_DBField(sb, indent, fld, inclInfo, value, false/*soapXML*/);
    }

    /**
    *** Create an XML DBField description tag
    *** @param sb       The StringBuffer to write the tag to
    *** @param indent   The number of spaces to indent
    *** @param fld      The DBField to create a tag for
    *** @param inclInfo True if field datatype/description should be included
    *** @param value    The value of the tag element
    *** @param soapXML  True is SOAP XML
    *** @return The StringBuffer comtaining the XML
    **/
    public static StringBuffer writeXML_DBField(StringBuffer sb, int indent, DBField fld, boolean inclInfo, String value, boolean soapXML)
    {
        // -- <Field name="NAME" primaryKey="true" alternateKeys="A,B,C" type="DATATYPE", title="TITLE">VALUE</FIELD>

        /* begin Field tag */
        sb.append(XMLTools.PREFIX(soapXML,indent));        
        sb.append(XMLTools.startTAG(soapXML,TAG_Field,
            XMLTools.ATTR(ATTR_name,fld.getName()) +
            (fld.isPrimaryKey()? XMLTools.ATTR(ATTR_primaryKey,"true") : "") + 
            (fld.isAlternateKey()? XMLTools.ATTR(ATTR_alternateKeys,StringTools.join(fld.getAlternateIndexes(),',')) : "") + 
            (inclInfo? (XMLTools.ATTR(ATTR_type,fld.getDataType()) + XMLTools.ATTR(ATTR_title,fld.getTitle(null))) : ""),
            (value == null), (value == null)));

        /* valid */
        if (value != null) {

            /* value */
            if (fld.isBoolean()) {
                // "true" / "false"
                sb.append(StringTools.parseBoolean(value,false));
            } else
            if (fld.isNumeric()) {
                // numeric value
                sb.append(value);
            } else
            if (fld.isBinary()) {
                // displayed in hex
                sb.append(value);
            } else { 
                // String
                if (!StringTools.isBlank(value)) {
                    sb.append(XMLTools.CDATA(soapXML,value));
                }
            }
        
            /* end Field tag */
            sb.append(XMLTools.endTAG(soapXML,TAG_Field,true));
            
        }
        
        /* field xml */
        //Print.logInfo("==> " + sb.toString());
        return sb;

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse and return a DBFactory from the specified XML node atribute "table".
    *** The specified node is expected have a "table" attribute.
    *** @param node  The XML node 
    *** @param nodeNames  A list of valid/expected node names
    *** @return The DBFactory
    *** @throws DBException if unable to create the DBFactory
    **/
    public static DBFactory<? extends DBRecord<?>> parseXML_DBFactory(Element node, String... nodeNames)
        throws DBException
    {

        /* validate node */
        if (node == null) {
            throw new DBException("Node tag element is null");
        }
        
        /* validate node names */
        if (!ListTools.isEmpty(nodeNames)) {
            String name = node.getTagName();
            if (!ListTools.containsIgnoreCase(nodeNames, name)) {
                throw new DBException("Invalid Node name: " + name);
            }
        }

        /* table name */
        String utableName = XMLTools.getAttribute(node, ATTR_table, null, false);
        if (StringTools.isBlank(utableName)) {
            throw new DBException("Table name is blank");
        }

        /* DBFactory */
        DBFactory<? extends DBRecord<?>> tableFact = DBFactory.getFactoryByName(utableName);
        if (tableFact == null) {
            // table not found
            throw new DBException("Table name not found: " + utableName);
        }
        return tableFact;

    }

    /**
    *** Parse and return a field value map from "Field" child node of the specified XML node element.
    *** @param node  The XML node 
    *** @return The field value map
    *** @throws DBException if unable to create the field value map
    **/
    public static <T extends DBRecord<T>> Map<String,String> parseXML_FieldValueMap(Element node, DBFactory<T> tableFact)
        throws DBException
    {

        /* validate node */
        if (node == null) {
            throw new DBException("Node tag element is null");
        }

        /* value map container */
        Map<String,String> valueMap = new OrderedMap<String,String>();

        /* parse fields */
        NodeList fieldList = XMLTools.getChildElements(node,TAG_Field);
        for (int f = 0; f < fieldList.getLength(); f++) {
            Element field = (Element)fieldList.item(f);
            String  name  = XMLTools.getAttribute(field,ATTR_name,null,false);
            //Print.logInfo("Parsing field: " + name);
            if (StringTools.isBlank(name)) {
                // blank field name
                Print.logWarn("Specified field name is null/blank");
            } else
            if (!tableFact.hasField(name)) {
                // invalid field name
                Print.logWarn("Field does not exist in DBFactory: " + name);
            } else {
                String  type    = XMLTools.getAttribute(field, ATTR_type, null, false);
                String  priKey  = XMLTools.getAttribute(field, ATTR_primaryKey, null, false);
                String  altKeys = XMLTools.getAttribute(field, ATTR_alternateKeys, null, false);
                String  value   = XMLTools.getNodeText( field, "\\n", false, "");
                valueMap.put(name,value);
            }
        }
        
        /* value map */
        return valueMap;
    }

    /**
    *** Parse and return a DBRecordKey from the specified XML node.
    *** The specified node is expected to be a "RecordKey" tag
    *** @param rcdTag  The XML node representing a "RecordKey" tag
    *** @return The DBRecordKey
    *** @throws DBException if unable to create the DBRecordKey
    **/
    @SuppressWarnings("unchecked")
    public static DBRecordKey<? extends DBRecord<?>> parseXML_DBRecordKey(Element rcdTag)
        throws DBException
    {
        DBFactory<? extends DBRecord<?>> tableFact = DBFactory.parseXML_DBFactory(rcdTag, TAG_Record, TAG_RecordKey);
        boolean isRecordKey = rcdTag.getTagName().equalsIgnoreCase(TAG_RecordKey);

        /* key type */
        int keyType = DBWhere.KEY_FULL;
        String partialKey = XMLTools.getAttribute(rcdTag, ATTR_partial, null, false);
        if (StringTools.isBlank(partialKey) || partialKey.equalsIgnoreCase("full")) {
            // all key fileds must be present
            keyType = DBWhere.KEY_FULL;
        } else
        if (partialKey.equalsIgnoreCase("first")) {
            // leading key fields must be present
            keyType = DBWhere.KEY_PARTIAL_FIRST;
        } else
        if (partialKey.equalsIgnoreCase("true") || partialKey.equalsIgnoreCase("all")) {
            // all key fields are optional
            keyType = DBWhere.KEY_PARTIAL_ALL_EMPTY;
        } else
        if (partialKey.equalsIgnoreCase("auto") || partialKey.equalsIgnoreCase("autoIndex")) {
            // all key fields are optional
            keyType = DBWhere.KEY_AUTO_INDEX;
        } else {
            // all key fileds must be present
            Print.logWarn("Unrecognized 'partial' attribute value: %s", partialKey);
            keyType = DBWhere.KEY_FULL;
        }

        /* parse fields */
        Map<String,String> valueMap = DBFactory.parseXML_FieldValueMap(rcdTag, tableFact);
        
        /* create key */
        DBRecordKey<? extends DBRecord<?>> rcdKey = null;
        try {
            rcdKey = tableFact.createKey(valueMap, keyType); // may throw DBException
        } catch (DBException dbe) {
            // any special checks?
            throw dbe;
        }

        /* find fields that are not primary keys */
        Set<String> dataFlds = null;
        for (String fldName : valueMap.keySet()) {
            DBField fld = tableFact.getField(fldName);
            if (fld == null) {
                // should not be null, ignore for now
            } else
            if (!fld.isPrimaryKey()) {
                if (dataFlds == null) { dataFlds = new HashSet<String>(); }
                dataFlds.add(fldName);
            }
        }
        rcdKey.setTaggedFieldNames(dataFlds);

        /* return key */
        return rcdKey;

    }

    /**
    *** Parse and return a DBRecord from the specified XML node.
    *** The specified node is expected to be a "Record" tag
    *** @param rcdTag  The XML node representing a "Record" tag
    *** @return The DBRecord
    *** @throws DBException if unable to create the DBRecord
    **/
    @SuppressWarnings("unchecked")
    public static DBRecord<?> parseXML_DBRecord(Element rcdTag)
        throws DBException
    {
        DBFactory<? extends DBRecord<?>> tableFact = DBFactory.parseXML_DBFactory(rcdTag, TAG_Record);
        Map<String,String> valueMap = DBFactory.parseXML_FieldValueMap(rcdTag, tableFact);
        return tableFact.createRecord(valueMap); // may throw DBException
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final int INCL_TABLE     = 0x0001;
    public static final int INCL_BLANK     = 0x0002;
    public static final int INCL_TYPE      = 0x0004;
    public static final int INCL_TITLE     = 0x0008;
    public static final int INCL_KEY       = 0x0010;
    
    // - INCL_TABLE: will include a "table" field entry and separate "fields" object
    //   entry in the following format:
    //     {
    //        "table": "EventData",
    //        "fields": {
    //           ...
    //        }
    //     }
    // - INCL_BLANK: will include String fields that have a blank value, and
    //   numeric fields that have a zero value.
    // - INCL_TYPE: will include the field datatype:
    //     ...
    //     "timestamp": {
    //        "type": "UINT32",
    //        "value": 1406876461
    //     },
    //     ...
    // - INCL_TITLE: will include the field title:
    //     ...
    //     "timestamp": {
    //        "title": "Event Timestamp",
    //        "type": "UINT32",
    //        "value": 1406876461
    //     },
    //     ...
    // - INCL_KEY: will include primary or alternate key information:
    //     ...
    //     "timestamp": {
    //        "title": "Event Timestamp",
    //        "type": "UINT32",
    //        "value": 1406876461
    //     },
    //     ...
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     *** Create XML DBField description XML (entity reference encoded)
     *** @param fld The DBFields to include
     *** @param fldVals The field column values
     *** @return The StringBuffer
     **/
    public static JSON._Object writeJSON_DBFields(DBField fld[], int inclMask, DBFieldValues fldVals)
    {
        JSON._Object obj = new JSON._Object();
        for (int i = 0; i < fld.length; i++) {
            String name  = fld[i].getName();
            String value = (fldVals != null)? fldVals.getFieldValueAsString(name) : null;
            obj.addKeyValue(DBFactory.writeJSON_DBField(fld[i], inclMask, value));
        }
        return obj;
    }

    /**
    *** Create an XML DBField description tag
    *** @param fld      The DBField to create a tag for
    *** @param inclInfo True if field datatype/description should be included
    *** @param value    The value of the tag element
    *** @return The StringBuffer comtaining the XML
    **/
    public static JSON._KeyValue writeJSON_DBField(DBField fld, int inclMask, Object value)
    {
        // -- "name" : {
        // -      "primaryKey" : true,
        // -      "alternateKeys" : [ "a", "b", "c" ],
        // -      "type" : "DATATYPE",
        // -      "title" : "TITLE",
        // -      "value" : VALUE
        // -  }

        /* what to include */
        boolean inclType     = ((inclMask & DBFactory.INCL_TYPE ) != 0);
        boolean inclTitle    = ((inclMask & DBFactory.INCL_TITLE) != 0);
        boolean inclKey      = ((inclMask & DBFactory.INCL_KEY  ) != 0);

        /* JSON field headers */
        JSON._Object obj = null;
        if (inclKey && fld.isPrimaryKey()) {
            if (obj == null) { obj = new JSON._Object(); }
            obj.addKeyValue(ATTR_primaryKey, true);
        }
        if (inclKey && fld.isAlternateKey()) {
            if (obj == null) { obj = new JSON._Object(); }
            String ak[] = fld.getAlternateIndexes();
            obj.addKeyValue(ATTR_alternateKeys, new JSON._Array(ak));
        }
        if (inclType) {
            if (obj == null) { obj = new JSON._Object(); }
            obj.addKeyValue(ATTR_type, fld.getDataType());
        }
        if (inclTitle) {
            if (obj == null) { obj = new JSON._Object(); }
            obj.addKeyValue(ATTR_title, fld.getTitle(null));
        }

        /* JSON field value */
        Object val;
        if (value == null) {
            val = null;
        } else
        if (fld.isBoolean()) {
            // -- Boolean
            val = (value instanceof Boolean)? value : new Boolean(StringTools.parseBoolean(value,false));
        } else
        if (fld.isDecimal()) {
            // -- Double, Float
            val = (value instanceof Double)? value : new Double(StringTools.parseDouble(value,0.0));
        } else
        if (fld.isNumeric()) {
            // -- Integer, Long, Short
            val = (value instanceof Long)? value : new Long(StringTools.parseLong(value,0L));
        } else
        if (fld.isBinary()) {
            // -- Binary
            if (value instanceof String) {
                // -- contents is already hex binary representation?
                byte b[] = StringTools.parseHex((String)value, null);
                if (!ListTools.isEmpty(b)) {
                    val = "0x" + StringTools.toHexString(b);
                } else {
                    val = "";
                }
            } else
            if (value instanceof byte[]) {
                // -- byte array
                byte b[] = (byte[])value;
                if (!ListTools.isEmpty(b)) {
                    val = "0x" + StringTools.toHexString(b);
                } else {
                    val = "";
                }
            } else {
                // -- not supported
                val = "";
            }
        } else
        if (fld.isString()) {
            // -- String
            if (value instanceof String) {
                val = (String)value;
            } else {
                val = StringTools.toString(value);
            }
        } else { 
            // -- unknown
            val = StringTools.toString(value);
        }

        /* return JSON._KeyValue */
        if (obj != null) {
            obj.addKeyValue(ATTR_value, val);
            return new JSON._KeyValue(fld.getName(), obj);
        } else {
            return new JSON._KeyValue(fld.getName(), val);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /*
    *** Creates XML for Hibernate support
    *** (Hibernate support is not yet fully supported.  This method should not be used)
    **/
    /*
    public void createHibernateXML()
    {
        // currently experimental purposes only
        String utableName = this.getUntranslatedTableName();
        Class<gDBR> tableClass = this.getRecordClass();
        Class keyClass   = this.getKeyClass();
        DBField key[]    = this.getKeyFields();
        DBField field[]  = this.getFields();
        StringBuffer sb  = new StringBuffer();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n");
        sb.append("<hibernate-mapping>\n");
        sb.append("  <class name=\"" + tableClass + "\" table=\"" + utableName + "\">\n");
        if (key.length > 0) {
            sb.append("\n");
            sb.append("    <composite-id name=\"key\" class=\"" + keyClass + "\">\n");
            for (int i = 0; i < key.length; i++) {
                String fldName   = key[i].getName();
                Class  typeClass = key[i].getTypeClass();
                String hibType   = typeClass.getName(); // key[i].getHibernateType();
                int    typeLen   = key[i].getLength();
                String hibLen    = (typeLen > 0)? ("length=\"" + typeLen + "\"") : "";
                String title     = key[i].getTitle(null);
                sb.append("\n");
                sb.append("      <!-- " + title + "-->\n");
                sb.append("      <key-property name=\"" + fldName + "\" type=\"" + hibType + "\" " + hibLen + "/>\n");
            }
            sb.append("\n");
            sb.append("    </composite-id>\n");
        }
        for (int i = 0; i < field.length; i++) {
            boolean isPriKey = field[i].isPrimaryKey();
            if (!isPriKey) {
                String fldName   = field[i].getName();
                Class  typeClass = field[i].getTypeClass();
                String hibType   = typeClass.getName(); // field[i].getHibernateType();
                int    typeLen   = field[i].getLength();
                String hibLen    = (typeLen > 0)? ("length=\"" + typeLen + "\"") : "";
                String title     = field[i].getTitle(null);
                sb.append("\n");
                sb.append("    <!-- " + title + "-->\n");
                sb.append("    <property  name=\"" + fldName + "\" type=\"" + hibType + "\" column=\"" + fldName + "\" " + hibLen + ">\n");
            }
        }
        sb.append("\n");
        sb.append("  </class>\n");
        sb.append("</hibernate-mapping>\n");
        File xmlFile = new File("./" + utableName + ".hbm.xml");
        try {
            FileTools.writeFile(StringTools.getBytes(sb), xmlFile);
        } catch (IOException ioe) {
            Print.logError("Unable to write file: " + xmlFile + " [" + ioe + "]");
        }
    }
    */
    
    // ------------------------------------------------------------------------

}
