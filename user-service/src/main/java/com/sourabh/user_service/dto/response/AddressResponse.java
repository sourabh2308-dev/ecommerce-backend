package com.sourabh.user_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response payload representing a saved user address.
 *
 * <p>Returned by address management endpoints (create, update, list).
 * Contains all address fields plus audit timestamps and a flag indicating
 * whether this is the user's default shipping address.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    /** Unique identifier of the address record. */
    private String uuid;

    /** User-assigned label (e.g. "Home", "Office"). */
    private String label;

    /** Full name of the contact at this address. */
    private String fullName;

    /** Contact phone number. */
    private String phone;

    /** Primary street address line. */
    private String addressLine1;

    /** Secondary address line (apartment, suite, etc.). */
    private String addressLine2;

    /** City name. */
    private String city;

    /** State or province name. */
    private String state;

    /** Postal / PIN code. */
    private String pincode;

    /** {@code true} if this is the user's default address. */
    private boolean isDefault;

    /** Timestamp when the address was first created. */
    private LocalDateTime createdAt;

    /** Timestamp when the address was last modified. */
    private LocalDateTime updatedAt;
}
