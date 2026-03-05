package com.sourabh.user_service.entity;

/**
 * Broad category assigned to a {@link SupportTicket} so that
 * support agents can triage and route tickets efficiently.
 *
 * @see SupportTicket
 */
public enum TicketCategory {

    /** Issue related to an existing order (status, tracking, etc.). */
    ORDER_ISSUE,

    /** Issue related to payment processing or billing. */
    PAYMENT_ISSUE,

    /** General question about a product listing. */
    PRODUCT_INQUIRY,

    /** Problem with shipping or delivery. */
    DELIVERY_ISSUE,

    /** Request to return an item or obtain a refund. */
    RETURN_REFUND,

    /** Issue related to user account access or settings. */
    ACCOUNT_ISSUE,

    /** Catch-all for issues that do not fit other categories. */
    OTHER
}
