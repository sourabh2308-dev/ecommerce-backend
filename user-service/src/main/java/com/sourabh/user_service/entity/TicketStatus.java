package com.sourabh.user_service.entity;

/**
 * Lifecycle states of a {@link SupportTicket}, representing the
 * progression from initial creation through to final closure.
 *
 * @see SupportTicket
 */
public enum TicketStatus {

    /** Ticket has been created but not yet picked up by support. */
    OPEN,

    /** A support agent is actively working on the ticket. */
    IN_PROGRESS,

    /** The support agent is waiting for a response from the customer. */
    AWAITING_CUSTOMER,

    /** The issue has been resolved by the support team. */
    RESOLVED,

    /** The ticket is closed and no further action is expected. */
    CLOSED
}
