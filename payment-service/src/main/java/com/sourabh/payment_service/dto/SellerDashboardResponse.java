package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Seller's financial dashboard overview.
 */
@Getter
@Builder
public class SellerDashboardResponse {
    private String sellerUuid;
    private Double totalEarnings;
    private Double pendingPayouts;
    private Double completedPayouts;
    private Long totalOrders;
}
