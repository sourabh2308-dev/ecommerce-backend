package com.sourabh.user_service.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String userName, String otpCode, String subject);

    void sendHtmlEmail(String toEmail, String subject, String htmlBody);

    void sendInvoiceEmail(String toEmail, String orderUuid, String pdfBase64);
}
