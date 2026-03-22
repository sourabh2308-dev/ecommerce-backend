package com.sourabh.product_service.common;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {

    private String errorCode;

    private String message;

    private List<String> details;

    private LocalDateTime timestamp;
}
