package com.rcll.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RobotBeacon {
    private Long robotId;
    private Long taskId;
    private String name;
    private Pose pose;
    private Instant receivedAt;
}
