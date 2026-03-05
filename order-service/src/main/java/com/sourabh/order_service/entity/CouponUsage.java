package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity that records an individual redemption of a {@link Coupon} by a buyer.
 *
 * <p>Each row represents a single coupon-application event. A composite unique
 * constraint on {@code (coupon_id, buyer_uuid, order_uuid)} prevents the same
 * coupon from being applied more than once to the same order by the same buyer.
 * Per-user usage counts are derived by querying this table grouped by
 * {@code (coupon_id, buyer_uuid)}.</p>
 *
 * <p>Mapped to the {@code coupon_usage} database table.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see Coupon
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "coupon_usage",
       uniqueConstraints = @UniqueConstraint(columnNames = {"coupon_id", "buyer_uuid", "order_uuid"}))
public class CouponUsage {

    /**
     * Database-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The coupon that was redeemed. Loaded lazily to avoid unnecessary joins
     * when only the usage record itself is needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    /**
     * UUID of the buyer who redeemed the coupon.
     */
    @Column(nullable = false)
    private String buyerUuid;

    /**
     * UUID of the order to which the coupon was applied.
     */
    @Column(nullable = false)
    private String orderUuid;

    /**
     * Timestamp of when the coupon was redeemed. Defaults to the current
     * system time at entity instantiation.
     */
    @Builder.Default
    private LocalDateTime usedAt = LocalDateTime.now();
}
