package com.sourabh.order_service.kafka;

import com.sourabh.order_service.kafka.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer responsible for publishing {@link OrderStatusChangedEvent}
 * messages whenever an order transitions between statuses.
 *
 * <p>Events are published to the {@code order.status.changed} topic using
 * the order UUID as the message key (ensuring all events for the same order
 * land on the same partition for ordering guarantees). The user-service
 * consumes these events to send email and in-app notifications.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see OrderStatusChangedEvent
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationPublisher {

    /** Spring Kafka template for sending messages. */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Target Kafka topic for order status change events. */
    private static final String TOPIC = "order.status.changed";

    /**
     * Publishes an {@link OrderStatusChangedEvent} to the
     * {@code order.status.changed} Kafka topic.
     *
     * @param orderUuid   UUID of the order whose status changed
     * @param buyerUuid   UUID of the buyer who placed the order
     * @param oldStatus   the previous order status
     * @param newStatus   the new order status
     * @param totalAmount total monetary amount of the order
     * @param currency    ISO 4217 currency code (e.g. {@code "INR"})
     */
    public void publishStatusChange(String orderUuid, String buyerUuid,
                                    String oldStatus, String newStatus,
                                    Double totalAmount, String currency) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                orderUuid, buyerUuid, oldStatus, newStatus, totalAmount, currency);
        kafkaTemplate.send(TOPIC, orderUuid, event);
        log.info("OrderStatusChangedEvent published: orderUuid={}, {} -> {}", orderUuid, oldStatus, newStatus);
    }
}
