package com.sourabh.order_service.entity;

/**
 * Distinguishes between a buyer-placed main order and a seller-specific sub-order
 * created during multi-seller order splitting.
 *
 * <p>When a buyer's cart contains products from multiple sellers, the platform
 * creates one {@code MAIN} order plus one {@code SUB} order per seller. All
 * related orders share the same {@link Order#getOrderGroupId()}.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see Order
 */
public enum OrderType {

    /**
     * Primary order placed by the buyer, or a single-seller order that
     * does not require splitting.
     */
    MAIN,

    /**
     * Seller-specific sub-order derived from a multi-seller main order.
     * Contains only the items belonging to one seller.
     */
    SUB
}
