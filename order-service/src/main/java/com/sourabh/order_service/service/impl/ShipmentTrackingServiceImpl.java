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
 * Implementation of {@link ShipmentTrackingService} that persists shipment
 * tracking events to PostgreSQL via Spring Data JPA.
 *
 * <p>Each tracking event captures a status change (e.g.&nbsp;PICKED,
 * IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED), the current location,
 * carrier name, tracking number, and an optional description.
 * Events are automatically timestamped by the database.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class ShipmentTrackingServiceImpl implements ShipmentTrackingService {

    /** Repository for persisting and querying {@link ShipmentTracking} entities. */
    private final ShipmentTrackingRepository trackingRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Builds a {@link ShipmentTracking} entity from the request fields,
     * persists it, and returns the mapped response including the
     * database-generated event timestamp.</p>
     *
     * @param request tracking event payload (order UUID, status, location,
     *                description, carrier, tracking number)
     * @return persisted tracking event as a {@link TrackingResponse}
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
     * {@inheritDoc}
     *
     * <p>Queries all tracking events for the given order UUID and returns
     * them sorted by {@code eventTime} ascending (oldest first), providing
     * the customer with a chronological shipment journey.</p>
     *
     * @param orderUuid UUID of the order
     * @return chronologically ordered list of {@link TrackingResponse};
     *         empty if no events exist yet
     */
    @Override
    @Transactional(readOnly = true)
    public List<TrackingResponse> getTrackingHistory(String orderUuid) {
        return trackingRepository.findByOrderUuidOrderByEventTimeAsc(orderUuid)
                .stream().map(this::mapToResponse).toList();
    }

    /**
     * Maps a {@link ShipmentTracking} entity to a {@link TrackingResponse} DTO.
     *
     * @param t the tracking entity to convert
     * @return populated response DTO
     */
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
