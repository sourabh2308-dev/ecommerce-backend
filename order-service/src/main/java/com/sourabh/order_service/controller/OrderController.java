package com.sourabh.order_service.controller;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.response.OrderResponse;
import com.sourabh.order_service.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // =========================
    // CREATE ORDER (BUYER)
    // =========================
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

    @GetMapping("/seller")
    public ResponseEntity<PageResponse<OrderResponse>> sellerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        String role = request.getHeader("X-User-Role");
        String sellerUuid = request.getHeader("X-User-UUID");

        if (!"SELLER".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                orderService.listSellerOrders(page, size, sellerUuid)
        );
    }

    @PutMapping("/{uuid}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String uuid,
            @RequestParam String status,
            HttpServletRequest request) {

        String role = request.getHeader("X-User-Role");
        String userUuid = request.getHeader("X-User-UUID");

        return ResponseEntity.ok(
                orderService.updateOrderStatus(
                        uuid,
                        role,
                        userUuid,
                        status
                )
        );
    }

    @PutMapping("/internal/payment-update/{uuid}")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable String uuid,
            @RequestParam String status) {

        orderService.updatePaymentStatus(uuid, status);
        return ResponseEntity.ok().build();
    }


}
