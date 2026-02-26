package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Aggregated order metrics for seller dashboards.
 */
@Data
@Builder
public class SellerDashboardResponse {
    private long totalOrders;
    private long pendingOrders;
    private long deliveredOrders;
    private long returnedOrders;
    private double totalRevenue;
}
