package com.sourabh.order_service.kafka;

import com.sourabh.order_service.kafka.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "order.status.changed";

    public void publishStatusChange(String orderUuid, String buyerUuid,
                                    String oldStatus, String newStatus,
                                    Double totalAmount, String currency) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                orderUuid, buyerUuid, oldStatus, newStatus, totalAmount, currency);
        kafkaTemplate.send(TOPIC, orderUuid, event);
        log.info("OrderStatusChangedEvent published: orderUuid={}, {} -> {}", orderUuid, oldStatus, newStatus);
    }
}
