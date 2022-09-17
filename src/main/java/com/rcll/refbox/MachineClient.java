package com.rcll.refbox;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rcll.domain.MachineClientUtils;
import com.rcll.domain.Ring;
import com.rcll.domain.TeamColor;
import com.rcll.protobuf_lib.RobotMessageRegister;
import lombok.extern.java.Log;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.*;
import org.robocup_logistics.llsf_utils.Key;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.robocup_logistics.llsf_msgs.ProductColorProtos.*;

//todo move to refbox folder!
@Log
class MachineClient {
    private final TeamColor teamColor;
    private final Map<MachineClientUtils.Machine, GeneratedMessageV3> sendQueue;
    private final Map<MachineClientUtils.Machine, MachineClientUtils.MachineState> machineStates;
    private final Map<MachineClientUtils.RingColor, MachineClientUtils.Machine> ringColorToMachine;
    private final Map<MachineClientUtils.RingColor, Integer> ringColorToCost;

    public MachineClient(TeamColor teamColor) {
        this.teamColor = teamColor;
        this.sendQueue = new ConcurrentHashMap<>();
        this.machineStates = new ConcurrentHashMap<>();
        this.ringColorToMachine = new ConcurrentHashMap<>();
        this.ringColorToCost = new ConcurrentHashMap<>();
    }

    public void sendResetMachine(MachineClientUtils.Machine machine) {
        MachineInstructionProtos.ResetMachine reset = MachineInstructionProtos.ResetMachine.newBuilder()
                .setMachine(machineNameForMsg(machine, teamColor))
                .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                .build();
        log.info("Sending ResetMachine: " + reset.toString());
        addMessageToSendQueue(machine, reset);
    }

