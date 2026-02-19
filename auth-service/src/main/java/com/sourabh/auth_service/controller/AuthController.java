package com.sourabh.auth_service.controller;

import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.response.AuthResponse;
import com.sourabh.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestParam String refreshToken) {

        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }


    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "Hello Admin!";
    }

    @GetMapping("/seller-only")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerEndpoint() {
        return "Hello Seller!";
    }

    @GetMapping("/buyer-only")
    @PreAuthorize("hasRole('BUYER')")
    public String buyerEndpoint() {
        return "Hello Buyer!";
    }

}
