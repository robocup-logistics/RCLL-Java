package com.grips.model.teamserver;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Zone {
    final TeamColor color;
    final int number;

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
