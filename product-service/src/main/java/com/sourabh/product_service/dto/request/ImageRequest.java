package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ImageRequest {

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    private Integer displayOrder;

    private String altText;
}
