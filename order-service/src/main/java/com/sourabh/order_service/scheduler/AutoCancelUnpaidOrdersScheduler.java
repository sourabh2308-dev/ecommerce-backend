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

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCancelUnpaidOrdersScheduler {

    private final OrderRepository orderRepository;

    @Value("${scheduler.auto-cancel-unpaid-orders.enabled:true}")
    private boolean enabled;

    @Value("${scheduler.auto-cancel-unpaid-orders.minutes:30}")
    private Integer unpaidOrderThresholdMinutes;

    @Value("${scheduler.auto-cancel-unpaid-orders.cron:0 */5 * * * *}")
    private String cronExpression;

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
