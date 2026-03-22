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
import org.springframework.dao.DataIntegrityViolationException;
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
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final OTPVerificationRepository otpRepository;

    private final SellerDetailRepository sellerDetailRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    @Override
    @Transactional
    @CacheEvict(value = "usersByEmail", allEntries = true)
    public UserResponse registerUser(RegisterRequest request) {

        String normalizedEmail = normalizeEmail(request.getEmail());

        Optional<User> existingUserOptional = userRepository.findByEmailIgnoreCaseForUpdate(normalizedEmail);

        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();

            if (!existingUser.isDeleted()
                    && existingUser.isEmailVerified()
                    && existingUser.getStatus() != UserStatus.PENDING_VERIFICATION) {
                throw new UserAlreadyExistsException("Email already registered");
            }

            User updatedUser = reRegisterExistingUser(existingUser, request);

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

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            User existingUser = userRepository.findByEmailIgnoreCaseForUpdate(normalizedEmail)
                .orElseThrow(() -> e);
            User updatedUser = reRegisterExistingUser(existingUser, request);

            log.info("Concurrent registration reconciled by reusing existing row: email={}, role={}",
                updatedUser.getEmail(), updatedUser.getRole());
            return mapToResponse(updatedUser);
        }

        generateAndSendOTP(user, OTPType.EMAIL);

        log.info("New user registered: email={}, role={}", user.getEmail(), user.getRole());

        return mapToResponse(user);
    }

    private User reRegisterExistingUser(User existingUser, RegisterRequest request) {
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
        return updatedUser;
    }

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

    @Override
    @Cacheable(value = "usersByEmail", key = "#email == null ? '' : #email.toLowerCase()")
    public InternalUserDto getUserByEmailInternal(String email) {
        String normalizedEmail = normalizeEmail(email);
        log.debug("Cache miss for usersByEmail email={} -- fetching from DB", normalizedEmail);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(this::toInternalDto)
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Override
    public InternalUserDto getUserByUuidInternal(String uuid) {
        log.debug("Cache miss for usersByUuid uuid={} -- fetching from DB", uuid);
        return userRepository.findByUuid(uuid)
                .map(this::toInternalDto)
                .orElse(null);
    }

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

    @Override
    @Cacheable(value = "profileByUuid", key = "#userUuid")
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
