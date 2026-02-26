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

/**
 * ORDER CONTROLLER - REST API Endpoints for Order Management
 *
 * This controller handles all order-related REST endpoints. It enforces authorization via
 * @PreAuthorize, extracts user context from headers (injected by API Gateway), and delegates
 * business logic to OrderService. Responses are in JSON format.
 *
 * BASE PATH: /api/order
 * Auth: JWT validation at API Gateway + role-based @PreAuthorize on methods
 */
// REST API Controller - Handles HTTP requests and responses
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/order - Create new order (BUYER only)
     *
     * Creates a new order with provided items and shipping details. Only BUYERs can access.
     * The endpoint is part of a saga pattern: order creation triggers async payment processing.
     * Response contains order details with paymentStatus = PENDING until payment completes.
     *
     * Request: Valid CreateOrderRequest with items and shipping address
     * Response: OrderResponse with generated UUID and initial status CREATED
     * Authorization: @PreAuthorize("hasRole('BUYER')")
     * Errors: 400 (validation), 401 (auth), 403 (role), 404 (product not found)
     */
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

    /**
     * GET /api/order - List orders with pagination (BUYER/ADMIN)
     *
     * Returns paginated list of orders filtered by role:
     * - BUYER: Only their own orders
     * - ADMIN: All orders
     *
     * Query params: page (0-indexed), size (default 10)
     * Response: PageResponse with orders, pagination metadata
     * Sorting: By createdAt DESC (newest first)
     */
    /**

     * API ENDPOINT

     * 

     * HTTP Method: GET

     * 

     * PURPOSE:

     * Handles HTTP requests for this endpoint. Validates input, delegates to service

     * layer for business logic, and returns JSON response.

     * 

     * PROCESS FLOW:

     * 1. API Gateway forwards request after JWT validation

     * 2. Spring deserializes JSON to request object

     * 3. @Valid triggers bean validation (if present)

     * 4. @PreAuthorize checks user role (if present)

     * 5. Service layer executes business logic

     * 6. Response object serialized to JSON

     * 7. HTTP status code sent (200/201/400/403/404/500)

     * 

     * SECURITY:

     * - JWT validation at API Gateway (user authenticated)

     * - Role-based access via @PreAuthorize annotation

     * - Input validation via @Valid and constraint annotations

     * 

     * ERROR HANDLING:

     * - Service exceptions caught by GlobalExceptionHandler

     * - Returns standardized error response with HTTP status

     */

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

    /**
     * GET /api/order/seller - List seller's orders (SELLER only)
     *
     * Returns paginated orders for the authenticated seller.
     * Only sellers can access their own order list.
     */
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

    /**
     * GET /api/order/{uuid} - Retrieve single order details
     *
     * Fetches complete order details including items, shipping info, and payment status.
     * Authorization is enforced at service layer:
     * - BUYER: Can only see their own orders
     * - SELLER: Can see orders containing their items
     * - ADMIN: Can see any order
     *
     * Path vars: uuid (order ID)
     * Response: OrderResponse with all order details
     * Errors: 404 (order not found), 403 (no permission)
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String role     = httpRequest.getHeader("X-User-Role");
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(orderService.getOrderByUuid(uuid, role, userUuid));
    }

    /**
     * PUT /api/order/{uuid}/status - Update order status
     *
     * Updates order status. Different roles can perform different transitions:
     * - ADMIN/SELLER: Can update status (exact transitions controlled by service layer)
     *
     * Path vars: uuid (order ID)
     * Query params: status (new status value)
     * Response: Updated OrderResponse
     */
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

    /**
     * PUT /api/order/internal/payment-update/{uuid} - Internal payment saga endpoint
     *
     * INTERNAL ENDPOINT - Called only by payment-service, not exposed to clients.
     * Updates order payment status after payment processing completes.
     * Part of async saga pattern: payment service publishes event → calls this endpoint.
     *
     * Path vars: uuid (order ID)
     * Query params: status (payment result: SUCCESS or FAILED)
     * Response: 200 OK empty body
     * Auth: Validated by API Gateway (X-Internal-Secret header)
     *
     * Saga flow:
     * 1. Client creates order (POST /api/order)
     * 2. Service publishes order.created event
     * 3. Payment service processes payment
     * 4. Payment service calls this endpoint with result
     * 5. Order status updated, client polls GET /api/order/{uuid}
     */
    @PutMapping("/internal/payment-update/{uuid}")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable String uuid,
            @RequestParam String status) {

        orderService.updatePaymentStatus(uuid, status);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/order/{uuid}/sub-orders - Get all sub-orders for a parent order
     * 
     * Used to view order splits for multi-seller orders.
     * Returns empty list if order is not split.
     * 
     * Response: 200 OK with list of OrderResponse
     * Auth: Buyer can only see their own orders, admin can see all
     */
    @GetMapping("/{uuid}/sub-orders")
    public ResponseEntity<List<OrderResponse>> getSubOrders(
            @PathVariable String uuid,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid) {

        List<OrderResponse> subOrders = orderService.getSubOrders(uuid, role, userUuid);
        return ResponseEntity.ok(subOrders);
    }

    /**
     * GET /api/order/group/{groupId} - Get all orders in a group
     * 
     * Returns main order + all sub-orders sharing the same orderGroupId.
     * 
     * Response: 200 OK with list of OrderResponse
     * Auth: Buyer can only see their own orders, admin can see all
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<OrderResponse>> getOrderGroup(
            @PathVariable String groupId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid) {

        List<OrderResponse> groupOrders = orderService.getOrderGroup(groupId, role, userUuid);
        return ResponseEntity.ok(groupOrders);
    }
}
