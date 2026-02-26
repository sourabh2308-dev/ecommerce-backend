package com.sourabh.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Detailed return/refund request for an order.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "return_request")
public class ReturnRequest extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uuid;

    @Column(nullable = false)
    private String orderUuid;

    @Column(nullable = false)
    private String buyerUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnType returnType;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.PENDING;

    private String adminNotes;

    private Double refundAmount;

    private LocalDateTime resolvedAt;

    @PrePersist
    private void generateUuid() {
        if (uuid == null) uuid = java.util.UUID.randomUUID().toString();
    }

    public enum ReturnStatus {
        PENDING, APPROVED, REJECTED, PICKUP_SCHEDULED, PICKED_UP, RECEIVED, REFUNDED, EXCHANGED
    }
}
