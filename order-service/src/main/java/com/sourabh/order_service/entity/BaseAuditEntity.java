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
 * Abstract mapped superclass that provides automatic auditing timestamps
 * for all JPA entities in the order-service.
 *
 * <p>Entities extending this class automatically inherit {@code createdAt} and
 * {@code updatedAt} columns whose values are managed by Spring Data JPA's
 * {@link AuditingEntityListener}. The application configuration must enable
 * JPA auditing with {@code @EnableJpaAuditing} for the timestamps to be
 * populated on persist and merge operations.</p>
 *
 * <p>Subclasses include {@link Order}, {@link Coupon}, and {@link ReturnRequest}.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

    /**
     * Timestamp recording when the entity was first persisted.
     *
     * <p>Set automatically by Spring Data JPA on the initial {@code save()} call.
     * The column is marked {@code updatable = false} so the value remains
     * immutable after creation.</p>
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp recording the most recent modification to the entity.
     *
     * <p>Updated automatically by Spring Data JPA on every subsequent
     * {@code save()} call. Useful for change-tracking, cache invalidation,
     * and optimistic-locking strategies.</p>
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
