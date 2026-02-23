package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.response.OrderItemResponse;
import com.sourabh.order_service.dto.response.OrderResponse;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.PaymentStatus;
import com.sourabh.order_service.exception.OrderAccessException;
import com.sourabh.order_service.exception.OrderNotFoundException;
import com.sourabh.order_service.exception.OrderStateException;
import com.sourabh.order_service.feign.ProductServiceClient;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.service.OrderService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String role, String buyerUuid) {

        if (!"BUYER".equalsIgnoreCase(role)) {
            throw new OrderAccessException("Only buyers can place orders");
        }

        ProductDto product = fetchProduct(request.getProductUuid());

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new OrderStateException("Product is not active");
        }

        try {
            productServiceClient.reduceStock(request.getProductUuid(), request.getQuantity());
        } catch (FeignException e) {
            throw new OrderStateException("Failed to reduce product stock: " + e.getMessage());
        }

        double totalAmount = product.getPrice() * request.getQuantity();

        Order order = Order.builder()
                .uuid(UUID.randomUUID().toString())
                .buyerUuid(buyerUuid)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .isDeleted(false)
                .build();

        OrderItem item = OrderItem.builder()
                .productUuid(product.getUuid())
                .sellerUuid(product.getSellerUuid())
                .price(product.getPrice())
                .quantity(request.getQuantity())
                .order(order)
                .build();

        order.setItems(List.of(item));
        orderRepository.save(order);

        log.info("Order created: orderUuid={}, productUuid={}", order.getUuid(), product.getUuid());
        return mapToResponse(order);
    }

    @Override
    public PageResponse<OrderResponse> listOrders(int page, int size, String role, String buyerUuid) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Order> orderPage = "BUYER".equalsIgnoreCase(role)
                ? orderRepository.findByBuyerUuidAndIsDeletedFalse(buyerUuid, pageable)
                : orderRepository.findByIsDeletedFalse(pageable);

        return toPageResponse(orderPage);
    }

    @Override
    public PageResponse<OrderResponse> listSellerOrders(int page, int size, String sellerUuid) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return toPageResponse(orderRepository.findOrdersBySeller(sellerUuid, pageable));
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", key = "#uuid")
    public OrderResponse updateOrderStatus(String uuid, String role, String buyerUuid, String newStatus) {

        Order order = orderRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + uuid));

        OrderStatus requestedStatus = OrderStatus.valueOf(newStatus.toUpperCase());

        if ("BUYER".equalsIgnoreCase(role)) {
            return cancelByBuyer(order, buyerUuid, requestedStatus);
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return advanceByAdmin(order, requestedStatus);
        }

        throw new OrderAccessException("Unauthorized action");
    }

    @Override
    @Cacheable(value = "orders", key = "#uuid")
    public OrderResponse getOrderByUuid(String uuid) {
        log.debug("Cache miss for order uuid={} — fetching from DB", uuid);
        return orderRepository.findByUuidAndIsDeletedFalse(uuid)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + uuid));
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", key = "#uuid")
    public void updatePaymentStatus(String uuid, String status) {
        Order order = orderRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + uuid));

        PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
        order.setPaymentStatus(paymentStatus);

        if (paymentStatus == PaymentStatus.FAILED) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);
        log.info("Payment status updated: orderUuid={}, paymentStatus={}", uuid, status);
    }

    // ─────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────

    private OrderResponse cancelByBuyer(Order order, String buyerUuid, OrderStatus requestedStatus) {
        if (!order.getBuyerUuid().equals(buyerUuid)) {
            throw new OrderAccessException("You can only modify your own order");
        }
        if (requestedStatus != OrderStatus.CANCELLED) {
            throw new OrderStateException("Buyer can only cancel an order");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new OrderStateException("Order cannot be cancelled at this stage");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled by buyer: uuid={}", order.getUuid());
        return mapToResponse(order);
    }

    private OrderResponse advanceByAdmin(Order order, OrderStatus requestedStatus) {
        OrderStatus current = order.getStatus();
        OrderStatus expected = switch (current) {
            case CREATED   -> OrderStatus.CONFIRMED;
            case CONFIRMED -> OrderStatus.SHIPPED;
            case SHIPPED   -> OrderStatus.DELIVERED;
            default -> throw new OrderStateException("Order cannot transition further from: " + current);
        };

        if (requestedStatus != expected) {
            throw new OrderStateException(
                    "Invalid transition from " + current + " to " + requestedStatus + ". Expected: " + expected);
        }

        order.setStatus(requestedStatus);
        orderRepository.save(order);
        log.info("Order status updated by admin: uuid={}, newStatus={}", order.getUuid(), requestedStatus);
        return mapToResponse(order);
    }

    private ProductDto fetchProduct(String productUuid) {
        try {
            return productServiceClient.getProduct(productUuid);
        } catch (FeignException.NotFound e) {
            throw new OrderStateException("Product not found: " + productUuid);
        }
    }

    private PageResponse<OrderResponse> toPageResponse(Page<Order> orderPage) {
        List<OrderResponse> responses = orderPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<OrderResponse>builder()
                .content(responses)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .productUuid(item.getProductUuid())
                                .sellerUuid(item.getSellerUuid())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build())
                        .toList();

        return OrderResponse.builder()
                .uuid(order.getUuid())
                .buyerUuid(order.getBuyerUuid())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .items(itemResponses)
                .build();
    }
}
