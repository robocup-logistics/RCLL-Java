/*
 *
 * Copyright (c) 2017, Graz Robust and Intelligent Production System (grips)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.grips.refbox;

import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_msgs.*;

import java.io.IOException;

@CommonsLog
public class RefBoxConnectionManager {
    public final static int CIPHER_TYPE_NO_CIPHER = 0;
    public final static int CIPHER_TYPE_AES_128_ECB = 1;
    public final static int CIPHER_TYPE_AES_128_CBC = 2;
    public final static int CIPHER_TYPE_AES_256_ECB = 3;
    public final static int CIPHER_TYPE_AES_256_CBC = 4;

    private RefboxConnection publicPeer;
    private RefboxConnection privatePeer;

    private final RefboxConnectionConfig connectionConfig;
    private final TeamConfig teamConfig;
    private final PeerConfig usedPrivatePeer;

    private int cipher_type = RefBoxConnectionManager.CIPHER_TYPE_AES_128_CBC;

    public RefBoxConnectionManager(RefboxConnectionConfig connectionConfig,
                                   TeamConfig teamConfig,
                                   RefboxHandler privateHandler,
                                   RefboxHandler publicHandler) {
        this.connectionConfig = connectionConfig;
        this.teamConfig = teamConfig;

        publicPeer = new RefboxConnection(connectionConfig.getIp(),
                connectionConfig.getPublicPeer().getSendPort(), connectionConfig.getPublicPeer().getReceivePort(), publicHandler);
        if (teamConfig.getColor().equals("CYAN")) {
            usedPrivatePeer = connectionConfig.getCyanPeer();
        } else if (teamConfig.getColor().equals("MAGENTA")) {
            usedPrivatePeer = connectionConfig.getMagentaPeer();
        } else {
            throw new RuntimeException("Invalid team color: " + teamConfig.getColor());
        }

        privatePeer = new RefboxConnection(connectionConfig.getIp(), usedPrivatePeer.getSendPort(),
                usedPrivatePeer.getReceivePort(), privateHandler, true, cipher_type, teamConfig.getCryptoKey());
    }


    public void startServer() {
        startPublicPeer();
        startPrivatePeer();
        log.info("Successfully create RefBoxConnectionManager!");
    }

    private void startPrivatePeer() {
        try {
            privatePeer.start("RbcmPrivatePeer");
            registerTeamMsgs();
        } catch (IOException e) {
            log.error("Error starting private peer: ", e);
            throw new RuntimeException("Not able to create private peer!");
        }
    }

    private void startPublicPeer() {
        try {
            publicPeer.start("RbcmPublicPeer");
            registerBroadcastMsgs();
        } catch (IOException e) {
            log.error("Error starting public peer: ", e);
            throw new RuntimeException("Not able to create public peer!");
        }
    }

    public void sendPublicMsg(ProtobufMessage msg) {
        log.debug("Sending public message to Refbox: " + msg.toString());
        publicPeer.getPeer().enqueue(msg);
    }

    public void sendPrivateMsg(ProtobufMessage msg) {
        log.debug("Sending private message to Refbox: " + msg.toString());
        privatePeer.getPeer().enqueue(msg);
    }

    private void registerBroadcastMsgs() {
        publicPeer.add_message(BeaconSignalProtos.BeaconSignal.class);
        publicPeer.add_message(OrderInfoProtos.OrderInfo.class);       // Not documented but sent!
        publicPeer.add_message(RingInfoProtos.RingInfo.class);         // Not documented but sent!
        publicPeer.add_message(GameStateProtos.GameState.class);
        publicPeer.add_message(ExplorationInfoProtos.ExplorationInfo.class);
        publicPeer.add_message(VersionProtos.VersionInfo.class);
        publicPeer.add_message(RobotInfoProtos.RobotInfo.class);
        publicPeer.add_message(NavigationChallengeProtos.NavigationRoutes.class);
    }

    private void registerTeamMsgs() {
        privatePeer.add_message(BeaconSignalProtos.BeaconSignal.class);
        privatePeer.add_message(OrderInfoProtos.OrderInfo.class);
        privatePeer.add_message(RingInfoProtos.RingInfo.class);         // Not documented but sent!
        privatePeer.add_message(MachineInfoProtos.MachineInfo.class);
        privatePeer.add_message(MachineReportProtos.MachineReportInfo.class);
        privatePeer.add_message(ExplorationInfoProtos.ExplorationInfo.class);
        privatePeer.add_message(MachineInstructionProtos.PrepareMachine.class);
        privatePeer.add_message(MachineInstructionProtos.ResetMachine.class);
        privatePeer.add_message(NavigationChallengeProtos.NavigationRoutes.class);
    }
}
