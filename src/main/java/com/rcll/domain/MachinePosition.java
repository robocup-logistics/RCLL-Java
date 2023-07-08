package com.rcll.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MachinePosition {
    ZoneName zone;
    Integer rotation;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MachinePosition) {
            return ((MachinePosition) obj).zone.equals(this.zone) && rotation == ((MachinePosition) obj).rotation;
        }
        return false;
    }
}
