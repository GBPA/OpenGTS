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
//  JavaMail support.
//  The use of this module requires that the JavaMail api in downloaed and installed.
//  Support for the JavaMail api can be downloaded from the following location:
//   http://java.sun.com/products/javamail/index.jsp
// References:
//   - http://www.programmers-corner.com/sourcecode/278?PHPSESSID=93f30447970535d4c2a068a22ddb6e23
//   - http://forum.java.sun.com/thread.jspa?threadID=5146702&tstart=314
//   - http://forum.java.sun.com/thread.jspa?threadID=706550&tstart=135
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/03  Martin D. Flynn
//     -Renamed source file to "SendMail.java.save" to temporarily remove 
//      the requirement of having the JavaMail api installed in order to compile
//      the server.
//  2006/06/30  Martin D. Flynn
//     -Repackaged.
//  2006/07/13  Martin D. Flynn
//     -Added support for specifying user/password.
//     -Added support SSL connections.
//  2006/09/16  Martin D. Flynn
//     -Added 'main' to allow testing this module
//  2008/02/27  Martin D. Flynn
//     -Added "image/png" mime type
//  2009/01/01  Martin D. Flynn
//     -Added thread-model THREAD_NONE for debug purposes.  
//      Similar to THREAD_DEBUG but skips sending email quietly.
//  2012/02/03  Martin D. Flynn
//     -Fixed displayed error when "SendMailArgs" class cannot be found/returned.
//  2012/10/16  Martin D. Flynn
//     -Added "sendSysadmin(...)" for sending sysadmin notification emails.
//  2013/09/26  Martin D. Flynn
//     -Added support for overriding ThreadPool parameters.
//  2016/04/06  Martin D. Flynn
//     -Added "EMailContent" class.  
//     -Added "Attachments" class to hold MimeMultipart type and Attachment list.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

