package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link CouponUsage} entities.
 *
 * <p>Primarily used to enforce per-user coupon redemption limits by
 * counting the number of times a specific buyer has redeemed a given
 * coupon.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see CouponUsage
 */
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /**
     * Counts how many times a specific buyer has redeemed a given coupon.
     *
     * @param couponId  the database ID of the coupon
     * @param buyerUuid the UUID of the buyer
     * @return the number of redemptions
     */
    int countByCouponIdAndBuyerUuid(Long couponId, String buyerUuid);
}
