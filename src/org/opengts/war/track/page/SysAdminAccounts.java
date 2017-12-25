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
//  2008/12/01  Martin D. Flynn
//     -Initial release
//  2009/01/28  Martin D. Flynn
//     -Added "Logged-In" list column
//  2009/08/23  Martin D. Flynn
//     -Added ability to log-in to selected account (this feature controlled by
//      property "sysAdminAccounts.allowAccountLogin" - default is "false").
//     -Convert new entered IDs to lowercase
//  2009/09/23  Martin D. Flynn
//     -Added "TemporaryProperties" field.
//  2010/09/09  Martin D. Flynn
//     -Moved to "org.opengts.war.track.page"
//  2011/03/08  Martin D. Flynn
//     -Added GeocoderMode, IsBorderCrossing (moved from AccountInfo.java)
//  2011/06/16  Martin D. Flynn
//     -Added "Notes" text option
//  2012/12/24  Martin D. Flynn
//     -Change "form" target to "_self" (rather than "_top")
//  2013/03/01  Martin D. Flynn
//     -Default AccountManagerID to selected AccountID, if unspecified.
//  2013/08/06  Martin D. Flynn
//     -Added ACL support for Read/View
//     -Added setting for Account "Notify Enable" (see PARM_ACCT_RULE_ALLOW)
//  2014/11/30  Martin D. Flynn
//     -Added support for delete control
//     -Added support for global "alwaysAllowWebService" property settings. [2.5.8-B36]
//     -Save AccountManagerID even if selected account is not an AccountManager [2.5.8-B62]
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

