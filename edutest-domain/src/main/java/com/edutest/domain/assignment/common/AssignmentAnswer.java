package com.edutest.domain.assignment.common;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AssignmentAnswer {

    private Long id;
    private Long assignmentId;
    private Long testAttemptId;
    private Long studentId;
    private LocalDateTime answeredAt;
    private Float score;
    private boolean graded;
    private String teacherFeedback;

    public abstract boolean isCorrect();
    public abstract Float calculateScore(Integer maxPoints);
    public abstract String getAnswerText();
    public abstract boolean hasAnswer();
    public abstract boolean isValidAnswer();
    public abstract String getValidationError();

    public boolean isGraded() {
        return graded;
    }

    public boolean needsGrading() {
        return hasAnswer() && !graded;
    }

    public boolean hasScore() {
        return score != null;
    }

    public boolean hasFeedback() {
        return teacherFeedback != null && !teacherFeedback.trim().isEmpty();
    }

    public void grade(Float newScore, String feedback) {
        this.score = newScore;
        this.teacherFeedback = feedback;
        this.graded = true;
    }

    public void grade(Float newScore) {
        grade(newScore, null);
    }

    public void resetGrading() {
        this.score = null;
        this.teacherFeedback = null;
        this.graded = false;
    }

    public Float getScorePercentage(Integer maxPoints) {
        if (score == null || maxPoints == null || maxPoints == 0) {
            return 0.0f;
        }
        return (score / maxPoints) * 100.0f;
    }

    public boolean isPerfectScore(Integer maxPoints) {
        return score != null && maxPoints != null &&
                score.equals(maxPoints.floatValue());
    }

    public boolean isZeroScore() {
        return score != null && score == 0.0f;
    }

    public boolean hasBeenAnswered() {
        return answeredAt != null;
    }

    public void markAsAnswered() {
        if (answeredAt == null) {
            answeredAt = LocalDateTime.now();
        }
    }

    public long getAnswerAgeInMinutes() {
        if (answeredAt == null) {
            return 0;
        }
        return java.time.Duration.between(answeredAt, LocalDateTime.now()).toMinutes();
    }

    // Validation for base class
    public boolean isComplete() {
        return hasAnswer() && isValidAnswer();
    }

    public String getCompletionStatus() {
        if (!hasAnswer()) {
            return "NOT_ANSWERED";
        } else if (!isValidAnswer()) {
            return "INVALID_ANSWER";
        } else if (!isGraded()) {
            return "PENDING_GRADING";
        } else {
            return "COMPLETED";
        }
    }
}