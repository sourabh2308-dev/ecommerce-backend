package com.sourabh.auth_service.service.impl;

import com.sourabh.auth_service.dto.UserDto;
import com.sourabh.auth_service.dto.request.ForgotPasswordRequest;
import com.sourabh.auth_service.dto.request.LoginRequest;
import com.sourabh.auth_service.dto.request.ResetPasswordRequest;
import com.sourabh.auth_service.dto.response.AuthResponse;
import com.sourabh.auth_service.entity.RefreshToken;
import com.sourabh.auth_service.exception.AuthException;
import com.sourabh.auth_service.exception.UserAccountException;
import com.sourabh.auth_service.repository.RefreshTokenRepository;
import com.sourabh.auth_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private RestTemplate restTemplate;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserDto activeUser;

    @BeforeEach
    void setUp() {
        
        ReflectionTestUtils.setField(authService, "internalSecret", "test-internal-secret");
        ReflectionTestUtils.setField(authService, "userServiceBaseUrl", "http://user-service:8080");
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L); 

        activeUser = new UserDto();
        activeUser.setUuid("user-uuid");
        activeUser.setEmail("user@example.com");
        activeUser.setPassword("$2b$hashed");
        activeUser.setRole("BUYER");
        activeUser.setStatus("ACTIVE");
        activeUser.setEmailVerified(true);
    }

    @Test
    @DisplayName("login: success — valid credentials return AuthResponse")
    void login_success_returnsTokenPair() {
        LoginRequest req = loginRequest("user@example.com", "Password1!");

        when(restTemplate.exchange(anyString(), any(), any(), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok(activeUser));
        when(passwordEncoder.matches("Password1!", "$2b$hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("access-jwt");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-jwt");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login: fails — user not found returns AuthException")
    void login_userNotFound_throwsAuthException() {
        LoginRequest req = loginRequest("unknown@example.com", "any");
        when(restTemplate.exchange(anyString(), any(), any(), eq(UserDto.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("login: fails — email not verified throws UserAccountException")
    void login_emailNotVerified_throwsUserAccountException() {
        activeUser.setEmailVerified(false);
        LoginRequest req = loginRequest("user@example.com", "Password1!");
        when(restTemplate.exchange(anyString(), any(), any(), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok(activeUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UserAccountException.class)
                .hasMessageContaining("Email not verified");
    }

    @Test
    @DisplayName("login: fails — account not in allowed status throws UserAccountException")
    void login_inactiveStatus_throwsUserAccountException() {
        activeUser.setStatus("BLOCKED");
        LoginRequest req = loginRequest("user@example.com", "Password1!");
        when(restTemplate.exchange(anyString(), any(), any(), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok(activeUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UserAccountException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("login: fails — wrong password throws AuthException")
    void login_wrongPassword_throwsAuthException() {
        LoginRequest req = loginRequest("user@example.com", "WrongPass");
        when(restTemplate.exchange(anyString(), any(), any(), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("login: success — PENDING_DETAILS seller can log in")
    void login_pendingDetailsSeller_succeeds() {
        activeUser.setStatus("PENDING_DETAILS");
        activeUser.setRole("SELLER");
        LoginRequest req = loginRequest("user@example.com", "Password1!");

        when(restTemplate.exchange(anyString(), any(), any(), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("seller-jwt");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.login(req);
        assertThat(response.getAccessToken()).isEqualTo("seller-jwt");
    }

    @Test
    @DisplayName("login: email is normalised to lower-case")
    void login_emailNormalised() {
        LoginRequest req = loginRequest("  USER@Example.COM  ", "Password1!");

        when(restTemplate.exchange(contains("user@example.com"), any(), any(), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("tok");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.login(req);

        verify(restTemplate).exchange(contains("user@example.com"), any(), any(), eq(UserDto.class));
    }

    @Test
    @DisplayName("refreshToken: success — rotates token pair")
    void refreshToken_success_rotatesTokens() {
        RefreshToken token = validRefreshToken("rt-value", "user-uuid");
        when(refreshTokenRepository.findByToken("rt-value")).thenReturn(Optional.of(token));
        when(restTemplate.exchange(anyString(), any(), any(), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok(activeUser));
        when(jwtUtil.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("new-access");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.refreshToken("rt-value");

        assertThat(token.isRevoked()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isNotEqualTo("rt-value"); 
    }

    @Test
    @DisplayName("refreshToken: fails — unknown token throws AuthException")
    void refreshToken_unknownToken_throwsAuthException() {
        when(refreshTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("refreshToken: fails — revoked token throws AuthException")
    void refreshToken_revokedToken_throwsAuthException() {
        RefreshToken token = validRefreshToken("rt-value", "user-uuid");
        token.setRevoked(true);
        when(refreshTokenRepository.findByToken("rt-value")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refreshToken("rt-value"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("refreshToken: fails — expired token throws AuthException")
    void refreshToken_expiredToken_throwsAuthException() {
        RefreshToken token = expiredRefreshToken("rt-value", "user-uuid");
        when(refreshTokenRepository.findByToken("rt-value")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refreshToken("rt-value"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("refreshToken: fails — blocked user throws UserAccountException")
    void refreshToken_blockedUser_throwsUserAccountException() {
        activeUser.setStatus("BLOCKED");
        RefreshToken token = validRefreshToken("rt-value", "user-uuid");
        when(refreshTokenRepository.findByToken("rt-value")).thenReturn(Optional.of(token));
        when(restTemplate.exchange(anyString(), any(), any(), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok(activeUser));

        assertThatThrownBy(() -> authService.refreshToken("rt-value"))
                .isInstanceOf(UserAccountException.class)
                .hasMessageContaining("no longer active");
    }

    @Test
    @DisplayName("logout: success — marks token as revoked")
    void logout_success_revokesToken() {
        RefreshToken token = validRefreshToken("rt-value", "user-uuid");
        when(refreshTokenRepository.findByToken("rt-value")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.logout("rt-value");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("logout: idempotent — already-revoked token not saved again")
    void logout_alreadyRevoked_idempotent() {
        RefreshToken token = validRefreshToken("rt-value", "user-uuid");
        token.setRevoked(true);
        when(refreshTokenRepository.findByToken("rt-value")).thenReturn(Optional.of(token));

        authService.logout("rt-value");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("logout: fails — unknown token throws AuthException")
    void logout_unknownToken_throwsAuthException() {
        when(refreshTokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("ghost"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("forgotPassword: always returns generic message")
    void forgotPassword_returnsGenericMessage() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("any@example.com");
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        String result = authService.forgotPassword(req);

        assertThat(result).contains("OTP has been sent");
    }

    @Test
    @DisplayName("forgotPassword: swallows HttpClientErrorException — returns same message")
    void forgotPassword_swallowsHttpError() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("noone@example.com");
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        String result = authService.forgotPassword(req);

        assertThat(result).contains("OTP has been sent");
    }

    @Test
    @DisplayName("resetPassword: success — returns confirmation message")
    void resetPassword_success() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("user@example.com");
        req.setOtpCode("123456");
        req.setNewPassword("NewPass1!");
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        String result = authService.resetPassword(req);

        assertThat(result).contains("success");
    }

    @Test
    @DisplayName("resetPassword: fails — HttpClientErrorException wraps as AuthException")
    void resetPassword_failure_throwsAuthException() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("user@example.com");
        req.setOtpCode("wrong");
        req.setNewPassword("NewPass1!");
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "invalid OTP"));

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("reset failed");
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private RefreshToken validRefreshToken(String token, String userUuid) {
        return RefreshToken.builder()
                .token(token)
                .userUuid(userUuid)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    private RefreshToken expiredRefreshToken(String token, String userUuid) {
        return RefreshToken.builder()
                .token(token)
                .userUuid(userUuid)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();
    }
}
