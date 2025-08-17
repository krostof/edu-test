package com.edutest.domain.assignment.singlechoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SingleChoiceAnswer {

    private Long id;
    private Long assignmentId;
    private Long testAttemptId;
    private Long studentId;
    private LocalDateTime answeredAt;
    private Float score;
    private boolean graded;
    private String teacherFeedback;

    private Long selectedOptionId;

    public boolean hasAnswer() {
        return selectedOptionId != null;
    }

    public boolean isOptionSelected(Long optionId) {
        return selectedOptionId != null && selectedOptionId.equals(optionId);
    }
}