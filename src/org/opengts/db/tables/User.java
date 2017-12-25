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
//     -Moved to "org.opengts.db.tables"
//  2007/06/13  Martin D. Flynn
//     -Added BLANK_PASSWORD to explicitly support blank passwords
//  2007/06/14  Martin D. Flynn
//     -Fixed 'isAuthorizedDevice' to return true if no 'deviceGroup' has been specified.
//  2007/07/14  Martin D. Flynn
//     -Added "-nopass" & "-password" options to command-line administration.
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//  2007/11/28  Martin D. Flynn
//     -Added '-editall' command-line option to display all fields.
//  2008/07/21  Martin D. Flynn
//     -Fixed problem preventing device groups from being set properly.
//  2008/08/15  Martin D. Flynn
//     -Explicitly write DeviceGroup "all" to authorized device group list when 
//      DBConfig.DEFAULT_DEVICE_AUTHORIZATION is 'false'.
//     -Added static methods 'getAdminUserID()' and 'isAdminUser(...)'
//  2008/09/01  Martin D. Flynn
//     -Added 'FLD_firstLoginPageID'
//  2008/10/16  Martin D. Flynn
//     -Changed 'getUsersForContactEmail' to return a list of 'User' objects.
//     -Changed unspecified 'gender' text from "Unknown" to "n/a" (not applicable)
//     -Added fields 'FLD_preferredDeviceID', 'FLD_roleID'
//  2011/03/08  Martin D. Flynn
//     -Added FLD_notifyEmail
//  2013/11/11  Martin D. Flynn
//     -Default timezone is now obtained from the Account record.
//  2016/01/04  Martin D. Flynn
//     -Added FLD_expirationTime
//  2016/04/01  Martin D. Flynn
//     -Added FLD_suspendUntilTime [2.6.2-B50]
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

