package com.sourabh.user_service.dto.response;

import lombok.*;

import java.util.List;

/**
 * Response payload for a user's cart summary.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private List<CartItemResponse> items;
    private int totalItems;
    private double totalAmount;
}
