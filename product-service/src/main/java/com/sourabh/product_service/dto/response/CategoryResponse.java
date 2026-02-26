package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload describing a category and its hierarchy.
 */
@Getter
@Builder
public class CategoryResponse {
    private String uuid;
    private String name;
    private String description;
    private String imageUrl;
    private String parentUuid;
    private Integer displayOrder;
    private boolean isActive;
    private List<CategoryResponse> children;
    private LocalDateTime createdAt;
}
