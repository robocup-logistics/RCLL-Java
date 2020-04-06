package com.grips.model.teamserver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MachineClientTest {
    @Test
    public void testPrepareDS() {
        MachineClient machineClient = new MachineClient(MachineClientUtils.TeamColor.CYAN);
        assertThat(machineClient.fetchMachinesPreparing()).isEmpty();
        assertThat(machineClient.fetchPrepareMessages()).isEmpty();
        machineClient.sendPrepareDS(123, 456);
        assertThat(machineClient.fetchMachinesPreparing()).size().isOne();
        assertThat(machineClient.fetchPrepareMessages()).size().isOne();
    }
}
