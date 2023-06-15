package com.rcll.refbox;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rcll.domain.*;
import com.rcll.llsf_comm.Key;
import com.rcll.llsf_comm.ProtobufMessage;
import com.rcll.protobuf_lib.RobotMessageRegister;
import lombok.extern.java.Log;
import org.robocup_logistics.llsf_msgs.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.robocup_logistics.llsf_msgs.ProductColorProtos.BaseColor;

//todo move to refbox folder!
@Log
class MachineClient {
    private final TeamColor teamColor;
    private final Map<Machine, GeneratedMessageV3> sendQueue;
    private final Map<Machine, MachineState> machineStates;
    private final Map<Machine, ZoneName> machineZones;
    private final Map<RingColor, Machine> ringColorToMachine;
    private final Map<RingColor, Integer> ringColorToCost;
    private Set<Integer> preparedOrders;


    public MachineClient(TeamColor teamColor) {
        this.teamColor = teamColor;
        this.sendQueue = new ConcurrentHashMap<>();
        this.machineStates = new ConcurrentHashMap<>();
        this.machineZones = new ConcurrentHashMap<>();
        this.ringColorToMachine = new ConcurrentHashMap<>();
        this.ringColorToCost = new ConcurrentHashMap<>();
        this.preparedOrders = new HashSet<>();
    }

    public void sendResetMachine(Machine machine) {
        MachineInstructionProtos.ResetMachine reset = MachineInstructionProtos.ResetMachine.newBuilder()
                .setMachine(machineNameForMsg(machine, teamColor))
                .setTeamColor(TeamProtos.Team.valueOf(teamColor.toString()))
                .build();
        log.info("Sending ResetMachine: " + reset.toString());
        addMessageToSendQueue(machine, reset);
    }

    public void sendPrepareBS(MachineSide side, Base base_color) {
        Machine machine = Machine.BS;
        BaseColor bColor = null;
        switch (base_color) {
            case Red:
                bColor = BaseColor.BASE_RED;
                break;
            case Silver:
                bColor = BaseColor.BASE_SILVER;
                break;
            case Black:
                bColor = BaseColor.BASE_BLACK;
                break;
            default:
                throw new IllegalArgumentException("Not mapped baseColor: " + base_color);
        }
        MachineInstructionProtos.PrepareInstructionBS bsInstruction =
                MachineInstructionProtos.PrepareInstructionBS.newBuilder()
                        //.setSide(task.getSide() == SubProductionTask.MachineSide.INPUT ? MachineDescriptionProtos.MachineSide.INPUT : MachineDescriptionProtos.MachineSide.OUTPUT)
                        .setSide(side == MachineSide.Input ? MachineInstructionProtos.MachineSide.INPUT : MachineInstructionProtos.MachineSide.OUTPUT)
                        .setColor(bColor)
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
        Machine machine = Machine.DS;
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
        log.info("Sending PrepareDS: " + prepareMachineMsg);
        this.preparedOrders.add(orderId);
        addMessageToSendQueue(machine, prepareMachineMsg);
    }

