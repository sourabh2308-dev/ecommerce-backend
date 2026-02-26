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
 * REST Controller for managing order-related audit logs.
 * 
 * <p>Provides admin-only access to audit trail for compliance and debugging.
 * 
 * <p>Audit logs capture:
 * <ul>
 *   <li>Order lifecycle events (created, confirmed, shipped, delivered)</li>
 *   <li>Who performed each action (actor UUID and role)</li>
 *   <li>What changed (before/after state stored as JSON)</li>
 *   <li>When the action occurred (timestamp)</li>
 * </ul>
 * 
 * <p>Useful for:
 * <ul>
 *   <li>Investigating order disputes</li>
 *   <li>Compliance audits</li>
 *   <li>Tracking admin/seller actions</li>
 * </ul>
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@RestController
@RequestMapping("/api/order/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    /**
     * Lists all audit logs with pagination.
     * 
     * <p>Returns comprehensive audit trail of all order-related activities.
     * 
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return ResponseEntity with paginated audit logs
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
     * Retrieves audit logs for a specific actor (user).
     * 
     * <p>Filters audit trail to show only actions performed by
     * a particular user (admin/seller/buyer).
     * 
     * @param actorUuid the UUID of the actor
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return ResponseEntity with paginated audit logs for the actor
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
