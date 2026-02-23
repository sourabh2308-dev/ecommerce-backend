package com.sourabh.order_service.feign;

import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.feign.fallback.ProductServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "product-service",
        url = "${service.product.url}",
        fallback = ProductServiceClientFallback.class
)
public interface ProductServiceClient {

    @GetMapping("/api/product/{uuid}")
    ProductDto getProduct(@PathVariable("uuid") String uuid);

    @PutMapping("/api/product/internal/reduce-stock/{uuid}")
    void reduceStock(@PathVariable("uuid") String uuid, @RequestParam("quantity") int quantity);

    /**
     * Saga compensation: restore stock when payment fails.
     */
    @PutMapping("/api/product/internal/restore-stock/{uuid}")
    void restoreStock(@PathVariable("uuid") String uuid, @RequestParam("quantity") int quantity);
}
