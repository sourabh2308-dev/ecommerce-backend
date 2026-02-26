package com.sourabh.user_service.service;

import com.sourabh.user_service.dto.request.CartItemRequest;
import com.sourabh.user_service.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(String userUuid);

    CartResponse addToCart(String userUuid, CartItemRequest request);

    CartResponse updateCartItem(String userUuid, Long itemId, int quantity);

    CartResponse removeFromCart(String userUuid, Long itemId);

    void clearCart(String userUuid);
}
