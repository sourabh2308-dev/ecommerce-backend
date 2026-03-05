package com.sourabh.order_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event consumed from the {@code payment.completed} topic, published
 * by the payment-service after processing a payment attempt.
 *
 * <p>The {@link #status} field indicates the outcome ({@code "SUCCESS"}
 * or {@code "FAILED"}). On failure the order-service triggers saga
 * compensation (stock restoration and order cancellation).</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see com.sourabh.order_service.kafka.consumer.PaymentEventConsumer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    /** UUID of the order the payment pertains to. */
    private String orderUuid;

    /** Payment outcome: {@code "SUCCESS"} or {@code "FAILED"}. */
    private String status;

    /** Unique payment identifier used as the idempotency key. */
    private String paymentUuid;
}
