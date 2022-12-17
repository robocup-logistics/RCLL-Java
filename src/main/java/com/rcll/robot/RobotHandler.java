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

package com.rcll.robot;

import com.google.protobuf.GeneratedMessageV3;
import com.rcll.protobuf_lib.RobotConnections;
import com.rcll.protobuf_lib.RobotMessageRegister;
import lombok.extern.apachecommons.CommonsLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

@CommonsLog
public class RobotHandler implements Runnable {

    private final static int PROTOCOL_FIELD_SIZE = 1;
    private final static int CIPHER_FIELD_SIZE = 1;
    private final static int RESERVED_1_FIELD_SIZE = 1;
    private final static int RESERVED_2_FIELD_SIZE = 1;
    private final static int PAYLOAD_SIZE_FIELD_SIZE = 4;
    private final static int COMPONENT_ID_FIELD_SIZE = 2;
    private final static int MESSAGE_TYPE_FIELD_SIZE = 2;


    private final RobotConnections robotConnections;
    private final IRobotMessageThreadFactory factory;

    private final List<Thread> threads;

    private Socket _socket;
    private boolean received_invalid_packet = false;

    public RobotHandler(RobotConnections robotConnections,
                        IRobotMessageThreadFactory factory) {
        this.robotConnections = robotConnections;
        this.factory = factory;
        this.threads = new ArrayList<>();
    }

    public void set_socket(Socket _socket) {
        this._socket = _socket;
    }

    private void handle(/*@Nullable*/ InputStream in) throws IOException {
        if (null == in) {
            log.error("ERROR: in is null in RobotHandler");
            return;
        }

        AbstractMap.SimpleEntry<GeneratedMessageV3, byte[]> msg = parse_input(in);
        if (null == msg || null == msg.getKey()) {
            log.error("ERROR: Could not parse input!");
            received_invalid_packet = true;
            return;
        }

        Thread thread = factory.create(_socket, msg);
        this.threads.add(thread);
        thread.start();
    }


