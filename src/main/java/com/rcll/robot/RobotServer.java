package com.rcll.robot;

import com.rcll.protobuf_lib.ProtobufServer;
import com.rcll.protobuf_lib.RobotConnections;

import java.io.IOException;
import java.util.function.Consumer;

public class RobotServer {
    private final RobotConnections robotConnections;
    private final ProtobufServer protobufServer;
    private final RobotTaskCreator robotTaskCreator;
    private final RobotClient robotClient;
    public RobotServer(int listeningPort, int robotTimeoutInMs, IRobotMessageThreadFactory threadFactory,
                       Consumer<Integer> robotAddedHandler, RobotTaskCreator robotTaskCreator) {
        this.robotConnections = new RobotConnections(robotTimeoutInMs);
        this.robotTaskCreator = robotTaskCreator;
        this.protobufServer = new ProtobufServer(listeningPort, robotConnections, threadFactory, robotAddedHandler);
        this.robotClient = new RobotClient(this.robotTaskCreator, this.robotConnections);
    }

    public void start() throws IOException {
        this.protobufServer.start();
    }
}
