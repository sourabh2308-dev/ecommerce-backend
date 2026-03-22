package com.sourabh.review_service.feign.fallback;

import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.exception.ReviewException;
import com.sourabh.review_service.feign.OrderServiceClient;
import org.springframework.stereotype.Component;

@Component
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    public OrderDto getOrder(String uuid) {
        throw new ReviewException("Order service is currently unavailable. Cannot verify order.");
    }
}
