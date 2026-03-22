package com.sourabh.order_service.service;

import com.sourabh.order_service.dto.request.TrackingEventRequest;
import com.sourabh.order_service.dto.response.TrackingResponse;

import java.util.List;

public interface ShipmentTrackingService {

    TrackingResponse addTrackingEvent(TrackingEventRequest request);

    List<TrackingResponse> getTrackingHistory(String orderUuid);
}
