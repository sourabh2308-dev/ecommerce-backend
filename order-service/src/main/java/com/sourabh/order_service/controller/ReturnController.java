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

@RestController
@RequestMapping("/api/order/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

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

    @PutMapping("/{returnUuid}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReturnResponse> approve(
            @PathVariable String returnUuid,
            @RequestParam(required = false) String adminNotes,
            @RequestParam(required = false) Double refundAmount) {
        return ResponseEntity.ok(returnService.approveReturn(returnUuid, adminNotes, refundAmount));
    }

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
