package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating or updating a product category in the catalogue.
 *
 * <p>Categories form a tree hierarchy.  A category whose {@link #parentUuid}
 * is {@code null} is treated as a root category; otherwise it becomes a child
 * of the referenced parent.
 *
 * <p>Validation is triggered automatically when the controller parameter is
 * annotated with {@code @Valid}.
 */
@Getter
@Setter
public class CreateCategoryRequest {

    /** Display name of the category (required, max 100 chars). */
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be under 100 characters")
    private String name;

    /** Optional free-text description (max 500 chars). */
    @Size(max = 500, message = "Description must be under 500 characters")
    private String description;

    /** Optional URL pointing to the category image. */
    private String imageUrl;

    /** UUID of the parent category, or {@code null} for a root category. */
    private String parentUuid;

    /** Position used to sort sibling categories in the UI. */
    private Integer displayOrder;
}
