package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a return or exchange request submitted by a buyer
 * after an order has been delivered.
 *
 * <p>A {@code ReturnRequest} tracks the full lifecycle of a return — from the
 * initial buyer request through admin approval/rejection, pickup scheduling,
 * warehouse receipt, and final resolution (refund or exchange). Each request
 * is linked to a specific order via {@link #orderUuid}.</p>
 *
 * <p>Mapped to the {@code return_request} database table. Inherits audit
 * timestamps from {@link BaseAuditEntity}. A UUID is auto-generated on
 * persist if not explicitly set.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ReturnType
 * @see Order
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "return_request")
public class ReturnRequest extends BaseAuditEntity {

    /**
     * Database-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Universally unique identifier for this return request, exposed to
     * external clients. Auto-generated on persist if not supplied.
     */
    @Column(unique = true, nullable = false)
    private String uuid;

    /**
     * UUID of the order for which the return is being requested.
     */
    @Column(nullable = false)
    private String orderUuid;

    /**
     * UUID of the buyer who submitted the return request.
     */
    @Column(nullable = false)
    private String buyerUuid;

    /**
     * Whether the buyer wants a monetary refund or a replacement item.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnType returnType;

    /**
     * Free-text reason provided by the buyer for returning the order.
     * Maximum 1000 characters.
     */
    @Column(length = 1000)
    private String reason;

    /**
     * Current processing status of this return request. Defaults to
     * {@link ReturnStatus#PENDING} on creation.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.PENDING;

    /**
     * Optional notes added by an admin when approving or rejecting the request.
     */
    private String adminNotes;

    /**
     * Refund amount to be credited to the buyer. May differ from the order
     * total in the case of partial refunds.
     */
    private Double refundAmount;

    /**
     * Timestamp recording when the return request was resolved (approved,
     * rejected, refunded, or exchanged). {@code null} while still pending.
     */
    private LocalDateTime resolvedAt;

    /**
     * JPA lifecycle callback that auto-generates a UUID before the entity
     * is persisted, if one has not already been set.
     */
    @PrePersist
    private void generateUuid() {
        if (uuid == null) uuid = java.util.UUID.randomUUID().toString();
    }

    /**
     * Enumeration of processing states a {@link ReturnRequest} may pass
     * through during its lifecycle.
     */
    public enum ReturnStatus {
        /** Return request submitted, awaiting admin review. */
        PENDING,
        /** Return request approved by admin. */
        APPROVED,
        /** Return request rejected by admin. */
        REJECTED,
        /** A courier pickup has been scheduled for the returned item. */
        PICKUP_SCHEDULED,
        /** The returned item has been picked up from the buyer. */
        PICKED_UP,
        /** The returned item has been received at the warehouse. */
        RECEIVED,
        /** A monetary refund has been issued to the buyer. */
        REFUNDED,
        /** A replacement item has been dispatched to the buyer. */
        EXCHANGED
    }
}
