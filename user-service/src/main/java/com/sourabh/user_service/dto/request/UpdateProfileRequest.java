package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for updating a user's profile information.
 *
 * <p>All fields are optional &mdash; clients may send only the fields
 * they wish to change. {@code null} fields are ignored by the service
 * layer during the update.</p>
 */
@Getter
@Setter
public class UpdateProfileRequest {

    /** Updated first name (1–50 characters). */
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    /** Updated last name (1–50 characters). */
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    /** Updated phone number (max 15 characters, international format). */
    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber;
}
