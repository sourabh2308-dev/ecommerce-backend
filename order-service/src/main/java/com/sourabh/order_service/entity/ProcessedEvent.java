package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity that records already-processed Kafka event identifiers to guarantee
 * idempotent event consumption across the order-service.
 *
 * <p>Before processing any incoming Kafka event, consumers check whether its
 * unique {@link #eventId} already exists in this table. If it does, the event
 * is treated as a duplicate and silently skipped, preventing side-effects such
 * as double stock adjustments or duplicate status updates.</p>
 *
 * <p>Mapped to the {@code processed_events} table with a unique index on
 * the {@code event_id} column for fast look-ups.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see com.sourabh.order_service.kafka.consumer.PaymentEventConsumer
 * @see com.sourabh.order_service.repository.ProcessedEventRepository
 */
@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_processed_events_event_id", columnList = "event_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    /**
     * Database-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier of the processed event, used as the idempotency /
     * deduplication key (e.g. {@code "payment-completed:<paymentUuid>"}).
     * Maximum length is 64 characters.
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    /**
     * Name of the Kafka topic from which the event was consumed
     * (e.g. {@code "payment.completed"}). Maximum length is 128 characters.
     */
    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    /**
     * Timestamp recording when the event was processed and this record
     * was persisted.
     */
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
