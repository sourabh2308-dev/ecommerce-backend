package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.response.ProductResponse;

import java.util.List;

public interface RecentlyViewedService {

    /** Record that a user viewed a product */
    void recordView(String userUuid, String productUuid);

    /** Get recently viewed product UUIDs for a user */
    List<String> getRecentlyViewed(String userUuid, int limit);
}
