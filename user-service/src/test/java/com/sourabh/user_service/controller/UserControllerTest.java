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

/**
 * Unit tests for {@link UserController}.
 *
 * <p>Uses Mockito ({@code @ExtendWith(MockitoExtension.class)}) to isolate the
 * controller from the service layer. Only the internal invoice-forwarding
 * endpoint is covered here; higher-level integration tests cover the
 * remaining REST endpoints.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    /** Mocked user service (not exercised in current tests). */
    @Mock
    private UserService userService;

    /** Mocked email service used to verify invoice-email delegation. */
    @Mock
    private EmailService emailService;

    /** Controller under test with mocked dependencies auto-injected. */
    @InjectMocks
    private UserController controller;

    /**
     * Verifies that the internal invoice endpoint correctly delegates to
     * {@link EmailService#sendInvoiceEmail(String, String, String)} and
     * returns a success response envelope.
     */
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
        assertThat(resp.getBody().getMessage()).isEqualTo("sent");
        assertThat(resp.getBody().getData()).isNull();
    }
}
