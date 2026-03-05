package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Coupon} entities.
 *
 * <p>Provides look-up by code, existence checks, and a query used by
 * {@link com.sourabh.order_service.scheduler.ExpireCouponsScheduler}
 * to find coupons that have passed their validity window.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see Coupon
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Finds an active coupon by its code.
     *
     * @param code the coupon code
     * @return an {@link Optional} containing the active coupon, or empty
     *         if no active coupon with the given code exists
     */
    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    /**
     * Checks whether a coupon with the given code already exists,
     * regardless of its active/inactive status.
     *
     * @param code the coupon code
     * @return {@code true} if a coupon with the code exists
     */
    boolean existsByCode(String code);

    /**
     * Finds all active coupons whose validity window has expired
     * (i.e. {@code validUntil} is before the given timestamp).
     *
     * <p>Used by the {@link com.sourabh.order_service.scheduler.ExpireCouponsScheduler}
     * to auto-deactivate expired coupons.</p>
     *
     * @param validUntil the reference timestamp (typically {@code LocalDateTime.now()})
     * @return list of expired but still active coupons
     */
    List<Coupon> findByValidUntilBeforeAndIsActiveTrue(LocalDateTime validUntil);
}
