package com.rcll.robot;

import com.google.protobuf.GeneratedMessageV3;
import com.rcll.domain.MachineName;
import com.rcll.domain.MachineSide;
import com.rcll.protobuf_lib.ProtobufServer;
import com.rcll.protobuf_lib.RobotConnections;
import com.rcll.protobuf_lib.RobotMessageRegister;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_msgs.*;

@CommonsLog
public class RobotClient {

    private boolean robotsStopped;

    private final ProtobufServer protobufServer;
    private final RobotTaskCreator robotTaskCreator;
    private final RobotConnections robotConnections;

    public RobotClient(ProtobufServer protobufServer,
                       RobotTaskCreator robotTaskCreator,
                       RobotConnections robotConnections) {
        this.protobufServer = protobufServer;
        this.robotTaskCreator = robotTaskCreator;
        this.robotConnections = robotConnections;
        RobotMessageRegister.getInstance().add_message(BeaconSignalProtos.BeaconSignal.class);
        RobotMessageRegister.getInstance().add_message(AgentTasksProtos.AgentTask.class);
        new Thread(protobufServer).start();
        this.robotsStopped = false;
    }

    public boolean isRobotsStopped() {
        return this.robotsStopped;
    }

    public void cancelTask(int robotId) {
        log.info("Canceling current task of robot: " + robotId);
        AgentTasksProtos.AgentTask prsTask = robotTaskCreator.createCancelTask((long) robotId);
        sendPrsTaskToRobot(prsTask);
    }

    public void sendDummyTaskToRobot(@NonNull Long robotId,
                                     @NonNull String machine,
                                     MachineSide side) {
        AgentTasksProtos.AgentTask prsTask = robotTaskCreator.createDummyTask(robotId,
                machine,
                side);
        sendPrsTaskToRobot(prsTask);
    }

    public void sendDeliverTaskToRobot(@NonNull Long robotId,
                                       @NonNull Long taskId,
                                       @NonNull String machine,
                                       @NonNull MachineSide side,
                                       Integer machineCount) {
        AgentTasksProtos.AgentTask deliverPrsTask = robotTaskCreator.createDeliverWorkPieceTask(
                robotId,
                taskId,
                machine,
                side,
                convertSideToShelfSlide(side, machineCount));
        sendPrsTaskToRobot(deliverPrsTask);
    }

    public void sendWaitingTaskToRobot(@NonNull Long robotId,
                                       @NonNull String zone) {
        AgentTasksProtos.AgentTask prsTask = robotTaskCreator.createWaitingTask(robotId, zone);
        sendPrsTaskToRobot(prsTask);
    }

    public void sendGetTaskToRobot(@NonNull Long robotId,
                                   @NonNull Long taskId,
                                   @NonNull String machine,
                                   MachineSide side,
                                   Integer machineCount) {
        AgentTasksProtos.AgentTask getPrsTask = robotTaskCreator.createGetWorkPieceTask(
                robotId,
                taskId,
                machine,
                side,
                convertSideToShelfSlide(side, machineCount));
        sendPrsTaskToRobot(getPrsTask);
    }

    private String convertSideToShelfSlide(MachineSide side, Integer materialCount) {
        switch (side) {
            case Input:
                return "input";
            case Output:
                return "output";
            case Shelf:
                return "shelf" + (materialCount);
            case Slide:
                return "slide";
            default:
                throw new IllegalArgumentException("Invalid machineSide: " + side);
        }
    }

    public void stopAllRobots() {
        this.robotsStopped = true;
        robotConnections.getclientId().forEach(this::stopRobot);
    }

    public void startAllRobots() {
        this.robotsStopped = false;
        robotConnections.getclientId().forEach(this::startRobot);
    }

    private void startRobot(Long robotId) {
        sendPrsTaskToRobot(robotTaskCreator.createStopTask(robotId, false));
    }

    private void stopRobot(Long robotId) {
        sendPrsTaskToRobot(robotTaskCreator.createStopTask(robotId, true));
    }

    public <T extends GeneratedMessageV3> void sendToRobot(long robot_id, @NonNull T msg) {
        protobufServer.send_to_robot(robot_id, msg);
    }

    public void sendPrsTaskToRobot(AgentTasksProtos.AgentTask task) {
        log.info("Sending Task: " + task.getTaskId() + " to robot: " + task.getRobotId() + " - " + task.toString());
        try {
            robotConnections.getRobot(task.getRobotId()).setTimeLastTaskAssignment(System.currentTimeMillis());
            this.sendToRobot(task.getRobotId(), task);
        } catch (Exception e) {
            log.warn("robot not found, retrying after 0.3 seconds");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            sendPrsTaskToRobot(task);
        }
    }

    public void sendBufferCap(Long robotId, Long taskId, MachineName machine, Integer shelf) {
        AgentTasksProtos.AgentTask getPrsTask = robotTaskCreator.createBufferCapTask(
                robotId,
                taskId.intValue(),
                machine,
                shelf);
        sendPrsTaskToRobot(getPrsTask);
    }

    public void sendMoveTask(Long robotId, Long taskId, MachineName machineName, MachineSide side) {
        AgentTasksProtos.AgentTask getPrsTask = robotTaskCreator.createMoveToMachineTask(robotId, taskId.intValue(),
                machineName, side);
        sendPrsTaskToRobot(getPrsTask);
    }

    public void sendMoveToZoneTask(Long robotId, Long taskId, String zone) {
        AgentTasksProtos.AgentTask getPrsTask = robotTaskCreator.createMoveToWaypointTask(robotId, taskId.intValue(), zone);
        sendPrsTaskToRobot(getPrsTask);
    }
}

