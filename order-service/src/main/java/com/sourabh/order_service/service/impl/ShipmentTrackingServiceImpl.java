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

@Service
@RequiredArgsConstructor
public class ShipmentTrackingServiceImpl implements ShipmentTrackingService {

    private final ShipmentTrackingRepository trackingRepository;

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
