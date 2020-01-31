package com.grips.model.teamserver;

import com.google.protobuf.GeneratedMessage;
import org.robocup_logistics.llsf_msgs.MachineDescriptionProtos;
import org.robocup_logistics.llsf_msgs.MachineInstructionProtos;
import org.robocup_logistics.llsf_msgs.ProductColorProtos;
import org.robocup_logistics.llsf_msgs.TeamProtos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MachineClient {

    private MachineClientUtils.TeamColor color;
    private Map<MachineClientUtils.Machine, GeneratedMessage> sendQueue;

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
}
