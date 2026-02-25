package com.sourabh.payment_service.kafka.consumer;

import com.sourabh.payment_service.entity.*;
import com.sourabh.payment_service.kafka.event.OrderCreatedEvent;
import com.sourabh.payment_service.kafka.event.OrderItemEvent;
import com.sourabh.payment_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.payment_service.repository.PaymentRepository;
import com.sourabh.payment_service.repository.PaymentSplitRepository;
import com.sourabh.payment_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Consumes order.created events and processes payment automatically as part of
 * the Order-Payment Saga. Creates payment splits per seller for revenue sharing.
 * Idempotent: skips already-processed events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentRepository paymentRepository;
    private final PaymentSplitRepository paymentSplitRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.platform-fee-percent:10.0}")
    private double platformFeePercent;

    @Value("${payment.delivery-fee-per-item:30.0}")
    private double deliveryFeePerItem;

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
        log.info("Received OrderCreatedEvent: eventId={}, orderUuid={}, items={}",
                event.getEventId(), event.getOrderUuid(),
                event.getItems() != null ? event.getItems().size() : 0);

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

        // Simulate payment gateway (100% success for demo)
        boolean success = true; // Changed from Math.random() > 0.2 for reliable demo
        PaymentStatus finalStatus = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        payment.setStatus(finalStatus);
        paymentRepository.save(payment);

        log.info("Auto-payment processed via Saga: orderUuid={}, status={}", event.getOrderUuid(), finalStatus);

        // Create payment splits per seller/item when payment succeeds
        if (finalStatus == PaymentStatus.SUCCESS && event.getItems() != null) {
            createPaymentSplits(paymentUuid, event.getOrderUuid(), event.getItems());
        }

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

    /**
     * Creates a PaymentSplit record for each order item, calculating
     * platform fee, delivery fee and net seller payout.
     */
    private void createPaymentSplits(String paymentUuid, String orderUuid, List<OrderItemEvent> items) {
        List<PaymentSplit> splits = new ArrayList<>();
        for (OrderItemEvent item : items) {
            double itemAmount = item.getSubtotal();
            double platformFee = Math.round(itemAmount * platformFeePercent) / 100.0;
            double deliveryFee = deliveryFeePerItem;
            double sellerPayout = itemAmount - platformFee - deliveryFee;
            if (sellerPayout < 0) sellerPayout = 0;

            PaymentSplit split = PaymentSplit.builder()
                    .paymentUuid(paymentUuid)
                    .orderUuid(orderUuid)
                    .sellerUuid(item.getSellerUuid())
                    .productUuid(item.getProductUuid())
                    .itemAmount(itemAmount)
                    .platformFeePercent(platformFeePercent)
                    .platformFee(platformFee)
                    .deliveryFee(deliveryFee)
                    .sellerPayout(sellerPayout)
                    .status(PaymentSplitStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .build();
            splits.add(split);

            log.info("PaymentSplit created: seller={}, item={}, payout={}, platformFee={}, deliveryFee={}",
                    item.getSellerUuid(), item.getProductUuid(), sellerPayout, platformFee, deliveryFee);
        }
        paymentSplitRepository.saveAll(splits);
    }
}
