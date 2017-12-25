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
//  2015/09/24  Martin D. Flynn
//     -Initial release [EXPERIMENTAL]
//  2015/12/07  Martin D. Flynn
//     -Auto atart auto-trim thread. [2.6.1-B35]
// ----------------------------------------------------------------------------
package org.opengts.geocoder;

import java.util.*;

import org.opengts.util.*;

//import org.opengts.extra.util.MemCache;
//import org.opengts.extra.util.MemCacheAPI;

public class ReverseGeocodeCache
{

    // ------------------------------------------------------------------------

    private static final long   DEFAULT_MAX_AGE_MS          = DateTime.MinuteSeconds(3*60) * 1000L;
    private static final int    DEFAULT_MAX_SIZE            = 2000;
    private static final long   MIN_TRIM_INTERVAL_MS        = DateTime.MinuteSeconds(10) * 1000L;
    private static final long   DEFAULT_TRIM_INTERVAL_MS    = 0L;

    private static final int    GEOPOINT_DEC_HIRES          = 5; // 1.5 meters
    private static final int    GEOPOINT_DEC_LORES          = 4; // 15 meters

    private static final int    Store_As_FullAddress        = 0;
    private static final int    Store_As_StringJSON         = 1;
    private static final int    Store_As_ReverseGeocode     = 2;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Encodes the specified GeoPoint latitude/longitude into a Long value
    *** @param gp   The GeoPoint to encode
    *** @param dec  The the number of decimal points to retain (must be either 4 or 5)
    *** @return The encoded Long GeoPoint
    **/
    private static long EncodeGeoPoint(GeoPoint gp, int dec)
    {
        if (GeoPoint.isValid(gp)) {
            double gpLat = gp.getLatitude();
            double gpLon = gp.getLongitude();
            if (dec >= 5) {
                // -- 5 decimal places (high resolution, 1.5 meters)
                long   gpLAT = Math.round(gpLat * 100000.0); //  -9000000[F76ABC0] to  9000000[0895440]
                long   gpLON = Math.round(gpLon * 100000.0); // -18000000[EED5780] to 18000000[112A880]
                long   LL    = ((gpLAT & 0xFFFFFFFL) << 28) | (gpLON & 0xFFFFFFFL);
                LL |= 0x4000000000000000L;
                return LL;
            } else {
                // -- 4 decimal places (low resolution, 15 meters)
                long   gpLAT = Math.round(gpLat *  10000.0); //  -900000[F24460]   to  900000[0DBBA0]
                long   gpLON = Math.round(gpLon *  10000.0); // -1800000[E488C0]   to 1800000[1B7740]
                long   LL    = ((gpLAT &  0xFFFFFFL) << 24) | (gpLON &  0xFFFFFFL);
                return LL;
            }
        } else {
            return 0L;
        }
    }

