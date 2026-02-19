package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.RegisterRequest;
import com.sourabh.user_service.dto.request.VerifyOTPRequest;
import com.sourabh.user_service.dto.response.UserResponse;
import com.sourabh.user_service.entity.*;
import com.sourabh.user_service.exception.OTPException;
import com.sourabh.user_service.exception.UserAlreadyExistsException;
import com.sourabh.user_service.exception.UserNotFoundException;
import com.sourabh.user_service.repository.OTPVerificationRepository;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OTPVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        user = userRepository.save(user);

        generateAndSendOTP(user, OTPType.EMAIL);

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public String verifyOTP(VerifyOTPRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
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

        if (!otp.getOtpCode().equals(request.getOtpCode())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
            throw new OTPException("Invalid OTP");
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        user.setEmailVerified(true);

        if (user.getRole() == Role.SELLER) {
            user.setStatus(UserStatus.PENDING_APPROVAL);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        userRepository.save(user);

        return "OTP verified successfully";
    }

    // ========================
    // Helper Methods
    // ========================

    private void generateAndSendOTP(User user, OTPType type) {

        String otpCode = String.valueOf(100000 + new Random().nextInt(900000));

        OTPVerification otp = OTPVerification.builder()
                .otpCode(otpCode)
                .type(type)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .user(user)
                .build();

        otpRepository.save(otp);

        // Simulate sending OTP
        System.out.println("OTP for user " + user.getEmail() + " is: " + otpCode);
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
    public String approveSeller(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new OTPException("Email not verified yet");
        }

        if (user.getRole() != Role.SELLER) {
            throw new RuntimeException("User is not a seller");
        }

        if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Seller is not pending approval");
        }



        user.setStatus(UserStatus.ACTIVE);
        user.setApproved(true);

        userRepository.save(user);

        return "Seller approved successfully";
    }

    @Override
    @Transactional
    public String rejectSeller(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new RuntimeException("User is not a seller");
        }

        user.setStatus(UserStatus.BLOCKED);
        user.setApproved(false);
        userRepository.save(user);

        return "Seller rejected successfully";
    }

    @Override
    @Transactional
    public String blockUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setStatus(UserStatus.BLOCKED);

        userRepository.save(user);

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
    public String softDeleteUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setDeleted(true);
        user.setStatus(UserStatus.DELETED);

        userRepository.save(user);

        return "User soft deleted successfully";
    }

    @Override
    @Transactional
    public String restoreUser(String userUuid) {

        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setDeleted(false);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

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
    public String resendOTP(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        OTPVerification otp = otpRepository
                .findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.EMAIL)
                .orElseThrow(() -> new OTPException("No previous OTP found"));

        if (otp.getLastSentAt().plusSeconds(60)
                .isAfter(LocalDateTime.now())) {
            throw new OTPException("Please wait before requesting OTP again");
        }

        generateAndSendOTP(user, OTPType.EMAIL);

        return "OTP resent successfully";
    }






}