public class User
    extends UserRecord<User>
    implements UserInformation
{

    // ------------------------------------------------------------------------

    /* optional columns */
    public static final String  OPTCOLS_AddressFieldInfo            = "startupInit.User.AddressFieldInfo";
    public static final String  OPTCOLS_ExtraFieldInfo              = "startupInit.User.ExtraFieldInfo";
    public static final String  OPTCOLS_PlatinumInfo                = "startupInit.User.PlatinumInfo";

    // ------------------------------------------------------------------------
    // Administrator user name

    public static final String  USER_ADMIN = "admin";

    /**
    *** Gets the defined "admin" user id
    *** @return The defined "admin" user id
    **/
    public static String getAdminUserID()
    {
        return USER_ADMIN;
    }

    /**
    *** Returns true if specified user is and "admin" user
    *** @param userID  The userID to test
    *** @return True if the specified is an "admin" user
    **/
    public static boolean isAdminUser(String userID)
    {
        if (StringTools.isBlank(userID)) {
            return false; // must be explicit
        } else {
            return User.getAdminUserID().equals(userID);
        }
    }

    /**
    *** Returns true if specified user is and "admin" user
    *** @param user  The user to test
    *** @return True if the specified is an "admin" user
    **/
    public static boolean isAdminUser(User user)
    {
        if (user == null) {
            return true; // null user is considered an 'admin'
        } else {
            return User.getAdminUserID().equalsIgnoreCase(user.getUserID());
        }
    }
    
    /**
    *** Gets the account/user name for the specified user. <br>
    *** (typically used for debug/logging purposes)
    *** @param user  The user for which the account/user name is returned
    *** @return The account/user id/name
    **/
    public static String getUserName(User user)
    {
        if (user == null) {
            return "null";
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            sb.append(user.getAccountID());
            sb.append("/");
            sb.append(user.getUserID());
            sb.append("] ");
            sb.append(user.getDescription());
            return sb.toString().trim();
        }
    }

    // ------------------------------------------------------------------------
    // Blank password
    
    public static final String  BLANK_PASSWORD                  = Account.BLANK_PASSWORD;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // User preferred device authorization

    public enum PreferredDeviceAuth {
        FALSE,
        TRUE,
        ONLY
    };

    /**
    *** Gets the global enumerated value for the property:<br>
    ***     User.authorizedPreferredDeviceID
    **/
    public static PreferredDeviceAuth GetPreferredDeviceAuth()
    {
        // -- User.authorizedPreferredDeviceID=false|true|only
        String prefDevAuth = RTConfig.getString(DBConfig.PROP_User_authorizedPreferredDeviceID,"");
        if (StringTools.isBlank(prefDevAuth)) {
            return PreferredDeviceAuth.FALSE;
        } else
        if (prefDevAuth.equalsIgnoreCase("false")) {
            return PreferredDeviceAuth.FALSE;
        } else
        if (prefDevAuth.equalsIgnoreCase("true")) {
            return PreferredDeviceAuth.TRUE;
        } else
        if (prefDevAuth.equalsIgnoreCase("only")) { // [2.6.3-B30]
            return PreferredDeviceAuth.ONLY;
        } else {
            return PreferredDeviceAuth.FALSE;
        }
    }

    /**
    *** True if the global enumerated "User.authorizedPreferredDeviceID" property value is ONLY
    **/
    public static boolean IsPreferredDeviceAuth_ONLY()
    {
        // -- User.authorizedPreferredDeviceID=only
        return User.GetPreferredDeviceAuth().equals(PreferredDeviceAuth.ONLY);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // User type enum

    public enum UserType implements EnumTools.StringLocale, EnumTools.IntValue {
        TYPE_000    (  0, I18N.getString(User.class,"User.type.type000"  ,"Type000"  )), // default
        TYPE_001    (  1, I18N.getString(User.class,"User.type.type001"  ,"Type001"  )),
        TYPE_002    (  2, I18N.getString(User.class,"User.type.type002"  ,"Type002"  )),
        TYPE_003    (  3, I18N.getString(User.class,"User.type.type003"  ,"Type003"  )),
        TYPE_010    ( 10, I18N.getString(User.class,"User.type.type010"  ,"Type010"  )),
        TYPE_011    ( 11, I18N.getString(User.class,"User.type.type011"  ,"Type011"  )),
        TYPE_020    ( 20, I18N.getString(User.class,"User.type.type020"  ,"Type020"  )),
        TYPE_021    ( 21, I18N.getString(User.class,"User.type.type021"  ,"Type021"  )),
        TYPE_030    ( 30, I18N.getString(User.class,"User.type.type030"  ,"Type030"  )),
        TYPE_031    ( 31, I18N.getString(User.class,"User.type.type031"  ,"Type031"  )),
        TEMPORARY   (900, I18N.getString(User.class,"User.type.temporary","Temporary")),
        SYSTEM      (999, I18N.getString(User.class,"User.type.system"   ,"System"   ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        UserType(int v, I18N.Text a)                { vv=v; aa=a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isDefault()                  { return this.equals(TYPE_000); }
        public boolean isTemporary()                { return this.equals(TEMPORARY); }
        public boolean isSystem()                   { return this.equals(SYSTEM); }
        public boolean isType(int type)             { return this.getIntValue() == type; }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Gender enum

    public enum Gender implements EnumTools.StringLocale, EnumTools.IntValue {
        UNKNOWN     (0, I18N.getString(User.class,"User.gender.notSpecified","n/a"    )),
        MALE        (1, I18N.getString(User.class,"User.gender.male"        ,"Male"   )),
        FEMALE      (2, I18N.getString(User.class,"User.gender.female"      ,"Female" ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        Gender(int v, I18N.Text a)                  { vv=v; aa=a; }
        public int     getIntValue()                { return vv; }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
    };

    /**
    *** Returns the defined Gender for the specified user.
    *** @param u  The user from which the Gender will be obtained.  
    ***           If null, the default Gender will be returned.
    *** @return The Gender
    **/
    public static Gender getGender(User u)
    {
        return (u != null)? EnumTools.getValueOf(Gender.class,u.getGender()) : EnumTools.getDefault(Gender.class);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** EXPERIMENTAL
    *** DBRecordListener callback API
    **/
    public static class RecordListener
        implements DBRecordListener<User>
    {
        private DBRecordListener<User> delegate = null;
        public RecordListener() {
            this.delegate = null;
            // -- TODO: assign delegate
        }
        public void recordWillInsert(User user) {
            //Print.logDebug("* User will be inserted: " + user.getAccountID() + "/" + user.getUserID());
            if (this.delegate != null) {
                this.delegate.recordWillInsert(user);
            }
        }
        public void recordDidInsert(User user) {
            Print.logDebug("* User inserted: " + user.getAccountID() + "/" + user.getUserID());
            if (this.delegate != null) {
                this.delegate.recordDidInsert(user);
            }
        }
        public void recordWillUpdate(User user) {
            //Print.logDebug("* User will be updated: " + user.getAccountID() + "/" + user.getUserID());
            if (this.delegate != null) {
                this.delegate.recordWillUpdate(user);
            }
        }
        public void recordDidUpdate(User user) {
            Print.logDebug("* User updated: " + user.getAccountID() + "/" + user.getUserID());
            if (this.delegate != null) {
                this.delegate.recordDidUpdate(user);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Timezone

    /**
    *** Gets the default User timezone
    **/
    public static String GetDefaultTimeZone()
    {
        return Account.GetDefaultTimeZone();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME                  = "User";
    public static String TABLE_NAME() { return DBProvider._preTranslateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_userType                 = "userType";
    public static final String FLD_roleID                   = RoleAcl.FLD_roleID; 
    public static final String FLD_password                 = Account.FLD_password;
    public static final String FLD_tempPassword             = Account.FLD_tempPassword;
    public static final String FLD_lastPasswords            = Account.FLD_lastPasswords;
    public static final String FLD_gender                   = "gender";
    public static final String FLD_notifyEmail              = Account.FLD_notifyEmail;
    public static final String FLD_contactName              = Account.FLD_contactName;
    public static final String FLD_contactPhone             = Account.FLD_contactPhone;
    public static final String FLD_contactEmail             = Account.FLD_contactEmail;
    public static final String FLD_timeZone                 = Account.FLD_timeZone;
    public static final String FLD_speedUnits               = Account.FLD_speedUnits;
    public static final String FLD_distanceUnits            = Account.FLD_distanceUnits;
    public static final String FLD_firstLoginPageID         = "firstLoginPageID";       // first page viewed at login
    public static final String FLD_preferredDeviceID        = "preferredDeviceID";      // preferred device ID
    public static final String FLD_maxAccessLevel           = "maxAccessLevel";
    public static final String FLD_passwdChangeTime         = "passwdChangeTime";
    public static final String FLD_passwdQueryTime          = "passwdQueryTime";
    public static final String FLD_expirationTime           = "expirationTime";
    public static final String FLD_suspendUntilTime         = "suspendUntilTime";
    public static final String FLD_lastLoginTime            = "lastLoginTime";
    private static DBField FieldInfo[] = {
        // Key fields
        newField_accountID(true),
        newField_userID(true),
        // User fields
        new DBField(FLD_userType            , Integer.TYPE  , DBField.TYPE_UINT16      , "User Type"                 , "edit=2"),
        new DBField(FLD_roleID              , String.class  , DBField.TYPE_ROLE_ID()   , "User Role"                 , "edit=2 altkey=role"),
        new DBField(FLD_password            , String.class  , DBField.TYPE_STRING(32)  , "Password"                  , "edit=2 editor=password"),
        new DBField(FLD_tempPassword        , String.class  , DBField.TYPE_STRING(32)  , "Temporary Password"        , "edit=2 editor=password"),
        new DBField(FLD_lastPasswords       , String.class  , DBField.TYPE_STRING(300) , "Prior Passwords"           , "edit=2"),
        new DBField(FLD_gender              , Integer.TYPE  , DBField.TYPE_UINT8       , "Gender"                    , "edit=2 enum=User$Gender"),
        new DBField(FLD_notifyEmail         , String.class  , DBField.TYPE_EMAIL_LIST(), "Notification EMail Address", "edit=2"),
        new DBField(FLD_contactName         , String.class  , DBField.TYPE_STRING(64)  , "Contact Name"              , "edit=2 utf8=true"),
        new DBField(FLD_contactPhone        , String.class  , DBField.TYPE_STRING(32)  , "Contact Phone"             , "edit=2"),
        new DBField(FLD_contactEmail        , String.class  , DBField.TYPE_STRING(64)  , "Contact EMail Address"     , "edit=2 altkey=email"),
        new DBField(FLD_timeZone            , String.class  , DBField.TYPE_STRING(32)  , "Time Zone"                 , "edit=2 editor=timeZone"),
      //new DBField(FLD_speedUnits          , Integer.TYPE  , DBField.TYPE_UINT8       , "Speed Units"               , "edit=2 enum=Account$SpeedUnits"),
      //new DBField(FLD_distanceUnits       , Integer.TYPE  , DBField.TYPE_UINT8       , "Distance Units"            , "edit=2 enum=Account$DistanceUnits"),
        new DBField(FLD_firstLoginPageID    , String.class  , DBField.TYPE_STRING(24)  , "First Login Page ID"       , "edit=2"),
        new DBField(FLD_preferredDeviceID   , String.class  , DBField.TYPE_DEV_ID()    , "Preferred Device ID"       , "edit=2"),
        new DBField(FLD_maxAccessLevel      , Integer.TYPE  , DBField.TYPE_UINT16      , "Maximum Access Level"      , "edit=2 enum=AclEntry$AccessLevel"),
        new DBField(FLD_passwdChangeTime    , Long.TYPE     , DBField.TYPE_UINT32      , "Last Password Change Time" , "format=time"),
        new DBField(FLD_passwdQueryTime     , Long.TYPE     , DBField.TYPE_UINT32      , "Last Password Query Time"  , "format=time"),
        new DBField(FLD_expirationTime      , Long.TYPE     , DBField.TYPE_UINT32      , "Expiration Time"           , "format=time"),
        new DBField(FLD_suspendUntilTime    , Long.TYPE     , DBField.TYPE_UINT32      , "Suspend Until Time"        , "format=time"),
        new DBField(FLD_lastLoginTime       , Long.TYPE     , DBField.TYPE_UINT32      , "Last Login Time"           , "format=time"),
        // Common fields
        newField_isActive(),
        newField_displayName(),
        newField_description(),
        newField_notes(),
        newField_lastUpdateTime(),
        newField_lastUpdateAccount(true),
        newField_lastUpdateUser(true),
        newField_creationTime(),
    };

    // Address fields
    // startupInit.User.AddressFieldInfo=true
    public static final String FLD_addressLine1             = "addressLine1";           // address line 1
    public static final String FLD_addressLine2             = "addressLine2";           // address line 2
    public static final String FLD_addressLine3             = "addressLine3";           // address line 3
    public static final String FLD_addressCity              = "addressCity";            // address city
    public static final String FLD_addressState             = "addressState";           // address state/province
    public static final String FLD_addressPostalCode        = "addressPostalCode";      // address postal code
    public static final String FLD_addressCountry           = "addressCountry";         // address country
    public static final String FLD_officeLocation           = "officeLocation";         // office location (id, region, etc)
    public static final DBField AddressFieldInfo[] = {
        new DBField(FLD_addressLine1        , String.class  , DBField.TYPE_STRING(70)  , "Address Line 1"            , "edit=2 utf8=true"),
        new DBField(FLD_addressLine2        , String.class  , DBField.TYPE_STRING(70)  , "Address Line 2"            , "edit=2 utf8=true"),
        new DBField(FLD_addressLine3        , String.class  , DBField.TYPE_STRING(70)  , "Address Line 3"            , "edit=2 utf8=true"),
        new DBField(FLD_addressCity         , String.class  , DBField.TYPE_STRING(50)  , "Address City"              , "edit=2 utf8=true"),
        new DBField(FLD_addressState        , String.class  , DBField.TYPE_STRING(50)  , "Address State/Province"    , "edit=2 utf8=true"),
        new DBField(FLD_addressPostalCode   , String.class  , DBField.TYPE_STRING(20)  , "Address Postal Code"       , "edit=2 utf8=true"),
        new DBField(FLD_addressCountry      , String.class  , DBField.TYPE_STRING(20)  , "Address Country"           , "edit=2 utf8=true"),
        new DBField(FLD_officeLocation      , String.class  , DBField.TYPE_STRING(200) , "Office Location"           , "edit=2 utf8=true"),
    };

    // Misc fields
    // startupInit.User.ExtraFieldInfo=true
    public static final String FLD_customAttributes         = "customAttributes";      // custom attributes
    public static final DBField ExtraFieldInfo[] = {
        new DBField(FLD_customAttributes    , String.class , DBField.TYPE_TEXT         , "Custom Fields"             , "edit=2 utf8=true"),
    };

    // -- Platinum Edition fields
    // -  [OPTCOLS_PlatinumInfo] startupInit.User.PlatinumInfo=true
    public static final String FLD_isDispatcher             = "isDispatcher";
    public static final DBField PlatinumInfo[]              = {
        new DBField(FLD_isDispatcher        , Boolean.TYPE , DBField.TYPE_BOOLEAN      , "isDispatcher"              , "edit=2"),
    };

    /* key class */
    public static class Key
        extends UserKey<User>
    {
        public Key() {
            super();
        }
        public Key(String acctId, String userId) {
            super.setKeyValue(FLD_accountID, ((acctId != null)? acctId.toLowerCase() : ""));
            super.setKeyValue(FLD_userID   , ((userId != null)? userId.toLowerCase() : ""));
        }
        public DBFactory<User> getFactory() {
            return User.getFactory();
        }
    }

    /* factory constructor */
    private static DBFactory<User> factory = null;
    public static DBFactory<User> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                User.TABLE_NAME(), 
                User.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                User.class, 
                User.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public User()
    {
        super();
    }

    /* database record */
    public User(User.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(User.class, loc);
        return i18n.getString("User.description", 
            "This table defines " +
            "Account specific Users."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    /* return the user type */
    public int getUserType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_userType);
        return (v != null)? v.intValue() : 0;
    }

    /* set the user type */
    public void setUserType(int v)
    {
        this.setFieldValue(FLD_userType, ((v >= 0)? v : 0));
    }

    // ------------------------------------------------------------------------

    /* gets the defined Role, or null if no role was defined */
    private Role userRole = null;
    public Role getRole()
    {
        if ((this.userRole == null) && !StringTools.isBlank(this.getRoleID())) {
            try {
                this.userRole = Role.getRole(this.getAccountID(), this.getRoleID());
                if (this.userRole != null) {
                    if (this.hasAccount() && !this.userRole.isSystemAdminRole()) {
                        // Only set the Role account if not a SystemAdmin Role.
                        this.userRole.setAccount(this.getAccount());
                    }
                } else {
                    Print.logError("User Role not found: %s/%s [user=%s]", this.getAccountID(), this.getRoleID(), this.getUserID());
                    return null;
                }
            } catch (DBException dbe) {
                Print.logException("Error retrieving User Role: " + this.getAccountID() + "/" + this.getRoleID(), dbe);
                return null;
            }
        }
        return this.userRole; // may be null
    }

    /* get the user role id */
    public String getRoleID()
    {
        String v = (String)this.getFieldValue(FLD_roleID);
        return StringTools.trim(v);
    }

    /* set the user role id */
    public void setRoleID(String v)
    {
        this.setFieldValue(FLD_roleID, StringTools.trim(v));
        this.userRole = null;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the encoded password of this account 
    **/
    public String getPassword()
    {
        String p = (String)this.getFieldValue(FLD_password);
        return (p != null)? p : "";
    }

    /**
    *** Sets the encoded password for this user 
    **/
    public void setPassword(String p)
    {
        this.addLastPassword(this.getPassword());
        this.setFieldValue(FLD_password, ((p != null)? p : ""));
        this.setPasswdChangeTime(DateTime.getCurrentTimeSec());
    }

    /** 
    *** Gets the encoded password of this account 
    **/
    public String getEncodedPassword()
    {
        return this.getPassword();
    }

    /**
    *** Sets the encoded password for this user 
    **/
    public void setEncodedPassword(String p)
    {
        this.setPassword(p);
    }

    // --------

    /** 
    *** Gets the previous encoded passwords
    *** (Comma separated list of Base64 encoded passwords)
    **/
    public String getLastPasswords()
    {
        String p = (String)this.getFieldValue(FLD_lastPasswords);
        return (p != null)? p : "";
    }

    /**
    *** Sets the previous encoded passwords
    *** (Comma separated list of Base64 encoded passwords)
    **/
    public void setLastPasswords(String p)
    {
        this.setFieldValue(FLD_lastPasswords, ((p != null)? p : ""));
    }

    /**
    *** Adds a password to the last passwords list
    **/
    public void addLastPassword(String p)
    {
        // -- get number of required unique passwords
        PasswordHandler pwh = Account.getPasswordHandler(this.getAccount()); // non-null
        int reqUniqPass = pwh.getRequiredUniquePasswordCount();
        if (reqUniqPass <= 0) {
            this.setLastPasswords("");
            return;
        }
        // -- 
        java.util.List<String> lpList = new Vector<String>();
        lpList.add(p);
        // --
        java.util.List<String> lpl = Account.decodeLastPasswords(this.getLastPasswords());
        if (!ListTools.isEmpty(lpl)) {
            for (int i = 0; (lpList.size() < reqUniqPass) && (i < lpl.size()); i++) {
                lpList.add(lpl.get(i));
            }
        }
        // --
        this.setLastPasswords(Account.encodeLastPasswords(lpList));
    }

    // --------

    /**
    *** Gets a list of the last used encoded passwords (including current password)
    **/
    public String[] getLastEncodedPasswords()
    {
        // -- get number of required unique passwords
        PasswordHandler pwh = Account.getPasswordHandler(this.getAccount()); // non-null
        int reqUniqPass = pwh.getRequiredUniquePasswordCount();
        if (reqUniqPass <= 0) {
            return null;
        }
        // -- include current password only
        if (reqUniqPass == 1) {
            return new String[] { this.getEncodedPassword() };
        }
        // -- current/previous passwords
        java.util.List<String> lpList = new Vector<String>();
        lpList.add(this.getEncodedPassword()); // current
        java.util.List<String> lpl = Account.decodeLastPasswords(this.getLastPasswords()); 
        if (!ListTools.isEmpty(lpl)) {
            for (int i = 0; (lpList.size() < reqUniqPass) && (i < lpl.size()); i++) {
                String p = lpl.get(i);
                if (StringTools.isBlank(p)) { continue; }
                lpList.add(p);
            }
        }
        // -- return previous passwords
        return lpList.toArray(new String[lpList.size()]);
    }

    // --------

    /**
    *** Gets the decoded password for this user.
    *** Returns null if password cannot be decoded.
    **/
    public String getDecodedPassword(BasicPrivateLabel bpl)
    {
        if (bpl == null) {
            bpl = Account.getPrivateLabel(this.getAccount());
        }
        String pass = Account.decodePassword(bpl, this.getEncodedPassword());
        // -- it is possible that this password cannot be decoded
        return pass; // 'null' if password cannot be decoded
    }

    /**
    *** Encodes and sets the entered password for this user 
    **/
    public void setDecodedPassword(BasicPrivateLabel bpl, String enteredPass, boolean isTemp)
    {
        // -- get BasicPrivateLabel
        if (bpl == null) {
            bpl = Account.getPrivateLabel(this.getAccount());
        }
        // -- encode and set password
        String encodedPass = Account.encodePassword(bpl, enteredPass);
        if (!this.getEncodedPassword().equals(encodedPass)) {
            this.setEncodedPassword(encodedPass);
        }
        // -- temporary password?
        if (isTemp) {
            this.setTempPassword(enteredPass);
        } else {
            this.setTempPassword(null); // clear temporary password
        }
    }

    // --------

    /* reset the password */
    // -- does not save the record!
    public String resetPassword(BasicPrivateLabel bpl)
    {
        String enteredPass = Account.createRandomPassword(Account.TEMP_PASSWORD_LENGTH);
        this.setDecodedPassword(bpl, enteredPass, true);
        return enteredPass; // record not yet saved!
    }

    /* check that the specified password is a match for this account */
    public boolean checkPassword(BasicPrivateLabel bpl, String enteredPass, boolean suspend)
    {
        // -- get BasicPrivateLabel
        if (bpl == null) {
            bpl = Account.getPrivateLabel(this.getAccount());
        }
        // -- check password
        boolean ok = Account.checkPassword(bpl, enteredPass, this.getEncodedPassword());
        if (!ok && suspend) {
            // -- suspend on excessive failed login attempts
            this.suspendOnLoginFailureAttempt(true); // count current login failure
        }
        return ok;
    }

    /* check that the specified password is a match for this account */
    // -- the released CelltracGTS/Server still references this method
    @Deprecated
    public boolean checkPassword(BasicPrivateLabel bpl, String enteredPass)
    {
        return this.checkPassword(bpl, enteredPass, false);
    }

    // --------

    /**
    *** Gets the temporary clear-text password of this user.
    **/
    public String getTempPassword()
    {
        String p = (String)this.getFieldValue(FLD_tempPassword);
        return (p != null)? p : "";
    }

    /**
    *** Sets the temporary clear-text password of this user.
    *** This temporary password will be cleared when the regular password is set.
    **/
    public void setTempPassword(String p)
    {
        this.setFieldValue(FLD_tempPassword, ((p != null)? p : ""));
    }

    // --------

    /** 
    *** Update password fields
    **/
    public void updatePasswordFields()
        throws DBException
    {
        this.update(User.FLD_password, User.FLD_tempPassword, User.FLD_lastPasswords);
    }

    // ------------------------------------------------------------------------

    /* get the gender of the user */
    public int getGender()
    {
        Integer v = (Integer)this.getFieldValue(FLD_gender);
        return (v != null)? v.intValue() : EnumTools.getDefault(Gender.class).getIntValue();
    }

    /* set the gender */
    public void setGender(int v)
    {
        this.setFieldValue(FLD_gender, EnumTools.getValueOf(Gender.class,v).getIntValue());
    }

    /* set the gender */
    public void setGender(Gender v)
    {
        this.setFieldValue(FLD_gender, EnumTools.getValueOf(Gender.class,v).getIntValue());
    }

    /* set the string representation of the gender */
    public void setGender(String v, Locale locale)
    {
        this.setFieldValue(FLD_gender, EnumTools.getValueOf(Gender.class,v,locale).getIntValue());
    }

    // ------------------------------------------------------------------------

    /* return the notification email address for this account */
    public String getNotifyEmail()
    {
        String v = (String)this.getFieldValue(FLD_notifyEmail);
        return StringTools.trim(v);
    }

    /* set the notification email address for this account */
    public void setNotifyEmail(String v)
    {
        this.setFieldValue(FLD_notifyEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get contact name of this user */
    public String getContactName()
    {
        String v = (String)this.getFieldValue(FLD_contactName);
        return StringTools.trim(v);
    }

    public void setContactName(String v)
    {
        this.setFieldValue(FLD_contactName, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get contact phone of this user */
    public String getContactPhone()
    {
        String v = (String)this.getFieldValue(FLD_contactPhone);
        return StringTools.trim(v);
    }

    public void setContactPhone(String v)
    {
        this.setFieldValue(FLD_contactPhone, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    /* get contact email of this user */
    public String getContactEmail()
    {
        String v = (String)this.getFieldValue(FLD_contactEmail);
        return StringTools.trim(v);
    }

    public void setContactEmail(String v)
    {
        this.setFieldValue(FLD_contactEmail, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    private TimeZone timeZone = null;

    /**
    *** Gets the TimeZone instance for this user 
    *** @param dft The default timezone if no timezone is defined for usr/account
    **/
    public TimeZone getTimeZone(TimeZone dft)
    {
        if (this.timeZone == null) {
            this.timeZone = DateTime.getTimeZone(this.getTimeZone(), null);
            if (this.timeZone == null) {
                Account acct = this.getAccount(); // should never be null
                if (acct != null) {
                    this.timeZone = acct.getTimeZone(dft);
                } else {
                    this.timeZone = (dft != null)? dft : DateTime.getGMTTimeZone();
                }
            }
        }
        return this.timeZone;
    }

    /**
    *** Gets time zone for this user 
    **/
    public String getTimeZone()
    {
        String v = (String)this.getFieldValue(FLD_timeZone);
        if (StringTools.isBlank(v)) {
            Account acct = this.getAccount(); // should never be null
            return (acct != null)? acct.getTimeZone() : User.GetDefaultTimeZone();
        } else {
            return v.trim();
        }
    }

    /**
    *** Sets the timezone for this user
    **/
    public void setTimeZone(String v)
    {
        String tz = StringTools.trim(v);
        if (!StringTools.isBlank(tz)) {
            // -- validate timezone value?
        }
        this.timeZone = null;
        this.setFieldValue(FLD_timeZone, tz);
    }

    /* return current DateTime (relative the User TimeZone) */
    public DateTime getCurrentDateTime()
    {
        return new DateTime(this.getTimeZone(null));
    }

    // ------------------------------------------------------------------------

    /* get the speed-units for this account */
    public int getSpeedUnits()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_speedUnits);
        return (v != null)? v.intValue() : EnumTools.getDefault(Account.SpeedUnits.class).getIntValue();
    }

    /* set the speed-units */
    public void setSpeedUnits(int v)
    {
        this.setOptionalFieldValue(FLD_speedUnits, EnumTools.getValueOf(Account.SpeedUnits.class,v).getIntValue());
    }

    /* set the speed-units */
    public void setSpeedUnits(Account.SpeedUnits v)
    {
        this.setOptionalFieldValue(FLD_speedUnits, EnumTools.getValueOf(Account.SpeedUnits.class,v).getIntValue());
    }

    /* set the string representation of the speed-units */
    public void setSpeedUnits(String v, Locale locale)
    {
        this.setOptionalFieldValue(FLD_speedUnits, EnumTools.getValueOf(Account.SpeedUnits.class,v,locale).getIntValue());
    }

    /* return a formatted speed string */
    public String getSpeedString(double speedKPH, boolean inclUnits, Locale locale)
    {
        return this.getSpeedString(speedKPH, "0", null, inclUnits, locale);
    }

    /* return a formatted speed string */
    public String getSpeedString(double speedKPH, String format, boolean inclUnits, Locale locale)
    {
        return this.getSpeedString(speedKPH, format, null, inclUnits, locale);
    }

    /* return a formatted speed string */
    public String getSpeedString(double speedKPH, String format, Account.SpeedUnits speedUnitsEnum, boolean inclUnits, Locale locale)
    {
        if (speedUnitsEnum == null) { speedUnitsEnum = Account.getSpeedUnits(this); }
        double speed = speedUnitsEnum.convertFromKPH(speedKPH);
        String speedFmt = StringTools.format(speed, format);
        if (speed <= 0.0) {
            return speedFmt;
        } else {
            if (inclUnits) {
                return speedFmt + " " + speedUnitsEnum.toString(locale);
            } else {
                return speedFmt;
            }
        }
    }

    // ------------------------------------------------------------------------

    /* get the distance units for this account */
    public int getDistanceUnits()
    {
        Integer v = (Integer)this.getOptionalFieldValue(FLD_distanceUnits);
        return (v != null)? v.intValue() : EnumTools.getDefault(Account.DistanceUnits.class).getIntValue();
    }

    /* set the distance units */
    public void setDistanceUnits(int v)
    {
        this.setOptionalFieldValue(FLD_distanceUnits, EnumTools.getValueOf(Account.DistanceUnits.class,v).getIntValue());
    }

    /* set the distance units */
    public void setDistanceUnits(Account.DistanceUnits v)
    {
        this.setOptionalFieldValue(FLD_distanceUnits, EnumTools.getValueOf(Account.DistanceUnits.class,v).getIntValue());
    }

    /* set the string representation of the distance units */
    public void setDistanceUnits(String v, Locale locale)
    {
        this.setOptionalFieldValue(FLD_distanceUnits, EnumTools.getValueOf(Account.DistanceUnits.class,v,locale).getIntValue());
    }

    /* return a formatted distance string */
    public String getDistanceString(double distKM, boolean inclUnits, Locale locale)
    {
        Account.DistanceUnits units = Account.getDistanceUnits(this);
        String distUnitsStr = units.toString(locale);
        double dist         = units.convertFromKM(distKM);
        String distStr      = StringTools.format(dist, "0");
        return inclUnits? (distStr + " " + distUnitsStr) : distStr;
    }

    // ------------------------------------------------------------------------

    /* get default login page ID */
    public String getFirstLoginPageID()
    {
        String v = (String)this.getFieldValue(FLD_firstLoginPageID);
        return StringTools.trim(v);
    }

    public void setFirstLoginPageID(String v)
    {
        this.setFieldValue(FLD_firstLoginPageID, StringTools.trim(v));
    }
    
    public boolean hasFirstLoginPageID()
    {
        return !StringTools.isBlank(this.getFirstLoginPageID());
    }

    // ------------------------------------------------------------------------

    /* get preferred device ID */
    public String getPreferredDeviceID()
    {
        String v = (String)this.getFieldValue(FLD_preferredDeviceID);
        return StringTools.trim(v);
    }

    public void setPreferredDeviceID(String v)
    {
        this.setFieldValue(FLD_preferredDeviceID, StringTools.trim(v));
    }
    
    public boolean hasPreferredDeviceID()
    {
        return !StringTools.isBlank(this.getPreferredDeviceID());
    }

    // ------------------------------------------------------------------------

    /* get maximum access level */
    public int getMaxAccessLevel()
    {
        if (this.isAdminUser()) {
            // admin user is never restricted
            return AccessLevel.ALL.getIntValue();
        } else {
            Integer v = (Integer)this.getFieldValue(FLD_maxAccessLevel);
            if (v != null) {
                int aclLevel = v.intValue();
                if ((aclLevel < 0) || (aclLevel == AccessLevel.NONE.getIntValue())) {
                    // default to ALL, if invalid/undefined
                    return AccessLevel.ALL.getIntValue();
                } else
                if (aclLevel > AccessLevel.ALL.getIntValue()) {
                    // cannot me more than ALL
                    return AccessLevel.ALL.getIntValue();
                } else {
                    // defined maximum access level
                    return aclLevel;
                }
            } else {
                // default to ALL, if undefined
                return AccessLevel.ALL.getIntValue();
            }
        }
    }

    public void setMaxAccessLevel(int v)
    {
        int accessLevel = EnumTools.getValueOf(AccessLevel.class,v).getIntValue();
        this.setFieldValue(FLD_maxAccessLevel, accessLevel);
    }

    public void setMaxAccessLevel(String v)
    {
        int accessLevel = EnumTools.getValueOf(AccessLevel.class,v).getIntValue();
        this.setFieldValue(FLD_maxAccessLevel, accessLevel);
    }

    public void setMaxAccessLevel(AccessLevel v)
    {
        int accessLevel = (v != null)? v.getIntValue() : AccessLevel.ALL.getIntValue();
        this.setFieldValue(FLD_maxAccessLevel, accessLevel);
    }

    // ------------------------------------------------------------------------

    /* return the last time the password was changed for this account */
    public long getPasswdChangeTime()
    {
        Long v = (Long)this.getFieldValue(FLD_passwdChangeTime);
        return (v != null)? v.longValue() : 0L;
    }

    /* set the time the password was changed for this account */
    public void setPasswdChangeTime(long v)
    {
        this.setFieldValue(FLD_passwdChangeTime, v);
    }

    /* password expired? */
    public boolean hasPasswordExpired()
    {
        PasswordHandler pwh = Account.getPasswordHandler(this.getAccount()); // non-null
        return pwh.hasPasswordExpired(this.getPasswdChangeTime());
    }

    // ------------------------------------------------------------------------

    /* return time of last password query */
    public long getPasswdQueryTime()
    {
        Long v = (Long)this.getFieldValue(FLD_passwdQueryTime);
        return (v != null)? v.longValue() : 0L;
    }

    public void setPasswdQueryTime(long v)
    {
        this.setFieldValue(FLD_passwdQueryTime, v);
    }

    // ------------------------------------------------------------------------

    /* return the time this account expires */
    public long getExpirationTime()
    {
        Long v = (Long)this.getFieldValue(FLD_expirationTime);
        return (v != null)? v.longValue() : 0L;
    }

    /* set the time this account expires */
    public void setExpirationTime(long v)
    {
        this.setFieldValue(FLD_expirationTime, v);
    }

    /* return true if this account has expired */
    public boolean isExpired()
    {

        /* not active? (assume expired if not active) */
        if (!this.isActive()) {
            return true;
        }

        /* expired? */
        long expireTime = this.getExpirationTime();
        if ((expireTime > 0L) && (expireTime < DateTime.getCurrentTimeSec())) {
            return true;
        }

        /* not expired */
        return false;

    }
    
    /* return true if this account has an expiry date */
    public boolean doesExpire()
    {
        long expireTime = this.getExpirationTime();
        return (expireTime > 0L);
    }

    /* return true if this account will expire within the specified # of seconds */
    public boolean willExpire(long withinSec)
    {

        /* will account expire? */
        long expireTime = this.getExpirationTime();
        if ((expireTime > 0L) && 
            ((withinSec < 0L) || (expireTime < (DateTime.getCurrentTimeSec() + withinSec)))) {
            return true;
        }

        /* will not expired */
        return false;

    }

    // ------------------------------------------------------------------------

    /* return the user suspend time */
    public long getSuspendUntilTime()
    {
        Long v = (Long)this.getFieldValue(FLD_suspendUntilTime);
        return (v != null)? v.longValue() : 0L;
    }

    /* set the time of this user suspension */
    public void setSuspendUntilTime(long v)
    {
        this.setFieldValue(FLD_suspendUntilTime, v);
    }

    /* return true if this user is suspended [2.6.2-B50] */
    public boolean isSuspended()
    {

        /* account suspended? */
        long suspendTime = this.getSuspendUntilTime();
        if ((suspendTime > 0L) && (suspendTime >= DateTime.getCurrentTimeSec())) {
            return true;
        }

        /* not suspended */
        return false;

    }

    /**
    *** Called after login failure, check for user temporary suspension 
    *** @param addCurrentFailure  True to add the current login failure to the previously 
    ***     recorded login failures.  Should be true if "Audit.userLoginFailed(...)" has
    ***     not yet been called to record the current login failure.
    **/
    public boolean suspendOnLoginFailureAttempt(boolean addCurrentFailure)
    {

        /* suspend on failed login attempt disabled? */
        PasswordHandler pwh = Account.getPasswordHandler(this.getAccount()); // not null
        if (!pwh.getFailedLoginSuspendEnabled()) {
            // -- failed login attempt suspend is not enabled
            return false;
        }

        /* number of failed login attempts */
        String accountID   = this.getAccountID();
        String userID      = this.getUserID();
        long   asOfTime    = DateTime.getCurrentTimeSec(); // now
        long   sinceTime   = asOfTime - pwh.getFailedLoginAttemptInterval();
        long   addCount    = addCurrentFailure? 1L : 0L;
        long   failCount   = Audit.getFailedLoginAttempts(accountID, userID, sinceTime) + addCount;
        long   suspendTime = pwh.getFailedLoginAttemptSuspendTime((int)failCount, asOfTime);
        if (suspendTime > 0L) {
            // -- too many failed login attempts, suspend user
            this.setSuspendUntilTime(suspendTime);
            try {
                this.update(User.FLD_suspendUntilTime);
            } catch (DBException dbe) {
                Print.logError("Unable to set suspendUntilTime for user ("+accountID+"/"+userID+"): " + dbe);
            }
            return true;
        } else {
            return false;
        }

    }

    // ------------------------------------------------------------------------

    /* last user login time */
    public long getLastLoginTime()
    {
        Long v = (Long)this.getFieldValue(FLD_lastLoginTime);
        return (v != null)? v.longValue() : 0L;
    }

    /* last user login time */
    public String getLastLoginTimeString(TimeZone tmz)
    {
        long llTime = this.getLastLoginTime();
        if (llTime > 0L) {
            TimeZone   tz = (tmz != null)? tmz : this.getTimeZone(DateTime.GMT);
            DateTime llDT = new DateTime(llTime, tz);
            String  dtFmt = Account.GetDateTimeFormat(this.getAccount()); // TimeZone not included
            String  llStr = llDT.format(dtFmt, tz);
            return llStr + ", " + llDT.getTimeZoneShortName();
        } else {
            I18N i18n = I18N.getI18N(User.class, Account.GetLocale(this.getAccount()));
            return i18n.getString("User.loginNever", "never");
        }
    }

    /* last user login time */
    public String getLastLoginTimeString()
    {
        TimeZone tmz = null; // Account.GetTimeZone(this.getAccount(),this.getTimeZone(DateTime.GMT));
        return this.getLastLoginTimeString(tmz);
    }

    public void setLastLoginTime(long v)
    {
        this.setFieldValue(FLD_lastLoginTime, v);
    }

    // ------------------------------------------------------------------------

    public String getAddressLine1()
    {
        String v = (String)this.getFieldValue(FLD_addressLine1);
        return StringTools.trim(v);
    }

    public String getAddressLine2()
    {
        String v = (String)this.getFieldValue(FLD_addressLine2);
        return StringTools.trim(v);
    }
    
    public String getAddressLine3()
    {
        String v = (String)this.getFieldValue(FLD_addressLine3);
        return StringTools.trim(v);
    }
    
    public String[] getAddressLines()
    {
        return new String[] {
            this.getAddressLine1(),
            this.getAddressLine2(),
            this.getAddressLine3()
        };
    }
    
    public String getAddressCity()
    {
        String v = (String)this.getFieldValue(FLD_addressCity);
        return StringTools.trim(v);
    }
    
    public String getAddressState()
    {
        String v = (String)this.getFieldValue(FLD_addressState);
        return StringTools.trim(v);
    }
   
    public String getAddressPostalCode()
    {
        String v = (String)this.getFieldValue(FLD_addressPostalCode);
        return StringTools.trim(v);
    }

    public String getAddressCountry()
    {
        String v = (String)this.getFieldValue(FLD_addressCountry);
        return StringTools.trim(v);
    }

    public void setAddressLine1(String v)
    {
        this.setFieldValue(FLD_addressLine1, StringTools.trim(v));
    }

    public void setAddressLine2(String v)
    {
        this.setFieldValue(FLD_addressLine2, StringTools.trim(v));
    }

    public void setAddressLine3(String v)
    {
        this.setFieldValue(FLD_addressLine3, StringTools.trim(v));
    }
    
    public void setAddressLines(String lines[])
    {
        if ((lines != null) && (lines.length > 0)) {
            int n = 0;
            while ((n < lines.length) && ((lines[n] == null) || lines[n].trim().equals(""))) { n++; }
            this.setAddressLine1((n < lines.length)? lines[n++].trim() : "");
            while ((n < lines.length) && ((lines[n] == null) || lines[n].trim().equals(""))) { n++; }
            this.setAddressLine2((n < lines.length)? lines[n++].trim() : "");
            while ((n < lines.length) && ((lines[n] == null) || lines[n].trim().equals(""))) { n++; }
            this.setAddressLine3((n < lines.length)? lines[n++].trim() : "");
        } else {
            this.setAddressLine1("");
            this.setAddressLine2("");
            this.setAddressLine3("");
        }
    }

    public void setAddressCity(String v)
    {
        this.setFieldValue(FLD_addressCity, StringTools.trim(v));
    }

    public void setAddressState(String v)
    {
        this.setFieldValue(FLD_addressState, StringTools.trim(v));
    }

    public void setAddressPostalCode(String v)
    {
        this.setFieldValue(FLD_addressPostalCode, StringTools.trim(v));
    }

    public void setAddressCountry(String v)
    {
        this.setFieldValue(FLD_addressCountry, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    public String getOfficeLocation()
    {
        String v = (String)this.getFieldValue(FLD_officeLocation);
        return StringTools.trim(v);
    }

    public void setOfficeLocation(String v)
    {
        this.setFieldValue(FLD_officeLocation, StringTools.trim(v));
    }

    // ------------------------------------------------------------------------

    private RTProperties customAttrRTP = null;
    private Collection<String> customAttrKeys = null;

    /* get the custom attributes as a String */
    public String getCustomAttributes()
    {
        String v = (String)this.getOptionalFieldValue(FLD_customAttributes);
        return StringTools.trim(v);
    }

    /* set the custom attributes as a String */
    public void setCustomAttributes(String v)
    {
        this.setOptionalFieldValue(FLD_customAttributes, StringTools.trim(v));
        this.customAttrRTP  = null;
        this.customAttrKeys = null;
    }

    /* get custom attributes a an RTProperties */
    public RTProperties getCustomAttributesRTP()
    {
        if (this.customAttrRTP == null) {
            this.customAttrRTP = new RTProperties(this.getCustomAttributes());
        }
        return this.customAttrRTP;
    }

    /* get the custom attributes keys */
    public Collection<String> getCustomAttributeKeys()
    {
        if (this.customAttrKeys == null) {
            this.customAttrKeys = this.getCustomAttributesRTP().getPropertyKeys(null);
        }
        return this.customAttrKeys;
    }

    /* get the custom attributes as a String */
    public String getCustomAttribute(String key)
    {
        return this.getCustomAttributesRTP().getString(key,null);
    }

    /* get the custom attributes as a String */
    public String setCustomAttribute(String key, String value)
    {
        return this.getCustomAttributesRTP().getString(key,value);
    }

    // ------------------------------------------------------------------------

    /** (NOT CURRENTLY USED)
    *** Returns true if this user is a dispatcher
    *** @return True if this user is a dispatcher
    **/
    public boolean getIsDispatcher()
    {
        // -- check account
        Account account = this.getAccount();
        if (account == null) {
            // -- unlikely to occur
            return false;
        } else
        if (!account.getIsDispatcher()) {
            // -- Account must be a dispatcher
            return false;
        }
        // -- "admin" is always a dispatcher
        if (this.isAdminUser()) {
            return true;
        }
        // -- check assigned isDispatcher state
        Boolean v = (Boolean)this.getOptionalFieldValue(FLD_isDispatcher);
        return (v != null)? v.booleanValue() : false;
    }

    /** (NOT CURRENTLY USED)
    *** Sets the "Dispatcher" state for this User
    *** @param v The "Dispatcher" state for this User
    **/
    public void setIsDispatcher(boolean v)
    {
        // -- check account
        if (v == true) {
            // -- set "true" to "false" if account is not a dispatcher
            Account account = this.getAccount();
            if (account == null) {
                // -- unlikely to occur
                v = false;
            } else
            if (!account.getIsDispatcher()) {
                // -- Account must be a dispatcher
                v = false;
            }
        } else {
            // -- set "false" to "true" if user is "admin"
            if (this.isAdminUser()) {
                v = true;
            }
        }
        // -- set value
        this.setOptionalFieldValue(FLD_isDispatcher, v);
    }

    /** (NOT CURRENTLY USED)
    *** Returns true if the specified AccountID/UserID is a dispatcher
    **/
    public static boolean IsDispatcher(String accountID, String userID)
    {
        // -- check account
        Account account = null;
        try {
            account = Account.getAccount(accountID);
            if (account == null) {
                return false;
            } else
            if (!account.getIsDispatcher()) {
                return false;
            }
        } catch (DBException dbe) {
            Print.logException("Reading Account", dbe);
            return false;
        }
        // -- "admin" user?
        if (StringTools.isBlank(userID) || User.isAdminUser(userID)) {
            return true;
        }
        // -- check user
        User user = null;
        try {
            user = User.getUser(account, userID);
            if (user == null) {
                return false; // user not found
            } else
            if (user.isAdminUser()) {
                return true;
            } else
            if (user.getIsDispatcher()) {
                return true;
            } else {
                return false;
            }
        } catch (DBException dbe) {
            Print.logException("Reading User", dbe);
            return false;
        }
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        Account acct = this.getAccount();
        this.setIsActive(true);
        if (this.isAdminUser()) {
            this.setDescription("Administrator");
            if (acct != null) {
                this.setEncodedPassword(acct.getEncodedPassword());
                this.setTimeZone(acct.getTimeZone());
            }
        } else {
            this.setDescription("New User");
            if (acct != null) {
                this.setTimeZone(acct.getTimeZone());
            }
        }
        super.setRuntimeDefaultValues();
    }

    // ------------------------------------------------------------------------

    /* return true if this user is the admin user */
    public boolean isAdminUser()
    {
        return User.isAdminUser(this.getUserID());
    }

    /* return default device authorization */
    public boolean getDefaultDeviceAuthorization()
    {
        if (this.isAdminUser()) {
            // -- authorized for "ALL" devices
            return true;
        } else {
            // -- check for "ALL" device authorization
            return DBConfig.GetDefaultDeviceAuthorization(this.getAccountID());
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the DBSelect statement for the specified account/user */
    protected static DBSelect<? extends DBRecord<?>> _getGroupListSelect(String acctId, String userId, long limit)
    {

        /* empty/null account */
        if (StringTools.isBlank(acctId)) {
            return null;
        }

        /* empty/null user */
        if (StringTools.isBlank(userId)) {
            return null;
        }
        
        /* get select */
        // DBSelect: SELECT * FROM GroupList WHERE ((accountID='acct') and (userID='user')) ORDER BY sequence;
        DBSelect<GroupList> dsel = new DBSelect<GroupList>(GroupList.getFactory());
        dsel.setSelectedFields(GroupList.FLD_groupID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(GroupList.FLD_accountID,acctId),
                    dwh.EQ(GroupList.FLD_userID   ,userId)
                )
            )
        );
        if (GroupList.getFactory().hasExistingColumn(GroupList.FLD_sequence)) {
            dsel.setOrderByFields(GroupList.FLD_sequence);
        } else {
            dsel.setOrderByFields(GroupList.FLD_groupID);
        }
        dsel.setLimit(limit);
        return dsel;

    }

    /* return list of all Groups within the specified GroupList (NOT SCALABLE BEYOND A FEW HUNDRED GROUPS) */
    public static java.util.List<String> getGroupsForUser(String acctId, String userId)
        throws DBException
    {
        return User.getGroupsForUser(acctId, userId, -1L);
    }

    /**
    *** Gets a list of all GroupIDs within the specified GroupList 
    *** (May not be scalable beyond a few hundred groups) 
    **/
    public static java.util.List<String> getGroupsForUser(String acctId, String userId, long limit)
        throws DBException
    {

        /* valid account/groupId? */
        if (StringTools.isBlank(acctId)) {
            return null;
        } else
        if (StringTools.isBlank(userId)) {
            return null;
        }

        /* get db selector */
        DBSelect<? extends DBRecord<?>> dsel = User._getGroupListSelect(acctId, userId, limit);
        if (dsel == null) {
            return null;
        }

        /* read devices for account */
        java.util.List<String> grpList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String grpId = rs.getString(GroupList.FLD_groupID);
                grpList.add(grpId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Getting User DeviceGroup List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return grpList;

    }

    /* return authorized device groups */
    private java.util.List<String> deviceGroupList = null;
    public java.util.List<String> getDeviceGroups(boolean refresh)
        throws DBException
    {
        if ((this.deviceGroupList == null) || refresh) {
            this.deviceGroupList = User.getGroupsForUser(this.getAccountID(), this.getUserID(), -1L);
        }
        return this.deviceGroupList;
    }

    /**
    *** Gets the first authorized device groupID
    **/
    public String getFirstDeviceGroupID()
    {
        try {
            java.util.List<String> grpIDs = User.getGroupsForUser(this.getAccountID(), this.getUserID(), 1L);
            if (ListTools.size(grpIDs) > 0) {
                return grpIDs.get(0);
            }
            return null;
        } catch (DBException dbe) {
            Print.logException("Error reading first DeviceGroupID", dbe);
            return null;
        }
    }

    /**
    *** Returns true if this User is authorized for 'all' groups/devices
    **/
    public boolean isDeviceGroupAll()
        throws DBException
    {
        java.util.List<String> groups = this.getDeviceGroups(false/*refresh*/);
        if (ListTools.isEmpty(groups)) {
            return this.getDefaultDeviceAuthorization();
        } else {
            for (String groupID : groups) {
                if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) { 
                    return true; 
                }
            }
            return false;
        }
    }

    /**
    *** Sets the authorized device groups for this user
    **/
    public boolean setDeviceGroups(String groupList[])
    {
        // see "BasicPrivateLabel.PROP_UserInfo_authorizedGroupCount"
        return this._setDeviceGroups(ListTools.toIterator(groupList));
    }

    /**
    *** Sets the authorized device groups for this user
    **/
    public boolean setDeviceGroups(java.util.List<String> groupList)
    {
        return this._setDeviceGroups(ListTools.toIterator(groupList));
    }

    /**
    *** Sets the authorized device groups for this user
    **/
    protected boolean _setDeviceGroups(Iterator<String> groupListIter)
    {
        String accountID = this.getAccountID();
        String userID    = this.getUserID();
        this.deviceGroupList = null;

        /* delete all existing DeviceGroup entries from the GroupList table for this User */
        // -- [DELETE FROM GroupList WHERE accountID='account' AND userID='user']
        try {
            DBRecordKey<GroupList> grpListKey = new GroupList.Key();
            grpListKey.setFieldValue(GroupList.FLD_accountID, accountID);
            grpListKey.setFieldValue(GroupList.FLD_userID   , userID);
            DBDelete ddel = new DBDelete(GroupList.getFactory());
            ddel.setWhere(grpListKey.getWhereClause(DBWhere.KEY_PARTIAL_FIRST));
            DBConnection dbc = null;
            try {
                dbc = DBConnection.getDBConnection_delete();
                dbc.executeUpdate(ddel.toString());
            } finally {
                DBConnection.release(dbc);
            }
        } catch (Throwable th) { // DBException, SQLException
            Print.logException("Error deleting existing DeviceGroup entries from the User GroupList table", th);
            return false;
        }

        /* add new entries */
        if (groupListIter != null) {

            /* check groups other than ALL or blank */
            boolean all = false;
            int grpCount = 0;
            OrderedSet<String> addGroups = new OrderedSet<String>();
            for (;groupListIter.hasNext();) {
                String groupID = groupListIter.next();
                if (DeviceGroup.DEVICE_GROUP_ALL.equalsIgnoreCase(groupID)) {
                    all = true;
                    addGroups.clear();
                    break;
                } else
                if (DeviceGroup.DEVICE_GROUP_NONE.equalsIgnoreCase(groupID)) {
                    // -- skip this reserved group id
                } else
                if (StringTools.isBlank(groupID)) {
                    // -- skip blank group ids
                } else {
                    try {
                        if (DeviceGroup.exists(accountID,groupID)) {
                            grpCount++;
                            addGroups.add(groupID);
                        } else {
                            Print.logError("DeviceGroup does not exist: %s/%s", accountID, groupID);
                        }
                    } catch (DBException dbe) {
                        Print.logException("Error creating new DeviceGroup entries in the User GroupList table", dbe);
                        return false;
                    }
                }
            }

            /* add groupIDs in list */
            if (all) {
                if (!this.getDefaultDeviceAuthorization()) {
                    // -- if the default device authorization is false, we do explicitly specify that 
                    // -  this user has authority to view "ALL" devices, otherwise he will not athority 
                    // -  to view any device.
                    try {
                        GroupList groupListItem = GroupList.getGroupList(this, DeviceGroup.DEVICE_GROUP_ALL, true);
                        groupListItem.setSequence(0); // [2.6.2-B71]
                        groupListItem.save(); // insert();
                    } catch (DBException dbe) {
                        Print.logException("Error creating new DeviceGroup entries in the User GroupList table", dbe);
                        return false;
                    }
                }
            } else
            if (!ListTools.isEmpty(addGroups)) {
                try {
                    int grpLen = addGroups.size();
                    for (int g = 0; g < grpLen; g++) {
                        String groupID = addGroups.get(g);
                        GroupList groupListItem = GroupList.getGroupList(this, groupID, true/*create*/); 
                        groupListItem.setSequence(g); // [2.6.2-B71]
                        groupListItem.save(); // insert();
                    }
                } catch (DBException dbe) {
                    Print.logException("Error creating new DeviceGroup entries in the User GroupList table", dbe);
                    return false;
                }
            }

        }
        
        /* success */
        return true;

    }

    /* should not be used (does not set sequence/index)
    public void addDeviceGroup(String groupID)
        throws DBException
    {
        if (!StringTools.isBlank(groupID)) {
            String accountID = this.getAccountID();
            if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL) || DeviceGroup.exists(accountID,groupID)) {
                this.deviceGroupList = null;
                if (!GroupList.exists(accountID,this.getUserID(),groupID)) {
                    GroupList groupListItem = GroupList.getGroupList(this, groupID, true);
                  //groupListItem.setSequence(???); // [2.6.2-B71]
                    groupListItem.save();
                } else {
                    // -- already exists (quietly ignore)
                }
            } else {
                Print.logError("DeviceGroup does not exist: %s/%s", accountID, groupID);
            }
        }
    }
    */

    /* should not be used
    public void removeDeviceGroup(String groupID)
        throws DBException
    {
        if (!StringTools.isBlank(groupID)) {
            this.deviceGroupList = null;
            GroupList.Key grpListKey = new GroupList.Key(this.getAccountID(), this.getUserID(), groupID);
            grpListKey.delete(true);
        }
    }
    */

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* add all user authorized devices to the internal device map */
    public static OrderedSet<String> getAuthorizedDeviceIDs(User user, Account account, boolean inclInactv)
        throws DBException
    {
        if (user != null) {
            return user.getAuthorizedDeviceIDs(inclInactv);
        } else
        if (account != null) {
            return Device.getDeviceIDsForAccount(account.getAccountID(), null, inclInactv);
        } else {
            return new OrderedSet<String>();
        }
    }

    /* add all user authorized devices to the internal device map */
    public static OrderedSet<String> getAuthorizedDeviceIDs(User user, String accountID, boolean inclInactv)
        throws DBException
    {
        if (user != null) {
            return user.getAuthorizedDeviceIDs(inclInactv);
        } else
        if (accountID != null) {
            return Device.getDeviceIDsForAccount(accountID, null, inclInactv);
        } else {
            return new OrderedSet<String>();
        }
    }

    /* get all authorized devices for this user */
    protected OrderedSet<String> getAuthorizedDeviceIDs(boolean inclInactv)
        throws DBException
    {
        java.util.List<String> groupList = this.getDeviceGroups(true/*refresh*/);
        if (!ListTools.isEmpty(groupList)) {
            // The user is authorized to all Devices in the listed groups (thus "User" can be null)
            OrderedSet<String> list = new OrderedSet<String>();
            for (String groupID : groupList) {
                OrderedSet<String> d = DeviceGroup.getDeviceIDsForGroup(this.getAccountID(), groupID, null/*User*/, inclInactv);
                ListTools.toList((java.util.List<String>)d, list);
            }
            return list;
        } else {
            // -- no explicit defined groups, get all authorized devices
            if (this.getDefaultDeviceAuthorization()) {
                // -- all devices are authorized
                return Device.getDeviceIDsForAccount(this.getAccountID(), null, inclInactv, -1L);
            } else {
                // -- no devices are authorized
                return new OrderedSet<String>();
            }
        }
    }

    /* return ture if specified device is authorized for this User */
    public boolean isAuthorizedDevice(String deviceID)
        throws DBException
    {

        /* always allow "admin" user */
        if (this.isAdminUser()) {
            return true;
        }

        /* deviceID blank? */
        if (StringTools.isBlank(deviceID)) {
            return false; // blank deviceID not authorized 
        }

        /* preferred deviceID is authorized? [2.6.1-B44] */
        // -- User.authorizedPreferredDeviceID=false|true|only
        PreferredDeviceAuth prefDevAuth = User.GetPreferredDeviceAuth();
        if (!prefDevAuth.equals(PreferredDeviceAuth.FALSE)) {
            // -- check preferred device match
            String prefDevID = this.getPreferredDeviceID();
            if (!StringTools.isBlank(prefDevID) && deviceID.equalsIgnoreCase(prefDevID)) {
                return true; // authorized
            }
            // -- return false if only the preferred device can be authorized [2.6.3-B30]
            if (prefDevAuth.equals(PreferredDeviceAuth.ONLY)) {
                return false; // not authorized
            }
        }

        /* check deviceID in an authorized group */
        java.util.List<String> groupList = this.getDeviceGroups(false/*refresh*/);
        if (ListTools.isEmpty(groupList)) {
            // -- db.defaultDeviceAuthorization.ACCOUNT=true
            // -- db.defaultDeviceAuthorization=true
            return this.getDefaultDeviceAuthorization();
        } else {
            for (String groupID : groupList) {
                // -- authorized if the device exists in the DeviceGroup (DeviceList)
                if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
                    // -- always authorized for group 'all'
                    return true;
                } else
                if (DeviceGroup.exists(this.getAccountID(), groupID, deviceID)) {
                    return true;
                }
            }
            // does not exist in any authorized group
            Print.logInfo("Not authorized device for user '%s': %s", this.getUserID(), deviceID);
            return false;
        }

    }

    /* get the preferred/first authorized device for this user */
    public String getDefaultDeviceID(boolean inclInactv)
        throws DBException
    {

        /* first check preferred device */
        if (this.hasPreferredDeviceID()) {
            String devID = this.getPreferredDeviceID();
            try {
                if (Device.exists(this.getAccountID(),devID) && this.isAuthorizedDevice(devID)) {
                    return devID;
                }
            } catch (DBException dbe) {
                // -- 'Device.exists' error, ignore
            }
            // -- device does not exist, or not authorized for preferred device
        }

        /* check for first authorized device */
        java.util.List<String> groupList = User.getGroupsForUser(this.getAccountID(), this.getUserID(), 1L);
        if (ListTools.isEmpty(groupList)) {
            // -- no defined groups
            if (this.getDefaultDeviceAuthorization()) {
                // -- all devices are authorized, return first device
                OrderedSet<String> d = Device.getDeviceIDsForAccount(this.getAccountID(), null, inclInactv, 1);
                return !ListTools.isEmpty(d)? d.get(0) : null;
            } else {
                // -- no devices are authorized
                return null;
            }
        } else {
            String groupID = groupList.get(0);
            OrderedSet<String> d = DeviceGroup.getDeviceIDsForGroup(this.getAccountID(), groupID, null, inclInactv, 1L);
            return !ListTools.isEmpty(d)? d.get(0) : null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return list of all currently defined ACLs for this User */
    // does not return null
    public String[] getAclsForUser()
        throws DBException
    {
        String acctID = this.getAccountID();
        
        /* read ACLs for user */
        java.util.List<String> aclList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
        
            /* select */
            // DBSelect: SELECT aclID FROM UserAcl WHERE (accountID='acct') AND (userID='user') ORDER BY aclID
            DBSelect<UserAcl> dsel = new DBSelect<UserAcl>(UserAcl.getFactory());
            dsel.setSelectedFields(UserAcl.FLD_aclID);
            DBWhere dwh = dsel.createDBWhere();
            dsel.setWhere(dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(UserAcl.FLD_accountID,this.getAccountID()),
                    dwh.EQ(UserAcl.FLD_userID,this.getUserID())
                )
            ));
            dsel.setOrderByFields(UserAcl.FLD_aclID);
    
            /* get records */
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String aclId = rs.getString(UserAcl.FLD_aclID);
                aclList.add(aclId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting User ACL List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return aclList.toArray(new String[aclList.size()]);

    }

    // ------------------------------------------------------------------------

    /* to String value */
    public String toString()
    {
        return this.getAccountID() + "/" + this.getUserID();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return true if the specified user exists */
    public static boolean exists(String acctID, String userID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (userID != null)) {
            User.Key userKey = new User.Key(acctID, userID);
            return userKey.exists();
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    /* Return specified user (may return null) */
    public static User getUser(Account account, String userID)
        throws DBException
    {
        if (account == null) {
            throw new DBException("Account is null");
        } else
        if (userID == null) {
            throw new DBException("UserID is null");
        } else {
            String acctID = account.getAccountID();
            User.Key userKey = new User.Key(acctID, userID);
            if (userKey.exists()) {
                User user = userKey.getDBRecord(true);
                user.setAccount(account);
                return user;
            } else {
                return null;
            }
        }
    }

    /* Return specified user, create if specified (does not return null) */
    public static User getUser(Account account, String userId, boolean create)
        throws DBException
    {
        
        /* account-id specified? */
        if (account == null) {
            throw new DBNotFoundException("Account not specified.");
        }
        String acctId = account.getAccountID();

        /* user-id specified? */
        if ((userId == null) || userId.equals("")) {
            throw new DBNotFoundException("User-ID not specified.");
        }

        /* get/create user */
        User user = null;
        User.Key userKey = new User.Key(acctId, userId);
        if (!userKey.exists()) { // may throw DBException
            if (create) {
                user = userKey.getDBRecord();
                user.setAccount(account);
                user.setCreationDefaultValues();
                return user; // not yet saved!
            } else {
                throw new DBNotFoundException("User-ID does not exists '" + userKey + "'");
            }
        } else
        if (create) {
            // we've been asked to create the user, and it already exists
            throw new DBAlreadyExistsException("User-ID already exists '" + userKey + "'");
        } else {
            user = User.getUser(account, userId); // may throw DBException
            if (user == null) {
                throw new DBException("Unable to read existing User-ID '" + userKey + "'");
            }
            return user;
        }

    }

    /* Create specified user.  Return null if user already exists */
    public static User createNewUser(Account account, String userID, String contactEmail, String passwd)
        throws DBException
    {
        if ((account != null) && (userID != null) && !userID.equals("")) {
            // -- create user record (not yet saved)
            User user = User.getUser(account, userID, true); // does not return null
            // -- set contact email address
            if (contactEmail != null) {
                user.setContactEmail(contactEmail);
            }
            // -- set password
            if (passwd != null) {
                user.setDecodedPassword(null, passwd, true);
            }
            // -- save
            user.save();
            return user;
        } else {
            throw new DBNotFoundException("Invalid Account/UserID specified");
        }
    }

    // ------------------------------------------------------------------------

    /* return list of all Users owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getUsersForAccount(String acctId)
        throws DBException
    {
        return User.getUsersForAccount(acctId, -1);
    }

    /* return list of all Users owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getUsersForAccount(String acctId, int userType)
        throws DBException
    {

        /* invalid account */
        if ((acctId == null) || acctId.equals("")) {
            return new String[0];
        }

        /* select */
        // DBSelect: SELECT userID FROM User WHERE (accountID='acct') ORDER BY userID
        DBSelect<User> dsel = new DBSelect<User>(User.getFactory());
        dsel.setSelectedFields(User.FLD_userID);
        DBWhere dwh = dsel.createDBWhere();
        //dsel.setWhere(dwh.WHERE_(dwh.EQ(User.FLD_accountID,acctId)));
        dwh.append(dwh.EQ(User.FLD_accountID,acctId));
        if (userType >= 0) {
            // AND (userType=0)
            dwh.append(dwh.AND_(dwh.EQ(User.FLD_userType,userType)));
        }
        dsel.setWhere(dwh.WHERE(dwh.toString()));
        dsel.setOrderByFields(User.FLD_userID);

        /* select */
        return User.getUserIDs(dsel);
        
    }
    
    /* return list of all Users owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static String[] getUserIDs(DBSelect<User> dsel)
        throws DBException
    {

        /* invalid selection */
        if (dsel == null) {
            return new String[0];
        }
        dsel.setSelectedFields(User.FLD_userID);

        /* read users for account */
        java.util.List<String> userList = new Vector<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get records */
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs = stmt.getResultSet();
            while (rs.next()) {
                String userId = rs.getString(User.FLD_userID);
                userList.add(userId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account User List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return userList.toArray(new String[userList.size()]);

    }

    // ------------------------------------------------------------------------

    /* return the first user which specifies this email address as the contact email */
    public static User getUserForContactEmail(String acctId, String emailAddr)
        throws DBException
    {
        java.util.List<User> userList = User.getUsersForContactEmail(acctId, emailAddr);
        return !ListTools.isEmpty(userList)? userList.get(0) : null;
    }

    /* return all users which list this email address as the contact email */
    public static java.util.List<User> getUsersForContactEmail(String acctId, String emailAddr)
        throws DBException
    {
        java.util.List<User> userList = new Vector<User>();

        /* invalid account? */
        boolean acctIdBlank = StringTools.isBlank(acctId);
        //if (acctIdBlank) {
        //    return userList;
        //}

        /* EMailAddress specified? */
        if (StringTools.isBlank(emailAddr)) {
            return userList; // empty list
        }

        /* read users for contact email */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* select */
            // DBSelect: SELECT userID FROM User WHERE [(accountID='account') AND] (contactEmail='email')
            DBSelect<User> dsel = new DBSelect<User>(User.getFactory());
            //dsel.setSelectedFields(User.FLD_accountID,User.FLD_userID);
            DBWhere dwh = dsel.createDBWhere();
            if (acctIdBlank) {
                dwh.append(
                    dwh.EQ(User.FLD_contactEmail,emailAddr)
                );
            } else {
                dwh.append(dwh.AND(
                    dwh.EQ(User.FLD_accountID   ,acctId),
                    dwh.EQ(User.FLD_contactEmail,emailAddr)
                ));
            }
            dsel.setWhere(dwh.WHERE(dwh.toString()));
            dsel.setOrderByFields(User.FLD_userID);
            // -- Note: The index on the column FLD_contactEmail is not unique

            /* get records */
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String aid = rs.getString(User.FLD_accountID);
                String uid = rs.getString(User.FLD_userID);
                User user = new User(new User.Key(aid, uid));
                user.setAllFieldValues(rs);
                userList.add(user);
            }

        } catch (SQLException sqe) {
            throw new DBException("Get User ContactEmail", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        return userList;
    }

    // ------------------------------------------------------------------------

    /* return the DBSelect statement for the specified account/user */
    protected static DBSelect<User> _getUsersForRoleSelect(String acctID, String roleID, long limit)
    {

        /* invalid accountID? */
        if (StringTools.isBlank(acctID)) {
            return null;
        }

        /* invalid roleID? */
        if (StringTools.isBlank(roleID)) {
            return null;
        }

        /* select */
        // DBSelect: SELECT accountID,userID FROM User WHERE (accountID='account') AND (roleID='role')
        DBSelect<User> dsel = new DBSelect<User>(User.getFactory());
        dsel.setSelectedFields(User.FLD_accountID,User.FLD_userID);
        DBWhere dwh = dsel.createDBWhere();
        dwh.append(dwh.AND(
            dwh.EQ(User.FLD_accountID, acctID),
            dwh.EQ(User.FLD_roleID   , roleID)
        ));
        dsel.setWhere(dwh.WHERE(dwh.toString()));
        dsel.setOrderByFields(User.FLD_userID);
        dsel.setLimit(limit);
        return dsel;

    }

    /* return true if there are any users that reference the specified role */
    public static boolean hasUserIDsForRole(String acctID, String roleID)
        throws DBException
    {
        return !ListTools.isEmpty(User.getUserIDsForRole(acctID, roleID, 1L));
    }

    /* return all user IDs for the specified role ID */
    public static java.util.List<String> getUserIDsForRole(String acctID, String roleID)
        throws DBException
    {
        return User.getUserIDsForRole(acctID, roleID, -1L);
    }

    /* return all user IDs for the specified role ID */
    public static long countUserIDsForRole(String acctID, String roleID)
        throws DBException
    {

        /* valid account/roleId? */
        if (StringTools.isBlank(acctID)) {
            return 0L;
        } else
        if (StringTools.isBlank(roleID)) {
            return 0L;
        }

        /* get db selector */
        DBSelect<User> dsel = User._getUsersForRoleSelect(acctID, roleID, -1);
        if (dsel == null) {
            return 0L;
        }

        /* count users */
        long recordCount = 0L;
        try {
            DBProvider.lockTables(new String[] { TABLE_NAME() }, null);
            recordCount = DBRecord.getRecordCount(dsel);
        } finally {
            DBProvider.unlockTables();
        }
        return recordCount;

    }

    /* return all user IDs for the specified role ID */
    public static java.util.List<String> getUserIDsForRole(String acctID, String roleID, long limit)
        throws DBException
    {
        java.util.List<String> userList = new Vector<String>();

        /* valid account/roleId? */
        if (StringTools.isBlank(acctID)) {
            return null;
        } else
        if (StringTools.isBlank(roleID)) {
            return null;
        }

        /* get db selector */
        DBSelect<User> dsel = User._getUsersForRoleSelect(acctID, roleID, limit);
        if (dsel == null) {
            return null;
        }

        /* read users for roleID */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDBConnection_read();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String aid = rs.getString(User.FLD_accountID);
                String uid = rs.getString(User.FLD_userID);
                userList.add(uid);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Users for Role", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        return userList;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // This section supports a method for obtaining human readable information 
    // from the User record for reporting, or email purposes. (currently 
    // this is used by the 'rules' engine when generating notification emails).

    private static final String KEY_ACCOUNT[]       = EventData.KEY_ACCOUNT;
    private static final String KEY_ACCOUNT_ID[]    = EventData.KEY_ACCOUNT_ID;
    private static final String KEY_USER[]          = { "user"     , "userDesc" };  // 
    private static final String KEY_USER_ID[]       = { "userID"                };  // 
    private static final String KEY_CONTACT_NAME[]  = { "contactName"           };
    private static final String KEY_CONTACT_EMAIL[] = { "contactEmail"          };
    private static final String KEY_CONTACT_PHONE[] = { "contactPhone"          };
    private static final String KEY_TEMP_PASSWORD[] = { "tempPassword"          };

    private static final String KEY_ADDRESS_1[]     = { "addressLine1"          };
    private static final String KEY_ADDRESS_2[]     = { "addressLine2"          };
    private static final String KEY_ADDRESS_3[]     = { "addressLine3"          };
    private static final String KEY_ADDRESS_CITY[]  = { "addressCity"           };
    private static final String KEY_ADDRESS_STATE[] = { "addressState"          };
    private static final String KEY_ADDRESS_ZIP[]   = { "addressZip"            };

    private static final String KEY_DATETIME[]      = EventData.KEY_DATETIME;
    private static final String KEY_DATE_YEAR[]     = EventData.KEY_DATE_YEAR;
    private static final String KEY_DATE_MONTH[]    = EventData.KEY_DATE_MONTH;
    private static final String KEY_DATE_DAY[]      = EventData.KEY_DATE_DAY;
    private static final String KEY_DATE_DOW[]      = EventData.KEY_DATE_DOW;
    private static final String KEY_TIME[]          = EventData.KEY_TIME;
    
    public static String getKeyFieldTitle(String key, String arg, Locale locale)
    {
        return User._getKeyFieldString(
            true/*title*/, key, arg, 
            locale, null/*BasicPrivateLabel*/, null/*User*/);
    }

    // getFieldValueString
    public String getKeyFieldValue(String key, String arg, BasicPrivateLabel bpl)
    {
        Locale locale = (bpl != null)? bpl.getLocale() : null;
        return User._getKeyFieldString(
            false/*value*/, key, arg, 
            locale, bpl, this);
    }

    public static String _getKeyFieldString(
        boolean getTitle, String key, String arg, 
        Locale locale, BasicPrivateLabel bpl, User user)
    {

        /* check for valid field name */
        if (key == null) {
            return null;
        } else
        if ((user == null) && !getTitle) {
            return null; // user required for value (not for title)
        }
        if ((locale == null) && (bpl != null)) { locale = bpl.getLocale(); }
        I18N i18n = I18N.getI18N(Account.class, locale);
        long now = DateTime.getCurrentTimeSec();

        /* Account */
        if (EventData._keyMatch(key,User.KEY_ACCOUNT)) {
            if (getTitle) {
                return i18n.getString("User.key.accountDescription", "Account");
            } else {
                Account account = user.getAccount();
                if (account == null) {
                    return user.getAccountID();
                } else
                if ((arg != null) && arg.equalsIgnoreCase("id")) {
                    return user.getAccountID();
                } else {
                    return account.getDescription();
                }
            }
        } else
        if (EventData._keyMatch(key,User.KEY_ACCOUNT_ID)) {
            if (getTitle) {
                return i18n.getString("User.key.accountID", "Account-ID");
            } else {
                return user.getAccountID();
            }
        }

        /* User */
        if (EventData._keyMatch(key,User.KEY_USER)) {
            if (getTitle) {
                return i18n.getString("User.key.userDescription", "Account");
            } else {
                if ((arg != null) && arg.equalsIgnoreCase("id")) {
                    return user.getUserID();
                } else {
                    String d = user.getDescription();
                    return !StringTools.isBlank(d)? d : user.getContactName();
                }
            }
        } else
        if (EventData._keyMatch(key,User.KEY_USER_ID)) {
            if (getTitle) {
                return i18n.getString("User.key.userID", "User-ID");
            } else {
                return user.getUserID();
            }
        } 

        /* Contact */
        if (EventData._keyMatch(key,User.KEY_CONTACT_NAME)) {
            if (getTitle) {
                return i18n.getString("User.key.contactName", "Contact Name");
            } else {
                String cn = user.getContactName();
                return !StringTools.isBlank(cn)? cn : user.getDescription();
            }
        } else
        if (EventData._keyMatch(key,User.KEY_CONTACT_EMAIL)) {
            if (getTitle) {
                return i18n.getString("User.key.contactEmail", "Contact EMail");
            } else {
                return user.getContactEmail();
            }
        } else
        if (EventData._keyMatch(key,User.KEY_CONTACT_PHONE)) {
            if (getTitle) {
                return i18n.getString("User.key.contactPhone", "Contact Phone");
            } else {
                return user.getContactPhone();
            }
        }

        /* Temporary Password */
        if (EventData._keyMatch(key,User.KEY_TEMP_PASSWORD)) {
            if (getTitle) {
                return i18n.getString("User.key.temporaryPassword", "Temporary Password");
            } else {
                return user.getTempPassword();
            }
        }

        /* Address */
        if (EventData._keyMatch(key,User.KEY_ADDRESS_1)) {
            if (getTitle) {
                return i18n.getString("User.key.addressLine1", "Address-1");
            } else {
                return user.getAddressLine1();
            }
        } else
        if (EventData._keyMatch(key,User.KEY_ADDRESS_2)) {
            if (getTitle) {
                return i18n.getString("User.key.addressLine2", "Address-2");
            } else {
                return user.getAddressLine2();
            }
        } else
        if (EventData._keyMatch(key,User.KEY_ADDRESS_3)) {
            if (getTitle) {
                return i18n.getString("User.key.addressLine3", "Address-3");
            } else {
                return user.getAddressLine3();
            }
        } else
        if (EventData._keyMatch(key,User.KEY_ADDRESS_CITY)) {
            if (getTitle) {
                return i18n.getString("User.key.addressCity", "City");
            } else {
                return user.getAddressCity();
            }
        } else
        if (EventData._keyMatch(key,User.KEY_ADDRESS_STATE)) {
            if (getTitle) {
                return i18n.getString("User.key.addressState", "State");
            } else {
                return user.getAddressState();
            }
        } else
        if (EventData._keyMatch(key,User.KEY_ADDRESS_ZIP)) {
            if (getTitle) {
                return i18n.getString("User.key.addressPostalCode", "Zip");
            } else {
                return user.getAddressPostalCode();
            }
        }

        /* Date/Time */
        if (EventData._keyMatch(key,User.KEY_DATETIME)) {
            if (getTitle) {
                return i18n.getString("User.key.dateTime", "Date/Time");
            } else {
                TimeZone tmz = user.getTimeZone(null); // non-null
                Account account = user.getAccount();
                return EventData.getTimestampString(now, account, tmz, bpl);
            }
        } else
        if (EventData._keyMatch(key,User.KEY_DATE_YEAR)) {
            if (getTitle) {
                return i18n.getString("User.key.dateYear", "Year");
            } else {
                TimeZone tmz = user.getTimeZone(null); // non-null
                return EventData.getTimestampYear(now, tmz);
            }
        } else
        if (EventData._keyMatch(key,User.KEY_DATE_MONTH)) {
            if (getTitle) {
                return i18n.getString("User.key.dateMonth", "Month");
            } else {
                TimeZone tmz = user.getTimeZone(null); // non-null
                return EventData.getTimestampMonth(now, false, tmz, locale);
            }
        } else
        if (EventData._keyMatch(key,User.KEY_DATE_DAY)) {
            if (getTitle) {
                return i18n.getString("User.key.dateDay", "Day");
            } else {
                TimeZone tmz = user.getTimeZone(null); // non-null
                return EventData.getTimestampDayOfMonth(now, tmz);
            }
        } else
        if (EventData._keyMatch(key,User.KEY_DATE_DOW)) {
            if (getTitle) {
                return i18n.getString("User.key.dayOfWeek", "Day Of Week");
            } else {
                TimeZone tmz = user.getTimeZone(null); // non-null
                return EventData.getTimestampDayOfWeek(now, false, tmz, locale);
            }
        } else
        if (EventData._keyMatch(key,User.KEY_TIME)) {
            if (getTitle) {
                return i18n.getString("User.key.time", "Time");
            } else {
                TimeZone tmz = user.getTimeZone(null); // non-null
                Account account = user.getAccount();
                return EventData.getTimestampString(now, account, tmz, bpl);
            }
        }

        /* User fields */
        if (getTitle) {
            DBField dbFld = User.getFactory().getField(key);
            if (dbFld != null) {
                return dbFld.getTitle(locale);
            }
            // -- field not found
        } else {
            String fldName = user.getFieldName(key); // this gets the field name with proper case
            DBField dbFld = (fldName != null)? user.getField(fldName) : null;
            if (dbFld != null) {
                Object val = user.getFieldValue(fldName); // straight from table
                if (val == null) { val = dbFld.getDefaultValue(); }
                Account account = user.getAccount();
                if (account != null) {
                    val = account.convertFieldUnits(dbFld, val, true/*inclUnits*/, locale);
                    return StringTools.trim(val);
                } else {
                    return dbFld.formatValue(val);
                }
            }
            // -- field not found
        }

        /* try temporary properties */
        if (user.hasTemporaryProperties()) {
            RTProperties rtp = user.getTemporaryProperties();
            Object text = (rtp != null)? rtp.getProperty(key,null) : null;
            if (text instanceof I18N.Text) {
                if (getTitle) {
                    // -- all we have is the key name for the title
                    return key;
                } else {
                    // -- return Localized version of value
                    return ((I18N.Text)text).toString(locale);
                }
            }
        }

        // ----------------------------
        // User key not found

        /* not found */
        //Print.logWarn("User key not found: " + key);
        return null;

    }

    // ------------------------------------------------------------------------
    
    // -- special case replacement vars
    private static final String KEY_PASSWORD[]      = new String[] { "password"              };  // 

    private static final String START_DELIM         = "${";
    private static final String END_DELIM           = "}";
    private static final String DFT_DELIM           = "=";

    /**
    *** Insert User replacement values in specified text String
    **/
    public static String insertUserKeyValues(User user, String text)
    {
        if (user != null) {
            return StringTools.insertKeyValues(text, 
                START_DELIM, END_DELIM, DFT_DELIM,
                new User.UserValueMap(user));
        } else {
            return null;
        }
    }

    /**
    *** Insert User replacement values in specified text String
    **/
    public String insertUserKeyValues(String text)
    {
        return User.insertUserKeyValues(this, text);
    }

    public static class UserValueMap
        implements StringTools.KeyValueMap // ReplacementMap
    {
        private User              user      = null;
        private BasicPrivateLabel privLabel = null;
        public UserValueMap(User user) {
            this.user      = user;
            this.privLabel = null;
        }
        public String getKeyValue(String key, String arg, String dft) {
            if (EventData._keyMatch(key,User.KEY_PASSWORD)) {
                if (this.user != null) {
                    String pwd = this.user.getDecodedPassword(this.privLabel);
                    return (pwd != null)? pwd : dft;
                } else {
                    return dft;
                }
            } else
            if (this.user != null) {
                String fldStr = this.user.getKeyFieldValue(key,arg,this.privLabel);
                return (fldStr != null)? fldStr : dft; // "("+key+")";
            } else {
                //Print.logWarn("Key not found: " + key);
                return dft; // "("+key+")";
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final int ID_LEN = 32;

    public static class UserLoadValidator
        extends DBLoadValidatorAdapter
    {
        private   Account account          = null;
        private   boolean filterIDs        = false;
        private   boolean validateOnly     = false;
        public UserLoadValidator(Account acct, boolean filtIDs, boolean valOnly) throws DBException {
            this.account      = acct;
            this.filterIDs    = filtIDs;
            this.validateOnly = valOnly;
        }
        // --
        public Account getAccount() {
            return this.account;
        }
        public String getAccountID() {
            return (this.account != null)? this.account.getAccountID() : "";
        }
        // --
        public boolean isFilterIDs() {
            return this.filterIDs;
        }
        // --
        public boolean isValidateOnly() {
            return this.validateOnly;
        }
        // -- set fields that will be inserted/updated
        public boolean setFields(String f[]) throws DBException {
            if (!super.setFields(f)) {
                return false;
            }
            // -- required fields
            if (!this.hasField(FLD_accountID)) {
                Print.logError("Load file is missing column: " + FLD_accountID);
                this.setError();
                return false;
            }
            if (!this.hasField(FLD_userID)) {
                Print.logError("Load file is missing column: " + FLD_userID);
                this.setError();
                return false;
            }
            // -- success
            return true;
        }
        // -- validate field values
        public boolean validateValues(String v[]) throws DBException {
            if (!super.validateValues(v)) {
                return false;
            }
            // -- validate accountID
            String accountID = this.getFieldValue(FLD_accountID,v);
            if (StringTools.isBlank(accountID)) {
                Print.logError("Blank/Null AccountID found: [#" + this.getCount() + "] " + accountID);
                this.setError();
                return false;
            } else
            if (!accountID.equals(this.getAccountID())) {
                Print.logError("Unexpected AccountID: [#" + this.getCount() + "] found '" + accountID + "', expected '"+this.getAccountID()+"'");
                this.setError();
                return false;
            }
            // -- validate userID
            String userID = this.getFieldValue(FLD_userID,v);
            if (this.isFilterIDs()) {
                userID = AccountRecord.getFilteredID(userID,false/*noNull*/,true/*lowerCase*/,true/*strict*/);
                if (userID.length() > ID_LEN) { userID = userID.substring(0,ID_LEN); }
                this.setFieldValue(FLD_userID,v,userID);
            }
            if (StringTools.isBlank(userID)) {
                Print.logError("Blank/Null UserID found: [#" + this.getCount() + "] " + userID);
                this.setError();
                return false;
            } else
            if (!AccountRecord.isValidID(userID)) {
                Print.logError("Invalid UserID found: [#" + this.getCount() + "] " + userID);
                this.setError();
                return false;
            } else
            if (userID.length() > ID_LEN) {
                Print.logError("UserID exceeds maximum ID length: [#" + this.getCount() + "] " + userID);
                this.setError();
                return false;
            }
            // -- validate tempPassword (if present, must not be blank)
            String tempPass = this.getFieldValue(FLD_tempPassword,v);
            if ((tempPass != null) && StringTools.isBlank(tempPass)) {
                Print.logError("Blank Temporary Password found: [#" + this.getCount() + "]");
                this.setError();
                return false;
            }
            // -- force invalid if validate only
            if (this.isValidateOnly()) {
                return false; // force invalid
            }
            // -- ok
            return true;
        }
        // -- validate record for insertion
        public boolean validateInsert(DBRecord<?> dbr) throws DBException {
            return super.validateInsert(dbr);
        }
        public void recordDidInsert(DBRecord<?> dbr) {
            this.recordDidInsertUpdate(dbr, true, null);
        }
        // -- validate record for update
        public boolean validateUpdate(DBRecord<?> dbr, Set<String> updFields) throws DBException {
            return super.validateUpdate(dbr, updFields);
        }
        public void recordDidUpdate(DBRecord<?> dbr, Set<String> updFields) {
            this.recordDidInsertUpdate(dbr, false, updFields);
        }
        // -- validate record for insert/update
        public boolean validateRecord(DBRecord<?> dbr, boolean newRecord, Set<String> updFields) throws DBException {
            this._encodePassword(dbr,newRecord,updFields); // encode temporary password
            return true;
        }
        protected void recordDidInsertUpdate(DBRecord<?> dbr, boolean newRecord) {
            // --
        }
        // --
        protected void _encodePassword(DBRecord<?> dbr, boolean newRecord, Set<String> updFields) {
            if ((dbr != null) && this.hasField(FLD_tempPassword)) {
                User user = (User)dbr;
                String tempPass = user.getTempPassword();
                user.setDecodedPassword(null,tempPass,true/*isTemp*/);
                if ((updFields != null) && !updFields.contains(FLD_password)) {
                    updFields.add(FLD_password); // add "password" field for update
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Main admin entry point below

    private static final String ARG_ACCOUNT[]   = { "account" , "acct"      , "a" };
    private static final String ARG_USER[]      = { "user"    , "usr"       , "u" };
    private static final String ARG_EMAIL[]     = { "email"                 };
    private static final String ARG_CREATE[]    = { "create"  , "cr"        };
    private static final String ARG_NOPASS[]    = { "nopass"                };
    private static final String ARG_PASSWORD[]  = { "password", "passwd"    , "pass" };
    private static final String ARG_EDIT[]      = { "edit"    , "ed"        };
    private static final String ARG_EDITALL[]   = { "editall" , "eda"       };
    private static final String ARG_DELETE[]    = { "delete"  , "purge"     };
    private static final String ARG_LIST[]      = { "list"                  };

    private static final String ARG_LOAD[]      = { "load"    , "import"    };
    private static final String ARG_OVERWRITE[] = { "overwrite"             }; // -overwrite=true
    private static final String ARG_VALIDATE[]  = { "validate"              }; // -validate=true
    private static final String ARG_FILTERID[]  = { "filterID", "filter"    }; // -filter=true
    private static final String ARG_VALIDATOR[] = { "validate", "validator" }; // -validator=CLASS_NAME

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + User.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>   Acount ID which owns User");
        Print.logInfo("  -user=<id>      User ID to create/edit");
        Print.logInfo("  -create         Create a new User");
        Print.logInfo("  -edit           Edit an existing (or newly created) User");
        Print.logInfo("  -delete         Delete specified User");
        Print.logInfo("  -list           List Users for Account");
        System.exit(1);
    }

    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args,true);  // main
        String acctID = RTConfig.getString(ARG_ACCOUNT, "");
        String userID = RTConfig.getString(ARG_USER   , "");

        /* option count */
        int opts = 0;

        /* account-id specified? */
        if (StringTools.isBlank(acctID)) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID); // may return DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + acctID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* list */
        if (RTConfig.getBoolean(ARG_LIST, false)) {
            opts++;
            try {
                Print.logInfo("Account: " + acctID);
                String userList[] = User.getUsersForAccount(acctID);
                for (int i = 0; i < userList.length; i++) {
                    Print.logInfo("  User: " + userList[i]);
                }
            } catch (DBException dbe) {
                Print.logError("Error listing Users: " + acctID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* load */
        if (RTConfig.hasProperty(ARG_LOAD)) {
            opts++;
            SendMail.SetThreadModel(SendMail.THREAD_CURRENT);
            // -- get load file
            String  loadFileName = RTConfig.getString(ARG_LOAD,"");
            File    loadFile     = !StringTools.isBlank(loadFileName)? new File(loadFileName) : null;
            if (!FileTools.isFile(loadFile)) {
                Print.sysPrintln("ERROR: Load file does not exist: " + loadFile);
                System.exit(99);
            } else
            if (!FileTools.hasExtension(loadFile,"csv")) {
                Print.sysPrintln("ERROR: Load file does not have '.csv' extension: " + loadFile);
                System.exit(99);
            }
            // -- parameters
            boolean overwrite     = RTConfig.getBoolean(ARG_OVERWRITE,false);// overwrite existing
            boolean validateOnly  = RTConfig.hasProperty(ARG_VALIDATE);      // validate (no insert)
            boolean filterIDs     = RTConfig.getBoolean(ARG_FILTERID,false); // filter/adjust userID
            boolean insertRecord  = !validateOnly;
            boolean noDropWarning = false;
            // -- UserLoadValidator
            UserLoadValidator usrLoadVal;
            String userValCN = RTConfig.getString(ARG_VALIDATOR,null);
            if (!StringTools.isBlank(userValCN)) {
                try {
                    Class<?> userValClass = Class.forName(userValCN);
                    MethodAction ma = new MethodAction(userValClass,Account.class,Boolean.TYPE,Boolean.TYPE);
                    usrLoadVal = (UserLoadValidator)ma.invoke(acct,filterIDs,validateOnly); // non-null
                } catch (Throwable th) { 
                    // -- ClassNotFoundException, NoSuchMethodException, 
                    Print.logException("ERROR: unable to instantiate custom UserLoadValidator: "+userValCN, th);
                    System.exit(99);
                    // -- control does not reach here 
                    // -  (below only needed to keep compiler from complaining about "usrLoadVal")
                    return;
                }
            } else {
                try {
                    usrLoadVal = new UserLoadValidator(acct,filterIDs,validateOnly); // non-null
                } catch (Throwable th) {
                    Print.logException("ERROR: unable to instantiate UserLoadValidator", th);
                    System.exit(99);
                    // -- control does not reach here 
                    // -  (below only needed to keep compiler from complaining about "usrLoadVal")
                    return;
                }
            }
            Print.sysPrintln("UserLoadValidator: " + StringTools.className(usrLoadVal));
            if (usrLoadVal.hasErrors()) {
                Print.logError("UserLoadValidator has errors");
                System.exit(99);
            }
            // -- load User table
            DBFactory<User> fact = User.getFactory();
            String mode = validateOnly? "Validating" : "Loading";
            try {
                Print.sysPrintln(mode + " file: " + loadFile);
                if (!fact.tableExists()) {
                    Print.sysPrintln("ERROR: Table does not exist: " + TABLE_NAME());
                    System.exit(99);
                }
                fact.loadTable(loadFile,usrLoadVal,insertRecord,overwrite,noDropWarning);
                if (usrLoadVal.hasErrors()) {
                    Print.sysPrintln(mode+" error encountered: " + loadFile);
                    System.exit(1);
                } else {
                    Print.sysPrintln(mode+" successful: " + loadFile);
                }
            } catch (DBException dbe) {
                Print.logException(mode+" error encountered", dbe);
                System.exit(1);
            }
            // -- done
            System.exit(0);
        }

        // ---------------------------------------------
        // -- the following require a "-user" specification

        /* user-id specified? */
        if (StringTools.isBlank(userID)) {
            Print.logError("User-ID not specified.");
            usage();
        }

        /* user exists? */
        boolean userExists = false;
        try {
            userExists = User.exists(acctID, userID);
        } catch (DBException dbe) {
            Print.logError("Error determining if User exists: " + acctID + "," + userID);
            System.exit(99);
        }

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false) && !acctID.equals("") && !userID.equals("")) {
            opts++;
            if (!userExists) {
                Print.logWarn("User does not exist: " + acctID + "/" + userID);
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                User.Key userKey = new User.Key(acctID, userID);
                userKey.delete(true); // also delete dependencies
                Print.logInfo("User deleted: " + acctID + "/" + userID);
            } catch (DBException dbe) {
                Print.logError("Error deleting User: " + acctID + "/" + userID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (userExists) {
                Print.logWarn("User already exists: " + acctID + "/" + userID);
            } else {
                String contactEmail = RTConfig.getString(ARG_EMAIL, "");
                try {
                    String passwd = null;
                    if (RTConfig.getBoolean(ARG_NOPASS,false)) {
                        passwd = BLANK_PASSWORD;
                    } else
                    if (RTConfig.hasProperty(ARG_PASSWORD)) {
                        passwd = RTConfig.getString(ARG_PASSWORD,"");
                    }
                    User.createNewUser(acct, userID, contactEmail, passwd);
                    Print.logInfo("Created User-ID: " + acctID + "/" + userID);
                } catch (DBException dbe) {
                    Print.logError("Error creating User: " + acctID + "/" + userID);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT,false) || RTConfig.getBoolean(ARG_EDITALL,false)) {
            opts++;
            if (!userExists) {
                Print.logError("User does not exist: " + acctID + "/" + userID);
            } else {
                try {
                    boolean allFlds = RTConfig.getBoolean(ARG_EDITALL,false);
                    User user = User.getUser(acct, userID, false); // may throw DBException
                    DBEdit editor = new DBEdit(user);
                    editor.edit(allFlds); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing User: " + acctID + "/" + userID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }

    }

    // ------------------------------------------------------------------------

}
