package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.response.ProductResponse;

import java.util.List;

/**
 * Service interface for tracking recently viewed products per user.
 *
 * <p>Backed by a Redis sorted set keyed per user, with the current
 * timestamp as the score. The set is auto-trimmed to a configurable
 * maximum so that only the most recent views are retained.</p>
 *
 * @see com.sourabh.product_service.service.impl.RecentlyViewedServiceImpl
 */
public interface RecentlyViewedService {

    /**
     * Records that a user viewed a product.
     *
     * <p>If the product was already in the user's history, its score
     * (timestamp) is updated to the current time.</p>
     *
     * @param userUuid    the UUID of the viewing user
     * @param productUuid the UUID of the viewed product
     */
    void recordView(String userUuid, String productUuid);

    /**
     * Returns the most recently viewed product UUIDs for a user.
     *
     * @param userUuid the UUID of the user
     * @param limit    maximum number of UUIDs to return
     * @return list of product UUIDs ordered most-recent-first
     */
    List<String> getRecentlyViewed(String userUuid, int limit);
}
