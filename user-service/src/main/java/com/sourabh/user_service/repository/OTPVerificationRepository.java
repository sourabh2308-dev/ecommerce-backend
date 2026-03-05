package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.OTPType;
import com.sourabh.user_service.entity.OTPVerification;
import com.sourabh.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link OTPVerification} entities.
 * <p>
 * Provides standard CRUD operations and a finder for retrieving the
 * most recently created OTP of a given type for a specific user.
 * </p>
 *
 * @see OTPVerification
 */
public interface OTPVerificationRepository extends JpaRepository<OTPVerification, Long> {

    /**
     * Retrieves the most recent OTP issued to the given user for the
     * specified purpose, ordered by creation time descending.
     *
     * @param user the target {@link User}
     * @param type the {@link OTPType} to filter on
     * @return the latest matching OTP record, if any
     */
    Optional<OTPVerification> findTopByUserAndTypeOrderByCreatedAtDesc(User user, OTPType type);
}
