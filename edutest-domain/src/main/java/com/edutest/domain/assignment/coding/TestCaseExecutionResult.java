package com.edutest.domain.assignment.coding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseExecutionResult {
    private Long testCaseId;
    private boolean passed;
    private String actualOutput;
    private String errorMessage;
    private long executionTimeMs;
    private int memoryUsedMb;

    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    public String getStatus() {
        if (passed) return "PASSED";
        if (hasError()) return "ERROR";
        return "FAILED";
    }
}