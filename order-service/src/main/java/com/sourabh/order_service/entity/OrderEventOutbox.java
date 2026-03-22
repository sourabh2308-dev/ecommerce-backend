package com.sourabh.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_event_outbox", indexes = {
        @Index(name = "idx_order_event_outbox_published_created", columnList = "published, created_at"),
        @Index(name = "idx_order_event_outbox_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_order_event_outbox_order_uuid", columnList = "order_uuid", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "order_uuid", nullable = false, unique = true, length = 64)
    private String orderUuid;

    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    @Builder.Default
    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}