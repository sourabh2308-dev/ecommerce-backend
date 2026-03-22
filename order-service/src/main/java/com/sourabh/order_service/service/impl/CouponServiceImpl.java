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

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    private final CouponUsageRepository couponUsageRepository;

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

    @Override
    @Transactional
    public String deactivateCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setIsActive(false);
        couponRepository.save(coupon);
        return "Coupon deactivated";
    }

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
