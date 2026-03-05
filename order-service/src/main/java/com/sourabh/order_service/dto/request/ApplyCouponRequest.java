package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO submitted by clients to apply or validate a discount coupon
 * code against an order.
 *
 * <p>The {@code couponCode} field is validated as non-blank; a
 * {@link org.springframework.web.bind.MethodArgumentNotValidException} is
 * raised if the constraint is violated.</p>
 */
@Data
public class ApplyCouponRequest {

    /** The alphanumeric coupon code to apply (must not be blank). */
    @NotBlank
    private String couponCode;
}
