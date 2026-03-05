package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for changing an authenticated user's password.
 *
 * <p>The caller must supply the current password for verification, the
 * desired new password, and a confirmation of the new password. All
 * fields are mandatory and validated via Bean Validation annotations.</p>
 */
@Getter
@Setter
public class ChangePasswordRequest {

    /** The user's current password, used for identity verification. */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /** The desired new password (minimum 8 characters). */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;

    /** Must match {@link #newPassword} exactly; verified in the service layer. */
    @NotBlank(message = "Confirm password is required")
    private String confirmNewPassword;
}
