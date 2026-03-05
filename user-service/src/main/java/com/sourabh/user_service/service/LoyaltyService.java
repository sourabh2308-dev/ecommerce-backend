package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.response.LoyaltyBalanceResponse;
import com.sourabh.user_service.dto.response.LoyaltyPointResponse;
import com.sourabh.user_service.entity.PointsTransactionType;

/**
 * Service interface for the loyalty-points programme.
 *
 * <p>Users earn points after order delivery and may redeem them at checkout
 * for a monetary discount. The conversion rate is configurable in the
 * implementation (default: 1 point = ₹0.25).</p>
 *
 * <p>Every earn/redeem operation is recorded as a {@code LoyaltyPoint}
 * transaction with a running {@code balanceAfter} snapshot.</p>
 *
 * @see com.sourabh.user_service.service.impl.LoyaltyServiceImpl
 */
public interface LoyaltyService {

    /**
     * Credits loyalty points to a user's account.
     *
     * <p>Typically invoked by the Kafka order-event consumer after an order
     * is marked as delivered (e.g. ₹100 spent = 10 points).</p>
     *
     * @param userUuid    the UUID of the user earning points
     * @param points      the number of points to credit (positive integer)
     * @param type        the transaction type (e.g. {@code EARNED}, {@code BONUS})
     * @param referenceId an external reference such as the order UUID
     * @param description a human-readable description of the transaction
     */
    void earnPoints(String userUuid, int points, PointsTransactionType type, String referenceId, String description);

    /**
     * Redeems loyalty points at checkout and returns the monetary discount.
     *
     * @param userUuid       the UUID of the user redeeming points
     * @param pointsToRedeem the number of points the user wishes to redeem
     * @param orderUuid      the order against which points are being redeemed
     * @return the discount amount in rupees (₹) based on the point-to-currency ratio
     * @throws RuntimeException if the user has insufficient points
     */
    double redeemPoints(String userUuid, int pointsToRedeem, String orderUuid);

    /**
     * Returns the current loyalty-points balance and its monetary equivalent.
     *
     * @param userUuid the UUID of the user
     * @return a {@link LoyaltyBalanceResponse} containing balance and monetary value
     */
    LoyaltyBalanceResponse getBalance(String userUuid);

    /**
     * Retrieves a paginated history of loyalty-point transactions.
     *
     * @param userUuid the UUID of the user
     * @param page     zero-based page index
     * @param size     number of records per page
     * @return a {@link PageResponse} of {@link LoyaltyPointResponse} entries ordered by date descending
     */
    PageResponse<LoyaltyPointResponse> getHistory(String userUuid, int page, int size);
}
