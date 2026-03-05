package com.sourabh.order_service.service;

import com.sourabh.order_service.dto.request.TrackingEventRequest;
import com.sourabh.order_service.dto.response.TrackingResponse;

import java.util.List;

/**
 * Service interface for recording and querying shipment tracking events.
 *
 * <p>Each order may accumulate multiple tracking events as its shipment
 * progresses through the logistics pipeline (e.g.&nbsp;PICKED → IN_TRANSIT →
 * OUT_FOR_DELIVERY → DELIVERED). Events capture carrier information,
 * location, and description text.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see TrackingResponse
 */
public interface ShipmentTrackingService {

    /**
     * Records a new tracking event for an order shipment.
     *
     * @param request tracking event details including order UUID, status,
     *                location, description, carrier name, and tracking number
     * @return {@link TrackingResponse} with the persisted event data and timestamp
     */
    TrackingResponse addTrackingEvent(TrackingEventRequest request);

    /**
     * Retrieves the complete tracking history for an order in chronological order.
     *
     * @param orderUuid UUID of the order whose tracking events are requested
     * @return list of {@link TrackingResponse} sorted by event time ascending;
     *         empty list if no events have been recorded yet
     */
    List<TrackingResponse> getTrackingHistory(String orderUuid);
}
