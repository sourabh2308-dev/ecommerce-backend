package com.sourabh.order_service.entity;

/**
 * Enumeration representing the payment lifecycle states of an {@link Order}.
 *
 * <p>Payment processing is handled asynchronously by the payment-service via
 * Kafka events. The order-service updates this status when it receives a
 * {@link com.sourabh.order_service.kafka.event.PaymentCompletedEvent}.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see Order
 */
public enum PaymentStatus {

    /** Payment has been initiated but not yet confirmed by the payment gateway. */
    PENDING,

    /** Payment was successfully processed and funds have been captured. */
    SUCCESS,

    /** Payment attempt failed (e.g. insufficient funds, gateway error). */
    FAILED,

    /** A previously successful payment has been refunded to the buyer. */
    REFUNDED
}
