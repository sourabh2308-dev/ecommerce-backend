package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WishlistRequest {

    @NotBlank(message = "Product UUID is required")
    private String productUuid;

    private String productName;

    private String productImage;

    @Positive(message = "Price must be positive")
    private double price;
}
