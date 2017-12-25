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
//  2009/08/07  Martin D. Flynn
//     -Initial release
//  2015/05/03  Martin D. Flynn
//     -Added support for obfuscating Authorization attributes.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;
import java.net.*;
import java.math.BigInteger;

import java.lang.management.*; 
import javax.management.*; 
import javax.management.remote.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
*** ServoceRequest tools
**/

public class ServiceRequest
{

    // ------------------------------------------------------------------------

    protected static final String  SRTAG_GTSRequest     = "GTSRequest";
    protected static final String  SRTAG_GTSResponse    = "GTSResponse";
    protected static final String  SRTAG_Message        = "Message";
    protected static final String  SRTAG_Authorization  = "Authorization";

    protected static final String  SRATTR_command       = "command";
    protected static final String  SRATTR_result        = "result";
    protected static final String  SRATTR_code          = "code";

    protected static final String  SRATTR_account       = "account";
    protected static final String  SRATTR_user          = "user";
    protected static final String  SRATTR_password      = "password";
    protected static final String  SRATTR_auth          = "auth";

    protected static final String  METHOD_handleRequest = "handleRequest";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Request Body handler class
    **/
    public interface RequestBody
    {
        public StringBuffer appendRequestBody(StringBuffer sb, int indent);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final String AUTH_ENCODING_PREFIX = "%%";

