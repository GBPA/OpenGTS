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
//  2009/08/07  Martin D. Flynn
//     -Initial release
//  2012/06/29  Martin D. Flynn
//     -Added "getGpsAge", "getCreationAge"
// ----------------------------------------------------------------------------
package org.opengts.db;
 
import java.lang.*;
import java.util.*;

import org.opengts.util.*;
import org.opengts.db.tables.Account;
import org.opengts.db.tables.Device;
import org.opengts.db.tables.EventData;

public class EventDataProviderAdapter
    implements EventDataProvider /* GeoPointProvider */
{

    public EventDataProviderAdapter()
    {
        super();
    }

    // ------------------------------------------------------------------------

    public String getAccountID()
    {
        return null; // should be overridden
    }

    // ------------------------------------------------------------------------

    public String getDeviceID()
    {
        return null; // should be overridden
    }

    public String getDeviceDescription() {
        return ""; 
    }

    public String getDeviceVIN() {
        return ""; 
    }

    // ------------------------------------------------------------------------

    public long getTimestamp()
    {
        return 0L;
    }

    // ------------------------------------------------------------------------

    public int getStatusCode()
    {
        return StatusCodes.STATUS_NONE;
    }

    public String getStatusCodeDescription(BasicPrivateLabel bpl)
    {
        return "";
    }

    public StatusCodeProvider getStatusCodeProvider(BasicPrivateLabel bpl)
    {
        return null;
    }

    // ------------------------------------------------------------------------

    public int getPushpinIconIndex(String iconSelector, OrderedMap<String,PushpinIcon> iconMap,
        boolean isFleet, BasicPrivateLabel bpl)
    {
        return 0; // black
    }

    // ------------------------------------------------------------------------

    public boolean isValidGeoPoint()
    {
        double lat = this.getLatitude();
        double lon = this.getLongitude();
        return GeoPoint.isValid(lat, lon);
    }

    private int latRecurs = 0;  // not thread safe
    public double getLatitude()
    {
        // -- should be overridden
        double lat = 0.0;
        if (this.latRecurs == 0) { // not thread safe
            // -- recursion check in case 'getGeoPoint' is defined in terms of 'getLatitude'/'getLongitude'
            this.latRecurs++;
            GeoPoint gp = this.getGeoPoint();
            this.latRecurs--;
            lat = (gp != null)? gp.getLatitude() : 0.0;
        }
        return lat;
    }

    private int lonRecurs = 0;  // not thread safe
    public double getLongitude()
    {
        // -- should be overridden
        double lon = 0.0;
        if (this.lonRecurs == 0) { // not thread safe
            // -- recursion check in case 'getGeoPoint' is defined in terms of 'getLatitude'/'getLongitude'
            this.lonRecurs++;
            GeoPoint gp = this.getGeoPoint();
            this.lonRecurs--;
            lon = (gp != null)? gp.getLongitude() : 0.0;
        }
        return lon;
    }

    private int gpRecurs = 0;  // not thread safe
    public GeoPoint getGeoPoint()
    {
        // -- should be overridden
        GeoPoint gp = GeoPoint.INVALID_GEOPOINT;
        if (this.gpRecurs == 0) { // not thread safe
            // -- recursion check in case 'getLatitude'/'getLongitude' are defined in terms of 'getGeoPoint'
            this.gpRecurs++;
            double lat = this.getLatitude();
            double lon = this.getLongitude();
            this.gpRecurs--;
            if (GeoPoint.isValid(lat,lon)) {
                gp = new GeoPoint(lat,lon);
            }
        }
        return gp;
    }

    // ------------------------------------------------------------------------

    public long getGpsAge()
    {
        return 0L;
    }

    public long getCreationAge()
    {
        return 0L;
    }

    public double getHorzAccuracy()
    {
        return 0.0;
    }
    
    public GeoPoint getBestGeoPoint()
    {
        return this.getGeoPoint();
    }
    
    public double getBestAccuracy()
    {
        return -1.0;
    }

    public int getSatelliteCount()
    {
        return 0;
    }

    public double getBatteryLevel()
    {
        return 0.0;
    }

    public double getBatteryVolts()
    {
        return 0.0;
    }

    public double getVBatteryVolts()
    {
        return 0.0;
    }

    public double getSpeedKPH()
    {
        return 0.0;
    }

    public double getHeading()
    {
        return 0.0;
    }

    public double getAltitude()
    {
        return 0.0;
    }

    public String getGeozoneID()
    {
        return "";
    }

    public String getAddress()
    {
        return "";
    }

    public long   getInputMask()
    {
        return 0L;
    }

    public double getOdometerKM()
    {
        return 0.0;
    }

    // ------------------------------------------------------------------------

    private int eventIndex = -1;
    public void setEventIndex(int ndx)
    {
        this.eventIndex = ndx;
    }

    public int getEventIndex()
    {
        return this.eventIndex;
    }

    public boolean getIsFirstEvent()
    {
        return (this.getEventIndex() == 0);
    }

    // ------------------------------------------------------------------------
    
    private boolean isLastEvent = false;

    public void setIsLastEvent(boolean isLast)
    {
        this.isLastEvent = isLast;
    }

    public boolean getIsLastEvent()
    {
        return this.isLastEvent;
    }

    // ------------------------------------------------------------------------

}
