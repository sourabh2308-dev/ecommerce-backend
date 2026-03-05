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

/**
 * Implementation of {@link WishlistService} for user wishlist management.
 *
 * <p>Wishlist items are stored in the {@code wishlist_items} table with a
 * composite uniqueness constraint on {@code (user_id, product_uuid)} to
 * prevent duplicate entries. Each add/remove operation returns the full
 * updated wishlist for immediate UI rendering.</p>
 *
 * @see WishlistService
 * @see WishlistItemRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    /** Repository for {@link WishlistItem} persistence operations. */
    private final WishlistItemRepository wishlistItemRepository;

    /** Repository for {@link User} lookups. */
    private final UserRepository userRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String userUuid) {
        User user = findUser(userUuid);
        return wishlistItemRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<WishlistItemResponse> removeFromWishlist(String userUuid, String productUuid) {
        User user = findUser(userUuid);
        wishlistItemRepository.deleteByUserAndProductUuid(user, productUuid);
        log.info("Wishlist item removed: userUuid={}, productUuid={}", userUuid, productUuid);
        return getWishlist(userUuid);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(String userUuid, String productUuid) {
        User user = findUser(userUuid);
        return wishlistItemRepository.existsByUserAndProductUuid(user, productUuid);
    }

    /**
     * Looks up a {@link User} by UUID or throws {@link UserNotFoundException}.
     *
     * @param userUuid the UUID to search for
     * @return the matching {@link User} entity
     */
    private User findUser(String userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * Maps a {@link WishlistItem} entity to a {@link WishlistItemResponse} DTO.
     *
     * @param item the wishlist item entity
     * @return the corresponding response DTO
     */
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
