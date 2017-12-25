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

/**
*** Memory cache handler interface
**/

public interface MemCacheAPI<KEY,VAL>
{

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum cache size
    *** @param maxSize  The maximum cache size
    *** @return The set maximum cache size, or -1 if unable to set the size.
    **/
    public int setMaximumCacheSize(int maxSize);

    /**
    *** Gets the maximum cache size
    *** @return The maximum cache size, or -1 if unable to retrieve the size.
    **/
    public int getMaximumCacheSize();

    // ------------------------------------------------------------------------

    /**
    *** Add entry to cache
    *** @param key  The key to add
    *** @param val  The value to associate with the specified key
    **/
    public void addValue(KEY key, VAL val);

    /**
    *** Returns true if the specified key exists in the cache
    *** @param key  The key to check for existence
    *** @return True if the specified key exists in the cache, false otherwise
    **/
    public boolean hasValue(KEY key);

    /**
    *** Get entry from cache
    *** @param key  The key of the value to retrieve
    *** @param dft  The default value if the key does not exist
    *** @return The value for the specified key, or specified default if the key does not exist
    **/
    public VAL getValue(KEY key, VAL dft);

}
