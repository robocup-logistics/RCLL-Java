package com.grips.refbox;

import com.google.protobuf.GeneratedMessageV3;
import com.grips.model.teamserver.MachineClientUtils;
import com.grips.model.teamserver.TeamColor;
import com.grips.protobuf_lib.RobotMessageRegister;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos;
import org.robocup_logistics.llsf_msgs.RobotInfoProtos;
import org.robocup_logistics.llsf_utils.Key;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RobotClient {
    private final Map<Integer, BeaconSignalProtos.BeaconSignal> sendQueue;

    public RobotClient() {
        this.sendQueue = new ConcurrentHashMap<>();
    }

    //todo introduce new, more simple model for beacon signal.
    public void sendRobotBeaconMsg(BeaconSignalProtos.BeaconSignal bs) {
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
