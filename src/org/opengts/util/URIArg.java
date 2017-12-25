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
//  URI argument wrapper
// ----------------------------------------------------------------------------
// Change History:
//  2006/05/15  Martin D. Flynn
//     -Initial release
//  2008/05/14  Martin D. Flynn
//     -Added method 'obfuscateArg'.
//  2009/07/01  Martin D. Flynn
//     -Fixed 'decodeArg' (improper conversion to hex value)
//     -Added 'rtpEncode'/'rtpDecode' methods
//  2010/11/29  Martin D. Flynn
//     -Added method/options to remove arguments with blank values
//  2011/01/28  Martin D. Flynn
//     -Changed "setArgValue" to add the argument, if not already present.
//     -Added methods to provide for multiple argument key name checks.
//  2015/08/16  Martin D. Flynn
//     -Added ability to specify which characters get obfuscated
//  2016/02/23  Martin D. Flynn
//     -Fixed NPE on copy-constructor when argument is null
//  2017/04/05  Martin D. Flynn
//     -Added support for "setUserInfo"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;
import java.math.BigInteger;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.UnsupportedEncodingException;

/**
*** URI argument wrapper
**/

public class URIArg
{

    // ------------------------------------------------------------------------

    /* a scrambled Base64 alphabet */
    private static final String     PROP_uriArgScrambleSeed     = "URIArg.ScrambleSeed";
    private static final BigInteger DefaultScrambleSeed         = BigInteger.ONE;

    private static       char       Base64Alphabet[]            = null;
    private static final char       Base64Pad                   = '.';

    /**
    *** Gets the scrambled Base64 alphabet
    *** @return The scrambled Base64 alphabet
    **/
    private static char[] getBase64Alphabet()
    {
        if (Base64Alphabet == null) {
            BigInteger seed = RTConfig.getBigInteger(PROP_uriArgScrambleSeed,DefaultScrambleSeed);
            Base64Alphabet  = Base64.shuffleAlphabet(seed);
            for (int i = 0; i < Base64Alphabet.length; i++) {
                if (Base64Alphabet[i] == '+') {
                    Base64Alphabet[i] = '-';
                } else
                if (Base64Alphabet[i] == '/') {
                    Base64Alphabet[i] = '_';
                }
            } 
            //Print.logInfo("Base64 Alpha ["+seed+"]: "+StringTools.toStringValue(Base64Alphabet));
        }
        return Base64Alphabet;
    };

    /**
    *** Descrambles String
    **/
    public static String _des64(String e)
    {
        if (!StringTools.isBlank(e)) {
            try {
                byte b[] = Base64.decode(e, getBase64Alphabet(), Base64Pad);
                return StringTools.toStringValue(b,' ');
            } catch (Base64.Base64DecodeException bde) {
                Print.logError("Invalid Base64 characters", bde);
                return "";
            }
        } else {
            return "";
        }
    }

    /**
    *** Scrambles String
    **/
    public static String _ens64(String d)
    {
        if (!StringTools.isBlank(d)) {
            return Base64.encode(StringTools.getBytes(d), getBase64Alphabet(), Base64Pad);
        } else {
            return "";
        }
    }

    /**
    *** Decodes an RTP encoded argument
    **/
    public static RTProperties parseRTP(String rtpArg)
    {
        if (!StringTools.isBlank(rtpArg)) {
            String s = _des64(rtpArg);
            if (!StringTools.isBlank(s)) {
                return new RTProperties(s);
            }
        }
        return null;
    }

    /**
    *** RTP encodes an argument
    **/
    public static String encodeRTP(RTProperties rtp)
    {
        if (rtp != null) {
            return _ens64(rtp.toString());
        }
        return null;
    }

    // ------------------------------------------------------------------------

    public  static final String DFT_NonEncodedChars     = "A_-.";
    private static       String NonEncodedChars         = DFT_NonEncodedChars;

    /**
    *** Sets the non-encoded characters.
    *** To not encode Alpha characters, include the letter "A" in the list of characters.
    *** To encode all characters, specify an empty String.
    *** To reset to the default non-encoded characters list, specify null.
    **/
    public static void SetDefaultNonEncodedCharacters(String nesc)
    {
        NonEncodedChars = (nesc != null)? nesc : DFT_NonEncodedChars;
    }

    /**
    *** Sets the non-encoded special characters
    **/
    public static String getDefaultNonEncodedCharacters()
    {
        return NonEncodedChars;
    }

