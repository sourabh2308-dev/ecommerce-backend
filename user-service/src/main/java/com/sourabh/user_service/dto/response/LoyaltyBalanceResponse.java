package com.sourabh.user_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoyaltyBalanceResponse {

    private String userUuid;

    private int balance;

    private double monetaryValue;
}
