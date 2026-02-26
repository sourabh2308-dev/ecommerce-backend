package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload describing return request status and resolution details.
 */
@Data
@Builder
public class ReturnResponse {
    private String uuid;
    private String orderUuid;
    private String buyerUuid;
    private String returnType;
    private String reason;
    private String status;
    private String adminNotes;
    private Double refundAmount;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
