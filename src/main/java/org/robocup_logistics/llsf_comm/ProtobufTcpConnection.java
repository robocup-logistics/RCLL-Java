package org.robocup_logistics.llsf_comm;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageV3;
import org.robocup_logistics.llsf_exceptions.EncryptedStreamMessageException;
import org.robocup_logistics.llsf_exceptions.UnknownProtocolVersionException;
import org.robocup_logistics.llsf_utils.Key;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * The ProtobufClient is a client to communicate with a refbox via protobuf messages. You can
 * send stream messages (TCP) by calling the enqueue method. To receive messages, register a
 * ProtobufMessageHandler, incoming messages will be passed to your handler. To send and receive
 * broadcast messages (UDP), use the ProtobufBroadcastPeer.
 */
public class ProtobufTcpConnection implements ProtobufConnection {

    private String hostname;
    private int port;
    private SocketChannel sockchan;

    private Queue<ProtobufMessage> queue1;
    private Queue<ProtobufMessage> queue2;
    private Queue<ProtobufMessage> act_q;
    private Queue<ProtobufMessage> send_q;
    private ConThread con;
    private SendThread send;
    private RecvThread recv;

    private HashMap<Key, GeneratedMessageV3> msgs = new HashMap<>();
    private HashMap<String, Key> classNameToKey = new HashMap<>();
    private ProtobufMessageHandler handler;

    private boolean is_connected = false;

