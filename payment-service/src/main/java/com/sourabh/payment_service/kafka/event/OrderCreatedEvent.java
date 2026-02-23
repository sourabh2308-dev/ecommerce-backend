package com.sourabh.payment_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published by order-service when a new order is placed.
 * Payment-service consumes this to trigger automatic payment processing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    /** Unique event ID used for idempotency — deduplication before processing */
    private String eventId;
    private String orderUuid;
    private String buyerUuid;
    private String productUuid;
    private int quantity;
    private double totalAmount;
}
