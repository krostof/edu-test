package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchGradingResultDto {
    private int totalAnswers;
    private int gradedCount;
    private int errorCount;
    private int skippedCount;

    public boolean hasErrors() {
        return errorCount > 0;
    }

    public boolean isFullyGraded() {
        return gradedCount == totalAnswers;
    }

    public float getSuccessRate() {
        return totalAnswers > 0 ? (float) gradedCount / totalAnswers * 100 : 0;
    }
}
