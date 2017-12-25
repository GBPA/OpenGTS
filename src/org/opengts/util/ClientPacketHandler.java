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
// Description:
//  Socket client packet handler
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.net.*;
import javax.net.*;

//import javax.net.ssl.*;

/**
*** Inteface for packet handling clients [CHECK]
**/

public interface ClientPacketHandler
{

    // ------------------------------------------------------------------------

    /**
    *** Called when new client session initiated
    *** @param inetAddr The host IP address
    *** @param isTCP True if the connection is TCP
    *** @param isText True if the connection is text
    **/
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText);

    /**
    *** Called after client session terminated
    *** @param err 
    *** @param readCount
    *** @param writeCount 
    **/
    public void sessionTerminated(Throwable err, long readCount, long writeCount);

    // ------------------------------------------------------------------------

    /**
    *** Gets the session ID for this instance
    *** @return The session ID for this instance
    **/
    public String getSessionID();

    /**
    *** Returns true if the specified session ID matches the current session ID
    *** @param sessionID  The session ID to test (specifying null should always return false)
    *** @return True if the session IDs match, false otherwise
    **/
    public boolean equalsSessionID(String sessionID);

    // ------------------------------------------------------------------------

    /**
    *** Sets the session info handler
    *** @param sessionInfo An implementation of the ServerSocketThread.SessionInfo interface
    **/
    public void setSessionInfo(ServerSocketThread.SessionInfo sessionInfo);
    
    /**
    *** Gets a reference to the ClientPacketHandler's session info implementation
    *** @return Reference to the session info object
    **/
    public ServerSocketThread.SessionInfo getSessionInfo();

    // ------------------------------------------------------------------------

    /**
    *** Return initial response to the open session
    *** @return The initial response to be sent when the session opens
    **/
    public byte[] getInitialPacket() throws Exception;

    /**
    *** Return final response to the session before it closes
    *** @return the final response to be sent before the session closes
    **/
    public byte[] getFinalPacket(boolean hasError) throws Exception;

    // ------------------------------------------------------------------------

    /**
    *** Gets the line terminator characters.
    *** May be used to override value provided by ServerSocketThread.
    *** Returns null if ServerSocketThread value should be used.
    *** @return  The line terminator characters
    **/
    public int[] getLineTerminatorChar();

    /**
    *** Gets the characters to ignore
    *** May be used to override value provided by ServerSocketThread.
    *** Returns null if ServerSocketThread value should be used.
    *** @return  The ignore characters
    **/
    public int[] getIgnoreChar();

    /**
    *** Gets the idle timeout in milliseconds
    *** May be used to override value provided by ServerSocketThread.
    *** Returns -1 if ServerSocketThread value should be used.
    *** @return  The idle timeout in milliseconds
    **/
    public long getIdleTimeoutMillis();

    /**
    *** Gets the packet timeout in milliseconds
    *** May be used to override value provided by ServerSocketThread.
    *** Returns -1 if ServerSocketThread value should be used.
    *** @return  The packet timeout in milliseconds
    **/
    public long getPacketTimeoutMillis();

    /**
    *** Gets the session timeout in milliseconds
    *** May be used to override value provided by ServerSocketThread.
    *** Returns -1 if ServerSocketThread value should be used.
    **/
    public long getSessionTimeoutMillis();

    /**
    *** Gets the minimum packet length
    *** May be used to override value provided by ServerSocketThread.
    *** Returns -1 if ServerSocketThread value should be used.
    *** @return  The minimum packet length
    **/
    public int getMinimumPacketLength();
    
    /**
    *** Gets the maximum packet length
    *** May be used to override value provided by ServerSocketThread.
    *** Returns -1 if ServerSocketThread value should be used.
    *** @return  The maximum packet length
    **/
    public int getMaximumPacketLength();

    // ------------------------------------------------------------------------

    /**
    *** Sets the prompt enabled state
    *** @param enable  True to enable prompt, false to disable
    **/
    public void setPromptEnabled(boolean enable);

    /**
    *** Gets the prompt enabled state
    *** @return  True to enable prompt, false to disable
    **/
    public boolean getPromptEnabled();

    // ------------------------------------------------------------------------

    /**
    *** Callback: timeout interrupt
    *** Called periodically during an idle read.  The periodic timeout is based on the value
    *** specified on the call to "<code>ServerSocketThread.setMinimuTimeoutIntervalMS</code>"
    **/
    public void idleTimeoutInterrupt();

    // ------------------------------------------------------------------------

    /**
    *** Return actual packet length based on this partial packet
    **/
    public int getActualPacketLength(byte packet[], int packetLen); // non-text

    /**
    *** Process packet and return response
    *** @param cmd The packet
    *** @return The response
    **/
    public byte[] getHandlePacket(byte cmd[]) throws Exception;

    // --------------------------------

    /**
    *** Returns true if the returned response from "getHandlePacket" should be sent back to client
    **/
    public boolean getSendResponse();

    /**
    *** Sets the send-response state
    **/
    public void setSendResponse(boolean sendResp);

    // ------------------------------------------------------------------------

    /**
    *** Write bytes to TCP output stream asynchronously<br>
    *** Instended to be called from a separate thread wishing to write a command
    *** asynchronously to the active device session.
    *** @param data  The data bytes to write
    *** @return True if bytes were written, false otherwise
    **/
    public boolean tcpWrite(byte data[]);

    // ------------------------------------------------------------------------

    /**
    *** Return the port for UDP Datagram responses
    *** @return The port for UDP Datafram responses
    **/
    public int getResponsePort(); // may return '0' to default to "<ServerSocketThread>.getRemotePort()"

    // ------------------------------------------------------------------------

    /**
    *** forces the client TCP session to close
    **/
    public void forceCloseTCPSession();

    /**
    *** Sets the session termination state (a hint to the session thread that the
    *** session should be terminated)
    **/
    public void setTerminateSession();

    /**
    *** Indicates if the session should terminate
    *** @return True if the session should terminate
    **/
    public boolean getTerminateSession();
    public boolean terminateSession(); // OBSOLETE

}
