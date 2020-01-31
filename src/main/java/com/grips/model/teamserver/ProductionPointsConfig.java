package com.grips.model.teamserver;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductionPointsConfig {
    private int bufferBaseInRS;
    private int bufferCapInCS;
    private int finishCC0;
    private int finishCC1;
    private int finishCC2;
    private int lastRingC1;
    private int lastRingC2;
    private int lastRingC3;
    private int mountCap;
    private int inTimeDelivery;
    private int lateDelivery;
    private int competitivePoints;
    private int competitiveDeduction;
}
