package com.sourabh.payment_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponse {

    private Double totalGrossRevenue;

    private Double totalPlatformEarnings;

    private Double totalDeliveryFees;

    private Double totalSellerPayouts;

    private Long totalCompletedOrders;

    private Long activeSellers;
}
