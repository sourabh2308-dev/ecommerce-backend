package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.ApiResponse;
import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.*;
import com.sourabh.user_service.dto.response.InternalUserDto;
import com.sourabh.user_service.dto.InvoiceEmailRequest;
import com.sourabh.user_service.service.EmailService;
import com.sourabh.user_service.dto.response.SellerDetailResponse;
import com.sourabh.user_service.dto.response.UserResponse;
import com.sourabh.user_service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    private final EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully. OTP sent.", userService.registerUser(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOTP(
            @Valid @RequestBody VerifyOTPRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.verifyOTP(request), null));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOTP(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.resendOTP(email), null));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", userService.getProfile(userUuid)));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userService.updateProfile(userUuid, body)));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success(userService.changePassword(userUuid, body), null));
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/me/seller-details")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> submitSellerDetails(
            @Valid @RequestBody SellerDetailRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Seller details submitted successfully",
                userService.submitSellerDetails(userUuid, body)));
    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/me/seller-details")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> getSellerDetails(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Seller details fetched successfully",
                userService.getSellerDetails(userUuid)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/seller-details/{uuid}")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> getSellerDetailsByAdmin(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success("Seller details fetched successfully",
                userService.getSellerDetailsByAdmin(uuid)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/approve/{uuid}")
    public ResponseEntity<ApiResponse<String>> approveSeller(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.approveSeller(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/reject/{uuid}")
    public ResponseEntity<ApiResponse<String>> rejectSeller(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.rejectSeller(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/block/{uuid}")
    public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.blockUser(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/unblock/{uuid}")
    public ResponseEntity<ApiResponse<String>> unblockUser(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.unblockUser(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully",
                userService.getAllUsers(page, size, sortBy, direction, role, status)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<String>> softDelete(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.softDeleteUser(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/restore/{uuid}")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.restoreUser(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Search results fetched successfully",
                        userService.searchUsers(keyword, page, size)));
    }

    @GetMapping("/internal/email/{email}")
    public ResponseEntity<InternalUserDto> getUserByEmailInternal(@PathVariable String email) {
        log.debug("Internal lookup by email={}", email);
        InternalUserDto dto = userService.getUserByEmailInternal(email);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/internal/uuid/{uuid}")
    public ResponseEntity<InternalUserDto> getUserByUuidInternal(@PathVariable String uuid) {
        log.debug("Internal lookup by uuid={}", uuid);
        InternalUserDto dto = userService.getUserByUuidInternal(uuid);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping("/internal/invoice")
    public ResponseEntity<ApiResponse<String>> sendInvoiceInternal(@RequestBody InvoiceEmailRequest req) {
        log.debug("Internal invoice email request to={} order={}", req.getToEmail(), req.getOrderUuid());
        emailService.sendInvoiceEmail(req.getToEmail(), req.getOrderUuid(), req.getPdfBase64());
        return ResponseEntity.ok(ApiResponse.success("sent", null));
    }

    @PostMapping("/internal/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPasswordInternal(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.forgotPassword(email), null));
    }

    @PostMapping("/internal/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPasswordInternal(
            @RequestParam String email,
            @RequestParam String otpCode,
            @RequestParam String newPassword) {
        return ResponseEntity.ok(ApiResponse.success(userService.resetPassword(email, otpCode, newPassword), null));
    }
}
