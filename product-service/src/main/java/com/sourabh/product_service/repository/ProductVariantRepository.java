package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProductVariant} entities.
 * <p>
 * Provides CRUD operations and query methods for retrieving active variants
 * by product, looking up variants by UUID, and checking for duplicate
 * variant combinations.
 * </p>
 */
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Returns all active variants belonging to a given product.
     *
     * @param productId database ID of the parent product
     * @return list of active variants
     */
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    /**
     * Finds a variant by its public UUID.
     *
     * @param uuid the universally unique identifier
     * @return an {@link Optional} containing the matching variant, or empty
     */
    Optional<ProductVariant> findByUuid(String uuid);

    /**
     * Checks whether a variant with the given name/value combination already
     * exists for the specified product. Used to enforce the composite
     * unique constraint at the application level.
     *
     * @param productId    database ID of the parent product
     * @param variantName  variant dimension name (e.g. "Size")
     * @param variantValue variant dimension value (e.g. "XL")
     * @return {@code true} if such a combination already exists
     */
    boolean existsByProductIdAndVariantNameAndVariantValue(Long productId, String variantName, String variantValue);
}
