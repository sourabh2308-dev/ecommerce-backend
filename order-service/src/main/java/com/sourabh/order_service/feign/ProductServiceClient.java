package com.sourabh.order_service.feign;

import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.feign.fallback.ProductServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Declarative Feign client for synchronous HTTP communication with the
 * product-service microservice.
 *
 * <p>Enables the order-service to:
 * <ul>
 *   <li>Fetch product details (name, price, stock, seller) before order creation.</li>
 *   <li>Reduce product stock when an order is confirmed.</li>
 *   <li>Restore product stock as a saga compensation step when payment fails.</li>
 * </ul>
 *
 * <p>Service discovery is handled by Eureka (via the logical name
 * {@code product-service}), with the base URL overridable through the
 * {@code service.product.url} property. If the product-service is
 * unavailable, the {@link ProductServiceClientFallback} is invoked.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ProductServiceClientFallback
 */
@FeignClient(
        name = "product-service",
        url = "${service.product.url}",
        fallback = ProductServiceClientFallback.class
)
public interface ProductServiceClient {

    /**
     * Retrieves product details from the product-service.
     *
     * @param uuid the product UUID
     * @return {@link ProductDto} containing name, price, stock, imageUrl, etc.
     */
    @GetMapping("/api/product/{uuid}")
    ProductDto getProduct(@PathVariable("uuid") String uuid);

    /**
     * Reduces the available stock of a product after order confirmation.
     *
     * @param uuid     the product UUID
     * @param quantity the number of units to deduct from stock
     */
    @PutMapping("/api/product/internal/reduce-stock/{uuid}")
    void reduceStock(@PathVariable("uuid") String uuid, @RequestParam("quantity") int quantity);

    /**
     * Restores product stock as a saga compensation step when payment
     * processing fails and the order is cancelled.
     *
     * @param uuid     the product UUID
     * @param quantity the number of units to restore
     */
    @PutMapping("/api/product/internal/restore-stock/{uuid}")
    void restoreStock(@PathVariable("uuid") String uuid, @RequestParam("quantity") int quantity);
}
