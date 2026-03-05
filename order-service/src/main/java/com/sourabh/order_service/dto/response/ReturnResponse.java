package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO describing the current state of a return request,
 * including resolution details and refund information.
 */
@Data
@Builder
public class ReturnResponse {

    /** Unique identifier of the return request. */
    private String uuid;

    /** UUID of the order being returned. */
    private String orderUuid;

    /** UUID of the buyer who initiated the return. */
    private String buyerUuid;

    /** Type of return: {@code "REFUND"} or {@code "EXCHANGE"}. */
    private String returnType;

    /** Buyer-supplied reason for the return. */
    private String reason;

    /** Current status of the return request (e.g., {@code "PENDING"}, {@code "APPROVED"}, {@code "REJECTED"}). */
    private String status;

    /** Optional notes added by the admin during review. */
    private String adminNotes;

    /** Refund amount issued (applicable for {@code "REFUND"} type returns). */
    private Double refundAmount;

    /** Timestamp when the return request was resolved (approved or rejected). */
    private LocalDateTime resolvedAt;

    /** Timestamp when the return request was created. */
    private LocalDateTime createdAt;
}
