package com.sourabh.user_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long id;

    private String productUuid;

    private String productName;

    private String productImage;

    private double price;

    private int quantity;

    private double subtotal;

    private LocalDateTime createdAt;
}
