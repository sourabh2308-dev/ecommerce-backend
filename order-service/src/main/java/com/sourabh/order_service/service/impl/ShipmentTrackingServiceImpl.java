package com.sourabh.order_service.service.impl;

import com.sourabh.order_service.dto.request.TrackingEventRequest;
import com.sourabh.order_service.dto.response.TrackingResponse;
import com.sourabh.order_service.entity.ShipmentTracking;
import com.sourabh.order_service.repository.ShipmentTrackingRepository;
import com.sourabh.order_service.service.ShipmentTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * SHIPMENT TRACKING SERVICE IMPLEMENTATION
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Manages real-time shipment tracking events for orders in transit.
 * This service orchestrates:
 *   1. Recording tracking events at different stages (picked, in-transit, out-for-delivery, etc.)
 *   2. Storing carrier information and tracking numbers
 *   3. Location updates and status progression
 *   4. Chronological retrieval of complete tracking history
 *   5. Integration with third-party logistics providers
 * 
 * KEY RESPONSIBILITIES:
 * ---------------------
 * - Add tracking events to order shipment timeline
 * - Capture event status (picked, in-transit, out-for-delivery, delivered, etc.)
 * - Store location information (city, state, facility name, etc.)
 * - Record carrier details (DHL, FedEx, Courier Service name)
 * - Store unique tracking numbers from carriers
 * - Retrieve complete chronological history for customer display
 * - Enable order status visualization with location updates
 * 
 * TRACKING STATUSES:
 * ───────────────
 * PICKED: Order picked from warehouse, ready for shipment
 * IN_TRANSIT: Package in transit to destination
 * OUT_FOR_DELIVERY: Package in local delivery vehicle, arriving today
 * DELIVERED: Package delivered to recipient
 * CANCELLED: Shipment cancelled
 * EXCEPTION: Exception during transit (damage, lost, etc.)
 * 
 * TYPICAL EVENT FLOW:
 * ─────────────────
 * 1. PICKED: Warehouse, "Package picked and packed"
 * 2. IN_TRANSIT: Distribution Center A, "Package in transit to DC B"
 * 3. IN_TRANSIT: Distribution Center B, "Package sorted and forwarded"
 * 4. OUT_FOR_DELIVERY: Local delivery hub, "Package out for delivery"
 * 5. DELIVERED: Customer address, "Successfully delivered"
 * 
 * DEPENDENCIES:
 * ──────────────
 * - ShipmentTrackingRepository: JPA repository for tracking events (CRUD operations)
 * 
 * ANNOTATIONS:
 * ─────────────
 * @Service: Marks class as Spring service layer component (business logic)
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
public class ShipmentTrackingServiceImpl implements ShipmentTrackingService {

    private final ShipmentTrackingRepository trackingRepository;

    /**
     * Record a new tracking event for a shipment.
     * 
     * PURPOSE:
     * Creates a new tracking event entry when shipment status changes.
     * Events are timestamped automatically and stored in order for retrieval.
     * 
     * PROCESS FLOW:
     * 1. Accept tracking event details (status, location, carrier info)
     * 2. Build ShipmentTracking entity with request data
     * 3. Event timestamp is set automatically by database (default to current time)
     * 4. Save event to database
     * 5. Return tracking response to caller
     * 
     * TYPICAL USAGE:
     * - Called by order fulfillment service when status changes
     * - Called by carrier integration service to sync external tracking updates
     * - Called by webhook handlers receiving carrier updates
     * 
     * @param request TrackingEventRequest containing:
     *        - orderUuid: UUID of order being tracked
     *        - status: Current tracking status (PICKED, IN_TRANSIT, DELIVERED, etc.)
     *        - location: Current location (city, facility, address, etc.)
     *        - description: Event description (e.g., "Package delivered to recipient")
     *        - carrier: Logistics carrier name (DHL, FedEx, Local Courier, etc.)
     *        - trackingNumber: Unique tracking identifier from carrier
     * 
     * @return TrackingResponse with recorded event details including:
     *         - All input fields
     *         - eventTime: Timestamp when event was recorded
     * 
     * @throws Exception if order UUID invalid or database save fails
     */
    @Override
    @Transactional
    public TrackingResponse addTrackingEvent(TrackingEventRequest request) {
        ShipmentTracking event = ShipmentTracking.builder()
                .orderUuid(request.getOrderUuid())
                .status(ShipmentTracking.TrackingStatus.valueOf(request.getStatus()))
                .location(request.getLocation())
                .description(request.getDescription())
                .carrier(request.getCarrier())
                .trackingNumber(request.getTrackingNumber())
                .build();
        return mapToResponse(trackingRepository.save(event));
    }

    /**
     * Retrieve complete tracking history for an order in chronological order.
     * 
     * PURPOSE:
     * Fetches all tracking events for an order sorted by event time (ascending).
     * Provides customer with complete shipment journey from warehouse to delivery.
     * Used in order detail and shipment tracking pages.
     * 
     * PROCESS FLOW:
     * 1. Query database for all tracking events of order
     * 2. Sort by eventTime ascending (oldest to newest)
     * 3. Convert entities to response DTOs
     * 4. Return as list
     * 
     * CHRONOLOGICAL ORDER:
     * Returns events in order they occurred, allowing customers to see progression:
     * 1. PICKED (warehouse time)
     * 2. IN_TRANSIT (first hub)
     * 3. IN_TRANSIT (intermediate hub)
     * 4. OUT_FOR_DELIVERY (local facility)
     * 5. DELIVERED (destination)
     * 
     * @param orderUuid UUID of order to fetch tracking for
     * 
     * @return List<TrackingResponse> containing all events for order in chronological order.
     *         Empty list if no tracking events found (order may not have shipped yet).
     *         Includes all fields: status, location, description, carrier, trackingNumber, eventTime
     */
    @Override
    @Transactional(readOnly = true)
    public List<TrackingResponse> getTrackingHistory(String orderUuid) {
        return trackingRepository.findByOrderUuidOrderByEventTimeAsc(orderUuid)
                .stream().map(this::mapToResponse).toList();
    }

    private TrackingResponse mapToResponse(ShipmentTracking t) {
        return TrackingResponse.builder()
                .orderUuid(t.getOrderUuid())
                .status(t.getStatus().name())
                .location(t.getLocation())
                .description(t.getDescription())
                .carrier(t.getCarrier())
                .trackingNumber(t.getTrackingNumber())
                .eventTime(t.getEventTime())
                .build();
    }
}
