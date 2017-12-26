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
//  2007/03/30  Martin D. Flynn
//     -Initial release
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/06/30  Martin D. Flynn
//     -Added User table view
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2008/08/15  Martin D. Flynn
//     -The 'admin' user [see "User.getAdminUserID()"] is always granted "ALL" access.
//  2008/08/17  Martin D. Flynn
//     -Fix display of View/Edit buttons on creation of first user.
//  2008/09/01  Martin D. Flynn
//     -Added editable field for User first login page.
//     -Added delete confirmation
//  2008/10/16  Martin D. Flynn
//     -Update with new ACL usage
//     -Added support to user Roles.
//  2008/12/01  Martin D. Flynn
//     -Disable password field for current user
//  2009/08/23  Martin D. Flynn
//     -Convert new entered IDs to lowercase
//  2010/11/29  Martin D. Flynn
//     -Changes to "EMail" login to support "sysadmin" login if sysadmin does not
//      have a set contact email address.
//     -Allow creating "admin" user, if logged-in as the default "admin" user.
//  2011/03/08  Martin D. Flynn
//     -Added support for notification email.
//  2012/04/03  Martin D. Flynn
//     -Added option to hide ACL list override (see PROP_UserInfo_showAccessControlList)
//  2013/03/01  Martin D. Flynn
//     -Populate Roles pull-down based on _ACL_ROLE (was _ACL_ACLS)
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class UserInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    public  static final String _ACL_ALL                = "all";
    public  static final String _ACL_ACLS               = "acls";
    public  static final String _ACL_GROUPS             = "groups";
    public  static final String _ACL_ROLE               = "role";
    public  static final String _ACL_DEVICEID           = "deviceID";
    public  static final String _ACL_PASSWORD           = "password";
    private static final String _ACL_LIST[]             = new String[] { _ACL_ALL, _ACL_ACLS, _ACL_GROUPS, _ACL_ROLE, _ACL_DEVICEID };

    private static final String ACL_DEFAULT             = "default";

    // ------------------------------------------------------------------------

    // password holder/indicator
    private static final String PASSWORD_HOLDER         = "**********";

    // ------------------------------------------------------------------------
    // Parameters

    // forms
    public  static final String FORM_USER_SELECT        = "UserInfoSelect";
    public  static final String FORM_USER_EDIT          = "UserInfoEdit";
    public  static final String FORM_USER_NEW           = "UserInfoNew";

    // commands
    public  static final String COMMAND_INFO_UPDATE     = "update";
    public  static final String COMMAND_INFO_SELECT     = "select";
    public  static final String COMMAND_INFO_NEW        = "new";

    // submit
    public  static final String PARM_SUBMIT_EDIT        = "u_subedit";
    public  static final String PARM_SUBMIT_VIEW        = "u_subview";
    public  static final String PARM_SUBMIT_CHG         = "u_subchg";
    public  static final String PARM_SUBMIT_DEL         = "u_subdel";
    public  static final String PARM_SUBMIT_NEW         = "u_subnew";

    // buttons
    public  static final String PARM_BUTTON_CANCEL      = "u_btncan";
    public  static final String PARM_BUTTON_BACK        = "u_btnbak";

    // parameters
    public  static final String PARM_NEW_NAME           = "u_newname";
    public  static final String PARM_USER_SELECT        = "u_user";
    public  static final String PARM_USER_NAME          = "u_name";
    public  static final String PARM_USER_PASSWORD      = "u_pass";
    public  static final String PARM_ACCESS_LEVEL       = "u_accLvl";
    public  static final String PARM_USER_ROLE          = "u_role";
    public  static final String PARM_CONTACT_NAME       = "u_contact";
    public  static final String PARM_CONTACT_PHONE      = "u_phone";
    public  static final String PARM_CONTACT_EMAIL      = "u_email";
    public  static final String PARM_NOTIFY_EMAIL       = "u_notify";
    public  static final String PARM_TIMEZONE           = "u_tmz";
    public  static final String PARM_SPEED_UNITS        = "u_spdun";
    public  static final String PARM_DIST_UNITS         = "u_dstun";
    public  static final String PARM_ACL_               = "u_acl_";
    public  static final String PARM_USER_ACTIVE        = "u_actv";
    public  static final String PARM_LOGIN_PAGE         = "u_1stpage";
    public  static final String PARM_USER_EXPIRE        = "u_xpire";
    public  static final String PARM_PREF_DEVICE        = "u_devid";

    /* address */
    public  static final String PARM_ADDRESS_LINE_1     = "u_adrl1";
  //public  static final String PARM_ADDRESS_LINE_2     = "u_adrl2";
    public  static final String PARM_ADDRESS_CITY       = "u_adrcity";
    public  static final String PARM_ADDRESS_STATE      = "u_adrst";
    public  static final String PARM_OFFICE_LOC         = "u_ofcloc";

    /* notes/custom */
    public  static final String PARM_USER_NOTES         = "u_notes";
    public  static final String PARM_USER_CUSTOM_       = "u_c_";

    /* auth device groups */
    public  static final String PARM_DEV_GROUP_         = "u_dg_";

    // ------------------------------------------------------------------------

    public enum FirstLogin implements EnumTools.StringLocale, EnumTools.StringValue {
        TOP_MENU        (Constants.PAGE_MENU_TOP           , I18N.getString(UserInfo.class,"UserInfo.firstLogin.topMenu"      ,"Main Menu"      )), // default
        MAP_DEVICE      (Constants.PAGE_MAP_DEVICE         , I18N.getString(UserInfo.class,"UserInfo.firstLogin.mapDevice"    ,"Device Map"     )),
        MAP_FLEET       (Constants.PAGE_MAP_FLEET          , I18N.getString(UserInfo.class,"UserInfo.firstLogin.mapFleet"     ,"Fleet Map"      )),
        REPORT_DETAIL   (Constants.PAGE_MENU_RPT_DEVDETAIL , I18N.getString(UserInfo.class,"UserInfo.firstLogin.reportDetail" ,"Detail Reports" )),
        REPORT_SUMMARY  (Constants.PAGE_MENU_RPT_GRPSUMMRY , I18N.getString(UserInfo.class,"UserInfo.firstLogin.reportSummary","Fleet Reports"  ));
        // ---
        private String      pn = "";
        private I18N.Text   aa = null;
        FirstLogin(String p, I18N.Text a)           { pn = p; aa = a; }
        public String  getStringValue()             { return pn; }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isDefault()                  { return this.equals(TOP_MENU); }
    };

    public static FirstLogin GetFirstLogin(String pageName)
    {
        return EnumTools.getValueOf(FirstLogin.class, pageName);
    }

    public static FirstLogin[] GetFirstLoginList()
    {
        return FirstLogin.class.getEnumConstants();
    }

    private static ComboMap GetFirstLoginListMap(Locale locale)
    {
        ComboMap map = new ComboMap();
        for (FirstLogin f : GetFirstLoginList()) {
            map.add(f.getStringValue(), f.toString(locale));
        }
        return map;
    }

    // ------------------------------------------------------------------------
    // WebPage interface

    public UserInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_USER_INFO);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
        //this.setCssDirectory("css");
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(UserInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("UserInfo.editMenuDesc","View/Edit User Information"));
    }

    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(UserInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("UserInfo.editMenuHelp","View and Edit User information"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(UserInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("UserInfo.navDesc","User"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(UserInfo.class);
        return super._getNavigationTab(reqState,i18n.getString("UserInfo.navTab","User Admin"));
    }

    // ------------------------------------------------------------------------

    public String[] getChildAclList()
    {
        return _ACL_LIST;
    }

    // ------------------------------------------------------------------------

    private boolean isValidPassword(BasicPrivateLabel bpl, User user, String pwd, I18N.Text msg)
    {
        if (StringTools.isBlank(pwd)) {
            return true; // user is not allowed to log-in
        } else
        if (pwd.equals(PASSWORD_HOLDER)) {
            if (msg != null) {
                msg.set(I18N.getString(UserInfo.class,"UserInfo.invalidPasswordSpecified","Invalid password specified"));
            }
            return false;
        } else {
            PasswordHandler pwh = Account.getPasswordHandler(bpl);
            String lastEncPass[] = (user != null)? user.getLastEncodedPasswords() : null;
            return pwh.validateNewPassword(pwd, lastEncPass, msg);
        }
    }

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final I18N    i18n        = privLabel.getI18N(UserInfo.class);
        final Locale  locale      = reqState.getLocale();
        final Account currAcct    = reqState.getCurrentAccount(); // never null
        final String  currAcctID  = reqState.getCurrentAccountID();
        final User    currUser    = reqState.getCurrentUser(); // may be null
        final String  currUserID  = reqState.getCurrentUserID();
        final boolean isAdminUser = User.isAdminUser(currUserID);
        final String  pageName    = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* argument user-id */
        String userList[] = reqState.getUserList();
        String selUserID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_USER_SELECT, "");
        if (StringTools.isBlank(selUserID)) {
            if ((userList.length > 0) && (userList[0] != null)) {
                selUserID = userList[0];
            } else {
                selUserID = currUserID;
            }
        }
        final boolean isCurrentUserSelected = selUserID.equals(currUserID);
        final boolean isSelectedAdminUser = User.isAdminUser(selUserID);
        if (userList.length == 0) {
            userList = new String[] { selUserID };
        }

        /* user db */
        User selUser = null;
        try {
            selUser = !StringTools.isBlank(selUserID)? User.getUser(currAcct, selUserID) : null; // may still be null
        } catch (DBException dbe) {
            // ignore
        }

        /* command */
        String  userCmd      = reqState.getCommandName();
        boolean listUsers    = false;
        boolean updateUser   = userCmd.equals(COMMAND_INFO_UPDATE);
        boolean selectUser   = userCmd.equals(COMMAND_INFO_SELECT);
        boolean newUser      = userCmd.equals(COMMAND_INFO_NEW);
        boolean deleteUser   = false;
        boolean editUser     = false;
        boolean viewUser     = false;

        /* submit buttons */
        String  submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String  submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String  submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String  submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String  submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* display attributes */
        boolean requireEmail = false; // require a non-blank contact email address?
        boolean dupEmailOK   = privLabel.getBooleanProperty(PrivateLabel.PROP_UserInfo_allowDuplicateContactEmail,true);
        boolean showRole     = (privLabel.getWebPage(Constants.PAGE_ROLE_INFO) != null);
        boolean showACLs     = privLabel.getBooleanProperty(PrivateLabel.PROP_UserInfo_showAccessControlList,true);
        boolean showAddress  = privLabel.getBooleanProperty(PrivateLabel.PROP_UserInfo_showAddressFields,false);
        boolean showOffice   = privLabel.getBooleanProperty(PrivateLabel.PROP_UserInfo_showOfficeLocation,false);
        boolean showNotes    = privLabel.getBooleanProperty(PrivateLabel.PROP_UserInfo_showNotes,false);
        int     authGroupCnt = privLabel.getIntProperty(PrivateLabel.PROP_UserInfo_authorizedGroupCount,1);
        boolean showExpire   = isAdminUser && privLabel.getBooleanProperty(PrivateLabel.PROP_UserInfo_showExpiration,false);

        if (authGroupCnt <  1) { authGroupCnt =  1; }
        if (authGroupCnt > 10) { authGroupCnt = 10; }

        /* CACHE_ACL: ACL allow edit/view */
        // see also "CACHE_ACL" below
        AccessLevel aclALL   = privLabel.getAccessLevel(currUser, this.getAclName(_ACL_ALL));
        AccessLevel aclSELF  = privLabel.getAccessLevel(currUser, this.getAclName());
        boolean allowNew     = AclEntry.okAll(aclALL);
        boolean allowDelete  = allowNew;  // 'delete' allowed if 'new' allowed
        boolean allowEditAll = allowNew     || AclEntry.okWrite(aclALL);
        boolean viewAll      = allowEditAll || AclEntry.okRead(aclALL);
        boolean editSelf     = allowEditAll || AclEntry.okWrite(aclSELF);
        boolean viewSelf     = editSelf     || AclEntry.okRead(aclSELF);
        boolean allowEdit    = allowEditAll || editSelf;
        boolean allowView    = allowEdit    || viewAll || viewSelf;

        /* Roles */
        AccessLevel aclROLE  = showRole? privLabel.getAccessLevel(currUser, this.getAclName(_ACL_ROLE)) : AccessLevel.NONE;
        boolean editRole     = !isSelectedAdminUser && AclEntry.okWrite(aclROLE);
        boolean viewRole     = editRole  || AclEntry.okRead(aclROLE);

        /* show preferred deviceID (_ACL_DEVICEID) */
        AccessLevel aclDEVID = privLabel.getAccessLevel(currUser, UserInfo.this.getAclName(_ACL_DEVICEID));
        boolean editDevID    = AclEntry.okWrite(aclDEVID);
        boolean viewDevID    = editDevID || AclEntry.okRead(aclDEVID);
        String  showPrefDevS = privLabel.getStringProperty(PrivateLabel.PROP_UserInfo_showPreferredDeviceID,"").toLowerCase();
        boolean showPrefDev  = AclEntry.okRead(aclDEVID) && (showPrefDevS.equals("true") || (showPrefDevS.equals("admin") && isAdminUser));

        /* ACLs */
        AccessLevel aclACLS  = privLabel.getAccessLevel(currUser, this.getAclName(_ACL_ACLS));
        boolean editAcls     = !isSelectedAdminUser && AclEntry.okWrite(aclACLS);
        boolean viewAcls     = editAcls || AclEntry.okRead(aclACLS);

        /* sub-command */
        String newUserID = null;
        if (newUser) {
            if (!allowNew) {
               newUser = false; // not authorized
            } else {
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newUserID = AttributeTools.getRequestString(httpReq,PARM_NEW_NAME,"").trim();
                newUserID = newUserID.toLowerCase();
                if (StringTools.isBlank(newUserID)) {
                    m = i18n.getString("UserInfo.enterNewUser","Please enter a new user ID."); // UserErrMsg
                    error = true;
                    newUser = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_UserInfo_validateNewIDs,*/newUserID)) {
                    m = i18n.getString("UserInfo.invalidIDChar","ID contains invalid characters"); // UserErrMsg
                    error = true;
                    newUser = false;
                }
            }
        } else
        if (updateUser) {
            //Print.logInfo("Change User ...");
            if (!allowEdit) {
                // not authorized to update users
                updateUser = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("UserInfo.change","Change"))) {
                updateUser = false;
            } else
            if (!isCurrentUserSelected && !allowEditAll) {
                updateUser = false;
            } else
            if (selUser == null) {
                // should not occur
                m = i18n.getString("UserInfo.unableToUpdate","Unable to update User, ID not found"); // UserErrMsg
                error = true;
                updateUser = false;
            }
        } else
        if (selectUser) {
            //Print.logInfo("Select User: " + submit);
            if (SubmitMatch(submitDelete,i18n.getString("UserInfo.delete","Delete"))) {
                if (allowDelete) {
                    if (selUser == null) {
                        m = i18n.getString("UserInfo.pleaseSelectUser","Please select a User"); // UserErrMsg
                        error = true;
                        listUsers = true;
                    } else
                    if (isCurrentUserSelected) {
                        m = i18n.getString("UserInfo.cannotDeleteCurrentUser","Cannot delete current logged-in user"); // UserErrMsg
                        error = true;
                        listUsers = true;
                    } else {
                        deleteUser = true;
                    }
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("UserInfo.edit","Edit"))) {
                if (allowEdit) {
                    if (selUser == null) {
                        m = i18n.getString("UserInfo.pleaseSelectUser","Please select a User"); // UserErrMsg
                        error = true;
                        listUsers = true;
                    } else {
                        editUser = allowEditAll || (isCurrentUserSelected && editSelf);
                        viewUser = true;
                    }
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("UserInfo.view","View"))) {
                if (allowView) {
                    if (selUser == null) {
                        m = i18n.getString("UserInfo.pleaseSelectUser","Please select a User"); // UserErrMsg
                        error = true;
                        listUsers = true;
                    } else {
                        viewUser = true;
                    }
                }
            } else {
                listUsers = true;
            }
        } else {
            listUsers = true;
        }

        /* delete user? */
        if (deleteUser) {
            if (selUser == null) {
                m = i18n.getString("UserInfo.pleaseSelectUser","Please select a User"); // UserErrMsg
                error = true;
            } else {
                try {
                    User.Key userKey = (User.Key)selUser.getRecordKey();
                    Print.logWarn("Deleting User: " + userKey);
                    userKey.delete(true); // will also delete dependencies
                    selUserID = "";
                    selUser = null;
                    reqState.setUserList(null);
                    userList = reqState.getUserList();
                    if (!ListTools.isEmpty(userList)) {
                        selUserID = userList[0];
                        try {
                            selUser = !selUserID.equals("")? User.getUser(currAcct, selUserID) : null; // may still be null
                        } catch (DBException dbe) {
                            // ignore
                        }
                    }
                } catch (DBException dbe) {
                    Print.logException("Deleting User", dbe);
                    m = i18n.getString("UserInfo.errorDelete","Internal error deleting User"); // UserErrMsg
                    error = true;
                }
            }
            listUsers = true;
        }

        /* new user? */
        if (newUser) {
            boolean createUserOK = true;
            for (int u = 0; u < userList.length; u++) {
                if (!newUserID.equalsIgnoreCase(userList[u])) {
                    // continue
                } else
                if (User.isAdminUser(newUserID) && !currAcct.hasAdminUser()) {
                    // new user ID is "admin", and the current account doesn't really have an "admin" user
                    // ok to create "admin" user
                    break;
                } else {
                    // user ID already exists
                    m = i18n.getString("UserInfo.alreadyExists","This user already exists"); // UserErrMsg
                    error = true;
                    createUserOK = false;
                    break;
                }
            }
            if (createUserOK) {
                try {
                    String contactEmail = User.isAdminUser(newUserID)? currAcct.getContactEmail() : null;
                    String decodedPass  = User.isAdminUser(newUserID)? currAcct.getDecodedPassword(privLabel) : null;
                    User user = User.createNewUser(currAcct, newUserID, contactEmail, decodedPass); // already saved
                    reqState.setUserList(null);
                    userList = reqState.getUserList(); // refresh user list
                    selUser = user;
                    selUserID = user.getUserID();
                    m = i18n.getString("UserInfo.createdUser","New user has been created"); // UserErrMsg
                } catch (DBException dbe) {
                    Print.logException("Creating User", dbe);
                    m = i18n.getString("UserInfo.errorCreate","Internal error creating User"); // UserErrMsg
                    error = true;
                }
            }
            listUsers = true;
        }

        /* change/update the user info? */
        if (updateUser) {
            if (selUser == null) {
                m = i18n.getString("UserInfo.noUsers","There are currently no defined users for this account."); // UserErrMsg
            } else {
                String userActive   = AttributeTools.getRequestString(request, PARM_USER_ACTIVE  , "");
                String userDesc     = AttributeTools.getRequestString(request, PARM_USER_NAME    , "");
                String userPassword = AttributeTools.getRequestString(request, PARM_USER_PASSWORD, "");
                String contactName  = AttributeTools.getRequestString(request, PARM_CONTACT_NAME , "");
                String contactPhone = AttributeTools.getRequestString(request, PARM_CONTACT_PHONE, "");
                String contactEmail = AttributeTools.getRequestString(request, PARM_CONTACT_EMAIL, "");
                String notifyEmail  = AttributeTools.getRequestString(request, PARM_NOTIFY_EMAIL , "");
                String timeZone     = AttributeTools.getRequestString(request, PARM_TIMEZONE     , "");
                String speedUnits   = AttributeTools.getRequestString(request, PARM_SPEED_UNITS  , "");
                String distUnits    = AttributeTools.getRequestString(request, PARM_DIST_UNITS   , "");
                String loginPage    = AttributeTools.getRequestString(request, PARM_LOGIN_PAGE   , "");
                String userExpired  = AttributeTools.getRequestString(request, PARM_USER_EXPIRE  , "");
                String prefDevice   = AttributeTools.getRequestString(request, PARM_PREF_DEVICE  , "");
                String maxAccLevel  = AttributeTools.getRequestString(request, PARM_ACCESS_LEVEL , "");
                String userRole     = AttributeTools.getRequestString(request, PARM_USER_ROLE    , "");
                String aclKeys[]    = AttributeTools.getMatchingKeys( request, PARM_ACL_);
                String devGroup[]   = new String[authGroupCnt];
                for (int g = 0; g < devGroup.length; g++) {
                    String devGroup_key = PARM_DEV_GROUP_ + g;
                    String dg   = AttributeTools.getRequestString(request, devGroup_key, "");
                    devGroup[g] = !DeviceGroup.DEVICE_GROUP_NONE.equalsIgnoreCase(dg)? dg : "";
                }
                listUsers = true;
                // -- update
                try {
                    boolean saveOK = true;
                    // -- active
                    if (isCurrentUserSelected) {
                        if (!selUser.getIsActive()) {
                            selUser.setIsActive(true);
                        }
                    } else {
                        boolean userActv = ComboOption.parseYesNoText(locale, userActive, true);
                        if (selUser.getIsActive() != userActv) {
                            selUser.setIsActive(userActv);
                        }
                    }
                    // -- password
                    //WebPage passPage = privLabel.getWebPage(Constants.PAGE_PASSWD);
                    //boolean editPass = (passPage != null)? privLabel.hasWriteAccess(currUser,passPage.getAclName()) : false;
                    if (!isCurrentUserSelected) {
                        I18N.Text pwdMsg = new I18N.Text();
                        if (userPassword.equals(PASSWORD_HOLDER) || StringTools.isBlank(userPassword)) {
                            // -- password not entered
                        } else
                        if (selUser.checkPassword(privLabel,userPassword,false/*suspend?*/)) {
                            // -- password did not change
                        } else
                        if (this.isValidPassword(privLabel,null/*selUser*/,userPassword,pwdMsg)) {
                            selUser.setDecodedPassword(privLabel,userPassword,false/*temp?*/);
                        } else {
                            m = i18n.getString("UserInfo.pleaseEnterValidPassword","Please enter a valid password"); // UserErrMsg
                            if ((pwdMsg != null) && pwdMsg.hasKey()) {
                                m += "\n" + pwdMsg.toString(locale);
                            }
                            error = true;
                            saveOK = false;
                            editUser = true;
                            listUsers = false;
                        }
                    }
                    // -- description
                    if (!userDesc.equals("")) {
                        selUser.setDescription(userDesc);
                    }
                    // -- contact name
                    selUser.setContactName(contactName);
                    // -- contact phone
                    selUser.setContactPhone(contactPhone);
                    // -- contact email
                    if (!requireEmail && StringTools.isBlank(contactEmail)) {
                        // -- allow blank
                        selUser.setContactEmail(contactEmail);
                    } else
                    if (contactEmail.equals(selUser.getContactEmail())) {
                        // -- already set, leave as-is (even if invalid)
                    } else
                    if (!EMail.validateAddress(contactEmail)) {
                        // -- invalid email (blank email address will fail)
                        m = i18n.getString("UserInfo.pleaseEnterContactEMail","Please enter a valid contact email address"); // UserErrMsg
                        m = i18n.getString("UserInfo.pleaseEnterEMail","Please enter a valid email address"); // UserErrMsg
                        error     = true;
                        saveOK    = false;
                        editUser  = true;
                        listUsers = false;
                    } else
                    if (dupEmailOK) {
                        // -- duplicates are allowed, no need to check for duplicates
                        selUser.setContactEmail(contactEmail);
                    } else {
                        // -- check for duplicates
                        User user = User.getUserForContactEmail(null,contactEmail);
                        if (user == null) {
                            // -- no duplicate found
                            selUser.setContactEmail(contactEmail);
                        } else
                        if (user.getAccountID().equals(selUser.getAccountID()) &&
                            user.getUserID().equals(selUser.getUserID())         ) {
                            // -- this user is the duplicate (unlikely since we prechecked above
                            selUser.setContactEmail(contactEmail);
                        } else {
                            // -- duplicate
                            m = i18n.getString("UserInfo.duplicateEMailAddress","The Contact Email address has already been specified for another user");
                            error     = true;
                            saveOK    = false;
                            editUser  = true;
                            listUsers = false;
                        }
                    }
                    // -- notify email
                    if (notifyEmail != null) {
                        String userNE = selUser.getNotifyEmail();
                        if (StringTools.isBlank(notifyEmail)) {
                            if (!userNE.equals(notifyEmail)) {
                                selUser.setNotifyEmail(notifyEmail);
                            }
                        } else
                        if (!EMail.isSendMailEnabled()) {
                            m = i18n.getString("UserInfo.sendMailDisabled","EMail notification is disabled (notify email not allowed), place contact your system administrator");
                            error = true;
                            saveOK = false;
                            editUser = true;
                            listUsers = false;
                        } else
                        if (EMail.validateAddresses(notifyEmail,true/*acceptSMS*/)) {
                            if (!userNE.equals(notifyEmail)) {
                                selUser.setNotifyEmail(notifyEmail);
                            }
                        } else {
                            m = i18n.getString("UserInfo.pleaseEnterNotifyEMail","Please enter a valid notify email/sms address"); // UserErrMsg
                            error = true;
                            saveOK = false;
                            editUser = true;
                            listUsers = false;
                        }
                    }
                    // -- timezone
                    if (!StringTools.isBlank(timeZone)) {
                        selUser.setTimeZone(timeZone);
                    } else {
                        selUser.setTimeZone(DateTime.GMT_TIMEZONE);
                    }
                    // -- speed units
                    if (!StringTools.isBlank(speedUnits)) {
                        selUser.setSpeedUnits(speedUnits, locale);
                    }
                    // -- distance units
                    if (!StringTools.isBlank(distUnits)) {
                        selUser.setDistanceUnits(distUnits, locale);
                    }
                    // -- first login page
                    if (!selUser.getFirstLoginPageID().equals(loginPage)) {
                        selUser.setFirstLoginPageID(loginPage);
                    }
                    // -- expiration
                    if (isAdminUser && showExpire) {
                        if (!StringTools.isBlank(userExpired)) {
                            try {
                                TimeZone tmz = currAcct.getTimeZone(null);
                                DateTime dt  = DateTime.parseArgumentDate(userExpired,tmz,DateTime.DefaultParsedTime.DayEnd);
                                long     ts  = dt.getTimeSec();
                                selUser.setExpirationTime(ts);
                            } catch (DateTime.DateParseException dpe) {
                                // -- invalid date/time format, leave as-is
                            }
                        } else {
                            selUser.setExpirationTime(0L);
                        }
                    }
                    // -- preferred device
                    if (showPrefDev && editDevID && !selUser.getPreferredDeviceID().equals(prefDevice)) {
                        selUser.setPreferredDeviceID(prefDevice);
                    }
                    // -- address info
                    if (showAddress) {
                        // Address, City, State
                        String aline1 = AttributeTools.getRequestString(request, PARM_ADDRESS_LINE_1, "");
                        String aline2 = ""; // AttributeTools.getRequestString(request, PARM_ADDRESS_LINE_2, "");
                        String acity  = AttributeTools.getRequestString(request, PARM_ADDRESS_CITY, "");
                        String astate = AttributeTools.getRequestString(request, PARM_ADDRESS_STATE, "");
                        selUser.setAddressLine1(aline1);
                        selUser.setAddressLine2(aline2);
                        selUser.setAddressCity(acity);
                        selUser.setAddressState(astate);
                    }
                    // -- office location
                    if (showOffice) {
                        String officeLoc = AttributeTools.getRequestString(request, PARM_OFFICE_LOC, "");
                        selUser.setOfficeLocation(officeLoc);
                    }
                    // -- Custom Attributes
                    String cstKeys[] = AttributeTools.getMatchingKeys(request, PARM_USER_CUSTOM_);
                    if (!ListTools.isEmpty(cstKeys)) {
                        String oldCustAttr = selUser.getCustomAttributes();
                        RTProperties rtp = selUser.getCustomAttributesRTP();
                        for (int i = 0; i < cstKeys.length; i++) {
                            String cstKey = cstKeys[i];
                            String rtpVal = AttributeTools.getRequestString(request, cstKey, "");
                            String rtpKey = cstKey.substring(PARM_USER_CUSTOM_.length());
                            rtp.setString(rtpKey, rtpVal);
                        }
                        String rtpStr = rtp.toString();
                        if (!rtpStr.equals(oldCustAttr)) {
                            //Print.logInfo("Setting custom attributes: " + rtpStr);
                            selUser.setCustomAttributes(rtpStr);
                        }
                    }
                    // -- authorized device group
                    if (privLabel.hasWriteAccess(currUser, this.getAclName(_ACL_GROUPS))) {
                        // -- blank ids, and duplicate ids, are already handled
                        selUser.setDeviceGroups(devGroup);
                    }
                    // -- notes
                    if (showNotes) {
                        String notes = AttributeTools.getRequestString(request, PARM_USER_NOTES, "");
                        selUser.setNotes(notes);
                    }
                    // -- Max Access Level
                    if (editAcls) {
                        selUser.setMaxAccessLevel(EnumTools.getValueOf(AccessLevel.class,maxAccLevel,locale));
                    }
                    // -- ACL Role
                    if (showRole && editRole) {
                        if (StringTools.isBlank(userRole) || userRole.equals(i18n.getString("UserInfo.none","None"))) {
                            if (!selUser.getRoleID().equals("")) {
                                //Print.logInfo("Setting user role to blank");
                                selUser.setRoleID("");
                            }
                        } else {
                            if (!selUser.getRoleID().equals(userRole)) {
                                //Print.logInfo("Setting user role to %s", userRole);
                                selUser.setRoleID(userRole);
                            }
                        }
                    }
                    // -- ACLs
                    if (showACLs && editAcls && !ListTools.isEmpty(aclKeys)) {
                        for (int i = 0; i < aclKeys.length; i++) {
                            String aclID  = aclKeys[i].substring(PARM_ACL_.length());
                            String aclVal = AttributeTools.getRequestString(request, aclKeys[i], "");
                            if (aclVal.equalsIgnoreCase(ACL_DEFAULT) ||
                                aclVal.equalsIgnoreCase(i18n.getString("UserInfo.default","Default"))) {
                                try {
                                    UserAcl.deleteAccessLevel(selUser, aclID);
                                } catch (DBException dbe) {
                                    Print.logException("Deleting UserAcl: "+ selUser.getAccountID() + "/" + selUser.getUserID() + "/" + aclID, dbe);
                                    m = i18n.getString("UserInfo.errorDeleteAcl","Internal error deleting UserAcl"); // UserErrMsg
                                    error = true;
                                }
                            } else {
                                AccessLevel selLevel = EnumTools.getValueOf(AccessLevel.class, aclVal, locale);
                                AccessLevel dftLevel = privLabel.getDefaultAccessLevel(aclID);
                                //Print.logInfo("Found ACL: " + aclID + " ==> " + level);
                                try {
                                    if (isSelectedAdminUser) {
                                        // The UserAcl table is not consulted for 'admin' user, delete if present
                                        UserAcl.deleteAccessLevel(selUser, aclID);
                                    //} else
                                    //if (dftLevel.equals(selLevel)) {
                                    //    // matches the default, no reason for this ACL to be in the UserAcl table, delete if present
                                    //    UserAcl.deleteAccessLevel(selUser, aclID);
                                    } else {
                                        // differs from the UserAcl table, set ACL
                                        UserAcl.setAccessLevel(selUser, aclID, selLevel);
                                    }
                                } catch (DBException dbe) {
                                    Print.logException("Updating UserAcl: "+ selUser.getAccountID() + "/" + selUser.getUserID() + "/" + aclID, dbe);
                                    m = i18n.getString("UserInfo.errorUpdate","Internal error updating User"); // UserErrMsg
                                    error = true;
                                }
                            }
                        }
                        // -- CACHE_ACL: update cached ACL
                        aclALL       = privLabel.getAccessLevel(currUser, this.getAclName(_ACL_ALL));
                        aclSELF      = privLabel.getAccessLevel(currUser, this.getAclName());
                        allowNew     = AclEntry.okAll(aclALL);
                        allowDelete  = allowNew;  // 'delete' allowed if 'new' allowed
                        allowEditAll = allowNew     || AclEntry.okWrite(aclALL);
                        viewAll      = allowEditAll || AclEntry.okRead(aclALL);
                        editSelf     = allowEditAll || AclEntry.okWrite(aclSELF);
                        viewSelf     = editSelf     || AclEntry.okRead(aclSELF);
                        allowEdit    = allowEditAll || editSelf;
                        allowView    = allowEdit    || viewAll || viewSelf;
                    }
                    // -- save
                    if (saveOK) {
                        selUser.save();
                        //Track.writeMessageResponse(reqState,
                        //    i18n.getString("UserInfo.userUpdated","User information updated"));
                        m = i18n.getString("UserInfo.userUpdated","User information updated"); // UserErrMsg
                    } else {
                        // -- should stay on this page
                        editUser = true;
                        listUsers = false;
                    }
                } catch (Throwable t) {
                    //Track.writeErrorResponse(reqState,
                    //    i18n.getString("UserInfo.errorUpdate","Internal error updating User"));
                    Print.logException("Updating User", t);
                    m = i18n.getString("UserInfo.errorUpdate","Internal error updating User"); // UserErrMsg
                    error = true;
                    //return;
                }
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = UserInfo.this.getCssDirectory();
                //WebPageAdaptor.writeCssLink(out, reqState, "UserInfo.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
            }
        };

        /* Content */
        final boolean  _showPrefDev  = showPrefDev;
        final boolean  _showAddress  = showAddress;
        final boolean  _showOffice   = showOffice;
        final boolean  _showExpire   = isAdminUser && showExpire;
        final boolean  _showNotes    = showNotes;
        final boolean  _showACLs     = showACLs;
        final boolean  _showRole     = showRole;
        final int      _authGroupCnt = authGroupCnt;
        final String   _selUserID    = selUserID;
        final User     _selUser      = selUser;
        final String   _userList[]   = userList;
        final boolean  _allowEdit    = allowEdit;
        final boolean  _allowEditAll = allowEditAll;
        final boolean  _allowView    = allowView;
        final boolean  _allowNew     = allowNew;
        final boolean  _allowDelete  = allowDelete;
        final boolean  _editUser     = _allowEdit && editUser;
        final boolean  _viewUser     = _editUser || viewUser;
        final boolean  _listUsers    = listUsers || (!_editUser && !_viewUser);
        final ComboMap _tzList       = privLabel.getTimeZoneComboMap();
        final ComboMap _suList       = privLabel.getEnumComboMap(Account.SpeedUnits.class      );
        final ComboMap _duList       = privLabel.getEnumComboMap(Account.DistanceUnits.class   );
        //AccessLevel   aclROLE       = _showRole? privLabel.getAccessLevel(currUser, UserInfo.this.getAclName(_ACL_ROLE)) : AccessLevel.NONE;
        //final boolean _editRole     = _editUser && AclEntry.okWrite(aclROLE);
        //final boolean _viewRole     = _editRole || AclEntry.okRead( aclROLE);
        final boolean  _editRole     = _showRole && editRole && _editUser;
        final boolean  _viewRole     = _showRole && (_editRole || viewRole);
        final boolean  _editDevID    = _showPrefDev && editDevID && _editUser;
        final boolean  _viewDevID    = _showPrefDev && (_editDevID || viewDevID);
        final boolean  _editAcls     = editAcls && _editUser;
        final boolean  _viewAcls     = _editAcls || viewAcls;
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = UserInfo.this.getPageName();

                /* custom attributes */
                Collection<String> customKeys = new OrderedSet<String>();
                Collection<String> ppKeys = privLabel.getPropertyKeys(PrivateLabel.PROP_UserInfo_custom_);
                for (String ppKey : ppKeys) {
                    customKeys.add(ppKey.substring(PrivateLabel.PROP_UserInfo_custom_.length()));
                }
                if (_selUser != null) {
                    customKeys.addAll(_selUser.getCustomAttributeKeys());
                }

                // frame header
              //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String editURL    = UserInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String selectURL  = UserInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String newURL     = UserInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String frameTitle = _allowEdit?
                    i18n.getString("UserInfo.viewEditUser","View/Edit User Information") :
                    i18n.getString("UserInfo.viewUser","View User Information");
              //String selectUserJS = "javascript:dinfoSelectUser()";
                out.write("<h1 class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</h1>\n");

                // user selection table (Select, User ID, User Desc)
                if (_listUsers) {

                    // user selection table (Select, User ID, User Desc)
                    out.write("<h3 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("UserInfo.selectUser","Select a User")+":</h3>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form class='form-horizontal' name='"+FORM_USER_SELECT+"' method='post' class='form-horizontal' action='"+selectURL+"' target='_self'>"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+" table' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+FilterText(i18n.getString("UserInfo.select","Select"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("UserInfo.userID","User ID"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("UserInfo.userName","User Desc"))+"</th>\n");
                    if (_showRole && _viewRole) {
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("UserInfo.role","Role"))+"</th>\n");
                    }
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("UserInfo.contactName","Contact Name"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("UserInfo.contactEmail","Contact Email"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("UserInfo.timeZone","Time Zone"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("UserInfo.active","Active"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+FilterText(i18n.getString("UserInfo.lastLogin","Last Login\n{0}",currAcct.getTimeZone()))+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    for (int u = 0; u < _userList.length; u++) {
                        if ((u & 1) == 0) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        try {
                            User usr = User.getUser(currAcct, _userList[u]);
                            if (usr != null) {
                                String uid          = usr.getUserID();
                                String userID       = FilterText(uid);
                                String userDesc     = FilterText(usr.getDescription());
                                String contactName  = FilterText(usr.getContactName());
                                String contactEmail = FilterText(usr.getContactEmail());
                                String timeZone     = FilterText(usr.getTimeZone());
                                String active       = FilterText(ComboOption.getYesNoText(locale,usr.isActive()));
                                long  lastLoginTime = usr.getLastLoginTime();
                                String lastLogin    = FilterText(currAcct.formatDateTime(lastLoginTime));
                                String checked      = _selUserID.equals(uid)? " checked" : "";
                                String viewStyle    = (_allowEdit && !_allowEditAll)? ((currUserID.equals(uid))?"background-color:#FFFFFF;":"background-color:#E5E5E5;") : "";
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"' style='"+viewStyle+"'><input type='radio' name='"+PARM_USER_SELECT+"' id='"+userID+"' value='"+userID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+userID+"'>"+userID+"</label></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+userDesc+"</td>\n");
                                if (_showRole && _viewRole) {
                                String userRoleID   = FilterText(Role.getDisplayRoleID(usr.getRoleID()));
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+userRoleID+"</td>\n");
                                }
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+contactName+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+contactEmail+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+timeZone+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+active+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' "+SORTTABLE_SORTKEY+"='"+lastLoginTime+"' nowrap>"+lastLogin+"</td>\n");
                            }
                        } catch (DBException dbe) {
                            //
                        }
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' class='table' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView  ) {
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' class='btn btn-success' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("UserInfo.view","View")+"'>");
                        out.write("</td>\n");
                    }
                    if (_allowEdit  ) {
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' class='btn btn-warning' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("UserInfo.edit","Edit")+"'>");
                        out.write("</td>\n");
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' class='btn btn-danger' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("UserInfo.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;");
                    }
                    out.write("</td>\n");
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");


                    /* new user */
                    if (_allowNew) {
                    out.write("<h3 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("UserInfo.createNewUser","Create a new user")+":</h3\n");
                    out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<form class='form-horizontal' name='"+FORM_USER_NEW+"' method='post' class='form-horizontal' action='"+newURL+"' target='_self'>"); // target='_top'
                    out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                    out.write(i18n.getString("UserInfo.userID","User ID")+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+" form-control' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='32'><br>\n");
                    out.write(" <input type='submit' class='btn btn-success' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("UserInfo.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");

                    }

                } else {
                    // user view/edit form

                    /* start of form */
                    out.write("<form class='form-horizontal' name='"+FORM_USER_EDIT+"' method='post' action='"+editURL+"' target='_self'>\n"); // target='_top'
                    out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                    /* password */
                    //AccessLevel aclPASS = privLabel.getAccessLevel(currUser, UserInfo.this.getAclName(_ACL_PASSWORD));
                    boolean _editPass = _editUser && !isCurrentUserSelected; // && AclEntry.okWrite(aclPASS);
                    boolean _viewPass = _editPass || true; // _editPass || AclEntry.okRead(aclPASS);

                    /* device group(s) */
                    boolean   onlyPrefDev = User.IsPreferredDeviceAuth_ONLY(); // [2.6.3-B30]
                    AccessLevel aclGROUPS = privLabel.getAccessLevel(currUser, UserInfo.this.getAclName(_ACL_GROUPS));
                    boolean   _editGroups = !onlyPrefDev && (_editUser   && AclEntry.okWrite(aclGROUPS));
                    boolean   _viewGroups = !onlyPrefDev && (_editGroups || AclEntry.okRead( aclGROUPS));
                    ComboMap   _grpsMap[] = null;
                    String     _devGrps[] = null;
                    if (_viewGroups) {
                        _grpsMap = new ComboMap[_authGroupCnt];
                        _devGrps = new String[_authGroupCnt];
                        // -- get list of available device groups
                        java.util.List<String> devGroups = null;
                        try {
                            devGroups = (_selUser!=null)? _selUser.getDeviceGroups(true) : null;
                        } catch (DBException dbe) {
                            devGroups = null;
                        }
                        // -- first/second device group
                        if (!ListTools.isEmpty(devGroups)) {
                            // at least one group
                            for (int g = 0; g < _devGrps.length; g++) {
                                _devGrps[g] = (devGroups.size() > g)? devGroups.get(g) : DeviceGroup.DEVICE_GROUP_NONE;
                            }
                        } else {
                            // -- no groups, default to "ALL"
                            _devGrps[0] = DeviceGroup.DEVICE_GROUP_ALL;
                            for (int g = 1; g < _devGrps.length; g++) {
                                _devGrps[g] = DeviceGroup.DEVICE_GROUP_NONE;
                            }
                        }
                        // -- create ComboMaps
                        if (_editGroups) {
                            //OrderedSet<String> grpList = reqState.getDeviceGroupIDList(true);
                            try {
                                OrderedSet<String> grpList = DeviceGroup.getDeviceGroupsForAccount(currAcctID,false);
                                for (int g = 0; g < _devGrps.length; g++) {
                                    // -- check for obsolete groups
                                    if (DeviceGroup.DEVICE_GROUP_ALL.equalsIgnoreCase(_devGrps[g])) {
                                        // -- skip "ALL"
                                    } else
                                    if (DeviceGroup.DEVICE_GROUP_NONE.equalsIgnoreCase(_devGrps[g])) {
                                        // -- skip "none"
                                    } else
                                    if (!grpList.contains(_devGrps[g])) {
                                        Print.logWarn("Obsolete device group: " + _devGrps[g]);
                                        grpList.add(_devGrps[g]);
                                    }
                                }
                                for (int g = 0; g < _grpsMap.length; g++) {
                                    _grpsMap[g] = new ComboMap(grpList);
                                    if (g == 0) {
                                        _grpsMap[g].insert(DeviceGroup.DEVICE_GROUP_ALL);
                                    } else {
                                        _grpsMap[g].insert(DeviceGroup.DEVICE_GROUP_NONE, i18n.getString("UserInfo.selectGroup","-select group-"));
                                    }
                                }
                            } catch (DBException dbe) {
                                Print.logException("Getting DeviceGroups for Account", dbe);
                                for (int g = 0; g < _grpsMap.length; g++) {
                                    _grpsMap[g] = new ComboMap(_devGrps[g]);
                                    if (g == 0) {
                                        _grpsMap[g].insert(DeviceGroup.DEVICE_GROUP_ALL);
                                    } else {
                                        _grpsMap[g].insert(DeviceGroup.DEVICE_GROUP_NONE, i18n.getString("UserInfo.selectGroup","-select group-"));
                                    }
                                }
                            }
                        } else {
                            for (int g = 0; g < _grpsMap.length; g++) {
                                _grpsMap[g] = new ComboMap(_devGrps[g]);
                            }
                        }
                    }
                    String grpTitles[]  = reqState.getDeviceGroupTitles();
                    String devTitles[]  = reqState.getDeviceTitles();

                    /* first login page */
                    ComboMap loginList = UserInfo.GetFirstLoginListMap(locale);
                    String loginPage = (_selUser != null)? _selUser.getFirstLoginPageID() : Constants.PAGE_MENU_TOP;

                    /* Roles */
                    String textNone     = i18n.getString("UserInfo.none","None");
                    String userRoleID   = (_selUser != null)? _selUser.getRoleID() : "";
                    ComboMap roleMap    = null;
                    if (_editRole) {
                        try {
                            String roleIDs[] = Role.getRolesForAccount(currAcct.getAccountID(),true);
                            roleMap = new ComboMap();
                            roleMap.put(textNone,textNone);
                            for (int i = 0; i < roleIDs.length; i++) {
                                roleMap.add(roleIDs[i], Role.getDisplayRoleID(roleIDs[i]));
                            }
                        } catch (DBException dbe) {
                            roleMap = new ComboMap(textNone);
                        }
                        if (StringTools.isBlank(userRoleID) || !ListTools.containsKey(roleMap,userRoleID)) {
                            userRoleID = textNone;
                        }
                    } else {
                        if (StringTools.isBlank(userRoleID)) {
                            userRoleID = textNone;
                        }
                    }

                    /* password */
                    String password = PASSWORD_HOLDER;
                    boolean showPass = privLabel.getBooleanProperty(PrivateLabel.PROP_UserInfo_showPassword,false);
                    if (showPass && (_selUser != null)) {
                        password = _selUser.getDecodedPassword(privLabel);
                        if (password == null) {
                            password = "?";
                        }
                    }

                    /* User fields */
                    ComboOption userActive = ComboOption.getYesNoOption(locale, ((_selUser != null) && _selUser.isActive()));
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+" table' cellspacing='0' callpadding='0' border='0'>");
                  //out.println(FormRow_ComboBox (PARM_USER_SELECT   , !isCurrentUserSelected, i18n.getString("UserInfo.userID","User ID")+":", _selUserID, _userList, selectUserJS, -1));
                    out.println(FormRow_TextField(PARM_USER_SELECT   , false     , i18n.getString("UserInfo.userID","User ID")+":"            , _selUserID, 40, 40));
                    out.println(FormRow_ComboBox (PARM_USER_ACTIVE   , _editUser && !isCurrentUserSelected, i18n.getString("UserInfo.active","Active")+":", userActive, ComboMap.getYesNoMap(locale), "", -1));
                    out.println(FormRow_TextField(PARM_USER_NAME     , _editUser , i18n.getString("UserInfo.userDescription","User Description")+":" , (_selUser!=null)?_selUser.getDescription() :"", 40, 40));
                    if (_viewPass) {
                        out.println(FormRow_TextField(PARM_USER_PASSWORD , _editPass , i18n.getString("UserInfo.password","Password")+":"     , password, 20, 20));
                    }
                    out.println(FormRow_TextField(PARM_CONTACT_NAME  , _editUser , i18n.getString("UserInfo.contactName","Contact Name")+":"  , (_selUser!=null)?_selUser.getContactName() :"", 50, 60));
                    out.println(FormRow_TextField(PARM_CONTACT_PHONE , _editUser , i18n.getString("UserInfo.contactPhone","Contact Phone")+":", (_selUser!=null)?_selUser.getContactPhone():"", 30, 30));
                    out.println(FormRow_TextField(PARM_CONTACT_EMAIL , _editUser , i18n.getString("UserInfo.contactEmail","Contact Email")+":", (_selUser!=null)?_selUser.getContactEmail():"", 60, 100));
                    out.println(FormRow_TextField(PARM_NOTIFY_EMAIL  , _editUser , i18n.getString("UserInfo.notifyEMail","Notify Email:")     , (_selUser!=null)?_selUser.getNotifyEmail() :"", 95, 125));
                    out.println(FormRow_ComboBox (PARM_TIMEZONE      , _editUser , i18n.getString("UserInfo.timeZone","Time Zone")+":"        , (_selUser!=null)?_selUser.getTimeZone()    :"", _tzList, null, 24));
                    // -- address/office
                    if (_showAddress) {
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextField(PARM_ADDRESS_LINE_1, _editUser , i18n.getString("UserInfo.addressLine1","Address Line")+":"  , (_selUser!=null)?_selUser.getAddressLine1() :"", 65, 69));
                        out.println(FormRow_TextField(PARM_ADDRESS_CITY  , _editUser , i18n.getString("UserInfo.addressCity" ,"Address City")+":"  , (_selUser!=null)?_selUser.getAddressCity()  :"", 49, 49));
                        out.println(FormRow_TextField(PARM_ADDRESS_STATE , _editUser , i18n.getString("UserInfo.addressState","Address State")+":" , (_selUser!=null)?_selUser.getAddressState() :"", 49, 49));
                    }
                    if (_showOffice) {
                        if (!_showAddress) { out.println(FormRow_Separator()); }
                        out.println(FormRow_TextField(PARM_OFFICE_LOC, _editUser , i18n.getString("UserInfo.officeLocation" ,"Office Location")+":", (_selUser!=null)?_selUser.getOfficeLocation() :"", 50, 50));
                    }
                    // -- speed/distance units
                    if (User.getFactory().hasField(User.FLD_speedUnits)) {
                        ComboOption speedUnits    = privLabel.getEnumComboOption(Account.getSpeedUnits(_selUser)      );
                        ComboOption distanceUnits = privLabel.getEnumComboOption(Account.getDistanceUnits(_selUser)   );
                        out.println(FormRow_ComboBox (PARM_SPEED_UNITS   , _editUser , i18n.getString("UserInfo.speedUnits","Speed Units:")       , speedUnits       , _suList, null, 10));
                        out.println(FormRow_ComboBox (PARM_DIST_UNITS    , _editUser , i18n.getString("UserInfo.distanceUnits","Distance Units:") , distanceUnits    , _duList, null, 10));
                    }
                    // -- authorized groups
                    if (_viewGroups) {
                        out.println(FormRow_Separator());
                        String s = i18n.getString("UserInfo.authDeviceGroup","Authorized {0}",grpTitles);
                        if (_grpsMap.length == 1) {
                            out.println(FormRow_ComboBox(PARM_DEV_GROUP_+"0", _editGroups, s+":", _devGrps[0], _grpsMap[0], "", 20));
                        } else {
                            for (int g = 0; g < _grpsMap.length; g++) {
                                out.println(FormRow_ComboBox(PARM_DEV_GROUP_+g, _editGroups, s+" #"+(g+1)+":", _devGrps[g], _grpsMap[g], "", 20));
                            }
                        }
                    }
                    //out.println(FormRow_Separator());
                    // -- login page
                    out.println(FormRow_ComboBox (PARM_LOGIN_PAGE    , _editUser , i18n.getString("UserInfo.firstLoginPage","First Login Page")+":" , loginPage, loginList, "", 16));
                    if (_showPrefDev && _viewDevID) {
                        String selDevID = (_selUser != null)? _selUser.getPreferredDeviceID() : "";
                        ComboMap devMap = new ComboMap(reqState.createDeviceDescriptionMap(true/*includeID*/));
                        devMap.insert("", i18n.getString("UserInfo.noDevice","None",devTitles)); // "No {0}"
                      //OrderedSet<String> devList = new OrderedSet<String>();
                      //devList.add(""); // none
                      //devList.addAll(reqState.getDeviceIDList(true));
                      //String devArry[] = (String[])devList.toArray(new String[devList.size()]);
                      //out.println(FormRow_TextField(PARM_PREF_DEVICE , _editUser, i18n.getString("UserInfo.preferredDevice","Preferred {0} ID",devTitles)+":", selDevID, 32, 32));
                        out.println(FormRow_ComboBox (PARM_PREF_DEVICE , _editDevID, i18n.getString("UserInfo.preferredDevice","Preferred {0} ID",devTitles)+":", selDevID, devMap, "", -1));
                    }
                    // -- expiration
                    if (isAdminUser && _showExpire) {
                        String dateFmt = "yyyy/MM/dd,HH:mm:ss,z";
                        TimeZone tmz = (_selUser != null)? _selUser.getTimeZone(DateTime.GMT) : DateTime.GMT;
                        //out.println(FormRow_Separator());
                        long     expireTime = (_selUser != null)? _selUser.getExpirationTime() : 0L;
                        DateTime expireDT   = (expireTime > 0L)? new DateTime(expireTime,tmz) : null;
                        String   expireFmt  = (expireDT != null)? expireDT.format(dateFmt) : "";
                        out.println(FormRow_TextField(PARM_USER_EXPIRE , _editUser, i18n.getString("UserInfo.expiration","Expiration Date/Time")+":", expireFmt, 30, 30));
                    }
                    // -- notes
                    if (_showNotes) {
                        String noteText = (_selUser != null)? StringTools.decodeNewline(_selUser.getNotes()) : "";
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextArea(PARM_USER_NOTES, _editUser, i18n.getString("UserInfo.notes" ,"General Notes")+":", noteText, 5, 70));
                    }
                    if (!ListTools.isEmpty(customKeys)) {
                        out.println(FormRow_Separator());
                        for (String key : customKeys) {
                            String desc  = privLabel.getStringProperty(PrivateLabel.PROP_UserInfo_custom_ + key, key);
                            String value = (_selUser != null)? _selUser.getCustomAttribute(key) : "";
                            out.println(FormRow_TextField(PARM_USER_CUSTOM_ + key, _editUser, desc + ":", value, 40, 50));
                        }
                    }

                    /* Default AccessLevel/Role */
                    out.println(FormRow_Separator());
                    ComboMap    dftAclOpt = null;
                    ComboOption dftAccLvl = null;
                    boolean     editALvl  = _editAcls; // if can edit ACLs
                    boolean     viewALvl  = _viewAcls; // if can view ACLs
                    if ((_selUser == null) || _selUser.isAdminUser()) {
                        // "admin" user
                        dftAclOpt = privLabel.getEnumComboMap(AccessLevel.class, new AccessLevel[] { AccessLevel.ALL });
                        dftAccLvl = privLabel.getEnumComboOption(AccessLevel.ALL);
                        editALvl  = false;  // cannot edit, since "ALL" is the only available option
                    } else {
                        dftAclOpt = privLabel.getEnumComboMap(AccessLevel.class, new AccessLevel[] { AccessLevel.READ, AccessLevel.WRITE, AccessLevel.ALL });
                        dftAccLvl = privLabel.getEnumComboOption(AclEntry.getAccessLevel(_selUser.getMaxAccessLevel()));
                    }
                    if (viewALvl) {
                        out.write(FormRow_ComboBox(PARM_ACCESS_LEVEL, editALvl, i18n.getString("UserInfo.maxAccessLevel","Maximum Access Level")+":", dftAccLvl, dftAclOpt, "", 18, null) + "\n");
                    }
                    if (_showRole && _viewRole) {
                        out.println(FormRow_ComboBox (PARM_USER_ROLE, _editRole, i18n.getString("UserInfo.defaultRole","Default ACL Role")+":", userRoleID, roleMap, null, 20));
                    }

                    /* end table */
                    out.println("</table>");


                    /* ACL entries (overrides) */
                    if (_showACLs && _viewAcls) {
                        out.write("<h3 style='margin-left: 4px; margin-top: '>");
                        out.write(i18n.getString("UserInfo.userAccessControl","User Access Control"));
                        //out.write(": ");
                        //out.write(i18n.getString("UserInfo.scrollDownToView","(scroll to view all configurable options)"));
                        out.write("</h3>\n");
                        out.write("<div class='userAclViewDiv'>\n");
                        out.write("<table>\n");
                        AclEntry aclEntries[] = privLabel.getAllAclEntries();
                        String aclLevels[] = EnumTools.getValueNames(AccessLevel.class, locale);
                        Role userRole = (_selUser != null)? _selUser.getRole() : null;
                        for (int a = 0; a < aclEntries.length; a++) {
                            AclEntry acl = aclEntries[a];
                            if (!acl.isHidden()) {
                                String      aclName  = acl.getName();
                                String      argKey   = PARM_ACL_ + aclName;
                                AccessLevel valAcc[] = acl.getAccessLevelValues();  // is not null
                                AccessLevel maxAcc   = acl.getMaximumAccessLevel(); // is not null
                                AccessLevel dftAcc   = User.isAdminUser(_selUser)? maxAcc : privLabel.getAccessLevel(userRole,aclName); // is not null
                                String      desc     = acl.getDescription(locale);
                                ComboMap    aclOpt   = privLabel.getEnumComboMap(AccessLevel.class, valAcc);
                                aclOpt.insert(ACL_DEFAULT, i18n.getString("UserInfo.default","Default"));
                                AccessLevel usrAcc   = (!isSelectedAdminUser && (_selUser != null))? UserAcl.getAccessLevel(_selUser,aclName,AccessLevel.UNDEFINED) : null;
                                if ((usrAcc != null) && usrAcc.equals(AccessLevel.UNDEFINED)) { usrAcc = null; }
                                if ((usrAcc != null) && !ListTools.contains(valAcc,usrAcc))   { usrAcc = maxAcc; } // to prevent selecting non-existent levels
                                if (_editAcls) {
                                    ComboOption accSel = (usrAcc != null)?
                                        privLabel.getEnumComboOption(usrAcc) :
                                        new ComboOption(ACL_DEFAULT, i18n.getString("UserInfo.default","Default"));
                                    String  dftHtml  = i18n.getString("UserInfo.defaultIsAcl","");
                                    out.write(FormRow_ComboBox(argKey, true , desc+":", accSel, aclOpt, "", 18, dftHtml) + "\n");
                                } else {
                                    ComboOption accSel = (usrAcc != null)?
                                        privLabel.getEnumComboOption(usrAcc) :
                                        privLabel.getEnumComboOption(dftAcc);
                                    out.write(FormRow_ComboBox(argKey, false, desc+":", accSel, aclOpt, "", 18, null) + "\n");
                                }
                            }
                        }
                        out.write("</table>\n");
                        out.write("</div>\n");
                        out.write("<hr />\n");
                    }

                    /* end of form */

                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editUser) {
                        out.write("<input type='submit' class='btn btn-warning' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("UserInfo.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' class='btn btn-default' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("UserInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    } else {
                        out.write("<input type='button' class='btn btn-default' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("UserInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_self');\">\n"); // target='_top'
                    }
                    out.write("</form>\n");

                }

            }
        };

        /* write frame */
        String onload = error? JS_alert(true,m) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------
}
