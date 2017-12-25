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

public class SelectionListModel
    extends FormModel
    implements GetValue
{

    // ------------------------------------------------------------------------

    public static final String PROP_header      = "header";
    public static final String PROP_rows        = "rows";

    // -- Columns
    public static final String PROP_name        = "name";
    public static final String PROP_id          = "id";
    public static final String PROP_radio       = "radio";
    public static final String PROP_checked     = "checked";
    public static final String PROP_labelFor    = "labelFor";
    public static final String PROP_nowrap      = "nowrap";
    public static final String PROP_style       = "style";
    public static final String PROP_sortKey     = "sortKey";
    public static final String PROP_value       = "value";

    // -- Header/Row
    public static final String PROP_columns     = "columns";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* replace blank strings with html "&nbsp;" */
    public static String FilterText(String s)
    {
        // Don't use StringTools.isBlank(...) - spaces are significant
        return ((s==null)||s.equals(""))? StringTools.HTML_SP : StringTools.htmlFilterText(s);
    }

    /* replace blank strings with html "&nbsp;" */
    public static String FilterValue(String s)
    {
        return StringTools.isBlank(s)? "" : StringTools.htmlFilterValue(s);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class Column
        extends PropertyModel
    {
        public Column(String val) {
            super(true);
            this.put(PROP_value, StringTools.trim(val));
        }
        public Column setSelect(String name, String id, boolean checked) {
            this.setName(name);
            this.setID(id);
            this.put(PROP_radio, "true");
            this.put(PROP_checked, checked?"true":"false");
            return this;
        }
        public Column setName(String n) {
            this.put(PROP_name, StringTools.trim(n));
            return this;
        }
        public Column setID(String ID) {
            this.put(PROP_id, StringTools.trim(ID));
            return this;
        }
        public Column setLabelFor(String lblFor) {
            this.put(PROP_labelFor, StringTools.trim(lblFor));
            return this;
        }
        public Column setWrap() {
            this.setNowrap(false);
            return this;
        }
        public Column setNowrap() {
            this.setNowrap(true);
            return this;
        }
        public Column setNowrap(boolean nowrap) {
            this.put(PROP_nowrap, nowrap?"true":"false");
            return this;
        }
        public Column setStyle(String style) {
            this.put(PROP_style, StringTools.trim(style));
            return this;
        }
        public Column setSortKey(String sortKey) {
            this.put(PROP_sortKey, StringTools.trim(sortKey));
            return this;
        }
        public Column setSortKey(int sortKey) {
            this.put(PROP_sortKey, String.valueOf(sortKey));
            return this;
        }
        public Object get(Object key) {
            return super.get(key);
        }
    }

    public static class Columns
        extends PropertyModel
    {
        public Columns() {
            super();
            this.put(PROP_columns,new Vector<Column>());
        }
        public Column addColumn(Column col) {
            if (col != null) {
                @SuppressWarnings("unchecked")
                Vector<Column> cols = (Vector<Column>)this.get(PROP_columns);
                cols.add(col);
            }
            return col;
        }
        public Column addColumn(String title) {
            return this.addColumn(new Column(title));
        }
        public Object get(Object key) {
            return super.get(key);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private Columns          header = new Columns();
    private Vector<Columns>  rows   = new Vector<Columns>();

    /**
    *** Constructor 
    **/
    public SelectionListModel()
    {
        super();
        this.put(PROP_header, this.header);
        this.put(PROP_rows  , this.rows);
    }

    // ------------------------------------------------------------------------

    /**
    *** gets the Header columns
    **/
    public Columns getHeader()
    {
        return this.header;
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a new Row
    **/
    public Columns newRow()
    {
        Columns row = new Columns();
        this.rows.add(row);
        return row;
    }

    // ------------------------------------------------------------------------

    /**
    *** A RequestProperties property accessor class.
    **/
    public static class FTL_SelectionListModel // TemplateHashModel
        extends FTLKeyValueHash
    {
        private SelectionListModel selListModel = null;
        public FTL_SelectionListModel(SelectionListModel slm) {
            super();
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
    *** Wraps the SelectionListModel in an accessor class
    **/
    public static Object WrapSelectionListModel(SelectionListModel slm)
    {
        if (slm != null) {
            return new FTL_SelectionListModel(slm);
        } else {
            return new FTL_SelectionListModel(new SelectionListModel());
        }
    }

    /**
    *** Passthrough to PropertyModel
    **/
    public Object get(Object key)
    {
        return super.get(key);
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
