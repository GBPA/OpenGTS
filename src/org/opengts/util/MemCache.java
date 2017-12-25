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
//  A simple memory cache handler
// ----------------------------------------------------------------------------
// Change History:
//  2014/09/16  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.*;
import java.util.*;
import java.math.*;

import org.opengts.util.*;

/**
*** A simple memory cache handler
**/

public class MemCache<KEY,VAL>
    implements MemCacheAPI<KEY,VAL>
{

    // ------------------------------------------------------------------------

    public  static final int        MINIMUM_CACHE_SIZE      =   50;
    public  static final int        DEFAULT_CACHE_SIZE      = 3000;

    public  static final long       MAXIMUM_CACHE_AGE_MS    = 0L; // indefinite

    // ------------------------------------------------------------------------

    public  static final double     DEFAULT_TRIM_PERCENT    = 0.90;
    public  static final int        DEFAULT_TRIM_SIZE       = TrimSize(DEFAULT_CACHE_SIZE);
    public  static int TrimSize(int size) { return (int)((double)size * DEFAULT_TRIM_PERCENT); }

    public  static final double     MAX_MEMORY_UTILIZATION  = 0.95;
    public  static final double     RECLAIMED_MEMORY_UTIL   = MAX_MEMORY_UTILIZATION - 0.10;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Class MemEntry
    **/
    public static class MemEntry<KEY,VAL>
    {
        private KEY     key         = null;
        private VAL     value       = null;
        private long    createTime  = 0L;
        private long    timestamp   = 0L;
        public MemEntry(KEY k, VAL v) {
            this.key        = k;
            this.value      = v;
            this.createTime = System.currentTimeMillis();
            this.timestamp  = this.createTime;
        }
        public KEY getKey() {
            return this.key;
        }
        public VAL getValue() {
            return this.getValue(true);
        }
        public VAL getValue(boolean update) {
            if (update) {
                this.timestamp = System.currentTimeMillis();
            }
            return this.value;
        }
        public long getCreateTimeMS() {
            return this.createTime;
        }
        public long getRefreshTimeMS() {
            return this.timestamp;
        }
        public String getKeyString() {
            return (this.key != null)? this.key.toString() : "null";
        }
        public String getValueString() {
            return (this.value != null)? this.value.toString() : "null";
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            sb.append(this.getRefreshTimeMS());
            sb.append("] ");
            sb.append(this.getKeyString());
            sb.append(" ==> ");
            sb.append(this.getValueString());
            return sb.toString();
        }
    }

    /**
    *** Class MemEntryComparator (used for debugging only)
    **/
    public static class MemEntryComparator<KEY,VAL>
        implements Comparator<MemEntry<KEY,VAL>>
    {
        private boolean ascending = true;
        public MemEntryComparator() {
            this(true);
        }
        public MemEntryComparator(boolean ascending) {
            super();
            this.ascending = ascending;
        }
        public int compare(MemEntry<KEY,VAL> o1, MemEntry<KEY,VAL> o2) {
            long t1 = (o1 != null)? o1.getRefreshTimeMS() : 0L;
            long t2 = (o2 != null)? o2.getRefreshTimeMS() : 0L;
            if (t1 < t2) {
                return this.ascending? -1 : 1;
            } else 
            if (t1 > t2) {
                return this.ascending? 1 : -1;
            } else {
                return 0;
            }
        }
        public boolean equals(Object other) {
            if (other instanceof MemEntryComparator) {
                MemEntryComparator<?,?> mc = (MemEntryComparator)other;
                return (mc.ascending == this.ascending)? true : false;
            }
            return false;
        }
        public int hashCode() {
            return super.hashCode();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Class MemCachMap
    **/
    public class MemCacheMap<KEY,VAL>
        implements MemCacheAPI<KEY,VAL>
    {
        private int                             cutbackCount = 0;
        private int                             maxSize      = DEFAULT_CACHE_SIZE;
        private int                             trimSize     = DEFAULT_TRIM_SIZE;
        private long                            maxAgeMS     = 0L;
        private HashMap<KEY,MemEntry<KEY,VAL>>  cacheMap     = null;
        public MemCacheMap() {
            this(DEFAULT_CACHE_SIZE);
        }
        public MemCacheMap(int maxSize) {
            super();
            this.cacheMap = new HashMap<KEY,MemEntry<KEY,VAL>>((maxSize * 3) / 2);
            this.setMaximumCacheSize(maxSize);
        }
        public int getMaximumCacheSize() {
            return this.maxSize;
        }
        public int setMaximumCacheSize(int max) {
            this.maxSize = (max > MINIMUM_CACHE_SIZE)? max : MINIMUM_CACHE_SIZE;
            this.setTrimCacheSize(-1); // reset minimum cache size
            this.trimCache("setMaximumCacheSize");
            return this.maxSize;
        }
        public boolean exceedsMaximumCacheSize(double maxGain) {
            int max = this.getMaximumCacheSize();
            if (max <= 0) {
                return false;
            } else {
                int absMax = (int)((double)max * maxGain);
                return (this.getSize() > absMax)? true : false;
            }
        }
        public int getTrimCacheSize() {
            return this.trimSize;
        }
        public int setTrimCacheSize(int min) {
            this.trimSize = ((min > 0) && (min <= this.maxSize))? min : TrimSize(this.maxSize);
            return this.trimSize;
        }
        public int getMaximumCacheSizeCutbackCount() {
            return this.cutbackCount;
        }
        public long getMaximumEntryAgeMS() {
            return this.maxAgeMS;
        }
        public long setMaximumEntryAgeMS(long max) {
            this.maxAgeMS = (max >= 0L)? max : 0L;
            this.trimCache("setMaximumEntryAgeMS");
            return this.maxAgeMS;
        }
        public int getSize() {
            return this.cacheMap.size();
        }
        public boolean hasValue(KEY key) {
            if (key != null) {
                return this.cacheMap.containsKey(key);
            } else {
                return false;
            }
        }
        public void addValue(KEY key, VAL val) {
            if (key != null) {
                MemEntry<KEY,VAL> me = new MemEntry<KEY,VAL>(key,val);
                this.cacheMap.put(key, me);
                //this.trimCache("addValue");
            }
        }
        public VAL getValue(KEY key, VAL dft) {
            // -- get entry (return default if not found)
            MemEntry<KEY,VAL> me = this.cacheMap.get(key);
            if (me == null) {
                // -- not found
                return dft;
            }
            // -- check max age
            long nowMS = System.currentTimeMillis();
            long maxMS = this.getMaximumEntryAgeMS();
            if ((maxMS > 0L) && ((nowMS - me.getCreateTimeMS()) > maxMS)) {
                // -- entry is too old (will be removed on next trim)
                return dft;
            }
            // -- return entry value
            return me.getValue(true); // updates timestamp
        }
        public void trimCache(String msg) {
            long startMS = System.currentTimeMillis();
            boolean didRemove = false;
            // -- check memory utilization
            double memUtilPct = OSTools.getMemoryUtilization();
            if (memUtilPct >= MAX_MEMORY_UTILIZATION) {
                // -- we've exceeded our preset memory utilization threshold.
                if (this.maxSize <= MINIMUM_CACHE_SIZE) {
                    // -- we are already at the minimum cache size
                    // -  the memory utilization issue isn't caused by this cache
                    // -  just continue
                    //Print.logDebug("TrimCache["+msg+"]: Excess memory usage ("+(int)(memUtilPct*100.0)+"%), already at minimum cache size ...");
                } else {
                    // -- garbage-collect and try again
                    //Print.logDebug("TrimCache["+msg+"]: Excess memory usage ("+(int)(memUtilPct*100.0)+"%), GC and trying again ...");
                    System.gc(); // expensive operation
                    memUtilPct = OSTools.getMemoryUtilization();
                    if (memUtilPct > RECLAIMED_MEMORY_UTIL) {
                        // -- not enough memory reclaimed, cut back on the cache maximum size
                        this.cutbackCount++;
                        int currSize = this.getSize();
                        int cbSize   = Math.min(currSize,this.getMaximumCacheSize());
                        int newMax   = ((int)((double)cbSize * 0.90) / 1000) * 1000; // reset max size
                        this.maxSize = newMax; // set directly to avoid recursive "trimCache"
                        if (this.maxSize < MINIMUM_CACHE_SIZE) { this.maxSize = MINIMUM_CACHE_SIZE; }
                        Print.logWarn("TrimCache["+msg+"]: Excess memory usage "+StringTools.format(memUtilPct,"0.0")+"% ["+this.cutbackCount+"@"+currSize+"], max size reset to "+this.maxSize);
                        this.setTrimCacheSize(-1); // reset minimum cache size
                    }
                }
            }
            // -- trim
            int max = this.getMaximumCacheSize();
            if ((max > 0) && (this.getSize() > max)) {
                // -- exceeds maximum, trim aged entries, and get oldest
                int min   = this.getTrimCacheSize();
                int limit = this.getSize() - min;
                Vector<MemEntry<KEY,VAL>> meList = this._getOldestUsedEntries(true,limit); // trim aged
              //Print.logDebug("TrimCache["+msg+"]: Removing " + meList.size() + " oldest entries");
                for (MemEntry<KEY,VAL> me : meList) {
                    this.cacheMap.remove(me.getKey());
                    didRemove = true;
                }
            }
            // -- garbage collect, etc.
            if (didRemove) {
                //System.gc(); // run garbage collection
              //Print.logDebug("TrimCache["+msg+"] completed in " + (System.currentTimeMillis() - startMS) + " ms");
            }
        }
        private Vector<MemEntry<KEY,VAL>> _getOldestUsedEntries(boolean trimAged, int limit) {
            Vector<MemEntry<KEY,VAL>> meList = new Vector<MemEntry<KEY,VAL>>();
            long nowMS   = System.currentTimeMillis();
            long maxMS   = this.getMaximumEntryAgeMS();
            long lastTS  = 0L; // last refresh timestamp MS
            int  sortCnt = 0;
          //int  ndx    = 0;
            MemEntryComparator<KEY,VAL> meComp = new MemEntryComparator<KEY,VAL>(); // ascending
            for (Iterator<KEY> i = this.cacheMap.keySet().iterator(); i.hasNext();) {
                // -- get MemEntry for key
                KEY key = i.next();
                // -- debug
                //ndx++;if((ndx%10000)==0){Print.logInfo("Entry ["+ndx+"]: "+key+" - MEList "+meList.size());}
                // -- get entry
                MemEntry<KEY,VAL> me = this.cacheMap.get(key);
                if (me == null) {
                    // -- unlikely, but check anyway
                    i.remove(); // remove this null entry
                    continue;
                }
                // -- check expired
                if (trimAged && (maxMS > 0L) && ((nowMS - me.getCreateTimeMS()) > maxMS)) {
                    // -- delete expired MemEntry
                    i.remove();
                    continue;
                }
                // -- added to list, and sort/trim
                boolean alwaysAdd = false;
                if (alwaysAdd || (lastTS <= 0L) || (me.getRefreshTimeMS() <= lastTS)) {
                    // -- only keep entries that are older than the previous lastMS
                    meList.add(me);
                    if (meList.size() >= (2 * limit)) {
                        // -- sort ascending
                        ListTools.sort(meList, meComp); // ascending
                        // -- truncate to limit (oldest entries, with smallest timestamps)
                        meList.setSize(limit);
                        // -- get refresh timestamp of last entry (youngest of the old)
                        lastTS = meList.get(meList.size() - 1).getRefreshTimeMS();
                        sortCnt++;
                        //Print.logInfo(sortCnt + ") Sorted: LastOldest="+meList.get(0).getRefreshTimeMS()+" FirstOldest=" + lastTS);
                    }
                } else {
                    //Print.logInfo("Omitting entry: " + me.getRefreshTimeMS());
                }
            }
            // -- trim list to limit, and return
            if (meList.size() > limit) {
                // -- sort
                ListTools.sort(meList, meComp); // ascending
                // -- truncate to limit (oldest entries)
                meList.setSize(limit);
                // -- get refresh timestamp of last entry (youngest of the old)
                lastTS = meList.get(meList.size() - 1).getRefreshTimeMS();
            }
            ////Print.logDebug("FirstOldest="+lastTS);
            ////Print.logDebug("LastOldest ="+meList.get(0).getRefreshTimeMS()+" [" + (System.currentTimeMillis() - nowMS) + "ms]");
            return meList;
        }
        public String toString() { // used for debugging only
            StringBuffer sb = new StringBuffer();
            Vector<MemEntry<KEY,VAL>> meList = new Vector<MemEntry<KEY,VAL>>();
            meList.addAll(this.cacheMap.values());
            ListTools.sort(meList, new MemEntryComparator<KEY,VAL>());
            int ndx = 0;
            for (MemEntry<KEY,VAL> me : meList) {
                sb.append(ndx++);
                sb.append(": ");
                sb.append(me.toString());
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private MemCacheMap<KEY,VAL>    memCache    = null;
    private volatile boolean        trimOnAdd   = true;

    /**
    *** Constructor
    **/
    public MemCache() 
    {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
    *** Constructor
    **/
    public MemCache(int maxSize) 
    {
        this.memCache = new MemCacheMap<KEY,VAL>();
        this.setMaximumCacheSize(maxSize);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the current size of the memory-cache
    *** @return The current size of the memory-cache
    **/
    public int getSize() 
    {
        int rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.getSize();
        }
        return rtn;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum cache size
    *** @param maxSize  The maximum cache size
    *** @return The maximum cache size that was successfully set, or -1 if unable to set the size.
    **/
    public int setMaximumCacheSize(int maxSize)
    {
        int rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.setMaximumCacheSize(maxSize);
        }
        return rtn;
    }

    /**
    *** Gets the maximum cache size
    *** @return  The maximum cache size, or -1 if unable to retrieve the size.
    **/
    public int getMaximumCacheSize()
    {
        int rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.getMaximumCacheSize();
        }
        return rtn;
    }

    /**
    *** Returns true if the current size exceeds the maximum cache size
    *** @param maxGain  The gain/percentage to apply to the max cache size 
    *** @return True if the current size exceeds the maximum size, multiplied by the maxGain.
    **/
    public boolean exceedsMaximumCacheSize(double maxGain)
    {
        boolean rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.exceedsMaximumCacheSize(maxGain);
        }
        return rtn;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum age of a cached entry (in milliseconds)
    *** @param maxAgeMS  The maximum age of a cached entry (in milliseconds)
    *** @return The maximum age that was successfully set.
    **/
    public long setMaximumEntryAgeMS(long maxAgeMS)
    {
        long rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.setMaximumEntryAgeMS(maxAgeMS);
        }
        return rtn;
    }

    /**
    *** Gets the maximum age of a cached entry (in milliseconds)
    *** @return  The maximum age of a cached entry.
    **/
    public long getMaximumEntryAgeMS()
    {
        long rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.getMaximumEntryAgeMS();
        }
        return rtn;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the number of size cutbacks performed on the cache which were
    *** caused by excessive memory usage.
    **/
    public int getMaximumCacheSizeCutbackCount() 
    {
        int rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.getMaximumCacheSizeCutbackCount();
        }
        return rtn;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the trim-on-add state
    **/
    public void setTrimOnAdd(boolean toa)
    {
        this.trimOnAdd = toa;
    }

    /**
    *** Gets the trim-on-add state
    **/
    public boolean getTrimOnAdd()
    {
        return this.trimOnAdd;
    }

    /**
    *** Remove old/excessive entries in cache
    **/
    public void trimCache(String msg)
    {
        synchronized (this.memCache) {
            this.memCache.trimCache(msg);
        }
    }

    /**
    *** Remove old/excessive entries in cache
    **/
    public void trimCache()
    {
        this.trimCache("trimCache");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Add entry to cache
    *** @param key  The key to add
    *** @param val  The value to associate with the specified key
    **/
    public void addValue(KEY key, VAL val)
    {
        synchronized (this.memCache) {
            this.memCache.addValue(key, val);
            if (this.getTrimOnAdd()) {
                // -- standard trim on each add
                this.memCache.trimCache("addValue:TrimOnAdd");
            } else
            if (this.exceedsMaximumCacheSize(1.10)) {
                // -- exceeds max size by 10%, trim now anyway
                Print.logWarn("*** Excessive cache size, trimming now on 'addValue' ***");
                this.memCache.trimCache("addValue:ExceedsMax");
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified key exists in the cache
    *** @param key  The key to check for existence
    *** @return True if the specified key exists in the cache, false otherwise
    **/
    public boolean hasValue(KEY key)
    {
        boolean rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.hasValue(key);
        }
        return rtn;
    }

    // ------------------------------------------------------------------------

    /**
    *** Get entry from cache
    *** @param key  The key of the value to retrieve
    *** @return The value for the specified key, or null if the key does not exist
    **/
    public VAL getValue(KEY key)
    {
        return this.getValue(key, null);
    }

    /**
    *** Get entry from cache
    *** @param key  The key of the value to retrieve
    *** @param dft  The default value if the key does not exist
    *** @return The value for the specified key, or specified default if the key does not exist
    **/
    public VAL getValue(KEY key, VAL dft)
    {
        VAL rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.getValue(key, dft);
        }
        return rtn;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a String representation of the contents of this MemCache.
    *** (used for debugging only)
    **/
    public String toString()
    {
        String rtn;
        synchronized (this.memCache) {
            rtn = this.memCache.toString();
        }
        return rtn;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
