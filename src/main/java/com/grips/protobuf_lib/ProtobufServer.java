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

package com.grips.protobuf_lib;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_utils.Key;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

@CommonsLog
public class ProtobufServer implements Runnable {

    private RobotConnections robotConnections;
    private ServerSocket _server_socket;
    private Consumer<Socket> onNewConnection;
    private int listenPort;

    public ProtobufServer(int listenPort, RobotConnections robotConnections, Consumer<Socket> onNewConnection) {
        this.onNewConnection = onNewConnection;
        this.robotConnections = robotConnections;
        this.listenPort = listenPort;
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

                this.onNewConnection.accept(live_socket);
            } catch (IOException e) {
                log.error("IOException: ", e);
            } catch (Exception e) {
                log.error("Exeption: ", e);
            }
        }
    }

    public void send_to_robot(long robot_id, ProtobufMessage msg) {
        if (null == msg) {
            log.error("Error: msg to send is null!");
            return;
        }
        if (null == robotConnections.getConnection(robot_id)) {
            log.error("Error: No socket for robot with id " + robot_id + " stored!");
            return;
        }

        ByteBuffer serialized_msg = msg.serialize(false, null);
        try {
            DataOutputStream data_out = new DataOutputStream(robotConnections.getConnection(robot_id).getOutputStream());
            data_out.write(serialized_msg.array());
            data_out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T extends GeneratedMessageV3> void send_to_robot(long robot_id, T msg) {
        if (null == msg) {
            log.error("Error: msg to send is null!");
            return;
        }

        Key key = RobotMessageRegister.getInstance().get_msg_key_from_class(msg.getClass());
        ProtobufMessage proto_msg = new ProtobufMessage(key.cmp_id, key.msg_id, msg);
        send_to_robot(robot_id, proto_msg);
    }
}
