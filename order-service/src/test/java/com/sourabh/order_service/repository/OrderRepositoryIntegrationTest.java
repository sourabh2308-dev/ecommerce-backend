package com.sourabh.order_service.repository;

import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OrderRepository} running against a real
 * PostgreSQL 15 instance managed by Testcontainers.
 *
 * <p>Verifies custom query methods including soft-delete filtering,
 * buyer-scoped lookups, seller-based queries, and pagination behaviour.
 * {@code @DataJpaTest} restricts the context to JPA components only,
 * keeping test startup fast.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Disabled("Testcontainers incompatible with Docker 29+ (API v1.44 minimum)")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=update"
})
@DisplayName("OrderRepository Integration Tests (Testcontainers)")
class OrderRepositoryIntegrationTest {

    /** Disposable PostgreSQL container auto-wired into the datasource. */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    /** JPA repository under test. */
    @Autowired
    private OrderRepository orderRepository;

    /** Active order owned by buyer-1 (contains one item from seller-1). */
    private Order order1;

    /** Active order owned by buyer-2 (initially has no items). */
    private Order order2;

    /**
     * Seeds the database with two active orders and one soft-deleted order
     * before every test, ensuring a clean and predictable state.
     */
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
        order2.setItems(new java.util.ArrayList<>());

        Order deletedOrder = Order.builder()
                .uuid("order-deleted")
                .buyerUuid("buyer-1")
                .totalAmount(50.0)
                .status(OrderStatus.CANCELLED)
                .paymentStatus(PaymentStatus.FAILED)
                .isDeleted(true)
                .build();
        deletedOrder.setItems(new java.util.ArrayList<>());

        orderRepository.saveAll(List.of(order1, order2, deletedOrder));
    }

    /** Ensures a non-deleted order is returned when queried by UUID. */
    @Test
    @DisplayName("findByUuidAndIsDeletedFalse: returns non-deleted order")
    void findByUuid_notDeleted_found() {
        Optional<Order> result = orderRepository.findByUuidAndIsDeletedFalse("order-buyer1-a");
        assertThat(result).isPresent();
        assertThat(result.get().getBuyerUuid()).isEqualTo("buyer-1");
    }

    /** Confirms that a soft-deleted order is excluded from UUID lookups. */
    @Test
    @DisplayName("findByUuidAndIsDeletedFalse: does not return soft-deleted order")
    void findByUuid_deleted_notFound() {
        Optional<Order> result = orderRepository.findByUuidAndIsDeletedFalse("order-deleted");
        assertThat(result).isEmpty();
    }

    /** Verifies the global non-deleted listing excludes soft-deleted rows. */
    @Test
    @DisplayName("findByIsDeletedFalse: returns only non-deleted orders")
    void findByIsDeletedFalse_onlyActive() {
        Page<Order> page = orderRepository.findByIsDeletedFalse(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).noneMatch(Order::getIsDeleted);
    }

    /** Checks buyer-scoped query returns only that buyer’s active orders. */
    @Test
    @DisplayName("findByBuyerUuidAndIsDeletedFalse: returns orders for specific buyer only")
    void findByBuyerUuid_returnsOnlyBuyersOrders() {
        Page<Order> page = orderRepository.findByBuyerUuidAndIsDeletedFalse("buyer-1", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getUuid()).isEqualTo("order-buyer1-a");
    }

    /** Validates seller-based query returns orders containing the seller’s items. */
    @Test
    @DisplayName("findOrdersBySeller: returns orders containing seller's items")
    void findOrdersBySeller_returnsMatchingOrders() {
        Page<Order> page = orderRepository.findOrdersBySeller("seller-1", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getUuid()).isEqualTo("order-buyer1-a");
    }

    /** Ensures an unknown seller UUID yields an empty result set. */
    @Test
    @DisplayName("findOrdersBySeller: returns empty for unknown seller")
    void findOrdersBySeller_unknownSeller_empty() {
        Page<Order> page = orderRepository.findOrdersBySeller("unknown-seller", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isZero();
    }

    /** Confirms soft-deleted orders of the same buyer are excluded. */
    @Test
    @DisplayName("findByBuyerUuidAndIsDeletedFalse: excludes soft-deleted orders of same buyer")
    void findByBuyerUuid_excludesDeletedOrders() {
        Page<Order> page = orderRepository.findByBuyerUuidAndIsDeletedFalse("buyer-1", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    /** Tests that pagination metadata (total pages, size) is computed correctly. */
    @Test
    @DisplayName("findByIsDeletedFalse: pagination works correctly")
    void findByIsDeletedFalse_pagination() {
        Page<Order> page1 = orderRepository.findByIsDeletedFalse(PageRequest.of(0, 1));
        assertThat(page1.getTotalElements()).isEqualTo(2);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getSize()).isEqualTo(1);
    }

    /** Validates that UUID matching is exact (partial matches do not return results). */
    @Test
    @DisplayName("findByUuidAndIsDeletedFalse: exact UUID match required")
    void findByUuid_exactMatch() {
        Optional<Order> result1 = orderRepository.findByUuidAndIsDeletedFalse("order-buyer1-a");
        Optional<Order> result2 = orderRepository.findByUuidAndIsDeletedFalse("order-buyer1-b");

        assertThat(result1).isPresent();
        assertThat(result2).isEmpty();
    }

    /** Verifies seller query behaviour when a seller has items in multiple orders. */
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

    /** Ensures order items are persisted and eagerly loaded correctly. */
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
