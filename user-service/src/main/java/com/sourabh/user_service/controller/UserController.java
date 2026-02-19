package com.sourabh.user_service.controller;

import com.sourabh.user_service.common.ApiResponse;
import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.RegisterRequest;
import com.sourabh.user_service.dto.request.VerifyOTPRequest;
import com.sourabh.user_service.dto.response.UserResponse;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = userService.registerUser(request);

        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully. OTP sent.", response)
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOTP(
            @Valid @RequestBody VerifyOTPRequest request) {

        String result = userService.verifyOTP(request);

        return ResponseEntity.ok(
                ApiResponse.success(result, null)
        );
    }

    @PutMapping("/admin/approve/{uuid}")
    public ResponseEntity<ApiResponse<String>> approveSeller(@PathVariable String uuid) {

        String result = userService.approveSeller(uuid);

        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PutMapping("/admin/reject/{uuid}")
    public ResponseEntity<ApiResponse<String>> rejectSeller(@PathVariable String uuid) {

        String result = userService.rejectSeller(uuid);

        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PutMapping("/admin/block/{uuid}")
    public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable String uuid) {

        String result = userService.blockUser(uuid);

        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        PageResponse<UserResponse> response =
                userService.getAllUsers(page, size, sortBy, direction, role, status);

        return ResponseEntity.ok(
                ApiResponse.success("Users fetched successfully", response)
        );
    }


    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<String>> softDelete(@PathVariable String uuid) {

        String result = userService.softDeleteUser(uuid);

        return ResponseEntity.ok(
                ApiResponse.success(result, null)
        );
    }

    @PutMapping("/restore/{uuid}")
    public ResponseEntity<ApiResponse<String>> restore(@PathVariable String uuid) {

        String result = userService.restoreUser(uuid);

        return ResponseEntity.ok(
                ApiResponse.success(result, null)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<UserResponse> response =
                userService.searchUsers(keyword, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Search results fetched successfully", response)
        );
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOTP(
            @RequestParam String email) {

        String result = userService.resendOTP(email);

        return ResponseEntity.ok(
                ApiResponse.success(result, null)
        );
    }

    @GetMapping("/internal/email/{email}")
    public User getUserByEmailInternal(@PathVariable String email) {
        return userRepository.findByEmail(email)
                .orElse(null);
    }




}
