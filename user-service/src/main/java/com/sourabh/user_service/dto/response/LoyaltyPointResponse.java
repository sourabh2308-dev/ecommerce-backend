package com.sourabh.user_service.dto.response;

import com.sourabh.user_service.entity.PointsTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response payload describing a single loyalty-points transaction
 * (earn or redeem) in the user's loyalty ledger.
 */
@Getter
@Builder
public class LoyaltyPointResponse {

    /** Type of transaction (EARN or REDEEM). */
    private PointsTransactionType type;

    /** Number of points involved in this transaction. */
    private int points;

    /** Running balance after this transaction was applied. */
    private int balanceAfter;

    /** Optional reference identifier (e.g. order UUID). */
    private String referenceId;

    /** Human-readable description of the transaction. */
    private String description;

    /** Timestamp when the transaction was recorded. */
    private LocalDateTime createdAt;
}
