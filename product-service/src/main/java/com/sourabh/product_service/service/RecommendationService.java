package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.response.ProductResponse;

import java.util.List;

/**
 * Service interface for product recommendation logic.
 *
 * <p>Provides content-based recommendations by finding products in the
 * same category as a given product, ranked by average rating. This is
 * a simple heuristic; more advanced collaborative-filtering or ML-based
 * strategies can be plugged in behind this interface.</p>
 *
 * @see com.sourabh.product_service.service.impl.RecommendationServiceImpl
 */
public interface RecommendationService {

    /**
     * Returns products similar to the given one.
     *
     * <p>Similarity is determined by matching category; results are
     * ordered by highest average rating and capped at {@code limit}.</p>
     *
     * @param productUuid the UUID of the reference product
     * @param limit       maximum number of similar products to return
     * @return list of similar {@link ProductResponse} objects
     * @throws com.sourabh.product_service.exception.ProductNotFoundException
     *         if the reference product does not exist
     */
    List<ProductResponse> getSimilarProducts(String productUuid, int limit);
}
