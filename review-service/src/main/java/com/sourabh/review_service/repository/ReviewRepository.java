package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository
        extends JpaRepository<Review, Long> {

    boolean existsByProductUuidAndBuyerUuid(
            String productUuid,
            String buyerUuid);

    List<Review> findByProductUuid(String productUuid);

    List<Review> findBySellerUuid(String sellerUuid);
}

