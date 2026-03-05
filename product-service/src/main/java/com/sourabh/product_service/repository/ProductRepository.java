package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Product} entities.
 * <p>
 * Extends {@link JpaRepository} for standard CRUD and pagination,
 * and {@link JpaSpecificationExecutor} for dynamic, criteria-based queries
 * (e.g. multi-field filtering in product listings). All public-facing queries
 * exclude soft-deleted products ({@code isDeleted = false}).
 * </p>
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    /**
     * Finds a non-deleted product by its public UUID.
     *
     * @param uuid the universally unique identifier
     * @return an {@link Optional} containing the matching product, or empty
     */
    Optional<Product> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * Returns a paginated list of non-deleted products owned by a specific seller.
     *
     * @param sellerUuid UUID of the seller
     * @param pageable   pagination and sorting parameters
     * @return page of the seller's products
     */
    Page<Product> findBySellerUuidAndIsDeletedFalse(
            String sellerUuid,
            Pageable pageable);

    /**
     * Returns all non-deleted products owned by a specific seller (unpaginated).
     *
     * @param sellerUuid UUID of the seller
     * @return list of the seller's products
     */
    List<Product> findBySellerUuidAndIsDeletedFalse(String sellerUuid);

    /**
     * Finds products in the same category as a reference product, excluding
     * that product itself, ordered by average rating descending.
     * Used for "similar products" recommendations.
     *
     * @param category    category name to match
     * @param excludeUuid UUID of the product to exclude from results
     * @param pageable    pagination parameters (limit the result set)
     * @return list of similar products
     */
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.isDeleted = false AND p.uuid <> :excludeUuid ORDER BY p.averageRating DESC")
    List<Product> findSimilarProducts(String category, String excludeUuid, Pageable pageable);

    /**
     * Finds non-deleted products whose UUIDs are in the provided list.
     * Useful for batch lookups (e.g. resolving recently-viewed UUIDs to products).
     *
     * @param uuids list of product UUIDs to look up
     * @return matching products
     */
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.uuid IN :uuids")
    List<Product> findByUuidIn(List<String> uuids);

    /**
     * Cursor-based (keyset) pagination query.
     * Returns non-deleted products with an ID less than the given cursor,
     * ordered by ID descending (newest first). A {@code null} cursor fetches
     * the first page.
     *
     * @param cursor database ID to start after ({@code null} for first page)
     * @param pageable page-size parameter
     * @return list of products for the requested page
     */
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND (:cursor IS NULL OR p.id < :cursor) ORDER BY p.id DESC")
    List<Product> findWithCursor(@Param("cursor") Long cursor, Pageable pageable);

    /**
     * Counts all non-deleted products in the database.
     *
     * @return total number of active (non-deleted) products
     */
    long countByIsDeletedFalse();
}
