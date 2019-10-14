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
import com.grips.model.scheduler.SubProductionTask;
import com.grips.model.teamserver.MachineInfoRefBox;
import com.grips.model.teamserver.dao.GameStateDao;
import com.grips.model.teamserver.dao.MachineInfoRefBoxDao;
import org.jetbrains.annotations.Nullable;
import org.robocup_logistics.llsf_msgs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RobotConnectionManager {

    @Autowired
    private ConfigurableApplicationContext ctx;

    @Autowired
    private ResourceManager resourceManager;

    @Autowired
    private GameStateDao gameStateDao;

    @Autowired
    private RobotConnections robotConnections;

    @Autowired
    private MachineInfoRefBoxDao machineInfoRefBoxDao;


    private ProtobufServer _proto_server;
    private int listen_port = Config.ROBOT_LISTEN_PORT;

    private boolean allRobotsStopped;

    public boolean isAllRobotsStopped() {
        return allRobotsStopped;
    }

    public void setAllRobotsStopped(boolean stopped) {
        allRobotsStopped = stopped;
    }

    public RobotConnectionManager() {
        // empty constructor needed for spring
    }

    @PostConstruct
    public void afterConstruction() {
        _proto_server = ctx.getBean(ProtobufServer.class, listen_port);
        //_proto_server = new ProtobufServer(listen_port);

        //RobotMessageRegister.getInstance().add_message(BeaconSignalProtos.BeaconSignal.class);
        RobotMessageRegister.getInstance().add_message(RobotBeaconSignalProtos.RobotBeaconSignal.class);
        //RobotMessageRegister.getInstance().add_message(MachineLightStateUpdateProtos.MachineLightState.class);
        //RobotMessageRegister.getInstance().add_message(MachineInstructionProtos.PrepareMachine.class);
        RobotMessageRegister.getInstance().add_message(ReportAllMachinesProtos.ReportAllMachines.class);
        //RobotMessageRegister.getInstance().add_message(TaskProtos.Task.class);
        RobotMessageRegister.getInstance().add_message(PrsTaskProtos.PrsTask.class);
        RobotMessageRegister.getInstance().add_message(MachineOrientationStateUpdateProtos.MachineOrientationState.class);

        new Thread(_proto_server).start();
    }

    public void cancelTask(int robotId) {
        System.out.println("Cancelling current task of robot " + robotId);
        String teamColorString = gameStateDao.getTeamColor();
        if (!org.apache.commons.lang3.StringUtils.isEmpty(teamColorString)) {
            TeamProtos.Team teamColor = TeamProtos.Team.valueOf(teamColorString);

            if (teamColor != null) {
                PrsTaskProtos.PrsTask prsTask = PrsTaskProtos.PrsTask.newBuilder()
                        .setTaskId(0)
                        .setTeamColor(teamColor)
                        .setRobotId((int) robotId)
                        .setCancelTask(true)
                        .setExecutionResult(PrsTaskProtos.PrsTask.ExecutionResult.FAIL).build();
                send_to_robot((int) robotId, prsTask);
            }
        }
    }

    public <T extends GeneratedMessage> void send_to_robot(long robot_id, @Nullable T msg) {
        _proto_server.send_to_robot(robot_id, msg);
    }

    public void sendAllKnownMachines(int robotId) {
        List<MachineInfoProtos.Machine> machines = new ArrayList<>();
        for (MachineInfoRefBox mirb : machineInfoRefBoxDao.findAll()) {
            MachineInfoProtos.Machine m = MachineInfoProtos.Machine.newBuilder()
                    .setName(mirb.getName())
                    .setZone(ZoneProtos.Zone.valueOf(mirb.getZone()))
                    .setRotation(mirb.getRotation()).build();
            machines.add(m);
        }

        MachineInfoProtos.MachineInfo mi = MachineInfoProtos.MachineInfo.newBuilder().addAllMachines(machines).build();
        try {
            send_to_robot(robotId, mi);
        } catch (NumberFormatException nfe) {
            System.err.println("Beaconsignal does not contain parseable robot id");
        }
    }

    public void sendProductionTaskToRobot(SubProductionTask task) {
        if(task != null) {
            System.out.println("Task " + task.getId() + " sent to Robot " + task.getRobotId());

            if (task.getType().compareTo(SubProductionTask.TaskType.GET) == 0) {
                String providingtype = null;
                if(task.getSide().equals(SubProductionTask.MachineSide.OUTPUT)) {
                    providingtype = "output";
                } else {
                    providingtype = "input";
                }

                String shelfslide = null;

                if (task.getSide().equals(SubProductionTask.MachineSide.SHELF)) {

                    String shelfMachine = task.getMachine() + "_" + SubProductionTask.MachineSide.SHELF.toString();
                    resourceManager.addNewParts(shelfMachine);
                    int count = resourceManager.getMaterialCount(shelfMachine);
                    if (count == 3) {
                        resourceManager.clearAllParts(shelfMachine);
                    }

                    shelfslide = "shelf" + count; // was already decreased when selecting task
                    //shelfslide = "shelf1"; //robot moves to shelf1, then scans shelf for base automatically
                }

                if(task.getSide().equals(SubProductionTask.MachineSide.SLIDE)) {
                    shelfslide = "slide";
                }

                PrsTaskProtos.GetWorkPieceTask getWorkPieceTask= null;
                if(shelfslide != null) {
                    getWorkPieceTask = PrsTaskProtos.GetWorkPieceTask.newBuilder()
                            .setProvidingStation(task.getMachine())
                            .setProvidingType(providingtype)
                            .setShelfslide(shelfslide)
                            .build();
                } else {
                    getWorkPieceTask = PrsTaskProtos.GetWorkPieceTask.newBuilder()
                            .setProvidingStation(task.getMachine())
                            .setProvidingType(providingtype)
                            .build();
                }
                PrsTaskProtos.PrsTask getPrsTask = PrsTaskProtos.PrsTask.newBuilder()
                        .setGetWorkPieceTask(getWorkPieceTask)
                        .setRobotId(task.getRobotId())
                        .setTeamColor(TeamProtos.Team.valueOf(gameStateDao.getTeamColor()))
                        .setTaskId((int) task.getId()).build();
                send_to_robot(task.getRobotId(), getPrsTask);

                System.out.println("Sending task " + task.getId() + " : " + task.getName() +  " with machine " + task.getMachine() + " and side " + providingtype  + " to robot " + task.getRobotId());
            } else if (task.getType().compareTo(SubProductionTask.TaskType.DELIVER) == 0) {

                String providingtype = null;
                if(task.getSide().equals(SubProductionTask.MachineSide.OUTPUT)) {
                    providingtype = "output";
                } else {
                    providingtype = "input";
                }

                String shelfslide = null;

                if (task.getSide().equals(SubProductionTask.MachineSide.SHELF)) {
                    shelfslide = "shelf" + "_" + (resourceManager.getMaterialCount(task.getMachine() + "_" + SubProductionTask.MachineSide.SHELF.toString())+1); // was already decreased when selecting task
                }

                if(task.getSide().equals(SubProductionTask.MachineSide.SLIDE)) {
                    shelfslide = "slide";
                }

                PrsTaskProtos.DeliverWorkPieceTask deliverWorkPieceTask = null;

                if(shelfslide != null) {
                    deliverWorkPieceTask = PrsTaskProtos.DeliverWorkPieceTask.newBuilder()
                            .setDeliveredStation(task.getMachine())
                            .setDeliveredType(providingtype)
                            .setShelfslide(shelfslide)
                            .build();
                } else  {
                    deliverWorkPieceTask = PrsTaskProtos.DeliverWorkPieceTask.newBuilder()
                            .setDeliveredStation(task.getMachine())
                            .setDeliveredType(providingtype)
                            .build();
                }
                PrsTaskProtos.PrsTask deliverPrsTask = PrsTaskProtos.PrsTask.newBuilder()
                        .setDeliverWorkPieceTask(deliverWorkPieceTask)
                        .setRobotId(task.getRobotId())
                        .setTeamColor(TeamProtos.Team.valueOf(gameStateDao.getTeamColor()))
                        .setTaskId((int) task.getId()).build();
                send_to_robot(task.getRobotId(), deliverPrsTask);

                System.out.println("Sending task " + task.getId() + " : " + task.getName() +  " with machine " + task.getMachine() + " and side " + task.getSide().toString()  + " to robot " + task.getRobotId());
            } else if (task.getType().compareTo(SubProductionTask.TaskType.DUMMY) == 0) {
                PrsTaskProtos.MoveToWayPointTask moveToWayPointTask = PrsTaskProtos.MoveToWayPointTask.newBuilder()
                        .setWaypoint(task.getMachine() + task.getSide().toString().toLowerCase() + "_exploration")
                        .build();

                TeamProtos.Team teamColorProto = TeamProtos.Team.valueOf(gameStateDao.getTeamColor());

                PrsTaskProtos.PrsTask prsTask = PrsTaskProtos.PrsTask.newBuilder()
                        .setTaskId(new Long(System.currentTimeMillis()).intValue())
                        .setTeamColor(teamColorProto)
                        .setRobotId(task.getRobotId())
                        .setMoveToWayPointTask(moveToWayPointTask)
                        .setExecutionResult(PrsTaskProtos.PrsTask.ExecutionResult.FAIL).build();

                send_to_robot(task.getRobotId(), prsTask);

                System.out.println("Sending task " + task.getId() + " : " + task.getName() +  " with machine " + task.getMachine() + " and side " + task.getSide().toString()  + " to robot " + task.getRobotId());
            }
        }
    }

    public Set<Long> getRobotIds() {
        return robotConnections.getRobotIds();
    }
}
