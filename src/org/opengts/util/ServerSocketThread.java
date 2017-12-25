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
//  Template for general server socket support
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/05/01  David Cowan
//     -Added support for gracefully shutting down the server
//  2007/07/13  Martin D. Flynn
//     -End Of Stream errors on UDP connections simply returns the bytes we're 
//      just read (previously it threw an exception, and ignored the received data).
//  2008/07/08  Martin D. Flynn
//     -Removed (commented) default socket timeout.
//  2008/08/07  Martin D. Flynn
//     -If client handler returns 'PACKET_LEN_ASCII_LINE_TERMINATOR', check to see if the
//      last read byte is already a line terminator.
//  2009/01/28  Martin D. Flynn
//     -Added UDP support to send 'getFinalPacket' to client.
//     -Fixed UDP 'getLocalPort'
//  2009/02/20  Martin D. Flynn
//     -Moved check for 'getMinimumPacketLength' into 'ServerSessionThread'.
//  2009/04/02  Martin D. Flynn
//     -Changed 'ServerSessionThread' to allow the 'ClientPacketHandler' to override
//      the minimum/maximum packet lengths.
//     -Added support for incremental packet lengths
//     -Added method 'isValidPort'
//  2009/05/01  Martin D. Flynn
//     -Returned UDP ACKs now use the same DatagramSocket which the server 'listens' on.
//  2009/05/24  Martin D. Flynn
//     -Added "getRemotePort()" to SessionInfo interface.
//  2009/07/01  Martin D. Flynn
//     -Client may now immediately terminate a session after 'sessionStarted'.
//  2009/08/07  Martin D. Flynn
//     -Added ability to set the local bound interface
//  2009/09/23  Martin D. Flynn
//     -Fixed: now counts bytes ('writeByteCount') when writing via UDP
//  2010/09/09  Martin D. Flynn
//     -Fixed EOS during TCP session when EOS should be end of packet.
//  2011/05/13  Martin D. Flynn
//     -Added option to include line terminator character in returned packets.
//      [see "includePacketLineTerminator()"]
//     -Increased "PACKET_LEN_INCREMENTAL_" mask to 20 bits
//  2011/07/01  Martin D. Flynn
//     -Changed to allow timeouts for "PACKET_LEN_END_OF_STREAM" termination.
//     -Fixed "PACKET_LEN_END_OF_STREAM" to attempt reading only whats left in the stream.
//  2011/08/21  Martin D. Flynn
//     -Renamed "PACKET_LEN_ASCII_LINE_TERMINATOR" to "PACKET_LEN_LINE_TERMINATOR"
//     -Check for ASCII packets before removing non-printable chars.
//     -Added support for incremental line-termination char packet length.
//  2012/10/16  Martin D. Flynn
//     -Added support for calling "<ClientPacketHandler>.idleTimeoutInterrupt()".
//  2012/12/24  Martin D. Flynn
//     -Added "setLoggingEnabled"/"getLoggingEnabled" methods.
//     -Modified "createServerSocket" to also catch "IllegalArgumentException"
//  2013/04/08  Martin D. Flynn
//     -Many changes to better accomodate handling InputStream parsing.
//     -Changed "tcpWriteToSessionID" to write to all matching sessions
//  2013/05/28  Martin D. Flynn
//     -Changed non-critical "Print.logStackTrace" to "Print.logWarn".
//     -Added "logActiveSessions"
//  2013/11/11  Martin D. Flynn
//     -Added ability for InputStream ClientSockets to mimic isDuplex()
//     -Added additional end-of-session logging
//  2014/09/16  Martin D. Flynn
//     -Added support for limiting the number of client handler threads
//      (see "maxClientPoolSize")
//  2015/05/03  Martin D. Flynn
//     -"maxClientPoolSize" now also applied to UDP listener connections.
//     -Added handling of "OutOfMemoryError: unable to create new native thread"
//  2016/04/06  Martin D. Flynn
//     -Overhauled InputStream session handling
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.net.*;
import java.awt.event.*;
import javax.net.*;

import javax.net.ssl.SSLServerSocketFactory;