    /**
    *** Authorization class
    **/
    public static class Authorization
    {
        private ServiceRequest servReq      = null;
        private String         accountID    = null;
        private String         userID       = null;
        private String         password     = null;
        private byte           obfAuthKey[] = null;
        public Authorization() {
            super();
        }
        public Authorization(Authorization other) {
            this();
            if (other != null) {
                this.accountID  = other.accountID;
                this.userID     = other.userID;
                this.password   = other.password;
                this.setObfuscationKey(other.obfAuthKey);
            }
        }
        public Authorization(String aid, String uid, String pwd, Object authKey) {
            this();
            this.servReq    = null;
            this.accountID  = StringTools.trim(aid);
            this.userID     = StringTools.trim(uid);
            this.password   = pwd;
            this.setObfuscationKey(authKey);
        }
        public Authorization(String aup, Object authKey) {
            this();
            this.servReq    = null;
            this.setObfuscationKey(authKey);
            // -- decode
            String auth;
            if (!ListTools.isEmpty(this.obfAuthKey)) {
                try {
                    byte b[] = Base64.decode(aup, new BigInteger(this.obfAuthKey));
                    auth = StringTools.isPrintableASCII(b)? StringTools.toStringValue(b) : "";
                } catch (Base64.Base64DecodeException bde) {
                    // -- unable to decode "auth"
                    auth = null;
                }
            } else {
                auth = aup;
            }
            // -- parse
            if (!StringTools.isBlank(auth) && auth.startsWith(ServiceRequest.AUTH_ENCODING_PREFIX)) {
                String s = auth.substring(ServiceRequest.AUTH_ENCODING_PREFIX.length());
                String v[] = StringTools.parseArray(s,',');
                this.accountID = (v.length > 0)? v[0] : null;
                this.userID    = (v.length > 1)? v[1] : null;
                this.password  = (v.length > 2)? v[2] : null;
            }
        }
        public void setObfuscationKey(Object authKey) {
            // -- The purpose of the Obfuscation key is to hide the account/user/password information
            // -  in the XML request.  Note that this is not an encryption, so it is not uncrackable.
            if (authKey == null) {
                this.obfAuthKey = null;
            } else
            if (authKey instanceof byte[]) {
                byte b[] = (byte[])authKey;
                this.obfAuthKey = new byte[b.length];
                System.arraycopy(b,0,this.obfAuthKey,0,this.obfAuthKey.length);
            } else
            if (authKey instanceof String) {
                this.obfAuthKey = ((String)authKey).getBytes();
            } else {
                this.obfAuthKey = authKey.toString().getBytes();
            }
        }
        public void setServiceRequest(ServiceRequest servReq) {
            this.servReq = servReq;
        }
        public StringBuffer toXML(StringBuffer sb, int indent) {
            if (sb == null) { sb = new StringBuffer(); }
            sb.append(StringTools.replicateString(" ", indent));
            sb.append("<"+this.getTagAuthorization());
            if (!ListTools.isEmpty(this.obfAuthKey)) {
                // -- obfuscate authorization parameters account/user/password
                // -  "%%ACCOUNT,USER,PASSWORD"
                StringBuffer authSB = new StringBuffer();
                authSB.append(AUTH_ENCODING_PREFIX);
                authSB.append(StringTools.trim(this.accountID));
                authSB.append(",");
                authSB.append(StringTools.trim(this.userID));
                authSB.append(",");
                authSB.append(StringTools.trim(this.password));
                String rtpEnc = Base64.encode(authSB.toString(), new BigInteger(this.obfAuthKey));
                sb.append(" "+this.getAttrAuth()+"=\""+rtpEnc+"\"");
            } else {
                // -- clear-text authorization parameters account/user/password
                if (!StringTools.isBlank(this.accountID)) {
                    sb.append(" "+this.getAttrAccount()+"=\""+this.accountID+"\"");
                }
                if (!StringTools.isBlank(this.userID)) {
                    sb.append(" "+this.getAttrUser()+"=\""+this.userID+"\"");
                }
                if (!StringTools.isBlank(this.password)) {
                    sb.append(" "+this.getAttrPassword()+"=\""+this.password+"\"");
                }
            }
            sb.append("/>\n");
            return sb;
        }
        public String getAccountID() {
            return this.accountID;
        }
        public String getUserID() {
            return this.userID;
        }
        public String getPassword() {
            return this.password;
        }
        public String toString() {
            return this.toXML(null,3).toString();
        }
        public String getTagAuthorization() {
            return (this.servReq != null)? this.servReq.getTagAuthorization() : SRTAG_Authorization;
        }
        public String getAttrAccount() {
            return (this.servReq != null)? this.servReq.getAttrAccount() : SRATTR_account;
        }
        public String getAttrUser() {
            return (this.servReq != null)? this.servReq.getAttrUser() : SRATTR_user;
        }
        public String getAttrPassword() {
            return (this.servReq != null)? this.servReq.getAttrPassword() : SRATTR_password;
        }
        public String getAttrAuth() {
            return (this.servReq != null)? this.servReq.getAttrAuth() : SRATTR_auth;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private JMXServiceURL   jmxURL              = null;
    private String          jmxObjectName       = "";
    
    private URL             serviceURL          = null;
    
    private Authorization   auth                = null;
        
    private String          command             = null;
    private RequestBody     requestBody         = null;

    /**
    *** Constructor
    **/
    public ServiceRequest()
    {
        super();
    }

    /**
    *** Clone Constructor
    **/
    public ServiceRequest(ServiceRequest other)
    {
        super();
        if (other != null) {
            this.jmxURL         = other.jmxURL;
            this.jmxObjectName  = other.jmxObjectName;
            this.serviceURL     = other.serviceURL;
            this.command        = other.command;
            this.requestBody    = other.requestBody;
            if (other.auth != null) {
                this.setAuthorization(new Authorization(other.auth));
            }
        }
    }

    /**
    *** Constructor
    *** @param url          The Service URL
    **/
    public ServiceRequest(String url)
        throws MalformedURLException
    {
        this.setURL(url);
    }

    // ----------

    /**
    *** Constructor
    *** @param serviceURL   The Service URL
    **/
    public ServiceRequest(URL serviceURL)
    {
        this.setURL(serviceURL);
    }

    // ----------

    /**
    *** Constructor
    *** @param jmxURL       The JMX Service URL
    **/
    public ServiceRequest(JMXServiceURL jmxURL, String jmxObjName)
    {
        this.setURL(jmxURL, jmxObjName);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the service URL
    *** @param url  The Service URL
    *** @return This ServiceRequest
    **/
    public ServiceRequest setURL(String url)
        throws MalformedURLException
    {
        this.jmxURL = null;
        this.serviceURL = null;
        if (StringTools.startsWithIgnoreCase(url, "http")) {
            this.serviceURL = new URL(url);
        } else
        if (StringTools.startsWithIgnoreCase(url, "service:jmx:")) {
            this.jmxURL = new JMXServiceURL(url);
        } else {
            throw new MalformedURLException("Invalid URL: " + url);
        }
        return this;
    }

    // ----------

    /**
    *** Sets the service URL
    *** @param url  The Service URL
    *** @return This ServiceRequest
    **/
    public ServiceRequest setURL(URL url)
    {
        this.jmxURL = null;
        this.serviceURL = url;
        return this;
    }
    
    /**
    *** Gets the service URL
    *** @return url  The Service URL
    **/
    public URL getURL()
    {
        return this.serviceURL;
    }

    // ----------

    /**
    *** Sets the service URL
    *** @param url  The Service URL
    *** @return This ServiceRequest
    **/
    public ServiceRequest setURL(JMXServiceURL url, String jmxObjName)
    {
        this.jmxURL = url;
        this.setJMXObjectName(jmxObjName);
        this.serviceURL = null;
        return this;
    }
    
    /**
    *** Gets the service URL
    *** @return url  The Service URL
    **/
    public JMXServiceURL getJMXServiceURL()
    {
        return this.jmxURL;
    }

    /**
    *** Returns true if this is a JMX request
    *** @return True if this is a JMX request
    **/
    public boolean isJMX()
    {
        return (this.jmxURL != null);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the JMX service object name
    *** @param objName  The JMX service object name
    **/
    public void setJMXObjectName(String objName)
    {
        this.jmxObjectName = (objName != null)? objName : "";
    }

    /**
    *** Gets the JMX service object name
    *** @return  The JMX service object name
    **/
    public String getJMXObjectName()
    {
        return this.jmxObjectName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Service Authorization
    *** @param acctID       The Authorization account ID
    *** @param userID       The Authorization user ID
    *** @param passwd       The Authorization password
    *** @return This ServiceRequest
    **/
    public ServiceRequest setAuthorization(String acctID, String userID, String passwd, String authKey)
    {
        this.setAuthorization(new Authorization(acctID, userID, passwd, authKey));
        return this;
    }

    /**
    *** Sets the Service Authorization
    *** @param auth      The Authorization wrapper
    *** @return This ServiceRequest
    **/
    public ServiceRequest setAuthorization(Authorization auth)
    {
        this.auth = auth;
        if (this.auth != null) {
            this.auth.setServiceRequest(this);
        }
        return this;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the command ID included in the request header
    *** @param cmd  The command ID
    *** @return This ServiceRequest
    **/
    public ServiceRequest setCommand(String cmd)
    {
        this.command = StringTools.trim(cmd);
        return this;
    }
    
    /**
    *** Gets the command ID included in the request header
    *** @return  The command ID
    **/
    public String getCommand()
    {
        return (this.command != null)? this.command : "";
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the request body handler 
    *** @param rb  The RequestBody handler
    *** @return This ServiceRequest
    **/
    public ServiceRequest setRequestBody(RequestBody rb)
    {
        this.requestBody = rb;
        return this;
    }

    /**
    *** Appends the request body to the specified StringBuffer 
    *** @param sb     The StringBuffer
    *** @param indent The prefixing spaces to include
    *** @return The StringBuffer
    **/
    public StringBuffer appendRequestBody(StringBuffer sb, int indent)
    {
        if (this.requestBody != null) {
            this.requestBody.appendRequestBody(sb, indent);
        }
        return sb;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Assembles and returns an XML request string
    *** @param command  The command ID to include in the header
    *** @param rb       The RequestBody handler
    *** @return The XML request
    **/
    public String toXML(String command, RequestBody rb)
    {
        StringBuffer sb = new StringBuffer();
        int indent = 3;

        /* header */
        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='no' ?>\n");
        sb.append("<"+this.getTagRequest());
        String cmd = StringTools.blankDefault(command,this.getCommand());
        if (!StringTools.isBlank(cmd)) {
            sb.append(" "+this.getAttrCommand()+"=\""+cmd+"\"");
        }
        sb.append(">\n");
        
        /* request authorization */
        if (this.auth != null) {
            this.auth.toXML(sb,indent);
        }
        
        /* request body */
        if (rb != null) {
            rb.appendRequestBody(sb,indent);
        } else {
            this.appendRequestBody(sb,indent);
        }

        /* footer */
        sb.append("</"+this.getTagRequest()+">\n");
        return sb.toString();

    }

    /**
    *** Returns a XML string representation of this instance
    *** @return An XML string representation
    **/
    public String toString()
    {
        return this.toXML(this.getCommand(), null);
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Sends the request and returns the results as an XML Document
    **/
    public Document sendRequest(String command, RequestBody rb)
        throws IOException
    {
        int timeoutMS = -1;
        try {
            return this._sendRequest(this.toXML(command, rb), timeoutMS);
        } catch (HTMLTools.HttpIOException hioe) {
            int    respCode = hioe.getResponseCode();
            String respMsg  = hioe.getResponseMessage();
            Print.logError("HTTP Error: ["+respCode+"] " + respMsg);
            throw hioe;
        }
    }
    
    /**
    *** Sends the request and returns the results as an XML Document
    **/
    public Document sendRequest(File reqFile)
        throws IOException
    {
        int timeoutMS = -1;
        byte rq[] = FileTools.readFile(reqFile);
        if (!ListTools.isEmpty(rq)) {
            String rqStr = StringTools.toStringValue(rq).trim();
            if (rqStr.startsWith("<")) {
                try {
                    return this._sendRequest(rqStr, timeoutMS);
                } catch (HTMLTools.HttpIOException hioe) {
                    int    respCode = hioe.getResponseCode();
                    String respMsg  = hioe.getResponseMessage();
                    Print.logError("HTTP Error: ["+respCode+"] " + respMsg);
                    throw hioe;
                }
            } else {
                Print.logError("Invalid Request XML: \n" + rqStr);
                return null;
            }
        } else {
            Print.logError("File not found (or is empty): " + reqFile);
            return null;
        }
    }

    /**
    *** Sends the request and returns the results as an XML Document
    **/
    protected Document _sendRequest(String reqXMLStr, int timeoutMS)
        throws IOException // HTMLTools.HttpIOException
    {

        /* request */
        if (RTConfig.isDebugMode()) {
            if (this.isJMX()) {
                Print.logInfo("JMX url: " + this.getJMXServiceURL());
                Print.logInfo("   name: " + this.getJMXObjectName());
            } else {
                Print.logInfo("HTTP url: " + this.getURL());
            }
            Print.logInfo("Request:\n" + reqXMLStr);
        }

        /* service type */
        byte xmlResp[] = this.isJMX()? 
            this._sendRequest_JMX(reqXMLStr) :
            this._sendRequest_HTTP(reqXMLStr, timeoutMS); // HTMLTools.HttpIOException
        if (RTConfig.isDebugMode()) {
            Print.logInfo("Response:\n" + StringTools.toStringValue(xmlResp).trim());
        }

        /* get XML Document */
        Document xmlDoc = XMLTools.getDocument(xmlResp);
        if (xmlDoc == null) {
            Print.logError("Response:\n" + StringTools.toStringValue(xmlResp).trim());
            throw new IOException("Response XML Document error");
        }
        return xmlDoc;
        
    }

    /**
    *** Sends the request and returns the results as an XML Document
    **/
    protected byte[] _sendRequest_JMX(String reqXMLStr)
        throws IOException
    {

        /* connect to remote MBeanServer */
        JMXConnector jmxc = JMXConnectorFactory.connect(this.getJMXServiceURL(), null);

        /* invoke reuest handler */
        byte xmlResp[] = null;
        try {
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

            /* service name */
            String objName = this.getJMXObjectName(); // "org.opengts.extra.war.service:type=ServiceProxy";
            ObjectName name = null;
            try {
                name = new ObjectName(objName);
            } catch (MalformedObjectNameException mone) {
                throw new IOException("Invalid ObjectName: " + objName);
            }

            /* invoke */
            try {
                String xml = (String)mbsc.invoke(name, 
                    METHOD_handleRequest, 
                    new Object[] { reqXMLStr }, 
                    new String[] { "java.lang.String" });
                xmlResp = xml.getBytes();
            } catch (Throwable th) { // MBeanException, InstanceNotFoundException
                throw new IOException("MBean Invocation: " + th.getMessage());
            }

        } finally {

            /* close */
            try { jmxc.close(); } catch (Throwable th) { /* ignore */ }

        }

        return xmlResp;
    }

    /**
    *** Sends the request and returns the results as an XML Document
    **/
    protected byte[] _sendRequest_HTTP(String reqXMLStr, int timeoutMS)
        throws IOException // HTMLTools.HttpIOException
    {
        byte xmlReq[] = reqXMLStr.toString().getBytes();
        return HTMLTools.readPage_POST(
            this.getURL(), 
            HTMLTools.MIME_XML(), xmlReq, 
            timeoutMS, -1/*maxLen*/);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String getTagResponse()
    {
        return SRTAG_GTSResponse;
    }

    public String getTagRequest()
    {
        return SRTAG_GTSRequest;
    }

    public String getTagAuthorization() 
    {
        return SRTAG_Authorization;
    }

    public String getAttrCommand()
    {
        return SRATTR_command;
    }

    public String getAttrResult()
    {
        return SRATTR_result;
    }

    public String getAttrAccount()
    {
        return SRATTR_account;
    }

    public String getAttrUser()
    {
        return SRATTR_user;
    }

    public String getAttrPassword()
    {
        return SRATTR_password;
    }

    public String getAttrAuth()
    {
        return SRATTR_auth;
    }

    // ------------------------------------------------------------------------

}
