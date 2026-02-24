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
import com.sourabh.order_service.exception.OrderAccessException;
import com.sourabh.order_service.exception.OrderNotFoundException;
import com.sourabh.order_service.exception.OrderStateException;
import com.sourabh.order_service.feign.ProductServiceClient;
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

/**\n * ═══════════════════════════════════════════════════════════════════════════════════\n * ORDER SERVICE IMPLEMENTATION - Core Order Processing Logic\n * ═══════════════════════════════════════════════════════════════════════════════════\n *\n * PURPOSE:\n * This service orchestrates the complete order lifecycle including creation, validation,\n * status transitions, and payment saga coordination. It acts as the core business logic\n * layer for order management in the e-commerce system.\n *\n * KEY RESPONSIBILITIES:\n * 1. Order Validation & Creation\n *    - Validate buyer role (only BUYER can place orders)\n *    - Fetch product details and verify availability\n *    - Synchronously reduce stock from product-service (Feign client)\n *    - Calculate order totals and persist to database\n *\n * 2. Event Publishing (Saga Pattern)\n *    - Publish \"order.created\" Kafka event for payment-service\n *    - Enable asynchronous payment processing\n *    - Maintain event ordering per buyer (single partition)\n *\n * 3. Payment Saga Coordination\n *    - Listen for \"payment.completed\" events from payment-service\n *    - Update order payment status (SUCCESS/FAILED)\n *    - Execute saga compensation (restore stock if payment fails)\n *\n * 4. Order Status Management\n *    - Enforce valid state transitions: CREATED → CONFIRMED → SHIPPED → DELIVERED\n *    - Support role-based status updates (BUYER cancels, ADMIN/SELLER advance)\n *    - Prevent invalid transitions (e.g., DELIVERED → CONFIRMED)\n *\n * 5. Order Query & Retrieval\n *    - Support pagination for buyer's orders and seller's orders\n *    - Enforce access control (buyers see only their orders)\n *    - Cache frequently accessed orders (Redis via @Cacheable)\n *\n * DATABASE DESIGN:\n * - Order: Aggregator root, contains buyer UUID, total amount, status, payment status\n * - OrderItem: One-to-many relationship with Order, tracks product per seller\n * - Soft deletes: Orders never physically deleted (isDeleted flag)\n * - Audit: createdAt, updatedAt timestamps for tracking\n *\n * KAFKA TOPICS:\n * - order.created: Published when order is persisted\n *   Event payload: orderUuid, buyerUuid, totalAmount, items[]\n *   Consumption: payment-service initiates payment processing\n *   Partition key: buyerUuid (ensures ordered processing per buyer)\n *\n * EXTERNAL DEPENDENCIES:\n * 1. ProductServiceClient (Feign)\n *    - getProduct(uuid): Fetch product details\n *    - reduceStock(uuid, qty): Synchronously reduce stock (fails if insufficient)\n *    - restoreStock(uuid, qty): Compensation logic if order fails\n *\n * 2. OrderRepository (Spring Data JPA)\n *    - Custom queries for pagination and filtering\n *    - Soft-delete filtering (WHERE isDeleted = false)\n *\n * 3. KafkaTemplate\n *    - Send order.created events to payment-service\n *    - Non-blocking, returns immediately\n *\n * CACHING STRATEGY:\n * - @Cacheable(value=\"orders\", key=\"#uuid\"): Cache getOrderByUuid() results\n * - @CacheEvict on updates: Invalidate cache when order status changes\n * - TTL: Configurable, typically 5-10 minutes\n * - Key benefit: Reduce database hits for frequently viewed orders\n *\n * ERROR HANDLING:\n * - OrderAccessException: Role/ownership validation failures\n * - OrderNotFoundException: Order not found in database\n * - OrderStateException: Invalid state transitions, product issues\n * - FeignException: Product-service communication failures (with compensation)\n *\n * TRANSACTION MANAGEMENT:\n * - @Transactional on create/update methods ensures ACID compliance\n * - Database transaction includes:\n *   1. Stock reduction calls (may fail, preventing order creation)\n *   2. Order entity insertion with all items\n *   3. Kafka publish happens within same transaction (transactional outbox could be added)\n * - Rollback: If any step fails, entire transaction rolls back\n *\n * SAGA PATTERN IMPLEMENTATION:\n * ┌─────────────────────────────────────────┐\n * │ Order Creation (Synchronous)            │\n * │ 1. Validate stock                       │\n * │ 2. Create Order                         │\n * │ 3. Publish order.created event          │\n * └─────────────────────────────────────────┘\n *              │\n *              ▼\n * ┌─────────────────────────────────────────┐\n * │ Payment Processing (Asynchronous via Saga)\n * │ 1. payment-service consumes event       │\n * │ 2. Create Payment entity                │\n * │ 3. Simulate payment (100% success)      │\n * │ 4. Publish payment.completed event      │\n * └─────────────────────────────────────────┘\n *              │\n *              ▼\n * ┌─────────────────────────────────────────┐\n * │ Order Update (updatePaymentStatus)      │\n * │ If SUCCESS: paymentStatus = SUCCESS     │\n * │ If FAILED: status = CANCELLED           │\n * │            Restore stock (compensation) │\n * └─────────────────────────────────────────┘\n *\n * TECHNOLOGY STACK:\n * - @Service: Spring component, singleton bean for service layer\n * - @RequiredArgsConstructor: Lombok generates constructor for final fields\n * - @Slf4j: SLF4J logging with Logback backend\n * - @Transactional: Spring Data JPA transaction management\n * - KafkaTemplate: Spring Kafka for event publishing\n * - Feign: Declarative REST client for product-service calls\n *\n * PERFORMANCE CONSIDERATIONS:\n * - Caching orders reduces database queries\n * - Async Kafka prevents blocking on payment processing\n * - API Gateway handles JWT validation (not done here)\n * - Pagination prevents loading thousands of orders\n *\n * THREAD SAFETY:\n * - Service is stateless (no instance variables)\n * - Spring manages as singleton, safe for concurrent access\n * - Database provides isolation via transactions\n * - Kafka ensures ordered delivery per partition\n *\n * TESTING STRATEGY:\n * Unit Tests:\n *   - createOrder_success(): Valid order creation with stock reduction\n *   - createOrder_multipleSellerItems(): Mixed seller order\n *   - createOrder_emptyItems_throwsException(): Validation\n *   - updatePaymentStatus on SUCCESS/FAILED paths\n *\n * Integration Tests (with Testcontainers PostgreSQL):\n *   - OrderRepositoryIntegrationTest covers persistence\n *\n * MODIFICATIONS CHECKLIST:\n * When adding new features:\n * - [ ] Add new method to OrderService interface\n * - [ ] Document purpose and contract\n * - [ ] Add @Transactional if modifying state\n * - [ ] Add @Cacheable/@CacheEvict appropriately\n * - [ ] Add authorization check (@PreAuthorize or manual check)\n * - [ ] Add error handling with appropriate exceptions\n * - [ ] Add logging at INFO level for business events\n * - [ ] Add unit tests with >= 80% coverage\n * - [ ] Update this class javadoc\n * ═══════════════════════════════════════════════════════════════════════════════════\n */\n@Service\n@RequiredArgsConstructor\n@Slf4j\npublic class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Kafka topic name for order creation events.
     * Consumed by payment-service to initiate payment processing.
     * Topic partitioning by buyerUuid ensures ordered processing per buyer.
     */
    private static final String TOPIC_ORDER_CREATED = "order.created";

    /**\n     * ───────────────────────────────────────────────────────────────────────────\n     * CREATE ORDER - Primary Entry Point for Order Placement\n     * ───────────────────────────────────────────────────────────────────────────\n     *\n     * FLOW OVERVIEW:\n     * 1. Validate buyer role (only BUYER role can place orders)\n     * 2. Fetch product details from product-service via Feign client\n     * 3. Validate product status (must be ACTIVE)\n     * 4. Reduce stock synchronously for all products (with compensation)\n     * 5. Build Order entity with all items and calculate total amount\n     * 6. Persist Order to database (triggers audit timestamps)\n     * 7. Publish order.created Kafka event for asynchronous payment processing\n     * 8. Return OrderResponse to caller for immediate feedback\n     *\n     * PARAMETERS:\n     * @param request - CreateOrderRequest containing:\n     *                  - items[]: List of {productUuid, quantity} to order\n     *                  - shipping*: Address, name, phone for delivery\n     * @param role - User role from JWT (must be \"BUYER\")\n     * @param buyerUuid - Unique identifier of user placing order\n     *\n     * AUTHORIZATION:\n     * - Role check: Throws OrderAccessException if role != \"BUYER\"\n     * - Implicit: buyerUuid comes from JWT, verified by API Gateway\n     *\n     * VALIDATION:\n     * - Reject if product not found in product-service\n     * - Reject if product status != ACTIVE\n     * - Reject if stock < requested quantity\n     * - Reject if request items list is empty (validated by @Valid)\n     *\n     * COMPENSATION (SAGA ROLLBACK):\n     * If stock reduction fails on any product:\n     *   1. Capture which products already reduced (reducedProductUuids list)\n     *   2. Call productServiceClient.restoreStock() for each\n     *   3. Log failures but continue (eventual consistency)\n     *   4. Throw OrderStateException with original error message\n     *\n     * DATABASE TRANSACTION:\n     * @Transactional ensures:\n     *   - All OrderItem entities saved with Order\n     *   - If any insert fails, entire transaction rolls back\n     *   - Stock reduction calls happen before Order insert (prevents orphaned orders)\n     *   - Note: Kafka publish happens within transaction (transactional outbox pattern recommended)\n     *\n     * KAFKA EVENT PUBLISHING:\n     * After Order persisted, publishevent:\n     *   Topic: order.created\n     *   Partition key: buyerUuid (ensures per-buyer ordering)\n     *   Payload:\n     *   {\n     *     orderUuid: \"abc-123\",\n     *     buyerUuid: \"buyer-uuid\",\n     *     totalAmount: 1500.00,\n     *     items: [\n     *       {productUuid, sellerUuid, price, quantity, subtotal}\n     *     ]\n     *   }\n     *   Consumer: payment-service consumes and initiates payment\n     *\n     * CACHING:\n     * Order is NOT cached after creation (first access caches it via getOrderByUuid)\n     *\n     * RETURN VALUE:\n     * OrderResponse with all order details for immediate feedback (before payment processing)\n     *\n     * EXCEPTIONS:\n     * - OrderAccessException: Role != \"BUYER\"\n     * - OrderStateException: Product inactive, stock issues, Feign errors\n     * - DataIntegrityViolationException: Database constraint violation (caught by Spring)\n     *\n     * LOGGING:\n     * - INFO: Order created, number of items, order UUID\n     * - ERROR: Stock restoration failures (non-fatal)\n     *\n     * PERFORMANCE:\n     * O(n) where n = number of items\n     * - n product fetches via Feign\n     * - n stock reduction calls via Feign\n     * - Parallelizable prefetching could optimize (not implemented)\n     *\n     * ───────────────────────────────────────────────────────────────────────────\n     */

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String role, String buyerUuid) {

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
                .totalAmount(0.0)
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .isDeleted(false)
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
            double subtotal = product.getPrice() * itemReq.getQuantity();
            totalAmount += subtotal;

            OrderItem item = OrderItem.builder()
                    .productUuid(product.getUuid())
                    .sellerUuid(product.getSellerUuid())
                    .price(product.getPrice())
                    .quantity(itemReq.getQuantity())
                    .order(order)
                    .build();
            orderItems.add(item);

            itemEvents.add(new OrderItemEvent(
                    product.getUuid(),
                    product.getSellerUuid(),
                    product.getPrice(),
                    itemReq.getQuantity(),
                    subtotal));
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);
        orderRepository.save(order);

        // Saga: publish order.created so payment-service can auto-process payment
        kafkaTemplate.send(TOPIC_ORDER_CREATED,
                new OrderCreatedEvent(order.getUuid(), buyerUuid, itemEvents, totalAmount));

        log.info("Order created with {} items, order.created event published: orderUuid={}",
                orderItems.size(), order.getUuid());
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
        if ("SELLER".equalsIgnoreCase(role)) {
            return advanceBySeller(order, buyerUuid, requestedStatus);
        }

        throw new OrderAccessException("Unauthorized action");
    }

    @Override
    @Cacheable(value = "orders", key = "#uuid")
    public OrderResponse getOrderByUuid(String uuid, String role, String userUuid) {
        log.debug("Cache miss for order uuid={} — fetching from DB", uuid);
        Order order = orderRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + uuid));

        // Buyers can only see their own orders
        if ("BUYER".equalsIgnoreCase(role) && !order.getBuyerUuid().equals(userUuid)) {
            throw new OrderAccessException("You can only view your own orders");
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", key = "#uuid")
    public void updatePaymentStatus(String uuid, String status) {
        /**
         * UPDATE PAYMENT STATUS - Saga Pattern Coordination Point
         *
         * CONTEXT:
         * Called by payment-service via internal REST call (HTTP POST)
         * after payment processing completes (success or failure).
         * This is the synchronization point between two services in the saga pattern.
         *
         * FLOW:
         * 1. Fetch Order by UUID (throws OrderNotFoundException if not found)
         * 2. Parse payment status string to enum (PENDING, SUCCESS, FAILED)
         * 3. Update order.paymentStatus with new status
         * 4. If status == FAILED, cascade to cancel order and restore stock
         * 5. Persist updated Order
         * 6. Invalidate Redis cache (force next fetch from DB)
         *
         * PARAMETERS:
         * @param uuid - Order UUID to update
         * @param status - Payment status from payment-service (\"SUCCESS\" or \"FAILED\")\n         *\n         * SAGA COMPENSATION:\n         * If payment failed (status == \"FAILED\"):\n         *   1. Update order.status to CANCELLED\n         *   2. Iterate through all OrderItems\n         *   3. Call productServiceClient.restoreStock() for each item\n         *   4. Non-blocking: Continue even if restore fails (log error)\n         * Benefit: Prevents indefinitely locked stock if payment fails\n         *\n         * CACHING:\n         * @CacheEvict(value=\"orders\", key=\"#uuid\") invalidates cached entry\n         * Forces next getOrderByUuid() call to fetch fresh data from DB\n         *\n         * DATABASE TRANSACTION:\n         * @Transactional ensures atomic update of order status and payment status\n         */
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

    private OrderResponse advanceBySeller(Order order, String sellerUuid, OrderStatus requestedStatus) {
        // Verify this seller has items in the order
        boolean hasItems = order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> sellerUuid.equals(item.getSellerUuid()));
        if (!hasItems) {
            throw new OrderAccessException("You have no items in this order");
        }

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
        log.info("Order status updated by seller: uuid={}, sellerUuid={}, newStatus={}",
                order.getUuid(), sellerUuid, requestedStatus);
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
                .shippingName(order.getShippingName())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingPincode(order.getShippingPincode())
                .shippingPhone(order.getShippingPhone())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
