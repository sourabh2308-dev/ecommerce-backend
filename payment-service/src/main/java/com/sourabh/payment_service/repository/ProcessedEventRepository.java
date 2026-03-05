package com.sourabh.payment_service.repository;

import com.sourabh.payment_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link ProcessedEvent} entities.
 *
 * <p>Used exclusively by the Kafka consumer to enforce <strong>idempotency</strong>.
 * Before processing an incoming event the consumer checks
 * {@link #existsByEventId(String)}; if a record already exists the event
 * is silently skipped, preventing duplicate payment creation when Kafka
 * redelivers a message.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * Checks whether an event with the given composite identifier has
     * already been processed.
     *
     * @param eventId a unique event key (e.g. {@code "ORDER_CREATED:<orderUuid>"})
     * @return {@code true} if the event was already consumed
     */
    boolean existsByEventId(String eventId);
}
