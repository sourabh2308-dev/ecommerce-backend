package com.sourabh.order_service.feign.fallback;

import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.exception.OrderStateException;
import com.sourabh.order_service.feign.ProductServiceClient;
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
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    /**
     * GETPRODUCT - Method Documentation
     *
     * PURPOSE:
     * This method handles the getProduct operation.
     *
     * PARAMETERS:
     * @param uuid - String value
     *
     * RETURN VALUE:
     * @return ProductDto - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Override - Implements interface method
     *
     */
    public ProductDto getProduct(String uuid) {
        throw new OrderStateException("Product service is currently unavailable. Please try again later.");
    }

    @Override
    /**
     * REDUCESTOCK - Method Documentation
     *
     * PURPOSE:
     * This method handles the reduceStock operation.
     *
     * PARAMETERS:
     * @param uuid - String value
     * @param quantity - int value
     *
     * ANNOTATIONS USED:
     * @Override - Implements interface method
     * @Override - Implements interface method
     *
     */
    public void reduceStock(String uuid, int quantity) {
        throw new OrderStateException("Product service is currently unavailable. Stock could not be reduced.");
    }

    @Override
    /**
     * RESTORESTOCK - Method Documentation
     *
     * PURPOSE:
     * This method handles the restoreStock operation.
     *
     * PARAMETERS:
     * @param uuid - String value
     * @param quantity - int value
     *
     * ANNOTATIONS USED:
     * @Override - Implements interface method
     * @Override - Implements interface method
     *
     */
    public void restoreStock(String uuid, int quantity) {
        // Best-effort: log and continue. A real implementation would use an outbox/retry mechanism.
        throw new OrderStateException("Product service is currently unavailable. Stock could not be restored.");
    }
}
