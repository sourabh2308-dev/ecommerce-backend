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

@RestController
@RequestMapping("/api/user/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getWishlist(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched", wishlistService.getWishlist(userUuid)));
    }

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> addToWishlist(
            @Valid @RequestBody WishlistRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", wishlistService.addToWishlist(userUuid, body)));
    }

    @PreAuthorize("hasRole('BUYER')")
    @DeleteMapping("/{productUuid}")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> removeFromWishlist(
            @PathVariable String productUuid,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", wishlistService.removeFromWishlist(userUuid, productUuid)));
    }

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/check/{productUuid}")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(
            @PathVariable String productUuid,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Wishlist check", wishlistService.isInWishlist(userUuid, productUuid)));
    }
}
