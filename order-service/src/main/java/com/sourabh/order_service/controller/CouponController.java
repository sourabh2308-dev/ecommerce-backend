package com.sourabh.order_service.controller;

import com.sourabh.order_service.dto.request.ApplyCouponRequest;
import com.sourabh.order_service.dto.request.CreateCouponRequest;
import com.sourabh.order_service.dto.response.CouponResponse;
import com.sourabh.order_service.dto.response.CouponValidationResponse;
import com.sourabh.order_service.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing promotional discount coupons.
 *
 * <p>Supports the full coupon lifecycle: creation (admin/seller), validation
 * at checkout (buyer), retrieval of all coupons (admin), and deactivation
 * (admin). Coupons may be {@code PERCENTAGE}-based or {@code FLAT}-amount
 * and can be constrained by usage limits, minimum order amounts, and
 * validity periods.</p>
 *
 * <p>Base path: {@code /api/order/coupons}</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see CouponService
 * @see com.sourabh.order_service.entity.Coupon
 */
@RestController
@RequestMapping("/api/order/coupons")
@RequiredArgsConstructor
public class CouponController {

    /** Service encapsulating coupon business logic. */
    private final CouponService couponService;

    /**
     * Creates a new coupon.
     *
     * <p>Admins may create global coupons; sellers may create coupons scoped
     * to their own products.</p>
     *
     * @param request validated coupon creation payload containing code,
     *                discount details, limits, and validity window
     * @return {@link ResponseEntity} containing the persisted {@link CouponResponse}
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    /**
     * Validates a coupon code against a given order amount for the
     * authenticated buyer.
     *
     * <p>The validation checks whether the coupon exists and is active, falls
     * within its validity window, meets the minimum order amount requirement,
     * and has not exceeded its global or per-user usage limits.</p>
     *
     * @param buyerUuid   UUID of the buyer, injected from the
     *                    {@code X-User-UUID} request header
     * @param code        the coupon code to validate
     * @param orderAmount the current order subtotal before discount
     * @return {@link ResponseEntity} containing validation result and the
     *         calculated discount
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestHeader("X-User-UUID") String buyerUuid,
            @RequestParam String code,
            @RequestParam Double orderAmount) {
        return ResponseEntity.ok(couponService.validateCoupon(code, orderAmount, buyerUuid));
    }

    /**
     * Lists every coupon in the system, including inactive ones.
     *
     * @return {@link ResponseEntity} containing a list of all
     *         {@link CouponResponse} objects
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponse>> listAll() {
        return ResponseEntity.ok(couponService.listAll());
    }

    /**
     * Deactivates a coupon by its code.
     *
     * <p>Sets the coupon's {@code isActive} flag to {@code false}, preventing
     * any further redemptions. The coupon record is retained for historical
     * and audit purposes.</p>
     *
     * @param code the coupon code to deactivate
     * @return {@link ResponseEntity} containing a confirmation message
     */
    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deactivate(@PathVariable String code) {
        return ResponseEntity.ok(couponService.deactivateCoupon(code));
    }
}
