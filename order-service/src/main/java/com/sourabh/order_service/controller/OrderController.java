package com.sourabh.order_service.controller;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.response.OrderResponse;
import com.sourabh.order_service.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // =========================
    // CREATE ORDER (BUYER)
    // =========================
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        String role = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");

        OrderResponse response =
                orderService.createOrder(request, role, buyerUuid);

        return ResponseEntity.ok(response);
    }

    // =========================
    // LIST ORDERS
    // =========================
    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        String role = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");

        PageResponse<OrderResponse> response =
                orderService.listOrders(page, size, role, buyerUuid);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller")
    public ResponseEntity<PageResponse<OrderResponse>> sellerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        String sellerUuid = request.getHeader("X-User-UUID");

        return ResponseEntity.ok(
                orderService.listSellerOrders(page, size, sellerUuid)
        );
    }

    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @PutMapping("/{uuid}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String uuid,
            @RequestParam String status,
            HttpServletRequest request) {

        String role = request.getHeader("X-User-Role");
        String userUuid = request.getHeader("X-User-UUID");

        return ResponseEntity.ok(
                orderService.updateOrderStatus(uuid, role, userUuid, status)
        );
    }

    @PutMapping("/internal/payment-update/{uuid}")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable String uuid,
            @RequestParam String status) {

        orderService.updatePaymentStatus(uuid, status);
        return ResponseEntity.ok().build();
    }

    // =========================
    // GET SINGLE ORDER
    // =========================
    @GetMapping("/{uuid}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String role     = httpRequest.getHeader("X-User-Role");
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(orderService.getOrderByUuid(uuid, role, userUuid));
    }

}
