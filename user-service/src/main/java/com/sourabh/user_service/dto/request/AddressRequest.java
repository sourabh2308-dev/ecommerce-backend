package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating or updating an address.
 */
@Getter
@Setter
public class AddressRequest {

    private String label; // "Home", "Office", etc.

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Size(max = 15, message = "Phone number too long")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Size(max = 10, message = "Pincode too long")
    private String pincode;

    private boolean isDefault;
}
