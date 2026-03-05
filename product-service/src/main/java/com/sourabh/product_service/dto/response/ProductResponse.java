package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Response DTO exposing the full details of a product listing.
 *
 * <p>Returned by product retrieval and search endpoints.  Both a legacy
 * flat {@link #category} string and a structured {@link #categoryRef} are
 * included for backwards compatibility with older clients.
 *
 * <p>{@code @Jacksonized} enables Jackson deserialisation directly into
 * the Lombok builder, which is useful for Redis cache serialisation.
 */
@Getter
@Builder
@Jacksonized
public class ProductResponse {

    /** Unique public identifier of the product. */
    private String uuid;

    /** Display name of the product. */
    private String name;

    /** Detailed product description. */
    private String description;

    /** Unit price in the default currency. */
    private Double price;

    /** Current stock quantity available for purchase. */
    private Integer stock;

    /** Legacy flat category name (kept for backwards compatibility). */
    private String category;

    /** Structured category reference with full hierarchy information. */
    private CategoryResponse categoryRef;

    /** UUID of the seller who owns this product. */
    private String sellerUuid;

    /** Current lifecycle status (e.g.&nbsp;{@code ACTIVE}, {@code INACTIVE}). */
    private String status;

    /** Aggregate average rating computed from associated reviews. */
    private Double averageRating;

    /** Total number of reviews submitted for this product. */
    private Integer totalReviews;

    /** URL of the primary product image. */
    private String imageUrl;
}
