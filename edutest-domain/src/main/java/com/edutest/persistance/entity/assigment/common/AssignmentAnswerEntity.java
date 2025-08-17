package com.edutest.persistance.entity.assigment.common;

import com.edutest.persistance.entity.assigment.AssignmentEntity;
import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "assignment_answers")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "answer_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AssignmentAnswerEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AssignmentEntity assignmentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttemptEntity testAttemptEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "score")
    private Float score;

    @Column(name = "is_graded")
    @Builder.Default
    private Boolean isGraded = false;

    @Column(name = "teacher_feedback", length = 1000)
    private String teacherFeedback;

    private void setAnsweredAt() {
        if (answeredAt == null) {
            answeredAt = LocalDateTime.now();
        }
    }

    @PrePersist
    @PreUpdate
    private void validateStudent() {
        if (student != null && !student.isStudent()) {
            throw new IllegalStateException("Only students can submit answers");
        }
    }

    // Abstract methods to be implemented by subclasses
    public abstract boolean isCorrect();
    public abstract float calculateScore();
    public abstract String getAnswerText();

    // Business methods
    public void grade(float score, String feedback) {
        this.score = score;
        this.teacherFeedback = feedback;
        this.isGraded = true;
    }

    public void autoGrade() {
        this.score = calculateScore();
        this.isGraded = true;
    }

    public boolean needsManualGrading() {
        return !isGraded && assignmentEntity.getType().requiresManualGrading();
    }

    public boolean isAutoGradeable() {
        return assignmentEntity.getType().isAutoGradeable();
    }

    public float getScorePercentage() {
        if (score == null || assignmentEntity.getPoints() == 0) {
            return 0.0f;
        }
        return (score / assignmentEntity.getPoints()) * 100.0f;
    }
}
