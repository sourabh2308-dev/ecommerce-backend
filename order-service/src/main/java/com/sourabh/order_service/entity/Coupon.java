package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a promotional discount coupon in the e-commerce platform.
 *
 * <p>A coupon may provide either a fixed monetary ({@link DiscountType#FLAT}) or a
 * percentage-based ({@link DiscountType#PERCENTAGE}) discount on eligible orders.
 * Each coupon is governed by configurable rules including global and per-user usage
 * limits, minimum order thresholds, validity windows, and optional seller-specific
 * restrictions.</p>
 *
 * <p>Mapped to the {@code coupon} database table. Inherits {@code createdAt} and
 * {@code updatedAt} audit timestamps from {@link BaseAuditEntity}.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see CouponUsage
 * @see BaseAuditEntity
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "coupon")
public class Coupon extends BaseAuditEntity {

    /**
     * Database-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique alphanumeric coupon code entered by buyers at checkout (e.g. {@code "SAVE20"}).
     */
    @Column(unique = true, nullable = false)
    private String code;

    /**
     * Strategy used to calculate the discount — either {@link DiscountType#PERCENTAGE}
     * or {@link DiscountType#FLAT}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    /**
     * Numeric discount value. Interpreted as a percentage (0-100) when
     * {@link #discountType} is {@code PERCENTAGE}, or as an absolute currency
     * amount when it is {@code FLAT}.
     */
    @Column(nullable = false)
    private Double discountValue;

    /**
     * Minimum order subtotal (in the order currency) required before this
     * coupon can be applied. Defaults to {@code 0.0} (no minimum).
     */
    @Column(nullable = false)
    @Builder.Default
    private Double minOrderAmount = 0.0;

    /**
     * Optional upper cap on the discount amount in the order currency.
     * Primarily useful for percentage-based coupons to prevent excessively
     * large discounts on high-value orders. {@code null} means no cap.
     */
    private Double maxDiscount;

    /**
     * Maximum number of times this coupon can be redeemed across all users.
     * Defaults to {@code 1000}.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer totalUsageLimit = 1000;

    /**
     * Running count of how many times this coupon has already been redeemed.
     * Incremented transactionally on each successful application. Defaults to {@code 0}.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    /**
     * Maximum number of times a single user may redeem this coupon.
     * Defaults to {@code 1}.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer perUserLimit = 1;

    /**
     * Inclusive start of the coupon's validity window.
     * The coupon cannot be redeemed before this timestamp.
     */
    @Column(nullable = false)
    private LocalDateTime validFrom;

    /**
     * Inclusive end of the coupon's validity window.
     * The coupon cannot be redeemed after this timestamp and will be
     * auto-deactivated by {@link com.sourabh.order_service.scheduler.ExpireCouponsScheduler}.
     */
    @Column(nullable = false)
    private LocalDateTime validUntil;

    /**
     * Flag indicating whether this coupon is currently redeemable.
     * Inactive coupons are rejected during validation. Defaults to {@code true}.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Optional UUID of a specific seller whose products are eligible for this
     * coupon. When {@code null}, the coupon applies globally to all sellers.
     */
    private String sellerUuid;

    /**
     * Enumeration of supported discount calculation strategies.
     */
    public enum DiscountType {
        /**
         * Percentage-based discount (e.g. 20 %). The discount is calculated as
         * {@code orderSubtotal * discountValue / 100}, capped by {@link Coupon#maxDiscount}.
         */
        PERCENTAGE,

        /**
         * Fixed monetary discount (e.g. ₹100 off). The {@link Coupon#discountValue}
         * is subtracted directly from the order subtotal.
         */
        FLAT
    }
}
