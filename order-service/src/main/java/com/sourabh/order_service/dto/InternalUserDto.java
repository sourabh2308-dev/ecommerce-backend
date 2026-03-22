package com.sourabh.order_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class InternalUserDto {

    private String uuid;

    private String email;
}
