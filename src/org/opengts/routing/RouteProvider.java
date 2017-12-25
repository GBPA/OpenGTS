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

public interface RouteProvider
{

    // ------------------------------------------------------------------------

    /** 
    *** Returns the name of this RouteProvider 
    **/
    public String getName();

    /**
    *** Returns true if this RouteProvider is enabled
    *** @return True if this RouteProvider is enabled, false otherwise
    **/
    public boolean isEnabled();

    /** 
    *** Return true if this operation will take less than 20ms to complete 
    *** (The returned value is used to determine whether the 'getRoute' operation
    *** should be performed immediately, or lazily.)
    **/
    public boolean isFastOperation();

    // ------------------------------------------------------------------------

    /**
    *** Returns a GeoRoute for the requested route
    *** @param fromAddr  The from address
    *** @param toAddr    The to address
    *** @param localeStr The locale
    *** @param attr      The routing attributes
    *** @return The requested route
    **/
    public GeoRoute getDirections(String fromAddr, String toAddr, Locale locale, RTProperties attr);

    // ------------------------------------------------------------------------

    /**
    *** Returns a GeoRoute for the requested path which has been snapped to the nearest road
    *** @param path      An array of GeoPoints representing the path
    *** @param localeStr The locale
    *** @param attr      The routing attributes
    *** @return The requested route
    **/
    public GeoRoute getSnapToRoad(GeoPointProvider path[], Locale locale, RTProperties attr);

    // ------------------------------------------------------------------------

}
