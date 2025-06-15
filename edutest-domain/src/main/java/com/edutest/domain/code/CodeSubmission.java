package com.edutest.domain.code;


import com.edutest.domain.assignment.coding.CodingAssignment;
import com.edutest.domain.test.TestAttempt;
import com.edutest.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "code_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSubmission extends BaseEntity {

    private CodingAssignment assignment;

    private TestAttempt testAttempt;

    private User student;

    private String sourceCode;

    private String programmingLanguage;

    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private CompilationStatus compilationStatus;

    private String compilationError;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus executionStatus;

    private Float totalScore;

    private Long maxExecutionTimeMs;

    private Integer maxMemoryUsedMb;

    @Builder.Default
    private List<TestCaseResult> testCaseResults = new ArrayList<>();

    @PrePersist
    private void setSubmittedAt() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    @PrePersist
    @PreUpdate
    private void validateStudent() {
        if (student != null && !student.isStudent()) {
            throw new IllegalStateException("Only students can submit code");
        }
    }

    // Business methods
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
                .filter(TestCaseResult::isPassed)
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

    public void addTestCaseResult(TestCaseResult result) {
        if (result != null) {
            result.setSubmission(this);
            testCaseResults.add(result);
        }
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
}
