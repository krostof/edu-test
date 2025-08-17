package com.edutest.persistance;


import com.edutest.domain.code.CodeSubmission;
import com.edutest.domain.code.CompilationStatus;
import com.edutest.domain.code.ExecutionStatus;
import com.edutest.domain.code.TestCaseResult;
import com.edutest.persistance.entity.code.CodeSubmissionEntity;
import com.edutest.persistance.entity.code.CompilationStatusEnum;
import com.edutest.persistance.entity.code.ExecutionStatusEnum;
import com.edutest.persistance.entity.test.TestCaseResultEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CodeSubmissionMapper {

    public CodeSubmission toDomain(CodeSubmissionEntity entity) {
        if (entity == null) return null;

        return CodeSubmission.builder()
                .id(entity.getId())
                .assignmentId(entity.getAssignment() != null ? entity.getAssignment().getId() : null)
                .testAttemptId(entity.getTestAttempt() != null ? entity.getTestAttempt().getId() : null)
                .studentId(entity.getStudent() != null ? entity.getStudent().getId() : null)
                .sourceCode(entity.getSourceCode())
                .programmingLanguage(entity.getProgrammingLanguage())
                .submittedAt(entity.getSubmittedAt())
                .compilationStatus(mapCompilationStatus(entity.getCompilationStatus()))
                .compilationError(entity.getCompilationError())
                .executionStatus(mapExecutionStatus(entity.getExecutionStatus()))
                .totalScore(entity.getTotalScore())
                .maxExecutionTimeMs(entity.getMaxExecutionTimeMs())
                .maxMemoryUsedMb(entity.getMaxMemoryUsedMb())
                .testCaseResults(mapTestCaseResults(entity.getTestCaseResults()))
                .build();
    }

    public CodeSubmissionEntity toEntity(CodeSubmission domain) {
        if (domain == null) return null;

        return CodeSubmissionEntity.builder()
                .sourceCode(domain.getSourceCode())
                .programmingLanguage(domain.getProgrammingLanguage())
                .submittedAt(domain.getSubmittedAt())
                .compilationStatus(mapCompilationStatusEnum(domain.getCompilationStatus()))
                .compilationError(domain.getCompilationError())
                .executionStatus(mapExecutionStatusEnum(domain.getExecutionStatus()))
                .totalScore(domain.getTotalScore())
                .maxExecutionTimeMs(domain.getMaxExecutionTimeMs())
                .maxMemoryUsedMb(domain.getMaxMemoryUsedMb())
                .build();
    }

    private List<TestCaseResult> mapTestCaseResults(List<TestCaseResultEntity> entities) {
        if (entities == null) return List.of();

        return entities.stream()
                .map(this::mapTestCaseResult)
                .collect(Collectors.toList());
    }

    private TestCaseResult mapTestCaseResult(TestCaseResultEntity entity) {
        if (entity == null) return null;

        return TestCaseResult.builder()
                .id(entity.getId())
                .testCaseId(entity.getTestCase() != null ? entity.getTestCase().getId() : null)
                .submissionId(entity.getSubmission() != null ? entity.getSubmission().getId() : null)
                .actualOutput(entity.getActualOutput())
                .passed(entity.getPassed())
                .executionTimeMs(entity.getExecutionTimeMs())
                .memoryUsedMb(entity.getMemoryUsedMb())
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    private CompilationStatus mapCompilationStatus(CompilationStatusEnum enumValue) {
        if (enumValue == null) return null;
        return switch (enumValue) {
            case NOT_COMPILED -> CompilationStatus.NOT_COMPILED;
            case SUCCESS -> CompilationStatus.SUCCESS;
            case ERROR -> CompilationStatus.ERROR;
            case TIMEOUT -> CompilationStatus.TIMEOUT;
        };
    }

    private CompilationStatusEnum mapCompilationStatusEnum(CompilationStatus status) {
        if (status == null) return null;
        return switch (status) {
            case NOT_COMPILED -> CompilationStatusEnum.NOT_COMPILED;
            case SUCCESS -> CompilationStatusEnum.SUCCESS;
            case ERROR -> CompilationStatusEnum.ERROR;
            case TIMEOUT -> CompilationStatusEnum.TIMEOUT;
        };
    }

    private ExecutionStatus mapExecutionStatus(ExecutionStatusEnum enumValue) {
        if (enumValue == null) return null;
        return switch (enumValue) {
            case NOT_EXECUTED -> ExecutionStatus.NOT_EXECUTED;
            case SUCCESS -> ExecutionStatus.SUCCESS;
            case RUNTIME_ERROR -> ExecutionStatus.RUNTIME_ERROR;
            case TIME_LIMIT_EXCEEDED -> ExecutionStatus.TIME_LIMIT_EXCEEDED;
            case MEMORY_LIMIT_EXCEEDED -> ExecutionStatus.MEMORY_LIMIT_EXCEEDED;
            case SYSTEM_ERROR -> ExecutionStatus.SYSTEM_ERROR;
        };
    }

    private ExecutionStatusEnum mapExecutionStatusEnum(ExecutionStatus status) {
        if (status == null) return null;
        return switch (status) {
            case NOT_EXECUTED -> ExecutionStatusEnum.NOT_EXECUTED;
            case SUCCESS -> ExecutionStatusEnum.SUCCESS;
            case RUNTIME_ERROR -> ExecutionStatusEnum.RUNTIME_ERROR;
            case TIME_LIMIT_EXCEEDED -> ExecutionStatusEnum.TIME_LIMIT_EXCEEDED;
            case MEMORY_LIMIT_EXCEEDED -> ExecutionStatusEnum.MEMORY_LIMIT_EXCEEDED;
            case SYSTEM_ERROR -> ExecutionStatusEnum.SYSTEM_ERROR;
        };
    }
}