    public void sendPrepareRS(Machine machine, RingColor ringColor) {
        ProductColorProtos.RingColor refbox_color = toRefboxRingColor(ringColor);
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

    public void sendPrepareCS(Machine machine, CapStationInstruction operation) {
        MachineDescriptionProtos.CSOp refbox_operation;
        switch (operation) {
            case RetrieveCap:
                refbox_operation = MachineDescriptionProtos.CSOp.RETRIEVE_CAP;
                break;
            case MountCap:
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

    public void sendPrepareSS(Machine machine, int shelf, int slot) {

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

    public void stopMessageForMachine(Machine machine) {
        sendQueue.remove(machine);
    }

    public List<ProtobufMessage> fetchPrepareMessages() {
        return fetchMsgForType(MachineInstructionProtos.PrepareMachine.class);
    }

    public List<ProtobufMessage> fetchResetMessages() {
        return fetchMsgForType(MachineInstructionProtos.ResetMachine.class);
    }

    public Set<Machine> fetchMachinesPreparing() {
        return fetchMachinesForType(MachineInstructionProtos.PrepareMachine.class);
    }

    public Set<Machine> fetchMachinesResetting() {
        return fetchMachinesForType(MachineInstructionProtos.ResetMachine.class);
    }

    public Optional<MachineState> getStateForMachine(Machine machine) {
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
                    if (!machineStates.get(x).equals(MachineState.IDLE)) {
                        if (!machineStates.get(x).equals(MachineState.DOWN)) {
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
                    if (machineStates.get(x).equals(MachineState.BROKEN)) {
                        sendQueue.remove(x);
                    }
                });
    }

    public ProductColorProtos.RingColor toRefboxRingColor(RingColor ringColor) {
        switch (ringColor) {
            case Blue:
                return ProductColorProtos.RingColor.RING_BLUE;
            case Green:
                return ProductColorProtos.RingColor.RING_GREEN;
            case Orange:
                return ProductColorProtos.RingColor.RING_ORANGE;
            case Yellow:
                return ProductColorProtos.RingColor.RING_YELLOW;
        }
        throw new IllegalArgumentException("RingColor not mapped: " + ringColor);
    }

    public RingColor fromRefboxRingColor(ProductColorProtos.RingColor ringColor) {
        switch (ringColor) {
            case RING_BLUE:
                return RingColor.Blue;
            case RING_GREEN:
                return RingColor.Green;
            case RING_ORANGE:
                return RingColor.Orange;
            case RING_YELLOW:
                return RingColor.Yellow;
        }
        throw new IllegalArgumentException("RingColor not mapped: " + ringColor);
    }

    private void updateMachineStatus(MachineInfoProtos.Machine machineInfo) {
        Machine machine = parseMachineWithColor(machineInfo.getName());
        MachineState state = parseMachineState(machineInfo.getState());

        if (machine == Machine.RS1 || machine == Machine.RS2) {
            this.ringColorToMachine.put(fromRefboxRingColor(machineInfo.getRingColors(0)), machine);
            this.ringColorToMachine.put(fromRefboxRingColor(machineInfo.getRingColors(1)), machine);
        }
        machineStates.put(machine, state);
        if (machineInfo.hasZone()) {
            ZoneName zoneName = parseZoneName(machineInfo.getZone());
            machineZones.put(machine, zoneName);
        }
    }

    private ZoneName parseZoneName(ZoneProtos.Zone zone) {
        if (zone != null) {
            return new ZoneName(zone.name());
        }
        return null;
    }

    public Ring getRingForColor(RingColor ringColor) {
        if (!ringColorToMachine.containsKey(ringColor)) {
            throw new RuntimeException("Don't know which machine Ring is at: " + ringColor);
        }
        if (!ringColorToCost.containsKey(ringColor)) {
            throw new RuntimeException("Don't know how much Ring costs: " + ringColor);
        }
        return new Ring(ringColorToMachine.get(ringColor),ringColor, ringColorToCost.get(ringColor));
    }

    private String machineNameForMsg(Machine machine, TeamColor color) {
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

    private Set<Machine> fetchMachinesForType(Class<? extends GeneratedMessageV3> clazz) {
        Set<Machine> returner = new HashSet<>();
        this.sendQueue.forEach((machine, msg) -> {
            if (msg.getClass().equals(clazz)) {
                returner.add(machine);
            }
        });
        return returner;
    }

    private void addMessageToSendQueue(Machine machine, GeneratedMessageV3 msg) {
        sendQueue.put(machine, msg);
    }

    public void updateRingInfo(RingInfoProtos.RingInfo ringInfo) {
        ringInfo.getRingsList().forEach(this::updateRingInfo);
    }

    private void updateRingInfo(RingInfoProtos.Ring ring) {
        ringColorToCost.put(fromRefboxRingColor(ring.getRingColor()), ring.getRawMaterial());
    }

    private MachineState parseMachineState(String state) {
        switch (state) {
            case "READY-AT-OUTPUT":
                return MachineState.READY_AT_OUTPUT;
            case "IDLE":
                return MachineState.IDLE;
            case "DOWN":
                return MachineState.DOWN;
            case "BROKEN":
                return MachineState.BROKEN;
            case "PREPARED":
                return MachineState.PREPARED;
            case "PROCESSING":
                return MachineState.PROCESSING;
            case "PROCESSED":
                return MachineState.PROCESSED;
            case "WAIT-IDLE":
                return MachineState.WAIT_IDLE;
            case "AVAILABLE":
                return MachineState.AVAILABLE;
        }
        if (state.equals("")) {
            return MachineState.UNDEFINED;
        }
        throw new IllegalArgumentException("Unkown Machine state: " + state);
    }

    private Machine parseMachineWithColor(String machine) {
        if (machine.contains("BS")) {
            return Machine.BS;
        } else if (machine.contains("DS")) {
            return Machine.DS;
        } else if (machine.contains("CS1")) {
            return Machine.CS1;
        } else if (machine.contains("CS2")) {
            return Machine.CS2;
        } else if (machine.contains("RS1")) {
            return Machine.RS1;
        } else if (machine.contains("RS2")) {
            return Machine.RS2;
        } else if (machine.contains("SS")) {
            return Machine.SS;
        }
        throw new IllegalArgumentException("Unkown machine: " + machine);
    }

    public Integer getCountMachines() {
        return this.machineZones.size();
    }

    public Set<Integer> getPreparedOrders() {
        return this.preparedOrders;
    }
}
