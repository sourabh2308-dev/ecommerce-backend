package com.sourabh.order_service.entity;

/**
 * Enumeration of return resolution types available to buyers when requesting
 * a post-delivery return through the {@link ReturnRequest} workflow.
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ReturnRequest
 * @see Order
 */
public enum ReturnType {

    /** Buyer requests a monetary refund of the order amount. */
    REFUND,

    /** Buyer requests a replacement item instead of a monetary refund. */
    EXCHANGE
}
