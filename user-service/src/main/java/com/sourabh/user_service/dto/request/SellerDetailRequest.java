package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for seller to submit their business and ID verification details.
 */
@Getter
@Setter
public class SellerDetailRequest {

    /**


     * VALIDATION: This field is required and cannot be blank.


     * @NotBlank checks: not null, not empty string, not whitespace-only.


     * Triggers MethodArgumentNotValidException if violated (returns 400 Bad Request).


     */


    @NotBlank(message = "Business name is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String businessName;

    @NotBlank(message = "Business type is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String businessType; // INDIVIDUAL, COMPANY, PARTNERSHIP

    @Size(max = 20, message = "GST number must not exceed 20 characters")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String gstNumber;

    @Size(max = 15, message = "PAN number must not exceed 15 characters")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String panNumber;

    @NotBlank(message = "Address line 1 is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(min = 5, max = 255, message = "Address line 1 must be between 5 and 255 characters")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String addressLine1;

    @Size(max = 255)
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String addressLine2;

    @NotBlank(message = "City is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .'-]{2,100}$", message = "City must contain only letters and spaces")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String city;

    @NotBlank(message = "State is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .'-]{2,100}$", message = "State must contain only letters and spaces")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String state;

    @NotBlank(message = "Pincode is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String pincode;

    @NotBlank(message = "ID type is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    // Validation constraint
    // @NotBlank - Validates input before processing
    private String idType; // AADHAAR, PASSPORT, DRIVING_LICENSE

    @NotBlank(message = "ID number is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(max = 50)
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String idNumber;

    @NotBlank(message = "Bank account number is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Bank account number must be 9 to 18 digits")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String bankAccountNumber;

    @NotBlank(message = "Bank IFSC code is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC must be in valid format (e.g., HDFC0001234)")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String bankIfscCode;

    @NotBlank(message = "Bank name is required")
    // Validation constraint
    // @NotBlank - Validates input before processing
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .&'-]{2,100}$", message = "Bank name must contain only letters and spaces")
    // Validation constraint
    // @Size - Validates input before processing
    // Validation constraint
    // @Size - Validates input before processing
    private String bankName;
}
