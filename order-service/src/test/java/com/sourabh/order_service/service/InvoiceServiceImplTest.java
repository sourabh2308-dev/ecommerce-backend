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

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceServiceImpl Unit Tests")
class InvoiceServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .uuid("order-123")
                .buyerUuid("buyer-xyz")
                .build();
    }

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
