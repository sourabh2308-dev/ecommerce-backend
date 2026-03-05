package com.sourabh.order_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

/**
 * Request DTO sent from the order-service to the user-service (via Feign)
 * to trigger delivery of an invoice email to a buyer.
 *
 * <p>The invoice PDF is encoded as a Base64 string to avoid complications
 * with binary payloads over JSON-based inter-service communication.</p>
 */
@Getter
@Setter
@Builder
@Jacksonized
public class InvoiceEmailRequest {

    /** Recipient email address (the buyer's email). */
    private String toEmail;

    /** UUID of the order for which the invoice was generated. */
    private String orderUuid;

    /** Base64-encoded content of the invoice PDF. */
    private String pdfBase64;
}
