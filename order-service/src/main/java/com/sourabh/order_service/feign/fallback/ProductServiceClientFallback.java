package com.sourabh.order_service.feign.fallback;

import com.sourabh.order_service.dto.ProductDto;
import com.sourabh.order_service.exception.OrderStateException;
import com.sourabh.order_service.feign.ProductServiceClient;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation of {@link ProductServiceClient} that is activated
 * when the product-service is unreachable or returns an error.
 *
 * <p>Every method throws an {@link OrderStateException} with a user-friendly
 * message so that callers receive a clear indication that the downstream
 * service is unavailable rather than an opaque Feign exception.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see ProductServiceClient
 */
@Component
public class ProductServiceClientFallback implements ProductServiceClient {

    /**
     * {@inheritDoc}
     *
     * @throws OrderStateException always — product-service is unavailable
     */
    @Override
    public ProductDto getProduct(String uuid) {
        throw new OrderStateException("Product service is currently unavailable. Please try again later.");
    }

    /**
     * {@inheritDoc}
     *
     * @throws OrderStateException always — product-service is unavailable;
     *         stock could not be reduced
     */
    @Override
    public void reduceStock(String uuid, int quantity) {
        throw new OrderStateException("Product service is currently unavailable. Stock could not be reduced.");
    }

    /**
     * {@inheritDoc}
     *
     * @throws OrderStateException always — product-service is unavailable;
     *         stock could not be restored
     */
    @Override
    public void restoreStock(String uuid, int quantity) {
        throw new OrderStateException("Product service is currently unavailable. Stock could not be restored.");
    }
}
