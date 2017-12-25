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
// Description:
//  JSON data format support.
//  This version includes support for the following additional non-standard features:
//      - Commented areas can be specified within /* ... */
//      - Hexadecimal value number types are supported (ie. 0xABCD)
// ----------------------------------------------------------------------------
// Change History:
//  2011/07/15  Martin D. Flynn
//     -Initial release
//  2011/08/21  Martin D. Flynn
//     -Fixed JSON parsing.
//  2011/10/03  Martin D. Flynn
//     -Added multiple-name lookup support
//  2013/03/01  Martin D. Flynn
//     -Added 'null' object support
//  2013/04/08  Martin D. Flynn
//     -Handle parsing of arrays within arrays
//  2013/08/06  Martin D. Flynn
//     -Added "JSONParsingContext" for easier debugging of syntax errors
//     -Added support for "/*...*/" comments (NOTE: this is a non-standard feature
//      which is NOT supported by other JSON parsers, including JavaScript).
//  2013/11/11  Martin D. Flynn
//     -Added additional overflow checking.
//  2014/09/25  Martin D. Flynn
//     -Added "toString(boolean inclPrefix)" to JSON object.
//  2016/09/01  Martin D. Flynn
//     -Added support to "parse_Number" for parsing hex integers (non-standard)
//  2017/02/05  Martin D. Flynn
//     -Added reading JSON object from URL (se "ReadJSON")
//     -Added JSON object path traversal (see "GetValueForPath")
//     -Disallow control characters in String values (ie. embedded '\n', etc)
//     -Escape special key String characters when displaying JSON
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

