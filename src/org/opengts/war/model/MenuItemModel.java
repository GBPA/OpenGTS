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
//  2017/03/14  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.model;

import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

import org.opengts.util.*;

public class MenuItemModel
    extends PropertyModel
{

    // ------------------------------------------------------------------------
    
    public static final String PROP_title   = "title";
    public static final String PROP_help    = "help";
    public static final String PROP_url     = "url";
    public static final String PROP_target  = "target";
    public static final String PROP_window  = "window";
    public static final String PROP_width   = "width";
    public static final String PROP_height  = "height";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public MenuItemModel(
        String title, String help, String url, 
        String target, boolean window, int width, int height)
    {
        super(true);
        this.setTitle(title);
        this.setHelp(help);
        this.setURL(url);
        this.setTarget(target);
        this.setWindow(window, width, height);
    }

    // ------------------------------------------------------------------------

    public Object get(Object key)
    {
        return super.get(key);
    }

    // ------------------------------------------------------------------------

    public MenuItemModel setTitle(String title)
    {
        this.put(PROP_title, StringTools.trim(title));
        return this;
    }

    // ------------------------------------------------------------------------

    public MenuItemModel setHelp(String help)
    {
        this.put(PROP_help, StringTools.trim(help));
        return this;
    }

    // ------------------------------------------------------------------------

    public MenuItemModel setURL(String url)
    {
        this.put(PROP_url, StringTools.trim(url));
        return this;
    }

    // ------------------------------------------------------------------------

    public MenuItemModel setTarget(String target)
    {
        this.put(PROP_target, StringTools.trim(target));
        return this;
    }

    // ------------------------------------------------------------------------

    public MenuItemModel setWindow(boolean window, int width, int height) 
    {
        if (window && (width > 0) && (height > 0)) {
            this.put(PROP_window, new Boolean(window));
            this.put(PROP_width , new Integer(width));
            this.put(PROP_height, new Integer(height));
        } else {
            this.remove(PROP_window);
            this.remove(PROP_width);
            this.remove(PROP_height);
        }
        return this;
    }


    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("MenuItemModel:");
        sb.append(" Title="+this.get(PROP_title));
        sb.append(" Help="+this.get(PROP_help));
        sb.append(" URL="+this.get(PROP_url));
        sb.append(" Target="+this.get(PROP_target));
        sb.append(" Window="+this.get(PROP_window));
        sb.append(" Width="+this.get(PROP_width));
        sb.append(" Height="+this.get(PROP_height));
        return sb.toString();
    }

}
    
