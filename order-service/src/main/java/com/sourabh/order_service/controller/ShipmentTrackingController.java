package com.sourabh.order_service.controller;

import com.sourabh.order_service.dto.request.TrackingEventRequest;
import com.sourabh.order_service.dto.response.TrackingResponse;
import com.sourabh.order_service.service.ShipmentTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for recording and querying shipment tracking events.
 *
 * <p>Sellers and admins append tracking events (location updates, carrier
 * information, status changes) as an order progresses through the logistics
 * pipeline. Buyers can then retrieve the full chronological tracking history
 * to follow their order’s journey.</p>
 *
 * <p>Base path: {@code /api/order/tracking}</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ShipmentTrackingService
 * @see com.sourabh.order_service.entity.ShipmentTracking
 */
@RestController
@RequestMapping("/api/order/tracking")
@RequiredArgsConstructor
public class ShipmentTrackingController {

    /** Service encapsulating shipment tracking business logic. */
    private final ShipmentTrackingService trackingService;

    /**
     * Appends a new tracking event to an order’s shipment history.
     *
     * <p>The event may include a status update, location, carrier name,
     * tracking number, and a free-text description. Each event is timestamped
     * at creation time.</p>
     *
     * @param request validated {@link TrackingEventRequest} payload
     * @return {@link ResponseEntity} containing the persisted
     *         {@link TrackingResponse}
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<TrackingResponse> addEvent(@Valid @RequestBody TrackingEventRequest request) {
        return ResponseEntity.ok(trackingService.addTrackingEvent(request));
    }

    /**
     * Retrieves the complete tracking history for an order, sorted
     * chronologically (oldest event first).
     *
     * @param orderUuid UUID of the order
     * @return {@link ResponseEntity} containing a list of
     *         {@link TrackingResponse} events
     */
    @GetMapping("/{orderUuid}")
    public ResponseEntity<List<TrackingResponse>> getHistory(@PathVariable String orderUuid) {
        return ResponseEntity.ok(trackingService.getTrackingHistory(orderUuid));
    }
}
