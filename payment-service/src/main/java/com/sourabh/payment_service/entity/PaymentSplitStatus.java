package com.sourabh.payment_service.entity;

/**
 * Settlement status of an individual {@link PaymentSplit} record.
 *
 * <ul>
 *   <li>{@link #PENDING}   — split created but payout not yet settled.</li>
 *   <li>{@link #COMPLETED} — payout has been settled to the seller.</li>
 * </ul>
 */
public enum PaymentSplitStatus {

    /** The seller payout is awaiting settlement. */
    PENDING,

    /** The seller payout has been successfully settled. */
    COMPLETED
}
