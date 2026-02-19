package com.sourabh.payment_service.service;

import com.sourabh.payment_service.dto.PaymentRequest;

public interface PaymentService {
    String initiatePayment(PaymentRequest request);
}
