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

public class FieldModel
    extends PropertyModel
{

    private static volatile long FieldCounter = 0L;

    // ------------------------------------------------------------------------

    public static final String PROP_ID          = "id";
    public static final String PROP_styleClass  = "styleClass";
    public static final String PROP_title       = "title";
    public static final String PROP_tooltip     = "tooltip";
    public static final String PROP_type        = "type";
    public static final String PROP_size        = "size";
    public static final String PROP_length      = "length";
    public static final String PROP_editable    = "editable";
    public static final String PROP_valueMap    = "valueMap";
    public static final String PROP_value       = "value";
    public static final String PROP_onClick     = "onClick";

    // ------------------------------------------------------------------------

    public static final String TYPE_TEXT        = "TEXT";
    public static final String TYPE_SELECT      = "SELECT";
    public static final String TYPE_CHECKBOX    = "CHECKBOX";
    public static final String TYPE_DATE        = "DATE";
    public static final String TYPE_DATETIME    = "DATETIME";
    public static final String TYPE_PUSHPIN     = "PUSHPIN";
    public static final String TYPE_HIDDEN      = "HIDDEN";
    public static final String TYPE_SEP_1       = "SEP_1";
    public static final String TYPE_SEP_2       = "SEP_2";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private long fieldSeq = -1L;

    public FieldModel(
        String id, String clz, 
        String title, String tooltip,
        String type, boolean editable, int size, int maxLen,
        Map<?,String> valMap, Object value, 
        String onClick) 
    {
        super(false);
        this.put(PROP_ID        , StringTools.trim(id));
        this.put(PROP_styleClass, StringTools.trim(clz));
        this.put(PROP_title     , StringTools.trim(title));
        this.put(PROP_tooltip   , StringTools.trim(tooltip));
        this.put(PROP_type      , StringTools.trim(type));
        this.put(PROP_size      , new Integer(size));
        this.put(PROP_length    , new Integer(maxLen));
        this.put(PROP_editable  , new Boolean(editable));
        this.put(PROP_valueMap  , valMap);
        this.put(PROP_value     , value);
        this.put(PROP_onClick   , onClick);
        this.fieldSeq = FieldCounter++;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a property value from this PropertyModel
    **/
    public Object get(Object key)
    {
        return super.get(key);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the field ID 
    **/
    public String getID()
    {
        return StringTools.trim(this.get(PROP_ID));
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the value
    *** @return The value
    **/
    public Object getValue()
    {
        return this.get(PROP_value);
    }

    /**
    *** Gets the value as a String
    *** @return The value as a String
    **/
    public String getStringValue()
    {
        Object val = this.getValue();
        if (val instanceof String) {
            return (String)val;
        } else {
            return StringTools.trim(val);
        }
    }

    /**
    *** Gets the value as a Double
    *** @return The value as a Double
    **/
    public Double getDoubleValue()
    {
        Object val = this.getValue();
        if (val instanceof Double) {
            return (Double)val;
        } else {
            return new Double(StringTools.parseDouble(val,0.0));
        }
    }

    /**
    *** Gets the value as a Long
    *** @return The value as a Long
    **/
    public Long getLongValue()
    {
        Object val = this.getValue();
        if (val instanceof Long) {
            return (Long)val;
        } else {
            return new Long(StringTools.parseLong(val,0L));
        }
    }

    /**
    *** Gets the value as a Integer
    *** @return The value as a Integer
    **/
    public Integer getIntegerValue()
    {
        Object val = this.getValue();
        if (val instanceof Integer) {
            return (Integer)val;
        } else {
            return new Integer(StringTools.parseInt(val,0));
        }
    }

    /**
    *** Gets the value as a Boolean
    *** @return The value as a Boolean
    **/
    public Boolean getBooleanValue()
    {
        Object val = this.getValue();
        if (val instanceof Boolean) {
            return (Boolean)val;
        } else {
            return new Boolean(StringTools.parseBoolean(val,false));
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a String representation of this instance
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("FieldModel["+this.fieldSeq+"]:");
        sb.append(" ID="+this.getID());
        sb.append(" Value="+this.getStringValue());
        return sb.toString();
    }

}
