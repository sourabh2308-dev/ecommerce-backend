package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.ApiResponse;
import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.*;
import com.sourabh.user_service.dto.response.InternalUserDto;
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
 * ═══════════════════════════════════════════════════════════════════════════
 * USER MANAGEMENT CONTROLLER
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * REST API endpoints for comprehensive user lifecycle management in the e-commerce platform.
 * Handles user registration, OTP verification, profile management, seller onboarding,
 * and administrative operations. This controller serves as the central hub for all
 * user-related operations across BUYER, SELLER, and ADMIN roles.
 * 
 * KEY FUNCTIONALITIES:
 * --------------------
 * 1. PUBLIC ENDPOINTS (No Authentication):
 *    - User registration with email verification
 *    - OTP verification for account activation
 *    - OTP resend functionality
 * 
 * 2. AUTHENTICATED USER ENDPOINTS (Any verified user):
 *    - Profile retrieval (self)
 *    - Profile updates (name, password, etc.)
 *    - Seller registration for BUYERs upgrading to SELLER role
 *    - Internal user data fetch (called by other microservices)
 * 
 * 3. ROLE-SPECIFIC ENDPOINTS:
 *    - BUYER: Can register as seller, view own profile
 *    - SELLER: View own seller details, access seller-specific data
 *    - ADMIN: User management (suspend, delete, list all users/sellers)
 * 
 * ARCHITECTURE PATTERN:
 * ---------------------
 * - Controller Layer: Receives HTTP requests, validates input, extracts headers
 * - Delegates to Service Layer: All business logic in UserService
 * - Response Wrapping: Uses ApiResponse<T> for consistent JSON structure
 * - Pagination: Lists use PageResponse for efficient large dataset handling
 * 
 * ANNOTATIONS EXPLAINED:
 * ----------------------
 * @RestController:
 *   - Combines @Controller + @ResponseBody
 *   - Automatically serializes return objects to JSON
 *   - No need for manual HttpServletResponse writing
 * 
 * @RequestMapping("/api/user"):
 *   - Base path for all endpoints in this controller
 *   - All methods inherit this prefix (e.g., /api/user/register)
 * 
 * @RequiredArgsConstructor (Lombok):
 *   - Generates constructor injecting final UserService field
 *   - Preferred over @Autowired for testability
 * 
 * @Slf4j (Lombok):
 *   - Auto-generates: private static final Logger log
 *   - Used for request/response logging and debugging
 * 
 * @PreAuthorize("hasRole('ROLE')"):
 *   - Spring Security checks role before method execution
 *   - Throws AccessDeniedException if role doesn't match
 *   - Roles extracted from JWT by Spring Security filter chain
 * 
 * @Valid:
 *   - Triggers JSR-303 validation on @RequestBody
 *   - Validates @NotNull, @Email, @Size, @Pattern, etc.
 *   - Throws MethodArgumentNotValidException on failure
 * 
 * SECURITY MODEL:
 * ---------------
 * - Public routes: /register, /verify-otp, /resend-otp
 * - Authenticated routes: Require valid JWT token
 * - Role-based access: @PreAuthorize enforces BUYER/SELLER/ADMIN roles
 * - User context: X-User-UUID and X-User-Role headers injected by API Gateway
 * 
 * REQUEST/RESPONSE FLOW:
 * ----------------------
 * 1. API Gateway validates JWT token
 * 2. Gateway injects X-User-UUID and X-User-Role headers
 * 3. Controller extracts headers using HttpServletRequest
 * 4. @PreAuthorize checks if user has required role
 * 5. @Valid validates request body against bean validation rules
 * 6. Service layer processes business logic
 * 7. Controller wraps response in ApiResponse<T>
 * 8. Jackson serializes to JSON
 * 9. HTTP 200/201/400/403/404/500 returned
 * 
 * ERROR HANDLING:
 * ---------------
 * Exceptions thrown by service layer are caught by GlobalExceptionHandler:
 * - UserNotFoundException → 404 NOT FOUND
 * - UserAlreadyExistsException → 409 CONFLICT
 * - OTPException → 400 BAD REQUEST
 * - UserStateException → 400 BAD REQUEST
 * - MethodArgumentNotValidException → 400 BAD REQUEST (validation)
 * - AccessDeniedException → 403 FORBIDDEN
 * 
 * EXAMPLE API FLOWS:
 * ------------------
 * 
 * Flow 1: User Registration
 * POST /api/user/register
 * Request: { "firstName": "John", "email": "john@test.com", "password": "Pass@123", "role": "BUYER" }
 * → UserService.registerUser()
 * → Encrypt password with BCrypt
 * → Save user to database (isVerified = false)
 * → Generate 6-digit OTP
 * → Save OTP to otp_verification table (expires in 10 min)
 * → Send OTP email (simulated via logs in this project)
 * Response: { "success": true, "message": "User registered. OTP sent.", "data": UserResponse }
 * 
 * Flow 2: OTP Verification
 * POST /api/user/verify-otp
 * Request: { "email": "john@test.com", "otp": "123456" }
 * → Validate OTP not expired
 * → Match OTP value
 * → Set user.isVerified = true
 * → Delete OTP record
 * Response: { "success": true, "message": "Account verified successfully", "data": null }
 * 
 * Flow 3: Get My Profile (Authenticated)
 * GET /api/user/me
 * Headers: Authorization: Bearer <JWT>, X-User-UUID: abc-123
 * → Extract UUID from header
 * → Fetch user from database by UUID
 * → Convert to UserResponse (hide sensitive fields like password)
 * Response: { "success": true, "data": UserResponse }
 * 
 * Flow 4: Admin Lists All Users (Paginated)
 * GET /api/user/users?page=0&size=20&role=BUYER
 * Requires: @PreAuthorize("hasRole('ADMIN')")
 * → Pageable pagination object (page=0, size=20, sort by createdAt desc)
 * → Filter by role if provided
 * → Repository fetches Page<User>
 * → Convert to PageResponse<UserResponse>
 * Response: { "success": true, "data": { "content": [...], "totalElements": 150, "totalPages": 8 } }
 * 
 * TESTING NOTES:
 * --------------
 * - Mock UserService in unit tests to isolate controller logic
 * - Use @WebMvcTest(UserController.class) for focused testing
 * - MockMvc for simulating HTTP requests
 * - Verify @PreAuthorize with @WithMockUser(roles = "BUYER")
 * - Test validation failures with invalid request bodies
 */
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
        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully. OTP sent.", userService.registerUser(request)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOTP(
            @Valid @RequestBody VerifyOTPRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.verifyOTP(request), null));
    }

    @PostMapping("/resend-otp")
    /**
     * RESENDOTP - Method Documentation
     *
     * PURPOSE:
     * This method handles the resendOTP operation.
     *
     * PARAMETERS:
     * @param email - @RequestParam String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<String>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @PostMapping - REST endpoint handler
     * @Valid - Applied to this method
     * @PostMapping - REST endpoint handler
     *
     */
    public ResponseEntity<ApiResponse<String>> resendOTP(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.resendOTP(email), null));
    }

    // ─────────────────────────────────────────────
    // AUTHENTICATED USER ROUTES (any verified user)
    // ─────────────────────────────────────────────

    /** Get own profile. */
    @PreAuthorize("isAuthenticated()")
    /**

     * API ENDPOINT

     * 

     * HTTP Method: GET

     * 

     * PURPOSE:

     * Handles HTTP requests for this endpoint. Validates input, delegates to service

     * layer for business logic, and returns JSON response.

     * 

     * PROCESS FLOW:

     * 1. API Gateway forwards request after JWT validation

     * 2. Spring deserializes JSON to request object

     * 3. @Valid triggers bean validation (if present)

     * 4. @PreAuthorize checks user role (if present)

     * 5. Service layer executes business logic

     * 6. Response object serialized to JSON

     * 7. HTTP status code sent (200/201/400/403/404/500)

     * 

     * SECURITY:

     * - JWT validation at API Gateway (user authenticated)

     * - Role-based access via @PreAuthorize annotation

     * - Input validation via @Valid and constraint annotations

     * 

     * ERROR HANDLING:

     * - Service exceptions caught by GlobalExceptionHandler

     * - Returns standardized error response with HTTP status

     */

    @GetMapping("/me")
    /**
     * GETPROFILE - Method Documentation
     *
     * PURPOSE:
     * This method handles the getProfile operation.
     *
     * PARAMETERS:
     * @param request - HttpServletRequest value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<UserResponse>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @GetMapping - REST endpoint handler
     *
     */
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", userService.getProfile(userUuid)));
    }

    /** Update own profile (first/last name, phone). */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userService.updateProfile(userUuid, body)));
    }

    /** Change own password. */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success(userService.changePassword(userUuid, body), null));
    }

    // ─────────────────────────────────────────────
    // SELLER VERIFICATION ROUTES
    // ─────────────────────────────────────────────

    /** Seller submits business and ID verification details. */
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/me/seller-details")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> submitSellerDetails(
            @Valid @RequestBody SellerDetailRequest body,
            HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Seller details submitted successfully",
                userService.submitSellerDetails(userUuid, body)));
    }

    /** Seller views their own submitted details. */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/me/seller-details")
    /**
     * GETSELLERDETAILS - Method Documentation
     *
     * PURPOSE:
     * This method handles the getSellerDetails operation.
     *
     * PARAMETERS:
     * @param request - HttpServletRequest value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<SellerDetailResponse>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Valid - Applied to this method
     * @PreAuthorize - Security check before method execution
     * @GetMapping - REST endpoint handler
     *
     */
    public ResponseEntity<ApiResponse<SellerDetailResponse>> getSellerDetails(HttpServletRequest request) {
        String userUuid = request.getHeader("X-User-UUID");
        return ResponseEntity.ok(ApiResponse.success("Seller details fetched successfully",
                userService.getSellerDetails(userUuid)));
    }

    // ─────────────────────────────────────────────
    // ADMIN ROUTES
    // ─────────────────────────────────────────────

    /** Admin views a seller's verification details before approving. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/seller-details/{uuid}")
    /**
     * GETSELLERDETAILSBYADMIN - Method Documentation
     *
     * PURPOSE:
     * This method handles the getSellerDetailsByAdmin operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<SellerDetailResponse>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @PreAuthorize - Security check before method execution
     * @GetMapping - REST endpoint handler
     *
     */
    public ResponseEntity<ApiResponse<SellerDetailResponse>> getSellerDetailsByAdmin(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success("Seller details fetched successfully",
                userService.getSellerDetailsByAdmin(uuid)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/approve/{uuid}")
    /**
     * APPROVESELLER - Method Documentation
     *
     * PURPOSE:
     * This method handles the approveSeller operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<String>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @PreAuthorize - Security check before method execution
     * @GetMapping - REST endpoint handler
     * @PathVariable - Applied to this method
     * @PreAuthorize - Security check before method execution
     * @PutMapping - REST endpoint handler
     *
     */
    public ResponseEntity<ApiResponse<String>> approveSeller(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.approveSeller(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/reject/{uuid}")
    /**
     * REJECTSELLER - Method Documentation
     *
     * PURPOSE:
     * This method handles the rejectSeller operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<String>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @PreAuthorize - Security check before method execution
     * @PutMapping - REST endpoint handler
     * @PathVariable - Applied to this method
     * @PreAuthorize - Security check before method execution
     * @PutMapping - REST endpoint handler
     *
     */
    public ResponseEntity<ApiResponse<String>> rejectSeller(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.rejectSeller(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/block/{uuid}")
    /**
     * BLOCKUSER - Method Documentation
     *
     * PURPOSE:
     * This method handles the blockUser operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<String>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @PreAuthorize - Security check before method execution
     * @PutMapping - REST endpoint handler
     * @PathVariable - Applied to this method
     * @PreAuthorize - Security check before method execution
     * @PutMapping - REST endpoint handler
     *
     */
    public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.blockUser(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/unblock/{uuid}")
    /**
     * UNBLOCKUSER - Method Documentation
     *
     * PURPOSE:
     * This method handles the unblockUser operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<String>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @PreAuthorize - Security check before method execution
     * @PutMapping - REST endpoint handler
     * @PathVariable - Applied to this method
     * @PreAuthorize - Security check before method execution
     * @PutMapping - REST endpoint handler
     *
     */
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
    /**
     * SOFTDELETE - Method Documentation
     *
     * PURPOSE:
     * This method handles the softDelete operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<String>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @RequestParam - Applied to this method
     * @RequestParam - Applied to this method
     * @RequestParam - Applied to this method
     * @PreAuthorize - Security check before method execution
     * @DeleteMapping - REST endpoint handler
     *
     */
    public ResponseEntity<ApiResponse<String>> softDelete(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.softDeleteUser(uuid), null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/restore/{uuid}")
    /**
     * RESTORE - Method Documentation
     *
     * PURPOSE:
     * This method handles the restore operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<ApiResponse<String>> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @PreAuthorize - Security check before method execution
     * @DeleteMapping - REST endpoint handler
     * @PathVariable - Applied to this method
     * @PreAuthorize - Security check before method execution
     * @PutMapping - REST endpoint handler
     *
     */
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

    // ─────────────────────────────────────────────
    // INTERNAL ROUTES — reachable only with X-Internal-Secret
    // Returns InternalUserDto (contains hashed password for auth-service).
    // Must NEVER be exposed publicly.
    // ─────────────────────────────────────────────

    @GetMapping("/internal/email/{email}")
    /**
     * GETUSERBYEMAILINTERNAL - Method Documentation
     *
     * PURPOSE:
     * This method handles the getUserByEmailInternal operation.
     *
     * PARAMETERS:
     * @param email - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<InternalUserDto> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @GetMapping - REST endpoint handler
     *
     */
    public ResponseEntity<InternalUserDto> getUserByEmailInternal(@PathVariable String email) {
        log.debug("Internal lookup by email={}", email);
        InternalUserDto dto = userService.getUserByEmailInternal(email);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/internal/uuid/{uuid}")
    /**
     * GETUSERBYUUIDINTERNAL - Method Documentation
     *
     * PURPOSE:
     * This method handles the getUserByUuidInternal operation.
     *
     * PARAMETERS:
     * @param uuid - @PathVariable String value
     *
     * RETURN VALUE:
     * @return ResponseEntity<InternalUserDto> - Result of the operation
     *
     * ANNOTATIONS USED:
     * @GetMapping - REST endpoint handler
     * @PathVariable - Applied to this method
     * @GetMapping - REST endpoint handler
     *
     */
    public ResponseEntity<InternalUserDto> getUserByUuidInternal(@PathVariable String uuid) {
        log.debug("Internal lookup by uuid={}", uuid);
        InternalUserDto dto = userService.getUserByUuidInternal(uuid);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}
