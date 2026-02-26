package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUuidAndIsDeletedFalse(String uuid);

    boolean existsByProductUuidAndBuyerUuidAndIsDeletedFalse(String productUuid, String buyerUuid);

    Page<Review> findByProductUuidAndIsDeletedFalse(String productUuid, Pageable pageable);

    Page<Review> findByBuyerUuidAndIsDeletedFalse(String buyerUuid, Pageable pageable);

    List<Review> findBySellerUuidAndIsDeletedFalse(String sellerUuid);
}


