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
 * Automatically deactivates expired coupons.
 * Runs periodically to mark coupons with valid_until < now() as inactive.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireCouponsScheduler {

    private final CouponRepository couponRepository;

    @Value("${scheduler.expire-coupons.enabled:true}")
    private boolean enabled;

    @Value("${scheduler.expire-coupons.cron:0 0 * * * *}")  // Every hour
    private String cronExpression;

    /**
     * Run at configured interval (default: every hour).
     * Deactivate coupons where valid_until < now().
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
