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
        // password encoding only needed in tests that actually save the user,
        // use lenient() so the first scenario (existing user) does not trigger
        // unnecessary stubbing exception.
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
        when(userRepository.findByEmailIgnoreCase("foo@example.com")).thenReturn(Optional.of(existing));

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
