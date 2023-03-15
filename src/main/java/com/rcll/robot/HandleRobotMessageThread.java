package com.rcll.robot;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rcll.domain.Peer;
import com.rcll.protobuf_lib.RobotConnections;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_msgs.AgentTasksProtos;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos;

import java.net.Socket;
import java.util.AbstractMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@CommonsLog
public class HandleRobotMessageThread extends Thread {
    private final RobotConnections robotConnections;

    private final Consumer<Integer> robotAddedHandler;
    private final Consumer<BeaconSignalProtos.BeaconSignal> beaconMsgHandler;
    private final Consumer<AgentTasksProtos.AgentTask> prsTaskMsgHandler;

    private final Predicate<AbstractMap.SimpleEntry<GeneratedMessageV3, byte[]>> unknownMsgHandler;
    private final Socket socket;

    private AbstractMap.SimpleEntry<GeneratedMessageV3, byte[]> msg;

    public HandleRobotMessageThread(Socket socket,
                                    RobotConnections robotConnections,
                                    Consumer<Integer> robotAddedHandler,
                                    Consumer<BeaconSignalProtos.BeaconSignal> beaconMsgHandler,
                                    Consumer<AgentTasksProtos.AgentTask> prsTaskMsgHandler,
                                    Predicate<AbstractMap.SimpleEntry<GeneratedMessageV3, byte[]>> unknownMsgHandler,
                                    AbstractMap.SimpleEntry<GeneratedMessageV3, byte[]> msg) {
        this.socket = socket;
        this.robotConnections = robotConnections;
        this.robotAddedHandler = robotAddedHandler;
        this.beaconMsgHandler = beaconMsgHandler;
        this.prsTaskMsgHandler = prsTaskMsgHandler;
        this.unknownMsgHandler = unknownMsgHandler;
        this.msg = msg;
    }

    @Override
    public void run() {
        handleMsg(this.msg);
    }

    protected void handleMsg(AbstractMap.SimpleEntry<GeneratedMessageV3, byte[]> msg) {
        try {
            if (msg.getKey() instanceof BeaconSignalProtos.BeaconSignal) {
                BeaconSignalProtos.BeaconSignal beaconSignal = BeaconSignalProtos.BeaconSignal.parseFrom(msg.getValue());
                updateRobotNetworkActivity(beaconSignal);
                beaconMsgHandler.accept(beaconSignal);
            } else if (msg.getKey() instanceof AgentTasksProtos.AgentTask) {
                prsTaskMsgHandler.accept(AgentTasksProtos.AgentTask.parseFrom(msg.getValue()));
            } else {
                if (!unknownMsgHandler.test(msg)) {
                    log.warn("Unknown message in RobotHandler! " + msg.getKey().getClass().getSimpleName());
                }
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Can't parse msg in RobotHandler!", e);
        }
    }

    private void updateRobotNetworkActivity(BeaconSignalProtos.BeaconSignal beaconSignal) {
        if (!robotConnections.isRobotConnected(beaconSignal.getNumber())) {
            Peer robot = new Peer();
            robot.setId(beaconSignal.getNumber());
            robot.setLastActive(System.currentTimeMillis());
            robot.setConnection(socket);
            robotConnections.addRobot(robot);
            this.robotAddedHandler.accept(beaconSignal.getNumber());
        } else {
            robotConnections.getRobot(beaconSignal.getNumber()).setLastActive(System.currentTimeMillis());
        }
    }

}
