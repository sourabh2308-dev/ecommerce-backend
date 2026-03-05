package com.sourabh.payment_service.controller;

import com.sourabh.payment_service.common.PageResponse;
import com.sourabh.payment_service.dto.*;
import com.sourabh.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * REST controller exposing payment endpoints under {@code /api/payment}.
 *
 * <p>Responsibilities include payment initiation (buyer), payment retrieval
 * (buyer / admin), seller earnings views, an unauthenticated webhook for
 * external payment-gateway callbacks (Razorpay), and an admin financial
 * dashboard.
 *
 * <p><b>Authentication model:</b> JWT validation happens at the API Gateway.
 * The gateway forwards {@code X-User-UUID} and {@code X-User-Role} headers
 * which are converted into a Spring Security context by
 * {@link com.sourabh.payment_service.config.HeaderRoleAuthenticationFilter}.
 * Method-level authorisation is enforced via {@code @PreAuthorize}.
 *
 * @see com.sourabh.payment_service.service.PaymentService
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    /** Delegate for all payment business logic. */
    private final PaymentService paymentService;

    /**
     * Initiates a new payment for the authenticated buyer.
     *
     * <p>The buyer UUID is taken from the gateway-injected header, <b>not</b>
     * from the request body, to prevent spoofing.
     *
     * @param request     validated payment request body
     * @param httpRequest servlet request carrying gateway headers
     * @return the gateway result string (e.g. {@code "Payment SUCCESS"} or
     *         a Razorpay order ID when the payment is pending)
     */
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public ResponseEntity<String> pay(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.initiatePayment(request, role, buyerUuid));
    }

    /**
     * Returns a paginated list of the authenticated buyer's own payments.
     *
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 10)
     * @param httpRequest servlet request carrying the buyer UUID header
     * @return paginated {@link PaymentResponse} list
     */
    @PreAuthorize("hasRole('BUYER')")
    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentsByBuyer(buyerUuid, page, size));
    }

    /**
     * Retrieves a single payment by its UUID.
     *
     * <p>Buyers may only view their own payments; admins can view any payment.
     *
     * @param uuid        the payment UUID path variable
     * @param httpRequest servlet request carrying role and buyer UUID headers
     * @return the matching {@link PaymentResponse}
     */
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/{uuid}")
    public ResponseEntity<PaymentResponse> getPaymentByUuid(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentByUuid(uuid, role, buyerUuid));
    }

    /**
     * Retrieves the payment associated with a given order UUID.
     *
     * <p>Access rules are the same as {@link #getPaymentByUuid}: buyers see
     * only their own; admins see any.
     *
     * @param orderUuid   the order UUID path variable
     * @param httpRequest servlet request carrying role and buyer UUID headers
     * @return the matching {@link PaymentResponse}
     */
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/order/{orderUuid}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderUuid(
            @PathVariable String orderUuid,
            HttpServletRequest httpRequest) {
        String role      = httpRequest.getHeader("X-User-Role");
        String buyerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getPaymentByOrderUuid(orderUuid, role, buyerUuid));
    }

    /**
     * Returns a paginated list of payments in which the authenticated seller
     * has at least one {@link com.sourabh.payment_service.entity.PaymentSplit}.
     *
     * @param page        zero-based page index (default 0)
     * @param size        page size (default 10)
     * @param httpRequest servlet request carrying the seller UUID header
     * @return paginated {@link PaymentResponse} list filtered by seller
     */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller")
    public ResponseEntity<PageResponse<PaymentResponse>> getSellerPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        String sellerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getSellerPayments(sellerUuid, page, size));
    }

    /**
     * Returns the seller's financial dashboard containing aggregated earnings,
     * pending payouts, and total fulfilled orders.
     *
     * @param httpRequest servlet request carrying the seller UUID header
     * @return the {@link SellerDashboardResponse} for the authenticated seller
     */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/seller/dashboard")
    public ResponseEntity<SellerDashboardResponse> getSellerDashboard(
            HttpServletRequest httpRequest) {
        String sellerUuid = httpRequest.getHeader("X-User-UUID");
        return ResponseEntity.ok(paymentService.getSellerDashboard(sellerUuid));
    }

    /**
     * Public webhook endpoint invoked by the payment gateway (e.g. Razorpay)
     * when a payment's status changes asynchronously.
     *
     * <p>The method accepts the Razorpay callback payload and verifies the
     * signature using HMAC-SHA256.  If verification passes and a
     * {@code razorpay_payment_id} is present the payment is marked as
     * successful; otherwise it is marked as failed.
     *
     * <p><b>Note:</b> This endpoint is <em>not</em> protected by
     * {@code @PreAuthorize} because the caller is the external gateway,
     * not an authenticated user.
     *
     * @param payload   the JSON body sent by the gateway
     * @param signature optional {@code X-Razorpay-Signature} header
     * @return HTTP 200 acknowledgement
     */
    @PostMapping("/gateway/webhook")
    public ResponseEntity<Void> gatewayWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        String orderId  = (String) payload.get("razorpay_order_id");
        String paymentId = (String) payload.get("razorpay_payment_id");
        String sigPayload = (String) payload.get("razorpay_signature");

        String effectiveSig = signature != null ? signature : sigPayload;

        boolean verified = true;
        if (effectiveSig != null && orderId != null && paymentId != null) {
            verified = paymentService instanceof com.sourabh.payment_service.service.impl.PaymentServiceImpl ?
                    ((com.sourabh.payment_service.service.impl.PaymentServiceImpl) paymentService)
                            .getPaymentGateway().verify(orderId, paymentId, effectiveSig)
                    : true;
        }

        if (verified && orderId != null) {
            boolean success = paymentId != null && !paymentId.isBlank();
            paymentService.handleGatewayCallback(orderId, success, paymentId);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Returns the platform-wide admin financial dashboard with gross revenue,
     * platform earnings, delivery fees, seller payouts, and order counts.
     *
     * @return the {@link AdminDashboardResponse}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dashboard")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(paymentService.getAdminDashboard());
    }
}
