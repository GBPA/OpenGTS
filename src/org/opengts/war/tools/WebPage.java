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
//  2007/03/30  Martin D. Flynn
//     -Added access control
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.PixelDimension;
import org.opengts.util.URIArg;
import org.opengts.util.RTProperties;

public interface WebPage
{

    // ------------------------------------------------------------------------
    // Sortable table constants (used by 'sorttable.js')

    public static final String SORTTABLE_SORTKEY            = "sorttable_customkey";
    public static final String SORTTABLE_CSS_CLASS          = "sortable";
    public static final String SORTTABLE_CSS_NOSORT         = "nosort"; // MDF modified, was "sorttable_nosort";

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this page is enabled within the current environment
    **/
    public boolean getIsEnabled();

    // ------------------------------------------------------------------------

    /**
    *** Gets the base URL
    **/
    public String getBaseURI();

    /**
    *** Gets the page URL
    **/
    public URIArg getPageURI();

    // ------------------------------------------------------------------------

    /**
    *** Gets the page properties
    **/
    public RTProperties getProperties();

    // ------------------------------------------------------------------------

    /**
    *** Gets the page name 
    **/
    public String getPageName();

    /**
    *** Sets/Overrides the default page name <br>
    *** (the interface implementation may reject this override)
    **/
    public void setPageName(String pageName);

    // ------------------------------------------------------------------------

    /**
    *** Gets the JSP path which should be used to display this page 
    **/
    public String getJspURI();

    // ------------------------------------------------------------------------

    /**
    *** Gets the page 'target='
    **/
    public String getTarget();

    /**
    *** Gets the window pixel dimension (may return null) 
    **/
    public PixelDimension getWindowDimension();

    // ------------------------------------------------------------------------

    /**
    *** Gets the desired page navigation 
    **/
    public String getPageNavigationHTML(RequestProperties reqState);
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if a valid log is required for the display of this page
    **/
    public boolean isLoginRequired();

    // ------------------------------------------------------------------------

    /**
    *** Return true if the page indicates that it is ok to display (pending other restrictions) 
    **/
    public boolean isOkToDisplay(RequestProperties reqState);

    // ------------------------------------------------------------------------

    /**
    *** Gets the ACL name for this page 
    **/
    public String getAclName();

    /**
    *** Gets the ACL name for this page, with specified sub-ACL appended
    **/
    public String getAclName(String subAcl);

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this page is for the system admin only 
    **/
    public boolean systemAdminOnly();

    // ------------------------------------------------------------------------

    /**
    *** Gets the menu group (if any) 
    **/
    public MenuGroup getMenuGroup(RequestProperties reqState);

    /**
    *** Gets the menu name for menu navigation 
    **/
    public String getMenuName(RequestProperties reqState);

    /**
    *** Gets the menu icon image URI 
    **/
    public String getMenuIconImage();

    /**
    *** Gets the menu button image URI 
    **/
    public String getMenuButtonImage();

    /**
    *** Gets the menu button alternate image URI 
    **/
    public String getMenuButtonAltImage();

    /**
    *** Gets the menu description for the specified menu
    **/
    public String getMenuDescription(RequestProperties reqState, String parentMenuName);

    /**
    *** Gets the menu help for the specified menu 
    **/
    public String getMenuHelp(RequestProperties reqState, String parentMenuName);
 
    // ------------------------------------------------------------------------

    /**
    *** Gets the 'logged-in' navigation description 
    **/
    public String getNavigationDescription(RequestProperties reqState);

    /**
    *** Gets the 'logged-in' navigation description 
    **/
    public String getNavigationTab(RequestProperties reqState);

    // ------------------------------------------------------------------------

    /**
    *** Encode the URL to the WebPage 
    **/
    public String encodePageURL(RequestProperties reqState);

    /**
    *** Encode the URL to the WebPage 
    **/
    public String encodePageURL(RequestProperties reqState, String command);

    /**
    *** Encode the URL to the WebPage 
    **/
    public String encodePageURL(RequestProperties reqState, String command, String cmdArg);

    // ------------------------------------------------------------------------

    /**
    *** Writes the contents for this page to the 'response' output <br>
    *** connection state is available in 'reqState'
    **/
    public void writePage(RequestProperties reqState, String pageMsg)
        throws IOException;
    
    // ------------------------------------------------------------------------

}
