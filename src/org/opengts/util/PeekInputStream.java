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
package org.opengts.util;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.math.*;

/**
*** InputStream wrapper that allows peeking at bytes in the input stream
*** before actually reading them.
**/

public class PeekInputStream
    extends InputStream
{

    // ------------------------------------------------------------------------

    private InputStream          input = null;
    private ByteArrayInputStream bais  = null;

    public PeekInputStream(InputStream inp)
        throws IOException
    {
        if (inp == null) {
            throw new IOException("InputStream is null");
        }
        this.input = inp;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the internal InputStream
    **/
    public InputStream getInputStream() 
    {
        return this.input;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the number of bytes in the peek cache
    **/
    private int getCacheLen()
    {
        return (this.bais != null)? this.bais.available() : 0;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the number of bytes available without blocking
    **/
    public int available()
        throws IOException
    {
        return this.getCacheLen() + this.input.available();
    }

    // ------------------------------------------------------------------------

    /**
    *** Closes this input stream 
    **/
    public void close()
        throws IOException
    {
        if (this.bais != null) {
            this.bais.close();
            this.bais = null;
        }
        this.input.close();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if mark/reset supported by this implementation.
    *** Returns "false"
    **/
    public boolean markSupported()
    {
        if (!this.input.markSupported()) {
            // -- "mark"/"reset" not supported by InputStream
            return false;
        } else
        if (this.getCacheLen() > 0) {
            // -- "mark"/"reset" not supported while cache data present
            return false;
        } else {
            // -- "mark"/"reset" is supported by InputStream
            return true;
        }
    }

    /**
    *** Marks the current position in this input stream.
    *** (not supported in this implementation)
    **/
    public void mark(int readLimit) 
    {
        if (this.getCacheLen() > 0) {
            // -- "mark" not supported while cache data present
        } else {
            // -- delegate to input stream
            this.input.mark(readLimit);
        }
    }

    /**
    *** Repositions this stream to the position at the time the mark method was last called on this input stream.
    *** (not supported in this implementation)
    **/
    public void reset() 
        throws IOException
    {
        if (this.getCacheLen() > 0) {
            // -- "reset" not supported while cache data present
            throw new IOException("'reset' not supported");
        } else {
            // -- delegate to input stream
            this.input.reset();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Skips over and discards n bytes of data from this input stream.
    **/
    public long skip(long n)
        throws IOException
    {
        int baisLen = this.getCacheLen();
        if (baisLen > 0) {
            if (n < baisLen) {
                return this.bais.skip(n);
            } else {
                this.bais.skip(baisLen);
                this.bais = null; // empty
                return this.input.skip(n - baisLen);
            }
        } else {
            return this.input.skip(n);
        }
    }

    // ------------------------------------------------------------------------

    /**
    ***  Reads the next byte of data from the input stream.
    **/
    public int read()
        throws IOException
    {
        if (this.getCacheLen() > 0) {
            return this.bais.read();
        } else {
            return this.input.read();
        }
    }

    /**
    ***  Reads up to len bytes of data from the input stream into an array of bytes.
    **/
    public int read(byte b[], int off, int len)
        throws IOException
    {
        int baisLen = this.getCacheLen();
        if (baisLen > 0) {
            if (len < baisLen) {
                // -- BAIS contain all that was requested
                return this.bais.read(b, off, len);
            } else {
                // -- read everything that BAIS contains
                this.bais.read(b, off, baisLen);
                this.bais = null;
                off += baisLen;
                len -= baisLen;
            }
            // -- read remaining from InputStream
            int inpLen = this.input.read(b, off, len);
            if (inpLen < 0) {
                return baisLen;
            } else {
                return baisLen + inpLen;
            }
        } else {
            return this.input.read(b, off, len);
        }
    }

    /**
    ***  Reads bytes from the input stream and stores them into the buffer array
    **/
    public int read(byte b[])
        throws IOException
    {
        if (this.getCacheLen() > 0) {
            return this.read(b, 0, b.length);
        } else {
            return this.input.read(b);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Peek at the leading bytes in the input stream
    **/
    public int peek(byte b[], boolean blockOK)
        throws IOException
    {
        if ((b != null) && (b.length > 0)) {
            int maxLen = blockOK? b.length : Math.min(b.length,this.available());
            int n = this.read(b, 0, maxLen); 
            this.push(b, 0, n);
            return n;
        } else {
            return 0;
        }
    }

    /**
    *** Peek at the leading bytes and return true if the specified pattern is matched.
    *** Pattern matching will stop at the first byte that does not match the pattern
    **/
    public boolean peekPattern(byte pat[], boolean blockOK)
        throws IOException
    {
        // -- no pattern?
        if ((pat == null) || (pat.length <= 0)) {
            // -- nothin to match
            return false;
        }
        // -- not ok to block? (then we must have enough non-blocking bytes)
        if (!blockOK && (pat.length > this.available())) {
            // -- we will not find a match within the available bytes
            return false;
        }
        // -- check for match
        byte pushBack[] = new byte[pat.length];
        int  pb = 0;
        try {
            for (int i = 0; i < pat.length; i++) {
                int b = this.read();
                if (b >= 0) {
                    pushBack[pb++] = (byte)b;
                    if ((byte)b == pat[i]) {
                        continue; // matched so far
                    }
                }
                break;
            }
        } catch (IOException ioe) {
            // -- timeout?
            throw ioe;
        } finally {
            // -- push-back bytes, even if we get an error
            this.push(pushBack, 0, pb);
        }
        return (pb == pat.length)? true : false;
    }

    /**
    *** Push the specified bytes to the beginning of the input stream
    **/
    private void push(byte b[], int off, int len)
    {
        if (len > 0) {
            int baisLen = this.getCacheLen();
            if (baisLen > 0) {
                byte ba[] = new byte[len + baisLen];
                System.arraycopy(b,off,ba,0,len);
                this.bais.read(ba,len,baisLen); // IOException?
                this.bais = new ByteArrayInputStream(ba);
            } else {
                byte ba[] = new byte[len];
                System.arraycopy(b,off,ba,0,len);
                this.bais = new ByteArrayInputStream(ba);
            }
        }
    }

    // ------------------------------------------------------------------------

}
