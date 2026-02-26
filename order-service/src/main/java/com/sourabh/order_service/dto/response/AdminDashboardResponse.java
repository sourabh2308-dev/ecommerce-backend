package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Aggregated order metrics for admin dashboards.
 */
@Data
@Builder
public class AdminDashboardResponse {
    private long totalOrders;
    private long confirmedOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long returnRequests;
    private double totalRevenue;
}
