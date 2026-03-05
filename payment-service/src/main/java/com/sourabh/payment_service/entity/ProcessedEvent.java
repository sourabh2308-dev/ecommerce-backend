package com.sourabh.payment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity that records which Kafka event IDs have already been consumed,
 * providing <b>idempotent event processing</b>.
 *
 * <p>Before processing an incoming {@code OrderCreatedEvent}, the consumer
 * checks whether a {@code ProcessedEvent} with the same {@link #eventId}
 * already exists.  If it does, the event is silently skipped, preventing
 * duplicate payment creation when Kafka delivers the same message more
 * than once (at-least-once guarantee).
 *
 * <p>A unique index on {@code event_id} enforces the constraint at the
 * database level as an additional safety net.
 *
 * @see com.sourabh.payment_service.kafka.consumer.OrderEventConsumer
 */
@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_pe_event_id", columnList = "event_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique identifier of the Kafka event (carried inside the event payload). */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    /** Kafka topic from which the event was consumed (e.g. {@code order.created}). */
    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    /** Timestamp when the event was successfully processed. */
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
