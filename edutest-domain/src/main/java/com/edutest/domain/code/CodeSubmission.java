package com.edutest.domain.code;


import com.edutest.persistance.entity.code.CompilationStatusEnum;
import com.edutest.persistance.entity.code.ExecutionStatusEnum;
import com.edutest.persistance.entity.test.TestCaseResultEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CodeSubmission {

    private Long id;
    private Long assignmentId;
    private Long testAttemptId;
    private Long studentId;

    private String sourceCode;
    private String programmingLanguage;
    private LocalDateTime submittedAt;

    private CompilationStatus compilationStatus;
    private String compilationError;
    private ExecutionStatus executionStatus;

    private Float totalScore;
    private Long maxExecutionTimeMs;
    private Integer maxMemoryUsedMb;

    @Builder.Default
    private List<TestCaseResult> testCaseResults = new ArrayList<>();

    public boolean isCompiled() {
        return CompilationStatus.SUCCESS.equals(compilationStatus);
    }

    public boolean hasCompilationError() {
        return CompilationStatus.ERROR.equals(compilationStatus);
    }

    public boolean isExecuted() {
        return executionStatus != null && executionStatus != ExecutionStatus.NOT_EXECUTED;
    }

    public int getPassedTestCases() {
        return (int) testCaseResults.stream()
                .filter(TestCaseResult::isPassed)  // ✅ Domain method
                .count();
    }

    public int getTotalTestCases() {
        return testCaseResults.size();
    }

    public float getPassingPercentage() {
        if (getTotalTestCases() == 0) {
            return 0.0f;
        }
        return (float) getPassedTestCases() / getTotalTestCases() * 100.0f;
    }

    public boolean isSuccessful() {
        return isCompiled() &&
                ExecutionStatus.SUCCESS.equals(executionStatus) &&
                getPassedTestCases() == getTotalTestCases();
    }

    public String getStatusSummary() {
        if (hasCompilationError()) {
            return "Compilation Error";
        } else if (!isExecuted()) {
            return "Not Executed";
        } else if (ExecutionStatus.RUNTIME_ERROR.equals(executionStatus)) {
            return "Runtime Error";
        } else {
            return String.format("Passed %d/%d tests (%.1f%%)",
                    getPassedTestCases(), getTotalTestCases(), getPassingPercentage());
        }
    }

    public boolean hasAnswer() {
        return sourceCode != null && !sourceCode.trim().isEmpty();
    }

    public boolean hasTimeLimit() {
        return maxExecutionTimeMs != null && maxExecutionTimeMs > 0;
    }

    public boolean hasMemoryLimit() {
        return maxMemoryUsedMb != null && maxMemoryUsedMb > 0;
    }

    public boolean exceedsTimeLimit() {
        return hasTimeLimit() && maxExecutionTimeMs != null &&
                testCaseResults.stream()
                        .anyMatch(result -> result.getExecutionTimeMs() > maxExecutionTimeMs);
    }

    public boolean exceedsMemoryLimit() {
        return hasMemoryLimit() && maxMemoryUsedMb != null &&
                testCaseResults.stream()
                        .anyMatch(result -> result.getMemoryUsedMb() > maxMemoryUsedMb);
    }
}
