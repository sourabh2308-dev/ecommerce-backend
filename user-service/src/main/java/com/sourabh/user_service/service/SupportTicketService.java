package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.CreateTicketRequest;
import com.sourabh.user_service.dto.response.TicketResponse;
import com.sourabh.user_service.entity.TicketStatus;

/**
 * Service interface for the customer-support ticketing system.
 *
 * <p>Supports a full ticket lifecycle: creation &rarr; assignment &rarr;
 * messaging &rarr; resolution/closure.  Tickets carry a {@link TicketStatus}
 * that is updated automatically when messages are exchanged between the
 * customer and the assigned admin.</p>
 *
 * <p>Access control:</p>
 * <ul>
 *   <li>Customers may only view and message their own tickets.</li>
 *   <li>Admins may view all tickets, assign themselves, and change status.</li>
 * </ul>
 *
 * @see com.sourabh.user_service.service.impl.SupportTicketServiceImpl
 */
public interface SupportTicketService {

    /**
     * Creates a new support ticket on behalf of the user.
     *
     * @param request  ticket subject, description, category, and optional order UUID
     * @param userUuid the UUID of the user opening the ticket
     * @return the created {@link TicketResponse}
     */
    TicketResponse createTicket(CreateTicketRequest request, String userUuid);

    /**
     * Retrieves a single ticket by its UUID, enforcing ownership or admin access.
     *
     * @param ticketUuid the UUID of the ticket
     * @param userUuid   the UUID of the requesting user
     * @param role       the role of the requesting user (e.g. "ADMIN" or "BUYER")
     * @return the matching {@link TicketResponse}
     * @throws RuntimeException if the ticket is not found or access is unauthorized
     */
    TicketResponse getTicket(String ticketUuid, String userUuid, String role);

    /**
     * Returns a paginated list of tickets created by the specified user.
     *
     * @param userUuid the UUID of the user
     * @param page     zero-based page index
     * @param size     page size
     * @return paginated {@link TicketResponse} list ordered by creation date descending
     */
    PageResponse<TicketResponse> getMyTickets(String userUuid, int page, int size);

    /**
     * Returns a paginated list of <em>all</em> tickets (admin-only view).
     *
     * @param page zero-based page index
     * @param size page size
     * @return paginated {@link TicketResponse} list
     */
    PageResponse<TicketResponse> getAllTickets(int page, int size);

    /**
     * Returns a paginated list of tickets filtered by status (admin-only view).
     *
     * @param status the {@link TicketStatus} to filter by
     * @param page   zero-based page index
     * @param size   page size
     * @return paginated {@link TicketResponse} list ordered by creation date ascending
     */
    PageResponse<TicketResponse> getTicketsByStatus(TicketStatus status, int page, int size);

    /**
     * Appends a message to an existing ticket conversation.
     *
     * <p>Ticket status is auto-updated based on the sender's role:
     * admin replies set status to {@code AWAITING_CUSTOMER}; customer
     * replies set it to {@code IN_PROGRESS}.</p>
     *
     * @param ticketUuid the UUID of the ticket
     * @param senderUuid the UUID of the message sender
     * @param senderRole the role of the sender ("ADMIN" or user role)
     * @param content    the message text
     * @return the updated {@link TicketResponse} including the new message
     * @throws RuntimeException if unauthorized
     */
    TicketResponse sendMessage(String ticketUuid, String senderUuid, String senderRole, String content);

    /**
     * Manually updates the status of a ticket (admin action).
     *
     * <p>If the new status is {@code RESOLVED} or {@code CLOSED}, the
     * {@code resolvedAt} timestamp is set.</p>
     *
     * @param ticketUuid the UUID of the ticket
     * @param status     the new {@link TicketStatus}
     * @param adminUuid  the UUID of the admin performing the action
     * @return the updated {@link TicketResponse}
     */
    TicketResponse updateStatus(String ticketUuid, TicketStatus status, String adminUuid);

    /**
     * Assigns a ticket to an admin and sets its status to {@code IN_PROGRESS}.
     *
     * @param ticketUuid the UUID of the ticket
     * @param adminUuid  the UUID of the admin claiming the ticket
     * @return the updated {@link TicketResponse}
     */
    TicketResponse assignTicket(String ticketUuid, String adminUuid);
}
