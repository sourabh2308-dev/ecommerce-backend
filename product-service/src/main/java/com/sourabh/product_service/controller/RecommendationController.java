package com.sourabh.product_service.controller;

import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.service.RecentlyViewedService;
import com.sourabh.product_service.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller providing product recommendation and browsing-history endpoints.
 * <p>
 * Supports "similar products" suggestions based on the same category and
 * maintains per-user recently-viewed product lists backed by Redis.
 * </p>
 *
 * <p>Base path: {@code /api/product}</p>
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class RecommendationController {

    /** Service for computing similar-product recommendations. */
    private final RecommendationService recommendationService;

    /** Service for tracking and retrieving recently viewed products via Redis. */
    private final RecentlyViewedService recentlyViewedService;

    /**
     * Returns a list of products similar to the specified one.
     * Similarity is determined by shared category and highest average rating.
     *
     * @param productUuid UUID of the reference product
     * @param limit       maximum number of results to return (default 10)
     * @return list of similar product responses
     */
    @GetMapping("/{productUuid}/similar")
    public ResponseEntity<List<ProductResponse>> getSimilar(
            @PathVariable String productUuid,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getSimilarProducts(productUuid, limit));
    }

    /**
     * Retrieves the UUIDs of products recently viewed by the authenticated user.
     *
     * @param userUuid UUID of the authenticated user
     * @param limit    maximum number of entries to return (default 20)
     * @return ordered list of recently viewed product UUIDs (most recent first)
     */
    @GetMapping("/recently-viewed")
    public ResponseEntity<List<String>> getRecentlyViewed(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(recentlyViewedService.getRecentlyViewed(userUuid, limit));
    }

    /**
     * Records a product view event for the authenticated user.
     * The view is stored in Redis to power the recently-viewed feature.
     *
     * @param productUuid UUID of the viewed product
     * @param userUuid    UUID of the authenticated user
     * @return HTTP 200 with empty body
     */
    @PostMapping("/{productUuid}/view")
    public ResponseEntity<Void> recordView(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String userUuid) {
        recentlyViewedService.recordView(userUuid, productUuid);
        return ResponseEntity.ok().build();
    }
}
