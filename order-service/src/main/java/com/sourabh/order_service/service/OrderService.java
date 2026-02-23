package com.sourabh.order_service.service;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.response.OrderResponse;

public interface OrderService {

    OrderResponse createOrder(
            CreateOrderRequest request,
            String role,
            String buyerUuid);

    PageResponse<OrderResponse> listOrders(
            int page,
            int size,
            String role,
            String buyerUuid);

    OrderResponse updateOrderStatus(
            String uuid,
            String role,
            String buyerUuid,
            String newStatus);

    PageResponse<OrderResponse> listSellerOrders(
            int page,
            int size,
            String sellerUuid);

    void updatePaymentStatus(String uuid, String status);

    /**
     * Fetch a single order.
     * BUYER: may only see their own orders.
     * ADMIN / SELLER / null (internal service call): may see any order.
     */
    OrderResponse getOrderByUuid(String uuid, String role, String userUuid);

}
