package com.sourabh.order_service.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a single audit trail record stored in the
 * {@code audit_logs} PostgreSQL table.
 *
 * <p>Every auditable user or system action within the order-service is
 * captured as an {@code AuditLog} row, enabling compliance reporting,
 * incident investigation, and analytics.</p>
 *
 * <p>The table is indexed on actor UUID, resource type/ID composite,
 * action type, and creation timestamp for efficient querying.</p>
 *
 * @see AuditAspect
 * @see Auditable
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor_uuid"),
        @Index(name = "idx_audit_resource", columnList = "resource_type,resource_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_timestamp", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The type of operation that was performed (e.g., CREATE, UPDATE, DELETE). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    /** UUID of the user or system principal who performed the action. */
    @Column(nullable = false)
    private String actorUuid;

    /** Role of the actor at the time of the action (e.g., BUYER, SELLER, ADMIN). */
    @Column(nullable = false)
    private String actorRole;

    /** Logical type of the affected resource (e.g., ORDER, COUPON). */
    @Column(nullable = false)
    private String resourceType;

    /** Unique identifier of the affected resource (typically its UUID). */
    @Column(nullable = false)
    private String resourceId;

    /** Human-readable description or method name providing additional context (max 500 chars). */
    @Column(length = 500)
    private String details;

    /** Optional JSON-serialised before/after snapshot of the changed data (max 2000 chars). */
    @Column(length = 2000)
    private String changeData;

    /** IP address of the client that initiated the request. */
    @Column(nullable = false)
    private String ipAddress;

    /** Timestamp when the audit record was created; auto-set on first persist. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback that sets the {@link #createdAt} timestamp
     * to the current time if it has not already been assigned.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
