package com.sourabh.user_service.dto.response;

import com.sourabh.user_service.entity.PointsTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response payload describing a loyalty points transaction.
 */
@Getter
@Builder
public class LoyaltyPointResponse {
    private PointsTransactionType type;
    private int points;
    private int balanceAfter;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
}
