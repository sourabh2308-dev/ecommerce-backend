package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @Positive
    private Double price;

    @Min(0)
    private Integer stock;

    private String category;
}

