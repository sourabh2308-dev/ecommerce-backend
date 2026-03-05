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

/**
 * REST controller exposing endpoints for managing product reviews and ratings.
 *
 * <p>All requests arrive via the API Gateway, which performs JWT validation
 * and forwards the authenticated user's identity through
 * {@code X-User-UUID} and {@code X-User-Role} headers. This controller
 * extracts those headers where needed and delegates to the
 * {@link ReviewService} for business logic.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST   /api/review}                    — create a review (BUYER only)</li>
 *   <li>{@code GET    /api/review/product/{productUuid}} — list reviews for a product (public)</li>
 *   <li>{@code GET    /api/review/{uuid}}              — get a single review (public)</li>
 *   <li>{@code PUT    /api/review/{uuid}}              — update own review (BUYER only)</li>
 *   <li>{@code DELETE /api/review/{uuid}}              — delete review (BUYER own / ADMIN any)</li>
 *   <li>{@code GET    /api/review/me}                  — list own reviews (BUYER only)</li>
 *   <li>{@code POST   /api/review/{uuid}/vote}         — vote helpful/not helpful (authenticated)</li>
 *   <li>{@code POST   /api/review/{uuid}/images}       — add image to own review (BUYER only)</li>
 * </ul>
 *
 * <h3>Security</h3>
 * Role-based access is enforced via {@code @PreAuthorize} annotations that
 * evaluate roles set by
 * {@link com.sourabh.review_service.config.HeaderRoleAuthenticationFilter}.
 *
 * @see ReviewService
 * @see ReviewServiceImpl
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    /** Abstraction for core review CRUD operations. */
    private final ReviewService reviewService;

    /** Concrete implementation, used for vote and image operations. */
    private final ReviewServiceImpl reviewServiceImpl;

    /**
     * Creates a new product review.
     *
     * <p>Only buyers are authorised. The buyer UUID is read from the
     * gateway-forwarded {@code X-User-UUID} header. Before persisting the
     * review the service verifies that the buyer has a delivered order
     * containing the specified product (via Feign call to order-service).
     *
     * @param request     the validated review payload (order UUID, product UUID, rating, comment)
     * @param httpRequest the servlet request carrying the {@code X-User-UUID} header
     * @return the created {@link ReviewResponse} wrapped in {@code 200 OK}
     */
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {

        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(reviewService.createReview(request, buyerUuid));
    }

    /**
     * Returns a paginated list of reviews for a given product.
     *
     * <p>This endpoint is public (no authentication required). Reviews are
     * sorted by creation date descending so the most recent reviews appear
     * first.
     *
     * @param productUuid the UUID of the product whose reviews are requested
     * @param page        zero-based page index (default {@code 0})
     * @param size        number of reviews per page (default {@code 10})
     * @return paginated {@link ReviewResponse} list wrapped in {@code 200 OK}
     */
    @GetMapping("/product/{productUuid}")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByProduct(
            @PathVariable String productUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(reviewService.getReviewsByProduct(productUuid, page, size));
    }

    /**
     * Retrieves a single review by its UUID.
     *
     * <p>This endpoint is public. Returns {@code 404} if the review does
     * not exist or has been soft-deleted.
     *
     * @param uuid the unique identifier of the review
     * @return the matching {@link ReviewResponse} wrapped in {@code 200 OK}
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<ReviewResponse> getReviewByUuid(@PathVariable String uuid) {
        return ResponseEntity.ok(reviewService.getReviewByUuid(uuid));
    }

    /**
     * Updates the comment text of an existing review.
     *
     * <p>Only the original buyer (review author) may update a review. The
     * buyer UUID is read from the {@code X-User-UUID} header forwarded by
     * the API Gateway.
     *
     * @param uuid        the UUID of the review to update
     * @param request     the validated update payload (new comment text)
     * @param httpRequest the servlet request carrying the {@code X-User-UUID} header
     * @return the updated {@link ReviewResponse} wrapped in {@code 200 OK}
     */
    @PreAuthorize("hasRole('BUYER')")
    @PutMapping("/{uuid}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String uuid,
            @Valid @RequestBody UpdateReviewRequest request,
            HttpServletRequest httpRequest) {

        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(reviewService.updateReview(uuid, request, buyerUuid));
    }

    /**
     * Soft-deletes a review.
     *
     * <p>Buyers may delete their own reviews; users with the {@code ADMIN}
     * role may delete any review for moderation purposes. The role and buyer
     * UUID are read from gateway-forwarded headers.
     *
     * @param uuid        the UUID of the review to delete
     * @param httpRequest the servlet request carrying {@code X-User-Role} and {@code X-User-UUID}
     * @return a confirmation message wrapped in {@code 200 OK}
     */
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<String> deleteReview(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {

        String role = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(reviewService.deleteReview(uuid, role, buyerUuid));
    }

    /**
     * Returns a paginated list of reviews authored by the authenticated buyer.
     *
     * <p>Restricted to the {@code BUYER} role. The buyer UUID is read from
     * the {@code X-User-UUID} header. Results are sorted newest first.
     *
     * @param page        zero-based page index (default {@code 0})
     * @param size        number of reviews per page (default {@code 10})
     * @param httpRequest the servlet request carrying the {@code X-User-UUID} header
     * @return paginated {@link ReviewResponse} list wrapped in {@code 200 OK}
     */
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(reviewService.getMyReviews(buyerUuid, page, size));
    }

    /**
     * Records a helpfulness vote (helpful / not helpful) on a review.
     *
     * <p>Any authenticated user (BUYER, SELLER, or ADMIN) may vote. If the
     * voter has already voted on the review the existing vote is updated.
     *
     * @param uuid        the UUID of the review being voted on
     * @param helpful     {@code true} for helpful, {@code false} for not helpful
     * @param httpRequest the servlet request carrying the {@code X-User-UUID} header
     * @return a confirmation message wrapped in {@code 200 OK}
     */
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

    /**
     * Adds an image URL to the buyer's own review.
     *
     * <p>Only the review's author (BUYER) may add images. A maximum of five
     * images per review is enforced by the service layer.
     *
     * @param uuid        the UUID of the review to attach the image to
     * @param imageUrl    the publicly accessible URL of the image
     * @param httpRequest the servlet request carrying the {@code X-User-UUID} header
     * @return a confirmation message wrapped in {@code 200 OK}
     */
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


