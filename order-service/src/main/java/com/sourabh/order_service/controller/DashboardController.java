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

@RestController
@RequestMapping("/api/order/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderRepository orderRepository;

    private final ReturnRequestRepository returnRequestRepository;

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
