package com.sourabh.user_service.service;

/**
 * Service responsible for sending transactional emails (OTP, notifications).
 */
public interface EmailService {

    /**
     * Send an OTP email to the given recipient.
     */
    void sendOtpEmail(String toEmail, String userName, String otpCode, String subject);

    /**
     * Send a generic HTML email.
     */
    void sendHtmlEmail(String toEmail, String subject, String htmlBody);

    /**
     * Sends an invoice attachment encoded as base64 to the given address.
     * <p>The service is expected to decode the PDF data and attach it to an
     * email (MIME type application/pdf) before sending.  In development the
     * implementation may simply log the payload but production should use a
     * real SMTP connection.
     *
     * @param toEmail recipient address
     * @param orderUuid order identifier (used in filename/subject)
     * @param pdfBase64 Base64‑encoded PDF bytes
     */
    void sendInvoiceEmail(String toEmail, String orderUuid, String pdfBase64);
}
