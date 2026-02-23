package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.ChangePasswordRequest;
import com.sourabh.user_service.dto.request.RegisterRequest;
import com.sourabh.user_service.dto.request.UpdateProfileRequest;
import com.sourabh.user_service.dto.request.VerifyOTPRequest;
import com.sourabh.user_service.dto.response.InternalUserDto;
import com.sourabh.user_service.dto.response.UserResponse;

public interface UserService {

    UserResponse registerUser(RegisterRequest request);

    String verifyOTP(VerifyOTPRequest request);

    String resendOTP(String email);

    // ─── Authenticated user (self) ───────────────────────────────────────────

    UserResponse getProfile(String userUuid);

    UserResponse updateProfile(String userUuid, UpdateProfileRequest request);

    String changePassword(String userUuid, ChangePasswordRequest request);

    // ─── Admin ───────────────────────────────────────────────────────────────

    String approveSeller(String userUuid);

    String rejectSeller(String userUuid);

    String blockUser(String userUuid);

    String unblockUser(String userUuid);

    PageResponse<UserResponse> getAllUsers(
            int page,
            int size,
            String sortBy,
            String direction,
            String role,
            String status);

    String softDeleteUser(String userUuid);

    String restoreUser(String userUuid);

    PageResponse<UserResponse> searchUsers(String keyword, int page, int size);

    // ─── Internal (service-to-service) ──────────────────────────────────────

    InternalUserDto getUserByEmailInternal(String email);

    InternalUserDto getUserByUuidInternal(String uuid);
}

