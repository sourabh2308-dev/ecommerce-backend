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

@RestController
@RequestMapping("/api/user/support")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(supportTicketService.createTicket(request, userUuid));
    }

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping("/{uuid}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        String role = httpRequest.getHeader("X-User-Role");
        return ResponseEntity.ok(supportTicketService.getTicket(uuid, userUuid, role));
    }

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<TicketResponse>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String userUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(supportTicketService.getMyTickets(userUuid, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<TicketResponse>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(supportTicketService.getAllTickets(page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<PageResponse<TicketResponse>> getByStatus(
            @PathVariable TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(supportTicketService.getTicketsByStatus(status, page, size));
    }

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

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{uuid}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable String uuid,
            @RequestParam TicketStatus status,
            HttpServletRequest httpRequest) {
        String adminUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(supportTicketService.updateStatus(uuid, status, adminUuid));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{uuid}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String adminUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(supportTicketService.assignTicket(uuid, adminUuid));
    }
}
