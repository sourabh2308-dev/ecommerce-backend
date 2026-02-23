package com.sourabh.payment_service.service.impl;

import com.sourabh.payment_service.common.PageResponse;
import com.sourabh.payment_service.dto.PaymentRequest;
import com.sourabh.payment_service.dto.PaymentResponse;
import com.sourabh.payment_service.entity.Payment;
import com.sourabh.payment_service.entity.PaymentStatus;
import com.sourabh.payment_service.exception.PaymentAccessException;
import com.sourabh.payment_service.exception.PaymentNotFoundException;
import com.sourabh.payment_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.payment_service.repository.PaymentRepository;
import com.sourabh.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    // ─────────────────────────────────────────────
    // INITIATE PAYMENT (Buyer manual flow)
    // ─────────────────────────────────────────────

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

        // Simulate payment gateway (80% success)
        boolean success = Math.random() > 0.2;
        PaymentStatus finalStatus = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        payment.setStatus(finalStatus);
        paymentRepository.save(payment);

        log.info("Payment processed: orderUuid={}, status={}", request.getOrderUuid(), finalStatus);

        kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED,
                new PaymentCompletedEvent(request.getOrderUuid(), finalStatus.name(), paymentUuid));

        log.info("PaymentCompletedEvent published: orderUuid={}, status={}", request.getOrderUuid(), finalStatus);
        return "Payment " + finalStatus.name();
    }

    // ─────────────────────────────────────────────
    // GET PAYMENTS — buyer history (paginated)
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPaymentsByBuyer(String buyerUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> paymentPage = paymentRepository.findByBuyerUuid(buyerUuid, pageable);

        List<PaymentResponse> content = paymentPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<PaymentResponse>builder()
                .content(content)
                .page(paymentPage.getNumber())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .last(paymentPage.isLast())
                .build();
    }

    // ─────────────────────────────────────────────
    // GET PAYMENT BY UUID
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByUuid(String uuid, String role, String buyerUuid) {
        Payment payment = paymentRepository.findByUuid(uuid)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + uuid));

        enforceReadAccess(payment, role, buyerUuid);
        return mapToResponse(payment);
    }

    // ─────────────────────────────────────────────
    // GET PAYMENT BY ORDER UUID
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderUuid(String orderUuid, String role, String buyerUuid) {
        Payment payment = paymentRepository.findByOrderUuid(orderUuid)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderUuid));

        enforceReadAccess(payment, role, buyerUuid);
        return mapToResponse(payment);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    /** Buyers may only access their own payments; admins can see all. */
    private void enforceReadAccess(Payment payment, String role, String buyerUuid) {
        if ("BUYER".equalsIgnoreCase(role) && !payment.getBuyerUuid().equals(buyerUuid)) {
            throw new PaymentAccessException("You can only view your own payments");
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .uuid(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .buyerUuid(payment.getBuyerUuid())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
