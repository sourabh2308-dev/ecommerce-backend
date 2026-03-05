package com.sourabh.review_service.repository;

import com.sourabh.review_service.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Review} entities.
 *
 * <p>All query methods automatically exclude soft-deleted reviews
 * ({@code isDeleted = false}) so that deleted reviews are hidden from
 * public-facing operations while being retained for auditing.
 *
 * <p>Spring Data JPA derives the SQL implementation at runtime from the
 * method-naming conventions below.
 *
 * @see Review
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Finds a non-deleted review by its public UUID.
     *
     * @param uuid the externally visible review identifier
     * @return an {@link Optional} containing the review, or empty if not found
     */
    Optional<Review> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * Checks whether a non-deleted review already exists for the given
     * product and buyer combination. Used to enforce the one-review-per-
     * buyer-per-product business rule.
     *
     * @param productUuid the product UUID
     * @param buyerUuid   the buyer UUID
     * @return {@code true} if a matching review exists
     */
    boolean existsByProductUuidAndBuyerUuidAndIsDeletedFalse(String productUuid, String buyerUuid);

    /**
     * Returns a paginated list of non-deleted reviews for a product.
     *
     * @param productUuid the product UUID
     * @param pageable    pagination and sorting parameters
     * @return a {@link Page} of matching reviews
     */
    Page<Review> findByProductUuidAndIsDeletedFalse(String productUuid, Pageable pageable);

    /**
     * Returns a paginated list of non-deleted reviews written by a buyer.
     *
     * @param buyerUuid the buyer UUID
     * @param pageable  pagination and sorting parameters
     * @return a {@link Page} of matching reviews
     */
    Page<Review> findByBuyerUuidAndIsDeletedFalse(String buyerUuid, Pageable pageable);

    /**
     * Returns all non-deleted reviews for a given seller.
     *
     * @param sellerUuid the seller UUID
     * @return list of matching reviews
     */
    List<Review> findBySellerUuidAndIsDeletedFalse(String sellerUuid);
}


