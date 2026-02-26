package com.sourabh.user_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response payload describing a wishlist item.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {

    private Long id;
    private String productUuid;
    private String productName;
    private String productImage;
    private double price;
    private LocalDateTime createdAt;
}
