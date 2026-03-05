package com.sourabh.user_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

/**
 * Request payload consumed from the Kafka invoice topic to trigger an
 * invoice email to a customer.
 *
 * <p>Carries the recipient address, associated order identifier and the
 * Base64-encoded PDF attachment so that the email service can assemble
 * and dispatch the message without additional lookups.</p>
 */
@Getter
@Setter
@Builder
@Jacksonized
public class InvoiceEmailRequest {

    /** Email address of the invoice recipient. */
    private String toEmail;

    /** UUID of the order to which the invoice belongs. */
    private String orderUuid;

    /** Base64-encoded PDF content of the invoice attachment. */
    private String pdfBase64;
}
