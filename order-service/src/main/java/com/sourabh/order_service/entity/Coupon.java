package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Coupon/promo code for discounts.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "coupon")
public class Coupon extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    /** Percentage or flat amount depending on discountType */
    @Column(nullable = false)
    private Double discountValue;

    /** Minimum order amount to apply coupon */
    @Column(nullable = false)
    @Builder.Default
    private Double minOrderAmount = 0.0;

    /** Maximum discount cap (for percentage coupons) */
    private Double maxDiscount;

    /** Total number of times this coupon can be used */
    @Column(nullable = false)
    @Builder.Default
    private Integer totalUsageLimit = 1000;

    /** Times used so far */
    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    /** Max uses per individual user */
    @Column(nullable = false)
    @Builder.Default
    private Integer perUserLimit = 1;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** If set, coupon only valid for this seller's products */
    private String sellerUuid;

    public enum DiscountType {
        PERCENTAGE, FLAT
    }
}
