package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * JPA entity representing a customer order in the e-commerce platform.
 *
 * <p>An order is created when a buyer checks out. It contains one or more
 * {@link OrderItem}s, shipping details, tax and currency information, and
 * optional coupon discounts. Orders follow a lifecycle defined by
 * {@link OrderStatus} and track payment progress via {@link PaymentStatus}.</p>
 *
 * <p>Multi-seller order splitting is supported through the {@link OrderType}
 * field: a {@code MAIN} order placed by the buyer may be split into multiple
 * {@code SUB} orders, one per seller. Related orders share the same
 * {@link #orderGroupId}.</p>
 *
 * <p>Soft-delete is implemented via the {@link #isDeleted} flag.
 * Mapped to the {@code orders} database table. Inherits audit timestamps
 * from {@link BaseAuditEntity}.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see OrderItem
 * @see OrderStatus
 * @see PaymentStatus
 * @see OrderType
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order extends BaseAuditEntity {

    /**
     * Database-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Universally unique identifier exposed to external clients and used
     * for inter-service communication. Generated at order creation time.
     */
    @Column(unique = true, nullable = false)
    private String uuid;

    /**
     * UUID of the buyer who placed this order, extracted from the JWT
     * at the API Gateway and forwarded via the {@code X-User-UUID} header.
     */
    @Column(nullable = false)
    private String buyerUuid;

    /**
     * Total monetary amount of the order in the configured {@link #currency},
     * inclusive of tax and after any coupon discount has been applied.
     */
    @Column(nullable = false)
    private Double totalAmount;

    /**
     * Current lifecycle status of the order (e.g. CREATED, CONFIRMED,
     * SHIPPED, DELIVERED, CANCELLED). Persisted as a string in the database.
     */
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /**
     * Current payment status for this order (e.g. PENDING, SUCCESS, FAILED,
     * REFUNDED). Updated asynchronously via the payment saga.
     */
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    /**
     * Soft-delete flag. When {@code true} the order is treated as deleted
     * and excluded from all standard queries. Defaults to {@code false}.
     */
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Indicates whether this order is a {@link OrderType#MAIN} buyer-placed
     * order or a {@link OrderType#SUB} seller-specific split. Defaults to
     * {@code MAIN}.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderType orderType = OrderType.MAIN;

    /**
     * UUID of the parent order when this is a sub-order. {@code null} for
     * main orders.
     */
    private String parentOrderUuid;

    /**
     * Shared group identifier linking a main order with all of its
     * sub-orders for multi-seller order splitting.
     */
    private String orderGroupId;

    /**
     * Full name of the shipping recipient.
     */
    private String shippingName;

    /**
     * Street address for shipping.
     */
    private String shippingAddress;

    /**
     * City component of the shipping address.
     */
    private String shippingCity;

    /**
     * State or province component of the shipping address.
     */
    private String shippingState;

    /**
     * Postal / PIN code for the shipping address.
     */
    private String shippingPincode;

    /**
     * Contact phone number for the shipping recipient.
     */
    private String shippingPhone;

    /**
     * Type of return requested for this order, if any (REFUND or EXCHANGE).
     * {@code null} when no return has been requested.
     */
    @Enumerated(EnumType.STRING)
    private ReturnType returnType;

    /**
     * Free-text reason provided by the buyer when requesting a return.
     * Maximum 500 characters. {@code null} when no return has been requested.
     */
    @Column(length = 500)
    private String returnReason;

    /**
     * Applicable tax percentage (e.g. GST). Defaults to {@code 18.0} %.
     */
    @Builder.Default
    private Double taxPercent = 18.0;

    /**
     * Calculated tax amount in the order currency. Defaults to {@code 0.0}.
     */
    @Builder.Default
    private Double taxAmount = 0.0;

    /**
     * ISO 4217 currency code for the order. Defaults to {@code "INR"}.
     */
    @Builder.Default
    private String currency = "INR";

    /**
     * Coupon code applied to this order, or {@code null} if no coupon was used.
     */
    private String couponCode;

    /**
     * Monetary discount applied via coupon. Defaults to {@code 0.0}.
     */
    @Builder.Default
    private Double discountAmount = 0.0;

    /**
     * Line items belonging to this order. Managed with {@code CascadeType.ALL}
     * and orphan removal so that adding/removing items from this list is
     * automatically reflected in the database.
     */
    @OneToMany(mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<OrderItem> items;
}

