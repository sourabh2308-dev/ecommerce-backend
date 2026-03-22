package com.sourabh.order_service.kafka;

import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderEventOutbox;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.exception.OrderNotFoundException;
import com.sourabh.order_service.kafka.event.OrderCreatedEvent;
import com.sourabh.order_service.kafka.event.OrderCreatedOutboxRequestedEvent;
import com.sourabh.order_service.kafka.event.OrderItemEvent;
import com.sourabh.order_service.repository.OrderEventOutboxRepository;
import com.sourabh.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedOutboxPublisher {

    private final OrderEventOutboxRepository outboxRepository;

    private final OrderRepository orderRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedOutboxRequestedEvent event) {
        publishOutboxRecord(event.outboxId());
    }

    @Scheduled(fixedDelayString = "${scheduler.order-created-outbox.fixed-delay-ms:5000}")
    public void publishPendingRecords() {
        for (OrderEventOutbox outbox : outboxRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc()) {
            try {
                publishOutboxRecord(outbox.getId());
            } catch (Exception ex) {
                log.warn("Deferred retry for unpublished order-created event: outboxId={}, orderUuid={}",
                        outbox.getId(), outbox.getOrderUuid(), ex);
            }
        }
    }

    @Transactional
    public void publishOutboxRecord(Long outboxId) {
        OrderEventOutbox outbox = outboxRepository.findByIdForUpdate(outboxId).orElse(null);
        if (outbox == null || outbox.isPublished()) {
            return;
        }

        Order order = orderRepository.findByUuidAndIsDeletedFalse(outbox.getOrderUuid())
                .orElseThrow(() -> new OrderNotFoundException("Order not found for outbox event: " + outbox.getOrderUuid()));

        OrderCreatedEvent event = new OrderCreatedEvent(
                outbox.getEventId(),
                order.getUuid(),
                order.getBuyerUuid(),
                toItemEvents(order.getItems()),
                order.getTotalAmount());

        outbox.setAttemptCount(outbox.getAttemptCount() + 1);
        outbox.setLastAttemptAt(LocalDateTime.now());

        try {
            kafkaTemplate.send(OrderCreatedEvent.TOPIC, order.getBuyerUuid(), event).get(10, TimeUnit.SECONDS);
            outbox.setPublished(true);
            outbox.setPublishedAt(LocalDateTime.now());
            log.info("Published durable order-created event: orderUuid={}, eventId={}", order.getUuid(), outbox.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish durable order-created event: orderUuid={}, eventId={}",
                    order.getUuid(), outbox.getEventId(), e);
            throw new IllegalStateException("Failed to publish order-created event", e);
        }
    }

    private List<OrderItemEvent> toItemEvents(List<OrderItem> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> {
                    OrderItemEvent itemEvent = new OrderItemEvent();
                    itemEvent.setProductUuid(item.getProductUuid());
                    itemEvent.setSellerUuid(item.getSellerUuid());
                    itemEvent.setPrice(item.getPrice());
                    itemEvent.setQuantity(item.getQuantity());
                    itemEvent.setSubtotal(item.getPrice() * item.getQuantity());
                    return itemEvent;
                })
                .toList();
    }
}