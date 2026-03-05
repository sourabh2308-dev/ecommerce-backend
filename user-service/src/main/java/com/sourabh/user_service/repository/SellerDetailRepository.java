package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.SellerDetail;
import com.sourabh.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SellerDetail} entities.
 * <p>
 * Provides standard CRUD operations plus look-ups by the owning
 * {@link User} for seller-verification workflows.
 * </p>
 *
 * @see SellerDetail
 */
public interface SellerDetailRepository extends JpaRepository<SellerDetail, Long> {

    /**
     * Retrieves the seller-detail record associated with the given user.
     *
     * @param user the seller {@link User}
     * @return the matching detail record, if any
     */
    Optional<SellerDetail> findByUser(User user);

    /**
     * Checks whether a seller-detail record already exists for the
     * given user, avoiding duplicate submissions.
     *
     * @param user the {@link User} to check
     * @return {@code true} if details have already been submitted
     */
    boolean existsByUser(User user);
}
