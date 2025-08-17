package com.edutest.persistance.entity.test;


import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "test_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttemptEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private TestEntity testEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "score")
    private Float score;

    @Column(name = "is_completed", nullable = false)
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
        if (testEntity.getTimeLimit() == null || startedAt == null) {
            return Long.MAX_VALUE; // Brak limitu czasowego
        }

        long elapsed = getElapsedTimeInMinutes();
        long remaining = testEntity.getTimeLimit() - elapsed;
        return Math.max(0, remaining);
    }

    public boolean isTimeExpired() {
        if (testEntity.getTimeLimit() == null) {
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
