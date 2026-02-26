package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response payload describing the current loyalty balance.
 */
@Getter
@Builder
public class LoyaltyBalanceResponse {
    private String userUuid;
    private int balance;
    /** 1 point = ₹0.25 */
    private double monetaryValue;
}
