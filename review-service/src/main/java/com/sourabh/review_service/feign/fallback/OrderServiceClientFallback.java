package com.sourabh.review_service.feign.fallback;

import com.sourabh.review_service.dto.OrderDto;
import com.sourabh.review_service.exception.ReviewException;
import com.sourabh.review_service.feign.OrderServiceClient;
import org.springframework.stereotype.Component;

/**
 * Resilience4j circuit-breaker fallback for {@link OrderServiceClient}.
 *
 * <p>When the order-service is unreachable or the circuit breaker is open,
 * Spring Cloud invokes the corresponding method on this class instead of
 * performing the actual HTTP call. Every method throws a
 * {@link ReviewException} so that callers receive a clear,
 * domain-specific error rather than a generic Feign/connection exception.
 *
 * @see OrderServiceClient
 */
@Component
public class OrderServiceClientFallback implements OrderServiceClient {

    /**
     * Fallback for {@link OrderServiceClient#getOrder(String)}.
     *
     * <p>Always throws a {@link ReviewException} indicating that order
     * verification is temporarily unavailable.
     *
     * @param uuid the order UUID that could not be fetched
     * @return never returns normally
     * @throws ReviewException always thrown with a descriptive message
     */
    @Override
    public OrderDto getOrder(String uuid) {
        throw new ReviewException("Order service is currently unavailable. Cannot verify order.");
    }
}
