package com.sourabh.order_service.scheduler;

import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that automatically cancels orders which remain unpaid
 * beyond a configurable time threshold.
 *
 * <p>By default the scheduler runs every 5 minutes and cancels any order
 * with status {@link OrderStatus#CREATED} whose {@code createdAt} timestamp
 * is older than 30 minutes. Both the cron expression and the threshold are
 * driven by external configuration properties.</p>
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code scheduler.auto-cancel-unpaid-orders.enabled} — master
 *       on/off switch (default {@code true})</li>
 *   <li>{@code scheduler.auto-cancel-unpaid-orders.minutes} — age
 *       threshold in minutes (default {@code 30})</li>
 *   <li>{@code scheduler.auto-cancel-unpaid-orders.cron} — cron
 *       expression (default {@code 0 *&#47;5 * * * *})</li>
 * </ul>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see OrderRepository#findByStatusAndCreatedAtBefore(OrderStatus, LocalDateTime)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCancelUnpaidOrdersScheduler {

    /** Order repository for querying and persisting order status changes. */
    private final OrderRepository orderRepository;

    /** Whether this scheduler is enabled. */
    @Value("${scheduler.auto-cancel-unpaid-orders.enabled:true}")
    private boolean enabled;

    /** Number of minutes after which an unpaid order is considered stale. */
    @Value("${scheduler.auto-cancel-unpaid-orders.minutes:30}")
    private Integer unpaidOrderThresholdMinutes;

    /** Cron expression controlling the scheduler's execution frequency. */
    @Value("${scheduler.auto-cancel-unpaid-orders.cron:0 */5 * * * *}")
    private String cronExpression;

    /**
     * Scans for stale unpaid orders and cancels them.
     *
     * <p>Orders with status {@link OrderStatus#CREATED} that were created more
     * than {@link #unpaidOrderThresholdMinutes} minutes ago are transitioned
     * to {@link OrderStatus#CANCELLED}. Each cancellation is persisted
     * individually so that a failure on one order does not prevent the
     * remaining orders from being processed.</p>
     */
    @Scheduled(cron = "${scheduler.auto-cancel-unpaid-orders.cron:0 */5 * * * *}")
    @Transactional
    public void autoCancelUnpaidOrders() {
        if (!enabled) {
            log.debug("Auto-cancel unpaid orders scheduler is disabled");
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(unpaidOrderThresholdMinutes);

        try {
            List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                    OrderStatus.CREATED,
                    threshold
            );

            if (expiredOrders.isEmpty()) {
                log.debug("No unpaid orders to cancel");
                return;
            }

            for (Order order : expiredOrders) {
                try {
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    log.info("Auto-cancelled unpaid order: {} (created at: {})", order.getUuid(), order.getCreatedAt());
                } catch (Exception e) {
                    log.error("Failed to auto-cancel order {}: {}", order.getUuid(), e.getMessage());
                }
            }

            log.info("Auto-cancel unpaid orders completed: {} orders cancelled", expiredOrders.size());
        } catch (Exception e) {
            log.error("Error in auto-cancel unpaid orders scheduler: {}", e.getMessage(), e);
        }
    }
}
