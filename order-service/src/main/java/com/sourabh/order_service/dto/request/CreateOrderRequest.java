package com.sourabh.order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO submitted by a buyer to create a new order.
 *
 * <p>Contains a list of {@link OrderItemRequest} items together with the
 * buyer's shipping address details. All fields are validated via Bean
 * Validation annotations; constraint violations result in a
 * {@code 400 Bad Request} response with detailed error messages.</p>
 */
@Getter
@Setter
public class CreateOrderRequest {

    /** List of order items; must contain at least one valid entry. */
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    /** Full name of the shipping recipient (required). */
    @NotBlank(message = "Shipping name is required")
    private String shippingName;

    /** Street address for delivery (required). */
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    /** City for the shipping destination (required). */
    @NotBlank(message = "City is required")
    private String shippingCity;

    /** State or province for shipping (required). */
    @NotBlank(message = "State is required")
    private String shippingState;

    /** Postal / PIN code for shipping (required). */
    @NotBlank(message = "Pincode is required")
    private String shippingPincode;

    /** Contact phone number for the shipping recipient (required). */
    @NotBlank(message = "Phone number is required")
    private String shippingPhone;
}
