package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating or updating a user's shipping/billing address.
 *
 * <p>All mandatory fields are annotated with {@link NotBlank} and validated
 * automatically when the controller parameter is annotated with
 * {@code @Valid}.</p>
 */
@Getter
@Setter
public class AddressRequest {

    /** Optional label such as "Home" or "Office". */
    private String label;

    /** Full name of the recipient at this address. */
    @NotBlank(message = "Full name is required")
    private String fullName;

    /** Contact phone number for delivery purposes. */
    @NotBlank(message = "Phone is required")
    @Size(max = 15, message = "Phone number too long")
    private String phone;

    /** Primary street address line. */
    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    /** Secondary address line (apartment, suite, etc.); optional. */
    private String addressLine2;

    /** City name. */
    @NotBlank(message = "City is required")
    private String city;

    /** State or province name. */
    @NotBlank(message = "State is required")
    private String state;

    /** Postal / PIN code. */
    @NotBlank(message = "Pincode is required")
    @Size(max = 10, message = "Pincode too long")
    private String pincode;

    /** Whether this address should be used as the user's default address. */
    private boolean isDefault;
}
