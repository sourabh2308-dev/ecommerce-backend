package com.sourabh.product_service.entity;

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
 * Abstract base class providing automatic audit timestamps for all JPA entities.
 * <p>
 * Entities extending this class automatically receive {@code createdAt} and
 * {@code updatedAt} fields that are populated by Spring Data JPA's auditing
 * infrastructure. Requires {@code @EnableJpaAuditing} on the application
 * configuration class.
 * </p>
 *
 * @see org.springframework.data.jpa.domain.support.AuditingEntityListener
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

    /**
     * Timestamp indicating when the entity was first persisted.
     * Automatically set by Spring Data JPA on initial save and
     * marked as non-updatable so it remains immutable thereafter.
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the entity was last modified.
     * Automatically updated by Spring Data JPA on every save operation.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
