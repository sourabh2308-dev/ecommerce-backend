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

@RestController
@RequestMapping("/api/order/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestHeader("X-User-UUID") String buyerUuid,
            @RequestParam String code,
            @RequestParam Double orderAmount) {
        return ResponseEntity.ok(couponService.validateCoupon(code, orderAmount, buyerUuid));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CouponResponse>> listAll() {
        return ResponseEntity.ok(couponService.listAll());
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deactivate(@PathVariable String code) {
        return ResponseEntity.ok(couponService.deactivateCoupon(code));
    }
}
