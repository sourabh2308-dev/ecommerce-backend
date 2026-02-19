package com.sourabh.payment_service.controller;

import com.sourabh.payment_service.dto.PaymentRequest;
import com.sourabh.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> pay(
            @RequestBody PaymentRequest request) {

        return ResponseEntity.ok(
                paymentService.initiatePayment(request)
        );
    }
}
