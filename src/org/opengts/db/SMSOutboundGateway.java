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
//  Outbound SMS Gateway support
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Change History:
//  2010/07/18  Martin D. Flynn
//     -Initial release
//  2010/11/29  Martin D. Flynn
//     -Added "httpURL" format
//  2011/03/08  Martin D. Flynn
//     -Changed "getStringProperty" to first look at the account BasicPrivateLabel
//  2011/05/13  Martin D. Flynn
//     -Look for additional replacement vars "${var}", "%{var}", and "{VAR}",
//      where "var" is "mobile", "message", and "sender".
//  2012/02/03  Martin D. Flynn
//     -Added method "RemovePrefixSMS(...)"
//  2012/04/03  Martin D. Flynn
//     -Added ability to read http-base SMS authorization from Account record
//     -Added "1mobile" replacement var which prepends "1" to "6505551212".
//  2012/09/02  Martin D. Flynn
//     -Added "%{simID}" command URL replacement variable.
//     -Added support for "SmsGatewayHandler.GWNAME.maxMessageLength" property
//  2012/10/16  Martin D. Flynn
//     -Added specific error check for "clickatell.com" in "httpURL" mode.
//  2013/11/11  Martin D. Flynn
//     -Generalized replacement variable handling in the http-based SMS gateway.
//  2014/03/03  Martin D. Flynn
//     -Fixed "EncodeUrlReplacementVars" to allow "smsPhone" to override SIM 
//      phone number specified in the "device" record [2.5.4-B14]
//  2014/11/30  Martin D. Flynn
//     -Added support for external custom implementation.
//  2015/05/03  Martin D. Flynn
//     -Added support for "IsIgnoredSmsNumber" during "sendSmsMessage(..)"
//     -Completed support for Twilio (GTSE version only)
//  2016/12/21  Martin D. Flynn
//     -Renamed "EncodeUrlReplacementVars" to "ApplyStringReplacementVars"
//     -Added "emailBody" property PROP_emailBody_smsEmailSubject
//     -Added "emailBody" property PROP_emailBody_messageTerminator
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

/**
*** Outbound SMS gateway handler
**/
public abstract class SMSOutboundGateway
{

    // ------------------------------------------------------------------------

    /* ignored SMS file */
    public  static final String PROP_SmsGatewayHandler_             = "SmsGatewayHandler.";
    public  static final String PROP_ignoredSms_file                = PROP_SmsGatewayHandler_ + "ignoredSms.file";

    /* general SMSGatewayHandler properties */
    // -- "SmsGatewayHandler.defaultName" changed to "SmsGatewayHandler.gatewayName"
    public  static final String PROP_gatewayName                    = PROP_SmsGatewayHandler_ + SMSProperties._PROP_gatewayName;

    /* httpURL (Clickatell, CDYNE, Kannel, etc) */
    public  static final String GW_httpURL                          = "httpURL";
    public  static final String PROP_httpURL_url                    = PROP_SmsGatewayHandler_ + "httpURL.url";
    public  static final String GW_httpURL_props[] = {
        PROP_httpURL_url
    };

    /* Twilio (GTSE only, requires Twilio implementation) */
    public  static final String GW_twilio                           = "twilio";
    public  static final String PROP_twilio_className               = PROP_SmsGatewayHandler_ + "twilio.className";
    public  static final String PROP_twilio_accountSID              = PROP_SmsGatewayHandler_ + "twilio.accountSID";
    public  static final String PROP_twilio_authToken               = PROP_SmsGatewayHandler_ + "twilio.authToken";
    public  static final String PROP_twilio_fromPhoneNumber         = PROP_SmsGatewayHandler_ + "twilio.fromPhoneNumber";
    public  static final String GW_twilio_props[] = {
        PROP_twilio_className,
        PROP_twilio_accountSID,
        PROP_twilio_authToken,
        PROP_twilio_fromPhoneNumber
    };

    /* ClickATell (EMail - obsolete) */
    public  static final String GW_clickatell                       = "clickatell";
    public  static final String PROP_clickatell_smsEmailAddress     = PROP_SmsGatewayHandler_ + "clickatell.smsEmailAddress";
    public  static final String PROP_clickatell_user                = PROP_SmsGatewayHandler_ + "clickatell.user";
    public  static final String PROP_clickatell_password            = PROP_SmsGatewayHandler_ + "clickatell.password";
    public  static final String PROP_clickatell_api_id              = PROP_SmsGatewayHandler_ + "clickatell.api_id";

    /* ozekisms */
    public  static final String GW_ozekisms                         = "ozekisms";
    public  static final String PROP_ozekisms_hostPort              = PROP_SmsGatewayHandler_ + "ozekisms.hostPort";
    public  static final String PROP_ozekisms_originator            = PROP_SmsGatewayHandler_ + "ozekisms.originator";
    public  static final String PROP_ozekisms_user                  = PROP_SmsGatewayHandler_ + "ozekisms.user";
    public  static final String PROP_ozekisms_password              = PROP_SmsGatewayHandler_ + "ozekisms.password";

    /* Aeris (requires Aeris implementation) */
    public  static final String GW_aeris                            = "aeris";
    public  static final String PROP_aeris_className                = PROP_SmsGatewayHandler_ + "aeris.className";

    /* emailBody */
    public  static final String GW_emailBody                        = "emailBody";
    public  static final String PROP_emailBody_smsEmailAddress      = PROP_SmsGatewayHandler_ + "emailBody.smsEmailAddress";
    public  static final String PROP_emailBody_smsEmailSubject      = PROP_SmsGatewayHandler_ + "emailBody.smsEmailSubject";
    public  static final String PROP_emailBody_messageTerminator    = PROP_SmsGatewayHandler_ + "emailBody.messageLength";

    /* emailSubject */
    public  static final String GW_emailSubject                     = "emailSubject";
    public  static final String PROP_emailSubject_smsEmailAddress   = PROP_SmsGatewayHandler_ + "emailSubject.smsEmailAddress";

