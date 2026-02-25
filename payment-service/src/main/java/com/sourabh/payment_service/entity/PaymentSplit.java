package com.sourabh.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks the revenue split for each seller within a payment.
 * For each OrderItem from a seller, one PaymentSplit record is created.
 */
@Entity
@Table(name = "payment_split")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String paymentUuid;

    @Column(nullable = false)
    private String orderUuid;

    @Column(nullable = false)
    private String sellerUuid;

    @Column(nullable = false)
    private String productUuid;

    /** Gross amount for this line item (price * quantity) */
    @Column(nullable = false)
    private Double itemAmount;

    /** Platform commission percentage (e.g. 10.0 for 10%) */
    @Column(nullable = false)
    private Double platformFeePercent;

    /** Platform commission amount */
    @Column(nullable = false)
    private Double platformFee;

    /** Delivery fee charged */
    @Column(nullable = false)
    private Double deliveryFee;

    /** Net amount payable to seller = itemAmount - platformFee - deliveryFee */
    @Column(nullable = false)
    private Double sellerPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSplitStatus status;

    private LocalDateTime createdAt;
}
