package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.response.ProductResponse;

import java.util.List;

public interface RecentlyViewedService {

    void recordView(String userUuid, String productUuid);

    List<String> getRecentlyViewed(String userUuid, int limit);
}
