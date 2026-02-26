package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.response.LoyaltyBalanceResponse;
import com.sourabh.user_service.dto.response.LoyaltyPointResponse;
import com.sourabh.user_service.entity.PointsTransactionType;

public interface LoyaltyService {

    /** Earn points (e.g., ₹100 spent = 10 points). Called after order delivery. */
    void earnPoints(String userUuid, int points, PointsTransactionType type, String referenceId, String description);

    /** Redeem points at checkout. Returns actual discount amount in rupees. */
    double redeemPoints(String userUuid, int pointsToRedeem, String orderUuid);

    LoyaltyBalanceResponse getBalance(String userUuid);

    PageResponse<LoyaltyPointResponse> getHistory(String userUuid, int page, int size);
}
