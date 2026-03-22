package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findByUuidAndIsDeletedFalse(String uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.uuid = :uuid AND p.isDeleted = false")
    Optional<Product> findByUuidAndIsDeletedFalseForUpdate(@Param("uuid") String uuid);

    Page<Product> findBySellerUuidAndIsDeletedFalse(
            String sellerUuid,
            Pageable pageable);

    List<Product> findBySellerUuidAndIsDeletedFalse(String sellerUuid);

    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.isDeleted = false AND p.uuid <> :excludeUuid ORDER BY p.averageRating DESC")
    List<Product> findSimilarProducts(String category, String excludeUuid, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.uuid IN :uuids")
    List<Product> findByUuidIn(List<String> uuids);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND (:cursor IS NULL OR p.id < :cursor) ORDER BY p.id DESC")
    List<Product> findWithCursor(@Param("cursor") Long cursor, Pageable pageable);

    long countByIsDeletedFalse();
}
