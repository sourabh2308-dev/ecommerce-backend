package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Data Repository - Provides database access via Spring Data JPA
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUuid(String uuid);

    boolean existsByProductUuidAndBuyerUuid(String productUuid, String buyerUuid);

    Page<Review> findByProductUuid(String productUuid, Pageable pageable);

    Page<Review> findByBuyerUuid(String buyerUuid, Pageable pageable);

    List<Review> findBySellerUuid(String sellerUuid);
}


