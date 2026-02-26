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
 * Automatically deactivates expired flash deals.
 * Runs periodically to mark flash deals with endTime < now() as inactive.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireFlashDealsScheduler {

    private final FlashDealRepository flashDealRepository;

    @Value("${scheduler.expire-flash-deals.enabled:true}")
    private boolean enabled;

    @Value("${scheduler.expire-flash-deals.cron:0 */5 * * * *}")  // Every 5 minutes
    private String cronExpression;

    /**
     * Run at configured interval (default: every 5 minutes).
     * Deactivate flash deals where endTime < now().
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
