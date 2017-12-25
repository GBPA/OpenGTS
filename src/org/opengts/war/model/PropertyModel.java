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
import java.util.Collection;

import org.opengts.util.*;

public class PropertyModel
    extends OrderedMap<String,Object>
{

    public static final String PROP_values = "values";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Object      dftValue  = null;

    /**
    *** Constructor
    **/
    public PropertyModel(boolean blankDefault)
    {
        super();
        this.dftValue = blankDefault? "" : null;
    }

    /**
    *** Constructor
    **/
    public PropertyModel()
    {
        super();
    }

    // ------------------------------------------------------------------------

    /**
    *** Puts a property key/value to this PropertyModel
    **/
    public Object put(String key, Object val)
    {
        if (key != null) {
            return super.put(key, val);
        } else {
            return null;
        }
    }

    // --------------------------------

    /**
    *** Gets a property value from this PropertyModel
    **/
    public Object get(Object key)
    {
        return this.get(key, this.dftValue);
    }

    /**
    *** Gets a property value from this PropertyModel
    **/
    public Object get(Object key, Object dft)
    {
        if (!(key instanceof String)) {
            Object val = super.get(key);
            return (val != null)? val : dft;
        } else
        if (((String)key).equalsIgnoreCase(PROP_values)) {
            Collection<?> list = this.values();
            return list;
        } else {
            Object val = super.get(key);
            return (val != null)? val : dft;
        }
    }

    /**
    *** Gets a property value from this PropertyModel
    **/
    public Object getValue(String key, String arg, Object dft)
    {
        return this.get(key); // "dft" is ignored
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a String representation of this instance
    **/
    public String toString()
    {
        return StringTools.className(this);
    }

    // ------------------------------------------------------------------------

}
