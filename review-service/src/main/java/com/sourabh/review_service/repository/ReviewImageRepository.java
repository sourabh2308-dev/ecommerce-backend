package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ReviewImage} entities.
 *
 * <p>Provides CRUD operations plus custom finders used by the service
 * layer to manage images attached to a review.
 *
 * @see ReviewImage
 */
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /**
     * Retrieves all images for a review ordered by their display position.
     *
     * @param reviewId the primary key of the parent review
     * @return images sorted by {@code displayOrder} ascending
     */
    List<ReviewImage> findByReviewIdOrderByDisplayOrderAsc(Long reviewId);

    /**
     * Counts the number of images currently attached to a review.
     * Used to enforce the per-review image limit (maximum 5).
     *
     * @param reviewId the primary key of the parent review
     * @return the current image count
     */
    int countByReviewId(Long reviewId);

    /**
     * Deletes a specific image from a review by its composite key.
     *
     * @param reviewId the primary key of the parent review
     * @param id       the primary key of the image to remove
     */
    void deleteByReviewIdAndId(Long reviewId, Long imageId);
}
