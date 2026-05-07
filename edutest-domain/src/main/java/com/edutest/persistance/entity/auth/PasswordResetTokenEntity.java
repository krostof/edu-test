package com.edutest.persistance.entity.auth;

import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Single-use, time-limited token issued when a user requests a password reset.
 * Marked as used after consumption to prevent replay.
 */
@Entity
@Table(name = "password_reset_tokens",
        indexes = @Index(name = "idx_password_reset_tokens_token", columnList = "token"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetTokenEntity extends BaseEntity {

    @Column(name = "token", nullable = false, unique = true, length = 80)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isUsed() && !isExpired();
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
