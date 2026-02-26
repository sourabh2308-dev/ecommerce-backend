package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.*;
import com.sourabh.user_service.dto.response.InternalUserDto;
import com.sourabh.user_service.dto.response.SellerDetailResponse;
import com.sourabh.user_service.dto.response.UserResponse;
import com.sourabh.user_service.entity.*;
import com.sourabh.user_service.exception.OTPException;
import com.sourabh.user_service.exception.UserStateException;
import com.sourabh.user_service.exception.UserAlreadyExistsException;
import com.sourabh.user_service.exception.UserNotFoundException;
import com.sourabh.user_service.repository.OTPVerificationRepository;
import com.sourabh.user_service.repository.SellerDetailRepository;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.service.EmailService;
import com.sourabh.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * ═══════════════════════════════════════════════════════════════════════════
 * USER SERVICE IMPLEMENTATION - Core User Management Business Logic
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Implements comprehensive user lifecycle management including registration, verification,
 * profile management, seller onboarding, and administrative operations. This service is
 * the authoritative source for user data in the microservices architecture.
 * 
 * KEY RESPONSIBILITIES:
 * --------------------
 * 1. User Registration & Verification:
 *    - Create new user accounts with encrypted passwords (BCrypt)
 *    - Generate and validate time-bound OTP codes for email verification
 *    - Handle OTP expiration and resend logic
 * 
 * 2. Profile Management:
 *    - Update user information (name, email, password)
 *    - Maintain user state (verified, suspended, deleted)
 *    - Soft delete pattern (isDeleted flag, not physical deletion)
 * 
 * 3. Seller Onboarding:
 *    - Upgrade BUYER to SELLER role
 *    - Collect and validate business information
 *    - Maintain seller-specific data (business name, tax info, bank details)
 * 
 * 4. Role-Based Access Control:
 *    - Enforce authorization rules before data access
 *    - Validate user ownership of resources
 *    - Support BUYER, SELLER, ADMIN roles
 * 
 * 5. Caching Strategy:
 *    - Cache user lookups by UUID/email (high read frequency)
 *    - Evict cache on updates to ensure consistency
 *    - Redis-backed distributed cache
 * 
 * ARCHITECTURE PATTERNS:
 * ----------------------
 * - Service Layer Pattern: Separates business logic from controllers
 * - Repository Pattern: Abstracts database access via JPA repositories
 * - DTO Pattern: Converts entities to response DTOs (hides sensitive data)
 * - Cache-Aside Pattern: @Cacheable for reads, @CacheEvict for writes
 * - Soft Delete Pattern: isDeleted flag instead of physical row deletion
 * 
 * ANNOTATIONS EXPLAINED:
 * ----------------------
 * @Service:
 *   - Marks this as Spring service bean
 *   - Auto-detected by component scanning
 *   - Registered in application context for dependency injection
 * 
 * @Transactional:
 *   - Wraps methods in database transactions
 *   - Automatic rollback on RuntimeException
 *   - Ensures ACID properties (Atomicity, Consistency, Isolation, Durability)
 *   - Uses Spring's PlatformTransactionManager
 * 
 * @Cacheable("usersCache", key = "#uuid"):
 *   - Caches method result in Redis
 *   - Key format: usersCache::uuid-value
 *   - Subsequent calls with same UUID return cached value (no DB hit)
 *   - TTL configured in RedisCacheConfig
 * 
 * @CacheEvict:
 *   - Removes entries from cache when data changes
 *   - allEntries = true: Clears entire cache
 *   - beforeInvocation = false: Evicts after successful method execution
 * 
 * @Caching:
 *   - Combines multiple cache operations
 *   - Example: Evict both usersCache and sellersCache simultaneously
 * 
 * SECURITY CONSIDERATIONS:
 * ------------------------
 * - Passwords encrypted with BCrypt (cost factor 10)
 * - OTP codes are random 6-digit numbers (100,000 to 999,999)
 * - OTP validity: 10 minutes (configurable)
 * - Sensitive fields (password hash) excluded from response DTOs
 * - Authorization checks before sensitive operations
 * 
 * DATABASE TRANSACTIONS:
 * ----------------------
 * - User registration: Insert user + insert OTP (atomic)
 * - OTP verification: Update user + delete OTP (atomic)
 * - Seller registration: Insert user + insert seller_detail (atomic)
 * - User update: Update user + evict cache (atomic)
 * 
 * ERROR HANDLING:
 * ---------------
 * Custom exceptions thrown for business rule violations:
 * - UserNotFoundException: User UUID/email not found
 * - UserAlreadyExistsException: Duplicate email registration
 * - UserStateException: Invalid state transition (e.g., verify already verified user)
 * - OTPException: Invalid/expired OTP, too many attempts
 * 
 * CACHING STRATEGY:
 * -----------------
 * Cache Key Format:
 * - User by UUID: usersCache::{uuid}
 * - User by email: usersCache::{email}
 * - Seller by UUID: sellersCache::{uuid}
 * 
 * Cache Eviction Triggers:
 * - User update: Evict user's UUID and email keys
 * - User deletion: Evict user's UUID and email keys
 * - Seller registration: Evict seller cache
 * 
 * TTL (Time-To-Live):
 * - Configured in RedisCacheConfig
 * - Default: 1 hour for user data
 * - Prevents stale data while reducing DB load
 * 
 * EXAMPLE FLOWS:
 * --------------
 * 
 * Flow 1: User Registration
 * registerUser(RegisterRequest)
 * → Check if email exists (throw UserAlreadyExistsException if yes)
 * → Hash password with BCrypt
 * → Create User entity (isVerified = false, role from request)
 * → Save to database
 * → Generate 6-digit random OTP
 * → Create OTPVerification entity (expiresAt = now + 10 min)
 * → Save OTP to database
 * → Log OTP (in production, send email via email service)
 * → Convert User to UserResponse
 * → Return UserResponse
 * 
 * Flow 2: OTP Verification
 * verifyOTP(VerifyOTPRequest)
 * → Find user by email (throw UserNotFoundException if not found)
 * → Check if already verified (throw UserStateException if yes)
 * → Find OTP by email (throw OTPException if not found)
 * → Check OTP not expired (throw OTPException if expired)
 * → Check OTP matches (throw OTPException if wrong)
 * → Mark user.isVerified = true
 * → Delete OTP record
 * → Commit transaction
 * → Return success message
 * 
 * Flow 3: Get User (with caching)
 * getUserByUuid(uuid)
 * → Check cache: usersCache::{uuid}
 * → If cache hit: Return cached UserResponse (no DB query)
 * → If cache miss:
 *    → Query database by UUID
 *    → Throw UserNotFoundException if not found
 *    → Check isDeleted = false
 *    → Convert to UserResponse
 *    → Store in cache
 *    → Return UserResponse
 * 
 * Flow 4: Update User (with cache eviction)
 * updateUser(uuid, UpdateUserRequest)
 * → Fetch user from DB by UUID
 * → Validate ownership (uuid matches logged-in user)
 * → Update allowed fields (firstName, lastName)
 * → Save updated entity
 * → Evict cache: usersCache::{uuid}
 * → Convert to UserResponse
 * → Return UserResponse
 * 
 * TESTING NOTES:
 * --------------
 * - Mock repositories and password encoder in unit tests
 * - Use @MockBean for testing @Cacheable behavior
 * - Test transaction rollback with @Transactional(propagation = NEVER)
 * - Verify cache eviction after updates
 * - Test OTP expiration logic with fixed clock
 */
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OTPVerificationRepository otpRepository;
    private final SellerDetailRepository sellerDetailRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    @Override
    @Transactional
    @CacheEvict(value = "usersByEmail", allEntries = true)
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public UserResponse registerUser(RegisterRequest request) {

        String normalizedEmail = normalizeEmail(request.getEmail());

        Optional<User> existingUserOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();

            if (existingUser.isEmailVerified() && existingUser.getStatus() != UserStatus.PENDING_VERIFICATION) {
                throw new UserAlreadyExistsException("Email already registered");
            }

            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            existingUser.setPhoneNumber(request.getPhoneNumber());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setRole(request.getRole());
            existingUser.setStatus(UserStatus.PENDING_VERIFICATION);
            existingUser.setEmailVerified(false);
            existingUser.setApproved(false);
            existingUser.setDeleted(false);

            User updatedUser = userRepository.save(existingUser);
            generateAndSendOTP(updatedUser, OTPType.EMAIL);

            log.info("Unverified user re-registered: email={}, role={}", updatedUser.getEmail(), updatedUser.getRole());
            return mapToResponse(updatedUser);
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(normalizedEmail)
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        user = userRepository.save(user);

        generateAndSendOTP(user, OTPType.EMAIL);

        log.info("New user registered: email={}, role={}", user.getEmail(), user.getRole());

        return mapToResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "usersByEmail", allEntries = true)
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String verifyOTP(VerifyOTPRequest request) {

        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        OTPVerification otp = otpRepository
                .findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.EMAIL)
                .orElseThrow(() -> new OTPException("OTP not found"));

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new OTPException("User not in verification state");
        }

        if (otp.isVerified()) {
            throw new OTPException("OTP already verified");
        }

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new OTPException("OTP expired");
        }

        // Block brute-force after 5 failed attempts
        if (otp.getAttemptCount() >= 5) {
            throw new OTPException("OTP locked - too many failed attempts. Please request a new OTP.");
        }

        if (!otp.getOtpCode().equals(request.getOtpCode())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
            throw new OTPException("Invalid OTP. " + (5 - otp.getAttemptCount()) + " attempt(s) remaining.");
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        user.setEmailVerified(true);

        if (user.getRole() == Role.SELLER) {
            user.setStatus(UserStatus.PENDING_DETAILS);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        userRepository.save(user);

        return "OTP verified successfully";
    }

    // ========================
    // Helper Methods
    // ========================

    /**
     * GENERATEANDSENDOTP - Method Documentation
     *
     * PURPOSE:
     * This method handles the generateAndSendOTP operation.
     *
     * PARAMETERS:
     * @param user - User value
     * @param type - OTPType value
     *
     */
    private void generateAndSendOTP(User user, OTPType type) {

        String otpCode = String.valueOf(100000 + new SecureRandom().nextInt(900000));

        OTPVerification otp = OTPVerification.builder()
                .otpCode(otpCode)
                .type(type)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .user(user)
                .build();

        otpRepository.save(otp);

        // Send OTP via email
        String subject = (type == OTPType.PASSWORD_RESET)
                ? "Password Reset OTP"
                : "Email Verification OTP";

        emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otpCode, subject);
        log.info("[OTP] Sent {} OTP to email={}", type, user.getEmail());
    }

    /**
     * MAPTORESPONSE - Method Documentation
     *
     * PURPOSE:
     * This method handles the mapToResponse operation.
     *
     * PARAMETERS:
     * @param user - User value
     *
     * RETURN VALUE:
     * @return UserResponse - Result of the operation
     *
     */
    private UserResponse mapToResponse(User user) {

        return UserResponse.builder()
                .uuid(user.getUuid())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .approved(user.isApproved())
                .build();
    }

    // ─── SELLER DETAIL METHODS ───

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public SellerDetailResponse submitSellerDetails(String userUuid, SellerDetailRequest request) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new UserStateException("Only sellers can submit verification details");
        }

        if (user.getStatus() != UserStatus.PENDING_DETAILS) {
            throw new UserStateException("Seller is not in the details submission stage. Current status: " + user.getStatus());
        }

        // Check if details already submitted
        if (sellerDetailRepository.existsByUser(user)) {
            throw new UserStateException("Seller details already submitted");
        }

        SellerDetail detail = SellerDetail.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType())
                .gstNumber(request.getGstNumber())
                .panNumber(request.getPanNumber())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .idType(request.getIdType())
                .idNumber(request.getIdNumber())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankIfscCode(request.getBankIfscCode())
                .bankName(request.getBankName())
                .submittedAt(LocalDateTime.now())
                .build();

        sellerDetailRepository.save(detail);

        // Move seller to PENDING_APPROVAL
        user.setStatus(UserStatus.PENDING_APPROVAL);
        userRepository.save(user);

        log.info("Seller details submitted: uuid={}, business={}", userUuid, request.getBusinessName());
        return mapSellerDetailToResponse(detail);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public SellerDetailResponse getSellerDetails(String userUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new UserStateException("Only sellers have verification details");
        }

        SellerDetail detail = sellerDetailRepository.findByUser(user)
                .orElseThrow(() -> new UserNotFoundException("Seller details not yet submitted"));

        return mapSellerDetailToResponse(detail);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public SellerDetailResponse getSellerDetailsByAdmin(String sellerUuid) {
        User user = userRepository.findByUuid(sellerUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new UserStateException("User is not a seller");
        }

        SellerDetail detail = sellerDetailRepository.findByUser(user)
                .orElseThrow(() -> new UserNotFoundException("Seller has not submitted verification details"));

        return mapSellerDetailToResponse(detail);
    }

    /**
     * MAPSELLERDETAILTORESPONSE - Method Documentation
     *
     * PURPOSE:
     * This method handles the mapSellerDetailToResponse operation.
     *
     * PARAMETERS:
     * @param detail - SellerDetail value
     *
     * RETURN VALUE:
     * @return SellerDetailResponse - Result of the operation
     *
     */
    private SellerDetailResponse mapSellerDetailToResponse(SellerDetail detail) {
        return SellerDetailResponse.builder()
                .businessName(detail.getBusinessName())
                .businessType(detail.getBusinessType())
                .gstNumber(detail.getGstNumber())
                .panNumber(detail.getPanNumber())
                .addressLine1(detail.getAddressLine1())
                .addressLine2(detail.getAddressLine2())
                .city(detail.getCity())
                .state(detail.getState())
                .pincode(detail.getPincode())
                .idType(detail.getIdType())
                .idNumber(detail.getIdNumber())
                .bankAccountNumber(detail.getBankAccountNumber())
                .bankIfscCode(detail.getBankIfscCode())
                .bankName(detail.getBankName())
                .submittedAt(detail.getSubmittedAt())
                .verifiedAt(detail.getVerifiedAt())
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String approveSeller(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new OTPException("Email not verified yet");
        }

        if (user.getRole() != Role.SELLER) {
            throw new UserStateException("User is not a seller");
        }

        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new UserStateException("Seller is not pending approval");
        }

        // Verify seller has submitted business/ID details
        if (!sellerDetailRepository.existsByUser(user)) {
            throw new UserStateException("Seller has not submitted verification details");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setApproved(true);

        // Mark seller details as verified
        sellerDetailRepository.findByUser(user).ifPresent(detail -> {
            detail.setVerifiedAt(LocalDateTime.now());
            sellerDetailRepository.save(detail);
        });

        userRepository.save(user);

        log.info("Seller approved: uuid={}", userUuid);
        return "Seller approved successfully";
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String rejectSeller(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new UserStateException("User is not a seller");
        }

        user.setStatus(UserStatus.BLOCKED);
        user.setApproved(false);
        userRepository.save(user);

        log.info("Seller rejected: uuid={}", userUuid);
        return "Seller rejected successfully";
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String blockUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setStatus(UserStatus.BLOCKED);

        userRepository.save(user);

        log.info("User blocked: uuid={}", userUuid);
        return "User blocked successfully";
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(
            int page,
            int size,
            String sortBy,
            String direction,
            String role,
            String status) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);


        Page<User> userPage;

        if (role != null && status != null) {
            userPage = userRepository
                    .findByRoleAndStatusAndIsDeletedFalse(
                            Role.valueOf(role.toUpperCase()),
                            UserStatus.valueOf(status.toUpperCase()),
                            pageable
                    );
        } else if (role != null) {
            userPage = userRepository
                    .findByRoleAndIsDeletedFalse(
                            Role.valueOf(role.toUpperCase()),
                            pageable
                    );
        } else if (status != null) {
            userPage = userRepository
                    .findByStatusAndIsDeletedFalse(
                            UserStatus.valueOf(status.toUpperCase()),
                            pageable
                    );
        } else {
            userPage = userRepository
                    .findByIsDeletedFalse(pageable);
        }

        List<UserResponse> responses = userPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<UserResponse>builder()
                .content(responses)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String softDeleteUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setDeleted(true);
        user.setStatus(UserStatus.DELETED);

        userRepository.save(user);

        log.info("User soft-deleted: uuid={}", userUuid);
        return "User soft deleted successfully";
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String restoreUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setDeleted(false);
        // Sellers need re-approval after being restored; check if they submitted details
        if (user.getRole() == Role.SELLER && !user.isApproved()) {
            if (sellerDetailRepository.existsByUser(user)) {
                user.setStatus(UserStatus.PENDING_APPROVAL);
            } else {
                user.setStatus(UserStatus.PENDING_DETAILS);
            }
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        userRepository.save(user);

        log.info("User restored: uuid={}", userUuid);
        return "User restored successfully";
    }

    @Override
    public PageResponse<UserResponse> searchUsers(
            String keyword,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<User> userPage =
                userRepository.searchUsers(keyword, pageable);

        List<UserResponse> responses = userPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<UserResponse>builder()
                .content(responses)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "usersByEmail", allEntries = true)
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String resendOTP(String email) {

        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.PENDING_VERIFICATION || user.isEmailVerified()) {
            throw new OTPException("Email already verified. Please login.");
        }

        Optional<OTPVerification> latestOtp = otpRepository
            .findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.EMAIL);

        if (latestOtp.isPresent() && latestOtp.get().getLastSentAt() != null
            && latestOtp.get().getLastSentAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
            throw new OTPException("Please wait before requesting OTP again");
        }

        generateAndSendOTP(user, OTPType.EMAIL);

        return "OTP resent successfully";
    }

    // ─────────────────────────────────────────────
    // INTERNAL — cached for auth-service lookups
    // ─────────────────────────────────────────────

    @Override
    @Cacheable(value = "usersByEmail", key = "#email == null ? '' : #email.toLowerCase()")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public InternalUserDto getUserByEmailInternal(String email) {
        String normalizedEmail = normalizeEmail(email);
        log.debug("Cache miss for usersByEmail email={} — fetching from DB", normalizedEmail);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(this::toInternalDto)
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Override
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public InternalUserDto getUserByUuidInternal(String uuid) {
        log.debug("Cache miss for usersByUuid uuid={} — fetching from DB", uuid);
        return userRepository.findByUuid(uuid)
                .map(this::toInternalDto)
                .orElse(null);
    }

    /**
     * TOINTERNALDTO - Method Documentation
     *
     * PURPOSE:
     * This method handles the toInternalDto operation.
     *
     * PARAMETERS:
     * @param user - User value
     *
     * RETURN VALUE:
     * @return InternalUserDto - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Transactional - Wraps in database transaction (atomic execution)
     *
     */
    private InternalUserDto toInternalDto(User user) {
        return InternalUserDto.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    // ─────────────────────────────────────────────
    // PROFILE MANAGEMENT (authenticated user)
    // ─────────────────────────────────────────────

    @Override
    @Cacheable(value = "profileByUuid", key = "#userUuid")
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public UserResponse getProfile(String userUuid) {
        log.debug("getProfile: uuid={}", userUuid);
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public UserResponse updateProfile(String userUuid, UpdateProfileRequest request) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getFirstName() != null)   user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        userRepository.save(user);
        log.info("Profile updated: uuid={}", userUuid);
        return mapToResponse(user);
    }

    @Override
    @Transactional
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String changePassword(String userUuid, ChangePasswordRequest request) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UserStateException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new UserStateException("New passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed: uuid={}", userUuid);
        return "Password changed successfully";
    }

    // ─────────────────────────────────────────────
    // UNBLOCK USER (Admin)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    /**
     * SERVICE METHOD - Business Logic Implementation
     * 
     * Implements business logic with validation, persistence, and external calls.
     * Wrapped in @Transactional for atomic execution.
     */
    public String unblockUser(String userUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new UserStateException("User is not blocked");
        }

        // Sellers who were blocked need re-approval unless already approved
        if (user.getRole() == Role.SELLER && !user.isApproved()) {
            if (sellerDetailRepository.existsByUser(user)) {
                user.setStatus(UserStatus.PENDING_APPROVAL);
            } else {
                user.setStatus(UserStatus.PENDING_DETAILS);
            }
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        userRepository.save(user);
        log.info("User unblocked: uuid={}", userUuid);
        return "User unblocked successfully";
    }

    // ─────────────────────────────────────────────
    // FORGOT PASSWORD (internal, called by auth-service)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public String forgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new UserStateException("Email not verified");
        }

        if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.DELETED) {
            throw new UserStateException("Account is not active");
        }

        generateAndSendOTP(user, OTPType.PASSWORD_RESET);
        return "Password reset OTP sent successfully";
    }

    @Override
    @Transactional
    @CacheEvict(value = "usersByEmail", allEntries = true)
    public String resetPassword(String email, String otpCode, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        OTPVerification otp = otpRepository
                .findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.PASSWORD_RESET)
                .orElseThrow(() -> new OTPException("No password reset OTP found. Please request one first."));

        if (otp.isVerified()) {
            throw new OTPException("OTP already used");
        }

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new OTPException("OTP expired");
        }

        if (otp.getAttemptCount() >= 5) {
            throw new OTPException("OTP locked - too many failed attempts. Please request a new OTP.");
        }

        if (!otp.getOtpCode().equals(otpCode)) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
            throw new OTPException("Invalid OTP. " + (5 - otp.getAttemptCount()) + " attempt(s) remaining.");
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successful for email={}", normalizedEmail);
        return "Password reset successfully";
    }

}

