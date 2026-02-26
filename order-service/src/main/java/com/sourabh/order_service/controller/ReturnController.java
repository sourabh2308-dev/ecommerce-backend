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
 * REST Controller for managing order returns and exchanges.
 * 
 * <p>Handles the complete return/exchange lifecycle:
 * <ul>
 *   <li>Buyers can request returns/exchanges for delivered orders</li>
 *   <li>Admins can approve/reject return requests</li>
 *   <li>Status updates for return processing (pickup, inspection, refund)</li>
 * </ul>
 * 
 * <p>Return types supported: REFUND (money back) and EXCHANGE (replacement item).
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@RestController
@RequestMapping("/api/order/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    /**
     * Creates a new return/exchange request for a delivered order.
     * 
     * <p>Buyer must provide:
     * <ul>
     *   <li>Order UUID</li>
     *   <li>Return type (REFUND or EXCHANGE)</li>
     *   <li>Reason for return</li>
 * </ul>
     * 
     * @param buyerUuid the UUID of the buyer requesting return
     * @param request the return request details
     * @return ResponseEntity with created return request
     */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ReturnResponse> requestReturn(
            @RequestHeader("X-User-UUID") String buyerUuid,
            @Valid @RequestBody ReturnRequestDto request) {
        return ResponseEntity.ok(returnService.requestReturn(buyerUuid, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<PageResponse<ReturnResponse>> myReturns(
            @RequestHeader("X-User-UUID") String buyerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(returnService.getMyReturns(buyerUuid, page, size));
    }

    @GetMapping("/{returnUuid}")
    public ResponseEntity<ReturnResponse> getReturn(@PathVariable String returnUuid) {
        return ResponseEntity.ok(returnService.getReturn(returnUuid));
    }

    /**
     * Approves a return request.
     * 
     * <p>Admin can optionally specify refund amount (useful for partial refunds)
     * and add notes explaining the decision.
     * 
     * @param returnUuid the UUID of the return request
     * @param adminNotes optional notes from admin
     * @param refundAmount optional custom refund amount
     * @return ResponseEntity with approved return details
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
     * Rejects a return request.
     * 
     * <p>Admin should provide notes explaining why the return was rejected.
     * 
     * @param returnUuid the UUID of the return request
     * @param adminNotes optional notes from admin
     * @return ResponseEntity with rejected return details
     */
    @PutMapping("/{returnUuid}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReturnResponse> reject(
            @PathVariable String returnUuid,
            @RequestParam(required = false) String adminNotes) {
        return ResponseEntity.ok(returnService.rejectReturn(returnUuid, adminNotes));
    }

    @PutMapping("/{returnUuid}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<ReturnResponse> updateStatus(
            @PathVariable String returnUuid,
            @RequestParam String status) {
        return ResponseEntity.ok(returnService.updateReturnStatus(returnUuid, status));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ReturnResponse>> allReturns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(returnService.getAllReturns(status, page, size));
    }
}
