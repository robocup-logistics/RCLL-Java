package com.grips.refbox;

import org.junit.jupiter.api.Test;
import org.mockito.verification.VerificationMode;

import static com.grips.model.teamserver.TeamColor.CYAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        verify(rbcm, times(0)).sendPrivateMsg(any());
        refboxClient.sendBeaconSignal(1, "name", 1.0f, 2.0f, 90f);
        Thread.sleep(500);
        verify(rbcm, atLeast(1)).sendPrivateMsg(any());
    }
}
