package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.response.LoyaltyBalanceResponse;
import com.sourabh.user_service.dto.response.LoyaltyPointResponse;
import com.sourabh.user_service.entity.PointsTransactionType;
import com.sourabh.user_service.service.LoyaltyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/balance")
    public ResponseEntity<LoyaltyBalanceResponse> getBalance(HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(loyaltyService.getBalance(userUuid));
    }

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/history")
    public ResponseEntity<PageResponse<LoyaltyPointResponse>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(loyaltyService.getHistory(userUuid, page, size));
    }

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/redeem")
    public ResponseEntity<Map<String, Object>> redeem(
            @RequestParam int points,
            @RequestParam String orderUuid,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        double discount = loyaltyService.redeemPoints(userUuid, points, orderUuid);
        return ResponseEntity.ok(Map.of(
                "pointsRedeemed", points,
                "discountAmount", discount,
                "message", "Points redeemed successfully"
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/award")
    public ResponseEntity<String> awardPoints(
            @RequestParam String userUuid,
            @RequestParam int points,
            @RequestParam(defaultValue = "Admin adjustment") String description) {
        loyaltyService.earnPoints(userUuid, points, PointsTransactionType.ADMIN_ADJUSTMENT, null, description);
        return ResponseEntity.ok("Awarded " + points + " points to user " + userUuid);
    }
}
