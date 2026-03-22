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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyPointRepository loyaltyPointRepository;

    private static final double POINT_VALUE = 0.25;

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
