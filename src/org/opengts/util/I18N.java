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
// Description: Internationalization (I18N) support module
//   This modules looks for "LocalStrings_XX.properties" files in the specified 
//   package directory.  Within this file contains i18n keys, and displayed text
//   which is localized based on the language/country code of the containing file.
// File name format:
//   Localization files should have a naming format of "LocalStrings_XX[_YY].properties",
//   where "XX" is the localized language, and YY (optional) is the country code.
//   The language abbreviation should match the ISO-639 two-letter code, and the
//   country abbreviation should match the ISO-3166 two-letter code (if specified).
// Example language codes: [http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes]
//   en - English
//   es - Spanish
//   de - German
//   fr - French
//   it - Italian
// ----------------------------------------------------------------------------
// ISO-639:
//  - http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
// ISO-3166:
//  - http://en.wikipedia.org/wiki/ISO_3166-1
// ----------------------------------------------------------------------------
// Change History:
//  2007/05/25  Martin D. Flynn
//     -Initial release
//  2008/05/14  Martin D. Flynn
//     -Added command-line arguments to allow specific package/locale/key lookups.
//  2008/05/20  Martin D. Flynn
//     -Added a static 'getString(...)' method to allow for lazy localization
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.text.*;
import java.awt.*;

/**
*** A set of tools for creating i18n compliant code and providing localized text
**/

public class I18N
{

    // ------------------------------------------------------------------------
    // English: LocalStrings_en_US.properties
    // Mexican: LocalStrings_es_MX.properties

    public  static final String LOCAL_STRINGS   = "LocalStrings";
    public  static final String _LOCAL_STRINGS  = "." + LOCAL_STRINGS;

    // ------------------------------------------------------------------------
    
    private static Map<Locale,Map<String,I18N>> localeMap = new HashMap<Locale,Map<String,I18N>>();

    /**
    *** Returns an I18N instance based on the specified package name and Locale
    *** @param pkgClz  The class from which the class package is derived
    *** @param loc     The Locale resource from with the localized text is loaded
    **/
    public static I18N getI18N(Class<?> pkgClz, Locale loc)
    {
        return I18N.getI18N(pkgClz.getPackage().getName(), loc);
    }

    /**
    *** Returns an I18N instance based on the specified package name and Locale
    *** @param pkg     The resource package
    *** @param loc     The Locale resource from with the localized text is loaded
    **/
    public static I18N getI18N(Package pkg, Locale loc)
    {
        return I18N.getI18N(pkg.getName(), loc);
    }

