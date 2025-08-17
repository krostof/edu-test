package com.edutest.domain.assignment.openquestion;

import com.edutest.domain.assignment.common.AssignmentAnswer;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.PreUpdate;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OpenQuestionAnswer {

    private Long id;
    private Long assignmentId;
    private Long testAttemptId;
    private Long studentId;
    private LocalDateTime answeredAt;
    private Float score;
    private boolean graded;
    private String teacherFeedback;

    private String answerText;
    private Integer wordCount;
    private Integer characterCount;

    public boolean hasAnswer() {
        return answerText != null && !answerText.trim().isEmpty();
    }

    public boolean isGraded() {
        return graded;
    }

    public boolean isEmpty() {
        return answerText == null || answerText.trim().isEmpty();
    }
}
