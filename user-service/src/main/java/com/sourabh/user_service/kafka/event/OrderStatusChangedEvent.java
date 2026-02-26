package com.sourabh.user_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
