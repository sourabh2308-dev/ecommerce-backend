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
import com.sourabh.payment_service.gateway.PaymentGateway;
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

/**
 * Core implementation of {@link PaymentService}.
 *
 * <p>Handles the complete payment lifecycle: initiation via a pluggable
 * {@link PaymentGateway}, processing of asynchronous gateway callbacks,
 * buyer/seller/admin query operations, and financial dashboard
 * aggregations.
 *
 * <h3>Transaction semantics</h3>
 * The class-level {@code @Transactional} annotation ensures that every
 * public method runs inside a database transaction.  Read-only methods
 * override with {@code @Transactional(readOnly = true)} for optimised
 * connection handling.
 *
 * <h3>Revenue split model</h3>
 * <ul>
 *   <li>Platform fee — configurable percentage
 *       ({@code payment.platform-fee-percent}, default 10%).</li>
 *   <li>Delivery fee — flat per-item charge
 *       ({@code payment.delivery-fee-per-item}, default ₹30).</li>
 *   <li>Seller payout — {@code itemTotal - platformFee}.</li>
 * </ul>
 *
 * <h3>Saga integration</h3>
 * After a payment reaches a terminal status ({@code SUCCESS} or
 * {@code FAILED}) a {@link PaymentCompletedEvent} is published to the
 * {@code payment.completed} Kafka topic so that the order-service can
 * confirm or compensate.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    /** JPA repository for {@link Payment} entities. */
    private final PaymentRepository paymentRepository;

    /** JPA repository for {@link PaymentSplit} entities and aggregation queries. */
    private final PaymentSplitRepository paymentSplitRepository;

    /** Kafka producer used to publish {@link PaymentCompletedEvent} messages. */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Platform commission percentage applied to every order item.
     * Injected from {@code payment.platform-fee-percent} (default 10.0).
     */
    @Value("${payment.platform-fee-percent:10.0}")
    private double platformFeePercent;

    /**
     * Flat delivery fee charged per item unit.
     * Injected from {@code payment.delivery-fee-per-item} (default 30.0).
     */
    @Value("${payment.delivery-fee-per-item:30.0}")
    private double deliveryFeePerItem;

    /** Kafka topic for publishing payment completion events. */
    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    /** Pluggable payment gateway (mock or Razorpay, selected by config). */
    private final PaymentGateway paymentGateway;

    /**
     * {@inheritDoc}
     *
     * <p>Validates the caller's role, persists an {@code INITIATED}
     * payment, delegates to the configured {@link PaymentGateway}, and
     * transitions the payment to its final status.  A Kafka event is
     * published only for terminal statuses; {@code PENDING} payments
     * wait for the gateway callback.
     *
     * @throws PaymentAccessException if the caller role is not
     *                                {@code BUYER}
     */
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

        String gatewayResult = paymentGateway.initiate(request.getAmount(), "INR", paymentUuid);

        PaymentStatus finalStatus;
        if (gatewayResult.startsWith("Payment SUCCESS")) {
            finalStatus = PaymentStatus.SUCCESS;
        } else if (gatewayResult.startsWith("Payment FAILED")) {
            finalStatus = PaymentStatus.FAILED;
        } else {
            finalStatus = PaymentStatus.PENDING;
            payment.setGatewayOrderId(gatewayResult);
        }

        payment.setStatus(finalStatus);
        paymentRepository.save(payment);

        log.info("Payment processed: orderUuid={}, status={}", request.getOrderUuid(), finalStatus);

        if (finalStatus != PaymentStatus.PENDING) {
            kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED,
                    new PaymentCompletedEvent(request.getOrderUuid(), finalStatus.name(), paymentUuid));
            log.info("PaymentCompletedEvent published: orderUuid={}, status={}", request.getOrderUuid(), finalStatus);
        } else {
            log.info("Payment PENDING (awaiting gateway callback): orderUuid={}, gatewayOrderId={}",
                    request.getOrderUuid(), gatewayResult);
        }

        return gatewayResult;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Looks up the payment by gateway order ID first
     * (e.g. Razorpay {@code order_xxx}), falling back to the internal
     * order UUID for backward compatibility.  Updates the status and
     * publishes a Kafka event keyed on the <em>internal</em>
     * {@code orderUuid} so the order-service can correlate correctly.
     *
     * @throws PaymentNotFoundException if no matching payment exists
     */
    @Override
    public void handleGatewayCallback(String gatewayOrderId, boolean success, String gatewayResponse) {
        Payment payment = paymentRepository.findByGatewayOrderId(gatewayOrderId)
                .or(() -> paymentRepository.findByOrderUuid(gatewayOrderId))
                .orElseThrow(() -> new PaymentNotFoundException("No payment for gateway order " + gatewayOrderId));

        PaymentStatus finalStatus = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        payment.setStatus(finalStatus);
        paymentRepository.save(payment);

        log.info("[GATEWAY CALLBACK] gatewayOrderId={}, internalOrder={}, success={}, response={}",
                gatewayOrderId, payment.getOrderUuid(), success, gatewayResponse);

        kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED,
                new PaymentCompletedEvent(payment.getOrderUuid(), finalStatus.name(), payment.getUuid()));
    }

    /**
     * Returns the active {@link PaymentGateway} instance.
     *
     * <p>Exposed for the controller's webhook verification logic;
     * callers should prefer the dedicated callback handler for
     * normal payment processing.
     *
     * @return the configured payment gateway
     */
    public PaymentGateway getPaymentGateway() {
        return paymentGateway;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Results are sorted by {@code createdAt} descending so the
     * most recent payments appear first.
     */
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

    /**
     * {@inheritDoc}
     *
     * @throws PaymentNotFoundException if no payment matches the UUID
     * @throws PaymentAccessException   if the buyer tries to access
     *                                  another buyer's payment
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByUuid(String uuid, String role, String buyerUuid) {
        Payment payment = paymentRepository.findByUuid(uuid)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + uuid));

        enforceReadAccess(payment, role, buyerUuid);
        return mapToResponse(payment);
    }

    /**
     * {@inheritDoc}
     *
     * @throws PaymentNotFoundException if no payment exists for the
     *                                  given order UUID
     * @throws PaymentAccessException   if the buyer tries to access
     *                                  another buyer's payment
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderUuid(String orderUuid, String role, String buyerUuid) {
        Payment payment = paymentRepository.findByOrderUuid(orderUuid)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order: " + orderUuid));

        enforceReadAccess(payment, role, buyerUuid);
        return mapToResponse(payment);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses a custom JPQL sub-select in
     * {@link PaymentRepository#findBySellerUuid} to locate payments
     * that contain at least one split for the specified seller.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getSellerPayments(String sellerUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentPage = paymentRepository.findBySellerUuid(sellerUuid, pageable);

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

    /**
     * {@inheritDoc}
     *
     * <p>Aggregates completed payouts, pending payouts, and order
     * count from the {@link PaymentSplitRepository}.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Aggregates platform-wide metrics from the
     * {@link PaymentSplitRepository}: gross revenue, platform fees,
     * delivery fees, seller payouts, completed order count, and
     * active seller count.
     */
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

    /**
     * Verifies that the caller is authorised to read the specified
     * payment.  Buyers may only view their own payments; admins and
     * sellers bypass this check.
     *
     * @param payment   the payment entity
     * @param role      the caller's role
     * @param buyerUuid the caller's UUID
     * @throws PaymentAccessException if a buyer attempts to view
     *                                another buyer's payment
     */
    private void enforceReadAccess(Payment payment, String role, String buyerUuid) {
        if ("BUYER".equalsIgnoreCase(role) && !payment.getBuyerUuid().equals(buyerUuid)) {
            throw new PaymentAccessException("You can only view your own payments");
        }
    }

    /**
     * Maps a {@link Payment} entity and its associated
     * {@link PaymentSplit} records to a {@link PaymentResponse} DTO.
     *
     * @param payment the payment entity to convert
     * @return fully populated payment response
     */
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

    /**
     * Maps a single {@link PaymentSplit} entity to a
     * {@link PaymentSplitResponse} DTO.
     *
     * @param split the split entity to convert
     * @return split response DTO
     */
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
