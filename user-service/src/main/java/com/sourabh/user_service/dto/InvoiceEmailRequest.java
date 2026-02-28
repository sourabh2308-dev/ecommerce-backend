package com.sourabh.user_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
public class InvoiceEmailRequest {
    private String toEmail;
    private String orderUuid;
    private String pdfBase64;
}
