package com.sourabh.payment_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kafka event published by the order service on the {@code order.created}
 * topic when a buyer places a new order.
 *
 * <p>The payment service consumes this event to trigger automatic payment
 * processing as part of the Order-Payment Saga.  The {@link #eventId} field
 * provides idempotency — the consumer checks whether the event has already
 * been processed before creating a new {@code Payment}.
 *
 * <p>Multi-item orders are supported via the {@link #items} list; one
 * {@link com.sourabh.payment_service.entity.PaymentSplit} is created per item.
 *
 * @see com.sourabh.payment_service.kafka.consumer.OrderEventConsumer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    /** Unique event identifier used for idempotent deduplication. */
    private String eventId;

    /** UUID of the newly created order. */
    private String orderUuid;

    /** UUID of the buyer who placed the order. */
    private String buyerUuid;

    /** Per-seller/product line items in the order. */
    private List<OrderItemEvent> items;

    /** Total order amount in INR (sum of all item subtotals + fees). */
    private double totalAmount;
}
