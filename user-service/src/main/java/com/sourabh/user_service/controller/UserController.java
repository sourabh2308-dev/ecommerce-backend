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

/**
 * REST controller for comprehensive user lifecycle management.
 * <p>
 * Handles user registration, OTP verification, profile management,
 * seller onboarding, administrative user operations, and internal
 * inter-service endpoints. This is the central hub for all
 * user-related operations across BUYER, SELLER, and ADMIN roles.
 * </p>
 *
 * <h3>Endpoint groups</h3>
 * <ul>
 *   <li><b>Public</b> – registration, OTP verification, OTP resend</li>
 *   <li><b>Authenticated</b> – profile CRUD, password change</li>
 *   <li><b>Seller</b> – submit / view seller verification details</li>
 *   <li><b>Admin</b> – approve/reject sellers, block/unblock users,
 *       list users, soft-delete/restore, search</li>
 *   <li><b>Internal</b> – service-to-service calls protected by
 *       {@code X-Internal-Secret} header (user lookup, invoice email,
 *       forgot/reset password)</li>
 * </ul>
 *
 * <p>Base path: {@code /api/user}</p>
 *
 * @see UserService
 * @see EmailService
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    /** Primary service handling user business logic. */
    private final UserService userService;

    /** Service for sending transactional emails (invoice, OTP, etc.). */
    private final EmailService emailService;

    /**
     * Registers a new user account and sends an email-verification OTP.
     *
     * @param request validated registration payload
     * @return the created {@link UserResponse}
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully. OTP sent.", userService.registerUser(request)));
    }

    /**
     * Verifies the OTP submitted by the user to activate their account.
     *
     * @param request validated OTP verification payload (email + OTP code)
     * @return confirmation message
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOTP(
            @Valid @RequestBody VerifyOTPRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.verifyOTP(request), null));
    }

    /**
     * Re-sends a fresh OTP to the specified email address.
     *
     * @param email the user's email address
     * @return confirmation message
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOTP(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.resendOTP(email), null));
    }

    /**
     * Returns the authenticated user's own profile.
     *
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the user's {@link UserResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", userService.getProfile(userUuid)));
    }

    /**
     * Updates the authenticated user's profile (first/last name, phone).
     *
     * @param body    validated profile-update payload
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the updated {@link UserResponse}
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userService.updateProfile(userUuid, body)));
    }

    /**
     * Changes the authenticated user's password after verifying the
     * current password.
     *
     * @param body    validated change-password payload
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return confirmation message
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success(userService.changePassword(userUuid, body), null));
    }

    /**
     * Allows a seller to submit their business and ID verification details.
     *
     * @param body    validated seller-detail payload
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the submitted {@link SellerDetailResponse}
     */
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/me/seller-details")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> submitSellerDetails(
            @Valid @RequestBody SellerDetailRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Seller details submitted successfully",
                userService.submitSellerDetails(userUuid, body)));
    }

    /**
     * Returns the authenticated seller's own submitted verification details.
     *
     * @param request the HTTP request carrying the {@code X-User-UUID} header
     * @return the seller's {@link SellerDetailResponse}
     */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/me/seller-details")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> getSellerDetails(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Seller details fetched successfully",
                userService.getSellerDetails(userUuid)));
    }

    /**
     * Admin-only: retrieves a seller's verification details for review.
     *
     * @param uuid UUID of the seller whose details to fetch
     * @return the seller's {@link SellerDetailResponse}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/seller-details/{uuid}")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> getSellerDetailsByAdmin(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success("Seller details fetched successfully",
                userService.getSellerDetailsByAdmin(uuid)));
    }

    /**
     * Admin-only: approves a seller's verification, activating their account.
     *
     * @param uuid UUID of the seller to approve
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/approve/{uuid}")
    public ResponseEntity<ApiResponse<String>> approveSeller(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.approveSeller(uuid), null));
    }

    /**
     * Admin-only: rejects a seller's verification submission.
     *
     * @param uuid UUID of the seller to reject
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/reject/{uuid}")
    public ResponseEntity<ApiResponse<String>> rejectSeller(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.rejectSeller(uuid), null));
    }

    /**
     * Admin-only: blocks (suspends) a user account.
     *
     * @param uuid UUID of the user to block
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/block/{uuid}")
    public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.blockUser(uuid), null));
    }

    /**
     * Admin-only: unblocks a previously suspended user account.
     *
     * @param uuid UUID of the user to unblock
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/unblock/{uuid}")
    public ResponseEntity<ApiResponse<String>> unblockUser(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.unblockUser(uuid), null));
    }

    /**
     * Admin-only: returns a paginated, filterable list of all users.
     *
     * @param page      zero-based page index (default 0)
     * @param size      page size (default 10)
     * @param sortBy    field to sort by (default {@code createdAt})
     * @param direction sort direction: {@code asc} or {@code desc} (default {@code desc})
     * @param role      optional role filter (BUYER, SELLER, ADMIN)
     * @param status    optional status filter (ACTIVE, BLOCKED, etc.)
     * @return paginated {@link UserResponse} list
     */
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

    /**
     * Admin-only: soft-deletes a user account (sets {@code isDeleted = true}).
     *
     * @param uuid UUID of the user to delete
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<String>> softDelete(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.softDeleteUser(uuid), null));
    }

    /**
     * Admin-only: restores a previously soft-deleted user account.
     *
     * @param uuid UUID of the user to restore
     * @return confirmation message
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/restore/{uuid}")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.restoreUser(uuid), null));
    }

    /**
     * Admin-only: searches users by keyword across email, name, and phone.
     *
     * @param keyword search term
     * @param page    zero-based page index (default 0)
     * @param size    page size (default 10)
     * @return paginated {@link UserResponse} list matching the keyword
     */
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

    /**
     * Internal endpoint: looks up a user by email and returns an
     * {@link InternalUserDto} containing the hashed password for
     * authentication by the auth-service. Protected by
     * {@code X-Internal-Secret}.
     *
     * @param email the user's email address
     * @return the matching {@link InternalUserDto}, or 404 if not found
     */
    @GetMapping("/internal/email/{email}")
    public ResponseEntity<InternalUserDto> getUserByEmailInternal(@PathVariable String email) {
        log.debug("Internal lookup by email={}", email);
        InternalUserDto dto = userService.getUserByEmailInternal(email);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    /**
     * Internal endpoint: looks up a user by UUID and returns an
     * {@link InternalUserDto}. Protected by {@code X-Internal-Secret}.
     *
     * @param uuid the user's UUID
     * @return the matching {@link InternalUserDto}, or 404 if not found
     */
    @GetMapping("/internal/uuid/{uuid}")
    public ResponseEntity<InternalUserDto> getUserByUuidInternal(@PathVariable String uuid) {
        log.debug("Internal lookup by uuid={}", uuid);
        InternalUserDto dto = userService.getUserByUuidInternal(uuid);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    /**
     * Internal endpoint: receives a Base64-encoded invoice PDF and
     * sends it as an email attachment. Called by order-service after
     * order delivery. Protected by {@code X-Internal-Secret}.
     *
     * @param req the invoice email request containing recipient, order UUID,
     *            and Base64-encoded PDF
     * @return confirmation message
     */
    @PostMapping("/internal/invoice")
    public ResponseEntity<ApiResponse<String>> sendInvoiceInternal(@RequestBody InvoiceEmailRequest req) {
        log.debug("Internal invoice email request to={} order={}", req.getToEmail(), req.getOrderUuid());
        emailService.sendInvoiceEmail(req.getToEmail(), req.getOrderUuid(), req.getPdfBase64());
        return ResponseEntity.ok(ApiResponse.success("sent", null));
    }

    /**
     * Internal endpoint: initiates the forgot-password flow by generating
     * a PASSWORD_RESET OTP and emailing it to the user. Called by
     * auth-service. Protected by {@code X-Internal-Secret}.
     *
     * @param email the user's email address
     * @return confirmation message
     */
    @PostMapping("/internal/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPasswordInternal(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.forgotPassword(email), null));
    }

    /**
     * Internal endpoint: resets the user's password after OTP verification.
     * Called by auth-service. Protected by {@code X-Internal-Secret}.
     *
     * @param email       the user's email address
     * @param otpCode     the OTP code for verification
     * @param newPassword the new plaintext password to set (will be hashed)
     * @return confirmation message
     */
    @PostMapping("/internal/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPasswordInternal(
            @RequestParam String email,
            @RequestParam String otpCode,
            @RequestParam String newPassword) {
        return ResponseEntity.ok(ApiResponse.success(userService.resetPassword(email, otpCode, newPassword), null));
    }
}
