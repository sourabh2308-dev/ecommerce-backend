package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.request.OrderItemRequest;
import com.sourabh.order_service.dto.response.OrderItemResponse;
import com.sourabh.order_service.dto.response.OrderResponse;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderEventOutbox;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.PaymentStatus;
import com.sourabh.order_service.entity.ReturnType;
import com.sourabh.order_service.exception.OrderAccessException;
import com.sourabh.order_service.exception.OrderNotFoundException;
import com.sourabh.order_service.exception.OrderStateException;
import com.sourabh.order_service.feign.ProductServiceClient;
import com.sourabh.order_service.kafka.OrderNotificationPublisher;
import com.sourabh.order_service.kafka.event.OrderCreatedOutboxRequestedEvent;
import com.sourabh.order_service.kafka.event.OrderCreatedEvent;
import com.sourabh.order_service.repository.OrderEventOutboxRepository;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.service.OrderService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
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
    
    private final OrderEventOutboxRepository orderEventOutboxRepository;
    
    private final ProductServiceClient productServiceClient;
    
    private final OrderNotificationPublisher notificationPublisher;
    
    private final com.sourabh.order_service.service.OrderSplitterService orderSplitterService;
    
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String role, String buyerUuid) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderStateException("Order must contain at least one item");
        }

        if (!"BUYER".equalsIgnoreCase(role)) {
            throw new OrderAccessException("Only buyers can place orders");
        }

        List<ProductDto> products = new java.util.ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            ProductDto product = fetchProduct(itemReq.getProductUuid());
            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new OrderStateException("Product is not active: " + itemReq.getProductUuid());
            }
            products.add(product);
        }

        double totalAmount = 0;
        List<OrderItem> orderItems = new java.util.ArrayList<>();

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
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        List<String> reducedProductUuids = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < request.getItems().size(); i++) {
                OrderItemRequest itemReq = request.getItems().get(i);
                productServiceClient.reduceStock(itemReq.getProductUuid(), itemReq.getQuantity());
                reducedProductUuids.add(itemReq.getProductUuid());
            }

            if (orderSplitterService.requiresSplitting(savedOrder)) {
                List<Order> subOrders = orderSplitterService.splitOrderBySeller(savedOrder);
                log.info("Order {} split into {} sub-orders for independent seller fulfillment",
                        savedOrder.getUuid(), subOrders.size());
            }

            OrderEventOutbox outboxEntry = orderEventOutboxRepository.save(OrderEventOutbox.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderUuid(savedOrder.getUuid())
                    .topic(OrderCreatedEvent.TOPIC)
                    .build());

            applicationEventPublisher.publishEvent(new OrderCreatedOutboxRequestedEvent(outboxEntry.getId()));
        } catch (Exception e) {
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

            if (e instanceof FeignException feignException) {
                throw new OrderStateException("Failed to reduce product stock: " + feignException.getMessage());
            }
            throw e;
        }

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

        if ("BUYER".equalsIgnoreCase(role) && !order.getBuyerUuid().equals(userUuid)) {
            throw new OrderAccessException("You can only view your own orders");
        }

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

    private OrderResponse advanceBySeller(Order order, String sellerUuid, OrderStatus requestedStatus) {
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
        Order parentOrder = orderRepository.findByUuidAndIsDeletedFalse(parentOrderUuid)
                .orElseThrow(() -> new OrderNotFoundException("Parent order not found: " + parentOrderUuid));

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

        Order sampleOrder = groupOrders.get(0);
        if ("BUYER".equalsIgnoreCase(role) && !sampleOrder.getBuyerUuid().equals(userUuid)) {
            throw new OrderAccessException("Access denied to this order group");
        }

        return groupOrders.stream()
                .map(this::mapToResponse)
                .toList();
    }
}
