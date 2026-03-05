package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO representing a single line item within a
 * {@link CreateOrderRequest}.
 *
 * <p>Each item identifies the product to purchase and the desired
 * quantity. Fields are validated using Bean Validation annotations.</p>
 */
@Getter
@Setter
public class OrderItemRequest {

    /** UUID of the product to order (required, must not be blank). */
    @NotBlank(message = "Product UUID is required")
    private String productUuid;

    /** Number of units to order (required, minimum 1). */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
