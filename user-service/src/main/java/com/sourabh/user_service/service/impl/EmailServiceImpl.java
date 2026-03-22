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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address:noreply@ecommerce.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:E-Commerce Platform}")
    private String fromName;

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
