package com.grips.model.teamserver;

import com.google.protobuf.GeneratedMessage;
import com.grips.protobuf_lib.RobotMessageRegister;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.MachineDescriptionProtos;
import org.robocup_logistics.llsf_msgs.MachineInstructionProtos;
import org.robocup_logistics.llsf_msgs.ProductColorProtos;
import org.robocup_logistics.llsf_msgs.TeamProtos;
import org.robocup_logistics.llsf_utils.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MachineClient {

    private final MachineClientUtils.TeamColor color;
    private final Map<MachineClientUtils.Machine, GeneratedMessage> sendQueue;

    public MachineClient (MachineClientUtils.TeamColor color)
    {
        this.color = color;
        this.sendQueue = new ConcurrentHashMap<>();
    }

    private void addMessageToSendQueue(MachineClientUtils.Machine machine, GeneratedMessage msg)
    {
        sendQueue.put(machine, msg);
    }

    public void sendResetMachine(MachineClientUtils.Machine machine)
    {
        MachineInstructionProtos.ResetMachine reset = MachineInstructionProtos.ResetMachine.newBuilder()
                .setMachine(machine.toString())
                .setTeamColor(TeamProtos.Team.valueOf(color.toString()))
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
                        .setTeamColor(TeamProtos.Team.valueOf(color.toString()))
                        .setMachine(machine.toString())
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
                        .setTeamColor(TeamProtos.Team.valueOf(color.toString()))
                        .setMachine(machine.toString())
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
                        .setMachine(machine.toString())
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
                        .setTeamColor(TeamProtos.Team.valueOf(color.toString()))
                        .setMachine(machine.toString())
                        .setInstructionCs(csInstruction)
                        .build();

        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareSS(MachineClientUtils.Machine machine, MachineInstructionProtos.SSTask ssTask) {

        MachineInstructionProtos.PrepareInstructionSS ssInstruction = MachineInstructionProtos.PrepareInstructionSS.newBuilder()
                .setTask(ssTask)
                .build();

        MachineInstructionProtos.PrepareMachine prepareMachineMsg = MachineInstructionProtos.PrepareMachine.newBuilder()
                .setTeamColor(TeamProtos.Team.valueOf(color.toString()))
                .setMachine(machine.toString())
                .setInstructionSs(ssInstruction)
                .build();

        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public List<ProtobufMessage> fetchPrepareMessages() {
        return fetchForType(MachineInstructionProtos.PrepareMachine.class);
    }

    public List<ProtobufMessage> fetchResetMessages() {
        return fetchForType(MachineInstructionProtos.PrepareMachine.class);
    }

    private List<ProtobufMessage> fetchForType(Class<? extends GeneratedMessage> clazz) {
        Key key = RobotMessageRegister.getInstance().get_msg_key_from_class(clazz);
        return this.sendQueue.values().stream()
                .filter(x -> x.getClass().equals(clazz))
                .map(x -> new ProtobufMessage(key.cmp_id, key.msg_id, x))
                .collect(Collectors.toList());
    }
}
