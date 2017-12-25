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
//  SMTP properties container
// ----------------------------------------------------------------------------
// Change History:
//  2017/03/14  Martin D. Flynn
//     -Initial release (extracted from SendMail.java)
//  2017/10/09  Martin D. Flynn
//     -Added support for KEY_SEND_PARTIAL
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

/**
*** SMTP properties container
**/

public class SmtpProperties
{

    // ------------------------------------------------------------------------

    /* SMTP property key name prefix */
    private static final String SMTP_           = "smtp.";

    private static String Abbr(String smtpKey) 
    {
        if (smtpKey.startsWith(SMTP_)) {
            return smtpKey.substring(SMTP_.length());
        } else {
            return smtpKey;
        }
    }

    /* SMTP property keys */
    // -- Element size must be at least 2:
    // -   0  : Contains the full property key name
    // -   1  : Contains an abbreviated property key name, without the prefixing "smtp."
    // -   2+ : [optional] contains additional abbreviated property key names
    // -- Property key abbreviations are only used when reading SMTP properties defined by an account/user
    public static final String KEY_DEBUG[]              = { RTKey.SMTP_DEBUG             , Abbr(RTKey.SMTP_DEBUG             )                  };
    public static final String KEY_SERVER_HOST[]        = { RTKey.SMTP_SERVER_HOST       , Abbr(RTKey.SMTP_SERVER_HOST       )                  };
    public static final String KEY_SERVER_PORT[]        = { RTKey.SMTP_SERVER_PORT       , Abbr(RTKey.SMTP_SERVER_PORT       )                  };
    public static final String KEY_SERVER_USER[]        = { RTKey.SMTP_SERVER_USER       , Abbr(RTKey.SMTP_SERVER_USER       )                  };
    public static final String KEY_SERVER_USER_EMAIL[]  = { RTKey.SMTP_SERVER_USER_EMAIL , Abbr(RTKey.SMTP_SERVER_USER_EMAIL ), "user.email"    };
    public static final String KEY_SERVER_PASSWORD[]    = { RTKey.SMTP_SERVER_PASSWORD   , Abbr(RTKey.SMTP_SERVER_PASSWORD   ), "pass"          };
    public static final String KEY_ENABLE_SSL[]         = { RTKey.SMTP_ENABLE_SSL        , Abbr(RTKey.SMTP_ENABLE_SSL        ), "SSL"           };
    public static final String KEY_ENABLE_TLS[]         = { RTKey.SMTP_ENABLE_TLS        , Abbr(RTKey.SMTP_ENABLE_TLS        ), "TLS"           };
    public static final String KEY_SERVER_TIMEOUT_MS[]  = { RTKey.SMTP_SERVER_TIMEOUT_MS , Abbr(RTKey.SMTP_SERVER_TIMEOUT_MS )                  };
    public static final String KEY_SERVER_RETRY_COUNT[] = { RTKey.SMTP_SERVER_RETRY_COUNT, Abbr(RTKey.SMTP_SERVER_RETRY_COUNT), "retry"         };
    public static final String KEY_SEND_PARTIAL[]       = { RTKey.SMTP_SEND_PARTIAL      , Abbr(RTKey.SMTP_SEND_PARTIAL      )                  };
    public static final String KEY_MULTIPART_TYPE[]     = { RTKey.SMTP_MULTIPART_TYPE    , Abbr(RTKey.SMTP_MULTIPART_TYPE    ), "multipart"     };
    public static final String KEY_BCC_EMAIL[]          = { RTKey.SMTP_BCC_EMAIL         , Abbr(RTKey.SMTP_BCC_EMAIL         ), "bcc.email"     };
    public static final String KEY_SYSADMIN_EMAIL[]     = { RTKey.SMTP_SYSADMIN_EMAIL    , Abbr(RTKey.SMTP_SYSADMIN_EMAIL    ), "sysadmin.email"};
    public static final String KEY_THREAD_MODEL[]       = { RTKey.SMTP_THREAD_MODEL      , Abbr(RTKey.SMTP_THREAD_MODEL      )                  };
    public static final String KEY_THREAD_MODEL_SHOW[]  = { RTKey.SMTP_THREAD_MODEL_SHOW , Abbr(RTKey.SMTP_THREAD_MODEL_SHOW )                  };
    public static final String KEY_IGNORED_EMAIL_FILE[] = { RTKey.SMTP_IGNORED_EMAIL_FILE, Abbr(RTKey.SMTP_IGNORED_EMAIL_FILE)                  };

