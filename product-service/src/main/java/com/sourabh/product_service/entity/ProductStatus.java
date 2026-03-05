package com.sourabh.product_service.entity;

/**
 * Enumeration of possible lifecycle states for a {@link Product}.
 * <p>
 * A product transitions through these states during its lifetime:
 * <ul>
 *   <li>{@link #DRAFT} – newly created, pending admin approval</li>
 *   <li>{@link #ACTIVE} – approved and visible to buyers</li>
 *   <li>{@link #BLOCKED} – suspended by an admin (hidden from listings)</li>
 *   <li>{@link #OUT_OF_STOCK} – stock depleted; automatically set when stock reaches zero</li>
 * </ul>
 * </p>
 */
public enum ProductStatus {

    /** Product created by seller, awaiting admin approval. */
    DRAFT,

    /** Product approved and publicly visible in the catalogue. */
    ACTIVE,

    /** Product suspended by an administrator. */
    BLOCKED,

    /** Product stock has been fully depleted. */
    OUT_OF_STOCK
}

