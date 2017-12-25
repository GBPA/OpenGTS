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
// Eventually, this module should contain all database specific attributes, so that
// only this module need be changed in order to support another database provider.
// ----------------------------------------------------------------------------
// UTF-8 Encoding:
//  MySQL: 
//      SHOW CHARACTER SET;
//      SHOW FULL COLUMNS FROM <table>;  // new column "Collation"
//      ALTER TABLE <table> CONVERT TO CHARACTER SET utf8 COLLATE utf8_general_ci
//      ALTER TABLE <table> MODIFY <column> TEXT       CHARACTER SET utf8;
//      ALTER TABLE <table> MODIFY <column> VARCHAR(M) CHARACTER SET utf8;
// ----------------------------------------------------------------------------
// Change History:
//  2007/09/16  Martin D. Flynn
//     -Initial release
//  2007/11/28  Martin D. Flynn
//     -Added several data type methods:
//      'isDataTypeUnsigned(...)', 'isDataTypeNumeric(...)', 'isDataTypeString(...)', ...
//  2007/12/13  Martin D. Flynn
//     -Update 'updateRecordInTable' to display a warning when updating a primary key
//  2008/01/10  Martin D. Flynn
//     -Added "SUM(x)" field
//  2008/02/27  Martin D. Flynn
//     -Added 'isDataTypeDecimal' method
//     -Fixed '_getTypeIndex' to properly check for 'STRING[...]' types
//  2008/03/12  Martin D. Flynn
//     -Added 'supportsOffset' method
//     -Added separate 'createAlternateIndex' method
//  2008/04/11  Martin D. Flynn
//     -Added additional error message when JDBC driver is not found.
//  2008/05/14  Martin D. Flynn
//     -Added support for creating multiple alternate indexes
//  2008/12/01  Martin D. Flynn
//     -Added support MySQL InnoDB indexes.
//  2009/01/01  Martin D. Flynn
//     -Modified "getDBUri(...)" to check for new runtime property RTKey.DB_URL_DB
//      when needing to get a db connection URL which includes the db-name.
//     -Changed DB_CONNECTION[_DB] to DB_URL[_DB].
//  2009/01/28  Martin D. Flynn
//     -Added changes to support UTF8 character sets
//  2009/04/02  Martin D. Flynn
//     -Fixed to add "CHARACTER SET utf8" to specific fields upon initial table
//      creation (previously required an additional call to "dbAdmin -tables=ca").
//  2009/05/01  Martin D. Flynn
//     -Added DateTime datatype
//  2010/07/04  Martin D. Flynn
//     -Additional changes for PostgreSQL support (not yet tested)
//  2010/10/21  Martin D. Flynn
//     -Changed MySQL "type=" to "engine="
//  2012/01/29  Martin D. Flynn
//     -PostgreSQL support added (by Gaurav Kohli)
//  2012/04/11  Martin D. Flynn
//     -Added invalid SQL display on error in "updateRecordInTable"
//  2012/10/24  Martin D. Flynn
//     -Changed SQL Server data types SQL_INT8[smallint], SQL_UINT8[smallint]
//     -Changed SQL Server data types SQL_INT16[int], SQL_UINT16[int]
//  2013/02/06  Martin D. Flynn
//     -Fixed "getDataTypeFromSqlType" to also check for boolean types
//  2013/08/27  Martin D. Flynn
//     -Added support for "DBField.isUpdateAllowed()"
//     -Added support for obtaining SQLServer alternate keys (see SQLServer_AlternateKeys)
//  2012/09/20  Martin D. Flynn
//     -Added "ALTER ROLE [db_owner] ..." to MS-SQLServer db init.
//  2014/02/17  Martin D. Flynn
//     -SQLServer "ALTER ROLE" error is no longer fatal (displays error and continues)
//  2014/11/19  Martin D. Flynn
//     -Added additional error messages when loading MySQL JDBC driver [2.5.8-B17]
//  2017/06/16  Martin D. Flynn
//     -Added support for Insert/Update "IGNORE" (see "getIgnoreOnInsert"/"getIgnoreOnUpdate")
//     -Added "replaceRecordIntoTable"
// ----------------------------------------------------------------------------
package org.opengts.dbtools;

import java.lang.*;
import java.lang.reflect.*;
import java.io.File;
import java.util.*;
import java.sql.*;

import org.opengts.util.*;

/**
*** <code>DBProvider</code> specifies SQL database provider specific attributes.
**/

public class DBProvider
{

    // ------------------------------------------------------------------------

    public static final String      PRIMARY_INDEX_NAME      = "PRIMARY";    // MySQL
    public static final String      UNIQUE_INDEX_NAME       = "UNIQUE";     // MySQL
    public static final String      DEFAULT_ALT_INDEX_NAME  = "altIndex";

    // ------------------------------------------------------------------------

    // "like" wildcard character
    public static final String      LIKE_WILDCARD           = "%";
    
    // ------------------------------------------------------------------------

    // MySQL: "COUNT(*)"
    private static final String     FLD_count_              = "COUNT(*)";
    public static String FLD_COUNT() { return FLD_count_; }
    
    // MySQL: "SUM(<column>)"
    private static final String     FLD_sum_                = "SUM";
    public static String FLD_SUM(String col) { return "SUM(" + col + ")"; }

    // ------------------------------------------------------------------------

    public static final String      TYPE_UNKNOWN            = "UNKNOWN";
    public static final String      TYPE_BOOLEAN            = "BOOLEAN";
    public static final String      TYPE_INT8               = "INT8";
    public static final String      TYPE_UINT8              = "UINT8";
    public static final String      TYPE_INT16              = "INT16";
    public static final String      TYPE_UINT16             = "UINT16";
    public static final String      TYPE_INT32              = "INT32";
    public static final String      TYPE_UINT32             = "UINT32";
    public static final String      TYPE_INT64              = "INT64";
    public static final String      TYPE_UINT64             = "UINT64";
    public static final String      TYPE_FLOAT              = "FLOAT";
    public static final String      TYPE_DOUBLE             = "DOUBLE";
    public static final String      TYPE_SBLOB              = "SBLOB";      // [SMALL]BLOB (4095 bytes)
    public static final String      TYPE_BLOB               = "BLOB";       // BLOB (65535 bytes)
    public static final String      TYPE_MBLOB              = "MBLOB";      // MEDIUMBLOB (16777215 bytes)
    public static final String      TYPE_TEXT               = "TEXT";       // CLOB (65535 bytes)
    public static final String      TYPE_STRING             = "STRING";
    public static final String      TYPE_DATETIME           = "DATETIME";
    public static final String      TYPE_ARRAY[]            = new String[] {
        TYPE_UNKNOWN ,
        TYPE_BOOLEAN ,
        TYPE_INT8    ,
        TYPE_UINT8   ,
        TYPE_INT16   ,
        TYPE_UINT16  ,
        TYPE_INT32   ,
        TYPE_UINT32  ,
        TYPE_INT64   ,
        TYPE_UINT64  ,
        TYPE_FLOAT   ,
        TYPE_DOUBLE  ,
        TYPE_SBLOB   ,
        TYPE_BLOB    ,
        TYPE_MBLOB   ,
        TYPE_TEXT    ,
        TYPE_STRING  ,
        TYPE_DATETIME,
    };

    public static String TYPE_STRING(int size)
    { 
        return TYPE_STRING + "[" + size + "]"; 
    }

    public static String TYPE_INT(String val) 
    { 
        if (!StringTools.isBlank(val)) {
            if (val.equalsIgnoreCase("INT8"  )) { return TYPE_INT8;   }
            if (val.equalsIgnoreCase("UINT8" )) { return TYPE_UINT8;  }
            if (val.equalsIgnoreCase("INT16" )) { return TYPE_INT16;  }
            if (val.equalsIgnoreCase("UINT16")) { return TYPE_UINT16; }
            if (val.equalsIgnoreCase("INT32" )) { return TYPE_INT32;  }
            if (val.equalsIgnoreCase("UINT32")) { return TYPE_UINT32; }
            if (val.equalsIgnoreCase("INT64" )) { return TYPE_INT64;  }
            if (val.equalsIgnoreCase("UINT64")) { return TYPE_UINT64; }
        }
        Print.logStackTrace("ERROR: Invalid INTEGER type: " + val);
        return StringTools.trim(val); // <-- invalid integer type
    }

    public static final int         SQL_UNKNOWN             = 0;    // TYPE_UNKNOWN
    public static final int         SQL_BOOLEAN             = 1;    // TYPE_BOOLEAN
    public static final int         SQL_INT8                = 2;    // TYPE_INT8
    public static final int         SQL_UINT8               = 3;    // TYPE_UINT8
    public static final int         SQL_INT16               = 4;    // TYPE_INT16
    public static final int         SQL_UINT16              = 5;    // TYPE_UINT16
    public static final int         SQL_INT32               = 6;    // TYPE_INT32
    public static final int         SQL_UINT32              = 7;    // TYPE_UINT32
    public static final int         SQL_INT64               = 8;    // TYPE_INT64
    public static final int         SQL_UINT64              = 9;    // TYPE_UINT64
    public static final int         SQL_FLOAT               = 10;   // TYPE_FLOAT
    public static final int         SQL_DOUBLE              = 11;   // TYPE_DOUBLE
    public static final int         SQL_SBLOB               = 12;   // TYPE_SBLOB
    public static final int         SQL_BLOB                = 13;   // TYPE_BLOB
    public static final int         SQL_MBLOB               = 14;   // TYPE_MBLOB
    public static final int         SQL_TEXT                = 15;   // TYPE_TEXT
    public static final int         SQL_VARCHAR             = 16;   // TYPE_STRING
    public static final int         SQL_DATETIME            = 17;   // TYPE_DATETIME

    public static String SQL_VARCHAR(int size) { return SQL_VARCHAR + "(" + size + ")"; }

    // ------------------------------------------------------------------------
    
    public    static final int      DATATYPE_NONE           = 0x0000;
    public    static final int      DATATYPE_BOOLEAN        = 0x0001;
    public    static final int      DATATYPE_UNSIGNED       = 0x0002;
    public    static final int      DATATYPE_SIGNED         = 0x0004;
    public    static final int      DATATYPE_DECIMAL        = 0x0008;
    public    static final int      DATATYPE_STRING         = 0x0010;
    public    static final int      DATATYPE_BINARY         = 0x0020;
    public    static final int      DATATYPE_DATETIME       = 0x0040;

    public    static final int      DATATYPE_NUMERIC        = DATATYPE_UNSIGNED | DATATYPE_SIGNED | DATATYPE_DECIMAL;

    public    static final int      DATATYPE_ARRAY[]        = new int[] {
        DATATYPE_NONE    ,  // TYPE_UNKNOWN ,
        DATATYPE_BOOLEAN ,  // TYPE_BOOLEAN ,
        DATATYPE_SIGNED  ,  // TYPE_INT8    ,
        DATATYPE_UNSIGNED,  // TYPE_UINT8   ,
        DATATYPE_SIGNED  ,  // TYPE_INT16   ,
        DATATYPE_UNSIGNED,  // TYPE_UINT16  ,
        DATATYPE_SIGNED  ,  // TYPE_INT32   ,
        DATATYPE_UNSIGNED,  // TYPE_UINT32  ,
        DATATYPE_SIGNED  ,  // TYPE_INT64   ,
        DATATYPE_UNSIGNED,  // TYPE_UINT64  ,
        DATATYPE_DECIMAL ,  // TYPE_FLOAT   ,
        DATATYPE_DECIMAL ,  // TYPE_DOUBLE  ,
        DATATYPE_BINARY  ,  // TYPE_SBLOB   ,
        DATATYPE_BINARY  ,  // TYPE_BLOB    ,
        DATATYPE_BINARY  ,  // TYPE_MBLOB   ,
        DATATYPE_STRING  ,  // TYPE_TEXT    ,
        DATATYPE_STRING  ,  // TYPE_STRING  ,
        DATATYPE_DATETIME,  // TYPE_DATETIME,
    };

    // ------------------------------------------------------------------------

    public    static final int      DB_MYSQL                = 1; // MySQL:
    public    static final int      DB_POSTGRESQL           = 2; // PostgreSQL: 
    public    static final int      DB_DERBY                = 3; // Derby:      not fully supported!
    public    static final int      DB_SQLSERVER            = 4; // SQLServer:
    public    static final int      DB_ORACLEXE             = 5; // OracleXE:   not fully supported!
    public    static final int      DB_MARIADB              = 6; // MariaDB: plug compatible with MySQL

    // ------------------------------------------------------------------------

    public    static final long     FLAGS_NONE              = 0x0000000000000000L;
    public    static final long     FLAGS_LIMIT             = 0x0000000000000001L;
    public    static final long     FLAGS_OFFSET            = 0x0000000000000002L;

    // ------------------------------------------------------------------------

    private static class NameFilter
    {
        private String prefix = null;
        public NameFilter() {
            super();
        }
        public NameFilter(String pfx) {
            this.prefix = !StringTools.isBlank(pfx)? this.adjustCase(pfx) : null;
        }
        public String name(String name) {
            String n = this.adjustCase(name);
            if (n == null) {
                return "";
            } else
            if ((this.prefix == null) || this.prefix.equals("")) {
                return n;
            } else {
                return n.startsWith(this.prefix)? n : (this.prefix + n);
            }
        }
        protected String adjustCase(String n) {
            return n;
        }
    }

    private static class NameFilterUpperCase
        extends NameFilter
    {
        public NameFilterUpperCase() {
            super();
        }
        public NameFilterUpperCase(String pfx) {
            super(pfx);
        }
        protected String adjustCase(String n) {
            return (n != null)? n.toUpperCase() : n;
        }
    }

    private static class NameFilterLowerCase
        extends NameFilter
    {
        public NameFilterLowerCase() {
            super();
        }
        public NameFilterLowerCase(String pfx) {
            super(pfx);
        }
        protected String adjustCase(String n) {
            return (n != null)? n.toLowerCase() : n;
        }
    }

