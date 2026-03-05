package com.sourabh.user_service.entity;

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
 * Abstract mapped super-class providing audit timestamp fields to all
 * entities that extend it.
 * <p>
 * Spring Data JPA's {@link AuditingEntityListener} automatically
 * populates {@code createdAt} on initial persist and {@code updatedAt}
 * on every subsequent update, provided {@code @EnableJpaAuditing} is
 * declared on the application's main configuration class.
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
     * Timestamp recorded automatically when the entity is first persisted.
     * Marked as non-updatable so it is never overwritten by subsequent saves.
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp updated automatically every time the entity is modified
     * and flushed to the database.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
