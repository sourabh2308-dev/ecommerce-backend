package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO representing a single shipment tracking event for an order.
 *
 * <p>Contains logistics details such as the current tracking status,
 * geographic location, carrier information, and the timestamp of the
 * event.</p>
 */
@Data
@Builder
public class TrackingResponse {

    /** UUID of the order associated with this tracking event. */
    private String orderUuid;

    /** Current tracking status (e.g., {@code "PICKED_UP"}, {@code "IN_TRANSIT"}, {@code "DELIVERED"}). */
    private String status;

    /** Geographic location at the time of the tracking event (nullable). */
    private String location;

    /** Human-readable description of the tracking event (nullable). */
    private String description;

    /** Name of the shipping carrier (e.g., "FedEx", "Delhivery"). */
    private String carrier;

    /** Carrier-assigned tracking number. */
    private String trackingNumber;

    /** Timestamp when the tracking event occurred. */
    private LocalDateTime eventTime;
}
