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
    private Cap cap;
    private Ring ring1;
    private Ring ring2;
    private Ring ring3;
    private Base base;
    private boolean competitive;
}
