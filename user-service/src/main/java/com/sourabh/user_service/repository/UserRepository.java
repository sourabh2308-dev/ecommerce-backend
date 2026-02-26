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

// Data Repository - Provides database access via Spring Data JPA
/**
 * DATA ACCESS OBJECT - Database Query Interface
 * 
 * Extends JpaRepository to provide:
 *   - CRUD operations (Create, Read, Update, Delete)
 *   - Pagination and sorting (@Query custom methods)
 *   - Soft-delete support (isDeleted flag)
 * 
 * Spring Data JPA dynamically generates SQL from method names.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

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

    /**


     * CUSTOM DATABASE QUERY


     * 


     * This method executes a custom JPQL or native SQL query against the database.


     * 


     * @Query annotation allows writing complex queries beyond Spring Data naming conventions.


     * - JPQL queries use entity names and field names (database-independent)


     * - Native queries use actual table/column names (database-specific SQL)


     * - :paramName binds method parameters to query


     * - ?1, ?2 for positional parameters


     * 


     * WHY CUSTOM QUERY:


     * - Complex joins across multiple tables


     * - Aggregations (COUNT, SUM, AVG, GROUP BY)


     * - Subqueries or conditional logic


     * - Performance optimization (specific columns, indexes)


     * 


     * Spring Data auto-implements this method at runtime.


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
