package com.sourabh.order_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published when an order's status changes. Consumed by user-service
 * to send email notifications and in-app notifications to the buyer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent {
    private String orderUuid;
    private String buyerUuid;
    private String oldStatus;
    private String newStatus;
    private Double totalAmount;
    private String currency;
}
