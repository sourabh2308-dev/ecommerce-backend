package com.sourabh.user_service.scheduler;

import com.sourabh.user_service.entity.LoyaltyPoint;
import com.sourabh.user_service.entity.PointsTransactionType;
import com.sourabh.user_service.repository.LoyaltyPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that automatically expires loyalty points that have
 * remained unredeemed beyond a configurable threshold (default: 365 days).
 * <p>
 * Runs daily at midnight (configurable via
 * {@code scheduler.expire-loyalty-points.cron}) and can be disabled
 * entirely via {@code scheduler.expire-loyalty-points.enabled}.
 * </p>
 *
 * @see LoyaltyPoint
 * @see LoyaltyPointRepository#findPointsToExpire(LocalDateTime)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireLoyaltyPointsScheduler {

    /** Repository used to query and persist loyalty-point records. */
    private final LoyaltyPointRepository loyaltyPointRepository;

    /** Feature toggle — set to {@code false} to skip expiration. */
    @Value("${scheduler.expire-loyalty-points.enabled:true}")
    private boolean enabled;

    /** Number of days after which unredeemed points expire. */
    @Value("${scheduler.expire-loyalty-points.days:365}")
    private Integer expiryDays;

    /**
     * Cron expression controlling execution frequency.
     * Defaults to {@code 0 0 0 * * *} (midnight every day).
     */
    @Value("${scheduler.expire-loyalty-points.cron:0 0 0 * * *}")
    private String cronExpression;

    /**
     * Core scheduled method executed at the configured cron interval.
     * <p>
     * Finds all earned loyalty-point records older than {@link #expiryDays}
     * that have not yet been redeemed or already expired, negates their
     * point value, and marks them as {@link PointsTransactionType#EXPIRED}.
     * Each record is saved individually so that a single failure does not
     * roll back the entire batch.
     * </p>
     */
    @Scheduled(cron = "${scheduler.expire-loyalty-points.cron:0 0 0 * * *}")
    @Transactional
    public void expireLoyaltyPoints() {
        if (!enabled) {
            log.debug("Expire loyalty points scheduler is disabled");
            return;
        }

        LocalDateTime expiryThreshold = LocalDateTime.now().minusDays(expiryDays);

        try {
            List<LoyaltyPoint> pointsToExpire = loyaltyPointRepository.findPointsToExpire(expiryThreshold);

            if (pointsToExpire.isEmpty()) {
                log.debug("No loyalty points to expire");
                return;
            }

            for (LoyaltyPoint point : pointsToExpire) {
                try {
                    point.setType(PointsTransactionType.EXPIRED);
                    point.setPoints(-point.getPoints());
                    point.setDescription("Auto-expired due to inactivity");
                    loyaltyPointRepository.save(point);
                    log.info("Expired loyalty points for user: {} ({} points)",
                            point.getUserUuid(), Math.abs(point.getPoints()));
                } catch (Exception e) {
                    log.error("Failed to expire loyalty points for user {}: {}",
                            point.getUserUuid(), e.getMessage());
                }
            }

            log.info("Expire loyalty points completed: {} point records marked as expired", pointsToExpire.size());
        } catch (Exception e) {
            log.error("Error in expire loyalty points scheduler: {}", e.getMessage(), e);
        }
    }
}
