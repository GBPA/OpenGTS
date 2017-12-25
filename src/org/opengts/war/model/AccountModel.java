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
//  2017/01/23  Martin D. Flynn
//     -Initial release (cloned from AccountInfo.java)
// ----------------------------------------------------------------------------
package org.opengts.war.model;

import java.util.Locale;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.Calendar;
import org.opengts.war.track.*;

public class AccountModel
    extends WebPageAdaptor
    implements Constants
{
    
    // ------------------------------------------------------------------------
    // Parameters

    // -- commands
    public  static final String COMMAND_INFO_UPDATE     = "update";

    // -- submit types
    public  static final String PARM_SUBMIT_CHANGE      = "SubmitSave";
    public  static final String PARM_BUTTON_CANCEL      = "ButtonCancel";
    public  static final String PARM_BUTTON_BACK        = "ButtonBack";

    // -- parameters
    public  static final String PARM_ACCOUNT_ID         = "AccountID";
    public  static final String PARM_ACCOUNT_DESC       = "AccountDesc";
    public  static final String PARM_CONTACT_NAME       = "ContactName";
    public  static final String PARM_CONTACT_PHONE      = "ContactPhone";
    public  static final String PARM_CONTACT_EMAIL      = "ContactEMail";
    public  static final String PARM_NOTIFY_EMAIL       = "NotifyEMail";
    public  static final String PARM_TIMEZONE           = "Timezone";
    public  static final String PARM_SPEED_UNITS        = "SpeedUnits";
    public  static final String PARM_DIST_UNITS         = "DistanceUnits";
    public  static final String PARM_VOLM_UNITS         = "VolumeUnits";
    public  static final String PARM_ECON_UNITS         = "EconomyUnits";
    public  static final String PARM_PRESS_UNITS        = "PressureUnits";
    public  static final String PARM_TEMP_UNITS         = "TemperatureUnits";
    public  static final String PARM_LATLON_FORMAT      = "LatLonUnits";
    public  static final String PARM_DEVICE_TITLE       = "DeviceTitle";
    public  static final String PARM_DEVICES_TITLE      = "DevicesTitle";
    public  static final String PARM_GROUP_TITLE        = "GroupTitle";
    public  static final String PARM_GROUPS_TITLE       = "GroupsTitle";
    public  static final String PARM_ADDRESS_TITLE      = "AddressTitle";
    public  static final String PARM_ADDRESSES_TITLE    = "AddressesTitle";
    public  static final String PARM_DEFAULT_USER       = "DefaultUser";
    public  static final String PARM_ACCT_EXPIRE        = "Expiration"; // read-only
    public  static final String PARM_MAX_COMMANDS       = "MaxPings"; // read-only
    public  static final String PARM_TOT_COMMANDS       = "TotalPings"; // read-only

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public AccountModel()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_ACCOUNT_INFO);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }
    
    // ------------------------------------------------------------------------
   
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountModel.class);
        return super._getMenuDescription(reqState,i18n.getString("AccountModel.editMenuDesc","View/Edit Account Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountModel.class);
        return super._getMenuHelp(reqState,i18n.getString("AccountModel.editMenuHelp","View and Edit the current Account information"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountModel.class);
        return super._getNavigationDescription(reqState,i18n.getString("AccountModel.navDesc","Account"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(AccountModel.class);
        return i18n.getString("AccountModel.navTab","Account Admin");
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N    i18n     = privLabel.getI18N(AccountModel.class);
        final Locale  locale   = reqState.getLocale();
        final Account currAcct = reqState.getCurrentAccount();
        final User    currUser = reqState.getCurrentUser();
        final String  pageName = this.getPageName();
        String m = pageMsg;
        boolean error = false;

        /* ACL allow edit/view */
        boolean allowEdit = privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* command */
        String accountCmd = reqState.getCommandName();
        boolean updateAccount = accountCmd.equals(COMMAND_INFO_UPDATE);

        /* change the account info? */
        if (updateAccount) {
            HttpServletRequest request = reqState.getHttpServletRequest();
            String submit = AttributeTools.getRequestString(request, PARM_SUBMIT_CHANGE, "");
            if (SubmitMatch(submit,i18n.getString("AccountModel.change","Change"))) {
                String acctName     = AttributeTools.getRequestString(request, PARM_ACCOUNT_DESC,"");
                String contactName  = AttributeTools.getRequestString(request, PARM_CONTACT_NAME,"");
                String contactPhone = AttributeTools.getRequestString(request, PARM_CONTACT_PHONE,"");
                String contactEmail = AttributeTools.getRequestString(request, PARM_CONTACT_EMAIL,"");
                String notifyEmail  = AttributeTools.getRequestString(request, PARM_NOTIFY_EMAIL,"");
                String timeZone     = AttributeTools.getRequestString(request, PARM_TIMEZONE,"");
                String speedUnits   = AttributeTools.getRequestString(request, PARM_SPEED_UNITS,"");
                String distUnits    = AttributeTools.getRequestString(request, PARM_DIST_UNITS,"");
                String volUnits     = AttributeTools.getRequestString(request, PARM_VOLM_UNITS,"");
                String econUnits    = AttributeTools.getRequestString(request, PARM_ECON_UNITS,"");
                String pressUnits   = AttributeTools.getRequestString(request, PARM_PRESS_UNITS,"");
                String tempUnits    = AttributeTools.getRequestString(request, PARM_TEMP_UNITS,"");
                String latLonFormat = AttributeTools.getRequestString(request, PARM_LATLON_FORMAT,"");
                String deviceTitle  = AttributeTools.getRequestString(request, PARM_DEVICE_TITLE,"");
                String devicesTitle = AttributeTools.getRequestString(request, PARM_DEVICES_TITLE,"");
                String groupTitle   = AttributeTools.getRequestString(request, PARM_GROUP_TITLE,"");
                String groupsTitle  = AttributeTools.getRequestString(request, PARM_GROUPS_TITLE,"");
                String addrTitle    = AttributeTools.getRequestString(request, PARM_ADDRESS_TITLE,"");
                String addrsTitle   = AttributeTools.getRequestString(request, PARM_ADDRESSES_TITLE,"");
                String defaultUser  = AttributeTools.getRequestString(request, PARM_DEFAULT_USER,"");
                try {
                    boolean saveOK = true;
                    // -- description
                    if (!StringTools.isBlank(acctName)) {
                        currAcct.setDescription(acctName);
                    } else {
                        currAcct.setDescription(currAcct.getAccountID());
                    }
                    // -- contact name
                    currAcct.setContactName(contactName);
                    // -- contact phone
                    currAcct.setContactPhone(contactPhone);
                    // -- contact email
                    if (StringTools.isBlank(contactEmail) || EMail.validateAddress(contactEmail)) {
                        currAcct.setContactEmail(contactEmail);
                    } else {
                        Print.logWarn("Contact EMail address is invalid: " + contactEmail);
                        m = i18n.getString("AccountModel.pleaseEnterContactEMail","Please enter a valid contact email address"); // UserErrMsg
                        error = true;
                        saveOK = false;
                    }
                    // -- notify email
                    if (StringTools.isBlank(notifyEmail)) {
                        // -- clearing email address
                        if (!currAcct.getNotifyEmail().equals(notifyEmail)) {
                            currAcct.setNotifyEmail(notifyEmail);
                        }
                    } else
                    if (!EMail.isSendMailEnabled()) {
                        // -- email not enabled
                        m = i18n.getString("AccountModel.sendMailDisabled","EMail notification is disabled (notify email not allowed), place contact your system administrator");
                        error = true;
                        saveOK = false;
                    } else
                    if (EMail.validateAddresses(notifyEmail,true/*acceptSMS*/)) {
                        // -- set valid email address
                        if (!currAcct.getNotifyEmail().equals(notifyEmail)) {
                            currAcct.setNotifyEmail(notifyEmail);
                        }
                    } else {
                        m = i18n.getString("AccountModel.pleaseEnterNotifyEMail","Please enter a valid notify email/sms address"); // UserErrMsg
                        error = true;
                        saveOK = false;
                    }
                    // -- timezone
                    currAcct.setTimeZone(timeZone);
                    // -- speed units
                    currAcct.setSpeedUnits(speedUnits, locale);
                    // -- distance units
                    currAcct.setDistanceUnits(distUnits, locale);
                    // -- volume units
                    currAcct.setVolumeUnits(volUnits, locale);
                    // -- economy units
                    currAcct.setEconomyUnits(econUnits, locale);
                    // -- pressure units
                    currAcct.setPressureUnits(pressUnits, locale);
                    // -- temperature units
                    currAcct.setTemperatureUnits(tempUnits, locale);
                    // -- latitude/longitude format
                    currAcct.setLatLonFormat(latLonFormat, locale);
                    // -- reverse-geocoder mode
                  //currAcct.setGeocoderMode(revGeoMode, locale);
                    // -- 'Device' title
                    String devSingTitle = deviceTitle;
                    String devPlurTitle = devicesTitle;
                    currAcct.setDeviceTitle(devSingTitle, devPlurTitle);
                    // -- 'DeviceGroup' title
                    String grpSingTitle = groupTitle;
                    String grpPlurTitle = groupsTitle;
                    currAcct.setDeviceGroupTitle(grpSingTitle, grpPlurTitle);
                    // -- 'Address' title
                    String adrSingTitle = addrTitle; 
                    String adrPlurTitle = addrsTitle;
                    currAcct.setAddressTitle(adrSingTitle, adrPlurTitle);
                    // -- default user
                    currAcct.setDefaultUser(defaultUser);
                    // -- save
                    if (saveOK) {
                        /* exclude fields that only the SysAdmin/AccountManager should change */
                        currAcct.addExcludedUpdateFields(
                            Account.FLD_isActive,
                            Account.FLD_isAccountManager,
                            Account.FLD_managerID,
                            Account.FLD_privateLabelName,
                            Account.FLD_isBorderCrossing,
                            Account.FLD_geocoderMode
                            );
                        currAcct.save();
                        AttributeTools.setSessionAttribute(request, Calendar.PARM_TIMEZONE[0], timeZone);
                        //Track.writeMessageResponse(reqState, i18n.getString("AccountModel.updatedAcct","Account information updated"));
                        m = i18n.getString("AccountModel.updatedAcct","Account information updated"); // UserErrMsg
                    }
                } catch (Throwable t) {
                    Print.logException("Updating Account", t);
                    m = i18n.getString("AccountModel.errorUpdate","Internal error updating Account"); // UserErrMsg
                    error = true;
                    return;
                }
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = AccountModel.this.getCssDirectory();
                //WebPageAdaptor.writeCssLink(out, reqState, "AccountModel.css", cssDir);
            }
        };

        /* javascript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
            }
        };

        /* model */
        final boolean _allowEdit = allowEdit;
        final DataModel model = new DataModel(this,m,error) {
            public void initModel() {

                String menuURL = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String saveURL = privLabel.getWebPageURL(reqState, pageName, COMMAND_INFO_UPDATE);
                FieldListModel fieldModel = this.getFieldListModel();
                fieldModel.setForm("AccountModel_EditView", saveURL);
                if (_allowEdit) {
                    this.setPageContext(DataModel.CONTEXT_EDIT); // EDIT|VIEW|LIST|COMMANDS|etc
                    this.setFrameTitle(i18n.getString("AccountModel.editAccount","Edit Account Information"));
                    fieldModel.addFormSubmit(FieldListModel.PROP_btnChange, PARM_SUBMIT_CHANGE, i18n.getString("AccountModel.change","Change"));
                    fieldModel.addFormButton(FieldListModel.PROP_btnCancel, PARM_BUTTON_CANCEL, i18n.getString("AccountModel.cancel","Cancel"), "javascript:openURL('"+menuURL+"','_self');");
                } else {
                    this.setPageContext(DataModel.CONTEXT_VIEW); // EDIT|VIEW|LIST|COMMANDS|etc
                    this.setFrameTitle(i18n.getString("AccountModel.viewAccount","View Account Information"));
                    fieldModel.addFormButton(FieldListModel.PROP_btnBack, PARM_BUTTON_BACK, i18n.getString("AccountModel.back","Back"), "javascript:openURL('"+menuURL+"','_self');");
                }

                // -- frame content
                ComboMap    _tzList          = privLabel.getTimeZoneComboMap();
                ComboMap    _suList          = privLabel.getEnumComboMap(Account.SpeedUnits.class      );
                ComboMap    _duList          = privLabel.getEnumComboMap(Account.DistanceUnits.class   );
                ComboMap    _vuList          = privLabel.getEnumComboMap(Account.VolumeUnits.class     );
                ComboMap    _ecList          = privLabel.getEnumComboMap(Account.EconomyUnits.class    );
                ComboMap    _puList          = privLabel.getEnumComboMap(Account.PressureUnits.class   );
                ComboMap    _tuList          = privLabel.getEnumComboMap(Account.TemperatureUnits.class);
                ComboMap    _llList          = privLabel.getEnumComboMap(Account.LatLonFormat.class    );
                ComboOption speedUnits       = privLabel.getEnumComboOption(Account.getSpeedUnits(currAcct)      );
                ComboOption distanceUnits    = privLabel.getEnumComboOption(Account.getDistanceUnits(currAcct)   );
                ComboOption volumeUnits      = privLabel.getEnumComboOption(Account.getVolumeUnits(currAcct)     );
                ComboOption economyUnits     = privLabel.getEnumComboOption(Account.getEconomyUnits(currAcct)    );
                ComboOption pressureUnits    = privLabel.getEnumComboOption(Account.getPressureUnits(currAcct)   );
                ComboOption temperatureUnits = privLabel.getEnumComboOption(Account.getTemperatureUnits(currAcct));
                ComboOption latLonFormat     = privLabel.getEnumComboOption(Account.getLatLonFormat(currAcct)    );
                String      devTitles[]      = currAcct.getDeviceTitles(locale, new String[]{"",""});
                String      grpTitles[]      = currAcct.getDeviceGroupTitles(locale, new String[]{"",""});
                String      adrTitles[]      = currAcct.getAddressTitles(locale, new String[]{"",""});
                int         neFldLen         = Account.getMaximumNotifyEmailLength(); // 125

                /* add fields */
                // --
                fieldModel.addInputText(  PARM_ACCOUNT_ID      , i18n.getString("AccountModel.accountID","Account ID:")                 , null, false     , 32,       32, currAcct.getAccountID()   , null);
                fieldModel.addInputText(  PARM_ACCOUNT_DESC    , i18n.getString("AccountModel.accountName","Account Description")       , null, _allowEdit, 40,       40, currAcct.getDescription() , null);
                fieldModel.addInputText(  PARM_CONTACT_NAME    , i18n.getString("AccountModel.contactName","Contact Name")              , null, _allowEdit, 40,       40, currAcct.getContactName() , null);
                fieldModel.addInputText(  PARM_CONTACT_PHONE   , i18n.getString("AccountModel.contactPhone","Contact Phone")            , null, _allowEdit, 20,       20, currAcct.getContactPhone(), null);
                fieldModel.addInputText(  PARM_CONTACT_EMAIL   , i18n.getString("AccountModel.contactEMail","Contact Email")            , null, _allowEdit, 60,      100, currAcct.getContactEmail(), null);
                fieldModel.addInputText(  PARM_NOTIFY_EMAIL    , i18n.getString("AccountModel.notifyEMail","Notify Email")              , null, _allowEdit, 95, neFldLen, currAcct.getNotifyEmail() , null);
                // -- units
                fieldModel.addInputSelect(PARM_TIMEZONE        , i18n.getString("AccountModel.timeZone","Time Zone")                    , null, _allowEdit, 20,  _tzList, currAcct.getTimeZone()    , null);
                fieldModel.addInputSelect(PARM_SPEED_UNITS     , i18n.getString("AccountModel.speedUnits","Speed Units")                , null, _allowEdit, 10,  _suList, speedUnits                , null);
                fieldModel.addInputSelect(PARM_DIST_UNITS      , i18n.getString("AccountModel.istanceUnits","Distance Units")           , null, _allowEdit, 10,  _duList, distanceUnits             , null);
                fieldModel.addInputSelect(PARM_VOLM_UNITS      , i18n.getString("AccountModel.volumeUnits","Volume Units")              , null, _allowEdit, 10,  _vuList, volumeUnits               , null);
                fieldModel.addInputSelect(PARM_ECON_UNITS      , i18n.getString("AccountModel.economyUnits","Economy Units")            , null, _allowEdit, 10,  _ecList, economyUnits              , null);
                fieldModel.addInputSelect(PARM_PRESS_UNITS     , i18n.getString("AccountModel.pressureUnits","Pressure Units")          , null, _allowEdit, 10,  _puList, pressureUnits             , null);
                fieldModel.addInputSelect(PARM_TEMP_UNITS      , i18n.getString("AccountModel.temperatureUnits","Temperature Units")    , null, _allowEdit,  5,  _tuList, temperatureUnits          , null);
                fieldModel.addInputSelect(PARM_LATLON_FORMAT   , i18n.getString("AccountModel.latLonFormat","Latitude/Longitude Format"), null, _allowEdit, 15,  _llList, latLonFormat              , null);
                // -- device titles
                fieldModel.addInputText(  PARM_DEVICE_TITLE    , i18n.getString("AccountModel.deviceTitle","'Device' Title")            , null, _allowEdit, 20,       40, devTitles[0]              , null);
                fieldModel.addInputText(  PARM_DEVICES_TITLE   , i18n.getString("AccountModel.devicesTitle","'Devices' Title")          , null, _allowEdit, 20,       40, devTitles[1]              , null);
                // -- group titles
                fieldModel.addInputText(  PARM_GROUP_TITLE     , i18n.getString("AccountModel.groupTitle","'DeviceGroup' Title")        , null, _allowEdit, 20,       40, grpTitles[0]              , null);
                fieldModel.addInputText(  PARM_GROUPS_TITLE    , i18n.getString("AccountModel.groupsTitle","'DeviceGroups' Title")      , null, _allowEdit, 20,       40, grpTitles[1]              , null);
                // -- address titles
                fieldModel.addInputText(  PARM_ADDRESS_TITLE   , i18n.getString("AccountModel.addressTitle","'Address' Title")          , null, _allowEdit, 20,       40, adrTitles[0]              , null);
                fieldModel.addInputText(  PARM_ADDRESSES_TITLE , i18n.getString("AccountModel.addressTitlees","'Addresses' Title")      , null, _allowEdit, 20,       40, adrTitles[1]              , null);
                // -- default login user
                fieldModel.addInputText(  PARM_DEFAULT_USER    , i18n.getString("AccountModel.defaultUser","Default Login UserID")      , null, _allowEdit, 20,       32, currAcct.getDefaultUser() , null);
                // -- expiration
                long expireTime = currAcct.getExpirationTime();
                if (expireTime > 0L) {
                String  expireTimeStr = StringTools.blankDefault(reqState.formatDateTime(expireTime), "n/a");
                fieldModel.addInputText(  PARM_ACCT_EXPIRE     , i18n.getString("AccountModel.expiration","Expiration")                 , null, false     , 30,       30, expireTimeStr             , null);
                }
                // -- max pings / total pings
                int maxPingCnt = currAcct.getMaxPingCount();
                if (maxPingCnt > 0) {
                int totPingCnt = currAcct.getTotalPingCount();
                int remaining  = (maxPingCnt > totPingCnt)? (maxPingCnt - totPingCnt) : 0;
                fieldModel.addInputText(  PARM_MAX_COMMANDS    , i18n.getString("AccountModel.maxCommandCount","Max Allowed Commands")  , null, false     ,  5,        5, String.valueOf(maxPingCnt), null);
                fieldModel.addInputText(  PARM_TOT_COMMANDS    , i18n.getString("AccountModel.remainingCommands","Remaining Commands")  , null, false     ,  5,        5, String.valueOf(remaining) , null);
                }

            }
        };

        /* write frame */
        String onload = error? JS_alert(true,m) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // JavaScript
            null,                       // Navigation
            null,                       // Content
            model);                     // DataModel

    }
    
    // ------------------------------------------------------------------------
}
