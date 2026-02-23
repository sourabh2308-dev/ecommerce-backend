package com.sourabh.payment_service.controller;

import com.sourabh.payment_service.common.PageResponse;
import com.sourabh.payment_service.dto.PaymentRequest;
import com.sourabh.payment_service.dto.PaymentResponse;
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
}
