package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload for flash deal pricing and schedule details.
 */
@Data
@Builder
public class FlashDealResponse {
    private String uuid;
    private String productUuid;
    private String productName;
    private Double originalPrice;
    private Double discountPercent;
    private Double discountedPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
}
