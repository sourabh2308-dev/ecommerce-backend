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

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireLoyaltyPointsScheduler {

    private final LoyaltyPointRepository loyaltyPointRepository;

    @Value("${scheduler.expire-loyalty-points.enabled:true}")
    private boolean enabled;

    @Value("${scheduler.expire-loyalty-points.days:365}")
    private Integer expiryDays;

    @Value("${scheduler.expire-loyalty-points.cron:0 0 0 * * *}")
    private String cronExpression;

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
