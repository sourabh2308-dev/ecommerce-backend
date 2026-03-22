package com.sourabh.order_service.kafka.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class OrderCreatedEvent {

    public static final String TOPIC = "order.created";

    private String eventId = UUID.randomUUID().toString();

    private String orderUuid;

    private String buyerUuid;

    private List<OrderItemEvent> items;

    private double totalAmount;

    public OrderCreatedEvent(String orderUuid, String buyerUuid, List<OrderItemEvent> items, double totalAmount) {
        this.eventId = UUID.randomUUID().toString();
        this.orderUuid = orderUuid;
        this.buyerUuid = buyerUuid;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    public OrderCreatedEvent(String eventId, String orderUuid, String buyerUuid, List<OrderItemEvent> items, double totalAmount) {
        this.eventId = eventId;
        this.orderUuid = orderUuid;
        this.buyerUuid = buyerUuid;
        this.items = items;
        this.totalAmount = totalAmount;
    }
}
