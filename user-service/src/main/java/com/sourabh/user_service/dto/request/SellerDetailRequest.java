package com.sourabh.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerDetailRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    private String businessName;

    @NotBlank(message = "Business type is required")
    private String businessType;

    @Size(max = 20, message = "GST number must not exceed 20 characters")
    private String gstNumber;

    @Size(max = 15, message = "PAN number must not exceed 15 characters")
    private String panNumber;

    @NotBlank(message = "Address line 1 is required")
    @Size(min = 5, max = 255, message = "Address line 1 must be between 5 and 255 characters")
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .'-]{2,100}$", message = "City must contain only letters and spaces")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .'-]{2,100}$", message = "State must contain only letters and spaces")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    @NotBlank(message = "ID type is required")
    private String idType;

    @NotBlank(message = "ID number is required")
    @Size(max = 50)
    private String idNumber;

    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Bank account number must be 9 to 18 digits")
    private String bankAccountNumber;

    @NotBlank(message = "Bank IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC must be in valid format (e.g., HDFC0001234)")
    private String bankIfscCode;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Za-z .&'-]{2,100}$", message = "Bank name must contain only letters and spaces")
    private String bankName;
}
