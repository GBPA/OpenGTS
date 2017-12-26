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
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/12/13  Martin D. Flynn
//     -Changed form target to '_self' for "ContentOnly" display
//  2008/12/01  Martin D. Flynn
//     -Increased maxsize for account/user/password fields to match length specified
//      in their respective tables.
//  2009/01/01  Martin D. Flynn
//     -Added popup 'alert' for login errors
//  2009/04/11  Martin D. Flynn
//     -Added focus on login field
//     -Added support for hiding the "Password" field on the login page
//  2013/08/27  Martin D. Flynn
//     -Added support for returning to SysAdmin/Manager originator (if applicable)
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class AccountLogin
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Tomcat conf/server.xml
    //   emptySessionPath="true"
    //   <SessionId cookiesFirst="true" noCookies="true"/>
    // HttpServletResponse.encodeURL()
    // ------------------------------------------------------------------------

    private static       String  FORM_LOGIN                 = "Login";
    private static       String  FORM_DEMO                  = "Demo";

    // ------------------------------------------------------------------------

    private static final String CSS_ACCOUNT_LOGIN[]         = new String[] { "accountLoginTable", "accountLoginCell" };

    private static final String CSS_LOGIN_CONTENT_TABLE     = "accountLoginContentTable";
    public  static final String CSS_LOGIN_VSEP_CELL         = "accountLoginVertSepCell";
    public  static final String CSS_LOGIN_TEXT_CELL         = "accountLoginTextCell";
    public  static final String CSS_LOGIN_FORM_TABLE        = "accountLoginFormTable";

    // ------------------------------------------------------------------------
    // Properties

    public  static final String PROP_customLoginUrl         = "customLoginUrl";
    public  static final String PROP_VSeparatorImage        = "VSeparatorImage.path";
    public  static final String PROP_VSeparatorImage_W      = "VSeparatorImage.width";
    public  static final String PROP_VSeparatorImage_H      = "VSeparatorImage.height";

    public  static final String PROP_PlaceholderLabelOnly   = "PlaceholderLabelOnly";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // WebPage interface

    public AccountLogin()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        super.setPageName(PAGE_LOGIN); // 'super' required here
        this.setPageNavigation(new String[] { PAGE_LOGIN });
        this.setLoginRequired(false);
    }

    // ------------------------------------------------------------------------

    public void setPageName(String pageName)
    {
        // -- ignore (changing the PAGE_LOGIN name is not allowed)
    }

    // ------------------------------------------------------------------------

    public String getCustomLoginURL()
    {
        return this.getProperties().getString(PROP_customLoginUrl,null);
    }

    public boolean hasCustomLoginURL()
    {
        return !StringTools.isBlank(this.getCustomLoginURL());
    }

    public URIArg getPageURI(String command, String cmdArg)
    {
        String loginURL = this.getCustomLoginURL();
        if (!StringTools.isBlank(loginURL)) {
            //Print.logInfo("Login custom URL: " + loginURL);
            //Print.logStackTrace("here");
            return new URIArg(loginURL);
        } else {
            return super.getPageURI(command, cmdArg);
        }
    }

    public String getJspURI()
    {
        return super.getJspURI();
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountLogin.class);
        return super._getMenuDescription(reqState,i18n.getString("AccountLogin.menuDesc","Logout"));
    }

    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountLogin.class);
        return super._getMenuHelp(reqState,i18n.getString("AccountLogin.menuHelp","Logout"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountLogin.class);
        if (reqState.isLoggedIn()) {
            if (reqState.isReturnToSysAdminEnabled()) {
                HttpServletRequest req = reqState.getHttpServletRequest();
                String saAcctID = AttributeTools.getSessionString(req,Constants.PARM_SA_RELOGIN_ACCT,"?");
                return i18n.getString("AccountLogin.navDescReturn","Return to {0}", saAcctID);
            } else {
                return i18n.getString("AccountLogin.navDesc","Logout");
            }
        } else
        if (privLabel.getBooleanProperty(PrivateLabel.PROP_AccountLogin_showLoginLink,true)) {
            return i18n.getString("AccountLogin.navDesc.login","Login");
        } else {
            return super._getNavigationDescription(reqState,"");
        }
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountLogin.class);
        return i18n.getString("AccountLogin.navTab","Logout");
    }

    // ------------------------------------------------------------------------

    public boolean isOkToDisplay(RequestProperties reqState)
    {
        PrivateLabel privLabel = (reqState != null)? reqState.getPrivateLabel() : null;
        if (privLabel == null) {
            // no PrivateLabel?
            return false;
        } else
        if (!privLabel.getShowPassword()) {
            // Password is hidden, don't show the "Change Password" page
            return false;
        } else {
            // show "Change Password"
            return true;
        }
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(AccountLogin.class);
        final String HR = "<hr style='height:3px;margin-right:5px;'/>";

        /* custom login banner image (roughly 300px wide) */
        final String  banner300Image;
        final boolean showLogin300Banner;
        final boolean showLogin300BannerTitle;
        if (StringTools.parseBoolean(reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_banner300_show,null,null),false)) {
            // -- show login banner300 image
            String bannerImg = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_banner300_image,null,null);
            if (StringTools.isBlank(bannerImg)) {
                bannerImg = reqState.getKeyValue(PrivateLabel.PROP_Banner_imageSource,null,null);
            }
            // --
            if (!StringTools.isBlank(bannerImg)) {
                // -- found a banner image
                banner300Image       = bannerImg;
                showLogin300Banner   = true;
            } else {
                // -- no banner image found
                banner300Image       = null;
                showLogin300Banner   = false;
            }
        } else {
            // -- do not show banner300 image?
            String bannerImg = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_banner300_image,null,null);
            if (!StringTools.isBlank(bannerImg)) {
                // -- found a banner image, show anyway
                banner300Image       = bannerImg;
                showLogin300Banner   = true;
            } else {
                // -- no banner image found
                banner300Image       = null;
                showLogin300Banner   = false;
            }
        }
        // --
        if (!showLogin300Banner) {
            // -- no banner300 image, no title
            showLogin300BannerTitle = false;
        } else
        if (StringTools.parseBoolean(reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_banner300_includeTitle,null,null),false)) {
            // -- expressly want the banner300 title
            showLogin300BannerTitle = true;
        } else {
            // -- do not want the banner300 title
            showLogin300BannerTitle = false;
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = AccountLogin.this.getCssDirectory();
                //WebPageAdaptor.writeCssLink(out, reqState, "AccountLogin.css", cssDir);
                // -- Login page background image? (ie "./extra/images/texture/Clouds.png")
                String bgImage    = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_background_image     , null, null        );
                String bgSize     = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_background_size      , null, "cover"     );
                String bgPosition = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_background_position  , null, "center top");
                String bgRepeat   = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_background_repeat    , null, "no-repeat" );
                String bgAttach   = reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_background_attachment, null, "fixed"     );
                out.write("  <!-- AcountLogin custom CSS -->\n");
                out.write("  <style type='text/css'>\n");
                if (!StringTools.isBlank(bgImage)) {
                    // -- override body background image with custom login page image
                    out.write("    BODY {\n");
                    out.write("        background-image: url('"+bgImage   +"');\n");
                    out.write("        background-size: "      +bgSize    +";\n");
                    out.write("        background-position: "  +bgPosition+";\n");
                    out.write("        background-repeat: "    +bgRepeat  +";\n");
                    out.write("        background-attachment: "+bgAttach  +";\n");
                    out.write("    }\n");
                    out.write("    TABLE.bodyTable {\n");
                    out.write("        background-color: rgba(255,255,255,0.0);\n"); // transparent
                    out.write("    }\n");
                }
                if (showLogin300Banner) {
                    out.write("    .accountLoginTable {\n");
                    out.write("        margin-top: 0px;\n");
                    out.write("    }\n");
                    out.write("    TABLE.accountLoginContentTable {\n");
                    out.write("        background-color: rgba(255,255,255,0.6);\n");
                    out.write("    }\n");
                }
                out.write("  </style>\n");
            }
        };

        /* write frame */
        String cssAccountLogin[] = CSS_ACCOUNT_LOGIN;
        HTMLOutput HTML_CONTENT = new HTMLOutput(cssAccountLogin, pageMsg) {
            public String getContentAlign() {
                // -- also "LoginBackground.image"
              //String ca = StringTools.trim(privLabel.getStringProperty(PrivateLabel.PROP_AccountLogin_contentAlign,""));
                String ca = StringTools.trim(reqState.getKeyValue(PrivateLabel.PROP_AccountLogin_contentAlign,null,null));
                if (ca.equalsIgnoreCase("left")   ||
                    ca.equalsIgnoreCase("center") ||
                    ca.equalsIgnoreCase("right")    ) {
                    return ca;
                } else {
                    return showLogin300Banner? "right" : "center";
                }
            }
            public void write(PrintWriter out) throws IOException {
                PrivateLabel privLabel = reqState.getPrivateLabel();
                RTProperties acctRTP   = AccountLogin.this.getProperties();
                // -- baseURL
                URIArg  baseURI    = MakeURL(RequestProperties.TRACK_BASE_URI(),null,null,null);
                HttpServletRequest req = reqState.getHttpServletRequest();
                String rtpArg      = (req != null)? req.getParameter(AttributeTools.ATTR_RTP) : null;
                if (!StringTools.isBlank(rtpArg)) { baseURI.addArg(AttributeTools.ATTR_RTP,rtpArg); }
                String  baseURL    = EncodeURL(reqState, baseURI);
                String  accountID  = StringTools.trim(AccountRecord.getFilteredID(AttributeTools.getRequestString(req,Constants.PARM_ACCOUNT,"")));
                String  userID     = StringTools.trim(AccountRecord.getFilteredID(AttributeTools.getRequestString(req,Constants.PARM_USER   ,"")));
                // -- other args
                String  newURL     = privLabel.hasWebPage(PAGE_ACCOUNT_NEW )?
                    //EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_ACCOUNT_NEW ) : null;
                    privLabel.getWebPageURL(reqState,PAGE_ACCOUNT_NEW) : null;
                String  forgotURL  = privLabel.hasWebPage(PAGE_PASSWD_EMAIL)?
                    //EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_PASSWD_EMAIL) : null;
                    privLabel.getWebPageURL(reqState,PAGE_PASSWD_EMAIL) : null;
                boolean acctLogin  = privLabel.getAccountLogin();
                boolean userLogin  = privLabel.getUserLogin();
                boolean emailLogin = privLabel.getAllowEmailLogin();
                boolean showPasswd = reqState.getShowPassword();
                boolean showLocale = reqState.showLocaleSelection(); // <-- also examines override properties
                boolean showDemo   = reqState.getEnableDemo();
                String  target     = "_self"; // reqState.getPageFrameContentOnly()? "_self" : "_top";  // target='_top'
                boolean loginOK    = privLabel.getBooleanProperty(BasicPrivateLabelLoader.ATTR_allowLogin, true);
                String  ro         = loginOK? "" : "readonly";
                // ----------------------------------
                // Basic login input form:
                //  <form name="login" method="post" action="http://track.example.com:8080/track/Track" target="_top">
                //      Account:  <input name="account"  type="text"     size='20' maxlength='32' placeholder='Account' > <br>
                //      User:     <input name="user"     type="text"     size='20' maxlength='32' placeholder='User'    > <br>
                //      Password: <input name="password" type="password" size='20' maxlength='32' placeholder='Password'> <br>
                //      <input type="submit" name="submit" value="Login">
                //  </form>

                // -- start LoginTable
                out.print("<table class='"+CSS_LOGIN_CONTENT_TABLE+"' cellpadding='0' cellspacing='0' border='0'");
                    out.print(" width='100%'");
                    out.print(">\n");

                // -- top image row
                if (showLogin300Banner) {
                    // -- JSP "nobanner" is expected
                    out.println("<tr class='banner300ImageRow'>");
                    out.println("  <td class='banner300ImageCell' colSpan='2'>");
                    if (showLogin300BannerTitle) {
                    out.println("    <span class='banner300ImageTitle'>"+privLabel.getPageTitle()+"</span><br>");
                    }
                    out.println("    <img src='"+banner300Image+"'/>"); // width='300' />");
                    out.println("  </td>");
                    out.println("</tr>");
                }

                // -- start LoginTable/AccountUserPasswordContentRow
                out.println("<tr class='accountUserPasswordContentRow'>");

                // -- vertical separator (optional)
                String vSepImg = acctRTP.getString(PROP_VSeparatorImage,"./images/VSep_DBlue.png");
                if (!StringTools.isBlank(vSepImg)) {
                    String W = acctRTP.getString(PROP_VSeparatorImage_W,"8");
                    String H = acctRTP.getString(PROP_VSeparatorImage_H,"300");
                    out.print("<td class='"+CSS_LOGIN_VSEP_CELL+"'>");
                    out.print("<img ");
                    if (!StringTools.isBlank(W)) { out.print(" width='"+W+"'"); }
                    if (!StringTools.isBlank(H)) { out.print(" height='"+H+"'"); }
                    out.print(" src='"+vSepImg+"'/>");
                    out.print("</td>");
                }

                // -- start AccountUserPasswordCell
                out.println("<td class='"+CSS_LOGIN_TEXT_CELL+"'>");

                // -- "Please Log In"
                String enterLoginText_ = showPasswd? // not used
                    i18n.getString("AccountLogin.enterLogin","Enter your Login ID and Password") :
                    i18n.getString("AccountLogin.enterLoginNoPass","Enter Login ID (No Password Required)");
                String enterLoginText = i18n.getString("AccountLogin.pleaseLogIn","Please Log In");
                out.println("<h1>"+enterLoginText+"</h1>");

                // -- Account/User/Password form/table
                out.println("<form name='"+FORM_LOGIN+"' method='post' class='form-horizontal' action='"+baseURL+"' target='"+target+"'>");
                out.println("  <table class='"+CSS_LOGIN_FORM_TABLE+" table' cellpadding='0' cellspacing='0' border='0'>");
                String focusFieldID = "";

                // -- Account login field
                // -  "placeholderOnly" specifies that the field label should only use the "placeholder" attribute
                boolean placeholderOnly = acctRTP.getBoolean(PROP_PlaceholderLabelOnly,false);
                if (acctLogin) {
                    String fldID = "accountLoginField";
                    String text_ = i18n.getString("AccountLogin.account","Account:");
                    String text  = text_.endsWith(":")?text_.substring(0,text_.length()-1):text_;
                    out.print("  <tr>");
                    if (!placeholderOnly) {
                    out.print(      "<td class='accountLoginFieldLabel' style='font-weight: 500;'>"+text_+"</td>");
                    }
                    out.print(      "<td class='accountLoginFieldValue'>");
                    out.print(      "<input id='"+fldID+"' class='"+CommonServlet.CSS_TEXT_INPUT+" form-control' type='text' "+ro+" name='"+Constants.PARM_ACCOUNT+"' value='"+accountID+"' placeholder='"+text+"' size='32' maxlength='32'>");
                    out.print(      "</td>");
                    out.print(  "</tr>\n");
                    focusFieldID = fldID;
                }

                // -- User/EMail login field
                if (userLogin && emailLogin) {
                    String fldID = "userLoginField";
                    String text_ = i18n.getString("AccountLogin.userEmail","User/EMail:");
                    String text  = text_.endsWith(":")?text_.substring(0,text_.length()-1):text_;
                    out.print("  <tr>");
                    if (!placeholderOnly) {
                    out.print(      "<td class='accountLoginFieldLabel' style='font-weight: 500;'>"+text_+"</td>");
                    }
                    out.print(      "<td class='accountLoginFieldValue'>");
                    out.print(      "<input id='"+fldID+"' class='"+CommonServlet.CSS_TEXT_INPUT+" form-control' type='text' "+ro+" name='"+Constants.PARM_USER+"' value='"+userID+"' placeholder='"+text+"' size='32' maxlength='40'>");
                    out.print(      "</td>");
                    out.print(  "</tr>\n");
                    if (StringTools.isBlank(focusFieldID)) { focusFieldID = fldID; }
                } else
                if (userLogin) {
                    String fldID = "userLoginField";
                    String text_ = i18n.getString("AccountLogin.user","User:");
                    String text  = text_.endsWith(":")?text_.substring(0,text_.length()-1):text_;
                    out.print("  <tr>");
                    if (!placeholderOnly) {
                    out.print(      "<td class='accountLoginFieldLabel' style='font-weight: 500;'>"+text_+"</td>");
                    }
                    out.print(      "<td class='accountLoginFieldValue'>");
                    out.print(      "<input id='"+fldID+"' class='"+CommonServlet.CSS_TEXT_INPUT+" form-control' type='text' "+ro+" name='"+Constants.PARM_USER+"' value='"+userID+"' placeholder='"+text+"' size='32' maxlength='32'>");
                    out.print(      "</td>");
                    out.print(  "</tr>\n");
                    if (StringTools.isBlank(focusFieldID)) { focusFieldID = fldID; }
                } else
                if (emailLogin) {
                    String fldID = "emailLoginField";
                    String text_ = i18n.getString("AccountLogin.email","EMail:");
                    String text  = text_.endsWith(":")?text_.substring(0,text_.length()-1):text_;
                    out.print("  <tr>");
                    if (!placeholderOnly) {
                    out.print(      "<td class='accountLoginFieldLabel' style='font-weight: 500;'>"+text_+"</td>");
                    }
                    out.print(      "<td class='accountLoginFieldValue'>");
                    out.print(      "<input id='"+fldID+"' class='"+CommonServlet.CSS_TEXT_INPUT+" form-control' type='text' "+ro+" name='"+Constants.PARM_USEREMAIL+"' value='"+userID+"' placeholder='"+text+"' size='32' maxlength='40'>");
                    out.print(      "</td>");
                    out.print(  "</tr>\n");
                    if (StringTools.isBlank(focusFieldID)) { focusFieldID = fldID; }
                }

                // -- Password field
                if (showPasswd) {
                    String text_ = i18n.getString("AccountLogin.password","Password:");
                    String text  = text_.endsWith(":")?text_.substring(0,text_.length()-1):text_;
                    out.print("  <tr>");
                    if (!placeholderOnly) {
                    out.print(      "<td class='accountLoginFieldLabel' style='font-weight: 500;'>"+text_+"</td>");
                    }
                    out.print(      "<td class='accountLoginFieldValue'>");
                    out.print(      "<input class='"+CommonServlet.CSS_TEXT_INPUT+" form-control' type='password' "+ro+" name='"+Constants.PARM_PASSWORD+"' value='' placeholder='"+text+"' size='32' maxlength='32'>");
                    out.print(      "</td>");
                    out.print(  "</tr>\n");
                }

                // -- Language selection
                if (showLocale) {
                    String dftLocale = privLabel.getLocaleString();
                    String text_ = i18n.getString("AccountLogin.language","Language:");
                    String text  = text_.endsWith(":")?text_.substring(0,text_.length()-1):text_;
                    Map<String,String> localeMap = BasicPrivateLabel.GetSupportedLocaleMap(privLabel.getLocale());
                    ComboMap comboLocaleMap = new ComboMap(localeMap);
                    out.print("  <tr>");
                    if (!placeholderOnly) {
                    out.print(    "<td class='accountLoginFieldLabel' style='font-weight: 500;'>"+text_+"</td>");
                    }
                    out.print(    "<td class='accountLoginFieldValue'>");
                    out.write(      Form_ComboBox(CommonServlet.PARM_LOCALE, CommonServlet.PARM_LOCALE, true, comboLocaleMap, dftLocale, null/*onchange*/));
                    out.print(    "</td>");
                    out.print(  "</tr>\n");
                }

                // -- end Account/User/Password table
                out.print("</table>\n");

                // -- Login
                out.print("<br>");
                out.print("<input type='submit' class='btn btn-success' name='submit' value='"+i18n.getString("AccountLogin.login","Login")+"' >\n");


                // -- end Account/User/Password forn
                out.println("</form>");


                // -- Demo button
                if (showDemo) {
                    out.println(HR);
                    //out.println("<br/>");
                    out.println("<form class='form-horizontal' name='"+FORM_DEMO+"' method='post' action='"+baseURL+"' target='"+target+"'>");
                    out.println("  <input type='hidden' name='"+Constants.PARM_ACCOUNT  +"' value='"+reqState.getDemoAccountID()+"'/>");
                    out.println("  <input type='hidden' name='"+Constants.PARM_USER     +"' value=''/>");
                    out.println("  <input type='hidden' name='"+Constants.PARM_PASSWORD +"' value=''/>");
                    out.println("  <span style='font-size:9pt;padding-right:5px;'>"+i18n.getString("AccountLogin.freeDemo","Click here for a Demo")+"</span>");
                    out.println("  <input type='submit' name='submit' value='"+i18n.getString("AccountLogin.demo","Demo")+"'>");
                    out.println("</form>");
                    //out.println("<br/>");
                }

                // -- New Account
                if (newURL != null) {

                }

                // -- end AccountUserPasswordCell
                out.println("</td>");

                // -- end LoginTable/AccountUserPasswordContentRow
                out.println("</tr>");

                // -- end LoginTable
                out.println("</table>");

                /* set focus */
                if (!StringTools.isBlank(focusFieldID)) {
                    out.write("<script type=\"text/javascript\">\n");
                    out.write("var loginFocusField = document.getElementById('"+focusFieldID+"');\n");
                    out.write("if (loginFocusField) {\n");
                    out.write("    loginFocusField.focus();\n");
                    out.write("    loginFocusField.select();\n");
                    out.write("}\n");
                    out.write("</script>\n");
                }


            }
        };

        /* write frame */
        String onload = (!StringTools.isBlank(pageMsg) && reqState._isLoginErrorAlert())? JS_alert(true,pageMsg) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTMLOutput.NOOP,            // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------

}
