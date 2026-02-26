package com.sourabh.order_service.service;

import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.request.OrderItemRequest;
import com.sourabh.order_service.dto.response.OrderResponse;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.PaymentStatus;
import com.sourabh.order_service.exception.OrderAccessException;
import com.sourabh.order_service.exception.OrderNotFoundException;
import com.sourabh.order_service.exception.OrderStateException;
import com.sourabh.order_service.feign.ProductServiceClient;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductServiceClient productServiceClient;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private com.sourabh.order_service.service.OrderSplitterService orderSplitterService;
    @Mock private com.sourabh.order_service.kafka.OrderNotificationPublisher notificationPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private ProductDto activeProduct;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        activeProduct = new ProductDto();
        activeProduct.setUuid("prod-uuid");
        activeProduct.setStatus("ACTIVE");
        activeProduct.setPrice(100.0);
        activeProduct.setSellerUuid("seller-uuid");

        sampleOrder = Order.builder()
                .uuid("order-uuid")
                .buyerUuid("buyer-uuid")
                .totalAmount(100.0)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .isDeleted(false)
                .items(List.of())
                .build();
    }

    // ──────────────────────────────────────────────────────
    // createOrder
    // ──────────────────────────────────────────────────────

    private OrderItemRequest makeItem(String productUuid, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductUuid(productUuid);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    @DisplayName("createOrder: success — buyer creates valid order")
    void createOrder_success() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItems(List.of(makeItem("prod-uuid", 2)));

        when(productServiceClient.getProduct("prod-uuid")).thenReturn(activeProduct);
        when(orderRepository.save(any())).thenReturn(sampleOrder);

        OrderResponse response = orderService.createOrder(req, "BUYER", "buyer-uuid");

        assertThat(response).isNotNull();
        verify(productServiceClient).reduceStock("prod-uuid", 2);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: fails — non-buyer role rejected")
    void createOrder_nonBuyer_throwsOrderAccessException() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItems(List.of(makeItem("prod-uuid", 1)));

        assertThatThrownBy(() -> orderService.createOrder(req, "SELLER", "seller-uuid"))
                .isInstanceOf(OrderAccessException.class)
                .hasMessageContaining("Only buyers");

        verifyNoInteractions(productServiceClient, orderRepository);
    }

    @Test
    @DisplayName("createOrder: fails — product is not ACTIVE")
    void createOrder_inactiveProduct_throwsOrderStateException() {
        activeProduct.setStatus("INACTIVE");
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItems(List.of(makeItem("prod-uuid", 1)));

        when(productServiceClient.getProduct("prod-uuid")).thenReturn(activeProduct);

        assertThatThrownBy(() -> orderService.createOrder(req, "BUYER", "buyer-uuid"))
                .isInstanceOf(OrderStateException.class)
                .hasMessageContaining("not active");

        verify(orderRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────
    // getOrderByUuid
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderByUuid: returns order when found")
    void getOrderByUuid_found() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));

        OrderResponse response = orderService.getOrderByUuid("order-uuid", "ADMIN", "admin-uuid");

        assertThat(response.getUuid()).isEqualTo("order-uuid");
    }

    @Test
    @DisplayName("getOrderByUuid: throws when not found")
    void getOrderByUuid_notFound_throwsOrderNotFoundException() {
        when(orderRepository.findByUuidAndIsDeletedFalse("bad-uuid"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByUuid("bad-uuid", "ADMIN", "admin-uuid"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // ──────────────────────────────────────────────────────
    // updateOrderStatus — buyer cancels
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOrderStatus: buyer can cancel a CREATED order")
    void updateStatus_buyerCancels_success() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);

        OrderResponse response = orderService.updateOrderStatus("order-uuid", "BUYER", "buyer-uuid", "CANCELLED", null, null);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("updateOrderStatus: buyer cannot cancel another buyer's order")
    void updateStatus_buyerWrongOwner_throwsOrderAccessException() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus("order-uuid", "BUYER", "other-buyer", "CANCELLED", null, null))
                .isInstanceOf(OrderAccessException.class)
                .hasMessageContaining("your own order");
    }

    @Test
    @DisplayName("updateOrderStatus: buyer can only use CANCELLED — not SHIPPED")
    void updateStatus_buyerSetShipped_throwsOrderStateException() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus("order-uuid", "BUYER", "buyer-uuid", "SHIPPED", null, null))
                .isInstanceOf(OrderStateException.class)
                .hasMessageContaining("only cancel or request return");
    }

    // ──────────────────────────────────────────────────────
    // updateOrderStatus — admin advances
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOrderStatus: admin advances CREATED → CONFIRMED")
    void updateStatus_adminConfirms_success() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);

        OrderResponse response = orderService.updateOrderStatus("order-uuid", "ADMIN", null, "CONFIRMED", null, null);

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("updateOrderStatus: admin invalid transition CREATED → SHIPPED throws")
    void updateStatus_adminInvalidTransition_throwsOrderStateException() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus("order-uuid", "ADMIN", null, "SHIPPED", null, null))
                .isInstanceOf(OrderStateException.class)
                .hasMessageContaining("Invalid transition");
    }

    // ──────────────────────────────────────────────────────
    // updatePaymentStatus
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePaymentStatus: FAILED payment cancels order")
    void updatePaymentStatus_failed_cancelsOrder() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);

        orderService.updatePaymentStatus("order-uuid", "FAILED");

        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(sampleOrder.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("updatePaymentStatus: SUCCESS does not cancel order")
    void updatePaymentStatus_success_doesNotCancelOrder() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenReturn(sampleOrder);

        orderService.updatePaymentStatus("order-uuid", "SUCCESS");

        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(sampleOrder.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    // Additional edge case tests

    @Test
    @DisplayName("createOrder: multiple items with different sellers")
    void createOrder_multipleSellerItems() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItems(List.of(
                makeItem("prod-uuid-1", 2),
                makeItem("prod-uuid-2", 1)
        ));

        ProductDto product1 = new ProductDto();
        product1.setUuid("prod-uuid-1");
        product1.setStatus("ACTIVE");
        product1.setPrice(100.0);
        product1.setSellerUuid("seller-1");

        ProductDto product2 = new ProductDto();
        product2.setUuid("prod-uuid-2");
        product2.setStatus("ACTIVE");
        product2.setPrice(50.0);
        product2.setSellerUuid("seller-2");

        when(productServiceClient.getProduct("prod-uuid-1")).thenReturn(product1);
        when(productServiceClient.getProduct("prod-uuid-2")).thenReturn(product2);
        when(orderRepository.save(any())).thenReturn(sampleOrder);

        OrderResponse response = orderService.createOrder(req, "BUYER", "buyer-uuid");

        assertThat(response).isNotNull();
        verify(productServiceClient).reduceStock("prod-uuid-1", 2);
        verify(productServiceClient).reduceStock("prod-uuid-2", 1);
    }

    @Test
    @DisplayName("createOrder: empty items list fails")
    void createOrder_emptyItems_throwsException() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItems(List.of());

        assertThatThrownBy(() -> orderService.createOrder(req, "BUYER", "buyer-uuid"))
                .isInstanceOf(Exception.class);

        verifyNoInteractions(productServiceClient, orderRepository);
    }

    @Test
    @DisplayName("getOrderByUuid: returns order details correctly mapped")
    void getOrderByUuid_mapsAllFields() {
        when(orderRepository.findByUuidAndIsDeletedFalse("order-uuid"))
                .thenReturn(Optional.of(sampleOrder));

        OrderResponse response = orderService.getOrderByUuid("order-uuid", "BUYER", "buyer-uuid");

        assertThat(response.getUuid()).isEqualTo("order-uuid");
        assertThat(response.getTotalAmount()).isEqualTo(100.0);
        assertThat(response.getStatus()).isEqualTo("CREATED");
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("updatePaymentStatus: order not found throws exception")
    void updatePaymentStatus_orderNotFound() {
        when(orderRepository.findByUuidAndIsDeletedFalse("bad-uuid"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updatePaymentStatus("bad-uuid", "SUCCESS"))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
