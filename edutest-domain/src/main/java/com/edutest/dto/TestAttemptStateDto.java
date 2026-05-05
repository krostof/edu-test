package com.edutest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestAttemptStateDto {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
    private Long remainingTimeSeconds;
    private LocalDateTime startedAt;
    private Boolean isCompleted;
    private Boolean allowNavigation;
    private List<Long> assignmentOrder;
    private List<Long> answeredAssignmentIds;
}
