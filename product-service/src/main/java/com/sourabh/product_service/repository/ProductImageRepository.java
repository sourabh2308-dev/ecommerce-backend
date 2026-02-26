package com.sourabh.product_service.repository;

import com.sourabh.product_service.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    void deleteByProductIdAndId(Long productId, Long imageId);

    int countByProductId(Long productId);
}
