package com.sourabh.order_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Minimal DTO matching the internal user response provided by user-service.
 * Only the fields required by order-service are included.
 */
@Getter
@Builder
@Jacksonized
public class InternalUserDto {
    private String uuid;
    private String email;
    // other fields omitted
}
