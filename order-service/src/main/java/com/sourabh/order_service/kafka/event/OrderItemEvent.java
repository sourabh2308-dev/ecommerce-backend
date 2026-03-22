package com.sourabh.order_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEvent {

    private String productUuid;

    private String sellerUuid;

    private double price;

    private int quantity;

    private double subtotal;
}
