package com.rcll.robot;

import com.google.protobuf.GeneratedMessageV3;

import java.net.Socket;
import java.util.AbstractMap;

public interface IRobotMessageThreadFactory {
    HandleRobotMessageThread create(Socket _socket, AbstractMap.SimpleEntry<GeneratedMessageV3, byte[]> msg);
}
