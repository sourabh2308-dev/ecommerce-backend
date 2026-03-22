package com.sourabh.product_service.controller;

import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.service.RecentlyViewedService;
import com.sourabh.product_service.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    private final RecentlyViewedService recentlyViewedService;

    @GetMapping("/{productUuid}/similar")
    public ResponseEntity<List<ProductResponse>> getSimilar(
            @PathVariable String productUuid,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getSimilarProducts(productUuid, limit));
    }

    @GetMapping("/recently-viewed")
    public ResponseEntity<List<String>> getRecentlyViewed(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(recentlyViewedService.getRecentlyViewed(userUuid, limit));
    }

    @PostMapping("/{productUuid}/view")
    public ResponseEntity<Void> recordView(
            @PathVariable String productUuid,
            @RequestHeader("X-User-UUID") String userUuid) {
        recentlyViewedService.recordView(userUuid, productUuid);
        return ResponseEntity.ok().build();
    }
}
