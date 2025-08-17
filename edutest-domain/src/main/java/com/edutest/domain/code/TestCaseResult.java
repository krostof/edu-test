package com.edutest.domain.code;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {
    private Long id;
    private Long testCaseId;
    private Long submissionId;
    private String actualOutput;
    private Boolean passed;
    private Long executionTimeMs;
    private Integer memoryUsedMb;
    private String errorMessage;

    public boolean isPassed() {
        return Boolean.TRUE.equals(passed);
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    public String getStatusDescription() {
        if (isPassed()) {
            return "PASSED";
        } else if (hasError()) {
            return "ERROR: " + errorMessage;
        } else {
            return "FAILED";
        }
    }

    public boolean exceedsTimeLimit(Long timeLimitMs) {
        return timeLimitMs != null && executionTimeMs != null &&
                executionTimeMs > timeLimitMs;
    }

    public boolean exceedsMemoryLimit(Integer memoryLimitMb) {
        return memoryLimitMb != null && memoryUsedMb != null &&
                memoryUsedMb > memoryLimitMb;
    }
}
