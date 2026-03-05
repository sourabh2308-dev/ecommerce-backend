package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.ApiResponse;
import com.sourabh.user_service.dto.request.WishlistRequest;
import com.sourabh.user_service.dto.response.WishlistItemResponse;
import com.sourabh.user_service.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for product-wishlist operations.
 * <p>
 * All endpoints are restricted to users with the {@code BUYER} role.
 * The authenticated user's UUID is read from the {@code X-User-UUID}
 * header injected by the API Gateway.
 * </p>
 *
 * <p>Base path: {@code /api/user/wishlist}</p>
 *
 * @see WishlistService
 */
@RestController
@RequestMapping("/api/user/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    /** Service layer handling wishlist business logic. */
    private final WishlistService wishlistService;

    /**
     * Retrieves all wishlist items for the authenticated buyer.
     *
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return list of {@link WishlistItemResponse}
     */
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getWishlist(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched", wishlistService.getWishlist(userUuid)));
    }

    /**
     * Adds a product to the authenticated buyer's wishlist.
     *
     * @param body    validated wishlist request containing product details
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the updated wishlist
     */
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> addToWishlist(
            @Valid @RequestBody WishlistRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", wishlistService.addToWishlist(userUuid, body)));
    }

    /**
     * Removes a product from the authenticated buyer's wishlist.
     *
     * @param productUuid UUID of the product to remove
     * @param request     the HTTP request carrying the {@code X-User-UUID} header
     * @return the updated wishlist
     */
    @PreAuthorize("hasRole('BUYER')")
    @DeleteMapping("/{productUuid}")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> removeFromWishlist(
            @PathVariable String productUuid,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", wishlistService.removeFromWishlist(userUuid, productUuid)));
    }

    /**
     * Checks whether a given product is in the authenticated buyer's wishlist.
     *
     * @param productUuid UUID of the product to check
     * @param request     the HTTP request carrying the {@code X-User-UUID} header
     * @return {@code true} if the product is wishlisted, {@code false} otherwise
     */
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/check/{productUuid}")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(
            @PathVariable String productUuid,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Wishlist check", wishlistService.isInWishlist(userUuid, productUuid)));
    }
}
