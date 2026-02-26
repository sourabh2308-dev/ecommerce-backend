package com.sourabh.product_service.dto.response;

import com.sourabh.product_service.entity.StockMovement;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload describing a stock movement entry.
 */
@Data
@Builder
public class StockMovementResponse {
    private Long id;
    private String productUuid;
    private StockMovement.MovementType type;
    private Integer quantity;
    private Integer stockAfter;
    private String reference;
    private LocalDateTime createdAt;
}
