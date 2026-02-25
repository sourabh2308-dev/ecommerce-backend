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

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final ProcessedEventRepository processedEventRepository;

    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "true"
    )
    /**

     * KAFKA EVENT CONSUMER - Async Event Processing

     * 

     * PURPOSE:

     * Subscribes to Kafka topic and processes events asynchronously.

     * Part of event-driven architecture for inter-service communication.

     * 

     * HOW IT WORKS:

     * 1. Spring Kafka polls topic for new messages

     * 2. Deserializes JSON to event object

     * 3. Invokes this method in consumer thread pool

     * 4. Acknowledges message on successful processing

     * 5. On exception, retries or sends to DLQ (Dead Letter Queue)

     * 

     * @KafkaListener annotation parameters:

     * - topics: Topic name(s) to subscribe to

     * - groupId: Consumer group (enables load balancing)

     * - containerFactory: Custom config for concurrency, error handling

     * 

     * EVENTUAL CONSISTENCY:

     * Kafka ensures at-least-once delivery. Method must be idempotent

     * (safe to process same event multiple times).

     * 

     * ERROR HANDLING:

     * - Exceptions trigger retry mechanism (configurable retry count)

     * - Failed messages after retries sent to DLQ topic

     * - Use @Transactional to rollback DB changes on error

     */

    @KafkaListener(topics = TOPIC_PAYMENT_COMPLETED, groupId = "order-service")
    @Transactional
    /**
     * HANDLEPAYMENTCOMPLETED - Method Documentation
     *
     * PURPOSE:
     * This method handles the handlePaymentCompleted operation.
     *
     * PARAMETERS:
     * @param event - PaymentCompletedEvent value
     *
     * ANNOTATIONS USED:
     * @Transactional - Wraps in database transaction (atomic execution)
     * @KafkaListener - Consumes events from Kafka topic
     * @Transactional - Wraps in database transaction (atomic execution)
     *
     */
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: orderUuid={}, status={}, paymentUuid={}",
                event.getOrderUuid(), event.getStatus(), event.getPaymentUuid());

        // Idempotency guard — use paymentUuid as deduplication key
        String idempotencyKey = "payment-completed:" + event.getPaymentUuid();
        if (processedEventRepository.existsByEventId(idempotencyKey)) {
            log.warn("Duplicate PaymentCompletedEvent, skipping: paymentUuid={}", event.getPaymentUuid());
            return;
        }

        // Update order payment status (also cancels order if FAILED)
        orderService.updatePaymentStatus(event.getOrderUuid(), event.getStatus());

        // Saga compensation: restore stock if payment failed
        if ("FAILED".equalsIgnoreCase(event.getStatus())) {
            compensateFailedPayment(event.getOrderUuid());
        }

        // Record as processed
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(idempotencyKey)
                .topic(TOPIC_PAYMENT_COMPLETED)
                .processedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Compensation step: restores product stock after a failed payment.
     * The order has already been cancelled by {@code updatePaymentStatus}.
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
                    // Log and continue — compensation must not block saga acknowledgment.
                    // In production, use outbox pattern for guaranteed delivery.
                    log.error("Failed to restore stock for productUuid={}: {}",
                            item.getProductUuid(), e.getMessage());
                }
            });
        });
    }
}

