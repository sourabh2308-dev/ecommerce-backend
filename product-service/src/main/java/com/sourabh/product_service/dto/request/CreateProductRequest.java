package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    @Positive
    private Double price;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotBlank
    private String category;
}
