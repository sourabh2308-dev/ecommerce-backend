package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.request.OrderItemRequest;
import com.sourabh.order_service.dto.response.OrderItemResponse;
import com.sourabh.order_service.dto.response.OrderResponse;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.PaymentStatus;
import com.sourabh.order_service.entity.ReturnType;
import com.sourabh.order_service.exception.OrderAccessException;
import com.sourabh.order_service.exception.OrderNotFoundException;
import com.sourabh.order_service.exception.OrderStateException;
import com.sourabh.order_service.feign.ProductServiceClient;
import com.sourabh.order_service.kafka.OrderNotificationPublisher;
import com.sourabh.order_service.kafka.event.OrderCreatedEvent;
import com.sourabh.order_service.kafka.event.OrderItemEvent;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ORDER SERVICE IMPLEMENTATION - Core Order Processing Logic
 *
 * Orchestrates complete order lifecycle: creation, validation, status transitions, and payment saga.
 * Uses Feign for sync product-service calls, Kafka for async payment events.
 *
 * SAGA PATTERN:
 * 1. createOrder: Validate + persist + publish order.created event
 * 2. payment-service: Consumes event, processes payment, publishes payment.completed
 * 3. updatePaymentStatus: Receives event, updates status + compensates if needed
 *
 * COMPENSATION:
 * If payment fails, cancels order and restores product stock via productServiceClient
 */
