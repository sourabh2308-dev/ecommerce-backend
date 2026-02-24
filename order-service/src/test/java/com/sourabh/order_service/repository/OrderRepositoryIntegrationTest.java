package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("OrderRepository Integration Tests (Testcontainers)")
class OrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private OrderRepository orderRepository;

    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        order1 = Order.builder()
                .uuid("order-buyer1-a")
                .buyerUuid("buyer-1")
                .totalAmount(100.0)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .isDeleted(false)
                .build();

        OrderItem item = OrderItem.builder()
                .productUuid("prod-uuid")
                .sellerUuid("seller-1")
                .price(100.0)
                .quantity(1)
                .order(order1)
                .build();
        order1.setItems(List.of(item));

        order2 = Order.builder()
                .uuid("order-buyer2-a")
                .buyerUuid("buyer-2")
                .totalAmount(200.0)
                .status(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PENDING)
                .isDeleted(false)
                .build();
        order2.setItems(List.of());

        Order deletedOrder = Order.builder()
                .uuid("order-deleted")
                .buyerUuid("buyer-1")
                .totalAmount(50.0)
                .status(OrderStatus.CANCELLED)
                .paymentStatus(PaymentStatus.FAILED)
                .isDeleted(true)
                .build();
        deletedOrder.setItems(List.of());

        orderRepository.saveAll(List.of(order1, order2, deletedOrder));
    }

    @Test
    @DisplayName("findByUuidAndIsDeletedFalse: returns non-deleted order")
    void findByUuid_notDeleted_found() {
        Optional<Order> result = orderRepository.findByUuidAndIsDeletedFalse("order-buyer1-a");
        assertThat(result).isPresent();
        assertThat(result.get().getBuyerUuid()).isEqualTo("buyer-1");
    }

    @Test
    @DisplayName("findByUuidAndIsDeletedFalse: does not return soft-deleted order")
    void findByUuid_deleted_notFound() {
        Optional<Order> result = orderRepository.findByUuidAndIsDeletedFalse("order-deleted");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIsDeletedFalse: returns only non-deleted orders")
    void findByIsDeletedFalse_onlyActive() {
        Page<Order> page = orderRepository.findByIsDeletedFalse(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).noneMatch(Order::getIsDeleted);
    }

    @Test
    @DisplayName("findByBuyerUuidAndIsDeletedFalse: returns orders for specific buyer only")
    void findByBuyerUuid_returnsOnlyBuyersOrders() {
        Page<Order> page = orderRepository.findByBuyerUuidAndIsDeletedFalse("buyer-1", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getUuid()).isEqualTo("order-buyer1-a");
    }

    @Test
    @DisplayName("findOrdersBySeller: returns orders containing seller's items")
    void findOrdersBySeller_returnsMatchingOrders() {
        Page<Order> page = orderRepository.findOrdersBySeller("seller-1", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getUuid()).isEqualTo("order-buyer1-a");
    }

    @Test
    @DisplayName("findOrdersBySeller: returns empty for unknown seller")
    void findOrdersBySeller_unknownSeller_empty() {
        Page<Order> page = orderRepository.findOrdersBySeller("unknown-seller", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findByBuyerUuidAndIsDeletedFalse: excludes soft-deleted orders of same buyer")
    void findByBuyerUuid_excludesDeletedOrders() {
        Page<Order> page = orderRepository.findByBuyerUuidAndIsDeletedFalse("buyer-1", PageRequest.of(0, 10));
        // buyer-1 has order1 (active) and deletedOrder (soft-deleted), should only return 1
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByIsDeletedFalse: pagination works correctly")
    void findByIsDeletedFalse_pagination() {
        Page<Order> page1 = orderRepository.findByIsDeletedFalse(PageRequest.of(0, 1));
        assertThat(page1.getTotalElements()).isEqualTo(2);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByUuidAndIsDeletedFalse: exact UUID match required")
    void findByUuid_exactMatch() {
        Optional<Order> result1 = orderRepository.findByUuidAndIsDeletedFalse("order-buyer1-a");
        Optional<Order> result2 = orderRepository.findByUuidAndIsDeletedFalse("order-buyer1-b");

        assertThat(result1).isPresent();
        assertThat(result2).isEmpty();
    }

    @Test
    @DisplayName("findOrdersBySeller: seller with multiple orders")
    void findOrdersBySeller_multipleOrdersForSeller() {
        OrderItem item2 = OrderItem.builder()
                .productUuid("prod-uuid-2")
                .sellerUuid("seller-1")
                .price(50.0)
                .quantity(2)
                .order(order2)
                .build();
        order2.getItems().add(item2);
        orderRepository.save(order2);

        Page<Order> page = orderRepository.findOrdersBySeller("seller-1", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("order with items persisted correctly")
    void orderWithItems_persistedCorrectly() {
        Optional<Order> foundOrder = orderRepository.findByUuidAndIsDeletedFalse("order-buyer1-a");
        
        assertThat(foundOrder).isPresent();
        Order order = foundOrder.get();
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getProductUuid()).isEqualTo("prod-uuid");
    }
}
