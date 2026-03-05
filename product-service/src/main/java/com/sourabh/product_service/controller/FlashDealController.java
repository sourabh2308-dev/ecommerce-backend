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

/**
 * REST controller for managing flash deals (time-limited product discounts).
 * <p>
 * Sellers can create and cancel their own deals, while the active-deals
 * endpoint is publicly accessible for buyers browsing current promotions.
 * </p>
 *
 * <p>Base path: {@code /api/product/deals}</p>
 */
@RestController
@RequestMapping("/api/product/deals")
@RequiredArgsConstructor
public class FlashDealController {

    /** Service encapsulating flash-deal business logic. */
    private final FlashDealService flashDealService;

    /**
     * Creates a new flash deal for a product owned by the authenticated seller.
     *
     * @param sellerUuid UUID of the authenticated seller (injected from gateway header)
     * @param request    validated payload containing deal details
     * @return the newly created flash deal
     */
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<FlashDealResponse> createDeal(
            @RequestHeader("X-User-UUID") String sellerUuid,
            @Valid @RequestBody FlashDealRequest request) {
        return ResponseEntity.ok(flashDealService.createDeal(sellerUuid, request));
    }

    /**
     * Lists all currently active flash deals whose time window includes the present moment.
     *
     * @return list of active flash deals sorted by end time ascending
     */
    @GetMapping("/active")
    public ResponseEntity<List<FlashDealResponse>> getActiveDeals() {
        return ResponseEntity.ok(flashDealService.getActiveDeals());
    }

    /**
     * Lists all flash deals created by the authenticated seller.
     *
     * @param sellerUuid UUID of the authenticated seller
     * @return list of the seller's deals ordered by creation date descending
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<FlashDealResponse>> getMyDeals(
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(flashDealService.getMyDeals(sellerUuid));
    }

    /**
     * Cancels (deactivates) a flash deal owned by the authenticated seller.
     *
     * @param dealUuid   UUID of the deal to cancel
     * @param sellerUuid UUID of the authenticated seller
     * @return confirmation message
     */
    @DeleteMapping("/{dealUuid}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> cancelDeal(
            @PathVariable String dealUuid,
            @RequestHeader("X-User-UUID") String sellerUuid) {
        return ResponseEntity.ok(flashDealService.cancelDeal(dealUuid, sellerUuid));
    }
}
