package com.edutest.service.auth;

import com.edutest.persistance.entity.auth.PasswordResetTokenEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.PasswordResetTokenJpaRepository;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenJpaRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.password-reset.token-validity-minutes:30}")
    private long validityMinutes;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    /**
     * Issue a reset token and email a link to the user.
     *
     * Anti-enumeration: returns silently if the email is unknown — caller cannot
     * tell whether the email is registered or not. The endpoint always responds 200.
     */
    @Transactional
    public void requestReset(String email) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown email: {}", email);
            return;
        }
        UserEntity user = userOpt.get();

        String token = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        // ~64 chars, URL-safe (hex only)

        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(validityMinutes))
                .build();
        tokenRepository.save(entity);

        String link = frontendBaseUrl + "/reset-password?token=" + token;
        String body = String.format("""
                Cześć %s,

                Otrzymaliśmy prośbę o zresetowanie hasła do Twojego konta w EduTest.
                Aby ustawić nowe hasło, kliknij poniższy link (ważny %d minut):

                %s

                Jeśli to nie Ty inicjowałeś tę prośbę, zignoruj tę wiadomość — Twoje hasło pozostaje bez zmian.

                EduTest
                """, user.getFirstName(), validityMinutes, link);

        emailService.send(user.getEmail(), "EduTest — reset hasła", body);
        log.info("Password reset token issued for user {}", user.getId());
    }

    /**
     * Validate token without consuming it. Used by frontend to show "token expired"
     * before user types a new password.
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetTokenEntity::isValid)
                .orElse(false);
    }

    @Transactional
    public void completeReset(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        PasswordResetTokenEntity entity = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (!entity.isValid()) {
            throw new IllegalArgumentException(
                    entity.isUsed() ? "This reset link has already been used" : "Reset link expired");
        }

        UserEntity user = entity.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        entity.markUsed();
        tokenRepository.save(entity);

        log.info("Password reset completed for user {}", user.getId());
    }
}
