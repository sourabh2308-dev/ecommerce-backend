package com.sourabh.review_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderDto {
    private String uuid;
    private String buyerUuid;
    private String status;
    private List<OrderItemDto> items;
}