public class JSON
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified value represents scalar quantity.
    *** Includes types: String, Number(Double,Long,etc), and Boolean
    **/
    public static boolean IsScalarValue(Object val) {
        if (val instanceof String) {
            return true;
        } else
        if (val instanceof Number) { // Double, Long, etc.
            return true;
        } else
        if (val instanceof Boolean) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final int MAX_READ_LEN = 100000;

    /**
    *** Reads a JSON object from the specified URL
    *** @param url        The URL from which the JSON object is read
    *** @param timeoutMS  The specified timeout (in milliseconds)
    *** @return The JSON object
    *** @throws MalformedURLException      If an invalid URL syntax is specified
    *** @throws HTMLTools.HttpIOException  If a not-found(404) or forbidden(403) error occurs
    *** @throws IOException                If a general IO error occurs
    *** @throws JSON.JSONParsingException  If a JSON parsing exception occurs
    **/
    public static JSON ReadJSON_GET(
        URL url, 
        int timeoutMS)
        throws JSON.JSONParsingException, HTMLTools.HttpIOException, IOException
    {
        JSON   jsonObj = null;
        String jsonStr = null;
        try {
            byte b[] = HTMLTools.readPage_GET(
                url,
                timeoutMS, MAX_READ_LEN);
            jsonStr  = StringTools.toStringValue(b);
            jsonObj  = new JSON(jsonStr);
        } catch (JSON.JSONParsingException jpe) {
            // -- invalid JSON object
            //Print.logError("Invalid JSON object (GET):\n" + jsonStr);
            throw jpe;
        } catch (HTMLTools.HttpIOException hioe) {
            // -- possible not-found(404) or forbidden(403) error 
            throw hioe;
        } catch (MalformedURLException mue) {
            // -- invalid URL format
            throw mue;
        } catch (IOException ioe) {
            // -- general IO error
            throw ioe;
        }
        return jsonObj;
    }

    /**
    *** Reads a JSON object from the specified URL
    *** @param url         The URL from which the JSON object is read
    *** @param contentType The MIME type of the POST data sent to the server
    *** @param postData    Data to send in a POST, if null then GET will be used
    *** @param timeoutMS   The specified timeout (in milliseconds)
    *** @return The JSON object
    *** @throws MalformedURLException      If an invalid URL syntax is specified
    *** @throws HTMLTools.HttpIOException  If a not-found(404) or forbidden(403) error occurs
    *** @throws IOException                If a general IO error occurs
    *** @throws JSON.JSONParsingException  If a JSON parsing exception occurs
    **/
    public static JSON ReadJSON_POST(
        URL url, 
        String contentType, byte postData[], 
        int timeoutMS)
        throws JSON.JSONParsingException, HTMLTools.HttpIOException, IOException
    {
        JSON   jsonObj = null;
        String jsonStr = null;
        try {
            byte b[] = HTMLTools.readPage_POST(
                url, 
                contentType, postData, 
                timeoutMS, MAX_READ_LEN);
            jsonStr  = StringTools.toStringValue(b);
            jsonObj  = new JSON(jsonStr);
        } catch (JSON.JSONParsingException jpe) {
            // -- invalid JSON object
            //Print.logError("Invalid JSON object (POST): " + jpe + "\n" + jsonStr);
            throw jpe;
        } catch (HTMLTools.HttpIOException hioe) {
            // -- possible not-found(404) or forbidden(403) error 
            throw hioe;
        } catch (MalformedURLException mue) {
            // -- invalid URL format
            throw mue;
        } catch (IOException ioe) {
            // -- general IO error
            throw ioe;
        }
        return jsonObj;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Traverses the specified JSON Object/Array and returns the Value found
    *** at the specified path.
    **/
    private static JSON._Value GetValueForPath(Object obj, String path) 
    {
        if (!StringTools.isBlank(path)) {
            char sep = (path.indexOf("/") >= 0)? '/' : '.';
            return JSON.GetValueForPath(obj, StringTools.split(path,sep));
        } else {
            return null;
        }
    }

    /**
    *** Traverses the specified JSON Object/Array and returns the Value found
    *** at the specified path.
    **/
    private static JSON._Value GetValueForPath(Object obj, String... path) 
    {
        // -- no Object?
        if (obj == null) {
            return null;
        }
        // -- no path?
        if (path == null) {
            return null;
        }
        // -- traverse path
        JSON._Value val = null;
        for (int p = 0; p < path.length; p++) {
            if (obj instanceof JSON._Object) {
                // -- JSON Object
                JSON._Object target = (JSON._Object)obj;
                JSON._KeyValue kv = target.getKeyValue(path[p]);
                if (kv == null) {
                    // -- path not found
                    return null;
                }
                val = kv.getValue();
            } else
            if (obj instanceof JSON._Array) {
                // -- JSON Array
                JSON._Array target = (JSON._Array)obj;
                int ndx = StringTools.parseInt(path[p],-1);
                if ((ndx < 0) || (ndx >= target.size())) {
                    // -- outside of array bounds
                    return null;
                }
                val = target.getValueAt(ndx);
            } else {
                // -- cannot traverse a scalar type
                return null;
            }
            // -- next Object
            obj = val.getJavaObject();
        }
        return val;
    }

    // ------------------------------------------------------------------------

    /**
    *** Flattens a JSON Object into a property map
    **/
    public static Map<String,Object> createPropertyMap(JSON._Object obj)
    {
        Map<String,Object> propMap = new HashMap<String,Object>();
        JSON._flattenObject(propMap, obj, null, ".");
        return propMap;
    }

    /**
    *** Flattens a JSON Array into a property map
    **/
    public static Map<String,Object> createPropertyMap(JSON._Array array)
    {
        Map<String,Object> propMap = new HashMap<String,Object>();
        JSON._flattenArray(propMap, array, null, ".");
        return propMap;
    }

    /**
    *** Flattens the specified JSON Value into a the property map
    **/
    private static void _flattenValue(Map<String,Object> map, JSON._Value val, String keyPfx, String sep)
    {
        if (val == null) {
            // -- ignore
        } else
        if (val.isScalarValue()) {
            // -- scalar value
            map.put(keyPfx, val.getJavaObject());
        } else
        if (val.isObjectValue()) {
            // -- flatten object
            JSON._flattenObject(map, val.getObjectValue(null), keyPfx, sep);
        } else
        if (val.isArrayValue()) {
            // -- flatten array
            JSON._flattenArray(map, val.getArrayValue(null), keyPfx, sep);
        } else
        if (val.isNullValue()) {
            // -- null
            map.put(keyPfx, null);
        } else {
            // -- unlikely: not sure what this could be, ignore it
        }
    }

    /**
    *** Flattens the specified JSON Object into a the property map
    **/
    private static void _flattenObject(Map<String,Object> map, JSON._Object obj, String keyPfx, String sep)
    {
        if (obj != null) {
            for (int i = 0; i < obj.getKeyValueCount(); i++) {
                JSON._KeyValue kv = obj.getKeyValueAt(i);
                String         ks = ((keyPfx!=null)?(keyPfx+sep):"") + kv.getKey();
                JSON._Value    v  = kv.getValue();
                JSON._flattenValue(map, v, ks, sep);
            }
        }
    }

    /**
    *** Flattens the specified JSON Array into a the property map
    **/
    private static void _flattenArray(Map<String,Object> map, JSON._Array array, String keyPfx, String sep)
    {
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                String      ks = ((keyPfx!=null)?(keyPfx+sep):"") + i;
                JSON._Value v  = array.getValueAt(i);
                JSON._flattenValue(map, v, ks, sep);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final boolean CASE_SENSITIVE = false;

    private static boolean NameEquals(String n1, String n2)
    {
        if ((n1 == null) || (n2 == null)) {
            return false;
        } else
        if (CASE_SENSITIVE) {
            return n1.equals(n2);
        } else {
            return n1.equalsIgnoreCase(n2);
        }
    }

    // ------------------------------------------------------------------------

    private static final String INDENT = "   ";

    /**
    *** Return indent spaces
    **/
    private static String indent(int count)
    {
        return StringTools.replicateString(INDENT,count);
    }

    // ------------------------------------------------------------------------

    private static final char ESCAPE_CHAR = '\\';

    /**
    *** Converts the specified String to a JSON escaped value String.<br>
    *** @param s  The String to convert to a JSON encoded String
    *** @return The JSON encoded String
    **/
    public static String escapeJSON(String s)
    {
        if (s != null) {
            StringBuffer sb = new StringBuffer();
            int len = s.length();
            for (int i = 0; i < len; i++) {
                char ch = s.charAt(i);
                if (ch == ESCAPE_CHAR) {
                    // -- "\\"
                    sb.append(ESCAPE_CHAR).append(ESCAPE_CHAR);
                } else
                if (ch == '\n') {
                    // -- newline
                    sb.append(ESCAPE_CHAR).append('n');
                } else
                if (ch == '\r') {
                    // -- carriage-return
                    sb.append(ESCAPE_CHAR).append('r');
                } else
                if (ch == '\t') {
                    // -- horizontal tab
                    sb.append(ESCAPE_CHAR).append('t');
                } else
                if (ch == '\b') {
                    // -- backspace
                    sb.append(ESCAPE_CHAR).append('b');
                } else
                if (ch == '\f') {
                    // -- formfeed
                    sb.append(ESCAPE_CHAR).append('f');
                } else
              //if (ch == '\'') {
              //    // -- single-quote
              //    sb.append(ESCAPE_CHAR).append('\''); <-- should not be escaped
              //} else
                if (ch == '\"') { // double-quote
                    // -- "\""
                    sb.append(ESCAPE_CHAR).append('\"');
                } else
                if ((ch >= 0x0020) && (ch <= 0x007e)) {
                    // -- ASCII
                    sb.append(ch);
                } else
                if (ch < 0x0020) {
                    // -- control characters: "/u00FF"
                    sb.append(ESCAPE_CHAR).append("u").append(StringTools.toHexString((int)ch,16));
                } else 
                if (ch < 0x00FF) {
                    // -- non-ASCII characters: "/u00FF"
                    sb.append(ESCAPE_CHAR).append("u").append(StringTools.toHexString((int)ch,16));
                } else {
                    // -- unicode characters: "/uFFFF"
                    //sb.append(ch);
                    sb.append(ESCAPE_CHAR).append("u").append(StringTools.toHexString((int)ch,16));
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** JSON Parsing Context
    **/
    public static class JSONParsingContext
    {
        private int index       = 0;
        private int line        = 1;
        private int indexAtLine = 0;
        // --
        public JSONParsingContext() {
            this.index       = 0;
            this.line        = 1;
            this.indexAtLine = this.index;
        }
        public JSONParsingContext(int ndx, int lin) {
            this.index = ndx;
            this.line  = lin;
            this.indexAtLine = this.index;
        }
        // --
        public int getIndex() {
            return this.index;
        }
        public void incrementIndex(int val) {
            this.index += val;
        }
        public void incrementIndex() {
            this.index++;
        }
        // --
        public int getLine() {
            return this.line;
        }
        public int getIndexAtLine() {
            return this.indexAtLine;
        }
        public void incrementLine() {
            this.line++;
            this.indexAtLine = this.index;
        }
        // --
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.line);
            sb.append("/");
            sb.append(this.index);
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** JSON Parse Exception
    **/
    public static class JSONParsingException
        extends Exception
    {
        private int    index       = 0;
        private int    line        = 0;
        private int    indexAtLine = 0;
        private String jsonSrc     = null;
        public JSONParsingException(String msg, JSONParsingContext context, String jsonS) {
            super(msg);
            this.index       = (context != null)? context.getIndex()       : -1;
            this.line        = (context != null)? context.getLine()        : -1;
            this.indexAtLine = (context != null)? context.getIndexAtLine() : -1;
            this.jsonSrc     = jsonS;
        }
        public int getIndex() {
            return this.index;
        }
        public int getLine() {
            return this.line;
        }
        public int getIndexAtLine() {
            return this.indexAtLine;
        }
        public String getJsonSource() {
            return this.jsonSrc;
        }
        public String getParseErrorDisplay() {
            int    ndx = this.getIndex();
            int    L1  = this.getLine();
            int    Lx  = this.getIndexAtLine();
            String JS  = this.getJsonSource();
            if (StringTools.isBlank(JS) || (ndx < 0) || (ndx > JS.length()) || (L1 < 1)) {
                // -- nothing to display
                return "";
            }
            // --
            /*
            if (Lx >= 0) {
                int B = Lx;
                int E = Lx;
                for (;(B > 0) && (JS.charAt(B-1) != '\n');B--);
                for (;(E < JS.length()) && (JS.charAt(E) != '\n');E++);
            }
            */
            // --
            int ndxB = ndx;
            int ndxE = ndx;
            for (;(ndxB > 0) && (JS.charAt(ndxB-1) != '\n');ndxB--);
            for (;(ndxE < JS.length()) && (JS.charAt(ndxE) != '\n');ndxE++);
            // --
            StringBuffer sb = new StringBuffer();
            sb.append("---------------------------------------------------------\n");
            sb.append("Line " + L1 + ": \n");
            sb.append(JS.substring(ndxB,ndxE)).append("\n");
            sb.append(StringTools.replicateString(" ", (ndx-ndxB)));
            sb.append("^\n");
            sb.append("---------------------------------------------------------\n");
            return sb.toString();
        }
        public String toString() { // JSON.JSONParsingException
            String s = super.toString();
            return s + " ["+this.line+"/"+this.index+"]";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._Object
    
    /**
    *** JSON Object class
    **/
    public static class _Object
        extends Vector<JSON._KeyValue>
    {

        private boolean formatIndent = true;

        // -----------------------------------------

        /**
        *** _Object: Constructor
        **/
        public _Object() {
            super();
        }

        /**
        *** _Object: Constructor
        **/
        public _Object(Vector<JSON._KeyValue> list) {
            this();
            this.addAll(list);
        }

        /**
        *** _Object: Constructor
        **/
        public _Object(JSON._KeyValue... kv) {
            this();
            if (kv != null) {
                for (int i = 0; i < kv.length; i++) {
                    this.add(kv[i]);
                }
            }
        }

        // --------------------------------------

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(JSON._KeyValue kv) {
            return super.add(kv);
        }
        public boolean add(JSON._KeyValue kv) {
            return super.add(kv);
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, String value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, int value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, long value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, double value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, boolean value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, Object value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Array value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Object value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        /**
        *** _Object: Adds a key/value pair to this object
        **/
        public boolean addKeyValue(String key, JSON._Value value) {
            return this.add(new JSON._KeyValue(key, value));
        }

        // --------------------------------------

        /**
        *** _Object: Gets the number of key/value pairs in this object
        **/
        public int getKeyValueCount() {
            return super.size();
        }

        /**
        *** _Object: Gets the key/value pair at the specified index
        **/
        public JSON._KeyValue getKeyValueAt(int ndx) {
            if ((ndx >= 0) && (ndx < this.size())) {
                return this.get(ndx);
            } else {
                return null;
            }
        }

        // --------------------------------------

        /**
        *** _Object: Gets the key/value pair for the specified name
        **/
        public JSON._KeyValue getKeyValue(String n) {
            if (n != null) {
                for (JSON._KeyValue kv : this) {
                    String kvn = kv.getKey();
                    if (JSON.NameEquals(n,kvn)) {
                        return kv;
                    }
                }
            }
            return null;
        }

        // --------------------------------------

        /**
        *** _Object: Removes all key/value entries from this Object which match the specified key
        *** @return The last key/value entry removed, or null if no entries were removed
        **/
        public JSON._KeyValue removeKeyValue(String n)
        {
            JSON._KeyValue rtn = null;
            if (n != null) {
                for (int i = 0; i < this.getKeyValueCount();) {
                    JSON._KeyValue kv = this.getKeyValueAt(i);
                    String kvn = kv.getKey();
                    if (JSON.NameEquals(n,kvn)) {
                        rtn = this.remove(i);
                    } else {
                        i++;
                    }
                }
            }
            return rtn;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the JSON._Value for the specified name
        **/
        public JSON._Value getValueForName(String n) {
            JSON._KeyValue kv = this.getKeyValue(n);
            return (kv != null)? kv.getValue() : null;
        }

        /**
        *** _Object: Gets the JSON._Value for the specified name
        **/
        public JSON._Value getValueForName(String name[]) {
            if (name != null) {
                for (String n : name) {
                    JSON._Value jv = this.getValueForName(n);
                    if (jv != null) {
                        return jv;
                    }
                }
            }
            return null;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the JSON._Value for the specified path
        **/
        public JSON._Value getValueForPath(String path) {
            return JSON.GetValueForPath(this, path);
        }

        /**
        *** _Object: Gets the JSON._Value for the specified path
        **/
        public JSON._Value getValueForPath(String... path) {
            return JSON.GetValueForPath(this, path);
        }

        /**
        *** _Object: Gets the Java Object for the specified path
        **/
        public Object getJavaObjectForPath(String path) {
            JSON._Value val = this.getValueForPath(path);
            return (val != null)? val.getJavaObject() : null;
        }

        /**
        *** _Object: Gets the Java Object for the specified path
        **/
        public Object getJavaObjectForPath(String... path) {
            JSON._Value val = this.getValueForPath(path);
            return (val != null)? val.getJavaObject() : null;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the JSON._Array for the specified name
        **/
        public JSON._Array getArrayForName(String name, JSON._Array dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getArrayValue(dft) : dft;
        }

        /**
        *** _Object: Gets the JSON._Array for the specified name
        **/
        public JSON._Array getArrayForName(String name[], JSON._Array dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getArrayValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the JSON._Array for the specified name
        **/
        public String[] getStringArrayForName(String name, String dft[]) {
            JSON._Value jv = this.getValueForName(name);
            JSON._Array ar = (jv != null)? jv.getArrayValue(null) : null;
            return (ar != null)? ar.getStringArray() : dft;
        }

        /**
        *** _Object: Gets the JSON._Array for the specified name
        **/
        public String[] getStringArrayForName(String name[], String dft[]) {
            JSON._Value jv = this.getValueForName(name);
            JSON._Array ar = (jv != null)? jv.getArrayValue(null) : null;
            return (ar != null)? ar.getStringArray() : dft;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the JSON._Object value for the specified name
        **/
        public JSON._Object getObjectForName(String name, JSON._Object dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getObjectValue(dft) : dft;
        }

        /**
        *** _Object: Gets the JSON._Object value for the specified name
        **/
        public JSON._Object getObjectForName(String name[], JSON._Object dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getObjectValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the String value for the specified name
        **/
        public String getStringForName(String name, String dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getStringValue(dft) : dft;
        }

        /**
        *** _Object: Gets the String value for the specified name
        **/
        public String getStringForName(String name[], String dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getStringValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the Integer value for the specified name
        **/
        public int getIntForName(String name, int dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getIntValue(dft) : dft;
        }

        /**
        *** _Object: Gets the Integer value for the specified name
        **/
        public int getIntForName(String name[], int dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getIntValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the Long value for the specified name
        **/
        public long getLongForName(String name, long dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getLongValue(dft) : dft;
        }

        /**
        *** _Object: Gets the Long value for the specified name
        **/
        public long getLongForName(String name[], long dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getLongValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the Double value for the specified name
        **/
        public double getDoubleForName(String name, double dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getDoubleValue(dft) : dft;
        }

        /**
        *** _Object: Gets the Double value for the specified name
        **/
        public double getDoubleForName(String name[], double dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getDoubleValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** _Object: Gets the String value for the specified name
        **/
        public boolean getBooleanForName(String name, boolean dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getBooleanValue(dft) : dft;
        }

        /**
        *** _Object: Gets the String value for the specified name
        **/
        public boolean getBooleanForName(String name[], boolean dft) {
            JSON._Value jv = this.getValueForName(name);
            return (jv != null)? jv.getBooleanValue(dft) : dft;
        }

        // --------------------------------------

        /**
        *** _Object: Gets a list of all key names in this object
        **/
        public Collection<String> getKeyNames() {
            Collection<String> keyList = new Vector<String>();
            for (JSON._KeyValue kv : this) {
                keyList.add(kv.getKey());
            }
            return keyList;
        }

        /**
        *** _Object: Print object contents (for debug purposes only)
        **/
        public void debugDisplayObject(int level) {
            String pfx0 = StringTools.replicateString(INDENT,level);
            String pfx1 = StringTools.replicateString(INDENT,level+1);
            for (String key : this.getKeyNames()) {
                JSON._KeyValue kv = this.getKeyValue(key);
                Object val = kv.getValue().getJavaObject();
                Print.sysPrintln(pfx0 + key + " ==> " + StringTools.className(val));
                if (val instanceof JSON._Object) {
                    JSON._Object obj = (JSON._Object)val;
                    obj.debugDisplayObject(level+1);
                } else
                if (val instanceof JSON._Array) {
                    JSON._Array array = (JSON._Array)val;
                    for (JSON._Value jv : array) {
                        Object av = jv.getJavaObject();
                        Print.sysPrintln(pfx1 + " ==> " + StringTools.className(av));
                        if (av instanceof JSON._Object) {
                            JSON._Object obj = (JSON._Object)av;
                            obj.debugDisplayObject(level+2);
                        }
                    }
                }
            }
        }

        // --------------------------------------

        /**
        *** _Object: Set format indent state
        **/
        public _Object setFormatIndent(boolean indent) {
            this.formatIndent = indent;
            return this;
        }

        /**
        *** _Object: Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            boolean fullFormat = this.formatIndent && (prefix >= 0);
            String pfx0 = fullFormat? JSON.indent(prefix)   : "";
            String pfx1 = fullFormat? JSON.indent(prefix+1) : "";
            sb.append("{");
            if (fullFormat) {
                sb.append("\n");
            }
            if (this.size() > 0) {
                int size = this.size();
                for (int i = 0; i < size; i++) {
                    JSON._KeyValue kv = this.get(i);
                    sb.append(pfx1);
                    kv.toStringBuffer((fullFormat?(prefix+1):-1),sb);
                    if ((i + 1) < size) {
                        sb.append(",");
                    }
                    if (fullFormat) {
                        sb.append("\n");
                    }
                }
            }
            sb.append(pfx0).append("}");
            if (fullFormat && (prefix == 0)) {
                sb.append("\n");
            }
            return sb;
        }

        /**
        *** _Object: Returns a String representation of this instance 
        **/
        public String toString() { // JSON._Object
            return this.toStringBuffer(0,null).toString();
        }

        /**
        *** _Object: Returns a String representation of this instance 
        **/
        public String toString(boolean inclPrefix/*indent*/) { // JSON._Object
            return this.toStringBuffer((inclPrefix/*indent*/?0:-1),null).toString();
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse a JSON Comment from the specified String, starting at the 
    *** specified location
    **/
    public static String parse_Comment(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int    len  = StringTools.length(v);
        String val  = null;

        /* skip leading whitespace */
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else {
                break;
            }
        }

        /* next characters must be "/*" */
        int startLine  = context.getLine();
        int startIndex = context.getIndex();
        if ((startIndex + 2) >= len) {
            throw new JSONParsingException("Overflow", context, v);
        } else
        if ((v.charAt(startIndex  ) != '/') ||
            (v.charAt(startIndex+1) != '*')   ) {
            throw new JSONParsingException("Invalid beginning of comment", context, v);
        }
        context.incrementIndex(2);

        /* parse comment body */
        StringBuffer comment = new StringBuffer();
        commentParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                comment.append(ch);
                continue; // skip space
            } else
            if (ch == '*') {
                context.incrementIndex();
                int ndx = context.getIndex();
                if (ndx >= len) {
                    throw new JSONParsingException("Overflow", context, v);
                } else
                if ((v.charAt(ndx) == '/')) {
                    context.incrementIndex(); // consume final '/'
                    break commentParse;
                } else {
                    comment.append(ch);
                }
                continue;
            } else {
                comment.append(ch);
                context.incrementIndex();
            }
        } // commentParse
        val = comment.toString().trim();

        /* return comment */
        return val;

    }

    // ------------------------------------------------------------------------

    /**
    *** Parse a JSON Object from the specified String.
    *** Does not return null.
    **/
    public static _Object parse_Object(String v)
        throws JSONParsingException 
    {
        return JSON.parse_Object(v,new JSONParsingContext());
    }

    /**
    *** Parse a JSON Object from the specified String, starting at the 
    *** specified location.
    *** Does not return null.
    **/
    public static _Object parse_Object(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        JSON._Object obj  = null;
        boolean      comp = false;

        objectParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                // -- skip whitespace
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                // -- start of comment (non-standard JSON)
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (ch == '{') {
                // -- start of object
                if (obj != null) {
                    throw new JSONParsingException("Object already started", context, v);
                }
                context.incrementIndex();
                obj = new JSON._Object();
            } else
            if (ch == '\"') {
                // -- "key": VALUE
                if (obj == null) {
                    throw new JSONParsingException("No start of Object", context, v);
                }
                JSON._KeyValue kv = JSON.parse_KeyValue(v, context);
                if (kv == null) {
                    throw new JSONParsingException("Invalid KeyValue ...", context, v);
                }
                obj.add(kv);
            } else
            if (ch == ',') {
                // -- ignore extraneous commas (non-standard JSON)
                context.incrementIndex();
            } else
            if (ch == '}') {
                // -- end of object
                context.incrementIndex();
                if (obj == null) {
                    throw new JSONParsingException("No start of Object", context, v);
                }
                comp = true; // iff Object is defined
                break objectParse;
            } else {
                // -- invalid character
                throw new JSONParsingException("Invalid JSON syntax ...", context, v);
            }
        } // objectParse

        /* object completed? */
        if (!comp || (obj == null)) {
            throw new JSONParsingException("Incomplete Object", context, v);
        }

        /* return object */
        return obj;
        
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._KeyValue

    /**
    *** JSON Key/Value pair
    **/
    public static class _KeyValue
    {

        private String      key   = null;
        private JSON._Value value = null;

        // -----------------------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, String value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, Integer value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, int value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, Long value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, long value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, Float value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, float value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, Double value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, double value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, Boolean value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, boolean value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, JSON._Value value) {
            this.key   = key;
            this.value = value;
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, JSON._Array value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, JSON._Object value) {
            this.key   = key;
            this.value = new JSON._Value(value);
        }

        // ----------------------------

        /**
        *** _KeyValue: Constructor 
        **/
        public _KeyValue(String key, Object value) {
            this.key   = key;
            if (value instanceof JSON._Value) {
                this.value = (JSON._Value)value;
            } else {
                this.value = new JSON._Value(value);
            }
        }

        // -----------------------------------------

        /**
        *** _KeyValue: Gets the key of this key/value pair 
        **/
        public String getKey() {
            return this.key;
        }
        
        /**
        *** _KeyValue: Gets the value of this key/value pair 
        **/
        public JSON._Value getValue() {
            return this.value;
        }

        /**
        *** _KeyValue: Gets the Java Object value of this key/value pair
        **/
        public Object getJavaObjectValue()
        {
            return (this.value != null)? this.value.getJavaObject() : null;
        }

        // -----------------------------------------

        /**
        *** _KeyValue: Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            sb.append("\"");
            sb.append(JSON.escapeJSON(this.key));
            sb.append("\"");
            sb.append(":");
            if (prefix >= 0) {
                sb.append(" ");
            }
            if (this.value != null) {
                this.value.toStringBuffer(prefix,sb);
            } else {
                sb.append("null");
            }
            return sb;
        }
        
        /**
        *** _KeyValue: Returns a String representation of this instance 
        **/
        public String toString() { // JSON._KeyValue
            return this.toStringBuffer(1,null).toString();
        }

        /**
        *** _KeyValue: Returns a String representation of this instance 
        **/
        public String toString(boolean inclPrefix/*indent*/) { // JSON._KeyValue
            return this.toStringBuffer((inclPrefix/*indent*/?1:-1),null).toString();
        }

    }

    /**
    *** Parse a Key/Value pair from the specified String at the specified location
    **/
    public static JSON._KeyValue parse_KeyValue(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int            len  = StringTools.length(v);
        JSON._KeyValue kv   = null;
        boolean        comp = false;

        String key = null;
        boolean colon = false;
        keyvalParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (!colon && (ch == '\"')) {
                // -- Key
                key = JSON.parse_String(v, context);
                if (key == null) {
                    throw new JSONParsingException("Invalid key String", context, v);
                }
            } else
            if (ch == ':') {
                if (colon) {
                    throw new JSONParsingException("More than one ':'", context, v);
                } else
                if (key == null) {
                    throw new JSONParsingException("Key not defined", context, v);
                }
                context.incrementIndex();
                colon = true;
            } else {
                // -- JSON._Value
                JSON._Value val = JSON.parse_Value(v, context);
                if (val == null) {
                    throw new JSONParsingException("Invalid value", context, v);
                }
                kv = new JSON._KeyValue(key,val);
                comp = true;
                break keyvalParse;
            }
        } // keyvalParse

        /* key/value completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete Key/Value", context, v);
        }

        /* return key/value */
        return kv; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- JSON._Value

    /**
    *** JSON Value
    **/
    public static class _Value
    {

        private Object value = null;
        // -- "value" must be one of the following datatypes:
        // -    null
        // -    JSON._Array
        // -    JSON._Object
        // -    String
        // -    Double (Float converted to Double)
        // -    Long (Integer/Short/Byte converted to Long)
        // -    Boolean

        // -----------------------------------------

        /**
        *** _Value: Constructor 
        **/
        public _Value() {
            this.value = null;
        }

        // ----------------------------

        /**
        *** _Value: Generic "Object" Constructor.
        *** Value must be one of type String, Float(converted to Double), Double, Integer(converted to Long),
        *** Long, Boolean, JSON._Value, JSON._Array, or JSON._Object. (may also be null).
        **/
        public _Value(Object v) {
            if (v == null) {
                this.value = null;
            } else
            if (v instanceof JSON._Value) {
                this.value = ((JSON._Value)v).getJavaObject(); // shallow copy
            } else
            if (v instanceof JSON._Array) {
                this.value = v;
            } else 
            if (v instanceof JSON._Object) {
                this.value = v;
            } else
            if (v instanceof String) {
                this.value = v;
            } else
            if (v instanceof Double) {
                this.value = v;
            } else
            if (v instanceof Float) {
                this.value = new Double(((Float)v).doubleValue());
            } else
            if (v instanceof Long) {
                this.value = v;
            } else
            if (v instanceof Integer) {
                this.value = new Long(((Integer)v).longValue());
            } else
            if (v instanceof Short) {
                this.value = new Long(((Short)v).longValue());
            } else
            if (v instanceof Byte) {
                this.value = new Long(((Byte)v).longValue());
            } else
            if (v instanceof Boolean) {
                this.value = v;
            } else {
                this.value = v.toString();
            }
        }

        // ----------------------------

        /**
        *** _Value: "String" Constructor 
        **/
        public _Value(String v) {
            this.value = v; // may be null
        }

        // ----------------------------

        /**
        *** _Value: "Integer" Constructor (converted to Long)
        **/
        public _Value(Integer v) {
            this.value = (v != null)? new Long(v.longValue()) : null;
        }

        /**
        *** _Value: "int" Constructor (converted to Long)
        **/
        public _Value(int v) {
            this.value = new Long((long)v);
        }

        // ----------------------------

        /**
        *** _Value: "Long" Constructor 
        **/
        public _Value(Long v) {
            this.value = v; // may be null
        }

        /**
        *** _Value: "long" Constructor 
        **/
        public _Value(long v) {
            this.value = new Long(v);
        }

        // ----------------------------

        /**
        *** _Value: "Float" Constructor (converted to Double)
        **/
        public _Value(Float v) {
            this.value = (v != null)? new Double(v.doubleValue()) : null;
        }

        /**
        *** _Value: "float" Constructor (converted to Double)
        **/
        public _Value(float v) {
            this.value = new Double((double)v);
        }

        // ----------------------------

        /**
        *** _Value: "Double" Constructor 
        **/
        public _Value(Double v) {
            this.value = v; // may be null
        }

        /**
        *** _Value: "double" Constructor 
        **/
        public _Value(double v) {
            this.value = new Double(v);
        }

        // ----------------------------

        /**
        *** _Value: "Boolean" Constructor 
        **/
        public _Value(Boolean v) {
            this.value = v;
        }

        /**
        *** _Value: "boolean" Constructor 
        **/
        public _Value(boolean v) {
            this.value = new Boolean(v);
        }

        // ----------------------------

        /**
        *** _Value: "_Array" Constructor 
        **/
        public _Value(JSON._Array v) {
            this.value = v; // may be null
        }

        // ----------------------------

        /**
        *** _Value: "_Object" Constructor 
        **/
        public _Value(JSON._Object v) {
            this.value = v; // may be null
        }

        // -----------------------------------------

        /**
        *** _Value: Gets the value as a Java Object.
        *** Will be one of the following class types:<br>
        ***  - JSON._Object<br>
        ***  - JSON._Array<br>
        ***  - Double<br>
        ***  - Long<br>
        ***  - String<br>
        ***  - Boolean<br>
        ***  - null<br>
        **/
        public Object getJavaObject() {
            return this.value;
        }

        @Deprecated
        public Object _getObjectValue() {
            return this.value;
        }

        // -----------------------------------------

        /**
        *** _Value: Returns true if this value represents scalar quantity.
        *** Includes types: String, Number(Double,Long,etc), and Boolean
        **/
        public boolean isScalarValue() {
            return JSON.IsScalarValue(this.value);
        }

        // -----------------------------------------

        /**
        *** _Value: Returns true if this value represents a nul Object 
        **/
        public boolean isNullValue() {
            return (this.value == null);
        }

        // -----------------------------------------

        /**
        *** _Value: Returns true if this value represents a String 
        **/
        public boolean isStringValue() {
            return (this.value instanceof String);
        }

        /**
        *** _Value: Gets the String representation of this value if the value type is one of
        *** String, Long, Double, or Boolean
        **/
        public String getStringValue(String dft) {
            if (this.value instanceof String) {
                return (String)this.value;
            } else
            if (this.value instanceof Number) { // Long/Double
                return this.value.toString();
            } else
            if (this.value instanceof Boolean) {
                return this.value.toString();
            } else  {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** _Value: Returns true if this value represents a Long 
        **/
        public boolean isLongValue() {
            return (this.value instanceof Long);
        }

        /**
        *** _Value: Gets the Long representation of this value if the value type is one of
        *** Number(longValue), String(parseLong), or Boolean('0' if false, '1' otherwise)
        **/
        public long getLongValue(long dft) {
            if (this.value instanceof Number) { // Long/Double
                return ((Number)this.value).longValue();
            } else
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue()? 1L : 0L;
            } else
            if (this.value instanceof String) {
                return StringTools.parseLong(this.value,dft);
            } else {
                return dft;
            }
        }

        /**
        *** _Value: Gets the Integer representation of this value if the value type is one of
        *** Number(intValue), String(parseInt), or Boolean('0' if false, '1' otherwise)
        **/
        public int getIntValue(int dft) {
            return (int)this.getLongValue((long)dft);
        }

        // -----------------------------------------

        /**
        *** _Value: Returns true if this value represents a Double 
        **/
        public boolean isDoubleValue() {
            return (this.value instanceof Double);
        }

        /**
        *** _Value: Gets the Double representation of this value if the value type is one of
        *** Number(doubleValue), String(parseDouble), or Boolean('0.0' if false, '1.0' otherwise)
        **/
        public double getDoubleValue(double dft) {
            if (this.value instanceof Number) { // Long/Double
                return ((Number)this.value).doubleValue();
            } else
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue()? 1.0 : 0.0;
            } else
            if (this.value instanceof String) {
                return StringTools.parseDouble(this.value,dft);
            } else {
                return dft;
            }
        }

        /**
        *** _Value: Gets the Float representation of this value if the value type is one of
        *** Number, String, or Boolean
        **/
        public float getFloatValue(float dft) {
            return (float)this.getDoubleValue((double)dft);
        }

        // -----------------------------------------

        /**
        *** _Value: Returns true if this value represents a Boolean 
        **/
        public boolean isBooleanValue() {
            return (this.value instanceof Boolean);
        }

        /**
        *** _Value: Gets the Boolean representation of this value if the value type is one of
        *** Boolean(booleanValue), String(parseBoolean), or Number(false if '0', true otherwise)
        **/
        public boolean getBooleanValue(boolean dft) {
            if (this.value instanceof Boolean) {
                return ((Boolean)this.value).booleanValue();
            } else
            if (this.value instanceof Number) { // Long/Double
                return (((Number)this.value).longValue() != 0L)? true : false;
            } else
            if (this.value instanceof String) {
                return StringTools.parseBoolean(this.value,dft);
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** _Value: Returns true if this value represents a JSON._Array 
        **/
        public boolean isArrayValue() {
            return (this.value instanceof JSON._Array);
        }

        /**
        *** _Value: Gets the JSON._Array value 
        **/
        public JSON._Array getArrayValue(JSON._Array dft) {
            if (this.value instanceof JSON._Array) {
                return (JSON._Array)this.value;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** _Value: Returns true if this value represents a JSON._Object 
        **/
        public boolean isObjectValue() {
            return (this.value instanceof JSON._Object);
        }

        /**
        *** _Value: Gets the JSON._Object value 
        **/
        public JSON._Object getObjectValue(JSON._Object dft) {
            if (this.value instanceof JSON._Object) {
                return (JSON._Object)this.value;
            } else {
                return dft;
            }
        }

        // -----------------------------------------

        /**
        *** _Value: Returns the class of the value object
        **/
        public Class<?> getValueClass() {
            return (this.value != null)? this.value.getClass() : null;
        }
        
        /**
        *** _Value: Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            if (this.value == null) {
                sb.append("null");
            } else 
            if (this.value instanceof JSON._Array) {
                ((JSON._Array)this.value).toStringBuffer(prefix, sb);
            } else
            if (this.value instanceof JSON._Object) {
                ((JSON._Object)this.value).toStringBuffer(prefix, sb);
            } else
            if (this.value instanceof String) {
                sb.append("\"");
                sb.append(JSON.escapeJSON((String)this.value));
                sb.append("\"");
            } else
            if (this.value instanceof Number) { // Long/Double
                sb.append(this.value.toString());
            } else
            if (this.value instanceof Boolean) {
                sb.append(this.value.toString());
            } else {
                // ignore
            }
            return sb;
        }

        /**
        *** _Value: Returns a String representation of this instance 
        **/
        public String toString() { // JSON._Value
            return this.toStringBuffer(0,null).toString();
        }

        /**
        *** _Value: Returns a String representation of this instance 
        **/
        public String toString(boolean inclPrefix/*indent*/) { // JSON._Value
            return this.toStringBuffer((inclPrefix/*indent*/?0:-1),null).toString();
        }

    }

    /**
    *** Parse a JSON Array from the specified String
    **/
    public static JSON._Value parse_Value(String v)
        throws JSONParsingException 
    {
        return JSON.parse_Value(v, new JSONParsingContext());
    }

    /**
    *** Parse JSON Value
    **/
    public static JSON._Value parse_Value(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        JSON._Value  val  = null;
        boolean      comp = false;

        valueParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (ch == '\"') {
                // -- parse String
                String sval = JSON.parse_String(v, context);
                if (sval == null) {
                    throw new JSONParsingException("Invalid String value", context, v);
                } else {
                    val = new JSON._Value(sval);
                }
                comp = true;
                break valueParse;
            } else
            if ((ch == '-') || (ch == '+') || Character.isDigit(ch)) {
                // -- parse Number (Long/Double)
                Number num = JSON.parse_Number(v, context); // Long/Double
                if (num == null) {
                    throw new JSONParsingException("Invalid Number value", context, v);
                } else
                if (num instanceof Float) {
                    val = new JSON._Value((Float)num);
                } else
                if (num instanceof Double) {
                    val = new JSON._Value((Double)num);
                } else
                if (num instanceof Integer) {
                    //boolean hex = false;
                    val = new JSON._Value((Integer)num);
                } else
                if (num instanceof Long) {
                    //boolean hex = false;
                    val = new JSON._Value((Long)num);
                } else {
                    throw new JSONParsingException("Unsupported Number type: " + StringTools.className(num), context, v);
                }
                comp = true;
                break valueParse;
            } else
            if (ch == 't') { 
                // -- true
                context.incrementIndex();
                int ndx = context.getIndex();
                if ((ndx + 2) >= len) {
                    throw new JSONParsingException("Overflow", context, v);
                } else
                if ((v.charAt(ndx  ) == 'r') && 
                    (v.charAt(ndx+1) == 'u') && 
                    (v.charAt(ndx+2) == 'e')   ) {
                    context.incrementIndex(3);
                    val = new JSON._Value(Boolean.TRUE);
                } else {
                    throw new JSONParsingException("Invalid Boolean 'true'", context, v);
                }
                comp = true;
                break valueParse;
            } else
            if (ch == 'f') { 
                // -- false
                context.incrementIndex();
                int ndx = context.getIndex();
                if ((ndx + 3) >= len) {
                    throw new JSONParsingException("Overflow", context, v);
                } else
                if ((v.charAt(ndx  ) == 'a') && 
                    (v.charAt(ndx+1) == 'l') && 
                    (v.charAt(ndx+2) == 's') &&
                    (v.charAt(ndx+3) == 'e')   ) {
                    context.incrementIndex(4);
                    val = new JSON._Value(Boolean.FALSE);
                } else {
                    throw new JSONParsingException("Invalid Boolean 'false'", context, v);
                }
                comp = true;
                break valueParse;
            } else
            if (ch == 'n') { 
                // -- null
                context.incrementIndex();
                int ndx = context.getIndex();
                if ((ndx + 2) >= len) {
                    throw new JSONParsingException("Overflow", context, v);
                } else
                if ((v.charAt(ndx  ) == 'u') && 
                    (v.charAt(ndx+1) == 'l') && 
                    (v.charAt(ndx+2) == 'l')   ) {
                    context.incrementIndex(3);
                    val = new JSON._Value((JSON._Object)null); // null object
                } else {
                    throw new JSONParsingException("Invalid 'null'", context, v);
                }
                comp = true;
                break valueParse;
            } else
            if (ch == '[') {
                // -- JSON._Array
                JSON._Array array = JSON.parse_Array(v, context);
                if (array == null) {
                    throw new JSONParsingException("Invalid array", context, v);
                }
                val = new JSON._Value(array);
                comp = true;
                break valueParse;
            } else
            if (ch == '{') {
                // -- JSON._Object
                JSON._Object obj = JSON.parse_Object(v, context);
                if (obj == null) {
                    throw new JSONParsingException("Invalid object", context, v);
                }
                val = new JSON._Value(obj);
                comp = true;
                break valueParse;
            } else {
                throw new JSONParsingException("Unexpected character", context, v);
            }
        } // valueParse

        /* value completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete Value", context, v);
        }

        /* return value */
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // JSON._Array

    /**
    *** JSON Array 
    **/
    public static class _Array
        extends Vector<JSON._Value>
    {

        private boolean formatIndent = true;

        // -----------------------------------------

        /**
        *** _Array: Constructor 
        **/
        public _Array() {
            super();
        }

        /**
        *** _Array: Constructor 
        *** An Array of other Values
        **/
        public _Array(JSON._Value... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.add(array[i]);
                }
            }
        }

        /**
        *** _Array: Constructor 
        *** An Array of Strings
        **/
        public _Array(String... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** _Array: Constructor 
        *** An Array of Longs
        **/
        public _Array(long... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** _Array: Constructor 
        *** An Array of Doubles
        **/
        public _Array(double... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** _Array: Constructor 
        *** An Array of Booleans
        **/
        public _Array(boolean... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** _Array: Constructor 
        *** An Array of Objects
        **/
        public _Array(JSON._Object... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** _Array: Constructor 
        *** An Array of other Arrays
        **/
        public _Array(JSON._Array... array) {
            if (array != null) {
                for (int i = 0; i < array.length; i++) {
                    this.addValue(array[i]);
                }
            }
        }

        /**
        *** _Array: Constructor 
        **/
        /*
        private _Array(Collection list) {
            if (list != null) {
                for (Object val : list) {
                    if (val == null) {
                        this.addValue("");
                    } else
                    if (val instanceof JSON._Value) {
                        this.addValue((JSON._Value)val);
                    } else
                    if (val instanceof JSON._Array) {
                        this.addValue((JSON._Array)val);
                    } else
                    if (val instanceof JSON._Object) {
                        this.addValue((JSON._Object)val);
                    } else
                    if (val instanceof String) {
                        this.addValue((String)val);
                    } else
                    if (val instanceof Long) {
                        this.addValue(((Long)val).longValue());
                    } else
                    if (val instanceof Double) {
                        this.addValue(((Double)val).doubleValue());
                    } else
                    if (val instanceof Boolean) {
                        this.addValue(((Boolean)val).booleanValue());
                    } else {
                        Print.logInfo("Unrecognized data type: " + StringTools.className(val));
                        this.addValue(val.toString());
                    }
                }
            }
        }
        */

        // --------------------------------------

        /**
        *** _Array: Add a JSON._Value to this JSON._Array 
        **/
        public boolean add(JSON._Value value) {
            return super.add(value);
        }

        // ----------------------------

        /**
        *** _Array: Add a JSON._Value to this JSON._Array 
        **/
        public boolean addValue(JSON._Value value) {
            return this.add(value);
        }

        // ----------------------------

        /**
        *** _Array: Add a String to this JSON._Array 
        **/
        public boolean addValue(String value) {
            return this.add(new JSON._Value(value));
        }

        // ----------------------------

        /**
        *** _Array: Add a Integer to this JSON._Array (converted to Long)
        **/
        public boolean addValue(Integer value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** _Array: Add a int to this JSON._Array (converted to Long)
        **/
        public boolean addValue(int value) {
            return this.add(new JSON._Value(value));
        }

        // ----------------------------

        /**
        *** _Array: Add a Long to this JSON._Array 
        **/
        public boolean addValue(Long value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** _Array: Add a Long to this JSON._Array 
        **/
        public boolean addValue(long value) {
            return this.add(new JSON._Value(value));
        }

        // ----------------------------

        /**
        *** _Array: Add a Float to this JSON._Array  (converted to Double)
        **/
        public boolean addValue(Float value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** _Array: Add a float to this JSON._Array (converted to Double)
        **/
        public boolean addValue(float value) {
            return this.add(new JSON._Value(value));
        }

        // ----------------------------

        /**
        *** _Array: Add a Double to this JSON._Array 
        **/
        public boolean addValue(Double value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** _Array: Add a Double to this JSON._Array 
        **/
        public boolean addValue(double value) {
            return this.add(new JSON._Value(value));
        }

        // ----------------------------

        /**
        *** _Array: Add a Boolean to this JSON._Array 
        **/
        public boolean addValue(Boolean value) {
            return this.add(new JSON._Value(value));
        }

        /**
        *** _Array: Add a Boolean to this JSON._Array 
        **/
        public boolean addValue(boolean value) {
            return this.add(new JSON._Value(value));
        }

        // ----------------------------

        /**
        *** _Array: Add a JSON._Object to this JSON._Array 
        **/
        public boolean addValue(JSON._Object value) {
            return this.add(new JSON._Value(value));
        }

        // ----------------------------

        /**
        *** _Array: Add a JSON._Array to this JSON._Array 
        **/
        public boolean addValue(JSON._Array value) {
            return this.add(new JSON._Value(value));
        }

        // ----------------------------

        /**
        *** _Array: Add a Object to this JSON._Array 
        **/
        public boolean addValue(Object value) {
            return this.add(new JSON._Value(value));
        }

        // --------------------------------------

        /**
        *** _Array: Returns the JSON._Value at the specified index
        **/
        public JSON._Value getValueAt(int ndx) {
            if ((ndx >= 0) && (ndx < this.size())) {
                return this.get(ndx);
            } else {
                return null;
            }
        }

        /**
        *** _Array: Returns the JSON._Object value at the specified index
        **/
        public JSON._Object getObjectValueAt(int ndx, JSON._Object dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getObjectValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** _Array: Returns the JSON._Array value at the specified index
        **/
        public JSON._Array getArrayValueAt(int ndx, JSON._Array dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getArrayValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** _Array: Returns the String value at the specified index
        **/
        public String getStringValueAt(int ndx, String dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getStringValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** _Array: Returns the Integer value at the specified index
        **/
        public int getIntValueAt(int ndx, int dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getIntValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** _Array: Returns the Long value at the specified index
        **/
        public long getLongValueAt(int ndx, long dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getLongValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** _Array: Returns the Double value at the specified index
        **/
        public double getDoubleValueAt(int ndx, double dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getDoubleValue(dft) : dft;
            } else {
                return dft;
            }
        }

        /**
        *** _Array: Returns the Boolean value at the specified index
        **/
        public boolean getBooleanValueAt(int ndx, boolean dft) {
            if ((ndx >= 0) && (ndx < this.size())) {
                JSON._Value jv = this.get(ndx);
                return (jv != null)? jv.getBooleanValue(dft) : dft;
            } else {
                return dft;
            }
        }

        // --------------------------------------

        /**
        *** _Array: Returns a String array of values contained in this JSON Array
        **/
        public String[] getStringArray() {
            String v[] = new String[this.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = this.getStringValueAt(i,"");
            }
            return v;
        }

        /**
        *** _Array: Returns an int array of values contained in this JSON Array
        **/
        public int[] getIntArray() {
            int v[] = new int[this.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = this.getIntValueAt(i,0);
            }
            return v;
        }

        /**
        *** _Array: Returns a long array of values contained in this JSON Array
        **/
        public long[] getLongArray() {
            long v[] = new long[this.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = this.getLongValueAt(i,0L);
            }
            return v;
        }

        /**
        *** _Array: Returns a double array of values contained in this JSON Array
        **/
        public double[] getDoubleArray() {
            double v[] = new double[this.size()];
            for (int i = 0; i < v.length; i++) {
                v[i] = this.getDoubleValueAt(i,0L);
            }
            return v;
        }

        /**
        *** _Array: Returns an array of Java Object values contained in this JSON Array
        **/
        public Object[] _getObjectArray() {
            Object v[] = new Object[this.size()];
            for (int i = 0; i < v.length; i++) {
                JSON._Value jv = this.get(i);
                v[i] = (jv != null)? jv.getJavaObject() : null;
            }
            return v;
        }

        // --------------------------------------

        /**
        *** _Array: Gets the JSON._Value for the specified path
        **/
        public JSON._Value getValueForPath(String path) {
            return JSON.GetValueForPath(this, path);
        }

        /**
        *** _Array: Gets the JSON._Value for the specified path
        **/
        public JSON._Value getValueForPath(String... path) {
            return JSON.GetValueForPath(this, path);
        }

        /**
        *** _Array: Gets the Java Object for the specified path
        **/
        public Object getJavaObjectForPath(String path) {
            JSON._Value val = this.getValueForPath(path);
            return (val != null)? val.getJavaObject() : null;
        }

        /**
        *** _Array: Gets the Java Object for the specified path
        **/
        public Object getJavaObjectForPath(String... path) {
            JSON._Value val = this.getValueForPath(path);
            return (val != null)? val.getJavaObject() : null;
        }

        // --------------------------------------

        /**
        *** _Array: Gets the number of items in this array
        *** @return The number of items in this array
        **/
        public int size() {
            return super.size();
        }

        /**
        *** _Array: Returns true if this array is empty 
        *** @return True if this array is empty
        **/
        public boolean isEmpty() {
            return super.isEmpty();
        }

        // --------------------------------------

        /**
        *** _Array: Print array contents (for debug purposes only)
        **/
        public void debugDisplayArray(int level) {
            String pfx0 = StringTools.replicateString(INDENT,level);
            String pfx1 = StringTools.replicateString(INDENT,level+1);
            for (JSON._Value jv : this) {
                Object av = jv.getJavaObject();
                Print.sysPrintln(pfx1 + " ==> " + StringTools.className(av));
                if (av instanceof JSON._Object) {
                    JSON._Object obj = (JSON._Object)av;
                    obj.debugDisplayObject(level+2);
                }
            }
        }

        // --------------------------------------

        /**
        *** _Array: Set format indent state
        **/
        public _Array setFormatIndent(boolean indent) {
            this.formatIndent = indent;
            return this;
        }

        /**
        *** _Array: Write a String representation of this instance to the StringBuffer
        **/
        public StringBuffer toStringBuffer(int prefix, StringBuffer sb) {
            if (sb == null) { sb = new StringBuffer(); }
            boolean fullFormat = this.formatIndent && (prefix >= 0);
            String pfx0 = fullFormat? JSON.indent(prefix)   : "";
            String pfx1 = fullFormat? JSON.indent(prefix+1) : "";
            sb.append("[");
            if (fullFormat) {
                sb.append("\n");
            }
            int size = this.size();
            for (int i = 0; i < this.size(); i++) {
                JSON._Value v = this.get(i);
                sb.append(pfx1);
                v.toStringBuffer((fullFormat?(prefix+1):-1), sb);
                if ((i + 1) < size) { 
                    sb.append(","); 
                }
                if (fullFormat) {
                    sb.append("\n");
                }
            }
            sb.append(pfx0).append("]");
            return sb;
        }

        /**
        *** _Array: Returns a String representation of this instance 
        **/
        public String toString() { // JSON._Array
            return this.toStringBuffer(1,null).toString();
        }

        /**
        *** _Array: Returns a String representation of this instance 
        **/
        public String toString(boolean inclPrefix/*indent*/) { // JSON._Array
            return this.toStringBuffer((inclPrefix/*indent*/?1:-1),null).toString();
        }

    }

    /**
    *** Parse a JSON Array from the specified String
    **/
    public static JSON._Array parse_Array(String v)
        throws JSONParsingException 
    {
        return JSON.parse_Array(v, new JSONParsingContext());
    }

    /**
    *** Parse JSON Array from the specified String
    **/
    public static JSON._Array parse_Array(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len   = StringTools.length(v);
        JSON._Array  array = null;
        boolean      comp  = false;

        arrayParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (ch == '[') {
                if (array == null) {
                    context.incrementIndex();
                    array = new JSON._Array();
                } else {
                    // -- array within array
                    JSON._Value val = JSON.parse_Value(v, context);
                    if (val == null) {
                        throw new JSONParsingException("Invalid Value", context, v);
                    }
                    array.add(val);
                }
            } else
            if (ch == ',') {
                // -- ignore item separators
                // -  TODO: should insert a placeholder for unspecified values?
                context.incrementIndex();
            } else
            if (ch == ']') {
                // end of array
                context.incrementIndex();
                comp = true;
                break arrayParse;
            } else {
                JSON._Value val = JSON.parse_Value(v, context);
                if (val == null) {
                    throw new JSONParsingException("Invalid Value", context, v);
                }
                array.add(val);
            }
        }

        /* array completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete Array", context, v);
        }

        /* return array */
        return array;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // String

    /**
    *** Parse a JSON String
    **/
    public static String parse_String(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        String       val  = null;
        boolean      comp = false;

        stringParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if (ch == '\"') {
                // parse String
                context.incrementIndex(); // consume initial quote
                StringBuffer sb = new StringBuffer();
                quoteParse:
                for (;context.getIndex() < len;) {
                    ch = v.charAt(context.getIndex());
                    if (ch == '\\') {
                        context.incrementIndex(); // skip '\'
                        if (context.getIndex() >= len) {
                            throw new JSONParsingException("Overflow", context, v);
                        }
                        ch = v.charAt(context.getIndex());
                        context.incrementIndex(); // skip char
                        switch (ch) {
                            case '"' : sb.append('"' ); break;
                            case '\\': sb.append('\\'); break;
                            case '/' : sb.append('/' ); break;
                            case 'b' : sb.append('\b'); break;
                            case 'f' : sb.append('\f'); break;
                            case 'n' : sb.append('\n'); break;
                            case 'r' : sb.append('\r'); break;
                            case 't' : sb.append('\t'); break;
                            case 'u' : {
                                int ndx = context.getIndex();
                                if ((ndx + 4) >= len) {
                                    throw new JSONParsingException("Overflow", context, v);
                                }
                                String hex = v.substring(ndx,ndx+4);
                                int uchi = StringTools.parseHexInt(hex,-1);
                                if ((uchi & 0xFF) == uchi) {
                                    // -- ASCII character
                                    sb.append((char)uchi);
                                } else {
                                    // -- Unicode [TODO:]
                                }
                                context.incrementIndex(4); // additional length of char hex
                                break;
                            }
                            default  : sb.append(ch); break;
                        }
                    } else
                    if (ch == '\"') {
                        context.incrementIndex();  // consume final quote
                        comp = true;
                        break quoteParse; // we're done
                    } else
                    if (ch < ' ' ) { // included \n \r 
                        throw new JSONParsingException("Invalid character in String", context, v);
                    } else {
                        sb.append(ch);
                        context.incrementIndex();
                        if (context.getIndex() >= len) {
                            throw new JSONParsingException("Overflow", context, v);
                        }
                    }
                } // quoteParse
                val = sb.toString();
                break stringParse;
            } else {
                throw new JSONParsingException("Missing initial String quote", context, v);
            }
        } // stringParse

        /* String completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete String", context, v);
        }

        /* return String */
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Number

    /**
    *** Parse a JSON Number
    *** Returns Long, Double, or null (if invalid number)
    **/
    public static Number parse_Number(String v, JSONParsingContext context)
        throws JSONParsingException 
    {
        if (context == null) { context = new JSONParsingContext(); }
        int          len  = StringTools.length(v);
        Number       val  = null;
        boolean      comp = false;

        numberParse:
        for (;context.getIndex() < len;) {
            char ch = v.charAt(context.getIndex());
            if (Character.isWhitespace(ch)) {
                context.incrementIndex(); // consume space
                if (ch == '\n') { context.incrementLine(); }
                continue; // skip space
            } else
            if (ch == '/') {
                String comment = JSON.parse_Comment(v, context);
                continue; // skip comment
            } else
            if ((ch == '-') || (ch == '+') || Character.isDigit(ch)) {
                StringBuffer num = new StringBuffer();
                num.append(ch); // save first character
                context.incrementIndex();
                int intDig = Character.isDigit(ch)? 1 : 0; // count first digit
                int frcDig = 0; // digits in fraction (after decimal)
                int expDig = 0; // digits in exponent (after 'E')
                int hexDig = 0; // hex digits
                boolean frcCh = false; // '.'
                boolean esnCh = false; // '+'/'-' (exponent sign)
                boolean expCh = false; // 'e'/'E'
                boolean hexCh = false; // 'x'/'X'
                digitParse:
                for (;context.getIndex() < len;) {
                    char d = v.charAt(context.getIndex());
                    if (Character.isDigit(d)) {
                        if (expCh) {
                            expDig++; // digits after exponent
                        } else
                        if (frcCh) {
                            frcDig++; // digits after decimal
                        } else {
                            intDig++; // leading digits
                        }
                        num.append(d);
                        context.incrementIndex();
                    } else
                    if ((d == 'e') || (d == 'E')) {
                        if (frcCh && (frcDig == 0)) {
                            // -- no digits after decimal
                            throw new JSONParsingException("Invalid numeric value (no digits after '.')", context, v);
                        } else
                        if (expCh) {
                            // -- more than one 'E'
                            throw new JSONParsingException("Invalid numeric value (multiple 'E')", context, v);
                        } else
                        if (hexCh) {
                            // -- 'E' allowed in hex value
                            hexDig++; // hex digits after 'x'
                        } else {
                            // -- assume exponent
                            expCh = true;
                        }
                        num.append(d);
                        context.incrementIndex();
                    } else
                    if (StringTools.isHexDigit(d)) { // A/B/C/D/F (0..9 and 'E' handled above)
                        if (!hexCh) {
                            // -- hex value not prefaced with '0x'
                            throw new JSONParsingException("Invalid numeric value (invalid hex value)", context, v);
                        }
                        hexDig++; // hex digits after 'x'
                        num.append(d);
                        context.incrementIndex();
                    } else
                    if (d == '.') {
                        if (frcCh) {
                            // -- more than one '.'
                            throw new JSONParsingException("Invalid numeric value (multiple '.')", context, v);
                        } else
                        if (intDig == 0) {
                            // -- no digits before decimal
                            throw new JSONParsingException("Invalid numeric value (no digits before '.')", context, v);
                        } else
                        if (hexCh) {
                            // -- decimal not allow in hex value
                            throw new JSONParsingException("Invalid numeric value (invalid hex value)", context, v);
                        }
                        frcCh = true;
                        num.append(d);
                        context.incrementIndex();
                    } else
                    if ((d == '-') || (d == '+')) {
                        if (!expCh) {
                            // -- no 'E'
                            throw new JSONParsingException("Invalid numeric value (no 'E')", context, v);
                        } else
                        if (esnCh) {
                            // -- more than one '-/+'
                            throw new JSONParsingException("Invalid numeric value (more than one '+/-')", context, v);
                        }
                        esnCh = true;
                        num.append(d);
                        context.incrementIndex();
                    } else
                    if ((d == 'x') || (d == 'X')) {
                        if (hexCh) {
                            // -- more than one 'x'
                            throw new JSONParsingException("Invalid numeric value (more than one 'x')", context, v);
                        } else
                        if ((intDig != 1) || !num.toString().equals("0")) {
                            // -- missing "0" before 'x'
                            throw new JSONParsingException("Invalid numeric value (invalid hex value)", context, v);
                        }
                        hexCh = true;
                        num.append(d);
                        context.incrementIndex();
                    } else {
                        comp = true;
                        break digitParse; // first non-numeric character
                    }
                } // digitParse
                if (context.getIndex() >= len) {
                    throw new JSONParsingException("Overflow", context, v);
                }
                String numStr = num.toString();
                if (frcCh || expCh) {
                    final double D = StringTools.parseDouble(numStr,0.0);
                    val = (Number)(new Double(D));
                } else
                if (hexCh) {
                    final long L = StringTools.parseLong(numStr,0L);
                    // -- unfortunately cannot subclass "Long"
                    //val = (Number)(new Long(L) { public String toString() { return StringTools.toHexString(this.longValue(),16); } } );
                    val = (Number)(new Long(L));
                } else {
                    final long L = StringTools.parseLong(numStr,0L);
                    val = (Number)(new Long(L));
                }
                break numberParse;
            } else {
                throw new JSONParsingException("Missing initial Numeric +/-/0", context, v);
            }
        } // numberParse

        /* number completed? */
        if (!comp) {
            throw new JSONParsingException("Incomplete Number", context, v);
        }

        /* return number */
        return val; // may be null

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private JSON._Object object = null;
    private JSON._Array  array  = null;

    /**
    *** JSON: Constructor 
    **/
    public JSON()
    {
        super();
    }

    /**
    *** JSON: Constructor 
    **/
    public JSON(JSON._Object obj)
    {
        this.object = obj;
    }

    /**
    *** JSON: Constructor 
    **/
    public JSON(JSON._Array array)
    {
        this.array = array;
    }

    /**
    *** JSON: Constructor 
    **/
    public JSON(String json)
        throws JSONParsingException 
    {

        /* parse object/array */
        this._parseJSON(json);

    }

    /**
    *** JSON: Constructor 
    **/
    public JSON(InputStream input)
        throws JSONParsingException, IOException
    {

        /* nothing to parse */
        if (input == null) {
            throw new JSONParsingException("Invalid object/array", null, null);
        }

        /* read JSON string */
        this._parseJSON(StringTools.toStringValue(FileTools.readStream(input)));

    }

    /**
    *** JSON: Constructor 
    **/
    public JSON(File file)
        throws JSONParsingException, IOException
    {

        /* nothing to parse */
        if (file == null) {
            throw new JSONParsingException("Invalid object/array", null, null);
        }

        /* read/parse JSON string */
        this._parseJSON(StringTools.toStringValue(FileTools.readFile(file)));

    }

    /**
    *** JSON: Common JSON Object/Array parser
    **/
    private void _parseJSON(String json)
        throws JSONParsingException
    {

        /* first non-whitespace */
        int c = StringTools.indexOfFirstNonWhitespace(json);
        if (c < 0) { // also handles null String
            throw new JSONParsingException("Invalid object/array", null, json);
        }

        /* parse Object/Array */
        switch (json.charAt(c)) {
            case '{' :
                this.object = JSON.parse_Object(json);
                break;
            case '[' :
                this.array = JSON.parse_Array(json);
                break;
            default :
                JSONParsingContext context = new JSONParsingContext(c,1);
                throw new JSONParsingException("Invalid object/array", context, json);
        }

    }

    // ------------------------------------------------------------------------

    /** 
    *** JSON: Returns true if an object is defined
    **/
    public boolean hasObject()
    {
        return (this.object != null);
    }

    /** 
    *** JSON: Gets the main JSON._Object
    **/
    public JSON._Object getObject()
    {
        return this.object;
    }

    /** 
    *** JSON: Sets the main JSON._Object
    **/
    public void setObject(JSON._Object obj)
    {
        this.object = obj;
        this.array  = null;
    }

    // ------------------------------------------------------------------------

    /** 
    *** JSON: Returns true if an array is defined
    **/
    public boolean hasArray()
    {
        return (this.array != null);
    }

    /** 
    *** JSON: Gets the main JSON._Array
    **/
    public JSON._Array getArray()
    {
        return this.array;
    }

    /** 
    *** JSON: Sets the main JSON._Array
    **/
    public void setArray(JSON._Array array)
    {
        this.array  = array;
        this.object = null;
    }

    // ------------------------------------------------------------------------

    /**
    *** JSON: Gets the JSON._Value for the specified path
    **/
    public JSON._Value getValueForPath(String path) 
    {
        if (this.hasObject()) {
            return JSON.GetValueForPath(this.getObject(), path);
        } else
        if (this.hasArray()) {
            return JSON.GetValueForPath(this.getArray(), path);
        } else {
            return null;
        }
    }

    /**
    *** JSON: Gets the JSON._Value for the specified path
    **/
    public JSON._Value getValueForPath(String... path) 
    {
        if (this.hasObject()) {
            return JSON.GetValueForPath(this.getObject(), path);
        } else
        if (this.hasArray()) {
            return JSON.GetValueForPath(this.getArray(), path);
        } else {
            return null;
        }
    }

    /**
    *** JSON: Gets the Java Object for the specified path
    **/
    public Object getJavaObjectForPath(String path) {
        JSON._Value val = this.getValueForPath(path);
        return (val != null)? val.getJavaObject() : null;
    }

    /**
    *** JSON: Gets the Java Object for the specified path
    **/
    public Object getJavaObjectForPath(String... path) {
        JSON._Value val = this.getValueForPath(path);
        return (val != null)? val.getJavaObject() : null;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** JSON: Return a String representation of this instance
    **/
    public String toString()  // JSON
    {
        if (this.object != null) {
            return this.object.toString();
        } else
        if (this.array != null) {
            return this.array.toString();
        } else {
            return "";
        }
    }

    /**
    *** JSON: Return a String representation of this instance
    **/
    public String toString(boolean inclPrefix/*indent*/)  // JSON
    {
        if (this.object != null) {
            return this.object.toString(inclPrefix/*indent*/);
        } else
        if (this.array != null) {
            return this.array.toString(inclPrefix/*indent*/);
        } else {
            return "";
        }
    }

    /**
    *** JSON: Print object contents (debug purposes only)
    **/
    public void debugDisplayObject()
    {
        if (this.object != null) {
            this.object.debugDisplayObject(0);
        } else
        if (this.array != null) {
            this.array.debugDisplayArray(0);
        } else {
            Print.sysPrintln("n/a");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
