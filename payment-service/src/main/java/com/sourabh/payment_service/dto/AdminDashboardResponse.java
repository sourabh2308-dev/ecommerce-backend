package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO carrying the platform-wide financial dashboard data returned to
 * admin users via {@code GET /api/payment/admin/dashboard}.
 *
 * <p>All monetary values are in the default platform currency (INR).
 * Aggregations are computed from completed {@code PaymentSplit} records.
 */
@Getter
@Builder
public class AdminDashboardResponse {

    /** Sum of all item amounts across completed payment splits. */
    private Double totalGrossRevenue;

    /** Sum of platform commission fees collected from sellers. */
    private Double totalPlatformEarnings;

    /** Sum of delivery fees charged to buyers. */
    private Double totalDeliveryFees;

    /** Sum of net amounts paid out to sellers. */
    private Double totalSellerPayouts;

    /** Number of distinct orders that have completed payment processing. */
    private Long totalCompletedOrders;

    /** Number of distinct sellers with at least one completed payout. */
    private Long activeSellers;
}
