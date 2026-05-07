package com.edutest.persistance.repository;

import com.edutest.persistance.entity.auth.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByToken(String token);

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt < :before OR t.usedAt IS NOT NULL")
    int deleteExpiredOrUsed(@Param("before") LocalDateTime before);
}
