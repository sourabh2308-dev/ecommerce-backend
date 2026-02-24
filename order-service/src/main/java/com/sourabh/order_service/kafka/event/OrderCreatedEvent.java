package com.sourabh.order_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    /** Unique event ID used as idempotency key in payment-service */
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
}