    private static GeoPoint decodeGeoPoint(long gpLL)
    {
        //Print.logInfo("Convertion to GP: " + StringTools.toHexString(gpLL));
        if ((gpLL & 0x4000000000000000L) != 0L) {
            // -- 5 decimal places
            //Print.logInfo("5-decimal locations ...");
            long LL  = gpLL;
            long MSK = 0xFFFFFFFL; // 28-bits
            long LAT = (LL >> 28) & MSK;
            long LON = (LL >>  0) & MSK;
            if ((LAT & 0x8000000L) != 0L) { LAT = ~MSK | LAT; }
            if ((LON & 0x8000000L) != 0L) { LON = ~MSK | LON; }
            double lat = (double)LAT / 100000.0;
            double lon = (double)LON / 100000.0;
            //Print.logInfo("5-dec Lat/Lon: " + lat + "/" + lon);
            return GeoPoint.isValid(lat,lon)? new GeoPoint(lat,lon) : null;
        } else {
            // -- 4 decimal places
            //Print.logInfo("4-decimal locations ...");
            long LL  = gpLL;
            long MSK = 0xFFFFFFL; // 24-bits
            long LAT = (LL >> 24) & MSK;
            long LON = (LL >>  0) & MSK;
            if ((LAT & 0x800000L) != 0L) { LAT = ~MSK | LAT; }
            if ((LON & 0x800000L) != 0L) { LON = ~MSK | LON; }
            double lat = (double)LAT / 10000.0;
            double lon = (double)LON / 10000.0;
            //Print.logInfo("4-dec Lat/Lon: " + lat + "/" + lon);
            return GeoPoint.isValid(lat,lon)? new GeoPoint(lat,lon) : null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Class for periodically trimming the ReverseGeocodeCache
    **/
    private static class AutoTrimThread
        implements Runnable
    {
        private          ReverseGeocodeCache rgCache        = null;
        private          Thread              trimThread     = null;
        private volatile boolean             isStopped      = false;
        private          long                intervalMS     = 5L * 60000L;
        private          Object              intervalLock   = new Object();
        public AutoTrimThread(ReverseGeocodeCache rgc, long intervalMS) {
            super();
            this.rgCache    = rgc; // MUST not be null
            this.intervalMS = intervalMS;
            this.isStopped  = false;
            this.trimThread = null;
        }
        public String getName() {
            return "RGAutoTrim_" + this.rgCache.getName();
        }
        public boolean isRunning() {
            return (this.trimThread != null) && !this.isStopped;
        }
        public boolean start() {
            if (this.trimThread != null) {
                // -- already started
                return false;
            } else
            if (this.isStopped) {
                // -- already stopped
                return false;
            } else {
                // -- create/start thread
                this.trimThread = new Thread(this, this.getName());
                this.trimThread.start();
                return true;
            }
        }
        public void stop() {
            if (this.trimThread != null) {
                this.isStopped = true;
                this.trimThread.interrupt();
            }
        }
        public void run() {
            Print.logInfo("AutoTrimThread started: " + this.getName());
            // -- turn off trim-on-add
            this.rgCache.setTrimOnAdd(false);
            // -- loop and trim
            for (;!this.isStopped;) {
                // -- trim
                this.rgCache.trimCache();
                // -- wait for next interval
                synchronized (this.intervalLock) {
                    long nowMS = System.currentTimeMillis();
                    long futMS = nowMS + this.intervalMS;
                    while (!this.isStopped && (futMS > nowMS)) {
                        try {
                            this.intervalLock.wait(futMS - nowMS);
                        } catch (InterruptedException ie) {
                            // -- thread interrupted, possible stop request
                        } catch (Throwable th) {
                            // -- unepected error
                            this.isStopped = true;
                            break;
                        }
                        nowMS = System.currentTimeMillis();
                    }
                }
            }
            // -- stopped: turn on trim-on-add
            this.rgCache.setTrimOnAdd(true);
            Print.logInfo("AutoTrimThread stopped: " + this.getName());
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                  rgCacheName         = null;
    private MemCache<Long,Object>   rgCacheMap          = null;
    private int                     rgStoreType         = Store_As_StringJSON;
    
    private boolean                 highResolution      = false; // false=4dec, true=5dec

    private long                    autoTrimIntervalMS  = 0L;
    private AutoTrimThread          autoTrimThread      = null;
    private volatile boolean        autoTrimChecked     = false;

    /**
    *** Constructor
    **/
    public ReverseGeocodeCache()
    {
        this("general",DEFAULT_MAX_SIZE,DEFAULT_MAX_AGE_MS,DEFAULT_TRIM_INTERVAL_MS);
    }

    /**
    *** Constructor
    **/
    public ReverseGeocodeCache(String name, int maxSize, long maxAgeMS, long autoTrimMS)
    {
        super();
        this.rgCacheName = StringTools.trim(name);
        this.rgStoreType = Store_As_StringJSON;
        this.rgCacheMap  = new MemCache<Long,Object>();
        this.rgCacheMap.setMaximumCacheSize(maxSize);
        this.rgCacheMap.setMaximumEntryAgeMS(maxAgeMS);
        this.setAutoTrimInterval(autoTrimMS);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name of this ReverseGeocodeCache instance
    **/
    public String getName()
    {
        return this.rgCacheName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the current size of the memory cache
    **/
    public int getSize()
    {
        return this.rgCacheMap.getSize();
    }
    
    public int getSizeCutbackCount()
    {
        return this.rgCacheMap.getMaximumCacheSizeCutbackCount();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum cache size 
    **/
    public void setMaximumSize(int maxSize)
    {
        int ms = (maxSize > 100)? maxSize : 100;
        this.rgCacheMap.setMaximumCacheSize(ms);
    }

    /**
    *** Gets the maximum cache size 
    **/
    public int getMaximumSize()
    {
        return this.rgCacheMap.getMaximumCacheSize();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum entry age, in milliseconds 
    **/
    public void setMaximumAgeMS(long maxAgeMS)
    {
        long maMS = (maxAgeMS >= 0L)? maxAgeMS : DEFAULT_MAX_AGE_MS;
        this.rgCacheMap.setMaximumEntryAgeMS(maMS);
    }

    /**
    *** Gets the maximum entry age, in milliseconds 
    **/
    public long getMaximumAgeMS()
    {
        return this.rgCacheMap.getMaximumEntryAgeMS();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the trim-on-add attribute
    **/
    public void setTrimOnAdd(boolean trim)
    {
        this.rgCacheMap.setTrimOnAdd(trim);
    }

    /**
    *** Trims/removes aged/excessive entries from cache
    **/
    protected void trimCache()
    {
        this.rgCacheMap.trimCache("trimCache");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Set the auto-trim thread interval.
    **/
    public void setAutoTrimInterval(long intervalMS)
    {
        synchronized (this) { // lock while setting interval
            this.autoTrimIntervalMS = (intervalMS > MIN_TRIM_INTERVAL_MS)? 
                intervalMS : MIN_TRIM_INTERVAL_MS;
        }
    }

    /**
    *** Set the auto-trim thread interval.
    **/
    public long getAutoTrimInterval()
    {
        long intervalMS;
        synchronized (this) { // lock while getting interval
            intervalMS = this.autoTrimIntervalMS;
        }
        return intervalMS;
    }

    // --------------------------------

    /**
    *** Starts the "auto-trim" thread.<br>
    *** Calling this method before adding an address is not required.
    *** This method will automatically be called when the first address is added.
    **/
    public void _startAutoTrimThread()
    {
        if (!this.autoTrimChecked) {
            synchronized (this) { // lock while starting thread
                if (!this.autoTrimChecked) { // retest boolean
                    if (this.autoTrimIntervalMS <= 0L) {
                        // -- no auto-trim interval requested
                        Print.logInfo("AutoTrimThread not used");
                    } else
                    if (this.autoTrimThread == null) { // this.autoTrimThread always null here
                        Print.logInfo("AutoTrimThread starting with interval "+this.autoTrimIntervalMS+" ms");
                        this.autoTrimThread = new AutoTrimThread(this, this.autoTrimIntervalMS);
                        this.autoTrimThread.start();
                    }
                    this.autoTrimChecked = true;
                }
            }
        }
    }

    /**
    *** Stop the "trim" thread.
    *** Once stopped, it cannot be restarted.
    **/
    public void stopAutoTrimThread()
    {
        synchronized (this) { // lock while stopping thread
            if (this.autoTrimThread != null) {
                this.autoTrimThread.stop();
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets high/low GeoPoint caching resolution.
    *** 'True' uses 5-decimal points of resolution in the Latitude/Longitude (about 1.5 meters).
    *** 'False' uses 4-decimal points of resolution (about 15 meters).
    *** @param hiRes The high/low GeoPoint caching resolution.
    **/
    public void setHighResolution(boolean hiRes)
    {
        this.highResolution = hiRes;
    }

    /**
    *** Gets high/low GeoPoint caching resolution.
    *** 'True' uses 5-decimal points of resolution in the Latitude/Longitude (about 1.5 meters).
    *** 'False' uses 4-decimal points of resolution (about 15 meters).
    *** @return The high/low GeoPoint caching resolution.
    **/
    public boolean getHighResolution()
    {
        return this.highResolution;
    }

    /**
    *** Encodes the specified GeoPoint latitude/longitude into a Long value
    *** @param gp   The GeoPoint to encode
    *** @return The encoded Long GeoPoint
    **/
    private Long encodeGeoPoint(GeoPoint gp)
    {
        int dec = this.highResolution? GEOPOINT_DEC_HIRES : GEOPOINT_DEC_LORES;
        return new Long(ReverseGeocodeCache.EncodeGeoPoint(gp,dec));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the cached ReverseGeocode for the specified GeoPoint, or null if
    *** no ReverseGeocode exists for the specified GeoPoint.
    **/
    public ReverseGeocode getReverseGeocode(GeoPoint gp)
    {
        if (GeoPoint.isValid(gp)) {
            // -- get ReverseGeocode entry
            Long   rgKey = this.encodeGeoPoint(gp);
            Object rgVal = this.rgCacheMap.getValue(rgKey);
            // -- not found?
            if (rgVal == null) {
                // -- key does not exist
                return null;
            }
            // -- is ReverseGeocode instance?
            if (rgVal instanceof ReverseGeocode) {
                return (ReverseGeocode)rgVal;
            }
            // -- not a String?
            if (!(rgVal instanceof String)) {
                // -- should not occur
                Print.logWarn("Invalid object type in ReverseGeocode Cache: " + StringTools.className(rgVal));
                return null;
            }
            // -- parse String
            String rgValS = rgVal.toString();
            if (rgValS.startsWith("{")) {
                // -- contains JSON
                try {
                    return new ReverseGeocode(new JSON(rgValS));
                } catch (JSON.JSONParsingException jpe) {
                    // -- unable to parse JSON
                    Print.logWarn("Invalid JSON found in ReverseGeocode Cachs: " + rgValS);
                    return null;
                }
            } else {
                // -- assume full address
                ReverseGeocode rg = new ReverseGeocode();
                rg.setFullAddress(rgValS);
                return rg;
            }
        } else {
            // -- invalid GeoPoint, to ReverseGeocode instance
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Adds the specified ReverseGeocode to the cache for the specified GeoPoint.
    **/
    public boolean addReverseGeocode(GeoPoint gp, ReverseGeocode rg)
    {

        /* invalid GeoPoint or no ReverseGeocode entry? */
        if (!GeoPoint.isValid(gp) || (rg == null)) {
            return false;
        }

        /* create GeoPoint key/value */
        Long   rgKey = this.encodeGeoPoint(gp);
        Object rgVal;
        switch (this.rgStoreType) {
            case Store_As_FullAddress:
                rgVal = rg.getFullAddress();
                while (((String)rgVal).startsWith("{")) { rgVal = ((String)rgVal).substring(1); }
                break;
            case Store_As_StringJSON:
                rgVal = rg.toJSON().toString(false);
                break;
            case Store_As_ReverseGeocode:
                rgVal = rg;
                break;
            default:
                rgVal = rg.toJSON().toString(false);
                break;
        }

        /* add to cache */
        this.rgCacheMap.addValue(rgKey, rgVal);

        /* start auto-trim thread? */
        if (!this.autoTrimChecked) {
            this._startAutoTrimThread();
        }

        /* success */
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
