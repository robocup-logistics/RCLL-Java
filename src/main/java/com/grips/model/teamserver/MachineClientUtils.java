package com.grips.model.teamserver;

import org.robocup_logistics.llsf_msgs.ProductColorProtos;

public class MachineClientUtils {

    public static MachineState parseMachineState(String state) {
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
            return Machine.RS1;
        } else if (machine.contains("RS2")) {
            return Machine.RS2;
        } else if (machine.contains("SS")) {
            return Machine.SS;
        }
        throw new IllegalArgumentException("Unkown machine: " + machine);
    }

    public enum Machine {
        BS, DS, RS1, RS2, CS1, CS2, SS;
    }

    public enum MachineSide {
        Input,
        Output
    }

    public enum BaseColor {
        BASE_RED,
        BASE_BLACK,
        BASE_SILVER
    }

    public enum RingColor {
        RING_BLUE,
        RING_GREEN,
        RING_ORANGE,
        RING_YELLOW
    }

    public enum MachineState {
        READY_AT_OUTPUT,
        IDLE,
        WAIT_IDLE,
        DOWN,
        BROKEN,
        PREPARED,
        PROCESSING,
        PROCESSED,
        AVAILABLE,
        UNDEFINED
    }

    public enum CSOp {
        RETRIEVE_CAP,
        MOUNT_CAP
    }
}
