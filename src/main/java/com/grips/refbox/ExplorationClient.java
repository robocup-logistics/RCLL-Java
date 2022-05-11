package com.grips.refbox;

import com.grips.model.teamserver.TeamColor;
import com.grips.protobuf_lib.RobotMessageRegister;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.BeaconSignalProtos;
import org.robocup_logistics.llsf_msgs.MachineReportProtos;
import org.robocup_logistics.llsf_msgs.TeamProtos;
import org.robocup_logistics.llsf_utils.Key;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ExplorationClient {
    private final Map<Integer, MachineReportProtos.MachineReport> sendQueue;
    private final TeamColor team;

    private MachineReportProtos.MachineReport msg;
    public ExplorationClient(TeamColor team) {
        this.team = team;
        this.sendQueue = new ConcurrentHashMap<>();
        msg = null;
    }

    //todo introduce new, more simple model for explore - by machine name.
    public void sendExploreMachine(List<MachineReportProtos.MachineReportEntry> machineReports) {
        MachineReportProtos.MachineReport msg = MachineReportProtos.MachineReport.newBuilder()
                .addAllMachines(machineReports)
                .setTeamColor(TeamProtos.Team.valueOf(team.toString())).build();
        this.sendQueue.put(1, msg);
    }

    public List<ProtobufMessage> fetchExplorationMsgs() {
        Key key = RobotMessageRegister.getInstance().get_msg_key_from_class(MachineReportProtos.MachineReport.class);
        return this.sendQueue.values().stream()
                .map(x -> new ProtobufMessage(key.cmp_id, key.msg_id, x))
                .collect(Collectors.toList());
    }

    public void clearExploraionMsgs() {
        this.sendQueue.clear();
    }
}
