package com.sourabh.payment_service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published on the {@code payment.completed} topic after a
 * payment reaches a terminal state ({@code SUCCESS} or {@code FAILED}).
 *
 * <p>The order service consumes this event to complete the saga:
 * <ul>
 *   <li>On {@code SUCCESS} — the order status moves to {@code CONFIRMED}.</li>
 *   <li>On {@code FAILED} — the order is cancelled and product stock is
 *       restored via a compensating action.</li>
 * </ul>
 *
 * @see com.sourabh.payment_service.kafka.consumer.OrderEventConsumer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    /** UUID of the order whose payment was processed. */
    private String orderUuid;

    /** Terminal payment status: {@code "SUCCESS"} or {@code "FAILED"}. */
    private String status;

    /** UUID of the payment record in the payment service. */
    private String paymentUuid;
}
