package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.response.OrderResponse;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.PaymentStatus;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL =
            "http://localhost:8083/api/products/";

    @Override
    @Transactional
    public OrderResponse createOrder(
            CreateOrderRequest request,
            String role,
            String buyerUuid) {

        if (!"BUYER".equalsIgnoreCase(role)) {
            throw new RuntimeException("Only buyers can place orders");
        }

        // 1️⃣ Fetch product
        ProductDto product = restTemplate.getForObject(
                PRODUCT_SERVICE_URL + request.getProductUuid(),
                ProductDto.class
        );

        if (product == null) {
            throw new RuntimeException("Product not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new RuntimeException("Product is not active");
        }

        // 2️⃣ Call Product Service to reduce stock
        String reduceStockUrl =
                PRODUCT_SERVICE_URL +
                        "internal/reduce-stock/" +
                        request.getProductUuid() +
                        "?quantity=" +
                        request.getQuantity();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", "veryStrongInternalSecret123");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(
                reduceStockUrl,
                HttpMethod.PUT,
                entity,
                String.class
        );


        // 3️⃣ Calculate total
        double totalAmount =
                product.getPrice() * request.getQuantity();

        // 4️⃣ Create order
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

        log.info("Order created and stock deducted for product: {}",
                product.getUuid());

        return mapToResponse(order);
    }


    @Override
    public PageResponse<OrderResponse> listOrders(
            int page,
            int size,
            String role,
            String buyerUuid) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<Order> orderPage;

        if ("BUYER".equalsIgnoreCase(role)) {
            orderPage =
                    orderRepository.findByBuyerUuidAndIsDeletedFalse(
                            buyerUuid,
                            pageable);
        } else {
            orderPage =
                    orderRepository.findAll(pageable);
        }

        List<OrderResponse> responses =
                orderPage.getContent()
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

        return OrderResponse.builder()
                .uuid(order.getUuid())
                .buyerUuid(order.getBuyerUuid())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .build();
    }

    @Override
    public PageResponse<OrderResponse> listSellerOrders(
            int page,
            int size,
            String sellerUuid) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<Order> orderPage =
                orderRepository.findOrdersBySeller(
                        sellerUuid,
                        pageable);

        List<OrderResponse> responses =
                orderPage.getContent()
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

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(
            String uuid,
            String role,
            String buyerUuid,
            String newStatus) {

        Order order = orderRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        OrderStatus requestedStatus =
                OrderStatus.valueOf(newStatus.toUpperCase());

        // =========================
        // BUYER CANCEL LOGIC
        // =========================
        if ("BUYER".equalsIgnoreCase(role)) {

            if (!order.getBuyerUuid().equals(buyerUuid)) {
                throw new RuntimeException(
                        "You can only modify your own order");
            }

            if (requestedStatus != OrderStatus.CANCELLED) {
                throw new RuntimeException(
                        "Buyer can only cancel order");
            }

            if (order.getStatus() != OrderStatus.CREATED) {
                throw new RuntimeException(
                        "Order cannot be cancelled now");
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            return mapToResponse(order);
        }

        // =========================
        // ADMIN LIFECYCLE LOGIC
        // =========================
        if ("ADMIN".equalsIgnoreCase(role)) {

            switch (order.getStatus()) {

                case CREATED -> {
                    if (requestedStatus != OrderStatus.CONFIRMED)
                        throw new RuntimeException(
                                "Invalid transition");
                }

                case CONFIRMED -> {
                    if (requestedStatus != OrderStatus.SHIPPED)
                        throw new RuntimeException(
                                "Invalid transition");
                }

                case SHIPPED -> {
                    if (requestedStatus != OrderStatus.DELIVERED)
                        throw new RuntimeException(
                                "Invalid transition");
                }

                default -> throw new RuntimeException(
                        "Order cannot transition further");
            }

            order.setStatus(requestedStatus);
            orderRepository.save(order);

            return mapToResponse(order);
        }

        throw new RuntimeException("Unauthorized action");
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String uuid, String status) {

        Order order = orderRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        PaymentStatus paymentStatus =
                PaymentStatus.valueOf(status);

        order.setPaymentStatus(paymentStatus);

        if (paymentStatus == PaymentStatus.FAILED) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);
    }

}
