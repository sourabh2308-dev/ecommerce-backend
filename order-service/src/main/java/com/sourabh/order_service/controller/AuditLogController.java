package com.sourabh.order_service.controller;

import com.sourabh.order_service.audit.AuditLog;
import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing admin-only endpoints for querying the order audit trail.
 *
 * <p>Audit logs capture every significant order lifecycle event including who
 * performed the action (actor UUID and role), what changed (before/after state
 * stored as JSON), and when it occurred. This controller surfaces that data
 * for compliance, dispute resolution, and operational monitoring.</p>
 *
 * <p>Base path: {@code /api/order/audit}</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see AuditLog
 * @see AuditLogRepository
 */
@RestController
@RequestMapping("/api/order/audit")
@RequiredArgsConstructor
public class AuditLogController {

    /** Repository providing paginated access to the audit log table. */
    private final AuditLogRepository auditLogRepository;

    /**
     * Retrieves a paginated list of all audit log entries.
     *
     * @param page zero-based page index (default {@code 0})
     * @param size number of records per page (default {@code 50})
     * @return {@link ResponseEntity} containing a {@link PageResponse} of
     *         {@link AuditLog} entries
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<AuditLog>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logPage = auditLogRepository.findAll(PageRequest.of(page, size));
        PageResponse<AuditLog> response = PageResponse.<AuditLog>builder()
                .content(logPage.getContent())
                .page(logPage.getNumber())
                .size(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .last(logPage.isLast())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves audit log entries for a specific actor (user), ordered by
     * creation date descending.
     *
     * @param actorUuid UUID of the actor whose actions should be retrieved
     * @param page      zero-based page index (default {@code 0})
     * @param size      number of records per page (default {@code 50})
     * @return {@link ResponseEntity} containing a {@link PageResponse} of
     *         {@link AuditLog} entries filtered by the given actor
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/actor/{actorUuid}")
    public ResponseEntity<PageResponse<AuditLog>> getActorAuditLogs(
            @PathVariable String actorUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logPage = auditLogRepository.findByActorUuidOrderByCreatedAtDesc(actorUuid, PageRequest.of(page, size));
        PageResponse<AuditLog> response = PageResponse.<AuditLog>builder()
                .content(logPage.getContent())
                .page(logPage.getNumber())
                .size(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .last(logPage.isLast())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves audit log entries for a specific resource, ordered by
     * creation date descending.
     *
     * <p>A resource is identified by its type (e.g. {@code "ORDER"}) and its
     * unique identifier (e.g. the order UUID).</p>
     *
     * @param resourceType the type of the audited resource
     * @param resourceId   the unique identifier of the audited resource
     * @param page         zero-based page index (default {@code 0})
     * @param size         number of records per page (default {@code 50})
     * @return {@link ResponseEntity} containing a {@link PageResponse} of
     *         matching {@link AuditLog} entries
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/resource/{resourceType}/{resourceId}")
    public ResponseEntity<PageResponse<AuditLog>> getResourceAuditLogs(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logPage = auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId, PageRequest.of(page, size));
        PageResponse<AuditLog> response = PageResponse.<AuditLog>builder()
                .content(logPage.getContent())
                .page(logPage.getNumber())
                .size(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .last(logPage.isLast())
                .build();
        return ResponseEntity.ok(response);
    }
}
