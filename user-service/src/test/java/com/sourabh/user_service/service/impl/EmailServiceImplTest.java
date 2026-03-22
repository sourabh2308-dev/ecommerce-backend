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

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl Unit Tests")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "E-Commerce Test");
    }

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
