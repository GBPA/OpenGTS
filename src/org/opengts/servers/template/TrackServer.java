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
//  Server Initialization
// ----------------------------------------------------------------------------
// Change History:
//  2006/06/30  Martin D. Flynn
//     -Initial release
//  2006/07/27  Martin D. Flynn
//     -Moved constant information to 'Constants.java'
// ----------------------------------------------------------------------------
package org.opengts.servers.template;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

/**
*** <code>TrackServer</code> - This module initializes the various communication
*** methods used to received data packets from the remote tracking device.
**/

public class TrackServer
{

    // ------------------------------------------------------------------------
    // initialize runtime configuration

    public static void configInit()
    {
        DCServerConfig dcs = Main.getServerConfig(null);
        if (dcs != null) {
            TrackServer.setTcpIdleTimeout(   dcs.getTcpIdleTimeoutMS(   Constants.TIMEOUT_TCP_IDLE   ));
            TrackServer.setTcpPacketTimeout( dcs.getTcpPacketTimeoutMS( Constants.TIMEOUT_TCP_PACKET ));
            TrackServer.setTcpSessionTimeout(dcs.getTcpSessionTimeoutMS(Constants.TIMEOUT_TCP_SESSION));
            TrackServer.setUdpIdleTimeout(   dcs.getUdpIdleTimeoutMS(   Constants.TIMEOUT_UDP_IDLE   ));
            TrackServer.setUdpPacketTimeout( dcs.getUdpPacketTimeoutMS( Constants.TIMEOUT_UDP_PACKET ));
            TrackServer.setUdpSessionTimeout(dcs.getUdpSessionTimeoutMS(Constants.TIMEOUT_UDP_SESSION));
        } else {
            Print.logWarn("DCServer not found: " + Main.getServerName());
        }
    }

    // ------------------------------------------------------------------------
    // Start TrackServer (TrackServer is a singleton)
    
    private static TrackServer trackServerInstance = null;
        
    /* start TrackServer on array of ports */
    public static TrackServer startTrackServer(int tcpPorts[], int udpPorts[], int commandPort)
        throws Throwable
    {
        if (trackServerInstance == null) {
            trackServerInstance = new TrackServer(tcpPorts, udpPorts, commandPort);
        }
        return trackServerInstance;
    }
    
    public static TrackServer getInstance()
    {
        return trackServerInstance;
    }

    // ------------------------------------------------------------------------
    // TCP Session timeouts

    /* idle timeout */
    private static long tcpTimeout_idle = Constants.TIMEOUT_TCP_IDLE;
    public static void setTcpIdleTimeout(long timeout)
    {
        TrackServer.tcpTimeout_idle = timeout;
    }
    public static long getTcpIdleTimeout()
    {
        return TrackServer.tcpTimeout_idle;
    }
    
    /* inter-packet timeout */
    private static long tcpTimeout_packet = Constants.TIMEOUT_TCP_PACKET;
    public static void setTcpPacketTimeout(long timeout)
    {
        TrackServer.tcpTimeout_packet = timeout;
    }
    public static long getTcpPacketTimeout()
    {
        return TrackServer.tcpTimeout_packet;
    }

    /* total session timeout */
    private static long tcpTimeout_session  = Constants.TIMEOUT_TCP_SESSION;
    public static void setTcpSessionTimeout(long timeout)
    {
        TrackServer.tcpTimeout_session = timeout;
    }
    public static long getTcpSessionTimeout()
    {
        return TrackServer.tcpTimeout_session;
    }

    // ------------------------------------------------------------------------
    // UDP Session timeouts

    /* idle timeout */
    private static long udpTimeout_idle = Constants.TIMEOUT_UDP_IDLE;
    public static void setUdpIdleTimeout(long timeout)
    {
        TrackServer.udpTimeout_idle = timeout;
    }
    public static long getUdpIdleTimeout()
    {
        return TrackServer.udpTimeout_idle;
    }

    /* inter-packet timeout */
    private static long udpTimeout_packet = Constants.TIMEOUT_UDP_PACKET;
    public static void setUdpPacketTimeout(long timeout)
    {
        TrackServer.udpTimeout_packet = timeout;
    }
    public static long getUdpPacketTimeout()
    {
        return TrackServer.udpTimeout_packet;
    }

