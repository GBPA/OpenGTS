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
//  2016/09/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.routing;

import java.util.*;

import org.opengts.util.*;

import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.Account;

public abstract class RouteProviderAdapter
    implements RouteProvider
{

    // ------------------------------------------------------------------------

    public static final String PROP_RouteProvider_          = "RouteProvider.";
    public static final String _PROP_isEnabled              = ".isEnabled";

    // ------------------------------------------------------------------------

    private String       name           = null;
    private TriState     isEnabled      = TriState.UNKNOWN;

    private String       accessKey      = null;
    private RTProperties properties     = null;

    /**
    *** Constructor
    *** @param name    The name of this RouteProvider
    *** @param key     The access key (may be null)
    *** @param rtProps The properties (may be null)
    **/
    public RouteProviderAdapter(String name, String key, RTProperties rtProps)
    {
        super();
        this.setName(name);
        this.setAuthorization(key);
        this.setProperties(rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this RouteProvider
    *** @param name  The name of this RouteProvider
    **/
    public void setName(String name)
    {
        this.name = (name != null)? name : "";
    }

    /**
    *** Gets the name of this RouteProvider
    *** @return  The name of this RouteProvider
    **/
    public String getName()
    {
        return (this.name != null)? this.name : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the authorization key of this RouteProvider
    *** @param key  The key of this RouteProvider
    **/
    public void setAuthorization(String key)
    {
        this.accessKey = key;
    }
    
    /**
    *** Gets the authorization key of this RouteProvider
    *** @return The access key of this RouteProvider
    **/
    public String getAuthorization()
    {
        return this.accessKey;
    }

    // ------------------------------------------------------------------------

    /**
    *** Parse and return the user name and password
    *** @return The username and password (always a 2 element array)
    **/
    protected String[] _getUserPass()
    {
        String username = null;
        String password = null;
        String key = this.getAuthorization();
        if ((key != null) && !key.equals("")) {
            int p = key.indexOf(":");
            if (p >= 0) {
                username = key.substring(0,p);
                password = key.substring(p+1);
            } else {
                username = key;
                password = "";
            }
        } else {
            username = null;
            password = null;
        }
        return new String[] { username, password };
    }

    /** 
    *** Return authorization username.  This assumes that the username and password are
    *** separated by a ':' character
    *** @return The username
    **/
    protected String getUsername()
    {
        return this._getUserPass()[0];
    }

    /** 
    *** Return authorization password.  This assumes that the username and password are
    *** separated by a ':' character
    *** @return The password
    **/
    protected String getPassword()
    {
        return this._getUserPass()[1];
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the properties for this RouteProvider
    *** @param rtProps  The properties for this RouteProvider
    **/
    public void setProperties(RTProperties rtProps)
    {
        this.properties = rtProps;
    }

    /**
    *** Gets the properties for this RouteProvider
    *** @return The properties for this RouteProvider
    **/
    public RTProperties getProperties()
    {
        if (this.properties == null) {
            this.properties = new RTProperties();
        }
        return this.properties;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this RouteProvider is enabled
    *** @return True if enabled
    **/
    public boolean isEnabled()
    {
        if (this.isEnabled.isUnknown()) {
            String key = PROP_RouteProvider_ + this.getName() + _PROP_isEnabled;
            if (RTConfig.getBoolean(key,true)) {
                this.isEnabled = TriState.TRUE;
            } else {
                this.isEnabled = TriState.FALSE;
                Print.logWarn("RouteProvider disabled: " + this.getName());
            }
        }
        //Print.logInfo("Checking RGP 'isEnabled': " + this.getName() + " ==> " + this.isEnabled.isTrue());
        return this.isEnabled.isTrue();
    }

    // ------------------------------------------------------------------------

    /* RouteProvider interface methods */
    public abstract boolean isFastOperation();
    public abstract GeoRoute getDirections(String fromAddr, String toAddr, Locale locale, RTProperties attr);
    public abstract GeoRoute getSnapToRoad(GeoPointProvider path[], Locale locale, RTProperties attr);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]   = new String[] { "account"          , "acct"         };
    private static final String ARG_PLN[]       = new String[] { "privateLabelName" , "pln"   , "pl" };
    private static final String ARG_FR_ADDR[]   = new String[] { "fromAddr"         , "frAddr", "fa" };
    private static final String ARG_TO_ADDR[]   = new String[] { "toAddr"           , "toAddr", "ta" };

    private static void usage()
    {
        String n = RouteProvider.class.getName();
        Print.sysPrintln("");
        Print.sysPrintln("Description:");
        Print.sysPrintln("   Routing Testing Tool ...");
        Print.sysPrintln("");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("   java ... " + n + " -address=<addr> -account=<id>");
        Print.sysPrintln(" or");
        Print.sysPrintln("   java ... " + n + " -address=<addr> -pln=<name>");
        Print.sysPrintln("");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("   -fromAddr=<addr>   From Address");
        Print.sysPrintln("   -toAddr=<addr>     To Address");
        Print.sysPrintln("   -account=<id>      Account ID from which to obtain the RouteProvider");
        Print.sysPrintln("   -pln=<name>        PrivateLabel name/host");
        Print.sysPrintln("");
        System.exit(1);
    }

    public static void _main()
    {

        /* Address */
        String frAddr = RTConfig.getString(ARG_FR_ADDR,"").replace('_',' ');
        String toAddr = RTConfig.getString(ARG_TO_ADDR,"").replace('_',' ');
        if (StringTools.isBlank(frAddr) || StringTools.isBlank(toAddr)) {
            Print.sysPrintln("ERROR: From/To Address not specified");
            usage();
        }
        Print.logInfo("FROM Address = " + frAddr);
        Print.logInfo("TO   Address = " + toAddr);

        /* get PrivateLabel */
        BasicPrivateLabel privLabel = null;
        String accountID = RTConfig.getString(ARG_ACCOUNT, "");
        if (!StringTools.isBlank(accountID)) {
            Account acct = null;
            try {
                acct = Account.getAccount(accountID); // may throw DBException
                if (acct == null) {
                    Print.sysPrintln("ERROR: Account-ID does not exist: " + accountID);
                    usage();
                }
                privLabel = acct.getPrivateLabel();
            } catch (DBException dbe) {
                Print.logException("Error loading Account: " + accountID, dbe);
                //dbe.printException();
                System.exit(99);
            }
        } else {
            String pln = RTConfig.getString(ARG_PLN,"default");
            if (StringTools.isBlank(pln)) {
                Print.sysPrintln("ERROR: Must specify '-account=<Account>'");
                usage();
            } else {
                privLabel = BasicPrivateLabelLoader.getPrivateLabel(pln);
                if (privLabel == null) {
                    Print.sysPrintln("ERROR: PrivateLabel name not found: %s", pln);
                    usage();
                }
            }
        }

        /* get RouteProvider */
        RouteProvider rp = privLabel.getRouteProvider();
        if (rp == null) {
            Print.sysPrintln("ERROR: No RouteProvider for PrivateLabel: %s", privLabel.getName());
            System.exit(99);
        } else
        if (!rp.isEnabled()) {
            Print.sysPrintln("WARNING: RouteProvider disabled: " + rp.getName());
            System.exit(0);
        }

        /* routing attributes */
        RTProperties attr = null;

        /* get Route */
        try {
            // -- make sure the Domain properties are available to RTConfig
            privLabel.pushRTProperties();   // stack properties (may be redundant in servlet environment)
            long startMS = DateTime.getCurrentTimeMillis();
            GeoRoute gr = rp.getDirections(frAddr, toAddr, privLabel.getLocale(), attr); // get the route
            long deltaMS = DateTime.getCurrentTimeMillis() - startMS;
            if (gr != null) {
                Print.logInfo("");
                Print.logInfo("Route: ["+deltaMS+" ms]");
                //Print.sysPrintln(gr.toString());
                gr.printRoute();
                Print.logInfo("");
            } else {
                Print.logInfo("ERROR: Unable to obtain Route for specified addresses");
            }
        } catch (Throwable th) {
            // -- ignore
        } finally {
            privLabel.popRTProperties();    // remove from stack
        }

    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        RouteProviderAdapter._main();
    }

}
