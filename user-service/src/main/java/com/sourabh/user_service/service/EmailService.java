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
}
