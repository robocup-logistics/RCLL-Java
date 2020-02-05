package com.grips.model.teamserver;

public class MachineClientUtils {
    public static Machine parseMachineWithColor(String machine) {
        if (machine.contains("BS")) {
            return Machine.BS;
        } else if (machine.contains("DS")) {
            return Machine.DS;
        } else if (machine.contains("CS1")) {
            return Machine.CS1;
        } else if (machine.contains("CS2")) {
            return Machine.CS2;
        } else if (machine.contains("RS1")) {
            return Machine.RS2;
        } else if (machine.contains("RS2")) {
            return Machine.RS2;
        } else if (machine.contains("SS")) {
            return Machine.SS;
        }
        throw new IllegalArgumentException("Unkown machine: " + machine);
    }
    public enum TeamColor {CYAN, MAGENTA}
    public enum Machine {
        BS, DS, RS1, RS2, CS1, CS2, SS;
    }
    public enum MachineSide {Input, Output}
    public enum BaseColor {BASE_RED, BASE_BLACK, BASE_SILVER}
    public enum RingColor {RING_BLUE, RING_GREEN, RING_ORANGE, RING_YELLOW}
    public enum CSOp {RETRIEVE_CAP, MOUNT_CAP}
}