    // ------------------------------------------------------------------------
    // MySQL drivers
    // Example configuration properties:
    //   db.sql.provider=mysql
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=3306
    //   db.sql.user=gts
    //   db.sql.password=opengts
    //   db.sql.url=jdbc:mysql://localhost:${db.sql.port}/
    // ------------------------------------------------------------------------
    // Fully supported
    // ------------------------------------------------------------------------
    // Porting to InnoDB:
    //  - http://www.linux.com/articles/46370

    public static final String  MySQL_Old_Name     = "mysql_old";
    protected static DBProvider MySQL_Old_Provider = new DBProvider(
        MySQL_Old_Name, DB_MYSQL, 3306,
        new String[] { "com.mysql.jdbc.Driver", "org.gjt.mm.mysql.Driver", "org.mariadb.jdbc.Driver" },
        null,                                       // column name filter
        null,                                       // index name filter
        null,                                       // table name filter
        "type=MyISAM",                              // "CREATE TABLE" suffix
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^12 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    public static final String  MySQL_MyISAM_Name     = "mysql_myisam";
    protected static DBProvider MySQL_MyISAM_Provider = new DBProvider(
        MySQL_MyISAM_Name, DB_MYSQL, 3306,
        new String[] { "com.mysql.jdbc.Driver", "org.gjt.mm.mysql.Driver", "org.mariadb.jdbc.Driver" },
        null,                                       // column name filter
        null,                                       // index name filter
        null,                                       // table name filter
        "engine=MyISAM",                            // "CREATE TABLE" suffix
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^12 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // -- "SELECT COUNT(*) FROM table" is slow for "InnoDB":
    // --  http://www.mysqlperformanceblog.com/2006/12/01/count-for-innodb-tables/
    public static final String  MySQL_InnoDB_Name     = "mysql_innodb";
    protected static DBProvider MySQL_InnoDB_Provider = new DBProvider(
        MySQL_InnoDB_Name, DB_MYSQL, 3306,
        new String[] { "com.mysql.jdbc.Driver", "org.gjt.mm.mysql.Driver", "org.mariadb.jdbc.Driver" },
        null,                                       // column name filter
        null,                                       // index name filter
        null,                                       // table name filter
        "engine=InnoDB",                            // "CREATE TABLE" suffix
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^12 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    // MariaDB driver
    // Plug compatible with MySQL
    // https://mariadb.com/products/mariadb
    // Example configuration properties:
    //   db.sql.provider=mysql
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=3306
    //   db.sql.user=gts
    //   db.sql.password=opengts
    //   db.sql.url=jdbc:mysql://localhost:${db.sql.port}/
    // ------------------------------------------------------------------------
    // Not fully tested
    // ------------------------------------------------------------------------

    public static final String  MariaDB_Name     = "mariadb";
    protected static DBProvider MariaDB_Provider = new DBProvider(
        MariaDB_Name, DB_MARIADB, 3306,
        new String[] { "org.mariadb.jdbc.Driver" },
        null,                                       // column name filter
        null,                                       // index name filter
        null,                                       // table name filter
        "engine=MyISAM",                            // "CREATE TABLE" suffix
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^12 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    // Oracle XE drivers
    // Example configuration properties:
    //   db.sql.provider=oraclexe
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=1521
    //   db.sql.user=gts
    //   db.sql.password=opengts
    //   db.sql.url=jdbc:oracle:thin:${db.sql.user}/${db.sql.password}@// ${db.sql.host}:${db.sql.port}/XE
    // ------------------------------------------------------------------------
    // Not yet supported
    // ------------------------------------------------------------------------

    public static final String  Oracle_XE_Name     = "oraclexe";
    protected static DBProvider Oracle_XE_Provider = new DBProvider(
        Oracle_XE_Name, DB_ORACLEXE, 1521,
        new String[] { "oracle.jdbc.OracleDriver" }, // "ojdbc14.jar", "ojdbc14_g.jar"
        null,                                       // column name filter
        null,                                       // index name filter
        null,                                       // table name filter
        "",                                         // "CREATE TABLE" suffix
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^12 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    // PostgreSQL driver
    // http://jdbc.postgresql.org/download.html
    // Example configuration properties:
    //   db.sql.provider=postgresql
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=5432
    //   db.sql.user=gts
    //   db.sql.password=opengts
    //   db.sql.url=jdbc:postgresql://localhost:${db.sql.port}/
    // ------------------------------------------------------------------------
    // Note: MAY NOT BE FULLY SUPPORTED
    // ------------------------------------------------------------------------

    public static final String  PostgreSQL_Name     = "postgresql";
    protected static DBProvider PostgreSQL_Provider = new DBProvider(
        PostgreSQL_Name, DB_POSTGRESQL, 5432,
        new String[] { "org.postgresql.Driver" },
        new NameFilter(""),                         // column name filter
        null,                                       // index name filter
        new NameFilterLowerCase(""),                // table name filter
        null,                                       // "CREATE TABLE" suffix (ie. index type)
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "SMALLINT",           //  8bit          Java 'boolean'
            /* SQL_INT8    */ "SMALLINT",           //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "SMALLINT",           //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "INTEGER",            // 16bit (signed)
            /* SQL_INT32   */ "INTEGER",            // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "BIGINT",             // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "NUMERIC(20)",        // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE PRECISION",
            /* SQL_SBLOB   */ "BYTEA",              // max (216 - 1) bytes
            /* SQL_BLOB    */ "BYTEA",              // max (216 - 1) bytes
            /* SQL_MBLOB   */ "BYTEA",              // max (224 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (216 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "TIMESTAMP",
        }
    );  

    public static final String  PostgreSQL_Name_old     = "postgresql";
    protected static DBProvider PostgreSQL_Provider_old = new DBProvider(
        PostgreSQL_Name_old, DB_POSTGRESQL, 5432,
        new String[] { "org.postgresql.Driver" },
        null,                                       // column name filter
        null,                                       // index name filter
        new NameFilter("PG"),                       // table name filter
        null,                                       // "CREATE TABLE" suffix (ie. index type)
        FLAGS_LIMIT|FLAGS_OFFSET,                   // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "TINYINT",            //  8bit (signed) Java 'byte'
            /* SQL_UINT8   */ "TINYINT UNSIGNED",   //  8bit          Java 'byte'
            /* SQL_INT16   */ "SMALLINT",           // 16bit (signed)
            /* SQL_UINT16  */ "SMALLINT UNSIGNED",  // 16bit (signed)
            /* SQL_INT32   */ "INT",                // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "INT UNSIGNED",       // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT UNSIGNED",    // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_BLOB    */ "BLOB",               // max (2^16 - 1) bytes
            /* SQL_MBLOB   */ "MEDIUMBLOB",         // max (2^24 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^16 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    // Apache Derby driver
    // http://db.apache.org/derby
    // Example configuration properties:
    //   db.sql.provider=derby
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=1527
    //   db.sql.user=gts
    //   db.sql.password=opengts
    //   db.sql.url=jdbc:derby://localhost:${db.sql.port}/${db.sql.dbname};create=true
    // Notes:
    //   - Installing the Derby JDBC jar (optional)
    //      cp $DERBY_HOME/lib/derbyclient.jar $JAVA_HOME/lib/ext/.
    //   - Starting Derby:
    //      $DERBY_HOME/bin/startNetworkServer &
    //      (java -jar $DERBY_HOME/lib/derbyrun.jar server start -noSecurityManager &)
    //      "derby.log" will be created in the current directory
    //   - Stopping Derby:
    //      $DERBY_HOME/bin/stopNetworkServer
    //      (java -jar $DERBY_HOME/lib/derbyrun.jar server shutdown)
    //   - Command-line DB query:
    //      java -jar $DERBY_HOME/lib/derbyrun.jar ij
    //      ij> CONNECT 'jdbc:derby:gts;create=true';
    //      ij> CONNECT 'jdbc:derby:/usr/local/gts/derby/gts;create=true';
    // ------------------------------------------------------------------------
    // Partially supported
    // ------------------------------------------------------------------------
    
    public static final String  Derby_Name     = "derby";
    protected static DBProvider Derby_Provider = new DBProvider(
        Derby_Name, DB_DERBY, 1527,
        new String[] { "org.apache.derby.jdbc.ClientDriver", "org.apache.derby.jdbc.EmbeddedDriver" },
        new NameFilterUpperCase(""),                // column name filter
        null,                                       // index name filter
        new NameFilterUpperCase("DB"),              // table name filter
        null,                                       // "CREATE TABLE" suffix
        FLAGS_NONE,                                 // flags
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "SMALLINT",
            /* SQL_INT8    */ "SMALLINT",
            /* SQL_UINT8   */ "SMALLINT",
            /* SQL_INT16   */ "SMALLINT",
            /* SQL_UINT16  */ "SMALLINT",
            /* SQL_INT32   */ "INTEGER",
            /* SQL_UINT32  */ "INTEGER",
            /* SQL_INT64   */ "BIGINT",
            /* SQL_UINT64  */ "BIGINT",
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "DOUBLE",
            /* SQL_SBLOB   */ "BLOB",
            /* SQL_BLOB    */ "BLOB",
            /* SQL_MBLOB   */ "BLOB",
            /* SQL_TEXT    */ "CLOB",
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------
    // Microsoft SQL Server driver
    // http://msdn.microsoft.com/en-us/data/aa937724.aspx
    // http://msdn.microsoft.com/en-us/library/ms378428%28SQL.90%29.aspx
    // Example configuration properties:
    //   db.sql.provider=sqlserver
    //   db.sql.dbname=gts
    //   db.sql.host=localhost
    //   db.sql.port=1433
    //   db.sql.user=gts
    //   db.sql.password=opengts
    //   db.sql.url=jdbc:sqlserver://localhost:${db.sql.port}
    // ------------------------------------------------------------------------
    // Mostly supported
    // ------------------------------------------------------------------------
    
    public static final String  MS_SQLServer_Name     = "sqlserver";
    protected static DBProvider MS_SQLServer_Provider = new DBProvider(
        MS_SQLServer_Name, DB_SQLSERVER, 1433,
        new String[] { "com.microsoft.sqlserver.jdbc.SQLServerDriver" },
        null,                                       // column name filter
        null,                                       // index name filter
        new NameFilter("MS"),                       // table name filter
        null,                                       // "CREATE TABLE" suffix
        FLAGS_LIMIT,                                // flags (does not support offset)
        new String[] {
            /* SQL_UNKNOWN */ "",
            /* SQL_BOOLEAN */ "TINYINT",            //  8bit          Java 'boolean'
            /* SQL_INT8    */ "SMALLINT",           //  8bit (signed) Java 'byte'   [was TINYINT]
            /* SQL_UINT8   */ "SMALLINT",           //  8bit          Java 'byte'   [was TINYINT]
            /* SQL_INT16   */ "INT",                // 16bit (signed)               [was SMALLINT]
            /* SQL_UINT16  */ "INT",                // 16bit (signed)               [was SMALLINT]
            /* SQL_INT32   */ "BIGINT",             // 32bit (signed) Java 'int'
            /* SQL_UINT32  */ "BIGINT",             // 32bit          Java 'int'
            /* SQL_INT64   */ "BIGINT",             // 64bit (signed) Java 'long'
            /* SQL_UINT64  */ "BIGINT",             // 64bit          Java 'long'
            /* SQL_FLOAT   */ "FLOAT",
            /* SQL_DOUBLE  */ "FLOAT",
            /* SQL_SBLOB   */ "VARBINARY",          // max 8000 bytes
            /* SQL_BLOB    */ "IMAGE",              // max (2^31 - 1) bytes
            /* SQL_MBLOB   */ "IMAGE",              // max (2^31 - 1) bytes
            /* SQL_TEXT    */ "TEXT",               // max (2^31 - 1) bytes
            /* SQL_VARCHAR */ "VARCHAR",
            /* SQL_DATETIME*/ "DATETIME",
        }
    );

    // ------------------------------------------------------------------------

    private static DBProvider globalDBProvider = null;
    private static HashMap<String,DBProvider> globalDBProviderMap = new HashMap<String,DBProvider>();
    static {
        // ---
        globalDBProviderMap.put(MySQL_MyISAM_Provider.getJDBCName() , MySQL_MyISAM_Provider);
        globalDBProviderMap.put(MySQL_InnoDB_Provider.getJDBCName() , MySQL_InnoDB_Provider);
        globalDBProviderMap.put(MySQL_Old_Provider.getJDBCName()    , MySQL_Old_Provider   );
        // ---
        globalDBProviderMap.put(MariaDB_Provider.getJDBCName()      , MariaDB_Provider);
        // ---
        globalDBProviderMap.put(MS_SQLServer_Provider.getJDBCName() , MS_SQLServer_Provider);
        // ---
        globalDBProviderMap.put(PostgreSQL_Provider.getJDBCName()   , PostgreSQL_Provider  );
        // ---
        globalDBProviderMap.put(Oracle_XE_Provider.getJDBCName()    , Oracle_XE_Provider   );
        globalDBProviderMap.put(Derby_Provider.getJDBCName()        , Derby_Provider       );
    }

    /**
    *** Returns the DBProvider forthe specified name
    *** @return the DBProvider forthe specified name, or null if not found
    **/
    public static DBProvider getProvider(String dbpName)
    {
        return globalDBProviderMap.get(dbpName); // may return null
    }

    /**
    *** Returns the current DBProvider as defined in the Runtime configuration file 
    *** (property "RTKey.DB_PROVIDER")
    *** @return the current DBProvider
    **/
    public static DBProvider getProvider()
    {
        if (globalDBProvider == null) {
            String dbpName = RTConfig.getString(RTKey.DB_PROVIDER);
            if (StringTools.isBlank(dbpName)) {
                // -- "db.sql.provider" not specified (or is blank)
                globalDBProvider = MySQL_MyISAM_Provider; // default to MySQL MyISAM
                Print.logWarn("DBProvider not specified, using default ("+globalDBProvider.getJDBCName()+").");
            } else {
                globalDBProvider = globalDBProviderMap.get(dbpName);
                if (globalDBProvider == null) {
                    // -- specified db provider not found
                    globalDBProvider = MySQL_MyISAM_Provider; // default to MySQL MyISAM
                    if (!dbpName.equalsIgnoreCase("mysql")) {
                        // -- display warning iff not "mysql"
                        Print.logWarn("DBProvider '"+dbpName+"' not found, using default ("+globalDBProvider.getJDBCName()+").");
                    }
                } else {
                    // -- found specified db provider
                    Print.logDebug("DBProvider installed: " + globalDBProvider.getJDBCName());
                }
            }
            DBProvider.isTableLockingEnabled(); // cache
        }
        return globalDBProvider;
    }

    /**
    *** Returns the name of the current DBProvider
    *** @return The current DBProvider name
    **/
    public static String getProviderName()
    {
        return DBProvider.getProvider().getJDBCName();
    }

    /**
    *** Returns the index type of the current DBProvider
    *** @return The current DBProvider index type (ie. "engine=MyISAM")
    **/
    public static String getProviderIndexType()
    {
        return DBProvider.getProvider().getTableIndexType();
    }

    /**
    *** Returns true if the index type of the current DBProvider if MySQL "InnoDB"
    *** @return True if the index type of the current DBProvider if MySQL "InnoDB"
    **/
    public static boolean isMySqlInnoDB()
    {
        String ndxType = StringTools.trim(DBProvider.getProviderIndexType()).toLowerCase();
        return ndxType.equalsIgnoreCase("engine=innodb");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Loads the current DBProvider class drivers
    *** @return the name of the loaded DBProvider class
    **/
    public static String loadJDBCDriver()
    {
        String driver[] = DBProvider.getProvider().getDrivers();
        if (driver != null) {

            /* try loading drivers */
            for (int i = 0; i < driver.length; i++) {
                try {
                    Class.forName(driver[i]); // java.sql.DriverManager
                    return driver[i]; // stop at the first one that works
                } catch (ClassNotFoundException cnfe) {
                    String warnMsg = "JDBC driver class not found: " + driver[i]; // + " ["+cnfe+"]";
                    Print.logWarn(warnMsg);
                    //continue;
                } catch (UnsupportedClassVersionError ucve) {
                    // -- "Unsupported major.minor version 51.0"
                    // -   J2SE 7   = 51 (0x33 hex)
                    // -   J2SE 6.0 = 50 (0x32 hex)
                    // -   J2SE 5.0 = 49 (0x31 hex)
                    // -   JDK  1.4 = 48 (0x30 hex)
                    // -   JDK  1.3 = 47 (0x2F hex)
                    // -   JDK  1.2 = 46 (0x2E hex)
                    // -   JDK  1.1 = 45 (0x2D hex)
                    String warnMsg = "JDBC driver not compatible with JDK version: " + driver[i] + " ["+ucve+"]";
                    Print.logWarn(warnMsg);
                    //continue;
                } catch (Throwable t) { // ?
                    Print.logException("JDBC driver class load error: " + driver[i], t);
                    //continue;
                }
            }

            /* error message */
            Print.logError("-------------------------------------------------------------------------");
            String extDir = System.getProperty("java.ext.dirs");
            if (extDir != null) {
                String edir[] = StringTools.split(extDir, File.pathSeparatorChar);
                if (edir.length > 1) {
                    Print.logError("Install the JDBC jar file in one of the following directories:");
                    for (int i = 0; i < edir.length; i++) {
                        Print.logError(" > " + edir[i]);
                    }
                } else {
                    Print.logError("Install the JDBC jar file in the following directory:");
                    Print.logError(" > " + extDir);
                }
            } else {
                Print.logError("Install JDBC drivers");
            }
            Print.logError("(NOTE: The installed jar file permissions must also be world-readable)");
            Print.logError("-------------------------------------------------------------------------");

        }
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sends a notification for CommunicationException errors
    **/
    public static boolean sendJdbcCommunicationExceptionNotification(Throwable th)
    {

        /* no exception? */
        if (th == null) {
            return false;
        }

        /* check for MySQL "CommunicationException" */
        String cn = StringTools.className(th);
        if (cn.indexOf("CommunicationException") < 0) {
            return false; 
        }

        /* send notification */
        // SendMail
        
        /* return true */
        return false;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** Returns the host name for the DBProvider server
    *** @return The name of the host serving the database
    **/
    public static String getDBHost()
    {
        return RTConfig.getString(RTKey.DB_HOST);
    }
    
    /**
    *** Returns the port number for the DBProvider server
    *** @return The port number
    **/
    public static int getDBPort()
    {
        return DBProvider.getProvider().getDefaultPort();
    }

    /**
    *** Returns the database name
    *** @return The database name
    **/
    public static String getDBName()
    {
        return RTConfig.getString(RTKey.DB_NAME);
    }

    /**
    *** Returns the DB user name
    *** @return The DB user name
    **/
    public static String getDBUsername()
    {
        if (RTConfig.hasProperty(RTKey.DB_USER,false)) {
            return RTConfig.getString(RTKey.DB_USER, "");
        } else {
            String user = RTConfig.getString(RTKey.DB_USER);
            return (user != null)? user : "";
        }
    }

    /**
    *** Returns the DB user password
    *** @return The DB user password
    **/
    public static String getDBPassword()
    {
        if (RTConfig.hasProperty(RTKey.DB_PASS,false)) {
            return RTConfig.getString(RTKey.DB_PASS);
        } else {
            String pass = RTConfig.getString(RTKey.DB_PASS);
            return (pass != null)? pass : "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static boolean didLogJdbcURL    = false;

    private static String  JDBC_URL[]       = new String[] { RTKey.DB_URL    };
    private static String  JDBC_URL_DB[]    = new String[] { RTKey.DB_URL_DB };

    /**
    *** Returns the database access URI
    *** @param inclDBName  True to include the name of the database in the URI
    *** @return The database access URI
    **/
    public static String getDBUri(boolean inclDBName)
    {

        /* check runtime properties for URL */
        String urlStr = null;
        if (inclDBName) {
            urlStr = RTConfig.getString(JDBC_URL_DB,"").trim();
            if (!StringTools.isBlank(urlStr)) {
                // -- explicitly specified in runtime properties
                //Print.logInfo("DBProvider '"+JDBC_URL_DB[0]+"': " + urlStr);
                return urlStr;
            }
            // start with standard connection url
            urlStr = RTConfig.getString(JDBC_URL,"").trim();
        } else {
            urlStr = RTConfig.getString(JDBC_URL,"").trim();
            if (!StringTools.isBlank(urlStr)) {
                // -- explicitly specified in runtime properties
                //Print.logInfo("DBProvider '"+JDBC_URL[0]+"': " + urlStr);
                return urlStr;
            }
        }

        /* construct default URL */
        StringBuffer uri = new StringBuffer(urlStr);
        DBProvider dbProv = DBProvider.getProvider();
        switch (dbProv.getID()) {
            case DB_MYSQL: {
                // -- MySQL: jdbc:mysql://<host>:<port>/[<database>]
                if (uri.length() == 0) {
                    uri.append("jdbc:mysql://").append(getDBHost()).append(":").append(getDBPort()).append("/");
                }
                if (inclDBName) {
                    uri.append(getDBName());
                }
                break;
            }
            case DB_POSTGRESQL: {
                // -- PostgreSQL: jdbc:postgresql://<host>:<port>/[<database>]
                if (uri.length() == 0) {
                    uri.append("jdbc:postgresql://").append(getDBHost()).append(":").append(getDBPort()).append("/");
                }
                if (inclDBName) {
                    uri.append(getDBName());
                }
                break;
            }
            case DB_DERBY: {
                // -- Derby: jdbc:derby://<host>:<port>/<database>[;create=true]
                // -  ij> connect 'jdbc:derby://localhost:1527/gts;create=true';
                if (uri.length() == 0) {
                    // TODO:
                    uri.append("jdbc:derby://").append(getDBHost()).append(":").append(getDBPort()).append("/");
                    uri.append(getDBName()).append(";create=true");
                }
                break;
            }
            case DB_SQLSERVER: {
                // -- ["Building the Connection URL"] http://msdn2.microsoft.com/en-us/library/ms378428.aspx
                // -  SQLServer: jdbc:sqlserver://<host>[;instanceName=<instance>]:<port>;property=value[;databaseName=<db>]
                // -  Note: in some cases, specifying both the instanceName and port, will cause the connection to hang/timeout.
                if (uri.length() == 0) {
                    uri.append("jdbc:sqlserver://").append(getDBHost()).append(":").append(getDBPort());
                }
                if (inclDBName && (uri.indexOf("databaseName=") < 0)) {
                    uri.append(";databaseName=").append(getDBName()); 
                }
                break;
            }
            default : {
                // -- unrecognized DB provider
                Print.logInfo("DBProvider unrecognized provider: " + dbProv.getID());
                return null;
            }
        }

        /* return URL */
        if (!didLogJdbcURL) {
            didLogJdbcURL = true;
            if (uri.length() > 0) {
                Print.logDebug("Connection URL: " + uri);
            }
        }
        //Print.logInfo("DBProvider URI: " + uri);
        return uri.toString();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the index of the specified SQL SQL datatype
    *** @param localType     The local type
    *** @param isDefinedType Is a 'defined' type
    *** @return The type 'index'
    **/
    protected static int _getTypeIndex(String localType, boolean isDefinedType)
    {
        if (localType != null) {
            localType = localType.toUpperCase();
            for (int i = 1; i < TYPE_ARRAY.length; i++) {
                if (localType.equals(TYPE_ARRAY[i])) {
                    return i;
                }
            }
            if (localType.startsWith(TYPE_STRING + "[")) {
                return SQL_VARCHAR;
            }
        }
        if (isDefinedType) {
            Print.logWarn("Type not found: " + localType);
        }
        return 0;
    }

    /**
    *** Returns 'true' is the specified types are equivalent
    *** @param defType  The defined type
    *** @param actType  The actual tupe
    *** @return True if the types are equivalent
    **/
    public static boolean areTypesEquivalent(String defType, String actType)
    {
        boolean equivalent = false;
        if ((defType == null) || (actType == null)) {
            // one of the types is null
            equivalent = false;
        } else
        if (defType.equalsIgnoreCase(actType)) {
            // types are equal
            equivalent = true;
        } else
        if (defType.startsWith(TYPE_STRING + "[")) {
            // STRING[xx] types must match exactly (and they don't, if we are here)
            equivalent = false;
        } else {
            // return true if types are backed by the same sql type
            int defNdx = DBProvider._getTypeIndex(defType,true);
            int actNdx = DBProvider._getTypeIndex(actType,false);
            String sqlTypes[] = DBProvider.getProvider().getSQLTypes();
            equivalent = sqlTypes[defNdx].equals(sqlTypes[actNdx]);
        }
        //Print.logInfo("Type equivalent? " + defType + " <==> " + actType + " : " + equivalent);
        return equivalent;
    }

    /**
    *** Returns the SQL type for the defined specified local type
    *** @param localType  The local type
    *** @return The SQL type
    **/
    public static String getSqlTypeFromDataType(String localType)
    {
        if (localType != null) {
            localType = localType.toUpperCase();
            if (localType.startsWith(TYPE_STRING + "[")) {
                String x = localType.substring(TYPE_STRING.length() + 1);
                int len = StringTools.parseInt(x, 32);
                return DBProvider.getSQLType(SQL_VARCHAR) + "(" + len + ")";
            } else {
                int ndx = DBProvider._getTypeIndex(localType,true);
                return DBProvider.getSQLType(ndx);
            }
        } else {
            return "";
        }
    }

    /**
    *** Returns the defined local type for the specified SQL type
    *** @param sqlType  The SQL type
    *** @return  The defined local type
    **/
    public static String getDataTypeFromSqlType(String sqlType)
    {
        if (sqlType != null) {
            sqlType = sqlType.toUpperCase();
            DBProvider dbProv = DBProvider.getProvider();
            String sqlTypes[] = dbProv.getSQLTypes();
            if (sqlType.startsWith(sqlTypes[SQL_INT8])) {
                return (sqlType.indexOf("UNSIGNED") > 0)? TYPE_UINT8 : TYPE_INT8;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_BOOLEAN])) { // test BOOLEAN _after_ INT8
                return TYPE_BOOLEAN;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_INT16])) {
                return (sqlType.indexOf("UNSIGNED") > 0)? TYPE_UINT16 : TYPE_INT16;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_INT32])) {
                return (sqlType.indexOf("UNSIGNED") > 0)? TYPE_UINT32 : TYPE_INT32;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_INT64])) {
                return (sqlType.indexOf("UNSIGNED") > 0)? TYPE_UINT64 : TYPE_INT64;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_FLOAT])) {
                return TYPE_FLOAT;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_DOUBLE])) {
                return TYPE_DOUBLE;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_SBLOB])) {
                return TYPE_SBLOB;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_BLOB])) {
                return TYPE_BLOB;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_MBLOB])) {
                return TYPE_MBLOB;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_TEXT])) {
                return TYPE_TEXT;
            } else
            if (sqlType.startsWith(sqlTypes[SQL_VARCHAR] + "(")) {
                String x = sqlType.substring(sqlTypes[SQL_VARCHAR].length() + 1);
                int len = StringTools.parseInt(x, 32);
                return TYPE_STRING(len);
            } else {
                Print.logWarn("Unrecognized SQL type: " + sqlType);
                return TYPE_UNKNOWN;
            }
        } else {
            return TYPE_UNKNOWN;
        }
    }

    /**
    *** Returns the local data type for the specified index
    *** @param dataTypeNdx  The data type index (Note: NO BOUNDS CHEKCING IS PERFORMED!)
    *** @return The local data type definition
    **/
    public static String getSQLType(int dataTypeNdx)
    {
        try {
            String dt[] = DBProvider.getProvider().getSQLTypes();
            return dt[dataTypeNdx];
        } catch (Throwable th) { // IndexOufOfBounds, etc
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Get String value from ResultSet, return 'dft' if column does not exist
    *** @param rs       The ResultSet
    *** @param colName  The Column Name
    *** @param dft      The default value returned if the column does not exist
    *** @return The ResultSet column value
    **/
    private static String rs_GetString(ResultSet rs, String colName, String dft)
    {
        try {
            return rs.getString(colName);
        } catch (SQLException sqe) {
            return dft;
        }
    }

    /**
    *** Returns the existing fields in the specified table
    *** @param utableName The table name
    *** @return An array of fields
    *** @throws DBException if an error occured fetching the fields
    **/
    public static DBField[] getActualTableFields(String utableName)
        throws DBException
    {

        /* get table index map */
        DBTableIndexMap indexMap = null;
        try {
            indexMap = DBProvider.getActualTableIndexMap(utableName);
        } catch (DBException dbe) {
            Print.logWarn("Unable to retrieve table index information: " + dbe.getMessage());
        }

        /* column names */
        String colName_Field   = "Field";
        String colName_Type    = "Type";
        String colName_CharSet = "Collation";
        String colName_Default = "Default";
        String colName_Extra   = "Extra";
        String colName_Key     = "Key";

        /* this is currently only supported by MySQL and MS SQLServer */
        String xtableName = DBProvider.translateTableName(utableName);
        DBProvider dbp    = DBProvider.getProvider();
        String showCols   = null;
        String dftCharSet = "";
        if (dbp.getID() == DB_MYSQL) {
            // Ref: http://dev.mysql.com/doc/refman/5.0/en/show-columns.html
            // MySQL: "SHOW FULL COLUMNS FROM <TableName>"
            showCols      = "SHOW FULL COLUMNS FROM " + xtableName;
            dftCharSet    = "latin1_swedish_ci";
        } else
        if (dbp.getID() == DB_SQLSERVER) {
            // SQLServer: "SELECT column_name FROM <dbname>.information_schema.columns WHERE table_name = '<table>'"
            showCols      = "SELECT column_name as [Field],(CAST(Data_type as nvarchar) + '(' + CAST(isnull(Character_maximum_length,numeric_precision) as nvarchar) + ')' ) as [Type], 'KEY' = CASE IS_NULLABLE WHEN 'NO' THEN 'PRI' ELSE '' END from information_schema.columns WHERE table_name='" + xtableName + "';";
            dftCharSet    = "SQL_Latin1_General_CP1_CI_AS"; // "Latin1_General_CI_AS"
            // SELECT * FROM gts.information_schema.columns WHERE table_name = 'MSDevice';
            // TABLE_CATALOG   TABLE_SCHEMA TABLE_NAME COLUMN_NAME      ORDINAL_POSITION   COLUMN_DEFAULT  IS_NULLABLE DATA_TYPE   CHARACTER_MAXIMUM_LENGTH    CHARACTER_OCTET_LENGTH  NUMERIC_PRECISION   NUMERIC_PRECISION_RADIX NUMERIC_SCALE   DATETIME_PRECISION  CHARACTER_SET_CATALOG   CHARACTER_SET_SCHEMA    CHARACTER_SET_NAME  COLLATION_CATALOG   COLLATION_SCHEMA    COLLATION_NAME          DOMAIN_CATALOG  DOMAIN_SCHEMA   DOMAIN_NAME
            // gts             dbo          MSDevice   accountID        1                  NULL            NO          varchar     32                          32                      NULL                NULL                    NULL            NULL                NULL                    NULL                    iso_1               NULL                NULL                Latin1_General_CI_AS    NULL            NULL            NULL
            // gts             dbo          MSDevice   deviceID         2                  NULL            NO          varchar     32                          32                      NULL                NULL                    NULL            NULL                NULL                    NULL                    iso_1               NULL                NULL                Latin1_General_CI_AS    NULL            NULL            NULL
            // gts             dbo          MSDevice   groupID          3                  NULL            YES         varchar     32                          32                      NULL                NULL                    NULL            NULL                NULL                    NULL                    iso_1               NULL                NULL                Latin1_General_CI_AS    NULL            NULL            NULL
            // gts             dbo          MSDevice   uniqueID         13                 NULL            YES         varchar     40                          40                      NULL                NULL                    NULL            NULL                NULL                    NULL                    iso_1               NULL                NULL                Latin1_General_CI_AS    NULL            NULL            NULL
            // gts             dbo          MSDevice   dataKey          23                 NULL            YES         text        2147483647                  2147483647              NULL                NULL                    NULL            NULL                NULL                    NULL                    iso_1               NULL                NULL                Latin1_General_CI_AS    NULL            NULL            NULL
            // gts             dbo          MSDevice   ignitionIndex    24                 NULL            YES         smallint    NULL                        NULL                    5                   10                      0               NULL                NULL                    NULL                    NULL                NULL                NULL                NULL                    NULL            NULL            NULL
            // gts             dbo          MSDevice   lastPingTime     31                 NULL            YES         bigint      NULL                        NULL                    19                  10                      0               NULL                NULL                    NULL                    NULL                NULL                NULL                NULL                    NULL            NULL            NULL
            // gts             dbo          MSDevice   expectAck        34                 NULL            YES         tinyint     NULL                        NULL                    3                   10                      0               NULL                NULL                    NULL                    NULL                NULL                NULL                NULL                    NULL            NULL            NULL
            // gts             dbo          MSDevice   totalProfileMask 43                 NULL            YES         image       2147483647                  2147483647              NULL                NULL                    NULL            NULL                NULL                    NULL                    NULL                NULL                NULL                NULL                    NULL            NULL            NULL
            // gts             dbo          MSDevice   lastFuelLevel    55                 NULL            YES         float       NULL                        NULL                    53                  2                       NULL            NULL                NULL                    NULL                    NULL                NULL                NULL                NULL                    NULL            NULL            NULL
        } else
        if (dbp.getID() == DB_DERBY) {
            // http://db.apache.org/derby/docs/10.9/ref/refderby.pdf
            // http://db.apache.org/derby/docs/10.9/ref/
            showCols      = "SELECT " + 
                "T.TABLENAME,C.COLUMNNAME,C.REFERENCEID,C.COLUMNNUMBER,C.COLUMNDATATYPE,C.COLUMNDEFAULT,N.TYPE " +
                "FROM SYS.SYSCOLUMNS C, SYS.SYSCONSTRAINTS N, SYS.SYSTABLES T " +
                "WHERE T.TABLENAME = '"+xtableName.toUpperCase()+"' AND T.TABLEID=C.REFERENCEID AND T.TABLEID=N.TABLEID " +
                "ORDER BY C.COLUMNNUMBER";
            colName_Field = "COLUMNNAME";
            colName_Type  = "COLUMNDATATYPE";
            colName_Key   = "TYPE"; // P-primaryKey, U-unique, C-check,  F-foreignKey
            dftCharSet    = "utf8";
        } else
        if (dbp.getID() == DB_POSTGRESQL) {
            showCols      = "SELECT " +
                " column_name as \"Field\", " +
                " data_type as \"Type\", " +
                " collation_name as \"Collation\", " +
                " column_default as \"Default\", " +
                " '' as \"Extra\", " +
                " '' as \"Key\" " +
                "FROM information_schema.columns " +
                "WHERE table_name ='"+xtableName+"'";
            dftCharSet = "utf8";
        } else {
            return null;
        }

        /* extract fields */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet   rs   = null;
        Set<String> dbfn = new HashSet<String>();
        Vector<DBField> dbf = new Vector<DBField>();
        try {
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(showCols);
            rs   = stmt.getResultSet();
            while (rs.next()) {
                /*
                if (dbp.getID() == DB_DERBY) {
                    Print.logInfo("Derby table info:");
                    String tblID = rs_GetString(rs,"TABLEID","?");
                    String tblNm = rs.getString("TABLENAME");
                    Print.logInfo("TableID="+tblID + " TableNmae="+tblNm);
                    String refID = rs.getString("REFERENCEID");
                    String colID = rs.getString("COLUMNNAME");
                    String colTp = rs.getString("COLUMNDATATYPE");
                    String colDf = rs.getString("COLUMNDEFAULT");
                    Print.logInfo("RefID="+refID + " ColID="+colID + " Type="+colTp +" Default="+colDf);
                }
                */
                String fldName = rs.getString(colName_Field);
                if (dbfn.contains(fldName)) {
                    //Print.logWarn("Skipping duplicate field name entry: " + fldName);
                    continue;
                }
                String fldType = rs.getString(colName_Type);
                String charSet = StringTools.trim(rs_GetString(rs,colName_CharSet,dftCharSet)); // latin1_swedish_ci, utf8_general_ci
                String fldDflt = StringTools.trim(rs_GetString(rs,colName_Default,""));
                String fldExtr = StringTools.trim(rs_GetString(rs,colName_Extra  ,""));
                //Print.logInfo("Field="+fldName +" Type="+fldType +" Collation="+charSet + " Default="+fldDflt + " Extra="+fldExtr);
                Set<String> ndxSet = null;
                if (indexMap != null) {
                    ndxSet = indexMap.getIndexesForColumn(fldName);
                } else {
                    ndxSet = new HashSet<String>();
                    String keyType = rs_GetString(rs,colName_Key,null);  // useless for multiple alternate keys
                    //Print.logInfo("Field:" + fldName + "  Key:" + keyType);
                    if (keyType == null) {
                        ndxSet.add("?");
                    } else
                    if (keyType.equalsIgnoreCase("PRI") || keyType.equalsIgnoreCase("P")) {
                        ndxSet.add(PRIMARY_INDEX_NAME);
                    } else
                    if (keyType.equalsIgnoreCase("UNI") || keyType.equalsIgnoreCase("U")) {
                        //ndxSet.add(DEFAULT_ALT_INDEX_NAME); 
                        ndxSet.add(UNIQUE_INDEX_NAME);
                    } else
                    if (keyType.equalsIgnoreCase("MUL")) {
                        ndxSet.add(DEFAULT_ALT_INDEX_NAME); 
                    } else
                    if (!keyType.equals("")) {
                        ndxSet.add(PRIMARY_INDEX_NAME);
                    }
                    if (ndxSet.isEmpty()) { ndxSet = null; }
                }
                boolean autoIncr = fldExtr.equalsIgnoreCase("auto_increment");
                DBField field = new DBField(utableName, fldName, fldType, autoIncr, charSet, ndxSet);
                dbf.add(field);
                dbfn.add(fldName);
            }
        } catch (SQLException sqe) {
            Print.logWarn("Table Error.  Displaying column info [" + sqe);
            // "rs" may be null if error occurred during "execute"
            if (rs != null) {
                try {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int colCnt = rsmd.getColumnCount();
                    for (int i = 1; i <= colCnt; i++) {
                        Print.logWarn(i + ") Column Name: " + rsmd.getColumnName(i));
                    }
                } catch (SQLException sqle) {
                    Print.logError("Unable to read ResultSet MetaData ... " + sqle);
                } catch (Throwable th) {
                    // should not occur
                    Print.logException("Unexpected exception while reading ResultSet MetaData:", th);
                }
            }
            throw new DBException("Unable to get fields", sqe);
        } catch (DBException dbe) {
            Print.logError("DBException: " + dbe);
            throw dbe;
        } catch (Throwable th) {
            // should not occur
            throw new DBException("Unexpected Exception", th);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        //if (dbp.getID() == DB_DERBY) { return null; }

        /* return fields */
        return dbf.toArray(new DBField[dbf.size()]);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a map of column names to index names
    *** @param utableName The untranslated table name
    *** @return A map of column names to index names
    *** @throws DBException if an error occured fetching the indexes
    **/
    public static DBTableIndexMap getActualTableIndexMap(String utableName)
        throws DBException
    {

        /* column names */
        String colName_Table_name  = "Table";
        String colName_Column_name = "Column_name";
        String colName_Key_name    = "Key_name";

        /* this is currently only supported by MySQL */
        String xtableName = DBProvider.translateTableName(utableName);
        DBProvider dbp = DBProvider.getProvider();
        String showCols = null;
        if (dbp.getID() == DB_MYSQL) {
            // MySQL: "SHOW INDEXES FROM <TableName>"
            showCols = "SHOW INDEXES FROM " + xtableName;
        } else
        if (dbp.getID() == DB_SQLSERVER) {
            // SQLServer: SQLServer_AlternateKeys
            // ----------------------
            // The following displays which tables have which alternate indexes, but not which columns are included
            // SELECT
            //    so.name AS TableName,
            //    si.name AS IndexName,
            //    si.type_desc AS IndexType
            // FROM
            //    sys.indexes si
            //    JOIN sys.objects so ON si.[object_id] = so.[object_id]
            // WHERE
            //    so.type = 'U'    --Only get indexes for User Created Tables
            //    AND si.name IS NOT NULL
            // ORDER BY
            //    so.name, si.type
            // ----------------------
            // The following displays the indexes by table column
            // SELECT
            //     t.name AS Table_name,
            //     ind.name AS Index_Name,
            //     col.name AS Column_Name
            // FROM sys.indexes ind
            // INNER JOIN sys.index_columns ic
            //     ON ind.object_id = ic.object_id and ind.index_id = ic.index_id
            // INNER JOIN sys.columns col
            //     ON ic.object_id = col.object_id and ic.column_id = col.column_id
            // INNER JOIN sys.tables t
            //     ON ind.object_id = t.object_id
            // ORDER BY
            //     t.name, ind.name
            // ;
            // ----------------------
            //return null;
            showCols =
                "SELECT " +
                    "t.name AS Table_name, " +
                    "ind.name AS Key_name, " +
                    "col.name AS Column_name " +
                "FROM sys.indexes ind " +
                "INNER JOIN sys.index_columns ic " +
                    "ON ind.object_id = ic.object_id and ind.index_id = ic.index_id " +
                "INNER JOIN sys.columns col " +
                    "ON ic.object_id = col.object_id and ic.column_id = col.column_id " +
                "INNER JOIN sys.tables t " +
                    "ON ind.object_id = t.object_id " +
                "WHERE " +
                    "t.name = '"+xtableName+"' " +
              //"ORDER BY " +
              //    "t.name, ind.name " +
                ";";
            // Table_name  Key_name                        Column_name
            // MSDevice    altIndex                        uniqueID
            // MSDevice    PK__MSDevice__9A2CC475A82EFF9B  accountID
            // MSDevice    PK__MSDevice__9A2CC475A82EFF9B  deviceID
        } else
        if (dbp.getID() == DB_POSTGRESQL) {
            showCols =
                " select " +
                "    t.relname as table_name," +
                "    i.relname as \"Key_name\"," +
                "    a.attname as \"Column_name\"" +
                " from" +
                "    pg_class t," +
                "    pg_class i," +
                "    pg_index ix," +
                "    pg_attribute a" +
                " where" +
                "    t.oid = ix.indrelid" +
                "    and i.oid = ix.indexrelid" +
                "    and a.attrelid = t.oid" +
                "    and a.attnum = ANY(ix.indkey)" +
                "    and t.relkind = 'r'" +
              //"    and t.relname = 'device'";
                "    and t.relname = '"+xtableName+"'";
        } else {
            // Derby:: "SELECT ???"
            return null;
        }

        /* extract fields */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet   rs   = null;
        DBTableIndexMap indexMap = null;
        try {
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(showCols);
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String fldName = rs.getString(colName_Column_name);
                String ndxName = rs.getString(colName_Key_name);
                //Print.logInfo("Column '%s', Key '%s'", fldName, ndxName);
                if (!StringTools.isBlank(fldName) && !StringTools.isBlank(ndxName)) {
                    if (indexMap == null) { indexMap = new DBTableIndexMap(utableName); }
                    if (dbp.getID() == DB_SQLSERVER) {
                        // SQLServer ==> MySQL
                        // Table      Column_name   Key_name
                        // MSAccount  contactEmail  email     
                        // MSAccount  managerID     manager              
                        // MSAccount  accountID     PK__MSAccoun__F267253E903766D3    
                        if (ndxName.startsWith("PK__")) { 
                            ndxName = PRIMARY_INDEX_NAME; // "PRIMARY"
                        } else
                        if (ndxName.startsWith("UQ__")) { 
                            ndxName = UNIQUE_INDEX_NAME;  // "UNIQUE"
                        }
                    }
                    indexMap.addIndexColumn(ndxName, fldName);
                } else {
                    throw new DBException("Expected index column names not found");
                }
            }
        } catch (SQLException sqe) {
            throw new DBException("Unable to get indexes", sqe);
        } catch (Throwable th) {
            // we have received an unexplained "java.lang.NoClassDefFoundError" here 
            // due to a missing "DBTableIndexMap" class.
            Print.logException("Unexpected exception: ", th);
            throw new DBException("Unexpected exception", th);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return index map */
        return indexMap;
        
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Get data type mask for type
    *** @param localType The nocal type name
    *** @return The data type mask for type
    **/
    public static int getDataTypeMask(String localType)
    {
        int ndx = DBProvider._getTypeIndex(localType,true);
        return DATATYPE_ARRAY[ndx];
    }

    /**
    *** Returns true if the specified data type is a Boolean type
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is a Boolean type
    **/
    public static boolean isDataTypeBoolean(int typeNdx)
    {
        return ((typeNdx & DATATYPE_BOOLEAN) != 0);
    }
    public static boolean isDataTypeBoolean(String localType)
    {
        return isDataTypeBoolean(DBProvider.getDataTypeMask(localType));
    }

    /**
    *** Returns true if the specified data type is numeric
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is numeric
    **/
    public static boolean isDataTypeNumeric(int typeNdx)
    {
        return ((typeNdx & DATATYPE_NUMERIC) != 0);
    }
    public static boolean isDataTypeNumeric(String localType)
    {
        return isDataTypeNumeric(DBProvider.getDataTypeMask(localType));
    }

    /**
    *** Returns true if the specified data type is a floating point type
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is a floating point type
    **/
    public static boolean isDataTypeDecimal(int typeNdx)
    {
        return ((typeNdx & DATATYPE_DECIMAL) != 0);
    }
    public static boolean isDataTypeDecimal(String localType)
    {
        return isDataTypeDecimal(DBProvider.getDataTypeMask(localType));
    }

    /**
    *** Returns true if the specified data type is unsigned
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is unsigned
    **/
    public static boolean isDataTypeUnsigned(int typeNdx)
    {
        return ((typeNdx & DATATYPE_UNSIGNED) != 0);
    }

    /**
    *** Returns true if the specified data type is unsigned
    *** @param localType The local data type name
    *** @return True if the specified data type is a Boolean type
    **/
    public static boolean isDataTypeUnsigned(String localType)
    {
        return isDataTypeUnsigned(DBProvider.getDataTypeMask(localType));
    }

    /** 
    *** Returns true if the specified data type is a String type
    *** @param typeNdx The data type mask for type
    *** @return True if the specified data type is a string
    **/
    public static boolean isDataTypeString(int typeNdx)
    {
        return ((typeNdx & DATATYPE_STRING) != 0);
    }

    /**
    *** Returns true if the specified data type is a String type
    *** @param localType The local data type name
    *** @return True if the specified data type is a String type
    **/
    public static boolean isDataTypeString(String localType)
    {
        return isDataTypeString(DBProvider.getDataTypeMask(localType));
    }

    /**
    *** Returns true if the specified data type is a Binary type
    *** @param typeNdx The data type mask for type mask for type
    *** @return True if the specified data type is a String type
    **/
    public static boolean isDataTypeBinary(int typeNdx)
    {
        return ((typeNdx & DATATYPE_BINARY) != 0);
    }

    /**
    *** Returns true if the specified data type is a Binary type
    *** @param localType The local data type name
    *** @return True if the specified data type is a String type
    **/
    public static boolean isDataTypeBinary(String localType)
    {
        return isDataTypeBinary(DBProvider.getDataTypeMask(localType));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Create a database (needed for initialization only)

    /**
    *** Create the database specified by the Runtime config (needed for 
    *** initialization only)
    *** @param rootUser The database root user account name
    *** @param rootPass The database root user password
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createDatabase(String rootUser, String rootPass)
        throws DBException, SQLException
    {
        String dbName = DBProvider.getDBName();

        /* validate arguments */
        //if (StringTools.isBlank(rootUser)) {
        //    throw new DBException("Root user not specified");
        //} else
        if (StringTools.isBlank(dbName)) {
            throw new DBException("No database name specified");
        }

        /* connection */
        String dbUri = DBProvider.getDBUri(false);
        DBConnection dbc = null;

        /* create */
        switch (DBProvider.getProvider().getID()) {
            case DB_MYSQL    : {
                // -- MySQL: CREATE DATABASE <DataBase>
                dbc = DBConnection.getDBConnection_write(dbUri, rootUser, rootPass);
                try {
                    dbc.executeUpdate("CREATE DATABASE " + dbName + ";");
                } finally {
                    DBConnection.release(dbc);
                }
                break;
            }
            case DB_POSTGRESQL: {
                // -- PostgreSQL: CREATE DATABASE <DataBase>
                dbc = DBConnection.getDBConnection_write(dbUri, rootUser, rootPass);
                try {
                    dbc.executeUpdate("CREATE DATABASE " + dbName + ";");
                } finally {
                    DBConnection.release(dbc);
                }
                break;
            }
            case DB_DERBY: {
                // -- Derby: already created as part of the connection
                dbc = DBConnection.getDBConnection_write(dbUri, rootUser, rootPass);
                DBConnection.release(dbc);
                break;
            }
            case DB_SQLSERVER: {
                // -- SQLServer: CREATE DATABASE <DataBase>
                dbc = DBConnection.getDBConnection_write(dbUri, rootUser, rootPass);
                try {
                    dbc.executeUpdate("CREATE DATABASE " + dbName + ";");
                } finally {
                    DBConnection.release(dbc);
                }
                break;
            }
        }

        /* log success */
        Print.logInfo("Database created: " + dbName);

        // To drop a database: (not implemented here)
        //    "DROP DATABASE <DataBase_Name>;"

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Translates table name for DB provider
    *** @param dbprov   The DBProvider instance
    *** @param tn       The base table name
    *** @return The translated table name (table name prefixes added, etc.)
    **/
    public static String translateTableName(DBProvider dbprov, String tn)
    {
        DBProvider dbp = (dbprov != null)? dbprov : DBProvider.getProvider();
        NameFilter tnf = dbp.getTableNameFilter();
        return (tnf != null)? tnf.name(tn) : tn;
    }

    /**
    *** Translates table name for DB provider
    *** @param tn The base table name
    *** @return The translated table name (table name prefixes added, etc.)
    **/
    public static String translateTableName(String tn)
    {
        DBProvider dbp = DBProvider.getProvider();
        NameFilter tnf = dbp.getTableNameFilter();
        return (tnf != null)? tnf.name(tn) : tn;
    }

    /**
    *** Pre-translates table name for DB provider (used only in calls to "TABLE_NAME()")
    *** @param tn  The base (untranslated) table name
    *** @return The pre-translated table name (before DBProvider dependent translation)
    **/
    public static String _preTranslateTableName(String tn)
    {
        if (!StringTools.isBlank(tn)) {
            // -- ie. "EventData.tableName=EventData_201505"
            String tableNameKey = tn + ".tableName";
            String tableName    = RTConfig.getString(tableNameKey, null);
            return !StringTools.isBlank(tableName)? tableName : tn;
        } else {
            return tn; // return as-is
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Translates column name for DB provider
    *** @param cn The base column name
    *** @return The translated column name (column name prefixes added, etc.)
    **/
    public static String translateColumnName(String cn)
    {
        DBProvider dbp = DBProvider.getProvider();
        NameFilter cnf = dbp.getColumnNameFilter();
        return (cnf != null)? cnf.name(cn) : cn;
    }

    // ------------------------------------------------------------------------

    /**
    *** Create the backing table for the specified DBFactory
    *** @param factory The DBFactory
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createTable(DBFactory<? extends DBRecord<?>> factory)
        throws SQLException, DBException
    {
        String utableName   = factory.getUntranslatedTableName();
        DBField fields[]    = factory.getFields();
        DBField priKeys[]   = factory.getKeyFields();
        String priKeyType   = factory.getKeyType();
        DBProvider.createTable(utableName, fields, priKeys, priKeyType, factory.getAlternateIndexes());
    }

    /**
    *** Create the specified SQL table
    *** @param utableName The untranslated table name
    *** @param flds       The table columns
    *** @param priKeys    The table primary keys
    *** @param keyType    The key type
    *** @param altIndexes The table alternate indexes
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createTable(String utableName, DBField flds[], DBField priKeys[], String keyType, DBAlternateIndex altIndexes[])
        throws SQLException, DBException
    {
        DBProvider dbp = DBProvider.getProvider();
        int dbProvID = dbp.getID();
        String xtableName = DBProvider.translateTableName(utableName);
        StringBuffer sb = new StringBuffer();

        // MySQL:      CREATE TABLE <table> ( <fields...>, <KeyType> ( <Keys...> ) )
        // SQLServer:  CREATE TABLE <table> ( <fields...>, <KeyType> ( <Keys...> ) )
        // PostgreSQL: CREATE TABLE <table> ( <fields...>, <KeyType> ( <Keys...> ) )
        // Derby:      CREATE TABLE <table> ( <fields...>, <KeyType> ( <Keys...> ) )
        sb.append("CREATE TABLE ").append(xtableName).append(" (");

        /* fields */
        DBField autoIncrField = null;
        for (int fx = 0; fx < flds.length; fx++) {
            if (fx > 0) { sb.append(","); }
            sb.append(flds[fx].getFieldDefinition());
            if (dbProvID == DB_MYSQL) {
                // TODO: need to qualify other DBProviders
                if (flds[fx].isAutoIncrement()) {
                    autoIncrField = flds[fx];
                    sb.append(" NOT NULL");
                    sb.append(" auto_increment");
                    Print.logInfo("Adding 'auto_increment' key: " + xtableName+"."+autoIncrField.getName());
                } else
                if (flds[fx].isUTF8()) {
                    sb.append(" CHARACTER SET utf8");
                }
            }
        }

        /* primary keys */
        if ((priKeys != null) && (priKeys.length > 0)) {
            sb.append(", ");
            sb.append(keyType).append(" (");
            for (int pkx = 0; pkx < priKeys.length; pkx++) {
                if (pkx > 0) { sb.append(","); }
                sb.append(priKeys[pkx].getName());
            }
            sb.append(")");
        }

        /* table type */
        sb.append(")"); 
        String indexType = dbp.getTableIndexType();
        if (indexType != null) {
            sb.append(" ").append(indexType);
        }

        /* create table */
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDBConnection_write();
            dbc.executeUpdate(sb.toString());
        } finally {
            DBConnection.release(dbc);
        }

        /* alternate index (created separately) */
        if ((altIndexes != null) && (altIndexes.length > 0)) {
            for (int i = 0; i < altIndexes.length; i++) {
                DBProvider.createAlternateIndex(utableName, altIndexes[i]);
            }
        }

        /* separate "autoIndex" alternate index */
        if (autoIncrField != null) {
            // the "auto_increment" field must be part of the PRIMARY KEY
            // we also add a separate index for the auto-increment field
            // so that the index value is unique over the entire table.
            String altIndexName = "autoIncrement";
            Print.logInfo("Adding 'auto_increment' altIndex: " + xtableName + " " + altIndexName);
            DBField altNdxFlds[] = new DBField[] { autoIncrField };
            DBProvider.createAlternateIndex(utableName, altIndexName, altNdxFlds, true);
        }

    }

    /**
    *** Create an alternate Index
    *** @param utableName The untranslated table name
    *** @param altIndex   The alternate index
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createAlternateIndex(String utableName, DBAlternateIndex altIndex)
        throws SQLException, DBException
    {
        String  indexName   = altIndex.getIndexName(); // unfiltered
        DBField indexFlds[] = altIndex.getFields();
        boolean isUnique    = altIndex.isUnique();
        DBProvider.createAlternateIndex(utableName, indexName, indexFlds, isUnique);
    }
    
    /**
    *** Create an alternate Index
    *** @param utableName The unfiltered table name
    *** @param indexName  The alternate index name
    *** @param altKeys    The alternate keys (columns)
    *** @param isUnique   True if the index is unique
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    private static void createAlternateIndex(String utableName, String indexName, DBField altKeys[], boolean isUnique)
        throws SQLException, DBException
    {
        String xtableName = DBProvider.translateTableName(utableName);
        if ((altKeys != null) && (altKeys.length > 0)) {

            /* default index name */
            if (StringTools.isBlank(indexName)) { 
                indexName = DEFAULT_ALT_INDEX_NAME; 
            }

            /* index name filter */
            switch (DBProvider.getProvider().getID()) {
                case DB_DERBY:
                    // Derby: alternate index names must be unique across all tables
                    indexName = (utableName + "_" + indexName);
                    break;
            }

            /* KeyType */
            DBFactory.KeyType keyType = isUnique? DBFactory.KeyType.UNIQUE_INDEX : DBFactory.KeyType.INDEX;

            /* assemble SQL statement */
            // MySQL:      CREATE [UNIQUE] INDEX altIndex ON <table> (<key>[,<key>[,...]])
            // Derby:      CREATE [UNIQUE] INDEX altIndex ON <table> (<key>[,<key>[,...]])
            // SQLServer:  CREATE [UNIQUE] INDEX altIndex ON <table> (<key>[,<key>[,...]])
            // PostgreSQL: CREATE [UNIQUE] INDEX altIndex ON <table> (<key>[,<key>[,...]])
            //Print.logDebug("Creating ALternate Index: " + indexName);
            StringBuffer altSB = new StringBuffer();
            altSB.append("CREATE ");
            altSB.append(DBFactory.getKeyTypeName(keyType)).append(" ");
            altSB.append(indexName).append(" ");
            altSB.append("ON ").append(xtableName).append(" ");
            altSB.append("(");
            for (int akx = 0; akx < altKeys.length; akx++) {
                if (akx > 0) { altSB.append(","); }
                altSB.append(altKeys[akx].getName());
            }
            altSB.append(")");

            /* create index */
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDBConnection_write();
                dbc.executeUpdate(altSB.toString());
            } finally {
                DBConnection.release(dbc);
            }

        } else {

            Print.logWarn("Ignoring alternate index with no specified fields: " + xtableName + " " + indexName);

        }
    }

    /**
    *** Removes an alternate Index
    *** @param utableName The untranslated table name
    *** @param indexName  The alternate index name
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void removeAlternateIndex(String utableName, String indexName)
        throws SQLException, DBException
    {
        String xtableName = DBProvider.translateTableName(utableName);

        /* default index name */
        if (StringTools.isBlank(indexName)) { 
            indexName = DEFAULT_ALT_INDEX_NAME;
        }

        /* db provider filters */
        switch (DBProvider.getProvider().getID()) {
            case DB_SQLSERVER:
                // quote table name?
                //xtableName = "'" + xtableName + "'";
                break;
            case DB_DERBY:
                // Derby: alternate index names must be unique across all tables
                indexName = (utableName + "_" + indexName);
                break;
        }

        /* remove index */
        StringBuffer sb = new StringBuffer();
        // MySQL: ALTER TABLE <table> DROP INDEX altIndex
        sb.append("ALTER TABLE ").append(xtableName).append(" ");
        sb.append("DROP ");
        sb.append(DBFactory.getKeyTypeName(DBFactory.KeyType.INDEX));
        sb.append(" ");
        sb.append(indexName);
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDBConnection_write();
            dbc.executeUpdate(sb.toString());
        } finally {
            DBConnection.release(dbc);
        }

    }

    // ------------------------------------------------------------------------
    
    /**
    *** Create primary key Index (see createTable)
    *** @param dbFact The DBFactory
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void createPrimaryIndex(DBFactory<? extends DBRecord<?>> dbFact)
        throws SQLException, DBException
    {
        // MySQL: ALTER TABLE <table> ADD PRIMARY KEY ( <key>,<key>,... )
        DBField priKeys[] = (dbFact != null)? dbFact.getKeyFields() : null;
        if ((priKeys != null) && (priKeys.length > 0)) {
            String xtableName = dbFact.getTranslatedTableName();

            /* db provider filters */
            switch (DBProvider.getProvider().getID()) {
                case DB_SQLSERVER:
                    // quote table name?
                    //xtableName = "'" + xtableName + "'";
                    break;
            }

            /* ALTER TABLE ... */
            StringBuffer sb = new StringBuffer();
            sb.append("ALTER TABLE ").append(xtableName);
            sb.append(" ADD ").append(dbFact.getKeyType());
            sb.append(" (");
            for (int pkx = 0; pkx < priKeys.length; pkx++) {
                if (pkx > 0) { sb.append(","); }
                sb.append(priKeys[pkx].getName());
            }
            sb.append(")");

            /* execute change */
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDBConnection_write();
                dbc.executeUpdate(sb.toString());
            } finally {
                DBConnection.release(dbc);
            }

        }
    }

    /**
    *** Remove primary key Index
    *** @param  utableName The untranslated table name
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void removePrimaryIndex(String utableName)
        throws SQLException, DBException
    {
        String xtableName = DBProvider.translateTableName(utableName);

        /* db provider filters */
        // MySQL: ALTER TABLE <table> DROP PRIMARY KEY
        switch (DBProvider.getProvider().getID()) {
            case DB_SQLSERVER:
                // quote table name?
                //xtableName = "'" + xtableName + "'";
                break;
        }

        /* ALTER TABLE ... */
        StringBuffer altSB = new StringBuffer();
        altSB.append("ALTER TABLE ").append(xtableName).append(" ");
        altSB.append("DROP PRIMARY KEY");
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDBConnection_write();
            dbc.executeUpdate(altSB.toString());
        } finally {
            DBConnection.release(dbc);
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Drop (delete) the specified table
    *** @param utableName  The untranslated table name
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void dropTable(String utableName)
        throws SQLException, DBException
    {
        String xtableName = DBProvider.translateTableName(utableName);
        String drop = null;
        switch (DBProvider.getProvider().getID()) {
            case DB_MYSQL:
                // MySQL: DROP TABLE IF EXISTS <TableName>
                // no error is generated if the table does not exist
                drop = "DROP TABLE IF EXISTS " + xtableName;
                break;
            case DB_POSTGRESQL:
                // PostgreSQL: DROP TABLE <TableName>
                drop = "DROP TABLE " + xtableName;
                break;
            case DB_DERBY:
                // Derby: DROP TABLE <TableName>
                drop = "DROP TABLE " + xtableName;
                break;
            case DB_SQLSERVER:
                // SQLServer: DROP TABLE <TableName>
                drop = "DROP TABLE " + xtableName;
                break;
        }
        
        /* execue 'drop' statement */
        if (drop != null) {
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDBConnection_write();
                dbc.executeUpdate(drop);
            } catch (SQLException sqe) {
                String sqlMsg = sqe.getMessage();
                int errCode = sqe.getErrorCode();
                if (errCode == DBFactory.SQLERR_TABLE_NONEXIST) { // MySQL: ?
                    // ignore
                } else
                if (errCode == DBFactory.MSQL_ERR_CANT_DROP_TABLE) { // SQLServer: :
                    // ignore
                } else
                if (sqlMsg.indexOf("does not exist") >= 0) {
                    // ignore
                } else {
                    // re-throw
                    throw sqe;
                }
            } finally {
                DBConnection.release(dbc);
            }
        } else {
            Print.logError("SQL 'DROP' not supported");
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Replace record into table (MySQL only!)
    *** @param rec       The record to insert/update
    *** @param xTblName  The table name into which the record should be inserted/replaced (null for default)
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static <T extends DBRecord<T>> boolean replaceRecordIntoTable(DBRecord<T> rec, String xTblName)
        throws SQLException, DBException
    {
        DBRecordKey<T>      recKey          = rec.getRecordKey();
        String              xtableName      = !StringTools.isBlank(xTblName)? xTblName : recKey.getTranslatedTableName();
        DBFactory<T>        recFact         = recKey.getFactory();
        Map<String,DBField> existingColumns = recFact.getExistingColumnMap(false);
        DBField             field[]         = recKey.getFields();
        DBFieldValues       fieldValues     = recKey.getFieldValues();
        DBField             autoIncrField   = null;
        StringBuffer        sb              = new StringBuffer();

        /* is MySQL? */
        if (DBProvider.getProvider().getID() != DBProvider.DB_MYSQL) {
            throw new DBException("'REPLACE' supported only by MySQL");
        }

        /* replace */
        // MySQL:      REPLACE INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // SQLServer:  REPLACE INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // Derby:      REPLACE INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // PostgreSQL: REPLACE INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        sb.append("REPLACE ");
        sb.append("INTO ").append(xtableName);

        /* replaced columns */
        StringBuffer colSB = new StringBuffer();
        StringBuffer valSB = new StringBuffer();
        boolean addedField = false;
        for (int i = 0; i < field.length; i++) {
            String fldName = field[i].getName();
            String xFldName = DBProvider.translateColumnName(fldName);
            if (field[i].isAutoIncrement()) {
                // -- we skip 'auto_increment' fields on INSERT
                autoIncrField = field[i]; // "There must be only one"
                continue;
            }
            if ((existingColumns == null) || existingColumns.containsKey(xFldName)) {
                // -- insert existing columns
                if (addedField) { 
                    colSB.append(","); 
                    valSB.append(","); 
                }
                Object fldVal = fieldValues.getFieldValue(fldName,true);
                String dbVal  = field[i].getQValue(fldVal);
                //colSB.append(DBProvider.getProvider().getStartColumnChar());
                //colSB.append(fldName);
                //colSB.append(DBProvider.getProvider().getEndColumnChar());
                colSB.append(DBProvider.getProvider().quoteColumnName(fldName));
                valSB.append(dbVal);
                addedField = true;
            } else
            if (recFact.logMissingColumnWarning()) {
                // -- ignore non-existant columns
                Print.logWarn("Insert: Field does not exist: " + xtableName + "." + fldName + " [ignored]");
            }
        }
        sb.append(" (").append(colSB).append(")");
        sb.append(" VALUES (").append(valSB).append(")");

        /* execute */
        if (addedField) {
            //ResultSet rs = <Statement>.getGeneratedKeys();
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDBConnection_write();
                if (autoIncrField != null) {
                    long autoIncrVal = dbc.executeUpdate(sb.toString(), true);
                    if (autoIncrVal >= 0) {
                        Print.logDebug("Auto-Increment value: " + autoIncrVal);
                        fieldValues.setFieldValue(autoIncrField.getName(), autoIncrVal);
                    }
                } else {
                    dbc.executeUpdate(sb.toString());
                }
            } finally {
                DBConnection.release(dbc);
            }
            return true;
        } else {
            Print.logInfo("No columns specified to insert!!! [" + xtableName + "]");
            if (existingColumns != null) {
                Print.logInfo("Existing columns: ");
                for (Object fldName : existingColumns.keySet()) {
                    Print.logInfo("  Column " + fldName);
                }
            }
            return false;
        }
        
    }

    // --------------------------------

    /**
    *** Insert record into table
    *** @param rec The record to insert
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static <T extends DBRecord<T>> boolean insertRecordIntoTable(DBRecord<T> rec)
        throws SQLException, DBException
    {
        return DBProvider.insertRecordIntoTable(rec, null);
    }

    /**
    *** Insert record into table
    *** @param rec       The record to insert
    *** @param xTblName  The table name into which the record should be inserted/replaced (null for default)
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static <T extends DBRecord<T>> boolean insertRecordIntoTable(DBRecord<T> rec, String xTblName)
        throws SQLException, DBException
    {
        DBRecordKey<T>      recKey          = rec.getRecordKey();
        String              xtableName      = !StringTools.isBlank(xTblName)? xTblName : recKey.getTranslatedTableName();
        DBFactory<T>        recFact         = recKey.getFactory();
        Map<String,DBField> existingColumns = recFact.getExistingColumnMap(false);
        DBField             field[]         = recKey.getFields();
        DBFieldValues       fieldValues     = recKey.getFieldValues();
        DBField             autoIncrField   = null;
        StringBuffer        sb              = new StringBuffer();

        /* insert */
        // MySQL:      INSERT [IGNORE] INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // SQLServer:  INSERT [IGNORE] INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // Derby:      INSERT [IGNORE] INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        // PostgreSQL: INSERT [IGNORE] INTO <table> (<column>,<column>,...) VALUES (<value>,<value>,...)
        sb.append("INSERT ");
        if (recKey.getIgnoreOnInsert()) {
            // -- https://dev.mysql.com/doc/refman/5.7/en/insert.html
            // -  "If you use the IGNORE modifier, errors that occur while executing the 
            // -  INSERT statement are ignored.  For example, without IGNORE, a row that 
            // -  duplicates an existing UNIQUE index or PRIMARY KEY value in the table 
            // -  causes a duplicate-key error and the statement is aborted. With IGNORE, 
            // -  the row is discarded and no error occurs.  Ignored errors generate 
            // -  warnings instead."
            sb.append("IGNORE ");
        }
        sb.append("INTO ").append(xtableName);

        /* inserted columns */
        StringBuffer colSB = new StringBuffer();
        StringBuffer valSB = new StringBuffer();
        boolean addedField = false;
        for (int i = 0; i < field.length; i++) {
            String fldName = field[i].getName();
            String xFldName = DBProvider.translateColumnName(fldName);
            if (field[i].isAutoIncrement()) {
                // -- we skip 'auto_increment' fields on INSERT
                autoIncrField = field[i]; // "There must be only one"
                continue;
            }
            if ((existingColumns == null) || existingColumns.containsKey(xFldName)) {
                // -- insert existing columns
                if (addedField) { 
                    colSB.append(","); 
                    valSB.append(","); 
                }
                Object fldVal = fieldValues.getFieldValue(fldName,true);
                String dbVal  = field[i].getQValue(fldVal);
                //colSB.append(DBProvider.getProvider().getStartColumnChar());
                //colSB.append(fldName);
                //colSB.append(DBProvider.getProvider().getEndColumnChar());
                colSB.append(DBProvider.getProvider().quoteColumnName(fldName));
                valSB.append(dbVal);
                addedField = true;
            } else
            if (recFact.logMissingColumnWarning()) {
                // -- ignore non-existant columns
                Print.logWarn("Insert: Field does not exist: " + xtableName + "." + fldName + " [ignored]");
            }
        }
        sb.append(" (").append(colSB).append(")");
        sb.append(" VALUES (").append(valSB).append(")");
            
        /* MySQL also supports this version of "INSERT" */
        // MySQL: INSERT INTO <table> SET <column>=<value>, ...
        /*
        sb.append("INSERT INTO ").append(xtableName).append(" SET ");
        for (int i = 0; i < field.length; i++) {
            String fldName = field[i].getName();
            if ((existingColumns == null) || existingColumns.containsKey(fldName)) {
                // insert existing columns
                if (addedField) { sb.append(", "); }
                Object fldVal = fieldValues.getFieldValue(fldName,true);
                String dbVal  = field[i].getQValue(fldVal);
                sb.append(fldName).append("=").append(dbVal);
                addedField = true;
            } else
            if (recFact.logMissingColumnWarning()) {
                // ignore non-existant columns
                Print.logWarn("Insert: Field does not exist: " + xtableName + "." + fldName + " [ignored]");
            }
        }
        */

        /* execute */
        if (addedField) {
            //ResultSet rs = <Statement>.getGeneratedKeys();
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDBConnection_write();
                if (autoIncrField != null) {
                    long autoIncrVal = dbc.executeUpdate(sb.toString(), true);
                    if (autoIncrVal >= 0) {
                        Print.logDebug("Auto-Increment value: " + autoIncrVal);
                        fieldValues.setFieldValue(autoIncrField.getName(), autoIncrVal);
                    }
                } else {
                    dbc.executeUpdate(sb.toString());
                }
            } finally {
                DBConnection.release(dbc);
            }
            return true;
        } else {
            Print.logInfo("No columns specified to insert!!! [" + xtableName + "]");
            if (existingColumns != null) {
                Print.logInfo("Existing columns: ");
                for (Object fldName : existingColumns.keySet()) {
                    Print.logInfo("  Column " + fldName);
                }
            }
            return false;
        }
        
    }

    // --------------------------------

    /**
    *** Assemble an update statement (and execute) for the specified record and
    *** array of fields to update
    *** @param rec The record to update
    *** @param updFldArray The array of fields to update
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static <T extends DBRecord<T>> boolean updateRecordInTable(DBRecord<T> rec, String updFldArray[])
        throws SQLException, DBException
    {
       
        /* fields */
        // -- TODO: the process of determining which fields to update could be better optimized
        Set<String> updFldSet = null;
        if (updFldArray != null) {
            updFldSet = new HashSet<String>();
            for (int i = 0; i < updFldArray.length; i++) { 
                // -- ignore field entries which are null
                if (updFldArray[i] != null) {
                    updFldSet.add(updFldArray[i]); 
                }
            }
        }

        /* update fields */
        return DBProvider.updateRecordInTable(rec, updFldSet);

    }
    
    /**
    *** Assemble an update statement (and execute) for the specified record and
    *** set of fields to update
    *** @param rec The record to update
    *** @param updFldSet The set of fields to update
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static <T extends DBRecord<T>> boolean updateRecordInTable(DBRecord<T> rec, Set<String> updFldSet)
        throws SQLException, DBException
    {
        DBRecordKey<T> recKey     = rec.getRecordKey();
        String         xtableName = recKey.getTranslatedTableName();
        StringBuffer   sb         = new StringBuffer();

        /* update */
        // MySQL:      UPDATE <table> SET <column>=<value>, ...
        // SQLServer:  UPDATE <table> SET <column>=<value>, ...
        // PostgreSQL: UPDATE <table> SET <column>=<value>, ...
        // Derby:      UPDATE <table> SET <column>=<value>, ...
        sb.append("UPDATE ");
        if (recKey.getIgnoreOnUpdate()) {
            // -- https://dev.mysql.com/doc/refman/5.7/en/update.html
            // -  "With the IGNORE modifier, the update statement does not abort even if errors occur 
            // -  during the update. Rows for which duplicate-key conflicts occur on a unique key 
            // -  value are not updated. Rows updated to values that would cause data conversion errors 
            // -  are updated to the closest valid values instead."
            // -  see https://dev.mysql.com/doc/refman/5.7/en/sql-mode.html#ignore-strict-comparison
            sb.append("IGNORE ");
        }
        sb.append(xtableName);

        /* set columns */
        sb.append(" SET ");
        DBFactory<T>        recFact         = recKey.getFactory();
        Map<String,DBField> existingColumns = recFact.getExistingColumnMap(false);
        DBFieldValues       fieldValues     = recKey.getFieldValues();
        DBField             field[]         = recKey.getFields();
        boolean             addedField      = false;
        for (int i = 0; i < field.length; i++) {
            String fldName = field[i].getName();
            // -- check explicit update field set
            if ((updFldSet != null) && !updFldSet.contains(fldName)) {
                continue;
            }
            // -- skip fields that are not allowed to update
            if (!field[i].isUpdateAllowed()) { // 2.5.2-B10
                if (updFldSet != null) {
                    // -- field explicitly specified, display disallowed update warning
                    String type = field[i].isPrimaryKey()? "Primary key" : "Field";
                    Print.logWarn(type + " update not allowed: " + xtableName + "." + fldName + " [ignored]");
                }
                continue;
            }
            // -- skip primary keys
            /* OBSOLETED BY ABOVE "isUpdateAllowed"
            if (field[i].isPrimaryKey()) {
                // ignore update to primary key (if not explicitly allowed)
                if (updFldSet != null) {
                    // field explicitly specified, display disallowed update warning
                    Print.logWarn("Primary key update not allowed: " + xtableName + "." + fldName + " [ignored]");
                } 
                continue;
            }
            */
            // -- skip auto-increment fields
            if (field[i].isAutoIncrement()) {
                // -- quietly skip 'auto_increment' fields
                continue;
            }
            // -- explicit excluded field?
            if (updFldSet == null) {
                if (rec.excludeFieldFromUpdate(field[i])) {
                    // -- quietly skip excluded update fields (only if explicit update field are not specified)
                    continue;
                } else
                if (fldName.equals(DBRecord.FLD_creationTime)) {
                    // -- "creationTime" should only be set at the record creation
                    continue;
                } else
                if (fldName.equals(DBRecord.FLD_creationMillis)) {
                    // -- "creationMillis" should only be set at the record creation
                    continue;
                }
            }
            // -- check column existance
            if ((existingColumns != null) && !existingColumns.containsKey(fldName)) {
                // -- ignore non-existant columns (ie. field defined in Java class, but not in MySQL table)
                if (recFact.logMissingColumnWarning()) {
                    Print.logWarn("Update: Field does not exist: " + xtableName + "." + fldName + " [ignored]");
                }
                continue;
            }
            // -- update existing columns
            if (addedField) { sb.append(", "); }
            Object fldVal = fieldValues.getFieldValue(fldName,true);
            String dbVal  = field[i].getQValue(fldVal);
            //sb.append(DBProvider.getProvider().getStartColumnChar());
            //sb.append(fldName);
            //sb.append(DBProvider.getProvider().getEndColumnChar());
            sb.append(DBProvider.getProvider().quoteColumnName(fldName));
            sb.append("=").append(dbVal);
            addedField = true;
        } // updated field loop

        /* where */
        sb.append(recKey.getWhereClause(DBWhere.KEY_FULL));
        //Print.logInfo("Update SQL: " + sb);

        /* execute */
        if (addedField) {
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDBConnection_write();
                dbc.executeUpdate(sb.toString());
            } catch (SQLException sqle) {
                Print.logError("SQL(ExecuteUpdate): " + sb);
                throw sqle; // rethrow exception
            } catch (DBException dbe) {
                Print.logError("SQL(ExecuteUpdate): " + sb);
                throw dbe; // rethrow exception
            } finally {
                DBConnection.release(dbc);
            }
            return true;
        } else {
            Print.logInfo("No columns specified to update!!! [" + xtableName + "]");
            return false;
        }
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // grant access to database (needed for initialization only)
    
    /** Execute an SQL statement granting access to the DB user (needed for 
    *** initialization only) 
    *** @param rootUser The root user accoount
    *** @param rootPass The root user password
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static void grantDBUser(String rootUser, String rootPass)
        throws DBException, SQLException
    {
        String grantUser = DBProvider.getDBUsername();
        String grantPass = DBProvider.getDBPassword();
        String dbName    = DBProvider.getDBName();

        /* validate arguments */
        if (StringTools.isBlank(rootUser)) {
            throw new DBException("Root user not specified");
        } else
        if (StringTools.isBlank(grantUser)) {
            throw new DBException("User not specified for GRANT access");
        } else
        if (grantUser.equalsIgnoreCase("root")) {
            throw new DBException("Refusing to change privileges for 'root'");
        } else
        if (StringTools.isBlank(dbName)) {
            throw new DBException("No database name specified");
        }

        /* connection */
        String dbUri = DBProvider.getDBUri(false);
        DBConnection dbc = DBConnection.getDBConnection_write(dbUri, rootUser, rootPass);

        /* grant */
        try {
            switch (DBProvider.getProvider().getID()) {
                case DB_MYSQL: {
                    // MySQL:
                    //   GRANT ALL ON <dbname>.* TO <user>@localhost IDENTIFIED BY '<password>' WITH GRANT OPTION;
                    //   GRANT ALL ON <dbname>.* TO <user>@"%" IDENTIFIED BY '<password>' WITH GRANT OPTION;
                    //   FLUSH PRIVILEGES;
                    // Default Example:
                    //   GRANT ALL ON gts.* TO gts@localhost IDENTIFIED BY 'opengts' WITH GRANT OPTION;
                    //   GRANT ALL ON gts.* TO gts@"%" IDENTIFIED BY 'opengts' WITH GRANT OPTION;
                    //   FLUSH PRIVILEGES;
                    String grantS = "GRANT ALL ON " + dbName + ".* TO " + grantUser + "@";
                    String grantE = " IDENTIFIED BY " + DBField.quote(grantPass) + " WITH GRANT OPTION;";
                    dbc.executeUpdate(grantS + "localhost" + grantE);
                    dbc.executeUpdate(grantS + DBField.quote("%") + grantE);
                    dbc.executeUpdate("FLUSH PRIVILEGES;");
                    break;
                }
                case DB_POSTGRESQL: {
                    // PostgreSQL: ?
                    String grantS = "GRANT ALL ON " + dbName + " TO " + grantUser;
                    String grantE = " WITH GRANT OPTION;";
                    dbc.executeUpdate(grantS + grantE);
                    break;
                }
                case DB_DERBY: {
                    // Derby: ?
                    break;
                }
                case DB_SQLSERVER: {
                    // SQLServer: CREATE LOGIN <user> WITH PASSWORD='<password>', DEFAULT_DATABASE=[<dbname>]
                    try {
                        String createLogin = "CREATE LOGIN " + grantUser + " WITH PASSWORD='" + grantPass + "', DEFAULT_DATABASE=[" + dbName + "]";
                        dbc.executeUpdate(createLogin);
                    } catch (SQLException sqe) {
                        int errCode = sqe.getErrorCode();
                        if (errCode == DBFactory.MSQL_ERR_LOGIN_EXISTS) {
                            Print.logWarn("This login already exists: " + grantUser);
                        } else {
                            throw sqe;
                        }
                    }
                    // SQLServer: USE <dbname>; CREATE USER <user> FOR LOGIN <user>
                    try {
                        String createUser  = "USE " + dbName + "; CREATE USER " + grantUser + " FOR LOGIN " + grantUser;
                        dbc.executeUpdate(createUser);
                    } catch (SQLException sqe) {
                        int errCode = sqe.getErrorCode();
                        if (errCode == DBFactory.MSQL_ERR_USER_EXISTS) {
                            Print.logWarn("This user already exists: " + grantUser);
                        } else {
                            throw sqe;
                        }
                    }
                    // SQLServer: USE <dbname>; ALTER ROLE [db_owner] ADD MEMBER [<user>]
                    {
                        // -- required for SQLServer 2012
                        // -- not supported by SQLServer 2005
                        String alterRole = "USE " + dbName + "; ALTER ROLE [db_owner] ADD MEMBER [" + grantUser + "]";
                        try {
                            // "ALTER ROLE db_owner ADD MEMBER gts" should work as well.
                            dbc.executeUpdate(alterRole);
                        } catch (SQLException sqe) {
                            // -- will fail for 2005 SQLServer
                            Print.logException("Alter Role failed: " + alterRole + " (continuing...)", sqe);
                            //throw sqe; <== removed for v2.5.4-B26
                        }
                    }
                    // SQLServer: USE <dbname>; GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES TO <user>
                    try {
                        String perm = "SELECT,INSERT,UPDATE,DELETE,REFERENCES";
                        String grantAll = "USE " + dbName + "; GRANT " + perm + " TO " + grantUser;
                        dbc.executeUpdate(grantAll);
                    } catch (SQLException sqe) {
                        throw sqe;
                    }
                    break;
                }
            }
        } finally {
            DBConnection.release(dbc);
        }

        /* log success */
        Print.logInfo("Privileges granted to user: " + grantUser);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // attempt to obtain the database server default cahracter set
    
    /**
    *** Gets the database provider default character encoding.  The returned
    *** String has the format "&gt;CharacterSet&lt;[&gt;Collation&lt;]"
    *** @return The database provider default character encoding.
    *** @throws SQLException  If an SQL error occurs
    *** @throws DBException   If a database error occurs
    **/
    public static String getDefaultCharacterSet()
        throws DBException, SQLException
    {
        String user   = DBProvider.getDBUsername();
        String pass   = DBProvider.getDBPassword();
        String dbName = DBProvider.getDBName();

        /* validate arguments */
        if (StringTools.isBlank(user)) {
            throw new DBException("User not specified");
        } else
        if (StringTools.isBlank(dbName)) {
            throw new DBException("No database name specified");
        }

        /* character set */
        StringBuffer charSet = new StringBuffer();
        DBConnection dbc = null;
        try {
            dbc = DBConnection.getDBConnection_read();
            Statement stmt = null;
            ResultSet rs   = null;
            switch (DBProvider.getProvider().getID()) {
                case DB_MYSQL: {
                    // character set
                    try {
                        stmt = dbc.execute("SHOW VARIABLES LIKE 'character_set_database'");
                        rs   = stmt.getResultSet();
                        if (rs.next()) {
                            charSet.append(StringTools.trim(rs.getString("Value")));
                        }
                    } finally {
                        if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
                        if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
                    }
                    // collation
                    try {
                        stmt = dbc.execute("SHOW VARIABLES LIKE 'collation_database'");
                        rs   = stmt.getResultSet();
                        if (rs.next()) {
                            charSet.append("[").append(StringTools.trim(rs.getString("Value"))).append("]");
                        }
                    } finally {
                        if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
                        if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
                    }
                    break;
                }
            }
        } finally {
            DBConnection.release(dbc);
        }
        
        /* return character set */
        return charSet.toString();

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Table lock/unlock
    // SQLServer: http://msdn2.microsoft.com/en-us/library/aa213026(SQL.80).aspx
    // Note: By default, table locking is disabled.

    private static Boolean          lockingEnabled = null;
    private static Stack<Throwable> lockLevel      = new Stack<Throwable>();
    private static int              lockSeq        = 0;
    private static int              unlockSeq      = 0;

    /**
    *** Returns true if table locking is enabled
    *** @return True if table locking is enabled
    **/
    public static boolean isTableLockingEnabled()
    {
        if (DBConnection.ALWAYS_NEW_CONNECTION) {
            // If DBConnection.ALWAYS_NEW_CONNECTION is 'true', then
            // return 'false'. Otherwise table deadlocks _will_ occur.
            Print.logDebug("Table locking is disabled (per DBConnection.ALWAYS_NEW_CONNECTION)");
            return false;
        } else {
            if (lockingEnabled == null) {
                lockingEnabled = new Boolean(RTConfig.getBoolean(RTKey.DB_TABLE_LOCKING)); // "db.tableLocking"
                Print.logDebug("Table locking is " + (lockingEnabled.booleanValue()?"enabled":"disabled"));
            }
            return lockingEnabled.booleanValue();
        }
    }

    /**
    *** Returns the current recursive lock level
    *** @return The current recursive lock level
    **/
    public static int getLockLevel()
    {
        return DBProvider.lockLevel.size();
    }

    /**
    *** Lock specified tables for write/read
    *** @param writeTables The array of tables to lock for writing
    *** @param readTables The array of tables to lock for reading
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    **/
    public static boolean lockTables(String writeTables[], String readTables[])
        throws DBException
    {
        return DBProvider._lockTables(writeTables, readTables, false);
    }
    
    /**
    *** Lock specified tables for write/read
    *** @param writeTables The array of untranslated table names to lock for writing
    *** @param readTables  The array of untranslated table names to lock for reading
    *** @param forceLock If the lock query should be attemped whether or not 
    ***        locking is enabled
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    **/
    private static boolean _lockTables(String writeTables[], String readTables[], boolean forceLock)
        throws DBException
    {

        /* keep track of how many times we attempt a lock (this is never decremented) */
        ++DBProvider.lockSeq;
        //Print.logDebug("Lock Sequence: " + DBProvider.lockSeq);
        
        /* nothing to lock */
        if ((writeTables == null) && (readTables == null)) {
            return false; 
        }

        /* supported by DBProvider? */
        DBProvider dbp = DBProvider.getProvider();
        int dbid = dbp.getID();
        if (dbid != DBProvider.DB_MYSQL) {
            return false;
        }

        /* check recursive locking */
        if (!DBProvider.lockLevel.empty()) {
            // -- You amy get this message when this section is acessed by multiple threads
            Print.logStackTrace("Locking: Lock-Level is not empty!");
            Print.logStackTrace("Location of prior table locking", DBProvider.lockLevel.peek());
        }

        try {

            // -- MySQL: LOCK TABLES ...
            StringBuffer sb = new StringBuffer();
            sb.append("LOCK TABLES ");

            /* lock writes */
            if (writeTables != null) {
                for (int w = 0; w < writeTables.length; w++) {
                    if (w > 0) { sb.append(","); }
                    // -- MySQL: [LOCK TABLES] ..., <table> WRITE
                    String xtableName = DBProvider.translateTableName(writeTables[w]);
                    sb.append(xtableName).append(" WRITE");
                }
            }

            /* lock reads */
            if (readTables != null) {
                if (writeTables != null) { sb.append(","); }
                for (int r = 0; r < readTables.length; r++) {
                    if (r > 0) { sb.append(","); }
                    // -- MySQL: [LOCK TABLES] ..., <table> READ
                    String xtableName = DBProvider.translateTableName(readTables[r]);
                    sb.append(xtableName).append(" READ");
                }
            }

            /* execute locking */
            if (forceLock || DBProvider.isTableLockingEnabled()) {
                Throwable t = new Throwable();
                t.fillInStackTrace();
                DBConnection dbc = null;
                try {
                    dbc = DBConnection.getDBConnection_readWrite(); // depends on next operation
                    dbc.executeUpdate(sb.toString());
                } finally {
                    DBConnection.release(dbc);
                }
                DBProvider.lockLevel.push(t);
            }
            
            return true;
            
        } catch (SQLException sqe) {
            //this.setLastCaughtSQLException(sqe);
            Print.logSQLError("Read/Write lock", sqe);
            return false;
        }
        
    }

    /**
    *** Lock specified tables for read
    *** @param utableName The untranslated table to lock for reading
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    **/
    // -- Only called by "<DBRecordKey>.exists()"
    public static boolean lockTableForExists(String utableName)
        throws DBException
    {
        return DBProvider._lockTables(null, new String[] { utableName }, true/*forceLock*/);
    }

    /**
    *** Unlock locked tables
    *** @return True if the query was successful
    *** @throws DBException   If a database error occurs
    **/
    public static boolean unlockTables()
        throws DBException
    {

        /* keep track of how many times we attempt an unlock (this is never decremented) */
        ++DBProvider.unlockSeq;
        //Print.logDebug("UnLock Sequence: " + DBProvider.unlockSeq);

        /* supported by DBProvider? */
        DBProvider dbp = DBProvider.getProvider();
        int dbid = dbp.getID();
        if (dbid != DBProvider.DB_MYSQL) {
            return false;
        }

        /* lock tables */
        try {
            if (DBProvider.isTableLockingEnabled() || !DBProvider.lockLevel.empty()) {
                // MySQL: UNLOCK TABLES
                String unlockSql = "UNLOCK TABLES";
                DBConnection dbc = null;
                try {
                    dbc = DBConnection.getDBConnection_readWrite(); // depends on previous operation
                    dbc.executeUpdate(unlockSql);
                } finally {
                    DBConnection.release(dbc);
                }
                if (!DBProvider.lockLevel.empty()) {
                    DBProvider.lockLevel.pop();
                } else {
                    Print.logStackTrace("Lock-Level stack is empty");
                }
            }
            if (!DBProvider.lockLevel.empty()) { 
                // TODO: this could get out-of-sync, if the above had previously thrown an exception
                Print.logStackTrace("Unlock: Lock-Level is not empty!"); 
            }
            return true;
        } catch (SQLException sqe) {
            //this.setLastCaughtSQLException(sqe);
            Print.logSQLError("unlock tables", sqe);
            return false;
        }
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int                 jdbcId              = 0;
    private String              jdbcName            = null;
    private String              jdbcDrivers[]       = null;
    private String              tableIndexType      = null;
    private String              dataTypes[]         = null;
    
    private long                jdbcFlags           = 0x0000L;
    private int                 defaultPort         = 0;
    private NameFilter          tableNameFilter     = null;
    private NameFilter          columnNameFilter    = null;
    private NameFilter          indexNameFilter     = null;

    /**
    *** Constructor
    *** @param name       The JDBC name
    *** @param id         The JDBC ID
    *** @param dftPort    The default connection port
    *** @param drivers    The JDBC drivers
    *** @param colNameFlt The column name filter
    *** @param ndxNameFlt The index name filter
    *** @param tblNameFlt The table name filter
    *** @param tblNdxType The table idex type ("CREATE TABLE" prefix)
    *** @param flags      The JDBC flags
    *** @param dataTypes  The array of data type names
    *** @throws RuntimeException If <code>dataTypes</code> is not the correct 
    ***         length
    **/
    public DBProvider(
        String name, int id, int dftPort,
        String drivers[], 
        NameFilter colNameFlt,  // column name filter
        NameFilter ndxNameFlt,  // index name filter
        NameFilter tblNameFlt,  // table name filter
        String tblNdxType,
        long flags,             // FLAGS_LIMIT
        String dataTypes[])
    {
        super();
        
        /* vars */
        this.jdbcId           = id;
        this.jdbcName         = name;
        this.jdbcDrivers      = drivers;
        this.jdbcFlags        = flags;
        this.defaultPort      = dftPort;
        this.columnNameFilter = colNameFlt;
        this.indexNameFilter  = ndxNameFlt;
        this.tableNameFilter  = tblNameFlt;
        this.tableIndexType   = (tblNdxType != null)? tblNdxType : "";
        
        /* data types */
        this.dataTypes        = dataTypes;
        if ((this.dataTypes == null) || (this.dataTypes.length != TYPE_ARRAY.length)) {
            // This is serious enough to stop everything right here!
            String msg = "Invalid number of data-types for DBProvider: " + this.jdbcName;
            Print.logStackTrace(msg);
            throw new RuntimeException(msg);
        }
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the JDBC ID
    *** @return The JDBC ID
    **/
    public int getID()
    {
        return this.jdbcId;
    }
    
    /**
    *** Gets the JDBC name
    *** @return The JDBC name
    **/
    public String getJDBCName()
    {
        return this.jdbcName;
    }
    
    /**
    *** Gets the JDBC drivers
    *** @return The JDBC drivers
    **/
    public String[] getDrivers()
    {
        return this.jdbcDrivers;
    }
    
    /**
    *** Gets the default connection port. Returns the port from RTConfig, if 
    *** defined; else returns the DBProvider's assigned default port
    *** @return The default connection port
    **/
    public int getDefaultPort()
    {
        if (RTConfig.hasProperty(RTKey.DB_PORT,false)) {
            return RTConfig.getInt(RTKey.DB_PORT);
        } else {
            return this.defaultPort;
        }
    }
    
    /**
    *** Gets the column name filter
    *** @return The column name filter
    **/
    public NameFilter getColumnNameFilter()
    {
        return this.columnNameFilter;
    }

    /**
    *** Gets the table name filter
    *** @return The table name filter
    **/
    public NameFilter getTableNameFilter()
    {
        return this.tableNameFilter;
    }

    /**
    *** Gets the table index type
    *** @return The table index type
    **/
    public String getTableIndexType()
    {
        return this.tableIndexType;
    }
    
    /**
    *** Gets an array of the SQL datatypes
    *** @return An array of the SQL datatypes
    **/
    public String[] getSQLTypes()
    {
        return this.dataTypes;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the JDBC flags
    *** @return The JDBC flags
    **/
    public long getFlags()
    {
        return this.jdbcFlags;
    }
    
    /**
    *** Returns true if the DBProvider supports limits
    *** @return True if the DBProvider supports limits
    **/
    public boolean supportsLimit()
    {
        return ((this.jdbcFlags & FLAGS_LIMIT) != 0);
    }
    
    /**
    *** Returns true if the DBProvider supports offsets
    *** @return True if the DBProvider supports offsets
    **/
    public boolean supportsOffset()
    {
        return ((this.jdbcFlags & FLAGS_OFFSET) != 0);
    }

    // ------------------------------------------------------------------------

    /*
    public String getStartColumnChar ()
    {
        switch(DBProvider.getProvider().getID()) {
            case DB_POSTGRESQL:
                return "\"";
            case DB_MYSQL:
                return "`";
            case DB_SQLSERVER:
                return "[";
            case DB_DERBY:
                return ""; // dont know.
            default:
                return "";
        }
    } */

    /*
    public String getEndColumnChar ()
    {
        switch(DBProvider.getProvider().getID()) {
            case DB_POSTGRESQL:
                return "\"";
            case DB_MYSQL:
                return "`";
            case DB_SQLSERVER:
                return "]";
            case DB_DERBY:
                return ""; // dont know.
            default:
                return "";
        }
    } */

    /**
    *** Quotes the column name based on the requirements of the DBProvider
    *** @param colName  The column name
    *** @return The quoted column name
    **/
    public String quoteColumnName(String colName)
    {
        switch(DBProvider.getProvider().getID()) {
            case DB_POSTGRESQL:
                return "\"" + colName + "\"";
            case DB_MYSQL:
                return colName; // "`" + colName + "`";
            case DB_SQLSERVER:
                return colName; // "[" + colName + "]";
            case DB_DERBY:
                return colName; // dont know.
            default:
                return colName;
        }
    }

    /**
    *** Quotes the table name based on the requirements of the DBProvider
    *** @param tblName  The column name
    *** @return The quoted column name
    **/
    public String quoteTableName(String tblName)
    {
        switch(DBProvider.getProvider().getID()) {
            case DB_POSTGRESQL:
                return "\"" + tblName + "\"";
            case DB_MYSQL:
                return tblName; // "`" + colName + "`";
            case DB_SQLSERVER:
                return tblName; // "[" + colName + "]";
            case DB_DERBY:
                return tblName; // dont know.
            default:
                return tblName;
        }
    }

    // ------------------------------------------------------------------------

}
