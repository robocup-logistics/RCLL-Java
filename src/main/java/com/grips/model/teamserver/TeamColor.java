package com.grips.model.teamserver;

import org.robocup_logistics.llsf_msgs.TeamProtos;

public enum TeamColor {
    CYAN,
    MAGENTA;

    public static TeamColor fromString(String str) {
        if (str.equalsIgnoreCase("magenta")) {
            return MAGENTA;
        } else if (str.equalsIgnoreCase("cyan")) {
            return CYAN;
        }
        throw new RuntimeException("Unkown TeamColor: " + str);
    }
}