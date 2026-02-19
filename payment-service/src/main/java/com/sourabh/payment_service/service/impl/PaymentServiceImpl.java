package com.sourabh.payment_service.service.impl;

import com.sourabh.payment_service.dto.PaymentRequest;
import com.sourabh.payment_service.entity.Payment;
import com.sourabh.payment_service.entity.PaymentStatus;
import com.sourabh.payment_service.repository.PaymentRepository;
import com.sourabh.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    private static final String ORDER_SERVICE_URL =
            "http://localhost:8084/api/orders/internal/payment-update/";

    @Override
    public String initiatePayment(PaymentRequest request) {

        Payment payment = Payment.builder()
                .uuid(UUID.randomUUID().toString())
                .orderUuid(request.getOrderUuid())
                .buyerUuid(request.getBuyerUuid())
                .amount(request.getAmount())
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Simulate payment processing
        boolean success = Math.random() > 0.2;

        payment.setStatus(
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        paymentRepository.save(payment);

        // Notify Order Service
        String url = ORDER_SERVICE_URL
                + request.getOrderUuid()
                + "?status=" + payment.getStatus().name();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret",
                "veryStrongInternalSecret123");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Void.class
        );

        return "Payment " + payment.getStatus().name();
    }
}
