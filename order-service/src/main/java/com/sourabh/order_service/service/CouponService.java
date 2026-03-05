package com.sourabh.order_service.service;

import com.sourabh.order_service.dto.request.CreateCouponRequest;
import com.sourabh.order_service.dto.response.CouponResponse;
import com.sourabh.order_service.dto.response.CouponValidationResponse;

import java.util.List;

/**
 * Service interface for coupon lifecycle management in the order-service microservice.
 *
 * <p>Provides operations for creating, validating, tracking, deactivating, and
 * listing discount coupons. Two discount types are supported:</p>
 * <ul>
 *   <li>{@code PERCENTAGE} – discount as a percentage of the order total, subject
 *       to an optional maximum cap ({@code maxDiscount}).</li>
 *   <li>{@code FIXED_AMOUNT} – a flat monetary discount regardless of order value.</li>
 * </ul>
 *
 * <p>Validation enforces expiry windows, global and per-user usage limits, and
 * minimum order amount thresholds before a coupon may be applied.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see CouponResponse
 * @see CouponValidationResponse
 */
public interface CouponService {

    /**
     * Creates a new coupon with the given discount configuration.
     *
     * <p>The code is normalised to uppercase and must be unique. Default values are
     * applied for optional fields: {@code minOrderAmount=0}, {@code totalUsageLimit=1000},
     * {@code perUserLimit=1}.</p>
     *
     * @param request creation payload containing code, discount type/value,
     *                validity window, usage limits, and optional seller UUID
     * @return {@link CouponResponse} representing the persisted coupon
     * @throws IllegalArgumentException if a coupon with the same code already exists
     */
    CouponResponse createCoupon(CreateCouponRequest request);

    /**
     * Validates a coupon against business rules and calculates the applicable discount.
     *
     * <p>Checks executed in order: existence/active flag, validity window,
     * global usage limit, per-user limit, and minimum order amount.</p>
     *
     * @param code        coupon code to validate (case-insensitive)
     * @param orderAmount total order amount before discount
     * @param buyerUuid   UUID of the buyer attempting to redeem the coupon
     * @return {@link CouponValidationResponse} with validity flag, discount amount,
     *         final amount, or an explanatory message when invalid
     */
    CouponValidationResponse validateCoupon(String code, Double orderAmount, String buyerUuid);

    /**
     * Records coupon usage after a successful order placement.
     *
     * <p>Atomically increments the global {@code usedCount} and persists a
     * {@code CouponUsage} audit record linking coupon, buyer, and order.</p>
     *
     * @param code      coupon code that was applied
     * @param buyerUuid UUID of the buyer who redeemed the coupon
     * @param orderUuid UUID of the order the coupon was applied to
     * @throws RuntimeException if the coupon is not found or is inactive
     */
    void recordUsage(String code, String buyerUuid, String orderUuid);

    /**
     * Deactivates an active coupon, preventing further redemptions.
     *
     * <p>The coupon record is retained for audit purposes; only the
     * {@code isActive} flag is set to {@code false}.</p>
     *
     * @param code coupon code to deactivate
     * @return confirmation message {@code "Coupon deactivated"}
     * @throws RuntimeException if the coupon is not found or already inactive
     */
    String deactivateCoupon(String code);

    /**
     * Lists every coupon in the system regardless of active status.
     *
     * <p>Intended for administrative dashboards and seller coupon-management views.</p>
     *
     * @return list of {@link CouponResponse} for all coupons
     */
    List<CouponResponse> listAll();
}
