package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.dto.request.CreateCouponRequest;
import com.sourabh.order_service.dto.response.CouponResponse;
import com.sourabh.order_service.dto.response.CouponValidationResponse;
import com.sourabh.order_service.entity.Coupon;
import com.sourabh.order_service.entity.CouponUsage;
import com.sourabh.order_service.repository.CouponRepository;
import com.sourabh.order_service.repository.CouponUsageRepository;
import com.sourabh.order_service.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * COUPON SERVICE IMPLEMENTATION
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Manages coupon creation, validation, tracking, and lifecycle in the order system.
 * This service orchestrates:
 *   1. Coupon creation with validation (duplicate code prevention)
 *   2. Coupon validation against order amount and business rules
 *   3. Coupon usage tracking per user and globally
 *   4. Automatic discount calculation (percentage or fixed amount)
 *   5. Coupon deactivation and lifecycle management
 * 
 * KEY RESPONSIBILITIES:
 * ---------------------
 * - Create coupons with discount types (PERCENTAGE, FIXED_AMOUNT)
 * - Validate coupon applicability (expiry, usage limits, minimum order amount)
 * - Calculate appropriate discount amounts with maximum cap enforcement
 * - Track usage per user to enforce per-user limits
 * - Maintain global usage count for total limit enforcement
 * - Deactivate expired or exhausted coupons
 * 
 * COUPON TYPES:
 * ─────────────
 * PERCENTAGE: Discount applied as percentage of order amount, capped by maxDiscount
 * FIXED_AMOUNT: Discount applied as fixed amount regardless of order value
 * 
 * VALIDATION RULES:
 * ─────────────────
 * - Code uniqueness: Prevent duplicate coupon codes in system
 * - Validity window: Current time must be between validFrom and validUntil
 * - Global limit: Used count must not exceed totalUsageLimit
 * - Per-user limit: User usage count must not exceed perUserLimit
 * - Minimum order: Order amount must be >= minOrderAmount
 * 
 * BUSINESS LOGIC:
 * ───────────────
 * When validating a coupon, the service checks all constraints in order:
 * 1. Coupon exists and is active
 * 2. Current time is within validity period
 * 3. Global usage limit not reached
 * 4. User has not exceeded personal usage limit
 * 5. Order amount meets minimum threshold
 * 6. Calculate discount (respecting max cap for percentage discounts)
 * 
 * DEPENDENCIES:
 * ──────────────
 * - CouponRepository: JPA repository for coupon entities (CRUD operations)
 * - CouponUsageRepository: JPA repository for tracking per-user coupon usage
 * 
 * ANNOTATIONS:
 * ─────────────
 * @Service: Marks class as Spring service layer component (business logic)
 * @RequiredArgsConstructor: Lombok generates constructor for final fields
 * @Transactional: Default transactional behavior for create/update operations
 * @Transactional(readOnly = true): Optimizes database access for query-only methods
 * 
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    /**
     * Create a new coupon with discount configuration.
     * 
     * PURPOSE:
     * Creates a new coupon and stores it in the database after validating
     * that the coupon code is not already in use.
     * 
     * PROCESS:
     * 1. Check if coupon code already exists (case-insensitive)
     * 2. Throw exception if code is duplicate (prevent conflicts)
     * 3. Normalize code to uppercase for consistency
     * 4. Build coupon entity with all configuration parameters
     * 5. Set defaults for optional fields (minOrderAmount: 0, totalUsageLimit: 1000, etc.)
     * 6. Save to database and return response
     * 
     * DISCOUNT CALCULATION:
     * - PERCENTAGE: Applies percentage discount, capped by maxDiscount if set
     * - FIXED_AMOUNT: Applies fixed discount amount regardless of order total
     * 
     * VALIDATION:
     * - Code must be unique (case-insensitive comparison)
     * - discountType must be valid enum value (PERCENTAGE or FIXED_AMOUNT)
     * - validFrom must be before validUntil for validity window
     * 
     * @param request CreateCouponRequest containing coupon configuration:
     *        - code: Unique coupon code to use at checkout
     *        - discountType: Either "PERCENTAGE" or "FIXED_AMOUNT"
     *        - discountValue: Percentage (0-100) or fixed amount
     *        - minOrderAmount: Minimum order amount to apply coupon (optional, default: 0)
     *        - maxDiscount: Maximum discount cap for percentage coupons (optional)
     *        - totalUsageLimit: Total times coupon can be used globally (optional, default: 1000)
     *        - perUserLimit: Maximum times one user can use coupon (optional, default: 1)
     *        - validFrom: Coupon start validity date
     *        - validUntil: Coupon end validity date
     *        - sellerUuid: UUID of seller creating coupon
     * 
     * @return CouponResponse with created coupon details
     * @throws IllegalArgumentException if coupon code already exists
     */
    @Override
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists");
        }
        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountType(Coupon.DiscountType.valueOf(request.getDiscountType()))
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : 0.0)
                .maxDiscount(request.getMaxDiscount())
                .totalUsageLimit(request.getTotalUsageLimit() != null ? request.getTotalUsageLimit() : 1000)
                .perUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .sellerUuid(request.getSellerUuid())
                .build();
        return mapToResponse(couponRepository.save(coupon));
    }

    /**
     * Validate if a coupon can be applied to an order and calculate discount.
     * 
     * PURPOSE:
     * Performs comprehensive validation of a coupon against business rules
     * and calculates the discount amount if coupon is applicable.
     * 
     * VALIDATION SEQUENCE:
     * 1. Coupon exists and is active in database
     * 2. Current time is within validity period (validFrom to validUntil)
     * 3. Global usage limit not exceeded (usedCount < totalUsageLimit)
     * 4. User has not exhausted personal usage limit
     * 5. Order amount meets minimum threshold (orderAmount >= minOrderAmount)
     * 6. Calculate discount with appropriate cap
     * 
     * DISCOUNT CALCULATION:
     * - PERCENTAGE: (orderAmount * discountValue / 100), capped by maxDiscount
     * - FIXED_AMOUNT: discountValue applied directly
     * - Result rounded to 2 decimal places for currency precision
     * 
     * RESPONSE HANDLING:
     * - If any validation fails: Returns CouponValidationResponse with valid=false and reason
     * - If all validations pass: Returns valid=true with calculated discount and final amount
     * - finalAmount = max(0, orderAmount - discountAmount) to prevent negative totals
     * 
     * @param code Coupon code to validate (converted to uppercase)
     * @param orderAmount Total order amount before discount
     * @param buyerUuid UUID of buyer attempting to use coupon (tracks per-user usage)
     * 
     * @return CouponValidationResponse containing:
     *         - valid: true if coupon can be applied, false otherwise
     *         - message: Reason if invalid (e.g., "Coupon expired", "You have already used this coupon")
     *         - discountAmount: Calculated discount if valid (null if invalid)
     *         - finalAmount: Order total after discount if valid (null if invalid)
     */
    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, Double orderAmount, String buyerUuid) {
        var optCoupon = couponRepository.findByCodeAndIsActiveTrue(code.toUpperCase());
        if (optCoupon.isEmpty()) {
            return CouponValidationResponse.builder().valid(false).message("Coupon not found or inactive").build();
        }
        Coupon coupon = optCoupon.get();
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            return CouponValidationResponse.builder().valid(false).message("Coupon expired or not yet valid").build();
        }
        if (coupon.getUsedCount() >= coupon.getTotalUsageLimit()) {
            return CouponValidationResponse.builder().valid(false).message("Coupon usage limit reached").build();
        }
        int userUsage = couponUsageRepository.countByCouponIdAndBuyerUuid(coupon.getId(), buyerUuid);
        if (userUsage >= coupon.getPerUserLimit()) {
            return CouponValidationResponse.builder().valid(false).message("You have already used this coupon").build();
        }
        if (orderAmount < coupon.getMinOrderAmount()) {
            return CouponValidationResponse.builder().valid(false)
                    .message("Minimum order amount is " + coupon.getMinOrderAmount()).build();
        }

        double discount = calculateDiscount(coupon, orderAmount);
        return CouponValidationResponse.builder()
                .valid(true)
                .message("Coupon applied")
                .discountAmount(discount)
                .finalAmount(Math.max(0, orderAmount - discount))
                .build();
    }

    /**
     * Record coupon usage when order is successfully created.
     * 
     * PURPOSE:
     * Atomically updates coupon usage tracking after order placement.
     * Increments global usage counter and creates usage record for audit trail.
     * 
     * PROCESS:
     * 1. Fetch coupon by code (throws exception if not found/inactive)
     * 2. Increment global usedCount by 1
     * 3. Save updated coupon to database
     * 4. Create CouponUsage record linking coupon, buyer, and order
     * 5. Persist usage record for tracking and audit purposes
     * 
     * TRANSACTION SAFETY:
     * @Transactional ensures both coupon update and usage record creation
     * are atomic (both succeed or both rollback on failure).
     * 
     * @param code Coupon code that was applied to order
     * @param buyerUuid UUID of buyer who used the coupon
     * @param orderUuid UUID of order in which coupon was applied
     * 
     * @throws RuntimeException if coupon not found or inactive
     */
    @Override
    @Transactional
    public void recordUsage(String code, String buyerUuid, String orderUuid) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        couponUsageRepository.save(CouponUsage.builder()
                .coupon(coupon)
                .buyerUuid(buyerUuid)
                .orderUuid(orderUuid)
                .build());
    }

    /**
     * Deactivate an active coupon to prevent further usage.
     * 
     * PURPOSE:
     * Marks a coupon as inactive to permanently disable it without deletion.
     * Maintains coupon records for audit trail and historical tracking.
     * 
     * PROCESS:
     * 1. Fetch coupon by code (must be active)
     * 2. Set isActive flag to false
     * 3. Save updated coupon to database
     * 4. Return confirmation message
     * 
     * USAGE SCENARIOS:
     * - Expired coupons past validUntil date
     * - Coupons with exhausted usage limits
     * - Admin-driven coupon cancellation
     * - Seasonal or promotional coupon closure
     * 
     * @param code Coupon code to deactivate
     * 
     * @return Confirmation message "Coupon deactivated"
     * @throws RuntimeException if coupon not found or already inactive
     */
    @Override
    @Transactional
    public String deactivateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setIsActive(false);
        couponRepository.save(coupon);
        return "Coupon deactivated";
    }

    /**
     * Retrieve all coupons in the system converts to response DTOs.
     * 
     * PURPOSE:
     * Provides list of all coupons for administrative purposes (view, manage, analytics).
     * Useful for seller dashboards and admin panels.
     * 
     * @return List of CouponResponse containing all coupons with their configurations
     */
    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> listAll() {
        return couponRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    private double calculateDiscount(Coupon coupon, double orderAmount) {
        double discount;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discount = orderAmount * (coupon.getDiscountValue() / 100.0);
            if (coupon.getMaxDiscount() != null) {
                discount = Math.min(discount, coupon.getMaxDiscount());
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        return Math.round(discount * 100.0) / 100.0;
    }

    private CouponResponse mapToResponse(Coupon c) {
        return CouponResponse.builder()
                .code(c.getCode())
                .discountType(c.getDiscountType().name())
                .discountValue(c.getDiscountValue())
                .minOrderAmount(c.getMinOrderAmount())
                .maxDiscount(c.getMaxDiscount())
                .totalUsageLimit(c.getTotalUsageLimit())
                .usedCount(c.getUsedCount())
                .perUserLimit(c.getPerUserLimit())
                .validFrom(c.getValidFrom())
                .validUntil(c.getValidUntil())
                .isActive(c.getIsActive())
                .sellerUuid(c.getSellerUuid())
                .build();
    }
}
