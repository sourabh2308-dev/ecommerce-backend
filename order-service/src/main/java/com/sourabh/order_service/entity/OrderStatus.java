package com.sourabh.order_service.entity;

/**
 * Enumeration of all possible lifecycle states an {@link Order} can transition
 * through, from initial creation to final delivery or return resolution.
 *
 * <p>Status transitions are enforced by the order service's business logic.
 * Typical happy-path flow:
 * {@code CREATED → CONFIRMED → SHIPPED → DELIVERED}.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see Order
 */
public enum OrderStatus {

    /** Order has been created but payment has not yet been confirmed. */
    CREATED,

    /** Payment succeeded and the order is confirmed for fulfilment. */
    CONFIRMED,

    /** Order has been shipped by the seller / warehouse. */
    SHIPPED,

    /** Order has been delivered to the buyer. */
    DELIVERED,

    /** Order has been cancelled (by buyer, seller, or system). */
    CANCELLED,

    /** Buyer has requested a return or exchange for this order. */
    RETURN_REQUESTED,

    /** A pickup has been scheduled for the returned item. */
    PICKUP_SCHEDULED,

    /** Returned item has been picked up from the buyer. */
    PICKED_UP,

    /** Returned item has been received at the warehouse. */
    RETURN_RECEIVED,

    /** An exchange replacement has been issued to the buyer. */
    EXCHANGE_ISSUED,

    /** A monetary refund has been issued to the buyer. */
    REFUND_ISSUED,

    /** The return request was rejected by an admin. */
    RETURN_REJECTED
}
