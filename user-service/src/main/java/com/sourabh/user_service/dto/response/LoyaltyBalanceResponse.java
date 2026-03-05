package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response payload describing a user's current loyalty-points balance.
 *
 * <p>The monetary equivalent is calculated at 1 point = \u20B90.25 and is
 * provided for convenience so that the frontend can display the redeemable
 * value directly.</p>
 */
@Getter
@Builder
public class LoyaltyBalanceResponse {

    /** UUID of the user whose balance is being reported. */
    private String userUuid;

    /** Current points balance. */
    private int balance;

    /** Equivalent monetary value of the balance (1 point = \u20B90.25). */
    private double monetaryValue;
}
