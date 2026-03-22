package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrackingEventRequest {

    @NotBlank
    private String orderUuid;

    @NotBlank
    private String status;

    private String location;

    private String description;

    private String carrier;

    private String trackingNumber;
}
