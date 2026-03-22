package com.sourabh.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private Double itemAmount;

    @Column(nullable = false)
    private Double platformFeePercent;

    @Column(nullable = false)
    private Double platformFee;

    @Column(nullable = false)
    private Double deliveryFee;

    @Column(nullable = false)
    private Double sellerPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSplitStatus status;

    private LocalDateTime createdAt;
}
