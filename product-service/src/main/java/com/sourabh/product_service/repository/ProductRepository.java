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

// Data Repository - Provides database access via Spring Data JPA
/**
 * DATA ACCESS OBJECT - Database Query Interface
 * 
 * Extends JpaRepository to provide:
 *   - CRUD operations (Create, Read, Update, Delete)
 *   - Pagination and sorting (@Query custom methods)
 *   - Soft-delete support (isDeleted flag)
 * 
 * Spring Data JPA dynamically generates SQL from method names.
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findByUuidAndIsDeletedFalse(String uuid);

    Page<Product> findBySellerUuidAndIsDeletedFalse(
            String sellerUuid,
            Pageable pageable);

    List<Product> findBySellerUuidAndIsDeletedFalse(String sellerUuid);

    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.isDeleted = false AND p.uuid <> :excludeUuid ORDER BY p.averageRating DESC")
    List<Product> findSimilarProducts(String category, String excludeUuid, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.uuid IN :uuids")
    List<Product> findByUuidIn(List<String> uuids);

    /** Cursor-based pagination: fetch products with id > cursor, newest first */
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND (:cursor IS NULL OR p.id < :cursor) ORDER BY p.id DESC")
    List<Product> findWithCursor(@Param("cursor") Long cursor, Pageable pageable);

    long countByIsDeletedFalse();
}
