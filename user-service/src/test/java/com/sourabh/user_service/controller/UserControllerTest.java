package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.ApiResponse;
import com.sourabh.user_service.dto.InvoiceEmailRequest;
import com.sourabh.user_service.service.EmailService;
import com.sourabh.user_service.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserController controller;

    @Test
    @DisplayName("sendInvoiceInternal should forward to EmailService and return success")
    void sendInvoiceInternal_sendsEmail() {
        InvoiceEmailRequest req = InvoiceEmailRequest.builder()
                .toEmail("buyer@example.com")
                .orderUuid("order-1")
                .pdfBase64("BASE64PDF")
                .build();

        ResponseEntity<ApiResponse<String>> resp = controller.sendInvoiceInternal(req);

        verify(emailService).sendInvoiceEmail("buyer@example.com", "order-1", "BASE64PDF");
        assertThat(resp.getBody()).isNotNull();
        // controller returns the message in the response envelope, data is null
        assertThat(resp.getBody().getMessage()).isEqualTo("sent");
        assertThat(resp.getBody().getData()).isNull();
    }
}
