package com.sourabh.payment_service.service;

import com.sourabh.payment_service.common.PageResponse;
import com.sourabh.payment_service.dto.*;

public interface PaymentService {

    String initiatePayment(PaymentRequest request, String role, String buyerUuid);

    PageResponse<PaymentResponse> getPaymentsByBuyer(String buyerUuid, int page, int size);

    PaymentResponse getPaymentByUuid(String uuid, String role, String buyerUuid);

    PaymentResponse getPaymentByOrderUuid(String orderUuid, String role, String buyerUuid);

    PageResponse<PaymentResponse> getSellerPayments(String sellerUuid, int page, int size);

    SellerDashboardResponse getSellerDashboard(String sellerUuid);

    AdminDashboardResponse getAdminDashboard();

    void handleGatewayCallback(String gatewayOrderId, boolean success, String gatewayResponse);
}
