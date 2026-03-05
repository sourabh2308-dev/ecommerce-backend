package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.ApiResponse;
import com.sourabh.user_service.dto.request.AddressRequest;
import com.sourabh.user_service.dto.response.AddressResponse;
import com.sourabh.user_service.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing a user's shipping addresses.
 * <p>
 * All endpoints require an authenticated user. The user's UUID is
 * extracted from the {@code X-User-UUID} header injected by the
 * API Gateway after JWT validation.
 * </p>
 *
 * <p>Base path: {@code /api/user/me/addresses}</p>
 *
 * @see AddressService
 */
@RestController
@RequestMapping("/api/user/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    /** Service layer handling address business logic. */
    private final AddressService addressService;

    /**
     * Retrieves all addresses belonging to the authenticated user,
     * ordered with the default address first.
     *
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return list of {@link AddressResponse} wrapped in {@link ApiResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched", addressService.getAddresses(userUuid)));
    }

    /**
     * Creates a new address for the authenticated user.
     *
     * @param body    validated address payload
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the newly created {@link AddressResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Address added", addressService.addAddress(userUuid, body)));
    }

    /**
     * Updates an existing address identified by its UUID.
     *
     * @param addressUuid UUID of the address to update
     * @param body        validated address payload
     * @param request     the HTTP request carrying the {@code X-User-UUID} header
     * @return the updated {@link AddressResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{addressUuid}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable String addressUuid,
            @Valid @RequestBody AddressRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Address updated", addressService.updateAddress(userUuid, addressUuid, body)));
    }

    /**
     * Deletes an address identified by its UUID.
     *
     * @param addressUuid UUID of the address to delete
     * @param request     the HTTP request carrying the {@code X-User-UUID} header
     * @return confirmation message
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{addressUuid}")
    public ResponseEntity<ApiResponse<String>> deleteAddress(
            @PathVariable String addressUuid,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        addressService.deleteAddress(userUuid, addressUuid);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }

    /**
     * Sets the specified address as the user's default shipping address.
     *
     * @param addressUuid UUID of the address to set as default
     * @param request     the HTTP request carrying the {@code X-User-UUID} header
     * @return the updated {@link AddressResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{addressUuid}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @PathVariable String addressUuid,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Default address set", addressService.setDefaultAddress(userUuid, addressUuid)));
    }
}
