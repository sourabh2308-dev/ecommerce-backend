package com.sourabh.user_service.service;

import com.sourabh.user_service.common.PageResponse;
import com.sourabh.user_service.dto.request.*;
import com.sourabh.user_service.dto.response.InternalUserDto;
import com.sourabh.user_service.dto.response.SellerDetailResponse;
import com.sourabh.user_service.dto.response.UserResponse;

/**
 * Core service interface for user lifecycle management.
 *
 * <p>Covers registration, OTP verification, profile updates, seller
 * on-boarding, administrative actions (block/unblock/delete/restore),
 * and internal service-to-service lookups consumed by the auth-service
 * via OpenFeign.</p>
 *
 * <p>Caching is applied on read-heavy paths ({@code getProfile},
 * {@code getUserByEmailInternal}) and evicted on every write operation
 * to ensure consistency across the Redis-backed distributed cache.</p>
 *
 * @see com.sourabh.user_service.service.impl.UserServiceImpl
 */
public interface UserService {

    /**
     * Registers a new user account. If the email already belongs to a
     * soft-deleted or unverified record the row is reused.  A 6-digit
     * OTP is generated and emailed for verification.
     *
     * @param request registration payload (name, email, password, phone, role)
     * @return the created {@link UserResponse}
     * @throws com.sourabh.user_service.exception.UserAlreadyExistsException if a verified, active account exists
     */
    UserResponse registerUser(RegisterRequest request);

    /**
     * Verifies the user's email address by validating the supplied OTP code.
     *
     * @param request email and OTP code
     * @return a success message string
     * @throws com.sourabh.user_service.exception.OTPException on invalid, expired, or locked OTP
     */
    String verifyOTP(VerifyOTPRequest request);

    /**
     * Resends a fresh OTP to the user's email. Rate-limited to one resend every 60 seconds.
     *
     * @param email the email address of the user awaiting verification
     * @return a confirmation message
     * @throws com.sourabh.user_service.exception.OTPException if the user is already verified or rate-limited
     */
    String resendOTP(String email);

    /**
     * Retrieves the authenticated user's own profile (cached by UUID).
     *
     * @param userUuid the UUID of the authenticated user
     * @return the user's {@link UserResponse}
     * @throws com.sourabh.user_service.exception.UserNotFoundException if the UUID is invalid
     */
    UserResponse getProfile(String userUuid);

    /**
     * Updates the authenticated user's profile fields (first name, last name, phone).
     *
     * @param userUuid the UUID of the authenticated user
     * @param request  fields to update (null fields are skipped)
     * @return the updated {@link UserResponse}
     */
    UserResponse updateProfile(String userUuid, UpdateProfileRequest request);

    /**
     * Changes the authenticated user's password after verifying the current one.
     *
     * @param userUuid the UUID of the authenticated user
     * @param request  current password, new password, and confirmation
     * @return a success message string
     * @throws com.sourabh.user_service.exception.UserStateException if the current password is wrong or new passwords don't match
     */
    String changePassword(String userUuid, ChangePasswordRequest request);

    /**
     * Submits seller verification details (business info, tax IDs, bank details).
     * Moves the seller's status from {@code PENDING_DETAILS} to {@code PENDING_APPROVAL}.
     *
     * @param userUuid the UUID of the seller
     * @param request  the seller detail payload
     * @return the persisted {@link SellerDetailResponse}
     * @throws com.sourabh.user_service.exception.UserStateException if the user is not a seller or not in the correct state
     */
    SellerDetailResponse submitSellerDetails(String userUuid, SellerDetailRequest request);

    /**
     * Returns the seller's own submitted verification details.
     *
     * @param userUuid the UUID of the seller
     * @return the {@link SellerDetailResponse}
     */
    SellerDetailResponse getSellerDetails(String userUuid);

    /**
     * Returns a seller's verification details for admin review before approval.
     *
     * @param sellerUuid the UUID of the seller to inspect
     * @return the {@link SellerDetailResponse}
     */
    SellerDetailResponse getSellerDetailsByAdmin(String sellerUuid);

    /**
     * Approves a seller account, setting status to {@code ACTIVE} and marking details as verified.
     *
     * @param userUuid the UUID of the seller to approve
     * @return a success message string
     */
    String approveSeller(String userUuid);

    /**
     * Rejects a seller account, blocking the user.
     *
     * @param userUuid the UUID of the seller to reject
     * @return a success message string
     */
    String rejectSeller(String userUuid);

    /**
     * Blocks a user account (sets status to {@code BLOCKED}).
     *
     * @param userUuid the UUID of the user to block
     * @return a success message string
     */
    String blockUser(String userUuid);

    /**
     * Unblocks a previously blocked user, restoring the appropriate status
     * based on their role and approval state.
     *
     * @param userUuid the UUID of the user to unblock
     * @return a success message string
     */
    String unblockUser(String userUuid);

    /**
     * Retrieves a paginated, optionally filtered and sorted list of all non-deleted users.
     *
     * @param page      zero-based page index
     * @param size      page size
     * @param sortBy    the field to sort by (e.g. "createdAt")
     * @param direction sort direction ("asc" or "desc")
     * @param role      optional role filter (e.g. "BUYER", "SELLER", "ADMIN")
     * @param status    optional status filter (e.g. "ACTIVE", "BLOCKED")
     * @return paginated {@link UserResponse} list
     */
    PageResponse<UserResponse> getAllUsers(
            int page,
            int size,
            String sortBy,
            String direction,
            String role,
            String status);

    /**
     * Soft-deletes a user by setting {@code isDeleted = true} and status to {@code DELETED}.
     *
     * @param userUuid the UUID of the user to delete
     * @return a success message string
     */
    String softDeleteUser(String userUuid);

    /**
     * Restores a soft-deleted user, resetting status based on role and approval state.
     *
     * @param userUuid the UUID of the user to restore
     * @return a success message string
     */
    String restoreUser(String userUuid);

    /**
     * Full-text search across user names and emails.
     *
     * @param keyword the search term
     * @param page    zero-based page index
     * @param size    page size
     * @return paginated {@link UserResponse} list matching the keyword
     */
    PageResponse<UserResponse> searchUsers(String keyword, int page, int size);

    /**
     * Internal endpoint consumed by auth-service via Feign to look up a user by email.
     * Returns the full {@link InternalUserDto} including the password hash.
     *
     * @param email the user's email address
     * @return the {@link InternalUserDto}, or {@code null} if not found
     */
    InternalUserDto getUserByEmailInternal(String email);

    /**
     * Internal endpoint to look up a user by UUID for service-to-service calls.
     *
     * @param uuid the user's UUID
     * @return the {@link InternalUserDto}, or {@code null} if not found
     */
    InternalUserDto getUserByUuidInternal(String uuid);

    /**
     * Initiates the forgot-password flow by generating and emailing a
     * {@code PASSWORD_RESET} OTP.
     *
     * @param email the user's registered email address
     * @return a confirmation message
     * @throws com.sourabh.user_service.exception.UserStateException if the email is not verified or the account is inactive
     */
    String forgotPassword(String email);

    /**
     * Resets the user's password after validating the password-reset OTP.
     *
     * @param email       the user's email address
     * @param otpCode     the OTP code received via email
     * @param newPassword the new password to set
     * @return a success message string
     * @throws com.sourabh.user_service.exception.OTPException on invalid, expired, or locked OTP
     */
    String resetPassword(String email, String otpCode, String newPassword);
}

