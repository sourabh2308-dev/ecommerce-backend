package com.sourabh.user_service.dto.request;

import com.sourabh.user_service.entity.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating a new customer-support ticket.
 *
 * <p>A ticket must have a subject and description. The optional
 * {@link #category} classifies the issue (e.g. ORDER, PAYMENT, ACCOUNT),
 * and {@link #orderUuid} links the ticket to a specific order when
 * applicable.</p>
 */
@Getter
@Setter
public class CreateTicketRequest {

    /** Brief summary of the support issue (max 200 characters). */
    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must be under 200 characters")
    private String subject;

    /** Detailed description of the issue (max 2000 characters). */
    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must be under 2000 characters")
    private String description;

    /** Optional category for classifying the ticket. */
    private TicketCategory category;

    /** Optional UUID of the order related to this support ticket. */
    private String orderUuid;
}
