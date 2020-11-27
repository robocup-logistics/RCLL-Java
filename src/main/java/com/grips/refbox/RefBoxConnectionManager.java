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

import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_comm.ProtobufMessageHandler;
import org.robocup_logistics.llsf_msgs.*;

@CommonsLog
public class RefBoxConnectionManager {
    public final static int CIPHER_TYPE_NO_CIPHER = 0;
    public final static int CIPHER_TYPE_AES_128_ECB = 1;
    public final static int CIPHER_TYPE_AES_128_CBC = 2;
    public final static int CIPHER_TYPE_AES_256_ECB = 3;
    public final static int CIPHER_TYPE_AES_256_CBC = 4;

    RefboxConnectionConfig connectionConfig;
    TeamConfig teamConfig;
    PeerConfig usedPrivatePeer;

    RefboxConnection teamConnection;
    RefboxConnection publicConnection;

    private int cipher_type = RefBoxConnectionManager.CIPHER_TYPE_AES_128_CBC;

    public RefBoxConnectionManager(RefboxConnectionConfig connectionConfig,
                                   TeamConfig teamConfig,
                                   ProtobufMessageHandler privateHandler,
                                   ProtobufMessageHandler publicHandler) {
        this.connectionConfig = connectionConfig;
        this.teamConfig = teamConfig;
        this.publicConnection = new RefboxConnection(connectionConfig.getIp(),
                connectionConfig.getPublicPeer().getSendPort(), connectionConfig.getPublicPeer().getReceivePort(), privateHandler, true, cipher_type, teamConfig.getCryptoKey());

        if (teamConfig.getColor().equals("CYAN")) {
            usedPrivatePeer = connectionConfig.getCyanPeer();
        } else if (teamConfig.getColor().equals("MAGENTA")) {
            usedPrivatePeer = connectionConfig.getMagentaPeer();
        } else {
            throw new RuntimeException("Invalid team color: " + teamConfig.getColor());
        }

        this.teamConnection = new RefboxConnection(connectionConfig.getIp(), usedPrivatePeer.getSendPort(),
                usedPrivatePeer.getReceivePort(), publicHandler, true, cipher_type, teamConfig.getCryptoKey());
    }

    @SneakyThrows
    public void startServer() {
        publicConnection.start();
        teamConnection.start();
        log.info("Successfully started RefBoxConnectionManager!");
    }

    public void sendPublicMsg(ProtobufMessage msg) {
        log.debug("Sending public message to Refbox: " + msg.toString());
        publicConnection.enqueue(msg);
    }

    public void sendPrivateMsg(ProtobufMessage msg) {
        log.debug("Sending public message to Refbox: " + msg.toString());
        teamConnection.enqueue(msg);
    }

    private void registerBroadcastMsgs() {
        publicConnection.add_message(BeaconSignalProtos.BeaconSignal.class);
        publicConnection.add_message(OrderInfoProtos.OrderInfo.class);       // Not documented but sent!
        publicConnection.add_message(RingInfoProtos.RingInfo.class);         // Not documented but sent!
        publicConnection.add_message(GameStateProtos.GameState.class);
        publicConnection.add_message(ExplorationInfoProtos.ExplorationInfo.class);
        publicConnection.add_message(VersionProtos.VersionInfo.class);
        publicConnection.add_message(RobotInfoProtos.RobotInfo.class);
    }

    private void registerTeamMsgs() {
        teamConnection.add_message(BeaconSignalProtos.BeaconSignal.class);
        teamConnection.add_message(OrderInfoProtos.OrderInfo.class);
        teamConnection.add_message(RingInfoProtos.RingInfo.class);         // Not documented but sent!
        teamConnection.add_message(MachineInfoProtos.MachineInfo.class);
        teamConnection.add_message(MachineReportProtos.MachineReportInfo.class);
        teamConnection.add_message(ExplorationInfoProtos.ExplorationInfo.class);
        teamConnection.add_message(MachineInstructionProtos.PrepareMachine.class);
        teamConnection.add_message(MachineInstructionProtos.ResetMachine.class);
    }
}
