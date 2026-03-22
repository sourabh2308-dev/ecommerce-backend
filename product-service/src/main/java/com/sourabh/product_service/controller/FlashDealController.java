package com.sourabh.product_service.controller;

import com.sourabh.product_service.dto.request.FlashDealRequest;
import com.sourabh.product_service.dto.response.FlashDealResponse;
import com.sourabh.product_service.service.FlashDealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product/deals")
@RequiredArgsConstructor
public class FlashDealController {

    private final FlashDealService flashDealService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<FlashDealResponse> createDeal(
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody FlashDealRequest request) {
        return ResponseEntity.ok(flashDealService.createDeal(sellerUuid, request));
    }

    @GetMapping("/active")
    public ResponseEntity<List<FlashDealResponse>> getActiveDeals() {
        return ResponseEntity.ok(flashDealService.getActiveDeals());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<FlashDealResponse>> getMyDeals(
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(flashDealService.getMyDeals(sellerUuid));
    }

    @DeleteMapping("/{dealUuid}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> cancelDeal(
            @PathVariable String dealUuid,
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(flashDealService.cancelDeal(dealUuid, sellerUuid));
    }
}