    /**
    *** Returns an I18N instance based on the specified package name and Locale
    *** @param pkgName The resource package name
    *** @param loc     The Locale resource from with the localized text is loaded
    **/
    public static I18N getI18N(String pkgName, Locale loc)
    {
        if (pkgName != null) {
            loc = I18N.getLocale(loc);

            /* get package map for specific Locale */
            Map<String,I18N> packageMap = localeMap.get(loc);
            if (packageMap == null) {
                packageMap = new HashMap<String,I18N>();
                localeMap.put(loc, packageMap);
            }

            /* get I18N instance for package */
            I18N i18n = packageMap.get(pkgName);
            if (i18n == null) {
                i18n = new I18N(pkgName, loc);
                packageMap.put(pkgName, i18n);
            }
            return i18n;

        } else {

            /* no package specified */
            return null;

        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    //public static Locale getLocale()
    //{
    //    return I18N.getLocale((String)null);
    //}
    
    /**
    *** Gets the Java Locale instance based on the specified locale name
    *** @param loc  The name of the Locale
    *** @return The Java Locale instance
    **/
    public static Locale getLocale(String loc)
    {
        Locale locale = I18N.getLocale(loc, null);
        return (locale != null)? locale : I18N.getDefaultLocale();
    }
    
    /**
    *** Gets the Java Locale instance based on the specified locale name
    *** @param loc  The name of the Locale
    *** @param dft  The default Locale returned
    *** @return The Java Locale instance
    **/
    public static Locale getLocale(String loc, Locale dft)
    {
        String locale = !StringTools.isBlank(loc)? loc : RTConfig.getString(RTKey.LOCALE,"");
        if (StringTools.isBlank(locale)) {
            return dft;
        } else {
            int p = locale.indexOf("_");
            try {
                if (p < 0) {
                    String language = locale;
                    return new Locale(language);
                } else {
                    String language = locale.substring(0,p);
                    String country  = locale.substring(p+1);
                    return new Locale(language,country);
                }
            } catch (Throwable th) {
                return dft;
            }
        }
    }
    
    /**
    *** Returns the specified Locale, or the default Locale if the specified Locale is null
    *** @param loc  The default Locale
    *** @return A Java Locale instance
    **/
    public static Locale getLocale(Locale loc)
    {
        if (loc != null) {
            return loc;
        } else {
            return I18N.getDefaultLocale();
        }
    }
    
    /**
    *** Gets the System default Locale
    *** @return The default Java Locale instance
    **/
    public static Locale getDefaultLocale()
    {
        return Locale.getDefault(); // System default
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private ResourceBundle resBundle = null;
    private Locale locale = null;
    
    /**
    *** Constructor
    *** @param pkgName The resource package name
    *** @param loc     The Locale resource from with the localized text is loaded
    **/
    private I18N(String pkgName, Locale loc)
    {
        String bundleName = null;
        try {
            this.locale = I18N.getLocale(loc);
            bundleName = ((pkgName == null) || pkgName.equals(""))? LOCAL_STRINGS : (pkgName + _LOCAL_STRINGS);
            this.resBundle = ResourceBundle.getBundle(bundleName, this.locale);
            //Print.logInfo("Found bundle: " + bundleName);
        } catch (Throwable th) { 
            // MissingResourceException
            if (loc != null) {
                // quietly ignore this exception if (loc == null)
                Print.logInfo("Bundle not found: " + bundleName + " [" + th);
            }
            this.resBundle = null;
        }
    }
    
    /**
    *** Constructor (the default Locale will be assumed)
    *** @param pkgName The resource package name
    **/
    private I18N(String pkgName)
    {
        this(pkgName, null);
    }
    
    // ------------------------------------------------------------------------

    /**
    *** gets the Locale for this I18N instance
    *** @return The Locale
    **/
    public Locale getLocale()
    {
        return this.locale;
    }
    
    // ------------------------------------------------------------------------

    /** 
    *** Gets an Enumeration of the LocalString keys for this I18N instance
    *** @return The Enumeration of the LocalString keys for this I18N instance
    **/
    public Enumeration<String> getKeys()
    {
        return (this.resBundle != null)? this.resBundle.getKeys() : null;
    }
    
    /**
    *** Prints all LocalString keys for this I18N instance
    **/
    public void printKeyValues()
    {
        Enumeration<String> e = this.getKeys();
        if (e != null) {
            for (;e.hasMoreElements();) {
                String k = e.nextElement();
                String v = this.getString(k,"?");
                Print.logInfo("Key:" + k + " Value:" + v);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the Localized value for the specified key.  The default String text is returned
    *** if the specified key does not exist
    *** @param key  The LocalStrings key
    *** @param dft  The default String text to return if the LocalStrings key does not exist
    *** @return The Localized String text
    **/
    public String getString(String key, String dft)
    {
        if (!StringTools.isBlank(key) && (this.resBundle != null)) {
            RTProperties cfgProps = RTConfig.getConfigFileProperties();
            if (!cfgProps.hasProperty(key) || cfgProps.getBoolean(key,true)) {
                try {
                    String s = this.resBundle.getString(key);
                    if (s != null) {
                        return I18N.decodeNewLine(s);
                    }
                } catch (Throwable th) {
                    //Print.logException("",th);
                    // MissingResourceException - if no object for the given key can be found 
                    // ClassCastException - if the object found for the given key is not a string
                }
            }
        }
        return I18N.decodeNewLine(dft);
    }
    
    /**
    *** Gets the Localized value for the specified key.  The default String text is return if
    *** the specified key does not exist
    *** @param key  The LocalStrings key
    *** @param dft  The default String text to return if the LocalStrings key does not exist
    *** @param args An array of replacement fields
    *** @return The Localized String text
    **/
    public String getString(String key, String dft, Object args[])
    {
        String val = this.getString(key, dft);
        if ((args != null) && (args.length > 0) && (val != null)) {
            try {
                MessageFormat mf = new MessageFormat(val);
                mf.setLocale(this.locale);
                StringBuffer sb = mf.format(args, new StringBuffer(), null);
                return I18N.decodeNewLine(sb).toString();
            } catch (Throwable th) {
                Print.logInfo("Exception: " + key + " ==> " + val);
            }
        }
        return I18N.decodeNewLine(val);
    }
    
    /**
    *** Gets the Localized value for the specified key.  The default String text is return if
    *** the specified key does not exist
    *** @param key  The LocalStrings key
    *** @param dft  The default String text to return if the LocalStrings key does not exist
    *** @param arg  A single replacement field
    *** @return The Localized String text
    **/
    public String getString(String key, String dft, Object arg)
    {
        return this.getString(key, dft, new Object[] { NonNull(arg) });
    }

    /**
    *** Gets the Localized value for the specified key.  The default String text is return if
    *** the specified key does not exist
    *** @param key  The LocalStrings key
    *** @param dft  The default String text to return if the LocalStrings key does not exist
    *** @param arg0 The first replacement field
    *** @param arg1 The second replacement field
    *** @return The Localized String text
    **/
    public String getString(String key, String dft, Object arg0, Object arg1)
    {
        return this.getString(key, dft, new Object[] { NonNull(arg0), NonNull(arg1) });
    }

    /**
    *** Gets the Localized value for the specified key.  The default String text is return if
    *** the specified key does not exist
    *** @param key  The LocalStrings key
    *** @param dft  The default String text to return if the LocalStrings key does not exist
    *** @param arg0 The first  replacement field
    *** @param arg1 The second replacement field
    *** @param arg2 The third  replacement field
    *** @return The Localized String text
    **/
    public String getString(String key, String dft, Object arg0, Object arg1, Object arg2)
    {
        return this.getString(key, dft, new Object[] { NonNull(arg0), NonNull(arg1), NonNull(arg2) });
    }

    /**
    *** Gets the Localized value for the specified key.  The default String text is return if
    *** the specified key does not exist
    *** @param key  The LocalStrings key
    *** @param dft  The default String text to return if the LocalStrings key does not exist
    *** @param arg0 The first  replacement field
    *** @param arg1 The second replacement field
    *** @param arg2 The third  replacement field
    *** @param arg3 The forth  replacement field
    *** @return The Localized String text
    **/
    public String getString(String key, String dft, Object arg0, Object arg1, Object arg2, Object arg3)
    {
        return this.getString(key, dft, new Object[] { NonNull(arg0), NonNull(arg1), NonNull(arg2), NonNull(arg3) });
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the specified Object, or an empty String if the specified Object is null
    *** @param obj  The Object to return
    *** @return The specified Object, or an empty String if the specified Object is null
    **/
    protected static Object NonNull(Object obj)
    {
        return (obj != null)? obj : "";
    }

    /**
    *** Converts "\\n" patterns into literal newlines (\n)
    *** @param s  The String to convert "\\n" to "\n"
    *** @return The decoded String
    **/
    protected static String decodeNewLine(String s)
    {
        return StringTools.replace(s, "\\n", "\n");
    }

    /**
    *** Converts "\\n" patterns into literal newlines (\n)
    *** @param s  The StringBuffer to convert "\\n" to "\n"
    *** @return The decoded StringBuffer
    **/
    protected static StringBuffer decodeNewLine(StringBuffer s)
    {
        return StringTools.replace(s, "\\n", "\n");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // XML text localization
    // <Text>[$I18N:ReportFactory.sampleText]This is default text</Text>
    // <Text>[$I18N=ReportFactory.sampleText]This is default text</Text>
    // <Text>[$i18n=ReportFactory.sampleText]This is default text</Text>

    protected static final String I18N_KEY_STARTE  = "[$I18N=";
    protected static final String I18N_KEY_STARTC  = "[$I18N:";
    protected static final String I18N_KEY_END     = "]";

    /**
    *** Class used to provide lazy localization
    **/
    public static class Text
    {
        private String pkg      = null;
        private String key      = "";
        private String dft      = "";
        public Text() {
            this._set(null,null,null);
        }
        public Text(String dft) {
            this._set(null,null,dft);
        }
        public Text(String pkg, String key, String dft) {
            this._set(pkg, key, dft);
        }
        public Text(Class<?> clazz, String key, String dft) {
            String pkg = (clazz != null)? clazz.getPackage().getName() : null;
            this._set(pkg, key, dft);
        }
        private void _set(String pkg, String key, String dft) {
            this.pkg = ((pkg != null) && !pkg.equals(""))? pkg : null; // may be null
            this.key = (key != null)? key : "";
            this.dft = (dft != null)? dft : "";
            //Print.logInfo("I18N Text: key=" + key + " default=" + dft);
        }
        public void set(I18N.Text txt) {
            if (txt != null) {
                this._set(txt.pkg, txt.key, txt.dft);
            }
        }
        public boolean hasPackage() {
            return !StringTools.isBlank(this.getPackage());
        }
        public String getPackage() {
            return this.pkg; // may be null
        }
        public boolean hasKey() {
            return !StringTools.isBlank(this.getKey());
        }
        public String getKey() {
            return this.key; // never null
        }
        public boolean hasDefault() {
            return ((this.dft != null) && !this.dft.equals(""))? true : false;
        }
        public String getDefault() {
            return I18N.decodeNewLine(this.dft); // never null
        }
        public String getDefault(boolean decodeNewLine) {
            return decodeNewLine? I18N.decodeNewLine(this.dft) : this.dft; // never null
        }
        public String toString() {
            String locStr = RTConfig.getString(RTKey.SESSION_LOCALE,null);
            if (StringTools.isBlank(locStr)) {
                return this.getDefault();
            } else {
                Locale loc = I18N.getLocale(locStr);
                return this.toString(loc);
            }
        }
        public String toString(I18N i18n) {
            return this.toString(i18n, this.dft, null);
        }
        public String toString(I18N i18n, String dftVal) {
            return this.toString(i18n, dftVal, null);
        }
        public String toString(I18N i18n, String args[]) {
            return this.toString(i18n, this.dft, args);
        }
        public String toString(I18N i18n, String dftVal, Object args[]) {
            String dv = (dftVal != null)? dftVal : this.dft;
            if (i18n != null) {
                return i18n.getString(this.getKey(), dv, args);
            } else {
                return I18N.decodeNewLine(dv);
            }
        }
        public String toString(Locale loc) {
            return this.toString(loc, this.dft, null);
        }
        public String toString(Locale loc, String dftVal) {
            return this.toString(loc, dftVal, null);
        }
        public String toString(Locale loc, String args[]) {
            return this.toString(loc, null, args);
        }
        public String toString(Locale loc, String dftVal, String args[]) {
            String dv = (dftVal != null)? dftVal : this.dft;
            if ((loc != null) && (this.pkg != null)) {
                return this.toString(I18N.getI18N(this.pkg,loc), dv, args);
            } else {
                return I18N.decodeNewLine(dv);
            }
        }
        public boolean equals(Object other) {
            if (!(other instanceof Text)) {
                return false;
            }
            Text that = (Text)other;
            if (!this.getKey().equals(that.getKey())) {
                return false; // key does not match
            } else
            if (!this.getDefault(false).equals(that.getDefault(false))) {
                return true; // default text does not match
            } else
            if (this.hasPackage() != that.hasPackage()) {
                return false; // package does not match
            } else
            if (this.hasPackage() && !this.getPackage().equals(that.getPackage())) {
                return false; // package does not match
            } else {
                return true;
            }
        }
        public int hashCode() {
            return super.hashCode();
        }
    }

    /**
    *** Returns an I18N.Text instance used for lazy localization.<br>
    *** (use in XML loaders to avoid expression matches when auto-updating 'LocalStrings_XX.properties')
    *** @param pkg    The package name
    *** @param key    The localization key
    *** @param dft    The default localized text
    *** @return An I18N.Text instance used for lazy localization
    **/
    public static I18N.Text parseText(String pkg, String key, String dft)
    {
        return parseText(pkg, key, dft, true);
    }
    
    /**
    *** Returns an I18N.Text instance used for lazy localization.<br>
    *** (use in XML loaders to avoid expression matches when auto-updating 'LocalStrings_XX.properties')
    *** @param pkg    The package name
    *** @param key    The localization key
    *** @param dft    The default localized text
    *** @param showError  If true, a stacktrace will be display if the key is invalid.
    *** @return An I18N.Text instance used for lazy localization
    **/
    public static I18N.Text parseText(String pkg, String key, String dft, boolean showError)
    {
        if (dft == null) {
            Print.logStackTrace("Default value is null!");
            return new I18N.Text(pkg, key, "");
        } else
        if (!StringTools.isBlank(key)) {
            return new I18N.Text(pkg, key, dft);
        } else
        if (!StringTools.startsWithIgnoreCase(dft,I18N_KEY_STARTE) &&
            !StringTools.startsWithIgnoreCase(dft,I18N_KEY_STARTC)   ) {
            if (showError) {
                Print.logStackTrace("Invalid/missing key definition!\n" +
                    "Package: " + pkg + "\n" +
                    "Key    : " + key + "\n" +
                    "Default: " + dft);
            }
            return new I18N.Text(pkg, null, dft);
        } else {
            int ks = I18N_KEY_STARTE.length();
            int ke = dft.indexOf(I18N_KEY_END, ks);
            if (ke < ks) {
                return new I18N.Text(pkg, null, dft); // ']' is missing, return string as-is
            }
            String k = dft.substring(ks, ke).trim();
            String v = dft.substring(ke + I18N_KEY_END.length()).trim();
            return new I18N.Text(pkg, k, v);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an I18N.Text instance used for lazy localization
    *** @param clazz  The class from which the package is derived
    *** @param key    The localization key
    *** @param dft    The default localized text
    *** @return An I18N.Text instance used for lazy localization
    **/
    public static I18N.Text _getString(Class<?> clazz, String key, String dft)
    {
        // i18n 'key' is separately specified
        String pkg = (clazz != null)? clazz.getPackage().getName() : null;
        return I18N.parseText(pkg, key, dft);
    }

    /**
    *** Returns an I18N.Text instance used for lazy localization
    *** @param clazz  The class from which the package is derived
    *** @param key    The localization key
    *** @param dft    The default localized text
    *** @return An I18N.Text instance used for lazy localization
    **/
    public static I18N.Text getString(Class<?> clazz, String key, String dft)
    {
        // i18n 'key' is separately specified
        String pkg = (clazz != null)? clazz.getPackage().getName() : null;
        return I18N.parseText(pkg, key, dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
