package com.sourabh.order_service.kafka.consumer;

import com.sourabh.order_service.kafka.event.PaymentCompletedEvent;
import com.sourabh.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment.completed", groupId = "order-service")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: orderUuid={}, status={}",
                event.getOrderUuid(), event.getStatus());
        orderService.updatePaymentStatus(event.getOrderUuid(), event.getStatus());
    }
}
