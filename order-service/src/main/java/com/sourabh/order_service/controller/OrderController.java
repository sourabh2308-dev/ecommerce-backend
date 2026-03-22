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

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

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

    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
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

    @GetMapping("/{uuid}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String role     = httpRequest.getHeader("X-User-Role");
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(orderService.getOrderByUuid(uuid, role, userUuid));
    }

    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN', 'SELLER')")
    @PutMapping("/{uuid}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String uuid,
            @RequestParam String status,
            @RequestParam(required = false) String returnType,
            @RequestParam(required = false) String returnReason,
            HttpServletRequest request) {

        String role = request.getHeader("X-User-Role");
        String userUuid = request.getHeader("X-User-UUID");

        return ResponseEntity.ok(
            orderService.updateOrderStatus(uuid, role, userUuid, status, returnType, returnReason)
        );
    }

    @PutMapping("/internal/payment-update/{uuid}")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable String uuid,
            @RequestParam String status) {

        orderService.updatePaymentStatus(uuid, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{uuid}/sub-orders")
    public ResponseEntity<List<OrderResponse>> getSubOrders(
            @PathVariable String uuid,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid) {

        List<OrderResponse> subOrders = orderService.getSubOrders(uuid, role, userUuid);
        return ResponseEntity.ok(subOrders);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<OrderResponse>> getOrderGroup(
            @PathVariable String groupId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid) {

        List<OrderResponse> groupOrders = orderService.getOrderGroup(groupId, role, userUuid);
        return ResponseEntity.ok(groupOrders);
    }
}
