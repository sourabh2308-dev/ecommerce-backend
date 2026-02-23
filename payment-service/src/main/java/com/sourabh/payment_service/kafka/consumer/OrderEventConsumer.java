package com.sourabh.payment_service.kafka.consumer;

import com.sourabh.payment_service.entity.Payment;
import com.sourabh.payment_service.entity.PaymentStatus;
import com.sourabh.payment_service.entity.ProcessedEvent;
import com.sourabh.payment_service.kafka.event.OrderCreatedEvent;
import com.sourabh.payment_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.payment_service.repository.PaymentRepository;
import com.sourabh.payment_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumes order.created events and processes payment automatically as part of
 * the Order-Payment Saga. Idempotent: skips already-processed events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";
    private static final String TOPIC_ORDER_CREATED = "order.created";

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = TOPIC_ORDER_CREATED, groupId = "payment-service")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: eventId={}, orderUuid={}", event.getEventId(), event.getOrderUuid());

        // Idempotency guard — skip if this event was already processed
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate event detected, skipping: eventId={}", event.getEventId());
            return;
        }

        // Avoid double-payment for the same order (e.g. REST + event both trigger)
        if (paymentRepository.existsByOrderUuid(event.getOrderUuid())) {
            log.warn("Payment already exists for orderUuid={}, skipping event processing", event.getOrderUuid());
            return;
        }

        String paymentUuid = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .uuid(paymentUuid)
                .orderUuid(event.getOrderUuid())
                .buyerUuid(event.getBuyerUuid())
                .amount(event.getTotalAmount())
                .status(PaymentStatus.INITIATED)
                .createdAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        // Simulate payment gateway (80% success rate)
        boolean success = Math.random() > 0.2;
        PaymentStatus finalStatus = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        payment.setStatus(finalStatus);
        paymentRepository.save(payment);

        log.info("Auto-payment processed via Saga: orderUuid={}, status={}", event.getOrderUuid(), finalStatus);

        // Mark event as processed
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .topic(TOPIC_ORDER_CREATED)
                .processedAt(LocalDateTime.now())
                .build());

        // Notify order-service of the payment outcome
        kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED,
                new PaymentCompletedEvent(event.getOrderUuid(), finalStatus.name(), paymentUuid));
        log.info("PaymentCompletedEvent published for Saga: orderUuid={}, status={}", event.getOrderUuid(), finalStatus);
    }
}
