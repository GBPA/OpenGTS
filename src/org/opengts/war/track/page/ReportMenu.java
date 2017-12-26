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
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/03/25  Martin D. Flynn
//     -Added CSV output format
//     -Added report category support
//  2007/03/30  Martin D. Flynn
//     -Added access control
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/12/13  Martin D. Flynn
//     -Changes made to allow subclassing
//  2009/11/01  Martin D. Flynn
//     -Added ReportOption support
//  2012/05/27  Martin D. Flynn / Ricardo Trigo
//     -Initial support for PDF file output.
//  2013/08/06  Martin D. Flynn
//     -Added "id=" attribute to "span" for report selection radio button.
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.Locale;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.TimeZone;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.track.*;

public class ReportMenu
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    private static final String  ID_DEVICE_ID                   = "deviceSelector";
    private static final String  ID_DEVICE_DESCR                = "deviceDescDisp";

    // ------------------------------------------------------------------------
    // Forms:

    public  static final String  FORM_GET_REPORT                = "GetReport";
    public  static final String  FORM_SELECT_REPORT             = "SelectReport";
    public  static final String  FORM_DEVICE_GROUP              = "DeviceGroup";

    // ------------------------------------------------------------------------
    // Report type classifications:

    public  static final String  REPORT_TYPE_ALL                = "all";

    // ------------------------------------------------------------------------
    // ACLs

    public  static final String  _ACL_NON_HTML_FORMATS          = "nonHtml";
    public  static final String  _ACL_LIST[]                    = {
        //_ACL_NON_HTML_FORMATS
    };

    // ------------------------------------------------------------------------

    public  static final String  CSS_REPORT_RADIO_BUTTON        = "reportRadioButton";
    public  static final String  CSS_REPORT_RADIO_OPTION        = "reportRadioOption";

    public  static final String  COMMAND_REPORT_SELECT          = "rptsel";     // arg=<reportName>

    public  static final String  PARM_GROUP_ID                  = PARM_GROUP;
    public  static final String  PARM_DEVICE_ID                 = PARM_DEVICE;
    public  static final String  PARM_DRIVER_ID                 = PARM_DRIVER;

    public  static final String  PARM_REPORT_SUBMIT             = "r_submit";

    public  static final String  PARM_REPORT[]                  = ReportURL.RPTARG_REPORT;
    public  static final String  PARM_REPORT_OPT_               = "r_opt_";
    public  static final String  PARM_REPORT_OPT                = "r_option";
    public  static final String  PARM_REPORT_TEXT_              = "r_txt_";
    public  static final String  PARM_REPORT_TEXT               = "r_text";
    public  static final String  PARM_LIMIT[]                   = ReportURL.RPTARG_LIMIT;
    public  static final String  PARM_LIMIT_TYPE[]              = ReportURL.RPTARG_LIMIT_TYPE; // not used
    public  static final String  PARM_FORMAT[]                  = ReportURL.RPTARG_FORMAT;

    public  static final String  PARM_EMAIL_ADDR[]              = ReportURL.RPTARG_EMAIL;

    public  static final String  PARM_MENU                      = "r_menu";

    // ------------------------------------------------------------------------

    /* Calendar IDs */
    public  static final String  CALENDAR_FROM                  = "rptCal_fr";
    public  static final String  CALENDAR_TO                    = "rptCal_to";

    // ------------------------------------------------------------------------

    /**
    *** Writes the JavaScript information
    **/
    protected static void writeJS_MenuUpdate(PrintWriter out, RequestProperties reqState, ReportMenu rptMenu,
        boolean showFromCal, String parm_RANGE_FR[],
        boolean showToCal  , String parm_RANGE_TO[],
        String parm_TIMEZONE[],
        boolean allowNonHtmlFormat)
        throws IOException
    {
        HttpServletRequest request   = reqState.getHttpServletRequest();
        PrivateLabel       privLabel = reqState.getPrivateLabel();
        I18N               i18n      = privLabel.getI18N(ReportMenu.class);
        boolean            isGroup   = rptMenu.isReportTypeDeviceGroup(); // isFleet
        boolean            isDevice  = rptMenu.isReportTypeDevice();
        boolean            isDriver  = rptMenu.isReportTypeDriver();
        boolean            isTable   = rptMenu.isReportTypeTable();
        reqState.setFleet(isGroup); // not used (or really shouldn't be)

        /* allowed formats */
        boolean outFormat_CSV    = allowNonHtmlFormat;
        boolean outFormat_XLS    = allowNonHtmlFormat;
        boolean outFormat_PDF    = allowNonHtmlFormat;
        boolean outFormat_XML    = allowNonHtmlFormat;
        boolean outFormat_EHTML  = allowNonHtmlFormat;
        boolean outFormat_CUSTOM = allowNonHtmlFormat;
        boolean outFormat_SCHED  = allowNonHtmlFormat;

        /* HTML URL */
        String htmlURL   = Track.GetBaseURL(reqState); // EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI());

        /* custom URL */
        String customURL = privLabel.getStringProperty(PrivateLabel.PROP_ReportMenu_customFormatURL,"");
        if (!StringTools.isBlank(customURL)) {
            Print.logInfo("Including ReportCustomURL: " + customURL);
        }

        /* start JavaScript */
        JavaScriptTools.writeStartJavaScript(out);

        /* Group/Device list */
        if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
            //DeviceChooser.writeDeviceList(out, reqState, "ReportSelectorList");
        }

        /* Calendar */
        // -- from
        if (showFromCal) {
            Calendar.writeNewCalendar(out, CALENDAR_FROM, null, i18n.getString("ReportMenu.dateFrom","From"), reqState.getEventDateFrom());
        }
        // -- to
        if (showToCal) {
            Calendar.writeNewCalendar(out, CALENDAR_TO  , null, i18n.getString("ReportMenu.dateTo"  ,"To"  ), reqState.getEventDateTo());
        }

        /* vars */
        out.write("// Report vars \n");
        JavaScriptTools.writeJSVar(out, "ReportIsGroup"   , isGroup);
        JavaScriptTools.writeJSVar(out, "ReportPageName"  , rptMenu.getPageName());
        JavaScriptTools.writeJSVar(out, "ReportHtmlURL"   , htmlURL);
        JavaScriptTools.writeJSVar(out, "ReportCustomURL" , customURL);

        out.write("// Onload \n");
        out.write("function rptmOnLoad() {\n");
        String _calFrom = "null";
        if (showFromCal) {
            _calFrom = CALENDAR_FROM;
            out.write("    "+_calFrom+".setCollapsible(false, false, false);\n");
            out.write("    "+_calFrom+".setYearAdvanceSelection(false);\n");
        }
        String _calTo = "null";
        if (showToCal) {
            _calTo = CALENDAR_TO;
            out.write("    "+_calTo  +".setCollapsible(false, false, false);\n");
            out.write("    "+_calTo  +".setYearAdvanceSelection(false);\n");
        }
        out.write("    calWriteCalendars("+_calFrom+","+_calTo+");\n");
        out.write("    rptmReportRadioChanged();\n");
        out.write("}\n");

        if (isGroup) {
            out.write("// device group ID \n");
            out.write("function rptmGetDeviceGroup() {\n");
            out.write("   return document."+FORM_DEVICE_GROUP+"."+PARM_GROUP_ID+".value;\n");
            out.write("}\n");
        } else
        if (isDevice) {
            out.write("// device ID \n");
            out.write("function rptmGetDevice() {\n");
            out.write("   return document."+FORM_DEVICE_GROUP+"."+PARM_DEVICE_ID+".value;\n");
            out.write("}\n");
        } else
        if (isDriver) {
            out.write("// driver ID \n");
            out.write("function rptmGetDriver() {\n");
            out.write("   return document."+FORM_DEVICE_GROUP+"."+PARM_DRIVER_ID+".value;\n");
            out.write("}\n");
        }

        out.write("// selected report \n");
        out.write("function rptmGetReport() {\n");
        out.write("   if (document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".length) {\n");
        out.write("     var rc = document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".length;\n");
        out.write("     for (var i = 0; i < rc; i++) {\n");
        out.write("       if (document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+"[i].checked) {\n");
        out.write("           return document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+"[i].value;\n");
        out.write("       }\n");
        out.write("     }\n");
        out.write("     return '?';\n");
        out.write("   } else {\n"); // assume that there is at least 1 report in the list
        out.write("     return document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".value;\n");
        out.write("   }\n");
        out.write("}\n");

        out.write("// selected report option\n");
        out.write("function rptmGetReportOption() {\n");
        out.write("   var rptName = rptmGetReport();\n");
        out.write("   var rptOptID = '" + PARM_REPORT_OPT_ + "' + rptName;\n");
        out.write("   var rptOpt = document.getElementById(rptOptID);\n");
        out.write("   if (rptOpt) {\n");
        out.write("       return rptOpt.value;\n");
        out.write("   }\n");
        out.write("   return '';\n");
        out.write("}\n");

        out.write("// selected report text\n");
        out.write("function rptmGetReportText() {\n");
        out.write("   var rptName = rptmGetReport();\n");
        out.write("   var rptTxtID = '" + PARM_REPORT_TEXT_ + "' + rptName;\n");
        out.write("   var rptTxt = document.getElementById(rptTxtID);\n");
        out.write("   if (rptTxt) {\n");
        out.write("       return rptTxt.value;\n");
        out.write("   }\n");
        out.write("   return '';\n");
        out.write("}\n");

        out.write("// record limit/type \n");
        out.write("function rptmGetLimit() {\n");
        out.write("   return '';\n");
        out.write("}\n");
        out.write("function rptmGetLimitType() {\n");
        out.write("   return '';\n");
        out.write("}\n");

        out.write("// report format \n");
        out.write("function rptmGetFormat() {\n");
        out.write("   return document."+FORM_GET_REPORT+"."+PARM_FORMAT[0]+".value;\n");
        out.write("}\n");

        out.write("// report format \n");
        out.write("function rptGetToEMailAddress() {\n");
        out.write("   try { return document."+FORM_GET_REPORT+"."+PARM_EMAIL_ADDR[0]+".value; } catch(e) { return ''; }\n");
        out.write("}\n");

        out.write("// submit command \n");
        out.write("function rptmSubmitCmd(page, cmd, arg) {\n");
        out.write("   var outFmt    = rptmGetFormat();\n");
        out.write("   var rptName   = rptmGetReport();\n");
        out.write("   var rptOption = rptmGetReportOption();\n");
        out.write("   var rptText   = rptmGetReportText();\n");
      //out.write("   alert('Report Option: ' + rptOption);\n");
        out.write("   document."+FORM_COMMAND+".method = 'post';\n");
        if (!allowNonHtmlFormat) {
        out.write("   document."+FORM_COMMAND+".action = ReportHtmlURL;\n");    // POST
        } else {
        if (outFormat_CSV) {
        out.write("   if (outFmt == '"+ReportURL.FORMAT_CSV+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.csv';\n"); // POST
        out.write("   } else\n");
        out.write("   if (outFmt == '"+ReportURL.FORMAT_TXT+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.txt';\n"); // POST  [ie. "Track_EventDetail.txt"]
        out.write("   } else\n");
        }
        if (outFormat_XLS) {
        out.write("   if (outFmt == '"+ReportURL.FORMAT_XLS+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.xls';\n"); // POST  [ie. "Track_EventDetail.xls"]
        out.write("   } else\n");
        out.write("   if (outFmt == '"+ReportURL.FORMAT_XLSX+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.xlsx';\n"); // POST [ie. "Track_EventDetail.xlsx"]
        out.write("   } else\n");
        }
        if (outFormat_PDF) {
        out.write("   if (outFmt == '"+ReportURL.FORMAT_PDF+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.pdf';\n"); // POST  [ie. "Track_EventDetail.pdf"]
        out.write("   } else\n");
        }
        if (outFormat_XML) {
        out.write("   if (outFmt == '"+ReportURL.FORMAT_XML+"') {\n");
        out.write("       var rptEnc = rptName.replace(/\\./g,'_');\n"); // <-- change '\\' to '\' when moving to a .js file
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL + '_' + rptEnc + '.xml';\n"); // POST  [ie. "Track_EventDetail.xml"]
        out.write("   } else\n");
        }
        if (outFormat_CUSTOM) {
        out.write("   if (outFmt == '"+ReportURL.FORMAT_CUSTOM+"') {\n");
        out.write("       document."+FORM_COMMAND+".action = ReportCustomURL;\n");  // GET
        out.write("       document."+FORM_COMMAND+".method = 'get';\n");            // GET
        out.write("       document."+FORM_COMMAND+".target = '_blank';\n");         // new page
        out.write("   } else\n");
        }
        out.write("   {\n");
        out.write("       document."+FORM_COMMAND+".action = ReportHtmlURL;\n");    // POST
        out.write("   }\n");
        }
        out.write("   document."+FORM_COMMAND+"."+PARM_PAGE         +".value = page;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_COMMAND      +".value = cmd;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_ARGUMENT     +".value = arg;\n");
        if (isGroup) {
        out.write("   document."+FORM_COMMAND+"."+PARM_GROUP_ID     +".value = rptmGetDeviceGroup();\n");
        } else
        if (isDevice) {
        out.write("   document."+FORM_COMMAND+"."+PARM_DEVICE_ID    +".value = rptmGetDevice();\n");
        } else
        if (isDriver) {
        out.write("   document."+FORM_COMMAND+"."+PARM_DRIVER_ID    +".value = rptmGetDriver();\n");
        }
        if (showFromCal) {
        out.write("   document."+FORM_COMMAND+"."+parm_RANGE_FR[0]  +".value = "+CALENDAR_FROM+".getArgDateTime();\n");
        }
        if (showToCal) {
        out.write("   document."+FORM_COMMAND+"."+parm_RANGE_TO[0]  +".value = "+CALENDAR_TO+".getArgDateTime();\n");
        }
        out.write("   document."+FORM_COMMAND+"."+parm_TIMEZONE[0]  +".value = calGetTimeZone();\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_LIMIT[0]     +".value = rptmGetLimit();\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_LIMIT_TYPE[0]+".value = rptmGetLimitType();\n"); // not used
        out.write("   document."+FORM_COMMAND+"."+PARM_FORMAT[0]    +".value = outFmt;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_EMAIL_ADDR[0]+".value = rptGetToEMailAddress();\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_REPORT[0]    +".value = rptName;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_REPORT_OPT   +".value = rptOption;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_REPORT_TEXT  +".value = rptText;\n");
        out.write("   document."+FORM_COMMAND+"."+PARM_MENU         +".value = ReportPageName;\n");
        out.write("   document."+FORM_COMMAND+".submit();\n");
        out.write("}\n");

        out.write("// 'Get Report' \n");
        out.write("function rptmSubmitReport() {\n");
        out.write("   rptmSubmitCmd('"+PAGE_REPORT_SHOW+"','"+COMMAND_REPORT_SELECT+"','');\n");
        out.write("}\n");

        String csvMsg   = i18n.getString("ReportMenu.csvFormat"  ,"Report will be returned as a comma-separated-value file");
        String xlsMsg   = i18n.getString("ReportMenu.xlsFormat"  ,"Report will be returned as an XLS spreadsheet file");
        String xlsxMsg  = i18n.getString("ReportMenu.xlsxFormat" ,"Report will be returned as an XLSX spreadsheet file");
        String xmlMsg   = i18n.getString("ReportMenu.xmlFormat"  ,"Report will be returned as an XML formatted file");
        String emailMsg = i18n.getString("ReportMenu.emailFormat","Report will be emailed to the following list of comma-separated email address(es):");
        String schedMsg = i18n.getString("ReportMenu.schedFormat","Report will be scheduled for periodic email reporting:"); // EXPERIMENTAL
        out.write("// Format selection changed \n");
        out.write("function rptmFormatChanged() {\n");
        out.write("   var toEmailElem = document.getElementById('"+PARM_EMAIL_ADDR[0]+"');\n");
        out.write("   if (toEmailElem) { toEmailElem.style.visibility = 'hidden'; }\n");
        out.write("   var fmtSelElem = document.getElementById('"+PARM_FORMAT[0]+"');\n");
        out.write("   var selVal = fmtSelElem? fmtSelElem.value : '';\n");
        out.write("   var formatMsgElem = document.getElementById('formatMsgElem');\n");
        out.write("   if (formatMsgElem) {\n");
        if (outFormat_CSV) {
        out.write("       if (selVal == '"+ReportURL.FORMAT_CSV+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+csvMsg+"\";\n");
        out.write("       } else\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_TXT+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+csvMsg+"\";\n");
        out.write("       } else\n");
        }
        if (outFormat_XLS) {
        out.write("       if (selVal == '"+ReportURL.FORMAT_XLS+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+xlsMsg+"\";\n");
        out.write("       } else\n");
        out.write("       if (selVal == '"+ReportURL.FORMAT_XLSX+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+xlsxMsg+"\";\n");
        out.write("       } else\n");
        }
        if (outFormat_XML) {
        out.write("       if (selVal == '"+ReportURL.FORMAT_XML+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+xmlMsg+"\";\n");
        out.write("       } else\n");
        }
        if (outFormat_EHTML) {
        out.write("       if (selVal == '"+ReportURL.FORMAT_EHTML+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+emailMsg+"\";\n");
        out.write("           if (toEmailElem) { toEmailElem.style.visibility = 'visible'; }\n");
        out.write("       } else\n");
        }
        if (outFormat_SCHED) { // EXPERIMENTAL
        out.write("       if (selVal == '"+ReportURL.FORMAT_SCHEDULE+"') {\n");
        out.write("           formatMsgElem.innerHTML = \""+schedMsg+"\";\n");
        out.write("       } else ");
        }
        out.write("       {\n");
        out.write("           formatMsgElem.innerHTML = '';\n");
        out.write("       }\n");
        out.write("   }\n");
        out.write("}\n");

        out.write("// Report radio button selection changed \n");
        out.write("function rptmReportRadioChanged() {\n");
        out.write("   try {\n");
        out.write("      if (document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".length) {\n");
        out.write("         var rc = document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+".length;\n");
        out.write("         for (var i = 0; i < rc; i++) {\n");
        out.write("            var rptName = document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+"[i].value;\n");
        out.write("            var rptChkd = document."+FORM_SELECT_REPORT+"."+PARM_REPORT[0]+"[i].checked;\n");
        out.write("            var rptOptn = document.getElementById('" + PARM_REPORT_OPT_ + "' + rptName);\n");
        out.write("            if (rptOptn) {\n");
        out.write("               rptOptn.disabled = rptChkd? false : true;\n");
        out.write("            }\n");
        out.write("            var rptText = document.getElementById('" + PARM_REPORT_TEXT_ + "' + rptName);\n");
        out.write("            if (rptText) {\n");
        out.write("               rptText.disabled = rptChkd? false : true;\n");
        out.write("            }\n");
        out.write("         }\n");
        out.write("      }\n");
        out.write("   } catch (e) {\n");
        out.write("      //\n");
        out.write("   }\n");
        out.write("}\n");

        if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
            out.write("// Device/Group selector \n");
            out.write("function rptmShowSelector() {\n");
            out.write("   if (deviceShowChooserList) {\n");
            out.write("       var list = (typeof ReportSelectorList != 'undefined')? ReportSelectorList : null;\n");
            out.write("       deviceShowChooserList('"+ID_DEVICE_ID+"','"+ID_DEVICE_DESCR+"',list);\n");
            out.write("   }\n");
            out.write("}\n");
            out.write("function deviceDeviceChanged() {\n");
                // NO-OP
            out.write("}\n");
        }

        /* end JavaScript */
        JavaScriptTools.writeEndJavaScript(out);

        /* sorttable.js */
        if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
            JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(ReportPresentation.SORTTABLE_JS), request);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String reportType = REPORT_TYPE_ALL;

    /**
    *** Constructor
    **/
    public ReportMenu()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_MENU_REPORT);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the report-type of this instance
    *** @param rptType  The report-type
    **/
    public void setReportType(String rptType)
    {
        // -- see "ReportFactory.REPORT_TYPE_*"
        this.reportType = rptType;
    }

    /**
    *** Gets the report-type of this instance
    *** @return  The report-type
    **/
    public String getReportType()
    {
        // -- see "ReportFactory.REPORT_TYPE_*"
        return this.reportType;
    }

    /**
    *** Returns true if this report-type is "ALL"
    *** @return  True if this report-type is "ALL"
    **/
    public boolean isReportTypeAll()
    {
        String rt = this.getReportType();
        if ((rt == null) || rt.equals("")) {
            // -- this is the default, if not specified
            return true;
        } else
        if (rt.equalsIgnoreCase(REPORT_TYPE_ALL)) {
            // -- explicitly 'ALL'
            return true;
        } else {
            // -- otherwise not 'all'
            return false;
        }
    }

    // --------------------------------

    /**
    *** Returns true if this report type should display device groups
    *** @return True if this report type should display device groups
    **/
    public boolean isReportTypeDeviceGroup()
    {
        return ReportFactory.getReportTypeIsGroup(this.getReportType());
    }

    /**
    *** Returns true if this report type should display devices
    *** @return True if this report type should display devices
    **/
    public boolean isReportTypeDevice()
    {
        return ReportFactory.getReportTypeIsDevice(this.getReportType());
    }

    /**
    *** Returns true if this report type should display drivers
    *** @return True if this report type should display drivers
    **/
    public boolean isReportTypeDriver()
    {
        return ReportFactory.getReportTypeIsDriver(this.getReportType());
    }

    /**
    *** Returns true if this report type should display tables
    *** @return True if this report type should display tables
    **/
    public boolean isReportTypeTable()
    {
        return ReportFactory.getReportTypeIsTable(this.getReportType());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the menu name
    *** @param reqState  The login session RequestProperties instance
    *** @return The menu name
    **/
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_REPORTS;
    }

    /**
    *** Gets the menu description
    *** @param reqState  The login session RequestProperties instance
    *** @param parentMenuName  The name of the parent menu
    *** @return The menu descirption
    **/
    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportMenu.class);
        return super._getMenuDescription(reqState,i18n.getString("ReportMenu.menuDesc","GPS tracking reports"));
    }

    /**
    *** Gets the menu help text
    *** @param reqState  The login session RequestProperties instance
    *** @param parentMenuName  The name of the parent menu
    *** @return The menu help text
    **/
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportMenu.class);
        return super._getMenuHelp(reqState,i18n.getString("ReportMenu.menuHelp","Display various historical GPS detail and summary reports"));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the navigation description
    *** @param reqState  The login session RequestProperties instance
    *** @return The navigation description
    **/
    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportMenu.class);
        return super._getNavigationDescription(reqState,i18n.getString("ReportMenu.navDesc","Reports"));
    }

    /**
    *** Gets the navigation tab title
    *** @param reqState  The login session RequestProperties instance
    *** @return The navigation tab title
    **/
    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportMenu.class);
        return super._getNavigationTab(reqState,i18n.getString("ReportMenu.navTab","Reports"));
    }

    // ------------------------------------------------------------------------

    public String[] getChildAclList()
    {
        return _ACL_LIST;
    }

    public String getFormatAclName(String fmt)
    {
        String grpAcl = this.getAclName();
        int         p = grpAcl.lastIndexOf(".");
        String parAcl = (p > 0)? grpAcl.substring(0,p) : grpAcl;
        return !StringTools.isBlank(fmt)? AclEntry.CreateAclName(parAcl,fmt) : parAcl;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the Timezone is applicable and may be show
    *** @return True if the Timezone can be shown, false if it should not be shown
    **/
    public boolean getShowTimezone()
    {
        // -- override to change
        return true;
    }

    /**
    *** Returns true if the "From" Calendar is applicable and may be show
    *** @return True if the "From" Calendar can be shown, false if it should not be shown
    **/
    public boolean getShowFromCalendar()
    {
        // -- override to change
        return true;
    }

    /**
    *** Returns true if the "To" Calendar is applicable and may be show
    *** @return True if the "To" Calendar can be shown, false if it should not be shown
    **/
    public boolean getShowToCalendar()
    {
        // -- override to change
        return true;
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes the page html
    *** @param reqState  The login session RequestProperties instance
    *** @param pageMsg   The page message
    **/
    public void writePage(
        final RequestProperties reqState,
        final String pageMsg)
        throws IOException
    {
        HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n        = privLabel.getI18N(ReportMenu.class);
        final Locale locale    = reqState.getLocale();
        Account currAcct       = reqState.getCurrentAccount();
        User    currUser       = reqState.getCurrentUser();    // may be null
        String  cmdName        = reqState.getCommandName();    // not used?
        final boolean isGroup  = this.isReportTypeDeviceGroup();
        final boolean isDevice = this.isReportTypeDevice();
        final boolean isDriver = this.isReportTypeDriver();
        final boolean isTable  = this.isReportTypeTable();
        reqState.setFleet(isGroup);

        /* error */
        String m = pageMsg;
        boolean error = !StringTools.isBlank(m);

        /* date parameters */
        final boolean showFromCal  = this.getShowFromCalendar();
        final boolean showToCal    = this.getShowToCalendar();
        final boolean useMapDates  = privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_useMapDates,true);
        final String  parm_RANGE_FR[] = useMapDates? Calendar.PARM_RANGE_FR : Calendar.PARM_RANGE_FR2;
        final String  parm_RANGE_TO[] = useMapDates? Calendar.PARM_RANGE_TO : Calendar.PARM_RANGE_TO2;
        final String  parm_TIMEZONE[] = Calendar.PARM_TIMEZONE;

        /* date args */
        String rangeFr   = (String)AttributeTools.getRequestAttribute(request, parm_RANGE_FR  , "");
        String rangeTo   = (String)AttributeTools.getRequestAttribute(request, parm_RANGE_TO  , "");
        String tzStr     = (String)AttributeTools.getRequestAttribute(request, parm_TIMEZONE  , "");

        /* other args */
        String limitStr  = (String)AttributeTools.getRequestAttribute(request, PARM_LIMIT     , "");
        String limitType = (String)AttributeTools.getRequestAttribute(request, PARM_LIMIT_TYPE, ""); // not used
        String format    = (String)AttributeTools.getRequestAttribute(request, PARM_FORMAT    , ReportURL.FORMAT_HTML);

        /* 'demo' date range? */
        if (reqState.isDemoAccount()) {
            // -- Special case for the device with demo data.
            String firstRptKey = "FirstDemoReport_" + reqState.getSelectedDeviceID();
            String firstRpt    = (String)AttributeTools.getSessionAttribute(request, firstRptKey, null); // from session only
            if (firstRpt == null) {
                String dateRange[] = reqState.getDemoDateRange();
                if ((dateRange != null) && (dateRange.length >= 2)) {
                    rangeFr = dateRange[0];
                    rangeTo = dateRange[1];
                }
                AttributeTools.setSessionAttribute(request, firstRptKey, "true");
            }
        }

        /* TimeZone */
        if (StringTools.isBlank(tzStr)) {
            if (currUser != null) {
                // -- try User timezone
                tzStr = currUser.getTimeZone(); // may be blank
                if (StringTools.isBlank(tzStr) || tzStr.equals(Account.GetDefaultTimeZone())) {
                    // -- override with Account timezone
                    tzStr = currAcct.getTimeZone();
                }
            } else {
                // -- get Account timezone
                tzStr = currAcct.getTimeZone();
            }
            if (StringTools.isBlank(tzStr)) {
                // -- make sure we have a timezone
                // -  (unecessary, since Account/User will return a timezone)
                tzStr = Account.GetDefaultTimeZone();
            }
        }
        final TimeZone tz = DateTime.getTimeZone(tzStr); // will be GMT if invalid
        AttributeTools.setSessionAttribute(request, parm_TIMEZONE[0], tzStr);
        reqState.setTimeZone(tz, tzStr);

        /* Event date range */
        DateTime dateFr = Calendar.parseDate(rangeFr,tz,false);
        DateTime dateTo = Calendar.parseDate(rangeTo,tz,true );
        if (dateFr == null) { dateFr = Calendar.getCurrentDayStart(tz); }
        if (dateTo == null) { dateTo = Calendar.getCurrentDayEnd(tz); }
        if (dateFr.isAfter(dateTo)) { dateFr = dateTo; }
        reqState.setEventDateFrom(dateFr);
        reqState.setEventDateTo(  dateTo);
        AttributeTools.setSessionAttribute(request, parm_RANGE_FR[0], Calendar.formatArgDateTime(dateFr));
        AttributeTools.setSessionAttribute(request, parm_RANGE_TO[0], Calendar.formatArgDateTime(dateTo));

        /* reset previous 'reportID' */
        // -- TODO: should reset this iff this invocation is a different instance of ReportMenu
        String reportID = ""; // for now, always reset the previous report-id
        AttributeTools.setSessionAttribute(request, PARM_REPORT[0]  , reportID);
        AttributeTools.setSessionAttribute(request, PARM_REPORT_OPT , "");
        AttributeTools.setSessionAttribute(request, PARM_REPORT_TEXT, "");

        /* group/device */
        String deviceID = "";
        String groupID  = "";
        String driverID = "";
        if (isGroup) {
            String rptGrp = (String)AttributeTools.getRequestAttribute(request, PARM_GROUP_ID , "");
            groupID = StringTools.isBlank(rptGrp)? reqState.getSelectedDeviceGroupID() : rptGrp;
            AttributeTools.setSessionAttribute(request, PARM_GROUP_ID , groupID);
        } else
        if (isDevice) {
            String rptDev = (String)AttributeTools.getRequestAttribute(request, PARM_DEVICE_ID, "");
            deviceID = StringTools.isBlank(rptDev)? reqState.getSelectedDeviceID() : rptDev;
            AttributeTools.setSessionAttribute(request, PARM_DEVICE_ID, deviceID);
        } else
        if (isDriver) {
            String rptDrv = (String)AttributeTools.getRequestAttribute(request, PARM_DRIVER_ID, "");
            driverID = StringTools.isBlank(rptDrv)? reqState.getSelectedDriverID() : rptDrv;
            AttributeTools.setSessionAttribute(request, PARM_DRIVER_ID, driverID);
        }

        /* store vars as session attributes */
        AttributeTools.setSessionAttribute(request, PARM_LIMIT[0]     , limitStr);
        AttributeTools.setSessionAttribute(request, PARM_LIMIT_TYPE[0], limitType); // not used
        AttributeTools.setSessionAttribute(request, PARM_FORMAT[0]    , format);

        /* Style Sheets */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = ReportMenu.this.getCssDirectory();
                //WebPageAdaptor.writeCssLink(out, reqState, "ReportMenu.css", cssDir);
                Calendar.writeStyle(out, reqState);
                if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
                    DeviceChooser.writeStyle(out, reqState);
                }
            }
        };

        /* report output formats */
        final boolean allowNonHtmlFormat = privLabel.hasReadAccess(currUser, this.getFormatAclName(_ACL_NON_HTML_FORMATS)) &&
            privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_allowNonHtmlFormats,true);
        final boolean outFormat_HTML = true; // always enabled
        final boolean outFormat_CSV  = allowNonHtmlFormat; // &&
            //ReportURL.hasFormatReadAccess(currUser, privLabel, this.getAclName(), ReportURL.FORMAT_CSV);
        final boolean outFormat_XML  = allowNonHtmlFormat; // &&
            //ReportURL.hasFormatReadAccess(currUser, privLabel, this.getAclName(), ReportURL.FORMAT_XML);
        final boolean outFormat_XLS  = allowNonHtmlFormat && ReportSpreadsheet.IsExcelSpreadsheetSupported(); // &&
            //ReportURL.hasFormatReadAccess(currUser, privLabel, this.getAclName(), ReportURL.FORMAT_XML);
        final boolean outFormat_PDF  = false; // allowNonHtmlFormat &&
            //ReportURL.hasFormatReadAccess(currUser, privLabel, this.getAclName(), ReportURL.FORMAT_PDF);

        /* has notification email address */
        SmtpProperties smtpProps = currAcct.getSmtpProperties(privLabel);
        final String toEmailAddress = StringTools.trim(Account.getReportEmailAddress(currAcct,currUser));
        final boolean outFormat_EHTML; // see also "ReportURL.FORMAT_EHTML"
        if (!allowNonHtmlFormat) {
            // -- all non-html formats disabled
            outFormat_EHTML = false;
        } else
        if (!privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_enableReportEmail,true)) {
            // -- "EMail" option quietly disabled
            outFormat_EHTML = false;
        } else
        if (StringTools.isBlank(smtpProps.getFromEmailType("report"))) {
            // -- no "From" email address
            Print.logWarn("No valid 'From' notification email address defined ('EMail' option disabled)");
            outFormat_EHTML = false;
        } else
        if (!EMail.isSendMailEnabled()) {
            // -- SendMail not installed/enabled
            Print.logWarn("SendMail/JavaMail not installed.");
            outFormat_EHTML = false;
        } else {
            // -- otherwise enabled
            outFormat_EHTML = true; // &&
                //ReportURL.hasFormatReadAccess(currUser, privLabel, this.getAclName(), ReportURL.FORMAT_EHTML);
        }

        /* has report schedule (EXPERIMENTAL) */
        final boolean outFormat_SCHED; // see also "ReportURL.FORMAT_SCHEDULE"
        if (!allowNonHtmlFormat) {
            outFormat_SCHED = false;
        } else
        if (!privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_enableReportSchedule,false)) {
            outFormat_SCHED = false;
        } else {
            outFormat_SCHED = true; // EXPERIMENTAL (may not be fully supported)
                //ReportURL.hasFormatReadAccess(currUser, privLabel, this.getAclName(), ReportURL.FORMAT_SCHEDULE);
        }

        /* Misc attributes */
        final boolean showTimezoneSelect = this.getShowTimezone() && privLabel.getBooleanProperty(PrivateLabel.PROP_ReportMenu_showTimezoneSelection,true);
        final boolean outFormat_CUSTOM   = allowNonHtmlFormat && !StringTools.isBlank(privLabel.getStringProperty(PrivateLabel.PROP_ReportMenu_customFormatURL,""));

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String pageName = ReportMenu.this.getPageName();
                MenuBar.writeJavaScript(out, pageName, reqState);
                Calendar.writeJavaScript(out, reqState);
                if (DeviceChooser.isDeviceChooserUseTable(privLabel)) {
                    DeviceChooser.writeJavaScript(out, locale, reqState,
                        privLabel.getWebPageURL(reqState, pageName, Track.COMMAND_DEVICE_LIST));
                }
                ReportMenu.writeJS_MenuUpdate(out, reqState, ReportMenu.this,
                    showFromCal, parm_RANGE_FR,
                    showToCal  , parm_RANGE_TO,
                    parm_TIMEZONE,
                    allowNonHtmlFormat);
            }
        };

        /* write frame */
        final String _reportID = reportID;
        final String _groupID  = groupID;
        final String _deviceID = deviceID;
        final String _driverID = driverID;
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName  = ReportMenu.this.getPageName();
                String reportURL = Track.GetBaseURL(reqState); // EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI());

                /* Command Form */
                // -- This entire form is 'hidden'.  It's used by JS functions to submit specific commands
                String pageTarget = "_self"; // change to "_blank" to open reports in a separate page  // target='_top'
                out.write("\n");
                out.write("<form class='form-horizontal' id='"+FORM_COMMAND+"' name='"+FORM_COMMAND+"' method='post' action=\""+reportURL+"\" target='"+pageTarget+"'>\n");
                out.write(" <input type='hidden' name='"+PARM_PAGE              +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_COMMAND           +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_ARGUMENT          +"' value=''/>\n");
                if (isGroup)  {
                out.write(" <input type='hidden' name='"+PARM_GROUP_ID          +"' value=''/>\n");
                } else
                if (isDevice) {
                out.write(" <input type='hidden' name='"+PARM_DEVICE_ID         +"' value=''/>\n");
                } else
                if (isDriver) {
                out.write(" <input type='hidden' name='"+PARM_DRIVER_ID         +"' value=''/>\n");
                }
                out.write(" <input type='hidden' name='"+PARM_REPORT[0]         +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_REPORT_OPT        +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_REPORT_TEXT       +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+parm_RANGE_FR[0]       +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+parm_RANGE_TO[0]       +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+parm_TIMEZONE[0]       +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_LIMIT[0]          +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_LIMIT_TYPE[0]     +"' value=''/>\n"); // not used
                out.write(" <input type='hidden' name='"+PARM_FORMAT[0]         +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_EMAIL_ADDR[0]     +"' value=''/>\n");
                out.write(" <input type='hidden' name='"+PARM_MENU              +"' value=''/>\n");
                out.write("</form>\n");
                out.write("\n");

                // -- frame header
                out.write("<h1 class='"+CommonServlet.CSS_MENU_TITLE+"'>"+i18n.getString("ReportMenu.gpsReports","GPS Tracking Reports")+"</h1>\n");


                /* begin calendar/report-selection table */
                out.write("<table height='90%' class='table' border='0' cellspacing='0' cellpadding='0'>\n"); // {
                out.write("<tr>\n");
                out.write("<td valign='top' height='100%' style='padding-right:3px; border-right: 3px double black;'>\n");

                /* device[group] list */
                out.write("<form class='form-horizontal' id='"+FORM_DEVICE_GROUP+"' name='"+FORM_DEVICE_GROUP+"' method='post' action=\"javascript:rptmSubmitReport();\" target='_self'>\n"); // target='_top'
                IDDescription.SortBy sortBy = DeviceChooser.getSortBy(privLabel);
                if (isGroup) {
                    // -- fleet group selection
                    String grpAllDesc  = DeviceGroup.GetDeviceGroupAllTitle(reqState.getCurrentAccount(), locale);
                    String grpTitles[] = reqState.getDeviceGroupTitles();
                    out.write("<b>"+i18n.getString("ReportMenu.deviceGroup","{0}:",grpTitles)+"</b><br>\n");
                    if (DeviceChooser.isDeviceChooserUseTable(privLabel)) { // Fleet
                        out.write("<table cellspacing='0' class='table' cellpadding='0' border='0'><tr>");
                        out.write("<td>");
                        String chooserStyle   = "height:17px; padding:0px 0px 0px 3px; margin:0px 0px 0px 3px; cursor:pointer;";
                        String chooserOnclick = "javascript:rptmShowSelector()";
                        String chooserLen     = "16";
                        switch (sortBy) {
                            case DESCRIPTION : {
                                String grDesc = FilterValue(reqState.getDeviceGroupDescription(_groupID,false));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='hidden' value='"+_groupID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='form-control' type='text' value='"+grDesc+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            case NAME : {
                                String grName = FilterValue(reqState.getDeviceGroupDescription(_groupID,true));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='hidden' value='"+_groupID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='form-control' type='text' value='"+grName+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            default : {
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"'class='form-control' type='text' value='"+_groupID+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                        }
                        out.write("</td>");
                        out.write("<td><img src='https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Pulldown.png' height='17' style='cursor:pointer;' onclick='"+chooserOnclick+"'></td>");
                        out.write("</tr></table>\n");
                    } else {
                        OrderedSet<String> groupList = reqState.getDeviceGroupIDList(true); // non-null, length > 0
                        if (ListTools.isEmpty(groupList)) {
                            // will not occur
                            String id   = DeviceGroup.DEVICE_GROUP_NONE;
                            String desc = FilterValue("?");
                            out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='hidden' value='"+id+"'>");
                            out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                        } else
                        if (DeviceChooser.showSingleItemTextField(privLabel) && (groupList.size() == 1)) {
                            String id   = groupList.get(0);
                            if (sortBy.equals(IDDescription.SortBy.ID)) {
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+id+"'>");
                            } else {
                                boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                                String desc = FilterValue(reqState.getDeviceGroupDescription(id,rtnDispName));
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_GROUP_ID  +"' type='hidden' value='"+id+"'>");
                                out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                            }
                        } else {
                            java.util.List<IDDescription> sortList = new Vector<IDDescription>();
                            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                            for (String id : groupList) {
                                String desc = reqState.getDeviceGroupDescription(id,rtnDispName);
                                sortList.add(new IDDescription(id,desc));
                            }
                            IDDescription.SortList(sortList, rtnDispName? IDDescription.SortBy.DESCRIPTION : sortBy);
                            out.write("<select class='form-control' id='"+ID_DEVICE_ID+"' name='"+PARM_GROUP_ID+"'>\n");
                            for (IDDescription dd : sortList) {
                                String id   = dd.getID();
                                String desc = dd.getDescription();
                                String sel  = id.equals(_groupID)? "selected" : "";
                                String disp = FilterValue((sortBy.equals(IDDescription.SortBy.ID)?id:desc));
                                out.write("<option value='"+id+"' "+sel+">"+disp+"</option>\n");
                            }
                            out.write("</select>\n");
                        }
                    }
                } else
                if (isDevice) {
                    // -- device selection
                    String devTitles[] = reqState.getDeviceTitles();
                    out.write("<b>"+i18n.getString("ReportMenu.device","{0}:",devTitles)+"</b><br>\n");
                    if (DeviceChooser.isDeviceChooserUseTable(privLabel)) { // Device
                        out.write("<table cellspacing='0' cellpadding='0' border='0'><tr>");
                        out.write("<td>");
                        String chooserStyle   = "height:17px; padding:0px 0px 0px 3px; margin:0px 0px 0px 3px; cursor:pointer; ";
                        String chooserOnclick = "javascript:rptmShowSelector()";
                        String chooserLen     = "16";
                        switch (sortBy) {
                            case DESCRIPTION : {
                                String dvDesc = FilterValue(reqState.getDeviceDescription(_deviceID,false));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' type='hidden' value='"+_deviceID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='form-control' type='text' value='"+dvDesc   +"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            case NAME : {
                                String dvName = FilterValue(reqState.getDeviceDescription(_deviceID,true));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' type='hidden' value='"+_deviceID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='form-control' type='text' value='"+dvName   +"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            default : {
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"'class='form-control' type='text' value='"+_deviceID+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                        }
                        out.write("</td>");
                        out.write("<td><img src='https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Pulldown.png' height='17' style='cursor:pointer;' onclick='"+chooserOnclick+"'></td>");
                        out.write("</tr></table>\n");
                    } else {
                        OrderedSet<String> devList = reqState.getDeviceIDList(false/*inclInactv*/);
                        if (ListTools.isEmpty(devList)) {
                            String id   = DeviceGroup.DEVICE_GROUP_NONE;
                            String desc = FilterValue("?");
                            out.println("<input id='"+ID_DEVICE_ID+"' name='"+PARM_DEVICE_ID+"' type='hidden' value='"+id+"'>");
                            out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                        } else
                        if (DeviceChooser.showSingleItemTextField(privLabel) && (devList.size() == 1)) {
                            String id   = devList.get(0);
                            if (sortBy.equals(IDDescription.SortBy.ID)) {
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+id+"'>");
                            } else {
                                boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                                String desc = FilterValue(reqState.getDeviceDescription(id,rtnDispName));
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DEVICE_ID +"' type='hidden' value='"+id+"'>");
                                out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                            }
                        } else {
                            java.util.List<IDDescription> sortList = new Vector<IDDescription>();
                            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                            for (String id : devList) {
                                String desc = reqState.getDeviceDescription(id,rtnDispName);
                                sortList.add(new IDDescription(id,desc));
                            }
                            IDDescription.SortList(sortList, rtnDispName? IDDescription.SortBy.DESCRIPTION : sortBy);
                            out.write("<select class='form-control' id='"+ID_DEVICE_ID+"' name='"+PARM_DEVICE_ID+"'>\n");
                            for (IDDescription dd : sortList) {
                                String id   = dd.getID();
                                String desc = dd.getDescription();
                                String sel  = id.equals(_deviceID)? "selected" : "";
                                String disp = FilterValue((sortBy.equals(IDDescription.SortBy.ID)?id:desc));
                                out.write("<option value='"+id+"' "+sel+">"+disp+"</option>\n");
                            }
                            out.write("</select>\n");
                        }
                    }
                } else
                if (isDriver) {
                    // -- driver selection
                    out.write("<b>"+i18n.getString("ReportMenu.driver","Driver:")+"</b><br>\n");
                    if (DeviceChooser.isDeviceChooserUseTable(privLabel)) { // Driver
                        out.write("<table cellspacing='0' cellpadding='0' border='0'><tr>");
                        out.write("<td>");
                        String chooserStyle   = "height:17px; padding:0px 0px 0px 3px; margin:0px 0px 0px 3px; cursor:pointer; ";
                        String chooserOnclick = "javascript:rptmShowSelector()";
                        String chooserLen     = "16";
                        switch (sortBy) {
                            case DESCRIPTION : {
                                String dvDesc = FilterValue(reqState.getDriverDescription(_driverID,false));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DRIVER_ID +"' type='hidden' value='"+_driverID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' type='text' value='"+dvDesc   +"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            case NAME : {
                                String dvName = FilterValue(reqState.getDeviceDescription(_driverID,true));
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DRIVER_ID +"' type='hidden' value='"+_driverID+"'>");
                                out.write("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' type='text' value='"+dvName   +"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                            default : {
                                out.write("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DRIVER_ID +"' type='text' value='"+_driverID+"' readonly size='"+chooserLen+"' style='"+chooserStyle+"' onclick=\""+chooserOnclick+"\">");
                                } break;
                        }
                        out.write("</td>");
                        out.write("<td><img src='https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/images/Pulldown.png' height='17' style='cursor:pointer;' onclick='"+chooserOnclick+"'></td>");
                        out.write("</tr></table>\n");
                    } else {
                        OrderedSet<String> drvList = reqState.getDriverIDList();
                        if (ListTools.isEmpty(drvList)) {
                            String id   = "n/a";
                            String desc = FilterValue("?");
                            out.println("<input id='"+ID_DEVICE_ID+"' name='"+PARM_DRIVER_ID+"' type='hidden' value='"+id+"'>");
                            out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                        } else
                        if (DeviceChooser.showSingleItemTextField(privLabel) && (drvList.size() == 1)) {
                            String id   = drvList.get(0);
                            if (sortBy.equals(IDDescription.SortBy.ID)) {
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DRIVER_ID +"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+id+"'>");
                            } else {
                                boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                                String desc = FilterValue(reqState.getDeviceDescription(id,rtnDispName));
                                out.println("<input id='"+ID_DEVICE_ID   +"' name='"+PARM_DRIVER_ID +"' type='hidden' value='"+id+"'>");
                                out.println("<input id='"+ID_DEVICE_DESCR+"' name='"+ID_DEVICE_DESCR+"' class='"+CommonServlet.CSS_TEXT_READONLY+" form-control' type='text' readonly size='14' maxlength='32' value='"+desc+"'>");
                            }
                        } else {
                            java.util.List<IDDescription> sortList = new Vector<IDDescription>();
                            boolean rtnDispName = sortBy.equals(IDDescription.SortBy.NAME);
                            for (String id : drvList) {
                                String desc = reqState.getDriverDescription(id,rtnDispName);
                                sortList.add(new IDDescription(id,desc));
                            }
                            IDDescription.SortList(sortList, rtnDispName? IDDescription.SortBy.DESCRIPTION : sortBy);
                            out.write("<select class='form-control' id='"+ID_DEVICE_ID+"' name='"+PARM_DRIVER_ID+"'>\n");
                            for (IDDescription dd : sortList) {
                                String id   = dd.getID();
                                String desc = dd.getDescription();
                                String sel  = id.equals(_driverID)? "selected" : "";
                                String disp = FilterValue((sortBy.equals(IDDescription.SortBy.ID)?id:desc));
                                out.write("<option value='"+id+"' "+sel+">"+disp+"</option>\n");
                            }
                            out.write("</select>\n");
                        }
                    }
                }
                out.write("</form>\n");

                /* From/To Calendars */
                DateTime fr = reqState.getEventDateFrom();
                if (fr == null) { fr = new DateTime(tz); }
                DateTime to = reqState.getEventDateTo();
                if (to == null) { to = new DateTime(tz); }
                boolean sameMonth = (fr.getYear() == to.getYear()) && (fr.getMonth1() == to.getMonth1());
                out.write("\n");
                out.println("<!-- Calendars -->");
                out.write("<div style='height: 100%; margin-top: 8px;'>\n");
                if (showFromCal || showToCal) {
                out.write(  "<b>"+i18n.getString("ReportMenu.selectDate","Select Date Range:")+"</b>\n");
                }
                out.write(  "<div class='"+Calendar.CLASS_CAL_DIV+"' id='"+Calendar.ID_CAL_DIV+"' style='text-align: center; padding: 4px 5px 0px 5px;'>\n");
                if (showFromCal) {
                out.write(    "<div id='"+CALENDAR_FROM+"'></div>\n");
                }
                if (showToCal) {
                out.write(    "<div id='"+CALENDAR_TO+  "' style='padding-top: 8px;'></div>\n");
                }
                if (showTimezoneSelect) {
                    out.println("<!-- Timezone select -->");
                    out.println("<div style='padding-top: 5px; text-align: left;'>");
                    out.println(  "<form class='form-horizontal' id='TimeZoneSelect' name='TimeZoneSelect' method='get' action=\"javascript:true;\" target='_self'>"); // target='_top'
                    out.println(  "<span style=''><b>"+i18n.getString("ReportMenu.timeZone","TimeZone:")+"</b></span><br>");
                    out.println(  "<select class='form-control' name='"+parm_TIMEZONE[0]+"' onchange=\"javascript:calSelectTimeZone(document.TimeZoneSelect."+parm_TIMEZONE[0]+".value)\">");
                    String timeZone = reqState.getTimeZoneString(null);
                    java.util.List _tzList = reqState.getTimeZonesList();
                    for (Iterator i = _tzList.iterator(); i.hasNext();) {
                        String tmz = (String)i.next();
                        String sel = tmz.equals(timeZone)? "selected" : "";
                        out.println("  <option value='"+tmz+"' "+sel+">"+tmz+"</option>");
                    }
                    out.println(  "</select>");
                    out.println(  "</form>");
                    out.println("</div>");
                    out.println("");
                }

                // the following pushes the calendars to the top
                // (however, it also pushes the footer to the bottom of the frame, and leaves
                // a bunch of space below the footer)
                //out.write("<div style='height:100%'>&nbsp;</div>\n");

                out.write(  "</div>\n");
                out.write("</div>\n");
                out.write("\n");

                out.write("</td>\n");

                out.write("<td nowrap width='100%' height='100%' valign='top' style='margin-left:10px;'>\n");

                /* reports */
                out.write("<!-- Begin Reports -->\n");
                out.write("<table width='100%'>\n"); // {
                out.write("<tr>\n");
                out.write("<td valign='top' style='margin-top: 8px; padding-left: 5px;'>\n");
                out.write("<form class='form-horizontal' id='"+FORM_SELECT_REPORT+"' name='"+FORM_SELECT_REPORT+"' method='post' action=\"javascript:rptmSubmitReport();\" target='_self'>\n"); // target='_top'
                boolean checked = false;
                int rptSel = 0;
                for (int t = 0; t < ReportFactory.REPORT_TYPES.length; t++) {
                    // -- check report type
                    String type = ReportFactory.REPORT_TYPES[t];
                    if (!ReportMenu.this.isReportTypeAll() && !ReportMenu.this.getReportType().equalsIgnoreCase(type)) {
                        // -- report type does not match what this report menu supports
                        continue;
                    }
                    // -- include report type
                    String desc = ReportFactory.getReportTypeDescription(reqState, type);
                    java.util.List<ReportEntry> reportItems = ReportMenu.this.getReportItems(reqState, type);
                    if (reportItems.size() > 0) {
                        // -- at least one report with this type
                        out.write("  <b>"+desc+"</b><br/>\n");
                        for (Iterator<ReportEntry> i = reportItems.iterator(); i.hasNext();) {
                            ReportEntry   re = i.next();
                            ReportFactory rf = re.getReportFactory();
                            String        rn = rf.getReportName();
                            out.write("<span class='"+CSS_REPORT_RADIO_BUTTON+"' id='" + rn + "'>");
                            out.write("<input class='"+CSS_REPORT_RADIO_BUTTON+"' type='radio' name='"+PARM_REPORT[0]+"' id='" + rn + "_btn' value='" + FilterValue(rn) + "' onchange=\"javascript:rptmReportRadioChanged();\"");
                            if (!checked && (StringTools.isBlank(_reportID) || _reportID.equals(rn))) {
                                out.write(" checked");
                                checked = true;
                            }
                            out.write(">");
                            String rmd = StringTools.replaceKeys(rf.getMenuDescription(locale),reqState,null);
                            out.write("<label for='"+rn+"_btn'>" + rmd + "</label>");
                            out.write("</span>\n");
                            if (rf.hasReportOptions(reqState)) {
                                OrderedMap<String,String> optMap = rf.getReportOptionDescriptionMap(reqState);
                                String optId = PARM_REPORT_OPT_ + rn;
                                /*
                                if (optMap.size() == 1) {
                                    String k = optMap.getFirstKey();
                                    String d = optMap.get(k);
                                    out.write("<input id='"+optId+"' name='"+optId+"' type='hidden' value='"+k+"'>");
                                    out.write("<span class='"+CSS_REPORT_RADIO_OPTION+"'>["+d+"]</span>\n");
                                } else
                                */
                                {
                                    ComboMap comboOptMap = new ComboMap(optMap);
                                    out.write(Form_ComboBox(optId, optId, true, comboOptMap, (String)null/*selected*/, null/*onchange*/));
                                }
                            } else
                            if (rf.hasReportTextInput()) {
                                String textId = PARM_REPORT_TEXT_ + rn;
                                out.write(Form_TextField(textId, textId, true, "", 40, 60));
                            }
                            out.write("<br/>\n");
                            rptSel++;
                        }
                    }
                }
                out.write("</form>\n");
                out.write("</td>\n");
                out.write("</tr>\n");

                out.write(" <!-- Begin Report Submit -->\n");
                out.write(" <tr>\n");
                out.write("  <td valign='bottom' style='text-align: left;'>\n");

                out.write("    <form class='form-horizontal' id='"+FORM_GET_REPORT+"' name='"+FORM_GET_REPORT+"' method='post' action=\"javascript:rptmSubmitReport();\" target='_self'>\n"); // target='_top'
                out.write("    <span style='padding-left: 5px;'><b>"+i18n.getString("ReportMenu.format","Format:")+"</b></span>\n");
                out.write("    <select id='"+PARM_FORMAT[0]+"' class='form-control' name='"+PARM_FORMAT[0]+"' onchange=\"javascript:rptmFormatChanged();\">\n");
                if (outFormat_HTML) {
                    out.write("      <option value='"+ReportURL.FORMAT_HTML +"' selected>HTML</option>\n");
                }
                if (outFormat_CSV) {
                    out.write("      <option value='"+ReportURL.FORMAT_CSV  +"'>CSV</option>\n");
                  //out.write("      <option value='"+ReportURL.FORMAT_TXT  +"'>TXT</option>\n");
                }
                if (outFormat_XML) {
                    out.write("      <option value='"+ReportURL.FORMAT_XML  +"'>XML</option>\n");
                }
                if (outFormat_PDF) {
                    out.write("      <option value='"+ReportURL.FORMAT_PDF  +"'>PDF</option>\n");
                }
                if (outFormat_XLS) {
                    out.write("      <option value='"+ReportURL.FORMAT_XLS +"'>XLS</option>\n");
                  //out.write("      <option value='"+ReportURL.FORMAT_XLSX +"'>XLSX</option>\n");
                }
                if (outFormat_EHTML) {
                    out.write("      <option value='"+ReportURL.FORMAT_EHTML+"'>EMail</option>\n");
                }
                if (outFormat_SCHED) { // EXPERIMENTAL
                    out.write("      <option value='"+ReportURL.FORMAT_SCHEDULE+"'>Schedule</option>\n");
                }
                if (outFormat_CUSTOM) {
                    out.write("      <option value='"+ReportURL.FORMAT_CUSTOM+"'>Custom</option>\n");
                }
                out.write("    </select>\n");
                out.write("    <br>\n");
                out.write("    <span style='margin-left:30px;'><input type='submit'class='btn btn-success' name='"+PARM_REPORT_SUBMIT+"' value='"+i18n.getString("ReportMenu.getReport","Get Report")+"'></span>\n");
                out.write("    <br>\n");
                out.write("    <br>\n");
                out.write("    <span id='formatMsgElem' style='margin-top:10px; margin-left:5px;'></span>\n");
                out.write("    <br>\n");
                if (outFormat_EHTML) {
                    boolean emailEditable = !reqState.isDemoAccount();
                    //boolean emailEditable = true;
                    String emailRO = emailEditable? "" : "readonly";
                    String emailClass = emailEditable? CommonServlet.CSS_TEXT_INPUT : CommonServlet.CSS_TEXT_READONLY;
                    out.write("    <input class='"+emailClass+"' id='"+PARM_EMAIL_ADDR[0]+"' name='"+PARM_EMAIL_ADDR[0]+"' style='margin-top:5px; margin-left:10px;; visibility:hidden' type='text' "+emailRO+" value='"+toEmailAddress+"' size='76'>");
                } else {
                    out.write("    <input id='"+PARM_EMAIL_ADDR[0]+"' name='"+PARM_EMAIL_ADDR[0]+"' type='hidden' value=''/>\n");
                }
                out.write("    </form>\n");
                out.write("  </td>\n");
                out.write(" </tr>\n");
                out.write(" <!-- End Report Submit -->\n");

                out.write("</table>\n"); // }
                out.write("<!-- End Reports -->\n");

                /* end table */
                out.write("</td>\n");
                out.write("</tr>\n");
                out.write("</table>\n");  // }

                /* write DeviceChooser DIV */
                if (DeviceChooser.isDeviceChooserUseTable(privLabel) &&
                    (isGroup || isDevice || isDriver)) {
                    java.util.List<IDDescription> idList;
                    if (isGroup) {
                        idList = reqState.createGroupIDDescriptionList(true/*inclAll*/, sortBy);
                    } else
                    if (isDevice) {
                        idList = reqState.createDeviceIDDescriptionList(false/*inclInactv*/, sortBy);
                    } else
                    if (isDriver) {
                        idList = reqState.createDriverIDDescriptionList(sortBy);
                    } else {
                        idList = new Vector<IDDescription>(); // empty (will not occur)
                    }
                    IDDescription list[] = (idList != null)? idList.toArray(new IDDescription[idList.size()]) : null;
                    DeviceChooser.writeChooserDIV(out, reqState, list, null);
                }

            }
        };

        /* write frame */
        String rptOnLoad = "javascript:rptmOnLoad();";
        String onload = (error && !StringTools.isBlank(m))? (rptOnLoad + JS_alert(false,m)) : rptOnLoad;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a list of ReportEntry items
    *** @param reqState  The login session RequestProperties instance
    *** @param rptType   The report type for which the list of ReportEntry's are returned
    *** @return A list of ReportEntry items.
    **/
    protected java.util.List<ReportEntry> getReportItems(RequestProperties reqState, String rptType)
    {
        java.util.List<ReportEntry> list = new Vector<ReportEntry>();
        if (reqState != null) {
            PrivateLabel pl = reqState.getPrivateLabel();
            Map<String,ReportEntry> reportMap = pl.getReportMap();
            if (reportMap != null) {
                Account currAcct = reqState.getCurrentAccount();
                User currUser = reqState.getCurrentUser();
                for (Iterator<ReportEntry> i = reportMap.values().iterator(); i.hasNext();) {
                    ReportEntry   re = i.next();
                    ReportFactory rf = re.getReportFactory();
                    if (StringTools.isBlank(rptType) || rptType.equalsIgnoreCase(rf.getReportType())) {
                        if (rf.isSysAdminOnly() && ((currAcct == null) || !currAcct.isSystemAdmin())) {
                            // skip this report
                        } else
                        if (pl.hasReadAccess(currUser,re.getAclName())) {
                            list.add(re);
                        }
                    }
                }
            }
        } else {
            Print.logStackTrace("RequestProperties is null!");
        }
        return list;
    }

    // ------------------------------------------------------------------------

}
