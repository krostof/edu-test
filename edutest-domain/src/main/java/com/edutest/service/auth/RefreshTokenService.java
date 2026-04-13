package com.edutest.service.auth;

import com.edutest.persistance.entity.auth.RefreshTokenEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.RefreshTokenRepository;
import com.edutest.persistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.refreshTokenExpirationDays:7}")
    private int refreshTokenExpirationDays;

    public RefreshTokenEntity createRefreshToken(Long userId) {
        log.debug("Creating refresh token for userId={}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Revoke all existing tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .revoked(false)
                .build();

        RefreshTokenEntity saved = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for userId={}", userId);

        return saved;
    }

    public RefreshTokenEntity createRefreshToken(UserEntity user) {
        log.debug("Creating refresh token for user={}", user.getUsername());

        // Revoke all existing tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .revoked(false)
                .build();

        RefreshTokenEntity saved = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user={}", user.getUsername());

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshTokenEntity> findByToken(String token) {
        return refreshTokenRepository.findValidToken(token, LocalDateTime.now());
    }

    public RefreshTokenEntity verifyExpiration(RefreshTokenEntity token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token was expired. Please login again.");
        }

        if (token.getRevoked()) {
            throw new IllegalArgumentException("Refresh token was revoked. Please login again.");
        }

        return token;
    }

    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(t -> {
            t.revoke();
            refreshTokenRepository.save(t);
            log.info("Revoked refresh token");
        });
    }

    public void revokeAllUserTokens(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            refreshTokenRepository.revokeAllUserTokens(user);
            log.info("Revoked all refresh tokens for userId={}", userId);
        });
    }

    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired refresh tokens");
    }
}
