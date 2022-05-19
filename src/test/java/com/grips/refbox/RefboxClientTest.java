package com.grips.refbox;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
public class RefboxClientTest {
    @Test
    public void testSendBeaconSignal() throws InterruptedException {
        RefBoxConnectionManager rbcm = mock(RefBoxConnectionManager.class);
        RefboxClient refboxClient = new RefboxClient(
                new RefboxConnectionConfig("123",
                        new PeerConfig(1, 2),
                        new PeerConfig(3, 4),
                        new PeerConfig(5, 6)),
                new TeamConfig("randomkey", "MAGENTA", "GRIPS"),
                new RefboxHandler(),
                new RefboxHandler(),
                100, rbcm);
        log.debug("DEBUG123");
        log.info("INFO123");
        log.warn("WARN123");
        log.error("ERROR123");
        System.out.println("TEST123");
        verify(rbcm, times(0)).sendPrivateMsg(any());
        refboxClient.sendBeaconSignal(1, "name", 1.0f, 2.0f, 90f);
        Thread.sleep(500);
        verify(rbcm, atLeast(1)).sendPrivateMsg(any());
    }
}
