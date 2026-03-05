package com.sourabh.user_service.service.impl;

import com.sourabh.user_service.dto.request.RegisterRequest;
import com.sourabh.user_service.entity.Role;
import com.sourabh.user_service.entity.User;
import com.sourabh.user_service.entity.UserStatus;
import com.sourabh.user_service.exception.UserAlreadyExistsException;
import com.sourabh.user_service.repository.OTPVerificationRepository;
import com.sourabh.user_service.repository.UserRepository;
import com.sourabh.user_service.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl} focusing on the user registration flow.
 *
 * <p>Two critical edge-cases are verified:</p>
 * <ol>
 *   <li><strong>Duplicate email rejection</strong> &ndash; registering with an email that
 *       already belongs to a non-deleted account must throw
 *       {@link UserAlreadyExistsException}.</li>
 *   <li><strong>Soft-deleted user re-registration</strong> &ndash; if the existing account
 *       was previously soft-deleted ({@code isDeleted=true}), the system should
 *       reset the record and allow registration to proceed.</li>
 * </ol>
 *
 * <p>All repository and external-service interactions are mocked via Mockito.
 * {@link PasswordEncoder#encode} is stubbed leniently because only the
 * re-registration test actually triggers password encoding.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    /** Mock for user persistence operations. */
    @Mock
    private UserRepository userRepository;

    /** Mock for OTP record persistence (unused directly but required by the service). */
    @Mock
    private OTPVerificationRepository otpRepository;

    /** Mock for outbound email delivery (OTP emails on registration). */
    @Mock
    private EmailService emailService;

    /** Mock for BCrypt password hashing. */
    @Mock
    private PasswordEncoder passwordEncoder;

    /** Service under test with all mocked dependencies auto-injected. */
    @InjectMocks
    private UserServiceImpl service;

    /** Captures the {@link User} entity passed to {@code userRepository.save()}. */
    @Captor
    private ArgumentCaptor<User> userCaptor;

    /** Reusable registration request populated before each test. */
    private RegisterRequest sampleRequest;

    /**
     * Builds a standard registration request and leniently stubs the password
     * encoder.  The lenient stub prevents Mockito's strict-stubbing exception
     * in tests that never invoke {@code passwordEncoder.encode()} (e.g. the
     * duplicate-rejection test).
     */
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

    /**
     * Ensures that attempting to register with an email already tied to a
     * non-deleted user throws {@link UserAlreadyExistsException} and that
     * no user entity is persisted.
     */
    @Test
    void register_throwsIfNonDeletedAlreadyExists() {
        User existing = User.builder()
                .email("foo@example.com")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .build();
        when(userRepository.findByEmailIgnoreCase("foo@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.registerUser(sampleRequest))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    /**
     * Verifies that a previously soft-deleted user can re-register with the
     * same email.  The saved entity must:
     * <ul>
     *   <li>Have {@code isDeleted} reset to {@code false}.</li>
     *   <li>Transition to {@link UserStatus#PENDING_VERIFICATION}.</li>
     *   <li>Contain the newly encoded password.</li>
     * </ul>
     * An OTP email must also be dispatched to the registering address.
     */
    @Test
    void register_allowsRecreationOfDeletedUser() {
        User existing = User.builder()
                .email("foo@example.com")
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .isDeleted(true)
                .build();
        when(userRepository.findByEmailIgnoreCase("foo@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registerUser(sampleRequest);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(saved.getPassword()).isEqualTo("encoded");
        verify(emailService).sendOtpEmail(eq("foo@example.com"), any(), any(), any());
    }
}
