package com.sourabh.payment_service.controller;

import com.sourabh.payment_service.common.PageResponse;
import com.sourabh.payment_service.dto.*;
import com.sourabh.payment_service.gateway.PaymentGateway;
import com.sourabh.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    
    private final PaymentGateway paymentGateway;

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<String> pay(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.initiatePayment(request, role, buyerUuid));
    }

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentsByBuyer(buyerUuid, page, size));
    }

    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/{uuid}")
    public ResponseEntity<PaymentResponse> getPaymentByUuid(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentByUuid(uuid, role, buyerUuid));
    }

    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/order/{orderUuid}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderUuid(
            @PathVariable String orderUuid,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentByOrderUuid(orderUuid, role, buyerUuid));
    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller")
    public ResponseEntity<PageResponse<PaymentResponse>> getSellerPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String sellerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getSellerPayments(sellerUuid, page, size));
    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller/dashboard")
    public ResponseEntity<SellerDashboardResponse> getSellerDashboard(
            HttpServletRequest httpRequest) {
        String sellerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getSellerDashboard(sellerUuid));
    }

    @PostMapping("/gateway/webhook")
    public ResponseEntity<Void> gatewayWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        String orderId  = (String) payload.get("razorpay_order_id");
        String paymentId = (String) payload.get("razorpay_payment_id");
        String sigPayload = (String) payload.get("razorpay_signature");
        String eventName = payload.get("event") == null ? "" : payload.get("event").toString().toLowerCase();

        String effectiveSig = signature != null ? signature : sigPayload;

        if (isBlank(orderId) || isBlank(paymentId) || isBlank(effectiveSig)) {
            log.warn("Rejecting webhook with missing verification fields: orderIdPresent={}, paymentIdPresent={}, signaturePresent={}",
                    !isBlank(orderId), !isBlank(paymentId), !isBlank(effectiveSig));
            return ResponseEntity.badRequest().build();
        }

        if (!paymentGateway.verify(orderId, paymentId, effectiveSig)) {
            log.warn("Rejecting webhook with invalid signature for gatewayOrderId={}", orderId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean success = !eventName.contains("failed") && !eventName.contains("refund");
        paymentService.handleGatewayCallback(orderId, success, paymentId);
        return ResponseEntity.ok().build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dashboard")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(paymentService.getAdminDashboard());
    }
}
