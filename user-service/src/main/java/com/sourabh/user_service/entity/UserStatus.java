package com.sourabh.user_service.entity;

/**
 * Lifecycle states of a {@link User} account, from initial
 * registration through full activation, blocking, or deletion.
 *
 * @see User
 */
public enum UserStatus {

    /** Email has been registered but not yet verified via OTP. */
    PENDING_VERIFICATION,

    /** Seller has verified their email but has not yet submitted business / ID details. */
    PENDING_DETAILS,

    /** Seller has submitted verification details and is awaiting admin approval. */
    PENDING_APPROVAL,

    /** Account is fully active and the user may transact on the platform. */
    ACTIVE,

    /** Account has been suspended by an administrator. */
    BLOCKED,

    /** Account has been soft-deleted and is no longer accessible. */
    DELETED
}
