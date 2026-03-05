package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing a customer-support ticket raised by a
 * {@link User}.
 * <p>
 * A ticket tracks the lifecycle of a support request from creation
 * ({@link TicketStatus#OPEN}) through assignment and resolution.
 * Threaded conversation is modelled via the one-to-many relationship
 * with {@link SupportMessage}. An optional {@link #orderUuid} links
 * the ticket to a specific order for context.
 * </p>
 *
 * <p>Mapped to the {@code support_tickets} table. Inherits audit
 * timestamps from {@link BaseAuditEntity}.</p>
 *
 * @see SupportMessage
 * @see TicketCategory
 * @see TicketStatus
 */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket extends BaseAuditEntity {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Publicly-exposed unique identifier; auto-generated on first persist. */
    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    /** UUID of the user who created this ticket. */
    @Column(nullable = false)
    private String userUuid;

    /** Short summary describing the issue. */
    @Column(nullable = false)
    private String subject;

    /** Detailed description of the problem (up to 2 000 characters). */
    @Column(length = 2000, nullable = false)
    private String description;

    /** Broad category that the issue falls into. Defaults to {@link TicketCategory#OTHER}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketCategory category = TicketCategory.OTHER;

    /** Current processing status of the ticket. Defaults to {@link TicketStatus#OPEN}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    /** Optional UUID of the order related to this support request. */
    private String orderUuid;

    /** UUID of the admin to whom this ticket is currently assigned. */
    private String assignedAdminUuid;

    /** Timestamp when the ticket was marked as resolved. */
    private LocalDateTime resolvedAt;

    /** Ordered list of conversation messages, oldest first. */
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("createdAt ASC")
    private List<SupportMessage> messages = new ArrayList<>();

    /**
     * JPA lifecycle callback that generates a random UUID for this
     * ticket if one has not already been assigned.
     */
    @PrePersist
    protected void onCreateTicket() {
        if (this.uuid == null) this.uuid = UUID.randomUUID().toString();
    }
}
