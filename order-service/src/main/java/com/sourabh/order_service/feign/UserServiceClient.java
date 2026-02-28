package com.sourabh.order_service.feign;

import com.sourabh.order_service.dto.InternalUserDto;
import com.sourabh.order_service.dto.InvoiceEmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client used by order-service to call the user-service internal endpoints.
 * The internal APIs are used exclusively for inter-service communication
 * (hence the `/internal` path segment) and are not exposed publicly.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/user/internal/uuid/{uuid}")
    InternalUserDto getUserByUuid(@PathVariable("uuid") String uuid);

    @PostMapping("/api/user/internal/invoice")
    void sendInvoice(@RequestBody InvoiceEmailRequest request);
}