    /* total session timeout */
    private static long udpTimeout_session = Constants.TIMEOUT_UDP_SESSION;
    public static void setUdpSessionTimeout(long timeout)
    {
        TrackServer.udpTimeout_session = timeout;
    }
    public static long getUdpSessionTimeout()
    {
        return TrackServer.udpTimeout_session;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // TCP port listener threads
    private OrderedMap<Integer,ServerSocketThread>  tcpThread = null;

    // UDP port listener threads
    private OrderedMap<Integer,ServerSocketThread>  udpThread = null;
    private OrderedMap<Integer,DatagramSocket>      udpSocket = null;

    // Command port listener thread
    private ServerSocketThread                      cmdThread = null;

    // ------------------------------------------------------------------------

    /* private constructor */
    private TrackServer(int tcpPorts[], int udpPorts[], int commandPort)
        throws Throwable
    {
        int listeners = 0;

        // Start TCP listeners
        if (!ListTools.isEmpty(tcpPorts)) {
            this.tcpThread = new OrderedMap<Integer,ServerSocketThread>();
            for (int i = 0; i < tcpPorts.length; i++) {
                int port = tcpPorts[i];
                if (ServerSocketThread.isValidPort(port)) {
                    try {
                        ServerSocketThread sst = this._startTCP(port);
                        this.tcpThread.put(new Integer(port), sst);
                        listeners++;
                    } catch (java.net.BindException be) {
                        Print.logError("TCP: Error binding to port: %d", port);
                    }
                } else {
                    throw new Exception("TCP: Invalid port number: " + port);
                }
            }
        }

        // Start UDP listeners
        if (!ListTools.isEmpty(udpPorts)) {
            this.udpThread = new OrderedMap<Integer,ServerSocketThread>();
            this.udpSocket = new OrderedMap<Integer,DatagramSocket>();
            for (int i = 0; i < udpPorts.length; i++) {
                int port = udpPorts[i];
                if (ServerSocketThread.isValidPort(port)) {
                    try {
                        ServerSocketThread sst = this._startUDP(port);
                        this.udpThread.put(new Integer(port), sst);
                        this.udpSocket.put(new Integer(port), sst.getDatagramSocket());
                        listeners++;
                    } catch (java.net.BindException be) {
                        Print.logError("UDP: Error binding to port: %d", port);
                    }
                } else {
                    throw new Exception("UDP: Invalid port number: " + port);
                }
            }
        }

        /* do we have any active listeners? */
        if (listeners <= 0) {
            Print.logWarn("No active device communication listeners!");
        }

        // start command listener
        if (commandPort > 0) {
            if (ServerSocketThread.isValidPort(commandPort)) {
                try {
                    this._startCommand(commandPort);
                } catch (java.net.BindException be) {
                    Print.logError("Command: Error binding to port: %d", commandPort);
                }
            } else {
                throw new Exception("Command: Invalid port number: " + commandPort);
            }
        } else {
            // ignore command listener
            Print.logWarn("Ignoring CommandPort listener");
        }

    }

    // ------------------------------------------------------------------------

    /* start TCP listener */
    private ServerSocketThread _startTCP(int port)
        throws Throwable
    {
        ServerSocketThread sst = null;

        /* create server socket */
        try {
            sst = new ServerSocketThread(port);
        } catch (Throwable t) { // trap any server exception
            Print.logException("ServerSocket error", t);
            throw t;
        }
        
        /* initialize */
        //sst.setIncludePacketLineTerminator(true);
        sst.setTextPackets(Constants.ASCII_PACKETS);
        sst.setBackspaceChar(null); // no backspaces allowed
        sst.setLineTerminatorChar(Constants.ASCII_LINE_TERMINATOR);
        sst.setMaximumPacketLength(Constants.MAX_PACKET_LENGTH);
        sst.setMinimumPacketLength(Constants.MIN_PACKET_LENGTH);
        sst.setIdleTimeout(TrackServer.getTcpIdleTimeout());         // time between packets
        sst.setPacketTimeout(TrackServer.getTcpPacketTimeout());     // time from start of packet to packet completion
        sst.setSessionTimeout(TrackServer.getTcpSessionTimeout());   // time for entire session
        sst.setLingerTimeoutSec(5);
        sst.setTerminateOnTimeout(Constants.TERMINATE_ON_TIMEOUT);
        sst.setClientPacketHandlerClass(TrackClientPacketHandler.class);

        /* start thread */
        DCServerConfig.startServerSocketThread(sst,"Event");
        return sst;

    }

    public ServerSocketThread getServerSocketThread_tcp(int port)
    {
        if (ListTools.isEmpty(this.tcpThread)) {
            return null;
        } else
        if (port <= 0) {
            return this.tcpThread.getFirstValue();
        } else {
            ServerSocketThread sst = this.tcpThread.get(new Integer(port));
            if (sst != null) {
                return sst;
            } else {
                Print.logWarn("TCP 'Listen' port["+port+"] not found, returning first ServerSocketThread");
                return this.tcpThread.getFirstValue();
            }
        }
    }

    // ------------------------------------------------------------------------

    /* start UDP listener */
    private ServerSocketThread _startUDP(int port)
        throws Throwable
    {
        ServerSocketThread sst = null;

        /* create server socket */
        try {
            sst = new ServerSocketThread(ServerSocketThread.createDatagramSocket(null,port));
        } catch (Throwable t) { // trap any server exception
            Print.logException("ServerSocket error", t);
            throw t;
        }
        
        /* initialize */
        //sst.setIncludePacketLineTerminator(true);
        sst.setTextPackets(Constants.ASCII_PACKETS);
        sst.setBackspaceChar(null); // no backspaces allowed
        sst.setLineTerminatorChar(Constants.ASCII_LINE_TERMINATOR);
        sst.setMaximumPacketLength(Constants.MAX_PACKET_LENGTH);
        sst.setMinimumPacketLength(Constants.MIN_PACKET_LENGTH);
        sst.setIdleTimeout(TrackServer.getUdpIdleTimeout());
        sst.setPacketTimeout(TrackServer.getUdpPacketTimeout());
        sst.setSessionTimeout(TrackServer.getUdpSessionTimeout());
        sst.setTerminateOnTimeout(Constants.TERMINATE_ON_TIMEOUT);
        sst.setClientPacketHandlerClass(TrackClientPacketHandler.class);

        /* start thread */
        DCServerConfig.startServerSocketThread(sst,"Event");
        return sst;

    }

    public DatagramSocket getUdpDatagramSocket(int port)
    {
        if (ListTools.isEmpty(this.udpSocket)) {
            return null;
        } else
        if (port <= 0) {
            return this.udpSocket.getFirstValue();
        } else {
            DatagramSocket ds = this.udpSocket.get(new Integer(port));
            if (ds != null) {
                return ds;
            } else {
                Print.logWarn("'Listen' port["+port+"] not found, returning first DatagramSocket");
                return this.udpSocket.getFirstValue();
            }
        }
    }

    // ------------------------------------------------------------------------

    /* start Command listener */
    @SuppressWarnings("unchecked")
    private void _startCommand(int port)
        throws Throwable
    {
        ServerSocketThread sst = null;

        /* get CommandPacketHandler class */
        Class<? extends CommandPacketHandler> cmdPktClass = null;
        try {
            cmdPktClass = (Class<CommandPacketHandler>)Class.forName("org.opengts.servers.template.TemplateCommandHandler");
        } catch (Throwable th) {
            return;
        }

        /* create server socket */
        try {
            sst = new ServerSocketThread(port);
        } catch (Throwable t) { // trap any server exception
            Print.logException("ServerSocket error", t);
            throw t;
        }

        /* initialize */
        sst.setTextPackets(true);
        sst.setBackspaceChar(null); // no backspaces allowed
        sst.setLineTerminatorChar(new int[] { '\r', '\n' });
        sst.setIgnoreChar(null);
        sst.setMaximumPacketLength(1200);       // safety net
        sst.setMinimumPacketLength(1);
        sst.setIdleTimeout(1000L);              // time between packets
        sst.setPacketTimeout(1000L);            // time from start of packet to packet completion
        sst.setSessionTimeout(10000L);          // time for entire session
        sst.setLingerTimeoutSec(5);
        sst.setTerminateOnTimeout(true);
        sst.setClientPacketHandlerClass(cmdPktClass);

        /* start thread */
        Print.logInfo("Starting Command listener thread on port " + port + " [timeout=" + sst.getSessionTimeout() + "ms] ...");
        sst.start();
        this.cmdThread = sst;

    }

    // ------------------------------------------------------------------------
        
}
