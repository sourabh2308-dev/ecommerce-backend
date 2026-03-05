package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.exception.ProductNotFoundException;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Content-based implementation of {@link RecommendationService}.
 *
 * <p>Recommends products similar to a reference product by matching on
 * category and ranking by average rating. The reference product itself
 * is excluded from the results. This provides a simple but effective
 * "customers also viewed" style recommendation.</p>
 *
 * @see RecommendationService
 * @see ProductRepository#findSimilarProducts
 */
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    /** Product repository used for similarity queries. */
    private final ProductRepository productRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Looks up the reference product to extract its category, then
     * delegates to {@link ProductRepository#findSimilarProducts} which
     * returns same-category products ordered by highest average rating.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getSimilarProducts(String productUuid, int limit) {
        Product product = productRepository.findByUuidAndIsDeletedFalse(productUuid)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        return productRepository.findSimilarProducts(
                        product.getCategory(), productUuid, PageRequest.of(0, limit))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Maps a {@link Product} entity to a {@link ProductResponse} DTO.
     *
     * @param p the product entity
     * @return the response DTO
     */
    private ProductResponse mapToResponse(Product p) {
        return ProductResponse.builder()
                .uuid(p.getUuid())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .category(p.getCategory())
                .sellerUuid(p.getSellerUuid())
                .status(p.getStatus().name())
                .averageRating(p.getAverageRating())
                .totalReviews(p.getTotalReviews())
                .imageUrl(p.getImageUrl())
                .build();
    }
}
