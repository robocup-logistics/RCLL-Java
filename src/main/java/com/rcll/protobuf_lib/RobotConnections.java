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

package com.rcll.protobuf_lib;

import com.google.protobuf.GeneratedMessageV3;
import com.rcll.domain.Peer;
import com.rcll.domain.Pose;
import com.rcll.domain.RobotBeacon;
import com.rcll.llsf_comm.Key;
import com.rcll.llsf_comm.ProtobufMessage;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@CommonsLog
public class RobotConnections {

    private Collection<Peer> _connectedRobots = new ArrayList<>();
    private Map<Long, RobotBeacon> robotBeaconMap = new ConcurrentHashMap<>();
    private int Timeout_Time;

    public RobotConnections(int mil_sec){
        Timeout_Time = mil_sec;
    }

    public synchronized Set<Long>  getclientId() {
        return _connectedRobots.stream().map(r -> r.getId()).collect(Collectors.toSet());
    }

    public synchronized Socket getConnection(long clientId) {
        Optional<Peer> robot = _connectedRobots.stream().filter(r -> r.getId() == clientId).findFirst();
        if (!robot.isPresent()) {
            System.out.println("Robot with given ID was not found in list");
            return null;
        }

        return robot.get().getConnection();
    }

    public synchronized void addRobot(Peer robot) {
        if (!isRobotConnected(robot.getId())) {
            _connectedRobots.add(robot);
            log.info("Robot with ID added " + robot.getId());
        } else {
            log.info("Robot with this ID was already added!");
        }
    }

    public synchronized void removeRobot(long clientId) {
        Optional<Peer> robot = _connectedRobots.stream().filter(r -> r.getId() == clientId).findFirst();
        if (!robot.isPresent()) {
            log.info("Robot that should be removed was not found in list!");
            return;
        }

        _connectedRobots.remove(robot.get());
    }

    public synchronized boolean isRobotConnected(long clientId) {
        return _connectedRobots.stream().filter(r -> r.getId() == clientId).findAny().isPresent();
    }

    public synchronized Peer getRobot(long clientId) {
        Optional<Peer> robot = _connectedRobots.stream().filter(r -> r.getId() == clientId).findFirst();
        if (!robot.isPresent()) {
            log.info("Robot with ID " + clientId + " you are looking for does not exist");
            return null;
        }

        return robot.get();
    }

    public synchronized void removeLostRobot(Socket socket) {
        Optional<Peer> robot = _connectedRobots.stream().filter(r -> r.getConnection().equals(socket)).findFirst();
        if (!robot.isPresent()) {
            log.info("Robot to whom connection was lost, already was removed from list");
            return;
        } else {
            //Robot r = robot.get();
            //productionScheduler.failRobotTasks(new Long(r.getId()).intValue());

            // do not fail robot tasks, it will be done automatically ones it comes up again
        }
        _connectedRobots.remove(robot.get());
    }

    public synchronized boolean isRobotActive(long clientId) {
        Optional<Peer> robot = _connectedRobots.stream().filter(r -> r.getId() == clientId).findAny();
        if (!robot.isPresent()) {
            log.info("Robot that should be checked for activity was not found");
            return false;
        }

        long lastActive = robot.get().getLastActive();

        if (lastActive + Timeout_Time < System.currentTimeMillis()) {
            return false;
        }

        return true;
    }

    public ArrayList<Peer> getRobots() {
        return new ArrayList<>(_connectedRobots);
    }

    public void send_to_robot(long robot_id, ProtobufMessage msg) {
        if (null == msg) {
            log.error("Error: msg to send is null!");
            return;
        }
        if (null == getConnection(robot_id)) {
            log.error("Error: No socket for robot with id " + robot_id + " stored!");
            return;
        }

        ByteBuffer serialized_msg = msg.serialize(false, null);
        try {
            DataOutputStream data_out = new DataOutputStream(getConnection(robot_id).getOutputStream());
            data_out.write(serialized_msg.array());
            data_out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public <T extends GeneratedMessageV3> void send_to_robot(long robot_id, T msg) {
        if (null == msg) {
            log.error("Error: msg to send is null!");
            return;
        }

        Key key = RobotMessageRegister.getInstance().get_msg_key_from_class(msg.getClass());
        ProtobufMessage proto_msg = new ProtobufMessage(key.cmp_id, key.msg_id, msg);
        send_to_robot(robot_id, proto_msg);
    }

    public RobotBeacon getBeaconForRobot(Long robotId) {
        if (robotBeaconMap.containsKey(robotId)) {
            return robotBeaconMap.get(robotId);
        }
        throw new RuntimeException("Requested beacon data for unkown robot: " + robotId);
    }

    public void updateRobotBeacon(BeaconSignalProtos.BeaconSignal beaconSignal) {
        robotBeaconMap.put((long) beaconSignal.getNumber(),
                new RobotBeacon((long)beaconSignal.getNumber(), (long)beaconSignal.getTask().getTaskId(),
                beaconSignal.getPeerName(), new Pose(beaconSignal.getPose().getX(), beaconSignal.getPose().getY(),
                        beaconSignal.getPose().getOri()), Instant.now()));
    }
}
