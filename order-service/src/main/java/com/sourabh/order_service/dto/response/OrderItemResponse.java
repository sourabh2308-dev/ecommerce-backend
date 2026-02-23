package com.sourabh.order_service.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class OrderItemResponse {

    private String productUuid;
    private String sellerUuid;
    private Double price;
    private Integer quantity;
}
