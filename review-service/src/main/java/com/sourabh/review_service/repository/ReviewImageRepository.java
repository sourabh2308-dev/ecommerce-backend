package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findByReviewIdOrderByDisplayOrderAsc(Long reviewId);

    int countByReviewId(Long reviewId);

    void deleteByReviewIdAndId(Long reviewId, Long imageId);
}
