package com.grips.refbox;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefboxConnectionConfig {
    private String ip;
    PeerConfig publicPeer;
    PeerConfig cyanPeer;
    PeerConfig magentaPeer;
}
