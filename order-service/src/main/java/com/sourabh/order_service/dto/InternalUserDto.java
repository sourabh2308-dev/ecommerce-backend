package com.sourabh.order_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Lightweight Data Transfer Object that mirrors the minimal user information
 * returned by the user-service's internal API.
 *
 * <p>Only the fields actually required by the order-service (UUID and email)
 * are included to keep inter-service payloads small and reduce coupling.</p>
 *
 * <p>Annotated with {@link Jacksonized} to support immutable deserialisation
 * via Jackson combined with Lombok's {@link Builder}.</p>
 */
@Getter
@Builder
@Jacksonized
public class InternalUserDto {

    /** Universally unique identifier of the user. */
    private String uuid;

    /** Email address of the user, used for sending invoices and notifications. */
    private String email;
}
