package com.edutest.codeexecution;

import com.edutest.persistance.entity.code.CompilationStatusEnum;
import com.edutest.persistance.entity.code.ExecutionStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExecutionReport {

    private final CompilationStatusEnum compilationStatus;
    private final String compilationError;
    private final ExecutionStatusEnum executionStatus;
    private final List<TestCaseRunResult> testCaseResults;
    private final Long maxExecutionTimeMs;
    private final Integer maxMemoryUsedMb;

    public static ExecutionReport systemError(String message) {
        return ExecutionReport.builder()
                .compilationStatus(CompilationStatusEnum.NOT_COMPILED)
                .executionStatus(ExecutionStatusEnum.SYSTEM_ERROR)
                .compilationError(message)
                .testCaseResults(List.of())
                .maxExecutionTimeMs(0L)
                .maxMemoryUsedMb(0)
                .build();
    }

    public static ExecutionReport compilationFailed(String error) {
        return ExecutionReport.builder()
                .compilationStatus(CompilationStatusEnum.ERROR)
                .compilationError(error)
                .executionStatus(ExecutionStatusEnum.NOT_EXECUTED)
                .testCaseResults(List.of())
                .maxExecutionTimeMs(0L)
                .maxMemoryUsedMb(0)
                .build();
    }
}
