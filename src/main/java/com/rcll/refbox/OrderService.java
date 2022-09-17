package com.rcll.refbox;

import com.rcll.domain.*;
import lombok.Synchronized;
import org.robocup_logistics.llsf_msgs.OrderInfoProtos;

import java.util.List;
import java.util.stream.Collectors;

public class OrderService {
    private OrderInfoProtos.OrderInfo orderInfo;
    private final MachineClient machineClient;
    private final TeamColor team;

    public OrderService(MachineClient machineClient, TeamColor team) {
        this.machineClient = machineClient;
        orderInfo = null;
        this.team = team;
    }

    @Synchronized
    public void update(OrderInfoProtos.OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    @Synchronized
    public List<Order> getOrders() {
        return this.orderInfo.getOrdersList().stream()
                .map(this::protoOrderToOrder)
                .collect(Collectors.toList());
    }

    @Synchronized
    public Order getOrder(int id) {
        return getOrderNonLocked(id);
    }

    private Order getOrderNonLocked(int id) {
        var protoOrder = orderInfo.getOrdersList().stream()
                .filter(order -> order.getId() == id)
                .findFirst();
        if (protoOrder.isPresent()) {
            return protoOrderToOrder(protoOrder.get());
        } else {
            throw new RuntimeException("Unknown Order: " + id);
        }
    }

    private Order protoOrderToOrder(OrderInfoProtos.Order protoOrder) {
        int delivered = 0;
        switch (team) {
            case CYAN:
                delivered = protoOrder.getQuantityDeliveredCyan();
                break;
            case MAGENTA:
                delivered = protoOrder.getQuantityDeliveredMagenta();
                break;
        }
        Cap cap;
        switch (protoOrder.getCapColor()) {
            case CAP_BLACK:
                cap = Cap.Black;
                break;
            case CAP_GREY:
                cap = Cap.Grey;
                break;
            default:
                throw new RuntimeException("Unknown CapColor: " + protoOrder.getCapColor());
        }
        Base base;
        switch (protoOrder.getBaseColor()) {
            case BASE_RED:
                base = Base.Red;
                break;
            case BASE_BLACK:
                base = Base.Black;
                break;
            case BASE_SILVER:
                base = Base.Silver;
                break;
            default:
                throw new RuntimeException("Unknown BaseColor: " + protoOrder.getBaseColor());
        }
        Ring ring1 = ringForIndex(protoOrder, 0);
        Ring ring2 = ringForIndex(protoOrder, 1);
        Ring ring3 = ringForIndex(protoOrder, 2);
        return new Order(protoOrder.getId(), protoOrder.getQuantityRequested(), delivered,
                protoOrder.getDeliveryPeriodBegin(), protoOrder.getDeliveryPeriodEnd(), cap,
                ring1, ring2, ring3, base, protoOrder.getCompetitive());
    }

    private Ring ringForIndex(OrderInfoProtos.Order protoOrder, int index) {
        if (protoOrder.getRingColorsCount() <= index) {
            return null;
        }
        return machineClient.getRingForProtoRing(protoOrder.getRingColors(index));
    }
}
