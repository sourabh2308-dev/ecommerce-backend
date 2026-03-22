package com.sourabh.user_service.service;

import com.sourabh.user_service.dto.request.WishlistRequest;
import com.sourabh.user_service.dto.response.WishlistItemResponse;

import java.util.List;

public interface WishlistService {

    List<WishlistItemResponse> getWishlist(String userUuid);

    List<WishlistItemResponse> addToWishlist(String userUuid, WishlistRequest request);

    List<WishlistItemResponse> removeFromWishlist(String userUuid, String productUuid);

    boolean isInWishlist(String userUuid, String productUuid);
}
