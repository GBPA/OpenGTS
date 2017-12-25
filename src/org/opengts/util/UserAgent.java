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
//  HTTP "user-agent" parser.
//  Currently only checks for specific keywoard to determine device type.
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Change History:
//  2016/09/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.net.*;

/**
*** User-Agent parser.<br>
*** Currently only used to check keywords to determine device type (desktop, tablet, mobile, etc)
**/

public class UserAgent
{

    // ------------------------------------------------------------------------

    /**
    *** Parsed Device Type
    **/
    public enum DeviceType {
        UNKNOWN,
        DESKTOP,
        TABLET,     // "iPad", "Tablet"
        PHONE;      // "iPhone", "iOS", "Android", "Windows Phone", "Windows Mobile", "Windows CE"
        public boolean isUnknown() { return this.equals(UNKNOWN); }
        public boolean isPhone()   { return this.equals(PHONE); }
        public boolean isTablet()  { return this.equals(TABLET); }
        public boolean isMobile()  { return this.isPhone() || this.isTablet(); }
        public boolean isDesktop() { return this.equals(DESKTOP) || this.isUnknown(); }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Some Examples:
    // -   Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36
    // -   Mozilla/5.0 (iPhone; CPU iPhone OS 7_1 like Mac OS X) AppleWebKit/537.47.1 (KHTML, like Gecko) CriOS/26.0.1472.13 Mobile/1AD362 Safari/6938.17
    // -   Mozilla/5.0 (iPad; CPU OS 5_1_1 like Mac OS X; es-es) AppleWebKit/535.32 (KHTML, like Gecko) CriOS/24.0.1562.72 Mobile/9B2E6 Safari/9843.31.1
    // -   Mozilla/5.0 (Linux; Android 4.1.1; GT-N7100 Build/JRO03C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.99 Mobile Safari/537.36
    // -   Mozilla/4.0 (compatible; MSIE 7.0; AOL 9.0; Windows NT 5.1; WebProducts; SpamBlocker 4.8.3)
    // -   Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; 7 Pro T5682)

    private String      userAgent   = "";
    private DeviceType  deviceType  = DeviceType.UNKNOWN;

    /**
    *** Constructor
    **/
    public UserAgent(String ua)
    {
        super();
        this.userAgent = StringTools.trim(ua);
        String uaLC = this.userAgent.toLowerCase();
        if (uaLC.indexOf("ipad") >= 0) {
            this.deviceType = DeviceType.TABLET;
        } else
        if (uaLC.indexOf("tablet") >= 0) {
            // -- "Android Tablet", "Windows Tablet"
            this.deviceType = DeviceType.TABLET;
        } else
        if (uaLC.indexOf("phone") >= 0) {
            // -- "iphone", "Windows Phone"
            this.deviceType = DeviceType.PHONE;
        } else
        if (uaLC.indexOf("mobile") >= 0) {
            this.deviceType = DeviceType.PHONE;
        } else
        if (uaLC.indexOf("android") >= 0) {
            this.deviceType = DeviceType.PHONE;
        } else {
            this.deviceType = DeviceType.DESKTOP;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the user-agent String
    **/
    public String getUserAgentString()
    {
        return this.userAgent;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the device type represented by this UserAgent
    **/
    public DeviceType getDeviceType()
    {
        return this.deviceType;
    }

    /**
    *** Returns true if this user-agent represents a phone
    **/
    public boolean isPhone()
    {
        return this.getDeviceType().isPhone();
    }

    /**
    *** Returns true if this user-agent represents a phone
    **/
    public boolean isTablet()
    {
        return this.getDeviceType().isTablet();
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a String representation of this instance
    **/
    public String toString()
    {
        return "[" + this.getDeviceType() + "] " + this.getUserAgentString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_USER_AGENT[] = { "user-agent", "userAgent", "ua" };

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        String userAgent = RTConfig.getString(ARG_USER_AGENT,null);
        if (!StringTools.isBlank(userAgent)) {
            UserAgent ua = new UserAgent(userAgent);
            Print.sysPrintln("User Agent : " + ua.getUserAgentString());
            Print.sysPrintln("Device Type: " + ua.getDeviceType());
        }
    }

}
