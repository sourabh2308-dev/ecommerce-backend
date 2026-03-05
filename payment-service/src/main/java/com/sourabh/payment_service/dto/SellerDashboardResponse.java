package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO carrying the seller-specific financial dashboard data returned via
 * {@code GET /api/payment/seller/dashboard}.
 *
 * <p>Aggregations are derived from {@code PaymentSplit} records belonging
 * to the authenticated seller.
 */
@Getter
@Builder
public class SellerDashboardResponse {

    /** UUID of the seller this dashboard belongs to. */
    private String sellerUuid;

    /** Lifetime earnings (completed + pending payouts). */
    private Double totalEarnings;

    /** Payouts not yet settled (split status = PENDING). */
    private Double pendingPayouts;

    /** Payouts already settled (split status = COMPLETED). */
    private Double completedPayouts;

    /** Number of distinct orders the seller has fulfilled. */
    private Long totalOrders;
}
