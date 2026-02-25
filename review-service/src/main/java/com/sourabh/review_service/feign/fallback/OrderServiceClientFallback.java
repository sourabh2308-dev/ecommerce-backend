package com.sourabh.review_service.feign.fallback;

import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.exception.ReviewException;
import com.sourabh.review_service.feign.OrderServiceClient;
import org.springframework.stereotype.Component;

@Component
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
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    /**
     * GETORDER - Method Documentation
     *
     * PURPOSE:
     * This method handles the getOrder operation.
     *
     * PARAMETERS:
     * @param uuid - String value
     *
     * RETURN VALUE:
     * @return OrderDto - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Override - Implements interface method
     *
     */
    public OrderDto getOrder(String uuid) {
        throw new ReviewException("Order service is currently unavailable. Cannot verify order.");
    }
}
