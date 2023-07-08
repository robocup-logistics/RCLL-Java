package com.rcll.domain;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class MachinePosition {
    ZoneName zone;
    Integer rotation;
}
