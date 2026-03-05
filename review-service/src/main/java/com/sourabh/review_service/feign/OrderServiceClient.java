package com.sourabh.review_service.feign;

import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.feign.fallback.OrderServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative Feign client for synchronous HTTP communication with the
 * <strong>order-service</strong>.
 *
 * <p>At runtime Spring Cloud OpenFeign generates an implementation of this
 * interface that translates method invocations into HTTP requests. The base
 * URL is resolved from the {@code service.order.url} property (typically
 * {@code http://order-service:8080}).
 *
 * <h3>Circuit Breaker</h3>
 * Resilience4j wraps every call with a circuit breaker
 * ({@code resilience4j.circuitbreaker.instances.order-service}). When the
 * breaker is open the {@link OrderServiceClientFallback} is invoked instead,
 * which throws a {@link com.sourabh.review_service.exception.ReviewException}
 * informing the caller that order verification is temporarily unavailable.
 *
 * <h3>Internal Authentication</h3>
 * The {@link com.sourabh.review_service.config.FeignConfig} request
 * interceptor automatically attaches the {@code X-Internal-Secret} header
 * to every outgoing request.
 *
 * @see OrderServiceClientFallback
 * @see com.sourabh.review_service.config.FeignConfig
 */
@FeignClient(
        name = "order-service",
        url = "${service.order.url}",
        fallback = OrderServiceClientFallback.class
)
public interface OrderServiceClient {

    /**
     * Retrieves an order by its UUID from the order-service.
     *
     * <p>Used during review creation to verify that the buyer owns the
     * order, the order has been delivered, and the order contains the
     * product being reviewed.
     *
     * @param uuid the unique identifier of the order to fetch
     * @return the {@link OrderDto} representing the order
     */
    @GetMapping("/api/order/{uuid}")
    OrderDto getOrder(@PathVariable("uuid") String uuid);
}
