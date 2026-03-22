package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.*;
import com.sourabh.user_service.dto.response.InternalUserDto;
import com.sourabh.user_service.dto.response.SellerDetailResponse;
import com.sourabh.user_service.dto.response.UserResponse;

public interface UserService {

    UserResponse registerUser(RegisterRequest request);

    String verifyOTP(VerifyOTPRequest request);

    String resendOTP(String email);

    UserResponse getProfile(String userUuid);

    UserResponse updateProfile(String userUuid, UpdateProfileRequest request);

    String changePassword(String userUuid, ChangePasswordRequest request);

    SellerDetailResponse submitSellerDetails(String userUuid, SellerDetailRequest request);

    SellerDetailResponse getSellerDetails(String userUuid);

    SellerDetailResponse getSellerDetailsByAdmin(String sellerUuid);

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

    InternalUserDto getUserByEmailInternal(String email);

    InternalUserDto getUserByUuidInternal(String uuid);

    String forgotPassword(String email);

    String resetPassword(String email, String otpCode, String newPassword);
}

