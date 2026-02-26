package com.sourabh.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ledger entry for loyalty points earned or redeemed by a user.
 */
@Entity
@Table(name = "loyalty_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointsTransactionType type;

    /** Positive for earned, negative for redeemed */
    @Column(nullable = false)
    private int points;

    /** Running balance after this transaction */
    @Column(nullable = false)
    private int balanceAfter;

    /** Optional reference (e.g. order UUID) */
    private String referenceId;

    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
