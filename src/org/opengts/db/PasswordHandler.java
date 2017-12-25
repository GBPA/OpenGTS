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
//  2009/09/23  Martin D. Flynn
//     -Initial release
//  2012/04/03  Martin D. Flynn
//     -Added "validateNewPassword", "getPasswordFormatDescription", "hasPasswordExpired"
//  2016/09/01  Martin D. Flynn
//     -Added an array of the last used encoded passwords to "validateNewPassword"
//     -Added parameter to "validateNewPassword" to return the reason for invalid password
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.Locale;

import org.opengts.util.*;

public interface PasswordHandler
{

    /**
    *** Gets the name of this instance
    *** @return The name of this instance
    **/
    public String getName();

    // ------------------------------------------------------------------------

    /**
    *** Encode/Encrypt password
    *** @param userPass  Password to encode/encrypt
    *** @return The encoded/encrypted password
    **/
    public String encodePassword(String userPass);

    /**
    *** Decode/Decrypt password
    *** @param tablePass  Password to decode/decrypt
    *** @return The decoded/decrypted password, or null if the password cannot be decoded/decrypted
    **/
    public String decodePassword(String tablePass);

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the entered password matches the password saved in the table
    *** @param enteredPass  User entered password
    *** @param tablePass    The password saved in the table (possibly encrypted)
    *** @return True if password match, false otherwise
    **/
    public boolean checkPassword(String enteredPass, String tablePass);

    // ------------------------------------------------------------------------

    /**
    *** Gets the number of previous passwords to check for uniqueness
    *** @return The number of previous passwords to check.
    **/
    public int getRequiredUniquePasswordCount();

    /**
    *** Returns true if the new password adheres to the password policy
    *** @param newPass     The new password candidate (decoded)
    *** @param oldEncPass  List of old encoded passwords (encoded)
    *** @param msg         The returned error message describing the reason for a failed validation
    *** @return Null if the password adheres to the password policy, otherwise I18N.Text error message
    **/
    public boolean validateNewPassword(String newPass, String oldEncPass[], I18N.Text msg);

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the password has expired (time for new password)
    *** @param lastChangedTime  Time of last password change
    *** @return True is the password has expired
    **/
    public boolean hasPasswordExpired(long lastChangedTime);

    // ------------------------------------------------------------------------

    /**
    *** Gets the maximum failed login attempts
    *** @return The maximum failed login attempts
    **/
    public int getFailedLoginMaximumAttempts();

    /**
    *** Gets the maximum failed login attempt interval (in seconds)
    *** @return The maximum failed login attempt interval (in seconds).
    **/
    public long getFailedLoginAttemptInterval();

    /**
    *** Gets the suspend interval on failed login (in seconds)
    *** @return The suspend interval on failed login (in seconds)
    **/
    public long getFailedLoginSuspendInterval();

    /**
    *** Returns true if suspend on failed login attempts is enabled
    *** @return True if suspend on failed login attempts is enabled
    **/
    public boolean getFailedLoginSuspendEnabled();

    /**
    *** Checks the maximum failed login attempts, and returns a suspend time.
    *** @param failedLoginAttempts  The current number of failed login attempts
    *** @param asOfTimeSec The time of the latest failed login attempt
    **/
    public long getFailedLoginAttemptSuspendTime(int failedLoginAttempts, long asOfTimeSec);

    // ------------------------------------------------------------------------

    /**
    *** Gets the short text description of the acceptable password format
    *** @param locale  The language locale
    *** @return The short text description of the acceptable password format
    **/
    public String getPasswordFormatDescription(Locale locale);

}
