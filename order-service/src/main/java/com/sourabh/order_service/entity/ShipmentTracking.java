package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks shipping/delivery events for an order.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipment_tracking")
public class ShipmentTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingStatus status;

    private String location;

    private String description;

    private String carrier;

    private String trackingNumber;

    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    public enum TrackingStatus {
        ORDER_PLACED,
        PACKED,
        SHIPPED,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        RETURN_INITIATED,
        RETURN_PICKED_UP,
        RETURN_RECEIVED
    }
}
