package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a product category and its position in the
 * catalogue hierarchy.
 *
 * <p>Categories may contain nested {@link #children} entries, allowing the
 * client to render a full category tree in a single response.
 */
@Getter
@Builder
public class CategoryResponse {

    /** Unique public identifier of the category. */
    private String uuid;

    /** Display name of the category. */
    private String name;

    /** Optional free-text description of the category. */
    private String description;

    /** URL of the category image. */
    private String imageUrl;

    /** UUID of the parent category, or {@code null} for root categories. */
    private String parentUuid;

    /** Position used to sort sibling categories in the UI. */
    private Integer displayOrder;

    /** Whether this category is currently visible in the storefront. */
    private boolean isActive;

    /** Direct child categories (may be empty, never {@code null}). */
    private List<CategoryResponse> children;

    /** Timestamp when the category was created. */
    private LocalDateTime createdAt;
}
