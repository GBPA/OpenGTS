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
//  2015/05/03  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class Recipients
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Set<String> NewSet()
    {
      //return new HashSet<String>();
        return new OrderedSet<String>(); // maintain sequential order
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Extracts and returns all email recipients in the specified comma-separated recipient list
    **/
    public static String extractEmailRecipients(String recipCSV)
    {
        Recipients r = new Recipients(recipCSV); // comma-separated list
        return r.getEmailRecipientsString(); // EMail only (empty String is no email recipients defined)
    }

    /**
    *** Extracts and returns all SMS recipients in the specified comma-separated recipient list
    **/
    public static String extractSmsRecipients(String recipCSV)
    {
        Recipients r = new Recipients(recipCSV); // comma-separated list
        return r.getSmsRecipientsString(); // SMS only (empty String is no SMS recipients defined)
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Set<String>     recipEMail = null;
    private Set<String>     recipSMS   = null;

    /**
    *** Constructor: Empty list of recipients
    **/
    public Recipients() 
    {
        this.recipEMail = null;
        this.recipSMS   = null;
    }

    /**
    *** Constructor: Set containing a list of EMail/SMS recipients
    **/
    public Recipients(Set<String> recipSet) 
    {
        this();
        this.addRecipients(recipSet);
    }

    /**
    *** Constructor: String containing a comma-separated list of EMail/SMS recipients
    **/
    public Recipients(String recipCSV) 
    {
        this();
        this.addRecipients(recipCSV);
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the specified comma-separated list of recipients to this instance
    **/
    public void addRecipients(String recipCSV)
    {

        /* no recipients specified? */
        if (StringTools.isBlank(recipCSV)) {
            return;
        }

        /* add recipients */
        Set<String> recipSet = ListTools.toSet(StringTools.split(recipCSV,','),NewSet());
        this.addRecipients(recipSet);

    }

    /**
    *** Adds the specified set of recipients to this instance
    **/
    public void addRecipients(Set<String> recipSet)
    {

        /* no recipients specified? */
        if (ListTools.isEmpty(recipSet)) {
            return;
        }

        /* add list of recipients */
        for (String R : recipSet) {
            //Print.logInfo("Checking: " + R);
            if (SMSOutboundGateway.StartsWithSMS(R)) { // separate SMS
                // -- SMS phone numbers
                String PH = SMSOutboundGateway.RemovePrefixSMS(R);
                if (!StringTools.isBlank(PH)) {
                    if (this.recipSMS == null) { this.recipSMS = NewSet(); }
                    if (!this.recipSMS.contains(PH)) {
                        // -- add if not already present
                        //Print.logInfo("Adding SMS#: " + PH);
                        this.recipSMS.add(PH);
                    }
                }
            } else {
                // -- assume email addresses
                String EM = R;
                if (!StringTools.isBlank(EM)) {
                    if (this.recipEMail == null) { this.recipEMail = NewSet(); }
                    if (!this.recipEMail.contains(EM)) {
                        // -- add if not already present
                        //Print.logInfo("Adding Email: " + EM);
                        this.recipEMail.add(EM); // as-is
                    }
                }
            }
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the set of email recipients (null if no email recipients specified)
    **/
    public Set<String> getEmailRecipients() 
    {
        return this.recipEMail; // may be null
    }

    /**
    *** Returns the comma-separated String of email recipients
    **/
    public String getEmailRecipientsString() 
    {
        Set<String> rs = this.getEmailRecipients(); // may be null

        /* no email addresses? */
        if (ListTools.isEmpty(rs)) {
            return "";
        }

        /* return comma-separated list */
        return StringTools.join(rs,",");

    }

    /**
    *** Returns true if this instance defined at least one email address
    **/
    public boolean hasEmailRecipients()
    {
        return !ListTools.isEmpty(this.getEmailRecipients());
    }

    // --------------------------------

    /**
    *** Validates contained email addresses
    **/
    public boolean validateEmailRecipients()
    {
        Set<String> rs = this.getEmailRecipients(); // may be null

        /* no email addresses? */
        if (ListTools.isEmpty(rs)) {
            // -- no email addresses, thus all "contained" email addresses are valid
            return true;
        }

        /* validate individual email addresses */
        for (String R : rs) {
             if (StringTools.isBlank(R)) {
                // -- cannot be null/blank
                return false; 
            } else
            if (!SendMail.validateAddress(R)) {
                // -- email address is invalid
                return false; 
            }
        }

        /* all are valid */
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the set of SMS recipients (null if no SMS recipients specified)
    **/
    public Set<String> getSmsRecipients() 
    {
        return this.recipSMS; // may be null
    }

    /**
    *** Returns the comma-separated String of SMS recipients
    **/
    public String getSmsRecipientsString() 
    {
        Set<String> rs = this.getSmsRecipients();

        /* no SMS phone numbers? */
        if (ListTools.isEmpty(rs)) {
            return "";
        }

        /* return comma-separated list */
        return StringTools.join(rs,",");

    }

    /**
    *** Returns true if this instance defined at least one sms number
    **/
    public boolean hasSmsRecipients()
    {
        return !ListTools.isEmpty(this.getSmsRecipients());
    }

    // --------------------------------

    /**
    *** Validates contained SMS phone numbers
    **/
    public boolean validateSmsRecipients()
    {
        Set<String> rs = this.getSmsRecipients();

        /* no SMS phone numbers? */
        if (ListTools.isEmpty(rs)) {
            // -- no SMS phone numbers, thus all "contained" sms phone#s are valid
            return true;
        }

        /* validate individual SMS phone numbers */
        for (String R : rs) {
            if (StringTools.isBlank(R)) {
                // -- cannot be null/blank
                return false; 
            } else
            if (!StringTools.isNumeric(R)) {
                // -- contains non-digits
                return false; 
            }
        }

        /* all are valid */
        return true;

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this instance defines at least one recipient
    **/
    public boolean hasRecipients()
    {
        return this.hasEmailRecipients() || this.hasSmsRecipients();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance (debug only)
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("EMail: " + this.getEmailRecipientsString());
        sb.append("\n");
        sb.append("SMS  : " + this.getSmsRecipientsString());
        sb.append("\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);

        /* parse recipients */
        String recip = RTConfig.getString("recip",null);
        if (!StringTools.isBlank(recip)) {
            Recipients R = new Recipients(recip);
            Print.sysPrintln(R.toString());
        }

    }

}
