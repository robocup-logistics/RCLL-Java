package com.rcll.domain;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineName {
    private static String DUMMY_MACHINE = "XXX";
    private final String rawMachineName;

    public MachineName(TeamColor teamColor, Machine machine) {
        String name = "";
        switch (teamColor) {
            case CYAN:
                name +="C-";
                break;
            case MAGENTA:
                name +="M-";
                break;
        }
        switch (machine) {
            case BS:
                name += "BS";
                break;
            case DS:
                name += "DS";
                break;
            case RS1:
                name += "RS1";
                break;
            case RS2:
                name += "RS2";
                break;
            case CS1:
                name += "CS1";
                break;
            case CS2:
                name += "CS2";
                break;
            case SS:
                name += "SS";
                break;
        }
        this.rawMachineName = name;
    }
    public MachineName(String name) {
        if (name.charAt(0) != 'M' && name.charAt(0) != 'C' && !name.equalsIgnoreCase(DUMMY_MACHINE)) {
            throw new IllegalArgumentException("Invalid machine name: " + name);
        }
        this.rawMachineName = name;
    }

    public static MachineName dummyMachine() {
        return new MachineName(DUMMY_MACHINE);
    }

    public boolean isDummyMachine() {
        return rawMachineName.equalsIgnoreCase(DUMMY_MACHINE);
    }

    public boolean isCyan() {
        return this.rawMachineName.charAt(0) == 'C';
    }

    public boolean isMagenta() {
        return this.rawMachineName.charAt(0) == 'M';
    }

    public boolean isCapStation() {
        return this.rawMachineName.contains("CS");
    }

    public boolean isCapStation1() {
        return this.rawMachineName.contains("CS1");
    }

    public boolean isCapStation2() {
        return this.rawMachineName.contains("CS2");
    }

    public boolean isBaseStation() {
        return this.rawMachineName.contains("BS");
    }

    public boolean isRingStation() {
        return this.rawMachineName.contains("RS");
    }

    public boolean isRingStation1() {
        return this.rawMachineName.contains("RS1");
    }

    public boolean isRingStation2() {
        return this.rawMachineName.contains("RS2");
    }

    public boolean isDeliveryStation() {
        return this.rawMachineName.contains("DS");
    }

    public boolean isStorageStation() {
        return this.rawMachineName.contains("SS");
    }

    public Machine asMachineEnum() {
        if (isBaseStation()) {
            return Machine.BS;
        }
        if (isDeliveryStation()) {
            return Machine.DS;
        }
        if (isRingStation()) {
            if (this.rawMachineName.contains("1")) {
                return Machine.RS1;
            }
            if (this.rawMachineName.contains("2")) {
                return Machine.RS2;
            }
        }
        if (isCapStation()) {
            if (this.rawMachineName.contains("1")) {
                return Machine.CS1;
            }
            if (this.rawMachineName.contains("2")) {
                return Machine.CS2;
            }
        }
        if (isStorageStation()) {
            return Machine.SS;
        }
        throw new RuntimeException("Unkonwn Machine: " + this.rawMachineName);
    }

    public MachineName mirror() {
        if (isCyan()) {
            return new MachineName('M' + this.rawMachineName.substring(1));
        } else {
            return new MachineName('C' + this.rawMachineName.substring(1));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MachineName) {
            return ((MachineName) o).rawMachineName.equalsIgnoreCase(rawMachineName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return rawMachineName.hashCode();
    }

    @Override
    public String toString() {
        return this.rawMachineName;
    }
}
