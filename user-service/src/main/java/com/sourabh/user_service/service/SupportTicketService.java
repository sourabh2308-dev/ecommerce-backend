package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.CreateTicketRequest;
import com.sourabh.user_service.dto.response.TicketResponse;
import com.sourabh.user_service.entity.TicketStatus;

public interface SupportTicketService {

    TicketResponse createTicket(CreateTicketRequest request, String userUuid);

    TicketResponse getTicket(String ticketUuid, String userUuid, String role);

    PageResponse<TicketResponse> getMyTickets(String userUuid, int page, int size);

    PageResponse<TicketResponse> getAllTickets(int page, int size);

    PageResponse<TicketResponse> getTicketsByStatus(TicketStatus status, int page, int size);

    TicketResponse sendMessage(String ticketUuid, String senderUuid, String senderRole, String content);

    TicketResponse updateStatus(String ticketUuid, TicketStatus status, String adminUuid);

    TicketResponse assignTicket(String ticketUuid, String adminUuid);
}
