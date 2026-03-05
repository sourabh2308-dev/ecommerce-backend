package com.sourabh.order_service.repository;

import com.sourabh.order_service.audit.AuditLog;
import com.sourabh.order_service.audit.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link AuditLog} entities.
 *
 * <p>Provides paginated and filtered access to the audit trail, allowing
 * queries by actor, action type, resource, and time range. Method names
 * follow Spring Data naming conventions so implementations are generated
 * at runtime.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see AuditLog
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Finds audit logs by actor UUID, ordered by creation date descending.
     *
     * @param actorUuid UUID of the actor
     * @param pageable  pagination information
     * @return page of matching {@link AuditLog} entries
     */
    Page<AuditLog> findByActorUuidOrderByCreatedAtDesc(String actorUuid, Pageable pageable);

    /**
     * Finds audit logs by action type, ordered by creation date descending.
     *
     * @param action   the {@link AuditAction} to filter by
     * @param pageable pagination information
     * @return page of matching {@link AuditLog} entries
     */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    /**
     * Finds audit logs for a specific resource (identified by type and ID),
     * ordered by creation date descending.
     *
     * @param resourceType the type of the audited resource (e.g. {@code "ORDER"})
     * @param resourceId   the unique identifier of the audited resource
     * @param pageable     pagination information
     * @return page of matching {@link AuditLog} entries
     */
    Page<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(String resourceType, String resourceId, Pageable pageable);

    /**
     * Finds audit logs created within a given time range, ordered by
     * creation date descending.
     *
     * @param from range start (inclusive)
     * @param to   range end (inclusive)
     * @return list of matching {@link AuditLog} entries
     */
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
