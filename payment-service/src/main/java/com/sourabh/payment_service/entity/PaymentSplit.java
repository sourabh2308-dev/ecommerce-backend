package com.sourabh.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity tracking the revenue split for a single order line item within a
 * {@link Payment}.
 *
 * <p>For every {@code OrderItemEvent} consumed from Kafka, one
 * {@code PaymentSplit} row is created.  The split captures the gross item
 * amount and how it is divided among:
 * <ul>
 *   <li>Platform commission ({@link #platformFee})</li>
 *   <li>Delivery charge ({@link #deliveryFee})</li>
 *   <li>Net seller payout ({@link #sellerPayout})</li>
 * </ul>
 *
 * <p><b>Calculation:</b>
 * <pre>
 *   platformFee  = itemAmount * (platformFeePercent / 100)
 *   deliveryFee  = deliveryFeePerItem * quantity
 *   sellerPayout = itemAmount - platformFee - deliveryFee   (floor 0)
 * </pre>
 *
 * @see Payment
 * @see PaymentSplitStatus
 */
@Entity
@Table(name = "payment_split")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSplit {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID of the parent {@link Payment} this split belongs to. */
    @Column(nullable = false)
    private String paymentUuid;

    /** UUID of the order that triggered this split. */
    @Column(nullable = false)
    private String orderUuid;

    /** UUID of the seller receiving the payout. */
    @Column(nullable = false)
    private String sellerUuid;

    /** UUID of the product in this line item. */
    @Column(nullable = false)
    private String productUuid;

    /** Gross amount for this line item (price x quantity). */
    @Column(nullable = false)
    private Double itemAmount;

    /** Platform commission percentage applied (e.g. 10.0 for 10 %). */
    @Column(nullable = false)
    private Double platformFeePercent;

    /** Absolute platform commission amount. */
    @Column(nullable = false)
    private Double platformFee;

    /** Delivery fee charged for this line item. */
    @Column(nullable = false)
    private Double deliveryFee;

    /** Net amount payable to the seller: {@code itemAmount - platformFee - deliveryFee}. */
    @Column(nullable = false)
    private Double sellerPayout;

    /** Current settlement status of this split. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSplitStatus status;

    /** Timestamp when this split record was created. */
    private LocalDateTime createdAt;
}
