package com.sourabh.order_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDto {

    private String uuid;
    private String name;
    private Double price;
    private Integer stock;
    private String sellerUuid;
    private String category;
    private String imageUrl;
    private String status;
}
