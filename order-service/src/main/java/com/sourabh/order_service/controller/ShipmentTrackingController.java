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
 * REST Controller for shipment tracking and order status updates.
 * 
 * <p>Allows sellers and admins to add tracking events for orders,
 * and provides tracking history for buyers to follow their order progress.
 * 
 * <p>Tracking events include:
 * <ul>
 *   <li>Order status changes (SHIPPED, OUT_FOR_DELIVERY, DELIVERED)</li>
 *   <li>Location updates</li>
 *   <li>Carrier information and tracking numbers</li>
 *   <li>Estimated delivery updates</li>
 * </ul>
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@RestController
@RequestMapping("/api/order/tracking")
@RequiredArgsConstructor
public class ShipmentTrackingController {

    private final ShipmentTrackingService trackingService;

    /**
     * Adds a new tracking event for an order.
     * 
     * <p>Seller or admin can add events to update order location,
     * carrier details, and status. Each event is timestamped.
     * 
     * @param request the tracking event details
     * @return ResponseEntity with the created tracking event
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<TrackingResponse> addEvent(@Valid @RequestBody TrackingEventRequest request) {
        return ResponseEntity.ok(trackingService.addTrackingEvent(request));
    }

    /**
     * Retrieves complete tracking history for an order.
     * 
     * <p>Returns all tracking events in chronological order,
     * showing the complete journey of the order from creation to delivery.
     * 
     * @param orderUuid the UUID of the order
     * @return ResponseEntity with list of tracking events
     */
    @GetMapping("/{orderUuid}")
    public ResponseEntity<List<TrackingResponse>> getHistory(@PathVariable String orderUuid) {
        return ResponseEntity.ok(trackingService.getTrackingHistory(orderUuid));
    }
}
