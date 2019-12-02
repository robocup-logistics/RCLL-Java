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

import org.robocup_logistics.llsf_comm.ProtobufBroadcastPeer;
import org.robocup_logistics.llsf_comm.ProtobufMessage;
import org.robocup_logistics.llsf_comm.ProtobufMessageHandler;
import org.robocup_logistics.llsf_msgs.*;

import javax.annotation.PostConstruct;
import java.io.IOException;

public class RefBoxConnectionManager<PublicRefBoxHandler> {
    public final static int CIPHER_TYPE_NO_CIPHER = 0;
    public final static int CIPHER_TYPE_AES_128_ECB = 1;
    public final static int CIPHER_TYPE_AES_128_CBC = 2;
    public final static int CIPHER_TYPE_AES_256_ECB = 3;
    public final static int CIPHER_TYPE_AES_256_CBC = 4;

    private ProtobufBroadcastPeer _proto_broadcast_peer;
    private ProtobufBroadcastPeer _proto_team_peer;
    //TODO: remove these unnecessary members
    private String refbox_ip;
    private int refbox_public_port_send;
    private int refbox_public_port_receive;
    private int refbox_private_port_send_cyan;
    private int refbox_private_port_receive_cyan;
    private int refbox_private_port_send_magenta;
    private int refbox_private_port_receive_magenta;
    private String crypto_key;
    private String teamcolor;

    private int refbox_team_port_send = 0;
    private int refbox_team_port_receive = 0;

    private ProtobufMessageHandler team_handler;
    private ProtobufMessageHandler public_handler;

    private int cipher_type = RefBoxConnectionManager.CIPHER_TYPE_AES_128_CBC;

    public RefBoxConnectionManager(String refbox_ip, int refbox_public_port_send, int refbox_public_port_receive, int refbox_private_port_send_cyan, int refbox_private_port_receive_cyan, int refbox_private_port_send_magenta, int refbox_private_port_receive_magenta, String crypto_key, String teamcolor, ProtobufMessageHandler team_handler, ProtobufMessageHandler public_handler) {
        this.refbox_ip = refbox_ip;
        this.refbox_public_port_send = refbox_public_port_send;
        this.refbox_public_port_receive = refbox_public_port_receive;
        this.refbox_private_port_send_cyan = refbox_private_port_send_cyan;
        this.refbox_private_port_receive_cyan = refbox_private_port_receive_cyan;
        this.refbox_private_port_send_magenta = refbox_private_port_send_magenta;
        this.refbox_private_port_receive_magenta = refbox_private_port_receive_magenta;
        this.crypto_key = crypto_key;
        this.teamcolor = teamcolor;
        this.refbox_team_port_send = refbox_team_port_send;
        this.refbox_team_port_receive = refbox_team_port_receive;
        this.team_handler = team_handler;
        this.public_handler = public_handler;
        this.cipher_type = cipher_type;
    }

    @PostConstruct
    public void afterConstruct() {
        if ((CIPHER_TYPE_NO_CIPHER != cipher_type) && (CIPHER_TYPE_AES_128_ECB != cipher_type) &&
                (CIPHER_TYPE_AES_128_CBC != cipher_type) && (CIPHER_TYPE_AES_256_ECB != cipher_type) &&
                (CIPHER_TYPE_AES_256_CBC != cipher_type)) {
            System.err.println("ERROR: Invalid cipher type! Using AES 128 CBC as default...");
            cipher_type = CIPHER_TYPE_AES_128_CBC;
        }

        if(teamcolor.equals("CYAN")) {
            refbox_team_port_receive = refbox_private_port_receive_cyan;
            refbox_team_port_send = refbox_private_port_send_cyan;
        } else if(teamcolor.equals("MAGENTA")){
            refbox_team_port_receive = refbox_private_port_receive_magenta;
            refbox_team_port_send = refbox_private_port_send_magenta;
        } else {
            System.err.println("!!!!!!!!!!TeamColor neither CYAN nor MAGENTA!!!!!!!!!!");
        }


        // Setup Public Broadcast channel
        _proto_broadcast_peer = new ProtobufBroadcastPeer(refbox_ip, refbox_public_port_send, refbox_public_port_receive);

        try {
            _proto_broadcast_peer.start();
            registerBroadcastMsgs();
            _proto_broadcast_peer.register_handler(public_handler);
        } catch (IOException e) {
            e.printStackTrace();
        }



        // Setup Private Team channel
        _proto_team_peer = new ProtobufBroadcastPeer(refbox_ip, refbox_team_port_send,
                refbox_team_port_receive, true, cipher_type, crypto_key);
        try {
            _proto_team_peer.start();
            registerTeamMsgs();
            _proto_team_peer.register_handler(team_handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPublicMsg(ProtobufMessage msg) {
        _proto_broadcast_peer.enqueue(msg);
    }

    public void sendPrivateMsg(ProtobufMessage msg) {
        _proto_team_peer.enqueue(msg);
    }

    private void registerBroadcastMsgs() {
        _proto_broadcast_peer.add_message(BeaconSignalProtos.BeaconSignal.class);
        _proto_broadcast_peer.add_message(OrderInfoProtos.OrderInfo.class);       // Not documented but sent!
        _proto_broadcast_peer.add_message(RingInfoProtos.RingInfo.class);         // Not documented but sent!
        _proto_broadcast_peer.add_message(GameStateProtos.GameState.class);
        _proto_broadcast_peer.add_message(ExplorationInfoProtos.ExplorationInfo.class);
        _proto_broadcast_peer.add_message(VersionProtos.VersionInfo.class);
        _proto_broadcast_peer.add_message(RobotInfoProtos.RobotInfo.class);
    }

    private void registerTeamMsgs() {
        _proto_team_peer.add_message(BeaconSignalProtos.BeaconSignal.class);
        _proto_team_peer.add_message(OrderInfoProtos.OrderInfo.class);
        _proto_team_peer.add_message(RingInfoProtos.RingInfo.class);         // Not documented but sent!
        _proto_team_peer.add_message(MachineInfoProtos.MachineInfo.class);
        _proto_team_peer.add_message(MachineReportProtos.MachineReportInfo.class);
        _proto_team_peer.add_message(ExplorationInfoProtos.ExplorationInfo.class);
        _proto_team_peer.add_message(MachineInstructionProtos.PrepareMachine.class);
        _proto_team_peer.add_message(MachineInstructionProtos.ResetMachine.class);
    }
}
