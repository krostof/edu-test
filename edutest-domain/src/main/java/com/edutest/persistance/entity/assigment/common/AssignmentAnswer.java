package com.edutest.persistance.entity.assigment.common;

import com.edutest.persistance.entity.assigment.Assignment;
import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.test.TestAttempt;
import com.edutest.persistance.entity.user.User;
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
public abstract class AssignmentAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttempt testAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "score")
    private Float score;

    @Column(name = "is_graded")
    @Builder.Default
    private Boolean isGraded = false;

    @Column(name = "teacher_feedback", length = 1000)
    private String teacherFeedback;

    @PrePersist
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
        return !isGraded && assignment.getType().requiresManualGrading();
    }

    public boolean isAutoGradeable() {
        return assignment.getType().isAutoGradeable();
    }

    public float getScorePercentage() {
        if (score == null || assignment.getPoints() == 0) {
            return 0.0f;
        }
        return (score / assignment.getPoints()) * 100.0f;
    }
}
