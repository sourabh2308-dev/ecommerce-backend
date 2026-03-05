package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.Role;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 * <p>
 * Provides standard CRUD operations, pagination, soft-delete-aware
 * queries, and a custom keyword search across user fields.
 * </p>
 *
 * @see User
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email address (case-insensitive).
     *
     * @param email the email to search for
     * @return the matching user, if present
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Finds a user by their public UUID.
     *
     * @param uuid the public UUID
     * @return the matching user, if present
     */
    Optional<User> findByUuid(String uuid);

    /**
     * Finds a non-deleted user by UUID.
     *
     * @param uuid the public UUID
     * @return the matching active user, if present
     */
    Optional<User> findByUuidAndIsDeletedFalse(String uuid);

    /**
     * Checks whether an email address is already registered (case-insensitive).
     *
     * @param email the email to check
     * @return {@code true} if a user with that email exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Returns users with a specific role and status, with pagination.
     *
     * @param role     the {@link Role} filter
     * @param status   the {@link UserStatus} filter
     * @param pageable pagination parameters
     * @return a page of matching users
     */
    Page<User> findByRoleAndStatus(Role role, UserStatus status, Pageable pageable);

    /**
     * Returns all non-deleted users with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of active users
     */
    Page<User> findByIsDeletedFalse(Pageable pageable);

    /**
     * Returns non-deleted users filtered by role, with pagination.
     *
     * @param role     the {@link Role} filter
     * @param pageable pagination parameters
     * @return a page of matching users
     */
    Page<User> findByRoleAndIsDeletedFalse(Role role, Pageable pageable);

    /**
     * Returns non-deleted users filtered by status, with pagination.
     *
     * @param status   the {@link UserStatus} filter
     * @param pageable pagination parameters
     * @return a page of matching users
     */
    Page<User> findByStatusAndIsDeletedFalse(UserStatus status, Pageable pageable);

    /**
     * Returns non-deleted users filtered by both role and status, with pagination.
     *
     * @param role     the {@link Role} filter
     * @param status   the {@link UserStatus} filter
     * @param pageable pagination parameters
     * @return a page of matching users
     */
    Page<User> findByRoleAndStatusAndIsDeletedFalse(
            Role role,
            UserStatus status,
            Pageable pageable
    );

    /**
     * Performs a keyword search across user fields (email, first name,
     * last name, phone number) for non-deleted users.
     * <p>
     * Uses a custom JPQL query with case-insensitive {@code LIKE} matching
     * on multiple columns, connected by {@code OR}.
     * </p>
     *
     * @param keyword  the search term (partial match)
     * @param pageable pagination parameters
     * @return a page of users matching the keyword
     */
    @Query("""
       SELECT u FROM User u
       WHERE u.isDeleted = false AND
       (
           LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           u.phoneNumber LIKE CONCAT('%', :keyword, '%')
       )
       """)
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}
