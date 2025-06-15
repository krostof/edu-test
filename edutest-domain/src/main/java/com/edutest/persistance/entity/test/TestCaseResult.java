package com.edutest.persistance.entity.test;


import com.edutest.persistance.entity.assigment.coding.TestCase;
import com.edutest.persistance.entity.code.CodeSubmission;
import com.edutest.persistance.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_case_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private CodeSubmission submission;

    @Column(name = "actual_output", length = 2000)
    private String actualOutput;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "memory_used_mb")
    private Integer memoryUsedMb;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public boolean isTimeLimitExceeded(Integer timeLimitMs) {
        return timeLimitMs != null &&
                executionTimeMs != null &&
                executionTimeMs > timeLimitMs;
    }

    public boolean isMemoryLimitExceeded(Integer memoryLimitMb) {
        return memoryLimitMb != null &&
                memoryUsedMb != null &&
                memoryUsedMb > memoryLimitMb;
    }

    public boolean isPassed() {
        return Boolean.TRUE.equals(passed);
    }

    public String getStatusDescription() {
        if (passed) {
            return "PASSED";
        } else if (errorMessage != null && !errorMessage.isEmpty()) {
            return "ERROR: " + errorMessage;
        } else if (isTimeLimitExceeded(testCase.getAssignment().getTimeLimitMs())) {
            return "TIME_LIMIT_EXCEEDED";
        } else if (isMemoryLimitExceeded(testCase.getAssignment().getMemoryLimitMb())) {
            return "MEMORY_LIMIT_EXCEEDED";
        } else {
            return "WRONG_ANSWER";
        }
    }
}
