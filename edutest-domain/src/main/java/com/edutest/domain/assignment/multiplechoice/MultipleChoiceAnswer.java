package com.edutest.domain.assignment.multiplechoice;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultipleChoiceAnswer {

    private Long id;
    private Long assignmentId;
    private Long testAttemptId;
    private Long studentId;
    private LocalDateTime answeredAt;
    private Float score;
    private boolean graded;
    private String teacherFeedback;

    @Builder.Default
    private List<Long> selectedOptionIds = new ArrayList<>();

    public boolean hasAnswer() {
        return selectedOptionIds != null && !selectedOptionIds.isEmpty();
    }

    public boolean isGraded() {
        return graded;
    }

    public int getSelectedCount() {
        return selectedOptionIds.size();
    }

    public boolean isOptionSelected(Long optionId) {
        return selectedOptionIds.contains(optionId);
    }
}