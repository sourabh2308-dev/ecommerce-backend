package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    Optional<ProductVariant> findByUuid(String uuid);

    boolean existsByProductIdAndVariantNameAndVariantValue(Long productId, String variantName, String variantValue);
}
