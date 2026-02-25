package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * UPDATE PROFILE REQUEST DTO
 * 
 * Data Transfer Object for user profile update requests.
 * Contains fields that users can modify in their profile.
 * 
 * VALIDATION:
 * - All fields are optional (user can update selectively)
 * - @Size constraints ensure reasonable lengths
 * - Validated by @Valid annotation in controller
 */
@Getter
@Setter
public class UpdateProfileRequest {

    // First name: 1-50 characters
    // @Size validates minimum and maximum length
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    // Last name: 1-50 characters
    // @Size validates minimum and maximum length
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    // Phone number: up to 15 characters (international format)
    // @Size validates maximum length
    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber;
}
