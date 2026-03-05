package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.response.LoyaltyBalanceResponse;
import com.sourabh.user_service.dto.response.LoyaltyPointResponse;
import com.sourabh.user_service.entity.LoyaltyPoint;
import com.sourabh.user_service.entity.PointsTransactionType;
import com.sourabh.user_service.repository.LoyaltyPointRepository;
import com.sourabh.user_service.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link LoyaltyService} for the loyalty-points programme.
 *
 * <p>Point-to-currency conversion rate: <strong>1 point = ₹{@value #POINT_VALUE}</strong>.
 * Points are earned post-delivery and may be redeemed at checkout for a
 * monetary discount.  Every transaction (earn or redeem) is persisted as
 * a {@link LoyaltyPoint} row with a {@code balanceAfter} snapshot to enable
 * an auditable transaction history.</p>
 *
 * <p>The class-level {@code @Transactional} ensures that balance reads and
 * writes are consistent within a single database transaction.</p>
 *
 * @see LoyaltyService
 * @see LoyaltyPointRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoyaltyServiceImpl implements LoyaltyService {

    /** Repository for loyalty-point transaction persistence. */
    private final LoyaltyPointRepository loyaltyPointRepository;

    /** Monetary value per loyalty point in Indian Rupees (₹). */
    private static final double POINT_VALUE = 0.25;

    /**
     * {@inheritDoc}
     *
     * <p>Computes the new balance by adding <em>points</em> to the current balance
     * and records the transaction with the supplied metadata.</p>
     */
    @Override
    public void earnPoints(String userUuid, int points, PointsTransactionType type,
                           String referenceId, String description) {
        int currentBalance = loyaltyPointRepository.getBalance(userUuid);
        int newBalance = currentBalance + points;

        LoyaltyPoint lp = LoyaltyPoint.builder()
                .userUuid(userUuid)
                .type(type)
                .points(points)
                .balanceAfter(newBalance)
                .referenceId(referenceId)
                .description(description)
                .build();
        loyaltyPointRepository.save(lp);
        log.info("Points earned: userUuid={}, points={}, balance={}", userUuid, points, newBalance);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates that the user has enough points, deducts them, records a
     * {@code REDEEMED} transaction, and returns the corresponding discount in ₹.</p>
     */
    @Override
    public double redeemPoints(String userUuid, int pointsToRedeem, String orderUuid) {
        int currentBalance = loyaltyPointRepository.getBalance(userUuid);
        if (pointsToRedeem > currentBalance) {
            throw new RuntimeException("Insufficient loyalty points. Available: " + currentBalance);
        }
        int newBalance = currentBalance - pointsToRedeem;
        LoyaltyPoint lp = LoyaltyPoint.builder()
                .userUuid(userUuid)
                .type(PointsTransactionType.REDEEMED)
                .points(-pointsToRedeem)
                .balanceAfter(newBalance)
                .referenceId(orderUuid)
                .description("Redeemed " + pointsToRedeem + " points for order " + orderUuid)
                .build();
        loyaltyPointRepository.save(lp);
        double discount = pointsToRedeem * POINT_VALUE;
        log.info("Points redeemed: userUuid={}, points={}, discount=₹{}", userUuid, pointsToRedeem, discount);
        return discount;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getBalance(String userUuid) {
        int balance = loyaltyPointRepository.getBalance(userUuid);
        return LoyaltyBalanceResponse.builder()
                .userUuid(userUuid)
                .balance(balance)
                .monetaryValue(balance * POINT_VALUE)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoyaltyPointResponse> getHistory(String userUuid, int page, int size) {
        Page<LoyaltyPoint> lpPage = loyaltyPointRepository
                .findByUserUuidOrderByCreatedAtDesc(userUuid, PageRequest.of(page, size));
        return PageResponse.<LoyaltyPointResponse>builder()
                .content(lpPage.getContent().stream().map(this::mapToResponse).toList())
                .page(lpPage.getNumber())
                .size(lpPage.getSize())
                .totalElements(lpPage.getTotalElements())
                .totalPages(lpPage.getTotalPages())
                .last(lpPage.isLast())
                .build();
    }

    /**
     * Maps a {@link LoyaltyPoint} entity to a {@link LoyaltyPointResponse} DTO.
     *
     * @param lp the loyalty-point transaction entity
     * @return the corresponding response DTO
     */
    private LoyaltyPointResponse mapToResponse(LoyaltyPoint lp) {
        return LoyaltyPointResponse.builder()
                .type(lp.getType())
                .points(lp.getPoints())
                .balanceAfter(lp.getBalanceAfter())
                .referenceId(lp.getReferenceId())
                .description(lp.getDescription())
                .createdAt(lp.getCreatedAt())
                .build();
    }
}
