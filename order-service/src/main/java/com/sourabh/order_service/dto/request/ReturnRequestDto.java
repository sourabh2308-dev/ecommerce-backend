package com.sourabh.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReturnRequestDto {

    @NotBlank
    private String orderUuid;

    @NotBlank
    private String returnType;

    @NotBlank
    private String reason;
}
