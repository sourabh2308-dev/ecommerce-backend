package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.ApiResponse;
import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.RegisterRequest;
import com.sourabh.user_service.dto.request.VerifyOTPRequest;
import com.sourabh.user_service.dto.response.InternalUserDto;
import com.sourabh.user_service.dto.response.UserResponse;
import com.sourabh.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // ─────────────────────────────────────────────
    // PUBLIC ROUTES (no JWT required)
    // ─────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = userService.registerUser(request);
        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully. OTP sent.", response));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOTP(
            @Valid @RequestBody VerifyOTPRequest request) {

        String result = userService.verifyOTP(request);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOTP(@RequestParam String email) {

        String result = userService.resendOTP(email);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    // ─────────────────────────────────────────────
    // ADMIN ROUTES (protected — X-User-Role: ADMIN)
    // ─────────────────────────────────────────────

    @PutMapping("/admin/approve/{uuid}")
    public ResponseEntity<ApiResponse<String>> approveSeller(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.approveSeller(uuid), null));
    }

    @PutMapping("/admin/reject/{uuid}")
    public ResponseEntity<ApiResponse<String>> rejectSeller(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.rejectSeller(uuid), null));
    }

    @PutMapping("/admin/block/{uuid}")
    public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.blockUser(uuid), null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        PageResponse<UserResponse> response =
                userService.getAllUsers(page, size, sortBy, direction, role, status);
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", response));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<String>> softDelete(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.softDeleteUser(uuid), null));
    }

    @PutMapping("/restore/{uuid}")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.restoreUser(uuid), null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<UserResponse> response = userService.searchUsers(keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Search results fetched successfully", response));
    }

    // ─────────────────────────────────────────────
    // INTERNAL ROUTES — reachable only with X-Internal-Secret
    // Returns InternalUserDto (contains hashed password for auth-service).
    // Must NEVER be exposed publicly.
    // ─────────────────────────────────────────────

    @GetMapping("/internal/email/{email}")
    public ResponseEntity<InternalUserDto> getUserByEmailInternal(
            @PathVariable String email) {
        log.debug("Internal lookup by email={}", email);
        InternalUserDto dto = userService.getUserByEmailInternal(email);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/internal/uuid/{uuid}")
    public ResponseEntity<InternalUserDto> getUserByUuidInternal(
            @PathVariable String uuid) {
        log.debug("Internal lookup by uuid={}", uuid);
        InternalUserDto dto = userService.getUserByUuidInternal(uuid);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}

