package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.ReturnRequestDto;
import com.sourabh.order_service.dto.response.ReturnResponse;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderStatus;
import com.sourabh.order_service.entity.ReturnRequest;
import com.sourabh.order_service.entity.ReturnType;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.repository.ReturnRequestRepository;
import com.sourabh.order_service.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * RETURN SERVICE IMPLEMENTATION
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Manages order return/refund/exchange requests from customers.
 * This service orchestrates:
 *   1. Return request creation with validation (only delivered orders can be returned)
 *   2. Return approval/rejection by administrators
 *   3. Status progression (REQUESTED -> APPROVED/REJECTED -> REFUNDED/EXCHANGED)
 *   4. Automatic order status synchronization
 *   5. Refund amount tracking and override capability
 *   6. Pagination and filtering support for admin dashboards
 * 
 * KEY RESPONSIBILITIES:
 * ---------------------
 * - Validate return eligibility (order must be DELIVERED)
 * - Prevent duplicate returns for same order
 * - Create and track return requests with reason and type
 * - Manage return lifecycle (approval, rejection, completion)
 * - Update parent order status based on return decision
 * - Calculate and override refund amounts
 * - Provide customer-specific return history with pagination
 * - Provide admin view with filtering by status
 * 
 * RETURN TYPES:
 * ──────────
 * REFUND: Customer returns product, receives money back
 * EXCHANGE: Customer returns product, receives replacement instead
 * 
 * RETURN STATUSES:
 * ────────────
 * REQUESTED: Initial state when customer submits return request
 * APPROVED: Admin approves return (refund/exchange will be processed)
 * REJECTED: Admin rejects return (customer keeps product)
 * REFUNDED: Payment refunded to customer
 * EXCHANGED: Replacement product shipped to customer
 * 
 * VALIDATION RULES:
 * ──────────────
 * - Order ownership: Buyer UUID must match order's buyer UUID
 * - Order delivery: Order status must be DELIVERED (cannot return incomplete orders)
 * - No duplicates: Only one return request per order (prevent abuse)
 * - Return type: Must be valid enum (REFUND or EXCHANGE)
 * 
 * DEPENDENCIES:
 * ──────────────
 * - ReturnRequestRepository: JPA repository for return request CRUD
 * - OrderRepository: Fetches and updates order status
 * 
 * ANNOTATIONS:
 * ─────────────
 * @Service: Marks class as Spring service layer component
 * @RequiredArgsConstructor: Lombok generates constructor for final fields
 * @Transactional: Default for methods that modify state
 * @Transactional(readOnly = true): Optimizes query-only methods
 * 
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;

    /**
     * Initiate a return/exchange request for a delivered order.
     * 
     * PURPOSE:
     * Creates a new return request after comprehensive validation.
     * Only delivered orders can be returned, preventing abuse and incomplete returns.
     * 
     * VALIDATION SEQUENCE:
     * 1. Order exists and not deleted (or throws RuntimeException)
     * 2. Requesting buyer is the order's actual buyer (ownership check)
     * 3. Order status is DELIVERED (cannot return pending, cancelled, etc.)
     * 4. No existing return request for this order (prevent duplicates)
     * 
     * PROCESS FLOW:
     * 1. Update parent order status to RETURN_REQUESTED
     * 2. Store return type and reason on order
     * 3. Create ReturnRequest entity with:
     *    - Order UUID (link to parent order)
     *    - Buyer UUID (for audit and authorization)
     *    - Return type (REFUND or EXCHANGE)
     *    - Reason (customer explanation)
     *    - Refund amount (defaults to full order total, can be overridden by admin)
     * 4. Save return request to database
     * 5. Return response with return request details
     * 
     * @param buyerUuid UUID of buyer initiating return (from authentication token)
     * @param request ReturnRequestDto containing:
     *        - orderUuid: UUID of order to return
     *        - returnType: Either "REFUND" or "EXCHANGE"
     *        - reason: Customer explanation for return (e.g., "Wrong color", "Defective")
     * 
     * @return ReturnResponse with created return request UUID and status (REQUESTED)
     * 
     * @throws RuntimeException if:
     *         - Order not found or marked as deleted
     *         - Buyer UUID doesn't match order's buyer
     *         - Order not in DELIVERED status
     *         - Return already exists for this order
     */
    @Override
    @Transactional
    public ReturnResponse requestReturn(String buyerUuid, ReturnRequestDto request) {
        Order order = orderRepository.findByUuidAndIsDeletedFalse(request.getOrderUuid())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getBuyerUuid().equals(buyerUuid)) {
            throw new RuntimeException("Not your order");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Can only return delivered orders");
        }
        if (returnRequestRepository.findByOrderUuid(request.getOrderUuid()).isPresent()) {
            throw new RuntimeException("Return already requested for this order");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnType(ReturnType.valueOf(request.getReturnType()));
        order.setReturnReason(request.getReason());
        orderRepository.save(order);

        ReturnRequest rr = ReturnRequest.builder()
                .orderUuid(request.getOrderUuid())
                .buyerUuid(buyerUuid)
                .returnType(ReturnType.valueOf(request.getReturnType()))
                .reason(request.getReason())
                .refundAmount(order.getTotalAmount())
                .build();
        return mapToResponse(returnRequestRepository.save(rr));
    }

    /**
     * Approve a return request and authorize refund/exchange.
     * 
     * PURPOSE:
     * Admin approves return request, permitting refund/exchange processing.
     * Return status moves from REQUESTED to APPROVED.
     * Admin can override refund amount (e.g., partial refund for damage).
     * 
     * @param returnUuid UUID of return request to approve
     * @param adminNotes Administrative notes (approval reason, refund justification, etc.)
     * @param refundAmount Optional refund amount (overrides order total if set).
     *                      If null, uses refund amount set at request time.
     *                      Useful for partial refunds or damage deductions.
     * 
     * @return ReturnResponse with updated return status (APPROVED) and admin notes
     * 
     * @throws RuntimeException if return request not found
     */
    @Override
    @Transactional
    public ReturnResponse approveReturn(String returnUuid, String adminNotes, Double refundAmount) {
        ReturnRequest rr = findReturn(returnUuid);
        rr.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        rr.setAdminNotes(adminNotes);
        if (refundAmount != null) rr.setRefundAmount(refundAmount);
        return mapToResponse(returnRequestRepository.save(rr));
    }

    /**
     * Reject a return request and keep order as-is.
     * 
     * PURPOSE:
     * Admin rejects return request. Return status moves to REJECTED.
     * Parent order status updated to RETURN_REJECTED.
     * No refund/exchange will be processed.
     * 
     * @param returnUuid UUID of return request to reject
     * @param adminNotes Reason for rejection (e.g., "Return window expired", "Item not defective")
     * 
     * @return ReturnResponse with updated status (REJECTED) and resolution timestamp
     * 
     * @throws RuntimeException if return request not found
     */
    @Override
    @Transactional
    public ReturnResponse rejectReturn(String returnUuid, String adminNotes) {
        ReturnRequest rr = findReturn(returnUuid);
        rr.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        rr.setAdminNotes(adminNotes);
        rr.setResolvedAt(LocalDateTime.now());

        // Restore order status
        orderRepository.findByUuidAndIsDeletedFalse(rr.getOrderUuid())
                .ifPresent(order -> {
                    order.setStatus(OrderStatus.RETURN_REJECTED);
                    orderRepository.save(order);
                });

        return mapToResponse(returnRequestRepository.save(rr));
    }

    /**
     * Update return request status to next lifecycle state.
     * 
     * PURPOSE:
     * Transitions return request through status pipeline:
     * REQUESTED -> APPROVED -> REFUNDED/EXCHANGED
     * Or: REQUESTED -> REJECTED (handled by rejectReturn())
     * 
     * BEHAVIOR:
     * - When transitioning to REFUNDED or EXCHANGED:
     *   1. Sets resolvedAt timestamp (marks as completed)
     *   2. Updates parent order status accordingly:
     *      - REFUNDED -> order status becomes REFUND_ISSUED
     *      - EXCHANGED -> order status becomes EXCHANGE_ISSUED
     * 
     * @param returnUuid UUID of return request to update
     * @param status New status (must be valid ReturnStatus enum value:
     *               REQUESTED, APPROVED, REJECTED, REFUNDED, EXCHANGED)
     * 
     * @return ReturnResponse with updated status and resolved timestamp (if applicable)
     * 
     * @throws RuntimeException if return not found or status invalid
     */
    @Override
    @Transactional
    public ReturnResponse updateReturnStatus(String returnUuid, String status) {
        ReturnRequest rr = findReturn(returnUuid);
        rr.setStatus(ReturnRequest.ReturnStatus.valueOf(status));
        if (status.equals("REFUNDED") || status.equals("EXCHANGED")) {
            rr.setResolvedAt(LocalDateTime.now());
            orderRepository.findByUuidAndIsDeletedFalse(rr.getOrderUuid())
                    .ifPresent(order -> {
                        order.setStatus(status.equals("REFUNDED") ? OrderStatus.REFUND_ISSUED : OrderStatus.EXCHANGE_ISSUED);
                        orderRepository.save(order);
                    });
        }
        return mapToResponse(returnRequestRepository.save(rr));
    }

    /**
     * Fetch a single return request by UUID.
     * 
     * PURPOSE:
     * Retrieves detailed information about a specific return request.
     * Used in return detail pages and admin dashboards.
     * 
     * @param returnUuid UUID of return request to fetch
     * 
     * @return ReturnResponse with all return request details
     * 
     * @throws RuntimeException if return request not found
     */
    @Override
    @Transactional(readOnly = true)
    public ReturnResponse getReturn(String returnUuid) {
        return mapToResponse(findReturn(returnUuid));
    }

    /**
     * Fetch paginated list of return requests for a specific buyer.
     * 
     * PURPOSE:
     * Provides customer with history of their return requests in order timeline.
     * Used in customer portal / my orders / return history page.
     * Results sorted by creation date (newest first).
     * 
     * @param buyerUuid UUID of buyer whose returns to fetch
     * @param page Zero-indexed page number (0 = first page)
     * @param size Number of results per page (e.g., 10, 20)
     * 
     * @return PageResponse containing:
     *         - content: List of return requests for this buyer
     *         - page: Current page number
     *         - size: Page size
     *         - totalElements: Total returns for this buyer
     *         - totalPages: Total pages available
     *         - last: Whether this is the last page
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getMyReturns(String buyerUuid, int page, int size) {
        Page<ReturnRequest> pg = returnRequestRepository.findByBuyerUuid(buyerUuid,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return toPageResponse(pg);
    }

    /**
     * Fetch paginated list of all return requests with optional status filter.
     * 
     * PURPOSE:
     * Admin view for return request management.
     * Filter by status to focus on pending approvals, rejected, refunded, etc.
     * Results sorted by creation date (newest first).
     * 
     * @param status Optional status filter (null or blank = no filter, returns all).
     *               Valid values: REQUESTED, APPROVED, REJECTED, REFUNDED, EXCHANGED
     * @param page Zero-indexed page number (0 = first page)
     * @param size Number of results per page
     * 
     * @return PageResponse with filtered/paginated return requests
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getAllReturns(String status, int page, int size) {
        Page<ReturnRequest> pg;
        if (status != null && !status.isBlank()) {
            pg = returnRequestRepository.findByStatus(ReturnRequest.ReturnStatus.valueOf(status),
                    PageRequest.of(page, size, Sort.by("createdAt").descending()));
        } else {
            pg = returnRequestRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        }
        return toPageResponse(pg);
    }

    // ─── Helpers ───

    private ReturnRequest findReturn(String uuid) {
        return returnRequestRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Return request not found"));
    }

    private PageResponse<ReturnResponse> toPageResponse(Page<ReturnRequest> pg) {
        List<ReturnResponse> content = pg.getContent().stream().map(this::mapToResponse).toList();
        return PageResponse.<ReturnResponse>builder()
                .content(content)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .totalPages(pg.getTotalPages())
                .last(pg.isLast())
                .build();
    }

    private ReturnResponse mapToResponse(ReturnRequest rr) {
        return ReturnResponse.builder()
                .uuid(rr.getUuid())
                .orderUuid(rr.getOrderUuid())
                .buyerUuid(rr.getBuyerUuid())
                .returnType(rr.getReturnType().name())
                .reason(rr.getReason())
                .status(rr.getStatus().name())
                .adminNotes(rr.getAdminNotes())
                .refundAmount(rr.getRefundAmount())
                .resolvedAt(rr.getResolvedAt())
                .createdAt(rr.getCreatedAt())
                .build();
    }
}
