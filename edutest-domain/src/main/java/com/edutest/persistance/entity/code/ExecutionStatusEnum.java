package com.edutest.persistance.entity.code;

import lombok.Getter;

public enum ExecutionStatusEnum {
    NOT_EXECUTED("Not Executed"),
    SUCCESS("Execution Successful"),
    RUNTIME_ERROR("Runtime Error"),
    TIME_LIMIT_EXCEEDED("Time Limit Exceeded"),
    MEMORY_LIMIT_EXCEEDED("Memory Limit Exceeded"),
    SYSTEM_ERROR("System Error");

    private final String displayName;

    ExecutionStatusEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
