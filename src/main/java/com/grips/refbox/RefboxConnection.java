package com.grips.refbox;

import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_comm.ProtobufBroadcastPeer;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_comm.ProtobufMessageHandler;

import java.io.IOException;

@CommonsLog
public class RefboxConnection {
    private final ProtobufMessageHandler handler;
    private ProtobufBroadcastPeer peer;

    public RefboxConnection(String ip, int sendPort, int receivePort,
                            ProtobufMessageHandler handler) {
        this.handler = handler;
        peer = new ProtobufBroadcastPeer(ip, sendPort, receivePort);
    }

    public void start() throws IOException {
        log.info("Starting Refbox Connection....");
        peer.start();
    }

    public <T extends GeneratedMessageV3> void add_message(Class<T> classType){
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
