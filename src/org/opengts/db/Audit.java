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
//  2010/04/11  Martin D. Flynn
//     -Initial release
//  2012/09/02  Martin D. Flynn
//     -Added "deviceCommand" audit
//  2015/05/03  Martin D. Flynn
//     -Added "reportEmail" audit.
//     -Added RTConfig property checks for individual audit types.
//     -Renamed "ruleNotification" to "ruleEmailNotification", and added "ruleID"
//     -Added "ruleSmsNotification"
//  2017/06/16  Martin D. Flynn
//     -Changes to SMS message auditing
//     -Added "untilTime" to "AuditHandler.countAuditEntries" interface
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class Audit
{

    // ------------------------------------------------------------------------

    public static final int    GROUP_UNKNOWN            = 0x0000; //    0
    public static final int    GROUP_LOGIN              = 0x0100; //  256
    public static final int    GROUP_EMAIL              = 0x0200; //  512
    public static final int    GROUP_DB                 = 0x0300; //  768
    public static final int    GROUP_SMS                = 0x0400; // 1024
    public static final int    GROUP_DEVCMD             = 0x0600; // 1536

    public static final int    AUDIT_UNKNOWN            = GROUP_UNKNOWN | 0x00; // 0x0000 /    0
    
    public static final int    AUDIT_LOGIN_OK           = GROUP_LOGIN   | 0x00; // 0x0100 /  256
    public static final int    AUDIT_LOGIN_FAILED       = GROUP_LOGIN   | 0x01; // 0x0101 /  257
    public static final int    AUDIT_LOGOUT             = GROUP_LOGIN   | 0x10; // 0x0110 /  272
    
    public static final int    AUDIT_EMAIL_NOTIFY       = GROUP_EMAIL   | 0x01; // 0x0201 /  513
    
    public static final int    AUDIT_SMS_NOTIFY         = GROUP_SMS     | 0x01; // 0x0401 / 1025
    public static final int    AUDIT_SMS_COMMAND        = GROUP_SMS     | 0x02; // 0x0402 / 1026 
  //public static final int    AUDIT_SMS_SENT           = GROUP_SMS     | 0x10; // 0x0410 / 1040 (not currently used)
  //public static final int    AUDIT_SMS_DELIVERED      = GROUP_SMS     | 0x20; // 0x0420 / 1056 (not currently used)
  //public static final int    AUDIT_SMS_ACK            = GROUP_SMS     | 0x30; // 0x0430 / 1072 (not currently used)
    
    public static final int    AUDIT_DEVICE_COMMAND     = GROUP_DEVCMD  | 0x01; // 0x0601 / 1537
    public static final int    AUDIT_DEVICE_COMMAND_ACK = GROUP_DEVCMD  | 0x30; // 0x0630 / 1584
    
  //public static final int    AUDIT_DB_NEW_ACCOUNT     = GROUP_DB      | 0x01; // 0x0301 /  769
  //public static final int    AUDIT_DB_DEL_ACCOUNT     = GROUP_DB      | 0x02; // 0x0302 /  770

    public static String GetAuditAbbrev(int auditCode)
    {
        switch (auditCode) {
            case AUDIT_UNKNOWN            : return "UNKWN";
            case AUDIT_LOGIN_OK           : return "LOGIN";
            case AUDIT_LOGIN_FAILED       : return "LOGFAIL";
            case AUDIT_LOGOUT             : return "LOGOUT";
            case AUDIT_EMAIL_NOTIFY       : return "EMAIL";
            case AUDIT_SMS_NOTIFY         : return "SMSNOTIFY";
            case AUDIT_SMS_COMMAND        : return "SMSCOMMAND";
          //case AUDIT_SMS_SENT           : return "SMSSENT";
          //case AUDIT_SMS_DELIVERED      : return "SMSDELIVER";
          //case AUDIT_SMS_ACK            : return "SMSACK";
            case AUDIT_DEVICE_COMMAND     : return "DEVCMD";
            case AUDIT_DEVICE_COMMAND_ACK : return "DEVCMDACK";
          //case AUDIT_DB_NEW_ACCOUNT     : return "NEWACCT";
          //case AUDIT_DB_DEL_ACCOUNT     : return "DELACCT";
            default                       : return "UNDEF";
        }
    }

    public static String GetAuditName(int auditCode)
    {
        switch (auditCode) {
            case AUDIT_UNKNOWN            : return "Unknown";
            case AUDIT_LOGIN_OK           : return "User Login OK";
            case AUDIT_LOGIN_FAILED       : return "User Login Failed";
            case AUDIT_LOGOUT             : return "User Logout";
            case AUDIT_EMAIL_NOTIFY       : return "Email Notification";
            case AUDIT_SMS_NOTIFY         : return "SMS Notification";
            case AUDIT_SMS_COMMAND        : return "SMS Command";
          //case AUDIT_SMS_SENT           : return "SMS Sent";
          //case AUDIT_SMS_DELIVERED      : return "SMS Delivered";
          //case AUDIT_SMS_ACK            : return "SMS ACK";
            case AUDIT_DEVICE_COMMAND     : return "Device Command";
            case AUDIT_DEVICE_COMMAND_ACK : return "Device Command ACK";
          //case AUDIT_DB_NEW_ACCOUNT     : return "New Account";
          //case AUDIT_DB_DEL_ACCOUNT     : return "Delete Account";
            default                       : return "Undefined";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* audit handler */
    public interface AuditHandler
    {
        public void addAuditEntry(
            String accountID, long auditTime, int auditCode,
            String userID, String deviceID, 
            String ipAddress,
            String privateLabelName,
            String notes);
        public long countAuditEntries(
            String accountID, int auditCodes[],
            String userID, String deviceID, 
            long   sinceTime, long untilTime);
    }

    /* set the audit handler */
    private static AuditHandler auditHandler = null;
    public static void SetAuditHandler(AuditHandler sah)
    {
        Audit.auditHandler = sah;
    }

    /* add an audit entry */
    public static void AddAudit(
        String accountID, long auditTime, int auditCode,
        String userID, String deviceID, 
        String ipAddress,
        String privateLabelName,
        String notes)
    {
        if (Audit.auditHandler != null) {
            long auditTS = (auditTime > 0L)? auditTime : DateTime.getCurrentTimeSec();
            Audit.auditHandler.addAuditEntry(
                accountID, auditTS, auditCode, 
                userID, deviceID, 
                ipAddress, 
                privateLabelName,
                notes);
        }
    }

    /* count audit entries */
    public static long CountAudit(
        String accountID, int auditCodes[],
        String userID, String deviceID, 
        long   sinceTime, long untilTime)
    {
        if (Audit.auditHandler != null) {
            return Audit.auditHandler.countAuditEntries(
                accountID, auditCodes,
                userID, deviceID,
                sinceTime, untilTime);
        } else {
            return 0L;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Audit successful login
    **/
    public static void userLoginOK(String acctID, String userID, String ipAddr, String bplName, String notes)
    {
        long nowTimeSec = DateTime.getCurrentTimeSec();
        Print.logInfo("Login(OK): Time="+nowTimeSec + " Domain="+bplName + " Account="+acctID + " User="+userID + " IP="+ipAddr + " Notes="+notes);
        if (RTConfig.getBoolean(DBConfig.PROP_audit_saveSuccessfulLogin)) {
            Audit.AddAudit(acctID, nowTimeSec, AUDIT_LOGIN_OK, userID, null/*dev*/, ipAddr, bplName, notes);
        }
    }

    /**
    *** Audit failed login
    **/
    public static void userLoginFailed(String acctID, String userID, String ipAddr, String bplName, String notes)
    {
        long nowTimeSec = DateTime.getCurrentTimeSec();
        Print.logInfo("Login(Failed): Time="+nowTimeSec + " Domain="+bplName + " Account="+acctID + " User="+userID + " IP="+ipAddr + " Notes="+notes);
        if (RTConfig.getBoolean(DBConfig.PROP_audit_saveFailedLogin)) {
            Audit.AddAudit(acctID, nowTimeSec, AUDIT_LOGIN_FAILED, userID, null/*dev*/, ipAddr, bplName, notes);
        }
    }

    // --------------------------------

    /**
    *** Return number of failed login attempts since specified time
    **/
    public static long getFailedLoginAttempts(String acctID, String userID, long sinceTime)
    {
        int auditCodes[] = { Audit.AUDIT_LOGIN_FAILED };
        return Audit.CountAudit(
            acctID, auditCodes, 
            userID, null/*deviceID*/,
            sinceTime, -1L);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return number of SMS messages sent during specified time range 
    *** on behalf of the specified AccountID.
    *** This includes SMS messages sent for both command and notification purposes.
    **/
    public static long getSmsMessagesSent(String acctID, long sinceTime, long untilTime)
    {
        //Print.logInfo("SMS count ["+acctID+"]: from="+sinceTime+", to="+untilTime);
        int auditCodes[] = { Audit.AUDIT_SMS_NOTIFY, AUDIT_SMS_COMMAND };
        String userID = null;
        return Audit.CountAudit(
            acctID, auditCodes, 
            userID, null/*deviceID*/,
            sinceTime, untilTime);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Audit rule-trigger Email notifications 
    **/
    public static void ruleEmailNotification(
        String acctID, String devID, String ruleID,
        String toEMail, String subject, String body)
    {
        long nowTimeSec = DateTime.getCurrentTimeSec();
        Print.logInfo("Rule EMail: Time="+nowTimeSec + " Account="+acctID + " Device="+devID + " Rule="+ruleID + " ToEmail="+toEMail);
        if (RTConfig.getBoolean(DBConfig.PROP_audit_saveRuleNotification)) {
            String notes = "Rule="+ruleID + " ToEmail="+toEMail;
            Audit.AddAudit(acctID, nowTimeSec, AUDIT_EMAIL_NOTIFY, null/*user*/, devID, null/*ipAddr*/, null/*bpl*/, notes);
        }
    }

    /**
    *** Audit rule-trigger SMS notifications 
    **/
    public static void ruleSmsNotification(
        String acctID, String devID, String ruleID,
        String toSMS, String message)
    {
        long nowTimeSec = DateTime.getCurrentTimeSec();
        Print.logInfo("Rule SMS: Time="+nowTimeSec + " Account="+acctID + " Device="+devID + " Rule="+ruleID + " ToSMS="+toSMS);
        if (RTConfig.getBoolean(DBConfig.PROP_audit_saveRuleNotification)) {
            String notes = "Rule="+ruleID + " ToSMS="+toSMS;
            Audit.AddAudit(acctID, nowTimeSec, AUDIT_SMS_NOTIFY, null/*user*/, devID, null/*ipAddr*/, null/*bpl*/, notes);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void reportEmail(
        String acctID, String userID, 
        String reportID,
        String toEMail)
    {
        long nowTimeSec = DateTime.getCurrentTimeSec();
        Print.logInfo("Report EMail: Time="+nowTimeSec + " Account="+acctID +" ReportID="+reportID +" ToEmail="+toEMail);
        if (RTConfig.getBoolean(DBConfig.PROP_audit_saveReportEmail)) {
            String notes = "ReportID="+reportID + " ToEmail="+toEMail;
            Audit.AddAudit(acctID, nowTimeSec, AUDIT_EMAIL_NOTIFY, userID, null/*dev*/, null/*ipAddr*/, null/*bpl*/, notes);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Audit device commands.
    *** @param acctID    The ID of the Account that owns the device
    *** @param userID    The ID of the User that sent the command (may be null)
    *** @param devID     The ID of the device to which the command was sent
    *** @param ipAddr    The IP address which originated the command request (may be null)
    *** @param cmdStr    The command string
    *** @param cmdProto  The command protocol (UDP,TCP,SMS), null if unknown
    **/
    public static void deviceCommand(
        String acctID, String userID, String devID,
        String ipAddr, 
        String cmdStr,
        DCServerConfig.CommandProtocol cmdProto)
    {
        long nowTimeSec = DateTime.getCurrentTimeSec();
        Print.logInfo("Device Command: Time="+nowTimeSec+" Acct="+acctID+" User="+userID+" Dev="+devID+" Cmd="+cmdStr+" Proto="+cmdProto);
        if (RTConfig.getBoolean(DBConfig.PROP_audit_saveDeviceCommand)) {
            Audit.AddAudit(acctID, nowTimeSec, AUDIT_DEVICE_COMMAND, userID, devID, ipAddr, null/*bpl*/, cmdStr/*notes*/);
            if ((cmdProto != null) && cmdProto.isSMS()) {
                Audit.AddAudit(acctID, nowTimeSec, AUDIT_SMS_COMMAND, userID, devID, ipAddr, null/*bpl*/, cmdStr/*notes*/);
            }
        }
    }

    // ------------------------------------------------------------------------

}
