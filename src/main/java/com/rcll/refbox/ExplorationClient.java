package com.rcll.refbox;

import com.rcll.domain.MachineName;
import com.rcll.domain.TeamColor;
import com.rcll.domain.ZoneName;
import com.rcll.protobuf_lib.RobotMessageRegister;
import lombok.extern.log4j.Log4j2;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.MachineReportProtos;
import org.robocup_logistics.llsf_msgs.TeamProtos;
import org.robocup_logistics.llsf_utils.Key;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
class ExplorationClient {
    private final Map<MachineName, MachineReportProtos.MachineReportEntry> sendQueue;
    private final TeamColor team;

    public ExplorationClient(TeamColor team) {
        this.team = team;
        this.sendQueue = new ConcurrentHashMap<>();
    }
    public void sendExploreMachine(MachineName machineName, ZoneName zone, int rotation) {
        MachineReportProtos.MachineReportEntry msg = MachineReportProtos.MachineReportEntry.newBuilder()
                .setName(machineName.toString())
                .setZone(zone.toProto())
                .setRotation(rotation)
                .build();
        this.sendQueue.put(machineName, msg);
    }

    public List<ProtobufMessage> fetchExplorationMsg() {
        if (this.sendQueue.isEmpty()) {
            return Collections.emptyList();
        }
        MachineReportProtos.MachineReport msg = MachineReportProtos.MachineReport.newBuilder()
                .addAllMachines(this.sendQueue.values())
                .setTeamColor(TeamProtos.Team.valueOf(team.toString())).build();
        log.debug("ExplorationMessages: " + msg.toString());
        Key key = RobotMessageRegister.getInstance().get_msg_key_from_class(MachineReportProtos.MachineReport.class);
        return Collections.singletonList(new ProtobufMessage(key.cmp_id, key.msg_id, msg));
    }

    public void clearExploraionMsgs() {
        this.sendQueue.clear();
    }
}
