package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.Role;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    Optional<User> findByUuid(String uuid);

    Optional<User> findByUuidAndIsDeletedFalse(String uuid);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByRoleAndStatus(Role role, UserStatus status, Pageable pageable);

    Page<User> findByIsDeletedFalse(Pageable pageable);

    Page<User> findByRoleAndIsDeletedFalse(Role role, Pageable pageable);

    Page<User> findByStatusAndIsDeletedFalse(UserStatus status, Pageable pageable);

    Page<User> findByRoleAndStatusAndIsDeletedFalse(
            Role role,
            UserStatus status,
            Pageable pageable
    );

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
