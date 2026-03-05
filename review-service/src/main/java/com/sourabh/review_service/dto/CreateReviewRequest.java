package com.sourabh.review_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating a new product review.
 *
 * <p>Submitted by a buyer via {@code POST /api/review}. Jakarta Bean Validation
 * constraints are enforced automatically when the controller parameter is
 * annotated with {@code @Valid}; violations result in a {@code 400 Bad Request}
 * response produced by
 * {@link com.sourabh.review_service.exception.GlobalExceptionHandler}.
 *
 * <h3>Example JSON</h3>
 * <pre>{@code
 * {
 *   "orderUuid"   : "ord-abc123",
 *   "productUuid" : "prod-xyz789",
 *   "rating"      : 5,
 *   "comment"     : "Excellent quality!"
 * }
 * }</pre>
 *
 * @see com.sourabh.review_service.controller.ReviewController#createReview
 */
@Getter
@Setter
public class CreateReviewRequest {

    /**
     * UUID of the order that contains the purchased product.
     * Used to verify that the buyer actually purchased the item
     * (order must have status {@code DELIVERED}).
     */
    @NotBlank(message = "Order UUID is required")
    private String orderUuid;

    /**
     * UUID of the product being reviewed.
     * Must match one of the items inside the referenced order.
     */
    @NotBlank(message = "Product UUID is required")
    private String productUuid;

    /**
     * Star rating between 1 (poor) and 5 (excellent), inclusive.
     * Must not be {@code null}.
     */
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer rating;

    /**
     * Optional free-text comment accompanying the review.
     * Maximum length is enforced at the entity level (2 000 characters).
     */
    private String comment;
}
