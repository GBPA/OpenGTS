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
//  2008/07/21  Martin D. Flynn
//     -Initial release
//  2010/11/29  Martin D. Flynn
//     -Added "default=" attribute
//  2017/03/14  Martin D. Flynn
//     -Move track section output to RequestProperties
// ----------------------------------------------------------------------------
package org.opengts.war.track.taglib;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class TrackTag 
    extends BodyTagSupport
    implements Constants
{

    // ------------------------------------------------------------------------

    private static final char   PropSeparator                   = StringTools.PropertySeparatorSEMIC;
    private static final char   KeyValSeparator[]               = new char[] { StringTools.KeyValSeparatorCOLON };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String section = null;

    /**
    *** Gets the "section" attribute
    *** @return The "section" attribute
    **/
    public String getSection()
    {
        return this.section;
    }
    
    /**
    *** Sets the "section" attribute
    *** @param s  The "section" attribute value
    **/
    public void setSection(String s)
    {
        //Print.sysPrintln("Taglib section=" + s);
        this.section = s;
    }
    
    // ------------------------------------------------------------------------

    private String options = null;

    /**
    *** Gets the "option" attribute
    *** @return The "option" attribute
    **/
    public String getOptions()
    {
        return this.options;
    }
    
    /**
    *** Sets the "option" attribute
    *** @param opt  The "option" attribute value
    **/
    public void setOptions(String opt)
    {
        this.options = opt;
    }
    
    /**
    *** Returns an RTProperties instance based on the supplied options
    *** @return A RTProperties instance, or null if there are no properties
    **/
    public RTProperties getProperties()
    {
        if (!StringTools.isBlank(this.options)) {
            return new RTProperties(this.options,PropSeparator,KeyValSeparator);
        } else {
            return null;
        }
    }
    
    // ------------------------------------------------------------------------

    private String arg = null;

    /**
    *** Gets the "arg" attribute
    *** @return The "arg" attribute
    **/
    public String getArg()
    {
        return this.arg;
    }
    
    /**
    *** Sets the "arg" attribute
    *** @param arg  The "arg" attribute value
    **/
    public void setArg(String arg)
    {
        this.arg = arg;
    }

    // ------------------------------------------------------------------------

    private String dft = null;

    /**
    *** Gets the "default" attribute
    *** @return The "default" attribute
    **/
    public String getDefault()
    {
        return this.dft;
    }
    
    /**
    *** Returns true if a default is defined
    *** @return True is a default is defined
    **/
    public boolean hasDefault()
    {
        return !StringTools.isBlank(this.dft);
    }
    
    /**
    *** Sets the "default" attribute
    *** @param dft  The "default" attribute value
    **/
    public void setDefault(String dft)
    {
        this.dft = dft;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String COMPARE_EQ              = "eq";         // ==
    private static final String COMPARE_NE              = "ne";         // !=
    private static final String COMPARE_GT              = "gt";         // >
    private static final String COMPARE_GE              = "ge";         // >=
    private static final String COMPARE_LT              = "lt";         // <
    private static final String COMPARE_LE              = "le";         // <=
    private static final String COMPARE_DEFINED         = "defined";    // not in set

    private static final String BOOLEAN_TRUE            = "true";
    private static final String BOOLEAN_FALSE           = "false";

    private String  ifKey           = null;
    private String  ifCompare       = null;
    private String  ifCompareType   = COMPARE_EQ;

    /**
    *** Gets the "ifKey" attribute
    *** @return The "ifKey" attribute
    **/
    public String getIfKey()
    {
        return this.getIf();
    }

    /**
    *** Gets the "if" attribute
    *** @return The "if" attribute
    **/
    public String getIf()
    {
        return this.ifKey;
    }

    /**
    *** Gets the "ifDefined" attribute
    *** @return The "ifDefined" attribute
    **/
    public String getIfDefined()
    {
        if ((this.ifCompare != null) && this.ifCompare.equalsIgnoreCase(BOOLEAN_TRUE)) {
            return this.ifKey;
        } else {
            return null;
        }
    }

    /**
    *** Gets the "ifTrue" attribute
    *** @return The "ifTrue" attribute
    **/
    public String getIfTrue()
    {
        if ((this.ifCompare != null) && this.ifCompare.equalsIgnoreCase(BOOLEAN_TRUE)) {
            return this.ifKey;
        } else {
            return null;
        }
    }

    /**
    *** Gets the "ifFalse" attribute
    *** @return The "ifFalse" attribute
    **/
    public String getIfFalse()
    {
        if ((this.ifCompare != null) && this.ifCompare.equalsIgnoreCase(BOOLEAN_FALSE)) {
            return this.ifKey;
        } else {
            return null;
        }
    }

    /**
    *** Gets the comparison value
    *** @return The comparison value
    **/
    public String getValue()
    {
        return !StringTools.isBlank(this.ifCompare)? this.ifCompare : BOOLEAN_TRUE;
    }

    /**
    *** Gets the "compare" type
    *** @return The "compare" type
    **/
    public String getCompare()
    {
        return !StringTools.isBlank(this.ifCompareType)? this.ifCompareType : COMPARE_EQ;
    }

    // --------------------------------

    /**
    *** Sets the "ifKey" attribute
    *** @param k  The "ifKey" attribute value
    **/
    public void setIfKey(String k)
    {
        this.setIf(k);
    }

    /**
    *** Sets the "if" attribute
    *** @param k  The "if" attribute value
    **/
    public void setIf(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = null;
      //this.ifCompareType  = COMPARE_EQ; <-- explicitly set later
    }

    /**
    *** Sets the "ifDefined" attribute
    *** @param k  The "ifDefined" attribute value
    **/
    public void setIfDefined(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = BOOLEAN_TRUE;
        this.ifCompareType  = COMPARE_DEFINED;
    }

    /**
    *** Sets the "ifTrue" attribute
    *** @param k  The "ifTrue" attribute value
    **/
    public void setIfTrue(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = BOOLEAN_TRUE;
        this.ifCompareType  = COMPARE_EQ;
    }

    /**
    *** Sets the "ifFalse" attribute
    *** @param k  The "ifFalse" attribute value
    **/
    public void setIfFalse(String k)
    {
        this.ifKey          = k;
        this.ifCompare      = BOOLEAN_FALSE;
        this.ifCompareType  = COMPARE_EQ;
    }

    /**
    *** Sets the "ifPage" attribute
    *** @param p  The page name
    **/
    public void setIfPage(String p)
    {
        this.ifKey          = RequestProperties.KEY_pageName;
        this.ifCompare      = StringTools.trim(p);
        this.ifCompareType  = COMPARE_EQ;
    }

    /**
    *** Sets the "ifNotPage" attribute
    *** @param p  The page name
    **/
    public void setIfNotPage(String p)
    {
        this.ifKey          = RequestProperties.KEY_pageName;
        this.ifCompare      = StringTools.trim(p);
        this.ifCompareType  = COMPARE_NE;
    }

    /**
    *** Sets the comparison value
    *** @param val  The comparison value
    **/
    public void setValue(String val)
    {
        this.ifCompare = val;
    }

    /**
    *** Sets the "compare" type
    *** @param comp  The "compare" type
    **/
    public void setCompare(String comp)
    {
        this.ifCompareType = comp;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Session attribute for the specified key
    *** @param key  The attribute key
    *** @param dft  The default value
    *** @return The value for the specified key
    **/
    public String getAttributeValue(String key, String dft)
    {
        if (!StringTools.isBlank(key)) {
            ServletRequest request = super.pageContext.getRequest();
            RequestProperties rp = (RequestProperties)request.getAttribute(CommonServlet.PARM_REQSTATE);
            if (rp != null) {
                String v = rp.getKeyValue(key, null/*arg*/, null/*dft*/);
                if (v != null) {
                    return v;
                }
            } else {
                Print.logWarn("RequestProperties is null!!!");
            }
        }
        return dft;

    }

    // "StringTools.KeyValueMap" interface
    public String getKeyValue(String key, String arg, String dft)
    {
        return this.getAttributeValue(key, dft);
    }

    /**
    *** Returns true if the attribute key matches the current comparison value, based on the
    *** comparison type.
    **/
    public boolean isMatch()
    {

        /* key 'ifKey' */
        String ifKY = this.getIfKey();
        if (StringTools.isBlank(ifKY)) {
            // -- key not defined (always true)
            return true;
        }

        /* check "if" comparison */
        String  ifCT  = this.getCompare().toLowerCase();
        String  ifCV  = this.getValue();                        // constant (not null)
        String  ifKV  = this.getAttributeValue(ifKY, null);     // variable (may be null)
        boolean match = this._compare(ifCT, ifCV, ifKV);
        //Print.logInfo("Compare: "+ifKV+" "+ifCT+" "+ifCV+" ==> "+match);

        return match;

    }
    
    private boolean _compare(String ct, String cv, String kv)
    {
        // ct == CompareType
        // cv == CompareValue
        // kv == KeyValue
        boolean match = false;
        if (ct.equals(COMPARE_EQ)) {
            // -- compare equals
            match = (kv != null)?  kv.equalsIgnoreCase(cv) : false;
        } else
        if (ct.equals(COMPARE_NE)) {
            // -- compare not equals
            match = (kv != null)? !kv.equalsIgnoreCase(cv) : true;
        } else
        if (kv == null) {
            // -- no value
            match = false;
        } else
        if (ct.equals(COMPARE_GT)) {
            // -- compare greater-than
            match = (StringTools.parseDouble(kv,0.0) >  StringTools.parseDouble(cv,0.0));
        } else
        if (ct.equals(COMPARE_GE)) {
            // -- compare greater-than-or-equals-to
            match = (StringTools.parseDouble(kv,0.0) >= StringTools.parseDouble(cv,0.0));
        } else
        if (ct.equals(COMPARE_LT)) {
            // -- compare less-than
            match = (StringTools.parseDouble(kv,0.0) <  StringTools.parseDouble(cv,0.0));
        } else
        if (ct.equals(COMPARE_LE)) {
            // -- compare less-than-or-equals-to
            match = (StringTools.parseDouble(kv,0.0) <= StringTools.parseDouble(cv,0.0));
        } else
        if (ct.equals(COMPARE_DEFINED)) {
            // -- compare defined
            boolean def = StringTools.parseBoolean(cv,true);
            if (!RTConfig.hasProperty(kv)) {
                match = !def; // not defined
            } else 
            if (StringTools.isBlank(RTConfig.getString(kv,null))) {
                match = !def; // has blank value
            } else {
                match = def; // defined and non-blank
            }
        } else {
            // -- false
            match = false;
        }
        return match;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public int doStartTag()
        throws JspTagException
    {
        if (this.isMatch()) {
            return EVAL_BODY_BUFFERED;
        } else {
            // -- no-match, do not process this tag-block
            return SKIP_BODY;
        }
    }

    public int doEndTag()
        throws JspTagException
    {
        HttpServletRequest request = (HttpServletRequest)super.pageContext.getRequest();
        RequestProperties reqState = (RequestProperties)request.getAttribute(CommonServlet.SECTION_REQUESTPROPS);

        /* ignore blank section definitions */
        String key = this.getSection().toLowerCase();
        if (StringTools.isBlank(key)) {
            // -- ignore
            return EVAL_PAGE;
        }

        /* not a match? */
        if (!this.isMatch()) {
            // -- ignore
            return EVAL_PAGE;
        }

        /* write section to JSP output stream */
        try {
            if (reqState != null) {
                JspWriter jspOut = super.pageContext.getOut();
                PrintWriter pw = new PrintWriter(jspOut, jspOut.isAutoFlush());
                String keyArg = this.getArg();
                String keyDft = this.getDefault();
                reqState.writeTrackSection(request, pw, key, keyArg, keyDft);
            } else {
                Print.logError("RequestProperties not defined!");
            }
            return EVAL_PAGE;
        } catch (IOException ioe) {
            throw new JspTagException(ioe.toString(), ioe);
        } catch (Throwable th) {
            throw new JspTagException(th.toString(), th);
        }

    }

    // ------------------------------------------------------------------------
        
    public void setBodyContent(BodyContent body)
    {
        super.setBodyContent(body);
    }
    
    /**
    *** Invoked before the body of the tag is evaluated but after body content is set
    **/
    public void doInitBody()
        throws JspException
    {
        // invoked after 'setBodyContent'
        super.doInitBody();
    }
    
    /**
    *** Invoked after body content is evaluated
    **/
    public int doAfterBody()
        throws JspException
    {
        // invoked after 'doInitBody'        
        return SKIP_BODY; // EVAL_BODY_TAG loops
    }

    // ------------------------------------------------------------------------

    public void release()
    {
        this.section = null;
        this.options = null;
    }

}
