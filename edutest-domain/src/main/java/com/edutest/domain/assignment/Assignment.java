package com.edutest.domain.assignment;

import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.test.Test;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public abstract class Assignment {

    private Long id;
    private Long testId;
    private String title;
    private String description;
    private Integer orderNumber;
    private Integer points;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public abstract AssignmentType getType();
    public abstract ValidationResult validateAnswer(String answer);
    public abstract Float calculateScore(String answer);
    public abstract boolean supportsAttachments();

    public boolean hasValidPoints() {
        return points != null && points > 0;
    }

    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }

    public boolean isValid() {
        return hasTitle() && hasValidPoints();
    }

    public Float getScorePercentage(Float score) {
        if (score == null || points == null || points == 0) {
            return 0.0f;
        }
        return (score / points) * 100.0f;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePoints(Integer newPoints) {
        this.points = newPoints;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTest(Test test) {
        if (test != null) {
            this.testId = test.getId();
        } else {
            this.testId = null;
        }
        this.updatedAt = LocalDateTime.now();
    }
}