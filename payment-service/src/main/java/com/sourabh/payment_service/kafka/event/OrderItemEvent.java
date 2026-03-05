package com.sourabh.payment_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single line item within an {@link OrderCreatedEvent}.
 *
 * <p>Contains the product, seller, pricing, and quantity details needed by
 * the payment service to compute per-seller revenue splits (platform fee,
 * delivery fee, and net seller payout).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEvent {

    /** UUID of the product being purchased. */
    private String productUuid;

    /** UUID of the seller who owns the product. */
    private String sellerUuid;

    /** Unit price of the product at the time of order placement. */
    private double price;

    /** Number of units purchased. */
    private int quantity;

    /** Pre-computed subtotal: {@code price * quantity}. */
    private double subtotal;
}
