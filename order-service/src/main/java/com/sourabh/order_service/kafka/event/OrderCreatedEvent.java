package com.sourabh.order_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Kafka event published when a new order is created in the order-service.
 *
 * <p>Consumed by the payment-service to initiate payment processing as
 * part of the order creation saga. Each event carries a unique
 * {@link #eventId} that the consumer uses as an idempotency key to
 * ensure at-most-once processing.</p>
 *
 * <p>Published to the {@code order.created} Kafka topic.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see OrderItemEvent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    /**
     * Unique event identifier used as an idempotency key by consumers.
     * Auto-generated as a random UUID on construction.
     */
    private String eventId = UUID.randomUUID().toString();

    /** UUID of the newly created order. */
    private String orderUuid;

    /** UUID of the buyer who placed the order. */
    private String buyerUuid;

    /** Line items included in the order. */
    private List<OrderItemEvent> items;

    /** Total monetary amount of the order. */
    private double totalAmount;

    /**
     * Convenience constructor that auto-generates the {@link #eventId}.
     *
     * @param orderUuid   UUID of the order
     * @param buyerUuid   UUID of the buyer
     * @param items       list of order item events
     * @param totalAmount total order amount
     */
    public OrderCreatedEvent(String orderUuid, String buyerUuid, List<OrderItemEvent> items, double totalAmount) {
        this.eventId = UUID.randomUUID().toString();
        this.orderUuid = orderUuid;
        this.buyerUuid = buyerUuid;
        this.items = items;
        this.totalAmount = totalAmount;
    }
}
