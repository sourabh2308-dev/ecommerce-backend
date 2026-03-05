package com.sourabh.order_service.service;

import com.sourabh.order_service.dto.InternalUserDto;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.feign.UserServiceClient;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.service.impl.InvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InvoiceServiceImpl} covering the {@code emailInvoice}
 * workflow.
 *
 * <p>Uses Mockito to stub {@link OrderRepository} and {@link UserServiceClient}
 * so that tests run without database or network dependencies.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceServiceImpl Unit Tests")
class InvoiceServiceImplTest {

    /** Mocked order repository used to supply test order data. */
    @Mock
    private OrderRepository orderRepository;

    /** Mocked Feign client for user-service calls (email lookup, invoice dispatch). */
    @Mock
    private UserServiceClient userServiceClient;

    /** Service under test with mocks auto-injected by Mockito. */
    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    /** Reusable sample order initialised before each test. */
    private Order sampleOrder;

    /** Builds a minimal order fixture for use across test methods. */
    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .uuid("order-123")
                .buyerUuid("buyer-xyz")
                .build();
    }

    /**
     * Happy-path test: verifies that {@code emailInvoice} fetches the order,
     * generates a PDF (stubbed), resolves the buyer email, and dispatches
     * the invoice through the user-service Feign client.
     */
    @Test
    @DisplayName("emailInvoice: should fetch order, generate PDF and call user client")
    void emailInvoice_success() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-123"))
                .thenReturn(Optional.of(sampleOrder));
        when(userServiceClient.getUserByUuid("buyer-xyz"))
                .thenReturn(InternalUserDto.builder().uuid("buyer-xyz").email("foo@bar.com").build());

        InvoiceServiceImpl spy = spy(invoiceService);
        doReturn("dummy".getBytes()).when(spy).generateInvoice("order-123");

        spy.emailInvoice("order-123");

        verify(userServiceClient).sendInvoice(any());
    }

    /**
     * Failure-path test: confirms that a {@link RuntimeException} is thrown
     * when the requested order does not exist in the repository.
     */
    @Test
    @DisplayName("emailInvoice: missing order throws runtime exception")
    void emailInvoice_noOrder_throws() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-123"))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> invoiceService.emailInvoice("order-123"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Order not found");
    }
}
