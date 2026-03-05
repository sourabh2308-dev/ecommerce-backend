package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a single ledger entry in the loyalty-points
 * programme.
 * <p>
 * Every earn, redeem, expiry, or admin adjustment is recorded as a
 * separate row. The {@link #balanceAfter} column keeps a running total
 * so the current balance can be read from the latest transaction
 * without summing the full history.
 * </p>
 *
 * <p>Mapped to the {@code loyalty_points} table.</p>
 *
 * @see PointsTransactionType
 */
@Entity
@Table(name = "loyalty_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPoint {

    /** Database surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID of the user to whom this points transaction belongs. */
    @Column(nullable = false)
    private String userUuid;

    /** Type of the points transaction (earned, redeemed, expired, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointsTransactionType type;

    /** Point delta: positive for earned / adjusted, negative for redeemed / expired. */
    @Column(nullable = false)
    private int points;

    /** Running balance immediately after this transaction was applied. */
    @Column(nullable = false)
    private int balanceAfter;

    /** Optional reference identifier, typically an order UUID or review UUID. */
    private String referenceId;

    /** Human-readable description of the transaction. */
    private String description;

    /** Timestamp of when this transaction was created. */
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
