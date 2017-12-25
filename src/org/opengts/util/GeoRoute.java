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
package org.opengts.util;

import java.util.*;

import org.opengts.util.*;

public class GeoRoute
    implements Cloneable, GeoPointProvider
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* routing node properties */
    public static final String NODE_originalIndex       = "originalIndex";      // Integer
    public static final String NODE_elevationMeters     = "elevationMeters";    // Double
    // --
    public static final String NODE_instructions        = "instructions";       // String
    public static final String NODE_maneuver            = "maneuver";           // String
    public static final String NODE_summary             = "summary";            // GeoPointProvider[]
    public static final String NODE_elapsedTimeMS       = "elapsedTimeMS";      // Long
    public static final String NODE_warningMessage      = "warningMessage";     // String
    public static final String NODE_firstOriginalIndex  = "firstOriginalIndex"; // Integer
    public static final String NODE_lastOriginalIndex   = "lastOriginalIndex";  // Integer

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** GeoPoint subclass that can hold properties
    **/
    public static class GPNode // GeoRoute.GPNode
        extends GeoPoint.GeoPointProp
    {
        public GPNode(double lat, double lon) {
            super(lat,lon);
        }
        public GPNode(GPNode node) {
            super(node);
        }
        public Object clone() {
            return new GPNode(this);
        }
        // --
        public void setOriginalIndex(int ndx) {
            // -- The index of this GeoPoint specified in the original routing request 
            // -  that produced this GeoRoute, or -1 if this GeoPoint was not part of 
            // -  original routing request.
            this.getProperties().setInt(GeoRoute.NODE_originalIndex, ndx);
        }
        public int getOriginalIndex() {
            return this.getPropertyInt(GeoRoute.NODE_originalIndex, -1);
        }
        // --
        public void setElevationMeters(double elevM) {
            this.getProperties().setDouble(GeoRoute.NODE_elevationMeters, elevM);
        }
        public double getElevationMeters() {
            return this.getPropertyDouble(GeoRoute.NODE_elevationMeters, 0.0);
        }
        // --
        public String toString() {
            if (!this.hasProperties()) {
                return super.toString();
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append(super.toString());
                sb.append(" [").append(this.getProperties().toString()).append("]");
                return sb.toString();
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Vector<? extends GeoPointProvider>  pathList    = null;
    private double                              distanceKM  = 0.0;

    private RTProperties                        props       = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public GeoRoute()
    {
        super();
    }

    /**
    *** Constructor
    **/
    public GeoRoute(GeoPointProvider gpa[])
    {
        this();
        this.setPathList(ListTools.toList(gpa,null));
    }

    /**
    *** Constructor
    **/
    public GeoRoute(java.util.List<? extends GeoPointProvider> gpl)
    {
        this();
        this.setPathList(gpl);
    }

    /**
    *** Copy Constructor
    **/
    public GeoRoute(GeoRoute route)
    {
        this();
        if (route != null) {
            this.setPathList(route.pathList);
            if (route.hasProperties()) {
                this.props = new RTProperties(route.getProperties());
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this GeoRoute is valid
    **/
    public boolean isValid()
    {
        return !ListTools.isEmpty(this.pathList);
    }

    /**
    *** Returns true if the specified GeoRoute is valid
    **/
    public static boolean isValid(GeoRoute route)
    {
        return ((route != null) && route.isValid())? true : false;
    }


    // ------------------------------------------------------------------------

    /**
    *** Return true if this instance has defined properties
    **/
    public boolean hasProperties()
    {
        return ((this.props != null) && !this.props.isEmpty())? true : false;
    }

    /**
    *** Gets the properties for this instance
    **/
    public RTProperties getProperties()
    {
        if (this.props == null) {
            this.props = new RTProperties();
        }
        return this.props;
    }

    // --------------------------------

    /**
    *** Gets a String value from the properties
    **/
    public String getStringProperty(String key, String dft)
    {
        return this.hasProperties()? this.getProperties().getString(key,dft) : dft;
    }

    /**
    *** Gets an int value from the properties
    **/
    public int getIntProperty(String key, int dft)
    {
        return this.hasProperties()? this.getProperties().getInt(key,dft) : dft;
    }

    /**
    *** Gets a long value from the properties
    **/
    public long getLongProperty(String key, long dft)
    {
        return this.hasProperties()? this.getProperties().getLong(key,dft) : dft;
    }

    /**
    *** Gets a double value from the properties
    **/
    public double getDoubleProperty(String key, double dft)
    {
        return this.hasProperties()? this.getProperties().getDouble(key,dft) : dft;
    }

    // --------------------------------
    
    /** 
    *** Sets the instructions for this route segment
    **/
    public void setInstructions(String instructions)
    {
        if (!StringTools.isBlank(instructions)) {
            this.getProperties().setString(GeoRoute.NODE_instructions, instructions);
        } else
        if (this.hasProperties()) {
            this.getProperties().removeProperty(GeoRoute.NODE_instructions);
        }
    }

    /**
    *** Gets the instructions for this route segment (may be null);
    **/
    public String getInstructions()
    {
        return this.hasProperties()? this.getProperties().getString(GeoRoute.NODE_instructions,null) : null;
    }

    // --------------------------------
    
    /** 
    *** Sets the maneuver for this route segment
    **/
    public void setManeuver(String maneuver)
    {
        if (!StringTools.isBlank(maneuver)) {
            this.getProperties().setString(GeoRoute.NODE_maneuver, maneuver);
        } else
        if (this.hasProperties()) {
            this.getProperties().removeProperty(GeoRoute.NODE_maneuver);
        }
    }

    /**
    *** Gets the maneuver for this route segment (may be null);
    **/
    public String getManeuver()
    {
        return this.hasProperties()? this.getProperties().getString(GeoRoute.NODE_maneuver,null) : null;
    }

    // --------------------------------

    /**
    *** Sets the summary GeoPoint set for this route
    **/
    public void setSummary(GeoPointProvider gpp[])
    {
        if (gpp != null) {
            this.getProperties().setProperty(GeoRoute.NODE_summary, gpp);
        } else
        if (this.hasProperties()) {
            this.getProperties().removeProperty(GeoRoute.NODE_summary);
        }
    }

    /**
    *** Gets the summary GeoPoint set for this route
    **/
    public GeoPointProvider[] getSummary()
    {
        return this.hasProperties()? (GeoPointProvider[])this.getProperties().getProperty(GeoRoute.NODE_summary,null) : null;
    }

    // --------------------------------

    /**
    *** Set the number of milliseconds elasped in the creation of this GeoRoute
    **/
    public void setElapsedTimeMS(long elapsedMS)
    {
        if (elapsedMS >= 0L) {
            this.getProperties().setLong(GeoRoute.NODE_elapsedTimeMS, elapsedMS);
        } else
        if (this.hasProperties()) {
            this.getProperties().removeProperty(GeoRoute.NODE_elapsedTimeMS);
        }
    }

    /**
    *** Gets the number of milliseconds elapsed in the creation of this GeoRoute
    **/
    public long getElapsedTimeMS()
    {
        return this.hasProperties()? this.getProperties().getLong(GeoRoute.NODE_elapsedTimeMS,-1L) : -1L;
    }

    // --------------------------------
    
    /** 
    *** Sets the warning message for this route segment
    **/
    public void setWarningMessage(String msg)
    {
        if (!StringTools.isBlank(msg)) {
            this.getProperties().setString(GeoRoute.NODE_warningMessage, msg);
        } else
        if (this.hasProperties()) {
            this.getProperties().removeProperty(GeoRoute.NODE_warningMessage);
        }
    }

    /**
    *** Gets the warning message for this route segment (may be null);
    **/
    public String getWarningMessage()
    {
        return this.hasProperties()? this.getProperties().getString(GeoRoute.NODE_warningMessage,null) : null;
    }

    // --------------------------------

    /** 
    *** Sets the first original point index.
    *** @param ndx  This value should provide the index of the first lat/lon point 
    ***         specified in the original routing request that produced this GeoRoute.
    **/
    public void setFirstOriginalIndex(int ndx)
    {
        if (ndx >= 0) {
            this.getProperties().setInt(GeoRoute.NODE_firstOriginalIndex, ndx);
        } else
        if (this.hasProperties()) {
            this.getProperties().removeProperty(GeoRoute.NODE_firstOriginalIndex);
        }
    }

    /**
    *** Gets the first original point index.
    *** @return The index of the first lat/lon point specified in the original routing 
    ***         request that produced this GeoRoute.
    **/
    public int getFirstOriginalIndex()
    {
        return this.hasProperties()? this.getProperties().getInt(GeoRoute.NODE_firstOriginalIndex,-1) : -1;
    }

    // --------------------------------
    
    /** 
    *** Sets the last original point index.
    *** @param ndx  This value should provide the index of the last lat/lon point 
    ***         specified in the original routing request that produced this GeoRoute.
    **/
    public void setLastOriginalIndex(int ndx)
    {
        if (ndx >= 0) {
            this.getProperties().setInt(GeoRoute.NODE_lastOriginalIndex, ndx);
        } else
        if (this.hasProperties()) {
            this.getProperties().removeProperty(GeoRoute.NODE_lastOriginalIndex);
        }
    }

    /**
    *** Gets the last original point index.
    *** @return The index of the last lat/lon point specified in the original routing 
    ***         request that produced this GeoRoute.
    **/
    public int getLastOriginalIndex()
    {
        return this.hasProperties()? this.getProperties().getInt(GeoRoute.NODE_lastOriginalIndex,-1) : -1;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if a path is defined
    **/
    public boolean hasPathList()
    {
        return !ListTools.isEmpty(this.pathList)? true : false;
    }

    /**
    *** Returns the internal path-List instance (may be null)
    **/
    public java.util.List<? extends GeoPointProvider> getPathList()
    {
        return this.pathList;
    }

    /**
    *** Sets the latitude/longitude path (shallow copy)
    **/
    public void setPathList(java.util.List<? extends GeoPointProvider> gpl)
    {
        this.pathList   = null;
        this.distanceKM = 0.0;
        if (!ListTools.isEmpty(gpl)) {
            Vector<GeoPointProvider> ngpl = new Vector<GeoPointProvider>();
            GeoPoint lastGP = null;
            for (GeoPointProvider gpp : gpl) {
                if (gpp instanceof GeoRoute) {
                    GeoRoute subRoute = (GeoRoute)gpp;
                    if (GeoRoute.isValid(subRoute)) {
                        GeoRoute gr = new GeoRoute(subRoute);
                        ngpl.add(gr);
                        this.distanceKM += gr.getDistanceKM();
                        lastGP = gr.getLastGeoPoint();
                    } else {
                        // -- invalid GeoRoute (ignore)
                    }
                } else
                if (GeoPoint.isValid(gpp)) {
                    ngpl.add(gpp);
                    GeoPoint gp = gpp.getGeoPoint();
                    if (GeoPoint.isValid(lastGP)) {
                        this.distanceKM += lastGP.kilometersToPoint(gp);
                    }
                    lastGP = gp;
                } else {
                    // -- invalid GeoPointProvider (ignore)
                }
            }
            if (!ListTools.isEmpty(ngpl)) {
                this.pathList = ngpl;
            }
        }
    }

    // --------------------------------

    /**
    *** Returns the number of GeoPoints in this route
    **/
    public int getPathLength()
    {
        return ListTools.size(this.pathList);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the first/starting GeoPoint
    **/
    public GeoPoint getFirstGeoPoint()
    {
        java.util.List<? extends GeoPointProvider> PL = this.getPathList();
        if (!ListTools.isEmpty(PL)) {
            GeoPointProvider gpp = PL.get(0);
            if (gpp instanceof GeoRoute) {
                return ((GeoRoute)gpp).getFirstGeoPoint();
            } else {
                return gpp.getGeoPoint();
            }
        } else {
            return null;
        }
    }

    /**
    *** Gets the last/ending GeoPoint
    **/
    public GeoPoint getLastGeoPoint()
    {
        java.util.List<? extends GeoPointProvider> PL = this.getPathList();
        if (!ListTools.isEmpty(PL)) {
            GeoPointProvider gpp = PL.get(PL.size() - 1);
            if (gpp instanceof GeoRoute) {
                return ((GeoRoute)gpp).getLastGeoPoint();
            } else {
                return gpp.getGeoPoint();
            }
        } else {
            return null;
        }
    }

    /**
    *** GeoPointProvider interface
    **/
    public GeoPoint getGeoPoint()
    {
        // -- arbitrary, do not count on this value
        return this.getFirstGeoPoint();
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the latitude/longitude path (shallow copy)
    **/
    public void setPath(java.util.List<? extends GeoPointProvider> pathL)
    {
        this.setPathList(pathL);
    }

    /**
    *** Gets a copy of the full latitude/longitude path
    *** @param pathList  The list object into which the path is copied
    *** @return The List object containing the copied path (never null)
    **/
    public java.util.List<? extends GeoPointProvider> getPath(java.util.List<GeoPointProvider> pathL)
    {
        // -- make sure we have a path List
        if (pathL == null) {
            pathL = new Vector<GeoPointProvider>();
        }
        // -- copy in path
        if (this.hasPathList()) {
            for (GeoPointProvider gpp : this.getPathList()) {
                if (gpp instanceof GeoRoute) {
                    ((GeoRoute)gpp).getPath(pathL);
                } else
                if (GeoPoint.isValid(gpp)) {
                    pathL.add(gpp);
                }
            }
        }
        // -- return path List
        return pathL; // does not return null
    }

    /**
    *** Gets the latitude/longitude path
    **/
    public GeoPointProvider[] getPath()
    {
        if (this.hasPathList()) {
            java.util.List<GeoPointProvider> pathGP = new Vector<GeoPointProvider>();
            this.getPath(pathGP);
            if (!ListTools.isEmpty(pathGP)) {
                return pathGP.toArray(new GeoPointProvider[pathGP.size()]);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the accumulated distance of the route path
    **/
    public double getDistanceKM()
    {
        if ((this.distanceKM <= 0.0) && this.hasPathList()) {
            this.distanceKM = 0.0;
            GeoPoint lastGP = null;
            for (GeoPointProvider gpp : this.getPathList()) {
                if (gpp instanceof GeoRoute) {
                    this.distanceKM += ((GeoRoute)gpp).getDistanceKM();
                    lastGP = ((GeoRoute)gpp).getLastGeoPoint();
                } else
                if (GeoPoint.isValid(gpp)) {
                    GeoPoint gp = gpp.getGeoPoint();
                    if (GeoPoint.isValid(lastGP)) {
                        this.distanceKM += lastGP.kilometersToPoint(gp);
                    }
                    lastGP = gp;
                } else {
                    // -- should never occur
                    Print.logError("Invalid GeoPointProvider found in path list");
                }
            }
        }
        return this.distanceKM;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a clone of this GeoRoute
    **/
    public Object clone()
    {
        return new GeoRoute(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a String representation of this instance
    **/
    public String toString()
    {
        GeoPointProvider gppa[] = this.getPath();
        StringBuffer sb = new StringBuffer();
        sb.append(ListTools.size(gppa)).append(" points ");
        sb.append("[").append(StringTools.format(this.getDistanceKM(),"0.0")).append("km] ");
        if (!ListTools.isEmpty(gppa)) {
            for (int i = 0; i < gppa.length; i++) {
                if (i > 0) { sb.append(","); }
                GeoPoint gp = gppa[i].getGeoPoint();
                sb.append(""+gp);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------

    /**
    *** Print the contents of this route to stdout
    **/
    public void printRoute()
    {
        this._printRoute(0);
    }

    /**
    *** Print the contents of this route to stdout
    **/
    private void _printRoute(int recurseLvl)
    {
        String indent   = StringTools.replicateString("  ",recurseLvl);
        long   elapseMS = this.getElapsedTimeMS();
        String instruct = StringTools.blankDefault(this.getInstructions(),"");
        String maneuver = StringTools.blankDefault(this.getManeuver(),"n/a");
        double distKM   = this.getDistanceKM();
        // --
        Print.logInfo(indent + recurseLvl + ") -----------------------------------------");
        if (elapseMS >= 0L) {
        Print.logInfo(indent + "Elapsed Time: " + elapseMS + " ms");
        }
        if (!StringTools.isBlank(instruct)) {
        Print.logInfo(indent + "Instructions: " + instruct);
        }
        if (!StringTools.isBlank(instruct)) {
        Print.logInfo(indent + "Maneuver    : " + maneuver);
        }
        Print.logInfo(indent + "Distance(km): " + StringTools.format(distKM,"0.0"));
        Print.logInfo(indent + "Point Count : " + this.getPathLength());
        // --
        if (this.hasPathList()) {
            java.util.List<? extends GeoPointProvider> path = this.getPathList();
            for (GeoPointProvider gpp : path) {
                if (gpp instanceof GeoRoute) {
                    ((GeoRoute)gpp)._printRoute(recurseLvl + 1);
                } else {
                    Print.logInfo(indent + "GeoPoint: " + gpp.getGeoPoint() + " [class: "+StringTools.className(gpp)+"]");
                }
            }
        }
        // --
        if (recurseLvl == 0) {
            Print.logInfo(indent + "Done) -------------------------------------------");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
