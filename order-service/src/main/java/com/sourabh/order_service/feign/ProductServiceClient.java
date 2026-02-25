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
/**
 * FEIGN CLIENT - Declarative REST Client for Inter-Service Communication
 * 
 * PURPOSE:
 * Enables synchronous HTTP calls to other microservices in a declarative way.
 * Feign generates implementation at runtime from this interface definition.
 * 
 * HOW IT WORKS:
 * 1. @FeignClient registers this interface with Spring Cloud
 * 2. Service discovery (Eureka) resolves service name to actual host:port
 * 3. Load balancer (Ribbon) selects instance if multiple replicas exist
 * 4. Circuit breaker (Resilience4j) wraps calls for fault tolerance
 * 5. Method invocation triggers HTTP request with specified path/method
 * 
 * ANNOTATIONS:
 * @FeignClient(name = "service-name")
 *   - name: Matches spring.application.name of target service
 *   - Enables service discovery via Eureka
 * 
 * @GetMapping/@PostMapping/@PutMapping/@DeleteMapping
 *   - Maps to HTTP methods
 *   - Path combines with @FeignClient base path
 * 
 * @PathVariable/@RequestParam/@RequestBody
 *   - Maps method parameters to HTTP request parts
 * 
 * ERROR HANDLING:
 * Throws FeignException on HTTP errors (4xx, 5xx)
 * Caller must handle exceptions and implement compensation logic
 * 
 * EXAMPLE FLOW:
 * OrderService → ProductServiceClient.reduceStock()
 *   → HTTP POST product-service/api/product/internal/{uuid}/reduce-stock
 *   → Product stock updated in product_db
 *   → Response or FeignException returned
 */
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
