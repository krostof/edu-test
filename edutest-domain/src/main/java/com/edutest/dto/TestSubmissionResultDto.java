package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TestSubmissionResultDto {
    private Long attemptId;
    private Long testId;
    private LocalDateTime submittedAt;
    private Float totalScore;
    private Float maxPossibleScore;
    private Float scorePercentage;
    private int gradedCount;
    private int pendingGradingCount;

    public static TestSubmissionResultDto create(
            Long attemptId,
            Long testId,
            LocalDateTime submittedAt,
            Float totalScore,
            Float maxPossibleScore,
            int gradedCount,
            int pendingGradingCount) {
        float percentage = maxPossibleScore != null && maxPossibleScore > 0
                ? (totalScore != null ? totalScore : 0f) / maxPossibleScore * 100f
                : 0f;

        return TestSubmissionResultDto.builder()
                .attemptId(attemptId)
                .testId(testId)
                .submittedAt(submittedAt)
                .totalScore(totalScore != null ? totalScore : 0f)
                .maxPossibleScore(maxPossibleScore)
                .scorePercentage(percentage)
                .gradedCount(gradedCount)
                .pendingGradingCount(pendingGradingCount)
                .build();
    }
}
