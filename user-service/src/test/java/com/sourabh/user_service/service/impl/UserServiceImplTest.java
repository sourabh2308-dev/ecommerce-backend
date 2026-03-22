package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.dto.request.RegisterRequest;
import com.sourabh.user_service.entity.Role;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.entity.UserStatus;
import com.sourabh.user_service.exception.UserAlreadyExistsException;
import com.sourabh.user_service.repository.OTPVerificationRepository;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.service.EmailService;
import com.sourabh.user_service.dto.request.VerifyOTPRequest;
import com.sourabh.user_service.entity.OTPType;
import com.sourabh.user_service.entity.OTPVerification;
import com.sourabh.user_service.exception.OTPException;
import com.sourabh.user_service.exception.UserNotFoundException;
import com.sourabh.user_service.exception.UserStateException;
import com.sourabh.user_service.repository.SellerDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OTPVerificationRepository otpRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl service;
    
    @Mock
    private SellerDetailRepository sellerDetailRepository;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private RegisterRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = new RegisterRequest();
        sampleRequest.setEmail("foo@example.com");
        sampleRequest.setFirstName("Foo");
        sampleRequest.setLastName("Bar");
        sampleRequest.setPassword("Password1!");
        sampleRequest.setPhoneNumber("1234567890");
        sampleRequest.setRole(Role.BUYER);
        lenient().when(passwordEncoder.encode(any())).thenReturn("encoded");
    }

    @Test
    void register_throwsIfNonDeletedAlreadyExists() {
        User existing = User.builder()
                .email("foo@example.com")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("foo@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.registerUser(sampleRequest))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_allowsRecreationOfDeletedUser() {
        User existing = User.builder()
                .email("foo@example.com")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .isDeleted(true)
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("foo@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registerUser(sampleRequest);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(saved.getPassword()).isEqualTo("encoded");
        verify(emailService).sendOtpEmail(eq("foo@example.com"), any(), any(), any());
    }

    @Test
    @DisplayName("verifyOTP: buyer email verified → ACTIVE")
    void verifyOTP_buyer_setsActive() {
    User user = pendingUser(Role.BUYER);
    OTPVerification otp = validOtp(user, "123456");
    when(userRepository.findByEmailIgnoreCase("foo@example.com")).thenReturn(Optional.of(user));
    when(otpRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.EMAIL))
        .thenReturn(Optional.of(otp));
    when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    VerifyOTPRequest req = new VerifyOTPRequest();
    req.setEmail("foo@example.com");
    req.setOtpCode("123456");
    service.verifyOTP(req);

    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("verifyOTP: seller email verified → PENDING_DETAILS")
    void verifyOTP_seller_setsPendingDetails() {
    User user = pendingUser(Role.SELLER);
    OTPVerification otp = validOtp(user, "654321");
    when(userRepository.findByEmailIgnoreCase("foo@example.com")).thenReturn(Optional.of(user));
    when(otpRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.EMAIL))
        .thenReturn(Optional.of(otp));
    when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    VerifyOTPRequest req = new VerifyOTPRequest();
    req.setEmail("foo@example.com");
    req.setOtpCode("654321");
    service.verifyOTP(req);

    assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_DETAILS);
    }

    @Test
    @DisplayName("verifyOTP: wrong code throws OTPException and increments attempts")
    void verifyOTP_wrongCode_throwsOtpException() {
    User user = pendingUser(Role.BUYER);
    OTPVerification otp = validOtp(user, "111111");
    when(userRepository.findByEmailIgnoreCase("foo@example.com")).thenReturn(Optional.of(user));
    when(otpRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.EMAIL))
        .thenReturn(Optional.of(otp));
    when(otpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    VerifyOTPRequest req = new VerifyOTPRequest();
    req.setEmail("foo@example.com");
    req.setOtpCode("999999");

    assertThatThrownBy(() -> service.verifyOTP(req))
        .isInstanceOf(OTPException.class)
        .hasMessageContaining("Invalid OTP");

    assertThat(otp.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("verifyOTP: expired OTP throws OTPException")
    void verifyOTP_expiredOtp_throwsOtpException() {
    User user = pendingUser(Role.BUYER);
    OTPVerification otp = validOtp(user, "123456");
    otp.setExpiryTime(LocalDateTime.now().minusMinutes(1));
    when(userRepository.findByEmailIgnoreCase("foo@example.com")).thenReturn(Optional.of(user));
    when(otpRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.EMAIL))
        .thenReturn(Optional.of(otp));

    VerifyOTPRequest req = new VerifyOTPRequest();
    req.setEmail("foo@example.com");
    req.setOtpCode("123456");

    assertThatThrownBy(() -> service.verifyOTP(req))
        .isInstanceOf(OTPException.class)
        .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("verifyOTP: 5 failed attempts locks OTP")
    void verifyOTP_tooManyAttempts_throwsOtpException() {
    User user = pendingUser(Role.BUYER);
    OTPVerification otp = validOtp(user, "123456");
    otp.setAttemptCount(5);
    when(userRepository.findByEmailIgnoreCase("foo@example.com")).thenReturn(Optional.of(user));
    when(otpRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, OTPType.EMAIL))
        .thenReturn(Optional.of(otp));

    VerifyOTPRequest req = new VerifyOTPRequest();
    req.setEmail("foo@example.com");
    req.setOtpCode("123456");

    assertThatThrownBy(() -> service.verifyOTP(req))
        .isInstanceOf(OTPException.class)
        .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("approveSeller: PENDING_APPROVAL → ACTIVE")
    void approveSeller_success_setsActive() {
    User seller = sellerUser(UserStatus.PENDING_APPROVAL, true);
    when(userRepository.findByUuid("seller-uuid")).thenReturn(Optional.of(seller));
    when(sellerDetailRepository.existsByUser(seller)).thenReturn(true);
    when(sellerDetailRepository.findByUser(seller)).thenReturn(Optional.empty());
    when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    String result = service.approveSeller("seller-uuid");

    assertThat(seller.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(seller.isApproved()).isTrue();
    assertThat(result).contains("approved");
    }

    @Test
    @DisplayName("approveSeller: non-seller throws UserStateException")
    void approveSeller_nonSeller_throwsUserStateException() {
    User buyer = User.builder()
        .uuid("buyer-uuid")
        .role(Role.BUYER)
        .status(UserStatus.ACTIVE)
        .emailVerified(true)
        .build();
    when(userRepository.findByUuid("buyer-uuid")).thenReturn(Optional.of(buyer));

    assertThatThrownBy(() -> service.approveSeller("buyer-uuid"))
        .isInstanceOf(UserStateException.class)
        .hasMessageContaining("not a seller");
    }

    @Test
    @DisplayName("approveSeller: not PENDING_APPROVAL throws UserStateException")
    void approveSeller_wrongStatus_throwsUserStateException() {
    User seller = sellerUser(UserStatus.ACTIVE, true);
    when(userRepository.findByUuid("seller-uuid")).thenReturn(Optional.of(seller));

    assertThatThrownBy(() -> service.approveSeller("seller-uuid"))
        .isInstanceOf(UserStateException.class)
        .hasMessageContaining("not pending approval");
    }

    @Test
    @DisplayName("rejectSeller: PENDING_APPROVAL → BLOCKED")
    void rejectSeller_success_setsBlocked() {
    User seller = sellerUser(UserStatus.PENDING_APPROVAL, true);
    when(userRepository.findByUuid("seller-uuid")).thenReturn(Optional.of(seller));
    when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.rejectSeller("seller-uuid");

    assertThat(seller.getStatus()).isEqualTo(UserStatus.BLOCKED);
    assertThat(seller.isApproved()).isFalse();
    }

    @Test
    @DisplayName("blockUser: sets status to BLOCKED")
    void blockUser_success_setsBlocked() {
    User user = User.builder()
        .uuid("target-uuid")
        .role(Role.BUYER)
        .status(UserStatus.ACTIVE)
        .build();
    when(userRepository.findByUuid("target-uuid")).thenReturn(Optional.of(user));
    when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.blockUser("target-uuid");

    assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    @DisplayName("blockUser: user not found throws UserNotFoundException")
    void blockUser_notFound_throwsUserNotFoundException() {
    when(userRepository.findByUuid("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.blockUser("ghost"))
        .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("softDeleteUser: sets isDeleted and status DELETED")
    void softDeleteUser_success_setsDeletedFlag() {
    User user = User.builder()
        .uuid("target-uuid")
        .role(Role.BUYER)
        .status(UserStatus.ACTIVE)
        .isDeleted(false)
        .build();
    when(userRepository.findByUuid("target-uuid")).thenReturn(Optional.of(user));
    when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.softDeleteUser("target-uuid");

    assertThat(user.isDeleted()).isTrue();
    assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    private User pendingUser(Role role) {
    return User.builder()
        .email("foo@example.com")
        .firstName("Foo")
        .role(role)
        .status(UserStatus.PENDING_VERIFICATION)
        .emailVerified(false)
        .isDeleted(false)
        .build();
    }

    private User sellerUser(UserStatus status, boolean emailVerified) {
    return User.builder()
        .uuid("seller-uuid")
        .role(Role.SELLER)
        .status(status)
        .emailVerified(emailVerified)
        .isDeleted(false)
        .build();
    }

    private OTPVerification validOtp(User user, String code) {
    return OTPVerification.builder()
        .otpCode(code)
        .type(OTPType.EMAIL)
        .user(user)
        .verified(false)
        .attemptCount(0)
        .expiryTime(LocalDateTime.now().plusMinutes(5))
        .build();
    }
}
