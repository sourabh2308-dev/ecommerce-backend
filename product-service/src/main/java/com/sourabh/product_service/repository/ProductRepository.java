package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
