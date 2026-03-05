package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link EmailService} using Spring Mail and SMTP.
 *
 * <p>All send methods are annotated with {@code @Async} so that SMTP I/O runs on
 * a separate thread pool, preventing the calling thread (e.g. the HTTP request
 * handler) from blocking on network latency. Failures are logged but <strong>not
 * propagated</strong>; the OTP is already persisted in the database, so the user
 * can request a resend if the email is lost.</p>
 *
 * <p>Configuration is drawn from {@code application.properties}:</p>
 * <ul>
 *   <li>{@code app.mail.from-address} &ndash; the sender email address</li>
 *   <li>{@code app.mail.from-name} &ndash; the sender display name</li>
 *   <li>{@code spring.mail.*} &ndash; standard Spring Mail SMTP settings</li>
 * </ul>
 *
 * @see EmailService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    /** Spring-provided SMTP mail sender. */
    private final JavaMailSender mailSender;

    /** Sender email address shown in the "From" header. */
    @Value("${app.mail.from-address:noreply@ecommerce.com}")
    private String fromAddress;

    /** Sender display name shown alongside the email address. */
    @Value("${app.mail.from-name:E-Commerce Platform}")
    private String fromName;

    /**
     * {@inheritDoc}
     *
     * <p>Constructs a branded HTML email using {@link #buildOtpHtml(String, String, String)}
     * and sends it asynchronously via SMTP.</p>
     */
    @Override
    @Async
    public void sendOtpEmail(String toEmail, String userName, String otpCode, String subject) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildOtpHtml(userName, otpCode, subject), true);

            mailSender.send(message);
            log.info("[EMAIL] OTP email sent to {}", toEmail);

        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException ex) {
            log.error("[EMAIL] Failed to send OTP email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends an arbitrary HTML body (e.g. order confirmation, delivery notification)
     * asynchronously via SMTP.</p>
     */
    @Override
    @Async
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EMAIL] HTML email sent to {} - subject: {}", toEmail, subject);
        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException ex) {
            log.error("[EMAIL] Failed to send HTML email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Decodes the Base64 PDF payload, attaches it as
     * {@code invoice-{orderUuid}.pdf} (MIME type {@code application/pdf}),
     * and sends the email asynchronously.</p>
     */
    @Override
    @Async
    public void sendInvoiceEmail(String toEmail, String orderUuid, String pdfBase64) {
        try {
            byte[] pdfBytes = pdfBase64 != null ? java.util.Base64.getDecoder().decode(pdfBase64) : new byte[0];

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Invoice for order " + orderUuid);
            helper.setText("Please find attached your invoice.", true);

            if (pdfBytes.length > 0) {
                helper.addAttachment("invoice-" + orderUuid + ".pdf",
                        new jakarta.mail.util.ByteArrayDataSource(pdfBytes, "application/pdf"));
            }

            mailSender.send(message);
            log.info("[EMAIL] Invoice email sent to {} for order {} ({} bytes)",
                    toEmail, orderUuid, pdfBytes.length);
        } catch (Exception ex) {
            log.error("[EMAIL] Failed to send invoice email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    /**
     * Builds a branded HTML email template for OTP verification.
     *
     * <p>The template includes the subject as a header, a personalized greeting,
     * the OTP code in large styled text, and a footer with a 5-minute validity notice.</p>
     *
     * @param userName the recipient's first name (falls back to "User" if blank)
     * @param otpCode  the 6-digit OTP code to display
     * @param subject  the email subject, also used as the template header
     * @return the complete HTML string ready for sending
     */
    private String buildOtpHtml(String userName, String otpCode, String subject) {
        String displayName = (userName != null && !userName.isBlank()) ? userName : "User";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;font-family:Arial,Helvetica,sans-serif;background:#f4f4f7;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:40px auto;background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                    <tr>
                      <td style="background:#4f46e5;padding:24px 32px;">
                        <h1 style="margin:0;color:#ffffff;font-size:22px;">%s</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <p style="margin:0 0 16px;font-size:16px;color:#333;">Hello <strong>%s</strong>,</p>
                        <p style="margin:0 0 24px;font-size:16px;color:#555;">Use the following one-time password to complete your verification:</p>
                        <div style="text-align:center;margin:0 0 24px;">
                          <span style="display:inline-block;font-size:32px;font-weight:700;letter-spacing:8px;padding:16px 32px;background:#f0f0ff;border-radius:8px;color:#4f46e5;">%s</span>
                        </div>
                        <p style="margin:0 0 8px;font-size:14px;color:#888;">This code is valid for <strong>5 minutes</strong>. Do not share it with anyone.</p>
                        <p style="margin:0;font-size:14px;color:#888;">If you did not request this, please ignore this email.</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:16px 32px;background:#f9fafb;text-align:center;font-size:12px;color:#aaa;">
                        &copy; 2026 E-Commerce Platform. All rights reserved.
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(subject, displayName, otpCode);
    }
}
