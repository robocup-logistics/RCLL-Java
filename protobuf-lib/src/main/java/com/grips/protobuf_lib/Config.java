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

package com.grips.protobuf_lib;

import com.google.common.primitives.Ints;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Config {
    public static final int MAX_BASES_CS = 1;
    public static final int MAX_BASES_RS = 3;
    public static final int SLEEP_PRODUCTION_START_MILLIS = 5000;
    public static final boolean SIMULATING = true;
    public static final String UNKNOWN_MACHINE_IDENTIFIER = "X-XX";
    public static final String UNKNOWN_MACHINE_TYPE = "";
    public static final int NUM_ZONES_WIDTH = 14;
    public static final int NUM_ZONES_HEIGHT = 8;
    //public static final long NUM_ZONES_HEIGHT = 5; //TODO: change back to 8 for competitionq
    public static final long NUM_ZONES_GAMEFIELD = NUM_ZONES_HEIGHT * NUM_ZONES_WIDTH;
    public static final double GAME_ZONE_WIDTH = 1.0;
    public static final double GAME_ZONE_HEIGHT = 1.0;
    public static final BigInteger NUM_NSECS_ROBOT_LOST = new BigInteger("5000000000");
    public static final long NUM_MILIS_ROBOT_LOST = 5000;
    public static final long NUM_ROBOTS_PER_TEAM = 3;
    public static final List<String> MPS_NAMES = Collections
            .unmodifiableList(Arrays.asList("C-BS", "C-DS", "C-SS","C-RS1", "C-RS2", "C-CS1", "C-CS2",
                    "M-BS", "M-DS", "M-SS", "M-RS1", "M-RS2", "M-CS1", "M-CS2", "C-SS", "M-SS"));
    public static final List<Integer> INSERTION_ZONE_NUMBERS = Collections.unmodifiableList(Arrays.asList(51, 61, 71));
    public static final List<Integer> FREE_ZONE_NUMBERS = Collections.unmodifiableList(Arrays.asList(52));
    public static final List<String> TEAM_PREFIX = Collections.unmodifiableList(Arrays.asList("C", "M"));

    public static final List<Integer> INVALID_ZONES = new ArrayList<>(Ints.asList(51,61,71,52));


    public static final BigInteger NUM_NSEC_REPORT_AR_ONLY = new BigInteger("176000000000"); // 2min 57s

    public static final BigInteger NUM_NSECS_EXPLORATION_START_REPORTING = new BigInteger("0");
    public static final int NUM_RING_COLORS = 4;

    // values are only default values and will be overwritten
    // by values defined in springs application.properties file!!!
    public static String REFBOX_IP = "127.0.0.1";
    public static int SENDPORT = 4445;
    public static int RECVPORT = 4444;
    public static int CYAN_SENDPORT = 4446;
    public static int CYAN_RECVPORT = 4441;
    public static int MAGENTA_SENDPORT = 4447;
    public static int MAGENTA_RECVPORT = 4442;
    public static int ROBOT_LISTEN_PORT = 2016;
    public static String CRYPTO_KEY = "randomkey";
    public static String TEAM_NAME = "GRIPS";
    public static String TEAM_CYAN_COLOR = "CYAN";
    public static String TEAM_MAGENTA_COLOR = "MAGENTA";

    public void setRefboxIP(String ip) {
        REFBOX_IP = ip;
    }

    public void setSendPort(int port) {
        SENDPORT = port;
    }

    public void setRecvPort(int port) {
        RECVPORT = port;
    }

    public void setCyanSendPort(int port) {
        CYAN_SENDPORT = port;
    }

    public void setCyanRecvPort(int port) {
        CYAN_RECVPORT = port;
    }

    public void setMagentaSendPort(int port) {
        MAGENTA_SENDPORT = port;
    }

    public void setMagentaRecvPort(int port) {
        MAGENTA_RECVPORT = port;
    }

    public void setRobotListenPort(int port) {
        ROBOT_LISTEN_PORT = port;
    }

    public void setCryptoKey(String cryptoKey) {
        CRYPTO_KEY = cryptoKey;
    }


    public static int POINTS_BUFFER_BASE_IN_RS;
    public static int POINTS_BUFFER_CAP_IN_CS;
    public static int POINTS_FINISH_CC0;
    public static int POINTS_FINISH_CC1;
    public static int POINTS_FINISH_CC2;
    public static int POINTS_LAST_RING_C1;
    public static int POINTS_LAST_RING_C2;
    public static int POINTS_LAST_RING_C3;
    public static int POINTS_MOUNT_CAP;
    public static int POINTS_IN_TIME_DELIVERY;
    public static int POINTS_LATE_DELIVERY;
    public static String GREY_CAP_MACHINE;
    public static String BLACK_CAP_MACHINE;
    public static int PRIORITY_FIRST_RING;
    public static int PRIORITY_SECOND_RING;
    public static int PRIORITY_THIRD_RING;
    public static int PRIORITY_MOUNT_CAP_C0;
    public static int PRIORITY_MOUNT_CAP_C1;
    public static int PRIORITY_MOUNT_CAP_C2;
    public static int PRIORITY_MOUNT_CAP_C3;
    public static int PRIORITY_DELIVER_C0;
    public static int PRIORITY_DELIVER_C1;
    public static int PRIORITY_DELIVER_C2;
    public static int PRIORITY_DELIVER_C3;
    public static int PRIORITY_MOUNT_CAP_C0_COMPETITIVE;
    public static int PRIORITY_DELIVER_C0_COMPETITIVE;
    public static boolean DO_C0;
    public static boolean DO_C1;
    public static boolean DO_C2;
    public static boolean DO_C3;
    public static int MAX_SIMULTANEOUS_PRODUCTS;
    public static int MAX_SIMULTANEOUS_PRODUCTS_SAME_CAP;
    public static boolean DO_STANDING_C0_SS;

    public void setBufferBaseInRs(int p) { POINTS_BUFFER_BASE_IN_RS = p; }

    public void setBufferCapInCs(int p) { POINTS_BUFFER_CAP_IN_CS = p; }

    public void setFinishCC0(int p) { POINTS_FINISH_CC0 = p; }

    public void setFinishCC1(int p) { POINTS_FINISH_CC1 = p; }

    public void setFinishCC2(int p) { POINTS_FINISH_CC2 = p; }

    public void setLastRingC1(int p) { POINTS_LAST_RING_C1 = p; }

    public void setLastRingC2(int p) { POINTS_LAST_RING_C2 = p; }

    public void setLastRingC3(int p) { POINTS_LAST_RING_C3 = p; }

    public void setMountCap(int p) { POINTS_MOUNT_CAP = p; }

    public void setInTimeDelivery(int p) { POINTS_IN_TIME_DELIVERY = p; }

    public void setLateDelivery(int p) { POINTS_LATE_DELIVERY = p; }

    public void setGreyCapMachine(String m) { GREY_CAP_MACHINE = m; }

    public void setBlackCapMachine(String m) { BLACK_CAP_MACHINE = m; }

    public void setPriorityFirstRing(int p) { PRIORITY_FIRST_RING = p; }
    public void setPrioritySecondRing(int p) { PRIORITY_SECOND_RING = p; }
    public void setPriorityThirdRing(int p) { PRIORITY_THIRD_RING = p; }


    public void setPriorityMountCapC0(int p) { PRIORITY_MOUNT_CAP_C0 = p; }
    public void setPriorityMountCapC1(int p) { PRIORITY_MOUNT_CAP_C1 = p; }
    public void setPriorityMountCapC2(int p) { PRIORITY_MOUNT_CAP_C2 = p; }
    public void setPriorityMountCapC3(int p) { PRIORITY_MOUNT_CAP_C3 = p; }

    public void setPriorityDeliverC0(int p) { PRIORITY_DELIVER_C0 = p; }
    public void setPriorityDeliverC1(int p) { PRIORITY_DELIVER_C1 = p; }
    public void setPriorityDeliverC2(int p) { PRIORITY_DELIVER_C2 = p; }
    public void setPriorityDeliverC3(int p) { PRIORITY_DELIVER_C3 = p; }

    public void setPriorityMountCap0competitive(int p) { PRIORITY_MOUNT_CAP_C0_COMPETITIVE = p; }
    public void setPriorityDeliverC0Competitive(int p) { PRIORITY_DELIVER_C0_COMPETITIVE = p; }

    public void setDoC0(boolean d) { DO_C0 = d; }
    public void setDoC1(boolean d) { DO_C1 = d; }
    public void setDoC2(boolean d) { DO_C2 = d; }
    public void setDoC3(boolean d) { DO_C3 = d; }

    public void setDoStandingC0Ss(boolean d) { DO_STANDING_C0_SS = d; }

    public void setMaxSimultaneousProducts(int m) { MAX_SIMULTANEOUS_PRODUCTS = m; }

    public void setMaxSimultaneousProductsSameCap(int m) { MAX_SIMULTANEOUS_PRODUCTS_SAME_CAP = m; }
}
