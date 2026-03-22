package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrackingResponse {

    private String orderUuid;

    private String status;

    private String location;

    private String description;

    private String carrier;

    private String trackingNumber;

    private LocalDateTime eventTime;
}