    @Override
    public void run() {
        while (null != _socket && _socket.isConnected() && !received_invalid_packet) {
            try {
                handle(_socket.getInputStream());
            } catch (IOException e) {
                //robotConnections.removeConnection(_socket);
                robotConnections.removeLostRobot(_socket);
                log.error("Connection to Robot lost");
                break;
            }
        }
        log.warn("after while");
        try {
            //robotConnections.removeConnection(_socket);
            robotConnections.removeLostRobot(_socket);
            _socket.close();
            log.error("Closing socket from " + _socket.getInetAddress().getHostAddress() + ":" + _socket.getPort() + ".");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //@Contract("null -> null")
    //@Nullable
    private AbstractMap.SimpleEntry<GeneratedMessageV3, byte[]> parse_input(/*@Nullable*/ InputStream in) throws IOException {
        if (null == in) return null;

        byte[] protocol_version = new byte[PROTOCOL_FIELD_SIZE];
        byte[] cipher = new byte[PROTOCOL_FIELD_SIZE];
        byte[] reserved_1 = new byte[RESERVED_1_FIELD_SIZE];
        byte[] reserved_2 = new byte[RESERVED_2_FIELD_SIZE];
        byte[] payload_size = new byte[PAYLOAD_SIZE_FIELD_SIZE];
        byte[] component_id = new byte[COMPONENT_ID_FIELD_SIZE];
        byte[] message_type = new byte[MESSAGE_TYPE_FIELD_SIZE];

        int bytes_protocol_version_read = in.read(protocol_version);
        if (PROTOCOL_FIELD_SIZE != bytes_protocol_version_read) {
            System.err.println("ERROR: Invalid Protocol field, size is " + bytes_protocol_version_read
                    + " instead of " + PROTOCOL_FIELD_SIZE + "!");
            return null;
        }
        int bytes_cipher_read = in.read(cipher);
        if (CIPHER_FIELD_SIZE != bytes_cipher_read) {
            System.err.println("ERROR: Invalid cipher field, size is " + bytes_cipher_read
                    + " instead of " + CIPHER_FIELD_SIZE + "!");
            return null;
        }
        int bytes_reserved_1_read = in.read(reserved_1);
        if (RESERVED_1_FIELD_SIZE != bytes_reserved_1_read) {
            System.err.println("ERROR: Invalid reserved 1 field, size is " + bytes_reserved_1_read
                    + " instead of " + RESERVED_1_FIELD_SIZE + "!");
            return null;
        }
        int bytes_reserved_2_read = in.read(reserved_2);
        if (RESERVED_2_FIELD_SIZE != bytes_reserved_2_read) {
            System.err.println("ERROR: Invalid reserved 2 field, size is " + bytes_reserved_2_read
                    + " instead of " + RESERVED_2_FIELD_SIZE + "!");
            return null;
        }
        int bytes_payload_size_read = in.read(payload_size);
        if (PAYLOAD_SIZE_FIELD_SIZE != bytes_payload_size_read) {
            System.err.println("ERROR: Invalid payload size field, size is " + bytes_payload_size_read
                    + " instead of " + PAYLOAD_SIZE_FIELD_SIZE + "!");
            return null;
        }
        int bytes_component_id_read = in.read(component_id);
        if (COMPONENT_ID_FIELD_SIZE != bytes_component_id_read) {
            System.err.println("ERROR: Invalid component id field, size is " + bytes_component_id_read
                    + " instead of " + COMPONENT_ID_FIELD_SIZE + "!");
            return null;
        }
        int bytes_message_type_read = in.read(message_type);
        if (MESSAGE_TYPE_FIELD_SIZE != bytes_message_type_read) {
            System.err.println("ERROR: Invalid component id field, size is " + bytes_message_type_read
                    + " instead of " + MESSAGE_TYPE_FIELD_SIZE + "!");
            return null;
        }

        int protocol_version_cvt = RobotHandler.getUnsignedInt8(ByteBuffer.wrap(protocol_version));
        int cipher_cvt = RobotHandler.getUnsignedInt8(ByteBuffer.wrap(cipher));
        int payload_size_cvt = (int) RobotHandler.getUnsignedInt32_BE(ByteBuffer.wrap(payload_size));
        int component_id_cvt = RobotHandler.getUnsignedInt16_BE(ByteBuffer.wrap(component_id));
        int message_type_cvt = RobotHandler.getUnsignedInt16_BE(ByteBuffer.wrap(message_type));


        if (payload_size_cvt - COMPONENT_ID_FIELD_SIZE - COMPONENT_ID_FIELD_SIZE < 0) {
            System.err.println("Error: Invalid Protobuf msg to parse, payload size is " + payload_size_cvt + "!");
            System.err.println("       Protocol Version: " + protocol_version_cvt);
            System.err.println("       Cipher: " + cipher_cvt);
            System.err.println("       CompID: " + component_id_cvt);
            System.err.println("       MsgID: " + message_type_cvt);
            return null;
        }

        int protobuf_msg_size = payload_size_cvt - COMPONENT_ID_FIELD_SIZE - MESSAGE_TYPE_FIELD_SIZE;
        byte[] protobuf_msg = new byte[protobuf_msg_size];
        int bytes_protobuf_msg_read = in.read(protobuf_msg);
        if (protobuf_msg_size != bytes_protobuf_msg_read) {
            System.err.println("ERROR: Invalid protobuf_msg field, size is " + bytes_protobuf_msg_read
                    + " instead of " + protobuf_msg_size + "!");
            return null;
        }

        GeneratedMessageV3 empty_msg = RobotMessageRegister.getInstance().
                get_generated_empty_msg_from_key(component_id_cvt, message_type_cvt);

        return new AbstractMap.SimpleEntry<>(empty_msg, protobuf_msg);
    }

    public static short getUnsignedInt8(ByteBuffer data) {
        byte bytes = data.get();
        int ints = 0xFF & bytes;
        short n = (short) ints;
        return n;
    }

    public static int getUnsignedInt16(ByteBuffer data) {
        byte[] bytes = new byte[2];
        for (int i = 1; i >= 0; i--) {
            bytes[i] = data.get();
        }
        int[] ints = new int[2];
        for (int i = 0; i < 2; i++) {
            ints[i] = 0xFF & bytes[i];
        }
        int n = ((int) (ints[0] << 8
                | ints[1]))
                & 0xFFFF;            //FIXED! 0xFF is too less for uint16_t!
        return n;
    }

    public static int getUnsignedInt16_BE(ByteBuffer data) {
        byte[] bytes = new byte[2];
        for (int i = 0; i <= 1; i++) {
            bytes[i] = data.get();
        }
        int[] ints = new int[2];
        for (int i = 0; i < 2; i++) {
            ints[i] = 0xFF & bytes[i];
        }
        int n = ((int) (ints[0] << 8
                | ints[1]))
                & 0xFFFF;            //FIXED! 0xFF is too less for uint16_t!
        return n;
    }

    public static long getUnsignedInt32(ByteBuffer data) {
        byte[] bytes = new byte[4];
        for (int i = 3; i >= 0; i--) {
            bytes[i] = data.get();
        }
        int[] ints = new int[4];
        for (int i = 0; i < 4; i++) {
            ints[i] = 0xFF & bytes[i];
        }
        long n = ((long) (ints[0] << 24
                | ints[1] << 16
                | ints[2] << 8
                | ints[3]))
                & 0xFFFFFFFFL;
        return n;
    }

    public static long getUnsignedInt32_BE(ByteBuffer data) {
        byte[] bytes = new byte[4];
        for (int i = 0; i <= 3; i++) {
            bytes[i] = data.get();
        }
        int[] ints = new int[4];
        for (int i = 0; i < 4; i++) {
            ints[i] = 0xFF & bytes[i];
        }
        long n = ((long) (ints[0] << 24
                | ints[1] << 16
                | ints[2] << 8
                | ints[3]))
                & 0xFFFFFFFFL;
        return n;
    }

    public static String getString(ByteBuffer data, int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = data.get();
        }
        String value = new String(bytes);
        return value;
    }

    public static boolean getBool(ByteBuffer data) {
        byte b = data.get();
        if ((int) b == 0) {
            return false;
        } else {
            return true;
        }
    }

    public static void putBool(ByteBuffer data, boolean b) {
        if (b) {
            data.put((byte) 42);
        } else {
            data.put((byte) 0);
        }
    }
}
