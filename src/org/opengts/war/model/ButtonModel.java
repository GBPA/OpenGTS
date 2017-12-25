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

import org.opengts.util.*;

public class ButtonModel
    extends PropertyModel
{

    // ------------------------------------------------------------------------
    
    public static final String PROP_type    = "type";
    public static final String PROP_name    = "name";
    public static final String PROP_title   = "title";
    public static final String PROP_url     = "url";
    public static final String PROP_onClick = "onClick";

    // ------------------------------------------------------------------------

    public static final String TYPE_SUBMIT  = "SUBMIT";
    public static final String TYPE_BUTTON  = "BUTTON";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public ButtonModel(String type, String name, String title)
    {
        super(true);
        this.setType(type);
        this.setName(name);
        this.setTitle(title);
    }

    // ------------------------------------------------------------------------

    public Object get(Object key)
    {
        return super.get(key);
    }

    // ------------------------------------------------------------------------

    public ButtonModel setType(String type)
    {
        if (type == null) {
            this.put(PROP_type , TYPE_SUBMIT);
        } else
        if (type.equalsIgnoreCase(TYPE_BUTTON)) {
            this.put(PROP_type , TYPE_BUTTON);
        } else {
            this.put(PROP_type , TYPE_SUBMIT);
        }
        return this;
    }

    // ------------------------------------------------------------------------

    public ButtonModel setName(String name)
    {
        this.put(PROP_name, StringTools.trim(name));
        return this;
    }

    // ------------------------------------------------------------------------

    public ButtonModel setTitle(String title)
    {
        this.put(PROP_title, StringTools.trim(title));
        return this;
    }

    // ------------------------------------------------------------------------

    public ButtonModel setURL(String URL) 
    {
        this.put(PROP_url, StringTools.trim(URL));
        return this;
    }

    // ------------------------------------------------------------------------

    public ButtonModel setOnClick(String onClick) 
    {
        this.put(PROP_onClick, StringTools.trim(onClick));
        return this;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("ButtonModel:");
        sb.append(" Name="+this.get(PROP_name));
        sb.append(" Type="+this.get(PROP_type));
        sb.append(" Title="+this.get(PROP_title));
        return sb.toString();
    }

}
    
