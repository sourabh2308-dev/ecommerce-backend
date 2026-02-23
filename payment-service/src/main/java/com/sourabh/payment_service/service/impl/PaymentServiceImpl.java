package com.sourabh.payment_service.service.impl;

import com.sourabh.payment_service.dto.PaymentRequest;
import com.sourabh.payment_service.entity.Payment;
import com.sourabh.payment_service.entity.PaymentStatus;
import com.sourabh.payment_service.exception.PaymentAccessException;
import com.sourabh.payment_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.payment_service.repository.PaymentRepository;
import com.sourabh.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    @Override
    public String initiatePayment(PaymentRequest request, String role, String buyerUuid) {

        if (!"BUYER".equalsIgnoreCase(role)) {
            throw new PaymentAccessException("Only buyers can initiate payments");
        }

        request.setBuyerUuid(buyerUuid);

        String paymentUuid = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .uuid(paymentUuid)
                .orderUuid(request.getOrderUuid())
                .buyerUuid(buyerUuid)
                .amount(request.getAmount())
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Simulate payment gateway
        boolean success = Math.random() > 0.2;
        PaymentStatus finalStatus = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        payment.setStatus(finalStatus);
        paymentRepository.save(payment);

        log.info("Payment processed: orderUuid={}, status={}", request.getOrderUuid(), finalStatus);

        // Notify order-service asynchronously via Kafka
        kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED,
                new PaymentCompletedEvent(request.getOrderUuid(), finalStatus.name(), paymentUuid));

        log.info("PaymentCompletedEvent published: orderUuid={}, status={}", request.getOrderUuid(), finalStatus);
        return "Payment " + finalStatus.name();
    }
}
