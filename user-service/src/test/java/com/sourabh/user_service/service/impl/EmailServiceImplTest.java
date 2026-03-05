package com.sourabh.user_service.service.impl;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailServiceImpl}.
 *
 * <p>Uses Mockito to mock the {@link JavaMailSender} so no real SMTP connection
 * is required.  {@code @Value}-injected fields ({@code fromAddress}, {@code fromName})
 * are populated via {@link ReflectionTestUtils} since Mockito does not process
 * Spring annotations.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl Unit Tests")
class EmailServiceImplTest {

    /** Mocked Spring mail sender. */
    @Mock
    private JavaMailSender mailSender;

    /** Service under test with mocked dependencies auto-injected. */
    @InjectMocks
    private EmailServiceImpl emailService;

    /** Reusable MIME message instance returned by the mocked mail sender. */
    private MimeMessage mimeMessage;

    /**
     * Creates a bare {@link MimeMessage} and configures the mock mail sender
     * to return it.  Also injects the {@code fromAddress} and {@code fromName}
     * fields that are normally populated by Spring {@code @Value}.
     */
    @BeforeEach
    void setUp() {
        mimeMessage = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "E-Commerce Test");
    }

    /**
     * Verifies that {@code sendInvoiceEmail} decodes a Base64 PDF payload,
     * attaches it to the MIME message with the correct filename, and sends
     * the message via the mail sender.
     *
     * @throws Exception if MIME message inspection fails
     */
    @Test
    @DisplayName("sendInvoiceEmail: attaches decoded PDF and sends")
    void sendInvoiceEmail_attachesPdf() throws Exception {
        String base64 = java.util.Base64.getEncoder().encodeToString("PDFDATA".getBytes());
        emailService.sendInvoiceEmail("foo@bar.com", "order-1", base64);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).contains("Invoice for order order-1");

        Object content = sent.getContent();
        assertThat(content).isInstanceOf(jakarta.mail.Multipart.class);
        jakarta.mail.Multipart multipart = (jakarta.mail.Multipart) content;
        boolean found = false;
        for (int i = 0; i < multipart.getCount(); i++) {
            jakarta.mail.BodyPart part = multipart.getBodyPart(i);
            if (part.getFileName() != null && part.getFileName().contains("invoice-order-1")) {
                found = true;
                Object data = part.getDataHandler().getContent();
                byte[] bytes;
                if (data instanceof byte[]) {
                    bytes = (byte[]) data;
                } else if (data instanceof java.io.InputStream) {
                    bytes = ((java.io.InputStream) data).readAllBytes();
                } else {
                    throw new AssertionError("Unexpected attachment type: " + data.getClass());
                }
                assertThat(bytes).isNotEmpty();
            }
        }
        assertThat(found).isTrue();
    }
}
