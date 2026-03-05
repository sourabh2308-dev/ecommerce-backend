package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO containing aggregated order metrics intended for the
 * administrator dashboard.
 *
 * <p>Provides a high-level overview of order volume, fulfilment status,
 * return activity, and total revenue across the entire platform.</p>
 */
@Data
@Builder
public class AdminDashboardResponse {

    /** Total number of orders placed on the platform. */
    private long totalOrders;

    /** Number of orders in the {@code CONFIRMED} state. */
    private long confirmedOrders;

    /** Number of orders that have been successfully delivered. */
    private long deliveredOrders;

    /** Number of orders that have been cancelled. */
    private long cancelledOrders;

    /** Number of return requests submitted by buyers. */
    private long returnRequests;

    /** Cumulative revenue from all orders (in the platform's base currency). */
    private double totalRevenue;
}