    /* TextAnywhere: mail2txt, mail2txt160, mail2txtid, mail2txt160id [deprecated - do not use] */
    public  static final String GW_mail2txt                         = "mail2txt";
    public  static final String PROP_mail2txt_smsEmailAddress       = PROP_SmsGatewayHandler_ + "mail2txt.smsEmailAddress";
    // --
    public  static final String GW_mail2txt160                      = "mail2txt160";
    public  static final String PROP_mail2txt160_smsEmailAddress    = PROP_SmsGatewayHandler_ + "mail2txt160.smsEmailAddress";
    // --
    public  static final String GW_mail2txtid                       = "mail2txtid";
    public  static final String PROP_mail2txtid_smsEmailAddress     = PROP_SmsGatewayHandler_ + "mail2txtid.smsEmailAddress";
    public  static final String PROP_mail2txtid_from                = PROP_SmsGatewayHandler_ + "mail2txtid.from";
    // --
    public  static final String GW_mail2txt160id                    = "mail2txt160id";
    public  static final String PROP_mail2txt160id_smsEmailAddress  = PROP_SmsGatewayHandler_ + "mail2txt160id.smsEmailAddress";
    public  static final String PROP_mail2txt160id_from             = PROP_SmsGatewayHandler_ + "mail2txt160id.from";

    /* Custom (requires custom implementation) */
    public  static final String GW_custom                           = "custom";
    public  static final String PROP_custom_className               = PROP_SmsGatewayHandler_ + "custom.className";

