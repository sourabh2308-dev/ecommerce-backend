package com.sourabh.order_service.service;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.CreateOrderRequest;
import com.sourabh.order_service.dto.response.OrderResponse;

import java.util.List;

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
            String newStatus,
            String returnType,
            String returnReason);

    PageResponse<OrderResponse> listSellerOrders(
            int page,
            int size,
            String sellerUuid);

    void updatePaymentStatus(String uuid, String status);

    OrderResponse getOrderByUuid(String uuid, String role, String userUuid);

    List<OrderResponse> getSubOrders(String parentOrderUuid, String role, String userUuid);

    List<OrderResponse> getOrderGroup(String orderGroupId, String role, String userUuid);

}
