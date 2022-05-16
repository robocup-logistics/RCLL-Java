package com.grips.refbox;

import com.grips.model.teamserver.TeamColor;
import com.grips.protobuf_lib.RobotMessageRegister;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos;
import org.robocup_logistics.llsf_msgs.Pose2DProtos;
import org.robocup_logistics.llsf_msgs.TeamProtos;
import org.robocup_logistics.llsf_msgs.TimeProtos;
import org.robocup_logistics.llsf_utils.Key;
import org.robocup_logistics.llsf_utils.NanoSecondsTimestampProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class RobotClient {
    private final Map<Integer, BeaconSignalProtos.BeaconSignal> sendQueue;
    private final TeamColor teamColor;

    private final String teamName;
    private final Map<Integer, Integer> seq;
    public RobotClient(TeamColor teamColor, String teamName) {
        this.teamColor = teamColor;
        this.teamName = teamName;
        this.sendQueue = new ConcurrentHashMap<>();
        this.seq = new HashMap<>();
    }
    public void sendRobotBeaconMsg(int robotNumber, String robotName,
                                   float x, float y, float yaw){
        this.seq.putIfAbsent(robotNumber, 1);
        NanoSecondsTimestampProvider nstp = new NanoSecondsTimestampProvider();
        long ms = System.currentTimeMillis();
        long ns = nstp.currentNanoSecondsTimestamp();
        int sec = (int) (ms / 1000);
        long nsec = ns - (ms * 1000000L);
        TimeProtos.Time t = TimeProtos.Time.newBuilder().setSec(sec).setNsec(nsec).build();

        BeaconSignalProtos.BeaconSignal bs = BeaconSignalProtos.BeaconSignal.newBuilder()
                .setNumber(robotNumber)
                .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                .setPeerName(robotName)
                .setPose(Pose2DProtos.Pose2D.newBuilder()
                        .setX(x)
                        .setY(y)
                        .setOri(yaw)
                        .setTimestamp(t)
                        .build())
                .setTime(t)
                .setSeq(this.seq.get(robotNumber))
                .setTeamName(teamName)
                .build();
        this.sendQueue.put(bs.getNumber(), bs);
        this.seq.put(robotNumber, this.seq.get(robotNumber) + 1);
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
