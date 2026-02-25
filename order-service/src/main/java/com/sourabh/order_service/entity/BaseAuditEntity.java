package com.sourabh.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Shared audit fields for all entities.
 * Spring Data JPA populates {@code createdAt} and {@code updatedAt}
 * automatically when {@code @EnableJpaAuditing} is present on the main class.
 */
@MappedSuperclass
// JPA Entity - Domain model persisted in database
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

    @CreatedDate
    /**

     * DATABASE COLUMN MAPPING

     * 

     * @Column configures how this field maps to database column:

     * - name: Actual column name in table (default: field name in snake_case)

     * - nullable: Can be NULL in database (default: true)

     * - unique: Enforces uniqueness constraint (default: false)

     * - length: Max length for VARCHAR columns (default: 255)

     * - updatable: Can be modified after insert (default: true)

     * - insertable: Included in INSERT statements (default: true)

     * 

     * JPA auto-generates SQL schema based on these annotations.

     */

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
