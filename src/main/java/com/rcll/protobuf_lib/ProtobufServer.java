/*
 *
 * Copyright (c) 2017, Graz Robust and Intelligent Production System (grips)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.rcll.protobuf_lib;

import com.rcll.robot.IRobotMessageThreadFactory;
import com.rcll.robot.RobotHandler;
import lombok.extern.apachecommons.CommonsLog;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@CommonsLog
public class ProtobufServer implements Runnable {

    Consumer<Integer> robotAddedHandler;
    private RobotConnections robotConnections;
    private List<RobotHandler> robotHandlerList;
    private ServerSocket _server_socket;
    private int listenPort;

    private final IRobotMessageThreadFactory threadFactory;

    public ProtobufServer(int listenPort, RobotConnections robotConnections,
                          IRobotMessageThreadFactory threadFactory,
                          Consumer<Integer> robotAddedHandler) {
        this.robotConnections = robotConnections;
        this.listenPort = listenPort;
        this.threadFactory = threadFactory;
        this.robotHandlerList = new ArrayList<>();
        this.robotAddedHandler = robotAddedHandler;
    }

    public void start() throws IOException {
        log.error("Called start of the protobuf server");
        _server_socket = new ServerSocket(listenPort);
        new Thread(this).start();
    }

    @Override
    public void run() {
        log.info("The serversockt hast listenport " + listenPort);
        if (_server_socket == null) {
            log.error("_server_socket is null, did you call start()");
            return;
        }
        while (true) {
            try {
                Socket live_socket = _server_socket.accept();
                String robot_address = live_socket.getInetAddress().getHostAddress();
                log.info("New Connection from IP: " + robot_address + ":" + live_socket.getPort() + ".");
                this.createHandlerForNewRobot(live_socket, threadFactory, robotAddedHandler);
            } catch (IOException e) {
                log.error("IOException: ", e);
            } catch (Exception e) {
                log.error("Exeption: ", e);
            }
        }
    }

    private void createHandlerForNewRobot(Socket liveSocket, IRobotMessageThreadFactory threadFactory,
                                          Consumer<Integer> robotAddedHandler) {
        RobotHandler handler = new RobotHandler(robotConnections, threadFactory, robotAddedHandler);
        handler.set_socket(liveSocket);
        this.robotHandlerList.add(handler);
        new Thread(handler).start();
    }
}
