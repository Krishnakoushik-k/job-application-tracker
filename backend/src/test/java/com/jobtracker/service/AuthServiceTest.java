package com.jobtracker.service;

import com.jobtracker.dto.AuthResponse;
import com.jobtracker.dto.LoginRequest;
import com.jobtracker.dto.RegisterRequest;
import com.jobtracker.entity.User;
import com.jobtracker.exception.EmailAlreadyExistsException;
import com.jobtracker.exception.InvalidCredentialsException;
import com.jobtracker.mapper.UserMapper;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // The constructor pre-computes a dummy hash used to equalise timing for unknown emails.
        when(passwordEncoder.encode("timing-attack-protection")).thenReturn("dummy-hash");
        authService = new AuthService(userRepository, passwordEncoder, jwtService, new UserMapper());
    }

    @Test
    void registerHashesPasswordNormalizesEmailAndReturnsToken() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(), org.mockito.ArgumentMatchers.eq("asha@example.com")))
                .thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(
                new RegisterRequest("  Asha Reddy ", "  Asha@Example.COM ", "Secret123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("asha@example.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("Asha Reddy");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-password").isNotEqualTo("Secret123");

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
        assertThat(response.user().email()).isEqualTo("asha@example.com");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Asha", "asha@example.com", "Secret123")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registerTranslatesUniqueIndexRaceIntoEmailAlreadyExists() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Asha", "asha@example.com", "Secret123")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User user = userWithHash("stored-hash");
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123", "stored-hash")).thenReturn(true);
        when(jwtService.generateToken(any(), org.mockito.ArgumentMatchers.eq("asha@example.com")))
                .thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest(" ASHA@example.com ", "Secret123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("asha@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        User user = userWithHash("stored-hash");
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("asha@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginFailsForUnknownEmailWithSameMessageAndStillHashes() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "Secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder).matches("Secret123", "dummy-hash");
    }

    private User userWithHash(String hash) {
        User user = new User();
        user.setFullName("Asha Reddy");
        user.setEmail("asha@example.com");
        user.setPasswordHash(hash);
        return user;
    }
}
