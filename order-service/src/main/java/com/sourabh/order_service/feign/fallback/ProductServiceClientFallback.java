package com.sourabh.order_service.feign.fallback;

import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.exception.OrderStateException;
import com.sourabh.order_service.feign.ProductServiceClient;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    public ProductDto getProduct(String uuid) {
        throw new OrderStateException("Product service is currently unavailable. Please try again later.");
    }

    @Override
    public void reduceStock(String uuid, int quantity) {
        throw new OrderStateException("Product service is currently unavailable. Stock could not be reduced.");
    }

    @Override
    public void restoreStock(String uuid, int quantity) {
        // Best-effort: log and continue. A real implementation would use an outbox/retry mechanism.
        throw new OrderStateException("Product service is currently unavailable. Stock could not be restored.");
    }
}
