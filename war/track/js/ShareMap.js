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
//  2016/10/31  Martin D. Flynn
//     -Cloned from FullMap.js
// ----------------------------------------------------------------------------

var fmLimitOverride = -1;

/* return the event 'limit' for non-fleet maps */
function shareEventLimit()
{
    return IS_FLEET? -1 : fmLimitOverride;
};

// ----------------------------------------------------------------------------

/* this is executed when the page is loaded */
function shareOnLoad()
{
    shareUpdateMap(shareEventLimit(), jsmRecenterZoomMode(RECENTER_ZOOM));
};

/* this is executed when the page is unloaded */
function shareOnUnload()
{
    mapProviderUnload();
};

// ----------------------------------------------------------------------------

/* this is executed when the "Update All" is clicked */
function shareClickedUpdateAll() // all
{
    shareUpdateMap(shareEventLimit(), jsmRecenterZoomMode(RECENTER_ZOOM));
};

/* this is executed when "Update Map" is clicked */
function shareUpdateMap(limit, recenterMode) 
{
    var url = MAP_UPDATE_URL + 
        "&sha=" + SHARE_ACCOUNT +
        "&shd=" + SHARE_DEVICE +
        "&shp=" + SHARE_PASSCODE +
        "&_u=" + Math.random();  // necessary to make the URL unique
    mapProviderUpdateMap(url, recenterMode, 0);
};

// ----------------------------------------------------------------------------
