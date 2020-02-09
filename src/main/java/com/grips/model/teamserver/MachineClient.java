package com.grips.model.teamserver;

import com.google.protobuf.GeneratedMessage;
import com.grips.protobuf_lib.RobotMessageRegister;
import lombok.extern.java.Log;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.*;
import org.robocup_logistics.llsf_utils.Key;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Log
public class MachineClient {
    private final MachineClientUtils.TeamColor teamColor;
    private final Map<MachineClientUtils.Machine, GeneratedMessage> sendQueue;
    private final Map<MachineClientUtils.Machine, MachineClientUtils.MachineState> machineStatuse;

    public MachineClient (MachineClientUtils.TeamColor teamColor)
    {
        this.teamColor = teamColor;
        this.sendQueue = new ConcurrentHashMap<>();
        this.machineStatuse = new ConcurrentHashMap<>();
    }

    public void sendResetMachine(MachineClientUtils.Machine machine)
    {
        MachineInstructionProtos.ResetMachine reset = MachineInstructionProtos.ResetMachine.newBuilder()
                .setMachine(machineNameForMsg(machine, teamColor))
                .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                .build();
        addMessageToSendQueue(machine, reset);
    }

    public void sendPrepareBS(MachineClientUtils.MachineSide side, MachineClientUtils.BaseColor base_color)
    {
        MachineClientUtils.Machine machine = MachineClientUtils.Machine.BS;
        MachineInstructionProtos.PrepareInstructionBS bsInstruction =
                MachineInstructionProtos.PrepareInstructionBS.newBuilder()
                        //.setSide(task.getSide() == SubProductionTask.MachineSide.INPUT ? MachineDescriptionProtos.MachineSide.INPUT : MachineDescriptionProtos.MachineSide.OUTPUT)
                        .setSide(side == MachineClientUtils.MachineSide.Input ? MachineInstructionProtos.MachineSide.INPUT : MachineInstructionProtos.MachineSide.OUTPUT)
                        .setColor(ProductColorProtos.BaseColor.valueOf(base_color.toString()))
                        .build();
        MachineInstructionProtos.PrepareMachine prepareMachineMsg =
                MachineInstructionProtos.PrepareMachine.newBuilder()
                        .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                        .setMachine(machineNameForMsg(machine, teamColor))
                        .setInstructionBs(bsInstruction)
                        .build();

        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareDS(int gate, int orderId) {
        MachineClientUtils.Machine machine = MachineClientUtils.Machine.DS;
        MachineInstructionProtos.PrepareInstructionDS dsInstruction =
                MachineInstructionProtos.PrepareInstructionDS.newBuilder()
                        .setGate(gate)
                        .setOrderId(orderId)
                        .build();
        MachineInstructionProtos.PrepareMachine prepareMachineMsg =
                MachineInstructionProtos.PrepareMachine.newBuilder()
                        .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                        .setMachine(machineNameForMsg(machine, teamColor))
                        .setInstructionDs(dsInstruction)
                        .build();

        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareRS(MachineClientUtils.Machine machine, MachineClientUtils.RingColor color) {
        ProductColorProtos.RingColor refbox_color = ProductColorProtos.RingColor.valueOf(color.ordinal()+1);
        MachineInstructionProtos.PrepareInstructionRS rsInstruction =
                MachineInstructionProtos.PrepareInstructionRS.newBuilder()
                        .setRingColor(refbox_color)
                        .build();

        MachineInstructionProtos.PrepareMachine prepareMachineMsg =
                MachineInstructionProtos.PrepareMachine.newBuilder()
                        .setTeamColor(TeamProtos.Team.valueOf(color.toString()))
                        .setMachine(machineNameForMsg(machine, teamColor))
                        .setInstructionRs(rsInstruction)
                        .build();

        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareCS(MachineClientUtils.Machine machine, MachineClientUtils.CSOp operation) {
        MachineDescriptionProtos.CSOp refbox_operation = MachineDescriptionProtos.CSOp.valueOf(operation.ordinal() + 1);
        MachineInstructionProtos.PrepareInstructionCS csInstruction =
                MachineInstructionProtos.PrepareInstructionCS.newBuilder()
                        .setOperation(refbox_operation)
                        .build();

        MachineInstructionProtos.PrepareMachine prepareMachineMsg =
                MachineInstructionProtos.PrepareMachine.newBuilder()
                        .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                        .setMachine(machineNameForMsg(machine, teamColor))
                        .setInstructionCs(csInstruction)
                        .build();

        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareSS(MachineClientUtils.Machine machine, MachineInstructionProtos.SSTask ssTask) {

        MachineInstructionProtos.PrepareInstructionSS ssInstruction = MachineInstructionProtos.PrepareInstructionSS.newBuilder()
                .setTask(ssTask)
                .build();

        MachineInstructionProtos.PrepareMachine prepareMachineMsg = MachineInstructionProtos.PrepareMachine.newBuilder()
                .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                .setMachine(machineNameForMsg(machine, teamColor))
                .setInstructionSs(ssInstruction)
                .build();

        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void stopMessageForMachine(MachineClientUtils.Machine machine) {
        sendQueue.remove(machine);
    }

    public List<ProtobufMessage> fetchPrepareMessages() {
        return fetchMsgForType(MachineInstructionProtos.PrepareMachine.class);
    }

    public List<ProtobufMessage> fetchResetMessages() {
        return fetchMsgForType(MachineInstructionProtos.ResetMachine.class);
    }

    public Set<MachineClientUtils.Machine> fetchMachinesPreparing() {
        return fetchMachinesForType(MachineInstructionProtos.PrepareMachine.class);
    }

    public Set<MachineClientUtils.Machine> fetchMachinesResetting() {
        return fetchMachinesForType(MachineInstructionProtos.ResetMachine.class);
    }

    public void update(MachineInfoProtos.MachineInfo info) {
        this.updateMachineState(info);
        this.updateMessages();
    }

    private void updateMachineState(MachineInfoProtos.MachineInfo info) {
        if (info.hasTeamColor()) {
            if (TeamProtos.Team.CYAN.equals(info.getTeamColor()) && this.teamColor.equals(MachineClientUtils.TeamColor.CYAN)) {
                info.getMachinesList().forEach(this::updateMachineStatus);
            } else if (TeamProtos.Team.MAGENTA.equals(info.getTeamColor()) && this.teamColor.equals(MachineClientUtils.TeamColor.MAGENTA)) {
                info.getMachinesList().forEach(this::updateMachineStatus);
            }
        } else {
            log.warning("MachineInfo without team color received");
        }
    }

    //todo check if remove conditions are correct!.
    private void updateMessages() {
        fetchMachinesPreparing().stream()
                .filter(machineStatuse::containsKey)
                .filter(sendQueue::containsKey)
                .forEach(x -> {
                    if (!machineStatuse.get(x).equals(MachineClientUtils.MachineState.IDLE)) {
                        sendQueue.remove(x);
                    }
                });
        fetchMachinesResetting().stream()
                .filter(machineStatuse::containsKey)
                .filter(sendQueue::containsKey)
                .forEach(x -> {
                    if (machineStatuse.get(x).equals(MachineClientUtils.MachineState.DOWN)) {
                        sendQueue.remove(x);
                    }
                });
    }


    private void updateMachineStatus(MachineInfoProtos.Machine machineInfo) {
        MachineClientUtils.Machine machine = MachineClientUtils.parseMachineWithColor(machineInfo.getName());
        MachineClientUtils.MachineState state = MachineClientUtils.parseMachineState(machineInfo.getState());
        machineStatuse.put(machine, state);
    }

    private String machineNameForMsg(MachineClientUtils.Machine machine, MachineClientUtils.TeamColor color) {
        StringBuilder returner = new StringBuilder();
        switch (color) {
            case CYAN:
                returner.append("C-");
                break;
            case MAGENTA:
                returner.append("M-");
        }
        returner.append(machine.toString());
        return returner.toString();
    }

    private List<ProtobufMessage> fetchMsgForType(Class<? extends GeneratedMessage> clazz) {
        Key key = RobotMessageRegister.getInstance().get_msg_key_from_class(clazz);
        return this.sendQueue.values().stream()
                .filter(x -> x.getClass().equals(clazz))
                .map(x -> new ProtobufMessage(key.cmp_id, key.msg_id, x))
                .collect(Collectors.toList());
    }

    private Set<MachineClientUtils.Machine> fetchMachinesForType(Class<? extends GeneratedMessage> clazz) {
        Set<MachineClientUtils.Machine> returner = new HashSet<>();
        this.sendQueue.forEach((machine, msg) -> {
            if (msg.getClass().equals(clazz)) {
                returner.add(machine);
            }
        });
        return returner;
    }

    private void addMessageToSendQueue(MachineClientUtils.Machine machine, GeneratedMessage msg)
    {
        sendQueue.put(machine, msg);
    }

}
