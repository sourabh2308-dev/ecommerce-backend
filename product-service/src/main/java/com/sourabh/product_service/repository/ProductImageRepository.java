package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ProductImage} entities.
 * <p>
 * Provides CRUD operations and query methods for retrieving, deleting,
 * and counting images belonging to a specific product.
 * </p>
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * Returns all images for a product, ordered by display position ascending.
     *
     * @param productId database ID of the product
     * @return ordered list of product images
     */
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    /**
     * Deletes a specific image belonging to a product.
     *
     * @param productId database ID of the product
     * @param imageId   database ID of the image to remove
     */
    void deleteByProductIdAndId(Long productId, Long imageId);

    /**
     * Counts the total number of images associated with a product.
     *
     * @param productId database ID of the product
     * @return image count
     */
    int countByProductId(Long productId);
}
