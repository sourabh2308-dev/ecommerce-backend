package com.sourabh.user_service.service;

/**
 * Service interface for sending transactional emails.
 *
 * <p>All methods are designed to be called asynchronously via {@code @Async}
 * so that SMTP latency does not block the calling thread.  Implementations
 * use Spring Mail with a {@link jakarta.mail.internet.MimeMessage} for
 * HTML content and attachments.</p>
 *
 * <p>Supported email types:</p>
 * <ul>
 *   <li><strong>OTP emails</strong> &ndash; branded HTML template with a one-time passcode</li>
 *   <li><strong>Generic HTML emails</strong> &ndash; arbitrary HTML body (order confirmations, etc.)</li>
 *   <li><strong>Invoice emails</strong> &ndash; PDF attachment decoded from a Base64 payload</li>
 * </ul>
 *
 * @see com.sourabh.user_service.service.impl.EmailServiceImpl
 */
public interface EmailService {

    /**
     * Sends an OTP verification email using a branded HTML template.
     *
     * @param toEmail  the recipient email address
     * @param userName the display name used in the greeting (e.g. "Hello Sourabh")
     * @param otpCode  the 6-digit OTP code to embed in the email body
     * @param subject  the email subject line (e.g. "Email Verification OTP")
     */
    void sendOtpEmail(String toEmail, String userName, String otpCode, String subject);

    /**
     * Sends a generic HTML email with the supplied body.
     *
     * @param toEmail  the recipient email address
     * @param subject  the email subject line
     * @param htmlBody the complete HTML content to use as the email body
     */
    void sendHtmlEmail(String toEmail, String subject, String htmlBody);

    /**
     * Sends an invoice email with a PDF attachment.
     *
     * <p>The PDF bytes are provided as a Base64-encoded string. The implementation
     * decodes the payload and attaches it as {@code invoice-{orderUuid}.pdf}
     * (MIME type {@code application/pdf}).</p>
     *
     * @param toEmail   the recipient email address
     * @param orderUuid the order identifier used in the filename and subject
     * @param pdfBase64 Base64-encoded PDF bytes to attach
     */
    void sendInvoiceEmail(String toEmail, String orderUuid, String pdfBase64);
}
