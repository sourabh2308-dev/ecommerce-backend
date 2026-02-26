package com.sourabh.order_service.controller;

import com.sourabh.order_service.dto.response.AdminDashboardResponse;
import com.sourabh.order_service.dto.response.SellerDashboardResponse;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.ReturnRequest;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for order-related dashboard metrics.
 * 
 * <p>Provides aggregate statistics and KPIs for:
 * <ul>
 *   <li>ADMIN: Platform-wide order metrics (total orders, revenue, returns)</li>
 *   <li>SELLER: Seller-specific order metrics (their orders, deliveries, returns)</li>
 * </ul>
 * 
 * <p>Used by frontend dashboards to display business intelligence
 * and performance metrics.
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@RestController
@RequestMapping("/api/order/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    /**
     * Retrieves platform-wide order dashboard metrics for admins.
     * 
     * <p>Metrics included:
     * <ul>
     *   <li>Total orders in the system</li>
     *   <li>Confirmed, delivered, cancelled order counts</li>
     *   <li>Total return requests</li>
     *   <li>Total revenue generated</li>
     * </ul>
     * 
     * @return ResponseEntity with admin dashboard statistics
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> adminDashboard() {
        return ResponseEntity.ok(AdminDashboardResponse.builder()
                .totalOrders(orderRepository.countByIsDeletedFalse())
                .confirmedOrders(orderRepository.countByStatus(OrderStatus.CONFIRMED))
                .deliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .returnRequests(returnRequestRepository.count())
                .totalRevenue(orderRepository.sumTotalRevenue())
                .build());
    }

    /**
     * Retrieves seller-specific order dashboard metrics.
     * 
     * <p>Metrics included:
     * <ul>
     *   <li>Total orders for this seller</li>
     *   <li>Pending (unconfirmed) orders</li>
     *   <li>Delivered orders</li>
     *   <li>Returned orders</li>
     *   <li>Total revenue for this seller</li>
     * </ul>
     * 
     * @param sellerUuid the UUID of the seller from JWT
     * @return ResponseEntity with seller dashboard statistics
     */
    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerDashboardResponse> sellerDashboard(
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(SellerDashboardResponse.builder()
                .totalOrders(orderRepository.countBySellerAndStatus(sellerUuid, OrderStatus.CONFIRMED)
                        + orderRepository.countBySellerAndStatus(sellerUuid, OrderStatus.DELIVERED)
                        + orderRepository.countBySellerAndStatus(sellerUuid, OrderStatus.SHIPPED)
                        + orderRepository.countBySellerAndStatus(sellerUuid, OrderStatus.CREATED))
                .pendingOrders(orderRepository.countBySellerAndStatus(sellerUuid, OrderStatus.CREATED))
                .deliveredOrders(orderRepository.countBySellerAndStatus(sellerUuid, OrderStatus.DELIVERED))
                .returnedOrders(orderRepository.countBySellerAndStatus(sellerUuid, OrderStatus.RETURN_REQUESTED))
                .totalRevenue(orderRepository.sumRevenueForSeller(sellerUuid))
                .build());
    }
}
