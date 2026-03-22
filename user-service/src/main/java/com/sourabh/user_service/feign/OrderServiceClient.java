package com.sourabh.user_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/order/internal/{uuid}/invoice")
    byte[] getInvoice(@PathVariable("uuid") String uuid,
                      @RequestHeader("X-Internal-Secret") String secret);
}
