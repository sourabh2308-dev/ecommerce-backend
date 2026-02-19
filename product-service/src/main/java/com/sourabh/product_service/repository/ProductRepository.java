package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findByUuidAndIsDeletedFalse(String uuid);

    Page<Product> findBySellerUuidAndIsDeletedFalse(
            String sellerUuid,
            Pageable pageable);
}
