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
//  Support for Google Roads API
// ----------------------------------------------------------------------------
// Change History:
//  2016/09/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.routing.google;

import java.util.*;
import java.io.*;
import java.net.*;

import org.opengts.util.*;
import org.opengts.routing.*;
import org.opengts.google.*;

public class GoogleRouteProvider
    extends RouteProviderAdapter
{

    // ------------------------------------------------------------------------
    // -- Properties

    /* Roads API properties */
    public    static final String   PROP_roadsApiKey        = GoogleRoads.PROP_roadsApiKey;
    public    static final String   PROP_snapToRoadsURL     = GoogleRoads.PROP_snapToRoadsURL;
    public    static final String   PROP_maxSnapDistanceM   = GoogleRoads.PROP_maxSnapDistanceM;

    /* Directions API properties */
    public    static final String   PROP_directionsApiKey   = GoogleDirections.PROP_directionsApiKey;
    public    static final String   PROP_directionsURL      = GoogleDirections.PROP_directionsURL;

    // ------------------------------------------------------------------------

    private GoogleRoads         roadsAPI        = null;
    private GoogleDirections    directionsAPI   = null;

    /**
    *** Constructor
    *** @param name    The name of this RouteProvider
    *** @param key     The access key (may be null)
    *** @param rtProps The properties (may be null)
    **/
    public GoogleRouteProvider(String name, String key, RTProperties rtProps)
    {
        super(name, key, rtProps);
        this.roadsAPI      = new GoogleRoads(rtProps);
        this.directionsAPI = new GoogleDirections(rtProps);
    }

    // ------------------------------------------------------------------------

    /**
    *** Not a fast opertation
    **/
    public boolean isFastOperation()
    {
        return false;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the GeoRoute with directions from the specified 'fromAddr' to the specified 'toAddr'.
    *** @param fromAddr  The origination address
    *** @param toAddr    The destination address
    *** @param locale    The preferred locale
    *** @param attr      The additional attributes used to determine the returned route
    *** @return The GeoRoute
    **/
    public GeoRoute getDirections(String fromAddr, String toAddr, Locale locale, RTProperties attr)
    {
        return this.directionsAPI.getDirections(fromAddr, toAddr, locale, attr);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a GeoRoute of the requested path which has been snapped to the nearest road
    *** @param path      An array of GeoPoints representing the path
    *** @param locale    The locale
    *** @param attr      The routing attributes
    *** @return The requested route
    **/
    public GeoRoute getSnapToRoad(GeoPointProvider path[], Locale locale, RTProperties attr)
    {
        // -- add "interprolate"
        if (attr == null) {
            attr = new RTProperties();
            attr.setBoolean(GoogleRoads.ATTR_interpolate,true);
        } else
        if (!attr.hasProperty(GoogleRoads.ATTR_interpolate)) {
            attr.setBoolean(GoogleRoads.ATTR_interpolate,true);
        }
        // --
        GeoRoute route = this.roadsAPI.getSnapToRoad(path, locale, attr);
        if (route == null) {
            return null;
        } else {
            //route.printRoute();
            return route;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
