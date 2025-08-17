package com.edutest.persistance.entity.test;


import com.edutest.persistance.entity.assigment.coding.TestCaseEntity;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
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
public class TestCaseResultEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCaseEntity testCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private CodeSubmissionEntity submission;

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
}

