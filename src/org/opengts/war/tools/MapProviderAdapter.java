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
//  2008/02/11  Martin D. Flynn
//     -Initial release
//  2008/04/11  Martin D. Flynn
//     -Modified to maintain its own set of properties
//  2008/06/20  Martin D. Flynn
//     -Disregard 'PROP_auto_count_...' when checking for AutoUpdateEnabled.
//  2008/07/27  Martin D. Flynn
//     -Added RequestProperties argument to 'getPushpinIconMap(..)'
//     -Removed 'getPushpinIconURL' and 'getPushpinShadowURL'
//  2008/08/24  Martin D. Flynn
//     -Added 'getReplayEnabled()' and 'getReplayInterval()' methods.
//  2008/09/19  Martin D. Flynn
//     -Added 'getAutoUpdateOnLoad()' method.
//  2009/01/28  Martin D. Flynn
//     -Added 'toString()' to return MapProvider name
//  2009/09/23  Martin D. Flynn
//     -Added support for customizing the Geozone map width/height
//  2009/11/01  Martin D. Flynn
//     -Added ability to distinguish between device/fleet maps when returning the
//      maximum number of allowed pushpins (see "getMaxPushpins")
//  2009/04/11  Martin D. Flynn
//     -"getMaxPushpins" modified to support a 'report' type limit as well.
//  2013/08/06  Martin D. Flynn
//     -Fixed Legend localization
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.routing.RouteProvider;

