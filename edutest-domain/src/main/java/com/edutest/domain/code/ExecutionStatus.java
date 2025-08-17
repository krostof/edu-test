package com.edutest.domain.code;

public enum ExecutionStatus {
    NOT_EXECUTED("Not Executed"),
    SUCCESS("Execution Successful"),
    RUNTIME_ERROR("Runtime Error"),
    TIME_LIMIT_EXCEEDED("Time Limit Exceeded"),
    MEMORY_LIMIT_EXCEEDED("Memory Limit Exceeded"),
    SYSTEM_ERROR("System Error");

    private final String displayName;

    ExecutionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSuccessful() {
        return this == SUCCESS;
    }

    public boolean hasError() {
        return this == RUNTIME_ERROR || this == SYSTEM_ERROR;
    }

    public boolean hasLimitExceeded() {
        return this == TIME_LIMIT_EXCEEDED || this == MEMORY_LIMIT_EXCEEDED;
    }
}
