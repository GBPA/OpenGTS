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
//  SMS Gateway properties
// ----------------------------------------------------------------------------
// Change History:
//  2016/12/21  Martin D. Flynn
//     -Initial release
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
*** SMS gateway properties
**/

public class SMSProperties
{

    // ------------------------------------------------------------------------

    public  static final int    DEFAULT_MAX_MESSAGE_LENGTH          = 160;

    // ------------------------------------------------------------------------

    /* SMS property key name prefix */
    public  static final String PROP_SmsGatewayHandler_             = SMSOutboundGateway.PROP_SmsGatewayHandler_;
    // --
    public  static final String _PROP_gatewayName                   = "gatewayName";
    public  static final String _PROP_debug                         = "debug";
    // --
    public  static final String _PROP_maxMessageLength              = "maxMessageLength";

    /**
    *** Return array of possible property keys for specified GW name and key
    **/
    private String[] PropKeys(String gwName, String key)
    {
        if (StringTools.isBlank(key)) {
            return new String[0];
        } else
        if (StringTools.isBlank(gwName)) {
            return new String[] {
                PROP_SmsGatewayHandler_ + key,                // SmsGatewayHandle.maxMessageLength
                key                                           // maxMessageLength
            };
        } else {
            return new String[] {
                PROP_SmsGatewayHandler_ + gwName + "." + key, // SmsGatewayHandle.httpURL.maxMessageLength
                PROP_SmsGatewayHandler_ + key,                // SmsGatewayHandle.maxMessageLength
                key                                           // maxMessageLength
            };
        }
    }

