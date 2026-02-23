package com.sourabh.review_service.feign;

import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.feign.fallback.OrderServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "order-service",
        url = "${service.order.url}",
        fallback = OrderServiceClientFallback.class
)
public interface OrderServiceClient {

    @GetMapping("/api/order/{uuid}")
    OrderDto getOrder(@PathVariable("uuid") String uuid);
}
