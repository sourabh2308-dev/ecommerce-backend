package com.sourabh.order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    // ── Shipping address ────────────────────────────────
    @NotBlank(message = "Shipping name is required")
    private String shippingName;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "City is required")
    private String shippingCity;

    @NotBlank(message = "State is required")
    private String shippingState;

    @NotBlank(message = "Pincode is required")
    private String shippingPincode;

    @NotBlank(message = "Phone number is required")
    private String shippingPhone;
}
