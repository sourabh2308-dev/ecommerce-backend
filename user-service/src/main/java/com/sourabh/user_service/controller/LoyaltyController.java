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

/**
 * REST controller for the loyalty-points programme.
 * <p>
 * Buyers can view their balance, browse transaction history, and
 * redeem points at checkout. Administrators can manually award or
 * adjust points for any user.
 * </p>
 *
 * <p>Base path: {@code /api/user/loyalty}</p>
 *
 * @see LoyaltyService
 */
@RestController
@RequestMapping("/api/user/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    /** Service layer handling loyalty-points business logic. */
    private final LoyaltyService loyaltyService;

    /**
     * Returns the current loyalty-points balance for the authenticated buyer.
     *
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return {@link LoyaltyBalanceResponse} with the current balance
     */
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/balance")
    public ResponseEntity<LoyaltyBalanceResponse> getBalance(HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(loyaltyService.getBalance(userUuid));
    }

    /**
     * Returns a paginated history of loyalty-point transactions for
     * the authenticated buyer, ordered newest-first.
     *
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 20)
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return paginated {@link LoyaltyPointResponse} list
     */
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/history")
    public ResponseEntity<PageResponse<LoyaltyPointResponse>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(loyaltyService.getHistory(userUuid, page, size));
    }

    /**
     * Redeems a specified number of loyalty points against an order,
     * returning the monetary discount earned.
     *
     * @param points      number of points the buyer wants to redeem
     * @param orderUuid   UUID of the order to apply the discount to
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return map containing redeemed points, discount amount, and a message
     */
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

    /**
     * Admin-only endpoint to manually award loyalty points to any user.
     *
     * @param userUuid    UUID of the user to receive points
     * @param points      number of points to award
     * @param description optional reason for the adjustment (defaults to "Admin adjustment")
     * @return confirmation message
     */
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
