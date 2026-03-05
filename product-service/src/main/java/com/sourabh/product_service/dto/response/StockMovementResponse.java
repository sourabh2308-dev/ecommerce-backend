package com.sourabh.product_service.dto.response;

import com.sourabh.product_service.entity.StockMovement;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO exposing an individual stock-movement audit entry.
 *
 * <p>Stock movements are created whenever a product's inventory level changes
 * (manual adjustments, order fulfilment, returns, etc.) and serve as an
 * immutable audit trail.
 */
@Data
@Builder
public class StockMovementResponse {

    /** Internal database identifier of the movement record. */
    private Long id;

    /** UUID of the product whose stock was affected. */
    private String productUuid;

    /** Direction of the movement (e.g.&nbsp;{@code IN}, {@code OUT}). */
    private StockMovement.MovementType type;

    /** Number of units moved. */
    private Integer quantity;

    /** Stock level immediately after this movement was applied. */
    private Integer stockAfter;

    /** Optional reference linking this movement to an external event (e.g. order UUID). */
    private String reference;

    /** Timestamp when the movement was recorded. */
    private LocalDateTime createdAt;
}
