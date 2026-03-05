package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * Response payload containing a seller's business and identity verification
 * details.
 *
 * <p>Returned after successful submission of seller onboarding data and
 * during admin review of pending seller applications.</p>
 */
@Getter
@Builder
@Jacksonized
public class SellerDetailResponse {

    /** Registered business name. */
    private String businessName;

    /** Type of business entity (INDIVIDUAL, COMPANY, PARTNERSHIP). */
    private String businessType;

    /** GST registration number (may be {@code null}). */
    private String gstNumber;

    /** PAN number (may be {@code null}). */
    private String panNumber;

    /** Primary business address line. */
    private String addressLine1;

    /** Secondary address line (may be {@code null}). */
    private String addressLine2;

    /** Business city. */
    private String city;

    /** Business state or province. */
    private String state;

    /** Postal PIN code. */
    private String pincode;

    /** Government-issued ID type used for verification. */
    private String idType;

    /** Government-issued ID number. */
    private String idNumber;

    /** Seller's bank account number. */
    private String bankAccountNumber;

    /** IFSC code of the seller's bank branch. */
    private String bankIfscCode;

    /** Name of the seller's bank. */
    private String bankName;

    /** Timestamp when the seller details were submitted. */
    private LocalDateTime submittedAt;

    /** Timestamp when an admin verified/approved the details (may be {@code null}). */
    private LocalDateTime verifiedAt;
}
