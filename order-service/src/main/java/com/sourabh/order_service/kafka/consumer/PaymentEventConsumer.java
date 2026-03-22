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

