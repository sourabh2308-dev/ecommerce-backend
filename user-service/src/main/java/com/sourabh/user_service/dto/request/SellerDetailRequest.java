package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for seller to submit their business and ID verification details.
 */
@Getter
@Setter
public class SellerDetailRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 200)
    private String businessName;

    @NotBlank(message = "Business type is required")
    private String businessType; // INDIVIDUAL, COMPANY, PARTNERSHIP

    @Size(max = 20, message = "GST number must not exceed 20 characters")
    private String gstNumber;

    @Size(max = 15, message = "PAN number must not exceed 15 characters")
    private String panNumber;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Size(max = 10)
    private String pincode;

    @NotBlank(message = "ID type is required")
    private String idType; // AADHAAR, PASSPORT, DRIVING_LICENSE

    @NotBlank(message = "ID number is required")
    @Size(max = 50)
    private String idNumber;

    @NotBlank(message = "Bank account number is required")
    @Size(max = 30)
    private String bankAccountNumber;

    @NotBlank(message = "Bank IFSC code is required")
    @Size(max = 20)
    private String bankIfscCode;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100)
    private String bankName;
}