    public void sendPrepareBS(MachineClientUtils.MachineSide side, MachineClientUtils.BaseColor base_color) {
        MachineClientUtils.Machine machine = MachineClientUtils.Machine.BS;
        MachineInstructionProtos.PrepareInstructionBS bsInstruction =
                MachineInstructionProtos.PrepareInstructionBS.newBuilder()
                        //.setSide(task.getSide() == SubProductionTask.MachineSide.INPUT ? MachineDescriptionProtos.MachineSide.INPUT : MachineDescriptionProtos.MachineSide.OUTPUT)
                        .setSide(side == MachineClientUtils.MachineSide.Input ? MachineInstructionProtos.MachineSide.INPUT : MachineInstructionProtos.MachineSide.OUTPUT)
                        .setColor(BaseColor.valueOf(base_color.toString()))
                        .build();
        MachineInstructionProtos.PrepareMachine prepareMachineMsg =
                MachineInstructionProtos.PrepareMachine.newBuilder()
                        .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                        .setMachine(machineNameForMsg(machine, teamColor))
                        .setInstructionBs(bsInstruction)
                        .build();
        log.info("Sending PrepareBS: " + prepareMachineMsg.toString());
        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareDS(int gate, int orderId) {
        MachineClientUtils.Machine machine = MachineClientUtils.Machine.DS;
        MachineInstructionProtos.PrepareInstructionDS dsInstruction =
                MachineInstructionProtos.PrepareInstructionDS.newBuilder()
                        //.setGate(gate)
                        .setOrderId(orderId)
                        .build();
        MachineInstructionProtos.PrepareMachine prepareMachineMsg =
                MachineInstructionProtos.PrepareMachine.newBuilder()
                        .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                        .setMachine(machineNameForMsg(machine, teamColor))
                        .setInstructionDs(dsInstruction)
                        .build();
        log.info("Sending PrepareDS: " + prepareMachineMsg.toString());
        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareRS(MachineClientUtils.Machine machine, MachineClientUtils.RingColor ringColor) {
        RingColor refbox_color = toRefboxRingColor(ringColor);
        MachineInstructionProtos.PrepareInstructionRS rsInstruction =
                MachineInstructionProtos.PrepareInstructionRS.newBuilder()
                        .setRingColor(refbox_color)
                        .build();

        MachineInstructionProtos.PrepareMachine prepareMachineMsg =
                MachineInstructionProtos.PrepareMachine.newBuilder()
                        .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                        .setMachine(machineNameForMsg(machine, teamColor))
                        .setInstructionRs(rsInstruction)
                        .build();
        log.info("Sending PrepareRS: " + prepareMachineMsg.toString());
        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareCS(MachineClientUtils.Machine machine, MachineClientUtils.CSOp operation) {
        MachineDescriptionProtos.CSOp refbox_operation;
        switch (operation) {
            case RETRIEVE_CAP:
                refbox_operation = MachineDescriptionProtos.CSOp.RETRIEVE_CAP;
                break;
            case MOUNT_CAP:
                refbox_operation = MachineDescriptionProtos.CSOp.MOUNT_CAP;
                break;
            default:
                throw new RuntimeException("Unsupported CS command: " + operation);
        }
        ;
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
        log.info("Sending PrepareCS: " + prepareMachineMsg.toString());
        log.info("ByteStringSize: " + prepareMachineMsg.getSerializedSize());
        log.info("ByteString: " + prepareMachineMsg.toByteString().toStringUtf8());
        try {
            MachineInstructionProtos.PrepareMachine parsed = MachineInstructionProtos.PrepareMachine.parseFrom(prepareMachineMsg.toByteArray());
            log.info("Parsed Sending PrepareCS: " + prepareMachineMsg.toString());
            log.info("Parsed ByteStringSize: " + prepareMachineMsg.getSerializedSize());
            log.info("Parsed ByteString: " + prepareMachineMsg.toByteString().toStringUtf8());
        } catch (InvalidProtocolBufferException e) {
            log.throwing("Error on deserialization: ", "Here1", e);
            e.printStackTrace();
        }
        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareSS(MachineClientUtils.Machine machine, int shelf, int slot) {

        MachineInstructionProtos.PrepareInstructionSS ssInstruction = MachineInstructionProtos.PrepareInstructionSS.newBuilder()
                .setShelf(shelf)
                .setSlot(slot)
                .build();

        MachineInstructionProtos.PrepareMachine prepareMachineMsg = MachineInstructionProtos.PrepareMachine.newBuilder()
                .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                .setMachine(machineNameForMsg(machine, teamColor))
                .setInstructionSs(ssInstruction)
                .build();
        log.info("Sending PrepareSS: " + prepareMachineMsg.toString());
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

    public Optional<MachineClientUtils.MachineState> getStateForMachine(MachineClientUtils.Machine machine) {
        return Optional.ofNullable(this.machineStates.get(machine));
    }

    public void update(MachineInfoProtos.MachineInfo info) {
        this.updateMachineState(info);
        this.updateMessages();
        log.info("States after update: " + machineStates.toString());
    }

    public void updateMachineState(MachineInfoProtos.MachineInfo info) {
        if (info.hasTeamColor()) {
            if (TeamProtos.Team.CYAN.equals(info.getTeamColor()) && this.teamColor.equals(TeamColor.CYAN)) {
                info.getMachinesList().forEach(this::updateMachineStatus);
            } else if (TeamProtos.Team.MAGENTA.equals(info.getTeamColor()) && this.teamColor.equals(TeamColor.MAGENTA)) {
                info.getMachinesList().forEach(this::updateMachineStatus);
            }
        } else {
            log.warning("MachineInfo without team color received");
        }
    }

    //todo check if remove conditions are correct!.
    public void updateMessages() {
        fetchMachinesPreparing().stream()
                .filter(machineStates::containsKey)
                .filter(sendQueue::containsKey)
                .forEach(x -> {
                    if (!machineStates.get(x).equals(MachineClientUtils.MachineState.IDLE)) {
                        if (!machineStates.get(x).equals(MachineClientUtils.MachineState.DOWN)) {
                            sendQueue.remove(x);
                        } else {
                            log.info("Machine " + x + " is down, not removing from send queue!");
                        }
                    }
                });
        fetchMachinesResetting().stream()
                .filter(machineStates::containsKey)
                .filter(sendQueue::containsKey)
                .forEach(x -> {
                    if (machineStates.get(x).equals(MachineClientUtils.MachineState.BROKEN)) {
                        sendQueue.remove(x);
                    }
                });
    }

    private RingColor toRefboxRingColor(MachineClientUtils.RingColor ringColor) {
        switch (ringColor) {
            case Blue:
                return RingColor.RING_BLUE;
            case Green:
                return RingColor.RING_GREEN;
            case Orange:
                return RingColor.RING_ORANGE;
            case Yellow:
                return RingColor.RING_YELLOW;
        }
        throw new IllegalArgumentException("RingColor not mapped: " + ringColor);
    }

    private MachineClientUtils.RingColor fromRefboxRingColor(RingColor ringColor) {
        switch (ringColor) {
            case RING_BLUE:
                return MachineClientUtils.RingColor.Blue;
            case RING_GREEN:
                return MachineClientUtils.RingColor.Green;
            case RING_ORANGE:
                return MachineClientUtils.RingColor.Orange;
            case RING_YELLOW:
                return MachineClientUtils.RingColor.Yellow;
        }
        throw new IllegalArgumentException("RingColor not mapped: " + ringColor);
    }

    private void updateMachineStatus(MachineInfoProtos.Machine machineInfo) {
        MachineClientUtils.Machine machine = MachineClientUtils.parseMachineWithColor(machineInfo.getName());
        MachineClientUtils.MachineState state = MachineClientUtils.parseMachineState(machineInfo.getState());
        if (machine == MachineClientUtils.Machine.RS1 || machine == MachineClientUtils.Machine.RS2) {
            this.ringColorToMachine.put(fromRefboxRingColor(machineInfo.getRingColors(0)), machine);
            this.ringColorToMachine.put(fromRefboxRingColor(machineInfo.getRingColors(1)), machine);
        }
        machineStates.put(machine, state);
    }

    public Ring getRingForProtoRing(RingColor protoRingColor) {
        var ringColor = fromRefboxRingColor(protoRingColor);
        if (!ringColorToMachine.containsKey(ringColor)) {
            throw new RuntimeException("Don't know which machine Ring is at: " + ringColor);
        }
        if (!ringColorToCost.containsKey(ringColor)) {
            throw new RuntimeException("Don't know how much Ring costs: " + ringColor);
        }
        return new Ring(ringColorToMachine.get(ringColor),ringColor, ringColorToCost.get(ringColor));
    }

    private String machineNameForMsg(MachineClientUtils.Machine machine, TeamColor color) {
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

    private List<ProtobufMessage> fetchMsgForType(Class<? extends GeneratedMessageV3> clazz) {
        Key key = RobotMessageRegister.getInstance().get_msg_key_from_class(clazz);
        return this.sendQueue.values().stream()
                .filter(x -> x.getClass().equals(clazz))
                .map(x -> new ProtobufMessage(key.cmp_id, key.msg_id, x))
                .collect(Collectors.toList());
    }

    private Set<MachineClientUtils.Machine> fetchMachinesForType(Class<? extends GeneratedMessageV3> clazz) {
        Set<MachineClientUtils.Machine> returner = new HashSet<>();
        this.sendQueue.forEach((machine, msg) -> {
            if (msg.getClass().equals(clazz)) {
                returner.add(machine);
            }
        });
        return returner;
    }

    private void addMessageToSendQueue(MachineClientUtils.Machine machine, GeneratedMessageV3 msg) {
        sendQueue.put(machine, msg);
    }

    public void updateRingInfo(RingInfoProtos.RingInfo ringInfo) {
        ringInfo.getRingsList().forEach(this::updateRingInfo);
    }

    private void updateRingInfo(RingInfoProtos.Ring ring) {
        ringColorToCost.put(fromRefboxRingColor(ring.getRingColor()), ring.getRawMaterial());
    }
}
