package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.CreateTicketRequest;
import com.sourabh.user_service.dto.request.SendMessageRequest;
import com.sourabh.user_service.dto.response.TicketResponse;
import com.sourabh.user_service.entity.TicketStatus;
import com.sourabh.user_service.service.SupportTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for customer-support ticket management.
 * <p>
 * Buyers and sellers can create tickets, view their own tickets, and
 * post messages. Administrators can view all tickets, filter by
 * status, update statuses, and assign tickets to themselves.
 * </p>
 *
 * <p>Base path: {@code /api/user/support}</p>
 *
 * @see SupportTicketService
 */
@RestController
@RequestMapping("/api/user/support")
@RequiredArgsConstructor
public class SupportTicketController {

    /** Service layer handling support-ticket business logic. */
    private final SupportTicketService supportTicketService;

    /**
     * Creates a new support ticket on behalf of the authenticated user.
     *
     * @param request     validated ticket creation payload
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return the newly created {@link TicketResponse}
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(supportTicketService.createTicket(request, userUuid));
    }

    /**
     * Retrieves a specific ticket by its UUID. Admins may view any
     * ticket; regular users may only view their own.
     *
     * @param uuid        UUID of the ticket
     * @param httpRequest the HTTP request carrying user context headers
     * @return the matching {@link TicketResponse}
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping("/{uuid}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        String role = httpRequest.getHeader("X-User-Role");
        return ResponseEntity.ok(supportTicketService.getTicket(uuid, userUuid, role));
    }

    /**
     * Returns a paginated list of tickets created by the authenticated user.
     *
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 10)
     * @param httpRequest the HTTP request carrying the {@code X-User-UUID} header
     * @return paginated {@link TicketResponse} list
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<TicketResponse>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(supportTicketService.getMyTickets(userUuid, page, size));
    }

    /**
     * Admin-only: returns a paginated list of all support tickets,
     * newest first.
     *
     * @param page zero-based page index (default 0)
     * @param size page size (default 10)
     * @return paginated {@link TicketResponse} list
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<TicketResponse>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(supportTicketService.getAllTickets(page, size));
    }

    /**
     * Admin-only: returns tickets filtered by the given status.
     *
     * @param status the {@link TicketStatus} to filter on
     * @param page   zero-based page index (default 0)
     * @param size   page size (default 10)
     * @return paginated {@link TicketResponse} list matching the status
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<PageResponse<TicketResponse>> getByStatus(
            @PathVariable TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(supportTicketService.getTicketsByStatus(status, page, size));
    }

    /**
     * Posts a new message on an existing ticket thread. Available to
     * both the ticket owner and support administrators.
     *
     * @param uuid        UUID of the ticket
     * @param request     validated message payload
     * @param httpRequest the HTTP request carrying sender context headers
     * @return the updated {@link TicketResponse} including the new message
     */
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @PostMapping("/{uuid}/message")
    public ResponseEntity<TicketResponse> sendMessage(
            @PathVariable String uuid,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        String senderUuid = httpRequest.getHeader("X-User-UUID");
        String senderRole = httpRequest.getHeader("X-User-Role");
        return ResponseEntity.ok(supportTicketService.sendMessage(uuid, senderUuid, senderRole, request.getContent()));
    }

    /**
     * Admin-only: updates the status of a ticket (e.g. OPEN to
     * IN_PROGRESS, or RESOLVED).
     *
     * @param uuid        UUID of the ticket to update
     * @param status      new {@link TicketStatus}
     * @param httpRequest the HTTP request carrying the admin's UUID
     * @return the updated {@link TicketResponse}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{uuid}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable String uuid,
            @RequestParam TicketStatus status,
            HttpServletRequest httpRequest) {
        String adminUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(supportTicketService.updateStatus(uuid, status, adminUuid));
    }

    /**
     * Admin-only: assigns the ticket to the requesting administrator.
     *
     * @param uuid        UUID of the ticket to assign
     * @param httpRequest the HTTP request carrying the admin's UUID
     * @return the updated {@link TicketResponse}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{uuid}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String adminUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(supportTicketService.assignTicket(uuid, adminUuid));
    }
}
