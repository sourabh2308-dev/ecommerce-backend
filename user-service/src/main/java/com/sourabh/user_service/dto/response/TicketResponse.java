package com.sourabh.user_service.dto.response;

import com.sourabh.user_service.entity.TicketCategory;
import com.sourabh.user_service.entity.TicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload describing a support ticket and its messages.
 */
@Getter
@Builder
public class TicketResponse {
    private String uuid;
    private String userUuid;
    private String subject;
    private String description;
    private TicketCategory category;
    private TicketStatus status;
    private String orderUuid;
    private String assignedAdminUuid;
    private List<MessageResponse> messages;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
