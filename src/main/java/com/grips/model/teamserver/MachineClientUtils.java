package com.grips.model.teamserver;

public class MachineClientUtils {
    public static Machine parseMachineWithColor(String machine) {
        if (machine.contains("BS")) {
            return Machine.BS;
        } else if (machine.contains("DS")) {
            return Machine.DS;
        } else if (machine.contains("CS1")) {
            return Machine.CS_1;
        } else if (machine.contains("CS2")) {
            return Machine.CS_2;
        } else if (machine.contains("RS1")) {
            return Machine.RS_2;
        } else if (machine.contains("RS2")) {
            return Machine.RS_2;
        } else if (machine.contains("SS")) {
            return Machine.SS;
        }
        throw new IllegalArgumentException("Unkown machine: " + machine);
    }
    public enum TeamColor {CYAN, MAGENTA}
    public enum Machine {
        BS("BS"), DS("DS"), RS_1("RS-1"), RS_2("RS-2"), CS_1("CS-1"), CS_2("CS-2"), SS("SS");
        private final String machine;
        private Machine(final String machine){ this.machine = machine; }
        public boolean isCS() { if(this.machine.contains("CS")) {return true;} return false; }
        public boolean isRS() { if(this.machine.contains("RS")) {return true;} return false; }
        public boolean isBS() { if(this.machine.contains("BS")) {return true;} return false; }
        public boolean isDS() { if(this.machine.contains("DS")) {return true;} return false; }
        public boolean isSS() { if(this.machine.contains("SS")) {return true;} return false; }
    }
    public enum MachineSide {Input, Output}
    public enum BaseColor {BASE_RED, BASE_BLACK, BASE_SILVER}
    public enum RingColor {RING_BLUE, RING_GREEN, RING_ORANGE, RING_YELLOW}
    public enum CSOp {RETRIEVE_CAP, MOUNT_CAP}
}
