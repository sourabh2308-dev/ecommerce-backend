package com.sourabh.review_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemDto {
    private String productUuid;
    private String sellerUuid;
}
