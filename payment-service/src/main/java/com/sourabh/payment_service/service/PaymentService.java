package com.sourabh.payment_service.service;

import com.sourabh.payment_service.common.PageResponse;
import com.sourabh.payment_service.dto.*;

/**
 * Service contract for payment processing and financial reporting.
 *
 * <p>Defines the operations available to the payment controller and
 * the Kafka consumer.  The primary implementation is
 * {@link com.sourabh.payment_service.service.impl.PaymentServiceImpl}.
 *
 * <h3>Responsibility groups</h3>
 * <ol>
 *   <li><strong>Payment initiation</strong> — buyer-triggered payment
 *       that delegates to the configured {@code PaymentGateway}.</li>
 *   <li><strong>Gateway callback handling</strong> — processes
 *       asynchronous webhook notifications from external gateways.</li>
 *   <li><strong>Query operations</strong> — paginated listing and
 *       single-entity lookup for buyers, sellers, and admins.</li>
 *   <li><strong>Dashboard aggregations</strong> — seller and admin
 *       financial summaries.</li>
 * </ol>
 */
public interface PaymentService {

    /**
     * Initiates a new payment for the given order.
     *
     * <p>Only users with the {@code BUYER} role may call this method.
     * The implementation delegates to the configured
     * {@code PaymentGateway} and returns a gateway-specific result
     * string (e.g. a Razorpay order ID or a mock success/failure
     * message).
     *
     * @param request   payment details including order UUID and amount
     * @param role      the caller's role (must be {@code "BUYER"})
     * @param buyerUuid the authenticated buyer UUID (overrides any
     *                  value in the request body)
     * @return gateway-specific result string
     */
    String initiatePayment(PaymentRequest request, String role, String buyerUuid);

    /**
     * Returns a paginated list of payments for the specified buyer.
     *
     * @param buyerUuid the buyer UUID
     * @param page      zero-based page index
     * @param size      page size
     * @return page of payment responses
     */
    PageResponse<PaymentResponse> getPaymentsByBuyer(String buyerUuid, int page, int size);

    /**
     * Retrieves a single payment by its public UUID, enforcing
     * read-access rules based on the caller's role.
     *
     * @param uuid      payment UUID
     * @param role      caller role
     * @param buyerUuid caller UUID (used for buyer-scoped access check)
     * @return payment response
     */
    PaymentResponse getPaymentByUuid(String uuid, String role, String buyerUuid);

    /**
     * Retrieves a single payment by the associated order UUID,
     * enforcing read-access rules based on the caller's role.
     *
     * @param orderUuid order UUID
     * @param role      caller role
     * @param buyerUuid caller UUID (used for buyer-scoped access check)
     * @return payment response
     */
    PaymentResponse getPaymentByOrderUuid(String orderUuid, String role, String buyerUuid);

    /**
     * Returns a paginated list of payments that contain at least one
     * split for the specified seller.
     *
     * @param sellerUuid the seller UUID
     * @param page       zero-based page index
     * @param size       page size
     * @return page of payment responses
     */
    PageResponse<PaymentResponse> getSellerPayments(String sellerUuid, int page, int size);

    /**
     * Builds a financial summary for the specified seller including
     * completed payouts, pending payouts, and total order count.
     *
     * @param sellerUuid the seller UUID
     * @return seller dashboard response
     */
    SellerDashboardResponse getSellerDashboard(String sellerUuid);

    /**
     * Builds a platform-wide financial summary for administrators
     * covering gross revenue, platform fees, delivery fees, seller
     * payouts, order counts, and active seller counts.
     *
     * @return admin dashboard response
     */
    AdminDashboardResponse getAdminDashboard();

    /**
     * Handles an asynchronous notification from the payment gateway
     * (webhook / callback).  Updates the payment status and publishes
     * a {@code payment.completed} Kafka event so that the order-service
     * can advance or compensate the saga.
     *
     * @param gatewayOrderId gateway-issued order identifier (or
     *                       internal order UUID for backward compat)
     * @param success        {@code true} if the payment succeeded
     * @param gatewayResponse additional provider-specific payload
     */
    void handleGatewayCallback(String gatewayOrderId, boolean success, String gatewayResponse);
}
