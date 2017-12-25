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
import java.util.Vector;

import org.opengts.util.*;

import org.opengts.war.tools.*;

public class FieldListModel
    extends FormModel
{

    public static final String PROP_list    = "list";

    // ------------------------------------------------------------------------

    private static volatile long FieldListCounter = 0L;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Vector<FieldModel> fieldList = new Vector<FieldModel>();

    private long fieldListSeq = -1L;

    /**
    *** Constructor
    **/
    public FieldListModel()
    {
        super();
        this.fieldListSeq = FieldListCounter++;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the field list
    **/
    public Vector<FieldModel> getFieldList()
    {
        if (this.fieldList == null) {
            this.fieldList = new Vector<FieldModel>();
            super.put(PROP_list, this.fieldList);
        }
        return this.fieldList;
    }
    
    /**
    *** Adds a FieldModel to the field map
    **/
    private void _addField(FieldModel fvm)
    {
        if (fvm != null) {
            super.put(fvm.getID(), fvm);
            this.getFieldList().add(fvm);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds a small seperator
    **/
    public void addSeparator_1()
    {
        this._addField(new FieldModel(null,null,null,null,FieldModel.TYPE_SEP_1,false,-1,-1,null,null,null));
    }

    /**
    *** Adds a long seperator
    **/
    public void addSeparator_2()
    {
        this._addField(new FieldModel(null,null,null,null,FieldModel.TYPE_SEP_2,false,-1,-1,null,null,null));
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds an Input-Hidden FieldModel entry
    **/
    public void addInputHidden(String id, 
        Object value)
    {
        this._addField(new FieldModel(null,null,null,null,FieldModel.TYPE_HIDDEN,false,-1,-1,null,value,null));
    }

    // --------------------------------

    /**
    *** Adds an Input-Text FieldModel entry
    **/
    public void addInputText(String id, 
        String title, String tooltip, 
        boolean editable, int size, int maxLen, 
        Object value, 
        String onClick)
    {
        this._addField(new FieldModel(id,null,title,tooltip,FieldModel.TYPE_TEXT,editable,size,maxLen,null,value,onClick));
    }

    // --------------------------------

    /**
    *** Adds an Input-Select (pulldown menu) FieldModel entry
    **/
    public void addInputSelect(String id, 
        String title, String tooltip, 
        boolean editable, int size, 
        ComboMap valMap, ComboOption value, 
        String onChange)
    {
        this._addField(new FieldModel(id,null,title,tooltip,FieldModel.TYPE_SELECT,editable,size,-1,valMap,value,onChange));
    }

    /**
    *** Adds an Input-Select (pulldown menu) FieldModel entry
    **/
    public void addInputSelect(String id, 
        String title, String tooltip, 
        boolean editable, int size, 
        ComboMap valMap, Object value, 
        String onChange)
    {
        ComboOption val = ComboMap.getComboOption(valMap, StringTools.trim(value));
        this.addInputSelect(id, title, tooltip, editable, size, valMap, val, onChange);
    }

    // --------------------------------

    /**
    *** Adds an Input-Checkbox FieldModel entry
    **/
    public void addInputCheckbox(String id, 
        String title, String tooltip, 
        boolean editable, 
        Boolean value, 
        String onChange)
    {
        this._addField(new FieldModel(id,null,title,tooltip,FieldModel.TYPE_CHECKBOX,editable,-1,-1,null,value,onChange));
    }

    /**
    *** Adds an Input-Checkbox FieldModel entry
    **/
    public void addInputCheckbox(String id, 
        String title, String tooltip, 
        boolean editable, 
        boolean value, 
        String onChange)
    {
        Boolean val = new Boolean(value);
        this.addInputCheckbox(id, title, tooltip, editable, val, onChange);
    }

    /**
    *** Adds an Input-Checkbox FieldModel entry
    **/
    public void addInputCheckbox(String id, 
        String title, String tooltip, 
        boolean editable, 
        Object value, 
        String onChange)
    {
        Boolean val = new Boolean(StringTools.parseBoolean(value,false));
        this.addInputCheckbox(id, title, tooltip, editable, val, onChange);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** A RequestProperties property accessor class.
    **/
    public static class FTL_FieldListModel // TemplateHashModel
        extends FTLKeyValueHash
    {
        private FieldListModel fldListModel = null;
        public FTL_FieldListModel(FieldListModel flm) {
            super();
            this.fldListModel = flm;
        }
        protected Object _getValue(String key, String arg, Object dft) {
            if (this.fldListModel != null) {
                return this.fldListModel.getValue(key,arg,dft);
            } else {
                return dft;
            }
        }
    }

    /** 
    *** Wraps the FieldListModel in an accessor class
    **/
    public static Object WrapFieldListModel(FieldListModel flm)
    {
        if (flm != null) {
            return new FTL_FieldListModel(flm);
        } else {
            return new FTL_FieldListModel(new FieldListModel());
        }
    }

    /**
    *** Gets a property value from this PropertyModel
    **/
    public Object get(Object key)
    {
        if (!(key instanceof String)) {
            return super.get(key);
        } else 
        if (((String)key).equalsIgnoreCase(PROP_list)) {
            return this.getFieldList();
        } else {
            Object val = super.get(key);
            return val;
        }
    }

    /**
    *** Gets a property value from this PropertyModel
    **/
    public Object getValue(String key, String arg, Object dft)
    {
        return this.get(key);
    }

    // ------------------------------------------------------------------------

}
