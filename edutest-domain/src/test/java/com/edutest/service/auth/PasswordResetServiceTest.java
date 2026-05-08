package com.edutest.service.auth;

import com.edutest.persistance.entity.auth.PasswordResetTokenEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.PasswordResetTokenJpaRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenJpaRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService service;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setFirstName("Alice");

        // @Value-injected fields
        ReflectionTestUtils.setField(service, "validityMinutes", 30L);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:4200");
    }

    @Nested
    @DisplayName("requestReset")
    class RequestResetTests {

        @Test
        @DisplayName("Issues token, persists, sends email with reset link")
        void happyPath() {
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

            service.requestReset("alice@example.com");

            ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor =
                    ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            PasswordResetTokenEntity saved = tokenCaptor.getValue();
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getToken()).isNotBlank();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(29));
            assertThat(saved.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(31));
            assertThat(saved.getUsedAt()).isNull();

            // Email content includes the link
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).send(eq("alice@example.com"), any(), bodyCaptor.capture());
            assertThat(bodyCaptor.getValue())
                    .contains("Alice")
                    .contains("http://localhost:4200/reset-password?token=" + saved.getToken())
                    .contains("30");
        }

        @Test
        @DisplayName("Anti-enumeration: silent no-op for unknown email")
        void unknownEmail() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            service.requestReset("ghost@example.com");

            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).send(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValidTests {

        @Test
        @DisplayName("Returns true for fresh, unused token")
        void validToken() {
            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .user(user)
                    .token("abc")
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

            assertThat(service.isTokenValid("abc")).isTrue();
        }

        @Test
        @DisplayName("Returns false for expired token")
        void expiredToken() {
            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .user(user)
                    .token("abc")
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

            assertThat(service.isTokenValid("abc")).isFalse();
        }

        @Test
        @DisplayName("Returns false for already-used token")
        void usedToken() {
            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .user(user)
                    .token("abc")
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .usedAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

            assertThat(service.isTokenValid("abc")).isFalse();
        }

        @Test
        @DisplayName("Returns false for unknown token")
        void unknownToken() {
            when(tokenRepository.findByToken("nope")).thenReturn(Optional.empty());

            assertThat(service.isTokenValid("nope")).isFalse();
        }
    }

    @Nested
    @DisplayName("completeReset")
    class CompleteResetTests {

        @Test
        @DisplayName("Updates user password (encoded) and marks token used")
        void happyPath() {
            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .user(user)
                    .token("abc")
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NewPassw0rd!")).thenReturn("encoded-new");

            service.completeReset("abc", "NewPassw0rd!");

            assertThat(user.getPassword()).isEqualTo("encoded-new");
            verify(userRepository).save(user);
            assertThat(token.getUsedAt()).isNotNull();
            verify(tokenRepository).save(token);
        }

        @Test
        @DisplayName("Rejects passwords shorter than 6 characters")
        void shortPassword() {
            assertThatThrownBy(() -> service.completeReset("abc", "12345"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 6");
            // Repository must not be touched on validation failure
            verify(tokenRepository, never()).findByToken(any());
        }

        @Test
        @DisplayName("Rejects null password")
        void nullPassword() {
            assertThatThrownBy(() -> service.completeReset("abc", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rejects unknown token")
        void unknownToken() {
            when(tokenRepository.findByToken("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeReset("nope", "newPass1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid reset token");
        }

        @Test
        @DisplayName("Rejects expired token (no save, no encode)")
        void expiredToken() {
            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .user(user)
                    .token("abc")
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.completeReset("abc", "newPass1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects already-used token with specific message")
        void usedToken() {
            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .user(user)
                    .token("abc")
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .usedAt(LocalDateTime.now().minusMinutes(2))
                    .build();
            when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.completeReset("abc", "newPass1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already been used");
            verify(userRepository, never()).save(any());
        }
    }
}
