package com.grips.refbox;

import org.junit.jupiter.api.Test;

import static com.grips.model.teamserver.TeamColor.CYAN;
import static org.assertj.core.api.Assertions.assertThat;

public class RobotClientTest {
    //todo add more asserts in this Test
    @Test
    public void testSendBeaconSignal() {
        RobotClient robotClient = new RobotClient(CYAN, "GRIPS");
        assertThat(robotClient.fetchBeaconSignals()).isEmpty();
        robotClient.sendRobotBeaconMsg(1, "test1", 1f, 2f, 90f);
        assertThat(robotClient.fetchBeaconSignals()).size().isOne();
    }
}
