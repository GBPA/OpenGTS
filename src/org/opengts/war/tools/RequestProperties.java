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
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/03/11  Martin D. Flynn
//     -Added 'isLoggedIn()' as convenience to determine if an account user is
//      currently logged-in.
//  2007/03/30  Martin D. Flynn
//     -Added access control support
//     -Added 'User' table support (not yet fully supported)
//  2007/05/20  Martin D. Flynn
//     -Added 'getMapProperties' method.
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies (see "setCookiesEnabled")
//     -Added User ACL convenience methods "hasReadAccess" & "hasWriteAccess".
//  2007/07/27  Martin D. Flynn
//     -Added support for MapDimension
//  2007/11/28  Martin D. Flynn
//     -Added convenience method 'getMapEvents()' for returning the mappable events
//      for the current selection
//     -Increase default map size
//  2007/12/13  Martin D. Flynn
//     -Added methods to allow customizing 'Device', 'Device Group', and 'Entity' 
//      titles.
//  2008/04/11  Martin D. Flynn
//     -Removed 'getMapProperties' method (the MapProvider now contains its own properties)
//  2008/07/21  Martin D. Flynn
//     -Optimized the "StringTools.KeyValueMap" 'getKeyValue' lookup, and added some 
//      additional keys.
//  2008/08/15  Martin D. Flynn
//     -The 'admin' user [see "User.getAdminUserID()"] is always granted "ALL" access.
//  2009/02/20  Martin D. Flynn
//     -Renamed 'setCookiesEnabled' to 'setCookiesRequired'
//  2009/05/24  Martin D. Flynn
//     -Added "i18n.User" property string key
//  2009/09/23  Martin D. Flynn
//     -Added "isSoapRequest()" method
//  2010/01/29  Martin D. Flynn
//     -Added "formatDayNumber" methods
//  2010/04/11  Martin D. Flynn
//     -Added support for hiding the "Password" field on the login page
//  2010/07/04  Martin D. Flynn
//     -Added "isLoggedInFromSysAdmin()"
//  2015/05/03  Martin D. Flynn
//     -Initial support for status-markers (marker in a sequence of events) [see "getStatusMarker"]
//  2016/01/11  Martin D. Flynn
//     -Fixed possible NPE in "getFleetMapEventSortBy" [2.6.1-B50]
//  2016/12/21  Martin D. Flynn
//     -Added KEY_driverDesc [2.6.4-B21]
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.geocoder.*;

import org.opengts.Version;
import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

import org.opengts.war.model.FTLKeyValueHash;
import org.opengts.war.model.DataModel;

import org.opengts.war.track.Constants;
import org.opengts.war.track.ExpandMenu;
import org.opengts.war.track.page.UserInfo;
import org.opengts.war.track.page.TrackMap;

