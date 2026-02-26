package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.dto.request.CartItemRequest;
import com.sourabh.user_service.dto.response.CartItemResponse;
import com.sourabh.user_service.dto.response.CartResponse;
import com.sourabh.user_service.entity.CartItem;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.exception.UserNotFoundException;
import com.sourabh.user_service.exception.UserStateException;
import com.sourabh.user_service.repository.CartItemRepository;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String userUuid) {
        User user = findUser(userUuid);
        List<CartItem> items = cartItemRepository.findByUserOrderByCreatedAtDesc(user);
        return buildCartResponse(items);
    }

    @Override
    @Transactional
    public CartResponse addToCart(String userUuid, CartItemRequest request) {
        User user = findUser(userUuid);

        Optional<CartItem> existing = cartItemRepository.findByUserAndProductUuid(user, request.getProductUuid());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setPrice(request.getPrice());
            if (request.getProductName() != null) item.setProductName(request.getProductName());
            if (request.getProductImage() != null) item.setProductImage(request.getProductImage());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .productUuid(request.getProductUuid())
                    .productName(request.getProductName())
                    .productImage(request.getProductImage())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        log.info("Cart updated: userUuid={}, productUuid={}", userUuid, request.getProductUuid());
        return getCart(userUuid);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(String userUuid, Long itemId, int quantity) {
        User user = findUser(userUuid);
        CartItem item = cartItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new UserStateException("Cart item not found"));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCart(userUuid);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(String userUuid, Long itemId) {
        User user = findUser(userUuid);
        CartItem item = cartItemRepository.findByIdAndUser(itemId, user)
                .orElseThrow(() -> new UserStateException("Cart item not found"));
        cartItemRepository.delete(item);
        return getCart(userUuid);
    }

    @Override
    @Transactional
    public void clearCart(String userUuid) {
        User user = findUser(userUuid);
        cartItemRepository.deleteAllByUser(user);
        log.info("Cart cleared: userUuid={}", userUuid);
    }

    private User findUser(String userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private CartResponse buildCartResponse(List<CartItem> items) {
        List<CartItemResponse> responses = items.stream()
                .map(item -> CartItemResponse.builder()
                        .id(item.getId())
                        .productUuid(item.getProductUuid())
                        .productName(item.getProductName())
                        .productImage(item.getProductImage())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getPrice() * item.getQuantity())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();

        double total = responses.stream().mapToDouble(CartItemResponse::getSubtotal).sum();

        return CartResponse.builder()
                .items(responses)
                .totalItems(responses.size())
                .totalAmount(total)
                .build();
    }
}
