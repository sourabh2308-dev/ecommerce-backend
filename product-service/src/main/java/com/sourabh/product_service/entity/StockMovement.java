package com.sourabh.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks every stock change for audit purposes.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    @Column(nullable = false)
    private Integer quantity;

    /** Stock level after this movement */
    @Column(nullable = false)
    private Integer stockAfter;

    /** e.g. order UUID, manual adjustment note */
    private String reference;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MovementType {
        SALE,           // stock reduced via order
        RESTORATION,    // stock restored (cancelled order / failed payment)
        RESTOCK,        // seller added more stock
        ADJUSTMENT,     // manual admin/seller correction
        RESERVATION,    // temporarily reserved during checkout
        RELEASE         // reservation released (timeout/cancellation)
    }
}
