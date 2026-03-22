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

@RestController
@RequestMapping("/api/order/tracking")
@RequiredArgsConstructor
public class ShipmentTrackingController {

    private final ShipmentTrackingService trackingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<TrackingResponse> addEvent(@Valid @RequestBody TrackingEventRequest request) {
        return ResponseEntity.ok(trackingService.addTrackingEvent(request));
    }

    @GetMapping("/{orderUuid}")
    public ResponseEntity<List<TrackingResponse>> getHistory(@PathVariable String orderUuid) {
        return ResponseEntity.ok(trackingService.getTrackingHistory(orderUuid));
    }
}
