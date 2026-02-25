package com.sourabh.payment_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * KAFKA EVENT - Async Message for Event-Driven Architecture
 * 
 * Published to Kafka topic for other microservices to consume.
 * Enables asynchronous processing and eventual consistency.
 * @Data: Auto-generates JSON serialization support.
 */
public class OrderItemEvent {
    private String productUuid;
    private String sellerUuid;
    private double price;
    private int quantity;
    private double subtotal;
}
