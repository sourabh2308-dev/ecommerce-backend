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
 * REST Controller for managing discount coupons.
 * 
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Creating coupons (ADMIN/SELLER)</li>
 *   <li>Validating coupons before order placement (BUYER)</li>
 *   <li>Listing all coupons (ADMIN)</li>
 *   <li>Deactivating coupons (ADMIN)</li>
 * </ul>
 * 
 * <p>Coupons can be of type PERCENTAGE or FLAT and may have usage limits,
 * minimum order amounts, and validity periods.
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@RestController
@RequestMapping("/api/order/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * Creates a new coupon.
     * 
     * <p>Admins can create global coupons; sellers can create seller-specific coupons.
     * 
     * @param request the coupon creation request containing code, discount details, and validity
     * @return ResponseEntity containing the created coupon details
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    /**
     * Validates a coupon code for a specific order amount.
     * 
     * <p>Checks if the coupon:
     * <ul>
     *   <li>Exists and is active</li>
     *   <li>Is within validity period</li>
     *   <li>Meets minimum order amount requirement</li>
     *   <li>Has not exceeded usage limits (global and per-user)</li>
     * </ul>
     * 
     * @param buyerUuid the UUID of the buyer applying the coupon
     * @param code the coupon code to validate
     * @param orderAmount the total order amount before discount
     * @return ResponseEntity with validation result and calculated discount
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
     * Retrieves all coupons in the system.
     * 
     * <p>Admin-only endpoint for viewing all coupons including inactive ones.
     * 
     * @return ResponseEntity containing list of all coupons
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponse>> listAll() {
        return ResponseEntity.ok(couponService.listAll());
    }

    /**
     * Deactivates a coupon by its code.
     * 
     * <p>Sets the coupon's isActive flag to false, preventing further usage.
     * Does not delete the coupon to maintain historical records.
     * 
     * @param code the coupon code to deactivate
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deactivate(@PathVariable String code) {
        return ResponseEntity.ok(couponService.deactivateCoupon(code));
    }
}
