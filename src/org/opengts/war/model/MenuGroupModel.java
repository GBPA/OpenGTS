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

public class MenuGroupModel
    extends PropertyModel
{

    // ------------------------------------------------------------------------
    
    public static final String PROP_name    = "name";
    public static final String PROP_title   = "title";
    public static final String PROP_items   = "items";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private java.util.List<MenuItemModel> items = null;

    public MenuGroupModel(String name, String title)
    {
        super(true);
        this.setName(name);
        this.setTitle(title);
        this.items = new Vector<MenuItemModel>();
        this.put(PROP_items, items);
    }

    // ------------------------------------------------------------------------

    public Object get(Object key)
    {
        return super.get(key);
    }

    // ------------------------------------------------------------------------

    public MenuGroupModel setName(String name)
    {
        this.put(PROP_name, StringTools.trim(name));
        return this;
    }

    // ------------------------------------------------------------------------

    public MenuGroupModel setTitle(String title)
    {
        this.put(PROP_title, StringTools.trim(title));
        return this;
    }

    // ------------------------------------------------------------------------

    public MenuGroupModel addMenuItem(MenuItemModel mim)
    {
        this.items.add(mim);
        return this;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("MenuGroupModel:");
        sb.append(" Title="+this.get(PROP_title));
        return sb.toString();
    }

}
    
