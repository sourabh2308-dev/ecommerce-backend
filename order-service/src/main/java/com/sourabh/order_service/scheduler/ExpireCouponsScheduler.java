package com.sourabh.order_service.scheduler;

import com.sourabh.order_service.entity.Coupon;
import com.sourabh.order_service.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that automatically deactivates coupons whose validity
 * window has passed.
 *
 * <p>Runs at a configurable interval (default: every hour) and sets
 * {@code isActive = false} on all coupons whose {@code validUntil}
 * timestamp is in the past but whose {@code isActive} flag is still
 * {@code true}.</p>
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code scheduler.expire-coupons.enabled} — master on/off
 *       switch (default {@code true})</li>
 *   <li>{@code scheduler.expire-coupons.cron} — cron expression
 *       (default {@code 0 0 * * * *})</li>
 * </ul>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see CouponRepository#findByValidUntilBeforeAndIsActiveTrue(LocalDateTime)
 * @see Coupon
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireCouponsScheduler {

    /** Coupon repository for querying and persisting coupon status. */
    private final CouponRepository couponRepository;

    /** Whether this scheduler is enabled. */
    @Value("${scheduler.expire-coupons.enabled:true}")
    private boolean enabled;

    /** Cron expression controlling the scheduler's execution frequency. */
    @Value("${scheduler.expire-coupons.cron:0 0 * * * *}")
    private String cronExpression;

    /**
     * Scans for active coupons that have exceeded their {@code validUntil}
     * timestamp and marks them as inactive.
     *
     * <p>Each coupon is deactivated individually so that a failure on one
     * record does not prevent the remaining coupons from being processed.</p>
     */
    @Scheduled(cron = "${scheduler.expire-coupons.cron:0 0 * * * *}")
    @Transactional
    public void expireCoupons() {
        if (!enabled) {
            log.debug("Expire coupons scheduler is disabled");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        try {
            List<Coupon> expiredCoupons = couponRepository.findByValidUntilBeforeAndIsActiveTrue(now);

            if (expiredCoupons.isEmpty()) {
                log.debug("No expired coupons to deactivate");
                return;
            }

            for (Coupon coupon : expiredCoupons) {
                try {
                    coupon.setIsActive(false);
                    couponRepository.save(coupon);
                    log.info("Expired coupon: {} (valid until: {})", coupon.getCode(), coupon.getValidUntil());
                } catch (Exception e) {
                    log.error("Failed to expire coupon {}: {}", coupon.getCode(), e.getMessage());
                }
            }

            log.info("Expire coupons completed: {} coupons deactivated", expiredCoupons.size());
        } catch (Exception e) {
            log.error("Error in expire coupons scheduler: {}", e.getMessage(), e);
        }
    }
}
