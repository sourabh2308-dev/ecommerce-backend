package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO containing aggregated order metrics scoped to a specific
 * seller, intended for the seller dashboard.
 *
 * <p>Provides counts of orders by fulfilment state, return activity,
 * and the seller's total revenue.</p>
 */
@Data
@Builder
public class SellerDashboardResponse {

    /** Total number of orders containing this seller's products. */
    private long totalOrders;

    /** Number of orders pending confirmation or shipment. */
    private long pendingOrders;

    /** Number of orders successfully delivered. */
    private long deliveredOrders;

    /** Number of orders that have been returned by buyers. */
    private long returnedOrders;

    /** Cumulative revenue from the seller's orders (in the platform's base currency). */
    private double totalRevenue;
}
