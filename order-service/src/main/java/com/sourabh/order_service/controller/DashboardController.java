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
 * REST controller that provides aggregated dashboard metrics for the
 * e-commerce platform's order subsystem.
 *
 * <p>Two dashboards are exposed:
 * <ul>
 *   <li><strong>Admin dashboard</strong> — platform-wide KPIs such as total
 *       orders, confirmed/delivered/cancelled counts, return requests, and
 *       cumulative revenue.</li>
 *   <li><strong>Seller dashboard</strong> — seller-scoped KPIs including
 *       total orders, pending/delivered/returned counts, and seller revenue.</li>
 * </ul>
 *
 * <p>Base path: {@code /api/order/dashboard}</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see OrderRepository
 * @see ReturnRequestRepository
 */
@RestController
@RequestMapping("/api/order/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    /** Repository for querying order counts and revenue aggregates. */
    private final OrderRepository orderRepository;

    /** Repository for querying return-request counts. */
    private final ReturnRequestRepository returnRequestRepository;

    /**
     * Retrieves platform-wide order dashboard metrics.
     *
     * <p>Metrics include total orders, counts by status (confirmed, delivered,
     * cancelled), total return requests, and cumulative revenue.</p>
     *
     * @return {@link ResponseEntity} containing an {@link AdminDashboardResponse}
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
     * <p>Metrics include the seller's total orders (across several statuses),
     * pending orders, delivered orders, returned orders, and total revenue.</p>
     *
     * @param sellerUuid UUID of the authenticated seller, extracted from the
     *                   {@code X-User-UUID} header
     * @return {@link ResponseEntity} containing a {@link SellerDashboardResponse}
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