    /**
    *** Returns the abbreviated/short key for the specified key.
    *** Removes prefixing SmsGatewayHandler.[GATEWAY.]
    **/
    private static String ShortKey(String gwName, String smsKey) 
    {
        // -- remove prefixing "SmsGatewayHandler."
        if (smsKey.startsWith(PROP_SmsGatewayHandler_)) {
            smsKey = smsKey.substring(PROP_SmsGatewayHandler_.length());
        }
        // -- remove prefixing "GATEWAY."
        if ((gwName != null) && smsKey.startsWith(gwName+".")) {
            smsKey = smsKey.substring((gwName+".").length());
        }
        // -- return
        return smsKey;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the SMSProperties
    **/
    public static SMSProperties getSmsProperties(Account account)
    {
        if (account != null) {
            return account.getSmsProperties(null);
        } else {
            return new SMSProperties();
        }
    }

    /**
    *** Gets the SMSProperties
    **/
    public static SMSProperties getSmsProperties(Device device)
    {
        if (device != null) {
            return SMSProperties.getSmsProperties(device.getAccount());
        } else {
            return new SMSProperties();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String          name            = "";

    private RTProperties    smsProps        = null;
    private SMSProperties   smsDelegate     = null;

    /**
    *** Constructor (default values)
    *** @param name  The name of this instance (for debugging purposes)
    **/
    public SMSProperties() 
    {
        this("Default");
    }

    /**
    *** Constructor (default values)
    *** @param name  The name of this instance (for debugging purposes)
    **/
    public SMSProperties(String name) 
    {
        super();
        this.setName(name);
        this.smsProps = new RTProperties();
    }

    /**
    *** Constructor
    *** @param name     The name of this instance (for debugging purposes)
    *** @param smsRTP   RTProperties instance from which the SMS properties are copied
    **/
    public SMSProperties(String name, RTProperties smsRTP) 
    {
        this(name);
        this.smsProps.setProperties(smsRTP,false);
    }

    /**
    *** Constructor
    *** @param name     The name of this instance (for debugging purposes)
    *** @param smsRTP   RTProperties instance from which the SMS properties are copied
    **/
    public SMSProperties(SMSProperties sms) 
    {
        this((sms != null)? (sms.getName()+"(Copy)") : "null(Copy)");
        if (sms != null) {
            this.smsProps.setProperties(sms.smsProps,false); // copy
            this.setDelegate(sms.smsDelegate);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a deep copy of this instance
    *** @param userFilter  True to copy only user-level SMS properties
    *** @return A copy of this instance
    **/
    public SMSProperties copy()
    {
        return new SMSProperties(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this SMSProperties is empty
    **/
    public boolean isEmpty()
    {
        if (!this.smsProps.isEmpty()) {
            return false;
        } else
        if (this.smsDelegate != null) {
            return this.smsDelegate.isEmpty();
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this SMSProperties instance (for debugging purposes)
    **/
    public SMSProperties setName(String name)
    {
        this.name = StringTools.trim(name);
        return this; // for chaining
    }

    /**
    *** Gets the name of this SMSProperties instance (for debugging purposes)
    **/
    public String getName()
    {
        return this.name;
    }

    // ------------------------------------------------------------------------

    /**
    *** The delegate SMSProperties instance to check if this instance does not
    *** contain the requested property value
    *** @param sms  The delegate SMSProperties instance
    **/
    public SMSProperties setDelegate(SMSProperties sms) 
    {
        this.smsDelegate = sms;
        return this; // for chaining
    }

    /**
    *** Gets the specified property from this instance from the delegate
    *** @param keys  The key of the property value to return 
    ***              (first element is the full property name, the second element is the abbreviation)
    *** @return The property value
    **/
    private String _getDelegateProperty(String keys[], String dft) 
    {
        if ((keys == null) || (keys.length <= 0)) {
            // -- no keys?
            return null;
        } else
        if (this.smsDelegate != null) {
            // -- delegate property
            return this.smsDelegate._getDelegateProperty(keys, dft);
        } else 
        if (keys[0].startsWith(PROP_SmsGatewayHandler_)) {
            // -- system property
            return RTConfig.getString(keys[0], dft); // "SmsGatewayHandler." only
        } else {
            // -- first key is not "SmsGatewayHandler." property
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the gateway name property 
    **/
    public void setGatewayName(String gwName) 
    {
        String key = _PROP_gatewayName;
        this.smsProps.removeProperties(PropKeys(null,key));
        if (!StringTools.isBlank(gwName)) {
            this.smsProps.setString(key, StringTools.trim(gwName));
        }
    }

    /**
    *** Gets the gateway name property 
    **/
    public String getGatewayName() 
    {
        String key = _PROP_gatewayName;
        String keys[] = PropKeys(null,key);
        if (this.smsProps.hasProperty(keys)) {
            return this.smsProps.getString(keys, "unknown");
        } else {
            return this._getDelegateProperty(keys, "unknown");
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the gateway name property 
    **/
    public void setProperty(String _key, String val) 
    {
        if (!StringTools.isBlank(_key)) {
            String gwName = this.getGatewayName();
            String key    = ShortKey(gwName,_key);
            this.smsProps.removeProperties(PropKeys(gwName,key));
            if (!StringTools.isBlank(val)) {
                this.smsProps.setString(key, StringTools.trim(val));
            }
        }
    }

    /**
    *** Gets the gateway name property 
    **/
    public String getProperty(String _key, String dft) 
    {
        if (!StringTools.isBlank(_key)) {
            String gwName = this.getGatewayName();
            String key    = ShortKey(gwName,_key);
            String keys[] = PropKeys(gwName,key);
            if (this.smsProps.hasProperty(keys)) {
                return this.smsProps.getString(keys, dft);
            } else {
                return this._getDelegateProperty(keys, dft);
            }
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the debug property 
    **/
    public void setDebug(boolean V) 
    {
        this.setProperty(_PROP_debug,(V?"true":"false"));
    }

    /**
    *** Gets the debug property 
    **/
    public boolean getDebug() 
    {
        return StringTools.parseBoolean(this.getProperty(_PROP_debug,null),false);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum message length
    **/
    public void setMaximumMessageLength(int maxLen) 
    {
        this.setProperty(_PROP_maxMessageLength, ((maxLen > 0)? String.valueOf(maxLen) : null));
    }

    /**
    *** Gets the maximum message length
    **/
    public int getMaximumMessageLength() 
    {
        return StringTools.parseInt(this.getProperty(_PROP_maxMessageLength,null),DEFAULT_MAX_MESSAGE_LENGTH);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the RTProperties instance
    **/
    public RTProperties getProperties()
    {
        if (!this.smsProps.isEmpty()) {
            return this.smsProps;
        } else
        if (this.smsDelegate != null) {
            return this.smsDelegate.getProperties();
        } else {
            return this.smsProps; // empty
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    **/
    public String toString()
    {
        return this.smsProps.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** (Debug Purposes) Print the contents of this instance to stdout
    **/
    public void printProperties(String msg)
    {
        this.smsProps.printProperties(msg);
    }

    // ------------------------------------------------------------------------

}
