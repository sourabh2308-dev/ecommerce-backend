package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request payload for scheduling a flash deal for a product.
 */
@Data
public class FlashDealRequest {

    @NotBlank(message = "Product UUID is required")
    private String productUuid;

    @NotNull @DecimalMin("1.0") @DecimalMax("99.0")
    private Double discountPercent;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}
