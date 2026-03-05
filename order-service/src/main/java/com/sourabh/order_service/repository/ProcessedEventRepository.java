package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link ProcessedEvent} entities.
 *
 * <p>Used by Kafka consumers to check whether an event has already been
 * processed, providing idempotent event consumption and preventing
 * duplicate side-effects.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ProcessedEvent
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * Checks whether an event with the given idempotency key has already
     * been processed.
     *
     * @param eventId the unique event / deduplication key
     * @return {@code true} if the event has already been recorded
     */
    boolean existsByEventId(String eventId);
}
