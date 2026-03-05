package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO submitted (typically by a seller or logistics system) to
 * record a new shipment tracking event for an order.
 *
 * <p>Required fields are the order UUID and the tracking status. Optional
 * fields provide additional logistics detail such as current location,
 * carrier name, and tracking number.</p>
 */
@Data
public class TrackingEventRequest {

    /** UUID of the order associated with this tracking event (required). */
    @NotBlank
    private String orderUuid;

    /** Name of the {@code TrackingStatus} enum value (e.g., {@code "IN_TRANSIT"}) (required). */
    @NotBlank
    private String status;

    /** Optional geographic location at the time of the event. */
    private String location;

    /** Optional human-readable description of the tracking event. */
    private String description;

    /** Optional name of the shipping carrier (e.g., "FedEx", "Delhivery"). */
    private String carrier;

    /** Optional carrier-assigned tracking number. */
    private String trackingNumber;
}
