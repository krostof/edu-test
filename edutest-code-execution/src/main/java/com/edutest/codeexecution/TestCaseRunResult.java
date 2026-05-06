package com.edutest.codeexecution;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestCaseRunResult {

    private final Long testCaseId;
    private final boolean passed;
    private final String actualOutput;
    private final String errorMessage;
    private final long executionTimeMs;
    private final int memoryUsedMb;
    private final boolean timedOut;
    private final boolean outOfMemory;
}
