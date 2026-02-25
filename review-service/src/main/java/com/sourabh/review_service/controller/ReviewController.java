package com.sourabh.review_service.controller;

import com.sourabh.review_service.common.PageResponse;
import com.sourabh.review_service.dto.CreateReviewRequest;
import com.sourabh.review_service.dto.ReviewResponse;
import com.sourabh.review_service.dto.UpdateReviewRequest;
import com.sourabh.review_service.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// REST API Controller - Handles HTTP requests and responses
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    // ───────────────────────────────────────────────────────────
    // CREATE REVIEW  —  BUYER only
    // ───────────────────────────────────────────────────────────

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {

        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(reviewService.createReview(request, buyerUuid));
    }

    // ───────────────────────────────────────────────────────────
    // GET REVIEWS BY PRODUCT  —  public
    // ───────────────────────────────────────────────────────────

    @GetMapping("/product/{productUuid}")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByProduct(
            @PathVariable String productUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(reviewService.getReviewsByProduct(productUuid, page, size));
    }

    // ───────────────────────────────────────────────────────────
    // GET SINGLE REVIEW  —  public
    // ───────────────────────────────────────────────────────────

    @GetMapping("/{uuid}")
    public ResponseEntity<ReviewResponse> getReviewByUuid(@PathVariable String uuid) {
        return ResponseEntity.ok(reviewService.getReviewByUuid(uuid));
    }

    // ───────────────────────────────────────────────────────────
    // UPDATE REVIEW  —  BUYER (own review only)
    // ───────────────────────────────────────────────────────────

    @PreAuthorize("hasRole('BUYER')")
    @PutMapping("/{uuid}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String uuid,
            @Valid @RequestBody UpdateReviewRequest request,
            HttpServletRequest httpRequest) {

        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(reviewService.updateReview(uuid, request, buyerUuid));
    }

    // ───────────────────────────────────────────────────────────
    // DELETE REVIEW  —  BUYER (own) or ADMIN
    // ───────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<String> deleteReview(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {

        String role = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(reviewService.deleteReview(uuid, role, buyerUuid));
    }

    // ───────────────────────────────────────────────────────────
    // MY REVIEWS  —  BUYER only, paginated
    // ───────────────────────────────────────────────────────────

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(reviewService.getMyReviews(buyerUuid, page, size));
    }
}