    /**
    *** Contains all SMTP properties
    **/
    public static final String KEY_PROPERTIES[][] = {
        KEY_DEBUG                ,
        KEY_SERVER_HOST          ,
        KEY_SERVER_PORT          ,
        KEY_SERVER_USER          ,
        KEY_SERVER_USER_EMAIL    ,
        KEY_SERVER_PASSWORD      ,
        KEY_ENABLE_SSL           ,
        KEY_ENABLE_TLS           ,
        KEY_SERVER_TIMEOUT_MS    ,
        KEY_SERVER_RETRY_COUNT   ,
        KEY_SEND_PARTIAL         ,
        KEY_MULTIPART_TYPE       ,
        KEY_BCC_EMAIL            , // may not be supported
        KEY_SYSADMIN_EMAIL       ,
        KEY_THREAD_MODEL         ,
        KEY_THREAD_MODEL_SHOW    ,
        KEY_IGNORED_EMAIL_FILE   ,
    };

    /**
    *** Contains only User configurable SMTP properties
    **/
    public static final String KEY_USER_PROPERTIES[][] = {
        KEY_DEBUG                ,
        KEY_SERVER_HOST          ,
        KEY_SERVER_PORT          ,
        KEY_SERVER_USER          ,
        KEY_SERVER_USER_EMAIL    ,
        KEY_SERVER_PASSWORD      ,
        KEY_ENABLE_SSL           ,
        KEY_ENABLE_TLS           ,
        KEY_SERVER_TIMEOUT_MS    ,
        KEY_SERVER_RETRY_COUNT   ,
        KEY_SEND_PARTIAL         ,
        KEY_MULTIPART_TYPE       ,
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SMTP Properties:
    //  smtp.debug=[true|false]                 SMTP_DEBUG
    //  smtp.host=[HOSTNAME]                    SMTP_SERVER_HOST
    //  smtp.port=[1..65535]                    SMTP_SERVER_PORT
    //  smtp.user=[USERNAME]                    SMTP_SERVER_USER
    //  smtp.user.emailAddress=[USER@DOMAIN]    SMTP_SERVER_USER_EMAIL
    //  smtp.password=[PASSWORD]                SMTP_SERVER_PASSWORD
    //  smtp.enableSSL=[true|false|only]        SMTP_ENABLE_SSL
    //  smtp.enableTLS=[true|false|only]        SMTP_ENABLE_TLS
    //  smtp.timeout=[milliseconds]             SMTP_SERVER_TIMEOUT_MS
    //  smtp.retryCount=[retryCount]            SMTP_SERVER_RETRY_COUNT
    //  smtp.multipartType=[multipartType]      SMTP_MULTIPART_TYPE
    // ------------------------------------------------------------------------

    private String          name         = "";
    private RTProperties    smtpProps    = null;
    private SmtpProperties  smtpDelegate = null;

    /**
    *** Constructor (default values)
    *** @param name  The name of this instance (for debugging purposes)
    **/
    public SmtpProperties(String name) 
    {
        super();
        this.name      = StringTools.trim(name);
        this.smtpProps = new RTProperties();
    }

    /**
    *** Constructor
    *** @param name     The name of this instance (for debugging purposes)
    *** @param smtpRTP  RTProperties instance from which the SMTP properties are copied
    *** @param userFilter  True to copy only user-level SMTP properties
    **/
    public SmtpProperties(String name, RTProperties smtpRTP, boolean userFilter) 
    {
        this(name);
        if (smtpRTP != null) {
            String smtpKeys[][] = userFilter? KEY_USER_PROPERTIES : KEY_PROPERTIES;
            for (String K[] : smtpKeys) {
                String V = smtpRTP.getString(K, null);
                if (V != null) {
                    this.smtpProps.setString(K[0], V); // "smtp."
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a deep copy of this instance
    *** @param userFilter  True to copy only user-level SMTP properties
    *** @return A copy of this instance
    **/
    public SmtpProperties copy(boolean userFilter)
    {
        String name = this.getName()+"(Copy)";
        return new SmtpProperties(name, this.smtpProps, userFilter);
    }

    // ------------------------------------------------------------------------

    /**
    *** The delegate SmtpProperties instance to check if this instance does not
    *** contain the requested property value
    *** @param smtp  The delegate SmtpProperties instance
    **/
    public SmtpProperties setDelegate(SmtpProperties smtp) 
    {
        this.smtpDelegate = smtp;
        return this; // for chaining
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this SMtpProperties instance (for debugging purposes)
    **/
    public SmtpProperties setName(String name)
    {
        this.name = StringTools.trim(name);
        return this; // for chaining
    }

    /**
    *** Gets the name of this SMtpProperties instance (for debugging purposes)
    **/
    public String getName()
    {
        return this.name;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the specified property from this instance
    *** @param keys  The key of the property value to return 
    ***              (first element is the full property name, the second element is the abbreviation)
    *** @return The property value
    **/
    private String _getString(String keys[], boolean delegate) 
    {

        /* no key, no value */
        if ((keys == null) || (keys.length <= 0)) {
            // -- no keys?
            return null;
        }

        /* check local properties */
        if (this.smtpProps.hasProperty(keys)) {
            // -- "host" is defined and has local property
            return this.smtpProps.getString(keys,null);
        }

        /* ok to delegate? */
        if (!delegate) {
            // -- "host" is defined, but local property not defined, and not ok to delegate
            return null;
        }

        /* delegate */
        if (this.smtpDelegate != null) {
            // -- delegate property
            return this.smtpDelegate._getString(keys, delegate);
        } else 
        if (keys[0].startsWith(SMTP_)) {
            // -- system property
            return RTConfig.getString(keys[0]); // "smtp." only
        } else {
            // -- first key is not "smtp." property
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the debug property 
    **/
    public void setDebug(boolean V) 
    {
        this.smtpProps.removeProperties(KEY_DEBUG);
        if (V) {
            this.smtpProps.setBoolean(KEY_DEBUG[0], V);
        }
    }

    /**
    *** Gets the debug property 
    **/
    public boolean getDebug() 
    {
        boolean delegate = true; // delegate ok
        return StringTools.parseBoolean(this._getString(KEY_DEBUG,delegate),false);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this SmtpProperties instance defines a "host".
    *** (Some properties should not allow checking the delegate SmtpProperties
    *** instance if this instance has a defined host)
    **/
    public boolean hasHost()
    {
        String host = this.smtpProps.getString(KEY_SERVER_HOST,null);
        return !StringTools.isBlank(host)? true : false;
    }

    /**
    *** Sets the SMTP host
    **/
    public void setHost(String V)
    {
        this.smtpProps.removeProperties(KEY_SERVER_HOST);
        if (!StringTools.isBlank(V)) {
            this.smtpProps.setString(KEY_SERVER_HOST[0], StringTools.trim(V));
        }
    }

    /**
    *** Gets the SMTP host
    **/
    public String getHost()
    {
        boolean delegate = !this.hasHost(); // delegate iff host not defined
        String V = StringTools.trim(this._getString(KEY_SERVER_HOST,delegate));
        return !StringTools.endsWithIgnoreCase(V,SendMail.EXAMPLE_DOT_COM)? V : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the SMTP port
    **/
    public void setPort(int V)
    {
        this.smtpProps.removeProperties(KEY_SERVER_PORT);
        if ((V > 0) && (V <= 65535)) {
            this.smtpProps.setInt(KEY_SERVER_PORT[0], V);
        }
    }

    /**
    *** Gets the SMTP host
    **/
    public int getPort()
    {
        boolean delegate = !this.hasHost(); // delegate iff host not defined
        return StringTools.parseInt(this._getString(KEY_SERVER_PORT,delegate),25);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the SMTP user
    **/
    public void setUser(String V)
    {
        this.smtpProps.removeProperties(KEY_SERVER_USER);
        if (!StringTools.isBlank(V)) {
            this.smtpProps.setString(KEY_SERVER_USER[0], StringTools.trim(V));
        }
    }

    /**
    *** Gets the SMTP host
    **/
    public String getUser()
    {
        boolean delegate = !this.hasHost(); // delegate iff host not defined
        return StringTools.trim(this._getString(KEY_SERVER_USER,delegate));
    }

    // ------------------------------------------------------------------------

    private static final String SENDPARTIAL_OPTIONS[] = { "true", "false" };

    /**
    *** Sets the default SMTP 'sendpartial' flag
    **/
    public void setSendPartial(boolean V)
    {
        this.setSendPartial(V?"true":"false");
    }

    /**
    *** Sets the default SMTP 'sendpartial' flag
    *** Valid values include "false" | "true" | ""
    **/
    public void setSendPartial(String V)
    { // [2.6.5-B16]
        this.smtpProps.removeProperties(KEY_SEND_PARTIAL);
        V = StringTools.trim(V).toLowerCase();
        if (ListTools.contains(SENDPARTIAL_OPTIONS,V)) {
            this.smtpProps.setString(KEY_SEND_PARTIAL[0], V);
        }
    }

    /**
    *** Sets the default SMTP 'sendpartial' flag
    **/
    public String getSendPartial()
    { // [2.6.5-B16]
        boolean delegate = true; // delegate ok
        return StringTools.trim(this._getString(KEY_SEND_PARTIAL,delegate));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default SMTP multipart MIME type
    **/
    public void setMultipartType(String V)
    { // [2.6.2-B52]
        this.smtpProps.removeProperties(KEY_MULTIPART_TYPE);
        if (!StringTools.isBlank(V)) {
            this.smtpProps.setString(KEY_MULTIPART_TYPE[0], StringTools.trim(V));
        }
    }

    /**
    *** Gets the default SMTP multipart MIME type
    **/
    public String getMultipartType()
    { // [2.6.2-B52]
        // -- Multipart MIME Types:
        // -    SendMail.MULTIPART_MIXED       = "mixed";       // multipart/mixed
        // -    SendMail.MULTIPART_ALTERNATIVE = "alternative"; // multipart/alternative
        // -    SendMail.MULTIPART_RELATED     = "related";     // multipart/related
        // -    SendMail.MULTIPART_REPORT      = "report";      // multipart/report
        boolean delegate = true; // delegate ok
        return StringTools.trim(this._getString(KEY_MULTIPART_TYPE,delegate));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the SMTP user email address
    **/
    public void setUserEmail(String V)
    {
        this.smtpProps.removeProperties(KEY_SERVER_USER_EMAIL);
        if (!StringTools.isBlank(V) && (V.indexOf("@") > 0)) {
            this.smtpProps.setString(KEY_SERVER_USER_EMAIL[0], StringTools.trim(V));
        }
    }

    /**
    *** Gets the SMTP user email address
    **/
    public String getUserEmail()
    {
        boolean delegate = !this.hasHost(); // delegate iff host not defined
        String V = StringTools.trim(this._getString(KEY_SERVER_USER_EMAIL,delegate));
        return !SendMail.IsBlankEmailAddress(V)? V : "";
    }

    // --------------------------------

    /**
    *** Gets the SMTP "From" email address
    *** @param type Reserved for future use
    *** @return The SMTP "From" email address
    **/
    public String getFromEmailType(String type)
    {
        return this.getUserEmail(); // may be blank (error to be handled by caller)
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the SMTP password
    **/
    public void setPassword(String V)
    {
        this.smtpProps.removeProperties(KEY_SERVER_PASSWORD);
        if (V != null) {
            this.smtpProps.setString(KEY_SERVER_PASSWORD[0], V); // do not trim password
        }
    }

    /**
    *** Sets the SMTP password
    **/
    public String getPassword()
    {
        boolean delegate = !this.hasHost(); // delegate iff host not defined
        String V = this._getString(KEY_SERVER_PASSWORD,delegate);
        return (V != null)? V : ""; // do not trim password
    }

    // ------------------------------------------------------------------------

    private static final String SSL_OPTIONS[] = { "true", "only" }; // omit "false"

    /**
    *** Sets the SMTP SSL enabled state.
    **/
    public void setEnableSSL(boolean V)
    {
        this.setEnableSSL(V?"true":"false");
    }

    /**
    *** Sets the SMTP SSL enabled state.
    *** Valid values include "false" | "true" | "only"
    **/
    public void setEnableSSL(String V)
    {
        this.smtpProps.removeProperties(KEY_ENABLE_SSL);
        V = StringTools.trim(V).toLowerCase();
        if (ListTools.contains(SSL_OPTIONS,V)) {
            this.smtpProps.setString(KEY_ENABLE_SSL[0], V);
        }
    }

    /**
    *** Gets the SMTP SSL enabled state.
    *** false | true | only
    **/
    public String getEnableSSL()
    {
        boolean delegate = !this.hasHost(); // delegate iff host not defined
        return StringTools.trim(this._getString(KEY_ENABLE_SSL,delegate));
    }

    // ------------------------------------------------------------------------

    private static final String TLS_OPTIONS[] = { "true", "only" }; // omit "false"

    /**
    *** Sets the SMTP SSL enabled state.
    **/
    public void setEnableTLS(boolean V)
    {
        this.setEnableTLS(V?"true":"false");
    }

    /**
    *** Sets the SMTP TLS enabled state.
    *** Valid values include "false" | "true" | "only"
    **/
    public void setEnableTLS(String V)
    {
        this.smtpProps.removeProperties(KEY_ENABLE_TLS);
        V = StringTools.trim(V).toLowerCase();
        if (ListTools.contains(TLS_OPTIONS,V)) {
            this.smtpProps.setString(KEY_ENABLE_TLS[0], V);
        }
    }

    /**
    *** Gets the SMTP TLS enabled state.
    *** false | true | only
    **/
    public String getEnableTLS()
    {
        boolean delegate = !this.hasHost(); // delegate iff host not defined
        return StringTools.trim(this._getString(KEY_ENABLE_TLS,delegate));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the SMTP timeout in milliseconds
    **/
    public void setTimeoutMS(int V)
    {
        this.smtpProps.removeProperties(KEY_SERVER_TIMEOUT_MS);
        if (V > 0) {
            this.smtpProps.setInt(KEY_SERVER_TIMEOUT_MS[0], V);
        }
    }

    /**
    *** Gets the SMTP timeout in milliseconds
    **/
    public int getTimeoutMS()
    {
        boolean delegate = true; // delegate ok
        return StringTools.parseInt(this._getString(KEY_SERVER_TIMEOUT_MS,delegate),30000);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the SMTP retry count
    **/
    public void setRetryCount(int V)
    {
        this.smtpProps.removeProperties(KEY_SERVER_RETRY_COUNT);
        if ((V > 0) && (V < 10)) {
            this.smtpProps.setInt(KEY_SERVER_RETRY_COUNT[0], V);
        }
    }

    /**
    *** Gets the SMTP retry count
    **/
    public int getRetryCount()
    {
        boolean delegate = true; // delegate ok
        return Math.max(StringTools.parseInt(this._getString(KEY_SERVER_RETRY_COUNT,delegate),0),0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the SMTP BCC property
    **/
    public void setBccEmail(String V)
    {
        this.smtpProps.removeProperties(KEY_BCC_EMAIL);
        if (!StringTools.isBlank(V) && (V.indexOf("@") > 0)) {
            this.smtpProps.setString(KEY_BCC_EMAIL[0], StringTools.trim(V));
        }
    }

    /**
    *** Gets the SMTP BCC property
    **/
    public String getBccEmail()
    {
        boolean delegate = !this.hasHost(); // delegate iff host not defined
        String V = StringTools.trim(this._getString(KEY_BCC_EMAIL,delegate));
        return !SendMail.IsBlankEmailAddress(V)? V : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Sysadmin email address
    **/
    public void setSysadminEmail(String V)
    {
        this.smtpProps.removeProperties(KEY_SYSADMIN_EMAIL);
        if (!StringTools.isBlank(V) && (V.indexOf("@") > 0)) {
            this.smtpProps.setString(KEY_SYSADMIN_EMAIL[0], StringTools.trim(V));
        }
    }

    /**
    *** Gets the Sysadmin email address
    **/
    public String getSysadminEmail()
    {
        boolean delegate = true; // delegate ok
        String V = StringTools.trim(this._getString(KEY_SYSADMIN_EMAIL,delegate));
        return !SendMail.IsBlankEmailAddress(V)? V : "";
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified value is equal to this instance
    **/
    public boolean equals(Object other)
    {
        if (!(other instanceof SmtpProperties)) {
            return false;
        }
        return this.smtpProps.equals(((SmtpProperties)other).smtpProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    **/
    public String toString()
    {
        return this.smtpProps.toString();
    }

    /**
    *** Returns a String representation of this instance
    *** @param abbrev  True to return a String containing the abbriated property names
    **/
    public String toString(boolean abbrev)
    {
        if (abbrev) {
            RTProperties rtp = new RTProperties();
            for (String K[] : KEY_PROPERTIES) {
                String V = this.smtpProps.getString(K, null);
                if (V != null) {
                    rtp.setString(K[K.length - 1], V);
                }
            }
            return rtp.toString();
        } else {
            return this.smtpProps.toString();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** (Debug Purposes) Print the contents of this instance to stdout
    **/
    public void printProperties(String msg)
    {
        this.smtpProps.printProperties(msg);
    }

    // ------------------------------------------------------------------------

}
