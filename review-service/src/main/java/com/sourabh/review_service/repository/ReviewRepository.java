package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUuidAndIsDeletedFalse(String uuid);

    boolean existsByProductUuidAndBuyerUuidAndIsDeletedFalse(String productUuid, String buyerUuid);

    Page<Review> findByProductUuidAndIsDeletedFalse(String productUuid, Pageable pageable);

    Page<Review> findByBuyerUuidAndIsDeletedFalse(String buyerUuid, Pageable pageable);

    List<Review> findBySellerUuidAndIsDeletedFalse(String sellerUuid);
}

