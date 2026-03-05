package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.CreateTicketRequest;
import com.sourabh.user_service.dto.response.MessageResponse;
import com.sourabh.user_service.dto.response.TicketResponse;
import com.sourabh.user_service.entity.*;
import com.sourabh.user_service.repository.SupportTicketRepository;
import com.sourabh.user_service.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link SupportTicketService} for the customer-support ticketing system.
 *
 * <p>Manages the full ticket lifecycle from creation through resolution/closure.
 * Each ticket may have an ordered list of {@link SupportMessage} entries that
 * form a conversation between the customer and the assigned admin.</p>
 *
 * <p>Ticket status transitions are automated where possible:</p>
 * <ul>
 *   <li>Admin reply &rarr; {@code AWAITING_CUSTOMER}</li>
 *   <li>Customer reply &rarr; {@code IN_PROGRESS}</li>
 *   <li>Admin assigns themselves &rarr; {@code IN_PROGRESS}</li>
 *   <li>Status set to {@code RESOLVED}/{@code CLOSED} &rarr; {@code resolvedAt} timestamp recorded</li>
 * </ul>
 *
 * @see SupportTicketService
 * @see SupportTicketRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SupportTicketServiceImpl implements SupportTicketService {

    /** Repository for {@link SupportTicket} persistence operations. */
    private final SupportTicketRepository ticketRepository;

    /** {@inheritDoc} */
    @Override
    public TicketResponse createTicket(CreateTicketRequest request, String userUuid) {
        SupportTicket ticket = SupportTicket.builder()
                .userUuid(userUuid)
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : TicketCategory.OTHER)
                .orderUuid(request.getOrderUuid())
                .build();
        ticketRepository.save(ticket);
        log.info("Support ticket created: uuid={}, userUuid={}", ticket.getUuid(), userUuid);
        return mapToResponse(ticket);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicket(String ticketUuid, String userUuid, String role) {
        SupportTicket ticket = ticketRepository.findByUuid(ticketUuid)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketUuid));
        if (!"ADMIN".equalsIgnoreCase(role) && !ticket.getUserUuid().equals(userUuid)) {
            throw new RuntimeException("Unauthorized access to ticket");
        }
        return mapToResponse(ticket);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getMyTickets(String userUuid, int page, int size) {
        Page<SupportTicket> ticketPage = ticketRepository
                .findByUserUuidOrderByCreatedAtDesc(userUuid, PageRequest.of(page, size));
        return toPageResponse(ticketPage);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getAllTickets(int page, int size) {
        Page<SupportTicket> ticketPage = ticketRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return toPageResponse(ticketPage);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getTicketsByStatus(TicketStatus status, int page, int size) {
        Page<SupportTicket> ticketPage = ticketRepository
                .findByStatusOrderByCreatedAtAsc(status, PageRequest.of(page, size));
        return toPageResponse(ticketPage);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Auto-updates ticket status based on sender role:
     * admin replies set {@code AWAITING_CUSTOMER}, customer replies set {@code IN_PROGRESS}.</p>
     */
    @Override
    public TicketResponse sendMessage(String ticketUuid, String senderUuid, String senderRole, String content) {
        SupportTicket ticket = ticketRepository.findByUuid(ticketUuid)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketUuid));

        if (!"ADMIN".equalsIgnoreCase(senderRole) && !ticket.getUserUuid().equals(senderUuid)) {
            throw new RuntimeException("Unauthorized to send messages on this ticket");
        }

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .senderUuid(senderUuid)
                .senderRole(senderRole)
                .content(content)
                .build();
        ticket.getMessages().add(message);

        if ("ADMIN".equalsIgnoreCase(senderRole)) {
            ticket.setStatus(TicketStatus.AWAITING_CUSTOMER);
        } else {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        ticketRepository.save(ticket);
        log.info("Message sent on ticket {}: senderRole={}", ticketUuid, senderRole);
        return mapToResponse(ticket);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets {@code resolvedAt} timestamp when status becomes
     * {@code RESOLVED} or {@code CLOSED}.</p>
     */
    @Override
    public TicketResponse updateStatus(String ticketUuid, TicketStatus status, String adminUuid) {
        SupportTicket ticket = ticketRepository.findByUuid(ticketUuid)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketUuid));
        ticket.setStatus(status);
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        ticketRepository.save(ticket);
        log.info("Ticket {} status updated to {} by admin {}", ticketUuid, status, adminUuid);
        return mapToResponse(ticket);
    }

    /** {@inheritDoc} */
    @Override
    public TicketResponse assignTicket(String ticketUuid, String adminUuid) {
        SupportTicket ticket = ticketRepository.findByUuid(ticketUuid)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketUuid));
        ticket.setAssignedAdminUuid(adminUuid);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);
        log.info("Ticket {} assigned to admin {}", ticketUuid, adminUuid);
        return mapToResponse(ticket);
    }

    /**
     * Maps a {@link SupportTicket} entity (including its messages) to a
     * {@link TicketResponse} DTO.
     *
     * @param ticket the ticket entity to convert
     * @return the corresponding response DTO with embedded message list
     */
    private TicketResponse mapToResponse(SupportTicket ticket) {
        List<MessageResponse> messages = ticket.getMessages() != null
                ? ticket.getMessages().stream().map(m -> MessageResponse.builder()
                        .id(m.getId())
                        .senderUuid(m.getSenderUuid())
                        .senderRole(m.getSenderRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build()).toList()
                : Collections.emptyList();

        return TicketResponse.builder()
                .uuid(ticket.getUuid())
                .userUuid(ticket.getUserUuid())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .status(ticket.getStatus())
                .orderUuid(ticket.getOrderUuid())
                .assignedAdminUuid(ticket.getAssignedAdminUuid())
                .messages(messages)
                .createdAt(ticket.getCreatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }

    /**
     * Converts a Spring Data {@link Page} of {@link SupportTicket} entities
     * into a generic {@link PageResponse} of {@link TicketResponse} DTOs.
     *
     * @param page the page of ticket entities
     * @return the assembled page response
     */
    private PageResponse<TicketResponse> toPageResponse(Page<SupportTicket> page) {
        return PageResponse.<TicketResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
