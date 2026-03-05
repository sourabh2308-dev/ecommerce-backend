package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an in-app notification delivered to a user.
 * <p>
 * Notifications are created in response to domain events such as order
 * status changes, payment outcomes, or admin-triggered messages. Each
 * notification carries a {@link NotificationType} that the frontend
 * can use to apply appropriate icons and routing.
 * </p>
 *
 * <p>Mapped to the {@code notifications} table.</p>
 *
 * @see NotificationType
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Publicly-exposed unique identifier; auto-generated on first persist. */
    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    /** UUID of the user to whom this notification is addressed. */
    @Column(nullable = false)
    private String userUuid;

    /** Category of the notification (order, payment, promotion, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** Short summary shown as the notification headline. */
    @Column(nullable = false)
    private String title;

    /** Detailed notification body (up to 1 000 characters). */
    @Column(length = 1000)
    private String message;

    /** Optional reference to a related entity, such as an order or payment UUID. */
    private String referenceId;

    /** Whether the user has marked this notification as read. Defaults to {@code false}. */
    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    /** Timestamp of when this notification was created. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback invoked before the entity is first persisted.
     * Generates a UUID and sets {@link #createdAt} if they are not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (this.uuid == null) this.uuid = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
