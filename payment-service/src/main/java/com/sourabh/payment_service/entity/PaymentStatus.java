package com.sourabh.payment_service.entity;

/**
 * Lifecycle status of a {@link Payment} transaction.
 *
 * <p><b>State transitions:</b>
 * <pre>
 *   INITIATED ──► PENDING ──► SUCCESS
 *                         └──► FAILED
 *
 *   INITIATED ──► SUCCESS   (mock gateway, immediate result)
 *             └──► FAILED
 * </pre>
 *
 * <ul>
 *   <li>{@link #INITIATED} — payment record created, gateway not yet called.</li>
 *   <li>{@link #PENDING}   — gateway returned an order ID; awaiting async callback.</li>
 *   <li>{@link #SUCCESS}   — payment confirmed by the gateway or mock.</li>
 *   <li>{@link #FAILED}    — payment rejected or timed out.</li>
 * </ul>
 */
public enum PaymentStatus {

    /** Payment record just created; gateway call pending. */
    INITIATED,

    /** Awaiting asynchronous confirmation from the external payment gateway. */
    PENDING,

    /** Payment completed successfully. */
    SUCCESS,

    /** Payment was rejected or could not be completed. */
    FAILED
}
