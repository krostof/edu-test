package com.edutest.persistance.entity.code;


import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.test.CodingAssignment;
import com.edutest.persistance.entity.test.TestAttempt;
import com.edutest.persistance.entity.test.TestCaseResult;
import com.edutest.persistance.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "code_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSubmission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CodingAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttempt testAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "source_code", length = 10000, nullable = false)
    private String sourceCode;

    @Column(name = "programming_language", length = 50, nullable = false)
    private String programmingLanguage;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "compilation_status")
    @Enumerated(EnumType.STRING)
    private CompilationStatus compilationStatus;

    @Column(name = "compilation_error", length = 2000)
    private String compilationError;

    @Column(name = "execution_status")
    @Enumerated(EnumType.STRING)
    private ExecutionStatus executionStatus;

    @Column(name = "total_score")
    private Float totalScore;

    @Column(name = "max_execution_time_ms")
    private Long maxExecutionTimeMs;

    @Column(name = "max_memory_used_mb")
    private Integer maxMemoryUsedMb;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
