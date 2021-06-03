package com.grips.model.teamserver;

import com.grips.refbox.PeerConfig;
import com.grips.refbox.RefBoxConnectionManager;
import com.grips.refbox.RefboxConnectionConfig;
import com.grips.refbox.TeamConfig;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@CommonsLog
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

    @SneakyThrows
    @Test
    @Disabled
    public void testPrepareCS() {
        MachineClient machineClient = new MachineClient(MachineClientUtils.TeamColor.CYAN);
        assertThat(machineClient.fetchMachinesPreparing()).isEmpty();
        assertThat(machineClient.fetchPrepareMessages()).isEmpty();
        machineClient.sendPrepareCS(MachineClientUtils.Machine.CS2, MachineClientUtils.CSOp.RETRIEVE_CAP);

        RefBoxConnectionManager rbcm =
                new RefBoxConnectionManager(
                        new RefboxConnectionConfig(
                                "localhost",
                                new PeerConfig(4444, 4445),
                                new PeerConfig(4441, 4446),
                                new PeerConfig(4442, 4447)),
                        new TeamConfig("randomkey", "CYAN"),
                        null,
                        null);
        try {
            rbcm.startServer();
            log.info("Started RefBoxConnectionManager Servers!");
        } catch (Exception e) {
            log.error("Error starting RefboxConnectionManager - is the refbox running?", e);
            System.exit(1);
        }

        for (int i = 0; i < 100; i++) {
            machineClient.fetchPrepareMessages().forEach(rbcm::sendPrivateMsg);
            Thread.currentThread().sleep(1000);
        }

    }
}
