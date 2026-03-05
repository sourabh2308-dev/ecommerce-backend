package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO containing metadata for a product image.
 *
 * <p>Each product can have multiple images; the {@link #displayOrder} field
 * determines their presentation sequence in the storefront gallery.
 */
@Data
@Builder
public class ImageResponse {

    /** Internal database identifier of the image record. */
    private Long id;

    /** Fully-qualified URL of the image resource. */
    private String imageUrl;

    /** Sort position among the product's images. */
    private Integer displayOrder;

    /** Alternative text used for accessibility (screen readers, tooltips). */
    private String altText;
}
