package com.rcll.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Ring {
    Machine machine;
    RingColor color;
    int rawMaterial;
}
