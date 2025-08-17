package com.edutest.domain.test;


import com.edutest.domain.user.User;
import com.edutest.persistance.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttempt extends BaseEntity {

    private Test test;

    private User student;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Float score;

    @Builder.Default
    private Boolean isCompleted = false;

    @PrePersist
    private void validateStudent() {
        if (student != null && !student.isStudent()) {
            throw new IllegalStateException("Only students can have test attempts");
        }
    }

    // Business methods
    public boolean isInProgress() {
        return !isCompleted && startedAt != null && finishedAt == null;
    }

    public boolean isFinished() {
        return isCompleted || finishedAt != null;
    }

    public long getElapsedTimeInMinutes() {
        if (startedAt == null) {
            return 0;
        }

        LocalDateTime endTime = finishedAt != null ? finishedAt : LocalDateTime.now();
        return ChronoUnit.MINUTES.between(startedAt, endTime);
    }

    public long getRemainingTimeInMinutes() {
        if (test.getTimeLimit() == null || startedAt == null) {
            return Long.MAX_VALUE; // Brak limitu czasowego
        }

        long elapsed = getElapsedTimeInMinutes();
        long remaining = test.getTimeLimit() - elapsed;
        return Math.max(0, remaining);
    }

    public boolean isTimeExpired() {
        if (test.getTimeLimit() == null) {
            return false;
        }

        return getRemainingTimeInMinutes() <= 0;
    }

    public void finish() {
        this.finishedAt = LocalDateTime.now();
        this.isCompleted = true;
    }

    public void finish(Float score) {
        finish();
        this.score = score;
    }

    public boolean canBeResumed() {
        return isInProgress() && !isTimeExpired();
    }

    public String getStatus() {
        if (isCompleted) {
            return "COMPLETED";
        } else if (isTimeExpired()) {
            return "TIME_EXPIRED";
        } else if (isInProgress()) {
            return "IN_PROGRESS";
        } else {
            return "NOT_STARTED";
        }
    }
}
