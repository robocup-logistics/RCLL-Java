package com.grips.refbox;

import com.google.protobuf.GeneratedMessageV3;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_comm.ProtobufUpdBroadcastConnection;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_comm.ProtobufMessageHandler;

import java.io.IOException;

@CommonsLog
@Getter
public class RefboxConnection {
    private ProtobufUpdBroadcastConnection peer;

    public RefboxConnection(String ip, int sendPort, int receivePort,
                            ProtobufMessageHandler handler, boolean encrypt, int cipher_type, String cryptoKey) {
        log.info("Creating broadcast peer ip: " + ip + " sendPort: " + sendPort + " receivePort: " + receivePort);
        peer = new ProtobufUpdBroadcastConnection(ip, sendPort, receivePort, encrypt, cipher_type, cryptoKey);
        peer.register_handler(handler);
    }

    public RefboxConnection(String ip, int sendPort, int receivePort,
                            ProtobufMessageHandler handler) {
        this(ip, sendPort, receivePort, handler, false, -1, null);
    }

    public void start(String threadName) throws IOException {
        log.info("Starting Refbox Connection....");
        peer.start(threadName);
    }

    public <T extends GeneratedMessageV3> void add_message(Class<T> classType) {
        log.info("Adding message: " + classType.getName());
        this.peer.add_message(classType);
    }

    public void enqueue(ProtobufMessage msg) {
        this.peer.enqueue(msg);
    }

    public void enqueue(GeneratedMessageV3 msg) {
        this.peer.enqueue(msg);
    }
}
