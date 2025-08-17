package com.edutest.persistance.entity.code;


import com.edutest.persistance.entity.common.BaseEntity;
import com.edutest.persistance.entity.assigment.coding.CodingAssignmentEntity;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestCaseResultEntity;
import com.edutest.persistance.entity.user.UserEntity;
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
public class CodeSubmissionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CodingAssignmentEntity assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttemptEntity testAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(name = "source_code", length = 10000, nullable = false)
    private String sourceCode;

    @Column(name = "programming_language", length = 50, nullable = false)
    private String programmingLanguage;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "compilation_status")
    @Enumerated(EnumType.STRING)
    private CompilationStatusEnum compilationStatus;

    @Column(name = "compilation_error", length = 2000)
    private String compilationError;

    @Column(name = "execution_status")
    @Enumerated(EnumType.STRING)
    private ExecutionStatusEnum executionStatus;

    @Column(name = "total_score")
    private Float totalScore;

    @Column(name = "max_execution_time_ms")
    private Long maxExecutionTimeMs;

    @Column(name = "max_memory_used_mb")
    private Integer maxMemoryUsedMb;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TestCaseResultEntity> testCaseResults = new ArrayList<>();

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
}
