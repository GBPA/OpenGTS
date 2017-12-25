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
//  Provides a per-thread Map instance
// ----------------------------------------------------------------------------
// Notes:
//  - Possible ThreadLocal memory leaks:
//      http://wiki.apache.org/tomcat/MemoryLeakProtection
//      http://stackoverflow.com/questions/13852632/is-it-really-my-job-to-clean-up-threadlocal-resources-when-classes-have-been-exp
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/05/14  Martin D. Flynn
//     -Added initial Java 5 'generics'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

/**
*** A thread local map container
*** @see java.lang.ThreadLocal
*** @see java.util.Map
**/

public class ThreadLocalMap<K,V>
    extends ThreadLocal<Map<K,V>>
    implements Map<K,V>
{

    // ------------------------------------------------------------------------

    private String                  name        = "";
    private Class<Map<K,V>>         mapClass    = null;
    private ThreadLocal<Map<K,V>>   threadLocal = null;

    /**
    *** Constructor
    **/
    public ThreadLocalMap(String name)
    {
        this(name, null);
    }

    /**
    *** Constructor
    *** @param mapClass The class to use for the supporting Map
    **/
    public ThreadLocalMap(final String name, final Class<Map<K,V>> mapClass) 
    {
        super();
        // -- warning if running in a servlet context
        if (RTConfig.isTrueServlet()) {
            Print.logWarn("Creating a ThreadLocalMap within a servlet!");
        }
        // -- initialize
        this.name        = StringTools.trim(name);
        this.mapClass    = mapClass;
        this.threadLocal = new ThreadLocal<Map<K,V>>() {
            private String className = null;
            protected Map<K,V> initialValue() {
                Map<K,V> map;
                String tlmName = ThreadLocalMap.this.getName();
                String thrName = Thread.currentThread().getName();
                Print.logInfo("Initializing ThreadLocalMap for thread [" + tlmName + "]: " + thrName);
                if (ThreadLocalMap.this.mapClass == null) {
                    map = new Hashtable<K,V>();
                } else {
                    try {
                        map = ThreadLocalMap.this.mapClass.newInstance(); // throw ClassCastException
                    } catch (Throwable t) {
                        // -- Give up and try a Hashtable
                        Print.logException("Error instantiating: " + StringTools.className(ThreadLocalMap.this.mapClass), t);
                        map = new Hashtable<K,V>();
                    }
                }
                this.className = StringTools.className(map);
                return map;
            }
            public Map<K,V> get() {
                //Print.logStackTrace("Getting ThreadLocal Map contents ...");
                return super.get();
            }
            public String toString() {
                String str = "(Map class)" + this.className; // do not use "this.get()"
                //Print.logStackTrace(str);
                return str;
            }
        };
        // -- current thread info
        Thread currThread = Thread.currentThread();
        Print.logInfo("ThreadLocalMap created in thread: " + currThread.getName() + " ["+StringTools.className(currThread)+"]");
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the name of the ThreadLocalMap instance
    **/
    public String getName()
    {
        return this.name;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the class of the supporting Map
    *** @return The class of the supporting Map
    **/
    public Class<Map<K,V>> getMapClass() 
    {
        return this.mapClass;
    }

    /**
    *** Gets the supporting map
    *** @return The supporting map
    **/
    protected Map<K,V> _getMap()
    {
        Map<K,V> map = this.threadLocal.get(); // may call "initialValue()"
        if (map == null) {
            Print.logError("'<ThreadLocal>.get()' has return null!");
        }
        return map;
    }

    // ------------------------------------------------------------------------
    // Map interface

    /**
    *** Removes all of the mappings from this map 
    **/
    public void clear()
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
        } else {
            map.clear();
        }
    }

    /**
    *** Returns true if this map contains a mapping for the specified key
    *** @param key key whose presence in this map is to be tested
    *** @return True if this map contains a mapping for the specified key
    **/
    public boolean containsKey(Object key)
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return false;
        } else {
            return map.containsKey(key);
        }
    }

    /**
    *** Returns true if this map maps one or more keys to the
    *** specified value
    *** @param value value whose presence in this map is to be tested
    *** @return True if this map maps one or more keys to the
    ***         specified value
    **/
    public boolean containsValue(Object value)
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return false;
        } else {
            return map.containsValue(value);
        }
    }

    /**
    *** Returns a Set view of the mappings contained in this map
    *** @return A Set view of the mappings contained in this map
    **/
    public Set<Map.Entry<K,V>> entrySet()
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return null;
        } else {
            return map.entrySet();
        }
    }

    /**
    *** Compares a specified object with this map for equality.  Returns
    *** true if the given object is a map representing the same mappings.
    *** @param o object to be compared for equality with this map
    *** @return True if the specified object is equal to this map
    **/
    public boolean equals(Object o)
    {
        if (!(o instanceof ThreadLocalMap)) {
            return false;
        }
        // -- 
        Map<K,V> map    = this._getMap();
        @SuppressWarnings("unchecked") /*javac1.7*/
        Map<K,V> objMap = ((ThreadLocalMap<K,V>)o)._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return (objMap == null)? true : false;
        } else {
            return map.equals(objMap);
        }
    }

    /**
    *** Gets a value to which a specified value is mapped
    *** @param key The key whose associated value is to be returned
    *** @return The value to which the specified key is mapped, or
    ***         null if this map contains no mapping for the key
    **/
    public V get(Object key)
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return null;
        } else {
            return (key != null)? map.get(key) : null;
        }
    }

    /**
    *** Returns the hash code value for this map
    *** @return The hash code value for this map
    **/
    public int hashCode()
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return super.hashCode();
        } else {
            return map.hashCode();
        }
    }

    /**
    *** Returns true if the map is empty
    *** @return Ture if the map is empty
    **/
    public boolean isEmpty()
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return true;
        } else {
            return map.isEmpty();
        }
    }

    /**
    *** Returns a Set view of the keys contained in this map
    *** @return A Set view of the keys contained in this map
    **/
    public Set<K> keySet()
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return null;
        } else {
            return map.keySet();
        }
    }

    /**
    *** Associates a specied value with a specified key
    *** @param key key with which the specified value is to be associated
    *** @param value value to be associated with the specified key
    *** @return The previous value associated with <code>key</code>, or null
    **/
    public V put(K key, V value)
    {
        if (key == null) {
            Print.logStackTrace("Null key");
            return null;
        }
        // --
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return null;
        } else
        if (value == null) {
            map.remove(key);
            return null;
        } else {
            return map.put(key, value);
        }
    }

    /**
    *** Copies all of the mappings from the specified map to this map
    *** @param t map whose mappings will be stored in this map
    **/
    public void putAll(Map<? extends K, ? extends V> t)
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
        } else {
            map.putAll(t);
        }
    }
    
    /**
    *** Removes the mapping for a key from this map if it is present
    *** @param key The key whose value will be removed
    *** @return The previous value associated with <code>key</key>, or null
    **/
    public V remove(Object key)
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return null;
        } else {
            return map.remove(key);
        }
    }

    /**
    *** Returns the number of key-value mappings in this map
    *** @return The number of key-value mappings in this map
    **/
    public int size()
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return 0;
        } else {
            return map.size();
        }
    }

    /**
    *** Returns a Collection view of the values contained in this map
    *** @return A Collection view of the values contained in this map
    **/
    public Collection<V> values()
    {
        Map<K,V> map = this._getMap();
        if (map == null) {
            // -- ERROR: map not initialized, or has been released
            return null;
        } else {
            return map.values();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Release the ThreadLocal resource for the current thread.
    **/
    public void releaseResources()
    {
        // -- To be effective, this needs to be called in every thread that happened to call 
        // -  "this.threadLocal.initialValue()".  Ineffective if called by a thread that
        // -  never called "initialValue()".
      //String threadName = Thread.currentThread().getName();
      //Print.logInfo("Releasing ThreadLocalMap resources in thread: " + threadName);
      
        /* release the contained ThreadLocal */
        // -  If the current thread never called "this.threadLocal.initialValue()" to begin
        // -  with, this will end up creating a resource, then immediately try to release it.
        Map<K,V> map = this.threadLocal.get();
        if (map != null) {
            map.clear();
            this.threadLocal.remove();
        }

        /* release this ThreadLocal (may already be null) */
        this.remove();
      //Print.logInfo("Released ThreadLocal map resources in thread: " + threadName);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
