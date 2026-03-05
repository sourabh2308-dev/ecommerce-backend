package com.sourabh.order_service.service;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.response.OrderResponse;

import java.util.List;

/**
 * Core service interface for order lifecycle management.
 *
 * <p>Covers order creation, status transitions, payment-status updates, listing
 * (with role-based visibility), and multi-seller order grouping / sub-order retrieval.</p>
 *
 * <h3>Saga Pattern</h3>
 * <ol>
 *   <li>{@link #createOrder} validates items, reduces stock, persists the order, and
 *       publishes an {@code order.created} Kafka event consumed by payment-service.</li>
 *   <li>Payment-service processes payment and publishes {@code payment.completed}.</li>
 *   <li>{@link #updatePaymentStatus} receives the event and updates the order
 *       accordingly, cancelling it and restoring stock on failure.</li>
 * </ol>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see OrderResponse
 */
public interface OrderService {

    /**
     * Creates a new order, reduces product stock, and triggers the payment saga.
     *
     * @param request   order creation payload (items, shipping details)
     * @param role      authenticated user role ({@code BUYER} required)
     * @param buyerUuid UUID of the authenticated buyer
     * @return {@link OrderResponse} with payment status {@code PENDING}
     * @throws com.sourabh.order_service.exception.OrderAccessException if the role is not {@code BUYER}
     * @throws com.sourabh.order_service.exception.OrderStateException  if a product is inactive or stock reduction fails
     */
    OrderResponse createOrder(
            CreateOrderRequest request,
            String role,
            String buyerUuid);

    /**
     * Lists orders with role-based filtering.
     *
     * <p>{@code ADMIN} sees all non-deleted orders; {@code BUYER} sees only their own.</p>
     *
     * @param page      zero-based page index
     * @param size      page size
     * @param role      authenticated user role
     * @param buyerUuid UUID of the buyer (used when role is {@code BUYER})
     * @return paginated {@link OrderResponse} list sorted by creation date descending
     */
    PageResponse<OrderResponse> listOrders(
            int page,
            int size,
            String role,
            String buyerUuid);

    /**
     * Transitions an order to a new status, applying role-specific business rules.
     *
     * <p>Buyers may cancel or request a return; admins and sellers advance the
     * order through the fulfilment pipeline (CONFIRMED → SHIPPED → DELIVERED, etc.).</p>
     *
     * @param uuid         order UUID
     * @param role         authenticated user role
     * @param buyerUuid    UUID of the authenticated user
     * @param newStatus    target {@link com.sourabh.order_service.entity.OrderStatus} name
     * @param returnType   return type ({@code REFUND} or {@code EXCHANGE}), required for return requests
     * @param returnReason free-text reason for the return, required for return requests
     * @return updated {@link OrderResponse}
     */
    OrderResponse updateOrderStatus(
            String uuid,
            String role,
            String buyerUuid,
            String newStatus,
            String returnType,
            String returnReason);

    /**
     * Lists orders containing items sold by the specified seller.
     *
     * @param page       zero-based page index
     * @param size       page size
     * @param sellerUuid UUID of the seller
     * @return paginated {@link OrderResponse} list
     */
    PageResponse<OrderResponse> listSellerOrders(
            int page,
            int size,
            String sellerUuid);

    /**
     * Updates the payment status of an order (called from the Kafka consumer).
     *
     * <p>When the status is {@code FAILED}, the order is automatically cancelled
     * and product stock is restored as part of the saga compensation.</p>
     *
     * @param uuid   order UUID
     * @param status new {@link com.sourabh.order_service.entity.PaymentStatus} name
     */
    void updatePaymentStatus(String uuid, String status);

    /**
     * Fetches a single order by UUID with role-based access control.
     *
     * <p>Buyers may only view their own orders. Admins, sellers with items in the
     * order, and internal service calls may view any order.</p>
     *
     * @param uuid     order UUID
     * @param role     authenticated user role (may be {@code null} for internal calls)
     * @param userUuid UUID of the authenticated user
     * @return {@link OrderResponse} for the requested order
     * @throws com.sourabh.order_service.exception.OrderNotFoundException if the order does not exist
     * @throws com.sourabh.order_service.exception.OrderAccessException  if access is denied
     */
    OrderResponse getOrderByUuid(String uuid, String role, String userUuid);

    /**
     * Retrieves all sub-orders created by the order-splitter for a parent order.
     *
     * <p>Used to inspect individual seller fulfilment units within a multi-seller order.</p>
     *
     * @param parentOrderUuid UUID of the parent (MAIN) order
     * @param role            authenticated user role
     * @param userUuid        UUID of the authenticated user
     * @return list of sub-order {@link OrderResponse} objects
     */
    List<OrderResponse> getSubOrders(String parentOrderUuid, String role, String userUuid);

    /**
     * Returns every order that belongs to the same order group (parent + all sub-orders).
     *
     * @param orderGroupId shared group identifier assigned during order splitting
     * @param role         authenticated user role
     * @param userUuid     UUID of the authenticated user
     * @return list of {@link OrderResponse} objects in the group
     */
    List<OrderResponse> getOrderGroup(String orderGroupId, String role, String userUuid);

}
