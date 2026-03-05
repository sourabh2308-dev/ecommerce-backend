package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing an individual message within a
 * {@link SupportTicket} conversation thread.
 * <p>
 * Messages are posted by either the customer (buyer/seller) or a
 * support administrator. The {@link #senderRole} field indicates
 * the role of the sender so the UI can render the conversation
 * with appropriate styling.
 * </p>
 *
 * <p>Mapped to the {@code support_messages} table.</p>
 *
 * @see SupportTicket
 */
@Entity
@Table(name = "support_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportMessage {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The support ticket to which this message belongs (many-to-one, lazy-loaded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    /** UUID of the user who sent this message. */
    @Column(nullable = false)
    private String senderUuid;

    /** Role of the sender at the time of posting (e.g. BUYER, SELLER, ADMIN). */
    @Column(nullable = false)
    private String senderRole;

    /** Body text of the message (up to 2 000 characters). */
    @Column(length = 2000, nullable = false)
    private String content;

    /** Timestamp of when this message was created. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback that sets {@link #createdAt} to the current
     * time when the entity is first persisted, if not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
