package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity that records individual shipment / delivery tracking events
 * for an {@link Order}.
 *
 * <p>Each row represents a discrete event in the order’s shipping journey
 * (e.g. packed, shipped, in-transit, delivered). Events are appended
 * by sellers or admins through the
 * {@link com.sourabh.order_service.controller.ShipmentTrackingController}
 * and displayed in chronological order to buyers.</p>
 *
 * <p>Mapped to the {@code shipment_tracking} database table.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see Order
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipment_tracking")
public class ShipmentTracking {

    /**
     * Database-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID of the order this tracking event belongs to.
     */
    @Column(nullable = false)
    private String orderUuid;

    /**
     * Status snapshot at the time this tracking event was created.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingStatus status;

    /**
     * Optional human-readable location description (e.g. "Mumbai Hub").
     */
    private String location;

    /**
     * Free-text description of the event (e.g. "Package arrived at sort facility").
     */
    private String description;

    /**
     * Name of the shipping carrier (e.g. "BlueDart", "Delhivery").
     */
    private String carrier;

    /**
     * Carrier-assigned tracking / AWB number for the shipment.
     */
    private String trackingNumber;

    /**
     * Timestamp of when this tracking event occurred. Defaults to the
     * current system time at entity instantiation.
     */
    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    /**
     * Enumeration of shipment tracking statuses covering the forward
     * delivery and reverse (return) logistics flows.
     */
    public enum TrackingStatus {
        /** Order has been placed and is awaiting packing. */
        ORDER_PLACED,
        /** Order has been packed and is ready for dispatch. */
        PACKED,
        /** Order has been handed over to the shipping carrier. */
        SHIPPED,
        /** Package is in transit between hubs. */
        IN_TRANSIT,
        /** Package is out for last-mile delivery. */
        OUT_FOR_DELIVERY,
        /** Package has been delivered to the buyer. */
        DELIVERED,
        /** A return has been initiated for this shipment. */
        RETURN_INITIATED,
        /** Returned package has been picked up from the buyer. */
        RETURN_PICKED_UP,
        /** Returned package has been received at the warehouse. */
        RETURN_RECEIVED
    }
}
