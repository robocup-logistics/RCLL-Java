package com.rcll.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.robocup_logistics.llsf_msgs.ZoneProtos;

@AllArgsConstructor
@EqualsAndHashCode
public class ZoneName {
    private final String rawZone;

    public boolean isMagenta() {
        return this.rawZone.charAt(0) == 'M';
    }

    public boolean isCyan() {
        return this.rawZone.charAt(0) == 'M';
    }

    public ZoneName mirror() {
        if (this.isMagenta()) {
            return new ZoneName(this.rawZone.replace("M", "C"));
        } else {
            return new ZoneName(this.rawZone.replace("C", "M"));
        }
    }

    public ZoneProtos.Zone toProto() {
        return ZoneProtos.Zone.valueOf(this.rawZone);
    }

    public String toString() {
        return this.rawZone;
    }
}
