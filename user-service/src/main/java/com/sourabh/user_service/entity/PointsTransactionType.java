package com.sourabh.user_service.entity;

public enum PointsTransactionType {
    EARNED_ORDER,      // Points earned from order
    EARNED_REVIEW,     // Points earned for writing a review
    EARNED_REFERRAL,   // Points earned from referral
    REDEEMED,          // Points redeemed at checkout
    EXPIRED,           // Points expired
    ADMIN_ADJUSTMENT   // Manual admin adjustment
}
