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
//  2009/11/10  Martin D. Flynn
//     -Initial Creation
//  2013/05/19  Martin D. Flynn
//     -Added support for displaying last/next service time calendar
// ----------------------------------------------------------------------------

var MAX_COMMAND_ARGS   = 10;
var COMMAND_ARG_PREFIX = "rct_";

// Onload
function devCommandOnLoad() {
    devCommandRadioChanged();
};

// ----------------------------------------------------------------------------

/**
*** Show Device Expire Calander
**/
function deviceToggleDevExpCalendar()
{
    var fade = false;
    if (devExpCal) {
        var cal = devExpCal;
        var fld = document.getElementById(ID_DEVICE_EXPIRE);
        if (cal.isExpanded()) {
            // collapse
            cal.setExpanded(false, fade);
            if (fld) { fld.value = cal.getTimeAsString(); }
        } else {
            // expand
            if (fld) { cal.setDateAsString(fld.value); }
            cal.setExpanded(true, fade);
            cal.setCallbackOnSelect(function() {
                cal.setExpanded(false, fade);
                if (fld) { fld.value = cal.getDateAsString(); }
            });
        }
    }
};

// ----------------------------------------------------------------------------

/**
*** Show ShareMap Start Calander
**/
function deviceToggleShareStartCalendar()
{
    var fade = false;
    if (devExpCal) {
        var cal = shareFrCal;
        var fld = document.getElementById(ID_SHARE_START);
        if (cal.isExpanded()) {
            // collapse
            cal.setExpanded(false, fade);
            if (fld) { fld.value = cal.getTimeAsString(); }
        } else {
            // expand
            if (fld) { cal.setDateAsString(fld.value); }
            cal.setExpanded(true, fade);
            cal.setCallbackOnSelect(function() {
                cal.setExpanded(false, fade);
                if (fld) { fld.value = cal.getDateAsString(); }
            });
        }
    }
};

/**
*** Show ShareMap End Calander
**/
function deviceToggleShareEndCalendar()
{
    var fade = false;
    if (devExpCal) {
        var cal = shareToCal;
        var fld = document.getElementById(ID_SHARE_END);
        if (cal.isExpanded()) {
            // collapse
            cal.setExpanded(false, fade);
            if (fld) { fld.value = cal.getTimeAsString(); }
        } else {
            // expand
            if (fld) { cal.setDateAsString(fld.value); }
            cal.setExpanded(true, fade);
            cal.setCallbackOnSelect(function() {
                cal.setExpanded(false, fade);
                if (fld) { fld.value = cal.getDateAsString(); }
            });
        }
    }
};

// ----------------------------------------------------------------------------

/**
*** Show License Expire Calander
**/
function deviceToggleLicExpCalendar()
{
    var fade = false;
    if (licExpCal) {
        var cal = licExpCal;
        var fld = document.getElementById(ID_LICENSE_EXPIRE);
        if (cal.isExpanded()) {
            // collapse
            cal.setExpanded(false, fade);
            if (fld) { fld.value = cal.getTimeAsString(); }
        } else {
            // expand
            if (fld) { cal.setDateAsString(fld.value); }
            cal.setExpanded(true, fade);
            cal.setCallbackOnSelect(function() {
                cal.setExpanded(false, fade);
                if (fld) { fld.value = cal.getDateAsString(); }
            });
        }
    }
};

// ----------------------------------------------------------------------------

/**
*** Show Last Service Calander
**/
function deviceToggleLastServiceTimeCalendar()
{
    var fade = false;
    if (lastSrvDateCal) {
        var cal = lastSrvDateCal;
        var fld = document.getElementById(ID_LAST_SERVICE_DATE);
        if (cal.isExpanded()) {
            // collapse
            cal.setExpanded(false, fade);
            if (fld) { fld.value = cal.getTimeAsString(); }
        } else {
            // expand
            if (fld) { cal.setDateAsString(fld.value); }
            cal.setExpanded(true, fade);
            cal.setCallbackOnSelect(function() {
                cal.setExpanded(false, fade);
                if (fld) { fld.value = cal.getDateAsString(); }
            });
        }
    }
};

/**
*** Show Next Service Calander
**/
function deviceToggleNextServiceTimeCalendar()
{
    var fade = false;
    if (nextSrvDateCal) {
        var cal = nextSrvDateCal;
        var fld = document.getElementById(ID_NEXT_SERVICE_DATE);
        if (cal.isExpanded()) {
            // collapse
            cal.setExpanded(false, fade);
            if (fld) { fld.value = cal.getTimeAsString(); }
        } else {
            // expand
            if (fld) { cal.setDateAsString(fld.value); }
            cal.setExpanded(true, fade);
            cal.setCallbackOnSelect(function() {
                cal.setExpanded(false, fade);
                if (fld) { fld.value = cal.getDateAsString(); }
            });
        }
    }
};

// ----------------------------------------------------------------------------

// Command radio button selection changed
function devCommandRadioChanged() {
   //try {
      if (document.DeviceCommandForm.cmdRadioSel.length) {
         var rc = document.DeviceCommandForm.cmdRadioSel.length;
         //alert("Radio selection changed ... " + rc);
         for (var i = 0; i < rc; i++) {
            var cmdName = document.DeviceCommandForm.cmdRadioSel[i].value;
            var cmdChkd = document.DeviceCommandForm.cmdRadioSel[i].checked;
            //alert("Command: " + i + " " + cmdName);
            for (var a = 0; a < MAX_COMMAND_ARGS; a++) {
                var cmdOptn = document.getElementById(COMMAND_ARG_PREFIX + cmdName + '_' + a);
                if (!cmdOptn) { continue; }
                //alert("Radio selection changed ... " + i + " " + cmdChkd);
                if (cmdChkd) {
                   cmdOptn.disabled  = false;
                   cmdOptn.className = "textInput";
                } else {
                   cmdOptn.disabled  = true;
                   cmdOptn.className = "textReadOnly";
                }
            }
         }
      }
   //} catch (e) {
      //
   //}
};