    /**
     * Instantiates a new ProtobufClient. This method does not connect (see connect).
     *
     * @param hostname the IP address of the refbox
     * @param port     the port to which to connect
     * @see connect()
     */
    public ProtobufTcpConnection(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Tries to connect to the refbox at the IP address and port given in the contructor.
     * Throws an UnknownHostException if the hostname defined in the contructor is not known.
     * Throws an IOException if the connection cannot be established.
     *
     * @throws IOException Signals that the connection to the refbox cannot be established.
     */
    public void connect() throws InterruptedException, IOException, UnresolvedAddressException {
        con = new ConThread();
        con.start();
        try {
            con.join(); //Wait for Connection Thread to succeed
            if (con.getException() == null) { //there was no Exception

                //Initialize Queues
                queue1 = new LinkedList<ProtobufMessage>();
                queue2 = new LinkedList<ProtobufMessage>();
                act_q = queue1;
                send_q = queue2;

                //Start Send and Receive Threads
                send = new SendThread();
                send.start();
                recv = new RecvThread();
                recv.start();

            } else {
                if (con.getException() instanceof UnknownHostException) {
                    throw new UnknownHostException("Don't know about host " + hostname);
                } else if (con.getException() instanceof IOException) {
                    throw new IOException("Couldn't get I/O for the connection to " + hostname);
                } else if (con.getException() instanceof UnresolvedAddressException) {
                    throw new UnresolvedAddressException();
                }
            }
        } catch (InterruptedException e) {
            throw e;
        }

        is_connected = true;
    }

    /**
     * Disconnects from the refbox.
     */
    public void disconnect(boolean wait) {
        try {
            if (send != null) {
                send.terminate();
            }
            if (recv != null) {
                recv.terminate();
            }

            if (wait) {
                try {
                    if (send != null) {
                        send.join();
                    }
                    if (recv != null) {
                        recv.join();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (sockchan != null) {
                sockchan.close();
                sockchan = null;
            }
            //sockchan.close();

            is_connected = false;
        } catch (IOException e) {
        }
    }

    /**
     * Checks if a connection to the refbox is established.
     *
     * @return true, if connection is established.
     */
    public boolean is_connected() {
        return is_connected;
    }

    /**
     * Registers a new ProtobufMessageHandler responsible for handling and deserializing incoming
     * protobuf messages. Required if you want to access received messages. Only allows one registered
     * handler at the same time.
     *
     * @param handler the ProtobufMessageHandler
     * @see ProtobufMessageHandler
     */
    public void register_handler(ProtobufMessageHandler handler) {
        this.handler = handler;
    }

    /**
     * Adds and registers a new protobuf message type. This is required to instantiate the correct
     * protobuf message object when a message is received from the refbox. For example, if you want
     * the client to be able to receive and process a GameState message, call
     * client.&ltGameState&gtadd_message(GameState.class).
     *
     * @param <T> the type of the protobuf message to register, has to extend from GeneratedMessage
     * @param c   the class object of the same protobuf message
     */
    @SuppressWarnings("unchecked")
    public <T extends GeneratedMessageV3> void add_message(Class<T> c) {
        try {
            Method m = c.getMethod("getDefaultInstance", (Class<?>[]) null);
            T msg = (T) m.invoke((Object[]) null, (Object[]) null);
            EnumDescriptor desc = msg.getDescriptorForType().findEnumTypeByName("CompType");

            int cmp_id = desc.findValueByName("COMP_ID").getNumber();
            int msg_id = desc.findValueByName("MSG_TYPE").getNumber();
            Key key = new Key(cmp_id, msg_id);
            msgs.put(key, msg);
            classNameToKey.put(c.getName(), key);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void handle_message(int cmp_id, int msg_id, ByteBuffer in_msg) {
        if (handler != null) {
            for (Map.Entry<Key, GeneratedMessageV3> e : msgs.entrySet()) {
                Key key = e.getKey();
                if (key.cmp_id == cmp_id && key.msg_id == msg_id) {
                    handler.handle_message(in_msg, e.getValue());
                    break;
                }
            }
        }
    }


    /**
     * Puts a ProtobufMessage into the send queue in order to be sent out to the refbox.
     *
     * @param msg the ProtobufMessage to send
     * @see ProtobufMessage
     */
    @Override
    public void enqueue(ProtobufMessage msg) {
        synchronized (act_q) {
            act_q.add(msg);
            try {
                act_q.notifyAll();
            } catch (IllegalMonitorStateException e) {
            }
        }
    }

    @Override
    public void enqueue(GeneratedMessageV3 msg) {
        if (!classNameToKey.containsKey(msg.getClass().getName())) {
            throw new RuntimeException("classNameToKey does not container entry for: " + msg.getClass().getName());
        }
        Key key = classNameToKey.get(msg.getClass().getName());
        this.enqueue(new ProtobufMessage(key.cmp_id, key.msg_id, msg));
    }

    public <T> void enqueue(GeneratedMessageV3 msg, Class<T> c) {
        Method m = null;
        try {
            m = c.getMethod("getDefaultInstance", (Class<?>[]) null);
            Descriptors.EnumDescriptor desc = msg.getDescriptorForType().findEnumTypeByName("CompType");

            int cmp_id = desc.findValueByName("COMP_ID").getNumber();
            int msg_id = desc.findValueByName("MSG_TYPE").getNumber();
            this.enqueue(new ProtobufMessage(cmp_id, msg_id, msg));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private class ConThread extends Thread {

        private Exception e = null;

        public void run() {
            try {
                sockchan = SocketChannel.open();
                sockchan.connect(new InetSocketAddress(hostname, port));
                sockchan.finishConnect();
            } catch (UnknownHostException e) {
                this.e = e;
            } catch (IOException e) {
                this.e = e;
            } catch (UnresolvedAddressException e) {
                this.e = e;
            }
        }

        public Exception getException() {
            return this.e;
        }

    }

    private class SendThread extends Thread {

        private boolean run = true;

        public void run() {
            while (run) {
                synchronized (act_q) {
                    if (act_q.isEmpty()) {
                        try {
                            act_q.wait();
                            Queue<ProtobufMessage> help_q = send_q;
                            send_q = act_q;
                            act_q = help_q;
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                synchronized (send_q) {
                    try {
                        while (!send_q.isEmpty()) {
                            ProtobufMessage msg = send_q.remove();
                            if (sockchan != null && sockchan.isConnected()) {
                                sockchan.write(msg.serialize(false, null));
                            } else {
                                throw new IOException();
                            }
                        }
                    } catch (IOException e) {
                        run = false;
                        disconnect(true);
                        handler.connection_lost(e);
                    }
                }
            }
        }

        public void terminate() {
            this.run = false;
            this.interrupt();
            synchronized (act_q) {
                act_q.notifyAll();
            }
        }

    }

    private class RecvThread extends Thread {

        private boolean run = true;

        public void run() {
            while (run) {
                try {
                    //read headers
                    ByteBuffer in_header = ByteBuffer.allocate(ProtobufMessage.FRAME_HEADER_SIZE + ProtobufMessage.MESSAGE_HEADER_SIZE);
                    if (sockchan != null && sockchan.isConnected()) {
                        int read = sockchan.read(in_header);
                        if (read == -1) {
                            throw new IOException();
                        }
                    } else {
                        throw new IOException();
                    }
                    in_header.order(ByteOrder.BIG_ENDIAN);
                    in_header.rewind();

                    ProtobufFrameHeader frameHeader = readHeader(in_header);

                    int protocolVersion = frameHeader.getProtocolVersion();
                    if (protocolVersion != 2) {
                        throw new UnknownProtocolVersionException("Protocol version " + protocolVersion + " does not exist and cannot be processed.");
                    }

                    int cipher = frameHeader.getCipher();
                    if (cipher != 0) {
                        throw new EncryptedStreamMessageException("Encryption in stream messages is not allowed.");
                    }

                    //read payload
                    int cid = in_header.getShort();
                    int msgid = in_header.getShort();

                    int size = frameHeader.getPayloadSize();

                    if (size < 0 || size > 1000000) {
                        continue;
                    }

                    ByteBuffer in_msg = ByteBuffer.allocate(size - ProtobufMessage.MESSAGE_HEADER_SIZE);
                    while (in_msg.remaining() != 0) {
                        if (sockchan != null && sockchan.isConnected()) {
                            int read = sockchan.read(in_msg);
                            if (read == -1) {
                                throw new IOException();
                            }
                        } else {
                            throw new IOException();
                        }
                    }

                    handle_message(cid, msgid, (ByteBuffer) in_msg.rewind());
                } catch (IOException e) {
                    run = false;
                    disconnect(true);
                    handler.connection_lost(e);
                }
            }
        }

        public void terminate() {
            this.run = false;
        }

        private ProtobufFrameHeader readHeader(ByteBuffer header) {
            ProtobufFrameHeader frameHeader = new ProtobufFrameHeader();

            frameHeader.setProtocolVersion((int) header.get());
            frameHeader.setCipher((int) header.get());
            frameHeader.setReserved1((int) header.get());
            frameHeader.setReserved2((int) header.get());

            frameHeader.setPayloadSize(header.getInt());

            return frameHeader;
        }

    }

}