public class ServerSocketThread
    extends Thread
{

    // ------------------------------------------------------------------------
    // References:
    //   http://tvilda.stilius.net/java/java_ssl.php
    //   http://www.jguru.com/faq/view.jsp?EID=993651

    // ------------------------------------------------------------------------
    // SSL:
    //    keytool -genkey -keystore <mySrvKeystore> -keyalg RSA
    // Required Properties:
    //   -Djavax.net.ssl.keyStore=<mySrvKeystore>
    //   -Djavax.net.ssl.keyStorePassword=<123456>
    // For debug, also add:
    //   -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol 
    //   -Djavax.net.debug=ssl
    // ------------------------------------------------------------------------

    /* read additional byte length designated by mask and again call "getActualPacketLength" */
    public static final int         PACKET_LEN_INCREMENTAL_             = 0x01000000;
    public static final int         PACKET_LEN_INCREMENTAL_MASK         = 0x00FFFFFF;

    /* read to packet terminator */
    public static final int         PACKET_LEN_LINE_TERMINATOR          = -1;  // line-term char
    public static final int         PACKET_LEN_END_OF_STREAM            = -2;  // end of stream
  //public static final int         PACKET_LEN_MATCH_PATTERN            = -3;  // pattern match (NOT SUPPORTED!)
  //public static final int         PACKET_LEN_HTTP_HEADER              = -5;  // read until blank line (\r\n)  (NOT SUPPORTED!)

    /* read additional bytes up until line-terminator, and again call "getActualPacketLength" */
    public static final int         PACKET_LEN_INCREMENT_EOL            = PACKET_LEN_INCREMENTAL_ | PACKET_LEN_INCREMENTAL_MASK;

    // ------------------------------------------------------------------------

    public static final boolean     ACK_FROM_LISTEN_PORT                = true;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* minimum timeout interval */
    public static       int         MinimumTimeoutIntervalMS            = 0; // milliseconds

    /**
    *** Sets the minimum timeout interval used for calling the ClientPacketHandler method
    *** "<code>idleTimeoutInterrupt()</code>".
    *** @param minTMS  The minimum timeout interval in milliseconds.
    **/
    public static void setMinimuTimeoutIntervalMS(int minTMS)
    {
        MinimumTimeoutIntervalMS = (minTMS > 5000)? minTMS : 5000;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* per "ServerSocket.java", the default 'backlog' is "50" */
    private static      int         ListenBacklog                       = 50;
    private static      InetAddress LocalBindAddress                    = null;

    /**
    *** Sets the listen backlog for all created ServerSocket's
    *** @param backlog  The listen backlog
    **/
    public static void setListenBacklog(int backlog)
    {
        ListenBacklog = backlog;
    }

    /**
    *** Sets the local bind address for all created ServerSocket's
    *** @param bindAddr  The local bind address
    **/
    public static void setBindAddress(InetAddress bindAddr)
    {
        if ((bindAddr != null) && !ServerSocketThread.isLocalInterfaceAddress(bindAddr)) {
            Print.logWarn("BindAddress not found in NetworkInterface: " + bindAddr);
        }
        LocalBindAddress = bindAddr;
    }

    /**
    *** Returns true if a local bind address has been defined, otherwise false
    *** @return True if a local bind address has been defined, otherwise false
    **/
    public static boolean hasBindAddress()
    {
        return (LocalBindAddress != null);
    }

    /**
    *** Gets the local bind address for all created ServerSocket's, or null if no specific 
    *** bind address has been set
    *** @return The local bind address
    **/
    public static InetAddress getDefaultBindAddress()
    {
        return LocalBindAddress;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an array of all local network interface addresses (excluding loopback)
    *** @return An array of all local network interface addresses
    **/
    public static InetAddress[] getNetworkInterfaceAddresses()
        throws SocketException
    {
        java.util.List<InetAddress> ialist = new Vector<InetAddress>();
        for (Enumeration<NetworkInterface> ne = NetworkInterface.getNetworkInterfaces(); ne.hasMoreElements();) {
            NetworkInterface ni = ne.nextElement();
            if (!ni.isLoopback()) {
                //System.out.println("NetworkInterface: " + ni.getName());
                for (Enumeration<InetAddress> ie = ni.getInetAddresses(); ie.hasMoreElements();) {
                    InetAddress ia = ie.nextElement();
                    //System.out.println("  InetAddress : " + ia.getHostAddress());
                    ialist.add(ia);
                }
            }
        }
        return ialist.toArray(new InetAddress[ialist.size()]);
    }
    
    /**
    *** Returns true if the specified InetAddress is a local bound interface (including loopback)
    *** @return True if the specified InetAddress is a local bound interface
    **/
    public static boolean isLocalInterfaceAddress(InetAddress addr)
    {
        if (addr == null) {
            return false;
        } else
        if (addr.isLoopbackAddress()) {
            return true;
        } else {
            try {
                Set<InetAddress> ias = ListTools.toSet(ServerSocketThread.getNetworkInterfaceAddresses());
                return ias.contains(addr);
            } catch (Throwable th) {
                Print.logException("Getting NetworkInterface addresses", th);
                return false;
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates a DatagramSocket bound to the default local interface
    *** @return The created DatagramSocket
    **/
    public static DatagramSocket createDatagramSocket(InetAddress bindAddr, int port)
        throws SocketException
    {
        InetAddress bind = (bindAddr != null)? bindAddr : ServerSocketThread.getDefaultBindAddress();
        DatagramSocket dgSock;
        if (bind != null) {
            // -- bind to specific interface
            dgSock = new DatagramSocket(new InetSocketAddress(bind,port));
        } else {
            // -- bind to all interfaces
            dgSock = new DatagramSocket(port);
        }
        return dgSock;
    }
    
    /**
    *** Creates a DatagramSocket bound to the default local interface
    *** @return The created DatagramSocket
    **/
    //public static DatagramSocket createDatagramSocket(int port)
    //    throws SocketException
    //{
    //    return ServerSocketThread.createDatagramSocket((InetAddress)null, port);
    //}

    // ------------------------------------------------------------------------

    /**
    *** Creates a ServerSocket bound to the default local interface
    *** @return The created ServerSocket
    **/
    public static ServerSocket createServerSocket(InetAddress bindAddr, int port)
        throws IOException
    {
        InetAddress bind = (bindAddr != null)? bindAddr : ServerSocketThread.getDefaultBindAddress();
        try {
            return new ServerSocket(port, ListenBacklog, bind);
        } catch (IllegalArgumentException iae) {
            // -- IE. "Port value out of range"
            throw new IOException(iae);
        } catch (java.net.BindException be) {
            // -- IE. "Can't assign requested address"
            Print.logError("Bind Address: " + bind);
            throw be;
        } catch (IOException ioe) {
            // -- re-throw IOException
            throw ioe;
        } catch (Throwable th) {
            // -- catch all
            throw new IOException(th);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified port is valid
    *** @param port  The port to test
    *** @return True if the specified port is valid
    **/
    public static boolean isValidPort(int port)
    {
        return ((port > 0) && (port <= 65535));
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Vector<ServerSocketThread> SSTList = new Vector<ServerSocketThread>();

    private static void _AddSST(ServerSocketThread sst)
    {
        synchronized (SSTList) {
            SSTList.add(sst);
        }
    }

    private static void _RemoveSST(ServerSocketThread sst)
    {
        synchronized (SSTList) {
            SSTList.remove(sst);
        }
    }

    public static boolean shutdownAll(long tmoMS)
    {
        final long timeoutMS = (tmoMS >= 1000L)? tmoMS : 1000L;

        /* get ServerSocketThread count */
        int count = 0;
        ServerSocketThread firstSST = null;
        synchronized (SSTList) {
            count = SSTList.size();
            if (count > 0) {
                firstSST = SSTList.get(0);
            }
        }

        /* nothing to stop? */
        if (count == 0) {
            return true; // done
        }

        /* only a single ServerSocketThread? */
        if ((count == 1) && (firstSST != null)) {
            boolean ok = firstSST.shutdown(timeoutMS); // blocks until stopped or timeout
            //Print.logInfo("0) SST shutdown: " + ok);  // static
            return ok;
        }

        /* we have several ServerSocketThreads */
        final AccumulatorInteger timeoutCount = new AccumulatorInteger(0);
        synchronized (SSTList) {
            int ndx = 0;
            for (final ServerSocketThread sst : SSTList) {
                final int ndxVal = ndx++;
                try {
                    (new Thread(new Runnable() {
                        public void run() {
                            boolean ok = sst.shutdown(timeoutMS);
                            //Print.logDebug(ndxVal + ") SST shutdown: " + ok); // static
                            if (!ok) {
                                timeoutCount.increment();
                            }
                        }
                    })).start(); // java.lang.OutOfMemoryError: unable to create new native thread
                } catch (OutOfMemoryError oome) {
                    // -- "java.lang.OutOfMemoryError: unable to create new native thread"
                    Print.logError("Shutdown failed (unable to create new thread), trying in-line ...");
                    // -- try shutdown in-line 
                    // -  Note: "SSTList" is locked, so care must be taken to prevent a deadlock
                    boolean ok = sst.shutdown(timeoutMS);
                    //Print.logDebug(ndxVal + ") SST shutdown: " + ok); // static
                    if (!ok) {
                        timeoutCount.increment();
                    }
                }
            }
        }

        /* wait for ServerSocketThreads threads to stop */
        long waitMS = timeoutMS + 1000L;
        long startMS = DateTime.getCurrentTimeMillis();
        boolean didTimeout = false;
        int remainingThreads = 0;
        do {
            // -- get thread count
            synchronized (SSTList) {
                remainingThreads = SSTList.size();
            }
            // -- all client threads stopped?
            if (remainingThreads <= 0) {
                // -- everything has stopped
                break;
            }
            //Print.logInfo("Remaining SST threads: " + remainingThreads); // static
            // -- timeout?
            long nowMS = DateTime.getCurrentTimeMillis();
            long deltaMS = nowMS - startMS;
            if (deltaMS >= waitMS) {
                didTimeout = true;
                break;
            }
            // -- short sleep
            try { Thread.sleep(500L); } catch (Throwable th) { /* ignore */ }
        } while (remainingThreads > 0);

        /* successful if this did not timeout */
        return !didTimeout && (timeoutCount.get() <= 0);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int                                  listenPort               = 0;
    private int                                  clientPort               = -1;  // use for UDP response connections only
    
    private InetAddress                          bindAddress              = null;

    private DatagramSocket                       datagramSocket           = null; // UDP
    private ServerSocket                         serverSocket             = null; // TCP
    
    private java.util.List<ServerSessionThread>  clientThreadPool         = null;
    private int                                  maxClientPoolSize        = 0;
    private java.util.List<ClientPacketHandler>  activeSessionList        = null;

    private ClientPacketHandler                  clientControlChannel     = null;
    private byte                                 controlChannelHeader[]   = null;

    private ClientPacketHandler                  clientPacketHandler      = null;
    private Class<? extends ClientPacketHandler> clientPacketHandlerClass = null;

    private long                                 sessionTimeoutMS         = -1L;
    private long                                 idleTimeoutMS            = -1L;
    private long                                 packetTimeoutMS          = -1L;
    
    private int                                  lingerTimeoutSec         = 4;    // SO_LINGER timeout is in *Seconds*

    private int                                  maxReadLength            = -1;   // safety net only
    private int                                  minReadLength            = -1;

    private boolean                              terminateOnTimeout       = true;

    private boolean                              isTextPackets            = true;
    private int                                  lineTerminatorChar[]     = new int[] { '\n' };
    private int                                  backspaceChar[]          = new int[] { '\b' };
    private int                                  ignoreChar[]             = new int[] { '\r' };
    private boolean                              includePacketLineTerm    = false;

    private byte                                 packetTermPattern[]      = null;

    private boolean                              promptEnabled            = true;
    private byte                                 prompt[]                 = null;
    private int                                  promptIndex              = -1;
    private boolean                              autoPrompt               = false;

    private java.util.List<ActionListener>       actionListeners          = null;

    private boolean                              LogEnable                = true;

    private ServerSocketThread                   inputStreamHandler       = null;

    // ------------------------------------------------------------------------

    /**
    *** Private Constructor
    **/
    private ServerSocketThread()
    {
        super("Server_" + (ServeSocketThread_counter++));
        //Print.logInfo("*** ServerSocketThread: " + this.getName() + " ...");
        // --
        this.bindAddress       = ServerSocketThread.getDefaultBindAddress();
        this.clientThreadPool  = new Vector<ServerSessionThread>();
        this.activeSessionList = new Vector<ClientPacketHandler>();
        this.actionListeners   = new Vector<ActionListener>();
        // -- apply maximum allowed client threads
        this.setMaximumClientThreadPoolSize(RTConfig.getInt(RTKey.ServerSocketThread_maximumClientThreadPoolSize,0));
        // -- save a pointer to this instance
        _AddSST(this);
    }

    /**
    *** UDP Constructor
    **/
    public ServerSocketThread(DatagramSocket ds) 
    {
        this();
        // --
        this.datagramSocket = ds;
        this.bindAddress    = (ds != null)? ds.getLocalAddress() : ServerSocketThread.getDefaultBindAddress();
        this.listenPort     = (ds != null)? ds.getLocalPort() : -1;
        // --
        try {
            int recvBuffSize = RTConfig.getInt(RTKey.ServerSocketThread_udpReceiveBufferSize,0); // max 7456540 [0x71C71C] (Mac)
            if ((this.datagramSocket != null) && (recvBuffSize > 0)) {
                //Print.logInfo("DatagramSocket SO_RCVBUF size (before): "+this.datagramSocket.getReceiveBufferSize()+" ["+recvBuffSize+"]");
                this.datagramSocket.setReceiveBufferSize(recvBuffSize);
                Print.logDebug("DatagramSocket SO_RCVBUF size: " + this.datagramSocket.getReceiveBufferSize());
                /*
                DatagramChannel dgChan = this.datagramSocket.getChannel();
                if (dgChan != null) {
                    // -- TODO: allow compiling on Java-6
                    dgChan.setOption(SocketOption.SO_RCVBUF, recvBuffSize); // Java 7+
                }
                */
            }
        } catch (Throwable th) { // SocketException, UnsupportedOperationException, IllegalArgumentException, etc.
            // -- ignore
            Print.logWarn("Unable to set DatagramSocket 'setReceiveBufferSize': " + th);
            //Print.logException("Unable to set DatagramSocket 'setReceiveBufferSize': ",th);
        }
    }

    /**
    *** TCP Constructor
    *** @param ss  The ServerSocket containing the 'listen' port information
    **/
    public ServerSocketThread(ServerSocket ss) 
    {
        this();
        // --
        this.serverSocket = ss;
        this.bindAddress  = (ss != null)? ss.getInetAddress() : ServerSocketThread.getDefaultBindAddress();
        this.listenPort   = (ss != null)? ss.getLocalPort() : -1;
    }

    /**
    *** TCP Constructor
    *** @param port  The port on which to listen for incoming connections
    **/
    public ServerSocketThread(InetAddress bindAddr, int port)
        throws IOException 
    {
        this();
        // --
        this.bindAddress  = (bindAddr != null)? bindAddr : ServerSocketThread.getDefaultBindAddress();
        this.serverSocket = ServerSocketThread.createServerSocket(this.bindAddress, port);
        this.listenPort   = port;
    }

    /**
    *** TCP Constructor
    *** @param port  The port on which to listen for incoming connections
    **/
    public ServerSocketThread(int port)
        throws IOException 
    {
        this((InetAddress)null, port);
    }

    /**
    *** TCP Constructor
    *** @param port  The port on which to listen for incoming connections
    *** @param useSSL  True to enable an SSL
    **/
    public ServerSocketThread(InetAddress bindAddr, int port, boolean useSSL)
        throws IOException 
    {
        this();
        // --
        this.bindAddress  = (bindAddr != null)? bindAddr : ServerSocketThread.getDefaultBindAddress();
        this.serverSocket = useSSL?
            SSLServerSocketFactory.getDefault().createServerSocket(port, ListenBacklog, this.bindAddress) :
            ServerSocketFactory   .getDefault().createServerSocket(port, ListenBacklog, this.bindAddress);
        this.listenPort = port;
    }

    /**
    *** TCP Constructor
    *** @param port  The port on which to listen for incoming connections
    *** @param useSSL  True to enable an SSL
    **/
    public ServerSocketThread(int port, boolean useSSL)
        throws IOException 
    {
        this((InetAddress)null, port, useSSL);
    }

    // ------------------------------------------------------------------------

    /**
    *** Starts main ServerSocketThread port listener thread
    *** @throws java.lang.OutOfMemoryError If unable to create new native thread.
    *** @throws IllegalThreadStateException If thread has already been started.
    **/
    public void start()
    {
        try {
            super.start(); // java.lang.OutOfMemoryError: unable to create new native thread
        } catch (OutOfMemoryError oome) {
            // -- "java.lang.OutOfMemoryError: unable to create new native thread"
            // -  (unlikely here, since we are just starting up)
            Print.logException("Unable to start thread: ServerSocketThread",oome);
            throw oome;
        } catch (IllegalThreadStateException itse) {
            // -- unlikely
            Print.logException("Thread already started: ServerSocketThread",itse);
            //throw itse; // ignore
        }
    }

    /** 
    *** Starts this thread handler
    *** @param name the name to assign to this thread
    **/
    public void start(String name)
    {
        if (!StringTools.isBlank(name)) {
            this.setName(name);
        }
        this.start(); // java.lang.OutOfMemoryError: unable to create new native thread
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the logging enabled flag.  (Print.logInfo and Print.logDebug messages
    *** will not be performed).
    *** @param enable  True to enable, false to deisable  (default is enabled)
    **/
    public void setLoggingEnabled(boolean enable)
    {
        LogEnable = enable;
    }

    /**
    *** Gets the logging enabled flag.  
    *** @return True if enabled, false otherwise
    **/
    public boolean getLoggingEnabled()
    {
        return LogEnable;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum client thread pool size
    *** @param maxSize  The maximum number of allowed client handler threads.
    ***                 <= 0 for unlimited.
    **/
    public void setMaximumClientThreadPoolSize(int maxSize)
    {
        this.maxClientPoolSize = (maxSize > 0)? maxSize : 0;
    }

    /**
    *** Gets the maximum client thread pool size
    *** @return The maximum number of allowed client handler threads.
    ***         0 if unlimited.
    **/
    public int getMaximumClientThreadPoolSize()
    {
        return this.maxClientPoolSize;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the bound UDP DatagramSocket for this server handler.  Will 
    *** return null if this server handler does not handle UDP connections.
    *** @return The DatagramSocket handle
    **/
    public DatagramSocket getDatagramSocket()
    {
        return this.datagramSocket;
    }

    /**
    *** Gets the bound TCP ServerSocket for this server handler.  Will 
    *** return null if this server handler does not handle TCP connections.
    *** @return The DatagramSocket handle
    **/
    public ServerSocket getServerSocket()
    {
        return this.serverSocket;
    }

    /**
    *** Gets the local port to which this socket is bound
    *** @return the local port to which this socket is bound
    **/
    public int getLocalPort()
    {
        return this.listenPort;
    }
    
    /**
    *** Gets the local bind address
    **/
    public InetAddress getBindAddress()
    {
        return this.bindAddress;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the ServerSocketThread that is assigned to handle InputStream parsing
    **/
    public void setInputStreamHandler(ServerSocketThread inpStrmHandler)
    {
        this.inputStreamHandler = inpStrmHandler;
    }

    /**
    *** Gets the ServerSocketThread that is assigned to handle InputStream parsing
    **/
    public ServerSocketThread getInputStreamHandler()
    {
        return this.inputStreamHandler;
    }
    
    /**
    *** Returns true if an InputStream handler has been assigned
    **/
    public boolean hasInputStreamHandler()
    {
        return (this.inputStreamHandler != null)? true : false;
    }

    /**
    *** Run a test session from the specified input stream
    *** @param parentSST The parent ServerSocketThread instance
    *** @param dataInput The test input stream
    *** @param isDuplex  True to mimic "isDuplex()" (TCP) in the AbstractClientPacketHandler
    **/
    public void runInputStreamSessionInline(
        ServerSessionThread parentSST,
        InputStream dataInput, boolean isDuplex)
    {

        /* delegate to InputStream handler? */
        if ((parentSST != null) && this.hasInputStreamHandler()) {
            // -- delegate to our InputStream handler
            ServerSocketThread sockSST = this.getInputStreamHandler();
            sockSST.runInputStreamSessionInline(parentSST, dataInput, isDuplex);
            return;
        }

        /* get current ServerSessionThread context */
        ServerSessionThread currSST = null;
        Thread currThread = Thread.currentThread();
        if (parentSST != null) {
            currSST = parentSST;
        } else
        if (currThread instanceof ServerSessionThread) {
            currSST = (ServerSessionThread)currThread;
        } else {
            // -- Note: If we are running within a "testSession" the current thread  
            // -  will not be s ServerSessionThread, and this may fail.
        }

        /* get InputStream handler ServerSocketThread */
        ServerSessionThread inpStreamSST = null;
        if (currSST != null) {
            // -- check current thread for cached InputStream handler ServerSessionThread
            //Print.logInfo("Current thread is ServerSessionThread: " + currThread.getName());
            inpStreamSST = currSST.getInputStreamSST(); // cached ServerSessionThread for current thread
            if (inpStreamSST == null) {
                inpStreamSST = new ServerSessionThread(null,false);
                inpStreamSST.setParentSessionInfo(currSST);
                currSST.setInputStreamSST(inpStreamSST);
            } else {
                // -- local copy of a ServerSessionThread
                Print.logWarn("Creating local instance of InputStream ServerSessionThread (1) ...");
                inpStreamSST = new ServerSessionThread(null,false);
              //inpStreamSST.setParentSessionInfo(currSST);
            }
        } else {
            // -- local copy of a ServerSessionThread
            Print.logWarn("Creating local instance of InputStream ServerSessionThread (2) ...");
            inpStreamSST = new ServerSessionThread(null,false);
          //inpStreamSST.setParentSessionInfo(currSST);
        }

        /* ClientSocket with specific InputStream */
        ClientSocket clientSocket = new ClientSocket(dataInput, isDuplex, !isDuplex); // InputStream

        /* run inline */
        try {
            inpStreamSST.setClientIfAvailable(clientSocket);
            inpStreamSST.handleClientSession(clientSocket);
            inpStreamSST.close();
        } catch (IOException ior) {
            Print.logError("Error handling InputStream session");
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Run a test session from the specified input data array
    *** @param data  The test input data array
    **/
    public void testSession(byte data[], boolean isDuplex)
    {
        InputStream bais = (data != null)? new ByteArrayInputStream(data) : null;
        this.testSession(bais, isDuplex);
    }

    /**
    *** Run a test session from the specified input stream
    *** @param dataInput  The test input stream
    **/
    public void testSession(InputStream dataInput, boolean isDuplex) 
    {

        /* nothing to test? */
        if (dataInput == null) {
            Print.logError("InputStream is null");
            return;
        }

        /* run client test session */
        ServerSessionThread parentSST = null; // no parent here
        this.runInputStreamSessionInline(parentSST, dataInput, isDuplex);

    }

    // ------------------------------------------------------------------------

    /**
    *** Listens for incoming connections and dispatches them to a handler thread
    **/
    public void run() 
    {
        while (true) {
            ClientSocket clientSocket = null;

            /* wait for client session */
            try {
                if (this.serverSocket != null) {
                    // -- TCP (accept)
                    clientSocket = new ClientSocket(this.serverSocket.accept()); // (block) TCP
                } else
                if (this.datagramSocket != null) {
                    // -- UDP (receive)
                    byte b[] = new byte[ServerSocketThread.this.getMaximumPacketLength()]; // TODO: control channel minimum size?
                    DatagramPacket dp = new DatagramPacket(b, b.length);
                    this.datagramSocket.receive(dp); // (block)
                    // TODO: figure out how to get the local IP address to which the client sent this packet
                    // -- BSD  : IP_RECVIF, IP_RECVDSTADDR
                    // -- Linux: IP_PKTINFO
                    // - http://man7.org/linux/man-pages/man7/ip.7.html
                    // - http://stackoverflow.com/questions/3940612/c-dgram-socket-get-the-receiver-address
                    clientSocket = new ClientSocket(dp); // UDP
                    if (LogEnable) {
                        try {
                            InetAddress   localAddr  = this.datagramSocket.getLocalAddress(); // fixed value
                            SocketAddress localSock  = this.datagramSocket.getLocalSocketAddress(); // fixed value
                            InetAddress   remoteAddr = dp.getAddress();
                            SocketAddress remoteSock = dp.getSocketAddress();
                            Print.logInfo("Datagram: local="+localAddr+"["+localSock+"], remote="+remoteAddr+"["+remoteSock+"]"); 
                            //Print.logInfo("Datagram: inetAddress="+this.datagramSocket.getInetAddress() + ", remoteSocket="+this.datagramSocket.getRemoteSocketAddress()); 
                        } catch (Throwable th) { // IllegalArgumentException
                            // -- ignore
                        }
                    }
                } else {
                    Print.logStackTrace("ServerSocketThread has not been properly initialized");
                    break;
                }
            } catch (SocketException se) {
                // -- shutdown support
                if (this.serverSocket != null) {
                    // -- TCP
                    int port = this.serverSocket.getLocalPort(); // should be same as 'this.listenPort'
                    if (port <= 0) { port = this.getLocalPort(); }
                    String portStr = (port <= 0)? "?" : String.valueOf(port);
                    if (LogEnable) { Print.logInfo("Shutdown TCP server on port " + portStr); }
                } else
                if (this.datagramSocket != null) {
                    // -- UDP
                    int port = this.datagramSocket.getLocalPort(); // should be same as 'this.listenPort'
                    if (port <= 0) { port = this.getLocalPort(); }
                    String portStr = (port <= 0)? "?" : String.valueOf(port);
                    if (LogEnable) { Print.logInfo("Shutdown UDP server on port " + portStr); }
                } else {
                    if (LogEnable) { Print.logInfo("Shutdown must have been called"); }
                }
            	break; // exit thread
            } catch (IOException ioe) {
                Print.logError("Connection - " + ioe);
                continue; // go back and wait again
            }

            /* ip address : port */
            //String clientIPAddress;
            //try {
            //    InetAddress inetAddr = clientSocket.getInetAddress();
            //    clientIPAddress = (inetAddr != null)? inetAddr.getHostAddress() : "?";
            //} catch (Throwable t) {
            //    clientIPAddress = "?";
            //}
            //int clientRemotePort = clientSocket.getPort();

            /* control channel packet? */
            if (this.hasControlChannel() && clientSocket.enablePeek()) {
                //Print.logInfo("Checking for ControlChannel packet ...");
                try {
                    byte cch[] = this.controlChannelHeader;
                    if (clientSocket.peekPattern(cch,500)) { // block until data or timeout
                        clientSocket.setControlChannel();
                        clientSocket.getInputStream().skip(cch.length); // skip header bytes
                        //Print.logInfo("ControlChannel header found");
                    } else {
                        //Print.logInfo("ControlChannel header not found");
                    }
                } catch (IOException ioe) {
                    Print.logException("Peek Error", ioe);
                }
            }

            /* find an available client thread */
            // -- dispatchServerSessionThread
            ServerSessionThread dispatchedSST = null;
            synchronized (this.clientThreadPool) {
                for (Iterator<ServerSessionThread> i = this.clientThreadPool.iterator(); i.hasNext();) {
                    ServerSessionThread sst = i.next();
                    boolean foundThread = sst.setClientIfAvailable(clientSocket);
                    if (foundThread) {
                        dispatchedSST = sst;
                        break;
                    }
                }
                if (dispatchedSST == null) { // add new thread to pool
                    // -- no existing ServerSessionThread found
                    int maxPoolSize = this.getMaximumClientThreadPoolSize();
                    if ((maxPoolSize <= 0) || (this.clientThreadPool.size() < maxPoolSize)) {
                        // -- create a new ServerSessionThread
                        try {
                            ServerSessionThread sst = new ServerSessionThread(clientSocket,true/*startThread*/);
                            dispatchedSST = sst;
                            this.clientThreadPool.add(dispatchedSST);
                        } catch (OutOfMemoryError oome) {
                            // -- "java.lang.OutOfMemoryError: unable to create new native thread"
                            Print.logError("Discarding client connection (unable to create new thread)");
                            try {
                                clientSocket.close();
                            } catch (Throwable th) {
                                // -- ignore
                            }
                            // -- set max client thread pool size to prevent this from occurring again
                            int mps = this.clientThreadPool.size();
                            Print.logWarn("Setting maximum client thread pool size to current size: " + mps);
                            this.setMaximumClientThreadPoolSize(mps);
                            // -- "dispatchedSST" remains null
                        }
                    } else {
                        // -- too many threads, close ClientSocket
                        // -  the client will hopefully attempt to reconnect at a later time.
                        Print.logWarn("Discarding client connection (too many threads)");
                        try {
                            clientSocket.close();
                        } catch (Throwable th) {
                            // -- ignore
                        }
                        // -- "dispatchedSST" remains null
                    }
                } else {
                    //if (LogEnable) { Print.logDebug("Reusing existing thread for ip ["+clientIPAddress+"] ..."); }
                }
            }

        } // while (true)

        /* remove from ServerSocketThread list */
        _RemoveSST(this);

    } // run()

    /**
    *** Shuts down the server 
    **/
    public boolean shutdown(long tmoMS) 
    {
        final long timeoutMS = (tmoMS >= 1000L)? tmoMS : 1000L;
    	try {

            /* shutdown all client handler threads */
            synchronized (this.clientThreadPool) {
                Iterator<ServerSessionThread> it = this.clientThreadPool.iterator();
                while (it.hasNext()) {
                    ServerSessionThread sst = it.next();
                    if (sst != null) {
                        sst.signalShutdown();
                    }
                }
            }

            /* wait for client handler threads to stop */
            long startMS = DateTime.getCurrentTimeMillis();
            boolean didTimeout = false;
            int remainingThreads = 0;
            do {
                // -- get thread count
                synchronized (this.clientThreadPool) {
                    remainingThreads = this.clientThreadPool.size();
                }
                // -- all client threads stopped?
                if (remainingThreads <= 0) {
                    // -- everything has stopped
                    break;
                }
                //if (LogEnable) { Print.logInfo("Remaining client handler threads: " + remainingThreads); }
                // -- timeout?
                long nowMS = DateTime.getCurrentTimeMillis();
                long deltaMS = nowMS - startMS;
                if (deltaMS >= timeoutMS) {
                    didTimeout = true;
                    break;
                }
                // -- short sleep
                try { Thread.sleep(500L); } catch (Throwable th) { /* ignore */ }
            } while (remainingThreads > 0);

            /* shutdown TCP listener sockets */
	    	if (this.serverSocket != null) {
	    		this.serverSocket.close();
	    	}

            /* shutdown UDP listener */
	    	if (this.datagramSocket != null) {
	    		this.datagramSocket.close();
	    	}

	    	/* successful if this did not timeout */
	    	return !didTimeout;

    	} catch (Throwable th) {

    		Print.logException("Error shutting down ServerSocketThreads",th);
    		return false;

    	}

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the remote UDP response port
    *** @param remotePort The remote UDP respose port
    **/
    public void setRemotePort(int remotePort)
    {
        this.clientPort = remotePort;
    }

    /**
    *** Gets the remote UDP response port
    *** @return The remote UDP respose port
    **/
    public int getRemotePort()
    {
        return this.clientPort;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this object has action listeners
    *** @return True if this object has action listeners
    **/
    public boolean hasListeners()
    {
        return (this.actionListeners.size() > 0);
    }

    /**
    *** Adds an action listener
    *** @param al The action listener to add
    **/
    public void addActionListener(ActionListener al)
    {
        // used for simple one way messaging
        if (!this.actionListeners.contains(al)) {
            this.actionListeners.add(al);
        }
    }

    /**
    *** Removes an action listener
    *** @param al The action listener to remove
    **/
    public void removeActionListener(ActionListener al)
    {
        this.actionListeners.remove(al);
    }

    /**
    *** Invokes action listener with the specified message
    *** @param msgBytes The message to invoke the listeners with as a byte array
    *** @return True if succesful
    **/
    protected boolean invokeListeners(byte msgBytes[])
        throws Exception
    {
        if (msgBytes != null) {
            String msg = StringTools.toStringValue(msgBytes);
            for (Iterator<?> i = this.actionListeners.iterator(); i.hasNext();) {
                Object alObj = i.next();
                if (alObj instanceof ActionListener) {
                    ActionListener al = (ActionListener)alObj;
                    ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, msg);
                    al.actionPerformed(ae);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the control channel handler
    *** @param ccc The control channel handler
    **/
    public void setControlChannelHandler(ClientPacketHandler ccc, byte ccHeader[])
    {
        if ((ccc != null) && !ListTools.isEmpty(ccHeader)) {
            this.clientControlChannel = ccc;
            this.controlChannelHeader = ccHeader;
        } else {
            this.clientControlChannel = null;
            this.controlChannelHeader = null;
        }
    }
    
    /**
    *** Returns true if this instance has a defined control channel
    **/
    public boolean hasControlChannel()
    {
        if (this.controlChannelHeader != null) {
            return true;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the client packet handler [CHECK]
    *** @param cph The client packet handler
    **/
    public void setClientPacketHandler(ClientPacketHandler cph)
    {
        this.clientPacketHandler = cph;
    }
    
    /**
    *** Sets the client packet handler class [CHECK]
    *** @param cphc The client packet handler class
    **/
    public void setClientPacketHandlerClass(Class<? extends ClientPacketHandler> cphc)
    {
        if ((cphc == null) || ClientPacketHandler.class.isAssignableFrom(cphc)) {
            this.clientPacketHandlerClass = cphc;
            this.clientPacketHandler = null;
        } else {
            throw new ClassCastException("Invalid ClientPacketHandler class");
        }
    }

    /**
    *** Gets the current client packet handler
    *** @param ctlChan  True if the control channel handler should be returned
    *** @return The current client packet handler
    **/
    public ClientPacketHandler getClientPacketHandler(boolean ctlChan)
    {
        if (ctlChan) {
            return this.clientControlChannel; // may be null
        } else
        if (this.clientPacketHandler != null) {
            // -- single instance (special case only)
            return this.clientPacketHandler;
        } else
        if (this.clientPacketHandlerClass != null) {
            // -- new instance (method used by most DCS modules)
            try {
                return (ClientPacketHandler)this.clientPacketHandlerClass.newInstance();
            } catch (Throwable t) {
                Print.logException("ClientPacketHandler", t);
                return null;
            }
        } else {
            // -- not defined
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the session timeout in milliseconds
    *** @param timeoutMS The session timeout in milliseconds
    **/
    public void setSessionTimeout(long timeoutMS)
    {
        this.sessionTimeoutMS = timeoutMS;
    }

    /**
    *** Gets the session timeout in milliseconds
    *** @return The session timeout in milliseconds
    **/
    public long getSessionTimeout()
    {
        return this.sessionTimeoutMS;
    }

    /**
    *** Returns true if the session timeout has been defined
    *** @return True if the session timeout has been defined
    **/
    public boolean hasSessionTimeout()
    {
        return (this.sessionTimeoutMS > 0L)? true : false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the idle timeout in milliseconds
    *** @param timeoutMS The idle timeout in milliseconds
    **/
    public void setIdleTimeout(long timeoutMS)
    {
        this.idleTimeoutMS = timeoutMS;
    }
    
    /**
    *** Gets the idle timeout in milliseconds
    *** @return The idle timeout in milliseconds
    **/
    public long getIdleTimeout()
    {
        // the timeout for waiting for something to appear on the socket
        return this.idleTimeoutMS;
    }

    /**
    *** Sets the packet timeout in milliseconds
    *** @param timeoutMS The packet timeout in milliseconds
    **/
    public void setPacketTimeout(long timeoutMS)
    {
        // once a byte is finally read, the timeout for waiting until the 
        // entire packet is finished
        this.packetTimeoutMS = timeoutMS;
    }
    
    /**
    *** Gets the packet timeout in milliseconds
    *** @return The packet timeout in milliseconds
    **/
    public long getPacketTimeout()
    {
        return this.packetTimeoutMS;
    }

    /**
    *** Sets if the thread should be terminated after a timeout [CHECK]
    *** @param timeoutQuit True if the thread should be terminated after a timeout
    **/
    public void setTerminateOnTimeout(boolean timeoutQuit)
    {
        this.terminateOnTimeout = timeoutQuit;
    }

    /**
    *** Gets if the thread should be terminated after a timeout [CHECK]
    *** @return True if the thread should be terminated after a timeout
    **/
    public boolean getTerminateOnTimeout()
    {
        return this.terminateOnTimeout;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the linger timeout in seconds
    *** @param timeoutSec The linger timeout in seconds
    **/
    public void setLingerTimeoutSec(int timeoutSec)
    {
        this.lingerTimeoutSec = timeoutSec;
    }

    /**
    *** Gets the linger timeout in seconds
    *** @return The linger timeout in seconds
    **/
    public int getLingerTimeoutSec()
    {
        return this.lingerTimeoutSec;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets if the packets to be sent are text
    *** @param isText True if the packets are text
    **/
    public void setTextPackets(boolean isText)
    {
        this.isTextPackets = isText;
        if (!this.isTextPackets()) {
            this.setBackspaceChar(null);
            this.setIgnoreChar(null);
            //this.setLineTerminatorChar(null);
        }
    }

    /**
    *** Returns true if the packets are text
    *** @return Ture if the packets are text
    **/
    public boolean isTextPackets()
    {
        return this.isTextPackets;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the maximum packet length
    *** @param len  The maximum packet length
    **/
    public void setMaximumPacketLength(int len)
    {
        this.maxReadLength = len;
    }

    /**
    *** Gets the maximum packet length
    *** @return  The maximum packet length
    **/
    public int getMaximumPacketLength()
    {
        if (this.maxReadLength > 0) {
            return this.maxReadLength;
        } else
        if (this.isTextPackets()) {
            return 2048; // default for text packets
        } else {
            return 1024; // default for binary packets
        }
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the minimum packet length
    *** @param len  The minimum packet length
    **/
    public void setMinimumPacketLength(int len)
    {
        this.minReadLength = len;
    }

    /**
    *** Gets the minimum packet length
    *** @return  The minimum packet length
    **/
    public int getMinimumPacketLength()
    {
        if (this.minReadLength > 0) {
            return this.minReadLength;
        } else
        if (this.isTextPackets()) {
            return 1; // at least '\r' (however, this isn't used for text packets)
        } else {
            return this.getMaximumPacketLength();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the line terminator character
    *** @param term The line terminator character
    **/
    public void setLineTerminatorChar(int term)
    {
        this.setLineTerminatorChar(new int[] { term });
    }

    /**
    *** Sets the line terminator characters
    *** @param term The line terminator characters
    **/
    public void setLineTerminatorChar(int term[])
    {
        this.lineTerminatorChar = term;
    }

    /**
    *** Gets the line terminator characters
    *** @return The line terminator characters
    **/
    public int[] getLineTerminatorChar()
    {
        return this.lineTerminatorChar;
    }

    /**
    *** Returns true if <code>ch</code> is a line terminator
    *** @return True if <code>ch</code> is a line terminator
    **/
    public boolean isLineTerminatorChar(int ch)
    {
        int termChar[] = this.getLineTerminatorChar();
        return this._isCharInList(ch, termChar);
    }

    /**
    *** Returns true if <code>ch</code> is a line terminator
    *** @return True if <code>ch</code> is a line terminator
    **/
    private boolean _isCharInList(int ch, int termChar[])
    {
        if ((termChar != null) && (ch >= 0)) {
            for (int i = 0; i < termChar.length; i++) {
                if (termChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets whether the line terminator character should be included in the returned packets
    *** @param rtnTerm  True to include the line terminator character in returned packets
    **/
    public void setIncludePacketLineTerminator(boolean rtnTerm)
    {
        this.includePacketLineTerm = rtnTerm;
    }
   
    /**
    *** Returns True if the line terminator character should be included in the returned packet
    *** @return True if the line terminator character should be included in the returned packet
    **/
    public boolean getIncludePacketLineTerminator()
    {
        return this.includePacketLineTerm;
    }

    /**
    *** Returns True if the line terminator character should be included in the returned packet
    *** @return True if the line terminator character should be included in the returned packet
    **/
    public boolean includePacketLineTerminator()
    {
        return this.includePacketLineTerm;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the packet terminator pattern
    *** @param pktTerm The packet terminator pattern
    **/
    public void setPacketTerminatorPattern(byte pktTerm[])
    {
        // "getActualPacketLength" is not called when this is set
        this.packetTermPattern = !ListTools.isEmpty(pktTerm)? pktTerm : null;
        this.setTextPackets(false);
    }

    /**
    *** Returns the packet terminator pattern
    *** @return The packet terminator pattern
    **/
    public byte[] getPacketTerminatorPattern()
    {
        return !ListTools.isEmpty(this.packetTermPattern)? this.packetTermPattern : null;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the backspace character
    *** @param bs The backspace character
    **/
    public void setBackspaceChar(int bs)
    {
        this.setBackspaceChar(new int[] { bs });
    }

    /**
    *** Sets the backspace characters
    *** @param bs The backspace characters
    **/
    public void setBackspaceChar(int bs[])
    {
        this.backspaceChar = bs;
    }

    /**
    *** Gets the backspace characters
    *** @return The backspace characters
    **/
    public int[] getBackspaceChar()
    {
        return this.backspaceChar;
    }

    /**
    *** Returns true if <code>ch</code> is a backspace character
    *** @return True if <code>ch</code> is a backspace character
    **/
    public boolean isBackspaceChar(int ch)
    {
        if (!this.hasPrompt()) {
            // -- no backspace for non-prompted sessions
            return false;
        } else
        if ((this.backspaceChar != null) && (ch >= 0)) {
            for (int i = 0; i < this.backspaceChar.length; i++) {
                if (this.backspaceChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the characters to ignore
    *** @param bs The characters to ignore
    **/
    public void setIgnoreChar(int bs[])
    {
        this.ignoreChar = bs;
    }

    /**
    *** Gets the characters to ignore
    *** @return The characters to ignore
    **/
    public int[] getIgnoreChar()
    {
        return this.ignoreChar;
    }
    
    /**
    *** Returns true if <code>ch</code> is a character to ignore
    *** @return True if <code>ch</code> is a character to ignore
    **/
    public boolean isIgnoreChar(int ch)
    {
        if ((this.ignoreChar != null) && (ch >= 0)) {
            for (int i = 0; i < this.ignoreChar.length; i++) {
                if (this.ignoreChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }
   
    // ------------------------------------------------------------------------
    
    /**
    *** If a default automatically generated prompt should be used [CHECK](all prompt related below)
    *** @param auto Ture if default automatic prompt should be used
    **/
    public void setAutoPrompt(boolean auto)
    {
        if (auto) {
            this.prompt = null;
            this.autoPrompt = true;
        } else {
            this.autoPrompt = false;
        }
    }
    
    /**
    *** Sets the prompt for TCP connections
    *** @param prompt The prompt
    **/
    public void setPrompt(byte prompt[])
    {
        this.prompt = prompt;
        this.autoPrompt = false;
    }

    /**
    *** Sets the prompt for TCP connections
    *** @param prompt The prompt
    **/
    public void setPrompt(String prompt)
    {
        this.setPrompt(StringTools.getBytes(prompt));
    }

    /**
    *** Gets the prompt for a specified index
    *** @param ndx The index (used for auto prompt)
    **/
    protected byte[] getPrompt(int ndx)
    {
        this.promptIndex = ndx;
        if (this.prompt != null) {
            return this.prompt;
        } else
        if (this.autoPrompt && this.isTextPackets()) {
            return StringTools.getBytes("" + (this.promptIndex+1) + "> ");
        } else {
            return null;
        }
    }

    /**
    *** If this server has a valid prompt
    *** @return True if this server has a valid prompt
    **/
    public boolean hasPrompt()
    {
        return (this.prompt != null) || (this.autoPrompt && this.isTextPackets());
    }

    /**
    *** Sets the prompt enabled state
    *** @param enable  True to enable prompt, false to disable
    **/
    public void setPromptEnabled(boolean enable)
    {
        this.promptEnabled = enable;
    }

    /**
    *** Gets the prompt enabled state
    *** @return  True to enable prompt, false to disable
    **/
    public boolean getPromptEnabled()
    {
        return this.promptEnabled && this.hasPrompt();
    }

    /**
    *** Gets the current prompt index (used for auto prompt)
    *** @return The current prompt index
    **/
    protected int getPromptIndex()
    {
        return this.promptIndex;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Find the named TCP session and write the specified bytes TCP output stream
    *** @param sessionID  The session ID
    *** @param data       The bytes to write
    *** @return True if the bytes were written, false otherwise
    **/
    public boolean tcpWriteToSessionID(String sessionID, byte data[])
    {

        /* no SessionID specified? */
        if (StringTools.isBlank(sessionID)) {
            Print.logError("No TCP SessionID specified");
            return false;
        }

        /* no data to write? */
        if (ListTools.isEmpty(data)) {
            Print.logWarn("No data to write to TCP session: " + sessionID);
            return false;
        }

        /* find matching TCP SessionID and send packet */
        int     sidCount = 0;
        int     rtnOK    = 0;
        synchronized (this.activeSessionList) {
            for (ClientPacketHandler cph : this.activeSessionList) {
                if (cph.equalsSessionID(sessionID)) {
                    // -- log that we found the SessionID
                    SessionInfo si = cph.getSessionInfo();
                    if (LogEnable && (si != null)) {
                        // -- Found TCP SessionID #0 'demo/demo': 192.168.1.1:30123, 1365450028, 1365450032
                        InetAddress clIP = si.getInetAddress();
                        int       clPort = si.getRemotePort();
                        long    sessTime = si.getSessionStartTime();
                        long    recvTime = si.getSessionReceiveTime();
                        StringBuffer sb = new StringBuffer();
                        sb.append("Found TCP SessionID #");
                        sb.append(sidCount);
                        sb.append(" '").append(sessionID).append("': ");
                        sb.append(StringTools.trim(clIP)).append(":").append(clPort);
                        sb.append(", ");
                        sb.append(sessTime);
                        sb.append(", ");
                        sb.append(recvTime);
                        Print.logInfo(sb.toString());
                    }
                    // -- write command
                    if (cph.tcpWrite(data)) { rtnOK++; }
                    // -- log success/fail
                    if (LogEnable) {
                        // -- Write TCP SessionID #0 'demo/demo': 0xABCDEF0123456789 (success)
                        StringBuffer sb = new StringBuffer();
                        sb.append("Write TCP SessionID #");
                        sb.append(sidCount);
                        sb.append(" '").append(sessionID).append("': ");
                        sb.append("0x").append(StringTools.toHexString(data));
                        sb.append((rtnOK > 0)?" (success)":" (failed)");
                        Print.logInfo(sb.toString());
                    }
                    // count 
                    sidCount++;
                    // debug test terminate session
                    /* * /
                    byte DEBUG_CLOSE_SESSION[] = "@@CloseSession\r\n".getBytes();
                    if (StringTools.compare(data,DEBUG_CLOSE_SESSION) == 0) {
                        Print.logInfo("Attempting to close client TCP session");
                        cph.forceCloseTCPSession();
                    }
                    / * */
                    // break; (stop at first matching session)
                    // if (rtnOK > 0) { break; } (stop at first successful matching session)
                    // else send to all matching sessions
                    // NOTE: consider the case where a device establishes a TCP session,
                    // then abandons the session and create another.  In this case it is
                    // possible that the first command-write to the matched session above
                    // may be the abandonded/incorrect session.  To get around this, we
                    // should write to the latest active session.  For now write to all
                    // matching sessions.
                }
            }
        }
        if (LogEnable && (sidCount <= 0)) {
            Print.logWarn("TCP SessionID not found: " + sessionID); 
        }
        return (rtnOK > 0)? true : false;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final String SI_SessionID             = "SessionID";
    public static final String SI_SessionThreadName     = "SessionThreadName";
    public static final String SI_SessionProtocol       = "SessionProtocol";
    public static final String SI_SessionLocalPort      = "SessionLocalPort";
    public static final String SI_SessionRemoteHost     = "SessionRemoteHost";
    public static final String SI_SessionRemotePort     = "SessionRemotePort";
    public static final String SI_SessionStartTime      = "SessionStartTime";
    public static final String SI_SessionRunningMS      = "SessionRunningMS";
    public static final String SI_SessionError          = "SessionError";

    /**
    *** Writes the SessionInfo information for the specified ClientPacketHandler 
    *** to the specified StringBuffer.
    *** @param cph    The ClientPacketHandler from which the SessionInfo is obtained
    *** @param nowMS  The current timestamp in milliseconds
    *** @param csb    The StringBuffer into which the SessionInfo is written
    **/
    private StringBuffer _writeSessionInfoToStringBuffer(ClientPacketHandler cph, long nowMS, StringBuffer csb)
    {
        // --
        if (cph == null) {
            return csb;
        } else
        if (csb == null) {
            return csb;
        }
        // --
        String sessionID = cph.getSessionID(); // may be null
        csb.append(SI_SessionID+"=").append((sessionID != null)? sessionID : "null");
        // --
        SessionInfo si = cph.getSessionInfo();
        if (si != null) {
            // -- "SessionThreadName=Thread_1 SessionProtocol=TCP SessionLocalPort=1234 SessionRemoteHost=localhost SessionRemotePort=2345 SessionStartTime=123456789 SessionRunningMS=123"
            try {
                Thread sessThread = si.getSessionThread();
                String  protoMode = si.isTCP()? "TCP" : si.isUDP()? "UDP" : "???";
                int     localPort = si.getLocalPort();
                InetAddress remIP = si.getInetAddress();
                int       remPort = si.getRemotePort();
                long   sessTimeMS = si.getSessionStartTimeMS();
              //long     recvTime = si.getSessionReceiveTime();
                csb.append(" "+SI_SessionThreadName+"=").append(sessThread.getName());
                csb.append(" "+SI_SessionProtocol  +"=").append(protoMode);
                csb.append(" "+SI_SessionLocalPort +"=").append(localPort);
                csb.append(" "+SI_SessionRemoteHost+"=").append(remIP.toString());
                csb.append(" "+SI_SessionRemotePort+"=").append(remPort);
                csb.append(" "+SI_SessionStartTime +"=").append(sessTimeMS / 1000L);
                if (nowMS > 0L) {
                csb.append(" "+SI_SessionRunningMS +"=").append(nowMS - sessTimeMS);
                }
            } catch (Throwable th) {
                // -- unlikely, but just in case
                csb.append(" "+SI_SessionError+"=").append(StringTools.quoteString(th.toString()));
            }
        } else {
            csb.append(" "+SI_SessionError+"=").append(StringTools.quoteString("NotAvailable"));
        }
        return csb;
    }

    /**
    *** Gets the SessionInfo for the specified sessionID as a String
    *** @param sessionID The SessionID for which the SessionInfo is returned
    *** @return The SessionInfo for the specified sessionID as a String
    **/
    public String getActiveSession(String sessionID)
    {
        StringBuffer csb = new StringBuffer();
        if (!StringTools.isBlank(sessionID)) {
            synchronized (this.activeSessionList) {
                for (ClientPacketHandler cph : this.activeSessionList) {
                    if (cph.equalsSessionID(sessionID)) {
                        this._writeSessionInfoToStringBuffer(cph,0L,csb);
                        break;
                    }
                }
            }
        }
        return csb.toString(); // blank if sessionID not found
    }

    /**
    *** Gets a list of active sessionIDs
    **/
    public String[] getActiveSessionIDs()
    {
        Vector<String> actvSessIDs = new Vector<String>();
        synchronized (this.activeSessionList) {
            for (ClientPacketHandler cph : this.activeSessionList) {
                String sessionID = cph.getSessionID(); // may be null
                if (!StringTools.isBlank(sessionID)) {
                    actvSessIDs.add(sessionID);
                }
            }
        }
        return actvSessIDs.toArray(new String[actvSessIDs.size()]);
    }

    /**
    *** Displays the active session list
    *** @param header  The header to include on the list
    *** @param asb     If non-null, writes the active session list to this StringBuffer.
    ***                Otherwise to the current log file (using "Print.logInfo").
    *** @return True if at least one active session was found
    **/
    public boolean logActiveSessions(String header, StringBuffer asb)
    {
        long nowMS = DateTime.getCurrentTimeMillis();
        String PFX = "  ";

        /* header */
        if (!StringTools.isBlank(header)) {
            if (asb != null) {
                asb.append(header).append("\n");
            } else {
                Print.logInfo(header);
            }
        }

        /* iterate through active sessions */
        int count = 0;
        synchronized (this.activeSessionList) {
            StringBuffer csb = new StringBuffer();
            for (ClientPacketHandler cph : this.activeSessionList) {
                SessionInfo si = cph.getSessionInfo();
                if (si != null) {
                    count++;
                    csb.setLength(0); // clear StringBuffer
                    csb.append(PFX).append("Client: ");
                    this._writeSessionInfoToStringBuffer(cph,nowMS,csb);
                    if (asb != null) {
                        asb.append(csb).append("\n");
                    } else {
                        Print.logInfo(csb.toString());
                    }
                }
            }
        }

        /* no active sessions? */
        if ((count <= 0) && !StringTools.isBlank(header)) {
            String footer = "No active sessions";
            if (asb != null) {
                asb.append(PFX).append(footer).append("\n");
            } else {
                Print.logInfo(PFX + footer);
            }
        }

        /* return */
        return (count > 0)? true : false;

    }

    /**
    *** Displays the active session list to the current log file (using "Print.logInfo")
    *** @param header  The header to include on the list
    *** @return True if at least one active session was found
    **/
    public boolean logActiveSessions(String header)
    {
        return this.logActiveSessions(header,null);
    }

    /**
    *** Prints the active session list to stdout
    *** @param header  The header to include on the list
    *** @return True if at least one active session was found
    **/
    public boolean sysPrintActiveSessions(String header)
    {
        StringBuffer asb = new StringBuffer();
        boolean S = this.logActiveSessions(header,asb);
        Print.sysPrintln(asb.toString());
        return S;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /** 
    *** ClientSocket
    **/
    private class ClientSocket
    {
        private boolean        isOpen      = true;
        private boolean        isControl   = false;
        private Socket         tcpClient   = null;
        private DatagramPacket udpClient   = null;
        private InputStream    inpStream   = null;
        private boolean        isInpStream = false;
        private boolean        mimicTCP    = false; // InputStream
        private boolean        mimicUDP    = false; // InputStream
        // -- TCP Constructor
        public ClientSocket(Socket tcpClient) {
            this.tcpClient   = tcpClient;
            this.isOpen      = true;
        }
        // -- UDP Constructor
        public ClientSocket(DatagramPacket udpClient) {
            this.udpClient   = udpClient;
            this.isOpen      = true;
        }
        // -- InputStream Constructor
        public ClientSocket(InputStream inStream, boolean mimicTCP, boolean mimicUDP) {
            //this.udpClient = new DatagramPacket(new byte[1], 1); // placeholder to mimic UDP
            this.inpStream   = inStream;
            this.isInpStream = true;
            this.isOpen      = true;
            this.mimicTCP    = mimicTCP;
            this.mimicUDP    = mimicUDP;
        }
        // -- true if TCP
        public boolean isTCP() {
            return (this.tcpClient != null)? true : false;
        }
        // -- true if UDP
        public boolean isUDP() {
            return (this.udpClient != null)? true : false;
        }
        // -- true if input stream
        public boolean isInputStream() {
            return this.isInpStream;
        }
        // -- true if simulated TCP input stream
        public boolean isInputStreamTCP() {
            return this.isInpStream && this.mimicTCP;
        }
        // -- true if simulated UDP input stream
        public boolean isInputStreamUDP() {
            return this.isInpStream && this.mimicUDP;
        }
        // -- true if this ClientSocket represents a control channel
        public boolean isControlChannel() {
            return this.isControl;
        }
        public void setControlChannel() {
            this.isControl = true;
        }
        // -- return session type String
        public String getSessionType() {
            if (this.isTCP()) {
                return "TCP";
            } else
            if (this.isUDP()) {
                return "UDP";
            } else
            if (this.isInputStream()) {
                return "InputStream";
            } else {
                return "UNKNOWN";
            }
        }
        // -- return number of available bytes to read
        public int available() {
            // -- check if closed
            if (!this.isOpen) {
                return 0;
            }
            // -- return number of bytes available to read (without blocking)
            try {
                return this.getInputStream().available();
            } catch (Throwable t) {
                return 0;
            }
        }
        // -- return remote client IP address
        public InetAddress getInetAddress() {
            // -- check if closed
            if (!this.isOpen) {
                return null;
            }
            // -- return client remote address
            if (this.isTCP()) {
                return this.tcpClient.getInetAddress();
            } else 
            if (this.isUDP()) {
                try {
                    SocketAddress sa = this.udpClient.getSocketAddress();
                    if (sa instanceof InetSocketAddress) {
                        return ((InetSocketAddress)sa).getAddress();
                    } else {
                        return null;
                    }
                } catch (Throwable th) { // IllegalArgumentException
                    return null;
                }
            } else
            if (this.isInputStream()) {
                return null;
            } else {
                return null;
            }
        }
        // -- return remote client port
        public int getPort() {
            // -- check if closed
            if (!this.isOpen) {
                return -1;
            }
            // -- get client remote port
            if (this.isTCP()) {
                return this.tcpClient.getPort();
            } else 
            if (this.isUDP()) {
                return this.udpClient.getPort();
            } else 
            if (this.isInputStream()) {
                return -1;
            } else {
                return -1;
            }
        }
        // -- return local port
        public int getLocalPort() {
            // -- check if closed
            if (!this.isOpen) {
                return -1;
            }
            // -- return local client port
            return ServerSocketThread.this.getLocalPort();
            /*
            if (this.isTCP()) {
                return this.tcpClient.getLocalPort();
            } else 
            if (this.isUDP()) {
                // This does not return the proper port
                SocketAddress sa = this.udpClient.getSocketAddress();
                if (sa instanceof InetSocketAddress) {
                    return ((InetSocketAddress)sa).getPort();
                } else {
                    return -1;
                }
            } else
            if (this.isInputStream()) {
                return -1;
            } else {
                return -1;
            }
            */
        }
        // -- return remote client IP address
        public InetAddress getLocalInetAddress() {
            if (this.isTCP()) {
                try {
                    SocketAddress sa = this.tcpClient.getLocalSocketAddress();
                    if (sa instanceof InetSocketAddress) {
                        return ((InetSocketAddress)sa).getAddress();
                    } else {
                        return null;
                    }
                } catch (Throwable th) { // IllegalArgumentException
                    return null;
                }
            } else
            if (this.isUDP()) {
                // -- Java Bug: Java does not provide a way to get the specific 
                // -  local address the remote client connected.  
                // -  Only an issue when the UDP DatagramSocket is bound to "ANY".
                // -  (See IP_PKTINFO above for additional info)
                /*
                try {
                    SocketAddress sa = this.udpClient.getLocalSocketAddress();
                    if (sa instanceof InetSocketAddress) {
                        return ((InetSocketAddress)sa).getAddress();
                    } else {
                        return null;
                    }
                } catch (Throwable th) { // IllegalArgumentException
                    return null;
                }
                */
                return null;
            } else {
                return null;
            }
        }
        // -- return output stream
        public OutputStream getOutputStream() throws IOException {
            // -- check if closed
            if (!this.isOpen) {
                return null;
            }
            // -- return output stream (UDP does not have an output stream)
            if (this.isTCP()) {
                return this.tcpClient.getOutputStream();
            } else
            if (this.isUDP()) {
                return null;
            } else
            if (this.isInputStream()) {
                return null;
            } else {
                return null;
            }
        }
        // -- return input stream
        public InputStream getInputStream() throws IOException {
            // -- check if closed
            if (!this.isOpen) {
                return null;
            }
            // -- return input stream
            if (this.isTCP()) {
                if (this.inpStream == null) {
                    this.inpStream = this.tcpClient.getInputStream();
                }
                return this.inpStream;
            } else 
            if (this.isUDP()) {
                if (this.inpStream == null) {
                    this.inpStream = new ByteArrayInputStream(this.udpClient.getData(), 0, this.udpClient.getLength());
                } 
                return this.inpStream;
            } else
            if (this.isInputStream()) {
                return this.inpStream;
            } else {
                return null;
            }
        }
        // -- peek at the existing bytes in the stream
        public boolean enablePeek() {
            // -- check if closed
            if (!this.isOpen) {
                return false;
            }
            // -- get InputStream
            try {
                InputStream inp = this.getInputStream();
                if (inp == null) {
                    return false;
                }
                // -- wrap in PeekInputStream
                if (!(inp instanceof PeekInputStream)) {
                    this.inpStream = new PeekInputStream(this.inpStream);
                }
                return true;
            } catch (IOException ioe) {
                return false;
            }
        }
        public boolean peekPattern(byte pat[], int blockTimeoutMS) {
            if (!this.isOpen) {
                return false;
            }
            try {
                InputStream inp = this.getInputStream();
                if (inp instanceof PeekInputStream) {
                    if (blockTimeoutMS > 0L) {
                        // -- ok to block
                        this.setSoTimeout(blockTimeoutMS);  // short timeout
                        return ((PeekInputStream)inp).peekPattern(pat, true);
                    } else {
                        // -- no blocking
                        return ((PeekInputStream)inp).peekPattern(pat, false);
                    }
                } else {
                    Print.logWarn("Peek not supported");
                    return false;
                }
            } catch (SocketTimeoutException ste) {
                // -- timeout (return quietly)
                return false;
            } catch (IOException ioe) {
                Print.logWarn("Error during peekPattern: " + ioe);
                return false;
            }
        }
        // -- set socket timeout
        public void setSoTimeout(int timeoutSec) throws SocketException {
            // -- check if closed
            if (!this.isOpen) {
                return;
            }
            // -- set read timeout (TCP only)
            if (this.isTCP()) {
                this.tcpClient.setSoTimeout(timeoutSec);
            } else
            if (this.isUDP()) {
                // -- n/a
            } else
            if (this.isInputStream()) {
                // -- n/a
            } else {
                // -- n/a
            }
        }
        // -- set socket linger-on-close
        public void setSoLinger(int timeoutSec) throws SocketException {
            // -- check if closed
            if (!this.isOpen) {
                return;
            }
            // -- set linger on close (TCP only)
            if (this.isTCP()) {
                try {
                    if (timeoutSec <= 0) {
                        this.tcpClient.setSoLinger(false, 0); // no linger
                    } else {
                        this.tcpClient.setSoLinger(true, timeoutSec);
                        // -- may throw "java.net.SocketException: Invalid argument"
                    }
                } catch (SocketException se) {
                    // --
                    // java.net.SocketException: Invalid argument
                    // 	at java.net.PlainSocketImpl.socketNativeSetOption(Native Method)
                    // 	at java.net.PlainSocketImpl.socketSetOption(PlainSocketImpl.java:531)
                    // 	at java.net.PlainSocketImpl.setOption(PlainSocketImpl.java:310)
                    // 	at java.net.Socket.setSoLinger(Socket.java:913)
                    // 	at org.opengts.util.ServerSocketThread$ClientSocket.setSoLinger(ServerSocketThread.java:2011)
                    // 	at org.opengts.util.ServerSocketThread$ServerSessionThread.handleClientSession(ServerSocketThread.java:2747)
                    // 	at org.opengts.util.ServerSocketThread$ServerSessionThread.run(ServerSocketThread.java:2414)
                    // --
                    // Apparently this issue is resolved in JDK version 7.
                    throw se;
                }
            } else
            if (this.isUDP()) {
                // -- n/a
            } else
            if (this.isInputStream()) {
                // -- n/a
            } else {
                // -- n/a
            }
        }
        // -- set socket linger-on-close
        public void setSoLinger(boolean on, int timeoutSec) throws SocketException {
            // -- check if closed
            if (!this.isOpen) {
                return;
            }
            // -- set linger on close (TCP only)
            if (this.isTCP()) {
                if (timeoutSec <= 0) { on = false; }
                this.tcpClient.setSoLinger(on, timeoutSec);
            } else
            if (this.isUDP()) {
                // -- n/a
            } else
            if (this.isInputStream()) {
                // -- n/a
            } else {
                // -- n/a
            }
        }
        // -- close socket
        public void close() throws IOException {
            if (this.isTCP()) {
                this.tcpClient.close();
            } else
            if (this.isUDP()) {
                // -- n/a
            } else
            if (this.isInputStream()) {
                // -- n/a
            } else {
                // -- n/a
            }
            this.isOpen = false;
        }
    }
              
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** SessionInfo
    **/
    public interface SessionInfo // class SessionInfo
    {

        public ServerSocketThread   getSocketThread();      // the socket thread
        public ServerSessionThread  getSessionThread();     // the session thread

        public boolean              hasParentSessionInfo();
        public SessionInfo          getParentSessionInfo();

        public long                 getSessionStartTimeMS();  // Epoch session start time (milliseconds)
        public long                 getSessionStartTime();    // Epoch session start time (seconds)

        public long                 getSessionReceiveTimeMS();// Epoch session last receive time (milliseconds)
        public long                 getSessionReceiveTime();  // Epoch session last receive time (seconds)

      //public void                 setSessionObject(Object obj);
      //public Object               getSessionObject();

        public int                  getLocalPort();         // local bound port

        public boolean              isTCP();                // true if TCP session
        public boolean              isUDP();                // true if UDP session
        public boolean              isInputStream();        // true if InputStream session
        public void                 forceCloseTCPSession(); // force the TCP session to close

        public int                  getAvailableBytes();    // remaining available read bytes
        public InetAddress          getInetAddress();       // remote client IP address
        public int                  getRemotePort();        // remote client port

        public boolean              tcpWrite(byte data[]);  // write bytes asynchronously to TCP output stream
        public boolean              udpWrite(byte data[]);  // EXPERIMENTAL: send UDP datagram

        public long                 getReadByteCount();     // how many bytes we've read so far
        public long                 getWriteByteCount();    // how many bytes we've written so far

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static volatile long ServeSocketThread_counter   = 0L;
    private static volatile long ServerSessionThread_counter = 0L;

    /**
    *** ServerSessionThread
    **/
    public class ServerSessionThread
        extends Thread
        implements SessionInfo
    {

        private SessionInfo         parentSessInfo       = null;

        private Object              runLock              = new Object();
        private Object              tcpWriteLock         = new Object(); // TCP write: synchronous/asynchronous
        
        private boolean             hasStarted           = false;

        private ClientSocket        client               = null;
        
        private long                sessionStartTimeMS   = 0L;  // milliseconds
        private long                sessionReceiveTimeMS = 0L;  // milliseconds

        private Object              sessionObject        = null;
        
        private ServerSessionThread inputStreamSST       = null;

        private long                readByteCount        = 0L;
        private long                writeByteCount       = 0L;
        
        private boolean             shutdown             = false;

        //public ServerSessionThread(Socket client) {
        //    super("ClientSession");
        //    this.client = new ClientSocket(client); // TCP?
        //    this.start(); // java.lang.OutOfMemoryError: unable to create new native thread
        //}

        /**
        *** Empty Constructor 
        **/
        public ServerSessionThread() {
            this((ClientSocket)null, false);
        }

        /**
        *** ClientSocket handler constructor
        *** @param clientSock  The ClientSocket instance
        *** @param startThread True to start this thread
        **/
        public ServerSessionThread(ClientSocket clientSock, boolean startThread) {
            super("ClientSession" + "_" + 
                (startThread?
                    StringTools.format(ServerSessionThread_counter++,"000").trim() :
                    "XXX")); // this.getName()
            this.client = clientSock; // new ClientSocket thread
            if (startThread) {
                this.start(); // java.lang.OutOfMemoryError: unable to create new native thread
            }
        }

        // --------------------------------------------------------------------

        /**
        *** Gets the name of this ServerSessionThread
        **/
        private String _getName()
        {
            return super.getName();
        }

        // --------------------------------------------------------------------

        /**
        *** Start Client session thread
        *** @throws java.lang.OutOfMemoryError If unable to create new native thread.
        *** @throws IllegalThreadStateException If thread has already been started.
        **/
        public void start() { // ServerSessionThread
            try {
                if (LogEnable) { Print.logInfo("("+this._getName()+") Starting session thread ..."); }
                super.start(); // java.lang.OutOfMemoryError: unable to create new native thread
                this.hasStarted = true;
            } catch (OutOfMemoryError oome) {
                // -- "java.lang.OutOfMemoryError: unable to create new native thread"
                // -  must be handled by the method that call this "start()".
                Print.logException("Unable to start thread: ServerSessionThread",oome);
                throw oome;
            } catch (IllegalThreadStateException itse) {
                // -- unlikely
                Print.logException("Thread already started: ServerSessionThread",itse);
                //throw itse; // ignore
            }
        }

        /**
        *** (SessionInfo interface) returns the current socket thread
        *** @return the current thread
        **/
        public ServerSocketThread getSocketThread() {
            return ServerSocketThread.this;
        }

        /**
        *** (SessionInfo interface) returns the current session thread
        *** @return the current thread
        **/
        public ServerSessionThread getSessionThread() {
            return this;
        }

        // --------------------------------------------------------------------

        /**
        *** Sets the parent SessionInfo instance
        **/
        public void setParentSessionInfo(SessionInfo si) {
            this.parentSessInfo = si;
        }

        /**
        *** Gets the Parent SessionInfo instance, or this SessionInfo, if this 
        *** ServerSessionThread does not have a parent.
        *** (does not return null)
        **/
        public SessionInfo getParentSessionInfo() {
            return (this.parentSessInfo != null)? this.parentSessInfo : this.getSessionInfo();
        }

        /**
        *** Returns true is a Parent SessionInfo instance has been defined.
        **/
        public boolean hasParentSessionInfo() {
            return (this.parentSessInfo != null)? true : false;
        }

        /**
        *** Gets the SessionInfo instance to assign to clients
        *** (does not return null)
        **/
        public SessionInfo getSessionInfo() {
            return this;
        }

        // --------------------------------------------------------------------

        /* find an existing/unused ClientSocket handler thread */
        public boolean setClientIfAvailable(ClientSocket clientSocket) {
            boolean rtn = false;
            synchronized (this.runLock) {
                if (!this.hasStarted) {
                    // -- thread has not yet been started 
                    // -  (typically used for InputStream handling)
                    if (this.client != null) {
                        // -- should not occur
                        Print.logWarn("Overwriting current client!");
                    }
                    this.client = clientSocket;
                    this.runLock.notify(); // <-- not needed here, since we haven't been started
                    rtn = true;
                } else
                if (this.client != null) {
                    // -- thread running, and we already are working on a client
                    rtn = false; // not available
                } else {
                    // -- thread is running and idle, set new client
                    this.client = clientSocket;
                    this.runLock.notify();
                    rtn = true;
                }
            }
            return rtn;
        }

        public boolean isAvailable() {
            // must only be called while "ServerSocketThread.this.clientThreadPool" is locked
            boolean rtn = false;
            synchronized (this.runLock) {
                rtn = (this.client != null)? false : true;
            }
            return rtn;
        }

        // --------------------------------------------------------------------

        public int getLocalPort() {
            return ServerSocketThread.this.getLocalPort();
        }

        public int getRemotePort() {
            return this._getRemotePort(this.client);
        }

        // --------------------------------------------------------------------

        /** 
        *** Returns true if the current session transport is TCP
        **/
        public boolean isTCP() {
            return (this.client != null)? this.client.isTCP() : false;
        }

        /** 
        *** Returns true if the current session transport is UDP
        **/
        public boolean isUDP() {
            return (this.client != null)? this.client.isUDP() : false;
        }

        /** 
        *** Returns true if the current session transport is a generic InputStream
        **/
        public boolean isInputStream() {
            return (this.client != null)? this.client.isInputStream() : false;
        }

        // --------------------------------------------------------------------

        public InetAddress getInetAddress() {
            return (this.client != null)? this.client.getInetAddress() : null;
        }

        // --------------------------------------------------------------------

        public int getAvailableBytes() {
            return (this.client != null)? this.client.available() : 0;
        }

        public long getReadByteCount() {
            return this.readByteCount;
        }

        public long getWriteByteCount() {
            return this.writeByteCount;
        }

        // --------------------------------------------------------------------

        public long getSessionStartTimeMS() {
            return this.sessionStartTimeMS;
        }

        public long getSessionStartTime() {
            return this.sessionStartTimeMS / 1000L;
        }

        public long getSessionReceiveTimeMS() {
            // -- Needs to be synchronized because the read-loop thread can change
            // -  this at any time.
            long rtn = 0L;
            synchronized (this.runLock) {
                rtn = this.sessionReceiveTimeMS;
            }
            return rtn;
        }

        public long getSessionReceiveTime() {
            // -- Needs to be synchronized because the read-loop thread can change
            // -  this at any time.
            long rtn = 0L;
            synchronized (this.runLock) {
                rtn = this.sessionReceiveTimeMS / 1000L;
            }
            return rtn;
        }

        // --------------------------------------------------------------------

        /**
        *** Sets the cached InputStream handler ServerSessionThread
        **/
        public void setInputStreamSST(ServerSessionThread ihSST) {
            this.inputStreamSST = ihSST;
        }

        /**
        *** Gets the cached InputStream handler ServerSessionThread
        **/
        public ServerSessionThread getInputStreamSST() {
            return this.inputStreamSST;
        }

        // --------------------------------------------------------------------

        /**
        *** Sets the generic Object for this instance.
        **/
        public void setSessionObject(Object obj) {
            this.sessionObject = obj;
        }

        /**
        *** Gets the generic Object for this instance.
        **/
        public Object getSessionObject() {
            return this.sessionObject;
        }

        // --------------------------------------------------------------------

        /**
        *** Writes the specified data to the current client TCP OutputStream
        **/
        public boolean tcpWrite(byte data[]) {
            // -- this is intended to be called by a external thread/handler
            boolean rtn = false;
            if ((data != null) && (data.length > 0)) {
                synchronized (this.runLock) {
                    if ((this.client != null) && this.client.isTCP()) {
                        try {
                            OutputStream output = this.client.getOutputStream();
                            rtn = this._tcpWrite(output, data);
                        } catch (Throwable th) {
                            rtn = false;
                        }
                    }
                }
            }
            return rtn;
        }

        /** EXPERIMENTAL
        *** Writes the specified data as a UDP packet to the current client
        **/
        public boolean udpWrite(byte data[]) {
            // -- this is intended to be called by a external thread/handler
            InetAddress bindAddr = null; // TODO:
            boolean rtn = false;
            if ((data != null) && (data.length > 0)) {
                boolean isControl = ((this.client != null) && this.client.isControlChannel())? true : false;
                synchronized (this.runLock) {
                    if (this.client == null) {
                        // -- skip
                    } else
                    if (this.client.isTCP()) {
                        if (!isControl && LogEnable) { 
                            Print.logInfo("("+this._getName()+") UDP] Ignoring TCP write: 0x%s", StringTools.toHexString(data)); 
                        }
                    } else
                    if (this.client.isUDP()) {
                        try {
                            InetAddress inetAddr = this.client.getInetAddress();
                            int rp = this.getRemotePort();
                            this._sendUDPResponse(isControl, bindAddr, inetAddr, rp, data);
                            rtn = true;
                        } catch (Throwable th) {
                            rtn = false;
                        }
                    } else
                    if (this.client.isInputStream()) {
                        if (!isControl && LogEnable) { 
                            Print.logInfo("("+this._getName()+") UDP] Ignoring InputStream write: 0x%s", StringTools.toHexString(data)); 
                        }
                    } else {
                        // ?
                    }
                }
            }
            return rtn;
        }

        // --------------------------------------------------------------------

        /**
        *** Interrupt/close client session
        **/
        public void forceCloseTCPSession()
        {
            synchronized (this.runLock) {
                this.interrupt(); // may not interrupt pending reads
                if (this.client != null) {
                    // The above "interrupt()" does not necessarily interrupt pending
                    // reads, so the following is a bit of a hack. Closing the client
                    // socket will force the TCP session to close.
                    try { 
                        this.client.close(); 
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
        }

        /**
        *** Close the current session
        **/
        public void close() throws IOException {
            IOException rethrowIOE = null;
            synchronized (this.runLock) {
                if (this.client != null) {
                    try { 
                        this.client.close(); 
                    } catch (IOException ioe) {
                        /* unable to close? */
                        rethrowIOE = ioe;
                    }
                    this.client = null;
                    // now ready for next available ClientSocket
                }
            }
            if (rethrowIOE != null) {
                throw rethrowIOE;
            }
        }

        // --------------------------------------------------------------------

        /**
        *** Signal thread to shut down 
        **/
        public void signalShutdown() {
            synchronized (this.runLock) {
                this.shutdown = true;
                this.runLock.notify();
                // TODO: nudge blocks on TCP read
            }
        }

        /* return true if shutdown in progress */
        public boolean isShutdown() {
            return this._isShutdown();
        }

        // --------------------------------------------------------------------

        public void run() {

            /* loop forever */
            stopThread:
            while (true) {

                /* check for global shutdown request */
                if (this.isShutdown()) {
                    break stopThread; // stop this thread
                }

                /* wait for client (if necessary) */
                synchronized (this.runLock) {
                    while (this.client == null) {

                        /* wait for interrupt */
                        try { this.runLock.wait(); } catch (InterruptedException ie) {}

                        /* check for global shutdown request */
                        if (this._isShutdown()) {
                            // we've not started (or have just started) a new session, proceed to shutdown
                            break stopThread; // stop this thread
                        }

                    }
                }
                // -- this ServerSessionThread is now active in a session

                // ------------------------------------------------------------
                // Begin client session

                /* "this.client" is non-null here */
                this.handleClientSession(this.client);

                // End client session
                // ------------------------------------------------------------
    
                /* clear for next requestor */
                synchronized (this.runLock) {
                    this.client = null;
                    // now available for next ClientSocket
                }

            } // while (true)

            /* close */
            try {
                this.close();
            } catch (IOException ioe) {
                // ignore (we're closing anyway)
            }

            /* remove from thread pool */
            synchronized (ServerSocketThread.this.clientThreadPool) {
                ServerSocketThread.this.clientThreadPool.remove(this);
            }

        } // run()

        // --------------------------------------------------------------------

        public void handleClientSession(ClientSocket clientSock) {

            /* session start/recieve time */
            this.sessionStartTimeMS   = DateTime.getCurrentTimeMillis();
            this.sessionReceiveTimeMS = 0L;

            /* reset byte counts */
            this.readByteCount  = 0L;
            this.writeByteCount = 0L;

            /* "clientSock" should be non-null, but check anyway */
            if (clientSock == null) {
                Print.logStackTrace("ClientSocket is null");
                return;
            }
            boolean isControl = clientSock.isControlChannel();

            /* remote client IP address/port */
            InetAddress  inetAddr = clientSock.getInetAddress();
            int        remotePort = clientSock.getPort();
            InetAddress localAddr = clientSock.getLocalInetAddress(); // null, due to Java bug
            if (clientSock.isTCP() || clientSock.isUDP()) {
                if (LogEnable) { Print.logInfo("("+this._getName()+") Remote client port: " + inetAddr + ":" + remotePort + " [to " + localAddr + ":" + clientSock.getLocalPort() + "]"); }
            }

            /* client session handler (creates new instance if necessary) */
            ClientPacketHandler clientHandler = ServerSocketThread.this.getClientPacketHandler(isControl); // new instance for most DCS modules
            if (clientHandler != null) {
                // -- set a handle to this session thread
                clientHandler.setSessionInfo(this.getSessionInfo());
                synchronized (ServerSocketThread.this.activeSessionList) {
                    ServerSocketThread.this.activeSessionList.add(clientHandler);
                }
                boolean isDuplex = clientSock.isTCP() || clientSock.isInputStreamTCP();
                clientHandler.sessionStarted(inetAddr, isDuplex, (!isControl && ServerSocketThread.this.isTextPackets()));
            }

            /* session timeout */
            long sessionTimeoutMS = this._getSessionTimeoutMillis(clientHandler);
            long sessionTimeoutAt = (sessionTimeoutMS > 0L)? (DateTime.getCurrentTimeMillis() + sessionTimeoutMS) : -1L;

            /* process client requests */
            Throwable termError = null;
            OutputStream output = null;
            try {

                /* get output stream */
                output = clientSock.getOutputStream(); // null for UDP

                /* check for client termination request */
                if ((clientHandler == null) || !clientHandler.getTerminateSession()) {

                    /* write initial packet from server */
                    if (clientHandler != null) {
                        byte initialPacket[] = clientHandler.getInitialPacket(); // may be null
                        if ((initialPacket != null) && (initialPacket.length > 0)) {
                            if (clientSock.isTCP()) {
                                if (LogEnable) { Print.logInfo("("+this._getName()+") TCP] Initial Packet: 0x"+StringTools.toHexString(initialPacket)); }
                                this._tcpWrite(output, initialPacket);   // TCP write: synchronous
                            } else {
                                // -- ignore
                            }
                        }
                    }

                    /* loop until timeout, error, client terminate */
                    for (int i = 0;; i++) {

                        /* session timeout? */
                        if (sessionTimeoutAt > 0L) {
                            long currentTimeMS = DateTime.getCurrentTimeMillis();
                            if (currentTimeMS >= sessionTimeoutAt) {
                                throw new SSSessionTimeoutException("Session timeout");
                            }
                        }

                        /* display prompt */
                        if (this._isPromptEnabled(clientSock,clientHandler)) {
                            byte prompt[] = ServerSocketThread.this.getPrompt(i); // non-null here
                            if ((prompt != null) && (prompt.length > 0)) {
                                this._tcpWrite(output, prompt);   // TCP write: synchronous
                            }
                        }

                        /* read packet */
                        byte line[] = null;
                        if (!isControl && ServerSocketThread.this.isTextPackets()) {
                            // -- ASCII: read until packet EOL
                            line = this._readLine(clientSock, clientHandler);
                            // -- "getTerminateOnTimeout()" called on timeout (SSReadTimeoutException)
                        } else {
                            // -- Binary: read until packet length or timeout
                            line = this._readPacket(clientSock, clientHandler);
                            // -- "getTerminateOnTimeout()" called on timeout (SSReadTimeoutException)
                        }
                        // -- timeout occurred?

                        /* check for requested terminate */
                        if ((clientHandler != null) && clientHandler.getTerminateSession()) {
                            break;
                        }

                        /* set receive time */
                        if (line != null) {
                            this.sessionReceiveTimeMS = DateTime.getCurrentTimeMillis();
                        }

                        /* send packet to listeners */
                        if ((line != null) && ServerSocketThread.this.hasListeners()) {
                            try {
                                ServerSocketThread.this.invokeListeners(line);
                            } catch (Throwable t) {
                                // -- a listener can terminate this session
                                Print.logWarn("Listener terminated: " + t);
                                termError = t; // re-throw?
                                break;
                            }
                        }

                        /* handle packet, and get response */
                        if ((line != null) && (clientHandler != null)) {
                            try {
                                clientHandler.setSendResponse(true); // default to send response (ACK)
                                byte response[] = clientHandler.getHandlePacket(line);
                                if ((response != null) && (response.length > 0) && clientHandler.getSendResponse()) {
                                    if (clientSock.isTCP()) {
                                        // -- TCP: Send response over socket connection
                                        if (!isControl && LogEnable) {
                                            if (!StringTools.isPrintableASCII(response)) {
                                            Print.logInfo("("+this._getName()+") TCP Resp Hex: 0x%s", StringTools.toHexString(response)); 
                                            }
                                            Print.logInfo("("+this._getName()+") TCP Resp Asc: %s"  , StringTools.toStringValue(response,'.')); 
                                        }
                                        this._tcpWrite(output, response);   // TCP write: synchronous
                                    } else
                                    if (clientSock.isUDP()) {
                                        // -- UDP: Send response via datagram ('ServerSocketThread.this.datagramSocket' is non-null)
                                        int rp = this._getRemotePort(clientSock,clientHandler.getResponsePort());
                                        try {
                                            InetAddress clientBindAddr = clientSock.getLocalInetAddress(); // null, due to Java bug
                                            this._sendUDPResponse(isControl, clientBindAddr, inetAddr, rp, response); // possible IOException
                                        } catch (IOException ioeUDP) {
                                            Print.logException("Sending UDP response ["+inetAddr+":"+rp+"]", ioeUDP); 
                                            // -- exception ignored for now
                                        }
                                    } else
                                    if (clientSock.isInputStream()) {
                                        if (LogEnable) { Print.logInfo("("+this._getName()+") Ignoring InputStream response: 0x%s", StringTools.toHexString(response)); }
                                    } else {
                                        // -- ?
                                    }
                                } else {
                                    //if (LogEnable) { Print.logInfo("("+this._getName()+") No response requested"); }
                                }
                                if (clientHandler.getTerminateSession()) {
                                    break;
                                }
                            } catch (OutOfMemoryError oome) {
                                // -- "java.lang.OutOfMemoryError: Java heap space"
                                Print.logException("Out of memory error: ", oome);
                                OSTools.checkMemoryUsage(false/*reset*/);
                                break;
                            } catch (Throwable t) {
                                // -- the ClientPacketHandler can terminate this session
                                Print.logException("Unexpected exception: ", t);
                                break;
                            }
                        }

                        /* terminate now if we're reading a Datagram and we're out of data */
                        if (clientSock.isTCP()) {
                            // -- check for shutdown request during TCP
                            if (this._isShutdown()) {
                                // -- force TCP session to terminate now
                                break;
                            }
                        } else
                        if (clientSock.isUDP()) {
                            int avail = clientSock.available();
                            if (avail <= 0) {
                                // -- Normal end of UDP connection
                                break; // break socket read loop
                            } else {
                                // -- Still have more UDP packet data
                                if (LogEnable) { Print.logDebug("("+this._getName()+") UDP: bytes remaining - %d", avail); }
                            }
                        } else
                        if (clientSock.isInputStream()) {
                            int avail = clientSock.available();
                            if (avail <= 0) {
                                // -- Normal end of InputStream connection
                                break; // break socket read loop
                            } else {
                                // -- Still have more InputStream packet data
                                if (LogEnable) { Print.logDebug("("+this._getName()+") InputStream: bytes remaining - %d", avail); }
                            }
                        } else {
                            // -- ?
                        }

                    } // socket read loop

                }

            } catch (SSSessionTimeoutException ste) {
                if (RTConfig.isDebugMode()) {
                    //Print.logException("Timeout", ste);
                    Print.logWarn(ste.getMessage());
                } else {
                    Print.logWarn(ste.getMessage());
                }
                termError = ste;
            } catch (SSReadTimeoutException rte) {
                if (rte.getByteIndex() <= 0) {
                    // end of stream at normal packet boundry
                    Print.logInfo(rte.getMessage());
                } else {
                    // end of stream within expected packet
                    Print.logWarn(rte.getMessage());
                    termError = rte;
                }
            } catch (SSEndOfStreamException eos) {
                if (clientSock.isTCP()) { // run
                    if (eos.getByteIndex() <= 0) {
                        // end of stream at packet boundry
                        Print.logInfo(eos.getMessage());
                    } else {
                        // end of stream within expected packet
                        Print.logWarn(eos.getMessage());
                        termError = eos;
                    }
                } else {
                    // We're at the normal end of the UDP datastream
                }
            } catch (SocketException se) {
                Print.logError("Connection closed");
                termError = se;
            } catch (Throwable t) {
                Print.logException("?", t);
                termError = t;
            }

            /* display end-of-session logging */
            if (!isControl && LogEnable) { 
                String sessType = clientSock.getSessionType();
                long   deltaMS  = DateTime.getCurrentTimeMillis() - this.sessionStartTimeMS;
                if ((clientHandler != null) && clientHandler.getTerminateSession()) {
                    Print.logInfo("("+this._getName()+") End of " + sessType + " session [" + deltaMS + " ms] (terminated) ..."); 
                } else
                if (termError != null) {
                    Print.logInfo("("+this._getName()+") End of " + sessType + " session [" + deltaMS + " ms] (error/warning) ..."); 
                } else 
                if (this._isShutdown()) {
                    Print.logInfo("("+this._getName()+") End of " + sessType + " session [" + deltaMS + " ms] (shutdown) ..."); 
                } else {
                    Print.logInfo("("+this._getName()+") End of " + sessType + " session [" + deltaMS + " ms] (normal) ..."); 
                }
            }

            /* client session terminated */
            if (clientHandler != null) {
                if (clientHandler.getSendResponse()) {
                    // -- send final packet
                    try {
                        byte finalPacket[] = clientHandler.getFinalPacket(termError != null);
                        if ((finalPacket != null) && (finalPacket.length > 0)) {
                            if (clientSock.isTCP()) {
                                // -- TCP: Send response over socket connection
                                if (!isControl && LogEnable) { 
                                    Print.logInfo("("+this._getName()+") TCP] Final Packet: 0x"+StringTools.toHexString(finalPacket)); 
                                }
                                this._tcpWrite(output, finalPacket);   // TCP write: synchronous
                            } else
                            if (clientSock.isUDP()) {
                                // -- UDP: Send response via datagram ('ServerSocketThread.this.datagramSocket' is non-null)
                                if (LogEnable) { Print.logInfo("("+this._getName()+") UDP] Final Packet: 0x"+StringTools.toHexString(finalPacket)); }
                                InetAddress clientBindAddr = clientSock.getLocalInetAddress(); // null, due to Java bug
                                int rp = this._getRemotePort(clientSock,clientHandler.getResponsePort());
                                this._sendUDPResponse(isControl, clientBindAddr, inetAddr, rp, finalPacket);
                            } else 
                            if (clientSock.isInputStream()) {
                                if (LogEnable) { Print.logInfo("("+this._getName()+") Ignoring InputStream finalPacket: 0x" + StringTools.toHexString(finalPacket)); }
                            } else {
                                // 
                            }
                        }
                    } catch (Throwable t) {
                        Print.logException("Final packet transmission", t);
                    }
                }
                // -- session terminated
                clientHandler.sessionTerminated(termError, this.readByteCount, this.writeByteCount);
                synchronized (ServerSocketThread.this.activeSessionList) {
                    ServerSocketThread.this.activeSessionList.remove(clientHandler);
                }
                // -- clear the session so that it doesn't hold on to an instance of this class
                clientHandler.setSessionInfo(null);
            }

            /* flush output before closing */
            if (output != null) { // TCP
                try {
                    output.flush();
                } catch (IOException ioe) {
                    Print.logException("Flush", ioe);
                } catch (Throwable t) {
                    Print.logException("?", t);
                }
            }

            /* linger on close */
            if (clientSock.isTCP()) {
                int ltSec = ServerSocketThread.this.getLingerTimeoutSec();
                try {
                    clientSock.setSoLinger(ltSec); // (seconds)
                } catch (SocketException se) {
                    // java.net.SocketException: Invalid argument
                    // 	at java.net.PlainSocketImpl.socketNativeSetOption(Native Method)
                    // 	at java.net.PlainSocketImpl.socketSetOption(PlainSocketImpl.java:531)
                    // 	at java.net.PlainSocketImpl.setOption(PlainSocketImpl.java:310)
                    // 	at java.net.Socket.setSoLinger(Socket.java:913)
                    // 	at org.opengts.util.ServerSocketThread$ClientSocket.setSoLinger(ServerSocketThread.java:2011)
                    // 	at org.opengts.util.ServerSocketThread$ServerSessionThread.handleClientSession(ServerSocketThread.java:2747)
                    // 	at org.opengts.util.ServerSocketThread$ServerSessionThread.run(ServerSocketThread.java:2414)
                    if (RTConfig.isDebugMode()) {
                        Print.logException("[non-critical]: setSoLinger("+ltSec+")", se);
                    } else {
                        Print.logWarn("[non-critical]: setSoLinger("+ltSec+"): " + se);
                    }
                    // -- continue
                } catch (Throwable t) {
                    Print.logException("[non-critical]: Unexpected exeption - setSoLinger("+ltSec+")", t);
                    // -- continue
                }
            } else
            if (clientSock.isUDP()) {
                // no linger
            } else
            if (clientSock.isInputStream()) {
                // no linger
            } else {
                // ?
            }

            /* close socket */
            try { 
                clientSock.close(); 
            } catch (IOException ioe) {
                /* unable to close? */
            }
                
        }

        // --------------------------------------------------------------------

        private int _getRemotePort(ClientSocket clientSock) {
            int rPort = ServerSocketThread.this.getRemotePort(); // likely always '0'
            if ((rPort <= 0) && (clientSock != null)) { 
                rPort = clientSock.getPort(); // preferred port#
            }
            return rPort;
        }

        private int _getRemotePort(ClientSocket clientSock, int overridePort) {
            return (overridePort > 0)? overridePort : this._getRemotePort(clientSock);
        }

        // --------------------------------------------------------------------

        private boolean _isLineTerminatorChar(ClientPacketHandler clientHandler, int ch)
        {
            if (clientHandler != null) {
                int termChar[] = clientHandler.getLineTerminatorChar();
                if (termChar != null) {
                    return ServerSocketThread.this._isCharInList(ch,termChar);
                }
            }
            return ServerSocketThread.this.isLineTerminatorChar(ch);
        }

        private boolean _isIgnoreChar(ClientPacketHandler clientHandler, int ch)
        {
            if (clientHandler != null) {
                int ignChar[] = clientHandler.getIgnoreChar();
                if (ignChar != null) {
                    return ServerSocketThread.this._isCharInList(ch,ignChar);
                }
            }
            return ServerSocketThread.this.isIgnoreChar(ch);
        }

        private long _getIdleTimeoutMillis(ClientPacketHandler clientHandler)
        {
            if (clientHandler != null) {
                long tmo = clientHandler.getIdleTimeoutMillis();
                if (tmo > 0L) {
                    return tmo;
                }
            }
            return ServerSocketThread.this.getIdleTimeout();
        }

        private long _getPacketTimeoutMillis(ClientPacketHandler clientHandler)
        {
            if (clientHandler != null) {
                long tmo = clientHandler.getPacketTimeoutMillis();
                if (tmo > 0L) {
                    return tmo;
                }
            }
            return ServerSocketThread.this.getIdleTimeout();
        }

        private long _getSessionTimeoutMillis(ClientPacketHandler clientHandler)
        {
            if (clientHandler != null) {
                long tmo = clientHandler.getSessionTimeoutMillis();
                if (tmo > 0L) {
                    return tmo;
                }
            }
            return ServerSocketThread.this.getSessionTimeout();
        }

        private int _getMinimumPacketLength(ClientPacketHandler clientHandler) {
            if (clientHandler != null) {
                int len = clientHandler.getMinimumPacketLength();
                if (len > 0) {
                    return len;
                }
            }
            return ServerSocketThread.this.getMinimumPacketLength();
        }

        private int _getMaximumPacketLength(ClientPacketHandler clientHandler) {
            if (clientHandler != null) {
                int len = clientHandler.getMaximumPacketLength();
                if (len > 0) {
                    return len;
                }
            }
            return ServerSocketThread.this.getMaximumPacketLength();
        }

        // --------------------------------------------------------------------

        private boolean _isShutdown() {
            return this.shutdown;
        }

        // --------------------------------------------------------------------

        private boolean _isPromptEnabled(ClientSocket clientSock, ClientPacketHandler clientHandler) {
            if ((clientSock != null) && !clientSock.isTCP()) {
                return false; // prompt for TCP only
            } else
            if (!ServerSocketThread.this.getPromptEnabled()) {
                return false; // disabled by ServerSocketThread
            } else
            if ((clientHandler != null) && !clientHandler.getPromptEnabled()) {
                return false; // disabled by ClientPacketHandler
            } else {
                return true; // OK, display prompt
            }
        }

        // --------------------------------------------------------------------

        /*
        private void sendBytes(ClientSocket clientSock, byte resp[]) throws IOException {
            boolean isControl = ((clientSock != null) && clientSock.isControlChannel())? true : false;
            // -- not currently used
            InetAddress bindAddr = null;
            if (clientSock == null) {
                // ignore
            } else
            if (clientSock.isTCP()) {
                // TCP
                this._tcpWrite(clientSock.getOutputStream(), resp);
            } else
            if (clientSock.isUDP()) {
                // UDP: Send response via datagram 
                InetAddress inetAddr = clientSock.getInetAddress();
                int rp = this._getRemotePort(clientSock);
                this._sendUDPResponse(isControl, bindAddr, inetAddr, rp, resp);
            } else
            if (clientSock.isInputStream()) {
                // ignore
            } else {
                // ?
            }
        }
        */

        private void _sendUDPResponse(boolean isControl, InetAddress clientBindAddr, InetAddress clientAddr, int clientPort, byte pkt[]) 
            throws IOException {
            // -- "ServerSocketThread.this.datagramSocket" is non-null for UDP sessions
            if ((pkt == null) || (pkt.length == 0)) {
                //if (LogEnable) { Print.logInfo("("+this._getName()+") No response requested"); }
            } else
            if (clientPort <= 0) {
                Print.logWarn("Unable to send packet Datagram: unknown port");
            } else {
                // -- get datagram socket
                boolean closeSocket = false;
                DatagramSocket dgSocket = null;
                //if (COPY_DATAGRAM_SOCKET) {
                //    dgSocket = ServerSocketThread.this.datagramSocket;
                //    closeSocket = false;
                //} else
                if (clientBindAddr != null) {
                    // -- WARN: routing is dependent on proper bind address
                    dgSocket = ServerSocketThread.createDatagramSocket(clientBindAddr,0);
                    closeSocket = true;
                } else
                if (ACK_FROM_LISTEN_PORT) {
                    // -- ACK returned through main DatagramSocket
                    dgSocket = ServerSocketThread.this.datagramSocket; // preferred (non-null for UDP)
                    closeSocket = false;
                } else {
                    // -- WARN: may not be routed properly
                    dgSocket = ServerSocketThread.createDatagramSocket(null,0);
                    closeSocket = true;
                }
                // -- construct datagram packet
                DatagramPacket respPkt = new DatagramPacket(pkt, pkt.length, clientAddr, clientPort);
                // -- send
                int retry = 1;
                for (;retry > 0; retry--) {
                    if (!isControl && LogEnable) {
                        String clientIP = clientAddr.toString();
                        String pktHex   = StringTools.toHexString(pkt);
                        if (RTConfig.isDebugMode()) {
                            int    locPort  = dgSocket.getLocalPort();
                            String bindAddr = StringTools.blankDefault(dgSocket.getLocalAddress(),"null");
                            Print.logDebug("("+this._getName()+") UDP Response (from %s:%d to %s:%d) 0x%s",bindAddr,locPort,clientIP,clientPort,pktHex);
                        } else {
                            Print.logInfo("("+this._getName()+") UDP Response [%s:%d] 0x%s",clientIP,clientPort,pktHex);
                        }
                    }
                    dgSocket.send(respPkt);
                    this.writeByteCount += pkt.length; // + OVERHEAD?
                }
                // close
                if (closeSocket) {
                    dgSocket.close();
                }
            }
        }

        private boolean _tcpWrite(OutputStream output, byte data[]) throws IOException {
            // -- should only be called for TCP ('output' will be null for UDP)
            boolean rtn = false;
            if ((output != null) && (data != null) && (data.length > 0)) {
                synchronized (this.tcpWriteLock) { // locked to allow for asynchronous writing
                    try {
                        //String d = StringTools.toStringValue(data);
                        //if (LogEnable) { Print.logInfo("("+this._getName()+") TCPWrite [" + d.length() + "] " + d); }
                        output.write(data);
                        output.flush();
                        this.writeByteCount += data.length;
                        rtn = true;
                    } catch (IOException t) {
                        Print.logError("writeBytes error - " + t);
                        throw t;
                    }
                }
            }
            return rtn;
        }

        // --------------------------------------------------------------------

        private int _readByte(ClientSocket clientSock, ClientPacketHandler clientHandler, long timeoutAtMS, int byteNdx) throws IOException {
            // Read until:
            //  - Timeout
            //  - IO error
            //  - Read byte
            int ch;
            InputStream input = clientSock.getInputStream();
            while (true) {
                // check packet timeout
                if (timeoutAtMS > 0L) {
                    long currentTimeMS = DateTime.getCurrentTimeMillis();
                    if (currentTimeMS >= timeoutAtMS) {
                        // timeout occurred
                        if (byteNdx <= 0) {
                            throw new SSReadTimeoutException("Read timeout [empty packet]", byteNdx);
                        } else {
                            throw new SSReadTimeoutException("Read timeout [@ " + byteNdx + "]", byteNdx);
                        }
                    }
                    //if (input.available() <= 0) {
                    int minTimeout = MinimumTimeoutIntervalMS; // minimum timeout in MS
                    int maxTimeout = (int)(timeoutAtMS - currentTimeMS); // maximum timeout interval in MS
                    int actTimeout = (minTimeout <= 0)? maxTimeout : (minTimeout < maxTimeout)? minTimeout : maxTimeout;
                    clientSock.setSoTimeout(actTimeout); 
                    //}
                }
                // prform read
                try {
                    // this read is expected to time-out if no data is available
                    ch = input.read();
                    if (ch < 0) {
                        // socket likely closed by client
                        if (byteNdx <= 0) {
                            throw new SSEndOfStreamException("End of stream [empty packet]", byteNdx);
                        } else {
                            throw new SSEndOfStreamException("End of stream [@ " + byteNdx + "]", byteNdx);
                        }
                    }
                    this.readByteCount++;
                    return ch; // <-- valid character returned
                } catch (ClosedByInterruptException cbie) {
                    // timeout/interrupt
                    throw new SSEndOfStreamException("End of stream [close interrupt detected]", byteNdx);
                } catch (InterruptedIOException ie) {
                    // timeout/interrupt
                    if (clientHandler != null) {
                        clientHandler.idleTimeoutInterrupt();
                        if (clientHandler.getTerminateSession()) {
                            throw new SSEndOfStreamException("End of stream [terminate interrupt detected]", byteNdx);
                        }
                    }
                    continue;
                } catch (SocketException se) {
                    // rethrow IO error
                    throw se;
                } catch (IOException ioe) {
                    // rethrow IO error
                    throw ioe;
                }
            }
        }

        private byte[] _readLine(ClientSocket clientSock, ClientPacketHandler clientHandler) 
            throws IOException { // SSReadTimeoutException, SSEndOfStreamException, 
            // Read until:
            //  - EOL
            //  - Timeout
            //  - IO error
            //  - Read 'maxLen' characters
            boolean isControl = ((clientSock != null) && clientSock.isControlChannel())? true : false;

            /* timeouts */
            long idleTimeoutMS = this._getIdleTimeoutMillis(clientHandler);   // ServerSocketThread.this.getIdleTimeout();
            long pcktTimeoutMS = this._getPacketTimeoutMillis(clientHandler); // ServerSocketThread.this.getPacketTimeout();
            long pcktTimeoutAt = (idleTimeoutMS > 0L)? (DateTime.getCurrentTimeMillis() + idleTimeoutMS) : -1L;

            /* max read length */
            int maxLen = this._getMaximumPacketLength(clientHandler); // safety net only
            // no minimum
            
            /* set default socket timeout */
            //clientSock.setSoTimeout(10000);

            /* packet */
            byte buff[]  = new byte[maxLen];
            int  buffLen = 0;
            boolean isIdle = true;
            long readStartTime = DateTime.getCurrentTimeMillis();
            try {
                while (true) {

                    /* read byte */
                    int ch = this._readByte(clientSock, clientHandler, pcktTimeoutAt, buffLen);
                    // -- valid character returned

                    /* reset idle timeout */
                    if (isIdle) {
                        isIdle = false;
                        if (pcktTimeoutMS > 0L) {
                            // -- reset timeout
                            pcktTimeoutAt = DateTime.getCurrentTimeMillis() + pcktTimeoutMS;
                        }
                    }

                    /* check special characters */
                    if (this._isLineTerminatorChar(clientHandler,ch)) {
                        // -- end of line/packet
                        if (ServerSocketThread.this.includePacketLineTerminator()) {
                            if (buffLen >= buff.length) { // overflow?
                                byte newBuff[] = new byte[buff.length + 1];
                                System.arraycopy(buff, 0, newBuff, 0, buff.length);
                                buff = newBuff;
                            }
                            buff[buffLen++] = (byte)ch;
                        }
                        break;
                    } else
                    if (!isControl && ServerSocketThread.this.isTextPackets()) {
                        if (this._isIgnoreChar(clientHandler,ch)) {
                            // -- ignore this character (typically '\r')
                            continue;
                        } else
                        if (ServerSocketThread.this.isBackspaceChar(ch)) {
                            if (buffLen > 0) {
                                buffLen--;
                            }
                            continue;
                        } else
                        if (ch < ' ') {
                            // -- ignore non-printable characters
                            if (ch != '\t') { // keep tab chars
                                continue;
                            }
                        }
                    }

                    /* save byte */
                    if (buffLen >= buff.length) { // overflow?
                        byte newBuff[] = new byte[buff.length * 2];
                        System.arraycopy(buff, 0, newBuff, 0, buff.length);
                        buff = newBuff;
                    }
                    buff[buffLen++] = (byte)ch;

                    /* check lengths */
                    if ((maxLen > 0) && (buffLen >= maxLen)) {
                        // we've read all the bytes we can
                        break;
                    }

                }
            } catch (SSReadTimeoutException te) {
                // This could mean a protocol error
                if (buffLen > 0) {
                    Print.logWarn("Timeout: " + StringTools.toStringValue(buff, 0, buffLen));
                }
                if (ServerSocketThread.this.getTerminateOnTimeout()) {
                    throw te;
                }
           } catch (SSEndOfStreamException eos) {
                if (clientSock.isTCP()) { // readLine
                    // This could mean a protocol error
                    if (buffLen > 0) {
                        Print.logWarn("EOS: (ASCII) " + StringTools.toStringValue(buff, 0, buffLen));
                    }
                    Print.logError(eos.getMessage());
                    throw eos;
                } else
                if (clientSock.isUDP()) {
                    // We're at the end of the UDP datastream (may be an expected condition)
                    // (just fall through to return what bytes we've already read.)
                } else
                if (clientSock.isInputStream()) {
                    // We're at the end of the InputStream (may be an expected condition)
                    // (just fall through to return what bytes we've already read.)
                } else {
                    // ?
                }
            } catch (IOException ioe) {
                Print.logError("ReadLine error - " + ioe);
                throw ioe;
            }
            long readEndTime = DateTime.getCurrentTimeMillis();

            /* return packet */
            if (buff.length == buffLen) {
                // highly unlikely
                return buff;
            } else {
                // resize buffer
                byte newBuff[] = new byte[buffLen];
                System.arraycopy(buff, 0, newBuff, 0, buffLen);
                return newBuff;
            }

        }

        private byte[] _readPacket(ClientSocket clientSock, ClientPacketHandler clientHandler) 
            throws IOException { // SSReadTimeoutException, SSEndOfStreamException, SocketException
            // Read until:
            //  - Timeout
            //  - IO error
            //  - Read 'maxLen' characters
            //  - Read 'actualLen' characters
            boolean isControl = ((clientSock != null) && clientSock.isControlChannel())? true : false;

            /* timeouts */
            long idleTimeoutMS = this._getIdleTimeoutMillis(clientHandler);   // ServerSocketThread.this.getIdleTimeout();
            long pcktTimeoutMS = this._getPacketTimeoutMillis(clientHandler); // ServerSocketThread.this.getPacketTimeout();
            long pcktTimeoutAt = (idleTimeoutMS > 0L)? (DateTime.getCurrentTimeMillis() + idleTimeoutMS) : -1L;

            /* packet/read length */
            int maxLen = this._getMaximumPacketLength(clientHandler); // safety net only
            int minLen = this._getMinimumPacketLength(clientHandler); // tcp/udp dependent

            /* set default socket timeout */
            //clientSock.setSoTimeout(10000);

            /* packet termination pattern */
            byte pktTerm[] = !isControl? ServerSocketThread.this.getPacketTerminatorPattern() : null;
            int  pktState  = 0;

            /* read packet */
            byte packet[] = new byte[maxLen];
            int  packetLen = 0;
            boolean isIdle = true;
            boolean breakOnLineTerm = false;
            boolean incrementOnLineTerm = false;
            boolean failOnEOS = clientSock.isTCP();
            try {
                int actualLen = 0;
                while (true) {

                    /* read byte */
                    // hangs until byte read or timeout
                    int lastByte = this._readByte(clientSock, clientHandler, pcktTimeoutAt, packetLen);
                    // valid byte returned 

                    /* reset idle timeout */
                    if (isIdle) {
                        isIdle = false;
                        if (pcktTimeoutMS > 0L) {
                            // reset packet timeout
                            pcktTimeoutAt = DateTime.getCurrentTimeMillis() + pcktTimeoutMS;
                        }
                    }

                    /* look for line terminator? */
                    if (breakOnLineTerm) {
                        if (this._isLineTerminatorChar(clientHandler,lastByte)) {
                            // -- end of line (typically '\n')
                            if (ServerSocketThread.this.includePacketLineTerminator()) {
                                // -- TODO: check for overflow?
                                packet[packetLen++] = (byte)lastByte;
                            }
                            break;
                        } else
                        if (this._isIgnoreChar(clientHandler,lastByte)) {
                            // -- ignore this character (typically '\r')
                            continue;
                        } else {
                            // -- save byte
                            // -  TODO: check for overflow?
                            packet[packetLen++] = (byte)lastByte;
                        }
                    } else {
                        // -- save byte
                        // -  TODO: check for overflow?
                        packet[packetLen++] = (byte)lastByte;
                    }

                    /* already read maximum allowed bytes? */
                    if (packetLen >= maxLen) {
                        // -- we've read the maximum number of bytes allowed
                        // -  ignore any incremental state that may be in effect
                        break;
                    }

                    /* do we have a specified packet length? */
                    if (actualLen > 0) {
                        if (packetLen >= actualLen) {
                            // -- we've read the bytes we expected to read
                            break;
                        }
                        // -- continue reading until we achieve the requested length
                        continue;
                    }

                    // ---------------------------------------------------------
                    // -- at this point we do not yet have the actual packet length

                    /* check pattern matching */
                    // -- EXPERIMENTAL
                    if (pktTerm != null) {
                        // -- check packet termination pattern
                        if (pktTerm[pktState] == (byte)lastByte) {
                            pktState++;
                            if (pktState >= pktTerm.length) {
                                // -- we've matched the packet terminating pattern
                                break;
                            }
                        } else {
                            // -- back to initial state
                            // -- TODO: should do a proper state reset
                            // -  Consider the pattern "#!", and the input string "##!".
                            // -  In this case the 'pktState' should be set to 1 instead of 0.
                            pktState = 0;
                        }
                    }

                    /* scan for incremental line-terminator? */
                    if (incrementOnLineTerm && this._isLineTerminatorChar(clientHandler,lastByte)) {
                        // -- INCREMENTAL: found EOL
                        incrementOnLineTerm = false;
                        minLen = packetLen; // reset minLen to what we've read so far
                    }

                    /* have we met the minimum-daily-requirements? */
                    if (packetLen < minLen) {
                        // -- continue reading
                        continue;
                    }

                    // ---------------------------------------------------------
                    // -- at this point we've read the minimum required packet length

                    /* no clientHandler? */
                    if (clientHandler == null) {
                        // -- continue reading
                        continue;
                    }

                    // ---------------------------------------------------------
                    // -- at this point the client handler determines packet length

                    /* get the actual/next expected packet length */
                    int     newPktLen  = clientHandler.getActualPacketLength(packet, packetLen);
                    boolean haveActual = ((newPktLen >= 0) && (newPktLen < PACKET_LEN_INCREMENTAL_MASK));
                    int     nextLen    = (newPktLen < 0)? newPktLen : (newPktLen & PACKET_LEN_INCREMENTAL_MASK);

                    /* has the client indicated that session should be terminated? */
                    if (clientHandler.getTerminateSession()) {
                        // -- done reading, do not continue
                        break;
                    }

                    /* actual packet length specified? */
                    if (haveActual) {
                        // -- (nextLen >= 0) guaranteed
                        if (nextLen == packetLen) {
                            // -- already have exactly what we need
                            actualLen = packetLen;
                            // -- done reading, we have a packet
                            break; 
                        } else
                        if (nextLen < packetLen) {
                            // -- ERROR: "getActualPacketLength" returned a value less than the current length
                            Print.logError("Actual length ["+nextLen+"] < Packet length ["+packetLen+"]");
                            actualLen = packetLen;
                            // -- done reading, we have a packet
                            break; 
                        } else
                        if (nextLen > maxLen) {
                            Print.logError("Actual length ["+nextLen+"] > Maximum length ["+maxLen+"]");
                            actualLen = maxLen;
                            // -- continue reading until 'maxLen'
                            continue;
                        } else {
                            actualLen = nextLen;
                            // -- continue reading until packet
                            continue;
                        }
                    }

                    /* check for special case packet termination */
                    if (nextLen == PACKET_LEN_LINE_TERMINATOR) { // "-1"
                        // -- look for line terminator character
                        //if (LogEnable) { Print.logInfo("("+this._getName()+") Last Byte Read: %s [%s]", StringTools.toHexString(lastByte,8), StringTools.toHexString(packet[packetLen-1])); }
                        if (this._isLineTerminatorChar(clientHandler,lastByte)) {
                            // -- last byte was already a line terminator
                            if (!ServerSocketThread.this.includePacketLineTerminator()) {
                                packetLen--; // remove terminator
                            }
                            actualLen = packetLen;
                            // -- done reading, we have a packet
                            break;
                        } else {
                            breakOnLineTerm = true;
                            actualLen = maxLen; // continue until line-term
                            // -- continue reading until EOL
                            continue;
                        }
                    } else
                    if (nextLen == PACKET_LEN_END_OF_STREAM) { // "-2"
                        // -- read the rest of the stream
                        int avail = clientSock.available();
                        //if (LogEnable) { Print.logDebug("("+this._getName()+") Reading remaining stream bytes: " + avail); }
                        actualLen = packetLen + avail; // what we've already read, plus any remaining
                        if (actualLen > maxLen) {
                            // -- more available bytes than the allowed maximum
                            actualLen = maxLen;
                        }
                        failOnEOS = false;
                        // -- continue reading until 'maxLen' or EOS
                        continue;
                    } else
                    if (nextLen < PACKET_LEN_END_OF_STREAM) { // <= "-3"
                        // -- DEFAULT TO END-OF-STREAM
                        // -  read the rest of the stream
                        int avail = clientSock.available();
                        //if (LogEnable) { Print.logDebug("("+this._getName()+") Reading remaining stream bytes: " + avail); }
                        actualLen = packetLen + avail; // what we've already read, plus any remaining
                        if (actualLen > maxLen) {
                            // -- more available bytes than the allowed maximum
                            actualLen = maxLen;
                        }
                        failOnEOS = false;
                        // -- continue reading until 'maxLen' or EOS
                        continue;
                    }

                    /* INCREMENTAL read */
                    if (nextLen == PACKET_LEN_INCREMENTAL_MASK) {
                        // -- should scan for EOL char
                        incrementOnLineTerm = true;
                        minLen = maxLen;
                        // -- continue reading until next EOL char
                        continue;
                    } else
                    if (nextLen > maxLen) {
                        // -- specified incremental length is greater that the maximum
                        Print.logWarn("Incremental length ["+nextLen+"] > Maximum length ["+maxLen+"]");
                        minLen = maxLen;
                        // -- continue reading until 'maxLen'
                        continue;
                    } else {
                        // -- reset minimum to next length (at least one more byte)
                        minLen = (nextLen > packetLen)? nextLen : (packetLen + 1);
                        // -- continue reading until next 'minLen'
                        continue;
                    }

                } // while (true)
            } catch (SSReadTimeoutException rte) {
                if (failOnEOS) { // PACKET_LEN_END_OF_STREAM
                    // -- This could mean a protocol error
                    if (packetLen > 0) {
                        Print.logWarn("Timeout: 0x" + StringTools.toHexString(packet, 0, packetLen));
                    }
                    if (isControl || ServerSocketThread.this.getTerminateOnTimeout()) {
                        throw rte;
                    }
                } else {
                    // -- We've received a Timeout during a TCP session and the Timeout was expected
                    // -  (just fall through to return what bytes we've already read.)
                }
            } catch (SSEndOfStreamException eos) {
                if ((clientHandler != null) && clientHandler.getTerminateSession()) {
                    // -- session should be terminated
                    // -  (quietly fall through)
                } else
                if (failOnEOS) { // PACKET_LEN_END_OF_STREAM
                    // -- This could mean a protocol error
                    if (packetLen > 0) {
                        Print.logWarn("EOS: 0x" + StringTools.toHexString(packet, 0, packetLen));
                    }
                    Print.logError(eos.getMessage());
                    throw eos;
                } else {
                    // -- We've received a EOS during a TCP session and the EOS was expected, or
                    // -  We're at the end of the UDP datastream (may be an expected condition)
                    // -  (just fall through to return what bytes we've already read.)
                }
            } catch (SocketException se) {
                if ((clientHandler != null) && clientHandler.getTerminateSession()) {
                    // -- session should be terminated
                    // -  (quietly fall through)
                } else {
                    Print.logError("ReadPacket error - " + se);
                    throw se;
                }
            } catch (IOException ioe) {
                if ((clientHandler != null) && clientHandler.getTerminateSession()) {
                    // -- session should be terminated
                    // -  (quietly fall through)
                } else {
                    Print.logError("ReadPacket error - " + ioe);
                    throw ioe;
                }
            }

            /* return packet */
            if (packet.length == packetLen) {
                // -- highly unlikely
                return packet;
            } else {
                // -- resize buffer
                byte newPacket[] = new byte[packetLen];
                System.arraycopy(packet, 0, newPacket, 0, packetLen);
                return newPacket;
            }

        } // _readPacket(...)

        public String toString() {
            return this.getName();
        }

    } // ServerSessionThread
    
    // ------------------------------------------------------------------------
    
    /**
    *** SSSessionTimeoutException
    **/
    public static class SSSessionTimeoutException
        extends IOException
    {
        public SSSessionTimeoutException(String msg) {
            super(msg);
        }
    }

    /**
    *** SSReadTimeoutException
    **/
    public static class SSReadTimeoutException
        extends IOException
    {
        private int byteIndex = 0;
        private byte dataPacket[]  = null;
        private int  dataPacketLen = 0;
        public SSReadTimeoutException(String msg, int byteNdx) {
            super(msg);
            this.byteIndex = byteNdx;
        }
        public int getByteIndex() {
            return this.byteIndex;
        }
        public void setDataPacket(byte data[], int len) {
            this.dataPacket    = data;
            this.dataPacketLen = len;
        }
    }
    
    /**
    *** SSEndOfStreamException
    **/
    public static class SSEndOfStreamException
        extends IOException
    {
        private int byteIndex = 0;
        public SSEndOfStreamException(String msg, int byteNdx) {
            super(msg);
            this.byteIndex = byteNdx;
        }
        public int getByteIndex() {
            return this.byteIndex;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sends a datagram to the specified host:port
    *** @param host  The destination host
    *** @param port  The destination port
    *** @param data  The data to send
    *** @throws IOException  if an IO error occurs
    **/
    public static void sendDatagram(InetAddress host, int port, byte data[])
        throws IOException
    {
        InetAddress bind = null; // bind to any
        ServerSocketThread.sendDatagram(bind, host, port, data);
    }

    /**
    *** Sends a datagram to the specified host:port
    *** @param bind  The local bind address
    *** @param host  The destination host
    *** @param port  The destination port
    *** @param data  The data to send
    *** @throws IOException  if an IO error occurs
    **/
    public static void sendDatagram(InetAddress bind, InetAddress host, int port, byte data[])
        throws IOException
    {
        if (host == null) {
            throw new IOException("Invalid destination host");
        } else
        if (data == null) {
            throw new IOException("Data buffer is null");
        } else {
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, host, port);
            DatagramSocket datagramSocket = ServerSocketThread.createDatagramSocket(bind,0);
            datagramSocket.send(sendPacket);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        int port = RTConfig.getInt("port",1234);
        InetAddress bindAddr = null;
        try {
            ServerSocketThread sst = new ServerSocketThread(bindAddr, port, false/*ssl?*/);
            sst.setName("TCPListener_" + port);
            sst.start(); // java.lang.OutOfMemoryError: unable to create new native thread
        } catch (IOException ioe) {
            Print.logException("Error",ioe);
        }
    }

}
