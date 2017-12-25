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
//  2017/01/25  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.model;

import java.util.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

/**
*** A Freemarker property accessor class.
*** Implements "get(KEY)" to return a contained property value.
*** If initialized with default-blank "true", the "get(KEY)" method will return a blank String
*** if the key is undefined (never returns null).
*** If initialized with default-blank "false", the "get(KEY)" method will return null if the
*** key is undefined, or if the resulting value is blank/null.
**/

public abstract class FTLKeyValueHash // TemplateHashModel
{

    // ------------------------------------------------------------------------

    private boolean  dftBlank   = false;
    private GetValue callback   = null;

    /**
    *** Constructor
    **/
    public FTLKeyValueHash() 
    {
        this(false, null);
    }

    /**
    *** Constructor
    **/
    public FTLKeyValueHash(boolean dftBlank) 
    {
        this(dftBlank, null);
    }

    /**
    *** Constructor
    **/
    public FTLKeyValueHash(GetValue getVal) 
    {
        this(false, getVal);
    }

    /**
    *** Constructor
    **/
    public FTLKeyValueHash(boolean dftBlank, GetValue getVal) 
    {
        super();
        this.dftBlank = dftBlank;
        this.callback = getVal;
    }

    // ------------------------------------------------------------------------

    /**
    *** Abstract class, provided by subclasses
    **/
    protected Object _getValue(String key, String arg, Object dft) 
    {
        // -- may be overriden
        if (this.callback != null) {
            return this.callback.getValue(key,arg,dft);
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Private implementation to get a key/value
    **/
    private Object _get(Object key, String arg, Object dft) 
    {
        // -- key is not a String
        if (!(key instanceof String)) {
            return null;
        }
        // -- key is blank
        if (StringTools.isBlank((String)key)) {
            return null;
        }
        // -- try key as-is
        Object val = this._getValue((String)key, arg, null);
        if (val == null) {
            // -- replace key chars '_'==>'.' and try again
            String k = ((String)key).replace('_', '.');
            val = this._getValue(k, arg, null);
        }
        return (val != null)? val : dft;
    }

    // --------------------------------

    public Object get(Object key)
    {
        return this.get(key,null,null);
    }

    public Object get(Object key, String arg)
    {
        return this.get(key,arg,null);
    }

    public Object get(Object key, String arg, Object dft)
    {
        Object val = this._get(key, arg, dft);
        //Print.logInfo("Get '"+key+"' arg="+arg+" class="+StringTools.className(val) + " value="+val);
        if (this.dftBlank) {
            return (val != null)? val : ""; // -- always return a non-null value
        } else
        if (val instanceof String) {
            return !StringTools.isBlank(val)? val : null; // -- return null if blank
        } else {
            return val; // may be null
        }
    }

    // ------------------------------------------------------------------------

}
