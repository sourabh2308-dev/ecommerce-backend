package com.sourabh.order_service.feign;

import com.sourabh.order_service.dto.InternalUserDto;
import com.sourabh.order_service.dto.InvoiceEmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative Feign client for synchronous HTTP communication with the
 * user-service microservice.
 *
 * <p>Targets the user-service's internal API endpoints (prefixed with
 * {@code /internal}) which are not exposed publicly and are protected
 * by the {@code X-Internal-Secret} header.</p>
 *
 * <p>Used by the order-service to:
 * <ul>
 *   <li>Fetch user profile details (name, email) for invoice generation.</li>
 *   <li>Delegate invoice email dispatch to the user-service.</li>
 * </ul>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Retrieves a user's profile by UUID from the user-service.
     *
     * @param uuid the user's UUID
     * @return {@link InternalUserDto} containing name, email, and role
     */
    @GetMapping("/api/user/internal/uuid/{uuid}")
    InternalUserDto getUserByUuid(@PathVariable("uuid") String uuid);

    /**
     * Sends a generated invoice PDF to the buyer's email address via the
     * user-service's email infrastructure.
     *
     * @param request the {@link InvoiceEmailRequest} containing the buyer
     *                UUID, order UUID, and Base64-encoded PDF bytes
     */
    @PostMapping("/api/user/internal/invoice")
    void sendInvoice(@RequestBody InvoiceEmailRequest request);
}