public class RequestProperties
    implements StringTools.KeyValueMap
{

    // ------------------------------------------------------------------------
    // -- Login frame generation targets
    
    public  static final String       HTML_LOGIN_FRAME      = "loginFrame.html";
    public  static final String       _HTML_LOGIN_FRAME     = "/" + HTML_LOGIN_FRAME;
    public  static final String       HTML_LOGIN            = "login.html";
    public  static final String       _HTML_LOGIN           = "/" + HTML_LOGIN;

    // ------------------------------------------------------------------------

    public  static final String       TEMPLATE_JSP          = "JSP"; // JSP
    public  static final String       TEMPLATE_FTL          = "FTL"; // Freemarker

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- PrivateLabel used when none are defined

    public  static final PrivateLabel NullPrivateLabel      = new PrivateLabel("null");

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Web-page elements (see 'getPageFrameSection', 'writePageFrameSection')
    
    public  static final int    PAGE_FRAME_HEADER           = 0x0001;
    public  static final int    PAGE_FRAME_NAVIGATION       = 0x0002;
    public  static final int    PAGE_FRAME_FOOTER           = 0x0004;
    public  static final int    PAGE_FRAME_LEFT             = 0x0010;
    public  static final int    PAGE_FRAME_RIGHT            = 0x0020;
    public  static final int    PAGE_FRAME_CONTENT          = 0x0100;
    public  static final int    PAGE_FRAME_CONTENT_MENUBAR  = 0x0200;
    public  static final int    PAGE_FRAME_ALL              = 
        PAGE_FRAME_HEADER | 
        PAGE_FRAME_NAVIGATION | 
        PAGE_FRAME_LEFT | 
        PAGE_FRAME_CONTENT | 
        PAGE_FRAME_CONTENT_MENUBAR | 
        PAGE_FRAME_RIGHT | 
        PAGE_FRAME_FOOTER;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Image name map (relative to current context path)

    private static final String IMAGE_EXTN[]  = { "png", "jpg", "jpeg", "gif" };
    private static final String IMAGE_KEY_PFX = "%";

    public static class ImageMap
    {
        private AtomicBoolean      imgInit    = new AtomicBoolean(false);
        private Map<String,String> imgMap     = new HashMap<String,String>();
        private Map<String,String> imgNames   = new HashMap<String,String>();
        private String             subPaths[] = null;
        public ImageMap(String... paths) {
            this.subPaths = paths; // must not be null
        }
        // -- initialize
        private void _init() {
            if (!this.imgInit.get()) {
                synchronized (this.imgInit) {
                    if (!this.imgInit.get()) { // check again after lock
                        // -- initialize
                        String ctxPathS = RTConfig.getContextPath(null); // "../apache-tomcat/webapps/track"
                        File   ctxPath  = !StringTools.isBlank(ctxPathS)? new File(ctxPathS) : null;
                        if ((ctxPath != null) && (this.subPaths != null)) {
                            // -- Tomcat "ROOT" directory
                            File rootPath = null;
                            String webappsSD = "/webapps/";
                            if (ctxPathS.indexOf(webappsSD) >= 0) {
                                // -- likely, if running under Tomcat
                                int p = ctxPathS.indexOf(webappsSD);
                                File rdir = new File(ctxPathS.substring(0,p+webappsSD.length()) + "ROOT");
                                if (FileTools.isDirectory(rdir)) {
                                    // -- "../apache-tomcat/webapps/ROOT"
                                    //Print.logInfo("Found Tomcat 'ROOT' directory not found: " + rdir);
                                    rootPath = rdir;
                                } else {
                                    Print.logWarn("Tomcat 'ROOT' directory not found: " + rdir);
                                }
                            }
                            // -- loop through subpaths
                            for (String sp : this.subPaths) {
                                // -- create fully qualified image directory path
                                File imagePath;
                                if (sp.startsWith("/")) {
                                    // -- ie. ""../apache-tomcat/ROOT/images/texture"
                                    imagePath = (rootPath != null)? new File(rootPath,sp.substring(1)) : null;
                                } else {
                                    // -- ie. ""../apache-tomcat/track/images/texture"
                                    imagePath = new File(ctxPath,sp);
                                }
                                if ((imagePath == null) || !FileTools.isDirectory(imagePath)) {
                                    // -- image path is not a valid directory
                                    continue; 
                                }
                                // -- get list of image files in image directory
                                //Print.logInfo("Looking for images in: " + imagePath);
                                File imageFiles[] = FileTools.getFiles(imagePath, IMAGE_EXTN, false);
                                if (!ListTools.isEmpty(imageFiles)) {
                                    for (File img: imageFiles) {
                                        String fName = img.getName();
                                        int    p     = fName.indexOf("."); // always >= 0
                                        String name  = (p > 0)? fName.substring(0,p) : fName;
                                        String key   = IMAGE_KEY_PFX + name.toLowerCase();
                                        String path  = sp + "/" + fName;
                                        this.imgMap.put(key, path);                    // %key ==> path 
                                        this.imgNames.put(key, IMAGE_KEY_PFX + name);  // %key ==> %name
                                    }
                                } else {
                                    //Print.logInfo("No images found at path: " + imagePath);
                                }
                            }
                        }
                        // -- set as initialized
                        this.imgInit.set(true);
                    }
                } // synchronized (this.imgInit)
            }
        }
        // -- get URL
        public String getURL(String key, String dft) {
            this._init(); // initialize
            String url = this.imgMap.get(StringTools.toLowerCase(key));
            return !StringTools.isBlank(url)? url : dft;
        }
        // -- get names
        public String[] getNames() {
            this._init(); // initialize
            java.util.List<String> n = ListTools.toList(this.imgNames.values(), new Vector<String>());
            return ListTools.sort(n.toArray(new String[n.size()]));
        }
    }

    // --------------------------------

    /* pre-defined background images */
    private static final ImageMap BackgroundURLMap = new ImageMap(
        // -- 1) background image locations
        "images/background",
        "extra/images/background",
        "imagePack/background",
        // -- 2) look in Tomcat "ROOT/images/background"
        "/images/background",
        "/imagePack/background"
    );

    /**
    *** Gets the URL for the specified pre-defined image key
    *** @param key  The image key
    *** @param dft  The default returned value if the image key is not found
    *** @return The predefined image URL, or null if the image key is not found
    **/
    public static String GetBackgroundImageURL(String key, String dft)
    {
        return BackgroundURLMap.getURL(key, dft);
    }

    /**
    *** Gets an array of the background image names
    **/
    public static String[] GetBackgroundImageNames()
    {
        return BackgroundURLMap.getNames();
    }

    // --------------------------------

    /* pre-defined banner images */
    private static final ImageMap BannerURLMap = new ImageMap(
        // -- 1) banner image locations
        "images/banner",
        "extra/images/banner", 
        "imagePack/banner",
        // -- 2) look in Tomcat "ROOT/images/banner"
        "/images/banner",
        "/imagePack/banner"
    );

    /**
    *** Gets the URL for the specified pre-defined image key
    *** @param key  The image key
    *** @param dft  The default returned value if the image key is not found
    *** @return The predefined image URL, or null if the image key is not found
    **/
    public static String GetBannerImageURL(String key, String dft)
    {
        return BannerURLMap.getURL(key, dft);
    }

    /**
    *** Gets an array of the banner image names
    **/
    public static String[] GetBannerImageNames()
    {
        return BannerURLMap.getNames();
    }

    // --------------------------------

    /* pre-defined texture images */
    private static final ImageMap TextureURLMap = new ImageMap(
        // -- 1) texture image locations
        "images/texture", 
        "extra/images/texture", 
        "imagePack/texture",
        // -- 2) look in Tomcat "ROOT/images/texture"
        "/images/texture",
        "/imagePack/texture",
        // -- 3) textures can also use background images
        "images/background",
        "extra/images/background",
        "imagePack/background",
        // -- 4) look in Tomcat "ROOT/images/background"
        "/images/background",
        "/imagePack/background"
    );

    /**
    *** Gets the URL for the specified pre-defined image key
    *** @param key  The image key
    *** @param dft  The default returned value if the image key is not found
    *** @return The predefined image URL, or null if the image key is not found
    **/
    public static String GetTextureImageURL(String key, String dft)
    {
        return TextureURLMap.getURL(key, dft);
    }

    /**
    *** Gets an array of the texture image names
    **/
    public static String[] GetTextureImageNames()
    {
        return TextureURLMap.getNames();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Base URI

    /* return base uri */
    private static String TRACK_BASE_URI = null;
    public static String TRACK_BASE_URI()
    {
        if (TRACK_BASE_URI == null) {

            /* Runtime URI */
            String uri = RTConfig.getString(DBConfig.PROP_track_baseURI, null);
            if (uri == null) { 
                uri = Constants._DEFAULT_BASE_URI; 
            }

            /* set Track baseURI */
            if (uri.equals(".")) {
                TRACK_BASE_URI = "./";
            } else
            if (uri.startsWith("./")) {
                TRACK_BASE_URI = uri;
            } else
            if (uri.startsWith("/")) {
                TRACK_BASE_URI = "." + uri;
            } else {
                TRACK_BASE_URI = "./" + uri;
            }

        }
        return TRACK_BASE_URI;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Instance vars

    private HttpServletResponse response                = null;
    private HttpServletRequest  request                 = null;
    private boolean             isSoapRequest           = false;
    
    private String              baseURI                 = null;
    
    private String              webPageURI              = null; // non-null to override default
    
    private boolean             cookiesRequired         = true; // default to true

    private String              ipAddress               = "";

    private String              faviconLink             = "";
    private String              pageName                = "";
    private String              pageNavHTML             = null;
    private int                 pageFrameSections       = PAGE_FRAME_ALL;

    private String              cmdName                 = "";
    private String              cmdArg                  = "";

    private boolean             isFleet                 = false;
    private boolean             isReport                = false;
    private boolean             notifyEventsOnly        = false;

    private PrivateLabel        privLabel               = null;
    private String              localeStr               = null;

    private MapProvider         mapProvider             = null;

    private Account             account                 = null;
    private User                user                    = null;

    private Account             sysadmin                = null;

    private DeviceGroup         selDeviceGroup          = null;
    private String              selDeviceGroupID        = null;
    private boolean             isActualSelGrpID        = false;

    private Device              selDevice               = null;
    private String              selDeviceID             = null;
    private boolean             isActualSelDevID        = false;

    private Driver              selDriver               = null;
    private String              selDriverID             = null;
    private boolean             isActualSelDrvID        = false;

    private boolean             loginErrorAlert         = false;

    private String              userList[]              = null;
    private OrderedSet<String>  driverList              = null;
    private OrderedSet<String>  deviceList              = null;
    private OrderedSet<String>  deviceGrpSet            = null;
    private OrderedSet<String>  deviceGrpSetAll         = null;

    private DateTime            dateFrom                = null;
    private DateTime            dateTo                  = null;
    private TimeZone            timeZone                = null;
    private String              timeZoneShortStr        = null;
    private String              timeZoneLongStr         = null;

    private int                 statusCodes[]           = null;
    private int                 statusMarkers[]         = null;

    private long                eventLimitCnt           = 100L;
    private EventData.LimitType eventLimitType          = EventData.LimitType.LAST;
    private DateTime            lastEvent               = null;

    private int                 showPassword            = -1; // tri-state

    private int                 enableDemo              = -1; // tri-state

    private boolean             encodeEmailHTML         = false;

    /* new ReguestProperties instance */
    public RequestProperties()
    {
        super();
    }

    // ------------------------------------------------------------------------

    public boolean getEncodeEMailHTML()
    {
        return this.encodeEmailHTML;
    }

    public void setEncodeEMailHTML(boolean state)
    {
        this.encodeEmailHTML = state;
    }

    // ------------------------------------------------------------------------

    /* set the base URI */
    public void setBaseURI(String baseUri)
    {
        this.baseURI = baseUri;
    }

    /* get the base URI */
    public String getBaseURI()
    {
        return (this.baseURI != null)? this.baseURI : "/";
    }

    // ------------------------------------------------------------------------

    /* set the HttpServletResponse instance for this session */
    public void setHttpServletResponse(HttpServletResponse response)
    {
        this.response = response;
    }

    /* return the HttpServletResponse instance for this session */
    public HttpServletResponse getHttpServletResponse()
    {
        return this.response;
    }

    // ------------------------------------------------------------------------

    /* set the HttpServletRequest instance for this session */
    public void setHttpServletRequest(HttpServletRequest request)
    {
        this.request = request;
    }

    /* return the HttpServletRequest instance for this session */
    public HttpServletRequest getHttpServletRequest()
    {
        return this.request;
    }

    /* return the request URL */
    // -- Should not be used!
    //public String getHttpServletRequestURL()
    //{
    //    if (this.request != null) {
    //        return this.request.getRequestURL().toString();
    //    } else {
    //        return "";
    //    }
    //}

    /* return the URL for this request */
    public URIArg getHttpServletRequestURIArg(boolean inclUserPass)
    {
        if (this.request != null) {
            //URIArg url = WebPageAdaptor.MakeURL(this.getBaseURI());
            URIArg url = new URIArg(this.getBaseURI(), true); // EncodeURL
            url.addArg(Constants.PARM_ACCOUNT, this.getCurrentAccountID());
            url.addArg(Constants.PARM_USER   , this.getCurrentUserID());
            if (inclUserPass) {
                String encPass = this.getCurrentUserEncodedPassword();
                if (!encPass.equals(Account.BLANK_PASSWORD)) {
                    url.addArg(Constants.PARM_ENCPASS, encPass);
                }
            }
            for (Enumeration<String> e = this.request.getParameterNames(); e.hasMoreElements();) {
                String key = e.nextElement();
                String val = this.request.getParameter(key);
                //Print.logInfo("Key:" + key + " ==> " + val);
                url.addArg(key, val);
            }
            return url;
        } else {
            return null;
        }
    }

    /* pass-through for "request.isSecure()" */
    public boolean isSecure()
    {
        if (this.request != null) {
            return this.request.isSecure();
        } else {
            return false; // assume false
        }
    }

    // ------------------------------------------------------------------------

    /* SOAP request? (OBSOLETE) */
    public void setSoapRequest(boolean soap)
    {
        this.isSoapRequest = soap;
    }

    /* SOAP request? (OBSOLETE) */
    public boolean isSoapRequest()
    {
        return this.isSoapRequest;
    }

    // ------------------------------------------------------------------------

    /* set the cookies enabled flag */
    public void setCookiesRequired(boolean cookiesReq)
    {
        this.cookiesRequired = cookiesReq;
    }
    
    /* return the cookies enabled flag */
    public boolean getCookiesRequired()
    {
        return this.cookiesRequired;
    }

    // ------------------------------------------------------------------------

    /* set the current IP address */
    public void setIPAddress(String ipAddr)
    {
        this.ipAddress = ipAddr;
    }
    
    /* return the current IP address */
    public String getIPAddress()
    {
        return (this.ipAddress != null)? this.ipAddress : "";
    }

    // ------------------------------------------------------------------------

    /* sets the "favicon" image */
    public void setFaviconLink(String favicon)
    {
        this.faviconLink = StringTools.trim(favicon);
    }

    /* gets the "favicon" image */
    public String getFaviconLink()
    {
        if (StringTools.isBlank(this.faviconLink)) {
            // -- delegate to PrivateLabel
            PrivateLabel _privLabel = this.getPrivateLabel();
            return _privLabel.getFaviconLink(); // may still be null/blank
        } else
        if (this.faviconLink.equalsIgnoreCase("NONE")) {
            // -- blank FavIcon requested
            return "";
        } else {
            // -- explicit FavIcon
            return this.faviconLink;
        }
    }
    
    /* returns true if favicon is defined */
    public boolean hasFaviconLink()
    {
        return !StringTools.isBlank(this.faviconLink)? true : false;
    }

    // ------------------------------------------------------------------------

    /* set the current page name */
    public void setPageName(String page)
    {
        this.pageName = page;
    }

    /* return the current page name */
    public String getPageName()
    {
        return (this.pageName != null)? this.pageName : "";
    }

    /* return the current web-page */
    public WebPage getWebPage()
    {
        if (this.pageName != null) {
            PrivateLabel _privLabel = this.getPrivateLabel();
            return _privLabel.getWebPage(this.pageName);
        } else {
            return null;
        }
    }

    /* return the current JSP name */
    public String getJspName()
    {
        String jn = this.getPrivateLabel().getWebPageJSP();
        return StringTools.replaceKeys(jn, this, null);
    }

    /* return the current JSP URI */
    public String getJspURI()
    {
        String jspURI  = this.getWebPageURI();
        //Print.logInfo("getWebPageURI: " + jspURI);
        String jspFile = this.getPrivateLabel().getWebPageJSP(jspURI, this);
        //Print.logInfo("Returning JSP file: " + jspFile);
        return jspFile;
    }

    // ------------------------------------------------------------------------

    /* return the current web-page [JSP|FTL] */
    public String getWebPageTemplateProvider()
    {
        String dft = TEMPLATE_JSP;
        String uri = this.getJspURI();
        //Print.logInfo("Page "+this.getPageName()+" JSPFile="+uri);
        int p = (uri != null)? uri.lastIndexOf(".") : -1;
        if (p > 0) {
            String ext = uri.substring(p+1).toUpperCase();
            if (ext.equalsIgnoreCase(TEMPLATE_JSP)) {
                //Print.logInfo("TemplateProvider: " + TEMPLATE_JSP);
                return TEMPLATE_JSP;
            } else
            if (ext.equalsIgnoreCase(TEMPLATE_FTL)) {
                //Print.logInfo("TemplateProvider: " + TEMPLATE_FTL);
                return TEMPLATE_FTL;
            }
            // -- unrecognized type
            //Print.logInfo("TemplateProvider: (Unrecognized) " + ext);
            return dft;
        }
        //Print.logInfo("TemplateProvider: (No WebPage) " + dft);
        return dft;
    }

    /* returns true if the current WebPage template provider is "JSP" */
    public boolean isWebPageTemplateJSP()
    {
        String wpTP = this.getWebPageTemplateProvider();
        return TEMPLATE_JSP.equals(wpTP);
    }

    /* returns true if the current WebPage template provider is "FTL" (Freemarker) */
    public boolean isWebPageTemplateFTL()
    {
        String wpTP = this.getWebPageTemplateProvider();
        return TEMPLATE_FTL.equals(wpTP);
    }

    // ------------------------------------------------------------------------

    /* set WebPage JSP URI override */
    public void setWebPageURI(String uri)
    {
        this.webPageURI = uri;
    }

    /* get WebPage JSP URI */
    public String getWebPageURI()
    {
        if (!StringTools.isBlank(this.webPageURI)) {
            //Print.logInfo("Returning JSP[1]: " + this.webPageURI);
            return this.webPageURI;
        } else {
            WebPage page = this.getWebPage();
            String jsp = (page != null)? page.getJspURI() : null;
            //Print.logInfo("Returning JSP[2]: " + jsp);
            return jsp;
        }
    }

    // ------------------------------------------------------------------------

    /* set "report" request (used for report 'map') */
    public void setReport(boolean report)
    {
        this.isReport = report;
    }
    
    /* return true if this is a "report" request */
    public boolean isReport()
    {
        return this.isReport;
    }

    // ------------------------------------------------------------------------

    /* set "fleet" request */
    public void setFleet(boolean fleet)
    {
        this.isFleet = fleet;
    }

    /* return true if this is a "fleet" request */
    public boolean isFleet()
    {
        return this.isFleet;
    }

    /* return max number of events per device for fleet map */
    public long getFleetDeviceEventCount()
    {
        PrivateLabel _privLabel = this.getPrivateLabel();

        /* check current web-page for override */
        WebPage webPage = this.getWebPage();
        if (webPage != null) {
            // -- NOTE: this may not work for AJAX map event updates.
            long dec = webPage.getProperties().getLong(TrackMap.PROP_fleetDeviceEventCount,-1L);
            if (dec > 0L) {
                return dec;
            }
        }

        /* default to global PrivateLabel property */
        long dec = _privLabel.getLongProperty(PrivateLabel.PROP_TrackMap_fleetDeviceEventCount,1L);
        return (dec >= 1L)? dec : 1L;

    }

    /* return fleet map device filter rule selector */
    // -- requires installed RuleFactory
    public String getFleetMapDeviceSelector()
    {
        PrivateLabel _privLabel = this.getPrivateLabel();

        /* check current web-page for override */
        WebPage webPage = this.getWebPage();
        if (webPage != null) {
            // -- NOTE: this may not work for AJAX map event updates.
            String filt = webPage.getProperties().getString(PrivateLabel.PROP_TrackMap_fleetMapDeviceSelector,null);
            if (!StringTools.isBlank(filt)) {
                return filt;
            }
        }

        /* default to global PrivateLabel property */
        String filt = _privLabel.getStringProperty(PrivateLabel.PROP_TrackMap_fleetMapDeviceSelector,null);
        return !StringTools.isBlank(filt)? filt : "";

    }

    /* return fleet map event sort option for "Location Details" */
    private static final int FLEET_EVENT_SORT_DEVICE_ID     = 1;
    private static final int FLEET_EVENT_SORT_DEVICE_DESC   = 2;
    private static final int FLEET_EVENT_SORT_TIMESTAMP     = 3;
    public int getFleetMapEventSortBy()
    {
        String sortBy = "";

        /* check current web-page for override */
        if (StringTools.isBlank(sortBy)) {
            WebPage webPage = this.getWebPage();
            if (webPage != null) {
                // -- NOTE: this may not work for AJAX map event updates.
                sortBy = webPage.getProperties().getString(PrivateLabel.PROP_TrackMap_fleetMapEventSortBy,"");
            }
        }

        /* default to global PrivateLabel property */
        if (StringTools.isBlank(sortBy)) {
            PrivateLabel _privLabel = this.getPrivateLabel();
            sortBy = _privLabel.getStringProperty(PrivateLabel.PROP_TrackMap_fleetMapEventSortBy,"");
        }

        /* return sort option */
        if (StringTools.isBlank(sortBy)) { // [2.6.1-B50]
            // -- Device Description (default)
            return FLEET_EVENT_SORT_DEVICE_DESC;
        } else
        if (sortBy.equalsIgnoreCase("id")      ||
            sortBy.equalsIgnoreCase("deviceID")  ) {
            // -- Device ID
            return FLEET_EVENT_SORT_DEVICE_ID;
        } else
        if (sortBy.equalsIgnoreCase("description")      ||
            sortBy.equalsIgnoreCase("deviceDescription")  ) {
            // -- Device Description
            return FLEET_EVENT_SORT_DEVICE_DESC;
        } else
        if (sortBy.equalsIgnoreCase("timestamp")) {
            // -- Timestamp
            return FLEET_EVENT_SORT_TIMESTAMP;
        } else {
            // -- Device Description (default)
            return FLEET_EVENT_SORT_DEVICE_DESC;
        }

    }

    // ------------------------------------------------------------------------

    /* set "notifyEventsOnly" state */
    public void setDeviceNotifyEventsOnly(boolean notifyEvents)
    {
        this.notifyEventsOnly = notifyEvents;
    }
    
    /* return true if this request should get notify events only */
    public boolean getDeviceNotifyEventsOnly()
    {
        return this.notifyEventsOnly;
    }

    // ------------------------------------------------------------------------

    /* set the current page navigation */
    public void setPageNavigationHTML(String pageNav)
    {
        this.pageNavHTML = pageNav;
    }

    /* return the current page navigation */
    public String getPageNavigationHTML()
    {
        return this.pageNavHTML;
    }

    // ------------------------------------------------------------------------
    
    /* set page frame sections written to client */
    public void setPageFrameSections(int pfs)
    {
        this.pageFrameSections = pfs | PAGE_FRAME_CONTENT;
    }
    
    /* return page frame sections to write to client */
    public int getPageFrameSections()
    {
        return this.pageFrameSections;
    }

    /* set content only */
    public void setPageFrameContentOnly(boolean contentOnly)
    {
        if (contentOnly) {
            this.setPageFrameSections(PAGE_FRAME_CONTENT);
        } else {
            this.setPageFrameSections(PAGE_FRAME_ALL);
        }
    }

    /* gett content only state */
    public boolean getPageFrameContentOnly()
    {
        return (this.getPageFrameSections() == PAGE_FRAME_CONTENT);
    }

    /* return true if specified page frame section is enabled */
    public boolean writePageFrameSection(int pfs)
    {
        return ((pfs & this.pageFrameSections) != 0);
    }

    // ------------------------------------------------------------------------

    /* set the URL command name */
    public void setCommandName(String cmd)
    {
        this.cmdName = cmd;
    }
    
    /* return the URL command name */
    public String getCommandName()
    {
        return (this.cmdName != null)? this.cmdName : "";
    }

    /* set the URL argument string */
    public void setCommandArg(String arg)
    {
        this.cmdArg = arg;
    }
    
    /* return the URL argument string */
    public String getCommandArg()
    {
        return (this.cmdArg != null)? this.cmdArg : "";
    }

    // ------------------------------------------------------------------------

    /* set the PrivateLabel for this domain */
    public void setPrivateLabel(PrivateLabel _privLabel)
    {
        this.privLabel = _privLabel;
    }
    
    /* get the PrivateLabel for this domain */
    // does(must) not return null
    public PrivateLabel getPrivateLabel()
    {
        return (this.privLabel != null)? this.privLabel : NullPrivateLabel;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the locale String code for this Session
    *** @param localeStr  The locale String associated with this Session
    **/
    public void setLocaleString(String localeStr)
    {
        this.localeStr = !StringTools.isBlank(localeStr)? localeStr : null;
    }

    /* get the current PrivateLabel Locale */
    public Locale getLocale()
    {
        if (!StringTools.isBlank(this.localeStr)) {
            return I18N.getLocale(this.localeStr);
        } else {
            return this.getPrivateLabel().getLocale();
        }
    }

    /* returns true if locale is RTL (right-to-left) */
    private static final String LocaleRTL[] = { 
        "ar",   // Arabic
        "arc",  // Aramaic
        "he",   // Hebrew
        "fa",   // Persian
        "yi",   // Yiddish
        // https://en.wikipedia.org/wiki/Right-to-left#RTL_Wikipedia_languages
    };
    public boolean isLocaleRTL() 
    {
        Locale locale = this.getLocale();
        if (locale != null) {
            String locCode = StringTools.trim(locale.getLanguage()).toLowerCase();
            if (ListTools.contains(RequestProperties.LocaleRTL,locCode)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /* set the current map provider */
    public void setMapProvider(MapProvider mapProv)
    {
        this.mapProvider = mapProv;
    }

    /* return the current map provider */
    public MapProvider getMapProvider()
    {
        if (this.mapProvider == null) {
            this.mapProvider = this.getPrivateLabel().getMapProvider();
        }
        return this.mapProvider;
    }

    /* return list of pushpin icons (in order) */
    private OrderedMap<String,PushpinIcon> pushpinIconMap = null; // optimization
    public OrderedMap<String,PushpinIcon> getMapProviderIconMap()
    {
        if (this.pushpinIconMap == null) {
            MapProvider mapProv = this.getMapProvider();
            if (mapProv != null) {
                this.pushpinIconMap = mapProv.getPushpinIconMap(this);
            } else {
                this.pushpinIconMap = new OrderedMap<String,PushpinIcon>();
            }
        }
        return this.pushpinIconMap;
    }

    /* return list of pushpin icons (in order) */
    public java.util.List<String> getMapProviderPushpinIDs()
    {
        MapProvider mapProv = this.getMapProvider();
        if (mapProv != null) {
            return ListTools.toList(mapProv.getPushpinIconMap(this).keySet(), new Vector<String>());
        } else {
            return new Vector<String>();
        }
    }

    /* return the named PushpinIcon instance */
    public PushpinIcon getPushpinIcon(String ppName)
    {
        MapProvider mapProv = this.getMapProvider();
        if (mapProv != null) {
            OrderedMap<String,PushpinIcon> ppMap = mapProv.getPushpinIconMap(this); // not null
            return ppMap.get(ppName);
        } else {
            return null;
        }
    }

    /* return the named PushpinIcon instance */
    public PushpinIcon getPushpinIcon(int ppNdx)
    {
        MapProvider mapProv = this.getMapProvider();
        if (mapProv != null) {
            OrderedMap<String,PushpinIcon> ppMap = mapProv.getPushpinIconMap(this); // not null
            return ppMap.getValue(ppNdx); // null if index not found
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    public void _setLoginErrorAlert()
    {
        this.loginErrorAlert = true;
    }
    
    public boolean _isLoginErrorAlert()
    {
        return this.loginErrorAlert;
    }

    // ------------------------------------------------------------------------

    /**
    *** Return True if logged in from SysAdmin account page
    *** @return True if logged in from SysAdmin account page
    **/
    public boolean isLoggedInFromSysAdmin()
    {
        return RequestProperties.isLoggedInFromSysAdmin(this.getHttpServletRequest(),this.getPrivateLabel());
    }

    /**
    *** Return True if logged in from SysAdmin account page
    *** @return True if logged in from SysAdmin account page
    **/
    public static boolean isLoggedInFromSysAdmin(HttpServletRequest request, PrivateLabel _privLabel)
    {

        /* check HTTP request */
        if (request == null) {
            // -- no HttpServletRequest
            //Print.logInfo("HttpServletRequest is null");
            return false;
        } else 
        if (_privLabel == null) {
            // -- no PrivateLabel
            //Print.logInfo("PrivateLabel is null");
            return false;
        }

        /* SysAdmin/Manager originator */
        String saAcctID = AttributeTools.getSessionString(request,Constants.PARM_SA_RELOGIN_ACCT,"");
        String saUserID = AttributeTools.getSessionString(request,Constants.PARM_SA_RELOGIN_USER,"");
        String saPass   = (String)AttributeTools.getSessionAttribute(request,Constants.PARM_SA_RELOGIN_PASS,""); // session only
        long   saTimeSt = AttributeTools.getSessionLong(request,Constants.PARM_SA_RELOGIN_SESS,0L);

        /* SysAdmin/Manager relogin allowed? */
        if (!_privLabel.isSystemAccountsLoginEnabled(saAcctID)) {
            // -- managed account login not supported
            //Print.logInfo("SystemAccountsLogin disabled");
            return false;
        }

        /* SysAdmin/Manager relogin key */
        if (StringTools.isBlank(saPass)) {
            // -- no relogin key
            //Print.logInfo("No relogin key");
            return false;
        }

        /* SysAdmin/Manager originator */
        if (StringTools.isBlank(saAcctID)) {
            // -- no relogin account
            Print.logWarn("Relogin passcode specified, but not a SysAdmin/Manager relogin session ...");
            return false;
        }

        /* SysAdmin/Manager login session timeout? */
        if ((saTimeSt + DateTime.MinuteSeconds(30)) < DateTime.getCurrentTimeSec()) {
            Print.logWarn("SysAdmin/Manager relogin session timeout ...");
            return false;
        }

        /* check passcode */
        // -- TODO: this should check a 'relogin' password from the "SysAdmin/Manager" record
        String passcode = _privLabel.getSystemAccountsLoginPasscode(saAcctID);
        if (StringTools.isBlank(passcode)) {
            // -- a blank reloginPasscode implies that the relogin feature is disabled
            //Print.logInfo("Blank passcode for " + saAcctID);
            return false;
        } else
        if (!saPass.equals(passcode)) {
            //Print.logInfo("Relogin passcode mismatch ...");
            return false;
        }

        /* if we get to here, we've passed all tests */
        Print.logInfo("Auto Logged-In from '"+saAcctID+"/"+saUserID+"' ...");
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Return True if logged in from SysAdmin account page and logging out from the current
    *** account will return to the SysAdmin System Accounts page.
    **/
    public boolean isReturnToSysAdminEnabled()
    {
        HttpServletRequest request = this.getHttpServletRequest();
        PrivateLabel privLabel = this.getPrivateLabel();
        return RequestProperties.isReturnToSysAdminEnabled(request,privLabel);
    }

    /**
    *** Return True if logged in from SysAdmin account page and logging out from the current
    *** account will return to the SysAdmin System Accounts page.
    **/
    public static boolean isReturnToSysAdminEnabled(
        HttpServletRequest request, PrivateLabel _privLabel)
    {

        /* check HTTP request */
        if (request == null) {
            // no HttpServletRequest
            return false;
        } else 
        if (_privLabel == null) {
            // no PrivateLabel
            return false;
        }

        /* relogin session attibutes */
        String saAcctID = AttributeTools.getSessionString(request,Constants.PARM_SA_RELOGIN_ACCT,"");
        long  saLoginTS = AttributeTools.getSessionLong(request,Constants.PARM_SA_RELOGIN_SESS,0L);

        /* no SysAdmin/Manager account? */
        if (StringTools.isBlank(saAcctID)) {
            return false;
        }

        /* not logged in from SysAdmin */
        if (!RequestProperties.isLoggedInFromSysAdmin(request,_privLabel)) {
            return false;
        }

        /* return not enabled */
        if (!_privLabel.isSystemAccountsLoginReturnEnabled(saAcctID)) {
            return false;
        }

        /* timeout expired */
        long rtnTMOSec = _privLabel.getSystemAccountsLoginReturnTimeout(saAcctID);
        if ((rtnTMOSec >= 0L) && ((saLoginTS + rtnTMOSec) < DateTime.getCurrentTimeSec())) {
            return false;
        }

        /* ok */
        return true;

    }

    // ------------------------------------------------------------------------

    /* set the current Account */
    public void setCurrentAccount(Account account)
    {
        this.account = account;
        if (this.account != null) {
            //Print.logInfo("Set Account: " + this.account.getAccountID());
        } else {
            Print.logWarn("Account not specified!");
        }
    }
    
    /* return the current Account */
    public Account getCurrentAccount()
    {
        return this.account;
    }

    /* return the current account ID/name */
    public String getCurrentAccountID()
    {
        return (this.account != null)? this.account.getAccountID() : "";
    }

    /* return true if we have an account */
    public boolean isLoggedIn()
    {
        return (this.getCurrentAccount() != null);
    }

    // ------------------------------------------------------------------------

    /* set the current User */
    public void setCurrentUser(User user)
    {
        this.user = user;
    }

    /* get the current User */
    public User getCurrentUser()
    {
        return this.user;
    }

    /* get the current User */
    public String getCurrentUserEncodedPassword()
    {
        if (this.user != null) {
            return this.user.getEncodedPassword();
        } else
        if (this.account != null) {
            return this.account.getEncodedPassword();
        } else {
            return null;
        }
    }

    /* return the current User ID/name */
    public String getCurrentUserID()
    {
        return (this.user != null)? this.user.getUserID() : User.getAdminUserID();
    }

    /* set the list of known users for this account */
    public void setUserList(String[] userList)
    {
        this.userList = userList;
    }

    /* return a list of known users for this account */
    public String[] getUserList()
    {
        if (this.userList == null) {
            try {
                User user = this.getCurrentUser();
                PrivateLabel privLabel = this.getPrivateLabel(); // never null
                WebPage userPage = privLabel.getWebPage(Constants.PAGE_USER_INFO);
                if (userPage == null) {
                    this.userList = new String[0];
                } else
                if (privLabel.hasReadAccess(user, userPage.getAclName(UserInfo._ACL_ALL))) {
                    this.userList = User.getUsersForAccount(this.getCurrentAccountID());
                } else
                if (privLabel.hasReadAccess(user, userPage.getAclName())) {
                    this.userList = new String[] { user.getUserID() };
                } else {
                    this.userList = new String[0];
                }
            } catch (DBException dbe) {
                Print.logWarn("Error getting User list: " + dbe);
                String uid = this.getCurrentUserID();
                this.userList = StringTools.isBlank(uid)? new String[0] : new String[] { uid };
            }
        }
        return this.userList;
    }

    // ------------------------------------------------------------------------

    /* return the current device group ID/name */
    public void setSelectedDeviceGroupID(String groupID)
    {
        this.setSelectedDeviceGroupID(groupID, true);
    }
    
    /* return the current device group ID/name */
    public void setSelectedDeviceGroupID(String groupID, boolean isActualSpecifiedGroup)
    {
        this.selDeviceGroupID = groupID;
        this.isActualSelGrpID = isActualSpecifiedGroup;
        this.selDeviceGroup   = null;
    }

    /* return the current device group ID/name */
    public String getSelectedDeviceGroupID()
    {
        if (!StringTools.isBlank(this.selDeviceGroupID)) {
            return this.selDeviceGroupID;
        } else {
            OrderedSet<String> grpList = this.getDeviceGroupIDList(false);
            if (!ListTools.isEmpty(grpList)) {
                return grpList.get(0);
            } else {
                return DeviceGroup.DEVICE_GROUP_ALL;
            }
        }
    }

    /* is actual specified group ID (ie. not a 'default' selection) */
    public boolean isActualSpecifiedGroup()
    {
        return !StringTools.isBlank(this.selDeviceGroupID) && this.isActualSelGrpID;
    }

    /* get the current DeviceGroup */
    public DeviceGroup getSelectedDeviceGroup()
    {
        if (this.selDeviceGroup == null) {
            String groupID = this.getSelectedDeviceGroupID();
            if (!groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                try {
                    Account account = this.getCurrentAccount();
                    this.selDeviceGroup = DeviceGroup.getDeviceGroup(account, groupID);
                    if (this.selDeviceGroup == null) {
                        this.selDeviceGroupID = DeviceGroup.DEVICE_GROUP_ALL;
                    }
                } catch (DBException dbe) {
                    this.selDeviceGroupID = DeviceGroup.DEVICE_GROUP_ALL;
                    Print.logException("Error reading DeviceGroup: " + this.getCurrentAccountID() + "/" + groupID, dbe);
                }
            }
        }
        return this.selDeviceGroup;
    }

    /* clear the list of device groups */
    public void clearDeviceGroupList()
    {
        this.deviceGrpSet     = null;
        this.deviceGrpSetAll  = null;
        this.selDeviceGroupID = null;
        this.selDeviceGroup   = null;
    }

    /* return a list of authorized devices for this account/user */
    // does not return null
    public OrderedSet<String> getDeviceGroupIDList(boolean inclAll)
    {
        // Warning: this caches the returned value!  
        if (this.deviceGrpSet == null) {
            User user = this.getCurrentUser();
            if (!User.isAdminUser(user)) {
                // (user is not null) get User authorized groups
                try {
                    java.util.List<String> grpList = user.getDeviceGroups(true/*refresh*/);
                    if (!ListTools.isEmpty(grpList)) {
                        // -- user has been given a specific list of authorized groups
                        this.deviceGrpSet    = new OrderedSet<String>(grpList);
                        this.deviceGrpSetAll = new OrderedSet<String>(this.deviceGrpSet); // shallow copy
                        this.deviceGrpSetAll.add(0, DeviceGroup.DEVICE_GROUP_ALL); // all "authorized" devices
                    } else {
                        // list is empty, 'ALL' is ok
                        //this.deviceGrpSet    = DeviceGroup.GROUP_LIST_EMPTY;
                        //this.deviceGrpSetAll = DeviceGroup.GROUP_LIST_ALL;
                        this.deviceGrpSet    = DeviceGroup.getDeviceGroupsForAccount(this.getCurrentAccountID(),false);
                        this.deviceGrpSetAll = new OrderedSet<String>(this.deviceGrpSet); // shallow copy
                        this.deviceGrpSetAll.add(0, DeviceGroup.DEVICE_GROUP_ALL);
                    }
                } catch (DBException dbe) {
                    Print.logException("Retrieving user groups: " + user.getUserID(), dbe);
                    this.deviceGrpSet    = DeviceGroup.GROUP_LIST_EMPTY;
                    this.deviceGrpSetAll = DeviceGroup.GROUP_LIST_EMPTY;
                }
            } else {
                // no user (assume admin) get all groups for current account
                try {
                    this.deviceGrpSet    = DeviceGroup.getDeviceGroupsForAccount(this.getCurrentAccountID(),false);
                    this.deviceGrpSetAll = new OrderedSet<String>(this.deviceGrpSet);
                    this.deviceGrpSetAll.add(0, DeviceGroup.DEVICE_GROUP_ALL);
                } catch (DBException dbe) {
                    this.deviceGrpSet    = DeviceGroup.GROUP_LIST_EMPTY;
                    this.deviceGrpSetAll = DeviceGroup.GROUP_LIST_ALL;
                }
            }
        }
        return inclAll? this.deviceGrpSetAll : this.deviceGrpSet;
    }
    
    /* get the description of a specific device */
    private DeviceGroup descLastGroup = null;
    public String getDeviceGroupDescription(String grpID, boolean rtnDispName)
    {

        /* no group ID specified? */
        if (StringTools.isBlank(grpID)) {
            return "";
        }

        /* previous device group? */
        if ((this.descLastGroup != null) && 
            this.descLastGroup.getAccountID().equals(this.getCurrentAccountID()) && 
            this.descLastGroup.getGroupID().equals(grpID)) {
            String n = rtnDispName? this.descLastGroup.getDisplayName() : this.descLastGroup.getDescription();
            return !n.equals("")? n : grpID;
        }

        /* get account group-id description */
        Account acct = this.getCurrentAccount();
        if (acct != null) {
            try {
                this.descLastGroup = DeviceGroup.getDeviceGroup(acct, grpID);
                if (this.descLastGroup != null) {
                    String n = rtnDispName? this.descLastGroup.getDisplayName() : this.descLastGroup.getDescription();
                    return !n.equals("")? n : grpID;
                } else {
                    if (grpID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                        return DeviceGroup.GetDeviceGroupAllTitle(acct, this.getLocale());
                    } else {
                        return grpID;
                    }
                }
            } catch (DBException dbe) {
                // unable to read group description, return group-id
                return grpID;
            }
        }

        /* default to returning groupID */
        return grpID;

    }

    // ------------------------------------------------------------------------

    /* return the current device ID */
    public void setSelectedDeviceID(String devID)
    {
        this.setSelectedDeviceID(devID, true);
    }

    /* return the current device ID */
    public void setSelectedDeviceID(String devID, boolean isActualSpecifiedDevice)
    {
        this.selDeviceID      = devID;
        this.isActualSelDevID = isActualSpecifiedDevice;
        this.selDevice        = null;
    }

    /* sets the current device */
    public void setSelectedDevice(Device dev)
    {
        Account acct = this.getCurrentAccount();
        if (AccountRecord.isAccount(dev,acct)) {
            this.selDeviceID      = dev.getDeviceID();
            this.isActualSelDevID = true;
            this.selDevice        = dev;
        } else {
            Print.logWarn("Device does not belong to account["+acct+"]: " + dev);
            this.selDeviceID      = "";
            this.isActualSelDevID = false;
            this.selDevice        = null;
        }
    }

    /* return the current device ID/name */
    public String getSelectedDeviceID()
    {
        return (this.selDeviceID != null)? this.selDeviceID : "";
    }

    /* is actual specified device ID (ie. not a 'default' selection) */
    public boolean isActualSpecifiedDevice()
    {
        return !StringTools.isBlank(this.selDeviceID) && this.isActualSelDevID;
    }

    /* get the current Device */
    public Device getSelectedDevice()
    {
        if (this.selDevice == null) {
            String deviceID = this.getSelectedDeviceID();
            if (!StringTools.isBlank(deviceID)) {
                try {
                    Account account = this.getCurrentAccount();
                    this.selDevice = Device.getDevice(account, deviceID); // null if non-existent
                    if (this.selDevice == null) {
                        Print.logWarn("Device not found: " + deviceID);
                    }
                } catch (DBException dbe) {
                    Print.logException("Error reading Device: " + this.getCurrentAccountID() + "/" + deviceID, dbe);
                }
            }
        }
        return this.selDevice;
    }

    /* get the description of a specific device */
    private Device descLastDeviceDescription = null; // last device-description cache
    public String getDeviceDescription(String devID, boolean rtnDispName)
    {
        String devDescFields[] = new String[] {
            Device.FLD_displayName, 
            Device.FLD_description
        };

        /* no device ID specified? */
        if (StringTools.isBlank(devID)) {
            return "";
        }

        /* previous device? */
        if ((this.descLastDeviceDescription != null) && 
            this.descLastDeviceDescription.getAccountID().equals(this.getCurrentAccountID()) && 
            this.descLastDeviceDescription.getDeviceID().equals(devID)) {
            String n = rtnDispName? 
                this.descLastDeviceDescription.getDisplayName() :   // Device.FLD_displayName
                this.descLastDeviceDescription.getDescription();    // Device.FLD_description
            return !n.equals("")? n : devID;
        }

        /* get account device-id description */
        Account acct = this.getCurrentAccount();
        if (acct != null) {
            try {
                this.descLastDeviceDescription = Device._getDevice(acct, devID, devDescFields);
                if (this.descLastDeviceDescription != null) {
                    String n = rtnDispName? 
                        this.descLastDeviceDescription.getDisplayName() :  // Device.FLD_displayName
                        this.descLastDeviceDescription.getDescription();   // Device.FLD_description
                    return !n.equals("")? n : devID;
                } else {
                    return devID;
                }
            } catch (DBException dbe) {
                // -- drop through below
            }
        }

        /* default to returning deviceID */
        return devID;

    }

    /* set the list of known devices for this account */
    public void clearDeviceList()
    {
        this.deviceList = null;
    }

    /* return a list of known devices for this account, and authorized by the current user */
    public OrderedSet<String> getDeviceIDList(boolean inclInactv)
    {
        if (this.deviceList == null) {
            try {
                this.deviceList = User.getAuthorizedDeviceIDs(this.getCurrentUser(), this.getCurrentAccountID(), inclInactv);
            } catch (DBException dbe) {
                this.deviceList = new OrderedSet<String>();
            }
        }
        return this.deviceList;
    }

    /* return a list of known devices for this account */
    protected OrderedSet<String> _getDeviceIDsForSelectedGroup(boolean isFleet, boolean inclInactv)
        throws DBException
    {
        if (isFleet) {
            String accountID = this.getCurrentAccountID();
            String groupID   = this.getSelectedDeviceGroupID();
            if (!groupID.equals("") && !groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                OrderedSet<String> dList = DeviceGroup.getDeviceIDsForGroup(accountID, groupID, null/*User*/, inclInactv);
                // TODO: filter authorized devices for current user?
                return dList;
            } else {
                // return all Account devices
                return this.getDeviceIDList(inclInactv);
            }
        } else {
            return this.getDeviceIDList(inclInactv);
        }
    }

    // ------------------------------------------------------------------------

    /* return the current driver ID/name */
    public void setSelectedDriverID(String drvID)
    {
        this.setSelectedDriverID(drvID, true);
    }

    /* return the current driver ID/name */
    public void setSelectedDriverID(String drvID, boolean isActualSpecifiedDriver)
    {
        this.selDriverID      = drvID;
        this.isActualSelDrvID = isActualSpecifiedDriver;
        this.selDriver        = null;
    }

    /* return the current driver ID/name */
    public String getSelectedDriverID()
    {
        return (this.selDriverID != null)? this.selDriverID : "";
    }

    /* is actual specified driver ID (ie. not a 'default' selection) */
    public boolean isActualSpecifiedDriver()
    {
        return !StringTools.isBlank(this.selDriverID) && this.isActualSelDrvID;
    }

    /* get the current Driver */
    public Driver getSelectedDriver()
    {
        if (this.selDriver == null) {
            String driverID = this.getSelectedDriverID();
            if (!StringTools.isBlank(driverID)) {
                try {
                    Account account = this.getCurrentAccount();
                    this.selDriver = Driver.getDriver(account, driverID); // null if non-existent
                    if (this.selDriver == null) {
                        Print.logWarn("Driver not found: " + driverID);
                    }
                } catch (DBException dbe) {
                    Print.logException("Error reading Driver: " + this.getCurrentAccountID() + "/" + driverID, dbe);
                }
            }
        }
        return this.selDriver;
    }

    /* get the description of a specific driver */
    private Driver descLastDriverDescription = null; // last driver-description cache
    public String getDriverDescription(String drvID, boolean rtnDispName)
    {

        /* no driver ID specified? */
        if (StringTools.isBlank(drvID)) {
            return "";
        }

        /* previous driver? */
        if ((this.descLastDriverDescription != null) && 
            this.descLastDriverDescription.getAccountID().equals(this.getCurrentAccountID()) && 
            this.descLastDriverDescription.getDriverID().equals(drvID)) {
            String n = rtnDispName? 
                this.descLastDriverDescription.getDisplayName() :   // Driver.FLD_displayName
                this.descLastDriverDescription.getDescription();    // Driver.FLD_description
            return !n.equals("")? n : drvID;
        }

        /* get account driver-id description */
        Account acct = this.getCurrentAccount();
        if (acct != null) {
            try {
                this.descLastDriverDescription = Driver.getDriver(acct, drvID);
                if (this.descLastDriverDescription != null) {
                    String n = rtnDispName? 
                        this.descLastDriverDescription.getDisplayName() :  // Driver.FLD_displayName
                        this.descLastDriverDescription.getDescription();   // Driver.FLD_description
                    return !n.equals("")? n : drvID;
                } else {
                    return drvID;
                }
            } catch (DBException dbe) {
                // -- drop through below
            }
        }

        /* default to returning driverID */
        return drvID;

    }

    /* set the list of known drivers for this account */
    public void clearDriverList()
    {
        this.driverList = null;
    }

    /* return a list of known devices for this account */
    public OrderedSet<String> getDriverIDList()
    {
        if (this.driverList == null) {
            try {
                this.driverList = Driver.getDriverIDsForAccount(this.getCurrentAccountID());
            } catch (DBException dbe) {
                this.driverList = new OrderedSet<String>();
            }
        }
        return this.driverList;
    }

    // ------------------------------------------------------------------------

    /* create Device/DeviceGroup IDDescription list */
    public java.util.List<IDDescription> createGroupIDDescriptionList(
        boolean inclAll,
        IDDescription.SortBy sortBy)
    {
        OrderedSet<String> dgList = this.getDeviceGroupIDList(inclAll);
        java.util.List<IDDescription> idList = new Vector<IDDescription>();
        if (!ListTools.isEmpty(dgList)) {
            sortBy = IDDescription.GetSortBy(sortBy); // make sure 'sortBy' is not null
            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
            for (int i = 0; i < dgList.size(); i++) {
                String dgid = dgList.get(i); // Device/Group ID
                String desc = this.getDeviceGroupDescription(dgid,rtnDispName);
                idList.add(new IDDescription(dgid, desc));
                //Print.logInfo("DeviceGroup: " + dgid + " - " + desc);
            }
            if (rtnDispName) { sortBy = IDDescription.SortBy.DESCRIPTION; }
            IDDescription.SortList(idList, sortBy);
        }
        return idList;
    }

    /* create <Group,Description> map (sorted by description) */
    public OrderedMap<String,String> createGroupDescriptionMap(boolean inclID, boolean inclAll)
    {
        java.util.List<IDDescription> list = this.createGroupIDDescriptionList(inclAll, null);
        OrderedMap<String,String> map = new OrderedMap<String,String>();
        for (IDDescription idd : list) {
            String id   = idd.getID();
            String desc = idd.getDescription();
            if (inclID) { desc += " [" + id + "]"; }
            map.put(id, desc);
        }
        return map;
    }

    // --------------------------

    /* create Device/DeviceGroup IDDescription list */
    public java.util.List<IDDescription> createDeviceIDDescriptionList(boolean inclInactv,
        IDDescription.SortBy sortBy)
    {
        OrderedSet<String> dgList = this.getDeviceIDList(inclInactv);
        java.util.List<IDDescription> idList = new Vector<IDDescription>();
        if (!ListTools.isEmpty(dgList)) {
            sortBy = IDDescription.GetSortBy(sortBy); // makes sure 'sortBy' is not null
            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
            for (int i = 0; i < dgList.size(); i++) {
                String dgid = dgList.get(i); // Device/Group ID
                String desc = this.getDeviceDescription(dgid, rtnDispName);
                idList.add(new IDDescription(dgid, desc));
                //Print.logInfo("DeviceGroup: " + dgid + " - " + desc);
            }
            if (rtnDispName) { sortBy = IDDescription.SortBy.DESCRIPTION; }
            IDDescription.SortList(idList, sortBy);
        }
        return idList;
    }

    /* create <Device,Description> map (sorted by description) */
    public OrderedMap<String,String> createDeviceDescriptionMap(boolean inclID)
    {
        java.util.List<IDDescription> list = this.createDeviceIDDescriptionList(false/*inclInactv*/, null);
        OrderedMap<String,String> map = new OrderedMap<String,String>();
        for (IDDescription idd : list) {
            String id   = idd.getID();
            String desc = idd.getDescription();
            if (inclID) { desc += " [" + id + "]"; }
            map.put(id, desc);
        }
        return map;
    }

    // --------------------------

    /* create Driver IDDescription list */
    public java.util.List<IDDescription> createDriverIDDescriptionList(
        IDDescription.SortBy sortBy)
    {
        OrderedSet<String> dgList = this.getDriverIDList();
        java.util.List<IDDescription> idList = new Vector<IDDescription>();
        if (!ListTools.isEmpty(dgList)) {
            sortBy = IDDescription.GetSortBy(sortBy); // make sure 'sortBy' is not null
            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
            for (int i = 0; i < dgList.size(); i++) {
                String dgid = dgList.get(i); // Device/Group ID
                String desc = this.getDriverDescription(dgid, rtnDispName);
                idList.add(new IDDescription(dgid, desc));
                //Print.logInfo("Driver: " + dgid + " - " + desc);
            }
            if (rtnDispName) { sortBy = IDDescription.SortBy.DESCRIPTION; }
            IDDescription.SortList(idList, sortBy);
        }
        return idList;
    }

    /* create <Driver,Description> map (sorted by description) */
    public OrderedMap<String,String> createDriverDescriptionMap(boolean inclID)
    {
        java.util.List<IDDescription> list = this.createDriverIDDescriptionList(null);
        OrderedMap<String,String> map = new OrderedMap<String,String>();
        for (IDDescription idd : list) {
            String id   = idd.getID();
            String desc = idd.getDescription();
            if (inclID) { desc += " [" + id + "]"; }
            map.put(id, desc);
        }
        return map;
    }

    // --------------------------

    /* create Device/DeviceGroup IDDescription list */
    @Deprecated // used only by DashboardMap.java
    public java.util.List<IDDescription> createIDDescriptionList(boolean groupList, 
        IDDescription.SortBy sortBy)
    {
        boolean inclAll = true, inclInactv = false;
        OrderedSet<String> dgList = groupList? 
            this.getDeviceGroupIDList(inclAll) : 
            this.getDeviceIDList(inclInactv);
        java.util.List<IDDescription> idList = new Vector<IDDescription>();
        if (!ListTools.isEmpty(dgList)) {
            sortBy = IDDescription.GetSortBy(sortBy); // make sure 'sortBy' is not null
            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
            for (int i = 0; i < dgList.size(); i++) {
                String dgid = dgList.get(i); // Device/Group ID
                String desc = groupList? 
                    this.getDeviceGroupDescription(dgid,rtnDispName) : 
                    this.getDeviceDescription     (dgid,rtnDispName);
                idList.add(new IDDescription(dgid, desc));
                //Print.logInfo("DeviceGroup: " + dgid + " - " + desc);
            }
            if (rtnDispName) { sortBy = IDDescription.SortBy.DESCRIPTION; }
            IDDescription.SortList(idList, sortBy);
        }
        return idList;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean _didCacheZoomeRegionShapes = false;
    private Map<String,MapShape> _cacheZoomRegionShapes = null;
    public Map<String,MapShape> getZoomRegionShapes()
    {
        if (!_didCacheZoomeRegionShapes) {
            _didCacheZoomeRegionShapes = true;

            /* read Geozone defined zoom-regions */
            // TODO:

            /* read global defined zoom-regions */
            Map<String,MapShape> msList = this.getPrivateLabel().getMapShapes();
            if (!ListTools.isEmpty(msList)) {
                if (_cacheZoomRegionShapes != null) {
                    // may be used when we eventually read zoom-reagions from the Geozone table
                    _cacheZoomRegionShapes.putAll(msList);
                } else {
                    _cacheZoomRegionShapes = msList;
                }
            }
            
        }
        return _cacheZoomRegionShapes;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance contains a status-code list
    **/
    public boolean hasStatusCodes()
    {
        return ListTools.isEmpty(this.statusCodes)? false : true;
    }

    /**
    *** Gets the status-code list, or null if no status-code list defined
    **/
    public int[] getStatusCodes()
    {
        return this.statusCodes;
    }

    /**
    *** Sets the status-code list
    **/
    public void setStatusCodes(int sc[])
    {
        this.statusCodes = sc;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance contains a status-code marker
    **/
    public boolean hasStatusMarkers()
    {
        return ListTools.isEmpty(this.statusMarkers)? false : true;
    }

    /**
    *** Gets the status-code marker, or null if no status-code marker defined
    **/
    public int[] getStatusMarkers()
    {
        return this.statusMarkers;
    }

    /**
    *** Sets the status-code marker
    **/
    public void setStatusMarkers(int sm[])
    {
        this.statusMarkers = sm;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Filter accepted devices on Fleet map
    **/
    public boolean acceptDeviceForFleetMapEvents(Device device)
    {

        /* ignore null devices */
        if (device == null) {
            return false;
        }

        /* active only? ("getFleetMapActiveDevicesOnly") */
        // -- TODO: make this a map/page property setting, etc
        boolean MAP_ACTIVE_DEVICES_ONLY = false;
        if (MAP_ACTIVE_DEVICES_ONLY && !device.isActive()) {
            // -- skip inactive devices
            return false;
        }

        /* recent events only? */
        // -- TODO: make this a map/page property setting, etc
        long MAP_RECENT_DEVICE_LOCATION_AGE = 0L; // seconds
        if (MAP_RECENT_DEVICE_LOCATION_AGE > 0L) {
            long pastTS = DateTime.getCurrentTimeSec() - MAP_RECENT_DEVICE_LOCATION_AGE;
            if (device.getLastGPSTimestamp() < pastTS) {
                return false;
            }
        }

        /* rule-based selector match only? */
        String filterSelector = this.getFleetMapDeviceSelector();
        if (!StringTools.isBlank(filterSelector) && Device.hasRuleFactory()) {
            RuleFactory ruleFact = Device.getRuleFactory(); // not null here
            if (!ruleFact.isSelectorMatch(filterSelector,device)) {
                // -- rule selector returned false
                return false;
            }
        }

        /* accept */
        return true;

    }

    // --------------------------------

    /* return array of events based on requested parameters */
    public EventData[] getMapEvents()
        throws DBException
    {
        return this.getMapEvents(-1L);
    }

    /* return array of events based on requested parameters */
    public EventData[] getMapEvents(long perDevLimit)
        throws DBException
    {
        PrivateLabel privLabel = this.getPrivateLabel();
        // -- this assumes that the number of returned records is reasonable and fits in memory
        long limitCnt = this.getEventLimit(); // total record limit
        EventData.LimitType limitType = this.getEventLimitType();

        /* per device record limit */
        if (perDevLimit <= 0L) {
            perDevLimit = this.isFleet()? this.getFleetDeviceEventCount() : limitCnt;
        }

        /* notify events only */
        boolean notifyEventsOnly = this.getDeviceNotifyEventsOnly();

        /* selected date range */
        long startTime = this.getEventDateFromSec();
        long endTime   = this.getEventDateToSec();

        /* get events */
        if (this.isFleet()) {
            // -- fleet events

            // -- get account
            Account account = this.getCurrentAccount();
            if (account == null) {
                return EventData.EMPTY_ARRAY;
            }

            // -- get user
            User user = this.getCurrentUser();

            // -- get list of devices
            OrderedSet<String> devIDList = this._getDeviceIDsForSelectedGroup(true/*fleet*/,false/*inclActv*/);
            if (ListTools.isEmpty(devIDList)) {
                Print.logInfo("No devices ...");
                return EventData.EMPTY_ARRAY;
            }

            // -- not every device may have an event
            java.util.List<EventData> evList = new Vector<EventData>();
            for (int i = 0; i < devIDList.size(); i++) { // apply limit?
                String deviceID = devIDList.get(i);

                // -- omit unauthorized devices
                if ((user != null) && !user.isAuthorizedDevice(deviceID)) {
                    continue;
                }

                // -- get Device
                Device device = Device._getDevice(account, deviceID);
                if (device == null) {
                    // -- (unlikely) skip this deviceID
                    continue;
                }

                /* accept device? */
                if (!this.acceptDeviceForFleetMapEvents(device)) {
                    // -- skip this device
                    continue;
                }

                // -- start/end time for device
                long devStartTime = startTime;
                long devEndTime   = endTime;
                if (this.hasStatusMarkers()) {
                    // -- adjust devStartTime to last received markser status-code
                    int sm[] = this.getStatusMarkers(); // non-null here
                    EventData ed = device.getLastEvent(sm, devEndTime, false/*validGPS?*/);
                    if ((ed != null) && (ed.getTimestamp() > devStartTime)) {
                        devStartTime = ed.getTimestamp();
                    }
                }
                //Print.logInfo("Date Range: " + new DateTime(devStartTime) + " to " + new DateTime(devEndTime));

                /* status coedes */
                int statusCodes[] = this.getStatusCodes();

                // -- get last event(s) for Device
                if (notifyEventsOnly) {
                    EventData E = device.getLastNotifyEvent();
                    if (E != null) {
                        long ts = E.getTimestamp();
                        if ((devStartTime > 0L) && (ts < devStartTime)) {
                            // -- skip this event
                        } else
                        if ((devEndTime > 0L) && (ts > devEndTime)) {
                            // -- skip this event
                        } else {
                            evList.add(E);
                        }
                    }
                } else {
                    EventData ev[] = device.getRangeEvents(
                        devStartTime,               // startTime
                        devEndTime,                 // endTime
                        statusCodes,                // status codes
                        true,                       // validGPS (or cell lat/lon?)
                        limitType,                  // limitType (LAST)
                        perDevLimit);               // max points
                        // -- 'ev' already points to 'device'
                    if (ev != null) {
                        for (int e = 0; e < ev.length; e++) {
                            evList.add(ev[e]);
                        }
                    }
                }

                // -- limit?
                if ((limitCnt > 0L) && (evList.size() >= limitCnt)) {
                    //Print.logWarn("Limit Reached: " + evList.size());
                    break;
                }

            } // Device loop

            /* sort events (fleet) */
            switch (this.getFleetMapEventSortBy()) {
                case FLEET_EVENT_SORT_DEVICE_ID: // Device ID
                    Collections.sort(evList, EventData.getDeviceIDComparator());
                    break;
                case FLEET_EVENT_SORT_DEVICE_DESC: // Device Description
                    Collections.sort(evList, EventData.getDeviceDescriptionComparator());
                    break;
                case FLEET_EVENT_SORT_TIMESTAMP: // Event Timestamp
                    Collections.sort(evList, EventData.getTimestampComparator());
                    break;
                default: // Device ID (default)
                    Collections.sort(evList, EventData.getDeviceIDComparator());
                    break;
            }

            // -- return fleet events
            return evList.toArray(new EventData[evList.size()]);
                
        } else {
            // -- individual device events
            
            // -- selected device
            Device device = this.getSelectedDevice();
            if (device == null) {
                // -- no events for a null device
                return EventData.EMPTY_ARRAY;
            }

            // -- start/end time for device
            long devStartTime = startTime;
            long devEndTime   = endTime;
            if (this.hasStatusMarkers()) {
                // -- adjust devStartTime to last received markser status-code
                int sm[] = this.getStatusMarkers(); // non-null here
                EventData ed = device.getLastEvent(sm, devEndTime, false/*validGPS?*/);
                if ((ed != null) && (ed.getTimestamp() > devStartTime)) {
                    devStartTime = ed.getTimestamp();
                }
            }
            //Print.logInfo("Date Range: " + new DateTime(devStartTime) + " to " + new DateTime(devEndTime));

            // -- return device events
            EventData[] ev;
            if ((devStartTime <= 0L) && (devEndTime <= 0L)) {
                ev = device.getRangeEvents( // may return null
                    -1L,                        // startTime
                    -1L,                        // endTime
                    statusCodes,                // status codes
                    true,                       // validGPS
                    limitType,                  // limitType
                    perDevLimit);               // max points
            } else {
                ev = device.getRangeEvents( // may return null
                    devStartTime,               // startTime
                    devEndTime,                 // endTime
                    statusCodes,                // status codes
                    true,                       // validGPS
                    limitType,                  // limitType
                    perDevLimit);               // max points
            }
            // -- 'ev' already points to 'device'

            /* no data? */
            if (ev == null) {
                return EventData.EMPTY_ARRAY;
            }

            /* return events */
            return ev;

        }
 
    }

    // ------------------------------------------------------------------------

    /* return array of events based on requested parameters */
    public Collection<Device> getMapEventsByDevice(long perDevLimit)
        throws DBException
    {
        PrivateLabel privLabel = this.getPrivateLabel();
        // -- this assumes that the number of returned records is reasonable and fits in memory
        long limitCnt = this.getEventLimit(); // total record limit
        EventData.LimitType limitType = this.getEventLimitType();
        int statusCodes[] = this.getStatusCodes(); // may be null

        /* per device record limit */
        if (perDevLimit <= 0L) {
            perDevLimit = this.isFleet()? this.getFleetDeviceEventCount() : limitCnt;
        }
        //Print.logInfo("Limit Count: " + limitCnt + " [per device: " + perDevLimit);

        /* notify events only */
        boolean notifyEventsOnly = this.getDeviceNotifyEventsOnly();

        /* selected date range */
        long startTime = this.getEventDateFromSec();
        long endTime   = this.getEventDateToSec();

        /* get events */
        java.util.List<Device> devList = new Vector<Device>();
        if (this.isFleet()) {
            // -- fleet events

            // -- get account
            Account account = this.getCurrentAccount();
            if (account == null) {
                return null;
            }

            // -- get user
            User user = this.getCurrentUser();

            // -- get list of devices
            OrderedSet<String> devIDList = this._getDeviceIDsForSelectedGroup(true/*fleet*/,false/*inclActv*/);
            if (ListTools.isEmpty(devIDList)) {
                Print.logInfo("No devices ...");
                return null;
            }

            // -- not every device may have an event
            int evCount = 0;
            for (int i = 0; i < devIDList.size(); i++) { // apply limit?
                String deviceID = devIDList.get(i);

                // -- omit unauthorized devices
                if ((user != null) && !user.isAuthorizedDevice(deviceID)) {
                    continue;
                }

                // -- get Device
                Device device = Device._getDevice(account, deviceID);
                if (device == null) {
                    // -- (unlikely) skip this deviceID
                    continue;
                }

                // -- start/end time for device
                long devStartTime = startTime;
                long devEndTime   = endTime;
                if (this.hasStatusMarkers()) {
                    // -- adjust devStartTime to last received markser status-code
                    int sm[] = this.getStatusMarkers(); // non-null here
                    EventData ed = device.getLastEvent(sm, devEndTime, false/*validGPS?*/);
                    if ((ed != null) && (ed.getTimestamp() > devStartTime)) {
                        devStartTime = ed.getTimestamp();
                    }
                }
                //Print.logInfo("Date Range: " + new DateTime(devStartTime) + " to " + new DateTime(devEndTime));

                // -- get last event(s) for Device
                EventData ev[] = null;
                if (notifyEventsOnly) {
                    EventData E = device.getLastNotifyEvent();
                    if (E != null) {
                        long ts = E.getTimestamp();
                        if ((devStartTime > 0L) && (ts < devStartTime)) {
                            // -- skip this event
                        } else
                        if ((devEndTime > 0L) && (ts > devEndTime)) {
                            // -- skip this event
                        } else {
                            ev = new EventData[] { E };
                        }
                    }
                } else {
                    ev = device.getRangeEvents(
                        devStartTime,               // startTime
                        devEndTime,                 // endTime
                        statusCodes,                // status codes
                        true,                       // validGPS
                        limitType,                  // limitType (LAST)
                        perDevLimit);               // max points
                        // 'ev' already points to 'device'
                }
                if (!ListTools.isEmpty(ev)) {
                    device.setSavedRangeEvents(ev);
                    devList.add(device);
                    evCount += ev.length;
                }

                // limit?
                if ((limitCnt > 0L) && (evCount >= limitCnt)) {
                    //Print.logWarn("Limit Reached: " + evList.size());
                    break;
                }

            } // Device loop

            /* sort by Device Description (fleet) */
            Collections.sort(devList, Device.getDeviceDescriptionComparator());
            switch (this.getFleetMapEventSortBy()) {
                case FLEET_EVENT_SORT_DEVICE_ID: // Device ID
                    //Collections.sort(devList, Device.getDeviceIDComparator());  // TODO: not yet supported
                    Collections.sort(devList, Device.getDeviceDescriptionComparator());
                    break;
                case FLEET_EVENT_SORT_DEVICE_DESC: // Device Description
                    Collections.sort(devList, Device.getDeviceDescriptionComparator());
                    break;
                default: // Device Description (default)
                    Collections.sort(devList, Device.getDeviceDescriptionComparator());
                    break;
            }

            // -- return fleet events
            //Print.logWarn("Event Count: " + evCount);
            return !ListTools.isEmpty(devList)? devList : null;
                
        } else {
            // -- individual device events

            // -- selected device
            Device device = this.getSelectedDevice();
            if (device == null) {
                // -- no events for a null device
                return null;
            }

            // -- start/end time for device
            long devStartTime = startTime;
            long devEndTime   = endTime;
            if (this.hasStatusMarkers()) {
                // -- adjust devStartTime to last received markser status-code
                int sm[] = this.getStatusMarkers(); // non-null here
                EventData ed = device.getLastEvent(sm, devEndTime, false/*validGPS?*/);
                if ((ed != null) && (ed.getTimestamp() > devStartTime)) {
                    devStartTime = ed.getTimestamp();
                }
            }
            //Print.logInfo("Date Range: " + new DateTime(devStartTime) + " to " + new DateTime(devEndTime));

            // -- return device events
            EventData[] ev;
            if ((devStartTime <= 0L) && (devEndTime <= 0L)) {
                ev = device.getRangeEvents( // may return null
                    -1L,                        // startTime
                    -1L,                        // endTime
                    statusCodes,                // status codes
                    true,                       // validGPS
                    limitType,                  // limitType
                    perDevLimit);               // max points
            } else {
                ev = device.getRangeEvents( // may return null
                    devStartTime,               // startTime
                    devEndTime,                 // endTime
                    statusCodes,                // status codes
                    true,                       // validGPS
                    limitType,                  // limitType
                    perDevLimit);               // max points
            }
            // 'ev' already points to 'device'

            /* no data? */
            if (!ListTools.isEmpty(ev)) {
                device.setSavedRangeEvents(ev);
                devList.add(device);
            }

            /* return events */
            return !ListTools.isEmpty(devList)? devList : null;

        }
 
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return true if addresses are to be displayed */
    public boolean getShowAddress()
    {
        Account acct = this.getCurrentAccount();
        if (acct == null) {
            // account not available
            return false;
        } else
        if (Account.getGeocoderMode(acct).isNone()) {
            // no reverse-geocoding performed/allowed, thus no address
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /* set the event-range date "From" value */
    public void setEventDateFrom(DateTime fr)
    {
        this.dateFrom = fr;
    }

    /* return the event-range date "From" value */
    public DateTime getEventDateFrom()
    {
        return this.dateFrom; // may be null
    }
    public long getEventDateFromSec()
    {
        return (this.dateFrom != null)? this.dateFrom.getTimeSec() : -1L;
    }

    /* set the event-range date "To" value */
    public void setEventDateTo(DateTime to)
    {
        this.dateTo = to;
    }

    /* return the event-range date "To" value */
    public DateTime getEventDateTo()
    {
        return this.dateTo; // may be null
    }
    public long getEventDateToSec()
    {
        return (this.dateTo != null)? this.dateTo.getTimeSec() : -1L;
    }

    // ------------------------------------------------------------------------

    /* return date format (ie. "yyyy/MM/dd", etc) */
    public String getDateFormat()
    {
        return this.getPrivateLabel().getDateFormat();
    }
    
    // ------------------------------------------------------------------------

    /* set the current time zone */
    public void setTimeZone(TimeZone tz, String tzStr)
    {
        this.timeZone = tz;
        this.timeZoneLongStr = tzStr;
    }

    /* set the current time zone string representation */
    //public void setTimeZoneString(String tzStr)
    //{
    //    this.timeZoneLongStr = tzStr;
    //}

    /* get the curent time zone as a String */
    public String getTimeZoneString(DateTime dt)
    {

        /* initialize Timezone String */
        if (StringTools.isBlank(this.timeZoneLongStr)) {
            String tzStr = null;
            
            /* User/Account timezone string */
            Account a = this.getCurrentAccount();
            User    u = this.getCurrentUser();
            if (u != null) {
                // try User timezone
                tzStr = u.getTimeZone(); // may be blank
                if (StringTools.isBlank(tzStr) && (a != null)) {
                    // override with Account timezone
                    tzStr = a.getTimeZone();
                    //Print.logInfo("Account TimeZone: " + tzStr);
                } else {
                    //Print.logInfo("User TimeZone: " + tzStr);
                }
            } else 
            if (a != null) {
                // get Account timezone
                tzStr = a.getTimeZone();
                //Print.logInfo("Account TimeZone: " + tzStr);
            }

            /* still no timezone? */
            if (StringTools.isBlank(tzStr)) {
                // make sure we have a timezone 
                tzStr = Account.GetDefaultTimeZone();
            }

            /* set timezone string */
            this.timeZoneLongStr = tzStr;
            //Print.logInfo("Using TimeZone: " + tzStr);

        }

        /* return short/long name? */
        if (dt != null) {
            if (StringTools.isBlank(this.timeZoneShortStr)) {
                TimeZone tz = DateTime.getTimeZone(this.timeZoneShortStr, null);
                if (tz != null) {
                    boolean dst = dt.isDaylightSavings(tz);
                    this.timeZoneShortStr = tz.getDisplayName(dst, TimeZone.SHORT);
                } else {
                    // timezone iz invalid
                    this.timeZoneShortStr = this.timeZoneLongStr;
                }
            }
            return this.timeZoneShortStr;
        } else {
            return this.timeZoneLongStr;
        }

    }

    /* set the current time zone */
    public void setTimeZone(TimeZone tz)
    {
        this.timeZone = tz;
    }

    /* get the current time zone */
    // -- does not return null
    public TimeZone getTimeZone()
    {
        if (this.timeZone != null) {
            return this.timeZone;
        } else
        if (this.dateFrom != null) {
            this.timeZone = this.dateFrom.getTimeZone();
            return this.timeZone;
        } else {
            this.timeZone = DateTime.getTimeZone(this.getTimeZoneString(null));
            return this.timeZone;
        }
    }
    
    /* return the list of time zones */
    public java.util.List<String> getTimeZonesList()
    {
        String tmz = this.getTimeZoneString(null);
        java.util.List<String> tzList = this.getPrivateLabel().getTimeZonesList();
        if (!StringTools.isBlank(tmz) && !tzList.contains(tmz)) {
            tzList = new Vector<String>(tzList);
            tzList.add(tmz);
        }
        return tzList;
    }

    // ------------------------------------------------------------------------

    /* set the event retrieval limit */
    public void setEventLimit(long limitCnt)
    {
        this.eventLimitCnt = limitCnt;
    }

    /* get event retrieval limit */
    public long getEventLimit()
    {
        return this.eventLimitCnt;
    }

    // ------------------------------------------------------------------------

    /* set event retrieval limit type [first/last] */
    public void setEventLimitType(EventData.LimitType limitType)
    {
        this.eventLimitType = (limitType != null)? limitType : EventData.LimitType.LAST;
    }

    /* set event retrieval limit type [first/last] */
    public void setEventLimitType(String limitType)
    {
        this.eventLimitType = (limitType != null) && limitType.equalsIgnoreCase("first")? 
            EventData.LimitType.FIRST : EventData.LimitType.LAST;
    }

    /* get event retrieval limit type [first/last] */
    public EventData.LimitType getEventLimitType()
    {
        return this.isFleet()? EventData.LimitType.LAST : 
            ((this.eventLimitType != null)? this.eventLimitType : EventData.LimitType.LAST);
    }

    // ------------------------------------------------------------------------
    
    /* set the last event time */
    public void setLastEventTime(DateTime lastTime)
    {
        this.lastEvent = lastTime;
    }

    /* return true if the last event time was defined */
    public boolean hasLastEvent()
    {
        return (this.lastEvent != null) && (this.lastEvent.getTimeSec() > 0L);
    }

    /* return the last event time */
    public DateTime getLastEventTime()
    {
        return this.lastEvent;
    }

    // ------------------------------------------------------------------------

    /* return the last event time as a string */
    public String formatDateTime(DateTime dt)
    {
        return this.formatDateTime(dt, this.getTimeZone(), "");
    }

    /* return the last event time as a string */
    public String formatDateTime(DateTime dt, String dft)
    {
        return this.formatDateTime(dt, this.getTimeZone(), dft);
    }

    /* return the last event time as a string */
    public String formatDateTime(long timestamp)
    {
        return this.formatDateTime(timestamp, this.getTimeZone(), "");
    }

    /* return the last event time as a string */
    public String formatDateTime(long timestamp, String dft)
    {
        return this.formatDateTime(timestamp, this.getTimeZone(), dft);
    }

    /* return the last event time as a string */
    public String formatDateTime(long timestamp, TimeZone tmz)
    {
        return this.formatDateTime(timestamp, tmz, "");
    }

    /* return the last event time as a string */
    public String formatDateTime(long timestamp, TimeZone tmz, String dft)
    {
        return (timestamp > 0L)? this.formatDateTime(new DateTime(timestamp,tmz),tmz,dft) : dft;
    }

    /* return the last event time as a string */
    public String formatDateTime(DateTime dt, TimeZone tmz)
    {
        return this.formatDateTime(dt, tmz, "");
    }

    /* return the last event time as a string */
    public String formatDateTime(DateTime dt, TimeZone tmz, String dft)
    {
        if ((dt != null) && (dt.getTimeSec() > 0L)) {
            Account a = this.getCurrentAccount();
            String dateFmt = (a != null)? a.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
            String timeFmt = (a != null)? a.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();
            return dt.format(dateFmt + " " + timeFmt + " z", tmz);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    /* return the last event time as a string */
    public String formatDayNumber(long dayNumber)
    {
        return this.formatDayNumber(dayNumber, "");
    }

    /* return the last event time as a string */
    public String formatDayNumber(long dayNumber, String dft)
    {
        return (dayNumber > 0L)? this.formatDayNumber(new DayNumber(dayNumber),dft) : dft;
    }

    /* return the last event time as a string */
    public String formatDayNumber(DayNumber dn)
    {
        return this.formatDayNumber(dn, "");
    }

    /* return the last event time as a string */
    public String formatDayNumber(DayNumber dn, String dft)
    {
        if ((dn != null) && (dn.getDayNumber() > 0L)) {
            Account a = this.getCurrentAccount();
            String dateFmt = (a != null)? a.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
            return dn.format(dateFmt);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    /* return the speed units */
    public Account.SpeedUnits getSpeedUnits()
    {
        return Account.getSpeedUnits(this.getCurrentAccount());
    }

    /* return the distance units */
    public Account.DistanceUnits getDistanceUnits()
    {
        return Account.getDistanceUnits(this.getCurrentAccount());
    }

    /* return the temperature units */
    public Account.TemperatureUnits getTemperatureUnits()
    {
        return Account.getTemperatureUnits(this.getCurrentAccount());
    }

    // ------------------------------------------------------------------------
    // This section provide opportunity for the Account to orverride the default name/title
    // of the element "title".

    /* return the "Device" title for this account */
    // IE. "Device", "Tractor", "Taxi", etc
    public String[] getDeviceTitles()
    {
        Locale  locale   = this.getLocale();
        Account account  = this.getCurrentAccount();
        String  titles[] = (account != null)? account.getDeviceTitles(locale) : null;
        return (titles != null)? titles : Device.GetTitles(locale);
    }

    /* return the "Device Group" titles for this account */
    // IE. "Group", "Fleet", etc.
    public String[] getDeviceGroupTitles()
    {
        Locale  locale   = this.getLocale();
        Account account  = this.getCurrentAccount();
        String  titles[] = (account != null)? account.getDeviceGroupTitles(locale) : null;
        return (titles != null)? titles : DeviceGroup.GetTitles(locale);
    }

    /* return the "Entity" titles for this account */
    // IE. "Entity", "Trailer", "Package", etc.
    public String[] getEntityTitles()
    {
        Locale  locale   = this.getLocale();
        Account account  = this.getCurrentAccount();
        String  titles[] = (account != null)? account.getEntityTitles(locale) : null;
        return (titles != null)? titles : new String[] { "", "" };
    }

    /* return the "Address" titles for this account */
    // IE. "Address", "Landmark", etc.
    public String[] getAddressTitles()
    {
        Locale  locale   = this.getLocale();
        Account account  = this.getCurrentAccount();
        String  titles[] = (account != null)? account.getAddressTitles(locale) : null;
        return (titles != null)? titles : new String[] { "", "" };
    }

    // ------------------------------------------------------------------------

    public void setShowPassword(boolean showPass)
    {
        this.showPassword = showPass? 1 : 0;
    }

    /**
    *** Returns true if the password field should be displayed on the login page
    **/
    public boolean getShowPassword()
    {
        if (this.showPassword < 0) {
            return this.getPrivateLabel().getShowPassword();
        } else {
            return (this.showPassword > 0)? true : false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the Locale selection pull-down should be displayed on the login page
    **/
    public boolean showLocaleSelection()
    {
        boolean dft = false;
        String v = this.getKeyValue(PrivateLabel.PROP_AccountLogin_showLocaleSelection, null, null);
        if ((v != null) && ListTools.isScaler(v)) {
            return StringTools.parseBoolean(v,dft);
        } else {
            return dft;
        }
    }

    // ------------------------------------------------------------------------

    /* set 'demo' mode */
    public void setEnableDemo(boolean enableDemo)
    {
        this.enableDemo = enableDemo? 1 : 0;
    }

    /* get 'demo' mode */
    public boolean getEnableDemo()
    {
        if (this.enableDemo < 0) {
            return this.getPrivateLabel().getEnableDemo();
        } else {
            return (this.enableDemo > 0)? true : false;
        }
    }

    /* return 'demo' accountID */
    public String getDemoAccountID()
    {
        return this.getEnableDemo()? Account.GetDemoAccountID() : "";
    }

    /* 'true' if this is the demo account */
    public boolean isDemoAccount()
    {
        return this.getEnableDemo() && this.getCurrentAccountID().equals(this.getDemoAccountID());
    }

    /* get demo device date range */
    public String[] getDemoDateRange()
    {
        if (this.getEnableDemo()) {
            return Account.GetDemoDeviceDateRange(this.getDemoAccountID(),this.getSelectedDeviceID());
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private DataModel dataModel = null;

    /**
    *** Sets the page data model
    **/
    public void setDataModel(DataModel datamodel)
    {
        this.dataModel = dataModel;
    }

    /**
    *** returns true if the page data model is defined
    **/
    public boolean hasDataModel()
    {
        return (this.dataModel != null)? true : false;
    }

    /**
    *** Gets the page data model
    **/
    public DataModel getDataModel()
    {
        return this.dataModel;
    }

    // ------------------------------------------------------------------------

    /**
    *** KeyValue specific to RequestProperties
    **/
    private static interface KeyValue
    {
        public String getValue(RequestProperties reqState, String arg);
    }

    // ------------------------------------------------------------------------
    // -- I18N map

    public static final String KEY_i18n                 = "i18n";
    public static final String KEY_i18n_                = "i18n.";
    public static final String KEY_i18n_Login           = "Login";
    public static final String KEY_i18n_Account         = "Account";
    public static final String KEY_i18n_Accounts        = "Accounts";
    public static final String KEY_i18n_Device          = "Device";
    public static final String KEY_i18n_Devices         = "Devices";
    public static final String KEY_i18n_Vehicle         = "Vehicle";
    public static final String KEY_i18n_Vehicles        = "Vehicles";
    public static final String KEY_i18n_User            = "User";
    public static final String KEY_i18n_Users           = "Users";
    public static final String KEY_i18n_Group           = "Group";
    public static final String KEY_i18n_Groups          = "Groups";

    private static Map<String,Object> reqI18NKeyMap = null; // KeyValue, I18N.Text

    private static Map<String,Object> _getI18NKeyMap()
    {

        /* already initialized? */
        if (RequestProperties.reqI18NKeyMap != null) {
            return RequestProperties.reqI18NKeyMap;
        }

        /* initialize */
        OrderedMap<String,Object> i18nMap = new OrderedMap<String,Object>() { // case-insensitive
            public String toString() {
                return "FTL_I18NMap";
            }
        };
        i18nMap.setIgnoreCase(true);
        RequestProperties.reqI18NKeyMap = i18nMap;

        /* Misc */
        i18nMap.put(KEY_i18n_Login,I18N.getString(RequestProperties.class,"RequestProperties.login","Login"));

        /* Account/Accounts */
        i18nMap.put(KEY_i18n_Account,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                if (acct != null) {
                    try {
                        AccountString as = AccountString.getAccountString(acct, AccountString.ID_ACCOUNT);
                        if ((as != null) && as.hasSingularTitle()) {
                            return as.getSingularTitle(); // String
                        }
                    } catch (DBException dbe) {
                        // -- ignore
                    }
                }
                return Account.GetTitles(reqState.getLocale())[0];
            }
        });
        i18nMap.put(KEY_i18n_Accounts,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                if (acct != null) {
                    try {
                        AccountString as = AccountString.getAccountString(acct, AccountString.ID_ACCOUNT);
                        if ((as != null) && as.hasPluralTitle()) {
                            return as.getPluralTitle();
                        }
                    } catch (DBException dbe) {
                        // -- ignore
                    }
                }
                return Account.GetTitles(reqState.getLocale())[1];
            }
        });

        /* Device/Devices */
        i18nMap.put(KEY_i18n_Device,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                if (acct != null) {
                    try {
                        AccountString as = AccountString.getAccountString(acct, AccountString.ID_DEVICE);
                        if ((as != null) && as.hasSingularTitle()) {
                            return as.getSingularTitle();
                        }
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
                return Device.GetTitles(reqState.getLocale())[0];
            }
        });
        i18nMap.put(KEY_i18n_Devices,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                if (acct != null) {
                    try {
                        AccountString as = AccountString.getAccountString(acct, AccountString.ID_DEVICE);
                        if ((as != null) && as.hasPluralTitle()) {
                            return as.getPluralTitle();
                        }
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
                return Device.GetTitles(reqState.getLocale())[1];
            }
        });

        /* Vehicle/Vehicles */
        i18nMap.put(KEY_i18n_Vehicle, i18nMap.get(KEY_i18n_Device));
        i18nMap.put(KEY_i18n_Vehicles, i18nMap.get(KEY_i18n_Devices));

        /* Group/Groups */
        i18nMap.put(KEY_i18n_Group,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                if (acct != null) {
                    try {
                        AccountString as = AccountString.getAccountString(acct, AccountString.ID_DEVICE_GROUP);
                        if ((as != null) && as.hasSingularTitle()) {
                            return as.getSingularTitle();
                        }
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
                return DeviceGroup.GetTitles(reqState.getLocale())[0];
            }
        });
        i18nMap.put(KEY_i18n_Groups,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                if (acct != null) {
                    try {
                        AccountString as = AccountString.getAccountString(acct, AccountString.ID_DEVICE_GROUP);
                        if ((as != null) && as.hasPluralTitle()) {
                            return as.getPluralTitle();
                        }
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
                return Device.GetTitles(reqState.getLocale())[1];
            }
        });

        /* User/Users */
        i18nMap.put(KEY_i18n_User,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                if (acct != null) {
                    try {
                        AccountString as = AccountString.getAccountString(acct, AccountString.ID_USER);
                        if ((as != null) && as.hasSingularTitle()) {
                            return as.getSingularTitle();
                        }
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
                I18N i18n = I18N.getI18N(RequestProperties.class, reqState.getLocale());
                return i18n.getString("RequestProperties.user","User");
            }
        });
        i18nMap.put(KEY_i18n_Users,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                if (acct != null) {
                    try {
                        AccountString as = AccountString.getAccountString(acct, AccountString.ID_USER);
                        if ((as != null) && as.hasSingularTitle()) {
                            return as.getPluralTitle();
                        }
                    } catch (DBException dbe) {
                        // ignore
                    }
                }
                I18N i18n = I18N.getI18N(RequestProperties.class, reqState.getLocale());
                return i18n.getString("RequestProperties.users","Users");
            }
        });

        /* return initialized map */
        return RequestProperties.reqI18NKeyMap;

    } // _getI18NKeyMap()
    
    // --------------------------------

    /**
    *** I18N String accessor (Freemarker)
    **/
    public class FTL_I18NAccessor
        extends FTLKeyValueHash
    {
        public FTL_I18NAccessor() {
            super(true);
        }
        protected Object _getValue(String key, String arg, Object dft) {
            RequestProperties reqState = RequestProperties.this;
            // -- get Locale text
            Object locText = null;
            for (;;) { // single pass loop
                Map<String,Object> i18nMap = RequestProperties._getI18NKeyMap();
                // -- full key
                locText = i18nMap.get(key);
                if (locText != null) {
                    break;
                }
                // -- remove prefixing "i18n."
                if (key.startsWith(KEY_i18n_)) { // ie. "i18n.Devices"
                    String k = key.substring(KEY_i18n_.length());
                    locText = i18nMap.get(k);
                    if (locText != null) {
                        break;
                    }
                }
                // -- PrivateLabel i18n Strings (full key)
                locText = reqState.getPrivateLabel().getI18NTextString(key, null);
                if (locText != null) {
                    break;
                }
                // -- not found
                locText = null;
                break; // exit SPL
            }
            // -- return localized String
            if (locText == null) {
                return dft;
            } else
            if (locText instanceof KeyValue) {
                return ((KeyValue)locText).getValue(reqState, null);
            } else
            if (locText instanceof I18N.Text) {
                return ((I18N.Text)locText).toString(reqState.getLocale());
            } else {
                return locText.toString();
            }
        }
    }

    private FTLKeyValueHash i18bAccessor_blank = null;

    /**
    *** Create a new I18N String accessor object (Freemarker)
    **/
    public Object ftl_I18NAccessor() 
    {
        if (this.i18bAccessor_blank == null) {
            // -- lazy init
            this.i18bAccessor_blank = new FTL_I18NAccessor();
        }
        return this.i18bAccessor_blank;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Property map

    public static final String KEY_faviconLink          = "faviconLink";
    public static final String KEY_pageName             = "pageName";
    public static final String KEY_navigation           = "navigation";
    public static final String KEY_pageTitle            = "pageTitle";
    public static final String KEY_author               = "author";
    public static final String KEY_copyright            = "copyright";
    public static final String KEY_isLoggedIn           = "isLoggedIn";
    public static final String KEY_hideBanner           = "hideBanner";
    public static final String KEY_hideNavigation       = "hideNavigation";
    public static final String KEY_loginURL             = "loginURL";
    public static final String KEY_loginCount           = "loginCount";
    public static final String KEY_accountID            = "accountID";
    public static final String KEY_accountDesc          = "accountDesc";
    public static final String KEY_accountJsp           = "accountJsp";
    public static final String KEY_deviceID             = "deviceID";
    public static final String KEY_deviceDesc           = "deviceDesc";
    public static final String KEY_driverDesc           = "driverDesc";
    public static final String KEY_groupID              = "groupID";
    public static final String KEY_groupDesc            = "groupDesc";
    public static final String KEY_userID               = "userID";
    public static final String KEY_userDesc             = "userDesc";
    public static final String KEY_speedUnits           = "speedUnits";
    public static final String KEY_altitudeUnits        = "altitudeUnits";
    public static final String KEY_accuracyUnits        = "accuracyUnits";
    public static final String KEY_distanceUnits        = "distanceUnits";
    public static final String KEY_economyUnits         = "economyUnits";
    public static final String KEY_pressureUnits        = "pressureUnits";
    public static final String KEY_temperatureUnits     = "temperatureUnits";
    public static final String KEY_volumeUnits          = "volumeUnits";
    public static final String KEY_currency             = "currency";
    public static final String KEY_currencySymbol       = "currencySymbol";
    public static final String KEY_statusCodeDesc       = "statusCodeDesc";
    public static final String KEY_version              = "version";
    public static final String KEY_hostname             = "hostname";
    public static final String KEY_locale               = "locale";
    public static final String KEY_isLocaleRTL          = "isLocaleRTL";
    public static final String KEY_localeDirection      = "localeDirection";
    public static final String KEY_ipAddress            = "ipAddress";
    public static final String KEY_privateLabelName     = "privateLabelName";
    public static final String KEY_platinumLogoURL      = "platinumLogoURL";

    private static Map<String,Object> reqPropKeyMap = null; // KeyValue, I18N.Text, scaler, container

    private static Map<String,Object> _getPropertyKeyMap()
    {

        /* already initialized? */
        if (reqPropKeyMap != null) {
            return reqPropKeyMap;
        }

        /* initialize */
        OrderedMap<String,Object> propMap = new OrderedMap<String,Object>() { // case-insensitive
            public String toString() {
                return "FTL_PropMap";
            }
        };
        propMap.setIgnoreCase(true);
        RequestProperties.reqPropKeyMap = propMap; // case-insensitive

        /* Page */
        propMap.put(KEY_faviconLink,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return reqState.getFaviconLink();
            }
        });
        propMap.put(KEY_pageName,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return reqState.getPageName();
            }
        });
        propMap.put(KEY_navigation,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return reqState.getPageNavigationHTML();
            }
        });
        propMap.put(KEY_pageTitle,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                String title = privLabel.getPageTitle();
                if (StringTools.isBlank(title)) {
                    Print.logWarn("PageTitle is blank: " + privLabel.getName());
                }
                return title;
            }
        });
        propMap.put(KEY_author,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                String author = privLabel.getAuthor();
                return StringTools.blankDefault(author,"Geo"+"Telematic "+"Solutions,"+"Inc.");
            }
        });
        propMap.put(KEY_copyright,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                String copyright = privLabel.getCopyright();
                return copyright;
            }
        });

        /* hide banner/navigation */
        propMap.put(KEY_hideBanner,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                Account acct = reqState.getCurrentAccount(); // may be null;
                // -- "isLoggedIn" && "login.hideBannerAfterLogin"
                boolean isLoggedIn = (acct != null); // loggrd-in?
                boolean hideAfterLogin = StringTools.parseBoolean(reqState.getKeyValue(PrivateLabel.PROP_login_hideBannerAfterLogin,null,null),false);
                if (isLoggedIn && hideAfterLogin) {
                    return "true";
                }
                // -- (pageName == PAGE_LOGIN) && (banner300Image != "")
                String pageName = reqState.getPageName();
                if (Constants.PAGE_LOGIN.equals(pageName)) { // "login" page
                    String banner300 = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_banner300_image,null,null);
                    if (!StringTools.isBlank(banner300)) {
                        return "true"; // special login page
                    }
                }
                // -- do not hide banner
                return "false";
            }
        });
        propMap.put(KEY_hideNavigation,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                // -- (pageName == PAGE_LOGIN) && (banner300Image != "")
                String pageName = reqState.getPageName();
                if (Constants.PAGE_LOGIN.equals(pageName)) { // "login" page
                    String banner300 = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_banner300_image,null,null);
                    if (!StringTools.isBlank(banner300)) {
                        return "true"; // special login page
                    }
                }
                // -- do not hide navigation
                return "false";
            }
        });

        /* Login */
        propMap.put(KEY_isLoggedIn,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                //Print.logInfo("IsLoggedIn: " + ((acct != null)? acct.getAccountID() : "false"));
                boolean isLoggedIn = (acct != null);
                return isLoggedIn? "true" : "false"; // I18N?
            }
        });
        propMap.put(KEY_loginURL,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                HttpServletRequest request = reqState.getHttpServletRequest();
                if (request != null) {
                    // http://localhost:8080/track/XXXX
                    String url = StringTools.trim(request.getRequestURL().toString());
                    int p = url.lastIndexOf("/");
                    if (p > 0) {
                        // Strip "/XXXX" and append baseURI
                        // TODO: may still need some tweaking 
                        String baseURI = RequestProperties.TRACK_BASE_URI();
                        if (baseURI.startsWith(".")) { baseURI = baseURI.substring(1); }
                        url = url.substring(0,p) + baseURI;
                        RTProperties hostProps = (RTProperties)AttributeTools.getSessionAttribute(request, CommonServlet.HOST_PROPERTIES, null);
                        // "lfid" host properties ID
                        String hostPropID = (hostProps != null)? hostProps.getString(CommonServlet.HOST_PROPERTIES_ID,null) : null;
                        if (!StringTools.isBlank(hostPropID)) {
                            url += "?" + CommonServlet.HOST_PROPERTIES_ID + "=" + hostPropID;
                        } else {
                            url += "?" + CommonServlet.HOST_PROPERTIES_ID + "=" + CommonServlet.DEFAULT_HOST_PROPERTIES_ID;
                        }
                        // "DebugProperties"
                        //boolean debugPP = hostProps.getBoolean(EventData.DEBUG_PUSHPINS,false);
                        //if (debugPP) {
                        //    url += "&" + EventData.DEBUG_PUSHPINS[0] + "=true";
                        //}
                    }
                    return url;
                } else {
                    return "";
                }
            }
        });
        propMap.put(KEY_loginCount,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                int count = reqState.GetLoginCount();
                if (count < 0) {
                    // unable to determine
                    return "?";
                } else
                if (reqState.isLoggedIn()) {
                    // count current Account/User login sessions
                    return String.valueOf(count);
                } else {
                    // count all login sessions
                    return String.valueOf(count) + "*";
                }
            }
        });

        /* Account */
        propMap.put(KEY_accountID,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                String acctID = reqState.getCurrentAccountID();
                return !StringTools.isBlank(acctID)? acctID : null;
            }
        });
        propMap.put(KEY_accountDesc,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return (acct != null)? acct.getDescription() : null;
            }
        });
        propMap.put(KEY_accountJsp,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return (acct != null)? StringTools.blankDefault(acct.getPrivateLabelJsp(),null) : null;
            }
        });

        /* Device */
        propMap.put(KEY_deviceID,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                String selDevID = reqState.getSelectedDeviceID();
                if (!StringTools.isBlank(selDevID)) {
                    return selDevID;
                }
                Device selDev = reqState.getSelectedDevice();
                if (selDev != null) {
                    return selDev.getDeviceID();
                }
                Print.logWarn("RequestProperties does not have a selected Device");
                return null;
            }
        });
        propMap.put(KEY_deviceDesc,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Device selDev = reqState.getSelectedDevice();
                if (selDev != null) {
                    return selDev.getDescription();
                }
                Print.logWarn("RequestProperties does not have a selected Device");
                return null;
            }
        });

        /* DeviceGroup */
        propMap.put(KEY_groupID,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                String selGrpID = reqState.getSelectedDeviceGroupID();
                if (!StringTools.isBlank(selGrpID)) {
                    return selGrpID;
                }
                DeviceGroup selGrp = reqState.getSelectedDeviceGroup();
                if (selGrp != null) {
                    return selGrp.getGroupID();
                }
                Print.logWarn("RequestProperties does not have a selected DeviceGroup");
                return null;
            }
        });
        propMap.put(KEY_groupDesc,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                DeviceGroup selGrp = reqState.getSelectedDeviceGroup();
                if (selGrp != null) {
                    return selGrp.getDescription();
                }
                Print.logWarn("RequestProperties does not have a selected DeviceGroup");
                return null;
            }
        });

        /* User */
        propMap.put(KEY_userID,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                String userID = reqState.getCurrentUserID();
                return !StringTools.isBlank(userID)? userID : null;
            }
        });
        propMap.put(KEY_userDesc,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return (reqState.user != null)? reqState.user.getDescription() : User.getAdminUserID();
            }
        });

        /* Driver */
        propMap.put(KEY_driverDesc,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Driver selDrv = reqState.getSelectedDriver();
                if (selDrv != null) {
                    return selDrv.getDescription();
                }
                Print.logWarn("RequestProperties does not have a selected Driver");
                return null;
            }
        });

        /* Units */
        propMap.put(KEY_speedUnits, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return Account.getSpeedUnits(acct).toString(reqState.getLocale());
            }
        });
        propMap.put(KEY_distanceUnits, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return Account.getDistanceUnits(acct).toString(reqState.getLocale());
            }
        });
        propMap.put(KEY_altitudeUnits, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                I18N i18n = I18N.getI18N(RequestProperties.class, reqState.getLocale());
                Account.AltitudeUnits altUnits = Account.getAltitudeUnits(acct);
                if (altUnits.isFeet()) {
                    return i18n.getString("RequestProperties.feet","feet");
                } else
                if (altUnits.isMeters()) {
                    return i18n.getString("RequestProperties.meters","meters");
                } else {
                    return altUnits.toString(reqState.getLocale());
                }
            }
        });
        propMap.put(KEY_accuracyUnits, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                I18N i18n = I18N.getI18N(RequestProperties.class, reqState.getLocale());
                Account.AltitudeUnits altUnits = Account.getAltitudeUnits(acct);
                if (altUnits.isFeet()) {
                    return i18n.getString("RequestProperties.feet","feet");
                } else
                if (altUnits.isMeters()) {
                    return i18n.getString("RequestProperties.meters","meters");
                } else {
                    return altUnits.toString(reqState.getLocale());
                }
            }
        });
        propMap.put(KEY_economyUnits, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return Account.getEconomyUnits(acct).toString(reqState.getLocale());
            }
        });
        propMap.put(KEY_pressureUnits, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return Account.getPressureUnits(acct).toString(reqState.getLocale());
            }
        });
        propMap.put(KEY_temperatureUnits, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return Account.getTemperatureUnits(acct).toString(reqState.getLocale());
            }
        });
        propMap.put(KEY_volumeUnits, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return Account.getVolumeUnits(acct).toString(reqState.getLocale());
            }
        });

        /* Currency */
        propMap.put(KEY_currency, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return Account.getCurrency(acct);
            }
        });
        propMap.put(KEY_currencySymbol, new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                Account acct = reqState.getCurrentAccount(); // may be null;
                return Account.getCurrencySymbol(acct);
            }
        });

        /* Status Code */
        propMap.put(KEY_statusCodeDesc,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                int sc = StringTools.parseInt(arg,-1);
                if (sc <= 0) {
                    return StatusCodes.GetDescription(StatusCodes.STATUS_NONE,null);
                } else {
                    PrivateLabel privLabel = reqState.getPrivateLabel();
                    String acctID = reqState.getCurrentAccountID();
                    return StatusCode.getDescription(acctID, sc, privLabel, null);
                }
            }
        });

        /* Locale */
        propMap.put(KEY_locale,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                return privLabel.getLocaleString();
            }
        });
        propMap.put(KEY_isLocaleRTL,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return reqState.isLocaleRTL()? "true" : "false";
            }
        });
        propMap.put(KEY_localeDirection,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return reqState.isLocaleRTL()? "RTL" : "LTR";
            }
        });

        /* Misc */
        propMap.put(KEY_version,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return Version.getVersion();
            }
        });
        propMap.put(KEY_hostname,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return OSTools.getHostName();
            }
        });
        propMap.put(KEY_ipAddress,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                return reqState.getIPAddress();
            }
        });
        propMap.put(KEY_privateLabelName,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                return privLabel.getName();
            }
        });

        /* Platinum Logo URL */
        propMap.put(KEY_platinumLogoURL,new KeyValue() {
            public String getValue(RequestProperties reqState, String arg) {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                String       acctID    = reqState.getCurrentAccountID();
                String       logoURL   = privLabel.getPlatinumLogoURL(acctID, null);
                if (!StringTools.isBlank(logoURL)) {
                    return logoURL;
                } else {
                    Print.logWarn("Platinum Logo URL is blank: " + privLabel.getName());
                    return "images/logo.png";
                }
            }
        });

        /* init I18N Strings map */
        //propMap.put(KEY_i18n_, RequestProperties._getI18NKeyMap());

        /* return initialized map */
        return RequestProperties.reqPropKeyMap;

    } // _getPropertyKeyMap()

    // --------------------------------

    /**
    *** A RequestProperties property accessor class.
    **/
    public class FTL_PropertyHash // TemplateHashModel
        extends FTLKeyValueHash
    {
        public FTL_PropertyHash(boolean dftBlank) {
            super(dftBlank);
        }
        protected Object _getValue(String key, String arg, Object dft) {
            return RequestProperties.this._getKeyValue(key,arg,dft);
        }
    }

    private FTL_PropertyHash propertyHash_null  = null;
    private FTL_PropertyHash propertyHash_blank = null;

    /**
    *** Returns a FTL_PropertyHash instance that returns property values contained by this instance
    *** @param dftBlank True to return a blank String if the value is undefined/null.
    ***                 False to return null if the value is undefined/blank/null.
    **/
    public FTL_PropertyHash ftl_PropertyHash(boolean dftBlank)
    {
        if (dftBlank) {
            if (this.propertyHash_blank == null) {
                // -- lazy init
                this.propertyHash_blank = new FTL_PropertyHash(true);
            }
            return this.propertyHash_blank;
        } else {
            if (this.propertyHash_null == null) {
                // -- lazy init
                this.propertyHash_null = new FTL_PropertyHash(false);
            }
            return this.propertyHash_null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // -- Track map

    private static final int MIN_BANNER_WIDTH = 860;

    /**
    *** Writes the Track section specified by the key, into the provided PrintWriter.
    *** (used by the track taglib)
    **/
    public void writeTrackSection(HttpServletRequest request, PrintWriter pw, 
        String key, String keyArg, String keyDft)
        throws IOException
    {
        RequestProperties reqState = this;
        PrivateLabel privLabel = (reqState != null)? reqState.getPrivateLabel() : RequestProperties.NullPrivateLabel;

        /* keyArg default */
        // -- for when the arg/param cannot be specified separately (Freemarker)
        { // local scope
            int a = key.indexOf("@");
            if (a >= 0) {
                String arg = key.substring(a+1).trim();
                key = key.substring(0,a);
                if (StringTools.isBlank(keyArg)) {
                    keyArg = arg;
                }
            }
        }

        /* no key? */
        if (StringTools.isBlank(key)) {
            return;
        }

        // --------------------------------------------------------------------

        /* navigation */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_NAVIGATION)) { // see also KEY_navigation
            String nav = reqState.getPageNavigationHTML();
            if (!StringTools.isBlank(nav)) {
                pw.print(nav);
            }
            return;
        }

        // --------------------------------------------------------------------

        /* "onload='...'" */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_BODY_ONLOAD)) {
            String bodyOnLoad = (String)request.getAttribute(CommonServlet.SECTION_BODY_ONLOAD);
            if (!StringTools.isBlank(bodyOnLoad)) {
                pw.print(bodyOnLoad);
            }
            return;
        }

        /* "onunload='...'" */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_BODY_ONUNLOAD)) {
            String bodyOnUnload = (String)request.getAttribute(CommonServlet.SECTION_BODY_ONUNLOAD);
            if (!StringTools.isBlank(bodyOnUnload)) {
                pw.print(bodyOnUnload);
            }
            return;
        }

        // --------------------------------------------------------------------

        /* expandMenu style */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_MENU_STYLE)) {
            ExpandMenu.writeStyle(pw, reqState);
            return;
        }

        /* expandMenu javascript */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_MENU_JAVASCRIPT)) {
            ExpandMenu.writeJavaScript(pw, reqState);
            return;
        }

        /* expandMenu */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_MENU)) {
            ExpandMenu.writeMenu(pw, reqState, 
                null/*menuID*/, true/*expandableMenu*/, 
                false/*showIcon*/, ExpandMenu.DESC_LONG, false/*showMenuHelp*/);
            return;
        }

        // --------------------------------------------------------------------

        /* content table class */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_CONTENT_CLASS_TABLE)) {
            HTMLOutput content = (HTMLOutput)request.getAttribute(CommonServlet.SECTION_CONTENT_BODY);
            if (content != null) {
                String tableClass = content.getTableClass();
                pw.write(!StringTools.isBlank(tableClass)? tableClass : "contentTableClass");
            }
            return;
        }

        /* content cell class */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_CONTENT_CLASS_CELL)) {
            HTMLOutput content = (HTMLOutput)request.getAttribute(CommonServlet.SECTION_CONTENT_BODY);
            if (content != null) {
                String cellClass = content.getCellClass();
                pw.write(!StringTools.isBlank(cellClass)? cellClass : "contentCellClass");
            }
            return;
        }

        /* content message id */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_CONTENT_ID_MESSAGE)) {
            pw.write(CommonServlet.ID_CONTENT_MESSAGE);
            return;
        }

        /* content message class */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_CONTENT_CLASS_MESSAGE)) {
            pw.write(CommonServlet.CSS_CONTENT_MESSAGE);
            return;
        }

        /* content menubar */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_CONTENT_MENUBAR)) {
            HTMLOutput content = (HTMLOutput)request.getAttribute(CommonServlet.SECTION_CONTENT_BODY);
            if (content != null) {
                String contentClass = content.getTableClass();
                if (ListTools.contains(CommonServlet.CSS_MENUBAR_OK,contentClass)) {
                    MenuBar.writeTableRow(pw, reqState.getPageName(), reqState);
                } else {
                    pw.write("<!-- no menubar ['"+contentClass+"'] -->");
                }
            }
            return;
        }

        /* content align */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_CONTENT_ALIGN)) {
            HTMLOutput content = (HTMLOutput)request.getAttribute(CommonServlet.SECTION_CONTENT_BODY);
            String align = (content != null)? StringTools.trim(content.getContentAlign()) : "center";
            pw.write(align);
            return;
        }

        /* content message */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_CONTENT_MESSAGE)) {
            HTMLOutput content = (HTMLOutput)request.getAttribute(CommonServlet.SECTION_CONTENT_BODY);
            String msg = (content != null)? StringTools.trim(content.getTableMessage()) : "";
            pw.write(msg); // TODO: HTML encode?
            return;
        }

        // --------------------------------------------------------------------

        /* request context path */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_REQUEST_CONTEXT)) {
            pw.write(request.getContextPath());
            return;
        }

        // --------------------------------------------------------------------

        /* CSS file */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_CSSFILE)) {
            String cssFilePath = StringTools.trim(keyArg);
            if (!StringTools.isBlank(cssFilePath)) {
                WebPageAdaptor.writeCssLink(pw, reqState, cssFilePath, null);
            }
            return;
        }

        // --------------------------------------------------------------------

        /* Banner Image Height */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_BANNER_WIDTH)) {
            String kSfx = StringTools.trim(keyArg);
            // -- property values
            String bannerWidth = privLabel.getStringProperty(PrivateLabel.PROP_Banner_width   + kSfx, null);
            if (StringTools.isBlank(bannerWidth)) {
                bannerWidth = privLabel.getStringProperty(PrivateLabel.PROP_Banner_imageWidth + kSfx, null);
            }
            // -- minimum value
            if (StringTools.isBlank(bannerWidth)) {
                bannerWidth = !StringTools.isBlank(keyDft)? keyDft : "100%";
            } else
            if (!bannerWidth.endsWith("%")) {
                int W = StringTools.parseInt(bannerWidth, 0);
                bannerWidth = String.valueOf((W < MIN_BANNER_WIDTH)? MIN_BANNER_WIDTH : W);
            }
            // -- generate html
            pw.write(bannerWidth);
            return;
        }

        /* Banner Style */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_BANNER_STYLE)) {
            String kSfx = StringTools.trim(keyArg);
            // -- property values
            String bannerStyle = privLabel.getStringProperty(PrivateLabel.PROP_Banner_style + kSfx, null);
            // -- generate html
            if (!StringTools.isBlank(bannerStyle)) {
                pw.write(bannerStyle);
            } else
            if (!StringTools.isBlank(keyDft)) {
                pw.write(keyDft);
            }
            return;
        }

        /* Banner Image */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_BANNER_IMAGE)) {
            String kSfx = StringTools.trim(keyArg);
            // -- property values
            String imgSrc    = privLabel.getStringProperty(PrivateLabel.PROP_Banner_imageSource + kSfx, null);
            String imgWidth  = privLabel.getStringProperty(PrivateLabel.PROP_Banner_imageWidth  + kSfx, null);
            String imgHeight = privLabel.getStringProperty(PrivateLabel.PROP_Banner_imageHeight + kSfx, null);
            String imgLink   = privLabel.getStringProperty(PrivateLabel.PROP_Banner_imageLink   + kSfx, null);
            // -- pre-defined banner image?
            if ((imgSrc != null) && imgSrc.startsWith("%")) {
                imgSrc = RequestProperties.GetBannerImageURL(imgSrc,null); // may return null
            }
            // -- generate html
            if (!StringTools.isBlank(imgSrc)) {
                // -- <a href='LINK' target='_blank'><img src='IMAGE' border='0' with='WIDTH' height='HEIGHT'/></a>
                StringBuffer sb = new StringBuffer();
                if (!StringTools.isBlank(imgLink)) { 
                    sb.append("<a href='").append(imgLink).append("' target='_blank'>"); 
                }
                sb.append("<img src='").append(imgSrc).append("' border='0'");
                if (!StringTools.isBlank(imgWidth)) {
                    sb.append(" width='").append(imgWidth).append("'");
                }
                if (!StringTools.isBlank(imgHeight)) {
                    sb.append(" height='").append(imgHeight).append("'");
                }
                sb.append("/>");
                if (!StringTools.isBlank(imgLink)) {
                    sb.append("</a>");
                }
                pw.write(sb.toString());
            } else
            if (!StringTools.isBlank(keyDft)) {
                pw.write(keyDft);
            }
            return;
        }

        /* Banner Image */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_BANNER_IMAGE_SOURCE)) {
            String kSfx = StringTools.trim(keyArg);
            // -- property values
            String imgSrc = privLabel.getStringProperty(PrivateLabel.PROP_Banner_imageSource + kSfx, null);
            // -- pre-defined banner image?
            if ((imgSrc != null) && imgSrc.startsWith("%")) {
                imgSrc = RequestProperties.GetBannerImageURL(imgSrc,null); // may return null
            }
            // -- generate html
            if (!StringTools.isBlank(imgSrc)) {
                //Print.sysPrintln("Property Image Source: " + imgSrc);
                pw.write(imgSrc);
            } else
            if (!StringTools.isBlank(keyDft)) {
                //Print.sysPrintln("Default Image Source: " + this.getDefault());
                pw.write(keyDft);
            }
            return;
        }

        /* Banner Image Height */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_BANNER_IMAGE_WIDTH)) {
            String kSfx = StringTools.trim(keyArg);
            // -- property values
            String imgWidth = privLabel.getStringProperty(PrivateLabel.PROP_Banner_imageWidth  + kSfx, null);
            // -- generate html
            if (!StringTools.isBlank(imgWidth)) {
                pw.write(imgWidth);
            } else
            if (!StringTools.isBlank(keyDft)) {
                pw.write(keyDft);
            }
            return;
        }

        /* Banner Image Height */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_BANNER_IMAGE_HEIGHT)) {
            String kSfx = StringTools.trim(keyArg);
            // -- property values
            String imgHeight = privLabel.getStringProperty(PrivateLabel.PROP_Banner_imageHeight + kSfx, null);
            // -- generate html
            if (!StringTools.isBlank(imgHeight)) {
                pw.write(imgHeight);
            } else
            if (!StringTools.isBlank(keyDft)) {
                pw.write(keyDft);
            }
            return;
        }

        // --------------------------------------------------------------------

        /* JavaScript */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_JAVASCRIPT)) {
            // -- always write "utils.js"
            JavaScriptTools.writeUtilsJS(pw, request);
            // -- check for other javascript 
            Object obj = request.getAttribute(CommonServlet.SECTION_JAVASCRIPT);
            if (obj instanceof HTMLOutput) {
                ((HTMLOutput)obj).write(pw); 
            } else {
                pw.write("<!-- Unexpected section type '" + key + "' [" + StringTools.className(obj) + "] -->"); 
            }
            return;
        }

        // --------------------------------------------------------------------

        /* current page name */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_PAGE_NAME)) { // "pagename"
            String pageName = reqState.getPageName();
            if (!StringTools.isBlank(pageName)) {
                pw.write(pageName);
            }
            return;
        }

        // --------------------------------------------------------------------

        /* Page URL */
        if (key.equalsIgnoreCase(CommonServlet.SECTION_PAGE_URL)) {
            String pageName = StringTools.trim(keyArg);
            String cmd = null;
            String cmdArg = null;
            WebPage wp = privLabel.getWebPage(pageName);
            String url = (wp != null)? wp.encodePageURL(reqState,cmd,cmdArg) : null;
            if (!StringTools.isBlank(url)) {
                pw.write(url);
            }
            return;
        }
 
        // --------------------------------------------------------------------

        /* HTMLOutput */
        Object obj = request.getAttribute(key);
        if (obj == null) {
            pw.write("<!-- Undefined section '" + key + "' -->"); 
        } else
        if (obj instanceof HTMLOutput) {
            ((HTMLOutput)obj).write(pw); 
        } else {
            pw.write("<!-- Unexpected section type '" + key + "' [" + StringTools.className(obj) + "] -->"); 
        }
        return;

    } // writeTrackSection

    // --------------------------------

    /**
    *** A Track property accessor class.
    **/
    public class FTL_TrackHash // TemplateHashModel
        extends FTLKeyValueHash
    {
        public FTL_TrackHash() {
            super(true);
        }
        protected Object _getValue(String key, String arg, Object dft) {
            String k = key.replace('_','.');
            String d = (dft != null)? dft.toString() : null;
            HttpServletRequest request = RequestProperties.this.getHttpServletRequest();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos);
            try { // write section to byte array
                RequestProperties.this.writeTrackSection(request, pw, k, arg, d);
            } catch (IOException ioe) {
                Print.logWarn("Track section: " + key + " ["+ioe+"]");
            } finally {
                try {
                    pw.flush();
                    pw.close(); // not necessary
                } catch (Throwable th) {
                    // -- ignore
                }
            }
            return StringTools.toStringValue(baos.toByteArray()); // convert byte array to String
        }
    }

    private FTL_TrackHash trackHash_blank = new FTL_TrackHash();

    /**
    *** Returns a FTL_TrackHash instance that returns property values contained by this instance
    *** @param dftBlank True to return a blank String if the value is undefined/null.
    ***                 False to return null if the value is undefined/blank/null.
    **/
    public FTL_TrackHash ftl_TrackHash()
    {
        return this.trackHash_blank;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the value for the specified key
    **/
    public String getKeyValue(String key, String arg, String dft)
    {
        Object v = this._getKeyValue(key, arg, null);
        if ((v != null) && ListTools.isScaler(v)) {
            return v.toString();
        } else {
            return dft;
        }
    }

    /**
    *** Gets the value for the first matching specified key
    **/
    public String getKeyValue(String key[], String arg, String dft)
    {
        if (!ListTools.isEmpty(key)) {
            for (String k : key) {
                Object v = this._getKeyValue(k, arg, null);
                if ((v != null) && ListTools.isScaler(v)) {
                    return v.toString();
                }
            }
        }
        return dft;
    }

    /**
    *** Gets the value for the specified key
    **/
    private Object _getKeyValue(String key, String keyArg, Object keyDft)
    {
        boolean debug = false; //

        /* keyArg default */
        // -- used when the arg/param cannot be specified separately (Freemarker)
        { // local scope
            int a = key.indexOf("@");
            if (a >= 0) {
                String arg = key.substring(a+1).trim();
                key = key.substring(0,a);
                if (StringTools.isBlank(keyArg)) {
                    keyArg = arg;
                }
            }
        }

        /* no key? */
        if (StringTools.isBlank(key)) {
            if (debug) { Print.logInfo("Key is null/blank (Default): " + key + " ==> " + keyDft); }
            return keyDft;
        }

        /* single-pass loop */
        Object rtn;
        for (;;) {

            /* temporary/thread properties */
            RTProperties threadRTP = RTConfig.getThreadProperties(key);
            if (threadRTP != null) {
                Object v = threadRTP.getProperty(key,null);
                if (v != null) {
                    if (v instanceof KeyValue) {
                        rtn = ((KeyValue)v).getValue(this, keyArg); // String
                    } else
                    if (v instanceof I18N.Text) {
                        rtn = ((I18N.Text)v).toString(this.getLocale());
                    } else
                    if (ListTools.isScaler(v)) {
                        rtn = v; // String/Number/Boolean
                    } else
                    if (ListTools.isContainer(v)) {
                        rtn = v; // Collection/Map/array
                    } else {
                        rtn = v.toString();
                    }
                    if (debug) { Print.logInfo("Found key (Thread): " + key + " ==> " + rtn); }
                    break;
                }
            }

            /* PrivateLabel i18n Strings */
            PrivateLabel privLabel = this.getPrivateLabel();
            String i18nVal = privLabel.getI18NTextString(key, null);
            if (i18nVal != null) {
                rtn = i18nVal;
                if (debug) { Print.logInfo("Found key (I18nText): " + key + " ==> " + rtn); }
                break;
            }

            /* PrivateLabel Strings properties */
            String propVal = privLabel.getStringProperty(key, null);
            if (propVal != null) {
                rtn = propVal;
                if (debug) { Print.logInfo("Found key (PrivateLabel): " + key + " ==> " + rtn); }
                break;
            }

            /* RequestProperties i18n */
            if (key.startsWith(KEY_i18n_)) { // ie. "i18n.Devices"
                String i18nKey = key.substring(KEY_i18n_.length());
                Object v = RequestProperties._getI18NKeyMap().get(i18nKey);
                if (v != null) {
                    if (v instanceof KeyValue) {
                        rtn = ((KeyValue)v).getValue(this, keyArg);
                    } else
                    if (v instanceof I18N.Text) {
                        rtn = ((I18N.Text)v).toString(this.getLocale());
                    } else {
                        rtn = v.toString();
                    }
                    if (debug) { Print.logInfo("Found key (RequestProperties:i18n): " + key + " ==> " + rtn); }
                    break;
                }
            }

            /* RequestProperties properties */
            { // local scope
                Object v = RequestProperties._getPropertyKeyMap().get(key);
                if (v != null) {
                    if (v instanceof KeyValue) {
                        rtn = ((KeyValue)v).getValue(this, keyArg);
                    } else
                    if (v instanceof I18N.Text) {
                        rtn = ((I18N.Text)v).toString(this.getLocale());
                    } else
                    if (ListTools.isScaler(v)) {
                        rtn = v; // String/Number/Boolean
                    } else
                    if (ListTools.isContainer(v)) {
                        rtn = v; // Collection/Map/array
                    } else {
                        rtn = v.toString();
                    }
                    if (debug) { Print.logInfo("Found key (RequestProperties:prop): " + key + " ==> " + rtn); }
                    break;
                }
            }

            /* session attributes */
            if (this.request != null) {
                Object v = AttributeTools.getSessionAttribute(this.request, key, null);
                if (v != null) {
                    if (v instanceof I18N.Text) {
                        rtn = ((I18N.Text)v).toString(this.getLocale());
                    } else
                    if (ListTools.isScaler(v)) {
                        rtn = v; // String/Number/Boolean
                    } else
                    if (ListTools.isContainer(v)) {
                        rtn = v; // Collection/Map/array
                    } else {
                        rtn = v.toString();
                    }
                    if (debug) { Print.logInfo("Found key (Session): " + key + " ==> " + rtn); }
                    break;
                }
            }

            /* runtime properties */
            if (RTConfig.hasProperty(key)) {
                Object v = RTConfig.getProperty(key,null);
                if (v != null) {
                    if (v instanceof I18N.Text) {
                        rtn = ((I18N.Text)v).toString(this.getLocale());
                    } else
                    if (ListTools.isScaler(v)) {
                        rtn = v; // String/Number/Boolean
                    } else
                    if (ListTools.isContainer(v)) {
                        rtn = v; // Collection/Map/array
                    } else {
                        rtn = v.toString();
                    }
                    if (debug) { Print.logInfo("Found key (Runtime): " + key + " ==> " + rtn); }
                    break;
                }
            }

            /* still nothing, return detault */
            rtn = keyDft;
            if (debug) { Print.logWarn("Key not Found (Default): " + key + " ==> " + rtn); }
            break; // exit single-pass loop

        }

        /* translate property key special values */
        if (key.equalsIgnoreCase(BasicPrivateLabel.LAF_Background_Image)) {
            String rtnS = StringTools.trim(rtn);
            String dftS = (keyDft != null)? StringTools.trim(keyDft) : null;
            if (StringTools.isBlank(rtnS)) {
                // -- blank means default value
                rtn = dftS; // may be null
            } else
            if (rtnS.equalsIgnoreCase("none") || 
                rtnS.equalsIgnoreCase("%none")  ) {
                // -- no image requested
                rtn = ""; // blank, not null
            } else
            if (rtnS.equalsIgnoreCase("LoginBackground") ||
                rtnS.equalsIgnoreCase("%LoginBackground")  ) {
                // -- use login background image if present, else default value
                Object url = this._getKeyValue(BasicPrivateLabel.LAF_LoginBackground_Image,null,null);
                rtn = ((url instanceof String) && !StringTools.isBlank(url))? (String)url : dftS;
            } else
            if (rtnS.startsWith("%")) {
                // -- look-up image
                rtn = GetBackgroundImageURL(rtnS, dftS);
            }
        } else
        if (key.equalsIgnoreCase(BasicPrivateLabel.LAF_Background_Overlay)) {
            String rtnS = StringTools.trim(rtn);
            String dftS = (keyDft != null)? StringTools.trim(keyDft) : null;
            if (StringTools.isBlank(rtnS)) {
                // -- blank means default value
                rtn = dftS; // may be null
            } else
            if (rtnS.equalsIgnoreCase("none") || 
                rtnS.equalsIgnoreCase("%none")  ) {
                // -- transparent
                rtn = "transparent";
            } else
            if (rtnS.equalsIgnoreCase("Translucent") || 
                rtnS.equalsIgnoreCase("%Translucent")  ) {
                // -- translucent white
                rtn = "rgba(255,255,255,0.8)";
            }
        } else
        if (key.equalsIgnoreCase(BasicPrivateLabel.LAF_LoginBackground_Image)) {
            String rtnS = StringTools.trim(rtn);
            String dftS = (keyDft != null)? StringTools.trim(keyDft) : null;
            if (StringTools.isBlank(rtnS)) {
                // -- blank means default value
                rtn = dftS; // may be null
            } else
            if (rtnS.startsWith("%")) {
                // -- look-up image
                rtn = GetBackgroundImageURL(rtnS, dftS);
            }
        } else
        if (key.equalsIgnoreCase(BasicPrivateLabel.LAF_LoginBanner300_Image)) {
            String rtnS = StringTools.trim(rtn);
            String dftS = (keyDft != null)? StringTools.trim(keyDft) : null;
            if (StringTools.isBlank(rtnS)) {
                // -- blank means default value
                rtn = dftS; // may be null
            } else
            if (rtnS.startsWith("%")) {
                // -- look-up image
                rtn = GetBannerImageURL(rtnS, dftS);
            }
        } else
        if (key.equalsIgnoreCase(BasicPrivateLabel.LAF_ContentCell_Image)) {
            String rtnS = StringTools.trim(rtn);
            String dftS = (keyDft != null)? StringTools.trim(keyDft) : null;
            if (StringTools.isBlank(rtnS)) {
                // -- blank means default value
                rtn = dftS; // may be null
            } else
            if (rtnS.startsWith("%")) {
                // -- look-up image ("%GraniteGray", etc)
                rtn = GetTextureImageURL(rtnS, dftS);
            }
        }

        /* return result */
        return rtn;

    }

    /**
    *** Print all key/values
    **/
    public void printKeyValues(String m)
    {

        /* PrivateLabel (also prints thread properties) */
        PrivateLabel privLabel = this.getPrivateLabel();
        privLabel.printProperties(m);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the number of active login session matching the current AccountID/UserID
    *** @return The current number of login sessions for the current Account/User
    **/
    public int GetLoginCount()
    {
        HttpSession hs = AttributeTools.getSession(this.getHttpServletRequest());
        if (hs != null) {
            String aid = this.getCurrentAccountID();
            String uid = this.getCurrentUserID();
            if (StringTools.isBlank(uid)) {
                uid = User.getAdminUserID();
            }
            return RequestProperties.GetLoginCount(hs.getServletContext(), aid, uid);
        } else {
            // IllegalStateException?
            return -1;
        }
    }

    /**
    *** Returns the current number of login session matching the specified AccountID/UserID
    *** @param sc        The ServletContext
    *** @param accountID The AccountID
    *** @param userID    The UserID, or null for all users
    *** @return The current number of login sessions for the specified Account/User
    **/
    public static int GetLoginCount(ServletContext sc, String accountID, String userID)
    {
        if (StringTools.isBlank(accountID)) {
            return RTConfigContextListener.GetSessionCount(sc);
        } else {
            final String aid = StringTools.trim(accountID);
            final String uid = !StringTools.isBlank(userID)? StringTools.trim(userID) : null;
            return RTConfigContextListener.GetSessionCount(sc,
                new RTConfigContextListener.HttpSessionFilter() {
                    public boolean countSession(HttpSession s) {
                        Object sa = AttributeTools.getSessionAttribute(s,Constants.PARM_ACCOUNT,"");
                        if (aid.equals(sa)) {
                            if (uid == null) {
                                return true;
                            } else {
                                Object su = AttributeTools.getSessionAttribute(s,Constants.PARM_USER,"");
                                return uid.equals(su);
                            }
                        } else {
                            return false;
                        }
                    }
                }
            );
        }
    }

    // ------------------------------------------------------------------------

}
