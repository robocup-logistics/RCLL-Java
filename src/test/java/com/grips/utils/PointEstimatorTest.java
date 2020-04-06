package com.grips.utils;

import com.grips.model.teamserver.Base;
import com.grips.model.teamserver.Cap;
import com.grips.model.teamserver.Order;
import com.grips.model.teamserver.ProductionPointsConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PointEstimatorTest {

    @Test
    public void testC0() {
        Assertions.assertThat(test(Order.builder()
                .cap(Cap.Grey)
                .base(Base.Black)
                .build(), true, false)).isEqualTo(32);
    }

    @Test
    public void testC0Late() {
        Assertions.assertThat(test(Order.builder()
                .cap(Cap.Grey)
                .base(Base.Black)
                .build(), false, false)).isEqualTo(17);
    }

    private int test(Order order, boolean inTime, boolean deliveredByOtherTeam) {
        return new PointEstimator(ProductionPointsConfig.builder()
                .bufferBaseInRS(2)
                .bufferCapInCS(2)
                .finishCC0(5)
                .finishCC1(10)
                .finishCC2(20)
                .lastRingC1(10)
                .lastRingC2(30)
                .lastRingC3(80)
                .mountCap(10)
                .inTimeDelivery(20)
                .lateDelivery(5)
                .competitiveDeduction(10)
                .competitivePoints(10)
                .build()).estimatePoints(order, inTime, deliveredByOtherTeam);
    }
}
