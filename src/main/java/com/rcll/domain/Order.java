package com.rcll.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Order {
    private int id;
    private int requested;
    private int delivered;
    private int deliveryPeriodBegin;
    private int deliveryPeriodEnd;
    private int deliveryGate;
    private Cap cap;
    private Ring ring1;
    private Ring ring2;
    private Ring ring3;
    private Base base;
    private boolean competitive;

    public ProductComplexity getComplexity() {
        if (ring3 != null) {
            return ProductComplexity.C3;
        }
        if (ring2 != null) {
            return ProductComplexity.C2;
        }
        if (ring1 != null) {
            return ProductComplexity.C1;
        }
        return ProductComplexity.C0;
    }
}
