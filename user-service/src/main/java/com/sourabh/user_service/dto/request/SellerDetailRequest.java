package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for a seller to submit their business and identity
 * verification details during the onboarding process.
 *
 * <p>All mandatory fields are validated via Bean Validation annotations.
 * Once submitted, the details are reviewed by an admin before the seller
 * account is approved.</p>
 */
@Getter
@Setter
public class SellerDetailRequest {

    /** Registered business name (2–200 characters). */
    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    private String businessName;

    /** Type of business entity: INDIVIDUAL, COMPANY, or PARTNERSHIP. */
    @NotBlank(message = "Business type is required")
    private String businessType;

    /** GST registration number (optional, max 20 characters). */
    @Size(max = 20, message = "GST number must not exceed 20 characters")
    private String gstNumber;

    /** PAN (Permanent Account Number) for tax purposes (optional, max 15 characters). */
    @Size(max = 15, message = "PAN number must not exceed 15 characters")
    private String panNumber;

    /** Primary business address line (5–255 characters). */
    @NotBlank(message = "Address line 1 is required")
    @Size(min = 5, max = 255, message = "Address line 1 must be between 5 and 255 characters")
    private String addressLine1;

    /** Secondary address line (optional, max 255 characters). */
    @Size(max = 255)
    private String addressLine2;

    /** City of the business address (letters and spaces only). */
    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .'-]{2,100}$", message = "City must contain only letters and spaces")
    private String city;

    /** State or province of the business address (letters and spaces only). */
    @NotBlank(message = "State is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .'-]{2,100}$", message = "State must contain only letters and spaces")
    private String state;

    /** 6-digit postal PIN code. */
    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    /** Government-issued ID type: AADHAAR, PASSPORT, or DRIVING_LICENSE. */
    @NotBlank(message = "ID type is required")
    private String idType;

    /** Government-issued ID number (max 50 characters). */
    @NotBlank(message = "ID number is required")
    @Size(max = 50)
    private String idNumber;

    /** Bank account number (9–18 digits). */
    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Bank account number must be 9 to 18 digits")
    private String bankAccountNumber;

    /** Bank IFSC code in standard Indian format (e.g. HDFC0001234). */
    @NotBlank(message = "Bank IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC must be in valid format (e.g., HDFC0001234)")
    private String bankIfscCode;

    /** Name of the seller's bank (letters and spaces only, max 100 characters). */
    @NotBlank(message = "Bank name is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .&'-]{2,100}$", message = "Bank name must contain only letters and spaces")
    private String bankName;
}
