package com.sourabh.user_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * OpenFeign declarative HTTP client for the {@code order-service}.
 * <p>
 * Enables the user-service to call order-service REST endpoints
 * without manual HTTP boilerplate. The service name is resolved
 * via Eureka service discovery.
 * </p>
 */
@FeignClient(name = "order-service")
public interface OrderServiceClient {

    /**
     * Fetches the raw invoice PDF for a delivered order.
     * <p>
     * Calls the internal order-service endpoint that is protected
     * by the shared {@code X-Internal-Secret} header rather than
     * a user JWT.
     * </p>
     *
     * @param uuid   the order's public UUID
     * @param secret the shared internal secret for service-to-service auth
     * @return the invoice PDF as a byte array
     */
    @GetMapping("/api/order/internal/{uuid}/invoice")
    byte[] getInvoice(@PathVariable("uuid") String uuid,
                      @RequestHeader("X-Internal-Secret") String secret);
}
