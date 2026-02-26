package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for recording a shipment tracking event for an order.
 */
@Data
public class TrackingEventRequest {

    @NotBlank
    private String orderUuid;

    @NotBlank
    private String status; // TrackingStatus enum name

    private String location;

    private String description;

    private String carrier;

    private String trackingNumber;
}
