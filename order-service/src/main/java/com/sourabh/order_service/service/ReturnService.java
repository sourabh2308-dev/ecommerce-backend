package com.sourabh.order_service.service;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.ReturnRequestDto;
import com.sourabh.order_service.dto.response.ReturnResponse;

/**
 * Service interface for managing order return and exchange requests.
 *
 * <p>Supports the complete return lifecycle from customer-initiated requests
 * through admin approval/rejection to final resolution (refund or exchange).</p>
 *
 * <h3>Return Types</h3>
 * <ul>
 *   <li>{@code REFUND}   – customer returns the product and receives money back.</li>
 *   <li>{@code EXCHANGE} – customer returns the product and receives a replacement.</li>
 * </ul>
 *
 * <h3>Status Flow</h3>
 * <pre>
 * REQUESTED → APPROVED  → REFUNDED / EXCHANGED
 *           → REJECTED
 * </pre>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ReturnResponse
 */
public interface ReturnService {

    /**
     * Initiates a return/exchange request for a delivered order.
     *
     * <p>Only the buyer who owns the order may request a return, and the order
     * must be in {@code DELIVERED} status. Duplicate requests for the same order
     * are rejected.</p>
     *
     * @param buyerUuid UUID of the buyer initiating the return
     * @param request   DTO containing order UUID, return type, and reason
     * @return {@link ReturnResponse} with status {@code REQUESTED}
     * @throws RuntimeException if validation fails (wrong buyer, non-delivered order, duplicate)
     */
    ReturnResponse requestReturn(String buyerUuid, ReturnRequestDto request);

    /**
     * Approves a pending return request, optionally overriding the refund amount.
     *
     * @param returnUuid   UUID of the return request
     * @param adminNotes   administrative notes justifying approval
     * @param refundAmount optional override for the refund amount ({@code null} keeps the default)
     * @return {@link ReturnResponse} with status {@code APPROVED}
     * @throws RuntimeException if the return request is not found
     */
    ReturnResponse approveReturn(String returnUuid, String adminNotes, Double refundAmount);

    /**
     * Rejects a pending return request and restores the order status.
     *
     * @param returnUuid UUID of the return request
     * @param adminNotes reason for rejection
     * @return {@link ReturnResponse} with status {@code REJECTED} and resolution timestamp
     * @throws RuntimeException if the return request is not found
     */
    ReturnResponse rejectReturn(String returnUuid, String adminNotes);

    /**
     * Advances the return request to the given status.
     *
     * <p>When the target status is {@code REFUNDED} or {@code EXCHANGED}, the
     * parent order status is updated accordingly and a resolution timestamp is set.</p>
     *
     * @param returnUuid UUID of the return request
     * @param status     target {@code ReturnStatus} name
     * @return updated {@link ReturnResponse}
     */
    ReturnResponse updateReturnStatus(String returnUuid, String status);

    /**
     * Retrieves a single return request by its UUID.
     *
     * @param returnUuid UUID of the return request
     * @return {@link ReturnResponse} with full details
     * @throws RuntimeException if the return request is not found
     */
    ReturnResponse getReturn(String returnUuid);

    /**
     * Lists the authenticated buyer's return requests with pagination.
     *
     * @param buyerUuid UUID of the buyer
     * @param page      zero-based page index
     * @param size      page size
     * @return paginated {@link ReturnResponse} list sorted by creation date descending
     */
    PageResponse<ReturnResponse> getMyReturns(String buyerUuid, int page, int size);

    /**
     * Lists all return requests with optional status filtering (admin view).
     *
     * @param status optional status filter ({@code null} or blank returns all)
     * @param page   zero-based page index
     * @param size   page size
     * @return paginated {@link ReturnResponse} list sorted by creation date descending
     */
    PageResponse<ReturnResponse> getAllReturns(String status, int page, int size);
}
