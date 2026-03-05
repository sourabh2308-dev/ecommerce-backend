package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link StockMovement} entities.
 * <p>
 * Provides CRUD operations and query methods for retrieving a product's
 * stock-movement history, supporting both full paginated access and a
 * quick "last 10 movements" lookup.
 * </p>
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Returns a paginated history of stock movements for a product,
     * ordered by creation timestamp descending (newest first).
     *
     * @param productUuid UUID of the product
     * @param pageable    pagination and sorting parameters
     * @return page of stock movement records
     */
    Page<StockMovement> findByProductUuidOrderByCreatedAtDesc(String productUuid, Pageable pageable);

    /**
     * Returns the 10 most recent stock movements for a product.
     *
     * @param productUuid UUID of the product
     * @return list of up to 10 recent stock movements
     */
    List<StockMovement> findTop10ByProductUuidOrderByCreatedAtDesc(String productUuid);
}
