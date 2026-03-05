package com.sourabh.user_service.entity;

/**
 * Enumeration of all notification categories used by the
 * {@link Notification} entity.
 * <p>
 * The type determines the icon, colour, and deep-link behaviour
 * rendered by the frontend notification centre.
 * </p>
 *
 * @see Notification
 */
public enum NotificationType {

    /** A new order has been placed by the buyer. */
    ORDER_PLACED,

    /** The seller or system has confirmed the order. */
    ORDER_CONFIRMED,

    /** The order has been handed to the shipping carrier. */
    ORDER_SHIPPED,

    /** The order has been successfully delivered. */
    ORDER_DELIVERED,

    /** The order has been cancelled by the buyer, seller, or system. */
    ORDER_CANCELLED,

    /** Payment was processed successfully. */
    PAYMENT_SUCCESS,

    /** Payment attempt failed. */
    PAYMENT_FAILED,

    /** A return request has been approved. */
    RETURN_APPROVED,

    /** A return request has been rejected. */
    RETURN_REJECTED,

    /** A monetary refund has been issued. */
    REFUND_PROCESSED,

    /** A promotional or marketing notification. */
    PROMOTION,

    /** Notification related to loyalty points activity. */
    LOYALTY_POINTS,

    /** Generic system-level notification. */
    SYSTEM
}
