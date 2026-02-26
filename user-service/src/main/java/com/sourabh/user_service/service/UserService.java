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

    // ─── Authenticated user (self) ───────────────────────────────────────────

    UserResponse getProfile(String userUuid);

    UserResponse updateProfile(String userUuid, UpdateProfileRequest request);

    String changePassword(String userUuid, ChangePasswordRequest request);

    // ─── Seller verification ─────────────────────────────────────────────────

    /** Seller submits business and ID verification details */
    SellerDetailResponse submitSellerDetails(String userUuid, SellerDetailRequest request);

    /** Seller views their own submitted details */
    SellerDetailResponse getSellerDetails(String userUuid);

    /** Admin views a seller's submitted details before approving */
    SellerDetailResponse getSellerDetailsByAdmin(String sellerUuid);

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

    // ─── Forgot Password (internal, called by auth-service) ─────────────

    String forgotPassword(String email);

    String resetPassword(String email, String otpCode, String newPassword);
}

