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
 * Implementation of {@link CouponService} that manages the complete coupon lifecycle.
 *
 * <p>Handles coupon creation (with duplicate-code prevention), multi-rule validation,
 * discount calculation (percentage with optional cap, or fixed amount), per-user and
 * global usage tracking, and coupon deactivation.</p>
 *
 * <p>All write operations are transactional; read-only queries use
 * {@code @Transactional(readOnly = true)} for connection-pool optimisation.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    /** Repository for {@link Coupon} entity persistence. */
    private final CouponRepository couponRepository;

    /** Repository for recording and querying per-user coupon redemptions. */
    private final CouponUsageRepository couponUsageRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Normalises the code to uppercase, checks for duplicates, applies sensible
     * defaults for optional limits, and persists the new coupon.</p>
     *
     * @param request coupon creation payload
     * @return response DTO with the persisted coupon details
     * @throws IllegalArgumentException if a coupon with the same code already exists
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
     * {@inheritDoc}
     *
     * <p>Sequentially checks: active status, validity window, global usage limit,
     * per-user usage limit, and minimum order amount. On success the discount is
     * calculated (percentage capped by {@code maxDiscount}, or fixed amount) and
     * rounded to two decimal places.</p>
     *
     * @param code        coupon code (case-insensitive)
     * @param orderAmount order total before discount
     * @param buyerUuid   buyer attempting to redeem
     * @return validation result with discount and final amount, or failure reason
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
     * {@inheritDoc}
     *
     * <p>Atomically increments the coupon’s global {@code usedCount} and
     * creates a {@link CouponUsage} audit record linking coupon, buyer, and order.
     * Both operations participate in the same transaction.</p>
     *
     * @param code      coupon code applied to the order
     * @param buyerUuid UUID of the buyer
     * @param orderUuid UUID of the order
     * @throws RuntimeException if coupon is not found or inactive
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
     * {@inheritDoc}
     *
     * <p>Sets the coupon’s {@code isActive} flag to {@code false} without deleting
     * the record, preserving it for historical and audit purposes.</p>
     *
     * @param code coupon code to deactivate
     * @return confirmation string
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
     * {@inheritDoc}
     *
     * <p>Fetches all coupons (active and inactive) and maps each to a
     * {@link CouponResponse} for administrative display.</p>
     *
     * @return list of all coupon response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> listAll() {
        return couponRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    /**
     * Computes the discount amount for the given coupon and order total.
     *
     * <p>For {@code PERCENTAGE} coupons the result is capped by {@code maxDiscount}
     * when present. The value is rounded to two decimal places.</p>
     *
     * @param coupon      the coupon entity containing discount configuration
     * @param orderAmount the order total used as the basis for percentage discounts
     * @return calculated discount amount
     */
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

    /**
     * Maps a {@link Coupon} entity to a {@link CouponResponse} DTO.
     *
     * @param c the coupon entity to convert
     * @return populated response DTO
     */
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
