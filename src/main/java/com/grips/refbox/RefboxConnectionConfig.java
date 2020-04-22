package com.grips.refbox;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefboxConnectionConfig {
    private String ip;
    PeerConfig publicPeer;
    PeerConfig cyanPeer;
    PeerConfig magentaPeer;
}
