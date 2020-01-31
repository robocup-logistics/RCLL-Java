package com.grips.model.teamserver;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Order {
    private Cap cap;
    private Ring ring1;
    private Ring ring2;
    private Ring ring3;
    private Base base;
    private boolean competitive;
}
