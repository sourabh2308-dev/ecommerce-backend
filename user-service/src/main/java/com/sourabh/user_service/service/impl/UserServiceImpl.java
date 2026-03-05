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

/**
 * Implementation of {@link UserService} containing all core user-management business logic.
 *
 * <p>This service is the authoritative source for user data across the microservices
 * architecture.  It covers:</p>
 * <ul>
 *   <li><strong>Registration &amp; Verification</strong> &ndash; account creation with BCrypt
 *       password hashing and time-bound 6-digit OTP email verification.</li>
 *   <li><strong>Profile Management</strong> &ndash; name/phone updates, password changes,
 *       and soft-delete/restore with the {@code isDeleted} flag.</li>
 *   <li><strong>Seller On-boarding</strong> &ndash; business detail submission, admin
 *       approval/rejection, and status transitions through
 *       {@code PENDING_DETAILS -> PENDING_APPROVAL -> ACTIVE}.</li>
 *   <li><strong>Admin Operations</strong> &ndash; block/unblock, soft-delete/restore,
 *       paginated user listing with optional role/status filters.</li>
 *   <li><strong>Internal Lookups</strong> &ndash; email/UUID-based user retrieval consumed
 *       by auth-service via OpenFeign, with Redis caching for high read throughput.</li>
 *   <li><strong>Forgot/Reset Password</strong> &ndash; OTP-based password reset flow
 *       initiated from auth-service.</li>
 * </ul>
 *
 * <p><strong>Caching Strategy:</strong> Read-heavy paths ({@code getProfile},
 * {@code getUserByEmailInternal}) are cached in Redis.  Every write operation
 * evicts the relevant cache entries to ensure consistency.</p>
 *
 * @see UserService
 * @see UserRepository
 * @see OTPVerificationRepository
 * @see SellerDetailRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** JPA repository for {@link User} entity persistence. */
    private final UserRepository userRepository;

    /** JPA repository for {@link OTPVerification} entity persistence. */
    private final OTPVerificationRepository otpRepository;

    /** JPA repository for {@link SellerDetail} entity persistence. */
    private final SellerDetailRepository sellerDetailRepository;

    /** BCrypt password encoder for hashing and verifying passwords. */
    private final PasswordEncoder passwordEncoder;

    /** Email service for sending OTP and notification emails. */
    private final EmailService emailService;


    /**
     * {@inheritDoc}
     *
     * <p>If a soft-deleted or unverified row already exists for the given email,
     * it is reused (fields are overwritten) rather than creating a duplicate.
     * A fresh OTP is generated and emailed in all cases.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "usersByEmail", allEntries = true)
    public UserResponse registerUser(RegisterRequest request) {

        String normalizedEmail = normalizeEmail(request.getEmail());

        Optional<User> existingUserOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();

            if (!existingUser.isDeleted()
                    && existingUser.isEmailVerified()
                    && existingUser.getStatus() != UserStatus.PENDING_VERIFICATION) {
                throw new UserAlreadyExistsException("Email already registered");
            }

            existingUser.setDeleted(false);
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            existingUser.setPhoneNumber(request.getPhoneNumber());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setRole(request.getRole());
            existingUser.setStatus(UserStatus.PENDING_VERIFICATION);
            existingUser.setEmailVerified(false);
            existingUser.setApproved(false);

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

    /**
     * {@inheritDoc}
     *
     * <p>Validates the OTP against the most recent record for the user.  On success,
     * the user's {@code emailVerified} flag is set and their status advances to
     * {@code ACTIVE} (buyers) or {@code PENDING_DETAILS} (sellers).  Brute-force
     * protection locks the OTP after 5 failed attempts.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "usersByEmail", allEntries = true)
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

    /**
     * Generates a cryptographically random 6-digit OTP, persists it with a 5-minute
     * expiry, and sends it to the user's email address.
     *
     * @param user the user to send the OTP to
     * @param type the OTP type ({@code EMAIL} for registration, {@code PASSWORD_RESET} for forgot-password)
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

        String subject = (type == OTPType.PASSWORD_RESET)
                ? "Password Reset OTP"
                : "Email Verification OTP";

        emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otpCode, subject);
        log.info("[OTP] Sent {} OTP to email={}", type, user.getEmail());
    }

    /**
     * Converts a {@link User} entity to a {@link UserResponse} DTO, excluding
     * sensitive fields such as the password hash.
     *
     * @param user the user entity to convert
     * @return the client-safe response DTO
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

    /**
     * {@inheritDoc}
     *
     * <p>Only sellers in {@code PENDING_DETAILS} status may submit.  On success
     * the seller's status advances to {@code PENDING_APPROVAL}.</p>
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    public SellerDetailResponse submitSellerDetails(String userUuid, SellerDetailRequest request) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new UserStateException("Only sellers can submit verification details");
        }

        if (user.getStatus() != UserStatus.PENDING_DETAILS) {
            throw new UserStateException("Seller is not in the details submission stage. Current status: " + user.getStatus());
        }

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

        user.setStatus(UserStatus.PENDING_APPROVAL);
        userRepository.save(user);

        log.info("Seller details submitted: uuid={}, business={}", userUuid, request.getBusinessName());
        return mapSellerDetailToResponse(detail);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only users with role {@code SELLER} may call this endpoint.</p>
     */
    @Override
    @Transactional(readOnly = true)
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

    /**
     * {@inheritDoc}
     *
     * <p>Admin-only view of a seller's submitted verification details.</p>
     */
    @Override
    @Transactional(readOnly = true)
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
     * Converts a {@link SellerDetail} entity to a {@link SellerDetailResponse} DTO.
     *
     * @param detail the seller detail entity
     * @return the corresponding response DTO
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

    /**
     * {@inheritDoc}
     *
     * <p>Verifies that the seller's email is verified, role is {@code SELLER},
     * status is {@code PENDING_APPROVAL}, and business details have been submitted.
     * On approval, status becomes {@code ACTIVE} and {@code verifiedAt} is stamped.</p>
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
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

        if (!sellerDetailRepository.existsByUser(user)) {
            throw new UserStateException("Seller has not submitted verification details");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setApproved(true);

        sellerDetailRepository.findByUser(user).ifPresent(detail -> {
            detail.setVerifiedAt(LocalDateTime.now());
            sellerDetailRepository.save(detail);
        });

        userRepository.save(user);

        log.info("Seller approved: uuid={}", userUuid);
        return "Seller approved successfully";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets the seller's status to {@code BLOCKED} and clears the approval flag.</p>
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    public String blockUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setStatus(UserStatus.BLOCKED);

        userRepository.save(user);

        log.info("User blocked: uuid={}", userUuid);
        return "User blocked successfully";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Supports optional filtering by role and/or status.  Sorting direction
     * and field are configurable via parameters.</p>
     */
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    public String softDeleteUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setDeleted(true);
        user.setStatus(UserStatus.DELETED);

        userRepository.save(user);

        log.info("User soft-deleted: uuid={}", userUuid);
        return "User soft deleted successfully";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sellers who were soft-deleted need re-approval unless already approved.
     * The appropriate intermediate status ({@code PENDING_DETAILS} or
     * {@code PENDING_APPROVAL}) is restored based on whether business details
     * have been submitted.</p>
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    public String restoreUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setDeleted(false);
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

    /** {@inheritDoc} */
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

    /**
     * {@inheritDoc}
     *
     * <p>Rate-limited: if the most recent OTP was sent less than 60 seconds ago
     * the request is rejected to prevent abuse.</p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "usersByEmail", allEntries = true)
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

    /**
     * {@inheritDoc}
     *
     * <p>Results are cached in {@code usersByEmail} keyed by the lowercase email.
     * Returns {@code null} (not an exception) when the email is not found, allowing
     * auth-service to handle the 404 logic.</p>
     */
    @Override
    @Cacheable(value = "usersByEmail", key = "#email == null ? '' : #email.toLowerCase()")
    public InternalUserDto getUserByEmailInternal(String email) {
        String normalizedEmail = normalizeEmail(email);
        log.debug("Cache miss for usersByEmail email={} -- fetching from DB", normalizedEmail);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(this::toInternalDto)
                .orElse(null);
    }

    /**
     * Normalizes an email address by trimming whitespace and converting to lowercase.
     *
     * @param email the raw email input
     * @return the normalized email, or {@code null} if the input is null
     */
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** {@inheritDoc} */
    @Override
    public InternalUserDto getUserByUuidInternal(String uuid) {
        log.debug("Cache miss for usersByUuid uuid={} -- fetching from DB", uuid);
        return userRepository.findByUuid(uuid)
                .map(this::toInternalDto)
                .orElse(null);
    }

    /**
     * Converts a {@link User} entity to an {@link InternalUserDto} for
     * service-to-service communication.  Includes the password hash so that
     * auth-service can perform credential verification.
     *
     * @param user the user entity
     * @return the internal DTO with sensitive fields included
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

    /**
     * {@inheritDoc}
     *
     * <p>Cached in {@code profileByUuid} for fast repeated lookups by the frontend.</p>
     */
    @Override
    @Cacheable(value = "profileByUuid", key = "#userUuid")
    public UserResponse getProfile(String userUuid) {
        log.debug("getProfile: uuid={}", userUuid);
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToResponse(user);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only non-null fields in the request are applied to the entity.
     * Evicts both {@code profileByUuid} and {@code usersByEmail} caches.</p>
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
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

    /**
     * {@inheritDoc}
     *
     * <p>Verifies the current password before applying the change.  The new password
     * and confirmation must match.</p>
     */
    @Override
    @Transactional
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

    /**
     * {@inheritDoc}
     *
     * <p>For blocked sellers who have not yet been approved, the status is restored
     * to {@code PENDING_APPROVAL} or {@code PENDING_DETAILS} depending on whether
     * business details have been submitted.</p>
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "profileByUuid",  key = "#userUuid"),
            @CacheEvict(value = "usersByEmail", allEntries = true)
    })
    public String unblockUser(String userUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new UserStateException("User is not blocked");
        }

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

    /**
     * {@inheritDoc}
     *
     * <p>Validates that the user's email is verified and account is active before
     * generating a {@code PASSWORD_RESET} OTP.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Validates the password-reset OTP (expiry, attempt count, correctness),
     * then hashes and persists the new password.  The OTP is marked as verified
     * to prevent reuse.</p>
     */
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
