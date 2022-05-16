package com.grips.refbox;

import com.grips.model.teamserver.TeamColor;
import com.grips.protobuf_lib.RobotMessageRegister;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos;
import org.robocup_logistics.llsf_msgs.Pose2DProtos;
import org.robocup_logistics.llsf_msgs.TeamProtos;
import org.robocup_logistics.llsf_utils.Key;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class RobotClient {
    private final Map<Integer, BeaconSignalProtos.BeaconSignal> sendQueue;
    private final TeamColor team;

    public RobotClient(TeamColor team) {
        this.team = team;
        this.sendQueue = new ConcurrentHashMap<>();
    }
    public void sendRobotBeaconMsg(int robotNumber, String robotName,
                                   float x, float y, float yaw) {
        BeaconSignalProtos.BeaconSignal bs = BeaconSignalProtos.BeaconSignal.newBuilder()
                .setNumber(robotNumber)
                .setTeamColor(TeamProtos.Team.valueOf(team.toString()))
                .setPeerName(robotName)
                .setPose(Pose2DProtos.Pose2D.newBuilder()
                        .setX(x)
                        .setY(y)
                        .setOri(yaw)
                        .build())
                .build();
        this.sendQueue.put(bs.getNumber(), bs);
    }

    public List<ProtobufMessage> fetchBeaconSignals() {
        Key key = RobotMessageRegister.getInstance().get_msg_key_from_class(BeaconSignalProtos.BeaconSignal.class);
        return this.sendQueue.values().stream()
                .map(x -> new ProtobufMessage(key.cmp_id, key.msg_id, x))
                .collect(Collectors.toList());
    }

    public void clearBeaconSignals() {
        this.sendQueue.clear();
    }
}
