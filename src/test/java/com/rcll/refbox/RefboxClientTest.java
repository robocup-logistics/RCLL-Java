package com.rcll.refbox;

import com.rcll.domain.TeamColor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
public class RefboxClientTest {
    @Test
    @Disabled
    public void testSendBeaconSignal() throws InterruptedException {
        RefBoxConnectionManager rbcm = mock(RefBoxConnectionManager.class);
        RefboxHandler publicHandler = new RefboxHandler();
        publicHandler.setGameStateCallback(data -> {
            log.info(data);
        });
        RefboxClient refboxClient = new RefboxClient(
                new RefboxConnectionConfig("localhost",
                        new PeerConfig(4444, 4445),
                        new PeerConfig(4441, 4446),
                        new PeerConfig(4442, 4447)),
                new TeamConfig("randomkey", "GRIPS"),
                new RefboxHandler(),
                publicHandler,
                100,
                rbcm);
        refboxClient.startServer();
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
