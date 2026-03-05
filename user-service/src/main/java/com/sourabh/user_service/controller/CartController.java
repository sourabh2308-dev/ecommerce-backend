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

/**
 * REST controller for shopping-cart operations.
 * <p>
 * All endpoints are restricted to users with the {@code BUYER} role.
 * The authenticated user's UUID is read from the {@code X-User-UUID}
 * header injected by the API Gateway.
 * </p>
 *
 * <p>Base path: {@code /api/user/cart}</p>
 *
 * @see CartService
 */
@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class CartController {

    /** Service layer handling cart business logic. */
    private final CartService cartService;

    /**
     * Retrieves the full shopping cart for the authenticated buyer.
     *
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the user's {@link CartResponse} with all items and totals
     */
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", cartService.getCart(userUuid)));
    }

    /**
     * Adds a product to the cart, or increments its quantity if already present.
     *
     * @param body    validated cart-item payload containing product details
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the updated {@link CartResponse}
     */
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartItemRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartService.addToCart(userUuid, body)));
    }

    /**
     * Updates the quantity of a specific cart item.
     *
     * @param itemId   database ID of the cart item
     * @param quantity new desired quantity
     * @param request  the HTTP request carrying the {@code X-User-UUID} header
     * @return the updated {@link CartResponse}
     */
    @PreAuthorize("hasRole('BUYER')")
    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long itemId,
            @RequestParam int quantity,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Cart updated", cartService.updateCartItem(userUuid, itemId, quantity)));
    }

    /**
     * Removes a single item from the cart.
     *
     * @param itemId  database ID of the cart item to remove
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the updated {@link CartResponse}
     */
    @PreAuthorize("hasRole('BUYER')")
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long itemId,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartService.removeFromCart(userUuid, itemId)));
    }

    /**
     * Empties all items from the authenticated buyer's cart.
     *
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return confirmation message
     */
    @PreAuthorize("hasRole('BUYER')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearCart(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        cartService.clearCart(userUuid);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
