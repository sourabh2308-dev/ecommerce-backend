package com.sourabh.user_service.entity;

/**
 * Categorises each {@link LoyaltyPoint} ledger entry so that the
 * system can distinguish between different sources and sinks of
 * loyalty points.
 *
 * @see LoyaltyPoint
 */
public enum PointsTransactionType {

    /** Points credited when a purchase order is completed. */
    EARNED_ORDER,

    /** Points credited when the user writes a product review. */
    EARNED_REVIEW,

    /** Points credited through the referral programme. */
    EARNED_REFERRAL,

    /** Points debited when the user redeems them at checkout. */
    REDEEMED,

    /** Points voided due to expiration (inactivity beyond threshold). */
    EXPIRED,

    /** Points manually added or removed by a platform administrator. */
    ADMIN_ADJUSTMENT
}
