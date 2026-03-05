package com.sourabh.order_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published whenever an order transitions from one
 * {@link com.sourabh.order_service.entity.OrderStatus} to another.
 *
 * <p>Consumed by the user-service to send email notifications and
 * in-app alerts to the buyer about their order's progress.</p>
 *
 * <p>Published to the {@code order.status.changed} Kafka topic.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent {

    /** UUID of the order whose status changed. */
    private String orderUuid;

    /** UUID of the buyer who placed the order. */
    private String buyerUuid;

    /** Previous order status (e.g. {@code "CREATED"}). */
    private String oldStatus;

    /** New order status (e.g. {@code "CONFIRMED"}). */
    private String newStatus;

    /** Total monetary amount of the order. */
    private Double totalAmount;

    /** ISO 4217 currency code (e.g. {@code "INR"}). */
    private String currency;
}
