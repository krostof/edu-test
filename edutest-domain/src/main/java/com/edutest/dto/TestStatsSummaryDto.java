package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestStatsSummaryDto {
    private Long testId;
    private String testTitle;
    private int totalAttempts;
    private int completedAttempts;
    private int inProgressAttempts;
    private int gradedAttempts;
    private Float averageScore;
    private Float medianScore;
    private Float minScore;
    private Float maxScore;
    private Float averageScorePercentage;
    private List<ScoreDistributionItemDto> scoreDistribution;
}
