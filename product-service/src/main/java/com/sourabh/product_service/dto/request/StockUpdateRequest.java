package com.sourabh.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockUpdateRequest {

    @NotNull @Min(1)
    private Integer quantity;

    private String reference;
}
