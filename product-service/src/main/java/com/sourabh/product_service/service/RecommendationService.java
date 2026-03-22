package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.response.ProductResponse;

import java.util.List;

public interface RecommendationService {

    List<ProductResponse> getSimilarProducts(String productUuid, int limit);
}
