package com.sourabh.payment_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event published by order-service when a new order is placed.
 * Payment-service consumes this to trigger automatic payment processing.
 * Now supports multi-item orders with per-seller breakdown.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    /** Unique event ID used for idempotency — deduplication before processing */
    private String eventId;
    private String orderUuid;
    private String buyerUuid;
    private List<OrderItemEvent> items;
    private double totalAmount;
}
