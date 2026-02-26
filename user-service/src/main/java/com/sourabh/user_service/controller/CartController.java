package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.ApiResponse;
import com.sourabh.user_service.dto.request.CartItemRequest;
import com.sourabh.user_service.dto.response.CartResponse;
import com.sourabh.user_service.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", cartService.getCart(userUuid)));
    }

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartItemRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartService.addToCart(userUuid, body)));
    }

    @PreAuthorize("hasRole('BUYER')")
    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long itemId,
            @RequestParam int quantity,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Cart updated", cartService.updateCartItem(userUuid, itemId, quantity)));
    }

    @PreAuthorize("hasRole('BUYER')")
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long itemId,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartService.removeFromCart(userUuid, itemId)));
    }

    @PreAuthorize("hasRole('BUYER')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearCart(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        cartService.clearCart(userUuid);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
