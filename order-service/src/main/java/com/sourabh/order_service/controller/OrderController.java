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
 * Primary REST controller for order lifecycle management.
 *
 * <p>Handles creation, retrieval, status updates, and payment-status updates
 * for orders. Authorization is enforced via {@code @PreAuthorize} annotations;
 * user context (UUID, role) is extracted from headers injected by the API
 * Gateway after JWT validation. Business logic is delegated to
 * {@link OrderService}.</p>
 *
 * <p>Key flows:
 * <ul>
 *   <li><strong>Order creation</strong> — triggers the payment saga and
 *       publishes an {@code order.created} Kafka event.</li>
 *   <li><strong>Payment update</strong> — internal endpoint called by the
 *       payment-service to complete the saga.</li>
 *   <li><strong>Multi-seller splitting</strong> — sub-orders and order groups
 *       can be queried via dedicated endpoints.</li>
 * </ul>
 *
 * <p>Base path: {@code /api/order}</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see OrderService
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    /** Service layer encapsulating order business logic. */
    private final OrderService orderService;

    /**
     * Creates a new order for the authenticated buyer.
     *
     * <p>The order is persisted with status {@code CREATED} and payment status
     * {@code PENDING}. An asynchronous payment saga is initiated via Kafka so
     * the buyer can poll for the final payment result.</p>
     *
     * @param request     validated {@link CreateOrderRequest} containing items
     *                    and shipping details
     * @param httpRequest the servlet request carrying gateway-injected headers
     * @return {@link ResponseEntity} containing the created {@link OrderResponse}
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
     * Returns a paginated list of orders, filtered by the caller's role.
     *
     * <ul>
     *   <li>{@code BUYER} — sees only their own orders.</li>
     *   <li>{@code ADMIN} — sees all orders.</li>
     * </ul>
     *
     * <p>Results are sorted by creation date descending (newest first).</p>
     *
     * @param page        zero-based page index (default {@code 0})
     * @param size        number of records per page (default {@code 10})
     * @param httpRequest the servlet request carrying gateway-injected headers
     * @return {@link ResponseEntity} containing a {@link PageResponse} of
     *         {@link OrderResponse}
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
     * Returns a paginated list of orders containing items sold by the
     * authenticated seller.
     *
     * @param page    zero-based page index (default {@code 0})
     * @param size    number of records per page (default {@code 10})
     * @param request the servlet request carrying gateway-injected headers
     * @return {@link ResponseEntity} containing a {@link PageResponse} of
     *         {@link OrderResponse}
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
     * Retrieves full details of a single order by its UUID.
     *
     * <p>Visibility rules are enforced at the service layer:
     * <ul>
     *   <li>{@code BUYER} — may view only their own orders.</li>
     *   <li>{@code SELLER} — may view orders containing their items.</li>
     *   <li>{@code ADMIN} — may view any order.</li>
     * </ul>
     *
     * @param uuid        the order UUID
     * @param httpRequest the servlet request carrying gateway-injected headers
     * @return {@link ResponseEntity} containing the {@link OrderResponse}
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
     * Updates the lifecycle status of an order.
     *
     * <p>Allowed transitions depend on the caller's role and are enforced by
     * the service layer. Optionally accepts return metadata when the new
     * status involves a return request.</p>
     *
     * @param uuid         the order UUID
     * @param status       the target {@link com.sourabh.order_service.entity.OrderStatus} as a string
     * @param returnType   optional return type ({@code REFUND} or {@code EXCHANGE})
     * @param returnReason optional free-text reason for the return
     * @param request      the servlet request carrying gateway-injected headers
     * @return {@link ResponseEntity} containing the updated {@link OrderResponse}
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
     * Internal endpoint invoked by the payment-service to report the outcome
     * of payment processing as part of the asynchronous saga.
     *
     * <p>Not exposed to end-users; protected by the {@code X-Internal-Secret}
     * header validated at the API Gateway.</p>
     *
     * @param uuid   the order UUID
     * @param status the payment result ({@code SUCCESS} or {@code FAILED})
     * @return {@link ResponseEntity} with HTTP 200 and empty body
     */
    @PutMapping("/internal/payment-update/{uuid}")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable String uuid,
            @RequestParam String status) {

        orderService.updatePaymentStatus(uuid, status);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves all sub-orders derived from a multi-seller parent order.
     *
     * <p>Returns an empty list if the order was not split.</p>
     *
     * @param uuid     UUID of the parent order
     * @param role     the caller's role (from {@code X-User-Role} header)
     * @param userUuid the caller's UUID (from {@code X-User-UUID} header)
     * @return {@link ResponseEntity} containing a list of sub-order
     *         {@link OrderResponse} objects
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
     * Retrieves all orders sharing the same order group identifier, including
     * the main order and every sub-order.
     *
     * @param groupId  the shared order group ID
     * @param role     the caller's role (from {@code X-User-Role} header)
     * @param userUuid the caller's UUID (from {@code X-User-UUID} header)
     * @return {@link ResponseEntity} containing a list of grouped
     *         {@link OrderResponse} objects
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
