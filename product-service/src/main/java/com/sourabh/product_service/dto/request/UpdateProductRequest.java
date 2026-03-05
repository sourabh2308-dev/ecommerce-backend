package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for partially updating an existing product listing.
 *
 * <p>All fields are optional — only the non-{@code null} fields will be
 * applied to the product entity.  Validation constraints still apply to any
 * field that <em>is</em> provided, ensuring data integrity.
 */
@Getter
@Setter
public class UpdateProductRequest {

    /** Updated product display name (max 255 characters). */
    @Size(max = 255)
    private String name;

    /** Updated long-form description (max 2 000 characters). */
    @Size(max = 2000)
    private String description;

    /** Updated unit price; must be positive when provided. */
    @Positive
    private Double price;

    /** Updated stock quantity; zero or greater when provided. */
    @Min(0)
    private Integer stock;

    /** Updated category identifier (name or UUID). */
    private String category;

    /** Updated URL pointing to the primary product image. */
    private String imageUrl;
}