public class SendMail
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static long SleepMSBetweenEMails = 0L;

    /**
    *** Sets the amount of time to sleep after sending an email.<br>
    *** Note: this must be used with caution, since it could cause a significant
    *** backlog if many emails are sent to the threadpool.
    **/
    public static void SetSleepAfterEMailMS(long sleepMS) 
    {
        if (sleepMS <= 0L) {
            SendMail.SleepMSBetweenEMails = 0L;
        } else
        if (sleepMS > 20000L) {
            SendMail.SleepMSBetweenEMails = 20000L;
        } else {
            SendMail.SleepMSBetweenEMails = sleepMS;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String SendMailArgs_className  = "org.opengts.util.SendMailArgs";

    private static Class<?> _SendMailArgs_class         = null;

    /**
    *** Gets the "SendMailArgs" class
    *** @return The "SendMailArgs" class, if "SendMail" enabled, otherwise null.
    **/
    private static Class<?> GetSendMailArgs_class() 
    {
        if (_SendMailArgs_class == null) {
            try {
                _SendMailArgs_class = Class.forName(SendMailArgs_className);
            } catch (NoClassDefFoundError ncdfe) {
                // -- this class retrieval will fail if JavaMail "mail.jar" is not installed.
                Print.logError("Class '"+SendMailArgs_className+"': " + ncdfe);
                return null;
            } catch (ClassNotFoundException cnfe) {
                // -- this class retrieval will fail if JavaMail "mail.jar" is not installed.
                Print.logError("Class '"+SendMailArgs_className+"': " + cnfe);
                return null;
            } catch (Throwable th) {
                Print.logError("Class '"+SendMailArgs_className+"': " + th);
                return null;
            }
        }
        return _SendMailArgs_class;
    }

    /**
    *** Returns true if "SendMail" is enabled
    *** @return True if "SendMail" is enabled, otherwise false
    **/
    public  static boolean IsSendMailEnabled()
    {
        return (GetSendMailArgs_class() != null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String EXAMPLE_DOT_COM         = "example.com";
    
    public static boolean IsBlankEmailAddress(String email)
    {
        email = StringTools.trim(email); // trim
        if (email.equals("")) {
            return true;
        } else 
        if (StringTools.endsWithIgnoreCase(email,EXAMPLE_DOT_COM)) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private  static final Random Randomizer = new Random();

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Save specified SendMail Args to outbox
    **/
    public static boolean SaveToOutbox(SendMail.Args args)
    {
        // -- extract Args
        String                    from = args.getFrom();           // never null
        String                    to[] = args.getTo();             // never null
        String                    cc[] = args.getCc();             // never null
        String                   bcc[] = args.getBcc();            // never null
        String                 subject = args.getSubject();        // never null
        String                 msgBody = args.getBody();           // never null
        Properties             headers = args.getHeaders();        // never null
        SendMail.Attachments    attach = args.getAttachments();    // may be null
        SmtpProperties       smtpProps = args.getSmtpProperties(); // never null
        if (StringTools.isBlank(from)) {
            // -- no 'From' address
            return false;
        } else
        if ((to == null) || (to.length <= 0)) {
            // -- no 'To' address
            return false;
        }
        // -- convert to String
        String toStr        = StringTools.join(to,",");
        String ccStr        = StringTools.join(cc,",");
        String bccStr       = StringTools.join(bcc,",");
        String attachStr    = (attach    != null)? attach.toString() : "";
        String smtpPropsStr = (smtpProps != null)? smtpProps.toString() : "";
        String headersStr   = (headers   != null)? (new RTProperties(headers)).toString() : "";
        // -- save to outbox
        // -  TODO:
        return false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** SendMailException
    **/
    public static class SendMailException
        extends Exception
    {
        private boolean retrySend = false;
        public SendMailException(String msg) {
            super(msg);
        }
        public SendMailException(Throwable cause) {
            super(cause);
        }
        public SendMailException(String msg, Throwable cause) {
            super(msg, cause);
        }
        public SendMailException setRetry(boolean retry) {
            this.retrySend = retry;
            return this;
        }
        public boolean getRetry() {
            return this.retrySend;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Custom "X" headers

    public  static final String X_OwnerId               = "X-OwnerId";
    public  static final String X_AssetId               = "X-AssetId";
    public  static final String X_PageType              = "X-PageType";
    public  static final String X_Requestor             = "X-Requestor";
    public  static final String X_OriginatingIP         = "X-OriginatingIP";
    public  static final String X_EventTime             = "X-EventTime";
    public  static final String X_StatusCode            = "X-StatusCode";
    public  static final String X_AlarmRule             = "X-AlarmRule";
    public  static final String X_GPSLocation           = "X-GPSLocation";

    // ------------------------------------------------------------------------

    public  static final byte   MAGIC_GIF_87a[]         = HTMLTools.MAGIC_GIF_87a;
    public  static final byte   MAGIC_GIF_89a[]         = HTMLTools.MAGIC_GIF_89a;
    public  static final byte   MAGIC_JPEG[]            = HTMLTools.MAGIC_JPEG;
    public  static final byte   MAGIC_PNG[]             = HTMLTools.MAGIC_PNG; 

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final String THREAD_NONE             = "none";
    public  static final int    _THREAD_NONE            = -1;
    public  static final String THREAD_CURRENT          = "current";
    public  static final int    _THREAD_CURRENT         = 0;
    public  static final String THREAD_POOL             = "pool"; // preferred
    public  static final int    _THREAD_POOL            = 1;
    public  static final String THREAD_NEW              = "new";
    public  static final int    _THREAD_NEW             = 2;
    public  static final String THREAD_DEBUG            = "debug";
    public  static final int    _THREAD_DEBUG           = 3;

    /**
    *** Returns true if the SendMail thread-model should be displayed when in debug mode.
    **/
    public static void SetShowThreadModel(boolean show)
    {
        RTConfig.setBoolean(RTKey.SMTP_THREAD_MODEL_SHOW, show);
    }

    /**
    *** Sets the SendMail thread-model 'show' state (during debug mode)
    **/
    public static boolean GetShowThreadModel()
    {
        return RTConfig.getBoolean(RTKey.SMTP_THREAD_MODEL_SHOW);
    }

    /**
    *** Sets the 'thread model' for email sent by this class.<br>
    *** The valid values are THREAD_CURRENT, THREAD_POOL, THREAD_NEW, or THREAD_DEBUG.
    *** @param model  The specified thread model.
    **/
    public static void SetThreadModel(String model)
    {
        RTConfig.setString(RTKey.SMTP_THREAD_MODEL, model);
    }

    /**
    *** Sets the 'thread model' for email sent by this class.<br>
    *** The valid values are THREAD_CURRENT, THREAD_POOL, THREAD_NEW, or THREAD_DEBUG.
    *** @param model  The specified thread model.
    *** @param show   True to display the thread model when sending an email (debug purposes only).
    **/
    public static void SetThreadModel(String model, boolean show)
    {
        SendMail.SetThreadModel(model);
        SendMail.SetShowThreadModel(false); // show);
    }

    /** 
    *** Returns the thread model in effect for this class
    *** @return  The thread model in effect.
    **/
    private static int GetThreadModel() 
    {
        return GetThreadModel(RTConfig.getString(RTKey.SMTP_THREAD_MODEL));
    }
    
    /**
    *** Returns the int representation of the specified thread model.
    *** @param model The thread model String representation
    *** @return The thread model 'int' representation
    **/
    private static int GetThreadModel(String model) 
    {
        if (model == null) {
            // If this is a server, then we want a thread pool
            // If this is a one-shot 'main' program, we want the current thread
            return RTConfig.isTestMode()? _THREAD_CURRENT : _THREAD_POOL;
            // If a thread pool is needed while in test mode, then the thread model
            // will have to be set explicitly.
        } else
        if (model.equalsIgnoreCase(THREAD_NONE)) {
            return _THREAD_NONE;
        } else
        if (model.equalsIgnoreCase(THREAD_POOL)) {
            return _THREAD_POOL;
        } else
        if (model.equalsIgnoreCase(THREAD_CURRENT)) {
            return _THREAD_CURRENT;
        } else
        if (model.equalsIgnoreCase(THREAD_NEW)) {
            return _THREAD_NEW;
        } else
        if (model.equalsIgnoreCase(THREAD_DEBUG)) {
            return _THREAD_DEBUG;
        } else {
            return _THREAD_POOL;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* SendMail ThreadPool */
    // SendMail.ThreadPool.maximumPoolSize=20
    // SendMail.ThreadPool.maximumIdleSeconds=0
    // SendMail.ThreadPool.maximumQueueSize=0
    private static final RTKey PROP_ThreadPool_SendMail_    = RTKey.valueOf(RTKey.ThreadPool_SendMail_);
    private static final int   ThreadPool_SendMail_Size     = 20;   // max threads
    private static final int   ThreadPool_SendMail_IdleSec  =  0;   // trim idle threads
    private static final int   ThreadPool_SendMail_QueSize  =  0;   // max queue size
    private static ThreadPool  ThreadPool_SendMail          = new ThreadPool(
        "SendMail",
        PROP_ThreadPool_SendMail_, // property allowing default override
        ThreadPool_SendMail_Size, 
        ThreadPool_SendMail_IdleSec, 
        ThreadPool_SendMail_QueSize);
        
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Convenience method for sending notification regarding internal errors
    
    //public static void sendError(String subject, String msgBody)
    //{
    //    Properties headers = null;
    //    String emailFrom   = RTConfig.getString(RTKey.ERROR_EMAIL_FROM);
    //    String emailTo     = RTConfig.getString(RTKey.ERROR_EMAIL_TO);
    //    if ((emailFrom != null) && (emailTo != null)) {
    //        SendMail.Attachment attach[] = null;
    //        SendMail.send(headers,emailFrom,emailTo,subject,msgBody,attach,null/*SmtpProps*/);
    //    }
    //}

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the specified override "From" email address, if non-blank, otherwise
    *** the default configured "From" email address will be returned.
    *** @param mainEmail  The overriding email address to return if non-blank
    *** @return The SMTP user "From" email address.
    **/
    public static String getDefaultUserEmail(String mainEmail)
    {
        if (!SendMail.IsBlankEmailAddress(mainEmail)) {
            return StringTools.trim(mainEmail);
        } else {
            String email = RTConfig.getString(RTKey.SMTP_SERVER_USER_EMAIL, null);
            return !SendMail.IsBlankEmailAddress(email)? email : null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sends a system notification email to the defined system admin email address
    *** @param subject  The email subject
    *** @param msgBody  The email message body
    *** @return True if email is sent
    **/
    public static boolean sendSysadmin(String subject, String msgBody)
    {
        SmtpProperties smtpProps = new SmtpProperties("SendMail:sysadmin");
        boolean retrySend = false;

        /* "To:" */
        String to   = smtpProps.getSysadminEmail();
        if (StringTools.isBlank(to)) {
            Print.logWarn("'To' \""+RTKey.SMTP_SYSADMIN_EMAIL+"\" not specified");
            return false;
        }
        
        /* "From:" */
        String from = smtpProps.getFromEmailType("sysadmin");
        if (StringTools.isBlank(from)) {
            Print.logWarn("'From' \""+RTKey.SMTP_SERVER_USER_EMAIL+"\" not specified");
            return false;
        }

        /* send */
        return SendMail.send(from, to, subject, msgBody, smtpProps, retrySend);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sends an email
    *** @param from      The sender of the email
    *** @param to        A comma-separated list of email recipients.
    *** @param subject   The email subject.
    *** @param msgBody   The email message body.
    *** @param smtpProps The custom SMTP properties
    *** @return True if email is queued/sent
    **/
    public static boolean send(
        String from, String to, 
        String subject, String msgBody,
        SmtpProperties smtpProps)
    {
        String ato[]  = (to != null)? StringTools.parseStringArray(to ,',') : null;
        String acc[]  = null;
        String abcc[] = null;
        return SendMail.send(null,from,ato,acc,abcc,subject,msgBody,null/*attach*/,smtpProps,false);
    }

    /**
    *** Sends an email
    *** @param from      The sender of the email
    *** @param to        A comma-separated list of email recipients.
    *** @param subject   The email subject.
    *** @param msgBody   The email message body.
    *** @param smtpProps The custom SMTP properties
    *** @param queRetry  True to queue to outbox on connection/auth failure.
    *** @return True if email is queued/sent
    **/
    public static boolean send(
        String from, String to, 
        String subject, String msgBody,
        SmtpProperties smtpProps,
        boolean queRetry)
    {
        String ato[]  = (to != null)? StringTools.parseStringArray(to ,',') : null;
        String acc[]  = null;
        String abcc[] = null;
        return SendMail.send(null,from,ato,acc,abcc,subject,msgBody,null/*attach*/,smtpProps,queRetry);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sends an email
    *** @param from     The sender of the email
    *** @param to       A comma-separated list of email recipients.
    *** @param subject  The email subject.
    *** @param msgBody  The email message body.
    *** @param attach   An email attachment
    *** @param smtpProps The custom SMTP properties
    *** @return True if email is queued/sent
    **/
    public static boolean send(
        String from, String to, 
        String subject, String msgBody, 
        SendMail.Attachments attach,
        SmtpProperties smtpProps)
    {
        String ato[]  = (to  != null)? StringTools.parseStringArray(to ,',') : null;
        String acc[]  = null;
        String abcc[] = null;
        return SendMail.send(null,from,ato,acc,abcc,subject,msgBody,attach,smtpProps,false);
    }

    /**
    *** Sends an email
    *** @param from     The sender of the email
    *** @param to       A comma-separated list of email recipients.
    *** @param subject  The email subject.
    *** @param msgBody  The email message body.
    *** @param attach   An email attachment
    *** @param smtpProps The custom SMTP properties
    *** @param queRetry  True to queue to outbox on connection/auth failure.
    *** @return True if email is queued/sent
    **/
    public static boolean send(
        String from, String to, 
        String subject, String msgBody, 
        SendMail.Attachments attach,
        SmtpProperties smtpProps,
        boolean queRetry)
    {
        String ato[]  = (to  != null)? StringTools.parseStringArray(to ,',') : null;
        String acc[]  = null;
        String abcc[] = null;
        return SendMail.send(null,from,ato,acc,abcc,subject,msgBody,attach,smtpProps,queRetry);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sends an email
    *** @param from     The sender of the email
    *** @param to       A comma-separated list of 'To:' email recipients.
    *** @param cc       A comma-separated list of 'Cc:' email recipients.
    *** @param bcc      A comma-separated list of 'Bcc:' email recipients.
    *** @param subject  The email subject.
    *** @param msgBody  The email message body.
    *** @param attach   An email attachment
    *** @param smtpProps The custom SMTP properties
    *** @return True if email is queued/sent
    **/
    public static boolean send(
        String from, String to, String cc, String bcc,
        String subject, String msgBody, 
        SendMail.Attachments attach,
        SmtpProperties smtpProps)
    {
        String ato[]  = (to  != null)? StringTools.parseStringArray(to ,',') : null;
        String acc[]  = (cc  != null)? StringTools.parseStringArray(cc ,',') : null;
        String abcc[] = (bcc != null)? StringTools.parseStringArray(bcc,',') : null;
        return SendMail.send(null,from,ato,acc,abcc,subject,msgBody,attach,smtpProps,false);
    }

    /**
    *** Sends an email
    *** @param from     The sender of the email
    *** @param to       A comma-separated list of 'To:' email recipients.
    *** @param cc       A comma-separated list of 'Cc:' email recipients.
    *** @param bcc      A comma-separated list of 'Bcc:' email recipients.
    *** @param subject  The email subject.
    *** @param msgBody  The email message body.
    *** @param attach   An email attachment
    *** @param smtpProps The custom SMTP properties
    *** @param queRetry True to queue to outbox on connection/auth failure.
    *** @return True if email is queued/sent
    **/
    public static boolean send(
        String from, String to, String cc, String bcc,
        String subject, String msgBody, 
        SendMail.Attachments attach,
        SmtpProperties smtpProps,
        boolean queRetry)
    {
        String ato[]  = (to  != null)? StringTools.parseStringArray(to ,',') : null;
        String acc[]  = (cc  != null)? StringTools.parseStringArray(cc ,',') : null;
        String abcc[] = (bcc != null)? StringTools.parseStringArray(bcc,',') : null;
        return SendMail.send(null,from,ato,acc,abcc,subject,msgBody,attach,smtpProps,queRetry);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sends an email
    *** @param from      The sender of the email
    *** @param to        An array of 'To:' email recipients.
    *** @param cc        An array of 'Cc:' email recipients.
    *** @param bcc       An array of 'Bcc:' email recipients.
    *** @param subject   The email subject.
    *** @param msgBody   The email message body.
    *** @param attach    An email attachment
    *** @param smtpProps The custom SMTP properties
    *** @param queRetry  An email attachment
    *** @return True
    **/
    public static boolean send(
        Properties headers, 
        String from, String to[], String cc[], String bcc[], 
        String subject, String msgBody, 
        SendMail.Attachments attach,
        SmtpProperties smtpProps,
        boolean queRetry)
    {
        Args args = new SendMail.Args(headers,from,to,cc,bcc,subject,msgBody,attach,smtpProps,queRetry);
        SendMailRunnable smr = new SendMailRunnable(args);
        boolean showThreadModel = SendMail.GetShowThreadModel();
        switch (SendMail.GetThreadModel()) {
            case _THREAD_NONE     :
                //if (showThreadModel) {
                    Print.logDebug("Skipping SendMail (disabled by '"+RTKey.SMTP_THREAD_MODEL+"') ...");
                //}
                return false;
            case _THREAD_CURRENT  :
                if (showThreadModel) {
                    Print.logDebug("Running SendMail in current thread");
                }
                smr.run();
                return smr.emailSent();
            case _THREAD_NEW   :
                if (showThreadModel) {
                    Print.logDebug("Starting new SendMail thread");
                }
                (new Thread(smr)).start();
                return true;
            case _THREAD_DEBUG :
                Print.logDebug("Debug SendMail (email not sent)");
                Print.logDebug(smr.getArgs().toString());
                return false;
            case _THREAD_POOL  :
            default :
                if (showThreadModel) {
                    Print.logDebug("Running SendMail in thread pool");
                }
                ThreadPool_SendMail.run(smr);
                return true;
        }
    }

    /**
    *** SendMailRunnable class.
    **/
    private static class SendMailRunnable
        implements Runnable
    {
        private Args      args      = null;
        private Throwable sendError = null;
        private boolean   emailSent = false;
        private boolean   retrySend = false;
        public SendMailRunnable(Args args) {
            this.args = args;
        }
        public Args getArgs() {
            return this.args;
        }
        public void run() {
            // -- no args?
            if (this.args == null) {
                // -- exit now
                Print.logWarn("No SMTP parameters, ignoring email ...");
                return;
            }
            // -- send email
            long startMS    = System.currentTimeMillis();
            int maxTryCount = this.args.getRetryCount() + 1; // at least one
            for (int i = 0; i < maxTryCount; i++) {
                try {
                    this.retrySend = false;
                    //this.emailSent = SendMailArgs.send(this.args);
                    Class<?> sendMailArgs = GetSendMailArgs_class();
                    if (sendMailArgs != null) {
                        MethodAction ma = new MethodAction(sendMailArgs, "send", Args.class);
                        ma.invoke(this.args); 
                        this.emailSent = true; // successful if we are here
                        break; // success
                    } else {
                        throw new SendMail.SendMailException("SendMailArgs: 'javax.mail.jar' may not be properly installed");
                    }
                } catch (SendMail.SendMailException sme) {
                    // -- failed to send email
                    this.sendError = sme;
                    this.emailSent = false;
                    this.retrySend = sme.getRetry();
                    if (this.retrySend) {
                        // -- sleep for a random amount of time, then try again
                        Print.logInfo("Email 'send' failed: " + sme + " (retrying...)");
                        long sleepMS = (long)Randomizer.nextInt(500);
                        try { Thread.sleep(sleepMS); } catch (Throwable th) {/*ignore*/}
                        continue; // retry ok
                    }
                    Print.logWarn("Email 'send' failed: " + sme);
                } catch (Throwable th) {
                    // -- catch-all, should not occur
                    Print.logWarn("Email 'send' failed: " + th);
                    this.sendError = th;
                    this.emailSent = false;
                    this.retrySend = false;
                }
                // -- no retry
                break;
            }
            // -- still need to retry?  add to outbox
            if (!this.emailSent && this.retrySend && this.args.getQueueRetry()) {
                // -- save to outbox
                SendMail.SaveToOutbox(this.args);
            }
            // -- sleep after sending email?
            long endMS = System.currentTimeMillis();
            if (SendMail.SleepMSBetweenEMails > 0L) {
                // -- hack to slow down emails being sent for SMTP servers that cannot handle the volume
                long deltaMS = SendMail.SleepMSBetweenEMails - (endMS - startMS);
                if (deltaMS > 0L) {
                    long sleepMS = Math.min(deltaMS, 30000L);
                    try { Thread.sleep(sleepMS); } catch (Throwable th) {/*ignore*/}
                }
            }
        }
        public boolean emailSent() {
            return this.emailSent;
        }
        public boolean retrySend() {
            return this.retrySend;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the MIME contents type for the specified file contents
    *** @param data  The file contents
    *** @return The MIME content type
    **/
    public static String DefaultContentType(byte data[], String dft)
    {
        String code = null;

        /* GIF */
        if (StringTools.compareEquals(data,MAGIC_GIF_87a,-1)) {
            return HTMLTools.MIME_GIF();
        } else
        if (StringTools.compareEquals(data,MAGIC_GIF_89a,-1)) {
            return HTMLTools.MIME_GIF();
        }

        /* JPEG */
        if (StringTools.compareEquals(data,MAGIC_JPEG,-1)) {
            return HTMLTools.MIME_JPEG();
        }

        /* PNG */
        if (StringTools.compareEquals(data,MAGIC_PNG,-1)) {
            return HTMLTools.MIME_PNG();
        }

        /* HTML */
        if (HTMLTools.isHtmlText(data)) {
            return HTMLTools.MIME_HTML();
        }

        /* default */
        return dft;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String DFT_ATTACHMENT_NAME   = "attachment.att";
    private static final String DFT_ATTACHMENT_TYPE   = HTMLTools.CONTENT_TYPE_OCTET;

    public  static final String MULTIPART_MIXED       = "mixed";       // multipart/mixed
    public  static final String MULTIPART_ALTERNATIVE = "alternative"; // multipart/alternative
    public  static final String MULTIPART_RELATED     = "related";     // multipart/related
    public  static final String MULTIPART_REPORT      = "report";      // multipart/report

    /**
    *** A container for an email attachment
    **/
    public static class Attachment
    {
        private byte       data[] = null;
        private String     name   = DFT_ATTACHMENT_NAME;
        private String     type   = DFT_ATTACHMENT_TYPE;
        public Attachment(byte data[]) {
            // -- attachment data only, default name/type
            this(data, null, null);
        }
        public Attachment(byte data[], String name, String type) {
            // -- explicit attachment components
            this.data = data;
            this.name = !StringTools.isBlank(name)? name : DFT_ATTACHMENT_NAME;
            this.type = !StringTools.isBlank(type)? type : DFT_ATTACHMENT_TYPE;
        }
        public Attachment(String csvData) {
            // -- reconstruct attachment from String
            // "name,mime,0x1234567890"
            String d[] = StringTools.split(csvData,',');
            this.name = (d.length > 0)? d[0] : null;
            this.type = (d.length > 1)? d[1] : null;
            this.data = (d.length > 2)? StringTools.parseHex(d[2],null) : null;
            if (StringTools.isBlank(this.name)) {
                this.name = DFT_ATTACHMENT_NAME;
            }
            if (StringTools.isBlank(this.type)) {
                this.type = DFT_ATTACHMENT_TYPE;
            }
        }
        public byte[] getBytes() {
            return this.data;
        }
        public int getSize() {
            return (this.data != null)? this.data.length : 0;
        }
        public String getName() {
            return this.name;
        }
        public String getType() {
            return this.type;
        }
        public boolean isTextPlain() {
            String t = this.getType();
            if (t.equalsIgnoreCase(HTMLTools.CONTENT_TYPE_PLAIN)) {
                return true;
            }
            return false;
        }
        public boolean isHTML() {
            String t = this.getType();
            if (t.equalsIgnoreCase(HTMLTools.CONTENT_TYPE_HTML)) {
                return true;
            }
            return false;
        }
        public boolean isImage() {
            String t = this.getType();
            if (StringTools.startsWithIgnoreCase(t,HTMLTools.CONTENT_TYPE_IMAGE_)) {
                // -- image MIME types
                // -    HTMLTools.CONTENT_TYPE_GIF
                // -    HTMLTools.CONTENT_TYPE_JPEG
                // -    HTMLTools.CONTENT_TYPE_PNG
                // -    HTMLTools.CONTENT_TYPE_TIFF
                // -    HTMLTools.CONTENT_TYPE_BMP
                return true;
            } else {
                return false;
            }
        }
        public String toString() {
            // "name,mime,0x1234567890"
            StringBuffer sb = new StringBuffer();
            sb.append(this.getName()).append(",");
            sb.append(this.getType()).append(",");
            if (this.getSize() > 0) {
                sb.append("0x").append(StringTools.toHexString(this.getBytes()));
            }
            return sb.toString();
        }
    }

    /**
    *** A container for list of individual <code>Attachment</code> objects.
    *** (does not yet support containing recursive lists of <code>Attachments</code>)
    **/
    public static class Attachments
    {
        private String                      multipartType = "";
        private Vector<SendMail.Attachment> attach        = null;
        // --
        public Attachments() {
            super();
        }
        public Attachments(String mpt, SendMail.Attachment... att) {
            this();
            this.setMultipartType(mpt);
            this.setAttachments(att);
        }
        public Attachments(SendMail.Attachment... att) {
            this(null,att);
        }
        // --
        public void setMultipartType(String mpt) {
            this.multipartType = StringTools.trim(mpt);
        }
        public String getMultipartType() {
            return StringTools.trim(this.multipartType);
        }
        public String getMultipartType(String dftType) {
            String mpt = this.getMultipartType();
            if (!StringTools.isBlank(mpt)) {
                // -- explicit Multipart type for this group of Attachments
                return mpt;
            } else
            if (!StringTools.isBlank(dftType)) {
                // -- system default Multipart type
                return StringTools.trim(dftType);
            } else 
            if (!this.hasAttachments()) {
                // -- no Attachments, return "mixed"
                return ""; // mixed
            } else {
                // -- guess at MimeMultipart type based on Attachment types
                boolean hasPlain  = false;
                boolean hasHTML   = false;
                boolean hasImages = false;
                for (int i = 0; i < this.attach.size(); i++) {
                    SendMail.Attachment att = this.attach.get(i);
                    if (att.isTextPlain()) {
                        // -- alternative or mixed
                        hasPlain = true;
                        if (hasImages) {
                            return ""; // mixed (images and text)
                        }
                    } else
                    if (att.isHTML()) {
                        // -- alternative
                        hasHTML = true;
                    } else 
                    if (att.isImage()) {
                        // -- related or mixed
                        hasImages = true;
                        if (hasPlain) {
                            return ""; // mixed (images and text)
                        }
                    } else {
                        // -- mixed (something other than images/text)
                        return ""; // mixed 
                    }
                }
                // --
                if (!hasHTML) {
                    // -- no HTML, assume "mixed"
                    return ""; // mixed 
                } else
                if (hasImages && !hasPlain) {
                    // -- HTML and Images (no text)
                    return MULTIPART_RELATED;
                } else
                if (!hasImages && hasPlain) {
                    // -- HTML and PlainText (no images)
                    return MULTIPART_ALTERNATIVE;
                } else {
                    // -- HTML and Images and PlainText
                    return ""; // mixed
                }
            }
        }
        // --
        public void setAttachments(java.util.List<SendMail.Attachment> attList) {
            this.attach = null;
            if (attList != null) {
                for (SendMail.Attachment A : attList) {
                    this.addAttachment(A);
                }
            }
        }
        public void setAttachments(SendMail.Attachment... attList) {
            this.attach = null;
            if (attList != null) {
                for (SendMail.Attachment A : attList) {
                    this.addAttachment(A);
                }
            }
        }
        public void addAttachment(SendMail.Attachment att) {
            if ((att != null) && (att.getSize() > 0)) {
                if (this.attach == null) {
                    this.attach = new Vector<SendMail.Attachment>();
                }
                this.attach.add(att);
            }
        }
        public void insertAttachment(SendMail.Attachment att) {
            if ((att != null) && (att.getSize() > 0)) {
                if (this.attach == null) {
                    this.attach = new Vector<SendMail.Attachment>();
                }
                this.attach.add(0,att); // add as first Attachment
            }
        }
        public java.util.List<SendMail.Attachment> getAttachments() {
            return this.attach;
        }
        public boolean hasAttachments() {
            return !ListTools.isEmpty(this.attach);
        }
        // -- 
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getMultipartType());
            if (this.hasAttachments()) {
                for (SendMail.Attachment A : this.getAttachments()) {
                    sb.append("|");
                    sb.append(A.toString());
                }
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String START_DELIM   = RTProperties.KEY_START_DELIMITER;  // "${";
    private static final String END_DELIM     = RTProperties.KEY_END_DELIMITER;    // "}";
    private static final String DFT_DELIM     = RTProperties.KEY_DFT_DELIMITER;    // "=";

    private static final String SUBJECT_      = ".EmailSubject.";
    private static final String ATTACHMENT_   = ".EmailAttachment.";
    private static final String BODY_         = ".EmailBody.";

    /**
    *** A container for the content of an email (Subject/Body/Attachment)
    **/
    public static class EMailContent
        implements Cloneable
    {
        private String               subject     = "";
        private String               msgBody     = "";
        private SendMail.Attachments attachments = null;
        public EMailContent() {
            super();
        }
        public EMailContent(String subj, String body, SendMail.Attachments attach) {
            this.setSubject(subj);
            this.setBody(body);
            this.setAttachments(attach);
        }
        public EMailContent(String subj, String body) {
            this(subj, body, null);
        }
        public EMailContent(File file) {
            this();
            this.loadFromFile(file);
        }
        public EMailContent(EMailContent other) {
            super();
            this.setSubject(other.getSubject());
            this.setBody(other.getBody());
            this.setAttachments(other.getAttachments());
        }
        // --
        public boolean isValid() {
            if (!this.hasSubject()) {
                return false;
            } else
            if (!this.hasBody()) {
                return false;
            } else {
                return true;
            }
        }
        // --
        public boolean loadFromFile(File file) {
            // Format:
            //      .EMailSubject.
            //          This is the subject
            //      .EMailAttachments. 
            //          /path/to/attached/file.pdf
            //          /another/path/to/an/attached/file.xls
            //      .EMailBody.
            //      This is the body of the email
            boolean didLoad = false;
            // -- validate file
            if (!FileTools.isFile(file)) {
                // -- not a file
                return didLoad; // false
            } else
            if (!FileTools.canRead(file)) {
                // -- file not readable
                return didLoad; // false
            }
            // -- read file text
            String text = StringTools.toStringValue(FileTools.readFile(file));
            if (StringTools.isBlank(text)) {
                // -- file is empty
                return didLoad; // false
            }
            text = text.trim();
            // -- find location of email components
            int emailSubjectP = StringTools.indexOfIgnoreCase(text,SUBJECT_);    // required
            int emailAttachP  = StringTools.indexOfIgnoreCase(text,ATTACHMENT_); // optional
            int emailBodyP    = StringTools.indexOfIgnoreCase(text,BODY_);       // required
            // -- extract subject
            if (emailSubjectP >= 0) {
                // -- "Subject:" is present
                int s = emailSubjectP + SUBJECT_.length();
                int e = text.length();
                if ((emailAttachP >= s) && (emailAttachP < e)) { e = emailAttachP ; }
                if ((emailBodyP   >= s) && (emailBodyP   < e)) { e = emailBodyP   ; }
                // --
                String subject = text.substring(s,e).trim();
                this.setSubject(subject);
                didLoad = true;
            }
            // -- extract attachments
            if (emailAttachP >= 0) {
                // -- "Attachment:" is present
                int s = emailAttachP + ATTACHMENT_.length();
                int e = text.length();
                if ((emailSubjectP >= s) && (emailSubjectP < e)) { e = emailSubjectP; }
                if ((emailBodyP    >= s) && (emailBodyP    < e)) { e = emailBodyP   ; }
                // --
                String attachList[] = StringTools.split(text.substring(s,e),'\n');
                for (String attachS : attachList) {
                    if (StringTools.isBlank(attachS)) { continue; }
                    File attachF = new File(attachS);
                    byte attachB[] = FileTools.readFile(attachF);
                    if (!ListTools.isEmpty(attachB)) {
                        String fileName = attachF.getName();
                        String fileExtn = FileTools.getExtension(attachF);
                        String mimeType = HTMLTools.getMimeTypeFromExtension(fileExtn,null);
                        if (mimeType == null) {
                            mimeType = SendMail.DefaultContentType(attachB,HTMLTools.CONTENT_TYPE_OCTET);
                        }
                        this.addAttachment(new SendMail.Attachment(attachB,fileName,mimeType));
                        didLoad = true;
                    } else {
                        // -- unable to read attachment file?
                        Print.logError("Unable to load attachment file: " + attachS);
                    }
                }
            }
            // -- extract body
            if (emailBodyP >= 0) {
                // -- "Body:" is present
                int s = emailBodyP + BODY_.length();
                int e = text.length();
                if ((emailSubjectP >= s) && (emailSubjectP < e)) { e = emailSubjectP; }
                if ((emailAttachP  >= s) && (emailAttachP  < e)) { e = emailAttachP ; }
                // --
                String body = text.substring(s,e).trim();
                this.setBody(body);
                didLoad = true;
            }
            // -- updated?
            return didLoad;
        }
        // --
        public boolean hasSubject() {
            return !StringTools.isBlank(this.subject)? true : false;
        }
        public void setSubject(String subj) {
            this.subject = StringTools.trim(subj);
        }
        public String getSubject() {
            return this.subject;
        }
        public String getSubject(StringTools.KeyValueMap kvMap) {
            String subj = (kvMap != null)?
                StringTools.insertKeyValues(this.subject,START_DELIM,END_DELIM,DFT_DELIM,kvMap) :
                RTConfig.insertKeyValues(   this.subject,START_DELIM,END_DELIM,DFT_DELIM);
            return subj;
        }
        // --
        public boolean hasBody() {
            return !StringTools.isBlank(this.msgBody)? true : false;
        }
        public boolean isBodyHtml() {
            return HTMLTools.isHtmlText(this.msgBody);
        }
        public void setBody(String body) {
            this.msgBody = StringTools.trim(body);
        }
        public String getBody() {
            return this.msgBody;
        }
        public String getBody(StringTools.KeyValueMap kvMap) {
            String body = (kvMap != null)?
                StringTools.insertKeyValues(this.msgBody,START_DELIM,END_DELIM,DFT_DELIM,kvMap) :
                RTConfig.insertKeyValues(   this.msgBody,START_DELIM,END_DELIM,DFT_DELIM);
            return StringTools.replace(body,"\\n","\n");
        }
        // --
        public SendMail.Attachment createHtmlAttachment(String html) {
            if (!StringTools.isBlank(html)) {
                byte b[] = html.getBytes();
                return new SendMail.Attachment(b, "email.html", HTMLTools.MIME_HTML());
            } else {
                return null;
            }
        }
        // --
        public boolean hasAttachments() {
            return (this.attachments != null)? this.attachments.hasAttachments() : false;
        }
        public void setAttachments(SendMail.Attachments attach) {
            this.attachments = attach; // may be null
        }
        public void addAttachment(SendMail.Attachment att) {
            if ((att != null) && (att.getSize() > 0)) {
                if (this.attachments == null) {
                    this.attachments = new SendMail.Attachments();
                }
                this.attachments.addAttachment(att);
            }
        }
        public SendMail.Attachments getAttachments() {
            return this.attachments; // may be null
        }
        // --
        public EMailContent insertKeyValues(StringTools.KeyValueMap kvMap) {
            this.subject = this.getSubject(kvMap);
            this.msgBody = this.getBody(kvMap);
            return this;
        }
        public EMailContent insertKeyValues() {
            return this.insertKeyValues(null);
        }
        // -- 
        public EMailContent clone() {
            return new EMailContent(this);
        }
        public EMailContent clone(StringTools.KeyValueMap kvMap) {
            return this.clone().insertKeyValues(kvMap);
        }
        // --
        public String toString() {
            return this.getSubject();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** A container for the arguments of an email
    **/
    public static class Args
    {
        private Properties           headers     = null;
        private String               from        = null;
        private String               to[]        = null;
        private String               cc[]        = null;
        private String               bcc[]       = null;
        private String               subject     = null;
        private String               msgBody     = null;
        private SendMail.Attachments attachments = null;
        private SmtpProperties       smtpProps   = null;
        private boolean              queRetry    = false;
        //public Args(Properties headers, 
        //    String from, String to[], String cc[], String bcc[], 
        //    String subject, String msgBody, 
        //    SendMail.Attachment attach[],
        //    SmtpProperties smtpProps) {
        //    this(headers,from,to,cc,bcc,subject,msgBody,attach,smtpProps,false/*retry?*/);
        //}
        public Args(Properties headers, 
            String from, String to[], String cc[], String bcc[], 
            String subject, String msgBody, 
            SendMail.Attachments attach,
            SmtpProperties smtpProps,
            boolean queRetry) {
            this.headers     = (headers != null)? headers : new Properties();
            this.from        = from;
            this.to          = to;
            this.cc          = cc;
            this.bcc         = bcc;
            this.subject     = subject;
            this.msgBody     = msgBody;
            this.attachments = attach;
            this.smtpProps   = (smtpProps != null)? smtpProps : new SmtpProperties("SendMail:args");
            this.queRetry    = queRetry;
        }
        public Properties getHeaders() {
            return this.headers;
        }
        public String getFrom() {
            return (this.from != null)? this.from : "";
        }
        public String[] getTo() {
            return (this.to != null)? this.to : new String[0];
        }
        public String[] getCc() {
            return (this.cc != null)? this.cc : new String[0];
        }
        public String[] getBcc() {
            return (this.bcc != null)? this.bcc : new String[0];
        }
        public String getSubject() {
            return (this.subject != null)? this.subject : "";
        }
        public String getBody() {
            return (this.msgBody != null)? this.msgBody : "";
        }
        public SendMail.Attachments getAttachments() {
            return this.attachments;
        }
        public SmtpProperties getSmtpProperties() {
            return this.smtpProps; // never null
        }
        public boolean getQueueRetry() {
            return this.queRetry;
        }
        public int getRetryCount() {
            if (this.getQueueRetry()) {
                return this.getSmtpProperties().getRetryCount();
            } else {
                return 0;
            }
        }
        public String toString() {
            StringBuffer sb = new StringBuffer().append("\n");
            Properties headers = this.getHeaders();
            if ((headers != null) && !headers.isEmpty()) {
                for (Iterator<?> i = headers.keySet().iterator(); i.hasNext();) {
                    String k = (String)i.next();
                    String v = headers.getProperty(k);
                    if (v != null) {
                        sb.append(k).append(": ");
                        sb.append(v).append("\n");
                    }
                }
            }
            sb.append("From: ").append(this.getFrom()).append("\n");
            sb.append("To: ").append(StringTools.encodeArray(this.getTo())).append("\n");
            sb.append("Subject: ").append(this.getSubject()).append("\n");
            sb.append(this.getBody()).append("\n");
            SendMail.Attachments attach = this.getAttachments();
            if ((attach != null) && attach.hasAttachments()) {
                for (SendMail.Attachment A : attach.getAttachments()) {
                    sb.append("---- attachment ----\n");
                    sb.append(StringTools.toHexString(A.getBytes())).append("\n");
                }
            }
            sb.append("\n");
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Validate the specified list of comma-separated email addresses.
    *** @param addr  A comma-separated list of email addresses
    *** @return True if all addresses in the list are valid, false otherwise
    **/
    public static boolean validateAddresses(String addr)
    {
        if (StringTools.isBlank(addr)) {
            return false;
        }
        String addrArry[] = StringTools.parseStringArray(addr, ',');
        if (addrArry.length == 0) { return false; }
        for (int i = 0; i < addrArry.length; i++) {
            String em = addrArry[i].trim();
            if (em.equals("")) { return false; }
            if (!SendMail.validateAddress(em)) { return false; }
        }
        return true;
    }
    
    /**
    *** Validate the specified email address.
    *** @param addr  The email address to validate.
    *** @return True if the specified email address is valid, false otherwise
    **/
    public static boolean validateAddress(String addr)
    {
        return !StringTools.isBlank(SendMail.getEMailAddress(addr));
    }

    /** 
    *** Filters and returns the base email address from the specified String.<br>
    *** For example, if the String "Jones&lt;jones@example.com&gt;" is passed to this
    *** method, then the value "jones@example.com" will be returned.
    *** @param addr The email address to filter.
    *** @return  The filtered email address, or null if the specified email address is invalid.
    **/
    public static String getEMailAddress(String addr)
    {
        //return SendMailArgs.parseEMailAddress(addr);
        try {
            Class<?> sendMailArgs = GetSendMailArgs_class();
            if (sendMailArgs != null) {
                MethodAction ma = new MethodAction(sendMailArgs, "parseEMailAddress", String.class);
                return (String)ma.invoke(addr);
            } else {
                Print.logWarn("Unable to get SendMailArgs class (javax.mail.jar installed?)");
                return null;
            }
        } catch (Throwable th) {
            Print.logWarn("Unable to invoke 'parseEMailAddress': " + th);
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static String ARG_ADDR[]    = new String[] { "addr"     , "a"    };

    private static String ARG_FROM[]    = new String[] { "from"     , "f"    };
    private static String ARG_TO[]      = new String[] { "to"       , "t"    };
    private static String ARG_SUBJECT[] = new String[] { "subject"  , "subj", "s" };
    private static String ARG_BODY[]    = new String[] { "body"     , "b"    };
    private static String ARG_ATTACH[]  = new String[] { "attach"   , "att"  };
    private static String ARG_LOAD[]    = new String[] { "load"     , "file" };

    /**
    *** Command-line debug/testing entry point
    *** @param argv  The command-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        SendMail.SetThreadModel(SendMail.THREAD_CURRENT);
        SmtpProperties smtpProps = new SmtpProperties("SendMail:main"); // default properties
        boolean retrySend = false;

        /* validate email address */
        if (RTConfig.hasProperty(ARG_ADDR)) {
            String addr = RTConfig.getString(ARG_ADDR, "");
            Print.sysPrintln("Checking: " + addr);
            Print.sysPrintln("Address : " + SendMail.getEMailAddress(addr));
            Print.sysPrintln("Valid   ? " + SendMail.validateAddress(addr));
            Print.sysPrintln("");
            System.exit(0);
        }

        /* "From" */
        String fromAddr = RTConfig.getString(ARG_FROM,null);
        fromAddr = RTConfig.insertKeyValues(fromAddr);
        if (StringTools.isBlank(fromAddr) || fromAddr.endsWith(EXAMPLE_DOT_COM)) {
            fromAddr = smtpProps.getFromEmailType("cmdline");
            if (StringTools.isBlank(fromAddr)) {
                Print.sysPrintln("ERROR: Missing 'From' address.");
                Print.sysPrintln("(define \""+RTKey.SMTP_SERVER_USER_EMAIL+"\" in 'default.conf')");
                Print.sysPrintln("");
                System.exit(1);
            }
        } else
        if (fromAddr.equalsIgnoreCase("blank") || fromAddr.equalsIgnoreCase("default")) {
            fromAddr = null;
        }

        /* "To" */
        String toAddr = RTConfig.getString(ARG_TO,null);
        toAddr = RTConfig.insertKeyValues(toAddr);
        if (StringTools.isBlank(toAddr) || toAddr.endsWith(EXAMPLE_DOT_COM)) {
            Print.sysPrintln("Missing 'To' address");
            Print.sysPrintln("(specify '-to=<emailAddress>' option on command-line)");
            Print.sysPrintln("");
            System.exit(1);
        }

        /* Load Subject/Body/Attachment */
        EMailContent emailContent = new EMailContent();
        // -- "-load"
        if (RTConfig.hasProperty(ARG_LOAD)) {
            File file = RTConfig.getFile(ARG_LOAD,null);
            if (!emailContent.loadFromFile(file)) {
                Print.sysPrintln("ERROR: Unable to load EMailContent file: " + file);
                System.exit(1);
            }
        }
        // -- "-subject=" 
        if (RTConfig.hasProperty(ARG_SUBJECT)) {
            emailContent.setSubject(RTConfig.getString(ARG_SUBJECT,""));
        }
        // -- "-body="
        if (RTConfig.hasProperty(ARG_BODY)) {
            emailContent.setBody(RTConfig.getString(ARG_BODY,""));
        }
        // -- "-attach"
        if (RTConfig.hasProperty(ARG_ATTACH)) {
            File attachFile = RTConfig.getFile(ARG_ATTACH,null);
            if ((attachFile != null) && attachFile.isFile()) {
                InputStream fis = null;
                try {
                    fis = new FileInputStream(attachFile);
                    byte b[] = FileTools.readStream(fis); // IOException
                    String name = attachFile.getName();
                    String ext  = FileTools.getExtension(attachFile);
                    String type = HTMLTools.getMimeTypeFromExtension(ext,null); // CONTENT_TYPE_OCTET;
                    if (type == null) {
                        type = DefaultContentType(b,HTMLTools.CONTENT_TYPE_OCTET);
                    }
                    SendMail.Attachments attach = new SendMail.Attachments();
                    attach.addAttachment(new SendMail.Attachment(b,name,type));
                    emailContent.setAttachments(attach);
                } catch (Throwable th) {
                    Print.sysPrintln("ERROR: Unable to load attachment - " + th);
                } finally {
                    try { fis.close(); } catch (Throwable th) {/*ignore*/}
                }
            } else {
                Print.sysPrintln("ERROR: Specified Attachment does not exist - " + attachFile);
                System.exit(99);
            }
        }

        /* subject/body/attachment */
        String               subject = emailContent.getSubject(null);
        String               body    = emailContent.getBody(null);
        SendMail.Attachments attach  = emailContent.getAttachments();

        /* send email */
        Print.sysPrintln("Sending EMail:");
        Print.sysPrintln("   From   : " + fromAddr);
        Print.sysPrintln("   To     : " + toAddr);
        Print.sysPrintln("   Subject: " + subject);
        if ((attach != null) && attach.hasAttachments()) {
            for (SendMail.Attachment A : attach.getAttachments()) {
                if ((A == null) || (A.getSize() <= 0)) { continue; }
                Print.sysPrintln("   Attach : " + A.getName() + " ["+A.getSize()+" bytes]");
            }
        }
        Print.sysPrintln("   Body   : \n" + body);
        if (StringTools.indexOfIgnoreCase(toAddr,"localhost") >= 0) {
            Print.sysPrintln("... skipping email send");
        } else
        if (SendMail.send(fromAddr,toAddr,subject,body,attach,smtpProps,retrySend)) {
            Print.sysPrintln("... sent");
        } else {
            Print.sysPrintln("... Unable to send EMail");
        }
        Print.sysPrintln("");
        System.exit(0);

    }

}
