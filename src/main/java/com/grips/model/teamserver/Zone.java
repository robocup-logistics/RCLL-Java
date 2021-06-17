package com.grips.model.teamserver;

public class Zone {
    TeamColor color;
    int number;

    @Override
    public String toString() {
        switch (color) {
            case CYAN:
                return "C_Z" + number;
            case MAGENTA:
                return "M_Z" + number;
        }
        throw new RuntimeException("Invalid teamColor: " + this);
    }
}