    /**
    *** Returns true if the specified character should be hex-encoded in a URL
    *** @param ch    The character to test
    *** @param nesc  String containing non-encoded special characters
    *** @return True if the specified character should be hex-encoded in a URL
    **/
    private static boolean shouldEncodeArgChar(char ch, String nonEncChars)
    {
        String nesc = StringTools.trim(nonEncChars);
        if (Character.isLetterOrDigit(ch) && (nesc.indexOf('A') >= 0)) {
            return false;
        } else
        if (nesc.indexOf(ch) >= 0) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
    *** Hex-encodes a URL argument (if required)
    *** @param arg  The URL argument to encode (if required)
    *** @return The hex encoded argument
    **/
    public static String encodeArg(String arg)
    {
        String nesc = URIArg.getDefaultNonEncodedCharacters();
        return URIArg.encodeArg(null,arg,nesc).toString();
    }

    /**
    *** Hex-encodes a URL argument (if required)
    *** @param sb   The StringBuffer where the hex encoded String argument will be placed
    *** @param arg  The URL argument to encode (if required)
    *** @return The StringBuffer where the hex-encoded String will be placed
    **/
    public static StringBuffer encodeArg(StringBuffer sb, String arg)
    {
        String nesc = URIArg.getDefaultNonEncodedCharacters();
        return URIArg.encodeArg(sb,arg,nesc);
    }

    /**
    *** Hex-encodes a URL argument
    *** @param sb   The StringBuffer where the hex encoded String argument will be placed
    *** @param arg  The URL argument to encode
    *** @param nonEncChars  String containing characters for which encoding will not be performed (null for default)
    *** @return The StringBuffer where the hex-encoded String will be placed
    **/
    public static StringBuffer encodeArg(StringBuffer sb, String arg, String nonEncChars)
    {
        if (sb == null) { sb = new StringBuffer(); }
        String nesc = (nonEncChars != null)? nonEncChars : URIArg.getDefaultNonEncodedCharacters();
        if (arg != null) {
            char ch[] = new char[arg.length()];
            arg.getChars(0, arg.length(), ch, 0);
            for (int i = 0; i < ch.length; i++) {
                if (URIArg.shouldEncodeArgChar(ch[i],nesc)) {
                    // -- escape character
                    sb.append("%");
                    sb.append(Integer.toHexString(0x100 + (ch[i] & 0xFF)).substring(1));
                } else {
                    // -- letters and digits are ok as-is
                    sb.append(ch[i]);
                }
            }
        }
        return sb;
    }

    /**
    *** Obfuscates (hex-encodes) all characters in the String
    *** @param arg  The String to hex-encode
    *** @return The hex-encoded String
    **/
    public static String obfuscateArg(String arg)
    {
        return URIArg.encodeArg(new StringBuffer(),arg,"").toString();
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Decodes the specified hex-encoded argument (not yet fully tested)
    *** @param sb   The StringBuffer where the decoded String argument will be placed
    *** @param s    The String to decode
    *** @return The StringBuffer where the decoded String will be placed
    **/
    public static StringBuffer decodeArg(StringBuffer sb, String s)
    {
        if (sb == null) { sb = new StringBuffer(); }
        if (s != null) {
            char ch[] = new char[s.length()];
            s.getChars(0, s.length(), ch, 0);
            for (int i = 0; i < ch.length; i++) {
                if (ch[i] == '%') {
                    if ((i + 2) < ch.length) {
                        int ch1 = StringTools.hexIndex(ch[i+1]);
                        int ch2 = StringTools.hexIndex(ch[i+2]);
                        sb.append((char)(((ch1 << 4) | ch2) & 0xFF));
                        i += 2;
                    } else {
                        i = ch.length - 1;
                    }
                } else {
                    sb.append(ch[i]);
                }
            }
        }
        return sb;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the URL starts with a protocol definition (ie. "http://...")
    *** @param url  The URL to test
    *** @return True if the URL starts with a protocol definition
    **/
    public static boolean isAbsoluteURL(String url)
    {
        if (url == null) {
            return false;
        } else {
            // per "http://en.wikipedia.org/wiki/URI_scheme" all URL "schemes" contain only
            // alphanumeric or "." characters, and appears to be < 16 characters in length.
            for (int i = 0; (i < 16) && (i < url.length()); i++) {
                char ch = url.charAt(i);
                if (ch == ':') {
                    return true; // A colon is the first non-alphanumeric we ran in to
                } else
                if (!Character.isLetterOrDigit(ch) && (ch != '.')) {
                    return false;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean                 uniqueKeys = false;
    private Collection<KeyVal>      keys = null;
    private String                  nonEncodedChars = null;

    private String                  uri  = ""; // this.uri

    private String                  userInfo = null;

    /**
    *** Copy Constructor
    **/
    public URIArg(URIArg uriArg) 
    {
        if (uriArg != null) {
            this.setUniqueKeys(uriArg.hasUniqueKeys());
            this._setURI(uriArg.uri);
            this.setUserInfo(uriArg.userInfo);
            this.setKeys(uriArg.keys); // deep copy
        }
    }

    /**
    *** Constructor
    *** @param uri The URI
    **/
    public URIArg(String uri, boolean unique) 
    {
        this.setUniqueKeys(unique);
        this.setURI(uri);
    }

    /**
    *** Constructor
    *** @param uri The URI
    **/
    public URIArg(String uri) 
    {
        this(uri, false);
    }

    /**
    *** Constructor
    *** @param reqURL The URL (typically from "request.getRequestURL()")
    *** @param query  The request query string (typically from "request.getQueryString()")
    **/
    public URIArg(StringBuffer reqURL, String query) 
    {
        this.setUniqueKeys(false);
        this.setURI(StringTools.trim(reqURL) + "?" + StringTools.trim(query));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the 'Unique Arg' attribute
    *** @param unique  True to set unique args
    **/
    public void setUniqueKeys(boolean unique)
    {
        this.uniqueKeys = unique;
        this.setKeys(this.keys);
    }
    
    /**
    *** Gets the 'Unique Arg' attribute
    *** @return True if this instance retains only unique arguments/parameters
    **/
    public boolean hasUniqueKeys()
    {
        return this.uniqueKeys;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the non-encoded characters for this instance (null sets default)
    **/
    public void setNonEncodedCharacters(String nonEncChars)
    {
        this.nonEncodedChars = (nonEncChars != null)? nonEncChars : URIArg.getDefaultNonEncodedCharacters();
    }

    /**
    *** Add non-encoded character to list
    **/
    public void addNonEncodedCharacters(String nonEncChars)
    {
        if (!StringTools.isBlank(nonEncChars)) {
            String nec = this.getNonEncodedCharacters();
            char ch[] = nonEncChars.toCharArray();
            boolean changed = false;
            for (int i = 0; i < ch.length; i++) {
                if (nec.indexOf(ch[i]) < 0) {
                    nec += ch[i];
                    changed = true;
                }
            }
            if (changed) {
                this.setNonEncodedCharacters(nec);
            }
        }
    }

    /**
    *** Gers the non-encoded characters for this instance
    **/
    public String getNonEncodedCharacters()
    {
        return (this.nonEncodedChars != null)? this.nonEncodedChars : URIArg.getDefaultNonEncodedCharacters();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the URI from a string
    **/
    public void setURI(String uri) 
    {
        int p = (uri != null)? uri.indexOf("?") : -1;
        if (p >= 0) {
            // -- set URI
            this._setURI(uri.substring(0, p));
            // -- add args
            String a[] = StringTools.parseStringArray(uri.substring(p+1), "&");
            String nesc = this.getNonEncodedCharacters();
            for (int i = 0; i < a.length; i++) {
                String key = "", val = "";
                int e = a[i].indexOf("=");
                if (e >= 0) {
                    key = a[i].substring(0,e);
                    val = a[i].substring(e+1);
                } else {
                    key = a[i];
                    val = "";
                }
                this._addArg(key, val, false/*encode*/, nesc); // assume already encoded
            }
        } else {
            // -- set URI
            this._setURI(uri);
        }
    }

    /**
    *** Sets the URI from a string (must not contain any arguments/parameters)
    **/
    protected void _setURI(String uri)
    {
        if (StringTools.isBlank(uri)) {
            // -- blank/null URI
            this.uri = "";
        } else
        if (uri.indexOf("@") >= 0) {
            // -- may contain userInfo
            try {
                URL    url   = new URL(uri);
                String proto = url.getProtocol();
                String uInfo = url.getUserInfo(); // extract user:pass
                String host  = url.getHost();
                int    port  = url.getPort();
                String file  = url.getFile(); // without parameters
                this.uri     = (new URL(proto,host,port,file)).toString(); // sans user-info
                if (!StringTools.isBlank(uInfo)) {
                    this.setUserInfo(uInfo);
                }
            } catch (MalformedURLException mue) {
                // -- save as-is
                this.uri = uri;
            }
        } else {
            // -- save as-is
            this.uri = uri;
        }
    }

    // --------------------------------

    /**
    *** Gets the URI (without arguments/parameters)
    *** @return The URI
    **/
    public String getURI()
    {
        return this._getURI();
    }

    /**
    *** Gets the URI (without arguments/parameters)
    *** @return The URI
    **/
    public String _getURI()
    {
        return this.uri;
    }

    // --------------------------------

    /**
    *** Adds a file extension to the end of this URI, ".xml" etc. The 
    *** extension will be added to the URI if doesn't already end with it
    *** @param ext The extension to add
    **/
    public void addExtension(String ext)
    {
        if (!StringTools.isBlank(ext) && !this.uri.endsWith(ext)) {
            this.uri += ext;
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the argument keys in this URI
    *** @param k The argument keys
    **/
    protected void setKeys(Collection<KeyVal> k)
    {
        this.keys = null;
        if (k != null) {
            for (KeyVal kv : k) {
                this.getKeyValList().add(new KeyVal(kv)); // deep copy
            }
        }
    }

    /**
    *** Gets all the argument key/value pairs as a list of <code>KeyVal></code>s
    *** @return The list of argument key/value pairs
    **/
    protected Collection<KeyVal> getKeyValList()
    {
        if (this.keys == null) {
            this.keys = this.hasUniqueKeys()? new OrderedSet<KeyVal>() : new Vector<KeyVal>();
        }
        return this.keys;
    }

    /**
    *** Gets an <code>OrderedMap</code> of the argument key/value pairs indexed
    *** by their keys
    *** @return An <code>OrderedMap</code> of the argument key/value pairs
    **/
    protected OrderedMap<String,KeyVal> getKeyValMap()
    {
        OrderedMap<String,KeyVal> kvMap = new OrderedMap<String,KeyVal>();
        for (KeyVal kv : this.getKeyValList()) {
            // only the first occurance is retained
            String kn = kv.getKey();
            if (!kvMap.containsKey(kn)) {
                kvMap.put(kn, kv);
            }
        }
        return kvMap;
    }

    /**
    *** Gets a list of all the argument key names
    *** @return a list of all the argument key names
    **/
    public java.util.List<String> getArgNames()
    {
        java.util.List<String> knList = new Vector<String>();
        for (KeyVal kv : this.getKeyValList()) {
            knList.add(kv.getKey());
        }
        return knList;
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds an argument to the URI
    *** @param key The key name of the argument to add
    *** @param value The value of the new key
    *** @return This URIArg, with the argument added
    **/
    public URIArg addArg(String key, double value) 
    {
        String nesc = this.getNonEncodedCharacters();
        return this._addArg(key, String.valueOf(value), true/*encode*/, nesc);
    }

    /**
    *** Adds an argument to the URI
    *** @param key The key name of the argument to add
    *** @param value The value of the new key
    *** @return This URIArg, with the argument added
    **/
    public URIArg addArg(String key, int value) 
    {
        String nesc = this.getNonEncodedCharacters();
        return this._addArg(key, String.valueOf(value), true/*encode*/, nesc);
    }

    /**
    *** Adds an argument to the URI
    *** @param key The key name of the argument to add
    *** @param value The value of the new key
    *** @return This URIArg, with the argument added
    **/
    public URIArg addArg(String key, long value) 
    {
        String nesc = this.getNonEncodedCharacters();
        return this._addArg(key, String.valueOf(value), true/*encode*/, nesc);
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds an argument to the URI
    *** @param key The key name of the argument to add
    *** @param rtp The RTP encoded values of the new key
    *** @return This URIArg, with the argument added
    **/
    public URIArg addArg(String key, RTProperties rtp)
    {
        String nesc = this.getNonEncodedCharacters();
        String r = (rtp != null)? rtp.toString() : null;
        if (!StringTools.isBlank(r)) {
            return this._addArg(key, URIArg.encodeRTP(rtp), false/*encode*/, nesc);
        } else {
            return this.addArg(key, "");
        }
    }

    /**
    *** Adds an argument to the URI
    *** @param key The key name of the argument to add
    *** @param value The value of the new key
    *** @return This URIArg, with the argument added
    **/
    public URIArg addArg(String key, String value)
    {
        String nesc = this.getNonEncodedCharacters();
        return this._addArg(key, value, true/*encode*/, nesc);
    }

    /**
    *** Adds an argument to the URI
    *** @param key          The key name of the argument to add
    *** @param value        The value of the new key
    *** @param nonEncChars  String containing characters for which encoding will not be performed
    *** @return This URIArg, with the argument added
    **/
    public URIArg addArg(String key, String value, String nonEncChars)
    {
        return this._addArg(key, value, true/*encode*/, nonEncChars);
    }

    /**
    *** Adds an argument to the URI
    *** @param key          The key name of the argument to add
    *** @param value        The value of the new key
    *** @param encode       True if <code>value</code> shoudl be hex encoded
    *** @param nonEncChars  String containing characters for which encoding will not be performed
    *** @return This URIArg, with the argument added
    **/
    protected URIArg _addArg(String key, String value, boolean encode, String nonEncChars)
    {
        if (!StringTools.isBlank(key)) {
            String val = encode? this.encodeArg(value,nonEncChars) : value;
            this.getKeyValList().add(new KeyVal(key,val));
        }
        return this;
    }

    /**
    *** Adds an argument to the URI
    *** @param keyVal The key/value pair to add
    *** @return This URIArg, with the argument added
    **/
    protected URIArg addArg(KeyVal keyVal)
    {
        if (keyVal != null) {
            this.getKeyValList().add(new KeyVal(keyVal)); // deep copy
        }
        return this;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Removes all occurances of the specified key from the URI
    *** @param key The key to remove
    *** @return This URIArg, with the argument added
    **/
    public URIArg removeArg(String key)
    {
        if (key != null) {
            for (Iterator<KeyVal> i = this.getKeyValList().iterator(); i.hasNext();) {
                KeyVal kv = i.next();
                if (kv.getKey().equals(key)) {
                    i.remove();
                }
            }
        }
        return this;
    }
    
    /**
    *** Removes all occurances of the specified keys from the URI
    *** @param keys The key list to remove
    *** @return This URIArg, with the argument added
    **/
    public URIArg removeArg(String keys[])
    {
        if (keys != null) {
            for (String k : keys) {
                this.removeArg(k);
            }
        }
        return this;
    }

    /**
    *** Removes all arguments/parameters which have blank values
    *** @return This URIArg
    **/
    public URIArg removeBlankValues()
    {
        for (Iterator<KeyVal> i = this.getKeyValList().iterator(); i.hasNext();) {
            KeyVal kv = i.next();
            if (!kv.hasValue()) {
                i.remove();
            }
        }
        return this;
    }

    // ------------------------------------------------------------------------

    /**
    *** Return true if this URI contains the specified key
    *** @return True if this URI contains the specified key
    **/
    public boolean hasArg(String key)
    {
        return (this.getKeyVal(key) != null);
    }

    /**
    *** Return true if this URI contains one of the specified keys
    *** @return True if this URI contains one of the specified keys
    **/
    public boolean hasArg(String key[])
    {
        return (this.getKeyVal(key) != null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the specified argument key to the specified int value.
    *** @param key   The agrument key
    *** @param value The value to set
    *** @return True if the argument already existed, else false
    **/
    public boolean setArgValue(String key, int value)
    {
        String nesc = this.getNonEncodedCharacters();
        return this.setArgValue(key, String.valueOf(value), nesc);
    }

    /**
    *** Sets the specified argument key to the specified int value.
    *** @param key   The agrument key
    *** @param value The value to set
    *** @return True if the argument already existed, else false
    **/
    public boolean setArgValue(String key, long value)
    {
        String nesc = this.getNonEncodedCharacters();
        return this.setArgValue(key, String.valueOf(value), nesc);
    }

    /**
    *** Sets the specified argument key to the specified String value.
    *** @param key The key of the agrument
    *** @param value The value to set
    *** @return True if the argument already existed, else false
    **/
    public boolean setArgValue(String key, String value)
    {
        String nesc = this.getNonEncodedCharacters();
        return this.setArgValue(key, value, nesc);
    }

    /**
    *** Sets the specified argument key to the specified String value.
    *** @param key          The key of the agrument
    *** @param value        The value to set
    *** @param nonEncChars  String containing characters for which encoding will not be performed
    *** @return True if the argument already existed, else false
    **/
    public boolean setArgValue(String key, String value, String nonEncChars)
    {
        if (key != null) {
            KeyVal kv = this.getKeyVal(key);
            if (kv != null) {
                kv.setValue(this.encodeArg(value,nonEncChars));
                return true;
            } else {
                this.addArg(key,value,nonEncChars);
                return false; // new arg
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the specified argument key to the specified int value.
    *** @param key   The agrument key 
    *** @param value The value to set
    *** @return True if the argument already existed, else false
    **/
    public boolean setArgValue(String key[], int value)
    {
        String nesc = this.getNonEncodedCharacters();
        return this.setArgValue(key, String.valueOf(value), nesc);
    }

    /**
    *** Sets the specified argument key to the specified int value.
    *** @param key   The agrument key 
    *** @param value The value to set
    *** @return True if the argument already existed, else false
    **/
    public boolean setArgValue(String key[], long value)
    {
        String nesc = this.getNonEncodedCharacters();
        return this.setArgValue(key, String.valueOf(value), nesc);
    }

    /**
    *** Sets the specified argument key to the specified String value.
    *** @param key The key of the agrument
    *** @param value The value to set
    *** @return True if the argument already existed, else false
    **/
    public boolean setArgValue(String key[], String value)
    {
        String nesc = this.getNonEncodedCharacters();
        return this.setArgValue(key, value, nesc);
    }

    /**
    *** Sets the specified argument key to the specified String value.
    *** @param key          The key of the agrument
    *** @param value        The value to set
    *** @param nonEncChars  String containing characters for which encoding will not be performed
    *** @return True if the argument already existed, else false
    **/
    public boolean setArgValue(String key[], String value, String nonEncChars)
    {
        if (ListTools.size(key) >= 1) {
            boolean hadArg = this.hasArg(key);
            // -- remove all but the first key
            for (int i = 1; i < key.length; i++) {
                this.removeArg(key[i]);
            }
            // -- set the first key
            this.setArgValue(key[0], value, nonEncChars);
            return hadArg;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the first occuring KeyVal of the specified key
    *** @param key The key to get
    *** @return The KeyVal associated with <code>key</code>, if any
    **/
    protected KeyVal getKeyVal(String key)
    {
        if (key != null) {
            Collection<KeyVal> kvList = this.getKeyValList();
            for (KeyVal kv : kvList) {
                if (kv.getKey().equals(key)) {
                    return kv;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    /**
    *** Gets the first occuring KeyVal of the specified key
    *** @param keys The key to get
    *** @return The KeyVal associated with <code>key</code>, if any
    **/
    protected KeyVal getKeyVal(String keys[])
    {
        if (keys != null) {
            for (String k : keys) {
                KeyVal kv = this.getKeyVal(k);
                if (kv != null) {
                    return kv;
                }
            }
        }
        return null;
    }

    /**
    *** Gets the first occuring value of the specified key
    *** @param key The key to get
    *** @return The value associated with <code>key</code>, if any
    **/
    public String getArgValue(String key)
    {
        KeyVal kv = this.getKeyVal(key);
        return (kv != null)? kv.getValue() : null;
    }

    /**
    *** Gets the first occuring value of the specified key
    *** @param keys  The key list used to check for the returned value
    *** @return The value associated with <code>key</code>, if any
    **/
    public String getArgValue(String keys[])
    {
        KeyVal kv = this.getKeyVal(keys);
        return (kv != null)? kv.getValue() : null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of all key/values
    *** @return A String representation of all key/values
    **/
    public String getArgString()
    {
        return this.getArgString(null).toString();
    }

    /**
    *** Returns a String representation of all key/values
    *** @param sbuff The StringBuffer to write the key/values to, or null
    ***        for a new one
    *** @return A String representation of all key/values
    **/
    public StringBuffer getArgString(StringBuffer sbuff)
    {
        return this.getArgString(sbuff, true);
    }
    
    /**
    *** Returns a String representation of all key/values
    *** @param sbuff   The StringBuffer to write the key/values to, or null for a new one
    *** @param includeBlankValues True to include keys for blank values.
    *** @return A String representation of all key/values
    **/
    public StringBuffer getArgString(StringBuffer sbuff, boolean includeBlankValues)
    {
        StringBuffer sb = (sbuff != null)? sbuff : new StringBuffer();
        int argCnt = 0;
        for (Iterator<KeyVal> i = this.getKeyValList().iterator(); i.hasNext();) {
            KeyVal kv = i.next();
            if (includeBlankValues || kv.hasValue()) {
                if (argCnt > 0) {
                    sb.append("&");
                }
                sb.append(kv.toString());
                argCnt++;
            }
        }
        return sb;
    }
    
    /**
    *** Gets args as RTProperties instance
    *** @return A new RTProperties instance with this URIArg's key value pairs
    **/
    public RTProperties getArgProperties()
    {
        RTProperties rtp = new RTProperties();
        for (Iterator<KeyVal> i = this.getKeyValList().iterator(); i.hasNext();) {
            KeyVal kv = i.next();
            rtp.setString(kv.getKey(), this.decodeArg(kv.getValue()));
        }
        return rtp;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** A String reperesentation of this URI (with arguments/parameters)
    *** @return A String representation of this URI
    **/
    public String toString()
    {
        return this.toString(true);
    }

    /**
    *** A String reperesentation of this URI (with arguments/parameters)
    *** @param includeBlankValues True to include keys for blank values.
    *** @return A String representation of this URI
    **/
    public String toString(boolean includeBlankValues)
    {
        StringBuffer sb = new StringBuffer(this._getURI());
        if (!ListTools.isEmpty(this.getKeyValList())) {
            sb.append("?");
            this.getArgString(sb, includeBlankValues);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a new URIArg with this URIArg's arguments/parameters encoded into
    *** a single RTProperties added with a specified key [CHECK]
    *** @param rtpKey The key to add the encoded args at
    *** @param exclKeys keys to exclude from encoding
    *** @return A new URIArg with non excluded arguments/parameters encoded 
    **/
    public URIArg rtpEncode(String rtpKey, String... exclKeys)
    {
        URIArg rtpUrl = new URIArg(this._getURI());
        RTProperties rtp = new RTProperties();
        for (KeyVal kv : this.getKeyValList()) {
            String kn = kv.getKey();
            if (ListTools.contains(exclKeys,kn)) {
                rtpUrl.addArg(kv);
            } else {
                rtp.setString(kn, kv.getValue());
            }
        }
        rtpUrl.addArg(rtpKey, rtp);
        return rtpUrl;
    }

    /**
    *** Returns a new URIArg with this URIArg's arguments/parameters decoded from
    *** a single RTProperties added a specified key [CHECK]
    *** @param rtpKey The key of the RTProperties to decode the encoded args from
    *** @return A new URIArg with non excluded arguments/parameters encoded 
    **/
    public URIArg rtpDecode(String rtpKey)
    {
        URIArg cpyUrl = new URIArg(this._getURI());
        for (KeyVal kv : this.getKeyValList()) {
            String kn = kv.getKey();
            if (!kn.equals(rtpKey)) {
                cpyUrl.addArg(kv);
            } else {
                RTProperties rtp = URIArg.parseRTP(kv.getValue());
                for (Object rpk : rtp.getPropertyKeys()) {
                    String rk = rpk.toString();
                    String rv = rtp.getString(rk,"");
                    cpyUrl.addArg(rk,rv);
                }
            }
        }
        return cpyUrl;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns this URI as a new URL
    *** @return A URL representing this URI
    **/
    public URL toURL()
        throws MalformedURLException 
    {
        return new URL(this.toString());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the 'protocol' specification in the URI
    *** @return The 'protocol' specification in the URL, or null if unable to determine protocol
    **/
    public String getProtocol()
    {
        try {
            URL url = new URL(this._getURI());
            return url.getProtocol();
        } catch (MalformedURLException mue) {
            return null;
        }
    }

    /** 
    *** Sets the 'protocol'
    **/
    public boolean setProtocol(String _proto)
    {
        String uri = this._getURI(); // without userInfo or parameters
        if (!StringTools.isBlank(_proto) && URIArg.isAbsoluteURL(uri)) {
            try {
                URL   oldURI = new URL(uri);
                String proto = _proto;
                String host  = oldURI.getHost();
                int    port  = oldURI.getPort();
                String file  = oldURI.getFile(); // without parameters
                URL newURI = new URL(proto, host, port, file);
                this._setURI(newURI.toString()); // sans user-info
                return true;
            } catch (MalformedURLException mue) {
                // -- error
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the user-info is defined.
    **/
    public boolean hasUserInfo()
    {
        return !StringTools.isBlank(this.getUserInfo())? true : false;
    }

    /**
    *** Gets the user-info specification in the URI
    *** @return The user-info specification in the URL, or null/blank if not specified
    **/
    public String getUserInfo()
    {
        return !StringTools.isBlank(this.userInfo)? this.userInfo : null;
    }

    /**
    *** Sets the user-info
    **/
    public boolean setUserInfo(String userPass)
    {
        if (!StringTools.isBlank(userPass)) {
            this.userInfo = StringTools.trim(userPass);
        } else {
            this.userInfo = null;
        }
        return true;

        /*
        // -- The user-info is not well supported by the URL class (probably because
        // -  is a potential security issue and has been deprecated :-), so we need
        // -  to extract/add the user-info manually.
        // -- validate URI
        String uri = this._getURI();
        if (!URIArg.isAbsoluteURL(uri)) {
            return false;
        }
        // -- check for protocol separator
        int p = uri.indexOf("://");
        if (p < 0) {
            return false;
        }
        p += "://".length();
        // -- assemble url
        StringBuffer urlSB = new StringBuffer();
        urlSB.append(uri.substring(0,p));
        if (!StringTools.isBlank(userPass)) {
            urlSB.append(userPass).append("@");
        }
        int a = uri.indexOf("@",p);
        if (a >= 0) {
            urlSB.append(uri.substring(a+1));
        } else {
            urlSB.append(uri.substring(p));
        }
        // -- set and return
        this._setURI(urlSB.toString());
        return true;
        */

    }

    /**
    *** Sets the user-info
    **/
    public boolean setUserInfo(String user, String pass)
    {

        /* user:pass */
        StringBuffer upSB = new StringBuffer();
        upSB.append(StringTools.trim(user));
        if (!StringTools.isBlank(pass)) {
            upSB.append(":");
            upSB.append(pass);
        }

        /* set user:pass */
        return this.setUserInfo(upSB.toString());

    }

    // --------------------------------

    /**
    *** Returns true if the Authorization header value is defined.
    **/
    public boolean hasAuthorizationHeader()
    {
        return this.hasUserInfo();
    }

    /**
    *** Gets the Authorization header value, based on the current user-info.
    *** @return The authorization header value, or null if the user-info is not defined.
    **/
    public String getAuthorizationHeader()
    {
        String usrInf = this.getUserInfo();
        if (StringTools.isBlank(usrInf)) {
            return null;
        } else {
            try {
                return "Basic " + Base64.encode(usrInf.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException uce) {
                // -- "UTF-8" error (unlikely)
                return "Basic " + Base64.encode(usrInf.getBytes());
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the 'host' specification in the URI
    *** @return The 'host' specification in the URL, or null if unable to determine host
    **/
    public String getHost()
    {
        try {
            URL url = new URL(this._getURI());
            return url.getHost();
        } catch (MalformedURLException mue) {
            return null;
        }
    }

    /**
    *** Sets the 'host'
    **/
    public boolean setHost(String _host)
    {
        String uri = this._getURI(); // without userInfo or parameters
        if (!StringTools.isBlank(_host) && URIArg.isAbsoluteURL(uri)) {
            try {
                URL   oldURI = new URL(uri);
                String proto = oldURI.getProtocol();
                String host  = _host;
                int    port  = oldURI.getPort();
                String file  = oldURI.getFile(); // without parameters
                URL newURI = new URL(proto, host, port, file);
                this._setURI(newURI.toString()); // sans user-info
                return true;
            } catch (MalformedURLException mue) {
                // -- error
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the 'port' specification in the URI
    *** @return The 'port' specification in the URL, or -1 if not port is specified
    **/
    public int getPort()
    {
        try {
            URL url = new URL(this._getURI());
            return url.getPort();
        } catch (MalformedURLException mue) {
            return -1;
        }
    }

    /** 
    *** Sets the 'port'
    **/
    public boolean setPort(int _port)
    {
        String uri = this._getURI(); // without userInfo or parameters
        if ((_port > 0) && URIArg.isAbsoluteURL(uri)) {
            try {
                URL   oldURI = new URL(uri);
                String proto = oldURI.getProtocol();
                String host  = oldURI.getHost();
                int    port  = _port;
                String file  = oldURI.getFile(); // without parameters
                URL newURI = new URL(proto, host, port, file);
                this._setURI(newURI.toString()); // sans user-info
                return true;
            } catch (MalformedURLException mue) {
                // -- error
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the 'file' specification in the URI
    *** @return The 'file' specification in the URL, or null if unable to determine file
    **/
    public String getFile()
    {
        try {
            URL url = new URL(this.toString());
            return url.getFile();
        } catch (MalformedURLException mue) {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the 'path' specification in the URI
    *** @return The 'path' specification in the URL, or null if unable to determine path
    **/
    public String getPath()
    {
        try {
            URL url = new URL(this._getURI());
            return url.getPath();
        } catch (MalformedURLException mue) {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the query string
    *** @return The query string
    **/
    public String getQuery()
    {
        return this.getArgString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Hex-encodes a URL argument
    *** @param arg          The URL argument to encode
    *** @param nonEncChars  String containing characters for which encoding will not be performed
    *** @return The hex-encoded String (if encoded)
    **/
    private String encodeArg(String arg, String nonEncChars)
    {
        StringBuffer sb = URIArg.encodeArg(null, arg, nonEncChars);
        return sb.toString();
    }

    /**
    *** Decodes the specified hex-encoded argument (not yet fully tested)
    *** @param s    The String to decode
    *** @return The decoded String
    **/
    private String decodeArg(String s)
    {
        StringBuffer sb = URIArg.decodeArg(null, s);
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** A URI argument key/value pair
    **/
    protected class KeyVal
    {
        private String key = "";
        private String val = "";
        public KeyVal(KeyVal kv) {
            this.key = (kv != null)? kv.getKey()   : "";
            this.val = (kv != null)? kv.getValue() : "";
        }
        public KeyVal(String key, String val) {
            this.key = (key != null)? key : "";
            this.val = (val != null)? val : "";
        }
        public String getKey() {
            return this.key;
        }
        public boolean hasValue() {
            return ((this.val != null) && !this.val.equals("")); // don't use StringTools.isBlank(...)
        }
        public String getValue() {
            return this.val;
        }
        public void setValue(String val) {
            this.val = (val != null)? val : "";
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.key);
            if (this.hasValue()) {
                sb.append("=").append(this.val);
            }
            return sb.toString();
        }
        public boolean equals(Object other) {
            if (!(other instanceof KeyVal)) {
                return false;
            } else
            if (URIArg.this.hasUniqueKeys()) {
                return this.getKey().equals(((KeyVal)other).getKey());
            } else {
                return this.toString().equals(other.toString());
            }
        }
        public int hashCode() {
            return super.hashCode();
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String ARG_PARSEURL[]  = new String[] { "parseURL", "url" };
    private static final String ARG_DECODE[]    = new String[] { "decode"  , "d"   };
    private static final String ARG_ENCODE[]    = new String[] { "encode"  , "e"   };
    private static final String ARG_RTPENC[]    = new String[] { "rtpEnc"          };
    private static final String ARG_RTPDEC[]    = new String[] { "rtpDec"          };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + URIArg.class.getName() + " {options}");
        Print.logInfo("Options:");
        Print.logInfo("  -encode=<ASCII>    Encode ASCII string to URL argument string");
        Print.logInfo("  -decode=<args>     Decode URL argument string to ASCII");
        Print.logInfo("  -rtpEnc=<url>      RTP Encode URL [key = 'rtp']");
        Print.logInfo("  -rtpDec=<url>      RTP Decode URL [key = 'rtp']");
        System.exit(1);
    }

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* decode URI argument strings */
        if (RTConfig.hasProperty(ARG_DECODE)) {
            String a = RTConfig.getString(ARG_DECODE,"");
            String s = URIArg.decodeArg(new StringBuffer(),a).toString();
            Print.sysPrintln("ASCII: " + s);
            System.exit(0);
        }

        /* encode Base64 strings */
        if (RTConfig.hasProperty(ARG_ENCODE)) {
            String s = RTConfig.getString(ARG_ENCODE,"");
            String n = URIArg.getDefaultNonEncodedCharacters();
            String a = URIArg.encodeArg(new StringBuffer(),s,n).toString();
            Print.sysPrintln("Args: " + a);
            System.exit(0);
        }

        /* RTP decode */
        if (RTConfig.hasProperty(ARG_RTPDEC)) {
            URIArg rtpUrl = new URIArg(RTConfig.getString(ARG_RTPDEC,""));
            URIArg decUrl = rtpUrl.rtpDecode("rtp");
            Print.sysPrintln("URL: " + decUrl.toString());
            System.exit(0);
        }

        /* RTP encode */
        if (RTConfig.hasProperty(ARG_RTPENC)) {
            URIArg decUrl = new URIArg(RTConfig.getString(ARG_RTPENC,""));
            URIArg rtpUrl = decUrl.rtpEncode("rtp");
            Print.sysPrintln("URL: " + rtpUrl.toString());
            System.exit(0);
        }

        /* Parse URL */
        if (RTConfig.hasProperty(ARG_PARSEURL)) {
            String urlStr = RTConfig.getString(ARG_PARSEURL,"");
            //try {
                URIArg url = new URIArg(urlStr);
                Print.logInfo("URL      : " + url);
                Print.logInfo("URL proto: " + url.getProtocol());
                Print.logInfo("URL user : " + url.getUserInfo());
                Print.logInfo("URL host : " + url.getHost());
                Print.logInfo("URL port : " + url.getPort());
                Print.logInfo("URL file : " + url.getFile());
                Print.logInfo("URL path : " + url.getPath());
                Print.logInfo("URL query: " + url.getQuery());
	        //} catch (MalformedURLException mfue) {
	        //    Print.logError("Unable to parse URL: " + urlStr);
	        //    System.exit(99);
	        //}
            System.exit(0);
        }

        /* no options */
        usage();
        
    }

}
