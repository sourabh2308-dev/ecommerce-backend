package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.RegisterRequest;
import com.sourabh.user_service.dto.request.VerifyOTPRequest;
import com.sourabh.user_service.dto.response.UserResponse;

public interface UserService {

    UserResponse registerUser(RegisterRequest request);

    String verifyOTP(VerifyOTPRequest request);

    String approveSeller(String userUuid);

    String blockUser(String userUuid);

    String rejectSeller(String userUuid);

    PageResponse<UserResponse> getAllUsers(
            int page,
            int size,
            String sortBy,
            String direction,
            String role,
            String status
    );


    String softDeleteUser(String userUuid);

    String restoreUser(String userUuid);

    PageResponse<UserResponse> searchUsers(
            String keyword,
            int page,
            int size
    );

    String resendOTP(String email);




}
