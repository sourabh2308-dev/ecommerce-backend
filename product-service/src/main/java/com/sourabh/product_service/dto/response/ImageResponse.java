package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response payload for product image metadata.
 */
@Data
@Builder
public class ImageResponse {
    private Long id;
    private String imageUrl;
    private Integer displayOrder;
    private String altText;
}
