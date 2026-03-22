package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.dto.request.WishlistRequest;
import com.sourabh.user_service.dto.response.WishlistItemResponse;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.entity.WishlistItem;
import com.sourabh.user_service.exception.UserNotFoundException;
import com.sourabh.user_service.exception.UserStateException;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.repository.WishlistItemRepository;
import com.sourabh.user_service.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String userUuid) {
        User user = findUser(userUuid);
        return wishlistItemRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<WishlistItemResponse> addToWishlist(String userUuid, WishlistRequest request) {
        User user = findUser(userUuid);

        if (wishlistItemRepository.existsByUserAndProductUuid(user, request.getProductUuid())) {
            throw new UserStateException("Product already in wishlist");
        }

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .productUuid(request.getProductUuid())
                .productName(request.getProductName())
                .productImage(request.getProductImage())
                .price(request.getPrice())
                .build();
        wishlistItemRepository.save(item);

        log.info("Wishlist item added: userUuid={}, productUuid={}", userUuid, request.getProductUuid());
        return getWishlist(userUuid);
    }

    @Override
    @Transactional
    public List<WishlistItemResponse> removeFromWishlist(String userUuid, String productUuid) {
        User user = findUser(userUuid);
        wishlistItemRepository.deleteByUserAndProductUuid(user, productUuid);
        log.info("Wishlist item removed: userUuid={}, productUuid={}", userUuid, productUuid);
        return getWishlist(userUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(String userUuid, String productUuid) {
        User user = findUser(userUuid);
        return wishlistItemRepository.existsByUserAndProductUuid(user, productUuid);
    }

    private User findUser(String userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private WishlistItemResponse mapToResponse(WishlistItem item) {
        return WishlistItemResponse.builder()
                .id(item.getId())
                .productUuid(item.getProductUuid())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .price(item.getPrice())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
