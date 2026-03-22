package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uuid;

    @Column(nullable = false)
    private String buyerUuid;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Builder.Default
    private Boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderType orderType = OrderType.MAIN;

    private String parentOrderUuid;

    private String orderGroupId;

    private String shippingName;

    private String shippingAddress;

    private String shippingCity;

    private String shippingState;

    private String shippingPincode;

    private String shippingPhone;

    @Enumerated(EnumType.STRING)
    private ReturnType returnType;

    @Column(length = 500)
    private String returnReason;

    @Builder.Default
    private Double taxPercent = 18.0;

    @Builder.Default
    private Double taxAmount = 0.0;

    @Builder.Default
    private String currency = "INR";

    private String couponCode;

    @Builder.Default
    private Double discountAmount = 0.0;

    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<OrderItem> items;
}

