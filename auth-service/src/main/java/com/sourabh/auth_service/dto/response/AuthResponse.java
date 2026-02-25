package com.sourabh.auth_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
/**
 * DATA TRANSFER OBJECT (DTO) - Server Response Format
 * 
 * Defines the JSON structure returned to HTTP clients.
 * Built from Entity objects via mapper methods.
 * May include computed fields not in database.
 */
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
