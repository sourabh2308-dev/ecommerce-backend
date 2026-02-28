package com.sourabh.user_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Client for calling the order-service from within user-service.
 */
@FeignClient(name = "order-service")
public interface OrderServiceClient {

    /**
     * Fetch raw invoice PDF bytes using the internal endpoint. Requires the
     * shared X-Internal-Secret header, which is provided by the caller.
     */
    @GetMapping("/api/order/internal/{uuid}/invoice")
    byte[] getInvoice(@PathVariable("uuid") String uuid,
                      @RequestHeader("X-Internal-Secret") String secret);
}
