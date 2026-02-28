package com.sourabh.order_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

/**
 * Request DTO sent from order-service to user-service when asking the latter to
 * actually deliver an invoice email to a buyer. The PDF is encoded in base64
 * to avoid complications with binary payloads over JSON.
 */
@Getter
@Setter
@Builder
@Jacksonized
public class InvoiceEmailRequest {
    private String toEmail;
    private String orderUuid;
    private String pdfBase64;
}
