package com.sourabh.payment_service.service.impl;

import com.sourabh.payment_service.common.PageResponse;
import com.sourabh.payment_service.dto.*;
import com.sourabh.payment_service.entity.Payment;
import com.sourabh.payment_service.entity.PaymentSplit;
import com.sourabh.payment_service.entity.PaymentSplitStatus;
import com.sourabh.payment_service.entity.PaymentStatus;
import com.sourabh.payment_service.exception.PaymentAccessException;
import com.sourabh.payment_service.exception.PaymentNotFoundException;
import com.sourabh.payment_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.payment_service.repository.PaymentRepository;
import com.sourabh.payment_service.repository.PaymentSplitRepository;
import com.sourabh.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final PaymentSplitRepository paymentSplitRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Platform commission percentage — configurable via application.properties */
    @Value("${payment.platform-fee-percent:10.0}")
    private double platformFeePercent;

    /** Flat delivery fee per item — configurable via application.properties */
    @Value("${payment.delivery-fee-per-item:30.0}")
    private double deliveryFeePerItem;

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

        // Simulate payment gateway (100% success for demo)
        boolean success = true; // Changed from Math.random() > 0.2 for reliable demo
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
    // SELLER: paginated payment splits
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentSplitResponse> getSellerPayments(String sellerUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentSplit> splitPage = paymentSplitRepository.findBySellerUuid(sellerUuid, pageable);

        List<PaymentSplitResponse> content = splitPage.getContent()
                .stream()
                .map(this::mapSplitToResponse)
                .toList();

        return PageResponse.<PaymentSplitResponse>builder()
                .content(content)
                .page(splitPage.getNumber())
                .size(splitPage.getSize())
                .totalElements(splitPage.getTotalElements())
                .totalPages(splitPage.getTotalPages())
                .last(splitPage.isLast())
                .build();
    }

    // ─────────────────────────────────────────────
    // SELLER DASHBOARD
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SellerDashboardResponse getSellerDashboard(String sellerUuid) {
        Double completed = paymentSplitRepository.sumSellerPayout(sellerUuid);
        Double pending = paymentSplitRepository.sumSellerPendingPayout(sellerUuid);
        Long totalOrders = paymentSplitRepository.countSellerOrders(sellerUuid);

        return SellerDashboardResponse.builder()
                .sellerUuid(sellerUuid)
                .totalEarnings(completed + pending)
                .completedPayouts(completed)
                .pendingPayouts(pending)
                .totalOrders(totalOrders)
                .build();
    }

    // ─────────────────────────────────────────────
    // ADMIN DASHBOARD
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        return AdminDashboardResponse.builder()
                .totalGrossRevenue(paymentSplitRepository.sumTotalItemAmount())
                .totalPlatformEarnings(paymentSplitRepository.sumTotalPlatformFees())
                .totalDeliveryFees(paymentSplitRepository.sumTotalDeliveryFees())
                .totalSellerPayouts(paymentSplitRepository.sumTotalSellerPayouts())
                .totalCompletedOrders(paymentSplitRepository.countTotalCompletedOrders())
                .activeSellers(paymentSplitRepository.countActiveSellers())
                .build();
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
        List<PaymentSplitResponse> splits = paymentSplitRepository.findByPaymentUuid(payment.getUuid())
                .stream()
                .map(this::mapSplitToResponse)
                .toList();

        return PaymentResponse.builder()
                .uuid(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .buyerUuid(payment.getBuyerUuid())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .splits(splits)
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private PaymentSplitResponse mapSplitToResponse(PaymentSplit split) {
        return PaymentSplitResponse.builder()
                .sellerUuid(split.getSellerUuid())
                .productUuid(split.getProductUuid())
                .itemAmount(split.getItemAmount())
                .platformFeePercent(split.getPlatformFeePercent())
                .platformFee(split.getPlatformFee())
                .deliveryFee(split.getDeliveryFee())
                .sellerPayout(split.getSellerPayout())
                .status(split.getStatus().name())
                .build();
    }
}
