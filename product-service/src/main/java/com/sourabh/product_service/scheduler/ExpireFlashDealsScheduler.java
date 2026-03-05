package com.sourabh.product_service.scheduler;

import com.sourabh.product_service.entity.FlashDeal;
import com.sourabh.product_service.repository.FlashDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that automatically deactivates expired flash deals.
 * <p>
 * Runs at a configurable cron interval (default: every 5 minutes). On each
 * execution it queries for flash deals whose {@code endTime} has passed but
 * are still marked as active, then sets their {@code isActive} flag to
 * {@code false}. The scheduler can be disabled entirely via the
 * {@code scheduler.expire-flash-deals.enabled} application property.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireFlashDealsScheduler {

    /** Repository used to query and persist flash deal entities. */
    private final FlashDealRepository flashDealRepository;

    /** Feature flag to enable or disable this scheduler at runtime. */
    @Value("${scheduler.expire-flash-deals.enabled:true}")
    private boolean enabled;

    /** Cron expression controlling the scheduler's execution frequency. */
    @Value("${scheduler.expire-flash-deals.cron:0 */5 * * * *}")
    private String cronExpression;

    /**
     * Finds and deactivates all flash deals whose end time has elapsed.
     * <p>
     * Executed at the interval defined by the
     * {@code scheduler.expire-flash-deals.cron} property. Each expired deal is
     * individually saved so that a failure on one deal does not prevent the
     * others from being processed. The entire method runs within a single
     * transaction for consistency.
     * </p>
     */
    @Scheduled(cron = "${scheduler.expire-flash-deals.cron:0 */5 * * * *}")
    @Transactional
    public void expireFlashDeals() {
        if (!enabled) {
            log.debug("Expire flash deals scheduler is disabled");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        try {
            List<FlashDeal> expiredDeals = flashDealRepository.findByEndTimeBeforeAndIsActiveTrue(now);

            if (expiredDeals.isEmpty()) {
                log.debug("No expired flash deals to deactivate");
                return;
            }

            for (FlashDeal deal : expiredDeals) {
                try {
                    deal.setIsActive(false);
                    flashDealRepository.save(deal);
                    log.info("Expired flash deal for product ID: {} (ended at: {})", 
                            deal.getProduct().getId(), deal.getEndTime());
                } catch (Exception e) {
                    log.error("Failed to expire flash deal ID {}: {}", deal.getId(), e.getMessage());
                }
            }

            log.info("Expire flash deals completed: {} deals deactivated", expiredDeals.size());
        } catch (Exception e) {
            log.error("Error in expire flash deals scheduler: {}", e.getMessage(), e);
        }
    }
}
