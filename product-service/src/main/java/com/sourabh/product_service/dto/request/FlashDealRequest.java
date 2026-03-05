package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO for scheduling or updating a flash deal on a product.
 *
 * <p>Flash deals apply a time-limited percentage discount to a single product.
 * The discount percentage must be between 1&nbsp;% and 99&nbsp;%, and the
 * {@link #startTime} must precede the {@link #endTime}.
 */
@Data
public class FlashDealRequest {

    /** UUID of the product the deal applies to. */
    @NotBlank(message = "Product UUID is required")
    private String productUuid;

    /** Discount percentage (1.0 – 99.0 inclusive). */
    @NotNull @DecimalMin("1.0") @DecimalMax("99.0")
    private Double discountPercent;

    /** Date-time when the flash deal becomes active. */
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    /** Date-time when the flash deal expires. */
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}
