package com.sourabh.order_service.audit;

/**
 * Enumeration of all auditable actions that can be recorded in the {@link AuditLog}.
 *
 * <p>Each constant represents a distinct business operation tracked for
 * compliance, debugging, and analytics purposes within the order-service.</p>
 *
 * @see Auditable
 * @see AuditAspect
 */
public enum AuditAction {

    /** A new resource was created (e.g., a new order placed). */
    CREATE,

    /** An existing resource was modified. */
    UPDATE,

    /** A resource was permanently removed. */
    DELETE,

    /** A pending action or request was approved by an admin. */
    APPROVE,

    /** A pending action or request was rejected by an admin. */
    REJECT,

    /** An order was confirmed and is ready for processing. */
    CONFIRM,

    /** An order was cancelled before fulfilment. */
    CANCEL,

    /** An order was handed over to the shipping carrier. */
    SHIP,

    /** An order was successfully delivered to the buyer. */
    DELIVER,

    /** A refund was issued for a returned or cancelled order. */
    REFUND,

    /** A resource was viewed / read (used for sensitive-data access tracking). */
    VIEW,

    /** A user logged into the system. */
    LOGIN,

    /** A user logged out of the system. */
    LOGOUT
}
