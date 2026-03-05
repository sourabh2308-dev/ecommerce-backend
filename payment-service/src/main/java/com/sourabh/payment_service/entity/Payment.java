package com.sourabh.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a payment transaction in the platform.
 *
 * <p>A {@code Payment} is created when a buyer initiates a payment (REST) or
 * when the Kafka consumer processes an {@code order.created} event (saga).
 * Its lifecycle progresses through the statuses defined in
 * {@link PaymentStatus}: {@code INITIATED -> PENDING -> SUCCESS / FAILED}.
 *
 * <p>When the external payment gateway (e.g. Razorpay) returns an
 * asynchronous result, the {@link #gatewayOrderId} is used to correlate the
 * webhook callback with the correct internal payment record.
 *
 * @see PaymentStatus
 * @see PaymentSplit
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Universally unique identifier exposed to external clients. */
    @Column(unique = true)
    private String uuid;

    /** UUID of the order this payment settles. */
    private String orderUuid;

    /** UUID of the buyer who initiated the payment. */
    private String buyerUuid;

    /** Total payment amount in the platform currency (INR). */
    private Double amount;

    /**
     * Order ID returned by the external payment gateway (e.g. Razorpay
     * {@code order_xxx}).  Used to correlate webhook callbacks with the
     * internal payment; {@code null} when the mock gateway is active.
     */
    private String gatewayOrderId;

    /** Current payment status (persisted as its enum name string). */
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /** Timestamp when this payment record was created. */
    private LocalDateTime createdAt;
}
