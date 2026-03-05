package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for adding or updating a product image.
 *
 * <p>Multiple images can be attached to a single product; the
 * {@link #displayOrder} field controls their presentation sequence in the UI.
 */
@Data
public class ImageRequest {

    /** Fully-qualified URL of the image resource (required). */
    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    /** Optional sort position among the product's images. */
    private Integer displayOrder;

    /** Optional alternative text used for accessibility (e.g. screen readers). */
    private String altText;
}
