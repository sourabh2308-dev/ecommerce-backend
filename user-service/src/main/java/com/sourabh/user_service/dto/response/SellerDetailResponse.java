package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class SellerDetailResponse {

    private String businessName;

    private String businessType;

    private String gstNumber;

    private String panNumber;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String pincode;

    private String idType;

    private String idNumber;

    private String bankAccountNumber;

    private String bankIfscCode;

    private String bankName;

    private LocalDateTime submittedAt;

    private LocalDateTime verifiedAt;
}
