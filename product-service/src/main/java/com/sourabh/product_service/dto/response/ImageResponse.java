package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageResponse {

    private Long id;

    private String imageUrl;

    private Integer displayOrder;

    private String altText;
}
