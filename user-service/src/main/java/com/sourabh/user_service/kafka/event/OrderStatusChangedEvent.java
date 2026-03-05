package com.sourabh.user_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event DTO representing a change in an order's status.
 * <p>
 * Published by the order-service onto the {@code order.status.changed}
 * topic and consumed by the user-service to trigger notifications
 * and email alerts.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent {

    /** Public UUID that uniquely identifies the order. */
    private String orderUuid;

    /** UUID of the buyer who placed the order. */
    private String buyerUuid;

    /** The previous order status before the transition. */
    private String oldStatus;

    /** The new order status after the transition. */
    private String newStatus;

    /** Total monetary amount of the order (may be {@code null}). */
    private Double totalAmount;

    /** ISO 4217 currency code, e.g. {@code "INR"} (may be {@code null}). */
    private String currency;
}
