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

public class FormModel
    extends PropertyModel
    implements GetValue
{

    // ------------------------------------------------------------------------

    public static final String PROP_formID       = "formID";
    public static final String PROP_formAction   = "formAction";
    public static final String PROP_formMethod   = "formMethod";

    public static final String PROP_formTitle    = "formTitle";

    public static final String PROP_btnView      = "btnView";
    public static final String PROP_btnEdit      = "btnEdit";
    public static final String PROP_btnDelete    = "btnDelete";
    public static final String PROP_btnRefresh   = "btnRefresh";
    public static final String PROP_btnNew       = "btnNew";
    public static final String PROP_btnChange    = "btnChange";
    public static final String PROP_btnCommand   = "btnCommand";
    public static final String PROP_btnSms       = "btnSms";
    public static final String PROP_btnCancel    = "btnCancel";
    public static final String PROP_btnBack      = "btnBack";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    public FormModel(boolean blankDefault)
    {
        super(blankDefault);
    }

    /**
    *** Constructor
    **/
    public FormModel()
    {
        super();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** PropertyModel passthoough
    **/
    public Object get(Object key)
    {
        return super.get(key);
    }

    /**
    *** GetValue interface
    **/
    public Object getValue(String key, String arg, Object dft)
    {
        return super.get(key);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the frame title
    **/
    public void setFormTitle(String title)
    {
        this.put(PROP_formTitle, StringTools.trim(title));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the form 
    **/
    public void setForm(String ID, String actn)
    {
        this.put(PROP_formID    , StringTools.trim(ID));
        this.put(PROP_formAction, StringTools.trim(actn));
        this.put(PROP_formMethod, "POST");
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds a form submit button
    **/
    public ButtonModel addFormSubmit(String propKey, String name, String title)
    {
        ButtonModel btn = new ButtonModel(ButtonModel.TYPE_SUBMIT,name,title);
        this.put(propKey, btn);
        return btn;
    }

    /**
    *** Add an form onClick button
    **/
    public ButtonModel addFormButton(String propKey, String name, String title, String onClick)
    {
        ButtonModel btn = new ButtonModel(ButtonModel.TYPE_BUTTON,name,title).setOnClick(onClick);
        this.put(propKey, btn);
        return btn;
    }

    // ------------------------------------------------------------------------

}
