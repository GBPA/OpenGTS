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
//  2009/11/01  Martin D. Flynn
//     -Initial release
//  2014/10/07  Martin D. Flynn
//     -Added comments to indicate how to compile with Tomcat-8 [2.5.7]
//  2016/04/22  Martin D. Flynn
//     -"BufferedServletOutputStream" moved to "BufferedServletOutputStream.java"
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.Locale;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;

import javax.servlet.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.opengts.util.StringTools;
import org.opengts.util.Print;

public class BufferedHttpServletResponse
    extends HttpServletResponseWrapper
{

    // ------------------------------------------------------------------------

    private BufferedServletOutputStream byteStream  = null;
    private ServletOutputStream         outStream   = null;
    private PrintWriter                 printWriter = null;

    public BufferedHttpServletResponse(HttpServletResponse response)
    {
        super(response);
    }

    private ServletOutputStream createOutputStream()
        throws IOException 
    {
        if (this.byteStream == null) {
            this.byteStream = new BufferedServletOutputStream();
        }
        return this.byteStream;
    }

    public ServletOutputStream getOutputStream()
        throws IOException 
    {
        if (this.printWriter != null) {
            throw new IllegalStateException("getWriter() has already been called for this response");
        }
        if (this.outStream == null ) {
            this.outStream = this.createOutputStream();
        }
        return this.outStream;
    }

    public PrintWriter getWriter()
        throws IOException 
    {
        if (this.printWriter != null) {
            return this.printWriter;
        }
        if (this.outStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called for this response" );
        }
        this.outStream = this.createOutputStream();
        this.printWriter = new PrintWriter(this.outStream);
        return this.printWriter;
    }

    // ------------------------------------------------------------------------
    
    public void flushBuffer()
        throws IOException
    {
        super.flushBuffer();
    }
    
    public int getBufferSize()
    {
        //return super.getBufferSize();
        return (this.byteStream != null)? this.byteStream.getSize() : 0;
    }
    
    public String getCharacterEncoding()
    {
        return super.getCharacterEncoding();
    }
    
    public Locale getLocale()
    {
        return super.getLocale();
    }

    public boolean isCommitted()
    {
        return super.isCommitted();
    }

    public void reset()
    {
        super.reset();
    }

    public void resetBuffer()
    {
        // -- Ignore resetting buffer.
        //Print.logStackTrace("ResetBuffer ...");
        //super.resetBuffer();
    }

    public void setBufferSize(int size)
    {
        super.setBufferSize(size);
    }

    public void setContentLength(int len)
    {
        Print.logStackTrace("Setting Content Length: " + len);
        super.setContentLength(len);
    }

    public void setContentType(String type)
    {
        // -- Ignore setting content type
        //Print.logStackTrace("Setting Content Type: " + type);
        //super.setContentType(type);
    }

    public void setLocale(Locale loc)
    {
        super.setLocale(loc);
    }
                 
    // ------------------------------------------------------------------------

    public byte[] toByteArray()
    {
        return (this.byteStream != null)? this.byteStream.toByteArray() : null;
    }

    public String toString()
    {
        byte b[] = this.toByteArray();
        return StringTools.toStringValue(b);
    }

    // ------------------------------------------------------------------------

}
