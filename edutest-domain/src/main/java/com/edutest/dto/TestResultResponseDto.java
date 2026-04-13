package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TestResultResponseDto {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private Long studentId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Float totalScore;
    private Float maxPossibleScore;
    private Float scorePercentage;
    private boolean fullyGraded;
    private List<AssignmentResultDto> assignmentResults;

    public static TestResultResponseDto create(
            Long attemptId,
            Long testId,
            String testTitle,
            Long studentId,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            Float totalScore,
            Float maxPossibleScore,
            boolean fullyGraded,
            List<AssignmentResultDto> assignmentResults) {
        float percentage = maxPossibleScore != null && maxPossibleScore > 0
                ? (totalScore != null ? totalScore : 0f) / maxPossibleScore * 100f
                : 0f;

        return TestResultResponseDto.builder()
                .attemptId(attemptId)
                .testId(testId)
                .testTitle(testTitle)
                .studentId(studentId)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .totalScore(totalScore != null ? totalScore : 0f)
                .maxPossibleScore(maxPossibleScore)
                .scorePercentage(percentage)
                .fullyGraded(fullyGraded)
                .assignmentResults(assignmentResults)
                .build();
    }
}