public abstract class MapProviderAdapter
    implements MapProvider
{
    
    // ------------------------------------------------------------------------

    private static final String     DFT_AUTO_ENABLED           = "false";
    private static final String     DFT_AUTO_ONLOAD            = "false";
    private static final long       DFT_AUTO_DURATION          = DateTime.MinuteSeconds(20);
    private static final long       DFT_AUTO_INTERVAL          = DateTime.MinuteSeconds(1);
    private static final long       DFT_AUTO_MAXCOUNT          = DFT_AUTO_DURATION / DFT_AUTO_INTERVAL;

    private static final boolean    DFT_REPLAY_ENABLED         = false;
    private static final long       DFT_REPLAY_INTERVAL        = 1000L;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String ID_ZONE_LATITUDE(int ndx)
    {
        return MapProvider.ID_ZONE_LATITUDE_ + ndx;
    }

    public static String ID_ZONE_LONGITUDE(int ndx)
    {
        return MapProvider.ID_ZONE_LONGITUDE_ + ndx;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                          mapProviderName     = null;
    private String                          mapAuthorization    = null;
    private RTProperties                    mapProperties       = null;
    private MapDimension                    mapDimension        = null;

    private MapDimension                    mapZoneDimension    = null;
    
    //private Map<String,String>              zoomRegions         = null;

    private long                            mapFeatures         = 0L;

    private OrderedMap<String,PushpinIcon>  pushpinIconMap      = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor 
    *** @param name  This MapProvider name
    *** @param key   This MapProvider key
    **/
    public MapProviderAdapter(String name, String key)
    {
        this(name, key, 0L);
    }

    /**
    *** Constructor 
    *** @param name  This MapProvider name
    *** @param key   This MapProvider key
    **/
    public MapProviderAdapter(String name, String key, long featureMask)
    {
        super();
        this.mapProviderName  = (name != null)? name : "";
        this.mapAuthorization = (key != null)? key : "";
        this.mapFeatures      = featureMask;
    }

    // ------------------------------------------------------------------------

    /**
    *** Called after initialization of this MapProvider.  This allows the MapProvider
    *** to perform any required initialization after all attributes have been set 
    **/
    public void postInit()
    {
        // -- override implementation
    }
    
    // ------------------------------------------------------------------------
    
    /**
    *** Sets the supported map features
    *** @param featureMask  The mask containing the supported features
    **/
    public void setSupportedFeatures(long featureMask)
    {
        this.mapFeatures = featureMask;
    }
    
    /**
    *** Adds the specified feature to the list of supported map features
    *** @param feature  The feature to add to the supported features
    **/
    public void addSupportedFeature(long feature)
    {
        this.mapFeatures |= feature;
    }

    /**
    *** Returns true if the specified map feature is supported
    *** @param feature  The feature tested for support
    *** @return True if the specified feature is supported
    **/
    public boolean isFeatureSupported(long feature)
    {
        return ((feature & this.mapFeatures) != 0L);
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Performs simple validation checks on the authorization key, etc, and returns
    *** true is the validation was successful.
    *** @return  True if the validation checks are successful, false otherwise.
    **/
    public boolean validate()
    {
        // override to perform validation checks
        return true;
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets this MapProvider's name
    *** @return  The map provider name
    **/
    public String getName()
    {
        return (this.mapProviderName != null)? this.mapProviderName : "";
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets this MapProvider's authorization key
    *** @return  The map provider authorization key
    **/
    public String getAuthorization()
    {
        return (this.mapAuthorization != null)? this.mapAuthorization : "";
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

    /**
    *** Sets this MapProvider's properties
    *** @param props  The map provider properties
    **/
    public void setProperties(String props)
    {
        RTProperties rtp = this.getProperties();
        rtp.setProperties(props);
        this.mapDimension = null;
    }
    
    /**
    *** Adds a property key/value to this MapProvider
    *** @param key  The property key
    *** @param val  The property value
    **/
    public void setProperty(String key, String val)
    {
        if ((key != null) && !key.equals("")) {
            RTProperties rtp = this.getProperties();
            rtp.setProperty(key, ((val != null)? val : ""));
            this.mapDimension = null;
        }
    }

    /**
    *** Gets this MapProvider's properties
    *** @return  The map provider properties
    **/
    public RTProperties getProperties()
    {
        if (this.mapProperties == null) {
            this.mapProperties = new RTProperties();
            this.mapProperties.setPropertySeparatorChar(';');
        }
        return this.mapProperties;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the zoom regions
    *** @param The zoon regions 
    **/
    /*
    public void setZoomRegions(Map<String,String> map)
    {
        this.zoomRegions = map;
    }
    */

    /**
    *** Returns the zoom regions
    *** @return The zoon regions 
    **/
    /*
    public Map<String,String> getZoomRegions()
    {
        this.zoomRegions = null; // always force to null for now.
        if (this.zoomRegions == null) {
            String zoomRegion_ = "zoomRegion.";
            Map<String,String> zrMap = new OrderedMap<String,String>();
            RTProperties rtp = this.getProperties();
            Set<String> zrKeys = rtp.getPropertyKeys(zoomRegion_);
            if (!ListTools.isEmpty(zrKeys)) {
                for (String zrk : zrKeys) {
                    String zn = zrk.substring(zoomRegion_.length());
                    String zv = rtp.getString(zrk);
                    zrMap.put(zn,zv);
                }
            }
            this.zoomRegions = zrMap;
        }
        return this.zoomRegions;
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Sets this MapProvider's icon selector
    *** @param isFleet  True if the specified icon selector is for the fleet maps
    *** @param iconSel  The icon selector
    **/
    public void setIconSelector(boolean isFleet, String iconSel)
    {
        String key = isFleet? MapProvider.PROP_iconSel_fleet[0] : MapProvider.PROP_iconSelector[0];
        this.getProperties().setProperty(key, iconSel);
    }

    /**
    *** Gets this MapProvider's icon selector
    *** @param isFleet  True if the specified icon selector is for the fleet maps
    *** @return  The map provider icon selector
    **/
    public String getIconSelector(boolean isFleet)
    {
        RTProperties rtp = this.getProperties();
        if (isFleet) {
            // fleet icon selector
            String sel = rtp.getString(MapProvider.PROP_iconSel_fleet, null);
            if (!StringTools.isBlank(sel)) {
                return sel;
            }
        }
        // default icon selector
        return rtp.getString(MapProvider.PROP_iconSelector, null);
    }

    /**
    *** Gets this MapProvider's icon selector
    *** @return  The map provider icon selector
    **/
    public String getIconSelector(RequestProperties reqState)
    {
        boolean isFleet = (reqState != null) && reqState.isFleet();
        return this.getIconSelector(isFleet);
    }

    // ------------------------------------------------------------------------
    
    // <Legend>
    //    <Title><![CDATA[Pushpin Legend]]></Title>
    //    <Icon name="red"   ><![CDATA[Description]]></Icon>
    //    <Icon name="yellow"><![CDATA[Description]]></Icon>
    //    <Icon name="green" ><![CDATA[Description]]></Icon>
    // </Legend>
    private static final String TAG_Legend  = "Legend";
    private static final String TAG_Title   = "Title";
    private static final String TAG_Icon    = "Icon";
    private static final String ATTR_name   = "name";
    private static final String ATTR_url    = "url";
    private static final String ATTR_scale  = "scale";
    private static final String CSS_legend  = "mapProviderLegend";
    public String _getIconLegendHtml(String legendXml, RequestProperties reqState, boolean outputHtml)
    {
        String xml = legendXml.trim();
        if (StringTools.isBlank(xml)) {
            return "";
        }

        /* quick validation */
        if (!xml.startsWith("<"+TAG_Legend+">")) {
            Print.logError("IconLegend [<xml>:"+this.getName()+"]: Legend XML does not start with <"+TAG_Legend+">: \n" + xml);
            return "";
        }
        //Print.logInfo("Parsing Legend XML:\n" + xml);

        /* get XML doc */
        //xml = "<?xml version='1.0' encoding='UTF-8' standalone='no' ?>\n" + xml;
        Document xmlDoc = XMLTools.getDocument(xml);
        if (xmlDoc == null) {
            Print.logError("IconLegend [?:"+this.getName()+"]: Invalid Legend XML: \n" + xml);
            Print.logStackTrace("Invalid Legend XML");
            return "";
        }

        /* parse Legend xml */
        Element legend = xmlDoc.getDocumentElement();
        //return this._getIconLegendHtml(null, legend, reqState, outputHtml);
        String refName = "?:" + this.getName();
        Locale locale  = (reqState != null)? reqState.getLocale() : null;
        OrderedMap<String,PushpinIcon> iconMap = this.getPushpinIconMap(reqState);
        return MapProviderAdapter.GetIconLegendHtml(refName, locale, iconMap, null, legend, outputHtml);

    }

    public static String GetIconLegendHtml(String refName, Locale locale, 
        OrderedMap<String,PushpinIcon> pushpinMap, 
        String legendType, Element legendElem, 
        boolean outputHtml)
    {

        /* Icon tags */
        NodeList iconList = null;
        if (legendElem != null) {
            iconList = XMLTools.getChildElements(legendElem, TAG_Icon);
            if (iconList.getLength() <= 0) {
                // No "Icon" tags
                return "";
            }
        }
        //Print.logStackTrace("Legend Locale: " + locale);

        /* legend title */
        String legendTitle = null;
        if (legendElem != null) {
            NodeList titleList = XMLTools.getChildElements(legendElem, TAG_Title);
            if (titleList.getLength() > 0) {
                Element titleElem = (Element)titleList.item(0); // first item only
                legendTitle = XMLTools.getNodeText(titleElem," ",false).trim();
                //Print.logInfo("Legend Title (before): " + legendTitle);
                //legendTitle = RTConfig.insertKeyValues(titleText); <-- do not apply Locale here!
            } else 
            if (outputHtml) {
                I18N i18n = I18N.getI18N(MapProviderAdapter.class, locale);
                legendTitle = i18n.getString("MapProviderAdapter.legendTitle","Pushpin Legend");
            }
        } else {
            // legend.device.title=Pushpin Legend:
            String titleKey = "legend." + legendType + ".title";
            legendTitle = RTConfig.getString(titleKey, null);
        }
        //Print.logInfo("Legend Title: " + legendTitle);

        /* begin XML */
        StringBuffer sb = new StringBuffer();
        if (outputHtml) {
            // output HTML
        } else {
            // output XML
            sb.append("<"+TAG_Legend+">\n");
        }

        /* Legend Title output */
        if (!StringTools.isBlank(legendTitle)) {
            if (outputHtml) {
                sb.append("<span class=\""+CSS_legend+"\">" + legendTitle + "</span>\n");
            } else {
                sb.append("<"+TAG_Title+"><![CDATA[" + legendTitle + "]]></"+TAG_Title+">\n");
            }
        }
        
        /* begin icon table */
        if (outputHtml) {
            sb.append("<table class=\""+CSS_legend+"\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");
        } else {
            //
        }

        /* parse "Icons" */
        int iconCount = 0;
        int iconListLen = (iconList != null)? iconList.getLength() : 10;
        for (int i = 0; i < iconListLen; i++) {
            String  name  = null;
            String  url   = null;
            double  scale = 1.0;
            String  desc  = null;
            // Description Inportant Note: undefined keys are left as-is.
            // - The description may contain patterns such as ${mph:XX} or ${kph:XX} which are
            //   to be later, at the time the legend is displayed on the page.  This assumes
            //   that "mph" and "kph" are not defined at this point in the parsing process.

            /* icon attributes */
            Element iconElem = (iconList != null)? (Element)iconList.item(i) : null;
            if (iconElem != null) {
                name  = XMLTools.getAttribute(iconElem,ATTR_name,null,false);
                url   = XMLTools.getAttribute(iconElem,ATTR_url ,null,false);
                scale = StringTools.parseDouble(XMLTools.getAttribute(iconElem,ATTR_scale,null,false),0.0);
                desc  = XMLTools.getNodeText(iconElem,"",false).trim();
                //desc  = RTConfig.insertKeyValues(desc); <-- do not apply Locale here!
            } else {
                // legend.device.icon.1=greendot|0.7|More than 20 mph
                String iconKey  = "legend."+legendType+".icon."+(i+1);
                String iconItem = RTConfig.getString(iconKey, null);
                if (StringTools.isBlank(iconItem)) { 
                    break; // no more icons
                }
                String p[] = StringTools.split(iconItem,'|');
                if (p.length < 3) {
                    continue;
                }
                name  = StringTools.trim(p[0]);
                url   = null;
                scale = StringTools.parseDouble(p[1],0.0);
                desc  = StringTools.trim(p[2]);
            }

            /* find pushpin */
            PushpinIcon pp = pushpinMap.get(name);
            if (pp == null) {
                Print.logWarn("IconLegend ["+refName+"]: PushpinIcon not found: " + name);
                continue;
            }
            String imgUrl = !StringTools.isBlank(url)? url : pp.getImageURL(); // pp.getIconURL();
            if (StringTools.isBlank(imgUrl)) {
                Print.logWarn("IconLegend ["+refName+"]: Invalid PushpinIcon URL: " + imgUrl);
                continue;
            }

            /* output */
            if (outputHtml) {
                sb.append("<tr class=\""+CSS_legend+"\">");
                sb.append("<td class=\""+CSS_legend+"\">");
                if (pp != null) {
                    double S = (scale > 0.0)? scale : 1.0;
                    int    W = (int)Math.round(S * pp.getIconWidth()) ;
                    int    H = (int)Math.round(S * pp.getIconHeight());
                    sb.append("<img class=\""+CSS_legend+"\" src=\""+imgUrl+"\" width=\""+W+"\" height=\""+H+"\">");
                } else {
                    sb.append("<img class=\""+CSS_legend+"\" src=\""+imgUrl+"\">");
                }
                sb.append("</td>");
                sb.append("<td class=\""+CSS_legend+"\">");
                sb.append(desc);
                sb.append("</td>");
                sb.append("</tr>\n");
            } else {
                sb.append("<"+TAG_Icon+" "+ATTR_name+"=\""+name+"\"");
                if (scale > 0.0) {
                    sb.append(" "+ATTR_scale+"=\""+StringTools.format(scale,"0.0")+"\"");
                }
                if (!StringTools.isBlank(url)) {
                    sb.append(" "+ATTR_url+"=\""+url+"\"");
                }
                sb.append(">");
                sb.append("<![CDATA["+desc+"]]>");
                sb.append("</"+TAG_Icon+">\n");
            }
            iconCount++;
            
        }

        /* end output */
        if (outputHtml) {
            sb.append("</table>\n");
        } else {
            sb.append("</"+TAG_Legend+">");
        }
        //Print.logInfo("Legend output:\n" + sb);
        
        /* return result */
        return (iconCount > 0)? sb.toString() : "";
        
    }

    /**
    *** Sets this MapProvider's icon selector legend html
    *** @param isFleet  True if the specified legend is for the fleet maps
    *** @param legend   The legend HTML
    **/
    public void setIconSelectorLegend(boolean isFleet, String legend)
    {
        String key = isFleet? MapProvider.PROP_iconSel_fleet_legend[0] : MapProvider.PROP_iconSelector_legend[0];
        this.getProperties().setProperty(key, legend);
    }

    /**
    *** Gets this MapProvider's icon selector legend html
    *** @return  The map provider icon selector legend html
    **/
    public String getIconSelectorLegend(boolean isFleet)
    {
        RTProperties rtp = this.getProperties();
        String legend = isFleet?
            rtp.getString(MapProvider.PROP_iconSel_fleet_legend, null) :
            rtp.getString(MapProvider.PROP_iconSelector_legend, null);
        if (legend == null) {
            Print.logWarn("Legend is null");
            return null;
        } else 
        if (legend.startsWith("<"+TAG_Legend)) {
            String legendHtml = this._getIconLegendHtml(legend, null, true);
            legendHtml = RTConfig.insertKeyValues(legendHtml); // apply localization now
            //Print.logInfo("Legend: Found legend HTML display -\n" + legendHtml);
            return legendHtml;
        } else {
            String legendHtml = RTConfig.insertKeyValues(legend); // apply localization now
            //Print.logInfo("Legend: returning default legend HTML -\n" + legendHtml);
            return legendHtml;
        }
    }

    /**
    *** Gets this MapProvider's icon selector legend html
    *** @return  The map provider icon selector legend html
    **/
    public String getIconSelectorLegend(RequestProperties reqState)
    {
        if (reqState != null) {
            boolean isFleet = reqState.isFleet();
            if (isFleet) {
                DeviceGroup group = reqState.getSelectedDeviceGroup();
                if (group != null) {
                    String legendHtml = this._getIconLegendHtml(group.getMapLegend(), reqState, true);
                    if (!StringTools.isBlank(legendHtml)) {
                        //Print.logInfo("Legend: Found DeviceGroup legend display");
                        return legendHtml;
                    }
                }
            } else {
                Device device = reqState.getSelectedDevice();
                if (device != null) {
                    String legendHtml = this._getIconLegendHtml(device.getMapLegend(), reqState, true);
                    if (!StringTools.isBlank(legendHtml)) {
                        //Print.logInfo("Legend: Found Device legend display");
                        return legendHtml;
                    }
                }
            }
            Account account = reqState.getCurrentAccount();
            if (account != null) {
                String legendHtml = this._getIconLegendHtml(account.getMapLegend(isFleet), reqState, true);
                if (!StringTools.isBlank(legendHtml)) {
                    //Print.logInfo("Legend: Found Account legend display");
                    return legendHtml;
                }
            }
            return this.getIconSelectorLegend(isFleet);
        } else {
            return this.getIconSelectorLegend(false);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets this MapProvider's frame dimension for the Geozone map
    *** @return  The map provider frame dimension for the Geozone map
    **/
    public MapDimension getZoneDimension()
    {
        if (this.mapZoneDimension == null) {
            RTProperties rtp = this.getProperties();
            int w = rtp.getInt(MapProvider.PROP_zone_map_width , MapProvider.ZONE_WIDTH);
            int h = rtp.getInt(MapProvider.PROP_zone_map_height, MapProvider.ZONE_HEIGHT);
            this.mapZoneDimension = new MapDimension(w, h);
        }
        return this.mapZoneDimension;
    }

    /**
    *** Gets the MapProvider's frame width
    *** @return The map providers frame width
    ** /
    public int getZoneWidth()
    {
        return this.getZoneDimension().getWidth();
    }
    */

    /**
    *** Gets the MapProvider's frame height
    *** @return The map providers frame height
    ** /
    public int getZoneHeight()
    {
        return this.getZoneDimension().getHeight();
    }
    */

    // ------------------------------------------------------------------------

    /**
    *** Gets this MapProvider's frame dimension
    *** @return  The map provider frame dimension
    **/
    public MapDimension getDimension()
    {
        if (this.mapDimension == null) {
            RTProperties rtp = this.getProperties();
            if (rtp.getBoolean(MapProvider.PROP_map_fillFrame,false)) {
                this.mapDimension = new MapDimension(-1, -1);
            } else {
                int w = rtp.getInt(MapProvider.PROP_map_width , 0);
                int h = rtp.getInt(MapProvider.PROP_map_height, 0);
                if ((w != 0) && (h != 0)) {
                    this.mapDimension = new MapDimension(w, h);
                } else {
                    this.mapDimension = new MapDimension(MapProvider.MAP_WIDTH, MapProvider.MAP_HEIGHT);
                }
            }
        }
        return this.mapDimension;
    }

    /**
    *** Gets the MapProvider's frame width
    *** @return The map providers frame width
    **/
    public int getWidth()
    {
        return this.getDimension().getWidth();
    }

    /**
    *** Gets the MapProvider's frame height
    *** @return The map providers frame height
    **/
    public int getHeight()
    {
        return this.getDimension().getHeight();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the maximum number of allowed pushpins on the map
    *** @param reqState  The current session RequestProperties instance
    *** @return The maximum number of allowed pushpins
    **/
    public long getMaxPushpins(RequestProperties reqState)
    {
        RTProperties rtp = this.getProperties();
        if (reqState.isReport()) {
            String propName[] = MapProvider.PROP_maxPushpins_report;
            long mpp = rtp.getLong(propName, EventUtil.DFT_MAX_PUSHPIN_LIMIT);
            return (mpp > 0)? mpp : EventUtil.DFT_MAX_PUSHPIN_LIMIT;
        } else {
            String propName[] = reqState.isFleet()? 
                MapProvider.PROP_maxPushpins_fleet : 
                MapProvider.PROP_maxPushpins_device;
            long mpp = rtp.getLong(propName, EventUtil.DFT_MAX_PUSHPIN_LIMIT);
            return (mpp > 0)? mpp : EventUtil.DFT_MAX_PUSHPIN_LIMIT;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the default center GeoPoint (when no other points are displayed)
    *** @param dft  The default GeoPoint returned if this MapProvider does not explicitly define a point
    *** @return The default center GeoPoint
    **/
    public GeoPoint getDefaultCenter(GeoPoint dft)
    {
        RTProperties rtp = this.getProperties();
        double lat = rtp.getDouble(MapProvider.PROP_default_latitude , ((dft != null)? dft.getLatitude()  : MapProvider.DEFAULT_LATITUDE ));
        double lon = rtp.getDouble(MapProvider.PROP_default_longitude, ((dft != null)? dft.getLongitude() : MapProvider.DEFAULT_LONGITUDE));
        return new GeoPoint(lat, lon);
    }
    
    /**
    *** Gets the default zoom/scale level for this MapProvider
    *** @param dft  The default zoom/scale returned if this MapProvider does not explicitly define a value
    *** @param withPushpins  True to return the default zoom when pushpins are displayed
    *** @return The default zoom level
    **/
    public double getDefaultZoom(double dft, boolean withPushpins)
    {
        RTProperties rtp = this.getProperties();
        return withPushpins?
            rtp.getDouble(MapProvider.PROP_pushpin_zoom, dft) :
            rtp.getDouble(MapProvider.PROP_default_zoom, dft);
    }

    /**
    *** Gets the default zoom/scale level for this MapProvider when no pushpins are displayed
    *** @param dft  The default zoom/scale returned if this MapProvider does not explicitly define a value
    *** @return The default zoom level
    **/
    public int getDefaultZoom(int dft)
    {
        return this.getProperties().getInt(MapProvider.PROP_default_zoom, dft);
    }

    /**
    *** Gets the default zoom/scale level for this MapProvider when displaying pushpins
    *** @param dft  The default zoom/scale returned if this MapProvider does not explicitly define a value
    *** @return The default zoom level
    **/
    public int getPushpinZoom(int dft)
    {
        return this.getProperties().getInt(MapProvider.PROP_pushpin_zoom, dft);
    }

    /**
    *** Gets the default zoom/scale level for this MapProvider
    *** @param dft  The default zoom/scale returned if this MapProvider does not explicitly define a value
    *** @param withPushpins  True to return the default zoom when pushpins are displayed
    *** @return The default zoom level
    **/
    /*
    @Deprecated
    public int getDefaultZoom(int dft, boolean withPushpins)
    {
        return withPushpins? this.getPushpinZoom(dft) : this.getDefaultZoom(dft);
    }
    */

    // ------------------------------------------------------------------------
    
    /**
    *** Gets the auto-update enabled state for this MapProvider
    *** @return True if auto-update is enabled for this MapProvider
    **/
    public boolean getAutoUpdateEnabled(boolean isFleet)
    {
        String autoUpdate = isFleet?
            this.getProperties().getString(MapProvider.PROP_auto_enable_fleet , DFT_AUTO_ENABLED) :
            this.getProperties().getString(MapProvider.PROP_auto_enable_device, DFT_AUTO_ENABLED);
        if (autoUpdate.equalsIgnoreCase("false") || autoUpdate.equalsIgnoreCase("no")) {
            return false;
        } else
        if (this.getAutoUpdateInterval(isFleet) <= 0L) {
            return false;
        } else {
            return true;
        }
    }

    /**
    *** Gets the auto-update 'OnLoad' state for this MapProvider
    *** @return True if auto-update is to be automatically start on-load
    **/
    public boolean getAutoUpdateOnLoad(boolean isFleet)
    {
        RTProperties rtp = this.getProperties();

        /* check "alert.onload=true" */
        String autoOnload = isFleet?
            rtp.getString(MapProvider.PROP_auto_onload_fleet , "") :
            rtp.getString(MapProvider.PROP_auto_onload_device, "");
        if (!StringTools.isBlank(autoOnload)) {
            return StringTools.parseBoolean(autoOnload,false);
        }

        /* check "alert.enable=onload" */
        String autoEnable = isFleet?
            rtp.getString(MapProvider.PROP_auto_enable_fleet , "") :
            rtp.getString(MapProvider.PROP_auto_enable_device, "");
        return autoEnable.equalsIgnoreCase("onload");

    }

    /**
    *** Gets the auto-update interval for this MapProvider
    *** @return The auto-update interval
    **/
    public long getAutoUpdateInterval(boolean isFleet)
    {
        return isFleet?
            this.getProperties().getLong(MapProvider.PROP_auto_interval_fleet , DFT_AUTO_INTERVAL) :
            this.getProperties().getLong(MapProvider.PROP_auto_interval_device, DFT_AUTO_INTERVAL);
    }

    /**
    *** Gets the auto-update count for this MapProvider
    *** @return The auto-update count
    **/
    public long getAutoUpdateCount(boolean isFleet)
    {
        return isFleet?
            this.getProperties().getLong(MapProvider.PROP_auto_count_fleet , DFT_AUTO_MAXCOUNT) :
            this.getProperties().getLong(MapProvider.PROP_auto_count_device, DFT_AUTO_MAXCOUNT);
    }

    /**
    *** Gets the auto-update skip-radius (meters) for this MapProvider
    *** @return The auto-update skip-radius
    **/
    public double getAutoUpdateSkipRadius(boolean isFleet)
    {
        return isFleet?
            this.getProperties().getDouble(MapProvider.PROP_auto_radius_fleet , 0.0) :
            this.getProperties().getDouble(MapProvider.PROP_auto_radius_device, 0.0);
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Gets the replay enabled state for this MapProvider
    *** @return True if replay is enabled for this MapProvider
    **/
    public boolean getReplayEnabled()
    {
        if (!this.isFeatureSupported(MapProvider.FEATURE_REPLAY_POINTS)) {
            return false;
        } else
        if (!this.getProperties().getBoolean(MapProvider.PROP_replay_enable,DFT_REPLAY_ENABLED)) {
            return false;
        } else
        if (this.getReplayInterval() <= 0L) {
            return false;
        } else {
            return true;
        }
    }

    /**
    *** Gets the replay interval for this MapProvider (in milliseconds)
    *** @return The replay interval
    **/
    public long getReplayInterval()
    {
        long interval = this.getProperties().getLong(MapProvider.PROP_replay_interval, DFT_REPLAY_INTERVAL);
        return (interval < 30L)? (interval * 1000L) : interval;
    }

    /** 
    *** Returns true if only a single pushpin is to be displayed at a time during replay
    *** @return True if only a single pushpin is to be displayed at a time during replay
    **/
    public boolean getReplaySinglePushpin()
    {
        return this.getProperties().getBoolean(MapProvider.PROP_replay_singlePushpin, false);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the 'id' of the html tag block contain the map image
    *** @return The html tag block id
    **/
    public abstract String getMapID();

    // ------------------------------------------------------------------------

    /**
    *** Writes any required CSS to the specified PrintWriter.  This method is 
    *** intended to be overridden to provide the required behavior.
    *** @param out  The PrintWriter
    *** @param reqState The session RequestProperties
    **/
    public void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        // default to NO-OP
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes any required JavaScript to the html stream
    *** @param out   The handle to the html output stream
    *** @param state The current session state
    **/
    public abstract void writeJavaScript(PrintWriter out, RequestProperties state)
        throws IOException;

    // ------------------------------------------------------------------------

    /**
    *** Returns the style attributes for the displayed map cell
    *** @param reqState  The current session state
    *** @param mapDim    The specified map dimension
    *** @return The style attributes for the displayed map cell
    **/
    protected String getMapCellStyle(RequestProperties reqState, MapDimension mapDim)
    {
        MapDimension md = (mapDim != null)? mapDim : this.getDimension();
        int mapW = (md != null)? md.getWidth()  : -1;
        int mapH = (md != null)? md.getHeight() : -1;
        String styleW = (mapW > 0)? (mapW+"px") : "100%"; // 99%
        String styleH = (mapH > 0)? (mapH+"px") : "100%"; // 99%
        StringBuffer sb = new StringBuffer();
        sb.append("border:1px solid black;");
        sb.append("padding:0px;");
        sb.append("margin:0px;");
        sb.append("width:").append(styleW).append(";");
        sb.append("height:").append(styleH).append(";");
        return sb.toString();
    }

    /**
    *** Writes the map table view to the http output stream
    *** @param out      The http output stream
    *** @param reqState The current session state
    *** @param mapDim   The specified map dimensions
    **/
    public void writeMapCell(PrintWriter out, RequestProperties reqState, MapDimension mapDim)
        throws IOException
    {
      //MapDimension md = (mapDim != null)? mapDim : this.getDimension();
      //int mapW = (md != null)? md.getWidth()  : -1;
      //int mapH = (md != null)? md.getHeight() : -1;
      //String tableStyle = "width:100%;" + ((mapH < 0)? " height:100%;" : "");
      //String cellClass  = "mapProviderCell"; // "width:100%; border:1px solid black; padding:0px; margin:0px;"
      //String cellStyle  = "" + ((mapH < 0)? " height:100%;" : "");
      //out.println("<table valign='center' align='center' cellspacing='0' cellpadding='0' border='0' style='" + tableStyle + "'>");
      //out.println("<tr><td class='"+cellClass+"' align='center' style='" + cellStyle + "'>");
        String mapID    = this.getMapID();
        String divStyle = this.getMapCellStyle(reqState, mapDim);
        out.println("<div id='" + mapID + "' style='" + divStyle + "'></div>");
      //out.println("</td></tr>");
      //out.println("</table>");
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Allows the subclass MapProvider to adjust the session state prior to displaying the map
    *** @param reqState The current session state
    **/
    protected void writeMapUpdate_precheck(RequestProperties reqState)
    {
        // override to adjust RequestProperties as necessary
    }

    /**
    *** Writes the events in XML/JSON format to the http output stream.  The criteria used for
    *** selecting EventData records is specified with the RequestProperties session state.
    *** @param reqState The current session state containing the EventData selection criteria
    **/
    public void writeMapUpdate(
        int mapDataFormat,
        RequestProperties reqState)
        throws IOException
    {
        HttpServletResponse response = reqState.getHttpServletResponse();
        PrintWriter out = response.getWriter();

        /* mime content type */
        String mimeType = EventUtil.IsXMLMapDataFormat(mapDataFormat)? 
            HTMLTools.MIME_XML() : 
            HTMLTools.MIME_JSON();
        CommonServlet.setResponseContentType(response, mimeType, StringTools.CharEncoding_UTF_8);
        response.setHeader("CACHE-CONTROL", "NO-CACHE");
        response.setHeader("PRAGMA"       , "NO-CACHE");
        response.setDateHeader("EXPIRES"  , 0         );

        /* write map data */
        this.writeMapUpdate( // XML/JSON format specified by caller
            out, 0, 
            mapDataFormat, true/*isTopLevelTag*/,
            reqState); // XML/JSON

    }

    /**
    *** Writes the events in XML/JSON format to the http output stream.  The criteria used for
    *** selecting EventData records is specified with the RequestProperties session state.
    *** @param out            The output stream
    *** @param indentLevel    The indent level
    *** @param mapDataFormat  The map data format
    *** @param isTopLevelTag  True if top-level tag
    *** @param reqState       The current session state containing the EventData selection criteria
    *** @throws IOException
    **/
    public void writeMapUpdate(
        PrintWriter out, int indentLevel, 
        int mapDataFormat, boolean isTopLevelTag,
        RequestProperties reqState)
        throws IOException
    {
        //Print.logInfo("Getting map events ...");

        /* precheck (ie. adjust RequestProperties) */
        this.writeMapUpdate_precheck(reqState);

        /* extract records */
        EventDataProvider evdata[] = null;
        try {
            // -- This returns an array of EventData records based on the request attributes
            evdata = reqState.getMapEvents(-1L); // does not return null
            //Print.logInfo("Found Event count: " + evdata.length);
        } catch (DBException dbe) {
            Print.logException("Error reading Events", dbe);
            out.println("\nError reading Events");
            return;
        }

        /* arguments */
        RTProperties mapRTP     = this.getProperties(); // not null
        PrivateLabel privLabel  = reqState.getPrivateLabel(); // not null
        boolean      isFleet    = reqState.isFleet();
        Account      acct       = reqState.getCurrentAccount(); // should not be null
        User         user       = reqState.getCurrentUser(); // may be null;
        TimeZone     tmz        = reqState.getTimeZone();
        Device       selDev     = isFleet? null : reqState.getSelectedDevice();
        String       selID      = isFleet? reqState.getSelectedDeviceGroupID() : reqState.getSelectedDeviceID();
        DateTime     latest     = isFleet? null : reqState.getLastEventTime();
        double       lastBatt   = isFleet? 0.0 : (selDev == null)? 0.0 : selDev.getLastBatteryLevel();
        double       lastSig    = isFleet? 0.0 : (selDev == null)? 0.0 : 0.0; // selDev.getLastSignalStrength();
        double       proximityM = mapRTP.getDouble(MapProvider.PROP_map_minProximity, 0.0);
        String       iconSel    = this.getIconSelector(reqState);
        boolean      fleetRoute = (reqState.getFleetDeviceEventCount() > 1L);
        boolean      inclZones  = mapRTP.getBoolean(MapProvider.PROP_map_includeGeozones, false);

        /* pushpins */
        OrderedMap<String,PushpinIcon> iconMap = this.getPushpinIconMap(reqState);

        /* convert "evdata" to snapToRoad? [EXPERIMENTAL] */
        boolean ENABLE_SNAP_TO_ROAD = true;
        boolean snapToRoad = mapRTP.getBoolean(MapProvider.PROP_map_routeLine_snapToRoad, false);
        RouteProvider routeProv = privLabel.getRouteProvider();
        if (!ENABLE_SNAP_TO_ROAD) {
            //Print.logInfo("SnapToRoad["+privLabel.getName()+"/"+this.getName()+"]: not enabled ...");
        } else
        if (!snapToRoad) {
            //Print.logInfo("SnapToRoad["+privLabel.getName()+"/"+this.getName()+"]: '"+MapProvider.PROP_map_routeLine_snapToRoad[0]+"' is false ...");
        } else
        if (routeProv == null) {
            //Print.logInfo("SnapToRoad["+privLabel.getName()+"/"+this.getName()+"]: RouteProvider not defined ...");
        } else
        if (ListTools.size(evdata) < 2) {
            //Print.logInfo("SnapToRoad["+privLabel.getName()+"/"+this.getName()+"]: EventData list contains less than 2 points ...");
        } else
        if (isFleet) {
            //Print.logInfo("SnapToRoad["+privLabel.getName()+"/"+this.getName()+"]: not support for fleet map ...");
        } else {
            //Print.logInfo("Attempting Snap-To-Road["+privLabel.getName()+"/"+this.getName()+"] ...");
            final String evAcctID = evdata[0].getAccountID();
            final String evDevID  = evdata[0].getDeviceID();
            Locale locale = privLabel.getLocale();
            RTProperties rtp = null; // TODO: ie. MapProvider.PROP_map_routeLine_snapToRoad_attr
            GeoRoute snapRoute = routeProv.getSnapToRoad(evdata, locale, rtp);
            if (snapRoute != null) {
                GeoPointProvider gppa[] = snapRoute.getPath();
                EventDataProvider newEDP[] = new EventDataProvider[gppa.length];
                for (int g = 0; g < gppa.length; g++) {
                    final GeoPointProvider gpp = gppa[g];
                    final GeoPoint         gp  = gpp.getGeoPoint();
                    if (gpp instanceof EventData) {
                        newEDP[g] = (EventData)gpp;
                        continue;
                    } else
                    if (gpp instanceof GeoRoute.GPNode) {
                        int ndx = ((GeoRoute.GPNode)gpp).getOriginalIndex();
                        if (ndx >= 0) {
                            // -- add EventDataProvider point
                            double deltaM = GeoPoint.deltaMeters(evdata[ndx],gpp);
                            //Print.logInfo("Snapped event delta meters ["+ndx+"]: " + deltaM);
                            if (evdata[ndx] instanceof EventData) {
                                // -- this can change the location by a significant distance. ignore the
                                // -  change if the distance is beyond an acceptable threshold (the snapToRoad 
                                // -  may have chosen an incorrect route).
                                if (GeoPoint.deltaMeters(evdata[ndx],gpp) < 20.0) {
                                    // -- small distance changed
                                    ((EventData)evdata[ndx]).setGeoPoint(gpp.getGeoPoint());
                                } else {
                                    // -- ignore GeoPoint change
                                }
                            }
                            newEDP[g] = evdata[ndx];
                            continue;
                        }
                    }
                    // -- add route-only point
                    newEDP[g] = new EventDataProviderAdapter() {
                        public String   getAccountID() { return evAcctID; }
                        public String   getDeviceID()  { return evDevID; }
                        public long     getTimestamp() { return 0L; } // timestamp '0' means no pushpin
                        public GeoPoint getGeoPoint()  { return gp; }
                        public double   getLatitude()  { return gp.getLatitude(); }
                        public double   getLongitude() { return gp.getLongitude(); }
                        public int      getPushpinIconIndex(String iconSelector, OrderedMap<String,PushpinIcon> iconMap, boolean isFleet, BasicPrivateLabel bpl) { return -1; }
                    };
                }
                evdata = newEDP;
            }
        }

        /* return events */
        try {
            EventUtil evUtil = EventUtil.getInstance();
            // -- map data format: XML, JSON
            int mdf = EventUtil.IsXMLMapDataFormat(mapDataFormat)? EventUtil.MAPDATA_XML : EventUtil.MAPDATA_JSON;
            // -- debug/testing
            /*
            RTConfig.setDebugMode(true);
            if (RTConfig.isDebugMode()) {
                evUtil.writeMapEvents( // debug only
                    mdf, indentLevel, new PrintWriter(System.out,true),
                    false, isTopLevelTag,
                    privLabel, 
                    evdata, inclZones,
                    iconSel, iconMap,
                    isFleet, fleetRoute, selID,
                    tmz,
                    acct, user,
                    latest, lastBatt, lastSig, proximityM);
            }
            */
            // -- write map events to stream
            evUtil.writeMapEvents(
                mdf, indentLevel, out, 
                reqState.isSoapRequest(), isTopLevelTag,
                privLabel, 
                evdata, inclZones,
                iconSel, iconMap,
                isFleet, fleetRoute, selID,
                tmz, 
                acct, user,
                latest, lastBatt, lastSig, proximityM);
        } catch (IOException ioe) {
            Print.logException("Error writing events", ioe);
            out.println("\nError writing Events"); // output is Mime type plain
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the PushpinIcon map
    *** @param reqState  The RequestProperties state from the current session
    *** @return The PushpinIcon map
    **/
    public OrderedMap<String,PushpinIcon> getPushpinIconMap(RequestProperties reqState)
    {
        if (this.pushpinIconMap == null) {
          //this.pushpinIconMap = PushpinIcon.newDefaultPushpinIconMap();
            this.pushpinIconMap = new OrderedMap<String,PushpinIcon>(); // [2.6.4-B35]
        }
        return this.pushpinIconMap;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the number of supported points for the specified Geozone type
    *** @param type  The Geozone type
    *** @return The number of supported points for the specified Geozone type
    **/
    public int getGeozoneSupportedPointCount(int type)
    {
        if (!this.isFeatureSupported(MapProvider.FEATURE_GEOZONES)) {
            return 0;
        } else 
        if (type == Geozone.GeozoneType.POINT_RADIUS.getIntValue()) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
    *** Returns instructions for manipulating a Geozone
    *** @param loc  The current Locale
    *** @return The localized instructions
    **/
    public String[] getGeozoneInstructions(int type, Locale loc)
    {
        return null;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Returns the localized GeoCorridor instructions
    *** @param loc  The current Locale
    *** @return An array of instruction line items
    **/
    public String[] getCorridorInstructions(Locale loc)
    {
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
