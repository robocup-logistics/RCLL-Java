package com.rcll.llsf_comm;

import com.google.protobuf.GeneratedMessageV3;

public interface ProtobufConnection {
    void enqueue(GeneratedMessageV3 msg);
    void enqueue(ProtobufMessage msg);
}