    /* list of fully supported gateway names */
    public  static final Map<String,String[]> gatewayMap = new HashMap<String,String[]>();
    static {
        gatewayMap.put(GW_httpURL, GW_httpURL_props);
        gatewayMap.put(GW_twilio , GW_twilio_props);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Object      IgnSmsLock           = new Object();
    private static boolean     IgnSmsInit           = false;
    private static File        IgnSmsFile           = null;
    private static long        IgnSmsModTime        = 0L;
    private static Set<String> IgnSmsSet            = null;
    private static boolean     IgnSmsDebug          = false;

    private static boolean IsIgnoredSmsNumber(String e)
    {

        /* blank SMS number? */
        if (StringTools.isBlank(e)) {
            // -- ignore blank SMS numbers
            return true;
        }

        /* validate SMS number? */
        // TODO:

        /* init/reload file */
        synchronized (IgnSmsLock) {

            // -- initialize
            if (!IgnSmsInit) {
                File ignFile = RTConfig.getFile(PROP_ignoredSms_file, null);
                if ((ignFile != null) && !ignFile.isAbsolute()) {
                    File dir = RTConfig.getLoadedConfigDir();
                    ignFile = (dir != null)? new File(dir,ignFile.toString()) : null;
                }
                IgnSmsFile    = ignFile; // may be null
                IgnSmsModTime = 0L;
                IgnSmsSet     = null;
                IgnSmsInit    = true;
                if (IgnSmsFile != null) {
                    Print.logInfo("Init IgnoredSMS file: " + IgnSmsFile);
                }
            }

            // -- reload
            if ((IgnSmsFile == null) || !IgnSmsFile.isFile()) {
                // -- file not found
                if (IgnSmsSet != null) {
                    Print.logWarn("IgnoredSMS file no longer exists: " + IgnSmsFile);
                    IgnSmsModTime = 0L;
                    IgnSmsSet     = null;
                }
            } else {
                // -- file found, check last modified time
                long lastMod = IgnSmsFile.lastModified();
                if (lastMod == 0L) { 
                    if (IgnSmsModTime != 0L) {
                        Print.logWarn("No IgnoredSMS file last modified time: " + IgnSmsFile);
                    }
                    IgnSmsModTime = 0L;
                    IgnSmsSet     = null;
                } else
                if (lastMod > IgnSmsModTime) {
                    Print.logInfo("(Re)Loading IgnoredSMS file: " + IgnSmsFile);
                    Set<String> ignSet = new HashSet<String>();
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(IgnSmsFile);
                        for (int r = 0;; r++) {
                            // -- read line
                            String line = null;
                            try {
                                line = FileTools.readLine(fis);
                                if (line == null) { break; } // end of file
                                line = line.trim();
                            } catch (EOFException eof) {
                                break; // end of file
                            }
                            // -- simple validation
                            if (StringTools.isBlank(line)) {
                                continue;
                            } else
                            if (line.startsWith("#")) {
                                continue; // comment
                            }
                            // -- add to set
                            if (IgnSmsDebug) { Print.logInfo("Adding IgnoredSMS Addr: " + line); }
                            ignSet.add(line);
                        }
                    } catch (IOException ioe) {
                        Print.logException("IgnoredSMS file IO Error", ioe);
                    } finally {
                        if (fis != null) { try { fis.close(); } catch (Throwable th) {} }
                    }
                    // -- save
                    IgnSmsSet     = !ListTools.isEmpty(ignSet)? ignSet : null;
                    IgnSmsModTime = lastMod;
                }
            }

        } // synchronized (IgnSmsLock)

        /* empty ignore list */
        if (ListTools.isEmpty(IgnSmsSet)) {
            // -- nothing is ignored
            return false;
        }

        /* ignore? */
        if (IgnSmsSet.contains(e)) {
            // -- ignore
            return true;
        } else {
            // -- not ignored
            return false;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String SMS_Prefix = "SMS:";
    
    /**
    *** Return true if string starts with "SMS:"
    *** @param val  The string to test
    *** @return True is string starts with "SMS:", false otherwise
    **/
    public static boolean StartsWithSMS(String val)
    {
        return StringTools.startsWithIgnoreCase(val,SMS_Prefix);
    }

    /**
    *** Removes any prefixing "SMS:" from the specified string.
    *** @param val  The string from which the prefixing "SMS:" is removed.
    *** @return The specified string, sans the prefixing "SMS:"
    **/
    public static String RemovePrefixSMS(String val)
    {
        val = StringTools.trim(val);
        if (SMSOutboundGateway.StartsWithSMS(val)) { // remove prefix
            return val.substring(SMSOutboundGateway.SMS_Prefix.length()).trim();
        } else {
            return val; // leave as-is
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Truncates the specified message to the specified length
    **/
    public static String TruncateMessageToLength(String message, int length)
    {

        /* return empty string if null */
        if (message == null) {
            return "";
        }

        /* trim */
        message = message.trim();

        /* check minimum length */
        if (length <= 0) {
            // -- length not specified, return as-is
            return message;
        } else
        if (message.length() > length) {
            // -- truncate
            Print.logWarn("Truncating SMS text ["+length+"]: " + StringTools.length(message));
            return message.substring(0,length).trim();
        } else {
            // -- return as-is
            return message;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if device is authorized to send SMS messages
    **/
    public static boolean IsDeviceAuthorized(Device device)
    {

        /* invalid device */
        if (device == null) {
            return false;
        }

        /* invalid account */
        Account account = device.getAccount();
        if (account == null) {
            return false;
        }

        /* SMS enabled? */
        if (!account.getSmsEnabled()) {
            return false;
        }

        /* authorized */
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    //  %{mobile}   - replaced with the Device "SIM Phone Number" value
    //  %{1mobile}  - replaced with the Device "SIM Phone Number" value (checks for "1" prefix)
    //  %{sender}   - SmsRTProperties: "sender="
    //  %{dataKey}  - replaced with the Device "Data Key" value
    //  %{uniqueID} - replaced with the Device "Unique-ID"
    //  %{modemID}  - replaced with the Device Modem-ID (ie. Unique-ID with prefix removed)
    //  %{imei}     - replaced with the Device "IMEI #" value
    //  %{serial}   - replaced with the Device "Serial Number" value
    //  %{user}     - SmsRTProperties: "user="
    //  %{password} - SmsRTProperties: "password="
    //  %{authID}   - SmsRTProperties: "authID="
    //  %{message}  - replaced with the SMS text to send to the device
    public  static final String REPL_mobile[]   = new String[] { "%{mobile}"  , "{MOBILE}"  , "${mobile}"   };
    public  static final String REPL_1mobile[]  = new String[] { "%{1mobile}" , "{1MOBILE}" , "${1mobile}"  };
    public  static final String REPL_sender[]   = new String[] { "%{sender}"  , "{SENDER}"  , "${sender}"   };
    public  static final String REPL_dataKey[]  = new String[] { "%{dataKey}" , "{DATAKEY}" , "${dataKey}"  };
    public  static final String REPL_uniqueID[] = new String[] { "%{uniqueID}", "{UNIQUEID}", "${uniqueID}" };
    public  static final String REPL_modemID[]  = new String[] { "%{modemID}" , "{MODEMID}" , "${modemID}"  };
    public  static final String REPL_simID[]    = new String[] { "%{simID}"   , "{SIMID}"   , "${simID}"    };
    public  static final String REPL_imei[]     = new String[] { "%{imei}"    , "{IMEI}"    , "${imei}"     };
    public  static final String REPL_serial[]   = new String[] { "%{serial}"  , "{SERIAL}"  , "${serial}"   };
    public  static final String REPL_user[]     = new String[] { "%{user}"    , "{USER}"    , "${user}"     };
    public  static final String REPL_password[] = new String[] { "%{password}", "{PASSWORD}", "${password}" };
    public  static final String REPL_authID[]   = new String[] { "%{authID}"  , "{AUTHID}"  , "${authID}"   };
    public  static final String REPL_message[]  = new String[] { "%{message}" , "{MESSAGE}" , "${message}"  };

    /**
    *** Replace specified variables with replacement text
    *** @param s  Target String
    *** @param r  Replacement variable names
    *** @param m  Replacement text
    *** @return The new string with variables replaced with specified text
    **/
    public static String REPLACE(String s, String r[], String m)
    {
        for (int i = 0; i < r.length; i++) {
            s = StringTools.replace(s, r[i], m);
        }
        return s;
    }

    /**
    *** Replace variables in the specified string with the specified property values.
    *** @param s   Target String
    *** @param rtp Replacement property/value list
    *** @return The new string with variables replaced with specified text
    **/
    public static String REPLACE(String s, RTProperties rtp)
    {
        return StringTools.insertKeyValues(s, "%{", "}", "=", rtp);
    }

    // ------------------------------------------------------------------------

    /**
    *** Add properties to replacement variable list
    **/
    private static void AddEncodedRtpArgs(RTProperties RTP, RTProperties otherRTP, boolean urlEncode)
    {
        if (otherRTP != null) {
            Set<?> keys = otherRTP.getPropertyKeys();
            for (Object K : keys) {
                Object V = otherRTP.getProperty(K,null);
                if ((K instanceof String) && (V instanceof String) && !StringTools.isBlank((String)V)) {
                    String key = (String)K;
                    String val = (String)V;
                    SMSOutboundGateway.AddEncodedRtpArg(RTP, key, val, urlEncode);
                }
            }
        }
    }

    /**
    *** Add a key/value to the replacement variable list
    **/
    private static void AddEncodedRtpArg(RTProperties RTP, String key, String val, boolean urlEncode)
    {
        if (!StringTools.isBlank(val)) {
            RTP.setString(key, (urlEncode? URIArg.encodeArg(val) : val));
        }
    }

    /**
    *** Encode the specified target String with the standard replacement variables
    **/
    private static String ApplyStringReplacementVars(
        Account account, 
        Device  device, String smsPhone,
        String  targetString,
        String  messageStr,
        boolean urlEncode)
    {
        RTProperties repRTP = new RTProperties();

        /* no Account? */
        if ((account == null) && (device != null)) {
            account = device.getAccount();
        }

        /* Account-based replacement vars */
        SMSProperties smsProps = SMSProperties.getSmsProperties(account);
        // -- extract specific properties (for "REPLACE" below)
        String sender   = smsProps.getProperty("sender"  ,((account!=null)?account.getContactPhone():""));
        String user     = smsProps.getProperty("user"    ,""); // SMS authorization
        String password = smsProps.getProperty("password","");
        String authID   = smsProps.getProperty("authID"  ,"");
        int    maxLen   = smsProps.getMaximumMessageLength();
        // -- add default replacement vars
        AddEncodedRtpArg(repRTP, "sender"  , sender  , urlEncode);
        AddEncodedRtpArg(repRTP, "user"    , user    , urlEncode);
        AddEncodedRtpArg(repRTP, "password", password, urlEncode);
        AddEncodedRtpArg(repRTP, "authID"  , authID  , urlEncode);
        // -- add all properties contained in 'smsProps'
        AddEncodedRtpArgs(repRTP, smsProps.getProperties(), urlEncode);

        /* SIM phone number [v2.5.4-B14] */
        String devSPN   = (device != null)? device.getSimPhoneNumber() : "";
        String mobile   = !StringTools.isBlank(smsPhone)? StringTools.trim(smsPhone) : devSPN;
        String mobile_1 = ((mobile==null)||mobile.startsWith("1")||(mobile.length()!=10))? mobile : ("1"+mobile);
        AddEncodedRtpArg(repRTP, "mobile"  , mobile  , urlEncode);
        AddEncodedRtpArg(repRTP, "1mobile" , mobile_1, urlEncode); // "1" + areaCode(3) + prefix(3) + suffix(4)

        /* Device-based replacement values */
        String dataKey  = (device != null)? device.getDataKey()      : null;
        String uniqueID = (device != null)? device.getUniqueID()     : null;
        String modemID  = (device != null)? device.getModemID()      : null;
        String simID    = (device != null)? device.getSimID()        : null;
        String imeiNum  = (device != null)? device.getImeiNumber()   : null;
        String serial   = (device != null)? device.getSerialNumber() : null;
        AddEncodedRtpArg(repRTP, "dataKey" , dataKey , urlEncode);
        AddEncodedRtpArg(repRTP, "uniqueID", uniqueID, urlEncode);
        AddEncodedRtpArg(repRTP, "modemID" , modemID , urlEncode);
        AddEncodedRtpArg(repRTP, "simID"   , simID   , urlEncode);
        AddEncodedRtpArg(repRTP, "imei"    , imeiNum , urlEncode);
        AddEncodedRtpArg(repRTP, "serial"  , serial  , urlEncode);

        /* encode/truncate message */
        String message = StringTools.trim(messageStr); // trim whitespace
        message = StringTools.insertKeyValues(message, "%{", "}", "=", repRTP);
        message = SMSOutboundGateway.TruncateMessageToLength(message, maxLen);
        AddEncodedRtpArg(repRTP, "message" , message , urlEncode);

        /* apply replacement vars to URL */
        targetString = StringTools.insertKeyValues(targetString, "%{", "}", "=", repRTP);

        /* OBSOLETE: set URL replacement vars */
        // only necessary if the obsolete non-%{VAR} format has been used
        targetString = REPLACE(targetString, REPL_mobile  , (urlEncode?URIArg.encodeArg(mobile  ):mobile  )); // SIM phone number
        targetString = REPLACE(targetString, REPL_1mobile , (urlEncode?URIArg.encodeArg(mobile_1):mobile_1)); // 1 + SIM phone number
        targetString = REPLACE(targetString, REPL_message , (urlEncode?URIArg.encodeArg(message ):message )); // command string
        targetString = REPLACE(targetString, REPL_sender  , (urlEncode?URIArg.encodeArg(sender  ):sender  )); // Account contact phone number
        targetString = REPLACE(targetString, REPL_user    , (urlEncode?URIArg.encodeArg(user    ):user    )); // SMS auth user
        targetString = REPLACE(targetString, REPL_password, (urlEncode?URIArg.encodeArg(password):password)); // SMS auth password
        targetString = REPLACE(targetString, REPL_authID  , (urlEncode?URIArg.encodeArg(authID  ):authID  )); // SMS auth ID
        targetString = REPLACE(targetString, REPL_dataKey , (urlEncode?URIArg.encodeArg(dataKey ):dataKey )); // Device data key
        targetString = REPLACE(targetString, REPL_uniqueID, (urlEncode?URIArg.encodeArg(uniqueID):uniqueID)); // Device unique ID
        targetString = REPLACE(targetString, REPL_modemID , (urlEncode?URIArg.encodeArg(modemID ):modemID )); // Device modem ID
        targetString = REPLACE(targetString, REPL_simID   , (urlEncode?URIArg.encodeArg(simID   ):simID   )); // Device SIM ID
        targetString = REPLACE(targetString, REPL_imei    , (urlEncode?URIArg.encodeArg(imeiNum ):imeiNum )); // Device IMEI number
        targetString = REPLACE(targetString, REPL_serial  , (urlEncode?URIArg.encodeArg(serial  ):serial  )); // Device serial number

        /* return URL */
        return targetString;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Map<String,SMSOutboundGateway> SmsGatewayHandlerMap = null;

    /**
    *** Add SMS Gateway support provider
    **/
    public static void AddSMSGateway(String name, SMSOutboundGateway smsGW)
    {

        /* validate name */
        if (StringTools.isBlank(name)) {
            Print.logWarn("SMS Gateway name is blank");
            return;
        } else
        if (smsGW == null) {
            Print.logWarn("SMS Gateway handler is null");
            return;
        }
        smsGW.setName(name);

        /* initialize map? */
        if (SmsGatewayHandlerMap == null) { 
            SmsGatewayHandlerMap = new HashMap<String,SMSOutboundGateway>(); 
        }

        /* save handler */
        SmsGatewayHandlerMap.put(name.toLowerCase(), smsGW);
        Print.logDebug("Added SMS Gateway Handler: " + name);

    }

    /**
    *** Gets the SMSoutboubdGateway for the specified name
    **/
    public static SMSOutboundGateway GetSMSGateway(String name)
    {

        /* get handler */
        if (StringTools.isBlank(name)) {
            return null;
        } else {
            if (name.equalsIgnoreCase("body")) { 
                name = GW_emailBody; 
            } else
            if (name.equalsIgnoreCase("subject")) { 
                name = GW_emailSubject; 
            }
            return SmsGatewayHandlerMap.get(name.toLowerCase());
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Initialize outbound SMS gateway handlers
    **/
    public static void _startupInit()
    {
        Print.logDebug("SMSOutboundGateway initializing ...");

        /* already initialized? */
        if (SmsGatewayHandlerMap != null) {
            return;
        }

        // -----------------------------------------------
        // The following shows several example of outbound SMS gateway support.
        // The only method that needs to be overridden and implemented is
        //   public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr)
        // The "device" is the Device record instance to which the SMS message should be sent,
        // and "commandStr" is the SMS text (device command) which is to be sent to the device.
        // -----------------------------------------------

        /* EMail: standard "Body" command */
        // Property:
        //   SmsGatewayHandler.gatewayName=emailBody
        //   SmsGatewayHandler.emailBody.smsEmailAddress=@sms.example.com
        //   SmsGatewayHandler.emailBody.maxMessageLength=160
        //   SmsGatewayHandler.emailBody.smsEmailSubject=password=%{password} sender=%{sender}
        //   SmsGatewayHandler.emailBody.messageTerminator=END
        // Notes:
        //   This outbound SMS method sends the SMS text in an email message body to the device
        //   "smsEmailAddress".  If the device "smsEmailAddress" is blank, then the "To" email
        //   address is constructed from the device "simPhoneNumber" and the email address
        //   specified on the property "SmsGatewayHandler.emailBody.smsEmailAddress".
        SMSOutboundGateway.AddSMSGateway(GW_emailBody, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!IsDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                // -- "From" email address
                String frEmail = this.getFromEmailAddress(device);
                // -- "To" email address : "%{mobile}@sms.example.com", "@sms.example.com"
                String toEmail = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    String smsPhone = device.getSimPhoneNumber();
                    String smsEmail = this.getSmsProperty(device,PROP_emailBody_smsEmailAddress,"");
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                }
                // -- message body
                int    maxLen  = this.getMaximumMessageLength(device);
                String message = SMSOutboundGateway.TruncateMessageToLength(commandStr, maxLen);
                // -- subject
                String subject = this.getSmsProperty(device,PROP_emailBody_smsEmailSubject,"");
                if (!StringTools.isBlank(subject)) {
                    // -- subject may require special control vars
                    // -  IE: "password=%{password} sender=%{sender} flash=yes dlr=yes forwarddlr=no country=55"
                    subject = SMSOutboundGateway.ApplyStringReplacementVars( // GW_emailBody (command)
                        null/*account*/, device, device.getSimPhoneNumber(), 
                        subject, 
                        message, 
                        false/*urlEncode*/);
                }
                // -- message terminator (applied after string replacement)
                String msgTerm = this.getSmsProperty(device,PROP_emailBody_messageTerminator,null);
                if (!StringTools.isBlank(msgTerm)) {
                    if (!message.endsWith("\n")) { message += "\n"; }
                    message += msgTerm + "\n"; // ie "END\n"
                }
                // -- send as email
                return this.sendEmail(frEmail, toEmail, subject, message);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, Device device, String smsMessage, String smsPhone) { // GW_emailBody
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                if (StringTools.isBlank(smsPhone)) { return DCServerFactory.ResultCode.INVALID_SMS; }
                if (IsIgnoredSmsNumber(smsPhone))  { return DCServerFactory.ResultCode.IGNORED_SMS; }
                // -- "From" email address
                String frEmail  = this.getFromEmailAddress(account);
                // -- "To" email address : "%{mobile}@sms.example.com", "@sms.example.com"
                String smsEmail = this.getSmsProperty(account,PROP_emailBody_smsEmailAddress,"");
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                // -- message
                int    maxLen   = this.getMaximumMessageLength(account);
                String message  = SMSOutboundGateway.TruncateMessageToLength(smsMessage, maxLen);
                // -- subject
                String subject  = this.getSmsProperty(account,PROP_emailBody_smsEmailSubject,"");
                if (!StringTools.isBlank(subject)) {
                    subject = SMSOutboundGateway.ApplyStringReplacementVars( // GW_emailBody (message)
                        account, device, smsPhone, 
                        subject, 
                        message, 
                        false/*urlEncode*/);
                }
                // -- message terminator (applied after string replacement)
                String msgTerm = this.getSmsProperty(account,PROP_emailBody_messageTerminator,null);
                if (!StringTools.isBlank(msgTerm)) {
                    if (!message.endsWith("\n")) { message += "\n"; }
                    message += msgTerm + "\n";
                }
                // -- send as email
                return this.sendEmail(frEmail, toEmail, subject, message);
            }
        });

        /* EMail: standard "Subject" command */
        // Property:
        //   SmsGatewayHandler.gatewayName=emailSubject
        //   SmsGatewayHandler.emailSubject.smsEmailAddress=@sms.example.com
        //   SmsGatewayHandler.emailSubject.maxMessageLength=160
        // Notes:
        //   This outbound SMS method sends the SMS text in an email message subject to the device
        //   "smsEmailAddress".  If the device "smsEmailAddress" is blank, then the "To" email
        //   address is constructed from the device "simPhoneNumber" and the email address
        //   specified on the property "SmsGatewayHandler.emailSubject.smsEmailAddress".
        SMSOutboundGateway.AddSMSGateway(GW_emailSubject, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!IsDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                // ---
                String frEmail = this.getFromEmailAddress(device);
                String toEmail = this.getSmsEmailAddress(device);
                if (StringTools.isBlank(toEmail)) {
                    String smsPhone = device.getSimPhoneNumber();
                    String smsEmail = this.getSmsProperty(device,PROP_emailSubject_smsEmailAddress,"");
                    toEmail = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                }
                // ---
                int    maxLen  = this.getMaximumMessageLength(device);
                String message = SMSOutboundGateway.TruncateMessageToLength(commandStr, maxLen);
                // ---
                return this.sendEmail(frEmail, toEmail, message, ""/*body*/);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, Device device, String smsMessage, String smsPhone) { // GW_emailSubject
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                if (StringTools.isBlank(smsPhone)) { return DCServerFactory.ResultCode.INVALID_SMS; }
                if (IsIgnoredSmsNumber(smsPhone)) { return DCServerFactory.ResultCode.IGNORED_SMS; }
                // ---
                String frEmail  = this.getFromEmailAddress(account);
                String smsEmail = this.getSmsProperty(account,PROP_emailSubject_smsEmailAddress,"");
                String toEmail  = smsEmail.startsWith("@")? (smsPhone + smsEmail) : REPLACE(smsEmail, REPL_mobile, smsPhone);
                // ---
                int    maxLen   = this.getMaximumMessageLength(account);
                String message  = SMSOutboundGateway.TruncateMessageToLength(smsMessage, maxLen);
                // ---
                return this.sendEmail(frEmail, toEmail, message, ""/*body*/);
            }
        });

        /* HTTP: URL */
        // Property:
        //   SmsGatewayHandler.httpURL.url=http://localhost:12345/smsredirector/sendsms?flash=0&acctuser=user&tracking_Pwd=pass&source=5551212&destination=${mobile}&message=${message}
        //   SmsGatewayHandler.httpURL.url=http://localhost:12345/sendsms?user=%{user=pass}&pass=%{password=pass}&source=%{sender}&dest=${mobile}&text=${message}
        // Notes:
        //   - This outbound SMS method sends the SMS text in an HTTP "GET" request to the URL 
        //     specified on the property "SmsGatewayHandler.httpURL.url".  The following replacement
        //     variables may be specified in the URL string:
        //       %{sender}  - replaced with the Account "contactPhone" field contents
        //       %{mobile}  - replaced with the Device "simPhoneNumber" field contents
        //       %{1mobile} - replaced with the Device "simPhoneNumber" field contents and make sure number is prefixed with "1"
        //       %{message} - replaced with the SMS text/command to be sent to the device.
        //     It is expected that the server handling the request understands how to parse and
        //     interpret the various fields in the URL.
        //   - The "httpURL" outbound SMS gateway handler will also work for Clickatell http-mode.
        //     The repsonse will normally be an acknoeledgement ID, however an error response may
        //     look like the following:  "ERR: 113, Max message parts exceeded"
        SMSOutboundGateway.AddSMSGateway(GW_httpURL, new SMSOutboundGateway() {
            public DCServerFactory.ResultCode sendSMSCommand(Device device, String commandStr) {
                if (device == null) { return DCServerFactory.ResultCode.INVALID_DEVICE; }
                if (!IsDeviceAuthorized(device)) { return DCServerFactory.ResultCode.NOT_AUTHORIZED; }
                // -- create URL
                String httpURL = this.getSmsProperty(device, PROP_httpURL_url, "");
                if (StringTools.isBlank(httpURL)) {
                    Print.logWarn("'"+PROP_httpURL_url+"' not specified");
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                int maxLen = this.getMaximumMessageLength(device);
                httpURL = SMSOutboundGateway.ApplyStringReplacementVars( // GW_httpURL (command)
                    null/*account*/, device, null/*smsPhone*/, 
                    httpURL, 
                    commandStr,
                    true/*urlEncode*/);
                // -- send SMS
                return this._sendSMS(httpURL);
            }
            public DCServerFactory.ResultCode sendSMSMessage(Account account, Device device, String smsMessage, String smsPhone) { // GW_httpURL
                if (account == null) { return DCServerFactory.ResultCode.INVALID_ACCOUNT; }
                if (StringTools.isBlank(smsPhone)) { return DCServerFactory.ResultCode.INVALID_SMS; }
                if (IsIgnoredSmsNumber(smsPhone)) { return DCServerFactory.ResultCode.IGNORED_SMS; }
                // -- create URL
                String httpURL = this.getSmsProperty(account,PROP_httpURL_url,"");
                if (StringTools.isBlank(httpURL)) {
                    Print.logWarn("'"+PROP_httpURL_url+"' not specified");
                    return DCServerFactory.ResultCode.INVALID_SMS;
                }
                int maxLen = this.getMaximumMessageLength(account);
                String smsURL = SMSOutboundGateway.ApplyStringReplacementVars( // GW_httpURL (message)
                    account, device, smsPhone, 
                    httpURL, 
                    smsMessage,
                    true/*urlEncode*/);
                // -- send SMS
                return this._sendSMS(smsURL);
            }
            private DCServerFactory.ResultCode _sendSMS(String httpURL) {
                try {
                    Print.logInfo("SMS Gateway URL: " + httpURL);
                    //Print.logError("SMS Gateway URL: " + httpURL);
                    byte response[] = HTMLTools.readPage_GET(
                        new URL(httpURL), 
                        10000, -1);
                    String resp = StringTools.toStringValue(response);
                    // -- log response
                    int maxRespLen = 120;
                    if (resp.length() <= maxRespLen) {
                        Print.logInfo("SMS Gateway response (httpURL): " + resp);
                    } else {
                        String R = resp.substring(0, maxRespLen);
                        Print.logInfo("SMS Gateway response (httpURL): " + R + " ..."); // partial
                    }
                    // -- handle response
                    if (httpURL.indexOf("clickatell.com") > 0) {
                        // Clickatell:
                        //  - Success: ID: 3b912995fb87962fea42596099a3ecbb
                        //  - Failed : ERR: 001, Authentication failed
                        //  - Failed : ERR: 113, Max message parts exceeded
                        //  - Failed : ERR: 121, Destination mobile number blocked
                        if (resp.startsWith("ID:")) {
                            // -- Clickatell indicated success
                            return DCServerFactory.ResultCode.SUCCESS_SMS;
                        } else
                        if (resp.indexOf("Authentication") > 0) {
                            // -- Clickatell indicated failure: invalid authentication
                            Print.logError("SMS Gateway 'clickatell' Authentication Failure");
                            return DCServerFactory.ResultCode.GATEWAY_AUTH;
                        } else {
                            // -- otherwise assume error
                            Print.logError("SMS Gateway 'clickatell' General Error");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        }
                    } else
                    if (httpURL.indexOf("cdyne.com") > 0) {
                        // -- CDYNE:
                        String R = resp.toUpperCase();
                        if (R.indexOf("NOERROR") >= 0) { // "NoError"
                            // -- CDYNE indicated success (note: the text "error" should never be included in a response if it was successful!!)
                            // -   falsecaf2a7e7-8e1c-4cdc-c37f-42a7d967ac64trueNoErrorfalse0001-01-01T00:00:00
                            return DCServerFactory.ResultCode.SUCCESS_SMS;
                        } else
                        if (R.indexOf("LICENSEKEYINVALID") >= 0) { // "LicenseKeyInvalid"
                            // -- CDYNE indicated failure (unauthorized key)
                            // -   falseb02d8bc1-01c4-4e51-9ca1-6d7a7d9c0a59falseLicenseKeyInvalidfalse0001-01-01T00:00:00
                            Print.logError("SMS Gateway 'CDYNE' Authentication Failure");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else
                        if (R.indexOf("PHONENUMBERINVALID") >= 0) { // "PhoneNumberInvalid"
                            // -- CDYNE indicated failure (invalid phone number)
                            // -   false00000000-0000-0000-0000-000000000000falsePhoneNumberInvalidfalse0001-01-01T00:00:00
                            Print.logError("SMS Gateway 'CDYNE' Invalid Phone Number");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else
                        if (R.indexOf("INVALID") >= 0) { // generic "Invalid"
                            // -- CDYNE indicated failure (other "Invalid")
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else
                        if (R.indexOf("REQUEST ERROR") >= 0) { // general "Request Error"
                            // -- CDYNE indicated failure (invalid key length)
                            // -   <?xml version="1.0" encoding="utf-8"?>
                            // -   <!DOCTYPE ...>
                            // -   <html xmlns="http://www.w3.org/1999/xhtml">
                            // -     <head>
                            // -       <title>Request Error</title>
                            // -       ...
                            // -     </head>
                            // -     <body>
                            // -       <div id="content">
                            // -         <p class="heading1">Request Error</p>
                            // -         <p xmlns="">The server encountered an error processing the request. ...</p>
                            // -         ...
                            // -       </div>
                            // -     </body>
                            // -   </html>
                            Print.logError("SMS Gateway 'CDYNE' request format error");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else
                        if ((R.length() >= 45) && R.substring(41,45).equalsIgnoreCase("TRUE")) {
                            // -- assume "true" in columns 41:4 means success
                            // -    falsecaf2a7e7-8e2c-4cbc-c38f-42a6d467ac64trueXXXXXXXX
                            // -    0----+----1----+----2----+----3----+----4----+...
                            Print.logError("SMS Gateway 'CDYNE' assuming success");
                            return DCServerFactory.ResultCode.SUCCESS_SMS;
                        } else {
                            // -- otherwise assume error
                            Print.logError("SMS Gateway 'CDYNE' General Error");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        }
                    } else
                    if (httpURL.indexOf("octopush-dm.com") > 0) {
                        // -- OctoPush:
                        String R = resp.toUpperCase();
                        if (R.indexOf("<ERROR_CODE>000</ERROR_CODE>") >= 0) { // success
                            return DCServerFactory.ResultCode.SUCCESS_SMS;
                        } else {
                            Print.logError("SMS Gateway 'OctoPush' General Error");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        }
                    } else
                    if (httpURL.indexOf("txtlocal.com") > 0) {
                        // -- TextLocal [http://api.txtlocal.com/docs/sendsms]
                        // -    ==> JSON: "status":"success"
                        // -    ==> JSON: "status":"failure"
                        String R = resp.toUpperCase();
                        if (R.indexOf("FAILURE") >= 0) {
                            // -- response contains "FAILURE"
                            Print.logError("SMS Gateway 'FAILURE' found");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else
                        if (R.indexOf("ERROR") >= 0) {
                            // -- response contains "ERROR" tag section
                            Print.logError("SMS Gateway 'ERROR' found");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else {
                            // -- otherwise assume success
                            return DCServerFactory.ResultCode.SUCCESS_SMS;
                        }
                    } else {
                        // -- generic SMS service provider
                        String R = resp.toUpperCase();
                        if (R.indexOf("ERROR") >= 0) {
                            // -- response contains "ERROR"
                            Print.logError("SMS Gateway 'ERROR' found");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else
                        if (R.indexOf("FAIL") >= 0) {
                            // -- response contains "FAIL"
                            Print.logError("SMS Gateway 'FAIL' found");
                            return DCServerFactory.ResultCode.GATEWAY_ERROR;
                        } else {
                            // -- otherwise assume success
                            return DCServerFactory.ResultCode.SUCCESS_SMS;
                        }
                    }
                } catch (UnsupportedEncodingException uee) {
                    Print.logError("URL Encoding: " + uee);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                } catch (NoRouteToHostException nrthe) {
                    Print.logError("Unreachable Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (UnknownHostException uhe) {
                    Print.logError("Unknown Host: " + httpURL);
                    return DCServerFactory.ResultCode.UNKNOWN_HOST;
                } catch (FileNotFoundException fnfe) {
                    Print.logError("Invalid URL (not found): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (MalformedURLException mue) {
                    Print.logError("Invalid URL (malformed): " + httpURL);
                    return DCServerFactory.ResultCode.INVALID_SMS;
                } catch (Throwable th) {
                    Print.logError("HTML SMS error: " + th);
                    return DCServerFactory.ResultCode.TRANSMIT_FAIL;
                }
            }
        });

        /* HTTP: Custom */
        {
            String gwName = GW_custom;
            String gwKey  = PROP_custom_className;
            String gwClassName = RTConfig.getString(gwKey,""); // RTConfig ok
            if (!StringTools.isBlank(gwClassName)) {
                try {
                    Class<?> gwClass = Class.forName(gwClassName); // ClassNotFoundException
                    SMSOutboundGateway gwi = (SMSOutboundGateway)gwClass.newInstance(); // ClassCastException
                    SMSOutboundGateway.AddSMSGateway(gwName, gwi);
                } catch (ClassNotFoundException cnfe) {
                    Print.logException("Unable to load '"+gwName+"' SMSOutboundGateway class: " + gwClassName, cnfe);
                } catch (InstantiationException ie) {
                    Print.logException("Unable to instantiate '"+gwName+"' SMSOutboundGateway class: " + gwClassName, ie);
                } catch (ClassCastException cce) {
                    Print.logException("Not a subclass of SMSOutboundGateway: " + gwClassName, cce);
                } catch (Throwable th) {
                    Print.logException("Unable to create '"+gwName+"' SMSOutboundGateway class: " + gwClassName, th);
                }
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public String gwName = "";

    public SMSOutboundGateway()
    {
        // override
    }

    // ------------------------------------------------------------------------

    public void setName(String n)
    {
        this.gwName = StringTools.trim(n);
    }
    
    public String getName()
    {
        return this.gwName;
    }

    public String toString()
    {
        return "SMSGateway: " + this.getName();
    }

    // ------------------------------------------------------------------------

    /**
    *** Send an SMS command to the specified device
    *** @param device  The Device to which the command is sent (must not be null).
    *** @param command The command String to send to the Device.
    *** @return The result code
    **/
    public abstract DCServerFactory.ResultCode sendSMSCommand(Device device, String command);

    /**
    *** Send an SMS message to the specified phone number.
    *** @param account    The Account for which this SMS message is being sent (must not be null)
    *** @param device     The Device for which this SMS message is being sent (may be null)
    *** @param smsMessage The SMS message sent to the destination phone number
    *** @param smsPhone   The destination phone number
    *** @return The result code
    **/
    public abstract DCServerFactory.ResultCode sendSMSMessage(Account account, Device device, String smsMessage, String smsPhone); // abstract

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected String getFromEmailAddress(Device device) 
    {
        if (device == null) { return null; }
        return CommandPacketHandler.getFromEmailCommand(device.getAccount());
    }

    protected String getFromEmailAddress(Account account) 
    {
        if (account == null) { return null; }
        return CommandPacketHandler.getFromEmailCommand(account);
    }
    
    // ------------------------------------------------------------------------

    protected String getSmsEmailAddress(Device device) 
    {
        if (device == null) { return null; }
        String toEmail = device.getSmsEmail();
        return toEmail;
    }

    protected String getSmsPhoneNumber(Device device) 
    {
        if (device == null) { return null; }
        String smsPhone = device.getSimPhoneNumber();
        return smsPhone;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the String value for the specified property key
    *** @param account  The current Account instance
    *** @param key     The specified property key
    *** @param dft     The default value to return if the property key is not found
    *** @return  The String value for the specified property key.
    **/
    protected String getSmsProperty(Account account, String key, String dft) 
    {
        return SMSProperties.getSmsProperties(account).getProperty(key, dft);
    }

    /**
    *** Gets the String value for the specified property key
    *** @param device  The current Device instance
    *** @param key     The specified property key
    *** @param dft     The default value to return if the property key is not found
    *** @return  The String value for the specified property key.
    **/
    protected String getSmsProperty(Device device, String key, String dft) 
    {
        return SMSProperties.getSmsProperties(device).getProperty(key, dft);
    }

    // --------------------------------

    /**
    *** Gets the Maximum message length
    *** @param account  The current Account instance
    *** @return  The Maximum message length
    **/
    protected int getMaximumMessageLength(Account account) 
    {
        return SMSProperties.getSmsProperties(account).getMaximumMessageLength();
    }

    /**
    *** Gets the Maximum message length
    *** @param device  The current Device instance
    *** @return  The Maximum message length
    **/
    protected int getMaximumMessageLength(Device device) 
    {
        return SMSProperties.getSmsProperties(device).getMaximumMessageLength();
    }

    // ------------------------------------------------------------------------

    protected DCServerFactory.ResultCode sendEmail(String frEmail, String toEmail, String subj, String body) 
    {
        if (StringTools.isBlank(frEmail)) {
            Print.logError("'From' SMS Email address not specified");
            return DCServerFactory.ResultCode.INVALID_EMAIL_FR;
        } else
        if (StringTools.isBlank(toEmail) || !CommandPacketHandler.validateAddress(toEmail)) {
            Print.logError("'To' SMS Email address invalid, or not specified");
            return DCServerFactory.ResultCode.INVALID_EMAIL_TO;
        } else
        if (StringTools.isBlank(subj) && StringTools.isBlank(body)) {
            Print.logError("SMS Subject/Body string not specified");
            return DCServerFactory.ResultCode.INVALID_ARG;
        } else {
            try {
                Print.logInfo ("SMS email: From <" + frEmail + ">, To <" + toEmail + ">");
                Print.logDebug("  From   : " + frEmail);
                Print.logDebug("  To     : " + toEmail);
                Print.logDebug("  Subject: " + subj);
                Print.logDebug("  Message: " + body);
                SmtpProperties smtpProps = null; // TODO:
                SendMail.send(frEmail,toEmail,null,null,subj,body,null,smtpProps);
                return DCServerFactory.ResultCode.SUCCESS_SMS;
            } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                // this will fail if JavaMail support for SendMail is not available.
                Print.logWarn("SendMail error: " + t);
                return DCServerFactory.ResultCode.TRANSMIT_FAIL;
            }
        }
    }

    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]   = { "account", "a" };
    private static final String ARG_DEVICE[]    = { "device" , "d" };
    private static final String ARG_PHONE[]     = { "phone"  , "p" };
    private static final String ARG_URL[]       = { "url"    , "u" };
    private static final String ARG_MESSAGE[]   = { "message", "m", "msg" };
    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);
        String accountID = RTConfig.getString(ARG_ACCOUNT,null);
        String deviceID  = RTConfig.getString(ARG_DEVICE ,null);
        String smsPhone  = RTConfig.getString(ARG_PHONE  ,null);
        String httpURL   = RTConfig.getString(ARG_URL    ,"");
        String message   = RTConfig.getString(ARG_MESSAGE,"");

        /* get Account */
        Account account = null;
        if (!StringTools.isBlank(accountID)) {
            try {
                account = Account.getAccount(accountID); // may throw DBException
                if (account == null) {
                    Print.logError("Account-ID does not exist: " + accountID);
                    System.exit(99);
                }
            } catch (DBException dbe) {
                Print.logException("Error loading Account: " + accountID, dbe);
                System.exit(99);
            }
        }

        /* get Device */
        Device device = null;
        if (!StringTools.isBlank(deviceID)) {
            try {
                device = Device.getDevice(account, deviceID); // null if non-existent
                if (device == null) {
                    Print.logError("Device-ID does not exist: " + deviceID);
                    System.exit(99);
                }
            } catch (DBException dbe) {
                Print.logException("Error loading Device: " + deviceID, dbe);
                System.exit(99);
            }
        }

        /* encode */
        if ((account != null) && !StringTools.isBlank(httpURL)) {
            String newURL = SMSOutboundGateway.ApplyStringReplacementVars( // main
                account, device, smsPhone,
                httpURL,
                message,
                true/*urlEncode*/);
            Print.sysPrintln("Encoded URL: " + newURL);
        }

    }
}
