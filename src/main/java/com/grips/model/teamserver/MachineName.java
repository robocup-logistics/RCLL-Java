package com.grips.model.teamserver;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineName {
    private static String DUMMY_MACHINE = "XXX";
    private final String rawMachineName;

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

    public MachineClientUtils.Machine asMachineEnum() {
        if (isBaseStation()) {
            return MachineClientUtils.Machine.BS;
        }
        if (isDeliveryStation()) {
            return MachineClientUtils.Machine.DS;
        }
        if (isRingStation()) {
            if (this.rawMachineName.contains("1")) {
                return MachineClientUtils.Machine.RS1;
            }
            if (this.rawMachineName.contains("2")) {
                return MachineClientUtils.Machine.RS2;
            }
        }
        if (isCapStation()) {
            if (this.rawMachineName.contains("1")) {
                return MachineClientUtils.Machine.CS1;
            }
            if (this.rawMachineName.contains("2")) {
                return MachineClientUtils.Machine.CS2;
            }
        }
        if (isStorageStation()) {
            return MachineClientUtils.Machine.SS;
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
