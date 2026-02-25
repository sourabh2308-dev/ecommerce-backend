package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * UPDATE PRODUCT REQUEST DTO
 * 
 * Data Transfer Object for product update requests.
 * Sellers use this to modify existing product details.
 * 
 * VALIDATION:
 * - All fields are optional (partial updates supported)
 * - @Size, @Positive, @Min constraints ensure data integrity
 * - Validated by @Valid annotation in controller
 */
@Getter
@Setter
public class UpdateProductRequest {

    // Product name: max 255 characters
    // @Size validates maximum length
    @Size(max = 255)
    private String name;

    // Product description: max 2000 characters
    // @Size validates maximum length (prevents database overflow)
    @Size(max = 2000)
    private String description;

    // Product price: must be positive number
    // @Positive validates > 0 (prevents negative or zero prices)
    @Positive
    private Double price;

    // Stock quantity: must be non-negative
    // @Min(0) allows 0 stock (out of stock)
    @Min(0)
    private Integer stock;

    // Product category (e.g., "Electronics", "Clothing")
    // No validation - can be null or any string
    private String category;

    // Product image URL (external CDN or cloud storage link)
    // No validation - can be null or any string
    private String imageUrl;
}

