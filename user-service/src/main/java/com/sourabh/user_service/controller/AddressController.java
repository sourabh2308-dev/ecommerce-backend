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

@RestController
@RequestMapping("/api/user/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched", addressService.getAddresses(userUuid)));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Address added", addressService.addAddress(userUuid, body)));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{addressUuid}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable String addressUuid,
            @Valid @RequestBody AddressRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Address updated", addressService.updateAddress(userUuid, addressUuid, body)));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{addressUuid}")
    public ResponseEntity<ApiResponse<String>> deleteAddress(
            @PathVariable String addressUuid,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        addressService.deleteAddress(userUuid, addressUuid);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{addressUuid}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @PathVariable String addressUuid,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Default address set", addressService.setDefaultAddress(userUuid, addressUuid)));
    }
}
