package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductResponse {

    private String uuid;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String category;
    private String sellerUuid;
    private String status;
}
