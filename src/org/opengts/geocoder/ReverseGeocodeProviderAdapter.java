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
//  2008/05/14  Martin D. Flynn
//     -Initial release
//  2009/05/24  Martin D. Flynn
//     -Added command-line Reverse-Geocoder test.
//  2012/05/27  Martin D. Flynn
//     -Updated failover support
//  2013/08/06  Martin D. Flynn
//     -Added ability for subclass to specify a failover timeout value.
//  2016/01/04  Martin D. Flynn
//     -Added "failoverQuiet" hint [2.6.1-B03]
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import java.util.*;

import org.opengts.util.*;

import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.Account;
import org.opengts.db.tables.EventData;

public abstract class ReverseGeocodeProviderAdapter
    implements ReverseGeocodeProvider
{

    // ------------------------------------------------------------------------

    public static final String PROP_ReverseGeocodeProvider_ = "ReverseGeocodeProvider.";
    public static final String _PROP_isEnabled              = ".isEnabled";

    public static final String PROP_alwaysFast[]            = new String[] { "alwaysFast", "forceAlwaysFast" }; // Boolean: false
    public static final String PROP_maxFailoverSeconds[]    = new String[] { "maxFailoverSeconds" }; // Long: 
    public static final String PROP_failoverQuiet[]         = new String[] { "failoverQuiet" }; // Boolean: 

    // ------------------------------------------------------------------------

    public static       long   DEFAULT_MAX_FAILOVER_SECONDS = DateTime.HourSeconds(1);
    public static       long   MIN_FAILOVER_SECONDS         = DateTime.MinuteSeconds(10);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                  name                    = null;
    private TriState                isEnabled               = TriState.UNKNOWN;

    private String                  accessKey               = null;
    private RTProperties            properties              = null;

    private int                     failoverQuiet           = -1;  // quiet failover hint

    private ReverseGeocodeProvider  rgFailoverRGP           = null;
    private Object                  rgFailoverLock          = new Object();
    private long                    rgFailoverTime          = 0L; // Epoch time of failover
    private long                    rgFailoverTimeoutSec    = 0L; // failover timeout

    /**
    *** Constructor
    *** @param name  The name of this reverse-geocode provider
    *** @param key     The access key (may be null)
    *** @param rtProps The properties (may be null)
    **/
    public ReverseGeocodeProviderAdapter(String name, String key, RTProperties rtProps)
    {
        super();
        this.setName(name);
        this.setAuthorization(key);
        this.setProperties(rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the name of this ReverseGeocodeProvider
    *** @param name  The name of this reverse-geocode provider
    **/
    public void setName(String name)
    {
        this.name = (name != null)? name : "";
    }

    /**
    *** Gets the name of this ReverseGeocodeProvider
    *** @return The name of this reverse-geocode provider
    **/
    public String getName()
    {
        return (this.name != null)? this.name : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the authorization key of this ReverseGeocodeProvider
    *** @param key  The key of this reverse-geocode provider
    **/
    public void setAuthorization(String key)
    {
        this.accessKey = key;
    }

    /**
    *** Gets the authorization key of this ReverseGeocodeProvider
    *** @return The access key of this reverse-geocode provider
    **/
    public String getAuthorization()
    {
        return this.accessKey;
    }

    /**
    *** Returns true if the authorization key has been defined
    *** @return True if the authorization key has been defined, false otherwise.
    **/
    public boolean hasAuthorization()
    {
        return !StringTools.isBlank(this.getAuthorization());
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the failover ReverseGeocodeProvider
    *** @param rgp  The failover ReverseGeocodeProvider
    **/
    public void setFailoverReverseGeocodeProvider(ReverseGeocodeProvider rgp)
    {
        this.rgFailoverRGP = rgp;
    }

    /**
    *** Gets the failover ReverseGeocodeProvider
    *** @return The failover ReverseGeocodeProvider
    **/
    public ReverseGeocodeProvider getFailoverReverseGeocodeProvider()
    {
        return this.rgFailoverRGP;
    }

    /**
    *** Gets the failover ReverseGeocodeProvider name
    *** @return The failover ReverseGeocodeProvider name, or an empty string if
    ***         no failover is defined.
    **/
    public String getFailoverReverseGeocodeProviderName()
    {
        return (this.rgFailoverRGP != null)? this.rgFailoverRGP.getName() : "";
    }

    /**
    *** Has a failover ReverseGeocodeProvider
    *** @return True if this instance has a failover ReverseGeocodeProvider
    **/
    public boolean hasFailoverReverseGeocodeProvider()
    {
        return (this.rgFailoverRGP != null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Start failover mode (with default timeout)
    **/
    protected void startReverseGeocodeFailoverMode()
    {
        this.startReverseGeocodeFailoverMode(-1L);
    }

    /**
    *** Start failover mode (with specified timeout)
    *** @param failoverTimeoutSec The explicit failover timeout, or <= 0 for the default timeout.
    **/
    protected void startReverseGeocodeFailoverMode(long failoverTimeoutSec)
    {
        synchronized (this.rgFailoverLock) {
            this.rgFailoverTime       = DateTime.getCurrentTimeSec();
            this.rgFailoverTimeoutSec = (failoverTimeoutSec > 0L)? 
                failoverTimeoutSec : this.getMaximumFailoverElapsedSec();
        }
    }

    /** 
    *** Returns true if failover mode is active
    **/
    protected boolean isReverseGeocodeFailoverMode()
    {
        if (this.hasFailoverReverseGeocodeProvider()) {
            boolean rtn;
            synchronized (this.rgFailoverLock) {
                if (this.rgFailoverTime <= 0L) {
                    rtn = false;
                } else {
                    long deltaSec = DateTime.getCurrentTimeSec() - this.rgFailoverTime;
                    long maxFailoverSec = (this.rgFailoverTimeoutSec > 0L)? 
                        this.rgFailoverTimeoutSec : this.getMaximumFailoverElapsedSec();
                    rtn = (deltaSec < maxFailoverSec)? true : false;
                    if (!rtn) {
                        // no longer in failover timeout mode
                        this.rgFailoverTime       = 0L;
                        this.rgFailoverTimeoutSec = 0L;
                    }
                }
            }
            return rtn;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the failover "quiet" mode hint.
    **/
    public void setFailoverQuiet(boolean quiet)
    {
        this.failoverQuiet = quiet? 1 : 0;
    }

    /**
    *** Gets the failover "quiet" mode hint.
    **/
    public boolean getFailoverQuiet()
    {
        if (this.failoverQuiet >= 0) {
            return (this.failoverQuiet != 0)? true : false;
        } else {
            RTProperties rtp = this.getProperties();
            return rtp.getBoolean(PROP_failoverQuiet, false);
        }
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
    *** Sets the properties for this ReverseGeocodeProvider
    *** @param rtProps  The properties for this reverse-geocode provider
    **/
    public void setProperties(RTProperties rtProps)
    {
        this.properties = rtProps;
    }

    /**
    *** Gets the properties for this ReverseGeocodeProvider
    *** @return The properties for this reverse-geocode provider
    **/
    public RTProperties getProperties()
    {
        if (this.properties == null) {
            this.properties = new RTProperties();
        }
        return this.properties;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    *** @return A String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb= new StringBuffer();
        sb.append(this.getName());
        String auth = this.getAuthorization();
        if (!StringTools.isBlank(auth)) {
            sb.append(" [");
            sb.append(auth);
            sb.append("]");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this ReverseGeocodeProvider is enabled
    *** @return True if enabled
    **/
    public boolean isEnabled()
    {
        if (this.isEnabled.isUnknown()) {
            String key = PROP_ReverseGeocodeProvider_ + this.getName() + _PROP_isEnabled;
            if (RTConfig.getBoolean(key,true)) {
                this.isEnabled = TriState.TRUE;
            } else {
                this.isEnabled = TriState.FALSE;
                Print.logWarn("ReverseGeocodeProvider disabled: " + this.getName());
            }
        }
        //Print.logInfo("Checking RGP 'isEnabled': " + this.getName() + " ==> " + this.isEnabled.isTrue());
        return this.isEnabled.isTrue();
    }
    
    // ------------------------------------------------------------------------

    /* Fast operation? */
    public boolean isFastOperation(boolean dft)
    {
        RTProperties rtp = this.getProperties();
        return rtp.getBoolean(PROP_alwaysFast, dft);
    }

    /* Fast operation? */
    public boolean isFastOperation()
    {
        // -- default to a slow operation
        return this.isFastOperation(false);
    }

    /* Maximum failover elapsed seconds */
    public long getMaximumFailoverElapsedSec()
    {
        RTProperties rtp = this.getProperties();
        long sec = rtp.getLong(PROP_maxFailoverSeconds, DEFAULT_MAX_FAILOVER_SECONDS);
        return (sec > MIN_FAILOVER_SECONDS)? sec : MIN_FAILOVER_SECONDS;
    }

    /* get reverse-geocode */
    public abstract ReverseGeocode getReverseGeocode(GeoPoint gp, String localeStr, boolean cache);

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]   = new String[] { "account"          , "acct"       };
    private static final String ARG_PLN[]       = new String[] { "privateLabelName" , "pln" , "pl" };
    private static final String ARG_GEOPOINT[]  = new String[] { "geoPoint"         , "gp"         };
    private static final String ARG_ADDRESS[]   = new String[] { "address"          , "addr", "a"  };
    private static final String ARG_COUNTRY[]   = new String[] { "country"          , "c"          };
    private static final String ARG_CACHE[]     = new String[] { "cache"            , "save"       };

    private static void usage()
    {
        String n = ReverseGeocodeProviderAdapter.class.getName();
        Print.sysPrintln("");
        Print.sysPrintln("Description:");
        Print.sysPrintln("   Reverse-Geocode Testing Tool ...");
        Print.sysPrintln("");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("   java ... " + n + " -geoPoint=<gp> -account=<id>");
        Print.sysPrintln(" or");
        Print.sysPrintln("   java ... " + n + " -geoPoint=<gp> -pln=<name>");
        Print.sysPrintln("");
        Print.sysPrintln("Common Options:");
        Print.sysPrintln("   -account=<id>   Acount ID from which to obtain the ReverseGeocodeProvider");
        Print.sysPrintln("   -pln=<name>     PrivateLabel name/host");
        Print.sysPrintln("   -geoPoint=<gp>  GeoPoint in the form <latitude>/<longitude>");
        Print.sysPrintln("   -addr=<addr>    Address to Geocode");
        Print.sysPrintln("");
        System.exit(1);
    }

    public static void _main()
    {

        /* get GeoPoint(s) */
        GeoPoint GPA[] = null;
        if (RTConfig.hasProperty(ARG_GEOPOINT)) {
            String gpa[] = StringTools.split(RTConfig.getString(ARG_GEOPOINT,""),',');
            Vector<GeoPoint> gpList = new Vector<GeoPoint>();
            for (String gps : gpa) {
                Print.sysPrintln("Parsing: " + gps);
                GeoPoint gp = new GeoPoint(gps);
                if (gp.isValid()) {
                    gpList.add(gp);
                }
            }
            GPA = gpList.toArray(new GeoPoint[gpList.size()]);
        }
        if (ListTools.isEmpty(GPA)) {
            Print.sysPrintln("ERROR: No GeoPoint specified");
            usage();
        }

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

        /* get reverse-geocoder */
        ReverseGeocodeProvider rgp = privLabel.getReverseGeocodeProvider();
        if (rgp == null) {
            Print.sysPrintln("ERROR: No ReverseGeocodeProvider for PrivateLabel: %s", privLabel.getName());
            System.exit(99);
        } else
        if (!rgp.isEnabled()) {
            Print.sysPrintln("WARNING: ReverseGeocodeProvider disabled: " + rgp.getName());
            System.exit(0);
        }

        /* get ReverseGeocode */
        Print.sysPrintln("");
        try {
            // -- make sure the Domain properties are available to RTConfig
            privLabel.pushRTProperties();   // stack properties (may be redundant in servlet environment)
            boolean cache = RTConfig.getBoolean(ARG_CACHE, false);
            for (GeoPoint gp : GPA) {
                Print.sysPrintln("------------------------------------------------");
                Print.sysPrintln(rgp.getName() + "] ReverseGeocode: " + gp + (cache?" (cache)":""));
                long startTimeMS = DateTime.getCurrentTimeMillis();
                ReverseGeocode rg = rgp.getReverseGeocode(gp, privLabel.getLocaleString(), cache); // get the reverse-geocode
                long deltaMS = DateTime.getCurrentTimeMillis() - startTimeMS;
                if (rg != null) {
                    double kph = rg.getSpeedLimitKPH();
                    double mph = kph * GeoPoint.MILES_PER_KILOMETER;
                    Print.sysPrintln("Address:  " + rg.toString());
                    Print.sysPrintln("City    : " + rg.getCity());
                    Print.sysPrintln("State   : " + rg.getStateProvince());
                    Print.sysPrintln("Postal  : " + rg.getPostalCode());
                    Print.sysPrintln("Country : " + rg.getCountryCode());
                    Print.sysPrintln("SpeedLim: " + StringTools.format(kph,"0.0") + " km/h ["+StringTools.format(mph,"0.0")+" mph]");
                    Print.sysPrintln("Time    : " + deltaMS + " ms");
                } else {
                    Print.sysPrintln("Unable to reverse-geocode point");
                }
            }
            Print.sysPrintln("------------------------------------------------");
        } catch (Throwable th) {
            // -- ignore
        } finally {
            privLabel.popRTProperties();    // remove from stack
        }
        Print.sysPrintln("");

    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        if (RTConfig.hasProperty(ARG_ADDRESS)) {
            GeocodeProviderAdapter._main();
        } else {
            ReverseGeocodeProviderAdapter._main();
        }
    }

}
