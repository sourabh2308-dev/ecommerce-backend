package com.sourabh.payment_service.controller;

import com.sourabh.payment_service.common.PageResponse;
import com.sourabh.payment_service.dto.*;
import com.sourabh.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
/**
 * REST API CONTROLLER - Handles HTTP Requests
 * 
 * This controller exposes REST endpoints for HTTP clients (API Gateway, web browsers).
 * Each endpoint method:
 *   1. Validates request parameters and body with @Valid
 *   2. Extracts user context from headers (X-User-UUID, X-User-Role)
 *   3. Delegates business logic to Service layer
 *   4. Returns JSON response via ResponseEntity
 * 
 * Authorization:
 *   - @PreAuthorize: Spring Security checks user role before method execution
 *   - Headers injected by API Gateway after JWT validation
 */
public class PaymentController {

    private final PaymentService paymentService;

    // =========================
    // INITIATE PAYMENT (BUYER)
    // =========================
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<String> pay(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.initiatePayment(request, role, buyerUuid));
    }

    // =========================
    // GET MY PAYMENTS (BUYER paginated)
    // =========================
    @PreAuthorize("hasRole('BUYER')")
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

    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentsByBuyer(buyerUuid, page, size));
    }

    // =========================
    // GET PAYMENT BY UUID (BUYER own / ADMIN any)
    // =========================
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/{uuid}")
    public ResponseEntity<PaymentResponse> getPaymentByUuid(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentByUuid(uuid, role, buyerUuid));
    }

    // =========================
    // GET PAYMENT BY ORDER UUID (BUYER own / ADMIN any)
    // =========================
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/order/{orderUuid}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderUuid(
            @PathVariable String orderUuid,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentByOrderUuid(orderUuid, role, buyerUuid));
    }

    // =========================
    // SELLER: MY PAYMENTS (paginated splits)
    // =========================
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller")
    public ResponseEntity<PageResponse<PaymentSplitResponse>> getSellerPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String sellerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getSellerPayments(sellerUuid, page, size));
    }

    // =========================
    // SELLER: FINANCIAL DASHBOARD
    // =========================
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller/dashboard")
    public ResponseEntity<SellerDashboardResponse> getSellerDashboard(
            HttpServletRequest httpRequest) {
        String sellerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getSellerDashboard(sellerUuid));
    }

    // =========================
    // ADMIN: FINANCIAL DASHBOARD
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dashboard")
    /**
     * GETADMINDASHBOARD - Method Documentation
     *
     * PURPOSE:
     * This method handles the getAdminDashboard operation.
     *
     * RETURN VALUE:
     * @return ResponseEntity<AdminDashboardResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @PreAuthorize - Security check before method execution
     * @GetMapping - REST endpoint handler
     *
     */
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(paymentService.getAdminDashboard());
    }
}
