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

    OrderResponse getOrderByUuid(String uuid);

}
