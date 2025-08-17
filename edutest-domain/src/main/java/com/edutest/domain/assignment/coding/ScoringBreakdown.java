package com.edutest.domain.assignment.coding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringBreakdown {
    private int totalTestCases;
    private int passedTestCases;
    private int failedTestCases;
    private float score;
    private float maxScore;
    private float percentage;
    private boolean isPerfect;

    public String getSummary() {
        return String.format("Passed %d/%d tests (%.1f%%) - Score: %.1f/%.1f",
                passedTestCases, totalTestCases, percentage, score, maxScore);
    }
}