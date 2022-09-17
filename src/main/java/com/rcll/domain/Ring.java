package com.rcll.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Ring {
    MachineClientUtils.Machine machine;
    MachineClientUtils.RingColor color;
    int rawMaterial;
}
