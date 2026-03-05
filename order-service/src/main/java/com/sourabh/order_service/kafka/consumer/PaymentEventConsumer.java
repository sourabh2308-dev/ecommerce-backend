package com.sourabh.order_service.kafka.consumer;

import com.sourabh.order_service.entity.ProcessedEvent;
import com.sourabh.order_service.feign.ProductServiceClient;
import com.sourabh.order_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.repository.ProcessedEventRepository;
import com.sourabh.order_service.service.OrderService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Kafka consumer that handles {@link PaymentCompletedEvent} messages published
 * by the payment-service on the {@code payment.completed} topic.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Update the order's payment status (and order status on failure).</li>
 *   <li>Execute saga compensation (restore product stock) when payment fails.</li>
 *   <li>Record processed events for idempotent consumption.</li>
 * </ol>
 *
 * <p>Retry policy: up to 3 attempts with exponential back-off (1 s base,
 * 2× multiplier). Messages that exhaust retries are forwarded to the
 * dead-letter topic {@code payment.completed.DLT}.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see PaymentCompletedEvent
 * @see OrderService#updatePaymentStatus(String, String)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    /** Service layer for order business logic. */
    private final OrderService orderService;

    /** Repository for order look-ups during saga compensation. */
    private final OrderRepository orderRepository;

    /** Feign client used to restore product stock on payment failure. */
    private final ProductServiceClient productServiceClient;

    /** Repository for idempotent event tracking. */
    private final ProcessedEventRepository processedEventRepository;

    /** Kafka topic name for payment completion events. */
    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    /**
     * Consumes a {@link PaymentCompletedEvent} and updates the corresponding
     * order's payment status.
     *
     * <p>If the payment failed, saga compensation is triggered to restore
     * product stock. The method is idempotent — duplicate events (identified
     * by {@code paymentUuid}) are silently skipped.</p>
     *
     * @param event the payment completion event containing the order UUID,
     *              payment UUID, and outcome status
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = TOPIC_PAYMENT_COMPLETED, groupId = "order-service")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: orderUuid={}, status={}, paymentUuid={}",
                event.getOrderUuid(), event.getStatus(), event.getPaymentUuid());

        String idempotencyKey = "payment-completed:" + event.getPaymentUuid();
        if (processedEventRepository.existsByEventId(idempotencyKey)) {
            log.warn("Duplicate PaymentCompletedEvent, skipping: paymentUuid={}", event.getPaymentUuid());
            return;
        }

        orderService.updatePaymentStatus(event.getOrderUuid(), event.getStatus());

        if ("FAILED".equalsIgnoreCase(event.getStatus())) {
            compensateFailedPayment(event.getOrderUuid());
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(idempotencyKey)
                .topic(TOPIC_PAYMENT_COMPLETED)
                .processedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Saga compensation step: restores product stock for every item in the
     * order after a payment failure. The order has already been cancelled by
     * {@link OrderService#updatePaymentStatus(String, String)}.
     *
     * <p>Stock restoration is best-effort — failures are logged but do not
     * block saga acknowledgement. In production a transactional outbox
     * pattern would guarantee delivery.</p>
     *
     * @param orderUuid UUID of the cancelled order
     */
    private void compensateFailedPayment(String orderUuid) {
        orderRepository.findByUuidAndIsDeletedFalse(orderUuid).ifPresent(order -> {
            if (order.getItems() == null || order.getItems().isEmpty()) {
                log.warn("No order items for compensation: orderUuid={}", orderUuid);
                return;
            }
            order.getItems().forEach(item -> {
                try {
                    productServiceClient.restoreStock(item.getProductUuid(), item.getQuantity());
                    log.info("Stock restored for compensation: productUuid={}, quantity={}",
                            item.getProductUuid(), item.getQuantity());
                } catch (FeignException e) {
                    log.error("Failed to restore stock for productUuid={}: {}",
                            item.getProductUuid(), e.getMessage());
                }
            });
        });
    }
}

