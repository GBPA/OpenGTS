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
//  2016/04/22  Martin D. Flynn
//     -Moved from "BufferedHttpServletResponse.java"
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.io.ByteArrayOutputStream;

import javax.servlet.*;
import javax.servlet.ServletOutputStream;

/**
*** BufferedServletOutputStream class<br>
*** Override ServletOutputStream<br>
*** This class may need to be modified when using Tomcat-8 (Servlet Specification 3.1).
**/
public class BufferedServletOutputStream // <-- mod may be required for Tomcat-7/8 (see below for more info)
    extends ServletOutputStream
{

    // ------------------------------------------------------------------------

    private ByteArrayOutputStream baos = null;

    /**
    *** Constructor
    **/
    public BufferedServletOutputStream() 
    {
        this.baos = new ByteArrayOutputStream();
    }

    // ------------------------------------------------------------------------

    /**
    *** Write byte
    **/
    public void write(int b) 
    {
        this.baos.write(b);
    }

    /**
    *** Gets the current size of the buffer
    **/
    public int getSize() 
    {
        return this.baos.size();
    }

    /**
    *** Return the buffer as a byte array
    **/
    public byte[] toByteArray() 
    {
        return this.baos.toByteArray();
    }

    // ------------------------------------------------------------------------

    /**
    *** Servlet specification 3.1 support in Tomcat-8
    ***  - Required for Tomcat-8, not used in Tomcat-7
    **/
    public boolean isReady() 
    { 
        return true; 
    }

    // ------------------------------------------------------------------------

    /**
    *** Servlet specification 3.1 support in Tomcat-8
    *** - When using Tomcat 7:
    ***    Comment the line below ("javax.servlet.WriteListener" doesn't exist)
    *** - When using Tomcat 8:
    ***    Unomment the line below.
    ***    Remove source module "src/org/opengts/war/tools/WriteListener.java" (if present)
    **/
    public void setWriteListener(/*javax.servlet.*/WriteListener wl) {/*NO-OP*/}

    // ------------------------------------------------------------------------

}
