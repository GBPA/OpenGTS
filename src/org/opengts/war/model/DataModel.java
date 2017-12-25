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

import org.opengts.war.tools.*;

public class DataModel
    extends PropertyModel
    implements GetValue
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DataModel/PropertyModel
    //      SelectionListModel/FormModel/PropertyModel
    //          formID
    //          formAction          (URL)
    //          formMethod          ("POST")
    //          formTitle
    //          btnView
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnEdit
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnDelete
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnRefresh
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnNew
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnChange
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnCommand
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnSms
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnCancel
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          btnBack
    //              type            ("SUBMIT", "BUTTON")
    //              name
    //              title
    //              url             (may be null)
    //              onClick         (JavaScript callback - may be null)
    //          header
    //              columns[]
    //                  name        (name=)
    //                  id          (id=)
    //                  labelFor    (<label for=...>)
    //                  nowrap      ("nowrap")
    //                  style       (optional "style")
    //                  sortKey     (sort-by key)
    //                  radio       ("true" if should be radio-button)
    //                  checked     ("true" if radio-button and should be checked/selected)
    //                  value
    //          rows
    //              columns[]
    //                  name        (name=)
    //                  id          (id=)
    //                  labelFor    (<label for=...>)
    //                  nowrap      ("nowrap")
    //                  style       (optional "style")
    //                  sortKey     (sort-by key)
    //                  radio       ("true" if should be radio-button)
    //                  checked     ("true" if radio-button and should be checked/selected)
    //                  value
    //      FieldListModel/FormModel/PropertyModel
    //          formID
    //          formAction          (URL)
    //          formMethod          ("POST")
    //          formTitle
    //          list
    //              FieldModel[]
    //                  id
    //                  title
    //                  type        ("TEXT","SELECT","CHECKBOX","DATE","DATETIME","PUSHPIN","HIDDEN","SEP_1","SEP_2")
    //                  size        (preferred display size)
    //                  length      (maximum field length)
    //                  editable
    //                  valueMap[]  (List of acceptable selection values)
    //                  value
    //                  styleClass  (optional style class)
    //                  onClick     (JavaScript callback)
    //
    // ========================================================================
    //  PageContext               : ${model.pageContext!"PageContext?"}
    //  FrameTitle                : ${model.frameTitle!"FrameTitle?"}
    //  FieldListModel.formID     : ${model.fields.formID!"FormID?"}
    //  FieldListModel.formAction : ${model.fields.formAction!"FormAction?"}
    //  FieldListModel.formTitle  : ${model.fields.formTitle!"FormTitle?"}
    //  FieldListModel.list       :
    //    <#list model.fields.list as fld>
    //    Field: ${fld.id} ${fld.title}
    //    </#list>
    //  <#if model.fields.a_id??>
    //  FieldListModel.fields.a_id.id : ${model.fields.a_id.id!"AccountID?"}
    //  </#if>
    //  FieldListModel.btnChange  :
    //  <#if model.fields.btnChange??>
    //    FieldListModel.btnChange.id   : ${model.fields.btnChange.id!"BtnChange.id?"}
    //    FieldListModel.btnChange.name : ${model.fields.btnChange.name!"BtnChange.name?"}
    //    FieldListModel.btnChange.title: ${model.fields.btnChange.title!"BtnChange.title?"}
    //  </#if>
    //  SelectionListModel.formTitle  : ${model.selection.formTitle!"FormTitle?"}
    //  Header columns
    //  <#list model.selection.header.columns as col>
    //    Header: ${col.name} ${col.value}
    //  </#list>
    //  Rows:
    //  <#list model.selection.rows as row>
    //    ========
    //    <#list row.columns as col>
    //    Row: ${col.name} ${col.value}
    //    </#list>
    //  </#list>
    // ========================================================================

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String PROP_WebPage      = "webPage";

    public static final String PROP_pageContext  = "pageContext";
    public static final String PROP_frameTitle   = "frameTitle";

    public static final String PROP_message      = "message";
    public static final String PROP_hasError     = "hasError";

    public static final String PROP_selection    = "selection"; // SelectionListModel

    public static final String PROP_fields       = "fields";    // FieldListModel

    // ------------------------------------------------------------------------

    public static final String CONTEXT_UNKNOWN   = "";
    public static final String CONTEXT_EDIT      = "EDIT";
    public static final String CONTEXT_VIEW      = "VIEW";
    public static final String CONTEXT_LIST      = "LIST";
    public static final String CONTEXT_COMMANDS  = "COMMANDS"; // DeviceInfo only

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** A DataModel accessor class.
    **/
    public static class FTL_DataModelHash // TemplateHashModel
        extends FTLKeyValueHash
    {
        private DataModel model = null;
        public FTL_DataModelHash(DataModel model) {
            super();
            this.model = model;
        }
        public DataModel getModel() {
            return this.model;
        }
        protected Object _getValue(String key, String arg, Object dft) {
            Object obj;
            DataModel dm = this.getModel();
            if (dm != null) {
                obj = dm.getValue(key, arg, dft);
            } else {
                obj = dft;
            }
            return obj;
        }
    }

    /** 
    *** Wraps the DataModel in an accessor class
    **/
    public static Object WrapDataModel(DataModel model)
    {
        if (model != null) {
            model.initModel();
            return new FTL_DataModelHash(model);
        } else {
            return new FTL_DataModelHash(new DataModel());
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
        if (((String)key).equalsIgnoreCase(PROP_selection)) {
            this.getSelectionListModel();
            return super.get(key,null);
        } else
        if (((String)key).equalsIgnoreCase(PROP_fields)) {
            this.getFieldListModel();
            return super.get(key,null);
        } else {
            return super.get(key);
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
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public DataModel()
    {
        super(true);
    }

    /**
    *** Constructor
    **/
    public DataModel(WebPage wp, String msg, boolean hasError)
    {
        this();
        this.put(PROP_WebPage, wp);
        this.setMessage(msg, hasError);
    }

    /**
    *** Constructor
    **/
    public DataModel(WebPage wp)
    {
        this(wp, "", false);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the page context.
    *** This is typically one of "EDIT", "VIEW", "LIST", but also be other values 
    *** depending on the context.
    **/
    public void setPageContext(String context)
    {
        this.put(PROP_pageContext, StringTools.trim(context).toUpperCase());
    }
    
    /**
    *** Gets the page context
    **/
    public String getPageContext()
    {
        return StringTools.trim(this.get(PROP_pageContext));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the frame title
    **/
    public void setFrameTitle(String title)
    {
        this.put(PROP_frameTitle, StringTools.trim(title));
    }

    /**
    *** Sets the frame title
    **/
    public String getFrameTitle(String title)
    {
        return StringTools.trim(this.get(PROP_frameTitle));
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sets the message
    **/
    public void setMessage(String msg, boolean isErr)
    {
        this.put(PROP_message , StringTools.trim(msg));
        this.put(PROP_hasError, isErr? "true" : "false");
    }
    
    /**
    *** Gets the message
    **/
    public String getMessage()
    {
        return StringTools.trim(this.get(PROP_message));
    }
    
    /**
    *** Returns true if the page has an error 
    **/
    public boolean hasError()
    {
        return StringTools.parseBoolean(this.get(PROP_hasError),false);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private SelectionListModel selectionListModel = null;

    /**
    *** A RequestProperties property accessor class.
    **/
    public static class FTL_SelectionListModel // TemplateHashModel
        extends FTLKeyValueHash
    {
        private SelectionListModel selListModel = null;
        public FTL_SelectionListModel(SelectionListModel slm) {
            super(true);
            this.selListModel = slm;
        }
        protected Object _getValue(String key, String arg, Object dft) {
            if (this.selListModel != null) {
                return this.selListModel.getValue(key,arg,dft);
            } else {
                return dft;
            }
        }
    }

    /**
    *** Gets the SelectionListModel
    **/
    public SelectionListModel getSelectionListModel()
    {
        if (this.selectionListModel == null) {
            this.selectionListModel = new SelectionListModel();
            this.put(PROP_selection, SelectionListModel.WrapSelectionListModel(this.selectionListModel));
        }
        return this.selectionListModel;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private FieldListModel fieldListModel = null;

    /**
    *** Gets the FieldListModel
    **/
    public FieldListModel getFieldListModel()
    {
        if (this.fieldListModel == null) {
            this.fieldListModel = new FieldListModel();
            this.put(PROP_fields, FieldListModel.WrapFieldListModel(this.fieldListModel));
        }
        return this.fieldListModel;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Initialized the data model.  This method is intended to be overridden.
    *** The default implementation does nothing.
    **/
    public void initModel() 
    {
        // -- override
    }

    // ------------------------------------------------------------------------

}
