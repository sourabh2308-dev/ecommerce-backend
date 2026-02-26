package com.sourabh.review_service.controller;

import com.sourabh.review_service.common.PageResponse;
import com.sourabh.review_service.dto.CreateReviewRequest;
import com.sourabh.review_service.dto.ReviewResponse;
import com.sourabh.review_service.dto.UpdateReviewRequest;
import com.sourabh.review_service.service.ReviewService;
import com.sourabh.review_service.service.impl.ReviewServiceImpl;
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
/**
 * REST API CONTROLLER - Handles HTTP Requests
 * 
 * This controller exposes REST endpoints for HTTP clients (API Gateway, web browsers).
 * Each endpoint method:
 *   1. Validates request parameters and body with @Valid
 *   2. Extracts user context from headers (X-User-UUID, X-User-Role)
 *   3. Delegates business logic to Service layer
 *   4. Returns JSON response via ResponseEntity
 * 
 * Authorization:
 *   - @PreAuthorize: Spring Security checks user role before method execution
 *   - Headers injected by API Gateway after JWT validation
 */
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewServiceImpl reviewServiceImpl;

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

    /**


     * API ENDPOINT


     * 


     * HTTP Method: GET


     * 


     * PURPOSE:


     * Handles HTTP requests for this endpoint. Validates input, delegates to service


     * layer for business logic, and returns JSON response.


     * 


     * PROCESS FLOW:


     * 1. API Gateway forwards request after JWT validation


     * 2. Spring deserializes JSON to request object


     * 3. @Valid triggers bean validation (if present)


     * 4. @PreAuthorize checks user role (if present)


     * 5. Service layer executes business logic


     * 6. Response object serialized to JSON


     * 7. HTTP status code sent (200/201/400/403/404/500)


     * 


     * SECURITY:


     * - JWT validation at API Gateway (user authenticated)


     * - Role-based access via @PreAuthorize annotation


     * - Input validation via @Valid and constraint annotations


     * 


     * ERROR HANDLING:


     * - Service exceptions caught by GlobalExceptionHandler


     * - Returns standardized error response with HTTP status


     */


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
    /**
     * GETREVIEWBYUUID - Method Documentation
     *
     * PURPOSE:
     * This method handles the getReviewByUuid operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ReviewResponse> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @RequestParam - Applied to this method
     * @GetMapping - REST endpoint handler
     *
     */
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

    // ── Vote on a review (helpful / not helpful) ──

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @PostMapping("/{uuid}/vote")
    public ResponseEntity<String> voteReview(
            @PathVariable String uuid,
            @RequestParam boolean helpful,
            HttpServletRequest httpRequest) {
        String voterUuid = httpRequest.getHeader("X-User-UUID");
        reviewServiceImpl.voteReview(uuid, voterUuid, helpful);
        return ResponseEntity.ok("Vote recorded");
    }

    // ── Add image to own review ──

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/{uuid}/images")
    public ResponseEntity<String> addImage(
            @PathVariable String uuid,
            @RequestParam String imageUrl,
            HttpServletRequest httpRequest) {
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        reviewServiceImpl.addImageToReview(uuid, buyerUuid, imageUrl);
        return ResponseEntity.ok("Image added");
    }
}