// Service Implementation - Contains business logic and data operations
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderNotificationPublisher notificationPublisher;
    private final com.sourabh.order_service.service.OrderSplitterService orderSplitterService;

    private static final String TOPIC_ORDER_CREATED = "order.created";

    /**
     * Creates new order with items and shipping details, triggers payment saga.
     *
     * Steps:
     * 1. Validate buyer role (only BUYER can create)
     * 2. Fetch all products, validate ACTIVE status
     * 3. Reduce stock for all products (with compensation on failure)
     * 4. Persist Order with OrderItems to DB
     * 5. Publish order.created Kafka event (triggers payment-service)
     * 6. Return order response with paymentStatus = PENDING
     *
     * Compensation: If stock reduction fails on item N, restores stock for items 0..N-1
     *
     * Exceptions: OrderAccessException (not BUYER), OrderStateException (product issues)
     */
    @Override
    @Transactional
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public OrderResponse createOrder(CreateOrderRequest request, String role, String buyerUuid) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderStateException("Order must contain at least one item");
        }

        if (!"BUYER".equalsIgnoreCase(role)) {
            throw new OrderAccessException("Only buyers can place orders");
        }

        // Fetch all products and validate
        List<ProductDto> products = new java.util.ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            ProductDto product = fetchProduct(itemReq.getProductUuid());
            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new OrderStateException("Product is not active: " + itemReq.getProductUuid());
            }
            products.add(product);
        }

        // Reduce stock for all products
        List<String> reducedProductUuids = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < request.getItems().size(); i++) {
                OrderItemRequest itemReq = request.getItems().get(i);
                productServiceClient.reduceStock(itemReq.getProductUuid(), itemReq.getQuantity());
                reducedProductUuids.add(itemReq.getProductUuid());
            }
        } catch (FeignException e) {
            // Compensate: restore stock for already-reduced products
            for (int j = 0; j < reducedProductUuids.size(); j++) {
                try {
                    productServiceClient.restoreStock(
                            reducedProductUuids.get(j),
                            request.getItems().get(j).getQuantity());
                } catch (Exception ex) {
                    log.error("Failed to restore stock during compensation: productUuid={}",
                            reducedProductUuids.get(j), ex);
                }
            }
            throw new OrderStateException("Failed to reduce product stock: " + e.getMessage());
        }

        // Calculate total and build order items
        double totalAmount = 0;
        List<OrderItem> orderItems = new java.util.ArrayList<>();
        List<OrderItemEvent> itemEvents = new java.util.ArrayList<>();

        Order order = Order.builder()
                .uuid(UUID.randomUUID().toString())
                .buyerUuid(buyerUuid)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingName(request.getShippingName())
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingState(request.getShippingState())
                .shippingPincode(request.getShippingPincode())
                .shippingPhone(request.getShippingPhone())
                .build();

        for (int i = 0; i < request.getItems().size(); i++) {
            OrderItemRequest itemReq = request.getItems().get(i);
            ProductDto product = products.get(i);
            double itemTotal = product.getPrice() * itemReq.getQuantity();
            totalAmount += itemTotal;

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productUuid(itemReq.getProductUuid())
                    .productName(product.getName())
                    .productCategory(product.getCategory())
                    .productImageUrl(product.getImageUrl())
                    .sellerUuid(product.getSellerUuid())
                    .price(product.getPrice())
                    .quantity(itemReq.getQuantity())
                    .build();
            orderItems.add(orderItem);

            OrderItemEvent itemEvent = new OrderItemEvent();
            itemEvent.setProductUuid(itemReq.getProductUuid());
            itemEvent.setSellerUuid(product.getSellerUuid());
            itemEvent.setPrice(product.getPrice());
            itemEvent.setQuantity(itemReq.getQuantity());
            itemEvent.setSubtotal(itemTotal);
            itemEvents.add(itemEvent);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // Check if order contains items from multiple sellers and split if needed
        if (orderSplitterService.requiresSplitting(savedOrder)) {
            List<Order> subOrders = orderSplitterService.splitOrderBySeller(savedOrder);
            log.info("Order {} split into {} sub-orders for independent seller fulfillment", 
                    savedOrder.getUuid(), subOrders.size());
        }

        // Publish event for payment-service
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getUuid(),
                savedOrder.getBuyerUuid(),
                itemEvents,
                savedOrder.getTotalAmount());
        kafkaTemplate.send(TOPIC_ORDER_CREATED, savedOrder.getBuyerUuid(), event);

        log.info("Order created: uuid={}, buyerUuid={}, items={}", savedOrder.getUuid(), buyerUuid, orderItems.size());
        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listOrders(int page, int size, String role, String buyerUuid) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Order> orderPage = "ADMIN".equalsIgnoreCase(role)
                ? orderRepository.findByIsDeletedFalse(pageable)
                : orderRepository.findByBuyerUuidAndIsDeletedFalse(buyerUuid, pageable);

        return toPageResponse(orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listSellerOrders(int page, int size, String sellerUuid) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findOrdersBySeller(sellerUuid, pageable);
        return toPageResponse(orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#uuid")
    public OrderResponse getOrderByUuid(String uuid, String role, String userUuid) {
        Order order = orderRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + uuid));

        // Authorization: buyers see only their orders
        if ("BUYER".equalsIgnoreCase(role) && !order.getBuyerUuid().equals(userUuid)) {
            throw new OrderAccessException("You can only view your own orders");
        }

        // Sellers see orders containing their items
        if ("SELLER".equalsIgnoreCase(role)) {
            boolean hasItems = order.getItems() != null && order.getItems().stream()
                    .anyMatch(item -> userUuid.equals(item.getSellerUuid()));
            if (!hasItems) {
                throw new OrderAccessException("You have no items in this order");
            }
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", key = "#uuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
        public OrderResponse updateOrderStatus(
            String uuid,
            String role,
            String userUuid,
            String newStatusStr,
            String returnType,
            String returnReason) {
        Order order = orderRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + uuid));

        OrderStatus requestedStatus;
        try {
            requestedStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OrderStateException("Invalid order status: " + newStatusStr);
        }

        OrderResponse response = switch (role.toUpperCase()) {
            case "BUYER" -> handleBuyerStatusChange(order, userUuid, requestedStatus, returnType, returnReason);
            case "ADMIN" -> advanceByAdmin(order, requestedStatus);
            case "SELLER" -> advanceBySeller(order, userUuid, requestedStatus);
            default -> throw new OrderAccessException("Unauthorized role: " + role);
        };

        return response;
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", key = "#uuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
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

    // Private helper methods

    /**
     * CANCELBYBUYER - Method Documentation
     *
     * PURPOSE:
     * This method handles the cancelByBuyer operation.
     *
     * PARAMETERS:
     * @param order - Order value
     * @param buyerUuid - String value
     * @param requestedStatus - OrderStatus value
     *
     * RETURN VALUE:
     * @return OrderResponse - Result of the operation
     *
     */
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

        // Saga compensation: restore stock for all items
        if (order.getItems() != null) {
            order.getItems().forEach(item -> {
                try {
                    productServiceClient.restoreStock(item.getProductUuid(), item.getQuantity());
                    log.info("Stock restored for cancelled order: productUuid={}, qty={}",
                            item.getProductUuid(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to restore stock for productUuid={}: {}",
                            item.getProductUuid(), e.getMessage());
                }
            });
        }

        log.info("Order cancelled by buyer: uuid={}", order.getUuid());
        return mapToResponse(order);
    }

    private OrderResponse handleBuyerStatusChange(
            Order order,
            String buyerUuid,
            OrderStatus requestedStatus,
            String returnType,
            String returnReason) {

        if (requestedStatus == OrderStatus.CANCELLED) {
            return cancelByBuyer(order, buyerUuid, requestedStatus);
        }

        if (requestedStatus == OrderStatus.RETURN_REQUESTED) {
            return requestReturnByBuyer(order, buyerUuid, returnType, returnReason);
        }

        throw new OrderStateException("Buyer can only cancel or request return");
    }

    private OrderResponse requestReturnByBuyer(
            Order order,
            String buyerUuid,
            String returnTypeStr,
            String returnReason) {

        if (!order.getBuyerUuid().equals(buyerUuid)) {
            throw new OrderAccessException("You can only modify your own order");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new OrderStateException("Return can only be requested after delivery");
        }

        if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new OrderStateException("Return request is only allowed for successful payments");
        }

        if (returnTypeStr == null || returnTypeStr.isBlank()) {
            throw new OrderStateException("Return type is required (REFUND or EXCHANGE)");
        }

        ReturnType parsedReturnType;
        try {
            parsedReturnType = ReturnType.valueOf(returnTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OrderStateException("Invalid return type: " + returnTypeStr);
        }

        if (returnReason == null || returnReason.isBlank()) {
            throw new OrderStateException("Return reason is required");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnType(parsedReturnType);
        order.setReturnReason(returnReason.trim());
        orderRepository.save(order);

        log.info("Return requested by buyer: orderUuid={}, returnType={}", order.getUuid(), parsedReturnType);
        return mapToResponse(order);
    }

    /**
     * ADVANCEBYADMIN - Method Documentation
     *
     * PURPOSE:
     * This method handles the advanceByAdmin operation.
     *
     * PARAMETERS:
     * @param order - Order value
     * @param requestedStatus - OrderStatus value
     *
     * RETURN VALUE:
     * @return OrderResponse - Result of the operation
     *
     */
    private OrderResponse advanceByAdmin(Order order, OrderStatus requestedStatus) {
        List<OrderStatus> allowed = allowedTransitionsFor(order);

        if (!allowed.contains(requestedStatus)) {
            throw new OrderStateException(
                    "Invalid transition from " + order.getStatus() + " to " + requestedStatus + ". Allowed: " + allowed);
        }

        String oldStatus = order.getStatus().name();
        order.setStatus(requestedStatus);
        if (requestedStatus == OrderStatus.REFUND_ISSUED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        orderRepository.save(order);
        notificationPublisher.publishStatusChange(order.getUuid(), order.getBuyerUuid(),
                oldStatus, requestedStatus.name(), order.getTotalAmount(), order.getCurrency());
        log.info("Order status updated by admin: uuid={}, newStatus={}", order.getUuid(), requestedStatus);
        return mapToResponse(order);
    }

    /**
     * ADVANCEBYSELLER - Method Documentation
     *
     * PURPOSE:
     * This method handles the advanceBySeller operation.
     *
     * PARAMETERS:
     * @param order - Order value
     * @param sellerUuid - String value
     * @param requestedStatus - OrderStatus value
     *
     * RETURN VALUE:
     * @return OrderResponse - Result of the operation
     *
     */
    private OrderResponse advanceBySeller(Order order, String sellerUuid, OrderStatus requestedStatus) {
        // Verify this seller has items in the order
        boolean hasItems = order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> sellerUuid.equals(item.getSellerUuid()));
        if (!hasItems) {
            throw new OrderAccessException("You have no items in this order");
        }

        List<OrderStatus> allowed = allowedTransitionsFor(order);

        if (!allowed.contains(requestedStatus)) {
            throw new OrderStateException(
                    "Invalid transition from " + order.getStatus() + " to " + requestedStatus + ". Allowed: " + allowed);
        }

        String oldStatus = order.getStatus().name();
        order.setStatus(requestedStatus);
        if (requestedStatus == OrderStatus.REFUND_ISSUED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        orderRepository.save(order);
        notificationPublisher.publishStatusChange(order.getUuid(), order.getBuyerUuid(),
                oldStatus, requestedStatus.name(), order.getTotalAmount(), order.getCurrency());
        log.info("Order status updated by seller: uuid={}, sellerUuid={}, newStatus={}",
                order.getUuid(), sellerUuid, requestedStatus);
        return mapToResponse(order);
    }

    private List<OrderStatus> allowedTransitionsFor(Order order) {
        return switch (order.getStatus()) {
            case CREATED -> List.of(OrderStatus.CONFIRMED);
            case CONFIRMED -> List.of(OrderStatus.SHIPPED);
            case SHIPPED -> List.of(OrderStatus.DELIVERED);
            case RETURN_REQUESTED -> List.of(OrderStatus.PICKUP_SCHEDULED, OrderStatus.RETURN_REJECTED);
            case PICKUP_SCHEDULED -> List.of(OrderStatus.PICKED_UP);
            case PICKED_UP -> List.of(OrderStatus.RETURN_RECEIVED);
            case RETURN_RECEIVED -> {
                if (order.getReturnType() == null) {
                    throw new OrderStateException("Return type is missing for return workflow");
                }
                yield order.getReturnType() == ReturnType.EXCHANGE
                        ? List.of(OrderStatus.EXCHANGE_ISSUED)
                        : List.of(OrderStatus.REFUND_ISSUED);
            }
            default -> List.of();
        };
    }

    /**
     * FETCHPRODUCT - Method Documentation
     *
     * PURPOSE:
     * This method handles the fetchProduct operation.
     *
     * PARAMETERS:
     * @param productUuid - String value
     *
     * RETURN VALUE:
     * @return ProductDto - Result of the operation
     *
     */
    private ProductDto fetchProduct(String productUuid) {
        try {
            return productServiceClient.getProduct(productUuid);
        } catch (FeignException.NotFound e) {
            throw new OrderStateException("Product not found: " + productUuid);
        }
    }

    /**
     * TOPAGERESPONSE - Method Documentation
     *
     * PURPOSE:
     * This method handles the toPageResponse operation.
     *
     * PARAMETERS:
     * @param orderPage - Page<Order> value
     *
     * RETURN VALUE:
     * @return PageResponse<OrderResponse> - Result of the operation
     *
     */
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

    /**
     * MAPTORESPONSE - Method Documentation
     *
     * PURPOSE:
     * This method handles the mapToResponse operation.
     *
     * PARAMETERS:
     * @param order - Order value
     *
     * RETURN VALUE:
     * @return OrderResponse - Result of the operation
     *
     */
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .productUuid(item.getProductUuid())
                            .productName(item.getProductName())
                            .productCategory(item.getProductCategory())
                            .productImageUrl(item.getProductImageUrl())
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
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .parentOrderUuid(order.getParentOrderUuid())
                .orderGroupId(order.getOrderGroupId())
                .shippingName(order.getShippingName())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingPincode(order.getShippingPincode())
                .shippingPhone(order.getShippingPhone())
            .returnType(order.getReturnType() == null ? null : order.getReturnType().name())
            .returnReason(order.getReturnReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getSubOrders(String parentOrderUuid, String role, String userUuid) {
        // First verify parent order exists and user has access
        Order parentOrder = orderRepository.findByUuidAndIsDeletedFalse(parentOrderUuid)
                .orElseThrow(() -> new OrderNotFoundException("Parent order not found: " + parentOrderUuid));

        // Verify access rights
        if ("BUYER".equalsIgnoreCase(role) && !parentOrder.getBuyerUuid().equals(userUuid)) {
            throw new OrderAccessException("Access denied to this order");
        }

        List<Order> subOrders = orderRepository.findByParentOrderUuidAndIsDeletedFalse(parentOrderUuid);
        return subOrders.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderGroup(String orderGroupId, String role, String userUuid) {
        List<Order> groupOrders = orderRepository.findByOrderGroupIdAndIsDeletedFalse(orderGroupId);

        if (groupOrders.isEmpty()) {
            throw new OrderNotFoundException("No orders found for group: " + orderGroupId);
        }

        // Verify access rights (check any order in the group)
        Order sampleOrder = groupOrders.get(0);
        if ("BUYER".equalsIgnoreCase(role) && !sampleOrder.getBuyerUuid().equals(userUuid)) {
            throw new OrderAccessException("Access denied to this order group");
        }

        return groupOrders.stream()
                .map(this::mapToResponse)
                .toList();
    }
}