public class SysAdminAccounts
    extends WebPageAdaptor
    implements Constants
{
    
    // ------------------------------------------------------------------------
    
    private static final boolean SHOW_NOTES                     = false;

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String FORM_ACCOUNT_SELECT             = "SysAdminSelect";
    public  static final String FORM_ACCOUNT_EDIT               = "SysAdminEdit";
    public  static final String FORM_ACCOUNT_NEW                = "SysAdminNew";

    // commands
    public  static final String COMMAND_INFO_UPDATE             = "update";
    public  static final String COMMAND_INFO_SELECT             = "select";
    public  static final String COMMAND_INFO_NEW                = "new";

    // submit
    public  static final String PARM_SUBMIT_EDIT                = "a_subedit";
    public  static final String PARM_SUBMIT_VIEW                = "a_subview";
    public  static final String PARM_SUBMIT_CHG                 = "a_subchg";
    public  static final String PARM_SUBMIT_DEL                 = "a_subdel";
    public  static final String PARM_SUBMIT_NEW                 = "a_subnew";
    public  static final String PARM_SUBMIT_LOGIN               = "a_sublogin";

    // buttons
    public  static final String PARM_BUTTON_CANCEL              = "d_btncan";
    public  static final String PARM_BUTTON_BACK                = "d_btnbak";

    // parameters
    public  static final String PARM_NEW_NAME                   = "s_newname";
    public  static final String PARM_ACCOUNT_SELECT             = "s_account";
    
    public  static final String PARM_ACCT_ID                    = "a_id";
    public  static final String PARM_ACCT_CREATED               = "a_created";
    public  static final String PARM_ACCT_LAST_LOGIN            = "a_lastlogin";
    public  static final String PARM_ACCT_DESC                  = "a_desc";
    public  static final String PARM_ACCT_PASSWORD              = "a_pass";
    public  static final String PARM_ACCT_ACTIVE                = "a_active";
    public  static final String PARM_ACCT_CONTACT_NAME          = "a_contact";
    public  static final String PARM_ACCT_CONTACT_PHONE         = "a_phone";
    public  static final String PARM_ACCT_CONTACT_EMAIL         = "a_email";
    public  static final String PARM_ACCT_TIMEZONE              = "a_tmz";
    public  static final String PARM_ACCT_AUTODEV               = "a_autodev";
    public  static final String PARM_ACCT_MAXDEV                = "a_maxdev";
    public  static final String PARM_ACCT_EXPIRE                = "a_expire";
    public  static final String PARM_ACCT_SUSPEND               = "a_suspend";
    public  static final String PARM_ACCT_TEMP_PROPS            = "a_tmpProps";
    public  static final String PARM_ACCT_NOTES                 = "a_notes";

    public  static final String PARM_ACCT_IS_MANAGER            = "a_manager";
    public  static final String PARM_ACCT_MANAGER_ID            = "a_managerid";
    public  static final String PARM_ACCT_PRIVLABEL             = "a_privlbl";
    public  static final String PARM_ACCT_RG_MODE               = "a_rgmode";
    public  static final String PARM_ACCT_IS_BCROSS             = "a_bcross";
    public  static final String PARM_ACCT_DATA_PUSH_URL         = "a_pushurl";
    public  static final String PARM_ACCT_DCS_PROPS_ID          = "a_dcspropid";
    public  static final String PARM_ACCT_MAX_PING              = "a_pingmax";
    public  static final String PARM_ACCT_TOTAL_PING            = "a_pingtotal";
    public  static final String PARM_ACCT_PING_RESET            = "a_pingreset";

    public  static final String PARM_ACCT_RULE_ALLOW            = "a_ruleallw"; // see Device.PARM_DEV_RULE_ALLOW

    public  static final String PARM_ACCT_SMS_ENABLED           = "a_smsenable";
    public  static final String PARM_ACCT_SMS_PROPS             = "a_smsprops";

    public  static final String PARM_ACCT_RETAIN_EVENTS         = "a_retainEv";
    public  static final String PARM_ACCT_WEB_SERVICE           = "a_wsAllow";
    public  static final String PARM_ACCT_ELOG_ENABLE           = "a_elog";
    public  static final String PARM_ACCT_ELOG_ACCOUNT          = "a_elogAcct";
    public  static final String PARM_ACCT_ELOG_USER             = "a_elogUser";
    public  static final String PARM_ACCT_ELOG_PASSWORD         = "a_elogPass";

    public  static final String PARM_SMTP_HOST                  = "a_emHost";  // String
    public  static final String PARM_SMTP_PORT                  = "a_emPort";  // Integer
    public  static final String PARM_SMTP_USER                  = "a_emUser";  // String
    public  static final String PARM_SMTP_EMAIL                 = "a_emEmail"; // String
    public  static final String PARM_SMTP_PASS                  = "a_emPass";  // String
    public  static final String PARM_SMTP_SSL                   = "a_emSSL";   // SSL
    public  static final String PARM_SMTP_TLS                   = "a_emTLS";   // TLS

    // ------------------------------------------------------------------------

    // password holder/indicator
    private static final String PASSWORD_HOLDER                 = "**********";
    private static final char   PASSWORD_INVALID_CHAR           = '*'; // password can't have all '*'

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public SysAdminAccounts()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_SYSADMIN_ACCOUNTS);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
        //this.setCssDirectory("extra/css");
    }

    // ------------------------------------------------------------------------

    //public void setCssDirectory(String cssDir)
    //{
    //    super.setCssDirectory(cssDir);
    //    Print.logStackTrace("CSS Dir: " + cssDir);
    //}

    // ------------------------------------------------------------------------
   
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminAccounts.class);
        return super._getMenuDescription(reqState,i18n.getString("SysAdminAccounts.editMenuDesc","System Accounts"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminAccounts.class);
        return super._getMenuHelp(reqState,i18n.getString("SysAdminAccounts.editMenuHelp","Create/Delete/Edit/View System Accounts"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminAccounts.class);
        return super._getNavigationDescription(reqState,i18n.getString("SysAdminAccounts.navDesc","Accounts"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminAccounts.class);
        return i18n.getString("SysAdminAccounts.navTab","System Accounts");
    }

    // ------------------------------------------------------------------------

    /* true if this page iis for the system admin only */
    public boolean systemAdminOnly()
    {
        return false;
    }

    // ------------------------------------------------------------------------

    public boolean isOkToDisplay(RequestProperties reqState)
    {
        Account account = (reqState != null)? reqState.getCurrentAccount() : null;
        if (account == null) {
            return false; // no account?
        } else
        if (account.isSystemAdmin()) {
            return true;
        } else
        if (account.isAccountManager()) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    private static String filter(String s)
    {
        return StringTools.isBlank(s)? "&nbsp;" : StringTools.htmlFilterText(s);
    }
    
    private boolean isValidPassword(String pwd)
    {
        if (StringTools.isBlank(pwd)) {
            return true; // user is not allowed to log-in
        } else
        if (pwd.equals(PASSWORD_HOLDER)) {
            return false;
        } else {
            for (int i = 0; i < pwd.length(); i++) {
                if (pwd.charAt(i) != PASSWORD_INVALID_CHAR) {
                    return true;
                }
            }
            return false; // all '*'
        }
    }

    private Map<String,java.util.List<String>> getLoggedInAccounts(RequestProperties reqState)
    {
        final Map<String,java.util.List<String>> acctLoginMap = new HashMap<String,java.util.List<String>>();
        HttpSession session = AttributeTools.getSession(reqState.getHttpServletRequest());
        if (session != null) {
            int count = RTConfigContextListener.GetSessionCount(session.getServletContext(),
                new RTConfigContextListener.HttpSessionFilter() {
                    public boolean countSession(HttpSession session) {
                        String acctID = (String)AttributeTools.getSessionAttribute(session,Constants.PARM_ACCOUNT,null);
                        if (!StringTools.isBlank(acctID)) {
                            java.util.List<String> userList = acctLoginMap.get(acctID);
                            if (userList == null) {
                                userList = new Vector<String>();
                                acctLoginMap.put(acctID,userList);
                            }
                            String userID = (String)AttributeTools.getSessionAttribute(session,Constants.PARM_USER,null);
                            if (!StringTools.isBlank(userID)) {
                                userList.add(userID);
                            } else {
                                userID = "?";
                            }
                            Print.logInfo("Logged-in User: %s,%s", acctID, userID);
                            return true;
                        }
                        return false;
                    }
                }
            );
        }
        return acctLoginMap;
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final String   dtFormat      = privLabel.getDateFormat() + " " + privLabel.getTimeFormat();
        final I18N     i18n          = privLabel.getI18N(SysAdminAccounts.class);
        final Locale   locale        = reqState.getLocale();
        final Account  currAcct      = reqState.getCurrentAccount(); // never null
        final String   currAcctID    = reqState.getCurrentAccountID();
        final String   currAcctTZID  = currAcct.getTimeZone();
        final TimeZone currAcctTZ    = currAcct.getTimeZone(null);
        final boolean  isSysAdmin    = Account.isSystemAdmin(currAcct); // all access
        final boolean  isAccountMgr  = currAcct.isAccountManager();
        final User     currUser      = reqState.getCurrentUser(); // may be null
        final String   currUserID    = reqState.getCurrentUserID();
        final String   pageName      = this.getPageName();
        final boolean  accountProps  = privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_accountProperties,false);
        String  m     = pageMsg;
        boolean error = false;

        /* account manager */
        final boolean  hasAccountMgr = Account.SupportsAccountManager();
        final boolean  showAccountManager = hasAccountMgr &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showAccountManager,false);
        final boolean  showRetainEvents = 
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showRetainEvents,false);
        final boolean  showAllowWebService = 
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showAllowWebService,false);
        final boolean  showEnableDriverELogs = DBConfig.hasELogPackage() && 
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showEnableDriverELogs,true);
        final boolean  showAutoAddDevices = 
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showAutoAddDevices,false);
        final boolean  showExpiration = isSysAdmin &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showExpiration,false);
        final boolean  showSuspension = isSysAdmin &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showSuspension,false);
        final boolean  showSmtpProperties = isSysAdmin &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showSmtpProperties,false);
        final boolean  showMaxPingCount = DBConfig.hasExtraPackage();

        /* data push url */
        final boolean  showDataPushURL = 
            privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showDataPushURL,false) &&
            Account.SupportsDataPushURL();

        /* invalid user? */
        if (!isAccountMgr && !isSysAdmin) {
            Print.logWarn("Current user is neither a SysAdmin, nor Account Manager! ==> " + currAcctID);
            // access will be restricted below
        }

        /* list of authorized accounts */
        Collection<String> accountList = null;
        try {
            accountList = Account.getAuthorizedAccounts(currAcct);
        } catch (DBException dbe) {
            Print.logError("Error reading authorized Accounts");
        }
        if (ListTools.isEmpty(accountList)) {
            accountList = new Vector<String>();
            accountList.add(currAcctID);
        }
        // 'accountList' has at least one element in it.

        /* selected account-id */
        String selAccountID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_ACCOUNT_SELECT, "");
        if (StringTools.isBlank(selAccountID)) {
            selAccountID = ListTools.itemAt(accountList, 0, "");
        }
        if (!ListTools.contains(accountList,selAccountID)) {
            // Authorized account list does not contain the selected account
            selAccountID = currAcctID;
        }
        final boolean isCurrentAccountSelected = selAccountID.equals(currAcctID);

        /* account db */
        Account selAccount = null;
        try {
            selAccount = !StringTools.isBlank(selAccountID)? Account.getAccount(selAccountID) : null; // may still be null
        } catch (DBException dbe) {
            // ignore
        }

        /* command */
        String  accountCmd    = reqState.getCommandName();
        boolean listAccounts  = false;
        boolean updateAccount = accountCmd.equals(COMMAND_INFO_UPDATE);
        boolean selectAccount = accountCmd.equals(COMMAND_INFO_SELECT);
        boolean newAccount    = accountCmd.equals(COMMAND_INFO_NEW);
        boolean deleteAccount = false;
        boolean editAccount   = false;
        boolean viewAccount   = false;
        boolean loginAccount  = false;

        /* submit buttons */
        String  submitEdit    = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT , "");
        String  submitView    = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW , "");
        String  submitChange  = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG  , "");
        String  submitNew     = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW  , "");
        String  submitDelete  = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL  , "");
        String  submitLogin   = AttributeTools.getRequestString(request, PARM_SUBMIT_LOGIN, "");

        /* CACHE_ACL: ACL allow edit/view */
        boolean allowNew     = (isAccountMgr || isSysAdmin) && privLabel.hasAllAccess(currUser,this.getAclName());
        boolean allowDelete  = allowNew  && privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_allowDeleteAccount,true);
        boolean allowEdit    = allowNew  || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView    = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());
        boolean allowLogin   = allowEdit && privLabel.isSystemAccountsLoginEnabled(currAcctID);
        /*
        boolean allowNew     = isAccountMgr || isSysAdmin;
        boolean allowDelete  = allowNew;  // 'delete' allowed if 'new' allowed
        boolean allowEdit    = allowNew || (isAccountMgr || isSysAdmin);
        boolean allowView    = true;
        boolean allowLogin   = allowEdit && privLabel.isSystemAccountsLoginEnabled(currAcctID);
        */

        /* sub-command */
        String newAccountID = null;
        if (newAccount) {
            if (!allowNew) {
                newAccount = false; // not authorized
            } else {
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newAccountID = AttributeTools.getRequestString(httpReq,PARM_NEW_NAME,"").trim();
                newAccountID = newAccountID.toLowerCase();
                if (StringTools.isBlank(newAccountID)) {
                    m = i18n.getString("SysAdminAccounts.enterNewAccount","Please enter a new Account name."); // UserErrMsg
                    error = true;
                    newAccount = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_SysAdminAccounts_validateNewIDs,*/newAccountID)) {
                    m = i18n.getString("SysAdminAccounts.invalidIDChar","ID contains invalid characters"); // UserErrMsg
                    error = true;
                    newAccount = false;
                }
            }
        } else
        if (updateAccount) {
            if (!allowEdit) {
                // not authorized to update users
                updateAccount = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("SysAdminAccounts.change","Change"))) {
                updateAccount = false;
            }
        } else
        if (selectAccount) {
            if (SubmitMatch(submitLogin,i18n.getString("SysAdminAccounts.login","Login"))) {
                if (allowLogin) {
                    if (selAccount == null) {
                        m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account"); // UserErrMsg
                        error = true;
                        listAccounts = true;
                    } else
                    if (isCurrentAccountSelected) {
                        m = i18n.getString("SysAdminAccounts.alreadyLoggedInToAccount","Already Logged-In to this Account"); // UserErrMsg
                        error = true;
                        listAccounts = true;
                    } else {
                        loginAccount = true;
                    }
                }
            } else
            if (SubmitMatch(submitDelete,i18n.getString("SysAdminAccounts.delete","Delete"))) {
                if (allowDelete) {
                    if (selAccount == null) {
                        m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account"); // UserErrMsg
                        error = true;
                        listAccounts = true;
                    } else
                    if (isCurrentAccountSelected) {
                        m = i18n.getString("SysAdminAccounts.cannotDeleteCurrentAccount","Cannot delete current logged-in Account"); // UserErrMsg
                        error = true;
                        listAccounts = true;
                    } else {
                        deleteAccount = true;
                    }
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("SysAdminAccounts.edit","Edit"))) {
                if (allowEdit) {
                    if (selAccount == null) {
                        m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account"); // UserErrMsg
                        error = true;
                        listAccounts = true;
                    } else {
                        editAccount = !isCurrentAccountSelected;
                        viewAccount = true;
                    }
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("SysAdminAccounts.view","View"))) {
                if (allowView) {
                    if (selAccount == null) {
                        m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account"); // UserErrMsg
                        error = true;
                        listAccounts = true;
                    } else {
                        viewAccount = true;
                    }
                }
            } else {
                listAccounts = true;
            }
        } else {
            listAccounts = true;
        }

        /* login to account? */
        if (loginAccount) {
            if (selAccount == null) {
                m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account"); // UserErrMsg
                error = true;
            } else {
                try {
                    String loginAcctID = selAccount.getAccountID();
                    String loginUserID = User.getAdminUserID();
                    User   loginUser   = User.getUser(selAccount, loginUserID);
                    String loginPasswd = (loginUser != null)? loginUser.getDecodedPassword(null) : selAccount.getDecodedPassword(null);
                    String passcode    = privLabel.getSystemAccountsLoginPasscode(currAcctID);
                    URIArg url = new URIArg(reqState.getBaseURI());
                    url.addArg(Constants.PARM_ACCOUNT        , loginAcctID);
                    url.addArg(Constants.PARM_USER           , loginUserID);
                    url.addArg(Constants.PARM_PASSWORD       , StringTools.blankDefault(loginPasswd,""));
                    url.addArg(CommonServlet.PARM_PAGE       , Constants.PAGE_MENU_TOP);
                    url.addArg(Constants.PARM_SA_RELOGIN_PASS, passcode);
                    Print.logInfo("ReLogin URL: " + url);
                    AttributeTools.clearSessionAttributes(request); // invalidate/logout
                    AttributeTools.setSessionLong(     request, Constants.PARM_SA_RELOGIN_SESS, DateTime.getCurrentTimeSec());
                    AttributeTools.setSessionAttribute(request, Constants.PARM_SA_RELOGIN_ACCT, currAcctID);
                    AttributeTools.setSessionAttribute(request, Constants.PARM_SA_RELOGIN_USER, currUserID);
                    HttpServletResponse response = reqState.getHttpServletResponse();
                    RequestDispatcher rd = request.getRequestDispatcher(url.toString());
                    rd.forward(request, response);
                    return;
                } catch (Throwable th) {
                    m = i18n.getString("SysAdminAccounts.errorDuringLoginDispatch","Error ocurred during dispatch to login"); // UserErrMsg
                    error = true;
                }
            }
            listAccounts = true;
        }

        /* delete account? */
        if (deleteAccount) {
            if (selAccount == null) {
                m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account"); // UserErrMsg
                error = true;
            } else {
                try {
                    Account.Key accountKey = (Account.Key)selAccount.getRecordKey();
                    Print.logWarn("Deleting Account: " + accountKey);
                    accountKey.delete(true); // will also delete dependencies
                    accountList = Account.getAuthorizedAccounts(currAcct);
                    selAccountID = ListTools.itemAt(accountList, 0, "");
                    try {
                        selAccount = !selAccountID.equals("")? Account.getAccount(selAccountID) : null; // may still be null
                    } catch (DBException dbe) {
                        selAccount = null;
                    }
                } catch (DBException dbe) {
                    Print.logException("Unable to delete account: " + selAccount.getAccountID(), dbe);
                    m = i18n.getString("SysAdminAccounts.errorDelete","Internal error deleting Account"); // UserErrMsg
                    error = true;
                }
            }
            listAccounts = true;
        }

        /* new account? */
        if (newAccount) {
            boolean createAccountOK = true;
            try {
                if (Account.exists(newAccountID)) {
                    m = i18n.getString("SysAdminAccounts.alreadyExists","This Account already exists"); // UserErrMsg
                    error = true;
                    createAccountOK = false;
                }
            } catch (DBException dbe) {
                m = i18n.getString("SysAdminAccounts.accountError","Error checking account"); // UserErrMsg
                error = true;
                createAccountOK = false;
            }
            if (createAccountOK) {
                try {
                    String newPasswd = null;
                    Account account  = Account.createNewAccount(currAcct, newAccountID, newPasswd); // saved
                    accountList      = Account.getAuthorizedAccounts(currAcct);
                    selAccount       = account;
                    selAccountID     = account.getAccountID();
                    m = i18n.getString("SysAdminAccounts.createdAccount","New Account has been created"); // UserErrMsg
                } catch (DBNotAuthorizedException dbaee) {
                    m = i18n.getString("SysAdminAccounts.notAuthorized","Not authorized to create account"); // UserErrMsg
                    error = true;
                } catch (DBAlreadyExistsException dbaee) {
                    m = i18n.getString("SysAdminAccounts.alreadyExists","This Account already exists"); // UserErrMsg
                    error = true;
                } catch (DBException dbe) {
                    m = i18n.getString("SysAdminAccounts.errorCreate","Internal error creating Account"); // UserErrMsg
                    error = true;
                }
            }
            listAccounts = true;
        }

        /* change/update the account info? */
        if (updateAccount) {
            if (selAccount == null) {
                m = i18n.getString("SysAdminAccounts.noAccounts","There are currently no defined Accounts."); // UserErrMsg
            } else {
                String  acctDesc      = AttributeTools.getRequestString(request, PARM_ACCT_DESC          , "");
                String  acctActive    = AttributeTools.getRequestString(request, PARM_ACCT_ACTIVE        , "");
                String  acctPassword  = AttributeTools.getRequestString(request, PARM_ACCT_PASSWORD      , "");
                String  contactName   = AttributeTools.getRequestString(request, PARM_ACCT_CONTACT_NAME  , "");
                String  contactPhone  = AttributeTools.getRequestString(request, PARM_ACCT_CONTACT_PHONE , "");
                String  contactEmail  = AttributeTools.getRequestString(request, PARM_ACCT_CONTACT_EMAIL , "");
                String  acctTimeZone  = AttributeTools.getRequestString(request, PARM_ACCT_TIMEZONE      , "");
                String  acctAutoDev   = AttributeTools.getRequestString(request, PARM_ACCT_AUTODEV       , "");
                String  acctMaxDev    = AttributeTools.getRequestString(request, PARM_ACCT_MAXDEV        , "");
                String  acctExpired   = AttributeTools.getRequestString(request, PARM_ACCT_EXPIRE        , "");
                String  acctSuspended = AttributeTools.getRequestString(request, PARM_ACCT_SUSPEND       , "");
                String  acctIsManager = AttributeTools.getRequestString(request, PARM_ACCT_IS_MANAGER    , "");
                String  acctManagerID = AttributeTools.getRequestString(request, PARM_ACCT_MANAGER_ID    , "");
                String  acctPrivLabel = AttributeTools.getRequestString(request, PARM_ACCT_PRIVLABEL     , "<n/a>");
                String  acctGeoMode   = AttributeTools.getRequestString(request, PARM_ACCT_RG_MODE       , "");
                String  dcsPropsID    = AttributeTools.getRequestString(request, PARM_ACCT_DCS_PROPS_ID  , "");
                String  isBCrossStr   = AttributeTools.getRequestString(request, PARM_ACCT_IS_BCROSS     , "");
                int     maxPingCount  = AttributeTools.getRequestInt(   request, PARM_ACCT_MAX_PING      ,  0);
                boolean totPingReset  = !StringTools.isBlank(AttributeTools.getRequestString(request,PARM_ACCT_PING_RESET,null));
                String  dataPushURL   = AttributeTools.getRequestString(request, PARM_ACCT_DATA_PUSH_URL , "");
                String  noteText      = AttributeTools.getRequestString(request, PARM_ACCT_NOTES         , "");
                String  ruleAllowStr  = AttributeTools.getRequestString(request, PARM_ACCT_RULE_ALLOW    , null);
                String  smsEnabledStr = AttributeTools.getRequestString(request, PARM_ACCT_SMS_ENABLED   , "");
                String  smsProps      = AttributeTools.getRequestString(request, PARM_ACCT_SMS_PROPS     , "");
                String  retainEvStr   = AttributeTools.getRequestString(request, PARM_ACCT_RETAIN_EVENTS , "");
                String  wsAllowStr    = AttributeTools.getRequestString(request, PARM_ACCT_WEB_SERVICE   , "");
                String  elogEnableStr = AttributeTools.getRequestString(request, PARM_ACCT_ELOG_ENABLE   , "");
                String  elogAccount   = AttributeTools.getRequestString(request, PARM_ACCT_ELOG_ACCOUNT  , "");
                String  elogUser      = AttributeTools.getRequestString(request, PARM_ACCT_ELOG_USER     , "");
                String  elogPassword  = AttributeTools.getRequestString(request, PARM_ACCT_ELOG_PASSWORD , "");
                String  smtpHost      = AttributeTools.getRequestString(request, PARM_SMTP_HOST          , "");
                String  smtpPort      = AttributeTools.getRequestString(request, PARM_SMTP_PORT          , "");
                String  smtpUser      = AttributeTools.getRequestString(request, PARM_SMTP_USER          , "");
                String  smtpEmail     = AttributeTools.getRequestString(request, PARM_SMTP_EMAIL         , "");
                String  smtpPass      = AttributeTools.getRequestString(request, PARM_SMTP_PASS          , "");
                String  smtpSSL       = AttributeTools.getRequestString(request, PARM_SMTP_SSL           , "");
                String  smtpTLS       = AttributeTools.getRequestString(request, PARM_SMTP_TLS           , "");
                User    adminUser     = null;
                listAccounts = true;
                // -- sms default enabled?
                switch (Account.GetDefaultSmsEnabledState()) {
                    case FALSE:
                        smsEnabledStr = "false";
                        //smsProps = "";
                        break;
                    case TRUE:
                        smsEnabledStr = "true";
                        break;
                    case ACCOUNT:
                        // -- leave as is
                        break;
                }
                // -- update
                try {
                    boolean saveOK = true;
                    // -- active
                    if (isCurrentAccountSelected) {
                        if (!selAccount.getIsActive()) {
                            selAccount.setIsActive(true);
                        }
                    } else {
                        boolean acctActv = ComboOption.parseYesNoText(locale, acctActive, true);
                        if (selAccount.getIsActive() != acctActv) { 
                            selAccount.setIsActive(acctActv); 
                        }
                    }
                    // -- password
                    if (!isCurrentAccountSelected) {
                        if (acctPassword.equals(PASSWORD_HOLDER)) {
                            // -- password not entered
                        } else
                        if (this.isValidPassword(acctPassword)) {
                            try {
                                adminUser = User.getUser(selAccount, User.getAdminUserID());
                            } catch (DBException dbe) {
                                // -- ignore
                            }
                            if (adminUser != null) {
                                // -- "admin" user found, update both account and user passwords
                                adminUser.setDecodedPassword(null, acctPassword, true); // temporary password
                                selAccount.setDecodedPassword(null, acctPassword, false);
                            } else {
                                // -- no "admin" user, update account password only
                                selAccount.setDecodedPassword(null, acctPassword, true); // temporary password
                            }
                        } else {
                            m = i18n.getString("SysAdminAccounts.pleaseEnterValidPassword","Please enter a valid password"); // UserErrMsg
                            error = true;
                            saveOK = false;
                            editAccount  = true;
                            listAccounts = false;
                        }
                    }
                    // -- description
                    if (!acctDesc.equals("")) {
                        selAccount.setDescription(acctDesc);
                    }
                    // -- contact name
                    if (!contactName.equals(selAccount.getContactName())) {
                        selAccount.setContactName(contactName);
                    }
                    // -- contact phone
                    if (!contactPhone.equals(selAccount.getContactPhone())) {
                        selAccount.setContactPhone(contactPhone);
                    }
                    // -- contact email
                    if (!contactEmail.equals(selAccount.getContactEmail())) {
                        selAccount.setContactEmail(contactEmail);
                    }
                    // -- Timezone
                    if (!acctTimeZone.equals(selAccount.getTimeZone())) {
                        selAccount.setTimeZone(acctTimeZone);
                    }
                    // -- isAccountManager/ManagerID
                    if (showAccountManager && !isCurrentAccountSelected && isSysAdmin) {
                        boolean isManager = false;
                        if (!StringTools.isBlank(acctIsManager)) {
                            isManager = ComboOption.parseYesNoText(locale, acctIsManager, false);
                            if (selAccount.getIsAccountManager() != isManager) { 
                                selAccount.setIsAccountManager(isManager); 
                            }
                        }
                        String oldMgrID = selAccount.getManagerID();
                        if (isManager) {
                            String newMgrID = !StringTools.isBlank(acctManagerID)? 
                                acctManagerID : selAccount.getAccountID(); // default to AccountID
                            if (StringTools.isBlank(oldMgrID)) {
                                selAccount.setManagerID(newMgrID); 
                            } else
                            if (!oldMgrID.equals(newMgrID)) { 
                                Print.logWarn("Changing 'ManagerID': " + oldMgrID + " ==> " + newMgrID);
                                selAccount.setManagerID(newMgrID); 
                            }
                        } else {
                            //if (!StringTools.isBlank(oldMgrID)) { // removed [2.5.8-B62]
                            //    selAccount.setManagerID(""); // clear (not a manager)
                            //}
                            String newMgrID = acctManagerID;
                            if (!oldMgrID.equals(newMgrID)) { // [2.5.8-B62]
                                Print.logWarn("Changing 'ManagerID': " + oldMgrID + " ==> " + newMgrID);
                                selAccount.setManagerID(newMgrID);
                            }
                        }
                    }
                    // -- showRetainEvents
                    if (showRetainEvents && isSysAdmin) {
                        boolean retainEvents = ComboOption.parseYesNoText(locale, retainEvStr, false);
                        if (retainEvents) {
                            if (!selAccount.hasRetainedEventAge()) {
                                long retainSec = 99999999L; // DateTime.YearSecods(1) * 20L;
                                selAccount.setRetainedEventAge(retainSec); // 1981/07/17,07:00:00,GMT
                            } else {
                                // leave existing retained-age-sec as-is
                            }
                        } else {
                            selAccount.setRetainedEventAge(0L);
                        }
                    }
                    // -- private label name
                    if (isSysAdmin) {
                        if (!acctPrivLabel.equals("<n/a>")) {
                            selAccount.setPrivateLabelName(acctPrivLabel);
                        }
                    }
                    // -- reverse-geocoder mode
                    if (isSysAdmin) {
                        selAccount.setGeocoderMode(acctGeoMode, locale);
                    }
                    // -- DCS properties ID
                    if (isSysAdmin && !selAccount.getDcsPropertiesID().equals(dcsPropsID)) {
                        selAccount.setDcsPropertiesID(dcsPropsID);
                    }
                    // -- Allow Notification
                    if (isSysAdmin) {
                        boolean allowNotify = ComboOption.parseYesNoText(locale, ruleAllowStr, true);
                        if (selAccount.getAllowNotify() != allowNotify) { 
                            selAccount.setAllowNotify(allowNotify); 
                        }
                    }
                    // -- SMS
                    if (isSysAdmin) {
                        boolean smsEnabled = ComboOption.parseYesNoText(locale, smsEnabledStr, false);
                        if (selAccount.getSmsEnabled() != smsEnabled) {
                            selAccount.setSmsEnabled(smsEnabled);
                        }
                        if (!selAccount.getSmsProperties().equals(smsProps)) {
                            selAccount.setSmsProperties(smsProps);
                        }
                    }
                    // -- border crossing
                    if (Account.SupportsBorderCrossing()) {
                        boolean isBCross = ComboOption.parseYesNoText(locale, isBCrossStr, false);
                        if (selAccount.getIsBorderCrossing() != isBCross) { 
                            selAccount.setIsBorderCrossing(isBCross); 
                        }
                    }
                    // -- auto-add devices
                    if (showAutoAddDevices && isSysAdmin) {
                        boolean autoAddDev = ComboOption.parseYesNoText(locale, acctAutoDev, false);
                        if (selAccount.getAutoAddDevices() != autoAddDev) { 
                            selAccount.setAutoAddDevices(autoAddDev); 
                        }
                    }
                    // -- maximum allowed devices
                    if (!StringTools.isBlank(acctMaxDev)) {
                        long maxCnt = acctMaxDev.equalsIgnoreCase("n/a")? 0 : StringTools.parseLong(acctMaxDev,0L);
                        selAccount.setMaximumDevices(maxCnt);
                    }
                    // -- maximum allowed Commands/Locates
                    if (showMaxPingCount && (maxPingCount != selAccount.getMaxPingCount())) {
                        selAccount.setMaxPingCount((maxPingCount >= 0)? maxPingCount : 0);
                    }
                    if (showMaxPingCount && totPingReset) {
                        selAccount.resetTotalPingCount(false);
                    }
                    // -- expiration
                    if (isSysAdmin && showExpiration) {
                        if (!StringTools.isBlank(acctExpired)) {
                            try {
                                TimeZone tmz = selAccount.getTimeZone(null);
                                DateTime dt  = DateTime.parseArgumentDate(acctExpired,tmz,DateTime.DefaultParsedTime.DayEnd);
                                long     ts  = dt.getTimeSec();
                                selAccount.setExpirationTime(ts);
                            } catch (DateTime.DateParseException dpe) {
                                // -- invalid date/time format, leave as-is
                            }
                        } else {
                            selAccount.setExpirationTime(0L);
                        }
                    }
                    // -- suspension
                    if (isSysAdmin && showSuspension) {
                        if (!StringTools.isBlank(acctSuspended)) {
                            try {
                                TimeZone tmz = selAccount.getTimeZone(null);
                                DateTime dt  = DateTime.parseArgumentDate(acctSuspended,tmz,DateTime.DefaultParsedTime.DayEnd);
                                long     ts  = dt.getTimeSec();
                                selAccount.setSuspendUntilTime(ts);
                            } catch (DateTime.DateParseException dpe) {
                                // -- invalid date/time format, leave as-is
                            }
                        } else {
                            selAccount.setSuspendUntilTime(0L);
                        }
                    }
                    // -- datsPush URL
                    if (isSysAdmin && showDataPushURL) {
                        if (!selAccount.getDataPushURL().equals(dataPushURL)) {
                            selAccount.setDataPushURL(dataPushURL);
                        }
                    }
                    // -- Notes
                    boolean notesOK = isSysAdmin && // !isAccountMgr
                        privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showNotes,SHOW_NOTES);
                    if (notesOK && !selAccount.getNotes().equals(noteText)) {
                        selAccount.setNotes(noteText);
                    }
                    // -- save
                    if (saveOK) {
                        if (adminUser != null) {
                            try {
                                adminUser.updatePasswordFields(); // now save
                            } catch (DBException dbe) {
                                Print.logError("Error saving 'admin' User password", dbe);
                            }
                        }
                        if (!isSysAdmin) {
                            selAccount.addExcludedUpdateFields(
                                Account.FLD_isAccountManager,
                                Account.FLD_managerID
                                );
                        }
                        selAccount.save();
                        if (accountProps) {
                            String acctTempProps = AttributeTools.getRequestString(request, PARM_ACCT_TEMP_PROPS, "");
                            acctTempProps = (new RTProperties(acctTempProps.replace('\n',' '))).toString();
                            Resource.updateTemporaryProperties(selAccount, acctTempProps);
                        } // accountProps
                        m = i18n.getString("SysAdminAccounts.accountUpdated","Account information updated"); // UserErrMsg
                    } else {
                        // -- should stay on this page
                        editAccount  = !isCurrentAccountSelected;
                        listAccounts = false;
                    }
                } catch (Throwable t) {
                    m = i18n.getString("SysAdminAccounts.errorUpdate","Internal error updating Account"); // UserErrMsg
                    error = true;
                }
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = SysAdminAccounts.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "SysAdminAccounts.css", cssDir);
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
        final Collection<String> _accountList = accountList;
        final String  _selAccountID  = selAccountID;
        final Account _selAccount    = selAccount;
        final boolean _allowEdit     = allowEdit;
        final boolean _allowView     = allowView;
        final boolean _allowNew      = allowNew;
        final boolean _allowDelete   = allowDelete;
        final boolean _allowLogin    = allowLogin;
        final boolean _editAccount   = _allowEdit && editAccount;
        final boolean _viewAccount   = _editAccount || viewAccount;
        final boolean _listAccounts  = listAccounts;
        final ComboMap _rgList       = privLabel.getEnumComboMap(Account.GeocoderMode.class);
        final ComboMap _tzList       = privLabel.getTimeZoneComboMap();
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = SysAdminAccounts.this.getPageName();
                boolean notesOK = isSysAdmin && // !isAccountMgr
                    privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showNotes,SHOW_NOTES);

                // frame header
              //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String editURL    = SysAdminAccounts.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String selectURL  = SysAdminAccounts.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String newURL     = SysAdminAccounts.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String frameTitle = _allowNew? 
                    i18n.getString("SysAdminAccounts.createDeleteAccounts","Create/Delete/Edit Accounts") : 
                    i18n.getString("SysAdminAccounts.viewEditAccounts","View/Edit Accounts");
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                out.write("<hr>\n");

                // account selection table (Select, Account ID, Account Description)
                if (_listAccounts) {

                    // account selection table (Select, Account ID, Account Description)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("SysAdminAccounts.selectAccount","Select an Account")+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_ACCOUNT_SELECT+"' method='post' action='"+selectURL+"' target='_self'>"); // target='_top'
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+filter(i18n.getString("SysAdminAccounts.select","Select"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.accountID","Account ID"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.accountName","Account Description"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.active","Active"))+"</th>\n");
                    if (hasAccountMgr && (isSysAdmin || isAccountMgr)) {
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.manager","Manager"))+"</th>\n");
                    }
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.deviceCount","Device\nCount"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.privateLabel","PrivateLabel\nName"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.created","Created\n{0}",currAcctTZID))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.lastLogin","Last Login\n{0}",currAcctTZID))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.loggedIn","Logged\nIn Now"))+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    Map<String,java.util.List<String>> loggedInAccounts = SysAdminAccounts.this.getLoggedInAccounts(reqState);
                    for (int u = 0; u < ListTools.size(_accountList); u++) {
                        // -- get Account
                        Account acct = null;
                        try {
                            acct = Account.getAccount(ListTools.itemAt(_accountList,u,""));
                        } catch (DBException dbe) {
                            // 
                        }
                        if (acct == null) {
                            continue;
                        }
                        String acctID       = acct.getAccountID();
                        String acctDesc     = acct.getDescription();
                        String prvLabelName = acct.getPrivateLabelName();
                        //if (!prvLabelName.equals("*")) { continue; } // <-- debug/testing
                        // -- odd/even row
                        boolean oddRow = ((u & 1) == 0); // odd row index starts at '0'
                        if (oddRow) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        // display account info
                        String active       = ComboOption.getYesNoText(locale,acct.isActive());
                        long creationTime   = acct.getCreationTime();
                        String creationStr  = (creationTime > 0L)? new DateTime(creationTime,currAcctTZ).format(dtFormat) : i18n.getString("SysAdminAccounts.unknown","unknown");
                        long lastLoginTime  = acct.getLastLoginTime();
                        long deltaTimeSec   = DateTime.getCurrentTimeSec() - lastLoginTime;
                        String lastLoginStr = (lastLoginTime > 0L)? new DateTime(lastLoginTime,currAcctTZ).format(dtFormat) : i18n.getString("SysAdminAccounts.never","never");
                        String lastLoginCls = oddRow? "normalLoginDate_odd" : "normalLoginDate_even";
                        if (deltaTimeSec <= DateTime.DaySeconds(1)) {
                            // has logged i within the last 24 hours (green)
                            lastLoginCls = oddRow? "recentLoginDate_odd" : "recentLoginDate_even";
                        } else
                        if (deltaTimeSec <= DateTime.DaySeconds(7)) {
                            // has logged i within the last week (black)
                            lastLoginCls = oddRow? "normalLoginDate_odd" : "normalLoginDate_even";
                        } else
                        if (deltaTimeSec <= DateTime.DaySeconds(21)) {
                            // has logged i within the last 3 weeks (yellow)
                            lastLoginCls = oddRow? "oldLoginDate_odd" : "oldLoginDate_even";
                        } else {
                            // logged in more than 3 weeks ago (red)
                            lastLoginCls = oddRow? "veryOldLoginDate_odd" : "veryOldLoginDate_even"; // (196, 54, 54)
                        }
                        String deviceCountS = String.valueOf(acct.getDeviceCount());
                        int    loginCount   = 0;
                        String loginCountS  = "--"; // ComboOption.getYesNoText(locale,false);
                        if (loggedInAccounts.containsKey(acctID)) {
                            java.util.List<String> userList = loggedInAccounts.get(acctID);
                            loginCount  = userList.size();
                            loginCountS = "(" + loginCount + ")";
                        }
                        //if (prvLabelName.equals("*")) { prvLabelName = "default"; }
                        String checked      = _selAccountID.equals(acctID)? " checked" : "";
                        String viewStyle    = currAcctID.equals(acctID)? "background-color:#E5E5E5;" : "background-color:#FFFFFF;";
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"' style='"+viewStyle+"'><input type='radio' name='"+PARM_ACCOUNT_SELECT+"' id='"+acctID+"' value='"+acctID+"' "+checked+"></td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+acctID+"'>"+filter(acctID)+"</label></td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filter(acctDesc)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filter(active)+"</td>\n");
                        if (hasAccountMgr && (isSysAdmin || isAccountMgr)) {
                            String sortID = "";
                            String mgrID;
                            if (isSysAdmin) {
                                if (acct.isSystemAdmin()) {
                                    mgrID  = "***";
                                    sortID = " ";
                                } else
                                if (acct.isAccountManager()) {
                                    String m = StringTools.blankDefault(acct.getManagerID(),"?");
                                    mgrID  = "*" + m + "*";
                                    sortID = m;
                                } else {
                                    mgrID  = StringTools.blankDefault(acct.getManagerID(),"--");
                                    sortID = mgrID;
                                }
                            } else
                            if (isAccountMgr) {
                                if (acct.isSystemAdmin()) {
                                    mgrID  = ComboOption.getYesNoText(locale,true);
                                    sortID = "1";
                                } else
                                if (acct.isAccountManager()) {
                                    mgrID  = ComboOption.getYesNoText(locale,true);
                                    sortID = "1";
                                } else {
                                    mgrID  = "--";
                                    sortID = "0";
                                }
                            } else {
                                mgrID  = "--";
                                sortID = "0";
                            }
                            out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL+"' "+SORTTABLE_SORTKEY+"='"+sortID+"' nowrap>"+filter(mgrID)+"</td>\n");
                        }
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filter(deviceCountS)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filter(prvLabelName)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' "+SORTTABLE_SORTKEY+"='"+creationTime +"' nowrap>"+filter(creationStr)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' "+SORTTABLE_SORTKEY+"='"+lastLoginTime+"' nowrap><span class='"+lastLoginCls+"'>"+filter(lastLoginStr)+"</span></td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' "+SORTTABLE_SORTKEY+"='"+loginCount   +"' nowrap>"+filter(loginCountS)+"</td>\n");
                        // end of table row
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("SysAdminAccounts.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("SysAdminAccounts.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowLogin) {
                        out.write("<td style='padding-left:30px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_LOGIN+"' value='"+i18n.getString("SysAdminAccounts.login","Login")+"' "+Onclick_ConfirmLogin(locale)+">");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("SysAdminAccounts.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");

                    /* new Account */
                    if (_allowNew) {
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("SysAdminAccounts.createNewAccount","Create a new Account")+":</h1>\n");
                    out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<form name='"+FORM_ACCOUNT_NEW+"' method='post' action='"+newURL+"' target='_self'>");  // target='_top'
                    out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                    out.write(i18n.getString("SysAdminAccounts.accountID","Account ID")+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='32'><br>\n");
                    out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("SysAdminAccounts.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    }

                } else {
                    // user view/edit form

                    /* start of form */
                    out.write("<form name='"+FORM_ACCOUNT_EDIT+"' method='post' action='"+editURL+"' target='_self'>\n"); // target='_top'
                    out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                    /* password */
                    String decodedPass = PASSWORD_HOLDER;
                    boolean showPass = privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showPasswords,false);
                    if (showPass && (_selAccount != null)) {
                        try {
                            User adminUser = User.getUser(_selAccount, User.getAdminUserID());
                            if (adminUser != null) {
                                decodedPass = adminUser.getDecodedPassword(null);
                            } else {
                                decodedPass = _selAccount.getDecodedPassword(null);
                            }
                        } catch (DBException dbe) {
                            decodedPass = _selAccount.getDecodedPassword(null);
                        }
                        if (decodedPass == null) { decodedPass = "?"; }
                    }
                    decodedPass = StringTools.htmlFilterValue(decodedPass);

                    /* Account fields */
                    ComboOption acctActive  = ComboOption.getYesNoOption(locale, ((_selAccount != null) && _selAccount.isActive()));
                    String      acctDesc    = (_selAccount!=null)? _selAccount.getDescription()      : "";
                    ComboOption autoAddDev  = ComboOption.getYesNoOption(locale, ((_selAccount != null) && _selAccount.getAutoAddDevices()));
                    long        acctMaxDev  = (_selAccount!=null)? _selAccount.getMaximumDevices()   :  0;
                    String      contactName = (_selAccount!=null)? _selAccount.getContactName()      : "";
                    String      contactPhone= (_selAccount!=null)? _selAccount.getContactPhone()     : "";
                    String      contactEmail= (_selAccount!=null)? _selAccount.getContactEmail()     : "";
                    String      acctTimeZone= (_selAccount!=null)? _selAccount.getTimeZone()         : Account.GetDefaultTimeZone();
                    String      acctPrivLbl = (_selAccount!=null)? _selAccount.getPrivateLabelName() : "";
                    boolean     editSysAdmin= _editAccount && isSysAdmin; // only editable if sys-admin
                    ComboMap    privLblList = null;
                    if (editSysAdmin) {
                        // editable PrivateLabel
                        privLblList = isSysAdmin? 
                            new ComboMap(BasicPrivateLabelLoader.getPrivateLabelNames(true)) :
                            new ComboMap();
                        if (!ListTools.containsKey(privLblList, acctPrivLbl)) {
                            privLblList.insert(acctPrivLbl);
                        }
                        if (isSysAdmin) {
                            if (!ListTools.containsKey(privLblList, "")) {
                                privLblList.insert("");
                            }
                            if (!ListTools.containsKey(privLblList, "*")) {
                                privLblList.insert("*");
                            }
                        }
                    } else {
                        // non-editable PrivateLabel
                        privLblList = new ComboMap();
                        privLblList.insert(acctPrivLbl);
                    }
                    ComboOption geocoderMode = privLabel.getEnumComboOption(Account.getGeocoderMode(_selAccount));
                    ComboOption isBCross     = ComboOption.getYesNoOption(locale, ((_selAccount != null)? _selAccount.isBorderCrossing() : false));
                    int maxPingCnt    = (_selAccount != null)? _selAccount.getMaxPingCount()    : 0;
                    int totalPingCnt  = (_selAccount != null)? _selAccount.getTotalPingCount()  : 0;
                    String dcsPropsID = (_selAccount != null)? _selAccount.getDcsPropertiesID() : "";

                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");

                    long   createTS     = (_selAccount!=null)? _selAccount.getCreationTime()  : 0L;
                    String createStr    = reqState.formatDateTime(createTS   , "--");
                    long   lastLoginTS  = (_selAccount!=null)? _selAccount.getLastLoginTime() : 0L;
                    String lastLoginStr = reqState.formatDateTime(lastLoginTS, "--");
                    out.println(FormRow_TextField(PARM_ACCOUNT_SELECT    , false        , i18n.getString("SysAdminAccounts.accountID","Account ID")+":"                 , _selAccountID, 40, 40));
                    out.println(FormRow_TextField(PARM_ACCT_CREATED      , false        , i18n.getString("SysAdminAccounts.creationDate","Creation Date")+":"           , createStr, 24, 24));
                    out.println(FormRow_TextField(PARM_ACCT_LAST_LOGIN   , false        , i18n.getString("SysAdminAccounts.lastLoginDate","Last Login Date")+":"         , lastLoginStr, 24, 24));

                    /* description/password */
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextField(PARM_ACCT_DESC         , _editAccount , i18n.getString("SysAdminAccounts.accountDesc","Account Description")+":"      , acctDesc, 40, 40));
                    out.println(FormRow_ComboBox (PARM_ACCT_ACTIVE       , _editAccount , i18n.getString("SysAdminAccounts.isActive","Is Active")+":"                   , acctActive, ComboMap.getYesNoMap(locale), "", -1));
                    out.println(FormRow_TextField(PARM_ACCT_PASSWORD     , _editAccount , i18n.getString("SysAdminAccounts.password","Password")+":"                    , decodedPass, 20, 20));

                    /* contact/default info */
                    out.println(FormRow_Separator());
                    out.println(FormRow_TextField(PARM_ACCT_CONTACT_NAME , _editAccount , i18n.getString("SysAdminAccounts.contactName","Contact Name:")                , contactName, 40, 40));
                    out.println(FormRow_TextField(PARM_ACCT_CONTACT_PHONE, _editAccount , i18n.getString("SysAdminAccounts.contactPhone","Contact Phone:")              , contactPhone, 20, 20));
                    out.println(FormRow_TextField(PARM_ACCT_CONTACT_EMAIL, _editAccount , i18n.getString("SysAdminAccounts.contactEmail","Contact Email:")              , contactEmail, 60, 100));

                    /* initial timezone */
                    out.println(FormRow_ComboBox (PARM_ACCT_TIMEZONE     , _editAccount , i18n.getString("SysAdminAccounts.timeZone","Time Zone:")                      , acctTimeZone, _tzList, null, 20));

                    /* device counts */
                    if (showAutoAddDevices && isSysAdmin) {
                        out.println(FormRow_ComboBox (PARM_ACCT_AUTODEV  , _editAccount , i18n.getString("SysAdminAccounts.autoAddDevices","Allow Auto-Add Devices")+":", autoAddDev, ComboMap.getYesNoMap(locale), "", -1));
                    }
                    out.println(FormRow_TextField(PARM_ACCT_MAXDEV       , _editAccount , i18n.getString("SysAdminAccounts.maxDevices" ,"Maximum Devices")  +":"        , String.valueOf(acctMaxDev),  6, 7, i18n.getString("SysAdminAccounts.enter0ForUnlimited","(Enter '0' for unlimited)")));

                    /* ping count */
                    if (showMaxPingCount) {
                    out.print("<tr>");
                    out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("SysAdminAccounts.maxCommandCount","Max Commands/Locates")+":</td>");
                    out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                    out.print(Form_TextField(PARM_ACCT_MAX_PING  , editSysAdmin, String.valueOf(maxPingCnt)  , 7, 7));
                    out.print(" &nbsp;&nbsp;" + i18n.getString("SysAdminAccounts.totalCommandCount","Total") + ": ");
                    out.print(Form_TextField(PARM_ACCT_TOTAL_PING, false       , String.valueOf(totalPingCnt), 7, 7));
                    if (editSysAdmin) {
                        out.print(" &nbsp;&nbsp;(" + i18n.getString("SysAdminAccounts.pingReset","Check to Reset") + " ");
                        out.print(Form_CheckBox(PARM_ACCT_PING_RESET, PARM_ACCT_PING_RESET, true, false, null, null));
                        out.print(")");
                    }
                    out.print("</td>");
                    out.print("</tr>\n");
                    }

                    /* Account manager */
                    if (showAccountManager && isSysAdmin) {
                        // show account manager fields
                        out.println(FormRow_Separator());
                        ComboOption acctManager = ComboOption.getYesNoOption(locale, ((_selAccount != null) && _selAccount.isAccountManager()));
                        String acctManagerID = (_selAccount != null)? _selAccount.getManagerID() : "";
                        out.println(FormRow_ComboBox (PARM_ACCT_IS_MANAGER, editSysAdmin , i18n.getString("SysAdminAccounts.isManager","Is Account Manager")+":"        , acctManager, ComboMap.getYesNoMap(locale), "", -1));
                        out.println(FormRow_TextField(PARM_ACCT_MANAGER_ID, editSysAdmin , i18n.getString("SysAdminAccounts.managerID","Account Manager ID")+":"        , acctManagerID, 32, 32));
                    }

                    /* Data Push URL */
                    if (showDataPushURL && isSysAdmin) {
                        out.println(FormRow_Separator());
                        String dpURL = (_selAccount != null)? _selAccount.getDataPushURL() : "";
                        out.println(FormRow_TextField(PARM_ACCT_DATA_PUSH_URL, editSysAdmin, i18n.getString("SysAdminAccounts.dataPushURL","Data Push URL")+":"         , dpURL, 100, 200));
                    }

                    /* privateLabel, geocoderMode, isBorderCrossing */
                    out.println(FormRow_Separator());
                    out.println(FormRow_ComboBox (PARM_ACCT_PRIVLABEL , editSysAdmin , i18n.getString("SysAdminAccounts.privateLabelName","PrivateLabel Name")+":"      , acctPrivLbl , privLblList, "", -1));
                    out.println(FormRow_ComboBox (PARM_ACCT_RG_MODE   , editSysAdmin , i18n.getString("SysAdminAccounts.geocoderMode","Geocoder Mode:")                 , geocoderMode, _rgList, null, 10));

                    /* DCS Properties ID */
                    if (isSysAdmin) {
                        // -- "DCS Properties ID"
                        out.println(FormRow_TextField(PARM_ACCT_DCS_PROPS_ID, editSysAdmin, i18n.getString("SysAdminAccounts.dcsPropertiesID","DCS Properties ID")+":"  , dcsPropsID  , 32,  32));
                    }

                    /* Account allow notify */
                    if (isSysAdmin && RTConfig.getBoolean(DBConfig.PROP_Device_checkAccountAllowNotify,false)) {
                        // -- "Notify Enable":
                        // -  Not needed if the device "getAllowNotify(true)" doesn't check this value
                        ComboOption allowNotify = ComboOption.getYesNoOption(locale, ((_selAccount != null)? _selAccount.getAllowNotify() : false));
                        out.println(FormRow_ComboBox(PARM_ACCT_RULE_ALLOW, editSysAdmin, i18n.getString("SysAdminAccounts.notifyAllow","Notify Enable")+":"             , allowNotify , ComboMap.getYesNoMap(locale), "", -1));
                    }

                    /* SMS */
                    if (isSysAdmin) {
                        ComboOption smsEnabled = ComboOption.getYesNoOption(locale, ((_selAccount != null)? _selAccount.getSmsEnabled() : false));
                        String      smsProps   = (_selAccount != null)? _selAccount.getSmsProperties() : "";
                        Account.SMSDefaultState smsState = Account.GetDefaultSmsEnabledState();
                        if (smsState.equals(Account.SMSDefaultState.ACCOUNT)) {
                        // -- "Enable SMS": display only if Account selectable
                        out.println(FormRow_ComboBox (PARM_ACCT_SMS_ENABLED , editSysAdmin, i18n.getString("SysAdminAccounts.smsEnabled","Enable SMS")+":"              , smsEnabled  , ComboMap.getYesNoMap(locale), "", -1));
                        }
                        if (!smsState.equals(Account.SMSDefaultState.FALSE)) {
                        //-- "SMS Properties": display if TRUE or ACCOUNT
                        out.println(FormRow_TextField(PARM_ACCT_SMS_PROPS   , editSysAdmin, i18n.getString("SysAdminAccounts.smsProperties","SMS Properties")+":"       , smsProps    , 80, 130));
                        }
                    }

                    /* retain events */
                    if (showRetainEvents && isSysAdmin) {
                        // -- "Retain Events":
                        ComboOption retainEvents = ComboOption.getYesNoOption(locale, ((_selAccount != null) && _selAccount.hasRetainedEventAge()));
                        out.println(FormRow_ComboBox(PARM_ACCT_RETAIN_EVENTS, editSysAdmin, i18n.getString("SysAdminAccounts.retainEvents","Retain Events")+":"        , retainEvents, ComboMap.getYesNoMap(locale), "", -1));
                    }

                    /* account properties */
                    if (accountProps) {
                        RTProperties resRTP = (_selAccount != null)? Resource.getTemporaryProperties(_selAccount) : null;
                        String acctTempProps = (resRTP != null)? resRTP.toString(null,null,"") : "";
                        out.println(FormRow_TextArea(PARM_ACCT_TEMP_PROPS, _editAccount , i18n.getString("SysAdminAccounts.accountProperties" ,"Account Properties")+":", acctTempProps, 7, 75));
                    }

                    /* account expiration/suspension */
                    if (isSysAdmin && (showExpiration || showSuspension)) {
                        String dateFmt = "yyyy/MM/dd,HH:mm:ss,z";
                        TimeZone tmz = Account.getTimeZone(_selAccount,DateTime.GMT);
                        out.println(FormRow_Separator());
                        if (showExpiration) {
                            long     expireTime = (_selAccount != null)? _selAccount.getExpirationTime() : 0L;
                            DateTime expireDT   = (expireTime > 0L)? new DateTime(expireTime,tmz) : null;
                            String   expireFmt  = (expireDT != null)? expireDT.format(dateFmt) : "";
                            out.println(FormRow_TextField(PARM_ACCT_EXPIRE , _editAccount, i18n.getString("SysAdminAccounts.expiration","Expiration Date/Time")+":", expireFmt, 30, 30));
                        }
                        if (showSuspension) {
                            long     suspendTime = ((_selAccount != null) && _selAccount.isSuspended())? _selAccount.getSuspendUntilTime() : 0L;
                            DateTime suspendDT   = (suspendTime > 0L)? new DateTime(suspendTime,tmz) : null;
                            String   suspendFmt  = (suspendDT != null)? suspendDT.format(dateFmt) : "";
                            out.println(FormRow_TextField(PARM_ACCT_SUSPEND, _editAccount, i18n.getString("SysAdminAccounts.suspension","Suspension Date/Time")+":", suspendFmt, 30, 30));
                        }
                    }

                    /* Notes */
                    if (notesOK) {
                        String noteText = (_selAccount != null)? StringTools.decodeNewline(_selAccount.getNotes()) : "";
                        out.println(FormRow_Separator());
                        out.println(FormRow_TextArea(PARM_ACCT_NOTES, _editAccount, i18n.getString("SysAdminAccounts.notes" ,"General Notes")+":", noteText, 5, 70));
                    }

                    out.println("</table>");

                    /* end of form */
                    out.write("<hr>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editAccount) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("SysAdminAccounts.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("SysAdminAccounts.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("SysAdminAccounts.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
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

    protected String Onclick_ConfirmLogin(Locale locale)
    {
        I18N i18n = I18N.getI18N(SysAdminAccounts.class, locale);
        String confirmLogin = i18n.getString("SysAdminAccounts.confirmLogin",
            "Are you sure you want to login to the selected Account?");
        return "onclick=\"return confirm('"+confirmLogin+"');\"";
    }

    // ------------------------------------------------------------------------
}
