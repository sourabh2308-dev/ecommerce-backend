package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating a new product listing.
 *
 * <p>Submitted by sellers via the product creation endpoint.  All mandatory
 * fields are enforced with Bean Validation annotations; constraint violations
 * result in a {@code 400 Bad Request} response containing field-level error
 * details.
 */
@Getter
@Setter
public class CreateProductRequest {

    /** Product display name (required, max 255 characters). */
    @NotBlank
    @Size(max = 255)
    private String name;

    /** Optional long-form product description (max 2 000 characters). */
    @Size(max = 2000)
    private String description;

    /** Unit price in the default currency; must be a positive number. */
    @NotNull
    @Positive
    private Double price;

    /** Initial stock quantity; must be zero or greater. */
    @NotNull
    @Min(0)
    private Integer stock;

    /** Category identifier (name or UUID) the product belongs to. */
    @NotBlank
    private String category;

    /** Optional URL pointing to the primary product image. */
    private String imageUrl;
}
