package com.sourabh.order_service.controller;

import com.sourabh.order_service.common.PageResponse;
import com.sourabh.order_service.dto.request.ReturnRequestDto;
import com.sourabh.order_service.dto.response.ReturnResponse;
import com.sourabh.order_service.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller managing the order return and exchange workflow.
 *
 * <p>Covers the full return lifecycle:
 * <ol>
 *   <li>Buyer submits a return/exchange request for a delivered order.</li>
 *   <li>Admin reviews and approves or rejects the request.</li>
 *   <li>Fulfilment status is updated as the return progresses (pickup,
 *       inspection, refund/exchange).</li>
 * </ol>
 *
 * <p>Supported return types: {@code REFUND} (monetary credit) and
 * {@code EXCHANGE} (replacement item).</p>
 *
 * <p>Base path: {@code /api/order/returns}</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ReturnService
 * @see com.sourabh.order_service.entity.ReturnRequest
 */
@RestController
@RequestMapping("/api/order/returns")
@RequiredArgsConstructor
public class ReturnController {

    /** Service encapsulating return/exchange business logic. */
    private final ReturnService returnService;

    /**
     * Creates a new return or exchange request for a delivered order.
     *
     * @param buyerUuid UUID of the buyer, from the {@code X-User-UUID} header
     * @param request   validated {@link ReturnRequestDto} containing the order
     *                  UUID, return type, and reason
     * @return {@link ResponseEntity} containing the created {@link ReturnResponse}
     */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ReturnResponse> requestReturn(
            @RequestHeader("X-User-UUID") String buyerUuid,
            @Valid @RequestBody ReturnRequestDto request) {
        return ResponseEntity.ok(returnService.requestReturn(buyerUuid, request));
    }

    /**
     * Lists the authenticated buyer's own return requests with pagination.
     *
     * @param buyerUuid UUID of the buyer, from the {@code X-User-UUID} header
     * @param page      zero-based page index (default {@code 0})
     * @param size      number of records per page (default {@code 10})
     * @return {@link ResponseEntity} containing a {@link PageResponse} of
     *         {@link ReturnResponse}
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<PageResponse<ReturnResponse>> myReturns(
            @RequestHeader("X-User-UUID") String buyerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(returnService.getMyReturns(buyerUuid, page, size));
    }

    /**
     * Retrieves a single return request by its UUID.
     *
     * @param returnUuid UUID of the return request
     * @return {@link ResponseEntity} containing the matching {@link ReturnResponse}
     */
    @GetMapping("/{returnUuid}")
    public ResponseEntity<ReturnResponse> getReturn(@PathVariable String returnUuid) {
        return ResponseEntity.ok(returnService.getReturn(returnUuid));
    }

    /**
     * Approves a pending return request.
     *
     * <p>The admin may optionally specify a custom refund amount (for partial
     * refunds) and include explanatory notes.</p>
     *
     * @param returnUuid   UUID of the return request to approve
     * @param adminNotes   optional admin notes explaining the decision
     * @param refundAmount optional custom refund amount
     * @return {@link ResponseEntity} containing the approved {@link ReturnResponse}
     */
    @PutMapping("/{returnUuid}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReturnResponse> approve(
            @PathVariable String returnUuid,
            @RequestParam(required = false) String adminNotes,
            @RequestParam(required = false) Double refundAmount) {
        return ResponseEntity.ok(returnService.approveReturn(returnUuid, adminNotes, refundAmount));
    }

    /**
     * Rejects a pending return request.
     *
     * <p>The admin should provide notes explaining the rejection reason.</p>
     *
     * @param returnUuid UUID of the return request to reject
     * @param adminNotes optional admin notes explaining the rejection
     * @return {@link ResponseEntity} containing the rejected {@link ReturnResponse}
     */
    @PutMapping("/{returnUuid}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReturnResponse> reject(
            @PathVariable String returnUuid,
            @RequestParam(required = false) String adminNotes) {
        return ResponseEntity.ok(returnService.rejectReturn(returnUuid, adminNotes));
    }

    /**
     * Advances the status of a return request through the fulfilment pipeline
     * (e.g. {@code PICKUP_SCHEDULED}, {@code PICKED_UP}, {@code RECEIVED}).
     *
     * @param returnUuid UUID of the return request
     * @param status     the target {@link com.sourabh.order_service.entity.ReturnRequest.ReturnStatus}
     *                   as a string
     * @return {@link ResponseEntity} containing the updated {@link ReturnResponse}
     */
    @PutMapping("/{returnUuid}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<ReturnResponse> updateStatus(
            @PathVariable String returnUuid,
            @RequestParam String status) {
        return ResponseEntity.ok(returnService.updateReturnStatus(returnUuid, status));
    }

    /**
     * Lists all return requests across the platform, optionally filtered by
     * status, with pagination.
     *
     * @param status optional status filter
     * @param page   zero-based page index (default {@code 0})
     * @param size   number of records per page (default {@code 10})
     * @return {@link ResponseEntity} containing a {@link PageResponse} of
     *         {@link ReturnResponse}
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReturnResponse>> allReturns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(returnService.getAllReturns(status, page, size));
    }
}
