package com.sourabh.user_service.dto.response;

import com.sourabh.user_service.entity.TicketCategory;
import com.sourabh.user_service.entity.TicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload describing a customer-support ticket together with
 * its conversation messages.
 *
 * <p>Includes ticket metadata (subject, description, category, status),
 * admin assignment info, and the full list of {@link MessageResponse}
 * entries exchanged within the ticket.</p>
 */
@Getter
@Builder
public class TicketResponse {

    /** Unique identifier of the ticket. */
    private String uuid;

    /** UUID of the user who created the ticket. */
    private String userUuid;

    /** Brief summary of the support issue. */
    private String subject;

    /** Detailed description of the issue. */
    private String description;

    /** Category classifying the ticket (e.g. ORDER, PAYMENT, ACCOUNT). */
    private TicketCategory category;

    /** Current status of the ticket (e.g. OPEN, IN_PROGRESS, RESOLVED). */
    private TicketStatus status;

    /** Optional UUID of the order related to this ticket. */
    private String orderUuid;

    /** UUID of the admin assigned to handle this ticket (may be {@code null}). */
    private String assignedAdminUuid;

    /** Ordered list of messages exchanged on this ticket. */
    private List<MessageResponse> messages;

    /** Timestamp when the ticket was created. */
    private LocalDateTime createdAt;

    /** Timestamp when the ticket was resolved (may be {@code null}). */
    private LocalDateTime resolvedAt;
}
