package com.sourabh.order_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object representing a single order line item within a
 * Kafka event payload (e.g. {@link OrderCreatedEvent}).
 *
 * <p>Carries the product, seller, pricing, and quantity information
 * required by downstream consumers such as the payment-service.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see OrderCreatedEvent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEvent {

    /** UUID of the product being ordered. */
    private String productUuid;

    /** UUID of the seller who owns the product. */
    private String sellerUuid;

    /** Unit price of the product at the time of order creation. */
    private double price;

    /** Number of units ordered. */
    private int quantity;

    /** Computed subtotal for this line item ({@code price * quantity}). */
    private double subtotal;
}
